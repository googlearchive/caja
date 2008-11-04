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

/**
 * Splits lines into strings, comments, regular expression literals, and
 * blocks of non-whitespace.
 *
 * @author mikesamuel@gmail.com
 */
final class InputElementSplitter extends AbstractTokenStream<JsTokenType> {
  private final LineContinuingCharProducer p;
  /**
   * A trie used to split a chunk of text into punctuation tokens and
   * non-punctuation tokens.
   */
  private final PunctuationTrie punctuation;
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

  public InputElementSplitter(CharProducer p, PunctuationTrie punctuation) {
    this(p, punctuation, false);
  }

  public InputElementSplitter(
      CharProducer p, PunctuationTrie punctuation, boolean isQuasiliteral) {
    this.p = new LineContinuingCharProducer(p);
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
    StringBuilder text;
    JsTokenType type;
    FilePosition start;

    try {
      int ch;
      while ((ch = p.read()) >= 0 && JsLexer.isJsSpace((char) ch)) {
        // Issue a token break so that we can correctly compute semicolon
        // insertion rules later.
        if (p.tokenBreak()) {
          return Token.instance(
              "\\", JsTokenType.LINE_CONTINUATION, p.getCurrentPosition());
        }
      }
      if (ch < 0) { return null; }

      start = p.getLastPosition();
      text = new StringBuilder();
      text.append((char) ch);
      switch (ch) {
        case '"': case '\'':
        {
          boolean closed = false;
          boolean escaped = false;
          for (int ch2; (ch2 = p.read()) >= 0;) {
            text.append((char) ch2);
            if (ch2 == ch && !escaped) {
              closed = true;
              break;
            } else if (JsLexer.isJsLineSeparator((char) ch2)) {
              // will register as an unterminated string token below
              break;
            }
            escaped = !escaped && ch2 == '\\';
          }
          if (!closed) {
            throw new ParseException(
                new Message(
                    MessageType.UNTERMINATED_STRING_TOKEN,
                    FilePosition.span(start, p.getCurrentPosition())));
          }
          type = JsTokenType.STRING;
          break;
        }
        case '/':
        {
          int ch2 = p.read();
          if (ch2 < 0) {
            type = JsTokenType.PUNCTUATION;
          } else {
            switch (ch2) {
            case '/':
              do {
                text.append((char) ch2);
              } while ((ch2 = p.read()) >= 0 &&
                       !JsLexer.isJsLineSeparator((char) ch2));
              if (ch2 >= 0) { p.pushback(ch2); }
              type = JsTokenType.COMMENT;
              break;
            case '*':
              {
                text.append((char) ch2);
                boolean star = false;
                boolean closed = false;
                while ((ch2 = p.read()) >= 0) {
                  text.append((char) ch2);
                  if (star && '/' == ch2) {
                    closed = true;
                    break;
                  } else {
                    star = (ch2 == '*') && !p.tokenBreak();
                  }
                }
                if (!closed) {
                  throw new ParseException(
                      new Message(MessageType.UNTERMINATED_STRING_TOKEN,
                          FilePosition.span(start, p.getCurrentPosition())));
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
                    text.append((char) ch2);
                    if (JsLexer.isJsLineSeparator((char) ch2)) {
                      // will register as unterminated token below
                      break;
                    } else if (!escaped) {
                      switch (ch2) {
                        case '/':
                          if (!inCharSet) {
                            closed = true;
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
                  } while ((ch2 = p.read()) >= 0);
                  if (!closed) {
                    throw new ParseException(
                        new Message(MessageType.UNTERMINATED_STRING_TOKEN,
                            FilePosition.span(start, p.getCurrentPosition())));
                  }
                  // Pick up any modifiers at the end, e.g. /foo/g
                  // Firefox fails on "/foo/instanceof RegExp" with an
                  // invalid identifiers error, so just pick up all letters
                  while ((ch2 = p.read()) >= 0) {
                    if (!Character.isLetter(ch2)) {
                      p.pushback(ch2);
                      break;
                    }
                    text.append((char) ch2);
                  }

                  type = JsTokenType.REGEXP;
                } else {
                  p.pushback((char) ch2);
                  processPunctuation(text);
                  type = JsTokenType.PUNCTUATION;
                }
              }
              break;
            }
          }
          break;
        }
        case '.':
          {
            // punctuation that may start a number
            int ch2 = p.read();
            if (ch2 >= '0' && ch2 <= '9') {
              text.append((char) ch2);
              type = processNumber(text);
            } else {
              p.pushback(ch2);
              processPunctuation(text);
              type = JsTokenType.PUNCTUATION;
            }
          }
          break;
        case '0': case '1': case '2': case '3': case '4':
        case '5': case '6': case '7': case '8': case '9':
          type = processNumber(text);
          break;
        default:
          if (punctuation.contains((char) ch)) {
            processPunctuation(text);
            type = JsTokenType.PUNCTUATION;
          } else {
            for (int ch2; (ch2 = p.read()) >= 0;) {
              if (isQuasiliteral && text.length() > 0 && text.charAt(0) == '@'
                  && (ch2 == '*' || ch2 == '+' || ch2 == '?')) {
                text.append((char) ch2);
              } else  if (JsLexer.isJsSpace((char) ch2) || p.tokenBreak()
                          || '\'' == ch2 || '"' == ch2
                          || punctuation.contains((char) ch2)) {
                p.pushback(ch2);
                break;
              } else {
                text.append((char) ch2);
              }
            }
            type = JsTokenType.WORD;
          }
          break;
      }
    } catch (IOException ex) {
      throw new ParseException(
          new Message(MessageType.PARSE_ERROR, p.getCurrentPosition()), ex);
    }

    FilePosition pos = FilePosition.span(start, p.getCurrentPosition());
    p.advanceLast();
    return Token.instance(text.toString(), type, pos);
  }

  private JsTokenType processNumber(StringBuilder sb) throws IOException {
    // This recognizes several patterns
    // 0x<hex><modifiers>
    // <decimal>+("."<decimal>*)?<exponent>?<modifiers>
    // "."<decimal>+<exponent>?

    // It does *not* attempt to distinguish octal literals from decimal at
    // this stage and does not group signs with numbers or mantissas with
    // exponents containing a sign.
    // Both those groupings are done by the joiner to preserve the one
    // character lookahead rule.

    // Anything not obviously a number is labelled a word.
    NumberRecognizer nr = new NumberRecognizer();
    for (int i = 0, n = sb.length(); i < n; ++i) {
      if (!nr.consume(sb.charAt(i))) {
        return nr.getTokenType();
      }
    }

    for (int chi; (chi = p.read()) >= 0;) {
      char ch = (char) chi;
      if ('"' == ch || '\'' == ch || ('.' != ch && punctuation.contains(ch))
          || JsLexer.isJsSpace(ch) || p.tokenBreak() || !nr.consume(ch)) {
        p.pushback(ch);
        break;
      }

      sb.append(ch);
    }
    return nr.getTokenType();
  }

  private void processPunctuation(StringBuilder sb)
      throws IOException {
    PunctuationTrie t = this.punctuation;
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
    t = t.lookup(sb);
    assert t.isTerminal();
    for (int ch; (ch = p.read()) >= 0;) {
      PunctuationTrie t2 = t.lookup((char) ch);
      if (null == t2 || !t2.isTerminal() || p.tokenBreak()) {
        p.pushback(ch);
        break;
      }
      t = t2;
      sb.append((char) ch);
    }
  }
}
