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
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Conditional;
import com.google.caja.parser.js.ContinueStmt;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ForEachLoop;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.ReturnStmt;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import static com.google.caja.plugin.SyntheticNodes.s;

/**
 * Compiles a subset of gxp to javascript.
 * This class is responsible for making sure that a safe hierarchy is properly
 * compiled to javascript, that DOM ids and classes are properly namespaced, and
 * that the resulting hierarchy has nodes properly marked synthetic.  It is not
 * responsible for sanitizing expressions other than event handlers, or for
 * rejecting bad nodes.
 *
 * @author mikesamuel@gmail.com
 */
public final class GxpCompiler {
  private final CssSchema cssSchema;
  private final HtmlSchema htmlSchema;
  private final MessageQueue mq;
  private final PluginMeta meta;
  private final GxpValidator gxpValidator;
  private Map<String, TemplateSignature> sigs =
    new LinkedHashMap<String, TemplateSignature>();
  private Map<String, FunctionDeclaration> eventHandlers =
    new LinkedHashMap<String, FunctionDeclaration>();
  private int syntheticIdCounter;

  public GxpCompiler(CssSchema cssSchema, HtmlSchema htmlSchema,
                     PluginMeta meta, MessageQueue mq) {
    if (null == cssSchema || null == htmlSchema || null == meta || null == mq) {
      throw new NullPointerException();
    }
    this.cssSchema = cssSchema;
    this.htmlSchema = htmlSchema;
    this.mq = mq;
    this.meta = meta;
    this.gxpValidator = new GxpValidator(htmlSchema, mq);
  }

  /**
   * Compiles the signature for the given gxp.  All the gxp signatures for a
   * group of gxps must be compiled before the bodies can be
   * {@link #compileDocument compiled} to make sure that gxp calls,
   * <code>&lt;call:templateName&gt;</code>, work properly.  If two gxps are
   * guaranteed not to call each other, then they, and their signatures, can be
   * compiled separately.
   */
  public TemplateSignature compileTemplateSignature(DomTree.Tag document)
      throws BadContentException {

    List<? extends DomTree> children = document.children();
    DomTree.Value templateName;
    List<DomTree.Value> params = new ArrayList<DomTree.Value>();

    int importEnd;
    {
      Map<String, DomTree.Value> attribMap =
        new HashMap<String, DomTree.Value>();
      importEnd = gxpValidator.attribsAsMap(
          document, attribMap, ALLOWED_TEMPLATE_PARAMS);
      DomTree.Value name = attribMap.get("name");
      if (null == name) {
        throw new BadContentException(
            new Message(
                PluginMessageType.MISSING_ATTRIBUTE,
                document.getFilePosition(),
                MessagePart.Factory.valueOf("name"), document));
      }
      templateName = name;
    }

    for (; importEnd < children.size(); ++importEnd) {
      DomTree child = children.get(importEnd);
      if (HtmlTokenType.TEXT == child.getType()
          && "".equals(child.getValue().trim())) {
        continue;
      }
      if (HtmlTokenType.TAGBEGIN != child.getType()
          || !"gxp:param".equals(child.getValue())) {
        break;
      }
      DomTree.Tag el = (DomTree.Tag) child;
      Map<String, DomTree.Value> attribMap =
        new HashMap<String, DomTree.Value>();
      int attribEnd = gxpValidator.attribsAsMap(
          el, attribMap, ALLOWED_PARAM_PARAMS);
      DomTree.Value name = attribMap.get("name");
      if (null == name) {
        mq.addMessage(PluginMessageType.MISSING_ATTRIBUTE,
                      el.getFilePosition(), MessagePart.Factory.valueOf("name"),
                      el);
        continue;
      }
      if (attribEnd != child.children().size()) {
        mq.addMessage(PluginMessageType.EXTRANEOUS_CONTENT,
                      child.getFilePosition(), name);
      }
      assertSafeJsIdentifier(name.getValue(), name);
      params.add(name);
    }

    List<AncestorChain<? extends DomTree>> content
        = new ArrayList<AncestorChain<? extends DomTree>>();
    AncestorChain<DomTree.Tag> documentChain
        = new AncestorChain<DomTree.Tag>(document);
    for (DomTree child : children.subList(importEnd, children.size())) {
      content.add(new AncestorChain<DomTree>(documentChain, child));
    }

    TemplateSignature sig = new TemplateSignature(
        assertSafeJsIdentifier(templateName.getValue(), templateName),
        this.syntheticId(),
        params,
        content,
        document.getFilePosition()
        );
    sigs.put(templateName.getValue(), sig);
    return sig;
  }

  /**
   * Compiles a GXP template to a javascript function.
   * The resulting function will take the gxp parameters in the order they are
   * declared and return a snippet of html.
   *
   * <p>This method extracts embedded javascript but performs no validation on
   * it.</p>
   */
  public FunctionConstructor compileDocument(TemplateSignature sig)
      throws BadContentException {
    List<FormalParam> params = new ArrayList<FormalParam>();

    for (DomTree paramName : sig.parameterNames) {
      FormalParam param = s(new FormalParam(
          s(new Identifier(
                assertSafeJsIdentifier(paramName.getValue(), paramName)))));
      param.setFilePosition(paramName.getFilePosition());
      params.add(param);
    }

    String OUTPUT_ARRAY_NAME = "out___";
    // Chunks of html should be pushed onto an array called out___
    List<String> tgtChain = Arrays.asList(OUTPUT_ARRAY_NAME, "push");

    // var out___ = [];
    Block body = s(new Block(Collections.<Statement>emptyList()));
    body.insertBefore(
        s(new Declaration(
              new Identifier(OUTPUT_ARRAY_NAME),
              s(new ArrayConstructor(
                    Collections.<Expression>emptyList())))), null);

    for (AncestorChain<? extends DomTree> treeChain : sig.content) {
      compileDom(treeChain, tgtChain, false, JsWriter.Esc.HTML, body);
    }

    // Join the html via out___.join('') and mark it as safe html
    //   return ___OUTERS___.plugin_blessHtml___(out.join(''));
    ReturnStmt result = s(new ReturnStmt(
        TreeConstruction.call(
            TreeConstruction.memberAccess(
                ReservedNames.OUTERS, "plugin_blessHtml___"),
            TreeConstruction.call(
                TreeConstruction.memberAccess(OUTPUT_ARRAY_NAME, "join"),
                s(new StringLiteral("''"))))));
    body.insertBefore(result, null);
    return s(new FunctionConstructor(s(new Identifier(sig.templateName)),
                                     params, body));
  }

  public Collection<? extends TemplateSignature> getSignatures() {
    return sigs.values();
  }

  public Collection<? extends FunctionDeclaration> getEventHandlers() {
    return eventHandlers.values();
  }

  private void compileDom(
      AncestorChain<? extends DomTree> tChain, List<String> tgtChain,
      boolean inAttrib, JsWriter.Esc escaping, Block b)
      throws BadContentException {
    DomTree t = tChain.node;
    switch (t.getType()) {
    case TEXT:
      JsWriter.appendText(trimText(tChain.cast(DomTree.Text.class)),
                          escaping, tgtChain, b);
      break;
    case CDATA:
      JsWriter.appendText(t.getValue(), escaping, tgtChain, b);
      break;
    case TAGBEGIN:
      DomTree.Tag el = (DomTree.Tag) t;
      String tagName = el.getValue();
      if (GxpValidator.isGxp(tagName)) {
        if ("gxp:if".equals(tagName)) {
          handleIf(
              tChain.cast(DomTree.Tag.class), tgtChain, inAttrib, escaping, b);
        } else if ("gxp:eval".equals(tagName)) {
          handleEval(tChain.cast(DomTree.Tag.class), tgtChain, escaping, b);
        } else if ("gxp:loop".equals(tagName)) {
          handleLoop(
              tChain.cast(DomTree.Tag.class), tgtChain, inAttrib, escaping, b);
        } else if (tagName.startsWith("call:")) {
          if (inAttrib) {
            throw new BadContentException(
                new Message(PluginMessageType.TAG_NOT_ALLOWED_IN_ATTRIBUTE,
                            el.getFilePosition(),
                            MessagePart.Factory.valueOf("<" + tagName + ">")));
          }
          assert escaping != JsWriter.Esc.NONE;
          handleCall(el, tgtChain, b);
        } else {
          mq.addMessage(
              PluginMessageType.UNKNOWN_TAG, el.getFilePosition(),
              MessagePart.Factory.valueOf(tagName));
          return;
        }
      } else {
        assertNotBlacklistedTag(el);

        assert escaping != JsWriter.Esc.NONE;

        DomAttributeConstraint constraint =
          DomAttributeConstraint.Factory.forTag(tagName);

        tagName = assertHtmlIdentifier(tagName, el);
        if (inAttrib) {
          throw new BadContentException(
              new Message(PluginMessageType.TAG_NOT_ALLOWED_IN_ATTRIBUTE,
                          el.getFilePosition(),
                          MessagePart.Factory.valueOf("<" + tagName + ">")));
        }
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
          // Output parameters
          for (i = 0; i < children.size(); ++i) {
            DomTree child = children.get(i);
            if (HtmlTokenType.ATTRNAME != child.getType()) { break; }
            DomTree.Attrib attrib = (DomTree.Attrib) child;
            String name = attrib.getAttribName();
            DomTree.Value valueT = attrib.getAttribValueNode();

            boolean isExpr = false;
            if (name.startsWith("expr:")) {
              isExpr = true;
              name = name.substring("expr:".length());
            }
            name = assertHtmlIdentifier(name, attrib);

            Pair<String, String> wrapper = constraint.attributeValueHtml(name);
            if (null == wrapper) { continue; }

            if (!isExpr) {
              if ("style".equalsIgnoreCase(name)) {
                compileStyleAttrib(attrib, tgtChain, b);
              } else {
                HTML.Attribute a = htmlSchema.lookupAttribute(tagName, name);
                AttributeXform xform = xformForAttribute(a);

                StringBuilder buf = new StringBuilder();
                buf.append(' ').append(name).append("=\"");
                Escaping.escapeXml(wrapper.a, false, buf);
                if (null != xform) {
                  JsWriter.appendString(buf.toString(), tgtChain, b);
                  buf.setLength(0);
                  xform.apply(a, attrib, this, tgtChain, b);
                } else {
                  Escaping.escapeXml(valueT.getValue(), false, buf);
                }
                Escaping.escapeXml(wrapper.b, false, buf);
                buf.append("\"");
                JsWriter.appendString(buf.toString(), tgtChain, b);
              }
            } else {   // Handle expr:foo="<expression>"

              String wrapperFn = null;
                HTML.Attribute a = htmlSchema.lookupAttribute(tagName, name);
                AttributeXform xform = xformForAttribute(a);
              if (null != xform) {
                try {
                  wrapperFn = xform.runtimeFunction(
                      tagName, name, attrib, this);
                } catch (BadContentException ex) {
                  ex.toMessageQueue(mq);
                  continue;
                }
              }

              JsWriter.appendString(
                  " " + name + "=\"" + JsWriter.htmlEscape(wrapper.a),
                  tgtChain, b);
              if (null == wrapperFn) {
                JsWriter.append(asExpression(valueT), tgtChain, b);
              } else {
                // Wrap the expression in a wrapper function
                Operation e = TreeConstruction.call(
                    TreeConstruction.memberAccess(
                        ReservedNames.OUTERS, wrapperFn),
                    asExpression(valueT));
                JsWriter.append(e, tgtChain, b);
              }
              JsWriter.appendString(
                  JsWriter.htmlEscape(wrapper.b) + "\"", tgtChain, b);
            }
            constraint.attributeDone(name);
          }

          // Handle any gxp:attr children
          for (DomTree child : children.subList(i, children.size())) {
            if (HtmlTokenType.TAGBEGIN == child.getType()
                && "gxp:attr".equals(child.getValue())) {
              DomTree.Tag attrEl = (DomTree.Tag) child;
              AncestorChain<DomTree.Tag> attrElChain
                  = new AncestorChain<DomTree.Tag>(tChain, attrEl);
              Map<String, DomTree.Value> attribMap =
                new HashMap<String, DomTree.Value>();
              int s = gxpValidator.attribsAsMap(
                  attrEl, attribMap, ALLOWED_ATTR_PARAMS);

              DomTree.Value nameT = attribMap.get("name");
              if (null == nameT) {
                mq.addMessage(PluginMessageType.MISSING_ATTRIBUTE,
                              attrEl.getFilePosition(),
                              MessagePart.Factory.valueOf("name"), t);
                continue;
              }
              String name = assertHtmlIdentifier(nameT.getValue(), nameT);
              HTML.Attribute a = htmlSchema.lookupAttribute(tagName, name);
              AttributeXform xform = xformForAttribute(a);
              String wrapperFn = null;
              if (null != xform) {
                try {
                  wrapperFn = xform.runtimeFunction(
                      tagName, name, attrEl, this);
                } catch (BadContentException ex) {
                  mq.addMessage(PluginMessageType.ATTRIBUTE_CANNOT_BE_DYNAMIC,
                                attrEl.getFilePosition(),
                                MessagePart.Factory.valueOf(name),
                                MessagePart.Factory.valueOf(tagName));
                  continue;
                }
              }

              Pair<String, String> wrapper
                  = constraint.attributeValueHtml(name);
              if (null == wrapper) { continue; }

              JsWriter.appendString(
                  " " + name + "=\"" + JsWriter.htmlEscape(wrapper.a),
                  tgtChain, b);
              List<? extends DomTree> attrTrimmed = attrEl.children();
              attrTrimmed = attrTrimmed.subList(s, attrTrimmed.size());
              if (null == wrapperFn) {
                for (DomTree attr : attrTrimmed) {
                  compileDom(new AncestorChain<DomTree>(attrElChain, attr),
                             tgtChain, true, JsWriter.Esc.HTML_ATTRIB, b);
                }
              } else {
                // We need to collect in a separate list before processing and
                // converting to html
                String synthId = syntheticId();
                // var <synthId> = [];
                b.insertBefore(
                    s(new Declaration(
                          new Identifier(synthId),
                          s(new ArrayConstructor(
                                Collections.<Expression>emptyList())))), null);
                for (DomTree attr : attrTrimmed) {
                  compileDom(new AncestorChain<DomTree>(attrElChain, attr),
                             Arrays.asList(synthId, "push"), true,
                             JsWriter.Esc.NONE, b);
                }
                // <namespace>.plugin_htmlAttr___(<wrapper>(<synthId>.join('')))
                JsWriter.append(
                    TreeConstruction.call(
                        TreeConstruction.memberAccess(
                            ReservedNames.OUTERS, "plugin_htmlAttr___"),
                        TreeConstruction.call(
                            TreeConstruction.memberAccess(
                                ReservedNames.OUTERS, wrapperFn),
                            TreeConstruction.call(
                                TreeConstruction.memberAccess(synthId, "join"),
                                s(new StringLiteral("''"))))),
                    tgtChain, b);
              }
              JsWriter.appendString(
                  JsWriter.htmlEscape(wrapper.b) + "\"", tgtChain, b);
              constraint.attributeDone(name);
            }
          }

          for (Pair<String, String> extra : constraint.tagDone(el)) {
            JsWriter.appendString(
                " " + extra.a + "=\"" + JsWriter.htmlEscape(extra.b) + "\"",
                tgtChain, b);
          }

          JsWriter.appendString(">", tgtChain, b);

          // Recurse to contents
          boolean wroteChildElement = false;
          if (tagAllowsContent(tagName)) {
            for (DomTree child : children.subList(i, children.size())) {
              if (!(HtmlTokenType.TAGBEGIN == child.getType()
                    && "gxp:attr".equals(child.getValue()))) {
                compileDom(new AncestorChain<DomTree>(tChain, child),
                           tgtChain, false, JsWriter.Esc.HTML, b);
                wroteChildElement = true;
              }
            }
          } else {
            for (DomTree child : children.subList(i, children.size())) {
              if (!isWhitespaceTextNode(child) && !isGxpAttrElement(child)) {
                mq.addMessage(MessageType.MALFORMED_XHTML,
                              child.getFilePosition(), child);
              }
            }
          }

          if (wroteChildElement || requiresCloseTag(tagName)) {
            JsWriter.appendString("</" + tagName + ">", tgtChain, b);
          }
        }
      }
      break;
    default:
      throw new AssertionError(t.getType().name());
    }
  }

  private static final Pattern HTML_ID = Pattern.compile(
      "^[a-z][a-z0-9-]*$", Pattern.CASE_INSENSITIVE);
  private static String assertHtmlIdentifier(String s, DomTree node)
      throws BadContentException {
    if (!HTML_ID.matcher(s).matches()) {
      throw new BadContentException(
          new Message(PluginMessageType.BAD_IDENTIFIER, node.getFilePosition(),
                      MessagePart.Factory.valueOf(s)));
    }
    return s;
  }
  private static final Pattern JS_ID = Pattern.compile(
      "^[a-z][_a-z0-9]*$", Pattern.CASE_INSENSITIVE);
  private static String assertSafeJsIdentifier(String s, DomTree node)
      throws BadContentException {
    if (!JS_ID.matcher(s).matches() || s.endsWith("___")) {
      throw new BadContentException(
          new Message(PluginMessageType.BAD_IDENTIFIER, node.getFilePosition(),
                      MessagePart.Factory.valueOf(s)));
    }
    return s;
  }

  private void assertNotBlacklistedTag(DomTree node)
      throws BadContentException {
    String tagName = node.getValue().toLowerCase();
    if (!htmlSchema.isElementAllowed(tagName)) {
      throw new BadContentException(
          new Message(MessageType.MALFORMED_XHTML, node.getFilePosition(),
                    MessagePart.Factory.valueOf(tagName)));
    }
  }

  private boolean requiresCloseTag(String tag) {
    HTML.Element e = htmlSchema.lookupElement(tag.toLowerCase());
    return null == e || !e.isEmpty();
  }

  private boolean tagAllowsContent(String tag) {
    HTML.Element e = htmlSchema.lookupElement(tag.toLowerCase());
    return null == e || !e.isEmpty();
  }

  private static final Set<String> ALLOWED_IF_PARAMS =
      Collections.singleton("cond");
  private static final Set<String> ALLOWED_LOOP_PARAMS = new HashSet<String>(
      Arrays.asList("var", "iterator"));
  private static final Set<String> ALLOWED_EVAL_PARAMS =
      Collections.singleton("expr");
  private static final Set<String> ALLOWED_PARAM_PARAMS =
      Collections.singleton("name");
  private static final Set<String> ALLOWED_TEMPLATE_PARAMS =
      Collections.singleton("name");
  private static final Set<String> ALLOWED_ATTR_PARAMS =
      Collections.singleton("name");

  private static String trimText(AncestorChain<DomTree.Text> tChain) {
    DomTree.Text t = tChain.node;
    // Trim any text on the inside or outside of a gxp tag
    DomTree prev, next;
    ParseTreeNode parent = tChain.getParentNode();
    if (parent != null) {
      List<? extends ParseTreeNode> siblings = parent.children();
      int childIdx = siblings.indexOf(t);
      prev = childIdx > 0 ? (DomTree) siblings.get(childIdx - 1) : null;
      next = (childIdx + 1 < siblings.size()
              ? (DomTree) siblings.get(childIdx + 1) : null);
    } else {
      prev = next = null;
    }
    boolean isParentGxp = parent instanceof DomTree.Tag
                          && GxpValidator.isGxp(((DomTree) parent).getValue());
    boolean isFirstChild = null == prev || prev instanceof DomTree.Attrib;
    boolean isLastChild = null == next;

    boolean trimFromLeft =
      (prev instanceof DomTree.Tag && GxpValidator.isGxp(prev.getValue())
          || (isParentGxp && isFirstChild));
    boolean trimFromRight =
      (next instanceof DomTree.Tag && GxpValidator.isGxp(next.getValue())
          || (isParentGxp && isLastChild));

    // TODO(msamuel): Do not trim around <gxp:eval>.  Trim around it if there is
    // other evidence, such as first node of a <gxp:if>

    String value = t.getValue();
    if (!(trimFromLeft | trimFromRight)) { return value; }
    int s = 0, e = value.length();
    if (trimFromLeft) {
      while (s < e && Character.isWhitespace(value.charAt(s))) { ++s; }
    }
    if (trimFromRight) {
      while (e > s && Character.isWhitespace(value.charAt(e - 1))) { --e; }
    }
    return value.substring(s, e);
  }

  private void handleIf(
      AncestorChain<DomTree.Tag> tChain, List<String> tgtChain,
      boolean inAttrib, JsWriter.Esc escaping, Block b)
      throws BadContentException {
    DomTree.Tag t = tChain.node;

    // Should have one parameter exactly -- the condition
    List<? extends DomTree> children = t.children();
    Map<String, DomTree.Value> attribMap = new HashMap<String, DomTree.Value>();
    int attribEnd = gxpValidator.attribsAsMap(t, attribMap, ALLOWED_IF_PARAMS);
    DomTree.Value cond = attribMap.get("cond");
    if (null == cond) {
      mq.addMessage(PluginMessageType.MISSING_ATTRIBUTE, t.getFilePosition(),
                    MessagePart.Factory.valueOf("cond"), t);
      return;
    }

    int alternativeStart = children.size();
    Block alternative = null;
    for (int i = attribEnd; i < children.size(); ++i) {
      DomTree child = children.get(i);
      if (HtmlTokenType.TAGBEGIN == child.getType()
          && "gxp:else".equals(child.getValue())) {
        alternativeStart = i;
        alternative = s(new Block(Collections.<Statement>emptyList()));
        break;
      }
    }

    Block body = s(new Block(Collections.<Statement>emptyList()));
    Conditional c =
      s(new Conditional(
            Collections.singletonList(
                new Pair<Expression, Statement>(asExpression(cond), body)),
            alternative));
    b.insertBefore(c, null);
    for (DomTree child : children.subList(attribEnd, alternativeStart)) {
      compileDom(new AncestorChain<DomTree>(tChain, child),
                 tgtChain, inAttrib, escaping, body);
    }
    if (null != alternative) {
      for (DomTree child :
               children.subList(alternativeStart + 1, children.size())) {
        compileDom(new AncestorChain<DomTree>(tChain, child), tgtChain,
                   inAttrib, escaping, alternative);
      }
    }
  }

  private void handleLoop(
      AncestorChain<DomTree.Tag> tChain, List<String> tgtChain,
      boolean inAttrib, JsWriter.Esc escaping, Block b)
      throws BadContentException {
    DomTree.Tag t = tChain.node;
    // Should have two parameters -- the iterable and the variable
    Map<String, DomTree.Value> attribMap = new HashMap<String, DomTree.Value>();
    int attribEnd = gxpValidator.attribsAsMap(
        t, attribMap, ALLOWED_LOOP_PARAMS);
    DomTree.Value variable = attribMap.get("var"),
                  iterator = attribMap.get("iterator");

    if (null == variable || null == iterator) {
      if (null == variable) {
        mq.addMessage(
            PluginMessageType.MISSING_ATTRIBUTE,
            t.getFilePosition(), MessagePart.Factory.valueOf("var"), t);
      }
      if (null == iterator) {
        mq.addMessage(
            PluginMessageType.MISSING_ATTRIBUTE,
            t.getFilePosition(), MessagePart.Factory.valueOf("iterator"), t);
      }
      return;
    }
    String variableName = variable.getValue();

    // Generate code like
    // var <autogen> = <expr>;
    // if (<autogen>) {
    //   for (var <autogen2> in <autogen>) {
    //     var <var> = <autogen>[k];
    //     <body>
    //   }
    // }
    String iteratorId = syntheticId(),
              keyName = syntheticId();
    b.appendChild(s(new Declaration(s(new Identifier(iteratorId)),
                                    asExpression(iterator))));

    // TODO(mikesamuel): use the proper rewriting rule for this when ihab
    // gets around to implementing foreach loops.
    Block forEachBody = s(new Block(Collections.<Statement>emptyList()));
    forEachBody.createMutation().appendChild(
        s(new Conditional(Collections.singletonList(
              new Pair<Expression, Statement>(
              s(new Operation(
                    Operator.NOT,
                    TreeConstruction.call(
                        TreeConstruction.memberAccess("___", "canEnumPub"),
                        s(new Reference(s(new Identifier(iteratorId)))),
                        s(new Reference(s(new Identifier(keyName))))
                        )
                    )),
              s(new ContinueStmt(null)))),
              null)))
          .appendChild(
              s(new Declaration(
                    new Identifier(variableName),
                    s(new Operation(
                          Operator.SQUARE_BRACKET,
                          s(new Reference(s(new Identifier(iteratorId)))),
                          s(new Reference(s(new Identifier(keyName))))
                          ))
                    )))
          .execute();

    Block ifBody = s(new Block(Arrays.asList(
        s(new ForEachLoop(
              null,
              s(new Declaration(s(new Identifier(keyName)), null)),
              s(new Reference(s(new Identifier(iteratorId)))),
              forEachBody)))));

    Conditional iteratorCheck = s(new Conditional(
        Collections.singletonList(
            new Pair<Expression, Statement>(
                s(new Reference(s(new Identifier(iteratorId)))),
                ifBody)
            ), null));
    b.appendChild(iteratorCheck);

    List<? extends DomTree> children = t.children();
    for (DomTree child : children.subList(attribEnd, children.size())) {
      compileDom(new AncestorChain<DomTree>(tChain, child),
                 tgtChain, inAttrib, escaping, forEachBody);
    }
  }

  private void handleEval(
      AncestorChain<DomTree.Tag> tChain, List<String> tgtChain,
      JsWriter.Esc escaping, Block b) {
    DomTree.Tag t = tChain.node;
    // Should have one parameters -- the expression
    Map<String, DomTree.Value> attribMap = new HashMap<String, DomTree.Value>();
    int attribEnd = gxpValidator.attribsAsMap(
        t, attribMap, ALLOWED_EVAL_PARAMS);
    DomTree.Value expr = attribMap.get("expr");

    if (null == expr) {
      mq.addMessage(
          PluginMessageType.MISSING_ATTRIBUTE,
          t.getFilePosition(), MessagePart.Factory.valueOf("expr"), t);
      return;
    }

    if (t.children().size() > attribEnd) {
      mq.addMessage(PluginMessageType.EXTRANEOUS_CONTENT, t.getFilePosition(),
                    t);
      return;
    }

    Expression e = asExpression(expr);
    String fnName;
    switch (escaping) {
      case HTML:
        fnName = "plugin_html___";
        break;
      case HTML_ATTRIB:
        fnName = "plugin_htmlAttr___";
        break;
      default:
        fnName = null;
        break;
    }
    if (fnName != null) {
      e = TreeConstruction.call(
              TreeConstruction.memberAccess(ReservedNames.OUTERS, fnName), e);
    }
    JsWriter.append(e, tgtChain, b);
  }

  private void handleCall(DomTree.Tag t, List<String> tgtChain, Block b) {
    String templateName = t.getValue().substring("call:".length());
    TemplateSignature sig = sigs.get(templateName);
    if (null == sig) {
      mq.addMessage(PluginMessageType.NO_SUCH_TEMPLATE, t.getFilePosition(),
                    MessagePart.Factory.valueOf(templateName));
      return;
    }

    boolean bad = false;
    // Should have one parameter per template parameter
    Map<String, DomTree.Value> attribMap = new HashMap<String, DomTree.Value>();
    gxpValidator.attribsAsMap(t, attribMap, null);
    int nParams = sig.parameterNames.size();
    Expression[] operands = new Expression[nParams + 2];
    for (String paramName : attribMap.keySet()) {
      DomTree.Value actual = attribMap.get(paramName);
      int paramIndex = -1;
      for (int i = nParams; --i >= 0;) {
        if (paramName.equals(sig.parameterNames.get(i).getValue())) {
          paramIndex = i;
          break;
        }
      }
      if (paramIndex < 0) {
        mq.addMessage(PluginMessageType.UNKNOWN_TEMPLATE_PARAM,
                      actual.getFilePosition(),
                      MessagePart.Factory.valueOf(templateName), sig.loc,
                      MessagePart.Factory.valueOf(paramName));
        bad = true;
      } else {
        operands[paramIndex + 2] = asExpression(actual);
      }
    }

    for (int i = 0; i < nParams; ++i) {
      if (null == operands[i + 2]) {
        DomTree.Value formal = sig.parameterNames.get(i);
        String paramName = sig.parameterNames.get(i).getValue();
        mq.addMessage(PluginMessageType.MISSING_TEMPLATE_PARAM,
                      t.getFilePosition(),
                      MessagePart.Factory.valueOf(templateName),
                      MessagePart.Factory.valueOf(paramName),
                      formal.getFilePosition());
        bad = true;
      }
    }

    if (bad) { return; }

    // Append <assignedName>.call(___OUTERS___, <param 0>, ...);
    operands[0] = TreeConstruction.memberAccess(sig.assignedName, "call");
    operands[1] = s(new Reference(s(new Identifier(ReservedNames.OUTERS))));
    Operation call = s(new Operation(Operator.FUNCTION_CALL, operands));
    JsWriter.append(call, tgtChain, b);
  }

  private void compileStyleAttrib(
      DomTree.Attrib attrib, List<String> tgtChain, Block b)
      throws BadContentException {
    CssTree decls;
    try {
      decls = parseStyleAttrib(attrib);
    } catch (ParseException ex) {
      throw new BadContentException(ex.getCajaMessage(), ex);
    }

    // The validator will check that property values are well-formed,
    // marking those that aren't, and identifies all urls.
    CssValidator cssValidator = new CssValidator(cssSchema, htmlSchema, mq);
    boolean valid = cssValidator.validateCss(new AncestorChain<CssTree>(decls));
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

  private CssTree.DeclarationGroup parseStyleAttrib(DomTree.Attrib t)
      throws ParseException {
    // Parse the attribute value as CSS
    DomTree.Value value = t.getAttribValueNode();
    // Use the raw value so that the file positions come out right in
    // CssValidator error messages.
    String cssAsHtml = GxpCompiler.deQuote(value.getToken().text);
    // The raw value is html so we wrap it in an html unescaper
    CharProducer cp = CharProducer.Factory.fromHtmlAttribute(
        CharProducer.Factory.create(
            new StringReader(cssAsHtml), value.getFilePosition()));
    // Parse the css as a set of declarations separated by semicolons
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

  private Expression asExpression(DomTree.Value expr) {
    FilePosition pos = expr.getFilePosition();
    String src = deQuote(expr.getToken().text);
    CharProducer cp =
      CharProducer.Factory.fromHtmlAttribute(CharProducer.Factory.create(
          new StringReader(src), pos));
    return JsWriter.asExpression(cp, pos, mq);
  }

  private Block asBlock(DomTree stmt) {
    // Parse as a javascript expression.
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

    // Expression will be sanitized in a later pass

    return b;
  }

  /**
   * Produces an identifier that will not collide with any previously generated
   * identifier.
   */
  private String syntheticId() {
    return "c" + (++syntheticIdCounter) + "___";
  }

  /** Is the given node a text node that consists only of whitespace? */
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

  public static final class BadContentException extends CajaException {
    private static final long serialVersionUID = -5317800396186044550L;
    BadContentException(Message m) { super(m); }
    BadContentException(Message m, Throwable th) { super(m, th); }
  }

  /** Encapsulates a GXP templates name, and parameter declarations. */
  public static final class TemplateSignature {
    final String templateName;
    final String assignedName;
    final List<DomTree.Value> parameterNames;
    final List<AncestorChain<? extends DomTree>> content;
    final FilePosition loc;

    TemplateSignature(
        String templateName,
        String assignedName,
        List<DomTree.Value> params,
        List<AncestorChain<? extends DomTree>> content,
        FilePosition loc) {
      this.templateName = templateName;
      this.assignedName = assignedName;
      this.parameterNames = params;
      this.content = content;
      this.loc = loc;
    }

    public String getTemplateName() { return templateName; }
    public String getAssignedName() { return assignedName; }
    public List<? extends DomTree> getParameterNames() {
      return Collections.unmodifiableList(parameterNames);
    }
    public List<AncestorChain<? extends DomTree>> getContent() {
      return content;
    }
    public FilePosition getLocation() { return loc; }
  }

  /**
   * For a given html attribute, what kind of transformation do we have to
   * perform on the value?
   */
  private AttributeXform xformForAttribute(HTML.Attribute attrib) {
    if (null != attrib) {
      switch (attrib.getType()) {
      case STYLE:
        return AttributeXform.STYLE;
      case SCRIPT:
        return AttributeXform.SCRIPT;
      case URI:
        return AttributeXform.URI;
      case CLASSES: case ID: case IDREF: case IDREFS: case GLOBAL_NAME:
        return AttributeXform.NAMES_IDS_AND_CLASSES;
      case LOCAL_NAME:
        return null;
      default:
        return null;
      }
    }
    return null;
  }

  /** Is an html element with the given name a form element? */
  private static boolean isInput(String tagName) {
    tagName = tagName.toLowerCase();
    return "input".equals(tagName) || "select".equals(tagName)
        || "textarea".equals(tagName) || "map".equals(tagName)
        || "button".equals(tagName);
  }

  /**
   * Encapsulates a transformation on an html attribute value.
   * Some transformations are performed at compile time, and some may be
   * performed at runtime.
   */
  private static enum AttributeXform {
    NAMES_IDS_AND_CLASSES {
      @Override
      void apply(HTML.Attribute typeInfo, DomTree.Attrib t, GxpCompiler gxpc,
                 List<String> tgtChain, Block b) {
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
              sb.append(gxpc.meta.namespacePrefix).append('-');
              pos = i;
            } else {
              sb.append(nmTokens.substring(pos, i));
            }
            wasSpace = space;
          }
        }
        if (!wasSpace) { sb.append(nmTokens.substring(pos)); }
        JsWriter.appendString(JsWriter.htmlEscape(sb.toString()), tgtChain, b);
      }
      @Override
      String runtimeFunction(String tagName, String attribName, DomTree t,
                             GxpCompiler gxpc) {
        return "plugin_prefix___";
      }
    },
    URI {
      @Override
      void apply(HTML.Attribute typeInfo, DomTree.Attrib t, GxpCompiler gxpc,
                 List<String> tgtChain, Block b)
          throws BadContentException {
        String uriStr = t.getAttribValue();
        try {
          URI uri = new URI(uriStr);
          ExternalReference ref = new ExternalReference(
              uri, t.getFilePosition());
          String mimeType = typeInfo.getMimeTypes();
          if (mimeType == null) { mimeType = "*/*"; }

          String xuri = gxpc.meta.getPluginEnvironment().rewriteUri(
              ref, mimeType);
          if (xuri == null) {
            throw new BadContentException(new Message(
                PluginMessageType.DISALLOWED_URI,
                t.getFilePosition(), MessagePart.Factory.valueOf(uriStr)));
          }
          JsWriter.appendString(JsWriter.htmlEscape(xuri), tgtChain, b);
        } catch (URISyntaxException ex) {
          throw new BadContentException(new Message(
              PluginMessageType.MALFORMED_URL, t.getFilePosition(),
              MessagePart.Factory.valueOf(uriStr)
              ), ex);
        }
      }
      @Override
      String runtimeFunction(String tagName, String attribName, DomTree t,
                             GxpCompiler gxpc) {
        return "plugin_checkUriRelative___";
      }
    },
    STYLE {
      @Override
      void apply(HTML.Attribute typeInfo, DomTree.Attrib t, GxpCompiler gxpc,
                 List<String> tgtChain, Block b) {
        // Should be handled in compileDOM
        throw new AssertionError();
      }
      @Override
      String runtimeFunction(String tagName, String attribName, DomTree t,
                             GxpCompiler gxpc)
          throws BadContentException {
        throw new BadContentException(new Message(
            PluginMessageType.ATTRIBUTE_CANNOT_BE_DYNAMIC, t.getFilePosition(),
            MessagePart.Factory.valueOf(tagName),
            MessagePart.Factory.valueOf(attribName)));
      }
    },
    SCRIPT {
      @Override
      void apply(HTML.Attribute typeInfo, DomTree.Attrib t, GxpCompiler gxpc,
                 List<String> tgtChain, Block b) {
        // Extract the handler into a function so that it can be analyzed.
        Block handler = gxpc.asBlock(t.getAttribValueNode());

        String handlerFnName = gxpc.syntheticId();
        gxpc.eventHandlers.put(
            handlerFnName,
            new FunctionDeclaration(
                new Identifier(handlerFnName),
                s(new FunctionConstructor(
                      new Identifier(null),
                      Arrays.asList(
                          s(new FormalParam(
                                s(new Identifier(ReservedNames.THIS_NODE)))),
                          s(new FormalParam(s(new Identifier("event"))))),
                      // TODO(mikesamuel): replace instances of this in handler
                      // with synthetic references to ReservedNames.THIS_NODE
                      handler))));

        // ' foo="return plugin_dispatchevent___(this, event || window.event, ',
        // ___.getId(___OUTERS___), ', \'myHandlerFn\');"'
        JsWriter.appendString(
            "return plugin_dispatchEvent___(this, event || window.event, ",
            tgtChain, b);
        JsWriter.append(
            TreeConstruction.call(
                TreeConstruction.memberAccess("___", "getId"),
                s(new Reference(s(new Identifier(ReservedNames.OUTERS))))),
            tgtChain, b);
        JsWriter.appendString(
            ", " + StringLiteral.toQuotedValue(handlerFnName) + ");",
            tgtChain, b);
      }
      @Override
      String runtimeFunction(String tagName, String attribName, DomTree t,
                             GxpCompiler gxpc)
          throws BadContentException {
        throw new BadContentException(new Message(
            PluginMessageType.ATTRIBUTE_CANNOT_BE_DYNAMIC, t.getFilePosition(),
            MessagePart.Factory.valueOf(tagName),
            MessagePart.Factory.valueOf(attribName)));
      }
    },
    ;
    /**
     * Apply, at compile time, any preprocessing steps to the given attributes
     * value.
     */
    abstract void apply(
        HTML.Attribute typeInfo, DomTree.Attrib t, GxpCompiler gxpc,
        List<String> tgtChain, Block b)
        throws BadContentException;
    /**
     * Given an attribute name, the gxp attribute that specifies it, and the
     * compiler, return the name of a function that will take the attribute
     * value and the namespace prefix and return the processed version of the
     * value.
     */
    abstract String runtimeFunction(
        String tagName, String attribName, DomTree nameNode, GxpCompiler gxpc)
        throws BadContentException;
  }
}
