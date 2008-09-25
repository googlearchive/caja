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

import java.io.IOException;

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.ModuleEnvelope;
import com.google.caja.parser.js.Statement;
import com.google.caja.util.RhinoTestBed;

/**
 * @author metaweta@gmail.com
 */
public class DefaultValijaRewriterTest extends CommonJsRewriterTestCase {
  private Rewriter defaultValijaRewriter = new DefaultValijaRewriter(false);
  private Rewriter cajitaRewriter = new CajitaRewriter(false);
  private Rewriter innocentCodeRewriter = new InnocentCodeRewriter(false);

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // Start with this one, then switch later to CajitaRewriter for the second pass.
    setRewriter(defaultValijaRewriter);
  }

  public void testConstant() throws Exception {
    assertConsistent("1;");
  }

  public void testInit() throws Exception {
    assertConsistent("var a = 0; a;");
  }

  public void testNew() throws Exception {
    assertConsistent("function f() { this.x = 1; } f; var g = new f(); g.x;");
  }

  public void testProtoCall() throws Exception {
    assertConsistent("'' + Array.prototype.sort.call([3, 1, 2]);");
    assertConsistent("'' + [3, 1, 2].sort();");
    assertConsistent("'' + [3, 1, 2].sort.call([4, 2, 7]);");

    assertConsistent("String.prototype.indexOf.call('foo', 'o');");
    assertConsistent("'foo'.indexOf('o');");

    assertConsistent("'foo'.indexOf.call('bar', 'o');");
    assertConsistent("'foo'.indexOf.call('bar', 'a');");
  }

  public void testInherit() throws Exception {
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
  public void testRegExpLeak() throws Exception {
    rewriteAndExecute(
        "assertEquals('' + (/(.*)/).exec(), 'undefined,undefined');");
  }

  public void testClosure() throws Exception {
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

  public void testNamedFunctionShadow() throws Exception {
    assertConsistent("function f() { return f; } f === f();");
    assertConsistent(
        "(function () { function f() { return f; } return f === f(); })();");
  }

  public void testArray() throws Exception {
    assertConsistent("[3, 2, 1].sort().toString();");
    assertConsistent("[3, 2, 1].sort.call([4, 2, 7]).toString();");
  }

  public void testObject() throws Exception {
    assertConsistent("({ x: 1, y: 2 }).toString();");
  }

  public void testIndexOf() throws Exception {
    assertConsistent("'foo'.indexOf('o');");
  }

  public void testFunctionToStringCall() throws Exception {
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
        + "             'function (x$x, y_y) {\\n  [cajoled code]\\n}');");
  }

  public void testUnderscore() throws Exception {
     rewriteAndExecute(
         ""
         + "var msg;"
         + "try {"
         + "  var x_ = 1;"
         + "  x_;"
         + "} catch (ex) {"
         + "  msg = ex.message;"
         + "}"
         + "assertEquals('Not settable: ([Object]).x_', msg);"
         );
     rewriteAndExecute(
         ""
         + "var msg;"
         + "try {"
         + "  var o = { p_: 1 };"
         + "  o.p_;"
         + "} catch (ex) {"
         + "  msg = ex.message;"
         + "}"
         + "assertEquals('Not settable: ([Object]).p_', msg);"
         );
  }

  public void testDate() throws Exception {
    assertConsistent("(new Date(0)).getTime();");
    assertConsistent("'' + (new Date(0));");
    rewriteAndExecute(
        ""
        + "var time = (new Date - 1);"
        + "assertFalse(isNaN(time));"
        + "assertEquals('number', typeof time);");
  }

  public void testMultiDeclaration() throws Exception {
    rewriteAndExecute("var a, b, c;");
    rewriteAndExecute(
        ""
        + "var a = 0, b = ++a, c = ++a;"
        + "assertEquals(++a * b / c, 1.5);");
  }

  public void testDelete() throws Exception {
    assertConsistent(
        "(function () { var a = { x: 1 }; delete a.x; return a.x; })();");
    assertConsistent("var a = { x: 1 }; delete a.x; a.x;");
  }

  public void testIn() throws Exception {
    assertConsistent(
        "(function () {" +
        "  var a = { x: 1 };\n" +
        "  return '' + ('x' in a) + ('y' in a);" +
        "})();");
    assertConsistent(
        "var a = { x: 1 };\n" +
        "'' + ('x' in a) + ('y' in a);");
  }

  public void testForIn2() throws Exception {
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

  public void testValueOf() throws Exception {
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
         + "assertEquals('Not settable: ([Object]).valueOf', msg);"
         );
    checkFails("var x = { valueOf: function (hint) { return 2; } };",
               "The valueOf property must not be set");
    checkFails("var o = {}; o.valueOf = function (hint) { return 2; };",
               "The valueOf property must not be set");
  }

  /**
   * Tests that the container can get access to
   * "virtual globals" defined in cajoled code.
   */
  public void testWrapperAccess() throws Exception {
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
  public void testFuncCtor() throws Exception {
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

  public void testFuncArgs() throws Exception {
    rewriteAndExecute(
        ""
        + "var x = 0;"
        + "function f() { x = arguments[0]; }"
        + "f(3);"
        + "assertEquals(3, x);");
  }

  public void testStatic() throws Exception {
    assertConsistent("'' + Array.slice([3, 4, 5, 6], 1);");
  }

  @Override
  protected Object executePlain(String caja)
      throws IOException, ParseException {
    mq.getMessages().clear();
    setRewriter(innocentCodeRewriter);
    Statement innocentTree = (Statement) rewriteStatements(
        js(fromString(caja, is)));
    return RhinoTestBed.runJs(
        new RhinoTestBed.Input(getClass(), "/com/google/caja/cajita.js"),
        new RhinoTestBed.Input(getClass(), "../../plugin/asserts.js"),
        new RhinoTestBed.Input(render(innocentTree), getName() + "-uncajoled"));
  }

  @Override
  protected Object rewriteAndExecute(String pre, String caja, String post)
      throws IOException, ParseException {
    mq.getMessages().clear();

    setRewriter(defaultValijaRewriter);
    Block valijaTree = js(fromString(caja, is));
    Block cajitaTree = (Block) rewriteStatements(valijaTree);
    setRewriter(cajitaRewriter);
    Block body = (Block) rewriteStatements(new ModuleEnvelope(cajitaTree));
    String cajoledJs = render(body);
    Block valijaBody = (Block) rewriteStatements(new ModuleEnvelope(
        js(fromResource("../../valija-cajita.js"))));
    String valijaCajoled = render(valijaBody);
    assertNoErrors();

    Object result = RhinoTestBed.runJs(
        new RhinoTestBed.Input(
            getClass(), "/com/google/caja/plugin/console-stubs.js"),
        new RhinoTestBed.Input(getClass(), "/com/google/caja/cajita.js"),
        new RhinoTestBed.Input(getClass(), "../../plugin/asserts.js"),
        new RhinoTestBed.Input(getClass(), "/com/google/caja/log-to-console.js"),
        new RhinoTestBed.Input(
            "var valija = {};\n" +
            "var testImports = ___.copy(___.sharedImports);\n" +
            "testImports.loader = {\n" +
            "  provide: ___.simpleFunc(function (v) { valijaMaker=v; })\n" +
            "};\n" +
            "testImports.outers = ___.copy(___.sharedImports);\n" +
            "___.getNewModuleHandler().setImports(testImports);",
            getName() + "valija-setup"),
        new RhinoTestBed.Input(valijaCajoled, "valija-cajoled"),
        new RhinoTestBed.Input(
            // Set up the imports environment.
            "testImports = ___.copy(___.sharedImports);\n" +
            "testImports.console = console;" +
            "testImports.assertEquals = ___.simpleFunc(assertEquals);" +
            "___.grantCall(testImports, 'assertEquals');" +
            "testImports.assertTrue = ___.simpleFunc(assertTrue);" +
            "___.grantCall(testImports, 'assertTrue');" +
            "testImports.assertFalse = ___.simpleFunc(assertFalse);" +
            "___.grantCall(testImports, 'assertFalse');" +
            "testImports.$v = valijaMaker(testImports);\n" +
            "___.getNewModuleHandler().setImports(testImports);",
            getName() + "-test-fixture"),
        new RhinoTestBed.Input(pre, getName() + "-pre"),
        // Load the cajoled code.
        new RhinoTestBed.Input(cajoledJs, getName() + "-cajoled"),
        new RhinoTestBed.Input(post, getName() + "-post"),
        // Return the output field as the value of the run.
        new RhinoTestBed.Input("___.getNewModuleHandler().getLastValue();",
                               getName()));

    assertNoErrors();
    return result;
  }
}
