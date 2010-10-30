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

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Executor;
import com.google.caja.util.RhinoTestBed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import junit.framework.AssertionFailedError;

public class ArrayIndexOptimizationTest extends CajaTestCase {
  public final void testIsNumberOrUndefOperator() throws IOException {
    List<Statement> stmts = new ArrayList<Statement>();
    for (Operator op : Operator.values()) {
      if (!ArrayIndexOptimization.isNumberOrUndefOperator(op)) { continue; }
      Reference[] operands = new Reference[op.getType().getArity()];
      fillOperands(op, 0, operands, stmts);
    }
    runArrayOptOperatorTest(stmts);
  }

  public final void testTestFramework() throws IOException {
    List<Statement> stmts = new ArrayList<Statement>();
    fillOperands(Operator.ADDITION, 0, new Reference[2], stmts);
    try {
      runArrayOptOperatorTest(stmts);
    } catch (AssertionFailedError e) {
      return;
    }
    fail("Expected failure on foo+foo");
  }

  static final String REFERENCE_EXAMPLE = (
      ""
      + "var n = 3;"
      + "return function (arr) {"
      + "  var i = 0, j = 0, k, l = 0, m = 0, o = 1;"
      + "  for (var i = 0; i < arr.length; ++i, m++) { arr[i] += j; }"
      + "  j = arr[0].toString();"
      + "  k = arr[1] ? i * 2 : i;"
      + "  (function () {"
      + "     l += x;"
      + "   })();"
      + "  o = m + 1;"
      + "  return arr[i][j][k];"
      + "};");

  public final void testDoesVarReferenceArrayMember() throws Exception {
    ScopeTree global = ScopeTree.create(
        AncestorChain.instance(js(fromString(REFERENCE_EXAMPLE))));
    ScopeTree inner = global.children().get(0);
    assertTrue(ArrayIndexOptimization.doesVarReferenceVisibleProperty(
        new Reference(ident("i")), inner, new HashSet<String>()));
    // Can't determine what arr[0].toString() is.
    assertFalse(ArrayIndexOptimization.doesVarReferenceVisibleProperty(
        new Reference(ident("j")), inner, new HashSet<String>()));
    // i is, and k is defined in terms of numeric operations on i.
    // As long as this works, single use temporary variables will not prevent
    // this optimization from working.
    assertTrue(ArrayIndexOptimization.doesVarReferenceVisibleProperty(
        new Reference(ident("k")), inner, new HashSet<String>()));
    // l is modified in a closure using a value that is not provably numeric.
    assertFalse(ArrayIndexOptimization.doesVarReferenceVisibleProperty(
        new Reference(ident("l")), inner, new HashSet<String>()));
    // m is modified by a pre-increment which is not a numeric operator
    // for reasons discussed in isNumericOperator, but it always assigns a
    // numeric value, so m is numeric.
    assertTrue(ArrayIndexOptimization.doesVarReferenceVisibleProperty(
        new Reference(ident("m")), inner, new HashSet<String>()));
    // n is defined in an outer scope, but all uses of it are numeric.
    assertTrue(ArrayIndexOptimization.doesVarReferenceVisibleProperty(
        new Reference(ident("n")), inner, new HashSet<String>()));
    // o is assigned the result of an addition, but both operands are numeric
    // or undefined.
    assertTrue(ArrayIndexOptimization.doesVarReferenceVisibleProperty(
        new Reference(ident("o")), inner, new HashSet<String>()));
    // Initialization of arr is out of the control of the code sampled.
    assertFalse(ArrayIndexOptimization.doesVarReferenceVisibleProperty(
        new Reference(ident("arr")), inner, new HashSet<String>()));
    // x is not defined in this scope, so must be suspect
    assertFalse(ArrayIndexOptimization.doesVarReferenceVisibleProperty(
        new Reference(ident("x")), inner, new HashSet<String>()));
  }

  public final void testSimpleReferences() throws Exception {
    Block b = js(fromString(
        ""
        + "function map(f, arr) {\n"
        + "  for (var i = 0, n = arr.length; i < n; ++i) {\n"
        + "    f(arr[i]);\n"
        + "  }\n"
        + "}"));
    ArrayIndexOptimization.optimize(b);
    ParseTreeNode golden = js(fromString(
        ""
        + "function map(f, arr) {\n"
        + "  for (var i = 0, n = arr.length; i < n; ++i) {\n"
        + "    f(arr[+i]);\n"
        + "  }\n"
        + "}"));
    assertEquals(render(golden), render(b));
  }

  public void checkReferenceChains() throws Exception {
    Block b = js(fromString(REFERENCE_EXAMPLE));
    ArrayIndexOptimization.optimize(b);
    ParseTreeNode golden = js(fromString(
        ""
        + "function map(f, arr) {\n"
        + "  for (var i = 0, n = arr.length; i < n; ++i) {\n"
        + "    f(arr[+i]);\n"
        + "  }\n"
        + "}"));
    assertEquals(render(golden), render(b));
  }

  public final void testSubtraction() throws Exception {
    Block b = js(fromString(
        ""
        + "function lastOf(arr) {\n"
        + "  return arr[arr.length - 1];\n"
        + "}"));
    ArrayIndexOptimization.optimize(b);
    ParseTreeNode golden = js(fromString(
        ""
        + "function lastOf(arr) {\n"
        + "  return arr[+(arr.length - 1)];\n"
        + "}"));
    assertEquals(render(golden), render(b));
  }

  public final void testAddition() throws Exception {
    Block b = js(fromString(
        ""
        + "function join(arr, sep) {\n"
        + "  var s = '';\n"
        + "  for (var i = 0; i < arr.length; i++) {"
        + "    if (s && arr[i + 1]) { s += sep; }"
        + "    s += arr[i];"
        + "  }"
        + "}"
        + "join(myArray[foo + bar]);"));
    ArrayIndexOptimization.optimize(b);
    ParseTreeNode golden = js(fromString(
        ""
        + "function join(arr, sep) {\n"
        + "  var s = '';\n"
        + "  for (var i = 0; i < arr.length; i++) {"
        + "    if (s && arr[+(i + 1)]) { s += sep; }"
        + "    s += arr[+i];"
        + "  }"
        + "}"
        // Not optimized.
        + "join(myArray[foo + bar]);"));
    assertEquals(render(golden), render(b));
  }

  public final void testCompoundAssignments() throws Exception {
    Block b = js(fromString(
        ""
        + "function lastIndexOf(arr, o) {\n"
        + "  for (var i = arr.length; i > 0;) {\n"
        + "    if (o === arr[--i]) { return i; }\n"
        + "  }\n"
        + "}"));
    ArrayIndexOptimization.optimize(b);
    ParseTreeNode golden = js(fromString(
        ""
        + "function lastIndexOf(arr, o) {\n"
        + "  for (var i = arr.length; i > 0;) {\n"
        + "    if (o === arr[+(--i)]) { return i; }\n"
        + "  }\n"
        + "}"));
    assertEquals(render(golden), render(b));
  }

  public final void testConcatenation() throws Exception {
    Block b = js(fromString(
        ""
        + "function cheating(arr) {\n"
        + "  return arr['length' + 'length'];\n"
        + "}"));
    ArrayIndexOptimization.optimize(b);
    ParseTreeNode golden = js(fromString(
        ""
        + "function cheating(arr) {\n"
        + "  return arr['length' + 'length'];\n"
        + "}"));
    assertEquals(render(golden), render(b));
  }

  public final void testBug1292() throws Exception {
    Block b = js(fromString("this;"));
    ArrayIndexOptimization.optimize(b);
    assertEquals("this;", renderProgram(b));
  }

  /** Correspond to the global vars defined in array-opt-operator-test.js */
  private static Reference[] REFERENCES = {
    new Reference(ident("undefined")),
    new Reference(ident("nullValue")),
    new Reference(ident("zero")),
    new Reference(ident("negOne")),
    new Reference(ident("posOne")),
    new Reference(ident("truthy")),
    new Reference(ident("untruthy")),
    new Reference(ident("emptyString")),
    new Reference(ident("numberLikeString")),
    new Reference(ident("fooString")),
    new Reference(ident("lengthString")),
    new Reference(ident("anObj")),
    new Reference(ident("sneaky")),
    new Reference(ident("f")),
  };

  private void runArrayOptOperatorTest(List<Statement> stmts)
      throws IOException {
    RhinoTestBed.runJs(
        new Executor.Input(getClass(), "/js/jsunit/2.2/jsUnitCore.js"),
        new Executor.Input(getClass(), "array-opt-operator-test.js"),
        new Executor.Input(render(
            new Block(FilePosition.UNKNOWN, stmts)), getName()));
  }

  private static void fillOperands(
      Operator op, int operandIdx, Reference[] operands, List<Statement> out) {
    if (operandIdx == operands.length) {
      out.add(new ExpressionStmt(
          FilePosition.UNKNOWN,
          (Expression) QuasiBuilder.substV(
              "requireArrayMember(function () { return @e; });",
              "e", Operation.create(FilePosition.UNKNOWN, op, operands))));
      return;
    }
    for (Reference r : REFERENCES) {
      operands[operandIdx] = r;
      fillOperands(op, operandIdx + 1, operands, out);
    }
  }

  private static Identifier ident(String name) {
    return new Identifier(FilePosition.UNKNOWN, name);
  }
}
