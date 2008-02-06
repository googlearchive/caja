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

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.TryStmt;
import com.google.caja.parser.js.Declaration;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.Message;
import com.google.caja.util.TestUtil;
import junit.framework.TestCase;

/**
 *
 * @author ihab.awad@gmail.com
 */
public class ScopeTest extends TestCase {
  private MessageQueue mq;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mq = TestUtil.createTestMessageQueue(new MessageContext());
  }

  public void tearDown() {
    this.mq = null;
  }

  public void testSimpleDeclaredFunction() throws Exception {
    ParseTreeNode n = TestUtil.parse(
        "var x = 3;" +
        "function foo() {" +
        "  var y = 3;" +
        "  z = 4;" +
        "};");
    Scope s0 = Scope.fromRootBlock((Block)n, mq);
    Scope s1 = Scope.fromFunctionConstructor(s0, findFunctionConstructor(n, "foo"));

    assertTrue(s0.isDefined("x"));
    assertTrue(s0.isGlobal("x"));
    assertFalse(s0.isFunction("x"));
    assertFalse(s0.isDeclaredFunction("x"));
    assertFalse(s0.isConstructor("x"));

    assertTrue(s0.isDefined("foo"));
    assertTrue(s0.isGlobal("foo"));
    assertTrue(s0.isFunction("foo"));
    assertTrue(s0.isDeclaredFunction("foo"));
    assertFalse(s0.isConstructor("foo"));

    assertFalse(s0.isDefined("y"));
    assertTrue(s0.isGlobal("y"));
    assertFalse(s0.isFunction("y"));
    assertFalse(s0.isDeclaredFunction("y"));
    assertFalse(s0.isConstructor("y"));

    assertFalse(s0.isDefined("z"));
    assertTrue(s0.isGlobal("z"));
    assertFalse(s0.isFunction("z"));
    assertFalse(s0.isDeclaredFunction("z"));
    assertFalse(s0.isConstructor("z"));

    assertTrue(s1.isDefined("x"));
    assertTrue(s1.isGlobal("x"));
    assertFalse(s1.isFunction("x"));
    assertFalse(s1.isDeclaredFunction("x"));
    assertFalse(s1.isConstructor("x"));

    assertTrue(s1.isDefined("foo"));
    assertFalse(s1.isGlobal("foo"));
    assertTrue(s1.isFunction("foo"));
    assertFalse(s1.isDeclaredFunction("foo"));
    assertFalse(s1.isConstructor("foo"));

    assertTrue(s1.isDefined("y"));
    assertFalse(s1.isGlobal("y"));
    assertFalse(s1.isFunction("y"));
    assertFalse(s1.isDeclaredFunction("y"));
    assertFalse(s1.isConstructor("y"));

    assertFalse(s1.isDefined("z"));
    assertTrue(s1.isGlobal("z"));
    assertFalse(s1.isFunction("z"));
    assertFalse(s1.isDeclaredFunction("z"));
    assertFalse(s1.isConstructor("z"));
  }

  public void testAnonymousFunction() throws Exception {
    ParseTreeNode n = TestUtil.parse(
        "var x = function() {};");
    Scope s0 = Scope.fromRootBlock((Block)n, mq);
    Scope s1 = Scope.fromFunctionConstructor(s0, findFunctionConstructor(n, null));

    assertTrue(s0.isDefined("x"));
    assertTrue(s0.isGlobal("x"));
    assertFalse(s0.isFunction("x"));
    assertFalse(s0.isDeclaredFunction("x"));

    assertTrue(s1.isDefined("x"));
    assertTrue(s1.isGlobal("x"));
    assertFalse(s1.isFunction("x"));
    assertFalse(s1.isDeclaredFunction("x"));
  }

  public void testNamedFunction() throws Exception {
    ParseTreeNode n = TestUtil.parse(
        "var x = function foo() {};");
    Scope s0 = Scope.fromRootBlock((Block)n, mq);
    Scope s1 = Scope.fromFunctionConstructor(s0, findFunctionConstructor(n, "foo"));
    
    assertTrue(s0.isDefined("x"));
    assertTrue(s0.isGlobal("x"));
    assertFalse(s0.isFunction("x"));
    assertFalse(s0.isDeclaredFunction("x"));

    assertFalse(s0.isDefined("foo"));
    assertTrue(s0.isGlobal("foo"));
    assertFalse(s0.isFunction("foo"));
    assertFalse(s0.isDeclaredFunction("foo"));

    assertTrue(s1.isDefined("x"));
    assertTrue(s1.isGlobal("x"));
    assertFalse(s1.isFunction("x"));
    assertFalse(s1.isDeclaredFunction("x"));

    assertTrue(s1.isDefined("foo"));
    assertFalse(s1.isGlobal("foo"));
    assertTrue(s1.isFunction("foo"));
    assertFalse(s1.isDeclaredFunction("foo"));
  }

  public void testNamedFunctionSameName() throws Exception {
    ParseTreeNode n = TestUtil.parse(
        "var x = function x() {};");
    Scope s0 = Scope.fromRootBlock((Block)n, mq);
    Scope s1 = Scope.fromFunctionConstructor(s0, findFunctionConstructor(n, "x"));

    assertTrue(s0.isDefined("x"));
    assertTrue(s0.isGlobal("x"));
    assertFalse(s0.isFunction("x"));
    assertFalse(s0.isDeclaredFunction("x"));

    assertTrue(s1.isDefined("x"));
    assertFalse(s1.isGlobal("x"));
    assertTrue(s1.isFunction("x"));
    assertFalse(s1.isDeclaredFunction("x"));    
  }

  public void testFormalParams() throws Exception {
    ParseTreeNode n = TestUtil.parse(
        "function f(x) {};");
    Scope s0 = Scope.fromRootBlock((Block)n, mq);
    Scope s1 = Scope.fromFunctionConstructor(s0, findFunctionConstructor(n, "f"));

    assertFalse(s0.isDefined("x"));
    assertTrue(s1.isDefined("x"));    
  }

  public void testCatchBlocks() throws Exception {
    ParseTreeNode n = TestUtil.parse(
        "try { } catch (e) { var x; }");

    Block b = (Block) n;
    TryStmt t = (TryStmt) b.children().get(0);
    CatchStmt c = (CatchStmt) t.children().get(1);

    Scope s0 = Scope.fromRootBlock((Block)n, mq);
    Scope s1 = Scope.fromCatchStmt(s0, c);

    // e only defined in catch scope
    assertFalse(s0.isDefined("e"));
    assertTrue(s1.isDefined("e"));
    assertTrue(s1.isException("e"));

    // Definition of x appears in main scope
    assertTrue(s0.isDefined("x"));
  }

  public void testBodyOfNamedFunction() throws Exception {
    ParseTreeNode n = TestUtil.parse(
        "function foo() { var x; }");

    Declaration fd = findNodeWithIdentifier(n, Declaration.class, "foo");
    FunctionConstructor fc = (FunctionConstructor)fd.getInitializer();
    Block body = fc.getBody();

    Scope s0 = Scope.fromRootBlock((Block)n, mq);
    Scope s1 = Scope.fromBlock(s0, body);

    assertEquals(0, mq.getMessages().size());
    assertTrue(s0.isFunction("foo"));
    assertTrue(s0.isDeclaredFunction("foo"));
    assertTrue(s1.isFunction("foo"));
    assertTrue(s1.isDeclaredFunction("foo"));
  }

  public void testSymbolRedefinedError() throws Exception {
    ParseTreeNode n = TestUtil.parse(
        "function foo() {} var foo;");

    Scope s0 = Scope.fromRootBlock((Block)n, mq);

    assertMsgType(MessageType.SYMBOL_REDEFINED, mq.getMessages().get(0));
    assertMsgLevel(MessageLevel.ERROR, mq.getMessages().get(0));
  }

  public void testMaskedExceptionVariablesErrorA() throws Exception {
    ParseTreeNode n = TestUtil.parse(
        "var e; try { } catch (e) { var x; }");

    Block b = (Block) n;
    TryStmt t = (TryStmt) b.children().get(1);
    CatchStmt c = (CatchStmt) t.children().get(1);

    Scope s0 = Scope.fromRootBlock((Block)n, mq);
    Scope s1 = Scope.fromCatchStmt(s0, c);

    assertMsgType(MessageType.MASKING_SYMBOL, mq.getMessages().get(0));
    assertMsgLevel(MessageLevel.ERROR, mq.getMessages().get(0));
  }

  public void testMaskedExceptionVariablesErrorB() throws Exception {
    ParseTreeNode n = TestUtil.parse(
        "try { } catch (e) { function foo() { var e; } }");

    Block b = (Block)n;
    TryStmt t = (TryStmt)b.children().get(0);
    CatchStmt c = (CatchStmt)t.children().get(1);
    Declaration d = findNodeWithIdentifier(n, Declaration.class, "foo");
    FunctionConstructor fc = (FunctionConstructor)d.getInitializer();

    Scope s0 = Scope.fromRootBlock((Block)n, mq);
    Scope s1 = Scope.fromCatchStmt(s0, c);
    Scope s2 = Scope.fromFunctionConstructor(s1, fc);

    // TODO(ihab.awad): This spurious LINT message is because 'foo' is declared in the outer
    // scope, as DECLARED_FUNCTION, and within the function body, as FUNCTION. Fix the impl so
    // this spurious message is not emitted.
    assertMsgType(MessageType.MASKING_SYMBOL, mq.getMessages().get(0));
    assertMsgLevel(MessageLevel.LINT, mq.getMessages().get(0));

    assertMsgType(MessageType.MASKING_SYMBOL, mq.getMessages().get(1));
    assertMsgLevel(MessageLevel.ERROR, mq.getMessages().get(1));
  }

  public void testMaskedExceptionVariablesSame() throws Exception {
    ParseTreeNode n = TestUtil.parse(
        "try { } catch (e) { try { } catch (e) { var x; } }");

    Block outerBlock = (Block)n;
    TryStmt t0 = (TryStmt)outerBlock.children().get(0);
    CatchStmt c0 = t0.getCatchClause();
    Block b0 = (Block)c0.getBody();
    TryStmt t1 = (TryStmt)b0.children().get(0);
    CatchStmt c1 = t1.getCatchClause();
    Block b1 = (Block)c1.getBody();

    Scope sn = Scope.fromRootBlock((Block)n, mq);
    Scope sc0 = Scope.fromCatchStmt(sn, c0);
    Scope sc1 = Scope.fromCatchStmt(sc0, c1);

    assertEquals(0, mq.getMessages().size());
  }

  public void testConstructor() throws Exception {
    ParseTreeNode n = TestUtil.parse(
        "function ctor() { this.x = 3; }" +
        "function notctor() { x = 3; }");
    Scope s = Scope.fromRootBlock((Block)n, mq);

    assertTrue(s.isConstructor("ctor"));
    assertTrue(s.isDeclaredFunction("ctor"));
    assertTrue(s.isFunction("ctor"));

    assertFalse(s.isConstructor("notctor"));
    assertTrue(s.isDeclaredFunction("notctor"));
    assertTrue(s.isFunction("notctor"));
  }

  public void testPrimordialObjects() throws Exception {
    Scope s = Scope.fromRootBlock((Block)TestUtil.parse("{}"), mq);

    assertDefinedGlobalValue(s, "Global");
    assertDefinedGlobalValue(s, "Function");
    assertDefinedGlobalValue(s, "Array");
    assertDefinedGlobalValue(s, "String");
    assertDefinedGlobalValue(s, "Boolean");
    assertDefinedGlobalValue(s, "Number");
    assertDefinedGlobalValue(s, "Math");
    assertDefinedGlobalValue(s, "RegExp");

    assertDefinedGlobalCtor(s, "Object");
    assertDefinedGlobalCtor(s, "Date");    
    assertDefinedGlobalCtor(s, "Error");
    assertDefinedGlobalCtor(s, "EvalError");
    assertDefinedGlobalCtor(s, "RangeError");
    assertDefinedGlobalCtor(s, "ReferenceError");
    assertDefinedGlobalCtor(s, "SyntaxError");
    assertDefinedGlobalCtor(s, "TypeError");
    assertDefinedGlobalCtor(s, "URIError");
  }

  private void assertDefinedGlobalValue(Scope s, String name) {
    assertTrue(s.isDefined(name));
    assertTrue(s.isGlobal(name));
    assertFalse(s.isConstructor(name));
    assertFalse(s.isDeclaredFunction(name));
    assertFalse(s.isFunction(name));
  }

  private void assertDefinedGlobalCtor(Scope s, String name) {
    assertTrue(s.isDefined(name));
    assertTrue(s.isGlobal(name));
    assertTrue(s.isConstructor(name));
    assertTrue(s.isDeclaredFunction(name));
    assertTrue(s.isFunction(name));    
  }

  private FunctionConstructor findFunctionConstructor(ParseTreeNode root, String name) {
    return findNodeWithIdentifier(root, FunctionConstructor.class, name);
  }

  private static class Holder<T> { T value; }

  @SuppressWarnings("unchecked")
  private <T extends ParseTreeNode> T findNodeWithIdentifier(
      ParseTreeNode root,
      final Class<T> clazz,
      final String identifierValue) {
    final Holder<T> result = new Holder<T>();

    root.acceptPreOrder(new Visitor() {
      public boolean visit(AncestorChain<?> chain) {
        if (clazz.isAssignableFrom(chain.node.getClass()) &&
            chain.node.children().size() > 0 &&
            chain.node.children().get(0) instanceof Identifier) {
          Identifier id = (Identifier)chain.node.children().get(0);
          if ((identifierValue == null && id.getValue() == null) ||
              (identifierValue != null && identifierValue.equals(id.getValue()))) {
            assertNull(result.value);
            result.value = (T)chain.node;
            return false;
          }
        }
        return true;
      }
    },
    null);

    assertNotNull(result.value);
    return result.value;
  }

  private void assertMsgType(MessageType type, Message message) {
    assertEquals(type, message.getMessageType());
  }

  private void assertMsgLevel(MessageLevel level, Message message) {
    assertTrue(level.compareTo(message.getMessageLevel()) <= 0);
  }
}
