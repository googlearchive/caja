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

package com.google.caja.demos.benchmarks;

import com.google.caja.parser.AncestorChain;
import com.google.caja.plugin.PluginCompiler;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.RhinoTestBed;

/**
 * Unit test which executes the V8 benchmark and collates the result for rendering with varz
 */
public class BenchmarkRunner extends CajaTestCase {
  public void testRichards() throws Exception { runBenchmark("richards.js"); }
  public void testDeltaBlue() throws Exception { runBenchmark("deltablue.js"); }
  public void testCrypto() throws Exception { runBenchmark("crypto.js"); }
  public void testRayTrace() throws Exception { runBenchmark("raytrace.js"); }
  public void testEarleyBoyer() throws Exception {
    runBenchmark("earley-boyer.js");
  }

  /**
   * Runs the given benchmark
   * Accumulates the result and formats it for consumption by varz
   * Format:
   * VarZ:benchmark.<benchmark name>.<speed|size>.<language>.<debug?>.<engine>.<primed?>
   */
  private void runBenchmark(String filename) throws Exception {
    double scoreUncajoled = runUncajoled(filename);
    double scoreCajoled = runCajoled(filename);
    System.out.println(
        "VarZ:benchmark." + getName() + ".speed.uncajoled.nodebug.rhino.cold=" + scoreUncajoled);
    System.out.println(
        "VarZ:benchmark." + getName() + ".speed.valija.nodebug.rhino.cold=" + scoreCajoled);
    System.out.println(
        "VarZ:benchmark." + getName() + ".speeddiff.valija.nodebug.rhino.cold="
        + (scoreCajoled / scoreUncajoled));
  }

  // Like run.js but outputs the result differently.
  // Cannot use ___.getNewModuleHandler.getLastValue() to get the result since
  // there is no ModuleEnvelope until We fix the compilation unit problem.
  // Instead, we attach the result to an outer object called benchmark.
  private static final String RUN_SCRIPT = (
      ""
      + "BenchmarkSuite.RunSuites({\n"
      + "      NotifyResult: function (n, r) {\n"
      + "        benchmark.name = n;\n"
      + "        benchmark.result = r;\n"
      + "      },\n"
      + "      NotifyScore: function (s) { benchmark.score = s; }\n"
      + "    });"
      );

  private double runUncajoled(String filename) throws Exception {
    Number score = (Number) RhinoTestBed.runJs(
        new RhinoTestBed.Input("var benchmark = {};", "setup"),
        new RhinoTestBed.Input(getClass(), "base.js"),
        new RhinoTestBed.Input(getClass(), filename),
        new RhinoTestBed.Input(RUN_SCRIPT, getName()),
        new RhinoTestBed.Input("benchmark.score", "score"));
    return score.doubleValue();
  }

  private double runCajoled(String filename) throws Exception {
    PluginMeta meta = new PluginMeta();
    meta.setValijaMode(true);
    PluginCompiler pc = new PluginCompiler(meta, mq);
    pc.addInput(AncestorChain.instance(js(fromResource("base.js"))));
    pc.addInput(AncestorChain.instance(js(fromResource(filename))));
    pc.addInput(AncestorChain.instance(js(fromString(RUN_SCRIPT))));
    assertTrue(pc.run());
    String cajoledJs = render(pc.getJavascript());
    Number score = (Number) RhinoTestBed.runJs(
        new RhinoTestBed.Input(getClass(), "../../cajita.js"),
        new RhinoTestBed.Input(
            ""
            + "var testImports = ___.copy(___.sharedImports);\n"
            + "testImports.loader = ___.freeze({\n"
            + "        provide: ___.frozenFunc(\n"
            + "            function(v){ valijaMaker = v; })\n"
            + "    });\n"
            + "testImports.outers = ___.copy(___.sharedImports);\n"
            + "___.getNewModuleHandler().setImports(testImports);",
            getName() + "valija-setup"),
        new RhinoTestBed.Input(getClass(), "../../plugin/valija.co.js"),
        new RhinoTestBed.Input(
            // Set up the imports environment.
            ""
            + "testImports = ___.copy(___.sharedImports);\n"
            + "testImports.benchmark = {};\n"
            + "testImports.$v = valijaMaker.CALL___(testImports);\n"
            + "___.getNewModuleHandler().setImports(testImports);",
            "benchmark-container"),
        new RhinoTestBed.Input(cajoledJs, getName()),
	new RhinoTestBed.Input(
            "testImports.benchmark.score",
            "score"));
    return score.doubleValue();
  }
}
