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
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.MultiDeclaration;
import com.google.caja.parser.js.ObjProperty;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.RegexpLiteral;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.ValueProperty;
import com.google.caja.parser.js.scope.AbstractScope;
import com.google.caja.parser.js.scope.ES5ScopeAnalyzer;
import com.google.caja.parser.js.scope.ScopeListener;
import com.google.caja.parser.js.scope.ScopeType;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;
import com.google.caja.util.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * <p>
 * We do not muck with initializations inside a ternary or short-circuiting
 * logic op or in conditionals or loops, since those can be executed multiple
 * times or not at all.
 * And we don't muck with initializations in try blocks since reads could be
 * inside catch or finally which might get invoked when set fails.
 *
 * @author mikesamuel@gmail.com
 */
public class ConstLocalOptimization {
  public static Block optimize(Block program) {
    Block clone = (Block) program.clone();
    while (true) {
      Optimizer opt = new Optimizer();
      opt.examine(AncestorChain.instance(clone));
      opt.finish();
      if (!opt.changed) { return program; }
      program = clone;
    }
  }
}

class Optimizer {
  final Set<Var> vars = Sets.newLinkedHashSet();
  final Map<AncestorChain<?>, Integer> positions = Maps.newHashMap();
  boolean changed;

  void examine(AncestorChain<?> program) {
    ES5ScopeAnalyzer<OptScope> sa = new ES5ScopeAnalyzer<OptScope>(
        new ScopeListener<OptScope>() {
          public void assigned(
              AncestorChain<Identifier> id, OptScope useSite,
              OptScope definingSite) {
            if (definingSite != null) {
              definingSite.requireSymbol(id.node.getName()).writes.add(id);
            }
          }

          public OptScope createScope(
              ScopeType t, AncestorChain<?> root, OptScope parent) {
            return new OptScope(parent, t, root);
          }

          public void declaration(AncestorChain<Identifier> id, OptScope s) {
            s.requireSymbol(id.node.getName()).decls.add(id);
          }

          public void duplicate(AncestorChain<Identifier> id, OptScope scope) {
            // noop
          }

          public void enterScope(OptScope Scope) { /* noop */ }

          public void exitScope(OptScope scope) {
            if (scope.t == ScopeType.FUNCTION) {
              AncestorChain<FunctionConstructor> fc = scope.root.cast(
                  FunctionConstructor.class);
              AncestorChain<Block> body = fc.child(fc.node.getBody());
              for (Statement fnBodyStmt : body.node.children()) {
                if (!examineDeclaration(body.child(fnBodyStmt), scope)) {
                  break;
                }
              }
            }
          }

          public void inScope(AncestorChain<?> ac, OptScope scope) {
            int pos = positions.size();
            positions.put(ac, pos);
            // Identify non-local transfer of control.
            OptScope dc = scope;
            while (!dc.t.isDeclarationContainer) { dc = dc.containing; }
            if (dc.earliestNonLocalXfer == Integer.MAX_VALUE
                && ac.node instanceof Operation) {
              Operator op = ((Operation) ac.node).getOperator();
              switch (op) {
                case FUNCTION_CALL: case CONSTRUCTOR: case MEMBER_ACCESS:
                case SQUARE_BRACKET:
                  dc.earliestNonLocalXfer = pos;
                  break;
                default: break;
              }
            }
          }

          public void masked(
              AncestorChain<Identifier> id, OptScope inner, OptScope outer) {
            // noop
          }

          public void read(
              AncestorChain<Identifier> id, OptScope useSite,
              OptScope definingSite) {
            if (definingSite != null) {
              definingSite.requireSymbol(id.node.getName()).reads.add(id);
            }
          }

          public void splitInitialization(
              AncestorChain<Identifier> declared, OptScope declScope,
              AncestorChain<Identifier> initialized, OptScope maskingScope) {
            // noop
          }
        });
    sa.apply(program);
  }

  private boolean examineDeclaration(AncestorChain<Statement> ac, OptScope s) {
    Statement stmt = ac.node;
    if (stmt instanceof MultiDeclaration) {
      for (Declaration d : ((MultiDeclaration) stmt).children()) {
        if (!examineDeclaration(ac.child((Statement) d), s)) {
          return false;
        }
      }
      return true;
    } else if (stmt instanceof Declaration) {
      vars.add(new Var(((Declaration) stmt).getIdentifier(), s));
      return true;
    }
    return false;
  }

  void finish() {
    // Walk over the list of variables that appear at the top of a function
    // body.
    var:
    for (Var v : vars) {
      if ("arguments".equals(v.name)) { continue; }  // special in function body
      Symbol s = v.s.getSymbol(v.name);
      Expression value;
      int initPos;
      boolean isConst;
      AncestorChain<Identifier> write;

      switch (s.writes.size()) {
        case 1:
          write = s.writes.get(0);
          if (write.parent.node instanceof Declaration) {
            initPos = write.parent.node instanceof FunctionDeclaration
                ? -1  // hoisted
                : positions.get(write);
            AncestorChain<Declaration> d = write.parent.cast(Declaration.class);
            value = d.node.getInitializer();

            isConst = isConst(value);
            if (!isConst
                && (!isSinglyInlineable(value) || s.reads.size() > 1)) {
              continue var;
            }
          } else {
            continue var;
          }
          break;
        case 0:
          initPos = -1;
          value = null;
          write = null;
          isConst = true;
          break;
        default:
          continue var;
      }

      // If there is at most one assignment at the top level of the function
      // and all reads occur after that, and the value is constant, then
      // inline uses.

      // TODO: we can safely inline the reads that are okay.

      // TODO: check that reads are in the same function
      // or before the first call or member dereference op.

      int nInlined = 0;
      for (AncestorChain<Identifier> read : s.reads) {
        // If the read occur afterwards lexically, then, since we
        // know the declaration appears at the top of a run of
        // declarations in a function body, then it isn't being read by
        // any of the earlier declarations ; so the read must happen
        // after the var is initialized.
        if (positions.get(read) < initPos) { continue; }
        if (write != null && !inSameFn(write, read)) {
          // Can't reuse reference values across function boundaries.
          if (!isConst) { continue; }
          // If the use is across function boundaries, and there might have
          // been a non-local transfer of control, then we can't inline.
          if (initPos > v.s.earliestNonLocalXfer) { continue; }
        }

        AncestorChain<Reference> toReplace = read.parent.cast(Reference.class);
        Expression repl;
        if (value == null) {
          FilePosition fp = toReplace.node.getFilePosition();
          repl = Operation.create(fp, Operator.VOID, new IntegerLiteral(fp, 0));
        } else {
          repl = (Expression) value.clone();
        }
        toReplace.parent.cast(MutableParseTreeNode.class)
            .node.replaceChild(repl, toReplace.node);
        ++nInlined;
        changed = true;
      }

      if (nInlined == s.reads.size()) {
        for (AncestorChain<Identifier> decl : s.decls) {
          AncestorChain<?> toRemove = decl.parent;
          if (toRemove.node instanceof FormalParam) { continue; }
          if (toRemove.parent.node instanceof MultiDeclaration
              && toRemove.parent.node.children().size() == 1) {
            toRemove = toRemove.parent;
          }
          toRemove.parent.cast(MutableParseTreeNode.class)
              .node.removeChild(toRemove.node);
          changed = true;
        }
      }
    }
  }

  private static boolean isSinglyInlineable(Expression expr) {
    return isConst(expr)
        || expr instanceof StringLiteral  // inline large string literal once
        || expr instanceof RegexpLiteral
        || (expr instanceof ObjectConstructor
            && areSinglyInlineableProps(((ObjectConstructor) expr).children()))
        || (expr instanceof ArrayConstructor
            && areSinglyInlineable(((ArrayConstructor) expr).children()))
        || expr instanceof FunctionConstructor;
  }

  private static boolean areSinglyInlineable(List<? extends Expression> exprs) {
    for (Expression e : exprs) {
      if (!isSinglyInlineable(e)) { return false; }
    }
    return true;
  }

  private static boolean areSinglyInlineableProps(
      List<? extends ObjProperty> props) {
    for (ObjProperty prop : props) {
      if (prop instanceof ValueProperty
          && !isSinglyInlineable(((ValueProperty) prop).getValueExpr())) {
        return false;
      }
      // getters and setters are inlineable.
    }
    return true;
  }

  private static boolean isConst(Expression expr) {
    if (expr instanceof Literal) {
      if (expr instanceof RegexpLiteral) { return false; }
      if (expr instanceof StringLiteral) {
        // Don't inline large strings more than once.
        // TODO(mikesamuel): 20 is a tuning parameter.
        return ((StringLiteral) expr).getValue().length() < 20;
      }
      return true;
    }
    return (Operation.is(expr, Operator.VOID)
            && areConst(((Operation) expr).children()));
  }

  private static boolean areConst(List<? extends Expression> exprs) {
    for (Expression e : exprs) {
      if (!isConst(e)) { return false; }
    }
    return true;
  }

  private static boolean inSameFn(AncestorChain<?> a, AncestorChain<?> b) {
    while (a != null && !(a.node instanceof FunctionConstructor)) {
      a = a.parent;
    }
    while (b != null && !(b.node instanceof FunctionConstructor)) {
      b = b.parent;
    }
    return a != null && a.equals(b);
  }
}

final class Var {
  final String name;
  final OptScope s;

  Var(Identifier id, OptScope s) {
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

final class OptScope implements AbstractScope {
  final OptScope containing;
  final ScopeType t;
  final Map<String, Symbol> symbols = Maps.newLinkedHashMap();
  final AncestorChain<?> root;
  int earliestNonLocalXfer = Integer.MAX_VALUE;

  OptScope(
      OptScope containing, ScopeType t, AncestorChain<?> root) {
    this.containing = containing;
    this.t = t;
    this.root = root;
  }

  public AbstractScope getContainingScope() { return containing; }
  public ScopeType getType() { return t; }
  public boolean isSymbolDeclared(String name) {
    return symbols.containsKey(name);
  }
  public Symbol getSymbol(String name) {
    OptScope os = this;
    do {
      Symbol s = symbols.get(name);
      if (s != null) { return s; }
      os = os.containing;
    } while (os != null);
    return null;
  }
  public Symbol requireSymbol(String name) {
    Symbol s = symbols.get(name);
    if (s == null) {
      s = new Symbol();
      symbols.put(name, s);
    }
    return s;
  }
}

final class Symbol {
  List<AncestorChain<Identifier>> decls = Lists.newArrayList(),
      reads = Lists.newArrayList(),
      writes = Lists.newArrayList();
}
