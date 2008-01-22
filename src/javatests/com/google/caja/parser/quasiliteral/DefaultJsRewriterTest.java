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
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.Block;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.TestUtil;
import com.google.caja.plugin.SyntheticNodes;
import junit.framework.TestCase;

import java.io.StringReader;
import java.net.URI;
import java.util.Collections;

/**
 * @author ihab.awad@gmail.com
 */
public class DefaultJsRewriterTest extends TestCase {
  ////////////////////////////////////////////////////////////////////////
  // Handling of synthetic nodes
  ////////////////////////////////////////////////////////////////////////

  public void testSyntheticIsUntouched() throws Exception {
    ParseTreeNode input = parseText(
        "function foo() { this; arguments; }");
    setTreeSynthetic(input);
    checkSucceeds(input, input);
  }

  public void testNestedInsideSyntheticIsExpanded() throws Exception {
    ParseTreeNode innerInput = parseText("function foo() {}");
    ParseTreeNode input = ParseTreeNodes.newNodeInstance(
        Block.class,
        null,
        Collections.singletonList(innerInput));
    setSynthetic(input);
    ParseTreeNode expectedResult = parseText(
        "{ ___OUTERS___.foo = ___.simpleFunc(function () {}); }");
    checkSucceeds(input, expectedResult);
  }

  ////////////////////////////////////////////////////////////////////////
  // Handling of nested blocks
  ////////////////////////////////////////////////////////////////////////

  public void testNestedBlockWithFunction() throws Exception {
    checkSucceeds(
        "{ function foo() {} }",
        "{ ___OUTERS___.foo = ___.simpleFunc(function() {}); }");
  }

  public void testNestedBlockWithVariable() throws Exception {
    checkSucceeds(
        "{ var x = y; }",
        "{ ___OUTERS___.x = ___OUTERS___.y; }");
  }

  ////////////////////////////////////////////////////////////////////////
  // Miscellaneous stuff
  ////////////////////////////////////////////////////////////////////////

  public void testVarUnderscore() throws Exception {
  }

  ////////////////////////////////////////////////////////////////////////
  // Specific rules
  ////////////////////////////////////////////////////////////////////////

  public void testWith() throws Exception {
    // Our parser does not recognize "with" at all.
  }

  /**
   * TODO(ihab.awad): Implement these properly
   *

  public void testTryCatch() throws Exception {
    checkSucceeds(
        "try {" +
        "  x;" +
        "} catch (e) {" +
        "  e;" +
        "  y;" +
        "}",
        "try {" +
        "  ___OUTERS___.x;" +
        "} catch (e) {" +
        "  e;" +
        "  ___OUTERS___.y;" +
        "}");
  }

  public void testTryCatchFinally() throws Exception {
    checkSucceeds(
        "try {" +
        "  x;" +
        "} catch (e) {" +
        "  e;" +
        "  y;" +
        "} finally {" +
        "  z;" +
        "}",
        "try {" +
        "  ___OUTERS___.x;" +
        "} catch (e) {" +
        "  e;" +
        "  ___OUTERS___.y;" +
        "} finally {" +
        "  ___OUTERS___.z;" +
        "}");
  }

  public void testTryFinally() throws Exception {
  }

  */

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
        "___OUTERS___.foo = ___.ctor(function() {" +
        "  var t___ = this;" +
        "  ___OUTERS___.p = t___;" +
        "});");
    checkSucceeds(
        "this;",
        "___OUTERS___;");
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
        "  var foo = ___.simpleFunc(function() {});" +
        "  var f = ___.primFreeze(foo);" +
        "}));");
  }

  public void testVarGlobal() throws Exception {
    checkSucceeds(
        "foo;",
        "___OUTERS___.foo;");
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
        "  var foo = ___.ctor(function() {" +
        "    var t___ = this;" +
        "    ___OUTERS___.p = t___.x_canRead___ ? t___.x : ___.readProp(t___, 'x');" +
        "  });" +
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
        "___OUTERS___.p = (function() {" +
        "  var x___ = ___OUTERS___.foo;" +
        "  return x___.p_canRead___ ? x___.p : ___.readPub(x___, 'p');" +
        "})();");
  }

  public void testReadIndexInternal() throws Exception {
    checkSucceeds(
        "function foo() { p = this[3]; }",
        "___OUTERS___.foo = ___.ctor(" +
        "  function() {" +
        "    var t___ = this;" +
        "    ___OUTERS___.p = ___.readProp(t___, 3);" +
        "  }" +
        ");");
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
        "  var foo = ___.ctor(function() {" +
        "    var t___ = this;" +
        "    (function() {" +
        "      var x___ = ___OUTERS___.x;" +
        "      t___.p_canSet___ ? (t___.p = x___) : ___.setProp(t___, 'p', x___);" +
        "    })();" +
        "  });" +
        "}));");
  }

  public void testSetMember() throws Exception {
    checkSucceeds(
        "function() {" +
        "  function foo() {}" +
        "  foo.prototype.p = x;" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo = ___.simpleFunc(function() {});" +
        "  (function() {" +
        "    var x___ = ___OUTERS___.x;" +
        "    ___.setMember(foo, 'p', x___);" +
        "  })();" +
        "}));");
    checkSucceeds(
        "function() {" +
        "  function foo() {}" +
        "  foo.prototype.p = function(a, b) { this; }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +            
        "  var foo = ___.simpleFunc(function() {});" +
        "  (function() {" +
        "    var x___ = ___.method(foo, function(a, b) {" +
        "      var t___ = this;" +
        "      t___;" +
        "    });" +
        "    ___.setMember(foo, 'p', x___);" +
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
        "___OUTERS___.foo = ___.simpleFunc(function() {});" +
        "___.setMemberMap(" +
        "  ___.primFreeze(___OUTERS___.foo), {" +
        "    k0: ___OUTERS___.v0," +
        "    k1: ___.method(foo, function() { " +
        "      var t___ = this;" +
        "      (function() {" +
        "        var x___ = 3;" +
        "        t___.p_canSet___ ? (t___.p = x___) : ___.setProp(t___, 'p', x___);" +
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
        "  var foo = ___.simpleFunc(function() {});" +
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
        "    x___.p_canSet___ ? (x___.p = x0___) : ___.setPub(x___, 'p', x0___);" +
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
        "  var foo = ___.ctor(function() {" +
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

  public  void testSetInitialize() throws Exception {
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
  
  public  void testSetDeclare() throws Exception {
    checkSucceeds(
        "function() {" +
        "  var v;" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var v;" +
        "}));");
  }

  public void testNewCtor() throws Exception {
    checkSucceeds(
        "function() {" +
        "  function foo() {}" +
        "  new foo(x, y);" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo = ___.simpleFunc(function() {});" +
        "  new (___.asCtor(foo))(___OUTERS___.x, ___OUTERS___.y);" +
        "}));");
  }

  public void testNewFunc() throws Exception {
    checkSucceeds(
        "function() {" +
        "  var foo = undefined;" +
        "  new x(y, z);" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo = undefined;" +
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
        "  var foo = ___.ctor(function() {" +
        "    var t___ = this;" +
        "    (function() {" +
        "      var x0___ = ___OUTERS___.x;" +
        "      var x1___ = ___OUTERS___.y;" +
        "      return t___.f_canCall___ ?" +
        "          this.f(x0___, x1___) :" +
        "          ___.callProp(t___, 'f', [x0___, x1___]);" +
        "    })();" +
        "  });" +
        "}));");
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
        "  function WigglyPoint() {}" +
        "  caja.def(WigglyPoint, Point);" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var Point = ___.simpleFunc(function() {});" +
        "  var WigglyPoint = ___.simpleFunc(function() {});" +
        "  caja.def(WigglyPoint, Point);" +
        "}));");
  }

  public void testCallCajaDef3Plus() throws Exception {
    checkSucceeds(
        "function() {" +
        "  function Point() {}" +
        "  function WigglyPoint() {}" +
        "  caja.def(WigglyPoint, Point, { m0: x, m1: function() { this.p = 3; } });" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var Point = ___.simpleFunc(function() {});" +
        "  var WigglyPoint = ___.simpleFunc(function() {});" +
        "  caja.def(WigglyPoint, Point, {" +
        "      m0: ___OUTERS___.x," +
        "      m1: ___.method(WigglyPoint, function() {" +
        "        var t___ = this;" +
        "        (function() {" +
        "          var x___ = 3;" +
        "          t___.p_canSet___ ? (t___.p = x___) : ___.setProp(t___, 'p', x___);" +
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
        "  var Point = ___.simpleFunc(function() {});" +
        "  var WigglyPoint = ___.simpleFunc(function() {});" +
        "  caja.def(WigglyPoint, Point, {" +
        "      m0: ___OUTERS___.x," +
        "      m1: ___.method(WigglyPoint, function() {" +
        "        var t___ = this;" +
        "        (function() {" +
        "          var x___ = 3;" +
        "          t___.p_canSet___ ? (t___.p = x___) : ___.setProp(t___, 'p', x___);" +
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
        "  var foo = ___.ctor(function() {" +
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
        "  }" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var foo = ___.simpleFunc(function(x, y) {" +
        "      var a___ = ___.args(arguments);" +
        "      x = a___;" +
        "      y = ___OUTERS___.z;" +
        "  });"+
        "}));");
  }

  public void testFuncNamedSimpleValue() throws Exception {
    checkSucceeds(
        "function() {" +
        "  var f = function foo(x, y) {" +
        "    x = arguments;" +
        "    y = z;" +
        "  };" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var f = ___.primFreeze(___.simpleFunc(" +
        "    function foo(x, y) {" +
        "      var a___ = ___.args(arguments);" +
        "      x = a___;" +
        "      y = ___OUTERS___.z;" +
        "  }));"+
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
        "  var foo = ___.ctor(function(x, y) {" +
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
        "  var foo = ___.ctor(function(x, y) {" +
        "    var t___ = this;" +
        "    x = t___;" +
        "    y = ___OUTERS___.z;" +            
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
        "___OUTERS___.foo = ___.simpleFunc(function() {});" +
        "___OUTERS___.x instanceof ___.primFreeze(___OUTERS___.foo);");
  }

  public void testOtherBadInstanceof() throws Exception {
    checkFails(
        "var x = 3; y instanceof x;",
        "Invoked \"instanceof\" on non-function");
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
    new DefaultJsRewriter(true).expand(parseText(input), mq);

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
      throws Exception{
    MessageQueue mq = TestUtil.createTestMessageQueue(new MessageContext());
    ParseTreeNode actualResultNode = new DefaultJsRewriter().expand(inputNode, mq);
    for (Message m : mq.getMessages()) {
      if (m.getMessageLevel().compareTo(MessageLevel.WARNING) >= 0) {
        fail(m.toString());
      }
    }
    if (expectedResultNode != null) {
      assertEquals(
          format(expectedResultNode),
          format(actualResultNode));
    }
  }

  private void checkSucceeds(String input, String expectedResult) throws Exception {
    checkSucceeds(parseText(input), parseText(expectedResult));
  }

  private void checkSucceeds(String input) throws Exception {
    checkSucceeds(parseText(input), null);
  }

  private static String format(ParseTreeNode n) throws Exception {
    StringBuilder output = new StringBuilder();
    n.render(new RenderContext(new MessageContext(), output));
    // Alternative, to get "S-expression" tree representation for debug:
    //   n.format(new MessageContext(), output);
    return output.toString();
  }

  public ParseTreeNode parseText(String text) throws Exception {
    MessageContext mc = new MessageContext();
    MessageQueue mq = TestUtil.createTestMessageQueue(mc);
    InputSource is = new InputSource(new URI("file:///no/input/source"));
    CharProducer cp = CharProducer.Factory.create(new StringReader(text), is);
    JsLexer lexer = new JsLexer(cp);
    JsTokenQueue tq = new JsTokenQueue(lexer, is, JsTokenQueue.NO_NON_DIRECTIVE_COMMENT);
    Parser p = new Parser(tq, mq);
    Statement stmt = p.parse();
    p.getTokenQueue().expectEmpty();
    return stmt;

  }

  private String readResource(String resource) throws Exception {
    return TestUtil.readResource(getClass(), resource);    
  }
}
