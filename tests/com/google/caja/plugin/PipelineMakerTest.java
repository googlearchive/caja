// Copyright (C) 2010 Google Inc.
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
import com.google.caja.parser.quasiliteral.ModuleManager;
import com.google.caja.plugin.stages.StubJobCache;
import com.google.caja.reporting.TestBuildInfo;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Lists;
import com.google.caja.util.MoreAsserts;
import com.google.caja.util.Pipeline;

import java.util.Arrays;
import java.util.List;

public class PipelineMakerTest extends CajaTestCase {
  public final void testDefaultPipeline() throws Exception {
    assertPipeline(
        PipelineMaker.DEFAULT_PRECONDS,
        PipelineMaker.DEFAULT_GOALS,
        "LegacyNamespaceFixupStage",
        "ResolveUriStage",
        "RewriteHtmlStage",
        "InlineCssImportsStage",
        "SanitizeHtmlStage",
        "ValidateCssStage",
        "RewriteCssStage",
        "HtmlToBundleStage",
        "PipelineFetchStage",
        "OptimizeJavascriptStage",
        "ValidateJavascriptStage",
        "PipelineStoreStage",
        "ConsolidateCodeStage",
        "CheckForErrorsStage");
  }

  public final void testJsOnlyOutput() throws Exception {
    assertPipeline(
        PipelineMaker.DEFAULT_PRECONDS,
        PipelineMaker.DEFAULT_GOALS.without(PipelineMaker.HTML_SAFE_STATIC),
        "LegacyNamespaceFixupStage",
        "ResolveUriStage",
        "RewriteHtmlStage",
        "InlineCssImportsStage",
        "SanitizeHtmlStage",
        "ValidateCssStage",
        "RewriteCssStage",
        "HtmlToJsStage",
        "PipelineFetchStage",
        "OptimizeJavascriptStage",
        "ValidateJavascriptStage",
        "PipelineStoreStage",
        "ConsolidateCodeStage",
        "CheckForErrorsStage");
  }

  public final void testDebuggingModePipeline() throws Exception {
    assertPipeline(
        PipelineMaker.DEFAULT_PRECONDS,
        PipelineMaker.DEFAULT_GOALS
            .without(PipelineMaker.ONE_CAJOLED_MODULE)
            .with(PipelineMaker.ONE_CAJOLED_MODULE_DEBUG),
        "LegacyNamespaceFixupStage",
        "ResolveUriStage",
        "RewriteHtmlStage",
        "InlineCssImportsStage",
        "SanitizeHtmlStage",
        "ValidateCssStage",
        "RewriteCssStage",
        "HtmlToBundleStage",
        "PipelineFetchStage",
        "OptimizeJavascriptStage",
        "ValidateJavascriptStage",
        "PipelineStoreStage",
        "ConsolidateCodeStage",
        "InferFilePositionsStage",  // extra
        "DebuggingSymbolsStage",  // extra
        "CheckForErrorsStage");
  }

  public final void testHtmlAlreadyNamespaced() throws Exception {
    assertPipeline(
        PipelineMaker.HTML_XMLNS.with(PipelineMaker.JS).with(PipelineMaker.CSS),
        PipelineMaker.DEFAULT_GOALS,
        "ResolveUriStage",
        "RewriteHtmlStage",
        "InlineCssImportsStage",
        "SanitizeHtmlStage",
        "ValidateCssStage",
        "RewriteCssStage",
        "HtmlToBundleStage",
        "PipelineFetchStage",
        "OptimizeJavascriptStage",
        "ValidateJavascriptStage",
        "PipelineStoreStage",
        "ConsolidateCodeStage",
        "CheckForErrorsStage");
  }

  public final void testJsOnly() throws Exception {
    assertPipeline(
        PipelineMaker.JS,
        PipelineMaker.ONE_CAJOLED_MODULE.with(PipelineMaker.SANITY_CHECK),
        "PipelineFetchStage",
        "OptimizeJavascriptStage",
        "ValidateJavascriptStage",
        "PipelineStoreStage",  // Nothing fetches, so a noop.
        "ConsolidateCodeStage",
        "CheckForErrorsStage");
  }

  public final void testFailEarly() {
    try {
      PipelineMaker.planState("bogus");
    } catch (IllegalArgumentException ex) {
      assertEquals("bogus", ex.getMessage());
      return;
    }
    fail("completed");
  }

  public final void testNoPath() {
    PipelineMaker pm = new PipelineMaker(
        CssSchema.getDefaultCss21Schema(mq), HtmlSchema.getDefault(mq),
        new ModuleManager(
            new PluginMeta(), TestBuildInfo.getInstance(),
            UriFetcher.NULL_NETWORK, false, mq),
        new StubJobCache(), PipelineMaker.HTML, PipelineMaker.CSS);
    long t0 = System.nanoTime();
    try {
      pm.populate(Lists.<Pipeline.Stage<Jobs>>newArrayList());
    } catch (Planner.UnsatisfiableGoalException ex) {
      long t1 = System.nanoTime();
      System.err.println("Took " + ((t1 - t0) / 1e9) + "s");
      return;
    }
    fail("completed");
  }

  private void assertPipeline(
      Planner.PlanState preconds, Planner.PlanState goals, String... expected)
      throws Exception {

    List<Pipeline.Stage<Jobs>> stages = Lists.newArrayList();
    PipelineMaker pm = new PipelineMaker(
        CssSchema.getDefaultCss21Schema(mq), HtmlSchema.getDefault(mq),
        new ModuleManager(
            new PluginMeta(), TestBuildInfo.getInstance(),
            UriFetcher.NULL_NETWORK, false, mq),
        new StubJobCache(), preconds, goals);
    {
      long t0 = System.nanoTime();
      pm.populate(stages);
      long t1 = System.nanoTime();
      System.err.println("Took " + (t1 - t0) / 1e9 + "s");
    }

    List<String> classes = Lists.newArrayList();
    for (Pipeline.Stage<Jobs> stage : stages) {
      classes.add(stage.getClass().getSimpleName());
    }

    MoreAsserts.assertListsEqual(Arrays.asList(expected), classes);
  }
}
