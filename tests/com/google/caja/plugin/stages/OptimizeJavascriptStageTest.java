// Copyright (C) 2008 Google Inc.
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

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.ValueProperty;
import com.google.caja.parser.quasiliteral.ModuleManager;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.JobEnvelope;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.reporting.TestBuildInfo;
import com.google.caja.util.ContentType;
import com.google.caja.util.Join;

public class OptimizeJavascriptStageTest extends PipelineStageTestCase {
  public final void testEmptyInput() throws Exception {
    assertPipeline(
        job(";", ContentType.JS),
        job(ContentType.JS,
            "{",
            "  var dis___ = IMPORTS___;",
            "  var moduleResult___;",
            "  moduleResult___ = ___.NO_RESULT;",
            "  return moduleResult___;",
            "}"));
  }

/*
  public final void testLoopCounter() throws Exception {
    assertPipeline(
        job(ContentType.JS,
            "(function (f, arr) {",
            "  for (var i = 0; i < arr.length; ++i) {",
            "    f(arr[i]);",
            "  }",
            "})(f, [1, 2, 3])"),
        job(ContentType.JS,
            normJs(
                "{",
                "  var dis___ = IMPORTS___;",
                "  var moduleResult___;",
                "  moduleResult___ = ___.NO_RESULT;",
                "  moduleResult___ = ___.f(function (f, arr) {",
                "      var i;",
                "      for (i = 0; i < arr.length; ++i) {",
                "        f.i___(arr[ +i ]);",
                "      }",
                "    }).i___(" + outer("f") + ", [ 1, 2, 3 ]);",
                "  return moduleResult___;",
                "}")));
  }

  public final void testIndexOfUnknownProvenance() throws Exception {
    assertPipeline(
        job(ContentType.JS,
            "(function (i) {",
            "  for (; i < a.length; ++i) {",
            "    f(a[i]);",
            "  }",
            "})(x)"),
        job(ContentType.JS,
            normJs(
                "{",
                "  var dis___ = IMPORTS___;",
                "  var moduleResult___;",
                "  moduleResult___ = ___.NO_RESULT;",
                "  moduleResult___ = ___.f(function (i) {",
                "      for (; i < " + outer("a") + ".length; ++i) {",
                "        " + outer("f") + ".i___(" + outer("a") + ".v___(i));",
                "      }",
                "    }).i___(" + outer("x") + ");",
                "  return moduleResult___;",
                "}")));
  }

*/

  @Override
  protected boolean runPipeline(Jobs jobs) {
    getMeta().setEnableES53(true);
    return new OptimizeJavascriptStage().apply(jobs)
        && new ValidateJavascriptStage(new ModuleManager(
                new PluginMeta(), TestBuildInfo.getInstance(),
                UriFetcher.NULL_NETWORK, false, mq))
            .apply(jobs)
        && discardBoilerPlate(jobs);
  }

  private boolean discardBoilerPlate(Jobs jobs) {
    for (JobEnvelope env : jobs.getJobsByType(ContentType.JS)) {
      Job job = env.job;
      if (job.getRoot() instanceof CajoledModule) {
        jobs.getJobs().remove(env);
        ObjectConstructor cs = ((CajoledModule) job.getRoot()).getModuleBody();
        FunctionConstructor instantiate = (FunctionConstructor)
            ((ValueProperty) cs.propertyWithName("instantiate")).getValueExpr();
        jobs.getJobs().add(
            JobEnvelope.of(Job.jsJob(instantiate.getBody(), null)));
      }
    }
    return true;
  }

  private static final String outer(String id) {
    return "(IMPORTS___." + id + "_v___? IMPORTS___." + id
        + ": ___.ri(IMPORTS___, '" + id + "'))";
  }

  private String[] normJs(String... lines) throws ParseException {
    return renderProgram(js(fromString(Join.join("\n", lines)))).split("\n");
  }
}
