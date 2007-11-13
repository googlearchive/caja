// Copyright (C) 2006 Google Inc.
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

package com.google.caja.plugin;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.NullLiteral;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.TestUtil;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public class ExpressionSanitizerTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testCloseExpressionLoopholes() throws Exception {
    String golden = TestUtil.readResource(getClass(), "sanitizergolden1.js");

    MessageContext mc = new MessageContext();
    MessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(new OutputStreamWriter(System.out)), mc);
    ParseTreeNode pt = TestUtil.parseTree(getClass(), "sanitizerinput1.js", mq);
    new ExpressionSanitizer(mq).sanitize(pt);  // TODO: test output value
    StringBuilder out = new StringBuilder();
    RenderContext rc = new RenderContext(mc, out);

    pt.render(rc);
    assertEquals(golden.trim(), out.toString().trim());
  }

  public void testSyntheticnessAndFilePositionInference() throws Exception {
    String golden = TestUtil.readResource(getClass(), "sanitizergolden2.txt");

    MessageContext mc = new MessageContext();
    mc.relevantKeys = Collections.singleton(ExpressionSanitizer.SYNTHETIC);
    MessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(new OutputStreamWriter(System.out)), mc);
    Statement pt = TestUtil.parseTree(getClass(), "sanitizerinput1.js", mq);
    new ExpressionSanitizer(mq).sanitize(pt);
    StringBuilder out = new StringBuilder();

    pt.formatTree(mc, 0, out);
    assertEquals(golden.trim(), out.toString().trim());

    checkFilePositionInviariants(pt);
  }

  public void testArithmetic() throws Exception {
    runTest("1 + 2 * 3 / 4 - -5", "{\n  1 + 2 * 3 / 4 - -5;\n}", true);
  }

  public void testFunctionCalls() throws Exception {
    runTest("f()", "{\n  f.call(this);\n}", true);
  }

  public void testFunctionDeclarations() throws Exception {
    // nothing changed
    runTest("function f() {}",
            "{\n"
            + "  function f() {\n"
            + "  }\n"
            + "}",
            true);
    // a function that references this needs an assertion at the front
    runTest("function f() { return this; }",
            "{\n"
            + "  function f() {\n"
            + "    plugin_require___(this !== window);\n"
            + "    return this;\n"
            + "  }\n"
            + "}",
            true);
    // a function that doesn't reference this but contains a function that does,
    // doesn't need an assertion at the front
    runTest("function f() { return function () { return this; }; }",
            "{\n"
            + "  function f() {\n"
            + "    return function () {\n"
            + "      plugin_require___(this !== window);\n"
            + "      return this;\n"
            + "    };\n"
            + "  }\n"
            + "}",
            true);
    // a function that reference this but returns a function that does not,
    // does need an assertion at the front
    runTest("function f() { "
            + "var self = this;"
            + "return function () { return self; };"
            + "}",
            "{\n"
            + "  function f() {\n"
            + "    plugin_require___(this !== window);\n"
            + "    var self = this;\n"
            + "    return function () {\n"
            + "      return self;\n"
            + "    };\n"
            + "  }\n"
            + "}",
            true);
  }


  public void testBadDeclarations1() throws Exception {
    runTest("function plugin_get___() { ; }",
            "{\n"
            + "  function plugin_get___() {\n"
            + "    ;\n"
            + "  }\n"
            + "}",
        false);
  }

  public void testBadDeclarations2() throws Exception {
    runTest("var out___ = [ '<script></script>' ];",
            "{\n  var out___ = ['<script></script>'];\n}", false);
  }

  public void testBadReference1() throws Exception {
    runTest("var a = out___;", "{\n  var a = out___;\n}", false);
  }

  public void testBadReference2() throws Exception {
    runTest("f(MyClass.prototype);",
            "{\n  f.call(this, plugin_get___(MyClass, 'prototype'));\n}",
            // allowed, but should be denied by plugin_get___
            true);
  }

  public void testAssignmentToPrototypeOk1() throws Exception {
    runTest("MyClass.prototype = new MyOtherClass;",
            "{\n  MyClass.prototype = new MyOtherClass;\n}", true);
  }

  public void testAssignmentToPrototypeOk2() throws Exception {
    runTest("MyClass.prototype.method = f;",
            "{\n  MyClass.prototype.method = f;\n}", true);
  }

  public void testAssignmentToPrototypeNoCheating() throws Exception {
    runTest("x = MyClass.prototype = new MyOtherClass;",
            "{\n  x = MyClass.prototype = new MyOtherClass;\n}", false);
  }

  private void runTest(String input, String golden, boolean sanitary)
      throws Exception {
    MessageContext mc = new MessageContext();
    MessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(new OutputStreamWriter(System.out)), mc);

    InputSource is = new InputSource(new URI("test:///ExpressionSanitizer"));
    CharProducer cp = CharProducer.Factory.create(
        new StringReader(input), is);
    Block jsBlock;
    {
      JsLexer lexer = new JsLexer(cp);
      JsTokenQueue tq = new JsTokenQueue(
          lexer, is, JsTokenQueue.NO_NON_DIRECTIVE_COMMENT);
      Parser p = new Parser(tq, mq);
      jsBlock = p.parse();
      p.getTokenQueue().expectEmpty();
      jsBlock.parentify();
    }

    boolean actualSanitary =
      new ExpressionSanitizer(mq).sanitize(jsBlock);
    StringBuilder actualBuf = new StringBuilder();
    RenderContext rc = new RenderContext(mc, actualBuf);

    jsBlock.render(rc);

    String actual = actualBuf.toString();
    assertEquals(actual, golden.trim(), actual.trim());
    assertEquals(input, sanitary, actualSanitary);
  }

  private void checkFilePositionInviariants(ParseTreeNode n) {
    String msg = n + " : " + n.getFilePosition();
    try {
      // require that n start on or after its previous sibling
      ParseTreeNode prev = n.getPrevSibling();
      assertTrue(msg, null == prev
                 || (prev.getFilePosition().endCharInFile()
                     <= n.getFilePosition().startCharInFile()));

      // require that n end on or before its next sibling
      ParseTreeNode next = n.getNextSibling();
      assertTrue(msg, null == next
                 || (next.getFilePosition().startCharInFile()
                     >= n.getFilePosition().endCharInFile()));

      // require that n encompass its children
      List<? extends ParseTreeNode> children = n.children();
      if (!children.isEmpty()) {
        ParseTreeNode first = children.get(0),
                       last = children.get(children.size() - 1);
        assertTrue(msg, first.getFilePosition().startCharInFile()
                   >= n.getFilePosition().startCharInFile());
        assertTrue(msg, last.getFilePosition().endCharInFile()
                   <= n.getFilePosition().endCharInFile());
      }

      for (ParseTreeNode c : children) {
        checkFilePositionInviariants(c);
      }
    } catch (RuntimeException ex) {
      throw new RuntimeException(msg, ex);
    }
  }

  public void testInProtectedNamespace() throws Exception {
    for (String s : new String[] {
           "__proto__", "plugin_get___", "plugin_require___", "safe_ex__",
           "out___", "c1___", "plugin_checkUriRelative___", "plugin_html___",
           "plugin_prefix___", "plugin_dispatchEvent___", "plugin_log___",
           "plugin_init___",
         }) {
      assertTrue(s, ExpressionSanitizer.inProtectedNamespace(s));
    }
    for (String s : new String[] {
           "_", "x", "y", "foo", "$", "_x", "x_", "_x_",
         }) {
      assertTrue(s, !ExpressionSanitizer.inProtectedNamespace(s));
    }
  }

  public void testIsAssignedOnly1() throws Exception {
    Reference r = new Reference("prototype");
    ExpressionStmt es = new ExpressionStmt(
        new Operation(
            Operator.ASSIGN,
            new Operation(
                Operator.MEMBER_ACCESS,
                new Reference("Foo"),
                r),
            new StringLiteral("'bar'")
            )
        );
    es.parentify();
    assertTrue(ExpressionSanitizer.isAssignedOnly(r, false));
    assertTrue(ExpressionSanitizer.isAssignedOnly(r, true));
  }

  public void testIsAssignedOnly2() throws Exception {
    Reference r = new Reference("prototype");
    ExpressionStmt es = new ExpressionStmt(
        new Operation(
            Operator.ASSIGN,
            new Operation(
                Operator.MEMBER_ACCESS,
                r,
                new StringLiteral("'foo'")),
            new StringLiteral("'bar'")
            )
        );
    es.parentify();
    assertTrue(!ExpressionSanitizer.isAssignedOnly(r, false));
    assertTrue(!ExpressionSanitizer.isAssignedOnly(r, true));
  }

  public void testIsAssignedOnly3() throws Exception {
    Reference r = new Reference("prototype");
    ExpressionStmt es = new ExpressionStmt(r);
    es.parentify();
    assertTrue(!ExpressionSanitizer.isAssignedOnly(r, false));
    assertTrue(!ExpressionSanitizer.isAssignedOnly(r, true));
  }

  public void testIsAssignedOnly4() throws Exception {
    Reference r = new Reference("r");
    ExpressionStmt es = new ExpressionStmt(
        new Operation(
            Operator.ASSIGN,
            new Reference("x"),
            new Operation(
                Operator.ASSIGN,
                r,
                new NullLiteral()
            )
        )
    );
    es.parentify();
    assertTrue(!ExpressionSanitizer.isAssignedOnly(r, false));
    assertTrue(ExpressionSanitizer.isAssignedOnly(r, true));
  }
}
