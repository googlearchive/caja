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

import junit.framework.AssertionFailedError;

/**
 * Contains all the tests that should apply to any JS dialect defined
 * by the Caja project (i.e., Caja or Cajita).
 *
 * @author ihab.awad@gmail.com
 */
public abstract class CommonJsRewriterTest extends RewriterTestCase {

  /**
   * Tests that eval is uncallable.
   *
   * @throws Exception
   */
  public void testEval() throws Exception {
    rewriteAndExecute(
        "var success=false;" +
        "try{eval('1');}catch(e){success=true;}" +
        "if (!success)fail('Outer eval is accessible.')");
  }

  /**
   * Tests that arguments to functions are not mutable through the
   * arguments array.
   *
   * @throws Exception
   */
  public void testMutableArguments() throws Exception {
    rewriteAndExecute(
        "caja.log('___.args = ' + ___.args);",
        "function f(a) {" +
          "try{" +
            "arguments[0] = 1;" +
            "if (a) fail('Mutable arguments');" +
          "} catch (e) {" +
             // pass
          "}" +
        "}" +
        "f(0);",
        "");
  }

  /**
   * Tests that the caller attribute is unreadable.
   *
   * @throws Exception
   */
  public void testCaller() throws Exception {
    rewriteAndExecute(
        "function f(x) {" +
        "  try {" +
        "    if (arguments.caller || f.caller) {" +
        "      fail('caller is accessible');" +
        "    }" +
        "  } catch (e) {}" +
        "}" +
        "f(1);");
  }

  /**
   * Tests that the callee attribute is unreadable.
   *
   * @throws Exception
   */
  public void testCallee() throws Exception {
    rewriteAndExecute(
        "function f(x) {" +
        "  try {" +
        "    if (arguments.callee || f.callee) {" +
        "      fail('callee is accessible');" +
        "    }" +
        "  } catch (e) {}" +
        "}" +
        "f(1);");
  }

  /**
   * Tests that arguments are immutable from another function's scope.
   *
   * @throws Exception
   */
  public void testCrossScopeArguments() throws Exception {
    rewriteAndExecute(
        "function f(a) {" +
          "g();" +
          "if (a) fail('Mutable cross scope arguments');" +
        "}\n" +
        "function g() {" +
          "if (f.arguments) " +
            "f.arguments[0] = 1;" +
        "}" +
        "f(0);");
  }

  /**
   * Tests that exceptions are not visible outside of the catch block.
   *
   * @throws Exception
   */
  public void testCatch() throws Exception {
    try {
      rewriteAndExecute(
          "var e = 0;" +
          "try{ throw 1; } catch (e) {}" +
          "if (e) fail('Exception visible out of proper scope');");
      fail("Exception that masks var should not pass");
    } catch (AssertionFailedError e) {
      // pass
    }
  }

  /**
   * Tests that setTimeout is uncallable.
   *
   * @throws Exception
   */
  public void testSetTimeout() throws Exception {
    rewriteAndExecute(
        "var success=false;try{setTimeout('1',10);}" +
        "catch(e){success=true;}" +
        "if(!success)fail('setTimeout is accessible');");
  }

  /**
   * Tests that Object.watch is uncallable.
   *
   * @throws Exception
   */
  public void testObjectWatch() throws Exception {
    rewriteAndExecute(
        "var x={}; var success=false;" +
        "try{x.watch(y, function(){});}" +
        "catch(e){success=true;}" +
        "if(!success)fail('Object.watch is accessible');");
  }

  /**
   * Tests that unreadable global properties are not readable by way of
   * Object.toSource().
   *
   * @throws Exception
   */
  public void testToSource() throws Exception {
    rewriteAndExecute(
        "var x;" +
        "try{x=toSource();}catch(e){}" +
        "if(x) fail('Global write-only values are readable.');");
  }

  public void testForIn() throws Exception {
    // TODO(ihab.awad): Disabled until we figure out how to get a test fixture
    // that allows us to add stuff to IMPORTS___ before the test is run.
    if (false) {
    rewriteAndExecute(
        "",
        "function Foo() {" +
        "  this.x_ = 1;" +
        "  this.y = 2;" +
        "  this.z = 3;" +
        "}" +
        "var obj = new Foo();" +
        "var y = {};" +
        "var result = [];" +
        "for (y.k in obj) {" +
        "  result.push(y.k);" +
        "}",
        "assertEquals(" +
        "    ___.getNewModuleHandler().getImports().result.toSource()," +
        "    (['y', 'z']).toSource());");
    rewriteAndExecute(
        "",
        "function test(obj) {" +
        "  var y = {};" +
        "  var result = [];" +
        "  for (y.k in obj) {" +
        "    result.push(y.k);" +
        "  }" +
        "  return result;" +
        "}",
        "assertEquals(" +
        "    ___.getNewModuleHandler().getImports().test({x_:1, y:2, z:3})" +
        "        .sort().toSource()," +
        "    (['y', 'z']).toSource());");
    rewriteAndExecute(
        "",
        "function Foo() {" +
        "  this.x_ = 1;" +
        "  this.y = 2;" +
        "}" +
        "caja.def(Foo, Object, {" +
        "  test: function () {" +
        "    var y = {};" +
        "    var result = [];" +
        "    for (y.k in this) {" +
        "      result.push(y.k);" +
        "    }" +
        "    return result;" +
        "  }});" +
        "var obj = new Foo();",
        "assertEquals(" +
        "    ___.getNewModuleHandler().getImports().obj.test()" +
        "        .sort().toSource()," +
        "    (['test', 'x_', 'y']).toSource());");
    }
  }

  public void testFor() throws Exception{
    assertConsistent("var i; for (i = 0; i < 10; i++) {} i;");
    assertConsistent("for (var i = 0; i < 10; i++) {} i;");
    assertConsistent("for (var i = 0, j = 0; i < 10; i++) { j += 10; } j;");
    assertConsistent("for (var i = 0, j = 0; i < 10; i++, j += 10) { } j;");
  }
}
