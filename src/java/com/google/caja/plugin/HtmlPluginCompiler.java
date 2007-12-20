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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.Keyword;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.Css2;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.js.Identifier;
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
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Criterion;
import com.google.caja.util.Pair;
import com.google.caja.util.SyntheticAttributeKey;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Takes arbitrary html and js as a String, compiles and rewrites it, and
 * ends up with Strings holding rewritten js, rewritten css, and error messages.
 *
 * @author rdub@google.com (Ryan Williams)
 */
public class HtmlPluginCompiler {

  /**
   * A synthetic attribute that stores the name of a function extracted from a
   * script tag.
   */
  static final SyntheticAttributeKey<String> SCRIPT_TAG_CALLOUT_NAME
      = new SyntheticAttributeKey<String>(String.class, "scriptTagCallout");

  private MessageQueue mq;
  private MessageContext mc;
  private final PluginMeta meta;
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

  public HtmlPluginCompiler(String nsName, String nsPrefix,
                            String rootDivId, PluginMeta.TranslationScheme scheme) {
    this(new PluginMeta(nsName, nsPrefix, rootDivId, scheme));
  }

  public HtmlPluginCompiler(PluginMeta meta) {
    // TODO(msamuel): pass in an input source with the pluginSpec so that
    // we can report error messages during parsing.
    this.mq = new SimpleMessageQueue();
    this.mc = new MessageContext();
    this.mc.inputSources = new ArrayList<InputSource>();
    this.meta = meta;
  }

  public String getOutputJs() {
    return outputJs;
  }
  public String getOutputCss() {
    return outputCss;
  }

  public MessageQueue getMessageQueue() { return mq; }

  public MessageContext getMessageContext() { return mc; }

  public void setMessageQueue(MessageQueue inputMessageQueue) {
    assert null != inputMessageQueue;
    this.mq = inputMessageQueue;
  }

  public PluginMeta getPluginMeta() { return meta; }

  public void addInput(AncestorChain<?> input) {
    if (input == null) {
      throw new NullPointerException();
    }
    inputs.add(new Input(input));
  }

  public List<? extends ParseTreeNode> getInputs() {
    ParseTreeNode[] inputsCopy = new ParseTreeNode[this.inputs.size()];
    for (int i = 0; i < inputsCopy.length; ++i) {
      inputsCopy[i] = this.inputs.get(i).parsetree.node;
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

  DomTree rewriteDomTree(DomTree t) {
    // Rewrite styles and scripts.
    // <script>foo()</script>  ->  <script>(cajoled foo)</script>
    // <style>foo { ... }</style>  ->  <style>foo { ... }</style>
    // <script src=foo.js></script>  ->  <script>(cajoled, inlined foo)</script>
    // <link rel=stylesheet href=foo.css>
    //     ->  <style>(cajoled, inlined styles)</style>
    Visitor domRewriter = new Visitor() {
        public boolean visit(AncestorChain<?> ancestors) {
          DomTree n = (DomTree) ancestors.node;
          if (n instanceof DomTree.Tag) {
            MutableParseTreeNode parentNode
                = (MutableParseTreeNode) ancestors.getParentNode();
            DomTree.Tag tag = (DomTree.Tag) n;
            String name = tag.getTagName().toLowerCase();
            if ("script".equals(name)) {
              rewriteScriptTag(tag, parentNode);
            } else if ("style".equals(name)) {
              rewriteStyleTag(tag, parentNode);
            } else if ("link".equals(name)) {
              rewriteLinkTag(tag, parentNode);
            }
          }
          return true;
        }
      };
    t.acceptPreOrder(domRewriter, null);
    return t;
  }

  private void rewriteScriptTag(
      DomTree.Tag scriptTag, MutableParseTreeNode parent) {
    DomTree.Attrib type = lookupAttribute(scriptTag, "type");
    DomTree.Attrib src = lookupAttribute(scriptTag, "src");
    if (type != null && !isJavaScriptContentType(type.getAttribValue())) {
      mq.addMessage(PluginMessageType.UNRECOGNIZED_CONTENT_TYPE,
                    type.getFilePosition(),
                    MessagePart.Factory.valueOf(type.getAttribValue()),
                    MessagePart.Factory.valueOf(scriptTag.getTagName()));
      parent.removeChild(scriptTag);
      return;
    }
    // Name of a function created from the script tag contents.
    String calloutName = getPluginMeta().generateUniqueName("scriptElement");
    // The script contents.
    CharProducer jsStream;
    if (src == null) {  // Parse the script tag body.
      jsStream = textNodesToCharProducer(scriptTag.children(), true);
      if (jsStream == null) {
        parent.removeChild(scriptTag);
        return;
      }
    } else {  // Load the src attribute
      URI srcUri;
      try {
        srcUri = new URI(src.getAttribValue());
      } catch (URISyntaxException ex) {
        mq.getMessages().add(
            new Message(PluginMessageType.MALFORMED_URL, MessageLevel.ERROR,
                        src.getFilePosition(), src));
        parent.removeChild(scriptTag);
        return;
      }

      // Fetch the script source.
      jsStream = loadExternalResource(
          new ExternalReference(
              srcUri, src.getAttribValueNode().getFilePosition()),
          "text/javascript");
      if (jsStream == null) {
        parent.removeChild(scriptTag);
        mq.addMessage(
            PluginMessageType.FAILED_TO_LOAD_EXTERNAL_URL,
            src.getFilePosition(), MessagePart.Factory.valueOf("" + srcUri));
        return;
      }
    }

    // Parse the body and create a function.
    Block parsedScriptBody;
    try {
      parsedScriptBody = parseJs(
          jsStream.getCurrentPosition().source(), jsStream, mq);
    } catch (ParseException ex) {
      ex.toMessageQueue(mq);
      parent.removeChild(scriptTag);
      return;
    }

    // function scriptBody_123__() { script_content...; }
    Block scriptDelegate
        = s(new Block(Collections.singletonList(
                s(new FunctionDeclaration(
                    s(new Identifier(calloutName)),
                    s(new FunctionConstructor(
                          s(new Identifier(calloutName)), Collections.<FormalParam>emptyList(),
                          parsedScriptBody)))))));
    scriptDelegate.setFilePosition(parsedScriptBody.getFilePosition());

    // Register it for later processing.
    addInput(new AncestorChain<Block>(scriptDelegate));

    // Build a replacment element.
    // <script type="text/javascript">MyPlugin.tmp123__()</script>
    DomTree.Tag callout;
    {
      Token<HtmlTokenType> endToken = new Token<HtmlTokenType>(
          ">", HtmlTokenType.TAGEND,
          FilePosition.endOf(scriptTag.getFilePosition()));
      callout = new DomTree.Tag(
          Collections.<DomTree>emptyList(), scriptTag.getToken(), endToken);
      callout.getAttributes().set(SCRIPT_TAG_CALLOUT_NAME, calloutName);
    }

    // Replace the external script tag with the inlined script.
    parent.replaceChild(callout, scriptTag);
  }

  /**
   * May be overridden to support loading of externally sourced script tags and
   * stylesheets.
   */
  protected CharProducer loadExternalResource(
      ExternalReference ref, String mimeType) {
    return null;
  }

  /**
   * May be overridden to apply a URI policy and return a URI that enforces that
   * policy.
   * @return null if the URI cannot be made safe.
   */
  protected String rewriteUri(ExternalReference uri, String mimeType) {
    return null;
  }

  private void rewriteStyles(
      DomTree.Tag styleTag, CharProducer cssStream, DomTree.Attrib media) {
    DomTree.Attrib type = lookupAttribute(styleTag, "type");

    if (type != null && !isCssContentType(type.getAttribValue())) {
      mq.addMessage(PluginMessageType.UNRECOGNIZED_CONTENT_TYPE,
                    type.getFilePosition(),
                    MessagePart.Factory.valueOf(type.getAttribValue()),
                    MessagePart.Factory.valueOf(styleTag.getTagName()));
      return;
    }

    CssTree.StyleSheet stylesheet;
    try {
      stylesheet = parseCss(cssStream.getCurrentPosition().source(), cssStream);
    } catch (ParseException ex) {
      ex.toMessageQueue(mq);
      return;
    }

    Set<String> mediaTypes = Collections.<String>emptySet();
    if (media != null) {
      String[] mediaTypeArr = media.getAttribValue().trim().split("\\s*,\\s*");
      if (mediaTypeArr.length != 1 || !"".equals(mediaTypeArr[0])) {
        mediaTypes = new LinkedHashSet<String>();
        for (String mediaType : mediaTypeArr) {
          if (!Css2.isMediaType(mediaType)) {
            mq.addMessage(PluginMessageType.UNRECOGNIZED_MEDIA_TYPE,
                          media.getFilePosition(),
                          MessagePart.Factory.valueOf(mediaType));
            continue;
          }
          mediaTypes.add(mediaType);
        }
      }
    }
    if (!(mediaTypes.isEmpty() || mediaTypes.contains("all"))) {
      final List<CssTree.RuleSet> rules = new ArrayList<CssTree.RuleSet>();
      stylesheet.acceptPreOrder(
          new Visitor() {
            public boolean visit(AncestorChain<?> ancestors) {
              CssTree node = (CssTree) ancestors.node;
              if (node instanceof CssTree.StyleSheet) { return true; }
              if (node instanceof CssTree.RuleSet) {
                rules.add((CssTree.RuleSet) node);
                ((MutableParseTreeNode) ancestors.parent.node)
                    .removeChild(node);
              }
              // Don't touch RuleSets that are not direct children of a
              // stylesheet.
              return false;
            }
          }, null);
      if (!rules.isEmpty()) {
        List<CssTree> mediaChildren = new ArrayList<CssTree>();
        for (String mediaType : mediaTypes) {
          mediaChildren.add(
              new CssTree.Medium(type.getFilePosition(), mediaType));
        }
        mediaChildren.addAll(rules);
        CssTree.Media mediaBlock = new CssTree.Media(
            styleTag.getFilePosition(), mediaChildren);
        stylesheet.appendChild(mediaBlock);
      }
    }

    addInput(new AncestorChain<CssTree.StyleSheet>(stylesheet));
  }

  private void rewriteStyleTag(
      DomTree.Tag styleTag, MutableParseTreeNode parent) {
    parent.removeChild(styleTag);

    CharProducer cssStream
        = textNodesToCharProducer(styleTag.children(), false);
    if (cssStream != null) {
      rewriteStyles(styleTag, cssStream, null);
    }
  }

  private void rewriteLinkTag(
      DomTree.Tag styleTag, MutableParseTreeNode parent) {
    parent.removeChild(styleTag);

    DomTree.Attrib rel = lookupAttribute(styleTag, "rel");
    if (rel == null
        || !rel.getAttribValue().trim().equalsIgnoreCase("stylesheet")) {
      // If it's not a stylesheet then ignore it.
      // The HtmlValidator should complain but that's not our problem.
      return;
    }

    DomTree.Attrib href = lookupAttribute(styleTag, "href");
    DomTree.Attrib media = lookupAttribute(styleTag, "media");

    if (href == null) {
      mq.addMessage(
          PluginMessageType.MISSING_ATTRIBUTE, styleTag.getFilePosition(),
          MessagePart.Factory.valueOf("href"),
          MessagePart.Factory.valueOf("style"));
      return;
    }

    URI hrefUri;
    try {
      hrefUri = new URI(href.getAttribValue());
    } catch (URISyntaxException ex) {
      mq.getMessages().add(
          new Message(PluginMessageType.MALFORMED_URL, MessageLevel.ERROR,
                      href.getFilePosition(), href));
      return;
    }

    // Fetch the stylesheet source.
    CharProducer cssStream = loadExternalResource(
        new ExternalReference(
            hrefUri, href.getAttribValueNode().getFilePosition()),
        "text/css");
    if (cssStream == null) {
      parent.removeChild(styleTag);
      mq.addMessage(
          PluginMessageType.FAILED_TO_LOAD_EXTERNAL_URL,
          href.getFilePosition(), MessagePart.Factory.valueOf("" + hrefUri));
      return;
    }

    rewriteStyles(styleTag, cssStream, media);
  }

  /**
   * A CharProducer that produces characters from the concatenation of all
   * the text nodes in the given node list.
   */
  private static CharProducer textNodesToCharProducer(
      List<? extends DomTree> nodes, boolean stripComments) {
    List<DomTree> textNodes = new ArrayList<DomTree>();
    for (DomTree node : nodes) {
      if (node instanceof DomTree.Text || node instanceof DomTree.CData) {
        textNodes.add(node);
      }
    }
    if (textNodes.isEmpty()) { return null; }
    List<CharProducer> content = new ArrayList<CharProducer>();
    for (int i = 0, n = textNodes.size(); i < n; ++i) {
      DomTree node = textNodes.get(i);
      String text = node.getValue();
      if (stripComments) {
        if (i == 0) {
          text = text.replaceFirst("^(\\s*)<!--", "$1     ");
        }
        if (i + 1 == n) {
          text = text.replaceFirst("-->(\\s*)$", "   $1");
        }
      }
      content.add(CharProducer.Factory.create(
          new StringReader(text),
          FilePosition.startOf(node.getFilePosition())));
    }
    if (content.size() == 1) {
      return content.get(0);
    } else {
      return CharProducer.Factory.chain(content.toArray(new CharProducer[0]));
    }
  }

  /** "text/html;charset=UTF-8" -> "text/html" */
  private static String getMimeType(String contentType) {
    int typeEnd = contentType.indexOf(';');
    if (typeEnd < 0) { typeEnd = contentType.length(); }
    return contentType.substring(0, typeEnd).toLowerCase();
  }

  private static boolean isJavaScriptContentType(String contentType) {
    String mimeType = getMimeType(contentType);
    return ("text/javascript".equals(mimeType)
            || "application/x-javascript".equals(mimeType)
            || "type/ecmascript".equals(mimeType));
  }

  private static boolean isCssContentType(String contentType) {
    return "text/css".equals(getMimeType(contentType));
  }

  private static DomTree.Attrib lookupAttribute(
      DomTree.Tag el, String attribName) {
    for (DomTree child : el.children()) {
      if (!(child instanceof DomTree.Attrib)) { break; }
      DomTree.Attrib attrib = (DomTree.Attrib) child;
      if (attribName.equalsIgnoreCase(attrib.getAttribName())) {
        return attrib;
      }
    }
    return null;
  }

  public Block parseJs(
      InputSource is, CharProducer cp, MessageQueue localMessageQueue)
      throws ParseException {
    JsLexer lexer = new JsLexer(cp);
    JsTokenQueue tq = new JsTokenQueue(lexer, is);
    Parser p = new Parser(tq, localMessageQueue);
    Block body = p.parse();
    tq.expectEmpty();
    return body;
  }

  public CssTree.StyleSheet parseCss(InputSource is, CharProducer cp)
      throws ParseException {
    CssLexer lexer = new CssLexer(cp);
    CssTree.StyleSheet input;
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
  public boolean run() {
    return (// Create an object to hold the plugin
            setUpNamespace()
            // Extract unsafe bits from HTML
            && rewriteHtml()
            // Whitelist html tags and attributes, and supply values for
            // attributes that are missing them.
            && validateHtml()
            // Compile the html and add a method to the plugin for the
            // html and one for each extracted script tag and event handler.
            && compileHtml()
            // Make sure the css is well formed and prefix all rules
            // so that they don't affect nodes outside the plugin.
            && validateCss()
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
            // html.
            && rewriteGlobalReference()
            // Rewrite the javascript to prevent runtime sandbox violations
            && validateJavascript()
            // Consolidate different CSS inputs from multiple <style> tags into
            // one awesome CssTree that can be outted to a .css file.
            && consolidateCss()
            && hasNoFatalErrors()
            // Store the raw output JS and CSS in Strings.
            // TODO(mikesamuel): don't do this.  Let the client supply a buffer,
            // and render context.
            && renderOutputs()

            && hasNoErrors()
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
    if (meta.scheme == PluginMeta.TranslationScheme.BAJA ||
        meta.scheme == PluginMeta.TranslationScheme.CAJA) {
      this.jsTree = s(new Block(
          Collections.<Statement>emptyList()
          ));
    } else {
      this.jsTree = s(new Block(
                        Arrays.<Statement>asList(
                            // the private plugin
                            s(new Declaration(
                                s(new Identifier(meta.namespacePrivateName)), pluginPrivate)),
                            // the plugin namespace
                            s(new Declaration(
                                s(new Identifier(meta.namespaceName)), pluginNamespace)),
                            s(new ExpressionStmt(s(new Operation(
                                Operator.ASSIGN,
                                s(new Operation(
                                    Operator.MEMBER_ACCESS,
                                    s(new Reference(s(new Identifier(meta.namespacePrivateName)))),
                                    s(new Reference(s(new Identifier("plugin")))))),
                                s(new Reference(s(new Identifier(meta.namespaceName))))
                                )))),
                            s(new ExpressionStmt(s(new Operation(
                                Operator.FUNCTION_CALL,
                                s(new Reference(s(new Identifier("plugin_initialize___")))),
                                s(new Reference(s(new Identifier(meta.namespacePrivateName))))))))
                        )));
    }
  }

  private boolean rewriteHtml() {
    for (int i = 0; i < inputs.size(); ++i) {
      Input input = inputs.get(i);
      if (InputType.HTML == input.type) {
        rewriteDomTree((DomTree) input.parsetree.node);
      }
    }
    return true;
  }

  private boolean validateHtml() {
    HtmlValidator v = new HtmlValidator(mq);

    boolean valid = true;
    for (Input input : inputs) {
      if (InputType.HTML == input.type) {
        if (!v.validate((DomTree) input.parsetree.node, null)) {
          valid = false;
        }
      }
    }
    return valid;
  }

  private boolean compileHtml() {
    List<HtmlJob> jobs = new ArrayList<HtmlJob>();
    for (Input input : inputs) {
      if (InputType.HTML == input.type) {
        jobs.add(new HtmlJob((DomTree) input.parsetree.node, null));
      }
    }

    HtmlCompiler htmlc = new HtmlCompiler(mq, meta) {
        @Override
        protected String rewriteUri(ExternalReference uri, String mimeType) {
          return HtmlPluginCompiler.this.rewriteUri(uri, mimeType);
        }
      };

    List<Statement> renderedHtmlStatements = new ArrayList<Statement>();
    for (HtmlJob job : jobs) {
      try {
        renderedHtmlStatements.add(htmlc.compileDocument(job.docRoot));
      } catch (GxpCompiler.BadContentException ex) {
        ex.toMessageQueue(mq);
      }
    }

    for (FunctionDeclaration handler : htmlc.getEventHandlers()) {
      // function foo() { ... }
      // => ___OUTERS___.foo = function foo() { ... };
      Statement def = s(new ExpressionStmt(
          s(new Operation(
                Operator.ASSIGN,
                s(new Operation(
                      Operator.MEMBER_ACCESS,
                      s(new Reference(s(new Identifier(meta.namespaceName)))),
                      s(new Reference(handler.getIdentifier())))),
                handler.getInitializer()))));
      addInput(new AncestorChain<Block>(
                   new Block(Collections.singletonList(def))));
    }

    if (!renderedHtmlStatements.isEmpty()) {
      Block htmlGeneration;
      if (renderedHtmlStatements.size() == 1
          && renderedHtmlStatements.get(0) instanceof Block) {
        htmlGeneration = (Block) renderedHtmlStatements.get(0);
      } else {
        htmlGeneration = new Block(renderedHtmlStatements);
      }
      addInput(new AncestorChain<Block>(htmlGeneration));
    }

    boolean success = hasNoFatalErrors();
    return success;
  }

  /**
   * sanitizes and namespace any css inputs.
   * @return true if the input css was safe.  False if any destructive
   *   modifications had to be made to make it safe, or if such modifications
   *   were needed but could not be made.
   */
  private boolean validateCss() {
    // TODO(msamuel): build up a list of classes and ids for use in generating
    // "no such symbol" warnings from the GXPs.
    boolean valid = true;
    CssValidator v = new CssValidator(mq);
    CssRewriter rw = new CssRewriter(meta, mq);
    for (Input input : inputs) {
      AncestorChain<CssTree> cssTree;
      if (InputType.CSS == input.type) {
        // The parsetree node is a CssTree.StyleSheet
        cssTree = input.parsetree.cast(CssTree.class);
      } else {
        continue;
      }
      valid &= v.validateCss(cssTree);
      valid &= rw.rewrite(cssTree);
    }

    boolean success = valid && hasNoFatalErrors();
    System.out.println("validateCss: " + success);
    return success;
  }

  private boolean moveGlobalDefinitionsIntoPluginNamespace() {
    if (meta.scheme == PluginMeta.TranslationScheme.AAJA) {
      HtmlGlobalDefRewriter rw = new HtmlGlobalDefRewriter(meta);

      for (Input input : inputs) {
        if (InputType.JAVASCRIPT == input.type) {
          input.parsetree.node.acceptPreOrder(rw, input.parsetree.parent);
        }
      }
    }

    boolean success = hasNoFatalErrors();
    return success;
  }

  private boolean consolidateCodeIntoInitializerBody() {
    // create an initializer function
    Block initFunctionBody = s(new Block(Collections.<Statement>emptyList()));

    MutableParseTreeNode.Mutation newChanges =
        initFunctionBody.createMutation();
    for (Input input : inputs) {
      if (InputType.JAVASCRIPT == input.type) {
        Block body = (Block) input.parsetree.node;
        MutableParseTreeNode.Mutation oldChanges = body.createMutation();
        for (Statement s : body.children()) {
          oldChanges.removeChild(s);
          newChanges.insertBefore(s, null);
        }
        oldChanges.execute();
      }
    }
    newChanges.execute();

    // ___.loadModule(function (<namespace>) { <compiled code> })
    this.jsTree.insertBefore(
        s(new ExpressionStmt(TreeConstruction.call(
            TreeConstruction.memberAccess("___", "loadModule"),
            TreeConstruction.function(null, initFunctionBody,
                meta.namespaceName)))),
        null);

    boolean success = hasNoFatalErrors();
    System.out.println("consolidateCodeIntoInitializerBody: " + success);
    return success;
  }

  private boolean rewriteGlobalReference() {
    if (meta.scheme != PluginMeta.TranslationScheme.CAJA) {
      new HtmlGlobalReferenceRewriter(meta, mq).rewrite(
          this.jsTree, Collections.<String>emptySet());
    }

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
    }

    boolean valid;
    switch (meta.scheme) {
      case AAJA:
        valid = new ExpressionSanitizer(mq).sanitize(
            new AncestorChain<Block>(this.jsTree));
        break;
      case BAJA:
        valid = new ExpressionSanitizerBaja(mq, meta).sanitize(
            new AncestorChain<Block>(this.jsTree));
        break;
      case CAJA:
        valid = new ExpressionSanitizerCaja(mq, meta).sanitize(
            new AncestorChain<Block>(this.jsTree));
        break;
      default:
        throw new RuntimeException("Unrecognized scheme: " + meta.scheme);
    }

    boolean success = valid && hasNoFatalErrors();
    System.out.println("validateJavascript: " + success);
    return success;
  }

  private boolean consolidateCss() {
    List<CssTree.CssStatement> children = new ArrayList<CssTree.CssStatement>();
    for (Input input : inputs) {
      if (InputType.CSS != input.type) { continue; }
      CssTree.StyleSheet styleSheet = (CssTree.StyleSheet) input.parsetree.node;
      int firstToRemove = children.size();
      for (CssTree child : styleSheet.children()) {
        children.add((CssTree.CssStatement) child);
      }
      for (CssTree child : children.subList(firstToRemove, children.size())) {
        styleSheet.removeChild(child);
      }
    }
    FilePosition pos = FilePosition.startOfFile(
        new InputSource(URI.create("plugin-spec:///")));

    cssTree = new CssTree.StyleSheet(pos, children);
    return true;
  }

  private boolean hasNoFatalErrors() {
    return hasNoMessagesOfLevel(MessageLevel.FATAL_ERROR);
  }

  private boolean hasNoErrors() {
    return hasNoMessagesOfLevel(MessageLevel.ERROR);
  }

  private boolean hasNoMessagesOfLevel(MessageLevel level) {
    for (Message m : mq.getMessages()) {
      if (level.compareTo(m.getMessageLevel()) <= 0) {
        return false;
      }
    }
    return true;
  }

  private boolean renderOutputs() {
    try {
      StringBuilder jsOut = new StringBuilder();
      RenderContext rc = new RenderContext(mc, jsOut, true);
      jsTree.render(rc);
      rc.newLine();
      outputJs = jsOut.toString();
    } catch (IOException ex) {
      throw new RuntimeException("StringBuilders shouldn't throw", ex);
    }

    try {
      StringBuilder cssOut = new StringBuilder();
      RenderContext rc = new RenderContext(mc, cssOut, true);
      cssTree.render(rc);
      rc.newLine();
      outputCss = cssOut.toString();
    } catch (IOException ex) {
      throw new RuntimeException("StringBuilders shouldn't throw", ex);
    }

    return true;
  }

  /** make the given parse tree node synthetic. */
  private static <T extends ParseTreeNode> T s(T t) {
    t.getAttributes().set(ExpressionSanitizer.SYNTHETIC, Boolean.TRUE);
    return t;
  }

  private static class Input {
    final AncestorChain<?> parsetree;
    final InputType type;

    Input(AncestorChain<?> parsetree) {
      assert null != parsetree;

      ParseTreeNode parsetreeNode = parsetree.node;
      if (parsetreeNode instanceof Statement) {
        this.type = InputType.JAVASCRIPT;
      } else if (parsetreeNode instanceof DomTree.Fragment
                 || parsetreeNode instanceof DomTree.Tag) {
        this.type = InputType.HTML;
      } else if (parsetreeNode instanceof CssTree.StyleSheet) {
        this.type = InputType.CSS;
      } else {
        throw new AssertionError("Unknown input type " + parsetreeNode);
      }

      this.parsetree = parsetree;
    }
  }

  private enum InputType {
    CSS,
    JAVASCRIPT,
    HTML
    ;
  }
}

final class HtmlJob {
  final DomTree docRoot;
  final AncestorChain<?> toReplace;
  FunctionConstructor compiled;

  HtmlJob(DomTree docRoot, AncestorChain<?> toReplace) {
    assert null != docRoot;
    this.docRoot = docRoot;
    this.toReplace = toReplace;
  }
}

class HtmlGlobalDefRewriter implements Visitor {
  final PluginMeta meta;

  HtmlGlobalDefRewriter(PluginMeta meta) { this.meta = meta; }

  public boolean visit(AncestorChain<?> ancestors) {
    ParseTreeNode n = ancestors.node;
    if (n instanceof FunctionConstructor) { return false; }
    if (n instanceof MultiDeclaration) {
      // replace with a block.  Then recurse so that the declarations will get
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
      block.acceptPreOrder(this, ancestors.parent);
      return false;
    } else if (n instanceof Declaration) {
      MutableParseTreeNode parent
          = (MutableParseTreeNode) ancestors.getParentNode();
      if (parent instanceof CatchStmt && parent.children().get(0) == n) {
        // do not move the exception declaration in a catch block
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
      public boolean visit(AncestorChain<?> ancestors) {
        ParseTreeNode node = ancestors.node;
        // if we see a function constructor, we need to compute a new set of
        // local declarations and recurse
        if (node instanceof FunctionConstructor) {
          FunctionConstructor c = (FunctionConstructor) node;
          Set<String> fnLocals = new HashSet<String>(locals);
          fnLocals.addAll(IMPLICIT_FUNCTION_DEFINITIONS);
          LocalDeclarationInspector insp =
            new LocalDeclarationInspector(fnLocals);
          for (ParseTreeNode child : c.children()) {
            child.acceptPreOrder(insp, ancestors);
          }
          rewrite(c.getBody(), fnLocals);
          return false;
        }

        if (node instanceof Reference) {
          Reference ref = (Reference) node;
          String refName = ref.getIdentifierName();
          if (!node.getAttributes().is(ExpressionSanitizer.SYNTHETIC)) {
            if (refName.endsWith("__")) {
              mq.addMessage(MessageType.ILLEGAL_NAME, ref.getFilePosition(),
                            MessagePart.Factory.valueOf(refName));
            }
          }
          MutableParseTreeNode parent
              = (MutableParseTreeNode) ancestors.getParentNode();
          Operator parentOp = null;
          if (parent instanceof Operation) {
            parentOp = ((Operation) parent).getOperator();
          }
          // If node is part of a member access, and is not the leftmost
          // reference, then don't rewrite.  We don't want to rewrite the
          // b in a.b. Nor do we want to rewrite the b in "b in a".

          // We also don't want to rewrite synthetic nodes -- nodes created by
          // the PluginCompiler..
          boolean isFirstOperand = ancestors.isFirstSibling();
          if (!locals.contains(ref.getIdentifier())
              && !ref.getAttributes().is(ExpressionSanitizer.SYNTHETIC)
              && !(!isFirstOperand && Operator.MEMBER_ACCESS == parentOp)
              && !(isFirstOperand && Operator.IN == parentOp)) {

            Reference placeholder = s(new Reference(s(new Identifier("_"))));
            Operation pluginReference = s(
                new Operation(
                    Operator.MEMBER_ACCESS,
                    s(new Reference(s(new Identifier(meta.namespaceName)))),
                    placeholder));
            parent.replaceChild(pluginReference, ref);
            pluginReference.replaceChild(ref, placeholder);
          }
        }
        return true;
      }
    }, null);
  }

  static <T extends ParseTreeNode> T s(T n) {
    n.getAttributes().set(ExpressionSanitizer.SYNTHETIC, Boolean.TRUE);
    return n;
  }

  static final class LocalDeclarationInspector implements Visitor {
    final Set<String> locals;

    LocalDeclarationInspector(Set<String> locals) { this.locals = locals; }

    public boolean visit(AncestorChain<?> ancestors) {
      ParseTreeNode node = ancestors.node;
      if (node instanceof FunctionConstructor) { return false; }
      if (node instanceof Declaration
          && !(node instanceof FunctionDeclaration)) {
        locals.add(((Declaration) node).getIdentifierName());
      }
      return true;
    }
  }
}
