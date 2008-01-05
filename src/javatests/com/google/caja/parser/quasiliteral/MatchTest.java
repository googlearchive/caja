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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.reporting.DevNullMessageQueue;
import junit.framework.TestCase;

import java.io.StringReader;
import java.net.URI;
import java.util.List;

/**
 *
 * @author ihab.awad@gmail.com
 */
public class MatchTest extends TestCase {
  public void testLiteral() throws Exception {
    List<QuasiNode.QuasiMatch> m = match(
        "x = @a;",
        "x = 3;");
    assertEquals(1, m.size());
    assertEquals(Operation.class, m.get(0).getRoot().getClass());
    assertEquals(Operator.ASSIGN, ((Operation)m.get(0).getRoot()).getOperator());
    assertEquals(IntegerLiteral.class, m.get(0).getBindings().get("a").getClass());
    assertEquals(3, ((IntegerLiteral)m.get(0).getBindings().get("a")).getValue().intValue());
    m = match(
        "x = @a;",
        "y = 3;");
    assertEquals(0, m.size());
  }

  public void testReference() throws Exception {
    List<QuasiNode.QuasiMatch> m = match(
        "x = @a;",
        "x = y;");
    assertEquals(1, m.size());
    assertEquals(Operation.class, m.get(0).getRoot().getClass());
    assertEquals(Operator.ASSIGN, ((Operation)m.get(0).getRoot()).getOperator());
    assertEquals(Reference.class, m.get(0).getBindings().get("a").getClass());
    assertEquals("y", ((Reference)m.get(0).getBindings().get("a")).getIdentifierName());
    m = match(
        "x = @a;",
        "y = y;");
    assertEquals(0, m.size());
  }

  public void testExpression() throws Exception {
    List<QuasiNode.QuasiMatch> m = match(
        "x = @a;",
        "x = pi() * (r * r);");
    assertEquals(1, m.size());
    assertEquals(Operation.class, m.get(0).getRoot().getClass());
    assertEquals(Operator.ASSIGN, ((Operation)m.get(0).getRoot()).getOperator());
    assertEquals(Operation.class, m.get(0).getBindings().get("a").getClass());
    assertEquals(Operator.MULTIPLICATION, ((Operation)m.get(0).getBindings().get("a")).getOperator());
  }

  public void testAnyExpression() throws Exception {
    List<QuasiNode.QuasiMatch> m = match(
        "@a;",
        "x = 3;");

    assertEquals(4, m.size());

    assertEquals(Operation.class, m.get(0).getRoot().getClass());
    assertEquals(Operator.ASSIGN, ((Operation)m.get(0).getRoot()).getOperator());    

    assertEquals(Reference.class, m.get(1).getRoot().getClass());

    assertEquals(Identifier.class, m.get(2).getRoot().getClass());
    assertEquals("x", ((Identifier)m.get(2).getRoot()).getValue());

    assertEquals(IntegerLiteral.class, m.get(3).getRoot().getClass());
    assertEquals(3, ((IntegerLiteral)m.get(3).getRoot()).getValue().intValue());
  }

  public void testFunctionIdentifier() throws Exception {
    List<QuasiNode.QuasiMatch> m = match(
        "function @a() { }",
        "function x() { }");
    assertEquals(1, m.size());
    assertEquals(FunctionConstructor.class, m.get(0).getRoot().getClass());
    assertEquals(Identifier.class, m.get(0).getBindings().get("a").getClass());
    assertEquals("x", ((Identifier)m.get(0).getBindings().get("a")).getValue());
  }

  public void testFunctionWithBody() throws Exception {
    List<QuasiNode.QuasiMatch> m = match(
        "function @a() { x = 3; y = 4; }",
        "function x() { x = 3; y = 4; }");
    assertEquals(1, m.size());
    assertEquals(Identifier.class, m.get(0).getBindings().get("a").getClass());
    assertEquals("x", ((Identifier)m.get(0).getBindings().get("a")).getValue());
    m = match(
        "function @a() { x = 3; y = 4; }",
        "function x() { x = 3; y = 3; }");
    assertEquals(0, m.size());
  }

  public void testFormalParams() throws Exception {
    List<QuasiNode.QuasiMatch> m = match(
        "function(@ps*) { @b*; }",
        "function(x, y) { x = 3; y = 4; }");
    assertEquals(1, m.size());
    assertEquals(FunctionConstructor.class, m.get(0).getRoot().getClass());
    assertEquals(2, m.get(0).getBindings().get("ps").children().size());
    assertEquals(FormalParam.class, m.get(0).getBindings().get("ps").children().get(0).getClass());
    assertEquals("x", ((FormalParam)m.get(0).getBindings().get("ps").children().get(0)).getIdentifierName());
    assertEquals(2, m.get(0).getBindings().get("b").children().size());
    assertEquals(ExpressionStmt.class, m.get(0).getBindings().get("b").children().get(0).getClass());
  }

  public void testDotAccessorReference() throws Exception {
    List<QuasiNode.QuasiMatch> m = match(
        "@a.@b;",
        "foo.bar;");
    assertEquals(1, m.size());
    assertEquals(Reference.class, m.get(0).getBindings().get("a").getClass());
    assertEquals("foo", ((Reference)m.get(0).getBindings().get("a")).getIdentifierName());
    assertEquals(Reference.class, m.get(0).getBindings().get("b").getClass());
    assertEquals("bar", ((Reference)m.get(0).getBindings().get("b")).getIdentifierName());
  }

  public void testBracketAccessorReference() throws Exception {
    List<QuasiNode.QuasiMatch> m = match(
        "@a[@b];",
        "foo[bar];");
    assertEquals(1, m.size());
    assertEquals(Reference.class, m.get(0).getBindings().get("a").getClass());
    assertEquals("foo", ((Reference)m.get(0).getBindings().get("a")).getIdentifierName());
    assertEquals(Reference.class, m.get(0).getBindings().get("b").getClass());
    assertEquals("bar", ((Reference)m.get(0).getBindings().get("b")).getIdentifierName());
  }

  public void testBracketAccessorStringLiteral() throws Exception {
    List<QuasiNode.QuasiMatch> m = match(
        "@a[@b];",
        "foo[\"bar\"];");
    assertEquals(1, m.size());
    assertEquals(Reference.class, m.get(0).getBindings().get("a").getClass());
    assertEquals("foo", ((Reference)m.get(0).getBindings().get("a")).getIdentifierName());
    assertEquals(StringLiteral.class, m.get(0).getBindings().get("b").getClass());
    assertEquals("bar", ((StringLiteral)m.get(0).getBindings().get("b")).getUnquotedValue());
  }

  public void testBracketAccessorIntegerLiteral() throws Exception {
    List<QuasiNode.QuasiMatch> m = match(
        "@a[@b];",
        "foo[3];");
    assertEquals(1, m.size());
    assertEquals(Reference.class, m.get(0).getBindings().get("a").getClass());
    assertEquals("foo", ((Reference)m.get(0).getBindings().get("a")).getIdentifierName());
    assertEquals(IntegerLiteral.class, m.get(0).getBindings().get("b").getClass());
    assertEquals(3, ((IntegerLiteral)m.get(0).getBindings().get("b")).getValue().intValue());
  }

  public void testNew() throws Exception {
    List<QuasiNode.QuasiMatch> m = match(
        "new @a(@b*);",
        "new foo(x, y, z);");
    assertEquals(1, m.size());
    assertEquals(Reference.class, m.get(0).getBindings().get("a").getClass());
    assertEquals("foo", ((Reference)m.get(0).getBindings().get("a")).getIdentifierName());
    assertEquals(Reference.class, m.get(0).getBindings().get("a").getClass());
    assertEquals(3, m.get(0).getBindings().get("b").children().size());
    assertEquals(Reference.class, m.get(0).getBindings().get("b").children().get(0).getClass());
  }

  public void testUnderscore() throws Exception {
    List<QuasiNode.QuasiMatch> m = match(
        "@a___ = 5;",
        "foo___ = 5;");
    assertEquals(1, m.size());
    assertEquals(Identifier.class, m.get(0).getBindings().get("a").getClass());
    assertEquals("foo", ((Identifier)m.get(0).getBindings().get("a")).getValue());
  }

  public static List<QuasiNode.QuasiMatch> match(String pattern, String source) throws Exception {
    QuasiNode qn = QuasiBuilder.parseQuasiNode(
        new InputSource(URI.create("built-in:///js-quasi-literals")),
        pattern);
    System.out.println(qn.render());
    List<QuasiNode.QuasiMatch> result = qn.match(parse(source));
    System.out.println(result);
    return result;
  }

  public static ParseTreeNode parse(String src) throws Exception {
    InputSource inputSource = new InputSource(URI.create("built-in:///js-test"));
    Parser parser = new Parser(
        new JsTokenQueue(
            new JsLexer(
                CharProducer.Factory.create(
                    new StringReader(src),
                    inputSource)),
            inputSource,
            JsTokenQueue.NO_NON_DIRECTIVE_COMMENT),
        DevNullMessageQueue.singleton());

    Statement topLevelStatement = parser.parse();
    parser.getTokenQueue().expectEmpty();
    return topLevelStatement;
  }
}