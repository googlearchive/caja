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

import com.google.caja.lexer.CharProducer;
import com.google.caja.parser.AncestorChain;
import com.google.caja.plugin.PluginCompiler;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.reporting.TestBuildInfo;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Executor;
import com.google.caja.util.RhinoTestBed;

/**
 * Unit test which executes the V8 benchmark
 * and collates the result for rendering with varz
 */
public class BenchmarkRunner extends CajaTestCase {
  public final void testRichards() throws Exception {
    runBenchmark("v8-richards.js");
  }
  public final void testDeltaBlue() throws Exception {
    runBenchmark("v8-deltablue.js");
  }
  public final void testCrypto() throws Exception {
    runBenchmark("v8-crypto.js");
  }
  public final void testRayTrace() throws Exception {
    runBenchmark("v8-raytrace.js");
  }
  public final void testEarleyBoyer() throws Exception {
    runBenchmark("v8-earley-boyer.js");
  }
  public final void testFunctionClosure() throws Exception {
    runBenchmark("function-closure.js");
  }
  public final void testFunctionCorrectArgs() throws Exception {
    runBenchmark("function-correct-args.js");
  }
  public final void testFunctionEmpty() throws Exception {
    runBenchmark("function-empty.js");
  }
  public final void testFunctionExcessArgs() throws Exception {
    runBenchmark("function-excess-args.js");
  }
  public final void testFunctionMissingArgs() throws Exception {
    runBenchmark("function-missing-args.js");
  }
  public final void testFunctionSum() throws Exception {
    runBenchmark("function-sum.js");
  }
  public final void testLoopEmptyResolve() throws Exception {
    runBenchmark("loop-empty-resolve.js");
  }
  public final void testLoopEmpty() throws Exception {
    runBenchmark("loop-empty.js");
  }
  public final void testLoopSum() throws Exception {
    runBenchmark("loop-sum.js");
  }


  /**
   * Runs the given benchmark
   * Accumulates the result and formats it for consumption by varz
   * Format:
   * VarZ:benchmark.<benchmark name>.<speed|size>.<language>
   *               .<debug?>.<engine>.<primed?>
   */
  private void runBenchmark(String filename) throws Exception {
    double uncajoledTime = runUncajoled(filename);
    double cajitaTime = runCajoled(filename, false, false);
    double valijaTime = runCajoled(filename, true, false);
    double valijaWrappedTime = runCajoled(filename, true, true);
    double cajitaWrappedTime = runCajoled(filename, false, true);

    varz(getName(), "uncajoled", "time", uncajoledTime);
    varz(getName(), "valija", "time", valijaTime);
    varz(getName(), "cajita", "time", cajitaTime);

    varz(getName(), "valija", "timeratio",
        valijaTime < 0 ? -1 : valijaTime / uncajoledTime);
    varz(getName(), "cajita", "timeratio",
        cajitaTime < 0 ? -1 : cajitaTime / uncajoledTime);

    // We rename the test here because wrapping globals is an optimization
    // that changes the benchmark -- albeit a trivial one that is easy for
    // developers to perform on their own code.
    varz(getName() + "WrapGlobals", "valija", "time", valijaWrappedTime);
    varz(getName() + "WrapGlobals", "cajita", "time", cajitaWrappedTime);

    varz(getName() + "WrapGlobals", "valija", "timeratio",
        valijaWrappedTime < 0 ? -1 : valijaWrappedTime / uncajoledTime);
    varz(getName() + "WrapGlobals", "cajita", "timeratio",
        cajitaWrappedTime < 0 ? -1 : cajitaWrappedTime / uncajoledTime);
  }

  private void varz(String name, String lang, String feature, double value) {
    System.out.println(
        "VarZ:benchmark." + name + "." + feature + "." + lang +
        ".nodebug.rhino.cold=" + value);
  }

  private String wrapGlobals(String nakedJS) {
    return "(function() {" + nakedJS + "})();";
  }

  private double runUncajoled(String filename) throws Exception {
    Number elapsed = (Number) RhinoTestBed.runJs(
        new Executor.Input("var benchmark = {};", "setup"),
        new Executor.Input("benchmark.startTime = new Date();", "clock"),
        new Executor.Input(getClass(), filename),
        new Executor.Input("(new Date() - benchmark.startTime)", "elapsed"));
    return elapsed.doubleValue();
  }

  private double runCajoled(String filename, boolean valija,
      boolean wrapGlobals) throws Exception {
    PluginMeta meta = new PluginMeta();
    MessageQueue mq = new SimpleMessageQueue();
    PluginCompiler pc = new PluginCompiler(new TestBuildInfo(), meta, mq);
    CharProducer src = wrapGlobals ?
        fromString(wrapGlobals(plain(fromResource(filename)))):
            fromString(plain(fromResource(filename)));
    pc.addInput(AncestorChain.instance(valija
        ? BenchmarkUtils.addUseCajitaDirective(js(src))
        : js(src)), is.getUri());
    if (!pc.run()) {
      return -1;
    }
    String cajoledJs = render(pc.getJavascript());
    System.err.println("-- Cajoled:" + filename +
          "(wrapped: " + wrapGlobals +
          ", valija:" + valija + ") --\n" + cajoledJs + "\n---\n");
    Number elapsed = (Number) RhinoTestBed.runJs(
        new Executor.Input(getClass(),
            "../../../../../js/json_sans_eval/json_sans_eval.js"),
        new Executor.Input(getClass(), "../../cajita.js"),
        new Executor.Input(
            ""
            + "var testImports = ___.copy(___.sharedImports);\n"
            + "testImports.loader = ___.freeze({\n"
            + "        provide: ___.markFuncFreeze(\n"
            + "            function(v){ valijaMaker = v; })\n"
            + "    });\n"
            + "testImports.outers = ___.copy(___.sharedImports);\n"
            + "___.getNewModuleHandler().setImports(testImports);",
            getName() + "valija-setup"),
        new Executor.Input(getClass(), "../../plugin/valija.out.js"),
        new Executor.Input(
            // Set up the imports environment.
            ""
            + "testImports = ___.copy(___.sharedImports);\n"
            + "testImports.benchmark = {};\n"
            + "testImports.benchmark.startTime = new Date();"
            + "testImports.$v = valijaMaker.CALL___(testImports);\n"
            + "___.getNewModuleHandler().setImports(testImports);",
            "benchmark-container"),
        new Executor.Input(cajoledJs, getName()),
        new Executor.Input(
            "(new Date() - testImports.benchmark.startTime)",
            "elapsed"));
    return elapsed.doubleValue();
  }
}
