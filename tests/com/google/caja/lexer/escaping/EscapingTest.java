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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
@SuppressWarnings("static-method")
public class EscapingTest extends TestCase {

  private static final String CHARS;
  static {
    StringBuilder sb = new StringBuilder(150);
    for (int i = 0; i < 133; ++i) {
      sb.append((char) i);
    }
    sb.append('\u200E')  // [:Cf:] LRM
        .append('\u200F')  // [:Cf:] RLM
        .append('\u2010')  // Regular non-ascii codepoint
        .append('\u2028')  // newline
        .append('\u2029')  // newline
        .appendCodePoint(0x1D120)  // Regular supplemental
        .appendCodePoint(0x1D177);  // [:Cf:]
    CHARS = sb.toString();
  }
  private static final String WORD_CHARS;
  static {
    StringBuilder firstAndLastCodepage = new StringBuilder();
    for (int i = 0; i < 256; ++i) {
      firstAndLastCodepage.append((char) i);
    }
    for (int i = 0xff00; i < 0xffff; ++i) {
      firstAndLastCodepage.append((char) i);
    }

    StringBuilder sb = new StringBuilder();
    Matcher m = Pattern.compile("[\\p{javaLetterOrDigit}_$]+")
        .matcher(firstAndLastCodepage);
    while (m.find()) {
      sb.append(m.group());
    }
    WORD_CHARS = sb.toString();
  }

  public final void testMinimalEscapeJsString() {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeJsString(CHARS, false, false, sb);
    assertStringsEqual(
        ("\\x00\001\002\003\004\005\006\007\\b\t\\n\013\014\\r\016\017"
         + "\020\021\022\023\024\025\026\027\030\031\032\033\034\035\036\037"
         + " !\\\"#$%&\\'()*+,-./"
         + "0123456789:;<=>?"
         + "@ABCDEFGHIJKLMNO"
         + "PQRSTUVWXYZ[\\\\]^_"
         + "`abcdefghijklmno"
         + "pqrstuvwxyz{|}~\177"
         + "\u0080\u0081\u0082\u0083\u0084"
         + "\\u200e\\u200f\u2010\\u2028\\u2029"
         + "\ud834\udd20"
         + "\\ud834\\udd77"
         ),
        sb.toString());
    // Disallowed in strings in Firefox2
    assertJsEscaped("\\u200c\\u200d\\u200e\\u200f", "\u200C\u200D\u200E\u200F");
    assertJsEscaped(
        "\\u202a\\u202b\\u202c\\u202d\\u202e",
        "\u202A\u202B\u202C\u202D\u202E");
    assertJsEscaped(
        "\\u206b\\u206c\\u206d\\u206e\\u206f",
        "\u206B\u206C\u206D\u206E\u206F");
    assertJsEscaped("\\ufeff", "\uFEFF");
    // Disallowed in strings in IE6
    assertJsEscaped(
        "\\ufdd0\\ufdd1\\ufdd2\\ufdd3\\ufdd4\\ufdd5\\ufdd6\\ufdd7"
        + "\\ufdd8\\ufdd9\\ufdda\\ufddb\\ufddc\\ufddd\\ufdde\\ufddf",
        "\uFDD0\uFDD1\uFDD2\uFDD3\uFDD4\uFDD5\uFDD6\uFDD7"
        + "\uFDD8\uFDD9\uFDDA\uFDDB\uFDDC\uFDDD\uFDDE\uFDDF");
    assertJsEscaped(
        "\\ufde0\\ufde1\\ufde2\\ufde3\\ufde4\\ufde5\\ufde6\\ufde7"
        + "\\ufde8\\ufde9\\ufdea\\ufdeb\\ufdec\\ufded\\ufdee\\ufdef",
        "\uFDE0\uFDE1\uFDE2\uFDE3\uFDE4\uFDE5\uFDE6\uFDE7"
        + "\uFDE8\uFDE9\uFDEA\uFDEB\uFDEC\uFDED\uFDEE\uFDEF");
    assertJsEscaped(
        "\\ufff0\\ufff1\\ufff2\\ufff3\\ufff4\\ufff5\\ufff6\\ufff7"
        + "\\ufff8\\ufffe\\uffff",
        "\uFFF0\uFFF1\uFFF2\uFFF3\uFFF4\uFFF5\uFFF6\uFFF7"
        + "\uFFF8\uFFFE\uFFFF");
  }
  private static void assertJsEscaped(String golden, String raw) {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeJsString(raw, false, false, sb);
    assertStringsEqual(golden, sb.toString());
  }

  public final void testParanoidEscapeJsString() {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeJsString(CHARS, false, true, sb);
    assertStringsEqual(
        (// all ctrl chars escaped
         "\\x00\\x01\\x02\\x03\\x04\\x05\\x06\\x07"
         + "\\b\\t\\n\\x0b\\f\\r\\x0e\\x0f"
         + "\\x10\\x11\\x12\\x13\\x14\\x15\\x16\\x17"
         + "\\x18\\x19\\x1a\\x1b\\x1c\\x1d\\x1e\\x1f"
         + " !\\\"#$%\\x26\\'()*+,-./"  // & escaped
         + "0123456789:;\\x3c=\\x3e?"  // < and > escaped
         + "@ABCDEFGHIJKLMNO"
         + "PQRSTUVWXYZ[\\\\]^_"
         + "`abcdefghijklmno"
         + "pqrstuvwxyz{|}~\177"
         + "\u0080\u0081\u0082\u0083\u0084"
         + "\\u200e\\u200f\u2010\\u2028\\u2029"
         + "\ud834\udd20"
         + "\\ud834\\udd77"
         ),
        sb.toString());
  }

  public final void testEscapeJsonString() {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeJsonString(CHARS, false, sb);
    // JSON doesn't allow \x escapes, or \' as an escape sequence.
    assertStringsEqual(
        (// all ctrl chars escaped
         "\\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007"
         + "\\b\\t\\n\u000b\\f\\r\u000e\u000f"
         + "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017"
         + "\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001f"
         + " !\\\"#$%\u0026\\u0027()*+,-./"  // & escaped
         + "0123456789:;\u003c=\u003e?"  // < and > escaped
         + "@ABCDEFGHIJKLMNO"
         + "PQRSTUVWXYZ[\\\\]^_"
         + "`abcdefghijklmno"
         + "pqrstuvwxyz{|}~\177"
         + "\u0080\u0081\u0082\u0083\u0084"
         + "\\u200e\\u200f\u2010\\u2028\\u2029"
         + "\ud834\udd20"
         + "\\ud834\\udd77"
         ),
        sb.toString());
  }

  public final void testAsciiOnlyEscapeJsString() {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeJsString(CHARS, true, false, sb);
    assertStringsEqual(
        ("\\x00\001\002\003\004\005\006\007\\b\t\\n\013\014\\r\016\017"
         + "\020\021\022\023\024\025\026\027\030\031\032\033\034\035\036\037"
         + " !\\\"#$%&\\'()*+,-./"
         + "0123456789:;<=>?"
         + "@ABCDEFGHIJKLMNO"
         + "PQRSTUVWXYZ[\\\\]^_"
         + "`abcdefghijklmno"
         + "pqrstuvwxyz{|}~\\x7f"  // All chars >= 0x7f escaped
         + "\\x80\\x81\\x82\\x83\\x84"
         + "\\u200e\\u200f\\u2010\\u2028\\u2029"
         + "\\ud834\\udd20"
         + "\\ud834\\udd77"
         ),
        sb.toString());
  }

  public final void testParanoidAsciiOnlyEscapeJsString() {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeJsString(CHARS, true, true, sb);
    assertStringsEqual(
        ("\\x00\\x01\\x02\\x03\\x04\\x05\\x06\\x07"
         + "\\b\\t\\n\\x0b\\f\\r\\x0e\\x0f"
         + "\\x10\\x11\\x12\\x13\\x14\\x15\\x16\\x17"
         + "\\x18\\x19\\x1a\\x1b\\x1c\\x1d\\x1e\\x1f"
         + " !\\\"#$%\\x26\\'()*+,-./"
         + "0123456789:;\\x3c=\\x3e?"
         + "@ABCDEFGHIJKLMNO"
         + "PQRSTUVWXYZ[\\\\]^_"
         + "`abcdefghijklmno"
         + "pqrstuvwxyz{|}~\\x7f"
         + "\\x80\\x81\\x82\\x83\\x84"
         + "\\u200e\\u200f\\u2010\\u2028\\u2029"
         + "\\ud834\\udd20"
         + "\\ud834\\udd77"
         ),
        sb.toString());
  }

  public final void testIdentifierEscaping() {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeJsIdentifier(WORD_CHARS, true, sb);
    assertStringsEqual(
        (// all ctrl chars escaped
         "$0123456789"
         + "ABCDEFGHIJKLMNOPQRSTUVWXYZ_"
         + "abcdefghijklmnopqrstuvwxyz"
         + "\\u00aa\\u00b5\\u00ba"
         + "\\u00c0\\u00c1\\u00c2\\u00c3\\u00c4\\u00c5\\u00c6\\u00c7"
         + "\\u00c8\\u00c9\\u00ca\\u00cb\\u00cc\\u00cd\\u00ce\\u00cf"
         + "\\u00d0\\u00d1\\u00d2\\u00d3\\u00d4\\u00d5\\u00d6"
         + "\\u00d8\\u00d9\\u00da\\u00db\\u00dc\\u00dd\\u00de\\u00df"
         + "\\u00e0\\u00e1\\u00e2\\u00e3\\u00e4\\u00e5\\u00e6\\u00e7"
         + "\\u00e8\\u00e9\\u00ea\\u00eb\\u00ec\\u00ed\\u00ee\\u00ef"
         + "\\u00f0\\u00f1\\u00f2\\u00f3\\u00f4\\u00f5\\u00f6"
         + "\\u00f8\\u00f9\\u00fa\\u00fb\\u00fc\\u00fd\\u00fe\\u00ff"
         + "\\uff10\\uff11\\uff12\\uff13\\uff14\\uff15\\uff16\\uff17"
         + "\\uff18\\uff19"
         + "\\uff21\\uff22\\uff23\\uff24\\uff25\\uff26\\uff27"
         + "\\uff28\\uff29\\uff2a\\uff2b\\uff2c\\uff2d\\uff2e\\uff2f"
         + "\\uff30\\uff31\\uff32\\uff33\\uff34\\uff35\\uff36\\uff37"
         + "\\uff38\\uff39\\uff3a"
         + "\\uff41\\uff42\\uff43\\uff44\\uff45\\uff46\\uff47"
         + "\\uff48\\uff49\\uff4a\\uff4b\\uff4c\\uff4d\\uff4e\\uff4f"
         + "\\uff50\\uff51\\uff52\\uff53\\uff54\\uff55\\uff56\\uff57"
         + "\\uff58\\uff59\\uff5a"
         + "\\uff66\\uff67"
         + "\\uff68\\uff69\\uff6a\\uff6b\\uff6c\\uff6d\\uff6e\\uff6f"
         + "\\uff70\\uff71\\uff72\\uff73\\uff74\\uff75\\uff76\\uff77"
         + "\\uff78\\uff79\\uff7a\\uff7b\\uff7c\\uff7d\\uff7e\\uff7f"
         + "\\uff80\\uff81\\uff82\\uff83\\uff84\\uff85\\uff86\\uff87"
         + "\\uff88\\uff89\\uff8a\\uff8b\\uff8c\\uff8d\\uff8e\\uff8f"
         + "\\uff90\\uff91\\uff92\\uff93\\uff94\\uff95\\uff96\\uff97"
         + "\\uff98\\uff99\\uff9a\\uff9b\\uff9c\\uff9d\\uff9e\\uff9f"
         + "\\uffa0\\uffa1\\uffa2\\uffa3\\uffa4\\uffa5\\uffa6\\uffa7"
         + "\\uffa8\\uffa9\\uffaa\\uffab\\uffac\\uffad\\uffae\\uffaf"
         + "\\uffb0\\uffb1\\uffb2\\uffb3\\uffb4\\uffb5\\uffb6\\uffb7"
         + "\\uffb8\\uffb9\\uffba\\uffbb\\uffbc\\uffbd\\uffbe"
         + "\\uffc2\\uffc3\\uffc4\\uffc5\\uffc6\\uffc7"
         + "\\uffca\\uffcb\\uffcc\\uffcd\\uffce\\uffcf"
         + "\\uffd2\\uffd3\\uffd4\\uffd5\\uffd6\\uffd7"
         + "\\uffda\\uffdb\\uffdc"
         ),
        sb.toString());
  }


  public final void testMinimalEscapeRegex() {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeRegex(CHARS, false, false, sb);
    assertStringsEqual(
        ("\\x00\001\002\003\004\005\006\007\\b\t\\n\013\014\\r\016\017"
         + "\020\021\022\023\024\025\026\027\030\031\032\033\034\035\036\037"
         // quotes unescaped.  regex specials escaped
         + " !\"#\\$%&'\\(\\)\\*\\+,-\\.\\/"
         + "0123456789:;<=>\\?"
         + "@ABCDEFGHIJKLMNO"
         + "PQRSTUVWXYZ\\[\\\\\\]\\^_"
         + "`abcdefghijklmno"
         + "pqrstuvwxyz\\{\\|\\}~\177"
         + "\u0080\u0081\u0082\u0083\u0084"
         + "\\u200e\\u200f\u2010\\u2028\\u2029"
         + "\ud834\udd20"
         + "\\ud834\\udd77"
         ),
        sb.toString());
  }

  public final void testParanoidEscapeRegex() {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeRegex(CHARS, false, true, sb);
    assertStringsEqual(
        (// all ctrl chars escaped
         "\\x00\\x01\\x02\\x03\\x04\\x05\\x06\\x07"
         + "\\b\\t\\n\\x0b\\f\\r\\x0e\\x0f"
         + "\\x10\\x11\\x12\\x13\\x14\\x15\\x16\\x17"
         + "\\x18\\x19\\x1a\\x1b\\x1c\\x1d\\x1e\\x1f"
         + " !\"#\\$%\\x26'\\(\\)\\*\\+,-\\.\\/"
         + "0123456789:;\\x3c=\\x3e\\?"
         + "@ABCDEFGHIJKLMNO"
         + "PQRSTUVWXYZ\\[\\\\\\]\\^_"
         + "`abcdefghijklmno"
         + "pqrstuvwxyz\\{\\|\\}~\177"
         + "\u0080\u0081\u0082\u0083\u0084"
         + "\\u200e\\u200f\u2010\\u2028\\u2029"
         + "\ud834\udd20"
         + "\\ud834\\udd77"
         ),
        sb.toString());
  }

  public final void testRegexNormalization() {
    StringBuilder sb = new StringBuilder();
    Escaping.normalizeRegex(
        "<Foo+\\> \u2028 \\\\Ba*r \r Baz\\+\\+", sb);
    assertStringsEqual(
        "\\x3cFoo+\\x3e \\u2028 \\\\Ba*r \\r Baz\\+\\+", sb.toString());
  }

  public final void testRegexNormalizationBalancesCharGroups() {
    // Make sure that the normalized regex always has balanced [...] blocks
    // since / in those are not considered as closing the token.
    {
      StringBuilder sb = new StringBuilder();
      Escaping.normalizeRegex("[", sb);
      assertStringsEqual("\\[", sb.toString());
    }

    {
      StringBuilder sb = new StringBuilder();
      Escaping.normalizeRegex("[a-z][foo", sb);
      assertStringsEqual("[a-z]\\[foo", sb.toString());
    }

    {
      StringBuilder sb = new StringBuilder();
      Escaping.normalizeRegex("[a-z][[foo]", sb);
      assertStringsEqual("[a-z][\\[foo]", sb.toString());
    }

    {
      StringBuilder sb = new StringBuilder();
      Escaping.normalizeRegex("[a-z][[foo", sb);
      assertStringsEqual("[a-z]\\[\\[foo", sb.toString());
    }

    {
      StringBuilder sb = new StringBuilder();
      Escaping.normalizeRegex("[a-z][[foo[", sb);
      assertStringsEqual("[a-z]\\[\\[foo\\[", sb.toString());
    }
  }

  public final void testEscapeXml() {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeXml(CHARS, false, sb);
    assertStringsEqual(
        (// all ctrl chars escaped
         "&#0;&#1;&#2;&#3;&#4;&#5;&#6;&#7;"
         + "&#8;\t\n&#11;&#12;\r&#14;&#15;"
         + "&#16;&#17;&#18;&#19;&#20;&#21;&#22;&#23;"
         + "&#24;&#25;&#26;&#27;&#28;&#29;&#30;&#31;"
         + " !&#34;#$%&amp;&#39;()*+,-./"
         + "0123456789:;&lt;=&gt;?"
         + "@ABCDEFGHIJKLMNO"
         + "PQRSTUVWXYZ[\\]^_"
         + "&#96;abcdefghijklmno"
         + "pqrstuvwxyz{|}~&#127;"
         + "&#128;&#129;&#130;&#131;&#132;"
         + "\u200e\u200f\u2010\u2028\u2029"
         + "\ud834\udd20"
         + "\ud834\udd77"
         ),
        sb.toString());
  }

  public final void testEscapeXmlAsciiOnly() {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeXml(CHARS, true, sb);
    assertStringsEqual(
        (// all ctrl chars escaped
         "&#0;&#1;&#2;&#3;&#4;&#5;&#6;&#7;"
         + "&#8;\t\n&#11;&#12;\r&#14;&#15;"
         + "&#16;&#17;&#18;&#19;&#20;&#21;&#22;&#23;"
         + "&#24;&#25;&#26;&#27;&#28;&#29;&#30;&#31;"
         + " !&#34;#$%&amp;&#39;()*+,-./"
         + "0123456789:;&lt;=&gt;?"
         + "@ABCDEFGHIJKLMNO"
         + "PQRSTUVWXYZ[\\]^_"
         + "&#96;abcdefghijklmno"
         + "pqrstuvwxyz{|}~&#127;"
         + "&#128;&#129;&#130;&#131;&#132;"
         + "&#8206;&#8207;&#8208;&#8232;&#8233;"
         // Surrogate pairs are escaped by code-point, not code-unit.
         + "&#119072;"
         + "&#119159;"
         ),
        sb.toString());
  }

  public final void testEscapeCssString() {
    StringBuilder sb;

    sb = new StringBuilder();
    Escaping.escapeCssString(CHARS, sb);
    assertStringsEqual(
        ("\\0\\1\\2\\3\\4\\5\\6\\7"
         + "\\8 \\9 \\A\\B \\C \\D\\E\\F"
         + "\\10\\11\\12\\13\\14\\15\\16\\17"
         + "\\18\\19\\1A\\1B\\1C\\1D\\1E\\1F "
         + " !\\22#$%&\\27\\28\\29\\2A\\2B\\2C-./"
         + "0123456789:\\3B\\3C=\\3E?"
         + "\\40 ABCDEFGHIJKLMNO"
         + "PQRSTUVWXYZ\\5B\\5C\\5D^_"
         + "`abcdefghijklmno"
         + "pqrstuvwxyz\\7B\\7C\\7D~\\7F"
         + "\\80\\81\\82\\83\\84"
         + "\\200E\\200F\\2010\\2028\\2029"
         + "\\D834\\DD20"
         + "\\D834\\DD77 "
         ),
        sb.toString());

    sb = new StringBuilder();
    Escaping.escapeCssString("<foo>", sb);
    assertStringsEqual("\\3C foo\\3E ", sb.toString());

    sb = new StringBuilder();
    Escaping.escapeCssString("<Bar>", sb);
    assertStringsEqual("\\3C Bar\\3E ", sb.toString());

    sb = new StringBuilder();
    Escaping.escapeCssString("<ZZZ>", sb);
    assertStringsEqual("\\3CZZZ\\3E ", sb.toString());
  }

  public final void testEscapeCssIdent() {
    StringBuilder sb;

    sb = new StringBuilder();
    Escaping.escapeCssIdent(CHARS, sb);
    assertStringsEqual(
        ("\\0\\1\\2\\3\\4\\5\\6\\7"
         + "\\8 \\9 \\A\\B \\C \\D\\E\\F"
         + "\\10\\11\\12\\13\\14\\15\\16\\17"
         + "\\18\\19\\1A\\1B\\1C\\1D\\1E\\1F "
         + "\\20\\21\\22\\23\\24\\25\\26\\27"
         + "\\28\\29\\2A\\2B\\2C-\\2E\\2F "
         + "0123456789\\3A\\3B\\3C\\3D\\3E\\3F"
         + "\\40 ABCDEFGHIJKLMNO"
         + "PQRSTUVWXYZ\\5B\\5C\\5D\\5E_"
         + "\\60 abcdefghijklmno"
         + "pqrstuvwxyz\\7B\\7C\\7D\\7E\\7F"
         + "\\80\\81\\82\\83\\84"
         + "\\200E\\200F\\2010\\2028\\2029"
         + "\\D834\\DD20"
         + "\\D834\\DD77 "
         ),
        sb.toString());

    sb = new StringBuilder();
    Escaping.escapeCssIdent("foo-bar", sb);
    assertStringsEqual("foo-bar", sb.toString());

    sb = new StringBuilder();
    Escaping.escapeCssIdent("fo o", sb);
    assertStringsEqual("fo\\20o", sb.toString());

    sb = new StringBuilder();
    Escaping.escapeCssIdent("0foo", sb);
    assertStringsEqual("\\30 foo", sb.toString());

    sb = new StringBuilder();
    Escaping.escapeCssIdent("0zoicks", sb);
    assertStringsEqual("\\30zoicks", sb.toString());

    sb = new StringBuilder();
    Escaping.escapeCssIdent("4", sb);
    assertStringsEqual("\\34 ", sb.toString());
  }

  public static void testEscapeUri() {
    StringBuilder sb;

    sb = new StringBuilder();
    Escaping.escapeUri("", sb);
    assertEquals("", sb.toString());

    sb = new StringBuilder();
    Escaping.escapeUri("foo", sb);
    assertEquals("foo", sb.toString());

    sb = new StringBuilder();
    Escaping.escapeUri("FOO", sb);
    assertEquals("FOO", sb.toString());

    // All of the reserved characters must be encoded.
    sb = new StringBuilder();
    Escaping.escapeUri(":/?#[]@!$&'()*+,;=", sb);
    assertEquals(
        "%3a%2f%3f%23%5b%5d%40%21%24%26%27%28%29%2a%2b%2c%3b%3d",
        sb.toString());

    sb = new StringBuilder();
    Escaping.escapeUri(CHARS, sb);
    assertEquals(
        ""
        + "%00%01%02%03%04%05%06%07%08%09%0a%0b%0c%0d%0e%0f"
        + "%10%11%12%13%14%15%16%17%18%19%1a%1b%1c%1d%1e%1f"
        // %20, not + since that is not valid in non-hierarchical URIs.
        + "%20%21%22%23%24%25%26%27%28%29%2a%2b%2c-.%2f"
        + "0123456789%3a%3b%3c%3d%3e%3f"
        + "%40ABCDEFGHIJKLMNO"
        + "PQRSTUVWXYZ%5b%5c%5d%5e_"
        + "%60abcdefghijklmno"
        + "pqrstuvwxyz%7b%7c%7d~%7f"
        + "%c2%80%c2%81%c2%82%c2%83%c2%84"
        + "%e2%80%8e%e2%80%8f%e2%80%90%e2%80%a8%e2%80%a9"
        + "%ed%a0%b4%ed%b4%a0%ed%a0%b4%ed%b5%b7",
        sb.toString());
  }

  private static void assertStringsEqual(String a, String b) {
    if (a.equals(b)) { return; }
    int m = a.length(), n = b.length();
    int min = Math.min(m, n);

    int commonPrefix = 0;
    while (commonPrefix < min
           && a.charAt(commonPrefix) == b.charAt(commonPrefix)) {
      ++commonPrefix;
    }

    int commonSuffix = 0;
    while (commonSuffix < (min - commonPrefix)
           && a.charAt(m - commonSuffix - 1) == b.charAt(n - commonSuffix - 1)
           ) {
      ++commonSuffix;
    }

    int msgPrefix = Math.max(0, commonPrefix - 2);
    int msgSuffix = Math.max(0, commonSuffix - 2);
    fail(diagnosticString(a, msgPrefix, m - msgSuffix) + " != " +
         diagnosticString(b, msgPrefix, n - msgSuffix));
  }

  private static String diagnosticString(String s, int start, int end) {
    StringBuilder sb = new StringBuilder(end - start + 16);
    if (start != 0) { sb.append("..."); }
    for (int i = start; i < end; ++i) {
      char ch = s.charAt(i);
      if (ch >= 0x20 && ch < 0x7f) {
        sb.append(ch);
      } else {
        sb.append('{').append(Integer.toString(ch, 16)).append('}');
      }
    }
    if (end < s.length()) { sb.append("..."); }
    return sb.toString();
  }
}
