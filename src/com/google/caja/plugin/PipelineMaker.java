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
import com.google.caja.plugin.stages.CheckForErrorsStage;
import com.google.caja.plugin.stages.ConsolidateCodeStage;
import com.google.caja.plugin.stages.DebuggingSymbolsStage;
import com.google.caja.plugin.stages.HtmlToBundleStage;
import com.google.caja.plugin.stages.HtmlToJsStage;
import com.google.caja.plugin.stages.InferFilePositionsStage;
import com.google.caja.plugin.stages.InlineCssImportsStage;
import com.google.caja.plugin.stages.JobCache;
import com.google.caja.plugin.stages.LegacyNamespaceFixupStage;
import com.google.caja.plugin.stages.OpenTemplateStage;
import com.google.caja.plugin.stages.OptimizeJavascriptStage;
import com.google.caja.plugin.stages.PipelineFetchStage;
import com.google.caja.plugin.stages.PipelineStoreStage;
import com.google.caja.plugin.stages.ResolveUriStage;
import com.google.caja.plugin.stages.RewriteCssStage;
import com.google.caja.plugin.stages.RewriteHtmlStage;
import com.google.caja.plugin.stages.SanitizeHtmlStage;
import com.google.caja.plugin.stages.ValidateCssStage;
import com.google.caja.plugin.stages.ValidateJavascriptStage;
import com.google.caja.util.Join;
import com.google.caja.util.Lists;
import com.google.caja.util.Pair;
import com.google.caja.util.Pipeline;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A quick and dirty planner that builds a pipeline based on flags from the
 * command line or from a web service.
 * <p>
 * See {@link PipelineMaker#getGoalDocumentation()} and
 * {@link PipelineMaker#getPreconditionDocumentation()} for details on the
 * meanings of flags.
 *
 * @author mikesamuel@gmail.com
 */
public final class PipelineMaker {
  private final PlanInputs in;
  private final Planner.PlanState inputs;
  private final Planner.PlanState goals;

  private static final Planner PLANNER = new Planner();

  PipelineMaker(
      CssSchema cssSchema, HtmlSchema htmlSchema, ModuleManager mgr,
      JobCache cache, Planner.PlanState inputs, Planner.PlanState goals) {
    this.in = new PlanInputs(cssSchema, htmlSchema, mgr, cache);
    this.inputs = inputs;
    this.goals = goals;
  }

  /**
   * Creates a plan state from a set of '+' separated identifiers.
   * See the class comments for descriptions of useful identifiers.
   * @see #DEFAULT_GOALS
   * @see #DEFAULT_PRECONDS
   */
  public static Planner.PlanState planState(String... products) {
    return PLANNER.planState(false, products);
  }

  private static final Map<String, List<Tool>> PLAN_CACHE
      = Collections.synchronizedMap(new LinkedHashMap<String, List<Tool>>() {
        private static final long serialVersionUID = 8484573795809352579L;

        @Override
        public boolean removeEldestEntry(Map.Entry<String, List<Tool>> e) {
          return this.size() > 32;
        }
      });

  /**
   * Appends pipeline stages to the argument.
   *
   * @throws Planner.UnsatisfiableGoalException iff there is no path from the
   *   preconditions to the goal using the pipeline stages declared.
   */
  void populate(List<Pipeline.Stage<Jobs>> compilationPipeline)
      throws Planner.UnsatisfiableGoalException {
    String cacheKey = Arrays.toString(goals.properties) + "/"
        + Arrays.toString(inputs.properties);
    List<Tool> plan = PLAN_CACHE.get(cacheKey);
    if (plan == null) {
      List<Tool> tools = makeTools(goals);
      PLAN_CACHE.put(cacheKey, plan = PLANNER.plan(tools, inputs, goals));
    }
    for (Tool tool : plan) { tool.operate(in, compilationPipeline); }
  }

  public static List<Pair<String, String>> getPreconditionDocumentation() {
    return Collections.unmodifiableList(PRECOND_DOCS);
  }

  public static List<Pair<String, String>> getGoalDocumentation() {
    return Collections.unmodifiableList(GOAL_DOCS);
  }

  private static List<Pair<String, String>> PRECOND_DOCS = Lists.newArrayList();
  private static List<Pair<String, String>> GOAL_DOCS = Lists.newArrayList();

  private static Planner.PlanState makePrecond(String props, String... docs) {
    Planner.PlanState ps = PLANNER.planState(true, props);
    PRECOND_DOCS.add(Pair.pair(ps.toString(), Join.join("", docs)));
    return ps;
  }

  private static Planner.PlanState makeInner(String props) {
    return PLANNER.planState(true, props);
  }

  private static Planner.PlanState makeGoal(String props, String... docs) {
    Planner.PlanState ps = PLANNER.planState(true, props);
    GOAL_DOCS.add(Pair.pair(ps.toString(), Join.join("", docs)));
    return ps;
  }

  public static final Planner.PlanState CSS = makePrecond(
      "css", "when CSS can appear on the input.");
  public static final Planner.PlanState JS = makePrecond(
      "js", "when JavaScript can appear on the input.");
  public static final Planner.PlanState HTML = makePrecond(
      "html", "when HTML can appear on the input.");
  public static final Planner.PlanState HTML_XMLNS = makePrecond(
      "html+xmlns", "instead of html if no un-namespaced DOMs on the input.");
  private static final Planner.PlanState HTML_ABSURI_XMLNS = makeInner(
      "html+absuri+xmlns");
  private static final Planner.PlanState HTML_STATIC = makeInner(
      "html+static");
  private static final Planner.PlanState HTML_STATIC_STRIPPED = makeInner(
      "html+static+stripped");
  private static final Planner.PlanState CSS_NAMESPACED = makeInner(
      "css+namespaced");
  public static final Planner.PlanState CSS_INLINED = makePrecond(
      "css+inlined", "instead of css if no @import statements in the inputs.");
  public static final Planner.PlanState OPT_OPENTEMPLATE = makePrecond(
      "opt+opentemplate", "to desugar open(Template(...)) calls.");
  public static final Planner.PlanState HTML_SAFE_STATIC = makeGoal(
      "html+safe+static",
      "to output HTML.  Not exlusive with cajoled_module.");
  public static final Planner.PlanState CAJOLED_MODULES = makeInner(
      "cajoled_module");
  public static final Planner.PlanState ONE_CAJOLED_MODULE = makeGoal(
      "cajoled_module+one", "to output a bundle of JS.");
  public static final Planner.PlanState ONE_CAJOLED_MODULE_DEBUG = makeGoal(
      "cajoled_module+one+debug",
      "instead of cajoled_module if you want debug symbols.");
  public static final Planner.PlanState SANITY_CHECK = makeGoal(
      "sanity_check", "reports errors due to ERRORs, not just FATAL_ERRORS.");

  /** The default preconditions for a {@code PluginCompiler} pipeline. */
  public static final Planner.PlanState DEFAULT_PRECONDS = CSS
      .with(HTML).with(JS);
  /** The default goals of a {@code PluginCompiler} pipeline. */
  public static final Planner.PlanState DEFAULT_GOALS = ONE_CAJOLED_MODULE
      .with(HTML_SAFE_STATIC).with(SANITY_CHECK);

  private static List<Tool> makeTools(Planner.PlanState goals) {
    return Arrays.asList(
        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new LegacyNamespaceFixupStage());
          }
        }.given(HTML).produces(HTML_XMLNS),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new ResolveUriStage(in.htmlSchema));
          }
        }.given(HTML_XMLNS).produces(HTML_ABSURI_XMLNS),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new RewriteHtmlStage(in.htmlSchema, in.cache));
          }
        }.given(HTML_ABSURI_XMLNS)
         .produces(CSS).produces(JS).produces(HTML_STATIC),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new InlineCssImportsStage());
          }
        }.given(CSS).produces(CSS_INLINED),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new SanitizeHtmlStage(in.htmlSchema));
          }
        }.given(HTML_STATIC).produces(HTML_STATIC_STRIPPED),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new ValidateCssStage(in.cssSchema, in.htmlSchema));
            out.add(new RewriteCssStage());
          }
        }.given(CSS_INLINED).produces(CSS_NAMESPACED),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new HtmlToBundleStage(in.cssSchema, in.htmlSchema));
          }
        }.given(HTML_STATIC_STRIPPED).given(CSS_NAMESPACED)
         .produces(JS).produces(HTML_SAFE_STATIC),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new HtmlToJsStage(in.cssSchema, in.htmlSchema));
          }
        }.given(HTML_STATIC_STRIPPED).given(CSS_NAMESPACED).produces(JS),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new OpenTemplateStage());
          }
        }.given(JS).given(OPT_OPENTEMPLATE).produces(JS),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new PipelineFetchStage(in.cache));
            out.add(new OptimizeJavascriptStage());
            out.add(new ValidateJavascriptStage(in.moduleManager));
          }
        }.given(JS).produces(CAJOLED_MODULES),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new PipelineStoreStage(in.cache));
            out.add(new ConsolidateCodeStage(in.moduleManager));
          }
        }.given(CAJOLED_MODULES).produces(ONE_CAJOLED_MODULE),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new InferFilePositionsStage());
            out.add(new DebuggingSymbolsStage());
          }
        }.given(ONE_CAJOLED_MODULE).produces(ONE_CAJOLED_MODULE_DEBUG),

        new Tool() {
          public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out) {
            out.add(new CheckForErrorsStage());
          }
        }.given(goals).exceptNotGiven(SANITY_CHECK)
         .produces(goals).produces(SANITY_CHECK)
    );
  }

  private interface StageMaker {
    public void operate(PlanInputs in, List<Pipeline.Stage<Jobs>> out);
  }

  private static abstract class Tool extends Planner.Tool
      implements StageMaker {
    @Override
    Tool given(Planner.PlanState preconds) {
      return (Tool) super.given(preconds);
    }

    @Override
    Tool produces(Planner.PlanState postconds) {
      return (Tool) super.produces(postconds);
    }

    @Override
    Tool exceptNotGiven(Planner.PlanState exceptions) {
      return (Tool) super.exceptNotGiven(exceptions);
    }
  }

  // Visible for testing
  PlanInputs getPlanInputs() { return in; }

  // Visible for testing
  static final class PlanInputs {
    final CssSchema cssSchema;
    final HtmlSchema htmlSchema;
    final ModuleManager moduleManager;
    final JobCache cache;

    PlanInputs(
        CssSchema cssSchema, HtmlSchema htmlSchema, ModuleManager mgr,
        JobCache cache) {
      this.cssSchema = cssSchema;
      this.htmlSchema = htmlSchema;
      this.moduleManager = mgr;
      this.cache = cache;
    }
  }
}
