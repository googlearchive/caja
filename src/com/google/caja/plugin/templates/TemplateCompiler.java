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
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.html.AttribKey;
import com.google.caja.parser.html.ElKey;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.plugin.JobEnvelope;
import com.google.caja.plugin.Placeholder;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.stages.EmbeddedContent;
import com.google.caja.plugin.stages.HtmlEmbeddedContentFinder;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;
import com.google.caja.util.Pair;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Compiles an HTML document to a chunk of safe static HTML, and a bit of
 * javascript which attaches event handlers and other dynamic attributes, and
 * executes inline scripts.
 *
 * <p>
 * Requires that CSS be rewritten, that inline scripts have been replaced with
 * {@link Placeholder placeholders}, and that the output JS be run through
 * the CajitaRewriter.
 *
 * @author mikesamuel@gmail.com
 */
public class TemplateCompiler {
  private final List<IhtmlRoot> ihtmlRoots;
  private final List<ValidatedStylesheet> validatedStylesheets;
  private final HtmlSchema htmlSchema;
  private final PluginMeta meta;
  private final MessageContext mc;
  private final MessageQueue mq;
  private final HtmlAttributeRewriter aRewriter;

  /**
   * Maps {@link Node}s to JS parse trees.
   *
   * <ul>
   *
   * <li>If the value is {@code null}, then the literal value in the
   * original parse tree may be used.</li>
   *
   * <li>If the node is an attribute, then the value is an expression
   * that returns a (key, value) pair.</li>
   *
   * <li>If the node is a text node inside a script block, then the value is an
   * {@link UncajoledModule}.</li>
   *
   * <li>Otherwise, the value is a JavaScript expression which evaluates to the
   * dynamic text value.</li>
   *
   * </ul>
   */
  private final Map<Node, ParseTreeNode> scriptsPerNode
      = Maps.newIdentityHashMap();

  /**
   * Maps placeholder IDs to JS programs.
   *
   * We extract scripts early on and turn them into separate jobs, so that we
   * can use cached results for scripts even when the non-script details of the
   * containing HTML page changes.
   */
  private final Map<String, ScriptPlaceholder> scriptsPerPlaceholder
      = Maps.newHashMap();

  private final Map<Attr, EmbeddedContent> embeddedContent
      = Maps.newIdentityHashMap();

  /**
   * @param ihtmlRoots roots of trees to process and the baseURI used to resolve
   *     URIs in those nodes.
   * @param validatedStylesheets CSS style-sheets that have had unsafe
   *     constructs removed and had rules rewritten.
   * @param placeholderScripts placeholder IDs per unsanitized JS programs.
   *     We extract scripts early on and turn them into separate jobs, so that
   *     we can use cached results for scripts even when the non-script details
   *     of the containing HTML page changes.
   * @param meta specifies how URLs and other attributes are rewritten.
   * @param cssSchema specifies how STYLE attributes are rewritten.
   * @param htmlSchema specifies how elements and attributes are handled.
   * @param mq receives messages about invalid attribute values.
   */
  public TemplateCompiler(
      List<? extends IhtmlRoot> ihtmlRoots,
      List<? extends ValidatedStylesheet> validatedStylesheets,
      List<? extends ScriptPlaceholder> placeholderScripts,
      CssSchema cssSchema, HtmlSchema htmlSchema,
      PluginMeta meta, MessageContext mc, MessageQueue mq) {
    this.ihtmlRoots = Lists.newArrayList(ihtmlRoots);
    this.validatedStylesheets = Lists.newArrayList(validatedStylesheets);
    for (ScriptPlaceholder ph : placeholderScripts) {
      scriptsPerPlaceholder.put(ph.source.placeholderId, ph);
    }
    this.htmlSchema = htmlSchema;
    this.meta = meta;
    this.mc = mc;
    this.mq = mq;
    this.aRewriter = new HtmlAttributeRewriter(
        meta, cssSchema, htmlSchema, embeddedContent, mq);
  }

  /**
   * Examines the HTML document and writes messages about problematic portions
   * to the message queue passed to the constructor.
   */
  private void inspect() {
    if (!mq.hasMessageAtLevel(MessageLevel.FATAL_ERROR)) {
      for (IhtmlRoot ihtmlRoot : ihtmlRoots) {
        HtmlEmbeddedContentFinder finder = new HtmlEmbeddedContentFinder(
            htmlSchema, ihtmlRoot.baseUri, mq, mc);
        for (EmbeddedContent c : finder.findEmbeddedContent(ihtmlRoot.root)) {
          Node src = c.getSource();
          if (src instanceof Attr) { embeddedContent.put((Attr) src, c); }
        }
        inspect(ihtmlRoot.source, ihtmlRoot.root, ElKey.forHtmlElement("div"));
      }
    }
  }

  private void inspect(
      JobEnvelope source, Node n, ElKey containingHtmlElement) {
    switch (n.getNodeType()) {
      case Node.ELEMENT_NODE:
        inspectElement(source, (Element) n, containingHtmlElement);
        break;
      case Node.TEXT_NODE: case Node.CDATA_SECTION_NODE:
        inspectText((Text) n, containingHtmlElement);
        break;
      case Node.DOCUMENT_FRAGMENT_NODE:
        inspectFragment(source, (DocumentFragment) n, containingHtmlElement);
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
  private void inspectElement(
      JobEnvelope source, Element el, ElKey containingHtmlElement) {
    ElKey elKey = ElKey.forElement(el);

    // Recurse early so that ihtml:dynamic elements have been parsed before we
    // process the attributes element list.
    for (Node child : Nodes.childrenOf(el)) {
      inspect(source, child, elKey);
    }

    // For each attribute allowed on this element type, ensure that
    // (1) If it is not specified, and its default value is not allowed, then
    //     it is added with a known safe value.
    // (2) Its value is rewritten as appropriate.
    // We don't have to worry about disallowed attributes since those will
    // not be present in scriptsPerNode.  The TemplateSanitizer should have
    // stripped those out.  The TemplateSanitizer should also have stripped out
    // disallowed elements.
    if (!htmlSchema.isElementAllowed(elKey)) { return; }

    HTML.Element elInfo = htmlSchema.lookupElement(elKey);
    List<HTML.Attribute> attrs = elInfo.getAttributes();
    if (attrs != null) {
      for (HTML.Attribute a : attrs) {
        AttribKey attrKey = a.getKey();
        if (!htmlSchema.isAttributeAllowed(attrKey)) { continue; }
        Attr attr = null;
        String aUri = attrKey.ns.uri;
        String aName = attrKey.localName;
        Attr unsafe = el.getAttributeNodeNS(aUri, aName);
        if (a.getType() == HTML.Attribute.Type.FRAME_TARGET) {
          if (unsafe == null) {
            String safeValue =
                (a.getDefaultValue() != null
                 && a.getValueCriterion().accept(a.getDefaultValue()))
                ? a.getDefaultValue() : a.getSafeValue();
            attr = el.getOwnerDocument().createAttributeNS(
                attrKey.ns.uri, attrKey.localName);
            attr.setNodeValue(safeValue);
            el.setAttributeNode(attr);
          } else {
            // Leave it for later stages to deal with
            attr = unsafe;
          }
        } else if (unsafe != null
                   && a.getValueCriterion().accept(unsafe.getValue())) {
          attr = unsafe;
        } else if ((a.getDefaultValue() != null
                    && !a.getValueCriterion().accept(a.getDefaultValue()))
                   || !a.isOptional()) {
          attr = el.getOwnerDocument().createAttributeNS(aUri, aName);
          String safeValue;
          if (a.getType() == HTML.Attribute.Type.URI) {
            safeValue = "" + Nodes.getFilePositionFor(el).source().getUri();
          } else {
            if (unsafe == null &&
                a.getDefaultValue() != null &&
                a.getValueCriterion().accept(a.getDefaultValue())) {
              safeValue = a.getDefaultValue();
            } else {
              safeValue = a.getSafeValue();
            }
          }
          if (safeValue == null) {
            mq.addMessage(IhtmlMessageType.MISSING_ATTRIB,
                          Nodes.getFilePositionFor(el), elKey, attrKey);
            continue;
          }
          attr.setNodeValue(safeValue);
          el.setAttributeNodeNS(attr);
        }
        if (attr != null) {
          inspectHtmlAttribute(source, attr, a);
        }
      }
    }
    // Iterate over all attributes on el and allow any starting with data-caja-
    NamedNodeMap attrMap = el.getAttributes();
    int attrLen = attrMap.getLength();
    for (int i = 0; i < attrLen; ++i) {
      Attr attr = (Attr) attrMap.item(i);
      if (attr.getLocalName().startsWith("data-caja-")) {
        scriptsPerNode.put(attr, new StringLiteral(
            Nodes.getFilePositionForValue(attr),
            attr.getValue()));
      }
    }
    scriptsPerNode.put(el, null);
  }

  private void inspectText(Text t, ElKey containingHtmlElement) {
    if (!htmlSchema.isElementAllowed(containingHtmlElement)) { return; }
    scriptsPerNode.put(t, null);
  }

  private void inspectFragment(
      JobEnvelope source, DocumentFragment f, ElKey containingHtmlElement) {
    scriptsPerNode.put(f, null);
    for (Node child : Nodes.childrenOf(f)) {
      // We know that top level text nodes in a document fragment
      // are not significant if they are just newlines and indentation.
      // This decreases output size significantly.
      if (isWhitespaceOnlyTextNode(child)) { continue; }
      inspect(source, child, containingHtmlElement);
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
  private void inspectHtmlAttribute(
      JobEnvelope source, Attr attr, HTML.Attribute info) {
    if (Placeholder.ID_ATTR.is(attr)
        && scriptsPerPlaceholder.containsKey(attr.getValue())) {
      scriptsPerNode.put(attr, null);
    } else {
      HtmlAttributeRewriter.SanitizedAttr r = aRewriter.sanitizeStringValue(
          HtmlAttributeRewriter.fromAttr(attr, info, source));
      if (r.isSafe) {
        scriptsPerNode.put(attr, r.result);
      }
      // Otherwise the SanitizeHtmlStage should have emitted a warning.
    }
  }

  /**
   * Builds a tree of only the safe HTML parts ignoring IHTML elements.
   * If there are embedded script elements, then these will be removed, and
   * nodes may have synthetic IDs added so that the generated code can split
   * them into the elements present when each script is executed.
   * <p>
   * On introspection, the code will find that the output DOM is missing the
   * SCRIPT elements originally on the page. We consider this a known observable
   * fact of our transformation. If we wish to hid that as well, we could
   * change {@link SafeHtmlMaker} to include empty SCRIPT nodes. However, that
   * would make the output larger -- and, anyway, the text content of these
   * nodes would *still* not be identical to the original.
   *
   * @param doc a DOM {@link Document} object to be used as a factory for DOM
   *     nodes; it is not processed or transformed in any way.
   */
  public Pair<List<SafeHtmlChunk>, List<SafeJsChunk>> getSafeHtml(
      Document doc) {
    // Inspect the document.
    inspect();

    // Compile CSS to HTML when appropriate or to JS where not.
    // It always ends up at the top either way.
    List<SafeStylesheet> css = new SafeCssMaker(
        validatedStylesheets, doc).make();

    // Emit safe HTML with JS which attaches dynamic attributes.
    SafeHtmlMaker htmlMaker = new SafeHtmlMaker(
        meta, htmlSchema, mc, doc, scriptsPerNode, scriptsPerPlaceholder,
        ihtmlRoots, aRewriter.getHandlers());
    return htmlMaker.make(css);
  }
}
