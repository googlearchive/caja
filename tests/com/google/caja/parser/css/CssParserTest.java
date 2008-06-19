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

package com.google.caja.parser.css;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.parser.css.CssTree.StyleSheet;
import com.google.caja.reporting.MessageContext;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.TestUtil;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class CssParserTest extends CajaTestCase {

  public void testBadHashValue() throws Exception {
    throwsParseException("h1 { color: #OOOOOO}");
  }

  public void testUnescape() throws Exception {
    FilePosition pos = FilePosition.instance(is, 1, 1, 1, 1);
    assertEquals("", CssParser.unescape(
        Token.instance("", CssTokenType.IDENT, pos)));
    assertEquals("foo", CssParser.unescape(
        Token.instance("foo", CssTokenType.IDENT, pos)));
    assertEquals("foo", CssParser.unescape(
        Token.instance("f\\oo", CssTokenType.IDENT, pos)));
    assertEquals("!important", CssParser.unescape(
        Token.instance("! important", CssTokenType.IDENT, pos)));
    assertEquals("!important", CssParser.unescape(
        Token.instance("!   important", CssTokenType.IDENT, pos)));
    assertEquals("'foo bar'", CssParser.unescape(
        Token.instance("'foo bar'", CssTokenType.STRING, pos)));
    assertEquals("'foo bar'", CssParser.unescape(
        Token.instance("'foo\\ bar'", CssTokenType.STRING, pos)));
    assertEquals("'foo bar'", CssParser.unescape(
        Token.instance("'foo\\ b\\\nar'", CssTokenType.STRING, pos)));
    assertEquals("'foo bar'", CssParser.unescape(
        Token.instance("'foo\\ b\\\rar'", CssTokenType.STRING, pos)));
    assertEquals("'ffoo bar'", CssParser.unescape(
        Token.instance("'\\66 foo bar'", CssTokenType.STRING, pos)));
    assertEquals("foo-bar", CssParser.unescape(
        Token.instance("\\66oo-ba\\0072", CssTokenType.IDENT, pos)));
    assertEquals("\\66oo-bar", CssParser.unescape(
        Token.instance("\\\\66oo-ba\\0072", CssTokenType.IDENT, pos)));
  }

  public void testCssParser1() throws Exception {
    runTestCssParser("cssparserinput1.css", "cssparsergolden1.txt");
  }

  public void testCssParser2() throws Exception {
    runTestCssParser("cssparserinput2.css", "cssparsergolden2.txt");
  }

  public void testCssParser3() throws Exception {
    runTestCssParser("cssparserinput3.css", "cssparsergolden3.txt");
  }

  public void testCssParser4() throws Exception {
    runTestCssParser("cssparserinput4.css", "cssparsergolden4.txt");
  }

  private void runTestCssParser(String cssFile, String goldenFile)
      throws Exception {
    String golden = TestUtil.readResource(getClass(), goldenFile);
    CssTree.StyleSheet stylesheet;
    CharProducer cp = fromResource(cssFile);
    try {
      stylesheet = css(cp);
    } finally {
      cp.close();
    }
    StringBuilder sb = new StringBuilder();
    stylesheet.format(new MessageContext(), sb);
    assertEquals(golden.trim(), sb.toString().trim());
  }

  private void throwsParseException(String fuzzString) {
    try {
      parseString(fuzzString);
    } catch (ParseException e) {
      // ParseException thrown - parser worked
      return;
    } catch (Throwable e) {
      // any other kind of exception means the parser broke
      e.printStackTrace();
      fail();
    }
  }

  private StyleSheet parseString(String fuzzString) throws Exception {
    return css(fromString(fuzzString));
  }
}
