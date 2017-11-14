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
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ForEachLoop;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.NumberLiteral;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Adds the unary + operator to indices in square bracket operators
 * when the index can be proven to always be a number or undefined:
 * <code>a[i]</code> &rarr; <code>a[+i]</code>.
 *
 * <p>The ES53Rewriter will optimize reads of properties where the index is
 * provably numeric.</p>
 *
 * <h2>Assumptions</h2>
 * <ul>
 * <li>That an interpreter's primitive to string conversion cannot be affected
 *   by untrusted code.  {@code Number.prototype.toString = function () ...}
 *   does not work on targeted interpreters, cannot be effected by user code,
 *   or does not affect the result of {@code '' + 3}.
 * <li>That the string form of all numeric values (as specified in ES3 section
 *   9.8.1); including negative, infinite, {@code NaN}, and non-integral values;
 *   are visible properties of {@code Object}.
 * <li>That untrusted code cannot define a getter for an array index which
 *   mutates a frozen object.
 * <li>That a variable that is only ever assigned a numeric value, when used
 *   as an object property name before initialization matches the name
 *   {@code NaN} instead of the name {@code undefined}.
 *   This will be documented as a gotcha in the cajoler spec, and can be seen in
 *   the following code:<pre>
 *     var a = { 0: 'a', NaN: 'b', undefined: 'c' };
 *     var i;
 *     alert(a[i]);
 *     i = 0;
 *     alert(a[i]);
 *   </pre>
 *   If necessary, this gotcha can be repaired by checking for
 *   use-before-initialization.
 * <li>Accesses to local variables cannot trigger a
 *   getter which returns a non-numeric value if all assignments have assigned
 *   numeric values.
 * <li>Functions from other lexical scopes cannot modify function parameters
 *   or local.  I.e. there is no way to modify local variables in the way that
 *   <code>otherFunction.arguments[0] = 'foo'</code> modifies parameters.
 *   Specifically, code cannot use <code>eval</code> to assign non-numeric
 *   values to local variables.
 * </ul>
 *
 * <p>
 * In this class, the term "visible property" refers to any property which is
 * readable on <b>all</b> objects.
 *
 * @author mikesamuel@gmail.com
 */
public final class ArrayIndexOptimization {

  /**
   * Adds the unary + operator to square bracket operator indices where the
   * index is provably numeric.
   * @param root the root of a javascript tree.
   */
  public static void optimize(ParseTreeNode root) {
    ScopeTree scopeRoot = ScopeTree.create(AncestorChain.instance(root));
    optimize(root, scopeRoot);
  }

  public static boolean hasNumericResult(Expression e) {
    return "number".equals(e.typeOf());
  }

  private static void optimize(ParseTreeNode node, ScopeTree t) {
    if (node instanceof Operation) {
      Operation op = (Operation) node;
      if (op.getOperator() == Operator.SQUARE_BRACKET) {
        Expression index = op.children().get(1);
        Set<String> expanding = new HashSet<String>();
        if (isVisiblePropertyExpr(index, t, expanding)) {
          Operation numIndex = Operation.create(
              index.getFilePosition(), Operator.TO_NUMBER, index);
          numIndex.setFilePosition(index.getFilePosition());
          op.replaceChild(numIndex, index);
        }
      }
    }
    for (ParseTreeNode child : node.children()) {
      optimize(child, t.scopeForChild(child));
    }
  }

  /**
   * True if all uses of the given reference as a RightHandSideExpression in
   * scopeTree are guaranteed to result in a visible property.
   */
  static boolean doesVarReferenceVisibleProperty(
      Reference r, ScopeTree scopeTree, Set<String> identifiersExpanding) {
    if (!scopeTree.isSymbolDeclared(r.getIdentifierName())) {
      return false;
    }
    for (AncestorChain<Identifier> use
         : scopeTree.usesOf(r.getIdentifierName())) {
      if (use.parent.node instanceof Reference) {
        AncestorChain<?> gp = use.parent.parent;
        if (isKeyReceiver(use.parent)) {
          return false;
        }
        // If it's the LHS of an assignment, check the RHS
        if (gp.node instanceof Operation
            && use.parent.node == gp.node.children().get(0)) {
          Operation operation = gp.cast(Operation.class).node;
          Operator op = operation.getOperator();
          if (op == Operator.ASSIGN) {
            if (!isVisiblePropertyExpr(
                    operation.children().get(1), scopeTree,
                    identifiersExpanding)) {
              return false;
            }
          } else if (op.getAssignmentDelegate() != null) {
            if (!isNumberOrUndefOperator(op.getAssignmentDelegate())) {
              return false;
            }
          }
          // Although the increment/decrement operators are not handled here
          // and they do cause an assignment, they will always assign a numeric
          // value, so they do not need to be considered except for purposes of
          // uninitialized variable analysis.
        }
      } else if (use.parent.node instanceof Declaration) {
        Declaration d = (Declaration) use.parent.node;
        // If it's initialized as a result of some non-local value such as
        // a function call or thrown exception, assume it's not numeric.
        if (d instanceof FormalParam) { return false; }
        if (use.parent.parent.node instanceof CatchStmt) { return false; }
        // Otherwise the initializer had better not be a non-numeric value.
        if (d.getInitializer() != null) {
          Expression init = d.getInitializer();
          if (!isVisiblePropertyExpr(init, scopeTree, identifiersExpanding)) {
            return false;
          }
        } else if (isKeyReceiver(use)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * True if the expression is guaranteed to return a value {@code v} s.t.
   * {@code ('' + v)} is a visible property name.
   *
   * <h2>Shortcomings of the current implementation</h2>
   * This method is a heuristic.  It will return true for many expressions that
   * are guaranteed to return a visible property but not all.
   * <p>
   * Specifically, it will not handle concatenations such as {@code ('1' + '2')}
   * and will not attempt to predict function binding or evaluation.
   * <p>
   * It will also ignore any variables that are co-assigners, as in
   * {@code
   *   var i = 0, j = 0, tmp;
   *   ...
   *   j = (tmp = i, i = j, tmp);
   * }
   * and the simpler but equivalent
   * {@code
   *   var i = 0, j = 0, tmp;
   *   ...
   *   tmp = i;
   *   i = j;
   *   j = i;
   * }
   * <p>It will return false if the {@code +} operator is used to produce a
   * value assigned to the referenced variable.  This is true even if both
   * operands are provably numeric.
   *
   * @param e an expression evaluated in the context of scopeTree.
   * @param scopeTree the scope in which e is evaluated.
   * @param identifiersExpanding a set of identifiers in e that are being
   *     checked.  This is used to prevent infinite recursion when identifiers
   *     are co-assigned to one another.
   * @return true if e is guaranteed to either not halt or to return a value
   *     whose string form is a visible property.
   */
  static boolean isVisiblePropertyExpr(
      Expression e, ScopeTree scopeTree, Set<String> identifiersExpanding) {
    if (e instanceof NumberLiteral) { return true; }
    if (e instanceof Operation) {
      Operation op = (Operation) e;
      switch (op.getOperator()) {
        case COMMA:
          Expression last = op.children().get(1);
          return isVisiblePropertyExpr(last, scopeTree, identifiersExpanding);
        // || and && pass through one of their operands unchanged.
        // The addition operator works as follows:
        // 11.6.1 Additive Operator
        //   ...
        //   4. Call GetValue(Result(3)).
        //   5. Call ToPrimitive(Result(2)).
        //   6. Call ToPrimitive(Result(4)).
        //   7. If Type(Result(5)) is String or Type(Result(6)) is String, go to
        //      step 12. (Note that this step differs from step 3 in the
        //      comparison algorithm for the relational operators, by using or
        //      instead of and.)
        //   8. Call ToNumber(Result(5)).
        //   9. Call ToNumber(Result(6)).
        //   ...
        // which means that (undefined + undefined) is a number, and so if both
        // operands are undefined or numeric, the result is guaranteed to be
        // numeric.
        case LOGICAL_OR: case LOGICAL_AND: case ADDITION:
          return isVisiblePropertyExpr(
              op.children().get(0), scopeTree, identifiersExpanding)
              && isVisiblePropertyExpr(
              op.children().get(1), scopeTree, identifiersExpanding);
        case TERNARY:
          return isVisiblePropertyExpr(
              op.children().get(1), scopeTree, identifiersExpanding)
              && isVisiblePropertyExpr(
              op.children().get(2), scopeTree, identifiersExpanding);
        case ASSIGN:
          return isVisiblePropertyExpr(
              op.children().get(1), scopeTree, identifiersExpanding);
        default:
          if (isNumberOrUndefOperator(op.getOperator())) {
            return true;
          }
          break;
      }
    } else if (e instanceof Reference) {
      Reference r = (Reference) e;
      String name = r.getIdentifierName();
      if (!identifiersExpanding.contains(name)) {
        identifiersExpanding.add(name);
        return doesVarReferenceVisibleProperty(
            r, scopeTree, identifiersExpanding);
      }
    }
    return false;
  }

  /**
   * True if the given operator always produces a numeric or undefined result,
   * or fails with an exception.
   * This is independent of whether the operator also has a side effect.
   * This is a heuristic in the same way as {@link #isVisiblePropertyExpr}.
   */
  static boolean isNumberOrUndefOperator(Operator o) {
    if (o.getAssignmentDelegate() != null) {
      o = o.getAssignmentDelegate();
    }
    return o == Operator.VOID || hasNumericResult(o);
  }

  private static final EnumSet<Operator> NUMERIC_OPERATORS
      = EnumSet.noneOf(Operator.class);
  static {
    Reference xref = new Reference(new Identifier(FilePosition.UNKNOWN, "x"));
    for (Operator o : Operator.values()) {
      Reference[] operands = new Reference[o.getType().getArity()];
      Arrays.fill(operands, xref);
      Operation operation = Operation.create(FilePosition.UNKNOWN, o, operands);
      if ("number".equals(operation.typeOf())) {
        NUMERIC_OPERATORS.add(o);
      }
    }
  }
  static boolean hasNumericResult(Operator o) {
    return NUMERIC_OPERATORS.contains(o);
  }

  static boolean isKeyReceiver(AncestorChain<?> ac) {
    if (ac == null || ac.parent == null) { return false; }
    if (!(ac.parent.node instanceof CatchStmt)) {
      ac = ac.parent;
      if (ac.parent == null) { return false; }
      if (!(ac.parent.node instanceof ForEachLoop)) {
        return false;
      }
    }
    return ac.node == ac.parent.node.children().get(0);
  }
}
