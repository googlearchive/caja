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
   * Tests that Object.eval is uncallable.
   * 
   * @throws Exception
   */
  public void testObjectEval() throws Exception {
    execGadget(
        "<script>var success=false;" +
          "try{Object.eval('1')}catch(e){success=true;}" + 
          "if (!success)fail('Object.eval is accessible.')</script>",
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

  private void execGadget(String gadgetSpec, String tests) throws Exception {
    MessageContext mc = new MessageContext();
    MessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(System.err), mc, true);
    PluginMeta meta = new PluginMeta(
        "___OUTERS___", "test", "", "test", PluginMeta.TranslationScheme.CAJA,
        PluginEnvironment.CLOSED_PLUGIN_ENVIRONMENT);
    HtmlPluginCompiler compiler = new HtmlPluginCompiler(mq, meta);
    compiler.setMessageContext(mc);
    DomTree html = parseHtml(gadgetSpec);
    if (html != null) { compiler.addInput(new AncestorChain<DomTree>(html)); }

    boolean failed = !compiler.run();

    if (failed) {
      fail();
    } else {
      Block jsTree = compiler.getJavascript();
      StringBuilder js = new StringBuilder();
      RenderContext rc = new RenderContext(mc, js, false);
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

  DomTree parseHtml(String html) throws Exception {
    InputSource is = new InputSource(new URI("test://" + getName()));
    StringReader in = new StringReader(html);
    TokenQueue<HtmlTokenType> tq = DomParser.makeTokenQueue(is, in, false);
    if (tq.isEmpty()) { return null; }
    return DomParser.parseFragment(tq);
  }
}
