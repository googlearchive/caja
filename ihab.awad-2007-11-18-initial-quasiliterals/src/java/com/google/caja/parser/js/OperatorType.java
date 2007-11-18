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

/**
 *
 * @author mikesamuel@gmail.com
 */
public enum OperatorType {

  PREFIX(1),
  POSTFIX(1),
  INFIX(2),
  BRACKET(2),
  TERNARY(3),
  ;

  private int arity;

  OperatorType(int arity) { this.arity = arity; }

  public int getArity() { return this.arity; }

}
