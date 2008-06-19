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

  // TODO(metaweta): Move as many of these as possible to DefaultCajaRewriterTest
  // using assertConsistent and the rest to DebuggingSymbolsStageTest
  public void testEmptyGadget() throws Exception {
    execGadget("", "");
  }

  public void testPrimordialObjectExtension() throws Exception {
    // TODO(metaweta): Reenable once POE is part of warts mode.
    if (false) {
      execGadget(
          "var passed = false;" +
          "try{ caja.extend(Array, {length: 1}); }" +
          "catch(e) { passed = true; }" +
          "if (!passed) { fail('Able to mask existing methods with extensions.'); }",
          "");
      execGadget(
          "var passed = false;" +
          "function foo() {this;}" +
          "try{ caja.extend(foo, {x: 1}); }" +
          "catch(e) { passed = true; }" +
          "if (!passed) { fail('Able to extend arbitrary classes.'); }",
          "");
    }
  }

  public void testVariableRefInHandlerFunction() throws Exception {
    execGadget(
        "  <script type='text/javascript'>"
        + "var foo;"
        + "</script>"
        + "<a onclick='foo + bar;'>foo</a>",
        "");
  }

  /**
   * Tests that the container can get access to
   * "virtual globals" defined in cajoled code.
   *
   * @throws Exception
   */
  public void testWrapperAccess() throws Exception {
    // TODO(ihab.awad): SECURITY: Re-enable by reading (say) x.foo, and
    // defining the property IMPORTS___.foo.
    if (false) {
    execGadget(
        "<script>x='test';</script>",
        "if (___.getNewModuleHandler().getImports().x != 'test') {" +
          "fail('Cannot see inside the wrapper');" +
        "}"
        );
    }
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
   * Tests that setTimeout is uncallable.
   *
   * @throws Exception
   */
  public void testSetTimeout() throws Exception {
    execGadget(
        "<script>var success=false;try{setTimeout('1',10);}" +
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
        "<script>var x={}; var success=false;" +
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
        " document.getElementById('test-test').innerHTML)"
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
        "if (!foo) fail('Failed to construct a global object.');" +
        "assertEquals(foo.x, 2);" +
        "</script>",
        ""
        );
    execGadget(
        "<script>(function(){" +
        "function Foo(x){ this.x = x; }" +
        "var foo = new Foo(2);" +
        "if (!foo) fail('Failed to construct a local object.');" +
        "assertEquals(foo.x, 2);" +
        "})();</script>",
        ""
        );
    execGadget(
        "<script>" +
        "function Foo(x){" +
        "  console.log('called Foo(' + x + ')');" +
        "  this.x = x;" +
        "}" +
        "function Bar(y){" +
        "  Bar.super(this,5);" +
        "  console.log('called Bar(' + y + ')');" +
        "  this.y = y;" +
        "}" +
        "console.log('defined Foo and Bar');" +
        "caja.def(Bar, Foo);" +
        "var foo = new Foo(3);" +
        "var bar = new Bar(2);" +
        "if (!bar) fail('Failed to construct a derived object.');" +
        "assertEquals(bar.x, 5);" +
        "assertEquals(bar.y, 2);" +
        "</script>",
        ""
        );
    execGadget(
        "<script>" +
        "function Foo(){ }" +
        "var foo = new Foo();" +
        "if (!foo) fail('Failed to use a simple named function as a constructor.');" +
        "</script>",
        ""
        );
  }

  public void testCajaDef() throws Exception {
    execGadget(
        "<script>" +
        "function Foo(y) { this.y = y; }" +
        "function Bar(x) {" +
        "  Bar.super(this, 3);" +
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
    // TODO(ihab.awad): Disabled until we figure out how to get a test fixture
    // that allows us to add stuff to IMPORTS___ before the test is run.
    if (false) {
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
        "    ___.getNewModuleHandler().getImports().result.toSource()," +
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
        "    ___.getNewModuleHandler().getImports().test({x_:1, y:2, z:3})" +
        "        .sort().toSource()," +
        "    (['y', 'z']).toSource());");
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
        "    ___.getNewModuleHandler().getImports().obj.test()" +
        "        .sort().toSource()," +
        "    (['test', 'x_', 'y']).toSource());");
    }
  }

  public void testAttachedMethod() throws Exception {
    // The cases that succeed are tested in DefaultCajaRewriterTest
    execGadget(
        "<script>"
        + "var called = false;"
        + "var thisWhenCalled = undefined;"
        + "function Foo() {"
        + "  this.f = (function() {"
        + "    called = true;"
        + "    thisWhenCalled = this;"
        + "    this.x_ = 1;"
        + "  }).bind(this);"
        + "}"
        + "var foo = new Foo();"
        + "foo.f();"
        + "assertTrue(called); assertTrue(thisWhenCalled === foo);"
        + "called = false; thisWhenCalled = undefined;"
        + "var g = foo.f;"
        + "g();"
        + "assertTrue(called); assertTrue(thisWhenCalled === foo);"
        + "called = false; thisWhenCalled = undefined;"
        + "var x = {}; x.f = foo.f;"
        + "x.f();"
        + "assertTrue(called); assertTrue(thisWhenCalled === foo);"
        + "called = false; thisWhenCalled = undefined;"
        + "</script>",
        "");
    execGadget(
        "<script>" +
        "function Foo() {}" +
        "Foo.prototype.setX = function (x) { this.x_ = x; };" +
        "var foo = new Foo;" +
        "var h={setX:foo.setX};" +
        "var passed = false;" +
        "try { h.setX(1); } catch (e) { passed = true; }" +
        "if (!passed) {" +
        "  fail('Unattached methods are not being attached properly.');" +
        "}" +
        "</script>",
        "");
    execGadget(
        "<script>" +
        "function Foo() {}" +
        "Foo.prototype.setX = function (x) { this.x_ = x; };" +
        "var foo = new Foo;" +
        "var g = foo.setX;" +
        "var passed = false;" +
        "try { g(); } catch (e) { passed = true; }" +
        "if (!passed) {" +
        "  fail('Unattached methods are not being attached properly.');" +
        "}" +
        "</script>",
        "");
    execGadget(
        "<script>" +
        "function Foo() { this.gogo(); }" +
        "Foo.prototype.gogo = function () { " +
        "  this.Bar = function Bar(x) { " +
        "    this.x_ = x; " +
        "    this.getX = (function() { return this.x_; }).bind(this);" +
        "  };" +
        "};" +
        "var foo = new Foo;" +
        "var passed = false;" +
        "try { foo.Bar(5); } catch (e) { passed = true; }" +
        "if (!passed) {" +
        "  fail('Constructors are being attached as methods.');" +
        "}" +
        "</script>",
        "");
    execGadget(
        "<script>" +
        "function Foo() { this.gogo(); }" +
        "Foo.prototype.gogo = function () { " +
        "  function Bar(x){ " +
        "    this.x_ = x; " +
        "  }" +
        "  Bar.prototype.getX = function () { return this.x_; };" +
        "  this.Bar = Bar;" +
        "};" +
        "var foo = new Foo;" +
        "var passed = false;" +
        "try { foo.Bar(5); } catch (e) { passed = true; }" +
        "if (!passed) {" +
        "  fail('Constructors are being attached as methods.');" +
        "}" +
        "</script>",
        "");
  }

  public void testForwardReference() throws Exception {
    execGadget(
        "<script>" +
        "var g = Bar;" +
        "if (true) { var f = Foo; }" +
        "function Foo(){}" +
        "do { var h = Bar; function Bar(){this;} } while (0);" +
        "assertEquals(typeof f, 'function');" +
        "assertEquals(typeof g, 'undefined');" +
        "assertEquals(typeof h, 'function');" +
        "</script>",
        "");
    execGadget(
        "<script>(function(){" +
        "var g = Bar;" +
        "if (true) { var f = Foo; }" +
        "function Foo(){}" +
        "do { var h = Bar; function Bar(){this;} } while (0);" +
        "assertEquals(typeof f, 'function');" +
        "assertEquals(typeof g, 'undefined');" +
        "assertEquals(typeof h, 'function');" +
        "})();</script>",
        "");
  }

  private void execGadget(String gadgetSpec, String tests) throws Exception {
    MessageContext mc = new MessageContext();
    MessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(System.err), mc, true);
    PluginMeta meta = new PluginMeta();
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
      RenderContext rc = new RenderContext(mc, pp);
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
              "location = '" + htmlStubUrl + "';\n",
              "dom"),
          // Make the assertTrue, etc. functions available to javascript
          new RhinoTestBed.Input(getClass(), "asserts.js"),
          // Plugin Framework
          new RhinoTestBed.Input(getClass(), "../caja.js"),
          new RhinoTestBed.Input(getClass(), "html-emitter.js"),
          new RhinoTestBed.Input(getClass(), "container.js"),
          // The gadget
          new RhinoTestBed.Input(js.toString(), "gadget"),
          // The tests
          new RhinoTestBed.Input(tests, "tests"),
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
