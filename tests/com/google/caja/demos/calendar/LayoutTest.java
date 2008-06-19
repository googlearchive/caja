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

import junit.framework.TestCase;

import com.google.caja.util.RhinoTestBed;

/**
 * @author msamuel@google.com (Mike Samuel)
 */
public class LayoutTest extends TestCase {
  public void testBitSet() throws Exception {
    RhinoTestBed.runJsUnittestFromHtml(getClass(), "bitset_test.html");
  }

  public void testLayout() throws Exception {
    RhinoTestBed.runJsUnittestFromHtml(getClass(), "layout_test.html");
  }
}
