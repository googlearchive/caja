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

/**
 * @author ihab.awad@gmail.com
 */
public class RewriterTest extends RewriterTestCase {
  private boolean taintMode;

  public void testTainting() throws Exception {
    taintMode = true;
    checkAddsMessage(
        js(fromString("3;")),
        RewriterMessageType.UNSEEN_NODE_LEFT_OVER,
        MessageLevel.FATAL_ERROR);
    taintMode = false;
    checkSucceeds(
        js(fromString("3;")),
        null);
  }

  @Override
  protected Rewriter newRewriter() {
    return new Rewriter(true) {{
      addRule(new Rule () {
        @Override
        @RuleDescription(
            name="setTaint",
            synopsis="Ensures that the result is tainted",
            reason="Test that Rewriter tainting check works")
        public ParseTreeNode fire(ParseTreeNode node, Scope scope, MessageQueue mq) {
          if (taintMode) { return node; }
          node.getAttributes().remove(ParseTreeNode.TAINTED);
          for (ParseTreeNode c : node.children()) {
            getRewriter().expand(c, scope, mq);
          }
          return node;
        }
      });
    }};
  }

  @Override
  protected Object executePlain(String program) {
    throw new UnsupportedOperationException("Implemented in subclasses");
  }

  @Override
  protected Object rewriteAndExecute(String pre, String program, String post) {
    throw new UnsupportedOperationException("Implemented in subclasses");
  }
}
