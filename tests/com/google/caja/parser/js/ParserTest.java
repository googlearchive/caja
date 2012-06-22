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

package com.google.caja.parser.js;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.Keyword;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.MoreAsserts;
import com.google.caja.util.Strings;
import com.google.caja.util.TestUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.AssertionFailedError;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class ParserTest extends CajaTestCase {
  // TODO(mikesamuel): better comment each of the test input files.
  // What is each one supposed to test.

  public final void testParser() throws Exception {
    runParseTest("parsertest1.js", "parsergolden1.txt",
                 "Reserved word else used as an identifier");

    // Check warnings on message queue.
    Iterator<Message> msgs = mq.getMessages().iterator();

    assertNextMessage(
        msgs,
        MessageType.RESERVED_WORD_USED_AS_IDENTIFIER,
        "parsertest1.js:11+29 - 33",
        Keyword.ELSE);
    assertNextMessage(
        msgs,
        MessageType.NOT_IE,
        "parsertest1.js:35+7 - 8");
    assertNextMessage(
        msgs,
        MessageType.SEMICOLON_INSERTED,
        "parsertest1.js:96+2");

    // No semicolon needed at the end.
    assertTrue(!msgs.hasNext());
  }
  public final void testParser2() throws Exception {
    runParseTest("parsertest2.js", "parsergolden2.txt");

    Iterator<Message> msgs = mq.getMessages().iterator();

    assertNextMessage(
        msgs,
        MessageType.SEMICOLON_INSERTED,
        "parsertest2.js:4+3");

    assertTrue(!msgs.hasNext());
  }
  public final void testParser3() throws Exception {
    runParseTest("parsertest3.js", "parsergolden3.txt");
    assertTrue(mq.getMessages().isEmpty());
  }
  public final void testParser5() throws Exception {
    runParseTest("parsertest5.js", "parsergolden5.txt");
  }
  public final void testParser7() throws Exception {
    runParseTest("parsertest7.js", "parsergolden7.txt");
  }
  public final void testParser8() throws Exception {
    runParseTest("parsertest8.js", "parsergolden8.txt");
  }
  public final void testParser9() throws Exception {
    runParseTest("parsertest9.js", "parsergolden9.txt");
  }
  public final void testParser10() throws Exception {
    runParseTest("parsertest10.js", "parsergolden10.txt");

    Iterator<Message> msgs = mq.getMessages().iterator();

    assertNextMessage(
        msgs,
        MessageType.SEMICOLON_INSERTED,
        "parsertest10.js:14+15");
    assertNextMessage(
        msgs,
        MessageType.SEMICOLON_INSERTED,
        "parsertest10.js:15+15");
    assertNextMessage(
        msgs,
        MessageType.SEMICOLON_INSERTED,
        "parsertest10.js:20+15");
    assertNextMessage(
        msgs,
        MessageType.SEMICOLON_INSERTED,
        "parsertest10.js:21+15");
    assertNextMessage(
        msgs,
        MessageType.SEMICOLON_INSERTED,
        "parsertest10.js:26+15");
    assertNextMessage(
        msgs,
        MessageType.SEMICOLON_INSERTED,
        "parsertest10.js:27+15");
    assertNextMessage(
        msgs,
        MessageType.SEMICOLON_INSERTED,
        "parsertest10.js:32+15");
    assertNextMessage(
        msgs,
        MessageType.SEMICOLON_INSERTED,
        "parsertest10.js:33+15");
    assertNextMessage(
        msgs,
        MessageType.UNRECOGNIZED_DIRECTIVE_IN_PROLOGUE,
        "parsertest10.js:52+3 - 15",
        MessagePart.Factory.valueOf("bogusburps"));

    assertFalse(msgs.hasNext());
  }
  public final void testParser11() throws Exception {
    runParseTest("parsertest11.js", "parsergolden11.txt");
  }

  public final void testParseTreeRendering1() throws Exception {
    runRenderTest("parsertest1.js", "rendergolden1.txt");
  }
  public final void testParseTreeRendering2() throws Exception {
    runRenderTest("parsertest2.js", "rendergolden2.txt");
  }
  public final void testParseTreeRendering3() throws Exception {
    runRenderTest("parsertest3.js", "rendergolden3.txt");
  }
  public final void testParseTreeRendering4() throws Exception {
    runRenderTest("parsertest4.js", "rendergolden4.txt");
  }
  public final void testParseTreeRendering5() throws Exception {
    runRenderTest("parsertest5.js", "rendergolden5.txt");
  }
  public final void testSecureParseTreeRendering6() throws Exception {
    runRenderTest("parsertest6.js", "rendergolden6.txt");

    // Since we're doing these checks for security, double check that someone
    // hasn't adjusted the golden file.
    String golden = Strings.lower(
         TestUtil.readResource(getClass(), "rendergolden6.txt"));
    assertFalse(golden.contains("]]>"));
    assertFalse(golden.contains("<!"));
    assertFalse(golden.contains("<script"));
    assertFalse(golden.contains("</script"));
  }
  public final void testParseTreeRendering7() throws Exception {
    runRenderTest("parsertest7.js", "rendergolden7.txt");
  }
  public final void testParseTreeRendering8() throws Exception {
    runRenderTest("parsertest8.js", "rendergolden8.txt");
  }
  public final void testParseTreeRendering9() throws Exception {
    runRenderTest("parsertest9.js", "rendergolden9.txt");
  }
  public final void testParseTreeRendering11() throws Exception {
    runRenderTest("parsertest11.js", "rendergolden11.txt");
  }

  public final void test12ctorParse() throws Exception {
    runParseTest("test12ctor.js", "test12ctor-parse.txt");
  }
  public final void test12ctorRender() throws Exception {
    runRenderTest("test12ctor.js", "test12ctor-render.txt");
  }

  public final void testThrowAsRestrictedProduction() throws Exception {
    try {
      js(fromString("throw \n new Error()"));
      fail("throw followed by newline should fail");
    } catch (ParseException ex) {
      assertEquals(MessageType.EXPECTED_TOKEN,
                   ex.getCajaMessage().getMessageType());
    }
    // But it should pass if there is a line-continuation
    js(fromString("throw \\\n new Error()"));
  }
  public final void testCommaOperatorInReturn() throws Exception {
    Block bl = js(fromString("return 1  \n  , 2;"));
    assertTrue("" + mq.getMessages(), mq.getMessages().isEmpty());
    assertEquals("{\n  return 1, 2;\n}", render(bl));
  }

  public final void testRenderKeywordsAsIdentifiers() throws Exception {
    for (Keyword k : Keyword.values()) {
      assertRenderKeywordAsIdentifier(k);
    }
  }

  public final void testParseKeywordsAsIdentifiers() {
    for (Keyword k : Keyword.values()) {
      assertParseKeywordAsIdentifier(k);
    }
  }

  public final void testDebuggerKeyword() {
    // The debugger keyword can appear in a statement context
    assertParseSucceeds("{ debugger; }");
    // but not in an expression context
    assertParseFails("(debugger);");
    assertMessage(
        MessageType.RESERVED_WORD_USED_AS_IDENTIFIER,
        MessageLevel.ERROR);
    assertParseFails("debugger();");
    assertMessage(
        MessageType.EXPECTED_TOKEN,
        MessageLevel.ERROR,
        MessagePart.Factory.valueOf(";"),
        MessagePart.Factory.valueOf("("));
    // or as an identifier.
    assertParseFails("var debugger;");
    assertMessage(
        MessageType.RESERVED_WORD_USED_AS_IDENTIFIER,
        MessageLevel.ERROR);
    assertParseFails("debugger: foo();");
    assertMessage(
        MessageType.EXPECTED_TOKEN,
        MessageLevel.ERROR,
        MessagePart.Factory.valueOf(";"),
        MessagePart.Factory.valueOf(":"));
  }

  public final void testOctalLiterals() throws Exception {
    assertEquals("10", render(jsExpr(fromString("012"))));
    assertMessage(MessageType.OCTAL_LITERAL, MessageLevel.LINT);

    mq.getMessages().clear();
    assertEquals("12", render(jsExpr(fromString("12"))));
    assertTrue("" + mq.getMessages(), mq.getMessages().isEmpty());

    mq.getMessages().clear();
    assertEquals("18.0", render(jsExpr(fromString("018"))));
    assertMessage(MessageType.OCTAL_LITERAL, MessageLevel.ERROR);

    mq.getMessages().clear();
    try {
      assertEquals("018i", render(jsExpr(fromString("018i"))));
      fail("numeric literal with disallowed letter suffix allowed");
    } catch (ParseException ex) {
      mq.getMessages().add(ex.getCajaMessage());
    }
    assertMessage(MessageType.INVALID_IDENTIFIER, MessageLevel.ERROR);

    mq.getMessages().clear();
    assertEquals("-10", render(jsExpr(fromString("-012"))));
    assertMessage(MessageType.OCTAL_LITERAL, MessageLevel.LINT);

    mq.getMessages().clear();
    try {
      assertEquals("12.34", render(jsExpr(fromString("012.34"))));
      fail("012.34 is not legal javascript.");
    } catch (ParseException ex) {
      // pass
    }

    mq.getMessages().clear();
    assertEquals("(10).toString()",
                 // If . is treated as part of 012 then semicolon insertion
                 // treats a method call as a function call.
                 render(jsExpr(fromString("012.\ntoString()"))));
    assertMessage(MessageType.OCTAL_LITERAL, MessageLevel.LINT);
  }

  public final void testIntegerPartIsOctal() {
    assertTrue(Parser.integerPartIsOctal("012"));
    assertTrue(Parser.integerPartIsOctal("0012"));
    assertTrue(Parser.integerPartIsOctal("012.34"));
    assertFalse(Parser.integerPartIsOctal("12"));
    assertFalse(Parser.integerPartIsOctal("12.34"));
    assertFalse(Parser.integerPartIsOctal("0x12"));
    assertFalse(Parser.integerPartIsOctal("0"));
    assertFalse(Parser.integerPartIsOctal("00"));
    assertFalse(Parser.integerPartIsOctal("0.01"));
    assertFalse(Parser.integerPartIsOctal("0.12"));
  }

  public final void testNUL() throws Exception {
    assertEquals("'\\x00'", render(jsExpr(fromString("'\0'"))));
  }

  private String expand(String template, String value) {
    // Use string replace rather than quasis to avoid invoking the parser when
    // creating tests for the parser
    return template.replace("@@", value);
  }

  private String unicodeMunge(String k) throws IOException {
    StringBuilder munged = new StringBuilder();
    munged.append(k, 0, k.length()-1);
    Escaping.unicodeEscape(k.charAt(k.length()-1), munged);
    return munged.toString();
  }

  public final void testUnicodeInKeywords() throws Exception {
    String[] templates = {
        "function @@ (a){}",
        "function foo(@@) {}",
        "function foo(a, @@) {}",
        "function foo(a, b) { @@(){}; }",
        "function foo(a, b) { @@: bar(){}; }"
    };
    for (Keyword k : Keyword.values()) {
      for (String template : templates) {
        String mungedKeyword = unicodeMunge(k.toString());
        String candidate = expand(template, mungedKeyword);
        try {
          js(fromString(candidate));
        } catch (Exception e) {
          assertTrue(e instanceof ParseException);
        }
        assertMessage(true, MessageType.RESERVED_WORD_USED_AS_IDENTIFIER,
                      MessageLevel.ERROR);
      }
    }
  }

  public final void testRenderingOfMalformedRegexSafe() {
    assertEquals(
        "(new (/./.constructor)('foo', 'iii'))",
        render(new RegexpLiteral(FilePosition.UNKNOWN, "/foo/iii")));
    assertEquals(
        "(new (/./.constructor)('', ''))",
        render(new RegexpLiteral(FilePosition.UNKNOWN, "//")));
    assertEquals(
        "(new (/./.constructor)('x', '\\''))",
        render(new RegexpLiteral(FilePosition.UNKNOWN, "/x/'")));
  }

  public final void testOutOfRangeLiterals() throws Exception {
    NumberLiteral l = (NumberLiteral) jsExpr(fromString("0x7fffffffffffffff"));
    assertMessage(
        MessageType.UNREPRESENTABLE_INTEGER_LITERAL, MessageLevel.WARNING);
    assertEquals(new Double(9223372036854776000d), l.getValue());
  }

  public final void testOutOfRangeLiterals2() throws Exception {
    jsExpr(fromString("99999999999999999999999"));
    assertMessage(
        MessageType.UNREPRESENTABLE_INTEGER_LITERAL, MessageLevel.WARNING);
  }

  public final void testEncodedLiterals() throws Exception {
    assertEquals("255",render(jsExpr(fromString("0xff"))));
    assertEquals(Long.toString(1L << 50),
        render(jsExpr(fromString("0x" + Long.toHexString(1L << 50)))));

    assertEquals("57",render(jsExpr(fromString("071"))));
    assertMessage(
        MessageType.OCTAL_LITERAL, MessageLevel.LINT);
    mq.getMessages().clear();

    jsExpr(fromString("0x" + Long.toHexString(1L << 51)));
    assertMessage(
        MessageType.UNREPRESENTABLE_INTEGER_LITERAL, MessageLevel.WARNING);
    mq.getMessages().clear();

    assertParseFails(
        "var o = { 1E111111111111111111111111111111111111111111111111111:123};"
        );
    assertMessage(
        MessageType.MALFORMED_NUMBER,
        MessageLevel.FATAL_ERROR);
  }

  public final void testRedundantEscapeSequences() throws Exception {
    // Should issue a warning if there is an escape sequence in a string where
    // the escaped character is not interpreted differently, and the escaped
    // character has a special meaning in a regular expression.

    jsExpr(fromString(" new RegExp('foo\\s+bar') "));
    assertMessage(
        MessageType.REDUNDANT_ESCAPE_SEQUENCE, MessageLevel.LINT,
        FilePosition.instance(is, 1, 13, 13, 11),
        MessagePart.Factory.valueOf("\\s"));
    mq.getMessages().clear();

    jsExpr(fromString(" new RegExp('foo\\\\s+bar') "));
    assertMessagesLessSevereThan(MessageLevel.LINT);
    mq.getMessages().clear();

    jsExpr(fromString(" '<\\/script>' "));
    assertMessagesLessSevereThan(MessageLevel.LINT);
    mq.getMessages().clear();

    jsExpr(fromString(" '\\v' "));
    assertMessage(MessageType.AMBIGUOUS_ESCAPE_SEQUENCE, MessageLevel.WARNING);
    mq.getMessages().clear();
  }

  public final void testUnnormalizedIdentifiers() {
    // Test that identifiers not normalized to Normal Form C (Unicode NFC)
    // result in a ParseException with a useful error message.
    // According to chapter 6 of ES5, "The [source] text is expected to
    // have been normalized to Unicode Normalized Form C (canonical
    // composition)."
    try {
      js(fromString("C\0327();"));  // Normalizes to \xC7
      fail("Unnormalized identifier parsed without incident");
    } catch (ParseException ex) {
      mq.getMessages().add(ex.getCajaMessage());
    }
    assertMessage(
        MessageType.INVALID_IDENTIFIER,
        MessageLevel.ERROR,
        MessagePart.Factory.valueOf("C\0327"));
  }

  /**
   * If conditionals are moved around, they can get in a configuration that
   * would be impossible to parse.
   */
  public final void testRenderingOfRebuiltConditionals() throws ParseException {
    assertEquals(
        render(js(fromString("if (foo) { if (bar);} else baz"))),
        render(stripBlocks(js(fromString("if (foo) { if (bar) {} } else baz"))))
        );
    assertEquals(
        render(js(fromString(
            "if (foo) { while (foo) if (bar) break; } else baz"))),
        render(stripBlocks(js(fromString(
            "if (foo) { while (foo) if (bar) break; } else baz"))))
        );
  }

  public final void testMissingSemis() throws ParseException {
    js(fromString("foo();"));
    assertTrue(mq.getMessages().isEmpty());

    js(fromString("foo(\n  42);"));
    assertTrue(mq.getMessages().isEmpty());

    js(fromString("foo\n(42);"));
    assertMessage(true, MessageType.MAYBE_MISSING_SEMI, MessageLevel.WARNING);
    assertTrue(mq.getMessages().isEmpty());

    js(fromString("foo[42];"));
    assertTrue(mq.getMessages().isEmpty());

    js(fromString("foo[\n  42];"));
    assertTrue(mq.getMessages().isEmpty());

    js(fromString("foo\n[42];"));
    assertMessage(true, MessageType.MAYBE_MISSING_SEMI, MessageLevel.WARNING);
    assertTrue(mq.getMessages().isEmpty());
  }

  public final void testDoWhileSemis() throws Exception {
    js(fromString("do {;} while (0)1"));
    assertMessage(true, MessageType.SEMICOLON_INSERTED, MessageLevel.LINT);
    js(fromString("do {;} while (0) 1"));
    assertMessage(true, MessageType.SEMICOLON_INSERTED, MessageLevel.LINT);
    js(fromString("do {;} while (0)\n1"));
    assertMessage(true, MessageType.SEMICOLON_INSERTED, MessageLevel.LINT);
    js(fromString("do {;} while (0);1"));
    assertTrue(mq.getMessages().isEmpty());
    assertRender("{do{;}while(0)1}", "{\n  do {; } while (0);\n  1;\n}");
    assertMinified("{do{;}while(0)1}", "{do{;}while(0);1}");
  }

  public final void testCommas() {
    try {
      js(fromString("[1 2]"));
    } catch (ParseException ex) {
      // pass
      return;
    }
    fail("Commas not required");
  }

  public final void testQuotedPropertyNames() throws ParseException {
    ObjectConstructor obj = (ObjectConstructor)
        jsExpr(fromString("{ notquoted: 0, 'quoted': 1 }"));
    assertFalse(obj.propertyWithName("notquoted").isPropertyNameQuoted());
    assertTrue(obj.propertyWithName("quoted").isPropertyNameQuoted());
  }

  private void assertParseKeywordAsIdentifier(Keyword k) {
    assertAllowKeywordPropertyAccessor(k);
    assertAllowKeywordPropertyDeclaration(k);
    assertRejectKeywordAsExpr(k);
    assertRejectKeywordInVarDecl(k);
  }

  private void assertAllowKeywordPropertyAccessor(Keyword k) {
    assertParseSucceeds(asLvalue("foo." + k));
    assertParseSucceeds(asRvalue("foo." + k));
    assertParseSucceeds(asLvalue("foo." + k + ".bar"));
    assertParseSucceeds("foo." + k + ".bar;");
  }

  private void assertAllowKeywordPropertyDeclaration(Keyword k) {
    assertParseSucceeds("({ " + k + " : 42 });");
  }

  private void assertRejectKeywordAsExpr(Keyword k) {
    assertParseKeyword(asLvalue(k.toString()), isValidLvalue(k));
    assertParseKeyword(asRvalue(k.toString()), isValidRvalue(k));
    assertParseKeyword(asLvalue(k + ".foo"), isValidRvalue(k));
    assertParseKeyword(asRvalue(k + ".foo"), isValidRvalue(k));
  }

  private void assertRejectKeywordInVarDecl(Keyword k) {
    assertParseFails("var " + k + ";");
    assertMessage(
        MessageType.RESERVED_WORD_USED_AS_IDENTIFIER,
        MessageLevel.ERROR);
    assertParseFails("var foo, " + k + ", bar;");
    assertMessage(
        MessageType.RESERVED_WORD_USED_AS_IDENTIFIER,
        MessageLevel.ERROR);
  }

  private void assertParseKeyword(String code, boolean shouldSucceed) {
    if (shouldSucceed) {
      assertParseSucceeds(code);
    } else {
      assertParseFails(code);
    }
  }

  private void assertParseSucceeds(String code) {
    mq.getMessages().clear();
    try {
      js(fromString(code));
    } catch (ParseException ex) {
      AssertionFailedError afe = new AssertionFailedError(code);
      afe.initCause(ex);
      throw afe;
    }
    try {
      assertNoErrors();
    } catch (AssertionFailedError e) {
      log("assertParseSucceeds", code);
      throw e;
    }
  }

  private void assertParseFails(String code) {
    mq.getMessages().clear();
    try {
      js(fromString(code));
    } catch (ParseException e) {
      e.toMessageQueue(mq);
    }
    for (Message msg : mq.getMessages()) {
      if (msg.getMessageLevel().compareTo(MessageLevel.ERROR) >= 0) { return; }
    }
    log("assertParseFails", code);
    fail("expected failure");
  }

  private static boolean isValidLvalue(Keyword k) {
    return Keyword.THIS == k;
  }

  private static boolean isValidRvalue(Keyword k) {
    return Keyword.THIS == k
        || Keyword.TRUE == k
        || Keyword.FALSE == k
        || Keyword.NULL == k;
  }

  private static String asLvalue(String expr) {
    return expr + " = 42;";
  }

  private static String asRvalue(String expr) {
    return "x = " + expr + ";";
  }

  private void assertNextMessage(Iterator<Message> msgs,
                                 MessageType type,
                                 String filePositionString,
                                 Object... otherMessageParts)
      throws Exception {
    assertTrue(msgs.hasNext());
    Message m = msgs.next();
    assertEquals(type, m.getMessageType());
    assertFilePosition(
        filePositionString,
        (FilePosition) m.getMessageParts().get(0), mc);
    for (int i = 0; i < otherMessageParts.length; i++) {
      assertEquals(otherMessageParts[i], m.getMessageParts().get(i + 1));
    }
  }

  private void assertRenderKeywordAsIdentifier(Keyword k) throws Exception {
    assertRenderKeywordPropertyAccessor(k);
    assertRenderKeywordPropertyDeclaration(k);
  }

  private void assertRenderKeywordPropertyAccessor(Keyword k)
      throws Exception {
    assertRender(
        "x." + k + " = 42;",
        "x[ '" + k + "' ] = 42");
    assertRender(
        "y = x." + k + ";",
        "y = x[ '" + k + "' ]");
    assertRender(
        "x." + k + ".z = 42;",
        "x[ '" + k + "' ].z = 42");
    assertRender(
        "y = x." + k + ".z;",
        "y = x[ '" + k + "' ].z");
  }

  private void assertRenderKeywordPropertyDeclaration(Keyword k)
      throws Exception {
    assertRender(
        "({" + k + ": 42});",
        "({ '" + k + "': 42 })");
  }

  private void assertRender(String code, String expectedRendering)
      throws Exception {
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = new JsPrettyPrinter(sb);
    RenderContext rc = new RenderContext(tc);
    js(fromString(code)).children().get(0).render(rc);
    tc.noMoreTokens();
    assertEquals(code, expectedRendering, sb.toString());
  }

  private void assertMinified(String code, String expectedRendering)
      throws Exception {
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = new JsMinimalPrinter(sb);
    RenderContext rc = new RenderContext(tc);
    js(fromString(code)).children().get(0).render(rc);
    tc.noMoreTokens();
    assertEquals(code, expectedRendering, sb.toString());
  }

  private void log(String testName, String code) {
    System.err.println();
    System.err.println("*** " + testName + ": " + code);
  }

  private void runRenderTest(String testFile, String goldenFile)
      throws Exception {
    Statement parseTree = js(fromResource(testFile));
    checkFilePositionInvariants(parseTree);

    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = new JsPrettyPrinter(sb);
    RenderContext rc = new RenderContext(tc);
    parseTree.render(rc);
    tc.noMoreTokens();
    sb.append('\n');

    String golden = TestUtil.readResource(getClass(), goldenFile);
    String actual = sb.toString();
    assertEquals(actual, golden, actual);
  }

  private void assertFilePosition(
      String golden, FilePosition actual, MessageContext mc)
      throws IOException {
    StringBuilder sb = new StringBuilder();
    actual.format(mc, sb);
    assertEquals(golden, sb.toString());
  }

  private void runParseTest(
      String testFile, String goldenFile, String ... errors)
      throws Exception {
    Statement parseTree = js(fromResource(testFile));
    assertCloneable(parseTree);
    checkFilePositionInvariants(parseTree);

    StringBuilder output = new StringBuilder();
    parseTree.format(mc, output);
    output.append('\n');

    // Check that parse tree matches.
    String golden = TestUtil.readResource(getClass(), goldenFile);
    assertEquals(golden, output.toString());

    // Clone the parse tree, and check that it, too, matches
    Statement cloneParseTree = (Statement) parseTree.clone();
    StringBuilder cloneOutput = new StringBuilder();
    cloneParseTree.format(mc, cloneOutput);
    cloneOutput.append('\n');
    assertEquals(golden, cloneOutput.toString());

    List<String> actualErrors = new ArrayList<String>();
    for (Message m : mq.getMessages()) {
      if (MessageLevel.ERROR.compareTo(m.getMessageLevel()) <= 0) {
        String error = m.toString();
        actualErrors.add(error.substring(error.indexOf(": ") + 2));
      }
    }

    List<String> expectedErrors = Arrays.asList(errors);
    MoreAsserts.assertListsEqual(expectedErrors, actualErrors);
  }

  private static void checkFilePositionInvariants(ParseTreeNode root) {
    checkFilePositionInvariants(AncestorChain.instance(root));
  }

  private static void checkFilePositionInvariants(AncestorChain<?> nChain) {
    ParseTreeNode n = nChain.node;
    String msg = n + " : " + n.getFilePosition();
    try {
      // require that n start on or after its previous sibling
      int indexInParent = nChain.parent != null
          ? nChain.parent.node.children().indexOf(nChain.node) : -1;
      ParseTreeNode prev = indexInParent > 0
          ? nChain.parent.node.children().get(indexInParent - 1) : null;
      if (prev != null) {
        if (prev instanceof Identifier && n instanceof FunctionConstructor
            && nChain.parent != null
            && nChain.parent.node instanceof FunctionDeclaration) {
          // Special case for FunctionDeclarations which look like this
          // FunctionDeclaration
          //   Identifier
          //   FunctionConstructor
          //     Identifier
          // with the FunctionConstructor having the same position as the
          // declaration which makes the identifier overlap with its sibling.
          assertEquals(msg, prev.getFilePosition(),
                       nChain.parent.cast(FunctionDeclaration.class).node
                       .getIdentifier().getFilePosition());
        } else {
          assertTrue(msg, (prev.getFilePosition().endCharInFile()
                           <= n.getFilePosition().startCharInFile()));
        }
      }
      // require that n encompass its children
      List<? extends ParseTreeNode> children = n.children();
      if (!children.isEmpty()) {
        ParseTreeNode first = children.get(0),
                       last = children.get(children.size() - 1);
        assertTrue(msg + " > " + first + " : " + first.getFilePosition(),
                   (first.getFilePosition().startCharInFile()
                    >= n.getFilePosition().startCharInFile()));
        assertTrue(msg + " < " + last + " : " + last.getFilePosition(),
                   (last.getFilePosition().endCharInFile()
                    <= n.getFilePosition().endCharInFile()));
      }

      for (ParseTreeNode c : children) {
        checkFilePositionInvariants(AncestorChain.instance(nChain, c));
      }
    } catch (RuntimeException ex) {
      throw new SomethingWidgyHappenedError(msg, ex);
    }
  }

  private static Statement stripBlocks(Block b) {
    b.acceptPostOrder(new Visitor() {
      public boolean visit(AncestorChain<?> chain) {
        if (chain.node instanceof Block && chain.parent != null) {
          Block b = chain.cast(Block.class).node;
          List<? extends Statement> children = b.children();
          switch (children.size()) {
            case 0:
              chain.parent.cast(MutableParseTreeNode.class).node.replaceChild(
                  new Noop(b.getFilePosition()), b);
              break;
            case 1:
              chain.parent.cast(MutableParseTreeNode.class).node.replaceChild(
                  children.get(0), b);
              break;
          }
        }
        return true;
      }
    }, null);
    return b;
  }
}
