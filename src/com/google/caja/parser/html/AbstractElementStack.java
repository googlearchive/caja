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
import com.google.caja.lexer.HtmlTokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for OpenElementStack implementations that maintains the
 * open element stack as the tree is built around it.
 *
 * @author mikesamuel@gmail.com
 */
abstract class AbstractElementStack implements OpenElementStack {
  protected static final boolean DEBUG = false;
  private DomTree.Fragment rootElement = new DomTree.Fragment();
  /**
   * A list of open elements.
   */
  private final List<DomTree> openElements = new ArrayList<DomTree>();

  {
    openElements.add(rootElement);
  }

  AbstractElementStack() {}

  /** @inheritDoc */
  public final DomTree.Fragment getRootElement() {
    return rootElement;
  }

  /** @inheritDoc */
  public void open(boolean fragment) {}

  /** The current element &mdash; according to HTML5 the stack grows down. */
  protected final DomTree getBottomElement() {
    return openElements.get(openElements.size() - 1);
  }

  /** The count of open elements. */
  protected final int getNOpenElements() {
    return openElements.size();
  }

  /** The index-th open element counting from 0 at the root. */
  protected final DomTree.Tag getElement(int index) {
    assert index > 0 : "" + index;
    return (DomTree.Tag) openElements.get(index);
  }

  /**
   * Adds an element to the element stack, puts it on the previous head's
   * child list, and updates file positions.
   */
  protected final void push(DomTree.Tag el) {
    if (DEBUG) System.err.println("push(" + el + ")");
    DomTree parent = getBottomElement();
    openElements.add(el);
    doAppend(el, parent);
  }

  /**
   * Append a node to the DOM tree as the child of the bottom.
   * This may be overridden by subclasses if they wish to add at a different
   * location.
   */
  protected void doAppend(DomTree el, DomTree parent) {
    parent.insertBefore(el, null);
  }

  /**
   * Pop the N bottom levels of the open element stack.
   * @param endPos the position at which the popped elements should be
   * considered to end.
   */
  protected final void popN(int n, FilePosition endPos) {
    if (DEBUG) System.err.println("popN(" + n + ", " + endPos + ")");
    while (--n >= 0) {
      int top = openElements.size() - 1;
      DomTree node = openElements.remove(top);
      node.setFilePosition(FilePosition.span(node.getFilePosition(), endPos));
      if (openElements.size() == 1) {
        FilePosition rootPos = rootElement.getFilePosition();
        if (rootPos.endCharInFile() <= 1) {
          rootPos = rootElement.children().get(0).getFilePosition();
        }
        rootElement.setFilePosition(FilePosition.span(rootPos, endPos));
        break;
      }
    }
  }

  /** Strip ignorable whitespace nodes from the root. */
  protected void stripIgnorableText() {
    if (rootElement.children().isEmpty()) { return; }

    // No need to loop because processText normalizes.
    DomTree firstChild = rootElement.children().get(0);
    if (isIgnorableTextNode(firstChild)) {
      rootElement.removeChild(firstChild);

      if (rootElement.children().isEmpty()) { return; }
    }

    // No need to loop because processText normalizes.
    DomTree lastChild = rootElement.children().get(
        rootElement.children().size() - 1);
    if (isIgnorableTextNode(lastChild)) {
      rootElement.removeChild(lastChild);
    }
  }

  /**
   * @see <a href="http://www.w3.org/TR/REC-xml/#sec-white-space">ignorable
   *      white space</a>
   */
  private static boolean isIgnorableTextNode(DomTree t) {
    // TODO(mikesamuel): check against XML&HTML definitions of whitespace.
    // Note: CDATA and ESCAPED text purposefully not treated as whitespace.
    return t instanceof DomTree.Text && t.getToken().type == HtmlTokenType.TEXT
        && "".equals(t.getToken().text.trim());
  }
}
