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

/**
 * @author ihab.awad@gmail.com
 */
public class GeneralBrowserTest extends BrowserTestCase {
  private final String bv = BuildInfo.getInstance().getBuildVersion();

  public final void testPrecajole() throws Exception {
    // Only relevant to es53.
    runTestCase("es53-test-precajole-guest.html", false);
  }

  public final void testCajaJsInvocations() throws Exception {
    String result =
        runTestDriver(
            "es53-test-cajajs-invocation.js",
            "minified=false");
    assertContains(result, "{closured=false}");
    assertNotContains(result, "{closured=true}");
  }

  public final void testCajaJsMinifiedInvocations() throws Exception {
    String result =
        runTestDriver(
            "es53-test-cajajs-invocation.js",
            "minified=true");
    assertContains(result, "{closured=true}");
    assertNotContains(result, "{closured=false}");
  }

  public final void testUnicode() throws Exception {
    runTestDriver("es53-test-unicode.js");
  }

  public final void testCajaJsBare() throws Exception {
    runBrowserTest("cajajs-bare-test.html", "es5=false");
    runBrowserTest("cajajs-bare-test.html", "es5=true");
  }

  public final void testBasicFunctions() throws Exception {
    runTestCase("es53-test-basic-functions-guest.html");
  }

  // Several tests below will start a test server with modified versions
  // of js files.  This has potential to interact badly with the browser
  // cache since we're re-using the same browser.  But we're ok because:
  //   1. The test server always says Pragma: no-cache, which either
  //      prevents the resource from entering the cache, or forces the
  //      browser to revalidate with If-Modified-Since.
  //   2. When the test server serves a modified resource, we declare it
  //      has modification time = 0, which makes jetty's ResourceHandler
  //      ignore If-Modified-Since from the browser.  Subsequent browser
  //      requests for the same resource will have If-Modified-Since: 0,
  //      which is always superseded by the current state of the resource.

  private final void addVersionRewrite(String path, String newVersion)
      throws Exception {
    getCajaStatic().rewrite(path, bv, newVersion);
  }

  public final void testVersionSkewCajaJs() throws Exception {
    // Changing the version baked into caja.js will cause it to load the
    // wrongly-named files for the host and guest frames, which should cause
    // it to never make progress in load() or whenReady() calls.
    addVersionRewrite("/caja.js", "0000");
    addVersionRewrite("/caja-minified.js", "0000");
    runTestDriver("es53-test-cajajs-never-starts.js");
  }

  public final void testWrongSupportingResources() throws Exception {
    // Placing the wrong resources files where caja.js is expecting them should
    // cause it to never make progress in load() or whenReady() calls.
    getCajaStatic().link(
        "/" + bv + "/non-existent.js",
        "/" + bv + "/es53-guest-frame.opt.js");
    getCajaStatic().link(
        "/" + bv + "/non-existent.js",
        "/" + bv + "/es53-taming-frame.opt.js");
    runTestDriver("es53-test-cajajs-never-starts.js");
  }

  public final void testAlternateLocationSupportingResources()
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
    runTestDriver("es53-test-cajajs-never-starts.js");
  }

  public final void testVersionSkewTamingFrame() throws Exception {
    // Changing the version baked into the taming frame JS will cause a
    // version mismatch error in caja.js.
    addVersionRewrite("/" + bv + "/es53-taming-frame.opt.js", "0000");
    runTestDriver("es53-test-cajajs-version-skew-js-files.js");
  }

  public final void testVersionSkewGuestFrame() throws Exception {
    // Changing the version baked into the guest frame JS will cause a
    // version mismatch error in caja.js.
    addVersionRewrite("/" + bv + "/es53-guest-frame.opt.js", "0000");
    runTestDriver("es53-test-cajajs-version-skew-js-files.js", false);
    // TODO(kpreid): This test fails (does not detect skew) under ES5 mode,
    // since es53-guest-frame is not used. We should add version skew testing
    // for ES5 mode as well.
  }

  public final void testVersionSkewCajolerResponse() throws Exception {
    // Changing the version baked into *all* the JS will cause an incorrect
    // version number to be sent to the cajoler, which should then refuse
    // to compile the given content and return an error instead.
    addVersionRewrite("/caja.js", "0000");
    addVersionRewrite("/caja-minified.js", "0000");
    getCajaStatic().link(
        "/" + bv + "/es53-guest-frame.opt.js",
        "/0000/es53-guest-frame.opt.js");
    getCajaStatic().link(
        "/" + bv + "/es53-taming-frame.opt.js",
        "/0000/es53-taming-frame.opt.js");
    addVersionRewrite("/0000/es53-guest-frame.opt.js", "0000");
    addVersionRewrite("/0000/es53-taming-frame.opt.js", "0000");
    runTestDriver("es53-test-cajajs-version-skew-cajoler-response.js");
  }

  public final void testVersionMinorSkewCajolerResponse() throws Exception {
    // Changing the version baked into *all* the JS will cause a different
    // minor version number to be sent to the cajoler, which should emit a
    // LINT warning when compiling the given content
    String minorVariant = bv + "M3";
    addVersionRewrite("/caja.js", minorVariant);
    addVersionRewrite("/caja-minified.js", minorVariant);
    getCajaStatic().link(
        "/" + bv + "/es53-guest-frame.opt.js",
        "/" + minorVariant + "/es53-guest-frame.opt.js");
    getCajaStatic().link(
        "/" + bv + "/es53-taming-frame.opt.js",
        "/" + minorVariant + "/es53-taming-frame.opt.js");
    addVersionRewrite(
        "/" + minorVariant + "/es53-guest-frame.opt.js", minorVariant);
    addVersionRewrite(
        "/" + minorVariant + "/es53-taming-frame.opt.js", minorVariant);
    runTestDriver("es53-test-cajajs-minor-version-skew-cajoler-response.js");
  }

  public final void testVersionSkewCajoledModule() throws Exception {
    // only relevant to es53
    runTestDriver("es53-test-cajajs-version-skew-cajoled-module.js", false);
  }

  public final void testMinorVersionSkewCajoledModule() throws Exception {
    // only relevant to es53
    runTestDriver(
      "es53-test-cajajs-minor-version-skew-cajoled-module.js", false);
  }

  public final void testClientUriRewriting() throws Exception {
    runTestDriver("es53-test-client-uri-rewriting.js");
  }

  public final void testTamingTamed() throws Exception {
    runTestDriver("es53-test-taming-tamed.js");
  }

  public final void testTamingUntamed() throws Exception {
    runTestDriver("es53-test-taming-untamed.js");
  }

  public final void testTamingInout() throws Exception {
    runTestDriver("es53-test-taming-inout.js");
  }

  public final void testTamingErrors() throws Exception {
    runTestDriver("es53-test-taming-errors.js");
  }

  public final void testTamingAdvice() throws Exception {
    runTestDriver("es53-test-taming-advice.js");
  }

  public final void testDomadoCanvas() throws Exception {
    runTestCase("es53-test-domado-canvas-guest.html");
  }

  public final void testDomadoDom() throws Exception {
    // TODO(kpreid): Reenable the disabled tests in that file for ES5 mode once
    // the corresponding functionality has been implemented.
    runTestCase("es53-test-domado-dom-guest.html");
  }

  public final void testDomadoGlobal() throws Exception {
    runTestDriver("es53-test-domado-global.js");
  }

  public final void testDomadoEvents() throws Exception {
    runTestDriver("es53-test-domado-events.js");
  }

  public final void testDomadoForms() throws Exception {
    runTestCase("es53-test-domado-forms-guest.html");
  }

  public final void testDomadoSpecial() throws Exception {
    runTestDriver("es53-test-domado-special.js");
  }

  public final void testDomadoOpaque() throws Exception {
    runTestDriver("es53-test-domado-foreign.js");
  }

  public final void testLanguage() throws Exception {
    runTestCase("es53-test-language-guest.html");
  }

  public final void testProxies() throws Exception {
    runTestDriver("es53-test-proxies.js", false);
  }

  public final void testInlineScript() throws Exception {
    runTestCase("es53-test-inline-script.html");
  }

  public final void testExternalScript() throws Exception {
    runTestCase("es53-test-external-script-guest.html", true);
  }

  public final void testRelativeUrls() throws Exception {
    runTestDriver("es53-test-relative-urls.js");
  }

  public final void testDefensibleObjects() throws Exception {
    runTestDriver("es53-test-defensible-objects.js");
  }

  public final void testContainerOverflow() throws Exception {
    runTestDriver("es53-test-container-overflow.js");
  }

  public final void testTargetAttributePresets() throws Exception {
    runTestDriver("es53-test-target-attribute-presets.js");
  }

  public final void testApiTaming() throws Exception {
    runTestDriver("es53-test-apitaming.js");
  }

  public final void testAutoMode1() throws Exception {
    runTestDriver("es53-test-automode1.js", false);
  }

  public final void testAutoMode2() throws Exception {
    runTestDriver("es53-test-automode2.js", false);
  }

  public final void testAutoMode3() throws Exception {
    runTestDriver("es53-test-automode3.js", false);
  }

  public final void testAutoMode4() throws Exception {
    runTestDriver("es53-test-automode4.js", false);
  }
}
