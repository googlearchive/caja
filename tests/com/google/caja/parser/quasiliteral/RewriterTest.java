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

import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.NumberLiteral;

import java.util.Map;

/**
 * @author ihab.awad@gmail.com
 */
public class RewriterTest extends RewriterTestCase {

  /**
   * Demonstrates a failure mode where the rewriter returns the exact input it
   * was given.
   */
  private static class ReturnExactInputRewriter extends Rewriter {
    public ReturnExactInputRewriter(final boolean returnExactInput) {
      super(true, true);

      addRule(new Rule() {
        @Override
        @RuleDescription(
            name="setTaint",
            synopsis="",
            reason="")
        public ParseTreeNode fire(ParseTreeNode node,
                                  Scope scope,
                                  MessageQueue mq) {
          if (returnExactInput) { return node; }
          node.getAttributes().remove(ParseTreeNode.TAINTED);
          for (ParseTreeNode c : node.children()) {
            getRewriter().expand(c, scope, mq);
          }
          return node;
      }});
    }
  }

  /**
   * Demonstrates a failure mode where the rewriter returns a newly constructed
   * quasi substitution as the result of a match, but fails to recursively
   * expand the values plugged into the quasi substitution.
   */
  private static class ReturnUnexpandedRewriter extends Rewriter {
    public ReturnUnexpandedRewriter(final boolean returnUnexpanded) {
      super(true, true);

      // Top-level rule that matches an addition expression
      addRule(new Rule() {
        @Override
        @RuleDescription(
            name="quasiSubst",
            matches="@x + @y",
            substitutes="@x - @y",
            synopsis="",
            reason="")
        public ParseTreeNode fire(ParseTreeNode node,
                                  Scope scope,
                                  MessageQueue mq) {
          Map<String, ParseTreeNode> m = match(node);

          if (m != null) {
            ParseTreeNode x = m.get("x");
            ParseTreeNode y = m.get("y");

            if (!returnUnexpanded) {
              x = expandAll(x, scope, mq);
              y = expandAll(y, scope, mq);
            }

            return substV(
                "x", x,
                "y", y);
          }

          return NONE;
        }});

      // Recursive rule that matches a number and clears its taint
      addRule(new Rule() {
        @Override
        @RuleDescription(
            name="numberLiteral",
            synopsis="",
            reason="")
        public ParseTreeNode fire(ParseTreeNode node,
                                  Scope scope,
                                  MessageQueue mq) {
          if (node instanceof NumberLiteral) {
            node.getAttributes().remove(ParseTreeNode.TAINTED);
            return node;
          }
          return NONE;
        }});
    }
  }


  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  public final void testReturningExactInputIsCaught() throws Exception {
    setRewriter(new ReturnExactInputRewriter(true));
    checkAddsMessage(
        js(fromString("3;")),
        RewriterMessageType.UNSEEN_NODE_LEFT_OVER,
        MessageLevel.FATAL_ERROR);
    setRewriter(new ReturnExactInputRewriter(false));
    checkSucceeds(
        js(fromString("3;")),
        null);
  }

  public final void testReturningUnexpandedIsCaught() throws Exception {
    setRewriter(new ReturnUnexpandedRewriter(true));
    checkAddsMessage(
        makeSimpleAdditionExpr(),
        RewriterMessageType.UNSEEN_NODE_LEFT_OVER,
        MessageLevel.FATAL_ERROR);
    setRewriter(new ReturnUnexpandedRewriter(false));
    checkSucceeds(
        makeSimpleAdditionExpr(),
        null);
  }

  public final void testUnmatchedThrowsError() throws Exception {
    setRewriter(new Rewriter(true, true) {});  // No rules
    checkAddsMessage(
        js(fromString("3;")),
        RewriterMessageType.UNMATCHED_NODE_LEFT_OVER,
        MessageLevel.FATAL_ERROR);
  }

  private ParseTreeNode makeSimpleAdditionExpr() throws Exception {
    return jsExpr(fromString("1 + 2"));
  }

  @Override
  protected Object executePlain(String program) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Object rewriteAndExecute(String pre, String program, String post) {
    throw new UnsupportedOperationException();
  }
}
