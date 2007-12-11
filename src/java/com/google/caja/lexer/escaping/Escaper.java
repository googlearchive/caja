// Copyright (C) 2007 Google Inc.
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

package com.google.caja.lexer.escaping;

import com.google.caja.util.SparseBitSet;
import java.io.IOException;
import java.util.Arrays;

/**
 * A short lived object that encapsulates all the information needed to
 * escape a string onto an output buffer.
 *
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
final class Escaper {
  private final CharSequence chars;
  private final EscapeMap ascii;
  private final SparseBitSet nonAscii;
  private final Appendable out;

  Escaper(CharSequence chars, EscapeMap ascii, SparseBitSet nonAscii,
          Appendable out) {
    this.chars = chars;
    this.ascii = ascii;
    this.nonAscii = nonAscii;
    this.out = out;
  }

  /**
   * Treats chars as plain text, and appends to out the escaped version.
   */
  void escape() throws IOException {
    int pos = 0;
    int end = chars.length();
    int charCount;
    for (int i = 0; i < end; i += charCount) {
      int codepoint = Character.codePointAt(chars, i);
      charCount = Character.charCount(codepoint);
      if (escapeOneCodepoint(pos, i, codepoint)) { pos = i + charCount; }
    }
    out.append(chars, pos, end);
  }

  /**
   * Like escape, but treates the input as an already escaped string and only
   * tries to ensure that characters that might or need not be escaped for
   * correctness are consistently escaped.
   */
  void normalize() throws IOException {
    int pos = 0;
    int end = chars.length();
    int charCount;
    for (int i = 0; i < end; i += charCount) {
      int codepoint = Character.codePointAt(chars, i);
      charCount = Character.charCount(codepoint);

      if (codepoint == '\\') {
        // Already escaped.
        int escStart = i;
        i += charCount;
        if (i < end) {
          codepoint = Character.codePointAt(chars, i);
          charCount = Character.charCount(codepoint);
        }
        // Try to escape it anyway.  Since we pass escStart in as the second
        // arg instead of i, the output will not include the leading backslash
        // if it does decide to re-escape.
        if (escapeOneCodepoint(pos, escStart, codepoint)) {
          pos = i + charCount;
        }
        // Otherwise don't unescape.  Maybe it has a special significance in
        // the current context, like ? in regexps.
      } else {
        if (escapeOneCodepoint(pos, i, codepoint)) { pos = i + charCount; }
      }
    }
    out.append(chars, pos, end);
  }

  /**
   * Escapes a single unicode codepoint onto the output buffer iff it is
   * contained by either the nonAscii set or the ascii set.
   * @param pos the position past the last character in chars that has been
   *   written to out.
   * @param limit the position past the last character in chars that should
   *   be written preceding codepoint.
   * @param codepoint the codepoint to check.
   * @return true iff characters between pos and limit and codepoint itself
   *   were written to out.  false iff out was not changed by this call.
   */
  private boolean escapeOneCodepoint(int pos, int limit, int codepoint)
      throws IOException {
    if (codepoint < 0x7f) {
      String esc = ascii.getEscape(codepoint);
      if (esc != null) {
        out.append(chars, pos, limit).append(esc);
        return true;
      }
    } else if (nonAscii.contains(codepoint)) {
      out.append(chars, pos, limit);
      if (!Character.isSupplementaryCodePoint(codepoint)) {
        if (codepoint < 0x100) {
          octalEscape((char) codepoint, out);
        } else {
          unicodeEscape((char) codepoint, out);
        }
      } else {
        for (char surrogate : Character.toChars(codepoint)) {
          unicodeEscape(surrogate, out);
        }
      }
      return true;
    }
    return false;
  }

  static void octalEscape(char ch, Appendable out) throws IOException {
    out.append('\\').append((char) ('0' + ((ch & 0x1c0) >> 6)))
        .append((char) ('0' + ((ch & 0x38) >> 3)))
        .append((char) ('0' + (ch & 0x7)));
  }

  static void unicodeEscape(char ch, Appendable out) throws IOException {
    out.append("\\u").append("0123456789abcdef".charAt((ch >> 12) & 0xf))
        .append("0123456789abcdef".charAt((ch >> 8) & 0xf))
        .append("0123456789abcdef".charAt((ch >> 4) & 0xf))
        .append("0123456789abcdef".charAt(ch & 0xf));
  }
}
