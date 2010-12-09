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

import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CajoledModule;
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
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.util.Executor;
import com.google.caja.util.FailureIsAnOption;
import com.google.caja.util.RhinoTestBed;
import com.google.caja.reporting.TestBuildInfo;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author metaweta@gmail.com
 */
public class DefaultValijaRewriterTest extends CommonJsRewriterTestCase {
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

  private Rewriter valijaRewriter;
  private CajitaModuleRewriter cajitaModuleRewriter;
  private Rewriter innocentCodeRewriter;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    valijaRewriter = new DefaultValijaRewriter(mq, false);
    cajitaModuleRewriter = new CajitaModuleRewriter(
        new PluginMeta(), new TestBuildInfo(), new TestUriFetcher(), true, mq);
    innocentCodeRewriter = new InnocentCodeRewriter(mq, false);
    // Start with this one, then switch later to CajitaRewriter for
    // the second pass.
    setRewriter(valijaRewriter);
  }

  public final void testSyntheticIsUntouched() throws Exception {
    ParseTreeNode input = js(fromString("function foo() { this; arguments; }"));
    syntheticTree(input);
    checkSucceeds(
        input,
        js(fromString(
            ""
            + "var $dis = $v.getOuters();"
            + "$v.initOuter('onerror');"
            + "function foo() { this; arguments; }")));
  }

  public final void testSyntheticMemberAccess() throws Exception {
    ParseTreeNode input = js(fromString("({}).foo"));
    syntheticTree(input);
    checkSucceeds(
        input,
        js(fromString(
            ""
            + "var $dis = $v.getOuters();"
            + "$v.initOuter('onerror');"
            + "({}).foo")));
  }

  public final void testSyntheticNestedIsExpanded() throws Exception {
    Statement innerInput = js(fromString("function foo() {}"));
    Block input = new Block(FilePosition.UNKNOWN, Arrays.asList(innerInput));
    makeSynthetic(input);
    ParseTreeNode expectedResult = js(fromString(
        ""
        + "var $dis = $v.getOuters();"
        + "$v.initOuter('onerror');"
        + "{"
        + "  $v.so('foo', (function () {"
        + "                  var foo;"
        + "                  function foo$_caller($dis) {}"
        + "                  foo = $v.dis(foo$_caller, 'foo');"
        + "                  return foo;"
        + "                })());;"
        + "}"));
    checkSucceeds(input, expectedResult);
  }

  public final void testSyntheticNestedFunctionIsExpanded() throws Exception {
    // This test checks that a synthetic function, as is commonly generated
    // by Caja to wrap JavaScript event handlers declared in HTML, is rewritten
    // correctly.
    Block innerBlock = js(fromString("foo().x = bar();"));
    ParseTreeNode input = new Block(
        FilePosition.UNKNOWN,
        Arrays.asList(
            new FunctionDeclaration(
                SyntheticNodes.s(new FunctionConstructor(
                    FilePosition.UNKNOWN,
                    new Identifier(FilePosition.UNKNOWN, "f"),
                    Collections.<FormalParam>emptyList(),
                    innerBlock)))));
    // We expect the stuff in 'innerBlock' to be expanded, *but* we expect the
    // rewriter to be unaware of the enclosing function scope, so the temporary
    // variables generated by expanding 'innerBlock' spill out and get declared
    // outside the function rather than inside it.
    ParseTreeNode expectedResult = js(fromString(
        ""
        + "var $dis = $v.getOuters();"
        + "$v.initOuter('onerror');"
        + "function f() {"
        + "  $v.s($v.cf($v.ro('foo'), []), 'x', $v.cf($v.ro('bar'), []));"
        + "}"
        ));
    checkSucceeds(input, expectedResult);
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
            + "var $dis = $v.getOuters();"
            + "$v.initOuter('onerror');"
            // Since the function is not synthetic, it's exported to outers.
            + "$v.so('f', (function () {"
            + "  var f;"
            + "  function f$_caller($dis, x, y___) {"
            // x and y___ are formals, but z is free to the function.
            + "    return (x + y___) * $v.ro('z');"
            + "  }"
            + "  f = $v.dis(f$_caller, 'f');"
            + "  return f;"
            + "})());;")));

    SyntheticNodes.s(fc);
    checkSucceeds(
        new Block(
            unk,
            Arrays.asList(
                new FunctionDeclaration((FunctionConstructor) fc.clone()))),
        js(fromString(
            ""
            + "var $dis = $v.getOuters();"
            + "$v.initOuter('onerror');"
            // Since the function is synthetic, it is not attached to outers.
            + "function f(x, y___) {"
            // x and y___ are formals, but z is free to the function.
            + "  return (x + y___) * $v.ro('z');"
            + "}"
            )));
  }

  public final void testConstant() throws Exception {
    assertConsistent("1;");
  }

  public final void testInit() throws Exception {
    assertConsistent("var a = 0; a;");
  }

  public final void testNew() throws Exception {
    assertConsistent("function f() { this.x = 1; } f; var g = new f(); g.x;");
  }

  public final void testThrowCatch() throws Exception {
    assertConsistent(
        "var x = 0; try { throw 1; }" +
        "catch (e) { x = e; }" +
        "x;");
    assertConsistent(
        "var x = 0; try { throw { a: 1 }; }" +
        "catch (e) { x = e; }" +
        "x;");
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
        "WP.prototype = cajita.beget(Point.prototype);\n" +
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

  public final void testIndexOf() throws Exception {
    assertConsistent("'foo'.indexOf('o');");
  }

  public final void testFunctionToStringCall() throws Exception {
    rewriteAndExecute(
        "function foo() {}\n"
        + "assertEquals(foo.toString(),\n"
        + "             'function foo() {\\n  [cajoled code]\\n}');");
    rewriteAndExecute(
        "function foo (a, b) { xx; }\n"
        + "assertEquals(foo.toString(),\n"
        + "             'function foo(a, b) {\\n  [cajoled code]\\n}');");
    rewriteAndExecute(
        "function foo() {}\n"
        + "assertEquals(Function.prototype.toString.call(foo),\n"
        + "             'function foo() {\\n  [cajoled code]\\n}');");
    rewriteAndExecute(
        "var foo = function (x$x, y_y) {};\n"
        + "assertEquals(Function.prototype.toString.call(foo),\n"
        + "             'function foo$_var(x$x, y_y) {\\n  [cajoled code]\\n}');");
  }

  public final void testUnderscore() throws Exception {
    rewriteAndExecute(
         ""
         + "var msg;"
         + "try {"
         + "  var x__ = 1;"
         + "  x__;"
         + "} catch (ex) {"
         + "  msg = ex.message;"
         + "}"
         + "assertEquals('Not writable: ([Object]).x__', msg);");
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

  public final void testForIn2() throws Exception {
    assertConsistent(
        "(function () {" +
        "  var str = '';" +
        "  for (i in { x: 1, y: true }) { str += i; }" +
        "  return str;" +
        "})();");
    assertConsistent(
        "(function () {" +
        "  var str = '';" +
        "  for (var i in { x: 1, y: true }) { str += i; }" +
        " return str;" +
        "})();");
    assertConsistent(
        "str = ''; for (i in { x: 1, y: true }) { str += i; } str;");
    assertConsistent(
        "str = ''; for (var i in { x: 1, y: true }) { str += i; } str;");
  }

  public final void testValueOf() throws Exception {
     rewriteAndExecute(
         ""
         + "var msg;"
         + "try {"
         + "  var k = 'valueOf';"
         + "  var o = {};"
         + "  o[k] = function (hint) { return 2; };"
         + "} catch (ex) {"
         + "  msg = ex.message;"
         + "}"
         + "assertEquals('Not writable: ([Object]).valueOf', msg);"
         );
  }

  /**
   * Tests that the container can get access to
   * "virtual globals" defined in cajoled code.
   */
  public final void testWrapperAccess() throws Exception {
    // TODO(ihab.awad): SECURITY: Re-enable by reading (say) x.foo, and
    // defining the property IMPORTS___.foo.
    rewriteAndExecute(
        "",
        "x = 'test';",
        "if (___.getNewModuleHandler().getImports().x != 'test') {" +
          "fail('Cannot see inside the wrapper');" +
        "}");
  }

  /**
   * Try to construct some class instances.
   */
  public final void testFuncCtor() throws Exception {
    rewriteAndExecute(
        "function Foo(x) { this.x = x; }" +
        "var foo = new Foo(2);" +
        "if (!foo) { fail('Failed to construct a global object.'); }" +
        "assertEquals(foo.x, 2);");
    rewriteAndExecute(
        "(function () {" +
        "  function Foo(x) { this.x = x; }" +
        "  var foo = new Foo(2);" +
        "  if (!foo) { fail('Failed to construct a local object.'); }" +
        "  assertEquals(foo.x, 2);" +
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
        "cajita.getOwnPropertyNames(x).sort();");
    assertConsistent(
        "var x = [33];" +
        "Array.prototype.push.apply(x, [3,4,5]);" +
        "x;");
    assertConsistent(
        "var x = {blue:'green'};" +
        "Array.prototype.push.apply(x, [3,4,5]);" +
        "cajita.getOwnPropertyNames(x).sort();");
    assertConsistent(
        "var x = {blue:'green'};" +
        "x.foo = [].push;" +
        "x.foo.call(x, 44);" +
        "cajita.getOwnPropertyNames(x).sort();");
  }

  public final void testCallback() throws Exception {
    assertConsistent("Function.prototype.apply.call(function(a, b) {return a + b;}, {}, [3, 4]);");
    assertConsistent("Function.prototype.call.call(function(a, b) {return a + b;}, {}, 3, 4);");
    assertConsistent("Function.prototype.bind.call(function(a, b) {return a + b;}, {}, 3)(4);");
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
  }

  public final void testMonkeyPatchPrimordialFunction() throws Exception {
    assertConsistent(
        "isNaN.foo = 'bar';" +
        "isNaN.foo;");
  }

  /**
   * Although the golden output here tests many extraneous things, the point of
   * this test is to see that various anonymous functions in the original are
   * turned into named functions -- named after the variable or property
   * they are initializing.
   * <p>
   * The test is structured as as a call stack resulting in a breakpoint.
   * If executed, for example, in the testbed applet with Firebug enabled,
   * one should see the stack of generated function names.
   */
  public final void testFuncNaming() throws Exception {
    checkSucceeds(
        "function foo(){debugger;}" +
        "var x = {bar: function() {foo();}};" +
        "x.baz = function(){x.bar();};" +
        "var zip = function(){" +
        "  var lip = function(){x.baz();};" +
        "  var lap;" +
        "  lap = function(){lip();};" +
        "  lap();" +
        "};" +
        "var zap;" +
        "zap = function(){zip();};" +
        "zap();",

        "var $dis = $v.getOuters();" +
        "$v.initOuter('onerror');" +
        "$v.so('foo', (function () {" +
        "  var foo;" +
        "  function foo$_caller($dis) { debugger; }" +
        "  foo = $v.dis(foo$_caller, 'foo');" +
        "  return foo;" +
        "})());" +
        ";" +
        "$v.so('x',{" +
        "  'bar': (function () {" +
        "    function bar$_lit$($dis) {" +
        "      $v.cf($v.ro('foo'), []);" +
        "    }" +
        "    var bar$_lit = $v.dis(bar$_lit$, 'bar$_lit');" +
        "    return bar$_lit;" +
        "  })()" +
        "});" +
        "$v.s($v.ro('x'), 'baz', (function () {" +
        "  function baz$_meth$($dis) {" +
        "    $v.cm($v.ro('x'), 'bar', []);" +
        "  }" +
        "  var baz$_meth = $v.dis(baz$_meth$, 'baz$_meth');" +
        "  return baz$_meth;" +
        "})());" +
        "$v.so('zip', (function () {" +
        "  function zip$_var$($dis) {" +
        "    var lip = (function () {" +
        "      function lip$_var$($dis) {" +
        "        $v.cm($v.ro('x'), 'baz', [ ]);" +
        "      }" +
        "      var lip$_var = $v.dis(lip$_var$, 'lip$_var');" +
        "      return lip$_var;" +
        "    })();" +
        "    var lap;" +
        "    lap = (function () {" +
        "      function lap$_var$($dis) {" +
        "        $v.cf(lip, [ ]);" +
        "      }" +
        "      var lap$_var = $v.dis(lap$_var$, 'lap$_var');" +
        "      return lap$_var;" +
        "    })();" +
        "    $v.cf(lap, [ ]);" +
        "  }" +
        "  var zip$_var = $v.dis(zip$_var$, 'zip$_var');" +
        "  return zip$_var;" +
        "})());" +
        "$v.initOuter('zap');" +
        "$v.so('zap', (function () {" +
        "  function zap$_var$($dis) {" +
        "    $v.cf($v.ro('zip'), [ ]);" +
        "  }" +
        "  var zap$_var = $v.dis(zap$_var$, 'zap$_var');" +
        "  return zap$_var;" +
        "})());" +
        "$v.cf($v.ro('zap'), [ ]);");
  }

  @FailureIsAnOption
  public final void testInMonkeyDelete() throws Exception {
    assertConsistent(
        // TODO(erights): Fix when bug 953 is fixed.
        "delete Array.prototype.push;" +
        "('push' in []);");
  }

  @FailureIsAnOption
  public final void testMonkeyOverride() throws Exception {
    assertConsistent(
        // TODO(erights): Fix when bug 953 is fixed.
        "Date.prototype.propertyIsEnumerable = function(p) { return true; };" +
        "(new Date()).propertyIsEnumerable('foo');");
  }

  public final void testValijaTypeofConsistent() throws Exception {
    assertConsistent("[" +
        "  (typeof [].push)" +
        "];");
    rewriteAndExecute("assertEquals(typeof new RegExp('.*'), 'object');");
  }

  public final void testEmbeddedCajita() throws Exception {
    assertConsistent(
        ""
        + "\"use strict,cajita\"; \n"
        + "var foo; \n"
        + "(function () { \n"
        + "  foo = function () { return 8; }; \n"
        + "})(); \n"
        + "foo();"
        );
  }

  public final void testStaticModuleLoading() throws Exception {
    rewriteAndExecute(
        "includeScript('x');"
        + "assertEquals(x, 3);"
        );
  }

  /**
   * Tests that Error objects are not frozen by being caught by a Valija catch.
   *
   * See issue 1097, issue 1038,
   *     and {@link CommonJsRewriterTestCase#testErrorTaming()}}.
   */
  public final void testErrorFreeze() throws Exception {
    rewriteAndExecute(
            "try {" +
            "  throw new Error('foo');" +
            "} catch (ex) {" +
            "  assertFalse(cajita.isFrozen(ex));" +
            "}");
  }

  /**
   *
   */
  public final void testObjectFreeze() throws Exception {
    rewriteAndExecute( // records can be frozen
        "var r = Object.freeze({});" +
        "assertThrows(function(){r.foo = 8;});");
    rewriteAndExecute( // anon disfunctions are not frozen
        "var f = function(){};" +
        "f.foo = 8;");
    rewriteAndExecute( // anon functions are virtually not frozen
        "var f = function(){'use cajita';};" +
        "f.foo = 8;");
    rewriteAndExecute( // anon disfunctions can be frozen
        "var f = Object.freeze(function(){});" +
        "assertThrows(function(){f.foo = 8;});");
    rewriteAndExecute( // anon functions can be virtually frozen
        "var f = Object.freeze(function(){'use cajita';});" +
        "assertThrows(function(){f.foo = 8;});");
    rewriteAndExecute( // constructed objects cannot be frozen
        "function Point(x,y) {" +
        "  this.x = x;" +
        "  this.y = y;" +
        "}" +
        "___.markCtor(Point, Object, 'Point');" +
        "testImports.pt = new Point(3,5);" +
        "___.grantSet(testImports.pt, 'x');",

        "pt.x = 8;" +
        "assertThrows(function(){Object.freeze(pt);});",

        "");
    rewriteAndExecute( // pseudo-constructed objects can be frozen
        "function Point(x,y) {" +
        "  this.x = x;" +
        "  this.y = y;" +
        "}" +
        "var pt = new Point(3,5);" +
        "pt.x = 8;" +
        "Object.freeze(pt);" +
        "assertThrows(function(){pt.y = 9;});");
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
   * Tests that the apparent [[Class]] of the tamed JSON object is 'JSON', as
   * it should be according to ES5.
   *
   * See issue 1086
   */
  public final void testJSONClass() throws Exception {
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

  @Override
  protected Object executePlain(String caja)
      throws IOException, ParseException {
    mq.getMessages().clear();
    Rewriter old = getRewriter();
    setRewriter(innocentCodeRewriter);
    Statement innocentTree = (Statement) rewriteTopLevelNode(
        js(fromString(caja, is)));
    setRewriter(old);
    return RhinoTestBed.runJs(
        new Executor.Input(
            getClass(), "../../../../../js/json_sans_eval/json_sans_eval.js"),
        new Executor.Input(getClass(), "/com/google/caja/cajita.js"),
        new Executor.Input(
            getClass(), "../../../../../js/jsunit/2.2/jsUnitCore.js"),
        new Executor.Input(render(innocentTree), getName() + "-uncajoled"));

  }

  @Override
  protected Object rewriteAndExecute(String pre, String caja, String post)
      throws IOException, ParseException {
    mq.getMessages().clear();

    Rewriter old = getRewriter();
    setRewriter(valijaRewriter);
    Block valijaTree = js(fromString(caja, is));
    Block cajitaTree = (Block) rewriteTopLevelNode(valijaTree);
    CajoledModule cajoled = cajoleModule(new UncajoledModule(cajitaTree));
    String cajoledJs = render(cajoled);
    CajoledModule valijaBody = cajoleModule(
        new UncajoledModule(js(fromResource("../../valija-cajita.js"))));
    String valijaCajoled = render(valijaBody);
    setRewriter(old);

    assertNoErrors();

    Object result = RhinoTestBed.runJs(
        new Executor.Input(
            getClass(), "/com/google/caja/plugin/console-stubs.js"),
        new Executor.Input(
            getClass(), "../../../../../js/json_sans_eval/json_sans_eval.js"),
        new Executor.Input(getClass(), "/com/google/caja/cajita.js"),
        new Executor.Input(
            getClass(), "/com/google/caja/cajita-promise.js"),
        new Executor.Input(
            getClass(), "../../../../../js/jsunit/2.2/jsUnitCore.js"),
        new Executor.Input(
            getClass(), "/com/google/caja/log-to-console.js"),
        new Executor.Input(
            "var testImports = ___.copy(___.sharedImports);\n" +
            "testImports.Q = Q;" +
            "testImports.loader = ___.freeze({\n" +
            "        provide: ___.markFuncFreeze(\n" +
            "            function(v){ valijaMaker = v; })\n" +
            "    });\n" +
            "testImports.outers = ___.copy(___.sharedImports);\n" +
            "___.getNewModuleHandler().setImports(testImports);",
            getName() + "valija-setup"),
        new Executor.Input(valijaCajoled, "valija-cajoled"),
        new Executor.Input(
            // Set up the imports environment.
            "testImports = ___.copy(___.sharedImports);\n" +
            "testImports.Q = Q;" +
            "testImports.env = {x: 6};" +
            "testImports.console = console;" +
            "testImports.assertEquals = assertEquals;" +
            "___.grantFunc(testImports, 'assertEquals');" +
            "testImports.assertTrue = assertTrue;" +
            "___.grantFunc(testImports, 'assertTrue');" +
            "testImports.assertFalse = assertFalse;" +
            "___.grantFunc(testImports, 'assertFalse');" +
            "testImports.assertThrows = function(func, opt_msg) {" +
            "  assertThrows(___.toFunc(func), opt_msg);" +
            "};" +
            "___.grantFunc(testImports, 'assertThrows');" +
            "testImports.fail = fail;" +
            "___.grantFunc(testImports, 'fail');" +
            "testImports.$v = valijaMaker.CALL___(testImports);\n" +
            "___.getNewModuleHandler().setImports(testImports);",
            getName() + "-test-fixture"),
        new Executor.Input(pre, getName() + "-pre"),
        // Load the cajoled code.
        new Executor.Input(cajoledJs, getName() + "-cajoled"),
        new Executor.Input(post, getName() + "-post"),
        // Return the output field as the value of the run.
        new Executor.Input(
            "___.getNewModuleHandler().getLastValue();", getName()));

    assertNoErrors();
    return result;
  }

  private CajoledModule cajoleModule(UncajoledModule m) {
    URI baseUri = URI.create(
        "resource:///com/google/caja/parser/quasiliteral/");
    CajitaModuleRewriter rw = cajitaModuleRewriter;
    ModuleManager mm = rw.getModuleManager();
    ParseTreeNode expanded = new CajitaRewriter(baseUri, mm, false).expand(m);
    CajoledModule cm = (CajoledModule) expanded;
    return cajitaModuleRewriter.rewrite(Collections.singletonList(cm));
  }
}
