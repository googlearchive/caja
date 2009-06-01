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

import com.google.caja.lexer.FilePosition;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Abstract base class for OpenElementStack implementations that maintains the
 * open element stack as the tree is built around it.
 *
 * @author mikesamuel@gmail.com
 */
abstract class AbstractElementStack implements OpenElementStack {
  protected static final boolean DEBUG = false;
  protected final Document doc;
  protected final boolean needsDebugData;
  private final DocumentFragment rootElement;
  /** A list of open nodes. */
  private final List<Node> openNodes = new ArrayList<Node>();

  /**
   * @param needsDebugData see {@link DomParser#setNeedsDebugData(boolean)}
   */
  AbstractElementStack(Document doc, boolean needsDebugData) {
    this.doc = doc;
    this.needsDebugData = needsDebugData;
    this.rootElement = doc.createDocumentFragment();
    this.openNodes.add(rootElement);
  }

  public final Document getDocument() { return doc; }

  /** @inheritDoc */
  public final DocumentFragment getRootElement() {
    return rootElement;
  }

  /** @inheritDoc */
  public void open(boolean fragment) {}

  /** The current element &mdash; according to HTML5 the stack grows down. */
  protected final Node getBottomElement() {
    return openNodes.get(openNodes.size() - 1);
  }

  /** The count of open elements. */
  protected final int getNOpenElements() {
    return openNodes.size();
  }

  /** The index-th open element counting from 0 at the root. */
  protected final Element getElement(int index) {
    assert index > 0 : "" + index;
    return (Element) openNodes.get(index);
  }

  /**
   * Adds an element to the element stack, puts it on the previous head's
   * child list, and updates file positions.
   */
  protected final void push(Element el) {
    if (DEBUG) System.err.println("push(" + el + ")");
    Node parent = getBottomElement();
    openNodes.add(el);
    doAppend(el, parent);
  }

  /**
   * Append a node to the DOM tree as the child of the bottom.
   * This may be overridden by subclasses if they wish to add at a different
   * location.
   */
  protected void doAppend(Node el, Node parent) {
    parent.appendChild(el);
  }

  /**
   * Pop the N bottom levels of the open element stack.
   * @param endPos the position at which the popped elements should be
   * considered to end.
   */
  protected final void popN(int n, FilePosition endPos) {
    if (DEBUG) System.err.println("popN(" + n + ", " + endPos + ")");
    n = Math.min(n, openNodes.size() - 1);
    while (--n >= 0) {
      Node node = openNodes.remove(openNodes.size() - 1);
      if (needsDebugData) {
        Nodes.setFilePositionFor(
            node, FilePosition.span(Nodes.getFilePositionFor(node), endPos));
        if (openNodes.size() == 1) {
          FilePosition rootPos = Nodes.getFilePositionFor(rootElement);
          if (rootPos.endCharInFile() <= 1) {
            rootPos = Nodes.getFilePositionFor(rootElement.getFirstChild());
          }
          if (rootPos.startCharInFile() <= endPos.startCharInFile()) {
            Nodes.setFilePositionFor(
                rootElement, FilePosition.span(rootPos, endPos));
          }
        }
      }
    }
  }

  /** Strip ignorable whitespace nodes from the root. */
  protected void stripIgnorableText() {
    if (rootElement.getFirstChild() == null) { return; }

    // No need to loop because processText normalizes.
    Node firstChild = rootElement.getFirstChild();
    if (isIgnorableTextNode(firstChild)) {
      rootElement.removeChild(firstChild);

      if (rootElement.getFirstChild() == null) { return; }
    }

    // No need to loop because processText normalizes.
    Node lastChild = rootElement.getLastChild();
    if (isIgnorableTextNode(lastChild)) {
      rootElement.removeChild(lastChild);
    }
  }

  /**
   * @see <a href="http://www.w3.org/TR/REC-xml/#sec-white-space">ignorable
   *      white space</a>
   */
  private static boolean isIgnorableTextNode(Node t) {
    // TODO(mikesamuel): check against XML&HTML definitions of whitespace.
    // Note: CDATA and ESCAPED text purposefully not treated as whitespace.
    return t.getNodeType() == Node.TEXT_NODE
        && "".equals(t.getNodeValue().trim());
  }
}
