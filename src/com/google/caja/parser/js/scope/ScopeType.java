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

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.WithStmt;

/**
 * Describes the kind of parse tree node that introduces a scope.
 *
 * @author mikesamuel@gmail.com
 */
public enum ScopeType {

  /**
   * Describes the top level scope of a JavaScript program.
   * Example: the scope in which the variable 'foo' is defined in the following:
   *
   * <pre>
   * var foo = 3;
   * var bar = 4;
   * </pre>
   */
  PROGRAM(true),

  /**
   * Describes a scope created from a plain block.
   * Example: the scope of {@code foo} in the following:
   *
   * <pre>
   * if (someCondition) { foo; }
   * </pre>
   */
  BLOCK,

  /**
   * Describes the scope containing a function's actuals and body.
   * Example: the scope containing {@code x} and {@code foo}:
   *
   * <pre>
   * function someFunc(x) { foo; }
   * var someFunc = function(x) { foo; }
   * </pre>
   */
  FUNCTION(true),

  /**
   * Describes a scope created from a catch block.
   * Example: the block containing {@code e} and {@code foo} in the following:
   *
   * <pre>
   * try { doSomething(); } catch (e) { foo; }
   * </pre>
   */
  CATCH,

  /**
   * Describes a scope created from a with block.
   * Example: the block containing {@code o} and {@code foo} in the following:
   *
   * <pre>
   * with (o) { foo; }
   * </pre>
   */
  WITH,
  ;

  /**
   * True iff {@code var} declarations that appear inside this scope take
   * effect in this scope instead of being hoisted to a containing scope.
   */
  public final boolean isDeclarationContainer;

  ScopeType() { this(false); }
  ScopeType(boolean isDeclarationContainer) {
    this.isDeclarationContainer = isDeclarationContainer;
  }

  /**
   * The type of scope introduced by the given node or null if the given node
   * does not introduce a scope.
   */
  public static ScopeType forNode(ParseTreeNode n) {
    if (n instanceof FunctionConstructor) { return FUNCTION; }
    if (n instanceof CatchStmt) { return CATCH; }
    if (n instanceof WithStmt) { return WITH; }
    return null;
  }
}
