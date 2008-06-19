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
import com.google.caja.lexer.HtmlTextEscapingMode;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.render.Concatenator;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;

import java.util.ArrayList;
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

  DomTree(List<? extends DomTree> children,
          Token<HtmlTokenType> start, Token<HtmlTokenType> end) {
    this(children, start,
         start.pos != null ? FilePosition.span(start.pos, end.pos) : null);
  }

  DomTree(List<? extends DomTree> children, Token<HtmlTokenType> tok,
          FilePosition pos) {
    this.start = tok;
    if (pos != null) {
      setFilePosition(pos);
    }
    createMutation().appendChildren(children).execute();
  }

  /**
   * The type of the start token.
   */
  public HtmlTokenType getType() { return start.type; }

  /** The first token in the node's textual representation. */
  public Token<HtmlTokenType> getToken() { return start; }

  public final TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> exHandler) {
    return new Concatenator(out, exHandler);
  }

  /**
   * The node value.  For elements and attributes, the name of that element or
   * tag.  For text and comment nodes, the textual content with XML entities
   * expanded.
   */
  @Override
  public String getValue() {
    if (null == value && null != start) { value = computeValue(); }
    return value;
  }

  protected final void setValue(String value) {
    this.value = value;
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
        int n = s.length();
        if (n >= 2) {
          char ch0 = s.charAt(0);
          if (s.charAt(n - 1) == ch0
              && ('"' == ch0 || '\'' == ch0 || ch0 == '`')) {
            s = s.substring(1, n - 1);
          }
        }
        return xmlDecode(s);
      }
    case TEXT:
      return xmlDecode(start.text);
    case CDATA:
      // Strip <![CDATA[ and ]]>
      return start.text.substring(9, start.text.length() - 3);
    case UNESCAPED:
      return start.text;
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
   * A node that serves as a top level container for other nodes.
   * This can represent a snippet of markup.
   */
  public static final class Fragment extends DomTree {
    public Fragment() {
      super(Collections.<DomTree>emptyList(), NULL_TOKEN, NULL_TOKEN);
    }

    public Fragment(List<? extends DomTree> children) {
      super(children,
            Token.instance(
                "", HtmlTokenType.IGNORABLE,
                FilePosition.startOf(children.get(0).getFilePosition())),
            FilePosition.span(
                children.get(0).getFilePosition(),
                children.get(children.size() - 1).getFilePosition()));
    }

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      for (DomTree child : children()) {
        child.render(r);
      }
    }
  }
  private static final Token<HtmlTokenType> NULL_TOKEN
      = Token.instance("", HtmlTokenType.IGNORABLE, null);


  /**
   * A DOM element.  This node's value is its tag name.
   * Its chilren include its attributes, and the element content:
   * other tags, text nodes, CDATA sections.
   */
  public static final class Tag extends DomTree {
    public Tag(List<? extends DomTree> children,
               Token<HtmlTokenType> start, Token<HtmlTokenType> end) {
      super(children, start, end);
      assert start.type == HtmlTokenType.TAGBEGIN;
      assert end.type == HtmlTokenType.TAGEND;
    }

    public Tag(List<? extends DomTree> children,
               Token<HtmlTokenType> start, FilePosition pos) {
      super(children, start, pos);
      assert start.type == HtmlTokenType.TAGBEGIN;
    }

    void setTagName(String canonicalTagName) { setValue(canonicalTagName); }

    public String getTagName() { return getValue(); }

    public List<Attrib> getAttributeNodes() {
      List<Attrib> attribs = new ArrayList<Attrib>();
      for (DomTree child : children()) {
        if (!(child instanceof Attrib)) { break; }
        attribs.add((Attrib) child);
      }
      return attribs;
    }
    
    /**
     * Returns the first attribute of this element with the given name or null
     * if there is none.
     */
    public Attrib getAttribute(String name) {
      for (DomTree child : children()) {
        if (!(child instanceof Attrib)) { break; }
        Attrib attrib = (Attrib) child;
        if (name.equals(attrib.getAttribName())) { return attrib; }
      }
      return null;
    }

    public void render(RenderContext r) {
      TokenConsumer out = r.getOut();
      r.getOut().mark(getFilePosition());
      out.consume("<");
      renderHtmlIdentifier(getTagName(), r);
      List<? extends DomTree> children = children();
      int n = children.size();
      int i = 0;
      while (i < n) {
        DomTree child = children.get(i);
        if (!(child instanceof Attrib)) { break; }
        out.consume(" ");
        child.render(r);
        ++i;
      }
      if (i == n
          && ((r instanceof MarkupRenderContext
               && ((MarkupRenderContext) r).asXml())
              || HtmlTextEscapingMode.isVoidElement(getTagName()))) {
        // This is safe regardless of whether the output is XML or HTML since
        // we only skip the end tag for HTML elements that don't require one,
        // and the slash will cause XML to treat it as a void tag.
        out.consume(" />");
      } else {
        out.consume(">");
        while (i < n) {
          children.get(i++).render(r);
        }
        // This is not correct for HTML <plaintext> nodes, but live with it,
        // since handling plaintext correctly would require omitting end tags
        // for parent nodes, and so significantly complicate rendering for a
        // node we shouldn't ever render anyway.
        out.mark(FilePosition.endOfOrNull(getFilePosition()));
        out.consume("</");
        renderHtmlIdentifier(getTagName(), r);
        out.consume(">");
      }
    }
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
    public Attrib(Value value, Token<HtmlTokenType> start, FilePosition pos) {
      super(Collections.<DomTree>singletonList(value), start, pos);
      assert start.type == HtmlTokenType.ATTRNAME;
    }
    public String getAttribName() { return getValue(); }
    public String getAttribValue() { return children().get(0).getValue(); }
    public Value getAttribValueNode() { return (Value) children().get(0); }

    void setAttribName(String canonicalName) { setValue(canonicalName); }

    public void render(RenderContext r) {
      TokenConsumer out = r.getOut();
      out.mark(getFilePosition());
      renderHtmlIdentifier(getAttribName(), r);
      out.consume("=\"");
      getAttribValueNode().render(r);
      out.consume("\"");
    }

    @Override
    public Attrib clone() {
      Attrib clone = new Attrib(
          getAttribValueNode().clone(), getToken(), getFilePosition());
      clone.setAttribName(getAttribName());
      return clone;
    }
  }

  /**
   * An attribute value.  This nodes value is the attribute value text.
   */
  public static final class Value extends DomTree {
    public Value(Token<HtmlTokenType> tok) {
      super(Collections.<DomTree>emptyList(), tok, tok);
      assert tok.type == HtmlTokenType.ATTRVALUE;
    }

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      renderHtmlAttributeValue(getValue(), r);
    }

    @Override
    public Value clone() {
      return new Value(getToken());
    }
  }

  /**
   * A text node.  Its value is textual content.
   */
  public static final class Text extends DomTree {
    public Text(Token<HtmlTokenType> tok) {
      super(Collections.<DomTree>emptyList(), tok, tok);
      assert tok.type == HtmlTokenType.TEXT
          || tok.type == HtmlTokenType.UNESCAPED;
    }

    public void render(RenderContext r) {
      r.getOut().mark(getFilePosition());
      if (getToken().type == HtmlTokenType.UNESCAPED
          && !(r instanceof MarkupRenderContext
               && ((MarkupRenderContext) r).asXml())) {
        r.getOut().consume(getValue());
      } else {
        renderHtml(getValue(), r);
      }
    }
  }

  /**
   * A CDATA section.  Its value is textual content.
   */
  public static final class CData extends DomTree {
    public CData(Token<HtmlTokenType> tok) {
      super(Collections.<DomTree>emptyList(), tok, tok);
      assert tok.type == HtmlTokenType.CDATA;
    }

    public void render(RenderContext r) {
      TokenConsumer out = r.getOut();
      out.mark(getFilePosition());
      String value = getValue();
      if (!value.contains("]]>")
          && (r instanceof MarkupRenderContext
              && ((MarkupRenderContext) r).asXml())) {
        out.consume("<![CDATA[");
        out.consume(value);
        out.consume("]]>");
      } else {
        renderHtml(value, r);
      }
    }
  }

  private static void renderHtmlIdentifier(String text, RenderContext r) {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeXml(text, r.isAsciiOnly(), sb);
    r.getOut().consume(sb.toString());
  }

  private static void renderHtmlAttributeValue(String text, RenderContext r) {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeXml(text, r.isAsciiOnly(), sb);
    r.getOut().consume(sb.toString());
  }

  private static void renderHtml(String text, RenderContext r) {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeXml(text, r.isAsciiOnly(), sb);
    r.getOut().consume(sb.toString());
  }
}
