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
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Strings;

import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import nu.validator.htmlparser.common.DoctypeExpectation;
import nu.validator.htmlparser.impl.Tokenizer;

/**
 * A bridge between DomParser and html5lib which translates
 * {@code Token<HtmlTokenType>}s into SAX style events which are fed to the
 * TreeBuilder.  The TreeBuilder responds by issuing {@code createElement}
 * commands which are used to build a {@link DomTree}.
 *
 * @author mikesamuel@gmail.com
 */
public class Html5ElementStack implements OpenElementStack {
  private final CajaTreeBuilder builder = new CajaTreeBuilder();
  private final char[] charBuf = new char[1024];
  private final MessageQueue mq;
  private boolean isFragment;

  /** @param queue will receive error messages from html5lib. */
  Html5ElementStack(MessageQueue queue) {
    this.mq = queue;
  }

  /** @inheritDoc */
  public void open(boolean isFragment) {
    this.isFragment = isFragment;
    builder.setDoctypeExpectation(DoctypeExpectation.NO_DOCTYPE_ERRORS);
    try {
      builder.start(new Tokenizer(builder));
    } catch (SAXException ex) {
      throw new RuntimeException(ex);
    }
    builder.setErrorHandler(
        new ErrorHandler() {
          private FilePosition lastPos;
          private String lastMessage;

          public void error(SAXParseException ex) {
            // htmlparser is a bit strident, so we lower it's warnings to
            // MessageLevel.LINT.
            report(MessageLevel.LINT, ex);
          }
          public void fatalError(SAXParseException ex) {
            report(MessageLevel.FATAL_ERROR, ex);
          }
          public void warning(SAXParseException ex) {
            report(MessageLevel.LINT, ex);
          }

          private void report(MessageLevel level, SAXParseException ex) {
            String message = errorMessage(ex);
            FilePosition pos = builder.getErrorLocation();
            if (message.equals(lastMessage) && pos.equals(lastPos)) { return; }
            lastMessage = message;
            lastPos = pos;
            mq.getMessages().add(new Message(
                DomParserMessageType.GENERIC_SAX_ERROR, level, pos,
                MessagePart.Factory.valueOf(message)));
          }

          private String errorMessage(SAXParseException ex) {
            // Don't ask.
            return ex.getMessage()
                .replace('\u201c', '\'').replace('\u201d', '\'');
          }
        });
  }

  /** @inheritDoc */
  public void finish(FilePosition endOfFile) {
    builder.finish(endOfFile);
    builder.closeUnclosedNodes();
  }

  /** @inheritDoc */
  public String canonicalizeElementName(String elementName) {
    return canonicalElementName(elementName);
  }

  /** @inheritDoc */
  public String canonicalizeAttributeName(String attributeName) {
    return canonicalAttributeName(attributeName);
  }

  public static String canonicalElementName(String elementName) {
    // forces LANG=C like behavior.
    return Strings.toLowerCase(elementName);
  }

  public static String canonicalAttributeName(String attributeName) {
    // forces LANG=C like behavior.
    return Strings.toLowerCase(attributeName);
  }

  /** @inheritDoc */
  public DomTree.Fragment getRootElement() {
    // libHtmlParser always produces a document with html, head, and body tags
    // which we usually don't want, so unroll it.

    // If we can't throw away the head element, and the body header, then we
    // return the entire document.  Otherwise, we return a document fragment
    // consisting of the contents of the body.

    DomTree.Tag root = builder.getRootElement();
    DomTree.Fragment result = new DomTree.Fragment();
    result.setFilePosition(builder.getFragmentBounds());
    if (!isFragment) {
      result.appendChild(root);
      return result;
    }

    final List<? extends DomTree> children = root.children();

    // If disposing of the html, body, or head elements would lose info don't
    // do it, so look for attributes.
    boolean tagsBesidesHeadAndBody = false;
    boolean topLevelTagsWithAttributes = false;

    for (DomTree child : children) {
      if (child instanceof DomTree.Attrib) {
        topLevelTagsWithAttributes = true;
        break;
      } else if (child instanceof DomTree.Tag) {
        DomTree.Tag el = (DomTree.Tag) child;
        if (!("head".equals(el.getTagName())
              || "body".equals(el.getTagName()))) {
          tagsBesidesHeadAndBody = true;
          break;
        }
        if (!el.children().isEmpty()
            && el.children().get(0) instanceof DomTree.Attrib) {
          topLevelTagsWithAttributes = true;
          break;
        }
      }
    }

    // topLevelTagsWithAttributes is true in the following cases
    //   <html xml:lang="en">...</html>
    //   <html><body bgcolor=white>...</body></html>
    // tagsBesidesHeadAndBody is true for
    //   <html><frameset>...</frameset></html>
    if (tagsBesidesHeadAndBody || topLevelTagsWithAttributes) {
      // Merging the body and head would lose info.
      result.appendChild(root);
      return result;
    }

    // Merge the body and head into a fragment.
    // Convert
    // <html>
    //   <head>
    //     <link rel=stylesheet ...>
    //   </head>
    //   <body>
    //     <p>Hello World</p.
    //   </body>
    // </html>
    // to
    // #fragment
    //   <link rel=stylesheet ...>
    //   <p>Hello World</p.

    MutableParseTreeNode.Mutation mutation = result.createMutation();
    DomTree pending = null;
    for (DomTree child : children) {
      if (child instanceof DomTree.Tag) {
        // Shallow descent
        for (DomTree grandchild : child.children()) {
          pending = appendNormalized(pending, grandchild, mutation);
        }
      } else {
        pending = appendNormalized(pending, child, mutation);
      }
    }
    if (pending != null) { mutation.appendChild(pending); }

    mutation.execute();
    return result;
  }

  /**
   * Given one or two nodes, see if the two can be combined.
   * If two are passed in, they might be combined into one and returned, or
   * the first will be appended via mut, and the other returned.
   */
  private DomTree appendNormalized(
      DomTree pending, DomTree current, MutableParseTreeNode.Mutation mut) {
    if (pending == null) { return current; }
    Token<HtmlTokenType> pendingToken = pending.getToken();
    Token<HtmlTokenType> currentToken = current.getToken();
    if (!(HtmlTokenType.TEXT == pendingToken.type
          && HtmlTokenType.TEXT == currentToken.type)) {
      mut.appendChild(pending);
      return current;
    }
    return new DomTree.Text(
        Token.instance(
            pendingToken.text + currentToken.text, HtmlTokenType.TEXT,
            FilePosition.span(pendingToken.pos, currentToken.pos)));
  }

  /**
   * Records the fact that a tag has been seen, updating internal state
   *
   * @param start the token of the beginning of the tag, so {@code "<p"} for a
   *   paragraph start, {@code "</p"} for an end tag.
   * @param end the token of the beginning of the tag, so {@code ">"} for a
   *   paragraph start, {@code "/>"} for an unary break tag.
   * @param attrs the attributes for the element.  This will be empty
   *   for end tags.
   */
  public void processTag(Token<HtmlTokenType> start, Token<HtmlTokenType> end,
                         List<DomTree.Attrib> attrs) {
    builder.setTokenContext(start, end);
    AttributesImpl attrsWrapped = new AttributesImpl(attrs);
    try {
      String tagName = CajaTreeBuilder.tagName(start.text);
      if (CajaTreeBuilder.isEndTag(start.text)) {
        builder.endTag(tagName, attrsWrapped);
      } else {
        builder.startTag(tagName, attrsWrapped);
      }
    } catch (SAXException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Adds the given text node to the DOM.
   */
  public void processText(Token<HtmlTokenType> textToken) {
    builder.setTokenContext(textToken, textToken);
    String text = textToken.text;
    char[] chars;
    int n = text.length();
    if (n <= charBuf.length) {
      chars = charBuf;
      text.getChars(0, n, chars, 0);
    } else {
      chars = text.toCharArray();
    }
    try {
      builder.characters(chars, 0, n);
    } catch (SAXException ex) {
      throw new RuntimeException(ex);
    }
  }
}
