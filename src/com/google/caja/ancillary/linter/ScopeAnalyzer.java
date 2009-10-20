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

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.AssignOperation;
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.WithStmt;
import com.google.caja.util.Lists;
import com.google.caja.util.Sets;
import com.google.caja.util.SyntheticAttributeKey;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Given a DOM tree, associates lexical scopes with nodes and identifies uses.
 *
 * @author mikesamuel@gmail.com
 */
class ScopeAnalyzer {
  private static final SyntheticAttributeKey<LexicalScope> CONTAINING_SCOPE
      = new SyntheticAttributeKey<LexicalScope>(
        LexicalScope.class, "containingScope");

  private static final SyntheticAttributeKey<LexicalScope> DEFINING_SCOPE
      = new SyntheticAttributeKey<LexicalScope>(
        LexicalScope.class, "definingScope");

  static final Collection<String> ECMASCRIPT_BUILTINS
      = Collections.unmodifiableCollection(Sets.newLinkedHashSet(
          "Array",
          "Boolean",
          "Date",
          "decodeURI",
          "decodeURIComponent",
          "encodeURI",
          "encodeURIComponent",
          "Error",
          "EvalError",
          "Function",
          "Infinity",
          "isFinite",
          "isNaN",
          "Number",
          "Object",
          "parseFloat",
          "parseInt",
          "Math",
          "NaN",
          "RangeError",
          "ReferenceError",
          "RegExp",
          "String",
          "SyntaxError",
          "TypeError",
          "URIError",
          "undefined"));

  /**
   * May be overridden to make different decisions about when to introduce
   * a new lexical scope.
   * @return true whether the given node introduces a new lexical scope.
   */
  protected boolean introducesScope(AncestorChain<?> ac) {
    ParseTreeNode node = ac.node;
    return node instanceof FunctionConstructor || node instanceof CatchStmt
        || node instanceof WithStmt;
  }

  /**
   * May be overridden to implement hoisting differently.
   * @return true iff the given declaration should be hoisted out of the given
   *      scope.
   */
  protected boolean hoist(AncestorChain<?> d, LexicalScope s) {
    if (d.node instanceof Declaration && s.isCatchScope()) {
      return d.parent != null && !(d.parent.node instanceof CatchStmt);
    }
    return s.isWithScope();
  }

  /**
   * Initializes a scope's symbol table.  This is important for the builtins
   * in the global scope, and for state visible to a function.
   * <p>
   * This method may be overridden to initialize scope symbols differently.
   * It is not limited to operating only on the input scope -- to simulate
   * JScript quirks, an implementation might introducing a binding into an
   * ancestor scope for a named {@link FunctionConstructor}.
   *
   * @param scope to have its {@link LexicalScope#symbols symbol table}
   *     modified.
   */
  protected void initScope(LexicalScope scope) {
    if (scope.isFunctionScope()) {
      AncestorChain<FunctionConstructor> fn
          = scope.root.cast(FunctionConstructor.class);
      if (fn.node.getIdentifierName() != null) {
        scope.symbols.declare(fn.node.getIdentifierName(), fn);
      }
      scope.symbols.declare("this", fn);
      scope.symbols.declare("arguments", fn);
    } else if (scope.parent == null) {  // The global scope
      for (String builtin : ECMASCRIPT_BUILTINS) {
        scope.symbols.declare(builtin, scope.root);
      }
    }
  }

  /**
   * Computes lexical scopes for the given parse tree, attaching information
   * to the parse tree nodes.  This assumes that each node under root appears
   * at most once in the tree.
   * @return the scopes created.  The global scope will be the zero-th element
   *     in the list.
   */
  final List<LexicalScope> computeLexicalScopes(AncestorChain<?> root) {
    LexicalScope globalScope = new LexicalScope(root, null);
    initScope(globalScope);
    List<LexicalScope> scopes = Lists.newArrayList(globalScope);
    computeLexicalScopes(root, globalScope, scopes);
    return scopes;
  }

  /**
   * @param ac the parse tree to whom lexical scoping rules are to be applied.
   * @param parent the scope of ac's parent.
   * @param scopes a list that receives newly created scopes.
   */
  private void computeLexicalScopes(
      AncestorChain<?> ac, LexicalScope parent, List<LexicalScope> scopes) {
    // Compute the scope for the current node.
    // Since we create a global scope in the original caller, avoid creating
    // two scopes for the same object here.
    LexicalScope scope = parent;
    if (introducesScope(ac) && scope.root != ac) {
      scope = new LexicalScope(ac, parent);
      scopes.add(scope);
      // Sets up the symbol table.
      initScope(scope);
    }
    assert (
        ac.node instanceof Identifier
        || !ac.node.getAttributes().containsKey(CONTAINING_SCOPE))
        : "Scope already attached to node";
    ac.node.getAttributes().set(CONTAINING_SCOPE, scope);
    // initScope may have set up some symbols, but if this is a declaration,
    // do the appropriate hoisting and declarations.
    if (ac.node instanceof Declaration) {
      AncestorChain<Declaration> d = ac.cast(Declaration.class);
      LexicalScope definingScope = scope;
      while (definingScope.parent != null && hoist(d, definingScope)) {
        definingScope = definingScope.parent;
      }
      ac.node.getAttributes().set(DEFINING_SCOPE, definingScope);
      definingScope.symbols.declare(ac.cast(Declaration.class));
    }
    // recurse to children
    for (ParseTreeNode child : ac.node.children()) {
      computeLexicalScopes(AncestorChain.instance(ac, child), scope, scopes);
    }
  }

  /** Returns all the uses of symbols in the given AST. */
  List<Use> getUses(AncestorChain<?> root) {
    List<Use> uses = Lists.newArrayList();
    findUses(root, uses);
    return uses;
  }

  private static void findUses(AncestorChain<?> ac, List<Use> out) {
    if (ac.node instanceof Reference) {
      out.add(new Use(ac.cast(Reference.class)));
      return;
    }

    if (ac.node instanceof Operation) {
      Operator op = ac.cast(Operation.class).node.getOperator();
      if (op == Operator.MEMBER_ACCESS) {
        findUses(AncestorChain.instance(ac, ac.node.children().get(0)), out);
        // Do not recurse to member name
        return;
      }
    }
    for (ParseTreeNode child : ac.node.children()) {
      findUses(AncestorChain.instance(ac, child), out);
    }
  }

  /**
   * The scope containing the node.  This can only be called after
   * {@link #computeLexicalScopes} has been called on an ancestor node.
   */
  static LexicalScope containingScopeForNode(ParseTreeNode node) {
    return node.getAttributes().get(CONTAINING_SCOPE);
  }

  /**
   * The scope containing the node.  This can only be called after
   * {@link #computeLexicalScopes} has been called on an ancestor node.
   */
  static LexicalScope definingScopeForNode(Declaration decl) {
    return decl.getAttributes().get(DEFINING_SCOPE);
  }

  /** A use of a particular symbol. */
  static final class Use {
    final AncestorChain<? extends Reference> ref;

    Use(AncestorChain<? extends Reference> usage) { this.ref = usage; }

    boolean isLeftHandSideExpression() {
      AncestorChain<?> ac = ref;
      while (isObjectInMemberAccess(ac)) { ac = ac.parent; }
      return ac.parent != null && ac.parent.node instanceof AssignOperation
          && ac.node == ac.parent.node.children().get(0);
    }

    boolean isMemberAccess() {
      return isObjectInMemberAccess(ref);
    }

    private static boolean isObjectInMemberAccess(AncestorChain<?> ac) {
      if (ac.parent == null || !(ac.parent.node instanceof Operation)) {
        return false;
      }
      Operator op = ac.parent.cast(Operation.class).node.getOperator();
      return op == Operator.MEMBER_ACCESS || op == Operator.SQUARE_BRACKET
          && ac.node == ac.parent.node.children().get(0);
    }

    String getSymbolName() {
      return ref.node.getIdentifierName();
    }
  }
}
