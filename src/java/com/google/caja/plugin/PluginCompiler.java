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

import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.InputSource;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.js.Block;
import com.google.caja.plugin.stages.CheckForErrorsStage;
import com.google.caja.plugin.stages.CompileCssTemplatesStage;
import com.google.caja.plugin.stages.CompileGxpsStage;
import com.google.caja.plugin.stages.CompileHtmlStage;
import com.google.caja.plugin.stages.ConsolidateCodeStage;
import com.google.caja.plugin.stages.ConsolidateCssStage;
import com.google.caja.plugin.stages.RewriteHtmlStage;
import com.google.caja.plugin.stages.ValidateCssStage;
import com.google.caja.plugin.stages.ValidateHtmlStage;
import com.google.caja.plugin.stages.ValidateJavascriptStage;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.Criterion;
import com.google.caja.util.Pipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * Compiles a bundle of css, javascript, and gxp files to a sandboxed javascript
 * and css widget.
 *
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public final class PluginCompiler {
  private final Jobs jobs;
  /**
   * A configurable pipeline that performs the compilation of HTML, CSS, and JS
   * to CSS and JS.
   */
  private Pipeline<Jobs> compilationPipeline;
  private CssSchema cssSchema;
  private HtmlSchema htmlSchema;

  public PluginCompiler(PluginMeta meta, MessageQueue mq) {
    MessageContext mc = new MessageContext();
    mc.inputSources = new ArrayList<InputSource>();
    jobs = new Jobs(mc, mq, meta);
    cssSchema = CssSchema.getDefaultCss21Schema(mq);
    htmlSchema = HtmlSchema.getDefault(mq);
  }

  public MessageQueue getMessageQueue() { return jobs.getMessageQueue(); }

  public MessageContext getMessageContext() { return jobs.getMessageContext(); }

  public void setMessageContext(MessageContext inputMessageContext) {
    assert null != inputMessageContext;
    if (inputMessageContext.inputSources.isEmpty()) {
      inputMessageContext.inputSources = new ArrayList<InputSource>();
    }
    jobs.setMessageContext(inputMessageContext);
  }

  public PluginMeta getPluginMeta() { return jobs.getPluginMeta(); }

  public void setCssSchema(CssSchema cssSchema) {
    this.cssSchema = cssSchema;
    compilationPipeline = null;
  }

  public void setHtmlSchema(HtmlSchema htmlSchema) {
    this.htmlSchema = htmlSchema;
    compilationPipeline = null;
  }

  public void addInput(AncestorChain<?> input) {
    jobs.getJobs().add(new Job(input));
    jobs.getMessageContext().inputSources.add(
        input.node.getFilePosition().source());
  }

  /**
   * The list of parse trees that comprise the plugin after run has been called.
   * Valid after run has been called.
   */
  public List<? extends ParseTreeNode> getOutputs() {
    List<ParseTreeNode> outputs = new ArrayList<ParseTreeNode>();
    ParseTreeNode css = getCss(),
        js = getJavascript();
    if (css != null) { outputs.add(css); }
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
    stages.add(new ValidateHtmlStage(htmlSchema));
    stages.add(new CompileHtmlStage(cssSchema, htmlSchema));
    stages.add(new ValidateCssStage(cssSchema, htmlSchema));
    stages.add(new CompileGxpsStage(cssSchema, htmlSchema));
    stages.add(new CompileCssTemplatesStage(cssSchema));
    stages.add(new ConsolidateCodeStage());
    stages.add(new ValidateJavascriptStage());
    stages.add(new ConsolidateCssStage());
    stages.add(new CheckForErrorsStage());
  }

  protected Pipeline<Jobs> getCompilationPipeline() {
    if (compilationPipeline == null) { setupCompilationPipeline(); }
    return compilationPipeline;
  }

  /**
   * If the javascript has been compiled and consolidated, return the resulting
   * parse tree, otherwise return null.
   */
  public Block getJavascript() {
    Job soleJsJob = getConsolidatedOutput(new Criterion<Job>() {
          public boolean accept(Job job) {
            return job.getType() == Job.JobType.JAVASCRIPT;
          }
        });
    return soleJsJob != null ? (Block) soleJsJob.getRoot().node : null;
  }

  public CssTree getCss() {
    Job soleCssJob = getConsolidatedOutput(new Criterion<Job>() {
          public boolean accept(Job job) {
            return job.getType() == Job.JobType.CSS;
          }
        });
    return soleCssJob != null ? (CssTree) soleCssJob.getRoot().node : null;
  }

  private Job getConsolidatedOutput(Criterion<Job> filter) {
    Job match = null;
    for (Job job : this.jobs.getJobs()) {
      if (filter.accept(job)) {
        if (match != null) {
          throw new RuntimeException("Not consolidated.  Check your pipeline.");
        }
        match = job;
      }
    }
    return match;
  }

  /**
   * Run the compiler on all parse trees added via {@link #addInput}.
   * The output parse trees are available via {@link #getJavascript()} and
   * {@link #getCss}.
   * @return true on success, false on failure.
   */
  public boolean run() {
    return getCompilationPipeline().apply(jobs);
  }
}
