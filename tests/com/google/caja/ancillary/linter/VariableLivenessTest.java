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

package com.google.caja.ancillary.linter;

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.ReturnStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.ThrowStmt;
import com.google.caja.reporting.MessageContext;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.MoreAsserts;

import java.io.IOException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

/**
 * @author mikesamuel@gmail.com
 */
public class VariableLivenessTest extends CajaTestCase {
  public final void testSimpleDeclaration()  throws Exception {
    assertLiveness(
        js(fromString("var x = 1; return x;")),
        "Block",
        "  Declaration",
        "    Identifier : x",
        "    IntegerLiteral : 1",
        "  ReturnStmt ; liveness=(x)",
        "    Reference ; liveness=(x)",
        "      Identifier : x"
        );
  }

  public final void testSimpleAssignment()  throws Exception {
    assertLiveness(
        js(fromString("var x, y; x = 1; return x;")),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : x",
        "    Declaration",
        "      Identifier : y",
        "  ExpressionStmt",
        "    AssignOperation : ASSIGN",
        "      Reference",
        "        Identifier : x",
        "      IntegerLiteral : 1",
        "  ReturnStmt ; liveness=(x)",
        "    Reference ; liveness=(x)",
        "      Identifier : x"
        );
  }

  public final void testConditionWithElse1() throws Exception {
    assertLiveness(
        js(fromString("var x; if (r)  x = 1;  else  x = 2;  return x;")),
        "Block",
        "  Declaration",
        "    Identifier : x",
        "  Conditional",
        "    Reference",
        "      Identifier : r",
        "    ExpressionStmt",
        "      AssignOperation : ASSIGN",
        "        Reference",
        "          Identifier : x",
        "        IntegerLiteral : 1",
        "    ExpressionStmt",
        "      AssignOperation : ASSIGN",
        "        Reference",
        "          Identifier : x",
        "        IntegerLiteral : 2",
        "  ReturnStmt ; liveness=(x)",
        "    Reference ; liveness=(x)",
        "      Identifier : x"
        );
  }

  public final void testConditionWithElse2() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var x = 1, y, z, w;"
            + "if (r && (w = 1))"
            // w is live here
            + "  z = 1, y = 3;"
            + "else"
            // w is not live here
            + "  z = 2;"
            // x is live here since it is assigned early.
            // z is live here since it is assigned in all branches
            // neither w nor y are assigned in all branches so are not live.
            + "return;"
            )),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : x",
        "      IntegerLiteral : 1",
        "    Declaration ; liveness=(x)",
        "      Identifier : y",
        "    Declaration ; liveness=(x)",
        "      Identifier : z",
        "    Declaration ; liveness=(x)",
        "      Identifier : w",
        "  Conditional ; liveness=(x)",
        "    ControlOperation : LOGICAL_AND ; liveness=(x)",
        "      Reference ; liveness=(x)",
        "        Identifier : r",
        "      AssignOperation : ASSIGN ; liveness=(x)",
        "        Reference ; liveness=(x)",
        "          Identifier : w",
        "        IntegerLiteral : 1 ; liveness=(x)",
        "    ExpressionStmt ; liveness=(x w)",
        "      SpecialOperation : COMMA ; liveness=(x w)",
        "        AssignOperation : ASSIGN ; liveness=(x w)",
        "          Reference ; liveness=(x w)",
        "            Identifier : z",
        "          IntegerLiteral : 1 ; liveness=(x w)",
        "        AssignOperation : ASSIGN ; liveness=(x w z)",
        "          Reference ; liveness=(x w z)",
        "            Identifier : y",
        "          IntegerLiteral : 3 ; liveness=(x w z)",
        "    ExpressionStmt ; liveness=(x)",
        "      AssignOperation : ASSIGN ; liveness=(x)",
        "        Reference ; liveness=(x)",
        "          Identifier : z",
        "        IntegerLiteral : 2 ; liveness=(x)",
        "  ReturnStmt ; liveness=(x z)"  // z is assigned in both branches
        );
  }

  public final void testConditionWithoutElse() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var x = 1, y, z;"
            + "if (r)"
            + "  z = 1, y = 3;"
            + "return;"
            )),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : x",
        "      IntegerLiteral : 1",
        "    Declaration ; liveness=(x)",
        "      Identifier : y",
        "    Declaration ; liveness=(x)",
        "      Identifier : z",
        "  Conditional ; liveness=(x)",
        "    Reference ; liveness=(x)",
        "      Identifier : r",
        "    ExpressionStmt ; liveness=(x)",
        "      SpecialOperation : COMMA ; liveness=(x)",
        "        AssignOperation : ASSIGN ; liveness=(x)",
        "          Reference ; liveness=(x)",
        "            Identifier : z",
        "          IntegerLiteral : 1 ; liveness=(x)",
        "        AssignOperation : ASSIGN ; liveness=(x z)",
        "          Reference ; liveness=(x z)",
        "            Identifier : y",
        "          IntegerLiteral : 3 ; liveness=(x z)",
        "  ReturnStmt ; liveness=(x)"  // z is not assigned in both branches
        );
  }

  public final void testConditionWithReturn() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var x = 1, y, z;"
            + "if (r)"
            + "  z = 1, y = 3;"
            + "else"
            + "  return;"
            + "return z;"
            )),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : x",
        "      IntegerLiteral : 1",
        "    Declaration ; liveness=(x)",
        "      Identifier : y",
        "    Declaration ; liveness=(x)",
        "      Identifier : z",
        "  Conditional ; liveness=(x)",
        "    Reference ; liveness=(x)",
        "      Identifier : r",
        "    ExpressionStmt ; liveness=(x)",
        "      SpecialOperation : COMMA ; liveness=(x)",
        "        AssignOperation : ASSIGN ; liveness=(x)",
        "          Reference ; liveness=(x)",
        "            Identifier : z",
        "          IntegerLiteral : 1 ; liveness=(x)",
        "        AssignOperation : ASSIGN ; liveness=(x z)",
        "          Reference ; liveness=(x z)",
        "            Identifier : y",
        "          IntegerLiteral : 3 ; liveness=(x z)",
        "    ReturnStmt ; liveness=(x)",
        // z is assigned in exiting branches
        "  ReturnStmt ; liveness=(x z y)",
        "    Reference ; liveness=(x z y)",
        "      Identifier : z"
        );
  }

  public final void testTernaryOp() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a, b, c;"
            + "((a = x) || (b = y)) ? (c = z) : (c = w);"
            + ";")),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : a",
        "    Declaration",
        "      Identifier : b",
        "    Declaration",
        "      Identifier : c",
        "  ExpressionStmt",
        "    ControlOperation : TERNARY",
        "      ControlOperation : LOGICAL_OR",
        "        AssignOperation : ASSIGN",
        "          Reference",
        "            Identifier : a",
        "          Reference",
        "            Identifier : x",
        "        AssignOperation : ASSIGN ; liveness=(a)",
        "          Reference ; liveness=(a)",
        "            Identifier : b",
        "          Reference ; liveness=(a)",
        "            Identifier : y",
        "      AssignOperation : ASSIGN ; liveness=(a)",
        "        Reference ; liveness=(a)",
        "          Identifier : c",
        "        Reference ; liveness=(a)",
        "          Identifier : z",
        "      AssignOperation : ASSIGN ; liveness=(a b)",
        "        Reference ; liveness=(a b)",
        "          Identifier : c",
        "        Reference ; liveness=(a b)",
        "          Identifier : w",
        "  Noop ; liveness=(a c)"
        );
  }

  public final void testForLoop() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "for (var i = 0, j, k = 3, n; i < (n = arr.length); ++j, ++k) {"
            + "  var v = arr[i], e;"
            + "  if (v) {"
            + "    e = 3;"
            + "  } else {"
            + "    break;"
            + "  }"
            + "  ;"
            + "}"
            + ";"
            )),
        "Block",
        "  ForLoop : ",
        "    MultiDeclaration",
        "      Declaration",
        "        Identifier : i",
        "        IntegerLiteral : 0",
        "      Declaration ; liveness=(i)",
        "        Identifier : j",
        "      Declaration ; liveness=(i)",
        "        Identifier : k",
        "        IntegerLiteral : 3 ; liveness=(i)",
        "      Declaration ; liveness=(i k)",
        "        Identifier : n",
        "    SimpleOperation : LESS_THAN ; liveness=(i k)",
        "      Reference ; liveness=(i k)",
        "        Identifier : i",
        "      AssignOperation : ASSIGN ; liveness=(i k)",
        "        Reference ; liveness=(i k)",
        "          Identifier : n",
        "        SpecialOperation : MEMBER_ACCESS ; liveness=(i k)",
        "          Reference ; liveness=(i k)",
        "            Identifier : arr",
        "          Reference ; liveness=(i k)",
        "            Identifier : length",
        "    ExpressionStmt ; liveness=(i k n v e)",
        "      SpecialOperation : COMMA ; liveness=(i k n v e)",
        "        AssignOperation : PRE_INCREMENT ; liveness=(i k n v e)",
        "          Reference ; liveness=(i k n v e)",
        "            Identifier : j",
        "        AssignOperation : PRE_INCREMENT ; liveness=(i k n v e j)",
        "          Reference ; liveness=(i k n v e j)",
        "            Identifier : k",
        "    Block ; liveness=(i k n)",
        "      MultiDeclaration ; liveness=(i k n)",
        "        Declaration ; liveness=(i k n)",
        "          Identifier : v",
        "          SpecialOperation : SQUARE_BRACKET ; liveness=(i k n)",
        "            Reference ; liveness=(i k n)",
        "              Identifier : arr",
        "            Reference ; liveness=(i k n)",
        "              Identifier : i",
        "        Declaration ; liveness=(i k n v)",
        "          Identifier : e",
        "      Conditional ; liveness=(i k n v)",
        "        Reference ; liveness=(i k n v)",
        "          Identifier : v",
        "        Block ; liveness=(i k n v)",
        "          ExpressionStmt ; liveness=(i k n v)",
        "            AssignOperation : ASSIGN ; liveness=(i k n v)",
        "              Reference ; liveness=(i k n v)",
        "                Identifier : e",
        "              IntegerLiteral : 3 ; liveness=(i k n v)",
        "        Block ; liveness=(i k n v)",
        "          BreakStmt :  ; liveness=(i k n v)",
        "      Noop ; liveness=(i k n v e)",  // e is assigned here due to break
        "  Noop ; liveness=(i k n)"
        );
  }

  public final void testForLoopThatDoesNotComplete() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a, b;"
            + "x: for (var i = 0; ; a = 1) {"
            + "  b = 1;"
            + "  break x;"
            + "}"
            + ";")),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : a",
        "    Declaration",
        "      Identifier : b",
        "  ForLoop : x",
        "    Declaration",
        "      Identifier : i",
        "      IntegerLiteral : 0",
        "    BooleanLiteral : true ; liveness=(i)",
        // Since body always exits, increment is unreachable
        "    ExpressionStmt",
        "      AssignOperation : ASSIGN",
        "        Reference",
        "          Identifier : a",
        "        IntegerLiteral : 1",
        "    Block ; liveness=(i)",
        "      ExpressionStmt ; liveness=(i)",
        "        AssignOperation : ASSIGN ; liveness=(i)",
        "          Reference ; liveness=(i)",
        "            Identifier : b",
        "          IntegerLiteral : 1 ; liveness=(i)",
        "      BreakStmt : x ; liveness=(i b)",
        "  Noop ; liveness=(i)");
  }

  public final void testForEachLoop() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a = 1, b, c;"
            + "for (var k in (b = obj)) {"
            + "  c = 1;"
            + "}"
            + ";")),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : a",
        "      IntegerLiteral : 1",
        "    Declaration ; liveness=(a)",
        "      Identifier : b",
        "    Declaration ; liveness=(a)",
        "      Identifier : c",
        "  ForEachLoop :  ; liveness=(a)",
        "    Declaration ; liveness=(a b)",
        "      Identifier : k",
        "    AssignOperation : ASSIGN ; liveness=(a)",
        "      Reference ; liveness=(a)",
        "        Identifier : b",
        "      Reference ; liveness=(a)",
        "        Identifier : obj",
        "    Block ; liveness=(a b k)",
        "      ExpressionStmt ; liveness=(a b k)",
        "        AssignOperation : ASSIGN ; liveness=(a b k)",
        "          Reference ; liveness=(a b k)",
        "            Identifier : c",
        "          IntegerLiteral : 1 ; liveness=(a b k)",
        "  Noop ; liveness=(a b)"
        );
  }

  public final void testDoWhileLoop() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a, b = 0, c, d;"
            + "do {"
            + "  a += 1;"  // b is live here
            + "} while (c = d + 1);"  // a and b live here
            + ";"  // a, b, and c are all live here
            )),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : a",
        "    Declaration",
        "      Identifier : b",
        "      IntegerLiteral : 0",
        "    Declaration ; liveness=(b)",
        "      Identifier : c",
        "    Declaration ; liveness=(b)",
        "      Identifier : d",
        "  DoWhileLoop :  ; liveness=(b)",
        "    Block ; liveness=(b)",
        "      ExpressionStmt ; liveness=(b)",
        "        AssignOperation : ASSIGN_SUM ; liveness=(b)",
        "          Reference ; liveness=(b)",
        "            Identifier : a",
        "          IntegerLiteral : 1 ; liveness=(b)",
        "    AssignOperation : ASSIGN ; liveness=(b a)",
        "      Reference ; liveness=(b a)",
        "        Identifier : c",
        "      SimpleOperation : ADDITION ; liveness=(b a)",
        "        Reference ; liveness=(b a)",
        "          Identifier : d",
        "        IntegerLiteral : 1 ; liveness=(b a)",
        "  Noop ; liveness=(b a c)"
        );
  }

  public final void testWhileLoop() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a, b = 0, c, d;"
            + "while (c = d + 1) {"  // b is live here
            + "  a += 1;"  // b and c are live here
            + "  ;"
            + "}"
            + ";"  // b and c are live here, but not a
            )),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : a",
        "    Declaration",
        "      Identifier : b",
        "      IntegerLiteral : 0",
        "    Declaration ; liveness=(b)",
        "      Identifier : c",
        "    Declaration ; liveness=(b)",
        "      Identifier : d",
        "  WhileLoop :  ; liveness=(b)",
        "    AssignOperation : ASSIGN ; liveness=(b)",
        "      Reference ; liveness=(b)",
        "        Identifier : c",
        "      SimpleOperation : ADDITION ; liveness=(b)",
        "        Reference ; liveness=(b)",
        "          Identifier : d",
        "        IntegerLiteral : 1 ; liveness=(b)",
        "    Block ; liveness=(b c)",
        "      ExpressionStmt ; liveness=(b c)",
        "        AssignOperation : ASSIGN_SUM ; liveness=(b c)",
        "          Reference ; liveness=(b c)",
        "            Identifier : a",
        "          IntegerLiteral : 1 ; liveness=(b c)",
        "      Noop ; liveness=(b c a)",
        "  Noop ; liveness=(b c)"
        );
  }

  public final void testTryCatchFinally() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a, b = 0, c, d;"
            + "try {"
            + "  a = f();"  // Cannot assume this completed later
            + "  ;"
            + "} catch (e) {"
            + "  c = e;"
            + "  return;"  // has no effect
            + "} finally {"
            + "  d = 1;"
            + "  ;"
            + "}"
            + ";")),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : a",
        "    Declaration",
        "      Identifier : b",
        "      IntegerLiteral : 0",
        "    Declaration ; liveness=(b)",
        "      Identifier : c",
        "    Declaration ; liveness=(b)",
        "      Identifier : d",
        "  TryStmt ; liveness=(b)",
        "    Block ; liveness=(b)",
        "      ExpressionStmt ; liveness=(b)",
        "        AssignOperation : ASSIGN ; liveness=(b)",
        "          Reference ; liveness=(b)",
        "            Identifier : a",
        "          SpecialOperation : FUNCTION_CALL ; liveness=(b)",
        "            Reference ; liveness=(b)",
        "              Identifier : f",
        "      Noop ; liveness=(b a)",
        "    CatchStmt ; liveness=(b)",
        "      Declaration ; liveness=(b)",
        "        Identifier : e",
        "      Block ; liveness=(b e@2)",
        "        ExpressionStmt ; liveness=(b e@2)",
        "          AssignOperation : ASSIGN ; liveness=(b e@2)",
        "            Reference ; liveness=(b e@2)",
        "              Identifier : c",
        "            Reference ; liveness=(b e@2)",
        "              Identifier : e",
        "        ReturnStmt ; liveness=(b e@2 c)",
        "    FinallyStmt ; liveness=(b)",
        "      Block ; liveness=(b)",
        "        ExpressionStmt ; liveness=(b)",
        "          AssignOperation : ASSIGN ; liveness=(b)",
        "            Reference ; liveness=(b)",
        "              Identifier : d",
        "            IntegerLiteral : 1 ; liveness=(b)",
        "        Noop ; liveness=(b d)",
        "  Noop ; liveness=(b d)"
        );
  }

  public final void testTryCatch() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a, b = 0, c, d;"
            + "try {"
            + "  a = f();"  // Cannot assume this completed later
            + "  c = 1;"
            + "} catch (e) {"
            + "  c = d = e;"
            + "}"
            + ";"  // Since c assigned in both, it is live here
            )),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : a",
        "    Declaration",
        "      Identifier : b",
        "      IntegerLiteral : 0",
        "    Declaration ; liveness=(b)",
        "      Identifier : c",
        "    Declaration ; liveness=(b)",
        "      Identifier : d",
        "  TryStmt ; liveness=(b)",
        "    Block ; liveness=(b)",
        "      ExpressionStmt ; liveness=(b)",
        "        AssignOperation : ASSIGN ; liveness=(b)",
        "          Reference ; liveness=(b)",
        "            Identifier : a",
        "          SpecialOperation : FUNCTION_CALL ; liveness=(b)",
        "            Reference ; liveness=(b)",
        "              Identifier : f",
        "      ExpressionStmt ; liveness=(b a)",
        "        AssignOperation : ASSIGN ; liveness=(b a)",
        "          Reference ; liveness=(b a)",
        "            Identifier : c",
        "          IntegerLiteral : 1 ; liveness=(b a)",
        "    CatchStmt ; liveness=(b)",
        "      Declaration ; liveness=(b)",
        "        Identifier : e",
        "      Block ; liveness=(b e@2)",
        "        ExpressionStmt ; liveness=(b e@2)",
        "          AssignOperation : ASSIGN ; liveness=(b e@2)",
        "            Reference ; liveness=(b e@2)",
        "              Identifier : c",
        "            AssignOperation : ASSIGN ; liveness=(b e@2)",
        "              Reference ; liveness=(b e@2)",
        "                Identifier : d",
        "              Reference ; liveness=(b e@2)",
        "                Identifier : e",
        "  Noop ; liveness=(b c)"
        );
  }

  public final void testTryCatchWithReturn() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a, b = 0, c;"
            + "try {"
            + "  a = f();"  // Can assume this completed later
            + "} catch (e) {"
            + "  return null;"  // because if it failed, we would have returned
            + "}"
            + ";"  // a and b are both visible here
            )),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : a",
        "    Declaration",
        "      Identifier : b",
        "      IntegerLiteral : 0",
        "    Declaration ; liveness=(b)",
        "      Identifier : c",
        "  TryStmt ; liveness=(b)",
        "    Block ; liveness=(b)",
        "      ExpressionStmt ; liveness=(b)",
        "        AssignOperation : ASSIGN ; liveness=(b)",
        "          Reference ; liveness=(b)",
        "            Identifier : a",
        "          SpecialOperation : FUNCTION_CALL ; liveness=(b)",
        "            Reference ; liveness=(b)",
        "              Identifier : f",
        "    CatchStmt ; liveness=(b)",
        "      Declaration ; liveness=(b)",
        "        Identifier : e",
        "      Block ; liveness=(b e@2)",
        "        ReturnStmt ; liveness=(b e@2)",
        "          NullLiteral : null ; liveness=(b e@2)",
        "  Noop ; liveness=(b a)"
        );
  }

  public final void testTryFinally() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a, b = 0, d;"
            + "try {"
            + "  a = f();"  // Cannot assume this completed later
            + "  ;"
            + "} finally {"
            + "  d = 1;"
            + "  ;"
            + "}"
            + ";")),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : a",
        "    Declaration",
        "      Identifier : b",
        "      IntegerLiteral : 0",
        "    Declaration ; liveness=(b)",
        "      Identifier : d",
        "  TryStmt ; liveness=(b)",
        "    Block ; liveness=(b)",
        "      ExpressionStmt ; liveness=(b)",
        "        AssignOperation : ASSIGN ; liveness=(b)",
        "          Reference ; liveness=(b)",
        "            Identifier : a",
        "          SpecialOperation : FUNCTION_CALL ; liveness=(b)",
        "            Reference ; liveness=(b)",
        "              Identifier : f",
        "      Noop ; liveness=(b a)",
        "    FinallyStmt ; liveness=(b)",
        "      Block ; liveness=(b)",
        "        ExpressionStmt ; liveness=(b)",
        "          AssignOperation : ASSIGN ; liveness=(b)",
        "            Reference ; liveness=(b)",
        "              Identifier : d",
        "            IntegerLiteral : 1 ; liveness=(b)",
        "        Noop ; liveness=(b d)",
        "  Noop ; liveness=(b d)"
        );
  }

  public final void testTryAndCatchBreak() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a;"
            + "do {"
            + "  try {"
            + "    a = f();"  // Cannot assume this completed later
            + "    break;"
            + "  } catch (ex) {"
            + "    a = 1;"
            + "    break;"
            + "  }"
            + "} while (false);"
            + ";")),
        "Block",
        "  Declaration",
        "    Identifier : a",
        "  DoWhileLoop : ",
        "    Block",
        "      TryStmt",
        "        Block",
        "          ExpressionStmt",
        "            AssignOperation : ASSIGN",
        "              Reference",
        "                Identifier : a",
        "              SpecialOperation : FUNCTION_CALL",
        "                Reference",
        "                  Identifier : f",
        "          BreakStmt :  ; liveness=(a)",
        "        CatchStmt",
        "          Declaration",
        "            Identifier : ex",
        "          Block ; liveness=(ex@4)",
        "            ExpressionStmt ; liveness=(ex@4)",
        "              AssignOperation : ASSIGN ; liveness=(ex@4)",
        "                Reference ; liveness=(ex@4)",
        "                  Identifier : a",
        "                IntegerLiteral : 1 ; liveness=(ex@4)",
        "            BreakStmt :  ; liveness=(ex@4 a)",
        "    BooleanLiteral : false",
        "  Noop ; liveness=(a)"
        );
  }

  public final void testTryReturns() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a;"
            + "do {"
            + "  try {"
            + "    return f();"  // Whether this completes or not is irrelevant
            + "  } catch (ex) {"
            + "    a = 1;"
            // TODO(mikesamuel): should work if there is a break here.
            // To fix this, processTryStmt should be smarter.  Instead of
            // intersecting, it should recognize that if the body returns, the
            // resulting type is that of the
            + "    break;"
            + "  }"
            + "} while (false);"
            + ";")),
        "Block",
        "  Declaration",
        "    Identifier : a",
        "  DoWhileLoop : ",
        "    Block",
        "      TryStmt",
        "        Block",
        "          ReturnStmt",
        "            SpecialOperation : FUNCTION_CALL",
        "              Reference",
        "                Identifier : f",
        "        CatchStmt",
        "          Declaration",
        "            Identifier : ex",
        "          Block ; liveness=(ex@4)",
        "            ExpressionStmt ; liveness=(ex@4)",
        "              AssignOperation : ASSIGN ; liveness=(ex@4)",
        "                Reference ; liveness=(ex@4)",
        "                  Identifier : a",
        "                IntegerLiteral : 1 ; liveness=(ex@4)",
        "            BreakStmt :  ; liveness=(ex@4 a)",
        "    BooleanLiteral : false",
        "  Noop ; liveness=(a)"
        );
  }

  public final void testTryAlwaysThrows() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a;"
            + "try {"
            + "  throw new Error;\n"
            + "} catch (ex) {"
            + "  a = 1;"
            + "}"
            + ";")),
        "Block",
        "  Declaration",
        "    Identifier : a",
        "  TryStmt",
        "    Block",
        "      ThrowStmt",
        "        SpecialOperation : CONSTRUCTOR",
        "          Reference",
        "            Identifier : Error",
        "    CatchStmt",
        "      Declaration",
        "        Identifier : ex",
        "      Block ; liveness=(ex@2)",
        "        ExpressionStmt ; liveness=(ex@2)",
        "          AssignOperation : ASSIGN ; liveness=(ex@2)",
        "            Reference ; liveness=(ex@2)",
        "              Identifier : a",
        "            IntegerLiteral : 1 ; liveness=(ex@2)",
        "  Noop ; liveness=(a)"  // a is live since the catch is always reached
        );
  }

  public final void testSwitchStmtWithDefault() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a, b;"
            + "switch (x) {"
            + "  case 0:"
            + "    return null;"
            + "  case 1:"
            + "    if (y) { return null; }"
            + "  case (b = 2):"
            + "    a = 1;"
            + "    break;"
            + "  default:"
            + "    a = 2;"
            + "}"
            + ";")),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : a",
        "    Declaration",
        "      Identifier : b",
        "  SwitchStmt : ",
        "    Reference",
        "      Identifier : x",
        "    CaseStmt",
        "      IntegerLiteral : 0",
        "      Block",
        "        ReturnStmt",
        "          NullLiteral : null",
        "    CaseStmt",
        "      IntegerLiteral : 1",
        "      Block",
        "        Conditional",
        "          Reference",
        "            Identifier : y",
        "          Block",
        "            ReturnStmt",
        "              NullLiteral : null",
        "    CaseStmt",
        "      AssignOperation : ASSIGN",
        "        Reference",
        "          Identifier : b",
        "        IntegerLiteral : 2",
        "      Block ; liveness=(b)",
        "        ExpressionStmt ; liveness=(b)",
        "          AssignOperation : ASSIGN ; liveness=(b)",
        "            Reference ; liveness=(b)",
        "              Identifier : a",
        "            IntegerLiteral : 1 ; liveness=(b)",
        "        BreakStmt :  ; liveness=(b a)",
        "    DefaultCaseStmt",
        "      Block",
        "        ExpressionStmt",
        "          AssignOperation : ASSIGN",
        "            Reference",
        "              Identifier : a",
        "            IntegerLiteral : 2",
        "  Noop ; liveness=(a)");
  }

  public final void testSwitchStmtWithNonCompletingDefault() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a;"
            + "switch (s) {"
            + "  case 'a': a = false; break;"
            + "  case 'b': a = true;  break;"
            + "  default:"
            + "    return null;"
            + "}"
            + ";")),
        "Block",
        "  Declaration",
        "    Identifier : a",
        "  SwitchStmt : ",
        "    Reference",
        "      Identifier : s",
        "    CaseStmt",
        "      StringLiteral : 'a'",
        "      Block",
        "        ExpressionStmt",
        "          AssignOperation : ASSIGN",
        "            Reference",
        "              Identifier : a",
        "            BooleanLiteral : false",
        "        BreakStmt :  ; liveness=(a)",
        "    CaseStmt",
        "      StringLiteral : 'b'",
        "      Block",
        "        ExpressionStmt",
        "          AssignOperation : ASSIGN",
        "            Reference",
        "              Identifier : a",
        "            BooleanLiteral : true",
        "        BreakStmt :  ; liveness=(a)",
        "    DefaultCaseStmt",
        "      Block",
        "        ReturnStmt",
        "          NullLiteral : null",
        "  Noop ; liveness=(a)");
  }

  public final void testSwitchStmtWithoutDefault() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a, b;"
            + "switch (b = 1, x) {"
            + "  case 0:"
            + "    return null;"
            + "  case 1:"
            + "    if (y) { return null; }"
            + "  case (b = 2):"
            + "    a = 1;"
            + "    break;"
            + "}"
            + ";")),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : a",
        "    Declaration",
        "      Identifier : b",
        "  SwitchStmt : ",
        "    SpecialOperation : COMMA",
        "      AssignOperation : ASSIGN",
        "        Reference",
        "          Identifier : b",
        "        IntegerLiteral : 1",
        "      Reference ; liveness=(b)",
        "        Identifier : x",
        "    CaseStmt ; liveness=(b)",
        "      IntegerLiteral : 0 ; liveness=(b)",
        "      Block ; liveness=(b)",
        "        ReturnStmt ; liveness=(b)",
        "          NullLiteral : null ; liveness=(b)",
        "    CaseStmt ; liveness=(b)",
        "      IntegerLiteral : 1 ; liveness=(b)",
        "      Block ; liveness=(b)",
        "        Conditional ; liveness=(b)",
        "          Reference ; liveness=(b)",
        "            Identifier : y",
        "          Block ; liveness=(b)",
        "            ReturnStmt ; liveness=(b)",
        "              NullLiteral : null ; liveness=(b)",
        "    CaseStmt ; liveness=(b)",
        "      AssignOperation : ASSIGN ; liveness=(b)",
        "        Reference ; liveness=(b)",
        "          Identifier : b",
        "        IntegerLiteral : 2 ; liveness=(b)",
        "      Block ; liveness=(b)",
        "        ExpressionStmt ; liveness=(b)",
        "          AssignOperation : ASSIGN ; liveness=(b)",
        "            Reference ; liveness=(b)",
        "              Identifier : a",
        "            IntegerLiteral : 1 ; liveness=(b)",
        "        BreakStmt :  ; liveness=(b a)",
        "  Noop ; liveness=(b)");
  }

  public final void testSwitchAllBreak() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a, b;"
            + "switch (x) {"
            + "  case 0:"
            + "    b = 1;"
            + "  case 1:"
            + "  case 2:"
            + "    a = true;"
            + "    break;"
            + "  default:"
            + "    a = false;"
            + "    break;"
            + "}"
            + ";")),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : a",
        "    Declaration",
        "      Identifier : b",
        "  SwitchStmt : ",
        "    Reference",
        "      Identifier : x",
        "    CaseStmt",
        "      IntegerLiteral : 0",
        "      Block",
        "        ExpressionStmt",
        "          AssignOperation : ASSIGN",
        "            Reference",
        "              Identifier : b",
        "            IntegerLiteral : 1",
        "    CaseStmt",
        "      IntegerLiteral : 1",
        "      Block",
        "    CaseStmt",
        "      IntegerLiteral : 2",
        "      Block",
        "        ExpressionStmt",
        "          AssignOperation : ASSIGN",
        "            Reference",
        "              Identifier : a",
        "            BooleanLiteral : true",
        "        BreakStmt :  ; liveness=(a)",
        "    DefaultCaseStmt",
        "      Block",
        "        ExpressionStmt",
        "          AssignOperation : ASSIGN",
        "            Reference",
        "              Identifier : a",
        "            BooleanLiteral : false",
        "        BreakStmt :  ; liveness=(a)",
        "  Noop ; liveness=(a)");
  }

  public final void testSwitchNoCases() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a, b;"
            + "switch (a = x) {}"
            + ";")),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : a",
        "    Declaration",
        "      Identifier : b",
        "  SwitchStmt : ",
        "    AssignOperation : ASSIGN",
        "      Reference",
        "        Identifier : a",
        "      Reference",
        "        Identifier : x",
        "  Noop ; liveness=(a)");
  }

  public final void testFunctionBody() throws Exception {
    String inFoo = " ; liveness=(foo@2 this@2 arguments@2 x@2 y@2)";
    String inA = " ; liveness=(b@2 this@2 arguments@2)";
    String inBar = " ; liveness=(bar@2 this@2 arguments@2 x@2)";
    assertLiveness(
        js(fromString(
            ""
            + "foo(1, 2);"
            + "function foo(x, y) {"
            + "  return x + bar(y);"
            + "}"
            + "var a = function b() { return this; };"
            + "function bar(x) { return [x, x]; }"
            + ";")),
        "Block",
        "  ExpressionStmt ; liveness=(foo bar)",  // fn declarations hoisted
        "    SpecialOperation : FUNCTION_CALL ; liveness=(foo bar)",
        "      Reference ; liveness=(foo bar)",
        "        Identifier : foo",
        "      IntegerLiteral : 1 ; liveness=(foo bar)",
        "      IntegerLiteral : 2 ; liveness=(foo bar)",
        "  FunctionDeclaration",  // nothing live here since it was hoisted
        "    Identifier : foo",
        "    FunctionConstructor",
        "      Identifier : foo",
        "      FormalParam",
        "        Identifier : x",
        "      FormalParam",
        "        Identifier : y",
        "      Block" + inFoo,
        "        ReturnStmt" + inFoo,
        "          SimpleOperation : ADDITION" + inFoo,
        "            Reference" + inFoo,
        "              Identifier : x",
        "            SpecialOperation : FUNCTION_CALL" + inFoo,
        "              Reference" + inFoo,
        "                Identifier : bar",
        "              Reference" + inFoo,
        "                Identifier : y",
        "  Declaration ; liveness=(foo bar)",  // var decls not hoisted
        "    Identifier : a",
        "    FunctionConstructor ; liveness=(foo bar)",
        "      Identifier : b",
        "      Block" + inA,
        "        ReturnStmt" + inA,
        "          Reference" + inA,
        "            Identifier : this",
        "  FunctionDeclaration ; liveness=(foo)",  // Hoisted but after foo
        "    Identifier : bar",
        "    FunctionConstructor ; liveness=(foo)",
        "      Identifier : bar",
        "      FormalParam",
        "        Identifier : x",
        "      Block" + inBar,
        "        ReturnStmt" + inBar,
        "          ArrayConstructor" + inBar,
        "            Reference" + inBar,
        "              Identifier : x",
        "            Reference" + inBar,
        "              Identifier : x",
        "  Noop ; liveness=(foo bar a)"
        );
  }

  public final void testBreaksAndContinues() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a, b;"
            + "do {"
            + "  if (f()) {"
            + "    b = 0;"
            + "    a = g();"
            + "  } else {"
            + "    a += 1;"
            + "    break;"
            + "  }"
            + "} while (a < 10);"   // a and b are live here because of break
            + ";")),                // only a is live here
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : a",
        "    Declaration",
        "      Identifier : b",
        "  DoWhileLoop : ",
        "    Block",
        "      Conditional",
        "        SpecialOperation : FUNCTION_CALL",
        "          Reference",
        "            Identifier : f",
        "        Block",
        "          ExpressionStmt",
        "            AssignOperation : ASSIGN",
        "              Reference",
        "                Identifier : b",
        "              IntegerLiteral : 0",
        "          ExpressionStmt ; liveness=(b)",
        "            AssignOperation : ASSIGN ; liveness=(b)",
        "              Reference ; liveness=(b)",
        "                Identifier : a",
        "              SpecialOperation : FUNCTION_CALL ; liveness=(b)",
        "                Reference ; liveness=(b)",
        "                  Identifier : g",
        "        Block",
        "          ExpressionStmt",
        "            AssignOperation : ASSIGN_SUM",
        "              Reference",
        "                Identifier : a",
        "              IntegerLiteral : 1",
        "          BreakStmt :  ; liveness=(a)",
        "    SimpleOperation : LESS_THAN ; liveness=(b a)",
        "      Reference ; liveness=(b a)",
        "        Identifier : a",
        "      IntegerLiteral : 10 ; liveness=(b a)",
        "  Noop ; liveness=(a)"
        );
  }

  public final void testAnonymousFunctions() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a;"
            // Assignments in immediately called closures have an effect
            + "(function () {"
            + "  a = 1;"
            + "})();"
            + ";")),
        "Block",
        "  Declaration",
        "    Identifier : a",
        "  ExpressionStmt",
        "    SpecialOperation : FUNCTION_CALL",
        "      FunctionConstructor",
        "        Identifier",
        "        Block ; liveness=(this@3 arguments@3)",
        "          ExpressionStmt ; liveness=(this@3 arguments@3)",
        "            AssignOperation : ASSIGN ; liveness=(this@3 arguments@3)",
        "              Reference ; liveness=(this@3 arguments@3)",
        "                Identifier : a",
        "              IntegerLiteral : 1 ; liveness=(this@3 arguments@3)",
        "  Noop ; liveness=(a)"
        );
  }

  public final void testWithBlock() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a, b, c, d = 1;"
            // Assignments inside the parentheses are safe to consider, but
            // we can't draw any conclusions about anything else.
            + "with (a = obj) {"
            + "  b = c = d;"
            + "  ;"
            + "}"
            + ";")),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : a",
        "    Declaration",
        "      Identifier : b",
        "    Declaration",
        "      Identifier : c",
        "    Declaration",
        "      Identifier : d",
        "      IntegerLiteral : 1",
        "  WithStmt ; liveness=(d)",
        "    AssignOperation : ASSIGN ; liveness=(d)",
        "      Reference ; liveness=(d)",
        "        Identifier : a",
        "      Reference ; liveness=(d)",
        "        Identifier : obj",
        "    Block",   // No liveness info here.  Just treat it as a black hole.
        "      ExpressionStmt",
        "        AssignOperation : ASSIGN",
        "          Reference",
        "            Identifier : b",
        "          AssignOperation : ASSIGN",
        "            Reference",
        "              Identifier : c",
        "            Reference",
        "              Identifier : d",
        "      Noop",
        // The assignment inside the with parens does give useful info
        "  Noop ; liveness=(d a)"
        );
  }

  public final void testUnreachableCode1() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a, b, c = 1;"
            + "do {"
            + "  a = 1;"
            + "  break;"
            + "  b = 1;"  // not reached
            + "} while(false);"  // not reached
            + ";")),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : a",
        "    Declaration",
        "      Identifier : b",
        "    Declaration",
        "      Identifier : c",
        "      IntegerLiteral : 1",
        "  DoWhileLoop :  ; liveness=(c)",
        "    Block ; liveness=(c)",
        "      ExpressionStmt ; liveness=(c)",
        "        AssignOperation : ASSIGN ; liveness=(c)",
        "          Reference ; liveness=(c)",
        "            Identifier : a",
        "          IntegerLiteral : 1 ; liveness=(c)",
        "      BreakStmt :  ; liveness=(c a)",
        // No liveness calculated for unreachable code
        "      ExpressionStmt",
        "        AssignOperation : ASSIGN",
        "          Reference",
        "            Identifier : b",
        "          IntegerLiteral : 1",
        "    BooleanLiteral : false",
        "  Noop ; liveness=(c a)"
        );
  }

  public final void testUnreachableCode2() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a, b, c = 1;"
            + "do {"
            + "  a = 1;"
            + "  continue;"
            + "  b = 1;"  // Still not reached
            + "} while(false);"  // Unlike the prior, this is reached
            + ";")),
        "Block",
        "  MultiDeclaration",
        "    Declaration",
        "      Identifier : a",
        "    Declaration",
        "      Identifier : b",
        "    Declaration",
        "      Identifier : c",
        "      IntegerLiteral : 1",
        "  DoWhileLoop :  ; liveness=(c)",
        "    Block ; liveness=(c)",
        "      ExpressionStmt ; liveness=(c)",
        "        AssignOperation : ASSIGN ; liveness=(c)",
        "          Reference ; liveness=(c)",
        "            Identifier : a",
        "          IntegerLiteral : 1 ; liveness=(c)",
        "      ContinueStmt :  ; liveness=(c a)",
        // No liveness calculated for unreachable code
        "      ExpressionStmt",
        "        AssignOperation : ASSIGN",
        "          Reference",
        "            Identifier : b",
        "          IntegerLiteral : 1",
        "    BooleanLiteral : false ; liveness=(c a)",
        "  Noop ; liveness=(c a)"
        );
  }

  public final void testLoopLabels() throws Exception {
    assertLiveness(
        js(fromString(
            ""
            + "var a = null;"
            + "foo: while (1) {"
            + "  if (rand)"
            + "    break;"
            + "  else"
            + "    break foo;"
            + "  unreachable;"
            + "}"
            + ";")),  // reached
        "Block",
        "  Declaration",
        "    Identifier : a",
        "    NullLiteral : null",
        "  WhileLoop : foo ; liveness=(a)",
        "    IntegerLiteral : 1 ; liveness=(a)",
        "    Block ; liveness=(a)",
        "      Conditional ; liveness=(a)",
        "        Reference ; liveness=(a)",
        "          Identifier : rand",
        "        BreakStmt :  ; liveness=(a)",
        "        BreakStmt : foo ; liveness=(a)",
        "      ExpressionStmt",  // not reached
        "        Reference",
        "          Identifier : unreachable",
        "  Noop ; liveness=(a)"  // reached.  Yay!!!
        );
  }

  public final void testLogicOps() throws Exception {
    // In the next two tests,
    // a is live between the second && and the || but not right of the ||

    // This is the easy case where an assignment in the LHS of the && makes the
    // variable live in the RHS.
    assertLiveness(
        js(fromString(
            ""
            + "var a;"
            + "t && ((a = foo) && a.indexOf('f') >= 0) || f;")),
        "Block",
        "  Declaration",
        "    Identifier : a",
        "  ExpressionStmt",
        "    ControlOperation : LOGICAL_OR",
        "      ControlOperation : LOGICAL_AND",
        "        Reference",
        "          Identifier : t",
        "        ControlOperation : LOGICAL_AND",
        "          AssignOperation : ASSIGN",
        "            Reference",
        "              Identifier : a",
        "            Reference",
        "              Identifier : foo",
        "          SimpleOperation : GREATER_EQUALS ; liveness=(a)",
        "            SpecialOperation : FUNCTION_CALL ; liveness=(a)",
        "              SpecialOperation : MEMBER_ACCESS ; liveness=(a)",
        "                Reference ; liveness=(a)",
        "                  Identifier : a",
        "                Reference ; liveness=(a)",
        "                  Identifier : indexOf",
        "              StringLiteral : 'f' ; liveness=(a)",
        "            IntegerLiteral : 0 ; liveness=(a)",
        "      Reference",
        "        Identifier : f");
    // But when logical ops nest, it's possible for an implementation to lose
    // liveness information.
    assertLiveness(
        js(fromString(
            ""
            + "var a;"
            + "t && (a = foo) && a.indexOf('f') >= 0 || f;")),
        "Block",
        "  Declaration",
        "    Identifier : a",
        "  ExpressionStmt",
        "    ControlOperation : LOGICAL_OR",
        "      ControlOperation : LOGICAL_AND",
        "        ControlOperation : LOGICAL_AND",
        "          Reference",
        "            Identifier : t",
        "          AssignOperation : ASSIGN",
        "            Reference",
        "              Identifier : a",
        "            Reference",
        "              Identifier : foo",
        "        SimpleOperation : GREATER_EQUALS ; liveness=(a)",
        "          SpecialOperation : FUNCTION_CALL ; liveness=(a)",
        "            SpecialOperation : MEMBER_ACCESS ; liveness=(a)",
        "              Reference ; liveness=(a)",
        "                Identifier : a",
        "              Reference ; liveness=(a)",
        "                Identifier : indexOf",
        "            StringLiteral : 'f' ; liveness=(a)",
        "          IntegerLiteral : 0 ; liveness=(a)",
        "      Reference",
        "        Identifier : f");

    assertLiveness(
        js(fromString(
            ""
            + "var a;"
            + "if (b || (a = c)) {"
            + "} else {"
            + "}"
            + ";")),
        "Block",
        "  Declaration",
        "    Identifier : a",
        "  Conditional",
        "    ControlOperation : LOGICAL_OR",
        "      Reference",
        "        Identifier : b",
        "      AssignOperation : ASSIGN",
        "        Reference",
        "          Identifier : a",
        "        Reference",
        "          Identifier : c",
        "    Block",
        "    Block ; liveness=(a)",
        "  Noop");
  }

  public final void testLiveBreaks() throws Exception {
    ExitModes exits = setup(js(fromString(
        ""
        + "a: for (var i = 0; i < 10; ++i) {\n"
        + "  if (foo()) {\n"
        + "    continue a;\n"
        + "  } else if (bar()) {\n"
        + "    break a;\n"
        + "  } else if (baz()) {\n"
        + "    break b;\n"   // LIVE line 7
        + "  }\n"
        + "}\n"
        + "switch (x) {\n"
        + "  case 0: f(); break;\n"
        + "  case 1: continue;\n"   // LIVE line 12
        + "  case 2: break b;\n"   // LIVE line 13
        + "}\n"
        + "x: for (var x in o) {\n"
        + "  break x;\n"
        + "}\n"
        + "foo: {\n"
        + "  if (Math.random()) {\n"
        + "    break foo;\n"
        + "  } else if (Math.random()) {\n"
        + "    break;\n"
        + "  } else {\n"
        + "    break bar;\n"  // LIVE 24
        + "  }\n"
        + "}\n"
        + "break;\n"   // LIVE 27
        )));
    Iterator<Statement> stmts = exits.liveExits().iterator();
    assertTrue(stmts.hasNext());
    assertEquals(
        "testLiveBreaks:7", formatShort(stmts.next().getFilePosition()));
    assertTrue(stmts.hasNext());
    assertEquals(
        "testLiveBreaks:12", formatShort(stmts.next().getFilePosition()));
    assertTrue(stmts.hasNext());
    assertEquals(
        "testLiveBreaks:13", formatShort(stmts.next().getFilePosition()));
    assertTrue(stmts.hasNext());
    assertEquals(
        "testLiveBreaks:24", formatShort(stmts.next().getFilePosition()));
    assertTrue(stmts.hasNext());
    assertEquals(
        "testLiveBreaks:27", formatShort(stmts.next().getFilePosition()));
    assertFalse(stmts.hasNext());
  }

  public final void testDirectives() throws Exception {
    assertLiveness(
        js(fromString("\"use strict\";")),
        "Block",
        "  DirectivePrologue",
        "    Directive : use strict");
  }
  
  public final void testDebugger() throws Exception {
    assertLiveness(
        js(fromString("debugger; var x = 3; debugger;")),
        "Block",
        "  DebuggerStmt",
        "  Declaration",
        "    Identifier : x",
        "    IntegerLiteral : 3",
        "  DebuggerStmt ; liveness=(x)");
  }

  private static ExitModes setup(ParseTreeNode js) {
    ScopeAnalyzer sa = new ScopeAnalyzer();
    sa.computeLexicalScopes(AncestorChain.instance(js));
    return VariableLiveness.calculateLiveness(js).exits;
  }

  private static void assertLiveness(ParseTreeNode js, String... golden) {
    ExitModes exits = setup(js);
    // None of the tests above have breaks or continues out of place.
    // Since the liveExits() list is used to find out of place things,
    // assert that no breaks or continues occur on the list.
    for (Statement stmt : exits.liveExits()) {
      if (!(stmt instanceof ThrowStmt || stmt instanceof ReturnStmt)) {
        fail("Unexpected exit at " + stmt.getFilePosition());
      }
    }

    StringBuilder sb = new StringBuilder();
    try {
      MessageContext mc = new MessageContext();
      mc.relevantKeys = Collections.singleton(VariableLiveness.LIVENESS);
      js.format(mc, sb);
    } catch (IOException ex) {
      throw new RuntimeException(ex);  // Not for StringBuilder
    }

    MoreAsserts.assertListsEqual(
        Arrays.asList(golden),
        Arrays.asList(sb.toString().replaceAll("@0", "")
            .split("( ; liveness=\\(\\))?(?:\n|$)")));
  }
}
