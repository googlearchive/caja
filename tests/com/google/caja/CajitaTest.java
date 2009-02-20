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

package com.google.caja;

import com.google.caja.util.RhinoTestBed;
import com.google.caja.util.CajaTestCase;

/**
 * Direct JavaScript tests for cajita.js.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class CajitaTest extends CajaTestCase {
  public void testAllKeys() throws Exception {
    runTest(
        "  try { var x = cajita.allKeys(undefined); }"
        + "catch (e) { fail('should be allowed to enumerate properties of undefined'); }");
    runTest(
        "  try { var x = cajita.allKeys(null); }"
        + "catch (e) { fail('should be allowed to enumerate properties of null'); }");
    runTest(
        "  try { var x = cajita.allKeys(false); }"
        + "catch (e) { fail('should be allowed to enumerate properties of false'); }");
    runTest(
        "  try { var x = cajita.allKeys(0); }"
        + "catch (e) { fail('should be allowed to enumerate properties of zero'); }");
    runTest(
        "  try { var x = cajita.allKeys(''); }"
        + "catch (e) { fail('should be allowed to enumerate properties of empty string'); }");
    runTest(
        "  try { var x = cajita.allKeys({y:1, z:2}).sort(); }"
        + "catch (e) { fail('should be allowed to enumerate properties of an object'); }"
        + "assertTrue(x[0] === 'y' && x[1] === 'z');");
  }
  
  public void testGrantFunc() throws Exception {
    runTest(
        "  var o = { f: function(x) { this.x = x; } };"
        + "___.grantFunc(o, 'f');"
        + "assertTrue(___.isFunc(o.f));"
        + "___.asFunc(o.f);"
        + "assertEquals(42, ___.construct(o.f, [42]).x);");
  }

  public void testGrantGeneric() throws Exception {
    runTest(
        "  function A() {} function B() {} function C() {}"
        + "var f = function(x) { this.x = x; };"
        + "A.prototype.f = B.prototype.f = f;"
        + "___.grantGeneric(A.prototype, 'f');"
        + "var a = new A(), b = new B(), c = new C();"
        + "___.callPub(a, 'f', [42]); assertEquals(42, a.x);"
        + "assertThrows(function() { ___.callPub(b, 'f', [42]); });"
        + "assertThrows(function() { ___.callPub(c, 'f', [42]); });");
  }

  public void testJsonParse() throws Exception {
    runTest(
        ""
        + "var safeJSON = ___.sharedImports.JSON; \n"
        + "assertEquals('foo', safeJSON.parse('{ \"bar\": \"foo\" }').bar); \n"
        + "assertThrows( \n"
        + "    function () { safeJSON.parse('{ \"f_canCall___\": true }'); } \n"
        + "     ); \n"
        + "assertThrows( \n"
        + "    function () { safeJSON.parse('{ \"valueOf\": true }'); } \n"
        + "     ); \n"
        + "assertThrows( \n"
        + "    function () { safeJSON.parse('{ \"toString\": true }'); } \n"
        + "     );"
        );
  }

  protected void runTest(String code) throws Exception {
    mq.getMessages().clear();
    RhinoTestBed.runJs(
        new RhinoTestBed.Input(
            getClass(), "/com/google/caja/plugin/console-stubs.js"),
        new RhinoTestBed.Input(
            getClass(), "/com/google/caja/cajita.js"),
        new RhinoTestBed.Input(
            getClass(), "/js/jsunit/2.2/jsUnitCore.js"),
        new RhinoTestBed.Input(
            getClass(), "/com/google/caja/log-to-console.js"),
        new RhinoTestBed.Input(code, getName() + "-test"));
    assertNoErrors();
  }
}
