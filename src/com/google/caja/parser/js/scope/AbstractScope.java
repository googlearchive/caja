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

/**
 * Describes the set of symbols in scope at a point in a JavaScript program.
 *
 * @author mikesamuel@gmail.com
 */
public interface AbstractScope {

  /**
   * The type of scope.
   * @return not null, and does not change over the life of the instance.
   */
  ScopeType getType();

  /**
   * True iff the symbol is declared in this scope.
   * Some symbols may be defined but not declared, such as {@code this} and
   * {@code arguments}, and the dynamic set of symbols introduced by a
   * {@code with} block.
   * This method only deals with declarations that appear in program text.
   * @param name a non null JavaScript identifier.
   * @return true iff the named symbol is declared in this scope.  This does not
   *     recurse to parent scopes.
   */
  boolean isSymbolDeclared(String name);

  /**
   * The scope that contains this scope or null if this is the root scope.
   * @return null or an instance of a subclass of the scope type of the listener
   *     that created this instance.
   */
  AbstractScope getContainingScope();
}
