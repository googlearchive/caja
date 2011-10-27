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
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.Literal;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Join;
import com.google.caja.util.Lists;
import com.google.caja.util.MoreAsserts;
import com.google.caja.util.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ParseTreeKBTest extends CajaTestCase {
  List<Pair<Expression, Fact>> knowledge;
  ParseTreeKB kb;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    knowledge = Lists.newArrayList();
    kb = new ParseTreeKB() {
      @Override
      protected void putFact(Expression e, String digest, Fact f) {
        knowledge.add(Pair.pair(e, f));
        super.putFact(e, digest, f);
      }
    };
    kb.finishInference();
    knowledge.clear();
  }

  public final void testTypeof1() throws Exception {
    addFact("typeof undefined", " 'undefined' ");
    assertFacts("(typeof undefined) IS 'undefined'");
    // Test base facts
    assertEquals(
        "void 0", render(kb.getFact(jsExpr(fromString("undefined"))).value));
  }

  public final void testTypeof2() throws Exception {
    addFact("typeof window.JSON", " 'object' ");
    assertFacts(
        "(typeof window.JSON) IS 'object'",
        "(window.JSON !== void 0) IS true",
        "(window.JSON === void 0) IS false",
        "(void 0 !== window.JSON) IS true",
        "(void 0 === window.JSON) IS false");
  }

  public final void testTypeof3() throws Exception {
    addFact("typeof window.JSON", " 'function' ");
    assertFacts(
        "(typeof window.JSON) IS 'function'",
        "(window) LIKE true",
        "(window.JSON) LIKE true",
        "(window.JSON !== void 0) IS true",
        "(window.JSON === void 0) IS false",
        "(void 0 !== window.JSON) IS true",
        "(void 0 === window.JSON) IS false");
  }

  public final void testTypeof4() throws Exception {
    addFact("typeof window.JSON === 'object'", "true");
    assertFacts(
        "(typeof window.JSON === 'object') IS true",
        "(typeof window.JSON == 'object') IS true",
        "('object' === typeof window.JSON) IS true",
        "('object' == typeof window.JSON) IS true",
        "(typeof window.JSON !== 'object') IS false",
        "(typeof window.JSON != 'object') IS false",
        "('object' !== typeof window.JSON) IS false",
        "('object' != typeof window.JSON) IS false",
        "(typeof window.JSON) IS 'object'",
        "(window.JSON !== void 0) IS true",
        "(window.JSON === void 0) IS false",
        "(void 0 !== window.JSON) IS true",
        "(void 0 === window.JSON) IS false");
  }

  public final void testTypeof5() throws Exception {
    addFact("typeof addEventListener", "'undefined'");
    addFact("!!this.window && window === this", "true");
    kb.finishInference();
    assertFacts(
        "(window) LIKE this",
        "(! !this.window && window === this) IS true",
        "(! !this.window) IS true",
        "(!this.window) IS false",
        "(this.window) LIKE true",
        "(typeof addEventListener) IS 'undefined'",
        "(typeof this.addEventListener) IS 'undefined'",
        "(typeof window.addEventListener) IS 'undefined'",
        "(addEventListener) IS void 0",
        "(this.addEventListener) IS void 0",
        "(window.addEventListener) IS void 0",
        // The usual window alias guff
        "(this != window) IS false",
        "(this !== window) IS false",
        "(this == window) IS true",
        "(this === window) IS true",
        "(window != this) IS false",
        "(window !== this) IS false",
        "(window == this) IS true",
        "(window === this) IS true",
        "(window.undefined) IS void 0");
  }

  public final void testNot1() throws Exception {
    addFact("!w", "true");
    addFact("!x", "false");
    addFuzzyFact("y", false);
    addFuzzyFact("z", true);
    assertFacts(
        "(!w) IS true",
        "(!x) IS false",
        "(w) LIKE false",
        "(x) LIKE true",
        "(y) LIKE false",
        "(z) LIKE true");
  }

  public final void testNot2() throws Exception {
    addFuzzyFact("!(x || y)", true);
    assertFacts(
        "(! (x || y)) IS true",
        "(x || y) LIKE false",
        "(x) LIKE false",
        "(y) LIKE false");
  }

  public final void testComparisons1() throws Exception {
    addFact("version < 5", "true");
    assertFacts(
        "(version < 5) IS true",
        "(5 > version) IS true",
        "(version <= 5) IS true",
        "(5 >= version) IS true",
        "(version !== 5) IS true",
        "(version === 5) IS false",
        "(5 !== version) IS true",
        "(5 === version) IS false");
  }

  public final void testComparisons2() throws Exception {
    addFact("v >= 0", "false");
    assertFacts(
        "(v >= 0) IS false",
        "(0 <= v) IS false",
        "(v !== 0) IS true",
        "(0 !== v) IS true",
        "(v === 0) IS false",
        "(0 === v) IS false");
  }

  public final void testComparisons3() throws Exception {
    addFact("version > ''", "false");
    assertFacts(
        "(version > '') IS false",
        "('' < version) IS false");
  }

  public final void testComparisons4() throws Exception {
    addFact("v >= '7'", "true");
    assertFacts(
        "(v >= '7') IS true",
        "('7' <= v) IS true");
  }

  public final void testComparisons5() throws Exception {
    addFact("len > 5", "true");
    assertFacts(
        "(len > 5) IS true",
        "(5 < len) IS true",
        "(len >= 5) IS true",
        "(5 <= len) IS true",
        "(len !== 5) IS true",
        "(len === 5) IS false",
        "(5 !== len) IS true",
        "(5 === len) IS false");
  }

  public final void testInstanceof() throws Exception {
    addFact("a instanceof Foo", "true");
    addFact("b instanceof Bar", "false");
    assertFacts(
        "(a instanceof Foo) IS true",
        "(b instanceof Bar) IS false",
        "(a) LIKE true",
        "(typeof Foo) IS 'function'",
        "(typeof Bar) IS 'function'",
        "(Foo) LIKE true",
        "(Bar) LIKE true",
        "(Foo !== void 0) IS true",
        "(Foo === void 0) IS false",
        "(void 0 !== Foo) IS true",
        "(void 0 === Foo) IS false",
        "(Bar !== void 0) IS true",
        "(Bar === void 0) IS false",
        "(void 0 !== Bar) IS true",
        "(void 0 === Bar) IS false");
  }

  public final void testEquivalence() throws Exception {
    addFact("a == null", "true");
    addFact("b == 3", "false");
    assertFacts(
        "(a == null) IS true",
        "(null == a) IS true",
        "(a != null) IS false",
        "(null != a) IS false",
        "(b == 3) IS false",
        "(3 == b) IS false",
        "(b != 3) IS true",
        "(3 != b) IS true");
  }

  public final void testEquality() throws Exception {
    addFact("a === null", "true");
    addFact("b === 3", "false");
    assertFacts(
        "(a === null) IS true",
        "(null === a) IS true",
        "(a == null) IS true",
        "(null == a) IS true",
        "(a !== null) IS false",
        "(null !== a) IS false",
        "(a != null) IS false",
        "(null != a) IS false",
        "(a) IS null",
        "(b === 3) IS false",
        "(3 === b) IS false",
        "(b !== 3) IS true",
        "(3 !== b) IS true");
  }

  public final void testAnd1() throws Exception {
    addFuzzyFact("a && b", true);
    addFuzzyFact("c && !d", false);
    assertFacts(
        "(a) LIKE true",
        "(b) LIKE true",
        "(a && b) LIKE true",
        "(c && !d) LIKE false");
  }

  public final void testAnd2() throws Exception {
    addFact("a && b", "true");
    addFact("c && !d", "false");
    assertFacts(
        "(a) LIKE true",
        "(b) IS true",
        "(a && b) IS true",
        "(c && !d) IS false");
  }

  public final void testDemorgans() throws Exception {
    addFuzzyFact("!(!a && !b)", false);
    assertFacts(
        "(! (!a && !b)) IS false",
        "(!a && !b) IS true",
        "(!a) IS true",
        "(!b) IS true",
        "(a) LIKE false",
        "(b) LIKE false");
  }

  public final void testOperandSwitching1() throws Exception {
    addFact("a * b === 3", "true");
    assertFacts(
        "(a * b) IS 3",
        "(b * a) IS 3",
        "(a * b === 3) IS true",
        "(a * b !== 3) IS false",
        "(a * b == 3) IS true",
        "(a * b != 3) IS false",
        "(3 === a * b) IS true",
        "(3 !== a * b) IS false",
        "(3 == a * b) IS true",
        "(3 != a * b) IS false");
  }

  public final void testNoOperandSwitching1() throws Exception {
    addFact("a + b === +x", "true");
    assertFacts(
        "(a + b === +x) IS true",
        "(a + b !== +x) IS false",
        "(a + b == +x) IS true",
        "(a + b != +x) IS false",
        "(+x === a + b) IS true",
        "(+x !== a + b) IS false",
        "(+x == a + b) IS true",
        "(+x != a + b) IS false",
        "(typeof (a + b)) IS 'number'",
        "(a + b === void 0) IS false",
        "(a + b !== void 0) IS true",
        "(void 0 === a + b) IS false",
        "(void 0 !== a + b) IS true");
  }

  public final void testGlobalObject() throws Exception {
    addFact("this === global", "true");
    addFact("!global", "false");
    addFact("typeof addEventListener", "'function'");
    assertFacts(
        true,
       "(this != global) IS false",
       "(this !== global) IS false",
       "(this == global) IS true",
       "(this === global) IS true",
       "(global != this) IS false",
       "(global !== this) IS false",
       "(global == this) IS true",
       "(global === this) IS true",
       "(!global) IS false",
       "(global) LIKE this",
       "(global.undefined) IS void 0",
       "(typeof addEventListener) IS 'function'",
       "(addEventListener !== void 0) IS true",
       "(addEventListener === void 0) IS false",
       "(void 0 !== addEventListener) IS true",
       "(void 0 === addEventListener) IS false",
       "(addEventListener) LIKE true",
       "(typeof global.addEventListener) IS 'function'",
       "(global.addEventListener !== void 0) IS true",
       "(global.addEventListener === void 0) IS false",
       "(void 0 !== global.addEventListener) IS true",
       "(void 0 === global.addEventListener) IS false",
       "(global.addEventListener) LIKE true",
       "(typeof this.addEventListener) IS 'function'",
       "(this.addEventListener !== void 0) IS true",
       "(this.addEventListener === void 0) IS false",
       "(void 0 !== this.addEventListener) IS true",
       "(void 0 === this.addEventListener) IS false",
       "(this.addEventListener) LIKE true");
  }

  public final void testSpecialFloatingValues() throws Exception {
    addFact("NaN === NaN", "false");
    addFact("Infinity === 1/0", "true");
    addFact("NZERO", "-0");
    assertFacts(
        "((1/0) === Infinity) IS true",
        "((1/0) !== Infinity) IS false",
        "((1/0) == Infinity) IS true",
        "((1/0) != Infinity) IS false",
        "(Infinity === (1/0)) IS true",
        "(Infinity !== (1/0)) IS false",
        "(Infinity == (1/0)) IS true",
        "(Infinity != (1/0)) IS false",
        "(Infinity) IS (1/0)",
        "(NZERO) IS (-0)",
        "(NaN !== NaN) IS true",
        "(NaN === NaN) IS false",
        "(NaN) IS (0/0)");

    assertEquals(
        "{\n  alert((0/0), (1/0), (-1/0), (-1/0), (-0));\n}",
        render(kb.optimize(
            js(fromString(
                "alert(NaN, Infinity, -Infinity, 1/NZERO, NZERO);")),
            mq)));
    assertNoErrors();
  }

  public final void testGlobalExistence() throws Exception {
    addFact("window === this", "true");
    addFact("!!window.addEventListener", "true");
    addFact("!window.attachEvent", "true");
    assertFacts(
        true,
        "(! !window.addEventListener) IS true",
        "(!window.addEventListener) IS false",
        "(!window.attachEvent) IS true",
        "(window.attachEvent) LIKE false",
        "(this != window) IS false",
        "(this !== window) IS false",
        "(this == window) IS true",
        "(this === window) IS true",
        "(window != this) IS false",
        "(window !== this) IS false",
        "(window == this) IS true",
        "(window === this) IS true",
        "(window.addEventListener) LIKE true",
        "(addEventListener) LIKE true",
        "(window.undefined) IS void 0",
        "(window) LIKE this"
        );
    assertNoErrors();
  }

  public final void testReplacement() throws Exception {
    addFact("typeof JSON", "'undefined'");
    addFact("this === window", "'undefined'");
    assertEquals(
        render(js(fromString(
            ""
            + "var oldJSON = void 0;"
            + "JSON = { parse: null, stringify: null };"))),
        render(kb.optimize(
            js(fromString(
                ""
                + "var oldJSON = window.JSON;"
                + "window.JSON = { parse: null, stringify: null };")),
            mq)));
  }

  public final void testOptimizeOutSimpleConditional() throws Exception {
    addFact("typeof window.JSON", "'object'");
    assertEquals(
        render(js(fromString(
            "foo(); ; bar()"))),
        render(kb.optimize(
            js(fromString(
                ""
                + "foo();"
                + "if (typeof window.JSON === 'undefined') {"
                + "  baz();"
                + "}"
                + "bar();")),
            mq)));
  }

  public final void testOptimizeOutMiddleConditional() throws Exception {
    addFact("typeof window.JSON", "'object'");
    assertEquals(
        render(js(fromString(
            ""
            + "foo();"
            + "if (typeof window.jsonParse) {"
            + "  foo2();"
            + "} else {"
            + "  backup();"
            + "}"
            + "bar()"))),
        render(kb.optimize(
            js(fromString(
                ""
                + "foo();"
                + "if (typeof window.jsonParse) {"
                + "  foo2();"
                + "} else if (typeof window.JSON === 'undefined') {"
                + "  baz();"
                + "} else {"
                + "  backup();"
                + "}"
                + "bar();")),
            mq)));
  }

  public final void testOptimizeOutTail() throws Exception {
    addFact("typeof window.JSON", "'object'");
    assertEquals(
        render(js(fromString(
            ""
            + "foo();"
            + "if (typeof window.jsonParse) {"
            + "  foo2();"
            + "} else {"
            + "  baz();"
            + "}"
            + "bar()"))),
        render(kb.optimize(
            js(fromString(
                ""
                + "foo();"
                + "if (typeof window.jsonParse) {"
                + "  foo2();"
                + "} else if (typeof window.JSON === 'object') {"
                + "  baz();"
                + "} else {"
                + "  backup();"
                + "}"
                + "bar();")),
            mq)));
  }

  public final void testOptimizeOutTailPreservingSideEffect()
      throws Exception {
    addFact("typeof window.JSON", "'object'");
    assertEquals(
        render(js(fromString(
            ""
            + "foo();"
            + "if (typeof window.jsonParse) {"
            + "  foo2();"
            + "} else {"
            + "  foo();"
            + "  baz();"
            + "}"
            + "bar()"))),
        render(kb.optimize(
            js(fromString(
                ""
                + "foo();"
                + "if (typeof window.jsonParse) {"
                + "  foo2();"
                + "} else if (foo() || typeof window.JSON === 'object') {"
                + "  baz();"
                + "} else {"
                + "  backup();"
                + "}"
                + "bar();")),
            mq)));
  }

  public final void testOptimizeOutMiddlePreservingSideEffect()
      throws Exception {
    addFact("typeof window.JSON", "'undefined'");
    assertEquals(
        render(js(fromString(
            ""
            + "foo();"
            + "if (typeof window.jsonParse) {"
            + "  foo2();"
            + "} else {"
            + "  foo();"
            + "  backup();"
            + "}"
            + "bar()"))),
        render(kb.optimize(
            js(fromString(
                ""
                + "foo();"
                + "if (typeof window.jsonParse) {"
                + "  foo2();"
                + "} else if (foo() && typeof window.JSON === 'object') {"
                + "  baz();"
                + "} else {"
                + "  backup();"
                + "}"
                + "bar();")),
            mq)));
  }

  public final void testLogicalOperatorsFuzzy1() throws Exception {
    addFact("foo", "0");
    assertEquals(
        render(js(fromString(
            ""
            + "var a = bar;"
            + "var b = 0;"  // Not folded to false.
            + "if (bar) { baz(); }"
            + ";"))),
        render(kb.optimize(
            js(fromString(
                ""
                + "var a = foo || bar;"
                + "var b = foo && bar;"
                + "if (foo || bar) { baz(); }"
                + "if (foo && bar) { boo(); }")),
            mq)));
  }

  public final void testLogicalOperatorsFuzzy2() throws Exception {
    addFact("!foo", "true");
    assertEquals(
        render(js(fromString(
            ""
            + "var a = foo || bar;"  // TODO: Could optimize out foo.
            + "var b = foo && bar;"  // Not folded to a constant.
            + "if (bar) { baz(); }"
            + ";"))),
        render(kb.optimize(
            js(fromString(
                ""
                + "var a = foo || bar;"
                + "var b = foo && bar;"
                + "if (foo || bar) { baz(); }"
                + "if (foo && bar) { boo(); }")),
            mq)));
  }

  public final void testLogicalOperatorsFuzzy3() throws Exception {
    addFact("foo", "true");
    addFuzzyFact("baz", true);
    addFuzzyFact("boo", false);
    assertEquals(
        render(js(fromString("{ bar(); }"))),
        render(kb.optimize(
            js(fromString("if (foo ? baz : boo) { bar(); }")),
            mq)));
  }

  public final void testFoldingPreservesSideEffects() throws Exception {
    addFact("x", "false");
    assertEquals(
        render(js(fromString(
            ""
            + "foo();"  // Not folded to a constant.
            + "return bar(), baz();"))),
        render(kb.optimize(
            js(fromString(
                ""
                + "if (void foo()) { boo(); }"
                + "return (bar() && x) || baz()")),
            mq)));
  }

  public final void testPropertyNamesNotOptimized() throws Exception {
    assertEquals(
        render(js(fromString("[void 0, window.undefined]"))),
        render(kb.optimize(js(fromString("[undefined, window.undefined]")),
               mq)));
    // But do optimize the member access when appropriate
    kb.addFact(jsExpr(fromString("window.undefined")), Fact.UNDEFINED);
    assertEquals(
        render(js(fromString("[void 0, void 0]"))),
        render(kb.optimize(js(fromString("[undefined, window.undefined]")),
               mq)));
  }

  public final void testMemberAccess() throws Exception {
    addFact("navigator.userAgent", "\"Foo\\/Bar [Baz Compatible 1234] v5678\"");
    assertEquals(
        render(js(fromString("9"))),
        render(kb.optimize(js(fromString("navigator.userAgent.indexOf('Baz')")),
               mq)));
  }

  public final void testLocalVariableReferences1() throws Exception {
    addFact("ZERO", "0");
    addFact("ONE", "1");
    assertEquals(
        render(js(fromString(
            "[0, 1, (function (ONE) { return [0, ONE]; })]"))),
        render(kb.optimize(
            js(fromString(
                "[ZERO, ONE, (function (ONE) { return [ZERO, ONE]; })]")),
            mq)));
  }

  public final void testLocalVariableReferences2() throws Exception {
    addFact("ZERO", "0");
    addFact("ONE", "1");
    assertEquals(
        render(js(fromString(
            ""
            + "[0, 1,"
            + " (function () {"
            + "   try { throw 1; } catch (ONE) { return [0, ONE]; }"
            + " })]"))),
        render(kb.optimize(
            js(fromString(
                ""
                + "[ZERO, ONE,"
                + " (function () {"
                + "   try { throw ONE; } catch (ONE) { return [ZERO, ONE]; }"
                + " })]")),
            mq)));
  }

  public final void testInlining1() throws Exception {
    addFact("/MSIE/.test(navigator.userAgent)", "true");
    addFuzzyFact("window.opera", false);
    addFact("/Firefox/.test(navigator.appName || '')", "false");
    assertEquals(
        render(js(fromString(
            ""
            + "function listen(n, t, fn) {"
            + "  { n.attachEvent(t, fn); }"
            + "}"))),
        render(kb.optimize(
            js(fromString(
                ""
                + "function listen(n, t, fn) {"
                + "  var isIE = /MSIE/.test(navigator.userAgent)"
                + "      && !window.opera;"
                + "  var isFF = /Firefox/.test(navigator.appName || '');"
                + "  if (isIE) {"
                + "    n.attachEvent(t, fn);"
                + "  } else if (isFF) {"
                + "    n.addEventListener(t, fn);"
                + "  } else { throw new Error('forgot opera :('); }"
                + "}")),
            mq)));
  }

  public final void testInlining2() throws Exception {
    addFact("/MSIE/.test(navigator.userAgent)", "false");
    addFuzzyFact("window.opera", false);
    addFact("/Firefox/.test(navigator.appName || '')", "true");
    assertEquals(
        render(js(fromString(
            ""
            + "function listen(n, t, fn) {"
            + "  { n.addEventListener(t, fn); }"
            + "}"))),
        render(kb.optimize(
            js(fromString(
                ""
                + "function listen(n, t, fn) {"
                + "  var isIE = /MSIE/.test(navigator.userAgent)"
                + "      && !window.opera;"
                + "  var isFF = /Firefox/.test(navigator.appName || '');"
                + "  if (isIE) {"
                + "    n.attachEvent(t, fn);"
                + "  } else if (isFF) {"
                + "    n.addEventListener(t, fn);"
                + "  } else { throw new Error('forgot opera :('); }"
                + "}")),
            mq)));
  }

  public final void testInlining3() throws Exception {
    addFact("/MSIE/.test(navigator.userAgent)", "true");
    addFact("window.opera", "true");
    addFact("/Firefox/.test(navigator.appName || '')", "false");
    assertEquals(
        render(js(fromString(
            ""
            + "function listen(n, t, fn) {"
            + "  { throw new Error('forgot opera :('); }"
            + "}"))),
        render(kb.optimize(
            js(fromString(
                ""
                + "function listen(n, t, fn) {"
                + "  var isIE = /MSIE/.test(navigator.userAgent)"
                + "      && !window.opera;"
                + "  var isFF = /Firefox/.test(navigator.appName || '');"
                + "  if (isIE) {"
                + "    n.attachEvent(t, fn);"
                + "  } else if (isFF) {"
                + "    n.addEventListener(t, fn);"
                + "  } else { throw new Error('forgot opera :('); }"
                + "}")),
            mq)));
  }

  public final void testTypeofKnowledge() throws Exception {
    addFact("typeof Date.prototype.toISOString", "'function'");
    assertEquals(
        render(js(fromString(
            ""
            + ";"))),
        render(kb.optimize(
            js(fromString(
                ""
                + "if (Date.prototype.toISOString === void 0) {"
                + "  Date.prototype.toISOString = Date.prototype.toJSON;"
                + "}")),
            mq)));
  }

  public final void testHookOptimization() throws Exception {
    addFact("typeof Date.now", "'function'");
    assertEquals(
        render(js(fromString(
            "t = Date.now()"
            + ";"))),
        render(kb.optimize(
            js(fromString(
                "t = Date.now ? Date.now() : (new Date).getTime()")),
            mq)));
  }

  public final void testGlobalFolding1() throws Exception {
    addFact("typeof Date.now", "'function'");
    addFact("window === this", "'function'");
    checkGlobalFolding();
  }

  public final void testGlobalFolding2() throws Exception {
    addFact("typeof window.Date.now", "'function'");
    addFact("window === this", "'function'");
    checkGlobalFolding();
  }

  private void checkGlobalFolding() throws Exception {
    assertEquals(
        render(js(fromString(
            ""
            + "function borken() {"
            + "  return this.Date.now ? this.Date.now() : +(new this.Date);"
            + "}"
            + "function borken2(window) {"
            + "  return window.Date.now"
            + "      ? window.Date.now() : +(new window.Date);"
            + "}"
            + "function ok() {"
            + "  return Date.now();"
            + "}"
            + "var ok2 = Date.now();"
            + "var ok3 = Date.now();"))),
        render(kb.optimize(
            js(fromString(
                ""
                + "function borken() {"
                + "  return this.Date.now ? this.Date.now() : +(new this.Date);"
                + "}"
                + "function borken2(window) {"
                + "  return window.Date.now"
                + "      ? window.Date.now() : +(new window.Date);"
                + "}"
                + "function ok() {"
                + "  return window.Date.now"
                + "      ? window.Date.now() : +(new window.Date);"
                + "}"
                + "var ok2 = window.Date.now"
                + "    ? window.Date.now() : +(new window.Date);"
                + "var ok3 = this.Date.now"
                + "    ? this.Date.now() : +(new this.Date);"
                )),
            mq)));
    assertNoErrors();
  }

  public final void testOptimizeOutGlobals1() throws Exception {
    addFact("window === this", "'function'");
    assertEquals(
        render(js(fromString(
            ""
            + "function al(t, listener) {"
            + "  addEventListener(t, listener);"
            + "}"))),
        render(kb.optimize(
            js(fromString(
                ""
                + "function al(t, listener) {"
                + "  window.addEventListener(t, listener);"
                + "}")),
            mq)));
    assertNoErrors();
  }

  public final void testOptimizeOutGlobals2() throws Exception {
    addFact("window === this", "'function'");
    assertEquals(
        render(js(fromString(
            ""
            + "function al(window, t, listener) {"
            + "  window.addEventListener(t, listener);"
            + "}"))),
        render(kb.optimize(
            js(fromString(
                ""
                + "function al(window, t, listener) {"
                + "  window.addEventListener(t, listener);"
                + "}")),
            mq)));
    assertNoErrors();
  }

  public final void testOptimizeOutGlobals3() throws Exception {
    addFact("window === this", "'function'");
    assertEquals(
        render(js(fromString(
            ""
            + "function al(t, addEventListener) {"
            + "  window.addEventListener(t, addEventListener);"
            + "}"))),
        render(kb.optimize(
            js(fromString(
                ""
                + "function al(t, addEventListener) {"
                + "  window.addEventListener(t, addEventListener);"
                + "}")),
            mq)));
    assertNoErrors();
  }

  public final void testComparisonsToSpecials() throws Exception {
    // Since we know x is falsey we can't conclude that much.
    addFact("!x", "true");
    // But since y is truthy, it can't be null, undefined, false, 0, NaN, ''
    addFact("!!y", "true");
    addFact("NaN", "0/0");
    assertEquals(
        Join.join(
            ",",
            "alert([x==null",   "x===null",
                   "x!=null",   "x!==null",
                   "x==void 0", "x===void 0",
                   "x!=void 0", "x!==void 0",
                   "x!=''",     "x!==''",
                   "x!=0",      "x!==0",
                   "x!=1",      "true",
                   "x!=(0/0)",  "x!==(0/0)",
                   "x!=false",  "x!==false",
                   "false",     "false",
                   "true",      "true",
                   "false",     "false",
                   "true",      "true",
                   "y!=''",     "true",  // [] == '', but [] !== ''
                   "y!=0",      "true",  // new Number(0) == 0
                   "y!=1",      "y!==1",
                   "y!=(0/0)",  "true",  // possibly safe to optimize left
                   "y!=false",  "true])"),   // new Boolean(false) == false
        renderMin(kb.optimize(
            js(fromString(
                ""
                + "alert([\n"
                + "    x == null,      x === null,\n"
                + "    x != null,      x !== null,\n"
                + "    x == undefined, x === undefined,\n"
                + "    x != undefined, x !== undefined,\n"
                + "    x != '',        x !== '',\n"
                + "    x != 0,         x !== 0,\n"
                + "    x != 1,         x !== 1,\n"
                + "    x != NaN,       x !== NaN,\n"
                + "    x != false,     x !== false,\n"
                + "    y == null,      y === null,\n"
                + "    y != null,      y !== null,\n"
                + "    y == undefined, y === undefined,\n"
                + "    y != undefined, y !== undefined,\n"
                + "    y != '',        y !== '',\n"
                + "    y != 0,         y !== 0,\n"
                + "    y != 1,         y !== 1,\n"
                + "    y != NaN,       y !== NaN,\n"
                + "    y != false,     y !== false\n"
                + "    ])")),
            mq)));
    assertNoErrors();
  }

  public final void testFolding() throws Exception {
    assertEquals(
        render(js(fromString("x = 2;"))),
        render(kb.optimize(js(fromString("x = 1 + 1;")), mq)));
  }

  private void addFact(String expr, String value) throws ParseException {
    kb.addFact(jsExpr(fromString(expr)),
               Fact.is((Literal) jsExpr(fromString(value)).fold(false)));
  }

  private void addFuzzyFact(String expr, boolean truthy) throws ParseException {
    kb.addFact(jsExpr(fromString(expr)), truthy ? Fact.TRUTHY : Fact.FALSEY);
  }

  private void assertFacts(String... expected) {
    assertFacts(false, expected);
  }

  private void assertFacts(boolean finish, String... expected) {
    List<String> expectedKnowledge = Arrays.asList(expected);
    List<String> actualKnowledge = Lists.newArrayList();
    if (finish) { kb.finishInference(); }
    for (Pair<Expression, Fact> k : knowledge) {
      actualKnowledge.add(
          "(" + render(k.a) + ") " + k.b.type + " " + render(k.b.value));
    }
    Collections.sort(actualKnowledge);
    Collections.sort(expectedKnowledge);
    MoreAsserts.assertListsEqual(expectedKnowledge, actualKnowledge);
  }

  private static String renderMin(Block js) {
    StringBuilder sb = new StringBuilder();
    JsMinimalPrinter p = new JsMinimalPrinter(sb);
    p.setLineLengthLimit(1000);
    js.renderBody(new RenderContext(p));
    p.noMoreTokens();
    return sb.toString();
  }
}
