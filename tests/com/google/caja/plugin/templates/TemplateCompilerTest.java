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

package com.google.caja.plugin.templates;

import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.Dom;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.TranslatedCode;
import com.google.caja.plugin.CssDynamicExpressionRewriter;
import com.google.caja.plugin.CssRewriter;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.JobEnvelope;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.LoaderType;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.UriEffect;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.plugin.UriPolicy;
import com.google.caja.plugin.UriPolicyHintKey;
import com.google.caja.plugin.stages.RewriteHtmlStage;
import com.google.caja.plugin.stages.StubJobCache;
import com.google.caja.reporting.MarkupRenderMode;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Lists;
import com.google.caja.util.Pair;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

public class TemplateCompilerTest extends CajaTestCase {
  private PluginMeta meta;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    meta = new PluginMeta(UriFetcher.NULL_NETWORK, UriPolicy.IDENTITY);
  }

  public final void testEmptyModule() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("")),
        htmlFragment(fromString("")),
        new Block());
  }

  public final void testTopLevelTextNodes() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("Hello, <b title=howdy>World</b>!!!")),
        htmlFragment(fromString("Hello, <b title=howdy>World</b>!!!")),
        new Block());
  }

  public final void testSafeHtmlWithDynamicModuleId() throws Exception {
    assertSafeHtml(
        htmlFragment(fromResource("template-compiler-input1.html", is)),
        htmlFragment(fromResource("template-compiler-golden1-dynamic.html")),
        js(fromResource("template-compiler-golden1-dynamic.js")));
  }

  public final void testSafeHtmlWithNullUriPolicy() throws Exception {
    // Null URI policy also implies dynamic module ID
    meta = new PluginMeta(UriFetcher.NULL_NETWORK, null);
    assertSafeHtml(
        htmlFragment(fromResource("template-compiler-input1.html", is)),
        htmlFragment(fromResource("template-compiler-golden1-nulluripol.html")),
        js(fromResource("template-compiler-golden1-nulluripol.js")));
  }

  public final void testSafeHtmlWithStaticModuleId() throws Exception {
    meta.setIdClass("xyz___");

    assertSafeHtml(
        htmlFragment(fromResource("template-compiler-input1.html", is)),
        htmlFragment(fromResource("template-compiler-golden1-static.html")),
        js(fromResource("template-compiler-golden1-static.js")));
  }

  public final void testSignalLoadedAtEnd() throws Exception {
    // Ensure that, although finish() is called as soon as possible after the
    // last HTML is rolled forward, signalLoaded() is called at the very end,
    // after all scripts are encountered.
    assertSafeHtml(
        htmlFragment(fromString(
            ""
            + "<p id=\"a\">a</p>"
            + "<script type=\"text/javascript\" defer=defer>1;</script>")),
        htmlFragment(fromString(
            "<p id=\"id_2___\">a</p>")),
        js(fromString(
            ""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_2___');"
            + "    emitter___.setAttr("
            + "        el___, 'id', 'a-' + IMPORTS___.getIdClass___());"
            + "    el___ = emitter___.finish();"
            + "  }"
            + "}"
            + "function module() {"
            + "  try {"
            + "    { 1; }"
            + "  } catch (ex___) {"
            + "    ___.getNewModuleHandler().handleUncaughtException(ex___,"
            + "        onerror, 'testSignalLoadedAtEnd', '1');"
            + "  }"
            + "}"
            + "function module() {"
            + "  {"
            + "    IMPORTS___.htmlEmitter___.signalLoaded();"
            + "  }"
            + "}")));
  }

  public final void testTargetsRewritten() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("<a href='foo' target='foo'>hello</a>")),
        htmlFragment(fromString("<a href='foo' id=\"id_1___\" target='_blank'>hello</a>")),
        js(fromString(""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_1___');"
            + "    emitter___.setAttr("
            + "        el___,"
            + "        'target',"
            + "        IMPORTS___.rewriteTargetAttribute___("
            + "            'foo', 'a', 'target'));"
            + "    emitter___.rmAttr(el___, 'id');"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")));
  }

  public final void testFormRewritten() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("<form></form>")),
        htmlFragment(fromString(
            "<form action='http://example.org/testFormRewritten'"
            + " autocomplete='off' id=\"id_1___\" target='_blank'></form>")),
        js(fromString(""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_1___');"
            + "    emitter___.setAttr("
            + "        el___,"
            + "        'target',"
            + "        IMPORTS___.rewriteTargetAttribute___("
            + "            null, 'form', 'target'));"
            + "    emitter___.rmAttr(el___, 'id');"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")));
  }

  public final void testNamesRewritten() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("<a name='hi'></a>")),
        htmlFragment(fromString("<a id='id_1___' target='_blank'></a>")),
        js(fromString(""
            + "function module() {"
            + "  {"
            + "    var el___; var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_1___');"
            + "    emitter___.setAttr("
            + "        el___, 'name', 'hi-' + IMPORTS___.getIdClass___());"
            + "    emitter___.setAttr("
            + "        el___,"
            + "        'target',"
            + "        IMPORTS___.rewriteTargetAttribute___("
            + "            null, 'a', 'target'));"
            + "    emitter___.rmAttr(el___, 'id');"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")));

    meta.setIdClass("xyz___");
    assertSafeHtml(
        htmlFragment(fromString("<a name='hi'></a>")),
        htmlFragment(fromString(
            "<a name='hi-xyz___' id=\"id_2___\" target='_blank'></a>")),
        js(fromString(""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_2___');"
            + "    emitter___.setAttr("
            + "        el___,"
            + "        'target',"
            + "        IMPORTS___.rewriteTargetAttribute___("
            + "            null, 'a', 'target'));"
            + "    emitter___.rmAttr(el___, 'id');"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")));
  }

  public final void testSanityCheck() throws Exception {
    // The name attribute is not allowed on <p> elements, so
    // normally it would have been stripped out by the TemplateSanitizer pass.
    // Under no circumstances should it be emitted.
    assertSafeHtml(
        htmlFragment(fromString("<p name='hi'>Howdy</p>")),
        htmlFragment(fromString("<p>Howdy</p>")),
        new Block());
  }

  public final void testFormName() throws Exception {
    meta.setIdClass("suffix___");
    assertSafeHtml(
        htmlFragment(fromString("<form name='hi'></form>")),
        htmlFragment(fromString(
            "<form action='http://example.org/testFormName' autocomplete='off'"
            + " name='hi-suffix___' id=\"id_1___\" target=_blank></form>")),
        js(fromString(""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_1___');"
            + "    emitter___.setAttr("
            + "        el___,"
            + "        'target',"
            + "        IMPORTS___.rewriteTargetAttribute___("
            + "            null, 'form', 'target'));"
            + "    emitter___.rmAttr(el___, 'id');"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")));
  }

  // See bug 722
  public final void testFormOnSubmitTrue() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString(
            "<form onsubmit='alert(&quot;hi&quot;); return true;'></form>")),
        htmlFragment(fromString(
            "<form action='http://example.org/testFormOnSubmitTrue'"
            + " autocomplete='off' id=id_2___ target='_blank'></form>")),
        js(fromString(
            ""
            + "function module() {"
            + "  {"
            + "    var el___; var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_2___');"
            // The extracted handler.
            + "    var c_1___ = ___.markFuncFreeze("
            + "        function(event, thisNode___) {"
            + "          alert('hi');"  // Cajoled later
            + "          return true;"
            + "        });"
            + "    el___.onsubmit = function (event) {"
            + "      return ___.plugin_dispatchEvent___("
            + "          this, event, ___.getId(IMPORTS___), c_1___, 2);"
            + "    };"
            + "    emitter___.setAttr("
            + "        el___,"
            + "        'target',"
            + "        IMPORTS___.rewriteTargetAttribute___("
            + "            null, 'form', 'target'));"
            + "    emitter___.rmAttr(el___, 'id');"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")));
  }

  public final void testJavascriptUrl() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString(
            "<a href='javascript:alert(1+1)'>Two!!</a>")),
        htmlFragment(fromString(
            "<a id=\"id_2___\" target=\"_blank\">Two!!</a>")),
        js(fromString(
            ""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_2___');"
            + "    var c_1___ = IMPORTS___.handlers___.push(___.markFuncFreeze("
            // body is cajoled later
            + "        function() { alert(1 + 1); })) - 1;"
            // The extracted handler.
            + "    emitter___.setAttr(el___, 'href', 'javascript:'"
            + "      + encodeURIComponent("
            + "          'try{void ___.plugin_dispatchToHandler___('"
            + "          + ___.getId(IMPORTS___) + ',' + c_1___"
            + "          + ',[{}])}catch(_){}'));"
            + "    emitter___.setAttr("
            + "        el___,"
            + "        'target',"
            + "        IMPORTS___.rewriteTargetAttribute___("
            + "            null, 'a', 'target'));"
            + "    emitter___.rmAttr(el___, 'id');"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")));
  }

  public final void testJavascriptUrlWithUseStrict() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString(
            "<a href='javascript:%22use%20strict%22;alert(1+1)'>Two!!</a>")),
        htmlFragment(fromString(
            "<a id=\"id_2___\" target=\"_blank\">Two!!</a>")),
        js(fromString(
            ""
            + "function module() {"
            + "  {"
            + "    var el___; var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_2___');"
            // The extracted handler.
            + "    var c_1___ = IMPORTS___.handlers___.push(___.markFuncFreeze("
            + "        function () {"
            + "          'use strict';"
            + "          alert(1 + 1);"  // Cajoled later
            + "        })) - 1;"
            + "    emitter___.setAttr(el___, 'href', 'javascript:'"
            + "      + encodeURIComponent("
            + "          'try{void ___.plugin_dispatchToHandler___('"
            + "          + ___.getId(IMPORTS___) + ',' + c_1___"
            + "          + ',[{}])}catch(_){}'));"
            + "    emitter___.setAttr("
            + "        el___,"
            + "        'target',"
            + "        IMPORTS___.rewriteTargetAttribute___("
            + "            null, 'a', 'target'));"
            + "    emitter___.rmAttr(el___, 'id');"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")));
  }

  // See bug 722
  public final void testFormOnSubmitEmpty() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("<form onsubmit=''></form>")),
        htmlFragment(fromString(
            "<form action='http://example.org/testFormOnSubmitEmpty'"
            + " autocomplete='off' id=\"id_1___\" target='_blank'></form>")),
        js(fromString(""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_1___');"
            + "    emitter___.setAttr("
            + "        el___,"
            + "        'target',"
            + "        IMPORTS___.rewriteTargetAttribute___("
            + "            null, 'form', 'target'));"
            + "    emitter___.rmAttr(el___, 'id');"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")));
  }

  public final void testImageSrc() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("<img src='blank.gif' width='20'/>")),
        htmlFragment(fromString("<img src='blank.gif' width='20'/>")),
        new Block());
  }

  public final void testStyleRewriting() throws Exception {
    InputSource is = new InputSource(URI.create("http://example.org/"));
    assertSafeHtml(
        htmlFragment(fromString(
            "<div style=\"position: absolute; background: "
            + "url('bg-image')\">\n"
            + "Hello\n"
            + "</div>\n", is)),
        htmlFragment(fromString(
            "<div style=\"position: absolute; background: "
            + "url('http://example.org/bg-image')"
            + "\">\nHello\n</div>")),
        new Block());
  }

  public final void testEmptyStyleRewriting() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("<div style=>\nHello\n</div>\n")),
        htmlFragment(fromString("<div>\nHello\n</div>")),
        new Block());
    assertSafeHtml(
        htmlFragment(fromString("<div style=''>\nHello\n</div>\n")),
        htmlFragment(fromString("<div>\nHello\n</div>")),
        new Block());
  }

  public final void testEmptyScriptRewriting() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("<div onclick=''>\nHello\n</div>\n")),
        htmlFragment(fromString("<div>\nHello\n</div>")),
        new Block());
  }

  public final void testAsyncScripts() throws Exception {
    // If all the scripts are deferred, there is never any need to detach any
    // of the DOM tree.
    assertSafeHtml(
        htmlFragment(fromString("Hi<script async>alert('howdy');</script>\n")),
        htmlFragment(fromString("Hi")),
        js(fromString(
            ""
            + "function module() {"
            + "  try {"
            + "    { alert('howdy'); }"
            + "  } catch (ex___) {"
            + "    ___.getNewModuleHandler().handleUncaughtException("
            + "        ex___, onerror, 'testAsyncScripts', '1');"
            + "  }"
            + "}"
            + "function module() {"
            + "  {"
            + "    IMPORTS___.htmlEmitter___.signalLoaded();"
            + "  }"
            + "}"))
        );
  }

  public final void testMailto() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString(
            "<a href='mailto:x@y' target='_blank'>z</a>")),
        htmlFragment(fromString(
            "<a href='mailto:x%40y' id='id_1___' target='_blank'>z</a>")),
        js(fromString(""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_1___');"
            + "    emitter___.setAttr("
            + "        el___,"
            + "        'target',"
            + "        IMPORTS___.rewriteTargetAttribute___("
            + "            '_blank', 'a', 'target'));"
            + "    emitter___.rmAttr(el___, 'id');"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")));
  }

  public final void testComplexUrl() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString(
            "<a href='http://b/c;_d=e?f=g&i=%26' target='_blank'>z</a>")),
        htmlFragment(fromString(
            "<a href='http://b/c%3b%5fd%3de?f=g&i=%26'"
            + " id='id_1___' target='_blank'>z</a>")),
        js(fromString(""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_1___');"
            + "    emitter___.setAttr("
            + "        el___,"
            + "        'target',"
            + "        IMPORTS___.rewriteTargetAttribute___("
            + "            '_blank', 'a', 'target'));"
            + "    emitter___.rmAttr(el___, 'id');"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")));
  }

  public final void testTextAreas() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString(
            ""
            + "<textarea>Howdy!</textarea>"
            + "<script>alert('Howdy yourself!');</script>"
            + "<textarea>Bye!</textarea>")),
        htmlFragment(fromString(
            ""
            // textareas can't contain nodes, so the span had better follow it
            // which leaves it in the same position according to the
            // depth-first-ordering ignoring end tags used by the HTML emitter.
            + "<textarea autocomplete=\"off\">Howdy!</textarea>"
            + "<span id=\"id_2___\"></span>"
            + "<textarea autocomplete=\"off\">Bye!</textarea>")),
        js(fromString(
            ""
            + "function module() {"
            + "  {"
            + "    var el___; var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    emitter___.discard(emitter___.attach('id_2___'));"
            + "  }"
            + "}"
            + "function module() {"
            + "  try {"
            + "    { alert('Howdy yourself!'); }"
            + "  }catch (ex___) {"
            + "    ___.getNewModuleHandler().handleUncaughtException("
            + "        ex___, onerror, 'testTextAreas', '1');"
            + "  }"
            + "}"
            + "function module() {"
            + "  {"
            + "    var el___; var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")));
  }

  private static final class Holder<T> { T value; }

  public final void testUriAttributeResolution() throws Exception {
    // Ensure that the TemplateCompiler calls its PluginEnvironment with the
    // correct information when it encounters a URI-valued HTML attribute.

    final Holder<ExternalReference> savedRef = new Holder<ExternalReference>();

    meta = new PluginMeta(
        UriFetcher.NULL_NETWORK,
        new UriPolicy() {
          public String rewriteUri(
              ExternalReference u, UriEffect effect, LoaderType loader,
              Map<String, ?> hints) {
            assertEquals("a::href", UriPolicyHintKey.XML_ATTR.valueFrom(hints));
            savedRef.value = u;
            return "rewritten";
          }
        });

    DocumentFragment htmlInput =
        htmlFragment(fromString("<a href=\"x.html\"></a>"));
    assertSafeHtml(
        htmlInput,
        htmlFragment(fromString(
            "<a href=\"rewritten\" id=\"id_1___\" target=\"_blank\"></a>")),
        js(fromString(""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_1___');"
            + "    emitter___.setAttr("
            + "        el___,"
            + "        'target',"
            + "        IMPORTS___.rewriteTargetAttribute___("
            + "            null, 'a', 'target'));"
            + "    emitter___.rmAttr(el___, 'id');"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")));

    // The ExternalReference reference position should contain the URI of the
    // source in which the HREF was seen.
    assertEquals(
        Nodes.getFilePositionFor(htmlInput).source(),
        savedRef.value.getReferencePosition().source());

    // The ExternalReference target URI should be the URI that was embedded in
    // the original source. The TemplateCompiler should not attempt to resolve
    // it; that is the job of the PluginEnvironment.
    assertEquals(
        new URI("x.html"),
        savedRef.value.getUri());
  }

  public final void testFinishCalledAtEnd() throws Exception {
    // bug 1050, sometimes finish() is misplaced
    // http://code.google.com/p/google-caja/issues/detail?id=1050
    assertSafeHtml(
        htmlFragment(fromString(
            ""
            + "<div id=\"a\"></div>"
            + "<div id=\"b\"></div>"
            + "<script defer>1</script>")),
        htmlFragment(fromString(
            ""
            + "<div id=\"id_2___\"></div>"
            + "<div id=\"id_3___\"></div>")),
        js(fromString(
            ""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_2___');"
            + "    emitter___.setAttr(el___, 'id',"
            + "      'a-' + IMPORTS___.getIdClass___());"
            + "    el___ = emitter___.byId('id_3___');"
            + "    emitter___.setAttr(el___, 'id',"
            + "      'b-' + IMPORTS___.getIdClass___());"
            + "    el___ = emitter___.finish();"
            + "  }"
            + "}"
            + "function module() {"
            + "  try {"
            + "    {"
            + "      1;"
            + "    }"
            + "  } catch (ex___) {"
            + "    ___.getNewModuleHandler().handleUncaughtException(ex___,"
            + "      onerror, 'testFinishCalledAtEnd', '1');"
            + "  }"
            + "}"
            + "function module() {"
            + "  {"
            + "    IMPORTS___.htmlEmitter___.signalLoaded();"
            + "  }"
            + "}"
            )));
  }

  /**
   * {@code <textarea>} without cols= was triggering an NPE due to buggy
   * handling of mandatory attributes.
   * <a href="http://code.google.com/p/google-caja/issues/detail?id=1056">1056
   * </a>
   */
  public final void testBareTextarea() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("<textarea></textarea>")),
        htmlFragment(fromString("<textarea autocomplete=\"off\"></textarea>")),
        new Block());
  }

  public final void testValidClassNames() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("<div class='$-.:;()[]='></div>")),
        htmlFragment(fromString("<div class='$-.:;()[]='></div>")),
        new Block());
    assertNoWarnings();

    assertSafeHtml(
        htmlFragment(fromString("<div class='!@{} ok__1'></div>")),
        htmlFragment(fromString("<div class='!@{} ok__1'></div>")),
        new Block());
    assertNoWarnings();
  }

  public final void testValidIdNames() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("<input name='tag[]'>")),
        htmlFragment(fromString("<input autocomplete='off' name='tag[]'>")),
        new Block());
    assertNoWarnings();

    assertSafeHtml(
        htmlFragment(fromString("<input name='form$location'>")),
        htmlFragment(fromString(
            "<input autocomplete='off' name='form$location'>")),
        new Block());
    assertNoWarnings();

    assertSafeHtml(
        htmlFragment(fromString("<input name='$-.:;()[]='>")),
        htmlFragment(fromString(
            "<input autocomplete='off' name='$-.:;()[]='>")),
        new Block());
    assertNoWarnings();

    assertSafeHtml(
        htmlFragment(fromString(
            "<div id='23skiddoo'></div>"
            + "<div id='8675309'></div>"
            + "<div id='$-.:;()[]='></div>")),
        htmlFragment(fromString(
            "<div id='id_1___'></div>"
            + "<div id='id_2___'></div>"
            + "<div id='id_3___'></div>")),
        js(fromString(
            ""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_1___');"
            + "    emitter___.setAttr(el___, 'id',"
            + "      '23skiddoo-' + IMPORTS___.getIdClass___());"
            + "    el___ = emitter___.byId('id_2___');"
            + "    emitter___.setAttr(el___, 'id',"
            + "      '8675309-' + IMPORTS___.getIdClass___());"
            + "    el___ = emitter___.byId('id_3___');"
            + "    emitter___.setAttr(el___, 'id',"
            + "      '$-.:;()[]=-' + IMPORTS___.getIdClass___());"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")));
    assertNoWarnings();
  }

  public final void testInvalidClassNames() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("<div class='ok bad__'></div>")),
        htmlFragment(fromString("<div></div>")),
        new Block());
    assertMessage(true, IhtmlMessageType.ILLEGAL_NAME, MessageLevel.WARNING,
        MessagePart.Factory.valueOf("bad__"));
    assertNoWarnings();

    assertSafeHtml(
        htmlFragment(fromString("<div class='ok bad__ '></div>")),
        htmlFragment(fromString("<div></div>")),
        new Block());
    assertMessage(true, IhtmlMessageType.ILLEGAL_NAME, MessageLevel.WARNING,
        MessagePart.Factory.valueOf("bad__"));
    assertNoWarnings();

    assertSafeHtml(
        htmlFragment(fromString("<div class='bad__ ok'></div>")),
        htmlFragment(fromString("<div></div>")),
        new Block());
    assertMessage(true, IhtmlMessageType.ILLEGAL_NAME, MessageLevel.WARNING,
        MessagePart.Factory.valueOf("bad__"));
    assertNoWarnings();
  }

  public final void testInvalidIdNames() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("<input id='bad1__' name='bad2__'>")),
        htmlFragment(fromString("<input autocomplete='off'>")),
        new Block());
    assertMessage(true, IhtmlMessageType.ILLEGAL_NAME, MessageLevel.WARNING,
        MessagePart.Factory.valueOf("bad1__"));
    assertMessage(true, IhtmlMessageType.ILLEGAL_NAME, MessageLevel.WARNING,
        MessagePart.Factory.valueOf("bad2__"));
    assertNoWarnings();

    assertSafeHtml(
        htmlFragment(fromString("<input id='bad1__ ' name='bad2__ '>")),
        htmlFragment(fromString("<input autocomplete='off'>")),
        new Block());
    assertMessage(true, IhtmlMessageType.ILLEGAL_NAME, MessageLevel.WARNING,
        MessagePart.Factory.valueOf("bad1__ "));
    assertMessage(true, IhtmlMessageType.ILLEGAL_NAME, MessageLevel.WARNING,
        MessagePart.Factory.valueOf("bad2__ "));
    assertNoWarnings();

    assertSafeHtml(
        htmlFragment(fromString("<input id='b__ c'>")),
        htmlFragment(fromString("<input autocomplete='off'>")),
        new Block(),
        false);
    assertMessage(true, IhtmlMessageType.ILLEGAL_NAME, MessageLevel.ERROR,
        MessagePart.Factory.valueOf("b__ c"));
    assertNoWarnings();

    assertSafeHtml(
       htmlFragment(fromString("<input name='d__ e'>")),
       htmlFragment(fromString("<input autocomplete='off'>")),
       new Block(),
       false);
    assertMessage(true, IhtmlMessageType.ILLEGAL_NAME, MessageLevel.ERROR,
        MessagePart.Factory.valueOf("d__ e"));
    assertNoWarnings();
  }

  public final void testIdRefsRewriting() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString(
            "<table><tr><td headers='a b'></td></tr></table>")),
        htmlFragment(fromString(
            "<table><tr><td id='id_1___'></td></tr></table>")),
        js(fromString(
            ""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_1___');"
            + "    emitter___.setAttr(el___, 'headers',"
            + "      'a-' + IMPORTS___.getIdClass___()"
            + "      + ' b-' + IMPORTS___.getIdClass___());"
            + "    emitter___.rmAttr(el___, 'id');"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")));
  }

  public final void testMultiDocs() throws Exception {
    assertSafeHtml(
        Arrays.asList(
            htmlFragment(fromString("Hello")),
            htmlFragment(fromString(", World!"))),
        htmlFragment(fromString("Hello, World!")),
        new Block());
  }

  public final void testUsemapSanitized() throws Exception {
    meta.setIdClass("suffix___");
    assertSafeHtml(
        htmlFragment(fromString(
            ""
            + "<map name=foo><area href=foo.html></map>"
            + "<img usemap=#foo src=pic.gif>")),
        htmlFragment(fromString(
            ""
            + "<map name='foo-suffix___'>"
            + "<area id=\"id_1___\" target=\"_blank\" href=\"foo.html\" />"
            + "</map>"
            + "<img usemap=#foo-suffix___ src=pic.gif>")),
        js(fromString(""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_1___');"
            + "    emitter___.setAttr("
            + "        el___,"
            + "        'target',"
            + "        IMPORTS___.rewriteTargetAttribute___("
            + "            null, 'area', 'target'));"
            + "    emitter___.rmAttr(el___, 'id');"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")));
  }

  public final void testBadUriFragments() throws Exception {
    meta.setIdClass("suffix___");
    assertSafeHtml(
        htmlFragment(fromString(
            ""
            + "<map name=foo><area href=foo.html></map>"
            + "<img usemap=foo src=foo.gif>"
            + "<img usemap=##foo src=bar.gif>")),
        htmlFragment(fromString(
            ""
            + "<map name='foo-suffix___'>"
            + "<area id=\"id_1___\" target=\"_blank\" href=\"foo.html\" />"
            + "</map>"
            + "<img src=foo.gif>"
            + "<img src=bar.gif>")),
        js(fromString(""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_1___');"
            + "    emitter___.setAttr("
            + "        el___,"
            + "        'target',"
            + "        IMPORTS___.rewriteTargetAttribute___("
            + "            null, 'area', 'target'));"
            + "    emitter___.rmAttr(el___, 'id');"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")), false);
  }

  public final void testSingleValueAttrs() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("<input type=\"text\">")),
        htmlFragment(fromString("<input autocomplete=\"off\" type=\"text\">")),
        new Block(), false);
    assertSafeHtml(
        htmlFragment(fromString("<input autocomplete=\"on\" type=\"text\">")),
        htmlFragment(fromString("<input autocomplete=\"off\" type=\"text\">")),
        new Block(), false);
  }

  public final void testXmlnsAttrs() throws Exception {
    // XMLNS attributes stripped out.
    assertSafeHtml(
        htmlFragment(fromString(
            "<div xmlns:os=\"http://ns.opensocial.org/2008/markup\"></div>")),
        htmlFragment(fromString("<div></div>")),
        new Block(), true);
  }

  public final void testStyleAttrsWithUriPolicy() throws Exception {
    UriPolicy policy = new UriPolicy() {
      @Override
      public String rewriteUri(
          ExternalReference u,
          UriEffect effect,
          LoaderType loader,
          Map<String, ?> hints) {
        return "" + u.getUri() + "/URIPOLICY";
      }
    };
    meta = new PluginMeta(
        meta.getUriFetcher(),
        policy,
        meta.getPrecajoleMap(),
        meta.getPrecajoleMinify());
    assertSafeHtml(
        htmlFragment(fromString(
            "<div style=\"background-image: url(a.jpg)\"></div>")),
        htmlFragment(fromString(
            "<div"
            + " style=\"background-image:"
            + " url(&#39;http://example.org/a.jpg/URIPOLICY&#39;)\">"
            + "</div>")),
        new Block(), true);
  }

  public final void testStyleAttrsWithoutUriPolicy() throws Exception {
    meta = new PluginMeta(
        meta.getUriFetcher(),
        null,
        meta.getPrecajoleMap(),
        meta.getPrecajoleMinify());
    assertSafeHtml(
        htmlFragment(fromString(
            "<div style=\"background-image: url(a.jpg)\"></div>")),
        htmlFragment(fromString(
            "<div id=\"id_1___\"></div>")),
        js(fromString(""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_1___');"
            + "    emitter___.setAttr(el___, 'style', "
            + "        'background-image: url('"
            + "        + IMPORTS___.rewriteUriInCss___("
            + "              'http://example.org/a.jpg', 'background-image')"
            + "        + ')');"
            + "    emitter___.rmAttr(el___, 'id');"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")),
            true);
  }

  public final void testFormTargetSpecified() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString(
            "<form target=\"foo\"></form>")),
        htmlFragment(fromString(""
            + "<form "
            + "  target=\"_blank\""
            + "  action=\"http://example.org/testFormTargetSpecified\""
            + "  autocomplete=\"off\""
            + "  id=\"id_1___\"><"
            + "/form>")),
        js(fromString(""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_1___');"
            + "    emitter___.setAttr("
            + "        el___,"
            + "        'target',"
            + "        IMPORTS___.rewriteTargetAttribute___("
            + "            'foo', 'form', 'target'));"
            + "    emitter___.rmAttr(el___, 'id');"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")),
        true);
  }

  public final void testAnchorTargetEmpty() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString(
            "<a>a</a>")),
        htmlFragment(fromString(
            "<a target=\"_blank\" id=\"id_1___\">a</a>")),
        js(fromString(""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_1___');"
            + "    emitter___.setAttr("
            + "        el___,"
            + "        'target',"
            + "        IMPORTS___.rewriteTargetAttribute___("
            + "            null, 'a', 'target'));"
            + "    emitter___.rmAttr(el___, 'id');"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")),
        true);
  }

  public final void testAnchorTargetSpecified() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString(
            "<a target=\"foo\">a</a>")),
        htmlFragment(fromString(
            "<a target=\"_blank\" id=\"id_1___\">a</a>")),
        js(fromString(""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_1___');"
            + "    emitter___.setAttr("
            + "        el___,"
            + "        'target',"
            + "        IMPORTS___.rewriteTargetAttribute___("
            + "            'foo', 'a', 'target'));"
            + "    emitter___.rmAttr(el___, 'id');"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")),
        true);
  }

  public final void testDeferredScript1() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString(
            "<div id=x>Foo</div><script defer>alert()</script>")),
        htmlFragment(fromString(
            "<div id=\"id_2___\">Foo</div>")),
        js(fromString(""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_2___');"
            + "    emitter___.setAttr("
            + "        el___, 'id', 'x-' + IMPORTS___.getIdClass___());"
            + "    el___ = emitter___.finish();"
            + "  }"
            + "}"
            + "function module() {"
            + "  try {\n"
            + "    { alert(); }"
            + "  } catch (ex___) {"
            + "    ___.getNewModuleHandler().handleUncaughtException("
            + "      ex___, onerror, 'testDeferredScript1', '1');"
            + "  }"
            + "}"
            + "function module() {"
            + "  {"
            + "    IMPORTS___.htmlEmitter___.signalLoaded();"
            + "  }"
            + "}")),
        true);
  }

  public final void testDeferredScript2() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString(
            "<script defer>alert()</script><div id=x>Foo</div>")),
        htmlFragment(fromString(
            "<div id=\"id_2___\">Foo</div>")),
        js(fromString(""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_2___');"
            + "    emitter___.setAttr("
            + "        el___, 'id', 'x-' + IMPORTS___.getIdClass___());"
            + "    el___ = emitter___.finish();"
            + "  }"
            + "}"
            + "function module() {"
            + "  try {\n"
            + "    { alert(); }"
            + "  } catch (ex___) {"
            + "    ___.getNewModuleHandler().handleUncaughtException("
            + "      ex___, onerror, 'testDeferredScript2', '1');"
            + "  }"
            + "}"
            + "function module() {"
            + "  {"
            + "    IMPORTS___.htmlEmitter___.signalLoaded();"
            + "  }"
            + "}")),
        true);
  }

  public final void testScriptAtEnd() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString(
            "<div id=x>Foo</div><script>alert()</script>")),
        htmlFragment(fromString(
            "<div id=\"id_2___\">Foo<span id=\"id_3___\"></span></div>")),
        js(fromString(""
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.byId('id_2___');"
            + "    emitter___.setAttr("
            + "        el___, 'id', 'x-' + IMPORTS___.getIdClass___());"
            + "    emitter___.discard(emitter___.attach('id_3___'));"
            + "  }"
            + "}"
            + "function module() {"
            + "  try {\n"
            + "    { alert(); }"
            + "  } catch (ex___) {"
            + "    ___.getNewModuleHandler().handleUncaughtException("
            + "      ex___, onerror, 'testScriptAtEnd', '1');"
            + "  }"
            + "}"
            + "function module() {"
            + "  {"
            + "    var el___;"
            + "    var emitter___ = IMPORTS___.htmlEmitter___;"
            + "    el___ = emitter___.finish();"
            + "    emitter___.signalLoaded();"
            + "  }"
            + "}")),
        true);
  }

  private void assertSafeHtml(
      DocumentFragment input, DocumentFragment htmlGolden, Block jsGolden) {
    assertSafeHtml(input, htmlGolden, jsGolden, true);
  }

  private void assertSafeHtml(
      DocumentFragment input, DocumentFragment htmlGolden, Block jsGolden,
      boolean checkErrors) {
    assertSafeHtml(
        Collections.singletonList(input), htmlGolden, jsGolden, checkErrors);
  }

  private void assertSafeHtml(
      List<DocumentFragment> inputs, DocumentFragment htmlGolden,
      Block jsGolden) {
    assertSafeHtml(inputs, htmlGolden, jsGolden, true);
  }

  private void assertSafeHtml(
      List<DocumentFragment> inputs, DocumentFragment htmlGolden,
      Block jsGolden, boolean checkErrors) {
    List<IhtmlRoot> html = Lists.newArrayList();
    List<ValidatedStylesheet> css = Lists.newArrayList();
    List<ScriptPlaceholder> extractedScripts = Lists.newArrayList();
    for (DocumentFragment input : inputs) {
      extractScriptsAndStyles(
          input, URI.create("file:///"), html, css, extractedScripts);
    }

    TemplateCompiler tc = new TemplateCompiler(
        html, css, extractedScripts, CssSchema.getDefaultCss21Schema(mq),
        HtmlSchema.getDefault(mq), meta, mc, mq);
    Document doc = DomParser.makeDocument(null, null);
    Pair<List<SafeHtmlChunk>, List<SafeJsChunk>> safeContent
        = tc.getSafeHtml(doc);

    if (checkErrors) {
      assertNoErrors();
      // No warnings about skipped elements.  Warning is not the compiler's job.
    }

    StringBuilder renderedHtml = new StringBuilder();
    for (SafeHtmlChunk safeHtml : safeContent.a) {
      assertEquals(safeHtml.root.getOwnerDocument(), doc);
      renderedHtml.append(Nodes.render(safeHtml.root, MarkupRenderMode.XML));
    }
    assertEquals(
        Nodes.render(htmlGolden, MarkupRenderMode.XML),
        renderedHtml.toString());
    assertEquals(
        renderProgram(jsGolden), renderProgram(consolidate(safeContent.b)));
 }

  private void extractScriptsAndStyles(
      DocumentFragment n, URI baseUri, List<IhtmlRoot> htmlOut,
      List<ValidatedStylesheet> cssOut,
      List<ScriptPlaceholder> extractedScripts) {
    RewriteHtmlStage rhs = new RewriteHtmlStage(
        HtmlSchema.getDefault(mq), new StubJobCache());
    Jobs jobs = new Jobs(mc, mq, meta);
    jobs.getJobs().add(JobEnvelope.of(Job.domJob(new Dom(n), baseUri)));
    rhs.apply(jobs);

    for (JobEnvelope j : jobs.getJobs()) {
      switch (j.job.getType()) {
        case HTML:
          DocumentFragment f = (DocumentFragment)
            ((Dom) j.job.getRoot()).getValue();
          htmlOut.add(new IhtmlRoot(j, f, baseUri));
          break;
        case JS:
          extractedScripts.add(new ScriptPlaceholder(j, j.job.getRoot()));
          break;
        case CSS:
          CssTree.StyleSheet css = (CssTree.StyleSheet) j.job.getRoot();
          CssRewriter rw = new CssRewriter(
              null, CssSchema.getDefaultCss21Schema(mq),
              HtmlSchema.getDefault(mq), mq);
          rw.rewrite(AncestorChain.instance(css));
          CssDynamicExpressionRewriter rrw =
              new CssDynamicExpressionRewriter(meta);
          rrw.rewriteCss(css);
          cssOut.add(new ValidatedStylesheet(j, css, is.getUri()));
          break;
        default:
          throw new AssertionError(j.job.getType().toString());
      }
    }
    assertMessagesLessSevereThan(MessageLevel.ERROR);
  }

  private Block consolidate(List<SafeJsChunk> chunks) {
    Block consolidated = new Block();
    MutableParseTreeNode.Mutation mut = consolidated.createMutation();
    FilePosition unk = FilePosition.UNKNOWN;
    for (SafeJsChunk chunk : chunks) {
      Identifier ident = new Identifier(unk, "module");
      mut.appendChild(new FunctionDeclaration(new FunctionConstructor(
          unk, ident, Collections.<FormalParam>emptyList(),
          (Block) chunk.body)));
    }
    mut.execute();
    stripTranslatedCode(consolidated);
    return consolidated;
  }

  private static void stripTranslatedCode(ParseTreeNode node) {
    node.acceptPostOrder(new Visitor() {
      public boolean visit(AncestorChain<?> ac) {
        ParseTreeNode node = ac.node;
        if (node instanceof TranslatedCode) {
          ((MutableParseTreeNode) ac.parent.node)
              .replaceChild(((TranslatedCode) node).getTranslation(), node);
        }
        return true;
      }
    }, null);
  }
}
