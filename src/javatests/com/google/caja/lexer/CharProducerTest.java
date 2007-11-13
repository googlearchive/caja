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

import com.google.caja.util.TestUtil;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

/**
 *
 * @author mikesamuel@gmail.com
 */
public final class CharProducerTest extends TestCase {
  private static final InputSource STRING_SOURCE = new InputSource(
      URI.create("file:///CharProducerTest.java"));

  public void testFromFile() throws Exception {
    InputSource src = new InputSource(
        TestUtil.getResource(CharProducerTest.class, "testinput1.txt"));
    testProducer(
        CharProducer.Factory.create(src),
        "The quick brown fox\njumps over\nthe lazy dog\n",
      // 0         1          2         3         4
      // 01234567890123456789 01234567890 1234567890123 4
        ss(0,  FilePosition.instance(src, 1, 1, 1, 1)),
        ss(3,  FilePosition.instance(src, 1, 1, 4, 4)),
        ss(19, FilePosition.instance(src, 1, 1, 20, 20)),
        ss(20, FilePosition.instance(src, 2, 2, 21, 1)),
        ss(43, FilePosition.instance(src, 3, 3, 44, 13)),
        ss(44, FilePosition.instance(src, 4, 4, 45, 1))
        );
  }

  public void testFromString() throws Exception {
    String s =
      "but was shocked to learn\n\rthe lazy dog had\r\na fox-seeking missle.";
    // 0         1         2           3         4           5         6
    // 0123456789012345678901234 5 67890123456789012 3 4567890123456789012345

    testProducer(
        charProducerFromString(s),
        s,
        ss(0,  FilePosition.instance(STRING_SOURCE, 1, 1, 1, 1)),
        ss(24, FilePosition.instance(STRING_SOURCE, 1, 1, 25, 25)),
        ss(25, FilePosition.instance(STRING_SOURCE, 2, 2, 26, 1)),
        ss(26, FilePosition.instance(STRING_SOURCE, 3, 3, 27, 1)),
        ss(42, FilePosition.instance(STRING_SOURCE, 3, 3, 43, 17)),
        ss(43, FilePosition.instance(STRING_SOURCE, 4, 4, 44, 1)),
        ss(44, FilePosition.instance(STRING_SOURCE, 4, 4, 45, 1)),
        ss(65, FilePosition.instance(STRING_SOURCE, 4, 4, 66, 22))
    );

    testProducer(charProducerFromString(""), "");
  }

  public void testChaining() throws Exception {
    InputSource src1 = new InputSource(
        TestUtil.getResource(CharProducerTest.class, "testinput1.txt"));
    InputSource src2 = STRING_SOURCE;

    String input2 =
      "but was shocked to learn\n\rthe lazy dog had\r\na fox-seeking missle.";
    // 0         1         2           3         4           5         6
    // 0123456789012345678901234 5 67890123456789012 3 4567890123456789012345
    Reader r = new StringReader(input2);

    CharProducer prod1 = CharProducer.Factory.create(src1),
                 prod2 = CharProducer.Factory.create(r, src2);

    String golden1 = "The quick brown fox\njumps over\nthe lazy dog\n",
           golden2 = input2;
    String chainedGolden = golden1 + golden2;

    CharProducer chained = CharProducer.Factory.chain(prod1, prod2);

    testProducer(
        chained,
        chainedGolden,
        ss(0,  FilePosition.instance(src1, 1, 1, 1, 1)),
        ss(3,  FilePosition.instance(src1, 1, 1, 4, 4)),
        ss(19, FilePosition.instance(src1, 1, 1, 20, 20)),
        ss(20, FilePosition.instance(src1, 2, 2, 21, 1)),
        ss(43, FilePosition.instance(src1, 3, 3, 44, 13)),
        ss(44, FilePosition.instance(src1, 4, 4, 45, 1)),
        ss(44 + 24, FilePosition.instance(src2, 1, 1, 25, 25)),
        ss(44 + 25, FilePosition.instance(src2, 2, 2, 26, 1)),
        ss(44 + 26, FilePosition.instance(src2, 3, 3, 27, 1)),
        ss(44 + 42, FilePosition.instance(src2, 3, 3, 43, 17)),
        ss(44 + 43, FilePosition.instance(src2, 4, 4, 44, 1)),
        ss(44 + 44, FilePosition.instance(src2, 4, 4, 45, 1)),
        ss(44 + 65, FilePosition.instance(src2, 4, 4, 66, 22))
        );
  }

  public void testJsUnEscaping() throws Exception {
    String js =
      "The quick\\u0020brown fox\\njumps\\40over\\r\\nthe lazy dog\\n";
    // 0          1         2          3           4          5
    // 0123456789 012345678901234 5678901 2345678 90 12345678901234 56
    String golden =
      "The quick brown fox\njumps over\r\nthe lazy dog\n";
    // 0         1          2         3           4
    // 01234567890123456789 01234567890 1 2345678901234 5
    testProducer(
        CharProducer.Factory.fromJsString(charProducerFromString(js)),
        golden,
        ss(0, FilePosition.instance(STRING_SOURCE, 1, 1, 1, 1))
    );

    // test interrupted escapes and escapes at end of file handled gracefully
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\\\u000a")),
        "\\u000a");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\u00ziggy")),
        "u00ziggy");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\u\\u000a")),
        "u\n");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\u0\\u000a")),
        "u0\n");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\u00\\u000a")),
        "u00\n");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\u0")),
        "u0");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\u000")),
        "u000");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\u")),
        "u");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\uffff")),
        "\uffff");

    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\")),
        "");

    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\\\x0a")),
        "\\x0a");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\x0ziggy")),
        "x0ziggy");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\x\\u000a")),
        "x\n");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\x0\\u000a")),
        "x0\n");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\s0")),
        "s0");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\x")),
        "x");

    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\0")),
        "\0");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\11")),
        "\t");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\011")),
        "\t");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\009")),
        "\0009");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\09")),
        "\0009");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\9")),
        "9");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\00")),
        "\0");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\000")),
        "\0");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\0000")),
        "\u0000" + "0");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\37")),
        "\037");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\037")),
        "\037");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\040")),
        " ");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\40")),
        " ");
    testProducer(
        CharProducer.Factory.fromJsString(
        charProducerFromString("\\400")),
        " 0");
    testProducer(
        CharProducer.Factory.fromJsString(
            charProducerFromString("\\380")),
            "\003" + "80");

    // test the special escapes
    testProducer(
        CharProducer.Factory.fromJsString(
            charProducerFromString("\\n")),
            "\n");
    testProducer(
        CharProducer.Factory.fromJsString(
            charProducerFromString("\\r")),
            "\r");
    testProducer(
        CharProducer.Factory.fromJsString(
            charProducerFromString("\\r\\n")),
            "\r\n");
    testProducer(
        CharProducer.Factory.fromJsString(
            charProducerFromString("\\t")),
            "\t");
    testProducer(
        CharProducer.Factory.fromJsString(
            charProducerFromString("\\b")),
            "\b");
    testProducer(
        CharProducer.Factory.fromJsString(
            charProducerFromString("\\f")),
            "\f");
    testProducer(
        CharProducer.Factory.fromJsString(
            charProducerFromString("\\v")),
            "\013");
  }

  private static CharProducer charProducerFromString(String js) {
    Reader r = new StringReader(js);
    return CharProducer.Factory.create(r, STRING_SOURCE);
  }

  public void testHtmlUnEscaping() throws Exception {
    String js =
      "The quick&nbsp;brown fox&#xa;jumps over&#xd;&#10;the lazy dog&#x000a;";
    // 0          1         2          3           4          5
    // 0123456789 012345678901234 5678901 2345678 90 12345678901234 56
    String golden =
      "The quick\u00a0brown fox\njumps over\r\nthe lazy dog\n";
    // 0         1          2         3           4
    // 01234567890123456789 01234567890 1 2345678901234 5
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(charProducerFromString(js)),
        golden,
        ss(0, FilePosition.instance(STRING_SOURCE, 1, 1, 1, 1))
    );

    // test interrupted escapes and escapes at end of file handled gracefully
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("\\\\u000a")),
        "\\\\u000a");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#x000a;")),
        "\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#x00a;")),
        "\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#x0a;")),
        "\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#xa;")),
        "\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#xa")),
        "&#xa");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#x00ziggy")),
        "&#x00ziggy");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#xa00z;")),
        "&#xa00z;");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#&#x000a;")),
        "&#\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#x&#x000a;")),
        "&#x\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#xa&#x000a;")),
        "&#xa\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#&#xa;")),
        "&#\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#x")),
        "&#x");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#x0")),
        "&#x0");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#")),
        "&#");

    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("\\")),
        "\\");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&")),
        "&");

    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#000a;")),
        "&#000a;");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#10;")),
        "\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#010;")),
        "\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#0010;")),
        "\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#9;")),
        "\t");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#10")),
        "&#10");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#00ziggy")),
        "&#00ziggy");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#&#010;")),
        "&#\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#0&#010;")),
        "&#0\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#01&#10;")),
        "&#01\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#&#10;")),
        "&#\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#1")),
        "&#1");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
        charProducerFromString("&#10")),
        "&#10");

    // test the named escapes
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
            charProducerFromString("&lt;")),
            "<");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
            charProducerFromString("&gt;")),
            ">");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
            charProducerFromString("&quot;")),
            "\"");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
            charProducerFromString("&apos;")),
            "'");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
            charProducerFromString("&amp;")),
            "&");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
            charProducerFromString("&amp;lt;")),
            "&lt;");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
            charProducerFromString("&AMP;")),
            "&");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
            charProducerFromString("&AMP")),
            "&AMP");


    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
            charProducerFromString("&;")),
            "&;");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(
            charProducerFromString("&bogus;")),
            "&bogus;");
  }

  public void testChainingAndUnescaping() throws Exception {
    InputSource src = STRING_SOURCE;

    //                            12345678
    Reader r1 = new StringReader("hiThere(");
    //                            3333333
    //                            0123456
    Reader r2 = new StringReader(", now);");

    CharProducer prod1 = CharProducer.Factory.create(
        r1, FilePosition.instance(src, 1, 1, 1, 1)),
                 prod2 = CharProducer.Factory.create(
        r2, FilePosition.instance(src, 2, 2, 30, 1));

    String golden = "hiThere(, now);";

    CharProducer chained = CharProducer.Factory.fromHtmlAttribute(
        CharProducer.Factory.chain(prod1, prod2));

    testProducer(
        chained,
        golden,
        ss(0, FilePosition.instance(src, 1, 1, 1, 1)),
        ss(1, FilePosition.instance(src, 1, 1, 2, 2)),
        ss(2, FilePosition.instance(src, 1, 1, 3, 3)),
        ss(3, FilePosition.instance(src, 1, 1, 4, 4)),
        ss(4, FilePosition.instance(src, 1, 1, 5, 5)),
        ss(5, FilePosition.instance(src, 1, 1, 6, 6)),
        ss(6, FilePosition.instance(src, 1, 1, 7, 7)),
        ss(7, FilePosition.instance(src, 1, 1, 8, 8)),
        ss(8, FilePosition.instance(src, 1, 1, 9, 9)),
        ss(9, FilePosition.instance(src, 2, 2, 31, 2)),
        ss(10, FilePosition.instance(src, 2, 2, 32, 3)),
        ss(11, FilePosition.instance(src, 2, 2, 33, 4)),
        ss(12, FilePosition.instance(src, 2, 2, 34, 5)),
        ss(13, FilePosition.instance(src, 2, 2, 35, 6)),
        ss(14, FilePosition.instance(src, 2, 2, 36, 7)),
        ss(15, FilePosition.instance(src, 2, 2, 37, 8))
        );
  }

  /**
   * Asserts that the given producer produces the characters in golden, and that
   * the character at positions[k].charsRead falls at
   * FilePosition positions[k].pos.
   *
   * @param positions s.t. positions[k+1].charsRead > positions[k].charsRead.
   */
  private static void testProducer(
      CharProducer p, String golden, StreamState... positions)
      throws IOException {
    try {
      int k = 0;
      StringBuilder sb = new StringBuilder();
      while (true) {
        if (k < positions.length && sb.length() == positions[k].charsRead) {
          FilePosition pos = p.getCurrentPosition();
          assertEquals(
              "Read so far [" + sb + "] : ["
              + golden.substring(0, Math.min(sb.length(), golden.length()))
              + "]",
              positions[k++].pos, pos);
        }
        int ch = p.read();
        if (ch < 0) { break; }
        sb.append((char) ch);
      }
      String actual = sb.toString();
      assertEquals(
          "golden:[" + escape(golden) + "]\nactual:[" + escape(actual) + "]",
          golden, actual);
      assertEquals(positions.length, k);
    } finally {
      p.close();
    }
  }

  private static final Pattern ESCAPED =
    Pattern.compile("[^\\p{javaLetterOrDigit} \\.\\-\\:\\;\\'\\\",/\\?&\\#]");
  private static String escape(String s) {
    Matcher m = ESCAPED.matcher(s);
    if (!m.find()) { return s; }
    StringBuffer sb = new StringBuffer(s.length() + 16);
    char[] hex = new char[] { 0, 0, 0, 0 };
    do {
      int ch = m.group().charAt(0);
      for (int i = hex.length; --i >= 0;) {
        hex[i] = "0123456789abcdef".charAt(ch & 0xf);
        ch >>= 4;
      }
      m.appendReplacement(sb, "<" + new String(hex, 0, 4) + ">");
    } while (m.find());
    return m.appendTail(sb).toString();
  }

  private static StreamState ss(int charsRead, FilePosition p) {
    return new StreamState(charsRead, p);
  }

  static class StreamState {
    final int charsRead;
    final FilePosition pos;

    StreamState(int charsRead, FilePosition pos) {
      this.charsRead = charsRead;
      this.pos = pos;
    }
  }
}
