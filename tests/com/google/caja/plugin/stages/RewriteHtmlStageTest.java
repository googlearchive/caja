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
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.html.Dom;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.parser.js.Block;
import com.google.caja.plugin.ExtractedHtmlContent;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.PluginMessageType;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.ContentType;
import com.google.caja.util.Lists;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author mikesamuel@gmail.com
 */
public final class RewriteHtmlStageTest extends PipelineStageTestCase {
  public final void testScriptExtraction() throws Exception {
    assertPipeline(
        job("foo<script>extracted();</script>baz", ContentType.HTML),
        // The "jobnum" attribute was added by the extractScript method below.
        job("foo<span jobnum=\"1\"></span>baz", ContentType.HTML),
        job("{ extracted(); }", ContentType.JS)
        );
    assertNoErrors();

    assertPipeline(
        job("foo<script type=text/vbscript>deleted()</script>baz",
            ContentType.HTML),
        job("foobaz", ContentType.HTML)
        );
    assertMessage(
        PluginMessageType.UNRECOGNIZED_CONTENT_TYPE, MessageLevel.WARNING);

    assertPipeline(
        job("foo<script type=\"text/javascript\">var x = 1;</script>baz",
            ContentType.HTML),
        job("foo<span jobnum=\"1\"></span>baz", ContentType.HTML),
        job("{\n  var x = 1;\n}", ContentType.JS)
        );
    assertNoErrors();

    assertPipeline(
        job("foo<script type=\"text/javascript\""
            + ">useXml(<xml>foo</xml>);</script>baz",
            ContentType.HTML),
        job("foobaz", ContentType.HTML)
        );
    assertMessage(
        MessageType.UNEXPECTED_TOKEN,
        MessageLevel.ERROR,
        FilePosition.instance(is, 1, 42, 42, 1),
        MessagePart.Factory.valueOf("<"));

    assertPipeline(
        job("foo<script type=text/javascript>extracted();</script>baz",
            ContentType.HTML),
        job("foo<span jobnum=\"1\"></span>baz", ContentType.HTML),
        job("{ extracted(); }", ContentType.JS)
        );
    assertNoErrors();

    assertPipeline(
        job("foo<script></script>baz", ContentType.HTML),
        job("foobaz", ContentType.HTML)
        );
    assertNoErrors();
  }

  public final void testDataUris() throws Exception {
    assertPipeline(
        job("foo<script src='data:text/javascript,extracted();'>bar</script>baz", ContentType.HTML),
        job("foo<span jobnum=\"1\"></span>baz", ContentType.HTML),
        job("{ extracted(); }", ContentType.JS)
        );
    assertNoErrors();

    assertPipeline(
        job("foo<script src='data:,extracted();'>bar</script>baz", ContentType.HTML),
        job("foo<span jobnum=\"1\"></span>baz", ContentType.HTML),
        job("{ extracted(); }", ContentType.JS)
        );
    assertNoErrors();

    assertPipeline(
        job("foo<script src='data:iso-8859-7;charset=utf-8,extracted%28%29%3B'>bar</script>baz", ContentType.HTML),
        job("foo<span jobnum=\"1\"></span>baz", ContentType.HTML),
        job("{ extracted(); }", ContentType.JS)
        );
    assertNoErrors();

    assertPipeline(
        job("foo<script src='data:text/javascript;charset=utf-8;base64,ZXh0cmFjdGVkKCk7'>bar</script>baz", ContentType.HTML),
        job("foo<span jobnum=\"1\"></span>baz", ContentType.HTML),
        job("{ extracted(); }", ContentType.JS)
        );
    assertNoErrors();
}

  public final void testStyleExtraction() throws Exception {
    assertPipeline(
        job("Foo<style>p { color: blue }</style><p>Bar", ContentType.HTML),
        job("Foo<p>Bar</p>", ContentType.HTML),
        job("p {\n  color: blue\n}", ContentType.CSS));
    assertNoErrors();

    assertPipeline(
        job("Foo<link rel=stylesheet href=content:p+%7Bcolor%3A+blue%7D><p>Bar",
            ContentType.HTML),
        job("Foo<p>Bar</p>", ContentType.HTML),
        job("p {\n  color: blue\n}", ContentType.CSS));
    assertNoErrors();

    assertPipeline(
        job("Foo<style></style><p>Bar", ContentType.HTML),
        job("Foo<p>Bar</p>", ContentType.HTML));
    assertNoErrors();
  }

  public final void testOnLoadHandlers() throws Exception {
    assertPipeline(
        job("<body onload=init();>Foo</body>", ContentType.HTML),
        job("<html><head></head>"
            + "<body>Foo<span jobnum=\"1\"></span></body></html>",
            ContentType.HTML),
        job("{ init(); }", ContentType.JS));
    assertNoErrors();
  }

  public final void testImportedStyles() throws Exception {
    assertPipeline(
        job("<style>@import 'styles.css';</style>", ContentType.HTML),
        job("", ContentType.HTML),
        job("@import url('styles.css');", ContentType.CSS)
        );
    assertNoErrors();
  }

  public final void testTypeAndMediaAttributes() throws Exception {
    assertPipeline(
        job("<link rel=stylesheet media=screen href=content:p+%7B%7D>",
            ContentType.HTML),
        job("", ContentType.HTML),
        job("@media screen {\n  p {\n  }\n}", ContentType.CSS));
    assertNoErrors();

    assertPipeline(
        job("<link rel=stylesheet type=text/css href=content:p+%7B%7D>",
            ContentType.HTML),
        job("", ContentType.HTML),
        job("p {\n}", ContentType.CSS));
    assertNoErrors();

    assertPipeline(
        job("<link rel=stylesheet media=all href=content:p+%7B%7D>",
            ContentType.HTML),
        job("", ContentType.HTML),
        job("p {\n}", ContentType.CSS));
    assertNoErrors();

    assertPipeline(
        job("<link rel=stylesheet media=braille,tty type=text/css"
            + " href=content:p+%7B%7D>",
            ContentType.HTML),
        job("", ContentType.HTML),
        job("@media braille, tty {\n  p {\n  }\n}", ContentType.CSS));
    assertNoErrors();
  }

  public final void testDeferredScripts() throws Exception {
    assertPipeline(
        job("<script src=content:a();></script>"
            + "<script defer>b();</script>"
            + "<script src=content:c(); defer=defer></script>"
            + "<script src=content:d(); defer=no></script>"
            + "<br>",
            ContentType.HTML),
        job("<span jobnum=\"1\"></span><span jobnum=\"2\"></span><br />"
            + "<span jobnum=\"3\"></span><span jobnum=\"4\"></span>",
            ContentType.HTML),
        job("{ a(); }", ContentType.JS),
        job("{ d(); }", ContentType.JS),
        job("{ b(); }", ContentType.JS),
        job("{ c(); }", ContentType.JS));
    assertNoErrors();
  }

  public final void testUnloadableScripts() throws Exception {
    assertPipeline(
        job("<script src=content:onerror=panic;></script>"
            + "<script src=\"http://bogus.com/bogus.js#'!\"></script>"
            + "<script src=content:foo()></script>",
            ContentType.HTML),
        job("<span jobnum=\"1\"></span><span jobnum=\"2\"></span>"
            + "<span jobnum=\"3\"></span>",
            ContentType.HTML),
        job("{ onerror = panic; }", ContentType.JS),
        job("{\n  throw new Error('Failed to load bogus.js#%27%21');\n}",
            ContentType.JS),
        job("{ foo(); }", ContentType.JS));
    assertNoErrors();
  }

  public final void testBodyClasses() throws Exception {
    assertPipeline(
        job("<body class=foo><b>Hello, World!</b></body>", ContentType.HTML),
        job("<html><head></head><body><b>Hello, World!</b></body></html>",
            ContentType.HTML),
        job("IMPORTS___.htmlEmitter___.addBodyClasses('foo')",
            ContentType.JS));
  }

  @Override
  protected boolean runPipeline(Jobs jobs) throws Exception {
    mq.getMessages().clear();
    HtmlSchema schema = HtmlSchema.getDefault(mq);
    boolean result = new ResolveUriStage(schema).apply(jobs)
        && new RewriteHtmlStage(schema).apply(jobs);
    // Dump the extracted script bits on the queue.
    for (Job job : Lists.newArrayList(jobs.getJobsByType(ContentType.HTML))) {
      Dom dom = job.getRoot().cast(Dom.class).node;
      extractScripts(dom.getValue(), jobs);
    }
    return result;
  }

  private void extractScripts(Node node, Jobs jobs) {
    switch (node.getNodeType()) {
      case Node.ELEMENT_NODE:
        Element el = (Element) node;
        Block extracted = ExtractedHtmlContent.getExtractedScriptFor(el);
        if (extracted != null) {
          int jobNum = jobs.getJobs().size();
          el.setAttributeNS(
              Namespaces.HTML_NAMESPACE_URI, "jobnum", "" + jobNum);
          jobs.getJobs().add(
              Job.jsJob(null, AncestorChain.instance(extracted), null));
        }
        for (Node c = el.getFirstChild(); c != null; c = c.getNextSibling()) {
          extractScripts(c, jobs);
        }
        break;
      case Node.DOCUMENT_FRAGMENT_NODE: case Node.DOCUMENT_NODE:
        for (Node c = node.getFirstChild(); c != null; c = c.getNextSibling()) {
          extractScripts(c, jobs);
        }
        break;
    }
  }
}
