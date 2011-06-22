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

package com.google.caja.parser.quasiliteral;

import com.google.caja.util.CajaTestCase;
import com.google.caja.util.FailureIsAnOption;
import com.google.caja.util.RhinoTestBed;

/** See taming_test.html */
public class TamingTest extends CajaTestCase {
  @FailureIsAnOption("TamingTest not compatible with ES53")
  public final void testInRhino() throws Exception {
    RhinoTestBed.runJsUnittestFromHtml(html(fromResource("taming_test.html")));
  }
}
