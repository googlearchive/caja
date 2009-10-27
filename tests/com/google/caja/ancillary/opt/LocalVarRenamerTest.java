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

import com.google.caja.util.CajaTestCase;

public class LocalVarRenamerTest extends CajaTestCase {
  public final void testThisAndArguments() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "Foo.prototype.bar = function (a) { return this.x * this.y[a]; };"
            + "function cat() { return Array.prototype.join.call(arguments); }"
            ))),
        render(new LocalVarRenamer(mq).optimize(js(fromString(
            ""
            + "Foo.prototype.bar = function (n) { return this.x * this.y[n]; };"
            + "function cat() { return Array.prototype.join.call(arguments); }"
            )))));
  }

  public final void testFunctionReferences() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function fib(a) { return a <= 2 ? a : fib(a - 1) + fib(a - 2); }"
            + "function a(b) { return b < 0 ? -b : b; }"
            ))),
        render(new LocalVarRenamer(mq).optimize(js(fromString(
            ""
            + "function fib(n) { return n <= 2 ? n : fib(n - 1) + fib(n - 2); }"
            + "function a(x) { return x < 0 ? -x : x; }"
            )))));
  }

  public final void testFunctionExternReferences() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "(function () {"
            + "  var a = 1, b = 2, c = 3;"
            + "  return (function () {"
            + "    var d = function e(a) { return 4 * a; };"
            + "    return d(a + b + c);"
            + "  })();"
            + "})();"
            ))),
        render(new LocalVarRenamer(mq).optimize(js(fromString(
            ""
            + "(function () {"
            + "  var x = 1, y = 2, z = 3;"
            + "  return (function () {"
            + "    var foo = function bar(n) { return 4 * n; };"
            + "    return foo(x + y + z);"
            + "  })();"
            + "})();"
            )))));
  }

  public final void testFunctionSelfReferences() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "(function () {"
            + "  function a(b) { return b <= 2 ? b : a(b - 1) + a(b - 2); }"
            + "})();"
            ))),
        render(new LocalVarRenamer(mq).optimize(js(fromString(
            ""
            + "(function () {"
            + "  function f(n) { return n <= 2 ? n : f(n - 1) + f(n - 2); }"
            + "})();"
            )))));
  }

  public final void testUnusedDeclarations() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "(function () {"
            + "  var a;"
            + "})();"
            ))),
        render(new LocalVarRenamer(mq).optimize(js(fromString(
            ""
            + "(function () {"
            + "  var x;"
            + "})();"
            )))));
  }

  public final void testDuplicateDeclarations1() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "(function () {"
            + "  var a = 1, a = 2;"
            + "  return a;"
            + "})();"
            ))),
        render(new LocalVarRenamer(mq).optimize(js(fromString(
            ""
            + "(function () {"
            + "  var x = 1, x = 2;"
            + "  return x;"
            + "})();"
            )))));
  }

  public final void testDuplicateDeclarations2() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "(function () {"
            + "  var a = 1;"
            + "  function a() {}"
            + "  return a;"
            + "})();"
            ))),
        render(new LocalVarRenamer(mq).optimize(js(fromString(
            ""
            + "(function () {"
            + "  var x = 1;"
            + "  function x() {}"
            + "  return x;"
            + "})();"
            )))));
  }

  public final void testDuplicateDeclarations3() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "(function (a) {"
            + "  var a = 1;"
            + "  return a;"
            + "})();"
            ))),
        render(new LocalVarRenamer(mq).optimize(js(fromString(
            ""
            + "(function (x) {"
            + "  var x = 1;"
            + "  return x;"
            + "})();"
            )))));
  }

  public final void testDuplicateDeclarations4() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "(function (a) {"
            + "  function a(a) { var a = 1; return a; }"
            + "  return a;"
            + "})();"
            ))),
        render(new LocalVarRenamer(mq).optimize(js(fromString(
            ""
            + "(function (x) {"
            + "  function x(x) { var x = 1; return x; }"
            + "  return x;"
            + "})();"
            )))));
  }

  public final void testDuplicateDeclarations5() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "(function (a, a) {"
            + "  return a;"
            + "})();"
            ))),
        render(new LocalVarRenamer(mq).optimize(js(fromString(
            ""
            + "(function (x, x) {"
            + "  return x;"
            + "})();"
            )))));
  }

  public final void testMasking() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function f(a, b, c) {"
            + "  return map(c, function (c) { return a + b * c; });"
            + "}"
            ))),
        render(new LocalVarRenamer(mq).optimize(js(fromString(
            ""
            + "function f(x, y, arr) {"
            + "  return map(arr, function (a) { return x + y * a; });"
            + "}"
            )))));
  }

  public final void testCatchBlocks() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "var baz = (function (a) {"
            + "  (function () {"
            + "    var b = bar;"
            + "    try {"
            + "      return new b.baz();"
            + "    } catch (d) {"  // Cannot reuse a because of IE scoping bug.
            // Outer declarations used only in catch not clobbered.
            + "      var c = d.message || a;"
            + "    }"
            + "    return 'badness ' + c + ' : ' + b;"
            + "  })();"
            + "})('PANIC');"
            ))),
        render(new LocalVarRenamer(mq).optimize(js(fromString(
            ""
            + "var baz = (function (defaultMessage) {"
            + "  (function () {"
            + "    var foo = bar;"
            + "    try {"
            + "      return new foo.baz();"
            + "    } catch (e) {"
            + "      var out = e.message || defaultMessage;"
            + "    }"
            + "    return 'badness ' + out + ' : ' + foo;"
            + "  })();"
            + "})('PANIC');"
            )))));
  }

  public final void testNestedCatchBlocks() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "var baz = (function () {"
            + "  var a = bar;"
            + "  try {"
            + "    return new a.baz();"
            + "  } catch (d) {"  // Cannot reuse a because of IE scoping bug
            + "    try {"
            + "      var b = d.message;"
            + "    } catch (e) {"
            + "      var c = 'panic';"
            + "    }"
            + "  }"
            + "  return 'badness ' + (b || c) + ' : ' + a;"
            + "})();"
            ))),
        render(new LocalVarRenamer(mq).optimize(js(fromString(
            ""
            + "var baz = (function () {"
            + "  var foo = bar;"
            + "  try {"
            + "    return new foo.baz();"
            + "  } catch (e) {"
            + "    try {"
            + "      var out = e.message;"
            + "    } catch (e) {"
            + "      var panic = 'panic';"
            + "    }"
            + "  }"
            + "  return 'badness ' + (out || panic) + ' : ' + foo;"
            + "})();"
            )))));
  }

  public final void testExceptionCollision() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function panic(a) {"
            + "  try {"
            + "    throw new Error(a);"
            + "  } catch (b) {"
            + "    if (b.message !== a) {"
            + "      throw 'bad Error';"
            + "    }"
            + "  }"
            + "  throw new Error(a);"
            + "}"
            ))),
        render(new LocalVarRenamer(mq).optimize(js(fromString(
            ""
            + "function panic(msg) {"
            + "  try {"
            + "    throw new Error(msg);"
            + "  } catch (e) {"
            + "    if (e.message !== msg) {"
            + "      throw 'bad Error';"
            + "    }"
            + "  }"
            + "  throw new Error(msg);"
            + "}"
            )))));
  }

  public final void testNoContagion() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "var foo = bar;"
            + "function fn1(a, b) {"
            + "  var c = hello;"
            + "  d(notEval(c));"
            + "  function d(a) {"
            + "    function c(a) {"
            + "      var b = a * a;"
            + "      return b * gfoo;"
            + "    }"
            + "    return c(a - b);"
            + "  }"
            + "}"
            ))),
        render(new LocalVarRenamer(mq).optimize(js(fromString(
            ""
            + "var foo = bar;"
            + "function fn1(bar, baz) {"
            + "  var boo = hello;"
            + "  fn2(notEval(boo));"
            + "  function fn2(boo) {"
            + "    function fn3(baz) {"
            + "      var foo = baz * baz;"
            + "      return foo * gfoo;"
            + "    }"
            + "    return fn3(boo - baz);"
            + "  }"
            + "}"
            )))));
  }

  public final void testEvalContagion() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "var foo = bar;"
            + "function fn1(bar, baz) {"
            + "  var boo = hello;"
            + "  fn2(eval(boo));"
            + "  function fn2(a) {"
            + "    function b(a) {"
            + "      var c = a * a;"
            + "      return c * gfoo;"
            + "    }"
            + "    return b(a - baz);"
            + "  }"
            + "}"
            ))),
        render(new LocalVarRenamer(mq).optimize(js(fromString(
            ""
            + "var foo = bar;"
            + "function fn1(bar, baz) {"
            + "  var boo = hello;"
            + "  fn2(eval(boo));"
            + "  function fn2(boo) {"
            + "    function fn3(baz) {"
            + "      var foo = baz * baz;"
            + "      return foo * gfoo;"
            + "    }"
            + "    return fn3(boo - baz);"
            + "  }"
            + "}"
            )))));
  }

  public final void testWithContagion() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "var foo = bar;"
            + "function fn1(bar, baz) {"
            + "  var boo = hello;"
            + "  with (foo) {"
            + "    fn2(function (a) { return a[boo]; });"
            + "  }"
            + "  function fn2(a) {"
            + "    function b(a) {"
            + "      var c = a * a;"
            + "      return c * gfoo;"
            + "    }"
            + "    return b(a - baz);"
            + "  }"
            + "}"
            ))),
        render(new LocalVarRenamer(mq).optimize(js(fromString(
            ""
            + "var foo = bar;"
            + "function fn1(bar, baz) {"
            + "  var boo = hello;"
            + "  with (foo) {"
            + "    fn2(function (x) { return x[boo]; });"
            + "  }"
            + "  function fn2(boo) {"
            + "    function fn3(baz) {"
            + "      var foo = baz * baz;"
            + "      return foo * gfoo;"
            + "    }"
            + "    return fn3(boo - baz);"
            + "  }"
            + "}"
            )))));
  }
}
