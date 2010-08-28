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

import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.ReturnStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.SyntheticNodes;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.TestBuildInfo;
import com.google.caja.util.Executor;
import com.google.caja.util.FailureIsAnOption;
import com.google.caja.util.Lists;
import com.google.caja.util.RhinoTestBed;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import junit.framework.AssertionFailedError;

/**
 * @author ihab.awad@gmail.com
 */
public class CajitaRewriterTest extends CommonJsRewriterTestCase {

  protected class TestUriFetcher implements UriFetcher {
    public FetchedData fetch(ExternalReference ref, String mimeType)
        throws UriFetchException {
      URI uri = ref.getUri();
      if ("test".equals(uri.getScheme())) {
        try {
          InputSource is = new InputSource(uri);
          return dataFromResource(uri.getPath().substring(1), is);
        } catch (IOException ex) {
          throw new UriFetchException(ref, mimeType, ex);
        }
      } else {
        throw new UriFetchException(ref, mimeType);
      }
    }
  }

  protected Rewriter cajitaRewriter;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    cajitaRewriter = new CajitaRewriter(TestBuildInfo.getInstance(), mq, false);
    setRewriter(cajitaRewriter);
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
        "    " + tempObj + "." + varName + "_canSet___ === " + tempObj + " ?" +
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

  private static String weldPreludes(String... names) {
    Arrays.sort(names);
    StringBuilder sb = new StringBuilder();
    for (String name : names) {
      sb.append("var ").append(name).append(" = ___.readImport(IMPORTS___, ")
          .append(StringLiteral.toQuotedValue(name)).append(");\n");
    }
    return sb.toString();
  }

  private static String weldPrelude(String name) { return weldPreludes(name); }

  private static String weldPrelude(String name, String permitsUsed) {
    return "var " + name + " = ___.readImport(IMPORTS___, '" + name +
           "', " + permitsUsed + ");";
  }

  public final void testToString() throws Exception {
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

  public final void testInitializeMap() throws Exception {
    assertConsistent("var zerubabel = {bobble:2, apple:1}; zerubabel.apple;");
  }

  public final void testValueOf() throws Exception {
    checkFails("var a = {valueOf:1};", "The valueOf property must not be set");
    checkFails(
        "var a = {x:0,valueOf:1};",
        "The valueOf property must not be set");
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
    checkFails("var x = { valueOf: function (hint) { return 2; } };",
               "The valueOf property must not be set");
    checkFails("var x = { valueOf: function (hint) { return 2; }, x: y };",
               "The valueOf property must not be set");
    checkFails("var o = {}; o.valueOf = function (hint) { return 2; };",
               "The valueOf property must not be set");

  }

  public final void testFunctionDoesNotMaskVariable() throws Exception {
    // Regress http://code.google.com/p/google-caja/issues/detail?id=370
    // TODO(ihab.awad): Enhance test framework to allow "before" and "after"
    // un-cajoled code to be executed, then change this to a functional test.
    checkSucceeds(
        ""
        + "function boo() { return x; }"
        + "var x;",
        ""
        + "var x;"
        + "function boo() {\n"
        + "  return x;\n"
        + "}\n"
        + "boo.FUNC___ = \'boo\';");
  }

  public final void testAssertEqualsCajoled() throws Exception {
    try {
      rewriteAndExecute("assertEquals(1, 2);");
    } catch (AssertionFailedError e) {
      return;
    }
    fail("Assertions do not work in cajoled mode");
  }

  public final void testAssertThrowsCajoledNoError() throws Exception {
    rewriteAndExecute(
        "  assertThrows(function() { throw 'foo'; });");
    rewriteAndExecute(
        "  assertThrows("
        + "    function() { throw 'foo'; },"
        + "    'foo');");
  }

  public final void testAssertThrowsCajoledErrorNoMsg() throws Exception {
    try {
      rewriteAndExecute("assertThrows(function() {});");
    } catch (AssertionFailedError e) {
      return;
    }
    fail("Assertions do not work in cajoled mode");
  }

  public final void testAssertThrowsCajoledErrorWithMsg() throws Exception {
    try {
      rewriteAndExecute("assertThrows(function() {}, 'foo');");
    } catch (AssertionFailedError e) {
      return;
    }
    fail("Assertions do not work in cajoled mode");
  }

  public final void testFreeVariables() throws Exception {
    checkSucceeds(
        "var y = x;",
        weldPrelude("x")
        + "var y;"
        + "y = x;");
    checkSucceeds(
        "function() { var y = x; };",
        weldPrelude("x")
        + "___.markFuncFreeze(function() {"
        + "var y;"
        + "y = x;"
        + "});");
  }

  public final void testConstructionWithFunction() throws Exception {
    assertConsistent(
        "  function Point() {}"
        + "var p = new Point();"
        + "(p !== undefined);");
    assertConsistent(
        "  var Point = function() {};"
        + "var p = new Point();"
        + "(p !== undefined);");
  }

  public final void testReflectiveMethodInvocation() throws Exception {
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
  public final void testToxicBind() throws Exception {
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
  public final void testBadDelete() throws Exception {
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
  public final void testNameFuncExprScoping() throws Exception {
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
  public final void testCajaPropsFrozen() throws Exception {
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
   * In anticipation of ES5, we should be able to index into strings
   * using indexes which are numbers or stringified numbers, so long as
   * they are in range.
   */
  public final void testStringIndexing() throws Exception {
    rewriteAndExecute("assertEquals('b', 'abc'[1]);");
  }

  @FailureIsAnOption
  public final void testStringIndexing2() throws Exception {
    // TODO(erights): This test isn't green because we haven't yet fixed the bug.
    rewriteAndExecute("assertEquals('b', 'abc'['1']);");
  }

  /**
   * Tests that <a href=
   * "http://code.google.com/p/google-caja/issues/detail?id=464"
   * >bug#464</a> is fixed.
   * <p>
   * Reading the apply property of a function should result in the apply
   * method as attached to that function.
   */
  public final void testAttachedReflection() throws Exception {
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
  public final void testInVeil() throws Exception {
    rewriteAndExecute(
        "assertFalse('FROZEN___' in Object);");
  }

  ////////////////////////////////////////////////////////////////////////
  // Handling of synthetic nodes
  ////////////////////////////////////////////////////////////////////////

  public final void testSyntheticIsUntouched() throws Exception {
    ParseTreeNode input = js(fromString("function foo() { this; arguments; }"));
    syntheticTree(input);
    checkSucceeds(input, input);
  }

  public final void testSyntheticMemberAccess() throws Exception {
    ParseTreeNode input = js(fromString("({}).foo"));
    syntheticTree(input);
    checkSucceeds(input, js(fromString("___.iM([]).foo;")));
  }

  public final void testSyntheticNestedIsExpanded() throws Exception {
    Statement innerInput = js(fromString("function foo() {}"));
    Block input = new Block(FilePosition.UNKNOWN, Arrays.asList(innerInput));
    makeSynthetic(input);
    ParseTreeNode expectedResult = js(fromString(
        "var foo; { foo = (function () {\n" +
        "             function foo$_self() {\n" +
        "             }\n" +
        "             foo$_self.FUNC___ = \'foo\';\n" +
        "             return foo$_self;" +
        "           })(); }"));
    checkSucceeds(input, expectedResult);
  }

  public final void testSyntheticNestedFunctionIsExpanded() throws Exception {
    // This test checks that a synthetic function, as is commonly generated
    // by Caja to wrap JavaScript event handlers declared in HTML, is rewritten
    // correctly.
    Block innerBlock = js(fromString("foo().x = bar();"));
    ParseTreeNode input = new Block(
        FilePosition.UNKNOWN,
        Arrays.asList(
            new FunctionDeclaration(
                SyntheticNodes.s(new FunctionConstructor(
                    FilePosition.UNKNOWN,
                    new Identifier(FilePosition.UNKNOWN, "f"),
                    Collections.<FormalParam>emptyList(),
                    innerBlock)))));
    // We expect the stuff in 'innerBlock' to be expanded, *but* we expect the
    // rewriter to be unaware of the enclosing function scope, so the temporary
    // variables generated by expanding 'innerBlock' spill out and get declared
    // outside the function rather than inside it.
    ParseTreeNode expectedResult = js(fromString(
        weldPrelude("bar")
        + weldPrelude("foo")
        + "var x0___, x1___;"  // Temporaries are declared up here ...
        + "function f() {"
        + "  "  // ... not down here!
        + weldSetPub("foo.CALL___()", "x", "bar.CALL___()", "x0___", "x1___")
        + ";}"));
    checkSucceeds(input, expectedResult);
  }

  public final void testSyntheticFormals() throws Exception {
    FilePosition unk = FilePosition.UNKNOWN;
    FunctionConstructor fc = new FunctionConstructor(
        unk,
        new Identifier(unk, "f"),
        Arrays.asList(
            new FormalParam(new Identifier(unk, "x")),
            new FormalParam(
                SyntheticNodes.s(new Identifier(unk, "y___")))),
        new Block(
            unk,
            Arrays.<Statement>asList(new ReturnStmt(
                unk,
                Operation.createInfix(
                    Operator.MULTIPLICATION,
                    Operation.createInfix(
                        Operator.ADDITION,
                        new Reference(new Identifier(unk, "x")),
                        new Reference(SyntheticNodes.s(
                            new Identifier(unk, "y___")))),
                    new Reference(new Identifier(unk, "z")))))));
    checkSucceeds(
        new Block(
            unk,
            Arrays.asList(
                new FunctionDeclaration((FunctionConstructor) fc.clone()))),
        js(fromString(
            ""
            // x and y___ are formals, but z is free to the function.
            + "var z = ___.readImport(IMPORTS___, 'z');"
            + "function f(x, y___) {"
            + "  return (x + y___) * z;"
            + "}"
            // Since the function is not synthetic, it's marked.
            + "f.FUNC___ = 'f';")));

    SyntheticNodes.s(fc);
    checkSucceeds(
        new Block(
            unk,
            Arrays.asList(
                new FunctionDeclaration((FunctionConstructor) fc.clone()))),
        js(fromString(
            ""
            // x and y___ are formals, but z is free to the function.
            + "var z = ___.readImport(IMPORTS___, 'z');"
            + "function f(x, y___) {"
            + "  return (x + y___) * z;"
            + "}"
            // Since the function is synthetic, it is not marked.
            )));
  }

  ////////////////////////////////////////////////////////////////////////
  // Handling of nested blocks
  ////////////////////////////////////////////////////////////////////////

  public final void testNestedBlockWithFunction() throws Exception {
    checkSucceeds(
        "{ function foo() {} }",
        "var foo;" +
        "{ foo = (function () {\n" +
        "             function foo$_self() {\n" +
        "             }\n" +
        "             foo$_self.FUNC___ = \'foo\';\n" +
        "             return foo$_self;\n" +
        "           })(); }");
  }

  public final void testNestedBlockWithVariable() throws Exception {
    checkSucceeds(
        "{ var x = g().y; }",
        weldPrelude("g")
        + "var x, x0___;"
        + "{"
        + "  x = " + weldReadPub("g.CALL___()", "y", "x0___") + ";"
        + "}");
  }

  ////////////////////////////////////////////////////////////////////////
  // Specific rules
  ////////////////////////////////////////////////////////////////////////

  public final void testWith() throws Exception {
    checkFails("with (dreams || ambiguousScoping) anything.isPossible();",
               "\"with\" blocks are not allowed");
    checkFails("with (dreams || ambiguousScoping) { anything.isPossible(); }",
               "\"with\" blocks are not allowed");
  }

  public final void testTryCatch() throws Exception {
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
        "var x;" +
        "try {" +
        "  g[x + 0];" +
        "  e;" +
        "  g[x + 1];" +
        "} catch (e) {" +
        "  g[x + 2];" +
        "  e;" +
        "  g[x + 3];" +
        "}",
        weldPreludes("e", "g") +
        "var x;" +
        "try {" +
        "  ___.readPub(g, x + 0);" +
        "  e;" +
        "  ___.readPub(g, x + 1);" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    ___.readPub(g, x + 2);" +
        "    e;" +
        "    ___.readPub(g, x + 3);" +
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
        "  throw function foo() { throw 'should not be called'; };" +
        "} catch (ex) {" +
        "  assertEquals('In lieu of thrown function: foo', ex());" +
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
        "  assertEquals('Exception during exception handling.', ex);" +
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");
  }

  public final void testTryCatchFinally() throws Exception {
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
        "var x;" +
        "try {" +
        "  g[x + 0];" +
        "  e;" +
        "  g[x + 1];" +
        "} catch (e) {" +
        "  g[x + 2];" +
        "  e;" +
        "  g[x + 3];" +
        "} finally {" +
        "  g[x + 4];" +
        "  e;" +
        "  g[x + 5];" +
        "}",
        weldPreludes("e", "g") +
        "var x;" +
        "try {" +
        "  ___.readPub(g, x + 0);" +
        "  e;" +
        "  ___.readPub(g, x + 1);" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    ___.readPub(g, x + 2);" +
        "    e;" +
        "    ___.readPub(g, x + 3);" +
        "  }" +
        "} finally {" +
        "    ___.readPub(g, x + 4);" +
        "    e;" +
        "    ___.readPub(g, x + 5);" +
        "}");
  }

  public final void testTryFinally() throws Exception {
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
        "var x;" +
        "try {" +
        "  g[x + 0];" +
        "  e;" +
        "  g[x + 1];" +
        "} finally {" +
        "  g[x + 2];" +
        "  e;" +
        "  g[x + 3];" +
        "}",
        weldPreludes("e", "g") +
        "var x;" +
        "try {" +
        "  ___.readPub(g, x + 0);" +
        "  e;" +
        "  ___.readPub(g, x + 1);" +
        "} finally {" +
        "  ___.readPub(g, x + 2);" +
        "  e;" +
        "  ___.readPub(g, x + 3);" +
        "}");
  }

  public final void testVarArgs() throws Exception {
    checkSucceeds(
        "var p;" +
        "var foo = function() {" +
        "  p = arguments;" +
        "};",
        "var p, foo;" +
        "foo = (function () {\n" +
        "         function foo$_var() {\n" +
        "           var a___ = ___.args(arguments);\n" +
        "           p = a___;\n" +
        "         }\n" +
        "         return ___.markFuncFreeze(foo$_var, 'foo$_var');\n" +
        "       })();");
  }

  public final void testVarThisBad() throws Exception {
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

  public final void testVarBadSuffix() throws Exception {
    checkFails(
        "function() { foo__; };",
        "Variables cannot end in \"__\"");
    // Make sure *single* underscore is okay
    checkSucceeds(
        "function() { var foo_ = 3; };",
        "___.markFuncFreeze(function() { var foo_; foo_ = 3; });");
  }

  public final void testVarBadSuffixDeclaration() throws Exception {
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

  public final void testVarFuncFreeze() throws Exception {
    // We can cajole and refer to a function
    rewriteAndExecute(
        "function foo() {}" +
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
        "  function foo() {}" +
        "  var bar = foo;" +
        "  bar.x = 3;" +
        "});");
  }

  public final void testVarGlobal() throws Exception {
    checkSucceeds(
        "foo;",
        weldPrelude("foo") +
        "foo;");
    checkSucceeds(
        "function() {" +
        "  foo;" +
        "};",
        weldPrelude("foo") +
        "___.markFuncFreeze(function() {" +
        "  foo;" +
        "});");
    checkSucceeds(
        "function() {" +
        "  var foo;" +
        "  foo;" +
        "};",
        "___.markFuncFreeze(function() {" +
        "  var foo;" +
        "  foo;" +
        "});");
  }

  public final void testVarDefault() throws Exception {
    String unchanged = (
        ""
        + "var x, y;"
        + "x = 3;"
        + "if (x) {}"
        + "x + 3;"
        + "y = undefined;");
    checkSucceeds(
        "function() {" +
        "  " + unchanged +
        "};",
        "var undefined = ___.readImport(IMPORTS___, 'undefined', {});" +
        "___.markFuncFreeze(function() {" +
        "  " + unchanged +
        "});");
  }

  public final void testReadBadSuffix() throws Exception {
    checkFails(
        "x.y__;",
        "Properties cannot end in \"__\"");
  }

  public final void testReadBadPrototype() throws Exception {
    checkAddsMessage(
        js(fromString("function foo() {} foo.prototype;")),
        RewriterMessageType.PROTOTYPICAL_INHERITANCE_NOT_IN_CAJITA);
    checkAddsMessage(
        js(fromString("var q = function foo() { foo.prototype; };")),
        RewriterMessageType.PROTOTYPICAL_INHERITANCE_NOT_IN_CAJITA);
  }

  public final void testReadPublicLength() throws Exception {
    checkSucceeds(
        ""
        + "var arr = [1, 2, 3];"
        + "arr.length;",
        ""
        + "var arr;"
        + "arr = [1, 2, 3];"
        + "arr.length;");
    checkSucceeds(
        ""
        + "var arr = [1, 2, 3];"
        + "arr.length = 4;",
        ""
        + "var arr;"
        + "arr = [1, 2, 3];"
        + "arr.length_canSet___ === arr ? (arr.length = 4) : "
        + "                               ___.setPub(arr, 'length', 4)"
        );
    checkSucceeds(
        ""
        + "var arr = [1, 2, 3];"
        + "--arr.length;",
        ""
        + "var arr, x0___;"
        + "arr = [1, 2, 3];"
        + "x0___ = arr.length - 1,"
        + "arr.length_canSet___ === arr"
        + "    ? (arr.length = x0___)"
        + "    : ___.setPub(arr, 'length', x0___);"
        );
  }

  public final void testReadPublic() throws Exception {
    checkSucceeds(
        ""
        + "var p;"
        + "p = foo().p;",
        weldPrelude("foo")
        + "var p, x0___;"
        + "p = " + weldReadPub("foo.CALL___()", "p", "x0___") + ";");
  }

  public final void testReadNumPublic() throws Exception {
    checkSucceeds(
        "var p, q, x;" +
        "p = q[x&(-1>>>1)];",
        "var p, q, x;" +
        "p = q[x&(-1>>>1)];");
    // Make sure that setPub is used on assignment to test frozenness.
    checkSucceeds(
        "var p, q, x;" +
        "q[x&(-1>>>1)] = p;",
        "var p, q, x;" +
        "___.setPub(q, x&(-1>>>1), p);");
    checkSucceeds(
        ""
        + "var p, q;"
        + "p[q&(-1>>>1)] += 2;",
        ""
        + "var p, q, x0___, x1___;"
        + "x0___ = p,"
        + "x1___ = q&(-1>>>1),"
        + "___.setPub(x0___, x1___&(-1>>>1), x0___[x1___&(-1>>>1)] + 2);");
    checkSucceeds(
        ""
        + "var p, q;"
        + "p[q&(-1>>>1)]--;",
        ""
        + "var p, q, x0___, x1___, x2___;"
        + "x0___ = p,"
        + "x1___ = q&(-1>>>1),"
        + "x2___ = +x0___[x1___&(-1>>>1)],"
        + "___.setPub(x0___, x1___&(-1>>>1), x2___ - 1), x2___;");
    checkSucceeds(
        ""
        + "var p, q;"
        + "--p[q&(-1>>>1)];",
        ""
        + "var p, q, x0___, x1___;"
        + "x0___ = p,"
        + "x1___ = q&(-1>>>1),"
        + "___.setPub(x0___, x1___&(-1>>>1), x0___[x1___&(-1>>>1)] - 1);");
  }

  public final void testNumWithConstantIndex() throws Exception {
    checkSucceeds(
        "var p, q;" +
        "p = q[3];",
        "var p, q;" +
        "p = q[3];");
    // Make sure that setPub is used on assignment to test frozenness.
    checkSucceeds(
        "var p, q;" +
        "q[3] = p;",
        "var p, q;" +
        "___.setPub(q, 3, p);");
    checkSucceeds(
        "var p;" +
        "p[3] -= 2",
        "var p;" +
        "___.setPub(p, 3, p[3] - 2);");
    checkSucceeds(
        "var p;" +
        "--p[3]",
        "var p;" +
        "___.setPub(p, 3, p[3] - 1);");
  }

  public final void testReadIndexPublic() throws Exception {
    checkSucceeds(
        "var p, q, x;" +
        "p = q[x];",
        "var p, q, x;" +
        "p = ___.readPub(q, x);");
  }

  public final void testSetBadAssignToFunctionName() throws Exception {
    checkAddsMessage(js(fromString(
        "  function foo() {}"
        + "foo = 3;")),
        RewriterMessageType.CANNOT_ASSIGN_TO_FUNCTION_NAME);
    checkAddsMessage(js(fromString(
        "  function foo() {}"
        + "foo += 3;")),
        RewriterMessageType.CANNOT_ASSIGN_TO_FUNCTION_NAME);
    checkAddsMessage(js(fromString(
        "  function foo() {}"
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

  public final void testSetBadThis() throws Exception {
    checkAddsMessage(
        js(fromString("function f() { this = 3; }")),
        RewriterMessageType.THIS_NOT_IN_CAJITA);
  }

  public final void testSetBadFreeVariable() throws Exception {
    checkAddsMessage(
        js(fromString("Array = function () { return [] };")),
        RewriterMessageType.CANNOT_MASK_IDENTIFIER);
    checkAddsMessage(
        js(fromString("x = 1;")),
        RewriterMessageType.CANNOT_ASSIGN_TO_FREE_VARIABLE);
  }

  public final void testSetBadSuffix() throws Exception {
    checkFails(
        "x.y__ = z;",
        "Properties cannot end in \"__\"");
  }

  public final void testSetBadPrototype() throws Exception {
    checkAddsMessage(
        js(fromString("function foo() {} foo.prototype = 3;")),
        RewriterMessageType.PROTOTYPICAL_INHERITANCE_NOT_IN_CAJITA);
    checkAddsMessage(
        js(fromString("var q = function foo() { foo.prototype = 3; };")),
        RewriterMessageType.PROTOTYPICAL_INHERITANCE_NOT_IN_CAJITA);
  }

  public final void testSetPublic() throws Exception {
    checkSucceeds(
        "x().p = g[h];",
        weldPreludes("g", "h", "x")
        + "var x0___, x1___;"
        + weldSetPub("x.CALL___()", "p", "___.readPub(g, h)", "x0___", "x1___")
        + ";");
  }

  public final void testSetIndexPublic() throws Exception {
    checkSucceeds(
        "g[0][g[1]] = g[2];",
        weldPrelude("g") +
        "___.setPub(g[0], g[1], g[2]);");
    checkSucceeds(
        "g[a][g[b]] = g[c];",
        weldPreludes("a", "b", "c", "g") +
        "___.setPub(___.readPub(g, a), ___.readPub(g, b), ___.readPub(g, c));");
  }

  public final void testSetBadInitialize() throws Exception {
    checkFails(
        "var x__ = 3;",
        "Variables cannot end in \"__\"");
  }

  public final void testSetInitialize() throws Exception {
    checkSucceeds(
        "var v = g[h];",
        weldPreludes("g", "h")
        + "var v;"
        + "v = ___.readPub(g, h);");
  }

  public final void testSetBadDeclare() throws Exception {
    checkFails(
        "var x__;",
        "Variables cannot end in \"__\"");
  }

  public final void testSetDeclare() throws Exception {
    checkSucceeds(
        "var v;",
        "var v;");
    checkSucceeds(
        "try { } catch (e) { var v; }",
        ""
        + "var v;"
        + "try {"
        + "} catch (ex___) {"
        + "  try {"
        + "    throw ___.tameException(ex___);"
        + "  } catch (e) {"
        + "  }"
        + "}");
  }

  public final void testSetVar() throws Exception {
    checkAddsMessage(
        js(fromString("try {} catch (x__) { x__ = 3; }")),
        RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE);
    checkSucceeds(
        "var x, y;" +
        "x = g[y];",
        weldPrelude("g") +
        "var x, y;" +
        "x = ___.readPub(g, y);");
  }

  public final void testSetReadModifyWriteLocalVar() throws Exception {
    checkFails("x__ *= 2;", "");
    checkFails("x *= y__;", "");
    checkSucceeds(
        "var x, y; x += g[y];",
        weldPrelude("g")
        + "var x, y; x = x + ___.readPub(g, y);");
    checkSucceeds(
        "myArray().key += 1;",
        weldPrelude("myArray")
        + "var x0___, x1___;"
        + "x0___ = myArray.CALL___(),"
        + "x1___ = (x0___.key_canRead___"
        + "         ? x0___.key : ___.readPub(x0___, 'key')) + 1,"
        + "x0___.key_canSet___ === x0___"
        + "     ? (x0___.key = x1___) : ___.setPub(x0___, 'key', x1___);");
    checkSucceeds(
        "myArray()[myKey()] += 1;",
        weldPrelude("myArray")
        + weldPrelude("myKey")
        + "var x0___, x1___;"
        + "x0___ = myArray.CALL___(),"
        + "x1___ = myKey.CALL___(),"
        + "___.setPub(x0___, x1___,"
        + "           ___.readPub(x0___, x1___) + 1);");
    checkSucceeds(  // Local reference need not be assigned to a temp.
        "(function (myKey) { myArray()[myKey] += 1; });",
        weldPrelude("myArray")
        + "___.markFuncFreeze(function (myKey) {"
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
          "var x, y; x " + op.getSymbol() + " g[y];",
          weldPrelude("g")
          + "var x, y;"
          + "x = x " + op.getAssignmentDelegate().getSymbol()
              + "___.readPub(g, y);");
    }
  }

  public final void testSetIncrDecr() throws Exception {
    checkFails("x__--;", "");
    checkSucceeds(
        "g[i]++;",
        weldPreludes("g", "i")
        + "var x0___, x1___, x2___;"
        + "x0___ = g,"
        + "x1___ = i,"
        + "x2___ = +___.readPub(x0___, x1___),"
        + "___.setPub(x0___, x1___, x2___ + 1),"
        + "x2___;");
    checkSucceeds(
        "g[i]--;",
        weldPreludes("g", "i")
        + "var x0___, x1___, x2___;"
        + "x0___ = g,"
        + "x1___ = i,"
        + "x2___ = +___.readPub(x0___, x1___),"
        + "___.setPub(x0___, x1___, x2___ - 1),"
        + "x2___;");
    checkSucceeds(
        "++g[i];",
        weldPreludes("g", "i")
        + "var x0___, x1___;"
        + "x0___ = g,"
        + "x1___ = i,"
        + "___.setPub(x0___, x1___, ___.readPub(x0___, x1___) - -1);");

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

  public final void testSetIncrDecrOnLocals() throws Exception {
    checkFails("++x__;", "");
    checkSucceeds(
        "(function (x, y) { return [x--, --x, y++, ++y]; });",
        "___.markFuncFreeze(" +
        "  function (x, y) { return [x--, --x, y++, ++y]; });");

    assertConsistent(
        "(function () {" +
        "  var x = 2;" +
        "  var arr = [--x, x, x--, x, ++x, x, x++, x];" +
        "  assertEquals('1,1,1,0,1,1,1,2', arr.join(','));" +
        "  return arr;" +
        "})();");
  }

  public final void testSetIncrDecrOfComplexLValues() throws Exception {
    checkFails("arr[x__]--;", "Variables cannot end in \"__\"");
    checkFails("arr__[x]--;", "Variables cannot end in \"__\"");

    checkSucceeds(
        "o.x++;",
        weldPrelude("o")
        + "var x0___, x1___, x2___;"
        + "x0___ = o,"
        + "x1___ = +(x0___.x_canRead___ ? x0___.x : ___.readPub(x0___, 'x')),"
        + "(x2___ = x1___ + 1,"
        + " x0___.x_canSet___ === x0___"
        + "     ? (x0___.x = x2___)"
        + "     : ___.setPub(x0___, 'x', x2___)),"
        + "x1___;");

    assertConsistent(
        "(function () {" +
        "  var o = { x: 2 };" +
        "  var arr = [--o.x, o.x, o.x--, o.x, ++o.x, o.x, o.x++, o.x];" +
        "  assertEquals('1,1,1,0,1,1,1,2', arr.join(','));" +
        "  return arr;" +
        "})();");
  }

  public final void testSetIncrDecrOrderOfAssignment() throws Exception {
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

  public final void testNewCallledCtor() throws Exception {
    checkSucceeds(
        "new Date();",
        weldPrelude("Date", "{}")
        + "___.construct(Date, []);");
  }

  public final void testNewCalllessCtor() throws Exception {
    checkSucceeds(
        "(new Date);",
        weldPrelude("Date", "{}")
        + "___.construct(Date, []);");
  }

  public final void testDeletePub() throws Exception {
    checkFails("delete x.foo___;", "Properties cannot end in \"__\"");
    checkSucceeds(
        "delete foo()[bar()];",
        weldPreludes("bar", "foo") +
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

  public final void testDeleteFails() throws Exception {
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

  public final void testDeleteNonLvalue() throws Exception {
    checkFails("delete 4;", "Invalid operand to delete");
  }

  public final void testCallPublic() throws Exception {
    checkSucceeds(
        "g[i + 0].m(g[i + 1], g[i + 2]);",
        weldPreludes("g", "i")
        + "var x0___, x1___, x2___;"
        + "x0___ = ___.readPub(g, i + 0),"
        + "x1___ = ___.readPub(g, i + 1),"
        + "x2___ = ___.readPub(g, i + 2),"
        + "x0___.m_canCall___"
        + "    ? x0___.m(x1___, x2___)"
        + "    : ___.callPub(x0___, 'm', [x1___, x2___]);");
  }

  public final void testCallIndexPublic() throws Exception {
    checkSucceeds(
        "g[i + 0][g[i + 1]](g[i + 2], g[i + 3]);",
        weldPreludes("g", "i") +
        "___.callPub(" +
        "    ___.readPub(g, i + 0)," +
        "    ___.readPub(g, i + 1)," +
        "    [___.readPub(g, i + 2), ___.readPub(g, i + 3)]);");
  }

  public final void testCallFunc() throws Exception {
    checkSucceeds(
        "g(g[i + 1], g[i + 2]);",
        weldPreludes("g", "i") +
        "g.CALL___" +
        "     (___.readPub(g, i + 1), ___.readPub(g, i + 2));");
  }

  public final void testPermittedCall() throws Exception {
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
        + "  dis: ___.markFuncFreeze(function(n) { x = n; })"
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

  public final void testFuncAnonSimple() throws Exception {
    // TODO(ihab.awad): The below test is not as complete as it should be
    // since it does not test the "@stmts*" substitution in the rule
    checkSucceeds(
        "function(x, y, i) { x = arguments; y = g[i]; };",
        weldPrelude("g") +
        "___.markFuncFreeze(function(x, y, i) {" +
        "  var a___ = ___.args(arguments);" +
        "  x = a___;" +
        "  y = ___.readPub(g, i);" +
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
        "             function foo() {}" +
        "             foo.x = 3;" +
        "             return foo;" +
        "           })();" +
        "foo();" +
        "foo.x;");
  }

  public final void testFuncNamedSimpleDecl() throws Exception {
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
        "  function foo(x, y, i) {" +
        "    x = arguments;" +
        "    y = g[i];" +
        "    return foo(x - 1, y - 1);" +
        "  }" +
        "}",
        weldPrelude("g") +
        "___.markFuncFreeze(function() {" +
        "  function foo(x, y, i) {\n" +
        "    var a___ = ___.args(arguments);\n" +
        "    x = a___;\n" +
        "    y = ___.readPub(g, i);\n" +
        "    return foo.CALL___(x - 1, y - 1);\n" +
        "  }\n" +
        "  foo.FUNC___ = \'foo\';" +
        "});");
    checkSucceeds(
        "function foo(x, y ) {" +
        "  return foo(x - 1, y - 1);" +
        "}",
        "  function foo(x, y) {\n" +
        "    return foo.CALL___(x - 1, y - 1);\n" +
        "  }\n" +
        "  foo.FUNC___ = \'foo\';");
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

  public final void testFuncNamedSimpleValue() throws Exception {
    checkSucceeds(
        "var f = function foo(x, y) {" +
        "  x = arguments;" +
        "  y = z;" +
        "  return foo(x - 1, y - 1);" +
        "};",
        weldPrelude("z")
        + "var f;"
        + "f = function() {"
        + "  function foo(x, y) {"
        + "    var a___ = ___.args(arguments);"
        + "    x = a___;"
        + "    y = z;"
        + "    return foo.CALL___(x - 1, y - 1);"
        + "  }"
        + "  return ___.markFuncFreeze(foo, 'foo');"
        + "}();");
    checkSucceeds(
        "var bar = function foo_(x, y ) {" +
        "  return foo_(x - 1, y - 1);" +
        "};",
        ""
        + "var bar;"
        + "bar = function() {"
        + "  function foo_(x, y) {"
        + "    return foo_.CALL___(x - 1, y - 1);"
        + "  }"
        + "  return ___.markFuncFreeze(foo_, 'foo_');"
        + "}();");
  }

  public final void testMaskingFunction() throws Exception {
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

  public final void testMapEmpty() throws Exception {
    checkSucceeds(
        "var f = {};",
        "var f; f = ___.iM([]);");
  }

  public final void testMapBadKeySuffix() throws Exception {
    checkAddsMessage(
        js(fromString("var o = { x__: 3 };")),
        RewriterMessageType.PROPERTIES_CANNOT_END_IN_DOUBLE_UNDERSCORE);
  }

  public final void testMapNonEmpty() throws Exception {
    checkSucceeds(
        "var o = { k0: g().x, k1: g().y };",
        weldPrelude("g")
        + "var o, x0___, x1___;"
        + "o = ___.iM("
        + "    [ 'k0', " + weldReadPub("g.CALL___()", "x", "x0___") + ", "
        + "      'k1', " + weldReadPub("g.CALL___()", "y", "x1___") + " ]);");
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

  public final void testMapSingle() throws Exception {
    checkSucceeds(
        "var o = { k0: p.x };",
        weldPrelude("p")
        + "var o;"
        + "o = ___.iM(['k0', p.x_canRead___ ? p.x : ___.readPub(p, 'x')]);");
    checkFails("var o = { valueOf: p.x, k1: p.y };",
               "The valueOf property must not be set");
    checkFails("var o = { x___: p.x, k1: p.y };",
               "Properties cannot end in \"__\"");
  }

  public final void testMapPlural() throws Exception {
    checkSucceeds(
        "var o = { k0: p.x, k1: p.y };",
        weldPrelude("p")
        + "var o;"
        + "o = ___.iM(['k0', p.x_canRead___ ? p.x : ___.readPub(p, 'x'),"
        + "                'k1', p.y_canRead___ ? p.y : ___.readPub(p, 'y')]);"
        );
    checkFails("var o = { valueOf: p.x, k1: p.y };",
               "The valueOf property must not be set");
    checkFails("var o = { x___: p.x, k1: p.y };",
               "Properties cannot end in \"__\"");
    checkFails("var o = { k0: p.x, valueOf: p.y };",
               "The valueOf property must not be set");
    checkFails("var o = { k0: p.x, x___: p.y };",
               "Properties cannot end in \"__\"");
  }

  public final void testOtherInstanceof() throws Exception {
    checkSucceeds(
        "function foo() {}" +
        "g[void 0] instanceof foo;",
        weldPrelude("g") +
        "function foo() {\n" +
        "}\n" +
        "foo.FUNC___ = \'foo\';" +
        "___.readPub(g, void 0) instanceof ___.primFreeze(foo);");
    checkSucceeds(
        "g[i] instanceof Object;",
        weldPreludes("Object", "g", "i") +
        "___.readPub(g, i) instanceof Object;");

    assertConsistent("[ (({}) instanceof Object)," +
                     "  ((new Date) instanceof Date)," +
                     "  (({}) instanceof Date)" +
                     "];");
    assertConsistent("function foo() {}  (new foo) instanceof foo;");
    assertConsistent("function foo() {}  !(({}) instanceof foo);");
  }

  public final void testOtherTypeof() throws Exception {
    checkSucceeds(
        "typeof g[i];",
        weldPreludes("g", "i") +
        "___.typeOf(___.readPub(g, i));");
    checkFails("typeof ___;", "Variables cannot end in \"__\"");
    rewriteAndExecute("assertEquals(typeof new RegExp('.*'), 'object');");
  }

  public final void testLabeledStatement() throws Exception {
    checkFails("IMPORTS___: 1;", "Labels cannot end in \"__\"");
    checkFails("IMPORTS___: while (1);", "Labels cannot end in \"__\"");
    checkSucceeds("foo: 1;", "foo: 1;");
    checkFails("while (1) { break x__; }", "Labels cannot end in \"__\"");
    checkSucceeds("break foo;", "break foo;");
    checkFails("while (1) { continue x__; }", "Labels cannot end in \"__\"");
    checkSucceeds("continue foo;", "continue foo;");
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

  public final void testRegexLiteral() throws Exception {
    checkAddsMessage(
        js(fromString("/x/;")),
        RewriterMessageType.REGEX_LITERALS_NOT_IN_CAJITA);
    checkAddsMessage(
        js(fromString("var y = /x/;")),
        RewriterMessageType.REGEX_LITERALS_NOT_IN_CAJITA);
  }

  public final void testOtherSpecialOp() throws Exception {
    checkSucceeds("void 0;", "void 0;");
    checkSucceeds("void g();",
                  weldPrelude("g") +
                  "void g.CALL___();");
    checkSucceeds("g[i], g[1];",
                  weldPreludes("g", "i") +
                  "___.readPub(g, i), g[1];");
  }

  public final void testMultiDeclaration2() throws Exception {
    // 'var' in global scope, part of a block
    checkSucceeds(
        "var x, y;",
        "var x, y;");
    checkSucceeds(
        "var x = g[0], y = g[x];",
        weldPrelude("g")
        + "var x, y; x = g[0], y = ___.readPub(g, x);");
    checkSucceeds(
        "var x, y = g[0];",
        weldPrelude("g")
        + "var x, y; y = g[0];");
    // 'var' in global scope, 'for' statement
    checkSucceeds(
        "for (var x, y; ; ) {}",
        "var x, y; for (; ; ) {}");
    checkSucceeds(
        "for (var x = g[i], y = g[x]; ; ) {}",
        weldPreludes("g", "i")
        + "var x, y;"
        + "for (x = ___.readPub(g, i), y = ___.readPub(g, x); ; ) {}");
    checkSucceeds(
        "for (var x, y = g[i]; ; ) {}",
        weldPreludes("g", "i")
        + "var x, y;"
        + "for (y = ___.readPub(g, i); ; ) {}");
    // 'var' in global scope, part of a block
    checkSucceeds(
        "function() {" +
        "  var x, y;" +
        "};",
        "___.markFuncFreeze(function() {" +
        "  var x, y;" +
        "});");
    checkSucceeds(
        ""
        + "function (i) {"
        + "  var x = g[i], y = g[i + 1];"
        + "};",
        weldPrelude("g")
        + "___.markFuncFreeze(function (i) {"
        + "  var x, y;"
        + "  x = ___.readPub(g, i), y = ___.readPub(g, i + 1);"
        + "});");
    checkSucceeds(
        ""
        + "function (i) {"
        + "  var x, y = g[i];"
        + "}",
        weldPrelude("g")
        + "___.markFuncFreeze(function (i) {"
        + "  var x, y;"
        + "  y = ___.readPub(g, i);"
        + "});");
    // 'var' in global scope, 'for' statement
    checkSucceeds(
        ""
        + "function() {"
        + "  for (var x, y; ; ) {}"
        + "}",
        ""
        + "___.markFuncFreeze(function() {"
        + "  var x, y;"
        + "  for (; ; ) {}"
        + "});");
    checkSucceeds(
        ""
        + "function (a, b) {"
        + "  for (var x = g[a], y = g[b]; ; ) {}"
        + "}",
        weldPrelude("g")
        + "___.markFuncFreeze(function (a, b) {"
        + "  var x, y;"
        + "  for (x = ___.readPub(g, a), y = ___.readPub(g, b); ; ) {}"
        + "});");
    checkSucceeds(
        ""
        + "function (i) {"
        + "  for (var x, y = g[i]; ; ) {}"
        + "}",
        weldPrelude("g")
        + "___.markFuncFreeze(function (i) {"
        + "  var x, y;"
        + "  for (y = ___.readPub(g, i); ; ) {}"
        + "});");
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

  public final void testRecurseParseTreeNodeContainer() {
    // Tested implicitly by other cases
  }

  public final void testRecurseArrayConstructor() throws Exception {
    checkSucceeds(
        "var foo = [ g[a], g[b] ];",
        weldPreludes("a", "b", "g")
        + "var foo;"
        + "foo = [___.readPub(g, a), ___.readPub(g, b)];");
  }

  public final void testRecurseBlock() {
    // Tested implicitly by other cases
  }

  public final void testRecurseBreakStmt() throws Exception {
    checkSucceeds(
        "while (true) { break; }",
        "while (true) { break; }");
  }

  public final void testRecurseCaseStmt() throws Exception {
    checkSucceeds(
        "switch (g[i]) { case 1: break; }",
        weldPreludes("g", "i") +
        "switch (___.readPub(g, i)) { case 1: break; }");
  }

  public final void testRecurseConditional() throws Exception {
    checkSucceeds(
        "if (g[i] === g[i + 1]) {" +
        "  g[i + 2];" +
        "} else if (g[i + 3] === g[i + 4]) {" +
        "  g[i + 5];" +
        "} else {" +
        "  g[i + 6];" +
        "}",
        weldPreludes("g", "i") +
        "if (___.readPub(g, i) === ___.readPub(g, i + 1)) {" +
        "  ___.readPub(g, i + 2);" +
        "} else if (___.readPub(g, i + 3) === ___.readPub(g, i + 4)) {" +
        "  ___.readPub(g, i + 5);" +
        "} else {" +
        "  ___.readPub(g, i + 6);" +
        "}");
  }

  public final void testRecurseContinueStmt() throws Exception {
    checkSucceeds(
        "while (true) { continue; }",
        "while (true) { continue; }");
  }

  public final void testRecurseDebuggerStmt() throws Exception {
    checkSucceeds("debugger;", "debugger;");
  }

  public final void testRecurseDefaultCaseStmt() throws Exception {
    checkSucceeds(
        "switch (g[i]) { default: break; }",
        weldPreludes("g", "i") +
        "switch(___.readPub(g, i)) { default: break; }");
  }

  public final void testRecurseExpressionStmt() {
    // Tested implicitly by other cases
  }

  public final void testRecurseIdentifier() {
    // Tested implicitly by other cases
  }

  public final void testRecurseLiteral() throws Exception {
    checkSucceeds(
        "3;",
        "3;");
  }

  public final void testRecurseLoop() throws Exception {
    checkSucceeds(
        ""
        + "for (var i, k = 0; k < g[i]; k++) {"
        + "  g[i + 1];"
        + "}",
        weldPrelude("g")
        + "var i, k;"
        + "for (k = 0; k < ___.readPub(g, i); k++) {"
        + "  ___.readPub(g, i + 1);"
        + "}");
    checkSucceeds(
        "while (g[i]) { g[i + 1]; }",
        weldPreludes("g", "i") +
        "while (___.readPub(g, i)) { ___.readPub(g, i + 1); }");
  }

  public final void testRecurseNoop() throws Exception {
    checkSucceeds(js(fromString(";")), new Block(FilePosition.UNKNOWN));
  }

  public final void testRecurseOperation() throws Exception {
    checkSucceeds(
        "g[i] + g[j];",
        weldPreludes("g", "i", "j") +
        "___.readPub(g, i) + ___.readPub(g, j);");
    checkSucceeds(
        "1 + 2 * 3 / 4 - -5;",
        "1 + 2 * 3 / 4 - -5;");
    checkSucceeds(
        "var x, y, z;" +
        "x  = y = g[z];",
        weldPrelude("g") +
        "var x, y, z;" +
        "x = y = ___.readPub(g, z);");
  }

  public final void testRecurseReturnStmt() throws Exception {
    checkSucceeds(
        "return g[i];",
        weldPreludes("g", "i") +
        "return ___.readPub(g, i);");
  }

  public final void testRecurseSwitchStmt() throws Exception {
    checkSucceeds(
        "switch (g[i]) { }",
        weldPreludes("g", "i") +
        "switch (___.readPub(g, i)) { }");
  }

  public final void testRecurseThrowStmt() throws Exception {
    checkSucceeds(
        "throw g[i];",
        weldPreludes("g", "i") +
        "throw ___.readPub(g, i);");
    checkSucceeds(
        "function() {" +
        "  var x;" +
        "  throw x;" +
        "};",
        "___.markFuncFreeze(function() {" +
        "  var x;" +
        "  throw x;" +
        "});");
  }

  public final void testCantReadProto() throws Exception {
    rewriteAndExecute(
        "function foo(){}" +
        "assertEquals(foo.prototype, undefined);");
  }

  public final void testSpecimenClickme() throws Exception {
    checkSucceeds(fromResource("clickme.js"));
  }

  public final void testSpecimenListfriends() throws Exception {
    checkSucceeds(fromResource("listfriends.js"));
  }

  public final void testRecursionOnIE() throws Exception {
    ParseTreeNode input = js(fromString(
        ""
        + "var x = 1;\n"
        + "var f = function x(b) { return b ? 1 : x(true); };\n"
        + "assertEquals(2, x + f());"));
    ParseTreeNode cajoled = cajitaRewriter.expand(input);
    assertNoErrors();
    ParseTreeNode emulated = emulateIE6FunctionConstructors(cajoled);
    executePlain(
        ""
        + "___.getNewModuleHandler().getImports().assertEquals\n"
        + "    = ___.markFuncFreeze(assertEquals);\n"
        + "___.loadModule({\n"
        + "  instantiate: function (___, IMPORTS___) {\n"
        + "    " + render(emulated) + "\n"
        + "  }\n"
        + " });").toString();
  }

  public final void testAssertConsistent() throws Exception {
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

  public final void testIE_Emulation() throws Exception {
    ParseTreeNode input = js(fromString(
        ""
        + "void (function x() {});\n"
        + "assertEquals('function', typeof x);\n"));
    assertNoErrors();
    ParseTreeNode emulated = emulateIE6FunctionConstructors(input);
    executePlain(
        "  ___.loadModule({"
        + "  instantiate: function (___, IMPORTS___) {"
        + "    " + render(emulated)
        + "  }"
        + "});");
  }

  /**
   * Tests that the container can get access to
   * "virtual globals" defined in cajoled code.
   */
  @FailureIsAnOption
  public final void testWrapperAccess() throws Exception {
    // TODO(ihab.awad): SECURITY: Re-enable by reading (say) x.foo, and
    // defining the property IMPORTS___.foo.
    rewriteAndExecute(
        "",
        "x='test';",
        "if (___.getNewModuleHandler().getImports().x != 'test') {" +
          "fail('Cannot see inside the wrapper');" +
        "}");
  }

  /**
   * Tests that Array.prototype cannot be modified.
   */
  public final void testFrozenArray() throws Exception {
    rewriteAndExecute(
        "var success = false;" +
        "try {" +
          "Array.prototype[4] = 'four';" +
        "} catch (e){" +
          "success = true;" +
        "}" +
        "if (!success) { fail('Array.prototype not frozen.'); }");
  }

  /**
   * Tests that Object.prototype cannot be modified.
   */
  public final void testFrozenObject() throws Exception {
    rewriteAndExecute(
        "var success = false;" +
        "try {" +
          "Object.prototype.x = 'X';" +
        "} catch (e){" +
          "success = true;" +
        "}" +
        "if (!success) { fail('Object.prototype not frozen.'); }");
  }

  public final void testStamp() throws Exception {
    rewriteAndExecute(
        "function Foo(){}" +
        "var foo = new Foo();" +
        "var TestMark = cajita.Trademark('Test');" +
        "var passed = false;" +
        "try { " +
        "  cajita.stamp([TestMark.stamp], foo);" +
        "} catch (e) {" +
        "  if (e.message !== 'Can only stamp records: [object Object]') {" +
        "    fail(e.message);" +
        "  }" +
        "  passed = true;" +
        "}" +
        "if (!passed) { fail ('Able to stamp constructed objects.'); }");
    rewriteAndExecute(
        // Shows how privileged or uncajoled code can stamp
        // constructed objects anyway. Should only do this during
        // construction, though this is unenforceable.
        "___.getNewModuleHandler().getImports().stampAnyway =" +
        "  ___.markFuncFreeze(function(stamp, obj) {" +
        "    stamp.mark___(obj);" +
        "  });",
        "function Foo(){}" +
        "var foo = new Foo();" +
        "var TestMark = cajita.Trademark('Test');" +
        "try { " +
        "  stampAnyway(TestMark.stamp, foo);" +
        "} catch (e) {" +
        "  fail(e.message);" +
        "}" +
        "cajita.guard(TestMark.guard, foo);",
        "");
    rewriteAndExecute(
        "var foo = {};" +
        "var TestMark = cajita.Trademark('Test');" +
        "cajita.stamp([TestMark.stamp], foo);" +
        "cajita.guard(TestMark.guard, foo);");
    rewriteAndExecute(
        "var foo = {};" +
        "var TestMark = cajita.Trademark('Test');" +
        "cajita.stamp([TestMark.stamp], foo);" +
        "TestMark.guard.coerce(foo);");
    rewriteAndExecute(
        "var foo = {};" +
        "var TestMark = cajita.Trademark('Test');" +
        "var passed = false;" +
        "try { " +
        "  cajita.guard(TestMark.guard, foo);" +
        "} catch (e) {" +
        "  if (e.message !== 'Specimen does not have the \"Test\" trademark') {" +
        "    fail(e.message);" +
        "  }" +
        "  passed = true;" +
        "}" +
        "if (!passed) { fail ('Able to forge trademarks.'); }");
    rewriteAndExecute(
        "var foo = {};" +
        "var T1Mark = cajita.Trademark('T1');" +
        "var T2Mark = cajita.Trademark('T2');" +
        "var passed = false;" +
        "try { " +
        "  cajita.stamp([T1Mark.stamp], foo);" +
        "  cajita.guard(T2Mark.guard, foo);" +
        "} catch (e) {" +
        "  if (e.message !== 'Specimen does not have the \"T2\" trademark') {" +
        "    fail(e.message);" +
        "  }" +
        "  passed = true;" +
        "}" +
        "if (!passed) { fail ('Able to forge trademarks.'); }");
    rewriteAndExecute(
        "var foo = cajita.freeze({});" +
        "var TestMark = cajita.Trademark('Test');" +
        "var passed = false;" +
        "try {" +
        "  cajita.stamp([TestMark.stamp], foo);" +
        "} catch (e) {" +
        "  if (e.message !== 'Can\\'t stamp frozen objects: [object Object]') {" +
        "    fail(e.message);" +
        "  }" +
        "  passed = true;" +
        "}" +
        "if (!passed) { fail ('Able to stamp frozen objects.'); }");
    rewriteAndExecute(
        "var foo = {};" +
        "var bar = cajita.beget(foo);" +
        "var baz = cajita.beget(bar);" +
        "var TestMark = cajita.Trademark('Test');" +
        "cajita.stamp([TestMark.stamp], bar);" +
        "assertFalse(cajita.passesGuard(TestMark.guard, foo));" +
        "assertTrue(cajita.passesGuard(TestMark.guard, bar));" +
        "assertFalse(cajita.passesGuard(TestMark.guard, baz));");
  }

  public final void testForwardReference() throws Exception {
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

  public final void testReformedGenerics() throws Exception {
    rewriteAndExecute(
        "var x = [33];" +
        "x.foo = [].push;" +
        "assertThrows(function(){x.foo(44);});");
    rewriteAndExecute(
        "var x = {blue:'green'};" +
        "x.foo = [].push;" +
        "assertThrows(function(){x.foo(44);});");
    assertConsistent(
        "var x = {blue:'green'};" +
        "x.foo = [].push;" +
        "x.foo.call(x, 44);" +
        "cajita.getOwnPropertyNames(x).sort();");
  }

  public final void testIndexOf() throws Exception {
    assertConsistent("''.indexOf('1');");
  }

  public final void testCallback() throws Exception {
    // These two cases won't work in Valija since every Valija disfunction has
    // its own non-generic call and apply methods.
    assertConsistent(
        "(function(){}).apply.call(function(a, b) {return a + b;}, {}, [3, 4]);"
        );
    assertConsistent(
        "(function(){}).call.call(function(a, b) {return a + b;}, {}, 3, 4);");
    rewriteAndExecute("",
        "var a = [];\n" +
        "cajita.forOwnKeys({x:3}, function(k, v) {a.push(k, v);});" +
        "assertEquals(a.toString(), 'x,3');",
        "var a = [];\n" +
        "cajita.forOwnKeys({x:3}, ___.markFuncFreeze(function(k, v) {a.push(k, v);}));" +
        "assertEquals(a.toString(), 'x,3');");
    rewriteAndExecute("",
        "var a = [];\n" +
        "cajita.forAllKeys({x:3}, function(k, v) {a.push(k, v);});" +
        "assertEquals(a.toString(), 'x,3');",
        "var a = [];\n" +
        "cajita.forAllKeys({x:3}, ___.markFuncFreeze(function(k, v) {a.push(k, v);}));" +
        "assertEquals(a.toString(), 'x,3');");
  }

  /**
   * Tests the cajita.newTable(opt_useKeyLifetime) abstraction.
   * <p>
   * From here, we are not in a position to test the weak-GC properties this
   * abstraction is designed to provide, nor its O(1) complexity measure.
   * However, we can test that it works as a simple lookup table.
   */
  public final void testTable() throws Exception {
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
    rewriteAndExecute(
        "var t = cajita.newTable(true);" +
        "var k1 = {};" +
        "var k2 = cajita.beget(k1);" +
        "var k3 = cajita.beget(k2);" +
        "var k4 = cajita.beget(k3);" +
        "t.set(k2, 'foo');" +
        "t.set(k3, 'bar');" +
        "assertEquals(t.get(k2), 'foo');\n" +
        "assertEquals(t.get(k3), 'bar');\n" +
        "assertTrue(t.get(k1) === void 0);\n" +
        "assertTrue(t.get(k4) === void 0);");
    rewriteAndExecute(
        "var t = cajita.newTable();" +
        "var k1 = {};" +
        "var k2 = cajita.beget(k1);" +
        "var k3 = cajita.beget(k2);" +
        "var k4 = cajita.beget(k3);" +
        "t.set(k2, 'foo');" +
        "t.set(k3, 'bar');" +
        "assertEquals(t.get(k2), 'foo');\n" +
        "assertEquals(t.get(k3), 'bar');\n" +
        "assertTrue(t.get(k1) === void 0);\n" +
        "assertTrue(t.get(k4) === void 0);");
    rewriteAndExecute(
        "var t1 = cajita.newTable(true);" +
        "var t2 = cajita.newTable(true);" +
        "var k = {};" +
        "t1.set(k, 'foo');" +
        "t2.set(k, 'bar');" +
        "assertEquals(t1.get(k), 'foo');" +
        "assertEquals(t2.get(k), 'bar');" +
        "t1.set(k, void 0);" +
        "assertTrue(t1.get(k) === void 0);" +
        "assertEquals(t2.get(k), 'bar');");
    rewriteAndExecute(
        "var t1 = cajita.newTable();" +
        "var t2 = cajita.newTable();" +
        "var k = {};" +
        "t1.set(k, 'foo');" +
        "t2.set(k, 'bar');" +
        "assertEquals(t1.get(k), 'foo');" +
        "assertEquals(t2.get(k), 'bar');" +
        "t1.set(k, void 0);" +
        "assertTrue(t1.get(k) === void 0);" +
        "assertEquals(t2.get(k), 'bar');");
  }

  /**
   * Although the golden output here tests many extraneous things, the point of
   * this test is to see that various anonymous functions in the original are
   * turned into named functions -- named after the variable or property
   * they are initializing.
   * <p>
   * The test is structured as as a call stack resulting in a breakpoint.
   * If executed, for example, in the testbed applet with Firebug enabled,
   * one should see the stack of generated function names.
   */
  public final void testFuncNaming() throws Exception {
    checkSucceeds(
        ""
        + "function foo(){debugger;}"
        + "var x = {bar: function() {foo();}};"
        + "x.baz = function(){x.bar();};"
        + "var zip = function(){x.baz();};"
        + "var zap;"
        + "zap = function(){zip();};"
        + "zap();",

        ""
        + "var x, x0___, zip, zap;"
        + "function foo() { debugger; }"
        + "foo.FUNC___ = 'foo';"
        + "x = ___.iM(['bar', (function () {"
        + "  function bar$_lit() { foo.CALL___(); }"
        + "  return ___.markFuncFreeze(bar$_lit, 'bar$_lit');"
        + "})()]);"
        + "x0___ = (function () {"
        + "  function baz$_meth() {"
        + "    x.bar_canCall___? x.bar(): ___.callPub(x, 'bar', []);"
        + "  }"
        + "  return ___.markFuncFreeze(baz$_meth, 'baz$_meth');"
        + "})(), x.baz_canSet___ === x? (x.baz = x0___) : "
        + "                             ___.setPub(x, 'baz', x0___);"
        + "zip = (function () {"
        + "  function zip$_var() {"
        + "    x.baz_canCall___ ? x.baz() : ___.callPub(x, 'baz', []);"
        + "  }"
        + "  return ___.markFuncFreeze(zip$_var, 'zip$_var');"
        + "})();"
        + "zap = (function () {"
        + "  function zap$_var() { zip.CALL___(); }"
        + "  return ___.markFuncFreeze(zap$_var, 'zap$_var');"
        + "})();"
        + "zap.CALL___();");
  }

  @FailureIsAnOption
  public final void testRecordInheritance() throws Exception {
    rewriteAndExecute(
        "var x = {a: 8};" +
        "var y = cajita.beget(x);" +
        "testImports.y = y;",

        // TODO(erights): Fix when bug 956 is fixed.
        "assertTrue(cajita.canReadPub(y, 'a'));",
        "");
  }

  /**
   * Tests that properties that the global object inherits from
   * <tt>Object.prototype</tt> are not readable as global
   * variables.
   */
  public final void testNoPrototypicalImports() throws Exception {
    rewriteAndExecute(
        "var x;" +
        "try { x = toString; } catch (e) {}" +
        "if (x) { cajita.fail('Inherited global properties are readable'); }");
  }

  /**
   * Tests the static module loading
   */
  public final void testModule() throws Exception {
    // TODO: Refactor the test cases so that we can use CajitaModuleRewriter
    // for all tests
    // CajitaModuleRewriter only accepts an UncajoledModule, so it does not work
    // for those tests that run against other ParseTreeNodes
    final CajitaModuleRewriter moduleRewriter = new CajitaModuleRewriter(
        TestBuildInfo.getInstance(), new TestUriFetcher(), false, mq);

    setRewriter(new Rewriter(mq, true, false) {{
      addRule(new Rule("UncajoledModule", this) {
        @Override
        @RuleDescription(
            name="cajoledModule",
            synopsis="a cajoled module envelope",
            reason="",
            matches="<UncajoledModule>",
            substitutes="<CajoledModule>")
        public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
          CajitaRewriter cr = new CajitaRewriter(
              is.getUri(), moduleRewriter.getModuleManager(), false);
          return moduleRewriter.rewrite(Collections.singletonList(
              (CajoledModule) cr.expand(node, scope)));
        }
      });
    }});

    rewriteAndExecute(
        "var r = load('foo/testPrimordials')({}); "
        + "assertEquals(r, 9);");

    rewriteAndExecute(
        "var m = load('foo/testPrimordials'); "
        + "assertEquals('com.google.caja', m.cajolerName);"
        + "assertEquals('testBuildVersion', m.cajolerVersion);"
        + "assertEquals(0, m.cajoledDate);"
        + "assertEquals(void 0, m.includedModules);"
        + "assertThrows(function() { m.cajolerName = 'bar'; });"
        + "assertThrows(function() { m.includedModules = 'bar'; });"
        + "assertThrows(function() { m.includedModules.foo = 'bar'; });"
        + "assertThrows(function() { m.foo = 'bar'; });"
        );

    rewriteAndExecute(
        "var r = load('foo/b')({x: 6, y: 3}); "
        + "assertEquals(r, 11);");

    rewriteAndExecute(
        "var r1 = load('foo/b')({x: 6, y: 3}); "
        + "var r2 = load('foo/b')({x: 1, y: 2}); "
        + "var r3 = load('c')({x: 2, y: 6}); "
        + "var r = r1 + r2 + r3; "
        + "assertEquals(r, 24);");

    rewriteAndExecute(
        "var m = load('foo/b');"
        + "var s = m.cajolerName;"
        + "assertEquals('com.google.caja', s);");

    checkAddsMessage(
        new UncajoledModule(js(fromString("var m = load('foo/c');"))),
        RewriterMessageType.MODULE_NOT_FOUND,
        MessageLevel.FATAL_ERROR);

    checkAddsMessage(
        new UncajoledModule(js(fromString("var s = 'c'; var m = load(s);"))),
        RewriterMessageType.CANNOT_LOAD_A_DYNAMIC_CAJITA_MODULE,
        MessageLevel.FATAL_ERROR);
  }

  /**
   * Tests that Error objects are frozen on being caught by a Cajita catch.
   *
   * See issue 1097, issue 1038,
   *     and {@link CommonJsRewriterTestCase#testErrorTaming()}}.
   */
  public final void testErrorFreeze() throws Exception {
    rewriteAndExecute(
            "try {" +
            "  throw new Error('foo');" +
            "} catch (ex) {" +
            "  assertTrue(cajita.isFrozen(ex));" +
            "}");
  }

  public final void testUnderscore() throws Exception {
    checkFails(
        "var o = { p__: 1 };",
        "Properties cannot end in \"__\"");
  }

  /**
   *
   */
  public final void testObjectFreeze() throws Exception {
    rewriteAndExecute( // records can be frozen
        "var r = Object.freeze({});" +
        "assertThrows(function(){r.foo = 8;});");
    rewriteAndExecute( // anon functions are already frozen
        "var f = function(){};" +
        "assertThrows(function(){f.foo = 8;});");
    rewriteAndExecute( // anon functions can be frozen
        "var f = Object.freeze(function(){});" +
        "assertThrows(function(){f.foo = 8;});");
    rewriteAndExecute( // constructed objects cannot be frozen
        "function Point(x,y) {" +
        "  this.x = x;" +
        "  this.y = y;" +
        "}" +
        "___.markCtor(Point, Object, 'Point');" +
        "testImports.pt = new Point(3,5);" +
        "___.grantSet(testImports.pt, 'x');",

        "pt.x = 8;" +
        "assertThrows(function(){Object.freeze(pt);});",

        "");
  }

  /**
   * Tests that neither Cajita nor Valija code can cause a privilege
   * escalation by calling a tamed exophoric function with null as the
   * this-value.
   * <p>
   * The uncajoled branch of the tests below establish that a null does cause
   * a privilege escalation for normal non-strict JavaScript.
   */
  public final void testNoPrivilegeEscalation() throws Exception {
    rewriteAndExecute("",
        "assertTrue([].valueOf.call(null) === cajita.USELESS);",
        "assertTrue([].valueOf.call(null) === this);");
    rewriteAndExecute("",
        "assertTrue([].valueOf.apply(null, []) === cajita.USELESS);",
        "assertTrue([].valueOf.apply(null, []) === this);");
    rewriteAndExecute("",
        "assertTrue([].valueOf.bind(null)() === cajita.USELESS);",
        "assertTrue([].valueOf.bind(null)() === this);");
  }

  /**
   * Tests that the special handling of null on tamed exophora works.
   * <p>
   * The reification of tamed exophoric functions contains
   * special cases for when the first argument to call, bind, or apply
   * is null or undefined, in order to protect against privilege escalation.
   * {@code #testNoPrivilegeEscalation()} tests that we do prevent the
   * privilege escalation. Here, we test that this special case preserves
   * correct functionality.
   */
  public final void testTamedXo4aOkOnNull() throws Exception {
    rewriteAndExecute("this.foo = 8;",

        "var x = cajita.beget(cajita.USELESS);" +
        "assertFalse(({foo: 7}).hasOwnProperty.call(null, 'foo'));" +
        "assertTrue(cajita.USELESS.isPrototypeOf(x));" +
        "assertTrue(({foo: 7}).isPrototypeOf.call(null, x));",

        "assertTrue(({}).hasOwnProperty.call(null, 'foo'));" +
        "assertFalse(({bar: 7}).hasOwnProperty.call(null, 'bar'));");
    rewriteAndExecute("this.foo = 8;",

        "var x = cajita.beget(cajita.USELESS);" +
        "assertFalse(({foo: 7}).hasOwnProperty.apply(null, ['foo']));" +
        "assertTrue(cajita.USELESS.isPrototypeOf(x));" +
        "assertTrue(({foo: 7}).isPrototypeOf.apply(null, [x]));",

        "assertTrue(({}).hasOwnProperty.apply(null, ['foo']));" +
        "assertFalse(({bar: 7}).hasOwnProperty.apply(null, ['bar']));");
    rewriteAndExecute("this.foo = 8;",

        "var x = cajita.beget(cajita.USELESS);" +
        "assertFalse(({foo: 7}).hasOwnProperty.bind(null)('foo'));" +
        "assertTrue(cajita.USELESS.isPrototypeOf(x));" +
        "assertTrue(({foo: 7}).isPrototypeOf.bind(null)(x));",

        "assertTrue(({}).hasOwnProperty.bind(null)('foo'));" +
        "assertFalse(({bar: 7}).hasOwnProperty.bind(null)('bar'));");
  }

  /**
   * Tests that the apparent [[Class]] of the tamed JSON object is 'JSON', as
   * it should be according to ES5.
   *
   * See issue 1086
   */
  public final void testJSONClass() throws Exception {
    // In neither Cajita nor Valija is it possible to mask the real toString()
    // when used implicitly by a primitive JS coercion rule.
    rewriteAndExecute("assertTrue(''+JSON === '[object Object]');");
  }

  /**
   * Tests that an inherited <tt>*_canSet___</tt> fastpath flag does not enable
   * bogus writability.
   * <p>
   * See <a href="http://code.google.com/p/google-caja/issues/detail?id=1052"
   * >issue 1052</a>.
   */
  public final void testNoCanSetInheritance() throws Exception {
    rewriteAndExecute(
            "(function() {" +
            "  var a = {};" +
            "  var b = cajita.freeze(cajita.beget(a));" +
            "  a.x = 8;" +
            "  assertThrows(function(){b.x = 9;});" +
            "  assertEquals(b.x, 8);" +
            "})();");
  }

  /**
   *
   */
  public final void testFeralTameBoundary() throws Exception {
    rewriteAndExecute(
            "function forbidden() { return arguments; }" +
            "function xo4ic(f) {" +
            "  return ___.callPub(f, 'call', [___.USELESS, this]);" +
            "}" +
            "___.markXo4a(xo4ic);" +

            "function innocent(f) { return f(this); }" +
            "___.markInnocent(innocent);" +

            "function simple(f) {" +
            "  return ___.callPub(f, 'call', [___.USELESS, simple]); " +
            "}" +
            "___.markFuncFreeze(simple);" +

            "function Point(x,y) {" +
            "  this.x = x;" +
            "  this.y = y;" +
            "}" +
            "Point.prototype.getX = function() { return this.x; };" +
            "Point.prototype.getY = function() { return this.y; };" +
            "Point.prototype.badness = function(str) { return eval(str); };" +
            "Point.prototype.welcome = function(visitor) {" +
            "  return visitor(this.x, this.y); " +
            "};" +
            "___.markCtor(Point, Object, 'Point');" +
            "___.grantGenericMethod(Point.prototype, 'getX');" +
            "___.grantTypedMethod(Point.prototype, 'getY');" +
            "___.grantInnocentMethod(Point.prototype, 'welcome');" +

            "var pt = new Point(3,5);" +

            "function eight() { return 8; }" +
            "function nine() { return 9; }" +
            "___.markFuncFreeze(nine);" +
            "___.tamesTo(eight, nine);" +

            "var funcs = [[simple, Point, pt], forbidden, " +
            "             xo4ic, innocent, eight];" +
            "___.freeze(funcs[0]);" +
            "funcs[5] = funcs;" +
            "var f = { " +
            "  forbidden: forbidden," +
            "  xo4ic: xo4ic," +
            "  innocent: innocent," +
            "  simple: simple," +
            "  Point: Point," +
            "  pt: pt," +
            "  eight: eight," +
            "  funcs: funcs" +
            "};" +
            "f.f = f;" +
            "funcs[6] = f;" +
            "testImports.f = f;" + // purposeful safety violation
            "testImports.t = ___.tame(f);",


            "assertFalse(f === t);" +
            "assertFalse('forbidden' in t);" +
            "assertFalse(f.xo4ic === t.xo4ic);" +
            "assertFalse(f.innocent === t.innocent);" +
            "assertTrue(f.simple === t.simple);" +
            "assertTrue(f.Point === t.Point);" +
            "assertTrue(f.pt === t.pt);" +
            "assertFalse('x' in t.pt);" +
            "assertFalse('y' in t.pt);" +
            "assertTrue('getX' in t.pt);" +
            "assertTrue('getY' in t.pt);" +
            "assertFalse('badness' in t.pt);" +
            "assertTrue('welcome' in t.pt);" +
            "assertFalse(f.eight === t.eight);" +
            "assertFalse(f.funcs === t.funcs);" +
            "assertTrue(f.funcs[0][0] === t.funcs[0][0]);" +
            "assertTrue(f.funcs[0] === t.funcs[0]);" +
            "assertTrue('1' in t.funcs);" +
            "assertTrue(t.funcs[1] === void 0);" +
            "assertTrue(t.funcs === t.funcs[5]);" +
            "assertTrue(t === t.funcs[6]);" +
            "assertTrue(t === t.f);" +

            "var lastArg = void 0;" +
            "function capture(arg) { return lastArg = arg; }" +

            "var lastResult = t.xo4ic.call(void 0, capture);" +
            "assertTrue(lastArg === cajita.USELESS);" +
            "assertTrue(lastResult === cajita.USELESS);" +

            "lastResult = t.innocent.call(void 0, capture);" +
            "assertTrue(lastArg === cajita.USELESS);" +
            "assertTrue(lastResult === cajita.USELESS);" +

            "lastResult = t.innocent.apply(void 0, [capture]);" +
            "assertTrue(lastArg === cajita.USELESS);" +
            "assertTrue(lastResult === cajita.USELESS);" +

            "lastResult = t.simple(capture);" +
            "assertTrue(lastArg === t.simple);" +
            "assertTrue(lastResult === t.simple);" +

            "assertTrue(t.pt.getX() === 3);" +
            "assertTrue(t.pt.getY() === 5);" +
            "var getX = t.pt.getX;" +
            "var getY = t.pt.getY;" +
            "assertTrue(cajita.isPseudoFunc(getX));" +
            "assertTrue(cajita.isPseudoFunc(getY));" +
            "assertTrue(getX.call(t.pt) === 3);" +
            "assertTrue(getY.call(t.pt) === 5);" +
            "var nonpt = {x: 33, y: 55};" +
            "assertTrue(getX.call(nonpt) === 33);" +
            "assertThrows(function() { getY.call(nonpt); });" +

            "function visitor(x, y) {" +
            "  assertTrue(x === 3);" +
            "  assertTrue(y === 5);" +
            "}" +
            "t.pt.welcome(visitor);" +

            "assertTrue(t.eight() === 9);" +
            "lastResult = t.innocent.call(void 0, t.eight);" +
            "assertTrue(lastResult === 8);" +

            "lastResult = t.innocent.apply(void 0, [t.eight]);" +
            "assertTrue(lastResult === 8);",


            "");
  }

  @Override
  protected Object executePlain(String caja) throws IOException {
    mq.getMessages().clear();
    return RhinoTestBed.runJs(
        new Executor.Input(
            getClass(), "../../../../../js/json_sans_eval/json_sans_eval.js"),
        new Executor.Input(getClass(), "/com/google/caja/cajita.js"),
        new Executor.Input(
            getClass(), "../../../../../js/jsunit/2.2/jsUnitCore.js"),
        new Executor.Input(caja, getName() + "-uncajoled"));
  }

  @Override
  protected Object rewriteAndExecute(String pre, String caja, String post)
      throws IOException, ParseException {
    mq.getMessages().clear();

    List<Statement> children = Lists.newArrayList();
    children.add(js(fromString(caja, is)));
    String cajoledJs = render(rewriteTopLevelNode(
        new UncajoledModule(new Block(FilePosition.UNKNOWN, children))));

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
          .append("testImports." + f + " = ___.markFuncFreeze(" + f + ");")
          .append("___.grantRead(testImports, '" + f + "');");
    }
    importsSetup.append("___.getNewModuleHandler().setImports(testImports);");

    Object result = RhinoTestBed.runJs(
        new Executor.Input(
            getClass(), "/com/google/caja/plugin/console-stubs.js"),
        new Executor.Input(
            getClass(), "../../../../../js/json_sans_eval/json_sans_eval.js"),
        new Executor.Input(getClass(), "/com/google/caja/cajita.js"),
        new Executor.Input(
            getClass(), "../../../../../js/jsunit/2.2/jsUnitCore.js"),
        new Executor.Input(
            getClass(), "/com/google/caja/log-to-console.js"),
        new Executor.Input(
            importsSetup.toString(),
            getName() + "-test-fixture"),
        new Executor.Input(pre, getName()),
        // Load the cajoled code.
        new Executor.Input(cajoledJs, getName() + "-cajoled"),
        new Executor.Input(post, getName()),
        // Return the output field as the value of the run.
        new Executor.Input(
            "___.getNewModuleHandler().getLastValue();", getName()));

    assertNoErrors();
    return result;
  }
}
