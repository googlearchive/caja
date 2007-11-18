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

import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageType;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.ReturnStmt;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.css.CssParser;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.JsLexer;
import com.google.caja.util.Pair;
import com.google.caja.util.Criterion;
import com.google.caja.html.HTML4;
import com.google.caja.html.HTML;

import java.io.StringReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Compiles HTML containing CSS and JavaScript to Javascript + safe CSS.
 */
public final class HtmlCompiler {

  private final MessageQueue mq;
  private final PluginMeta meta;
  private Map<String, TemplateSignature> sigs =
    new LinkedHashMap<String, TemplateSignature>();
  private Map<String, FunctionDeclaration> eventHandlers =
    new LinkedHashMap<String, FunctionDeclaration>();
  private int syntheticIdCounter;

  public HtmlCompiler(MessageQueue mq, PluginMeta meta) {
    if (null == mq) { throw new NullPointerException(); }
    this.mq = mq;
    this.meta = meta;
  }

  /**
   * Compiles the signature for the given document.
   */
  public TemplateSignature compileTemplateSignature(DomTree.Tag document) {

    String id = this.syntheticId();

    TemplateSignature sig = new TemplateSignature(
        id,
        document.children(),
        document.getFilePosition()
        );
    sigs.put(id, sig);
    return sig;
  }

  /**
   * Compiles a document to a javascript function.
   *
   * <p>This method extracts embedded javascript but performs no validation on
   * it.</p>
   */
  public FunctionConstructor compileDocument(TemplateSignature sig)
      throws GxpCompiler.BadContentException {
    List<FormalParam> params = new ArrayList<FormalParam>();

    // var out = [];
    Block body = s(new Block(Collections.<Statement>emptyList()));
    body.insertBefore(
        s(new Declaration(
              "out___",
              s(new ArrayConstructor(
                    Collections.<Expression>emptyList())))), null);

    for (DomTree tree : sig.content) {
      compileDom(tree, "out___", false, JsWriter.Esc.HTML, body);
    }

    // join the html via out___.join('') and mark it as safe html
    //   return plugin_blessHtml___(out.join(''));
    ReturnStmt result = s(new ReturnStmt(
                              s(new Operation(
                                    Operator.FUNCTION_CALL,
                                    s(new Reference("plugin_blessHtml___")),
                                    s(new Operation(
                                          Operator.FUNCTION_CALL,
                                          s(new Operation(
                                                Operator.MEMBER_ACCESS,
                                                s(new Reference("out___")),
                                                s(new Reference("join")))),
                                          s(new StringLiteral("''"))
            ))
          ))
        ));
    body.insertBefore(result, null);
    return s(new FunctionConstructor(sig.assignedName, params, body));
  }

  public Collection<? extends TemplateSignature> getSignatures() {
    return sigs.values();
  }

  public Collection<? extends FunctionDeclaration> getEventHandlers() {
    return eventHandlers.values();
  }

  private void compileDom(
      DomTree t, String tgt, boolean inAttrib, JsWriter.Esc escaping, Block b)
      throws GxpCompiler.BadContentException {
    switch (t.getType()) {
    case TEXT:
      JsWriter.appendText(((DomTree.Text) t).getValue(), escaping, tgt, b);
      break;
    case CDATA:
      JsWriter.appendText(t.getValue(), escaping, tgt, b);
      break;
    case TAGBEGIN:
      DomTree.Tag el = (DomTree.Tag) t;
      String tagName = el.getValue();

      assertNotBlacklistedTag(el);

      assert escaping != JsWriter.Esc.NONE;

      DomAttributeConstraint constraint =
          DomAttributeConstraint.Factory.forTag(tagName);

      tagName = assertHtmlIdentifier(tagName, el);
      if (inAttrib) {
        throw new GxpCompiler.BadContentException(
            new Message(PluginMessageType.TAG_NOT_ALLOWED_IN_ATTRIBUTE,
                        el.getFilePosition(),
                        MessagePart.Factory.valueOf("<" + tagName + ">")));
      }
      JsWriter.appendString("<" + tagName, tgt, b);
      constraint.startTag(el);
      List<? extends DomTree> children = el.children();
      if (children.isEmpty()) {
        for (Pair<String, String> extra : constraint.tagDone(el)) {
          JsWriter.appendString(
              " " + extra.a + "=\"" + JsWriter.htmlEscape(extra.b) + "\"",
              tgt, b);
        }
        if (requiresCloseTag(tagName)) {
          JsWriter.appendString("></" + tagName + ">", tgt, b);
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
            compileStyleAttrib(attrib, tgt, b);
          } else {
            AttributeXform xform = xformForAttribute(tagName, name);
            String value = (null != xform)
                ? xform.apply(attrib, this)
                : valueT.getValue();

            JsWriter.appendString(
                " " + name + "=\""
                + JsWriter.htmlEscape(wrapper.a + value + wrapper.b)
                + "\"", tgt, b);
          }
          constraint.attributeDone(name);
        }

        for (Pair<String, String> extra : constraint.tagDone(el)) {
          JsWriter.appendString(
              " " + extra.a + "=\"" + JsWriter.htmlEscape(extra.b) + "\"",
              tgt, b);
        }

        JsWriter.appendString(">", tgt, b);

        List<? extends DomTree> childrenRemaining =
            children.subList(i, children.size());

        // recurse to contents
        boolean wroteChildElement = false;

        if (tagName.equals("script") || tagName.equals("style")) {

        } else if (tagAllowsContent(tagName)) {
          for (DomTree child : childrenRemaining) {
            compileDom(child, tgt, false, JsWriter.Esc.HTML, b);
            wroteChildElement = true;
          }
        } else {
          for (DomTree child : childrenRemaining) {
            if (!isWhitespaceTextNode(child) && !isGxpAttrElement(child)) {
              mq.addMessage(MessageType.MALFORMED_XHTML,
                            child.getFilePosition(), child);
            }
          }
        }

        if (wroteChildElement || requiresCloseTag(tagName)) {
          JsWriter.appendString("</" + tagName + ">", tgt, b);
        }
      }
      break;
      default:
        throw new AssertionError(t.getType().name() +
                                 "  " +
                                 ((DomTree.Attrib) t).getAttribName() +
                                 "  " +
                                 ((DomTree.Attrib) t).getAttribValue());
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

  private static boolean requiresCloseTag(String tag) {
    HTML.Element e = HTML4.lookupElement(tag.toUpperCase());
    return null == e || !e.isEmpty();
  }

  private static boolean tagAllowsContent(String tag) {
    HTML.Element e = HTML4.lookupElement(tag.toUpperCase());
    return null == e || !e.isEmpty();
  }

  private void compileStyleAttrib(DomTree.Attrib attrib, String tgt, Block b)
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
    boolean valid = v.validateCss(decls);
    // The rewriter will remove any unsafe constructs.
    // and put urls in the proper filename namespace
    CssRewriter rw = new CssRewriter(meta, mq);
    valid &= rw.rewrite(decls);
    // Notify the user that the style was changed.
    if (!valid) {
      mq.addMessage(
          PluginMessageType.REWROTE_STYLE,
          attrib.getAttribValueNode().getFilePosition(),
          MessagePart.Factory.valueOf(attrib.getAttribValue()));
    }

    JsWriter.appendString(" style=\"", tgt, b);
    CssTemplate.bodyToJavascript(decls, meta, tgt, b, JsWriter.Esc.HTML, mq);
    JsWriter.appendString("\"", tgt, b);
  }

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

  private static String deQuote(String s) {
    int len = s.length();
    if (len < 2) { return s; }
    char ch0 = s.charAt(0);
    return (('"' == ch0 || '\'' == ch0) && ch0 == s.charAt(len - 1))
           ? " " + s.substring(1, len - 1) + " "
           : s;
  }

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
    b.parentify();

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
    return "c" + (++syntheticIdCounter) + "___";
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

  private static boolean isGxpAttrElement(DomTree t) {
    return (HtmlTokenType.TAGBEGIN == t.getType()
            && "gxp:attr".equals(t.getValue()));
  }

  /** encapsulates a GXP templates name, and parameter declarations. */
  public static final class TemplateSignature {
    final String assignedName;
    final List<? extends DomTree> content;
    final FilePosition loc;

    TemplateSignature(
        String assignedName,
        List<? extends DomTree> content,
        FilePosition loc) {
      this.assignedName = assignedName;
      this.content = content;
      this.loc = loc;
    }

    public String getAssignedName() { return assignedName; }
    public List<? extends DomTree> getContent() { return content; }
    public FilePosition getLocation() { return loc; }
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

  /**
   * encapsulates a transformation on an html attribute value.
   * Some transformations are performed at compile time, and some may be
   * performed at runtime.
   */
  private static enum AttributeXform {
    NMTOKEN {
      @Override
      String apply(DomTree.Attrib t, HtmlCompiler htmlc) {
        String nmTokens = t.getAttribValue();
        StringBuilder sb = new StringBuilder(nmTokens.length() + 16);
        boolean wasSpace = true;
        int pos = 0;
        for (int i = 0, n = nmTokens.length(); i < n; ++i) {
          char ch = nmTokens.charAt(i);
          boolean space = Character.isWhitespace(ch);
          if (space != wasSpace) {
            if (!space) {
              if (0 != sb.length()) { sb.append(' '); }
              sb.append(htmlc.meta.namespacePrefix).append('-');
              pos = i;
            } else {
              sb.append(nmTokens.substring(pos, i));
            }
            wasSpace = space;
          }
        }
        if (!wasSpace) { sb.append(nmTokens.substring(pos)); }
        return sb.toString();
      }
      @Override
      String runtimeFunction(String tagName, String attribName, DomTree t,
                             HtmlCompiler htmlc) {
        return "plugin_prefix___";
      }
    },
    STYLE {
      @Override
      String apply(DomTree.Attrib t, HtmlCompiler htmlc) {
        // should be handled in compileDOM
        throw new AssertionError();
      }
      @Override
      String runtimeFunction(String tagName, String attribName, DomTree t,
                             HtmlCompiler htmlc)
          throws GxpCompiler.BadContentException {
        throw new GxpCompiler.BadContentException(new Message(
            PluginMessageType.ATTRIBUTE_CANNOT_BE_DYNAMIC, t.getFilePosition(),
            MessagePart.Factory.valueOf(tagName),
            MessagePart.Factory.valueOf(attribName)));
      }
    },
    SCRIPT {
      @Override
      String apply(DomTree.Attrib t, HtmlCompiler htmlc) {
        // Extract the handler into a function so that it can be analyzed.
        Block handler = htmlc.asBlock(t.getAttribValueNode());

        String handlerFnName = htmlc.syntheticId();
        htmlc.eventHandlers.put(
            handlerFnName,
            s(new FunctionDeclaration(
                  handlerFnName,
                  s(new FunctionConstructor(
                        null,
                        Collections.singletonList(
                            s(new FormalParam(
                                  "event"))),
                        handler)))));

        String owner = htmlc.meta.namespaceName;
        return "return plugin_dispatchEvent___(event || window.event, this, "
               + htmlc.meta.namespacePrivateName + ", "
               + (null != owner ? owner + "." : "") + handlerFnName + ");";
      }
      @Override
      String runtimeFunction(String tagName, String attribName, DomTree t,
                             HtmlCompiler htmlc)
          throws GxpCompiler.BadContentException {
        throw new GxpCompiler.BadContentException(new Message(
            PluginMessageType.ATTRIBUTE_CANNOT_BE_DYNAMIC, t.getFilePosition(),
            MessagePart.Factory.valueOf(tagName),
            MessagePart.Factory.valueOf(attribName)));
      }
    },
    ;
    /**
     * apply, at compile time, any preprocessing steps to the given attributes
     * value.
     */
    abstract String apply(DomTree.Attrib t, HtmlCompiler htmlc)
        throws GxpCompiler.BadContentException;
    /**
     * given an attribute name, the gxp attribute that specifies it, and the
     * compiler, return the name of a function that will take the attribute
     * value and the namespace prefix and return the processed version of the
     * value.
     */
    abstract String runtimeFunction(
        String tagName, String attribName, DomTree nameNode, HtmlCompiler htmlc)
        throws GxpCompiler.BadContentException;

  }

}
