// Copyright (C) 2008 Google Inc.
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

package com.google.caja.render;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.JsTokenType;
import com.google.caja.lexer.Keyword;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.MoreAsserts;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("static-method")
public class JsPrettyPrinterTest extends CajaTestCase {
  public final void testEmptyBlock() throws Exception {
    assertRendered("{ {} }", "{}");
  }

  public final void testAdjacentBlocks() throws Exception {
    assertRendered("{ {}\n  {} }", "{}{}");
  }

  public final void testSimpleStatement() throws Exception {
    assertRendered("{ foo(); }", "foo();");
  }

  public final void testLongLines() throws Exception {
    assertRendered(
        ""
        + "{\n  cdefgh10abcdefgh20abcdefgh30abcdefgh40"
        + "abcdefgh50abcdefgh60abcdefgh70abcd();\n}",
        ""
        + "  cdefgh10abcdefgh20abcdefgh30abcdefgh40"
        + "abcdefgh50abcdefgh60abcdefgh70abcd();"
        );
    assertRendered(
        ""
        + "{\n  cdefgh10abcdefgh20abcdefgh30abcdefgh40"
        + "abcdefgh50abcdefgh60abcdefgh70abcde();\n"
        + "}",
        ""
        + "  cdefgh10abcdefgh20abcdefgh30abcdefgh40"
        + "abcdefgh50abcdefgh60abcdefgh70abcde();"
        );
    assertRendered(
        ""
        + "{\n"
        + "  cdefgh10abcdefgh20abcdefgh30abcdefgh40"
        + "abcdefgh50abcdefgh60abcdefgh70abcdefgh()\n"
        + "    ;"
        + "\n}",
        ""
        + "  cdefgh10abcdefgh20abcdefgh30abcdefgh40"
        + "abcdefgh50abcdefgh60abcdefgh70abcdefgh();"
        );
  }

  public final void testSemisInsideParents() throws Exception {
    assertRendered(
        "{\n  for (var i = 0, n = a.length; i < n; ++i) {\n"
        + "    bar(a[ i ]);\n  }\n}",
        "for (var i = 0, n = a.length; i < n; ++i) {"
        + "  bar(a[ i ]);"
        + "}");
  }

  public final void testObjectConstructor() throws Exception {
    assertRendered(
        "{\n"
        + "  foo({\n"
        + "      'x': 1,\n"
        + "      'y': bar({ 'w': 4 }),\n"
        + "      'z': 3\n"
        + "    });\n"
        + "}",
        "foo({ x: 1, y: bar({ w: 4 }), z: 3 });");
  }

  public final void testMultipleStatements() throws Exception {
    assertRendered(
        "{\n"
        + "  (function (a, b, c) {\n"
        + "     foo(a);\n"
        + "     bar(b);\n"
        + "     return c;\n"
        + "   })(1, 2, 3);\n"
        + "}",
        "(function (a, b, c) { foo(a); bar(b); return (c); })(1, 2, 3);");
  }

  public final void testBreakBeforeWhile() throws Exception {
    assertRendered(
        "{\n"
        + "  do {\n"
        + "    foo(bar());\n"
        + "  } while (1);\n"
        + "}",
        "do { foo(bar()); } while(1);");
    assertRendered(
        "{\n"
        + "  {\n"
        + "    foo(bar());\n"
        + "  }\n"
        + "  while (1);\n"
        + "}",
        "{ foo(bar()); } while(1);");
  }

  public final void testMarkupEndStructures() throws Exception {
    // Make sure -->, </script, and ]]> don't show up in rendered output.
    // Preventing these in strings is handled separately.
    assertRendered(
        "{\n  (i--) > j, k < /script\\x3e/, [ [ 0 ] ] > 0;\n}",
        "i-->j, k</script>/, [[0]]>0;");
  }

  public final void testJSON() throws Exception {
    assertRendered(
        "{\n"
        + "  ({\n"
        + "     'a': [ 1, 2, 3 ],\n"
        + "     'b': {\n"
        + "       'c': [{}],\n"
        + "       'd': [{\n"
        + "           'e': null,\n"
        + "           'f': 'foo'\n"
        + "         }, null ]\n"
        + "     }\n"
        + "   });\n"
        + "}",
        "({ a: [1,2,3], b: { c: [{}], d: [{ e: null, f: 'foo' }, null] } });");
  }

  public final void testConditional() throws Exception {
    assertRendered(
        "{\n"
        + "  if (c1) { foo(); } else if (c2) bar();\n"
        + "  else baz();\n"
        + "}",
        "if (c1) { foo(); } else if (c2) bar(); else baz();");
  }

  public final void testNumberPropertyAccess() throws Exception {
    assertRendered("{\n  (3).toString();\n}", "(3).toString();");
  }

  public final void testComments() throws Exception {
    assertLexed(
        "var x = foo; /* end of line */\n"
        + "/** Own line */\n"
        + "function Bar() {}\n"
        + "/* Beginning */\n"
        + "var baz;\n"
        + "a+// Line comment\n"
        + "  b;",

        ""
        + "var x = foo;  /* end of line */\n"
        + "/** Own line */\n"
        + "function Bar() {}\n"
        + "/* Beginning */ var baz;\n"
        + "a +  // Line comment\n"
        + "b;");
  }

  public final void testDivisionByRegex() throws Exception {
    assertLexed("3/ /foo/;", "3 / /foo/;");
  }

  public final void testNegatedNegativeNumericConstants() {
    assertRendered(
        "- (-3)",  // not --3
        Operation.create(
            FilePosition.UNKNOWN, Operator.NEGATION,
            new IntegerLiteral(FilePosition.UNKNOWN,-3)));
  }

  public final void testRetokenization() throws Exception {
    long seed = Long.parseLong(
        System.getProperty("junit.seed", "" + System.currentTimeMillis()));
    Random rnd = new Random(seed);
    try {
      for (int i = 1000; --i >= 0;) {
        List<String> randomTokens = generateRandomTokens(rnd);
        StringBuilder sb = new StringBuilder();
        JsPrettyPrinter pp = new JsPrettyPrinter(sb);
        for (String token : randomTokens) {
          pp.consume(token);
        }
        pp.noMoreTokens();

        List<String> actualTokens = new ArrayList<String>();
        try {
          JsLexer lex = new JsLexer(fromString(sb.toString()));
          while (lex.hasNext()) {
            actualTokens.add(simplifyComments(lex.next().text));
          }
        } catch (ParseException ex) {
          for (String tok : randomTokens) {
            System.err.println(StringLiteral.toQuotedValue(tok));
          }
          System.err.println("<<<" + sb + ">>>");
          throw ex;
        }

        List<String> simplifiedRandomTokens = new ArrayList<String>();
        for (String randomToken : randomTokens) {
          simplifiedRandomTokens.add(simplifyComments(randomToken));
        }

        MoreAsserts.assertListsEqual(simplifiedRandomTokens, actualTokens);
      }
    } catch (Exception e) {
      System.err.println("Using seed " + seed);
      throw e;
    }
  }

  /**
   * The renderer is allowed to muck with comment internals to fix problems
   * with line-breaks in restricted productions.
   */
  private static String simplifyComments(String token) {
    if (token.startsWith("//")) {
      token = "/*" + token.substring(2) + "*/";
    }
    if (!token.startsWith("/*")) { return token; }

    StringBuilder sb = new StringBuilder(token);
    for (int i = sb.length() - 2; --i >= 2;) {
      if (JsLexer.isJsLineSeparator(sb.charAt(i))) {
        sb.setCharAt(i, ' ');
      }
    }
    for (int close = -1; (close = sb.indexOf("*/", close + 1)) >= 0;) {
      sb.setCharAt(close + 1, ' ');
    }
    return sb.toString();
  }

  public final void testIndentationAfterParens1() {
    assertTokens("longObjectInstance.reallyLongMethodName(a, b, c, d);",
                 "longObjectInstance", ".", "reallyLongMethodName", "(",
                 "a", ",", "b", ",", "c", ",", "d", ")", ";");
  }

  public final void testIndentationAfterParens2() {
    assertTokens("longObjectInstance.reallyLongMethodName(a, b, c,\n" +
                 "  d);",
                 "longObjectInstance", ".", "reallyLongMethodName", "(",
                 "a", ",", "b", ",", "c", ",", "\n", "d", ")", ";");
  }

  public final void testIndentationAfterParens3() {
    assertTokens("longObjectInstance.reallyLongMethodName(\n" +
                 "  a, b, c, d);",
                 "longObjectInstance", ".", "reallyLongMethodName", "(",
                 "\n", "a", ",", "b", ",", "c", ",", "d", ")", ";");
  }

  public final void testIndentationAfterParens4() {
    assertTokens("var x = ({\n" +
                 "    'fooBar': [\n" +
                 "      0, 1, 2, ]\n" +
                 "  });",
                 "var", "x", "=", "(", "{", "'fooBar'", ":", "[",
                 "\n", "0", ",", "1", ",", "2", ",", "]", "}", ")", ";");
  }

  public final void testCommentsInRestrictedProductions1() {
    assertTokens("return /* */ 4;", "return", "/*\n*/", "4", ";");
  }

  public final void testCommentsInRestrictedProductions2() {
    assertTokens("return /**/ 4;", "return", "//", "4", ";");
  }

  private static final JsTokenType[] TYPES = JsTokenType.values();
  private static final Operator[] OPERATORS = Operator.values();
  private static final Keyword[] KEYWORDS = Keyword.values();

  private static List<String> generateRandomTokens(Random rnd) {
    List<String> tokens = new ArrayList<String>();
    for (int i = 10; --i >= 0;) {
      final String tok;
      switch (TYPES[rnd.nextInt(TYPES.length)]) {
        case COMMENT:
          if (rnd.nextBoolean()) {
            String s = "//" + randomString(rnd).replace('\r', '\ufffd')
                .replace('\n', '\ufffd').replace('\u2028', '\ufffd')
                .replace('\u2029', '\ufffd');
            if (s.endsWith("\\")) {  // This is blocked in CStyleTokenClass
              s = s + " ";
            }
            tok = s;
          } else {
            tok = "/*" + randomString(rnd).replace('*', '.') + "*/";
          }
          break;
        case STRING:
          tok = StringLiteral.toQuotedValue(randomString(rnd));
          break;
        case REGEXP:
          // Since regexps are context sensitive, make sure we're in the right
          // context.
          tokens.add("=");
          StringBuilder out = new StringBuilder();
          out.append('/');
          Escaping.normalizeRegex(randomString(rnd), out);
          out.append('/');
          if (rnd.nextBoolean()) { out.append('g'); }
          if (rnd.nextBoolean()) { out.append('m'); }
          if (rnd.nextBoolean()) { out.append('i'); }
          tok = out.toString();
          break;
        case PUNCTUATION:
          Operator op = OPERATORS[rnd.nextInt(OPERATORS.length)];
          if (op.getClosingSymbol() != null) {
            tok = rnd.nextBoolean()
                ? op.getOpeningSymbol()
                : op.getClosingSymbol();
          } else {
            tok = op.getSymbol();
          }
          if (tok.startsWith("/")) {
            // Make sure / operators follow numbers so they're not interpreted
            // as regular expressions.
            tokens.add("3");
          }
          break;
        case WORD:
          tok = randomWord(rnd);
          break;
        case KEYWORD:
          tok = KEYWORDS[rnd.nextInt(KEYWORDS.length)].toString();
          break;
        case INTEGER:
          int j = rnd.nextInt(Integer.MAX_VALUE);
          switch (rnd.nextInt(3)) {
            case 0: tok = Integer.toString(j, 10); break;
            case 1: tok = "0" + Integer.toString(Math.abs(j), 8); break;
            default: tok = "0x" + Long.toString(Math.abs((long) j), 16); break;
          }
          break;
        case FLOAT:
          tok = "" + Math.abs(rnd.nextFloat());
          break;
        case LINE_CONTINUATION:
          continue;
        default:
          throw new SomethingWidgyHappenedError();
      }
      tokens.add(tok);
    }
    return tokens;
  }

  private static final String WORD_CHARS
      = "_$ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static String randomWord(Random rnd) {
    int len = rnd.nextInt(100) + 1;
    StringBuilder sb = new StringBuilder(len);
    for (int i = 0; i < len; ++i) {
      sb.append(WORD_CHARS.charAt(rnd.nextInt(WORD_CHARS.length())));
    }
    if (Character.isDigit(sb.charAt(0))) {
      sb.insert(0, '_');
    }
    return sb.toString();
  }

  private static String randomString(Random rnd) {
    int minCp = 0, maxCp = 0;
    if (rnd.nextBoolean()) {
      minCp = 0x20;
      maxCp = 0x7f;
    } else {
      minCp = 0x0;
      maxCp = 0xd000;
    }
    int len = rnd.nextInt(100) + 1;
    StringBuilder sb = new StringBuilder(len);
    for (int i = 0; i < len; ++i) {
      sb.appendCodePoint(rnd.nextInt(maxCp - minCp) + minCp);
    }
    return sb.toString();
  }

  private void assertRendered(String golden, String input) throws Exception {
    JsLexer lex = new JsLexer(fromString(input));
    JsTokenQueue tq = new JsTokenQueue(lex, is);
    ParseTreeNode node = new Parser(tq, mq).parse();
    tq.expectEmpty();

    assertRendered(golden, node);
  }

  private static void assertRendered(String golden, ParseTreeNode node) {
    StringBuilder out = new StringBuilder();
    JsPrettyPrinter pp = new JsPrettyPrinter(out);
    node.render(new RenderContext(pp));
    pp.noMoreTokens();

    assertEquals(golden, out.toString());
  }

  private void assertLexed(String golden, String input) throws Exception {
    StringBuilder out = new StringBuilder();
    JsPrettyPrinter pp = new JsPrettyPrinter(out);

    JsLexer lex = new JsLexer(fromString(input));
    while (lex.hasNext()) {
      Token<JsTokenType> t = lex.next();
      pp.mark(t.pos);
      pp.consume(t.text);
    }
    pp.noMoreTokens();

    assertEquals(golden, out.toString());
  }

  private static void assertTokens(String golden, String... input) {
    StringBuilder out = new StringBuilder();
    JsPrettyPrinter pp = new JsPrettyPrinter(out);

    for (String token : input) {
      pp.consume(token);
    }
    pp.noMoreTokens();
    assertEquals(golden, out.toString());
  }
}
