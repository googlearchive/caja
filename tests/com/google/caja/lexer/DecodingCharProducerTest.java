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

import com.google.caja.util.CajaTestCase;

import java.io.StringReader;
import java.util.Arrays;

public class DecodingCharProducerTest extends CajaTestCase {
  private DecodingCharProducer p;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    p = DecodingCharProducer.make(
        new DecodingCharProducer.Decoder() {
          // Replaces a backslash followed by dots with the digit for the
          // number of dots.
          @Override
          void decode(char[] chars, int offset, int limit) {
            int end = offset + 1;
            if (chars[offset] != '\\') {
              this.codePoint = chars[offset];
            } else {
              while (end < limit && chars[end] == '.') { ++end; }
              int count = end - offset - 1;
              this.codePoint = (char) Math.min('0' + count, '9');
            }
            this.end = end;
          }
        },
        CharProducer.Factory.create(
            new StringReader("foo\\...bar\\..baz\\.boo\\far"),
            FilePosition.instance(is, 1, 101, 101)));
  }

  public final void testDecoding() {
    assertEquals(
        "foo3bar2baz1boo0far",
        String.valueOf(
            p.getBuffer(), p.getOffset(), p.getLimit() - p.getOffset()));
  }

  public final void testCharInFile() {
    int[] charsInFile = new int[p.getLimit() - p.getOffset() + 1];
    for (int i = 0; i < charsInFile.length; ++i) {
      charsInFile[i] = p.getCharInFile(p.getOffset() + i);
    }

    // foo\...bar\..baz\.boo\far
    //          1         2
    // 12345678901234567890123456

    // foo3   bar2  baz1 boo0far
    //               1          2
    // 1234   5678  9012 34567890

    assertEquals(
        ""
        // We created the CharProducer in setUp() with char-in-line of 101.
        + "[101, 102, 103, 104,"
        + " 108, 109, 110, 111,"
        + " 114, 115, 116, 117,"
        + " 119, 120, 121, 122, 123, 124, 125, 126]",
        Arrays.toString(charsInFile));
  }

  public final void testUnderlyingOffsets() {
    int[] underlyingOffsets = new int[p.getLimit() - p.getOffset() + 1];
    for (int i = 0; i < underlyingOffsets.length; ++i) {
      underlyingOffsets[i] = p.getUnderlyingOffset(p.getOffset() + i);
    }

    // foo\...bar\..baz\.boo\far
    //           1         2
    // 01234567890123456789012345

    // foo3   bar2  baz1 boo0far
    //                1
    // 0123   4567  8901 23456789

    assertEquals(
        ""
        + "[0, 1, 2, 3,"
        + " 7, 8, 9, 10,"
        + " 13, 14, 15, 16,"
        + " 18, 19, 20, 21, 22, 23, 24, 25]",
        Arrays.toString(underlyingOffsets));
  }
}
