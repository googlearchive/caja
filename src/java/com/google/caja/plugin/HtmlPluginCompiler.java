// Copyright (C) 2007 Google Inc.
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

import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.UndefinedLiteral;
import com.google.caja.parser.js.MultiDeclaration;
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.html.HtmlDomParser;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.css.CssParser;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.RenderContext;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.StringInputSource;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.StringFilePosition;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.Keyword;
import com.google.caja.util.Criterion;
import com.google.caja.util.Pair;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Takes arbitrary html and js as a String, compiles and rewrites it, and
 * ends up with Strings holding rewritten js, rewritten css, and error messages.
 *
 * @author rdub@google.com (Ryan Williams)
 */
public final class HtmlPluginCompiler {

  private MessageQueue mq;
  private MessageContext mc;
  private final PluginMeta meta;
  private String pluginSpec;
  // TODO(ihab): Delete this field since HtmlPluginCompiler only called once.
  private boolean parsed;
  private final List<Input> inputs = new ArrayList<Input>();
  private Block jsTree;
  private CssTree cssTree;
  // TODO(ihab): Remove stored string representations; just keep trees.
  private String outputJs;
  private String outputCss;
  /** An object that is not available to sandboxed code. */
  private ObjectConstructor pluginNamespace;
  /** An object that is available to sandboxed code. */
  private ObjectConstructor pluginPrivate;

  public HtmlPluginCompiler(String pluginSpec, String nsName, String nsPrefix,
      String rootDivId, boolean isBaja) {
    this(pluginSpec, new PluginMeta(nsName, nsPrefix, rootDivId, isBaja));
  }

  public HtmlPluginCompiler(String pluginSpec, PluginMeta meta) {
    this.mq = new SimpleMessageQueue();
    this.mc = new MessageContext();
    this.mc.inputSources = new ArrayList<InputSource>();
    this.meta = meta;
    this.pluginSpec = pluginSpec;
    this.parsed = false;
  }

  public HtmlPluginCompiler(PluginMeta meta) {
    this("", meta);
  }
  public HtmlPluginCompiler(String nsName, String nsPrefix,
      String rootDivId, boolean isBaja) {
    this("", new PluginMeta(nsName, nsPrefix, rootDivId, isBaja));
  }

  public String getPluginSpec() {
    return pluginSpec;
  }

  public void setPluginSpec(String pluginSpec) {
    this.pluginSpec = pluginSpec;
    this.parsed = false;
  }

  public String getOutputJs() {
    return outputJs;
  }
  public String getOutputCss() {
    return outputCss;
  }

  public MessageQueue getMessageQueue() { return mq; }

  public void setMessageQueue(MessageQueue inputMessageQueue) {
    assert null != inputMessageQueue;
    this.mq = inputMessageQueue;
  }

  public PluginMeta getPluginMeta() { return meta; }

  public void addInput(ParseTreeNode input) {
    if (input == null) {
      return;
    }
    inputs.add(new Input(input));
    this.parsed = false;
  }

  public List<? extends ParseTreeNode> getInputs() {
    ParseTreeNode[] inputsCopy = new ParseTreeNode[this.inputs.size()];
    for (int i = 0; i < inputsCopy.length; ++i) {
      inputsCopy[i] = this.inputs.get(i).parsetree;
    }
    return Arrays.asList(inputsCopy);
  }

  /**
   * the list of parse trees that comprise the plugin after run has been called.
   * Valid after run has been called.
   */
  public List<? extends ParseTreeNode> getOutputs() {
    assert null != pluginNamespace;
    List<ParseTreeNode> outputs = new ArrayList<ParseTreeNode>();
    outputs.add(getJavascript());
    outputs.add(getCss());
    return outputs;
  }

  public Block getJavascript() { return this.jsTree; }
  public CssTree getCss() { return this.cssTree; }

  public boolean parseInput(String inputPluginSpec) throws ParseException {
    setPluginSpec(inputPluginSpec);
    return parseSpec();
  }

  private boolean parseSpec() throws ParseException {
    // HACK: need to synthesize a top-level node here.
    // would we want to make this encompassing div have the correct 'id' and
    // 'class' attributes so that we could just put the output html on the page
    // and it will work, rather than relying on the existence of a specially
    // crafted <div> already being there?
    // Also, if for some reason the plugin spec has lots of code on the first
    // line, the debugging char info will be off by 6 characters.
    String syntheticPluginSpec = "<div> " + pluginSpec + "</div>";
    StringInputSource is = new StringInputSource(syntheticPluginSpec);
    CharProducer cp = CharProducer.Factory.create(
        new StringReader(syntheticPluginSpec), is);
    ParseTreeNode input = parseHtml(is, cp);
    addInput(input);
    this.parsed = true;
    return input != null;
  }

  public ParseTreeNode parseHtml(StringInputSource is, CharProducer cp)
  throws ParseException {
    HtmlLexer lexer = new HtmlLexer(cp);
    lexer.setTreatedAsXml(true);
    TokenQueue<HtmlTokenType> tq = new TokenQueue<HtmlTokenType>(
        lexer, is, Criterion.Factory.<Token<HtmlTokenType>>optimist());
    return HtmlDomParser.parseDocument(tq, this);
  }

  public ParseTreeNode parseJsString(String script, StringFilePosition pos)
      throws GxpCompiler.BadContentException {
    if (script.trim().equals("")) {
      return null;
    }
    CharProducer cp = CharProducer.Factory.create(
        new StringReader(script), pos);
    ParseTreeNode scriptAsParsedJs;
    try {
      scriptAsParsedJs = parseJs(pos.source(), cp, this.mq);
    } catch (ParseException e) {
      throw new GxpCompiler.BadContentException(
          new Message(MessageType.IO_ERROR,
                      MessagePart.Factory.valueOf("script doesn't parse")));
    }
    return scriptAsParsedJs;
  }

  public ParseTreeNode parseJs(
      StringInputSource is, CharProducer cp, MessageQueue localMessageQueue)
      throws ParseException {
    JsLexer lexer = new JsLexer(cp);
    JsTokenQueue tq = new JsTokenQueue(lexer, is);
    Parser p = new Parser(tq, localMessageQueue);
    Block body = p.parse();
    body.parentify();
    tq.expectEmpty();
    return body;
  }

  public ParseTreeNode parseCssString(String style, StringFilePosition pos)
      throws GxpCompiler.BadContentException {
    if (style.trim().equals("")) {
      return null;
    }
    CharProducer cp = CharProducer.Factory.create(new StringReader(style), pos);
    ParseTreeNode styleAsParsedCss;
    try {
      styleAsParsedCss = parseCss(pos.source(), cp);
    } catch (ParseException e) {
      throw new GxpCompiler.BadContentException(
          new Message(MessageType.IO_ERROR,
                      MessagePart.Factory.valueOf("style doesn't parse")));
    }
    return styleAsParsedCss;
  }

  public ParseTreeNode parseCss(StringInputSource is, CharProducer cp)
      throws ParseException {
    CssLexer lexer = new CssLexer(cp);
    ParseTreeNode input;
    TokenQueue<CssTokenType> tq = new TokenQueue<CssTokenType>(
        lexer, is, new Criterion<Token<CssTokenType>>() {
          public boolean accept(Token<CssTokenType> tok) {
            return tok.type != CssTokenType.COMMENT
            && tok.type != CssTokenType.SPACE;
          }
        });

    CssParser p = new CssParser(tq);
    input = p.parseStyleSheet();
    tq.expectEmpty();
    return input;
  }

  /**
   * run the compiler on all parse trees added via {@link #addInput}.
   * THe output parse trees are available via {@link #getOutputs()}.
   * @return true on success, false on failure.
   */
  public boolean run() throws ParseException {
    if (!this.parsed) {
      parseSpec();
    }
    return (// first we create an object to hold the plugin
            setUpNamespace()
            // then we make sure the css is well formed and we prefix all rules
            // so that they don't affect nodes outside the plugin.
            && validateCss()
            // Now we compile the html and add a method to the plugin for the
            // html and one for each event handler.
            && compileHtml()
            // replace global definitions with operations that modify the
            // plugin.
            && moveGlobalDefinitionsIntoPluginNamespace()
            // put all the top level javascript code into an initializer block
            // that will set up the plugin.
            && consolidateCodeIntoInitializerBody()
            // replace references to globals with plugin accesses.
            // The handling of global declarations happens before consolidation
            // so that we know what is global.  This happens after consolidation
            // since it needs to affect references inside code compiled from
            // html.
            && rewriteGlobalReference()
            // rewrite the javascript to prevent runtime sandbox violations
            && validateJavascript()
            // consolidate different CSS inputs from multiple <style> tags into
            // one awesome CssTree that can be outted to a .css file.
            && consolidateCss()
            && hasNoFatalErrors()
            // store the raw output JS and CSS in Strings.
            && renderOutputs()
            );
  }

  private boolean setUpNamespace() {
    this.pluginNamespace = s(
        new ObjectConstructor(
            Collections.<Pair<Literal, Expression>>emptyList()));
    // create a meta object that contains the various prefixes and that is not
    // reachable from the plugin
    this.pluginPrivate = s(
        new ObjectConstructor(
            Collections.<Pair<Literal, Expression>>emptyList()));

    setupPluginPrivate();

    setupJsTree();

    return hasNoFatalErrors();
  }

  // This will look something like
  // var MyPluginMeta = {
  //   'nsPrefix':   'foo',
  //   'name':       'MyPlugin',
  //   'rootId':     'root',
  // };
  private void setupPluginPrivate() {
    pluginPrivate.createMutation()
        .insertBefore(s(new StringLiteral("'nsPrefix'")), null)
        .insertBefore(s(new StringLiteral(
            StringLiteral.toQuotedValue(meta.namespacePrefix))), null)
        .insertBefore(s(new StringLiteral("'name'")), null)
        .insertBefore(s(new StringLiteral(
            StringLiteral.toQuotedValue(meta.namespaceName))), null)
        .insertBefore(s(new StringLiteral("'rootId'")), null)
        .insertBefore(s(new StringLiteral(
            StringLiteral.toQuotedValue(meta.rootDomId))), null)
        .execute();
  }

  // create a tree that contains the declarations for the two and some
  // initialization code.
  // var PLUGIN_PRIVATE = { ... };
  // var PLUGIN = { .. };
  // PLUGIN_PRIVATE.plugin = PLUGIN;
  // plugin_initialize___(PLUGIN);
  private void setupJsTree() {
    if (meta.isBaja) {
      this.jsTree = s(new Block(Collections.<Statement>emptyList()));
    } else {
      this.jsTree = s(new Block(
                        Arrays.<Statement>asList(
                            // the private plugin
                            s(new Declaration(
                                meta.namespacePrivateName, pluginPrivate)),
                            // the plugin namespace
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
    }
    this.jsTree.parentify();
  }

  private boolean compileHtml() {
    List<HtmlJob> jobs = new ArrayList<HtmlJob>();
    for (Input input : inputs) {
      if (InputType.HTML == input.type) {
        jobs.add(new HtmlJob((DomTree.Tag) input.parsetree, null));
      }
    }
    if (jobs.size() != 1) {
      System.err.println("There should be exactly one html input, " +
          "instead seeing " + jobs.size());
      return false;
    }

    HtmlJob job = jobs.get(0);
    HtmlCompiler htmlc = new HtmlCompiler(mq, meta);
    HtmlValidator v = new HtmlValidator(mq);
    if (!v.validate(job.docRoot)) {
      System.err.println("Html invalid");
      return false;
    }

    job.sig = htmlc.compileTemplateSignature(job.docRoot);

    try {
      job.compiled = htmlc.compileDocument(job.sig);

      // create a node under PluginMeta for the canonical reference.
      // This is used for calls from one gxp to another to foil attacks
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

      // either replace it or put a reference to it in the main plugin
      if (null == job.toReplace) {
        StringLiteral htmlName = s(
            new StringLiteral(StringLiteral.toQuotedValue(
                                  job.sig.assignedName)));
        this.pluginNamespace.createMutation()
            .insertBefore(htmlName, null)
            .insertBefore(templateRef, null)
            .execute();
      } else {
        ((MutableParseTreeNode) job.toReplace.getParent()).replaceChild(
            templateRef, job.toReplace);
      }

      // (myPlugin.document).write(myPlugin.c1___());
      Block functionBody =
        s(new Block(
          Collections.singletonList(
            s(new ExpressionStmt(
              s(new Operation(
                Operator.FUNCTION_CALL,
                s(new Operation(
                  Operator.MEMBER_ACCESS,
                  s(new Operation(
                    Operator.MEMBER_ACCESS,
                    s(new Reference(meta.namespaceName)),
                    s(new Reference("document")))),
                  s(new Reference("write")))),
                s(new Operation(
                  Operator.FUNCTION_CALL,
                  s(new Operation(
                    Operator.MEMBER_ACCESS,
                    s(new Reference(meta.namespaceName)),
                    s(new Reference(job.sig.getAssignedName())))))))))))));

      // (function () {
      //    (myPlugin.document).write(myPlugin.c1___());
      // }).call(myPlugin);
      this.jsTree.insertBefore(
          s(new ExpressionStmt(
                s(new Operation(
                      Operator.FUNCTION_CALL,
                      s(new Operation(
                            Operator.MEMBER_ACCESS,
                            s(new FunctionConstructor(
                                  null, Collections.<FormalParam>emptyList(),
                                  functionBody)),
                            s(new Reference("call")))),
                      s(new Reference(meta.namespaceName)))))),
          null);

    } catch (GxpCompiler.BadContentException ex) {
      ex.toMessageQueue(mq);
    }

    for (FunctionDeclaration handler : htmlc.getEventHandlers()) {
      StringLiteral htmlName = s(
          new StringLiteral(StringLiteral.toQuotedValue(
              handler.getIdentifier())));
      Expression function = handler.getInitializer();
      handler.replaceChild(
          new FunctionConstructor(
              null,
              Collections.<FormalParam>emptyList(),
              new Block(Collections.<Statement>emptyList())),
          function);
      this.pluginNamespace.createMutation()
          .insertBefore(htmlName, null)
          .insertBefore(function, null)
          .execute();
    }

    boolean success = hasNoFatalErrors();
    System.out.println("compileHtmls: " + success);
    return success;
  }

  /**
   * sanitizes and namespace any css inputs.
   * @return true if the input css was safe.  False if any destructive
   *   modifications had to be made to make it safe, or if such modifivations
   *   were needed but could not be made.
   */
  private boolean validateCss() {
    // TODO(msamuel): build up a list of classes and ids for use in generating
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

    boolean success = valid && hasNoFatalErrors();
    System.out.println("validateCss: " + success);
    return success;
  }

  private boolean moveGlobalDefinitionsIntoPluginNamespace() {
    HtmlGlobalDefRewriter rw;
    if (meta.isBaja)
      rw = new HtmlGlobalDefRewriterBaja(meta);
    else
      rw = new HtmlGlobalDefRewriter(meta);

    for (Input input : inputs) {
      if (InputType.JAVASCRIPT == input.type) {
        Block body = (Block) input.parsetree;
        body.acceptPreOrder(rw);
      }
    }

    boolean success = hasNoFatalErrors();
    System.out.println("moveGlobalDefinitionsIntoPluginNamespace: " + success);
    return success;
  }

  private boolean consolidateCodeIntoInitializerBody() {
    // create an initializer function
    Block initFunctionBody = s(new Block(Collections.<Statement>emptyList()));

    MutableParseTreeNode.Mutation newChanges =
        initFunctionBody.createMutation();
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

    boolean success = hasNoFatalErrors();
    System.out.println("consolidateCodeIntoInitializerBody: " + success);
    return success;
  }

  private boolean rewriteGlobalReference() {
    new HtmlGlobalReferenceRewriter(meta, mq).rewrite(
        this.jsTree, Collections.<String>emptySet());

    boolean success = hasNoFatalErrors();
    System.out.println("rewriteGlobalReference: " + success);
    return success;
  }

  private boolean validateJavascript() {
    if (false) {
      StringBuffer out = new StringBuffer();
      MessageContext localMessageContext = new MessageContext();
      localMessageContext.relevantKeys =
          Collections.singleton(ExpressionSanitizer.SYNTHETIC);
      try {
        jsTree.formatTree(localMessageContext, 2, out);
      } catch (java.io.IOException ex) {
        throw new AssertionError(ex);
      }
      System.err.println("rw\n" + out + "\n\n");
    }

    boolean valid;
    if (meta.isBaja)
      valid = new ExpressionSanitizerBaja(mq).sanitize(this.jsTree);
    else
      valid = new ExpressionSanitizer(mq).sanitize(this.jsTree);
    boolean success = valid && hasNoFatalErrors();
    System.out.println("validateJavascript: " + success);
    return success;
  }

  private boolean consolidateCss() {
    ArrayList<CssTree.CssStatement> children =
        new ArrayList<CssTree.CssStatement>();
    for (Input input : inputs) {
      CssTree.StyleSheet styleSheet;
      if (InputType.CSS == input.type) {
        styleSheet = (CssTree.StyleSheet) input.parsetree;
      } else {
        continue;
      }
      for (CssTree child : styleSheet.children()) {
        children.add((CssTree.CssStatement) child);
      }
    }
    StringFilePosition pos = new StringFilePosition(
        new StringInputSource(pluginSpec));

    for (CssTree.CssStatement c : children) {
      ((MutableParseTreeNode)c.getParent()).removeChild(c);
    }

    cssTree = new CssTree.StyleSheet(pos, children);
    return true;
  }

  private boolean hasNoFatalErrors() {
    String errs = getErrors();
    if (errs.equals("")) {
      return true;
    }
    System.err.println(errs);
    return false;
  }

  public String getErrors() {
    String errs = "";
    for (Message m : mq.getMessages()) {
      if (MessageLevel.FATAL_ERROR.compareTo(m.getMessageLevel()) >= 0) {
        errs += "Error: " + m + "\n";
      }
    }
    return errs;
  }

  private boolean renderOutputs() {
    StringWriter cssOut = null, jsOut = null;
    boolean success = true;
    try {
      jsOut = new StringWriter();
      RenderContext rc = new RenderContext(mc, jsOut);
      jsTree.render(rc);
      rc.newLine();
      outputJs = jsOut.toString();

      cssOut = new StringWriter();
      rc = new RenderContext(mc, cssOut);
      cssTree.render(rc);
      rc.newLine();
      outputCss = cssOut.toString();
    } catch (IOException ex) {
      success = false;
      mq.addMessage(MessageType.IO_ERROR,
          MessagePart.Factory.valueOf("Output error: " + ex.toString()));
      ex.printStackTrace();
    } finally {
      try {
        if (null != cssOut) { cssOut.close(); }
      } catch (IOException ex) {
        success = false;
        mq.addMessage(MessageType.IO_ERROR,
            MessagePart.Factory.valueOf("Output error: " + ex.toString()));
        ex.printStackTrace();
      }
      try {
        if (null != jsOut) { jsOut.close(); }
      } catch (IOException ex) {
        success = false;
        mq.addMessage(MessageType.IO_ERROR,
            MessagePart.Factory.valueOf("Output error: " + ex.toString()));
        ex.printStackTrace();
      }
    }
    return success;
  }

  /** make the given parse tree node synthetic. */
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
        this.type = InputType.HTML;
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
    HTML
    ;
  }
}

final class HtmlGxpCompileDirectiveReplacer implements Visitor {
  private final List<HtmlGxpJob> jobs = new ArrayList<HtmlGxpJob>();
  private final MessageQueue mq;
  private final HtmlPluginCompiler c;

  HtmlGxpCompileDirectiveReplacer(MessageQueue mq, HtmlPluginCompiler c) {
    this.mq = mq;
    this.c = c;
  }

  List<HtmlGxpJob> getDoms() { return jobs; }

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
      DomTree doc = HtmlDomParser.parseDocument(tq, c);
      tq.expectEmpty();
      if (!(doc instanceof DomTree.Tag)) {
        throw new ParseException(new Message(
            PluginMessageType.CANT_CONVERT_TO_GXP, arg.getFilePosition(), arg));
      }
      jobs.add(new HtmlGxpJob((DomTree.Tag) doc, parent));
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

final class HtmlGxpJob {
  final DomTree.Tag docRoot;
  final ParseTreeNode toReplace;
  GxpCompiler.TemplateSignature sig;
  FunctionConstructor compiled;

  HtmlGxpJob(DomTree.Tag docRoot, ParseTreeNode toReplace) {
    assert null != docRoot;
    this.docRoot = docRoot;
    this.toReplace = toReplace;
  }
}

final class HtmlJob {
  final DomTree.Tag docRoot;
  final ParseTreeNode toReplace;
  HtmlCompiler.TemplateSignature sig;
  FunctionConstructor compiled;

  HtmlJob(DomTree.Tag docRoot, ParseTreeNode toReplace) {
    assert null != docRoot;
    this.docRoot = docRoot;
    this.toReplace = toReplace;
  }
}

class HtmlGlobalDefRewriter implements Visitor {
  final PluginMeta meta;

  HtmlGlobalDefRewriter(PluginMeta meta) { this.meta = meta; }

  public boolean visit(ParseTreeNode n) {
    if (n instanceof FunctionConstructor) { return false; }
    if (n instanceof MultiDeclaration) {
      // replace with a block.  Then recurse so that the declarations will get
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
        // do not move the exception declaration in a catch block
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

  /** make the given parse tree node synthetic. */
  private static <T extends ParseTreeNode> T s(T t) {
    t.getAttributes().set(ExpressionSanitizer.SYNTHETIC, Boolean.TRUE);
    return t;
  }

}

final class HtmlGlobalDefRewriterBaja extends HtmlGlobalDefRewriter {
  HtmlGlobalDefRewriterBaja(PluginMeta meta) { super(meta); }

  @Override
  public boolean visit(ParseTreeNode n) {
    return true;
  }
}

final class HtmlGlobalReferenceRewriter {
  final PluginMeta meta;
  final MessageQueue mq;

  private static final Set<String> IMPLICIT_FUNCTION_DEFINITIONS =
    new HashSet<String>(Arrays.asList("arguments", Keyword.THIS.toString()));

  HtmlGlobalReferenceRewriter(PluginMeta meta, MessageQueue mq) {
    this.meta = meta;
    this.mq = mq;
  }

  void rewrite(ParseTreeNode node, final Set<? extends String> locals) {
    node.acceptPreOrder(new Visitor() {
      public boolean visit(ParseTreeNode node) {
        // if we see a function constructor, we need to compute a new set of
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
          String refName = ref.getIdentifier();
          if (!node.getAttributes().is(ExpressionSanitizer.SYNTHETIC)) {
            if (refName.endsWith("__")) {
              mq.addMessage(MessageType.ILLEGAL_NAME, ref.getFilePosition(),
                  MessagePart.Factory.valueOf(refName));
            }
          }
          MutableParseTreeNode parent =
            (MutableParseTreeNode) node.getParent();
          Operator parentOp = null;
          if (parent instanceof Operation)
            parentOp = ((Operation)parent).getOperator();
          // If node is part of a member access, and is not the leftmost
          // reference, then don't rewrite.  We don't want to rewrite the
          // b in a.b. Nor do we want to rewrite the b in "b in a".

          // We also don't want to rewrite synthetic nodes -- nodes created by
          // the PluginCompiler..
          if (!locals.contains(ref.getIdentifier())
              && !ref.getAttributes().is(ExpressionSanitizer.SYNTHETIC)
              && !(null == ref.getNextSibling()
                   && Operator.MEMBER_ACCESS == parentOp)
              && !(null != ref.getNextSibling()
                   && Operator.IN == parentOp) ) {

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
