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

final class DecodingCharProducer extends CharProducer {
  /**
   * For each character {@code buf[i]} where offset <= i < limit,
   * the number of extra characters in HTML entities preceding it.
   * Because of this definition, it is true for all i where
   * offset <= i < limit, that
   * {@code buf[i]} corresponds to the underlying sequence
   * {@code underlying[i + deltas[i]:i + deltas[i+1]]}.
   */
  private final short[] deltas;
  private final int poffset;
  private final CharProducer p;

  static DecodingCharProducer make(Decoder d, CharProducer p) {
    int plimit = p.getLimit(),
        poffset = p.getOffset(),
        pavail = plimit - poffset;
    char[] pbuf = p.getBuffer();

    short[] deltas = new short[pavail + 1];
    char[] buf = new char[pavail];
    int limit = 0;
    short lastDelta = 0;
    for (int i = poffset; i < plimit; i = d.end) {
      d.decode(pbuf, i, plimit);
      int nCodeUnits = Character.charCount(d.codePoint);

      deltas[limit] = lastDelta;
      lastDelta = (short) (deltas[limit] + d.end - i - 1);
      Character.toChars(d.codePoint, buf, limit);
      for (int j = limit + 1; j < limit + nCodeUnits; ++j) {
        deltas[j] = deltas[j - 1];
      }

      limit += nCodeUnits;
    }
    if (limit != 0) {
      for (int i = limit; i < pavail + 1; ++i) { deltas[i] = deltas[i - 1]; }
    }
    return new DecodingCharProducer(buf, limit, p, deltas, poffset);
  }

  private DecodingCharProducer(DecodingCharProducer orig) {
    super(orig.getBuffer(), orig.getLimit());
    this.deltas = orig.deltas;
    this.poffset = orig.poffset;
    this.p = orig;
    this.consume(orig.getOffset());
  }

  private DecodingCharProducer(
      char[] buf, int limit, CharProducer p, short[] deltas, int poffset) {
    super(buf, limit);
    this.p = p;
    this.deltas = deltas;
    this.poffset = poffset;
  }

  @Override
  public int getCharInFile(int offset) {
    return p.getCharInFile(getUnderlyingOffset(offset));
  }

  @Override
  public SourceBreaks getSourceBreaks(int offset) {
    return p.getSourceBreaks(getUnderlyingOffset(offset));
  }

  @Override
  public CharProducer clone() {
    return new DecodingCharProducer(this);
  }

  /** The offset in the underlying CharProducer. */
  public int getUnderlyingOffset(int offset) {
    int nAvail = deltas.length;
    return offset - poffset + deltas[offset >= nAvail ? nAvail - 1 : offset];
  }

  static abstract class Decoder {
    int codePoint;
    int end;

    abstract void decode(char[] chars, int offset, int limit);

    /**
     * Parses hex digits in {@code buf[offset:limit]}, and sets
     * the {@link #codePoint} and {@link #end} fields.
     * @param buf possibly contains hex digits in the range [offset:limit].
     * @param offset the index of the first digit in buf.
     * @param limit the index past the last digit in buf.
     * @param end the amount to add to offset to get the end if
     *     {@code buf[offset:limit]} contains only valid digits.
     * @return true iff {@code buf[offset:limit]} contains only valid digits.
     */
    boolean decodeHex(char[] buf, int offset, int limit, int end) {
      if (offset == limit) {
        return false;
      }
      int codePoint = 0;
      for (int k = offset; k < limit; ++k) {
        char ch = buf[k];
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
            return false;
        }
      }
      this.end = end;
      this.codePoint = codePoint;
      return true;
    }

    /**
     * Parses octal digits as a prefix of {@code buf[offset:limit]}, and sets
     * the {@link #codePoint} and {@link #end} fields.
     * @param buf a buffer possibly containing octal digits starting at offset.
     * @param offset the index of the first digit in buf.
     * @param limit the index past the last digit in buf.
     */
    void decodeOctal(char[] buf, int offset, int limit) {
      int codePoint = 0;
      int k;
      for (k = offset; k < limit; ++k) {
        int ch = buf[k];
        if ('0' <= ch && '7' >= ch) {
          codePoint = (codePoint << 3) | (ch - '0');
        } else {
          // end of octal literal.  leave the rest in lookahead
          break;
        }
      }
      this.codePoint = codePoint;
      this.end = k;
    }

    /**
     * Parses decimal digits in {@code buf[offset:limit]}, and sets
     * the {@link #codePoint} and {@link #end} fields.
     * @param buf possibly contains decimal digits in the range [offset:limit].
     * @param offset the index of the first digit in buf.
     * @param limit the index past the last digit in buf.
     * @param end the amount to add to offset to get the end if
     *     {@code buf[offset:limit]} contains only valid digits.
     * @return true iff {@code buf[offset:limit]} contains only valid digits.
     */
    boolean decodeDecimal(char[] buf, int offset, int limit, int end) {
      if (offset == limit) { return false; }
      int codePoint = 0;
      for (int k = offset; k < limit; ++k) {
        int ch = buf[k];
        switch (ch) {
          case '0': case '1': case '2': case '3': case '4':
          case '5': case '6': case '7': case '8': case '9':
            codePoint = (codePoint * 10) + (ch - '0');
            break;
          default:
            // Invalid unicode literal.
            return false;
        }
      }
      this.end = end;
      this.codePoint = codePoint;
      return true;
    }
  }
}
