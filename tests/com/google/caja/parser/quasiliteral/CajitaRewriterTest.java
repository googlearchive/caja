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

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.ModuleEnvelope;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.SyntheticNodes;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.RhinoTestBed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import junit.framework.AssertionFailedError;

/**
 * @author ihab.awad@gmail.com
 */
public class CajitaRewriterTest extends CommonJsRewriterTestCase {

  protected Rewriter defaultCajaRewriter = new CajitaRewriter(false);

  @Override
  public void setUp() throws Exception {
    super.setUp();
    setRewriter(defaultCajaRewriter);
  }

  /**
   * Welds together a string representing the repeated pattern of
   * expected test output for assigning to an outer variable.
   *
   * @author erights@gmail.com
   */
  private static String weldSetPub(String obj,
                                   String varName,
                                   String value,
                                   String tempObj,
                                   String tempValue) {
    return
        tempObj + " = " + obj + "," +
        tempValue + " = " + value + "," +
        "    " + tempObj + "." + varName + "_canSet___ ?" +
        "    " + tempObj + "." + varName + " = " + tempValue + ":" +
        "    ___.setPub(" + tempObj + ", '" + varName + "', " + tempValue + ")";
  }

  /**
   * Welds together a string representing the repeated pattern of
   * expected test output for reading an outer variable.
   *
   * @author erights@gmail.com
   */
  private static String weldReadPub(String obj, String varName, String tempObj) {
    return weldReadPub(obj, varName, tempObj, false);
  }

  private static String weldReadPub(String obj, String varName, String tempObj, boolean flag) {
    return
        "(" +
        "(" + tempObj + " = " + obj + ")," +
        "(" + tempObj + "." + varName + "_canRead___ ?" +
        "    " + tempObj + "." + varName + ":" +
        "    ___.readPub(" + tempObj + ", '" + varName + "'" + (flag ? ", true" : "") + "))"+
        ")";
  }

  public static String weldPrelude(String name) {
    return "var " + name + " = ___.readImport(IMPORTS___, '" + name + "');";
  }

  public static String weldPrelude(String name, String permitsUsed) {
    return "var " + name + " = ___.readImport(IMPORTS___, '" + name +
           "', " + permitsUsed + ");";
  }

  public void testToString() throws Exception {
    assertConsistent(
        "var z = { toString: function () { return 'blah'; } };" +
        "try {" +
        "  '' + z;" +
        "} catch (e) {" +
        "  throw new Error('PlusPlus error: ' + e);" +
        "}");
    assertConsistent(
        "  function foo() {"
        + "  var x = 1;"
        + "  return {"
        + "    toString: function () {"
        + "      return x;"
        + "    }"
        + "  };"
        + "}"
        + "'' + (new foo);");
    rewriteAndExecute(
        "testImports.exports = {};",
        "exports.obj = {toString:function(){return '1';}};",
        "if (testImports.exports.obj.toString_canSet___) {" +
        "  fail('toString fastpath gets set.');" +
        "}"
        );
    rewriteAndExecute(
        "testImports.exports = {};",
        "exports.objMaker = function(f) { return { prop: f }; };",
        "assertThrows(function() {testImports.exports.objMaker(function(){return '1';});});"
        );
    rewriteAndExecute(
        "testImports.exports = {};",
        "function objMaker(f) {return {toString:f};}" +
        "exports.objMaker = objMaker;",
        "assertThrows(function() {testImports.exports.objMaker(function(){return '1';});});"
        );
  }

  public void testInitializeMap() throws Exception {
    assertConsistent("var zerubabel = {bobble:2, apple:1}; zerubabel.apple;");
  }

  public void testValueOf() throws Exception {
    checkFails("var a = {valueOf:1};", "The valueOf property must not be set");
    checkFails("var a={}; a.valueOf=1;", "The valueOf property must not be set");
    checkFails(
        "  function f(){}"
        + "f.prototype.valueOf=1;",
        "The valueOf property must not be set");
    checkFails(
        "var a={}; delete a.valueOf;",
        "The valueOf property must not be deleted");
    rewriteAndExecute("var a={}; assertThrows(function(){a['valueOf']=1;});");
    rewriteAndExecute(
        "var a={}; assertThrows(function(){delete a['valueOf'];});");
  }

  public void testFunctionDoesNotMaskVariable() throws Exception {
    // Regress http://code.google.com/p/google-caja/issues/detail?id=370
    // TODO(ihab.awad): Enhance test framework to allow "before" and "after"
    // un-cajoled code to be executed, then change this to a functional test.
    checkSucceeds(
        "  function boo() { return x; }"
        + "var x;",
        "  function boo() {\n" +
        "    return x;\n" +
        "  }\n" +
        "  ___.func(boo, \'boo\');"
        + ";"
        + "var x;");
  }

  public void testAssertEqualsCajoled() throws Exception {
    try {
      rewriteAndExecute("assertEquals(1, 2);");
    } catch (AssertionFailedError e) {
      return;
    }
    fail("Assertions do not work in cajoled mode");
  }

  public void testAssertThrowsCajoledNoError() throws Exception {
    rewriteAndExecute(
        "  assertThrows(function() { throw 'foo'; });");
    rewriteAndExecute(
        "  assertThrows("
        + "    function() { throw 'foo'; },"
        + "    'foo');");
  }

  public void testAssertThrowsCajoledErrorNoMsg() throws Exception {
    try {
      rewriteAndExecute("assertThrows(function() {});");
    } catch (AssertionFailedError e) {
      return;
    }
    fail("Assertions do not work in cajoled mode");
  }

  public void testAssertThrowsCajoledErrorWithMsg() throws Exception {
    try {
      rewriteAndExecute("assertThrows(function() {}, 'foo');");
    } catch (AssertionFailedError e) {
      return;
    }
    fail("Assertions do not work in cajoled mode");
  }

  public void testFreeVariables() throws Exception {
    checkSucceeds(
        "var y = x;",
        weldPrelude("x") +
        "var y = x;");
    checkSucceeds(
        "function() { var y = x; };",
        weldPrelude("x") +
        "___.frozenFunc(function() {" +
        "  var y = x;" +
        "});");
  }

  public void testConstructionWithFunction() throws Exception {
    assertConsistent(
        "  function Point() {}"
        + "var p = new Point();"
        + "(p !== undefined);");
    assertConsistent(
        "  var Point = function() {};"
        + "var p = new Point();"
        + "(p !== undefined);");
  }

  public void testReflectiveMethodInvocation() throws Exception {
    assertConsistent(
        "(function (first, second) { return 'a' + first + 'b' + second; })"
        + ".call([], 8, 9);");
    assertConsistent(
        "var a = []; [].push.call(a, 5, 6); a;");
    assertConsistent(
        "(function (a, b) { return 'a' + a + 'b' + b; }).apply([], [8, 9]);");
    assertConsistent(
        "var a = []; [].push.apply(a, [5, 6]); a;");
    assertConsistent(
        "[].sort.apply([6, 5]);");
    assertConsistent(
        "(function (first, second) { return 'a' + first + 'b' + second; })"
        + ".bind([], 8)(9);");
  }

  /**
   * Tests that <a href=
   * "http://code.google.com/p/google-caja/issues/detail?id=242"
   * >bug#242</a> is fixed.
   * <p>
   * The actual Function.bind() method used to be whitelisted and written to return a frozen
   * simple-function, allowing it to be called from all code on all functions. As a result,
   * if an <i>outer hull breach</i> occurs -- if Caja code
   * obtains a reference to a JavaScript function value not marked as Caja-callable -- then
   * that Caja code could call the whitelisted bind() on it, and then call the result,
   * causing an <i>inner hull breach</i> which threatens kernel integrity.
   */
  public void testToxicBind() throws Exception {
    rewriteAndExecute(
        "var confused = false;" +
        "testImports.keystone = function keystone() { confused = true; };",
        "assertThrows(function() {keystone.bind()();});",
        "assertFalse(confused);");
  }

  /**
   * Tests that <a href=
   * "http://code.google.com/p/google-caja/issues/detail?id=590"
   * >bug#590</a> is fixed.
   * <p>
   * As a client of an object, Caja code must only be able to directly delete
   * <i>public</i> properties of non-frozen JSON containers. Due to this bug, Caja
   * code was able to delete <i>protected</i> properties of non-frozen JSON
   * containers.
   */
  public void testBadDelete() throws Exception {
    rewriteAndExecute(
        "testImports.badContainer = {secret__: 3469};",
        "assertThrows(function() {delete badContainer['secret__'];});",
        "assertEquals(testImports.badContainer.secret__, 3469);");
    rewriteAndExecute(
        "assertThrows(function() {delete ({})['proto___'];});");
  }

  /**
   * Tests that <a href=
   * "http://code.google.com/p/google-caja/issues/detail?id=617"
   * >bug#617</a> is fixed.
   * <p>
   * The ES3 spec specifies an insane scoping rule which Firefox 2.0.0.15 "correctly"
   * implements according to the spec. The rule is that, within a named function
   * expression, the function name <i>f</i> is brought into scope by creating a new object
   * "as if by executing 'new Object()', adding an <tt>'<i>f</i>'</tt> property to this
   * object, and adding this object to the scope chain. As a result, all properties
   * inherited from <tt>Object.prototype</tt> shadow any outer lexically visible
   * declarations of those names as variable names.
   * <p>
   * Unfortunately, we're currently doing our JUnit testing using Rhino, which doesn't
   * engage in the questionable behavior of implementing specified but insane behavior.
   * As a result, the following test currently succeeds whether this bug is fixed or
   * not.
   */
  public void testNameFuncExprScoping() throws Exception {
    rewriteAndExecute(
        "assertEquals(0, function() { \n" +
        "  var propertyIsEnumerable = 0;\n" +
        "  return (function f() {\n" +
        "    return propertyIsEnumerable;\n" +
        "  })();\n" +
        "}());");
  }

  /**
   * Tests that <a href=
   * "http://code.google.com/p/google-caja/issues/detail?id=469"
   * >bug#469</a> is fixed.
   * <p>
   * The members of the <tt>caja</tt> object available to Caja code
   * (i.e., the <tt>safeCaja</tt> object) must be frozen. And if they
   * are functions, they should be marked as simple-functions. Before
   * this bug was fixed, <tt>cajita.js</tt> failed to do either.
   */
  public void testCajaPropsFrozen() throws Exception {
    rewriteAndExecute(";","0;",
    "assertTrue(___.isFunc(___.sharedImports.cajita.manifest));");
    rewriteAndExecute(";","0;",
    "assertTrue(___.isFrozen(___.sharedImports.cajita.manifest));");
  }

  /**
   * Tests that <a href=
   * "http://code.google.com/p/google-caja/issues/detail?id=292"
   * >bug#292</a> is fixed.
   * <p>
   * In anticipation of ES3.1, we should be able to index into strings
   * using indexes which are numbers or stringified numbers, so long as
   * they are in range.
   */
  public void testStringIndexing() throws Exception {
    rewriteAndExecute("assertEquals('b', 'abc'[1]);");

    // TODO(erights): This test isn't green because we haven't yet fixed the bug.
    if (false) {
      rewriteAndExecute("assertEquals('b', 'abc'['1']);");
    }
  }

  /**
   * Tests that <a href=
   * "http://code.google.com/p/google-caja/issues/detail?id=464"
   * >bug#464</a> is fixed.
   * <p>
   * Reading the apply property of a function should result in the apply
   * method as attached to that function.
   */
  public void testAttachedReflection() throws Exception {
    rewriteAndExecute(
        "function f() {}\n" +
        "f.apply;");
    // TODO(erights): Need more tests.
  }

  /**
   * Tests that <a href=
   * "http://code.google.com/p/google-caja/issues/detail?id=347"
   * >bug#347</a> is fixed.
   * <p>
   * The <tt>in</tt> operator should only test for properties visible to Caja.
   */
  public void testInVeil() throws Exception {
    rewriteAndExecute(
        "assertFalse('FROZEN___' in Object);");
  }

  ////////////////////////////////////////////////////////////////////////
  // Handling of synthetic nodes
  ////////////////////////////////////////////////////////////////////////

  public void testSyntheticIsUntouched() throws Exception {
    ParseTreeNode input = js(fromString("function foo() { this; arguments; }"));
    syntheticTree(input);
    checkSucceeds(input, input);
  }

  public void testSyntheticMemberAccess() throws Exception {
    ParseTreeNode input = js(fromString("({}).foo"));
    syntheticTree(input);
    checkSucceeds(input, js(fromString("___.initializeMap([]).foo;")));
  }

  public void testSyntheticNestedIsExpanded() throws Exception {
    ParseTreeNode innerInput = js(fromString("function foo() {}"));
    ParseTreeNode input = ParseTreeNodes.newNodeInstance(
        Block.class,
        null,
        Collections.singletonList(innerInput));
    makeSynthetic(input);
    ParseTreeNode expectedResult = js(fromString(
        "var foo; { foo = (function () {\n" +
        "             function foo$self() {\n" +
        "             }\n" +
        "             return ___.func(foo$self, \'foo\');\n" +
        "           })(); ; }"));
    checkSucceeds(input, expectedResult);
  }

  public void testSyntheticNestedFunctionIsExpanded() throws Exception {
    // This test checks that a synthetic function, as is commonly generated
    // by Caja to wrap JavaScript event handlers declared in HTML, is rewritten
    // correctly.
    Block innerBlock = js(fromString("foo().x = bar();"));
    ParseTreeNode input = new Block(java.util.Arrays.asList(
        new FunctionDeclaration(
            new Identifier("f"),
            SyntheticNodes.s(new FunctionConstructor(
                  new Identifier("f"),
                  Collections.<FormalParam>emptyList(),
                  innerBlock)))));
    // We expect the stuff in 'innerBlock' to be expanded, *but* we expect the
    // rewriter to be unaware of the enclosing function scope, so the temporary
    // variables generated by expanding 'innerBlock' spill out and get declared
    // outside the function rather than inside it.
    ParseTreeNode expectedResult = js(fromString(
        weldPrelude("bar")
        + weldPrelude("foo")
        + "var x0___;"  // Temporaries are declared up here ...
        + "var x1___;"
        + "function f() {"
        + "  "  // ... not down here!
        + weldSetPub("foo.CALL___()", "x", "bar.CALL___()", "x0___", "x1___")
        + ";}"));
    checkSucceeds(input, expectedResult);
  }

  ////////////////////////////////////////////////////////////////////////
  // Handling of nested blocks
  ////////////////////////////////////////////////////////////////////////

  public void testNestedBlockWithFunction() throws Exception {
    checkSucceeds(
        "{ function foo() {} }",
        "var foo;" +
        "{ foo = (function () {\n" +
        "             function foo$self() {\n" +
        "             }\n" +
        "             return ___.func(foo$self, \'foo\');\n" +
        "           })(); ; }");
  }

  public void testNestedBlockWithVariable() throws Exception {
    checkSucceeds(
        "{ var x = g().y; }",
        weldPrelude("g") +
         "var x0___;" +
        "{" +
         "  var x = " + weldReadPub("g.CALL___()", "y", "x0___") + ";"+
         "}");
  }

  ////////////////////////////////////////////////////////////////////////
  // Specific rules
  ////////////////////////////////////////////////////////////////////////

  public void testWith() throws Exception {
    checkFails("with (dreams || ambiguousScoping) anything.isPossible();",
               "\"with\" blocks are not allowed");
    checkFails("with (dreams || ambiguousScoping) { anything.isPossible(); }",
               "\"with\" blocks are not allowed");
  }

  public void testForInBad() throws Exception {
    checkAddsMessage(js(fromString(
        "for (var x in {}) {}")),
        RewriterMessageType.FOR_IN_NOT_IN_CAJITA,
        MessageLevel.FATAL_ERROR);
  }

  public void testTryCatch() throws Exception {
    checkAddsMessage(js(fromString(
        "try {" +
        "  throw 2;" +
        "} catch (e) {" +
        "  var e;" +
        "}")),
        MessageType.MASKING_SYMBOL,
        MessageLevel.ERROR);
    checkAddsMessage(js(fromString(
        "var e;" +
        "try {" +
        "  throw 2;" +
        "} catch (e) {" +
        "}")),
        MessageType.MASKING_SYMBOL,
        MessageLevel.ERROR);
    checkAddsMessage(js(fromString(
        "try {} catch (x__) { }")),
        RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE);
    // TODO(ihab.awad): The below should throw MessageType.MASKING_SYMBOL at
    // MessageLevel.ERROR. See bug #313. For the moment, we merely check that
    // it cajoles to something secure.
    checkSucceeds(
        "try {" +
        "  g[0];" +
        "  e;" +
        "  g[1];" +
        "} catch (e) {" +
        "  g[2];" +
        "  e;" +
        "  g[3];" +
        "}",
        weldPrelude("e") +
        weldPrelude("g") +
        "try {" +
        "  ___.readPub(g, 0);" +
        "  e;" +
        "  ___.readPub(g, 1);" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    ___.readPub(g, 2);" +
        "    e;" +
        "    ___.readPub(g, 3);" +
        "  }" +
        "}");
    rewriteAndExecute(
        "var handled = false;" +
        "try {" +
        "  throw null;" +
        "} catch (ex) {" +
        "  assertEquals(null, ex);" +  // Right value in ex.
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");  // Control reached and left the catch block.
    rewriteAndExecute(
        "var handled = false;" +
        "try {" +
        "  throw undefined;" +
        "} catch (ex) {" +
        "  assertEquals(undefined, ex);" +
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");
    rewriteAndExecute(
        "var handled = false;" +
        "try {" +
        "  throw true;" +
        "} catch (ex) {" +
        "  assertEquals(true, ex);" +
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");
    rewriteAndExecute(
        "var handled = false;" +
        "try {" +
        "  throw 37639105;" +
        "} catch (ex) {" +
        "  assertEquals(37639105, ex);" +
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");
    rewriteAndExecute(
        "var handled = false;" +
        "try {" +
        "  throw 'panic';" +
        "} catch (ex) {" +
        "  assertEquals('panic', ex);" +
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");
    rewriteAndExecute(
        "var handled = false;" +
        "try {" +
        "  throw new Error('hello');" +
        "} catch (ex) {" +
        "  assertEquals('hello', ex.message);" +
        "  assertEquals('Error', ex.name);" +
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");
    rewriteAndExecute(
        "var handled = false;" +
        "try {" +
        "  throw function () { throw 'should not be called'; };" +
        "} catch (ex) {" +
        "  assertEquals(undefined, ex);" +
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");
    rewriteAndExecute(
        "var handled = false;" +
        "try {" +
        "  throw { toString: function () { return 'hiya'; }, y: 4 };" +
        "} catch (ex) {" +
        "  assertEquals('string', typeof ex);" +
        "  assertEquals('hiya', ex);" +
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");
    rewriteAndExecute(
        "var handled = false;" +
        "try {" +
        "  throw { toString: function () { throw new Error(); } };" +
        "} catch (ex) {" +
        "  assertEquals(undefined, ex);" +
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");
  }

  public void testTryCatchFinally() throws Exception {
    checkAddsMessage(js(fromString(
        "try {" +
        "} catch (e) {" +
        "  var e;" +
        "} finally {" +
        "}")),
        MessageType.MASKING_SYMBOL,
        MessageLevel.ERROR);
    checkAddsMessage(js(fromString(
        "var e;" +
        "try {" +
        "} catch (e) {" +
        "} finally {" +
        "}")),
        MessageType.MASKING_SYMBOL,
        MessageLevel.ERROR);
    checkAddsMessage(js(fromString(
        "try {} catch (x__) { } finally { }")),
        RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE);
    checkSucceeds(
        "try {" +
        "  g[0];" +
        "  e;" +
        "  g[1];" +
        "} catch (e) {" +
        "  g[2];" +
        "  e;" +
        "  g[3];" +
        "} finally {" +
        "  g[4];" +
        "  e;" +
        "  g[5];" +
        "}",
        weldPrelude("e") +
        weldPrelude("g") +
        "try {" +
        "  ___.readPub(g, 0);" +
        "  e;" +
        "  ___.readPub(g, 1);" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    ___.readPub(g, 2);" +
        "    e;" +
        "    ___.readPub(g, 3);" +
        "  }" +
        "} finally {" +
        "    ___.readPub(g, 4);" +
        "    e;" +
        "    ___.readPub(g, 5);" +
        "}");
  }

  public void testTryFinally() throws Exception {
    assertConsistent(
        "var out = 0;" +
        "try {" +
        "  try {" +
        "    throw 2;" +
        "  } finally {" +
        "    out = 1;" +
        "  }" +
        "  out = 2;" +
        "} catch (e) {" +
        "}" +
        "out;");
    checkSucceeds(
        "try {" +
        "  g[0];" +
        "  e;" +
        "  g[1];" +
        "} finally {" +
        "  g[2];" +
        "  e;" +
        "  g[3];" +
        "}",
        weldPrelude("e") +
        weldPrelude("g") +
        "try {" +
        "  ___.readPub(g, 0);" +
        "  e;" +
        "  ___.readPub(g, 1);" +
        "} finally {" +
        "  ___.readPub(g, 2);" +
        "  e;" +
        "  ___.readPub(g, 3);" +
        "}");
  }

  public void testVarArgs() throws Exception {
    checkSucceeds(
        "var p;" +
        "var foo = function() {" +
        "  p = arguments;" +
        "};",
        "var p;" +
        "var foo = ___.frozenFunc(function() {" +
        "  var a___ = ___.args(arguments);" +
        "  p = a___;" +
        "});");
  }

  public void testVarThisBad() throws Exception {
    checkAddsMessage(
        js(fromString("var x = this;")),
        RewriterMessageType.THIS_NOT_IN_CAJITA,
        MessageLevel.FATAL_ERROR);
    checkAddsMessage(
        js(fromString("this = 42;")),
        RewriterMessageType.THIS_NOT_IN_CAJITA,
        MessageLevel.FATAL_ERROR);
    checkAddsMessage(
        js(fromString("function foo() { var x = this; }")),
        RewriterMessageType.THIS_NOT_IN_CAJITA,
        MessageLevel.FATAL_ERROR);
    checkAddsMessage(
        js(fromString("function foo() { this = 42; }")),
        RewriterMessageType.THIS_NOT_IN_CAJITA,
        MessageLevel.FATAL_ERROR);
  }

  public void testVarBadSuffix() throws Exception {
    checkFails(
        "function() { foo__; };",
        "Variables cannot end in \"__\"");
    // Make sure *single* underscore is okay
    checkSucceeds(
        "function() { var foo_ = 3; };",
        "___.frozenFunc(function() { var foo_ = 3; });");
  }

  public void testVarBadSuffixDeclaration() throws Exception {
    checkFails(
        "function foo__() { }",
        "Variables cannot end in \"__\"");
    checkFails(
        "var foo__ = 3;",
        "Variables cannot end in \"__\"");
    checkFails(
        "var foo__;",
        "Variables cannot end in \"__\"");
    checkFails(
        "function() { function foo__() { } };",
        "Variables cannot end in \"__\"");
    checkFails(
        "function() { var foo__ = 3; };",
        "Variables cannot end in \"__\"");
    checkFails(
        "function() { var foo__; };",
        "Variables cannot end in \"__\"");
  }

  public void testVarFuncFreeze() throws Exception {
    // We can cajole and refer to a function
    rewriteAndExecute(
        "function foo() {};" +
        "foo();");
    // We can assign a dotted property of a variable
    rewriteAndExecute(
        "var foo = {};" +
        "foo.x = 3;" +
        "assertEquals(foo.x, 3);");
    // We cannot assign to a function variable
    assertAddsMessage(
        "function foo() {}" +
        "foo = 3;",
        RewriterMessageType.CANNOT_ASSIGN_TO_FUNCTION_NAME,
        MessageLevel.FATAL_ERROR);
    // We cannot assign to a member of an aliased simple function
    // since it is frozen.
    rewriteAndExecute(
        "assertThrows(function() {" +
        "  function foo() {};" +
        "  var bar = foo;" +
        "  bar.x = 3;" +
        "});");
  }

  public void testVarGlobal() throws Exception {
    checkSucceeds(
        "foo;",
        weldPrelude("foo") +
        "foo;");
    checkSucceeds(
        "function() {" +
        "  foo;" +
        "};",
        weldPrelude("foo") +
        "___.frozenFunc(function() {" +
        "  foo;" +
        "});");
    checkSucceeds(
        "function() {" +
        "  var foo;" +
        "  foo;" +
        "};",
        "___.frozenFunc(function() {" +
        "  var foo;" +
        "  foo;" +
        "});");
  }

  public void testVarDefault() throws Exception {
    String unchanged =
        "var x = 3;" +
        "if (x) { }" +
        "x + 3;" +
        "var y = undefined;";
    checkSucceeds(
        "function() {" +
        "  " + unchanged +
        "};",
        "var undefined = ___.readImport(IMPORTS___, 'undefined', {});" +
        "___.frozenFunc(function() {" +
        "  " + unchanged +
        "});");
  }

  public void testReadBadSuffix() throws Exception {
    checkFails(
        "x.y__;",
        "Properties cannot end in \"__\"");
  }

  public void testReadBadPrototype() throws Exception {
    checkAddsMessage(
        js(fromString("function foo() {} foo.prototype;")),
        RewriterMessageType.PROTOTYPICAL_INHERITANCE_NOT_IN_CAJITA);
    checkAddsMessage(
        js(fromString("var q = function foo() { foo.prototype; };")),
        RewriterMessageType.PROTOTYPICAL_INHERITANCE_NOT_IN_CAJITA);
  }

  public void testReadPublic() throws Exception {
    checkSucceeds(
        "var p;" +
        "p = foo().p;",
        weldPrelude("foo") +
        "var x0___;" +
        "var p;" +
        "p = " + weldReadPub("foo.CALL___()", "p", "x0___") + ";");
  }

  public void testReadIndexPublic() throws Exception {
    checkSucceeds(
        "var p, q;" +
        "p = q[3];",
        "var p, q;" +
        "p = ___.readPub(q, 3);");
  }

  public void testSetBadAssignToFunctionName() throws Exception {
    checkAddsMessage(js(fromString(
        "  function foo() {};"
        + "foo = 3;")),
        RewriterMessageType.CANNOT_ASSIGN_TO_FUNCTION_NAME);
    checkAddsMessage(js(fromString(
        "  function foo() {};"
        + "foo += 3;")),
        RewriterMessageType.CANNOT_ASSIGN_TO_FUNCTION_NAME);
    checkAddsMessage(js(fromString(
        "  function foo() {};"
        + "foo++;")),
        RewriterMessageType.CANNOT_ASSIGN_TO_FUNCTION_NAME);
    checkAddsMessage(js(fromString(
        "  var x = function foo() {"
        + "  foo = 3;"
        + "};")),
        RewriterMessageType.CANNOT_ASSIGN_TO_FUNCTION_NAME);
    checkAddsMessage(js(fromString(
        "  var x = function foo() {"
        + "  foo += 3;"
        + "};")),
        RewriterMessageType.CANNOT_ASSIGN_TO_FUNCTION_NAME);
    checkAddsMessage(js(fromString(
        "  var x = function foo() {"
        + "  foo++;"
        + "};")),
        RewriterMessageType.CANNOT_ASSIGN_TO_FUNCTION_NAME);
  }

  public void testSetBadThis() throws Exception {
    checkAddsMessage(
        js(fromString("function f() { this = 3; }")),
        RewriterMessageType.THIS_NOT_IN_CAJITA);
  }

  public void testSetBadFreeVariable() throws Exception {
    checkAddsMessage(
        js(fromString("Array = function () { return [] };")),
        RewriterMessageType.CANNOT_MASK_IDENTIFIER);
    checkAddsMessage(
        js(fromString("x = 1;")),
        RewriterMessageType.CANNOT_ASSIGN_TO_FREE_VARIABLE);
  }

  public void testSetBadSuffix() throws Exception {
    checkFails(
        "x.y__ = z;",
        "Properties cannot end in \"__\"");
  }

  public void testSetBadPrototype() throws Exception {
    checkAddsMessage(
        js(fromString("function foo() {} foo.prototype = 3;")),
        RewriterMessageType.PROTOTYPICAL_INHERITANCE_NOT_IN_CAJITA);
    checkAddsMessage(
        js(fromString("var q = function foo() { foo.prototype = 3; };")),
        RewriterMessageType.PROTOTYPICAL_INHERITANCE_NOT_IN_CAJITA);
  }

  public void testSetPublic() throws Exception {
    checkSucceeds(
        "x().p = g[0];",
        weldPrelude("g") +
        weldPrelude("x") +
        "var x0___;" +
        "var x1___;" +
        weldSetPub("x.CALL___()", "p", "___.readPub(g, 0)", "x0___", "x1___") +
        ";");
  }

  public void testSetIndexPublic() throws Exception {
    checkSucceeds(
        "g[0][g[1]] = g[2];",
        weldPrelude("g") +
        "___.setPub(___.readPub(g, 0), ___.readPub(g, 1), ___.readPub(g, 2));");
  }

  public void testSetBadInitialize() throws Exception {
    checkFails(
        "var x__ = 3;",
        "Variables cannot end in \"__\"");
  }

  public void testSetInitialize() throws Exception {
    checkSucceeds(
        "var v = g[0];",
        weldPrelude("g") +
        "var v = ___.readPub(g, 0);");
  }

  public void testSetBadDeclare() throws Exception {
    checkFails(
        "var x__;",
        "Variables cannot end in \"__\"");
  }

  public  void testSetDeclare() throws Exception {
    checkSucceeds(
        "var v;",
        "var v;");
    checkSucceeds(
        "try { } catch (e) { var v; }",
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    var v;" +
        "  }" +
        "}");
  }

  public void testSetVar() throws Exception {
    checkAddsMessage(
        js(fromString("try {} catch (x__) { x__ = 3; }")),
        RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE);
    checkSucceeds(
        "var x;" +
        "x = g[0];",
        weldPrelude("g") +
        "var x;" +
        "x = ___.readPub(g, 0);");
  }

  public void testSetReadModifyWriteLocalVar() throws Exception {
    checkFails("x__ *= 2;", "");
    checkFails("x *= y__;", "");
    checkSucceeds(
        "var x; x += g[0];",
        weldPrelude("g")
        + "var x; x = x + ___.readPub(g, 0);");
    checkSucceeds(
        "myArray().key += 1;",
        weldPrelude("myArray")
        + "var x0___;"
        + "x0___ = myArray.CALL___(),"
        + "___.setPub(x0___, 'key',"
        + "           ___.readPub(x0___, 'key') + 1);");
    checkSucceeds(
        "myArray()[myKey()] += 1;",
        weldPrelude("myArray")
        + weldPrelude("myKey")
        + "var x0___;"
        + "var x1___;"
        + "x0___ = myArray.CALL___(),"
        + "x1___ = myKey.CALL___(),"
        + "___.setPub(x0___, x1___,"
        + "           ___.readPub(x0___, x1___) + 1);");
    checkSucceeds(  // Local reference need not be assigned to a temp.
        "(function (myKey) { myArray()[myKey] += 1; });",
        weldPrelude("myArray")
        + "___.frozenFunc(function (myKey) {"
        + "  var x0___;"
        + "  x0___ = myArray.CALL___(),"
        + "  ___.setPub(x0___, myKey,"
        + "             ___.readPub(x0___, myKey) + 1);"
        + "});");

    assertConsistent("var x = 3; x *= 2;");
    assertConsistent("var x = 1; x += 7;");
    assertConsistent("var x = 1; x /= '2';");
    assertConsistent("var o = { x: 'a' }; o.x += 'b'; o;");

    EnumSet<Operator> ops = EnumSet.of(
        Operator.ASSIGN_MUL,
        Operator.ASSIGN_DIV,
        Operator.ASSIGN_MOD,
        Operator.ASSIGN_SUM,
        Operator.ASSIGN_SUB,
        Operator.ASSIGN_LSH,
        Operator.ASSIGN_RSH,
        Operator.ASSIGN_USH,
        Operator.ASSIGN_AND,
        Operator.ASSIGN_XOR,
        Operator.ASSIGN_OR
        );
    for (Operator op : ops) {
      checkSucceeds(
          "var x; x " + op.getSymbol() + " g[0];",
          weldPrelude("g")
          + "var x;"
          + "x = x " + op.getAssignmentDelegate().getSymbol()
              + "___.readPub(g, 0);");
    }
  }

  public void testSetIncrDecr() throws Exception {
    checkFails("x__--;", "");
    checkSucceeds(
        "g[0]++;",
        weldPrelude("g") +
        "var x0___;" +
        "var x1___;" +
        "x0___ = g," +
        "x1___ = +___.readPub(x0___, 0)," +
        "___.setPub(x0___, 0, x1___ + 1)," +
        "x1___;");
    checkSucceeds(
        "g[0]--;",
        weldPrelude("g") +
        "var x0___;" +
        "var x1___;" +
        "x0___ = g," +
        "x1___ = +___.readPub(x0___, 0)," +
        "___.setPub(x0___, 0, x1___ - 1)," +
        "x1___;");
    checkSucceeds(
        "++g[0];",
        weldPrelude("g") +
        "var x0___;" +
        "x0___ = g," +
        "___.setPub(x0___, 0, ___.readPub(x0___, 0) - -1);");

    assertConsistent(
        "var x = 2;" +
        "var arr = [--x, x, x--, x, ++x, x, x++, x];" +
        "assertEquals('1,1,1,0,1,1,1,2', arr.join(','));" +
        "arr;");
    assertConsistent(
        "var x = '2';" +
        "var arr = [--x, x, x--, x, ++x, x, x++, x];" +
        "assertEquals('1,1,1,0,1,1,1,2', arr.join(','));" +
        "arr;");
  }

  public void testSetIncrDecrOnLocals() throws Exception {
    checkFails("++x__;", "");
    checkSucceeds(
        "(function (x, y) { return [x--, --x, y++, ++y]; });",
        "___.frozenFunc(" +
        "  function (x, y) { return [x--, --x, y++, ++y]; });");

    assertConsistent(
        "(function () {" +
        "  var x = 2;" +
        "  var arr = [--x, x, x--, x, ++x, x, x++, x];" +
        "  assertEquals('1,1,1,0,1,1,1,2', arr.join(','));" +
        "  return arr;" +
        "})();");
  }

  public void testSetIncrDecrOfComplexLValues() throws Exception {
    checkFails("arr[x__]--;", "Variables cannot end in \"__\"");
    checkFails("arr__[x]--;", "Variables cannot end in \"__\"");

    checkSucceeds(
        "o.x++;",
        weldPrelude("o") +
        "var x0___;" +
        "var x1___;" +
        "x0___ = o," +
        "x1___ = +___.readPub(x0___, 'x')," +
        "___.setPub(x0___, 'x', x1___ + 1)," +
        "x1___;");

    assertConsistent(
        "(function () {" +
        "  var o = { x: 2 };" +
        "  var arr = [--o.x, o.x, o.x--, o.x, ++o.x, o.x, o.x++, o.x];" +
        "  assertEquals('1,1,1,0,1,1,1,2', arr.join(','));" +
        "  return arr;" +
        "})();");
  }

  public void testSetIncrDecrOrderOfAssignment() throws Exception {
    assertConsistent(
        "(function () {" +
        "  var arrs = [1, 2];" +
        "  var j = 0;" +
        "  arrs[++j] *= ++j;" +
        "  assertEquals(2, j);" +
        "  assertEquals(1, arrs[0]);" +
        "  assertEquals(4, arrs[1]);" +
        "  return arrs;" +
        "})();");
    assertConsistent(
        "(function () {" +
        "  var foo = (function () {" +
        "               var k = 0;" +
        "               return function () {" +
        "                 switch (k++) {" +
        "                   case 0: return [10, 20, 30];" +
        "                   case 1: return 1;" +
        "                   case 2: return 2;" +
        "                   default: throw new Error(k);" +
        "                 }" +
        "               };" +
        "             })();" +
        "  return foo()[foo()] -= foo();" +
        "})();"
        );
  }

  public void testNewCallledCtor() throws Exception {
    checkSucceeds(
        "new Date();",
        weldPrelude("Date", "{}")
        + "___.construct(Date, []);");
  }

  public void testNewCalllessCtor() throws Exception {
    checkSucceeds(
        "(new Date);",
        weldPrelude("Date", "{}")
        + "___.construct(Date, []);");
  }

  public void testDeletePub() throws Exception {
    checkFails("delete x.foo___;", "Properties cannot end in \"__\"");
    checkSucceeds(
        "delete foo()[bar()];",
        weldPrelude("bar") +
        weldPrelude("foo") +
        "___.deletePub(foo.CALL___()," +
        "              bar.CALL___());");
    checkSucceeds(
        "delete foo().bar;",
        weldPrelude("foo") +
        "___.deletePub(foo.CALL___(), 'bar');");
    assertConsistent(
        "(function() {" +
        "  var o = { x: 3, y: 4 };" +    // A JSON object.
        "  function ptStr(o) { return '(' + o.x + ',' + o.y + ')'; }" +
        "  var history = [ptStr(o)];" +  // Record state before deletion.
        "  delete o.y;" +                // Delete
        "  delete o.z;" +                // Not present.  Delete a no-op
        "  history.push(ptStr(o));" +    // Record state after deletion.
        "  return history.toString();" +
        "})();");
    assertConsistent(
        "var alert = 'a';" +
        "var o = { a: 1 };" +
        "delete o[alert];" +
        "assertEquals(undefined, o.a);" +
        "o;");
  }

  public void testDeleteFails() throws Exception {
    assertConsistent(
        "var status;" +
        "try {" +
        "  if (delete [].length) {" +
        "    status = 'FAILED';" +  // Passing is not ok.
        "  } else {" +
        "    status = 'PASSED';" +  // Ok to return false
        "  }" +
        "} catch (e) {" +
        "  status = 'PASSED';" +  // Ok to fail with an exception
        "}" +
        "status;");
  }

  public void testDeleteNonLvalue() throws Exception {
    checkFails("delete 4;", "Invalid operand to delete");
  }

  public void testCallPublic() throws Exception {
    checkSucceeds(
        "g[0].m(g[1], g[2]);",
        weldPrelude("g") +
        "var x0___;" +
        "var x1___;" +
        "var x2___;" +
        "x0___ = ___.readPub(g, 0)," +
        "x1___ = ___.readPub(g, 1), x2___ = ___.readPub(g, 2)," +
        "x0___.m_canCall___ ?" +
        "  x0___.m(x1___, x2___) :" +
        "  ___.callPub(x0___, 'm', [x1___, x2___]);");
  }

  public void testCallIndexPublic() throws Exception {
    checkSucceeds(
        "g[0][g[1]](g[2], g[3]);",
        weldPrelude("g") +
        "___.callPub(" +
        "    ___.readPub(g, 0)," +
        "    ___.readPub(g, 1)," +
        "    [___.readPub(g, 2), ___.readPub(g, 3)]);");
  }

  public void testCallFunc() throws Exception {
    checkSucceeds(
        "g(g[1], g[2]);",
        weldPrelude("g") +
        "g.CALL___" +
        "     (___.readPub(g, 1), ___.readPub(g, 2));");
  }

  public void testPermittedCall() throws Exception {
    // TODO(ihab.awad): Once permit templates can be loaded dynamically, create
    // one here for testing rather than rely on the Valija permits.
    String fixture =
        "  var x = 0;"
        + "var callPubCalled = false;"
        + "var origCallPub = ___.callPub;"
        + "___.callPub = function(obj, name, args) {"
        + "  if (name === 'dis') { callPubCalled = true; }"
        + "  origCallPub.call(___, obj, name, args);"
        + "};"
        + "testImports.$v = ___.primFreeze({"
        + "  dis: ___.frozenFunc(function(n) { x = n; })"
        + "});";
    rewriteAndExecute(
        fixture,
        "  $v.dis(42);",
        "  assertFalse(callPubCalled);"
        + "assertEquals(42, x);");
    rewriteAndExecute(
        fixture,
        "  (function() {"
        + "  var $v = { dis: function(x) {} };"
        + "  $v.dis(42);"
        + "})();",
        "  assertTrue(callPubCalled);"
        + "assertEquals(0, x);");
  }

  public void testFuncAnonSimple() throws Exception {
    // TODO(ihab.awad): The below test is not as complete as it should be
    // since it does not test the "@stmts*" substitution in the rule
    checkSucceeds(
        "function(x, y) { x = arguments; y = g[0]; };",
        weldPrelude("g") +
        "___.frozenFunc(function(x, y) {" +
        "  var a___ = ___.args(arguments);" +
        "  x = a___;" +
        "  y = ___.readPub(g, 0);" +
        "});");
    rewriteAndExecute(
        "(function () {" +
        "  var foo = function () {};" +
        "  foo();" +
        "  try {" +
        "    foo.x = 3;" +
        "  } catch (e) { return; }" +
        "  fail('mutate frozen function');" +
        "})();");
    assertConsistent(
        "var foo = (function () {" +
        "             function foo() {};" +
        "             foo.x = 3;" +
        "             return foo;" +
        "           })();" +
        "foo();" +
        "foo.x;");
  }

  public void testFuncNamedSimpleDecl() throws Exception {
    rewriteAndExecute(
        "  assertThrows(function() {"
        + "  (function foo() { foo.x = 3; })();"
        + "});");
    rewriteAndExecute(
        "  assertThrows(function() {"
        + "  function foo() { foo.x = 3; }"
        + "  foo();"
        + "});");
    rewriteAndExecute(
        "  assertThrows(function() {"
        + "  var foo = function() {};"
        + "  foo.x = 3;"
        + "});");
    rewriteAndExecute(
        "  function foo() {}"
        + "foo.x = 3;");
    checkSucceeds(
        "function() {" +
        "  function foo(x, y) {" +
        "    x = arguments;" +
        "    y = g[0];" +
        "    return foo(x - 1, y - 1);" +
        "  }" +
        "};",
        weldPrelude("g") +
        "___.frozenFunc(function() {" +
        "   function foo(x, y) {\n" +
        "                           var a___ = ___.args(arguments);\n" +
        "                           x = a___;\n" +
        "                           y = ___.readPub(g, 0);\n" +
        "                           return foo.CALL___(x - 1, y - 1);\n" +
        "                         }\n" +
        "                         ___.func(foo, \'foo\');" +
        "  ;"+
        "});");
    checkSucceeds(
        "function foo(x, y ) {" +
        "  return foo(x - 1, y - 1);" +
        "}",
        "  function foo(x, y) {\n" +
        "    return foo.CALL___(x - 1, y - 1);\n" +
        "  }\n" +
        "  ___.func(foo, \'foo\');" +
        ";");
    rewriteAndExecute(
        "(function () {" +
        "  function foo() {}" +
        "  foo();" +
        "  try {" +
        "    foo.x = 3;" +
        "  } catch (e) { return; }" +
        "  fail('mutated frozen function');" +
        "})();");
    assertConsistent(
        "function foo() {}" +
        "foo.x = 3;" +
        "foo();" +
        "foo.x;");
    rewriteAndExecute(
        "  function f_() { return 31415; }"
        + "var x = f_();"
        + "assertEquals(x, 31415);");
  }

  public void testFuncNamedSimpleValue() throws Exception {
    checkSucceeds(
        "var f = function foo(x, y) {" +
        "  x = arguments;" +
        "  y = z;" +
        "  return foo(x - 1, y - 1);" +
        "};",
        weldPrelude("z") +
        "  var f = function() {" +
        "      function foo(x, y) {" +
        "        var a___ = ___.args(arguments);" +
        "        x = a___;" +
        "        y = z;" +
        "        return foo.CALL___(x - 1, y - 1);" +
        "      }" +
        "      return ___.frozenFunc(foo, 'foo');" +
        "    }();");
    checkSucceeds(
        "var bar = function foo_(x, y ) {" +
        "  return foo_(x - 1, y - 1);" +
        "};",
        "var bar = function() {" +
        "  function foo_(x, y) {" +
        "    return foo_.CALL___(x - 1, y - 1);" +
        "  }" +
        "  return ___.frozenFunc(foo_, 'foo_');" +
        "}();");
  }

  public void testMaskingFunction () throws Exception {
    assertAddsMessage(
        "function Goo() { function Goo() {} }",
        MessageType.SYMBOL_REDEFINED,
        MessageLevel.ERROR );
    assertAddsMessage(
        "function Goo() { var Goo = 1; }",
        MessageType.MASKING_SYMBOL,
        MessageLevel.LINT );
    assertMessageNotPresent(
        "function Goo() { this.x = 1; }",
        MessageType.MASKING_SYMBOL );
  }

  public void testMapEmpty() throws Exception {
    checkSucceeds(
        "var f = {};",
        "var f = ___.initializeMap([]);");
  }

  public void testMapBadKeySuffix() throws Exception {
    checkAddsMessage(
        js(fromString("var o = { x__: 3 };")),
        RewriterMessageType.PROPERTIES_CANNOT_END_IN_DOUBLE_UNDERSCORE);
  }

  public void testMapNonEmpty() throws Exception {
    checkSucceeds(
        "var o = { k0: g().x, k1: g().y };",
        weldPrelude("g") +
        "var x0___;" +
        "var x1___;" +
        "var o = ___.initializeMap(" +
        "    [ 'k0', " + weldReadPub("g.CALL___()", "x", "x0___") + ", " +
        "      'k1', " + weldReadPub("g.CALL___()", "y", "x1___") + " ]);");
    // Ensure that calling an untamed function throws
    rewriteAndExecute(
        "testImports.f = function() {};",
        "assertThrows(function() { f(); });",
        ";");
    // Ensure that calling a tamed function in an object literal works
    rewriteAndExecute(
        "  var f = function() {};"
        + "var m = { f : f };"
        + "m.f();");
    // Ensure that putting an untamed function into an object literal
    // causes an exception.
    rewriteAndExecute(
        "testImports.f = function() {};",
        "assertThrows(function(){({ isPrototypeOf : f });});",
        ";");
  }

  public void testOtherInstanceof() throws Exception {
    checkSucceeds(
        "function foo() {}" +
        "g[0] instanceof foo;",
        weldPrelude("g") +
        "function foo() {\n" +
        "  }\n" +
        "  ___.func(foo, \'foo\');" +
        ";" +
        "___.readPub(g, 0) instanceof ___.primFreeze(foo);");
    checkSucceeds(
        "g[0] instanceof Object;",
        weldPrelude("Object") +
        weldPrelude("g") +
        "___.readPub(g, 0) instanceof Object;");

    assertConsistent("[ (({}) instanceof Object)," +
                     "  ((new Date) instanceof Date)," +
                     "  (({}) instanceof Date)" +
                     "];");
    assertConsistent("function foo() {}; (new foo) instanceof foo;");
    assertConsistent("function foo() {}; !(({}) instanceof foo);");
  }

  public void testOtherTypeof() throws Exception {
    checkSucceeds(
        "typeof g[0];",
        weldPrelude("g") +
        "___.typeOf(___.readPub(g, 0));");
    checkFails("typeof ___;", "Variables cannot end in \"__\"");
  }

  public void testLabeledStatement() throws Exception {
    checkFails("IMPORTS___: 1;", "Labels cannot end in \"__\"");
    checkSucceeds("foo: 1;", "foo: 1;");
    assertConsistent(
        "var k = 0;" +
        "a: for (var i = 0; i < 10; ++i) {" +
        "  b: for (var j = 0; j < 10; ++j) {" +
        "    if (++k > 5) break a;" +
        "  }" +
        "}" +
        "k;");
    assertConsistent(
        "var k = 0;" +
        "a: for (var i = 0; i < 10; ++i) {" +
        "  b: for (var j = 0; j < 10; ++j) {" +
        "    if (++k > 5) break b;" +
        "  }" +
        "}" +
        "k;");
  }

  public void testRegexLiteral() throws Exception {
    checkAddsMessage(
        js(fromString("/x/;")),
        RewriterMessageType.REGEX_LITERALS_NOT_IN_CAJITA);
    checkAddsMessage(
        js(fromString("var y = /x/;")),
        RewriterMessageType.REGEX_LITERALS_NOT_IN_CAJITA);
  }

  public void testOtherSpecialOp() throws Exception {
    checkSucceeds("void 0;", "void 0;");
    checkSucceeds("void g();",
                  weldPrelude("g") +
                  "void g.CALL___();");
    checkSucceeds("g[0], g[1];",
                  weldPrelude("g") +
                  "___.readPub(g, 0), ___.readPub(g, 1);");
  }

  public void testMultiDeclaration2() throws Exception {
    // 'var' in global scope, part of a block
    checkSucceeds(
        "var x, y;",
        "var x, y;");
    checkSucceeds(
        "var x = g[0], y = g[1];",
        weldPrelude("g") +
        "var x = ___.readPub(g, 0), y = ___.readPub(g, 1);");
    checkSucceeds(
        "var x, y = g[0];",
        weldPrelude("g") +
        "var x, y = ___.readPub(g, 0);");
    // 'var' in global scope, 'for' statement
    checkSucceeds(
        "for (var x, y; ; ) {}",
        "for (var x, y; ; ) {}");
    checkSucceeds(
        "for (var x = g[0], y = g[1]; ; ) {}",
        weldPrelude("g") +
        "for (var x = ___.readPub(g, 0), y = ___.readPub(g, 1); ; ) {}");
    checkSucceeds(
        "for (var x, y = g[0]; ; ) {}",
        weldPrelude("g") +
        "for (var x, y = ___.readPub(g, 0); ; ) {}");
    // 'var' in global scope, part of a block
    checkSucceeds(
        "function() {" +
        "  var x, y;" +
        "};",
        "___.frozenFunc(function() {" +
        "  var x, y;" +
        "});");
    checkSucceeds(
        "function() {" +
        "  var x = g[0], y = g[1];" +
        "};",
        weldPrelude("g") +
        "___.frozenFunc(function() {" +
        "  var x = ___.readPub(g, 0), y = ___.readPub(g, 1);" +
        "});");
    checkSucceeds(
        "function() {" +
        "  var x, y = g[0];" +
        "};",
        weldPrelude("g") +
        "___.frozenFunc(function() {" +
        "  var x, y = ___.readPub(g, 0);" +
        "});");
    // 'var' in global scope, 'for' statement
    checkSucceeds(
        "function() {" +
        "  for (var x, y; ; ) {}" +
        "};",
        "___.frozenFunc(function() {" +
        "  for (var x, y; ; ) {}" +
        "});");
    checkSucceeds(
        "function() {" +
        "  for (var x = g[0], y = g[1]; ; ) {}" +
        "};",
        weldPrelude("g") +
        "___.frozenFunc(function() {" +
        "  for (var x = ___.readPub(g, 0), " +
        "           y = ___.readPub(g, 1); ; ) {}" +
        "});");
    checkSucceeds(
        "function() {" +
        "  for (var x, y = g[0]; ; ) {}" +
        "};",
        weldPrelude("g") +
        "___.frozenFunc(function() {" +
        "  for (var x, y = ___.readPub(g, 0); ; ) {}" +
        "});");
    assertConsistent(
        "var arr = [1, 2, 3], k = -1;" +
        "(function () {" +
        "  var a = arr[++k], b = arr[++k], c = arr[++k];" +
        "  return [a, b, c];" +
        "})();");
    // Check exceptions on read of uninitialized variables.
    assertConsistent(
        "(function () {" +
        "  var a = [];" +
        "  for (var i = 0, j = 10; i < j; ++i) { a.push(i); }" +
        "  return a;" +
        "})();");
    assertConsistent(
        "var a = [];" +
        "for (var i = 0, j = 10; i < j; ++i) { a.push(i); }" +
        "a;");
  }

  public void testRecurseParseTreeNodeContainer() throws Exception {
    // Tested implicitly by other cases
  }

  public void testRecurseArrayConstructor() throws Exception {
    checkSucceeds(
        "var foo = [ g[0], g[1] ];",
        weldPrelude("g") +
        "var foo = [___.readPub(g, 0), ___.readPub(g, 1)];");
  }

  public void testRecurseBlock() throws Exception {
    // Tested implicitly by other cases
  }

  public void testRecurseBreakStmt() throws Exception {
    checkSucceeds(
        "while (true) { break; }",
        "while (true) { break; }");
  }

  public void testRecurseCaseStmt() throws Exception {
    checkSucceeds(
        "switch (g[0]) { case 1: break; }",
        weldPrelude("g") +
        "switch (___.readPub(g, 0)) { case 1: break; }");
  }

  public void testRecurseConditional() throws Exception {
    checkSucceeds(
        "if (g[0] === g[1]) {" +
        "  g[2];" +
        "} else if (g[3] === g[4]) {" +
        "  g[5];" +
        "} else {" +
        "  g[6];" +
        "}",
        weldPrelude("g") +
        "if (___.readPub(g, 0) === ___.readPub(g, 1)) {" +
        "  ___.readPub(g, 2);" +
        "} else if (___.readPub(g, 3) === ___.readPub(g, 4)) {" +
        "  ___.readPub(g, 5);" +
        "} else {" +
        "  ___.readPub(g, 6);" +
        "}");
  }

  public void testRecurseContinueStmt() throws Exception {
    checkSucceeds(
        "while (true) { continue; }",
        "while (true) { continue; }");
  }

  public void testRecurseDebuggerStmt() throws Exception {
    checkSucceeds("debugger;", "debugger;");
  }

  public void testRecurseDefaultCaseStmt() throws Exception {
    checkSucceeds(
        "switch (g[0]) { default: break; }",
        weldPrelude("g") +
        "switch(___.readPub(g, 0)) { default: break; }");
  }

  public void testRecurseExpressionStmt() throws Exception {
    // Tested implicitly by other cases
  }

  public void testRecurseIdentifier() throws Exception {
    // Tested implicitly by other cases
  }

  public void testRecurseLiteral() throws Exception {
    checkSucceeds(
        "3;",
        "3;");
  }

  public void testRecurseLoop() throws Exception {
    checkSucceeds(
        "for (var k = 0; k < g[0]; k++) {" +
        "  g[1];" +
        "}",
        weldPrelude("g") +
        "for (var k = 0; k < ___.readPub(g, 0); k++) {" +
        "  ___.readPub(g, 1);" +
        "}");
    checkSucceeds(
        "while (g[0]) { g[1]; }",
        weldPrelude("g") +
        "while (___.readPub(g, 0)) { ___.readPub(g, 1); }");
  }

  public void testRecurseNoop() throws Exception {
    checkSucceeds(
        ";",
        ";");
  }

  public void testRecurseOperation() throws Exception {
    checkSucceeds(
        "g[0] + g[1];",
        weldPrelude("g") +
        "___.readPub(g, 0) + ___.readPub(g, 1);");
    checkSucceeds(
        "1 + 2 * 3 / 4 - -5;",
        "1 + 2 * 3 / 4 - -5;");
    checkSucceeds(
        "var x, y;" +
        "x  = y = g[0];",
        weldPrelude("g") +
        "var x, y;" +
        "x = y = ___.readPub(g, 0);");
  }

  public void testRecurseReturnStmt() throws Exception {
    checkSucceeds(
        "return g[0];",
        weldPrelude("g") +
        "return ___.readPub(g, 0);");
  }

  public void testRecurseSwitchStmt() throws Exception {
    checkSucceeds(
        "switch (g[0]) { }",
        weldPrelude("g") +
        "switch (___.readPub(g, 0)) { }");
  }

  public void testRecurseThrowStmt() throws Exception {
    checkSucceeds(
        "throw g[0];",
        weldPrelude("g") +
        "throw ___.readPub(g, 0);");
    checkSucceeds(
        "function() {" +
        "  var x;" +
        "  throw x;" +
        "};",
        "___.frozenFunc(function() {" +
        "  var x;" +
        "  throw x;" +
        "});");
  }

  public void testCantReadProto() throws Exception {
    rewriteAndExecute(
        "function foo(){}" +
        "assertEquals(foo.prototype, undefined);");
  }

  public void testSpecimenClickme() throws Exception {
    checkSucceeds(fromResource("clickme.js"));
  }

  public void testSpecimenListfriends() throws Exception {
    checkSucceeds(fromResource("listfriends.js"));
  }

  public void testRecursionOnIE() throws Exception {
    ParseTreeNode input = js(fromString(
        ""
        + "var x = 1;\n"
        + "var f = function x(b) { return b ? 1 : x(true); };\n"
        + "assertEquals(2, x + f());"));
    ParseTreeNode cajoled = defaultCajaRewriter.expand(input, mq);
    assertNoErrors();
    ParseTreeNode emulated = emulateIE6FunctionConstructors(cajoled);
    executePlain(
        ""
        + "___.getNewModuleHandler().getImports().assertEquals\n"
        + "    = ___.frozenFunc(assertEquals);\n"
        + "___.loadModule(function (___, IMPORTS___) {\n"
        + render(emulated)
        + "});").toString();
  }

  public void testAssertConsistent() throws Exception {
    // Since we test structurally, this works.
    assertConsistent("({})");
    try {
      // But this won't.
      assertConsistent("typeof (new RegExp('foo'))");
    } catch (AssertionFailedError e) {
      // Pass
      return;
    }
    fail("assertConsistent not working");
  }

  public void testIE_Emulation() throws Exception {
    ParseTreeNode input = js(fromString(
        ""
        + "void (function x() {});\n"
        + "assertEquals('function', typeof x);\n"));
    assertNoErrors();
    ParseTreeNode emulated = emulateIE6FunctionConstructors(input);
    executePlain(
        "___.loadModule(function (___, IMPORTS___) {"
        + render(emulated)
        + "});");
  }

  /**
   * Tests that the container can get access to
   * "virtual globals" defined in cajoled code.
   */
  public void testWrapperAccess() throws Exception {
    // TODO(ihab.awad): SECURITY: Re-enable by reading (say) x.foo, and
    // defining the property IMPORTS___.foo.
    if (false) {
    rewriteAndExecute(
        "",
        "x='test';",
        "if (___.getNewModuleHandler().getImports().x != 'test') {" +
          "fail('Cannot see inside the wrapper');" +
        "}");
    }
  }

  /**
   * Tests that Array.prototype cannot be modified.
   */
  public void testFrozenArray() throws Exception {
    rewriteAndExecute(
        "var success = false;" +
        "try {" +
          "Array.prototype[4] = 'four';" +
        "} catch (e){" +
          "success = true;" +
        "}" +
        "if (!success) fail('Array not frozen.');");
  }

  /**
   * Tests that Object.prototype cannot be modified.
   */
  public void testFrozenObject() throws Exception {
    rewriteAndExecute(
        "var success = false;" +
        "try {" +
          "Object.prototype.x = 'X';" +
        "} catch (e){" +
          "success = true;" +
        "}" +
        "if (!success) fail('Object not frozen.');");
  }

  /**
   * Tests that cajoled code can't construct new Function objects.
   */
  public void testFunction() throws Exception {
    rewriteAndExecute(
        "var success=false;" +
        "try{var f=new Function('1');}catch(e){success=true;}" +
        "if (!success)fail('Function constructor is accessible.')");
  }

  /**
   * Tests that constructors are inaccessible.
   */
  public void testConstructor() throws Exception {
    try {
      rewriteAndExecute(
          "function x(){}; var F = x.constructor;");
    } catch (junit.framework.AssertionFailedError e) {
      // pass
    }
  }

  public void testStamp() throws Exception {
    rewriteAndExecute(
        "___.getNewModuleHandler().getImports().stamp =" +
        "    ___.frozenFunc(___.stamp);" +
        "___.grantRead(___.getNewModuleHandler().getImports(), 'stamp');",
        "function Foo(){}" +
        "var foo = new Foo;" +
        "var passed = false;" +
        "cajita.log('### stamp = ' + stamp);" +
        "try { stamp({}, foo); }" +
        "catch (e) {" +
        "  if (!e.message.match('may not be stamped')) {" +
        "    fail(e.message);" +
        "  }" +
        "  passed = true;" +
        "}" +
        "if (!passed) { fail ('Able to stamp constructed objects.'); }",
        "");
    rewriteAndExecute(
        "___.getNewModuleHandler().getImports().stamp =" +
        "    ___.frozenFunc(___.stamp);" +
        "___.grantRead(___.getNewModuleHandler().getImports(), 'stamp');",
        "function Foo(){}" +
        "var foo = new Foo;" +
        "try { stamp({}, foo, true); }" +
        "catch (e) {" +
        "  fail(e.message);" +
        "}",
        "");
    rewriteAndExecute(
        "___.getNewModuleHandler().getImports().stamp =" +
        "    ___.frozenFunc(___.stamp);" +
        "___.grantRead(___.getNewModuleHandler().getImports(), 'stamp');",
        "var foo = {};" +
        "var tm = {};" +
        "stamp(tm, foo);" +
        "cajita.guard(tm, foo);",
        "");
    rewriteAndExecute(
        "___.getNewModuleHandler().getImports().stamp =" +
        "    ___.frozenFunc(___.stamp);" +
        "___.grantRead(___.getNewModuleHandler().getImports(), 'stamp');",
        "var foo = {};" +
        "var tm = {};" +
        "var passed = false;" +
        "try { cajita.guard(tm, foo); }" +
        "catch (e) {" +
        "  if (!e.message.match('This object does not have the given trademark')) {" +
        "    fail(e.message);" +
        "  }" +
        "  passed = true;" +
        "}" +
        "if (!passed) { fail ('Able to forge trademarks.'); }",
        "");
    rewriteAndExecute(
        "___.getNewModuleHandler().getImports().stamp =" +
        "    ___.frozenFunc(___.stamp);" +
        "___.grantRead(___.getNewModuleHandler().getImports(), 'stamp');",
        "var foo = {};" +
        "var tm = {};" +
        "var tm2 = {};" +
        "var passed = false;" +
        "try { stamp(tm, foo); cajita.guard(tm2, foo); }" +
        "catch (e) {" +
        "  if (!e.message.match('This object does not have the given trademark')) {" +
        "    fail(e.message);" +
        "  }" +
        "  passed = true;" +
        "}" +
        "if (!passed) { fail ('Able to forge trademarks.'); }",
        "");
    rewriteAndExecute(
        "___.getNewModuleHandler().getImports().stamp =" +
        "    ___.frozenFunc(___.stamp);" +
        "___.grantRead(___.getNewModuleHandler().getImports(), 'stamp');",
        "function foo(){};" +
        "var tm = {};" +
        "var passed = false;" +
        "try { stamp(tm, foo); }" +
        "catch (e) {" +
        "  if (!e.message.match('is frozen')) {" +
        "    fail(e.message);" +
        "  }" +
        "  passed = true;" +
        "}" +
        "if (!passed) { fail ('Able to stamp frozen objects.'); }",
        "");
  }

  public void testForwardReference() throws Exception {
    rewriteAndExecute(
        "var g = Bar;" +
        "if (true) { var f = Foo; }" +
        "function Foo(){}" +
        "do { var h = Bar; function Bar(){} } while (0);" +
        "assertEquals(typeof f, 'function');" +
        "assertEquals(typeof g, 'undefined');" +
        "assertEquals(typeof h, 'function');");
    rewriteAndExecute(
        "(function(){" +
        "var g = Bar;" +
        "if (true) { var f = Foo; }" +
        "function Foo(){}" +
        "do { var h = Bar; function Bar(){} } while (0);" +
        "assertEquals(typeof f, 'function');" +
        "assertEquals(typeof g, 'undefined');" +
        "assertEquals(typeof h, 'function');" +
        "})();");
  }

  public void testReformedGenerics() throws Exception {
    rewriteAndExecute(
        "var x = [33];" +
        "x.foo = [].push;" +
        "assertThrows(function(){x.foo(44)});");
    rewriteAndExecute(
        "var x = {blue:'green'};" +
        "x.foo = [].push;" +
        "assertThrows(function(){x.foo(44)});");
  }

  public void testIndexOf() throws Exception {
    assertConsistent("''.indexOf('1');");
  }

  public void testCallback() throws Exception {
    // These two cases won't work in Valija since every Valija disfunction has its own
    // non-generic call and apply methods.
    assertConsistent("(function(){}).apply.call(function(a, b) {return a + b;}, {}, [3, 4]);");
    assertConsistent("(function(){}).call.call(function(a, b) {return a + b;}, {}, 3, 4);");
  }

  /**
   * Tests the cajita.newTable(opt_useKeyLifetime) abstraction.
   * <p>
   * From here, we are not in a position to test the weak-GC properties this abstraction is
   * designed to provide, nor its O(1) complexity measure. However, we can test that it works
   * as a simple lookup table.
   */
  public void testTable() throws Exception {
    rewriteAndExecute(
        "var t = cajita.newTable();" +
        "var k1 = {};" +
        "var k2 = {};" +
        "var k3 = {};" +
        "t.set(k1, 'v1');" +
        "t.set(k2, 'v2');" +
        "assertEquals(t.get(k1), 'v1');" +
        "assertEquals(t.get(k2), 'v2');" +
        "assertTrue(t.get(k3) === void 0);");
    rewriteAndExecute(
        "var t = cajita.newTable(true);" +
        "var k1 = {};" +
        "var k2 = {};" +
        "var k3 = {};" +
        "t.set(k1, 'v1');" +
        "t.set(k2, 'v2');" +
        "assertEquals(t.get(k1), 'v1');" +
        "assertEquals(t.get(k2), 'v2');" +
        "assertTrue(t.get(k3) === void 0);");
    rewriteAndExecute(
        "var t = cajita.newTable();" +
        "t.set('foo', 'v1');" +
        "t.set(null, 'v2');" +
        "assertEquals(t.get('foo'), 'v1');" +
        "assertEquals(t.get(null), 'v2');" +
        "assertTrue(t.get({toString: function(){return 'foo';}}) === void 0);");
    rewriteAndExecute(
        "var t = cajita.newTable(true);" +
        "assertThrows(function(){t.set('foo', 'v1');});");
  }

  @Override
  protected Object executePlain(String caja) throws IOException {
    mq.getMessages().clear();
    return RhinoTestBed.runJs(
        new RhinoTestBed.Input(getClass(), "/com/google/caja/cajita.js"),
        new RhinoTestBed.Input(
            getClass(), "../../../../../js/jsunit/2.2/jsUnitCore.js"),
        new RhinoTestBed.Input(caja, getName() + "-uncajoled"));
  }

  @Override
  protected Object rewriteAndExecute(String pre, String caja, String post)
      throws IOException, ParseException {
    mq.getMessages().clear();

    List<Statement> children = new ArrayList<Statement>();
    children.add(js(fromString(caja, is)));
    String cajoledJs = render(rewriteStatements(
        new ModuleEnvelope(new Block(children))));

    assertNoErrors();

    final String[] assertFunctions = new String[] {
        "fail",
        "assertEquals",
        "assertTrue",
        "assertFalse",
        "assertLessThan",
        "assertNull",
        "assertThrows",
    };

    StringBuilder importsSetup = new StringBuilder();
    importsSetup.append("var testImports = ___.copy(___.sharedImports);");
    for (String f : assertFunctions) {
      importsSetup
          .append("testImports." + f + " = ___.func(" + f + ");")
          .append("___.grantRead(testImports, '" + f + "');");
    }
    importsSetup.append("___.getNewModuleHandler().setImports(testImports);");

    Object result = RhinoTestBed.runJs(
        new RhinoTestBed.Input(
            getClass(), "/com/google/caja/plugin/console-stubs.js"),
        new RhinoTestBed.Input(getClass(), "/com/google/caja/cajita.js"),
        new RhinoTestBed.Input(
            getClass(), "../../../../../js/jsunit/2.2/jsUnitCore.js"),
        new RhinoTestBed.Input(
            getClass(), "/com/google/caja/log-to-console.js"),
        new RhinoTestBed.Input(
            importsSetup.toString(),
            getName() + "-test-fixture"),
        new RhinoTestBed.Input(pre, getName()),
        // Load the cajoled code.
        new RhinoTestBed.Input(cajoledJs, getName() + "-cajoled"),
        new RhinoTestBed.Input(post, getName()),
        // Return the output field as the value of the run.
        new RhinoTestBed.Input(
            "___.getNewModuleHandler().getLastValue();", getName()));

    assertNoErrors();
    return result;
  }
}
