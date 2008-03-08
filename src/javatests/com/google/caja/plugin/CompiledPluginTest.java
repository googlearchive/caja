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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.css.CssTree;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.RhinoTestBed;
import com.google.caja.util.TestUtil;
import junit.framework.AssertionFailedError;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * End-to-end tests that compiles a plugin to javascript and runs the
 * javascript under Rhino to test the base plugin implementation.
 *
 * <p>This also serves as a test of plugin-base.js</p>
 *
 * @author mikesamuel@gmail.com
 */
public class CompiledPluginTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    TestUtil.enableContentUrls();
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testTestFramework() throws Exception {
    // Make sure that assertions in javascript can fail
    try {
      execPlugin("console.log(\"testing test framework.  'foo' != 'bar'\");\n" +
                 "assertEquals('foo', 'bar');");
      fail("javascript asserts are borked");
    } catch (AssertionFailedError e) {
      // pass
    } catch (Exception e) {
      fail("javascript asserts are borked");
    }
  }

  public void testHelloWorld() throws Exception {
    execPlugin(
        "\n  var div = document.createElement('DIV');" +
        "\n  div.id = 'pre-hello-base';" +
        "\n  document.body.appendChild(div);" +
        "\n  testOuters.main();" +
        "\n" +
        "\n  assertEquals(" +
        "\n      '<h1>Hello World</h1>'," +
        "\n      document.getElementById('pre-hello-base').innerHTML" +
        "\n      );",
        new PluginFile(
            new StringReader(
                "<gxp:template name=\"sayHello\">\n" +
                "  <h1>Hello World</h1>\n" +
                "</gxp:template>"),
            "file:///hello-world.gxp"),
        new PluginFile(
            new StringReader(
                "h1 { color: black; background: white; }"),
            "file:///hello-world.css"),
        new PluginFile(
            new StringReader(
                "function main() {\n" +
                "  document.getElementById('hello-base')\n" +
                "      .setInnerHTML(sayHello());\n" +
                "}"),
            "file:///hello-world.js")
        );
  }

  public void testConditional() throws Exception {
    execPlugin(
        "\n  var div = document.createElement('DIV');" +
        "\n  div.id = 'pre-base';" +
        "\n  document.body.appendChild(div);" +
        "\n" +
        "\n  testOuters.main(1);" +
        "\n  assertEquals(" +
        "\n      'Branch A'," +
        "\n      document.getElementById('pre-base').innerHTML" +
        "\n      );" +
        "\n" +
        "\n  testOuters.main(0);" +
        "\n  assertEquals(" +
        "\n      'Branch B'," +
        "\n      document.getElementById('pre-base').innerHTML" +
        "\n      );",
        new PluginFile(
            new StringReader(
                "<gxp:template name=\"test\">\n" +
                "  <gxp:param name=\"c\"/>\n" +
                "  <gxp:if cond=\"c\">" +
                "    Branch A\n" +
                "  <gxp:else/>\n" +
                "    Branch B\n" +
                "  </gxp:if>\n" +
                "</gxp:template>"),
            "file:///conditional-test.gxp"),
        new PluginFile(
            new StringReader(
                "function main(c) {\n" +
                "  document.getElementById('base').setInnerHTML(test(c));\n" +
                "}"),
            "file:///conditional-test.js")
        );
  }

  public void testLoop() throws Exception {
    execPlugin(
        "\n  var div = document.createElement('DIV');" +
        "\n  div.id = 'pre-base';" +
        "\n  document.body.appendChild(div);" +
        "\n" +
        "\n  testOuters.main(['foo', 'bar', 'boo & baz']);" +
        "\n  assertEquals(" +
        "\n      '<ul><li>foo</li><li>bar</li>' +" +
        "\n      '<li>boo &amp; baz</li></ul>'," +
        "\n      document.getElementById('pre-base').innerHTML" +
        "\n      );",
        new PluginFile(
            new StringReader(
                "<gxp:template name=\"test\">\n" +
                "  <gxp:param name=\"items\"/>\n" +
                "  <ul>\n" +
                "    <gxp:loop var=\"item\" iterator=\"items\">" +
                "      <li><gxp:eval expr=\"item\"/></li>\n" +
                "    </gxp:loop>\n" +
                "  </ul>\n" +
                "</gxp:template>"),
            "file:///loop-test.gxp"),
        new PluginFile(
            new StringReader(
                "function main(items) {\n" +
                "  document.getElementById('base')\n" +
                "      .setInnerHTML(test(items));\n" +
                "}"),
            "file:///loop-test.js")
        );
  }

  public void testAttr() throws Exception {
    execPlugin(
        "\n  var div = document.createElement('DIV');" +
        "\n  div.id = 'pre-base';" +
        "\n  document.body.appendChild(div);" +
        "\n" +
        "\n  testOuters.main();" +
        "\n  assertEquals(" +
        "\n      '<a class=\"pre-class1 pre-class2\"' +" +
        "\n      ' href=\"/plugin1/foo.html?a=b&amp;c=d\"' +" +
        "\n      ' id=\"pre-id\" target=\"_new\"' +" +
        "\n      ' title=\"&quot;hover text&quot;\"' +" +
        "\n      '>Clicky' +" +
        "\n      '\\n  </a>'," +
        "\n      document.getElementById('pre-base').innerHTML" +
        "\n      );",
        new PluginFile(
            new StringReader(
                "<gxp:template name=\"test\">\n" +
                "  <gxp:param name=\"url\"/>\n" +
                "  <gxp:param name=\"altText\"/>\n" +
                "  <gxp:param name=\"classes\"/>\n" +
                "  <a>\n" +
                "    <gxp:attr name=\"href\">\n" +
                "      <gxp:eval expr=\"url\"/>\n" +
                "    </gxp:attr>\n" +
                "    <gxp:attr name=\"title\">\n" +
                "      <gxp:eval expr=\"altText\"/>\n" +
                "    </gxp:attr>\n" +
                "    <gxp:attr name=\"id\">\n" +
                "      id\n" +
                "    </gxp:attr>\n" +
                "    <gxp:attr name=\"class\">\n" +
                "      <gxp:loop var=\"className\" iterator=\"classes\">\n" +
                "        <gxp:eval expr=\"className\"/>\n" +
                "        <![CDATA[ ]]>\n" +
                "      </gxp:loop>\n" +
                "    </gxp:attr>\n" +
                "    Clicky\n" +
                "  </a>\n" +
                "</gxp:template>"),
            "file:///attr-test.gxp"),
        new PluginFile(
            new StringReader(
                "function main() {\n" +
                "  document.getElementById('base').setInnerHTML(\n" +
                "       test('foo.html?a=b&c=d', '\"hover text\"',\n" +
                "            ['class1', 'class2']));\n" +
                "}"),
            "file:///attr-test.js")
        );
  }

  public void testCall() throws Exception {
    execPlugin(
        "\n  var div = document.createElement('DIV');" +
        "\n  div.id = 'pre-base';" +
        "\n  document.body.appendChild(div);" +
        "\n" +
        "\n  testOuters.main();" +
        "\n  assertEquals(" +
        "\n      '(1,2)(1,2)'," +
        "\n      document.getElementById('pre-base').innerHTML" +
        "\n      );",
        new PluginFile(
            new StringReader(
                "<gxp:template name=\"callerTemplate\">\n" +
                "  <call:calleeTemplate x=\"1\" y=\"2\"/>\n" +
                // with parameter order reversed
                "  <call:calleeTemplate y=\"2\" x=\"1\"/>\n" +
                "</gxp:template>"),
            "file:///call-test-1.gxp"),
        new PluginFile(
            new StringReader(
                "<gxp:template name=\"calleeTemplate\">\n" +
                "  <gxp:param name=\"x\"/>\n" +
                "  <gxp:param name=\"y\"/>\n" +
                "  (<gxp:eval expr=\"x\"/>, <gxp:eval expr=\"y\"/>)\n" +
                "</gxp:template>"),
            "file:///call-test-2.gxp"),
        new PluginFile(
            new StringReader(
                "function main() {\n" +
                "  document.getElementById('base')\n" +
                "      .setInnerHTML(callerTemplate());\n" +
                "}"),
            "file:///call-test.js")
        );
  }

  public void testCss() throws Exception {
    execPlugin(
        "\n  var div = document.createElement('DIV');" +
        "\n  div.id = 'pre-base';" +
        "\n  document.body.appendChild(div);" +
        "\n" +
        "\n  testOuters.main();" +
        "\n  assertEquals(" +
        "\n      'left: 25px;\\nwidth: 30px;\\nmargin: 5px 5px 5px 5px;\\n' +" +
        "\n      'color: #a0b0c0'," +
        "\n      document.getElementById('pre-base').style.cssText" +
        "\n      );",
        new PluginFile(
            new StringReader(
                "    @template('styles');" +
                "\n  @param('left');" +
                "\n  @param('right');" +
                "\n  @param('margin');" +
                "\n  @param('light');" +
                "\n  @param('dark');" +
                "\n" +
                "\n  left: $(left + margin)px;" +
                "\n  width: $(right - left - 2 * margin)px;" +
                "\n  margin: $(margin)px $(margin)px $(margin)px $(margin)px;" +
                "\n  color: $((light + dark) / 2);"  // simple interpolation
                ),
            "file:///css-test-1.css"),
        new PluginFile(
            new StringReader(
                "function main() {\n" +
                "  document.getElementById('base').setStyle(\n" +
                "      styles(20, 60, 5, 0x8090a0, 0xc0d0e0));\n" +
                "}"),
            "file:///css-test.js")
        );
  }

  /**
   * compiles a plugin, and runs it.  Since Rhino does not have a window
   * context, we fake one.
   */
  private void execPlugin(String tests, PluginFile... pluginFiles)
      throws IOException, ParseException {
    PluginMeta meta = new PluginMeta(
        "pre", PluginEnvironment.CLOSED_PLUGIN_ENVIRONMENT);
    MessageContext mc = new MessageContext();
    MessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(new OutputStreamWriter(System.err)), mc);
    PluginCompiler pc = new PluginCompiler(meta, mq);

    pc.setMessageContext(mc);

    List<InputSource> srcs = new ArrayList<InputSource>();
    for (PluginFile pluginFile : pluginFiles) {
      InputSource is = new InputSource(URI.create(pluginFile.source));
      srcs.add(is);
      CharProducer cp = CharProducer.Factory.create(pluginFile.input, is);
      try {
        ParseTreeNode input = PluginCompilerMain.parseInput(is, cp, mq);
        pc.addInput(new AncestorChain<ParseTreeNode>(input));
      } finally {
        cp.close();
      }
    }

    boolean success = pc.run();
    if (!success) {
      StringBuilder sb = new StringBuilder();
      sb.append("Failed to compile plugin");
      for (Message msg : mq.getMessages()) {
        sb.append("\n");
        msg.format(mc, sb);
      }
      fail(sb.toString());
    }

    StringBuilder buf = new StringBuilder();
    RenderContext rc = new RenderContext(mc, buf);
    for (ParseTreeNode output : pc.getOutputs()) {
      if (output instanceof CssTree) { continue; }
      output.render(rc);
      rc.newLine();
      rc.newLine();
    }

    String htmlStubUrl = TestUtil.makeContentUrl("<html><head/><body/></html>");

    String compiledPlugin = buf.toString();

    RhinoTestBed.Input[] inputs = new RhinoTestBed.Input[] {
        // Browser Stubs
        new RhinoTestBed.Input(getClass(), "/js/jqueryjs/runtest/env.js"),
        // Console Stubs
        new RhinoTestBed.Input(getClass(), "console-stubs.js"),
        // Plugin Framework
        new RhinoTestBed.Input(getClass(), "/com/google/caja/caja.js"),
        new RhinoTestBed.Input(getClass(), "caps/wrap_capability.js"),
        new RhinoTestBed.Input(getClass(), "plugin-base.js"),
        // Initialize the DOM
        new RhinoTestBed.Input(
            // Document not defined until window.location set.
            new StringReader("location = '" + htmlStubUrl + "';\n"),
            "dom"),
        // Make the assertTrue, etc. functions available to javascript
        new RhinoTestBed.Input(getClass(), "asserts.js"),
        new RhinoTestBed.Input(new StringReader(
            "var testOuters = ___.getNewModuleHandler().getOuters();\n"
            + "initPlugin(testOuters, 'pre', 'rootDiv', '/plugin1');"),
            "container"),
        // The Plugin
        new RhinoTestBed.Input(
            new StringReader(compiledPlugin), getName() + "-plugin.js"),
        // The tests
        new RhinoTestBed.Input(
            new StringReader(tests), getName() + "-tests.js"),
        };

    for (Message msg : mq.getMessages()) {
      rc.newLine();
      buf.append(msg.getMessageType().toString()).append(" : ")
         .append(msg.getMessageParts().get(0));
    }

    System.err.println(buf.toString());

    RhinoTestBed.runJs(null, inputs);
  }

  private static class PluginFile {
    public final Reader input;
    public final String source;
    public PluginFile(String resource) throws IOException {
      this.source = resource;
      this.input = new InputStreamReader(
          RhinoTestBed.Input.class.getResourceAsStream(resource), "UTF-8");
    }
    /** @param source file path or url from which the javascript came. */
    public PluginFile(Reader input, String source) {
      this.input = input;
      this.source = source;
    }
  }
}
