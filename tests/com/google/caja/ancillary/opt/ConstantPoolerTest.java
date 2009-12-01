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

package com.google.caja.ancillary.opt;

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.js.Block;
import com.google.caja.util.CajaTestCase;

public class ConstantPoolerTest extends CajaTestCase {
  public final void testGlobalUsesNotChanged() throws ParseException {
    assertOptimized(
        ""
        + "alert('Hello World!');\n"
        + "alert('Hello World!');\n"
        + "alert('Hello World!');",
        ""
        + "alert('Hello World!');\n"
        + "alert('Hello World!');\n"
        + "alert('Hello World!');");
  }

  public final void testPoolingInFunction() throws ParseException {
    assertOptimized(
        ""
        + "(function () {\n"
        + "var $_$__litpool__0$_$ = 'Hello World!';\n"
        + "alert($_$__litpool__0$_$);\n"
        + "alert($_$__litpool__0$_$);\n"
        + "alert($_$__litpool__0$_$);\n"
        + "alert($_$__litpool__0$_$);\n"
        + "alert($_$__litpool__0$_$);\n"
        + "alert($_$__litpool__0$_$);\n"
        + "})()",
        ""
        + "(function () {\n"
        + "alert('Hello World!');\n"
        + "alert('Hello World!');\n"
        + "alert('Hello World!');\n"
        + "alert('Hello World!');\n"
        + "alert('Hello World!');\n"
        + "alert('Hello World!');\n"
        + "})()"
        );
  }

  public final void testShorStringsNotPooled() throws ParseException {
    assertOptimized(
        ""
        + "(function () {\n"
        + "alert('');\n"
        + "alert('');\n"
        + "alert('');\n"
        + "})()",
        ""
        + "(function () {\n"
        + "alert('');\n"
        + "alert('');\n"
        + "alert('');\n"
        + "})()"
        );
  }

  public final void testVarThere() throws ParseException {
    assertOptimized(
        ""
        + "(function () {\n"
        + "var $_$__litpool__0$_$ = 'Hello World!', x = 1;\n"
        + "alert($_$__litpool__0$_$);\n"
        + "alert($_$__litpool__0$_$);\n"
        + "alert($_$__litpool__0$_$);\n"
        + "alert($_$__litpool__0$_$);\n"
        + "alert($_$__litpool__0$_$);\n"
        + "alert($_$__litpool__0$_$);\n"
        + "})()",
        ""
        + "(function () {\n"
        + "var x = 1;\n"
        + "alert('Hello World!');\n"
        + "alert('Hello World!');\n"
        + "alert('Hello World!');\n"
        + "alert('Hello World!');\n"
        + "alert('Hello World!');\n"
        + "alert('Hello World!');\n"
        + "})()"
        );
  }

  public final void testVarsThere() throws ParseException {
    assertOptimized(
        ""
        + "(function () {\n"
        + "var $_$__litpool__0$_$ = 'Hello World!', x = 1, y = 2;\n"
        + "alert($_$__litpool__0$_$);\n"
        + "alert($_$__litpool__0$_$);\n"
        + "alert($_$__litpool__0$_$);\n"
        + "alert($_$__litpool__0$_$);\n"
        + "alert($_$__litpool__0$_$);\n"
        + "alert($_$__litpool__0$_$);\n"
        + "})()",
        ""
        + "(function () {\n"
        + "var x = 1, y = 2;\n"
        + "alert('Hello World!');\n"
        + "alert('Hello World!');\n"
        + "alert('Hello World!');\n"
        + "alert('Hello World!');\n"
        + "alert('Hello World!');\n"
        + "alert('Hello World!');\n"
        + "})()"
        );
  }

  public final void testMultipleConstants() throws ParseException {
    assertOptimized(
        ""
        + "(function () {\n"
        + "var $_$__litpool__0$_$ = 'Hello World!',\n"
        + "    $_$__litpool__1$_$ = 123456789, x = 1, y = 2;\n"
        + "alert($_$__litpool__0$_$ != $_$__litpool__1$_$);\n"
        + "alert($_$__litpool__0$_$ != $_$__litpool__1$_$);\n"
        + "alert($_$__litpool__0$_$ != $_$__litpool__1$_$);\n"
        + "alert($_$__litpool__0$_$ != $_$__litpool__1$_$);\n"
        + "alert($_$__litpool__0$_$ != $_$__litpool__1$_$);\n"
        + "alert($_$__litpool__0$_$ != $_$__litpool__1$_$);\n"
        + "alert($_$__litpool__0$_$ != $_$__litpool__1$_$);\n"
        + "})()",
        ""
        + "(function () {\n"
        + "var x = 1, y = 2;\n"
        + "alert('Hello World!' != 123456789);\n"
        + "alert('Hello World!' != 123456789);\n"
        + "alert('Hello World!' != 123456789);\n"
        + "alert('Hello World!' != 123456789);\n"
        + "alert('Hello World!' != 123456789);\n"
        + "alert('Hello World!' != 123456789);\n"
        + "alert('Hello World!' != 123456789);\n"
        + "})()"
        );
  }

  public final void testObjectConstructors() throws ParseException {
    assertOptimized(
        ""
        + "(function () {\n"
        + "  var $_$__litpool__0$_$ = 'Kinda loooooooooong';\n"
        + "  return { 'Loooooooooooooooooong': $_$__litpool__0$_$,\n"
        + "           'Loooooooooooooooooong': $_$__litpool__0$_$,\n"
        + "           'Loooooooooooooooooong': $_$__litpool__0$_$,\n"
        + "           'Loooooooooooooooooong': $_$__litpool__0$_$,\n"
        + "           'Loooooooooooooooooong': $_$__litpool__0$_$ };\n"
        + "})()",
        ""
        + "(function () {\n"
        + "  return { 'Loooooooooooooooooong': 'Kinda loooooooooong',\n"
        + "           'Loooooooooooooooooong': 'Kinda loooooooooong',\n"
        + "           'Loooooooooooooooooong': 'Kinda loooooooooong',\n"
        + "           'Loooooooooooooooooong': 'Kinda loooooooooong',\n"
        + "           'Loooooooooooooooooong': 'Kinda loooooooooong' };\n"
        + "})()");

  }

  private void assertOptimized(String golden, String input)
      throws ParseException {
    Block prog = js(fromString(input));
    assertEquals(render(js(fromString(golden))),
                 render(ConstantPooler.optimize(prog)));
  }
}
