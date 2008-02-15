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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.Keyword;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.html.OpenElementStack;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.MultiDeclaration;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.UndefinedLiteral;
import com.google.caja.plugin.stages.CheckForErrorsStage;
import com.google.caja.plugin.stages.ConsolidateCodeStage;
import com.google.caja.plugin.stages.ConsolidateCssStage;
import com.google.caja.plugin.stages.RewriteGlobalReferencesStage;
import com.google.caja.plugin.stages.ValidateCssStage;
import com.google.caja.plugin.stages.ValidateJavascriptStage;
import com.google.caja.plugin.GxpCompiler.BadContentException;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Criterion;
import com.google.caja.util.Pair;
import com.google.caja.util.Pipeline;
import static com.google.caja.plugin.SyntheticNodes.s;

import java.io.StringReader;

import java.util.*;

/**
 * Compiles a bundle of css, javascript, and gxp files to a sandboxed javascript
 * and css widget.
 *
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public final class PluginCompiler {
  private final Pipeline<Jobs> compilationPipeline;
  private final Jobs jobs;
  /** An object that is not available to sandboxed code. */
  private ObjectConstructor pluginNamespace;
  /** An object that is available to sandboxed code. */
  private ObjectConstructor pluginPrivate;
  /** The root of the output javascript tree. */
  private Block jsTree;

  public PluginCompiler(PluginMeta meta) {
    this(meta, new SimpleMessageQueue());
  }

  public PluginCompiler(PluginMeta meta, MessageQueue mq) {
    MessageContext mc = new MessageContext();
    mc.inputSources = new ArrayList<InputSource>();
    jobs = new Jobs(mc, mq, meta);
    compilationPipeline = new Pipeline<Jobs>() {
      final long t0 = System.nanoTime();
      @Override
      protected boolean applyStage(
          Pipeline.Stage<? super Jobs> stage, Jobs jobs) {
        jobs.getMessageQueue().addMessage(
            MessageType.CHECKPOINT,
            MessagePart.Factory.valueOf(stage.getClass().getSimpleName()),
            MessagePart.Factory.valueOf((System.nanoTime() - t0) / 1e9));
        return super.applyStage(stage, jobs);
      }
    };
    setupCompilationPipeline();
  }

  public MessageQueue getMessageQueue() { return jobs.getMessageQueue(); }

  public MessageContext getMessageContext() { return jobs.getMessageContext(); }

  public void setMessageContext(MessageContext inputMessageContext) {
    assert null != inputMessageContext;
    if (inputMessageContext.inputSources.isEmpty()) {
      inputMessageContext.inputSources = new ArrayList<InputSource>();
    }
    jobs.setMessageContext(inputMessageContext);
  }

  public PluginMeta getPluginMeta() { return jobs.getPluginMeta(); }

  public void addInput(AncestorChain<?> input) {
    jobs.getJobs().add(new Job(input));
    jobs.getMessageContext().inputSources.add(
        input.node.getFilePosition().source());
  }

  /**
   * The list of parse trees that comprise the plugin after run has been called.
   * Valid after run has been called.
   */
  public List<? extends ParseTreeNode> getOutputs() {
    List<ParseTreeNode> outputs = new ArrayList<ParseTreeNode>();
    for (Job job : jobs.getJobs()) {
      switch (job.getType()) {
        case GXP: case CSS_TEMPLATE:
          // Have been rolled into the plugin namespace
          break;
        case JAVASCRIPT: case CSS:
          outputs.add(job.getRoot().node);
          break;
        default:
          throw new AssertionError();
      }
    }
    return outputs;
  }

  public Block getJavascript() {
    List<Job> jsJobs = jobs.getJobsByType(Job.JobType.JAVASCRIPT);
    if (jsJobs.isEmpty()) { return null; }
    assert jsJobs.size() == 1;
    return jsJobs.get(0).getRoot().cast(Block.class).node;
  }

  protected void setupCompilationPipeline() {
    List<Pipeline.Stage<Jobs>> stages = compilationPipeline.getStages();
    PluginMeta.TranslationScheme scheme = jobs.getPluginMeta().scheme;
    if (scheme != PluginMeta.TranslationScheme.CAJA) {
      stages.add(new SetUpNamespaceStage());
    }
    stages.add(new ValidateCssStage());
    stages.add(new CompileGxpsStage());
    stages.add(new CompileCssTemplatesStage());
    if (jobs.getPluginMeta().scheme != PluginMeta.TranslationScheme.CAJA) {
      stages.add(new MoveGlobalDefinitionsIntoPluginNamespaceStage());
      stages.add(new ConsolidateCodeIntoInitializerBodyStage());
      stages.add(new RewriteGlobalReferencesStage());
    } else {
      stages.add(new ConsolidateCodeStage());
    }
    stages.add(new ValidateJavascriptStage());
    stages.add(new ConsolidateCssStage());
    stages.add(new CheckForErrorsStage());
  }

  /**
   * Run the compiler on all parse trees added via {@link #addInput}.
   * The output parse trees are available via {@link #getOutputs()}.
   * @return true on success, false on failure.
   */
  public boolean run() {
    return compilationPipeline.apply(jobs);
  }

  private class SetUpNamespaceStage implements Pipeline.Stage<Jobs> {
    public boolean apply(Jobs jobs) {
      PluginMeta meta = getPluginMeta();

      PluginCompiler.this.pluginNamespace = s(
          new ObjectConstructor(
              Collections.<Pair<Literal, Expression>>emptyList()));
      // Create a meta object that contains the various prefixes and that is not
      // reachable from the plugin
      PluginCompiler.this.pluginPrivate = s(
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
      PluginCompiler.this.jsTree
          = s(new Block(Arrays.<Statement>asList(
                // The private plugin
                s(new Declaration(
                      s(new Identifier(meta.namespacePrivateName)),
                      pluginPrivate)),
                // The plugin namespace
                s(new Declaration(
                      s(new Identifier(meta.namespaceName)), pluginNamespace)),
                s(new ExpressionStmt(
                      s(new Operation(
                            Operator.ASSIGN,
                            s(new Operation(
                                  Operator.MEMBER_ACCESS,
                                  s(new Reference(
                                        s(new Identifier(
                                              meta.namespacePrivateName)))),
                                  s(new Reference(
                                        s(new Identifier("plugin")))))),
                            s(new Reference(
                                  s(new Identifier(meta.namespaceName))))
                                )))),
                            s(new ExpressionStmt(s(new Operation(
                                  Operator.FUNCTION_CALL,
                                  s(new Reference(
                                        s(new Identifier(
                                              "plugin_initialize___")))),
                                  s(new Reference(
                                        s(new Identifier(
                                              meta.namespacePrivateName))))))))
                )));

      return hasNoFatalErrors();
    }
  }

  private class CompileGxpsStage implements Pipeline.Stage<Jobs> {
    public boolean apply(Jobs jobs) {
      MessageQueue mq = getMessageQueue();
      PluginMeta meta = getPluginMeta();

      List<GxpJob> gxpJobs = new ArrayList<GxpJob>();
      for (Iterator<Job> jobIt = jobs.getJobs().iterator(); jobIt.hasNext();) {
        Job job = jobIt.next();
        switch (job.getType()) {
          case JAVASCRIPT:
            GxpCompileDirectiveReplacer r = new GxpCompileDirectiveReplacer(mq);
            job.getRoot().node.acceptPreOrder(r, job.getRoot().parent);
            gxpJobs.addAll(r.getDoms());
            break;
          case GXP:
            gxpJobs.add(new GxpJob((DomTree.Tag) job.getRoot().node, null));
            jobIt.remove();
            break;
        }
      }
      GxpCompiler gxpc = new GxpCompiler(mq, meta);
      GxpValidator v = new GxpValidator(mq);
      for (Iterator<GxpJob> it = gxpJobs.iterator(); it.hasNext();) {
        GxpJob job = it.next();
        if (!v.validate(new AncestorChain<DomTree>(job.docRoot))) {
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
      for (Iterator<GxpJob> it = gxpJobs.iterator(); it.hasNext();) {
        GxpJob job = it.next();
        try {
          job.compiled = gxpc.compileDocument(job.sig);
        } catch (GxpCompiler.BadContentException ex) {
          ex.printStackTrace();
          ex.toMessageQueue(mq);
          it.remove();
          continue;
        }

        if (jobs.getPluginMeta().scheme != PluginMeta.TranslationScheme.CAJA) {
          // Create a node under PluginMeta for the canonical reference.
          // This is used  for calls from one gxp to another to foil attacks
          // that rewrite a called gxp to emit unsafe content.
          PluginCompiler.this.pluginPrivate.createMutation()
              .insertBefore(s(new StringLiteral(
                  StringLiteral.toQuotedValue(job.sig.assignedName))), null)
              .insertBefore(job.compiled, null)
              .execute();

          Expression templateRef = s(
              new Operation(
                  Operator.MEMBER_ACCESS,
                  s(new Reference(
                        s(new Identifier(meta.namespacePrivateName)))),
                  s(new Reference(
                        s(new Identifier(job.sig.getAssignedName()))))
                  ));

          // Either replace it or put a reference to it in the main plugin
          if (null == job.toReplace) {
            StringLiteral gxpName = s(
                new StringLiteral(StringLiteral.toQuotedValue(
                    job.sig.templateName)));
            PluginCompiler.this.pluginNamespace.createMutation()
                .insertBefore(gxpName, null)
                .insertBefore(templateRef, null)
               .execute();
          } else {
            ((MutableParseTreeNode) job.toReplace.parent.node).replaceChild(
                templateRef, job.toReplace.node);
          }
        } else {
          FunctionConstructor templateFn = (FunctionConstructor) job.compiled;
          FunctionDeclaration template = new FunctionDeclaration(
              templateFn.getIdentifier(), templateFn);
          jobs.getJobs().add(
              new Job(new AncestorChain<FunctionDeclaration>(template)));
        }
      }

      for (FunctionDeclaration handler : gxpc.getEventHandlers()) {
        if (jobs.getPluginMeta().scheme != PluginMeta.TranslationScheme.CAJA) {
          StringLiteral gxpName = s(
              new StringLiteral(StringLiteral.toQuotedValue(
                  handler.getIdentifierName())));
          Expression function = handler.getInitializer();
          PluginCompiler.this.pluginNamespace.createMutation()
              .insertBefore(gxpName, null)
              .insertBefore(function, null)
              .execute();
        } else {
          jobs.getJobs().add(
              new Job(new AncestorChain<FunctionDeclaration>(handler)));
        }
      }

      return hasNoFatalErrors();
    }
  }

  // TODO(mikesamuel): move these into their own files.
  /** Takes CSS templates and turns them into functions. */
  private class CompileCssTemplatesStage implements Pipeline.Stage<Jobs> {
    public boolean apply(Jobs jobs) {
      for (Job job : jobs.getJobsByType(Job.JobType.CSS_TEMPLATE)) {
        CssTemplate t = (CssTemplate) job.getRoot().node;
        FunctionConstructor function;
        try {
          function = t.toJavascript(getPluginMeta(), getMessageQueue());
        } catch (BadContentException ex) {
          ex.toMessageQueue(getMessageQueue());
          continue;
        }

        StringLiteral templateName = s(new StringLiteral(
            StringLiteral.toQuotedValue(function.getIdentifier().getName())));
        PluginCompiler.this.pluginNamespace.createMutation()
            .insertBefore(templateName, null)
            .insertBefore(function, null)
            .execute();
      }
      return hasNoFatalErrors();
    }
  }

  private class MoveGlobalDefinitionsIntoPluginNamespaceStage
      implements Pipeline.Stage<Jobs> {
    public boolean apply(Jobs jobs) {
      GlobalDefRewriter rw = new GlobalDefRewriter(getPluginMeta());

      for (Job job : jobs.getJobsByType(Job.JobType.JAVASCRIPT)) {
        job.getRoot().node.acceptPreOrder(rw, job.getRoot().parent);
      }

      return hasNoFatalErrors();
    }
  }

  private class ConsolidateCodeIntoInitializerBodyStage
      implements Pipeline.Stage<Jobs> {
    public boolean apply(Jobs jobs) {
      PluginMeta meta = getPluginMeta();

      // Create an initializer function
      Block initFunctionBody = s(new Block(Collections.<Statement>emptyList()));

      MutableParseTreeNode.Mutation newChanges
          = initFunctionBody.createMutation();
      for (Iterator<Job> jobIt = jobs.getJobs().iterator(); jobIt.hasNext();) {
        Job job = jobIt.next();
        if (job.getType() != Job.JobType.JAVASCRIPT) { continue; }
        jobIt.remove();

        Block body = job.getRoot().cast(Block.class).node;
        MutableParseTreeNode.Mutation oldChanges = body.createMutation();
        for (Statement s : body.children()) {
          oldChanges.removeChild(s);
          newChanges.insertBefore(s, null);
        }
        oldChanges.execute();
      }
      newChanges.execute();

      // (function () { <initializer code> }).call(<plugin namespace);
      // call
      //   member access
      //     function constructor
      //       initializer body
      //     reference: call
      //   reference: plugin_namespace
      PluginCompiler.this.jsTree.insertBefore(
          s(new ExpressionStmt(
              s(new Operation(
                    Operator.FUNCTION_CALL,
                    s(new Operation(
                          Operator.MEMBER_ACCESS,
                          s(new FunctionConstructor(
                                s(new Identifier(null)),
                                Collections.<FormalParam>emptyList(),
                                initFunctionBody)),
                          s(new Reference(s(new Identifier("call"))))
                          )),
                    s(new Reference(s(new Identifier(meta.namespaceName))))
                    ))
              )),
          null);

      jobs.getJobs().add(new Job(new AncestorChain<Block>(jsTree)));

      return hasNoFatalErrors();
    }
  }


  private boolean hasNoFatalErrors() {
    for (Message m : getMessageQueue().getMessages()) {
      if (MessageLevel.FATAL_ERROR.compareTo(m.getMessageLevel()) <= 0) {
        return false;
      }
    }
    return true;
  }
}

final class GxpCompileDirectiveReplacer implements Visitor {
  private final List<GxpJob> jobs = new ArrayList<GxpJob>();
  private final MessageQueue mq;

  GxpCompileDirectiveReplacer(MessageQueue mq) {
    this.mq = mq;
  }

  List<GxpJob> getDoms() { return jobs; }

  public boolean visit(AncestorChain<?> ancestors) {
    ParseTreeNode node = ancestors.node;
    if (!(node instanceof Operation)) { return true; }
    Operation op = (Operation) node;
    if (Operator.FUNCTION_CALL != op.getOperator()
        || 2 != op.children().size()) {
      return true;
    }
    Expression fn = op.children().get(0),
              arg = op.children().get(1);
    if (!(fn instanceof Reference
          && "compileGxp".equals(((Reference) fn).getIdentifierName()))) {
      return true;
    }
    ParseTreeNode parent = ancestors.getParentNode();
    if (!(parent instanceof ExpressionStmt)) { return true; }
    try {
      CharProducer cp = stringExpressionAsCharProducer(arg);
      HtmlLexer lexer = new HtmlLexer(cp);
      lexer.setTreatedAsXml(true);
      TokenQueue<HtmlTokenType> tq = new TokenQueue<HtmlTokenType>(
          lexer, node.getFilePosition().source(),
          Criterion.Factory.<Token<HtmlTokenType>>optimist());
      DomTree doc = DomParser.parseDocument(
          tq, OpenElementStack.Factory.createXmlElementStack());
      tq.expectEmpty();
      if (!(doc instanceof DomTree.Tag)) {
        throw new ParseException(new Message(
            PluginMessageType.CANT_CONVERT_TO_GXP, arg.getFilePosition(), arg));
      }
      jobs.add(new GxpJob((DomTree.Tag) doc, ancestors.parent));
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
  final AncestorChain<?> toReplace;
  GxpCompiler.TemplateSignature sig;
  FunctionConstructor compiled;

  GxpJob(DomTree.Tag docRoot, AncestorChain<?> toReplace) {
    assert null != docRoot;
    this.docRoot = docRoot;
    this.toReplace = toReplace;
  }

  @Override
  public String toString() {
    return "[GxpJob " + (sig != null ? sig.templateName : "<uncompiled>") + "]";
  }
}

final class GlobalDefRewriter implements Visitor {
  final PluginMeta meta;

  GlobalDefRewriter(PluginMeta meta) {
    this.meta = meta;
  }

  public boolean visit(AncestorChain<?> ancestors) {
    ParseTreeNode n = ancestors.node;
    if (n instanceof FunctionConstructor) { return false; }
    if (n instanceof MultiDeclaration) {
      // Replace with a block.  Then recurse so that the declarations will get
      // converted to assignments
      MutableParseTreeNode parent
          = (MutableParseTreeNode) ancestors.getParentNode();
      MultiDeclaration multi = (MultiDeclaration) n;
      List<Declaration> decls = new ArrayList<Declaration>(multi.children());
      while (!multi.children().isEmpty()) {
        multi.removeChild(multi.children().get(0));
      }
      Block block = s(new Block(decls));
      block.setFilePosition(multi.getFilePosition());
      parent.replaceChild(block, multi);
      block.acceptPreOrder(this, ancestors);
      return false;
    } else if (n instanceof Declaration) {
      MutableParseTreeNode parent
          = (MutableParseTreeNode) ancestors.getParentNode();
      if (parent instanceof CatchStmt && parent.children().get(0) == n) {
        // Do not move the exception declaration in a catch block
        return false;
      }
      Declaration d = (Declaration) n;
      Expression initializer = d.getInitializer();
      if (null == initializer) {
        UndefinedLiteral placeholder = s(new UndefinedLiteral());
        placeholder.setFilePosition(FilePosition.endOf(d.getFilePosition()));
        initializer = placeholder;
      }

      ExpressionStmt rewritten = s(
          new ExpressionStmt(
              s(new Operation(
                    Operator.ASSIGN,
                    s(new Operation(
                        Operator.MEMBER_ACCESS,
                        s(new Reference(s(new Identifier(meta.namespaceName)))),
                        s(new Reference(
                              s(new Identifier(d.getIdentifierName())))))),
                    initializer))));
      rewritten.setFilePosition(d.getFilePosition());
      parent.replaceChild(rewritten, d);
    }
    return true;
  }
}

final class GlobalReferenceRewriter {
  final PluginMeta meta;

  private static final Set<String> IMPLICIT_FUNCTION_DEFINITIONS =
    new HashSet<String>(Arrays.asList("arguments", Keyword.THIS.toString()));

  GlobalReferenceRewriter(PluginMeta meta) { this.meta = meta; }

  void rewrite(ParseTreeNode node, final Set<? extends String> locals) {
    node.acceptPreOrder(new Visitor() {
      public boolean visit(AncestorChain<?> ancestors) {
        ParseTreeNode node = ancestors.node;
        // If we see a function constructor, we need to compute a new set of
        // local declarations and recurse
        if (node instanceof FunctionConstructor) {
          FunctionConstructor c = (FunctionConstructor) node;
          Set<String> fnLocals = new HashSet<String>(locals);
          fnLocals.addAll(IMPLICIT_FUNCTION_DEFINITIONS);
          LocalDeclarationInspector insp
              = new LocalDeclarationInspector(fnLocals);
          for (ParseTreeNode child : c.children()) {
            child.acceptPreOrder(insp, ancestors);
          }
          rewrite(c.getBody(), fnLocals);
          return false;
        }

        if (node instanceof Reference) {
          Reference ref = (Reference) node;
          MutableParseTreeNode parent
              = (MutableParseTreeNode) ancestors.getParentNode();
          // If node is part of a member access, and is not the leftmost
          // reference, then don't rewrite.  We don't want to rewrite the
          // b in a.b.

          // We also don't want to rewrite synthetic nodes -- nodes created by
          // the PluginCompiler.
          List<? extends ParseTreeNode> siblings = parent.children();
          if (!locals.contains(ref.getIdentifierName())
              && !ref.getAttributes().is(SyntheticNodes.SYNTHETIC)
              && !(parent instanceof Operation
                   && (Operator.MEMBER_ACCESS
                       == ((Operation) parent).getOperator())
                   && siblings.size() - 1 == siblings.lastIndexOf(ref))) {

            Operation pluginReference = s(
                new Operation(
                    Operator.MEMBER_ACCESS,
                    s(new Reference(s(new Identifier(meta.namespaceName)))),
                    ref));
            parent.replaceChild(pluginReference, ref);
          }
        }
        return true;
      }
    }, null);
  }

  static final class LocalDeclarationInspector implements Visitor {
    final Set<String> locals;

    LocalDeclarationInspector(Set<String> locals) { this.locals = locals; }

    public boolean visit(AncestorChain<?> ancestors) {
      ParseTreeNode node = ancestors.node;
      if (node instanceof FunctionConstructor) { return false; }
      if (node instanceof Declaration) {
        locals.add(((Declaration) node).getIdentifierName());
      }
      return true;
    }
  }
}
