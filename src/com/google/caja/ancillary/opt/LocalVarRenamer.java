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

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.WithStmt;
import com.google.caja.parser.js.scope.ScopeType;
import com.google.caja.parser.quasiliteral.Scope;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Iterators;
import com.google.caja.util.SafeIdentifierMaker;
import com.google.caja.util.Sets;
import com.google.caja.util.SyntheticAttributeKey;

import java.util.Iterator;
import java.util.Set;

/**
 * Renames all local variables in scopes not visible to calls to {@code eval}
 * to shorten names.  This change is semantics preserving on any valid ES 262
 * interpreter that throws an {@code EvalError} when {@code eval} is used via an
 * alias.
 *
 * @author mikesamuel@gmail.com
 */
public final class LocalVarRenamer {
  private final MessageQueue mq;

  /**
   * @param mq will receive warnings about scoping oddities.
   */
  public LocalVarRenamer(MessageQueue mq) { this.mq = mq; }

  private static final SyntheticAttributeKey<ScopeInfo> SCOPE
      = new SyntheticAttributeKey<ScopeInfo>(ScopeInfo.class, "scope");
  public Block optimize(Block program) {
    // Don't modify the input.
    program = (Block) program.clone();
    // Mark each node with a scope-wrapper that makes it easier to track
    // variable usage.
    attachScopes(AncestorChain.instance(program), new ScopeInfo(program, mq));
    // Walk the tree assigning names efficiently.
    assignNames(program.getAttributes().get(SCOPE).parent);
    // Modify the tree to use the assigned names.
    rename(program);
    return program;
  }

  private static boolean attachScopes(AncestorChain<?> ac, ScopeInfo scope) {
    // We infect scopes root-wards if we see a problematic construct like
    // "eval" or "with" which would change behavior if variables in scope where
    // it is declared were renamed.
    boolean infected = false;
    ParseTreeNode n = ac.node;
    n.getAttributes().set(SCOPE, scope);
    if (n instanceof FunctionConstructor) {
      FunctionConstructor fc = (FunctionConstructor) n;
      scope = new ScopeInfo(scope, Scope.fromFunctionConstructor(scope.s, fc));
      if (fc.getIdentifierName() != null) {
        scope.fns.add(ac.cast(FunctionConstructor.class));
      }
      // A ctor's name is apparent in its scope, unlike a fn declarations name
      // which is apparent in the containing scope.
      n.getAttributes().set(SCOPE, scope);
    } else if (n instanceof CatchStmt) {
      CatchStmt cs = (CatchStmt) n;
      scope = new ScopeInfo(scope, Scope.fromCatchStmt(scope.s, cs));
      // Normally, declaration in a catch block are hoisted to the parent.
      // Since the logic below does that, make sure that the exception
      // declaration is not hoisted.
      scope.decls.add(AncestorChain.instance(ac, cs.getException()));
      cs.getException().getAttributes().set(SCOPE, scope);
      // And recurse to the body manually so as to avoid recursing to the
      // exception declaration.
      attachScopes(AncestorChain.instance(ac, cs.getBody()), scope);
      return false;
    } else if (n instanceof Reference) {
      Reference r = (Reference) n;
      String rName = r.getIdentifierName();
      Scope definingScope = scope.s.thatDefines(rName);
      assert (definingScope != null) || scope.s.isOuter(rName) : rName;
      scope.uses.add(new Use(scope.withScope(definingScope), rName));
      if ("eval".equals(rName)) { infected = true; }
      infected = infected || "eval".equals(rName);
    } else if (n instanceof Declaration) {
      ScopeInfo declaring = scope;
      // Hoist out of catch block scopes.
      while (declaring.s.getType() == ScopeType.CATCH) {
        declaring = declaring.parent;
      }
      declaring.decls.add(ac.cast(Declaration.class));
    } else if (n instanceof WithStmt) {
      // References inside with(...){} could be variable names or they could
      // be property names.
      infected = true;
    } else if (Operation.is(n, Operator.MEMBER_ACCESS)) {
      // Do not let the property name reference be treated as a reference to
      // a var or global.
      attachScopes(AncestorChain.instance(ac, n.children().get(0)), scope);
      return false;
    }
    for (ParseTreeNode child : n.children()) {
      infected |= attachScopes(AncestorChain.instance(ac, child), scope);
    }
    if (infected) { scope.setDynamicUsePossible(); }
    return infected;
  }

  /**
   * @param scope the set of uses of names defined in ancestor scopes in usage
   *    and its descendants.
   */
  private static void assignNames(ScopeInfo scope) {
    Set<Use> outerUses = Sets.newHashSet();
    addUsedOuters(scope, scope.depth, outerUses);
    // Compute the set of names used in this scope and children which cannot
    // be masked by newly chosen names.
    Set<String> alreadyUsed = Sets.newHashSet(scope.mapping.values());
    for (Use u : outerUses) {
      String name = u.definingScope != null
          ? u.definingScope.mapping.get(u.origName) : null;
      // Happens if global, or if not renamed because eval is used in an outer
      // scope.
      if (name == null) { name = u.origName; }
      alreadyUsed.add(name != null ? name : u.origName);
    }

    Iterator<String> namer = Iterators.filter(
        new SafeIdentifierMaker(), alreadyUsed);
    for (AncestorChain<Declaration> d : scope.decls) {
      String dName = d.node.getIdentifierName();
      if (scope.mapping.containsKey(dName)) { continue; }  // Skip duplicates
      String newName = scope.isDynamicUsePossible() ? dName : namer.next();
      scope.mapping.put(dName, newName);
    }

    // Allocate names of function constructors in children to make sure
    // that, on IE with its weird scoping rules, their names do not collide
    // with any locals.
    for (ScopeInfo inner : scope.inners) {
      for (AncestorChain<FunctionConstructor> f : inner.fns) {
        FunctionConstructor fc = f.node;
        String fnName = fc.getIdentifierName();
        assert fnName != null;
        if (inner.mapping.containsKey(fnName)) { continue; }  // Skip duplicates
        String newName;
        if (f.parent.node instanceof FunctionDeclaration) {
          // Use the same name as the enclosing function declaration.
          newName = scope.mapping.get(fnName);
        } else {
          // Allocate a new that will not conflict with that of containing
          // declaration in the presence of IE's weird scoping rules.
          //   var foo;
          //   var bar = function foo() {};
          // Now foo is a function!!!
          newName = inner.isDynamicUsePossible() ? fnName : namer.next();
        }
        inner.mapping.put(fnName, newName);
      }

      // Allocate exception names as if they were local function declarations.
      // This behavior, where we allocate a scope, and then flatten it, makes
      // sure we interpret uses based on user-intent, but generate names that
      // do not introduce collisions on IE6 with its broken scoping rules.
      if (!inner.isDynamicUsePossible()) {
        allocateExceptionNames(inner, namer);
      }

      assignNames(inner);
    }
  }

  /**
   * Work around IE problems where catch block exception variables pollute the
   * containing function scope by preallocating the names using the function
   * scopes namer.  Specifically, on IE,
   * <pre>
   * (function () {
   *   var ex = 'from outer';
   *   try {
   *     throw 'from try';
   *   } catch (ex) {
   *   }
   *   alert(ex);  // alerts 'from try' on IE, 'from outer' according to spec.
   * })();
   * </pre>
   *
   * @param namer generates unique, non-colliding names in the containing
   *     function scope.
   */
  private static void allocateExceptionNames(
      ScopeInfo scope, Iterator<String> namer) {
    if (scope.s.getType() == ScopeType.CATCH) {
      for (AncestorChain<Declaration> d : scope.decls) {
        String name = d.node.getIdentifierName();
        if (!scope.mapping.containsKey(name)) {
          scope.mapping.put(name, namer.next());
        }
      }
      for (ScopeInfo inner : scope.inners) {
        allocateExceptionNames(inner, namer);
      }
    }
  }


  /**
   * Compute the set of names defined in containing scopes, used in scope
   * and any inner scope.
   *
   * <p>
   * E.g., in <pre>
   * var a, b;
   * function f(n) {
   *   return a + n;
   * }
   * </pre>
   * for the scope of f's body, {@code a} is a used outer, and {@code b} is not
   * a used outer since it is not referenced, and {@code n} is not a used outer
   * since, although it is used, it is defined in that scope so not an outer.
   *
   * @param scope a scope at depth {@code depth} or a scope entirely contained
   *    by such a scope.
   * @param depth the depth of the scope that is considered the current scope.
   * @param used receives all outer uses.  Modified in place.
   */
  private static void addUsedOuters(ScopeInfo scope, int depth, Set<Use> used) {
    for (Use u : scope.uses) {
      if (u.definingScope == null || u.definingScope.depth < depth) {
        used.add(u);
      }
    }
    for (ScopeInfo c : scope.inners) {
      addUsedOuters(c, depth, used);
    }
  }

  private static void rename(ParseTreeNode n) {
    // Walk post order so that function constructors are renamed before any
    // function declaration that contains them.

    // All the mutation done by this optimizer is done here, and the only
    // mutation done is to replace Identifier nodes.
    n.acceptPostOrder(new Visitor() {
      public boolean visit(AncestorChain<?> ac) {
        ParseTreeNode n = ac.node;
        if (n instanceof Reference) {
          if (!(Operation.is(ac.parent.node, Operator.MEMBER_ACCESS)
                && n == ac.parent.node.children().get(1))) {
            renameOne((Reference) n);
          }
        } else if (n instanceof FunctionConstructor) {
          FunctionConstructor fc = (FunctionConstructor) n;
          if (fc.getIdentifierName() != null) { renameOne(fc); }
        } else if (n instanceof Declaration) {
          renameOne((Declaration) n);
        }
        return true;
      }
    }, null);
  }

  private static void renameOne(MutableParseTreeNode n) {
    Identifier id = (Identifier) n.children().get(0);
    String origName = id.getName();
    ScopeInfo u = n.getAttributes().get(SCOPE);
    Scope s = (n instanceof FunctionConstructor)
        ? u.s :  u.s.thatDefines(origName);
    if (s != null) {  // Will be null for undeclared globals.
      String newName = u.withScope(s).mapping.get(origName);
      if (!newName.equals(origName)) {
        n.replaceChild(new Identifier(id.getFilePosition(), newName), id);
      }
    }
  }
}
