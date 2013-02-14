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
public abstract class UniversalBrowserTests extends BrowserTestCase<Void> {
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

  public void testBasicFunctions() throws Exception {
    runTestCase("es53-test-basic-functions-guest.html", es5Mode);
  }

  public void testVersionSkewJsFiles() throws Exception {
    // Changing the version baked into the taming frame JS will cause a
    // version mismatch error in caja.js.
    runTestDriver("es53-test-cajajs-version-skew-js-files.js", es5Mode);
    // TODO(felix8a): test taming.js right but guest.js wrong
  }

  public void testVersionSkewCajaJs() throws Exception {
    // Changing the version baked into caja.js will cause it to load the
    // wrongly-named files for the host and guest frames, which should cause
    // it to never make progress in load() or whenReady() calls.
    runTestDriver("es53-test-cajajs-never-starts.js", es5Mode,
        "cajajs=/caja/testing/skew-0000/caja.js");
  }

  public void testVersionSkewNonexistentResources() throws Exception {
    // Placing the wrong resources files where caja.js is expecting them should
    // cause it to never make progress in load() or whenReady() calls.
    runTestDriver("es53-test-cajajs-never-starts.js", es5Mode,
        "resources=/caja/testing/nonexistent");
  }

  public void testVersionSkewWrongResources() throws Exception {
    // Placing the wrong resources files where caja.js is expecting them should
    // cause it to never make progress in load() or whenReady() calls.
    runTestDriver("es53-test-cajajs-never-starts.js", es5Mode,
        "resources=/caja/testing/skew-0000");
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
