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

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.quasiliteral.ModuleManager;
import com.google.caja.parser.quasiliteral.Rewriter;
import com.google.caja.parser.quasiliteral.Rule;
import com.google.caja.parser.quasiliteral.Scope;
import com.google.caja.parser.quasiliteral.RuleDescription;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Identifier;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.TestBuildInfo;
import com.google.caja.util.CajaTestCase;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public class ExpressionSanitizerTest extends CajaTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public final void testBasicRewriting() throws Exception {
    assertSanitize(
        "  'use strict';"
        + "g[i];",
        "var dis___ = IMPORTS___;"
        + "(IMPORTS___.g_v___? IMPORTS___.g: ___.ri(IMPORTS___, 'g'))"
        + ".v___(IMPORTS___.i_v___? IMPORTS___.i: ___.ri(IMPORTS___, 'i'));");
  }

  public final void testNoSpuriousRewriteErrorFound() {
    newPassThruSanitizer().sanitize(
        new Identifier(FilePosition.UNKNOWN, "x"));
    assertFalse(mq.hasMessageAtLevel(MessageLevel.FATAL_ERROR));
  }

  public final void testRewriteErrorIsDetected() {
    newPassThruSanitizer().sanitize(
        new Identifier(FilePosition.UNKNOWN, "x__"));
    assertTrue(mq.hasMessageAtLevel(MessageLevel.FATAL_ERROR));
  }

  public final void testNonAsciiIsDetected() {
    newPassThruSanitizer().sanitize(
        new Identifier(FilePosition.UNKNOWN, "\u00e6"));
    assertTrue(mq.hasMessageAtLevel(MessageLevel.FATAL_ERROR));
  }

  private ExpressionSanitizerCaja newPassThruSanitizer() {
    ModuleManager mgr = new ModuleManager(
        new PluginMeta(), TestBuildInfo.getInstance(),
        UriFetcher.NULL_NETWORK, mq);
    return new ExpressionSanitizerCaja(mgr, null) {
      @Override
      protected Rewriter newES53Rewriter(ModuleManager mgr) {
        return new Rewriter(mq, true, true) {{
          addRule(new Rule() {
            @Override
            @RuleDescription(
                name="passthru",
                synopsis="Pass through input vacuously 'expanded'",
                reason="Dummy rule for testing")
                public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
              return expandAll(node.clone(), scope);
            }
          });
        }};
      }
    };
  }

  private void assertSanitize(String input, String golden)
      throws Exception {
    Block inputNode = js(fromString(input));
    ModuleManager mgr = new ModuleManager(
        new PluginMeta(), TestBuildInfo.getInstance(),
        UriFetcher.NULL_NETWORK, mq);
    ParseTreeNode sanitized = new ExpressionSanitizerCaja(mgr, null)
        .sanitize(inputNode);
    String inputCmp = render(sanitized);

    String goldenCmp = render(js(fromString(golden)));

    assertEquals(goldenCmp, inputCmp);
    assertFalse(mq.hasMessageAtLevel(MessageLevel.WARNING));
  }
}
