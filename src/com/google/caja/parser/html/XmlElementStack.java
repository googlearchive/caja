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
import com.google.caja.lexer.Token;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessagePart;
import com.google.caja.util.Name;

import java.util.List;

/**
 * An element stack implementation for XML.
 *
 * @author mikesamuel@gmail.com
 */
class XmlElementStack extends AbstractElementStack {
  XmlElementStack() {}

  public Name canonicalizeElementName(String elementName) {
    // This will need to change if we start accepting namespaced XML.
    return Name.xml(elementName);
  }

  public Name canonicalizeAttributeName(String attributeName) {
    // This will need to change if we start accepting namespaced XML.
    return Name.xml(attributeName);
  }

  /** @inheritDoc */
  public void processTag(Token<HtmlTokenType> start, Token<HtmlTokenType> end,
                         List<DomTree.Attrib> attrs)
      throws IllegalDocumentStateException {
    assert start.type == HtmlTokenType.TAGBEGIN;
    assert end.type == HtmlTokenType.TAGEND;

    boolean open = !start.text.startsWith("</");
    Name tagName = canonicalizeElementName(
        start.text.substring(open ? 1 : 2));

    processTag(tagName, open, start, end, attrs);
  }

  private void processTag(
      Name tagName, boolean open, Token<HtmlTokenType> start,
      Token<HtmlTokenType> end, List<DomTree.Attrib> attrs)
      throws IllegalDocumentStateException {
    if (open) {
      DomTree.Tag newElement = new DomTree.Tag(tagName, attrs, start, end);
      push(newElement);

      // Does the tag end immediately?
      if ("/>".equals(end.text)) { popN(1, end.pos); }
    } else {
      Name bottomElementName = null;
      DomTree bottom = getBottomElement();
      if (bottom instanceof DomTree.Tag) {
        bottomElementName = ((DomTree.Tag) bottom).getTagName();
      }
      if (!tagName.equals(bottomElementName)) {
        throw new IllegalDocumentStateException(new Message(
            DomParserMessageType.UNMATCHED_END,
            start.pos, MessagePart.Factory.valueOf(start.text),
            MessagePart.Factory.valueOf(bottom.getValue())));
      }
      popN(1, end.pos);
    }
  }

  /** @inheritDoc */
  public void processText(Token<HtmlTokenType> text) {
    DomTree parent = getBottomElement();

    DomTree textNode;
    switch (text.type) {
      case CDATA:
        textNode = new DomTree.CData(text);
        break;
      case TEXT:
        {
          List<? extends DomTree> siblings = parent.children();
          if (!siblings.isEmpty()) {
            DomTree lastSibling = siblings.get(siblings.size() - 1);
            if (lastSibling instanceof DomTree.Text
                && lastSibling.getToken().type == HtmlTokenType.TEXT) {
              // Normalize the DOM by collapsing adjacent text nodes.
              Token<HtmlTokenType> previous = lastSibling.getToken();
              Token<HtmlTokenType> combined = Token.instance(
                  previous.text + text.text, previous.type,
                  FilePosition.span(previous.pos, text.pos));
              parent.replaceChild(new DomTree.Text(combined), lastSibling);
              return;
            }
          }
        }
        textNode = new DomTree.Text(text);
        break;
      case UNESCAPED:
        textNode = new DomTree.Text(text);
        break;
      default:
        throw new IllegalArgumentException(text.toString());
    }
    doAppend(textNode, parent);
  }

  /** @inheritDoc */
  public void finish(FilePosition endOfDocument)
      throws IllegalDocumentStateException {
    stripIgnorableText();
    DomTree root = getRootElement();

    FilePosition rootStart = root.getFilePosition();
    if (FilePosition.UNKNOWN.equals(rootStart)) {
      if (root.children().isEmpty()) {
        rootStart = endOfDocument;
      } else {
        rootStart = root.children().get(0).getFilePosition();
      }
    }
    root.setFilePosition(
        FilePosition.span(rootStart, endOfDocument));

    int nOpen = getNOpenElements();
    if (nOpen != 1) {
      DomTree.Tag openEl = getElement(nOpen - 1);
      throw new IllegalDocumentStateException(new Message(
          DomParserMessageType.MISSING_END, endOfDocument,
          openEl.getTagName(), openEl.getFilePosition()));
    }
  }
}
