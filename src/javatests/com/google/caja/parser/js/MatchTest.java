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

package com.google.caja.parser.js;

import java.io.StringReader;
import java.net.URI;
import java.util.Map;

import junit.framework.TestCase;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.TestUtil;

/**
 * Test case of the matchHere(...) method as applied to JavaScript ASTs.
 * 
 * @author ihab.awad@gmail.com
 */
public class MatchTest extends TestCase {
  public void testMatchLiteral() throws Exception {
    Map<String, ParseTreeNode> result = match(
        "@foo;",
        "3;");
    assertNotNull(result);
    assertEquals(
        IntegerLiteral.class,
        result.get("foo").children().get(0).getClass());
    assertEquals(
        new Long(3),
        result.get("foo").children().get(0).getValue());
  }

  public void testMatchExpression() throws Exception {
    Map<String, ParseTreeNode> result = match(
        "x = @foo;",
        "x = 1 + sqrt(2);");
    assertNotNull(result);
    assertEquals(
        Operation.class,
        result.get("foo").getClass());
    assertEquals(
        Operator.ADDITION,
        ((Operation)result.get("foo")).getOperator());
  }

  public void testMatchFormalParams() throws Exception {
    Map<String, ParseTreeNode> result = match(
        "function foo(@p*) { }",
        "function foo(x, y, z) { }");
    assertNotNull(result);
    assertEquals(
        3,
        result.get("p").children().size());
    assertEquals(
        FormalParam.class,
        result.get("p").children().get(0).getClass());
    assertEquals(
        "x",
        result.get("p").children().get(0).getValue());
  }
  
  public void testMatchActualParams() throws Exception {
    Map<String, ParseTreeNode> result = match(
        "y = foo(@p*);",
        "y = foo(x, y, z);");
    assertNotNull(result);
    assertEquals(
        3,
        result.get("p").children().size());
    assertEquals(
        Reference.class,
        result.get("p").children().get(0).getClass());
    assertEquals(
        "x",
        result.get("p").children().get(0).getValue());
  }
  
  public void testMatchMultipleEqualPlaces() throws Exception {
    Map<String, ParseTreeNode> result = match(
        "x = @foo; y = @foo;",
        "x = 3; y = 3;");
    assertNotNull(result);
    assertEquals(
        IntegerLiteral.class,
        result.get("foo").getClass());
    assertEquals(
        new Long(3),
        result.get("foo").getValue());
    result = match(
        "x = @foo; y = @foo;",
        "x = 3; y = 4;");
    assertNull(result);
  }

  public void testMatchFunctionDeclaration() throws Exception {
    Map<String, ParseTreeNode> result = match(
        "function @f(x) { print(x); }",
        "function foo(x) { print(x); }");
    assertNotNull(result);
    assertEquals(
        Reference.class,
        result.get("f").getClass());
    assertEquals(
        "foo",
        result.get("f").getValue());
  }
  
  public void testMatchQuantifierPlus() throws Exception {
    Map<String, ParseTreeNode> result = match(
        "x = 3; @foo+;",
        "x = 3; y = 4; z = 5;");
    assertNotNull(result);
    assertEquals(
        2,
        result.get("foo").children().size());
    result = match(
        "x = 3; @foo+;",
        "x = 3;");
    assertNull(result);
  }
  
  public void testMatchQuantifierStar() throws Exception {
    Map<String, ParseTreeNode> result = match(
        "x = 3; @foo*;",
        "x = 3; y = 4; z = 5;");
    assertNotNull(result);
    assertEquals(
        2,
        result.get("foo").children().size());
    result = match(
        "x = 3; @foo*;",
        "x = 3;");
    assertNotNull(result);
    assertEquals(
        0,
        result.get("foo").children().size());
  }
  
  private static Map<String, ParseTreeNode> match(
      String pattern, String specimen)
      throws Exception {
    return parse(pattern).matchHere(parse(specimen));
  }
  
  public static ParseTreeNode parse(String src) throws Exception {
    MessageContext mc = new MessageContext();
    MessageQueue mq = TestUtil.createTestMessageQueue(mc);
    InputSource is = new InputSource(new URI("file:///no/input/source"));
    CharProducer cp = CharProducer.Factory.create(new StringReader(src), is);
    JsLexer lexer = new JsLexer(cp);
    JsTokenQueue tq = new JsTokenQueue(lexer, is, JsTokenQueue.NO_NON_DIRECTIVE_COMMENT);
    Parser p = new Parser(tq, mq);
    Statement stmt = p.parse();
    p.getTokenQueue().expectEmpty();
    return stmt;
  }
}
