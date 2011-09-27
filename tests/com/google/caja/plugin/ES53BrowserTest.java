// Copyright (C) 2010 Google Inc.
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

import com.google.caja.reporting.BuildInfo;

// TODO(kpreid): Rename this file, as it now tests the ES5 as well as ES53
// environment.

/**
 * @author ihab.awad@gmail.com
 */
public class ES53BrowserTest extends BrowserTestCase {
  private final String bv = BuildInfo.getInstance().getBuildVersion();

  public final void testCajaJsInvocations() {
    runTestDriver("es53-test-cajajs-invocation.js");
  }

  public final void testBasicFunctions() {
    // TODO(kpreid): Enable for ES5: tests for load(), reassigning-window test
    runTestCase("es53-test-basic-functions-guest.html");
  }

  private final void addVersionRewrite(String path, String newVersion) {
    getCajaStatic().rewrite(path, bv, newVersion);
  }

  public final void testVersionSkewCajaJs() {
    closeWebDriver();  // Need a browser with an empty cache
    // Changing the version baked into caja.js will cause it to load the
    // wrongly-named files for the host and guest frames, which should cause
    // it to never make progress in load() or whenReady() calls.
    addVersionRewrite("/caja.js", "0000");
    runTestDriver("es53-test-cajajs-never-starts.js");
  }

  public final void testWrongSupportingResources() {
    closeWebDriver();  // Need a browser with an empty cache
    // Placing the wrong resources files where caja.js is expecting them should
    // cause it to never make progress in load() or whenReady() calls.
    cajaStatic.link(
        "/" + bv + "/non-existent.js",
        "/" + bv + "/es53-guest-frame.opt.js");
    cajaStatic.link(
        "/" + bv + "/non-existent.js",
        "/" + bv + "/es53-taming-frame.opt.js");
    runTestDriver("es53-test-cajajs-never-starts.js");
  }

  public final void testAlternateLocationSupportingResources() {
    closeWebDriver();  // Need a browser with an empty cache
    // Placing the wrong resources files where caja.js is expecting them should
    // cause it to never make progress in load() or whenReady() calls.
    cajaStatic.link(
        "/" + bv + "/es53-guest-frame.opt.js",
        "/" + bv + "/alternative/es53-guest-frame.opt.js");        
    cajaStatic.link(
        "/" + bv + "/es53-taming-frame.opt.js",
        "/" + bv + "/alternative/es53-taming-frame.opt.js");
    cajaStatic.link(
        "/" + bv + "/non-existent.js",
        "/" + bv + "/es53-guest-frame.opt.js");
    cajaStatic.link(
        "/" + bv + "/non-existent.js",
        "/" + bv + "/es53-taming-frame.opt.js");
    runTestDriver("es53-test-cajajs-never-starts.js");
  }

  public final void testVersionSkewTamingFrame() {
    closeWebDriver();  // Need a browser with an empty cache
    // Changing the version baked into the taming frame JS will cause a
    // version mismatch error in caja.js.
    addVersionRewrite("/" + bv + "/es53-taming-frame.opt.js", "0000");
    runTestDriver("es53-test-cajajs-version-skew-js-files.js");
  }

  public final void testVersionSkewGuestFrame() {
    closeWebDriver();  // Need a browser with an empty cache
    // Changing the version baked into the guest frame JS will cause a
    // version mismatch error in caja.js.
    addVersionRewrite("/" + bv + "/es53-guest-frame.opt.js", "0000");
    runTestDriver("es53-test-cajajs-version-skew-js-files.js", false);
    // TODO(kpreid): This test fails (does not detect skew) under ES5 mode,
    // since es53-guest-frame is not used. We should add version skew testing
    // for ES5 mode as well.
  }

  public final void testVersionSkewCajolerResponse() {
    closeWebDriver();  // Need a browser with an empty cache
    // Changing the version baked into *all* the JS will cause an incorrect
    // version number to be sent to the cajoler, which should then refuse
    // to compile the given content and return an error instead.
    addVersionRewrite("/caja.js", "0000");
    cajaStatic.link(
        "/" + bv + "/es53-guest-frame.opt.js",
        "/0000/es53-guest-frame.opt.js");
    cajaStatic.link(
        "/" + bv + "/es53-taming-frame.opt.js",
        "/0000/es53-taming-frame.opt.js");
    addVersionRewrite("/0000/es53-guest-frame.opt.js", "0000");
    addVersionRewrite("/0000/es53-taming-frame.opt.js", "0000");
    runTestDriver("es53-test-cajajs-version-skew-cajoler-response.js");
  }

  public final void testVersionSkewCajoledModule() {
    closeWebDriver();  // Need a browser with an empty cache
    runTestDriver("es53-test-cajajs-version-skew-cajoled-module.js");
  }

  public final void testClientUriRewriting() {
    // TODO(kpreid): Enable for ES5. Fails due to relative/absolute confusion
    // and no CSS implementation.
    runTestDriver("es53-test-client-uri-rewriting.js", false);
  }

  public final void testTamingTamed() {
    // TODO(kpreid): Enable for ES5 once taming membrane implemented
    runTestDriver("es53-test-taming-tamed.js", false);
  }

  public final void testTamingUntamed() {
    // TODO(kpreid): Enable for ES5 once taming membrane implemented
    runTestDriver("es53-test-taming-untamed.js", false);
  }

  public final void testTamingInout() {
    // TODO(kpreid): Enable for ES5 once taming membrane implemented
    runTestDriver("es53-test-taming-inout.js", false);
  }

  public final void testTamingErrors() {
    // TODO(kpreid): Enable for ES5 once taming membrane implemented
    runTestDriver("es53-test-taming-errors.js", false);
  }

  public final void testDomadoCanvas() {
    // TODO(kpreid): Enable for ES5. Fails on Firefox due to SES issues --
    // "access to strict mode caller function is censored"; fails on Chrome
    // due to a context property access problem.
    runTestCase("es53-test-domado-canvas-guest.html", false);
  }

  public final void testDomadoDom() {
    // TODO(kpreid): Enable for ES5: various tests for unimplemented
    // functionality.
    runTestCase("es53-test-domado-dom-guest.html");
  }

  public final void testDomadoEvents() {
    // TODO(kpreid): Enable testOnclickHandler for ES5.
    runTestDriver("es53-test-domado-events.js");
  }

  public final void testDomadoForms() {
    runTestCase("es53-test-domado-forms-guest.html");
  }

  public final void testDomadoSpecial() {
    // TODO(kpreid): Enable for ES5. Currently fails because <script> elements
    // are not getting inserted in the DOM properly
    runTestDriver("es53-test-domado-special.js");
  }

  public final void testDomadoOpaque() {
    // TODO(kpreid): Enable for ES5 once HTML scripting works
    runTestDriver("es53-test-domado-foreign.js", false);
  }

  public final void testLanguage() {
    runTestCase("es53-test-language-guest.html");
  }

  public final void testProxies() {
    runTestDriver("es53-test-proxies.js");
  }

  public final void testInlineScript() {
    // TODO(kpreid): Enable for ES5. Currently fails by showing script text.
    runTestCase("es53-test-inline-script.html", false);
  }
}
