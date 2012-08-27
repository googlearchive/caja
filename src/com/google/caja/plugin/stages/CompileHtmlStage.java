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

import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.Dom;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.JobEnvelope;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.templates.IhtmlRoot;
import com.google.caja.plugin.templates.SafeHtmlChunk;
import com.google.caja.plugin.templates.SafeJsChunk;
import com.google.caja.plugin.templates.ScriptPlaceholder;
import com.google.caja.plugin.templates.TemplateCompiler;
import com.google.caja.plugin.templates.TemplateSanitizer;
import com.google.caja.plugin.templates.ValidatedStylesheet;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Lists;
import com.google.caja.util.Pair;
import com.google.caja.util.Pipeline;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.DocumentFragment;

/**
 * Compile the HTML, CSS, and JS to HTML and JS.
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
    List<IhtmlRoot> html = Lists.newArrayList();
    List<ValidatedStylesheet> css = Lists.newArrayList();
    List<ScriptPlaceholder> js = Lists.newArrayList();
    URI baseUriForJsModules = null;

    for (Iterator<JobEnvelope> it = jobs.getJobs().iterator(); it.hasNext();) {
      JobEnvelope env = it.next();
      Job job = env.job;
      switch (env.sourceType) {
        case CSS:
          if (!env.fromCache) {
            css.add(new ValidatedStylesheet(
                env, (CssTree.StyleSheet) job.getRoot(), job.getBaseUri()));
            it.remove();
          }
          break;
        case HTML:
          html.add(new IhtmlRoot(
              env, ((Dom) job.getRoot()).getValue(), job.getBaseUri()));
          // Module loading in embedded <script>s should use the URI of the
          // HTML file as the base URI. We use a heuristic that there's only
          // one HTML file per compilation task, and use the URI of that.
          if (baseUriForJsModules == null) {
            baseUriForJsModules = job.getBaseUri();
          }
          it.remove();
          break;
        case JS:
          if (env.placeholderId != null) {
            js.add(new ScriptPlaceholder(env, env.job.getRoot()));
            it.remove();
          }
          break;
        default: break;
      }
    }

    // TODO(ihab.awad): We do *not* want to support multiple HTML files
    // being cajoled at once since this can be mis-used for modularity
    // and we set up expectations on the part of our users to
    // maintain this behavior, regardless of whatever complexity that
    // might entail.

    MessageQueue mq = jobs.getMessageQueue();

    TemplateSanitizer ts = new TemplateSanitizer(htmlSchema, mq);
    for (IhtmlRoot ihtmlRoot : html) {
      ts.sanitize(ihtmlRoot.root);
    }

    TemplateCompiler tc = new TemplateCompiler(
        html, css, js, cssSchema, htmlSchema,
        jobs.getPluginMeta(), jobs.getMessageContext(), mq);
    Pair<List<SafeHtmlChunk>, List<SafeJsChunk>> htmlAndJs = tc.getSafeHtml(
        DomParser.makeDocument(null, null));

    for (SafeHtmlChunk outputHtml : htmlAndJs.a) {
      Job outJob = makeJobFromHtml(outputHtml.root, outputHtml.baseUri);
      if (outJob != null) {
        jobs.getJobs().add(outputHtml.source.withJob(outJob));
      }
    }

    for (SafeJsChunk outputJs : htmlAndJs.b) {
      if (outputJs.body instanceof Block) {  // Further processing required.
        assert !outputJs.source.fromCache;
        jobs.getJobs().add(outputJs.source.withJob(Job.jsJob(
            (Block) outputJs.body, baseUriForJsModules)));
      } else {  // Routed through from cache.
        assert outputJs.source.fromCache;
        jobs.getJobs().add(outputJs.source.withJob(Job.cajoledJob(
            (CajoledModule) outputJs.body)));
      }
    }

    return jobs.hasNoFatalErrors();
  }

  abstract Job makeJobFromHtml(DocumentFragment html, URI baseUri);
}
