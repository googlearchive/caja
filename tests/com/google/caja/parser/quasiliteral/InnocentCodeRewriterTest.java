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

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.js.Statement;
import com.google.caja.util.RhinoTestBed;

import java.io.IOException;

/**
 * @author adrienne.felt@gmail.com
 * @author ihab.awad@gmail.com
 */
public class InnocentCodeRewriterTest extends RewriterTestCase {
  @Override
  public void setUp() throws Exception {
    super.setUp();
    setRewriter(new InnocentCodeRewriter(true));
  }

  // Tests block-level forEach statements
  public void testForEachPlain() throws Exception {
    checkSucceeds(
        "for (var k in x) { k; }",

        "var x0___;" +
        "for (x0___ in x) {" +
        "  if (x0___.match(/___$/)) {" +
        "    continue;" +
        "  }" +
        "  k = x0___;" +
        "  { k; }" +
        "}");

    // Checks that test.number is only incremented once when
    // the for loop is executed
    rewriteAndExecute(
        "var test = new Object;" +
        "test.number = 0;" +
        "test.other = 450;" +
        "test.__defineGetter__('num', " +
        "    function() { this.number++; return this.number; });" +
        "test.__defineSetter__('num', " +
        "    function(y) { return this.number = y; });",
        "for (k in test) {" +
        "  var p = test[k];" +
        "}",
        "assertEquals(test.number, 1);");

    // Checks that k is only incremented once when
    // the for loop is executed
    rewriteAndExecute(
        "var test = new Object;" +
        "test.number = 0;" +
        "var k = new Object;" +
        "k.one = 1;" +
        "k.__defineGetter__('foo', " +
        "    function() { this.one++; return this.one; });" +
        "k.__defineSetter__('foo', " +
        "    function() { this.one++; return this.one; });",
        "for (k.foo in test) {" +
        "  k.one;" +
        "}",
        "assertEquals(k.one, 2);");

    // Checks that the hidden properties are not visited when
    // the for loop is executed
    rewriteAndExecute(
        "var test = new Object;" +
        "test.visible = 0;" +
        "test.hidden___ = 5;" +
        "test.hiddenObj___ = new Object;" +
        "var counter = 0;",
        "for (k in test) {" +
        "  counter++;" +
        "}",
        "assertEquals(counter,1);");
  }

  // Checks that var x0 is added *inside* the function scope
  public void testForEachFunc() throws Exception {
    checkSucceeds(
        "function add() {" +
        "  for (var k in x) { k; }" +
        "}",

        "function add() {" +
        "  var x0___;" +
        "  for (x0___ in x) {" +
        "    if (x0___.match(/___$/)) {" +
        "      continue;" +
        "    }" +
        "    k = x0___;" +
        "    { k; }" +
        "  }" +
        "}");

      rewriteAndExecute(
        ";",
        "function () {" +
        "  var test = new Object;" +
        "  test.visible = 0;" +
        "  var counter = 0;" +
        "  for (k in test) {" +
        "    counter++;" +
        "  }" +
        "}",
        "var q = 0;" +
        "if (typeof x0___ == 'undefined') {" +
        "  q = 1;" +
        "}"+
        "assertEquals(q,1);");
  }

  // Checks calls, sets, and reads of members of THIS
  public void testThis() throws Exception {
    rewriteAndExecute(
        ";",
        "assertThrows(function a() {" +
        "  this.fun();" +
        "});",
        ";");
    rewriteAndExecute(
        ";",
        "assertThrows(function a() {" +
        "  var q = this[0];" +
        "});",
        ";");
    rewriteAndExecute(
        ";",
        "assertThrows(function a() {" +
        "  var k = this.q;" +
        "});",
        ";");
    rewriteAndExecute(
        ";",
        "assertThrows(function a() {" +
        "  return this;" +
        "});",
        ";");
  }

  @Override
  protected Object executePlain(String caja) throws IOException {
    mq.getMessages().clear();
    return RhinoTestBed.runJs(
        new RhinoTestBed.Input(getClass(), "/com/google/caja/caja.js"),
        new RhinoTestBed.Input(getClass(), "../../plugin/asserts.js"),
        new RhinoTestBed.Input(caja, getName() + "-uncajoled"));
  }

  @Override
  protected Object rewriteAndExecute(String pre, String trans, String post)
      throws IOException, ParseException {
    mq.getMessages().clear();

    Statement cajaTree = js(fromString(trans, is));
    String transJs = render(
        rewriteStatements(js(fromResource("../../plugin/asserts.js")),
                          cajaTree));

    assertNoErrors();

    Object result = RhinoTestBed.runJs(
        new RhinoTestBed.Input(
            getClass(), "/com/google/caja/plugin/console-stubs.js"),
        new RhinoTestBed.Input(getClass(), "/com/google/caja/caja.js"),
        new RhinoTestBed.Input(pre, getName()),
        new RhinoTestBed.Input(transJs, getName()),
        new RhinoTestBed.Input(post, getName()));

    assertNoErrors();
    return result;
  }
}
