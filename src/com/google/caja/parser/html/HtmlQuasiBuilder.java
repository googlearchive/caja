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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.PositionInferer;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.lexer.TokenStream;
import com.google.caja.lexer.TokenQueue.Mark;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.reporting.DevNullMessageQueue;
import com.google.caja.util.Maps;
import com.google.caja.util.Sets;
import com.google.caja.util.Strings;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Quasi-literals for HTML.
 *
 * <p>
 * Given a fragment of HTML like {<b title="@title">@text</b>} and bindings like
 * {@code ("title" => "My title", "text", "<Hello World>")} produces a
 * fragment of HTML like {@code <b title="My title">&lt;Hello World&gt;</b>}.
 *
 * <p>
 * Bindings may be any of: <ul>
 * <li>DOM {@link Node node}s from any owner document</li>
 * <li>{@link String}s which can substitute for a text node or attribute value.
 * <li>{@link Boolean}s which can substitute for the value of an attribute.
 *     This is useful with
 *     {@link com.google.caja.lang.html.HTML.Attribute#isValueless valueless}
 *     attributes like {@code checked}.  False means that the attribute will be
 *     omitted from the output.
 * </ul>
 *
 * @author mikesamuel@gmail.com
 */
public class HtmlQuasiBuilder {
  private static Map<String, Node> QUASI_CACHE = Collections.synchronizedMap(
      new LinkedHashMap<String, Node>() {
        @Override
        public boolean removeEldestEntry(Map.Entry<String, Node> eldest) {
          return this.size() > 100;
        }
      });

  private final Document doc;

  private HtmlQuasiBuilder(Document doc) { this.doc = doc; }
  /**
   * Returns a quasi builder that returns nodes whose
   * {@link Node#getOwnerDocument} is the given document.
   */
  public static final HtmlQuasiBuilder getBuilder(Document doc) {
    return new HtmlQuasiBuilder(doc);
  }

  /**
   * Tags that are ignored in HTML5 unless seen inside a certain other tag, and
   * so cannot appear at the top level of a parsed HTML5 document fragment.
   */
  private static final Set<String> PROBLEMATIC_TAGS = Sets.newHashSet(
      "<thead", "<tbody", "<tfoot", "<caption", "<tr", "<td", "<th",
      "<option");
  /**
   * @param quasiHtml a string of HTML containing quasi-literal identifiers.
   * @param bindings adjacent quasi-identifier names and binding values.
   *     If a binding value is a DomTree, then that binding may be substituted
   *     inline in place anywhere PCDATA text may appear.
   *     If it is a String it can bind anywhere where PCDATA,
   *     CDATA, or RDATA, or an attribute value can appear, and is treated
   *     as plain text.
   *     Otherwise, it must be a boolean, and can only bind to an identifier
   *     which completely specifies the value of a boolean element attribute
   *     like {@code selected} or {@code checked}.
   */
  public Node substV(String quasiHtml, Object... bindings) {
    Node quasi = QUASI_CACHE.get(quasiHtml);
    if (quasi == null) {
      try {
        CharProducer cp = CharProducer.Factory.fromString(
            quasiHtml, InputSource.UNKNOWN);
        TokenQueue<HtmlTokenType> tq = new TokenQueue<HtmlTokenType>(
            new HtmlLexer(cp.clone()), InputSource.UNKNOWN);
        boolean isDocument = false;
        Mark m = tq.mark();
        Token<HtmlTokenType> firstTag = null;
        while (!tq.isEmpty()) {
          Token<HtmlTokenType> t = tq.pop();
          if (t.type == HtmlTokenType.TAGBEGIN) {
            if (firstTag == null) { firstTag = t; }
            if (Strings.equalsIgnoreCase("<html", t.text)) {
              isDocument = true;
              break;
            }
          }
        }
        boolean isProblematic = !isDocument && firstTag != null
            && PROBLEMATIC_TAGS.contains(Strings.toLowerCase(firstTag.text));
        if (isProblematic) {
          final TokenStream<HtmlTokenType> lexer = new HtmlLexer(cp);
          final TokenStream<HtmlTokenType> caseFilter
              = new TokenStream<HtmlTokenType>() {
            public boolean hasNext() throws ParseException {
              return lexer.hasNext();
            }
            public Token<HtmlTokenType> next() throws ParseException {
              Token<HtmlTokenType> t = lexer.next();
              switch (t.type) {
                case TAGBEGIN: case TAGEND: case ATTRNAME:
                  if (!t.text.contains(":")) {
                    t = Token.instance(
                        Strings.toLowerCase(t.text), t.type, t.pos);
                  }
                  break;
                default: break;
              }
              return t;
            }
          };
          tq = new TokenQueue<HtmlTokenType>(caseFilter, InputSource.UNKNOWN);
        } else {
          tq.rewind(m);
        }

        DomParser p = new DomParser(
            tq, isProblematic, DevNullMessageQueue.singleton());
        quasi = isDocument
            ? p.parseDocument()
            : p.parseFragment(DomParser.makeDocument(null, null));
      } catch (ParseException ex) {
        throw new RuntimeException("Malformed Quasiliteral : " + quasiHtml, ex);
      }
      QUASI_CACHE.put(quasiHtml, quasi);
    }
    Map<String, Object> bindingMap = Maps.newHashMap();
    for (int i = 0, n = bindings.length; i < n; i += 2) {
      bindingMap.put((String) bindings[i], bindings[i + 1]);
    }
    return subst(quasi, bindingMap);
  }

  /**
   * Parses as an HTML fragment.
   */
  public DocumentFragment toFragment(String html) throws ParseException {
    try {
      return new DomParser(
          DomParser.makeTokenQueue(
              FilePosition.startOfFile(InputSource.UNKNOWN),
              new StringReader(html), false),
          false, DevNullMessageQueue.singleton())
          .parseFragment(doc);
    } catch (IOException ex) {
      throw new RuntimeException("Can't drain StringReader", ex);
    }
  }

  /** The document used to create DOM nodes. */
  public Document getDocument() { return doc; }

  /**
   * Given a file position for a quasi generated sub-tree, make sure all
   * elements have reasonable file positions, so that error messages based on
   * analysis of the quasi generated tree point to roughly the right place.
   *
   * @param pos the position of the structure from which node was translated.
   * @param node a node with some unknown file positions that can be filled in
   *    by inferring positions from quasi bindings.
   *    Positions of this node and children are modified in place.
   */
  public static void usePosition(FilePosition pos, Node node) {
    if (InputSource.UNKNOWN.equals(Nodes.getFilePositionFor(node).source())) {
      Nodes.setFilePositionFor(node, pos);
    }

    PositionInferer inferer = new PositionInferer(pos) {
      @Override
      protected FilePosition getPosForNode(Object o) {
        return Nodes.getFilePositionFor((Node) o);
      }

      @Override
      protected void setPosForNode(Object o, FilePosition pos) {
        Node n = (Node) o;
        FilePosition old = Nodes.getFilePositionFor(n);
        if (InputSource.UNKNOWN.equals(old.source())) {
          Nodes.setFilePositionFor((Node) o, pos);
        }
      }
    };
    addRelations(node, true, inferer);

    inferer.solve();
  }

  private static void addRelations(
      Node n, boolean isRoot, PositionInferer inferer) {
    if (n instanceof Element) {
      Node firstChild = n.getFirstChild();
      for (Attr a : Nodes.attributesOf((Element) n)) {
        inferer.contains(n, a);
        inferer.precedes(a, a.getFirstChild());
        if (firstChild != null) { inferer.precedes(a, firstChild); }
      }
    }
    for (Node child : Nodes.childrenOf(n)) {
      inferer.contains(n, child);
      addRelations(child, false, inferer);
    }
    if (!isRoot) {
      Node next = n.getNextSibling();
      if (next != null) { inferer.adjacent(n, next); }
    }
  }

  private Node subst(Node quasiNode, Map<String, ?> bindings) {
    switch (quasiNode.getNodeType()) {
      case Node.ATTRIBUTE_NODE:
        return substAttrib((Attr) quasiNode, bindings);
      case Node.DOCUMENT_FRAGMENT_NODE:
        DocumentFragment f = doc.createDocumentFragment();
        expandAll(quasiNode, bindings, f);
        return f;
      case Node.ELEMENT_NODE:
        Element el = (Element) quasiNode;
        if ("body".equals(el.getLocalName())
            && Namespaces.HTML_NAMESPACE_URI.equals(el.getNamespaceURI())) {
          return substBody(el, bindings);
        } else {
          return substElement(el, bindings);
        }
      case Node.TEXT_NODE:
        return substText((Text) quasiNode, bindings);
      default:
        return doc.importNode(quasiNode, true);
    }
  }

  private void expandAll(Node quasi, Map<String, ?> bindings, Node parent) {
    for (Node c = quasi.getFirstChild(); c != null; c = c.getNextSibling()) {
      Node substitute = subst(c, bindings);
      flattenOnto(substitute, parent);
    }
  }

  private static final Pattern QUASI_PATTERN = Pattern.compile(
      "@([a-zA-Z][a-zA-Z0-9_]*) ?");
  private Node substText(Text t, Map<String, ?> bindings) {
    String unescaped = Nodes.getRawText(t);
    Matcher m = QUASI_PATTERN.matcher(unescaped);
    if (!m.find()) {
      Node result = doc.importNode(t, true);
      copyFilePositions(t, result);
      return result;
    }
    DocumentFragment parts = doc.createDocumentFragment();
    StringBuilder sb = new StringBuilder();
    int pos = 0;
    do {
      sb.append(unescaped, pos, m.start());
      pos = m.end();

      String quasiIdentifier = m.group(1);
      Object binding = bindings.get(quasiIdentifier);
      if (binding instanceof String) {
        Escaping.escapeXml((String) binding, false, sb);
      } else {
        if (sb.length() != 0) {
          parts.appendChild(doc.createTextNode(Nodes.decode(sb.toString())));
          sb.setLength(0);
        }
        Node bindingNode = (Node) binding;
        Node imported = doc.importNode(bindingNode, true);
        copyFilePositions(bindingNode, imported);
        flattenOnto(imported, parts);
      }
    } while (m.find());
    sb.append(unescaped, pos, unescaped.length());
    if (sb.length() != 0) {
      parts.appendChild(doc.createTextNode(Nodes.decode(sb.toString())));
    }
    if (parts.getFirstChild() != null
        && parts.getFirstChild().getNextSibling() == null) {
      return parts.getFirstChild();
    }
    return parts;
  }

  private Attr substAttrib(Attr a, Map<String, ?> bindings) {
    String oldValue = Nodes.getRawValue(a);
    String quasiIdentifier = singleIdentifier(dequote(oldValue));
    String uri = a.getNamespaceURI();
    String localName = a.getLocalName();
    if (quasiIdentifier != null) {
      // Handle boolean attributes like checked, selected
      Object binding = bindings.get(quasiIdentifier);
      if (binding instanceof Boolean) {
        boolean present = ((Boolean) binding).booleanValue();
        if (!present) { return null; }
        Attr result = doc.createAttributeNS(uri, localName);
        result.setNodeValue(result.getName());
        return result;
      } else if (binding instanceof Attr) {
        Attr bindingAttr = (Attr) binding;
        Attr result = doc.createAttributeNS(uri, localName);
        result.setNodeValue(bindingAttr.getNodeValue());
        copyFilePositions(bindingAttr, result);
        return result;
      }
    }
    Attr result = doc.createAttributeNS(uri, localName);
    result.setNodeValue(substAttrValue(oldValue, bindings));
    return result;
  }

  private static String substAttrValue(
      String rawText, Map<String, ?> bindings) {
    String unescaped = Nodes.decode(dequote(rawText));
    Matcher m = QUASI_PATTERN.matcher(unescaped);
    if (!m.find()) { return unescaped; }
    StringBuilder sb = new StringBuilder();
    int pos = 0;
    do {
      sb.append(unescaped, pos, m.start());
      pos = m.end();

      String quasiIdentifier = m.group(1);
      Object binding = bindings.get(quasiIdentifier);
      if (!(binding instanceof String)) {
        throw new ClassCastException("@" + quasiIdentifier);
      }
      Escaping.escapeXml((String) binding, false, sb);
    } while (m.find());
    sb.append(unescaped, pos, unescaped.length());
    return Nodes.decode(sb.toString());
  }

  private Element substElement(Element e, Map<String, ?> bindings) {
    Element result = doc.createElementNS(e.getNamespaceURI(), e.getLocalName());
    for (Attr attr : Nodes.attributesOf(e)) {
      Attr newAttr = substAttrib(attr, bindings);
      if (newAttr != null) {
        result.setAttributeNodeNS(newAttr);
      }
    }
    expandAll(e, bindings, result);
    return result;
  }

  private Element substBody(Element e, Map<String, ?> bindings) {
    // Handle a corner case around
    //     <html><title>Hi</title>@x</html>
    // where @x is a <frameset>.
    // If it contains attributes or doesn't contain only one text node,
    // then treat it as any other element.
    Node firstChild = e.getFirstChild();
    if (firstChild instanceof Text && firstChild.getNextSibling() == null) {
      String unescaped = Nodes.getRawText((Text) firstChild);
      String quasiIdentifier = singleIdentifier(unescaped);
      if (quasiIdentifier != null) {
        Object binding = bindings.get(quasiIdentifier);
        if (binding instanceof DocumentFragment) {
          DocumentFragment f = (DocumentFragment) binding;
          Node fFirstChild = f.getFirstChild();
          if (fFirstChild != null && fFirstChild.getNextSibling() == null) {
            binding = fFirstChild;
          }
        }
        if (binding instanceof Element) {
          Element bindingEl = (Element) binding;
          if ("frameset".equals(bindingEl.getLocalName())
              && Namespaces.HTML_NAMESPACE_URI.equals(
                  bindingEl.getNamespaceURI())) {
            Element result = (Element) doc.importNode(bindingEl, true);
            copyFilePositions(bindingEl, result);
            return result;
          }
        }
      }
    }
    return substElement(e, bindings);
  }

  private static void flattenOnto(Node toAdd, Node parent) {
    if (toAdd instanceof DocumentFragment) {
      Node c = toAdd.getFirstChild();
      while (c != null) {
        Node next = c.getNextSibling();
        parent.appendChild(c);
        c = next;
      }
    } else {
      parent.appendChild(toAdd);
    }
  }

  private static String singleIdentifier(String unescaped) {
    Matcher m = QUASI_PATTERN.matcher(unescaped.trim());
    if (m.matches()) {
      return m.group(1);
    }
    return null;
  }

  private static String dequote(String rawAttributeValue) {
    int start = 0;
    int end = rawAttributeValue.length();
    if (end > 0) {
      char chn = rawAttributeValue.charAt(end - 1);
      if (chn == '"' || chn == '\'') {
        --end;
        if (start < end && rawAttributeValue.charAt(0) == chn) {
          ++start;
        }
      }
    }
    return rawAttributeValue.substring(start, end);
  }

  private static void copyFilePositions(Node from, Node to) {
    Nodes.setFilePositionFor(to, Nodes.getFilePositionFor(from));
    for (Node fromChild = from.getFirstChild(), toChild = to.getFirstChild();
         fromChild != null;
         fromChild = fromChild.getNextSibling(),
         toChild = toChild.getNextSibling()) {
      copyFilePositions(fromChild, toChild);
    }
  }
}
