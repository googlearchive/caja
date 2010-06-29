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

import com.google.caja.util.FailureIsAnOption;

import junit.framework.AssertionFailedError;

/**
 * Contains all the tests that should apply to any JS dialect defined
 * by the Caja project (i.e., Valija or Cajita).
 *
 * @author ihab.awad@gmail.com
 */
public abstract class CommonJsRewriterTestCase extends RewriterTestCase {

  /**
   * Tests that "in" works as expected
   */
  public final void testIn() throws Exception {
    assertConsistent(
        "('length' in {}) && " +
        "fail('readable property mistaken for existing property');");
    assertConsistent(
        "('length' in []) || " +
        "fail('arrays should have a length');");

    assertConsistent(
        "('x' in { x: 1 }) || " +
        "fail('failed to find existing readable property');");
    assertConsistent(
        "('y' in { x: 1 }) && " +
        "fail('found nonexisting property');");
    assertConsistent(
        "var flag = true;" +
        "try { 'length' in '123'; }" +
        "catch (e) { flag = false; }" +
        "if (flag) { fail ('should throw TypeError'); }" +
        "true;");
  }
  
  /**
   * @see <a href="http://code.google.com/p/google-caja/issues/detail?id=1238"
   *      >issue 1238</a>
   */
  public final void testArrayLikeApply() throws Exception {
    assertConsistent(
	"function x(a,b) { return a===0 && b===1; }" +
	"function y() { return x.apply(null,arguments); }" +
	"x.apply(null, [0,1]);");
    assertConsistent(
        "function x(a,b) { return a===0 && b===1; }" +
        "function y() { return x.apply(null,arguments); }" +
        "y.apply(null, [0,1]);");
  }

  /**
   * Tests that the length property whitelisting works on non-objects
   */
  public final void testStringLength() throws Exception {
    assertConsistent("('123').length;");
  }

  /**
   * Tests that eval is uncallable.
   */
  public final void testEval() throws Exception {
    rewriteAndExecute(
        "var success = false;" +
        "try { eval('1'); } catch (e) { success = true; }" +
        "if (!success) { fail('Outer eval is accessible.'); }");
  }

  /**
   * Tests that arguments to functions are not mutable through the
   * arguments array, but the arguments array itself is mutable.
   */
  public final void testMutableArguments() throws Exception {
    rewriteAndExecute(
        "function f(a) {" +
        "  try {" +
        "    if (arguments[0] !== false || arguments.length !== 1) { " +
        "      fail('arguments not initialized correctly');" +
        "    }" +
        "    arguments[0] = true;" +
        "  } catch (e) {" +
        "    fail('assignment to arguments failed');" +
        "  }" +
        "  if (a) { fail('Joined arguments'); }" +
        "  if (!arguments[0]) { fail('arguments not mutated'); }" +
        "}" +
        "f(false);");
  }

  /**
   * Tests that arguments.caller is ungettable.
   */
  public final void testGetArgsCaller() throws Exception {
    rewriteAndExecute(
      "function f() {" +
      "  try { arguments.caller; } catch (e) { return; }" +
      "  fail('arguments.caller did not throw');" +
      "}" +
      "f();");
  }
    
  /**
   * Tests that arguments.caller is unsettable.
   */
  @FailureIsAnOption
  public final void testSetArgsCaller() throws Exception {
    // TODO(erights): failure should no longer be an option on (S)ES5/3.
    rewriteAndExecute(
      "function f() {" +
      "  try { arguments.caller = 8; } catch (e) { return; }" +
      "  fail('assigning to arguments.caller did not throw');" +
      "}" +
      "f();");
  }

  /**
   * Tests that func.caller is ungettable.
   */
  public final void testGetFuncCaller() throws Exception {
    rewriteAndExecute(
      "function f() {" +
      "  try { f.caller; } catch (e) { return; }" +
      "  fail('<function>.caller did not throw');" +
      "}" +
      "f();");
  }
  
  /**
   * Tests that func.caller is unsettable.
   */
  @FailureIsAnOption
  public final void testSetFuncCaller() throws Exception {
    // TODO(erights): failure should no longer be an option on (S)ES5/3.
    rewriteAndExecute(
      "function f() {" +
      "  try { f.caller = 9; } catch (e) { return; }" +
      "  fail('assigning to <function>.caller did not throw');" +
      "}" +
      "f();");
  }

  /**
   * Tests that arguments.callee is ungettable.
   */
  public final void testGetArgsCallee() throws Exception {
    rewriteAndExecute(
      "function f() {" +
      "  try { arguments.callee; } catch (e) { return; }" +
      "  fail('arguments.callee did not throw');" +
      "}" +
      "f();");
  }
  
  /**
   * Tests that arguments.callee is unsettable.
   */
  @FailureIsAnOption
  public final void testSetArgsCallee() throws Exception {
    // TODO(erights): failure should no longer be an option on (S)ES5/3.
    rewriteAndExecute(
      "function f() {" +
      "  try { arguments.callee = 7; } catch (e) { return; }" +
      "  fail('assigning to arguments.callee did not throw');" +
      "}" +
      "f();");
  }

  /**
   * Tests that func.arguments is ungettable.
   */
  public final void testGetFuncArguments() throws Exception {
    rewriteAndExecute(
      "function f(a) {" +
      "  g();" +
      "}\n" +
      "function g() {" +
      "  try { f.arguments; } catch (e) { return; }" +
      "  fail('<function>.arguments did not throw');" +
      "}" +
      "f(false);");
  }
  
  /**
   * Tests that func.arguments is unsettable.
   */
  @FailureIsAnOption
  public final void testSetFuncArguments() throws Exception {
    // TODO(erights): failure should no longer be an option on (S)ES5/3.
    rewriteAndExecute(
      "function f(a) {" +
      "  g();" +
      "}\n" +
      "function g() {" +
      "  try { f.arguments = 6; } catch (e) { return; }" +
      "  fail('assigning to <function>.arguments did not throw');" +
      "}" +
      "f(false);");
  }
  

  /**
  * Tests that arguments are immutable from another function's scope even if
  * func.arguments turns out to be readable.
  */
  public final void testCrossScopeArguments() throws Exception {
    rewriteAndExecute(
      "function f(a) {" +
      "  g();" +
      "  if (a) { fail('Mutable cross scope arguments'); }" +
      "}\n" +
      "function g() {" +
      "  var args;" +
      "  try { args = f.arguments; } catch (e) { return; }" +
      "  if (args) { args[0] = true; }" +
      "}" +
      "f(false);");
  }

  public final void testSameArguments() throws Exception {
    assertConsistent(
      "function foo() {" +
      "  return arguments === arguments;" +
      "}" +
      "foo();");
  }

  public final void testConcatArguments() throws Exception {
    assertConsistent(
      "function foo() {" +
      "  return [1].concat(arguments);" +
      "}" +
      "foo('a', 'b')[1][1];");
  }

  /**
   * Tests that exceptions are not visible outside of the catch block.
   */
  public final void testCatch() throws Exception {
    try {
      rewriteAndExecute(
          "var e = false;" +
          "try { throw true; } catch (e) {}" +
          "if (e) { fail('Exception visible out of proper scope'); }");
      fail("Exception that masks var should not pass");
    } catch (AssertionFailedError e) {
      // pass
    }
  }

  /**
   * Tests that setTimeout is uncallable.
   */
  public final void testSetTimeout() throws Exception {
    rewriteAndExecute(
        "var success=false;" +
        "try { setTimeout('1',10); } catch(e) { success=true; }" +
        "if (!success) { fail('setTimeout is accessible'); }");
  }

  /**
   * Tests that Object.watch is uncallable.
   */
  public final void testObjectWatch() throws Exception {
    rewriteAndExecute(
        "var x={}; var success=false;" +
        "try { x.watch(y, function(){}); } catch(e) { success=true; }" +
        "if (!success) { fail('Object.watch is accessible'); }");
  }

  public final void testForIn1() throws Exception {
    rewriteAndExecute(
        ""
        + "function Foo() {"
        + "  return { x: 1, y: 2, z: 3 };"
        + "}"
        + "var obj = new Foo();"
        + "var y = {};"
        + "var result = [];"
        + "for (y.k in obj) {"
        + "  result.push(y.k);"
        + "}"
        + "assertEquals("
        + "    '' + result,"
        + "    '' + ['x', 'y', 'z']);");
    rewriteAndExecute(
        ""
        + "function test(obj) {"
        + "  var y = {};"
        + "  var result = [];"
        + "  for (y.k in obj) {"
        + "    result.push(y.k);"
        + "  }"
        + "  return '' + result;"
        + "}"
        + "assertEquals('', test());");
    rewriteAndExecute(
        ""
        + "function Foo() {"
        + "  return { x: 1, y: 2, z: 3 };"
        + "}"
        + "var obj = new Foo();"
        + "var result = [];"
        + "for (var k in obj)"
        + "  result.push(k);"
        + "assertEquals("
        + "    '' + result,"
        + "    '' + ['x', 'y', 'z']);");
  }

  public final void testFor() throws Exception {
    assertConsistent("var i; for (i = 0; i < 10; i++) {} i;");
    assertConsistent("for (var i = 0; i < 10; i++) {} i;");
    assertConsistent("for (var i = 0, j = 0; i < 10; i++) { j += 10; } j;");
    assertConsistent("for (var i = 0, j = 0; i < 10; i++, j += 10) { } j;");
  }

  public final void testMultiDeclaration() throws Exception {
    assertConsistent("var a = 3, b = 4, c = 5; a + b + c;");
    assertConsistent("var a, b; a = 3; b = 4; a + b;");
    assertConsistent(
        "  function f() {"
        + "  var a = 3, b = 4;"
        + "  return a + b;"
        + " }"
        + "f();");
  }

  public final void testCommonReformedGenerics() throws Exception {
    assertConsistent(
        "var x = [33];" +
        "x.foo = [].push;" +
        "x.foo.call(x, 44);" +
        "x;");
    assertConsistent(
        "var x = [33];" +
        "x.foo = [].push;" +
        "x.foo.apply(x, [6,7,8]);" +
        "x;");
    assertConsistent(
        "var x = [33];" +
        "x.foo = [].push;" +
        "x.foo.bind(x)(6,7,8);" +
        "x;");
    assertConsistent(
        "var x = [33];" +
        "x.foo = [].push;" +
        "x.foo.bind(x,6)(7,8);" +
        "x;");
    assertConsistent(
        "[].push.length;");
    assertConsistent(
        "var x = {blue:'green'};" +
        "x.foo = [].push;" +
        "x.foo.call(x, 44);" +
        "delete x.foo;" +
        "x;");
    assertConsistent(
        "var x = {blue:'green'};" +
        "x.foo = [].push;" +
        "x.foo.call(x, 44);" +
        "cajita.getOwnPropertyNames(x).sort();");
  }

  public final void testTypeofConsistent() throws Exception {
    assertConsistent("[ (typeof noSuchGlobal), (typeof 's')," +
                     "  (typeof 4)," +
                     "  (typeof null)," +
                     "  (typeof (void 0))," +
                     "  (typeof [])," +
                     "  (typeof {})," +
                     "  (typeof (function () {}))," +
                     "  (typeof { x: 4.0 }.x)," +
                     "  (typeof { 2: NaN }[1 + 1])" +
                     "];");
    rewriteAndExecute("assertEquals(typeof new RegExp('.*'), 'object');");
  }

  /**
   * Tests that callbacks from the Cajita runtime and from the tamed ES3 API
   * to either Cajita or Valija code works.
   * <p>
   * The uncajoled branch of the tests below establish that the callbacks
   * work uncajoled when they are tamed as simple frozen functions.
   */
  public final void testCommonCallback() throws Exception {
    assertConsistent(
        "'abc'.replace('b', function() {return 'xy';});");
    assertConsistent(
        "var v = [1, 2, 3, 7, 4, 5];" +
        "var cmp = function(a, b) {" +
        "  return (a < b) ? +1 : (b < a) ? -1 : 0;" +
        "};" +
        "v.sort(cmp);");
    rewriteAndExecute("",
        "var a = [];\n" +
        "cajita.forOwnKeys({x:3}, function(k, v) {a.push(k, v);});" +
        "assertEquals(a.toString(), 'x,3');",
        "var a = [];\n" +
        "cajita.forOwnKeys({x:3}, ___.markFuncFreeze(function(k, v) {a.push(k, v);}));" +
        "assertEquals(a.toString(), 'x,3');");
    rewriteAndExecute("",
        "var a = [];\n" +
        "cajita.forAllKeys({x:3}, function(k, v) {a.push(k, v);});" +
        "assertEquals(a.toString(), 'x,3');",
        "var a = [];\n" +
        "cajita.forAllKeys({x:3}, ___.markFuncFreeze(function(k, v) {a.push(k, v);}));" +
        "assertEquals(a.toString(), 'x,3');");
    assertConsistent("(function(){}).bind.call(function(a, b) {return a + b;}, {}, 3)(4);");
  }

  /**
   * Tests that neither Cajita nor Valija code can cause a privilege
   * escalation by calling a tamed exophoric function with null as the
   * this-value.
   * <p>
   * The uncajoled branch of the tests below establish that a null does cause
   * a privilege escalation for normal non-strict JavaScript.
   */
  public final void testNoPrivilegeEscalation() throws Exception {
    rewriteAndExecute("",
        "assertTrue([].valueOf.call(null) === cajita.USELESS);",
        "assertTrue([].valueOf.call(null) === this);");
    rewriteAndExecute("",
        "assertTrue([].valueOf.apply(null, []) === cajita.USELESS);",
        "assertTrue([].valueOf.apply(null, []) === this);");
    rewriteAndExecute("",
        "assertTrue([].valueOf.bind(null)() === cajita.USELESS);",
        "assertTrue([].valueOf.bind(null)() === this);");
  }

  /**
   * Tests that the special handling of null on tamed exophora works.
   * <p>
   * The reification of tamed exophoric functions contains
   * special cases for when the first argument to call, bind, or apply
   * is null or undefined, in order to protect against privilege escalation.
   * {@code #testNoPrivilegeEscalation()} tests that we do prevent the
   * privilege escalation. Here, we test that this special case preserves
   * correct functionality.
   */
  public final void testTamedXo4aOkOnNull() throws Exception {
    rewriteAndExecute("this.foo = 8;",

        "var x = cajita.beget(cajita.USELESS);" +
        "assertFalse(({foo: 7}).hasOwnProperty.call(null, 'foo'));" +
        "assertTrue(cajita.USELESS.isPrototypeOf(x));" +
        "assertTrue(({foo: 7}).isPrototypeOf.call(null, x));",

        "assertTrue(({}).hasOwnProperty.call(null, 'foo'));" +
        "assertFalse(({bar: 7}).hasOwnProperty.call(null, 'bar'));");
    rewriteAndExecute("this.foo = 8;",

        "var x = cajita.beget(cajita.USELESS);" +
        "assertFalse(({foo: 7}).hasOwnProperty.apply(null, ['foo']));" +
        "assertTrue(cajita.USELESS.isPrototypeOf(x));" +
        "assertTrue(({foo: 7}).isPrototypeOf.apply(null, [x]));",

        "assertTrue(({}).hasOwnProperty.apply(null, ['foo']));" +
        "assertFalse(({bar: 7}).hasOwnProperty.apply(null, ['bar']));");
    rewriteAndExecute("this.foo = 8;",

        "var x = cajita.beget(cajita.USELESS);" +
        "assertFalse(({foo: 7}).hasOwnProperty.bind(null)('foo'));" +
        "assertTrue(cajita.USELESS.isPrototypeOf(x));" +
        "assertTrue(({foo: 7}).isPrototypeOf.bind(null)(x));",

        "assertTrue(({}).hasOwnProperty.bind(null)('foo'));" +
        "assertFalse(({bar: 7}).hasOwnProperty.bind(null)('bar'));");
  }

  /**
   * Tests that Error objects are preserved by tamed try/catch and that they
   * are born non-frozen.
   *
   * See issue 1097, issue 1038, {@link CajitaRewriterTest#testErrorFreeze},
   *     and {@link DefaultValijaRewriterTest#testErrorFreeze}.
   */
  public final void testErrorTaming() throws Exception {
    rewriteAndExecute(
            "var t = new Error('foo');" +
            "assertFalse(cajita.isFrozen(t));" +
            "try {" +
            "  throw t;" +
            "} catch (ex) {" +
            "  assertTrue(t === ex);" +
            "}");
  }

  /**
   * Tests that the apparent [[Class]] of the tamed JSON object is 'JSON', as
   * it should be according to ES5.
   *
   * See issue 1086
   */
  public final void testJSONClass() throws Exception {
    rewriteAndExecute(
            "assertTrue(({}).toString.call(JSON) === '[object JSON]');");
    rewriteAndExecute("assertTrue(JSON.toString() === '[object JSON]');");

    // In neither Cajita nor Valija is it possible to mask the real toString()
    // when used implicitly by a primitive JS coercion rule.
    rewriteAndExecute("assertTrue(''+JSON === '[object Object]');");
  }

  /**
   * Tests that an inherited <tt>*_canSet___</tt> fastpath flag does not enable
   * bogus writability.
   * <p>
   * See <a href="http://code.google.com/p/google-caja/issues/detail?id=1052"
   * >issue 1052</a>.
   */
  public final void testNoCanSetInheritance() throws Exception {
    rewriteAndExecute(
            "(function() {" +
            "  var a = {};" +
            "  var b = cajita.freeze(cajita.beget(a));" +
            "  a.x = 8;" +
            "  assertThrows(function(){b.x = 9;});" +
            "  assertEquals(b.x, 8);" +
            "})();");
  }

  /**
   *
   */
  public final void testFeralTameBoundary() throws Exception {
    rewriteAndExecute(
            "function forbidden() { return arguments; }" +
            "function xo4ic(f) {" +
            "  return ___.callPub(f, 'call', [___.USELESS, this]);" +
            "}" +
            "___.markXo4a(xo4ic);" +

            "function innocent(f) { return f(this); }" +
            "___.markInnocent(innocent);" +

            "function simple(f) {" +
            "  return ___.callPub(f, 'call', [___.USELESS, simple]); " +
            "}" +
            "___.markFuncFreeze(simple);" +

            "function Point(x,y) {" +
            "  this.x = x;" +
            "  this.y = y;" +
            "}" +
            "Point.prototype.getX = function() { return this.x; };" +
            "Point.prototype.getY = function() { return this.y; };" +
            "Point.prototype.badness = function(str) { return eval(str); };" +
            "Point.prototype.welcome = function(visitor) {" +
            "  return visitor(this.x, this.y); " +
            "};" +
            "___.markCtor(Point, Object, 'Point');" +
            "___.grantGenericMethod(Point.prototype, 'getX');" +
            "___.grantTypedMethod(Point.prototype, 'getY');" +
            "___.grantInnocentMethod(Point.prototype, 'welcome');" +

            "var pt = new Point(3,5);" +

            "function eight() { return 8; }" +
            "function nine() { return 9; }" +
            "___.markFuncFreeze(nine);" +
            "___.tamesTo(eight, nine);" +

            "var funcs = [[simple, Point, pt], forbidden, " +
            "             xo4ic, innocent, eight];" +
            "___.freeze(funcs[0]);" +
            "funcs[5] = funcs;" +
            "var f = { " +
            "  forbidden: forbidden," +
            "  xo4ic: xo4ic," +
            "  innocent: innocent," +
            "  simple: simple," +
            "  Point: Point," +
            "  pt: pt," +
            "  eight: eight," +
            "  funcs: funcs" +
            "};" +
            "f.f = f;" +
            "funcs[6] = f;" +
            "testImports.f = f;" + // purposeful safety violation
            "testImports.t = ___.tame(f);",


            "assertFalse(f === t);" +
            "assertFalse('forbidden' in t);" +
            "assertFalse(f.xo4ic === t.xo4ic);" +
            "assertFalse(f.innocent === t.innocent);" +
            "assertTrue(f.simple === t.simple);" +
            "assertTrue(f.Point === t.Point);" +
            "assertTrue(f.pt === t.pt);" +
            "assertFalse('x' in t.pt);" +
            "assertFalse('y' in t.pt);" +
            "assertTrue('getX' in t.pt);" +
            "assertTrue('getY' in t.pt);" +
            "assertFalse('badness' in t.pt);" +
            "assertTrue('welcome' in t.pt);" +
            "assertFalse(f.eight === t.eight);" +
            "assertFalse(f.funcs === t.funcs);" +
            "assertTrue(f.funcs[0][0] === t.funcs[0][0]);" +
            "assertTrue(f.funcs[0] === t.funcs[0]);" +
            "assertTrue('1' in t.funcs);" +
            "assertTrue(t.funcs[1] === void 0);" +
            "assertTrue(t.funcs === t.funcs[5]);" +
            "assertTrue(t === t.funcs[6]);" +
            "assertTrue(t === t.f);" +

            "var lastArg = void 0;" +
            "function capture(arg) { return lastArg = arg; }" +

            "var lastResult = t.xo4ic.call(void 0, capture);" +
            "assertTrue(lastArg === cajita.USELESS);" +
            "assertTrue(lastResult === cajita.USELESS);" +

            "lastResult = t.innocent.call(void 0, capture);" +
            "assertTrue(lastArg === cajita.USELESS);" +
            "assertTrue(lastResult === cajita.USELESS);" +

            "lastResult = t.innocent.apply(void 0, [capture]);" +
            "assertTrue(lastArg === cajita.USELESS);" +
            "assertTrue(lastResult === cajita.USELESS);" +

            "lastResult = t.simple(capture);" +
            "assertTrue(lastArg === t.simple);" +
            "assertTrue(lastResult === t.simple);" +

            "assertTrue(t.pt.getX() === 3);" +
            "assertTrue(t.pt.getY() === 5);" +
            "var getX = t.pt.getX;" +
            "var getY = t.pt.getY;" +
            "assertTrue(cajita.isPseudoFunc(getX));" +
            "assertTrue(cajita.isPseudoFunc(getY));" +
            "assertTrue(getX.call(t.pt) === 3);" +
            "assertTrue(getY.call(t.pt) === 5);" +
            "var nonpt = {x: 33, y: 55};" +
            "assertTrue(getX.call(nonpt) === 33);" +
            "assertThrows(function() { getY.call(nonpt); });" +

            "function visitor(x, y) {" +
            "  assertTrue(x === 3);" +
            "  assertTrue(y === 5);" +
            "}" +
            "t.pt.welcome(visitor);" +

            "assertTrue(t.eight() === 9);" +
            "lastResult = t.innocent.call(void 0, t.eight);" +
            "assertTrue(lastResult === 8);" +

            "lastResult = t.innocent.apply(void 0, [t.eight]);" +
            "assertTrue(lastResult === 8);",


            "");
  }
}


// Instructions for reviewers that appear at the end of codereview.appspot.
// Please make all test methods final so that they cannot be unintentionally
// overridden in subclasses.
