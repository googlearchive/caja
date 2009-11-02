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
    assertSimplified("++x", "++x");
    assertSimplified("x -= 2", "x -= 2");
    assertSimplified("x = 2", "x = 2");
    assertSimplified("x + y", "x + y");  // coercion might be side-effecting
  }

  public final void testTypeOf() throws Exception {
    assertTypeOf("boolean", "true");
    assertTypeOf("boolean", "false");
    assertTypeOf("number", "0");
    assertTypeOf("number", "1");
    assertTypeOf("number", "-1.5");
    assertTypeOf("number", "6.02e+23");
    assertTypeOf("string", "''");
    assertTypeOf("string", "'foo'");
    assertTypeOf("object", "null");
    assertTypeOf("object", "{}");
    assertTypeOf("function", "function () {}");
    assertTypeOf("boolean", "!x");
    assertTypeOf("boolean", "!x || !y");
    assertTypeOf(null, "!x || y");
    assertTypeOf(null, "x || !y");
    assertTypeOf("number", "+x");
    assertTypeOf("number", "-x");
    assertTypeOf("number", "x - y");
    assertTypeOf(null, "y++");  // See browser-expectations.html
    assertTypeOf(null, "z--");
    assertTypeOf("number", "++y");
    assertTypeOf("number", "--z");
    assertTypeOf(null, "a + b");
    assertTypeOf("number", "+a + 1");
    assertTypeOf("string", "'' + b");
    assertTypeOf("number", "'4' - 1");
    assertTypeOf("string", "foo() ? 'bar' : 'baz'");
    assertTypeOf(null, "foo() ? 'bar' : null");
    assertTypeOf(null, "foo() ? bar : 'baz'");
    assertTypeOf("string", "'bar' && ('' + baz)");
    assertTypeOf(null, "'bar' && null");
    assertTypeOf(null, "bar && 'baz'");
    assertTypeOf("boolean", "foo(), !x");
    assertTypeOf(null, "'' + foo(), x");
    assertTypeOf(null, "/./");
  }

  public final void testFold() throws Exception {
    assertFolded("4.0", "1 + 3");
    assertFolded("'13'", "1 + '3'");
    assertFolded("'13'", "'1' + 3");
    assertFolded("'13'", "'1' + '3'");
    assertFolded("'-1.5'", "'' + -1.5");
    assertFolded("'undefined'", "'' + void 0");
    assertFolded("'null'", "null + ''");
    assertFolded("null + 0", "null + 0");  // 0 in JS
    assertFolded("-2.0", "1 - 3");
    assertFolded("4.0", "2 * 2");
    assertFolded("true", "!''");
    assertFolded("true", "!0");
    assertFolded("false", "!'0'");
    assertFolded("true", "!null");
    assertFolded("true", "!(void 0)");
    assertFolded("! (void foo())", "!(void foo())");
    assertFolded("false", "!(4,true)");
    assertFolded("! (foo() || true)", "!(foo()||true)");
    assertFolded("true", "'foo' == 'foo'");
    assertFolded("true", "'foo' === 'foo'");
    assertFolded("false", "'foo' == 'bar'");
    assertFolded("false", "'foo' === 'bar'");
    assertFolded("false", "4 === '4'");
    assertFolded("4 == '4'", "4 == '4'");
    assertFolded("false", "'foo' != 'foo'");
    assertFolded("false", "'foo' !== 'foo'");
    assertFolded("true", "'foo' != 'bar'");
    assertFolded("true", "'foo' !== 'bar'");
    assertFolded("true", "4 !== '4'");
    assertFolded("4 != '4'", "4 != '4'");
    assertFolded("0.5", "1 / 2");
    assertFolded("1 / '2'", "1 / '2'");
    assertFolded("(1/0)", "1 / 0");
    assertFolded("(-1/0)", "-1 / 0");
    assertFolded("0 / 0", "0 / 0");
    assertFolded("1.0", "1 % 3");
    assertFolded("1.0", "1 % -3");
    assertFolded("-1.0", "-1 % 3");
    assertFolded("-1.0", "-1 % -3");
    assertFolded("4.0", "4.0");
    assertFolded("4.0", "+4.0");
    assertFolded("-1", "~0");
    assertFolded("-1", "-1");
    assertFolded("3", "'foo'.length");
    assertFolded("1", "'foo'.indexOf('o')");
    assertFolded("-1", "'foo'.indexOf('bar')");
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

  private void assertTypeOf(String type, String expr) throws ParseException {
    assertEquals(type, jsExpr(fromString(expr)).typeOf());
  }

  private void assertFolded(String result, String expr) throws ParseException {
    Expression input = jsExpr(fromString(expr));
      // Fold operands so we can test negative numbers.
      if (input instanceof Operation) {
        Operation op = (Operation) input;
        for (Expression operand : op.children()) {
          if (Operation.is(operand, Operator.NEGATION)
              && operand.children().get(0) instanceof NumberLiteral) {
            op.replaceChild(operand.fold(), operand);
          }
        }
      }
    Expression actual = input.fold();
    assertEquals(result, actual != null ? render(actual) : null);
  }
}
