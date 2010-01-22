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
            + "  return [isIE, false, function x() { isIE = !window.opera; }];"
            + "}"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "function f() {"
                + "  var isIE = true;"  // Inlined by the ParseTreeKB
                + "  var isFF = false;"  // Not inlined because can change
                + "  function x() { isIE = !window.opera; }"
                + "  return [isIE, isFF, x];"
                + "}")))));
  }

  public final void testAssignedInUnusedClosure() throws Exception {
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
                + "  var isIE = true;"
                + "  var isFF = false;"  // Could change if x were used.
                + "  function x() { isIE = !window.opera; }"
                + "  return [isIE, isFF];"
                + "}")))));
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
                + "}")))));
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
                + "}")))));
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
                + "}")))));
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
                + "}")))));
  }

  public final void testFunctionDeclarations() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f() {"
            + "  return function g() {};"
            + "}"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "function f() {"
                + "  function g() {}"
                + "  return g;"
                + "}")))));
  }

  public final void testFunctionMasking1() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f() {"
            + "  var g = 1;"
            + "  function g() {}"
            + "  return g;"
            + "}"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "function f() {"
                + "  var g = 1;"
                + "  function g() {}"
                + "  return g;"
                + "}")))));
  }

  public final void testFunctionMasking2() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f() {"
            + "  function g() {}"
            + "  var g = 1;"
            + "  return g;"
            + "}"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "function f() {"
                + "  function g() {}"
                + "  var g = 1;"
                + "  return g;"
                + "}")))));
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
    assertSame(cannotOptimize, ConstLocalOptimization.optimize(cannotOptimize));
    Block optimized = ConstLocalOptimization.optimize(canOptimize);
    assertEquals(render(cannotOptimize), render(optimized));
    assertFalse(canOptimize == optimized);
    assertSame(optimized, ConstLocalOptimization.optimize(optimized));
  }

  public final void testForEachLoop1() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f() {"
            + "  var lastKey = null;"
            + "  for (lastKey in obj);"
            + "  return lastKey;"
            + "}"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "function f() {"
                + "  var lastKey = null;"
                + "  for (lastKey in obj);"
                + "  return lastKey;"
                + "}")))));
  }

  public final void testForEachLoop2() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f() {"
            + "  lastKey = null;"
            + "  for (var lastKey in obj);"
            + "  return lastKey;"
            + "}"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "function f() {"
                + "  lastKey = null;"
                + "  for (var lastKey in obj);"
                + "  return lastKey;"
                + "}")))));
  }

  public final void testUnusedFunctions() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f() {"
            + "  return 3;"
            + "}"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "function f() {"
                + "  function g() { return 4; }"
                + "  var i = 3;"
                + "  return i;"
                + "}")))));
  }

  public final void testGlobalsNotEliminated() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "var x = true;"  // Declaration needs to stay around.
            + "function f() { alert('Hello World!'); }"
            // Could inline x, but only by reasoning about lack of intervening
            // assignments and side-effects of setters and getters.
            + "alert(x);"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "var x = true;"
                + "function f() { alert('Hello World!'); }"
                + "alert(x);")))));
  }

  public final void testSinglyUsedObjects() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f(x) {"
            + "  var obj2 = { 1: baz, 2: boo };"
            + "  return (function g(y) { return obj2[y]; })"
            + "      (({ foo: 1, bar: 2 })[x]);"
            + "}"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "function f(x) {"
                + "  var obj1 = { foo: 1, bar: 2 };"
                + "  var obj2 = { 1: baz, 2: boo };"
                + "  function g(y) { return obj2[y]; }"
                + "  return g(obj1[x]);"
                + "}")))));
  }

  public final void testRedundantDecls() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f(x) {"
            + "  return 1;"
            + "}"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "function f(x) {"
                + "  var x, x = 1, x;"
                + "  return x;"
                + "}")))));
  }

  public final void testNotInlineable() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f() {"
            + "  var x = y, y = 1;"
            + "  return [x, 1, 2];"
            + "}"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "function f() {"
                + "  var x = y, y = 1, z = 2;"
                + "  return [x, y, z];"
                + "}")))));
  }

  public final void testArguments() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f() {"
            + "  var arguments = 1;"
            + "  return arguments[0];"
            + "}"))),
        render(ConstLocalOptimization.optimize(
            js(fromString(
                ""
                + "function f() {"
                + "  var arguments = 1;"
                + "  return arguments[0];"
                + "}")))));
  }

  public final void testLiteralsAcrossFunctionBoundaries() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f() {"
            + "  return function g() { return 4; };"
            + "}"))),
        render(ConstLocalOptimization.optimize(js(fromString(
            ""
            + "function f() {"
            + "  var n = 4;"
            + "  function g() { return n; }"
            + "  return g;"
            + "}")))));
  }

  public final void testCalls() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function g() {"
            + "  var y = (function f() { return z; })(), z = 2;"
            + "  return 1 + y + 2;"
            + "}"))),
        render(ConstLocalOptimization.optimize(js(fromString(
            ""
            + "function g() {"
            + "  var x = 1, y = f(), z = 2;"
            + "  function f() { return z; }"
            + "  return x + y + z;"
            + "}")))));
  }
}
