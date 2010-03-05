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

import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.util.Executor;
import com.google.caja.util.RhinoTestBed;
import com.google.caja.util.CajaTestCase;

import junit.framework.AssertionFailedError;

/**
 * Direct JavaScript tests for cajita.js.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class CajitaTest extends CajaTestCase {
  public final void testAllKeys() throws Exception {
    runTest(
        ""
        + "try {"
        + "  var x = cajita.allKeys(undefined);"
        + "} catch (e) {"
        + "  fail('should be allowed to enumerate properties of undefined');"
        + "}");
    runTest(
        ""
        + "try {"
        + "  var x = cajita.allKeys(null);"
        + "} catch (e) {"
        + "  fail('should be allowed to enumerate properties of null');"
        + "}");
    runTest(
        ""
        + "try {"
        + "  var x = cajita.allKeys(false);"
        + "} catch (e) {"
        + "  fail('should be allowed to enumerate properties of false');"
        + "}");
    runTest(
        ""
        + "try {"
        + "  var x = cajita.allKeys(0);"
        + "} catch (e) {"
        + "  fail('should be allowed to enumerate properties of zero');"
        + "}");
    runTest(
        ""
        + "try {"
        + "  var x = cajita.allKeys('');"
        + "} catch (e) {"
        + "  fail('should be allowed to enumerate properties of empty string');"
        + "}");
    runTest(
        ""
        + "try {"
        + "  var x = cajita.allKeys({y:1, z:2}).sort();"
        + "} catch (e) {"
        + "  fail('should be allowed to enumerate properties of an object');"
        + "}"
        + "assertTrue(x[0] === 'y' && x[1] === 'z');");
  }

  public final void testGrantFunc() throws Exception {
    runTest(
        "  var o = { f: function(x) { this.x = x; } };"
        + "___.grantFunc(o, 'f');"
        + "assertTrue(___.isFunc(o.f));"
        + "___.asFunc(o.f);"
        + "assertEquals(42, ___.construct(o.f, [42]).x);");
  }

  public final void testGrantGeneric() throws Exception {
    runTest(
        "  function A() {} function B() {} function C() {}"
        + "var f = function(x) { this.x = x; };"
        + "A.prototype.f = B.prototype.f = f;"
        + "___.grantGenericMethod(A.prototype, 'f');"
        + "var a = new A(), b = new B(), c = new C();"
        + "___.callPub(a, 'f', [42]); assertEquals(42, a.x);"
        + "assertThrows(function() { ___.callPub(b, 'f', [42]); });"
        + "assertThrows(function() { ___.callPub(c, 'f', [42]); });");
  }

  public final void testJsonParse() throws Exception {
    runTest(
        ""
        + "function hop(o, k) {\n"
        + "  return ___.hasOwnProp(o, k); \n"
        + "}\n"
        + "var safeJSON = ___.sharedImports.JSON; \n"
        + "assertEquals('foo', safeJSON.parse('{ \"bar\": \"foo\" }').bar); \n"
        + "var o1 = {}, o2 = {}, o3 = {}; \n"
        + "try { \n"
        + "  o1 = safeJSON.parse('{ \"f___\": 1 }'); \n"
        + "} catch (e) {} \n"
        + "try { \n"
        + "  o2 = safeJSON.parse('{ \"valueOf\": true }'); \n"
        + "} catch (e) {} \n"
        + "try { \n"
        + "  o3 = safeJSON.parse('{ \"toString\": true }'); \n"
        + "} catch (e) {} \n"
        + "assertFalse(hop(o1, 'f___')); \n"
        + "assertFalse(hop(o1, 'valueOf')); \n"
        + "assertFalse(hop(o1, 'toString')); \n");
  }

  public final void testIssue1190() throws Exception {
    runTest(
        ""
        + "var x = safeJSON.parse('{\"a\":1, \"b\":2}');\n"
        + "var s = safeJSON.stringify(x);\n"
        + "assertEquals('{\"a\":1,\"b\":2}', s);\n"
        );
    runTest(
        ""
        + "var x = safeJSON.parse('{\"a\":1, \"b\":\"__proto__\"}');\n"
        + "var s = safeJSON.stringify(x);\n"
        + "assertEquals('{\"a\":1,\"b\":\"__proto__\"}', s);\n"
        );
    assertQuestionableJson(
        "{'a': 1, 'b___': 2}".replace('\'', '"'),
        "{'a':1}".replace('\'', '"'));
    assertQuestionableJson(
        "{'a': 1, 'b_\\u005f_': {'b': [1,2]}, 'c': [0.1]}".replace('\'', '"'),
        "{'a':1,'c':[0.1]}".replace('\'', '"'));
    assertBadJson("{'a': 1, 'b_\\x5f_': 2}".replace('\'', '"'));
    assertBadJson("{'a': 1, 'b_\\137_': 2}".replace('\'', '"'));
    assertBadJson("{'a': 1, b_\\x5f_: 2}".replace('\'', '"'));
    assertBadJson("{'a': 1, b_\\137_: 2}".replace('\'', '"'));
    // no closing {
    assertBadJson("{'a': 1, 'b': 2".replace('\'', '"'));
    assertBadJson("{'a': 1, 'b': 2".replace('\'', '"'));
    assertBadJson("___");
    assertQuestionableJson("[___]", "[]");
    // mismatched brackets
    runTest(
        ""
        + "var s;\n"
        + "try {\n"               // v- left square bracket
        + "  var x = safeJSON.parse('[\"a\":1, \"b__\":\"__proto__\"}');\n"
        + "  s = safeJSON.stringify(x);\n"
        + "} catch (e) {\n"
        + "  s = void 0;\n"
        + "}\n"
        + "if (s !== void 0) {\n"
        + "  assertEquals('[\"a\",1,\"b__\",\"__proto__\"]', s);\n"
        + "}");
    // more mismatched brackets
    runTest(
        ""
        + "var s;\n"
        + "try {\n"               // v- left square bracket
        + "  var x = safeJSON.parse('{\"a\": [2}, \"c___\": 3}');\n"
        + "  s = safeJSON.stringify(x);\n"
        + "} catch (e) {\n"
        + "  s = void 0;\n"
        + "}\n"
        + "if (s !== void 0) {\n"
        + "  assertEquals('{\"a\":[2]}', s);\n"
        + "}");
  }

  public final void testReplacers() throws Exception {
    runTest(
        ""
        + "var o = { z___: 3 };\n"
        + "___.setPub(o, 'x', 1);\n"
        + "___.setPub(o, 'y', -2);\n"
        + "var keysSeen = [];\n"
        + "function f(key, value) {\n"
        + "  keysSeen.push(key);\n"
        + "  return key === '' ? value : -value;\n"
        + "}\n"
        + "assertEquals(\n"
        + "    '{\"x\":-1,\"y\":2}',\n"
        + "    safeJSON.stringify(o, ___.markFuncFreeze(f)));\n"
        + "assertEquals(',x,y', '' + keysSeen);"
        );
  }

  public final void testRevivers() throws Exception {
    runTest(
        ""
        + "var s = '[ { \"millis\": 0 }, { \"millis\": 1000 } ]';\n"
        + "var keysSeen = [];\n"
        + "function f(key, val) {\n"
        + "  keysSeen.push(key);\n"
        + "  if (typeof val === 'object' && typeof val.millis === 'number') {\n"
        + "    var d = new Date(val.millis);\n"
        + "    d.millis__ = val.millis;\n"
        + "    return d;\n"
        + "  }\n"
        + "  return val;\n"
        + "}\n"
        + "var arr = safeJSON.parse(s, ___.markFuncFreeze(f));\n"
        + "assertEquals('millis,0,millis,1,', '' + keysSeen);\n"
        + "assertTrue('is array', arr instanceof Array);\n"
        + "assertEquals('arr.length', 2, arr.length);\n"
        + "assertTrue('Date 0', arr[0] instanceof Date);\n"
        + "assertTrue('Date 1', arr[1] instanceof Date);\n"
        + "assertEquals(0, arr[0].getTime());\n"
        + "assertEquals(1000, arr[1].getTime());\n"
        );
  }

  private void assertQuestionableJson(String json, String ok)
      throws Exception {
    StringBuilder escapedJson = new StringBuilder();
    Escaping.escapeJsString(json, false, false, escapedJson);
    StringBuilder escapedOk = new StringBuilder();
    Escaping.escapeJsString(ok, false, false, escapedOk);
    runTest(
        ""
        + "(function () {\n"
        + "  var obj = null;\n"
        + "  try {\n"
        + "    obj = safeJSON.parse('" + escapedJson + "');\n"
        + "  } catch (e) {\n"
        + "    return;\n"
        + "  }\n"
        + "  assertEquals('" + escapedOk + "', JSON.stringify(obj));\n"
        + "})();"
        );
  }

  private void assertBadJson(String json) throws Exception {
    StringBuilder escapedJson = new StringBuilder();
    Escaping.escapeJsString(json, false, false, escapedJson);
    runTest(
        ""
        + "var ok = false;\n"
        + "try {\n"
        + "  safeJSON.parse('" + escapedJson + "');\n"
        + "} catch (e) {\n"
        + "  ok = true;\n"
        + "}\n"
        + "assertTrue(ok);\n"
        );
  }

  public final void testTestFramework() throws Exception {
    try {
      runTest("assertEquals(0, 1);");
    } catch (AssertionFailedError e) {
      assertEquals(
          "Expected <0> (Number) but was <1> (Number)", e.getMessage());
      return;
    }
    fail("Did not raise");
  }

  protected void runTest(String code) throws Exception {
    mq.getMessages().clear();
    RhinoTestBed.runJs(
        new Executor.Input(
            getClass(), "/js/json_sans_eval/json_sans_eval.js"),
        new Executor.Input(
            getClass(), "/com/google/caja/plugin/console-stubs.js"),
        new Executor.Input(
            getClass(), "/com/google/caja/cajita.js"),
        new Executor.Input(
            getClass(), "/js/jsunit/2.2/jsUnitCore.js"),
        new Executor.Input(
            getClass(), "/com/google/caja/log-to-console.js"),
        new Executor.Input(code, getName() + "-test"));
    assertNoErrors();
  }
}
