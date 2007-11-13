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

package com.google.caja.parser.css;

import junit.framework.TestCase;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class Css2Test extends TestCase {

  public void testGetCssProperty() {
    assertNull(Css2.getCssProperty("bogus"));
    assertNotNull(Css2.getCssProperty("font-style"));

    // TODO(mikesamuel): test signature parse trees
  }

  // TODO(mikesamuel): test getSymbol

  public void testIsKeyword() {
    assertTrue(Css2.isKeyword("inherit"));
    assertTrue(Css2.isKeyword("default"));
    assertTrue(Css2.isKeyword("initial"));
    assertTrue(Css2.isKeyword("auto"));
    assertTrue(Css2.isKeyword("sans-serif"));
    assertTrue(Css2.isKeyword("monospace"));
    assertTrue(Css2.isKeyword("INHERIT"));
    assertTrue(!Css2.isKeyword("not-a-keyword"));
    assertTrue(!Css2.isKeyword("notakeyword"));
  }
}
