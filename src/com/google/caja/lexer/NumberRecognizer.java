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

import com.google.caja.SomethingWidgyHappenedError;

/**
 * A state machine that keeps track of whether a run of word characters and
 * dots might be a part of a number.
 *
 * @author mikesamuel@gmail.com
 */
final class NumberRecognizer {

  enum State {
    START,
    ZERO,
    DOT,
    INTEGER,
    INTEGER_DOT,
    OCTAL,
    HEX_PRE,
    HEX,
    FRACTION,
    EXP_PRE,
    EXP_SIGN,
    EXPONENT,
    WORD,
    ;
  }

  private final PunctuationTrie<?> punctuation;
  private final CharProducer p;
  private State state = State.START;

  NumberRecognizer(PunctuationTrie<?> punctuation, CharProducer p) {
    this.punctuation = punctuation;
    this.p = p;
  }

  boolean recognize(int offset) {
    char ch = p.getBuffer()[offset];
    State newState = null;
    switch (state) {
      case START:
        if ('0' == ch) {
          newState = State.ZERO;
        } else if ('1' <= ch && '9' >= ch) {
          newState = State.INTEGER;
        } else if ('.' == ch) {
          newState = State.DOT;
        } else {
          throw new SomethingWidgyHappenedError();
        }
        break;
      case ZERO:
        if (ch >= '1' && ch <= '9') {
          newState = State.OCTAL;
        } else if (ch == '.') {
          newState = State.INTEGER_DOT;
        } else if (ch == '0') {
          newState = State.ZERO;
        } else if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z') {
          if (ch == 'x' || ch == 'X') {
            newState = State.HEX_PRE;
          } else if (ch == 'e' || ch == 'E') {
            newState = State.EXP_PRE;
          }
        }
        break;
      case DOT:
        if (isDecimal(ch)) {
          newState = State.FRACTION;
        } else {
          return false;
        }
        break;
      case INTEGER:
        if (isDecimal(ch)) {
          newState = State.INTEGER;
        } else if ('.' == ch) {
          newState = State.INTEGER_DOT;
        } else if (ch == 'e' || ch == 'E') {
          newState = State.EXP_PRE;
        }
        break;
      case INTEGER_DOT:
        if (isDecimal(ch)) {
          newState = State.FRACTION;
        } else if (ch == 'e' || ch == 'E') {
          newState = State.EXP_PRE;
        }
        break;
      case OCTAL:
        if (ch >= '0' && ch <= '7') {
          newState = State.OCTAL;
        }
        break;
      case HEX_PRE:
        if (isDecimal(ch)
            || ch >= 'a' && ch <= 'f'
            || ch >= 'A' && ch <= 'F') {
          newState = State.HEX;
        }
        break;
      case HEX:
        if ((isDecimal(ch))
             || (ch >= 'a' && ch <= 'f')
             || (ch >= 'A' && ch <= 'F')) {
          newState = State.HEX;
        }
        break;
      case FRACTION:
        if (isDecimal(ch)) {
          newState = State.FRACTION;
        } else if (ch == 'e' || ch == 'E') {
          newState = State.EXP_PRE;
        }
        break;
      case EXP_PRE:
        if (isDecimal(ch)) {
          newState = State.EXPONENT;
        } else if ((ch == '+' || ch == '-')
                   && offset + 1 < p.getLimit()
                   && isDecimal(p.getBuffer()[offset + 1])) {
          newState = State.EXP_SIGN;
        }
        break;
      case EXP_SIGN:
        if (isDecimal(ch)) {
          newState = State.EXPONENT;
        }
        break;
      case EXPONENT:
        if (isDecimal(ch)) {
          newState = State.EXPONENT;
        }
        break;
      case WORD:
        break;
    }
    if (newState != null) {
      this.state = newState;
      return true;
    } else if (endsToken(ch)) {
      return false;
    } else {
      this.state = State.WORD;
      return true;
    }
  }

  State getState() { return state; }

  boolean isNumber() { return State.WORD != state; }
  boolean isHex() { return State.HEX == state; }
  boolean isOctal() { return State.OCTAL == state; }

  JsTokenType getTokenType() {
    switch (state) {
      case DOT: return JsTokenType.PUNCTUATION;
      case ZERO: case INTEGER: case HEX: case OCTAL:
        return JsTokenType.INTEGER;
      case INTEGER_DOT: case FRACTION: case EXPONENT:
        return JsTokenType.FLOAT;
      default:
        return JsTokenType.WORD;
    }
  }

  private static boolean isDecimal(char ch) {
    return '0' <= ch && ch <= '9';
  }

  private boolean endsToken(char ch) {
    return ch == '"' || ch == '\'' || punctuation.contains(ch)
        || JsLexer.isJsSpace(ch);
  }
}
