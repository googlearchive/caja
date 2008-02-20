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
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Parser;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.TestUtil;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;

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
    // TODO(mikesamuel): fix catch block when exceptions sanitized
    String golden = TestUtil.readResource(getClass(), "sanitizergolden1.js");

    MessageContext mc = new MessageContext();
    MessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(new OutputStreamWriter(System.err)), mc);
    PluginMeta meta = new PluginMeta("pre");
    ParseTreeNode pt = TestUtil.parseTree(getClass(), "sanitizerinput1.js", mq);
    // TODO: test output value
    new ExpressionSanitizerCaja(mq, meta).sanitize(ac(pt));
    StringBuilder out = new StringBuilder();
    RenderContext rc = new RenderContext(mc, out);

    pt.render(rc);
    assertEquals(golden.trim(), out.toString().trim());
    // TODO(mikesamuel): Test that we get meaningful file positions out of the
    // sanitizer.
    TestUtil.checkFilePositionInvariants(pt);
  }

  public void testArithmetic() throws Exception {
    runTest("1 + 2 * 3 / 4 - -5", "{\n  1 + 2 * 3 / 4 - -5;\n}", true);
  }

  public void testFunctionCalls() throws Exception {
    runTest("f()", "{\n  ___.asSimpleFunc(___OUTERS___.f)();\n}", true);
  }

  public void testSimpleFunc() throws Exception {
    runTest("function f() {}",
            "{\n"
            + "  ___OUTERS___.f = ___.simpleFunc(function f() {\n"
            + "    });\n"
            + "}",
            true);
  }
  
  public void testSimpleConstructor() throws Exception {
    // A function that references this is a constructor.
    runTest("function f() { return this; }",
            "{\n"
            + "  ___OUTERS___.f = ___.ctor(function f() {\n"
            + "      var t___ = this;\n"
            + "      return t___;\n"
            + "    });\n"
            + "}",
            true);
  }
  
  public void testClosuresInSimpleFunctions() throws Exception {
    // A function that doesn't reference this but contains a function that does,
    // is not a constructor.
    runTest("function f() { return function () { return this; }; }",
            "{\n"
            + "  ___OUTERS___.f = ___.simpleFunc(function f() {\n"
            + "      return ___.ctor(function () {\n"
            + "          var t___ = this;\n"
            + "          return t___;\n"
            + "        });\n"
            + "    });\n"
            + "}",
            true);
    // TODO(mikesamuel): crashes cajoler.
  }
  
  public void testConstructorThatReturnsClosure() throws Exception {
    // A function that reference this but returns a function that does not,
    // is a constructor.
    runTest("function f() { "
            + "var self = this;"
            + "return function () { return self; };"
            + "}",
            "{\n"
            + "  ___OUTERS___.f = ___.ctor(function f() {\n"
            + "      var t___ = this;\n"
            + "      var self = t___;\n"
            + "      return ___.primFreeze(___.simpleFunc(function () {\n"
            + "            return self;\n"
            + "          }));\n"
            + "    });\n"
            + "}",
            true);
  }


  public void testBadDeclarations1() throws Exception {
    // TODO(mikesamuel): Make sure this doesn't pass.
    runTest("function plugin_get___() { ; }",
            "{\n"
            + "  ___OUTERS___.plugin_get___"
            + " = ___.simpleFunc(function plugin_get___() {\n"
            + "      ;\n"
            + "    });\n"
            + "}",
            false);
  }

  public void testBadDeclarations2() throws Exception {
    // TODO(mikesamuel): Make sure this doesn't pass.
    runTest("var out___ = [ '<script></script>' ];",
            "{\n  ___OUTERS___.out___ = ['<script></script>'];\n}", false);
  }

  public void testBadReference1() throws Exception {
    // TODO(mikesamuel): Make sure this doesn't pass.
    runTest("var a = out___;", "{\n  ___OUTERS___.a = out___;\n}", false);
  }

  public void testBadReference2() throws Exception {
    runTest("f(MyClass.prototype);",
            "{\n  ___.asSimpleFunc(___OUTERS___.f)((function () {\n"
            + "        var x___ = ___OUTERS___.MyClass;\n"
            + "        return x___.prototype_canRead___ ? x___.prototype"
            + " : ___.readPub(x___, 'prototype');\n"
            + "      })());\n"
            + "}",
            // Allowed, but should be denied by ___.readPub
            true);
  }

  public void testAssignmentToPrototypeOk1() throws Exception {
    runTest("MyClass.prototype = new MyOtherClass;",
            "{\n  (function () {\n"
            + "      var x___ = ___OUTERS___.MyClass;\n"
            + "      var x0___ = new ___OUTERS___.MyOtherClass;\n"
            + "      x___.prototype_canSet___ ? (x___.prototype = x0___)"
            + " : ___.setPub(x___, 'prototype', x0___);\n"
            + "    })();\n"
            + "}",
            // Allowed.
            true);
  }

  public void testAssignmentToPrototypeChains() throws Exception {
    // TODO(mikesamuel): need return in inner closure.
    runTest("MyClass.prototype = MyOtherClass.prototype = new YetAnotherClass;",
            "{\n"
            + "  (function () {\n"
            + "      var x___ = ___OUTERS___.MyClass;\n"
            + "      var x0___ = (function () {\n"
            + "          var x___ = ___OUTERS___.MyOtherClass;\n"
            + "          var x0___ = new ___OUTERS___.YetAnotherClass;\n"
            + "          return x___.prototype_canSet___"
            + " ? (x___.prototype = x0___)"
            + " : ___.setPub(x___, 'prototype', x0___);\n"
            + "        })();\n"
            + "      x___.prototype_canSet___"
            + " ? (x___.prototype = x0___)"
            + " : ___.setPub(x___, 'prototype', x0___);\n"
            + "    })();\n"
            + "}",
            // Allowed.
            true);
  }

  public void testAssignmentToPrototypeOk2() throws Exception {
    runTest("MyClass.prototype.method = f;",
            "{\n" +
            "  (function () {\n"
            + "      var x___ = (function () {\n"
            + "          var x___ = ___OUTERS___.MyClass;\n"
            + "          return x___.prototype_canRead___"
            + " ? x___.prototype : ___.readPub(x___, 'prototype');\n"
            + "        })();\n"
            + "      var x0___ = ___OUTERS___.f;\n"
            + "      x___.method_canSet___"
            + " ? (x___.method = x0___) : ___.setPub(x___, 'method', x0___);\n"
            + "    })();\n"
            + "}",
            true);
  }

  public void testAssignmentToPrototypeNoCheating() throws Exception {
    runTest("x = MyClass.prototype = new MyOtherClass;",
            "{\n"
            + "  ___OUTERS___.x = (function () {\n"
            + "      var x___ = ___OUTERS___.MyClass;\n"
            + "      var x0___ = new ___OUTERS___.MyOtherClass;\n"
            // No return allowed here.
            + "      x___.prototype_canSet___"
            + " ? (x___.prototype = x0___)"
            + " : ___.setPub(x___, 'prototype', x0___);\n"
            + "    })();\n"
            + "}",
            // Ok as long as the closure does not return a value.
            true);
  }

  private void runTest(String input, String golden, boolean sanitary)
      throws Exception {
    MessageContext mc = new MessageContext();
    MessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(new OutputStreamWriter(System.err)), mc);
    PluginMeta meta = new PluginMeta("pre");

    InputSource is = new InputSource(new URI("test:///" + getName()));
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
    }

    boolean actualSanitary = new ExpressionSanitizerCaja(mq, meta)
        .sanitize(ac(jsBlock));
    if (actualSanitary) {
      for (Message msg : mq.getMessages()) {
        if (MessageLevel.ERROR.compareTo(msg.getMessageLevel()) <= 0) {
          if (sanitary) {
            fail(msg.toString());
          }
          break;
        }
      }
    }
    StringBuilder actualBuf = new StringBuilder();
    RenderContext rc = new RenderContext(mc, actualBuf);

    jsBlock.render(rc);

    String actual = actualBuf.toString();
    // TODO(mikesamuel): replace with a reparse and structural comparison.
    assertEquals(actual, golden.trim(), actual.trim());
    assertEquals(input, sanitary, actualSanitary);
  }

  public void testInProtectedNamespace() throws Exception {
    for (String s : new String[] {
           "__proto__", "plugin_get___", "plugin_require___", "safe_ex__",
           "out___", "c1___", "plugin_checkUriRelative___", "plugin_html___",
           "plugin_prefix___", "plugin_dispatchEvent___", "plugin_log___",
           "plugin_init___", "__", "___",
         }) {
      // TODO(mikesamuel): why are none of these blocked?
      runTest(s, "{\n  " + s + ";\n}", false);
    }
  }

  public void testNotInProtectedNamespace() throws Exception {
    // TODO(mikesamuel): make sure _ is not in the protected namespace.
    for (String s : new String[] {
           "caja", "_", "x", "y", "foo", "$", "_x", "x_", "_x_",
         }) {
      runTest(s, "{\n  ___OUTERS___." + s + ";\n}", true);
    }
  }

  public void testAssignment1() throws Exception {
    runTest(
        "Foo.prototype = 'bar'",
        "{\n"
        + "  (function () {\n"
        + "      var x___ = ___OUTERS___.Foo;\n"
        + "      var x0___ = 'bar';\n"
        + "      x___.prototype_canSet___"
        + " ? (x___.prototype = x0___)"
        + " : ___.setPub(x___, 'prototype', x0___);\n"
        + "    })();\n"
        + "}",
        true);
  }

  public void testAssignment2() throws Exception {
    runTest(
        "prototype['foo'] = 'bar'",
        "{\n"
        + "  ___.setPub(___OUTERS___.prototype, 'foo', 'bar');\n"
        + "}",
        true);
  }

  public void testIsGlobalPrototypeAvailable() throws Exception {
    runTest(
        "prototype",
        "{\n"
        + "  ___OUTERS___.prototype;\n"
        + "}",
        // TODO(mikesamuel): should this be available?
        false);
  }

  public void testIsGlobalConstructorAvailable() throws Exception {
    runTest(
        "constructor",
        "{\n"
        + "  ___OUTERS___.constructor;\n"
        + "}",
        // TODO(mikesamuel): should this be available?
        false);
  }

  public void testAssignmentOnly3() throws Exception {
    runTest(
        "x = r = null",
        "{\n"
        + "  ___OUTERS___.x = ___OUTERS___.r = null;\n"
        + "}",
        true);
  }

  private static <T extends ParseTreeNode> AncestorChain<T> ac(T node) {
    return new AncestorChain<T>(node);
  }
}
