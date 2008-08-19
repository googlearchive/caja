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

import java.io.IOException;

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.js.Statement;
import com.google.caja.util.RhinoTestBed;

/**
 * @author metaweta@gmail.com
 */
public class DefaultValijaRewriterTest extends RewriterTestCase {

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
  public void testFor() throws Exception {
    assertConsistent("for (var i=0; i<10; i++) {} i;");
  }
  public void testUnderscore() throws Exception {
    assertConsistent("var x_=1; x_;");
    checkFails("var o={p_:1}; o.p_;", "Key may not end in \"_\"");
  }
  public void testDate() throws Exception {
    //assertConsistent("''+new Date;");
  }
  public void testDelete() throws Exception {
    assertConsistent("var a={x:1}; delete a.x; a.x;");
  }
  public void testIn() throws Exception {
    assertConsistent("var a={x:1}; ''+ ('x' in a) + ('y' in a);");
  }
  public void testForIn() throws Exception {
    assertConsistent("str=''; for (var i in {x:1, y:true}) {str+=i;} str;");
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
    Statement cajitaTree = (Statement)rewriteStatements(valijaTree);
    setRewriter(defaultCajaRewriter);    
    String cajoledJs = "___.loadModule(function (___, IMPORTS___) {\n" + 
        render(rewriteStatements(cajitaTree)) + 
        "\n});";
    String valijaCajoled = render(rewriteStatements(js(fromResource("../../valija-cajita.js"))));
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
            "testImports.unittestResult___ = {\n" +
            "    toString: function () { return '' + this.value; },\n" +
            "    value: '--NO-RESULT--'\n" +
            "};\n" +
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
