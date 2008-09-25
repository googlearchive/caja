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

package com.google.caja.opensocial.applet;

import com.google.caja.parser.js.Statement;
import com.google.caja.parser.quasiliteral.CajitaRewriter;
import com.google.caja.util.CajaTestCase;

public class ExpressionLanguageStageTest extends CajaTestCase {
  public void testEmptyBlock() throws Exception {
    assertRewritten("{}", "{}");
  }

  public void testExpressionStmt() throws Exception {
    assertRewritten("{ return 2 + 2; }", "{ 2 + 2; }");
  }

  public void testMultipleStatementsInBlock() throws Exception {
    assertRewritten("{ 3 * 3; return 2 + 2; }", "{ 3 * 3; 2 + 2; }");
  }

  public void testReturnUnchanged() throws Exception {
    assertRewritten("{ return foo; }", "{ return foo; }");
  }

  public void testConditions() throws Exception {
    assertRewritten(
        "{ if (rnd()) { return 1; } else return 2; }",
        "{ if (rnd()) { 1; } else 2; }");
    assertRewritten("{ if (rnd()) { return 1; } }",
                    "{ if (rnd()) { 1; } }");
    assertRewritten(
        "{"
        + "if (rnd()) return 1;"
        + "else if (rnd()) return 2;"
        + "}",
        "{ if (rnd()) 1; else if (rnd()) 2; }");
  }

  public void testTryBlock() throws Exception {
    assertRewritten("{ try { return foo(); } catch (e) { bar(); } }",
                    "{ try { foo(); } catch (e) { bar(); } }");
  }

  public void testLoopUnchanged() throws Exception {
    assertRewritten("{ for (;;) 4; }",
                    "{ for (;;) 4; }");
  }

  public void testMultipleBlocks() throws Exception {
    assertRewritten("{ 1; } { return 2; }",
                    "{ 1; } { 2; }");
  }

  private void assertRewritten(String golden, String input) throws Exception {
    Statement actual = CajitaRewriter.returnLast(js(fromString(input)));
//    ExpressionLanguageStage.apply(AncestorChain.instance(actual));
    assertEquals(render(js(fromString(golden))), render(actual));
  }
}
