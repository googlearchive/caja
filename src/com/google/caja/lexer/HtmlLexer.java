// Copyright (C) 2005 Google Inc.
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

package com.google.caja.lexer;

import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageType;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Set;

/**
 * A flexible lexer for html, gxp, and related document types.
 *
 * @author mikesamuel@gmail.com
 */
public final class HtmlLexer extends AbstractTokenStream<HtmlTokenType> {
  private final HtmlInputSplitter splitter;
  private State state = State.OUTSIDE_TAG;

  public HtmlLexer(CharProducer p) {
    this.splitter = new HtmlInputSplitter(p);
  }

  /**
   * True iff this is treated as xml.  Xml-ness affects the treatment of
   * script tags, which must be CDATA or html-escaped in GXPs and other xml
   * types, but are specially handled by html-parsers.
   */
  public boolean getTreatedAsXml() {
    return splitter.getTreatedAsXml();
  }

  /** @see #getTreatedAsXml */
  public void setTreatedAsXml(boolean asXml) {
    splitter.setTreatedAsXml(asXml);
  }

  /**
   * An fsm that lets us reclassify text tokensinside tags as attribute
   * names/values
   */
  private static enum State {
    OUTSIDE_TAG,
    IN_TAG,
    SAW_NAME,
    SAW_EQ,
    ;
  }

  /**
   * Makes sure that this.token contains a token if one is available.
   * This may require fetching and combining multple tokens from the underlying
   * splitter.
   */
  @Override
  protected Token<HtmlTokenType> produce() throws ParseException {
    Token<HtmlTokenType> token = readToken();
    if (token == null) { return null; }

    switch (token.type) {

      // Keep track of whether we're inside a tag or not.
      case TAGBEGIN:
        state = State.IN_TAG;
        break;
      case TAGEND:
        if (state == State.SAW_EQ && HtmlTokenType.TAGEND == token.type
            && !getTreatedAsXml()) {
          // Distinguish <input type=checkbox checked=> from
          // <input type=checkbox checked>
          pushbackToken(token);
          state = State.IN_TAG;
          return Token.instance("", HtmlTokenType.ATTRVALUE,
                                FilePosition.startOf(token.pos));
        }

        state = State.OUTSIDE_TAG;
        break;

      // Drop ignorable tokens by zeroing out the one received and recursing
      case IGNORABLE:
        return produce();

      // collapse adjacent text nodes if we're outside a tag, or otherwise,
      // Recognize attribute names and values.
      default:
        switch (state) {
          case OUTSIDE_TAG:
            if (HtmlTokenType.TEXT == token.type
                || HtmlTokenType.UNESCAPED == token.type) {
              token = collapseSubsequent(token);
            }
            break;
          case IN_TAG:
            if (HtmlTokenType.TEXT == token.type && !"=".equals(token.text)) {
              // Reclassify as attribute name
              token = HtmlInputSplitter.reclassify(
                  token, HtmlTokenType.ATTRNAME);
              state = State.SAW_NAME;
            }
            break;
          case SAW_NAME:
            if (HtmlTokenType.TEXT == token.type) {
              if ("=".equals(token.text)) {
                state = State.SAW_EQ;
                // Skip the '=' token
                return produce();
              } else {
                // Reclassify as attribute name
                token = HtmlInputSplitter.reclassify(
                    token, HtmlTokenType.ATTRNAME);
              }
            } else {
              state = State.IN_TAG;
            }
            break;
          case SAW_EQ:
            if (HtmlTokenType.TEXT == token.type
                || HtmlTokenType.QSTRING == token.type) {
              if (HtmlTokenType.TEXT == token.type) {
                // Collapse adjacent text nodes to properly handle
                //   <a onclick=this.clicked=true>
                //   <a title=foo bar>
                token = collapseAttributeName(token);
              }
              // Reclassify as value
              token = HtmlInputSplitter.reclassify(
                  token, HtmlTokenType.ATTRVALUE);
              state = State.IN_TAG;
            }
            break;
        }
        break;
    }

    return token;
  }

  /**
   * Collapses all the following tokens of the same type into this.token.
   */
  private Token<HtmlTokenType> collapseSubsequent(Token<HtmlTokenType> token)
      throws ParseException {
    Token<HtmlTokenType> collapsed = token;
    for (Token<HtmlTokenType> next;
         (next= peekToken(0)) != null && next.type == token.type;
         readToken()) {
      collapsed = join(collapsed, next);
    }
    return collapsed;
  }

  private Token<HtmlTokenType> collapseAttributeName(Token<HtmlTokenType> token)
      throws ParseException {
    if (getTreatedAsXml()) { return token; }
    // We want to collapse tokens into the value that are not parts of an
    // attribute value.  We should include any space or text adjacent to the
    // value, but should stop at any of the following constructions:
    //   space end-of-file              e.g. name=foo_
    //   space valueless-attrib-name    e.g. name=foo checked
    //   space tag-end                  e.g. name=foo />
    //   space text space? '='          e.g. name=foo bar=
    int nToMerge = 0;
    for (Token<HtmlTokenType> t; (t = peekToken(nToMerge)) != null;) {
      if (t.type == HtmlTokenType.IGNORABLE) {
        Token<HtmlTokenType> text = peekToken(nToMerge + 1);
        if (text == null) { break; }
        if (text.type != HtmlTokenType.TEXT) { break; }
        if (isValuelessAttribute(text.text)) { break; }
        Token<HtmlTokenType> eq = peekToken(nToMerge + 2);
        if (eq != null && eq.type == HtmlTokenType.IGNORABLE) {
          eq = peekToken(nToMerge + 3);
        }
        if (eq == null || "=".equals(eq.text)) { break; }
      } else if (t.type != HtmlTokenType.TEXT) {
        break;
      }
      ++nToMerge;
    }
    if (nToMerge == 0) { return token; }
    StringBuilder sb = new StringBuilder(token.text);
    Token<HtmlTokenType> t;
    do {
      t = readToken();
      sb.append(t.text);
    } while (--nToMerge > 0);
    return Token.instance(
        sb.toString(), HtmlTokenType.TEXT, FilePosition.span(token.pos, t.pos));
  }

  private static Token<HtmlTokenType> join(
      Token<HtmlTokenType> a, Token<HtmlTokenType> b) {
    return Token.instance(
        a.text + b.text, a.type, FilePosition.span(a.pos, b.pos));
  }

  private final LinkedList<Token<HtmlTokenType>> lookahead
      = new LinkedList<Token<HtmlTokenType>>();
  private Token<HtmlTokenType> readToken() throws ParseException {
    if (!lookahead.isEmpty()) {
      return lookahead.remove();
    } else if (splitter.hasNext()) {
      return splitter.next();
    } else {
      return null;
    }
  }

  private Token<HtmlTokenType> peekToken(int i) throws ParseException {
    while (lookahead.size() <= i && splitter.hasNext()) {
      lookahead.add(splitter.next());
    }
    return lookahead.size() > i ? lookahead.get(i) : null;
  }

  private void pushbackToken(Token<HtmlTokenType> token) {
    lookahead.addFirst(token);
  }

  /** Can the attribute appear in HTML without a value. */
  private static boolean isValuelessAttribute(String attribName) {
    boolean valueless = VALUELESS_ATTRIB_NAMES.contains(
        attribName.toLowerCase(Locale.ENGLISH));
    return valueless;
  }

  // From http://issues.apache.org/jira/browse/XALANC-519
  private static final Set<String> VALUELESS_ATTRIB_NAMES = new HashSet<String>(
      Arrays.asList("checked", "compact", "declare", "defer", "disabled",
                    "ismap", "multiple", "nohref", "noresize", "noshade",
                    "nowrap", "readonly", "selected"));
}

/**
 * A token stream that breaks a character stream into <tt>
 * HtmlTokenType.{TEXT,TAGBEGIN,TAGEND,DIRECTIVE,COMMENT,CDATA,DIRECTIVE}</tt>
 * tokens.  The matching of attribute names and values is done in a later step.
 */
final class HtmlInputSplitter extends AbstractTokenStream<HtmlTokenType> {
  /** Should the input be considered xml?  are escape exempt blocks allowed? */
  private boolean asXml = false;

  /** The source of html character data. */
  private final LookaheadCharProducer p;
  /** True iff the current character is inside a tag. */
  private boolean inTag;
  /**
   * True if inside a script, xmp, listing, or similar tag whose content does
   * not follow the normal escaping rules.
   */
  private boolean inEscapeExemptBlock;

  /**
   * Null or the name of the close tag required to end the current escape exempt
   * block.
   * Preformatted tags include &lt;script&gt;, &lt;xmp&gt;, etc. that may
   * contain unescaped html input.
   */
  private String escapeExemptTagName = null;

  private HtmlTextEscapingMode textEscapingMode;

  public HtmlInputSplitter(CharProducer p) {
    this.p = new LookaheadCharProducer(p, 2);
  }

  /**
   * True iff this is treated as xml.  Xml-ness affects the treatment of
   * script tags, which must be CDATA or html-escaped in GXPs and other xml
   * types, but are specially handled by html-parsers.
   */
  public boolean getTreatedAsXml() {
    return this.asXml;
  }

  /** @see #getTreatedAsXml */
  public void setTreatedAsXml(boolean asXml) {
    this.asXml = asXml;
  }

  /**
   * Make sure that there is a token ready to yield in this.token.
   */
  @Override
  protected Token<HtmlTokenType> produce() throws ParseException {
    Token<HtmlTokenType> token = parseToken();
    if (null == token) { return null; }

    // Handle escape-exempt blocks.
    // The parse() method is only dimly aware of escape-excempt blocks, so
    // here we detect the beginning and ends of escape exempt blocks, and
    // reclassify as UNESCAPED, any tokens that appear in the middle.
    if (inEscapeExemptBlock) {
      if (token.type == HtmlTokenType.TAGBEGIN && '/' == token.text.charAt(1)
          && textEscapingMode != HtmlTextEscapingMode.PLAIN_TEXT
          && canonTagName(token.text.substring(2)).equals(escapeExemptTagName)
          ) {
        this.inEscapeExemptBlock = false;
        this.escapeExemptTagName = null;
        this.textEscapingMode = null;
      } else if (token.type != HtmlTokenType.SERVERCODE) {
        // classify RCDATA as text since it can contain entities
        token = reclassify(
            token, (this.textEscapingMode == HtmlTextEscapingMode.RCDATA
                    ? HtmlTokenType.TEXT
                    : HtmlTokenType.UNESCAPED));
      }
    } else if (!asXml) {
      switch (token.type) {
        case TAGBEGIN:
          {
            String canonTagName = canonTagName(token.text.substring(1));
            if (HtmlTextEscapingMode
                .isTagFollowedByLiteralContent(canonTagName)) {
              this.escapeExemptTagName = canonTagName;
              this.textEscapingMode
                  = HtmlTextEscapingMode.getModeForTag(canonTagName);
            }
            break;
          }
        case TAGEND:
          this.inEscapeExemptBlock = null != this.escapeExemptTagName;
          break;
        default:
          break;
      }
    }
    return token;
  }

  /**
   * States for a state machine for optimistically identifying tags and other
   * html/xml/phpish structures.
   */
  private static enum State {
    TAGNAME,
    SLASH,
    BANG,
    CDATA,
    CDATA_SQ_1,
    CDATA_SQ_2,
    BANG_DASH,
    COMMENT,
    COMMENT_DASH,
    COMMENT_DASH_DASH,
    DIRECTIVE,
    DONE,
    APP_DIRECTIVE,
    APP_DIRECTIVE_QMARK,
    SERVER_CODE,
    SERVER_CODE_PCT,

    // From HTML 5 section 8.1.2.6

    // The text in CDATA and RCDATA elements must not contain any
    // occurences of the string "</" followed by characters that
    // case-insensitively match the tag name of the element followed
    // by one of U+0009 CHARACTER TABULATION, U+000A LINE FEED (LF),
    // U+000B LINE TABULATION, U+000C FORM FEED (FF), U+0020 SPACE,
    // U+003E GREATER-THAN SIGN (>), or U+002F SOLIDUS (/), unless
    // that string is part of an escaping text span.

    // An escaping text span is a span of text (in CDATA and RCDATA
    // elements) and character entity references (in RCDATA elements)
    // that starts with an escaping text span start that is not itself
    // in an escaping text span, and ends at the next escaping text
    // span end.

    // An escaping text span start is a part of text that consists of
    // the four character sequence "<!--".

    // An escaping text span end is a part of text that consists of
    // the three character sequence "-->".

    // An escaping text span start may share its U+002D HYPHEN-MINUS characters
    // with its corresponding escaping text span end.
    UNESCAPED_LT_BANG,             // <!
    UNESCAPED_LT_BANG_DASH,        // <!-
    ESCAPING_TEXT_SPAN,            // Inside an escaping text span
    ESCAPING_TEXT_SPAN_DASH,       // Seen - inside an escaping text span
    ESCAPING_TEXT_SPAN_DASH_DASH,  // Seen -- inside an escaping text span
    ;
  }

  /**
   * Breaks the character stream into tokens.
   * This method returns a stream of tokens such that each token starts where
   * the last token ended.
   *
   * <p>This property is useful as it allows fetch to collapse and reclassify
   * ranges of tokens based on state that is easy to maintain there.
   *
   * <p>Later passes are responsible for throwing away useless tokens.
   */
  private Token<HtmlTokenType> parseToken() throws ParseException {
    // TODO(mikesamuel): rewrite with a transition table or just use ANTLR
    try {
      FilePosition start = p.getCurrentPosition();
      int ch = p.read();
      if (ch < 0) { return null; }

      HtmlTokenType type;
      StringBuilder text = new StringBuilder(128);
      text.append((char) ch);
      if (inTag) {
        if ('>' == ch) {
          type = HtmlTokenType.TAGEND;
          inTag = false;
        } else if ('/' == ch) {
          ch = p.read();
          if ('>' == ch) {
            type = HtmlTokenType.TAGEND;
            text.append((char) ch);
            inTag = false;
          } else {
            p.pushback();
            type = HtmlTokenType.TEXT;
          }
        } else if ('=' == ch) {
          type = HtmlTokenType.TEXT;
        } else if ('"' == ch || '\'' == ch) {
          type = HtmlTokenType.QSTRING;
          int delim = ch;
          while ((ch = p.read()) >= 0) {
            text.append((char) ch);
            if (delim == ch) { break; }
          }
        } else if (!Character.isWhitespace((char) ch)) {
          type = HtmlTokenType.TEXT;
          while ((ch = p.read()) >= 0) {
            // End a text chunk before />
            if ('/' == ch) {
              p.pushback();
              p.fetch(2);  // Make sure we have space for 2 lookahead.
              if ('>' == p.peek(1)) {
                break;
              } else {
                p.read();  // re-consume '/'
              }
            } else if ('>' == ch || '=' == ch
                       || Character.isWhitespace((char) ch)) {
              p.pushback();
              break;
            } else if ('"' == ch || '\'' == ch) {
              p.pushback();
              p.fetch(2);
              int ch2 = p.peek(1);
              if (ch2 >= 0 && Character.isWhitespace((char) ch2)
                  || ch2 == '>' || ch2 == '/') {
                text.append((char) ch);
                p.consume(1);
                break;
              }
              p.consume(1);
            }
            text.append((char) ch);
          }
        } else {
          // We skip whitespace tokens inside tag bodies.
          type = HtmlTokenType.IGNORABLE;
          while ((ch = p.read()) >= 0) {
            if (!Character.isWhitespace((char) ch)) {
              p.pushback();
              break;
            }
            text.append((char) ch);
          }
        }
      } else {
        if (ch == '<') {
          ch = p.read();
          if (ch < 0) {
            type = HtmlTokenType.TEXT;
          } else {
            type = null;
            State state = null;
            switch (ch) {
              case '/':  // close tag?
                state = State.SLASH;
                break;
              case '!':  // Comment or declaration
                if (!this.inEscapeExemptBlock) {
                  state = State.BANG;
                } else if (HtmlTextEscapingMode
                           .allowsEscapingTextSpan(escapeExemptTagName)) {
                  // Directives, and cdata suppressed in escape
                  // exempt mode as they could obscure the close of the
                  // escape exempty block, but comments are similar to escaping
                  // text spans, and are significant in all CDATA and RCDATA
                  // blocks except those inside <xmp> tags.
                  // See "Escaping text spans" in section 8.1.2.6 of HTML5.
                  // http://www.w3.org/html/wg/html5/#cdata-rcdata-restrictions
                  state = State.UNESCAPED_LT_BANG;
                } else {
                  text.append((char) ch);
                }
                break;
              case '?':
                if (!this.inEscapeExemptBlock) {
                  state = State.APP_DIRECTIVE;
                } else {
                  text.append((char) ch);
                }
                break;
              case '%':
                state = State.SERVER_CODE;
                break;
              default:
                if (Character.isLetter(ch) && !this.inEscapeExemptBlock) {
                  state = State.TAGNAME;
                } else if ('<' == ch) {
                  p.pushback();
                  type = HtmlTokenType.TEXT;
                } else {
                  text.append((char) ch);
                }
                break;
            }
            if (null != state) {
              text.append((char) ch);
              charloop:
              while ((ch = p.read()) >= 0) {
                switch (state) {
                  case TAGNAME:
                    if (Character.isWhitespace((char) ch)
                        || '>' == ch || '/' == ch) {
                      p.pushback();
                      type = HtmlTokenType.TAGBEGIN;
                      inTag = true;
                      state = State.DONE;
                      break charloop;
                    }
                    break;
                  case SLASH:
                    if (Character.isLetter((char) ch)) {
                      state = State.TAGNAME;
                    } else {
                      if ('<' == ch) {
                        p.pushback();
                        type = HtmlTokenType.TEXT;
                      } else {
                        text.append((char) ch);
                      }
                      break charloop;
                    }
                    break;
                  case BANG:
                    if ('[' == ch && asXml) {
                      state = State.CDATA;
                    } else if ('-' == ch) {
                      state = State.BANG_DASH;
                    } else {
                      state = State.DIRECTIVE;
                    }
                    break;
                  case CDATA:
                    if (']' == ch) { state = State.CDATA_SQ_1; }
                    break;
                  case CDATA_SQ_1:
                    if (']' == ch) {
                      state = State.CDATA_SQ_2;
                    } else {
                      state = State.CDATA;
                    }
                    break;
                  case CDATA_SQ_2:
                    if ('>' == ch) {
                      type = HtmlTokenType.CDATA;
                      state = State.DONE;
                    } else if (']' != ch) {
                      state = State.CDATA;
                    }
                    break;
                  case BANG_DASH:
                    if ('-' == ch) {
                      state = State.COMMENT;
                    } else {
                      state = State.DIRECTIVE;
                    }
                    break;
                  case COMMENT:
                    if ('-' == ch) {
                      state = State.COMMENT_DASH;
                    }
                    break;
                  case COMMENT_DASH:
                    state = ('-' == ch)
                        ? State.COMMENT_DASH_DASH
                        : State.COMMENT_DASH;
                    break;
                  case COMMENT_DASH_DASH:
                    if ('>' == ch) {
                      state = State.DONE;
                      type = HtmlTokenType.COMMENT;
                    } else if ('-' == ch) {
                      state = State.COMMENT_DASH_DASH;
                    } else {
                      state = State.COMMENT_DASH;
                    }
                    break;
                  case DIRECTIVE:
                    if ('>' == ch) {
                      type = HtmlTokenType.DIRECTIVE;
                      state = State.DONE;
                    }
                    break;
                  case APP_DIRECTIVE:
                    if ('?' == ch) { state = State.APP_DIRECTIVE_QMARK; }
                    break;
                  case APP_DIRECTIVE_QMARK:
                    if ('>' == ch) {
                      type = HtmlTokenType.DIRECTIVE;
                      state = State.DONE;
                    } else if ('?' != ch) {
                      state = State.APP_DIRECTIVE;
                    }
                    break;
                  case SERVER_CODE:
                    if ('%' == ch) {
                      state = State.SERVER_CODE_PCT;
                    }
                    break;
                  case SERVER_CODE_PCT:
                    if ('>' == ch) {
                      type = HtmlTokenType.SERVERCODE;
                      state = State.DONE;
                    } else if ('%' != ch) {
                      state = State.SERVER_CODE;
                    }
                    break;
                  case UNESCAPED_LT_BANG:
                    if ('-' == ch) {
                      state = State.UNESCAPED_LT_BANG_DASH;
                    } else {
                      type = HtmlTokenType.TEXT;
                      state = State.DONE;
                    }
                    break;
                  case UNESCAPED_LT_BANG_DASH:
                    if ('-' == ch) {
                      // According to HTML 5 section 8.1.2.6

                      // An escaping text span start may share its
                      // U+002D HYPHEN-MINUS characters with its
                      // corresponding escaping text span end.
                      state = State.ESCAPING_TEXT_SPAN_DASH_DASH;
                    } else {
                      type = HtmlTokenType.TEXT;
                      state = State.DONE;
                    }
                    break;
                  case ESCAPING_TEXT_SPAN:
                    if ('-' == ch) {
                      state = State.ESCAPING_TEXT_SPAN_DASH;
                    }
                    break;
                  case ESCAPING_TEXT_SPAN_DASH:
                    if ('-' == ch) {
                      state = State.ESCAPING_TEXT_SPAN_DASH_DASH;
                    } else {
                      state = State.ESCAPING_TEXT_SPAN;
                    }
                    break;
                  case ESCAPING_TEXT_SPAN_DASH_DASH:
                    if ('>' == ch) {
                      type = HtmlTokenType.TEXT;
                      state = State.DONE;
                    } else if ('-' != ch) {
                      state = State.ESCAPING_TEXT_SPAN;
                    }
                    break;
                  case DONE:
                    throw new AssertionError();
                }
                text.append((char) ch);
                if (State.DONE == state) { break; }
              }
              if (ch < 0) {
                switch (state) {
                  case DONE:
                    break;
                  case CDATA:
                  case CDATA_SQ_1:
                  case CDATA_SQ_2:
                    type = HtmlTokenType.CDATA;
                    break;
                  case COMMENT:
                  case COMMENT_DASH:
                  case COMMENT_DASH_DASH:
                    type = HtmlTokenType.COMMENT;
                    break;
                  case DIRECTIVE:
                  case APP_DIRECTIVE:
                  case APP_DIRECTIVE_QMARK:
                    type = HtmlTokenType.DIRECTIVE;
                    break;
                  case SERVER_CODE:
                  case SERVER_CODE_PCT:
                    type = HtmlTokenType.SERVERCODE;
                    break;
                  default:
                    type = HtmlTokenType.TEXT;
                    break;
                }
              }
            }
          }
        } else {
          type = null;
        }
      }
      if (null == type) {
        while ((ch = p.read()) >= 0 && '<' != ch) {
          text.append((char) ch);
        }
        p.pushback();
        type = HtmlTokenType.TEXT;
      }

      FilePosition end = p.getCurrentPosition();
      return Token.instance(
          text.toString(), type, FilePosition.span(start, end));
    } catch (IOException ex) {
      throw new ParseException(
          new Message(MessageType.PARSE_ERROR, p.getCurrentPosition()), ex);
    }
  }

  protected String canonTagName(String tagName) {
    return asXml ? tagName : tagName.toLowerCase(Locale.ENGLISH);
  }

  static <T extends TokenType>
  Token<T> reclassify(Token<T> token, T type) {
    return Token.instance(token.text, type, token.pos);
  }
}
