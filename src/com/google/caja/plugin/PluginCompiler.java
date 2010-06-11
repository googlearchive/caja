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
import com.google.caja.parser.html.Dom;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.quasiliteral.ModuleManager;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.util.ContentType;
import com.google.caja.util.Criterion;
import com.google.caja.util.Lists;
import com.google.caja.util.Pipeline;

import java.net.URI;
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
  private Planner.PlanState preconditions;
  private Planner.PlanState goals;

  public PluginCompiler(BuildInfo buildInfo, PluginMeta meta, MessageQueue mq) {
    this.buildInfo = buildInfo;
    this.jobs = new Jobs(new MessageContext(), mq, meta);
    this.cssSchema = CssSchema.getDefaultCss21Schema(mq);
    this.htmlSchema = HtmlSchema.getDefault(mq);
    this.preconditions = PipelineMaker.DEFAULT_PRECONDS;
    this.goals = PipelineMaker.DEFAULT_GOALS;
  }

  public MessageQueue getMessageQueue() { return jobs.getMessageQueue(); }

  public MessageContext getMessageContext() { return jobs.getMessageContext(); }

  public void setMessageContext(MessageContext inputMessageContext) {
    assert null != inputMessageContext;
    jobs.setMessageContext(inputMessageContext);
  }

  public PluginMeta getPluginMeta() { return jobs.getPluginMeta(); }

  public final Planner.PlanState getPreconditions() { return preconditions; }

  public void setPreconditions(Planner.PlanState s) { preconditions = s; }

  public final Planner.PlanState getGoals() { return goals; }

  public void setGoals(Planner.PlanState s) { goals = s; }

  public Jobs getJobs() { return jobs; }

  public void setCssSchema(CssSchema cssSchema) {
    this.cssSchema = cssSchema;
    this.compilationPipeline = null;
  }

  public void setHtmlSchema(HtmlSchema htmlSchema) {
    this.htmlSchema = htmlSchema;
    this.compilationPipeline = null;
  }

  public void addInput(AncestorChain<?> input, URI baseUri) {
    jobs.getJobs().add(Job.job(null, input, baseUri));
    jobs.getMessageContext().addInputSource(
        input.node.getFilePosition().source());
  }

  @Deprecated
  public void addInput(AncestorChain<?> input) {
    addInput(input, input.node.getFilePosition().source().getUri());
  }

  /**
   * The list of parse trees that comprise the plugin after run has been called.
   * Valid after run has been called.
   */
  public List<? extends ParseTreeNode> getOutputs() {
    List<ParseTreeNode> outputs = Lists.newArrayList();
    ParseTreeNode js = getJavascript();
    if (js != null) { outputs.add(js); }
    return outputs;
  }

  private final void setupCompilationPipeline()
      throws Planner.UnsatisfiableGoalException {
    compilationPipeline = new Pipeline<Jobs>() {
      final long t0 = System.nanoTime();
      @Override
      protected boolean applyStage(
          Pipeline.Stage<? super Jobs> stage, Jobs jobs) {
        long t1 = System.nanoTime();
        jobs.getMessageQueue().addMessage(
            MessageType.CHECKPOINT,
            MessagePart.Factory.valueOf(stage.getClass().getSimpleName()),
            MessagePart.Factory.valueOf((t1 - t0) / 1e9 /* ns/s */));
        return super.applyStage(stage, jobs);
      }
    };

    ModuleManager moduleMgr = new ModuleManager(
        buildInfo, jobs.getPluginMeta().getUriFetcher(), true,
        jobs.getMessageQueue());
    new PipelineMaker(cssSchema, htmlSchema, moduleMgr, preconditions, goals)
        .populate(compilationPipeline.getStages());
  }

  public final Pipeline<Jobs> getCompilationPipeline()
      throws Planner.UnsatisfiableGoalException {
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
            return job.getType() == ContentType.HTML;
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
            return job.getType() == ContentType.JS;
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
    try {
      return getCompilationPipeline().apply(jobs);
    } catch (Planner.UnsatisfiableGoalException ex) {
      jobs.getMessageQueue().addMessage(
          PluginMessageType.INVALID_PIPELINE,
          MessagePart.Factory.valueOf("" + preconditions),
          MessagePart.Factory.valueOf("" + goals));
      return false;
    }
  }
}
