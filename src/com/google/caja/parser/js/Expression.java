// Copyright (C) 2005 Google Inc.
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

package com.google.caja.parser.js;

import javax.annotation.Nullable;

import com.google.caja.parser.MutableParseTreeNode;

/**
 *
 * @author mikesamuel@gmail.com
 */
public interface Expression extends MutableParseTreeNode {
  boolean isLeftHandSide();

  /**
   * Returns an expression that has identical side effects, but that may return
   * a different result.
   * @return null if there are no side effects.
   */
  Expression simplifyForSideEffect();

  /**
   * Returns the result of evaluating the expression in a boolean context or
   * null if indeterminable.  This result is valid assuming that the expression
   * does not throw an exception.  If the expression provably always throws
   * an exception, then it may return any result.
   */
  @Nullable Boolean conditionResult();

  /**
   * {@code null} or the result of applying the {@code typeof} operator to
   * the result of this expression.
   *
   * @return if the expression yields a result with the same {@code typeof}
   *     in all environments in which it returns normally, then returns the
   *     result of applying the {@code typeof} operator to the result.
   *     {@code null} if the type cannot be determined.
   *     This method is conservative, so it may return null where it is possible
   *     to prove a bound.
   */
  String typeOf();

  /**
   * This expression or a semantically equivalent simpler expression.
   * This method does not recurse.
   * @param isFn true if the expression is the first operand to a function call.
   *     Parts of JS are semantics specified in terms of the syntactic structure
   *     of sub-expressions, such as the value of {@code this} in a method call
   *     or the {@code eval} function/operator distinction.  This parameter is
   *     used to ensure the semantics don't change even when the simplified
   *     sub-expression is called as a function.
   */
  Expression fold(boolean isFn);
}
