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

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.parser.js.Block;
import com.google.caja.plugin.Dom;
import com.google.caja.plugin.ExtractedHtmlContent;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.PluginMessageType;
import com.google.caja.plugin.Job.JobType;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author mikesamuel@gmail.com
 */
public final class RewriteHtmlStageTest extends PipelineStageTestCase {
  public final void testScriptExtraction() throws Exception {
    assertPipeline(
        job("foo<script>extracted();</script>baz", Job.JobType.HTML),
        // The "jobnum" attribute was added by the extractScript method below.
        job("foo<span jobnum=\"1\"></span>baz", Job.JobType.HTML),
        job("{ extracted(); }", Job.JobType.JAVASCRIPT)
        );
    assertNoErrors();

    assertPipeline(
        job("foo<script type=text/vbscript>deleted()</script>baz",
            Job.JobType.HTML),
        job("foobaz", Job.JobType.HTML)
        );
    assertMessage(
        PluginMessageType.UNRECOGNIZED_CONTENT_TYPE, MessageLevel.WARNING);

    assertPipeline(
        job("foo<script type=\"text/javascript\">var x = 1;</script>baz",
            Job.JobType.HTML),
        job("foo<span jobnum=\"1\"></span>baz", Job.JobType.HTML),
        job("{\n  var x = 1;\n}", Job.JobType.JAVASCRIPT)
        );
    assertNoErrors();

    assertPipeline(
        job("foo<script type=\"text/javascript\""
            + ">useXml(<xml>foo</xml>);</script>baz",
            Job.JobType.HTML),
        job("foobaz", Job.JobType.HTML)
        );
    assertMessage(
        MessageType.UNEXPECTED_TOKEN,
        MessageLevel.ERROR,
        FilePosition.instance(is, 1, 42, 42, 1),
        MessagePart.Factory.valueOf("<"));

    assertPipeline(
        job("foo<script type=text/javascript>extracted();</script>baz",
            Job.JobType.HTML),
        job("foo<span jobnum=\"1\"></span>baz", Job.JobType.HTML),
        job("{ extracted(); }", Job.JobType.JAVASCRIPT)
        );
    assertNoErrors();

    assertPipeline(
        job("foo<script></script>baz", Job.JobType.HTML),
        job("foobaz", Job.JobType.HTML)
        );
    assertNoErrors();
  }

  public final void testStyleExtraction() throws Exception {
    assertPipeline(
        job("Foo<style>p { color: blue }</style><p>Bar", Job.JobType.HTML),
        job("Foo<p>Bar</p>", Job.JobType.HTML),
        job("p {\n  color: blue\n}", Job.JobType.CSS));
    assertNoErrors();

    assertPipeline(
        job("Foo<link rel=stylesheet href=content:p+%7Bcolor%3A+blue%7D><p>Bar",
            Job.JobType.HTML),
        job("Foo<p>Bar</p>", Job.JobType.HTML),
        job("p {\n  color: blue\n}", Job.JobType.CSS));
    assertNoErrors();

    assertPipeline(
        job("Foo<style></style><p>Bar", Job.JobType.HTML),
        job("Foo<p>Bar</p>", Job.JobType.HTML));
    assertNoErrors();
  }

  public final void testOnLoadHandlers() throws Exception {
    assertPipeline(
        job("<body onload=init();>Foo</body>", Job.JobType.HTML),
        job("<html><head></head>"
            + "<body>Foo<span jobnum=\"1\"></span></body></html>",
            Job.JobType.HTML),
        job("{ init(); }", Job.JobType.JAVASCRIPT));
    assertNoErrors();
  }

  public final void testImportedStyles() throws Exception {
    assertPipeline(
        job("<style>@import 'styles.css';</style>", Job.JobType.HTML),
        job("", Job.JobType.HTML),
        job("@import url('styles.css');", Job.JobType.CSS)
        );
    assertNoErrors();
  }

  public final void testTypeAndMediaAttributes() throws Exception {
    assertPipeline(
        job("<link rel=stylesheet media=screen href=content:p+%7B%7D>",
            Job.JobType.HTML),
        job("", Job.JobType.HTML),
        job("@media screen {\n  p {\n  }\n}", Job.JobType.CSS));
    assertNoErrors();

    assertPipeline(
        job("<link rel=stylesheet type=text/css href=content:p+%7B%7D>",
            Job.JobType.HTML),
        job("", Job.JobType.HTML),
        job("p {\n}", Job.JobType.CSS));
    assertNoErrors();

    assertPipeline(
        job("<link rel=stylesheet media=all href=content:p+%7B%7D>",
            Job.JobType.HTML),
        job("", Job.JobType.HTML),
        job("p {\n}", Job.JobType.CSS));
    assertNoErrors();

    assertPipeline(
        job("<link rel=stylesheet media=braille,tty type=text/css"
            + " href=content:p+%7B%7D>",
            Job.JobType.HTML),
        job("", Job.JobType.HTML),
        job("@media braille, tty {\n  p {\n  }\n}", Job.JobType.CSS));
    assertNoErrors();
  }

  public final void testDeferredScripts() throws Exception {
    assertPipeline(
        job("<script src=content:a();></script>"
            + "<script defer>b();</script>"
            + "<script src=content:c(); defer=defer></script>"
            + "<script src=content:d(); defer=no></script>"
            + "<br>",
            Job.JobType.HTML),
        job("<span jobnum=\"1\"></span><span jobnum=\"2\"></span><br />"
            + "<span jobnum=\"3\"></span><span jobnum=\"4\"></span>",
            Job.JobType.HTML),
        job("{ a(); }", Job.JobType.JAVASCRIPT),
        job("{ d(); }", Job.JobType.JAVASCRIPT),
        job("{ b(); }", Job.JobType.JAVASCRIPT),
        job("{ c(); }", Job.JobType.JAVASCRIPT));
    assertNoErrors();
  }

  public final void testUnloadableScripts() throws Exception {
    assertPipeline(
        job("<script src=content:onerror=panic;></script>"
            + "<script src=\"http://bogus.com/bogus.js#'!\"></script>"
            + "<script src=content:foo()></script>",
            Job.JobType.HTML),
        job("<span jobnum=\"1\"></span><span jobnum=\"2\"></span>"
            + "<span jobnum=\"3\"></span>",
            Job.JobType.HTML),
        job("{\n  onerror = panic;\n}", Job.JobType.JAVASCRIPT),
        job("{\n  throw new Error("
                     + "'Failed to load http://bogus.com/bogus.js#\\'!');\n}",
            Job.JobType.JAVASCRIPT),
        job("{ foo(); }", Job.JobType.JAVASCRIPT));
    assertNoErrors();
  }

  @Override
  protected boolean runPipeline(Jobs jobs) throws Exception {
    mq.getMessages().clear();
    boolean result = new RewriteHtmlStage().apply(jobs);
    // Dump the extracted script bits on the queue.
    for (Job job : new ArrayList<Job>(jobs.getJobsByType(JobType.HTML))) {
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
          jobs.getJobs().add(new Job(AncestorChain.instance(extracted)));
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
