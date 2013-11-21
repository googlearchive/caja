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

package com.google.caja.parser.js;

import com.google.caja.util.CajaTestCase;

import java.util.Random;

/**
 *
 * @author mikesamuel@gmail.com
 */
@SuppressWarnings("static-method")
public class StringLiteralTest extends CajaTestCase {
  public final void testUnquotedValue() {
    assertEquals("", StringLiteral.getUnquotedValueOf(""));
    assertEquals("foo", StringLiteral.getUnquotedValueOf("foo"));
    assertEquals("foo\\bar", StringLiteral.getUnquotedValueOf("foo\\bar"));
    assertEquals("", StringLiteral.getUnquotedValueOf("''"));
    assertEquals("\"\"", StringLiteral.getUnquotedValueOf("'\"\"'"));
    assertEquals("\"\"", StringLiteral.getUnquotedValueOf("'\\\"\\\"'"));
    assertEquals("foo\bar", StringLiteral.getUnquotedValueOf("'foo\\bar'"));
    assertEquals("foo\nbar", StringLiteral.getUnquotedValueOf("'foo\\nbar'"));
    assertEquals("foobar", StringLiteral.getUnquotedValueOf("'foo\\\nbar'"));
    assertEquals("foo\\bar\\baz",
        StringLiteral.getUnquotedValueOf("'foo\\\\bar\\\\baz'"));
    assertEquals(
        "foo bar", StringLiteral.getUnquotedValueOf("'foo\\u0020bar'"));
    assertEquals("foo bar", StringLiteral.getUnquotedValueOf("'foo\\040bar'"));
    assertEquals("foo bar", StringLiteral.getUnquotedValueOf("'foo\\40bar'"));
    assertEquals("foo\0bar", StringLiteral.getUnquotedValueOf("'foo\\0bar'"));
    assertEquals("foo\0bar", StringLiteral.getUnquotedValueOf("'foo\\00bar'"));
    assertEquals("foo\0" + "0bar",
        StringLiteral.getUnquotedValueOf("'foo\\0000bar'"));
    assertEquals("foo8bar", StringLiteral.getUnquotedValueOf("'foo\\8bar'"));
    assertEquals("\n3", StringLiteral.getUnquotedValueOf("'\\0123'"));
    assertEquals(
        "\u0123" + "4", StringLiteral.getUnquotedValueOf("'\\u01234'"));
    assertEquals(
        "\u0123" + "a", StringLiteral.getUnquotedValueOf("'\\u0123a'"));
    assertEquals(
        "\u0123" + "A", StringLiteral.getUnquotedValueOf("'\\u0123A'"));
    assertEquals(
        "\u0123" + "f", StringLiteral.getUnquotedValueOf("'\\u0123f'"));
    assertEquals(
        "\u0123" + "F", StringLiteral.getUnquotedValueOf("'\\u0123F'"));
    assertEquals("'", StringLiteral.getUnquotedValueOf("'\\u0027'"));
    assertEquals("\"", StringLiteral.getUnquotedValueOf("'\\u0022'"));
    assertEquals("'", StringLiteral.getUnquotedValueOf("\"\\u0027\""));
    assertEquals("\"", StringLiteral.getUnquotedValueOf("\"\\u0022\""));
    assertEquals("@", StringLiteral.getUnquotedValueOf("'\\x40'"));
    assertEquals("x4", StringLiteral.getUnquotedValueOf("'\\x4'"));
  }

  public final void testQuoteValue() {
    assertEquals("''", StringLiteral.toQuotedValue(""));
    assertEquals("'foo'", StringLiteral.toQuotedValue("foo"));
    assertEquals("'foo\\bar'", StringLiteral.toQuotedValue("foo\bar"));
    assertEquals("'foo\\nbar'", StringLiteral.toQuotedValue("foo\nbar"));
    assertEquals(
        "'foo\\\\bar\\\\baz'", StringLiteral.toQuotedValue("foo\\bar\\baz"));
    assertEquals("'foo bar'", StringLiteral.toQuotedValue("foo bar"));
    assertEquals("'foo\\x00bar'", StringLiteral.toQuotedValue("foo\0bar"));
    assertEquals("'foo\\x7fbar'", StringLiteral.toQuotedValue("foo\u007fbar"));
    assertEquals(
        "'foo\\uabcdbar'", StringLiteral.toQuotedValue("foo\uabcdbar"));
    assertEquals("'\\'foo\\''", StringLiteral.toQuotedValue("'foo'"));
    assertEquals("'\\\"foo\\\"'", StringLiteral.toQuotedValue("\"foo\""));
    assertEquals("'\\\"foo\\\\\\\"'", StringLiteral.toQuotedValue("\"foo\\\""));
  }

  public final void testQuotingAndUnquotingAreComplements() {
    Random rnd = new Random(SEED);
    for (int i = 2000; --i >= 0;) {
      String s = makeRandomString(rnd);
      assertEquals(
          s, StringLiteral.getUnquotedValueOf(StringLiteral.toQuotedValue(s)));
    }
  }

  public final void testRandomStringsParseable() {
    Random rnd = new Random(SEED);
    for (int i = 2000; --i >= 0;) {
      String literal = makeRandomQuotedString(rnd);
      // Test that it parses.
      String value = StringLiteral.getUnquotedValueOf(literal);
      String requoted = StringLiteral.toQuotedValue(value);
      // Now make sure that our unquoting works
      assertEquals(value, StringLiteral.getUnquotedValueOf(requoted));
      // Make sure that the requoted string is coherent.
      char delimiter = requoted.charAt(0);
      assertTrue(requoted.length() >= 2);
      assertEquals(delimiter, requoted.charAt(requoted.length() - 1));
      if ("'\"".indexOf(delimiter) < 0) {
        fail("delimiter 0x" + Integer.toString(delimiter, 16));
      }
      for (int ci = 1, end = requoted.length() - 1; ci < end; ++ci) {
        char ch = requoted.charAt(ci);
        if (ch == '\\') {
          ++ci;
          assertTrue("close delimiter is escaped", ci < end - 1);
        }
        assertTrue("delimiter appears unescaped", ch != delimiter);
        if ("\r\n\u2028\u2029".indexOf(ch) >= 0) {
          fail("newline 0x" + Integer.toString(ch, 16));
        }
      }
    }
  }

  private static String makeRandomString(Random rnd) {
    int len = (int) Math.min(Math.abs(rnd.nextGaussian() * 128), 1024);
    StringBuilder sb = new StringBuilder(len);
    while (--len >= 0) {
      sb.appendCodePoint(randomCodePoint(rnd));
    }
    return sb.toString();
  }

  private static String makeRandomQuotedString(Random rnd) {
    int len = (int) Math.min(Math.abs(rnd.nextGaussian() * 128), 1024);
    int delim = rnd.nextInt(2) == 0 ? '"' : '\'';
    StringBuilder sb = new StringBuilder(len);
    sb.append(delim);
    while (--len >= 0) {
      char ch = randomChar(rnd);
      boolean escape;
      String sequence = null;
      switch (ch) {
        case '\u2028':
          escape = true;
          sequence = "\\u2028";
          break;
        case '\u2029':
          escape = true;
          sequence = "\\u2029";
          break;
        case '\r':
          escape = true;
          sequence = "\\r";
          break;
        case '\n':
          escape = true;
          sequence = "\\n";
          break;
        default:
          escape = ch == 0 || ch == delim;
          break;
      }
      if (!escape) {
        int escapeType = rnd.nextInt(16);
        switch (escapeType) {
          case 0:
            if (ch < 0377) {
              sequence = escapeSequence(ch, 8, rnd.nextInt(3));
            } else {
              sequence = "u" + escapeSequence(ch, 16, 4);
            }
            break;
          case 1:
            if (ch < 0x100) {
              sequence = escapeSequence(ch, 16, 2);
            } else {
              sequence = "u" + escapeSequence(ch, 16, 4);
            }
            break;
          case 2:
            sequence = "u" + escapeSequence(ch, 16, 4);
            break;
        }
        escape = sequence != null;
      }
      if (escape) {
        sb.append('\\');
      }
      if (sequence == null) {
        sb.append(ch);
      } else {
        sb.append(sequence);
      }
    }
    sb.append(delim);
    return sb.toString();
  }

  private static int randomCodePoint(Random rnd) {
    int mag;
    switch (rnd.nextInt(4)) {
      default:
        mag = 1 << 8;
        break;
      case 2:
        mag = 1 << 16;
        break;
      case 3:
        mag = 0x10ffff;
        break;
    }
    int codePoint = rnd.nextInt(mag);
    if (0xd800 <= codePoint && codePoint <= 0xdfff) {  // surrogates
      codePoint = codePoint & 0xff;
    }
    return codePoint;
  }

  private static char randomChar(Random rnd) {
    char ch = (char) rnd.nextInt(Character.MAX_VALUE - Character.MIN_VALUE + 1);
    if (0xd800 <= ch && ch <= 0xdfff) {  // surrogates
      ch = (char) (ch & 0xff);
    }
    return ch;
  }

  /**
   * Generates the numeric portion of an octal, hex, or unicode escape sequence.
   * @param ch the character to escape
   * @param radix 8 for octal, 16 for hex or unicode.
   * @param nDigits the minimum number of digits.  0's will be added to the left
   *   to pad the value to at least nDigits.
   */
  private static String escapeSequence(char ch, int radix, int nDigits) {
    StringBuilder sb = new StringBuilder(nDigits);
    sb.append(Integer.toString(ch, radix));
    while (sb.length() < nDigits) {
      sb.insert(0, '0');
    }
    return sb.toString();
  }
}
