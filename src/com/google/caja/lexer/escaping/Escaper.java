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

/**
 * A short lived object that encapsulates all the information needed to
 * escape a string onto an output buffer.
 *
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
final class Escaper {
  private final CharSequence chars;
  private final EscapeMap precomputedEscapes;
  private final SparseBitSet otherEscapes;
  private final Encoder encoder;
  private final Appendable out;
  private final int minNonPrecomputed;

  /**
   * @param precomputedEscapes maps ascii characters to escape to the escapes.
   *   Must not overlap with otherEscapes.  Escaped codepoints in precomputed
   *   greater than otherEscapes.minSetBit() will be ignored.
   */
  Escaper(CharSequence chars, EscapeMap precomputedEscapes,
          SparseBitSet otherEscapes, Encoder encoder, Appendable out) {
    this.chars = chars;
    this.precomputedEscapes = precomputedEscapes;
    this.otherEscapes = otherEscapes;
    this.minNonPrecomputed
        = otherEscapes.isEmpty() ? 128 : otherEscapes.minSetBit();
    this.encoder = encoder;
    this.out = out;
  }

  /**
   * Treats chars as plain text, and appends to out the escaped version.
   */
  void escape() throws IOException {
    int end = chars.length();
    if (end == 0) { return; }

    int pos = 0;  // The index into chars past the last char written to out.

    int codepoint = Character.codePointAt(chars, 0);
    for (int i = 0; i < end;) {
      int charCount = Character.charCount(codepoint);
      int i2 = i + charCount;
      int nextCodepoint = i2 < end ? Character.codePointAt(chars, i2) : -1;

      if (escapeOneCodepoint(pos, i, codepoint, nextCodepoint)) {
        pos = i2;
      }

      i = i2;
      codepoint = nextCodepoint;
    }
    out.append(chars, pos, end);
  }

  /**
   * Like escape, but treates the input as an already escaped string and only
   * tries to ensure that characters that might or need not be escaped for
   * correctness are consistently escaped.
   */
  void normalize() throws IOException {
    int end = chars.length();
    if (end == 0) { return; }

    int pos = 0;  // The index into chars past the last char written to out.

    int codepoint = Character.codePointAt(chars, 0);
    boolean escaped = false;  // Is the character we're considering escaped?
    for (int i = 0; i < end;) {
      int charCount = Character.charCount(codepoint);
      int i2 = i + charCount;
      int nextCodepoint = i2 < end ? Character.codePointAt(chars, i2) : -1;

      if (escaped) {
        escaped = false;
        // end pos is (i - 1) so we don't include the backslash twice.
        if (escapeOneCodepoint(pos, i - 1, codepoint, nextCodepoint)) {
          pos = i2;
        }
      } else if (codepoint == '\\') {
        escaped = true;
      } else if (escapeOneCodepoint(pos, i, codepoint, nextCodepoint)) {
        pos = i2;
      }

      i = i2;
      codepoint = nextCodepoint;
    }
    out.append(chars, pos, end);
  }

  /**
   * Escapes a single unicode codepoint onto the output buffer iff it is
   * contained by either the otherEscapes set or the ascii set.
   * @param pos the position past the last character in chars that has been
   *   written to out.
   * @param limit the position past the last character in chars that should
   *   be written preceding codepoint.
   * @param codepoint the codepoint to check.
   * @param nextCodepoint the next codepoint or -1 if none.
   * @return true iff characters between pos and limit and codepoint itself
   *   were written to out.  false iff out was not changed by this call.
   */
  private boolean escapeOneCodepoint(
      int pos, int limit, int codepoint, int nextCodepoint)
      throws IOException {
    if (codepoint < minNonPrecomputed) {
      String esc = precomputedEscapes.getEscape(codepoint);
      if (esc != null) {
        out.append(chars, pos, limit).append(esc);
        return true;
      }
    } else if (otherEscapes.contains(codepoint)) {
      out.append(chars, pos, limit);
      encoder.encode(codepoint, nextCodepoint, out);
      return true;
    }
    return false;
  }
}
