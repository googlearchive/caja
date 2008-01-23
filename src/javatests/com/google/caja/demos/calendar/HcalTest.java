// Copyright (C) 2008 Google Inc.
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

package com.google.caja.demos.calendar;

import com.google.caja.util.RhinoTestBed;
import com.google.caja.util.TestUtil;
import junit.framework.TestCase;

/**
 * @author mikesamuel@gmail.com
 */
public class HcalTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    TestUtil.enableContentUrls();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testExtractHcal() throws Exception {
    RhinoTestBed.runJs(
        null,
        // Emulate the browser
        new RhinoTestBed.Input(getClass(), "/js/jqueryjs/runtest/env.js"),
        // Emulate Firebug
        new RhinoTestBed.Input(getClass(), "../../plugin/console-stubs.js"),
        // Testbed
        new RhinoTestBed.Input(getClass(), "../../plugin/asserts.js"),
        new RhinoTestBed.Input(getClass(), "../../plugin/jsunit.js"),
        // Hcalendar source
        new RhinoTestBed.Input(getClass(), "util.js"),
        new RhinoTestBed.Input(getClass(), "html.js"),
        new RhinoTestBed.Input(getClass(), "uformat.js"),
        new RhinoTestBed.Input(getClass(), "hcalendar.js"),
        // Load the tests
        new RhinoTestBed.Input(getClass(), "hcalendar_test.js"),
        // Run the unittests from hcalendar_test.js
        new RhinoTestBed.Input(
            "function loadHtml(html, continuation) {"
            + "  window.location = 'content:' + encodeURIComponent(html);"
            + "  continuation(window.document);"
            + "}"
            + "jsunitRun()", "runTest")
        );
  }
}
