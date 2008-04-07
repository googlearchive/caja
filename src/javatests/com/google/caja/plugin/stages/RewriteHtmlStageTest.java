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

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import java.util.ArrayList;

/**
 *
 * @author mikesamuel@gmail.com
 */
public final class RewriteHtmlStageTest extends PipelineStageTestCase {
  public void testScriptExtraction() throws Exception {
    assertPipeline(
        job("foo<script>extracted();</script>baz", Job.JobType.HTML),
        job("foo<span></span>baz", Job.JobType.HTML),
        job("{\n  extracted();\n}", Job.JobType.JAVASCRIPT)
        );

    assertPipeline(
        job("foo<script type=text/vbscript>deleted()</script>baz",
            Job.JobType.HTML),
        job("foobaz", Job.JobType.HTML)
        );

    assertPipeline(
        job("foo<script type=text/javascript>extracted();</script>baz",
            Job.JobType.HTML),
        job("foo<span></span>baz", Job.JobType.HTML),
        job("{\n  extracted();\n}", Job.JobType.JAVASCRIPT)
        );
  }

  public void testStyleExtraction() throws Exception {
    assertPipeline(
        job("Foo<style>p { color: blue }</style><p>Bar", Job.JobType.HTML),
        job("Foo<p>Bar</p>", Job.JobType.HTML),
        job("p {\n  color: blue\n}", Job.JobType.CSS));

    assertPipeline(
        job("Foo<link rel=stylesheet href=content:p+%7Bcolor%3A+blue%7D><p>Bar",
            Job.JobType.HTML),
        job("Foo<p>Bar</p>", Job.JobType.HTML),
        job("p {\n  color: blue\n}", Job.JobType.CSS));
  }

  public void testOnLoadHandlers() throws Exception {
    assertPipeline(
        job("<body onload=init();>Foo</body>", Job.JobType.HTML),
        job("<html><head></head><body>Foo<span></span></body></html>",
            Job.JobType.HTML),
        job("{\n  init();\n}", Job.JobType.JAVASCRIPT));
  }

  @Override
  protected boolean runPipeline(final Jobs jobs) throws Exception {
    boolean result = new RewriteHtmlStage().apply(jobs);
    // Dump the extracted script bits on the queue.
    for (Job job : new ArrayList<Job>(jobs.getJobs())) {
      job.getRoot().cast(ParseTreeNode.class).node.acceptPreOrder(new Visitor() {
          public boolean visit(AncestorChain<?> ac) {
            Block extracted = ac.node.getAttributes()
                .get(RewriteHtmlStage.EXTRACTED_SCRIPT_BODY);
            if (extracted != null) {
              jobs.getJobs().add(new Job(new AncestorChain<Block>(extracted)));
            }
            return true;
          }
        }, null);
    }
    return result;
  }
}
