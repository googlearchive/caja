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
public class JsMinimalPrinterTest extends CajaTestCase {
  public final void testEmptyBlock() throws Exception {
    assertRendered("{{}}", "{}");
  }

  public final void testAdjacentBlocks() throws Exception {
    assertRendered("{{}{}}", "{}{}");
  }

  public final void testSimpleStatement() throws Exception {
    assertRendered("{foo()}", "foo();");
  }

  public final void testSemisInsideParents() throws Exception {
    assertRendered(
        "{for(var i=0,n=a.length;i<n;++i){"
        + "bar(a[i])}}",
        "for (var i = 0, n = a.length; i < n; ++i) {"
        + "  bar(a[ i ]);"
        + "}");
  }

  public final void testObjectConstructor() throws Exception {
    assertRendered(
        "{foo({'x':1,'y':bar({'w':4}),'z':3})}",
        "foo({ x: 1, y: bar({ w: 4 }), z: 3 });");
  }

  public final void testMultipleStatements() throws Exception {
    assertRendered(
        "{(function(a,b,c){foo(a);bar(b);return c})(1,2,3)}",
        "(function (a, b, c) { foo(a); bar(b); return (c); })(1, 2, 3);");
  }

  public final void testMarkupEndStructures() throws Exception {
    // Make sure -->, </script, and ]]> don't show up in rendered output.
    // Preventing these in strings is handled separately.
    assertRendered(
        "{(i--)>j,k< /script\\x3e/,[[0]] >0/ / / *x}",
        "i-->j, k</script>/, [[0]]>0 / / / * x;");
  }

  public final void testMarkupStartStructure() throws Exception {
    // Make sure <!-- and <![CDATA[ don't show up in rendered output.
    // Preventing these in strings is handled separately.
    assertRendered(
        "{1< !--b&&c< ![CDATA[0]]}",
        "1<!--b && c<![CDATA[0]]");
  }

  public final void testJSON() throws Exception {
    assertRendered(
        "{({'a':[1,2,3],'b':{'c':[{}],'d':[{'e':null,'f':'foo'},null]}})}",
        "({ a: [1,2,3], b: { c: [{}], d: [{ e: null, f: 'foo' }, null] } });");
  }

  public final void testConditional() throws Exception {
    assertRendered(
        "{if(c1){foo()}else if(c2)bar();else baz()}",
        "if (c1) { foo(); } else if (c2) bar(); else baz();");
  }

  public final void testNumberPropertyAccess() throws Exception {
    assertRendered("{(3).toString()}", "(3).toString();");
  }

  public final void testComments() throws Exception {
    assertLexed(
        "var x=foo;function Bar(){}var baz;a+b",

        ""
        + "var x = foo;  /* end of line */\n"
        + "/** Own line */\n"
        + "function Bar() {}\n"
        + "/* Beginning */ var baz;\n"
        + "a +  // Line comment\n"
        + "b;");
  }

  public final void testBackslashNewline() throws Exception {
    // Backslash newline is elided in strings.
    assertRendered(
        "{var x='string continuation'}",
        "var x = 'string \\\ncontinuation'");
    // Backslash newline is not special in comments.
    assertRendered(
        "{var x='noncontinuation'}",
        "var x = // comment \\\n'noncontinuation'");
    // Backslash newline elsewhere is a syntax error, and is rendered
    // in a way that is still a syntax error.
    assertLexed(
        "illegal\\ continuation",
        "illegal\\\ncontinuation");
  }

  public final void testDivisionByRegex() throws Exception {
    assertLexed("3/ /foo/", "3 / /foo/;");
  }

  public final void testPunctuationRun() throws Exception {
    assertLexed("!=|| =", "!= || =");
  }

  public final void testIdentifierNameDoesNotRunIntoRegExpFlags()
      throws Exception {
    assertRendered(
        "/foo/ instanceof RegExp",
        jsExpr(fromString("/foo/ instanceof RegExp")));
    assertRendered(
        "/foo/i instanceof RegExp",
        jsExpr(fromString("/foo/i instanceof RegExp")));
  }

  public final void testNegatedNegativeNumericConstants() {
    assertRendered(
        "-(-3)",  // not --3
        Operation.create(
            FilePosition.UNKNOWN, Operator.NEGATION,
            new IntegerLiteral(FilePosition.UNKNOWN,-3)));
  }

  public final void testRetokenization() throws Exception {
    long seed = Long.parseLong(
        System.getProperty("junit.seed", "" + System.currentTimeMillis()));
    Random rnd = new Random(seed);
    boolean pass = false;
    try {
      for (int i = 1000; --i >= 0;) {
        List<String> randomTokens = generateRandomTokens(rnd);
        StringBuilder sb = new StringBuilder();
        JsMinimalPrinter pp = new JsMinimalPrinter(sb);
        for (String token : randomTokens) {
          pp.consume(token);
        }
        pp.noMoreTokens();

        List<String> actualTokens = new ArrayList<String>();
        try {
          JsLexer lex = new JsLexer(fromString(sb.toString()));
          while (lex.hasNext()) {
            actualTokens.add(lex.next().text);
          }
        } catch (ParseException ex) {
          for (String tok : randomTokens) {
            System.err.println(StringLiteral.toQuotedValue(tok));
          }
          System.err.println("<<<" + sb + ">>>");
          throw ex;
        }

        MoreAsserts.assertListsEqual(randomTokens, actualTokens);
      }
      pass = true;
    } finally {
      if (!pass) { System.err.println("Using seed " + seed); }
    }
  }

  public final void testSpacingAroundBrackets1() {
    assertTokens("longObjectInstance.reallyLongMethodName(a,b,c,d)",
                 "longObjectInstance", ".", "reallyLongMethodName", "(",
                 "a", ",", "b", ",", "c", ",", "d", ")", ";");
  }

  public final void testSpacingAroundBrackets2() {
    assertTokens("longObjectInstance.reallyLongMethodName(a,b,c,d)",
                 "longObjectInstance", ".", "reallyLongMethodName", "(",
                 "a", ",", "b", ",", "c", ",", "\n", "d", ")", ";");
  }

  public final void testSpacingAroundBrackets3() {
    assertTokens("longObjectInstance.reallyLongMethodName(a,b,c,d)",
                 "longObjectInstance", ".", "reallyLongMethodName", "(",
                 "\n", "a", ",", "b", ",", "c", ",", "d", ")", ";");
  }

  public final void testSpacingAroundBrackets4() {
    assertTokens("var x=({'fooBar':[0,1,2,]})",
                 "var", "x", "=", "(", "{", "'fooBar'", ":", "[",
                 "\n", "0", ",", "1", ",", "2", ",", "]", "}", ")", ";");
  }

  public final void testConfusedTokenSequences() {
    assertTokens("< ! =", "<", "!", "=");
    assertTokens("< !=", "<", "!=");
  }

  public final void testNumbersAndDots() {
    assertTokens("2 .toString()", "2", ".", "toString", "(", ")");
    assertTokens("2..toString()", "2.", ".", "toString", "(", ")");
    assertTokens("2. .5", "2.", ".5");
    assertTokens("html4.bar", "html4", ".", "bar");
    assertTokens("html. 4 .bar", "html", ".", "4", ".", "bar");
    assertTokens("e4.bar", "e4", ".", "bar");
    assertTokens("1e4 .bar", "1e4", ".", "bar");
  }

  public final void testRestrictedSemicolonInsertion() throws Exception {
    ParseTreeNode node = js(fromString(
        ""
        // 0123456789
        + "var x=abcd+\n"
        + "+ef;return 1-\n"
        + "-c;if(b)throw new\n"
        + "Error();break label;do\n"
        + "nothing;while(0);continue top;a-\n"
        + "-b;number=counter++"
        + ";number=counter--"
        + ";number=n-++"
        + "counter"
        ));
    StringBuilder out = new StringBuilder();
    JsMinimalPrinter pp = new JsMinimalPrinter(out);
    pp.setLineLengthLimit(10);
    node.render(new RenderContext(pp));
    pp.noMoreTokens();
    assertEquals(
        "{var x=abcd+"
        + "\n+ef;return 1-"
        + "\n-c;if(b)throw new"
        + "\nError;break label;do"
        + "\nnothing;while(0);continue top;a-"
        + "\n-b;number=counter++;number=counter--;number=n-++counter}",
        out.toString());
  }

  public final void testNoops() throws ParseException {
    ParseTreeNode b = js(fromString(
        "for (;;) {",
        "  if (foo)",
        "    bar();",
        "  else",
        "    ;",
        "}"));
    StringBuilder out = new StringBuilder();
    JsMinimalPrinter pp = new JsMinimalPrinter(out);
    b.render(new RenderContext(pp));
    pp.noMoreTokens();
    assertEquals(
        "{for(;;){if(foo)bar();else;}}",
        out.toString());
  }

  public final void testInfixAndPrefixOpMerging() {
    // "x--1" tokenizes to "x", "--", "1" which is an invalid expression.
    assertTokens("x- -1", "x", "-", "-1");
    assertTokens("x- -1", "x", "-", "-", "1");
    assertTokens("x+ +1", "x", "+", "+1");
    assertTokens("x+ +1", "x", "+", "+", "1");
  }

  private static final JsTokenType[] TYPES = JsTokenType.values();
  private static final String[] PUNCTUATORS;
  static {
    List<String> puncStrs = new ArrayList<String>();
    JsLexer.getPunctuationTrie().toStringList(puncStrs);
    PUNCTUATORS = puncStrs.toArray(new String[0]);
  }
  private static final Keyword[] KEYWORDS = Keyword.values();

  private static List<String> generateRandomTokens(Random rnd) {
    List<String> tokens = new ArrayList<String>();
    String last = null;
    for (int i = 10; --i >= 0;) {
      String tok;
      switch (TYPES[rnd.nextInt(TYPES.length)]) {
        case COMMENT:
          continue;
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
          tok = PUNCTUATORS[rnd.nextInt(PUNCTUATORS.length)];
          if (tok.startsWith("/")) {
            // Make sure / operators follow numbers so they're not interpreted
            // as regular expressions.
            tokens.add("3");
          }
          if ("}".equals(tok) && ";".equals(last)) {
            tok = "{";
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
      last = tok;
    }
    while (";".equals(last)) {
      tokens.remove(tokens.size() - 1);
      last = tokens.isEmpty() ? null : tokens.get(tokens.size() - 1);
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
    JsMinimalPrinter pp = new JsMinimalPrinter(out);
    node.render(new RenderContext(pp));
    pp.noMoreTokens();

    assertEquals(golden, out.toString());
  }

  private void assertLexed(String golden, String input) throws ParseException {
    StringBuilder out = new StringBuilder();
    JsMinimalPrinter pp = new JsMinimalPrinter(out);

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
    JsMinimalPrinter pp = new JsMinimalPrinter(out);

    for (String token : input) {
      pp.consume(token);
    }
    pp.noMoreTokens();
    assertEquals(golden, out.toString());
  }
}
