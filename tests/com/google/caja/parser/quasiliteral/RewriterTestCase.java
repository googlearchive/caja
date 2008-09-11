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
import com.google.caja.parser.ParseTreeNodeContainer;

import static com.google.caja.parser.quasiliteral.QuasiBuilder.substV;

import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.SyntheticNodes;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.TestUtil;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageTypeInt;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;

/**
 * @author ihab.awad@gmail.com
 */
public abstract class RewriterTestCase extends CajaTestCase {
  protected Rewriter rewriter = null;

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

  // TODO(ihab.awad): Refactor tests to use checkAddsMessage(...) instead
  protected void checkFails(String input, String error) throws Exception {
    mq.getMessages().clear();
    getRewriter().expand(new Block(Arrays.asList(js(fromString(input, is)))), mq);

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
    ParseTreeNode actualResultNode = getRewriter().expand(inputNode, mq);
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
    getRewriter().expand(inputNode, mq);
    if (containsConsistentMessage(mq.getMessages(),type)) {
      fail("Unexpected add message of type " + type);
    }
  }

  protected void checkDoesNotAddMessage(
        ParseTreeNode inputNode,
        MessageTypeInt type,
        MessageLevel level)  {
    mq.getMessages().clear();
    getRewriter().expand(inputNode, mq);
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
    getRewriter().expand(inputNode, mq);
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

  protected boolean containsConsistentMessage(
      List<Message> list,
      MessageTypeInt type,
      MessageLevel level) {
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

  protected final <T extends ParseTreeNode> T syntheticTree(T node) {
    for (ParseTreeNode c : node.children()) { syntheticTree(c); }
    return makeSynthetic(node);
  }

  protected final <T extends ParseTreeNode> T makeSynthetic(T node) {
    SyntheticNodes.s(node);
    return node;
  }

  protected ParseTreeNode rewriteStatements(Statement... nodes) {
    return getRewriter().expand(new Block(Arrays.asList(nodes)), mq);
  }

  protected Rewriter getRewriter() {
    return rewriter;
  }

  protected void setRewriter(Rewriter r) {
    rewriter = r;
  }

  protected ParseTreeNode emulateIE6FunctionConstructors(ParseTreeNode node) {
    Rewriter w = new Rewriter(false) {};
    w.addRule(new Rule() {
      @Override
      @RuleDescription(
          name="blockScope",
          reason="Set up the root scope and handle block scope statements",
          synopsis="")
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof Block) {
          Scope s2;
          if (scope == null) {
            s2 = Scope.fromProgram((Block) node, mq);
          } else {
            s2 = Scope.fromPlainBlock(scope);
          }
          return QuasiBuilder.substV(
              "@startStmts*; @body*;",
              "startStmts", new ParseTreeNodeContainer(s2.getStartStatements()),
              "body", expandAll(
                  new ParseTreeNodeContainer(node.children()), s2, mq));
        }
        return NONE;
      }
    });
    w.addRule(new Rule() {
      @Override
      @RuleDescription(
          name="fnDeclarations",
          reason="function declarations contain function constructors but don't"
              + " have the same discrepencies on IE 6 as function constructors",
          synopsis="")
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof FunctionDeclaration) {
          FunctionDeclaration decl = ((FunctionDeclaration) node);
          FunctionConstructor ctor = decl.getInitializer();

          Scope s2 = Scope.fromFunctionConstructor(scope, ctor);
          FunctionConstructor rewritten
              = (FunctionConstructor) QuasiBuilder.substV(
                  "function @ident(@formals*) { @fh*; @stmts*; @body*; }",
                  "ident", ctor.getIdentifier(),
                  "formals", expandAll(
                      new ParseTreeNodeContainer(ctor.getParams()), s2, mq),
                  "fh", getFunctionHeadDeclarations(s2),
                  "stmts", new ParseTreeNodeContainer(s2.getStartStatements()),
                  "body", expandAll(
                      new ParseTreeNodeContainer(ctor.getBody().children()),
                      s2, mq)
                  );
          return new FunctionDeclaration(rewritten.getIdentifier(), rewritten);
        }
        return NONE;
      }
    });
    w.addRule(new Rule() {
      @Override
      @RuleDescription(
          name="ie6functions",
          reason="simulate IE 6's broken scoping of function constructors as "
              + "described in JScript Deviations Section 2.3",
          synopsis="")
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        if (node instanceof FunctionConstructor) {
          FunctionConstructor ctor = (FunctionConstructor) node;
          Scope s2 = Scope.fromFunctionConstructor(scope, ctor);
          if (ctor.getIdentifierName() == null) {
            return expandAll(node, s2, mq);
          }
          Identifier ident = ctor.getIdentifier();
          Reference identRef = new Reference(ident);
          identRef.setFilePosition(ident.getFilePosition());
          scope.addStartOfBlockStatement(new Declaration(ident, identRef));
          return QuasiBuilder.substV(
              "(@var = function @ident(@formals*) { @fh*; @stmts*; @body*; })",
              "var", identRef,
              "ident", ident,
              "formals", new ParseTreeNodeContainer(ctor.getParams()),
              "fh", getFunctionHeadDeclarations(s2),
              "stmts", new ParseTreeNodeContainer(s2.getStartStatements()),
              "body", expandAll(
                  new ParseTreeNodeContainer(ctor.getBody().children()),
                  s2, mq)
              );
        }
        return NONE;
      }
    });
    w.addRule(new Rule() {
      @Override
      @RuleDescription(
          name="catchAll",
          reason="Handles non function constructors.",
          synopsis="")
      public ParseTreeNode fire(
          ParseTreeNode node, Scope scope, MessageQueue mq) {
        return expandAll(node, scope, mq);
      }
    });
    return w.expand(node, mq);
  }
}
