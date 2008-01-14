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
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;

import java.util.LinkedList;
import java.util.regex.Pattern;

/**
 * Joins adjacent tokens that were too aggressively split by the
 * {@link InputElementSplitter}.
 *
 * @author mikesamuel@gmail.com
 */
class InputElementJoiner extends AbstractTokenStream<JsTokenType> {
  private final TokenStream<JsTokenType> tokens;
  private final PunctuationTrie punctuation;
  private final LinkedList<Token<JsTokenType>> lookahead
      = new LinkedList<Token<JsTokenType>>();

  /**
   * Should signs be included with number tokens?
   * Normally I leave signs off numbers since that makes it easier to for
   * visitors to avoid conflating the two IEEE floating point representations of
   * 0, and lint tools that need to distinguish +0 and +(0) can do that
   * by looking at FilePositions.
   */
  private static final boolean SIGNS_WITH_NUMBERS = false;

  public InputElementJoiner(
      TokenStream<JsTokenType> tokens, PunctuationTrie punctuation) {
    this.tokens = tokens;
    this.punctuation = punctuation;
  }

  @Override
  protected Token<JsTokenType> produce() throws ParseException {
    Token<JsTokenType> t;
    if (lookahead.isEmpty()) {
      if (!tokens.hasNext()) { return null; }
      t = tokens.next();
    } else {
      t = lookahead.poll();
    }
    boolean combined;
    do {
      combined = false;
      switch (t.type) {
        case PUNCTUATION:
          // join with the next token if possible
          {
            Token<JsTokenType> t2 = peek();
            if (null != t2) {
              if (JsTokenType.PUNCTUATION == t2.type) {
                if (areAdjacent(t, t2)) {
                  PunctuationTrie p =
                    punctuation.lookup(t.text).lookup(t2.text);
                  // the position past the last item in lookahead added to p
                  int n = 1;

                  while (null != p) {
                    if (p.isTerminal()) {
                      while (--n >= 0) {
                        t = combine(t, peek(), t.type);
                        lookahead.poll();
                      }
                      combined = true;
                      break;
                    } else {
                      t2 = lookaheadTo(n++);
                      if (null == t2) { break; }
                      p = punctuation.lookup(t2.text);
                    }
                  }
                }
              } else if (
                  SIGNS_WITH_NUMBERS
                  && (JsTokenType.INTEGER == t2.type
                      || JsTokenType.FLOAT == t2.type)
                      && isSign(t.text) && areAdjacent(t, t2)) {
                t = combine(t, t2, t2.type);
                combined = true;
                lookahead.poll();
              }
            }
          }
          break;
        case FLOAT:
        case INTEGER:
          // if t ends in e and lookahead is [+-] or an integer, then
          // join.
          if (EXPONENT_RE.matcher(t.text).find()) {
            Token<JsTokenType> t2 = peek();
            if (null != t2 && t2.type == JsTokenType.PUNCTUATION
                && areAdjacent(t, t2) && isSign(t2.text)) {
              t = combine(t, t2, JsTokenType.FLOAT);
              combined = true;
              lookahead.poll();
            } else {
              throw new ParseException(
                new Message(MessageType.MALFORMED_NUMBER, t.pos,
                  MessagePart.Factory.valueOf(t.text)));
            }
          // if t ends in e+ or e- and lookahead is an integer then join
          } else if (EXPONENT_SIGN_RE.matcher(t.text).find()) {
            Token<JsTokenType> t2 = peek();
            if (null != t2 && t2.type == JsTokenType.INTEGER
                && areAdjacent(t, t2) && !isHex(t2.text)) {
              t = combine(t, t2, JsTokenType.FLOAT);
              combined = true;
              lookahead.poll();
            } else {
              throw new ParseException(
                new Message(MessageType.MALFORMED_NUMBER, t.pos,
                  MessagePart.Factory.valueOf(t.text)));
            }
          }

          break;
        default: break;
      }
    } while (combined);

    return t;
  }

  private Token<JsTokenType> peek() throws ParseException {
    if (lookahead.isEmpty()) {
      if (tokens.hasNext()) {
        Token<JsTokenType> t = tokens.next();
        lookahead.addLast(t);
        return t;
      }
      return null;
    }
    return lookahead.peek();
  }

  private Token<JsTokenType> lookaheadTo(int n) throws ParseException {
    while (lookahead.size() <= n) {
      if (tokens.hasNext()) {
        Token<JsTokenType> t = tokens.next();
        lookahead.addLast(t);
      } else {
        return null;
      }
    }
    return lookahead.get(n);
  }

  private static final Pattern EXPONENT_RE = Pattern.compile("[eE]$");
  private static final Pattern EXPONENT_SIGN_RE = Pattern.compile("[eE][+-]$");

  private static boolean areAdjacent(
      Token<JsTokenType> a, Token<JsTokenType> b) {
    return a.pos.endCharInFile() == b.pos.startCharInFile()
        && a.pos.source().equals(b.pos.source());
  }

  private static boolean isSign(String s) {
    if (1 != s.length()) { return false; }
    char ch = s.charAt(0);
    return '+' == ch || '-' == ch;
  }

  private static boolean isHex(String s) {
    if (s.length() < 2) { return false; }
    char ch0 = s.charAt(0);
    char ch1 = s.charAt(1);
    return '0' == ch0 && ('x' == ch1 || 'X' == ch1);
  }

  private static Token<JsTokenType> combine(
    Token<JsTokenType> a, Token<JsTokenType> b, JsTokenType type) {
    return Token.instance(
        a.text + b.text, type, FilePosition.span(a.pos, b.pos));
  }
}
