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

package com.google.caja.ancillary.jsdoc;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ParseException;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.MoreAsserts;
import com.google.caja.util.TestUtil;

import java.util.Arrays;

public class UpdocParserTest extends CajaTestCase {
  public final void testParse1() throws Exception {
    assertParsed(
        ""
        + "$ Math.abs(-5)\n"
        + "# 5",

        "Updoc",
        "  Run",
        "    SpecialOperation : FUNCTION_CALL",
        "      SpecialOperation : MEMBER_ACCESS",
        "        Reference",
        "          Identifier : Math",
        "        Reference",
        "          Identifier : abs",
        "      SimpleOperation : NEGATION",
        "        IntegerLiteral : 5",
        "    IntegerLiteral : 5");
  }

  public final void testParse2() throws Exception {
    assertParsed(
        ""
        + "$ Math.abs(0)\n"
        + "# 0\n"
        + "$ abs(\n"
        + "      0 / 0)\n"
        + "# NaN",

        "Updoc",
        "  Run",
        "    SpecialOperation : FUNCTION_CALL",
        "      SpecialOperation : MEMBER_ACCESS",
        "        Reference",
        "          Identifier : Math",
        "        Reference",
        "          Identifier : abs",
        "      IntegerLiteral : 0",
        "    IntegerLiteral : 0",
        "  Run",
        "    SpecialOperation : FUNCTION_CALL",
        "      Reference",
        "        Identifier : abs",
        "      SimpleOperation : DIVISION",
        "        IntegerLiteral : 0",
        "        IntegerLiteral : 0",
        "    Reference",
        "      Identifier : NaN");
  }

  public final void testRender() throws Exception {
    assertEquals(
        ""
        + "$ Math.abs(0);\n"
        + "# 0;\n"
        + "$ abs(0 / 0);\n"
        + "# NaN;",
        render(updoc(fromString(
            ""
            + "$ Math.abs( 0 )\n"
            + "# 0\n"
            + "// comments can appear in updoc too\n"
            + "$ abs(0/0)\n"
            + "# NaN"))));
  }

  private void assertParsed(String input, String... golden)
      throws ParseException {
    MoreAsserts.assertListsEqual(
        Arrays.asList(golden),
        Arrays.asList(
            TestUtil.format(
                updoc(fromString(input))).split("\r\n?|\n")));
  }

  private Updoc updoc(CharProducer cp) throws ParseException {
    UpdocParser p = new UpdocParser(mq);
    return p.parseComplete(cp);
  }
}
