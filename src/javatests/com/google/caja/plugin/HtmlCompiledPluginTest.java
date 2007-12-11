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

import com.google.caja.lexer.ParseException;
import com.google.caja.util.RhinoTestBed;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;

/**
 * End-to-end tests that compile a gadget to javascript and run the
 * javascript under Rhino to test them.
 *
 * @author stay@google.com (Mike Stay)
 *
 */
public class HtmlCompiledPluginTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testEmptyGadget() throws Exception {
    execGadget("", "");
  }

  public void testWrapperAccess() throws Exception {
    execGadget(
        "<script>x='test';</script>",
        "if (___.latestOuters.x != 'test') fail('Cannot see inside the wrapper');"
        );
  }

  public void testArrayPoisoning() throws Exception {
    execGadget(
        "<script>try { Array.prototype[4] = 'four'; } catch(e) {}</script>",
        "a=[]; if (a[4]=='four') fail('Array poisoned');"
        );
  }

  public void testObjectPoisoning() throws Exception {
    execGadget(
        "<script>try { Object.prototype.toString = " +
        "function(){ this.name='mud'; }; } catch(e) {}</script>",
        "o={}; o.toString(); " +
        "if (o.name) fail('Object poisoned');"
        );
  }

  public void testEval() throws Exception {
    execGadget(
        "<script>x=eval;</script>",
        "if (___.latestOuters.x) fail('eval is accessible');"
        );
  }

  public void testObjectEval() throws Exception {
    execGadget(
        "<script>x=Object.eval;</script>",
        "if (___.latestOuters.x) fail('Object.eval is accessible');"
        );
  }

  public void testFunction() throws Exception {
    execGadget(
        "<script>x=Function;</script>",
        "if (___.latestOuters.x) fail('eval is accessible');"
        );
  }

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

  public void testMutableArguments() throws Exception {
    execGadget(
        "<script>" +
        "function f(a, fail) {" +
          "try { arguments[0] = 1; } catch(e) {}" +
          "if (a) fail('Mutable arguments');" +
        "}" +
        "</script>",
        "___.latestOuters.f(0, fail);"
        );
  }

  public void testCaller() throws Exception {
    execGadget(
        "<script>" +
        "function f(fail) {" +
          "if (arguments.caller || f.caller) fail('caller is accessible');" +
        "}" +
        "</script>",
        "___.latestOuters.f(fail);"
        );
  }

  public void testCallee() throws Exception {
    execGadget(
        "<script>" +
        "function f(fail) {" +
          "if (arguments.callee || f.callee) fail('callee is accessible');" +
        "}" +
        "</script>",
        "___.latestOuters.f(fail);"
        );
  }

  public void testCrossScopeArguments() throws Exception {
    execGadget(
        "<script>" +
        "function f(a, fail) {" +
          "g();" +
          "if (a) fail('Mutable cross scope arguments');" +
        "}\n" +
        "function g() {" +
          "if (f.arguments) " +
            "f.arguments[0] = 1;" +
        "}" +
        "</script>",
        "___.latestOuters.f(0, fail);"
        );
  }

  public void testCatch1() throws Exception {
    execGadget(
        "<script>" +
        "function f(fail) {" +
          "var e = 0;" +
          "try{ throw 1; } catch (e) {}" +
          "if (e) fail('Exception visible out of proper scope');" +
        "}" +
        "</script>",
        "___.latestOuters.f(fail);"
        );
  }

  public void testCatch2() throws Exception {
    execGadget(
        "<script>" +
        "(function () {" +
          "try{ throw 1; } catch (e) {}" +
        "})();" +
        "function f(fail) {" +
          "if (e) fail('Exception visible out of proper scope');" +
        "}" +
        "</script>",
        "___.latestOuters.f(fail);"
        );
  }

  public void testThisIsGlobalScope() throws Exception {
    execGadget(
        "<script>" +
        "var x;" +
        "(function () { try { x=this.document.cookie; } catch(e) {} })();" +
        "function f(fail) { if (x) fail('this accesses global scope') };" +
        "</script>",
        "___.latestOuters.f(fail);"
        );
  }

  public void testSetTimeout() throws Exception {
    execGadget(
        "<script>x=setTimeout;</script>",
        "if (___.latestOuters.x === setTimeout) fail('setTimeout is accessible');"
        );
  }

  public void testObjectWatch() throws Exception {
    execGadget(
        "<script>x={}; x=x.watch;</script>",
        "if (___.latestOuters.x) fail('Object.watch is accessible');"
        );
  }

  public void testToSource() throws Exception {
    execGadget(
        "<script>" +
        "function f(fail) {" +
          "if (toSource) fail('top level toSource is accessible');" +
        "}" +
        "</script>",
        "___.latestOuters.f(fail);"
    );
  }

  public void execGadget(String gadget_spec, String tests)
      throws IOException {
    HtmlPluginCompiler compiler = new HtmlPluginCompiler(
        gadget_spec, "test", "test", "test", true);
    boolean failed=false;
    try {
      if (!compiler.run()){
        failed = true;
      }
    } catch (ParseException e) {
      e.toMessageQueue(compiler.getMessageQueue());
      failed = true;
    }
    if (failed) {
      fail(compiler.getErrors());
    } else {
      String js = compiler.getOutputJs();
      System.out.println("Compiled gadget: " + js);
      RhinoTestBed.Input[] inputs = new RhinoTestBed.Input[] {
          // Make the assertTrue, etc. functions available to javascript
          new RhinoTestBed.Input(
              CompiledPluginTest.class,
              "browser-stubs.js"),
          new RhinoTestBed.Input(CompiledPluginTest.class, "asserts.js"),
          // Plugin Framework
          new RhinoTestBed.Input(
              CompiledPluginTest.class,
              "caps/wrap_capability.js"),
              // TODO(benl): read this from where it really is
//            new RhinoTestBed.Input(CompiledPluginTest.class,
//            "../../../../../js/com/google/caja/caja.js"),
          new RhinoTestBed.Input(CompiledPluginTest.class, "tmp-caja.js"),
          new RhinoTestBed.Input(CompiledPluginTest.class, "html-sanitizer.js"),
          new RhinoTestBed.Input(
              new StringReader(
                  "var div = document.createElement('div');\n" +
                  "div.id = 'test-test';\n" +
                  "document.body.appendChild(div);\n"),
              "dom"),
          // The Gadget
          new RhinoTestBed.Input(
              new StringReader(js),
              "gadget"),
          // The tests
          new RhinoTestBed.Input(new StringReader(tests), "tests"),
        };
      RhinoTestBed.runJs(null, inputs);
    }
  }
}
