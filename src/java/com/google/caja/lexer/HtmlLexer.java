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
import java.util.HashSet;
import java.util.Set;

/**
 * A flexible lexer for html, gxp, and related document types.
 *
 * @author mikesamuel@gmail.com
 */
public final class HtmlLexer extends AbstractTokenStream<HtmlTokenType> {
  private final HtmlInputSplitter splitter;
  private Token<HtmlTokenType> lookahead;
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
    Token<HtmlTokenType> token;
    if (null != lookahead) {
      token = lookahead;
      lookahead = null;
    } else if (splitter.hasNext()) {
      token = splitter.next();
    } else {
      return null;
    }

    switch (token.type) {

      // Keep track of whether we're inside a tag or not.
      case TAGBEGIN:
        state = State.IN_TAG;
        break;
      case TAGEND:
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
                token = collapseSubsequent(token);
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
   * The lookahead is left in this.lookahead, so that it will be used on next
   * fetch.
   */
  private Token<HtmlTokenType> collapseSubsequent(Token<HtmlTokenType> token)
      throws ParseException {
    if (null != lookahead) { throw new IllegalStateException(); }
    Token<HtmlTokenType> collapsed = token;
    while (splitter.hasNext()
           && collapsed.type == (lookahead = splitter.next()).type) {
      // collapse adjacent text nodes
      collapsed = Token.instance(
          collapsed.text + lookahead.text, collapsed.type,
          FilePosition.span(collapsed.pos, lookahead.pos));
      lookahead = null;
    }
    return collapsed;
  }
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
  private final CharProducer p;
  /** True iff the current character is inside a tag. */
  private boolean inTag;
  /**
   * True if inside a script, xmp, listing, or similar tag whose content does
   * not follow the normal escaping rules.
   */
  private boolean inEscapeExemptBlock;
  /** One character of lookahead */
  private int lookahead = -1;
  private CharProducer.MutableFilePosition lookaheadStart =
      new CharProducer.MutableFilePosition();

  /**
   * Null or the name of the close tag required to end the current escape exempt
   * block.
   * Preformatted tags include &lt;script&gt;, &lt;xmp&gt;, etc. that may
   * contain unescaped html input.
   */
  private String escapeExemptTagName = null;

  public HtmlInputSplitter(CharProducer p) { this.p = p; }

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
          && canonTagName(token.text.substring(2)).equals(escapeExemptTagName)
          ) {
        inEscapeExemptBlock = false;
        escapeExemptTagName = null;
      } else if (token.type != HtmlTokenType.SERVERCODE) {
        token = reclassify(token, HtmlTokenType.UNESCAPED);
      }
    } else {
      switch (token.type) {
        case TAGBEGIN:
          {
            String tagName = token.text.substring(1);
            if (this.isEscapeExemptTagName(tagName)) {
              this.escapeExemptTagName = canonTagName(tagName);
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
    ;
  }

  private int read() throws IOException {
    p.getCurrentPosition(lookaheadStart);
    return p.read();
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
      int ch;
      FilePosition start;
      if (lookahead >= 0) {
        start = lookaheadStart.toFilePosition();
        ch = lookahead;
        lookahead = -1;
      } else {
        start = p.getCurrentPosition();
        ch = p.read();
        if (ch < 0) { return null; }
      }

      HtmlTokenType type;
      StringBuilder text = new StringBuilder(128);
      text.append((char) ch);
      if (inTag) {
        if ('>' == ch) {
          type = HtmlTokenType.TAGEND;
          inTag = false;
        } else if ('/' == ch) {
          ch = read();
          if ('>' == ch) {
            type = HtmlTokenType.TAGEND;
            text.append((char) ch);
            inTag = false;
          } else {
            lookahead = ch;
            type = HtmlTokenType.TEXT;
          }
        } else if ('=' == ch) {
          type = HtmlTokenType.TEXT;
        } else if ('"' == ch || '\'' == ch) {
          type = HtmlTokenType.QSTRING;
          int delim = ch;
          while ((ch = read()) >= 0) {
            text.append((char) ch);
            if (delim == ch) { break; }
          }
        } else if (!Character.isWhitespace((char) ch)) {
          type = HtmlTokenType.TEXT;
          while ((ch = read()) >= 0) {
            if ('>' == ch || '=' == ch || '"' == ch || '\'' == ch
                || Character.isWhitespace((char) ch)) {
              lookahead = ch;
              break;
            }
            text.append((char) ch);
          }
        } else {
          // We skip whitespace tokens inside tag bodies.
          type = HtmlTokenType.IGNORABLE;
          while ((ch = read()) >= 0) {
            if (!Character.isWhitespace((char) ch)) {
              lookahead = ch;
              break;
            }
            text.append((char) ch);
          }
        }
      } else {
        if (ch == '<') {
          ch = read();
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
              case '?':
                if (!this.inEscapeExemptBlock) {
                  // Comments, directives, and cdata suppressed in escape
                  // exempt mode as they could obscure the close of the
                  // escape exempty block.
                  state = '!' == ch ? State.BANG : State.APP_DIRECTIVE;
                } else {
                  text.append((char) ch);
                }
                break;
              case '%':
                state = State.SERVER_CODE;
                break;
              default:
                if (Character.isLetter(ch)) {
                  state = State.TAGNAME;
                } else if ('<' == ch) {
                  lookahead = ch;
                  type = HtmlTokenType.TEXT;
                } else {
                  text.append((char) ch);
                }
                break;
            }
            if (null != state) {
              text.append((char) ch);
              charloop:
              while ((ch = read()) >= 0) {
                switch (state) {
                  case TAGNAME:
                    if (Character.isWhitespace((char) ch)
                        || '>' == ch || '/' == ch) {
                      lookahead = ch;
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
                        lookahead = ch;
                        type = HtmlTokenType.TEXT;
                      } else {
                        text.append((char) ch);
                      }
                      break charloop;
                    }
                    break;
                  case BANG:
                    if ('[' == ch) {
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
        while ((ch = read()) >= 0 && '<' != ch) {
          text.append((char) ch);
        }
        lookahead = ch;
        type = HtmlTokenType.TEXT;
      }

      FilePosition end = lookahead < 0
          ? p.getCurrentPosition()
          : this.lookaheadStart.toFilePosition();
      return Token.instance(
          text.toString(), type, FilePosition.span(start, end));
    } catch (IOException ex) {
      throw new ParseException(
          new Message(MessageType.PARSE_ERROR, p.getCurrentPosition()), ex);
    }
  }

  private static final Set<String> ESCAPE_EXEMPT_TAGS = new HashSet<String>();
  static {
    ESCAPE_EXEMPT_TAGS.add("listing");
    ESCAPE_EXEMPT_TAGS.add("script");
    ESCAPE_EXEMPT_TAGS.add("xmp");
  }

  protected boolean isEscapeExemptTagName(String tagName) {
    return !asXml && ESCAPE_EXEMPT_TAGS.contains(canonTagName(tagName));
  }

  protected String canonTagName(String tagName) {
    return asXml ? tagName : tagName.toLowerCase();
  }

  static <T extends TokenType>
  Token<T> reclassify(Token<T> token, T type) {
    return Token.instance(token.text, type, token.pos);
  }
}
