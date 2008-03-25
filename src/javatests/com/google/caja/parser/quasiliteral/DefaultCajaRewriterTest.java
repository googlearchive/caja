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

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.js.Block;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.TestUtil;
import com.google.caja.plugin.SyntheticNodes;
import junit.framework.TestCase;

import java.util.Collections;

/**
 * @author ihab.awad@gmail.com
 */
public class DefaultCajaRewriterTest extends TestCase {

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
    return
        "(___OUTERS___." + varName + "_canRead___ ?" +
        "    ___OUTERS___." + varName + ":" +
        "    ___.readPub(___OUTERS___, '" + varName + "', true))";
  }

  ////////////////////////////////////////////////////////////////////////
  // Handling of synthetic nodes
  ////////////////////////////////////////////////////////////////////////

  public void testSyntheticIsUntouched() throws Exception {
    ParseTreeNode input = TestUtil.parse(
        "function foo() { this; arguments; }");
    setTreeSynthetic(input);
    checkSucceeds(input, input);
  }

  public void testNestedInsideSyntheticIsExpanded() throws Exception {
    ParseTreeNode innerInput = TestUtil.parse("function foo() {}");
    ParseTreeNode input = ParseTreeNodes.newNodeInstance(
        Block.class,
        null,
        Collections.singletonList(innerInput));
    setSynthetic(input);
    ParseTreeNode expectedResult = TestUtil.parse(
        "{" + weldSetOuters("foo", "___.simpleFunc(function foo() {})") + "}");
    checkSucceeds(input, expectedResult);
  }

  ////////////////////////////////////////////////////////////////////////
  // Handling of nested blocks
  ////////////////////////////////////////////////////////////////////////

  public void testNestedBlockWithFunction() throws Exception {
    checkSucceeds(
        "{ function foo() {} }",
        "{" + weldSetOuters("foo", "___.simpleFunc(function foo() {})") + "}");
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
    // Our parser does not recognize "with" at all.
  }

  public void testForeach() throws Exception {
    if (false) {
    // TODO(ihab.awad): Enable when http://code.google.com/p/google-caja/issues/detail?id=68 fixed
    checkSucceeds(
        "1; for (var k in x) { k; }",
        // TOOD(ihab.awad): review welds        
        "1; {" +
        "  ___OUTERS___.x0___ = " + weldReadOuters("x") + ";" +
        "  ___OUTERS___.x1___ = undefined;" +
        "  ;" +
        "  for (___OUTERS___.x1 in ___OUTERS___.x0___) {" +
        "    if (___.canEnumPub(___OUTERS___.x0___, ___OUTERS___.x1___)) {" +
        "      " + weldSetOuters("k", "___OUTERS___.x1___") + ";" +
        "      { " + weldReadOuters("k") + "; }" +
        "    }" +
        "  }" +
        "}");
    }
    if (false) {
    // TODO(ihab.awad): Enable when http://code.google.com/p/google-caja/issues/detail?id=68 fixed
    checkSucceeds(
        "2; try { } catch (e) { for (var k in x) { k; } }",
        "2; try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    {" +
        "      ___OUTERS___.x0___ = " + weldReadOuters("x") + ";" +
        "      ___OUTERS___.x1___ = undefined;" +
        "      ;" +
        "      for (___OUTERS___.x1 in ___OUTERS___.x0___) {" +
        "        if (___.canEnumPub(___OUTERS___.x0___, ___OUTERS___.x1___)) {" +
        "          " + weldSetOuters("k", "___OUTERS___.x1___") + ";" +
        "          { " + weldReadOuters("k") + "; }" +
        "        }" +
        "      }" +
        "    }" +
        "  }" +
        "}");
    }
    checkSucceeds(
        "3; function() {" +
        "  for (var k in x) { k; }" +
        "};",
        "3; ___.primFreeze(___.simpleFunc(function() {" +
        "  {" +
        "    var x0___ = " + weldReadOuters("x") + ";" +
        "    var x1___ = undefined;" +
        "    var k;" +
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
        "  {" +
        "    var x0___ = " + weldReadOuters("x") + ";" +
        "    var x1___ = undefined;" +
        "    var k;" +
        "    for (x1___ in x0___) {" +
        "      if (___.canEnumPub(x0___, x1___)) {" +
        "        k = x1___;" +
        "        k;" +
        "      }" +
        "    }" +
        "  }" +
        "}));");
    if (false) {
    // TODO(ihab.awad): Enable when http://code.google.com/p/google-caja/issues/detail?id=68 fixed
    checkSucceeds(
        "5; function() {" +
        "  for (z[0] in x) { z; }" +
        "};",
        "5; ___.primFreeze(___.simpleFunc(function() {" +
        "  {" +
        "    var x0___ = " + weldReadOuters("x") + ";" +
        "    var x1___ = undefined;" +
        "    for (x1___ in x0___) {" +
        "      if (___.canEnumPub(x0___, x1___)) {" +
        "        ___.simpleFunc(" + weldReadOuters("z") + ")[0] = x1___;" +
        "        { " + weldReadOuters("z") + "; }" +
        "      }" +
        "    }" +
        "  }" +
        "}));");
    }
    if (false) {
    // TODO(ihab.awad): Enable when http://code.google.com/p/google-caja/issues/detail?id=68 fixed
    checkSucceeds(
        "6; for (k in x) { k; }",
        "6; {" +
        "  ___OUTERS___.x0___ = " + weldReadOuters("x") + ";" +
        "  ___OUTERS___.x1___ = undefined;" +
        "  for (___OUTERS___.x1 in ___OUTERS___.x0___) {" +
        "    if (___.canEnumPub(___OUTERS___.x0___, ___OUTERS___.x1___)) {" +
        "      " + weldSetOuters("k", "___OUTERS___.x1___") + ";" +
        "      { " + weldReadOuters("k") + "; }" +
        "    }" +
        "  }" +
        "}");
    }
    checkSucceeds(
        "7; function() {" +
        "  for (k in x) { k; }" +
        "};",
        "7; ___.primFreeze(___.simpleFunc(function() {" +
        "  {" +
        "    var x0___ = " + weldReadOuters("x") + ";" +
        "    var x1___ = undefined;" +
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
        "8; ___.primFreeze(___.simpleFunc(function() {" +
        "  var k;" +
        "  {" +
        "    var x0___ = " + weldReadOuters("x") + ";" +
        "    var x1___ = undefined;" +
        "    for (x1___ in x0___) {" +
        "      if (___.canEnumPub(x0___, x1___)) {" +
        "        k = x1___;" +
        "        { k; }" +
        "      }" +
        "    }" +
        "  }" +
        "}));");
    if (false) {
    // TODO(ihab.awad): Enable when http://code.google.com/p/google-caja/issues/detail?id=68 fixed
    checkSucceeds(
        "9; for (y.k in x) { y.k; }",
        "9; {" +
        "  ___OUTERS___.x0___ = " + weldReadOuters("x") + ";" +
        "  ___OUTERS___.x1___ = undefined;" +
        "  for (___OUTERS___.x1 in ___OUTERS___.x0___) {" +
        "    if (___.canEnumPub(___OUTERS___.x0___, ___OUTERS___.x1___)) {" +
        "      " + weldReadOuters("y") + ".k = ___OUTERS___.x1___;" +
        "      { " + weldReadOuters("y") + ".k; }" +
        "    }" +
        "  }" +
        "}");
    }
    if (false) {
    // TODO(ihab.awad): Enable when http://code.google.com/p/google-caja/issues/detail?id=68 fixed
    checkSucceeds(
        "10; function() {" +
        "  for (y.k in x) { y.k; }" +
        "};",
        "10; ___.primFreeze(___.simpleFunc(function() {" +
        "  {" +
        "    var x0___ = " + weldReadOuters("x") + ";" +
        "    var x1___ = undefined;" +
        "    for (x1___ in x0___) {" +
        "      if (___.canEnumPub(x0___, x1___)) {" +
        "        " + weldReadOuters("y") + ".k = x1___;" +
        "        { " + weldReadOuters("y") + ".k; }" +
        "      }" +
        "    }" +
        "  }" +
        "}));");
    }
    checkSucceeds(
        "11; function foo() {" +
        "  for (var k in this) { k; }" +
        "}",
        "11;" +
        weldSetOuters(
            "foo",
            "(function () {" +
            "  ___.splitCtor(foo, foo_init___);" +
            "  function foo(var_args) {" +
            "    return new foo.make___(arguments);" +
            "  }" +
            "  function foo_init___() {" +
            "    var t___ = this;" +
            "    {" +
            "      var x0___ = t___;" +
            "      var x1___ = undefined;" +
            "      var k;" +
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
        ";");
    checkSucceeds(
        "12; for (var k in this) { k; }",
        "12; {" +
        "  ___OUTERS___.x0___ = ___OUTERS___;" +
        "  ___OUTERS___.x1___ = undefined;" +
        "  " + weldReadOuters("k") + ";" +
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
        "13;" +
        weldSetOuters(
            "foo",
            "(function () {" +
            "  ___.splitCtor(foo, foo_init___);" +
            "  function foo(var_args) {" +
            "    return new foo.make___(arguments);" +
            "  }" +
            "  function foo_init___() {" +
            "    var t___ = this;" +
            "    {" +
            "      var x0___ = t___;" +
            "      var x1___ = undefined;" +
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
        ";");
    checkSucceeds(
        "14; function foo() {" +
        "  var k;" +
        "  for (k in this) { k; }" +
        "}",
        "14;" +
        weldSetOuters(
            "foo",
            "(function () {" +
            "  ___.splitCtor(foo, foo_init___);" +
            "  function foo(var_args) {" +
            "    return new foo.make___(arguments);" +
            "  }" +
            "  function foo_init___() {" +
            "    var t___ = this;" +
            "    var k;" +
            "    {" +
            "      var x0___ = t___;" +
            "      var x1___ = undefined;" +
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
        ";");
    if (false) {
    // TODO(ihab.awad): Enable when http://code.google.com/p/google-caja/issues/detail?id=68 fixed
    checkSucceeds(
        "15; function foo() {" +
        "  for (y.k in this) { y.k; }" +
        "}",
        "15; ___OUTERS___.foo = ___.ctor(function foo() {" +
        "  var t___ = this;" +
        "  {" +
        "    var x0___ = t___;" +
        "    var x1___ = undefined;" +
        "    for (x1___ in x0___) {" +
        "      if (___.canEnumProp(x0___, x1___)) {" +
        "        " + weldReadOuters("y") + ".k = x1___;" +
        "        { " + weldReadOuters("y") + ".k; }" +
        "      }" +
        "    }" +
        "  }" +
        "});");
    }
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
            "})()"));
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
        "  var foo = ___.simpleFunc(function foo() {});" +
        "  var f = ___.primFreeze(foo);" +
        "}));");
    checkSucceeds(
        "function foo() {}" +
        "var f = foo;",
        weldSetOuters("foo", "___.simpleFunc(function foo() {})") + ";" +
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
        weldReadOuters("x") + ";");
    checkSucceeds(
        "try { } catch (e) { this.x; }",
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    " + weldReadOuters("x") + ";" +
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
            "})()"));
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
        "}));");
  }

  public void testSetMember() throws Exception {
    checkSucceeds(
        "function() {" +
        "  function foo() {}" +
        "  foo.prototype.p = x;" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo = ___.simpleFunc(function foo() {});" +
        "  (function() {" +
        "    var x___ = " + weldReadOuters("x") + ";" +
        "    return ___.setMember(foo, 'p', x___);" +
        "  })();" +
        "}));");
    checkSucceeds(
        "function() {" +
        "  function foo() {}" +
        "  foo.prototype.p = function(a, b) { this; }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo = ___.simpleFunc(function foo() {});" +
        "  (function() {" +
        "    var x___ = ___.method(foo, function(a, b) {" +
        "      var t___ = this;" +
        "      t___;" +
        "    });" +
        "    return ___.setMember(foo, 'p', x___);" +
        "  })();" +
        "}));");
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
        "  var foo = ___.simpleFunc(function foo() {});" +
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
    String[] ops = new String[] {
        "&=",
        "/=",
        "<<=",
        "%=",
        "*=",
        "&=",
        ">>=",
        "-=",
        "+=",
        ">>>=",
        "^=",
    };
    for (String op : ops) {
      checkSucceeds(
          "function() { var x; x " + op + " y; };",
          "___.primFreeze(___.simpleFunc(function() {" +
          "  var x; x " + op + " " + weldReadOuters("y") + ";" +
          "}));");
    }
  }

  public void testSetPostIncrGlobal() throws Exception {
    checkSucceeds(
        "x++;",
        "(function() {" +
        "  var x___ = Number(___.readPub(___OUTERS___, 'x', true));" +
        "  ___.setPub(___OUTERS___, 'x', x___ + 1);" +
        "  return x___;" +
        "})();");
  }

  public void testNewCtor() throws Exception {
    checkSucceeds(
        "function foo() { this.p = 3; }" +
        "new foo(x, y);",
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
            "})()") + ";" +
        "new (___.asCtor(" + weldReadOuters("foo") + "))" +
        "    (" + weldReadOuters("x") + ", " + weldReadOuters("y") + ");");
    checkSucceeds(
        "function foo() {}" +
        "new foo(x, y);",
        weldSetOuters("foo", "___.simpleFunc(function foo() {})") + ";" +
        "new (___.asCtor(" + weldReadOuters("foo") + "))" +
        "    (" + weldReadOuters("x") + ", " + weldReadOuters("y") + ");");
    checkSucceeds(
        "function foo() {}" +
        "new foo();",
        weldSetOuters("foo", "___.simpleFunc(function foo() {})") + ";" +
        "new (___.asCtor(" + weldReadOuters("foo") + "))();");
  }

  public void testNewBadCtor() throws Exception {
    checkFails(
        "new foo.bar();",
        "Cannot invoke \"new\" on an arbitrary expression");
    checkFails(
        "new 3();",
        "Cannot invoke \"new\" on an arbitrary expression");
    checkFails(
        "new (x + y)();",
        "Cannot invoke \"new\" on an arbitrary expression");
  }

  public void testNewFunc() throws Exception {
    checkSucceeds(
        "function() {" +
        "  new x(y, z);" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  new (___.asCtor(" + weldReadOuters("x") + "))" +
        "      (" + weldReadOuters("y") + ", " + weldReadOuters("z") + ");" +
        "}));");
  }

  public void testCallBadSuffix() throws Exception {
    checkFails(
        "x.p__(3, 4);",
        "Selectors cannot end in \"__\"");
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
        weldSetOuters("Point", "___.simpleFunc(function Point() {})") + ";" +
        "caja.def(" + weldReadOuters("Point") + ", " + weldReadOuters("Object") + ");" +
        weldSetOuters("WigglyPoint", "___.simpleFunc(function WigglyPoint() {})") + ";" +
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
        weldSetOuters("Point", "___.simpleFunc(function Point() {})") + ";" +
        weldSetOuters("WigglyPoint", "___.simpleFunc(function WigglyPoint() {})") + ";" +
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
        weldSetOuters("Point", "___.simpleFunc(function Point() {})") + ";" +
        weldSetOuters("WigglyPoint", "___.simpleFunc(function WigglyPoint() {})") + ";" +
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
        "function() {" +
        "  function Point() {}" +
        "  function WigglyPoint() {}" +
        "  caja.def(WigglyPoint, Point, { foo: x }, { bar: function() { this.x = 3; } });" +
        "};",
        "Anonymous function references \"this\"");
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
        "  var foo = ___.simpleFunc(function foo(x, y) {" +
        "      var a___ = ___.args(arguments);" +
        "      x = a___;" +
        "      y = " + weldReadOuters("z") + ";" +
        "      return ___.asSimpleFunc(___.primFreeze(foo))(x - 1, y - 1);" +
        "  });"+
        "}));");
    checkSucceeds(
        "function foo(x, y ) {" +
        "  return foo(x - 1, y - 1);" +
        "}",
        weldSetOuters(
            "foo",
            "___.simpleFunc(function foo(x, y) {" +
            "  return ___.asSimpleFunc(___.primFreeze(foo))(x - 1, y - 1);" +
            "})"));
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

  public void testFuncBadMethod() throws Exception {
    checkFails(
        "function(x) { x = this; };",
        "Anonymous function references \"this\"");
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
        weldSetOuters("foo", "___.simpleFunc(function foo() {})") + ";" +
        weldReadOuters("x") + " instanceof ___.primFreeze(" + weldReadOuters("foo") + ");");
  }

  public void testOtherTypeof() throws Exception {
    checkSucceeds(
        "typeof x;",
        "typeof " + weldReadOuters("x") + ";");
  }

  public void testOtherBadInstanceof() throws Exception {
    checkFails(
        "var x = 3; y instanceof x;",
        "Invoked \"instanceof\" on non-function");
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
        "       var x___ = Number(___.readPub(___OUTERS___, 'k', true));" +
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
    checkSucceeds(readResource("clickme.js"));
  }

  public void testSpecimenListfriends() throws Exception {
    checkSucceeds(readResource("listfriends.js"));
  }

  public void testFuncCtor() throws Exception {
    checkSucceeds(
        "function Foo(x) { this.x_ = x; }",
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
        "  })();");
    checkSucceeds(
        "(function(){ function Foo(x) { this.x_ = x; } })()",
        "___.asSimpleFunc(___.primFreeze(___.simpleFunc(function () {" +
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
        "  })))();");
    checkSucceeds(
        "function Foo(x) { this.x_ = x; }" +
        "function Bar(y) {" +
        "  Foo.call(this,1);" +
        "  this.y = y;" +
        "}" +
        "bar = new Bar(3);",
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
        "(function () {" +
        "    var x___ = new (___.asCtor(___OUTERS___.Bar_canRead___ ? ___OUTERS___.Bar : ___.readPub(___OUTERS___, 'Bar', true)))(3);" +
        "    return ___OUTERS___.bar_canSet___ ? (___OUTERS___.bar = x___) : ___.setPub(___OUTERS___, 'bar', x___);" +
        "  })();");
  }

  private void setSynthetic(ParseTreeNode n) {
    n.getAttributes().set(SyntheticNodes.SYNTHETIC, true);
  }

  private void setTreeSynthetic(ParseTreeNode n) {
    setSynthetic(n);
    for (ParseTreeNode child : n.children()) {
      setTreeSynthetic(child);
    }
  }

  private void checkFails(String input, String error) throws Exception {
    MessageContext mc = new MessageContext();
    MessageQueue mq = TestUtil.createTestMessageQueue(mc);
    new DefaultCajaRewriter(true).expand(TestUtil.parse(input), mq);

    assertFalse(mq.getMessages().isEmpty());

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
    MessageQueue mq = TestUtil.createTestMessageQueue(new MessageContext());
    ParseTreeNode actualResultNode = new DefaultCajaRewriter().expand(inputNode, mq);
    for (Message m : mq.getMessages()) {
      if (m.getMessageLevel().compareTo(MessageLevel.WARNING) >= 0) {
        fail(m.toString());
      }
    }
    if (expectedResultNode != null) {
      // Test that the source code-like renderings are identical. This will catch any
      // obvious differences between expected and actual.
      assertEquals(
          TestUtil.render(expectedResultNode),
          TestUtil.render(actualResultNode));
      // Then, for good measure, test that the S-expression-like formatted representations
      // are also identical. This will catch any differences in tree topology that somehow
      // do not appear in the source code representation (usually due to programming errors).
      assertEquals(
          TestUtil.format(expectedResultNode),
          TestUtil.format(actualResultNode));
    }
  }

  private void checkSucceedsUnchanged(String input) throws Exception {
    checkSucceeds(input, input);
  }

  private void checkSucceeds(String input, String expectedResult) throws Exception {
    checkSucceeds(TestUtil.parse(input), TestUtil.parse(expectedResult));
  }

  private void checkSucceeds(String input) throws Exception {
    checkSucceeds(TestUtil.parse(input), null);
  }

  private String readResource(String resource) throws Exception {
    return TestUtil.readResource(getClass(), resource);    
  }
}
