// Copyright (C) 2007 Google Inc.
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

import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.ReturnStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.SyntheticNodes;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.TestBuildInfo;
import com.google.caja.util.Executor;
import com.google.caja.util.Lists;
import com.google.caja.util.RhinoTestBed;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import junit.framework.AssertionFailedError;

public class ES53RewriterTest extends CommonJsRewriterTestCase {
  protected class TestUriFetcher implements UriFetcher {
    public FetchedData fetch(ExternalReference ref, String mimeType)
        throws UriFetchException {
      try {
        URI uri = ref.getReferencePosition().source().getUri()
            .resolve(ref.getUri());
        if ("resource".equals(uri.getScheme())) {
          return dataFromResource(uri.getPath(), new InputSource(uri));
        } else {
          throw new UriFetchException(ref, mimeType);
        }
      } catch (IOException ex) {
        throw new UriFetchException(ref, mimeType, ex);
      }
    }
  }

  private Rewriter es53Rewriter;

  /**
   * Tests that an inherited <tt>*_w___</tt> flag does not enable
   * bogus writability.
   * <p>
   * See <a href="http://code.google.com/p/google-caja/issues/detail?id=1052"
   * >issue 1052</a>.
   */
  public final void testNoFastpathWritableInheritance() throws Exception {
    rewriteAndExecute(
            "(function() {" +
            "  var a = {};" +
            "  var b = Object.freeze(Object.create(a));" +
            "  a.x = 8;" +
            "  assertThrowsMsg(function(){b.x = 9;}, 'not extensible');" +
            "  assertEquals(b.x, 8);" +
            "})();");
  }

  public final void testConstant() throws Exception {
    assertConsistent("1;");
  }

  public final void testInit() throws Exception {
    assertConsistent("var a = 0; a;");
  }

  public final void testNew() throws Exception {
    assertConsistent(
        "function f() { this.x = 1; }" +
        "var g = new f();" +
        "g.x;");
  }

  public final void testThrowCatch() throws Exception {
    assertConsistent(
        "var x = 0; try { throw 1; }" +
        "catch (e) { x = e; }" +
        "x;");
    assertConsistent(
        "var x = 0; try { throw { a: 1 }; }" +
        "catch (e) { x = e; }" +
        "'' + x;");
    assertConsistent(
        "var x = 0; try { throw 'err'; }" +
        "catch (e) { x = e; }" +
        "x;");
    assertConsistent(
        "var x = 0; try { throw new Error('err'); }" +
        "catch (e) { x = e.message; }" +
        "x;");
    assertConsistent(
        "var x = 0; try { throw 1; }" +
        "catch (e) { x = e; }" +
        "finally { x = 2; }" +
        "x;");
    assertConsistent(
        "var x = 0; try { throw { a: 1 }; }" +
        "catch (e) { x = e; }" +
        "finally { x = 2; }" +
        "x;");
    assertConsistent(
        "var x = 0; try { throw 'err'; }" +
        "catch (e) { x = e; }" +
        "finally { x = 2; }" +
        "x;");
    assertConsistent(
        "var x = 0; try { throw new Error('err'); }" +
        "catch (e) { x = e.message; }" +
        "finally { x = 2; }" +
        "x;");
  }

  public final void testProtoCall() throws Exception {
    assertConsistent("Array.prototype.sort.call([3, 1, 2]);");
    assertConsistent("[3, 1, 2].sort();");
    assertConsistent("[3, 1, 2].sort.call([4, 2, 7]);");

    assertConsistent("String.prototype.indexOf.call('foo', 'o');");
    assertConsistent("'foo'.indexOf('o');");

    assertConsistent("'foo'.indexOf.call('bar', 'o');");
    assertConsistent("'foo'.indexOf.call('bar', 'a');");
  }

  public final void testInherit() throws Exception {
    assertConsistent(
        "function Point(x) { this.x = x; }\n" +
        "Point.prototype.toString = function () {\n" +
        "  return '<' + this.x + '>';\n" +
        "};\n" +
        "function WP(x) { Point.call(this,x); }\n" +
        "WP.prototype = Object.create(Point.prototype);\n" +
        "var pt = new WP(3);\n" +
        "pt.toString();");
  }

  /** See bug 528 */
  public final void testRegExpLeak() throws Exception {
    rewriteAndExecute(
        "assertEquals('' + (/(.*)/).exec(), 'undefined,undefined');");
  }

  public final void testClosure() throws Exception {
    assertConsistent(
        "function f() {" +
        "  var y = 2; " +
        "  this.x = function() {" +
        "    return y;" +
        "  }; " +
        "}" +
        "var g = new f();" +
        "var h = {};" +
        "f.call(h);" +
        "h.y = g.x;" +
        "h.x() + h.y();");
  }

  public final void testNamedFunctionShadow() throws Exception {
    assertConsistent("function f() { return f; } f === f();");
    assertConsistent(
        "(function () { function f() { return f; } return f === f(); })();");
  }

  public final void testArray() throws Exception {
    assertConsistent("[3, 2, 1].sort();");
    assertConsistent("[3, 2, 1].sort.call([4, 2, 7]);");
  }

  public final void testObject() throws Exception {
    assertConsistent("({ x: 1, y: 2 });");
  }

  public final void testFunctionCallWithSideEffects() throws Exception {
    assertConsistent(
        "(function() {\n" +
        "  function f(a, b, c) { return '' + [a, b, c]; }\n" +
        "  var i = 0;\n" +
        "  return f(f = 208, i, i++);\n" +
        "})();");
  }

  public final void testMethodCallWithSideEffects() throws Exception {
    assertConsistent(
        "(function () {\n" +
        "  var o = { f: function(a, b, c) { return '' + [a, b, c]; } };\n" +
        "  var i = 0;\n" +
        "  return o.f(o = 217, i, i++);\n" +
        "})();");
  }

  public final void testPropertyAssignmentWithSideEffects() throws Exception {
    assertConsistent(
        "(function () {\n" +
        "  var o = {};\n" +
        "  return o.x = (o = 225);\n" +
        "})();");
  }

  public final void testArrayAssignmentWithSideEffects1() throws Exception {
    assertConsistent(
        "(function () {\n" +
        "  var a = [];\n" +
        "  return a['3'] = (a = 233);\n" +
        "})();");
  }

  public final void testArrayAssignmentWithSideEffects2() throws Exception {
    assertConsistent(
        "(function () {\n" +
        "  var a = [];\n" +
        "  return a[+'3'] = (a = 241);\n" +
        "})();");
  }

  public final void testArrayAssignmentWithSideEffects3() throws Exception {
    assertConsistent(
        "(function () {\n" +
        "  var a = [];\n" +
        "  return a[3] = (a = 249);\n" +
        "})();");
  }

  public final void testFunctionToStringCall() throws Exception {
    rewriteAndExecute(
        "function foo() {}\n"
        + "assertEquals('\\nfunction foo() {\\n}\\n',\n"
        + "             foo.toString());");
    rewriteAndExecute(
        "function foo (a, b) { 1; }\n"
        + "assertEquals('\\nfunction foo(a, b) {\\n    1;\\n}\\n',\n"
        + "             foo.toString());");
    rewriteAndExecute(
        "function foo() {}\n"
        + "assertEquals('\\nfunction foo() {\\n}\\n',\n"
        + "             Function.prototype.toString.call(foo));");
    rewriteAndExecute(
        "var foo = function (x$x, y_y) {};\n"
        + "assertEquals("
        + "    '\\nfunction foo$_var(x$x, y_y) {\\n}\\n',\n"
        + "    Function.prototype.toString.call(foo));");
  }

  public final void testDate() throws Exception {
    assertConsistent("(new Date(0)).getTime();");
    assertConsistent("'' + (new Date(0));");
    rewriteAndExecute(
        ""
        + "var time = (new Date - 1);"
        + "assertFalse(isNaN(time));"
        + "assertEquals('number', typeof time);");
  }

  public final void testMultiDeclaration2() throws Exception {
    rewriteAndExecute("var a, b, c;");
    rewriteAndExecute(
        ""
        + "var a = 0, b = ++a, c = ++a;"
        + "assertEquals(++a * b / c, 1.5);");
  }

  public final void testDelete() throws Exception {
    assertConsistent(
        "(function () { var a = { x: 1 }; delete a.x; return typeof a.x; })();"
        );
    assertConsistent("var a = { x: 1 }; delete a.x; typeof a.x;");
    // Tests for the gotcha rather than the spec'ed behavior.
    // See http://code.google.com/p/google-caja/wiki/DifferencesBetweenES5Over3AndES5
    rewriteAndExecute(
        "var x = {a:1, '[object Object]':2};" +
        "delete x[{valueOf:function(){return 'a';}}];" +
        "assertEquals(x.a, void 0);" +
        "assertEquals(x['[object Object]'], 2);");
  }

  public final void testIn2() throws Exception {
    assertConsistent(
        "(function () {" +
        "  var a = { x: 1 };\n" +
        "  return '' + ('x' in a) + ('y' in a);" +
        "})();");
    assertConsistent(
        "var a = { x: 1 };\n" +
        "[('x' in a), ('y' in a)];");
  }

  /**
   * Try to construct some class instances.
   */
  public final void testFuncCtor() throws Exception {
    rewriteAndExecute(
        "function Foo(x) { this.x = x; }" +
        "var foo = new Foo(2);" +
        "if (!foo) { fail('Failed to construct a global object.'); }" +
        "assertEquals(2, foo.x);" +
        "assertEquals(Foo, foo.constructor);");
    rewriteAndExecute(
        "(function () {" +
        "  function Foo(x) { this.x = x; }" +
        "  var foo = new Foo(2);" +
        "  if (!foo) { fail('Failed to construct a local object.'); }" +
        "  assertEquals(2, foo.x);" +
        "})();");
    rewriteAndExecute(
        "function Foo() { }" +
        "var foo = new Foo();" +
        "if (!foo) {" +
        "  fail('Failed to use a simple named function as a constructor.');" +
        "}");
  }

  public final void testFuncArgs() throws Exception {
    rewriteAndExecute(
        ""
        + "var x = 0;"
        + "function f() { x = arguments[0]; }"
        + "f(3);"
        + "assertEquals(3, x);");
  }

  public final void testStatic() throws Exception {
    assertConsistent("Array.slice([3, 4, 5, 6], 1);");
  }

  public final void testConcatArgs() throws Exception {
    rewriteAndExecute("", "(function(x, y){ return [x, y]; })",
        "var f = ___.getNewModuleHandler().getLastValue();"
        + "function g(var_args) { return f.apply(___.USELESS, arguments); }"
        + "assertEquals(g(3, 4).toString(), [3, 4].toString());");
  }

  public final void testReformedGenerics() throws Exception {
    assertConsistent(
        "var x = [33];" +
        "x.foo = [].push;" +
        "x.foo(44);" +
        "x;");
    assertConsistent(
        "var x = {blue:'green'};" +
        "x.foo = [].push;" +
        "x.foo(44);" +
        "var keys = [];" +
        "for (var i in x) { if (x.hasOwnProperty(i)) { keys.push(i); } }" +
        "keys.sort();");
    assertConsistent(
        "var x = [33];" +
        "Array.prototype.push.apply(x, [3,4,5]);" +
        "x;");
    assertConsistent(
        "var x = {blue:'green'};" +
        "Array.prototype.push.apply(x, [3,4,5]);" +
        "var keys = [];" +
        "for (var i in x) { if (x.hasOwnProperty(i)) { keys.push(i); } }" +
        "keys.sort();");
    assertConsistent(
        "var x = {blue:'green'};" +
        "x.foo = [].push;" +
        "x.foo.call(x, 44);" +
        "var keys = [];" +
        "for (var i in x) { if (x.hasOwnProperty(i)) { keys.push(i); } }" +
        "keys.sort();");
  }

  public final void testMonkeyPatchPrimordialFunction() throws Exception {
    assertConsistent(
        "isNaN.foo = 'bar';" +
        "isNaN.foo;");
  }

  public final void testInMonkeyDelete() throws Exception {
    assertConsistent(
        "var x = {y:1 };" +
        "delete x.y;" +
        "('y' in x);");
  }

  public final void testMonkeyOverride() throws Exception {
    assertConsistent(
        // TODO(erights): Fix when bug 953 is fixed.
        "Date.prototype.propertyIsEnumerable = function(p) { return true; };" +
        "(new Date()).propertyIsEnumerable('foo');");
  }

  public final void testEmbeddedcajaVM() throws Exception {
    assertConsistent(
        ""
        + "\"use strict,cajaVM\"; \n"
        + "var foo; \n"
        + "(function () { \n"
        + "  foo = function () { return 8; }; \n"
        + "})(); \n"
        + "foo();"
        );
  }

  /**
   * Tests freezing objects.
   */
  public final void testObjectFreeze() throws Exception {
    rewriteAndExecute(
        "var r = Object.freeze({});" +
        "assertThrowsMsg(function(){r.foo = 8;}, 'not extensible');");
    rewriteAndExecute(
        "var f = function(){};" +
        "f.foo = 8;");
    rewriteAndExecute(
        "var f = Object.freeze(function(){});" +
        "assertThrowsMsg(function(){f.foo = 8;}, 'not extensible');");
    rewriteAndExecute(
        "function Point(x,y) {" +
        "  this.x = x;" +
        "  this.y = y;" +
        "}" +
        "var pt = new Point(3,5);" +
        "pt.x = 8;" +
        "Object.freeze(pt);" +
        "assertThrowsMsg(function(){pt.y = 9;}, 'not writable');");
    // Check that deferred creation of prototype property doesn't make it
    // writable.
    rewriteAndExecute(
        "function f(){}" +
        "Object.freeze(f);" +
        "assertThrowsMsg(function() { f.prototype = {}; }, 'not writable');");
  }

  /**
   * Tests that the {@code prototype}, {@code name}, and {@code length}
   * properties of function instances are set properly.
   */
  public final void testFunctionInstance() throws Exception {
    rewriteAndExecute(
        "assertTrue(!!((function(){}).prototype));");
    rewriteAndExecute(
        "assertEquals((function(a,b,c){}).length, 3);");
    rewriteAndExecute(
        "assertEquals((function x(a,b,c){}).name, 'x');");
    // Check frozen functions created early in es53.js
    rewriteAndExecute(
        "assertTrue(!!(cajaVM.USELESS.toString.prototype));");
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

        "var x = Object.create(cajaVM.USELESS);" +
        "assertFalse(({foo: 7}).hasOwnProperty.call(null, 'foo'));" +
        "assertTrue(cajaVM.USELESS.isPrototypeOf(x));" +
        "assertTrue(({foo: 7}).isPrototypeOf.call(null, x));",

        "assertTrue(({}).hasOwnProperty.call(null, 'foo'));" +
        "assertFalse(({bar: 7}).hasOwnProperty.call(null, 'bar'));");
    rewriteAndExecute("this.foo = 8;",

        "var x = Object.create(cajaVM.USELESS);" +
        "assertFalse(({foo: 7}).hasOwnProperty.apply(null, ['foo']));" +
        "assertTrue(cajaVM.USELESS.isPrototypeOf(x));" +
        "assertTrue(({foo: 7}).isPrototypeOf.apply(null, [x]));",

        "assertTrue(({}).hasOwnProperty.apply(null, ['foo']));" +
        "assertFalse(({bar: 7}).hasOwnProperty.apply(null, ['bar']));");
    rewriteAndExecute(
        "var x = Object.create(cajaVM.USELESS);" +
        "assertFalse(({foo: 7}).hasOwnProperty.bind(null)('foo'));" +
        "assertTrue(cajaVM.USELESS.isPrototypeOf(x));" +
        "assertTrue(({foo: 7}).isPrototypeOf.bind(null)(x));");
  }

  public final void testToString() throws Exception {
    assertConsistent(
        "var z = { toString: function () { return 'blah'; } };" +
        "try {" +
        "  '' + z;" +
        "} catch (e) {" +
        "  throw new Error('PlusPlus error: ' + e);" +
        "}");
    assertConsistent(
        "  function foo() {"
        + "  var x = 1;"
        + "  return {"
        + "    toString: function () {"
        + "      return x;"
        + "    }"
        + "  };"
        + "}"
        + "'' + (new foo);");
  }

  public final void testToStringToxicity() throws Exception {
    rewriteAndExecute(
        "",
        "function objMaker(f) {return {toString:f};}",
        "assertThrowsMsg(" +
        "    function() {testImports.objMaker(function(){return '1';});}," +
        "    'toxic');"
        );
  }

  public final void testInitializeMap() throws Exception {
    assertConsistent("var zerubabel = {bobble:2, apple:1}; zerubabel.apple;");
  }

  public final void testValueOf() throws Exception {
    assertConsistent("''+{valueOf:function(){return 5;}}");
  }

  public final void testAssertEqualsCajoled() throws Exception {
    try {
      rewriteAndExecute("assertEquals(1, 2);");
    } catch (AssertionFailedError e) {
      return;
    }
    fail("Assertions do not work in cajoled mode");
  }

  public final void testAssertThrowsCajoledNoError() throws Exception {
    rewriteAndExecute(
        "  assertThrows(function() { throw 'foo'; });");
    rewriteAndExecute(
        "  assertThrows("
        + "    function() { throw 'foo'; },"
        + "    'foo');");
  }

  public final void testAssertThrowsCajoledErrorNoMsg() throws Exception {
    try {
      rewriteAndExecute("assertThrows(function() {});");
    } catch (AssertionFailedError e) {
      return;
    }
    fail("Assertions do not work in cajoled mode");
  }

  public final void testAssertThrowsCajoledErrorWithMsg() throws Exception {
    try {
      rewriteAndExecute("assertThrows(function() {}, 'foo');");
    } catch (AssertionFailedError e) {
      return;
    }
    fail("Assertions do not work in cajoled mode");
  }

  public final void testConstructionWithFunction() throws Exception {
    assertConsistent(
        "  function Point() {}"
        + "var p = new Point();"
        + "(p !== undefined);");
    assertConsistent(
        "  var Point = function() {};"
        + "var p = new Point();"
        + "(p !== undefined);");
  }

  public final void testReflectiveMethodInvocation() throws Exception {
    assertConsistent(
        "(function (first, second) { return 'a' + first + 'b' + second; })"
        + ".call([], 8, 9);");
    assertConsistent(
        "var a = []; [].push.call(a, 5, 6); a;");
    assertConsistent(
        "(function (a, b) { return 'a' + a + 'b' + b; }).apply([], [8, 9]);");
    assertConsistent(
        "var a = []; [].push.apply(a, [5, 6]); a;");
    assertConsistent(
        "[].sort.apply([6, 5]);");
    assertConsistent(
        "function f(first, second) {" +
        "  return 'a' + first + 'b' + second;" +
        "}\n" +
        "var g = f.bind([], 8);\n" +
        "g(9);");
  }

  /**
   * Tests that <a href=
   * "http://code.google.com/p/google-caja/issues/detail?id=242"
   * >bug#242</a> is fixed.
   * <p>
   * The actual Function.bind() method used to be whitelisted and
   * written to return a frozen simple-function, allowing it to be called
   * from all code on all functions. As a result, if an <i>outer hull breach</i>
   * occurs -- if Caja code obtains a reference to a JavaScript
   * function value not marked as Caja-callable -- then
   * that Caja code could call the whitelisted bind() on it,
   * and then call the result, causing an <i>inner hull breach</i>,
   * which threatens kernel integrity.
   */
  public final void testToxicBind() throws Exception {
    rewriteAndExecute(
        "var confused = false;" +
        "testImports.keystone = function keystone() { confused = true; };" +
        "___.grantRead(testImports, 'keystone');",
        "assertThrowsMsg(function() {keystone.bind()();}, 'toxic');",
        "assertFalse(confused);");
  }

  /**
   * Tests that <a href=
   * "http://code.google.com/p/google-caja/issues/detail?id=590"
   * >bug#590</a> is fixed.
   * <p>
   * As a client of an object, Caja code must only be able to directly delete
   * <i>public</i> properties of non-frozen JSON containers. Due to this bug,
   * Caja code was able to delete properties in the Caja namespace.
   */
  public final void testBadDelete() throws Exception {
    rewriteAndExecute(
        "testImports.badContainer = {secret__: 3469};" +
        "___.grantRead(testImports, 'badContainer');",
        "assertThrowsMsg(function() {delete badContainer['secret__'];}," +
        "    'double underscore');",
        "assertEquals(testImports.badContainer.secret__, 3469);");
    rewriteAndExecute(
        "assertThrowsMsg(function() {delete ({})['proto___'];}," +
        "    'double underscore');");
  }

  /**
   * Tests that apply works.
   */
  public final void testApply() throws Exception {
    rewriteAndExecute(
        "",
        "var x = 0;" +
        "function f() { x = 1 }\n" +
        "f.apply({});",
        "assertEquals(testImports.x, 1);");
    // TODO(erights): Need more tests.
  }

  /**
   * Tests that <a href=
   * "http://code.google.com/p/google-caja/issues/detail?id=347"
   * >bug#347</a> is fixed.
   * <p>
   * The <tt>in</tt> operator should only test for properties visible to Caja.
   */
  public final void testInVeil() throws Exception {
    rewriteAndExecute(
        "assertFalse('f___' in Object);");
  }

  ////////////////////////////////////////////////////////////////////////
  // Handling of synthetic nodes
  ////////////////////////////////////////////////////////////////////////

  public final void testSyntheticIsUntouched() throws Exception {
    String source = "function foo() { this; arguments; }";
    ParseTreeNode input = js(fromString(source));
    syntheticTree(input);
    checkSucceeds(input, js(fromString("var dis___ = IMPORTS___;" + source)));
  }

  public final void testSyntheticMemberAccess() throws Exception {
    ParseTreeNode input = js(fromString("({}).foo"));
    syntheticTree(input);
    checkSucceeds(
        input,
        js(fromString("var dis___ = IMPORTS___; ___.iM([]).foo;")));
  }

  public final void testSyntheticFormals() throws Exception {
    FilePosition unk = FilePosition.UNKNOWN;
    FunctionConstructor fc = new FunctionConstructor(
        unk,
        new Identifier(unk, "f"),
        Arrays.asList(
            new FormalParam(new Identifier(unk, "x")),
            new FormalParam(
                SyntheticNodes.s(new Identifier(unk, "y___")))),
        new Block(
            unk,
            Arrays.<Statement>asList(new ReturnStmt(
                unk,
                Operation.createInfix(
                    Operator.MULTIPLICATION,
                    Operation.createInfix(
                        Operator.ADDITION,
                        new Reference(new Identifier(unk, "x")),
                        new Reference(SyntheticNodes.s(
                            new Identifier(unk, "y___")))),
                    new Reference(new Identifier(unk, "z")))))));
    checkSucceeds(
        new Block(
            unk,
            Arrays.asList(
                new FunctionDeclaration((FunctionConstructor) fc.clone()))),
        js(fromString(
            ""
            // x and y___ are formals, but z is free to the function.
            + "var dis___ = IMPORTS___;"
            + "{"
            + "  function f(x, y___) {"
            + "    return (x + y___) *"
            + "        (IMPORTS___.z_v___ ?"
            + "        IMPORTS___.z :"
            + "        ___.ri(IMPORTS___, 'z'));"
            + "  }"
            + "  IMPORTS___.w___('f', ___.f(f, 'f'));"
            + "}")));

    SyntheticNodes.s(fc);
    checkSucceeds(
        new Block(
            unk,
            Arrays.asList(
                new FunctionDeclaration((FunctionConstructor) fc.clone()))),
        js(fromString(
            ""
            // x and y___ are formals, but z is free to the function.
            + "var dis___ = IMPORTS___;"
            + "function f(x, y___) {"
            + "  return (x + y___) *"
            + "        (IMPORTS___.z_v___ ?"
            + "        IMPORTS___.z :"
            + "        ___.ri(IMPORTS___, 'z'));"
            + "}"
            // Since the function is synthetic, it is not marked.
            )));
  }

  ////////////////////////////////////////////////////////////////////////
  // Specific rules
  ////////////////////////////////////////////////////////////////////////

  public final void testWith() throws Exception {
    checkFails("with (dreams || ambiguousScoping) anything.isPossible();",
               "\"with\" blocks are not allowed");
    checkFails("with (dreams || ambiguousScoping) { anything.isPossible(); }",
               "\"with\" blocks are not allowed");
  }

  public final void testTryCatch() throws Exception {
    checkAddsMessage(js(fromString(
        "try {" +
        "  throw 2;" +
        "} catch (e) {" +
        "  var e;" +
        "}")),
        MessageType.MASKING_SYMBOL,
        MessageLevel.ERROR);
    checkAddsMessage(js(fromString(
        "var e;" +
        "try {" +
        "  throw 2;" +
        "} catch (e) {" +
        "}")),
        MessageType.MASKING_SYMBOL,
        MessageLevel.ERROR);
    checkAddsMessage(js(fromString(
        "try {} catch (x__) { }")),
        RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE);
    checkAddsMessage(js(fromString(
        "var x;" +
        "try {" +
        "  g[x + 0];" +
        "  g[x + 1];" +
        "} catch (e) {" +
        "  g[x + 2];" +
        "  e;" +
        "  g[x + 3];" +
        "}" +
        "var e;")),
        MessageType.MASKING_SYMBOL,
        MessageLevel.ERROR);
    rewriteAndExecute(
        "var handled = false;" +
        "try {" +
        "  throw null;" +
        "} catch (ex) {" +
        "  assertEquals(null, ex);" +  // Right value in ex.
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");  // Control reached and left the catch block.
    rewriteAndExecute(
        "var handled = false;" +
        "try {" +
        "  throw undefined;" +
        "} catch (ex) {" +
        "  assertEquals(undefined, ex);" +
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");
    rewriteAndExecute(
        "var handled = false;" +
        "try {" +
        "  throw true;" +
        "} catch (ex) {" +
        "  assertEquals(true, ex);" +
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");
    rewriteAndExecute(
        "var handled = false;" +
        "try {" +
        "  throw 37639105;" +
        "} catch (ex) {" +
        "  assertEquals(37639105, ex);" +
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");
    rewriteAndExecute(
        "var handled = false;" +
        "try {" +
        "  throw 'panic';" +
        "} catch (ex) {" +
        "  assertEquals('panic', ex);" +
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");
    rewriteAndExecute(
        "var handled = false;" +
        "try {" +
        "  throw new Error('hello');" +
        "} catch (ex) {" +
        "  assertEquals('hello', ex.message);" +
        "  assertEquals('Error', ex.name);" +
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");
    rewriteAndExecute(
        "var handled = false;" +
        "try {" +
        "  throw { toString: function () { return 'hiya'; }, y: 4 };" +
        "} catch (ex) {" +
        "  assertEquals('object', typeof ex);" +
        "  assertEquals('hiya', ex.toString());" +
        "  assertEquals(4, ex.y);" +
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");
  }

  public final void testTryCatchFinally() throws Exception {
    checkAddsMessage(js(fromString(
        "try {" +
        "} catch (e) {" +
        "  var e;" +
        "} finally {" +
        "}")),
        MessageType.MASKING_SYMBOL,
        MessageLevel.ERROR);
    checkAddsMessage(js(fromString(
        "var e;" +
        "try {" +
        "} catch (e) {" +
        "} finally {" +
        "}")),
        MessageType.MASKING_SYMBOL,
        MessageLevel.ERROR);
    checkAddsMessage(js(fromString(
        "try {} catch (x__) { } finally { }")),
        RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE);
    assertConsistent(
        "var out = '';" +
        "try {" +
        "  throw 'hi ';" +
        "} catch (e) {" +
        "  out += e;" +
        "} finally {" +
        "  out += 'there';" +
        "}" +
        "out;");
  }

  public final void testTryFinally() throws Exception {
    assertConsistent(
        "var out = 0;" +
        "try {" +
        "  try {" +
        "    throw 2;" +
        "  } finally {" +
        "    out = 1;" +
        "  }" +
        "  out = 2;" +
        "} catch (e) {" +
        "}" +
        "out;");
  }

  public final void testGlobalBadSuffix() throws Exception {
    checkFails(
        "x__ = 1;",
        "Variables cannot end in \"__\"");
    checkFails(
        "var x__ = 1;",
        "Variables cannot end in \"__\"");
    checkFails(
        "var x = x__;",
        "Variables cannot end in \"__\"");
    checkFails(
        "var x\\u005f\\u005f;",
        "Variables cannot end in \"__\"");
    checkFails(
        "var x__;",
        "Variables cannot end in \"__\"");
    checkFails(
        "x\u005F\u005F = 1;",
        "Variables cannot end in \"__\"");
    checkFails(
        "__ = 1;",
        "Variables cannot end in \"__\"");
    checkFails(
        "\u005F\u005F = 1;",
        "Variables cannot end in \"__\"");
  }

  public final void testBadSuffix() throws Exception {
    checkFails(
        "function() { foo__; };",
        "Variables cannot end in \"__\"");
    // Make sure *single* underscore is okay
    checkSucceeds(
        "function() { var foo_ = 3; };",
        "var dis___ = IMPORTS___;" +
        "___.f(function () {" +
        "    var foo_;" +
        "    foo_ = 3;" +
        "  });");
    checkFails(
        "var x = function __() { };",
        "Variables cannot end in \"__\"");
    checkFails(
        "function () { var x = function __() { }; }",
        "Variables cannot end in \"__\"");
    checkFails(
        "function foo__() { }",
        "Variables cannot end in \"__\"");
    checkFails(
        "var foo__ = 3;",
        "Variables cannot end in \"__\"");
    checkFails(
        "var foo__;",
        "Variables cannot end in \"__\"");
    checkFails(
        "function() { function foo__() { } };",
        "Variables cannot end in \"__\"");
    checkFails(
        "function() { var foo__ = 3; };",
        "Variables cannot end in \"__\"");
    checkFails(
        "function() { var foo__; };",
        "Variables cannot end in \"__\"");
  }

  public final void testVarFuncFreeze() throws Exception {
    // We can cajole and refer to a function
    rewriteAndExecute(
        "function foo() {}" +
        "foo();");
    // We can assign a dotted property of a variable
    rewriteAndExecute(
        "var foo = {};" +
        "foo.x = 3;" +
        "assertEquals(foo.x, 3);");
    assertConsistent(
        "function foo() {}" +
        "var bar = foo;" +
        "bar.x = 3;" +
        "bar.x;");
  }

  public final void testReadBadSuffix() throws Exception {
    checkFails(
        "x.y__;",
        "Properties cannot end in \"__\"");
  }

  /**
   * Tests assignment to unmaskable and maskable globals.
   */
  public final void testSetBadFreeVariable() throws Exception {
    // Array is in Scope.UNMASKABLE_IDENTIFIERS
    checkAddsMessage(
        js(fromString("Array = function () { return [] };")),
        RewriterMessageType.CANNOT_ASSIGN_TO_IDENTIFIER);
    // Throws a ReferenceError
    rewriteAndExecute("assertThrowsMsg(function () { x = 1; }, 'not defined')");
  }

  public final void testSetBadSuffix() throws Exception {
    checkFails(
        "x.y__ = z;",
        "Properties cannot end in \"__\"");
  }

  public final void testSetBadInitialize() throws Exception {
    checkFails(
        "var x__ = 3;",
        "Variables cannot end in \"__\"");
  }

  public final void testSetBadDeclare() throws Exception {
    checkFails(
        "var x__;",
        "Variables cannot end in \"__\"");
  }

  public final void testSetVar() throws Exception {
    checkAddsMessage(
        js(fromString("try {} catch (x__) { x__ = 3; }")),
        RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE);
  }

  public final void testSetReadModifyWriteLocalVar() throws Exception {
    checkFails("x__ *= 2;", "");
    checkFails("x *= y__;", "");

    assertConsistent("var x = 3; x *= 2;");
    assertConsistent("var x = 1; x += 7;");
    assertConsistent("var x = 1; x /= '2';");
    assertConsistent("var o = { x: 'a' }; o.x += 'b'; o;");

    EnumSet<Operator> ops = EnumSet.of(
        Operator.ASSIGN_MUL,
        Operator.ASSIGN_DIV,
        Operator.ASSIGN_MOD,
        Operator.ASSIGN_SUM,
        Operator.ASSIGN_SUB,
        Operator.ASSIGN_LSH,
        Operator.ASSIGN_RSH,
        Operator.ASSIGN_USH,
        Operator.ASSIGN_AND,
        Operator.ASSIGN_XOR,
        Operator.ASSIGN_OR
        );
    for (Operator op : ops) {
      assertConsistent("var x = 41, y = 0, g = [17]; x " +
          op.getSymbol() + " g[y];");
    }
  }

  public final void testSetIncrDecr() throws Exception {
    checkFails("x__--;", "");
    assertConsistent(
        "var x = 2;" +
        "var arr = [--x, x, x--, x, ++x, x, x++, x];" +
        "assertEquals('1,1,1,0,1,1,1,2', arr.join(','));" +
        "arr;");
    assertConsistent(
        "var x = '2';" +
        "var arr = [--x, x, x--, x, ++x, x, x++, x];" +
        "assertEquals('1,1,1,0,1,1,1,2', arr.join(','));" +
        "arr;");
  }

  public final void testSetIncrDecrOnLocals() throws Exception {
    checkFails("++x__;", "");
    assertConsistent(
        "(function () {" +
        "  var x = 2;" +
        "  var arr = [--x, x, x--, x, ++x, x, x++, x];" +
        "  assertEquals('1,1,1,0,1,1,1,2', arr.join(','));" +
        "  return arr;" +
        "})();");
  }

  public final void testSetIncrDecrOfComplexLValues() throws Exception {
    checkFails("arr[x__]--;", "Variables cannot end in \"__\"");
    checkFails("arr__[x]--;", "Variables cannot end in \"__\"");

    assertConsistent(
        "(function () {" +
        "  var o = { x: 2 };" +
        "  var arr = [--o.x, o.x, o.x--, o.x, ++o.x, o.x, o.x++, o.x];" +
        "  assertEquals('1,1,1,0,1,1,1,2', arr.join(','));" +
        "  return arr;" +
        "})();");
  }

  public final void testSetIncrDecrOrderOfAssignment() throws Exception {
    assertConsistent(
        "(function () {" +
        "  var arrs = [1, 2];" +
        "  var j = 0;" +
        "  arrs[++j] *= ++j;" +
        "  assertEquals(2, j);" +
        "  assertEquals(1, arrs[0]);" +
        "  assertEquals(4, arrs[1]);" +
        "  return arrs;" +
        "})();");
    assertConsistent(
        "(function () {" +
        "  var foo = (function () {" +
        "               var k = 0;" +
        "               return function () {" +
        "                 switch (k++) {" +
        "                   case 0: return [10, 20, 30];" +
        "                   case 1: return 1;" +
        "                   case 2: return 2;" +
        "                   default: throw new Error(k);" +
        "                 }" +
        "               };" +
        "             })();" +
        "  return foo()[foo()] -= foo();" +
        "})();"
        );
  }

  public final void testDeletePub() throws Exception {
    checkFails("delete x.foo___;", "Properties cannot end in \"__\"");
    assertConsistent(
        "(function() {" +
        "  var o = { x: 3, y: 4 };" +    // A JSON object.
        "  function ptStr(o) { return '(' + o.x + ',' + o.y + ')'; }" +
        "  var history = [ptStr(o)];" +  // Record state before deletion.
        "  delete o.y;" +                // Delete
        "  delete o.z;" +                // Not present.  Delete a no-op
        "  history.push(ptStr(o));" +    // Record state after deletion.
        "  return history.toString();" +
        "})();");
    assertConsistent(
        "var alert = 'a';" +
        "var o = { a: 1 };" +
        "delete o[alert];" +
        "assertEquals(undefined, o.a);" +
        "o;");
  }

  public final void testDeleteFails() throws Exception {
    rewriteAndExecute(
        "assertThrowsMsg(function (){delete (function f(){}).name;}," +
        "    'Cannot delete');");
  }

  public final void testDeleteNonLvalue() throws Exception {
    checkFails("delete 4;", "Invalid operand to delete");
  }

  public final void testFuncAnonSimple() throws Exception {
    assertConsistent(
        "var foo = (function () {" +
        "             function foo() {}" +
        "             foo.x = 3;" +
        "             return foo;" +
        "           })();" +
        "foo();" +
        "foo.x;");
  }

  public final void testFuncNamedSimpleDecl() throws Exception {
    rewriteAndExecute(
        "(function () {" +
        "  function foo() {}" +
        "  Object.freeze(foo);" +
        "  foo();" +
        "  try {" +
        "    foo.x = 3;" +
        "  } catch (e) { return; }" +
        "  fail('mutated frozen function');" +
        "})();");
    assertConsistent(
        "function foo() {}" +
        "foo.x = 3;" +
        "foo();" +
        "foo.x;");
    rewriteAndExecute(
        "  function f_() { return 31415; }"
        + "var x = f_();"
        + "assertEquals(x, 31415);");
  }

  public final void testMapSingle() throws Exception {
    checkFails("var o = { x___: p.x, k1: p.y };",
               "Properties cannot end in \"__\"");
  }

  public final void testInstanceof() throws Exception {
    assertConsistent("[ (({}) instanceof Object)," +
                     "  ((new Date) instanceof Date)," +
                     "  (({}) instanceof Date)" +
                     "];");
    assertConsistent("function foo() {}  (new foo) instanceof foo;");
    assertConsistent("function foo() {}  !(({}) instanceof foo);");
  }

  public final void testTypeof() throws Exception {
    rewriteAndExecute("assertThrowsMsg(function () { return typeof ___; }," +
        "'double underscore')");
    assertConsistent("typeof true;");
    assertConsistent("typeof 0;");
    assertConsistent("typeof undefined;");
    assertConsistent("typeof null;");
    assertConsistent("typeof 'string';");
    assertConsistent("typeof function () {};");
    assertConsistent("typeof ({});");
  }

  public final void testMaskingFunction() throws Exception {
    assertAddsMessage(
        "function Goo() { function Goo() {} }",
        MessageType.SYMBOL_REDEFINED,
        MessageLevel.LINT );
    assertAddsMessage(
        "function Goo() { var Goo = 1; }",
        MessageType.MASKING_SYMBOL,
        MessageLevel.LINT );
    assertMessageNotPresent(
        "function Goo() { this.x = 1; }",
        MessageType.MASKING_SYMBOL );
  }

  public final void testMapBadKeySuffix() throws Exception {
    checkAddsMessage(
        js(fromString("var o = { x__: 3 };")),
        RewriterMessageType.PROPERTIES_CANNOT_END_IN_DOUBLE_UNDERSCORE);
  }

  public final void testMapNonEmpty() throws Exception {
    // Ensure that calling an untamed function throws
    rewriteAndExecute(
        "testImports.f = function() {};" +
        "___.grantRead(testImports, 'f');",
        "assertThrowsMsg(function() { f(); }, 'toxic');",
        "");
    // Ensure that calling a tamed function in an object literal works
    rewriteAndExecute(
        "  var f = function() {};"
        + "var m = { f : f };"
        + "m.f();");
    // Ensure that putting an untamed function into an object literal
    // causes an exception.
    rewriteAndExecute(
        "testImports.f = function() {};" +
        "___.grantRead(testImports, 'f');",
        "assertThrowsMsg(function(){({ isPrototypeOf : f });}, 'toxic');",
        ";");
  }

  public final void testLabeledStatement() throws Exception {
    checkFails("IMPORTS___: 1;", "Labels cannot end in \"__\"");
    checkFails("IMPORTS___: while (1);", "Labels cannot end in \"__\"");
    checkFails("while (1) { break x__; }", "Labels cannot end in \"__\"");
    checkFails("while (1) { continue x__; }", "Labels cannot end in \"__\"");
    assertConsistent(
        "var k = 0;" +
        "a: for (var i = 0; i < 10; ++i) {" +
        "  b: for (var j = 0; j < 10; ++j) {" +
        "    if (++k > 5) break a;" +
        "  }" +
        "}" +
        "k;");
    assertConsistent(
        "var k = 0;" +
        "a: for (var i = 0; i < 10; ++i) {" +
        "  b: for (var j = 0; j < 10; ++j) {" +
        "    if (++k > 5) break b;" +
        "  }" +
        "}" +
        "k;");
  }

  /**
   * Tests that the container can get access to
   * "virtual globals" defined in cajoled code.
   */
  public final void testWrapperAccess() throws Exception {
    rewriteAndExecute(
        "",
        "var x = 'test';",
        "if (___.getNewModuleHandler().getImports().x != 'test') {" +
          "fail('Cannot see inside the wrapper');" +
        "}");
  }

  /**
   * Tests that Object.prototype cannot be modified.
   */
  public final void testFrozenObjectPrototype() throws Exception {
    rewriteAndExecute(
        "var success = false;" +
        "try {" +
          "Object.prototype.x = 'X';" +
        "} catch (e){" +
          "success = true;" +
        "}" +
        "if (!success) { fail('Object.prototype not frozen.'); }");
  }

  public final void testStamp() throws Exception {
    rewriteAndExecute(
        "function Foo(){}" +
        "var foo = new Foo();" +
        "Object.freeze(foo);" +
        "var TestMark = cajaVM.Trademark('Test');" +
        "var passed = false;" +
        "try { " +
        "  cajaVM.stamp([TestMark.stamp], foo);" +
        "} catch (e) {" +
        "  if (e.message !== " +
        "      'Can\\'t stamp frozen objects: [object Object]') {" +
        "    fail(e.message);" +
        "  }" +
        "  passed = true;" +
        "}" +
        "if (!passed) { fail ('Able to stamp frozen objects.'); }");
    rewriteAndExecute(
        // Shows how privileged or uncajoled code can stamp
        // frozen objects anyway.
        "___.getNewModuleHandler()." +
        "    getImports().DefineOwnProperty___('stampAnyway', {" +
        "      value: ___.markFuncFreeze(function(stamp, obj) {" +
        "          stamp.mark___(obj);" +
        "        })," +
        "      enumerable: false," +
        "      writable: true," +
        "      configurable: false" +
        "    });",
        "function Foo(){}" +
        "var foo = new Foo();" +
        "Object.freeze(foo);" +
        "var TestMark = cajaVM.Trademark('Test');" +
        "try { " +
        "  stampAnyway(TestMark.stamp, foo);" +
        "} catch (e) {" +
        "  fail(e.message);" +
        "}" +
        "cajaVM.guard(TestMark.guard, foo);",
        "");
    rewriteAndExecute(
        "var foo = {};" +
        "var TestMark = cajaVM.Trademark('Test');" +
        "cajaVM.stamp([TestMark.stamp], foo);" +
        "cajaVM.guard(TestMark.guard, foo);");
    rewriteAndExecute(
        "var foo = {};" +
        "var TestMark = cajaVM.Trademark('Test');" +
        "cajaVM.stamp([TestMark.stamp], foo);" +
        "TestMark.guard.coerce(foo);");
    rewriteAndExecute(
        "var foo = {};" +
        "var TestMark = cajaVM.Trademark('Test');" +
        "var passed = false;" +
        "try { " +
        "  cajaVM.guard(TestMark.guard, foo);" +
        "} catch (e) {" +
        "  if (e.message !== " +
        "      'Specimen does not have the \"Test\" trademark') {" +
        "    fail(e.message);" +
        "  }" +
        "  passed = true;" +
        "}" +
        "if (!passed) { fail ('Able to forge trademarks.'); }");
    rewriteAndExecute(
        "var foo = {};" +
        "var T1Mark = cajaVM.Trademark('T1');" +
        "var T2Mark = cajaVM.Trademark('T2');" +
        "var passed = false;" +
        "try { " +
        "  cajaVM.stamp([T1Mark.stamp], foo);" +
        "  cajaVM.guard(T2Mark.guard, foo);" +
        "} catch (e) {" +
        "  if (e.message !== 'Specimen does not have the \"T2\" trademark') {" +
        "    fail(e.message);" +
        "  }" +
        "  passed = true;" +
        "}" +
        "if (!passed) { fail ('Able to forge trademarks.'); }");
    rewriteAndExecute(
        "var foo = {};" +
        "var bar = Object.create(foo);" +
        "var baz = Object.create(bar);" +
        "var TestMark = cajaVM.Trademark('Test');" +
        "cajaVM.stamp([TestMark.stamp], bar);" +
        "assertFalse(cajaVM.passesGuard(TestMark.guard, foo));" +
        "assertTrue(cajaVM.passesGuard(TestMark.guard, bar));" +
        "assertFalse(cajaVM.passesGuard(TestMark.guard, baz));");
  }

  public final void testIndexOf() throws Exception {
    assertConsistent("''.indexOf('1');");
  }

  public final void testCallback() throws Exception {
    assertConsistent(
        "(function(){}).apply.call(function(a, b) {return a + b;}, {}, [3, 4]);"
        );
    assertConsistent(
        "(function(){}).call.call(function(a, b) {return a + b;}, {}, 3, 4);");
    rewriteAndExecute(
        "var a = [], b = {x:3};\n" +
        "for (var i in b) { a.push(i, b[i]); };" +
        "assertEquals(a.toString(), 'x,3');");
    assertConsistent(
        "Function.prototype.apply.call(" +
        "    function(a, b) {" +
        "      return a + b;" +
        "    }, " +
        "    {}, " +
        "    [3, 4]);");
    assertConsistent(
        "Function.prototype.call.call(" +
        "    function(a, b) {" +
        "      return a + b;" +
        "    }," +
        "    {}," +
        "    3," +
        "    4);");
    assertConsistent(
        "Function.prototype.bind.call(" +
        "    function(a, b) {" +
        "      return a + b;" +
        "    }," +
        "    {}," +
        "    3)(4);");
  }

  /**
   * Tests the WeakMap(opt_useKeyLifetime) abstraction.
   * <p>
   * From here, we are not in a position to test the weak-GC properties this
   * abstraction is designed to provide, nor its O(1) complexity measure.
   * However, we can test that it works as a simple lookup table.
   */
  public final void testTable() throws Exception {
    rewriteAndExecute(
        "var t = new WeakMap();" +
        "var k1 = {};" +
        "var k2 = {};" +
        "var k3 = {};" +
        "t.set(k1, 'v1');" +
        "t.set(k2, 'v2');" +
        "assertEquals(t.get(k1), 'v1');" +
        "assertEquals(t.get(k2), 'v2');" +
        "assertTrue(t.get(k3) === void 0);");
    rewriteAndExecute(
        "var t = new WeakMap(true);" +
        "var k1 = {};" +
        "var k2 = {};" +
        "var k3 = {};" +
        "t.set(k1, 'v1');" +
        "t.set(k2, 'v2');" +
        "assertEquals(t.get(k1), 'v1');" +
        "assertEquals(t.get(k2), 'v2');" +
        "assertTrue(t.get(k3) === void 0);");
    rewriteAndExecute(
        "var t = new WeakMap(true);" +
        "assertThrowsMsg(function(){t.set('foo', 'v1');}," +
        "    'primitive keys');");
    rewriteAndExecute(
        "var t = new WeakMap(true);" +
        "var k1 = {};" +
        "var k2 = Object.create(k1);" +
        "var k3 = Object.create(k2);" +
        "var k4 = Object.create(k3);" +
        "t.set(k2, 'foo');" +
        "t.set(k3, 'bar');" +
        "assertEquals(t.get(k2), 'foo');\n" +
        "assertEquals(t.get(k3), 'bar');\n" +
        "assertTrue(t.get(k1) === void 0);\n" +
        "assertTrue(t.get(k4) === void 0);");
    rewriteAndExecute(
        "var t = new WeakMap();" +
        "var k1 = {};" +
        "var k2 = Object.create(k1);" +
        "var k3 = Object.create(k2);" +
        "var k4 = Object.create(k3);" +
        "t.set(k2, 'foo');" +
        "t.set(k3, 'bar');" +
        "assertEquals(t.get(k2), 'foo');\n" +
        "assertEquals(t.get(k3), 'bar');\n" +
        "assertTrue(t.get(k1) === void 0);\n" +
        "assertTrue(t.get(k4) === void 0);");
    rewriteAndExecute(
        "var t1 = new WeakMap(true);" +
        "var t2 = new WeakMap(true);" +
        "var k = {};" +
        "t1.set(k, 'foo');" +
        "t2.set(k, 'bar');" +
        "assertEquals(t1.get(k), 'foo');" +
        "assertEquals(t2.get(k), 'bar');" +
        "t1.set(k, void 0);" +
        "assertTrue(t1.get(k) === void 0);" +
        "assertEquals(t2.get(k), 'bar');");
    rewriteAndExecute(
        "var t1 = new WeakMap();" +
        "var t2 = new WeakMap();" +
        "var k = {};" +
        "t1.set(k, 'foo');" +
        "t2.set(k, 'bar');" +
        "assertEquals(t1.get(k), 'foo');" +
        "assertEquals(t2.get(k), 'bar');" +
        "t1.set(k, void 0);" +
        "assertTrue(t1.get(k) === void 0);" +
        "assertEquals(t2.get(k), 'bar');");
  }

  /**
   * Tests that begetting works.
   */
  public final void testInheritance() throws Exception {
    rewriteAndExecute(
        "var x = {a:8}, y = Object.create(x); assertTrue(y.a === 8);");
  }

  /**
   * Tests that ES5/3 code can't cause a privilege
   * escalation by calling a tamed exophoric function with null as the
   * this-value.
   * <p>
   * The uncajoled branch of the tests below establish that a null does cause
   * a privilege escalation for normal non-strict JavaScript.
   */
  public final void testNoPrivilegeEscalation() throws Exception {
    rewriteAndExecute("assertTrue([].valueOf.call(null) === cajaVM.USELESS);");
    rewriteAndExecute("assertTrue([].valueOf.apply(null) === cajaVM.USELESS);");
    rewriteAndExecute(
        "assertTrue([].valueOf.bind(null)() === cajaVM.USELESS);");
  }

  /**
   * Tests that the apparent [[Class]] of the tamed JSON object is 'JSON', as
   * it should be according to ES5.  Also tests parse and stringify.
   *
   * See issue 1086
   */
  public final void testJSONClass() throws Exception {
    rewriteAndExecute("assertTrue(''+JSON === '[object JSON]');");
    rewriteAndExecute(
        "assertTrue(({}).toString.call(JSON) === '[object JSON]');");
    rewriteAndExecute(
        "var x = JSON.parse('{\"a\":[{\"b\":33}]}');" +
        "assertEquals(33, x.a[0].b);");
    rewriteAndExecute(
        "var x = JSON.stringify({a:33});" +
        "assertEquals('{\"a\":33}', x);");
    rewriteAndExecute(
        "var pass = false;" +
        "try { var x = JSON.parse('{\"b\":1, \"a___\":33}'); }" +
        "catch (e) { " +
        "  assertTrue(e.message.indexOf('a___') !== -1);" +
        "  pass = true;" +
        "}" +
        "assertTrue(pass);");
  }

  /**
   * Tests Object.getPrototypeOf().
   */
  public final void testGetPrototypeOf() throws Exception {
    rewriteAndExecute(
        "assertEquals(Object.getPrototypeOf({}), Object.prototype);");
    rewriteAndExecute(
        "assertEquals(Object.getPrototypeOf([]), Array.prototype);");
    rewriteAndExecute(
        "function Foo() {}" +
        "var foo = new Foo();" +
        "assertEquals(Object.getPrototypeOf(foo), Foo.prototype);");
  }

  /**
   * Tests Array.forEach
   */
  public final void testArrayForEach() throws Exception {
    rewriteAndExecute(
        "var testArray = ['a', 'b', 'c'];" +
        "var expectedI = 0;" +
        "testArray.forEach(function(item, index, orig) {" +
        "  assertEquals(item, testArray[expectedI]);" +
        "  assertEquals(index, expectedI);" +
        "  assertEquals(orig, testArray);" +
        "  expectedI++; " +
        "});");
  }

  /**
   * Tests assigning to an inherited read-only property
   */
  public final void testCanPut() throws Exception {
    // ES5/3 is consistent with Chrome and Safari in asserting that the
    // ES5 spec was mistaken to prohibit overriding read-only properties.
    // http://wiki.ecmascript.org/doku.php?id=strawman:fixing_override_mistake
    rewriteAndExecute(
        "var a = {};" +
        "Object.defineProperty(a, 'x', {value: 1, writable: false});" +
        "var b = Object.create(a);" +
        "b.x = 2;" +
        "assertEquals(b.x, 2);");
  }

  /**
   * Regression test: Object.defineProperties was nonfunctional.
   */
  public final void testDefineProperties() throws Exception {
    rewriteAndExecute(
        "var o = {};" +
        "Object.defineProperties(o, " +
        "  {p1: {value:1}, p2: {value:2, enumerable:true}});" +
        "function pd(p) { return Object.getOwnPropertyDescriptor(o, p); }" +
        "assertEquals(pd('p1').enumerable, false);" +
        "assertEquals(pd('p2').enumerable, true);" +
        "assertEquals(o.p1, 1);" +
        "assertEquals(o.p2, 2);");
  }

  /**
   * Test that objects inheriting properties marked as writable but not yet
   * fastpathed are actually writable.
   */
  public final void testWritableInheritance() throws Exception {
    rewriteAndExecute(
        "var o = {};" +
        "o.x = 1;" +
        "var o2 = Object.create(o);" +
        "o2.x = 2;");
  }

  /**
   * Tests that assignment returns the rhs, not the result of a setter.
   */
  public final void testAssignmentReturnValue() throws Exception {
    rewriteAndExecute(
        "var o = {};" +
        "Object.defineProperty(o, 'x', {set:function(v) { return 5; }});" +
        "assertEquals(1, o.x=1);");
  }

  public final void testExtensibile() throws Exception {
    rewriteAndExecute(
        "var o = {};" +
        "o.x = 1;" +
        "var o2 = Object.create(o);" +
        "Object.preventExtensions(o2);" +
        "assertThrowsMsg(function () { o2.x = 2; }, 'extensible');");
    rewriteAndExecute(
        "var o = {};" +
        "var o2 = Object.create(o);" +
        "Object.preventExtensions(o2);" +
        "assertThrowsMsg(function () { Array.prototype.sort.call(o2); }," +
        "    'extensible');");
    rewriteAndExecute(
        "var o = {};" +
        "o.x = 1;" +
        "var o2 = Object.create(o);" +
        "o2.y = 1;" +
        "Object.seal(o2);" +
        "assertThrowsMsg(function () { o2.x = 2; }, 'extensible');" +
        "var desc = Object.getOwnPropertyDescriptor(o2, 'y');" +
        "assertEquals(desc.configurable, false);");
  }

  public final void testProxy() throws Exception {
      rewriteAndExecute(
          // Taken from http://wiki.ecmascript.org/doku.php?id=harmony:proxies
          "Object.freeze(Function.prototype);" +
          "function handlerMaker(obj) {" +
          "  return {" +
          "    getOwnPropertyDescriptor: function(name) {" +
          "      var desc = Object.getOwnPropertyDescriptor(obj, name);" +
          "      if (desc !== undefined) { desc.configurable = true; }" +
          "      return desc;" +
          "    }," +
          "    getPropertyDescriptor: function(name) {" +
          "      return {" +
          "          value: obj[name]," +
          "          configurable:true," +
          "          enumerable:true," +
          "          writable:true" +
          "        };" +
          "    }," +
          "    getOwnPropertyNames: function() {" +
          "      return Object.getOwnPropertyNames(obj);" +
          "    }," +
          "    getPropertyNames: function() {" +
          "      return cajaVM.allKeys(obj);" +
          "    }," +
          "    defineProperty: function(name, desc) {" +
          "      Object.defineProperty(obj, name, desc);" +
          "    }," +
          "    delete: function(name) { return delete obj[name]; }," +
          "    fix: function() {" +
          "      var names = Object.getOwnPropertyNames(obj);" +
          "      var len = names.length;" +
          "      var result = {};" +
          "      for (var i = 0; i < len; ++i) {" +
          "        result[names[i]] = Object.getOwnPropertyDescriptor(" +
          "            obj, names[i]);" +
          "      }" +
          "      return result;" +
          "    }," +
          "    has: function(name) { return name in obj; }," +
          "    hasOwn: function(name) {" +
          "      return ({}).hasOwnProperty.call(obj, name);" +
          "    }," +
          "    get: function(name) { return obj[name]; }," +
          "    set: function(name, val) {" +
          "      obj[name] = val;" +
          "      return true;" +
          "    }, " +
          "    enumerate: function() {" +
          "      var result = [];" +
          "      for (var name in obj) { result.push(name); };" +
          "      return result;" +
          "    }," +
          "    keys: function() { return Object.keys(obj); }" +
          "  };" +
          "}" +
          "var o1 = {x: 1, y:function(x) { return x*2; }};" +
          "var obj = Object.create(o1);" +
          "obj.z = 3;" +
          "var proxy = Proxy.create(handlerMaker(obj));" +
          "cajaVM.log('for-in'); var keys = [];" +
          "  for (var i in proxy) { keys.push(i); }" +
          "  assertEquals('x,y,z', ''+keys.sort());" +
          "cajaVM.log('get'); assertEquals(1, proxy.x);" +
          "cajaVM.log('method'); assertEquals(10, proxy.y(5));" +
          "cajaVM.log('has'); assertTrue('x' in proxy);" +
          "cajaVM.log('hasOwn'); " +
          "  assertTrue(({}).hasOwnProperty.call(proxy, 'z'));" +
          "cajaVM.log('set'); proxy.x = 2; assertEquals(2, proxy.x);" +
          "cajaVM.log('desc value');" +
          "  var desc = Object.getOwnPropertyDescriptor(proxy, 'z');" +
          "  assertEquals(3, desc.value);" +
          "cajaVM.log('desc conf'); assertTrue(desc.configurable);" +
          "cajaVM.log('desc writ'); assertTrue(desc.writable);" +
          "cajaVM.log('fix pe');" +
          "  Object.preventExtensions(proxy);" +
          "  assertEquals(false, Object.isExtensible(proxy));" +
          "cajaVM.log('fix seal');" +
          "  proxy = Proxy.create(handlerMaker(obj));" +
          "  Object.seal(proxy);" +
          "  assertEquals(false, Object.isExtensible(proxy));" +
          "  desc = Object.getOwnPropertyDescriptor(proxy, 'z');" +
          "  assertEquals(false, desc.configurable);" +
          "cajaVM.log('fix freeze and arrays');" +
          "  proxy = Proxy.create(handlerMaker(['hi', 'there']));" +
          "  /* can't intercept numerics */" +
          "  assertEquals(void 0, proxy[1]);" +
          "  Object.freeze(proxy);" +
          "  assertTrue(Object.isFrozen(proxy));" +
          "  /* can have numeric properties after fixing */" +
          "  assertEquals('there', proxy[1]);" +
          "cajaVM.log('keys');" +
          "  proxy = Proxy.create(handlerMaker(obj));" +
          "  assertEquals('x,z', ''+Object.keys(proxy).sort());" +
          "cajaVM.log('defineProperty');" +
          "  Object.defineProperty(proxy, 'w', { value: 4 });" +
          "  assertEquals(4, obj.w);" +
          "cajaVM.log('extensibility');" +
          "  var o2 = {};" +
          "  var o3 = Object.create(o2);" +
          "  assertThrowsMsg(function(){Proxy.create(handlerMaker({}), o3);}," +
          "      'extensible');" +
          "  Object.preventExtensions(o3);" +
          "  assertThrowsMsg(function(){Proxy.create(handlerMaker({}), o3);}," +
          "      'extensible');" +
          "  Object.preventExtensions(o2);" +
          "  Proxy.create(handlerMaker({}), o3);" +
          "cajaVM.log('function as object');" +
          "  var f = function joe(){return 3;};" +
          "  proxy = Proxy.create(handlerMaker(f));" +
          "  assertEquals('joe', proxy.name);" +
          "cajaVM.log('function proxy');" +
          "  proxy = Proxy.createFunction(handlerMaker(f)," +
          "      function(){return 5;}," +
          "      function(){" +
          "        this.x = 1; " +
          "        this.f = function(){return this.x;};" +
          "      });" +
          "  assertEquals('joe', proxy.name);" +
          "  assertEquals(5, proxy());" +
          "  assertEquals(1, (new proxy()).f());" +
          "cajaVM.log('missing get');" +
          "  var noGetHandler = handlerMaker(obj);" +
          "  delete noGetHandler.get;" +
          "  proxy = Proxy.create(noGetHandler);" +
          "  assertEquals(3, proxy.z);" +
          "  /* proxy.x assigned to 2 in set test */" +
          "  assertEquals(2, proxy.x);" +
          "cajaVM.log('getOwnPropertyNames');" +
          "  proxy = Proxy.create(handlerMaker(obj));" +
          "  assertEquals('w,x,z'," +
          "      '' + Object.getOwnPropertyNames(proxy).sort());" +
          "  var o4 = {d:4};" +
          "  Object.freeze(o4);" +
          "  proxy = Proxy.create(handlerMaker({a:1, b:2, c:3}), o4);" +
          "  assertEquals('a,b,c'," +
          "      '' + Object.getOwnPropertyNames(proxy).sort());" +
          "cajaVM.log('getPropertyNames');" +
          "  var allKeys = cajaVM.allKeys(proxy).sort();" +
          "  assertEquals('a,b,c', '' + allKeys.slice(0,3));" +
          "  assertTrue('d' !== allKeys[3]);");
  }

  public final void testElision() throws Exception {
    rewriteAndExecute("var x = [0,,2,];\n" +
        "assertEquals(3, x.length);" +
        "assertEquals(0, x[0]);" +
        "assertEquals(undefined, x[1]);" +
        "assertEquals(2, x[2]);"
    );
  }

  public final void testBindOk() throws Exception {
    // Checks that bound functions get marked as safe to execute.
    rewriteAndExecute("{foo: (function (){}).bind(Number)}");
  }

  public final void testNoSetter() throws Exception {
    // Checks that configurable properties with no setter
    // and no backing property.
    rewriteAndExecute(
        "testImports.x = {};" +
        "testImports.x.DefineOwnProperty___('g'," +
        "    {configurable: true, get:___.markFunc(function(){}) });" +
        "delete testImports.x.g;" +
        "___.grantRead(testImports,'x');",
        "assertThrowsMsg(function(){x.g = 1;}, 'no setter');",
        "");
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    es53Rewriter = new ES53Rewriter(TestBuildInfo.getInstance(), mq, false);
    setRewriter(es53Rewriter);
  }

  private static String assertThrowsMsg =
      "function assertThrowsMsg(f, msg) {\n" +
      "  try { f(); } catch (e) {\n" +
      "    assertTrue(e.message.indexOf(msg) > -1);\n" +
      "    return true;\n" +
      "  }\n" +
      "  return false;\n" +
      "}\n";

  @Override
  protected Object executePlain(String caja) throws IOException {
    mq.getMessages().clear();
    return RhinoTestBed.runJs(
        new Executor.Input(
            getClass(), "../../../../../js/jsunit/2.2/jsUnitCore.js"),
        new Executor.Input(assertThrowsMsg, "assertThrowsMsg"),
        new Executor.Input(caja, getName() + "-uncajoled"));
  }

  @Override
  protected Object rewriteAndExecute(String pre, String caja, String post)
      throws IOException, ParseException {
    mq.getMessages().clear();

    List<Statement> children = Lists.newArrayList();
    children.add(js(fromString(caja, is)));
    String cajoledJs = render(rewriteTopLevelNode(
        new UncajoledModule(new Block(FilePosition.UNKNOWN, children))));

    assertNoErrors();

    final String[] assertFunctions = new String[] {
        "fail",
        "assertEquals",
        "assertTrue",
        "assertFalse",
        "assertLessThan",
        "assertNull",
        "assertThrows",
        "assertThrowsMsg",
    };

    StringBuilder importsSetup = new StringBuilder();
    importsSetup.append(
        "var testImports = ___.copy(___.whitelistAll(___.sharedImports));");
    for (String f : assertFunctions) {
      importsSetup
          .append("testImports." + f + " = ___.markFuncFreeze(" + f + ");")
          .append("___.grantRead(testImports, '" + f + "');");
    }
    importsSetup.append(
        "___.getNewModuleHandler().setImports(___.whitelistAll(testImports));");

    Object result = RhinoTestBed.runJs(
        new Executor.Input(
            getClass(), "/com/google/caja/plugin/console-stubs.js"),
        new Executor.Input(
            getClass(), "../../../../../js/json_sans_eval/json_sans_eval.js"),
        new Executor.Input(getClass(), "/com/google/caja/es53.js"),
        new Executor.Input(
            getClass(), "../../../../../js/jsunit/2.2/jsUnitCore.js"),
        new Executor.Input(assertThrowsMsg, "assertThrowsMsg"),
        new Executor.Input(
            getClass(), "/com/google/caja/log-to-console.js"),
        new Executor.Input(
            importsSetup.toString(),
            getName() + "-test-fixture"),
        new Executor.Input(pre, getName()),
        // Load the cajoled code.
        new Executor.Input(cajoledJs, getName() + "-cajoled"),
        new Executor.Input(post, getName()),
        // Return the output field as the value of the run.
        new Executor.Input(
            "___.getNewModuleHandler().getLastValue();", getName()));

    assertNoErrors();
    return result;
  }
}
