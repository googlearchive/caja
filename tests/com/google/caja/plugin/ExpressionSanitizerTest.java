// Copyright (C) 2006 Google Inc.
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

package com.google.caja.plugin;

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.quasiliteral.Rewriter;
import com.google.caja.parser.quasiliteral.Rule;
import com.google.caja.parser.quasiliteral.Scope;
import com.google.caja.parser.quasiliteral.RuleDescription;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Identifier;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.util.CajaTestCase;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public class ExpressionSanitizerTest extends CajaTestCase {
  private PluginMeta meta;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    meta = new PluginMeta();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    meta = null;
  }

  public void testBasicRewriting() throws Exception {
    assertSanitize(
        "g[0];",
        "var g = ___.readImport(IMPORTS___, 'g');"
        + "___.readPub(g, 0);");
  }

  public void testNoSpuriousRewriteErrorFound() throws Exception {
    newPassThruSanitizer().sanitize(ac(new Identifier("x")));
    assertFalse(mq.hasMessageAtLevel(MessageLevel.FATAL_ERROR));
  }

  public void testRewriteErrorIsDetected() throws Exception {
    newPassThruSanitizer().sanitize(ac(new Identifier("x__")));  
    assertTrue(mq.hasMessageAtLevel(MessageLevel.FATAL_ERROR));
  }

  public void testNonAsciiIsDetected() throws Exception {
    newPassThruSanitizer().sanitize(ac(new Identifier("\u00e6")));  
    assertTrue(mq.hasMessageAtLevel(MessageLevel.FATAL_ERROR));
  }

  private ExpressionSanitizerCaja newPassThruSanitizer() throws Exception {
    return new ExpressionSanitizerCaja(mq, meta) {
      @Override
      protected Rewriter newCajaRewriter() {
        return new Rewriter(true) {{
          addRule(new Rule() {
            @Override
            @RuleDescription(
                name="passthru",
                synopsis="Pass through input unchanged",
                reason="Dummy rule for testing")
            public ParseTreeNode fire(
                ParseTreeNode node, Scope scope, MessageQueue mq) {
              return node.clone();
            }
          });
        }};
      }
    };
  }

  private void assertSanitize(String input, String golden)
      throws Exception {
    Block inputNode = js(fromString(input));
    assertTrue(new ExpressionSanitizerCaja(mq, meta).sanitize(ac(inputNode)));
    String inputCmp = render(inputNode);

    String goldenCmp = render(js(fromString(golden)));

    assertEquals(goldenCmp, inputCmp);
    assertFalse(mq.hasMessageAtLevel(MessageLevel.WARNING));
  }

  private static <T extends ParseTreeNode> AncestorChain<T> ac(T node) {
    return new AncestorChain<T>(node);
  }
}
