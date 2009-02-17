// Copyright (C) 2006 Google Inc.
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
import com.google.caja.util.TestUtil;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public class CssLexerTest extends CajaTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testLex() throws Exception {
    CharProducer cp = fromResource("csslexerinput1.css");
    runTest(cp, TestUtil.readResource(getClass(), "csslexergolden1.txt"));
  }

  public void testUnterminatedStrings() throws Exception {
    assertFails("font-family: 'foo", "1+18: Unclosed string");
  }

  public void testLinebreakInString() throws Exception {
    assertFails("font-family: 'foo\nbar'", "1+18: Illegal char in string '\n'");
  }

  public void testEofInEscape() throws Exception {
    assertFails("font-family: 'foo\\abc", "1+22: Unclosed string");
  }

  public void testUnterminatedComment() throws Exception {
    assertFails("foo\nb /* bar ", "2+3: Unclosed comment");
  }

  public void testUnterminatedFunction() throws Exception {
    assertFails("url(bar", "1+8: Expected ) not -1");
  }

  public void testMalformedNumber() throws Exception {
    assertFails("100.?", "1+5: Malformed number 100.");
  }

  public void testDecodeCssIdentifier() throws Exception {
    assertEquals("foo", CssLexer.decodeCssIdentifier("foo"));
    assertEquals("foo", CssLexer.decodeCssIdentifier("f\\6fo"));
    assertEquals("foo", CssLexer.decodeCssIdentifier("f\\6f o"));
    assertEquals("foo", CssLexer.decodeCssIdentifier("fo\\6f"));
    assertEquals("foo", CssLexer.decodeCssIdentifier("f\\6f\\6f"));
    assertEquals("ofo", CssLexer.decodeCssIdentifier("\\6f f\\6f"));
    assertEquals("foo", CssLexer.decodeCssIdentifier("\\66\\6f\\6f"));
    assertEquals("foo", CssLexer.decodeCssIdentifier("\\66 \\6f \\6f "));
  }

  private void assertFails(String input, String golden) {
    try {
      runTest(input, "expected failure: " + golden);
      fail(input);
    } catch (ParseException ex) {
      String actual = ex.getCajaMessage().format(mc);
      actual = actual.substring(actual.indexOf(':') + 1);
      assertEquals(golden, actual);
    }
  }

  private void runTest(String input, String golden) throws ParseException {
    runTest(fromString(input), golden);
  }

  private void runTest(CharProducer cp, String golden) throws ParseException {
    CssLexer lexer = new CssLexer(cp, true);
    StringBuilder sb = new StringBuilder();
    while (lexer.hasNext()) {
      Token<CssTokenType> t = lexer.next();
      sb.append(abbr(t.type.name())).append(" [")
        .append(escape(t.text)).append("]: ").append(t.pos).append('\n');
      assert t.text.length() == t.pos.endCharInFile() - t.pos.startCharInFile()
           : t.text + ": " + t.pos;
    }
    assertEquals(golden.trim(), sb.toString().trim());
  }

  private static final String escape(String s) {
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); ++i) {
      char ch = s.charAt(i);
      switch (ch) {
        case '\n': sb.append("\\n"); break;
        case '\r': sb.append("\\r"); break;
        case '\\': sb.append("\\\\"); break;
        default: sb.append(ch); break;
      }
    }
    return sb.toString();
  }

  private static String abbr(String s) {
    int n = s.length();
    return n < 4 ? s + "    ".substring(0, 4 - n) : s.substring(0, 4);
  }
}
