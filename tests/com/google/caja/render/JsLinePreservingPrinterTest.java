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

package com.google.caja.render;

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;

/**
 * @author mikesamuel@gmail.com
 */
public class JsLinePreservingPrinterTest extends CajaTestCase {
  public void testRendering() throws Exception {
    assertEquals(
        ""
        + "\n"
        + "{ try {\n"
        + "var foo = bar (\n"
        + "a ,\n"
        + "b ,\n"
        + "\n"
        + "c , d ) ;\n"
        + "return foo ;\n"
        + "} catch ( ex ) {\n"
        + "panic ( ) ;\n"
        + "return false ;\n"
        + "} }",

        renderLP(js(fromString(
            ""
            + "// Blah Blah\n"
            + "try {\n"
            + "  var foo = bar(\n"
            + "     a,\n"
            + "     b,\n"
            + "     // blah blah\n"
            + "     c, d);\n"
            + "  return foo\n"
            + "} catch (ex) {\n"
            + "  panic();\n"
            + "  return false;\n"
            + "}"))));
  }

  private String renderLP(ParseTreeNode node) {
    StringBuilder out = new StringBuilder();
    RenderContext rc = new RenderContext(
        new MessageContext(), new JsLinePreservingPrinter(is, out, null));
    node.render(rc);
    rc.getOut().noMoreTokens();
    return out.toString();
  }
}
