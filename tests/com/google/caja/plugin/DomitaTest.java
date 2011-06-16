// Copyright (C) 2009 Google Inc.
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

/**
 * Perform the domita test automatically.
 *
 * @author maoziqing@gmail.com (Ziqing Mao)
 */
public class DomitaTest extends BrowserTestCase {
  @FailureIsAnOption
  public final void testDomitaCajita() {
    runBrowserTest("domita_test.html");
  }

  @FailureIsAnOption
  public final void testDomitaValija() {
    runBrowserTest("domita_test.html?valija=1");
  }
}
