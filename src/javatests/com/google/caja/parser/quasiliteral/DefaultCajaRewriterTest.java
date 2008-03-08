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
        "{ ___OUTERS___.foo = ___.simpleFunc(function foo() {}); }");
    checkSucceeds(input, expectedResult);
  }

  ////////////////////////////////////////////////////////////////////////
  // Handling of nested blocks
  ////////////////////////////////////////////////////////////////////////

  public void testNestedBlockWithFunction() throws Exception {
    checkSucceeds(
        "{ function foo() {} }",
        "{ ___OUTERS___.foo = ___.simpleFunc(function foo() {}); }");
  }

  public void testNestedBlockWithVariable() throws Exception {
    checkSucceeds(
        "{ var x = y; }",
        "{ ___OUTERS___.x = ___OUTERS___.y; }");
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
        "for (var k in x) { k; }",
        "{" +
        "  ___OUTERS___.x0___ = ___OUTERS___.x;" +
        "  ___OUTERS___.x1___ = undefined;" +
        "  ___OUTERS___.k;" +
        "  for (___OUTERS___.x1 in ___OUTERS___.x0___) {" +
        "    if (___.canEnumPub(___OUTERS___.x0___, ___OUTERS___.x1___)) {" +
        "      ___OUTERS___.k = ___OUTERS___.x1___;" +
        "      ___OUTERS___.k;" +
        "    }" +
        "  }" +
        "}");
    }
    if (false) {
    // TODO(ihab.awad): Enable when http://code.google.com/p/google-caja/issues/detail?id=68 fixed
    checkSucceeds(
        "try { } catch (e) { for (var k in x) { k; } }",
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    {" +
        "      ___OUTERS___.x0___ = ___OUTERS___.x;" +
        "      ___OUTERS___.x1___ = undefined;" +
        "      ___OUTERS___.k;" +
        "      for (___OUTERS___.x1 in ___OUTERS___.x0___) {" +
        "        if (___.canEnumPub(___OUTERS___.x0___, ___OUTERS___.x1___)) {" +
        "          ___OUTERS___.k = ___OUTERS___.x1___;" +
        "          ___OUTERS___.k;" +
        "        }" +
        "      }" +
        "    }" +
        "  }" +
        "}");
    }
    checkSucceeds(
        "function() {" +
        "  for (var k in x) { k; }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  {" +
        "    var x0___ = ___OUTERS___.x;" +
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
        "for (k in x) { k; }",
        "{" +
        "  ___OUTERS___.x0___ = ___OUTERS___.x;" +
        "  ___OUTERS___.x1___ = undefined;" +
        "  for (___OUTERS___.x1 in ___OUTERS___.x0___) {" +
        "    if (___.canEnumPub(___OUTERS___.x0___, ___OUTERS___.x1___)) {" +
        "      ___OUTERS___.k = ___OUTERS___.x1___;" +
        "      ___OUTERS___.k;" +
        "    }" +
        "  }" +
        "}");
    }
    checkSucceeds(
        "function() {" +
        "  for (k in x) { k; }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  {" +
        "    var x0___ = ___OUTERS___.x;" +
        "    var x1___ = undefined;" +
        "    for (x1___ in x0___) {" +
        "      if (___.canEnumPub(x0___, x1___)) {" +
        "        ___OUTERS___.k = x1___;" +
        "        ___OUTERS___.k;" +
        "      }" +
        "    }" +
        "  }" +
        "}));");
    checkSucceeds(
        "function() {" +
        "  var k;" +
        "  for (k in x) { k; }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var k;" +
        "  {" +
        "    var x0___ = ___OUTERS___.x;" +
        "    var x1___ = undefined;" +
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
        "for (y.k in x) { y.k; }",
        "{" +
        "  ___OUTERS___.x0___ = ___OUTERS___.x;" +
        "  ___OUTERS___.x1___ = undefined;" +
        "  for (___OUTERS___.x1 in ___OUTERS___.x0___) {" +
        "    if (___.canEnumPub(___OUTERS___.x0___, ___OUTERS___.x1___)) {" +
        "      ___OUTERS___.y.k = ___OUTERS___.x1___;" +
        "      ___OUTERS___.y.k;" +
        "    }" +
        "  }" +
        "}");
    }
    if (false) {
    // TODO(ihab.awad): Enable when http://code.google.com/p/google-caja/issues/detail?id=68 fixed
    checkSucceeds(
        "function() {" +
        "  for (y.k in x) { y.k; }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  {" +
        "    var x0___ = ___OUTERS___.x;" +
        "    var x1___ = undefined;" +
        "    for (x1___ in x0___) {" +
        "      if (___.canEnumPub(x0___, x1___)) {" +
        "        ___OUTERS___.y.k = x1___;" +
        "        ___OUTERS___.y.k;" +
        "      }" +
        "    }" +
        "  }" +
        "}));");
    }
    checkSucceeds(
        "function foo() {" +
        "  for (var k in this) { k; }" +
        "}",
        "___OUTERS___.foo = ___.ctor(function foo() {" +
        "  var t___ = this;" +
        "  {" +
        "    var x0___ = t___;" +
        "    var x1___ = undefined;" +
        "    var k;" +
        "    for (x1___ in x0___) {" +
        "      if (___.canEnumProp(x0___, x1___)) {" +
        "        k = x1___;" +
        "        k;" +
        "      }" +
        "    }" +
        "  }" +
        "});");
    checkSucceeds(
        "for (var k in this) { k; }",
        "{" +
        "  ___OUTERS___.x0___ = ___OUTERS___;" +
        "  ___OUTERS___.x1___ = undefined;" +
        "  ___OUTERS___.k;" +
        "  for (x1___ in x0___) {" +
        "    if (___.canEnumProp(x0___, x1___)) {" +
        "      ___OUTERS___.k = x1___;" +
        "      ___OUTERS___.k;" +
        "    }" +
        "  }" +
        "}");
    checkSucceeds(
        "function foo() {" +
        "  for (k in this) { k; }" +
        "}",
        "___OUTERS___.foo = ___.ctor(function foo() {" +
        "  var t___ = this;" +
        "  {" +
        "    var x0___ = t___;" +
        "    var x1___ = undefined;" +
        "    for (x1___ in x0___) {" +
        "      if (___.canEnumProp(x0___, x1___)) {" +
        "        ___OUTERS___.k = x1___;" +
        "        ___OUTERS___.k;" +
        "      }" +
        "    }" +
        "  }" +
        "});");
    checkSucceeds(
        "function foo() {" +
        "  var k;" +
        "  for (k in this) { k; }" +
        "}",
        "___OUTERS___.foo = ___.ctor(function foo() {" +
        "  var t___ = this;" +
        "  var k;" +
        "  {" +
        "    var x0___ = t___;" +
        "    var x1___ = undefined;" +
        "    for (x1___ in x0___) {" +
        "      if (___.canEnumProp(x0___, x1___)) {" +
        "        k = x1___;" +
        "        k;" +
        "      }" +
        "    }" +
        "  }" +
        "});");
    if (false) {
    // TODO(ihab.awad): Enable when http://code.google.com/p/google-caja/issues/detail?id=68 fixed
    checkSucceeds(
        "function foo() {" +
        "  for (y.k in this) { y.k; }" +
        "}",
        "___OUTERS___.foo = ___.ctor(function foo() {" +
        "  var t___ = this;" +
        "  {" +
        "    var x0___ = t___;" +
        "    var x1___ = undefined;" +
        "    for (x1___ in x0___) {" +
        "      if (___.canEnumProp(x0___, x1___)) {" +
        "        ___OUTERS___.y.k = x1___;" +
        "        ___OUTERS___.y.k;" +
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
        "  ___OUTERS___.e;" +
        "  ___OUTERS___.x;" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    e;" +
        "    ___OUTERS___.y;" +
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
        "  ___OUTERS___.e;" +
        "  ___OUTERS___.x;" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    e;" +
        "    ___OUTERS___.y;" +
        "  }" +
        "} finally {" +
        "  ___OUTERS___.e;" +
        "  ___OUTERS___.z;" +
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
        "  ___OUTERS___.x;" +
        "} finally {" +
        "  ___OUTERS___.z;" +
        "}");
  }

  public void testVarArgs() throws Exception {
    checkSucceeds(
        "var foo = function() {" +
        "  p = arguments;" +
        "};",
        "___OUTERS___.foo = ___.primFreeze(___.simpleFunc(function() {" +
        "  var a___ = ___.args(arguments);" +
        "  ___OUTERS___.p = a___;" +
        "}));");
  }

  public void testVarThis() throws Exception {
    checkSucceeds(
        "function foo() {" +
        "  p = this;" +
        "}",
        "___OUTERS___.foo = ___.ctor(function foo() {" +
        "  var t___ = this;" +
        "  ___OUTERS___.p = t___;" +
        "});");
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

  public void testVarBadCtorLeak() throws Exception {
    checkFails(
        "function Ctor() { this.x = 1; }; var c = Ctor;",
        "Constructors are not first class");
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
        "___OUTERS___.foo = ___.simpleFunc(function foo() {});" +
        "___OUTERS___.f = ___.primFreeze(___OUTERS___.foo);");
  }

  public void testVarGlobal() throws Exception {
    checkSucceeds(
        "foo;",
        "___OUTERS___.foo;");
    checkSucceeds(
        "function() {" +
        "  foo;" +
        "}",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  ___OUTERS___.foo;" +
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
        unchanged +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        unchanged +
        "}));");
  }

  public void testReadBadSuffix() throws Exception {
    checkFails(
        "x.y__;",
        "Properties cannot end in \"__\"");
  }

  public void testReadInternal() throws Exception {
    checkSucceeds(
        "function() {" +
        "  function foo() {" +
        "    p = this.x;" +
        "  }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo = ___.ctor(function foo() {" +
        "    var t___ = this;" +
        "    ___OUTERS___.p = t___.x_canRead___ ? t___.x : ___.readProp(t___, 'x');" +
        "  });" +
        "}));");
    checkSucceeds(
        "this.x;",
        "___OUTERS___.x_canRead___ ? ___OUTERS___.x : ___.readProp(___OUTERS___, 'x');");
    checkSucceeds(
        "try { } catch (e) { this.x; }",
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    ___OUTERS___.x_canRead___ ? ___OUTERS___.x : ___.readProp(___OUTERS___, 'x');" +
        "  }" +
        "}");
  }

  public void testReadBadInternal() throws Exception {
    checkFails(
        "foo.bar_;",
        "Public properties cannot end in \"_\"");
  }

  public void testReadPublic() throws Exception {
    checkSucceeds(
        "p = foo.p;",
        "___OUTERS___.p = (function() {" +
        "  var x___ = ___OUTERS___.foo;" +
        "  return x___.p_canRead___ ? x___.p : ___.readPub(x___, 'p');" +
        "})();");
  }

  public void testReadIndexInternal() throws Exception {
    checkSucceeds(
        "function foo() { p = this[3]; }",
        "___OUTERS___.foo = ___.ctor(" +
        "  function foo() {" +
        "    var t___ = this;" +
        "    ___OUTERS___.p = ___.readProp(t___, 3);" +
        "  }" +
        ");");
    checkSucceeds(
        "this[3];",
        "___.readProp(___OUTERS___, 3);");
    checkSucceeds(
        "try { } catch (e) { this[3]; }",
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    ___.readProp(___OUTERS___, 3);" +
        "  }" +
        "}");
  }
  
  public void testReadIndexPublic() throws Exception {
    checkSucceeds(
        "function() { var foo; p = foo[3]; };",
        "___.primFreeze(___.simpleFunc(" +
        "  function() {" +
        "    var foo;" +
        "    ___OUTERS___.p = ___.readPub(foo, 3);" +
        "}));");
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

  public void testSetInternal() throws Exception {
    checkSucceeds(
        "function() {" +
        "  function foo() { this.p = x; }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo = ___.ctor(function foo() {" +
        "    var t___ = this;" +
        "    (function() {" +
        "      var x___ = ___OUTERS___.x;" +
        "      return t___.p_canSet___ ? (t___.p = x___) : ___.setProp(t___, 'p', x___);" +
        "    })();" +
        "  });" +
        "}));");
    checkSucceeds(
        "this.p = x;",
        "(function() {" +
        "  var x___ = ___OUTERS___.x;" +
        "  return ___OUTERS___.p_canSet___ ?" +
        "      (___OUTERS___.p = x___) :" +
        "      ___.setProp(___OUTERS___, 'p', x___);" +
        "})();");
    checkSucceeds(
        "try { } catch (e) { this.p = x; }",
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "      (function() {" +
        "        var x___ = ___OUTERS___.x;" +
        "        return ___OUTERS___.p_canSet___ ?" +
        "            (___OUTERS___.p = x___) :" +
        "            ___.setProp(___OUTERS___, 'p', x___);" +
        "      })();" +
        "  }" +
        "}");
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
        "    var x___ = ___OUTERS___.x;" +
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

  public void testSetMemberMap() throws Exception {    
    checkFails(
        "function foo() {}" +
        "foo.prototype = x;",  
        "Map expression expected");
    checkFails(
        "function foo() {}" +
        "foo.prototype = function() {};",
        "Map expression expected");
    checkSucceeds(
        "function foo() {}" +
        "foo.prototype = { k0: v0, k1: function() { this.p = 3; } };",
        "___OUTERS___.foo = ___.simpleFunc(function foo() {});" +
        "___.setMemberMap(" +
        "  ___.primFreeze(___OUTERS___.foo), {" +
        "    k0: ___OUTERS___.v0," +
        "    k1: ___.method(foo, function() { " +
        "      var t___ = this;" +
        "      (function() {" +
        "        var x___ = 3;" +
        "        return t___.p_canSet___ ? (t___.p = x___) : ___.setProp(t___, 'p', x___);" +
        "      })();" +
        "    })" +
        "});");
  }

  public void testSetStatic() throws Exception {
    checkSucceeds(
        "function() {" +
        "  function foo() {}" +
        "  foo.p = x;" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo = ___.simpleFunc(function foo() {});" +
        "  ___.setPub(foo, 'p', ___OUTERS___.x);" +
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
        "    var x0___ = ___OUTERS___.y;" +
        "    return x___.p_canSet___ ? (x___.p = x0___) : ___.setPub(x___, 'p', x0___);" +
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
        "  var foo = ___.ctor(function foo() {" +
        "    var t___ = this;" +
        "    ___.setProp(t___, ___OUTERS___.x, ___OUTERS___.y);" +
        "  });" +
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
        "  ___.setPub(o, ___OUTERS___.x, ___OUTERS___.y);" +
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
        "  var v = ___OUTERS___.x;" +
        "}));");
    checkSucceeds(
        "var v = x",
        "___OUTERS___.v = ___OUTERS___.x");
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
        "___OUTERS___.v;");
    checkSucceeds(
        "try { } catch (e) { var v; }",
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    ___OUTERS___.v;" +
        "  }" +
        "}");
  }

  public void testNewCalllessCtor() throws Exception {
    checkSucceeds(
        "function foo() { this.p = 3; }" +
        "new foo;",
        "___OUTERS___.foo = ___.ctor(function foo() {" +
        "  var t___ = this;" +
        "  (function() {" +
        "    var x___ = 3;" +
        "    return t___.p_canSet___ ? (t___.p = x___) : ___.setProp(t___, 'p', x___);" +
        "  })();" +
        "});" +
        "new (___.asCtor(___OUTERS___.foo))();");
    checkSucceeds(
        "function foo() {}" +
        "new foo;",
        "___OUTERS___.foo = ___.simpleFunc(function foo() {});" +
        "new (___.asCtor(___OUTERS___.foo))();");
  }

  public void testNewCtor() throws Exception {
    checkSucceeds(
        "function foo() { this.p = 3; }" +
        "new foo(x, y);",
        "___OUTERS___.foo = ___.ctor(function foo() {" +
        "  var t___ = this;" +
        "  (function() {" +
        "    var x___ = 3;" +
        "    return t___.p_canSet___ ? (t___.p = x___) : ___.setProp(t___, 'p', x___);" +
        "  })();" +
        "});" +
        "new (___.asCtor(___OUTERS___.foo))(___OUTERS___.x, ___OUTERS___.y);");
    checkSucceeds(
        "function foo() {}" +
        "new foo(x, y);",
        "___OUTERS___.foo = ___.simpleFunc(function foo() {});" +
        "new (___.asCtor(___OUTERS___.foo))(___OUTERS___.x, ___OUTERS___.y);");
    checkSucceeds(
        "function foo() {}" +
        "new foo();",
        "___OUTERS___.foo = ___.simpleFunc(function foo() {});" +
        "new (___.asCtor(___OUTERS___.foo))();");
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
        "  new (___.asCtor(___OUTERS___.x))(___OUTERS___.y, ___OUTERS___.z);" +
        "}));");
  }

  public void testCallBadSuffix() throws Exception {
    checkFails(
        "x.p__(3, 4);",
        "Selectors cannot end in \"__\"");
  }
  
  public void testCallInternal() throws Exception {
    checkSucceeds(
        "function() {" +
        "  function foo() {" +
        "    this.f(x, y);" +
        "  }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo = ___.ctor(function foo() {" +
        "    var t___ = this;" +
        "    (function() {" +
        "      var x0___ = ___OUTERS___.x;" +
        "      var x1___ = ___OUTERS___.y;" +
        "      return t___.f_canCall___ ?" +
        "          t___.f(x0___, x1___) :" +
        "          ___.callProp(t___, 'f', [x0___, x1___]);" +
        "    })();" +
        "  });" +
        "}));");
    checkSucceeds(
        "this.f()",
        "(function() {" +
        "  return ___OUTERS___.f_canCall___ ?" +
        "      ___OUTERS___.f() :" +
        "      ___.callProp(___OUTERS___, 'f', []);" +
        "})()");
    checkSucceeds(
        "try { } catch (e) { this.f(); }",
        "try {" +
        "} catch (ex___) {" +
        "  try {" +
        "    throw ___.tameException(ex___);" +
        "  } catch (e) {" +
        "    (function() {" +
        "      return ___OUTERS___.f_canCall___ ?" +
        "          ___OUTERS___.f() :" +
        "          ___.callProp(___OUTERS___, 'f', []);" +
        "    })()" +
        "  }" +
        "}");
  }

  public void testCallBadInternal() throws Exception {
    checkFails(
        "o.p_();",
        "Public selectors cannot end in \"_\"");
  }

  public void testCallCajaDef2() throws Exception {
    checkSucceeds(
        "function() {" +
        "  function Point() {}" +
        "  caja.def(Point, Object);" +
        "  function WigglyPoint() {}" +
        "  caja.def(WigglyPoint, Point);" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var Point = ___.simpleFunc(function Point() {});" +
        "  caja.def(Point, Object);" +
        "  var WigglyPoint = ___.simpleFunc(function WigglyPoint() {});" +
        "  caja.def(WigglyPoint, Point);" +
        "}));");
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
        "function() {" +
        "  function Point() {}" +
        "  function WigglyPoint() {}" +
        "  caja.def(WigglyPoint, Point, { m0: x, m1: function() { this.p = 3; } });" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var Point = ___.simpleFunc(function Point() {});" +
        "  var WigglyPoint = ___.simpleFunc(function WigglyPoint() {});" +
        "  caja.def(WigglyPoint, Point, {" +
        "      m0: ___OUTERS___.x," +
        "      m1: ___.method(WigglyPoint, function() {" +
        "        var t___ = this;" +
        "        (function() {" +
        "          var x___ = 3;" +
        "          return t___.p_canSet___ ? (t___.p = x___) : ___.setProp(t___, 'p', x___);" +
        "        })();" +            
        "      })" +
        "  });" +
        "}));");
    checkSucceeds(
        "function() {" +
        "  function Point() {}" +
        "  function WigglyPoint() {}" +
        "  caja.def(WigglyPoint, Point," +
        "      { m0: x, m1: function() { this.p = 3; } }," +
        "      { s0: y, s1: function() { return 3; } });" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var Point = ___.simpleFunc(function Point() {});" +
        "  var WigglyPoint = ___.simpleFunc(function WigglyPoint() {});" +
        "  caja.def(WigglyPoint, Point, {" +
        "      m0: ___OUTERS___.x," +
        "      m1: ___.method(WigglyPoint, function() {" +
        "        var t___ = this;" +
        "        (function() {" +
        "          var x___ = 3;" +
        "          return t___.p_canSet___ ? (t___.p = x___) : ___.setProp(t___, 'p', x___);" +
        "        })();" +
        "      })" +
        "  }, {" +
        "      s0: ___OUTERS___.y," +
        "      s1: ___.primFreeze(___.simpleFunc(function() { return 3; }))" +
        "  });" +            
        "}));");
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
        "Method in non-method context");
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
        "  var x___ = ___OUTERS___.o;" +
        "  var x0___ = ___OUTERS___.x;" +
        "  var x1___ = ___OUTERS___.y;" +
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
        "  var foo = ___.ctor(function foo() {" +
        "    var t___ = this;" +
        "    ___.callProp(t___, ___OUTERS___.x, [___OUTERS___.y, ___OUTERS___.z]);" +
        "  });" +
        "}));");
  }

  public void testCallIndexPublic() throws Exception {
    checkSucceeds(
        "x[y](z, t);",
        "___.callPub(___OUTERS___.x, ___OUTERS___.y, [___OUTERS___.z, ___OUTERS___.t]);");
  }

  public void testCallFunc() throws Exception {
    checkSucceeds(
        "f(x, y);",
        "___.asSimpleFunc(___OUTERS___.f)(___OUTERS___.x, ___OUTERS___.y);");
  }

  public void testFuncAnonSimple() throws Exception {
    checkSucceeds(
        "function(x, y) { x = arguments; y = z; };",
        "___.primFreeze(___.simpleFunc(function(x, y) {" +
        "  var a___ = ___.args(arguments);" +
        "  x = a___;" +
        "  y = ___OUTERS___.z;" +
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
        "      y = ___OUTERS___.z;" +
        "      return ___.asSimpleFunc(___.primFreeze(foo))(x - 1, y - 1);" +
        "  });"+
        "}));");
    checkSucceeds(
        "function foo(x, y ) {" +
        "  return foo(x - 1, y - 1);" +
        "}",
        "___OUTERS___.foo = ___.simpleFunc(function foo(x, y) {" +
        "  return ___.asSimpleFunc(___.primFreeze(foo))(x - 1, y - 1);" +
        "});");
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
        "      y = ___OUTERS___.z;" +
        "      return ___.asSimpleFunc(___.primFreeze(foo))(x - 1, y - 1);" +            
        "  }));"+
        "}));");
    checkSucceeds(
        "var foo = function foo(x, y ) {" +
        "  return foo(x - 1, y - 1);" +
        "}",
        "___OUTERS___.foo = ___.primFreeze(___.simpleFunc(function foo(x, y) {" +
        "  return ___.asSimpleFunc(___.primFreeze(foo))(x - 1, y - 1);" +
        "}));");
  }

  public void testFuncBadMethod() throws Exception {
    checkFails(
        "function(x) { x = this; };",
        "Method in non-method context");
  }

  public void testFuncBadCtor() throws Exception {
    checkFails(
        "var f = function foo(x) { x = this; };",
        "Constructor cannot escape");
  }

  public void testFuncDerivedCtorDecl() throws Exception {
    checkSucceeds(
        "function() {" +
        "  function foo(x, y) {" +
        "    foo.Super.call(this, x + y);" +
        "    y = z;" +
        "  }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo = ___.ctor(function foo(x, y) {" +
        "    var t___ = this;" +
        "    foo.Super.call(t___, x + y);" +
        "    y = ___OUTERS___.z;" +
        "  });" +
        "}));");
  }

  public void testFuncCtorDecl() throws Exception {
    checkSucceeds(
        "function() {" +
        "  function foo(x, y) {" +
        "    x = this;" +
        "    y = z;" +
        "  }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo = ___.ctor(function foo(x, y) {" +
        "    var t___ = this;" +
        "    x = t___;" +
        "    y = ___OUTERS___.z;" +            
        "  });" +
        "}));");
    checkSucceeds(
        "function() {" +
        "  function foo() {" +
        "    var self = this;" +
        "    return function() { return self; };" +
        "  }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo = ___.ctor(function foo() {" +
        "    var t___ = this;" +
        "    var self = t___;" +
        "    return ___.primFreeze(___.simpleFunc(function() {" +
        "      return self;" +            
        "    }));" +
        "  });" +
        "}));");


  }

  public void testMapEmpty() throws Exception {
    checkSucceeds(
        "f = {};",
        "___OUTERS___.f = {};");
  }

  public void testMapBadKeySuffix() throws Exception {
    checkFails(
        "var o = { x_: 3 };",
        "Key may not end in \"_\"");
  }

  public void testMapNonEmpty() throws Exception {
    checkSucceeds(
        "var o = { k0: x, k1: y };",
        "___OUTERS___.o = { k0: ___OUTERS___.x, k1: ___OUTERS___.y };");
  }

  public void testOtherInstanceof() throws Exception {
    checkSucceeds(
        "function foo() {}" +
        "x instanceof foo;",      
        "___OUTERS___.foo = ___.simpleFunc(function foo() {});" +
        "___OUTERS___.x instanceof ___.primFreeze(___OUTERS___.foo);");
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
        "___OUTERS___.x, ___OUTERS___.y;");
    checkSucceeds(
        "var x = foo, y = bar;",
        "___OUTERS___.x = ___OUTERS___.foo, ___OUTERS___.y = ___OUTERS___.bar;");
    checkSucceeds(
        "var x, y = bar;",
        "___OUTERS___.x, ___OUTERS___.y = ___OUTERS___.bar;");
    // 'var' in global scope, 'for' statement
    checkSucceeds(
        "for (var x, y; ; ) {}",
        "for (___OUTERS___.x, ___OUTERS___.y; ; ) {}");
    checkSucceeds(
        "for (var x = foo, y = bar; ; ) {}",
        "for (___OUTERS___.x = ___OUTERS___.foo, ___OUTERS___.y = ___OUTERS___.bar; ; ) {}");
    checkSucceeds(
        "for (var x, y = bar; ; ) {}",
        "for (___OUTERS___.x, ___OUTERS___.y = ___OUTERS___.bar; ; ) {}");
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
        "  var x = ___OUTERS___.foo, y = ___OUTERS___.bar;" +
        "}));");
    checkSucceeds(
        "function() {" +
        "  var x, y = bar;" +
        "}",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var x, y = ___OUTERS___.bar;" +
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
        "  for (var x = ___OUTERS___.foo, y = ___OUTERS___.bar; ; ) {}" +
        "}));");
    checkSucceeds(
        "function() {" +
        "  for (var x, y = bar; ; ) {}" +
        "}",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  for (var x, y = ___OUTERS___.bar; ; ) {}" +
        "}));");
  }

  public void testRecurseParseTreeNodeContainer() throws Exception {
    // Tested implicitly by other cases
  }

  public void testRecurseArrayConstructor() throws Exception {
    checkSucceeds(
        "foo = [ bar, baz ];",
        "___OUTERS___.foo = [ ___OUTERS___.bar, ___OUTERS___.baz ];");
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
        "switch (___OUTERS___.x) { case 1: break; }");
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
        "if (___OUTERS___.x === ___OUTERS___.y) {" +
        "  ___OUTERS___.z;" +
        "} else if (___OUTERS___.z === ___OUTERS___.y) {" +
        "  ___OUTERS___.x;" +
        "} else {" +
        "  ___OUTERS___.y;" +
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
        "switch(___OUTERS___.x) { default: break; }");
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
        "for (___OUTERS___.k = 0; ___OUTERS___.k < 3; ___OUTERS___.k++) {" +
        "  ___OUTERS___.x;" +
        "}");
    checkSucceeds(
        "function() {" +
        "  for (var k = 0; k < 3; k++) {" +
        "    x;" +
        "  }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  for (var k = 0; k < 3; k++) {" +
        "    ___OUTERS___.x;" +
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
        "___OUTERS___.x + ___OUTERS___.y");
    checkSucceeds(
        "1 + 2 * 3 / 4 - -5",
        "1 + 2 * 3 / 4 - -5");
    checkSucceeds(
        "x  = y = 3;",
        "___OUTERS___.x = ___OUTERS___.y = 3;");
  }

  public void testRecurseReturnStmt() throws Exception {
    checkSucceeds(
        "return x;",
        "return ___OUTERS___.x;");
  }

  public void testRecurseSwitchStmt() throws Exception {
    checkSucceeds(
        "switch (x) { }",
        "switch (___OUTERS___.x) { }");
  }
  
  public void testRecurseThrowStmt() throws Exception {
    checkSucceeds(
        "throw x;",
        "throw ___OUTERS___.x;");
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
