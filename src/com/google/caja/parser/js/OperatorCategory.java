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

package com.google.caja.parser.js;

/**
 * Describes the category of an operator. This generalization allows us to
 * simplify reasoning about the semantics of the parse tree, e.g., in the
 * implementation of rewriting rules.
 *
 * @author ihab.awad@gmail.com
 */
public enum OperatorCategory {

  /**
   * An Operator that simply evaluates all its children in order as rValues and
   * then returns a result computed from the resulting values.
   */
  SIMPLE,

  /**
   * An Operator that assigns to its first child as an lValue.
   * <p>
   * All except ASSIGN(=) also read from that first child first, so those
   * are read-modify-write operations.
   */
  ASSIGNMENT,

  /**
   * An Operator that executes some of its operands conditionally, depending on
   * the results of evaluating other operands.
   */
  CONTROL,

  /**
   * A category of Operator not otherwise covered.
   */
  SPECIAL,
  ;
}
