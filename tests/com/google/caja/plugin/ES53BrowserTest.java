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

// TODO(kpreid): Rename this file, as it now tests the ES5 as well as ES53
// environment.

/**
 * @author ihab.awad@gmail.com
 */
public class ES53BrowserTest extends BrowserTestCase {
  public final void testCajaJsInvocations() {
    runTestDriver("es53-test-cajajs-invocation.js");
  }

  public final void testBasicFunctions() {
    // TODO(kpreid): Enable for ES5 once HTML scripting works
    runTestCase("es53-test-basic-functions-guest.html", false);
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

  public final void testDomitaCanvas() {
    // TODO(kpreid): Enable for ES5 once HTML scripting works
    runTestCase("es53-test-domita-canvas-guest.html", false);
  }

  public final void testDomitaDom() {
    // TODO(kpreid): Enable for ES5 once HTML scripting works
    runTestCase("es53-test-domita-dom-guest.html", false);
  }

  public final void testDomitaEvents() {
    // TODO(kpreid): Enable for ES5 once HTML scripting works
    runTestDriver("es53-test-domita-events.js", false);
  }

  public final void testDomitaForms() {
    // TODO(kpreid): Enable for ES5 once HTML scripting works
    runTestCase("es53-test-domita-forms-guest.html", false);
  }

  public final void testDomitaSpecial() {
    // TODO(kpreid): Enable for ES5 once HTML scripting works
    runTestDriver("es53-test-domita-special.js", false);
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
