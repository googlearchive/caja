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

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.plugin.PluginCompiler;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.TestBuildInfo;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.TestUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;

/**
 * Unit test which measures the size of cajoled javascript
 *
 * @author Jasvir Nagra (jasvir@gmail.com)
 */
public class BenchmarkSize extends CajaTestCase {

  // Javascript files to benchmark
  // TODO(jasvir): Find a nice collection of "typical" html files!
  String[][] pureJs = {
      {"v8-richards.js", "testRichards"},
      {"v8-deltablue.js", "testDeltaBlue"},
      {"v8-crypto.js", "testCrypto"},
      {"v8-earley-boyer.js", "testEarleyBoyer"},
      {"v8-raytrace.js", "testRayTrace"},
      {"function-closure.js", "testFunctionClosure"},
      {"function-correct-args.js", "testFunctionCorrectArgs"},
      {"function-empty.js", "testFunctionEmpty"},
      {"function-excess-args.js", "testFunctionExcessArgs"},
      {"function-missing-args.js", "testFunctionMissingArgs"},
      {"function-sum.js", "testFunctionSum"},
      {"loop-empty-resolve.js", "testLoopEmptyResolve"},
      {"loop-empty.js", "testLoopEmpty"},
      {"loop-sum.js", "testLoopSum"},
  };

  public final void testOverhead() throws IOException {
    varzOverhead("valija", "minify", "plain",
        size(charset(plain(fromResource("../../plugin/domita-minified.js")))) +
        size(charset(plain(fromResource("../../plugin/valija.out.js")))) +
        size(charset(plain(
            fromResource("../../plugin/html-sanitizer-minified.js")))));
    varzOverhead("valija", "minify", "gzip",
        size(gzip(charset(plain(
            fromResource("../../plugin/domita-minified.js"))))) +
        size(gzip(charset(plain(fromResource("../../plugin/valija.out.js"))))) +
        size(gzip(charset(plain(
            fromResource("../../plugin/html-sanitizer-minified.js"))))));

    varzOverhead("cajita", "minify", "plain",
        size(charset(plain(fromResource("../../plugin/domita-minified.js")))) +
        size(charset(plain(
            fromResource("../../plugin/html-sanitizer-minified.js")))));
    varzOverhead("cajita", "minify", "gzip",
        size(gzip(charset(plain(
            fromResource("../../plugin/domita-minified.js"))))) +
        size(gzip(charset(plain(
            fromResource("../../plugin/html-sanitizer-minified.js"))))));
  }

  /**
   * Measures the size of cajoled vs original javascript
   * Accumulates the result and formats it for consumption by varz
   * Format:
   * VarZ:benchmark.<benchmark>.size.<html|js>.<original|cajita|valija>
   *               .<pretty|minified>.<plain|gzip>
   */
  public final void testJavascript() throws ParseException, IOException {
    for (String[] pair : pureJs) {
      String js = pair[0];
      String name = pair[1];
      varzJS(name, "original", "pretty", "plain",
          size(charset(plain(fromResource(js)))));
      varzJS(name, "original", "pretty", "gzip",
          size(gzip(charset((plain(fromResource(js)))))));
      varzJS(name, "original", "minify", "plain",
          size(charset(minify(js(fromResource(js))))));
      varzJS(name, "original", "minify", "gzip",
          size(gzip(charset(minify(js(fromResource(js)))))));

      varzJS(name, "cajita", "pretty", "plain",
          size(charset(render(cajita(js(fromResource(js)))))));
      varzJS(name, "cajita", "pretty", "gzip",
          size(gzip(charset(render(cajita(js(fromResource(js))))))));
      varzJS(name, "cajita", "minify", "plain",
          size(charset(minify(cajita(js(fromResource(js)))))));
      varzJS(name, "cajita", "minify", "gzip",
          size(gzip(charset(minify(cajita(js(fromResource(js))))))));

      varzJS(name, "valija", "pretty", "plain",
          size(charset(render(valija(js(fromResource(js)))))));
      varzJS(name, "valija", "pretty", "gzip",
          size(gzip(charset(render(valija(js(fromResource(js))))))));
      varzJS(name, "valija", "minify", "plain",
          size(charset(minify(valija(js(fromResource(js)))))));
      varzJS(name, "valija", "minify", "gzip",
          size(gzip(charset(minify(valija(js(fromResource(js))))))));
    }
  }

  private int size(byte[] data) {
    return data == null ? -1 : data.length;
  }

  private void varzJS(String name, String lang, String rendering, String enc,
      long value) {
    System.out.println("VarZ:benchmark." + name + ".size.js." +
        lang + "." + rendering +"." + enc + "=" + value);
  }

  private void varzOverhead(String lang, String rendering, String enc,
      long value) {
    System.out.println("VarZ:benchmark.size.overhead." +
        lang + "." + rendering +"." + enc + "=" + value);
  }

  public byte[] charset(String v) throws UnsupportedEncodingException {
    return v == null ? null : v.getBytes("UTF-8");
  }

  public byte[] gzip(byte[] data) throws IOException {
    if (data == null) {
      return null;
    }
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    GZIPOutputStream gzipper = new GZIPOutputStream(stream);
    gzipper.write(data);
    gzipper.finish();
    return stream.toByteArray();
  }

  public CajoledModule valija(Block plain) {
    return cajole(plain, true);
  }

  public CajoledModule cajita(Block plain) {
    return cajole(plain, false);
  }

  HashMap<Block,CajoledModule> cMemo = new HashMap<Block, CajoledModule>();
  HashMap<Block, CajoledModule> vMemo = new HashMap<Block, CajoledModule>();
  public CajoledModule cajole(Block js, boolean valija) {
    CajoledModule result;
    if (valija) {
      result = vMemo.get(js);
    } else {
      result = cMemo.get(js);
    }
    if (result != null) {
      return result;
    }
    PluginMeta meta = new PluginMeta();
    MessageQueue mq = TestUtil.createTestMessageQueue(this.mc);
    if (!valija) { js = BenchmarkUtils.addUseCajitaDirective(js); }
    PluginCompiler pc = new PluginCompiler(new TestBuildInfo(), meta, mq);
    pc.addInput(AncestorChain.instance(js));
    if (pc.run()) {
      result = pc.getJavascript();
      if (valija) {
        vMemo.put(js, result);
      } else {
        cMemo.put(js, result);
      }
      return result;
    } else {
      return null;
    }
  }
}
