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
 * This class has subclasses Es5BrowserTest and Es53BrowserTest.
 * The methods here are tests that should be run in both es5
 * and es53 mode.
 *
 * @author ihab.awad@gmail.com
 */
public abstract class UniversalBrowserTests extends BrowserTestCase {
  protected final String bv = BuildInfo.getInstance().getBuildVersion();

  protected final boolean es5Mode;

  protected UniversalBrowserTests(boolean es5Mode) {
    this.es5Mode = es5Mode;
  }

  public void testCajaJsInvocations() throws Exception {
    String result =
        runTestDriver(
            "es53-test-cajajs-invocation.js",
            es5Mode,
            "minified=false");
    assertContains(result, "{closured=false}");
    assertNotContains(result, "{closured=true}");
  }

  public void testCajaJsMinifiedInvocations() throws Exception {
    String result =
        runTestDriver(
            "es53-test-cajajs-invocation.js",
            es5Mode,
            "minified=true");
    assertContains(result, "{closured=true}");
    assertNotContains(result, "{closured=false}");
  }

  public void testUnicode() throws Exception {
    runTestDriver("es53-test-unicode.js", es5Mode);
  }

  public void testBasicFunctions() throws Exception {
    runTestCase("es53-test-basic-functions-guest.html", es5Mode);
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

  protected void addVersionRewrite(String path, String newVersion)
      throws Exception {
    getCajaStatic().rewrite(path, bv, newVersion);
  }

  public void testVersionSkewTamingFrame() throws Exception {
    // Changing the version baked into the taming frame JS will cause a
    // version mismatch error in caja.js.
    addVersionRewrite("/" + bv + "/es53-taming-frame.opt.js", "0000");
    runTestDriver("es53-test-cajajs-version-skew-js-files.js", es5Mode);
    // TODO(felix8a): es5Mode is not used by this test?
  }

  public void testClientUriRewriting() throws Exception {
    runTestDriver("es53-test-client-uri-rewriting.js", es5Mode);
  }

  public void testTamingTamed() throws Exception {
    runTestDriver("es53-test-taming-tamed.js", es5Mode);
  }

  public void testTamingUntamed() throws Exception {
    runTestDriver("es53-test-taming-untamed.js", es5Mode);
  }

  public void testTamingInout() throws Exception {
    runTestDriver("es53-test-taming-inout.js", es5Mode);
  }

  public void testTamingErrors() throws Exception {
    runTestDriver("es53-test-taming-errors.js", es5Mode);
  }

  public void testTamingAdvice() throws Exception {
    runTestDriver("es53-test-taming-advice.js", es5Mode);
  }

  public void testDomadoCanvas() throws Exception {
    runTestCase("es53-test-domado-canvas-guest.html", es5Mode);
  }

  public void testDomadoDom() throws Exception {
    // TODO(kpreid): Reenable the disabled tests in that file for ES5 mode once
    // the corresponding functionality has been implemented.
    runTestCase("es53-test-domado-dom-guest.html", es5Mode);
  }

  public void testDomadoGlobal() throws Exception {
    runTestDriver("es53-test-domado-global.js", es5Mode);
  }

  public void testDomadoEvents() throws Exception {
    runTestDriver("es53-test-domado-events.js", es5Mode);
  }

  public void testDomadoForms() throws Exception {
    runTestCase("es53-test-domado-forms-guest.html", es5Mode);
  }

  public void testDomadoSpecial() throws Exception {
    runTestDriver("es53-test-domado-special.js", es5Mode);
  }

  public void testDomadoOpaque() throws Exception {
    runTestDriver("es53-test-domado-foreign.js", es5Mode);
  }

  public void testLanguage() throws Exception {
    runTestCase("es53-test-language-guest.html", es5Mode);
  }

  public void testInlineScript() throws Exception {
    runTestCase("es53-test-inline-script.html", es5Mode);
  }

  public void testRelativeUrls() throws Exception {
    runTestDriver("es53-test-relative-urls.js", es5Mode);
  }

  public void testDefensibleObjects() throws Exception {
    runTestDriver("es53-test-defensible-objects.js", es5Mode);
  }

  public void testContainerOverflow() throws Exception {
    runTestDriver("es53-test-container-overflow.js", es5Mode);
  }

  public void testTargetAttributePresets() throws Exception {
    runTestDriver("es53-test-target-attribute-presets.js", es5Mode);
  }

  public void testApiTaming() throws Exception {
    runTestDriver("es53-test-apitaming.js", es5Mode);
  }
}
