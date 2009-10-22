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

package com.google.caja.plugin.templates;

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.util.CajaTestCase;

public class JsConcatenatorTest extends CajaTestCase {
  public final void testEmptyAppender() throws ParseException {
    assertConcatenate(" '' ");
  }

  public final void testLiterals() throws ParseException {
    assertConcatenate(" 'foo' ", " 'foo' ");
    assertConcatenate(" '3' ", "3");
    assertConcatenate(" '-1' ", "-1");
    assertConcatenate(" '100000000000000000000' ", "1e20");
    assertConcatenate(" '1e+21' ", "1e21");
    assertConcatenate(" '-1e+21' ", "-1e21");
    assertConcatenate(" '0' ", "0");
    assertConcatenate(" 'true' ", "true");
    assertConcatenate(" 'false' ", "false");
    assertConcatenate(" 'null' ", "null");
  }

  public final void testMoreSingleAppends() throws ParseException {
    assertConcatenate(" a ? 'foo' : 'bar' ", " a ? 'foo' : 'bar' ");
    assertConcatenate(" rnd() ? '' + new Date : '' ", "rnd() ? new Date : '' ");
    assertConcatenate(" '' + (x || 'foo') ", " x || 'foo' ");
    assertConcatenate(" '' + ['foo'] ", " ['foo'] ");
    assertConcatenate(" x = f(), '' + x * x ", " x = f(), x * x ");
    assertConcatenate(" x = f(), '' + x * x ", " x = f(), '' + x * x ");
  }

  public final void testDecomposing() throws ParseException {
    assertConcatenate(" a + 'b' + c ", "a", "'b'", "c");
    assertConcatenate(" a + 'b' + c ", "a + 'b'", "c");
    assertConcatenate(" a + 'b' + c ", "a", "'b' + c");
    assertConcatenate(" a + 'b' + c ", "a + ('b' + c)");
    assertConcatenate(" a + 'b' + c ", "(a + 'b') + c");
    assertConcatenate(" a + 'b' + c ", " '' + a + 'b' + c");
  }

  public final void testNumericAdditionUnchanged() throws ParseException {
    assertConcatenate(" '' + (a + b + c) ", " a + b + c ");
    assertConcatenate(" '' + a + b + c ", "a", "b", "c");
  }

  public final void testCollapsing() throws ParseException {
    assertConcatenate(" 'Hello, ' + world + '!' ",
                     " 'Hello' ", " ', ' ", "world", " '!' ");
  }

  public final void testStringForcing() throws ParseException {
    assertConcatenate(
        "(rand() ? a : 'b') + 'foo' + bar",
        " rand() ? a : 'b' ", " 'foo' ", "bar");
    assertConcatenate(
        "(rand() ? '' + a : 'b') + foo + bar",
        " rand() ? a : 'b' ", "foo", "bar");
    // The (new) operator always returns an object which will cause the (+)
    // operator to return a string.
    assertConcatenate(
        "new Date(year, month, day) + message",
        "new Date(year, month, day)", "message");
    // But the (new) operator by itself does not always return a string, so in
    // the single operand case, we do need to force it to a string.
    assertConcatenate(
        "'' + new Date(year, month, day)",
        "new Date(year, month, day)");
    assertConcatenate(  // Coerces to a number.
        " '' + (+(new Date)) ", "+(new Date)");
  }

  public final void testSideEffects() throws ParseException {
    assertConcatenate(" foo(), '' ", "void foo()");
    assertConcatenate(" y(), 'xz' ", " 'x' ", " void y() ", " 'z' ");
    assertConcatenate(
        " y(), w(), 'xz' ", " 'x' ", " void y() ", " 'z' ", " void w() ");
    assertConcatenate(
        "(y(), 'x') + z() + (w(), '')",
        " 'x' ", " void y() ", " z() ", " void w() ");
    assertConcatenate(
        " x(), '' + y() ", "void x()", "y()");
    assertConcatenate(
        " x() + (y(), '') ", "x()", "void y()");
  }

  private void assertConcatenate(String goldenJs, String... parts)
      throws ParseException {
    JsConcatenator concat = new JsConcatenator();
    for (String part : parts) {
      Expression e = jsExpr(fromString(part));
      if (Operation.is(e, Operator.VOID)) {
        concat.forSideEffect(((Operation) e).children().get(0));
      } else {
        concat.append(e);
      }
    }
    assertEquals(
        render(jsExpr(fromString(goldenJs))),
        render(concat.toExpression(true)));
  }
}
