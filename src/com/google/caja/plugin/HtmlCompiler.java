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

import com.google.caja.CajaException;
import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HTML;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.Keyword;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.TryStmt;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.parser.quasiliteral.ReservedNames;
import com.google.caja.plugin.stages.RewriteHtmlStage;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Criterion;
import com.google.caja.util.Name;
import com.google.caja.util.Pair;
import static com.google.caja.parser.js.SyntheticNodes.s;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Compiles HTML containing CSS and JavaScript to Javascript + safe CSS.
 * This takes in a DOM, and outputs javascript that will render the DOM.
 * The resulting javascript requires "html-emitter.js" which builds the DOM
 * client side.
 *
 * @author mikesamuel@gmail.com
 */
public class HtmlCompiler {
  public static final class BadContentException extends CajaException {
    private static final long serialVersionUID = -5317800396186044550L;
    BadContentException(Message m) { super(m); }
    BadContentException(Message m, Throwable th) { super(m, th); }
  }

  private final CssSchema cssSchema;
  private final HtmlSchema htmlSchema;
  private final MessageContext mc;
  private final MessageQueue mq;
  private final PluginMeta meta;
  private Map<String, Statement> eventHandlers =
      new LinkedHashMap<String, Statement>();

  public HtmlCompiler(CssSchema cssSchema, HtmlSchema htmlSchema,
                      MessageContext mc, MessageQueue mq, PluginMeta meta) {
    if (null == cssSchema || null == htmlSchema || null == mq || null == meta) {
      throw new NullPointerException();
    }
    this.cssSchema = cssSchema;
    this.htmlSchema = htmlSchema;
    this.mc = mc;
    this.mq = mq;
    this.meta = meta;
  }

  /**
   * Compiles a document to a javascript function.
   *
   * <p>This method extracts embedded javascript but performs no validation on
   * it.</p>
   */
  public Block compileDocument(DomTree doc) throws BadContentException {
    // Produce calls to IMPORTS___.htmlEmitter___(...)
    // with interleaved script bodies.
    DomProcessingEvents cdom = new DomProcessingEvents();
    compileDom(doc, cdom);

    Block body = new Block(
        doc.getFilePosition(), Collections.<Statement>emptyList());
    cdom.toJavascript(body);
    return body;
  }

  public Collection<? extends Statement> getEventHandlers() {
    return eventHandlers.values();
  }

  /**
   * Appends to block, statements that will send events to out that can be used
   * to reproduce t on the browser.
   *
   * @param t the tree to render
   */
  private void compileDom(DomTree t, DomProcessingEvents out)
      throws BadContentException {
    if (t instanceof DomTree.Fragment) {
      for (DomTree child : t.children()) {
        compileDom(child, out);
      }
      return;
    }
    switch (t.getType()) {
      case TEXT:
        out.pcdata(t.getFilePosition(), ((DomTree.Text) t).getValue());
        break;
      case CDATA:
        out.cdata(t.getFilePosition(), ((DomTree.Text) t).getValue());
        break;
      case TAGBEGIN:
        DomTree.Tag el = (DomTree.Tag) t;
        Name tagName = el.getTagName();

        if ("span".equals(tagName.getCanonicalForm())) {
          Block extractedScriptBody = el.getAttributes().get(
              RewriteHtmlStage.EXTRACTED_SCRIPT_BODY);
          if (extractedScriptBody != null) {
            out.script(scriptBodyEnvelope(extractedScriptBody));
            return;
          }
        } else if ("style".equals(tagName.getCanonicalForm())) {
          // nothing to do.  Style tags get combined into one and output as
          // CSS, not written via javascript.
          return;
        }

        assertNotBlacklistedTag(el);

        DomAttributeConstraint constraint =
            DomAttributeConstraint.Factory.forTag(tagName);

        tagName = assertHtmlIdentifier(tagName, el);
        boolean requiresCloseTag = requiresCloseTag(tagName);
        constraint.startTag(el);
        List<? extends DomTree> children = el.children();

        out.begin(el.getToken().pos, tagName);

        if (children.isEmpty()) {
          for (Pair<Name, String> extra : constraint.tagDone(el)) {
            out.attr(FilePosition.UNKNOWN, extra.a, extra.b);
          }
          out.finishAttrs(!requiresCloseTag);
          if (requiresCloseTag) {
            out.end(FilePosition.endOf(t.getFilePosition()), tagName);
          }
        } else {
          int i;
          // output attributes
          for (i = 0; i < children.size(); ++i) {
            DomTree child = children.get(i);
            if (HtmlTokenType.ATTRNAME != child.getType()) { break; }
            DomTree.Attrib attrib = (DomTree.Attrib) child;
            Name name = attrib.getAttribName();

            name = assertHtmlIdentifier(name, attrib);

            Pair<String, String> wrapper = constraint.attributeValueHtml(name);
            if (null == wrapper) { continue; }

            if ("style".equals(name.getCanonicalForm())) {
              compileStyleAttrib(attrib, out);
            } else {
              AttributeXform xform = xformForAttribute(tagName, name);

              DomTree.Attrib temp =
                  new DomTree.Attrib(
                      attrib.getAttribName(),
                      new DomTree.Value(
                          Token.<HtmlTokenType>instance(
                              wrapper.a + attrib.getAttribValue() + wrapper.b,
                              HtmlTokenType.ATTRVALUE,
                              attrib.getFilePosition())),
                      attrib.getToken(),
                      attrib.getFilePosition());

              if (null == xform) {
                out.attr(temp.getFilePosition(), name, temp.getAttribValue());
              } else {
                List<DomTree> newchildren
                    = new ArrayList<DomTree>(el.children());
                newchildren.remove(attrib);
                newchildren.add(temp);
                DomTree parent = new DomTree.Tag(
                    el.getTagName(), newchildren, el.getToken(),
                    el.getFilePosition());

                xform.apply(
                    new AncestorChain<DomTree.Attrib>(
                        new AncestorChain<DomTree>(
                          parent), temp),
                    this, out);
              }
            }
            constraint.attributeDone(name);
          }

          for (Pair<Name, String> extra : constraint.tagDone(el)) {
            out.attr(FilePosition.UNKNOWN, extra.a, extra.b);
          }

          boolean tagAllowsContent = tagAllowsContent(tagName);
          out.finishAttrs(!(requiresCloseTag || tagAllowsContent));

          List<? extends DomTree> childrenRemaining =
              children.subList(i, children.size());

          // recurse to contents
          boolean wroteChildElement = false;

          if (tagAllowsContent) {
            for (DomTree child : childrenRemaining) {
              compileDom(child, out);
              wroteChildElement = true;
            }
          } else {
            for (DomTree child : childrenRemaining) {
              if (!isWhitespaceTextNode(child)) {
                mq.addMessage(MessageType.MALFORMED_XHTML,
                              child.getFilePosition(), child);
              }
            }
          }

          if (wroteChildElement || requiresCloseTag) {
            out.end(FilePosition.endOf(t.getFilePosition()), tagName);
          }
        }
        break;
      default:
        throw new AssertionError(t.getType().name() + "  " + t.toStringDeep());
    }
  }

  private static final Pattern HTML_ID = Pattern.compile(
      "^[a-z][a-z0-9-]*$", Pattern.CASE_INSENSITIVE);
  private static Name assertHtmlIdentifier(
      Name id, DomTree node)
      throws BadContentException {
    if (!HTML_ID.matcher(id.getCanonicalForm()).matches()) {
      throw new BadContentException(new Message(
          PluginMessageType.BAD_IDENTIFIER, node.getFilePosition(), id));
    }
    return id;
  }

  private void assertNotBlacklistedTag(DomTree.Tag node)
      throws BadContentException {
    Name tagName = node.getTagName();
    if (!htmlSchema.isElementAllowed(tagName)) {
      throw new BadContentException(
          new Message(PluginMessageType.UNSAFE_TAG, node.getFilePosition(),
                      tagName));
    }
  }

  /**
   * True if the given name requires a close tag.
   *   "TABLE" -> true, "BR" -> false.
   */
  private boolean requiresCloseTag(Name tagName) {
    HTML.Element e = htmlSchema.lookupElement(tagName);
    return null == e || !e.isEmpty();
  }

  /**
   * True if the tag can have content.  False for unitary tags like
   * {@code INPUT} and {@code BR}.
   */
  private boolean tagAllowsContent(Name tagName) {
    HTML.Element e = htmlSchema.lookupElement(tagName);
    return null == e || !e.isEmpty();
  }

  /**
   * Invokes the CSS validator to rewrite style attributes.
   * @param attrib an attribute with name {@code "style"}.
   */
  private void compileStyleAttrib(
      DomTree.Attrib attrib, DomProcessingEvents out)
      throws BadContentException {
    CssTree.DeclarationGroup decls;
    try {
      decls = parseStyleAttrib(attrib);
      if (decls == null) { return; }
    } catch (ParseException ex) {
      throw new BadContentException(ex.getCajaMessage(), ex);
    }

    // The validator will check that property values are well-formed,
    // marking those that aren't, and identifies all urls.
    CssValidator v = new CssValidator(cssSchema, htmlSchema, mq)
        .withInvalidNodeMessageLevel(MessageLevel.WARNING);
    v.validateCss(new AncestorChain<CssTree>(decls));
    // The rewriter will remove any unsafe constructs.
    // and put urls in the proper filename namespace
    new CssRewriter(meta, mq).withInvalidNodeMessageLevel(MessageLevel.WARNING)
        .rewrite(new AncestorChain<CssTree>(decls));

    Block cssBlock = new Block(
        FilePosition.UNKNOWN, Collections.<Statement>emptyList());
    // Produces a call to cat(bits, of, css);
    declGroupToStyleValue(
        decls, Arrays.asList("cat"), cssBlock, JsWriter.Esc.NONE);
    if (cssBlock.children().isEmpty()) { return; }
    if (cssBlock.children().size() != 1) {
      throw new IllegalStateException(attrib.getAttribValue());
    }
    Expression css = ((ExpressionStmt) cssBlock.children().get(0))
        .getExpression();
    // Convert cat(a, b, c) to (a + b) + c
    List<? extends Expression> operands = ((Operation) css).children();
    Expression cssOp = operands.get(1);
    for (Expression e : operands.subList(2, operands.size())) {
      cssOp = Operation.createInfix(Operator.ADDITION, cssOp, e);
    }
    out.attr(Name.html("style"), cssOp);
  }

  /**
   * Parses a style attribute's value as a CSS declaration group.
   */
  private CssTree.DeclarationGroup parseStyleAttrib(DomTree.Attrib t)
      throws ParseException {
    // parse the attribute value as CSS
    DomTree.Value value = t.getAttribValueNode();
    // use the raw value so that the file positions come out right in
    // CssValidator error messages.
    String cssAsHtml = deQuote(value.getToken().text);
    // the raw value is html so we wrap it in an html unescaper
    CharProducer cp = CharProducer.Factory.fromHtmlAttribute(
        CharProducer.Factory.create(
            new StringReader(cssAsHtml), value.getFilePosition()));
    // parse the css as a set of declarations separated by semicolons
    CssLexer lexer = new CssLexer(cp, true);
    TokenQueue<CssTokenType> tq = new TokenQueue<CssTokenType>(
        lexer, cp.getCurrentPosition().source(),
        new Criterion<Token<CssTokenType>>() {
          public boolean accept(Token<CssTokenType> t) {
            return CssTokenType.SPACE != t.type
                && CssTokenType.COMMENT != t.type;
          }
        });
    if (tq.isEmpty()) { return null; }
    tq.setInputRange(value.getFilePosition());
    CssParser p = new CssParser(tq);
    CssTree.DeclarationGroup decls = p.parseDeclarationGroup();
    tq.expectEmpty();
    return decls;
  }

  /**
   * Strip quotes from an attribute value if there are any.
   */
  private static String deQuote(String s) {
    int len = s.length();
    if (len < 2) { return s; }
    char ch0 = s.charAt(0);
    return (('"' == ch0 || '\'' == ch0) && ch0 == s.charAt(len - 1))
           ? " " + s.substring(1, len - 1) + " "
           : s;
  }

  /**
   * Parses an {@code onclick} handler's or other handler's attribute value
   * as a javascript statement.
   */
  private Block asBlock(DomTree stmt) {
    // parse as a javascript expression.
    String src = deQuote(stmt.getToken().text);
    FilePosition pos = stmt.getToken().pos;
    CharProducer cp = CharProducer.Factory.fromHtmlAttribute(
        CharProducer.Factory.create(new StringReader(src), pos));
    JsLexer lexer = new JsLexer(cp);
    JsTokenQueue tq = new JsTokenQueue(lexer, pos.source());
    tq.setInputRange(pos);
    Parser p = new Parser(tq, mq);
    List<Statement> statements = new ArrayList<Statement>();
    try {
      while (!tq.isEmpty()) {
        statements.add(p.parseStatement());
      }
    } catch (ParseException ex) {
      ex.toMessageQueue(mq);
      statements.clear();
    }

    // expression will be sanitized in a later pass
    return new Block(stmt.getFilePosition(), statements);
  }

  /**
   * Wrap the extracted script block in an exception handler so that
   * exceptions thrown in script blocks do not interfere with subsequent
   * execution.
   * @see <a href=
   *     "http://code.google.com/p/google-caja/wiki/UncaughtExceptionHandling"
   *     >UncaughtExceptionHandling</a>
   */
  private Statement scriptBodyEnvelope(Block scriptBody) {
    FilePosition pos = scriptBody.getFilePosition();
    String sourcePath = mc.abbreviate(pos.source());
    TryStmt envelope = (TryStmt) QuasiBuilder.substV(
        ""
        + "try {"
        + "  @scriptBody;"
        + "} catch (ex___) {"
        + "  ___./*@synthetic*/ getNewModuleHandler()"
        + "      ./*@synthetic*/ handleUncaughtException("
        + "      ex___, onerror, @sourceFile, @line);"
        + "}",

        "scriptBody", scriptBody,
        "sourceFile", StringLiteral.valueOf(FilePosition.UNKNOWN, sourcePath),
        "line", StringLiteral.valueOf(
            FilePosition.UNKNOWN, String.valueOf(pos.startLineNo())));
    envelope.setFilePosition(pos);
    return envelope;
  }

  /**
   * produces an identifier that will not collide with any previously generated
   * identifier.
   */
  private String syntheticId() {
    return meta.generateUniqueName("c");
  }

  /** is the given node a text node that consists only of whitespace? */
  private static boolean isWhitespaceTextNode(DomTree t) {
    switch (t.getType()) {
      case TEXT: case CDATA:
        return "".equals(t.getValue().trim());
      default:
        return false;
    }
  }

  /**
   * for a given html attribute, what kind of transformation do we have to
   * perform on the value?
   */
  private AttributeXform xformForAttribute(
      Name tagName, Name attribute) {
    HTML.Attribute a = htmlSchema.lookupAttribute(tagName, attribute);
    if (null != a) {
      switch (a.getType()) {
        case ID:
        case IDREF:
        case GLOBAL_NAME:
          return AttributeXform.NAMES;
        case CLASSES:
          return AttributeXform.CLASSES;
        case STYLE:
          return AttributeXform.STYLE;
        case SCRIPT:
          return AttributeXform.SCRIPT;
        case URI:
          return AttributeXform.URI;
        default: break;
      }
    }
    return null;
  }

  private String guessMimeType(Name tagName, Name attribName) {
    HTML.Attribute type = htmlSchema.lookupAttribute(tagName, attribName);
    String mimeType = type.getMimeTypes();
    return mimeType != null ? mimeType : "*/*";
  }

  /**
   * encapsulates a transformation on an html attribute value.
   * Transformations are performed at compile time.
   */
  private static enum AttributeXform {
    /** Applied to {@code name} and {@code id} attributes. */
    NAMES {
      @Override
      void apply(AncestorChain<DomTree.Attrib> tChain, HtmlCompiler htmlc,
                 DomProcessingEvents out) {
        DomTree.Attrib t = tChain.node;
        IdentifierWriter.ConcatenationEmitter emitter
            = new IdentifierWriter.ConcatenationEmitter();
        FilePosition valuePos = t.getAttribValueNode().getFilePosition();
        new IdentifierWriter(htmlc.mq, true)
            .toJavascript(valuePos, t.getAttribValue(), emitter);
        Expression value = emitter.getExpression();
        if (value != null) {
          out.attr(t.getAttribName(), value);
        }
      }
    },
    CLASSES {
      @Override
      void apply(AncestorChain<DomTree.Attrib> tChain, HtmlCompiler htmlc,
                 DomProcessingEvents out) {
        DomTree.Attrib t = tChain.node;
        IdentifierWriter.ConcatenationEmitter emitter
            = new IdentifierWriter.ConcatenationEmitter();
        FilePosition attribValue = t.getAttribValueNode().getFilePosition();
        new IdentifierWriter(htmlc.mq, false)
            .toJavascript(attribValue, t.getAttribValue(), emitter);
        Expression value = emitter.getExpression();
        if (value != null) {
          out.attr(t.getAttribName(), value);
        }
      }
    },
    /** Applied to CSS such as {@code style} attributes. */
    STYLE {
      @Override
      void apply(AncestorChain<DomTree.Attrib> tChain, HtmlCompiler htmlc,
                 DomProcessingEvents out) {
        // should be handled in compileDOM
        throw new AssertionError();
      }
    },
    /** Applied to javascript such as {@code onclick} attributes. */
    SCRIPT {
      @Override
      void apply(AncestorChain<DomTree.Attrib> tChain, HtmlCompiler htmlc,
                 DomProcessingEvents out) {
        DomTree.Attrib t = tChain.node;
        // Extract the handler into a function so that it can be analyzed.
        Block handler = htmlc.asBlock(t.getAttribValueNode());
        if (handler.children().isEmpty()) { return; }
        rewriteEventHandlerReferences(handler);

        // This function must not be synthetic.  If it were, the rewriter would
        // not treat its formals as affecting scope.
        FunctionConstructor handlerFn = new FunctionConstructor(
            t.getAttribValueNode().getFilePosition(),
            new Identifier(FilePosition.UNKNOWN, null),
            Arrays.asList(
                new FormalParam(s(
                    new Identifier(FilePosition.UNKNOWN, "event"))),
                new FormalParam(s(
                    new Identifier(
                        FilePosition.UNKNOWN, ReservedNames.THIS_NODE)))),
            handler);

        String handlerFnName = htmlc.syntheticId();
        htmlc.eventHandlers.put(
            handlerFnName,
            new ExpressionStmt(
                t.getAttribValueNode().getFilePosition(),
                (Expression) QuasiBuilder.substV(
                "IMPORTS___.@handlerFnName = @handlerFn;",
                "handlerFnName", TreeConstruction.ref(handlerFnName),
                "handlerFn", handlerFn)));

        String handlerFnNameLit = StringLiteral.toQuotedValue(handlerFnName);

        Operation dispatcher = Operation.createInfix(
            Operator.ADDITION,
            Operation.createInfix(
                Operator.ADDITION,
                StringLiteral.valueOf(
                    t.getAttribValueNode().getFilePosition(),
                    "return plugin_dispatchEvent___(this, event, "),
                TreeConstruction.call(
                    TreeConstruction.memberAccess("___", "getId"),
                    TreeConstruction.ref(ReservedNames.IMPORTS))),
                    StringLiteral.valueOf(
                        FilePosition.UNKNOWN, ", " + handlerFnNameLit + ")"));
        out.handler(t.getAttribName(), dispatcher);
      }
    },
    /** Applied to URIs such as {@code href} and {@code src} attributes. */
    URI {
      @Override
      void apply(AncestorChain<DomTree.Attrib> tChain, HtmlCompiler htmlc,
                 DomProcessingEvents out) {
        DomTree.Attrib t = tChain.node;
        URI uri;
        try {
          uri = new URI(t.getAttribValue());
        } catch (URISyntaxException ex) {
          htmlc.mq.addMessage(PluginMessageType.MALFORMED_URL,
                              t.getAttribValueNode().getFilePosition(),
                              MessagePart.Factory.valueOf(t.getAttribValue()));
          return;
        }
        String mimeType = htmlc.guessMimeType(
            ((DomTree.Tag) tChain.getParentNode()).getTagName(),
            tChain.node.getAttribName());
        String rewrittenUri = htmlc.meta.getPluginEnvironment().rewriteUri(
            new ExternalReference(
                uri, t.getAttribValueNode().getFilePosition()),
                mimeType);
        if (rewrittenUri != null) {
          out.attr(t.getFilePosition(), t.getAttribName(), rewrittenUri);
        } else {
          htmlc.mq.addMessage(
              PluginMessageType.DISALLOWED_URI,
              t.getAttribValueNode().getFilePosition(),
              MessagePart.Factory.valueOf(uri.toString()));
        }
      }
    },
    ;

    /**
     * apply, at compile time, any preprocessing steps to the given attributes
     * value.
     */
    abstract void apply(
        AncestorChain<DomTree.Attrib> tChain,
        HtmlCompiler htmlc, DomProcessingEvents out)
        throws BadContentException;
  }

  /**
   * Convert "this" -> "thisNode___" in event handlers.  Event handlers are
   * run in a context where this points to the current node.
   * We need to emulate that but still allow the event handlers to be simple
   * functions, so we pass in the tamed node as the first parameter.
   *
   * The event handler goes from:<br>
   *   {@code if (this.type === 'text') alert(this.value); }
   * to a function like:<pre>
   *   function (thisNode___, event) {
   *     if (thisNode___.type === 'text') {
   *       alert(thisNode___.value);
   *     }
   *   }</pre>
   * <p>
   * And the resulting function is called via a handler attribute like
   * {@code onchange="plugin_dispatchEvent___(this, node, 1234, 'handlerName')"}
   */
  static void rewriteEventHandlerReferences(Block block) {
    block.acceptPreOrder(
        new Visitor() {
          public boolean visit(AncestorChain<?> ancestors) {
            ParseTreeNode node = ancestors.node;
            // Do not recurse into closures.
            if (node instanceof FunctionConstructor) { return false; }
            if (node instanceof Reference) {
              Reference r = (Reference) node;
              if (Keyword.THIS.toString().equals(r.getIdentifierName())) {
                Identifier oldRef = r.getIdentifier();
                Identifier thisNode = new Identifier(
                    oldRef.getFilePosition(), ReservedNames.THIS_NODE);
                r.replaceChild(s(thisNode), oldRef);
              }
              return false;
            }
            return true;
          }
        }, null);
  }

  static void declGroupToStyleValue(
      CssTree.DeclarationGroup cssTree, final List<String> tgtChain,
      final Block b, final JsWriter.Esc esc) {

    declarationsToJavascript(cssTree, esc, new DynamicCssReceiver() {
        boolean first = true;

        public void property(CssTree.Property p) {
          StringBuilder out = new StringBuilder();
          TokenConsumer tc = p.makeRenderer(out, null);
          if (first) {
            first = false;
          } else {
            tc.consume(";");
          }
          p.render(new RenderContext(new MessageContext(), tc));
          tc.consume(":");
          tc.noMoreTokens();
          out.append(" ");
          rawCss(p.getFilePosition(), out.toString());
        }

        public void rawCss(FilePosition pos, String rawCss) {
          JsWriter.appendText(pos, rawCss, esc, tgtChain, b);
        }

        public void priority(CssTree.Prio p) {
          StringBuilder out = new StringBuilder();
          out.append(" ");
          TokenConsumer tc = p.makeRenderer(out, null);
          p.render(new RenderContext(new MessageContext(), tc));
          tc.noMoreTokens();
          rawCss(p.getFilePosition(), out.toString());
        }
      });
  }

  private static interface DynamicCssReceiver {
    void property(CssTree.Property p);

    void rawCss(FilePosition pos, String rawCss);

    void priority(CssTree.Prio p);
  }

  private static void declarationsToJavascript(
      CssTree.DeclarationGroup decls, JsWriter.Esc esc,
      DynamicCssReceiver out) {
    assert esc == JsWriter.Esc.NONE || esc == JsWriter.Esc.HTML_ATTRIB : esc;

    for (CssTree child : decls.children()) {
      CssTree.Declaration decl = (CssTree.Declaration) child;
      // Render the style to a canonical form with consistent escaping
      // conventions, so that we can avoid browser bugs.
      String css;
      {
        StringBuilder cssBuf = new StringBuilder();
        TokenConsumer tc = decl.makeRenderer(cssBuf, null);
        decl.getExpr().render(new RenderContext(new MessageContext(), tc));
        tc.noMoreTokens();

        // Contains the rendered CSS with ${\0###\0} placeholders.
        // Split around the placeholders, parse the javascript, escape the
        // literal text, and emit the appropriate javascript.
        css = cssBuf.toString();
      }

      out.property(decl.getProperty());
      out.rawCss(decl.getFilePosition(), css);
      if (decl.getPrio() != null) { out.priority(decl.getPrio()); }
    }
  }
}
