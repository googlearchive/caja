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
import com.google.caja.reporting.TestBuildInfo;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Lists;
import com.google.caja.util.MoreAsserts;
import com.google.caja.util.Pipeline;

import java.util.Arrays;
import java.util.List;

public class PipelineMakerTest extends CajaTestCase {
  public final void testDefaultPipeline() throws Exception {
    PluginMeta meta = new PluginMeta();
    assertPipeline(
        PipelineMaker.defaultPreconds(),
        PipelineMaker.defaultGoals(meta),
        "LegacyNamespaceFixupStage",
        "ResolveUriStage",
        "RewriteHtmlStage",
        "InlineCssImportsStage",
        "SanitizeHtmlStage",
        "ValidateCssStage",
        "RewriteCssStage",
        "CompileHtmlStage",
        "ConsolidateCodeStage",
        "ValidateJavascriptStage",
        "CheckForErrorsStage");
  }

  public final void testDebuggingModePipeline() throws Exception {
    PluginMeta meta = new PluginMeta();
    meta.setDebugMode(true);
    assertPipeline(
        PipelineMaker.defaultPreconds(),
        PipelineMaker.defaultGoals(meta),
        "LegacyNamespaceFixupStage",
        "ResolveUriStage",
        "RewriteHtmlStage",
        "InlineCssImportsStage",
        "SanitizeHtmlStage",
        "ValidateCssStage",
        "RewriteCssStage",
        "CompileHtmlStage",
        "ConsolidateCodeStage",
        "ValidateJavascriptStage",
        "InferFilePositionsStage",  // extra
        "DebuggingSymbolsStage",  // extra
        "CheckForErrorsStage");
  }

  public final void testHtmlAlreadyNamespaced() throws Exception {
    PluginMeta meta = new PluginMeta();
    assertPipeline(
        PipelineMaker.planState("html+xmlns", "js", "css"),
        PipelineMaker.defaultGoals(meta),
        "ResolveUriStage",
        "RewriteHtmlStage",
        "InlineCssImportsStage",
        "SanitizeHtmlStage",
        "ValidateCssStage",
        "RewriteCssStage",
        "CompileHtmlStage",
        "ConsolidateCodeStage",
        "ValidateJavascriptStage",
        "CheckForErrorsStage");
  }

  public final void testJsOnly() throws Exception {
    assertPipeline(
        PipelineMaker.planState("js"),
        PipelineMaker.planState("cajoled_module", "sanity_check"),
        "ConsolidateCodeStage",
        "ValidateJavascriptStage",
        "CheckForErrorsStage");
  }

  public final void testFailEarly() throws Exception {
    try {
      PipelineMaker.planState("bogus");
    } catch (IllegalArgumentException ex) {
      assertEquals("bogus", ex.getMessage());
      return;
    }
    fail("completed");
  }

  public final void testNoPath() throws Exception {
    PipelineMaker pm = new PipelineMaker(
        new TestBuildInfo(), CssSchema.getDefaultCss21Schema(mq),
        HtmlSchema.getDefault(mq), PipelineMaker.planState("html"),
        PipelineMaker.planState("css"));
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
        new TestBuildInfo(), CssSchema.getDefaultCss21Schema(mq),
        HtmlSchema.getDefault(mq), preconds, goals);
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
