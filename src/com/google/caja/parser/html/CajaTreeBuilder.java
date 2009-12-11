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

package com.google.caja.parser.html;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.Token;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.impl.TreeBuilder;

/**
 * Bridges between html5lib's TreeBuilder which actually builds the DOM, and
 * HtmlLexer which produces tokens.  This does a bit of accounting to make sure
 * that file positions are preserved on all DOM, text, and attribute nodes.
 *
 * @author mikesamuel@gmail.com
 */
final class CajaTreeBuilder extends TreeBuilder<Node> {
  private static final boolean DEBUG = false;

  // Keep track of the tokens bounding the section we're processing so that
  // we can compute file positions for all added nodes.
  private Token<HtmlTokenType> startTok;
  private Token<HtmlTokenType> endTok;
  // The root html element.  TreeBuilder always creates a valid tree with
  // html, head, and body elements.
  private Element rootElement;
  // Used to compute the spanning file position on the overall document.  Since
  // nodes can move around we can't easily compute this without looking at all
  // descendants.
  private FilePosition fragmentBounds;
  // Track unpopped elements since a </html> tag will not close tables
  // and other scoping elements.
  // @see #closeUnclosedNodes
  private final Set<Node> unpoppedElements = new HashSet<Node>();
  private final Document doc;
  private final boolean needsDebugData;
  private final MessageQueue mq;

  /** @param needsDebugData see {@link DomParser#setNeedsDebugData(boolean)} */
  CajaTreeBuilder(Document doc, boolean needsDebugData, MessageQueue mq) {
    super(
        // Allow loose parsing
        XmlViolationPolicy.ALLOW,
        // Don't coalesce text so that we can apply file positions.
        false);
    this.doc = doc;
    this.needsDebugData = needsDebugData;
    this.mq = mq;
    setIgnoringComments(false);
    setScriptingEnabled(true);  // Affects behavior of noscript
  }

  Element getRootElement() {
    return rootElement;
  }

  FilePosition getFragmentBounds() {
    return fragmentBounds;
  }

  FilePosition getErrorLocation() {
    return (startTok.pos != endTok.pos
            ? FilePosition.span(startTok.pos, endTok.pos)
            : startTok.pos);
  }

  void setTokenContext(Token<HtmlTokenType> start, Token<HtmlTokenType> end) {
    if (DEBUG) {
      System.err.println(
          "*** considering " + start.toString().replace("\n", "\\n"));
    }
    startTok = start;
    endTok = end;
    if (fragmentBounds == null) { fragmentBounds = start.pos; }
  }

  void finish(FilePosition pos) {
    // The position of this token is used for any end tags implied by the end
    // of file.
    Token<HtmlTokenType> eofToken = Token.instance(
        "", HtmlTokenType.IGNORABLE, pos);
    setTokenContext(eofToken, eofToken);
    fragmentBounds = FilePosition.span(fragmentBounds, pos);
    try {
      eof();  // Signal that we can close the html node now.
    } catch (SAXException ex) {
      throw new SomethingWidgyHappenedError(
          "Unexpected parsing error", ex);
    }
  }

  @Override
  protected void appendCommentToDocument(char[] buf, int start, int length) {
    appendComment(doc.getDocumentElement(), buf, start, length);
  }

  @Override
  protected void appendComment(Node el, char[] buf, int start, int length) {
    Comment comment = doc.createComment(new String(buf, start, length));
    el.appendChild(comment);
    if (needsDebugData) {
      Nodes.setFilePositionFor(comment, startTok.pos);
    }
  }

  @Override
  protected void appendCharacters(
      Node n, char[] buf, int start, int length) {
    insertCharactersBefore(buf, start, length, null, n);
  }

  @Override
  protected void insertCharactersBefore(
      char[] buf, int start, int length, Node sibling, Node parent) {
    // Normalize text by adding to an existing text node.
    Node priorSibling = sibling != null
        ? sibling.getPreviousSibling()
        : parent.getLastChild();

    FilePosition pos = null;
    String htmlText = null;
    String plainText;

    String tokText;
    if (bufferMatches(buf, start, length, startTok.text)) {
      tokText = startTok.text;
    } else {
      tokText = String.valueOf(buf, start, length);
    }

    if (startTok.type == HtmlTokenType.TEXT) {
      pos = startTok.pos;
      htmlText = tokText;
      plainText = Nodes.decode(htmlText);
    } else if (startTok.type == HtmlTokenType.UNESCAPED
               || startTok.type == HtmlTokenType.CDATA) {
      pos = startTok.pos;
      plainText = htmlText = tokText;
    } else {
      pos = endTok.pos;
      plainText = tokText;
      if (needsDebugData) {
        htmlText = Nodes.encode(plainText);
      }
    }

    if (priorSibling != null && priorSibling.getNodeType() == Node.TEXT_NODE) {
      Text prevText = (Text) priorSibling;
      // Normalize the DOM by collapsing adjacent text nodes.
      String prevTextContent = prevText.getTextContent();
      StringBuilder sb = new StringBuilder(prevTextContent.length() + length);
      sb.append(prevTextContent).append(buf, start, length);
      Text combined = doc.createTextNode(sb.toString());
      if (needsDebugData) {
        Nodes.setFilePositionFor(
            combined,
            FilePosition.span(
               Nodes.getFilePositionFor(priorSibling),
                 pos));
        Nodes.setRawText(combined, Nodes.getRawText(prevText) + htmlText);
      }
      parent.replaceChild(combined, priorSibling);
      return;
    }

    Text text = doc.createTextNode(plainText);
    if (needsDebugData) {
      Nodes.setFilePositionFor(text, pos);
      Nodes.setRawText(text, htmlText);
    }
    parent.insertBefore(text, sibling);
  }

  @Override
  protected void addAttributesToElement(Node node, Attributes attributes) {
    Element el = (Element) node;
    for (Attr a : getAttributes(attributes)) {
      String name = a.getName();
      if (!el.hasAttributeNS(a.getNamespaceURI(), a.getLocalName())) {
        el.setAttributeNodeNS(a);
      } else {
        mq.addMessage(
            MessageType.DUPLICATE_ATTRIBUTE, Nodes.getFilePositionFor(a),
            MessagePart.Factory.valueOf(name),
            Nodes.getFilePositionFor(
                el.getAttributeNodeNS(a.getNamespaceURI(), a.getLocalName())));
      }
    }
  }

  @Override
  protected void insertBefore(Node child, Node sibling, Node parent) {
    parent.insertBefore(child, sibling);
  }

  @Override
  protected Node parentElementFor(Node child) {
    return child.getParentNode();
  }

  @Override
  protected void appendChildrenToNewParent(Node oldParent, Node newParent) {
    if (DEBUG) {
      System.err.println(
          "Appending children of " + oldParent + " to " + newParent);
    }
    Node child;
    while ((child = oldParent.getFirstChild()) != null) {
      newParent.appendChild(child);
    }
  }

  @Override
  protected void detachFromParentAndAppendToNewParent(
      Node child, Node newParent) {
    newParent.appendChild(child);
  }

  @Override
  protected Node shallowClone(Node node) {
    Node clone = node.cloneNode(false);
    if (needsDebugData) {
      Nodes.setFilePositionFor(clone, Nodes.getFilePositionFor(node));
    }
    switch (node.getNodeType()) {
      case Node.ATTRIBUTE_NODE:
        if (needsDebugData) {
          Nodes.setFilePositionForValue(
              (Attr) clone, Nodes.getFilePositionForValue((Attr) node));
        }
        break;
      case Node.ELEMENT_NODE:
        Element el = (Element) node;
        Element cloneEl = (Element) clone;
        NamedNodeMap attrs = el.getAttributes();
        for (int i = 0, n = attrs.getLength(); i < n; ++i) {
          Attr a = (Attr) attrs.item(i);
          Attr cloneA = cloneEl.getAttributeNodeNS(
              a.getNamespaceURI(), a.getLocalName());
          if (needsDebugData) {
            Nodes.setFilePositionFor(cloneA, Nodes.getFilePositionFor(a));
            Nodes.setFilePositionForValue(
                cloneA, Nodes.getFilePositionForValue(a));
          }
        }
        break;
      case Node.TEXT_NODE: case Node.CDATA_SECTION_NODE:
        if (needsDebugData) {
          Text t = (Text) node;
          Nodes.setRawText(t, Nodes.getRawText(t));
        }
        break;
    }
    return clone;
  }

  @Override
  protected boolean hasChildren(Node node) {
    return node.getFirstChild() != null;
  }

  @Override
  protected void detachFromParent(Node node) {
    node.getParentNode().removeChild(node);
  }

  @Override
  protected Element createHtmlElementSetAsRoot(Attributes attributes) {
    Element documentElement = createElement("html", attributes);
    if (DEBUG) { System.err.println("Created root " + documentElement); }
    this.rootElement = documentElement;
    return documentElement;
  }

  @Override
  protected Element createElement(String name, Attributes attributes) {
    if (DEBUG) { System.err.println("Created element " + name); }
    // Intern since the TreeBuilder likes to compare strings by reference.
    name = name.intern();

    Element el;
    if (name.indexOf(':') < 0) {
      el = doc.createElementNS(Namespaces.HTML_NAMESPACE_URI, name);
    } else {  // Will be fixed up later.  See DomParser#fixup.
      el = doc.createElement(name);
    }
    addAttributesToElement(el, attributes);

    if (needsDebugData) {
      FilePosition pos;
      if (startTok == null) {
        pos = null;
      } else if (startTok.type == HtmlTokenType.TAGBEGIN
                 && tagMatchesElementName(tagName(startTok.text), name)) {
        pos = FilePosition.span(startTok.pos, endTok.pos);
      } else {
        pos = FilePosition.startOf(startTok.pos);
      }
      Nodes.setFilePositionFor(el, pos);
    }
    return el;
  }

  @Override
  protected void elementPopped(String name, Node node) {
    unpoppedElements.remove(node);
    if (DEBUG) { System.err.println("popped " + name + ", node=" + node); }
    if (needsDebugData) {
      name = Html5ElementStack.canonicalElementName(name);
      FilePosition endPos;
      if (startTok.type == HtmlTokenType.TAGBEGIN
          // A start <select> tag inside a select element is treated as a close
          // select tag.  Don't ask -- there's just no good reason.
          // http://www.whatwg.org/specs/web-apps/current-work/#in-select
          //    If the insertion mode is "in select"
          //    A start tag whose tag name is "select"
          //      Parse error. Act as if the token had been an end tag
          //      with the tag name "select" instead.
          && (isEndTag(startTok.text) || "select".equals(name))
          && tagCloses(tagName(startTok.text), name)) {
        endPos = endTok.pos;
      } else {
        // Implied ending.
        endPos = FilePosition.startOf(startTok.pos);
      }
      FilePosition startPos = Nodes.getFilePositionFor(node);
      if (startPos == null) {
        Node first = node.getFirstChild();
        startPos = first == null ? endPos : Nodes.getFilePositionFor(first);
      }
      if (endPos.endCharInFile() >= startPos.endCharInFile()) {
        Nodes.setFilePositionFor(node, FilePosition.span(startPos, endPos));
      }
    }
  }

  /**
   * htmlparser does not generate elementPopped events for the html or body
   * elements, or for void elements.
   */
  @Override
  protected void bodyClosed(Node body) {
    elementPopped("body", body);
  }

  @Override
  protected void htmlClosed(Node html) {
    elementPopped("html", html);
  }

  @Override
  protected void elementPushed(String name, Node node) {
    if (DEBUG) { System.err.println("pushed " + name + ", node=" + node); }
    unpoppedElements.add(node);
  }

  /**
   * Make sure that the end file position is correct for elements still open
   * when EOF is reached.
   */
  void closeUnclosedNodes() {
    if (needsDebugData) {
      for (Node node : unpoppedElements) {
        Nodes.setFilePositionFor(
            node,
            FilePosition.span(Nodes.getFilePositionFor(node), endTok.pos));
      }
    }
    unpoppedElements.clear();
  }

  private static boolean bufferMatches(
      char[] buf, int start, int len, String s) {
    if (len != s.length()) { return false; }
    for (int i = len; --i >= 0;) {
      if (s.charAt(i) != buf[start + i]) { return false; }
    }
    return true;
  }

  // htmlparser passes around an org.xml.sax Attributes list which is a
  // String->String map, but I want to use DomTree.Attrib nodes since they
  // have position info.  htmlparser in some cases does create its own
  // Attributes instances, such as when it is expanding a tag to emulate
  // deprecated elements.
  private List<Attr> getAttributes(Attributes attributes) {
    if (attributes instanceof AttributesImpl) {
      return ((AttributesImpl) attributes).getAttributes();
    }
    // There might be attributes here, but only for emulated tags, such as the
    // mess that is "isindex"
    int n = attributes.getLength();
    if (n == 0) {
      return Collections.<Attr>emptyList();
    } else {
      Attr[] newAttribs = new Attr[n];
      FilePosition pos = FilePosition.startOf(startTok.pos);
      for (int i = 0; i < n; ++i) {
        Attr a = doc.createAttributeNS(
            attributes.getURI(i), attributes.getLocalName(i));
        a.setNodeValue(attributes.getValue(i));
        if (needsDebugData) {
          Nodes.setFilePositionFor(a, pos);
          Nodes.setFilePositionForValue(a, pos);
        }
        newAttribs[i] = a;
      }
      return Arrays.asList(newAttribs);
    }
  }

  // the start token text is either <name or </name for a tag
  static boolean isEndTag(String tokenText) {
    return tokenText.length() >= 2 && tokenText.charAt(1) == '/';
  }

  static String tagName(String tokenText) {
    String name = tokenText.substring(isEndTag(tokenText) ? 2 : 1);
    // Intern since the TreeBuilder likes to compare strings by reference.
    return Html5ElementStack.canonicalElementName(name);
  }

  static boolean tagMatchesElementName(String tagName, String elementName) {
    return tagName.equals(elementName)
        || (tagName.equals("image") && elementName.equals("img"));
  }

  /**
   * true if a close tag with the given name closes an element with the
   * given name.
   */
  static boolean tagCloses(String tagName, String elementName) {
    return tagMatchesElementName(tagName, elementName)
        || (isHeading(tagName) && isHeading(elementName));
  }

  /** true for h1, h2, ... */
  static boolean isHeading(String tagName) {
    if (tagName.length() != 2 || 'h' != tagName.charAt(0)) { return false; }
    char ch1 = tagName.charAt(1);
    return ch1 >= '1' && ch1 <= '6';
  }
}
