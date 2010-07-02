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

package com.google.caja.ancillary.opt;

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Statement;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Join;

import java.util.Arrays;
import java.util.List;

public class StatementSimplifierTest extends CajaTestCase {
  public final void testExtraneousBlocks() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "{",
            "  if (foo())",
            "    return;",
            "  baz();",
            "}"),
        Arrays.asList(
            "if (foo()) {",
            "  return void 0;",
            "} else {",
            "  baz();",
            "}"));
    assertNoErrors();
  }

  public final void testNestedBlocks() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "if (foo())",
            "  bar();",
            "else {",
            "  while (x) baz();",
            "  boo();",
            "}"),
        Arrays.asList(
            "if (foo()) {",
            "  { bar(); }",
            "} else {",
            "  while (x) baz();",
            "  { boo(); }",
            "}"));
    assertNoErrors();
  }

  public final void testNoops() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "if (foo());",
            "else",
            "  boo();"),
        Arrays.asList(
            "if (foo()) {",
            "  { ; }",
            "} else {",
            "  ;",
            "  { boo(); }",
            "}"));
    assertNoErrors();
  }

  public final void testRequiredBlocks() throws ParseException {
    assertSimplified(Arrays.asList("function f() { return 4; }"),
                     Arrays.asList("function f() { return 4; }"));
    assertSimplified(Arrays.asList("try{f();}catch(e){g();}finally{h();}"),
                     Arrays.asList("try{f();}catch(e){g();}finally{h();}"));
    assertNoErrors();
  }

  public final void testExprStmtsSimplified() throws ParseException {
    assertSimplified(
        Arrays.asList("while (cond) foo(), f(), bar();"),
        Arrays.asList("while (cond) { foo(); true; void f(); bar(); }"));
    assertNoErrors();
  }

  public final void testCondOptimization1() throws ParseException {
    assertSimplified(
        Arrays.asList("c && (foo(), bar(), baz())"),
        Arrays.asList(
            "if (c) {",
            "  foo();",
            "  bar();",
            "  baz();",
            "}"));
    assertNoErrors();
  }

  public final void testCondOptimization2() throws ParseException {
    assertSimplified(
        Arrays.asList("c || (foo(), bar(), baz())"),
        Arrays.asList(
            "if (!c) {",
            "  foo();",
            "  bar(),",
            "  baz();",
            "}"));
    assertNoErrors();
  }

  public final void testCondOptimization3a() throws ParseException {
    assertSimplified(
        Arrays.asList("return c ? boo() : (foo(), bar()), baz()"),
        Arrays.asList(
            "if (!c) {",
            "  foo();",
            "  bar();",
            "  return baz();",
            "} else {",
            "  boo();",
            "  return baz();",
            "}"));
    assertNoErrors();
  }

  public final void testCondOptimization3b() throws ParseException {
    assertSimplified(
        Arrays.asList("return c ? void 0 : (foo(), baz())"),
        Arrays.asList(
            "if (!c) {",
            "  foo();",
            "  return baz();",
            "} else {",
            "  return;",
            "}"));
    assertNoErrors();
  }

  public final void testCondOptimization3c() throws ParseException {
    assertSimplified(
        Arrays.asList("return c ? boo() : (foo(), bar()), baz()"),
        Arrays.asList(
            "if (!c) {",
            "  return foo(),",
            "      bar(),",
            "      baz();",
            "} else {",
            "  boo();",
            "  return baz();",
            "}"));
    assertNoErrors();
  }

  public final void testCondOptimization3d() throws ParseException {
    assertSimplified(
        Arrays.asList("throw c ? boo() : (foo(), bar()), new Error(baz())"),
        Arrays.asList(
            "if (!c) {",
            "  foo();",
            "  bar();",
            "  throw new Error(baz());",
            "} else {",
            "  boo();",
            "  throw new Error(baz());",
            "}"));
    assertNoErrors();
  }

  public final void testCondOptimization4a() throws ParseException {
    assertSimplified(
        Arrays.asList("if (!c) return foo(), bar(), baz()"),
        Arrays.asList(
            "if (!c) {",
            "  foo();",
            "  bar();",
            "  return baz();",
            "}"));
    assertNoErrors();
  }

  public final void testCondOptimization4b() throws ParseException {
    assertSimplified(
        Arrays.asList("if (!c) throw foo(), bar(), baz()"),
        Arrays.asList(
            "if (!c) {",
            "  foo();",
            "  bar();",
            "  throw baz();",
            "}"));
    assertNoErrors();
  }

  public final void testCondOptimization5() throws ParseException {
    assertSimplified(
        Arrays.asList("return c || null"),
        Arrays.asList(
            "if (c) { return c; }",
            "return null;"));
    assertNoErrors();
  }

  public final void testCondOptimization6() throws ParseException {
    assertSimplified(
        Arrays.asList("return c || void 0"),
        Arrays.asList(
            "if (c) { return c; }",
            "return;"));
    assertNoErrors();
  }

  public final void testCondOptimization7() throws ParseException {
    assertSimplified(
        Arrays.asList("return c && void 0"),
        Arrays.asList(
            "if (c) { return; }",
            "return c;"));
    assertNoErrors();
  }

  public final void testCondOptimization8a() throws ParseException {
    assertSimplified(
        Arrays.asList("return !c"),
        Arrays.asList(
            "if (!c) {",
            "  return true;",
            "}",
            "return false;"));
    assertNoErrors();
  }

  public final void testCondOptimization8b() throws ParseException {
    assertSimplified(
        Arrays.asList("return !!c"),
        Arrays.asList(
            "if (c) {",
            "  return true;",
            "}",
            "return false;"));
    assertNoErrors();
  }

  public final void testCondOptimization8c() throws ParseException {
    assertSimplified(
        Arrays.asList("return c === b"),
        Arrays.asList(
            "if (c === b) return true;",
            "else return false;"));
    assertNoErrors();
  }

  public final void testCondOptimization8d() throws ParseException {
    assertSimplified(
        Arrays.asList("return c !== b"),
        Arrays.asList(
            "if (c === b) return false;",
            "return true;"));
    assertNoErrors();
  }

  public final void testNotFoldableToTernary() throws ParseException {
    assertSimplified(
        Arrays.asList("if (!c) foo(); else return bar();"),
        Arrays.asList(
            "if (!c) {",
            "  foo();",
            "} else {",
            "  return bar();",
            "}"));
    assertNoErrors();
  }

  public final void testCondOptimizationComposes1() throws ParseException {
    assertSimplified(
        Arrays.asList("return c ? d ? (e && baz(),boo) : far : (foo(), bar)"),
        Arrays.asList(
            "if (!c) {",
            "  foo();",
            "  return bar;",
            "} else if (d) {",
            "  if (e) {",
            "    baz();",
            "    return boo;",
            "  } else {",
            "    return boo;",
            "  }",
            "} else {",
            "  return far;",
            "}"));
    assertNoErrors();
  }

  public final void testCondOptimizationComposes2() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "{",
            "var x;",
            "return x = (c ? d ? e ? baz : boo : far : foo()), x;",
            "}"
            ),
        Arrays.asList(
            "var x;",
            "if (!c) {",
            "  x = foo();",
            "  return x;",
            "} else if (d) {",
            "  if (e) {",
            "    x = baz;",
            "  } else {",
            "    x = boo;",
            "  }",
            "  return x;",
            "} else {",
            "  x = far;",
            "  return x;",
            "}"));
    assertNoErrors();
  }

  public final void testMultiCondOpts() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "return a(),foo=='bar'?bar():foo=='bar2'?bar2():(baz(),boo(),far)"),
        Arrays.asList(
            "a();",
            "if (foo == 'bar') { return bar(); }",
            "else if (foo == 'bar2') { return bar2(); }",
            "baz();",
            "boo();",
            "return far;"));
    assertNoErrors();
  }

  public final void testHandingConds() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "if (foo) { for (var k in o) if (f(o[k])) break; } else bar();"),
        Arrays.asList(
            "if (foo) for (var k in o) { if (f(o[k])) break; } else bar();"));
    assertNoErrors();
  }

  public final void testUnnecessaryElses() throws ParseException {
    assertSimplified(
        Arrays.asList("if (foo()) bar();"),
        Arrays.asList("if (foo()) { bar(); } else { /* do nothing */ }"));
  }

  public final void testLabelRenaming() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "{",
            "  var foo = 1;",
            "  a: for (var k in o)",
            "    for (var j in o[k])",
            "      if (o[k][j]) break a;",
            "}"),
        Arrays.asList(
            "var foo = 1;",
            "foo: for (var k in o) {",
            "  bar: for (var j in o[k]) {",
            "    if (o[k][j]) { break foo; }",
            "  }",
            "}"));
    assertNoErrors();
  }

  public final void testUnknownLabel() throws ParseException {
    assertSimplified(Arrays.asList("break;"), Arrays.asList("break foo;"));
    assertMessage(
        true, MessageType.UNDEFINED_SYMBOL, MessageLevel.ERROR,
        MessagePart.Factory.valueOf("foo"));
    assertNoErrors();
  }

  public final void testUselessLabel1() throws ParseException {
    assertSimplified(
        Arrays.asList("foo()"),
        Arrays.asList("{ foo: { foo(); } }"));
    assertNoErrors();
  }

  public final void testUselessLabel2() throws ParseException {
    assertSimplified(
        // Brackets needed to disambiguate the fact that the a is not attached
        // to the while.
        Arrays.asList("a: { while (1) break a; }"),
        Arrays.asList("{ foo: { while (1) break foo; } }"));
    assertNoErrors();
  }

  public final void testMaskLabel() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "a: for (var k in o)",
            "  switch (k) {",
            "    case x:",
            "      if (f(k)) break a;",
            "      break;",
            "    case y:",
            "      b: for (var j in p)",
            "        switch (j) {",
            "          case z:",
            "            if (f(j)) break b;",
            "            break;",
            "          default: panic();",
            "        }",
            "      break;",
            "    case w:",
            "      if (g(k)) break a;",
            "      break;",
            "    default: panic();",
            "  }"),
        Arrays.asList(
            "foo: for (var k in o) {",
            "  switch (k) {",
            "    case x:",
            "      if (f(k)) { break foo; }",
            "      break;",
            "    case y:",
            "      foo: for (var j in p) {",
            "        switch (j) {",
            "          case z:",
            "            if (f(j)) { break foo; }",
            "            break;",
            "          default: panic();",
            "        }",
            "      }",
            "      break;",
            "    case w:",
            "      if (g(k)) break foo;",
            "      break;",
            "    default: panic();",
            "  }",
            "}"));
    assertNoErrors();
  }

  public final void testDeadCode() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "function f() { return foo(), bar(), baz(); }"),
        Arrays.asList(
            "function f() {",
            "  foo();",
            "  bar();",
            "  return baz();",
            "  boo();",
            "}"));
    assertNoErrors();
  }

  public final void testSwitchBlocks1() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "switch (x) {",
            "  case 1:",
            "    return foo();",
            "  case 2:",
            "  case 3:",
            "    bar();",
            "    break;",
            "  case 4:",
            "    baz();",
            "}"),
        Arrays.asList(
            "switch (x) {",
            "  case 1:",
            "    return foo();",
            "    break;",
            "  case 2:",
            "    bar();",
            "    break;",
            "  case 3:",
            "    bar();",
            "    break;",
            "  case 4:",
            "    baz();",
            "    break;",
            "  default:",
            "    break;",
            "}"));
    assertNoErrors();
  }

  public final void testSwitchBlocks3a() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "switch (x) {",
            "  case 1: return foo();",
            "  case 2: return bar();",
            "}"),
        Arrays.asList(
            "foo: switch (x) {",
            "  case 1: return foo();",
            "  case 2: return bar();",
            "  case 3:",
            "  case 4:",
            "  default:",
            "    break;",
            "}"));
    assertNoErrors();
  }

  public final void testSwitchBlocks3b() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "switch (x) {",
            "  case 1: return foo();",
            "  case 2: return bar();",
            "}"),
        Arrays.asList(
            "foo: switch (x) {",
            "  case 1: return foo();",
            "  case 2: return bar();",
            "  default:",
            "  case 3:",
            "  case 4:",
            "}"));
    assertNoErrors();
  }

  public final void testSwitchBlocks4() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "switch (x) {",
            "  case 1: return foo();",
            "  case 2: return bar();",
            "}"),
        Arrays.asList(
            "foo: switch (x) {",
            "  case 1: return foo();",
            "  case 2: return bar();",
            "  case 3:",
            "  case 4:",
            "  default:",
            "    break;",
            "}"));
    assertNoErrors();
  }

  public final void testSwitchBlocks5() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "switch (x) {",
            "  case 2: return foo();",
            "  case x():",
            "}"),
        Arrays.asList(
            "d: switch (x) {",
            "  case 1: break;",
            "  case 2: return foo();",
            "  case x():",
            "}"));
    assertNoErrors();
  }

  public final void testSwitchBlocks6() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "switch (x) {",
            "  case 1: break;",
            "  case 2: return bar();",
            "  default: baz();",
            "}"),
        Arrays.asList(
            "bar: switch (x) {",
            "  case 1: break bar;",
            "  case 2: return bar();",
            "  default: baz();",
            "}"));
    assertNoErrors();
  }

  public final void testSwitchBlocks7() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "a: switch (x) {",
            "  case 2:",
            "    for (var i = 0; i < n; ++i)",
            "      if (arr[i]) break a; else if (arr[i] == null) break;",
            "    break;",
            "  case y():",
            "}"),
        Arrays.asList(
            "foo: switch (x) {",
            "  case 1: break foo;",
            "  case 2:",
            "    for (var i = 0; i < n; ++i) {",
            "      if (arr[i]) break foo; else if (arr[i] == null) break;",
            "    }",
            "    break;",
            "  case y():",
            "  case 4:",
            "  default:",
            "    break;",
            "}"));
    assertNoErrors();
  }

  public final void testSwitchBlocks8() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "switch (x) {",
            "  case 4:",
            "  case 5:",
            "    return baz();",
            "}"),
        Arrays.asList(
            "foo: switch (x) {",
            "  case 1:",
            "  case 2:",
            "  case 3: break;",
            "  case 4:",
            "  case 5:",
            "    return baz();;",
            "  default:",
            "    break;",
            "}"));
    assertNoErrors();
  }

  public final void testSwitchBlocks9() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "switch (x) {",
            "  case 1:",
            "    foo();",
            "    break;",
            "  case 4: baz();",
            "}"),
        Arrays.asList(
            "foo: switch (x) {",
            "  case 1:",
            "    foo();",
            "  case 3: break;",
            "  case 4: baz();",
            "}"));
    assertNoErrors();
  }

  public final void testReturnValueVisited() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "function f() {",
            "  var i = 0;",
            "  return function () {",
            "     return ++i & 1 ? i : -i;",
            "  };",
            "}"),
        Arrays.asList(
            "function f() {",
            "  var i = 0;",
            "  return function () {",
            "    if (++i & 1) { return i; } else { return -i; }",
            "  };",
            "}"));
    assertNoErrors();
  }

  public final void testBlockTruncating1() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "function f(x) {",
            "  return f;",
            // Preserve hoisted decls.
            "  function f() { return x * x; }",
            "}"),
        Arrays.asList(
            "function f(x) {",
            "  return f;",
            "  function f() { return x * x; }",
            "}"));
    assertNoErrors();
  }

  public final void testBlockTruncating2() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "function f(x) {",
            "  return f;",
            // Preserve hoisted decls.
            "  function f() { return x * x; }",
            "}"),
        Arrays.asList(
            "function f(x) {",
            "  return f;",
            "  f();",
            "  function f() { return x * x; }",
            "  f();",
            "}"));
    assertNoErrors();
  }

  public final void testBlockTruncating3() throws ParseException {
    assertSimplified(
        Arrays.asList(
            "function f(x) {",
            "  return i++, f;",
            // Preserve hoisted decls.
            "  function f() { return x * x; }",
            // But do not hoist initializers.
            "  var i;",
            "  var msg;",
            "}"),
        Arrays.asList(
            "function f(x) {",
            "  i++;",
            "  return f;",
            "  if (foo) {",
            "    function f() { return x * x; }",
            "  } else {",
            "    try {",
            "      var i = 1;",
            "    } catch (ex) {",
            "      var msg = ex.message;",
            "    }",
            "  }",
            "}"));
    assertNoErrors();
  }

  public final void testExpressionStmtsOptimized() throws ParseException {
    assertSimplified(
        Arrays.asList(
            // i++ switched to more efficient form via simplifyForSideEffect,
            // but j-- not.
            "for (var i = 0; i < 10; ++i) arr[i] = inp[j--];"),
        Arrays.asList(
            "for (var i = 0; i < 10; i++) { arr[i] = inp[j--]; }"));
    assertNoErrors();
  }

  public final void testOptimizeExpressionFlow() throws ParseException {
    assertEquals("4", optFlow("4"));
    assertEquals("a || b", optFlow("a ? a : b"));
    assertEquals("a && b", optFlow("a ? b : a"));
    assertEquals(norm("++a ? ++a : b"), optFlow("++a ? ++a : b"));
    assertEquals(norm("++a ? b : ++a"), optFlow("++a ? b : ++a"));
    assertEquals(norm("a ? b : c, d"), optFlow("a ? (b,d) : (c,d)"));
    assertEquals(norm("a && b, d"), optFlow("a ? (b,d) : d"));
    assertEquals(norm("a || c, d"), optFlow("a ? d : (c,d)"));
    assertEquals(norm("a || c, d"), optFlow("!!a ? d : (c,d)"));
    assertEquals(norm("a && c, d"), optFlow("!a ? d : (c,d)"));
    assertEquals(norm("a ? b : c, d, e"), optFlow("a ? (b,d,e) : (c,d,e)"));
    assertEquals(norm("a && b, d, e, f, g"),
                 optFlow("a ? (b, d, e, f, g) : (d, e, f, g)"));
    assertEquals(norm("a ? x : (b, c, d), p, q, r"),
                 optFlow("!a ? (b, c, d, p, q, r) : (x, p, q, r)"));
    assertEquals(norm("foo"), optFlow("true ? foo : foo"));
    assertEquals(norm("c(),foo"), optFlow("c() ? foo : foo"));
    assertEquals(norm("c(),d() ? foo : bar"),
                 optFlow("c() ? d() ? foo : bar : !d() ? bar : foo"));
    assertEquals(norm("x + (c ? y : z)"), optFlow("c ? x + y : x + z"));
    assertEquals(norm("(c ? x : y) + z"), optFlow("c ? x + z : y + z"));
    assertEquals(norm("x = c ? y : z"), optFlow("c ? x = y : x = z"));
    assertEquals(norm("c ? x = z : y = z"), optFlow("c ? x = z : y = z"));
    assertNoErrors();
  }

  public final void testExits() throws ParseException {
    assertTrue(StatementSimplifier.exits(js(fromString("return;"))));
    assertTrue(StatementSimplifier.exits(js(fromString("return 1;"))));
    assertTrue(StatementSimplifier.exits(js(fromString("break foo;"))));
    assertTrue(StatementSimplifier.exits(js(fromString("break;"))));
    assertTrue(StatementSimplifier.exits(js(fromString("continue foo;"))));
    assertTrue(StatementSimplifier.exits(js(fromString("continue;"))));
    assertTrue(StatementSimplifier.exits(js(fromString("throw new Error();"))));
    assertTrue(StatementSimplifier.exits(js(fromString("throw null;"))));
    assertFalse(StatementSimplifier.exits(js(fromString("1;"))));
    assertFalse(StatementSimplifier.exits(js(fromString("f();"))));
    assertTrue(StatementSimplifier.exits(js(fromString("{ foo(); return; }"))));
    assertFalse(StatementSimplifier.exits(js(fromString("{ foo(); bar(); }"))));
    assertFalse(StatementSimplifier.exits(js(fromString("if (x) return;"))));
    assertTrue(StatementSimplifier.exits(js(fromString(
        "if (x) return y; else return z;"))));
    assertFalse(StatementSimplifier.exits(js(fromString(
        "if (x) y(); else return z;"))));
    assertFalse(StatementSimplifier.exits(js(fromString(
        "if (x) return y; else z();"))));
    assertNoErrors();
  }

  private String optFlow(String code) throws ParseException {
    return render(
        StatementSimplifier.optimizeExpressionFlow(jsExpr(fromString(code))));
  }

  private String norm(String code) throws ParseException {
    return render(jsExpr(fromString(code)));
  }

  private void assertSimplified(List<String> expected, List<String> input)
      throws ParseException {
    Block expectedStmt = js(fromString(Join.join("\n", expected)));
    Statement inputStmt = js(fromString(Join.join("\n", input)));
    ParseTreeNode optimized = StatementSimplifier.optimize(inputStmt, mq);
    assertEquals(renderBody(expectedStmt), renderStmt((Statement) optimized));
  }

  private static String renderBody(Block bl) {
    StringBuilder sb = new StringBuilder();
    RenderContext rc = new RenderContext(bl.makeRenderer(sb, null));
    bl.renderBody(rc);
    rc.getOut().noMoreTokens();
    return sb.toString();
  }

  private static String renderStmt(Statement s) {
    StringBuilder sb = new StringBuilder();
    RenderContext rc = new RenderContext(s.makeRenderer(sb, null));
    s.renderBlock(rc, true);
    rc.getOut().noMoreTokens();
    return sb.toString();
  }
}
