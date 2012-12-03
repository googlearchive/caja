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

  public void testVersionSkewGuestFrame() throws Exception {
    // Changing the version baked into the guest frame JS will cause a
    // version mismatch error in caja.js.
    addVersionRewrite("/" + bv + "/es53-guest-frame.opt.js", "0000");
    runTestDriver("es53-test-cajajs-version-skew-js-files.js", false);
    // TODO(kpreid): This test fails (does not detect skew) under ES5 mode,
    // since es53-guest-frame is not used. We should add version skew testing
    // for ES5 mode as well.
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

}
