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

import junit.framework.TestCase;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
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

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testMinimalEscapeJsString() throws Exception {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeJsString(CHARS, false, false, sb);
    assertStringsEqual(
        ("\\000\001\002\003\004\005\006\007\\b\t\\n\013\014\\r\016\017"
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
  }

  public void testParanoidEscapeJsString() throws Exception {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeJsString(CHARS, false, true, sb);
    assertStringsEqual(
        (// all ctrl chars escaped
         "\\000\\001\\002\\003\\004\\005\\006\\007"
         + "\\b\\t\\n\\v\\f\\r\\016\\017"
         + "\\020\\021\\022\\023\\024\\025\\026\\027"
         + "\\030\\031\\032\\033\\034\\035\\036\\037"
         + " !\\\"#$%&\\'()*+,-./"
         + "0123456789:;\\074=\\076?"  // < and > escaped
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

  public void testAsciiOnlyEscapeJsString() throws Exception {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeJsString(CHARS, true, false, sb);
    assertStringsEqual(
        ("\\000\001\002\003\004\005\006\007\\b\t\\n\013\014\\r\016\017"
         + "\020\021\022\023\024\025\026\027\030\031\032\033\034\035\036\037"
         + " !\\\"#$%&\\'()*+,-./"
         + "0123456789:;<=>?"
         + "@ABCDEFGHIJKLMNO"
         + "PQRSTUVWXYZ[\\\\]^_"
         + "`abcdefghijklmno"
         + "pqrstuvwxyz{|}~\\177"  // All chars >= 0x7f escaped
         + "\\200\\201\\202\\203\\204"
         + "\\u200e\\u200f\\u2010\\u2028\\u2029"
         + "\\ud834\\udd20"
         + "\\ud834\\udd77"
         ),
        sb.toString());
  }

  public void testParanoidAsciiOnlyEscapeJsString() throws Exception {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeJsString(CHARS, true, true, sb);
    assertStringsEqual(
        ("\\000\\001\\002\\003\\004\\005\\006\\007"
         + "\\b\\t\\n\\v\\f\\r\\016\\017"
         + "\\020\\021\\022\\023\\024\\025\\026\\027"
         + "\\030\\031\\032\\033\\034\\035\\036\\037"
         + " !\\\"#$%&\\'()*+,-./"
         + "0123456789:;\\074=\\076?"
         + "@ABCDEFGHIJKLMNO"
         + "PQRSTUVWXYZ[\\\\]^_"
         + "`abcdefghijklmno"
         + "pqrstuvwxyz{|}~\\177"
         + "\\200\\201\\202\\203\\204"
         + "\\u200e\\u200f\\u2010\\u2028\\u2029"
         + "\\ud834\\udd20"
         + "\\ud834\\udd77"
         ),
        sb.toString());
  }

  public void testMinimalEscapeRegex() throws Exception {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeRegex(CHARS, false, false, sb);
    assertStringsEqual(
        ("\\000\001\002\003\004\005\006\007\\b\t\\n\013\014\\r\016\017"
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

  public void testParanoidEscapeRegex() throws Exception {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeRegex(CHARS, false, true, sb);
    assertStringsEqual(
        (// all ctrl chars escaped
         "\\000\\001\\002\\003\\004\\005\\006\\007"
         + "\\b\\t\\n\\v\\f\\r\\016\\017"
         + "\\020\\021\\022\\023\\024\\025\\026\\027"
         + "\\030\\031\\032\\033\\034\\035\\036\\037"
         + " !\"#\\$%&'\\(\\)\\*\\+,-\\.\\/"
         + "0123456789:;\\074=\\076\\?"
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

  public void testRegexNormalization() throws Exception {
    StringBuilder sb = new StringBuilder();
    Escaping.normalizeRegex(
        "<Foo+\\> \u2028 \\\\Ba*r \r Baz\\+\\+", false, true, sb);
    assertStringsEqual(
        "\\074Foo+\\076 \\u2028 \\\\Ba*r \\r Baz\\+\\+", sb.toString());
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
