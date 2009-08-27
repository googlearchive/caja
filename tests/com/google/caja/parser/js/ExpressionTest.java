// Copyright (C) 2009 Google Inc.
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

package com.google.caja.parser.js;

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.js.Expression;
import com.google.caja.util.CajaTestCase;

public class ExpressionTest extends CajaTestCase {
  public final void testConditionResult() throws ParseException {
    assertFalse(jsExpr(fromString("false")).conditionResult());
    assertTrue(jsExpr(fromString("true")).conditionResult());
    assertFalse(jsExpr(fromString("0")).conditionResult());
    assertTrue(jsExpr(fromString("1")).conditionResult());
    assertFalse(jsExpr(fromString("''")).conditionResult());
    assertTrue(jsExpr(fromString("'foo'")).conditionResult());
    assertFalse(jsExpr(fromString("null")).conditionResult());
    assertTrue(jsExpr(fromString("/foo/")).conditionResult());

    assertNull(jsExpr(fromString("foo")).conditionResult());
    assertNull(jsExpr(fromString("x.y")).conditionResult());
    assertNull(jsExpr(fromString("x[y]")).conditionResult());
    assertNull(jsExpr(fromString("-x")).conditionResult());

    assertFalse(jsExpr(fromString("!1")).conditionResult());
    assertTrue(jsExpr(fromString("!0")).conditionResult());
    assertNull(jsExpr(fromString("!foo")).conditionResult());

    assertTrue(jsExpr(fromString("[]")).conditionResult());
    assertTrue(jsExpr(fromString("{}")).conditionResult());
    assertTrue(jsExpr(fromString("function () {}")).conditionResult());

    assertTrue(jsExpr(fromString("true && true")).conditionResult());
    assertFalse(jsExpr(fromString("false && true")).conditionResult());
    assertFalse(jsExpr(fromString("true && false")).conditionResult());
    assertFalse(jsExpr(fromString("false && false")).conditionResult());
    assertFalse(jsExpr(fromString("false && x")).conditionResult());
    assertFalse(jsExpr(fromString("x && false")).conditionResult());
    assertNull(jsExpr(fromString("x && true")).conditionResult());

    assertTrue(jsExpr(fromString("true || true")).conditionResult());
    assertTrue(jsExpr(fromString("false || true")).conditionResult());
    assertTrue(jsExpr(fromString("true || false")).conditionResult());
    assertFalse(jsExpr(fromString("false || false")).conditionResult());
    assertTrue(jsExpr(fromString("true || x")).conditionResult());
    assertTrue(jsExpr(fromString("x || true")).conditionResult());
    assertNull(jsExpr(fromString("x || false")).conditionResult());

    assertFalse(jsExpr(fromString("x,false")).conditionResult());
    assertTrue(jsExpr(fromString("x,true")).conditionResult());
    assertNull(jsExpr(fromString("x,y")).conditionResult());

    assertFalse(jsExpr(fromString("x ? false : 0")).conditionResult());
    assertFalse(jsExpr(fromString("0 ? x : 0")).conditionResult());
    assertFalse(jsExpr(fromString("1 ? false : x")).conditionResult());
    assertTrue(jsExpr(fromString("x ? true : 1")).conditionResult());
    assertTrue(jsExpr(fromString("0 ? x : 1")).conditionResult());
    assertTrue(jsExpr(fromString("1 ? true : x")).conditionResult());
    assertNull(jsExpr(fromString("1 ? x : 0")).conditionResult());
    assertNull(jsExpr(fromString("0 ? false : x")).conditionResult());
    assertNull(jsExpr(fromString("1 ? x : 1")).conditionResult());
    assertNull(jsExpr(fromString("0 ? true : x")).conditionResult());
    assertNull(jsExpr(fromString("x ? true : y")).conditionResult());
    assertNull(jsExpr(fromString("x ? false : y")).conditionResult());
    assertNull(jsExpr(fromString("x ? y : true")).conditionResult());
    assertNull(jsExpr(fromString("x ? y : false")).conditionResult());

    assertFalse(jsExpr(fromString("void true")).conditionResult());

    assertTrue(jsExpr(fromString("new Boolean")).conditionResult());
    assertTrue(jsExpr(fromString("new Boolean(false)")).conditionResult());
    assertTrue(jsExpr(fromString("new Date")).conditionResult());
    assertTrue(jsExpr(fromString("new Date()")).conditionResult());
    assertTrue(jsExpr(fromString("new RegExp('')")).conditionResult());
    assertTrue(jsExpr(fromString("new String('')")).conditionResult());
    assertNull(jsExpr(fromString("Date")).conditionResult());
    assertNull(jsExpr(fromString("Date()")).conditionResult());
  }

  public final void testSimplifyForSideEffect() throws ParseException {
    assertSimplified(null, "1");
    assertSimplified(null, "0");
    assertSimplified(null, "void 1");
    assertSimplified(null, "void 0");
    assertSimplified("foo()", "foo(), 1");
    assertSimplified("foo()", "1, foo()");
    assertSimplified(null, "1, 2");
    assertSimplified("foo()", "foo() || 1");
    assertSimplified("foo()", "1 || foo()");
    assertSimplified(null, "1 || 2");
    assertSimplified("foo()", "foo() && 1");
    assertSimplified("foo()", "1 && foo()");
    assertSimplified(null, "1 && 2");
    assertSimplified("x + y", "x + y");  // coercion might be side-effecting
  }

  private void assertSimplified(String golden, String input)
      throws ParseException {
    Expression simple = jsExpr(fromString(input)).simplifyForSideEffect();
    if (golden == null) {
      assertNull(input, simple);
      return;
    }
    assertEquals(input, render(jsExpr(fromString(golden))), render(simple));
  }
}
