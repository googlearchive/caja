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
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.OperatorCategory;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.Scope;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * An optimizer which uses facts about the environment in which the program
 * will be run to eliminate dead branches.
 *
 * @author mikesamuel@gmail.com
 */
public class ParseTreeKB {
  private final Map<String, Fact> facts = Maps.newHashMap();
  private int longestKeyLength = 0;

  private static final FilePosition UNK = FilePosition.UNKNOWN;

  public ParseTreeKB() {
    addFact(new Reference(new Identifier(UNK, "undefined")), Fact.UNDEFINED);
  }

  /**
   * @param e an expression containing no non-global references.
   */
  public Fact getFact(Expression e) {
    return facts.get(optNodeDigest(e));
  }

  /**
   * Produces a similar parse tree
   *
   * @param js not mutated.
   * @param mq receives scoping warnings.
   * @return js if no optimizations were performed.  Otherwise a partial clone.
   */
  public Block optimize(Block js, MessageQueue mq) {
    while (true) {
      Result out = new Result();
      Scope s = Scope.fromProgram(js, mq);
      optimize(s, js, false, false, out);
      Block optimized = ConstLocalOptimization.optimize((Block) out.node, mq);
      if (optimized == js) { return optimized; }
      js = optimized;
    }
  }

  /**
   * Adds a fact about the environment to the knowledge base, and adds facts
   * easily inferred from the input fact.
   */
  public void addFact(Expression e, Fact fact) {
    // If we know it is a boolean, we can upgrade from LIKE to IS.
    if (fact.type == Fact.Type.LIKE && "boolean".equals(e.typeOf())) {
      fact = fact.isTruthy() ? Fact.TRUE : Fact.FALSE;
    }

    String digest = nodeDigest(e);
    Fact oldFact = facts.get(digest);
    if (oldFact != null && !oldFact.isLessSpecificThan(fact)) { return; }
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
            Operator inclusive = op.getOperator() == Operator.LESS_THAN
                ? Operator.LESS_EQUALS : Operator.GREATER_EQUALS;
            addFact(Operation.create(UNK, inclusive, left, right), Fact.TRUE);
            addFact(
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
            addFact(
                Operation.create(UNK, Operator.STRICTLY_NOT_EQUAL, left, right),
                Fact.TRUE);
          }
          break;
        case INSTANCE_OF:
          // if (x instanceof y) does not throw, then y must be a function.
          // if it's true, then x must be an object.
          // Note: primitives are not instances of their wrapper class.
          addFact(
              Operation.create(UNK, Operator.TYPEOF, operands.get(1)),
              Fact.is(StringLiteral.valueOf(UNK, "function")));
          if (fact.isTruthy()) {
            addFact(operands.get(0), Fact.TRUTHY);
          }
          break;
        case EQUAL:
          addFact(
              Operation.create(
                  UNK, Operator.NOT_EQUAL, operands.get(0), operands.get(1)),
              fact.isTruthy() ? Fact.FALSE : Fact.TRUE);
          break;
        case NOT_EQUAL:
          addFact(
              Operation.create(
                  UNK, Operator.EQUAL, operands.get(0), operands.get(1)),
              fact.isTruthy() ? Fact.FALSE : Fact.TRUE);
          break;
        case STRICTLY_EQUAL:
          if (fact.isTruthy()) {
            Expression lhs = operands.get(0);
            Expression rhs = operands.get(1);
            addFact(Operation.create(UNK, Operator.EQUAL, lhs, rhs), Fact.TRUE);
            if (rhs instanceof Literal) {
              addFact(lhs, Fact.is((Literal) rhs));
            } else {
              String typeOf = rhs.typeOf();
              if (typeOf != null && lhs.typeOf() == null) {
                addFact(
                    Operation.create(UNK, Operator.TYPEOF, lhs),
                    Fact.is(StringLiteral.valueOf(UNK, typeOf)));
              }
              Boolean truthiness = rhs.conditionResult();
              if (truthiness != null) {
                addFuzzyFact(lhs, truthiness);
              }
            }
          }
          addFact(
              Operation.create(
                  UNK, Operator.STRICTLY_NOT_EQUAL,
                  operands.get(0), operands.get(1)),
              fact.isTruthy() ? Fact.FALSE : Fact.TRUE);
          break;
        case STRICTLY_NOT_EQUAL:
          addFact(Operation.create(
              UNK, Operator.STRICTLY_EQUAL, operands.get(0), operands.get(1)),
              fact.isTruthy() ? Fact.FALSE : Fact.TRUE);
          break;
        case LOGICAL_AND:
        case LOGICAL_OR:
          boolean isAnd = op.getOperator() == Operator.LOGICAL_AND;
          if (fact.isTruthy() == isAnd) {
            addFuzzyFact(operands.get(0), isAnd);
            addFact(operands.get(1), fact);  // Second value is result
          }
          break;
        case TYPEOF:
          if (fact.type == Fact.Type.IS
              && fact.value instanceof StringLiteral) {
            String s = ((StringLiteral) fact.value).getUnquotedValue();
            Expression op0 = operands.get(0);
            if ("undefined".equals(s)) {
              addFact(op0, Fact.UNDEFINED);
            } else {
              if ("function".equals(s)) { addFact(op0, Fact.TRUTHY); }
              // undefined is a commonly tested value, so infer its absence
              // for other types.
              addFact(
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
          addFact(
              Operation.create(
                  op.getFilePosition(), swapped,
                  operands.get(1), operands.get(0)),
              fact);
        }
      }
    }
  }

  private void addFuzzyFact(Expression e, boolean isTruthy) {
    addFact(e, isTruthy ? Fact.TRUTHY : Fact.FALSEY);
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
    facts.put(digest, fact);
  }

  /** Holds the result of optimizing a single node, and its digest. */
  private static class Result {
    String digest;
    ParseTreeNode node;
  }

  private void optimize(
      Scope s, ParseTreeNode node, boolean isFuzzy, boolean isLhs, Result out) {
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
        optimizeMemberAccess(s, (Operation) node, isFuzzy, isLhs, out);
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
        optimize(s, child, i < fuzzyLimit, i < lhsLimit, out);
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
        Fact f = facts.get(digest);
        if (f != null && (isFuzzy || f.type == Fact.Type.IS)) {
          node = f.value.clone();
          digest = optNodeDigest(f.value);
        }
      }
    } else {
      digest = null;
    }

    if (node instanceof Expression) {
      Expression folded = ((Expression) node).fold();
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
      optimize(s, child, true, false, out);
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
              optimize(s, children.get(i + 1), false, false, out);
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
      Scope s, Operation ma, boolean isFuzzy, boolean isLhs, Result out) {
    StringBuilder sb = new StringBuilder();
    sb.append('(');
    Expression obj = ma.children().get(0);
    Reference prop = (Reference) ma.children().get(1);
    optimize(s, obj, false, false, out);
    if (out.node != obj) {
      ma = Operation.createInfix(
          Operator.MEMBER_ACCESS, (Expression) out.node, prop);
    }
    sb = addDigest(out.digest, sb);
    if (sb != null) {
      nodeDigest(ma.children().get(1), sb);
      nodeTail(ma, sb);
    }
    String digest = sb != null ? sb.toString() : null;
    out.node = ma;
    out.digest = digest;
    if (digest != null && !isLhs) {
      Fact f = facts.get(digest);
      if (f != null && (isFuzzy || f.type == Fact.Type.IS)) {
        out.node = f.value.clone();
        out.digest = nodeDigest(out.node);
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
}
