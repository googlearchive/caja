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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
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
import com.google.caja.plugin.PluginEnvironment;
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
  protected class TestPluginEnvironment implements PluginEnvironment {
    public CharProducer loadExternalResource(
        ExternalReference ref, String mimeType) {
      URI uri;
      uri = ref.getReferencePosition().source().getUri().resolve(ref.getUri());
      try {
        InputSource is = new InputSource(uri);
        return fromResource(uri.getPath().substring(1), is);
      } catch (IOException e) {
      }
      return null;
    }

    public String rewriteUri(ExternalReference uri, String mimeType) {
      return null;
    }
  }

  private Rewriter valijaRewriter;
  private Rewriter cajitaRewriter;
  private Rewriter innocentCodeRewriter;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    valijaRewriter = new DefaultValijaRewriter(mq, false);
    cajitaRewriter = new CajitaModuleRewriter(
        new TestBuildInfo(), new TestPluginEnvironment(), mq, false, true);
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
        "var foo = function (x$x, y_y) {}\n"
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
        "if (!foo) fail('Failed to construct a global object.');" +
        "assertEquals(foo.x, 2);");
    rewriteAndExecute(
        "(function () {" +
        "  function Foo(x) { this.x = x; }" +
        "  var foo = new Foo(2);" +
        "  if (!foo) fail('Failed to construct a local object.');" +
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
  }

  public final void testCallback() throws Exception {
    assertConsistent("Function.prototype.apply.call(function(a, b) {return a + b;}, {}, [3, 4]);");
    assertConsistent("Function.prototype.call.call(function(a, b) {return a + b;}, {}, 3, 4);");
    assertConsistent("Function.prototype.bind.call(function(a, b) {return a + b;}, {}, 3)(4);");
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
    setRewriter(cajitaRewriter);
    CajoledModule cajoled = (CajoledModule)
        rewriteTopLevelNode(new UncajoledModule(cajitaTree));
    String cajoledJs = render(cajoled);
    CajoledModule valijaBody = (CajoledModule) rewriteTopLevelNode(
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
            "testImports.assertThrows = assertThrows;" +
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
        new Executor.Input("___.getNewModuleHandler().getLastValue();",
                               getName()));

    assertNoErrors();
    return result;
  }
}
