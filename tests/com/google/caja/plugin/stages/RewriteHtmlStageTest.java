// Copyright (C) 2008 Google Inc.
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

package com.google.caja.plugin.stages;

import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.FilePosition;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.PluginMessageType;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.ContentType;
import com.google.caja.util.Join;

/**
 *
 * @author mikesamuel@gmail.com
 */
public final class RewriteHtmlStageTest extends PipelineStageTestCase {
  public final void testScriptExtraction() throws Exception {
    assertPipeline(
        job("foo<script>extracted();</script>baz", ContentType.HTML),
        // The "id" attribute was added by the RewriteHtmlStage.
        htmlJobBody("foo<span __phid__=\"$1\"></span>baz"),
        job("{ extracted(); }", ContentType.JS)
        );
    assertNoErrors();

    assertPipeline(
        job("foo<script type=text/vbscript>deleted()</script>baz",
            ContentType.HTML),
        htmlJobBody("foobaz")
        );
    assertMessage(
        PluginMessageType.UNRECOGNIZED_CONTENT_TYPE, MessageLevel.WARNING);

    meta = createPluginMeta();
    assertPipeline(
        job("foo<script type=\"text/javascript\">var x = 1;</script>baz",
            ContentType.HTML),
        htmlJobBody("foo<span __phid__=\"$1\"></span>baz"),
        job("{\n  var x = 1;\n}", ContentType.JS)
        );
    assertNoErrors();

    assertPipeline(
        job("foo<script type=\"text/javascript\""
            + ">useXml(<xml>foo</xml>);</script>baz",
            ContentType.HTML),
        htmlJobBody("foobaz")
        );
    assertMessage(
        MessageType.UNEXPECTED_TOKEN,
        MessageLevel.ERROR,
        FilePosition.instance(is, 1, 42, 42, 1),
        MessagePart.Factory.valueOf("<"));

    meta = createPluginMeta();
    assertPipeline(
        job("foo<script type=text/javascript>extracted();</script>baz",
            ContentType.HTML),
        htmlJobBody("foo<span __phid__=\"$1\"></span>baz"),
        job("{ extracted(); }", ContentType.JS)
        );
    assertNoErrors();

    assertPipeline(
        job("foo<script></script>baz", ContentType.HTML),
        htmlJobBody("foobaz")
        );
    assertNoErrors();
  }

  public final void testDataUris() throws Exception {
    assertPipeline(
        job("foo<script src='data:text/javascript,extracted();'>"
            + "bar</script>baz",
            ContentType.HTML),
        htmlJobBody("foo<span __phid__=\"$1\"></span>baz"),
        job("{ extracted(); }", ContentType.JS)
        );
    assertNoErrors();

    meta = createPluginMeta();
    assertPipeline(
        job("foo<script src='data:,extracted();'>bar</script>baz",
            ContentType.HTML),
        htmlJobBody("foo<span __phid__=\"$1\"></span>baz"),
        job("{ extracted(); }", ContentType.JS)
        );
    assertNoErrors();

    meta = createPluginMeta();
    assertPipeline(
        job("foo<script src='data:iso-8859-7;charset=utf-8,extracted%28%29%3B'>"
            + "bar</script>baz", ContentType.HTML),
        htmlJobBody("foo<span __phid__=\"$1\"></span>baz"),
        job("{ extracted(); }", ContentType.JS)
        );
    assertNoErrors();

    meta = createPluginMeta();
    assertPipeline(
        job("foo<script src="
            + "'data:text/javascript;charset=utf-8;base64,ZXh0cmFjdGVkKCk7'>"
            + "bar</script>baz", ContentType.HTML),
        htmlJobBody("foo<span __phid__=\"$1\"></span>baz"),
        job("{ extracted(); }", ContentType.JS)
        );
    assertNoErrors();
}

  public final void testStyleExtraction() throws Exception {
    assertPipeline(
        job("Foo<style>p { color: blue }</style><p>Bar", ContentType.HTML),
        job("p {\n  color: blue\n}", ContentType.CSS),
        htmlJobBody("Foo<p>Bar</p>"));
    assertNoErrors();

    assertPipeline(
        job("Foo<link rel=stylesheet href=content:p+%7Bcolor%3A+blue%7D><p>Bar",
            ContentType.HTML),
        job("p {\n  color: blue\n}", ContentType.CSS),
        htmlJobBody("Foo<p>Bar</p>"));
    assertNoErrors();

    assertPipeline(
        job("Foo<style></style><p>Bar", ContentType.HTML),
        htmlJobBody("Foo<p>Bar</p>"));
    assertNoErrors();
  }

  public final void testOnLoadHandlers() throws Exception {
    assertPipeline(
        job("<body onload=init();>Foo</body>", ContentType.HTML),
        htmlJobBody("Foo<span __phid__=\"$1\"></span>"),
        job("{ init(); }", ContentType.JS));
    assertNoErrors();
  }

  public final void testImportedStyles() throws Exception {
    assertPipeline(
        job("<style>@import 'styles.css';</style>", ContentType.HTML),
        job("@import url('styles.css');", ContentType.CSS),
        htmlJobBody("")
        );
    assertNoErrors();
  }

  public final void testTypeAndMediaAttributes() throws Exception {
    assertPipeline(
        job("<link rel=stylesheet media=screen href=content:p+%7B%7D>",
            ContentType.HTML),
        job("@media screen {\n  p {\n  }\n}", ContentType.CSS),
        htmlJobBody(""));
    assertNoErrors();

    assertPipeline(
        job("<link rel=stylesheet type=text/css href=content:p+%7B%7D>",
            ContentType.HTML),
        job("p {\n}", ContentType.CSS),
        htmlJobBody(""));
    assertNoErrors();

    assertPipeline(
        job("<link rel=stylesheet media=all href=content:p+%7B%7D>",
            ContentType.HTML),
        job("p {\n}", ContentType.CSS),
        htmlJobBody(""));
    assertNoErrors();

    assertPipeline(
        job("<link rel=stylesheet media=braille,tty type=text/css"
            + " href=content:p+%7B%7D>",
            ContentType.HTML),
        job("@media braille, tty {\n  p {\n  }\n}", ContentType.CSS),
        htmlJobBody(""));
    assertNoErrors();
  }

  public final void testDeferredScripts() throws Exception {
    assertPipeline(
        job("<body><script src=content:a();></script>"
            + "<script defer>b();</script>"
            + "<script src=content:c(); defer=defer></script>"
            + "<script src=content:d(); defer=no></script>"
            + "<br>",
            ContentType.HTML),
        // This bogus HTML structure is OK because it will only be seen by our
        // runtime, and be cleaned up by the time guest code sees it.
        job("<html><head></head><body>"
            + "<span __phid__=\"$1\"></span>"   // a()
            + "<span __phid__=\"$4\"></span><br />"  // d()
            + "</body></html>"
            + "<span __phid__=\"finish\"></span>"  // document.close() here.
            + "<span __phid__=\"$2\"></span>"  // b()
            + "<span __phid__=\"$3\"></span>",  // c()
            ContentType.HTML),
        job("{ a(); }", ContentType.JS),
        job("{ b(); }", ContentType.JS),
        job("{ c(); }", ContentType.JS),
        job("{ d(); }", ContentType.JS));
    assertNoErrors();
  }

  public final void testUnloadableScripts() throws Exception {
    assertPipeline(
        job("<body><script src=content:onerror=panic;></script>"
            + "<script src=\"http://bogus.com/bogus.js#'!\"></script>"
            + "<script src=content:foo()></script>",
            ContentType.HTML),
        htmlJobBody("<span __phid__=\"$1\"></span>"
            + "<span __phid__=\"$2\"></span>"
            + "<span __phid__=\"$3\"></span>"),
        job("{ onerror = panic; }", ContentType.JS),
        job("{\n  throw new Error('Failed to load bogus.js#%27%21');\n}",
            ContentType.JS),
        job("{ foo(); }", ContentType.JS));
    assertNoErrors();
  }

  public final void testOSMLScriptElements() throws Exception {
    assertPipeline(
        job(Join.join(
                "\n",
                "Before OS Template",
                "<script type=\"text/opensocial\"",
                " xmlns:os=\"http://ns.opensocial.org/2008/markup\">",
                "  a ? Hello <os:template",
                "  name=\"bob\"> World",
                "  1 </os:template>/",
                "</script>",
                "After OS Template"),
            ContentType.HTML),
        // Script element removed, but not parsed as JS.
        htmlJobBody("Before OS Template\n\nAfter OS Template"));
    assertMessage(
        true, PluginMessageType.UNRECOGNIZED_CONTENT_TYPE, MessageLevel.WARNING,
        MessagePart.Factory.valueOf("text/opensocial"));
    assertNoWarnings();
  }

  @Override
  protected boolean runPipeline(Jobs jobs) throws Exception {
    mq.getMessages().clear();
    HtmlSchema schema = HtmlSchema.getDefault(mq);
    return new ResolveUriStage(schema).apply(jobs)
        && new RewriteHtmlStage(schema, new StubJobCache()).apply(jobs);
  }
}
