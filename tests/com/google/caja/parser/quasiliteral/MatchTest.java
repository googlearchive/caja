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

import com.google.caja.lexer.InputSource;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.ReturnStmt;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.util.CajaTestCase;

import java.net.URI;
import java.util.Map;

/**
 *
 * @author ihab.awad@gmail.com
 */
public class MatchTest extends CajaTestCase {
  private Map<String, ParseTreeNode> m;

  public final void testExactMatch() throws Exception {
    String code = "function foo() { var a = b; }";
    match(code, code);
    assertNotNull(m);

    match(code, "function bar() { var c = d; }");
    assertNull(m);
  }

  public final void testSingleHole() throws Exception {
    match(
        "x; @a;",
        "x;");
    assertNull(m);

    match(
        "x; @a;",
        "x; x;");
    assertNotNull(m);
    assertEquals(ExpressionStmt.class, m.get("a").getClass());

    match(
        "x; @a;",
        "x; x; x;");
    assertNull(m);
  }

  public final void testSingleOptionalHole() throws Exception {
    match(
        "x; @a?;",
        "x;");
    assertNotNull(m);
    assertNull(m.get("a"));

    match(
        "x; @a?;",
        "x; x;");
    assertNotNull(m);
    assertEquals(ExpressionStmt.class, m.get("a").getClass());

    match(
        "x; @a?;",
        "x; x; x");
    assertNull(m);
   }

  public final void testMultipleHole() throws Exception {
    match(
        "x; @a*;",
        "x;");
    assertNotNull(m);
    assertEquals(ParseTreeNodeContainer.class, m.get("a").getClass());
    assertEquals(0, m.get("a").children().size());

    match(
        "x; @a*;",
        "x; x;");
    assertNotNull(m);
    assertEquals(ParseTreeNodeContainer.class, m.get("a").getClass());
    assertEquals(1, m.get("a").children().size());
    assertEquals(ExpressionStmt.class, m.get("a").children().get(0).getClass());

    match(
        "x; @a*;",
        "x; x; x;");
    assertNotNull(m);
    assertEquals(2, m.get("a").children().size());
   }

  public final void testMultipleNonemptyHole() throws Exception {
    match(
        "x; @a+;",
        "x;");
    assertNull(m);

    match(
        "x; @a+;",
        "x; x;");
    assertNotNull(m);
    assertEquals(ParseTreeNodeContainer.class, m.get("a").getClass());
    assertEquals(1, m.get("a").children().size());
    assertEquals(ExpressionStmt.class, m.get("a").children().get(0).getClass());

    match(
        "x; @a+;",
        "x; x; x;");
    assertNotNull(m);
    assertEquals(2, m.get("a").children().size());
  }

  public final void testObjectConstructorHole() throws Exception {
    match(
        "({ @k*: @v* });",
        "({ })");
    assertNotNull(m);
    assertEquals(ParseTreeNodeContainer.class, m.get("k").getClass());
    assertEquals(ParseTreeNodeContainer.class, m.get("v").getClass());
    assertEquals(0, m.get("k").children().size());
    assertEquals(0, m.get("v").children().size());

    match(
        "({ @k*: @v*, foo: @bar });",
        "({ foo: bar, boo: baz })");
    assertNotNull(m);
    assertEquals(ParseTreeNodeContainer.class, m.get("k").getClass());
    assertEquals(ParseTreeNodeContainer.class, m.get("v").getClass());
    assertEquals(1, m.get("k").children().size());
    assertEquals(1, m.get("v").children().size());
    assertEquals(Reference.class, m.get("bar").getClass());
    assertEquals("bar", ((Reference) m.get("bar")).getIdentifierName());

    match(
        "({ @k* : @v* });",
        "({ a: 3, b: 4 })");
    assertNotNull(m);
    assertEquals(2, m.get("k").children().size());
    assertEquals(2, m.get("v").children().size());
    assertEquals(
        "a",
        ((StringLiteral) m.get("k").children().get(0)).getUnquotedValue());
    assertEquals(
        "b",
        ((StringLiteral) m.get("k").children().get(1)).getUnquotedValue());
    assertEquals(
        3,
        ((IntegerLiteral) m.get("v").children().get(0)).getValue().intValue());
    assertEquals(
        4,
        ((IntegerLiteral) m.get("v").children().get(1)).getValue().intValue());
  }

  public final void testTrailingUnderscoreIdentifierHole() throws Exception {
    match(
        "@a___ = 5;",
        "foo___ = 5;");
    assertNotNull(m);
    assertEquals("foo", ((Identifier) m.get("a")).getValue());
  }

  public final void testLiteral() throws Exception {
    match(
        "x = @a;",
        "x = 3;");
    assertNotNull(m);
    assertEquals(3, ((IntegerLiteral) m.get("a")).getValue().intValue());

    match(
        "x = @a;",
        "y = 3;");
    assertNull(m);
  }

  public final void testReference() throws Exception {
    match(
        "x = @a;",
        "x = y;");
    assertNotNull(m);
    assertEquals("y", ((Reference) m.get("a")).getIdentifierName());

    match(
        "x = @a;",
        "y = y;");
    assertNull(m);
  }

  public final void testExpression() throws Exception {
    match(
        "x = @a;",
        "x = pi() * (r * r);");
    assertNotNull(m);
    assertEquals(Operator.MULTIPLICATION, ((Operation) m.get("a")).getOperator());
  }

  public final void testFunctionIdentifier() throws Exception {
    match(
        "function @a() { }",
        "function x() { }");
    assertNotNull(m);
    assertEquals("x", ((Identifier) m.get("a")).getValue());
  }

  public final void testFunctionWithBody() throws Exception {
    match(
        "function @a() { x = 3; y = 4; }",
        "function x() { x = 3; y = 4; }");
    assertNotNull(m);
    assertEquals("x", ((Identifier) m.get("a")).getValue());

    match(
        "function @a() { x = 3; y = 4; }",
        "function x() { x = 3; y = 3; }");
    assertNull(m);
  }

  public final void testFormalParams() throws Exception {
    match(
        "function(@ps*) { @b*; }",
        "function(x, y) { x = 3; y = 4; }");
    assertNotNull(m);
    assertEquals(2, m.get("ps").children().size());
    assertEquals(FormalParam.class, m.get("ps").children().get(0).getClass());
    assertEquals(
        "x",
        ((FormalParam) m.get("ps").children().get(0)).getIdentifierName());
    assertEquals(2, m.get("b").children().size());
    assertEquals(ExpressionStmt.class, m.get("b").children().get(0).getClass());
  }

  public final void testFunctionWithOptionalName() throws Exception {
    match(
        "function @i?(@actuals*) { @body* }",
        "function x(a, b) { return; }");
    assertNotNull(m);
    assertEquals("x", ((Identifier) m.get("i")).getValue());

    match(
        "function @i?(@actuals*) { @body* }",
        "function (a, b) { return; };");
    assertNotNull(m);
    assertNull(m.get("i"));
    assertEquals(
        "a",
        ((FormalParam) m.get("actuals").children().get(0)).getIdentifierName());
    assertEquals(
        "b",
        ((FormalParam) m.get("actuals").children().get(1)).getIdentifierName());
    assertEquals(ReturnStmt.class, m.get("body").children().get(0).getClass());

    match(
        "function @i?(@actuals*) { @body* }",
        "function f(a, b) { return; }");
    assertNotNull(m);
    assertEquals("f", ((Identifier) m.get("i")).getValue());
    assertEquals(
        "a",
        ((FormalParam) m.get("actuals").children().get(0)).getIdentifierName());
    assertEquals(
        "b",
        ((FormalParam) m.get("actuals").children().get(1)).getIdentifierName());
    assertEquals(ReturnStmt.class, m.get("body").children().get(0).getClass());
  }

  public final void testDotAccessorReference() throws Exception {
    match(
        "@a.@b;",
        "foo.bar;");
    assertNotNull(m);
    assertEquals("foo", ((Reference) m.get("a")).getIdentifierName());
    assertEquals("bar", ((Reference) m.get("b")).getIdentifierName());
  }

  public final void testBracketAccessorReference() throws Exception {
    match(
        "@a[@b];",
        "foo[bar];");
    assertNotNull(m);
    assertEquals("foo", ((Reference) m.get("a")).getIdentifierName());
    assertEquals("bar", ((Reference) m.get("b")).getIdentifierName());
  }

  public final void testBracketAccessorStringLiteral() throws Exception {
    match(
        "@a[@b];",
        "foo[\"bar\"];");
    assertNotNull(m);
    assertEquals("foo", ((Reference) m.get("a")).getIdentifierName());
    assertEquals("bar", ((StringLiteral) m.get("b")).getUnquotedValue());
  }

  public final void testBracketAccessorIntegerLiteral() throws Exception {
    match(
        "@a[@b];",
        "foo[3];");
    assertNotNull(m);
    assertEquals("foo", ((Reference) m.get("a")).getIdentifierName());
    assertEquals(3, ((IntegerLiteral) m.get("b")).getValue().intValue());
  }

  public final void testNew() throws Exception {
    match(
        "new @a(@b*);",
        "new foo(x, y, z);");
    assertNotNull(m);
    assertEquals("foo", ((Reference) m.get("a")).getIdentifierName());
    assertEquals(Reference.class, m.get("a").getClass());
    assertEquals(3, m.get("b").children().size());
    assertEquals(Reference.class, m.get("b").children().get(0).getClass());
  }

  public final void testFunctionWithNulIdentifier() throws Exception {
    match(
        "function @f() {}",
        "var foo = function() {};");
    assertNull(m);

    match(
        "function() {}",
        "var foo = function() {};");
    assertNotNull(m);
    assertEquals(0, m.size());

    match(
        "function @f() {}",
        "function foo() {}");
    assertNotNull(m);
    assertEquals("foo", ((Identifier) m.get("f")).getName());
  }

  public final void testDirectivePrologueInFunctionBodies() throws Exception {
    match(
        "function () { 'use strict'; @stmts* }",
        "function () { return 4; };");
    assertNull(m);

    match(
        "function () { 'use strict'; @stmts* }",
        "function () { 'use strict'; return 4; };");
    assertNotNull(m);
    assertEquals(ReturnStmt.class, m.get("stmts").children().get(0).getClass());

    match(
        "function f() { 'use strict'; @stmts* }",
        "function f() { return 4; }");
    assertNull(m);

    match(
        "function f() { 'use strict'; @stmts* }",
        "function f() { 'use strict'; return 4; }");
    assertNotNull(m);
    assertEquals(ReturnStmt.class, m.get("stmts").children().get(0).getClass());

    match(
        "function f() { 'use strict'; @stmts* }",
        "(function f() { return 4; });");
    assertNull(m);

    match(
        "function f() { 'use strict'; @stmts* }",
        "(function f() { 'use strict'; return 4; });");
    assertNotNull(m);
    assertEquals(ReturnStmt.class, m.get("stmts").children().get(0).getClass());

    // Partial matches work too
    match(
        "function f() { 'use strict'; @stmts* }",
        "(function f() { 'use strict'; 'use strict'; return 4; });");
    assertNotNull(m);
    assertEquals(ReturnStmt.class, m.get("stmts").children().get(0).getClass());
  }

  public final void testStringLiteralQuasis() throws Exception {
    match(
        "o['@p'] = q;",
        "o['P'] = q;");
    assertNotNull(m);
    assertEquals("P", ((Identifier) m.get("p")).getName());

    match("function @name() { @body* }", "function foo() {}");
    assertNotNull(m);
    assertEquals("foo", ((Identifier) m.get("name")).getName());
    assertEquals(
        render(jsExpr(fromString("fakeGlobals['foo'] = function foo() {}"))),
        render(QuasiBuilder.substV(
            "fakeGlobals['@name'] = function @name() { @body* }",
            "name", m.get("name"),
            "body", m.get("body"))));
  }

  public final void testStringEquivalence() throws Exception {
    assertTrue(QuasiBuilder.match("('foo')", jsExpr(fromString("'foo'"))));
    assertTrue(QuasiBuilder.match("('bar')", jsExpr(fromString("'bar'"))));
    assertTrue(QuasiBuilder.match("('foo')", jsExpr(fromString("'f\\oo'"))));
    assertTrue(QuasiBuilder.match("('foo')", jsExpr(fromString("\"foo\""))));
    assertTrue(QuasiBuilder.match("('f\\oo')", jsExpr(fromString("'foo'"))));
    assertTrue(QuasiBuilder.match("('f\\oo')", jsExpr(fromString("'f\\oo'"))));
    assertFalse(QuasiBuilder.match("('bar')", jsExpr(fromString("'foo'"))));
  }

  private void match(String pattern, String source)
      throws Exception {
    QuasiNode qn = QuasiBuilder.parseQuasiNode(
        new InputSource(URI.create("built-in:///js-quasi-literals")),
        pattern);
    System.out.println(qn.render());
    m = null;
    findMatch(qn, quasi(fromString(source)));
    if (m != null) { System.out.println(m); }
  }

  private void findMatch(QuasiNode qn, ParseTreeNode n) {
    m = qn.match(n);
    if (m != null) { return; }
    for (ParseTreeNode c : n.children()) {
      findMatch(qn, c);
      if (m != null) { return; }
    }
  }
}
