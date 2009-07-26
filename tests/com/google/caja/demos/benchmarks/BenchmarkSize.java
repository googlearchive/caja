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
import com.google.caja.reporting.TestBuildInfo;
import com.google.caja.util.CajaTestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPOutputStream;

/**
 * Unit test which measures the size of cajoled javascript
 * 
 * @author Jasvir Nagra (jasvir@gmail.com)
 */
public class BenchmarkSize extends CajaTestCase {

  // Javascript files to benchmark
  // TODO(jasvir): Find a nice collection of "typical" html files!
  String[] pureJs = {
      "richards.js",
      "deltablue.js",
      "crypto.js",
      "raytrace.js"
  };

  /**
   * Measures the size of cajoled vs original javascript
   * Accumulates the result and formats it for consumption by varz
   * Format:
   * VarZ:benchmark.<benchmark>.size.<html|js>.<original|cajita|valija>
   *               .<pretty|minified>.<plain|gzip>
   */
  public void testJavascript () throws ParseException, IOException {
    for (String js : pureJs) {
      String name = stripExt(js, ".js");
      varz(name, "original", "pretty", "plain", 
          size(charset(plain(fromResource(js)))));
      varz(name, "original", "pretty", "gzip", 
          size(gzip(charset((plain(fromResource(js)))))));
      varz(name, "original", "minified", "plain", 
          size(charset(minify(js(fromResource(js))))));
      varz(name, "original", "minified", "gzip", 
          size(gzip(charset(minify(js(fromResource(js)))))));

      varz(name, "valija", "pretty", "plain", 
          size(charset(render(valija(js(fromResource(js)))))));
      varz(name, "valija", "pretty", "gzip", 
          size(gzip(charset(render(valija(js(fromResource(js))))))));
      varz(name, "valija", "minify", "plain", 
          size(charset(minify(valija(js(fromResource(js)))))));
      varz(name, "valija", "minify", "gzip", 
          size(gzip(charset(minify(valija(js(fromResource(js))))))));
    }
  }

  private int size(byte[] data) {
    return data.length;
  }
    
  private void varz(String name, String lang, String rendering, String enc,
      long value) {
    System.out.println("VarZ:benchmark." + name + ".size.js." +
        lang + "." + rendering +"." + enc + "=" + value);
  }
  
  private String stripExt(String filename, String extension) {
    if (filename.endsWith(extension)) {
      return filename.substring(0, filename.length() - extension.length());
    }
    return filename;
  }
  
  public byte[] charset(String v) throws UnsupportedEncodingException {
    return v.getBytes("UTF-8");
  }
  
  public byte[] gzip(byte[] data) throws IOException {
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

  public CajoledModule cajole(Block js, boolean valija) {
    PluginMeta meta = new PluginMeta();
    meta.setValijaMode(valija);
    PluginCompiler pc = new PluginCompiler(new TestBuildInfo(), meta, mq);
    pc.addInput(AncestorChain.instance(js));
    assertTrue(pc.run());
    return pc.getJavascript();
  }
}
