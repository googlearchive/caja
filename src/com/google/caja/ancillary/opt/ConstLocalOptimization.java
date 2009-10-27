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
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.MultiDeclaration;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.OperatorCategory;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.RegexpLiteral;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.quasiliteral.Scope;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Multimap;
import com.google.caja.util.Multimaps;

/**
 * Inlines uses of local variables at the top of a block which are immediately
 * initialized with a literal value and which are never assigned to.
 * <p>
 * E.g. <pre>
 * function f() {
 *   var foo = 3;
 *   var bar = 4;
 *   return foo + bar;
 * }
 * </pre>
 * &rarr;
 * <pre>
 * function f() { return 3 + 4; }
 * </pre>
 *
 * @author mikesamuel@gmail.com
 */
public class ConstLocalOptimization {
  public static Block optimize(Block program, MessageQueue mq) {
    Block clone = (Block) program.clone();
    Optimizer opt = new Optimizer();
    AncestorChain<Block> root = AncestorChain.instance(clone);
    Scope scope = Scope.fromProgram(clone, mq);
    for (Statement s : clone.children()) {
      opt.examine(AncestorChain.instance(root, s), scope);
    }
    opt.finish();
    return opt.changed ? clone : program;
  }
}

class Optimizer {
  final Multimap<Var, AncestorChain<?>> uses = Multimaps.newListHashMultimap();
  boolean changed;

  void examine(AncestorChain<?> ac, Scope s) {
    ParseTreeNode n = ac.node;
    if (n instanceof FunctionConstructor) {
      FunctionConstructor fn = (FunctionConstructor) n;
      s = Scope.fromFunctionConstructor(s, fn);
      AncestorChain<Block> body = AncestorChain.instance(ac, fn.getBody());
      for (Statement fnBodyStmt : body.node.children()) {
        if (!examineDeclaration(AncestorChain.instance(body, fnBodyStmt), s)) {
          break;
        }
      }
    } else if (n instanceof CatchStmt) {
      s = Scope.fromCatchStmt(s, ((CatchStmt) n));
    } else if (n instanceof Operation
               && Operator.MEMBER_ACCESS == n.getValue()) {
      examine(AncestorChain.instance(ac, n.children().get(0)), s);
      return;
    } else if (n instanceof Reference) {
      Identifier id = (Identifier) n.children().get(0);
      Scope defining = s.thatDefines(id.getName());
      if (defining != null) { uses.put(new Var(id, defining), ac); }
    }
    for (ParseTreeNode child : n.children()) {
      examine(AncestorChain.instance(ac, child), s);
    }
  }

  private boolean examineDeclaration(AncestorChain<Statement> ac, Scope s) {
    Statement stmt = ac.node;
    if (stmt instanceof MultiDeclaration) {
      for (Declaration d : ((MultiDeclaration) stmt).children()) {
        if (!examineDeclaration(AncestorChain.instance(ac, (Statement) d), s)) {
          return false;
        }
      }
      return true;
    } else if (stmt instanceof Declaration) {
      Declaration d = (Declaration) stmt;
      String name = d.getIdentifierName();
      if (s.isData(name)) { uses.put(new Var(d.getIdentifier(), s), ac); }
      return true;
    }
    return false;
  }

  void finish() {
    var:
    for (Var v : uses.keySet()) {
      Iterable<AncestorChain<?>> acs = uses.get(v);
      // If there is exactly one declaration which has a value,
      // and no assignments, then inline.
      Expression value = null;
      FilePosition declPos = null;
      for (AncestorChain<?> ac : acs) {
        if (ac.node instanceof Declaration) {
          Declaration d = ac.cast(Declaration.class).node;
          declPos = d.getFilePosition();
          if (d.getInitializer() != null) {
            if (value != null || !isConst(d.getInitializer())) {
              continue var;
            }
            value = d.getInitializer();
          }
        } else {
          AncestorChain<Reference> r = ac.cast(Reference.class);
          if (r.parent.node instanceof Operation
              && r.node == r.parent.node.children().get(0)) {
            Operator op = r.parent.cast(Operation.class).node.getOperator();
            if (op.getCategory() == OperatorCategory.ASSIGNMENT) {
              continue var;
            }
          }
        }
      }
      if (declPos == null) { continue; }
      if (value == null) {
        value = Operation.create(
            declPos, Operator.VOID, new IntegerLiteral(declPos, 0));
      }
      changed = true;
      for (AncestorChain<?> ac : acs) {
        if (ac.node instanceof Reference) {
          Reference ref = ac.cast(Reference.class).node;
          Expression subst = ParseTreeNodes.newNodeInstance(
              value.getClass(), ref.getFilePosition(),
              value.getValue(), value.children());
          ac.parent.cast(MutableParseTreeNode.class).node.replaceChild(
              subst, ref);
        } else {  // Remove the declaration
          AncestorChain<?> toRemove = ac;
          if (toRemove.parent.node instanceof MultiDeclaration
              && 1 == toRemove.parent.node.children().size()) {
            toRemove = toRemove.parent;
          }
          toRemove.parent.cast(MutableParseTreeNode.class).node.removeChild(
              toRemove.node);
        }
      }
    }
  }

  private static boolean isOperation(Operator op, Expression e) {
    return e instanceof Operation && op == ((Operation) e).getOperator();
  }

  private static boolean isConst(Expression expr) {
    return (expr instanceof Literal && !(expr instanceof RegexpLiteral))
        || (isOperation(Operator.VOID, expr)
            && isConst((Expression) expr.children().get(0)));
  }
}

final class Var {
  final String name;
  final Scope s;

  Var(Identifier id, Scope s) {
    this.name = id.getName();
    this.s = s;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Var)) { return false; }
    Var that = (Var) o;
    return this.s == that.s && this.name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode() + 31 * s.hashCode();
  }

  @Override
  public String toString() {
    return "[Var " + name + " in " + s + "]";
  }
}