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

import com.google.caja.lexer.escaping.UriUtil;
import com.google.caja.util.Pair;
import com.google.caja.util.TestUtil;

import java.io.Reader;
import java.io.StringReader;
import java.net.URI;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

/**
 *
 * @author mikesamuel@gmail.com
 */
@SuppressWarnings("static-method")
public final class CharProducerTest extends TestCase {
  private static final InputSource STRING_SOURCE = new InputSource(
      URI.create("file:///CharProducerTest.java"));

  public final void testFromReader() throws Exception {
    InputSource src = new InputSource(
        TestUtil.getResource(CharProducerTest.class, "testinput1.txt"));
    testProducer(
        CharProducer.Factory.create(
            new StringReader(
                TestUtil.readResource(CharProducerTest.class, "testinput1.txt")
                ),
            src),
        "The quick brown fox\njumps over\nthe lazy dog\n",
      // 0         1          2         3         4
      // 01234567890123456789 01234567890 1234567890123 4
        ss(0,  "testinput1.txt:1+1@1"),
        ss(3,  "testinput1.txt:1+4@4"),
        ss(19, "testinput1.txt:1+20@20"),
        ss(20, "testinput1.txt:2+1@21"),
        ss(43, "testinput1.txt:3+13@44"),
        ss(44, "testinput1.txt:4+1@45")
        );
  }

  public final void testFromString() {
    String s =
      "but was shocked to learn\n\rthe lazy dog had\r\na fox-seeking missle.";
    // 0         1         2           3         4           5         6
    // 0123456789012345678901234 5 67890123456789012 3 4567890123456789012345

    testProducer(
        fromString(s),
        s,
        ss(0,  "CharProducerTest.java:1+1@1"),
        ss(24, "CharProducerTest.java:1+25@25"),
        ss(25, "CharProducerTest.java:2+1@26"),
        ss(26, "CharProducerTest.java:3+1@27"),
        ss(42, "CharProducerTest.java:3+17@43"),
        ss(43, "CharProducerTest.java:3+18@44"),
        ss(44, "CharProducerTest.java:4+1@45"),
        ss(65, "CharProducerTest.java:4+22@66")
    );

    testProducer(fromString(""), "");
  }

  public final void testChaining() throws Exception {
    String input2 =
      "but was shocked to learn\n\rthe lazy dog had\r\na fox-seeking missle.";
    // 0         1         2           3         4           5         6
    // 0123456789012345678901234 5 67890123456789012 3 4567890123456789012345
    Reader r = new StringReader(input2);

    CharProducer prod1 = TestUtil.getResourceAsProducer(
        CharProducerTest.class, "testinput1.txt");
    CharProducer prod2 = CharProducer.Factory.create(r, STRING_SOURCE);

    String golden1 = "The quick brown fox\njumps over\nthe lazy dog\n",
           golden2 = input2;
    String chainedGolden = golden1 + golden2;

    CharProducer chained = CharProducer.Factory.chain(prod1, prod2);

    testProducer(
        chained,
        chainedGolden,
        ss(0,  "testinput1.txt:1+1@1"),
        ss(3,  "testinput1.txt:1+4@4"),
        ss(19, "testinput1.txt:1+20@20"),
        ss(20, "testinput1.txt:2+1@21"),
        ss(43, "testinput1.txt:3+13@44"),
        ss(44, "testinput1.txt:4+1@45"),
        ss(44 + 24, "CharProducerTest.java:1+25@25"),
        ss(44 + 25, "CharProducerTest.java:2+1@26"),
        ss(44 + 26, "CharProducerTest.java:3+1@27"),
        ss(44 + 42, "CharProducerTest.java:3+17@43"),
        ss(44 + 43, "CharProducerTest.java:3+18@44"),
        ss(44 + 44, "CharProducerTest.java:4+1@45"),
        ss(44 + 65, "CharProducerTest.java:4+22@66")
        );
  }

  public final void testJsUnEscaping() {
    String js =
      "The quick\\u0020brown fox\\njumps\\40over\\r\\nthe lazy dog\\n";
    // 0          1         2          3           4          5
    // 0123456789 012345678901234 5678901 2345678 90 12345678901234 56
    String golden =
      "The quick brown fox\njumps over\r\nthe lazy dog\n";
    // 0         1          2         3           4
    // 01234567890123456789 01234567890 1 2345678901234 5
    testProducer(
        CharProducer.Factory.fromJsString(fromString(js)),
        golden,
        ss(0, "CharProducerTest.java:1+1@1")
    );

    // test interrupted escapes and escapes at end of file handled gracefully
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\\\u000a")),
        "\\u000a");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\u00ziggy")),
        "u00ziggy");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\u\\u000a")),
        "u\n");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\u0\\u000a")),
        "u0\n");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\u00\\u000a")),
        "u00\n");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\u0")),
        "u0");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\u000")),
        "u000");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\u")),
        "u");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\uffff")),
        "\uffff");

    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\")),
        "\\");

    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\\\x0a")),
        "\\x0a");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\x0ziggy")),
        "x0ziggy");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\x\\u000a")),
        "x\n");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\x0\\u000a")),
        "x0\n");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\s0")),
        "s0");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\x")),
        "x");

    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\0")),
        "\0");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\11")),
        "\t");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\011")),
        "\t");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\009")),
        "\0009");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\09")),
        "\0009");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\9")),
        "9");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\00")),
        "\0");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\000")),
        "\0");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\0000")),
        "\u0000" + "0");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\37")),
        "\037");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\037")),
        "\037");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\040")),
        " ");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\40")),
        " ");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\400")),
        " 0");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\380")),
        "\003" + "80");

    // test the special escapes
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\n")),
        "\n");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\r")),
        "\r");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\r\\n")),
        "\r\n");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\t")),
        "\t");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\b")),
        "\b");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\f")),
        "\f");
    testProducer(
        CharProducer.Factory.fromJsString(fromString("\\v")),
        "\013");
  }

  private static CharProducer fromString(String js) {
    return CharProducer.Factory.create(new StringReader(js), STRING_SOURCE);
  }

  public final void testHtmlUnEscaping() {
    String html =
      "The quick&nbsp;brown fox&#xa;jumps over&#xd;&#10;the lazy dog&#x000a;";
    //          1         2         3         4         5         6
    // 123456789012345678901234567890123456789012345678901234567890123456789
    String golden =
      "The quick\u00a0brown fox\njumps over\r\nthe lazy dog\n";
    // 0              1          2         3           4
    // 0123456789     0123456789 01234567890 1 234567890123
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString(html)),
        golden,
        ss(0, "CharProducerTest.java:1+1@1"),
        ss(10, "CharProducerTest.java:1+16@16"),  // The 'b' in "brown"
        ss(20, "CharProducerTest.java:1+30@30"),  // The 'j' in "jumps"
        ss(30, "CharProducerTest.java:1+40@40"),  // The CR before "the lazy"
        ss(40, "CharProducerTest.java:1+58@58")   // The space before "dog"
    );

    // test interrupted escapes and escapes at end of file handled gracefully
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("\\\\u000a")),
        "\\\\u000a");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#x000a;")),
        "\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#x00a;")),
        "\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#x0a;")),
        "\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#xa;")),
        "\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#x10000;")),
        String.valueOf(Character.toChars(0x10000)));
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#xa")),
        "&#xa");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#x00ziggy")),
        "&#x00ziggy");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#xa00z;")),
        "&#xa00z;");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#&#x000a;")),
        "&#\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#x&#x000a;")),
        "&#x\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#xa&#x000a;")),
        "&#xa\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#&#xa;")),
        "&#\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#x")),
        "&#x");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#x0")),
        "&#x0");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#")),
        "&#");

    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("\\")),
        "\\");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&")),
        "&");

    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#000a;")),
        "&#000a;");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#10;")),
        "\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#010;")),
        "\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#0010;")),
        "\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#9;")),
        "\t");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#10")),
        "&#10");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#00ziggy")),
        "&#00ziggy");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#&#010;")),
        "&#\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#0&#010;")),
        "&#0\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#01&#10;")),
        "&#01\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#&#10;")),
        "&#\n");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#1")),
        "&#1");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#10")),
        "&#10");

    // test the named escapes
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&lt;")),
        "<");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&gt;")),
        ">");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&quot;")),
        "\"");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&apos;")),
        "'");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#39;")),
        "'");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&#x27;")),
        "'");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&amp;")),
        "&");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&amp;lt;")),
        "&lt;");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&AMP;")),
        "&");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&AMP")),
        "&AMP");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&AmP;")),
        "&");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&Alpha;")),
        "\u0391");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&alpha;")),
        "\u03b1");


    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&;")),
        "&;");
    testProducer(
        CharProducer.Factory.fromHtmlAttribute(fromString("&bogus;")),
        "&bogus;");
  }

  public final void testChainingAndUnescaping() throws Exception {
    InputSource src = STRING_SOURCE;

    //                            12345678
    Reader r1 = new StringReader("hiThere(");
    //                            3333333
    //                            0123456
    Reader r2 = new StringReader(", now);");

    CharProducer prod1 = CharProducer.Factory.create(
        r1, FilePosition.startOfFile(src)),
                 prod2 = CharProducer.Factory.create(
        r2, FilePosition.instance(src, 2, 30, 1));

    String golden = "hiThere(, now);";

    CharProducer chained = CharProducer.Factory.fromHtmlAttribute(
        CharProducer.Factory.chain(prod1, prod2));

    testProducer(
        chained,
        golden,
        ss(0, "CharProducerTest.java:1+1@1"),
        ss(1, "CharProducerTest.java:1+2@2"),
        ss(2, "CharProducerTest.java:1+3@3"),
        ss(3, "CharProducerTest.java:1+4@4"),
        ss(4, "CharProducerTest.java:1+5@5"),
        ss(5, "CharProducerTest.java:1+6@6"),
        ss(6, "CharProducerTest.java:1+7@7"),
        ss(7, "CharProducerTest.java:1+8@8"),
        ss(8, "CharProducerTest.java:1+9@9"),
        ss(9, "CharProducerTest.java:2+2@31"),
        ss(10, "CharProducerTest.java:2+3@32"),
        ss(11, "CharProducerTest.java:2+4@33"),
        ss(12, "CharProducerTest.java:2+5@34"),
        ss(13, "CharProducerTest.java:2+6@35"),
        ss(14, "CharProducerTest.java:2+7@36"),
        ss(15, "CharProducerTest.java:2+8@37")
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
      CharProducer p, String golden, StreamState... positions) {
    List<Pair<String, FilePosition>> actualPositions
        = new ArrayList<Pair<String, FilePosition>>();
    StringBuilder sb = new StringBuilder();
    char[] buf = p.getBuffer();
    for (int k = p.getOffset(); ; p.consume(1)) {
      int offset = p.getOffset();
      if (k < positions.length && sb.length() == positions[k].charsRead) {
        FilePosition pos = p.getSourceBreaks(offset).toFilePosition(
            p.getCharInFile(offset));
        actualPositions.add(Pair.pair(sb.toString(), pos));
        ++k;
      }
      if (offset != p.getLimit()) {
        sb.append(buf[p.getOffset()]);
      } else {
        break;
      }
    }

    String actual = sb.toString();
    assertEquals(
        "golden:[" + escape(golden) + "]\nactual:[" + escape(actual) + "]",
        golden, actual);

    for (int k = 0; k < Math.min(positions.length, actualPositions.size()); ++k) {
      Pair<String, FilePosition> actualPos = actualPositions.get(k);
      String posStr = actualPos.b.toString();
      assertEquals(
          "Read so far [" + actualPos.a + "] : ["
          + golden.substring(
              0, Math.min(actualPos.a.length(), golden.length()))
          + "]",
          positions[k].pos, posStr);
    }
    assertEquals(positions.length, actualPositions.size());
  }

  private static final String decodeUri(String uriPart) {
    return CharProducer.Factory.fromUri(
        CharProducer.Factory.fromString(uriPart, InputSource.UNKNOWN))
        .toString();
  }

  public final void testFromUri() {
    assertEquals("", decodeUri(""));
    assertEquals("foo", decodeUri("foo"));
    // Plus (+) character not decoded to space, since + is not an escape
    // character in the body of non-hierarchical URLs
    // javascript:alert('foo+bar') issues an alert containing a plus character.
    assertEquals("foo+bar", decodeUri("foo+bar"));
    assertEquals("foo+bar", decodeUri("foo%2bbar"));
    assertEquals("foo@bar", decodeUri("foo%40bar"));
    assertEquals("\u00A0", decodeUri("%A0"));   // A single ASCII char
    // Test some well-formed UTF-8 sequences.
    assertEquals("foo\u0123bar", decodeUri("foo%C4%a3bar"));
    assertEquals("foo\u20ACbar", decodeUri("foo%e2%82%Acbar"));
    // There are multiple ways to encode supplementary characters
    String supplemental = String.valueOf(Character.toChars(0x1d11e));
    assertEquals(
        supplemental,
        decodeUri("%ed%a0%B4%eD%b4%9E"));  // as a surrogate pair
    assertEquals(
        supplemental,
        decodeUri("%F0%9d%84%9E"));  // as a 4 byte sequence
    assertEquals(
        supplemental,
        decodeUri("%f0%9D%84%9e"));  // as a 4 byte sequence with different case
    // Make sure our encoder round trips properly.
    assertEquals(supplemental, decodeUri(UriUtil.encode(supplemental)));
    // Test boundary conditions.
    assertEquals("%", decodeUri("%"));
    assertEquals("%2", decodeUri("%2"));  // An incomplete sequence
    assertEquals("%z", decodeUri("%z"));  // A non-hex follower.
    assertEquals("%", decodeUri("%25"));
    assertEquals("%2", decodeUri("%252"));
    assertEquals("%25", decodeUri("%2525"));  // Don't over decode.
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

  private static StreamState ss(int charsRead, String p) {
    return new StreamState(charsRead, p);
  }

  static class StreamState {
    final int charsRead;
    final String pos;

    StreamState(int charsRead, String pos) {
      this.charsRead = charsRead;
      this.pos = pos;
    }
  }
}
