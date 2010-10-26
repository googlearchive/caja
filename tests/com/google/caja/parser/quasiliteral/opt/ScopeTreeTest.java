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

package com.google.caja.parser.quasiliteral.opt;

import java.util.Iterator;

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.js.Identifier;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Join;

public class ScopeTreeTest extends CajaTestCase {
  public final void testConstructor() throws Exception {
    ScopeTree t = ScopeTree.create(AncestorChain.instance(js(fromString(
        ""
        + "function foo(j) {\n"
        + "  var i = 0;\n"
        + "  return j + 1;\n"
        + "}\n"
        + "try {\n"
        + "  var bar = (function () {\n"
        + "     var i = 0;\n"
        + "     function bar() {\n"
        + "       return ++i;\n"
        + "     }\n"
        + "     return bar;\n"
        + "  })();\n"
        + "} catch (ex) {\n"
        + "  panic(ex);\n"
        + "}\n"
        ))));
    assertEquals(
        ""
        + "(ScopeTree Block\n"
        + "  (ScopeTree function foo)\n"
        + "  (ScopeTree function\n"
        + "    (ScopeTree function bar))\n"
        + "  (ScopeTree CatchStmt))",
        t.toString());
  }

  public final void testUsesOf() throws Exception {
    ScopeTree t = ScopeTree.create(AncestorChain.instance(js(fromString(
        ""
        + "for (var i = 0; i < 1000; i++) alert('annoying innit');\n"
        + "var counter = (function () {\n"
        + "  var i = -1;\n"
        + "  return function () { return ++i; };\n"
        + "})();"))));
    ScopeTree f1 = t.children().get(0);
    ScopeTree f2 = f1.children().get(0);
    Iterator<AncestorChain<Identifier>> uses;

    // No uses
    uses = f2.usesOf("j").iterator();
    assertFalse(uses.hasNext());

    // Includes uses from the declaring scope, but not masked declarations from
    // the global scope.
    uses = f2.usesOf("i").iterator();
    assertTrue(uses.hasNext());
    assertEquals(
        Join.join(
            "\n",
            "Block",
            "  Declaration",
            "    SpecialOperation : FUNCTION_CALL",
            "      FunctionConstructor",
            "        Block",
            "          Declaration",
            "            Identifier : i"),
        uses.next().toString());
    assertTrue(uses.hasNext());
    assertEquals(
        Join.join(
            "\n",
            "Block",
            "  Declaration",
            "    SpecialOperation : FUNCTION_CALL",
            "      FunctionConstructor",
            "        Block",
            "          ReturnStmt",
            "            FunctionConstructor",
            "              Block",
            "                ReturnStmt",
            "                  AssignOperation : PRE_INCREMENT",
            "                    Reference",
            "                      Identifier : i"),
        uses.next().toString());
    assertFalse(uses.hasNext());

    // Does not include declarations in sub-scopes where the declaration is
    // redefined.
    // for (var i = 0; i < 1000; i++) alert('annoying innit');
    uses = t.usesOf("i").iterator();
    assertTrue(uses.hasNext());
    assertEquals(
        Join.join(
            "\n",
            "Block",
            "  ForLoop : ",
            "    Declaration",
            "      Identifier : i"),
        uses.next().toString());
    assertTrue(uses.hasNext());
    assertEquals(
        Join.join(
            "\n",
            "Block",
            "  ForLoop : ",
            "    SimpleOperation : LESS_THAN",
            "      Reference",
            "        Identifier : i"),
        uses.next().toString());
    assertTrue(uses.hasNext());
    assertEquals(
        Join.join(
            "\n",
            "Block",
            "  ForLoop : ",
            "    ExpressionStmt",
            "      AssignOperation : POST_INCREMENT",
            "        Reference",
            "          Identifier : i"),
        uses.next().toString());
    assertFalse(uses.hasNext());
  }
}
