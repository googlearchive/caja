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
import com.google.caja.plugin.stages.CheckForErrorsStage;
import com.google.caja.plugin.stages.CompileHtmlStage;
import com.google.caja.plugin.stages.ConsolidateCodeStage;
import com.google.caja.plugin.stages.DebuggingSymbolsStage;
import com.google.caja.plugin.stages.InferFilePositionsStage;
import com.google.caja.plugin.stages.InlineCssImportsStage;
import com.google.caja.plugin.stages.LegacyNamespaceFixupStage;
import com.google.caja.plugin.stages.OpenTemplateStage;
import com.google.caja.plugin.stages.ResolveUriStage;
import com.google.caja.plugin.stages.RewriteCssStage;
import com.google.caja.plugin.stages.RewriteHtmlStage;
import com.google.caja.plugin.stages.SanitizeHtmlStage;
import com.google.caja.plugin.stages.ValidateCssStage;
import com.google.caja.plugin.stages.ValidateJavascriptStage;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.util.ContentType;
import com.google.caja.util.Pipeline;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A quick and dirty planner that builds a pipeline based on flags from the
 * command line or from a web service.
 *
 * <h2>Inputs</h2>
 * If you are unsure, just use {@link PipelineMaker#defaultPreconds}.
 * <table>
 * <tr><td>css<td>Include if your input can contain CSS</tr>
 * <tr><td>css+inlined</td>
 *    <td>Include instead of CSS if your input contains CSS
 *    without any {@code @imports}</tr>
 * <tr><td>html<td>Include if your input can contain HTML</tr>
 * <tr><td>html+xmlns<td>Include instead of 'html" if your input only contain
 *   namespace aware HTML or XML.</tr>
 * <tr><td>js<td>Include if your input can contain JS</tr>
 * <tr><td>option+inline_opentemplate<td>Include if you want calls to
 *   {@code open(Template(...))} desugared in your code.</tr>
 * </table>
 *
 * <h2>Goals</h2>
 * <table>
 * <tr><td>cajoled_module<td>Specifies that JS should be cajoled.</tr>
 * <tr><td>cajoled_module+debug_symbols<td>Instead, to cajole with debug
 *    symbols.</tr>
 * <tr><td>sanity_check<td>Always include this unless you are sure of what
 *    you are doing.  Causes the pipeline to report failure at the end on
 *    {@link MessageLevel#ERROR errors} instead of just fatal errors.</tr>
 * <tr>
 *
 * @author mikesamuel@gmail.com
 */
final class PipelineMaker {
  private final PlanInputs in;
  private final Planner.PlanState inputs;
  private final Planner.PlanState goals;

  private static final Planner PLANNER = new Planner();

  PipelineMaker(
      BuildInfo buildInfo, CssSchema cssSchema, HtmlSchema htmlSchema,
      Planner.PlanState inputs, Planner.PlanState goals) {
    this.in = new PlanInputs(cssSchema, htmlSchema, buildInfo);
    this.inputs = inputs;
    this.goals = goals;
  }

  static boolean initializedIdents;
  /**
   * Creates a plan state from a set of '+' separated identifiers.
   * See the class comments for descriptions of useful identifiers.
   * @see #defaultGoals(PluginMeta)
   * @see #defaultPreconds()
   */
  static Planner.PlanState planState(String... products) {
    if (!initializedIdents) {
      // Instantiate the tool set, so that all the useful property names have
      // been seen by PLANNER.
      makeTools(Planner.EMPTY);
      initializedIdents = true;
    }
    return PLANNER.planState(false, products);
  }

  /** The default preconditions for a {@code PluginCompiler} pipeline. */
  static final Planner.PlanState defaultPreconds() {
    return planState("css", "html", "js");
  }

  /** The default goals of a {@code PluginCompiler} pipeline. */
  static final Planner.PlanState defaultGoals(PluginMeta meta) {
    return planState(
        meta.isDebugMode() ? "cajoled_module+debug_symbols" : "cajoled_module",
        meta.isOnlyJsEmitted() ? null : "html+safe+static",
        "sanity_check");
  }

  private static final Map<String, List<Tool>> PLAN_CACHE
      = Collections.synchronizedMap(new LinkedHashMap<String, List<Tool>>() {
    @Override
    public boolean removeEldestEntry(Map.Entry<String, List<Tool>> e) {
      return this.size() > 32;
    }
  });

  /**
   * Appends pipeline stages to the argument.
   *
   * @throws UnsatisfiableGoalException iff there is no path from the
   *   preconditions to the goal using the pipeline stages declared.
   */
  void populate(List<Pipeline.Stage<Jobs>> compilationPipeline)
      throws Planner.UnsatisfiableGoalException {
    String cacheKey = Arrays.toString(goals.properties) + "/"
        + Arrays.toString(inputs.properties);
    List<Tool> plan = PLAN_CACHE.get(cacheKey);
    if (plan == null) {
      PLAN_CACHE.put(
          cacheKey, plan = PLANNER.plan(makeTools(goals), inputs, goals));
    }
    for (Tool tool : plan) { tool.operate(in, compilationPipeline); }
  }

  private static List<Tool> makeTools(Planner.PlanState goals) {
    return Arrays.asList(
        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new LegacyNamespaceFixupStage());
          }
        }.given("html").produces("html+xmlns"),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new ResolveUriStage(in.htmlSchema));
          }
        }.given("html+xmlns").produces("html+absuri+xmlns"),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new RewriteHtmlStage(in.htmlSchema));
          }
        }.given("html+absuri+xmlns").produces("js", "css", "html+static"),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new InlineCssImportsStage());
          }
        }.given("css").produces("css+inlined"),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new SanitizeHtmlStage(in.htmlSchema));
          }
        }.given("html+static").produces("html+stripped+static"),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new ValidateCssStage(in.cssSchema, in.htmlSchema));
            out.add(new RewriteCssStage());
          }
        }.given("css+inlined").produces("css+namespaced"),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new CompileHtmlStage(
                in.cssSchema, in.htmlSchema, ContentType.HTML));
          }
        }.given("html+stripped+static", "css+namespaced")
         .produces("js+safe", "html+safe+static"),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new CompileHtmlStage(
                in.cssSchema, in.htmlSchema, ContentType.JS));
          }
        }.given("html+stripped+static", "css+namespaced").produces("js"),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new OpenTemplateStage());
          }
        }.given("js", "option+inline_opentemplate").produces("js"),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new ConsolidateCodeStage());
          }
        }.given("js").produces("uncajoled_module"),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new ValidateJavascriptStage(in.buildInfo));
          }
        }.given("uncajoled_module").produces("cajoled_module"),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new InferFilePositionsStage());
            out.add(new DebuggingSymbolsStage());
          }
        }.given("cajoled_module")
         .produces("cajoled_module+debug_symbols"),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new CheckForErrorsStage());
          }
        }.given(goals).exceptNotGiven("sanity_check")
         .produces(goals).produces("sanity_check")
    );
  }

  private interface StageMaker {
    public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out);
  }

  private static abstract class Tool extends Planner.Tool
      implements StageMaker {
    Tool given(String... preconds) {
      return given(PLANNER.planState(true, preconds));
    }

    @Override
    Tool given(Planner.PlanState preconds) {
      return (Tool) super.given(preconds);
    }

    Tool produces(String... postconds) {
      return produces(PLANNER.planState(true, postconds));
    }

    @Override
    Tool produces(Planner.PlanState postconds) {
      return (Tool) super.produces(postconds);
    }

    Tool exceptNotGiven(String... exceptions) {
      exceptNotGiven(PLANNER.planState(true, exceptions));
      return this;
    }
  }

  private static class PlanInputs {
    final CssSchema cssSchema;
    final HtmlSchema htmlSchema;
    final BuildInfo buildInfo;

    PlanInputs(CssSchema cssSchema, HtmlSchema htmlSchema, BuildInfo buildInfo) {
      this.cssSchema = cssSchema;
      this.htmlSchema = htmlSchema;
      this.buildInfo = buildInfo;
    }
  }
}
