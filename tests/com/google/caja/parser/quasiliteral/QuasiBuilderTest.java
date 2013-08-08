// Copyright (C) 2007 Google Inc.
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

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.util.CajaTestCase;

import java.net.URI;
import java.util.Arrays;

/**
 *
 * @author ihab.awad@gmail.com
 */
@SuppressWarnings("static-method")
public class QuasiBuilderTest extends CajaTestCase {
  public final void testParseDoesNotFail() throws Exception {
    QuasiNode n = QuasiBuilder.parseQuasiNode(
        new InputSource(URI.create("built-in:///js-quasi-literals")),
        "function @a() { @b.@c = @d; @e = @f; }");
    assertTrue(n instanceof SimpleQuasiNode);
  }

  public final void testMultiProps() throws Exception {
    ParseTreeNode n = QuasiBuilder.substV(
        "({ '@k*': @v*, baz: @boo })",
        "k", new ParseTreeNodeContainer(Arrays.asList(
            jsExpr(fromString("'foo'")), jsExpr(fromString("'bar'")))),
        "v", new ParseTreeNodeContainer(Arrays.asList(
            jsExpr(fromString("0")), jsExpr(fromString("1")))),
        "boo", new IntegerLiteral(FilePosition.UNKNOWN, 2));
    assertEquals(
        render(jsExpr(fromString("{ foo: 0, bar: 1, baz: 2 }"))),
        render(n));
  }

  public final void testPropKeys() throws Exception {
    ParseTreeNode n = QuasiBuilder.substV(
        "({ @a: @b, '\\@c': @d })",
        "a", StringLiteral.valueOf(FilePosition.UNKNOWN, "a"),
        "b", StringLiteral.valueOf(FilePosition.UNKNOWN, "b"),
        "c", StringLiteral.valueOf(FilePosition.UNKNOWN, "c"),
        "d", StringLiteral.valueOf(FilePosition.UNKNOWN, "d"));
    assertEquals(
        render(jsExpr(fromString("{ a: 'b', '@c': 'd' }"))), render(n));
  }

  public final void testIdsWithUnderscores() throws Exception {
    String[] underscoreIds = {"x__", "x\u005f\u005f", "__", "\u005f\u005f" };
    for (String id : underscoreIds) {
      ParseTreeNode specimen = QuasiBuilder.substV(
          "{ var @idWithUnderscore = 1; }",
          "idWithUnderscore", new Identifier(FilePosition.UNKNOWN, id)
      );
      assertTrue("Valid id failed to parse: " + id,
          QuasiBuilder.match("{ var @x__ = 1; }", specimen));
    }
  }
}
