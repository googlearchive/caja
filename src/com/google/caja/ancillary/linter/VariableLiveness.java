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

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.AssignOperation;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.BreakStmt;
import com.google.caja.parser.js.CaseStmt;
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.Conditional;
import com.google.caja.parser.js.ContinueStmt;
import com.google.caja.parser.js.DebuggerStmt;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.DefaultCaseStmt;
import com.google.caja.parser.js.DirectivePrologue;
import com.google.caja.parser.js.DoWhileLoop;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FinallyStmt;
import com.google.caja.parser.js.ForEachLoop;
import com.google.caja.parser.js.ForLoop;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.LabeledStatement;
import com.google.caja.parser.js.LabeledStmtWrapper;
import com.google.caja.parser.js.MultiDeclaration;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.ObjProperty;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.ReturnStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.SwitchStmt;
import com.google.caja.parser.js.ThrowStmt;
import com.google.caja.parser.js.TryStmt;
import com.google.caja.parser.js.WhileLoop;
import com.google.caja.parser.js.WithStmt;
import com.google.caja.util.SyntheticAttributeKey;

import java.util.List;

/**
 * Given a variable definition and a set of uses, computes the liveness of
 * that variable.
 *
 * <p>
 * This class is implemented by doing the same kinds of tree based computations
 * that EcmaScript 262 uses to define the semantics of the various control
 * statements and expressions, so many of the comments below are expressed in
 * terms of conservative conclusions about the ways in which a hypothetical
 * parse-tree interpreter would process a program.
 *
 * <h2>Glossary</h2>
 * See also definitions in {@link LexicalScope}.
 * <dl>
 * <dt>{@link LiveSet Liveness}</dt>
 * <dd>At a point in a program the set of symbols that must have been assigned
 *     a value and not subsequently gone out of scope before control could
 *     reach that point in the program.</dd>
 * <dt>{@link ExitModes Exit Modes}</dt>
 * <dd>The ways in which control leaves a node.  The code below assumes that
 *     an exception can be thrown by any statement or expression except, so
 *     only explicit {@code throw} statements are used in calculations.
 *     This assumption simplifies processing of all structures except
 *     {@code try} blocks.
 * <dt>Normal exit</dt>
 * <dd>When control leaves a scope as a result of a
 *     {@link ReturnStmt <tt>return</tt>} statement or a block completing.
 *     A program that does not halt neither exits normally nor abnormally,
 *     though this analyzer cannot detect all infinite loops, and so assumes
 *     that any loop condition will eventually return false.</dd>
 * <dd>Abrupt exit</dt>
 * <dd>When control leaves a scope as a result of an exception.</dd>
 * </dl>
 *
 * <p>
 * The {@link #liveness} method dispatches on node
 * type to methods that recurse to walk the tree.  The result of this
 * calculation at each node is a {@link LiveCalc} object which contains
 * a {@link LiveSet set of live variables}, computed conservatively, and an
 * {@link ExitModes exit-mode} object that is analogous to the
 * <tt>(normal, empty, empty)</tt> style triples used in chapter 12 of
 * ES262 to define statement semantics.
 *
 * @author mikesamuel@gmail.com
 */
final class VariableLiveness {
  /** Visible for testing. */
  static final SyntheticAttributeKey<LiveSet> LIVENESS
      = new SyntheticAttributeKey<LiveSet>(LiveSet.class, "liveness");

  /**
   * Returns the set of symbols that must be live when control enters the
   * given node.
   * @return null if {@link #calculateLiveness} has not been called on any
   *     AST containing this node or if control never enters this node.
   *     Control does not enter some leaf nodes such as {@link Identifier}s
   *     and in unreachable code as in {@code foo(); return; unreachable();}.
   */
  static LiveSet livenessFor(ParseTreeNode node) {
    return node.getAttributes().get(LIVENESS);
  }

  /**
   * Compute liveness for the given tree.
   * @param node a Javascript AST that has been annotated with scopes as by a
   *     {@link ScopeAnalyzer}.
   * @return a pair describing the ways in which control leaves node.
   *     If the input node were to exit normally, then {@link LiveCalc#vars}
   *     would be the set of variables live once execution finished.
   *     If there are {@code break}s that match no labeled loop, then those
   *     will be visible via {@link LiveCalc#exits}.
   */
  static LiveCalc calculateLiveness(ParseTreeNode node) {
    return liveness(node, new LiveSet(node));
  }

  /**
   * Attaches liveness info to node and recurses to children.
   * @param onEntry the set of variables live when controls enters the given
   *     node.
   */
  private static LiveCalc liveness(ParseTreeNode node, LiveSet onEntry) {
    node.getAttributes().set(LIVENESS, onEntry);

    LiveCalc onExit;
    if (node instanceof Statement) {
      onExit = processStatement((Statement) node, onEntry);
      // If a break or continue reaches here, take it into account by
      // stopping propagation of exit mode info associate with its label and
      // incorporating liveness information at the point the break or continue
      // occurred.
      if (node instanceof LabeledStatement) {
        onExit = processLabel(((LabeledStatement) node), onExit);
      }
    } else if (node instanceof Expression) {
      // Because of our simplifying assumptions about all statements and
      // expressions possibly throwing exceptions, we can treat all expressions
      // as if they complete.
      onExit = new LiveCalc(
          processExpression((Expression) node, onEntry), ExitModes.COMPLETES);
    } else {
      // We can't handle parse tree nodes we don't recognizer.  If this occurs,
      // either the input isn't a valid JS tree, or a node-type handler needs
      // to be added.
      throw new RuntimeException(node.getClass().getName());
    }
    onExit = onExit.filter(ScopeAnalyzer.containingScopeForNode(node));
    return onExit;
  }
  private static LiveCalc processStatement(Statement s, LiveSet onEntry) {
    if (s instanceof Block) {
      return processBlock((Block) s, onEntry);
    } else if (s instanceof Conditional) {
      return processConditional((Conditional) s, onEntry);
    } else if (s instanceof DoWhileLoop) {
      return processDoWhileLoop((DoWhileLoop) s, onEntry);
    } else if (s instanceof WhileLoop) {
      return processWhileLoop((WhileLoop) s, onEntry);
    } else if (s instanceof ForLoop) {
      return processForLoop((ForLoop) s, onEntry);
    } else if (s instanceof ForEachLoop) {
      return processForEachLoop((ForEachLoop) s, onEntry);
    } else if (s instanceof ExpressionStmt) {
      return processExpressionStmt((ExpressionStmt) s, onEntry);
    } else if (s instanceof FunctionDeclaration) {
      return processFunctionDeclaration((FunctionDeclaration) s, onEntry);
    } else if (s instanceof Declaration) {
      return processDeclaration((Declaration) s, onEntry);
    } else if (s instanceof MultiDeclaration) {
      return processMultiDeclaration((MultiDeclaration) s, onEntry);
    } else if (s instanceof SwitchStmt) {
      return processSwitchStmt((SwitchStmt) s, onEntry);
    } else if (s instanceof DefaultCaseStmt) {
      return processDefaultCaseStmt((DefaultCaseStmt) s, onEntry);
    } else if (s instanceof CaseStmt) {
      return processCaseStmt((CaseStmt) s, onEntry);
    } else if (s instanceof TryStmt) {
      return processTryStmt((TryStmt) s, onEntry);
    } else if (s instanceof CatchStmt) {
      return processCatchStmt((CatchStmt) s, onEntry);
    } else if (s instanceof FinallyStmt) {
      return processFinallyStmt((FinallyStmt) s, onEntry);
    } else if (s instanceof BreakStmt) {
      return processBreakStmt((BreakStmt) s, onEntry);
    } else if (s instanceof ContinueStmt) {
      return processContinueStmt((ContinueStmt) s, onEntry);
    } else if (s instanceof ReturnStmt) {
      return processReturnStmt((ReturnStmt) s, onEntry);
    } else if (s instanceof ThrowStmt) {
      return processThrowStmt((ThrowStmt) s, onEntry);
    } else if (s instanceof WithStmt) {
      return processWithStmt((WithStmt) s, onEntry);
    } else if (s instanceof Noop) {
      return processNoop(onEntry);
    } else if (s instanceof LabeledStmtWrapper) {
      return processLabeledStmtWrapper((LabeledStmtWrapper) s, onEntry);
    } else if (s instanceof DirectivePrologue) {
      return processDirectivePrologue(onEntry);
    } else if (s instanceof DebuggerStmt) {
      return new LiveCalc(onEntry, ExitModes.COMPLETES);
    } else {
      throw new RuntimeException(s.getClass().getName());
    }
  }

  /**
   * Process an expression, computing the set of variables live after the
   * expression executes, but ignoring exit modes.
   * We don't currently track exit modes through expressions though, since they
   * can't return break or continue, and almost any expression can raise an
   * exception.
   */
  private static LiveSet processExpression(Expression e, LiveSet onEntry) {
    // We may want to reconsider tracking exceptions specifically through
    // expressions, to more accurately model
    //   try {
    //     return (function () { throw new Error; })();
    //   } catch (ex) {
    //     a = 1;
    //   }
    //   // a is provably live here

    if (e instanceof Operation) {
      return processOperation((Operation) e, onEntry);
    } else if (e instanceof FunctionConstructor) {
      // Returns the live-set at the end of the body.
      // Process that to fill out the information stored in the parse tree,
      // but don't use the result since the function has not been executed yet.
      processFunctionConstructor((FunctionConstructor) e);
      return onEntry;
    } else if (e instanceof Reference) {
      return processReference((Reference) e, onEntry);
    } else {
      return processLiteralOrConstructor(e, onEntry);
    }
  }

  private static LiveCalc processBlock(Block b, LiveSet onEntry) {
    LiveCalc onExit = new LiveCalc(onEntry, ExitModes.COMPLETES);
    for (Statement child : b.children()) {
      if (child instanceof FunctionDeclaration) {
        onExit = liveness(child, onExit.vars);
      }
    }
    for (Statement child : b.children()) {
      if (child instanceof FunctionDeclaration) { continue; }
      LiveCalc r = liveness(child, onExit.vars);
      if (r.exits.completes()) {
        // union for side-effects in series.
        onExit = new LiveCalc(
            r.vars.union(onExit.vars), r.exits.union(onExit.exits));
      } else {
        // once the block is exited, stop processing unreachable code
        onExit = r.withExits(onExit.exits.union(r.exits));
      }
      if (!onExit.exits.completes()) { break; }
    }
    return onExit;
  }

  private static LiveCalc processConditional(Conditional c, LiveSet onEntry) {
    List<? extends ParseTreeNode> children = c.children();
    LiveSet afterLastCond = onEntry;
    LiveCalc onExit = null;
    for (int i = 0, n = children.size(); i <= n; i += 2) {
      LiveCalc afterClause;
      if (i != n) {  // There is a clause to process.
        if (i + 1 < n) {  // Process if/else if.
          // We are not processing an else clause, or the implicit else; at the
          // end of a conditional.
          ConditionalLiveSet afterCond = processCondition(
              (Expression) children.get(i), afterLastCond);
          afterClause = liveness(children.get(i + 1), afterCond.truthy);
          afterLastCond = afterCond.falsey;
        } else {  // Process the else clause
          afterClause = liveness(children.get(i), afterLastCond);
        }
      } else {  // Pretend there's an else; to make sure exit modes are correct.
        afterClause = new LiveCalc(afterLastCond, ExitModes.COMPLETES);
      }
      if (onExit == null) {
        onExit = afterClause;
      } else {
        onExit = new LiveCalc(
            (afterClause.exits.completes()
             ? onExit.vars.intersection(afterClause.vars)
             : onExit.vars),
            afterClause.exits.intersection(onExit.exits));
      }
    }
    assert onExit != null;
    return onExit;
  }

  private static LiveCalc processDoWhileLoop(DoWhileLoop dw, LiveSet onEntry) {
    // label for this loop and others consumed in processLabel
    LiveCalc postBody = liveness(dw.getBody(), onEntry);
    LiveSet condVars;
    if (postBody.exits.completes()
        || postBody.exits.atContinue("") != null
        || postBody.exits.atContinue(dw.getLabel()) != null) {
      // If the body always exits without a continue, the condition is
      // unreachable code.
      // do-while loops are the only ones that behave this way.
      condVars = processCondition(dw.getCondition(), postBody.vars).falsey;
    } else {
      condVars = postBody.vars;
    }
    return postBody.withVars(condVars);
  }

  private static LiveCalc processWhileLoop(WhileLoop s, LiveSet onEntry) {
    ConditionalLiveSet postCond = processCondition(s.getCondition(), onEntry);
    LiveCalc postBody = liveness(s.getBody(), postCond.truthy);
    return postBody
        // If the condition is falsey, then the body never executed, so
        // the vars set are the intersection of the two cases
        .withVars(postCond.falsey.intersection(postBody.vars))
        // And the exit modes are the intersection of the body modes, and
        // the case where the statement completes because the condition was
        // initially false.
        .withExits(postBody.exits.intersection(ExitModes.COMPLETES));
  }

  private static LiveCalc processForLoop(ForLoop loop, LiveSet onEntry) {
    LiveCalc postInit = liveness(loop.getInitializer(), onEntry);
    ConditionalLiveSet postCond = processCondition(
        loop.getCondition(), postInit.vars);
    LiveCalc postBody = liveness(loop.getBody(), postCond.truthy);
    if (postBody.exits.completes()) {
      // If the body completes, then the loop increment is reachable.
      liveness(loop.getIncrement(), postBody.vars);
    }
    return postBody
        .withVars(
            postBody.exits.completes()
            ? postBody.vars.intersection(postCond.falsey)
            : postCond.falsey)
        // Unless the condition is provably false, then it completes.
        .withExits(postBody.exits.intersection(ExitModes.COMPLETES));
  }

  private static LiveCalc processForEachLoop(ForEachLoop s, LiveSet onEntry) {
    LiveCalc postObj = liveness(s.getContainer(), onEntry);
    Statement receiver = s.getKeyReceiver();
    LiveCalc preBody = liveness(s.getKeyReceiver(), postObj.vars);
    preBody = preBody.withExits(preBody.exits.union(postObj.exits));
    if (receiver instanceof Declaration) {
      // Define any variable that holds the key.
      preBody = preBody.withVars(
          preBody.vars.with((Declaration) receiver));
    } else {
      preBody = preBody.withVars(
          preBody.vars.with(
              (Reference) ((ExpressionStmt) receiver).getExpression()));
    }
    LiveCalc postBody = liveness(s.getBody(), preBody.vars);
    return postBody.withVars(postBody.vars.intersection(postObj.vars));
  }

  private static LiveCalc processExpressionStmt(
      ExpressionStmt es, LiveSet onEntry) {
    return liveness(es.getExpression(), onEntry);
  }

  private static LiveCalc processFunctionDeclaration(
      FunctionDeclaration fd, LiveSet onEntry) {
    liveness(fd.getInitializer(), onEntry);
    return new LiveCalc(onEntry.with(fd), ExitModes.COMPLETES);
  }

  private static LiveCalc processDeclaration(Declaration d, LiveSet onEntry) {
    if (d.getInitializer() == null) {
      // Declarations without initializers do not make their variable live.
      return new LiveCalc(onEntry, ExitModes.COMPLETES);
    }
    LiveCalc postInit = liveness(d.getInitializer(), onEntry);
    return new LiveCalc(postInit.vars.with(d), postInit.exits);
  }

  private static LiveCalc processMultiDeclaration(
      MultiDeclaration md, LiveSet onEntry) {
    LiveCalc onExit = new LiveCalc(onEntry, ExitModes.COMPLETES);
    for (Declaration d : md.children()) {
      LiveCalc r = liveness(d, onExit.vars);
      onExit = new LiveCalc(
          r.vars.union(onExit.vars), r.exits.union(onExit.exits));
    }
    return onExit;
  }

  private static LiveCalc processSwitchStmt(SwitchStmt s, LiveSet onEntry) {
    List<? extends ParseTreeNode> children = s.children();
    LiveCalc postSwitchValue = liveness(children.get(0), onEntry);
    boolean sawDefault = false;
    LiveCalc last = null;
    for (int i = 1, n = children.size(); i < n; ++i) {
      ParseTreeNode node = children.get(i);
      if (node instanceof DefaultCaseStmt) { sawDefault = true; }
      ExitModes exits = last == null ? null : last.exits;
      last = liveness(node, postSwitchValue.vars);
      if (exits != null) {
        last = last.withExits(last.exits.intersection(exits));
      }
    }
    if (sawDefault) {
      return last;
    } else if (last == null) {  // no case statements
      return postSwitchValue;
    } else {
      return new LiveCalc(
          postSwitchValue.vars.intersection(last.vars),
          postSwitchValue.exits.intersection(last.exits));
    }
  }

  private static LiveCalc processDefaultCaseStmt(
      DefaultCaseStmt s, LiveSet onEntry) {
    return liveness(s.getBody(), onEntry);
  }

  private static LiveCalc processCaseStmt(CaseStmt s, LiveSet onEntry) {
    LiveCalc postMatch = liveness(s.getCaseValue(), onEntry);
    return liveness(s.getBody(), postMatch.vars);
  }

  private static LiveCalc processTryStmt(TryStmt s, LiveSet onEntry) {
    LiveCalc postBody = liveness(s.getBody(), onEntry);
    // Compute the set of live variables before any finally statement.
    LiveCalc preFinally;
    boolean hasFinally = s.getFinallyClause() != null;
    if (s.getCatchClause() != null) {
      boolean postBodyReturnsAbruptly = postBody.exits.returnsAbruptly();
      postBody = postBody.withExits(postBody.exits.withoutAbruptReturn());
      LiveCalc postCatch = liveness(
          s.getCatchClause(),
          // The body may not have executed completely, so do the intersection
          // with the beginning.
          // We cannot use information about the vars associated with throws
          // clauses even if postBody.exits.returnAbruptly, since an exception
          // may have been raised by the interpreter before control reached
          // a throw clause.
          // Even in
          //     try { throw new Error(a = 1); } catch (ex) { ... }
          // we do not know if a is live in the catch, because an exception
          // may have been raised on the reference to Error before a = 1 is
          // evaluated.
          onEntry.intersection(postBody.vars));
      if (!hasFinally && postBody.exits.returnsNormally()) {
        preFinally = postCatch;
      } else if (!hasFinally && postCatch.exits.returns()) {
        preFinally = postBody;
      } else if (!postCatch.exits.completes()) {
        // Any return or break from the catch clause is trumped by a finally
        // block.
        preFinally = hasFinally
            ? new LiveCalc(
                  onEntry, postBody.exits.intersection(postCatch.exits))
            : postBody;
      } else if (postBodyReturnsAbruptly || !postBody.exits.completes()) {
        preFinally = postCatch;
      } else {
        preFinally = new LiveCalc(
            postBody.vars.intersection(postCatch.vars),
            ExitModes.COMPLETES);
      }
    } else {
      // Since there is no catch statement, there must be a finally,
      assert hasFinally;
      // so control will reach the finally block from the body regardless
      // of the body exit modes.
      preFinally = new LiveCalc(onEntry, ExitModes.COMPLETES);
    }
    if (hasFinally) {
      // finally always executes, so its results dominate
      return liveness(s.getFinallyClause(), preFinally.vars);
    }
    return preFinally;
  }

  private static LiveCalc processCatchStmt(CatchStmt s, LiveSet onEntry) {
    liveness(s.getException(), onEntry);
    return liveness(s.getBody(), onEntry.with(s.getException()));
  }

  private static LiveCalc processFinallyStmt(FinallyStmt s, LiveSet onEntry) {
    return liveness(s.getBody(), onEntry);
  }

  private static LiveCalc processBreakStmt(BreakStmt s, LiveSet onEntry) {
    return new LiveCalc(onEntry, ExitModes.COMPLETES.withBreak(s, onEntry));
  }

  private static LiveCalc processContinueStmt(ContinueStmt s, LiveSet onEntry) {
    return new LiveCalc(onEntry, ExitModes.COMPLETES.withContinue(s, onEntry));
  }

  private static LiveCalc processReturnStmt(ReturnStmt rs, LiveSet onEntry) {
    LiveSet onExit;
    if (rs.getReturnValue() != null) {
      LiveCalc r = liveness(rs.getReturnValue(), onEntry);
      if (r.exits.returnsAbruptly()) {
        // If the result of evaluating the return value consistently threw an
        // exception as in
        //     return (function () { throw new Error; })()
        // that should dominate.
        return r;
      }
      onExit = r.vars;
    } else {
      onExit = onEntry;
    }
    return new LiveCalc(
        onExit, ExitModes.COMPLETES.withNormalReturn(rs, onExit));
  }

  private static LiveCalc processThrowStmt(ThrowStmt ts, LiveSet onEntry) {
    LiveCalc onExit = liveness(ts.getException(), onEntry);
    return onExit.withExits(onExit.exits.withAbruptReturn(ts, onExit.vars));
  }

  private static LiveCalc processWithStmt(WithStmt ts, LiveSet onEntry) {
    LiveCalc afterObject = liveness(ts.getScopeObject(), onEntry);
    // We can't draw any conclusions about code under a with statement.
    // Not even about code in closures inside it.
    // Process the body and throw out everything but the exit modes.
    LiveCalc afterBody = liveness(ts.getBody(), afterObject.vars);
    scrub(ts.getBody());
    return afterObject.withExits(afterBody.exits);
  }
  private static void scrub(ParseTreeNode node) {
    node.getAttributes().remove(LIVENESS);
    for (ParseTreeNode child : node.children()) {
      scrub(child);
    }
  }

  private static LiveCalc processNoop(LiveSet onEntry) {
    return new LiveCalc(onEntry, ExitModes.COMPLETES);
  }

  private static LiveCalc processLabeledStmtWrapper(
      LabeledStmtWrapper s, LiveSet onEntry) {
    // label consumed in processLabel
    return liveness(s.getBody(), onEntry);
  }

  private static LiveSet processOperation(Operation op, LiveSet onEntry) {
    List<? extends Expression> operands = op.children();
    switch (op.getOperator()) {
      case TERNARY:
        ConditionalLiveSet postCond = processLogicOperand(op, 0, onEntry);
        LiveCalc postThen = liveness(operands.get(1), postCond.truthy);
        LiveCalc postElse = liveness(operands.get(2), postCond.falsey);
        return postThen.vars.intersection(postElse.vars);
      case LOGICAL_AND:
      case LOGICAL_OR:
        ConditionalLiveSet p = processCondition(op, onEntry);
        return p.truthy.intersection(p.falsey);
      case FUNCTION_CALL:
        if (operands.get(0) instanceof FunctionConstructor) {
          return processImmediatelyCalledFunction(op, onEntry);
        }
        break;
      default: break;
    }

    LiveSet postOperand = onEntry;
    for (Expression operand : operands) {
      postOperand = liveness(operand, postOperand).vars;
    }
    if (op instanceof AssignOperation
        && operands.get(0) instanceof Reference) {
      postOperand = postOperand.with((Reference) operands.get(0));
    }
    return postOperand;
  }

  /**
   * Do case based analysis of conditions so that conditional branches can
   * incorporate the fact that different variables may be live when an
   * expression returns a truthy value than when it returns a falsey one.
   */
  private static
  ConditionalLiveSet processCondition(Expression e, LiveSet onEntry) {
    // This branch is different from the others in that it recurses without
    // calling liveness(), so we have to be careful to store LIVENESS info here
    // too.
    e.getAttributes().set(LIVENESS, onEntry);
    if (e instanceof Operation) {
      Operation op = (Operation) e;
      switch (op.getOperator()) {
        case LOGICAL_AND: {
          ConditionalLiveSet left = processLogicOperand(op, 0, onEntry);
          ConditionalLiveSet right = processLogicOperand(op, 1, left.truthy);
          return new ConditionalLiveSet(
              // It is truthy if both the left and right are truthy
              left.truthy.union(right.truthy),
              // It is falsey if either the left or right are falsey
              left.falsey.intersection(right.falsey));
        }
        case LOGICAL_OR: {
          ConditionalLiveSet left = processLogicOperand(op, 0, onEntry);
          ConditionalLiveSet right = processLogicOperand(op, 1, left.falsey);
          return new ConditionalLiveSet(
              // It is truthy if either the left or right are truthy
              left.truthy.intersection(right.truthy),
              // It is falsey if both the left and right evaluate are falsey
              left.falsey.union(right.falsey));
        }
        case NOT:
          return processLogicOperand(op, 0, onEntry).inverse();
        default:
          break;
      }
    }
    LiveSet ls = processExpression(e, onEntry);
    return new ConditionalLiveSet(ls, ls);
  }

  private static ConditionalLiveSet processLogicOperand(
      Operation op, int opIdx, LiveSet onEntry) {
    return processCondition(op.children().get(opIdx), onEntry);
  }

  private static LiveSet processImmediatelyCalledFunction(
      Operation op, LiveSet onEntry) {
    List<? extends Expression> operands = op.children();
    FunctionConstructor fn = (FunctionConstructor) operands.get(0);
    // operands are resolved before the function body is executed
    LiveSet onExit = onEntry;
    for (Expression actual : operands.subList(1, operands.size())) {
      onExit = liveness(actual, onExit).vars;
    }
    LiveSet asAResultOfCallingFn = processFunctionConstructor(fn);
    return onExit.union(asAResultOfCallingFn);
  }

  /**
   * @return the live-set at the end of the function body.  Unlike other
   *     process methods, this does not return the live-set as a result of
   *     processing the declaration.
   */
  private static LiveSet processFunctionConstructor(FunctionConstructor fc) {
    // Process the function body, but do not the result since liveness does
    // not extend across function boundaries.
    LiveSet fnBodyDefs = new LiveSet(fc);
    for (FormalParam formal : fc.getParams()) {
      fnBodyDefs = fnBodyDefs.with(formal);
    }
    return liveness(fc.getBody(), fnBodyDefs).vars;
  }

  /** @param r unused. */
  private static LiveSet processReference(Reference r, LiveSet onEntry) {
    return onEntry;
  }

  private static LiveSet processLiteralOrConstructor(
      Expression e, LiveSet onEntry) {
    LiveSet last = onEntry;
    if (e instanceof ObjectConstructor) {
      for (ObjProperty p : ((ObjectConstructor) e).children()) {
        last = liveness(p.children().get(1), last).vars;
      }
    } else {
      for (ParseTreeNode child : e.children()) {
        Expression childE = (Expression) child;
        last = liveness(childE, last).vars;
      }
    }
    return last;
  }

  private static LiveCalc processDirectivePrologue(LiveSet onEntry) {
    return new LiveCalc(onEntry, ExitModes.COMPLETES);
  }

  /**
   * Incorporates the set of variables live at breaks into the live-set and
   * updates exit modes to take into account the way execution continues from
   * a labeled statement that is the target of a reachable {@code break}.
   */
  private static LiveCalc processLabel(LabeledStatement s, LiveCalc onExit) {
    String label = s.getLabel();
    LiveSet ls = onExit.vars;
    ExitModes em = onExit.exits;
    ExitModes.ExitMode breakSet = em.atBreak(label);
    if (breakSet != null) {
      if (breakSet.always) {
        // If the block always breaks, as is common for switch statements,
        // use the intersection of live-sets at the break points, as computed by
        // ExitModes.
        ls = breakSet.vars;
      } else {
        ls = ls.intersection(breakSet.vars);
      }
    }
    if (s.isTargetForContinue()) {
      // Do not intersect the liveset at continue with ls, since continues jump
      // to the start of the block, and so do not affect the live-set at the end
      // of the block in the same way that breaks do.
      em = em.withoutBreakOrContinue(label).withoutBreakOrContinue("");
    } else {
      em = em.withoutBreak(label).withoutBreak("");
    }
    return onExit.withVars(ls).withExits(em);
  }

  /**
   * The set of variables live once a node exits normally, and a description
   * of the ways in which control may leave a node.
   */
  static final class LiveCalc {
    final LiveSet vars;
    final ExitModes exits;

    private LiveCalc(LiveSet vars, ExitModes exits) {
      this.vars = vars;
      this.exits = exits;
    }

    private LiveCalc filter(LexicalScope containingScope) {
      return withVars(vars.filter(containingScope));
    }

    private LiveCalc withExits(ExitModes exits) {
      return exits == this.exits ? this : new LiveCalc(vars, exits);
    }

    private LiveCalc withVars(LiveSet vars) {
      return vars == this.vars ? this : new LiveCalc(vars, exits);
    }

    @Override
    public String toString() {
      return "[" + vars + " " + exits + "]";
    }
  }

  /**
   * For an expression whose result is used in a boolean computation, the set
   * of live variables broken down for each of the possible results.
   * <p>
   * E.g. in {@code (a = foo()) || (b = bar())} we can confidently say that
   * when the result is a truthy value
   */
  private static final class ConditionalLiveSet {
    /**
     * The set of variables live if the expression produces a value {@code v}
     * such that {@code !!v === true}.
     */
    final LiveSet truthy;
    /**
     * The set of variables live if the expression produces a value {@code v}
     * such that {@code !!v === false}.
     */
    final LiveSet falsey;

    ConditionalLiveSet(LiveSet truthy, LiveSet falsey) {
      this.truthy = truthy;
      this.falsey = falsey;
    }

    ConditionalLiveSet inverse() {
      return new ConditionalLiveSet(falsey, truthy);
    }

    @Override
    public String toString() {
      return "(truthy=" + truthy + ", falsey=" + falsey + ")";
    }
  }
}
