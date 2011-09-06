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
    // TODO(kpreid): Enable for ES5 once HTML scripting works
    runTestCase("es53-test-basic-functions-guest.html", false);
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

  public final void testVersionSkewTamingFrame() {
    closeWebDriver();  // Need a browser with an empty cache
    // Changing the version baked into the taming frame JS will cause a
    // version mismatch error in caja.js.
    addVersionRewrite("/es53-taming-frame-" + bv + ".opt.js", "0000");
    runTestDriver("es53-test-cajajs-version-skew-js-files.js");
  }

  public final void testVersionSkewGuestFrame() {
    closeWebDriver();  // Need a browser with an empty cache
    // Changing the version baked into the guest frame JS will cause a
    // version mismatch error in caja.js.
    addVersionRewrite("/es53-guest-frame-" + bv + ".opt.js", "0000");
    runTestDriver("es53-test-cajajs-version-skew-js-files.js");
  }

  public final void testVersionSkewCajolerResponse() {
    closeWebDriver();  // Need a browser with an empty cache
    // Changing the version baked into *all* the JS will cause an incorrect
    // version number to be sent to the cajoler, which should then refuse
    // to compile the given content and return an error instead.
    addVersionRewrite("/caja.js", "0000");
    cajaStatic.link(
        "/es53-guest-frame-" + bv + ".opt.js",
        "/es53-guest-frame-0000.opt.js");
    cajaStatic.link(
        "/es53-taming-frame-" + bv + ".opt.js",
        "/es53-taming-frame-0000.opt.js");
    addVersionRewrite("/es53-guest-frame-0000.opt.js", "0000");
    addVersionRewrite("/es53-taming-frame-0000.opt.js", "0000");
    runTestDriver("es53-test-cajajs-version-skew-cajoler-response.js");
  }

  public final void testVersionSkewCajoledModule() {
    closeWebDriver();  // Need a browser with an empty cache
    runTestDriver("es53-test-cajajs-version-skew-cajoled-module.js");
  }

  public final void testClientUriRewriting() {
    // TODO(kpreid): Enable for ES5 once HTML scripting works
    runTestDriver("es53-test-client-uri-rewriting.js", false);
  }

  public final void testTamingTamed() {
    runTestDriver("es53-test-taming-tamed.js", false);
  }

  public final void testTamingUntamed() {
    runTestDriver("es53-test-taming-untamed.js", false);
  }

  public final void testTamingInout() {
    runTestDriver("es53-test-taming-inout.js", false);
  }

  public final void testTamingErrors() {
    runTestDriver("es53-test-taming-errors.js", false);
  }

  public final void testDomadoCanvas() {
    // TODO(kpreid): Enable for ES5 once HTML scripting works
    runTestCase("es53-test-domado-canvas-guest.html", false);
  }

  public final void testDomadoDom() {
    // TODO(kpreid): Enable for ES5 once HTML scripting works
    runTestCase("es53-test-domado-dom-guest.html", false);
  }

  public final void testDomadoEvents() {
    // TODO(kpreid): Enable for ES5 once HTML scripting works
    runTestDriver("es53-test-domado-events.js", false);
  }

  public final void testDomadoForms() {
    // TODO(kpreid): Enable for ES5 once HTML scripting works
    runTestCase("es53-test-domado-forms-guest.html", false);
  }

  public final void testDomadoSpecial() {
    // TODO(kpreid): Enable for ES5 once HTML scripting works
    runTestDriver("es53-test-domado-special.js", false);
  }

  public final void testDomadoOpaque() {
    // TODO(kpreid): Enable for ES5 once HTML scripting works
    runTestDriver("es53-test-domado-foreign.js", false);
  }

  public final void testLanguage() {
    // TODO(kpreid): Enable for ES5 once HTML scripting works
    runTestCase("es53-test-language-guest.html", false);
  }

  public final void testProxies() {
    runTestDriver("es53-test-proxies.js");
  }

  public final void testInlineScript() {
    runTestCase("es53-test-inline-script.html");
  }
}
