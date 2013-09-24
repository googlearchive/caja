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

package com.google.caja.parser.html;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlEntities;
import com.google.caja.lexer.HtmlTextEscapingMode;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.render.Concatenator;
import com.google.caja.reporting.MarkupRenderMode;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.SparseBitSet;
import com.google.caja.util.Strings;
import com.google.common.collect.ImmutableSet;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;

import org.w3c.dom.Attr;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;

/**
 * Utilities for dealing with HTML/XML DOM trees.
 *
 * WARNING: The renderUnsafe methods in this class are unsafe for cajoled
 * code because the Caja pipeline does not sanitize comments.  In particular
 * IE comments rendered by renderUnsafe will be executable.
 *
 * @author mikesamuel@gmail.com
 */
public class Nodes {
  private static final String FP_KEY = "caja:filePosition";
  private static final String RAW_TEXT_KEY = "caja:rawHtml";
  private static final String HAS_XMLNS_DECLARATION_KEY = "caja:hasXmlns";

  /**
   * Used to copy the has-xmlns-declaration bit from an element to elements
   * cloned/imported from it.
   */
  private static final UserDataHandler HAS_XMLNS_DECLARATION_DATA_HANDLER
      = new UserDataHandler() {
        public void handle(
            short operation, String key, Object data, Node src, Node dest) {
          switch (operation) {
            case UserDataHandler.NODE_CLONED:
            case UserDataHandler.NODE_IMPORTED:
              dest.setUserData(HAS_XMLNS_DECLARATION_KEY, Boolean.TRUE, this);
              break;
          }
        }
      };

  /** A left to right {@link Iterable} over the children of the given node. */
  public static Iterable<? extends Node> childrenOf(final Node n) {
    return new Iterable<Node>() {
      public Iterator<Node> iterator() {
        return new Iterator<Node>() {
          Node child = n.getFirstChild();
          public boolean hasNext() {
            return child != null;
          }
          public Node next() {
            if (child == null) { throw new NoSuchElementException(); }
            Node result = child;
            child = child.getNextSibling();
            return result;
          }
          public void remove() { throw new UnsupportedOperationException(); }
        };
      }
    };
  }

  /** An {@link Iterable} over the attributes of the given element. */
  public static Iterable<? extends Attr> attributesOf(final Element el) {
    return new Iterable<Attr>() {
      public Iterator<Attr> iterator() {
        return new Iterator<Attr>() {
          NamedNodeMap attrs = el.getAttributes();
          int i = 0;
          // The DOM spec says that elements with no attributes should return
          // null, though Xerces returns an empty map.
          int n = attrs != null ? attrs.getLength() : 0;
          public boolean hasNext() {
            return i < n;
          }
          public Attr next() {
            if (i == n) { throw new NoSuchElementException(); }
            return (Attr) attrs.item(i++);
          }
          public void remove() { throw new UnsupportedOperationException(); }
        };
      }
    };
  }

  /**
   * An {@link Iterable} over the elements of the given node list.
   * @throws ClassCastException if a member is fetched that is not an instance
   *     of outType/
   */
  public static <T extends Node> Iterable<T> nodeListIterable(
      final NodeList nl, final Class<? extends T> outType) {
    return new Iterable<T>() {
      public Iterator<T> iterator() {
        return new Iterator<T>() {
          int i = 0, n = nl != null ? nl.getLength() : 0;
          public boolean hasNext() {
            return i < n;
          }
          public T next() {
            if (i == n) { throw new NoSuchElementException(); }
            return outType.cast(nl.item(i++));
          }
          public void remove() { throw new UnsupportedOperationException(); }
        };
      }
    };
  }

  private static final FilePosition UNKNOWN_START_OF_FILE
      = FilePosition.startOfFile(InputSource.UNKNOWN);

  /**
   * Returns the last file position associated with the given node by
   * {@link #setFilePositionFor} or related methods.
   */
  public static FilePosition getFilePositionFor(Node node) {
    FilePosition pos = (FilePosition) node.getUserData(FP_KEY);
    if (pos != null) { return pos; }
    return UNKNOWN_START_OF_FILE;
  }

  /** The file position of the value of the given attribute. */
  public static FilePosition getFilePositionForValue(Attr a) {
    return getFilePositionFor(a.getFirstChild());
  }

  /** @see #getFilePositionFor */
  public static void setFilePositionFor(Node node, FilePosition pos) {
    node.setUserData(FP_KEY, pos, null);
  }

  public static void setFilePositionForValue(Attr a, FilePosition pos) {
    setFilePositionFor(a.getFirstChild(), pos);
  }

  /**
   * @see #hasXmlnsDeclaration(Element)
   */
  public static void markAsHavingXmlnsDeclaration(Element el) {
    el.setUserData(
        HAS_XMLNS_DECLARATION_KEY, Boolean.TRUE,
        HAS_XMLNS_DECLARATION_DATA_HANDLER);
  }

  /**
   * True for elements that had an {@code xmlns="<namespace-uri>"} declaration
   * when parsed so that the renderer can include one where it is safe to do so.
   */
  public static boolean hasXmlnsDeclaration(Element el) {
    return Boolean.TRUE.equals(el.getUserData(HAS_XMLNS_DECLARATION_KEY));
  }

  /**
   * Associates the HTML textual value as parsed with the given attribute.
   * If a client may have called {@link Node#setNodeValue(String)} or an alias
   * since parsing, the output should not be trusted.
   */
  public static void setRawValue(Attr a, String html) {
    setRawText((Text) a.getFirstChild(), html);
  }

  /** @see #setRawValue */
  public static String getRawValue(Attr a) {
    return getRawText((Text) a.getFirstChild());
  }

  /**
   * Associates the HTML textual value as parsed with the given node.
   * If a client may have called {@link Node#setNodeValue(String)} or an alias
   * since parsing, the output should not be trusted.
   */
  public static void setRawText(Text cd, String html) {
    cd.setUserData(RAW_TEXT_KEY, html, null);
  }

  /** @see #setRawText */
  public static String getRawText(Text cd) {
    return (String) cd.getUserData(RAW_TEXT_KEY);
  }

  /**
   * Replace entity references in HTML CDATA with their plain text equivalents.
   */
  public static String decode(String html) {
    if (html.indexOf('&') < 0) { return html; }
    char[] chars = html.toCharArray();
    int delta = 0;
    int n = chars.length;
    for (int i = 0; i < n;) {
      char ch = chars[i];
      if (chars[i] == '&') {
        long packedEndAndCodepoint = HtmlEntities.decodeEntityAt(chars, i, n);
        int end = (int) (packedEndAndCodepoint >>> 32);
        if (end != i + 1) {
          int codepoint = ((int) packedEndAndCodepoint) & 0xffffff;
          delta += end - (i + Character.toChars(codepoint, chars, i - delta));
          i = end;
        } else {
          chars[i - delta] = ch;
          ++i;
        }
      } else {
        chars[i - delta] = ch;
        ++i;
      }
    }
    if (delta == 0) { return html; }
    return String.valueOf(chars, 0, n - delta);
  }

  /**
   * Convert HTML to plain text by replacing HTML special characters with HTML
   * entities.
   */
  public static String encode(String raw) {
    StringBuilder sb = new StringBuilder((raw.length() * 3) / 2);
    Escaping.escapeXml(raw, false, sb);
    return sb.toString();
  }

  /**
   * Serializes the given DOM node to HTML or XML.
   * @param rc a context where the token consumer is typically a
   *   {@link Concatenator}, and the {@link RenderContext#asXml} is significant.
   */
  public static void render(Node node, Namespaces ns, RenderContext rc) {
    render(node, ns, rc, false);
  }

  /**
   * @deprecated For use only by non-caja clients of the parser/render
   */
  @Deprecated
  public static void renderUnsafe(DocumentType docType, Node node,
      Namespaces ns, RenderContext rc) {
    render(docType, node, ns, rc, true);
  }

  public static void render(DocumentType docType, Node node, Namespaces ns,
      RenderContext rc) {
    render(docType, node, ns, rc, false);
  }

  /**
   * @deprecated For use only by non-caja clients of the parser/render
   */
  @Deprecated
  public static void renderUnsafe(Node node, Namespaces ns, RenderContext rc) {
    render(node, ns, rc, true);
  }

  private static void render(Node node, Namespaces ns,
      RenderContext rc, boolean renderUnsafe) {
    render(null, node, ns, rc, renderUnsafe);
  }

  private static void render(
      DocumentType docType, Node node, Namespaces ns, RenderContext rc,
      boolean renderUnsafe) {
    StringBuilder sb = new StringBuilder(1 << 18);
    if (null != docType) {
      String rendering = renderDocumentType(docType);
      if (null != rendering) {
        sb.append(rendering);
      }
    }
    new Renderer(rc, sb, rc.markupRenderMode(), ns)
        .render(node, ns, renderUnsafe);
    TokenConsumer out = rc.getOut();
    FilePosition pos = getFilePositionFor(node);
    out.mark(FilePosition.startOf(pos));
    out.consume(sb.toString());
    out.mark(FilePosition.endOf(pos));
  }

  /**
   * Returns a rendering of document type.  This is handled explicitly here
   * rather than in {@link Nodes#render(Node, MarkupRenderMode)} to avoid
   * rendering a document type in the middle of a document.
   *
   * @return null if nothing to render or docType is invalid.
   */
  private static @Nullable String renderDocumentType(DocumentType docType) {
    String publicId = docType.getPublicId();
    String systemId = docType.getSystemId();
    String nodeName;

    if (null != docType.getOwnerDocument() &&
        null != docType.getOwnerDocument().getDocumentElement() &&
        null != docType.getOwnerDocument().getDocumentElement().getNodeName()) {
      nodeName = docType.getOwnerDocument()
        .getDocumentElement()
        .getNodeName();
    } else {
      return null;
    }

    if (!DoctypeMaker.isHtml(nodeName, publicId, systemId)) {
      return null;
    }

    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE ").append(nodeName);
    // The Name in the document type declaration must match the element type
    // of the root element.
    if (null != publicId && publicId.length() > 0) {
      sb.append(" PUBLIC ")
        .append('"')
        .append(publicId.replaceAll("\"", "%22"))
        .append('"');
    }
    if (null != systemId && systemId.length() > 0) {
      // Sanity check - system urls should parse as an absolute uris
      try {
        URI u = new URI(systemId);
        if (u.isAbsolute() &&
            ("http".equals(u.getScheme()) || "https".equals(u.getScheme()))) {
          sb.append(" ")
            .append('"')
            .append(systemId.replaceAll("\"", "%22"))
            .append('"');
        }
      } catch (URISyntaxException e) {
        return null;
      }
    }
    sb.append(">");
    return sb.toString();
  }

  /**
   * Serializes the given DOM node to HTML or XML.
   * @param rc a context where the token consumer is typically a
   *   {@link Concatenator}, and the {@link RenderContext#asXml} is significant.
   */
  public static void render(Node node, RenderContext rc) {
    render(node, rc, false);
  }

  /**
   * @deprecated For use only by non-caja clients of the parser/render
   */
  @Deprecated
  public static void renderUnsafe(Node node, RenderContext rc) {
    render(node, Namespaces.HTML_DEFAULT, rc, true);
  }

  private static void render(Node node, RenderContext rc,
      boolean renderUnsafe) {
    render(node, Namespaces.HTML_DEFAULT, rc, renderUnsafe);
  }

  public static String render(Node node) {
    return render(node, false);
  }

  /**
   * @deprecated For use only by non-caja clients of the parser/render
   */
  @Deprecated
  public static String renderUnsafe(Node node) {
    return renderUnsafe(node, MarkupRenderMode.HTML);
  }

  @Deprecated
  public static String render(Node node, boolean asXml) {
    return render(node, asXml ? MarkupRenderMode.XML : MarkupRenderMode.HTML);
  }

  public static String render(Node node, MarkupRenderMode renderMode) {
    return render(node, renderMode, false);
  }

  /**
   * @deprecated For use only by non-caja clients of the parser/render
   */
  @Deprecated
  public static String renderUnsafe(Node node, MarkupRenderMode renderMode) {
    return render(node, renderMode, true);
  }

  private static String render(Node node, MarkupRenderMode renderMode,
      boolean renderUnsafe) {
    StringBuilder sb = new StringBuilder();
    RenderContext rc = new RenderContext(new Concatenator(sb, null))
        .withMarkupRenderMode(renderMode);
    render(node, rc, renderUnsafe);
    rc.getOut().noMoreTokens();
    return sb.toString();
  }

  public static String render(DocumentType docType, Node node,
      MarkupRenderMode renderMode) {
    return render(docType, node, renderMode, false);
  }

  /**
   * @deprecated For use only by non-caja clients of the parser/render
   */
  @Deprecated
  public static String renderUnsafe(DocumentType docType, Node node,
        MarkupRenderMode renderMode) {
    return render(docType, node, renderMode, true);
  }

  private static String render(DocumentType docType, Node node,
      MarkupRenderMode renderMode, boolean renderUnsafe) {
    StringBuilder sb = new StringBuilder();
    if (null != docType) {
      String rendering = renderDocumentType(docType);
      if (null != rendering) {
        sb.append(rendering);
      }
    }
    sb.append(render(node, renderMode, renderUnsafe));
    return sb.toString();
  }

  private Nodes() { /* uninstantiable */ }
}

final class Renderer {
  final RenderContext rc;
  final StringBuilder out;
  final MarkupRenderMode mode;
  final boolean asXml;
  final int namespaceDepthAtStart;

  Renderer(
      RenderContext rc, StringBuilder out, MarkupRenderMode mode,
      Namespaces ns) {
    this.rc = rc;
    this.out = out;
    this.mode = mode;
    this.asXml = mode == MarkupRenderMode.XML;
    this.namespaceDepthAtStart = depth(ns);
  }

  private static final String HTML_NS = Namespaces.HTML_NAMESPACE_URI;

  /**
   * @deprecated For use only by non-caja clients of the parser/render
   */
  @Deprecated
  void renderUnsafe(Node node, Namespaces ns) {
    render(node, ns, true);
  }

  void render(Node node, Namespaces ns) {
    render(node, ns, false);
  }

  void renderSibs(Node sib, Namespaces ns, boolean renderUnsafe) {
    for (; sib != null; sib = sib.getNextSibling()) {
      render(sib, ns, renderUnsafe);
    }
  }

  void render(Node node, Namespaces ns, boolean renderUnsafe) {
    switch (node.getNodeType()) {
      case Node.DOCUMENT_NODE: case Node.DOCUMENT_FRAGMENT_NODE:
        renderSibs(node.getFirstChild(), ns, renderUnsafe);
        break;
      case Node.ELEMENT_NODE: {
        Element el = (Element) node;
        out.append('<');
        int tagNameStart = out.length();
        boolean addElNs = false;
        Namespaces elNs;
        {
          String nsUri = el.getNamespaceURI();
          if (nsUri == null) { nsUri = HTML_NS; }
          elNs = ns.forUri(nsUri);
          if (elNs == null) {
            elNs = ns = addNamespace(ns, nsUri, el.getPrefix());
            addElNs = true;
          }
        }
        if (elNs.prefix.length() != 0) {
          out.append(elNs.prefix).append(':');
        }
        String localName = el.getLocalName();
        // TODO: do away with the below once Shindig has done away with Neko.
        // This is a workaround for a bug in Element.getLocalName in the version
        // of Neko used by Shindig.
        // See also similar attribute rendering code in this file.
        if (localName == null) {
          localName = el.getTagName();
          if (localName.indexOf(':') >= 0) {
            throw new UncheckedUnrenderableException(localName);
          }
        }
        boolean isHtml = elNs.uri == HTML_NS;
        if (isHtml) { localName = Strings.lower(localName); }
        out.append(localName);
        int tagNameEnd = out.length();

        if (addElNs) {
          out.append(' ');
          renderNamespace(elNs);
        } else if (elNs.prefix == "" && Nodes.hasXmlnsDeclaration(el)) {
          // Since the prefix of the namespace is blank, adding an xmlns cannot
          // possibly change the namespace resolution of contained elements or
          // attributes.
          out.append(" xmlns=\"");
          Escaping.escapeXml(elNs.uri, true, out);
          out.append('"');
        }
        NamedNodeMap attrs = el.getAttributes();
        for (int i = 0, n = attrs.getLength(); i < n; ++i) {
          Attr a = (Attr) attrs.item(i);
          String attrUri = a.getNamespaceURI();
          // Attributes created via setAttribute calls for ISINDEX elements and
          // xmlns attributes have no namespace URI.
          String attrLocalName = a.getLocalName();

          if (Namespaces.XMLNS_NAMESPACE_URI.equals(attrUri)) {
            String nsPrefix = attrLocalName;
            Namespaces added = addNamespaceFromAttribute(
                nsPrefix, a.getValue(), ns);
            if (added == null) { continue; }
            ns = added;
          } else if (attrLocalName == null) {
            attrLocalName = a.getName();
            if (isXmlnsDecl(attrLocalName)) {
              String nsPrefix = "";
              if (attrLocalName.length() > 5) {
                nsPrefix = attrLocalName.substring(6);
              }
              Namespaces added = addNamespaceFromAttribute(
                  nsPrefix, a.getValue(), ns);
              if (added == null) { continue; }
              ns = added;
            } else if (attrLocalName.indexOf(':') >= 0) {
              throw new UncheckedUnrenderableException(null);
            }
          }
          out.append(' ');
          if (attrUri != null && (attrUri = attrUri.intern()) != elNs.uri) {
            Namespaces attrNs = ns.forUri(attrUri);
            if (attrNs == null) {
              attrNs = ns = addNamespace(ns, attrUri, a.getPrefix());
              renderNamespace(attrNs);
              out.append(' ');
            }
            out.append(attrNs.prefix).append(':');
          }
          attrLocalName = emitLocalName(attrLocalName, isHtml);
          // http://www.w3.org/TR/html401/intro/sgmltut.html
          // #didx-boolean_attribute
          // Authors should be aware that many user agents only recognize the
          // minimized form of boolean attributes and not the full form.
          if (!(isHtml && mode == MarkupRenderMode.HTML4_BACKWARDS_COMPAT
                && BooleanAttrs.isBooleanAttr(attrLocalName))) {
            out.append("=\"");
            Escaping.escapeXml(a.getValue(), true, out);
            out.append('"');
          }
        }

        HtmlTextEscapingMode m =
            HtmlTextEscapingMode.getModeForTag(localName);

        Node first = el.getFirstChild();
        if (first == null && m == HtmlTextEscapingMode.VOID) {
          out.append(" />");
        } else {
          out.append(">");
          if (m == HtmlTextEscapingMode.CDATA
              || m == HtmlTextEscapingMode.PLAIN_TEXT) {
            renderCdata(localName, el, asXml);
          } else {
            renderSibs(first, ns, renderUnsafe);
          }
          out.append("</").append(out, tagNameStart, tagNameEnd).append(">");
        }
        break;
      }
      case Node.TEXT_NODE:
        // This is required for all PCDATA content to distinguish it from tags.
        // This is not only appropriate for RCDATA, but is required.
        // http://dev.w3.org/html5/markup/aria/syntax.html#escaping-text-span:
        //   The text in style, script, title, and textarea elements must not
        //   have an escaping text span start that is not followed by an
        //   escaping text span end.
        // The script and style mentioned above have CDATA content, not RCDATA,
        // but title and textarea are the RCDATA to which this is relevant.
        Escaping.escapeXml(node.getNodeValue(), true, out);
        break;
      case Node.CDATA_SECTION_NODE:
        String value = node.getNodeValue();
        Escaping.escapeXml(value, true, out);
        break;
      case Node.ATTRIBUTE_NODE: {
        Attr a = (Attr) node;
        String localName = a.getLocalName();
        if (localName == null) {
          localName = a.getName();
        }
        emitLocalName(localName, HTML_NS.equals(a.getNamespaceURI()));
        out.append("=\"");
        Escaping.escapeXml(a.getValue(), true, out);
        out.append('"');
        break;
      }
      case Node.PROCESSING_INSTRUCTION_NODE: {
        if (!asXml) {
          throw new UncheckedUnrenderableException(
              "XML not renderable as HTML due to processing instruction");
        }
        ProcessingInstruction pi = (ProcessingInstruction) node;
        String target = pi.getTarget();
        String data = pi.getData();
        if (data.contains("?>")) {
          throw new UncheckedUnrenderableException(
              "XML document not renderable due to \"?>\" inside "
              + "processing instruction");
        }
        if (Strings.eqIgnoreCase(target.substring(0, 3), "xml")
            || !isName(target)) {  // isName check avoids targets with "?>".
          throw new UncheckedUnrenderableException(
              "Bad processing instruction target " + target);
        }
        out.append("<?").append(target).append(' ').append(data).append("?>");
        break;
      }
      case Node.COMMENT_NODE: {
        // Comment nodes could either be of the standard HTML comment type,
        // or of the downlevel-hidden type of IE comments or
        // or of the downlevel-revealed type of IE comments.
        // 1) Standard HTML comments and IE downlevel-hidden comments are
        // have HtmlTokenType.COMMENT present in the userData of the node
        // These are only rendered if renderUnsafe is true, and these need
        // <!-- and --> to be attached to them, along with extra sanitization
        // to make sure they match the specs.
        // 2) IE downlevel-revealed comment markers do not need either
        // <!-- or --> and these should be rendered even if renderUnsafe is
        // false, because these are directives processed by non-IE browsers
        // as well.
        String commentType =
                node.getUserData("COMMENT_TYPE") != null ?
                node.getUserData("COMMENT_TYPE").toString() :
                // TODO(anupama): We need to have COMMENT as default because
                // Node.cloneNode does not copy over userData for nodes.
                // Figure out how we can fix this to behave correctly.
                HtmlTokenType.COMMENT.toString();
        String text = node.getNodeValue();
        boolean isStandardComment =
            HtmlTokenType.COMMENT.toString().equals(commentType);
        if (renderUnsafe && isStandardComment) {
          // HTML5 spec 11.1.6
          // Comments must start with the four character sequence (<!--).
          // Following this sequence, the comment may have text, with the
          // additional restriction that the text must not start with a
          // single U+003E GREATER-THAN SIGN character (>), nor start with a
          // U+002D HYPHEN-MINUS character (-) followed by a U+003E
          // GREATER-THAN SIGN (>) character, nor contain two consecutive
          // U+002D HYPHEN-MINUS characters (--), nor end with a U+002D
          // HYPHEN-MINUS character (-). Finally, the comment must be ended
          // by the three character sequence (-->).

          // XML 1.0 spec 2.5
          // Comment ::= '<!--' ((Char - '-') | ('-' (Char - '-')))* '-->'

          String problem = null;
          problem = text.startsWith(">") ? "starts with '>'" : problem;
          if (rc.markupRenderMode() != MarkupRenderMode.HTML) {
            problem = text.startsWith("-") ? "starts with '-'" : problem;
            problem = text.endsWith("-") ? "ends with '-'" : problem;
          } else {
            // If the comment starts or ends with "-", we remove these to make
            // it spec-compliant as far as possible.
            if (text.startsWith("-") || text.endsWith("-")) {
              while (text.startsWith("-")) {
                text = text.substring(1);
              }
              while (text.endsWith("-")) {
                text = text.substring(0, text.length() - 1);
              }
            }
          }
          // Comment nodes are only rendered in unsafe mode
          // TODO: Uncommenting the following check makes the comment rendering
          // html5/xml compliant, however, breaks some webpages which rely on
          // broken behaviour in browsers.
          // problem = text.contains("--") ? "contains '--'" : problem;
          if (null != problem) {
            throw new UncheckedUnrenderableException(
                "XML comment unrenderable because it " + problem);
          }
          out.append("<!--");
          out.append(text);
          out.append("-->");
        } else if (!isStandardComment) {
          // Downlevel-revealed comment.
          out.append(text);
        }
      }
      break;
    }
  }

  // This emits the contents of an HTML element that starts a RAWTEXT
  // parsing context, which means no entities or tags are parsed.
  private void renderCdata(String localName, Element el, boolean asXml) {
    StringBuilder cdata = new StringBuilder();
    for (Node c = el.getFirstChild(); c != null; c = c.getNextSibling()) {
      switch (c.getNodeType()) {
        case Node.TEXT_NODE: case Node.CDATA_SECTION_NODE:
          String text = c.getNodeValue();
          if (asXml) {
            Escaping.escapeXml(text, true, cdata);
          } else {
            cdata.append(text);
          }
          break;
        default:
          cdata.append(Nodes.render(c));
          break;
      }
    }

    // Whether we're emitting xml or html, verify that the result will
    // not break html parsing.
    int problemIndex = checkHtmlCdataCloseable(localName, cdata);
    if (problemIndex != -1) {
      throw new UncheckedUnrenderableException(
          "Document not renderable due to '"
           + cdata.subSequence(
               problemIndex,
               Math.min(cdata.length(), problemIndex + 10))
               + "' in RAWTEXT element");
    }

    out.append(cdata);
  }

  private static final int COMMON_NS_DEPTH = depth(Namespaces.COMMON);
  private static Namespaces addNamespace(
      Namespaces base, String uri, String suggestedPrefix) {
    if (isAlphaNumericId(suggestedPrefix)
        && base.forPrefix(suggestedPrefix) == null) {
      return new Namespaces(base, suggestedPrefix, uri);
    }
    // We subtract COMMON_NS_DEPTH so that when we modify Namespaces.COMMON,
    // we do not change the output for documents that do not depend on the
    // added or removed namespaces.
    // It is alright for depth to be negative since dashes can appear in
    // namespace prefixes.
    return new Namespaces(base, "_ns" + (depth(base) - COMMON_NS_DEPTH), uri);
  }

  private Namespaces addNamespaceFromAttribute(
      String nsPrefix, String nsUri, Namespaces ns) {
    Namespaces masked = ns.forPrefix(nsPrefix);
    if (masked != null) {
      if (!masked.uri.equals(nsUri) && depth(masked) <= namespaceDepthAtStart) {
        // Don't output masking declarations.
        return null;
      }
    }
    return new Namespaces(ns, nsPrefix, nsUri);
  }

  private static int depth(Namespaces ns) {
    int depth = 0;
    for (Namespaces p = ns; p != null; p = p.parent) { ++depth; }
    return depth;
  }

  private void renderNamespace(Namespaces ns) {
    out.append("xmlns:").append(ns.prefix).append("=\"");
    Escaping.escapeXml(ns.uri, true, out);
    out.append('"');
  }

  private static boolean isXmlnsDecl(String attrName) {
    int length = attrName.length();
    if (length == 5) {
      return "xmlns".equals(attrName);
    } else if (length > 6) {
      return attrName.startsWith("xmlns:");
    } else {
      return false;
    }
  }

  private static final boolean[] CASE_SENS_NAME_CHARS = new boolean['z' + 1];
  private static final boolean[] CASE_INSENS_NAME_CHARS = new boolean['z' + 1];
  static {
    for (char ch = '0'; ch <= '9'; ++ch) {
      CASE_SENS_NAME_CHARS[ch] = CASE_INSENS_NAME_CHARS[ch] = true;
    }
    for (char ch = 'a'; ch <= 'z'; ++ch) {
      CASE_SENS_NAME_CHARS[ch] = CASE_INSENS_NAME_CHARS[ch] = true;
    }
    for (char ch = 'A'; ch <= 'Z'; ++ch) {
      CASE_SENS_NAME_CHARS[ch] = true;
    }
  }

  private String emitLocalName(String name, boolean isHtml) {
    // speed up common case where we already have lower-cased letters and
    // digits.
    boolean[] simple = isHtml ? CASE_INSENS_NAME_CHARS : CASE_SENS_NAME_CHARS;
    for (int i = 0, n = name.length(); i < n; ++i) {
      char ch = name.charAt(i);
      if (ch > 'z' || !simple[ch]) {
        if (isHtml) { name = Strings.lower(name); }
        Escaping.escapeXml(name, true, out);
        return name;
      }
    }
    out.append(name);
    return name;
  }

  private static boolean isAlphaNumericId(String s) {
    if (s == null) { return false; }
    int n = s.length();
    if (n == 0) { return false; }
    char ch0 = s.charAt(0);
    if (!(('A' <= ch0 && ch0 <= 'Z') || ('a' <= ch0 && ch0 <= 'z'))) {
      return false;
    }
    for (int i = 1; i < n; ++i) {
      char ch = s.charAt(i);
      if (ch > 'z' || !CASE_SENS_NAME_CHARS[ch]) { return false; }
    }
    return true;
  }

  /**
   * Check that the content of a CDATA element does not contain a close tag
   * for that element or unbalanced escaping text spans.
   *
   * @return -1 if the content is safe, or the start index of a close tag or
   *     escaping text span boundary otherwise.
   */
  private static int checkHtmlCdataCloseable(
      String localName, StringBuilder sb) {
    int escapingTextSpanStart = -1;
    for (int i = 0, n = sb.length(); i < n; ++i) {
      char ch = sb.charAt(i);
      // NUL chars tend to confuse parsers
      if (ch == '\u0000') {
        return i;
      }
      switch (ch) {
        case '<':
          if (i + 3 < n
              && '!' == sb.charAt(i + 1)
              && '-' == sb.charAt(i + 2)
              && '-' == sb.charAt(i + 3)) {
            if (escapingTextSpanStart == -1) {
              escapingTextSpanStart = i;
            } else {
              return escapingTextSpanStart;
            }
          } else if (i + 1 + localName.length() < n
                     && '/' == sb.charAt(i + 1)
                     && Strings.eqIgnoreCase(localName,
                         sb.substring(i + 2, i + 2 + localName.length()))) {
            // A close tag contained in the content.
            if (escapingTextSpanStart < 0) {
              // We could try some recovery strategies here.
              // E.g. prepending "/<!--\n" to sb if "script".equals(localName)
              return i;
            }
            if (!"script".equals(localName)) {
              // Script tags are commonly included inside script tags.
              // <script><!--document.write('<script>f()</script>');--></script>
              // but this does not happen in other CDATA element types.
              // Actually allowing an end tag inside others is problematic.
              // Specifically,
              // <style><!--</style>-->/* foo */</style>
              // displays the text "/* foo */" on some browsers.
              return i;
            }
          }
          break;
        case '>':
          // From the HTML5 spec:
          //    The text in style, script, title, and textarea elements must not
          //    have an escaping text span start that is not followed by an
          //    escaping text span end.
          // We look left since the HTML 5 spec allows the escaping text span
          // end to share dashes with the start.
          if (i >= 2 && '-' == sb.charAt(i - 1) && '-' == sb.charAt(i - 2)) {
            if (escapingTextSpanStart < 0) { return i - 2; }
            escapingTextSpanStart = -1;
          }
          break;
      }
    }
    if (escapingTextSpanStart >= 0) {
      // We could try recovery strategies here.
      // E.g. appending "//-->" to the buffer if "script".equals(localName)
      return escapingTextSpanStart;
    }
    return -1;
  }

  /** As defined in section 2.6 of XML version 5. */
  private static boolean isName(String s) {
    int n = s.length();
    if (n == 0) { return false; }
    if (!NAME_START_CHARS.contains(s.codePointAt(0))) { return false; }

    for (int i = 1; i < n; ++i) {
      if (!NAME_CHARS.contains(s.charAt(i))) { return false; }
    }
    return true;
  }

  /**
   * From http://www.w3.org/TR/2008/REC-xml-20081126/#NT-NameStartChar
   * <pre>
   * NameStartChar     ::=      ":" | [A-Z] | "_" | [a-z] | [#xC0-#xD6]
   *     | [#xD8-#xF6] | [#xF8-#x2FF] | [#x370-#x37D] | [#x37F-#x1FFF]
   *     | [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF] | [#x3001-#xD7FF]
   *     | [#xF900-#xFDCF] | [#xFDF0-#xFFFD] | [#x10000-#xEFFFF]
   * </pre>
   */
  private static final SparseBitSet NAME_START_CHARS = SparseBitSet.withRanges(
      0x3a, 0x3b, 0x41, 0x5b, 0x5f, 0x60, 0x61, 0x7b, 0xc0, 0xd7, 0xd8, 0xf7,
      0x2ff, 0x300, 0x370, 0x37e, 0x37f, 0x2000, 0x200c, 0x200e, 0x2070, 0x2190,
      0x2c00, 0x2ff0, 0x3001, 0xd800, 0xf900, 0xfdd0, 0xfdf0, 0xfffe,
      0x10000, 0xf0000);

  /**
   * From http://www.w3.org/TR/2008/REC-xml-20081126/#NT-NameChar
   * <pre>
   * NameChar      ::=      NameStartChar | "-" | "." | [0-9] | #xB7
   *     | [#x0300-#x036F] | [#x203F-#x2040]
   * </pre>
   */
  private static final SparseBitSet NAME_CHARS = SparseBitSet.withRanges(
      0x2d, 0x2f, 0x30, 0x3b, 0x41, 0x5b, 0x5f, 0x60, 0x61, 0x7b, 0xb7, 0xb8,
      0xc0, 0xd7, 0xd8, 0xf7, 0x2ff, 0x37e, 0x37f, 0x2000, 0x200c, 0x200e,
      0x203f, 0x2041, 0x2070, 0x2190, 0x2c00, 0x2ff0, 0x3001, 0xd800,
      0xf900, 0xfdd0, 0xfdf0, 0xfffe, 0x10000, 0xf0000);
}

final class BooleanAttrs {
  /**
   * The set of HTML4.01 attributes that have the sole value {@code (<name>)}
   * where {@code <name>} is the attribute name and that are #IMPLIED.
   * @see <a href="http://www.w3.org/TR/html401/index/attributes.html">
   *    the HTML4.01 attributes index</a>
   */
  private static final Set<String> BOOLEAN_ATTR_NAMES = ImmutableSet.of(
      "checked", "compact", "declare", "defer", "disabled", "ismap", "multiple",
      "nohref", "noresize", "noshade", "nowrap", "readonly", "selected");

  // http://www.w3.org/TR/html401/index/attributes.html
  static boolean isBooleanAttr(String htmlAttrLocalName) {
    return BOOLEAN_ATTR_NAMES.contains(htmlAttrLocalName);
  }
}
