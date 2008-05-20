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

import static com.google.caja.parser.quasiliteral.QuasiBuilder.substV;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.plugin.SyntheticNodes;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.RhinoTestBed;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * @author ihab.awad@gmail.com
 */
public class DefaultCajaRewriterTest extends RewriterTestCase {

  /**
   * Welds together a string representing the repeated pattern of expected test output for
   * assigning to an outer variable.
   *
   * @author erights@gmail.com
   */
  public static String weldSetImports(
      String varName, String tempValue, String value) {
    return
        tempValue + " = " + value + "," +
        "    IMPORTS___." + varName + "_canSet___ ?" +
        "    IMPORTS___." + varName + " = " + tempValue + ":" +
        "    ___.setPub(IMPORTS___, '" + varName + "', " + tempValue + ")";
  }

  private static String weldSetPub(String obj, String varName, String value, String tempObj, String tempValue) {
    return weldSet(obj, varName, value, "Pub", tempObj, tempValue);
  }

  private static String weldSetProp(String varName, String value, String tempValue) {
    return
        tempValue + " = " + value + "," +
        "    t___." + varName + "_canSet___ ?" +
        "    t___." + varName + " = " + tempValue + ":" +
        "    ___.setProp(t___, '" + varName + "', " + tempValue + ")";
  }

  private static String weldSet(String obj, String varName, String value, String pubOrProp, String tempObj, String tempValue) {
    return
        tempObj + " = " + obj + "," +
        tempValue + " = " + value + "," +
        "    " + tempObj + "." + varName + "_canSet___ ?" +
        "    " + tempObj + "." + varName + " = " + tempValue + ":" +
        "    ___.set" + pubOrProp + "(" + tempObj + ", '" + varName + "', " + tempValue + ")";
  }

  /**
   * Welds together a string representing the repeated pattern of expected test output for
   * reading an outer variable.
   *
   * @author erights@gmail.com
   */
  public static String weldReadImports(String varName) {
    return weldReadImports(varName, true);
  }

  private static String weldReadImports(String varName, boolean flag) {
    return
        "(IMPORTS___." + varName + "_canRead___ ?" +
        "    IMPORTS___." + varName + ":" +
        "    ___.readPub(" +
        "        IMPORTS___, '" + varName + "'" + (flag ? ", true" : "") + "))";
  }

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

  public void testPrimordialObjectExtension() throws Exception {
    assertConsistent(
        "caja.extend(Object, {x:1});" +
        "({}).x;");
    assertConsistent(
        "caja.extend(Number, {inc: function(){return this.valueOf() + 1;}});" +
        "(2).inc();");
    assertConsistent(
        "caja.extend(Array, {size: function(){return this.length + 1;}});" +
        "([5, 6]).size();");
    assertConsistent(
        "caja.extend(Boolean, {not: function(){return !this.valueOf();}});" +
        "(true).not();");
    assertConsistent(
        "function foo() {this;}" +
        "caja.def(foo, Object);" +
        "function bar() {this;}" +
        "caja.def(bar, foo);" +
        "b=new bar;" +
        "caja.extend(Object, {x:1});" +
        "b.x;");
  }

  public void testConstructorProperty() throws Exception {
    assertConsistent(
        "pkg = {};" +
        "(function (){" +
        "  function Foo(x) {" +
        "    this.x_ = x;" +
        "  };" +
        "  Foo.prototype.getX = function(){ return this.x_; };" +
        "  pkg.Foo = Foo;" +
        "})();" +
        "foo = new pkg.Foo(2);" +
        "foo.getX();");
  }

  public void testAttachedMethod() throws Exception {
    // See also <tt>testAttachedMethod()</tt> in <tt>HtmlCompiledPluginTest</tt>
    // to check cases where calling the attached method should fail.
    assertConsistent(
        "function Foo(){" +
        "  this.f = function (){this.x_ = 1;};" +
        "  this.getX = function (){return this.x_;};" +
        "}" +
        "foo = new Foo();" +
        "foo.f();" +
        "foo.getX();");
    assertConsistent(
        "function Foo(){}" +
        "Foo.prototype.setX = function(x) { this.x_ = x; };" +
        "Foo.prototype.getX = function() { return this.x_; };" +
        "Foo.prototype.y = 1;" +
        "foo=new Foo;" +
        "foo.setX(5);" +
        "''+foo.y+foo.getX();");
    assertConsistent(
        "function Foo(){}" +
        "caja.def(Foo, Object, {" +
        "  setX: function(x) { this.x_ = x; }," +
        "  getX: function() { return this.x_; }," +
        "  y: 1" +
        "});" +
        "foo=new Foo;" +
        "foo.setX(5);" +
        "''+foo.y+foo.getX();");
    assertConsistent(
        "function Foo(){ this.gogo(); }" +
        "Foo.prototype.gogo = function() { this.setX = function(x) { this.x_ = x; }; };" +
        "Foo.prototype.getX = function() { return this.x_; };" +
        "Foo.prototype.y = 1;" +
        "foo=new Foo;" +
        "foo.setX(5);" +
        "''+foo.y+foo.getX();");
    assertConsistent(
        "function Foo(){ this.gogo(); }" +
        "caja.def(Foo, Object, {" +
        "  gogo: function() { this.setX = function(x) { this.x_ = x; }; }," +
        "  getX: function() { return this.x_; }," +
        "  y: 1" +
        "});" +
        "foo=new Foo;" +
        "foo.setX(5);" +
        "''+foo.y+foo.getX();");
    assertConsistent(
        "function Foo() { this.gogo(); }" +
        "Foo.prototype.gogo = function () { " +
        "  this.Bar = function Bar(x){ " +
        "    this.x_ = x; " +
        "    this.getX = function() { return this.x_; };" +
        "  }; " +
        "};" +
        "foo = new Foo;" +
        "Bar = foo.Bar;" +
        "bar = new Bar(5);" +
        "bar.getX();");
    assertConsistent(
        "function Foo() { this.gogo(); }" +
        "Foo.prototype.gogo = function () { " +
        "  function Bar(x){ " +
        "    this.x_ = x; " +
        "  }" +
        "  Bar.prototype.getX = function () { return this.x_; };" +
        "  this.Bar = Bar;" +
        "};" +
        "foo = new Foo;" +
        "Bar = foo.Bar;" +
        "bar = new Bar(5);" +
        "bar.getX();");
    checkFails(
        "function (){" +
        "  this.x_ = 1;" +
        "}",
        "Public properties cannot end in \"_\"");
    checkFails(
        "function Foo(){}" +
        "Foo.prototype.m = function () {" +
        "  var y = function() {" +
        "    var z = function() {" +
        "      this.x_ = 1;" +
        "    }" +
        "  }" +
        "}",
        "Public properties cannot end in \"_\"");
  }
  
  ////////////////////////////////////////////////////////////////////////
  // Handling of synthetic nodes
  ////////////////////////////////////////////////////////////////////////

  public void testSyntheticIsUntouched() throws Exception {
    ParseTreeNode input = js(fromString("function foo() { this; arguments; }"));
    setTreeSynthetic(input);
    checkSucceeds(input, input);
  }

  public void testSyntheticNestedIsExpanded() throws Exception {
    ParseTreeNode innerInput = js(fromString("function foo() {}"));
    ParseTreeNode input = ParseTreeNodes.newNodeInstance(
        Block.class,
        null,
        Collections.singletonList(innerInput));
    setSynthetic(input);
    ParseTreeNode expectedResult = js(fromString(
        "IMPORTS___.foo = undefined;" +
        "var x0___;" +
        "{" +
        weldSetImports("foo", "x0___", "___.simpleFunc(function foo() {})") +
        ";;}"));
    checkSucceeds(input, expectedResult);
  }

  public void testSyntheticNestedFunctionIsExpanded() throws Exception {
    // This test checks that a synthetic function, as is commonly generated by Caja
    // to wrap JavaScript event handlers declared in HTML, is rewritten correctly.
    ParseTreeNode innerBlock = js(fromString("foo = 3;"));
    // By creating the function using substV(), the function nodes are all
    // synthetic. But the stuff inside it -- 'innerBlock' -- is not.
    ParseTreeNode input = substV(
        "{ function f() { @blockStmts*; } }",
        "blockStmts", innerBlock);
    // We expect the stuff in 'innerBlock' to be expanded, *but* we expect the
    // rewriter to be unaware of the enclosing function scope, so the temporary
    // variables generated by expanding 'innerBlock' spill out and get declared
    // outside the function rather than inside it.
    ParseTreeNode expectedResult = js(fromString(
        "  var x0___;"  // Temporary is declared up here ...
        + "function f() {"
        + "  "  // ... not down here!
        + "  " + weldSetImports("foo", "x0___", "3") + ";"
        + "}"));
    checkSucceeds(input, expectedResult);
  }

  ////////////////////////////////////////////////////////////////////////
  // Handling of nested blocks
  ////////////////////////////////////////////////////////////////////////

  public void testNestedBlockWithFunction() throws Exception {
    checkSucceeds(
        "{ function foo() {} }",
        "IMPORTS___.foo  = undefined;" +
        "var x0___;" +
        "{" +
        weldSetImports("foo", "x0___", "___.simpleFunc(function foo() {})") +
        ";;}");
  }

  public void testNestedBlockWithVariable() throws Exception {
    checkSucceeds(
        "{ var x = y; }",
        "var x0___;" +
        "{" + weldSetImports("x", "x0___", weldReadImports("y")) + "}");
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

  public void testForeach() throws Exception {
    // TODO(ihab.awad): Refactor some of these tests to be functional, rather than golden.
    checkSucceeds(
        "1; for (var k in x) { k; }",
        "var x0___;" +
        "var x1___;" +
        "var x2___;" +
        "1;" +
        "{" +
        "  x0___ = " + weldReadImports("x") + ";" +
        "  for (x1___ in x0___) {" +
        "    if (___.canEnumPub(x0___, x1___)) {" +
        "      " + weldSetImports("k", "x2___", "x1___") + ";" +
        "      { " + weldReadImports("k") + "; }" +
        "    }" +
        "  }" +
        "}");
    checkSucceeds(
        "2; try { } catch (e) { for (var k in x) { k; } }",
        "var x0___;" +
        "var x1___;" +
        "var x2___;" +
        "2;" +
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    {" +
        "      x0___ = " + weldReadImports("x") + ";" +
        "      for (x1___ in x0___) {" +
        "        if (___.canEnumPub(x0___, x1___)) {" +
        "          " + weldSetImports("k", "x2___", "x1___") + ";" +
        "          { " + weldReadImports("k") + "; }" +
        "        }" +
        "      }" +
        "    }" +
        "  }" +
        "}");
    checkSucceeds(
        "3; function() {" +
        "  for (var k in x) { k; }" +
        "};",
        "3; ___.primFreeze(___.simpleFunc(function() {" +
        "  var x0___;" +
        "  var x1___;" +
        "    var k;" +
        "  {" +
        "    x0___ = " + weldReadImports("x") + ";" +
        "    for (x1___ in x0___) {" +
        "      if (___.canEnumPub(x0___, x1___)) {" +
        "        k = x1___;" +
        "        { k; }" +
        "      }" +
        "    }" +
        "  }" +
        "}));");
   checkSucceeds(
        "4; function() {" +
        "  for (var k in x) k;" +
        "};",
        "4; ___.primFreeze(___.simpleFunc(function() {" +
        "  var x0___;" +
        "  var x1___;" +
        "  var k;" +
        "  {" +
        "    x0___ = " + weldReadImports("x") + ";" +
        "    for (x1___ in x0___) {" +
        "      if (___.canEnumPub(x0___, x1___)) {" +
        "        k = x1___;" +
        "        k;" +
        "      }" +
        "    }" +
        "  }" +
        "}));");
    checkSucceeds(
        "5; function() {" +
        "  for (z[0] in x) { z; }" +
        "};",
        "5; ___.primFreeze(___.simpleFunc(function() {" +
        "  var x0___;" +
        "  var x1___;" +
        "  {" +
        "    x0___ = " + weldReadImports("x") + ";" +
        "    for (x1___ in x0___) {" +
        "      if (___.canEnumPub(x0___, x1___)) {" +
        "        ___.setPub(" + weldReadImports("z") + ", 0, x1___);" +
        "        { " + weldReadImports("z") + "; }" +
        "      }" +
        "    }" +
        "  }" +
        "}));");
    checkSucceeds(
        "6; for (k in x) { k; }",
        "var x0___;" +
        "var x1___;" +
        "var x2___;" +
        "6;" +
        "{" +
        "  x0___ = " + weldReadImports("x") + ";" +
        "  for (x1___ in x0___) {" +
        "    if (___.canEnumPub(x0___, x1___)) {" +
        "      " + weldSetImports("k", "x2___", "x1___") + ";" +
        "      { " + weldReadImports("k") + "; }" +
        "    }" +
        "  }" +
        "}");
    checkSucceeds(
        "7; function() {" +
        "  for (k in x) { k; }" +
        "};",
        "7;" +
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var x0___;" +
        "  var x1___;" +
        "  var x2___;" +
        "  {" +
        "    x0___ = " + weldReadImports("x") + ";" +
        "    for (x1___ in x0___) {" +
        "      if (___.canEnumPub(x0___, x1___)) {" +
        "        " + weldSetImports("k", "x2___", "x1___") + ";" +
        "        { " + weldReadImports("k") + "; }" +
        "      }" +
        "    }" +
        "  }" +
        "}));");
    checkSucceeds(
        "8; function() {" +
        "  var k;" +
        "  for (k in x) { k; }" +
        "};",
        "8;" +
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var x0___;" +
        "  var x1___;" +
        "  var k;" +
        "  {" +
        "    x0___ = " + weldReadImports("x") + ";" +
        "    for (x1___ in x0___) {" +
        "      if (___.canEnumPub(x0___, x1___)) {" +
        "        k = x1___;" +
        "        { k; }" +
        "      }" +
        "    }" +
        "  }" +
        "}));");
    checkSucceeds(
        "11; function foo() {" +
        "  for (var k in this) { k; }" +
        "}",
        "IMPORTS___.foo = undefined;" +
        "var x0___;" +
        weldSetImports(
            "foo",
            "x0___",
            "(function () {" +
            "  ___.splitCtor(foo, foo_init___);" +
            "  function foo(var_args) {" +
            "    return new foo.make___(arguments);" +
            "  }" +
            "  function foo_init___() {" +
            "    var t___ = this;" +
            "    var x0___;" +
            "    var x1___;" +
            "    var k;" +
            "    {" +
            "      x0___ = t___;" +
            "      for (x1___ in x0___) {" +
            "        if (___.canEnumProp(x0___, x1___)) {" +
            "          k = x1___;" +
            "          { k }" +
            "        }" +
            "      }" +
            "    }" +
            "  }" +
            "  return foo;" +
            "})()") +
        ";11;;");
    checkSucceeds(
        "12; for (var k in this) { k; }",
        "var x0___;" +
        "var x1___;" +
        "var x2___;" +
        "12;" +
        "{" +
        "  x0___ = IMPORTS___;" +
        "  for (x1___ in x0___) {" +
        "    if (___.canEnumPub(x0___, x1___)) {" +
        "      " + weldSetImports("k", "x2___", "x1___") + ";" +
        "      { " + weldReadImports("k") + "; }" +
        "    }" +
        "  }" +
        "}");
    checkSucceeds(
        "13; function foo() {" +
        "  for (k in this) { k; }" +
        "}",
        "IMPORTS___.foo = undefined;" +
        "var x0___;" +
        weldSetImports(
            "foo",
            "x0___",
            "(function () {" +
            "  ___.splitCtor(foo, foo_init___);" +
            "  function foo(var_args) {" +
            "    return new foo.make___(arguments);" +
            "  }" +
            "  function foo_init___() {" +
            "    var t___ = this;" +
            "    var x0___;" +
            "    var x1___;" +
            "    var x2___;" +
            "    {" +
            "      x0___ = t___;" +
            "      for (x1___ in x0___) {" +
            "        if (___.canEnumProp(x0___, x1___)) {" +
            "          " + weldSetImports("k", "x2___", "x1___") + ";" +
            "          { " + weldReadImports("k") + "; }" +
            "        }" +
            "      }" +
            "    }" +
            "  }" +
            "  return foo;" +
           "})()") +
        ";13;;");
    checkSucceeds(
        "14; function foo() {" +
        "  var k;" +
        "  for (k in this) { k; }" +
        "}",
        "IMPORTS___.foo = undefined;" +
        "var x0___;" +
        weldSetImports(
            "foo",
            "x0___",
            "(function () {" +
            "  ___.splitCtor(foo, foo_init___);" +
            "  function foo(var_args) {" +
            "    return new foo.make___(arguments);" +
            "  }" +
            "  function foo_init___() {" +
            "    var t___ = this;" +
            "    var x0___;" +
            "    var x1___;" +
            "    var k;" +
            "    {" +
            "      x0___ = t___;" +
            "      for (x1___ in x0___) {" +
            "        if (___.canEnumProp(x0___, x1___)) {" +
            "          k = x1___;" +
            "          { k; }" +
            "        }" +
            "      }" +
            "    }" +
            "  }" +
            "  return foo;" +
            "})()") +
        ";14;;");
    assertAddsMessage(
        "function f() { for (var x__ in a) {} }",
        RewriterMessageType.VARIABLES_CANNOT_END_IN_DOUBLE_UNDERSCORE,
        MessageLevel.FATAL_ERROR);
  }

  public void testTryCatch() throws Exception {
    checkSucceeds(
        "try {" +
        "  e;" +
        "  x;" +
        "} catch (e) {" +
        "  e;" +
        "  y;" +
        "}",
        "try {" +
        "  " + weldReadImports("e") + ";" +
        "  " + weldReadImports("x") + ";" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    e;" +
        "    " + weldReadImports("y") + ";" +
        "  }" +
        "}");
    assertConsistent(
        "var handled = false;" +
        "try {" +
        "  throw null;" +
        "} catch (ex) {" +
        "  assertEquals(null, ex);" +  // Right value in ex.
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");  // Control reached and left the catch block.
    assertConsistent(
        "var handled = false;" +
        "try {" +
        "  throw undefined;" +
        "} catch (ex) {" +
        "  assertEquals(undefined, ex);" +
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");
    assertConsistent(
        "var handled = false;" +
        "try {" +
        "  throw true;" +
        "} catch (ex) {" +
        "  assertEquals(true, ex);" +
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");
    assertConsistent(
        "var handled = false;" +
        "try {" +
        "  throw 37639105;" +
        "} catch (ex) {" +
        "  assertEquals(37639105, ex);" +
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");
    assertConsistent(
        "var handled = false;" +
        "try {" +
        "  throw 'panic';" +
        "} catch (ex) {" +
        "  assertEquals('panic', ex);" +
        "  handled = true;" +
        "}" +
        "assertTrue(handled);");
    assertConsistent(
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
    checkSucceeds(
        "try {" +
        "  e;" +
        "  x;" +
        "} catch (e) {" +
        "  e;" +
        "  y;" +
        "} finally {" +
        "  e;" +
        "  z;" +
        "}",
        "try {" +
        "  " + weldReadImports("e") + ";" +
        "  " + weldReadImports("x") + ";" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    e;" +
        "    " + weldReadImports("y") + ";" +
        "  }" +
        "} finally {" +
        "  " + weldReadImports("e") + ";" +
        "  " + weldReadImports("z") + ";" +
        "}");
  }

  public void testTryFinally() throws Exception {
    checkSucceeds(
        "try {" +
        "  x;" +
        "} finally {" +
        "  z;" +
        "}",
        "try {" +
        "  " + weldReadImports("x") + ";" +
        "} finally {" +
        "  " + weldReadImports("z") + ";" +
        "}");
  }

  public void testVarArgs() throws Exception {
    checkSucceeds(
        "var foo = function() {" +
        "  p = arguments;" +
        "};",
        "var x0___;" +
        weldSetImports(
            "foo",
            "x0___",
            "___.primFreeze(___.simpleFunc(function() {" +
            "  var a___ = ___.args(arguments);" +
            "  var x0___;" +
               weldSetImports("p", "x0___", "a___") +
            "}))"));
  }

  public void testVarThis() throws Exception {
    checkSucceeds(
        "function foo() {" +
        "  p = this;" +
        "}",
        "IMPORTS___.foo = undefined;" +
        "var x0___;" +
        weldSetImports(
            "foo",
            "x0___",
            "(function () {" +
            "  ___.splitCtor(foo, foo_init___);" +
            "  function foo(var_args) {" +
            "    return new foo.make___(arguments);" +
            "  }" +
            "  function foo_init___() {" +
            "    var t___ = this;" +
            "    var x0___;" +
            "    " + weldSetImports("p", "x0___", "t___") + ";" +
            "  }" +
            "  return foo;" +
            "})()") +
        ";;");
    checkSucceeds(
        "this;",
        "IMPORTS___;");
    checkSucceeds(
        "try { } catch (e) { this; }",
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    IMPORTS___;" +
        "  }" +
        "}");
  }

  public void testVarBadSuffix() throws Exception {
    checkFails(
        "function() { foo__; };",
        "Variables cannot end in \"__\"");
    // Make sure *single* underscore is okay
    checkSucceeds(
        "function() { var foo_ = 3; }",
        "___.primFreeze(___.simpleFunc(function() { var foo_ = 3; }))");
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

  public void testVarBadGlobalSuffix() throws Exception {
    checkFails(
        "foo_;",
        "Globals cannot end in \"_\"");
  }

  public void testVarFuncFreeze() throws Exception {
    checkSucceeds(
        "function() {"+
        "  function foo() {}" +
        "  var f = foo;" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo;" +
        "  var foo = ___.simpleFunc(function foo() {});" +
        "  ;" +
        "  var f = ___.primFreeze(foo);" +
        "}));");
    checkSucceeds(
        "function foo() {}" +
        "var f = foo;",
        "IMPORTS___.foo = undefined;" +
        "var x0___;" +
        weldSetImports("foo", "x0___", "___.simpleFunc(function foo() {})") +
        ";" +
        "var x1___;" +
        ";" +
        weldSetImports(
            "f", "x1___", "___.primFreeze(" + weldReadImports("foo") + ")") +
        ";");
  }

  public void testVarGlobal() throws Exception {
    checkSucceeds(
        "foo;",
        weldReadImports("foo"));
    checkSucceeds(
        "function() {" +
        "  foo;" +
        "}",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  " + weldReadImports("foo") + ";" +
        "}));");
    checkSucceeds(
        "function() {" +
        "  var foo;" +
        "  foo;" +
        "}",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo;" +
        "  foo;" +
        "}));");
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
        "___.primFreeze(___.simpleFunc(function() {" +
        "  " + unchanged +
        "}));");
  }

  public void testReadBadSuffix() throws Exception {
    checkFails(
        "x.y__;",
        "Properties cannot end in \"__\"");
  }

  public void testReadGlobalViaThis() throws Exception {
    checkSucceeds(
        "this.x;",
        weldReadImports("x", false) + ";");
    checkSucceeds(
        "try { } catch (e) { this.x; }",
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    " + weldReadImports("x", false) + ";" +
        "  }" +
        "}");
  }

  public void testReadInternal() throws Exception {
    checkSucceeds(
        "function() {" +
        "  function foo() {" +
        "    p = this.x;" +
        "  }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo;" +
        "  var foo = (function () {" +
        "      ___.splitCtor(foo, foo_init___);" +
        "      function foo(var_args) {" +
        "        return new foo.make___(arguments);" +
        "      }" +
        "      function foo_init___() {" +
        "        var t___ = this;" +
        "        var x0___;" +
        "        " + weldSetImports("p", "x0___",
                                    ("t___.x_canRead___" +
                                     " ? t___.x" +
                                     " : ___.readProp(t___, 'x')")) + ";" +
        "      }" +
        "      return foo;" +
        "    })();" +
        "  ;" +
        "}));");
  }

  public void testReadBadInternal() throws Exception {
    checkFails(
        "foo.bar_;",
        "Public properties cannot end in \"_\"");
  }

  public void testReadPublic() throws Exception {
    checkSucceeds(
        "p = foo.p;",
        "var x0___;" +
        "var x1___;" +
        weldSetImports(
            "p",
            "x0___",
            "(x1___ = " + weldReadImports("foo") + "," +
            "  x1___.p_canRead___ ? x1___.p : ___.readPub(x1___, 'p'))"));
  }

  public void testReadIndexGlobal() throws Exception {
    checkSucceeds(
        "this[3];",
        "___.readPub(IMPORTS___, 3);");
    checkSucceeds(
        "try { } catch (e) { this[3]; }",
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    ___.readPub(IMPORTS___, 3);" +
        "  }" +
        "}");
  }

  public void testReadIndexInternal() throws Exception {
    checkSucceeds(
        "function foo() { p = this[3]; }",
        "IMPORTS___.foo = undefined;" +
        "var x0___;" +
        weldSetImports(
            "foo",
            "x0___",
            "(function () {" +
            "  ___.splitCtor(foo, foo_init___);" +
            "  function foo(var_args) {" +
            "    return new foo.make___(arguments);" +
            "  }" +
            "  function foo_init___() {" +
            "    var t___ = this;" +
            "    var x0___;" +
            "    " + weldSetImports("p", "x0___", "___.readProp(t___, 3)") +
            "  }" +
            "  return foo;" +
            "})()") +
        ";;");
  }

  public void testReadIndexPublic() throws Exception {
    checkSucceeds(
        "function() { var foo; p = foo[3]; };",
        "___.primFreeze(___.simpleFunc(" +
        "  function() {" +
        "    var x0___;" +
        "    var foo;" +
        "    " + weldSetImports("p", "x0___", "___.readPub(foo, 3)") +
        "  }" +
        "));");
  }

  public void testSetGlobal() throws Exception {
    checkSucceeds(
        "x = 3;",
        "var x0___;" +
        weldSetImports("x", "x0___", "3") + ";");
    assertConsistent(
        "  var getCount = (function() {"
        + "  var count = 0;"
        + "  return function() { return count++; };"
        + "})();"
        + "x = getCount();"
        + "assertEquals(x, 0);"
        + "assertEquals(getCount(), 1);");
  }

  public void testSetBadThis() throws Exception {
    checkFails(
        "this = 3;",
        "Cannot assign to \"this\"");
    checkFails(
        "function f() { this = 3; }",
        "Cannot assign to \"this\"");
  }

  public void testSetBadSuffix() throws Exception {
    checkFails(
        "x.y__ = z;",
        "Properties cannot end in \"__\"");
  }

  public void testSetGlobalViaThis() throws Exception {
    checkSucceeds(
        "this.p = x;",
        "var x0___;" +
        weldSetImports("p", "x0___", weldReadImports("x")) + ";");
    checkSucceeds(
        "try { } catch (e) { this.p = x; }",
        "var x0___;" +
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    " + weldSetImports("p", "x0___", weldReadImports("x")) + ";" +
        "  }" +
        "}");
  }

  public void testSetInternal() throws Exception {
    checkSucceeds(
        "function() {" +
        "  function foo() { this.p = x; }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo;" +
        "  var foo = (function () {" +
        "      ___.splitCtor(foo, foo_init___);" +
        "      function foo(var_args) {" +
        "        return new foo.make___(arguments);" +
        "      }" +
        "      function foo_init___() {" +
        "        var t___ = this;" +
        "        var x0___;" +
        "        " + weldSetProp(
                         "p",
                         weldReadImports("x"),
                         "x0___") +
        "      }" +
        "      return foo;" +
        "  })();" +
        "  ;" +
        "}));");
  }

  public void testSetMember() throws Exception {
    checkSucceeds(
        "function() {" +
        "  function foo() {}" +
        "  foo.prototype.p = x;" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo;" +
        "  var foo = ___.simpleFunc(function foo() {});" +
        "  ;" +
        "  ___.setMember(foo, 'p', " + weldReadImports("x") + ");" +
        "}));");
    checkSucceeds(
        "function() {" +
        "  function foo() {}" +
        "  foo.prototype.p = function(a, b) { this; };" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo;" +
        "  var foo = ___.simpleFunc(function foo() {});" +
        "  ;" +
        "  ___.setMember(" +
        "      foo, 'p', ___.method(" +
        "          function(a, b) {" +
        "            var t___ = this;" +
        "            t___;" +
        "          }));" +
        "}));");
    checkSucceeds(  // Doesn't trigger setMember but should.
        "foo.bar.prototype.baz = boo;",
        "var x0___;" +
        "var x1___;" +
        "var x2___;" +
        "var x3___;" +
        weldSetPub(
            weldReadPub(
                weldReadPub(
                    weldReadImports("foo"),
                    "bar",
                    "x3___"),
                "prototype",
                "x2___"),
            "baz",
            weldReadImports("boo"),
            "x0___",
            "x1___") + ";");
  }

  public void testSetBadInternal() throws Exception {
    checkFails(
        "x.y_;",
        "Public properties cannot end in \"_\"");
  }

  public void testSetStatic() throws Exception {
    checkSucceeds(
        "function() {" +
        "  function foo() {}" +
        "  foo.p = x;" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo;" +
        "  var foo = ___.simpleFunc(function foo() {});" +
        "  ;" +
        "  ___.setPub(foo, 'p', " + weldReadImports("x") + ");" +
        "}));");
  }

  public void testSetPublic() throws Exception {
    checkSucceeds(
        "function() {" +
        "  var x = undefined;" +
        "  x.p = y;" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var x0___;" +
        "  var x1___;" +
        "  var x = undefined;" +
        "  " + weldSetPub(
                   "x",
                   "p",
                   weldReadImports("y"),
                   "x0___",
                   "x1___") +
        "}));");
  }

  public void testSetIndexInternal() throws Exception {
    checkSucceeds(
        "function() {" +
        "  function foo() {" +
        "    this[x] = y;" +
        "  }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo;" +
        "  var foo = (function () {" +
        "    ___.splitCtor(foo, foo_init___);" +
        "    function foo(var_args) {" +
        "      return new foo.make___(arguments);" +
        "    }" +
        "    function foo_init___() {" +
        "      var t___ = this;" +
        "      ___.setProp(t___, " + weldReadImports("x") + ", " +
                           weldReadImports("y") + ");" +
        "    }" +
        "    return foo;" +
        "  })();" +
        "  ;" +
        "}));");
  }

  public void testSetIndexPublic() throws Exception {
    checkSucceeds(
        "function() {" +
        "  var o = undefined;" +
        "  o[x] = y;" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var o = undefined;" +
        "  ___.setPub(o, " + weldReadImports("x") + ", " +
                      weldReadImports("y") + ");" +
        "}));");
  }

  public void testSetBadInitialize() throws Exception {
    checkFails(
        "var x__ = 3",
        "Variables cannot end in \"__\"");
  }

  public void testSetInitialize() throws Exception {
    checkSucceeds(
        "function() {" +
        "  var v = x;" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var v = " + weldReadImports("x") + ";" +
        "}));");
    checkSucceeds(
        "var v = x",
        "var x0___;" +
        weldSetImports("v", "x0___", weldReadImports("x")));
  }

  public void testSetBadDeclare() throws Exception {
    checkFails(
        "var x__",
        "Variables cannot end in \"__\"");
  }

  public  void testSetDeclare() throws Exception {
    checkSucceeds(
        "function() {" +
        "  var v;" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var v;" +
        "}));");
    checkSucceeds(
        "var v;",
        "___.setPub(IMPORTS___, 'v', ___.readPub(IMPORTS___, 'v'));");
    checkSucceeds(
        "try { } catch (e) { var v; }",
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    ___.setPub(IMPORTS___, 'v', ___.readPub(IMPORTS___, 'v'));" +
        "  }" +
        "}");
  }

  public void testSetVar() throws Exception {
    checkSucceeds(
        "x = y;",
        "var x0___;" +
        weldSetImports("x", "x0___", weldReadImports("y")));
    checkSucceeds(
        "function() {" +
        "  var x;" +
        "  x = y;" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var x;" +
        "  x = " + weldReadImports("y") + ";" +
        "}));");
  }

  public void testSetReadModifyWriteLocalVar() throws Exception {
    checkFails("x__ *= 2", "Variables cannot end in \"__\"");

    checkSucceeds(
        "x += 1",
        "___.setPub(IMPORTS___, 'x',"
        + "  ___.readPub(IMPORTS___, 'x', true) + 1)");
    checkSucceeds(
        "(function (x) { x += 1; })",
        "(___.primFreeze(___.simpleFunc(function (x) { x = x + 1; })))");
    checkSucceeds(
        "myArray().key += 1",
        "  var x0___;"
        + "x0___ = ___.asSimpleFunc(" + weldReadImports("myArray") + ")(),"
        + "___.setPub(x0___, 'key',"
        + "           ___.readPub(x0___, 'key', false) + 1);");
    checkSucceeds(
        "myArray()[myKey()] += 1",
        "  var x0___;"
        + "var x1___;"
        + "x0___ = ___.asSimpleFunc(" + weldReadImports("myArray") + ")(),"
        + "x1___ = ___.asSimpleFunc(" + weldReadImports("myKey") + ")(),"
        + "___.setPub(x0___, x1___,"
        + "           ___.readPub(x0___, x1___, false) + 1);");
    checkSucceeds(  // Local reference need not be assigned to a temp.
        "(function (myKey) { myArray()[myKey] += 1; })",
        "  ___.primFreeze(___.simpleFunc(function (myKey) {"
        + "  var x0___;"
        + "  x0___ = ___.asSimpleFunc(" + weldReadImports("myArray") + ")(),"
        + "  ___.setPub(x0___, myKey,"
        + "             ___.readPub(x0___, myKey, false) + 1);"
        + "}))");

    assertConsistent("var x = 3; x *= 2;");
    assertConsistent("var x = 1; x += 7;");
    assertConsistent("var o = { x: 'a' }; o.x += 'b';");

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
          "function() { var x; x " + op.getSymbol() + " y; };",
          "___.primFreeze(___.simpleFunc(function() {" +
          "  var x; x = x " + op.getAssignmentDelegate().getSymbol() + " " +
          weldReadImports("y") + ";" +
          "}));");
    }
  }

  public void testSetIncrDecr() throws Exception {
    checkFails("x__--;", "Variables cannot end in \"__\"");

    checkSucceeds(
        "x++;",
        "var x0___;" +
        "undefined," +
        "x0___ = ___.readPub(IMPORTS___, 'x', true) - 0," +
        "___.setPub(IMPORTS___, 'x', x0___ + 1)," +
        "x0___;");
    checkSucceeds(
        "x--",
        "var x0___;" +
        "undefined," +
        "x0___ = ___.readPub(IMPORTS___, 'x', true) - 0," +
        "___.setPub(IMPORTS___, 'x', x0___ - 1)," +
        "x0___;");
    checkSucceeds(
        "++x",
        "___.setPub(IMPORTS___, 'x'," +
        " ___.readPub(IMPORTS___, 'x', true) - -1);");

    assertConsistent(
        "var x = 2;" +
        "var arr = [--x, x, x--, x, ++x, x, x++, x];" +
        "assertEquals('1,1,1,0,1,1,1,2', arr.join(','));" +
        "arr.join(',');");
  }

  public void testSetIncrDecrOnLocals() throws Exception {
    checkFails("++x__", "Variables cannot end in \"__\"");
    checkSucceeds(
        "(function (x, y) { return [x--, --x, y++, ++y]; })",
        "___.primFreeze(___.simpleFunc(" +
        "  function (x, y) { return [x--, --x, y++, ++y]; }))");

    assertConsistent(
        "(function () {" +
        "  var x = 2;" +
        "  var arr = [--x, x, x--, x, ++x, x, x++, x];" +
        "  assertEquals('1,1,1,0,1,1,1,2', arr.join(','));" +
        "  return arr.join(',');" +
        "})();");
  }

  public void testSetIncrDecrOfComplexLValues() throws Exception {
    checkFails("arr[x__]--;", "Variables cannot end in \"__\"");
    checkFails("arr__[x]--;", "Variables cannot end in \"__\"");

    checkSucceeds(
        "o.x++",
        "var x0___;" +
        "var x1___;" +
        "x0___ = " + weldReadImports("o") + "," +
        "x1___ = ___.readPub(x0___, 'x', false) - 0," +
        "___.setPub(x0___, 'x', x1___ + 1)," +
        "x1___;");

    assertConsistent(
        "(function () {" +
        "  var o = { x: 2 };" +
        "  var arr = [--o.x, o.x, o.x--, o.x, ++o.x, o.x, o.x++, o.x];" +
        "  assertEquals('1,1,1,0,1,1,1,2', arr.join(','));" +
        "  return arr.join(',');" +
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
        "  return arrs.join(',');" +
        "})()");
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
        "  foo()[foo()] -= foo();" +
        "})()"
        );
  }

  public void testNewCalllessCtor() throws Exception {
    checkSucceeds(
        "(new Date);",
        "new (___.asCtor(___.primFreeze(" + weldReadImports("Date") + ")))()");
  }

  public void testNewCtor() throws Exception {
    checkSucceeds(
        "function foo() { this.p = 3; }" +
        "new foo(x, y);",
        "IMPORTS___.foo = undefined;" +
        "var x0___;" +
        weldSetImports(
            "foo",
            "x0___",
            "(function () {" +
            "  ___.splitCtor(foo, foo_init___);" +
            "  function foo(var_args) {" +
            "    return new foo.make___(arguments);" +
            "  }" +
            "  function foo_init___() {" +
            "    var t___ = this;" +
            "    var x0___;" +
            "    " + weldSetProp("p", "3", "x0___") +
            "  }" +
            "  return foo;" +
            "})()") +
        ";;" +
        "new (___.asCtor(___.primFreeze(" + weldReadImports("foo") + ")))" +
        "    (" + weldReadImports("x") + ", " + weldReadImports("y") + ");");
    checkSucceeds(
        "function foo() {}" +
        "new foo(x, y);",
        "IMPORTS___.foo = undefined;" +
        "var x0___;" +
        weldSetImports("foo", "x0___", "___.simpleFunc(function foo() {})") +
        ";;" +
        "new (___.asCtor(___.primFreeze(" + weldReadImports("foo") + ")))" +
        "    (" + weldReadImports("x") + ", " + weldReadImports("y") + ");");
    checkSucceeds(
        "function foo() {}" +
        "new foo();",
        "IMPORTS___.foo = undefined;" +
        "var x0___;" +
        weldSetImports("foo", "x0___", "___.simpleFunc(function foo() {})") +
        ";;" +
        "new (___.asCtor(___.primFreeze(" + weldReadImports("foo") + ")))();");
    checkSucceeds(
        "new foo.bar(0);",
        "var x0___;" +
        "new (___.asCtor(" +
        "    " + weldReadPub(
                     weldReadImports("foo"),
                     "bar",
                     "x0___") +
        "))(0);");
    assertConsistent(
        "var foo = { bar: Date };" +
        "(new foo.bar(0)).getFullYear()");
    checkSucceeds(
        "function() {" +
        "  new x(y, z);" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  new (___.asCtor(" + weldReadImports("x") + "))" +
        "      (" + weldReadImports("y") + ", " + weldReadImports("z") + ");" +
        "}));");
  }

  public void testDeleteProp() throws Exception {
    checkFails("delete this.foo___;", "Properties cannot end in \"__\"");
    checkSucceeds(
        "delete this[foo()];",
        "___.deleteProp(" +
        "    IMPORTS___, ___.asSimpleFunc(" + weldReadImports("foo") + ")());");
    checkSucceeds("delete this.foo_;", "___.deleteProp(IMPORTS___, 'foo_');");
    checkSucceeds("function Ctor() { D.call(this); delete this.foo_; }",
                  "IMPORTS___.Ctor = undefined;" +
                  "var x0___;" +
                  "x0___ = (function () {" +
                  "    ___.splitCtor(Ctor, Ctor_init___);" +
                  "    function Ctor(var_args) {" +
                  "      return new Ctor.make___(arguments);" +
                  "    }" +
                  "    function Ctor_init___() {" +
                  "      var t___ = this;" +
                  "      var x0___;" +
                  "      var x1___;" +
                  "      x1___ = IMPORTS___.D_canRead___" +
                  "          ? IMPORTS___.D" +
                  "          : ___.readPub(IMPORTS___, 'D', true)," +
                  "      x0___ = t___," +
                  "      x1___.call_canCall___" +
                  "          ? x1___.call(x0___)" +
                  "          : ___.callPub(x1___, 'call', [x0___]);" +
                  // The important bit.  t___ used locally.
                  "      ___.deleteProp(t___, 'foo_');" +
                  "    }" +
                  "    return Ctor;" +
                  "  })()," +
                  "  IMPORTS___.Ctor_canSet___" +
                  "      ? (IMPORTS___.Ctor = x0___)" +
                  "      : ___.setPub(IMPORTS___, 'Ctor', x0___);" +
                  ";");
    assertConsistent(
        // Set up a class that can delete one of its members.
        "function P() { this; }" +
        "caja.def(P, Object, {" +
        "  toString : function () {" +
        "    var pairs = [];" +
        "    for (var k in this) {" +
        // TODO(metaweta): come up with a better way to be the same cajoled and plain
        "      if (typeof this[k] !== 'function' && caja.canInnocentEnum(this, k)) {" +
        "        pairs.push(k + ':' + this[k]);" +
        "      }" +
        "    }" +
        "    pairs.sort();" +
        "    return '(' + pairs.join(', ') + ')';" +
        "  }," +
        "  mangle: function () {" +
        "    delete this.x_;" +            // Deleteable
        "    try {" +
        "      delete this.z_;" +          // Not present.
        "    } catch (ex) {" +
        "      ;" +
        "    }" +
        "  }, " +
        "  setX: function (x) { this.x_ = x; }," +
        "  setY: function (y) { this.y_ = y; }" +
        "});" +
        "var p = new P();" +
        "p.setX(0);" +
        "p.setY(1);" +
        "var hist = [p.toString()];" +     // Record state before deletion.
        "p.mangle();" +                    // Delete
        "hist.push(p.toString());" +       // Record state after deletion.
        "hist.toString();");
    assertConsistent(
        "var foo = 0;" +
        "var preContained = 'foo' in this ? 'prev-in' : 'prev-not-in';" +
        "var deleted = (delete this.foo) ? 'deleted' : 'not-deleted';" +
        "var afterContained = 'foo' in this ? 'post-in' : 'post-not-in';" +
        "var outcome = [preContained, deleted, afterContained].join();" +
        "assertTrue(outcome, outcome === 'prev-in,not-deleted,post-in'" +
        "           || outcome === 'prev-in,deleted,post-not-in');");
  }

  public void testDeletePub() throws Exception {
    checkFails("delete x.foo___", "Variables cannot end in \"__\"");
    checkSucceeds(
        "delete foo()[bar()]",
        "___.deletePub(___.asSimpleFunc(" + weldReadImports("foo") + ")()," +
        "              ___.asSimpleFunc(" + weldReadImports("bar") + ")())");
    checkSucceeds(
        "delete foo().bar",
        "___.deletePub(___.asSimpleFunc(" + weldReadImports("foo") + ")()," +
        "              'bar')");
    assertConsistent(
        "(function() {" +
        "  var o = { x: 3, y: 4 };" +    // A JSON object.
        "  function ptStr(o) { return '(' + o.x + ',' + o.y + ')'; }" +
        "  var history = [ptStr(o)];" +  // Record state before deletion.
        "  delete o.y;" +                // Delete
        "  delete o.z;" +                // Not present.  Delete a no-op
        "  history.push(ptStr(o));" +    // Record state after deletion.
        "  return history.toString();" +
        "})()");
    assertConsistent(
        "var alert = 'a';" +
        "var o = { a: 1 };" +
        "delete o[alert];" +
        "assertEquals(undefined, o.a);" +
        "o.a");
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
        "status");
  }

  public void testDeleteGlobal() throws Exception {
    checkFails("delete IMPORTS___", "Variables cannot end in \"__\"");
    checkSucceeds(
        "delete foo",
        "___.deletePub(IMPORTS___, 'foo')"
        );
    assertConsistent(
        "var foo = 0;" +
        "var preContained = 'foo' in this ? 'prev-in' : 'prev-not-in';" +
        "var deleted = (delete foo) ? 'deleted' : 'not-deleted';" +
        "var afterContained = 'foo' in this ? 'post-in' : 'post-not-in';" +
        "var outcome = [preContained, deleted, afterContained].join();" +
        "assertTrue(outcome, outcome === 'prev-in,not-deleted,post-in'" +
        "           || outcome === 'prev-in,deleted,post-not-in')");
  }

  public void testDeleteNonLvalue() throws Exception {
    checkFails("delete 4", "invalid operand to delete");
  }

  public void testCallGlobalViaThis() throws Exception {
    checkSucceeds(
        "this.f(x, y)",
        "var x0___;" +
        "var x1___;" +
        "x0___ = " + weldReadImports("x") + "," +
        "x1___ = " + weldReadImports("y") + "," +
        "IMPORTS___.f_canCall___ ?" +
        "    IMPORTS___.f(x0___, x1___) :" +
        "    ___.callPub(IMPORTS___, 'f', [x0___, x1___]);");
    checkSucceeds(
        "try { } catch (e) { this.f(x, y); }",
        "var x0___;" +
        "var x1___;" +
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    x0___ = " + weldReadImports("x") + "," +
        "    x1___ = " + weldReadImports("y") + "," +
        "    IMPORTS___.f_canCall___ ?" +
        "        IMPORTS___.f(x0___, x1___) :" +
        "        ___.callPub(IMPORTS___, 'f', [x0___, x1___]);" +
        "  }" +
        "}");
  }

  public void testCallInternal() throws Exception {
    checkSucceeds(
        "function() {" +
        "  function foo() {" +
        "    this.f(x, y);" +
        "  }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo;" +
        "  var foo = (function () {" +
        "    ___.splitCtor(foo, foo_init___);" +
        "    function foo(var_args) {" +
        "      return new foo.make___(arguments);" +
        "    }" +
        "    function foo_init___() {" +
        "      var t___ = this;" +
        "      var x0___;" +
        "      var x1___;" +
        "      x0___ = " + weldReadImports("x") + "," +
        "      x1___ = " + weldReadImports("y") + "," +
        "      t___.f_canCall___ ?" +
        "          t___.f(x0___, x1___) :" +
        "          ___.callProp(t___, 'f', [x0___, x1___]);" +
        "    }" +
        "    return foo;" +
        "  })();" +
        "  ;" +
        "}));");
  }

  public void testCallBadInternal() throws Exception {
    checkFails(
        "o.p_();",
        "Public selectors cannot end in \"_\"");
  }

  public void testCallCajaDef2() throws Exception {
    checkSucceeds(
        "function Point() {}" +
        "caja.def(Point, Object);" +
        "function WigglyPoint() {}" +
        "caja.def(WigglyPoint, Point);",
        "IMPORTS___.Point = undefined;" +
        "var x0___;" +
        weldSetImports(
            "Point", "x0___", "___.simpleFunc(function Point() {})") + ";" +
        "IMPORTS___.WigglyPoint = undefined;" +
        "var x1___;" +
        weldSetImports(
            "WigglyPoint", "x1___", "___.simpleFunc(function WigglyPoint() {})"
            ) + ";" +
        ";" +
        "caja.def(" + weldReadImports("Point") + ", " +
                  weldReadImports("Object") + ");" +
        ";" +
        "caja.def(" + weldReadImports("WigglyPoint") + ", " +
                  weldReadImports("Point") + ");");
  }

  public void testCallCajaDef2Bad() throws Exception {
    checkFails(
        "function() {" +
        "  function Point() {}" +
        "  caja.def(Point, Array);" +
        "};",
        "caja.def called with non-constructor");
    checkFails(
        "function() {" +
        "  var Point = 3;" +
        "  caja.def(Point, Object);" +
        "};",
        "caja.def called with non-constructor");
  }

  public void testCallCajaDef3Plus() throws Exception {
    checkSucceeds(
        "function Point() {}" +
        "function WigglyPoint() {}" +
        "caja.def(WigglyPoint, Point, { m0: x, m1: function() { this.p = 3; } });",
        "IMPORTS___.Point = undefined;" +
        "var x0___;" +
        weldSetImports(
            "Point", "x0___", "___.simpleFunc(function Point() {})") + ";" +
        "IMPORTS___.WigglyPoint = undefined;" +
        "var x1___;" +
        weldSetImports(
            "WigglyPoint", "x1___", "___.simpleFunc(function WigglyPoint() {})"
            ) + ";" +
        ";" +
        ";" +
        "caja.def(" + weldReadImports("WigglyPoint") + ", " +
                  weldReadImports("Point") + ", {" +
        "    m0: " + weldReadImports("x") + "," +
        "    m1: ___.method(function() {" +
        "                     var t___ = this;" +
        "                     var x0___;" +
        "                     " + weldSetProp("p", "3", "x0___") + ";" +
        "                   })" +
        "});");
    checkSucceeds(
        "function Point() {}" +
        "function WigglyPoint() {}" +
        "caja.def(WigglyPoint, Point," +
        "    { m0: x, m1: function() { this.p = 3; } }," +
        "    { s0: y, s1: function() { return 3; } });",
        "IMPORTS___.Point = undefined;" +
        "var x0___;" +
        weldSetImports(
            "Point", "x0___", "___.simpleFunc(function Point() {})") + ";" +
        "IMPORTS___.WigglyPoint = undefined;" +
        "var x1___;" +
        weldSetImports(
            "WigglyPoint", "x1___", "___.simpleFunc(function WigglyPoint() {})"
            ) + ";" +
        ";" +
        ";" +
        "caja.def(" + weldReadImports("WigglyPoint") + ", " +
                  weldReadImports("Point") + ", {" +
        "    m0: " + weldReadImports("x") + "," +
        "    m1: ___.method(function() {" +
        "                     var t___ = this;" +
        "                     var x0___;" +
        "                     " + weldSetProp("p", "3", "x0___") +
        "                   })" +
        "}, {" +
        "    s0: " + weldReadImports("y") + "," +
        "    s1: ___.primFreeze(___.simpleFunc(function() { return 3; }))" +
        "});");
    checkFails(
        "function() {" +
        "  function Point() {}" +
        "  function WigglyPoint() {}" +
        "  caja.def(WigglyPoint, Point, x);" +
        "};",
        "Map expression expected");
    checkFails(
        "function() {" +
        "  function Point() {}" +
        "  function WigglyPoint() {}" +
        "  caja.def(WigglyPoint, Point, { foo: x }, x);" +
        "};",
        "Map expression expected");
    checkFails(
        "function() {\n" +
        "  function Point() {}\n" +
        "  function WigglyPoint() {}\n" +
        "  caja.def(WigglyPoint, Point, { foo: x },\n" +
        "           { bar: function() { this.x_ = 3; } });\n" +
        "};",
        "Public properties cannot end in \"_\"");
  }

  public void testCallCajaDef3PlusBad() throws Exception {
    checkFails(
        "function() {" +
        "  function Point() {}" +
        "  caja.def(Point, Array, {});" +
        "};",
        "caja.def called with non-constructor");
    checkFails(
        "function() {" +
        "  var Point = 3;" +
        "  caja.def(Point, Object, {});" +
        "};",
        "caja.def called with non-constructor");
    checkFails(
        "function() {" +
        "  function Point() {}" +
        "  caja.def(Point, Array, {}, {});" +
        "};",
        "caja.def called with non-constructor");
    checkFails(
        "function() {" +
        "  var Point = 3;" +
        "  caja.def(Point, Object, {}, {});" +
        "};",
        "caja.def called with non-constructor");
  }

  public void testCallPublic() throws Exception {
    checkSucceeds(
        "o.m(x, y);",
        "var x0___;" +
        "var x1___;" +
        "var x2___;" +
        "x2___ = " + weldReadImports("o") + "," +
        "(" +
            "x0___ = " + weldReadImports("x") + "," +
            "x1___ = " + weldReadImports("y") +
        ")," +
        "x2___.m_canCall___ ?" +
        "  x2___.m(x0___, x1___) :" +
        "  ___.callPub(x2___, 'm', [x0___, x1___]);");
  }

  public void testCallIndexInternal() throws Exception {
    checkSucceeds(
        "function() {" +
        "  function foo() {" +
        "    this[x](y, z);" +
        "  }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo;" +
        "  var foo = (function () {" +
        "    ___.splitCtor(foo, foo_init___);" +
        "    function foo(var_args) {" +
        "      return new foo.make___(arguments);" +
        "    }" +
        "    function foo_init___() {" +
        "      var t___ = this;" +
        "      ___.callProp(" +
        "          t___, " +
        "          " + weldReadImports("x") + "," +
        "          [" + weldReadImports("y") + ", " +
                    weldReadImports("z") + "]);" +
        "    }" +
        "    return foo;" +
        "  })();" +
        "  ;" +
        "}));");
  }

  public void testCallIndexPublic() throws Exception {
    checkSucceeds(
        "x[y](z, t);",
        "___.callPub(" +
        "    " + weldReadImports("x") + ", " +
        "    " + weldReadImports("y") + ", " +
        "    [" + weldReadImports("z") + ", " + weldReadImports("t") + "]);");
  }

  public void testCallFunc() throws Exception {
    checkSucceeds(
        "function() { var f; f(x, y); }",
        "___.primFreeze(___.simpleFunc(function() {" +
        "    var f;" +
        "    ___.asSimpleFunc(f)(" +
        "        " + weldReadImports("x") + ", " + weldReadImports("y") + ");" +
        "}));");
    checkSucceeds(
        "foo(x, y);",
        "___.asSimpleFunc(" + weldReadImports("foo") + ")(" +
        "    " + weldReadImports("x") + "," +
        "    " + weldReadImports("y") +
        ");");
  }

  public void testFuncAnonSimple() throws Exception {
    checkSucceeds(
        "function(x, y) { x = arguments; y = z; };",
        "___.primFreeze(___.simpleFunc(function(x, y) {" +
        "  var a___ = ___.args(arguments);" +
        "  x = a___;" +
        "  y = " + weldReadImports("z") + ";" +
        "}));");
  }

  public void testFuncNamedSimpleDecl() throws Exception {
    checkSucceeds(
        "function() {" +
        "  function foo(x, y) {" +
        "    x = arguments;" +
        "    y = z;" +
        "    return foo(x - 1, y - 1);" +
        "  }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo;" +
        "  var foo = ___.simpleFunc(function foo(x, y) {" +
        "      var a___ = ___.args(arguments);" +
        "      x = a___;" +
        "      y = " + weldReadImports("z") + ";" +
        "      return ___.asSimpleFunc(___.primFreeze(foo))(x - 1, y - 1);" +
        "  });" +
        "  ;"+
        "}));");
    checkSucceeds(
        "function foo(x, y ) {" +
        "  return foo(x - 1, y - 1);" +
        "}",
        "IMPORTS___.foo = undefined;" +
        "var x0___;" +
        weldSetImports(
            "foo",
            "x0___",
            "___.simpleFunc(function foo(x, y) {" +
            "  return ___.asSimpleFunc(___.primFreeze(foo))(x - 1, y - 1);" +
            "})") +
        ";;");
  }

  public void testFuncNamedSimpleValue() throws Exception {
    checkSucceeds(
        "function() {" +
        "  var f = function foo(x, y) {" +
        "    x = arguments;" +
        "    y = z;" +
        "    return foo(x - 1, y - 1);" +
        "  };" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var f = ___.primFreeze(___.simpleFunc(" +
        "    function foo(x, y) {" +
        "      var a___ = ___.args(arguments);" +
        "      x = a___;" +
        "      y = " + weldReadImports("z") + ";" +
        "      return ___.asSimpleFunc(___.primFreeze(foo))(x - 1, y - 1);" +
        "  }));"+
        "}));");
    checkSucceeds(
        "var foo = function foo(x, y ) {" +
        "  return foo(x - 1, y - 1);" +
        "};",
        "var x0___;" +
        weldSetImports(
            "foo",
            "x0___",
            "___.primFreeze(___.simpleFunc(function foo(x, y) {" +
            "  return ___.asSimpleFunc(___.primFreeze(foo))(x - 1, y - 1);" +
            "}))") + ";");
  }

  public void testFuncExophoricFunction() throws Exception {
    checkSucceeds(
        "function (x) { return this.x; };",
        "var x0___;" +
        "___.xo4a(" +
        "    function (x) {" +
        "       var t___ = this;" +
        "       var t___ = this;" +
        "       return " + weldReadPub(
                               "t___",
                               "x",
                               "x0___") + ";" +
        "});");
    checkFails(
        "function (k) { return this[k]; }",
        "\"this\" in an exophoric function exposes only public fields");
    checkFails(
        "function () { delete this.k; }",
        "\"this\" in an exophoric function exposes only public fields");
    checkFails(
        "function () { x in this; }",
        "\"this\" in an exophoric function exposes only public fields");
    checkFails(
        "function () { 'foo_' in this; }",
        "\"this\" in an exophoric function exposes only public fields");
    checkSucceeds(
        "function () { 'foo' in this; }",
        "___.xo4a(" +
        "    function () {" +
        "      var t___ = this;" +
        "      var t___ = this;" +
        "      'foo' in t___;" +
        "    })");
    checkFails(
        "function () { for (var k in this); }",
        "\"this\" in an exophoric function exposes only public fields");
    checkFails(
        "function (y) { this.x = y; }",
        "\"this\" in an exophoric function exposes only public fields");
    assertConsistent(
        "({ f7: function () { return this.x + this.y; }, x: 1, y: 2 }).f7()");
  }

  public void testFuncBadMethod() throws Exception {
    checkFails(
        "function(x) { this.x_ = x; };",
        "Public properties cannot end in \"_\"");
  }

  public void testMaskingFunction () throws Exception {
    assertAddsMessage(
        "function Goo() { function Goo() {} }",
        MessageType.SYMBOL_REDEFINED,
        MessageLevel.ERROR );
    assertAddsMessage(
        "function Goo() { var Goo = 1}",
        MessageType.MASKING_SYMBOL,
        MessageLevel.LINT );
    assertMessageNotPresent(
        "function Goo() { this.x = 1; }",
        MessageType.MASKING_SYMBOL );
  }

  public void testFuncCtor() throws Exception {
    checkSucceeds(
        "function Foo(x) { this.x_ = x; }",
        "IMPORTS___.Foo = undefined;" +
        "var x0___;" +
        "x0___ = (function () {" +
        "      ___.splitCtor(Foo, Foo_init___);" +
        "      function Foo(var_args) {" +
        "        return new Foo.make___(arguments);" +
        "      }" +
        "      function Foo_init___(x) {" +
        "        var t___ = this;" +
        "        var x0___;" +
        "        " + weldSetProp("x_", "x", "x0___") + ";" +
        "      }" +
        "      return Foo;" +
        "    })()," +
        "    IMPORTS___.Foo_canSet___" +
        "        ? (IMPORTS___.Foo = x0___)" +
        "        : ___.setPub(IMPORTS___, 'Foo', x0___);" +
        ";");
    checkSucceeds(
        "(function(){ function Foo(x) { this.x_ = x; } })()",
        "___.asSimpleFunc(___.primFreeze(___.simpleFunc(function () {" +
        "    var Foo;" +
        "    var Foo = (function () {" +
        "        ___.splitCtor(Foo, Foo_init___);" +
        "        function Foo(var_args) {" +
        "          return new Foo.make___(arguments);" +
        "        }" +
        "        function Foo_init___(x) {" +
        "          var t___ = this;" +
        "          var x0___;" +
        "          " + weldSetProp("x_", "x", "x0___") + ";" +
        "        }" +
        "        return Foo;" +
        "      })();" +
        "    ;" +
        "  })))();");
    checkSucceeds(
        "function Foo(x) { this.x_ = x; }" +
        "function Bar(y) {" +
        "  Foo.call(this,1);" +
        "  this.y = y;" +
        "}" +
        "bar = new Bar(3);",
        "IMPORTS___.Foo = undefined;" +
        "var x0___;" +
        "x0___ = (function () {" +
        "        ___.splitCtor(Foo, Foo_init___);" +
        "        function Foo(var_args) {" +
        "          return new Foo.make___(arguments);" +
        "        }" +
        "        function Foo_init___(x) {" +
        "          var t___ = this;" +
        "          var x0___;" +
        "          " + weldSetProp("x_", "x", "x0___") + ";" +
        "        }" +
        "        return Foo;" +
        "    })()," +
        "    IMPORTS___.Foo_canSet___" +
        "        ? (IMPORTS___.Foo = x0___)" +
        "        : ___.setPub(IMPORTS___, 'Foo', x0___);" +
        "IMPORTS___.Bar = undefined;" +
        "var x1___;" +
        "x1___ = (function () {" +
        "        ___.splitCtor(Bar, Bar_init___);" +
        "        function Bar(var_args) {" +
        "          return new Bar.make___(arguments);" +
        "        }" +
        "        function Bar_init___(y) {" +
        "          var t___ = this;" +
        "          var x0___;" +
        "          (IMPORTS___.Foo_canRead___" +
        "           ? IMPORTS___.Foo" +
        "           : ___.readPub(IMPORTS___, 'Foo', true)).call(this, 1);" +
        "          " + weldSetProp("y", "y", "x0___") + ";" +
        "        }" +
        "        return Bar;" +
        "      })()," +
        "      IMPORTS___.Bar_canSet___" +
        "          ? (IMPORTS___.Bar = x1___)" +
        "          : ___.setPub(IMPORTS___, 'Bar', x1___);" +
        "var x2___;" +
        ";" +
        ";" +
        "x2___ = new (___.asCtor(___.primFreeze(" +
             weldReadImports("Bar", true) + ")))(3)," +
        "    IMPORTS___.bar_canSet___" +
        "        ? (IMPORTS___.bar = x2___)" +
        "        : ___.setPub(IMPORTS___, 'bar', x2___);");
  }

  public void testMapEmpty() throws Exception {
    checkSucceeds(
        "f = {};",
        "var x0___;" +
        weldSetImports("f", "x0___", "{}"));
  }

  public void testMapBadKeySuffix() throws Exception {
    checkFails(
        "var o = { x_: 3 };",
        "Key may not end in \"_\"");
  }

  public void testMapNonEmpty() throws Exception {
    checkSucceeds(
        "var o = { k0: x, k1: y };",
        "var x0___;" +
        weldSetImports("o", "x0___", "{ k0: " + weldReadImports("x") + ", " +
                       "                k1: " + weldReadImports("y") + " }"));
  }

  public void testOtherInstanceof() throws Exception {
    checkSucceeds(
        "function foo() {}" +
        "x instanceof foo;",
        "IMPORTS___.foo = undefined;" +
        "var x0___;" +
        weldSetImports("foo", "x0___", "___.simpleFunc(function foo() {})") +
        ";;" +
        weldReadImports("x") + " instanceof ___.primFreeze(" +
            weldReadImports("foo") + ");");
    checkSucceeds(
        "x instanceof Object",
        weldReadImports("x") + " instanceof ___.primFreeze(" +
            weldReadImports("Object") + ");");

    assertConsistent("({}) instanceof Object");
    assertConsistent("(new Date) instanceof Date");
    assertConsistent("({}) instanceof Date");
    assertConsistent("function foo() {}; (new foo) instanceof foo");
    assertConsistent("function foo() {}; !(({}) instanceof foo)");
  }

  public void testOtherTypeof() throws Exception {
    checkSucceeds("typeof x;", "typeof ___.readPub(IMPORTS___, 'x');");
    checkFails("typeof ___", "Variables cannot end in \"__\"");
    assertConsistent("typeof noSuchGlobal");
    assertConsistent("typeof 's'");
    assertConsistent("typeof 4");
    assertConsistent("typeof null");
    assertConsistent("typeof (void 0)");
    assertConsistent("typeof []");
    assertConsistent("typeof {}");
    assertConsistent("typeof /./");
    assertConsistent("typeof (function () {})");
    assertConsistent("typeof { x: 4.0 }.x");
    assertConsistent("typeof { 2: NaN }[1 + 1]");
  }

  public void testLabeledStatement() throws Exception {
    checkFails("IMPORTS___: 1", "Labels cannot end in \"__\"");
    checkSucceeds("foo: 1", "foo: 1");
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

  public void testOtherSpecialOp() throws Exception {
    checkSucceeds("void 0", "void 0");
    checkSucceeds("void foo()",
                  "void (___.asSimpleFunc)(" + weldReadImports("foo") + ")()");
    checkSucceeds("a, b", weldReadImports("a") + "," + weldReadImports("b"));
  }

  public void testMultiDeclaration() throws Exception {
    // 'var' in global scope, part of a block
    checkSucceeds(
        "var x, y;",
        "___.setPub(IMPORTS___, 'x', ___.readPub(IMPORTS___, 'x')), " +
        "___.setPub(IMPORTS___, 'y', ___.readPub(IMPORTS___, 'y'));");
    checkSucceeds(
        "var x = foo, y = bar;",
        "var x0___;" +
        "var x1___;" +
        weldSetImports("x", "x0___", weldReadImports("foo")) + ", " +
        "(" + weldSetImports("y", "x1___", weldReadImports("bar")) + ");");
    checkSucceeds(
        "var x, y = bar;",
        "var x0___;" +
        "___.setPub(IMPORTS___, 'x', ___.readPub(IMPORTS___, 'x')), " +
        "(" + weldSetImports("y", "x0___", weldReadImports("bar")) + ");");
    // 'var' in global scope, 'for' statement
    checkSucceeds(
        "for (var x, y; ; ) {}",
        "for (___.setPub(IMPORTS___, 'x', ___.readPub(IMPORTS___, 'x')), " +
        "___.setPub(IMPORTS___, 'y', ___.readPub(IMPORTS___, 'y')); ; ) {}");
    checkSucceeds(
        "for (var x = foo, y = bar; ; ) {}",
        "var x0___;" +
        "var x1___;" +
        "for (" + weldSetImports("x", "x0___", weldReadImports("foo")) + ", " +
        "     (" + weldSetImports("y", "x1___", weldReadImports("bar")) + ")" +
        "     ; ; ) {}");
    checkSucceeds(
        "for (var x, y = bar; ; ) {}",
        "var x0___;" +
        "for (___.setPub(IMPORTS___, 'x', ___.readPub(IMPORTS___, 'x')), " +
        "     (" + weldSetImports(
                       "y", "x0___", "(" + weldReadImports("bar") + ")") + ")" +
        "     ; ; ) {}");
    // 'var' in global scope, part of a block
    checkSucceeds(
        "function() {" +
        "  var x, y;" +
        "}",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var x, y;" +
        "}));");
    checkSucceeds(
        "function() {" +
        "  var x = foo, y = bar;" +
        "}",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var x = " + weldReadImports("foo") + ", " +
        "      y = " + weldReadImports("bar") + ";" +
        "}));");
    checkSucceeds(
        "function() {" +
        "  var x, y = bar;" +
        "}",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var x, y = " + weldReadImports("bar") + ";" +
        "}));");
    // 'var' in global scope, 'for' statement
    checkSucceeds(
        "function() {" +
        "  for (var x, y; ; ) {}" +
        "}",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  for (var x, y; ; ) {}" +
        "}));");
    checkSucceeds(
        "function() {" +
        "  for (var x = foo, y = bar; ; ) {}" +
        "}",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  for (var x = " + weldReadImports("foo") + ", " +
        "           y = " + weldReadImports("bar") + "; ; ) {}" +
        "}));");
    checkSucceeds(
        "function() {" +
        "  for (var x, y = bar; ; ) {}" +
        "}",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  for (var x, y = " + weldReadImports("bar") + "; ; ) {}" +
        "}));");
    assertConsistent(
        "var arr = [1, 2, 3], k = -1;" +
        "(function () {" +
        "  var a = arr[++k], b = arr[++k], c = arr[++k];" +
        "  return [a, b, c].join(',');" +
        "})()");
    // Check exceptions on read of uninitialized variables.
    assertConsistent(
        "var x, history;" +
        "history = '';" +
        "try { history += '(x=' + x + ')'; }" +
        "catch (ex) { history += '(threw x)'; }" +
        "try { history += '(y=' + y + ')'; }" +
        "catch (ex) { history += '(threw y)'; }" +
        "history;");
    assertConsistent(
        "(function () {" +
        "   var x, history;" +
        "   history = '';" +
        "   try { history += '(x=' + x + ')'; }" +
        "   catch (ex) { history += '(threw x)'; }" +
        "   try { history += '(y=' + y + ')'; }" +
        "   catch (ex) { history += '(threw y)'; }" +
        "   return history;" +
        " })()");
    assertConsistent(
        "(function () {" +
        "  var a = [];" +
        "  for (var i = 0, j = 10; i < j; ++i) { a.push(i); }" +
        "  return a.join(',');" +
        "})()");
    assertConsistent(
        "var a = [];" +
        "for (var i = 0, j = 10; i < j; ++i) { a.push(i); }" +
        "a.join(',')");
  }

  public void testRecurseParseTreeNodeContainer() throws Exception {
    // Tested implicitly by other cases
  }

  public void testRecurseArrayConstructor() throws Exception {
    checkSucceeds(
        "foo = [ bar, baz ];",
        "var x0___;" +
        weldSetImports(
            "foo",
            "x0___",
            "[" + weldReadImports("bar") + ", " +
                  weldReadImports("baz") + "]") +
        ";");
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
        "switch (x) { case 1: break; }",
        "switch (" + weldReadImports("x") + ") { case 1: break; }");
  }

  public void testRecurseConditional() throws Exception {
    checkSucceeds(
        "if (x === y) {" +
        "  z;" +
        "} else if (z === y) {" +
        "  x;" +
        "} else {" +
        "  y;" +
        "}",
        "if (" + weldReadImports("x") + " === " + weldReadImports("y") + ") {" +
        "  " + weldReadImports("z") + ";" +
        "} else if (" + weldReadImports("z") + " === " + weldReadImports("y") +
        "           ) {" +
        "  " + weldReadImports("x") + ";" +
        "} else {" +
        "  " + weldReadImports("y") + ";" +
        "}");
  }

  public void testRecurseContinueStmt() throws Exception {
    checkSucceeds(
        "while (true) { continue; }",
        "while (true) { continue; }");
  }

  public void testRecurseDefaultCaseStmt() throws Exception {
    checkSucceeds(
        "switch (x) { default: break; }",
        "switch(" + weldReadImports("x") + ") { default: break; }");
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
        "for (var k = 0; k < 3; k++) {" +
        "  x;" +
        "}",
        "var x0___;" +
        "var x1___;" +
        "for (" + weldSetImports("k", "x0___", "0") + "; " +
        "     " + weldReadImports("k") + " < 3;" +
        "     undefined," +
        "     x1___ = ___.readPub(IMPORTS___, 'k', true) - 0," +
        "     ___.setPub(IMPORTS___, 'k', x1___ + 1)," +
        "     x1___) {" +
        "  " + weldReadImports("x") + ";" +
        "}");
    checkSucceeds(
        "function() {" +
        "  for (var k = 0; k < 3; k++) {" +
        "    x;" +
        "  }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  for (var k = 0; k < 3; k++) {" +
        "    " + weldReadImports("x") + ";" +
        "  }" +
        "}));");
  }

  public void testRecurseNoop() throws Exception {
    checkSucceeds(
        ";",
        ";");
  }

  public void testRecurseOperation() throws Exception {
    checkSucceeds(
        "x + y;",
        weldReadImports("x") + " + " + weldReadImports("y") + ";");
    checkSucceeds(
        "1 + 2 * 3 / 4 - -5;",
        "1 + 2 * 3 / 4 - -5;");
    checkSucceeds(
        "x  = y = 3;",
        "var x0___;" +
        "var x1___;" +
        weldSetImports(
            "x", "x0___", "(" + weldSetImports("y", "x1___", "3") + ")") + ";");
  }

  public void testRecurseReturnStmt() throws Exception {
    checkSucceeds(
        "return x;",
        "return " + weldReadImports("x") + ";");
  }

  public void testRecurseSwitchStmt() throws Exception {
    checkSucceeds(
        "switch (x) { }",
        "switch (" + weldReadImports("x") + ") { }");
  }

  public void testRecurseThrowStmt() throws Exception {
    checkSucceeds(
        "throw x;",
        "throw " + weldReadImports("x") + ";");
    checkSucceeds(
        "function() {" +
        "  var x;" +
        "  throw x;" +
        "}",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var x;" +
        "  throw x;" +
        "}));");
  }

  public void testSpecimenClickme() throws Exception {
    checkSucceeds(fromResource("clickme.js"));
  }

  public void testSpecimenListfriends() throws Exception {
    checkSucceeds(fromResource("listfriends.js"));
  }

  @Override
  protected Object executePlain(String caja) throws IOException {
    mq.getMessages().clear();
    // Make sure the tree assigns the result to the unittestResult___ var.
    return RhinoTestBed.runJs(
        null,
        new RhinoTestBed.Input(getClass(), "/com/google/caja/caja.js"),
        new RhinoTestBed.Input(getClass(), "../../plugin/asserts.js"),
        new RhinoTestBed.Input(caja, getName() + "-uncajoled"));
  }

  @Override
  protected Object rewriteAndExecute(String caja)
      throws IOException, ParseException {
    mq.getMessages().clear();

    Statement cajaTree = replaceLastStatementWithEmit(
        js(fromString(caja, is)), "unittestResult___");
    String cajoledJs = render(
        cajole(js(fromResource("../../plugin/asserts.js")), cajaTree));

    Object result = RhinoTestBed.runJs(
        null,
        new RhinoTestBed.Input(
            getClass(), "/com/google/caja/plugin/console-stubs.js"),
        new RhinoTestBed.Input(getClass(), "/com/google/caja/caja.js"),
        new RhinoTestBed.Input(
            // Initialize the output field to something containing a unique
            // object value that will not compare identically across runs.
            "var unittestResult___ = { toString:\n" +
            "    function () { return '--NO-RESULT--'; }}\n" +
            // Set up the imports environment.
            "var testImports = ___.copy(___.sharedImports);\n" +
            "___.getNewModuleHandler().setImports(testImports);",
            getName() + "-test-fixture"),
        // Load the cajoled code.
        new RhinoTestBed.Input(
            "___.loadModule(function (___, IMPORTS___) {" + cajoledJs + "\n});",
            getName() + "-cajoled"),
        // Return the output field as the value of the run.
        new RhinoTestBed.Input("unittestResult___", getName()));

    assertNoErrors();
    return result;
  }

  private <T extends ParseTreeNode> T replaceLastStatementWithEmit(
      T node, String varName) {
    if (node instanceof ExpressionStmt) {
      ExpressionStmt es = (ExpressionStmt) node;
      Expression e = es.getExpression();
      Operation emitter = SyntheticNodes.s(Operation.create(
          Operator.ASSIGN,
          SyntheticNodes.s(new Reference(
              SyntheticNodes.s(new Identifier(varName)))),
          e));
      es.replaceChild(emitter, e);
    } else {
      List<? extends ParseTreeNode> children = node.children();
      if (!children.isEmpty()) {
        replaceLastStatementWithEmit(
            children.get(children.size() - 1), varName);
      }
    }
    return node;
  }

  private ParseTreeNode cajole(Statement... nodes) {
    return new DefaultCajaRewriter(false).expand(
        new Block(Arrays.asList(nodes)), mq);
  }

  @Override
  protected Rewriter newRewriter() {
    return new DefaultCajaRewriter(true);
  }
}
