// Copyright (C) 2009 Google Inc.
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

package com.google.caja.ancillary.opt;

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Statement;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Join;

import java.util.Collections;

public class JsOptimizerTest extends CajaTestCase {
  JsOptimizer opt;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    opt = new JsOptimizer(mq);
    opt.setRename(true);
    opt.setEnvJson(new ObjectConstructor(FilePosition.UNKNOWN));
  }

  public final void testOptimizeNothing() {
    assertOptimized(emptyProgram());
  }

  public final void testSideEffectFreeStmtsEliminated() throws Exception {
    assertOptimized(
        emptyProgram(),
        js(fromString("+4;")));
  }

  public final void testVoidNotChanged() throws Exception {
    assertOptimized(
        js(fromString("foo;")),
        js(fromString("void foo;")));
  }

  public final void testRenaming1() throws Exception {
    assertOptimized(
        js(fromString("function quotient(a, b) { return a / b; }")),
        js(fromString(
            "function quotient(dividend, divisor) { return dividend/divisor; }"
            )));
  }

  public final void testRenaming2() throws Exception {
    opt.setRename(false);
    assertOptimized(
        js(fromString(
            "function quotient(dividend, divisor) { return dividend/divisor; }"
            )),
        js(fromString(
            "function quotient(dividend, divisor) { return dividend/divisor; }"
            )));
  }

  public final void testFolding() throws Exception {
    assertOptimized(
        js(fromString("alert(2);")),
        js(fromString("alert(1+1);")));
  }

  public final void testMultiple() throws Exception {
    assertOptimized(
        js(fromString("alert(a?(foo(),bar(),baz()):boo())")),
        js(fromString("alert(function(){ if (a) { foo(); bar(); return baz(); }"
                      + "else return boo(); }());")));
  }

  public final void testIssue1348() throws Exception {
    String input = Join.join(
        "\n",
        "if (XMLHttpRequest && 'withCredentials' in new XMLHttpRequest()) {",
        "  // FF 3.5+ and Safari 4",
        "  request = requestFunctions['w3cxhr'];",
        "} else if (XDomainRequest) {",
        "  // IE8",
        "  request = requestFunctions['msxdr'];",
        "} else {",
        "  // Older browser; fallback",
        "  request = requestFunctions['jsonp'];",
        "}");
    assertOptimized(
        js(fromString(
          "XMLHttpRequest && 'withCredentials' in new XMLHttpRequest"
          + "? (request = requestFunctions.w3cxhr)"
          + ": (request = requestFunctions[XDomainRequest ? 'msxdr' : 'jsonp'])"
          )),
        js(fromString(input)));
  }

  private void assertOptimized(Statement golden, Block... inputs) {
    for (Block input : inputs) { opt.addInput(input); }
    Statement optimized = opt.optimize();
    assertEquals(renderProgram(golden), renderProgram(optimized));
  }

  private static Block emptyProgram() {
    return new Block(FilePosition.UNKNOWN, Collections.<Statement>emptyList());
  }

  private static String renderProgram(Statement s) {
    StringBuilder out = new StringBuilder();
    RenderContext rc = new RenderContext(new JsMinimalPrinter(out));
    if (s instanceof Block) {
      ((Block) s).renderBody(rc);
    } else {
      s.render(rc);
    }
    rc.getOut().noMoreTokens();
    return out.toString();
  }
}
