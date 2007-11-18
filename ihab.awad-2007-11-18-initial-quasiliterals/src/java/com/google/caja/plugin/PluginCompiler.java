// Copyright (C) 2006 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.caja.plugin;

import com.google.caja.lexer.*;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.js.*;
import com.google.caja.plugin.GxpCompiler.BadContentException;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Criterion;
import com.google.caja.util.Pair;

import java.io.StringReader;

import java.util.*;

/**
 * Compiles a bundle of css, javascript, and gxp files to a sandboxed javascript
 * and css widget.
 *
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public final class PluginCompiler {
  private MessageQueue mq;
  private final PluginMeta meta;
  private final List<Input> inputs = new ArrayList<Input>();
  private Block jsTree;
  /** An object that is not available to sandboxed code. */
  private ObjectConstructor pluginNamespace;
  /** An object that is available to sandboxed code. */
  private ObjectConstructor pluginPrivate;

  public PluginCompiler(PluginMeta meta) {
    this.mq = new SimpleMessageQueue();
    this.meta = meta;
  }

  public MessageQueue getMessageQueue() { return mq; }

  public void setMessageQueue(MessageQueue inputMessageQueue) {
    assert null != inputMessageQueue;
    this.mq = inputMessageQueue;
  }

  public PluginMeta getPluginMeta() { return meta; }

  public void addInput(ParseTreeNode input) {
    inputs.add(new Input(input));
  }

  public List<? extends ParseTreeNode> getInputs() {
    ParseTreeNode[] inputsCopy = new ParseTreeNode[this.inputs.size()];
    for (int i = 0; i < inputsCopy.length; ++i) {
      inputsCopy[i] = this.inputs.get(i).parsetree;
    }
    return Arrays.asList(inputsCopy);
  }

  /**
   * The list of parse trees that comprise the plugin after run has been called.
   * Valid after run has been called.
   */
  public List<? extends ParseTreeNode> getOutputs() {
    assert null != pluginNamespace;
    List<ParseTreeNode> outputs = new ArrayList<ParseTreeNode>();
    outputs.add(getJavascript());
    for (Input input : inputs) {
      switch (input.type) {
        case JAVASCRIPT: case GXP: case CSS_TEMPLATE:
          // Have been rolled into the plugin namespace
          break;
        case CSS:
          outputs.add(input.parsetree);
          break;
        default:
          throw new AssertionError();
      }
    }
    return outputs;
  }

  public Block getJavascript() { return this.jsTree; }

  /**
   * Run the compiler on all parse trees added via {@link #addInput}.
   * The output parse trees are available via {@link #getOutputs()}.
   * @return true on success, false on failure.
   */
  public boolean run() {
    return (// First we create an object to hold the plugin
            setUpNamespace()
            // Rhen we make sure the css is well formed and we prefix all rules
            // so that they don't affect nodes outside the plugin.
            && validateCss()
            // Now we compile the gxps and add methods to the plugin for each
            // gxp and each event handler.
            && compileGxps()
            // Compile the CSS templates and add a method to the plugin for each
            // template
            && compileCssTemplates()
            // Replace global definitions with operations that modify the
            // plugin.
            && moveGlobalDefinitionsIntoPluginNamespace()
            // Put all the top level javascript code into an initializer block
            // that will set up the plugin.
            && consolidateCodeIntoInitializerBody()
            // Replace references to globals with plugin accesses.
            // The handling of global declarations happens before consolidation
            // so that we know what is global.  This happens after consolidation
            // since it needs to affect references inside code compiled from
            // gxps.
            && rewriteGlobalReference()
            // Rewrite the javascript to prevent runtime sandbox violations
            && validateJavascript()
            && hasNoFatalErrors()
            );
  }

  private boolean setUpNamespace() {
    this.pluginNamespace = s(
        new ObjectConstructor(
            Collections.<Pair<Literal, Expression>>emptyList()));
    // Create a meta object that contains the various prefixes and that is not
    // reachable from the plugin
    this.pluginPrivate = s(
        new ObjectConstructor(
            Collections.<Pair<Literal, Expression>>emptyList()));
    // This will look something like
    // var MyPluginMeta = {
    //   'nsPrefix':   'foo',
    //   'pathPrefix': '/plugin/',
    //   'name':       'MyPlugin',
    // };
    pluginPrivate.createMutation()
        .insertBefore(s(new StringLiteral("'nsPrefix'")), null)
        .insertBefore(s(new StringLiteral(
            StringLiteral.toQuotedValue(meta.namespacePrefix))), null)
        .insertBefore(s(new StringLiteral("'pathPrefix'")), null)
        .insertBefore(s(new StringLiteral(
            StringLiteral.toQuotedValue(meta.pathPrefix))), null)
        .insertBefore(s(new StringLiteral("'name'")), null)
        .insertBefore(s(new StringLiteral(
            StringLiteral.toQuotedValue(meta.namespaceName))), null)
        .execute();
    // Create a tree that contains the declarations for the two and some
    // initialization code.
    // var PLUGIN_PRIVATE = { ... };
    // var PLUGIN = { .. };
    // PLUGIN_PRIVATE.plugin = PLUGIN;
    // plugin_initialize___(PLUGIN);
    this.jsTree = s(new Block(
                        Arrays.<Statement>asList(
                            // The private plugin
                            s(new Declaration(
                                meta.namespacePrivateName, pluginPrivate)),
                            // The plugin namespace
                            s(new Declaration(
                                meta.namespaceName, pluginNamespace)),
                            s(new ExpressionStmt(s(new Operation(
                                Operator.ASSIGN,
                                s(new Operation(
                                    Operator.MEMBER_ACCESS,
                                    s(new Reference(meta.namespacePrivateName)),
                                    s(new Reference("plugin")))),
                                s(new Reference(meta.namespaceName))
                                )))),
                            s(new ExpressionStmt(s(new Operation(
                                Operator.FUNCTION_CALL,
                                s(new Reference("plugin_initialize___")),
                                s(new Reference(meta.namespacePrivateName))))))
                        )));
    this.jsTree.parentify();
    return hasNoFatalErrors();
  }

  private boolean compileGxps() {
    List<GxpJob> jobs = new ArrayList<GxpJob>();
    for (Input input : inputs) {
      if (InputType.JAVASCRIPT == input.type) {
        GxpCompileDirectiveReplacer r = new GxpCompileDirectiveReplacer(mq);
        input.parsetree.acceptPreOrder(r);
        jobs.addAll(r.getDoms());
      } else if (InputType.GXP == input.type) {
        jobs.add(new GxpJob((DomTree.Tag) input.parsetree, null));
      }
    }
    GxpCompiler gxpc = new GxpCompiler(mq, meta);
    GxpValidator v = new GxpValidator(mq);
    for (Iterator<GxpJob> it = jobs.iterator(); it.hasNext();) {
      GxpJob job = it.next();
      if (!v.validate(job.docRoot)) {
        it.remove();
        continue;
      }
      try {
        job.sig = gxpc.compileTemplateSignature(job.docRoot);
      } catch (GxpCompiler.BadContentException ex) {
        ex.toMessageQueue(mq);
        it.remove();
      }
    }
    for (Iterator<GxpJob> it = jobs.iterator(); it.hasNext();) {
      GxpJob job = it.next();
      try {
        job.compiled = gxpc.compileDocument(job.sig);

        // Create a node under PluginMeta for the canonical reference.
        // This is used  for calls from one gxp to another to foil attacks
        // that rewrite a called gxp to emit unsafe content.
        this.pluginPrivate.createMutation()
            .insertBefore(s(new StringLiteral(
                StringLiteral.toQuotedValue(job.sig.assignedName))), null)
            .insertBefore(job.compiled, null)
            .execute();

        Expression templateRef = s(
            new Operation(
                Operator.MEMBER_ACCESS,
                s(new Reference(meta.namespacePrivateName)),
                s(new Reference(job.sig.getAssignedName()))));

        // Either replace it or put a reference to it in the main plugin
        if (null == job.toReplace) {
          StringLiteral gxpName = s(
              new StringLiteral(StringLiteral.toQuotedValue(
                  job.sig.templateName)));
          this.pluginNamespace.createMutation()
            .insertBefore(gxpName, null)
            .insertBefore(templateRef, null)
            .execute();
        } else {
          ((MutableParseTreeNode) job.toReplace.getParent()).replaceChild(
              templateRef, job.toReplace);
        }
      } catch (GxpCompiler.BadContentException ex) {
        ex.toMessageQueue(mq);
        it.remove();
      }
    }

    for (FunctionDeclaration handler : gxpc.getEventHandlers()) {
      StringLiteral gxpName = s(
          new StringLiteral(StringLiteral.toQuotedValue(
              handler.getIdentifier())));
      Expression function = handler.getInitializer();
      handler.replaceChild(
          new FunctionConstructor(
              null, Collections.<FormalParam>emptyList(),
              new Block(Collections.<Statement>emptyList())),
          function);
      this.pluginNamespace.createMutation()
        .insertBefore(gxpName, null)
        .insertBefore(function, null)
        .execute();
    }

    return hasNoFatalErrors();
  }

  /** Takes CSS templates and turns them into functions. */
  private boolean compileCssTemplates() {
    for (Input input : inputs) {
      if (InputType.CSS_TEMPLATE != input.type) { continue; }
      CssTemplate t = (CssTemplate) input.parsetree;
      FunctionConstructor function;
      try {
        function = t.toJavascript(meta, mq);
      } catch (BadContentException ex) {
        ex.toMessageQueue(mq);
        continue;
      }

      StringLiteral templateName =
        s(new StringLiteral(StringLiteral.toQuotedValue(function.getName())));
      this.pluginNamespace.createMutation()
        .insertBefore(templateName, null)
        .insertBefore(function, null)
        .execute();
    }
    return hasNoFatalErrors();
  }

  /**
   * Validates and rewrites css inputs.
   * @return true if the input css was safe.  False if any destructive
   *   modifications had to be made to make it safe, or if such modifications
   *   were needed but could not be made.
   */
  private boolean validateCss() {
    // TODO(mikesamuel): Build a list of classes and ids for use in generating
    // "no such symbol" warnings from the GXPs.
    boolean valid = true;
    CssValidator v = new CssValidator(mq);
    CssRewriter rw = new CssRewriter(meta, mq);
    for (Input input : inputs) {
      CssTree css;
      if (InputType.CSS == input.type) {
        css = (CssTree.StyleSheet) input.parsetree;
      } else if (InputType.CSS_TEMPLATE == input.type) {
        css = ((CssTemplate) input.parsetree).getCss();
      } else {
        continue;
      }
      valid &= v.validateCss(css);
      valid &= rw.rewrite(css);
    }

    return valid && hasNoFatalErrors();
  }

  private boolean moveGlobalDefinitionsIntoPluginNamespace() {
    GlobalDefRewriter rw = new GlobalDefRewriter(meta);

    for (Input input : inputs) {
      if (InputType.JAVASCRIPT == input.type) {
        Block body = (Block) input.parsetree;
        body.acceptPreOrder(rw);
      }
    }

    return hasNoFatalErrors();
  }

  private boolean consolidateCodeIntoInitializerBody() {
    // Create an initializer function
    Block initFunctionBody = s(new Block(Collections.<Statement>emptyList()));

    MutableParseTreeNode.Mutation newChanges
        = initFunctionBody.createMutation();
    for (Input input : inputs) {
      if (InputType.JAVASCRIPT == input.type) {
        Block body = (Block) input.parsetree;
        MutableParseTreeNode.Mutation oldChanges = body.createMutation();
        for (Statement s : body.children()) {
          oldChanges.removeChild(s);
          newChanges.insertBefore(s, null);
        }
        oldChanges.execute();
      }
    }
    newChanges.execute();

    // (function () { <initializer code> }).call(<plugin namespace);
    // call
    //   member access
    //     function constructor
    //       initializer body
    //     reference: call
    //   reference: plugin_namespace
    this.jsTree.insertBefore(
        s(new ExpressionStmt(
            s(new Operation(
                  Operator.FUNCTION_CALL,
                  s(new Operation(
                        Operator.MEMBER_ACCESS,
                        s(new FunctionConstructor(
                              null, Collections.<FormalParam>emptyList(),
                              initFunctionBody)),
                        s(new Reference("call"))
                        )),
                  s(new Reference(meta.namespaceName))
                  ))
            )),
        null);

    return hasNoFatalErrors();
  }

  private boolean rewriteGlobalReference() {
    new GlobalReferenceRewriter(meta).rewrite(
        this.jsTree, Collections.<String>emptySet());

    return hasNoFatalErrors();
  }

  private boolean validateJavascript() {
    if (false) {  // HACK DEBUG
      StringBuffer out = new StringBuffer();
      MessageContext mc = new MessageContext();
      mc.relevantKeys = Collections.singleton(ExpressionSanitizer.SYNTHETIC);
      try {
        jsTree.formatTree(mc, 2, out);
      } catch (java.io.IOException ex) {
        throw new AssertionError(ex);
      }
      System.err.println("rw\n" + out + "\n\n");
    }

    boolean valid = new ExpressionSanitizer(mq).sanitize(this.jsTree);
    return valid && hasNoFatalErrors();
  }

  private boolean hasNoFatalErrors() {
    for (Message m : mq.getMessages()) {
      if (MessageLevel.FATAL_ERROR.compareTo(m.getMessageLevel()) >= 0) {
        System.err.println("m=" + m);
        return false;
      }
    }
    return true;
  }

  /** Make the given parse tree node synthetic. */
  private static <T extends ParseTreeNode> T s(T t) {
    t.getAttributes().set(ExpressionSanitizer.SYNTHETIC, Boolean.TRUE);
    return t;
  }

  private static class Input {
    final ParseTreeNode parsetree;
    final InputType type;

    Input(ParseTreeNode parsetree) {
      assert null != parsetree;
      this.parsetree = parsetree;
      if (parsetree instanceof Statement) {
        this.type = InputType.JAVASCRIPT;
      } else if (parsetree instanceof DomTree.Tag) {
        this.type = InputType.GXP;
      } else if (parsetree instanceof CssTree.StyleSheet) {
        this.type = InputType.CSS;
      } else if (parsetree instanceof CssTemplate) {
        this.type = InputType.CSS_TEMPLATE;
      } else {
        throw new AssertionError("Unknown input type " + parsetree);
      }
    }
  }

  private enum InputType {
    CSS,
    CSS_TEMPLATE,
    JAVASCRIPT,
    GXP,
    ;
  }
}

final class GxpCompileDirectiveReplacer implements Visitor {
  private final List<GxpJob> jobs = new ArrayList<GxpJob>();
  private final MessageQueue mq;

  GxpCompileDirectiveReplacer(MessageQueue mq) {
    this.mq = mq;
  }

  List<GxpJob> getDoms() { return jobs; }

  public boolean visit(ParseTreeNode node) {
    if (!(node instanceof Operation)) { return true; }
    Operation op = (Operation) node;
    if (Operator.FUNCTION_CALL != op.getOperator()
        || 2 != op.children().size()) {
      return true;
    }
    Expression fn = op.children().get(0),
              arg = op.children().get(1);
    if (!(fn instanceof Reference
        && "compileGxp".equals(((Reference) fn).getIdentifier()))) {
      return true;
    }
    ParseTreeNode parent = op.getParent();
    if (!(parent instanceof ExpressionStmt)) { return true; }
    try {
      CharProducer cp = stringExpressionAsCharProducer(arg);
      HtmlLexer lexer = new HtmlLexer(cp);
      lexer.setTreatedAsXml(true);
      TokenQueue<HtmlTokenType> tq = new TokenQueue<HtmlTokenType>(
          lexer, node.getFilePosition().source(),
          Criterion.Factory.<Token<HtmlTokenType>>optimist());
      DomTree doc = DomParser.parseDocument(tq);
      tq.expectEmpty();
      if (!(doc instanceof DomTree.Tag)) {
        throw new ParseException(new Message(
            PluginMessageType.CANT_CONVERT_TO_GXP, arg.getFilePosition(), arg));
      }
      jobs.add(new GxpJob((DomTree.Tag) doc, (MutableParseTreeNode) parent));
    } catch (ParseException ex) {
      ex.toMessageQueue(mq);
    }

    return false;
  }

  private static CharProducer stringExpressionAsCharProducer(Expression e)
      throws ParseException {
    List<CharProducer> chunks = new ArrayList<CharProducer>();
    stringExpressionAsCharProducer(e, chunks);
    return chunks.size() == 1
           ? chunks.get(0)
           : CharProducer.Factory.chain(chunks.toArray(new CharProducer[0]));
  }
  private static void stringExpressionAsCharProducer(
      Expression e, List<CharProducer> chunks) throws ParseException {
    if (e instanceof StringLiteral) {
      String literal = ((StringLiteral) e).getValue();
      int n = literal.length();
      if (n >= 2) {
        char ch0 = literal.charAt(0);
        if (('\'' == ch0 || '\"' == ch0) && literal.charAt(n - 1) == ch0) {
          literal = " " + literal.substring(1, n - 1) + " ";
        }
      }
      chunks.add(CharProducer.Factory.fromJsString(
          CharProducer.Factory.create(
              new StringReader(literal), e.getFilePosition())));
      return;
    } else if (e instanceof Operation) {
      Operation op = (Operation) e;
      if (Operator.ADDITION == op.getOperator()) {
        for (Expression operand : op.children()) {
          stringExpressionAsCharProducer(operand, chunks);
        }
        return;
      }
    }

    throw new ParseException(new Message(
        PluginMessageType.CANT_CONVERT_TO_GXP, e.getFilePosition(), e));
  }
}

final class GxpJob {
  final DomTree.Tag docRoot;
  final ParseTreeNode toReplace;
  GxpCompiler.TemplateSignature sig;
  FunctionConstructor compiled;

  GxpJob(DomTree.Tag docRoot, ParseTreeNode toReplace) {
    assert null != docRoot;
    this.docRoot = docRoot;
    this.toReplace = toReplace;
  }
}

final class GlobalDefRewriter implements Visitor {
  final PluginMeta meta;

  GlobalDefRewriter(PluginMeta meta) {
    this.meta = meta;
  }

  public boolean visit(ParseTreeNode n) {
    if (n instanceof FunctionConstructor) { return false; }
    if (n instanceof MultiDeclaration) {
      // Replace with a block.  Then recurse so that the declarations will get
      // converted to assignments
      MutableParseTreeNode parent = (MutableParseTreeNode) n.getParent();
      MultiDeclaration multi = (MultiDeclaration) n;
      List<Declaration> decls = new ArrayList<Declaration>(multi.children());
      while (!multi.children().isEmpty()) {
        multi.removeChild(multi.children().get(0));
      }
      Block block = s(new Block(decls));
      block.setFilePosition(multi.getFilePosition());
      parent.replaceChild(block, multi);
      block.acceptPreOrder(this);
      return false;
    } else if (n instanceof Declaration) {
      MutableParseTreeNode parent = (MutableParseTreeNode) n.getParent();
      if (parent instanceof CatchStmt && null == n.getPrevSibling()) {
        // Do not move the exception declaration in a catch block
        return false;
      }
      Declaration d = (Declaration) n;
      Expression initializer = d.getInitializer();
      if (null == initializer) {
        UndefinedLiteral placeholder = s(new UndefinedLiteral());
        placeholder.setFilePosition(FilePosition.endOf(d.getFilePosition()));
        initializer = placeholder;
      } else if (d instanceof FunctionDeclaration) {
        FunctionConstructor placeholder = new FunctionConstructor(
            null, Collections.<FormalParam>emptyList(),
            new Block(Collections.<Statement>emptyList()));
        d.replaceChild(placeholder, initializer);
      } else {
        d.removeChild(initializer);
      }

      ExpressionStmt rewritten = s(
          new ExpressionStmt(
              s(new Operation(
                    Operator.ASSIGN,
                    s(new Operation(
                        Operator.MEMBER_ACCESS,
                        s(new Reference(meta.namespaceName)),
                        s(new Reference(d.getIdentifier())))),
                    initializer))));
      rewritten.setFilePosition(d.getFilePosition());
      parent.replaceChild(rewritten, d);
    }
    return true;
  }

  /** Make the given parse tree node synthetic. */
  private static <T extends ParseTreeNode> T s(T t) {
    t.getAttributes().set(ExpressionSanitizer.SYNTHETIC, Boolean.TRUE);
    return t;
  }
}

final class GlobalReferenceRewriter {
  final PluginMeta meta;

  private static final Set<String> IMPLICIT_FUNCTION_DEFINITIONS =
    new HashSet<String>(Arrays.asList("arguments", Keyword.THIS.toString()));

  GlobalReferenceRewriter(PluginMeta meta) { this.meta = meta; }

  void rewrite(ParseTreeNode node, final Set<? extends String> locals) {
    node.acceptPreOrder(new Visitor() {
      public boolean visit(ParseTreeNode node) {
        // If we see a function constructor, we need to compute a new set of
        // local declarations and recurse
        if (node instanceof FunctionConstructor) {
          FunctionConstructor c = (FunctionConstructor) node;
          Set<String> fnLocals = new HashSet<String>(locals);
          fnLocals.addAll(IMPLICIT_FUNCTION_DEFINITIONS);
          LocalDeclarationInspector insp =
            new LocalDeclarationInspector(fnLocals);
          for (ParseTreeNode child : c.children()) {
            child.acceptPreOrder(insp);
          }
          rewrite(c.getBody(), fnLocals);
          return false;
        }

        if (node instanceof Reference) {
          Reference ref = (Reference) node;
          MutableParseTreeNode parent =
            (MutableParseTreeNode) node.getParent();
          // If node is part of a member access, and is not the leftmost
          // reference, then don't rewrite.  We don't want to rewrite the
          // b in a.b.

          // We also don't want to rewrite synthetic nodes -- nodes created by
          // the PluginCompiler..
          if (!locals.contains(ref.getIdentifier())
              && !ref.getAttributes().is(ExpressionSanitizer.SYNTHETIC)
              && !(null == ref.getNextSibling()
                   && parent instanceof Operation
                   && (Operator.MEMBER_ACCESS
                        == ((Operation) parent).getOperator()))) {

            Reference placeholder = new Reference("_");
            Operation pluginReference = s(
                new Operation(
                    Operator.MEMBER_ACCESS,
                    s(new Reference(meta.namespaceName)),
                    placeholder));
            parent.replaceChild(pluginReference, ref);
            pluginReference.replaceChild(ref, placeholder);
          }
        }
        return true;
      }
    });
  }

  static <T extends ParseTreeNode> T s(T n) {
    n.getAttributes().set(ExpressionSanitizer.SYNTHETIC, Boolean.TRUE);
    return n;
  }

  static final class LocalDeclarationInspector implements Visitor {
    final Set<String> locals;

    LocalDeclarationInspector(Set<String> locals) { this.locals = locals; }

    public boolean visit(ParseTreeNode node) {
      if (node instanceof FunctionConstructor) { return false; }
      if (node instanceof Declaration) {
        locals.add(((Declaration) node).getIdentifier());
      }
      return true;
    }
  }
}
