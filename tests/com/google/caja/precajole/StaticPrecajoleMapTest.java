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

import com.google.caja.parser.js.CajoledModule;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;

@SuppressWarnings("static-method")
public class StaticPrecajoleMapTest extends CajaTestCase {

  private static String CANARY1 =
      "http://caja.appspot.com/imaginary-1/canary1.js";

  private static String CANARY1_TEXT =
      "window.canary1 = (window.canary1 || 0) + 1;";

  // TODO(felix8a): fails when running in eclipse
  public final void testJunitPretty() throws Exception {
    PrecajoleMap pm = StaticPrecajoleMap.getInstance();
    CajoledModule cm = pm.lookupUri(CANARY1, false);
    assertNotNull(cm);
    String result = render(cm);
    assertContains(result, "cajolerVersion");
    assertContains(result, "canary1");
    assertContains(result, "    ");
  }

  // TODO(felix8a): fails when running in eclipse
  public final void testJunitMinified() throws Exception {
    PrecajoleMap pm = StaticPrecajoleMap.getInstance();
    CajoledModule cm = pm.lookupUri(CANARY1, true);
    assertNotNull(cm);
    String result = render(cm);
    assertContains(result, "canary1");
    assertNotContains(result, "  ");
  }

  public final void testUnknownUri() throws Exception {
    PrecajoleMap pm = StaticPrecajoleMap.getInstance();
    CajoledModule cm = pm.lookupUri("http://unknown/", false);
    assertNull(cm);
  }

  public final void testInvalidUri() throws Exception {
    PrecajoleMap pm = StaticPrecajoleMap.getInstance();
    CajoledModule cm = pm.lookupUri(":", false);
    assertNull(cm);
  }

  public final void testSource1() {
    PrecajoleMap pm = StaticPrecajoleMap.getInstance();
    CajoledModule cm = pm.lookupSource(CANARY1_TEXT, false);
    assertNotNull(cm);
    String result = render(cm);
    assertContains(result, "precajole:");
    assertContains(result, "canary1");
  }

  public final void testSource2() {
    PrecajoleMap pm = StaticPrecajoleMap.getInstance();
    CajoledModule cm = pm.lookupSource(" " + CANARY1_TEXT + "\n", false);
    assertNotNull(cm);
    String result = render(cm);
    assertContains(result, "precajole:");
    assertContains(result, "canary1");
  }

  private final String render(CajoledModule cm) {
    StringBuilder buf = new StringBuilder();
    RenderContext rc = new RenderContext(
        new JsMinimalPrinter(new Concatenator(buf)));
    cm.render(rc);
    rc.getOut().noMoreTokens();
    return buf.toString();
  }
}
