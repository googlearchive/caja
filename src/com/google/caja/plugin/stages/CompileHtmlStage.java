// Copyright (C) 2007 Google Inc.
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
import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.InputSource;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.Dom;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.js.Block;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.JobEnvelope;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.templates.TemplateCompiler;
import com.google.caja.plugin.templates.TemplateSanitizer;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.ContentType;
import com.google.caja.util.Lists;
import com.google.caja.util.Multimap;
import com.google.caja.util.Multimaps;
import com.google.caja.util.Pair;
import com.google.caja.util.Pipeline;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Node;

/**
 * Compile the HTML and CSS to javascript.
 *
 * @author mikesamuel@gmail.com
 */
abstract class CompileHtmlStage implements Pipeline.Stage<Jobs> {
  private final CssSchema cssSchema;
  private final HtmlSchema htmlSchema;

  public CompileHtmlStage(CssSchema cssSchema, HtmlSchema htmlSchema) {
    if (null == cssSchema) { throw new NullPointerException(); }
    if (null == htmlSchema) { throw new NullPointerException(); }
    this.cssSchema = cssSchema;
    this.htmlSchema = htmlSchema;
  }

  public boolean apply(Jobs jobs) {
    Multimap<JobCache.Keys, Job> byKey = Multimaps.newListHashMultimap();
    for (Iterator<JobEnvelope> it = jobs.getJobs().iterator(); it.hasNext();) {
      JobEnvelope env = it.next();
      if (env.fromCache) { continue; }
      switch (env.job.getType()) {
        case CSS:
        case HTML:
          byKey.put(env.cacheKeys, env.job);
          it.remove();
          break;
        default: break;
      }
    }

    for (JobCache.Keys cacheKeys : byKey.keySet()) {
      URI baseUri = null;
      List<Pair<Node, URI>> ihtmlRoots = Lists.newArrayList();
      List<CssTree.StyleSheet> stylesheets = Lists.newArrayList();
      for (Job job : byKey.get(cacheKeys)) {
        switch (job.getType()) {
          case HTML:
            // TODO(ihab.awad): We do *not* want to support multiple HTML files
            // being cajoled at once since this can be mis-used for modularity
            // and we set up expectations on the part of our users to
            // maintain this behavior, regardless of whatever complexity that
            // might entail.
            ihtmlRoots.add(Pair.pair(
                ((Dom) job.getRoot()).getValue(), job.getBaseUri()));
            if (baseUri == null) { baseUri = job.getBaseUri(); }
            break;
          case CSS:
            stylesheets.add((CssTree.StyleSheet) job.getRoot());
            break;
          default: throw new SomethingWidgyHappenedError(job.getType().name());
        }
      }
      if (baseUri == null) { baseUri = InputSource.UNKNOWN.getUri(); }

      MessageQueue mq = jobs.getMessageQueue();

      TemplateSanitizer ts = new TemplateSanitizer(htmlSchema, mq);
      for (Pair<Node, URI> ihtmlRoot : ihtmlRoots) { ts.sanitize(ihtmlRoot.a); }
      TemplateCompiler tc = new TemplateCompiler(
          ihtmlRoots, stylesheets, cssSchema, htmlSchema,
          jobs.getPluginMeta(), jobs.getMessageContext(), mq);
      Pair<Node, List<Block>> htmlAndJs = tc.getSafeHtml(
          DomParser.makeDocument(null, null));

      Job outJob = makeJobFromHtml(htmlAndJs.a, baseUri);
      jobs.getJobs().add(
          new JobEnvelope(null, cacheKeys, ContentType.HTML, false, outJob));

      for (Block bl : htmlAndJs.b) {
        jobs.getJobs().add(new JobEnvelope(
            null, cacheKeys, ContentType.JS, false, Job.jsJob(bl, baseUri)));
      }
    }

    return jobs.hasNoFatalErrors();
  }

  abstract Job makeJobFromHtml(Node html, URI baseUri);
}
