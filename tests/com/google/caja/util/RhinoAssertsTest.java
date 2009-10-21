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

package com.google.caja.util;

import junit.framework.TestCase;

public class RhinoAssertsTest extends TestCase {
  public final void testString() throws Exception {
    assertStructuralForm("\"\"", "''");
    assertStructuralForm("\"foo\"", "'foo'");
    assertStructuralForm("\"foo\\nbar\"", "'foo\\nbar'");
  }

  public final void testNumbers() throws Exception {
    assertStructuralForm("0.0", "0");
    assertStructuralForm("-1.0", "-1");
    assertStructuralForm("1.0", "+1");
    assertStructuralForm("0.1", ".1");
    assertStructuralForm("3.141592654", "3.141592654");
    assertStructuralForm("NaN", "NaN");
    assertStructuralForm("-Infinity", "-Infinity");
  }

  public final void testBooleans() throws Exception {
    assertStructuralForm("true", "true");
    assertStructuralForm("false", "false");
  }

  public final void testNulls() throws Exception {
    assertStructuralForm("null", "null");
    assertStructuralForm("undefined", "undefined");
  }

  public final void testArrays() throws Exception {
    assertStructuralForm("[]", "[]");
    assertStructuralForm("[1.0, 2.0, 3.0]", "[1, 2, 3]");
    assertStructuralForm("[[]]", "[[]]");
    assertStructuralForm("[1.0, 2.0, [3.0], null]", "[1, 2, [3], null]");
  }

  public final void testObjects() throws Exception {
    assertStructuralForm("{}", "({})");
    assertStructuralForm("{\"a\": {\"b\": null}}", "({ a: { b: null } })");
    assertStructuralForm("{\"a\": 4.0, \"b\": null}", "({ b: null, a: 4 })");
    assertStructuralForm(
        "{\"0\": \"hi\", \"2\": \"there\", \"1.5\": null, \"length\": 4.0}",
        "({ length: 4, 2: 'there', 0: 'hi', 1.5: null })");
  }

  public final void testObjectGraphCycles() throws Exception {
    assertStructuralForm(
        "#1=[#1#]",
        "(function () { var a = []; a.push(a); return a; })()");
    assertStructuralForm(
        "#1=[#1#, [3.0]]",
        "(function () { var a = []; a.push(a, [3]); return a; })()");
    assertStructuralForm(
        "{\"x\": #1=[#1#, [3.0]]}",
        "(function () { var a = []; a.push(a, [3]); return { x: a }; })()");
    assertStructuralForm(
        "#1=[#2={\"x\": #1#, \"y\": #2#}, [], #1#, \"foo\", \"foo\"]",
        ""
        + "(function () {"
        + "  var a = [],"
        + "      b = { x: a };"
        + "  a.push(b, [], a, 'foo', 'foo');"
        + "  b.y = b;"
        + "  return a;"
        + "})()");
  }

  public final void testBuiltinObjs() throws Exception {
    assertIndeterminateStructuralForm("(function () {})");
    assertIndeterminateStructuralForm("(new Date(0))");
    assertIndeterminateStructuralForm(
        "(function () { function Array() { } return new Array(); })()");
    assertIndeterminateStructuralForm("java.lang.System.err");
  }

  private void assertStructuralForm(String structuralForm, String js) {
    Object result = RhinoTestBed.runJs(new Executor.Input(js, getName()));
    assertEquals(structuralForm, RhinoAsserts.structuralForm(result));
  }

  private void assertIndeterminateStructuralForm(String js) {
    Object result = RhinoTestBed.runJs(new Executor.Input(js, getName()));
    try {
      RhinoAsserts.structuralForm(result);
    } catch (IllegalArgumentException ex) {
      // Pass
      return;
    }
    fail("`" + js + "` should not have a structural form");
  }
}
