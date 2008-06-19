// Copyright (C) 2008 Google Inc.
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
 * Utility functions for decoding numeric escapes, such as HTML numeric
 * entities, and octal, hex, and unicode escapes in C-like languages.
 *
 * @author mikesamuel@gmail.com
 */
final class NumericEscapes {
  /**
   * Consume n characters from cp and return a codepoint, or return fallback
   * if the characters [start:end-1] are not all valid hex without consuming
   * anything.
   * @param cp a producer with a limit >= max(end, nToConsume).
   * @param start the index of the first digit in cp's lookahead buffer.
   * @param end the index past the last digit in cp's lookahead buffer.
   * @param nToConsume the number of characters to consume from cp's lookahead
   *    buffer if [start:end] contains only valid digits.
   */
  static int unescapeHex(LookaheadCharProducer cp, int start, int end,
                         int nToConsume, int fallback) {
    if (start == end) { return fallback; }
    int codePoint = 0;
    for (int k = start; k < end; ++k) {
      int ch = cp.peek(k);
      switch (ch) {
        case '0': case '1': case '2': case '3': case '4':
        case '5': case '6': case '7': case '8': case '9':
          codePoint = (codePoint << 4) | (ch - '0');
          break;
        case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
          codePoint = (codePoint << 4) | (ch - 'a' + 10);
          break;
        case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
          codePoint = (codePoint << 4) | (ch - 'A' + 10);
          break;
        default:
          // invalid unicode literal.  leave the rest in lookahead
          return fallback;
      }
    }
    cp.consume(nToConsume);
    return codePoint;
  }

  /**
   * Consume up to n octal digits from cp and return a codepoint.
   * @param cp a producer with a limit >= n.
   * @param n the number of characters in cp's lookahead buffer to consider.
   */
  static int unescapeOctal(LookaheadCharProducer cp, int n) {
    int codePoint = 0;
    for (int k = 0; k < n; ++k) {
      int ch = cp.peek(k);
      if ('0' <= ch && '7' >= ch) {
        codePoint = (codePoint << 3) | (ch - '0');
      } else {
        // end of octal literal.  leave the rest in lookahead
        n = k;
        break;
      }
    }
    cp.consume(n);  // consume characters from lookahead
    return codePoint;
  }

  /**
   * Consume n characters from cp and return a codepoint, or return fallback
   * if the characters [start:end-1] are not all valid decimal without consuming
   * anything.
   * @param cp a producer with a limit >= max(end, nToConsume).
   * @param start the index of the first digit in cp's lookahead buffer.
   * @param end the index past the last digit in cp's lookahead buffer.
   * @param nToConsume the number of characters to consume from cp's lookahead
   *    buffer if [start:end] contains only valid digits.
   */
  static int unescapeDecimal(LookaheadCharProducer cp, int start, int end,
                             int nToConsume, int fallback) {
    if (start == end) { return fallback; }
    int codePoint = 0;
    for (int k = start; k < end; ++k) {
      int ch = cp.peek(k);
      switch (ch) {
        case '0': case '1': case '2': case '3': case '4':
        case '5': case '6': case '7': case '8': case '9':
          codePoint = (codePoint * 10) + (ch - '0');
          break;
        default:
          // invalid unicode literal.  leave the rest in lookahead
          return fallback;
      }
    }
    cp.consume(nToConsume);
    return codePoint;
  }
}
