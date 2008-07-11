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

package com.google.caja.parser.quasiliteral;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.SyntheticNodes;
import static com.google.caja.parser.quasiliteral.QuasiBuilder.substV;
import com.google.caja.parser.js.*;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.TestUtil;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageTypeInt;

import junit.framework.AssertionFailedError;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;

/**
 * @author ihab.awad@gmail.com
 */
public abstract class RewriterTestCase extends CajaTestCase {

  /**
   * Create a new Rewriter for use by this test case.
   */
  protected abstract Rewriter newRewriter();

  /**
   * Given some code, execute it without rewriting and return the value of the
   * last expression in the code.
   */
  protected abstract Object executePlain(String program) throws IOException, ParseException;

  /**
   * Given some code, rewrite it, then execute it in the proper context for the rewritten
   * version and return the value of the last expression in the original code.
   *
   * @param pre a prefix program fragment to be executed plain.
   * @param program a program fragment to be rewritten.
   * @param post a postfix program fragment to be executed plain.
   */
  protected abstract Object rewriteAndExecute(String pre, String program, String post) 
      throws IOException, ParseException;

  protected Object rewriteAndExecute(String program) throws IOException, ParseException {
    return rewriteAndExecute(";", program, ";");
  }

  protected void setSynthetic(ParseTreeNode n) {
    SyntheticNodes.s(n);
  }

  protected void setTreeSynthetic(ParseTreeNode n) {
    setSynthetic(n);
    for (ParseTreeNode child : n.children()) {
      setTreeSynthetic(child);
    }
  }

  // TODO(ihab.awad): Refactor tests to use checkAddsMessage(...) instead
  protected void checkFails(String input, String error) throws Exception {
    mq.getMessages().clear();
    ParseTreeNode expanded = newRewriter().expand(js(fromString(input)), mq);

    assertFalse(
        "Expected error, found none: " + error,
        mq.getMessages().isEmpty());

    StringBuilder messageText = new StringBuilder();
    for (Message m : mq.getMessages()) {
      m.format(mc, messageText);
      messageText.append("\n");
    }
    assertTrue(
        "Messages do not contain \"" + error + "\": " + messageText.toString(),
        messageText.toString().contains(error));
  }

  protected void checkSucceeds(ParseTreeNode inputNode,
                               ParseTreeNode expectedResultNode)
      throws Exception {
    checkSucceeds(inputNode, expectedResultNode, MessageLevel.WARNING);
  }

  protected void checkSucceeds(
      ParseTreeNode inputNode,
      ParseTreeNode expectedResultNode,
      MessageLevel highest)
      throws Exception {
    mq.getMessages().clear();
    ParseTreeNode actualResultNode = newRewriter().expand(inputNode, mq);
    for (Message m : mq.getMessages()) {
      if (m.getMessageLevel().compareTo(highest) >= 0) {
        fail(m.toString());
      }
    }
    if (expectedResultNode != null) {
      // Test that the source code-like renderings are identical. This will catch any
      // obvious differences between expected and actual.
      assertEquals(render(expectedResultNode), render(actualResultNode));
      // Then, for good measure, test that the S-expression-like formatted representations
      // are also identical. This will catch any differences in tree topology that somehow
      // do not appear in the source code representation (usually due to programming errors).
      assertEquals(
          TestUtil.format(expectedResultNode),
          TestUtil.format(actualResultNode));
    }
  }

  protected void assertMessageNotPresent(String src, MessageTypeInt type, MessageLevel level)
      throws Exception {
    checkDoesNotAddMessage(js(fromString(src)), type, level);
  }

  protected void assertMessageNotPresent(String src, MessageTypeInt type) throws Exception {
    checkDoesNotAddMessage(js(fromString(src)), type);
  }

  private void checkDoesNotAddMessage(
      ParseTreeNode inputNode,
      MessageTypeInt type)  {
    mq.getMessages().clear();
    ParseTreeNode actualResultNode = newRewriter().expand(inputNode, mq);
    if (containsConsistentMessage(mq.getMessages(),type)) {
      fail("Unexpected add message of type " + type);
    }
  }

  protected void checkDoesNotAddMessage(
        ParseTreeNode inputNode,
        MessageTypeInt type,
        MessageLevel level)  {
    mq.getMessages().clear();
    ParseTreeNode actualResultNode = newRewriter().expand(inputNode, mq);
    if (containsConsistentMessage(mq.getMessages(),type, level)) {
      fail("Unexpected add message of type " + type + " and level " + level);
    }
  }

  // TODO(ihab.awad): Change dependents to use checkAddsMessage and just call js(fromString("..."))
  protected void assertAddsMessage(String src, MessageTypeInt type, MessageLevel level)
      throws Exception {
    checkAddsMessage(js(fromString(src)), type, level);
  }

  protected void checkAddsMessage(
        ParseTreeNode inputNode,
        MessageTypeInt type)  {
    checkAddsMessage(inputNode, type, type.getLevel());
  }

  protected void checkAddsMessage(
        ParseTreeNode inputNode,
        MessageTypeInt type,
        MessageLevel level)  {
    mq.getMessages().clear();
    ParseTreeNode actualResultNode = newRewriter().expand(inputNode, mq);
    if (!containsConsistentMessage(mq.getMessages(), type, level)) {
      fail("Failed to add message of type " + type + " and level " + level);
    }
  }

  protected boolean containsConsistentMessage(List<Message> list, MessageTypeInt type) {
    for (Message m : list) {
      System.out.println("**" + m.getMessageType() + "|" + m.getMessageLevel());
      if (m.getMessageType().equals(type)) {
        return true;
      }
    }
    return false;
  }

  protected boolean containsConsistentMessage(List<Message> list, MessageTypeInt type, MessageLevel level) {
    for (Message m : list) {
      System.out.println("**" + m.getMessageType() + "|" + m.getMessageLevel());
      if ( m.getMessageType().equals(type) && m.getMessageLevel().equals(level) ) {
        return true;
      }
    }
    return false;
  }

  protected void checkSucceeds(String input, String expectedResult) throws Exception {
    checkSucceeds(js(fromString(input)), js(fromString(expectedResult)));
  }

  protected void checkSucceeds(CharProducer cp) throws Exception {
    checkSucceeds(js(cp), null);
  }

  public void testAssertConsistent() throws Exception {
    try {
      // A value that cannot be consistent across invocations.
      assertConsistent("({})");
      fail("assertConsistent not working");
    } catch (AssertionFailedError e) {
      // Pass
    }
  }

  /**
   * Asserts that the given caja code produces the same value both cajoled and
   * uncajoled.
   *
   * @param caja executed in the context of asserts.js for its value.  The
   *    value is computed from the last statement in caja.
   */
  protected void assertConsistent(String caja)
      throws IOException, ParseException {
    assertConsistent(null, caja);
  }

  private void assertConsistent(String message, String caja)
      throws IOException, ParseException {
    Object plainResult = executePlain(caja);
    Object rewrittenResult = rewriteAndExecute(caja);
    System.err.println(
        "Results: "
        + "plain=<" + plainResult + "> "
        + "rewritten=<" + rewrittenResult + "> "
        + "for " + getName());
    assertEquals(message, plainResult, rewrittenResult);
  }

  protected <T extends ParseTreeNode> T replaceLastStatementWithEmit(
      T node, String lValueExprString) throws ParseException {
    if (node instanceof ExpressionStmt) {
      ParseTreeNode lValueExpr =
          js(fromString(lValueExprString))  // a Block
          .children().get(0)                // an ExpressionStmt
          .children().get(0);               // an Expression
      ExpressionStmt es = (ExpressionStmt) node;
      Expression e = es.getExpression();
      Operation emitter = (Operation)substV(
          "@lValueExpr = @e;",
          "lValueExpr", syntheticTree(lValueExpr),
          "e", e);
      es.replaceChild(emitter, e);
    } else {
      List<? extends ParseTreeNode> children = node.children();
      if (!children.isEmpty()) {
        replaceLastStatementWithEmit(
            children.get(children.size() - 1), lValueExprString);
      }
    }
    return node;
  }

  private <T extends ParseTreeNode> T syntheticTree(T node) {
    for (ParseTreeNode c : node.children()) { setTreeSynthetic(c); }
    return SyntheticNodes.s(node);
  }

  protected ParseTreeNode rewriteStatements(Statement... nodes) {
    return newRewriter().expand(new Block(Arrays.asList(nodes)), mq);
  }
}
