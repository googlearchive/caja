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
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.TranslatedCode;
import com.google.caja.plugin.CssRuleRewriter;
import com.google.caja.plugin.ExtractedHtmlContent;
import com.google.caja.plugin.PluginEnvironment;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Pair;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class TemplateCompilerTest extends CajaTestCase {
  private PluginMeta meta;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    meta = new PluginMeta(new PluginEnvironment() {
      public CharProducer loadExternalResource(
          ExternalReference ref, String mimeType) {
        throw new RuntimeException("NOT IMPLEMENTED");
      }

      // return the URI unchanged, so we can test URI normalization
      public String rewriteUri(ExternalReference ref, String mimeType) {
        return ref.getUri().toString();
      }
    });
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
            + "<script type=\"text/javascript\">1;</script>")),
        htmlFragment(fromString(
            "<p id=\"id_1___\">a</p>")),
        js(fromString(
            ""
            + "{"
            + "  var el___;"
            + "  var emitter___ = IMPORTS___.htmlEmitter___;"
            + "  el___ = emitter___.byId('id_1___');"
            + "  emitter___.setAttr("
            + "      el___, 'id', 'a-' + IMPORTS___.getIdClass___());"
            + "  el___ = emitter___.finish();"
            + "}"
            + "try {"
            + "  { 1; }"
            + "} catch (ex___) {"
            + "  ___.getNewModuleHandler().handleUncaughtException(ex___,"
            + "      onerror, 'testSignalLoadedAtEnd', '1');"
            + "}"
            + "{"
            + "  emitter___.signalLoaded();"
            + "}")));
  }

  public final void testTargetsRewritten() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("<a href='foo' target='_self'>hello</a>")),
        htmlFragment(fromString("<a href='foo' target='_blank'>hello</a>")),
        new Block());
  }

  public final void testFormRewritten() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("<form></form>")),
        htmlFragment(fromString(
            "<form action='test:///testFormRewritten'"
            + " target='_blank'></form>")),
        new Block());
  }

  public final void testNamesRewritten() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("<a name='hi'></a>")),
        htmlFragment(fromString("<a id='id_1___' target='_blank'></a>")),
        js(fromString(
            ""
            + "{"
            + "  var el___; var emitter___ = IMPORTS___.htmlEmitter___;"
            + "  el___ = emitter___.byId('id_1___');"
            + "  emitter___.setAttr("
            + "      el___, 'name', 'hi-' + IMPORTS___.getIdClass___());"
            + "  el___.removeAttribute('id');"
            + "  el___ = emitter___.finish();"
            + "  emitter___.signalLoaded();"
            + "}")));

    meta.setIdClass("xyz___");
    assertSafeHtml(
        htmlFragment(fromString("<a name='hi'></a>")),
        htmlFragment(fromString("<a name='hi-xyz___' target='_blank'></a>")),
        new Block());
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
            "<form action='test:///testFormName'"
            + " name='hi-suffix___' target=_blank></form>")),
        new Block());
  }

  // See bug 722
  public final void testFormOnSubmitTrue() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString(
            "<form onsubmit='alert(&quot;hi&quot;); return true;'></form>")),
        htmlFragment(fromString(
            "<form action='test:///testFormOnSubmitTrue'"
            + " id=id_2___ target='_blank'></form>")),
        js(fromString(
            ""
            + "{"
            // The extracted handler.
            + "  var c_1___ = ___.markFuncFreeze(function(event, thisNode___) {"
            + "    alert('hi');"  // Cajoled later
            + "    return true;"
            + "  });"
            + "  var el___; var emitter___ = IMPORTS___.htmlEmitter___;"
            + "  el___ = emitter___.byId('id_2___');"
            + "  el___.onsubmit = function (event) {"
            + "    return plugin_dispatchEvent___("
            + "        this, event, ___.getId(IMPORTS___), c_1___);"
            + "  };"
            + "  el___.removeAttribute('id');"
            + "  el___ = emitter___.finish();"
            + "  emitter___.signalLoaded();"
            + "}")));
  }

  // See bug 722
  public final void testFormOnSubmitEmpty() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("<form onsubmit=''></form>")),
        htmlFragment(fromString(
            "<form action='test:///testFormOnSubmitEmpty'"
            + " target='_blank'></form>")),
        new Block());
  }

  public final void testImageSrc() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString("<img src='blank.gif' width='20'/>")),
        htmlFragment(fromString("<img src='blank.gif' width='20'/>")),
        new Block());
  }

  public final void testStyleRewriting() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString(
            "<div style=\"position: absolute; background: url('bg-image')\">\n"
            + "Hello\n"
            + "</div>\n")),
        htmlFragment(fromString(
            "<div style=\"position: absolute; background: url('test:/bg-image')"
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

  public final void testDeferredScripts() throws Exception {
    // If all the scripts are deferred, there is never any need to detach any
    // of the DOM tree.
    assertSafeHtml(
        htmlFragment(fromString("Hi<script>alert('howdy');</script>\n")),
        htmlFragment(fromString("Hi")),
        js(fromString(
            ""
            + "try {"
            + "  { alert('howdy'); }"
            + "} catch (ex___) {"
            + "  ___.getNewModuleHandler().handleUncaughtException("
            + "      ex___, onerror, 'testDeferredScripts', '1');"
            + "}"
            + "{"
            + "  IMPORTS___.htmlEmitter___.signalLoaded();"
            + "}"))
        );
  }

  public final void testMailto() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString(
            "<a href='mailto:x@y' target='_blank'>z</a>")),
        htmlFragment(fromString(
            "<a href='mailto:x@y' target='_blank'>z</a>")),
        new Block());
  }

  public final void testComplexUrl() throws Exception {
    assertSafeHtml(
        htmlFragment(fromString(
            "<a href='http://b/c;_d=e?f=g&i=%26' target='_blank'>z</a>")),
        htmlFragment(fromString(
            "<a href='http://b/c;_d=e?f=g&i=%26' target='_blank'>z</a>")),
        new Block());
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
            + "<textarea>Howdy!</textarea>"
            + "<span id=\"id_1___\"></span>"
            + "<textarea>Bye!</textarea>")),
        js(fromString(
            ""
            + "{"
            + "  var el___; var emitter___ = IMPORTS___.htmlEmitter___;"
            + "  emitter___.discard(emitter___.attach('id_1___'));"
            + "}"
            + "try {"
            + "  { alert('Howdy yourself!'); }"
            + "}catch (ex___) {"
            + "  ___.getNewModuleHandler().handleUncaughtException("
            + "      ex___, onerror, 'testTextAreas', '1');"
            + "}"
            + "{"
            + "  el___ = emitter___.finish();"
            + "  emitter___.signalLoaded();"
            + "}")));
  }

  private class Holder<T> { T value; }

  public final void testUriAttributeResolution() throws Exception {
    // Ensure that the TemplateCompiler calls its PluginEnvironment with the
    // correct information when it encounters a URI-valued HTML attribute.

    final Holder<ExternalReference> savedRef = new Holder<ExternalReference>();

    meta = new PluginMeta(new PluginEnvironment() {
      public CharProducer loadExternalResource(
          ExternalReference ref, String mimeType) {
        throw new RuntimeException("NOT IMPLEMENTED");
      }

      public String rewriteUri(ExternalReference ref, String mimeType) {
        savedRef.value = ref;
        return "rewritten";
      }
    });

    DocumentFragment htmlInput =
        htmlFragment(fromString("<a href=\"x.html\"></a>"));
    assertSafeHtml(
        htmlInput,
        htmlFragment(fromString(
            "<a href=\"rewritten\" target=\"_blank\"></a>")),
        new Block());

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
            + "<script>1</script>")),
        htmlFragment(fromString(
            ""
            + "<div id=\"id_1___\"></div>"
            + "<div id=\"id_2___\"></div>")),
        js(fromString(
            ""
            + "{"
            + "  var el___;"
            + "  var emitter___ = IMPORTS___.htmlEmitter___;"
            + "  el___ = emitter___.byId('id_1___');"
            + "  emitter___.setAttr(el___, 'id',"
            + "    'a-' + IMPORTS___.getIdClass___());"
            + "  el___ = emitter___.byId('id_2___');"
            + "  emitter___.setAttr(el___, 'id',"
            + "    'b-' + IMPORTS___.getIdClass___());"
            + "  el___ = emitter___.finish();"
            + "}"
            + "try {"
            + "  {"
            + "    1;"
            + "  }"
            + "} catch (ex___) {"
            + "  ___.getNewModuleHandler().handleUncaughtException(ex___,"
            + "    onerror, 'testFinishCalledAtEnd', '1');"
            + "}"
            + "{"
            + "  emitter___.signalLoaded();"
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
        htmlFragment(fromString("<textarea></textarea>")),
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
        htmlFragment(fromString("<input name='tag[]'>")),
        new Block());
    assertNoWarnings();

    assertSafeHtml(
        htmlFragment(fromString("<input name='form$location'>")),
        htmlFragment(fromString("<input name='form$location'>")),
        new Block());
    assertNoWarnings();

    assertSafeHtml(
        htmlFragment(fromString("<input name='$-.:;()[]='>")),
        htmlFragment(fromString("<input name='$-.:;()[]='>")),
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
            "{"
            + "  var el___;"
            + "  var emitter___ = IMPORTS___.htmlEmitter___;"
            + "  el___ = emitter___.byId('id_1___');"
            + "  emitter___.setAttr(el___, 'id',"
            + "    '23skiddoo-' + IMPORTS___.getIdClass___());"
            + "  el___ = emitter___.byId('id_2___');"
            + "  emitter___.setAttr(el___, 'id',"
            + "    '8675309-' + IMPORTS___.getIdClass___());"
            + "  el___ = emitter___.byId('id_3___');"
            + "  emitter___.setAttr(el___, 'id',"
            + "    '$-.:;()[]=-' + IMPORTS___.getIdClass___());"
            + "  el___ = emitter___.finish();"
            + "  emitter___.signalLoaded();"
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
        htmlFragment(fromString("<input>")),
        new Block());
    assertMessage(true, IhtmlMessageType.ILLEGAL_NAME, MessageLevel.WARNING,
        MessagePart.Factory.valueOf("bad1__"));
    assertMessage(true, IhtmlMessageType.ILLEGAL_NAME, MessageLevel.WARNING,
        MessagePart.Factory.valueOf("bad2__"));
    assertNoWarnings();

    assertSafeHtml(
        htmlFragment(fromString("<input id='bad1__ ' name='bad2__ '>")),
        htmlFragment(fromString("<input>")),
        new Block());
    assertMessage(true, IhtmlMessageType.ILLEGAL_NAME, MessageLevel.WARNING,
        MessagePart.Factory.valueOf("bad1__ "));
    assertMessage(true, IhtmlMessageType.ILLEGAL_NAME, MessageLevel.WARNING,
        MessagePart.Factory.valueOf("bad2__ "));
    assertNoWarnings();

    assertSafeHtml(
        htmlFragment(fromString("<input id='b__ c'>")),
        htmlFragment(fromString("<input>")),
        new Block(),
        false);
    assertMessage(true, IhtmlMessageType.ILLEGAL_NAME, MessageLevel.ERROR,
        MessagePart.Factory.valueOf("b__ c"));
    assertNoWarnings();

    assertSafeHtml(
       htmlFragment(fromString("<input name='d__ e'>")),
       htmlFragment(fromString("<input>")),
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
            + "{"
            + "  var el___;"
            + "  var emitter___ = IMPORTS___.htmlEmitter___;"
            + "  el___ = emitter___.byId('id_1___');"
            + "  emitter___.setAttr(el___, 'headers',"
            + "    'a-' + IMPORTS___.getIdClass___()"
            + "    + ' b-' + IMPORTS___.getIdClass___());"
            + "  el___.removeAttribute('id');"
            + "  el___ = emitter___.finish();"
            + "  emitter___.signalLoaded();"
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
            + "<area target=_blank href=foo.html />"
            + "</map>"
            + "<img usemap=#foo-suffix___ src=pic.gif>")),
         new Block());
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
            + "<area target=_blank href=foo.html />"
            + "</map>"
            + "<img src=foo.gif>"
            + "<img src=bar.gif>")),
         new Block(), false);
  }

  private void assertSafeHtml(
      DocumentFragment input, DocumentFragment htmlGolden, Block jsGolden)
      throws ParseException {
    assertSafeHtml(input, htmlGolden, jsGolden, true);
  }

  private void assertSafeHtml(
      DocumentFragment input, DocumentFragment htmlGolden, Block jsGolden,
      boolean checkErrors) throws ParseException {
    assertSafeHtml(
        Collections.singletonList(input), htmlGolden, jsGolden, checkErrors);
  }

  private void assertSafeHtml(
      List<DocumentFragment> inputs, DocumentFragment htmlGolden,
      Block jsGolden) throws ParseException {
    assertSafeHtml(inputs, htmlGolden, jsGolden, true);
  }

  private void assertSafeHtml(
      List<DocumentFragment> inputs, DocumentFragment htmlGolden,
      Block jsGolden, boolean checkErrors) throws ParseException {
    List<Node> html = new ArrayList<Node>();
    List<CssTree.StyleSheet> css = new ArrayList<CssTree.StyleSheet>();
    for (DocumentFragment input : inputs) {
      extractScriptsAndStyles(input, html, css);
    }

    TemplateCompiler tc = new TemplateCompiler(
        html, css, CssSchema.getDefaultCss21Schema(mq),
        HtmlSchema.getDefault(mq), meta, mc, mq);
    Document doc = DomParser.makeDocument(null, null);
    Pair<Node, List<Block>> safeContent = tc.getSafeHtml(doc);

    if (checkErrors) {
      assertNoErrors();
      // No warnings about skipped elements.  Warning is not the compiler's job.
    }

    assertEquals(safeContent.a.getOwnerDocument(), doc);

    assertEquals(Nodes.render(htmlGolden, true),
                 Nodes.render(safeContent.a, true));
    assertEquals(render(jsGolden), render(consolidate(safeContent.b)));
  }

  private void extractScriptsAndStyles(
      Node n, List<Node> htmlOut, List<CssTree.StyleSheet> cssOut)
      throws ParseException {
    n = extractScripts(n);
    htmlOut.add(n);
    extractStyles(n, cssOut);
  }

  private static String HTML_NS = Namespaces.HTML_NAMESPACE_URI;
  private Node extractScripts(Node n) throws ParseException {
    if (n instanceof Element && "script".equals(n.getLocalName())
        && HTML_NS.equals(n.getNamespaceURI())) {
      Element span = n.getOwnerDocument().createElementNS(HTML_NS, "span");
      if (n.getParentNode() != null) {
        n.getParentNode().replaceChild(span, n);
      }
      FilePosition pos = Nodes.getFilePositionFor(n);
      String text = n.getFirstChild().getNodeValue();
      Block js = js(fromString(text, pos));
      ExtractedHtmlContent.setExtractedScriptFor(span, js);
      Nodes.setFilePositionFor(span, Nodes.getFilePositionFor(n));
      return span;
    }
    for (Node child : Nodes.childrenOf(n)) { extractScripts(child); }
    return n;
  }

  private void extractStyles(Node n, List<CssTree.StyleSheet> styles)
      throws ParseException {
    if (n instanceof Element && "style".equals(n.getNodeName())
        && HTML_NS.equals(n.getNamespaceURI())) {
      FilePosition pos = Nodes.getFilePositionFor(n);
      if (n.getFirstChild() != null) {
        String text = n.getFirstChild().getNodeValue();
        CssTree.StyleSheet css = css(fromString(text, pos));
        CssRuleRewriter rrw = new CssRuleRewriter(meta);
        rrw.rewriteCss(css);
        assertMessagesLessSevereThan(MessageLevel.ERROR);
        styles.add(css);
      }
      n.getParentNode().removeChild(n);
      return;
    }
    for (Node child : Nodes.childrenOf(n)) { extractStyles(child, styles); }
  }

  private Block consolidate(List<Block> blocks) {
    Block consolidated = new Block();
    MutableParseTreeNode.Mutation mut = consolidated.createMutation();
    for (Block bl : blocks) {
      mut.appendChildren(bl.children());
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
