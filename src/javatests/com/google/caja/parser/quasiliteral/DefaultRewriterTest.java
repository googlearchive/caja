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
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Statement;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.TestUtil;
import junit.framework.TestCase;

import java.io.StringReader;
import java.net.URI;

/**
 * Simple test harness for experimenting with quasiliteral rewrites. This
 * is not part of an automated test suite.
 *
 * @author ihab.awad@gmail.com
 */
public class DefaultRewriterTest extends TestCase {
  public void testSynthetic() throws Exception {
    // TODO(ihab.awad): Check that synthetic nodes are passed through
  }

  ////////////////////////////////////////////////////////////////////////
  // with
  ////////////////////////////////////////////////////////////////////////

  public void testWith0() throws Exception {
    // Our parser does not recognize "with" at all.
  }

  ////////////////////////////////////////////////////////////////////////
  // variable
  ////////////////////////////////////////////////////////////////////////

  public void testVariable0() throws Exception {
    // Tested by other cases since not independent
  }

  public void testVariable1() throws Exception {
    // Tested by other cases since not independent
  }

  public void testVariable2() throws Exception {
    checkFails(
        "function() { foo__; };",
        "Variables cannot end in \"__\"");
  }

  public void testVariable3() throws Exception {
    checkFails(
        "foo_;",
        "Globals cannot end in \"_\"");
  }

  public void testVariable4() throws Exception {
    /*
    checkFails(
        "function Ctor() { this.x = 1; }; var c = Ctor;",
        "Constructors are not first class");
    */
    // TODO(ihab.awad): This fails the "Constructor cannot escape" case instead
    // of the "Constructors are not first class" case; is there any way we can
    // trigger variable_4 instead? Or is variable_4 redundant?
  }

  public void testVariable5() throws Exception {
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

  public void testVariable6() throws Exception {
    checkSucceeds(
        "foo;",
        "___OUTERS___.foo;");
  }

  public void testVariable7() throws Exception {
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

  ////////////////////////////////////////////////////////////////////////
  // read
  ////////////////////////////////////////////////////////////////////////

  public void testRead0() throws Exception {
    checkFails(
        "x.y__;",
        "Properties cannot end in \"__\"");
  }

  public void testRead1() throws Exception {
    // TODO(ihab.awad): This throws "method in non-method context"
    /*
    checkSucceeds(
        "function() { this.x; };",
        "function() { this.x_canRead___ ? this.x : ___.readProp(this, 'x'); };");
    */
  }

  public void testRead2() throws Exception {
    checkFails(
        "foo.bar_;",
        "Public properties cannot end in \"_\"");
  }

  public void testRead3() throws Exception {
    checkSucceeds(
        "foo.p;",
        "(function() {" +
        "  var x___ = ___OUTERS___.foo;" +
        "  x___.p_canRead___ ? x___.p : ___.readPub(x___, 'p');" +
        "})();");
  }

  public void testRead4() throws Exception {
    checkSucceeds(
        "function foo() { this[3]; }",
        "___OUTERS___.foo = ___.ctor(" +
        "  function() {" +
        "    var t___ = this;" +
        "    ___.readProp(t___, 3);" +
        "  }" +
        ");");
  }
  
  public void testRead5() throws Exception {
    checkSucceeds(
        "function() { var foo; foo[3]; };",
        "___.primFreeze(___.simpleFunc(" +
        "  function() {" +
        "    var foo = undefined;" +
        "    ___.readPub(foo, 3);" +
        "}));");
  }

  ////////////////////////////////////////////////////////////////////////
  // set - assignments
  ////////////////////////////////////////////////////////////////////////

  public void testSet0() throws Exception {
    checkFails(
        "x.y__ = z;",
        "Properties cannot end in \"__\"");
  }

  public void testSet1() throws Exception {
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

  public void testSet2() throws Exception {
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

  public void testSet3() throws Exception {
    checkFails(
        "x.y_;",
        "Public properties cannot end in \"_\"");
  }

  public void testSet4() throws Exception {
    // TODO(ihab.awad): Implement. Have not reviewed expandMemberMap(...) stuff yet.
  }

  public void testSet5() throws Exception {
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

  public void testSet6() throws Exception {
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

  public void testSet7() throws Exception {
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

  public void testSet8() throws Exception {
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

  // TODO(ihab.awad): Why does MarkM have 2 cases for the below in expand.emaker, both checking
  // that "isVar" is true, and one with "var x = y" and the other with "x = y"?
  
  public  void testSet9() throws Exception {
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
  
  public  void testSet11() throws Exception {
    checkSucceeds(
        "function() {" +
        "  var v;" +
        "};",
        "___.primFreeze(___.simpleFunc(function() {" +
        "  var v = undefined;" +
        "}));");
  }

  public void testNew0() throws Exception {
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

  public void testNew1() throws Exception {
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

  public void testCall0() throws Exception {
    checkFails(
        "x.p__(3, 4);",
        "Selectors cannot end in \"__\"");
  }
  
  public void testCall1() throws Exception {
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
        "      t___.f_canCall___ ?" +
        "          this.f(x0___, x1___) :" +
        "          ___.callProp(t___, 'f', [x0___, x1___]);" +
        "    })();" +
        "  });" +
        "}));");
  }

  public void testCall2() throws Exception {
    checkFails(
        "o.p_();",
        "Public selectors cannot end in \"_\"");
  }

  public void testCall3() throws Exception {
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

  public void testCall4() throws Exception {
    // TODO(ihab.awad): Clarify expectations (and make object literals work!)
  }

  public void testCall5() throws Exception {
    checkSucceeds(
        "o.m(x, y);",
        "(function() {" +
        "  var x___ = ___OUTERS___.o;" +
        "  var x0___ = ___OUTERS___.x;" +
        "  var x1___ = ___OUTERS___.y;" +
        "  x___.m_canCall___ ? x___.m(x0___, x1___) : ___.callPub(x___, 'm', [x0___, x1___]);" +
        "})();");
  }

  public void testCall6() throws Exception {
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

  public void testCall7() throws Exception {
    checkSucceeds(
        "x[y](z, t);",
        "___.callPub(___OUTERS___.x, ___OUTERS___.y, [___OUTERS___.z, ___OUTERS___.t]);");
  }

  public void testCall8() throws Exception {
    checkSucceeds(
        "f(x, y);",
        "___.asSimpleFunc(___OUTERS___.f)(___OUTERS___.x, ___OUTERS___.y);");
  }

  public void testFunction0() throws Exception {
    checkSucceeds(
        "function(x, y) { x = arguments; y = z; };",
        "___.primFreeze(___.simpleFunc(function(x, y) {" +
        "  var a___ = ___.args(arguments);" +
        "  x = a___;" +
        "  y = ___OUTERS___.z;" +
        "}));");
  }

  public void testFunction1() throws Exception {
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

  public void testFunction2() throws Exception {
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

  public void testFunction3() throws Exception {
    checkFails(
        "function(x) { x = this; };",
        "Method in non-method context");
  }

  public void testFunction4() throws Exception {
    checkFails(
        "var f = function foo(x) { x = this; };",
        "Constructor cannot escape");
  }

  public void testFunction5() throws Exception {
    // TODO(ihab.awad): MarkM says hmmmm -- ??
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

  public void testFunction6() throws Exception {
    // TODO(ihab.awad): MarkM says hmmmm -- ??    
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

  public void testMap0() throws Exception {
    // TODO(ihab.awad): Unresolved issues; see source file.
  }

  public void testMap1() throws Exception {
    // TODO(ihab.awad): Implement object literals
    /*
    checkFails(
        "var o = { x_: 3, y: 4 };",
        "Key may not end in \"_\"");
    */
  }

  public void testMap2() throws Exception {
    // TODO(ihab.awad): Implement object literals
    /*
    checkSucceeds(
        "function() {" +
        "  var o = { k0: x, k1: y };" +
        "};",
        "function() {" +
        "  var o = { k0: ___OUTERS___.x, k1: ___OUTERS___.y };" +            
        "};");
    */
  }

  public void testMap3() throws Exception {
    // TODO(ihab.awad): Implement object literals
    /*
    checkFails(
        "var o = { 'a' + 'b' : 3 };",
        "Key expressions not yet supported");
    */
  }

  public void testOther0() throws Exception {
    checkSucceeds(
        "function foo() {}" +
        "x instanceof foo;",      
        "___OUTERS___.foo = ___.simpleFunc(function() {});" +
        "___OUTERS___.x instanceof ___.primFreeze(___OUTERS___.foo);");
  }

  public void testOther1() throws Exception {
    checkFails(
        "var x = 3; y instanceof x;",
        "Invoked instanceof on non-function");
  }

  //////////////////////////////////////////////////////////////////////////////////
  //
  // END OF NEW-STYLE, SYSTEMATIC TESTS OF REWRITER
  // TESTS BELOW ARE _AD HOC_ OLD STUFF
  //
  //////////////////////////////////////////////////////////////////////////////////

  public void testFunction() throws Exception {
    // TODO(ihab.awad): Apply some test conditions
    showTree("function.js");
  }

  public void testClickme() throws Exception {
    // TODO(ihab.awad): Apply some test conditions
    showTree("clickme.js");
  }

  public void testListfriends() throws Exception {
    // TODO(ihab.awad): Apply some test conditions
    showTree("listfriends.js");
  }
  
  private void checkFails(String input, String error) throws Exception {
    try {
      new DefaultRewriter().expand(parseText(input));
      fail("Should have failed on input: " + input);
    } catch (Exception e) {
      assertTrue(
          "Error does not contain \"" + error + "\": " + e.toString(),
          e.toString().contains(error));
    }
  }

  private void checkSucceeds(String input, String expectedResult) throws Exception {
    ParseTreeNode expectedResultNode = parseText(expectedResult);
    ParseTreeNode inputNode = parseText(input);
    ParseTreeNode actualResultNode = new DefaultRewriter().expand(inputNode);
    assertEquals(
        format(expectedResultNode),
        format(actualResultNode));
  }

  private void showTree(String resource) throws Exception {
    ParseTreeNode program = parseResource(resource);
    ParseTreeNode rewritten = new DefaultRewriter().expand(program);
    System.out.println("program = " + format(program));
    System.out.println("rewritten = " + format(rewritten));
  }

  private static String format(ParseTreeNode n) throws Exception {
    StringBuilder output = new StringBuilder();
    n.render(new RenderContext(new MessageContext(), output));
    // n.format(new MessageContext(), output);
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

  public ParseTreeNode parseResource(String resource) throws Exception {
    return parseText(TestUtil.readResource(getClass(), resource));    
  }
}