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
import com.google.caja.parser.js.BooleanLiteral;
import com.google.caja.parser.js.BreakStmt;
import com.google.caja.parser.js.CaseStmt;
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.Conditional;
import com.google.caja.parser.js.ContinueStmt;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.DefaultCaseStmt;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FinallyStmt;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.LabeledStatement;
import com.google.caja.parser.js.LabeledStmtWrapper;
import com.google.caja.parser.js.MultiDeclaration;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.OperatorCategory;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.ReturnStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.SwitchCase;
import com.google.caja.parser.js.SwitchStmt;
import com.google.caja.parser.js.ThrowStmt;
import com.google.caja.parser.js.TryStmt;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;
import com.google.caja.util.SafeIdentifierMaker;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Removes unnecessary blocks from a program to allow for smaller rendered
 * output.
 *
 * @author mikesamuel@gmail.com
 */
public class StatementSimplifier {
  /**
   * @param mq receives warnings about problems such as breaks to undefined
   *   labels.
   * @return the input if no changes were made.  Otherwise, a partial clone.
   */
  public static ParseTreeNode optimize(ParseTreeNode n, MessageQueue mq) {
    return new StatementSimplifier(mq).optimize(n, false);
  }

  private final MessageQueue mq;
  private final Map<String, StmtLabel> labels = Maps.newHashMap();
  private String blankLabel = "";
  private final SafeIdentifierMaker labelGenerator = new SafeIdentifierMaker();

  private StatementSimplifier(MessageQueue mq) { this.mq = mq; }

  private ParseTreeNode optimize(ParseTreeNode n, boolean needsBlock) {
    if (n instanceof LabeledStatement) {
      LabeledStatement ls = (LabeledStatement) n;
      String label = ls.getLabel();
      if (!(label == null || "".equals(label))) {
        StmtLabel oldSl = labels.get(label);
        String oldBlankLabel = blankLabel;
        StmtLabel sl = new StmtLabel(labelGenerator.next());
        labels.put(label, sl);
        blankLabel = label;
        boolean wrap = ls instanceof LabeledStmtWrapper;
        Statement unlabeled = wrap ? ((LabeledStmtWrapper) ls).getBody() : ls;
        Statement opt = (Statement) optimizeUnlabeled(unlabeled, needsBlock);
        if (oldSl == null) {
          labels.remove(label);
        } else {
          labels.put(label, oldSl);
        }
        blankLabel = oldBlankLabel;
        if (sl.nUses == 0) {
          if (!wrap && opt instanceof LabeledStatement
              && !"".equals(((LabeledStatement) opt).getLabel())) {
            return ParseTreeNodes.newNodeInstance(
                opt.getClass(), opt.getFilePosition(), "", opt.children());
          }
        } else {
          if (!wrap && opt instanceof LabeledStatement) {
            return ParseTreeNodes.newNodeInstance(
                opt.getClass(), opt.getFilePosition(), sl.newName,
                opt.children());
          } else {
            if (opt instanceof LabeledStatement) {
              // Add a block so we don't get ambiguity
              opt = new Block(opt.getFilePosition(), Arrays.asList(opt));
            }
            return new LabeledStmtWrapper(n.getFilePosition(), sl.newName, opt);
          }
        }
        return opt;
      } else {
        String oldBlankLabel = blankLabel;
        blankLabel = "";
        Statement opt = (Statement) optimizeUnlabeled(n, needsBlock);
        blankLabel = oldBlankLabel;
        return opt;
      }
    } else if (n instanceof BreakStmt || n instanceof ContinueStmt) {
      String lbl = (String) n.getValue();
      if (!(lbl == null || "".equals(lbl))) {
        String newName;
        if (blankLabel.equals(lbl)) {
          newName = "";
        } else {
          StmtLabel renamed = labels.get(lbl);
          if (renamed != null) {
            newName = renamed.newName;
            ++renamed.nUses;
          } else {
            // This will get triggered improperly if we ever optimize a subtree
            // twice.  Optimizing the entire tree twice is fine, but not a part.
            newName = "";
            mq.addMessage(
                MessageType.UNDEFINED_SYMBOL, MessageLevel.ERROR,
                n.getFilePosition(), MessagePart.Factory.valueOf(lbl));
          }
        }
        return ParseTreeNodes.newNodeInstance(
            n.getClass(), n.getFilePosition(), newName, n.children());
      }
    }
    return optimizeUnlabeled(n, needsBlock);
  }
  private ParseTreeNode optimizeUnlabeled(ParseTreeNode n, boolean needsBlock) {
    if (n instanceof Block) {
      List<? extends Statement> children = ((Block) n).children();
      int nChildren = children.size();
      List<Statement> flattened = flattenBlocksAndIgnoreNoops(children);
      List<Statement> joined = joinAdjacentExprs(
          flattened == null ? Lists.newArrayList(children) : flattened);
      List<Statement> newChildren = joined != null ? joined : flattened;
      if (newChildren != null) { nChildren = newChildren.size(); }
      if (!needsBlock) {
        switch (nChildren) {
          case 0: return new Noop(n.getFilePosition());
          case 1: return (newChildren == null ? children : newChildren).get(0);
        }
      }
      return newChildren != null
          ? new Block(n.getFilePosition(), newChildren) : n;
    } else if (n instanceof SwitchStmt) {
      return optimizeSwitch((SwitchStmt) n);
    } else if (n instanceof ReturnStmt) {
      ReturnStmt rs = (ReturnStmt) n;
      Expression returnValue = rs.getReturnValue();
      Expression optReturnValue = returnValue != null
          ? (Expression) optimize(returnValue, false)
          : null;
      if (optReturnValue != null && "undefined".equals(returnValue.typeOf())
          && optReturnValue.simplifyForSideEffect() == null) {
        return new ReturnStmt(rs.getFilePosition(), null);
      } else if (optReturnValue != returnValue) {
        return new ReturnStmt(rs.getFilePosition(), optReturnValue);
      }
      return rs;
    } else {
      List<? extends ParseTreeNode> children = n.children();
      int nChildren = children.size();
      List<ParseTreeNode> newChildren = null;
      boolean childNeedsBlock = (
          n instanceof FunctionConstructor || n instanceof TryStmt
          || n instanceof CatchStmt || n instanceof FinallyStmt);
      for (int i = 0; i < nChildren; ++i) {
        ParseTreeNode child = children.get(i);
        ParseTreeNode newChild = optimize(child, childNeedsBlock);
        if (child != newChild) {
          if (newChildren == null) {
            newChildren = Lists.newArrayList(nChildren);
          }
          newChildren.addAll(children.subList(newChildren.size(), i));
          newChildren.add(newChild);
        }
      }
      if (newChildren != null) {
        newChildren.addAll(children.subList(newChildren.size(), nChildren));
      }
      List<? extends ParseTreeNode> outChildren = newChildren == null
          ? children : newChildren;
      if (n instanceof ExpressionStmt) {
        Expression e = (Expression) outChildren.get(0);
        Expression simple = e.simplifyForSideEffect();
        if (simple == null) { return new Noop(n.getFilePosition()); }
        if (simple != e) {
          newChildren = Collections.<ParseTreeNode>singletonList(simple);
        }
      } else if (n instanceof Conditional) {
        List<ParseTreeNode> condParts = newChildren != null
            ? newChildren : Lists.newArrayList(children);
        Statement optCond = optimizeConditional(n.getFilePosition(), condParts);
        if (optCond != null) { return optCond; }
        int nCondParts = condParts.size();
        if ((nCondParts & 1) == 1) {  // There is an else clause
          // Remove a useless else clause.
          if (condParts.get(nCondParts - 1) instanceof Noop) {
            if (newChildren == null) {
              condParts = newChildren = Lists.newArrayList(condParts);
            }
            condParts.remove(--nCondParts);
          }
        }
        // If there is an else clause, and every other clause exits, then get
        // rid of the else.
        if ((nCondParts & 1) == 1) {  // There is an else clause
          boolean allExit = true;
          for (int i = 1; i < nCondParts; i += 2) {
            if (!exits(condParts.get(i))) {
              allExit = false;
              break;
            }
          }
          if (allExit) {
            //    if (foo) return bar; else baz();
            // => if (foo) return bar; baz();
            return combine(
                n.getFilePosition(),
                new Conditional(
                    FilePosition.span(
                        n.getFilePosition(),
                        condParts.get(nCondParts - 1).getFilePosition()),
                null, condParts.subList(0, nCondParts - 1)),
                (Statement) condParts.get(nCondParts - 1));
          }
        }
      }
      if (newChildren != null) {
        n = ParseTreeNodes.newNodeInstance(
            n.getClass(), n.getFilePosition(), n.getValue(), newChildren);
      }
      return n instanceof Expression ? ((Expression) n).fold(false) : n;
    }
  }

  /** <code>{ a; { b; c; } ; ; d }</code>  ->  <code>{ a; b; c; d; }</code> */
  private List<Statement> flattenBlocksAndIgnoreNoops(
      List< ? extends Statement> stmts) {
    int nStmts = stmts.size();
    List<Statement> newStmts = null;
    int pos = 0;  // Position of last stmt processed onto newStmts
    for (int i = 0; i < nStmts; ++i) {
      ParseTreeNode s = stmts.get(i);
      ParseTreeNode optS = optimize(s, false);
      if (s != optS || optS instanceof Noop || optS instanceof Block) {
        if (newStmts == null) { newStmts = Lists.newArrayList(nStmts); }
        newStmts.addAll(stmts.subList(pos, i));
        if (optS instanceof Block) {
          newStmts.addAll(((Block) optS).children());
        } else if (!(optS instanceof Noop)) {
          newStmts.add((Statement) optS);
        }
        pos = i + 1;
      }
    }
    if (newStmts != null) { newStmts.addAll(stmts.subList(pos, nStmts)); }
    // Eliminate dead code.
    // E.g. uses of breaks in switch clauses as in
    //   case 1:
    //     return x;
    //     break;
    {
      List<? extends ParseTreeNode> blockStmts = newStmts != null
          ? newStmts : stmts;
      for (int i = 0, last = blockStmts.size() - 1; i < last; ++i) {
        if (exits(blockStmts.get(i))) {
          // We need to preserve declarations following since they are
          // hoisted.
          boolean hasNonDecls = false;
          for (int j = i + 1; j <= last; ++j) {
            if (!(blockStmts.get(j) instanceof Declaration
                  || blockStmts.get(j) instanceof MultiDeclaration)) {
              hasNonDecls = true;
              break;
            }
          }
          if (!hasNonDecls) { break; }
          newStmts = Lists.newArrayList(stmts.subList(0, i + 1));
          for (int j = i + 1; j <= last; ++j) {
            hoistDecls((Statement) blockStmts.get(j), newStmts);
          }
          break;
        }
      }
    }
    return newStmts;
  }
  private static void hoistDecls(Statement s, List<Statement> out) {
    if (s instanceof Declaration) {
      Declaration d = (Declaration) s;
      if (d instanceof FunctionDeclaration || d.getInitializer() == null) {
        out.add(d);
      } else {
        out.add(new Declaration(d.getFilePosition(), d.getIdentifier(), null));
      }
    } else if (s instanceof CatchStmt) {
      hoistDecls(((CatchStmt) s).getBody(), out);
    } else {
      for (ParseTreeNode c : s.children()) {
        if (c instanceof Statement) {
          hoistDecls((Statement) c, out);
        }
      }
    }
  }

  /** <code>{ foo(); bar() }</code> -> <code> { foo(), bar(); }</code> */
  private List<Statement> joinAdjacentExprs(List<Statement> stmts) {
    if (stmts.isEmpty()) { return null; }
    boolean changed = false;
    boolean progress;
    do {
      progress = false;
      Statement last = stmts.get(0);
      for (int i = 1, n = stmts.size(); i < n; ++i) {
        Statement next = stmts.get(i);
        if (isExpressionListTerminator(next) && last instanceof Conditional) {
          // Handle cases like
          //   if (baz) return boo(); return far();
          // where the statement following the if, is implicitly an else
          // statement since the if always returns or throws.
          // We create a fake conditional, and try to optimize it in isolation,
          // which would yield (return baz ? boo() : far()) for the above.
          // This feeds into the tail handling for expression runs below.
          Conditional combined = condAndImplicitElse((Conditional) last, next);
          if (combined != null) {
            ParseTreeNode optCond = optimize(combined, false);
            if (isExpressionListTerminator(optCond)) {
              stmts.subList(i - 1, i + 1).clear();
              stmts.add(i - 1, last = (Statement) optCond);
              --n;
              --i;
              progress = true;
              continue;
            }
          }
        }
        last = next;
      }

      // Turning adjacent statements into comma operations replaces semis with
      // commas, and lets us eliminate a lot more brackets.  It also enables
      // better conditional/return statement optimizations.
      int firstExprStmt = -1;
      for (int i = 0, n = stmts.size(); ; ++i) {
        Statement s = i != n ? stmts.get(i) : null;

        if (firstExprStmt != -1 && (i == n || !(s instanceof ExpressionStmt))) {
          // We've finished a run of ExpressionStmts
          int start = firstExprStmt;
          int end = i;
          firstExprStmt = -1;

          if (isExpressionListTerminator(s)) {
            // We can combine the child onto it, a la
            //   { foo(); return bar(); }  ->  { return foo(), bar(); }
            ++end;
          }
          if (end - start >= 2) {
            progress = true;
            Expression joined = null;
            for (Statement toJoin : stmts.subList(start, end)) {
              List<? extends ParseTreeNode> tjChildren = toJoin.children();
              // tjChildren can be empty for return statements that implicitly
              // return undefined.
              Expression e = tjChildren.isEmpty()
                  ? undef(FilePosition.endOf(toJoin.getFilePosition()))
                  : (Expression) tjChildren.get(0);
              joined = joined == null ? e : commaOp(joined, e);
            }
            Statement newChild;
            FilePosition exprPos = joined.getFilePosition();
            if (s instanceof ReturnStmt) {
              newChild = new ReturnStmt(exprPos, joined);
            } else if (s instanceof ThrowStmt) {
              newChild = new ThrowStmt(exprPos, joined);
            } else {
              newChild = new ExpressionStmt(exprPos, joined);
            }
            stmts.subList(start, end).clear();
            stmts.add(start, newChild);
            n -= end - start;
            i = start;
          }
        } else if (s instanceof ExpressionStmt && firstExprStmt == -1) {
          firstExprStmt = i;
        }
        if (i == n) { break; }
      }
      // We have two optimizations that can help one another.
      // Consider
      //   if (foo()) return bar();
      //   baz();
      //   boo();
      //   return far();
      // On the first pass through this loop, we get
      //   if (foo()) return bar();
      //   return baz(),boo(),far()
      // On the second, that is collapsed to
      //   return foo()?bar():(baz(),boo(),far())
      if (progress) { changed = true; }
    } while (progress);
    return changed ? stmts : null;
  }

  private static Statement optimizeConditional(
      FilePosition parentPos, List<ParseTreeNode> condParts) {
    // We can optimize it if all the clauses are expression stmts, returns or
    // throws.
    int n = condParts.size();
    Class<? extends ParseTreeNode> clauseClass = condParts.get(1).getClass();
    boolean hasElse = (n & 1) == 1;
    if (clauseClass != ExpressionStmt.class
        && !(hasElse && (clauseClass == ReturnStmt.class
                         || clauseClass == ThrowStmt.class))) {
      return null;
    }
    for (int i = 3; i < n; i += 2) {
      if (condParts.get(i).getClass() != clauseClass) { return null; }
    }
    if (hasElse && condParts.get(n - 1).getClass() != clauseClass) {
      return null;
    }

    // Now we know we can optimize because the input is:
    //   if (e0) s0; else if (e1) s1;
    //   if (e0) s0; else if (e1) s1; else s2;
    // where s0... are expression statements evaluated for their side-effects.
    // Or,
    //   if (e0) return r0; else if (e1) return r1; else return r2
    // Or,
    //   if (e0) throw ex0; else if (e1) throw ex1; else throw ex2

    // Consider 3 cases
    // Case 0:
    //   if (a) return b; else if (c) return d else return e
    //   =>   return a ? b : c ? d : e;
    // Case 1:
    //   if (a) b; else if (c) d else e
    //   =>   a ? b : c ? d : e;
    // Case 2:
    //   if (a) b; else if (c) d
    //   =>  a ? b : c && d
    // In the first two cases we end up with nested hook expressions.
    // In the third, we need to special case the last condition.

    // So we start to build an expression from left to right.
    int pos = n;
    Expression e = expressionChildOf(condParts.get(--pos));
    if (!hasElse) {
      Expression lastCond = (Expression) condParts.get(--pos);
      if (Operation.is(lastCond, Operator.NOT)) {
        e = Operation.createInfix(
            Operator.LOGICAL_OR, (Expression) lastCond.children().get(0), e);
      } else {
        e = Operation.createInfix(Operator.LOGICAL_AND, lastCond, e);
      }
    }

    while (pos > 0) {
      Expression clause = expressionChildOf(condParts.get(--pos));
      Expression cond = (Expression) condParts.get(--pos);
      FilePosition fpos = FilePosition.span(
          cond.getFilePosition(), e.getFilePosition());
      if (clause instanceof BooleanLiteral && e instanceof BooleanLiteral) {
        BooleanLiteral a = (BooleanLiteral) clause,
            b = (BooleanLiteral) e;
        if (a.value == b.value) {
          e = commaOp(cond, a).fold(false);
        } else {
          // cond ? true : false -> !!cond
          int nNotsNeeded = a.value ? 2 : 1;
          if (nNotsNeeded == 2 && "boolean".equals(cond.typeOf())) {
            nNotsNeeded = 0;
          }
          e = cond;
          while (--nNotsNeeded >= 0) {
            e = Operation.create(e.getFilePosition(), Operator.NOT, e)
                .fold(false);
          }
        }
      } else if (Operation.is(cond, Operator.NOT)) {
        Expression notCond = ((Operation) cond).children().get(0);
        e = Operation.create(fpos, Operator.TERNARY, notCond, e, clause);
      } else {
        e = Operation.create(fpos, Operator.TERNARY, cond, clause, e);
      }
      e = optimizeExpressionFlow(e);
    }

    return (Statement) ParseTreeNodes.newNodeInstance(
        clauseClass, parentPos, null, Collections.singletonList(e));
  }

  private static Expression expressionChildOf(
      ParseTreeNode returnThrowOrExprStmt) {
    List<? extends ParseTreeNode> children = returnThrowOrExprStmt.children();
    assert children.size() < 2;
    if (children.isEmpty()) {
      FilePosition pos = FilePosition.endOf(
          returnThrowOrExprStmt.getFilePosition());
      return Operation.create(pos, Operator.VOID, new IntegerLiteral(pos, 0));
    }
    return (Expression) children.get(0);
  }

  static Expression optimizeExpressionFlow(Expression e) {
    if (!(e instanceof Operation)) { return e; }
    Operation op = (Operation) e;
    List<? extends Expression> operands = op.children();
    Expression[] newOperands = null;
    int n = operands.size();
    for (int i = 0; i < n; ++i) {
      Expression operand = operands.get(i);
      Expression newOperand = optimizeExpressionFlow(operand);
      if (operand != newOperand) {
        if (newOperands == null) {
          newOperands = operands.toArray(new Expression[n]);
        }
        newOperands[i] = newOperand;
      }
    }
    Operator oper = op.getOperator();
    FilePosition pos = e.getFilePosition();
    if (oper != Operator.TERNARY) {
      return newOperands == null ? e : Operation.create(pos, oper, newOperands);
    }
    // (c ? x,z : y,z) -> (c ? x:y),z
    Expression[] ternaryOperands = newOperands != null
        ? newOperands : operands.toArray(new Expression[3]);
    Expression c = ternaryOperands[0];
    Expression x = ternaryOperands[1];
    Expression y = ternaryOperands[2];
    while (Operation.is(c, Operator.NOT)) {
      c = ((Operation) c).children().get(0);
      Expression t = x;
      x = y;
      y = t;
    }
    if (ParseTreeNodes.deepEquals(x, y)) {
      if (c.simplifyForSideEffect() == null) { return x; }
      return commaOp(pos, c, x);
    }
    if (isSimple(c)) {
      // If a reference fails with an exception because it is undefined, then
      // control would never reach the second identical expression.
      if (ParseTreeNodes.deepEquals(c, x)) {
        // (c ? c : y) -> c || y     if c not side effecting
        return Operation.create(pos, Operator.LOGICAL_OR, c, y);
      } else if (ParseTreeNodes.deepEquals(c, y)) {
        // (c ? x : c) -> c && x     if c not side effecting
        return Operation.create(pos, Operator.LOGICAL_AND, c, x);
      }
    }
    // TODO(mikesamuel): if c is simple and not a global reference, optimize
    // out he common head as well.
    CommaCommonalities opt = commaCommonalities(x, y);
    if (opt != null) {
      // Both reduced sides can't be null since we checked above whether
      // x and y are structurally identical.
      if (opt.aReduced == null) {
        // (c ? z: y,z)  ->  (c || y),z
        return commaOp(
            pos,
            Operation.createInfix(Operator.LOGICAL_OR, c, opt.bReduced),
            opt.commonTail);
      } else if (opt.bReduced == null) {
        // (c ? x,z : z)  ->  (c && x),z
        return commaOp(
            pos,
            Operation.createInfix(Operator.LOGICAL_AND, c, opt.aReduced),
            opt.commonTail);
      } else {
        // (c ? x,z : y,z) -> (c ? x : y),z
        return commaOp(
            pos,
            optimizeExpressionFlow(
                Operation.createTernary(c, opt.aReduced, opt.bReduced)),
            opt.commonTail);
      }
    }
    ternaryOperands[0] = c;
    ternaryOperands[1] = x;
    ternaryOperands[2] = y;
    if (x instanceof Operation && y instanceof Operation) {
      Operation xop = (Operation) x;
      Operation yop = (Operation) y;
      Operator xoper = xop.getOperator();
      if (xoper == yop.getOperator()) {
        List<? extends Expression> xoperands = xop.children();
        List<? extends Expression> yoperands = yop.children();
        int nOperands = xoperands.size();
        if (nOperands == yoperands.size()) {
          Expression xoperand0 = xoperands.get(0);
          // We can often pull the rightmost operand out since it would be
          // evaluated last regardless.
          if (nOperands == 2
              && ParseTreeNodes.deepEquals(xoperands.get(1), yoperands.get(1))
              && xoper.getCategory() != OperatorCategory.ASSIGNMENT
              && (xoper != Operator.FUNCTION_CALL
                  || !(Operation.is(xoperand0, Operator.MEMBER_ACCESS)
                       || Operation.is(xoperand0, Operator.SQUARE_BRACKET)))) {
            // c ? foo(myNode) : bar(myNode)  =>  (c ? foo : bar)(myNode)
            return Operation.create(
                pos, xoper,
                optimizeExpressionFlow(Operation.createTernary(
                    c, xoperands.get(0), yoperands.get(0))),
                xoperands.get(1));
          }
          if (// Switching order of evaluation doesn't matter
              isSimple(xoperands.get(0)) && isSimple(c)
              && ParseTreeNodes.deepEquals(xoperands.get(0), yoperands.get(0))
              ) {
            // c ? (x + 1 : x + 2)  ->  x + (c ? 1 : 2)
            return Operation.create(
                pos, xoper, xoperands.get(0),
                optimizeExpressionFlow(
                    Operation.createTernary(
                        c, xoperands.get(1), yoperands.get(1))));
          }
        }
      }
    }

    if (operands.equals(Arrays.asList(ternaryOperands))) { return e; }
    return Operation.create(pos, Operator.TERNARY, ternaryOperands);
  }

  private static class CommaCommonalities {
    Expression aReduced;
    Expression bReduced;
    Expression commonTail;
  }

  private static CommaCommonalities commaCommonalities(
      Expression a, Expression b) {
    List<Expression> aChain = unrollComma(a);
    List<Expression> bChain = unrollComma(b);

    int aSize = aChain.size(), bSize = bChain.size();
    int minSize = Math.min(aSize, bSize);
    int nCommon = 0;
    for (int i = 0; i < minSize; ++i) {
      Expression ael = aChain.get(aSize - 1 - i);
      Expression bel = bChain.get(bSize - 1 - i);
      if (!ParseTreeNodes.deepEquals(ael, bel)) { break; }
      ++nCommon;
    }
    if (nCommon == 0) { return null; }
    CommaCommonalities c = new CommaCommonalities();
    if (aSize != nCommon) {
      c.aReduced = a;
      for (int i = nCommon; --i >= 0;) {
        assert Operation.is(c.aReduced, Operator.COMMA);
        c.aReduced = ((Operation) c.aReduced).children().get(0);
      }
    }
    if (bSize != nCommon) {
      c.bReduced = b;
      for (int i = nCommon; --i >= 0;) {
        assert Operation.is(c.bReduced, Operator.COMMA);
        c.bReduced = ((Operation) c.bReduced).children().get(0);
      }
    }
    c.commonTail = aChain.get(aSize - nCommon);
    for (int i = aSize - nCommon + 1; i < aSize; ++i) {
      c.commonTail = commaOp(c.commonTail, aChain.get(i));
    }
    return c;
  }
  private static List<Expression> unrollComma(Expression e) {
    List<Expression> chain = Lists.newArrayList();
    while (Operation.is(e, Operator.COMMA)) {
      List<? extends Expression> operands = ((Operation) e).children();
      e = operands.get(0);
      chain.add(operands.get(1));
    }
    chain.add(e);
    for (int i = 0, j = chain.size(); i < --j; ++i) {
      Expression t = chain.get(i);
      chain.set(i, chain.get(j));
      chain.set(j, t);
    }
    return chain;
  }

  private static Expression commaOp(
      FilePosition pos, Expression a, Expression b) {
    while (Operation.is(b, Operator.COMMA)) {
      // (a, (b, c)) -> (a, b, c)
      List<? extends Expression> operands = ((Operation) b).children();
      Expression op0 = operands.get(0);
      a = commaOp(a, op0);
      b = operands.get(1);
    }
    return Operation.create(pos, Operator.COMMA, a, b);
  }

  private static Expression commaOp(Expression a, Expression b) {
    return commaOp(
        FilePosition.span(a.getFilePosition(), b.getFilePosition()), a, b);
  }

  private static boolean isSimple(Expression e) {
    // TODO(mikesamuel): limit to local references not in with blocks.
    return e.simplifyForSideEffect() == null || e instanceof Reference;
  }

  private static boolean isExpressionListTerminator(ParseTreeNode s) {
    return s instanceof ReturnStmt || s instanceof ThrowStmt;
  }

  private static Expression undef(FilePosition pos) {
    return Operation.create(pos, Operator.VOID, new IntegerLiteral(pos, 0));
  }

  private static Conditional condAndImplicitElse(
      Conditional cond, Statement follower) {
    List<? extends ParseTreeNode> children = cond.children();
    if ((children.size() & 1) != 0) { return null; }
    Class<? extends ParseTreeNode> commonType = children.get(1).getClass();
    if (commonType != ReturnStmt.class && commonType != ThrowStmt.class) {
      return null;
    }
    for (int i = children.size() - 1; i >= 3; i -= 2) {
      if (children.get(i).getClass() != commonType) { return null; }
    }
    List<ParseTreeNode> allChildren = Lists.newArrayList(children);
    allChildren.add(follower);
    return new Conditional(
        FilePosition.span(cond.getFilePosition(), follower.getFilePosition()),
        null, allChildren);
  }

  private static final class StmtLabel {
    final String newName;
    int nUses;
    StmtLabel(String newName) { this.newName = newName; }
  }

  /**
   * Conservatively, does control exit the current function, throw an exception,
   * or break to a label instead of continuing to the following statement.
   */
  static boolean exits(ParseTreeNode node) {
    if (node instanceof Block) {
      List<? extends ParseTreeNode> children = node.children();
      return !children.isEmpty() && exits(children.get(children.size() - 1));
    } else if (node instanceof Conditional) {
      List<? extends ParseTreeNode> children = node.children();
      int n = children.size();
      if ((n & 1) == 0) { return false; }  // no else stmt
      for (int i = 1; i < n; i += 2) {
        if (!exits(children.get(i))) { return false; }
      }
      return exits(children.get(n - 1));
    } else if (node instanceof BreakStmt || node instanceof ContinueStmt
               || node instanceof ReturnStmt || node instanceof ThrowStmt) {
      return true;
    }
    return false;
  }

  private Statement optimizeSwitch(SwitchStmt ss) {
    List<ParseTreeNode> newChildren = Lists.newArrayList(ss.children());
    boolean changed = false;
    for (int i = 0, n = newChildren.size(); i < n; ++i) {
      ParseTreeNode child = newChildren.get(i);
      ParseTreeNode newChild = optimize(child, false);
      if (newChild != child) {
        changed = true;
        newChildren.set(i, newChild);
      }
    }
    // Eliminate unnecessary default statements.
    // Having a default statement prevents other optimizations so aggressively
    // optimize them out where possible.
    boolean hasDefault = false;
    for (int i = newChildren.size(); --i >= 1;) {
      SwitchCase cs = (SwitchCase) newChildren.get(i);
      if (!(cs instanceof DefaultCaseStmt)) { continue; }
      Statement body = cs.getBody();
      if (body instanceof Noop) {
        changed = true;
        newChildren.remove(i);
      } else if (isBlankBreak(body)) {
        if (i != 1 || !exits(((SwitchCase) newChildren.get(i - 1)).getBody())) {
          // Move the break into the preceding case.
          SwitchCase prev = (SwitchCase) newChildren.get(i - 1);
          newChildren.set(i - 1, withBody(prev, combine(prev.getBody(), body)));
          changed = true;
        }
        newChildren.remove(i);
        changed = true;
      } else {
        hasDefault = true;
      }
    }
    // Eliminate unnecessary breaks
    if (!hasDefault) {
      for (int i = newChildren.size(); --i >= 1;) {
        CaseStmt cs = (CaseStmt) newChildren.get(i);
        Statement body = cs.getBody();
        if (!isBlankBreak(body)) { continue; }
        if (cs.getCaseValue().simplifyForSideEffect() != null) { continue; }
        if (i != 1) {
          SwitchCase prev = (SwitchCase) newChildren.get(i - 1);
          if (!exits(prev.getBody())) {
            // Move the break into the preceding case.
            newChildren.set(
                i - 1, withBody(prev, combine(prev.getBody(), body)));
          }
        }
        newChildren.remove(i);
        changed = true;
      }
    }
    // Eliminate duplicate cases
    SwitchCase last = null;
    for (int i = 1; i < newChildren.size(); ++i) {
      SwitchCase cs = (SwitchCase) newChildren.get(i);
      if (last != null && !(last.getBody() instanceof Noop)
          && ParseTreeNodes.deepEquals(last.getBody(), cs.getBody())) {
        newChildren.set(i - 1, withoutBody(last));
        changed = true;
      }
      last = cs;
    }
    while (newChildren.size() > 1) {
      int lastIndex = newChildren.size() - 1;
      last = (SwitchCase) newChildren.get(lastIndex);

      Statement lastBody = last.getBody();
      boolean changedOne = false;
      if (lastBody instanceof Block) {
        // Eliminate trailing break statement.
        List<? extends Statement> stmts = ((Block) lastBody).children();
        int n = stmts.size();
        if (n > 0 && isBlankBreak(stmts.get(n - 1))) {
          stmts = stmts.subList(0, n - 1);
          Statement newBody = null;
          switch (stmts.size()) {
            case 0: newBody = new Noop(lastBody.getFilePosition()); break;
            case 1: newBody = stmts.get(0); break;
            default: newBody = new Block(lastBody.getFilePosition(), stmts);
          }
          newChildren.set(newChildren.size() - 1, withBody(last, newBody));
          changedOne = true;
        }
      } else if (isBlankBreak(lastBody)) {
        //    switch (...) { default: ... case foo: break; }
        // => switch (...) { default: ... case foo: }
        newChildren.set(lastIndex, withoutBody(last));
        changedOne = true;
      } else if (!hasDefault && lastBody instanceof Noop) {
        CaseStmt cs = (CaseStmt) last;  // OK since !hasDefault
        //    switch (...) { ... case 4: case 5: break; }
        // => switch (...) { ... }
        // We can eliminate cases entirely if there is no default since it
        // will not change the set of values that default matches.
        if (null == cs.getCaseValue().simplifyForSideEffect()) {
          newChildren.remove(lastIndex);
          changedOne = true;
        }
      }
      if (changedOne) {
        changed = true;
      } else {
        break;
      }
    }
    // Eliminate empty cases with side-effect free case values.
    return changed
        ? new SwitchStmt(ss.getFilePosition(), ss.getLabel(), newChildren) : ss;
  }

  private static SwitchCase withoutBody(SwitchCase sc) {
    return withBody(sc, new Noop(sc.getBody().getFilePosition()));
  }

  private static SwitchCase withBody(SwitchCase sc, Statement body) {
    if (sc instanceof DefaultCaseStmt) {
      return new DefaultCaseStmt(sc.getFilePosition(), body);
    } else {
      CaseStmt cs = (CaseStmt) sc;
      return new CaseStmt(sc.getFilePosition(), cs.getCaseValue(), body);
    }
  }

  private static boolean isBlankBreak(Statement s) {
    return s instanceof BreakStmt && "".equals(((BreakStmt) s).getLabel());
  }

  private static Statement combine(Statement a, Statement b) {
    FilePosition pos = FilePosition.span(
        a.getFilePosition(), b.getFilePosition());
    return combine(pos, a, b);
  }

  private static Statement combine(FilePosition pos, Statement a, Statement b) {
    if (b instanceof Noop) { return a; }
    if (a instanceof Noop) { return b; }
    List<Statement> stmts = Lists.newArrayList();
    if (a instanceof Block) {
      stmts.addAll(((Block) a).children());
    } else {
      stmts.add(a);
    }
    if (b instanceof Block) {
      stmts.addAll(((Block) b).children());
    } else {
      stmts.add(b);
    }
    return new Block(pos, stmts);
  }

  // TODO(mikesamuel): eliminate dead branches in if statements.
  // TODO(mikesamuel): simplify empty loops
  // TODO(mikesamuel): fold string appends in + statements
}
