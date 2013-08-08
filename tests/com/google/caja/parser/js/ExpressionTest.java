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

@SuppressWarnings("static-method")
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
    assertNull(jsExpr(fromString("new Date()()")).conditionResult());
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
    assertSimplified("++x", "x++");
    assertSimplified("--x", "--x");
    assertSimplified("--x", "x--");
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
    assertFolded("!void foo()", "!(void foo())");
    assertFolded("false", "!(4,true)");
    assertFolded("! (foo() || true)", "!(foo()||true)");
    assertFolded("false", "false && foo()");
    assertFolded("true", "true || foo()");
    assertFolded("foo()", "false || foo()");
    assertFolded("foo()", "true && foo()");
    assertFolded("foo != bar", "foo != bar && true");
    assertFolded("foo != bar", "foo != bar || false");
    // Can't fold.  foo() might return non-boolean
    assertFolded("foo() && true", "foo() && true");
    assertFolded("foo() || false", "foo() || false");
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
    assertFolded("a !== b", "!(a === b)");  // Correct around NaN
    assertFolded("a === b", "!(a !== b)");
    assertFolded("a != b", "!(a == b)");
    assertFolded("a == b", "!(a != b)");
    assertFolded("0.5", "1 / 2");
    assertFolded("1 / '2'", "1 / '2'");
    assertFolded("(1/0)", "1 / 0");
    assertFolded("(-1/0)", "-1 / 0");
    assertFolded("(0/0)", "0 / 0");
    assertFolded("1.0", "1 % 3");
    assertFolded("1.0", "1 % -3");
    assertFolded("-1.0", "-1 % 3");
    assertFolded("1.0", "1 % -3");
    assertFolded("-1.0", "-1 % 3");
    assertFolded("-1.0", "-1 % -3");
    assertFolded("" + 0x7fffffffL, "0x7fffffff | 0");
    assertFolded("-" + 0x7fffffffL, "-0x7fffffff | 0");
    assertFolded("-" + 0x80000000L, "0x80000000 | 0");
    assertFolded("-1", "0xffffffff | 0");
    assertFolded("-1", "0x1ffffffff | 0");
    assertFolded("0", "0x100000000 | 0");
    assertFolded("0", "0x200000000 | 0");
    assertFolded("1", "0x100000001 | 0");
    assertFolded("1661992960", "1e20 | 0");  // Outside the range of java longs
    assertFolded("-1661992960", "-1e20 | 0");
    assertFolded("1024", "1 << 10");
    assertFolded("" + (1 << 20), "1 << 20");
    assertFolded("1", "1 << 0/0");
    assertFolded("0", "0/0 << 0");
    assertFolded("1", "1 << 1/0");
    assertFolded("0", "1/0 << 0");
    assertFolded("2", "4 >> 1");
    assertFolded("1", "4 >> 2");
    assertFolded("0", "4 >> 3");
    assertFolded("-1", "-1 >> 1");
    assertFolded("1", "1 >> 1/0");
    assertFolded("0", "1/0 >> 0");
    assertFolded("" + (-1 >>> 1), "-1 >>> 1");
    assertFolded("1", "4 >>> 2");
    assertFolded("1", "1 >>> 1/0");
    assertFolded("0", "1/0 >>> 0");
    assertFolded("0", "1 & 2");
    assertFolded("2", "2 & 3");
    assertFolded("2", "3 & 2");
    assertFolded("0", "1 & 1/0");
    assertFolded("0", "1/0 & 0");
    assertFolded("3", "1 | 2");
    assertFolded("7", "6 | 5");
    assertFolded("11", "3 | 9");
    assertFolded("1", "1 | 1/0");
    assertFolded("0", "1/0 | 0");
    assertFolded("0", "0 ^ 0");
    assertFolded("0", "1 ^ 1");
    assertFolded("3", "1 ^ 2");
    assertFolded("-2", "-1 ^ 1");
    assertFolded("1", "1 ^ 1/0");
    assertFolded("0", "1/0 ^ 0");
    assertFolded("4.0", "4.0");
    assertFolded("4.0", "+4.0");
    assertFolded("-1", "~0");
    assertFolded("-1", "-1");
    assertFolded("(-0)", "-0");
    assertFolded("0.0", "-(-0)");
    assertFolded("3", "'foo'.length");
    assertFolded("1", "'foo'.indexOf('o')");
    assertFolded("-1", "'foo'.indexOf('bar')");
    assertFolded("foo[ 'bar' ]", "foo['bar']");
    assertFolded("foo[ 0 ]", "foo[0]");
    assertFolded("foo[ '0' ]", "foo['0']");
    assertFolded("foo[ 'null' ]", "foo['null']");
    assertFolded("new Date", "new Date()");
    assertFolded("new Date(0)", "new Date(0)");
    assertFolded(
        "(function () { return this; })()",
        "(function () { return this; })()");
    assertFolded(
        "(function () {\n   return -arguments[ i ];\n })()",
        "(function () { return -arguments[i]; })()");
    assertFolded("4", "(function () { return 4; })()");
    assertFolded("void 0", "(function () {})()");
    assertFolded("void 0", "(function () { return; })()");
    assertFolded("void foo()", "(function () { foo(); })()");
    assertFolded(
        "(function (i) { return i; })()",
        "(function (i) { return i; })()");
    assertFolded(
        "(function i() { return i; })()",
        "(function i() { return i; })()");
    assertFolded(
        "(function () {\n   arguments.callee();\n })()",
        "(function () { arguments.callee(); })()");
    assertFolded("eval", "0,eval", false);
    assertFolded("0, eval", "0,eval", true);
    assertFolded("foo.bar", "0,foo.bar", false);
    assertFolded("0, foo.bar", "0,foo.bar", true);
    assertFolded("foo[ bar ]", "0,foo[bar]", false);
    assertFolded("0, foo[ bar ]", "0,foo[bar]", true);
    assertFolded("foo[ 'bar' ]", "0,foo['bar']", false);
    assertFolded("0, foo[ 'bar' ]", "0,foo['bar']", true);
  }

  public final void testToInt32() {
    assertEquals(0, Operation.toInt32(0d));
    assertEquals(0, Operation.toInt32(-0d));
    assertEquals(0, Operation.toInt32(Double.POSITIVE_INFINITY));
    assertEquals(0, Operation.toInt32(Double.NEGATIVE_INFINITY));
    assertEquals(0, Operation.toInt32(Double.NaN));
    assertEquals(0, Operation.toInt32(0x100000000L));
    assertEquals(0, Operation.toInt32(-0x100000000L));
    assertEquals(0x7fffffffL, Operation.toInt32(0x7fffffffL));
    assertEquals(-0x7fffffffL, Operation.toInt32(-0x7fffffffL));
    assertEquals(-0x80000000L, Operation.toInt32(-0x80000000L));
    assertEquals(-0x80000000L, Operation.toInt32(0x80000000L));
    assertEquals(1, Operation.toInt32(1));
    assertEquals(-1, Operation.toInt32(-1));
    assertEquals(2, Operation.toInt32(2));
    assertEquals(-2, Operation.toInt32(-2));
    assertEquals((long) 1e6, Operation.toInt32(1e6));
    assertEquals((long) 1e7, Operation.toInt32(1e7));
    assertEquals((long) 1e8, Operation.toInt32(1e8));
    assertEquals((long) 1e9, Operation.toInt32(1e9));
    assertEquals(1410065408L, Operation.toInt32(1e10));
    assertEquals(1215752192L, Operation.toInt32(1e11));
    assertEquals(-727379968L, Operation.toInt32(1e12));
    assertEquals((long) -1e6, Operation.toInt32(-1e6));
    assertEquals((long) -1e7, Operation.toInt32(-1e7));
    assertEquals((long) -1e8, Operation.toInt32(-1e8));
    assertEquals((long) -1e9, Operation.toInt32(-1e9));
    assertEquals(-1410065408L, Operation.toInt32(-1e10));
    assertEquals(-1215752192L, Operation.toInt32(-1e11));
    assertEquals(727379968L, Operation.toInt32(-1e12));
    assertEquals(0, Operation.toInt32(0.5d));
    assertEquals(0, Operation.toInt32(-0.5d));
    assertEquals(1, Operation.toInt32(1.5d));
    assertEquals(-1, Operation.toInt32(-1.5d));
  }

  public final void testToUint32() {
    assertEquals(0, Operation.toUint32(0d));
    assertEquals(0, Operation.toUint32(-0d));
    assertEquals(0, Operation.toUint32(Double.POSITIVE_INFINITY));
    assertEquals(0, Operation.toUint32(Double.NEGATIVE_INFINITY));
    assertEquals(0, Operation.toUint32(Double.NaN));
    assertEquals(0, Operation.toUint32(0x100000000L));
    assertEquals(0, Operation.toUint32(-0x100000000L));
    assertEquals(0x7fffffffL, Operation.toUint32(0x7fffffffL));
    assertEquals(0x80000001L, Operation.toUint32(-0x7fffffffL));
    assertEquals(0x80000000L, Operation.toUint32(-0x80000000L));
    assertEquals(0x80000000L, Operation.toUint32(0x80000000L));
    assertEquals(1, Operation.toUint32(1));
    assertEquals(0xffffffffL, Operation.toUint32(-1));
    assertEquals(2, Operation.toUint32(2));
    assertEquals(0xfffffffeL, Operation.toUint32(-2));
    assertEquals((long) 1e6, Operation.toUint32(1e6));
    assertEquals((long) 1e7, Operation.toUint32(1e7));
    assertEquals((long) 1e8, Operation.toUint32(1e8));
    assertEquals((long) 1e9, Operation.toUint32(1e9));
    assertEquals(1410065408L, Operation.toUint32(1e10));
    assertEquals(1215752192L, Operation.toUint32(1e11));
    assertEquals(3567587328L, Operation.toUint32(1e12));
    assertEquals((long) (0x100000000L - 1e6), Operation.toUint32(-1e6));
    assertEquals((long) (0x100000000L - 1e7), Operation.toUint32(-1e7));
    assertEquals((long) (0x100000000L - 1e8), Operation.toUint32(-1e8));
    assertEquals((long) (0x100000000L - 1e9), Operation.toUint32(-1e9));
    assertEquals(2884901888L, Operation.toUint32(-1e10));
    assertEquals(3079215104L, Operation.toUint32(-1e11));
    assertEquals(727379968L, Operation.toUint32(-1e12));
    assertEquals(0, Operation.toUint32(0.5d));
    assertEquals(0, Operation.toUint32(-0.5d));
    assertEquals(1, Operation.toUint32(1.5d));
    assertEquals(0xffffffffL, Operation.toUint32(-1.5d));
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
    assertFolded(result, expr, false);
  }

  private void assertFolded(String result, String expr, boolean isFn)
      throws ParseException {
    Expression input = jsExpr(fromString(expr));
    if (input instanceof Operation) {
      Operation op = (Operation) input;
      for (Expression operand : op.children()) {
        // Fold some operands so we can test negative numbers.
        if ((Operation.is(operand, Operator.NEGATION)
            // and so that we can test corner cases around NaN and Infinity.
            || Operation.is(operand, Operator.DIVISION))
            && operand.children().get(0) instanceof NumberLiteral) {
          op.replaceChild(operand.fold(false), operand);
        }
      }
    }
    Expression actual = input.fold(isFn);
    assertEquals(expr, result, actual != null ? render(actual) : null);
  }
}
