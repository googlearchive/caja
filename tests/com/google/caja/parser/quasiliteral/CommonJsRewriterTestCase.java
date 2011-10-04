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
        + "    '' + result.sort(),"
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
        + "    '' + result.sort(),"
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
  }

  public final void testTypeofConsistent() throws Exception {
    assertConsistent("[ (typeof noSuchGlobal)," +
                     "  (typeof 's')," +
                     "  (typeof 4)," +
                     "  (typeof null)," +
                     "  (typeof (void 0))," +
                     "  (typeof [])," +
                     "  (typeof {})," +
                     "  (typeof (function () {}))," +
                     "  (typeof { x: 4.0 }.x)," +
                     "  (typeof { 2: NaN }[1 + 1])" +
                     "];");
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
    assertConsistent("(function(){}).bind.call(function(a, b) {return a + b;}, {}, 3)(4);");
  }

  /**
   * Tests that Error objects are preserved by tamed try/catch and that they
   * are born non-frozen.
   *
   * See issue 1097, issue 1038, {@link ES53RewriterTest#testObjectFreeze}.
   */
  public final void testErrorTaming() throws Exception {
    rewriteAndExecute(
            "var t = new Error('foo');" +
            "assertFalse(((typeof cajita !== 'undefined') ?" +
            "    cajita :" +
            "    Object).isFrozen(t));" +
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
  public final void testCommonJSONClass() throws Exception {
    rewriteAndExecute(
            "assertTrue(({}).toString.call(JSON) === '[object JSON]');");
    rewriteAndExecute("assertTrue(JSON.toString() === '[object JSON]');");
  }

  /**
   * Tests that cajoled code expecting a function can use the standard
   * cross-frame test.
   */
  public final void testFunctionClass() throws Exception {
    rewriteAndExecute(
        "assertTrue(({}).toString.call(function(){})==='[object Function]')");
  }
}


// Instructions for reviewers that appear at the end of codereview.appspot.
// Please make all test methods final so that they cannot be unintentionally
// overridden in subclasses.
