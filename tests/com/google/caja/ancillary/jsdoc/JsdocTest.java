// Copyright (C) 2008 Google Inc.
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

package com.google.caja.ancillary.jsdoc;

import com.google.caja.lexer.InputSource;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.js.Expression;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.FailureIsAnOption;

import java.net.URI;

public class JsdocTest extends CajaTestCase {
  public final void testNoCode() throws Exception {
    assertExtracted("{ \"@fileoverview\": {} }", ";");
    assertNoErrors();
  }

  public final void testVarAssignedPrimitive() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {},"
        + "  \"foo\": {"
        + "    \"@description\": \"Hello World \","
        + "    \"@pos\": \"testVarAssignedPrimitive:1+1 - 34\","
        + "    \"@summary\": \"Hello World \","
        + "    \"@type\": [\"string\"]"
        + "  }"
        + "}",
        "/** Hello World @type {string} */\n"
        + "var foo = 'bar';");
    assertNoErrors();
  }

  public final void testMultiDeclaration() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {},"
        + "  \"bar\": {"
        + "    \"@description\": \"bar \","
        + "    \"@pos\": \"testMultiDeclaration:3+3 - 13\","
        + "    \"@summary\": \"bar \","
        + "    \"@type\": [\"string\"]"
        + "  },"
        + "  \"foo\": {"
        + "    \"@description\": \"foo \","
        + "    \"@pos\": \"testMultiDeclaration:1+1 - 11\","
        + "    \"@summary\": \"foo \","
        + "    \"@type\": [\"string\"]"
        + "  }"
        + "}",
        "/** foo */\n"
        + "var foo = 'bar',\n"
        + "  /** bar */\n"
        + "  bar = 'bar';");
    assertNoErrors();
  }

  public final void testAliasedVars() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {},"
        + "  \"goog\": {"
        + "    \"@pos\": \"testAliasedVars:1+1 - 15\","
        + "    \"@public\": [true],"
        + "    \"@type\": [\"Object\"],"
        + "    \"bar\": {"
        + "      \"@aliases\": \"goog.foo\""
        + "    },"
        + "    \"foo\": {"
        + "      \"@description\": \"Foo \","
        + "      \"@pos\": \"testAliasedVars:3+1 - 11\","
        + "      \"@summary\": \"Foo \","
        + "      \"@type\": [\"number\"]"
        + "    }"
        + "  }"
        + "}",

        ""
        + "/** @public */\n"
        + "var goog = {};\n"
        + "/** Foo */\n"
        + "goog.foo = 4;\n"
        + "goog.bar = goog.foo;\n");
    assertNoErrors();
  }

  public final void testForwardLinks() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {},"
        + "  \"bar\": {"
        + "    \"@description\": \"Not spiffy :( \","
        + "    \"@pos\": \"testForwardLinks:3+1 - 21\","
        + "    \"@summary\": \"Not spiffy :( \","
        + "    \"@type\": [\"Object\"]"
        + "  },"
        + "  \"spiffyBar\": {"
        + "    \"@description\": \"Like <a href=\\x22javascript:"
            + "navigateToApiElement(%22bar%22)\\x22>bar</a> but spiffier. \","
        + "    \"@pos\": \"testForwardLinks:1+1 - 38\","
        + "    \"@summary\": \"Like <a href=\\x22javascript:"
            + "navigateToApiElement(%22bar%22)\\x22>bar</a> but spiffier.\","
        + "    \"@type\": [\"Object\"],"
        + "    \"spiffy\": {"
        + "      \"@type\": [\"boolean\"]"
        + "    }"
        + "  }"
        + "}",

        ""
        + "/** Like {@link bar} but spiffier. */\n"
        + "var spiffyBar = { spiffy: true };\n"
        + "/** Not spiffy :( */\n"
        + "var bar = {};");
    assertNoErrors();
  }

  public final void testUninitializedVars() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {},"
        + "  \"EPOCH\": {"
        + "    \"@description\": \"Time zero.  1 Jan 1970 12:00 UTC \","
        + "    \"@pos\": \"testUninitializedVars:1+1 - 40\","
        + "    \"@summary\": \"Time zero.\","
        + "    \"@type\": [\"Date\"]"
        + "  }"
        + "}",

        ""
        + "/** Time zero.  1 Jan 1970 12:00 UTC */\n"
        + "var EPOCH;\n"
        + ""
        + "(function () { EPOCH = new Date(0); })();"
        );
    assertNoErrors();
  }

  public final void testFileOverview1() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {"
        + "    \"testFileOverview1\": {"
        + "      \"@description\":"
        + "        \"\\x0aThis is a file comment.  Not part of var foo.\\x0a\","
        + "      \"@pos\": \"testFileOverview1:1+1 - 4+4\","
        + "      \"@see\": [{ \"url\": \"./otherfile.js\" }],"
        + "      \"@summary\": \"\\x0aThis is a file comment.\""
        + "    }"
        + "  },"
        + "  \"foo\": {"
        + "    \"@type\": [\"Object\"]"
        + "  }"
        + "}",

        ""
        + "/**\n"
        + " * @fileoverview This is a file comment.  Not part of var foo.\n"
        + " * @see ./otherfile.js\n"
        + " */\n"
        + "\n"
        + "var foo = {};");
    assertNoErrors();
  }

  public final void testFileOverview2() throws Exception {
    is = new InputSource(new URI("file:///foo/bar/baz.js"));

    // The foo is irrelevant, so is skipped everywhere file positions appear.
    mc = new MessageContext();
    mc.addInputSource(is);
    mc.addInputSource(new InputSource(new URI("file:///foo/boo/far.js")));

    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {"
        + "    \"bar\": {"
        + "      \"baz.js\": {"
        + "        \"@description\": \"file \","
        + "        \"@pos\": \"bar/baz.js:2+1 - 26\","
        + "        \"@summary\": \"file \""
        + "      }"
        + "    }"
        + "  },"
        + "  \"foo\": {"
        + "    \"@description\": \"Foo \","
        + "    \"@pos\": \"bar/baz.js:4+1 - 11\","
        + "    \"@summary\": \"Foo \","
        + "    \"@type\": [\"Object\"]"
        + "  }"
        + "}",

        ""
        + "// Not a doc comment\n"
        + "/** @fileoverview file */\n"
        + "\n"
        + "/** Foo */"
        + "var foo = {};");
    assertNoErrors();
  }

  public final void testSummarization1() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {},"
        + "  \"x\": {"
        + "    \"@description\": \"Sentence 1.  Sentence 2. \","
        + "    \"@pos\": \"testSummarization1:1+1 - 32\","
        + "    \"@summary\": \"Sentence 1.\","
        + "    \"@type\": [\"Object\"]"
        + "  }"
        + "}",

        ""
        + "/** Sentence 1.  Sentence 2. */\n"
        + "var x = {};");
  }

  public final void testSummarization2() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {},"
        + "  \"x\": {"
        + "    \"@description\": \"Dr.<!----> Evil is my name. \","
        + "    \"@pos\": \"testSummarization2:1+1 - 35\","
        + "    \"@summary\": \"Dr.<!----> Evil is my name.\","
        + "    \"@type\": [\"Object\"]"
        + "  }"
        + "}",

        ""
        + "/** Dr.<!----> Evil is my name. */\n"
        + "var x = {};");
  }

  public final void testSummarization3() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {},"
        + "  \"isRed\": {"
        + "    \"@description\": \"Is it red?  Or kind of red. \","
        + "    \"@pos\": \"testSummarization3:1+1 - 35\","
        + "    \"@summary\": \"Is it red?\","
        + "    \"@type\": [\"boolean\"]"
        + "  }"
        + "}",

        ""
        + "/** Is it red?  Or kind of red. */\n"
        + "var isRed = true;");
  }

  public final void testSummarization4() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {},"
        + "  \"isRed\": {"
        + "    \"@description\": \"It is red (i.e. not blue). \","
        + "    \"@pos\": \"testSummarization4:1+1 - 34\","
        + "    \"@summary\": \"It is red (i.e. not blue).\","
        + "    \"@type\": [\"boolean\"]"
        + "  }"
        + "}",

        ""
        + "/** It is red (i.e. not blue). */\n"
        + "var isRed = true;");
  }

  public final void testClassTypes() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {},"
        + "  \"MyClass\": {"
        + "    \"@constructor\": [ true ],"
        + "    \"@extends\": [\"Object\"],"
        + "    \"@field\": {"
        + "      \"x\": {"
        + "      }"
        + "    },"
        + "    \"@param\": [{"
        + "      \"name\": \"x\","
        + "      \"summary\": \"\","
        + "      \"type\": \"number\""
        + "    }],"
        + "    \"@pos\": \"testClassTypes:1+1 - 38\","
        + "    \"@type\": [\"Function\"],"
        + "    \"prototype\": {"
        + "      \"@type\": [\"MyClass\"],"
        + "      \"getX\": {"
        + "        \"@description\": \"get's x.  \","
        + "        \"@pos\": \"testClassTypes:3+1 - 42\","
        + "        \"@public\": [true],"
        + "        \"@return\": [{ \"summary\": \"\", \"type\": \"number\" }],"
        + "        \"@summary\": \"get's x.\","
        + "        \"@type\": [\"Function\"],"
        + "        \"prototype\": {"
        + "          \"@type\": [\"MyClass.prototype.getX\"]"
        + "        }"
        + "      }"
        + "    }"
        + "  },"
        + "  \"inst\": {"
        + "    \"@type\": [\"MyClass\"],"
        + "    \"x\": {"
        + "      \"@type\": [\"undefined\"]"
        + "    }"
        + "  }"
        + "}",

        ""
        + "/** @param {number} x @constructor */\n"
        + "function MyClass(x) { this.x = x; }\n"
        + "/** get's x.  @public @return {number} */\n"
        + "MyClass.prototype.getX = function () { return this.x; };\n"
        + "\n"
        + "var inst = new MyClass();");
    assertNoErrors();
  }

  public final void testHardToReachComments() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {},"
        + "  \"x\": {"
        + "    \"@type\": [\"Object\"],"
        + "    \"y\": {"
        + "      \"@description\": \"Foo\","
        + "      \"@pos\": \"testHardToReachComments:2+3 - 11\","
        + "      \"@summary\": \"Foo\","
        + "      \"@type\": [\"string\"]"
        + "    },"
        + "    \"z\": {"
        + "      \"@description\": \"Bar\","
        + "      \"@pos\": \"testHardToReachComments:4+6 - 14\","
        + "      \"@summary\": \"Bar\","
        + "      \"@type\": [\"string\"]"
        + "    }"
        + "  }"
        + "}",

        ""
        + "var x = {\n"
        + "  /**Foo*/\n"
        + "  y: 'foo',\n"
        + "  z: /**Bar*/'bar'\n"
        + "};");
    assertNoErrors();
  }

  public final void testChangedIntrinsics() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {},"
        + "  \"Array\": {"
        + "    \"sluice\": {"
        + "      \"@description\": \"Fill Array with water. \","
        + "      \"@pos\": \"testChangedIntrinsics:1+1 - 30\","
        + "      \"@summary\": \"Fill Array with water.\","
        + "      \"@type\": [\"Function\"],"
        + "      \"prototype\": {"
        + "        \"@type\": [\"Array.sluice\"]"
        + "      }"
        + "    }"
        + "  }"
        + "}",

        ""
        + "/** Fill Array with water. */\n"
        + "Array.sluice = function (arr, water) {\n"
        + "  while (arr.length < 10) { arr.push(water); }\n"
        + "  arr.length = 10;\n"
        + "};");
    assertNoErrors();
  }

  public final void testNoSuchParam() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {},"
        + "  \"f\": {"
        + "    \"@description\": \"\\x0a\","
        + "    \"@extends\": [\"Object\"],"
        + "    \"@field\": {},"
        + "    \"@param\": ["
        + "        { \"name\": \"x\", \"summary\": \"a parameter\\x0a\" },"
        + "        { \"name\": \"w\", \"summary\": \"missing\\x0a\" }"
        + "    ],"
        + "    \"@pos\": \"testNoSuchParam:1+1 - 4+4\","
        + "    \"@summary\": \"\\x0a\","
        + "    \"@type\": [\"Function\"],"
        + "    \"prototype\": {"
        + "      \"@type\": [\"f\"]"
        + "    }"
        + "  }"
        + "}",

        ""
        + "/**\n"
        + " * @param x a parameter\n"
        + " * @param w missing\n"
        + " */\n"
        + "function f(x, y, z) {}");

    assertJsdocMessage("Parameter w not defined on function f");
  }

  public final void testManyParams() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {},"
        + "  \"f\": {"
        + "    \"@description\": \"\\x0a\","
        + "    \"@extends\": [\"Object\"],"
        + "    \"@field\": {},"
        + "    \"@param\": ["
        + "        { \"name\": \"x\", \"summary\": \"a parameter\\x0a\","
        + "          \"type\": \"RegExp\" },"
        + "        { \"name\": \"y\", \"summary\": \"another\\x0a\","
        + "          \"type\": \"Array.<number>\" }"
        + "    ],"
        + "    \"@pos\": \"testManyParams:1+1 - 4+4\","
        + "    \"@summary\": \"\\x0a\","
        + "    \"@type\": [\"Function\"],"
        + "    \"prototype\": {"
        + "      \"@type\": [\"f\"]"
        + "    }"
        + "  }"
        + "}",

        ""
        + "/**\n"
        + " * @param {RegExp} x a parameter\n"
        + " * @param {Array.<number>} y another\n"
        + " */\n"
        + "function f(x, y, z) {}");

    assertNoErrors();
  }

  public final void testMultipleSees() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {},"
        + "  \"foo\": {"
        + "    \"@pos\": \"testMultipleSees:4+3 - 32\","
        + "    \"@see\": ["
        + "      { \"url\": \"" + navigationLink("foo.a") + "\" },"
        + "      { \"url\": \"" + navigationLink("other_name") + "\" }"
        + "    ],"
        + "    \"@type\": [\"Object\"],"
        + "    \"a\": {"
        + "      \"@type\": [\"Object\"]"
        + "    }"
        + "  },"
        + "  \"other_name\": {"
        + "    \"@type\": [\"Object\"]"
        + "  }"
        + "}",

        ""
        + "var foo = (function () {\n"
        + "  var that = {}, the_other = {};\n"
        + "  this.other_name = the_other;\n"
        + "  /**@see that @see the_other*/\n"
        + "  return { a: that };\n"
        + "})();");
    assertNoErrors();
  }

  public final void testUpdoc() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {"
        + "  },"
        + "  \"Math\": {"
        + "    \"sign\": {"
        + "      \"@description\": "
            + "\"\\x0aSign of a number.\\x0a"
            + "<pre class=\\x22prettyprint lang-js updoc\\x22>"
            + "<div class=\\x22pass\\x22>"
                + "$ Math.sign(4);\\x0a"
                + "# 1;"
            + "</div>\\x0a"
            + "<div class=\\x22pass\\x22>"
                + "$ Math.sign(-4);\\x0a"
                + "# -1;"
            + "</div>\\x0a"
            + "<div class=\\x22pass\\x22>"
                + "$ Math.sign(NaN);\\x0a"
                + "# NaN;"
            + "</div>\\x0a"
            + "<div class=\\x22fail\\x22>"
                + "$ Math.sign(0);\\x0a"
                + "# 0;  // Was NaN : number"
            + "</div>\\x0a"
            + "</pre>\\x0a\","
        + "      \"@pos\": \"testUpdoc:1+1 - 13+4\","
        + "      \"@summary\": \"\\x0aSign of a number.\","
        + "      \"@type\": [\"Function\"],"
        + "      \"prototype\": {"
        + "        \"@type\": [\"Math.sign\"]"
        + "      }"
        + "    }"
        + "  }"
        + "}",

        ""
        + "/**\n"
        + " * Sign of a number.\n"
        + " * {@updoc\n"
        + " * $ Math.sign(4)\n"
        + " * # 1\n"
        + " * $ Math.sign(-4)\n"
        + " * # -1\n"
        + " * $ Math.sign(NaN)\n"
        + " * # NaN\n"
        + " * $ Math.sign(0)\n"
        + " * # 0\n"
        + " * }\n"
        + " */\n"
        + "Math.sign = function (n) { return n / Math.abs(n); };\n"
        );
  }

  public final void testHtmlNormalization() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {},"
        + "  \"a\": {"
        + "    \"@description\":"
        + "        \"<p>a &lt; b<br>&amp;&amp; <b><i>c</i></b> &lt; d </p>\","
        + "    \"@pos\": \"testHtmlNormalization:1+1 - 38\","
        + "    \"@summary\":"
        + "        \"<p>a &lt; b<br>&amp;&amp; <b><i>c</i></b> &lt; d </p>\","
        + "    \"@type\": [\"undefined\"]"
        + "  }"
        + "}",

        ""
        + "/** <p>a < b<br>&& <b><i>c</b> < d */"
        + "var a;");
    assertNoErrors();
  }

  public final void testThisBinding() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {},"
        + "  \"C\": {"
        + "    \"@extends\": [\"Object\"],"
        + "    \"@field\": {"
        + "      \"x\": {"
        + "        \"@pos\": \"testThisBinding:2+16 - 37\","
        + "        \"@type\": [\"number\"]"
        + "      }"
        + "    },"
        + "    \"@type\": [\"Function\"],"
        + "    \"prototype\": {"
        + "      \"@type\": [\"C\"],"
        + "      \"g\": {"
        + "        \"@extends\": [\"Object\"],"
        + "        \"@field\": {\"x\": {}},"
        + "        \"@type\": [\"Function\"],"
        + "        \"prototype\": {"
        + "          \"@type\": [\"C.prototype.g\"]"
        + "        }"
        + "      }"
        + "    }"
        + "  },"
        + "  \"f\": {"
        + "    \"@extends\": [\"Object\"],"
        + "    \"@field\": {},"
        + "    \"@type\": [\"Function\"],"
        + "    \"prototype\": {"
        + "      \"@type\": [\"f\"]"
        + "    }"
        + "  }"
        + "}",

        ""
        + "function f() { return 4; }\n"
        + "function C() { /** @type {number} */ this.x = 4; }\n"
        + "C.prototype.g = function () { return this.x; };");
    assertNoErrors();
  }

  public final void testInheritance() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {},"
        + "  \"myNamespace\": {"
        + "    \"@type\": [ \"Object\" ],"
        + "    \"BC\": {"
        + "      \"@extends\": [ \"Object\" ],"
        + "      \"@field\": {},"
        + "      \"@type\": [ \"Function\" ],"
        + "      \"prototype\": {"
        + "        \"@type\": [ \"myNamespace.BC\" ]"
        + "      }"
        + "    },"
        + "    \"DC1\": {"
        + "      \"@extends\": [ \"myNamespace.BC\" ],"
        + "      \"@field\": {},"
        + "      \"@type\": [ \"Function\" ],"
        + "      \"prototype\": {"
        + "        \"@type\": [ \"object\" ]"
        + "      }"
        + "    },"
        + "    \"DC2\": {"
        + "      \"@extends\": [ \"myNamespace.BC\" ],"
        + "      \"@field\": {},"
        + "      \"@type\": [ \"Function\" ],"
        + "      \"prototype\": {"
        + "        \"@type\": [ \"myNamespace.DC2\" ],"
        + "        \"constructor\": {"
        + "          \"@aliases\": \"myNamespace.DC2\""
        + "        }"
        + "      }"
        + "    }"
        + "  }"
        + "}",

        ""
        + "var myNamespace = (function () {\n"
        + "  function BaseClass() {}\n"
        + "  function DerivedClass1() {}\n"
        + "  DerivedClass1.prototype = new BaseClass();\n"
        + "  // Forgot to set DerivedClass1.prototype.constructor\n"
        + "  function DerivedClass2() {}\n"
        + "  DerivedClass2.prototype = new BaseClass();\n"
        + "  DerivedClass2.prototype.constructor = DerivedClass2;\n"
        + "  return {\n"
        + "    BC: BaseClass,\n"
        + "    DC1: DerivedClass1,\n"
        + "    DC2: DerivedClass2\n"
        + "  };\n"
        + "})();");
    assertNoErrors();
  }

  public final void testBadTypes() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {"
        + "  },"
        + "  \"MyClass\": {"
        + "    \"@extends\": [ \"Object\" ],"
        + "    \"@field\": {"
        + "    },"
        + "    \"@type\": [ \"Function\" ],"
        + "    \"prototype\": {"
        + "      \"@type\": [ \"MyClass\" ]"
        + "    }"
        + "  },"
        + "  \"MyOtherClass\": {"
        + "    \"@extends\": [ \"Object\" ],"
        + "    \"@field\": {"
        + "    },"
        + "    \"@type\": [ \"Function\" ],"
        + "    \"prototype\": {"
        + "      \"@type\": [ \"MyOtherClass\" ]"
        + "    }"
        + "  },"
        + "  \"inst\": {"
        + "    \"@pos\": \"testBadTypes:3+1 - 44\","
        + "    \"@type\": [ \"Arra.<MyClass | MyOtherClas>\" ]"
        + "  }"
        + "}",

        ""
        + "function MyClass() {}\n"
        + "function MyOtherClass() {}\n"
        + "/** @type {Arra.<MyClass | MyOtherClas>} */\n"
        + "var inst = [];");
    assertJsdocMessage("testBadTypes:3+5 - 42 : Arra is not a type");
    assertJsdocMessage("testBadTypes:3+5 - 42 : MyOtherClas is not a type");
  }

  @FailureIsAnOption
  public final void testSpecialOperation() throws Exception {
    assertExtracted(""
        + "{"
        + "  '@fileoverview': {},"
        + "  'policy': {"
        + "    '@description': 'doc policy ',"
        + "    '@pos': 'testSpecialOperation:1+1 - 18',"
        + "    '@summary': 'doc policy ',"
        + "    '@type': [ 'Object' ],"
        + "    'net': {"
        + "      '@description': 'doc for net ',"
        + "      '@pos': 'testSpecialOperation:3+3 - 21',"
        + "      '@summary': 'doc for net ',"
        + "      '@type': [ 'Object' ],"
        + "      'only': {"
        + "        '@description': 'doc for only ',"
        + "        '@pos': 'testSpecialOperation:5+5 - 23',"
        + "        '@summary': 'doc for only ',"
        + "        '@type': [ 'Object' ]"
        + "      }"
        + "    }"
        + "  }"
        + "}",
        ""
        + "/** doc policy */\n"
        + "var policy = {\n"
        + "  /** doc for net */\n"
        + "  \"net\": {\n"
        + "    /** doc for only */\n"
        + "    \"only\": null"
        + "  }\n"
        + "};");
    assertNoErrors();
  }

  public final void testCorrectExpressionExecution() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {"
        + "  },"
        + "  \"aString\": {"
        + "    \"@type\": [ \"string\" ]"
        + "  },"
        + "  \"anotherString\": {"
        + "    \"@type\": [ \"string\" ]"
        + "  },"
        + "  \"b\": {"
        + "    \"@description\": \"a boolean \","
        + "    \"@pos\": \"testCorrectExpressionExecution:1+1 - 17\","
        + "    \"@summary\": \"a boolean \","
        + "    \"@type\": [ \"boolean\" ]"
        + "  },"
        + "  \"n\": {"
        + "    \"@description\": \"a number \","
        + "    \"@pos\": \"testCorrectExpressionExecution:1+31 - 46\","
        + "    \"@summary\": \"a number \","
        + "    \"@type\": [ \"number\" ]"
        + "  },"
        + "  \"neverInitialized\": {"
        + "    \"@type\": [ \"undefined\" ]"
        + "  },"
        + "  \"sum\": {"
        + "    \"@type\": [ \"number\" ]"
        + "  },"
        + "  \"yans\": {"
        + "    \"@type\": [ \"string\" ]"
        + "  }"
        + "}",

        ""
        + "/** a boolean */"
        + "var b = false;"
        + "/** a number */"
        + "var n = 4;"
        + "var sum = n + n;"
        + "var aString = b ? 4 : 'four';"
        + "var anotherString = b || 'hello';"
        + "var yans = typeof n === 'number' ? 'foo' : null;"
        + "if (typeof neverDefined !== 'undefined') {"
        + "  var neverInitialized = 4;"
        + "}"
        );
  }

  public final void testCorrectStatementExecution() throws Exception {
    assertExtracted(
        ""
        + "{"
        + "  \"@fileoverview\": {"
        + "  },"
        + "  \"b\": {"
        + "    \"@description\": \"a boolean \","
        + "    \"@pos\": \"testCorrectStatementExecution:1+1 - 17\","
        + "    \"@summary\": \"a boolean \","
        + "    \"@type\": [ \"boolean\" ]"
        + "  },"
        + "  \"n\": {"
        + "    \"@description\": \"a number \","
        + "    \"@pos\": \"testCorrectStatementExecution:1+31 - 46\","
        + "    \"@summary\": \"a number \","
        + "    \"@type\": [ \"number\" ]"
        + "  },"
        + "  \"s1\": {"
        + "    \"@type\": [ \"string\" ]"
        + "  },"
        + "  \"s2\": {"
        + "    \"@type\": [ \"string\" ]"
        + "  },"
        + "  \"s3\": {"
        + "    \"0\": {"
        + "      \"@type\": [ \"number\" ]"
        + "    },"
        + "    \"1\": {"
        + "      \"@type\": [ \"number\" ]"
        + "    },"
        + "    \"2\": {"
        + "      \"@type\": [ \"number\" ]"
        + "    },"
        + "    \"3\": {"
        + "      \"@type\": [ \"number\" ]"
        + "    },"
        + "    \"@type\": [ \"Array\" ]"
        + "  }"
        + "}",

        ""
        + "/** a boolean */"
        + "var b = false;"
        + "/** a number */"
        + "var n = 4;"
        + "if (b) ; else { var s1 = '1'; }"
        + "if (!b) { var s2 = '2'; }"
        + "var s3 = [];"
        + "(function () {"
        + "  for (var i = 0; i !== n; ++i) {"
        + "    s3[i] = i;"
        + "  }"
        + "})();");
  }

  private void assertExtracted(String docJson, String srcJs) throws Exception {
    Expression expected = jsExpr(fromString(docJson));
    Jsdoc jsd = new Jsdoc(new AnnotationHandlers(mc), mc, mq);
    jsd.addSource(js(fromString(srcJs)));
    Expression actual = jsd.extract();
    if (!ParseTreeNodes.deepEquals(expected, actual)) {
      assertEquals(render(expected), render(actual));
    }
  }

  private void assertJsdocMessage(String messageText) {
    StringBuilder actual = new StringBuilder();
    for (Message msg : mq.getMessages()) {
      if (msg.getMessageType() != JsdocMessageType.RUNTIME_MESSAGE) {
        continue;
      }
      String actualText = msg.getMessageParts().get(0).toString();
      if (actualText.equals(messageText)) { return; }
      actual.append(actualText).append('\n');
    }
    fail(actual.toString().trim());
  }

  private static String navigationLink(String lhs) {
    return "javascript:navigateToApiElement(%22" + lhs + "%22)";
  }
}
