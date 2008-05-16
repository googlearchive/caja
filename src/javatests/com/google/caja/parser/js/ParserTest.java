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

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.Keyword;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.MoreAsserts;
import com.google.caja.util.TestUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class ParserTest extends CajaTestCase {
  // TODO(mikesamuel): better comment each of the test input files.
  // What is each one supposed to test.

  public void testParser() throws Exception {
    runParseTest("parsertest1.js", "parsergolden1.txt",
                 "Reserved word else used as an identifier");

    // Check warnings on message queue.
    Iterator<Message> msgs = mq.getMessages().iterator();
    assertTrue(msgs.hasNext());
    Message m1 = msgs.next();
    assertEquals(MessageType.RESERVED_WORD_USED_AS_IDENTIFIER,
                 m1.getMessageType());
    assertFilePosition("parsertest1.js:11+29 - 33",
                       (FilePosition) m1.getMessageParts().get(0), mc);
    assertEquals(Keyword.ELSE, m1.getMessageParts().get(1));
    assertTrue(msgs.hasNext());
    Message m2 = msgs.next();
    assertEquals(MessageType.NOT_IE, m2.getMessageType());
    assertFilePosition("parsertest1.js:35+7 - 8",
                       (FilePosition) m2.getMessageParts().get(0), mc);
    assertTrue(!msgs.hasNext());
  }
  public void testParser2() throws Exception {
    runParseTest("parsertest2.js", "parsergolden2.txt");

    Iterator<Message> msgs = mq.getMessages().iterator();
    assertTrue(msgs.hasNext());
    Message m1 = msgs.next();
    assertEquals(MessageType.SEMICOLON_INSERTED, m1.getMessageType());
    assertFilePosition("parsertest2.js:4+3",
                       (FilePosition) m1.getMessageParts().get(0), mc);
    assertTrue(!msgs.hasNext());
  }
  public void testParser3() throws Exception {
    runParseTest("parsertest3.js", "parsergolden3.txt");
    assertTrue(mq.getMessages().isEmpty());
  }
  public void testParser5() throws Exception {
    runParseTest("parsertest5.js", "parsergolden5.txt");
  }
  public void testParser7() throws Exception {
    runParseTest("parsertest7.js", "parsergolden7.txt");
  }
  public void testParser8() throws Exception {
    runParseTest("parsertest8.js", "parsergolden8.txt");
  }

  public void testParseTreeRendering1() throws Exception {
    runRenderTest("parsertest1.js", "rendergolden1.txt", false);
  }
  public void testParseTreeRendering2() throws Exception {
    runRenderTest("parsertest2.js", "rendergolden2.txt", false);
  }
  public void testParseTreeRendering3() throws Exception {
    runRenderTest("parsertest3.js", "rendergolden3.txt", false);
  }
  public void testParseTreeRendering4() throws Exception {
    runRenderTest("parsertest4.js", "rendergolden4.txt", false);
  }
  public void testParseTreeRendering5() throws Exception {
    runRenderTest("parsertest5.js", "rendergolden5.txt", false);
  }
  public void testSecureParseTreeRendering6() throws Exception {
    runRenderTest("parsertest6.js", "rendergolden6.txt", true);

    // Since we're doing these checks for security, double check that someone
    // hasn't adjusted the golden file.
    String golden = TestUtil.readResource(getClass(), "rendergolden6.txt")
        .toLowerCase();
    assertFalse(golden.contains("]]>"));
    assertFalse(golden.contains("<!"));
    assertFalse(golden.contains("<script"));
    assertFalse(golden.contains("</script"));
  }
  public void testParseTreeRendering7() throws Exception {
    runRenderTest("parsertest7.js", "rendergolden7.txt", false);
  }
  public void testParseTreeRendering8() throws Exception {
    runRenderTest("parsertest8.js", "rendergolden8.txt", true);
  }

  public void testRenderKeywordsAsIdentifiers() throws Exception {
    for (Keyword k : Keyword.values()) {
      assertRenderKeywordAsIdentifier(k);
    }
  }

  public void testParseKeywordsAsIdentifiers() throws Exception {
    for (Keyword k : Keyword.values()) {
      assertParseKeywordAsIdentifier(k);
    }
  }

  private void assertParseKeywordAsIdentifier(Keyword k) throws Exception {
    assertAllowKeywordPropertyAccessor(k);
    assertAllowKeywordPropertyDeclaration(k);
    assertRejectKeywordAsExpr(k);
    assertRejectKeywordInVarDecl(k);
  }

  private void assertAllowKeywordPropertyAccessor(Keyword k)
      throws Exception {
    assertParseSucceeds(asLvalue("foo." + k));
    assertParseSucceeds(asRvalue("foo." + k));
    assertParseSucceeds(asLvalue("foo." + k + ".bar"));
    assertParseSucceeds("foo." + k + ".bar");
  }

  private void assertAllowKeywordPropertyDeclaration(Keyword k)
      throws Exception {
    assertParseSucceeds("({ " + k + " : 42 });");
  }

  private void assertRejectKeywordAsExpr(Keyword k)
      throws Exception {
    assertParse(asLvalue(k.toString()), isValidLvalue(k));
    assertParse(asRvalue(k.toString()), isValidRvalue(k));
    assertParse(asLvalue(k + ".foo"), isValidRvalue(k));
    assertParse(asRvalue(k + ".foo"), isValidRvalue(k));
  }

  private void assertRejectKeywordInVarDecl(Keyword k)
      throws Exception {
    assertParseFails("var " + k + ";");
    assertParseFails("var foo, " + k + ", bar;");
  }

  private void assertParse(String code, boolean shouldSucceed)
      throws Exception {
    if (shouldSucceed) assertParseSucceeds(code);
    else assertParseFails(code);
  }

  private void assertParseSucceeds(String code) throws Exception {
    log("assertParseSucceeds", code);
    mq.getMessages().clear();
    js(fromString(code));
    assertNoErrors();
  }

  private void assertParseFails(String code) throws Exception {
    log("assertParseFails", code);
    mq.getMessages().clear();
    try {
      js(fromString(code));
      assertMessage(
          MessageType.RESERVED_WORD_USED_AS_IDENTIFIER,
          MessageLevel.ERROR);
    } catch (ParseException e) {
      // Some reserved word usages confuse the parser and cause an exception,
      // not just a message queue error. We consider this a successful failure
      // to parse erroneous code.
      e.printStackTrace(System.err);
    }
  }

  private boolean isValidLvalue(Keyword k) {
    return Keyword.THIS == k;
  }

  private boolean isValidRvalue(Keyword k) {
    return Keyword.THIS == k
        || Keyword.TRUE == k
        || Keyword.FALSE == k
        || Keyword.NULL == k;
  }

  private String asLvalue(String expr) {
    return expr + " = 42;";
  }

  private String asRvalue(String expr) {
    return "x = " + expr + ";";
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
        "({\n"
         + "   '" + k + "': 42\n"
         + " })");
  }

  private void assertRender(String code, String expectedRendering)
      throws Exception {
    log("assertRender", code);
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = new JsPrettyPrinter(sb, null);
    RenderContext rc = new RenderContext(mc, true, tc);
    js(fromString(code)).children().get(0).render(rc);
    assertEquals(expectedRendering, sb.toString());
  }

  private void log(String testName, String code) {
    System.err.println();
    System.err.println("*** " + testName + ": " + code);
  }

  private void runRenderTest(
      String testFile, String goldenFile, boolean paranoid)
      throws Exception {
    Statement parseTree = js(fromResource(testFile));
    checkFilePositionInvariants(parseTree);

    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = new JsPrettyPrinter(sb, null);
    RenderContext rc = new RenderContext(mc, paranoid, tc);
    parseTree.render(rc);
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
    checkFilePositionInvariants(parseTree);

    StringBuilder output = new StringBuilder();
    parseTree.format(mc, output);

    // Check that parse tree matches.
    String golden = TestUtil.readResource(getClass(), goldenFile);
    assertEquals(golden, output.toString());

    // clone the parse tree, and check that it, too, matches
    Statement cloneParseTree = (Statement)parseTree.clone();
    StringBuilder cloneOutput = new StringBuilder();
    cloneParseTree.format(mc, cloneOutput);
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

  public static void checkFilePositionInvariants(ParseTreeNode root) {
    checkFilePositionInvariants(AncestorChain.instance(root));
  }

  private static void checkFilePositionInvariants(AncestorChain<?> nChain) {
    ParseTreeNode n = nChain.node;
    String msg = n + " : " + n.getFilePosition();
    try {
      // require that n start on or after its previous sibling
      ParseTreeNode prev = nChain.getPrevSibling();
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
        assertTrue(msg, (first.getFilePosition().startCharInFile()
                         >= n.getFilePosition().startCharInFile()));
        assertTrue(msg, (last.getFilePosition().endCharInFile()
                         <= n.getFilePosition().endCharInFile()));
      }

      for (ParseTreeNode c : children) {
        checkFilePositionInvariants(
            new AncestorChain<ParseTreeNode>(nChain, c));
      }
    } catch (RuntimeException ex) {
      throw new RuntimeException(msg, ex);
    }
  }
}
