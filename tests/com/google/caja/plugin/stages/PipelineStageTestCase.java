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

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.escaping.UriUtil;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.html.Dom;
import com.google.caja.plugin.DataUriFetcher;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.LoaderType;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.UriEffect;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.plugin.UriPolicy;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.ContentType;
import com.google.caja.util.Join;
import com.google.caja.util.Lists;
import com.google.caja.util.MoreAsserts;
import com.google.caja.util.Pair;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * An abstract test framework for pipeline stages.
 *
 * @author mikesamuel@gmail.com
 */
public abstract class PipelineStageTestCase extends CajaTestCase {
  protected PluginMeta meta;
  private TestUriFetcher uriFetcher;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    uriFetcher = new TestUriFetcher();
    meta = new PluginMeta(uriFetcher, new TestUriPolicy());
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    meta = null;
    uriFetcher = null;
  }

  public PluginMeta getMeta() { return meta; }

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
    parseJob(inputJob, jobs);
    assertTrue(runPipeline(jobs));
    assertOutputJobs(outputJobs, jobs);
  }

  protected void assertPipelineFails(JobStub inputJob, JobStub... outputJobs)
      throws Exception {
    Jobs jobs = new Jobs(mc, mq, meta);
    parseJob(inputJob, jobs);
    assertFalse(runPipeline(jobs));
    assertOutputJobs(outputJobs, jobs);
  }

  private void assertOutputJobs(JobStub[] outputJobs, Jobs jobs) {
    List<JobStub> actualJobs = Lists.newArrayList();
    for (Job job : jobs.getJobs()) {
      StringBuilder sb = new StringBuilder();
      ParseTreeNode node = job.getRoot();
      TokenConsumer tc = node.makeRenderer(sb, null);
      node.render(new RenderContext(tc));
      tc.noMoreTokens();
      actualJobs.add(new JobStub(sb.toString(), job.getType()));
    }
    MoreAsserts.assertListsEqual(Arrays.asList(outputJobs), actualJobs);
  }

  private void parseJob(JobStub inputJob, Jobs outputJobs)
      throws ParseException {
    switch (inputJob.type) {
      case HTML:
        outputJobs.getJobs().add(
            Job.domJob(
                null, new Dom(htmlFragment(fromString(inputJob.content, is))),
                is.getUri()));
        break;
      case CSS:
        outputJobs.getJobs().add(
            Job.cssJob(null, css(fromString(inputJob.content, is)), is.getUri())
            );
        break;
      case JS:
        outputJobs.getJobs().add(
            Job.jsJob(null, js(fromString(inputJob.content, is)), null));
        break;
      default:
        throw new IllegalArgumentException(inputJob.type.name());
    }
  }

  protected void addUrlToPluginEnvironment(String uri, String content) {
    URI absUrl = is.getUri().resolve(uri);
    addUrlToPluginEnvironment(
        absUrl, fromString(content, new InputSource(absUrl)));
  }

  protected void addUrlToPluginEnvironment(URI uri, CharProducer cp) {
    URI absUrl = is.getUri().resolve(uri);
    uriFetcher.filesToLoad.add(Pair.pair(absUrl, cp));
  }

  protected abstract boolean runPipeline(Jobs jobs) throws Exception;

  /** Create a stub job object. */
  protected static JobStub job(String content, ContentType type) {
    return new JobStub(content, type);
  }

  protected static JobStub job(ContentType type, String... contentLines) {
    return new JobStub(Join.join("\n", contentLines), type);
  }

  protected static final class JobStub {
    final String content;
    final ContentType type;

    JobStub(String content, ContentType type) {
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

  private static String decode(String s) {
    try {
      return URLDecoder.decode(s, "UTF-8");
    } catch (UnsupportedEncodingException ex) {
      throw new SomethingWidgyHappenedError(ex);
    }
  }

  private static class TestUriFetcher implements UriFetcher {
    private List<Pair<URI, CharProducer>> filesToLoad = Lists.newArrayList();
    private DataUriFetcher dataFetcher = new DataUriFetcher();

    public FetchedData fetch(ExternalReference ref, String mimeType)
        throws UriFetchException {
      URI uri = ref.getUri();
      for (Iterator<Pair<URI, CharProducer>> it = filesToLoad.iterator();
           it.hasNext();) {
        Pair<URI, CharProducer> entry = it.next();
        if (ref.getUri().equals(entry.a)) {
          it.remove();
          return FetchedData.fromCharProducer(
              entry.b.clone(), mimeType, "uTF-8");
        }
      }
      if ("data".equals(uri.getScheme())) {
        return dataFetcher.fetch(ref, mimeType);
      }
      if (!"content".equals(uri.getScheme())) {
        throw new UriFetchException(ref, mimeType);
      }
      return FetchedData.fromCharProducer(
          CharProducer.Factory.fromString(
              decode(uri.getRawSchemeSpecificPart()),
              new InputSource(uri)),
          mimeType, "UTF-8");
    }
  }

  private static class TestUriPolicy implements UriPolicy {
    public String rewriteUri(
        ExternalReference u, UriEffect effect, LoaderType loader,
        Map<String, ?> hints) {
      return "http://proxy/?uri=" + UriUtil.encode(u.getUri().toString())
          + "&effect=" + effect + "&loader=" + loader;
    }
  }
}
