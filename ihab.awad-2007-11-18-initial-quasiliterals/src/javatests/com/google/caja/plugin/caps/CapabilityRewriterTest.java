// Copyright (C) 2006 Google Inc.
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

package com.google.caja.plugin.caps;

import com.google.caja.util.RhinoTestBed;
import java.io.StringReader;
import junit.framework.TestCase;

/**
 * runs the junit test-cases for wrap_capability.js in Rhino.
 * That file needs to work on FF, IE, Safari, and Opera so just running this is
 * not sufficient.
 *
 * @author mikesamuel@gmail.com
 */
public class CapabilityRewriterTest extends TestCase {

  public void testRewriteCapability() throws Exception {
    String testRunner = "for (var k in this) if (/^test/.test(k)) this[k]();";
    RhinoTestBed.runJs(
        new RhinoTestBed.Input(getClass(), "../asserts.js"),
        new RhinoTestBed.Input(getClass(), "wrap_capability.js"),
        new RhinoTestBed.Input(getClass(), "wrap_capability_test.js"),
        new RhinoTestBed.Input(new StringReader(testRunner), "testrunner"));
  }
}
