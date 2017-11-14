// Copyright (C) 2011 Google Inc.
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

package com.google.caja.precajole;

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.html.Dom;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.plugin.PluginCompiler;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.plugin.UriPolicy;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.TestBuildInfo;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Join;

public class PrecajoledPipelineTest extends CajaTestCase {

  public final void testPretty() throws Exception {
    PluginCompiler compiler = makeCompiler(false);
    addHtml(compiler,
        "<script src='data:banana'></script>"
        + "<script src='data:capricorn'></script>"
        + "<script>cached:dalmatian</script>");
    assertTrue(compiler.run());
    String js = render(compiler.getJavascript());
    assertContains(js, "'precajoled': 'data:banana false'");
    assertContains(js, "'precajoled': 'data:capricorn false'");
    assertContains(js, "'precajoled': 'cached:dalmatian false'");
    assertPrecajoleHit("data:banana");
    assertPrecajoleHit("data:capricorn");
    assertNoWarnings();
  }

  public final void testMinified() throws Exception {
    PluginCompiler compiler = makeCompiler(true);
    addHtml(compiler,
        "<script src='data:banana'></script>"
        + "<script src='data:capricorn'></script>"
        + "<script>cached:dalmatian</script>");
    assertTrue(compiler.run());
    String js = render(compiler.getJavascript());
    assertContains(js, "'precajoled': 'data:banana true'");
    assertContains(js, "'precajoled': 'data:capricorn true'");
    assertContains(js, "'precajoled': 'cached:dalmatian true'");
    assertPrecajoleHit("data:banana");
    assertPrecajoleHit("data:capricorn");
    assertNoWarnings();
  }

  private void assertPrecajoleHit(String uri) throws Exception {
    assertMessage(true, MessageType.PRECAJOLE_HIT, MessageLevel.LOG,
        MessagePart.Factory.valueOf(uri));
  }

  private PluginCompiler makeCompiler(boolean minify) {
    PluginMeta meta = new PluginMeta(
        UriFetcher.NULL_NETWORK, UriPolicy.IDENTITY);
    meta.setPrecajoleMap(new MockPrecajoleMap());
    meta.setPrecajoleMinify(minify);
    PluginCompiler compiler = new PluginCompiler(
        TestBuildInfo.getInstance(), meta, mq);
    compiler.setMessageContext(mc);
    return compiler;
  }

  private void addHtml(PluginCompiler compiler, String... lines)
  throws ParseException {
    String html = Join.join("\n", lines);
    Dom dom = Dom.transplant(html(fromString(html)));
    compiler.addInput(dom, dom.getFilePosition().source().getUri());
  }

  private class MockPrecajoleMap implements PrecajoleMap {
    @Override
    public CajoledModule lookupUri(String uri, boolean minify) {
      if (!uri.startsWith("data:")) {
        return null;
      } else {
        return makeModule(uri, minify);
      }
    }

    @Override
    public CajoledModule lookupSource(String source, boolean minify) {
      if (!source.startsWith("cached:")) {
        return null;
      } else {
        return makeModule(source, minify);
      }
    }

    private CajoledModule makeModule(String s, boolean minify) {
      try {
        Expression js = jsExpr(fromString(
            "{ precajoled: '" + s + " " + minify + "' }"));
        return new CajoledModule((ObjectConstructor) js);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

}
