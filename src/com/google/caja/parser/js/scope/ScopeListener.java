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
import com.google.caja.parser.js.Identifier;

/**
 * Receives events from a {@link ScopeAnalyzer} describing the relationships
 * between declarations and uses in a JavaScript parse tree.
 *
 * @author mikesamuel@gmail.com
 */
public interface ScopeListener<S extends AbstractScope> {

  /**
   * Called when a variable is declared.
   * If the analyzer is not descending into scopes, this is only called for
   * {@link ScopeType#isDeclarationContainer declaration containers}
   * to allow listeners to build up the set of declarations, and is called for
   * every declaration in the declaration container including those inside
   * non declaration containers like catch block bodies.
   */
  void declaration(AncestorChain<Identifier> id, S scope);

  /**
   * Called for each node visited in the scope in which it appears.
   * Scope roots are in the scope they introduce.
   */
  void inScope(AncestorChain<?> ac, S scope);

  /**
   * Called when a symbol is declared that masks a symbol in a wider scope.
   * @param id the symbol that is being declared.
   * @param inner the scope in which id is declared.
   * @param outer the scope containing the masked declaration.
   */
  void masked(AncestorChain<Identifier> id, S inner, S outer);

  /**
   * A corner case of reverse masking where a declaration is hoisted but its
   * initializer instead assigns to a variable in a different scope.
   * <pre>
   * try {
   *   ...
   * } catch (e) {
   *   var e = 3;
   * }
   * </pre>
   * In the above, {@code var e} is hoisted out of the catch block, but
   * {@code e = 3} affects the exception variable.
   * A masking event will also be issued when the catch scope is entered, but
   * the relationship between initializer and declaration is different.
   *
   * <p>
   * The same problem can occur with {@code with} blocks, but not in a way that
   * is easily statically determined.
   *
   * @param declared the symbol declared.
   * @param declScope the scope in which the symbol is declared.
   * @param initialized the identifier actually initialized.
   * @param maskingScope the scope in which the masking symbol is introduced
   *     which is initialized.
   */
  void splitInitialization(
      AncestorChain<Identifier> declared, S declScope,
      AncestorChain<Identifier> initialized, S maskingScope);

  /**
   * Called when a symbol is declared twice in the same scope (post-hoisting).
   * @param id the symbol declared twice.
   * @param scope the scope in which id is declared.
   */
  void duplicate(AncestorChain<Identifier> id, S scope);

  /**
   * Called when a variable is read.
   *
   * @param useSite the scope in which id is read.
   * @param definingSite the scope in which id is defined or null in the case
   *     of undeclared globals.  NOTE: be skeptical of this event if there is a
   *     {@link ScopeType#WITH with} block between useSite and definingSite.
   *     {@code definingSite} will always be the same as {@code useSite} or on
   *     its {@link AbstractScope#getContainingScope() containing scope chain}.
   */
  void read(AncestorChain<Identifier> id, S useSite, S definingSite);

  /**
   * Called when a variable is assigned.
   *
   * <p>Formal parameters, exception declarations, {@code this}, and
   * {@code arguments} are implicitly set but assigned events are not generated
   * for them unless they are explicitly assigned.</p>
   *
   * @param useSite the scope in which id is assigned.
   * @param definingSite the scope in which id is defined or null in the case
   *     of undeclared globals.  NOTE: be skeptical of this event if there is a
   *     {@link ScopeType#WITH with} block between useSite and definingSite.
   *     {@code definingSite} will always be the same as {@code useSite} or on
   *     its {@link AbstractScope#getContainingScope() containing scope chain}.
   */
  void assigned(AncestorChain<Identifier> id, S useSite, S definingSite);

  /**
   * Creates a scope for the given node.
   *
   * @return a scope instance that will be passed to other listener methods.
   */
  S createScope(ScopeType t, AncestorChain<?> root, S parent);

  /**
   * Invoked when a scope is entered.
   * Calls to {@code enterScope} and {@link #exitScope} will nest, so
   * {@code exitScope} will not be called for a particular scope until all the
   * containing scopes have been entered and exited.
   */
  void enterScope(S Scope);

  /**
   * Invoked when all uses and declarations for a scope have been handled.
   */
  void exitScope(S scope);
}
