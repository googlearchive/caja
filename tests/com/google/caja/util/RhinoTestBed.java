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

package com.google.caja.util;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.parser.js.UseSubsetDirective;
import com.google.caja.parser.quasiliteral.CajitaRewriter;
import com.google.caja.parser.quasiliteral.DefaultValijaRewriter;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.TestBuildInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * A testbed that allows running javascript via the Rhino interpreter.
 * TODO(mikesamuel): maybe replace this with the JSR 223 stuff.
 *
 * @author mikesamuel@gmail.com
 */
public class RhinoTestBed {

  /**
   * Runs the javascript from the given inputs in order, and returns the
   * result.
   */
  public static Object runJs(Input... inputs) throws IOException {
    Context context = ContextFactory.getGlobal().enterContext();
    // Don't bother to compile tests to a class file.  Removing this causes
    // a 5x slow-down in Rhino-heavy tests.
    context.setOptimizationLevel(-1);
    try {
      ScriptableObject globalScope = context.initStandardObjects();
      Object stderr = Context.javaToJS(System.err, globalScope);
      ScriptableObject.putProperty(globalScope, "stderr", stderr);
      Object result = null;

      for (Input input : inputs) {
        String js = readReader(input.input);
        input.input.close();
        try {
          result = context.evaluateReader(
              globalScope, new StringReader(js), input.source, 1, null);
        } catch (RhinoException e) {
          if (e.getCause() instanceof AssertionFailedError) {
            throw (AssertionFailedError) e.getCause();
          }
          System.err.println(input.source + ": [[[");
          System.err.println(js);
          System.err.println("]]]");
          Assert.fail(e.details() + "\n" + e.getScriptStackTrace());
          return null;
        }
      }
      return result;
    } finally {
      Context.exit();
    }
  }

  /**
   * Given an HTML file that references javascript sources, load all
   * the scripts, set up the DOM using env.js, and start JSUnit.
   *
   * This lets us write test html files that can be run both
   * in a browser, and automatically via ANT.
   *
   * @param html an HTML DOM tree to run in Rhino.
   */
  public static void runJsUnittestFromHtml(Element html)
      throws IOException, ParseException {
    TestUtil.enableContentUrls();  // Used to get HTML to env.js
    List<Input> inputs = new ArrayList<Input>();

    // Stub out the Browser
    inputs.add(new Input(RhinoTestBed.class, "../plugin/console-stubs.js"));
    inputs.add(new Input(RhinoTestBed.class, "/js/jqueryjs/runtest/env.js"));
    int injectHtmlIndex = inputs.size();

    List<Pair<String, InputSource>> scriptContent
        = new ArrayList<Pair<String, InputSource>>();
    MessageContext mc = new MessageContext();
    MessageQueue mq = new EchoingMessageQueue(new PrintWriter(System.err), mc);

    List<Element> scripts = new ArrayList<Element>();
    for (Element script : Nodes.nodeListIterable(
             html.getElementsByTagName("script"), Element.class)) {
      scripts.add(script);
    }
    for (Element script : scripts) {
      Attr src = script.getAttributeNode("src");
      CharProducer scriptBody;
      if (src != null) {
        String resourcePath = src.getNodeValue();
        InputSource resource;
        if (resourcePath.startsWith("/")) {
          try {
            resource = new InputSource(
                RhinoTestBed.class.getResource(resourcePath).toURI());
          } catch (URISyntaxException ex) {
            throw new RuntimeException(
                "java.net.URL is not a valid java.net.URI", ex);
          }
        } else {
          InputSource baseUri = Nodes.getFilePositionFor(html).source();
          resource = new InputSource(baseUri.getUri().resolve(resourcePath));
        }
        scriptBody = loadResource(resource);
      } else {
        scriptBody = textContentOf(script);
      }
      String scriptText;
      Block js = parseJavascript(scriptBody, mq);
      if (hasUseSubsetDirective(js, "cajita")) {
        scriptText = render(cajoleCajita(js, mq));
      } else if (hasUseSubsetDirective(js, "valija")) {
        scriptText = render(cajoleValija(js, mq));
      } else {
        // Add blank lines at the front so that Rhino stack traces have correct
        // line numbers.
        scriptText = prefixWithBlankLines(
          scriptBody.toString(0, scriptBody.getLimit()),
          Nodes.getFilePositionFor(script).startLineNo() - 1);
      }
      scriptContent.add(Pair.pair(scriptText, js.getFilePosition().source()));
      mc.addInputSource(js.getFilePosition().source());
      script.getParentNode().removeChild(script);
    }
    for (Pair<String, InputSource> script : scriptContent) {
      inputs.add(new Input(script.a, mc.abbreviate(script.b)));
    }

    // Set up the DOM.  env.js requires that location be set to a URI before it
    // creates a DOM.  Since it fetches HTML via java.net.URL and passes it off
    // to the org.w3c parser, we use a content: URL which is handled by handlers
    // registered in TestUtil so that we can provide html without having a file
    // handy.
    String domJs = "window.location = "
        + StringLiteral.toQuotedValue(
            TestUtil.makeContentUrl(Nodes.render(html)))
        + ";";
    String htmlSource = Nodes.getFilePositionFor(html).source().toString();
    inputs.add(injectHtmlIndex, new Input(domJs, htmlSource));
    inputs.add(new Input(
        "(function () {\n"
        + "   var onload = document.body.getAttribute('onload');\n"
        + "   onload && eval(onload);\n"
        + " })();", htmlSource));

    // Execute for side-effect
    runJs(inputs.toArray(new Input[inputs.size()]));
  }

  /** An input javascript file. */
  public static final class Input {
    public final Reader input;
    public final String source;
    public Input(Class<?> base, String resource) throws IOException {
      this.source = resource;
      InputStream instream = TestUtil.getResourceAsStream(base, resource);
      this.input = new InputStreamReader(instream , "UTF-8");
    }
    /** @param source file path or url from which the javascript came. */
    public Input(Reader input, String source) {
      this.input = input;
      this.source = source;
    }

    public Input(String javascript, String source) {
      this(new StringReader(javascript), source);
    }

    @Override
    public String toString() { return "(InputSource " + source + ")"; }
  }

  private static String readReader(Reader reader) {
    Reader r;
    r = new BufferedReader(reader);
    Writer w = new StringWriter();

    try {
      for (int c; (c = r.read()) != -1; ) w.write(c);
    } catch (IOException e)  {
      throw new RuntimeException(e);
    }

    try {
      r.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return w.toString();
  }

  private static ParseTreeNode cajoleCajita(Block program, MessageQueue mq) {
    CajitaRewriter rw = new CajitaRewriter(new TestBuildInfo(), false);
    return rw.expand(new UncajoledModule(program), mq);
  }

  private static ParseTreeNode cajoleValija(Block program, MessageQueue mq) {
    DefaultValijaRewriter vrw = new DefaultValijaRewriter(false);
    CajitaRewriter crw = new CajitaRewriter(new TestBuildInfo(), false);
    return crw.expand(vrw.expand(new UncajoledModule(program), mq), mq);
  }

  private static String render(ParseTreeNode n) {
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = n.makeRenderer(sb, null);
    n.render(new RenderContext(new MessageContext(), tc).withAsXml(true));
    tc.noMoreTokens();
    return sb.toString();
  }

  private static boolean hasUseSubsetDirective(Block block, String subsetName) {
    if (block.children().isEmpty()) { return false; }
    Statement first = block.children().get(0);
    if (!(first instanceof UseSubsetDirective)) { return false; }
    UseSubsetDirective usd = (UseSubsetDirective) first;
    return usd.getSubsetNames().contains(subsetName);
  }

  private static Block parseJavascript(CharProducer cp, MessageQueue mq)
      throws ParseException {
    JsLexer lexer = new JsLexer(cp, false);
    Parser p = new Parser(
        new JsTokenQueue(lexer, cp.getSourceBreaks(0).source()), mq, false);
    return p.parse();
  }

  private static CharProducer loadResource(InputSource resource)
      throws IOException {
    File f = new File(resource.getUri());
    return CharProducer.Factory.create(
        new InputStreamReader(new FileInputStream(f), "UTF-8"), resource);
  }

  private static String prefixWithBlankLines(String s, int n) {
    if (n <= 0) { return s; }
    StringBuilder sb = new StringBuilder(s.length() + n);
    while (--n >= 0) { sb.append('\n'); }
    return sb.append(s).toString();
  }

  private static CharProducer textContentOf(Element script) {
    List<CharProducer> parts = new ArrayList<CharProducer>();
    for (Node child : Nodes.childrenOf(script)) {
      FilePosition childPos = Nodes.getFilePositionFor(child);
      switch (child.getNodeType()) {
        case Node.TEXT_NODE:
          String rawText = Nodes.getRawText((Text) child);
          String decodedText = child.getNodeValue();
          CharProducer cp = null;
          if (rawText != null) {
            cp = CharProducer.Factory.fromHtmlAttribute(
                CharProducer.Factory.create(
                    new StringReader(rawText), childPos));
            if (!String.valueOf(cp.getBuffer(), cp.getOffset(), cp.getLength())
                .equals(decodedText)) {  // XHTML
              cp = null;
            }
          }
          if (cp == null) {
            cp = CharProducer.Factory.create(
                new StringReader(child.getNodeValue()), childPos);
          }
          parts.add(cp);
          break;
        case Node.CDATA_SECTION_NODE:
          parts.add(CharProducer.Factory.create(
              new StringReader("         " + child.getNodeValue()), childPos));
          break;
        default: break;
      }
    }
    return CharProducer.Factory.chain(parts.toArray(new CharProducer[0]));
  }

  private RhinoTestBed() { /* uninstantiable */ }
}
