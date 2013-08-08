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

import com.google.caja.util.CajaTestCase;
import com.google.caja.util.FailureIsAnOption;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

import junit.framework.AssertionFailedError;

/**
 * testcases for {@link JsLexer}.
 *
 * @author mikesamuel@gmail.com
 */
public class JsLexerTest extends CajaTestCase {

  public final void testLexer() throws Exception {
    StringBuilder output = new StringBuilder();
    JsLexer t = new JsLexer(fromResource("lexertest1.js"));
    while (t.hasNext()) {
      Token<JsTokenType> tok = t.next();
      output.append(tok.type.toString().substring(0, 4)
                    + " [" + tok.text + "]: " + tok.pos + "\n");
    }

    String golden = fromResource("lexergolden1.txt").toString();

    assertEquals(golden, output.toString());
  }

  public final void testLexer2() throws Exception {
    StringBuilder output = new StringBuilder();

    JsLexer t = new JsLexer(fromResource("lexertest2.js"));
    while (t.hasNext()) {
      Token<JsTokenType> tok = t.next();
      output.append(tok.type.toString().substring(0, 4)
                    + " [" + tok.text + "]: " + tok.pos + "\n");
    }

    String golden = fromResource("lexergolden2.txt").toString();

    assertEquals(golden, output.toString());
  }

  public final void testRegexLiterals() {
    JsLexer lexer = createLexer("foo.replace(/[A-Z]/g, '#')");
    assertNext(lexer, JsTokenType.WORD, "foo");
    assertNext(lexer, JsTokenType.PUNCTUATION, ".");
    assertNext(lexer, JsTokenType.WORD, "replace");
    assertNext(lexer, JsTokenType.PUNCTUATION, "(");
    assertNext(lexer, JsTokenType.REGEXP, "/[A-Z]/g");
    assertNext(lexer, JsTokenType.PUNCTUATION, ",");
    assertNext(lexer, JsTokenType.STRING, "'#'");
    assertNext(lexer, JsTokenType.PUNCTUATION, ")");
    assertEmpty(lexer);
  }

  public final void testSimpleExpression() {
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

  public final void testQuasiExpressionSingle() {
    JsLexer lexer = createLexer("@foo * 1;", true);
    assertNext(lexer, JsTokenType.WORD, "@foo");
    assertNext(lexer, JsTokenType.PUNCTUATION, "*");
    assertNext(lexer, JsTokenType.INTEGER, "1");
    assertNext(lexer, JsTokenType.PUNCTUATION, ";");
    assertEmpty(lexer);
  }

  public final void testQuasiExpressionStar() {
    JsLexer lexer = createLexer("@foo* * 1;", true);
    assertNext(lexer, JsTokenType.WORD, "@foo*");
    assertNext(lexer, JsTokenType.PUNCTUATION, "*");
    assertNext(lexer, JsTokenType.INTEGER, "1");
    assertNext(lexer, JsTokenType.PUNCTUATION, ";");
    assertEmpty(lexer);
  }

  public final void testQuasiExpressionPlus() {
    JsLexer lexer = createLexer("@foo+ + 1;", true);
    assertNext(lexer, JsTokenType.WORD, "@foo+");
    assertNext(lexer, JsTokenType.PUNCTUATION, "+");
    assertNext(lexer, JsTokenType.INTEGER, "1");
    assertNext(lexer, JsTokenType.PUNCTUATION, ";");
    assertEmpty(lexer);
  }

  public final void testQuasiExpressionSingleParens() {
    JsLexer lexer = createLexer("(@foo)", true);
    assertNext(lexer, JsTokenType.PUNCTUATION, "(");
    assertNext(lexer, JsTokenType.WORD, "@foo");
    assertNext(lexer, JsTokenType.PUNCTUATION, ")");
    assertEmpty(lexer);
  }

  public final void testQuasiExpressionStarParens() {
    JsLexer lexer = createLexer("(@foo*)", true);
    assertNext(lexer, JsTokenType.PUNCTUATION, "(");
    assertNext(lexer, JsTokenType.WORD, "@foo*");
    assertNext(lexer, JsTokenType.PUNCTUATION, ")");
    assertEmpty(lexer);
  }

  public final void testQuasiExpressionPlusParens() {
    JsLexer lexer = createLexer("(@foo+)", true);
    assertNext(lexer, JsTokenType.PUNCTUATION, "(");
    assertNext(lexer, JsTokenType.WORD, "@foo+");
    assertNext(lexer, JsTokenType.PUNCTUATION, ")");
    assertEmpty(lexer);
  }

  public final void testDoubleDot() {
    JsLexer lexer = createLexer(
        "a == = function () {..} ... .. . .... foo", true);
    assertNext(lexer, JsTokenType.WORD, "a");
    assertNext(lexer, JsTokenType.PUNCTUATION, "==");
    assertNext(lexer, JsTokenType.PUNCTUATION, "=");
    assertNext(lexer, JsTokenType.KEYWORD, "function");
    assertNext(lexer, JsTokenType.PUNCTUATION, "(");
    assertNext(lexer, JsTokenType.PUNCTUATION, ")");
    assertNext(lexer, JsTokenType.PUNCTUATION, "{");
    assertNext(lexer, JsTokenType.PUNCTUATION, "..");
    assertNext(lexer, JsTokenType.PUNCTUATION, "}");
    assertNext(lexer, JsTokenType.PUNCTUATION, "...");
    assertNext(lexer, JsTokenType.PUNCTUATION, "..");
    assertNext(lexer, JsTokenType.PUNCTUATION, ".");
    assertNext(lexer, JsTokenType.PUNCTUATION, "...");
    assertNext(lexer, JsTokenType.PUNCTUATION, ".");
    assertNext(lexer, JsTokenType.WORD, "foo");
    assertEmpty(lexer);
  }

  public final void testNumberDotWord() {
    JsLexer lexer = createLexer("0..toString()", false);  // evaluates to "0"
    assertNext(lexer, JsTokenType.FLOAT, "0.");
    assertNext(lexer, JsTokenType.PUNCTUATION, ".");
    assertNext(lexer, JsTokenType.WORD, "toString");
    assertNext(lexer, JsTokenType.PUNCTUATION, "(");
    assertNext(lexer, JsTokenType.PUNCTUATION, ")");
    assertEmpty(lexer);
  }

  public final void testByteOrderMarkersAtBeginning() {
    JsLexer lexer = createLexer("\uFEFFvar foo", false);
    assertNext(lexer, JsTokenType.KEYWORD, "var");
    assertNext(lexer, JsTokenType.WORD, "foo");
    assertEmpty(lexer);
  }

  public final void testByteOrderMarkersBetweenTokens() {
    JsLexer lexer = createLexer("1.\uFEFF3", false);
    assertNext(lexer, JsTokenType.FLOAT, "1.");
    assertNext(lexer, JsTokenType.INTEGER, "3");
    assertEmpty(lexer);
  }

  public final void testByteOrderMarkersInStrings() {
    JsLexer lexer = createLexer("'\uFEFF'", false);
    assertNext(lexer, JsTokenType.STRING, "'\uFEFF'");
    assertEmpty(lexer);
  }

  public final void testEllipsisAndNumber() {
    JsLexer lexer = createLexer("...0x01", false);
    assertNext(lexer, JsTokenType.PUNCTUATION, "...");
    assertNext(lexer, JsTokenType.INTEGER, "0x01");
    assertEmpty(lexer);
  }

  public final void testEmphaticallyDecremented() {
    JsLexer lexer = createLexer("i---j", false);
    assertNext(lexer, JsTokenType.WORD, "i");
    assertNext(lexer, JsTokenType.PUNCTUATION, "--");
    assertNext(lexer, JsTokenType.PUNCTUATION, "-");
    assertNext(lexer, JsTokenType.WORD, "j");
    assertEmpty(lexer);
  }

  public final void testIsRegexpFollowingWord() {
    {
      JsLexer lexer = createLexer("min / max /*/**/", false);
      assertNext(lexer, JsTokenType.WORD, "min");
      assertNext(lexer, JsTokenType.PUNCTUATION, "/");
      assertNext(lexer, JsTokenType.WORD, "max");
      assertNext(lexer, JsTokenType.COMMENT, "/*/**/");
      assertEmpty(lexer);
    }
    {
      JsLexer lexer = createLexer("in / max /*/**/", false);
      assertNext(lexer, JsTokenType.KEYWORD, "in");
      assertNext(lexer, JsTokenType.REGEXP, "/ max /");
      assertNext(lexer, JsTokenType.PUNCTUATION, "*");
      assertNext(lexer, JsTokenType.COMMENT, "/**/");
      assertEmpty(lexer);
    }
  }

  public final void testRegexpFollowingVoid() {
    JsLexer lexer = createLexer("void /./", false);
    assertNext(lexer, JsTokenType.KEYWORD, "void");
    assertNext(lexer, JsTokenType.REGEXP, "/./");
    assertEmpty(lexer);
  }

  @FailureIsAnOption
  public final void testRegexpFollowingPreincrement() {
    // Dividing a post-incremented value
    //     x++ /x/m
    // is much more common than pre-incrementing a regular expression,
    //     x=++/x/m
    // so our lexer heuristic treats / after ++ or -- as a division
    // operator.
    JsLexer lexer = createLexer("x = ++/x/m", false);
    assertNext(lexer, JsTokenType.WORD, "x");
    assertNext(lexer, JsTokenType.PUNCTUATION, "=");
    assertNext(lexer, JsTokenType.PUNCTUATION, "++");
    assertNext(lexer, JsTokenType.REGEXP, "/x/m");
    assertEmpty(lexer);
  }

  @FailureIsAnOption
  public final void testDivisionFollowingObjectConstructor() {
    // Code like
    //     while (x) { ... }
    //     /foo/.test(bar) && ...
    // is much more common than attempts to divide an object constructor
    //     alert({ valueOf: function () { return 42; } } / 2);
    // so our lexer heuristic treats / after a close curly bracket as
    // a regular expression.
    JsLexer lexer = createLexer("({f:g}/h/i)", false);
    assertNext(lexer, JsTokenType.PUNCTUATION, "{");
    assertNext(lexer, JsTokenType.WORD, "f");
    assertNext(lexer, JsTokenType.PUNCTUATION, ":");
    assertNext(lexer, JsTokenType.WORD, "g");
    assertNext(lexer, JsTokenType.PUNCTUATION, "}");
    assertNext(lexer, JsTokenType.PUNCTUATION, "/");
    assertNext(lexer, JsTokenType.WORD, "h");
    assertNext(lexer, JsTokenType.PUNCTUATION, "/");
    assertNext(lexer, JsTokenType.WORD, "i");
    assertEmpty(lexer);
  }

  public final void testRegexpFollowingPostincrement() {
    JsLexer lexer = createLexer("x++/y/m", false);
    assertNext(lexer, JsTokenType.WORD, "x");
    assertNext(lexer, JsTokenType.PUNCTUATION, "++");
    assertNext(lexer, JsTokenType.PUNCTUATION, "/");
    assertNext(lexer, JsTokenType.WORD, "y");
    assertNext(lexer, JsTokenType.PUNCTUATION, "/");
    assertNext(lexer, JsTokenType.WORD, "m");
    assertEmpty(lexer);
  }

  private JsLexer createLexer(String src) {
    return createLexer(src, false);
  }

  private JsLexer createLexer(String src, boolean isQuasiliteral) {
    return new JsLexer(fromString(src), isQuasiliteral);
  }

  private static void assertNext(JsLexer lexer, JsTokenType type, String text) {
    Token<JsTokenType> tok = null;
    try {
      tok = lexer.next();
    } catch (ParseException e) {
      throw (AssertionFailedError)
          new AssertionFailedError(e.getMessage()).initCause(e);
    }
    assertEquals(type, tok.type);
    assertEquals("was '" + tok.text + "', expected '" + text + "'",
                 text, tok.text);
  }

  public static void assertEmpty(JsLexer lexer) {
    try {
      assertFalse(lexer.hasNext());
    } catch (ParseException e) {
      fail(e.toString());
    }
  }

  public static void main(String[] args) throws Exception {
    InputSource input = new InputSource(URI.create("file:///proc/self/fd/0"));

    BufferedReader in = new BufferedReader(
        new InputStreamReader(System.in, "UTF-8"));
    JsLexer t = new JsLexer(CharProducer.Factory.create(in, input));
    while (t.hasNext()) {
      Token<JsTokenType> tok = t.next();
      System.out.append(tok.type.toString().substring(0, 4)
                        + " [" + tok.text + "]: " + tok.pos + "\n");
    }
  }
}
