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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPOutputStream;

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.plugin.PluginCompiler;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.TestBuildInfo;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.TestUtil;

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
    String plainCajita =
      plain(fromResource("../../plugin/domita-minified.js"));

    String plainValija = plainCajita
        + plain(fromResource("../../plugin/valija.out.js"));
    
    String plainES53 =
      plain(fromResource("../../plugin/es53-taming-frame.opt.js"));

    byte[] plainCajitaBytes = charset(plainCajita);
    byte[] plainValijaBytes = charset(plainValija);
    byte[] plainES53Bytes = charset(plainES53);
    
    byte[] gzipCajitaBytes = gzip(plainCajitaBytes);
    byte[] gzipValijaBytes = gzip(plainValijaBytes);
    byte[] gzipES53Bytes = gzip(plainES53Bytes);
        
    varzOverhead("valija", "minify", "plain", size(plainValijaBytes));
    varzOverhead("valija", "minify", "gzip", size(gzipValijaBytes));

    varzOverhead("cajita", "minify", "plain", size(plainCajitaBytes));
    varzOverhead("cajita", "minify", "gzip", size(gzipCajitaBytes));

    varzOverhead("es53", "minify", "plain", size(plainES53Bytes));
    varzOverhead("es53", "minify", "gzip", size(gzipES53Bytes));
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

      String originalPrettyPlain = plain(fromResource(js));
      byte[] originalPrettyPlainBytes = charset(originalPrettyPlain);
      byte[] originalPrettyGzipBytes = gzip(originalPrettyPlainBytes);

      String originalMinifyPlain = minify(js(fromResource(js)));
      byte[] originalMinifyPlainBytes = charset(originalMinifyPlain);
      byte[] originalMinifyGzipBytes = gzip(originalMinifyPlainBytes);
      
      String cajitaPrettyPlain = render(cajita(js(fromResource(js))));
      byte[] cajitaPrettyPlainBytes = charset(cajitaPrettyPlain);
      byte[] cajitaPrettyGzipBytes = gzip(cajitaPrettyPlainBytes);

      String cajitaMinifyPlain = minify(cajita(js(fromResource(js))));
      byte[] cajitaMinifyPlainBytes = charset(cajitaMinifyPlain);
      byte[] cajitaMinifyGzipBytes = gzip(cajitaMinifyPlainBytes);

      String valijaPrettyPlain = render(valija(js(fromResource(js))));
      byte[] valijaPrettyPlainBytes = charset(valijaPrettyPlain);
      byte[] valijaPrettyGzipBytes = gzip(valijaPrettyPlainBytes);

      String valijaMinifyPlain = minify(valija(js(fromResource(js))));
      byte[] valijaMinifyPlainBytes = charset(valijaMinifyPlain);
      byte[] valijaMinifyGzipBytes = gzip(valijaMinifyPlainBytes);

      String es53PrettyPlain = render(es53(js(fromResource(js))));
      byte[] es53PrettyPlainBytes = charset(es53PrettyPlain);
      byte[] es53PrettyGzipBytes = gzip(es53PrettyPlainBytes);

      String es53MinifyPlain = minify(es53(js(fromResource(js))));
      byte[] es53MinifyPlainBytes = charset(es53MinifyPlain);
      byte[] es53MinifyGzipBytes = gzip(es53MinifyPlainBytes);

      varzJS(name, "original", "pretty", "plain",
          size(originalPrettyPlainBytes));
      varzJS(name, "original", "pretty", "gzip",
          size(originalPrettyGzipBytes));

      varzJS(name, "original", "minify", "plain",
          size(originalMinifyPlainBytes));
      varzJS(name, "original", "minify", "gzip",
          size(originalMinifyGzipBytes));

      varzJS(name, "cajita", "pretty", "plain", size(cajitaPrettyPlainBytes));
      varzJS(name, "cajita", "pretty", "gzip", size(cajitaPrettyGzipBytes));
      
      varzJS(name, "cajita", "minify", "plain", size(cajitaMinifyPlainBytes));
      varzJS(name, "cajita", "minify", "gzip", size(cajitaMinifyGzipBytes));

      varzJS(name, "valija", "pretty", "plain", size(valijaPrettyPlainBytes));
      varzJS(name, "valija", "pretty", "gzip", size(valijaPrettyGzipBytes));
      
      varzJS(name, "valija", "minify", "plain", size(valijaMinifyPlainBytes));
      varzJS(name, "valija", "minify", "gzip", size(valijaMinifyGzipBytes));
      
      varzJS(name, "es53", "pretty", "plain", size(es53PrettyPlainBytes));
      varzJS(name, "es53", "pretty", "gzip", size(es53PrettyGzipBytes));
      
      varzJS(name, "es53", "minify", "plain", size(es53MinifyPlainBytes));
      varzJS(name, "es53", "minify", "gzip", size(es53MinifyGzipBytes));
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
  
  public CajoledModule es53(Block plain) {
    CajoledModule result = null;
    PluginMeta meta = new PluginMeta();
    MessageQueue mq = TestUtil.createTestMessageQueue(this.mc);
    PluginCompiler pc = new PluginCompiler(
        TestBuildInfo.getInstance(), meta, mq);
    pc.addInput(plain, null);
    if (pc.run()) {
      result = pc.getJavascript();
      return result;
    } else {
      return null;
    }
  }

  public CajoledModule cajole(Block js, boolean valija) {
    CajoledModule result = null;
    PluginMeta meta = new PluginMeta();
    MessageQueue mq = TestUtil.createTestMessageQueue(this.mc);
    if (!valija) { js = BenchmarkUtils.addUseCajitaDirective(js); }
    PluginCompiler pc = new PluginCompiler(
        TestBuildInfo.getInstance(), meta, mq);
    pc.addInput(js, null);
    if (pc.run()) {
      result = pc.getJavascript();
      return result;
    } else {
      return null;
    }
  }
}
