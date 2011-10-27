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

package com.google.caja.render;

import junit.framework.TestCase;

public class CssPrettyPrinterTest extends TestCase {
  public final void testRender() {
    assertTokens(
        "p {\n  color: red;\n  background: blue\n}",
        "p", "{", "color", ":", " ", "red", ";",
        "background", ":", " ", "blue", "}");
  }

  public final void testDeclarationGroup() {
    assertTokens(
        "color: red; background: blue",
        "color", ":", " ", "red", ";", "background", ":", " ", "blue");
  }

  private void assertTokens(String golden, String... input) {
    StringBuilder out = new StringBuilder();
    CssPrettyPrinter pp = new CssPrettyPrinter(out);

    for (String token : input) {
      pp.consume(token);
    }
    pp.noMoreTokens();
    assertEquals(golden, out.toString());
  }
}
