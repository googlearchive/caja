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
    EXPONENT,
    WORD,
    ;
  }

  private State state = State.START;
  private boolean hasExponent;
  private boolean isDecimal;
  private boolean isHex;

  boolean consume(char ch) {
    if ('.' == ch && state.compareTo(State.INTEGER) > 0) { return false; }
    switch (state) {
      case START:
        if ('0' == ch) {
          state = State.ZERO;
        } else if ('1' <= ch && '9' >= ch) {
          state = State.INTEGER;
        } else if ('.' == ch) {
          state = State.DOT;
        } else {
          throw new AssertionError();
        }
        break;
      case ZERO:
        if (ch >= '1' && ch <= '9') {
          state = State.OCTAL;
        } else if (ch == '.') {
          state = State.INTEGER_DOT;
          isDecimal = true;
        } else if (ch == '0') {
          // pass
        } else if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z') {
          if (ch == 'x' || ch == 'X') {
            state = State.HEX_PRE;
          } else if (ch == 'e' || ch == 'E') {
            isDecimal = true;
            state = State.EXP_PRE;
          } else {
            state = State.WORD;
          }
        } else {
          state = State.WORD;
        }
        break;
      case DOT:
        if (ch >= '0' && ch <= '9') {
          isDecimal = true;
          state = State.FRACTION;
        } else {
          return false;
        }
        break;
      case INTEGER:
        if (ch >= '0' && ch <= '9') {
          // pass
        } else if ('.' == ch) {
          state = State.INTEGER_DOT;
          isDecimal = true;
        } else if (ch == 'e' || ch == 'E') {
          state = State.EXP_PRE;
          isDecimal = true;
        } else {
          state = State.WORD;
        }
        break;
      case INTEGER_DOT:
        if (ch >= '0' && ch <= '9') {
          state = State.FRACTION;
        } else if (ch == 'e' || ch == 'E') {
          state = State.EXP_PRE;
        } else {
          state = State.WORD;
        }
        break;
      case OCTAL:
        if (ch >= '0' && ch <= '7') {
          // pass
        } else {
          state = State.WORD;
        }
        break;
      case HEX_PRE:
        if (ch >= '0' && ch <= '9'
            || ch >= 'a' && ch <= 'f'
            || ch >= 'A' && ch <= 'F') {
          state = State.HEX;
          this.isHex = true;
        } else {
          state = State.WORD;
        }
        break;
      case HEX:
        if (!((ch >= '0' && ch <= '9')
              || (ch >= 'a' && ch <= 'f')
              || (ch >= 'A' && ch <= 'F'))) {
          state = State.WORD;
        }
        break;
      case FRACTION:
        if (ch >= '0' && ch <= '9') {
          // pass
        } else if (ch == 'e' || ch == 'E') {
          state = State.EXP_PRE;
        } else {
          state = State.WORD;
        }
        break;
      case EXP_PRE:
        if (ch >= '0' && ch <= '9') {
          state = State.EXPONENT;
        } else {
          state = State.WORD;
        }
        break;
      case EXPONENT:
        if (ch >= '0' && ch <= '9') {
          // pass
        } else {
          state = State.WORD;
        }
        break;
      case WORD:
        // pass
        break;
    }
    return true;
  }

  State getState() { return state; }

  boolean isNumber() { return State.WORD != state; }
  boolean isDecimal() { return State.WORD != state && this.isDecimal; }
  boolean isHex() { return State.WORD != state && this.isHex; }
  boolean isOctal() { return State.OCTAL == state; }
  boolean hasExponent() { return State.WORD != state && this.hasExponent; }

  JsTokenType getTokenType() {
    switch (state) {
      case ZERO: case INTEGER: case HEX: case OCTAL:
        return JsTokenType.INTEGER;
      case INTEGER_DOT: case FRACTION: case EXPONENT:
      case EXP_PRE:
        return JsTokenType.FLOAT;
      default:
        return JsTokenType.WORD;
    }
  }
}
