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

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.js.Block;
import com.google.caja.util.CajaTestCase;

public class VarCollectorTest extends CajaTestCase {
  public final void testEmptyBlock() throws ParseException {
    assertOptimized(";", ";");
  }

  public final void testOneVar() throws ParseException {
    assertOptimized("var a;", "var a;");
  }

  public final void testTwoVars() throws ParseException {
    assertOptimized("var a, b;", "var a, b;");
  }

  public final void testOneVarWithInit() throws ParseException {
    assertOptimized("var a = 1;", "var a = 1;");
  }

  public final void testTwoVarsWithInit1() throws ParseException {
    assertOptimized("var a = 1, b;", "var a = 1, b;");
  }

  public final void testTwoVarsWithInit2() throws ParseException {
    assertOptimized("var b = 2, a;", "var a, b = 2;");
  }

  public final void testTwoVarsWithInit3() throws ParseException {
    assertOptimized("var a = 1, b = 2;", "var a = 1, b = 2;");
  }

  public final void testDupe() throws ParseException {
    assertOptimized("var a = 1, b = 2; { a = 3; }", "var a = 1, b = 2, a = 3;");
  }

  public final void testInnerFns() throws ParseException {
    assertOptimized(
        (""
         + "var a = 1; \n"
         + "function f(x, y) {\n"
         + "  var count = y - x, y = Math.abs(y), i;\n"
         + "  for (i = 0; i < count; i += 2) {\n"
         + "    if (arr[i] == y) { return arr[i + 1]; }\n"
         + "  }\n"
         + "  for (i = 1; i < count; i += 2) {\n"
         + "    if (arr[i] == x) { return arr[i]; }\n"
         + "  }\n"
         + "  x = y * y;\n"
         + "  return x;\n"
         + "}"),
        (""
         + "var a = 1; \n"
         + "function f(x, y) {\n"
         + "  var count = y - x;\n"
         + "  var y = Math.abs(y);\n"
         + "  for (var i = 0; i < count; i += 2) {\n"
         + "    if (arr[i] == y) { return arr[i + 1]; }\n"
         + "  }\n"
         + "  for (var i = 1; i < count; i += 2) {\n"
         + "    if (arr[i] == x) { return arr[i]; }\n"
         + "  }\n"
         + "  var x = y * y;\n"
         + "  return x;\n"
         + "}"
        ));
  }

  public final void testForInLoops() throws ParseException {
    assertOptimized(
        "var k, x; for (k in o) { f(o[k]); }",
        "var x; for (var k in o) { f(o[k]); }");
  }

  public final void testForLoops() throws ParseException {
    assertOptimized(
        "var n = 10, i; for (i = 0; i < n; ++i) { f(a(i)); }",
        "var n = 10; for (var i = 0; i < n; ++i) { f(a(i)); }");
  }

  public final void testCatchException() throws ParseException {
    assertOptimized(
        "try { foo(); } catch (e) { throw new Error(e); }",
        "try { foo(); } catch (e) { throw new Error(e); }");
  }

  private void assertOptimized(String golden, String input)
      throws ParseException {
    Block program = js(fromString(input));
    VarCollector.optimize(program);
    assertEquals(render(js(fromString(golden))), render(program));
  }
}
