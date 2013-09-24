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
import com.google.caja.parser.Visitor;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.DirectivePrologue;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.ForEachLoop;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.MultiDeclaration;
import com.google.caja.parser.js.Noop;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.util.Lists;
import com.google.caja.util.Pair;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Collects all the {@code var} declarations in a function body or program into
 * one var statement at the top and folds assignment statements into
 * initializers.
 *
 * @author mikesamuel@gmail.com
 */
public class VarCollector {
  public static void optimize(Block program) {
    optimize(program, Collections.<Identifier>emptySet());
  }

  private static void optimize(Block body, Set<Identifier> alreadyDeclared) {
    Set<Identifier> locals = newIdentSet();
    List<FunctionConstructor> inners = Lists.newArrayList();
    unvar(body, locals, inners);
    Set<Identifier> needed = newIdentSet(locals);
    needed.removeAll(alreadyDeclared);
    if (!needed.isEmpty()) {
      List<Operation> topAssignments = extractAssignments(
          body, newIdentSet(locals));
      List<Declaration> decls = Lists.newArrayList();
      for (Operation topAssign : topAssignments) {
        Identifier id = ((Reference) topAssign.children().get(0))
            .getIdentifier();
        decls.add(new Declaration(
            topAssign.getFilePosition(), id, topAssign.children().get(1)));
        needed.remove(id);
      }
      for (Identifier name : needed) {
        decls.add(new Declaration(name.getFilePosition(), name, null));
      }
      Statement decl;
      if (decls.size() == 1) {
        decl = decls.get(0);
      } else {
        decl = new MultiDeclaration(
            FilePosition.startOf(body.getFilePosition()), decls);
      }
      Statement declFollower = null;
      List<? extends Statement> bodyChildren = body.children();
      if (!bodyChildren.isEmpty()) { declFollower = bodyChildren.get(0); }
      if (declFollower instanceof DirectivePrologue) {
        declFollower = bodyChildren.size() == 1 ? null : bodyChildren.get(1);
      }
      body.insertBefore(decl, declFollower);
    }
    for (FunctionConstructor inner : inners) {
      Set<Identifier> formals = newIdentSet();
      for (FormalParam p : inner.getParams()) {
        formals.add(p.getIdentifier());
      }
      optimize(inner.getBody(), formals);
    }
  }

  private static void unvar(
      Block node, final Set<Identifier> removedIdents,
      final List<FunctionConstructor> inners) {
    final List<Pair<AncestorChain<Statement>, Statement>> changes
        = Lists.newArrayList();
    node.acceptPreOrder(new Visitor() {
      public boolean visit(AncestorChain<?> chain) {
        ParseTreeNode node = chain.node;
        if (node instanceof Declaration
            && !(node instanceof FunctionDeclaration)) {
          if (chain.parent.node instanceof CatchStmt) { return true; }
          Declaration decl = (Declaration) node;
          Identifier id = decl.getIdentifier();
          removedIdents.add(id);
          Expression init = decl.getInitializer();
          Statement replacement;
          if (init != null) {
            replacement = new ExpressionStmt(toAssignment(decl));
          } else if (chain.parent.node instanceof ForEachLoop) {
            replacement = new ExpressionStmt(new Reference(id));
          } else {
            replacement = new Noop(decl.getFilePosition());
          }
          changes.add(Pair.pair(chain.cast(Statement.class), replacement));
          return true;
        } else if (node instanceof MultiDeclaration) {
          List<Expression> replacements = Lists.newArrayList();
          for (Declaration decl : ((MultiDeclaration) node).children()) {
            removedIdents.add(decl.getIdentifier());
            if (decl.getInitializer() == null) { continue; }
            visit(chain.child(decl).child(decl.getInitializer()));
            Expression assign = toAssignment(decl);
            replacements.add(assign);
          }
          Statement replacement;
          if (replacements.isEmpty()) {
            replacement = new Noop(node.getFilePosition());
          } else if (replacements.size() == 1) {
            Expression e = replacements.get(0);
            replacement = new ExpressionStmt(e.getFilePosition(), e);
          } else if (chain.parent.node instanceof Block) {
            List<Statement> stmts = Lists.newArrayList();
            for (Expression e : replacements) {
              stmts.add(new ExpressionStmt(e));
            }
            replacement = new Block(node.getFilePosition(), stmts);
          } else {
            Expression combo = null;
            for (Expression e : replacements) {
              combo = combo == null
                  ? e : Operation.createInfix(Operator.COMMA, combo, e);
            }
            replacement = new ExpressionStmt(node.getFilePosition(), combo);
          }
          changes.add(Pair.pair(chain.cast(Statement.class), replacement));
          return false;
        } else if (node instanceof FunctionConstructor) {
          inners.add((FunctionConstructor) node);
          return false;
        }
        return true;
      }
    }, null);
    for (Pair<AncestorChain<Statement>, Statement> change : changes) {
      change.a.parent.cast(MutableParseTreeNode.class).node
          .replaceChild(change.b, change.a.node);
    }
  }

  private static Expression toAssignment(Declaration decl) {
    return Operation.create(
        decl.getFilePosition(), Operator.ASSIGN,
        new Reference(decl.getIdentifier()), decl.getInitializer());
  }

  private static List<Operation> extractAssignments(
      Block body, Set<Identifier> unassigned) {
    List<Operation> extracted = Lists.newArrayList();
    while (!body.children().isEmpty()) {
      Statement first = body.children().get(0);
      if (first instanceof Noop) {
        body.removeChild(first);
        continue;
      }
      if (first instanceof Block) {
        extracted.addAll(extractAssignments((Block) first, unassigned));
        if (first.children().isEmpty()) {
          body.removeChild(first);
        } else {
          break;
        }
      }
      if (!(first instanceof ExpressionStmt)) { break; }
      Expression e = ((ExpressionStmt) first).getExpression();
      if (!Operation.is(e, Operator.ASSIGN)) { break; }
      Operation op = (Operation) e;
      Expression lhs = op.children().get(0);
      if (!(lhs instanceof Reference)) { break; }
      Reference r = (Reference) lhs;
      if (!unassigned.contains(r.getIdentifier())) { break; }
      // Don't return two with the same name, because we don't want to have
      // multiple var declarations for the same name.
      //     var foo = 1, bar = 2, foo = 3;
      // might not be legal in ES5 strict mode.
      unassigned.remove(r.getIdentifier());
      extracted.add(op);
      body.removeChild(first);
    }
    return extracted;
  }

  private static Set<Identifier> newIdentSet() {
    return Sets.newTreeSet(new Comparator<Identifier>() {
      public int compare(Identifier a, Identifier b) {
        return a.getName().compareTo(b.getName());
      }
    });
  }

  private static Set<Identifier> newIdentSet(Set<Identifier> c) {
    Set<Identifier> idents = newIdentSet();
    idents.addAll(c);
    return idents;
  }
}
