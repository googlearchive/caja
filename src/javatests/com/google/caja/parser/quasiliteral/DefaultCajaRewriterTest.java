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

import com.google.caja.lexer.CharProducer;
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
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.RhinoTestBed;
import com.google.caja.util.TestUtil;
import com.google.caja.plugin.SyntheticNodes;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Collections;
import junit.framework.AssertionFailedError;

/**
 * @author ihab.awad@gmail.com
 */
public class DefaultCajaRewriterTest extends CajaTestCase {

  /**
   * Welds together a string representing the repeated pattern of expected test output for
   * assigning to an outer variable.
   *
   * @author erights@gmail.com
   */
  private static String weldSetOuters(String varName, String expr) {
    return
        "(function() {" +
        "  var x___ = (" + expr + ");" +
        "  return (___OUTERS___." + varName + "_canSet___ ?" +
        "      (___OUTERS___." + varName + " = x___) :" +
        "      ___.setPub(___OUTERS___,'" + varName + "',x___));" +
        "})()";
  }

  /**
   * Welds together a string representing the repeated pattern of expected test output for
   * reading an outer variable.
   *
   * @author erights@gmail.com
   */
  private static String weldReadOuters(String varName) {
    return weldReadOuters(varName, true);
  }

  private static String weldReadOuters(String varName, boolean flag) {
    return
        "(___OUTERS___." + varName + "_canRead___ ?" +
        "    ___OUTERS___." + varName + ":" +
        "    ___.readPub(___OUTERS___, '" + varName + "'" + (flag ? ", true" : "") + "))";
  }

  ////////////////////////////////////////////////////////////////////////
  // Handling of synthetic nodes
  ////////////////////////////////////////////////////////////////////////

  public void testSyntheticIsUntouched() throws Exception {
    ParseTreeNode input = js(fromString("function foo() { this; arguments; }"));
    setTreeSynthetic(input);
    checkSucceeds(input, input);
  }

  public void testNestedInsideSyntheticIsExpanded() throws Exception {
    ParseTreeNode innerInput = js(fromString("function foo() {}"));
    ParseTreeNode input = ParseTreeNodes.newNodeInstance(
        Block.class,
        null,
        Collections.singletonList(innerInput));
    setSynthetic(input);
    ParseTreeNode expectedResult = js(fromString(
        "___OUTERS___.foo = undefined;" +
        "{" + weldSetOuters("foo", "___.simpleFunc(function foo() {})") + ";;}"));
    checkSucceeds(input, expectedResult);
  }

  ////////////////////////////////////////////////////////////////////////
  // Handling of nested blocks
  ////////////////////////////////////////////////////////////////////////

  public void testNestedBlockWithFunction() throws Exception {
    checkSucceeds(
        "{ function foo() {} }",
        "___OUTERS___.foo = undefined;" +
        "{" + weldSetOuters("foo", "___.simpleFunc(function foo() {})") + ";;}");
  }

  public void testNestedBlockWithVariable() throws Exception {
    checkSucceeds(
        "{ var x = y; }",
        "{" + weldSetOuters("x", weldReadOuters("y")) + "}");
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
        "1;" +            
        "{" +
        "  x0___ = " + weldReadOuters("x") + ";" +
        "  for (x1___ in x0___) {" +
        "    if (___.canEnumPub(x0___, x1___)) {" +
        "      " + weldSetOuters("k", "x1___") + ";" +
        "      { " + weldReadOuters("k") + "; }" +
        "    }" +
        "  }" +
        "}");
    checkSucceeds(
        "2; try { } catch (e) { for (var k in x) { k; } }",
        "var x0___;" +
        "var x1___;" +
        "2;" +
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    {" +
        "      x0___ = " + weldReadOuters("x") + ";" +
        "      for (x1___ in x0___) {" +
        "        if (___.canEnumPub(x0___, x1___)) {" +
        "          " + weldSetOuters("k", "x1___") + ";" +
        "          { " + weldReadOuters("k") + "; }" +
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
        "    x0___ = " + weldReadOuters("x") + ";" +
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
        "    x0___ = " + weldReadOuters("x") + ";" +
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
        "    x0___ = " + weldReadOuters("x") + ";" +
        "    for (x1___ in x0___) {" +
        "      if (___.canEnumPub(x0___, x1___)) {" +
        "        ___.setPub(" + weldReadOuters("z") + ", 0, x1___);" +
        "        { " + weldReadOuters("z") + "; }" +
        "      }" +
        "    }" +
        "  }" +
        "}));");
    checkSucceeds(
        "6; for (k in x) { k; }",
        "var x0___;" +
        "var x1___;" +
        "6;" +
        "{" +
        "  x0___ = " + weldReadOuters("x") + ";" +
        "  for (x1___ in x0___) {" +
        "    if (___.canEnumPub(x0___, x1___)) {" +
        "      " + weldSetOuters("k", "x1___") + ";" +
        "      { " + weldReadOuters("k") + "; }" +
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
        "  {" +
        "    x0___ = " + weldReadOuters("x") + ";" +
        "    for (x1___ in x0___) {" +
        "      if (___.canEnumPub(x0___, x1___)) {" +
        "        " + weldSetOuters("k", "x1___") + ";" +
        "        { " + weldReadOuters("k") + "; }" +
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
        "    x0___ = " + weldReadOuters("x") + ";" +
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
        "___OUTERS___.foo = undefined;" +
        weldSetOuters(
            "foo",
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
        "12;" +
        "{" +
        "  x0___ = ___OUTERS___;" +
        "  for (x1___ in x0___) {" +
        "    if (___.canEnumPub(x0___, x1___)) {" +
        "      " + weldSetOuters("k", "x1___") + ";" +
        "      { " + weldReadOuters("k") + "; }" +
        "    }" +
        "  }" +
        "}");
    checkSucceeds(
        "13; function foo() {" +
        "  for (k in this) { k; }" +
        "}",
        "___OUTERS___.foo = undefined;" +
        weldSetOuters(
            "foo",
            "(function () {" +
            "  ___.splitCtor(foo, foo_init___);" +
            "  function foo(var_args) {" +
            "    return new foo.make___(arguments);" +
            "  }" +
            "  function foo_init___() {" +
            "    var t___ = this;" +
            "    var x0___;" +
            "    var x1___;" +
            "    {" +
            "      x0___ = t___;" +
            "      for (x1___ in x0___) {" +
            "        if (___.canEnumProp(x0___, x1___)) {" +
            "          " + weldSetOuters("k", "x1___") + ";" +
            "          { " + weldReadOuters("k") + "; }" +
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
        "___OUTERS___.foo = undefined;" +
        weldSetOuters(
            "foo",
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
        "  " + weldReadOuters("e") + ";" +
        "  " + weldReadOuters("x") + ";" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    e;" +
        "    " + weldReadOuters("y") + ";" +
        "  }" +
        "}");
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
        "  " + weldReadOuters("e") + ";" +
        "  " + weldReadOuters("x") + ";" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    e;" +
        "    " + weldReadOuters("y") + ";" +
        "  }" +
        "} finally {" +
        "  " + weldReadOuters("e") + ";" +
        "  " + weldReadOuters("z") + ";" +
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
        "  " + weldReadOuters("x") + ";" +
        "} finally {" +
        "  " + weldReadOuters("z") + ";" +
        "}");
  }

  public void testVarArgs() throws Exception {
    checkSucceeds(
        "var foo = function() {" +
        "  p = arguments;" +
        "};",
        weldSetOuters(
            "foo",
            "___.primFreeze(___.simpleFunc(function() {" +
            "  var a___ = ___.args(arguments);" +
               weldSetOuters("p", "a___") +
            "}))"));
  }

  public void testVarThis() throws Exception {
    checkSucceeds(
        "function foo() {" +
        "  p = this;" +
        "}",
        "___OUTERS___.foo = undefined;" +
        weldSetOuters(
            "foo",
            "(function () {" +
            "  ___.splitCtor(foo, foo_init___);" +
            "  function foo(var_args) {" +
            "    return new foo.make___(arguments);" +
            "  }" +
            "  function foo_init___() {" +
            "    var t___ = this;" +
            "    " + weldSetOuters("p", "t___") + ";" +
            "  }" +
            "  return foo;" +
            "})()") +
        ";;");
    checkSucceeds(
        "this;",
        "___OUTERS___;");
    checkSucceeds(
        "try { } catch (e) { this; }",
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    ___OUTERS___;" +
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
        "___OUTERS___.foo = undefined;" +
        weldSetOuters("foo", "___.simpleFunc(function foo() {})") + ";;" +
        weldSetOuters("f", "___.primFreeze(" + weldReadOuters("foo") + ")") + ";");
  }

  public void testVarGlobal() throws Exception {
    checkSucceeds(
        "foo;",
        weldReadOuters("foo"));
    checkSucceeds(
        "function() {" +
        "  foo;" +
        "}",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  " + weldReadOuters("foo") + ";" +
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
        weldReadOuters("x", false) + ";");
    checkSucceeds(
        "try { } catch (e) { this.x; }",
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    " + weldReadOuters("x", false) + ";" +
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
        "        " + weldSetOuters("p", "t___.x_canRead___ ? t___.x : ___.readProp(t___, 'x')") +
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
        weldSetOuters(
            "p",
            "(function() {" +
            "  var x___ = " + weldReadOuters("foo") + ";" +
            "  return x___.p_canRead___ ? x___.p : ___.readPub(x___, 'p');" +
            "})()"));
  }

  public void testReadIndexGlobal() throws Exception {
    checkSucceeds(
        "this[3];",
        "___.readPub(___OUTERS___, 3);");
    checkSucceeds(
        "try { } catch (e) { this[3]; }",
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    ___.readPub(___OUTERS___, 3);" +
        "  }" +
        "}");
  }

  public void testReadIndexInternal() throws Exception {
    checkSucceeds(
        "function foo() { p = this[3]; }",
        "___OUTERS___.foo = undefined;" +
        weldSetOuters(
            "foo",
            "(function () {" +
            "  ___.splitCtor(foo, foo_init___);" +
            "  function foo(var_args) {" +
            "    return new foo.make___(arguments);" +
            "  }" +
            "  function foo_init___() {" +
            "    var t___ = this;" +
            "    " + weldSetOuters("p", "___.readProp(t___, 3)") +
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
        "    var foo;" +
        "    " + weldSetOuters("p", "___.readPub(foo, 3)") +
        "  }" +
        "));");
  }

  public void testSetGlobal() throws Exception {
    checkSucceeds(
        "x = 3;",
        weldSetOuters(
            "x",
            "3") +
        ";");
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
        weldSetOuters("p", weldReadOuters("x")));
    checkSucceeds(
        "try { } catch (e) { this.p = x; }",
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    " + weldSetOuters("p", weldReadOuters("x")) +
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
        "        (function() {" +
        "          var x___ = " + weldReadOuters("x") + ";" +
        "          return t___.p_canSet___ ?" +
        "              (t___.p = x___) : " +
        "              ___.setProp(t___, 'p', x___);" +
        "        })();" +
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
        "  ___.setMember(foo, 'p', " + weldReadOuters("x") + ");" +
        "}));");
    checkSucceeds(
        "function() {" +
        "  function foo() {}" +
        "  foo.prototype.p = function(a, b) { this; }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo;" +
        "  var foo = ___.simpleFunc(function foo() {});" +
        "  ;" +
        "  ___.setMember(" +
        "      foo, 'p', ___.method(" +
        // TODO(mikesamuel): Should not reevaluate foo if it is a global.
        "          foo," +
        "          function(a, b) {" +
        "            var t___ = this;" +
        "            t___;" +
        "          }));" +
        "}));");
    checkSucceeds(  // Doesn't trigger setMember but should.
        "foo.bar.prototype.baz = boo;",
        "(function () {" +
        "  var x___ = (" +
        "      function () {" +
        "        var x___ = (" +
        "            function () {" +
        "              var x___ = " + weldReadOuters("foo") + ";" +
        "              return x___.bar_canRead___ ? x___.bar : ___.readPub(x___, 'bar');" +
        "            })();" +
        "        return x___.prototype_canRead___ ? x___.prototype : ___.readPub(x___, 'prototype');" +
        "      })();" +
        "  var x0___ = " + weldReadOuters("boo") + ";" +
        "  return x___.baz_canSet___ ? (x___.baz = x0___) : ___.setPub(x___, 'baz', x0___);" +
        "})();");
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
        "  ___.setPub(foo, 'p', " + weldReadOuters("x") + ");" +
        "}));");
  }

  public void testSetPublic() throws Exception {
    checkSucceeds(
        "function() {" +
        "  var x = undefined;" +
        "  x.p = y;" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var x = undefined;" +
        "  (function() {" +
        "    var x___ = x;" +
        "    var x0___ = " + weldReadOuters("y") + ";" +
        "    return x___.p_canSet___ ?" +
        "        (x___.p = x0___) : " +
        "        ___.setPub(x___, 'p', x0___);" +
        "  })();" +
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
        "      ___.setProp(t___, " + weldReadOuters("x") + ", " + weldReadOuters("y") + ");" +
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
        "  ___.setPub(o, " + weldReadOuters("x") + ", " + weldReadOuters("y") + ");" +
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
        "  var v = " + weldReadOuters("x") + ";" +
        "}));");
    checkSucceeds(
        "var v = x",
        weldSetOuters("v", weldReadOuters("x")));
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
        "___.setPub(___OUTERS___, 'v', ___.readPub(___OUTERS___, 'v'));");
    checkSucceeds(
        "try { } catch (e) { var v; }",
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    ___.setPub(___OUTERS___, 'v', ___.readPub(___OUTERS___, 'v'));" +
        "  }" +
        "}");
  }

  public void testSetVar() throws Exception {
    checkSucceeds(
        "x = y;",
        weldSetOuters("x", weldReadOuters("y")));
    checkSucceeds(
        "function() {" +
        "  var x;" +
        "  x = y;" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var x;" +
        "  x = " + weldReadOuters("y") + ";" +
        "}));");
  }

  public void testSetReadModifyWriteLocalVar() throws Exception {
    checkFails("x__ *= 2", "Variables cannot end in \"__\"");

    checkSucceeds(
        "x += 1",
        "___.setPub(___OUTERS___, 'x',"
        + "  ___.readPub(___OUTERS___, 'x', true) + 1)");
    checkSucceeds(
        "(function (x) { x += 1; })",
        "(___.primFreeze(___.simpleFunc(function (x) { x = x + 1; })))");
    checkSucceeds(
        "myArray().key += 1",
        "var x0___;"
        + "(function () {"
        + "  x0___ = ___.asSimpleFunc(" + weldReadOuters("myArray") + ")();"
        + "  return ___.setPub(x0___, 'key',"
        + "                    ___.readPub(x0___, 'key', false) + 1);"
        + "})()");
    checkSucceeds(
        "myArray()[myKey()] += 1",
        "var x0___;"
        + "var x1___;"
        + "(function () {"
        + "  x0___ = ___.asSimpleFunc(" + weldReadOuters("myArray") + ")();"
        + "  x1___ = ___.asSimpleFunc(" + weldReadOuters("myKey") + ")();"
        + "  return ___.setPub(x0___, x1___,"
        + "                    ___.readPub(x0___, x1___, false) + 1);"
        + "})()");
    checkSucceeds(  // Local reference need not be assigned to a temp.
        "(function (myKey) { myArray()[myKey] += 1; })",
        "___.primFreeze(___.simpleFunc(function (myKey) {"
        + "  var x0___;"
        + "  (function () {"
        + "    x0___ = ___.asSimpleFunc(" + weldReadOuters("myArray")
        + ")();"
        + "    return ___.setPub(x0___, myKey,"
        + "                      ___.readPub(x0___, myKey, false) + 1);"
        + "  })()"
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
          weldReadOuters("y") + ";" +
          "}));");
    }
  }

  public void testSetIncrDecr() throws Exception {
    checkFails("x__--;", "Variables cannot end in \"__\"");

    checkSucceeds(
        "x++;",
        "(function() {" +
        "  var x___ = ___.readPub(___OUTERS___, 'x', true) - 0;" +
        "  ___.setPub(___OUTERS___, 'x', x___ + 1);" +
        "  return x___;" +
        "})();");
    checkSucceeds(
        "x--",
        "(function () {" +
        "  var x___ = ___.readPub(___OUTERS___, 'x', true) - 0;" +
        "  ___.setPub(___OUTERS___, 'x', x___ - 1);" +
        "  return x___;" +
        "})()");
    checkSucceeds(
        "++x",
        "___.setPub(___OUTERS___, 'x'," +
        " ___.readPub(___OUTERS___, 'x', true) - -1);");

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
        "(function () {" +
        "  x0___ = " + weldReadOuters("o") + ";" +
        "  var x___ = ___.readPub(x0___, 'x', false) - 0;" +
        "  ___.setPub(x0___, 'x', x___ + 1);" +
        "  return x___;" +
        "})()");

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
        "new (___.asCtor(___.primFreeze(" + weldReadOuters("Date") + ")))()");
  }

  public void testNewCtor() throws Exception {
    checkSucceeds(
        "function foo() { this.p = 3; }" +
        "new foo(x, y);",
        "___OUTERS___.foo = undefined;" +
        weldSetOuters(
            "foo",
            "(function () {" +
            "  ___.splitCtor(foo, foo_init___);" +
            "  function foo(var_args) {" +
            "    return new foo.make___(arguments);" +
            "  }" +
            "  function foo_init___() {" +
            "    var t___ = this;" +
            "    (function() {" +
            "      var x___ = 3;" +
            "      return t___.p_canSet___ ?" +
            "          (t___.p = x___) : " +
            "          ___.setProp(t___, 'p', x___);" +
            "    })();" +
            "  }" +
            "  return foo;" +
            "})()") + 
        ";;" +
        "new (___.asCtor(___.primFreeze(" + weldReadOuters("foo") + ")))" +
        "    (" + weldReadOuters("x") + ", " + weldReadOuters("y") + ");");
    checkSucceeds(
        "function foo() {}" +
        "new foo(x, y);",
        "___OUTERS___.foo = undefined;" +
        weldSetOuters("foo", "___.simpleFunc(function foo() {})") + ";;" +
        "new (___.asCtor(___.primFreeze(" + weldReadOuters("foo") + ")))" +
        "    (" + weldReadOuters("x") + ", " + weldReadOuters("y") + ");");
    checkSucceeds(
        "function foo() {}" +
        "new foo();",
        "___OUTERS___.foo = undefined;" +
        weldSetOuters("foo", "___.simpleFunc(function foo() {})") + ";;" +
        "new (___.asCtor(___.primFreeze(" + weldReadOuters("foo") + ")))();");
    checkSucceeds(
        "new foo.bar(0);",
        "new (___.asCtor((function () {" +
        "  var x___ = " + weldReadOuters("foo", true) + ";" +
        "  return x___.bar_canRead___ ? x___.bar: ___.readPub(x___, 'bar');" +
        "})()))(0)");
    assertConsistent(
        "var foo = { bar: Date };" +
        "(new foo.bar(0)).getFullYear()");
    checkSucceeds(
        "function() {" +
        "  new x(y, z);" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  new (___.asCtor(" + weldReadOuters("x") + "))" +
        "      (" + weldReadOuters("y") + ", " + weldReadOuters("z") + ");" +
        "}));");
  }

  public void testDeleteProp() throws Exception {
    checkFails("delete this.foo___", "Properties cannot end in \"__\"");
    checkSucceeds(
        "delete this[foo()]",
        "___.deleteProp(" +
        "    ___OUTERS___, ___.asSimpleFunc(" + weldReadOuters("foo") + ")())");
    checkSucceeds("delete this.foo_", "___.deleteProp(___OUTERS___, 'foo_')");
    checkSucceeds("function Ctor() { D.call(this); delete this.foo_; }",
                  "___OUTERS___.Ctor = undefined;" +
                  "(function () {" +
                  "  var x___ = (function () {" +
                  "    ___.splitCtor(Ctor, Ctor_init___);" +
                  "    function Ctor(var_args) {" +
                  "      return new Ctor.make___(arguments);" +
                  "    }" +
                  "    function Ctor_init___() {" +
                  "      var t___ = this;" +
                  "      (function () {" +
                  "        var x___ = ___OUTERS___.D_canRead___" +
                  "            ? ___OUTERS___.D" +
                  "            : ___.readPub(___OUTERS___, 'D', true);" +
                  "        var x0___ = t___;" +
                  "        return x___.call_canCall___" +
                  "            ? x___.call(x0___)" +
                  "            : ___.callPub(x___, 'call', [x0___]);" +
                  "      })();" +
                  // The important bit.  t___ used locally.
                  "      ___.deleteProp(t___, 'foo_');" +
                  "    }" +
                  "    return Ctor;" +
                  "  })();" +
                  "  return ___OUTERS___.Ctor_canSet___" +
                  "      ? (___OUTERS___.Ctor = x___)" +
                  "      : ___.setPub(___OUTERS___, 'Ctor', x___);" +
                  "})();;");
    assertConsistent(
        // Set up a class that can delete one of its members.
        "function P() { this; }" +
        "caja.def(P, Object, {" +
        "  toString : function () {" +
        "    var pairs = [];" +
        "    for (var k in this) {" +
        "      if (typeof this[k] !== 'function') {" +
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
        "hist.toString()");
    assertConsistent(
        "var foo = 0;" +
        "var preContained = 'foo' in this ? 'prev-in' : 'prev-not-in';" +
        "var deleted = (delete this.foo) ? 'deleted' : 'not-deleted';" +
        "var afterContained = 'foo' in this ? 'post-in' : 'post-not-in';" +
        "var outcome = [preContained, deleted, afterContained].join();" +
        "assertTrue(outcome, outcome === 'prev-in,not-deleted,post-in'" +
        "           || outcome === 'prev-in,deleted,post-not-in')");
  }

  public void testDeletePub() throws Exception {
    checkFails("delete x.foo___", "Variables cannot end in \"__\"");
    checkSucceeds(
        "delete foo()[bar()]",
        "___.deletePub(___.asSimpleFunc(" + weldReadOuters("foo") + ")()," +
        "              ___.asSimpleFunc(" + weldReadOuters("bar") + ")())");
    checkSucceeds(
        "delete foo().bar",
        "___.deletePub(___.asSimpleFunc(" + weldReadOuters("foo") + ")()," +
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
    checkFails("delete ___OUTERS___", "Variables cannot end in \"__\"");
    checkSucceeds(
        "delete foo",
        "___.deletePub(___OUTERS___, 'foo')"
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
        "(function() {" +
        "  var x0___ = " + weldReadOuters("x") + ";" +
        "  var x1___ = " + weldReadOuters("y") + ";" +
        "  return ___OUTERS___.f_canCall___ ?" +
        "      ___OUTERS___.f(x0___, x1___) :" +
        "      ___.callPub(___OUTERS___, 'f', [x0___, x1___]);" +
        "})();");
    checkSucceeds(
        "try { } catch (e) { this.f(x, y); }",
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    (function() {" +
        "      var x0___ = " + weldReadOuters("x") + ";" +
        "      var x1___ = " + weldReadOuters("y") + ";" +
        "      return ___OUTERS___.f_canCall___ ?" +
        "          ___OUTERS___.f(x0___, x1___) :" +
        "          ___.callPub(___OUTERS___, 'f', [x0___, x1___]);" +
        "    })();" +
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
        "      (function() {" +
        "        var x0___ = " + weldReadOuters("x") + ";" +
        "        var x1___ = " + weldReadOuters("y") + ";" +
        "        return t___.f_canCall___ ?" +
        "            t___.f(x0___, x1___) :" +
        "            ___.callProp(t___, 'f', [x0___, x1___]);" +
        "      })();" +
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
        "___OUTERS___.Point = undefined;" +
        weldSetOuters("Point", "___.simpleFunc(function Point() {})") + ";" +
        "___OUTERS___.WigglyPoint = undefined;" +
        weldSetOuters("WigglyPoint", "___.simpleFunc(function WigglyPoint() {})") + ";;" +
        "caja.def(" + weldReadOuters("Point") + ", " + weldReadOuters("Object") + ");;" +
        "caja.def(" + weldReadOuters("WigglyPoint") + ", " + weldReadOuters("Point") + ");");
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
        "___OUTERS___.Point = undefined;" +
        weldSetOuters("Point", "___.simpleFunc(function Point() {})") + ";" +
        "___OUTERS___.WigglyPoint = undefined;" +
        weldSetOuters("WigglyPoint", "___.simpleFunc(function WigglyPoint() {})") + ";" +
        ";;" +
        "caja.def(" + weldReadOuters("WigglyPoint") + ", " + weldReadOuters("Point") + ", {" +
        "    m0: " + weldReadOuters("x") + "," +
        "    m1: ___.method(" + weldReadOuters("WigglyPoint") + ", function() {" +
        "      var t___ = this;" +
        "      (function() {" +
        "        var x___ = 3;" +
        "        return t___.p_canSet___ ?" +
        "            (t___.p = x___) : " +
        "            ___.setProp(t___, 'p', x___);" +
        "      })();" +
        "    })" +
        "});");
    checkSucceeds(
        "function Point() {}" +
        "function WigglyPoint() {}" +
        "caja.def(WigglyPoint, Point," +
        "    { m0: x, m1: function() { this.p = 3; } }," +
        "    { s0: y, s1: function() { return 3; } });",
        "___OUTERS___.Point = undefined;" +
        weldSetOuters("Point", "___.simpleFunc(function Point() {})") + ";" +
        "___OUTERS___.WigglyPoint = undefined;" +
        weldSetOuters("WigglyPoint", "___.simpleFunc(function WigglyPoint() {})") + ";" +
        ";;" +
        "caja.def(" + weldReadOuters("WigglyPoint") + ", " + weldReadOuters("Point") + ", {" +
        "    m0: " + weldReadOuters("x") + "," +
        "    m1: ___.method(" + weldReadOuters("WigglyPoint") + ", function() {" +
        "      var t___ = this;" +
        "      (function() {" +
        "        var x___ = 3;" +
        "        return t___.p_canSet___ ?" +
        "            (t___.p = x___) : " +
        "            ___.setProp(t___, 'p', x___);" +
        "      })();" +
        "    })" +
        "}, {" +
        "    s0: " + weldReadOuters("y") + "," +
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
        "(function() {" +
        "  var x___ = " + weldReadOuters("o") + ";" +
        "  var x0___ = " + weldReadOuters("x") + ";" +
        "  var x1___ = " + weldReadOuters("y") + ";" +
        "  return x___.m_canCall___ ?" +
        "    x___.m(x0___, x1___) :" +
        "    ___.callPub(x___, 'm', [x0___, x1___]);" +
        "})();");
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
        "          " + weldReadOuters("x") + "," +
        "          [" + weldReadOuters("y") + ", " + weldReadOuters("z") + "]);" +
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
        "    " + weldReadOuters("x") + ", " +
        "    " + weldReadOuters("y") + ", " +
        "    [" + weldReadOuters("z") + ", " + weldReadOuters("t") + "]);");
  }

  public void testCallFunc() throws Exception {
    checkSucceeds(
        "function() { var f; f(x, y); }",
        "___.primFreeze(___.simpleFunc(function() {" +
        "    var f;" +
        "    ___.asSimpleFunc(f)" +
        "        (" + weldReadOuters("x") + ", " + weldReadOuters("y") + ");" +
        "}));");
    checkSucceeds(
        "foo(x, y);",
        "___.asSimpleFunc(" + weldReadOuters("foo") + ")(" +
        "    " + weldReadOuters("x") + "," +
        "    " + weldReadOuters("y") +
        ");");
  }

  public void testFuncAnonSimple() throws Exception {
    checkSucceeds(
        "function(x, y) { x = arguments; y = z; };",
        "___.primFreeze(___.simpleFunc(function(x, y) {" +
        "  var a___ = ___.args(arguments);" +
        "  x = a___;" +
        "  y = " + weldReadOuters("z") + ";" +
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
        "      y = " + weldReadOuters("z") + ";" +
        "      return ___.asSimpleFunc(___.primFreeze(foo))(x - 1, y - 1);" +
        "  });" +
        "  ;"+
        "}));");
    checkSucceeds(
        "function foo(x, y ) {" +
        "  return foo(x - 1, y - 1);" +
        "}",
        "___OUTERS___.foo = undefined;" +
        weldSetOuters(
            "foo",
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
        "      y = " + weldReadOuters("z") + ";" +
        "      return ___.asSimpleFunc(___.primFreeze(foo))(x - 1, y - 1);" +
        "  }));"+
        "}));");
    checkSucceeds(
        "var foo = function foo(x, y ) {" +
        "  return foo(x - 1, y - 1);" +
        "}",
        weldSetOuters(
            "foo",
            "___.primFreeze(___.simpleFunc(function foo(x, y) {" +
            "  return ___.asSimpleFunc(___.primFreeze(foo))(x - 1, y - 1);" +
            "}))"));
  }

  public void testFuncExophoricFunction() throws Exception {
    checkSucceeds(
        "function (x) { return this.x; };",
        "___.exophora(" +
        "    function (x) {" +
        "       var t___ = this;" +
        "       return (function () {" +
        "         var x___ = t___;" +
        "         return x___.x_canRead___ ? x___.x : ___.readPub(x___, 'x');" +
        "       })();" +
        "     })"
        );
    checkFails(
        "function (k) { return this[k]; }",
        "\"this\" in an exophoric function only exposes public fields");
    checkFails(
        "function () { delete this.k; }",
        "\"this\" in an exophoric function only exposes public fields");
    checkFails(
        "function () { x in this; }",
        "\"this\" in an exophoric function only exposes public fields");
    checkFails(
        "function () { 'foo_' in this; }",
        "\"this\" in an exophoric function only exposes public fields");
    checkSucceeds(
        "function () { 'foo' in this; }",
        "___.exophora(" +
        "    function () {" +
        "      var t___ = this;" +
        "      'foo' in t___;" +
        "    })");
    checkFails(
        "function () { for (var k in this); }",
        "\"this\" in an exophoric function only exposes public fields");
    checkFails(
        "function (y) { this.x = y; }",
        "\"this\" in an exophoric function only exposes public fields");

    assertConsistent(
        "({ f7: function () { return this.x + this.y; }, x: 1, y: 2 }).f7()");
  }

  public void testFuncBadMethod() throws Exception {
    checkFails(
        "function(x) { this.x_ = x; };",
        "Public properties cannot end in \"_\"");
  }

  public void testFuncCtor() throws Exception {
    checkSucceeds(
        "function Foo(x) { this.x_ = x; }",
        "___OUTERS___.Foo = undefined;" +
        "(function () {" +
        "    var x___ = (function () {" +
        "        ___.splitCtor(Foo, Foo_init___);" +
        "        function Foo(var_args) {" +
        "          return new Foo.make___(arguments);" +
        "        }" +
        "        function Foo_init___(x) {" +
        "          var t___ = this;" +
        "          (function () {" +
        "              var x___ = x;" +
        "              return t___.x__canSet___ ? (t___.x_ = x___) : ___.setProp(t___, 'x_', x___);" +
        "            })();" +
        "        }" +
        "        return Foo;" +
        "      })();" +
        "    return ___OUTERS___.Foo_canSet___ ? (___OUTERS___.Foo = x___) : ___.setPub(___OUTERS___, 'Foo', x___);" +
        "  })();" +
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
        "          (function () {" +
        "              var x___ = x;" +
        "              return t___.x__canSet___ ? (t___.x_ = x___) : ___.setProp(t___, 'x_', x___);" +
        "            })();" +
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
        "___OUTERS___.Foo = undefined;" +
        "(function () {" +
        "    var x___ = (function () {" +
        "        ___.splitCtor(Foo, Foo_init___);" +
        "        function Foo(var_args) {" +
        "          return new Foo.make___(arguments);" +
        "        }" +
        "        function Foo_init___(x) {" +
        "          var t___ = this;" +
        "          (function () {" +
        "              var x___ = x;" +
        "              return t___.x__canSet___ ? (t___.x_ = x___) : ___.setProp(t___, 'x_', x___);" +
        "            })();" +
        "        }" +
        "        return Foo;" +
        "      })();" +
        "    return ___OUTERS___.Foo_canSet___ ? (___OUTERS___.Foo = x___) : ___.setPub(___OUTERS___, 'Foo', x___);" +
        "  })();" +
        "___OUTERS___.Bar = undefined;" +
        "(function () {" +
        "    var x___ = (function () {" +
        "        ___.splitCtor(Bar, Bar_init___);" +
        "        function Bar(var_args) {" +
        "          return new Bar.make___(arguments);" +
        "        }" +
        "        function Bar_init___(y) {" +
        "          var t___ = this;" +
        "          (___OUTERS___.Foo_canRead___ ? ___OUTERS___.Foo : ___.readPub(___OUTERS___, 'Foo', true)).call(this, 1);" +
        "          (function () {" +
        "              var x___ = y;" +
        "              return t___.y_canSet___ ? (t___.y = x___) : ___.setProp(t___, 'y', x___);" +
        "            })();" +
        "        }" +
        "        return Bar;" +
        "      })();" +
        "    return ___OUTERS___.Bar_canSet___ ? (___OUTERS___.Bar = x___) : ___.setPub(___OUTERS___, 'Bar', x___);" +
        "  })();" +
        "  ;;" +
        "(function () {" +
        "    var x___ = new (___.asCtor(___.primFreeze(" + weldReadOuters("Bar", true) + ")))(3);" +
        "    return ___OUTERS___.bar_canSet___ ? (___OUTERS___.bar = x___) : ___.setPub(___OUTERS___, 'bar', x___);" +
        "  })();");
  }

  public void testMapEmpty() throws Exception {
    checkSucceeds(
        "f = {};",
        weldSetOuters("f", "{}"));
  }

  public void testMapBadKeySuffix() throws Exception {
    checkFails(
        "var o = { x_: 3 };",
        "Key may not end in \"_\"");
  }

  public void testMapNonEmpty() throws Exception {
    checkSucceeds(
        "var o = { k0: x, k1: y };",
        weldSetOuters("o", "{ k0: " + weldReadOuters("x") + ", k1: " + weldReadOuters("y") + " }"));
  }

  public void testOtherInstanceof() throws Exception {
    checkSucceeds(
        "function foo() {}" +
        "x instanceof foo;",
        "___OUTERS___.foo = undefined;" +
        weldSetOuters("foo", "___.simpleFunc(function foo() {})") + ";;" +
        weldReadOuters("x") + " instanceof ___.primFreeze(" + weldReadOuters("foo") + ");");
    checkSucceeds(
        "x instanceof Object",
        weldReadOuters("x") + " instanceof ___.primFreeze(" + weldReadOuters("Object") + ");");

    assertConsistent("({}) instanceof Object");
    assertConsistent("(new Date) instanceof Date");
    assertConsistent("({}) instanceof Date");
    assertConsistent("function foo() {}; (new foo) instanceof foo");
    assertConsistent("function foo() {}; !(({}) instanceof foo)");
  }

  public void testOtherTypeof() throws Exception {
    checkSucceeds("typeof x;", "typeof ___.readPub(___OUTERS___, 'x');");
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
    checkFails("___OUTERS___: 1", "Labels cannot end in \"__\"");
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
                  "void (___.asSimpleFunc)(" + weldReadOuters("foo") + ")()");
    checkSucceeds("a, b", weldReadOuters("a") + "," + weldReadOuters("b"));
  }

  public void testMultiDeclaration() throws Exception {
    // 'var' in global scope, part of a block
    checkSucceeds(
        "var x, y;",
        "___.setPub(___OUTERS___, 'x', ___.readPub(___OUTERS___, 'x')), " +
        "___.setPub(___OUTERS___, 'y', ___.readPub(___OUTERS___, 'y'));");
    checkSucceeds(
        "var x = foo, y = bar;",
        weldSetOuters("x", weldReadOuters("foo")) + ", " +
        weldSetOuters("y", weldReadOuters("bar")) + ";");
    checkSucceeds(
        "var x, y = bar;",
        "___.setPub(___OUTERS___, 'x', ___.readPub(___OUTERS___, 'x')), " +
        weldSetOuters("y", weldReadOuters("bar")) + ";");
    // 'var' in global scope, 'for' statement
    checkSucceeds(
        "for (var x, y; ; ) {}",
        "for (___.setPub(___OUTERS___, 'x', ___.readPub(___OUTERS___, 'x')), " +
        "___.setPub(___OUTERS___, 'y', ___.readPub(___OUTERS___, 'y')); ; ) {}");
    checkSucceeds(
        "for (var x = foo, y = bar; ; ) {}",
        "for (" + weldSetOuters("x", weldReadOuters("foo")) + ", " +
        "    " + weldSetOuters("y", weldReadOuters("bar")) + "; ; ) {}");
    checkSucceeds(
        "for (var x, y = bar; ; ) {}",
        "for (___.setPub(___OUTERS___, 'x', ___.readPub(___OUTERS___, 'x')), " +
        weldSetOuters("y", weldReadOuters("bar")) + "; ; ) {}");
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
        "  var x = " + weldReadOuters("foo") + ", y = " + weldReadOuters("bar") + ";" +
        "}));");
    checkSucceeds(
        "function() {" +
        "  var x, y = bar;" +
        "}",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var x, y = " + weldReadOuters("bar") + ";" +
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
        "  for (var x = " + weldReadOuters("foo") + ", y = " + weldReadOuters("bar") + "; ; ) {}" +
        "}));");
    checkSucceeds(
        "function() {" +
        "  for (var x, y = bar; ; ) {}" +
        "}",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  for (var x, y = " + weldReadOuters("bar") + "; ; ) {}" +
        "}));");
    assertConsistent(
        "var arr = [1, 2, 3], k = -1;" +
        "(function () {" +
        "  var a = arr[++k], b = arr[++k], c = arr[++k];" +
        "  return [a, b, c].join(',');" +
        "})()");
  }

  public void testRecurseParseTreeNodeContainer() throws Exception {
    // Tested implicitly by other cases
  }

  public void testRecurseArrayConstructor() throws Exception {
    checkSucceeds(
        "foo = [ bar, baz ];",
        weldSetOuters(
            "foo",
            "[" + weldReadOuters("bar") + ", " + weldReadOuters("baz") + "]") +
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
        "switch (" + weldReadOuters("x") + ") { case 1: break; }");
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
        "if (" + weldReadOuters("x") + " === " + weldReadOuters("y") + ") {" +
        "  " + weldReadOuters("z") + ";" +
        "} else if (" + weldReadOuters("z") + " === " + weldReadOuters("y") + ") {" +
        "  " + weldReadOuters("x") + ";" +
        "} else {" +
        "  " + weldReadOuters("y") + ";" +
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
        "switch(" + weldReadOuters("x") + ") { default: break; }");
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
        "for (" + weldSetOuters("k", "0") + "; " +
        "     " + weldReadOuters("k") + " < 3;" +
        "     (function () {" +
        "       var x___ = ___.readPub(___OUTERS___, 'k', true) - 0;" +
        "       ___.setPub(___OUTERS___, 'k', x___ + 1);" +
        "       return x___;" +
        "     })()) {" +
        "  " + weldReadOuters("x") + ";" +
        "}");
    checkSucceeds(
        "function() {" +
        "  for (var k = 0; k < 3; k++) {" +
        "    x;" +
        "  }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  for (var k = 0; k < 3; k++) {" +
        "    " + weldReadOuters("x") + ";" +
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
        weldReadOuters("x") + " + " + weldReadOuters("y"));
    checkSucceeds(
        "1 + 2 * 3 / 4 - -5",
        "1 + 2 * 3 / 4 - -5");
    checkSucceeds(
        "x  = y = 3;",
        weldSetOuters("x", weldSetOuters("y", "3")));
  }

  public void testRecurseReturnStmt() throws Exception {
    checkSucceeds(
        "return x;",
        "return " + weldReadOuters("x") + ";");
  }

  public void testRecurseSwitchStmt() throws Exception {
    checkSucceeds(
        "switch (x) { }",
        "switch (" + weldReadOuters("x") + ") { }");
  }

  public void testRecurseThrowStmt() throws Exception {
    checkSucceeds(
        "throw x;",
        "throw " + weldReadOuters("x") + ";");
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

  public void testAssertConsistent() throws Exception {
    try {
      // A value that cannot be consistent across invocations.
      assertConsistent("({})");
      fail("assertConsistent not working");
    } catch (AssertionFailedError e) {
      // Pass
    }
  }

  private void setSynthetic(ParseTreeNode n) {
    SyntheticNodes.s(n);
  }

  private void setTreeSynthetic(ParseTreeNode n) {
    setSynthetic(n);
    for (ParseTreeNode child : n.children()) {
      setTreeSynthetic(child);
    }
  }

  private void checkFails(String input, String error) throws Exception {
    mq.getMessages().clear();
    ParseTreeNode expanded = new DefaultCajaRewriter(true)
        .expand(js(fromString(input)), mq);

    assertFalse(render(expanded), mq.getMessages().isEmpty());

    StringBuilder messageText = new StringBuilder();
    for (Message m : mq.getMessages()) {
      m.format(mc, messageText);
      messageText.append("\n");
    }
    assertTrue(
        "Messages do not contain \"" + error + "\": " + messageText.toString(),
        messageText.toString().contains(error));
  }

  private void checkSucceeds(
      ParseTreeNode inputNode,
      ParseTreeNode expectedResultNode)
      throws Exception {
    mq.getMessages().clear();
    ParseTreeNode actualResultNode = new DefaultCajaRewriter().expand(inputNode, mq);
    for (Message m : mq.getMessages()) {
      if (m.getMessageLevel().compareTo(MessageLevel.WARNING) >= 0) {
        fail(m.toString());
      }
    }
    if (expectedResultNode != null) {
      // Test that the source code-like renderings are identical. This will catch any
      // obvious differences between expected and actual.
      assertEquals(render(expectedResultNode), render(actualResultNode));
      // Then, for good measure, test that the S-expression-like formatted representations
      // are also identical. This will catch any differences in tree topology that somehow
      // do not appear in the source code representation (usually due to programming errors).
      assertEquals(
          TestUtil.format(expectedResultNode),
          TestUtil.format(actualResultNode));
    }
  }

  private void checkSucceeds(String input, String expectedResult) throws Exception {
    checkSucceeds(js(fromString(input)), js(fromString(expectedResult)));
  }

  private void checkSucceeds(CharProducer cp) throws Exception {
    checkSucceeds(js(cp), null);
  }

  /**
   * Asserts that the given caja code produces the same value both cajoled and
   * uncajoled.
   *
   * @param caja executed in the context of asserts.js for its value.  The
   *    value is computed from the last statement in caja.
   */
  private void assertConsistent(String caja)
      throws IOException, ParseException {
    assertConsistent(null, caja);
  }
  private void assertConsistent(String message, String caja)
      throws IOException, ParseException {
    // Make sure the tree assigns the result to the unittestResult___ var.
    Object uncajoledResult = RhinoTestBed.runJs(
        null,
        new RhinoTestBed.Input(
            "var caja = { def: function (clazz, sup, props, statics) {" +
            "  function t() {}" +
            "  sup && (t.prototype = sup.prototype, clazz.prototype = new t);" +
            "  for (var k in props) { clazz.prototype[k] = props[k]; }" +
            "  for (var k in (statics || {})) { clazz[k] = statics[k]; }" +
            "} };",
            "caja-stub"),
        new RhinoTestBed.Input(getClass(), "../../plugin/asserts.js"),
        new RhinoTestBed.Input(caja, getName() + "-uncajoled"));

    mq.getMessages().clear();

    Statement cajaTree = replaceLastStatementWithEmit(
        js(fromString(caja, is)), "unittestResult___");
    String cajoledJs = render(
        cajole(js(fromResource("../../plugin/asserts.js")), cajaTree));

    for (Message msg : mq.getMessages()) {
      if (MessageLevel.ERROR.compareTo(msg.getMessageLevel()) <= 0) {
        fail(msg.format(mc));
      }
    }

    Object cajoledResult = RhinoTestBed.runJs(
        null,
        new RhinoTestBed.Input(
            getClass(), "/com/google/caja/plugin/console-stubs.js"),
        new RhinoTestBed.Input(getClass(), "/com/google/caja/caja.js"),
        new RhinoTestBed.Input(
            // Initialize the output field to something containing a unique
            // object value that will not compare identically across runs.
            "var unittestResult___ = { toString:\n" +
            "    function () { return '--NO-RESULT--'; }}\n" +
            // Set up the outers environment.
            "var testOuters = ___.copy(___.sharedOuters);\n" +
            "___.getNewModuleHandler().setOuters(testOuters);",
            getName()),
        // Load the cajoled code.
        new RhinoTestBed.Input(
            "___.loadModule(function (___OUTERS___) {" + cajoledJs + "\n});",
            getName() + "-cajoled"),
        // Return the output field as the value of the run.
        new RhinoTestBed.Input("unittestResult___", getName()));

    System.err.println("Result: " + cajoledResult + " for " + getName());
    assertEquals(message, uncajoledResult, cajoledResult);
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
}
