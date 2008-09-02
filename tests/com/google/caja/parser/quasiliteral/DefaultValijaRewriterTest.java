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
import com.google.caja.parser.js.Statement;
import com.google.caja.util.RhinoTestBed;

/**
 * @author metaweta@gmail.com
 */
public class DefaultValijaRewriterTest extends CommonJsRewriterTestCase {

  protected Rewriter defaultValijaRewriter = new DefaultValijaRewriter(true);
  protected Rewriter defaultCajaRewriter = new DefaultCajaRewriter(false, false);
  protected Rewriter innocentCodeRewriter = new InnocentCodeRewriter(false);

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // Start with this one, then switch later to DefaultCajaRewriter for the second pass.
    setRewriter(defaultValijaRewriter);
  }

  public void testConstant() throws Exception {
    assertConsistent("1;");
  }
  public void testInit() throws Exception {
    assertConsistent("var a=0; a;");
  }
  public void testNew() throws Exception {
    assertConsistent("function f(){ this.x = 1; } f; var g = new f(); g.x;");
  }
  public void testClosure() throws Exception {
    assertConsistent(
        "function f(){" +
        "  var y=2; " +
        "  this.x = function(){" +
        "    return y;" +
        "  }; " +
        "}" +
        "var g = new f();" +
        "var h={};" +
        "f.call(h);" +
        "h.y = g.x;" +
        "h.x() + h.y();");
  }
  public void testArray() throws Exception {
    assertConsistent("[3,2,1].sort().toString();");
  }
  public void testObject() throws Exception {
    assertConsistent("({x:1,y:2}).toString();");
  }
  public void testUnderscore() throws Exception {
    // TODO: enable this behavior
    // assertConsistent("var x_=1; x_;");
    // checkFails("var o={p_:1}; o.p_;", "Key may not end in \"_\"");
  }
  public void testDate() throws Exception {
    //assertConsistent("''+new Date;");
  }
  public void testDelete() throws Exception {
    assertConsistent("(function () { var a={x:1}; delete a.x; a.x; })();");
    assertConsistent("var a={x:1}; delete a.x; a.x;");
  }
  public void testIn() throws Exception {
    assertConsistent(
        "(function () {" +
        "  var a={x:1};\n" +
        "  '' + ('x' in a) + ('y' in a);" +
        "})();");
    assertConsistent(
        "var a={x:1};\n" +
        "'' + ('x' in a) + ('y' in a);");
  }

  public void testForInLoop() throws Exception {
    assertConsistent("(function(){ str=''; for (i in {x:1, y:true}) {str+=i;} str; })();");
    assertConsistent("(function(){ str=''; for (var i in {x:1, y:true}) {str+=i;} str;})();");
    assertConsistent("str=''; for (i in {x:1, y:true}) {str+=i;} str;");
    assertConsistent("str=''; for (var i in {x:1, y:true}) {str+=i;} str;");
  }

  /**
   * Tests that the container can get access to
   * "virtual globals" defined in cajoled code.
   */
  public void testWrapperAccess() throws Exception {
    // TODO(ihab.awad): SECURITY: Re-enable by reading (say) x.foo, and
    // defining the property IMPORTS___.foo.
    if (false) {
    rewriteAndExecute(
        "",
        "x='test';",
        "if (___.getNewModuleHandler().getImports().x != 'test') {" +
          "fail('Cannot see inside the wrapper');" +
        "}");
    }
  }

  /**
   * Try to construct some class instances.
   */
  public void testFuncCtor() throws Exception {
    rewriteAndExecute(
        "function Foo(x){ this.x = x; }" +
        "var foo = new Foo(2);" +
        "if (!foo) fail('Failed to construct a global object.');" +
        "assertEquals(foo.x, 2);");
    rewriteAndExecute(
        "(function(){" +
        "function Foo(x){ this.x = x; }" +
        "var foo = new Foo(2);" +
        "if (!foo) fail('Failed to construct a local object.');" +
        "assertEquals(foo.x, 2);" +
        "})();");
    rewriteAndExecute(
        "function Foo(){ }" +
        "var foo = new Foo();" +
        "if (!foo) fail('Failed to use a simple named function as a constructor.');");
  }

  public void testFuncArgs() throws Exception {
    rewriteAndExecute(
        "  var x = 0;"
        + "function f() { x = arguments[1]; }"
        + "f(3);"
        + "assertEquals(3, x);");
  }

  @Override
  protected Object executePlain(String caja) throws IOException, ParseException {
    mq.getMessages().clear();
    setRewriter(innocentCodeRewriter);
    Statement innocentTree = (Statement)rewriteStatements(js(fromString(caja, is)));
    // Make sure the tree assigns the result to the unittestResult___ var.
    return RhinoTestBed.runJs(
        new RhinoTestBed.Input(getClass(), "/com/google/caja/caja.js"),
        new RhinoTestBed.Input(getClass(), "../../plugin/asserts.js"),
        new RhinoTestBed.Input(render(innocentTree), getName() + "-uncajoled"));
  }

  @Override
  protected Object rewriteAndExecute(String pre, String caja, String post)
      throws IOException, ParseException {
    mq.getMessages().clear();

    setRewriter(defaultValijaRewriter);
    Statement valijaTree = replaceLastStatementWithEmit(
        js(fromString(caja, is)), "unittestResult___;");
    Statement cajitaTree = (Statement)rewriteStatements(
        valijaTree);
    setRewriter(defaultCajaRewriter);
    String cajoledJs = "___.loadModule(function (___, IMPORTS___) {\n" +
        render(rewriteStatements(cajitaTree)) +
        "\n});";
    String valijaCajoled = render(
        rewriteStatements(js(fromResource("../../valija-cajita.js"))));
    assertNoErrors();

    Object result = RhinoTestBed.runJs(
        new RhinoTestBed.Input(
            getClass(), "/com/google/caja/plugin/console-stubs.js"),
        new RhinoTestBed.Input(getClass(), "/com/google/caja/caja.js"),
        new RhinoTestBed.Input(getClass(), "../../plugin/asserts.js"),
        new RhinoTestBed.Input(getClass(), "/com/google/caja/log-to-console.js"),
        new RhinoTestBed.Input(
            "var valija = {};\n" +
            "var testImports = ___.copy(___.sharedImports);\n" +
            "testImports.loader = {provide:___.simpleFunc(function(v){valijaMaker=v;})};\n" +
            "testImports.outers = ___.copy(___.sharedImports);\n" +
            "___.getNewModuleHandler().setImports(testImports);",
            getName() + "valija-setup"),
        new RhinoTestBed.Input(
            "___.loadModule(function (___, IMPORTS___) {" + valijaCajoled + "\n});",
            "valija-cajoled"),
        new RhinoTestBed.Input(
            // Initialize the output field to something containing a unique
            // object value that will not compare identically across runs.
            // Set up the imports environment.
            "testImports = ___.copy(___.sharedImports);\n" +
            "var unittestResult___ = {\n" +
            "    toString: function () { return '' + this.value; },\n" +
            "    value: '--NO-RESULT--'\n" +
            "};\n" +
            "testImports.console = console;" +
            "testImports.assertEquals = ___.simpleFunc(assertEquals);" +
            "___.grantCall(testImports, 'assertEquals');" +
            "testImports.assertTrue = ___.simpleFunc(assertTrue);" +
            "___.grantCall(testImports, 'assertTrue');" +
            "testImports.assertFalse = ___.simpleFunc(assertFalse);" +
            "___.grantCall(testImports, 'assertFalse');" +
            "testImports.valija = valijaMaker(testImports);\n" +
            "___.getNewModuleHandler().setImports(testImports);",
            getName() + "-test-fixture"),
        new RhinoTestBed.Input(pre, getName() + "-pre"),
        // Load the cajoled code.
        new RhinoTestBed.Input(cajoledJs, getName() + "-cajoled"),
        new RhinoTestBed.Input(post, getName() + "-post"),
        // Return the output field as the value of the run.
        new RhinoTestBed.Input("unittestResult___;", getName()));

    assertNoErrors();
    return result;
  }
}
