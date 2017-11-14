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
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.escaping.UriUtil;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParserBase;
import com.google.caja.parser.html.Dom;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.NullLiteral;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.plugin.DataUriFetcher;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.JobEnvelope;
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
import com.google.common.collect.Lists;
import com.google.caja.util.Pair;
import com.google.caja.util.Strings;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import junit.framework.ComparisonFailure;

/**
 * An abstract test framework for pipeline stages.
 *
 * @author mikesamuel@gmail.com
 */
public abstract class PipelineStageTestCase extends CajaTestCase {
  protected PluginMeta meta;
  protected JobCache cache = new StubJobCache();
  private TestUriFetcher uriFetcher;

  protected PluginMeta createPluginMeta() {
    return new PluginMeta(uriFetcher, new TestUriPolicy());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    uriFetcher = new TestUriFetcher();
    meta = createPluginMeta();
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

  private void assertOutputJobs(JobStub[] outputJobs, Jobs jobs)
      throws ParseException {
    Map<String, ParseTreeNode> quasiBindings =
      new LinkedHashMap<String, ParseTreeNode>();
    List<JobEnvelope> jobList = jobs.getJobs();
    List<JobStub> actualJobs = Lists.newArrayList();

    // First we attempt to match outputJobs with jobs so that we can reduce
    // brittleness due to auto-generated ID mismatches by using quasi
    // substitution to rewrite "@foo_id__" style quasi nodes in outputJobs with
    // IDs that appear in jobs.
    outputJobs = outputJobs.clone();
    for (int i = 0, n = Math.min(jobList.size(), outputJobs.length);
         i < n; ++i) {
      JobStub golden = outputJobs[i];
      JobEnvelope actual = jobList.get(i);

      if (golden.type == actual.job.getType()) {
        switch (golden.type) {
          case JS:
            if (QuasiBuilder.match(
                golden.content, actual.job.getRoot(), quasiBindings)) {
              ParseTreeNode bodyWithIdsReplaced = QuasiBuilder.subst(
                  golden.content, quasiBindings);
              golden = new JobStub(
                  render(bodyWithIdsReplaced), golden.type);
            }
            break;
          case HTML:
            DocumentFragment html = ((Dom) actual.job.getRoot()).getValue();
            if (matchHtmlQuasi(golden.content, html, quasiBindings)) {
              html = substHtmlQuasi(golden.content, quasiBindings);
              if (html != null) {
                golden = new JobStub(Nodes.render(html), golden.type);
              }
            }
            break;
          default: break;
        }
      }
      outputJobs[i] = golden;

      // Second, we convert jobs to JobStubs so we can compare apples to oranges.
      StringBuilder sb = new StringBuilder();
      ParseTreeNode node = actual.job.getRoot();
      TokenConsumer tc = node.makeRenderer(sb, null);
      node.render(new RenderContext(tc));
      tc.noMoreTokens();
      actualJobs.add(new JobStub(sb.toString(), actual.job.getType()));
    }

    // Finally we do the actual comparison.
    if (!Arrays.asList(outputJobs).equals(actualJobs)) {
      throw new ComparisonFailure(
          null,
          prettyPrintedJobStubs(Arrays.asList(outputJobs)),
          prettyPrintedJobStubs(actualJobs));
    }
  }

  private void parseJob(JobStub inputJob, Jobs outputJobs)
      throws ParseException {
    switch (inputJob.type) {
      case HTML:
        outputJobs.getJobs().add(JobEnvelope.of(Job.domJob(
            Dom.transplant(html(fromString(inputJob.content, is))),
            is.getUri())));
        break;
      case CSS:
        outputJobs.getJobs().add(JobEnvelope.of(
            Job.cssJob(css(fromString(inputJob.content, is)), is.getUri())));
        break;
      case JS:
        outputJobs.getJobs().add(JobEnvelope.of(
            Job.jsJob(js(fromString(inputJob.content, is)), null)));
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

  /** Create a stub job object for a document-ified html fragment. */
  protected static JobStub htmlJobBody(String content) {
    return new JobStub(
        "<html><head></head><body>" + content + "</body></html>",
        ContentType.HTML);
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

  /**
   * Compares the HTML content in {@code htmlTemplate} structurally to node, and
   * looks for quasi identifiers in attribute values, treating an attribute value
   * as equivalent to a JS string literal so that literal IDs in JavaScript can be
   * correlated with IDs in HTML elements.
   */
  private boolean matchHtmlQuasi(
      String htmlTemplate, DocumentFragment node,
      Map<String, ParseTreeNode> bindings) throws ParseException {
    DocumentFragment quasi = htmlFragment(
        fromString(htmlTemplate, FilePosition.UNKNOWN));
    return matchHtmlQuasi(quasi, node, bindings);
  }

  private boolean matchHtmlQuasi(
      Node tmpl, Node inp, Map<String, ParseTreeNode> bindings) {
    if (tmpl.getNodeType() != inp.getNodeType()) {
      return false;
    }
    if (!Strings.eqIgnoreCase(tmpl.getNodeName(), inp.getNodeName())) {
      return false;
    }
    String tmplValue = tmpl.getNodeValue();
    String inpValue = inp.getNodeValue();
    if (tmplValue == null ? inpValue != null : !tmplValue.equals(inpValue)) {
      return false;
    }
    Node tmplChild = tmpl.getFirstChild();
    Node inpChild = inp.getFirstChild();
    for (; tmplChild != null && inpChild != null;
         tmplChild = tmplChild.getNextSibling(),
         inpChild = inpChild.getNextSibling()) {
      if (!matchHtmlQuasi(tmplChild, inpChild, bindings)) { return false; }
    }
    if (inpChild != null || tmplChild != null) { return false; }
    if (tmpl.getNodeType() == Node.ELEMENT_NODE) {
      Element inpEl = (Element) inp;
      Element tmplEl = (Element) tmpl;
      for (Attr inpAttr : Nodes.attributesOf(inpEl)) {
        if (!tmplEl.hasAttributeNS(
              inpAttr.getNamespaceURI(), inpAttr.getLocalName())) {
          return false;
        }
      }
      for (Attr tmplAttr : Nodes.attributesOf(tmplEl)) {
        Attr inpAttr = inpEl.getAttributeNodeNS(
           tmplAttr.getNamespaceURI(), tmplAttr.getLocalName());
        String tmplAttrValue = tmplAttr.getValue();
        if (ParserBase.isQuasiIdentifier(tmplAttrValue)) {
          String key = tmplAttrValue.substring(1);
          if (bindings.containsKey(key)) {
            ParseTreeNode n = bindings.get(key);
            if (n instanceof StringLiteral && inpAttr != null
                && ((StringLiteral) n).getUnquotedValue().equals(
                    inpAttr.getValue())) {
              // OK.  Two uses of quasi bind to same value.
            } else {
              return false;
            }
          } else {
            bindings.put(
                key,
                (inpAttr != null)
                ? StringLiteral.valueOf(
                    FilePosition.UNKNOWN, inpAttr.getValue())
                : new NullLiteral(FilePosition.UNKNOWN));
          }
        } else if (inpAttr == null
                   || !tmplAttrValue.equals(inpAttr.getValue())){
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Looks for quasi identifiers in attribute values, substituting attribute values
   * stored as string literals in bindings.  Removes attributes where the
   * corresponding binding is a {@code NullLiteral}.
   */
  private DocumentFragment substHtmlQuasi(
      String htmlTemplate, Map<String, ParseTreeNode> bindings)
      throws ParseException {
    DocumentFragment quasi = htmlFragment(
        fromString(htmlTemplate, FilePosition.UNKNOWN));
    if (substHtmlQuasi(quasi, bindings)) {
      return quasi;
    } else {
      return null;
    }
  }

  private boolean substHtmlQuasi(Node node, Map<String, ParseTreeNode> bindings) {
    if (node instanceof Element) {
      Element el = (Element) node;
      for (Attr attr : Lists.newArrayList(Nodes.attributesOf(el))) {
        String value = attr.getValue();
        if (ParserBase.isQuasiIdentifier(value)) {
          String key = value.substring(1);
          ParseTreeNode newValue = bindings.get(key);
          if (newValue instanceof StringLiteral) {
            String content = ((StringLiteral) newValue).getUnquotedValue();
            attr.setValue(content);
          } else if (newValue instanceof NullLiteral) {
            // There was no corresponding attribute in the matched DOM when
            // we did matchHtmlQuasi, or some JS quasi matched with (null).
            el.removeAttributeNode(attr);
          } else {
            return false;
          }
        }
      }
    }
    for (Node child : Nodes.childrenOf(node)) {
      if (!substHtmlQuasi(child, bindings)) { return false; }
    }
    return true;
  }

  private static String prettyPrintedJobStubs(
      Iterable<? extends JobStub> stubs) {
    StringBuilder sb = new StringBuilder();
    int i = 0;
    for (JobStub goldenStub : stubs) {
      sb.append(i++).append('\n').append(goldenStub.content).append('\n');
    }
    return sb.toString();
  }
}
