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
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.Token;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.Name;

import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * An element stack implementation for XML.
 *
 * @author mikesamuel@gmail.com
 */
class XmlElementStack extends AbstractElementStack {
  private final MessageQueue mq;

  XmlElementStack(Document doc, boolean needsDebugData, MessageQueue mq) {
    super(doc, needsDebugData);
    this.mq = mq;
  }

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
                         List<Attr> attrs)
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
      Token<HtmlTokenType> end, List<Attr> attrs)
      throws IllegalDocumentStateException {
    if (open) {
      Element newElement = doc.createElement(tagName.getCanonicalForm());
      for (Attr a : attrs) {
        String name = a.getName();
        if (!newElement.hasAttribute(name)) {
          newElement.setAttributeNode(a);
        } else {
          mq.addMessage(
              MessageType.DUPLICATE_ATTRIBUTE, Nodes.getFilePositionFor(a),
              MessagePart.Factory.valueOf(name),
              Nodes.getFilePositionFor(newElement.getAttributeNode(name)));
        }
      }
      if (needsDebugData) {
        Nodes.setFilePositionFor(
            newElement, FilePosition.span(start.pos, end.pos));
      }
      push(newElement);

      // Does the tag end immediately?
      if ("/>".equals(end.text)) { popN(1, end.pos); }
    } else {
      String bottomElementName = null;
      Node bottom = getBottomElement();
      if (bottom instanceof Element) {
        bottomElementName = ((Element) bottom).getTagName();
      }
      if (!tagName.getCanonicalForm().equals(bottomElementName)) {
        throw new IllegalDocumentStateException(new Message(
            DomParserMessageType.UNMATCHED_END,
            start.pos, MessagePart.Factory.valueOf(start.text),
            MessagePart.Factory.valueOf("<" + bottomElementName)));
      }
      popN(1, end.pos);
    }
  }

  /** @inheritDoc */
  public void processText(Token<HtmlTokenType> text) {
    Node parent = getBottomElement();

    Text textNode;
    switch (text.type) {
      case CDATA:
        textNode = doc.createCDATASection(
            text.text.substring(9, text.text.length() - 3));
        break;
      case TEXT:
        {
          Node lastSibling = parent.getLastChild();
          if (lastSibling != null) {
            if (lastSibling.getNodeType() == Node.TEXT_NODE) {
              Text combined = doc.createTextNode(
                  lastSibling.getNodeValue() + Nodes.decode(text.text));
              if (needsDebugData) {
                Nodes.setRawText(
                    combined, Nodes.getRawText((Text) lastSibling) + text.text);
                Nodes.setFilePositionFor(
                    combined,
                    FilePosition.span(
                        Nodes.getFilePositionFor(lastSibling), text.pos));
              }
              parent.replaceChild(combined, lastSibling);
              return;
            }
          }
        }
        textNode = doc.createTextNode(Nodes.decode(text.text));
        break;
      case UNESCAPED:
        textNode = doc.createTextNode(text.text);
        break;
      default:
        throw new IllegalArgumentException(text.toString());
    }
    if (needsDebugData) {
      Nodes.setRawText(textNode, text.text);
      Nodes.setFilePositionFor(textNode, text.pos);
    }
    doAppend(textNode, parent);
  }

  /** @inheritDoc */
  public void finish(FilePosition endOfDocument)
      throws IllegalDocumentStateException {
    stripIgnorableText();
    DocumentFragment root = getRootElement();

    if (needsDebugData) {
      FilePosition rootStart = Nodes.getFilePositionFor(root);
      if (rootStart == null || InputSource.UNKNOWN.equals(rootStart.source())) {
        if (root.getFirstChild() == null) {
          rootStart = endOfDocument;
        } else {
          rootStart = Nodes.getFilePositionFor(root.getFirstChild());
        }
      }
      if (rootStart.startCharInFile() <= endOfDocument.startCharInFile()) {
        Nodes.setFilePositionFor(
            root, FilePosition.span(rootStart, endOfDocument));
      }
    }

    int nOpen = getNOpenElements();
    if (nOpen != 1) {
      Element openEl = getElement(nOpen - 1);
      throw new IllegalDocumentStateException(new Message(
          DomParserMessageType.MISSING_END, endOfDocument,
          MessagePart.Factory.valueOf(openEl.getTagName()),
          Nodes.getFilePositionFor(openEl)));
    }
  }
}
