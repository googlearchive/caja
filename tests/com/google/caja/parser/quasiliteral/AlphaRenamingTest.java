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

package com.google.caja.parser.quasiliteral;

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.SyntheticNodes;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.SafeIdentifierMaker;
import com.google.caja.util.Sets;

import java.util.Arrays;
import java.util.Set;

public class AlphaRenamingTest extends CajaTestCase {
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  public final void testLiteral() throws Exception {
    assertRenamed("1", "1");
    assertRenamed("1", "1", "foo", "bar");
    assertRenamed("'foo'", "'foo'", "foo", "bar");
    assertRenamed("null", "null", "foo", "bar");
    assertNoErrors();
  }

  public final void testGlobals() throws Exception {
    assertRenamed("c - a * b", "foo - bar * baz", "bar", "baz", "foo");
    assertRenamed("c - a * b", "c - a * b", "a", "b", "c");
    assertNoErrors();
  }

  public final void testFreeGlobals() throws Exception {
    assertRenamed("null", "z", "a", "b", "c");
    assertMessage(
        true, RewriterMessageType.FREE_VARIABLE, MessageLevel.ERROR,
        MessagePart.Factory.valueOf("z"));
    assertNoErrors();
  }

  public final void testPropertyNames() throws Exception {
    assertRenamed("null.bar", "foo.bar");
    assertMessage(
        true, RewriterMessageType.FREE_VARIABLE, MessageLevel.ERROR,
        MessagePart.Factory.valueOf("foo"));
    assertRenamed("a.bar", "foo.bar", "foo", "bar");
    assertRenamed("b.foo", "bar.foo", "foo", "bar");
    assertRenamed("b[a]", "bar[foo]", "foo", "bar");
    assertNoErrors();
  }

  public final void testObjects1() throws Exception {
    assertRenamed("({ x: a })", "({ x: foo })", "foo");
    assertNoErrors();
    assertRenamed("({ x: null })", "({ x: foo })");
    assertMessage(
        true, RewriterMessageType.FREE_VARIABLE, MessageLevel.ERROR,
        MessagePart.Factory.valueOf("foo"));
    assertNoErrors();
  }

  public final void testGlobalThis() throws Exception {
    assertRenamed("null.foo()", "this.foo()");
    assertMessage(
        true, RewriterMessageType.THIS_IN_GLOBAL_CONTEXT,
        MessageLevel.FATAL_ERROR);
    assertNoErrors();
  }

  public final void testGlobalArguments() throws Exception {
    assertRenamed("null.length", "arguments.length");
    assertMessage(
        true, RewriterMessageType.ARGUMENTS_IN_GLOBAL_CONTEXT,
        MessageLevel.ERROR);
    assertNoErrors();
  }

  public final void testFunctionConstructor() throws Exception {
    assertRenamed(
        ""
        // In the below, factor and unused are a and b respectively
        + "(function c(d, e) {"
        + "  var f = d + e;"
        + "  return f > 0 ? c(d - 1, e - 1) : a * f;"
        + "})",
        ""
        + "(function fn(x, y) {"
        + "  var sum = x + y;"
        + "  return sum > 0 ? fn(x - 1, y - 1) : factor * sum;"
        + "})",
        "factor", "unused");
    assertNoErrors();
  }

  public final void testFunctionDeclaration() throws Exception {
    assertRenamed(
        ""
        + "(function () {"
        + "  function b(d, e) {"
        + "    var c = b;"
        + "    return d * e - a;"
        + "  }"
        + "  return b;"
        + "})()",
        ""
        + "(function () {"
        + "  function foo(bar, baz) {"
        + "    return bar * baz - global;"
        + "  }"
        + "  return foo;"
        + "})()",
        "global");
    assertNoErrors();
  }

  public final void testRecursiveFunctionDeclaration() throws Exception {
    assertRenamed(
        ""
        + "(function () {"
        + "  function a(c) {"
        + "    var b = a;"
        + "    return c < 2 ? c : b(c - 2) + b(c - 1);"
        + "  }"
        + "})()",
        ""
        + "(function () {"
        + "  function fib(n) {"
        + "    return n < 2 ? n : fib(n - 2) + fib(n - 1);"
        + "  }"
        + "})()"
        );
    assertNoErrors();
  }

  public final void testLocalThis() throws Exception {
    assertRenamed(
        "(function () { var a = this; return a; })",
        "(function () { return this; })");
    assertNoErrors();
  }

  public final void testLocalArguments() throws Exception {
    assertRenamed(
        "(function () { var a = arguments; return a; })",
        "(function () { return arguments; })");
    assertNoErrors();
  }

  public final void testCatch() throws Exception {
    assertRenamed(
        "(function (a) { try {} catch (b) { throw b; } return a; })",
        "(function (e) { try {} catch (e) { throw e; } return e; })");
    assertMessage(
        true, MessageType.MASKING_SYMBOL, MessageLevel.ERROR,
        MessagePart.Factory.valueOf("e"));
    assertNoErrors();
  }

  public final void testMaskingFunctionDeclarations() throws Exception {
    assertRenamed(
        ""
        + "(function () {"
        + "  function b() {"
        // There cannot be a {var c = b} here since the hoisted function
        // declaration should dominate it.
        + "    function c() {"
        + "      var d = c;"
        + "      return d, a;"
        + "    }"
        + "    return c;"
        + "  }"
        + "  return b;"
        + "})",
        ""
        + "(function () {"
        + "  function f() {"
        + "    function f() {"
        + "      return f, global;"
        + "    }"
        + "    return f;"
        + "  }"
        + "  return f;"
        + "})",
        "global");
    assertMessage(
        true, MessageType.SYMBOL_REDEFINED, MessageLevel.ERROR,
        FilePosition.instance(is, 1, 44, 44, 1),
        FilePosition.instance(is, 1, 26, 26, 1),
        MessagePart.Factory.valueOf("f"));
    assertNoErrors();
  }

  public final void testMaskingFunctionConstructors() throws Exception {
    assertRenamed(
        "(function b() { return b, function c() { return c, a; }; })",
        "(function f() { return f, function f() { return f, global; }; })",
        "global");
    assertNoErrors();
  }

  public final void testMaskingGlobals() throws Exception {
    assertRenamed(
        "(function(){var b=(function(){var c=2;return c;})();return b;})()",
        "(function(){var x=(function(){var x=2;return x;})();return x;})()",
        "x");
    assertNoErrors();
  }

  public final void testMaskingLocals() throws Exception {
    assertRenamed(
        "a + (function () { var b = 2; return b; })()",
        "x + (function () { var x = 2; return x; })()",
        "x");
    assertNoErrors();
  }

  public final void testMaskingFormals() throws Exception {
    assertRenamed(
        ""
        + "a + (function (b) {"
        + "       function c(e) { var d = c; return e * e; }"
        + "       return c(b);"
        + "     })()",
        "x + (function (x) { function f(x) { return x * x; } return f(x); })()",
        "x");
    assertNoErrors();
  }

  public final void testVarArguments() throws Exception {
    assertRenamed(
        ""
        + "(function () {"
        // This matches the behavior of all major interpreters except Opera.
        + "  var a = arguments;"
        + "  var a = a;"
        + "  return a;"
        + "})",
        ""
        + "(function () {"
        + "  var arguments = arguments;"
        + "  return arguments;"
        + "})");
    assertMessage(
        true, RewriterMessageType.CANNOT_MASK_IDENTIFIER,
        MessageLevel.FATAL_ERROR, MessagePart.Factory.valueOf("arguments"));
    assertNoErrors();
  }

  public final void testVarMaskingFunctionSelfName() throws Exception {
    assertRenamed(
        "(function b() { var b = b; return b; })",
        "(function f() { var f = f; return f; })", "f");
    assertMessage(true, MessageType.SYMBOL_REDEFINED, MessageLevel.ERROR,
                  MessagePart.Factory.valueOf("f"));
    assertNoErrors();
  }

  public final void testMultiDeclaration() throws Exception {
    assertRenamed(
        "(function (a, b, c) { var c, d, e, d; return a + b + c + d + e; })",
        "(function (m, n, o) { var o, p, q, p; return m + n + o + p + q; })");
    assertNoErrors();
  }

  public final void testSynthetics() throws Exception {
    assertRenamed(
        ""
        + "(function foo___(b, y___, d) {"
        + "  var w___ = d * d;"
        + "  function inner___() {}"
        + "  return b + y___ * w___;"
        + "})",
        ""
        + "(function foo___(x, y___, z) {"
        + "  var w___ = z * z;"
        + "  function inner___() {}"
        + "  return x + y___ * w___;"
        + "})");
  }

  public final void testSanityChecks() throws Exception {
    // ___ is free but synthetic so that it cannot just be rewritten.
    assertRenamed("null", "___.foo()");
    assertMessage(
        true, RewriterMessageType.ALPHA_RENAMING_FAILURE, MessageLevel.ERROR,
        MessagePart.Factory.valueOf(
            Arrays.asList(MessagePart.Factory.valueOf("___"))));
    // If ___ is provided as a global, it is allowed.
    assertRenamed("___.foo()", "___.foo()", "___");
    assertNoErrors();
  }

  public final void testRenamingOfPseudoKeywords() throws Exception {
    assertRenamed(
        ""
        + "[function (a) { var a = arguments; return a; },"
        + " function () { var b = arguments; return b; }]",
        ""
        + "[function (arguments) { return arguments; },"
        + " function () { return arguments; }]");
    assertRenamed(
        ""
        + "[function (b) { return b; },"
        + " function () { return a; }]",
        ""
        + "[function (undefined) { return undefined; },"
        + " function () { return undefined; }]",
        "undefined");
    assertMessage(
        true, MessageType.DUPLICATE_FORMAL_PARAM, MessageLevel.ERROR,
        MessagePart.Factory.valueOf("arguments"));
    assertMessage(
        true, RewriterMessageType.CANNOT_MASK_IDENTIFIER,
        MessageLevel.FATAL_ERROR, MessagePart.Factory.valueOf("arguments"));
    assertNoErrors();
  }

  public final void testDuplicateFormals() throws Exception {
    assertRenamed(
        "function (a, b, a) { return a - b; }",
        "function (x, y, x) { return x - y; }");
    assertMessage(
        true, MessageType.DUPLICATE_FORMAL_PARAM, MessageLevel.ERROR,
        MessagePart.Factory.valueOf("x"));
    // From the sanity check.
    assertMessage(
        true, MessageType.DUPLICATE_FORMAL_PARAM, MessageLevel.ERROR,
        MessagePart.Factory.valueOf("a"));
    assertNoErrors();
  }

  private void assertRenamed(String golden, String input, String... globals)
      throws Exception {
    NameContext<String, ?> nc = new NameContext<String, Object>(
        new SafeIdentifierMaker());
    Set<String> freeSynthetics = Sets.newLinkedHashSet();
    for (String global : globals) {
      if (global.endsWith("__")) {
        freeSynthetics.add(global);
      } else {
        nc.declare(global, FilePosition.startOfFile(is));
      }
    }
    Expression renamed = AlphaRenaming.rename(
        synth(jsExpr(fromString(input))), nc, freeSynthetics, mq);
    assertEquals(render(jsExpr(fromString(golden))), render(renamed));
  }

  private static Expression synth(Expression e) {
    e.acceptPreOrder(new Visitor() {
      public boolean visit(AncestorChain<?> chain) {
        ParseTreeNode n = chain.node;
        if (n instanceof Identifier) {
          Identifier id = (Identifier) n;
          if (id.getName() != null && id.getName().endsWith("___")) {
            SyntheticNodes.s(id);
          }
        } else if (n instanceof FunctionConstructor) {
          FunctionConstructor fc = (FunctionConstructor) n;
          if (fc.getIdentifierName() != null
              && fc.getIdentifierName().endsWith("___")) {
            SyntheticNodes.s(fc);
          }
        }
        return true;
      }
    }, null);
    return e;
  }
}
