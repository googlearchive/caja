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

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.quasiliteral.CajitaRewriter;
import com.google.caja.parser.html.Dom;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.reporting.MarkupRenderMode;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.TestBuildInfo;
import com.google.caja.util.Executor;
import com.google.caja.util.RhinoTestBed;
import com.google.caja.util.TestUtil;
import com.google.caja.util.CajaTestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import junit.framework.AssertionFailedError;

/**
 * End-to-end tests that compile a gadget to javascript and run the
 * javascript under Rhino to test them.
 *
 * @author stay@google.com (Mike Stay)
 */
public class HtmlCompiledPluginTest extends CajaTestCase {

  @Override
  protected void setUp() throws Exception {
    TestUtil.enableContentUrls();
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  // TODO(metaweta): Move as many of these as possible to
  // CajitaRewriterTest using assertConsistent and the rest to
  // DebuggingSymbolsStageTest
  public final void testEmptyGadget() throws Exception {
    execGadget("", "");
  }

  public final void testTestingFramework() throws Exception {
    try {
      // Make sure our JSUnit failures escape the try blocks that allow
      // execution to continue into subsequent script blocks.
      execGadget("<script>fail('hiya');</script>", "");
    } catch (AssertionFailedError ex) {
      assertEquals("hiya", ex.getMessage());
      return;
    }
    fail("Expected failure");
  }

  public final void testVariableRefInHandlerFunction() throws Exception {
    execGadget(
        "  <script type='text/javascript'>"
        + "var foo;"
        + "</script>"
        + "<a onclick='foo + bar;'>foo</a>",
        "");
  }

  public final void testNonNamespaceAwareDom() throws Exception {
    Document doc = DomParser.makeDocument(null, null);
    Node root = doc.createDocumentFragment();
    Element script = doc.createElement("script");
    script.setAttribute("type", "text/javascript");
    script.appendChild(doc.createTextNode("var foo;"));
    Element a = doc.createElement("a");
    a.setAttribute("onclick", "foo + bar;");
    a.appendChild(doc.createTextNode("foo"));
    root.appendChild(script);
    root.appendChild(a);
    execGadget(new Dom(root), "");
  }

  /**
   * Empty styles should not cause parse failure.
   * <a href="http://code.google.com/p/google-caja/issues/detail?id=56">bug</a>
   */
  public final void testEmptyStyle() throws Exception {
    execGadget("<style> </style>", "");
  }

  /**
   * Handlers should have their handlers rewritten.
   */
  public final void testHandlerRewriting() throws Exception {
    execGadget(
        "<a onclick=\"foo(this)\">hi</a>",

        // Handler is attached separately.
        ""
        + "assertEquals('<a target=\"_blank\">hi</a>',"
        + "             document.getElementById('test-test').innerHTML);");
  }

  public final void testECMAScript31Scoping() {
    // TODO(stay): Once they decide on scoping & initialization rules, test
    // them here.
  }

  public final void testExceptionsInScriptBlocks() throws Exception {
    execGadget(
        "<script>var a = 0, b = 0;</script>" +
        "<script>throw new Error(); a = 1;</script>" +
        "<script>b = 1;</script>\n" +
        "<script>\n" +
        "  assertEquals(0, a);" +
        "  assertEquals(1, b);" +
        "</script>",

        "");
  }

  public final void testCajitaBlocks() throws Exception {
    execGadget(
        ""
        + "<script>"
        + "  'use strict';"
        + "  Object.prototype.hello = 'there';"  // Monkey patch
        + "</script>"
        + "<script>"
        + "  'use strict';"
        + "  'use cajita';"
        + "  assertEquals('not visible in cajita', undefined, ({}).hello);"
        + "</script>"
        + "<script>"
        + "  'use strict';"
        + "  assertEquals('visible in valija', 'there', ({}).hello);"
        + "</script>"
        + "<script>"
        + "  assertEquals("
        + "      'nested cajita fns not patched',"
        + "      'there,,',"
        + "      [ ({}).hello,"
        + "        (function () {"
        + "          'use strict';"
        + "          'use cajita';"
        + "          return {}.hello;"
        + "        })(),"
        + "        (function f() {"
        + "          'use strict';"
        + "          'use cajita';"
        + "          return {}.hello;"
        + "        })() ]"
        + "      .join(','));"
        + "</script>"
        + "<script>"
        + "  assertEquals('cajita fn decls hoisted to block', undefined, f);"
        + "  {"
        + "    function f() { 'use strict'; 'use cajita'; return {}.hello; }"
        + "    assertEquals('cajita fns not patchable', undefined, f());"
        + "    assertThrows(function () { f.foo = 'bar'; });"
        + "    assertEquals('cajita fns frozen', undefined, f.foo);"
        + "  }"
        + "</script>",

        // Not visible when uncajoled.
        "assertEquals(undefined, ({}).hello);");
  }

  public final void testCustomOnErrorHandler() throws Exception {
    execGadget(
        "<script>\n" +
        "  'use cajita';\n" +
        "  window.a = 0, window.b = 0;\n" +
        "</script>\n" +
        "<script>\n" +
        "  'use cajita';\n" +
        "  window.messages = [];\n" +
        "  $v.so('onerror',\n" +
        "        function onerror(message, source, lineNumber) {\n" +
        "          window.messages.push(\n" +
        "              source + ':' + lineNumber + ': ' + message);\n" +
        "        });\n" +
        "</script>\n" +
        "<script>\n" +
        "  'use cajita';\n" +
        // The below is line 15
        "  throw new Error('panic');\n" +
        "  window.a = 1;</script>\n" +
        "<script>\n" +
        "  'use cajita';\n" +
        "  window.b = 1;\n" +
        "</script>\n" +
        "<script>\n" +
        "  'use cajita';\n" +
        "  assertEquals('window.a', 0, window.a);\n" +
        "  assertEquals('window.b', 1, window.b);\n" +
        "</script>\n" +
        "<script>\n" +
        "  'use cajita';\n" +
        "  assertEquals('# messages', 1, window.messages.length);\n" +
        "  assertEquals(\n" +
        "      'testCustomOnErrorHandler:15: panic', window.messages[0]);\n" +
        "</script>",

        // Sanity check to make sure that cajoled asserts ran properly.
        "assertEquals(1, imports.window.messages.length);");
  }

  public final void testPartialScript() throws Exception {
    PluginMeta meta = new PluginMeta();
    PluginCompiler compiler = new PluginCompiler(
        TestBuildInfo.getInstance(), meta, mq);
    compiler.setMessageContext(mc);
    Dom html = new Dom(htmlFragment(fromString("<script>{</script>")));
    compiler.addInput(html, is.getUri());

    boolean passed = compiler.run();
    assertFalse(passed);

    assertMessage(
        MessageType.END_OF_FILE, MessageLevel.ERROR,
        FilePosition.instance(is, 1, 9, 9, 1));
  }

  private void execGadget(String gadgetSpec, String tests) throws Exception {
    execGadget(new Dom(htmlFragment(fromString(gadgetSpec))), tests);
  }

  private void execGadget(Dom html, String tests) throws Exception {
    PluginMeta meta = new PluginMeta(
        UriFetcher.NULL_NETWORK, UriPolicy.IDENTITY);
    PluginCompiler compiler = new PluginCompiler(
        TestBuildInfo.getInstance(), meta, mq);
    compiler.setMessageContext(mc);
    compiler.addInput(html, html.getFilePosition().source().getUri());

    boolean failed = !compiler.run();

    if (failed) {
      fail();
    } else {
      CajoledModule jsTree = compiler.getJavascript();
      String staticHtml = Nodes.render(
          compiler.getStaticHtml(), MarkupRenderMode.XML);
      String js = render(jsTree);

      Block valijaOrigNode = js(fromResource(
          "/com/google/caja/valija-cajita.js"));
      ParseTreeNode valijaCajoledNode = new CajitaRewriter(
          TestBuildInfo.getInstance(), mq, false)
          .expand(new UncajoledModule(valijaOrigNode));
      String valijaCajoled = render(valijaCajoledNode);

      String htmlStubUrl = TestUtil.makeContentUrl(
          "<html><head/><body><div id=\"test-test\">"
          + staticHtml
          + "</div></body></html>");

      try {
        Executor.Input[] inputs = new Executor.Input[] {
            // Browser Stubs
            new Executor.Input(getClass(), "/js/jqueryjs/runtest/env.js"),
            // Console Stubs
            new Executor.Input(getClass(), "console-stubs.js"),
            // Initialize the DOM
            new Executor.Input(
                // Document not defined until window.location set.
                "location = '" + htmlStubUrl + "';\n",
                "dom"),
            // Make the assertTrue, etc. functions available to javascript
            new Executor.Input(
                getClass(), "../../../../js/jsunit/2.2/jsUnitCore.js"),
            // Plugin Framework
            new Executor.Input(
                getClass(), "../../../../js/json_sans_eval/json_sans_eval.js"),
            new Executor.Input(getClass(), "../cajita.js"),
            new Executor.Input(
                "___.setLogFunc(function(s, opt_stop) { console.log(s); });",
                "setLogFunc-setup"),
            new Executor.Input(
                "var valijaMaker = {};\n" +
                "var testImports = ___.copy(___.sharedImports);\n" +
                "testImports.loader = {\n" +
                "  provide: ___.markFuncFreeze(\n" +
                "      function(v) { valijaMaker = v; })\n" +
                "};\n" +
                "testImports.outers = ___.copy(___.sharedImports);\n" +
                "___.getNewModuleHandler().setImports(testImports);",
                getName() + "valija-setup"),
            new Executor.Input(valijaCajoled, "valija-cajoled"),
            new Executor.Input(getClass(), "html4-defs.js"),
            new Executor.Input(getClass(), "html-sanitizer.js"),
            new Executor.Input(getClass(), "bridal.js"),
            new Executor.Input(getClass(), "html-emitter.js"),
            new Executor.Input(getClass(), "container.js"),
            // The gadget
            new Executor.Input(js.toString(), "gadget"),
            // The tests
            new Executor.Input(tests, "tests"),
          };
        RhinoTestBed.runJs(inputs);
      } catch (Exception e) {
        System.out.println("Compiled gadget: \n" + staticHtml + "\n" + js);
        throw e;
      } catch (Error e) {
        System.out.println("Compiled gadget: \n" + staticHtml + "\n" + js);
        throw e;
      }
    }
  }
}
