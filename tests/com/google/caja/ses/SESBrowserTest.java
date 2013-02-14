// Copyright (C) 2013 Google Inc.
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

package com.google.caja.ses;

import com.google.caja.plugin.BrowserTestCase;

/**
 * Tests for standalone SES.
 *
 * @author kpreid@switchb.org
 */
public class SESBrowserTest extends BrowserTestCase<Void> {
  public final void testBasic() throws Exception {
    runBrowserTest("../ses/test-ses.html?load=initSES.js");
  }

  public final void testBasicMin() throws Exception {
    runBrowserTest("../ses/test-ses.html?load=initSES-minified.js");
  }

  public final void testPlus() throws Exception {
    runBrowserTest("../ses/test-ses.html?load=initSESPlus.js");
  }

  public final void testPlusMin() throws Exception {
    runBrowserTest("../ses/test-ses.html?load=initSESPlus-minified.js");
  }
}
