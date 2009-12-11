// Copyright (C) 2006 Google Inc.
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

package com.google.caja.plugin;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.plugin.stages.CheckForErrorsStage;
import com.google.caja.plugin.stages.CompileHtmlStage;
import com.google.caja.plugin.stages.ConsolidateCodeStage;
import com.google.caja.plugin.stages.DebuggingSymbolsStage;
import com.google.caja.plugin.stages.InferFilePositionsStage;
import com.google.caja.plugin.stages.InlineCssImportsStage;
import com.google.caja.plugin.stages.OpenTemplateStage;
import com.google.caja.plugin.stages.RewriteCssStage;
import com.google.caja.plugin.stages.RewriteHtmlStage;
import com.google.caja.plugin.stages.SanitizeHtmlStage;
import com.google.caja.plugin.stages.ValidateCssStage;
import com.google.caja.plugin.stages.ValidateJavascriptStage;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.util.Criterion;
import com.google.caja.util.Pipeline;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;

/**
 * Compiles a bundle of CSS, javascript, and HTML files to a sandboxed
 * javascript and HTML widget.
 *
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public final class PluginCompiler {
  private final BuildInfo buildInfo;
  private final Jobs jobs;
  /**
   * A configurable pipeline that performs the compilation of HTML, CSS, and JS
   * to CSS and JS.
   */
  private Pipeline<Jobs> compilationPipeline;
  private CssSchema cssSchema;
  private HtmlSchema htmlSchema;

  public PluginCompiler(BuildInfo buildInfo, PluginMeta meta, MessageQueue mq) {
    this.buildInfo = buildInfo;
    MessageContext mc = new MessageContext();
    jobs = new Jobs(mc, mq, meta);
    cssSchema = CssSchema.getDefaultCss21Schema(mq);
    htmlSchema = HtmlSchema.getDefault(mq);
  }

  public MessageQueue getMessageQueue() { return jobs.getMessageQueue(); }

  public MessageContext getMessageContext() { return jobs.getMessageContext(); }

  public void setMessageContext(MessageContext inputMessageContext) {
    assert null != inputMessageContext;
    jobs.setMessageContext(inputMessageContext);
  }

  public PluginMeta getPluginMeta() { return jobs.getPluginMeta(); }

  public Jobs getJobs() { return jobs; }

  public void setCssSchema(CssSchema cssSchema) {
    this.cssSchema = cssSchema;
    this.compilationPipeline = null;
  }

  public void setHtmlSchema(HtmlSchema htmlSchema) {
    this.htmlSchema = htmlSchema;
    this.compilationPipeline = null;
  }

  public void addInput(AncestorChain<?> input) {
    jobs.getJobs().add(new Job(input));
    jobs.getMessageContext().addInputSource(
        input.node.getFilePosition().source());
  }

  /**
   * The list of parse trees that comprise the plugin after run has been called.
   * Valid after run has been called.
   */
  public List<? extends ParseTreeNode> getOutputs() {
    List<ParseTreeNode> outputs = new ArrayList<ParseTreeNode>();
    ParseTreeNode js = getJavascript();
    if (js != null) { outputs.add(js); }
    return outputs;
  }

  protected void setupCompilationPipeline() {
    compilationPipeline = new Pipeline<Jobs>() {
      final long t0 = System.nanoTime();
      @Override
      protected boolean applyStage(
          Pipeline.Stage<? super Jobs> stage, Jobs jobs) {
        jobs.getMessageQueue().addMessage(
            MessageType.CHECKPOINT,
            MessagePart.Factory.valueOf(stage.getClass().getSimpleName()),
            MessagePart.Factory.valueOf((System.nanoTime() - t0) / 1e9));
        return super.applyStage(stage, jobs);
      }
    };

    List<Pipeline.Stage<Jobs>> stages = compilationPipeline.getStages();
    stages.add(new RewriteHtmlStage());
    stages.add(new InlineCssImportsStage());
    stages.add(new SanitizeHtmlStage(htmlSchema));
    stages.add(new ValidateCssStage(cssSchema, htmlSchema));
    stages.add(new RewriteCssStage());
    stages.add(new CompileHtmlStage(cssSchema, htmlSchema));
    stages.add(new OpenTemplateStage());
    stages.add(new ConsolidateCodeStage());
    stages.add(new ValidateJavascriptStage(buildInfo));
    stages.add(new InferFilePositionsStage());
    stages.add(new DebuggingSymbolsStage());
    stages.add(new CheckForErrorsStage());
  }

  public Pipeline<Jobs> getCompilationPipeline() {
    if (compilationPipeline == null) { setupCompilationPipeline(); }
    return compilationPipeline;
  }

  /**
   * If the HTML has been compiled and consolidated, return the static HTML
   * portion of the gadget.
   * @return null if no HTML portion.
   */
  public Node getStaticHtml() {
    Job soleHtmlJob = getConsolidatedOutput(new Criterion<Job>() {
          public boolean accept(Job job) {
            return job.getType() == Job.JobType.HTML;
          }
        });
    return soleHtmlJob != null
        ? soleHtmlJob.getRoot().cast(Dom.class).node.getValue() : null;
  }

  /**
   * If the javascript has been compiled and consolidated, return the script
   * portion of the gadget.
   * @return null if no javascript portion.
   */
  public CajoledModule getJavascript() {
    Job soleJsJob = getConsolidatedOutput(new Criterion<Job>() {
          public boolean accept(Job job) {
            return job.getType() == Job.JobType.JAVASCRIPT;
          }
        });
    return soleJsJob != null ? (CajoledModule) soleJsJob.getRoot().node : null;
  }

  private Job getConsolidatedOutput(Criterion<Job> filter) {
    Job match = null;
    for (Job job : this.jobs.getJobs()) {
      if (filter.accept(job)) {
        if (match != null) {
          throw new SomethingWidgyHappenedError(
              "Not consolidated.  Check your pipeline.");
        }
        match = job;
      }
    }
    return match;
  }

  /**
   * Run the compiler on all parse trees added via {@link #addInput}.
   * The output parse tree is available via {@link #getJavascript()}.
   * @return true on success, false on failure.
   */
  public boolean run() {
    return getCompilationPipeline().apply(jobs);
  }
}
