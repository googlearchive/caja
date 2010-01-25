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

import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
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

  XmlElementStack(
      Document doc, Namespaces ns, boolean needsDebugData, MessageQueue mq) {
    super(doc, ns, needsDebugData);
    this.mq = mq;
  }

  public boolean needsNamespaceFixup() { return false; }

  /** {@inheritDoc} */
  public void processTag(Token<HtmlTokenType> start, Token<HtmlTokenType> end,
                         List<AttrStub> attrs)
      throws IllegalDocumentStateException {
    assert start.type == HtmlTokenType.TAGBEGIN;
    assert end.type == HtmlTokenType.TAGEND;

    boolean open = !start.text.startsWith("</");
    processTag(start.text.substring(open ? 1 : 2), open, start, end, attrs);
  }

  private void processTag(
      String elQName, boolean open, Token<HtmlTokenType> start,
      Token<HtmlTokenType> end, List<AttrStub> attrs)
      throws IllegalDocumentStateException {
    if (open) {
      OpenNode bottom = getBottom();
      Namespaces ns = bottom.ns;
      for (Iterator<AttrStub> it = attrs.iterator(); it.hasNext();) {
        AttrStub a = it.next();
        Namespaces fromAttr = a.toNamespace(ns, mq);
        if (fromAttr != null) {
          ns = fromAttr;
          it.remove();
        }
      }

      Namespaces elNs = ns.forElementName(elQName);
      if (elNs == null) {
        elNs = ns = unknownNamespace(start.pos, ns, elQName, mq);
      }
      Element newElement = doc.createElementNS(elNs.uri, elQName);
      for (AttrStub a : attrs) {
        String attrQName = a.nameTok.text;
        Namespaces attrNs = ns.forAttrName(elNs, attrQName);
        if (attrNs == null) {
          attrNs = ns = unknownNamespace(a.nameTok.pos, ns, attrQName, mq);
        }
        String localAttrName = Namespaces.localName(attrNs.uri, attrQName);
        if (!newElement.hasAttributeNS(attrNs.uri, localAttrName)) {
          if (needsDebugData) {
            Attr attrNode = a.toAttr(doc, attrNs.uri, attrQName);
            newElement.setAttributeNodeNS(attrNode);
          } else {
            newElement.setAttributeNS(attrNs.uri, attrQName, a.value);
          }
        } else {
          mq.addMessage(
              MessageType.DUPLICATE_ATTRIBUTE, a.nameTok.pos,
              MessagePart.Factory.valueOf(attrQName),
              Nodes.getFilePositionFor(
                  newElement.getAttributeNodeNS(attrNs.uri, localAttrName)));
        }
      }
      if (needsDebugData) {
        Nodes.setFilePositionFor(
            newElement, FilePosition.span(start.pos, end.pos));
      }
      // Does the tag end immediately?
      if ("/>".equals(end.text)) {
        doAppend(newElement, bottom.n);
      } else {
        push(newElement, ns, elQName);
      }
    } else {
      String bottomElementName = getBottom().qname;
      if (!elQName.equals(bottomElementName)) {
        throw new IllegalDocumentStateException(new Message(
            DomParserMessageType.UNMATCHED_END,
            start.pos, MessagePart.Factory.valueOf(start.text),
            MessagePart.Factory.valueOf("<" + bottomElementName)));
      }
      popN(1, end.pos);
    }
  }

  /** {@inheritDoc} */
  public void processText(Token<HtmlTokenType> text) {
    Node parent = getBottom().n;

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

  /**
   * Adds the given comment node to the DOM.
   */
  public void processComment(Token<HtmlTokenType> commentToken) {
    String text = commentToken.text.substring("<!--".length(),
        commentToken.text.lastIndexOf("--"));
    Comment comment = doc.createComment(text);
    if (needsDebugData) {
      Nodes.setFilePositionFor(comment, commentToken.pos);
    }
    doAppend(comment, getBottom().n);
  }

  /** {@inheritDoc} */
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
