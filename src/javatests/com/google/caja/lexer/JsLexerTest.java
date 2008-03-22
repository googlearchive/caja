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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

import junit.framework.TestCase;

/**
 * testcases for {@link JsLexer}.
 *
 * @author mikesamuel@gmail.com
 */
public class JsLexerTest extends TestCase {

  public void testLexer() throws Exception {
    InputSource input = new InputSource(
        TestUtil.getResource(getClass(), "lexertest1.js"));
    StringBuilder output = new StringBuilder();

    BufferedReader in = new BufferedReader(
        new InputStreamReader(TestUtil.getResourceAsStream(
                                  getClass(), "lexertest1.js")));
    JsLexer t = new JsLexer(in, input);
    try {
      while (t.hasNext()) {
        Token<JsTokenType> tok = t.next();
        output.append(tok.type.toString().substring(0, 4)
                      + " [" + tok.text + "]: " + tok.pos + "\n");
      }
    } finally {
      in.close();
    }

    String golden;
    BufferedReader goldenIn = new BufferedReader(
        new InputStreamReader(TestUtil.getResourceAsStream(
                                  getClass(), "lexergolden1.txt")));
    try {
      StringBuilder sb = new StringBuilder();
      char[] buf = new char[1024];
      for (int n; (n = goldenIn.read(buf)) > 0;) {
        sb.append(buf, 0, n);
      }
      golden = sb.toString();
    } finally {
      goldenIn.close();
    }

    assertEquals(golden, output.toString());
  }

  public void testLexer2() throws Exception {
    InputSource input = new InputSource(
        TestUtil.getResource(getClass(), "lexertest2.js"));
    StringBuilder output = new StringBuilder();

    BufferedReader in = new BufferedReader(
        new InputStreamReader(TestUtil.getResourceAsStream(
                                  getClass(), "lexertest2.js")));
    JsLexer t = new JsLexer(in, input);
    try {
      while (t.hasNext()) {
        Token<JsTokenType> tok = t.next();
        output.append(tok.type.toString().substring(0, 4)
                      + " [" + tok.text + "]: " + tok.pos + "\n");
      }
    } catch (ParseException ex) {
      ex.printStackTrace();
    } finally {
      in.close();
    }

    String golden;
    BufferedReader goldenIn = new BufferedReader(
        new InputStreamReader(TestUtil.getResourceAsStream(
                                  getClass(), "lexergolden2.txt")));
    try {
      StringBuilder sb = new StringBuilder();
      char[] buf = new char[1024];
      for (int n; (n = goldenIn.read(buf)) > 0;) {
        sb.append(buf, 0, n);
      }
      golden = sb.toString();
    } finally {
      goldenIn.close();
    }

    assertEquals(golden, output.toString());
  }

  public void testSimpleExpression() {
    JsLexer lexer = createLexer("while (foo) { 1; }");
    assertNext(lexer, JsTokenType.KEYWORD, "while");
    assertNext(lexer, JsTokenType.PUNCTUATION, "(");
    assertNext(lexer, JsTokenType.WORD, "foo");
    assertNext(lexer, JsTokenType.PUNCTUATION, ")");
    assertNext(lexer, JsTokenType.PUNCTUATION, "{");
    assertNext(lexer, JsTokenType.INTEGER, "1");
    assertNext(lexer, JsTokenType.PUNCTUATION, ";");
    assertNext(lexer, JsTokenType.PUNCTUATION, "}");
    assertEmpty(lexer);
  }

  public void testQuasiExpressionSingle() {
    JsLexer lexer = createLexer("@foo * 1;", true);
    assertNext(lexer, JsTokenType.WORD, "@foo");
    assertNext(lexer, JsTokenType.PUNCTUATION, "*");
    assertNext(lexer, JsTokenType.INTEGER, "1");
    assertNext(lexer, JsTokenType.PUNCTUATION, ";");
    assertEmpty(lexer);
  }

  public void testQuasiExpressionStar() {
    JsLexer lexer = createLexer("@foo* * 1;", true);
    assertNext(lexer, JsTokenType.WORD, "@foo*");
    assertNext(lexer, JsTokenType.PUNCTUATION, "*");
    assertNext(lexer, JsTokenType.INTEGER, "1");
    assertNext(lexer, JsTokenType.PUNCTUATION, ";");
    assertEmpty(lexer);
  }

  public void testQuasiExpressionPlus() {
    JsLexer lexer = createLexer("@foo+ + 1;", true);
    assertNext(lexer, JsTokenType.WORD, "@foo+");
    assertNext(lexer, JsTokenType.PUNCTUATION, "+");
    assertNext(lexer, JsTokenType.INTEGER, "1");
    assertNext(lexer, JsTokenType.PUNCTUATION, ";");
    assertEmpty(lexer);
  }

  public void testQuasiExpressionSingleParens() {
    JsLexer lexer = createLexer("(@foo)", true);
    assertNext(lexer, JsTokenType.PUNCTUATION, "(");
    assertNext(lexer, JsTokenType.WORD, "@foo");
    assertNext(lexer, JsTokenType.PUNCTUATION, ")");
    assertEmpty(lexer);
  }

  public void testQuasiExpressionStarParens() {
    JsLexer lexer = createLexer("(@foo*)", true);
    assertNext(lexer, JsTokenType.PUNCTUATION, "(");
    assertNext(lexer, JsTokenType.WORD, "@foo*");
    assertNext(lexer, JsTokenType.PUNCTUATION, ")");
    assertEmpty(lexer);
  }

  public void testQuasiExpressionPlusParens() {
    JsLexer lexer = createLexer("(@foo+)", true);
    assertNext(lexer, JsTokenType.PUNCTUATION, "(");
    assertNext(lexer, JsTokenType.WORD, "@foo+");
    assertNext(lexer, JsTokenType.PUNCTUATION, ")");
    assertEmpty(lexer);
  }

  public void testDoubleDot() {
    JsLexer lexer = createLexer("a == = function () {..} ... .. . foo", true);
    assertNext(lexer, JsTokenType.WORD, "a");
    assertNext(lexer, JsTokenType.PUNCTUATION, "==");
    assertNext(lexer, JsTokenType.PUNCTUATION, "=");
    assertNext(lexer, JsTokenType.KEYWORD, "function");
    assertNext(lexer, JsTokenType.PUNCTUATION, "(");
    assertNext(lexer, JsTokenType.PUNCTUATION, ")");
    assertNext(lexer, JsTokenType.PUNCTUATION, "{");
    assertNext(lexer, JsTokenType.PUNCTUATION, ".");
    assertNext(lexer, JsTokenType.PUNCTUATION, ".");
    assertNext(lexer, JsTokenType.PUNCTUATION, "}");
    assertNext(lexer, JsTokenType.PUNCTUATION, "...");
    assertNext(lexer, JsTokenType.PUNCTUATION, ".");
    assertNext(lexer, JsTokenType.PUNCTUATION, ".");
    assertNext(lexer, JsTokenType.PUNCTUATION, ".");
    assertNext(lexer, JsTokenType.WORD, "foo");
    assertEmpty(lexer);
  }

  public void testNumberDotWord() {
    JsLexer lexer = createLexer("0..toString()", false);  // evaluates to "0"
    assertNext(lexer, JsTokenType.FLOAT, "0.");
    assertNext(lexer, JsTokenType.PUNCTUATION, ".");
    assertNext(lexer, JsTokenType.WORD, "toString");
    assertNext(lexer, JsTokenType.PUNCTUATION, "(");
    assertNext(lexer, JsTokenType.PUNCTUATION, ")");
    assertEmpty(lexer);
  }

  private JsLexer createLexer(String src) {
    return createLexer(src, false);
  }

  private JsLexer createLexer(String src, boolean isQuasiliteral) {
    InputSource input;
    try {
      input = new InputSource(new URI("file:///no/such/file"));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    BufferedReader in = new BufferedReader(new StringReader(src));
    return new JsLexer(in, input, isQuasiliteral);
  }

  private void assertNext(JsLexer lexer, JsTokenType type, String text) {
    Token<JsTokenType> tok = null;
    try {
      tok = lexer.next();
    } catch (ParseException e) {
      fail(e.toString());
    }
    assertEquals(type, tok.type);
    assertEquals("was '" + tok.text + "', expected '" + text + "'",
                 text, tok.text);
  }

  public void assertEmpty(JsLexer lexer) {
    try {
      assertFalse(lexer.hasNext());
    } catch (ParseException e) {
      fail(e.toString());
    }
  }

  public static void main(String[] args) throws Exception {
    InputSource input = new InputSource(URI.create("file:///proc/self/fd/0"));

    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    JsLexer t = new JsLexer(in, input);
    while (t.hasNext()) {
      Token<JsTokenType> tok = t.next();
      System.out.append(tok.type.toString().substring(0, 4)
                        + " [" + tok.text + "]: " + tok.pos + "\n");
    }
  }
}
