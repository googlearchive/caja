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
import com.google.caja.reporting.TestBuildInfo;

/**
 * Unit test which executes the V8 benchmark 
 * and collates the result for rendering with varz
 */
public class BenchmarkRunner extends CajaTestCase {
  public void testRichards() throws Exception {
    runBenchmark("v8-richards.js");
  }
  public void testDeltaBlue() throws Exception {
    runBenchmark("v8-deltablue.js");
  }
  public void testCrypto() throws Exception {
    runBenchmark("v8-crypto.js");
  }
  public void testRayTrace() throws Exception {
    runBenchmark("v8-raytrace.js");
  }
  public void testEarleyBoyer() throws Exception {
    runBenchmark("v8-earley-boyer.js");
  }
  public void testFunctionClosure() throws Exception {
    runBenchmark("function-closure.js"); 
  }
  public void testFunctionCorrectArgs() throws Exception { 
    runBenchmark("function-correct-args.js"); 
  }
  public void testFunctionEmpty() throws Exception { 
    runBenchmark("function-empty.js"); 
  }
  public void testFunctionExcessArgs() throws Exception {
    runBenchmark("function-excess-args.js");
  }
  public void testFunctionMissingArgs() throws Exception {
    runBenchmark("function-missing-args.js");
  }
  public void testFunctionSum() throws Exception {
    runBenchmark("function-sum.js");
  }
  public void testLoopEmptyResolve() throws Exception {
    runBenchmark("loop-empty-resolve.js"); 
  }
  public void testLoopEmpty() throws Exception {
    runBenchmark("loop-empty.js");
  }
  public void testLoopSum() throws Exception {
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
    double timeTakenUncajoled = runUncajoled(filename);
    double timeTakenCajoled = runCajoled(filename);
    
    System.out.println(
        "VarZ:benchmark." + getName() + ".time.uncajoled.nodebug.rhino.cold="
        + timeTakenUncajoled);
    System.out.println(
        "VarZ:benchmark." + getName() + ".time.valija.nodebug.rhino.cold=" 
        + timeTakenCajoled);
    System.out.println(
        "VarZ:benchmark." + getName() + ".timeratio.valija.nodebug.rhino.cold="
        + (timeTakenCajoled / timeTakenUncajoled));
  }
  
  private double runUncajoled(String filename) throws Exception {
    Number elapsed = (Number) RhinoTestBed.runJs(
        new RhinoTestBed.Input("var benchmark = {};", "setup"),
        new RhinoTestBed.Input("benchmark.startTime = new Date();", "clock"),
        new RhinoTestBed.Input(getClass(), filename),
        new RhinoTestBed.Input("(new Date() - benchmark.startTime)", "elapsed"));
    return elapsed.doubleValue();
  }

  private double runCajoled(String filename) throws Exception {
    PluginMeta meta = new PluginMeta();
    meta.setValijaMode(true);
    PluginCompiler pc = new PluginCompiler(new TestBuildInfo(), meta, mq);
    pc.addInput(AncestorChain.instance(js(fromResource(filename))));
    assertTrue(pc.run());
    String cajoledJs = render(pc.getJavascript());
    Number elapsed = (Number) RhinoTestBed.runJs(
        new RhinoTestBed.Input(getClass(), 
            "../../../../../js/json_sans_eval/json_sans_eval.js"),
        new RhinoTestBed.Input(getClass(), "../../cajita.js"),
        new RhinoTestBed.Input(
            ""
            + "var testImports = ___.copy(___.sharedImports);\n"
            + "testImports.loader = ___.freeze({\n"
            + "        provide: ___.markFuncFreeze(\n"
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
            + "testImports.benchmark.startTime = new Date();"
            + "testImports.$v = valijaMaker.CALL___(testImports);\n"
            + "___.getNewModuleHandler().setImports(testImports);",
            "benchmark-container"),
        new RhinoTestBed.Input(cajoledJs, getName()),
	new RhinoTestBed.Input(
            "(new Date() - testImports.benchmark.startTime)",
            "elapsed"));
    return elapsed.doubleValue();
  }
}
