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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.InputSource;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.DomTree;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.PluginEnvironment;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.MoreAsserts;
import com.google.caja.util.TestUtil;

import junit.framework.TestCase;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An abstract test framework for pipeline stages.
 *
 * @author mikesamuel@gmail.com
 */
public abstract class PipelineStageTestCase extends TestCase {
  protected MessageContext mc;
  protected MessageQueue mq;
  protected PluginMeta meta;
  protected InputSource is;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mc = new MessageContext();
    mq = TestUtil.createTestMessageQueue(mc);
    meta = new PluginMeta(
        "foo",
        new PluginEnvironment() {
          public CharProducer loadExternalResource(
              ExternalReference ref, String mimeType) {
            URI uri = ref.getUri();
            if (!"content".equals(uri.getScheme())) {
              return null;
            }
            return CharProducer.Factory.create(
                new StringReader(decode(uri.getRawSchemeSpecificPart())),
                new InputSource(uri));
          }

          public String rewriteUri(ExternalReference uri, String mimeType) {
            return "http://proxy/?uri=" + encode(uri.getUri().toString())
                + "&mimeType=" + encode(mimeType);
          }
        });
    is = new InputSource(URI.create("test:///" + getName()));
  }

  /**
   * Asserts that {@link #runPipeline} runs to completion on the given inputJob
   * and produces the given outputJobs.
   *
   * @param inputJob an initial job to seed the pipeline with.
   * @param outputJobs expected jobs after {@link #runPipeline} has been
   *     invoked.
   */
  protected void assertPipeline(JobStub inputJob, JobStub... outputJobs)
      throws Exception {
    Jobs jobs = new Jobs(mc, mq, meta);
    assertEquals(Job.JobType.HTML, inputJob.type);

    HtmlLexer lexer = new HtmlLexer(
        CharProducer.Factory.create(new StringReader(inputJob.content), is));
    DomTree fragment = new DomParser(lexer, is, mq).parseFragment();

    jobs.getJobs().add(new Job(new AncestorChain<DomTree>(fragment)));

    assertTrue(runPipeline(jobs));

    List<JobStub> actualJobs = new ArrayList<JobStub>();
    for (Job job : jobs.getJobs()) {
      StringBuilder sb = new StringBuilder();
      job.getRoot().node.render(
          new RenderContext(mc, job.getRoot().node.makeRenderer(sb, null)));
      actualJobs.add(new JobStub(sb.toString(), job.getType()));
    }

    MoreAsserts.assertListsEqual(Arrays.asList(outputJobs), actualJobs);
  }

  protected abstract boolean runPipeline(Jobs jobs) throws Exception;

  /** Create a stub job object. */
  protected static JobStub job(String content, Job.JobType type) {
    return new JobStub(content, type);
  }

  protected static final class JobStub {
    final String content;
    final Job.JobType type;

    JobStub(String content, Job.JobType type) {
      if (content == null || type == null) { throw new NullPointerException(); }
      this.content = content;
      this.type = type;
    }

    @Override
    public String toString() {
      return "(" + content + ": " + type + ")";
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof JobStub)) { return false; }
      JobStub that = (JobStub) o;
      return this.content.equals(that.content) && this.type == that.type;
    }

    @Override
    public int hashCode() {
      return content.hashCode() + 31 * type.hashCode();
    }
  }

  private static String encode(String s) {
    try {
      return URLEncoder.encode(s, "UTF-8");
    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static String decode(String s) {
    try {
      return URLDecoder.decode(s, "UTF-8");
    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex);
    }
  }
}
