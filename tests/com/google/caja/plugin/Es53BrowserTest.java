// Copyright (C) 2012 Google Inc.
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

import com.google.caja.util.FailureIsAnOption;

public class Es53BrowserTest extends UniversalBrowserTests {
  public Es53BrowserTest() {
    super(false /* es5Mode */);
  }

  public void testCajaJsBare() throws Exception {
    runBrowserTest("cajajs-bare-test.html", "es5=false");
  }

  public final void testPrecajole() throws Exception {
    // Only relevant to es53.
    runTestCase("es53-test-precajole-guest.html", false);
  }

  public void testVersionSkewCajolerResponse() throws Exception {
    // Changing the version baked into *all* the JS will cause an incorrect
    // version number to be sent to the cajoler, which should then refuse
    // to compile the given content and return an error instead.
    runTestDriver("es53-test-cajajs-version-skew-cajoler-response.js",
        false, "cajajs=/caja/testing/skew-0000/caja.js");
  }

  public void testVersionMinorSkewCajolerResponse() throws Exception {
    // Changing the version baked into *all* the JS will cause a different
    // minor version number to be sent to the cajoler, which should emit a
    // LINT warning when compiling the given content
    runTestDriver("es53-test-cajajs-minor-version-skew-cajoler-response.js",
        false, "cajajs=/caja/testing/skew-mmm/caja.js");
  }

  public void testVersionSkewCajoledModule() throws Exception {
    // only relevant to es53
    runTestDriver("es53-test-cajajs-version-skew-cajoled-module.js", false);
  }

  public void testMinorVersionSkewCajoledModule() throws Exception {
    // only relevant to es53
    runTestDriver(
      "es53-test-cajajs-minor-version-skew-cajoled-module.js", false);
  }

  public void testProxies() throws Exception {
    runTestDriver("es53-test-proxies.js", false);
  }

  public void testTamingPrimitives() throws Exception {
    runTestDriver("es53-test-taming-primitives.js", false);
  }

  public void testAutoMode1() throws Exception {
    runTestDriver("es53-test-automode1.js", false);
  }

  public void testAutoMode2() throws Exception {
    runTestDriver("es53-test-automode2.js", false);
  }

  public void testAutoMode3() throws Exception {
    runTestDriver("es53-test-automode3.js", false);
  }

  public void testAutoMode4() throws Exception {
    runTestDriver("es53-test-automode4.js", false);
  }

  // See http://code.google.com/p/google-caja/issues/detail?id=1621
  // TODO(jasvir): Move this test back into UniversalBrowserTests once this
  // caja.js api supports testing cajoling errors
  @FailureIsAnOption("Cajoling errors in ES53 not accessible via caja.js api")
  public void testUnicode() throws Exception {
    runTestDriver("es53-test-unicode.js", es5Mode);
  }
}
