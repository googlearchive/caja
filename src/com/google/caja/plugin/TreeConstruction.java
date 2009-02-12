// Copyright (C) 2007 Google Inc.
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

package com.google.caja.plugin;

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;

import static com.google.caja.parser.js.SyntheticNodes.s;

/**
 * Utilities for parse true construction
 *
 * @author benl@google.com (Ben Laurie)
 *
 */
public final class TreeConstruction {
  /** {@code thing.member } */
  public static Operation memberAccess(String thing, String member) {
    return Operation.create(
        FilePosition.UNKNOWN, Operator.MEMBER_ACCESS, ref(thing), ref(member));
  }
  /** {@code nodes[0](nodes[1...]) } */
  public static Operation call(Expression... nodes) {
    return Operation.create(
        FilePosition.span(
            nodes[0].getFilePosition(),
            nodes[nodes.length - 1].getFilePosition()),
        Operator.FUNCTION_CALL, nodes);
  }
  /** {@code function name(args) <body>}
  public static FunctionConstructor function(
      FilePosition pos, String name, Block body, String... args) {
    List<FormalParam> params = new Vector<FormalParam>();
    for (String arg : args) {
      params.add(new FormalParam(s(new Identifier(pos, arg))));
    }
    return new FunctionConstructor(
        pos, s(new Identifier(pos, name)), params, body);
  }*/

  public static ExpressionStmt assign(Expression lhs, Expression rhs) {
    Operation assign = Operation.createInfix(Operator.ASSIGN, lhs, rhs);
    return new ExpressionStmt(assign.getFilePosition(), assign);
  }

  public static Reference ref(String name) {
    assert name != null;
    return new Reference(s(new Identifier(FilePosition.UNKNOWN, name)));
  }

  // Can't instantiate
  private TreeConstruction() {}
}
