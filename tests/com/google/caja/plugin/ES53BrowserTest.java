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

/**
 * @author ihab.awad@gmail.com
 */
public class ES53BrowserTest extends BrowserTestCase {
  public final void testBasicFunctions() {
    runTestCase("es53-test-basic-functions-cajoled.html");
  }

  public final void testTamingTamed() {
    runTestDriver("es53-test-taming-tamed.js");
  }

  public final void testTamingUntamed() {
    runTestDriver("es53-test-taming-untamed.js");
  }

  public final void testTamingInout() {
    runTestDriver("es53-test-taming-inout.js");
  }

  public final void testTamingErrors() {
    runTestDriver("es53-test-taming-errors.js");
  }
}
