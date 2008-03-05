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

package com.google.caja.plugin;

import com.google.caja.lang.css.CssSchema;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Parser;
import com.google.caja.plugin.stages.CheckForErrorsStage;
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
import com.google.caja.reporting.BuildInfo;
import com.google.caja.util.Criterion;
import com.google.caja.util.Pipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * Takes arbitrary html and js as a String, compiles and rewrites it, and
 * ends up with Strings holding rewritten js, rewritten css, and error messages.
 *
 * @author rdub@google.com (Ryan Williams)
 */
public class HtmlPluginCompiler {
  /**
   * A configurable pipeline that performs the compilation of HTML, CSS, and JS
   * to CSS and JS.
   */
  private Pipeline<Jobs> compilationPipeline;
  private Jobs jobs;
  private CssSchema cssSchema;

  public HtmlPluginCompiler(MessageQueue mq, PluginMeta meta) {
    BuildInfo.getInstance().addBuildInfo(mq);
    MessageContext mc = new MessageContext();
    mc.inputSources = new ArrayList<InputSource>();
    jobs = new Jobs(mc, mq, meta);
    cssSchema = CssSchema.getDefaultCss21Schema(mq);
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

  public void addInput(AncestorChain<?> input) {
    jobs.getJobs().add(new Job(input));
    jobs.getMessageContext().inputSources.add(
        input.node.getFilePosition().source());
  }

  protected void setupCompilationPipeline() {
    compilationPipeline = new Pipeline<Jobs>() {
      @Override
      protected boolean applyStage(
          Pipeline.Stage<? super Jobs> stage, Jobs jobs) {
        jobs.getMessageQueue().addMessage(
            MessageType.CHECKPOINT,
            MessagePart.Factory.valueOf(stage.getClass().getSimpleName()),
            MessagePart.Factory.valueOf(System.nanoTime() / 1e9));
        return super.applyStage(stage, jobs);
      }
    };

    List<Pipeline.Stage<Jobs>> stages = compilationPipeline.getStages();
    stages.add(new RewriteHtmlStage());
    stages.add(new ValidateHtmlStage());
    stages.add(new CompileHtmlStage(cssSchema));
    stages.add(new ValidateCssStage(cssSchema));
    stages.add(new ConsolidateCodeStage());
    stages.add(new ValidateJavascriptStage());
    stages.add(new ConsolidateCssStage());
    stages.add(new CheckForErrorsStage());
  }

  public Pipeline<Jobs> getCompilationPipeline() {
    if (compilationPipeline == null) { setupCompilationPipeline(); }
    return compilationPipeline;
  }

  /**
   * the list of parse trees that comprise the plugin after run has been called.
   * Valid after run has been called.
   */
  public List<? extends ParseTreeNode> getOutputs() {
    List<ParseTreeNode> outputs = new ArrayList<ParseTreeNode>();
    outputs.add(getJavascript());
    outputs.add(getCss());
    return outputs;
  }

  /**
   * If the javascript has been compiled and consolidated, return the resulting
   * true, otherwise return null.
   */
  public Block getJavascript() {
    Job soleJsJob = getConsolidatedOutput(
        new Criterion<Job>() {
          public boolean accept(Job job) {
            return job.getType() == Job.JobType.JAVASCRIPT
                && job.getRoot().node instanceof Block;
          }
        });

    if (soleJsJob == null) { return null; }

    Block jsTree = (Block) soleJsJob.getRoot().node;
    if (!jsTree.getAttributes().is(SyntheticNodes.SYNTHETIC)) {
      // Not yet rewritten.
      return null;
    }
    return jsTree;
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
   * run the compiler on all parse trees added via {@link #addInput}.
   * The output parse trees are available via {@link #getOutputs()}.
   * @return true on success, false on failure.
   */
  public boolean run() {
    return getCompilationPipeline().apply(jobs);
  }

  public static Block parseJs(
      InputSource is, CharProducer cp, MessageQueue localMessageQueue)
      throws ParseException {
    JsLexer lexer = new JsLexer(cp);
    JsTokenQueue tq = new JsTokenQueue(lexer, is);
    Parser p = new Parser(tq, localMessageQueue);
    Block body = p.parse();
    tq.expectEmpty();
    return body;
  }

  public static CssTree.StyleSheet parseCss(InputSource is, CharProducer cp)
      throws ParseException {
    CssLexer lexer = new CssLexer(cp);
    CssTree.StyleSheet input;
    TokenQueue<CssTokenType> tq = new TokenQueue<CssTokenType>(
        lexer, is, new Criterion<Token<CssTokenType>>() {
          public boolean accept(Token<CssTokenType> tok) {
            return tok.type != CssTokenType.COMMENT
                && tok.type != CssTokenType.SPACE;
          }
        });
    if (tq.isEmpty()) { return null; }

    CssParser p = new CssParser(tq);
    input = p.parseStyleSheet();
    tq.expectEmpty();
    return input;
  }
}
