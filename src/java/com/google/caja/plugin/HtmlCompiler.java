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

import com.google.caja.html.HTML;
import com.google.caja.html.HTML4;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.Criterion;
import com.google.caja.util.Pair;

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
 * The content can be rendered by either pushing it onto a buffer as in
 * {@code tgtChain=['out__', 'push']} or to an emit function such as
 * {@code tgtChain=['document', 'write']}.
 *
 * <p>
 * TODO(mikesamuel): this shares a lot of code with GxpCompiler and the two
 * should be merged.
 * </p>
 *
 * @author mikesamuel@gmail.com
 */
public class HtmlCompiler {

  private final MessageQueue mq;
  private final PluginMeta meta;
  private Map<String, FunctionDeclaration> eventHandlers =
      new LinkedHashMap<String, FunctionDeclaration>();

  public HtmlCompiler(MessageQueue mq, PluginMeta meta) {
    if (null == mq) { throw new NullPointerException(); }
    this.mq = mq;
    this.meta = meta;
  }

  /**
   * May be overridden to apply a URI policy and return a URI that enforces that
   * policy.
   * @return null if the URI cannot be made safe.
   */
  protected String rewriteUri(ExternalReference uri, String mimeType) {
    return null;
  }

  /**
   * Compiles a document to a javascript function.
   *
   * <p>This method extracts embedded javascript but performs no validation on
   * it.</p>
   */
  public Statement compileDocument(DomTree doc)
      throws GxpCompiler.BadContentException {

    // Produce callse to ___OUTERS___.emitHtml___(<html>)
    // with inlined calls to functions extracted from script bodies.
    Block body = s(new Block(Collections.<Statement>emptyList()));
    compileDom(doc, Arrays.asList("___OUTERS___", "emitHtml___"), body);
    return body;
  }

  public Collection<? extends FunctionDeclaration> getEventHandlers() {
    return eventHandlers.values();
  }

  /**
   * Appends to block, statements that will call the function identified by
   * tgtChain with snippets of html that comprise t.
   *
   * @param t the tree to render
   * @param tgtChain the function to invoke with html chunks, such as
   *   {@code ['out__', 'push']} to output
   *   {@code out.push('<html>', foo, '</html>')}.
   * @param b the block to which statements are added.
   */
  private void compileDom(DomTree t, List<String> tgtChain, Block b)
      throws GxpCompiler.BadContentException {
    if (t instanceof DomTree.Fragment) {
      for (DomTree child : t.children()) {
        compileDom(child, tgtChain, b);
      }
      return;
    }
    switch (t.getType()) {
      case TEXT:
        JsWriter.appendText(
            ((DomTree.Text) t).getValue(), JsWriter.Esc.HTML, tgtChain, b);
        break;
      case CDATA:
        JsWriter.appendText(t.getValue(), JsWriter.Esc.HTML, tgtChain, b);
        break;
      case TAGBEGIN:
        DomTree.Tag el = (DomTree.Tag) t;
        String tagName = el.getValue().toLowerCase();

        if (tagName.equals("script")) {
          Block extractedScriptBody = el.getAttributes().get(
              HtmlPluginCompiler.EXTRACTED_SCRIPT_BODY);
          if (extractedScriptBody != null) {
            b.createMutation().appendChildren(extractedScriptBody.children())
                .execute();
          }
          return;
        } else if (tagName.equals("style")) {
          // nothing to do.  Style tags get combined into one and output as
          // CSS, not written via javascript.
          return;
        }

        assertNotBlacklistedTag(el);

        DomAttributeConstraint constraint =
            DomAttributeConstraint.Factory.forTag(tagName);

        tagName = assertHtmlIdentifier(tagName, el);
        JsWriter.appendString("<" + tagName, tgtChain, b);
        constraint.startTag(el);
        List<? extends DomTree> children = el.children();
        if (children.isEmpty()) {
          for (Pair<String, String> extra : constraint.tagDone(el)) {
            JsWriter.appendString(
                " " + extra.a + "=\"" + JsWriter.htmlEscape(extra.b) + "\"",
                tgtChain, b);
          }
          if (requiresCloseTag(tagName)) {
            JsWriter.appendString("></" + tagName + ">", tgtChain, b);
          }
        } else {
          int i;
          // output parameters
          for (i = 0; i < children.size(); ++i) {
            DomTree child = children.get(i);
            if (HtmlTokenType.ATTRNAME != child.getType()) { break; }
            DomTree.Attrib attrib = (DomTree.Attrib) child;
            String name = attrib.getAttribName();
            DomTree.Value valueT = attrib.getAttribValueNode();

            name = assertHtmlIdentifier(name, attrib);

            Pair<String, String> wrapper = constraint.attributeValueHtml(name);
            if (null == wrapper) { continue; }

            if ("style".equalsIgnoreCase(name)) {
              compileStyleAttrib(attrib, tgtChain, b);
            } else {
              AttributeXform xform = xformForAttribute(tagName, name);
              if (null == xform) {
                JsWriter.appendString(
                    " " + name + "=\""
                    + JsWriter.htmlEscape(
                          wrapper.a + valueT.getValue() + wrapper.b)
                    + "\"", tgtChain, b);
              } else {
                xform.apply(
                    new AncestorChain<DomTree.Attrib>(
                        new AncestorChain<DomTree>(el), attrib),
                    this, tgtChain, b);
              }
            }
            constraint.attributeDone(name);
          }

          for (Pair<String, String> extra : constraint.tagDone(el)) {
            JsWriter.appendString(
                " " + extra.a + "=\"" + JsWriter.htmlEscape(extra.b) + "\"",
                tgtChain, b);
          }

          JsWriter.appendString(">", tgtChain, b);

          List<? extends DomTree> childrenRemaining =
              children.subList(i, children.size());

          // recurse to contents
          boolean wroteChildElement = false;

          if (tagAllowsContent(tagName)) {
            for (DomTree child : childrenRemaining) {
              compileDom(child, tgtChain, b);
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

          if (wroteChildElement || requiresCloseTag(tagName)) {
            JsWriter.appendString("</" + tagName + ">", tgtChain, b);
          }
        }
        break;
      default:
        throw new AssertionError(t.getType().name() + "  " + t.toStringDeep());
    }
  }

  private static final Pattern HTML_ID = Pattern.compile(
      "^[a-z][a-z0-9-]*$", Pattern.CASE_INSENSITIVE);
  private static String assertHtmlIdentifier(String s, DomTree node)
      throws GxpCompiler.BadContentException {
    if (!HTML_ID.matcher(s).matches()) {
      throw new GxpCompiler.BadContentException(
          new Message(PluginMessageType.BAD_IDENTIFIER, node.getFilePosition(),
                      MessagePart.Factory.valueOf(s)));
    }
    return s;
  }

  private static void assertNotBlacklistedTag(DomTree node)
      throws GxpCompiler.BadContentException {
    String tagName = node.getValue().toUpperCase();
    if (!HtmlValidator.isAllowedTag(tagName)) {
      throw new GxpCompiler.BadContentException(
          new Message(MessageType.MALFORMED_XHTML, node.getFilePosition(),
                    MessagePart.Factory.valueOf(tagName)));
    }
  }

  /**
   * True if the given name requires a close tag.
   *   "TABLE" -> true, "BR" -> false.
   * @param tag a tag name, such as {@code P} for {@code <p>} tags.
   */
  private static boolean requiresCloseTag(String tag) {
    HTML.Element e = HTML4.lookupElement(tag.toUpperCase());
    return null == e || !e.isEmpty();
  }

  /**
   * True if the tag can have content.  False for unitary tags like
   * {@code INPUT} and {@code BR}.
   * @param tag a tag name, such as {@code P} for {@code <p>} tags.
   */
  private static boolean tagAllowsContent(String tag) {
    HTML.Element e = HTML4.lookupElement(tag.toUpperCase());
    return null == e || !e.isEmpty();
  }

  /**
   * Invokes the CSS validator to rewrite style attributes.
   * @param attrib an attribute with name {@code "style"}.
   * @param tgtChain the function to invoke with html chunks, such as
   *   {@code ['out__', 'push']} to output
   *   {@code out.push('<html>', foo, '</html>')}.
   * @param b the block to which statements are added.
   */
  private void compileStyleAttrib(
      DomTree.Attrib attrib, List<String> tgtChain, Block b)
      throws GxpCompiler.BadContentException {
    CssTree decls;
    try {
      decls = parseStyleAttrib(attrib);
    } catch (ParseException ex) {
      throw new GxpCompiler.BadContentException(ex.getCajaMessage(), ex);
    }

    // The validator will check that property values are well-formed,
    // marking those that aren't, and identifies all urls.
    CssValidator v = new CssValidator(mq);
    boolean valid = v.validateCss(new AncestorChain<CssTree>(decls));
    // The rewriter will remove any unsafe constructs.
    // and put urls in the proper filename namespace
    CssRewriter rw = new CssRewriter(meta, mq);
    valid &= rw.rewrite(new AncestorChain<CssTree>(decls));
    // Notify the user that the style was changed.
    if (!valid) {
      mq.addMessage(
          PluginMessageType.REWROTE_STYLE,
          attrib.getAttribValueNode().getFilePosition(),
          MessagePart.Factory.valueOf(attrib.getAttribValue()));
    }

    JsWriter.appendString(" style=\"", tgtChain, b);
    CssTemplate.bodyToJavascript(
        decls, meta, tgtChain, b, JsWriter.Esc.HTML_ATTRIB, mq);
    JsWriter.appendString("\"", tgtChain, b);
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
    String cssAsHtml = HtmlCompiler.deQuote(value.getToken().text);
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
    CharProducer cp =
      CharProducer.Factory.fromHtmlAttribute(CharProducer.Factory.create(
          new StringReader(src), stmt.getFilePosition()));
    JsLexer lexer = new JsLexer(cp);
    JsTokenQueue tq = new JsTokenQueue(lexer, stmt.getFilePosition().source());
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

    Block b = new Block(statements);
    b.setFilePosition(stmt.getFilePosition());

    // expression will be sanitized in a later pass
    return b;
  }

  /**
   * a convenience function used to mark all nodes created by the gxp compiler
   * as {@link ExpressionSanitizer#SYNTHETIC synthetic}.  The only non synthetic
   * nodes in the compiled javascript will be those corresponsing to javascript
   * embedded in the gxp.
   */
  private static <T extends ParseTreeNode> T s(T t) {
    t.getAttributes().set(ExpressionSanitizer.SYNTHETIC, Boolean.TRUE);
    return t;
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
  private static AttributeXform xformForAttribute(
      String tagName, String attribute) {
    attribute = attribute.toUpperCase();
    if ("STYLE".equals(attribute)) { return AttributeXform.STYLE; }
    if ("ID".equals(attribute)
        || "CLASS".equals(attribute)
        || ("NAME".equals(attribute) && !isInput(tagName))
        || "FOR".equals(attribute)) {
      return AttributeXform.NMTOKEN;
    }
    HTML.Attribute a = HTML4.lookupAttribute(attribute);
    if (null != a) {
      switch (a.getType()) {
      case SCRIPT:
        return AttributeXform.SCRIPT;
      case URI:
        return AttributeXform.URI;
      }
    }
    return null;
  }

  /** is an html element with the given name a form element? */
  private static boolean isInput(String tagName) {
    tagName = tagName.toUpperCase();
    return "INPUT".equals(tagName) || "SELECT".equals(tagName)
        || "TEXTAREA".equals(tagName) || "MAP".equals(tagName);
  }

  private static String guessMimeType(String tagName) {
    if ("IMG".equalsIgnoreCase(tagName)) {
      return "image/*";
    }
    return "*/*";
  }

  /**
   * encapsulates a transformation on an html attribute value.
   * Transformations are performed at compile time.
   */
  private static enum AttributeXform {
    /** Applied to NMTOKENs such as {@code id} and {@code class} attributes. */
    NMTOKEN {
      @Override
      void apply(AncestorChain<DomTree.Attrib> tChain, HtmlCompiler htmlc,
                 List<String> tgtChain, Block out) {
        DomTree.Attrib t = tChain.node;
        String nmTokens = t.getAttribValue();
        StringBuilder sb = new StringBuilder(nmTokens.length() + 16);
        boolean wasSpace = true;
        int pos = 0;

        sb.append(' ').append(t.getAttribName()).append("=\"");
        boolean firstIdent = true;
        for (int i = 0, n = nmTokens.length(); i < n; ++i) {
          char ch = nmTokens.charAt(i);
          boolean space = Character.isWhitespace(ch);
          if (space != wasSpace) {
            if (!space) {
              if (!firstIdent) {
                sb.append(' ');
              } else {
                firstIdent = false;
              }
              Escaping.escapeXml(htmlc.meta.namespacePrefix, true, sb);
              sb.append('-');
              pos = i;
            } else {
              Escaping.escapeXml(nmTokens.substring(pos, i), true, sb);
            }
            wasSpace = space;
          }
        }
        if (!wasSpace) {
          Escaping.escapeXml(nmTokens.substring(pos), true, sb);
        }
        sb.append('"');

        JsWriter.appendString(sb.toString(), tgtChain, out);
      }
    },
    /** Applied to CSS such as {@code style} attributes. */
    STYLE {
      @Override
      void apply(AncestorChain<DomTree.Attrib> tChain, HtmlCompiler htmlc,
                 List<String> tgtChain, Block out) {
        // should be handled in compileDOM
        throw new AssertionError();
      }
    },
    /** Applied to javascript such as {@code onclick} attributes. */
    SCRIPT {
      @Override
      void apply(AncestorChain<DomTree.Attrib> tChain, HtmlCompiler htmlc,
                 List<String> tgtChain, Block out) {
        DomTree.Attrib t = tChain.node;
        // Extract the handler into a function so that it can be analyzed.
        Block handler = htmlc.asBlock(t.getAttribValueNode());
        rewriteEventHandlerReferences(handler);

        String handlerFnName = htmlc.syntheticId();
        htmlc.eventHandlers.put(
            handlerFnName,
            s(new FunctionDeclaration(
                  new Identifier(handlerFnName),
                  s(new FunctionConstructor(
                        new Identifier(null),
                        Arrays.asList(
                            s(new FormalParam(
                                  new Identifier(ReservedNames.THIS_NODE))),
                            s(new FormalParam(
                                  new Identifier("event")))),
                        handler)))));

        JsWriter.appendString(
            " " + t.getAttribName() + "=\""
            + "return plugin_dispatchEvent___(event || window.event, this, ",
            tgtChain, out);

        JsWriter.append(
            s(new Operation(
                Operator.FUNCTION_CALL,
                s(new Operation(
                    Operator.MEMBER_ACCESS,
                    s(new Reference(new Identifier("___"))),
                    s(new Reference(new Identifier("getId"))))),
                s(new Reference(new Identifier(htmlc.meta.namespaceName))))),
            tgtChain, out);

        StringBuilder sb = new StringBuilder(", '");
        StringLiteral.escapeJsString(handlerFnName, sb);
        sb.append("')\"");
        JsWriter.appendString(sb.toString(), tgtChain, out);
      }
    },
    /** Applied to URIs such as {@code href} and {@code src} attributes. */
    URI {
      @Override
      void apply(AncestorChain<DomTree.Attrib> tChain, HtmlCompiler htmlc,
                 List<String> tgtChain, Block out) {
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
        String mimeType = guessMimeType(
            ((DomTree.Tag) tChain.getParentNode()).getTagName());
        String rewrittenUri = htmlc.rewriteUri(
            new ExternalReference(
                uri, t.getAttribValueNode().getFilePosition()),
                mimeType);
        if (rewrittenUri != null) {
          StringBuilder html = new StringBuilder();
          html.append(' ').append(t.getAttribName()).append("=\"");
          Escaping.escapeXml(rewrittenUri, true, html);
          html.append('"');
          JsWriter.appendString(html.toString(), tgtChain, out);
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
        HtmlCompiler htmlc, List<String> tgtChain, Block b)
        throws GxpCompiler.BadContentException;
  }

  /**
   * Convert "this" -> "thisNode___" in event handlers.  Event handlers are
   * run in a context where this points to the current node.
   * We need to emulate that but still allow the event handlers to be simple
   * functions, so we pass in the tamed node as the first parameter.
   *
   * The event handler goes from:<br>
   *   {@code if (this.type === 'text') { alert(this.value); } }
   * to a function like:<br>
   *   {@code function (thisNode___, event) {
   *       if (thisNode___.type === 'text') {
   *         alert(thisNode___.value);
   *       }
   *     } }
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
              if (ReservedNames.THIS.equals(r.getIdentifierName())) {
                Identifier oldRef = r.getIdentifier();
                Identifier thisNode = new Identifier(ReservedNames.THIS_NODE);
                thisNode.setFilePosition(oldRef.getFilePosition());
                r.replaceChild(s(thisNode), oldRef);
              }
              return false;
            }
            return true;
          }
        }, null);
  }
}
