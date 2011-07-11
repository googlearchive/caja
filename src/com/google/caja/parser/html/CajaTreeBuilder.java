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
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.Token;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.Sets;

import java.util.List;
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.impl.HtmlAttributes;
import nu.validator.htmlparser.impl.TreeBuilder;

/**
 * Bridges between html5lib's TreeBuilder which actually builds the DOM, and
 * HtmlLexer which produces tokens.  This does a bit of accounting to make sure
 * that file positions are preserved on all DOM, text, and attribute nodes.
 *
 * @author mikesamuel@gmail.com
 */
final class CajaTreeBuilder extends TreeBuilder<Node> {
  static final boolean DEBUG = false;
  private static final String HTML_NAMESPACE = Namespaces.HTML_NAMESPACE_URI;

  // Keep track of the tokens bounding the section we're processing so that
  // we can compute file positions for all added nodes.
  private Token<HtmlTokenType> startTok;
  private Token<HtmlTokenType> endTok;
  private Token<HtmlTokenType> pendingText;
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
  private final Set<Element> unpoppedElements = Sets.newHashSet();
  private final Document doc;
  final boolean needsDebugData;
  private final MessageQueue mq;

  /** @param needsDebugData see {@link DomParser#setNeedsDebugData(boolean)} */
  CajaTreeBuilder(Document doc, boolean needsDebugData, MessageQueue mq) {
    setNamePolicy(XmlViolationPolicy.ALLOW);  // Allow loose parsing
    this.doc = doc;
    this.needsDebugData = needsDebugData;
    this.mq = mq;
    setIgnoringComments(false);
    setScriptingEnabled(true);  // Affects behavior of <noscript>
  }

  Element getRootElement() {
    return rootElement;
  }

  FilePosition getFragmentBounds() {
    return fragmentBounds;
  }

  FilePosition getErrorLocation() {
    if (!needsDebugData) { return FilePosition.UNKNOWN; }
    return (startTok.pos != endTok.pos
            ? FilePosition.span(startTok.pos, endTok.pos)
            : startTok.pos);
  }

  boolean wasOpened(String htmlLocalName) {
    for (Node child = rootElement.getFirstChild();
         child != null; child = child.getNextSibling()) {
      if (child.getNodeType() == Node.ELEMENT_NODE
          && Namespaces.isHtml(child.getNamespaceURI())
          && htmlLocalName.equals(child.getLocalName())) {
        return true;
      }
    }
    return false;
  }

  void setTokenContext(Token<HtmlTokenType> start, Token<HtmlTokenType> end) {
    if (DEBUG) {
      System.err.println(
          "*** considering " + start.toString().replace("\n", "\\n")
          + " @ " + FilePosition.span(start.pos, end.pos));
    }
    startTok = start;
    endTok = end;
    switch (startTok.type) {
      case TEXT: case UNESCAPED: case CDATA:
        pendingText = startTok;
        break;
      default: break;
    }
    if (fragmentBounds == null) { fragmentBounds = start.pos; }
  }

  void finish(FilePosition pos) {
    if (DEBUG) { System.err.println("Finished at " + pos); }
    // The position of this token is used for any end tags implied by the end
    // of file.
    Token<HtmlTokenType> eofToken = Token.instance(
        "", HtmlTokenType.IGNORABLE, pos);
    setTokenContext(eofToken, eofToken);
    if (needsDebugData) {
      fragmentBounds = FilePosition.span(fragmentBounds, pos);
    }
    try {
      eof();  // Signal that we can close the html node now.
    } catch (SAXException ex) {
      throw new SomethingWidgyHappenedError(
          "Unexpected parsing error", ex);
    }
  }

  @Override
  protected void appendCommentToDocument(char[] buf, int start, int length) {
    Node el = doc.getDocumentElement();
    if (null == el) {
      el = doc.createDocumentFragment();
      doc.appendChild(el);
    }
    appendComment(el, buf, start, length);
  }

  @Override
  protected void appendComment(Node el, char[] buf, int start, int length) {
    Comment comment = doc.createComment(new String(buf, start, length));
    comment.setUserData("COMMENT_TYPE", startTok.type.toString(), null);
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

  private void insertCharactersBefore(
      char[] buf, int start, int length, Node sibling, Node parent) {
    if (DEBUG) {
      System.err.println(
          "Inserting characters "
          + String.valueOf(buf, start, length).replace("\n", "\\n")
          + " into " + parent);
    }
    // Normalize text by adding to an existing text node.
    Node priorSibling = sibling != null
        ? sibling.getPreviousSibling()
        : parent.getLastChild();

    String tokText;
    Token<HtmlTokenType> tok = pendingText;
    pendingText = null;
    if (tok != null && bufferMatches(buf, start, length, tok.text)) {
      tokText = tok.text;
    } else {
      tokText = String.valueOf(buf, start, length);
    }

    FilePosition pos = startTok.pos;
    String htmlText = null;
    String plainText = tokText;
    if (tok != null) {
      switch (tok.type) {
        case TEXT:
          pos = tok.pos;
          htmlText = tokText;
          plainText = Nodes.decode(htmlText);
          break;
        case UNESCAPED: case CDATA:
          pos = tok.pos;
          plainText = htmlText = tokText;
          break;
        default: break;
      }
    }
    if (needsDebugData && htmlText == null) {
      htmlText = Nodes.encode(plainText);
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
  protected void addAttributesToElement(Node node, HtmlAttributes attributes) {
    Element el = (Element) node;
    {
      List<Attr> associatedAttrs
          = Html5ElementStack.getAssociatedAttrs(attributes);
      int n = associatedAttrs.size();
      if (n != 0) {
        boolean hasXmlnsDeclaration = (
            associatedAttrs.get(n - 1) == Html5ElementStack.XMLNS_ATTR_MARKER);
        if (hasXmlnsDeclaration) {
          Nodes.markAsHavingXmlnsDeclaration(el);
          --n;
        }
        for (int j = 0; j < n; ++j) {
          Attr a = associatedAttrs.get(j);
          if (!el.hasAttributeNS(a.getNamespaceURI(), a.getLocalName())) {
            el.setAttributeNodeNS(a);
          } else {
            String name = a.getName();
            mq.addMessage(
                MessageType.DUPLICATE_ATTRIBUTE, Nodes.getFilePositionFor(a),
                MessagePart.Factory.valueOf(name),
                Nodes.getFilePositionFor(el.getAttributeNodeNS(
                    a.getNamespaceURI(), a.getLocalName())));
          }
        }
      }
    }
    if (attributes.getLength() != 0) {
      FilePosition pos;
      if (needsDebugData) {
        pos = FilePosition.startOf(Nodes.getFilePositionFor(el));
      } else {
        pos = null;
      }
      String elNs = el.getNamespaceURI();
      for (int i = 0, n = attributes.getLength(); i < n; ++i) {
        String name = attributes.getQName(i);
        String ns = attributes.getURI(i);
        if ("".equals(ns)) { ns = elNs; }
        boolean isNamespaced = !name.startsWith(AttributeNameFixup.PREFIX);
        if (isNamespaced) {
          String localName = attributes.getLocalName(i);
          if (el.hasAttributeNS(ns, localName)) { continue; }
        } else {
          if (el.hasAttribute(name)) { continue; }
        }
        String value = attributes.getValue(i);
        Attr a = doc.createAttributeNS(ns, name);
        a.setValue(value);
        if (pos != null) {
          Nodes.setFilePositionFor(a, pos);
          Nodes.setFilePositionForValue(a, pos);
        }
        el.setAttributeNodeNS(a);
      }
    }
  }

  @Override
  protected void appendElement(Node child, Node parent) {
    if (DEBUG) {
      System.err.println("appendElement " + child + " to " + parent);
    }
    parent.appendChild(child);
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
  protected void insertFosterParentedChild(
      Node child, Node table, Node stackParent) {
    // http://www.w3.org/TR/html5/syntax.html#foster-parent-element
    if (DEBUG) {
      System.err.println(
          "Inserting foster parented child.  child=" + child
          + ", table=" + table + ", stackParent=" + stackParent);
    }
    stackParent.insertBefore(child, table);
  }

  @Override
  protected void insertFosterParentedCharacters(
      char[] buf, int start, int length, Node table, Node stackParent) {
    if (DEBUG) {
      System.err.println(
          "Inserting foster parented characters.  chars="
          + String.valueOf(buf, start, length)
          + ", table=" + table + ", stackParent=" + stackParent);
    }
    insertCharactersBefore(buf, start, length, table, stackParent);
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
  protected Element createHtmlElementSetAsRoot(HtmlAttributes attributes) {
    Element documentElement = createElement(HTML_NAMESPACE, "html", attributes);
    if (DEBUG) { System.err.println("Setting as root " + documentElement); }
    this.rootElement = documentElement;
    return documentElement;
  }

  @Override
  protected Element createElement(
      String ns, String localName, HtmlAttributes attributes) {
    if (DEBUG) { System.err.println("Created element " + localName); }
    // Intern since the TreeBuilder likes to compare strings by reference.
    localName = localName.intern();

    Element el;
    if (localName.indexOf(':') < 0) {
      el = doc.createElementNS(Namespaces.HTML_NAMESPACE_URI, localName);
    } else {  // Will be fixed up later.  See DomParser#fixup.
      el = doc.createElement(localName);
    }
    addAttributesToElement(el, attributes);

    if (needsDebugData) {
      FilePosition pos;
      if (startTok == null) {
        pos = null;
      } else if (startTok.type == HtmlTokenType.TAGBEGIN
                 && tagMatchesElementName(tagName(startTok.text), localName)) {
        pos = FilePosition.span(startTok.pos, endTok.pos);
      } else {
        pos = FilePosition.startOf(startTok.pos);
      }
      Nodes.setFilePositionFor(el, pos);
    }
    return el;
  }

  @Override
  protected void elementPopped(String ns, String name, Node node) {
    boolean removed = unpoppedElements.remove(node);
    if (DEBUG) {
      System.err.println("popped " + ns + " : " + name + ", node=" + node
                         + " @ " + endTok.pos);
    }
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
      if (startPos.source().equals(InputSource.UNKNOWN)) {
        Node first = node.getFirstChild();
        if (first != null) {
          startPos = Nodes.getFilePositionFor(first);
        }
      }
      FilePosition lastPos = startPos;
      Node last;
      for (last = node.getLastChild(); last != null;
           last = last.getPreviousSibling()) {
        if (last.getNodeType() != Node.TEXT_NODE
            || !isWhitespace(last.getNodeValue())) {
          break;
        }
      }
      if (last != null) {
        lastPos = Nodes.getFilePositionFor(last);
      }
      if (DEBUG) {
        System.err.println("startPos=" + startPos + ", lastPos=" + lastPos
             + ", removed=" + removed);
      }
      if (endPos.endCharInFile() >= lastPos.endCharInFile()
          && (removed || lastPos.endCharInFile() > startPos.endCharInFile())) {
        Nodes.setFilePositionFor(node, FilePosition.span(startPos, endPos));
      }
    }
  }
  private static boolean isWhitespace(String s) {
    for (int i = s.length(); --i >= 0;) {
      switch (s.charAt(i)) {
        case '\r': case '\n': case ' ': case '\t': break;
        default: return false;
      }
    }
    return true;
  }

  /**
   * htmlparser does not generate elementPopped events for the head or body
   * elements, or for void elements.
   */
  protected void bodyClosed() {
    if (DEBUG) { System.err.println("In bodyClosed " + unpoppedElements); }
    for (Element unpopped : unpoppedElements) {
      if ("body".equals(unpopped.getTagName())
          && HTML_NAMESPACE.equals(unpopped.getNamespaceURI())) {
        elementPopped(HTML_NAMESPACE, "body", unpopped);
        return;
      }
    }
  }

  protected void headClosed() {
    if (DEBUG) { System.err.println("In headClosed " + unpoppedElements); }
    for (Element unpopped : unpoppedElements) {
      if ("head".equals(unpopped.getTagName())
          && HTML_NAMESPACE.equals(unpopped.getNamespaceURI())) {
        elementPopped(HTML_NAMESPACE, "head", unpopped);
        return;
      }
    }
  }

  protected void htmlClosed(Node html) {
    if (DEBUG) { System.err.println("In htmlClosed " + unpoppedElements); }
    elementPopped(HTML_NAMESPACE, "html", html);
  }

  @Override
  protected void elementPushed(String ns, String name, Node node) {
    if (DEBUG) {
      System.err.println("pushed " + ns + " : " + name + ", node=" + node);
    }
    unpoppedElements.add((Element) node);
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

  /** The start token text is either <name or </name for a tag. */
  static boolean isEndTag(String tokenText) {
    return tokenText.length() >= 2 && tokenText.charAt(1) == '/';
  }

  private static String tagName(String tokenText) {
    String name = tokenText.substring(isEndTag(tokenText) ? 2 : 1);
    return Html5ElementStack.canonicalElementName(name.intern());
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
