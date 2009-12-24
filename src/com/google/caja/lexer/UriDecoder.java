// Copyright (C) 2009 Google Inc.
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
 * Decodes url-encoded content as specified in section 2.4 of
 * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>.
 *
 * @author mikesamuel@gmail.com
 */
final class UriDecoder extends DecodingCharProducer.Decoder {
  @Override
  public void decode(char[] chars, int offset, int limit) {
    decodeOneChar(chars, offset, limit);

    int cp = codePoint;
    int e = end;
    // Decode surrogate pairs.
    if (Character.isHighSurrogate((char) codePoint) && e < limit) {
      decodeOneChar(chars, e, limit);
      if (Character.isLowSurrogate((char) codePoint)) {
        codePoint = Character.toCodePoint((char) cp, (char) codePoint);
      } else {
        codePoint = cp;
        end = e;
      }
    }
  }

  private void decodeOneChar(char[] chars, final int offset, int limit) {
    // URI encoding is odd in that it encodes a byte sequence in a char string,
    // so we cannot use the normal hex decoding which produces a code-unit.
    // This is correct for UTF-8 encoded byte sequences which is what the
    // JS encodeURIComponent and decodeURIComponent builtins do.
    // For other encoding schemes, this may not work as the URI author intended
    // but there is no standard to determine the encoding of a URL unless that
    // URL is part of an HTTP envelope.
    int end = offset + 1;
    int codepoint = chars[offset];
    if (codepoint == '%') {
      int b = toByte(chars, offset, limit);
      if (b >= 0) {
        if ((b & 0x80) == 0) {
          codepoint = b;
          end += 2;
        } else {
          int result;
          // If it is a valid multi-byte UTF_8 sequence, treat ot as such.
          if ((b & 0xe0) == 0xc0) {
            result = fromUtf8(b & 0x1f, 1, chars, offset, limit);
            if (result >= 0) { end += 5; }
          } else if ((b & 0xf0) == 0xe0) {
            result = fromUtf8(b & 0xf, 2, chars, offset, limit);
            if (result >= 0) { end += 8; }
          } else if ((b & 0xf8) == 0xf0) {
            result = fromUtf8(b & 0x7, 3, chars, offset, limit);
            if (result >= 0) { end += 11; }
          } else {
            result = -1;
          }
          if (result >= 0) {  // A well formed UTF-8 code unit.
            codepoint = result;
          } else {  // Treat as a single ASCII character.
            codepoint = b;
            end += 2;
          }
        }
      }
    }
    this.codePoint = codepoint;
    this.end = end;
  }

  /**
   * @param offset the position of a '%' in chars.
   * @return -1 if there are not enough characters to form a 2 byte hex
   *    sequence or if either hex digit is invalid.
   *    Otherwise, a number in [0, 255].
   */
  private static int toByte(char[] chars, int offset, int limit) {
    if (offset + 3 > limit) { return -1; }
    int a = fromHex(chars[offset + 1]);
    if (a < 0) { return -1; }
    int b = fromHex(chars[offset + 2]);
    if (b < 0) { return -1; }
    return ((a << 4) | b);
  }

  /**
   * The numberic valie of a hex digit or -1 if ch is not a hex digit.
   */
  private static int fromHex(char ch) {
    if (ch > 'f' || ch < '0') { return -1; }
    if (ch <= '9') { return ch - '0'; }
    if (ch >= 'a') { return ch - ('a' - 10); }
    if (ch <= 'F' && ch >= 'A') { return ch - ('A' - 10); }
    return -1;
  }

  /**
   * The code unit value of a URI encoded UTF-8 sequence like "%C4%A3".
   * @param prefix the significant bits in the first byte of the UTF-8
   *    sequence.
   * @param nChunks the number of extra bytes in the sequence.
   * @param offset the position of the % that starts the UTF-8 sequence;
   *      the first % in the example above.
   */
  private static int fromUtf8(
      int prefix, int nChunks, char[] chars, int offset, int limit) {
    int cur = offset + 2;
    int end = cur + 1 + 3 * nChunks;
    if (end > limit) { return -1; }
    int bits = prefix;
    for (int i = 0; i < nChunks; ++i) {
      if (chars[++cur] != '%') { return -1; }
      int a = fromHex(chars[++cur]);
      if (a < 0 || ((a & 0xc) != 0x8)) { return -1; }
      int b = fromHex(chars[++cur]);
      if (b < 0) { return -1; }
      bits = (bits << 6) | (((a & 0x3) << 4) | b);
    }
    return bits;
  }
}
