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

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.Conditional;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.NullLiteral;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.OperatorCategory;
import com.google.caja.parser.js.RealLiteral;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.Scope;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;
import com.google.caja.util.Pair;
import com.google.caja.util.Sets;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An optimizer which uses facts about the environment in which the program
 * will be run to eliminate dead branches.
 *
 * @author mikesamuel@gmail.com
 */
public class ParseTreeKB {
  private final Map<String, Pair<Expression, Fact>> facts = Maps.newHashMap();
  private boolean needsInference;
  private int longestKeyLength = 0;

  private static final FilePosition UNK = FilePosition.UNKNOWN;

  public ParseTreeKB() {
    addFactInt(new Reference(new Identifier(UNK, "undefined")), Fact.UNDEFINED);
    addFactInt(new Reference(new Identifier(UNK, "this")), Fact.GLOBAL);
  }

  /**
   * @param e an expression containing no non-global references.
   */
  public Fact getFact(Expression e) {
    return getFact(optNodeDigest(e));
  }

  private Fact getFact(String digest) {
    Pair<Expression, Fact> fact = facts.get(digest);
    return fact != null ? fact.b : null;
  }

  /**
   * Produces a similar parse tree
   *
   * @param js not mutated.
   * @param mq receives scoping warnings.
   * @return js if no optimizations were performed.  Otherwise a partial clone.
   */
  public Block optimize(Block js, MessageQueue mq) {
    finishInference();
    while (true) {
      Result out = new Result();
      Scope s = Scope.fromProgram(js, mq);
      optimize(s, js, false, false, false, false, out);
      Block optimized = ConstLocalOptimization.optimize((Block) out.node);
      if (optimized == js) { return optimized; }
      js = optimized;
    }
  }

  /**
   * Adds a fact about the environment to the knowledge base, and adds facts
   * easily inferred from the input fact.
   */
  public void addFact(Expression e, Fact fact) {
    // Recursively fold e, since that is how the optimizer will compare it.
    // This has the side effect of turning complex expressions like (1/0),
    // (0/0), and (-0) into NumberLiterals.
    addFactInt(rfold(e, false), fact);
  }

  private void addFactInt(Expression e, Fact fact) {
    // If we know it is a boolean, we can upgrade from LIKE to IS.
    if (fact.type == Fact.Type.LIKE && "boolean".equals(e.typeOf())) {
      fact = fact.isTruthy() ? Fact.TRUE : Fact.FALSE;
    }

    String digest = nodeDigest(e);
    Pair<Expression, Fact> oldFact = facts.get(digest);
    if (oldFact != null && !oldFact.b.isLessSpecificThan(fact)) { return; }
    putFact(e, digest, fact);

    // Infer extra facts
    if (e instanceof Operation) {
      Operation op = ((Operation) e);
      List<? extends Expression> operands = op.children();
      switch (op.getOperator()) {
        case NOT:
          // If (!x) then x is falsey.
          addFuzzyFact(operands.get(0), !fact.isTruthy());
          break;
        case LESS_THAN:
        case GREATER_THAN:
          if (fact.isTruthy()) {
            // If (a < b) the a != b, and !(a > b).
            Expression left = operands.get(0);
            Expression right = operands.get(1);
            Operator included = op.getOperator() == Operator.LESS_THAN
                ? Operator.LESS_EQUALS : Operator.GREATER_EQUALS;
            addFactInt(Operation.create(UNK, included, left, right), Fact.TRUE);
            addFactInt(
                Operation.create(UNK, Operator.STRICTLY_NOT_EQUAL, left, right),
                Fact.TRUE);
            // Incomparable values like NaN means we can conclude nothing if
            // !(a < b).
          }
          break;
        case LESS_EQUALS:
        case GREATER_EQUALS:
          if (fact.isFalsey()) {
            // if !(a <= b) then a !== b.
            Expression left = operands.get(0);
            Expression right = operands.get(1);
            addFactInt(
                Operation.create(UNK, Operator.STRICTLY_NOT_EQUAL, left, right),
                Fact.TRUE);
          }
          break;
        case INSTANCE_OF:
          // if (x instanceof y) does not throw, then y must be a function.
          // if it's true, then x must be an object.
          // Note: primitives are not instances of their wrapper class.
          addFactInt(
              Operation.create(UNK, Operator.TYPEOF, operands.get(1)),
              Fact.is(StringLiteral.valueOf(UNK, "function")));
          if (fact.isTruthy()) {
            addFactInt(operands.get(0), Fact.TRUTHY);
          }
          break;
        case EQUAL:
          addFactInt(
              Operation.create(
                  UNK, Operator.NOT_EQUAL, operands.get(0), operands.get(1)),
              fact.isTruthy() ? Fact.FALSE : Fact.TRUE);
          break;
        case NOT_EQUAL:
          addFactInt(
              Operation.create(
                  UNK, Operator.EQUAL, operands.get(0), operands.get(1)),
              fact.isTruthy() ? Fact.FALSE : Fact.TRUE);
          break;
        case STRICTLY_EQUAL:
          if (fact.isTruthy()) {
            Expression lhs = operands.get(0);
            Expression rhs = operands.get(1);
            addFactInt(Operation.create(UNK, Operator.EQUAL, lhs, rhs),
                       Fact.TRUE);
            if (rhs instanceof Literal) {
              // TODO(mikesamuel): what do we do about the fact that (0 === -0)?
              // Instead of inferring that the value IS 0, we could infer that
              // it's falsey, and the typeof is number.
              addFactInt(lhs, Fact.is((Literal) rhs));

            // (this.global === this) -> global aliases the global object.
            } else if (isThis(rhs) && lhs instanceof Reference) {
              addFactInt(lhs, Fact.GLOBAL);
            } else {
              String typeOf = rhs.typeOf();
              if (typeOf != null && lhs.typeOf() == null) {
                addFactInt(
                    Operation.create(UNK, Operator.TYPEOF, lhs),
                    Fact.is(StringLiteral.valueOf(UNK, typeOf)));
              }
              // TODO(mikesamuel): Is this useful?  When, in a comparison,
              // do we know that something is truthy or falsey, but not what
              // literal value it is.  The expressions:
              //    x === function () {}
              //    y === [1,2,3]
              //    z === {}
              // are never true, so control wouldn't reach here.
              Boolean truthiness = rhs.conditionResult();
              if (truthiness != null) {
                addFuzzyFact(lhs, truthiness);
              }
            }
          } else {
            Expression lhs = operands.get(0);
            Expression rhs = operands.get(1);
            if (ParseTreeNodes.deepEquals(lhs, rhs)) {
              addFactInt(lhs, Fact.is(new RealLiteral(UNK, Double.NaN)));
            }
          }
          addFactInt(
              Operation.create(
                  UNK, Operator.STRICTLY_NOT_EQUAL,
                  operands.get(0), operands.get(1)),
              fact.isTruthy() ? Fact.FALSE : Fact.TRUE);
          break;
        case STRICTLY_NOT_EQUAL:
          addFactInt(Operation.create(
              UNK, Operator.STRICTLY_EQUAL, operands.get(0), operands.get(1)),
              fact.isTruthy() ? Fact.FALSE : Fact.TRUE);
          break;
        case LOGICAL_AND:
        case LOGICAL_OR:
          boolean isAnd = op.getOperator() == Operator.LOGICAL_AND;
          if (fact.isTruthy() == isAnd) {
            addFuzzyFact(operands.get(0), isAnd);
            addFactInt(operands.get(1), fact);  // Second value is result
          }
          break;
        case MEMBER_ACCESS:
        case SQUARE_BRACKET:
          // If foo.bar is truthy, then so is foo.
          if (fact.isTruthy()) {
            addFuzzyFact(operands.get(0), true);
          }
          break;
        case TYPEOF:
          if (fact.type == Fact.Type.IS
              && fact.value instanceof StringLiteral) {
            String s = ((StringLiteral) fact.value).getUnquotedValue();
            Expression op0 = operands.get(0);
            if ("undefined".equals(s)) {
              addFactInt(op0, Fact.UNDEFINED);
            } else {
              if ("function".equals(s)) { addFactInt(op0, Fact.TRUTHY); }
              // undefined is a commonly tested value, so infer its absence
              // for other types.
              addFactInt(
                  Operation.create(
                      UNK, Operator.STRICTLY_EQUAL, op0, Fact.UNDEFINED.value),
                  Fact.FALSE);
            }
          }
          break;
        default:
          break;
      }
      // (a < b) -> (b > a) since we know (a) and (b) are pure
      if (operands.size() == 2) {
        Operator swapped = WITH_REVERSE_ORDER.get(op.getOperator());
        if (swapped != null) {
          addFactInt(
              Operation.create(
                  op.getFilePosition(), swapped,
                  operands.get(1), operands.get(0)),
              fact);
        }
      }
    }
  }

  /** Infer facts from the combination of two or more facts. */
  void finishInference() {
    if (!needsInference) { return; }
    Set<String> globals = Sets.newHashSet();
    globals.add("this");
    List<Pair<Expression, Fact>> factList = Lists.newArrayList(facts.values());
    for (Pair<Expression, Fact> fe : factList) {
      if (fe.b == Fact.GLOBAL) {
        globals.add(((Reference) fe.a).getIdentifierName());
      }
    }
    if (!globals.isEmpty()) {
      for (Pair<Expression, Fact> fe : factList) {
        if (fe.b == Fact.GLOBAL) { continue; }
        Expression e = fe.a;
        Operator op = null;
        if (Operation.is(e, Operator.TYPEOF)) {
          op = Operator.TYPEOF;
          e = (Expression) e.children().get(0);
        }
        String topRef = topRef(e);
        if (topRef != null) {
          if (!globals.contains(topRef)) {
            for (String globalAlias : globals) {
              Expression newExpr = withTopRef(e, globalAlias);
              if (op != null) {
                newExpr = Operation.create(UNK, op, newExpr);
              }
              addFactInt(newExpr, fe.b);
            }
          } else if (op == null && e instanceof Operation) {
            // Simplify the fact unless it is falsey and not false.
            // E.g. global.foo   IS   4
            //   -> foo          IS   4
            // but  global.foo   IS   undefined
            //   !> foo          IS   undefined
            // because foo could result in an Error.
            if (fe.b.isFalse() || fe.b.isTruthy()) {
              Expression newExpr = withoutTopRef(e);
              addFactInt(newExpr, fe.b);
            }
          }
        }
      }
    }
    needsInference = false;
  }

  private void addFuzzyFact(Expression e, boolean isTruthy) {
    addFactInt(e, isTruthy ? Fact.TRUTHY : Fact.FALSEY);
  }

  private static final EnumMap<Operator, Operator> WITH_REVERSE_ORDER
      = new EnumMap<Operator, Operator>(Operator.class);
  static {
    for (Operator op : new Operator[] {
            // Most mathematical operators are order-dependent besides
            // the bitwise ones, addition, and multiplication, but in JS,
            // addition for strings is order-dependent.
            Operator.BITWISE_AND, Operator.BITWISE_OR,
            Operator.BITWISE_XOR, Operator.EQUAL,
            Operator.MULTIPLICATION, Operator.NOT_EQUAL,
            Operator.STRICTLY_EQUAL, Operator.STRICTLY_NOT_EQUAL }) {
      WITH_REVERSE_ORDER.put(op, op);
    }
    WITH_REVERSE_ORDER.put(Operator.GREATER_EQUALS, Operator.LESS_EQUALS);
    WITH_REVERSE_ORDER.put(Operator.LESS_EQUALS, Operator.GREATER_EQUALS);
    WITH_REVERSE_ORDER.put(Operator.GREATER_THAN, Operator.LESS_THAN);
    WITH_REVERSE_ORDER.put(Operator.LESS_THAN, Operator.GREATER_THAN);
  }

  /**
   * @param e available to subclasses.
   */
  protected void putFact(Expression e, String digest, Fact fact) {
    if (digest.length() > longestKeyLength) {
      longestKeyLength = digest.length();
    }
    facts.put(digest, Pair.pair((Expression) e.clone(), fact));
    needsInference = true;
  }

  /** Holds the result of optimizing a single node, and its digest. */
  private static class Result {
    String digest;
    ParseTreeNode node;
  }

  private void optimize(
      Scope s, ParseTreeNode node, boolean isFuzzy, boolean isLhs,
      boolean throwsOnUndefined, boolean isFn, Result out) {
    if (node instanceof Conditional) {
      // Handle conditionals specially since the goal of this code is to cut
      // bits out of them.
      optimizeConditional(s, (Conditional) node, 0, out);
      return;
    } else if (node instanceof Operation) {
      Operator op = ((Operation) node).getOperator();
      if (Operator.MEMBER_ACCESS == op) {
        // The reference that is the second argument should not be treated as
        // an optimization target.
        optimizeMemberAccess(
            s, (Operation) node, isFuzzy, isLhs, throwsOnUndefined, out);
        return;
      }
    }

    // Stores the digest as it is built.
    StringBuilder sb = new StringBuilder();
    sb.append('(');

    if (node instanceof Reference) {
      // Set the digest to null if node is a non-global reference, so that
      // this node and ancestors cannot possibly match any facts in the
      // knowledge base.
      if (!s.isOuter(((Reference) node).getIdentifierName())) { sb = null; }
    } else if (node instanceof FunctionConstructor) {
      s = Scope.fromFunctionConstructor(s, (FunctionConstructor) node);
    } else if (node instanceof CatchStmt) {
      s = Scope.fromCatchStmt(s, (CatchStmt) node);
    }

    List<? extends ParseTreeNode> children = node.children();
    int n = children.size();
    if (n != 0) {
      // The number of operands for which we only care about the truthiness of
      // the result.
      int fuzzyLimit = 0;
      // The number of operands that are left hand side expressions.
      int lhsLimit = 0;
      // The number of operands that will cause the exception to fail with
      // an error if undefined.
      int touLimit = 0;
      // The number of operands that are functions.
      int fnLimit = 0;

      if (node instanceof Operation) {
        Operator op = ((Operation) node).getOperator();
        switch (op) {
          case LOGICAL_OR: case LOGICAL_AND:
            fuzzyLimit = isFuzzy ? 2 : 0;
            break;
          case TERNARY:
            fuzzyLimit = isFuzzy ? 3 : 1;
            break;
          case NOT:
            fuzzyLimit = 1;
            break;
          case FUNCTION_CALL:
            fnLimit = 1;
            // $FALL-THROUGH$
          case MEMBER_ACCESS: case CONSTRUCTOR:
          case SQUARE_BRACKET:
            touLimit = 1;
            break;
          default: break;
        }
        if (op.getCategory() == OperatorCategory.ASSIGNMENT) {
          // Don't optimize left-hand sides of assignments.  We allow code to
          // modify the environment after its sampled, as long as nothing is
          // deleted or replaced with a less functional replacement.
          lhsLimit = 1;
        }
      }

      // Build a replacement, making sure to return the original if
      // no-optimization was done, so that we can easily tell whether or not we
      // need to do another optimization pass.
      ParseTreeNode[] newChildren = null;
      for (int i = 0; i < n; ++i) {
        ParseTreeNode child = children.get(i);
        optimize(
            s, child, i < fuzzyLimit, i < lhsLimit, i < touLimit,
            i < fnLimit, out);
        sb = addDigest(out.digest, sb);
        if (out.node != child) {
          if (newChildren == null) {
            newChildren = children.toArray(new ParseTreeNode[n]);
          }
          newChildren[i] = out.node;
        }
      }
      if (newChildren != null) {
        node = ParseTreeNodes.newNodeInstance(
            node.getClass(), node.getFilePosition(), node.getValue(),
            Arrays.asList(newChildren));
      }
    }

    String digest;
    if (sb != null) {
      nodeTail(node, sb);

      digest = sb.toString();
      if (node instanceof Expression && !isLhs) {
        Fact f = getFact(digest);
        if (f == null) { f = foldComparisonToFalsey(node); }
        if (f != null) {
          if (f.isSubstitutable(isFuzzy)) {
            node = f.value.clone();
            digest = optNodeDigest(node);
          }
        }
      }
    } else {
      digest = null;
    }

    if (node instanceof Expression) {
      Expression folded = normNum(((Expression) node).fold(isFn));
      if (folded != node) {
        node = folded;
        digest = optNodeDigest(folded);
      }
    }
    out.node = node;
    out.digest = digest;
  }

  private void optimizeConditional(Scope s, Conditional c, int i, Result out) {
    List<? extends ParseTreeNode> children = c.children();
    int n = children.size();
    StringBuilder sb = new StringBuilder();
    sb.append('(');
    int nEmitted = i;
    List<ParseTreeNode> newChildren = null;
    if (i != 0) {
      newChildren = Lists.newArrayList(n);
    }
    while (i < n) {
      ParseTreeNode child = children.get(i);
      optimize(s, child, true, false, false, false, out);
      ParseTreeNode newChild = out.node;
      String newDigest = out.digest;
      Boolean optCond = (i & 1) == 0 && i + 1 < n
          ? ((Expression) newChild).conditionResult() : null;
      if (optCond != null || child != newChild) {
        if (newChildren == null) { newChildren = Lists.newArrayList(n); }
        newChildren.addAll(children.subList(nEmitted, i));
        if (optCond != null) {  // The condition is known, so we can remove it.
          Expression sideEffect = ((Expression) newChild)
              .simplifyForSideEffect();
          if (sideEffect == null) {
            if (!optCond) {
              nEmitted = i = i + 2;  // Skip the condition and its clause
              // if (false) { foo } else if (...) bar  =>  if (...) bar
              continue;
            } else {
              nEmitted = i + 1;
              n = i + 2;
              // Drop the condition, and treat its clause as
              // the final else clause.
              // if (true) { ... }  =>  { ... }
            }
          } else {
            if (optCond) {
              // if (foo() || true) { ... }  =>  { foo(); ... }
              optimize(s, children.get(i + 1), false, false, false, false, out);
              // if (foo() && 0) { ... } else { baz(); }  =>  { foo(); baz(); }
            } else {
              optimizeConditional(s, c, i + 2, out);
            }
            List<Statement> stmts = Lists.newArrayList();
            stmts.add(new ExpressionStmt(sideEffect));
            if (out.node instanceof Block) {
              stmts.addAll(((Block) out.node).children());
            } else if (!(out.node instanceof Noop)) {
              stmts.add((Statement) out.node);
            }
            if (!stmts.isEmpty()) {
              newChildren.add(
                  stmts.size() == 1 ? stmts.get(0) : new Block(UNK, stmts));
            }
            sb = addDigest(newDigest, sb);
            sb = addDigest(out.digest, sb);
            n = nEmitted = i + 1;
            break;
          }
        } else {
          newChildren.add(newChild);
          nEmitted = i + 1;
        }
      }
      sb = addDigest(newDigest, sb);
      ++i;
    }
    if (sb != null) {
      nodeTail(c, sb);
    }
    if (newChildren != null) {
      if (nEmitted < n) {
        newChildren.addAll(children.subList(nEmitted, n));
      }
      if (newChildren.size() < 2) {
        out.node = newChildren.isEmpty()
            ? new Noop(UNK) : (Statement) newChildren.get(0);
        out.digest = optNodeDigest(out.node);
      } else {
        out.node = new Conditional(UNK, null, newChildren);
        out.digest = sb != null ? sb.toString() : null;
      }
    } else {
      out.node = c;
      out.digest = sb != null ? sb.toString() : null;
    }
  }

  private void optimizeMemberAccess(
      Scope s, Operation ma, boolean isFuzzy, boolean isLhs,
      boolean throwsOnUndefined, Result out) {
    StringBuilder sb = new StringBuilder();
    sb.append('(');
    Expression obj = ma.children().get(0);
    optimize(s, obj, false, false, false, false, out);
    Reference prop = (Reference) ma.children().get(1);
    if (out.node != obj) {
      ma = Operation.createInfix(
          Operator.MEMBER_ACCESS, (Expression) out.node, prop);
    }
    sb = addDigest(out.digest, sb);
    int objDigestEnd = sb != null ? sb.length() : -1;
    if (sb != null) {
      nodeDigest(ma.children().get(1), sb);
      nodeTail(ma, sb);
    }
    String digest = sb != null ? sb.toString() : null;
    out.node = ma;
    out.digest = digest;
    if (digest != null) {
      if (!isLhs) {
        Fact f = getFact(digest);
        if (f != null && f.isSubstitutable(isFuzzy)) {
          out.node = f.value.clone();
          out.digest = nodeDigest(out.node);
          return;
        }
      }
      // window.addEventListener -> addEventListener
      String objDigest = digest.substring(1, objDigestEnd);
      Pair<Expression, Fact> objFe = facts.get(objDigest);
      if (objFe != null && objFe.b.isGlobal()
          && s.isOuter(prop.getIdentifierName())) {
        String propDigest = nodeDigest(prop);
        boolean canSimplify = false;
        if (isLhs || throwsOnUndefined) {
          // If it's being set, we don't need to worry about undefined global
          // errors, and we don't need to worry if the containing expression
          // would throw if it were undefined anyway.
          canSimplify = true;
        } else {
          // No difference between foo and global.foo because foo is
          // not undefined (truthy or (falsey and not undefined).
          Pair<Expression, Fact> propFe = facts.get(propDigest);
          if (propFe != null) {
            Fact pf = propFe.b;
            canSimplify = pf.isTruthy()
                || (pf.type == Fact.Type.IS && !pf.isUndefined());
          }
        }
        if (canSimplify) {
          out.node = prop;
          out.digest = propDigest;
        }
      }
    }
  }

  private StringBuilder addDigest(String digest, StringBuilder out) {
    // We poison the digest iff the output is getting longer than the digest
    // of the longest fact to avoid computing long keys for subtrees that can't
    // possibly match any fact, and to handle local variable references.
    // We should *never* optimize any expression containing a local variable
    // reference, so we set the digest to null whenever we see a local variable
    // reference which nulls the digest for all ancestor parse trees.
    if (digest != null && out != null
        && out.length() + digest.length() <= longestKeyLength) {
      return out.append(digest);
    } else {
      return null;
    }
  }

  /**
   * A node digest is a string that describes a JS parse tree, but which,
   * unlike the rendered form, can be efficiently composed from the
   * digests of the children.
   * <p>
   * This allows us to efficiently walk a tree finding structural matches
   * without incurring an O(n**2) overhead of repeatedly rendering parse trees.
   * <p>
   * Node digests cannot be serialized.
   * <p>
   * The digest consists of an open parenthesis, followed by the digests of any
   * children, followed by a key which identifies the class, and finally
   * followed by a string form of the value finished by a close parenthesis.
   */
  private static String nodeDigest(ParseTreeNode node) {
    StringBuilder sb = new StringBuilder();
    nodeDigest(node, sb);
    return sb.toString();
  }

  /**
   * Returns the digest of node, or null if the digest would be longer than
   * the longest key in the knowledge base, and thus unable to match any key
   * in the knowledge base.
   */
  private String optNodeDigest(ParseTreeNode node) {
    StringBuilder sb = new StringBuilder();
    sb.append('(');
    for (ParseTreeNode child : node.children()) {
      nodeDigest(child, sb);
      if (sb.length() > longestKeyLength) { return null; }
    }
    nodeTail(node, sb);
    if (sb.length() > longestKeyLength) { return null; }
    return sb.toString();
  }

  private Fact foldComparisonToFalsey(ParseTreeNode n) {
    if (!(n instanceof Operation)) { return null; }
    Operation op = (Operation) n;
    Operator o = op.getOperator();
    boolean eq;
    boolean strict;
    switch (o) {
      case EQUAL: case STRICTLY_EQUAL: eq = true; break;
      case NOT_EQUAL: case STRICTLY_NOT_EQUAL: eq = false; break;
      default: return null;
    }
    strict = o == Operator.STRICTLY_EQUAL || o == Operator.STRICTLY_NOT_EQUAL;
    List<? extends Expression> operands = op.children();
    Expression a = operands.get(0);
    Expression b = operands.get(1);
    if (strict ? isUndefOrLiteral(a) : isNullOrUndef(a)) {
      // continue to check
    } else if (strict ? isUndefOrLiteral(b) : isNullOrUndef(b)) {
      Expression t = a;
      a = b;
      b = t;
    } else {
      return null;
    }
    Pair<Expression, Fact> fe = facts.get(this.optNodeDigest(b));
    if (fe == null) { return null; }
    Boolean bool = a.conditionResult();
    if (bool == null || bool.booleanValue() == fe.b.isTruthy()) { return null; }
    return eq ? Fact.FALSE : Fact.TRUE;
  }

  private static boolean isUndefOrLiteral(Expression e) {
    if (e instanceof Literal) { return true; }
    return Operation.is(e, Operator.VOID) && e.simplifyForSideEffect() == null;
  }

  private static boolean isNullOrUndef(Expression e) {
    if (e instanceof NullLiteral) { return true; }
    return Operation.is(e, Operator.VOID) && e.simplifyForSideEffect() == null;
  }

  private static void nodeDigest(ParseTreeNode n, StringBuilder out) {
    out.append('(');
    for (ParseTreeNode child : n.children()) {
      nodeDigest(child, out);
    }
    nodeTail(n, out);
  }

  /** Maps node classes to short prefixes. */
  private static Map<Class<?>, String> CLASS_NAME_DIGEST_IDX
      = Collections.synchronizedMap(Maps.<Class<?>, String>newHashMap());
  private static void nodeTail(ParseTreeNode n, StringBuilder out) {
    Class<?> clazz = n.getClass();
    String idx;
    synchronized (CLASS_NAME_DIGEST_IDX) {
      idx = CLASS_NAME_DIGEST_IDX.get(clazz);
      if (idx == null) {
        idx = CLASS_NAME_DIGEST_IDX.size() + ":";
        CLASS_NAME_DIGEST_IDX.put(clazz, idx);
      }
    }
    out.append(idx);
    Object value = n instanceof StringLiteral
        ? ((StringLiteral) n).getValue() : n.getValue();
    if (value != null) {
      int start = out.length();
      out.append(n.getValue());
      while (start < out.length()) {
        char ch = out.charAt(start);
        if (ch == '(' || ch == ')' || ch == '*') {
          out.insert(start, '*');
          ++start;
        }
        ++start;
      }
    }
    out.append(')');
  }

  private static boolean isThis(ParseTreeNode node) {
    if (!(node instanceof Reference)) { return false; }
    return "this".equals(((Reference) node).getIdentifierName());
  }

  private static String topRef(Expression e) {
    while (true) {
      if (e instanceof Reference) { return ((Reference) e).getIdentifierName(); }
      if (!(e instanceof Operation)) { return null; }
      Operation op = (Operation) e;
      switch (op.getOperator()) {
        case MEMBER_ACCESS: case SQUARE_BRACKET:
          e = op.children().get(0);
          break;
        default: return null;
      }
    }
  }

  private static Expression withTopRef(Expression e, String lhs) {
    if (e instanceof Reference) {
      return Operation.createInfix(
          Operator.MEMBER_ACCESS, new Reference(new Identifier(UNK, lhs)), e);
    } else {
      Operation op = (Operation) e;
      List<? extends Expression> operands = op.children();
      return Operation.create(
          e.getFilePosition(), op.getOperator(),
          withTopRef(operands.get(0), lhs), operands.get(1));
    }
  }

  private static Expression withoutTopRef(Expression e) {
    Operation op = (Operation) e;
    List<? extends Expression> operands = op.children();
    Expression obj = operands.get(0), prop = operands.get(1);
    if (obj instanceof Reference) { return prop; }
    return Operation.create(
        e.getFilePosition(), op.getOperator(), withoutTopRef(obj), prop);
  }

  private static Expression rfold(Expression e, boolean isFn) {
    if (e instanceof Operation) {
      Operation o = (Operation) e;
      List<? extends Expression> children = o.children();
      Expression[] newChildren = null;
      boolean oIsFn = o.getOperator() == Operator.FUNCTION_CALL;
      for (int i = 0, n = children.size(); i < n; ++i) {
        Expression operand = children.get(i);
        Expression newOperand = rfold(operand, oIsFn && i == 0);
        if (operand == newOperand) { continue; }
        if (newChildren == null) {
          newChildren = children.toArray(new Expression[n]);
        }
        newChildren[i] = newOperand;
      }
      if (newChildren != null) {
        e = Operation.create(e.getFilePosition(), o.getOperator(), newChildren);
      }
    }
    return normNum(e.fold(isFn));
  }

  private static Expression normNum(Expression e) {
    if (!(e instanceof RealLiteral)) { return e; }
    RealLiteral rl = (RealLiteral) e;
    Number n = rl.getValue();
    double dv = n.doubleValue();
    long lv = n.longValue();
    // Convert 1.0 to 1, but do not convert -0.0 to 0.
    if (dv == lv && (dv != 0d || (1 / dv) == Double.POSITIVE_INFINITY)) {
      return new IntegerLiteral(rl.getFilePosition(), lv);
    }
    return rl;
  }
}
