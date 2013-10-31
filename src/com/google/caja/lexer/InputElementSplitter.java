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

/**
 * Splits lines into strings, comments, regular expression literals, and
 * blocks of non-whitespace.
 *
 * @author mikesamuel@gmail.com
 */
final class InputElementSplitter extends AbstractTokenStream<JsTokenType> {
  private final CharProducer p;
  /**
   * A trie used to split a chunk of text into punctuation tokens and
   * non-punctuation tokens.
   */
  private final PunctuationTrie<?> punctuation;
  /**
   * The last token that was not a comment token.
   * This can be used to decide whether to take a particular parsing path
   * based on token lookbehind, as javascript requires.
   */
  private Token<JsTokenType> lastNonCommentToken;

  /**
   * Whether we are parsing a quasiliteral pattern, as opposed to plain
   * JavaScript code.
   */
  private final boolean isQuasiliteral;

  public InputElementSplitter(CharProducer p, PunctuationTrie<?> punctuation) {
    this(p, punctuation, false);
  }

  public InputElementSplitter(CharProducer p, PunctuationTrie<?> punctuation,
                              boolean isQuasiliteral) {
    this.p = p;
    this.punctuation = punctuation;
    this.isQuasiliteral = isQuasiliteral;
  }

  @Override
  public Token<JsTokenType> next() throws ParseException {
    Token<JsTokenType> t = super.next();
    if (t.type != JsTokenType.COMMENT) {
      this.lastNonCommentToken = t;
    }
    return t;
  }

  @Override
  protected Token<JsTokenType> produce() throws ParseException {
    final char[] buf = p.getBuffer();
    int start = p.getOffset();
    final int limit = p.getLimit();
    if (start < limit && JsLexer.isJsSpace(buf[start])) {
      ++start;
      while (start < limit && JsLexer.isJsSpace(buf[start])) {
        ++start;
      }
      p.consumeTo(start);
    }

    if (p.isEmpty()) { return null; }

    JsTokenType type;
    int end = start + 1;
    char ch = buf[start];
    switch (ch) {
      case '"': case '\'':
      {
        boolean closed = false;
        boolean escaped = false;
        while (end < limit) {
          char ch2 = buf[end++];
          if (ch2 == ch && !escaped) {
            closed = true;
            break;
          } else if (!escaped && JsLexer.isJsLineSeparator(ch2)) {
            // will register as an unterminated string token below
          }
          escaped = !escaped && ch2 == '\\';
        }
        if (!closed) {
          throw new ParseException(
              new Message(
                  MessageType.UNTERMINATED_STRING_TOKEN,
                  p.filePositionForOffsets(start, end)));
        }
        type = JsTokenType.STRING;
        break;
      }
      case '/':
      {
        if (end == limit) {
          type = JsTokenType.PUNCTUATION;
        } else {
          char ch2 = buf[end];
          switch (ch2) {
          case '/':
            while (end < limit && !JsLexer.isJsLineSeparator(buf[end])) {
              ++end;
            }
            type = JsTokenType.COMMENT;
            break;
          case '*':
            {
              boolean star = false;
              boolean closed = false;
              while (++end < limit) {
                ch2 = buf[end];
                if (star && '/' == ch2) {
                  closed = true;
                  ++end;
                  break;
                } else {
                  star = (ch2 == '*');
                }
              }
              if (!closed) {
                throw new ParseException(
                    new Message(MessageType.UNTERMINATED_STRING_TOKEN,
                        p.filePositionForOffsets(start, p.getOffset())));
              }
              type = JsTokenType.COMMENT;
            }
            break;
          default:
            {
              if (lastNonCommentToken == null
                  || JsLexer.isRegexp(lastNonCommentToken.text)) {
                boolean closed = false;
                boolean escaped = false;
                boolean inCharSet = false;

                regex_body:
                do {
                  ch2 = buf[end];
                  if (JsLexer.isJsLineSeparator(ch2)) {
                    // will register as unterminated token below
                    break;
                  } else if (!escaped) {
                    switch (ch2) {
                      case '/':
                        if (!inCharSet) {
                          closed = true;
                          ++end;
                          break regex_body;
                        }
                        break;
                      case '[':
                        inCharSet = true;
                        break;
                      case ']':
                        inCharSet = false;
                        break;
                      case '\\':
                        escaped = true;
                        break;
                    }
                  } else {
                    escaped = false;
                  }
                  ++end;
                } while (end < limit);
                if (!closed) {
                  throw new ParseException(
                      new Message(MessageType.UNTERMINATED_STRING_TOKEN,
                          p.filePositionForOffsets(start, end)));
                }
                // Pick up any modifiers at the end, e.g. /foo/g
                // Firefox fails on "/foo/instanceof RegExp" with an
                // invalid identifiers error, so just pick up all letters
                while (end < limit && Character.isLetter(buf[end])) {
                  ++end;
                }

                type = JsTokenType.REGEXP;
              } else {
                end = processPunctuation(start, end);
                type = JsTokenType.PUNCTUATION;
              }
            }
            break;
          }
        }
        break;
      }
      case '.':
        // punctuation that may start a number
        if (end < limit && buf[end] >= '0' && buf[end] <= '9') {
          ParsedNumber pn = processNumber(p, start, end);
          end = pn.end;
          type = pn.type;
        } else {
          end = processPunctuation(start, end);
          type = JsTokenType.PUNCTUATION;
        }
        break;
      case '0': case '1': case '2': case '3': case '4':
      case '5': case '6': case '7': case '8': case '9':
        ParsedNumber pn = processNumber(p, start, end);
        end = pn.end;
        type = pn.type;
        break;
      default:
        if (punctuation.contains(ch)) {
          end = processPunctuation(start, end);
          type = JsTokenType.PUNCTUATION;
        } else {
          boolean isQuasi = isQuasiliteral && buf[start] == '@';
          while (end < limit) {
            char ch2 = buf[end];
            if (isQuasi && (ch2 == '*' || ch2 == '+' || ch2 == '?')) {
              ++end;
              break;
            } else if (JsLexer.isJsSpace(ch2)
                || '\'' == ch2 || '"' == ch2
                || punctuation.contains(ch2)) {
              break;
            } else {
              ++end;
            }
          }
          type = JsTokenType.WORD;
        }
        break;
    }

    FilePosition pos = p.filePositionForOffsets(start, end);
    p.consumeTo(end);
    return Token.instance(p.toString(start, end), type, pos);
  }

  static final class ParsedNumber {
    final JsTokenType type;
    final int end;
    ParsedNumber(JsTokenType type, int end) {
      this.type = type;
      this.end = end;
    }
  }
  private ParsedNumber processNumber(CharProducer p, int start, int end) {
    // This recognizes several patterns
    // 0x<hex>
    // <decimal>+("."<decimal>*)?<exponent>?
    // "."<decimal>+<exponent>?

    // Anything not obviously a number is labeled a word.
    NumberRecognizer nr = new NumberRecognizer(punctuation, p);
    for (int i = start; i < end; ++i) {
      if (!nr.recognize(i)) {
        return new ParsedNumber(nr.getTokenType(), end);
      }
    }

    int limit = p.getLimit();
    while (end < limit && nr.recognize(end)) { ++end; }
    return new ParsedNumber(nr.getTokenType(), end);
  }

  private int processPunctuation(int start, int end) {
    PunctuationTrie<?> t = this.punctuation;
    // Assumes that for every punctuation string pair (A, B) in t
    // where A is a strict prefix of B, then for every string C such that
    // A is a strict prefix of C and C is a strict prefix of B, then
    // (C is not terminal) -> (B - C) is a prefix in t.

    // This assumption is true for javascript punctuation:
    // . and ... are the only strings in t with a non-terminal (..) in-between
    // and ("..." - "..") = "." and "." is a javascript punctuation string.

    // This assumption lets me stick to the one-character lookahead assumption
    // which allows me to split pessimistically, and rejoin later.

    // There is one another assumption: that every terminal multi-character
    // punctuation string has a one character prefix that is also a
    // terminal punctuation string.
    char[] buf = p.getBuffer();
    int limit = p.getLimit();
    for (int i = start; i < end; ++i) {
      t = t.lookup(buf[i]);
    }
    assert t.isTerminal();
    while (end < limit) {
      char ch = buf[end];
      PunctuationTrie<?> t2 = t.lookup(ch);
      if (null == t2 || !t2.isTerminal()) { break; }
      ++end;
      t = t2;
    }
    return end;
  }
}
