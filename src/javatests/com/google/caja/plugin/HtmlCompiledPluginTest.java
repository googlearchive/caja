// Copyright 2007 Google Inc. All Rights Reserved
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

package com.google.caja.plugin;

import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.js.Block;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.RhinoTestBed;
import com.google.caja.util.TestUtil;

import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * End-to-end tests that compile a gadget to javascript and run the
 * javascript under Rhino to test them.
 *
 * @author stay@google.com (Mike Stay)
 */
public class HtmlCompiledPluginTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    TestUtil.enableContentUrls();
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  // TODO(stay): Move as many of these as possible to DefaultCajaRewriterTest
  //             using assertConsistent
  public void testEmptyGadget() throws Exception {
    execGadget("", "");
  }

  /**
   * Tests that the container can get access to
   * "virtual globals" defined in cajoled code.
   *
   * @throws Exception
   */
  public void testWrapperAccess() throws Exception {
    execGadget(
        "<script>x='test';</script>",
        "if (___.getNewModuleHandler().getOuters().x != 'test') {" +
          "fail('Cannot see inside the wrapper');" +
        "}"
        );
  }

  /**
   * Tests that Array.prototype cannot be modified.
   *
   * @throws Exception
   */
  public void testFrozenArray() throws Exception {
    execGadget(
        "<script>" +
        "var success = false;" +
        "try {" +
          "Array.prototype[4] = 'four';" +
        "} catch (e){" +
          "success = true;" +
        "}" +
        "if (!success) fail('Array not frozen.');" +
        "</script>",
        ""
        );
  }

  /**
   * Tests that Object.prototype cannot be modified.
   *
   * @throws Exception
   */
  public void testFrozenObject() throws Exception {
    execGadget(
        "<script>" +
        "var success = false;" +
        "try {" +
          "Object.prototype.x = 'X';" +
        "} catch (e){" +
          "success = true;" +
        "}" +
        "if (!success) fail('Object not frozen.');" +
        "</script>",
        ""
        );
  }

  /**
   * Tests that eval is uncallable.
   *
   * @throws Exception
   */
  public void testEval() throws Exception {
    execGadget(
        "<script>var success=false;" +
          "try{eval('1')}catch(e){success=true;}" +
          "if (!success)fail('Outer eval is accessible.')</script>",
        ""
        );
  }

  /**
   * Tests that cajoled code can't construct new Function objects.
   *
   * @throws Exception
   */
  public void testFunction() throws Exception {
    execGadget(
        "<script>var success=false;" +
          "try{var f=new Function('1')}catch(e){success=true;}" +
          "if (!success)fail('Function constructor is accessible.')</script>",
        ""
        );
  }

  /**
   * Tests that constructors are inaccessible.
   *
   * @throws Exception
   */
  public void testConstructor() throws Exception {
    try {
      execGadget(
          "<script>function x(){}; var F = x.constructor;</script>",
          ""
          );
    } catch (junit.framework.AssertionFailedError e) {
      // pass
    }
  }

  /**
   * Tests that arguments to functions are not mutable through the
   * arguments array.
   *
   * @throws Exception
   */
  public void testMutableArguments() throws Exception {
    execGadget(
        "<script>" +
        "function f(a) {" +
          "try{" +
            "arguments[0] = 1;" +
            "if (a) fail('Mutable arguments');" +
          "} catch (e) {" +
             // pass
          "}" +
        "}" +
        "f(0);" +
        "</script>",
        ""
        );
  }

  /**
   * Tests that the caller attribute is unreadable.
   *
   * @throws Exception
   */
  public void testCaller() throws Exception {
    execGadget(
        "<script>" +
        "function f(x) {" +
          "if (arguments.caller || f.caller) fail('caller is accessible');" +
        "}" +
        "f(1);" +
        "</script>",
        ""
        );
  }

  /**
   * Tests that the callee attribute is unreadable.
   *
   * @throws Exception
   */
  public void testCallee() throws Exception {
    execGadget(
        "<script>" +
        "function f(x) {" +
          "if (arguments.callee || f.callee) fail('caller is accessible');" +
        "}" +
        "f(1);" +
        "</script>",
        ""
        );
  }

  /**
   * Tests that arguments are immutable from another function's scope.
   *
   * @throws Exception
   */
  public void testCrossScopeArguments() throws Exception {
    execGadget(
        "<script>" +
        "function f(a) {" +
          "g();" +
          "if (a) fail('Mutable cross scope arguments');" +
        "}\n" +
        "function g() {" +
          "if (f.arguments) " +
            "f.arguments[0] = 1;" +
        "}" +
        "f(0);" +
        "</script>",
        ""
        );
  }

  /**
   * Tests that exceptions are not visible outside of the catch block.
   *
   * @throws Exception
   */
  public void testCatch() throws Exception {
    try {
      execGadget(
          "<script>" +
          "var e = 0;" +
          "try{ throw 1; } catch (e) {}" +
          "if (e) fail('Exception visible out of proper scope');" +
          "</script>",
          ""
          );
      fail("Exception that masks var should not pass");
    } catch (AssertionFailedError e) {
      // pass
    }
  }

  /**
   * Tests that cajoled code can refer to the virtual global scope.
   *
   * @throws Exception
   */
  public void testVirtualGlobalThis() throws Exception {
    execGadget(
        "<script>x=this;</script>",

        "if (___.getNewModuleHandler().getOuters().x"
        + "!== ___.getNewModuleHandler().getOuters())"
        + "  fail('this not rewritten to outers in global scope');"
        );
  }

  /**
   * Tests that the virtual global scope is not the real global scope.
   *
   * @throws Exception
   */
  public void testThisIsGlobalScope() throws Exception {
    execGadget(
        "<script>try{x=this;}catch(e){}</script>",
        "if (___.getNewModuleHandler().getOuters().x === this)" +
          "fail('Global scope is accessible');"
        );
  }

  /**
   * Tests that the 'prototype' property of the virtual global scope
   * is not visible.
   *
   * @throws Exception
   */
  public void testGlobalScopePrototypeInvisible() throws Exception {
    // TODO(ihab.awad): Disabled for now, but see issue145
    if (false) {
    execGadget(
        "<script>var x = 1; x = this.prototype; x = 2;</script>",
        "if (___.getNewModuleHandler().getOuters().x === 2)" +
          "fail('Global scope prototype is accessible');"
        );
    }
  }

  /**
   * Tests that setTimeout is uncallable.
   *
   * @throws Exception
   */
  public void testSetTimeout() throws Exception {
    execGadget(
        "<script>success=false;try{setTimeout('1',10);}" +
        "catch(e){success=true;}" +
        "if(!success)fail('setTimeout is accessible');</script>",
        ""
        );
  }

  /**
   * Tests that Object.watch is uncallable.
   *
   * @throws Exception
   */
  public void testObjectWatch() throws Exception {
    execGadget(
        "<script>x={}; success=false;" +
        "try{x.watch(y, function(){});}" +
        "catch(e){success=true;}" +
        "if(!success)fail('Object.watch is accessible');</script>",
        ""
        );
  }

  /**
   * Tests that unreadable global properties are not readable by way of
   * Object.toSource().
   *
   * @throws Exception
   */
  public void testToSource() throws Exception {
    execGadget(
        "<script>var x;" +
        "try{x=toSource();}catch(e){}" +
        "if(x) fail('Global write-only values are readable.');</script>",
        ""
        );
  }

  /**
   * Empty styles should not cause parse failure.
   * <a href="http://code.google.com/p/google-caja/issues/detail?id=56">bug</a>
   */
  public void testEmptyStyle() throws Exception {
    execGadget("<style> </style>", "");
  }

  /**
   * Handlers should have their handlers rewritten.
   */
  public void testHandlerRewriting() throws Exception {
    execGadget(
        "<a onclick=\"foo(this)\">hi</a>",

        "assertEquals('<a onclick=\"return plugin_dispatchEvent___(" +
        "this, event || window.event, 0, \\'c_1___\\')\">hi</a>'," +
        " outers.emitHtml___.htmlBuf_.join(''))"
        );
  }

  /**
   * Try to construct some class instances.
   */
  public void testFuncCtor() throws Exception {
    execGadget(
        "<script>" +
        "function Foo(x){ this.x = x; }" +
        "var foo = new Foo(2);" +
        "if (!foo) fail('Failed to construct a global object.')" +
        "assertEquals(foo.x, 2);" +
        "</script>",
        ""
        );
    execGadget(
        "<script>(function(){" +
        "function Foo(x){ this.x = x; }" +
        "var foo = new Foo(2);" +
        "if (!foo) fail('Failed to construct a local object.')" +
        "assertEquals(foo.x, 2);" +
        "})()</script>",
        ""
        );
    execGadget(
        "<script>" +
        "function Foo(x){ this.x = x; }" +
        "function Bar(y){ Foo.call(this,5); this.y = y; }" +
        "var bar = new Bar(2);" +
        "if (!bar) fail('Failed to construct a derived object.')" +
        "assertEquals(bar.x, 5);" +
        "assertEquals(bar.y, 2);" +
        "</script>",
        ""
        );
    execGadget(
        "<script>" +
        "function Foo(){ }" +
        "var foo = new Foo();" +
        "if (!foo) fail('Failed to use a simple named function as a constructor.')" +
        "</script>",
        ""
        );
  }

  public void testCajaDef() throws Exception {
    execGadget(
        "<script>" +
        "function Foo(y) { this.y = y; }" +
        "function Bar(x) {" +
        "  Foo.call(this, 3);" +
        "  this.x_ = x;" +
        "}" +
        "caja.def(Bar, Foo, {getX:function () { return this.x_; }});" +
        "var bar = new Bar(2);" +
        "assertEquals(bar.y, 3);" +
        "assertEquals(bar.getX(), 2);" +
        "(function (constr) {" +
        "  var baz = new constr(4);" +
        "  assertEquals(baz.getX(), 4);" +
        "})(Bar);" +
        "</script>",
        "");
  }

  public void testECMAScript31Scoping() throws Exception {
    // TODO(stay): Once they decide on scoping & initialization rules, test them here.
  }
  
  public void testForIn() throws Exception {
    execGadget(
        "<script>" +
        "function Foo() {" +
        "  this.x_ = 1;" +
        "  this.y = 2;" +
        "  this.z = 3;" +
        "}" +
        "var obj = new Foo();" +
        "var y = {};" +
        "var result = [];" +
        "for (y.k in obj) {" +
        "  result.push(y.k);" +
        "}" +
        "</script>",
        "assertEquals(" +
        "    ___.getNewModuleHandler().getOuters().result.toSource()," +
        "    (['y', 'z']).toSource());");
    execGadget(
        "<script>" +
        "function test(obj) {" +
        "  var y = {};" +
        "  var result = [];" +
        "  for (y.k in obj) {" +
        "    result.push(y.k);" +
        "  }" +
        "  return result;" +
        "}" +
        "</script>",
        "assertEquals(" +
        "    ___.getNewModuleHandler().getOuters().test({x_:1, y:2, z:3}).sort().toSource()," +
        "    (['y', 'z']).toSource());");
    // TODO(metaweta): Put this test back in when issue142 is fixed.
    if (false) {
      execGadget(
          "<script>" +
          "function Foo() {" +
          "  this.x_ = 1;" +
          "  this.y = 2;" +
          "}" +
          "caja.def(Foo, Object, {" +
          "  test: function () {" +
          "    var y = {};" +
          "    var result = [];" +
          "    for (y.k in this) {" +
          "      result.push(y.k);" +
          "    }" +
          "    return result;" +
          "  }});" +
          "var obj = new Foo();" +
          "</script>",
          "assertEquals(" +
          "    ___.getNewModuleHandler().getOuters().obj.test().sort().toSource()," +
          "    (['test', 'x', 'y']).toSource());");
    }
  }
    
  public void testInstanceMethod() throws Exception {
    // TODO(metaweta): Put this test back in when issue143 is fixed.
    if (false) {
      execGadget(
          "<script>" +
          "function Foo() { this.f = function(){ return this; }}" +
          "</script>",
          "");
    }
  }
  
  public void testGlobalThis() throws Exception {
    execGadget(
        "<script>" +
        "var y = this.foo;" +
        "assertEquals(y, undefined);" +
        "</script>",
        "");
    execGadget(
        "<script>" +
        "var passed = false;" +
        "try {" +
        "  var y = foo;" +
        "} catch (e) { passed = true; }" +
        "if (!passed) fail('Should have thrown a ReferenceError.');" +
        "</script>",
        "");
  }
  
  public void testStaticMembers() throws Exception {
    execGadget("<script>" +
        "function Foo(){}" +
        "Foo.prototype.x = 1;" +
        "</script>",
        "");
  }
  
  private void execGadget(String gadgetSpec, String tests) throws Exception {
    MessageContext mc = new MessageContext();
    MessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(System.err), mc, true);
    PluginMeta meta = new PluginMeta(
        "test", PluginEnvironment.CLOSED_PLUGIN_ENVIRONMENT);
    PluginCompiler compiler = new PluginCompiler(meta, mq);
    compiler.setMessageContext(mc);
    DomTree html = parseHtml(gadgetSpec, mq);
    if (html != null) { compiler.addInput(new AncestorChain<DomTree>(html)); }

    boolean failed = !compiler.run();

    if (failed) {
      fail();
    } else {
      Block jsTree = compiler.getJavascript();
      StringBuilder js = new StringBuilder();
      JsPrettyPrinter pp = new JsPrettyPrinter(js, null);
      RenderContext rc = new RenderContext(mc, false, pp);
      jsTree.render(rc);
      System.out.println("Compiled gadget: " + js);

      String htmlStubUrl = TestUtil.makeContentUrl(
          "<html><head/><body><div id=\"test-test\"/></body></html>");

      RhinoTestBed.Input[] inputs = new RhinoTestBed.Input[] {
          // Browser Stubs
          new RhinoTestBed.Input(getClass(), "/js/jqueryjs/runtest/env.js"),
          // Console Stubs
          new RhinoTestBed.Input(getClass(), "console-stubs.js"),
          // Initialize the DOM
          new RhinoTestBed.Input(
              // Document not defined until window.location set.
              new StringReader("location = '" + htmlStubUrl + "';\n"),
              "dom"),
          // Make the assertTrue, etc. functions available to javascript
          new RhinoTestBed.Input(getClass(), "asserts.js"),
          // Plugin Framework
          new RhinoTestBed.Input(getClass(), "../caja.js"),
          new RhinoTestBed.Input(getClass(), "container.js"),
          // The gadget
          new RhinoTestBed.Input(new StringReader(js.toString()), "gadget"),
          // The tests
          new RhinoTestBed.Input(new StringReader(tests), "tests"),
        };
      RhinoTestBed.runJs(null, inputs);
    }
  }

  DomTree parseHtml(String html, MessageQueue mq) throws Exception {
    InputSource is = new InputSource(new URI("test://" + getName()));
    StringReader in = new StringReader(html);
    TokenQueue<HtmlTokenType> tq = DomParser.makeTokenQueue(is, in, false);
    if (tq.isEmpty()) { return null; }
    return new DomParser(tq, false, mq).parseFragment();
  }
}
