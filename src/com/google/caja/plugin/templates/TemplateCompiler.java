// Copyright (C) 2009 Google Inc.
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

package com.google.caja.plugin.templates;

import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HTML;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.Keyword;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.SyntheticNodes;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.parser.quasiliteral.ReservedNames;
import com.google.caja.plugin.CssRewriter;
import com.google.caja.plugin.CssValidator;
import com.google.caja.plugin.ExtractedHtmlContent;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Name;
import com.google.caja.util.Pair;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Compiles an HTML document to a chunk of safe static HTML, and a bit of
 * javascript which attaches event handlers and other dynamic attributes, and
 * executes inline scripts.
 *
 * <p>
 * Requires that CSS be rewritten, that inline scripts have been
 * {@link ExtractedHtmlContent extracted}, and that the output JS be run through
 * the CajitaRewriter.
 *
 * @author mikesamuel@gmail.com
 */
public class TemplateCompiler {
  private final List<Node> ihtmlRoots;
  private final List<CssTree.StyleSheet> safeStylesheets;
  private final CssSchema cssSchema;
  private final HtmlSchema htmlSchema;
  private final PluginMeta meta;
  private final MessageContext mc;
  private final MessageQueue mq;
  /**
   * Maps {@link Node}s to JS parse trees.
   *
   * <ul>
   *
   * <li>If the value is {@code null}, then the literal value in the
   * original parse tree may be used.</li>
   *
   * <li>If the node is an {@code Element}, then the value is an expression
   * that returns a tag name.</li>
   *
   * <li>If the node is an attribute, then the value is an expression
   * that returns a (key, value) pair.</li>
   *
   * <li>If the node is a text node inside ascript block, then the value is an
   * {@link UncajoledModule}.</li>
   *
   * <li>Otherwise, the value is a JavaScript expression which evaluates to the
   * dynamic text value.</li>
   *
   * </ul>
   */
  private final Map<Node, ParseTreeNode> scriptsPerNode
      = new IdentityHashMap<Node, ParseTreeNode>();
  /** Extracted event handler functions. */
  private final List<Statement> handlers = new ArrayList<Statement>();
  /** Maps handler attribute source to handler names. */
  private final Map<String, String> handlerCache
      = new HashMap<String, String>();

  /**
   * @param ihtmlRoots roots of trees to process.
   * @param safeStylesheets CSS style-sheets that have had unsafe
   *     constructs removed and had rules rewritten.
   * @param meta specifies how URLs and other attributes are rewritten.
   * @param cssSchema specifies how STYLE attributes are rewritten.
   * @param htmlSchema specifies how elements and attributes are handled.
   * @param mq receives messages about invalid attribute values.
   */
  public TemplateCompiler(
      List<? extends Node> ihtmlRoots,
      List<? extends CssTree.StyleSheet> safeStylesheets,
      CssSchema cssSchema, HtmlSchema htmlSchema,
      PluginMeta meta, MessageContext mc, MessageQueue mq) {
    this.ihtmlRoots = new ArrayList<Node>(ihtmlRoots);
    this.safeStylesheets = new ArrayList<CssTree.StyleSheet>(safeStylesheets);
    this.cssSchema = cssSchema;
    this.htmlSchema = htmlSchema;
    this.meta = meta;
    this.mc = mc;
    this.mq = mq;
  }

  /**
   * Examines the HTML document and writes messages about problematic portions
   * to the message queue passed to the constructor.
   */
  private void inspect() {
    if (!mq.hasMessageAtLevel(MessageLevel.FATAL_ERROR)) {
      for (Node ihtmlRoot : ihtmlRoots) {
        inspect(ihtmlRoot, Name.html("div"));
      }
    }
  }

  private void inspect(Node n, Name containingHtmlElement) {
    switch (n.getNodeType()) {
      case Node.ELEMENT_NODE:
        inspectElement((Element) n, containingHtmlElement);
        break;
      case Node.TEXT_NODE: case Node.CDATA_SECTION_NODE:
        inspectText((Text) n, containingHtmlElement);
        break;
      case Node.DOCUMENT_FRAGMENT_NODE:
        inspectFragment((DocumentFragment) n, containingHtmlElement);
        break;
      default:
        // Since they don't show in the scriptsPerNode map, they won't appear in
        // any output trees.
        break;
    }
  }

  /**
   * @param containingHtmlElement the name of the HTML element containing el.
   *     If the HTML element is contained inside a template construct then this
   *     name may differ from el's immediate parent.
   */
  private void inspectElement(Element el, Name containingHtmlElement) {
    Name elName = Name.html(el.getTagName());

    // Recurse early so that ihtml:dynamic elements have been parsed before we
    // process the attributes element list.
    for (Node child : Nodes.childrenOf(el)) {
      inspect(child, elName);
    }

    // For each attribute allowed on this element type, ensure that
    // (1) If it is not specified, and its default value is not allowed, then
    //     it is added with a known safe value.
    // (2) Its value is rewritten as appropriate.
    // We don't have to worry about disallowed attributes since those will
    // not be present in scriptsPerNode.  The TemplateSanitizer should have
    // stripped those out.  The TemplateSanitizer should also have stripped out
    // disallowed elements.
    if (!htmlSchema.isElementAllowed(elName)) { return; }

    HTML.Element elInfo = htmlSchema.lookupElement(elName);
    List<HTML.Attribute> attrs = elInfo.getAttributes();
    if (attrs != null) {
      for (HTML.Attribute a : attrs) {
        Name attrName = a.getAttributeName();
        if (!htmlSchema.isAttributeAllowed(elName, attrName)) { continue; }
        HTML.Attribute attrInfo = htmlSchema.lookupAttribute(elName, attrName);
        String attrNameStr = attrName.getCanonicalForm();
        Attr attr = null;
        if (el.hasAttribute(attrNameStr)
            && attrInfo.getValueCriterion().accept(
                el.getAttribute(attrNameStr))) {
          attr = el.getAttributeNode(attrNameStr);
        } else if ((a.getDefaultValue() != null
                    && !a.getValueCriterion().accept(a.getDefaultValue()))
                   || !a.isOptional()) {
          attr = el.getOwnerDocument().createAttribute(attrNameStr);
          String safeValue;
          if (a.getType() == HTML.Attribute.Type.URI) {
            safeValue = Nodes.getFilePositionFor(el)
                .source().getUri().toString();
          } else {
            safeValue = a.getSafeValue();
          }
          if (safeValue == null) {
            mq.addMessage(IhtmlMessageType.MISSING_ATTRIB,
                Nodes.getFilePositionFor(el),
                MessagePart.Factory.valueOf(elName.toString()),
                MessagePart.Factory.valueOf(attrNameStr));
            continue;
          }
          attr.setNodeValue(safeValue);
          el.setAttributeNode(attr);
        }
        if (attr != null) {
          inspectHtmlAttribute(attr, attrInfo);
        }
      }
    }
    scriptsPerNode.put(el, null);
  }

  private void inspectText(Text t, Name containingHtmlElement) {
    if (!htmlSchema.isElementAllowed(containingHtmlElement)) { return; }
    scriptsPerNode.put(t, null);
  }

  private void inspectFragment(DocumentFragment f, Name containingHtmlElement) {
    scriptsPerNode.put(f, null);
    for (Node child : Nodes.childrenOf(f)) {
      // We know that top level text nodes in a document fragment
      // are not significant if they are just newlines and indentation.
      // This decreases output size significantly.
      if (isWhitespaceOnlyTextNode(child)) { continue; }
      inspect(child, containingHtmlElement);
    }
  }
  private static boolean isWhitespaceOnlyTextNode(Node child) {
    // This leaves whitespace without a leading EOL character intact.
    // TODO(ihab.awad): Investigate why this is the right criterion to use.
    return child.getNodeType() == Node.TEXT_NODE  // excludes CDATA sections
        && "".equals(child.getNodeValue().replaceAll("[\r\n]+[ \t]*", ""));
  }

  /**
   * For an HTML attribute, decides whether the value is valid according to the
   * schema and if it is valid, sets a value into {@link #scriptsPerNode}.
   * The expression is null if the current value is fine, or a StringLiteral
   * if it can be statically rewritten.
   */
  private void inspectHtmlAttribute(Attr attr, HTML.Attribute info) {
    FilePosition pos = Nodes.getFilePositionForValue(attr);
    String value = attr.getValue();

    Expression dynamicValue;
    switch (info.getType()) {
      case CLASSES:
        if (!checkRestrictedNames(value, pos)) { return; }
        dynamicValue = null;
        break;
      case FRAME_TARGET:
      case LOCAL_NAME:
        if (!checkRestrictedName(value, pos)) { return; }
        dynamicValue = null;
        break;
      case GLOBAL_NAME:
      case ID:
      case IDREF:
        if (!checkRestrictedName(value, pos)) { return; }
        dynamicValue = rewriteIdentifiers(pos, value);
        break;
      case IDREFS:
        if (!checkRestrictedNames(value, pos)) { return; }
        dynamicValue = rewriteIdentifiers(pos, value);
        break;
      case NONE:
        dynamicValue = null;
        break;
      case SCRIPT:
        String handlerFnName = handlerCache.get(attr.getValue());
        if (handlerFnName == null) {
          Block b;
          try {
            b = parseJsFromAttrValue(attr);
          } catch (ParseException ex) {
            ex.toMessageQueue(mq);
            return;
          }
          if (b.children().isEmpty()) { return; }
          rewriteEventHandlerReferences(b);

          handlerFnName = meta.generateUniqueName("c");
          handlers.add(QuasiUtil.quasiStmt(
              ""
              + "IMPORTS___.@handlerName = function ("
              + "    event, " + ReservedNames.THIS_NODE + ") { @body*; };",
              "handlerName", new Reference(SyntheticNodes.s(
                  new Identifier(FilePosition.UNKNOWN, handlerFnName))),
              "body", new ParseTreeNodeContainer(b.children())));
          handlerCache.put(attr.getValue(), handlerFnName);
        }

        dynamicValue = (Expression) QuasiBuilder.substV(
            "'return plugin_dispatchEvent___("
            + "this, event, ' + ___./*@synthetic*/getId(IMPORTS___) + @tail",
            "tail", StringLiteral.valueOf(
                pos, ", " + StringLiteral.toQuotedValue(handlerFnName) + ");"));
        break;
      case STYLE:
        CssTree.DeclarationGroup decls;
        try {
          decls = parseStyleAttrib(attr);
          if (decls == null) { return; }
        } catch (ParseException ex) {
          ex.toMessageQueue(mq);
          return;
        }

        // The validator will check that property values are well-formed,
        // marking those that aren't, and identifies all URLs.
        CssValidator v = new CssValidator(cssSchema, htmlSchema, mq)
            .withInvalidNodeMessageLevel(MessageLevel.WARNING);
        v.validateCss(AncestorChain.instance(decls));
        // The rewriter will remove any unsafe constructs.
        // and put URLs in the proper filename namespace
        new CssRewriter(meta, mq)
            .withInvalidNodeMessageLevel(MessageLevel.WARNING)
            .rewrite(AncestorChain.instance(decls));

        StringBuilder css = new StringBuilder();
        RenderContext rc = new RenderContext(decls.makeRenderer(css, null));
        decls.render(rc);
        rc.getOut().noMoreTokens();

        dynamicValue = StringLiteral.valueOf(pos, css);
        break;
      case URI:
        try {
          URI uri = new URI(value);
          ExternalReference ref = new ExternalReference(uri, pos);
          String rewrittenUri = meta.getPluginEnvironment()
              .rewriteUri(ref, info.getMimeTypes());
          if (rewrittenUri == null) {
            mq.addMessage(
                IhtmlMessageType.MALFORMED_URI, pos,
                MessagePart.Factory.valueOf(uri.toString()));
            return;
          }
          dynamicValue = StringLiteral.valueOf(
              ref.getReferencePosition(), rewrittenUri);
        } catch (URISyntaxException ex) {
          mq.addMessage(
              IhtmlMessageType.MALFORMED_URI, pos,
              MessagePart.Factory.valueOf(value));
          return;
        }
        break;
      default:
        throw new RuntimeException(info.getType().name());
    }
    scriptsPerNode.put(attr, dynamicValue);
  }

  private static final Pattern IDENTIFIER_SEPARATOR = Pattern.compile("\\s+");
  private static final Pattern ALLOWED_NAME = Pattern.compile(
      "^[\\p{Alpha}_:][\\p{Alnum}.\\-_:]*$");
  /** True if value is a valid XML names outside the restricted namespace. */
  private boolean checkRestrictedName(String value, FilePosition pos) {
    assert "".equals(value) || !IDENTIFIER_SEPARATOR.matcher(value).find();
    if (ALLOWED_NAME.matcher(value).find()) { return true; }
    System.err.println("rejected ident `" + value + "`");
    if (!"".equals(value)) {
      mq.addMessage(
          IhtmlMessageType.ILLEGAL_NAME, pos,
          MessagePart.Factory.valueOf(value));
    }
    return false;
  }
  /**
   * True iff value is a space separated group of XML names outside the
   * restricted namespace.
   */
  private boolean checkRestrictedNames(String value, FilePosition pos) {
    if ("".equals(value)) { return true; }
    boolean ok = true;
    for (String ident : IDENTIFIER_SEPARATOR.split(value)) {
      if ("".equals(ident)) { continue; }
      if (!ALLOWED_NAME.matcher(ident).matches()) {
        mq.addMessage(
            IhtmlMessageType.ILLEGAL_NAME, pos,
            MessagePart.Factory.valueOf(ident));
        ok = false;
      }
    }
    return ok;
  }

  /** "foo bar baz" -> "foo-suffix___ bar-suffix___ baz-suffix___". */
  private Expression rewriteIdentifiers(FilePosition pos, String names) {
    if ("".equals(names)) { return null; }
    String[] idents = IDENTIFIER_SEPARATOR.split(names);
    String idClass = meta.getIdClass();
    if (idClass != null) {
      StringBuilder result = new StringBuilder(names.length());
      for (String ident : idents) {
        if ("".equals(ident)) { continue; }
        if (result.length() != 0) { result.append(' '); }
        result.append(ident).append('-').append(idClass);
      }
      return StringLiteral.valueOf(pos, result.toString());
    } else {
      Expression result = null;
      for (String ident : idents) {
        if ("".equals(ident)) { continue; }
        Expression oneRewritten = (Expression) QuasiBuilder.substV(
            "@ident + IMPORTS___.getIdClass___()",
            "ident", StringLiteral.valueOf(
                pos, (result != null ? " " : "") + ident + "-"));
        if (result != null) {
          result = QuasiUtil.concat(result, oneRewritten);
        } else {
          result = oneRewritten;
        }
      }
      return result;
    }
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
  private static void rewriteEventHandlerReferences(Block block) {
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
                r.replaceChild(SyntheticNodes.s(thisNode), oldRef);
              }
              return false;
            }
            return true;
          }
        }, null);
  }

  /**
   * Builds a tree of only the safe HTML parts ignoring IHTML elements.
   * If there are embedded script elements, then these will be removed, and
   * nodes may have synthetic IDs added so that the generated code can split
   * them into the elements present when each script is executed.
   *
   * On introspection, the code will find that the output DOM is missing the
   * SCRIPT elements originally on the page. We consider this a known observable
   * fact of our transformation. If we wish to hid that as well, we could
   * change {@link SafeHtmlMaker} to include empty SCRIPT nodes. However, that
   * would make the output larger -- and, anyway, the text content of these
   * nodes would *still* not be identical to the original.
   *
   * @param doc a DOM {@link Document} object to be used as a factory for DOM
   * nodes; it is not processed or transformed in any way.
   */
  public Pair<Node, List<Block>> getSafeHtml(Document doc) {
    // Inspect the document.
    inspect();

    // Emit safe HTML with JS which attaches dynamic attributes.
    SafeHtmlMaker htmlMaker =
        new SafeHtmlMaker(meta, mc, doc, scriptsPerNode, ihtmlRoots, handlers);
    Pair<Node, List<Block>> htmlAndJs = htmlMaker.make();
    Node html = htmlAndJs.a;
    List<Block> js = htmlAndJs.b;
    Block firstJs;
    if (js.isEmpty()) {
      js.add(firstJs = new Block());
    } else {
      firstJs = js.get(0);
    }
    // Compile CSS to HTML when appropriate or to JS where not.
    // It always ends up at the top either way.
    new SafeCssMaker(html, firstJs, safeStylesheets).make();
    if (firstJs.children().isEmpty()) {
      js.remove(firstJs);
    }
    return Pair.pair(html, js);
  }

  /**
   * Parses an {@code onclick} handler's or other handler's attribute value
   * as a javascript statement.
   */
  private Block parseJsFromAttrValue(Attr attr) throws ParseException {
    FilePosition pos = Nodes.getFilePositionForValue(attr);
    CharProducer cp = fromAttrValue(attr);
    JsTokenQueue tq = new JsTokenQueue(new JsLexer(cp, false), pos.source());
    tq.setInputRange(pos);
    if (tq.isEmpty()) {
      return new Block(pos, Collections.<Statement>emptyList());
    }
    // Parse as a javascript block.
    Block b = new Parser(tq, mq).parse();
    // Block will be sanitized in a later pass.
    b.setFilePosition(pos);
    return b;
  }

  /**
   * Parses a style attribute's value as a CSS declaration group.
   */
  private CssTree.DeclarationGroup parseStyleAttrib(Attr a)
      throws ParseException {
    CharProducer cp = fromAttrValue(a);
    // Parse the CSS as a set of declarations separated by semicolons.
    TokenQueue<CssTokenType> tq = CssParser.makeTokenQueue(cp, mq, false);
    if (tq.isEmpty()) { return null; }
    tq.setInputRange(Nodes.getFilePositionForValue(a));
    CssParser p = new CssParser(tq, mq, MessageLevel.WARNING);
    CssTree.DeclarationGroup decls = p.parseDeclarationGroup();
    tq.expectEmpty();
    return decls;
  }

  private static CharProducer fromAttrValue(Attr a) {
    String value = a.getNodeValue();
    FilePosition pos = Nodes.getFilePositionForValue(a);
    String rawValue = Nodes.getRawValue(a);
    // Use the raw value so that the file positions come out right in
    // error messages.
    if (rawValue != null) {
      // The raw value is HTML so we wrap it in an HTML decoder.
      CharProducer cp = CharProducer.Factory.fromHtmlAttribute(
          CharProducer.Factory.create(
              new StringReader(deQuote(rawValue)), pos));
      // Check if the attribute value has been set since parsing.
      if (String.valueOf(cp.getBuffer(), cp.getOffset(), cp.getLength())
          .equals(value)) {
        return cp;
      }
    }
    // Reached if no raw value stored or if the raw value is out of sync.
    return CharProducer.Factory.create(new StringReader(value), pos);
  }

  /** Strip quotes from an attribute value if there are any. */
  private static String deQuote(String s) {
    int len = s.length();
    if (len < 2) { return s; }
    char ch0 = s.charAt(0);
    return (('"' == ch0 || '\'' == ch0) && ch0 == s.charAt(len - 1))
           ? " " + s.substring(1, len - 1) + " "
           : s;
  }
}
