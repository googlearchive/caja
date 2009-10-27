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

import com.google.caja.parser.js.Block;
import com.google.caja.util.CajaTestCase;

public class ConstLocalOptimizationTest extends CajaTestCase {
  public final void testAssignedInClosure() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f() {"
            + "  var isIE = true;"
            + "  function x() { isIE = !window.opera; }"
            + "  return [isIE, false];"
            + "}"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "function f() {"
                + "  var isIE = true;"  // Inlined by the ParseTreeKB
                + "  var isFF = false;"
                + "  function x() { isIE = !window.opera; }"
                + "  return [isIE, isFF];"
                + "}")),
            mq)));
  }

  public final void testDoubleInitialized() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f() {"
            + "  var isIE = true;"
            + "  var isIE = isIE && !window.opera;"
            + "  return [isIE, false];"
            + "}"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "function f() {"
                + "  var isIE = true;"  // Inlined by the ParseTreeKB
                + "  var isFF = false, isIE = isIE && !window.opera;"
                + "  return [isIE, isFF];"
                + "}")),
            mq)));
  }

  public final void testMultiDecls() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f() {"
            + "  return [true, false];"
            + "}"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "function f() {"
                + "  var isFF = false, isIE = true;"
                + "  return [isIE, isFF];"
                + "}")),
            mq)));
  }

  public final void testUndefined() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f() {"
            + "  return [void 1, 'undefined', void 0];"
            + "}"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "function f() {"
                + "  var x, x = void 1, y = 'undefined', z;"
                + "  return [x, y, z];"
                + "}")),
            mq)));
  }

  public final void testUndeclared() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f() {"
            + "  return x;"
            + "}"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "function f() {"
                + "  return x;"
                + "}")),
            mq)));
  }

  public final void testFunctionDeclarations() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f() {"
            + "  function g() {}"
            + "  return g;"
            + "}"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "function f() {"
                + "  function g() {}"
                + "  return g;"
                + "}")),
            mq)));
  }

  public final void testFunctionMasking1() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f() {"
            + "  var g;"
            + "  function g() {}"
            + "  return g;"
            + "}"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "function f() {"
                + "  var g;"
                + "  function g() {}"
                + "  return g;"
                + "}")),
            mq)));
  }

  public final void testFunctionMasking2() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f() {"
            + "  function g() {}"
            + "  var g;"
            + "  return g;"
            + "}"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "function f() {"
                + "  function g() {}"
                + "  var g;"
                + "  return g;"
                + "}")),
            mq)));
  }

  public final void testIdentityOfOutput() throws Exception {
    Block canOptimize = js(fromString(
        ""
        + "function f() {"
        + "  var x = true;"
        + "  return x;"
        + "}"));
    Block cannotOptimize = js(fromString(
        ""
        + "function f() {"
        + "  return true;"
        + "}"));
    assertSame(
        cannotOptimize, ConstLocalOptimization.optimize(cannotOptimize, mq));
    Block optimized = ConstLocalOptimization.optimize(canOptimize, mq);
    assertEquals(render(cannotOptimize), render(optimized));
    assertFalse(canOptimize == optimized);
    assertSame(optimized, ConstLocalOptimization.optimize(optimized, mq));
  }
}
