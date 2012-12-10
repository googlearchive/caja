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

public class Es5BrowserTest extends UniversalBrowserTests {
  public Es5BrowserTest() {
    super(true /* es5Mode */);
  }

  public void testCajaJsBare() throws Exception {
    runBrowserTest("cajajs-bare-test.html", "es5=true");
  }

  public void testExternalScript() throws Exception {
    runTestCase("es53-test-external-script-guest.html", true);
  }

  // The tests that use es53-test-cajajs-never-starts.js are not really
  // es5-specific, but there's no point in running them for both es5
  // and es53 mode, because we're expecting the test to throw an error
  // before th es5-vs-es53-mode decision.

  public void testVersionSkewCajaJs() throws Exception {
    // Changing the version baked into caja.js will cause it to load the
    // wrongly-named files for the host and guest frames, which should cause
    // it to never make progress in load() or whenReady() calls.
    addVersionRewrite("/caja.js", "0000");
    addVersionRewrite("/caja-minified.js", "0000");
    runTestDriver("es53-test-cajajs-never-starts.js", es5Mode);
  }

  public void testWrongSupportingResources() throws Exception {
    // Placing the wrong resources files where caja.js is expecting them should
    // cause it to never make progress in load() or whenReady() calls.
    getCajaStatic().link(
        "/" + bv + "/non-existent.js",
        "/" + bv + "/es53-guest-frame.opt.js");
    getCajaStatic().link(
        "/" + bv + "/non-existent.js",
        "/" + bv + "/es53-taming-frame.opt.js");
    runTestDriver("es53-test-cajajs-never-starts.js", es5Mode);
  }

  public void testAlternateLocationSupportingResources()
      throws Exception {
    // Placing the wrong resources files where caja.js is expecting them should
    // cause it to never make progress in load() or whenReady() calls.
    getCajaStatic().link(
        "/" + bv + "/es53-guest-frame.opt.js",
        "/" + bv + "/alternative/es53-guest-frame.opt.js");
    getCajaStatic().link(
        "/" + bv + "/es53-taming-frame.opt.js",
        "/" + bv + "/alternative/es53-taming-frame.opt.js");
    getCajaStatic().link(
        "/" + bv + "/non-existent.js",
        "/" + bv + "/es53-guest-frame.opt.js");
    getCajaStatic().link(
        "/" + bv + "/non-existent.js",
        "/" + bv + "/es53-taming-frame.opt.js");
    runTestDriver("es53-test-cajajs-never-starts.js", es5Mode);
  }


}
