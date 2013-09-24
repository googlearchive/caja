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

package com.google.caja.parser.js.scope;

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.ForEachLoop;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.OperatorCategory;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.WithStmt;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

/**
 * Examines a snippet of JavaScript code, and publishes events to a
 * {@link ScopeListener listener} about declarations in the code, their uses,
 * and possibly noteworthy conditions such as masking relationships, duplicate
 * declarations, etc.
 *
 * <h2>Glossary</h2>
 * <dl>
 *   <dt>Name</dt>
 *     <dd>A JavaScript identifier or special keyword like {@code this}.
 *   <dt>Scope</dt>
 *     <dd>A set of names defined in a program AST and any meta-data about those
 *       definitions or uses of those definitions.  When a name is defined in
 *       a scope, then uses will not fall-through to a parent scope.
 *   <dt>Containing Scope</dt>
 *     <dd>A scope "outer" contains "inner" if "outer" corresponds to an AST
 *       that entirely contains the subtree to which "inner" corresponds.
 *   <dt>Symbol</dt>
 *     <dd>A name in a scope.</dd>
 *   <dt>Declaration</dt>
 *     <dd>A program construct whose purpose is to add a name to the set of
 *       names defined in a scope.  The set of declarations for a scope is
 *       statically determinable.</dd>
 *   <dt>Defined</dt>
 *     <dd>The state a name is in when a particular scope includes a declaration
 *       for it ({@code var x}), or when that name has a special meaning in that
 *       scope ({@code this}).  The set of names defined in a scope is not
 *       statically determinable, e.g. in the case of object properties
 *       aliased by {@code with} blocks, or in the case of global variables
 *       introduced by {@code eval}ed code.  The set of names defined in a scope
 *       is a superset of those declared.
 *   <dt>Masking</dt>
 *     <dd>A symbol masks another symbol when they have the same name and the
 *       first appears in a scope that is wholly contained in the other's scope
 *       so that any uses of the name will not resolve to the symbol in the
 *       containing scope.  A symbol does not mask itself.
 *       E.g. in <code>var x; function (x) { ... x ... }}</code> the formal
 *       parameter {@code x} masks the variable {@code x} since uses of
 *       {@code x} in the function no longer resolve to the variable.
 * </dl>
 *
 * @param <S> the type of scope used by this analyzer to match references to
 *    a symbol to the scope in which that symbol is defined.
 * @author mikesamuel@gmail.com
 */
public abstract class ScopeAnalyzer<S extends AbstractScope> {
  private final ScopeListener<S> listener;

  protected ScopeAnalyzer(ScopeListener<S> listener) {
    if (listener == null) { throw new NullPointerException(); }
    this.listener = listener;
  }

  /**
   * @return true if, as in standard EcmaScript, a named function literal
   *     introduces a name into its body.
   */
  protected abstract boolean fnCtorsDeclareInBody();
  /**
   * @return true if, as in JScript's nonstandard scoping, a named function
   *     literal introduces a declaration in the containing scope.
   */
  protected abstract boolean fnCtorsDeclareInContaining();

  /**
   * Publishes events about the given JS parse tree to the listener passed to
   * the constructor.
   */
  public S apply(AncestorChain<? extends ParseTreeNode> ac) {
    ScopeTree<S> root = buildScopeTree(ac, null);
    publishEvents(root);
    return root.scopeImpl;
  }

  /** Build scope tree and collect declarations and uses. */
  private ScopeTree<S> buildScopeTree(AncestorChain<?> ac, ScopeTree<S> outer) {
    ScopeTree<S> scope = outer;
    ScopeType t = ScopeType.forNode(ac.node);
    if (outer == null && (t == null || !t.isDeclarationContainer)) {
      t = ScopeType.PROGRAM;
    }
    if (t != null) {  // Start a new scope
      scope = new ScopeTree<S>(
          outer, t, listener.createScope(t, ac, scopeImpl(outer)));
      if (t == ScopeType.WITH) {
        // With blocks are odd.  A scope corresponds to a tree, but in
        //   with (obj) body
        // obj is resolved in a different LexicalEnvironment than body, so
        // cannot be said to be a scope.
        // But we can't say that the scope corresponds just to body, since in
        //   with (obj) (function () { ... })
        // the body introduces a scope.  This will become more problematic when
        // we deal with let-scoped declarations.
        AncestorChain<WithStmt> with = ac.cast(WithStmt.class);
        buildScopeTree(with.child(with.node.getScopeObject()), outer);
        buildScopeTree(with.child(with.node.getBody()), scope);
        return scope;
      }
    }
    scope.inScope.add(ac);
    if (ac.node instanceof Declaration) {
      AncestorChain<Declaration> d = ac.cast(Declaration.class);
      AncestorChain<Identifier> id = d.child(d.node.getIdentifier());
      Symbol<S> symbol = new Symbol<S>(id, scope);
      hoist(id, scope).declarations.add(symbol);
      if (d.node.getInitializer() != null || isKeyReceiver(d)) {
        scope.uses.add(symbol);
      }
    } else if (ac.node instanceof Reference) {
      AncestorChain<Reference> r = ac.cast(Reference.class);
      if (!isPropertyName(r)) {
        scope.uses.add(new Symbol<S>(r.child(r.node.getIdentifier()), scope));
      }
    } else if (ac.node instanceof FunctionConstructor) {
      AncestorChain<FunctionConstructor> f = ac.cast(FunctionConstructor.class);
      AncestorChain<Identifier> id = f.child(f.node.getIdentifier());
      if (id.node.getName() != null) {
        if (fnCtorsDeclareInBody()) {  // Standard function scoping
          scope.declarations.add(new Symbol<S>(id, scope));
        }
        if (fnCtorsDeclareInContaining()  // IE style function scoping
            && ac.parent != null
            && !(ac.parent.node instanceof FunctionDeclaration)) {
          scope.outer.declarations.add(new Symbol<S>(id, scope.outer));
        }
      }
    }
    for (ParseTreeNode child : ac.node.children()) {
      buildScopeTree(ac.child(child), scope);
    }
    return scope;
  }

  private void publishEvents(ScopeTree<S> s) {
    S scopeImpl = s.scopeImpl;
    listener.enterScope(scopeImpl);
    for (AncestorChain<?> ac : s.inScope) { listener.inScope(ac, s.scopeImpl); }
    for (Symbol<S> decl : s.declarations) { declare(decl.id, decl.useScope); }
    // Recurse to inner scopes before handling uses so that all hoisted
    // declarations are taken into account before uses are resolved.
    for (ScopeTree<S> inner : s.innerScopes) { publishEvents(inner); }
    for (Symbol<S> use : s.uses) { handleUse(use.id, use.useScope); }
    listener.exitScope(scopeImpl);
  }

  private void handleUse(AncestorChain<Identifier> id, ScopeTree<S> s) {
    ParseTreeNode n = id.parent.node;
    if (n instanceof Reference) {
      // Now that we're done with all the declaration in the scope, we can
      // tell whether a use corresponds to a declaration in the scope.
      String symbolName = id.node.getName();
      ScopeTree<S> defSite = definingSite(symbolName, s);
      Operator assignOperator = assignOperator(id);
      if (assignOperator == null) {
        listener.read(id, s.scopeImpl, scopeImpl(defSite));
      } else if (assignOperator == Operator.ASSIGN) {
        listener.assigned(id, s.scopeImpl, scopeImpl(defSite));
      } else {  // ++foo, foo++, foo += 1 all read before assignment
        listener.read(id, s.scopeImpl, scopeImpl(defSite));
        listener.assigned(id, s.scopeImpl, scopeImpl(defSite));
      }
    } else if (n instanceof Declaration) {
      ScopeTree<S> defSite = definingSite(id.node.getName(), s);
      listener.assigned(id, s.scopeImpl, scopeImpl(defSite));
    } else {
      throw new ClassCastException("Unexpected use " + n);
    }
  }

  /**
   * If the given identifier is the target of an assignment, then returns the
   * operator that is assigning it.  Otherwise returns null.
   */
  private static Operator assignOperator(AncestorChain<Identifier> ac) {
    if (ac.parent == null) { return null; }
    if (!(ac.parent.node instanceof Reference)) { return null; }
    AncestorChain<?> grandparent = ac.parent.parent;
    if (grandparent == null) { return null; }
    if (grandparent.node instanceof Operation) {
      // Handles ++ac, ac += ..., ac = ...
      Operation op = grandparent.cast(Operation.class).node;
      Operator operator = op.getOperator();
      return (operator.getCategory() == OperatorCategory.ASSIGNMENT
              && ac.parent.node == op.children().get(0)) ? operator : null;
    } else if (grandparent.node instanceof ExpressionStmt
               && grandparent.parent != null
               && grandparent.parent.node instanceof ForEachLoop) {
      // Handle
      //    for (k in obj) { ... }
      ForEachLoop loop = grandparent.parent.cast(ForEachLoop.class).node;
      if (grandparent.node == loop.getKeyReceiver()) { return Operator.ASSIGN; }
    }
    return null;
  }

  /**
   * The scope in which the named symbol is defined or null if it is a free
   * variable.
   * @param useSite the scope in which symbol is referenced.
   */
  private static <S>
  ScopeTree<S> definingSite(String symbolName, ScopeTree<S> useSite) {
    if ("this".equals(symbolName)) {
      // "this" is defined in function & program scopes, and cannot be declared.
      for (ScopeTree<S> s = useSite; s != null; s = s.outer) {
        if (s.type == ScopeType.FUNCTION || s.type == ScopeType.PROGRAM) {
          return s;
        }
      }
    } else if ("arguments".equals(symbolName)) {
      // "arguments" is defined in all functions, but can be declared as well.
      for (ScopeTree<S> s = useSite; s != null; s = s.outer) {
        if (s.type == ScopeType.FUNCTION || s.declared.contains(symbolName)) {
          return s;
        }
      }
    } else {
      for (ScopeTree<S> s = useSite; s != null; s = s.outer) {
        if (s.declared.contains(symbolName)) { return s; }
      }
    }
    return null;
  }

  /**
   * True if the given reference refers to a property name.
   * E.g. {@code bar} in {@code foo.bar}.
   * Any reference in a {@code with} statement could refer to a property name
   * at some times in a program, and not in others.
   * This does not handle that distinction.
   */
  private static boolean isPropertyName(AncestorChain<Reference> ac) {
    return (ac.parent != null
            && Operation.is(ac.parent.node, Operator.MEMBER_ACCESS)
            && ac.node == ac.parent.node.children().get(1));
  }

  /**
   * True iff ac receives the object key value as does {@code k} in
   * {@code for(var k in obj)}.
   */
  private static boolean isKeyReceiver(AncestorChain<Declaration> ac) {
    return (ac.parent != null
            && ac.parent.node instanceof ForEachLoop
            && ac.node == ac.parent.node.children().get(0));
  }

  /**
   * The name that a right hand side expression is assigned to.
   * @param ac a right hand side expression.
   * @return null if no such name.
   */
  private static String nameAssignedTo(AncestorChain<?> ac) {
    if (ac.parent == null) { return null; }
    if (ac.parent.node instanceof Declaration) {
      return ac.parent.cast(Declaration.class).node.getIdentifierName();
    } else if (Operation.is(ac.parent.node, Operator.ASSIGN)) {
      ParseTreeNode lhs = ac.parent.node.children().get(0);
      return lhs instanceof Reference ?
          ((Reference) lhs).getIdentifierName() : null;
    }
    return null;
  }

  /**
   * Returns the scope into which the given declaration should be hoisted.
   * @param id an identifier being declared.
   * @param scope the scope in which the identifier appears.
   */
  private static <S>
  ScopeTree<S> hoist(AncestorChain<Identifier> id, ScopeTree<S> scope) {
    ScopeTree<S> declScope = scope;
    if (id.parent.parent == null
        || !(id.parent.parent.node instanceof CatchStmt)) {
      // If it's not an exception declaration,
      // we have to hoist the declaration out of any non-declaration scopes.
      while (!declScope.type.isDeclarationContainer) {
        declScope = declScope.outer;
      }
    }
    return declScope;
  }

  /** Fire declaration, masking, and overriding events, and update scope */
  private void declare(AncestorChain<Identifier> id, ScopeTree<S> scope) {
    String symbolName = id.node.getName();
    ScopeTree<S> declScope = hoist(id, scope);
    for (ScopeTree<S> s = scope; s != declScope; s = s.outer) {
      if (s.type != ScopeType.CATCH) { continue; }
      AncestorChain<CatchStmt> cs = s.inScope.get(0).cast(CatchStmt.class);
      AncestorChain<Declaration> ex = cs.child(cs.node.getException());
      Identifier exId = ex.node.getIdentifier();
      if (symbolName.equals(exId.getName())) {
        listener.splitInitialization(
            id, declScope.scopeImpl, ex.child(exId), s.scopeImpl);
      }
    }
    ScopeTree<S> maskedScope = definingSite(symbolName, declScope);
    declScope.declared.add(symbolName);
    listener.declaration(id, declScope.scopeImpl);
    if (maskedScope != null
        // Not a function declaration or a var declaration like
        //   var x = function x() { ... };
        && !(id.parent.node instanceof FunctionConstructor
             && symbolName.equals(nameAssignedTo(id.parent)))) {
      if (maskedScope == scope) {
        listener.duplicate(id, declScope.scopeImpl);
      } else {
        listener.masked(id, declScope.scopeImpl, scopeImpl(maskedScope));
      }
    }
  }

  /**
   * @return null if the input is null which is the case for the declaration
   *   scopes of free variables.
   */
  private static <S extends AbstractScope> S scopeImpl(ScopeTree<S> s) {
    return s != null ? s.scopeImpl : null;
  }

  /** Encapsulates a scope and its uses and declarations. */
  private static final class ScopeTree<S> {
    /** The scope that contains this scope tree or null if this is the root. */
    final ScopeTree<S> outer;
    final ScopeType type;
    /** The listener defined scope corresponding to this scope tree. */
    final S scopeImpl;
    /** The inner scopes contained by this scope, non-transitively. */
    final List<ScopeTree<S>> innerScopes = Lists.newArrayList();
    /** The set of AST nodes in this scope that are not in an inner scope. */
    final List<AncestorChain<?>> inScope = Lists.newArrayList();
    /**
     * The symbols corresponding to declarations in this scope,
     * non-transitively.
     * These may not be declared in this scope because of hoisting.
     */
    final List<Symbol<S>> declarations = Lists.newArrayList();
    /** The symbols corresponding to uses in this scope. */
    final List<Symbol<S>> uses = Lists.newArrayList();
    /** The set of names declared in this scope. */
    final Set<String> declared = Sets.newHashSet();

    ScopeTree(ScopeTree<S> outer, ScopeType t, S scopeImpl) {
      this.outer = outer;
      this.type = t;
      this.scopeImpl = scopeImpl;
      if (outer != null) { outer.innerScopes.add(this); }
    }
  }

  /** An identifier in a specific scope. */
  private static final class Symbol<S> {
    final AncestorChain<Identifier> id;
    final ScopeTree<S> useScope;

    Symbol(AncestorChain<Identifier> id, ScopeTree<S> useScope) {
      this.id = id;
      this.useScope = useScope;
    }
  }
}
