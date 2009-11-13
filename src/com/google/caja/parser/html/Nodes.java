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
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.render.Concatenator;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Strings;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Utilities for dealing with HTML/XML DOM trees.
 *
 * @author mikesamuel@gmail.com
 */
public class Nodes {
  private static final String FP_KEY = "caja:filePosition";
  private static final String RAW_TEXT_KEY = "caja:rawHtml";

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
          int i = 0, n = nl != null ? nl.getLength() : null;
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
    StringBuilder sb = new StringBuilder(1 << 18);
    new Renderer(sb, rc.asXml(), rc.isAsciiOnly()).render(node, ns);
    TokenConsumer out = rc.getOut();
    FilePosition pos = getFilePositionFor(node);
    out.mark(FilePosition.startOf(pos));
    out.consume(sb.toString());
    out.mark(FilePosition.endOf(pos));
  }

  /**
   * Serializes the given DOM node to HTML or XML.
   * @param rc a context where the token consumer is typically a
   *   {@link Concatenator}, and the {@link RenderContext#asXml} is significant.
   */
  public static void render(Node node, RenderContext rc) {
    render(node, Namespaces.HTML_DEFAULT, rc);
  }

  public static String render(Node node) {
    return render(node, false);
  }

  public static String render(Node node, boolean asXml) {
    StringBuilder sb = new StringBuilder();
    RenderContext rc = new RenderContext(new Concatenator(sb, null))
        .withAsXml(asXml);
    render(node, rc);
    rc.getOut().noMoreTokens();
    return sb.toString();
  }

  private Nodes() {}
}

final class Renderer {
  final StringBuilder out;
  final boolean asXml;
  final boolean isAsciiOnly;

  Renderer(StringBuilder out, boolean asXml, boolean isAsciiOnly) {
    this.out = out;
    this.asXml = asXml;
    this.isAsciiOnly = isAsciiOnly;
  }

  private static final String HTML_NS = Namespaces.HTML_NAMESPACE_URI;

  void render(Node node, Namespaces ns) {
    switch (node.getNodeType()) {
      case Node.DOCUMENT_NODE: case Node.DOCUMENT_FRAGMENT_NODE:
        for (Node c = node.getFirstChild();
             c != null; c = c.getNextSibling()) {
          render(c, ns);
        }
        break;
      case Node.ELEMENT_NODE: {
        Element el = (Element) node;
        out.append('<');
        int tagNameStart = out.length();
        boolean addElNs;
        Namespaces elNs;
        {
          String nsUri = el.getNamespaceURI();
          if (nsUri == null) { nsUri = HTML_NS; }
          elNs = ns.forUri(nsUri);
          addElNs = elNs == null;
          if (addElNs) {
            elNs = ns = addNamespace(ns, nsUri);
          }
        }
        if (elNs.prefix.length() != 0) {
          out.append(elNs.prefix).append(':');
        }
        String localName = el.getLocalName();
        boolean isHtml = elNs.uri == HTML_NS;
        if (isHtml) { localName = Strings.toLowerCase(localName); }
        out.append(localName);
        int tagNameEnd = out.length();

        if (addElNs) {
          out.append(' ');
          renderNamespace(elNs);
        }
        NamedNodeMap attrs = el.getAttributes();
        for (int i = 0, n = attrs.getLength(); i < n; ++i) {
          out.append(' ');
          Attr a = (Attr) attrs.item(i);
          String attrUri = a.getNamespaceURI();
          // Attributes created via setAttribute calls for ISINDEX elements
          // have no namespace URI.
          if (attrUri != null && (attrUri = attrUri.intern()) != elNs.uri) {
            Namespaces attrNs = ns.forUri(attrUri);
            if (attrNs == null) {
              attrNs = ns = addNamespace(ns, elNs.uri);
              renderNamespace(attrNs);
              out.append(' ');
            }
            out.append(attrNs.prefix).append(':');
          }
          renderAttr(a, HTML_NS.equals(attrUri));
        }

        HtmlTextEscapingMode m = asXml || !isHtml
            ? HtmlTextEscapingMode.PCDATA
            : HtmlTextEscapingMode.getModeForTag(localName);
        Node first = el.getFirstChild();
        if (first == null && (asXml || m == HtmlTextEscapingMode.VOID)) {
          // This is safe regardless of whether the output is XML or HTML since
          // we only skip the end tag for HTML elements that don't require one,
          // and the slash will cause XML to treat it as a void tag.
          out.append(" />");
        } else {
          out.append('>');
          if (!asXml) {
            if (m == HtmlTextEscapingMode.CDATA
                || m == HtmlTextEscapingMode.PLAIN_TEXT) {
              StringBuilder cdataContent = new StringBuilder();
              for (Node c = first; c != null; c = c.getNextSibling()) {
                switch (c.getNodeType()) {
                  case Node.TEXT_NODE: case Node.CDATA_SECTION_NODE:
                    cdataContent.append(c.getNodeValue());
                    break;
                }
              }
              // Make sure that the CDATA section does not contain a close
              // tag.
              if (containsEndTag(cdataContent)) {
                String lcaseContent = Strings.toLowerCase(
                    cdataContent.toString());
                for (int p = 1;
                     (p = lcaseContent.indexOf(localName, p + 1)) >= 0;) {
                  if (lcaseContent.regionMatches(p - 2, "</", 0, 2)) {
                    throw new IllegalStateException(
                        "XML document not renderable as HTML due to </"
                        + localName + " in CDATA tag");
                  }
                }
              }
              out.append(cdataContent);
            } else {
              for (Node c = first; c != null; c = c.getNextSibling()) {
                render(c, ns);
              }
            }
          } else {
            for (Node c = first; c != null; c = c.getNextSibling()) {
              render(c, ns);
            }
          }
          // This is not correct for HTML <plaintext> nodes, but live with it,
          // since handling plaintext correctly would require omitting end tags
          // for parent nodes, and so significantly complicate rendering for a
          // node we shouldn't ever render anyway.
          out.append("</")
              .append(out, tagNameStart, tagNameEnd)
              .append('>');
        }
        break;
      }
      case Node.TEXT_NODE:
        Escaping.escapeXml(node.getNodeValue(), isAsciiOnly, out);
        break;
      case Node.CDATA_SECTION_NODE:
        String value = node.getNodeValue();
        if (asXml && !value.contains("]]>")) {
          out.append("<![CDATA[");
          out.append(value);
          out.append("]]>");
        } else {
          Escaping.escapeXml(value, isAsciiOnly, out);
        }
        break;
      case Node.ATTRIBUTE_NODE: {
        Attr a = (Attr) node;
        renderAttr(a, HTML_NS.equals(a.getNamespaceURI()));
        break;
      }
    }
  }

  private Namespaces addNamespace(Namespaces base, String uri) {
    int depth = 0;
    for (Namespaces p = base; p != null; p = p.parent) { ++depth; }
    return new Namespaces(base, "_ns" + depth, uri);
  }

  private void renderNamespace(Namespaces ns) {
    out.append("xmlns:").append(ns.prefix).append("=\"");
    Escaping.escapeXml(ns.uri, isAsciiOnly, out);
    out.append('"');
  }

  private void renderAttr(Attr a, boolean isHtml) {
    emitLocalName(a.getLocalName(), isHtml);
    out.append("=\"");
    Escaping.escapeXml(a.getValue(), isAsciiOnly, out);
    out.append("\"");
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

  private void emitLocalName(String name, boolean isHtml) {
    // speed up common case where we already have lower-cased letters and
    // digits.
    boolean[] simple = isHtml ? CASE_INSENS_NAME_CHARS : CASE_SENS_NAME_CHARS;
    for (int i = 0, n = name.length(); i < n; ++i) {
      char ch = name.charAt(i);
      if (ch > 'z' || !simple[ch]) {
        if (isHtml) { name = Strings.toLowerCase(name); }
        Escaping.escapeXml(name, isAsciiOnly, out);
        return;
      }
    }
    out.append(name);
  }

  private static boolean containsEndTag(StringBuilder sb) {
    for (int i = 0, n = sb.length(); i < n; i += 2) {
      switch (sb.charAt(i)) {
        case '<':
          if (i + 1 < n && sb.charAt(i + 1) == '/') { return true; }
          break;
        case '/':
          if (i > 0 && sb.charAt(i - 1) == '<') { return true; }
          break;
      }
    }
    return false;
  }
}
