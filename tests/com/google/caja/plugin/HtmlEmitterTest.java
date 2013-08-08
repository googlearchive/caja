// Copyright (C) 2009 Google Inc.
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

import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.Statement;
import com.google.caja.plugin.stages.JobCache;
import com.google.caja.plugin.templates.IhtmlRoot;
import com.google.caja.plugin.templates.SafeHtmlChunk;
import com.google.caja.plugin.templates.SafeJsChunk;
import com.google.caja.plugin.templates.ScriptPlaceholder;
import com.google.caja.plugin.templates.TemplateCompiler;
import com.google.caja.plugin.templates.ValidatedStylesheet;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.ContentType;
import com.google.caja.util.Lists;
import com.google.caja.util.MoreAsserts;
import com.google.caja.util.Pair;
import com.google.caja.util.RhinoTestBed;
import com.google.caja.util.Strings;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

public class HtmlEmitterTest extends CajaTestCase {
  public final void testJavascript() throws Exception {
    RhinoTestBed.runJsUnittestFromHtml(
        html(fromResource("html-emitter-test.html")));
  }

  public final void testCompiler() throws Exception {
    // Test that the input HTML produces the series of calls tested in
    // html-emitter-test.html.
    String input = (
        ""
        + "<p>Hi</p>\n"
        + "<div><script>a()</script>Hello <script>b()</script>World!!!</div>\n"
        + "<h1>Foo <b><script>c()</script>Bar</b> Baz</h1>\n"
        + "<h2 id='x'>Boo</h2>\n");
    PluginMeta meta = new PluginMeta();
    List<ScriptPlaceholder> extractedScripts = Lists.newArrayList();
    TemplateCompiler tc = new TemplateCompiler(
        Collections.singletonList(new IhtmlRoot(
            new JobEnvelope(
                null, JobCache.none(), ContentType.HTML, false, null),
            htmlWithScriptsExtracted(input, extractedScripts), is.getUri())),
        Collections.<ValidatedStylesheet>emptyList(), extractedScripts,
        CssSchema.getDefaultCss21Schema(mq), HtmlSchema.getDefault(mq),
        meta, mc, mq);
    Pair<List<SafeHtmlChunk>, List<SafeJsChunk>> htmlAndJs = tc.getSafeHtml(
        DomParser.makeDocument(null, null));

    assertEquals(
        ""
        + "<p>Hi</p>"
        + "<div id=\"id_1___\">Hello <span id=\"id_2___\"></span>World!!!</div>"
        + "<h1>Foo <b id=\"id_3___\">Bar</b> Baz</h1>"
        + "<h2 id=\"id_4___\">Boo</h2>",
        renderAll(htmlAndJs.a));
    // If you change this JS, also update the tests in html-emitter-test.html.
    List<String> jsLines =  Arrays.asList(
        "/* Start translated code */",
        "throw 'Translated code must never be executed';",
        "{",
        "  var el___;",
        "  var emitter___ = IMPORTS___.htmlEmitter___;",
        "  el___ = emitter___.byId('id_1___');",
        "  emitter___.attach('id_1___');",
        "  emitter___.rmAttr(el___, 'id');",
        "} /* End translated code */",
        "try {",
        "  { a(); }",
        "} catch (ex___) {",
        "  ___.getNewModuleHandler().handleUncaughtException(ex___, onerror,",
        "    'testCompiler', '1');",
        "} /* Start translated code */",
        "throw 'Translated code must never be executed';",
        "{",
        "  var el___;",
        "  var emitter___ = IMPORTS___.htmlEmitter___;",
        "  emitter___.discard(emitter___.attach('id_2___'));",
        "} /* End translated code */",
        "try {",
        "  { b(); }",
        "} catch (ex___) {",
        "  ___.getNewModuleHandler().handleUncaughtException(ex___, onerror,",
        "    'testCompiler', '1');",
        "} /* Start translated code */",
        "throw 'Translated code must never be executed';",
        "{",
        "  var el___;",
        "  var emitter___ = IMPORTS___.htmlEmitter___;",
        "  el___ = emitter___.byId('id_3___');",
        "  emitter___.attach('id_3___');",
        "  emitter___.rmAttr(el___, 'id');",
        "} /* End translated code */",
        "try {",
        "  { c(); }",
        "} catch (ex___) {",
        "  ___.getNewModuleHandler().handleUncaughtException(ex___, onerror,",
        "    'testCompiler', '1');",
        "} /* Start translated code */",
        "throw 'Translated code must never be executed';",
        "{",
        "  var el___;",
        "  var emitter___ = IMPORTS___.htmlEmitter___;",
        "  el___ = emitter___.byId('id_4___');",
        "  emitter___.setAttr(el___, 'id', 'x-' + IMPORTS___.getIdClass___());",
        "  el___ = emitter___.finish();",
        "  emitter___.signalLoaded();",
        "}",
        "/* End translated code */"
        );
    MoreAsserts.assertListsEqual(
        jsLines, Arrays.asList(renderConsolidated(htmlAndJs.b).split("\n")));
  }

  private DocumentFragment htmlWithScriptsExtracted(
      String html, List<ScriptPlaceholder> extractedScripts)
      throws ParseException {
    return extract(htmlFragment(fromString(html)), extractedScripts);
  }

  private <N extends Node> N extract(
      N n, List<ScriptPlaceholder> extractedScripts)
      throws ParseException {
    if (n.getNodeType() == 1
        && Strings.eqIgnoreCase("script", n.getNodeName())) {
      String id = "$" + extractedScripts.size();
      // According to nodeType, n is an Element, and Placeholder.make returns
      // an Element, so this will succeed provided that N is not a proper
      // subtype of Element.
      @SuppressWarnings("unchecked")
      N placeholder = (N)Placeholder.make(n, id);
      extractedScripts.add(new ScriptPlaceholder(
          new JobEnvelope(id, JobCache.none(), ContentType.JS, false, null),
          js(fromString(n.getFirstChild().getNodeValue()))));
      n.getParentNode().replaceChild(placeholder, n);
      n = placeholder;
    }
    for (Node c : Nodes.childrenOf(n)) { extract(c, extractedScripts); }
    return n;
  }

  private static String renderConsolidated(List<SafeJsChunk> blocks) {
    List<Statement> statements = Lists.newArrayList();
    for (SafeJsChunk js : blocks) {
      for (Statement s : ((Block) js.body).children()) {
        if (s instanceof Noop) { continue; }
        if (s instanceof Block) {
          statements.addAll(((Block) s).children());
        } else {
          statements.add(s);
        }
      }
    }
    StringBuilder sb = new StringBuilder();
    RenderContext rc = new RenderContext(
        new JsPrettyPrinter(new Concatenator(sb)));
    for (Statement s : statements) {
      s.render(rc);
      if (!s.isTerminal()) { rc.getOut().consume(";"); }
    }
    rc.getOut().noMoreTokens();
    return sb.toString();
  }

  private static String renderAll(Iterable<? extends SafeHtmlChunk> html) {
    StringBuilder sb = new StringBuilder();
    for (SafeHtmlChunk chunk : html) {
      sb.append(Nodes.render(chunk.root));
    }
    return sb.toString();
  }
}
