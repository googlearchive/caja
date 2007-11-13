// Copyright (C) 2006 Google Inc.
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
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.Token;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.RenderContext;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;

import java.util.Collections;
import java.util.List;

/**
 * A ParseTreeNode implementation of an xml dom.
 *
 * <p>See inner classes for specifics of node types.
 * As with other ParseTreeNodes, each node is completely described by its
 * class, its children, and a single value.  For DomTrees, the value is always
 * a string.</p>
 *
 * <p>Each node, for convenience, is marked with the token that started it in
 * the parsed markup.</p>
 *
 * @author mikesamuel@gmail.com
 */
public abstract class DomTree extends AbstractParseTreeNode<DomTree> {
  private final Token<HtmlTokenType> start;
  private String value;

  DomTree(
      List<DomTree> children,
      Token<HtmlTokenType> start, Token<HtmlTokenType> end) {
    this.start = start;
    setFilePosition(FilePosition.span(start.pos, end.pos));

    this.children.addAll(children);
    childrenChanged();
    parentify(false);
  }

  /**
   * The type of the start token.
   */
  public HtmlTokenType getType() { return start.type; }

  /** The first token in the node's textual representation. */
  public Token<HtmlTokenType> getToken() { return start; }

  /**
   * The node value.  For elements and attributes, the name of that element or
   * tag.  For text and comment nodes, the textual content with XML entities
   * expanded.
   */
  @Override
  public String getValue() {
    if (null == value) { value = computeValue(); }
    return value;
  }

  private String computeValue() {
    switch (start.type) {
    case TAGBEGIN:
      return start.text.substring(1);  // tag name
    case ATTRNAME:
      return start.text;
    case ATTRVALUE:
      {
        String s = start.text;
        if (s.length() >= 2 && ('"' == s.charAt(0) || '\'' == s.charAt(0))) {
          s = s.substring(1, s.length() - 1);
        }
        return xmlDecode(s);
      }
    case TEXT:
      return xmlDecode(start.text);
    case CDATA:
      // Strip <![CDATA[ and ]]>
      return start.text.substring(9, start.text.length() - 3);
    default:
      return null;
    }
  }

  private static final InputSource DECODE =
      new InputSource(URI.create("decode:///xml"));
  /**
   * Converts a string of mime-type application/xml to text/plain by unescaping
   * XML entities such as <code>&amp;lt;</code>.  This does not process
   * external entities.
   */
  private static String xmlDecode(String s) {
    StringBuilder sb = new StringBuilder(s.length());
    CharProducer cp = CharProducer.Factory.fromHtmlAttribute(
        CharProducer.Factory.create(new StringReader(s), DECODE));
    try {
      try {
        for (int ch; (ch = cp.read()) >= 0;) {
          sb.append((char) ch);
        }
      } finally {
        cp.close();
      }
    } catch (IOException ex) {
      throw (AssertionError) new AssertionError(
          "IOException thrown using StringReader").initCause(ex);
    }
    return sb.toString();
  }

  /** Not yet implemented. */
  public void render(RenderContext r) throws IOException {
    // TODO: implement me
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    try {
      formatSelf(new MessageContext(), sb);
    } catch (IOException ex) {
      throw new AssertionError("StringBuilders shouldn't throw IOExceptions");
    }
    return sb.toString();
  }

  /**
   * A DOM element.  This node's value is its tag name.
   * Its chilren include its attributes, and the element content:
   * other tags, text nodes, CDATA sections.
   */
  public static final class Tag extends DomTree {
    Tag(List<DomTree> children,
        Token<HtmlTokenType> start, Token<HtmlTokenType> end) {
      super(children, start, end);
      assert start.type == HtmlTokenType.TAGBEGIN;
    }

    public String getTagName() { return getValue(); }
  }

  /**
   * An element attribute.
   * This node's value is the attribute name.
   * Its sole child is the {@link Value attribute value}.
   */
  public static final class Attrib extends DomTree {
    Attrib(Value value, Token<HtmlTokenType> start, Token<HtmlTokenType> end) {
      super(Collections.<DomTree>singletonList(value), start, end);
      assert start.type == HtmlTokenType.ATTRNAME;
    }
    public String getAttribName() { return getValue(); }
    public String getAttribValue() { return children().get(0).getValue(); }
    public Value getAttribValueNode() { return (Value) children().get(0); }
  }

  /**
   * An attribute value.  This nodes value is the attribute value text.
   */
  public static final class Value extends DomTree {
    Value(Token<HtmlTokenType> tok) {
      super(Collections.<DomTree>emptyList(), tok, tok);
      assert tok.type == HtmlTokenType.ATTRVALUE;
    }
  }

  /**
   * A text node.  Its value is textual content.
   */
  public static final class Text extends DomTree {
    Text(Token<HtmlTokenType> tok) {
      super(Collections.<DomTree>emptyList(), tok, tok);
      assert tok.type == HtmlTokenType.TEXT;
    }
  }

  /**
   * A CDATA section.  Its value is textual content.
   */
  public static final class CData extends DomTree {
    CData(Token<HtmlTokenType> tok) {
      super(Collections.<DomTree>emptyList(), tok, tok);
      assert tok.type == HtmlTokenType.CDATA;
    }
  }
}
