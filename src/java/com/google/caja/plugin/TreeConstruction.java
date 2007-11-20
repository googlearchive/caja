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

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;

import java.util.List;
import java.util.Vector;

/**
 * Utilities for parse true construction
 *
 * @author benl@google.com (Ben Laurie)
 *
 */
public final class TreeConstruction {
 // thing.member
  static Operation memberAccess(String thing, String member) {
    return s(new Operation(Operator.MEMBER_ACCESS, s(new Reference(thing)),
        s(new Reference(member))));
  }
  // nodes[0](nodes[1...])
  static Operation call(Expression... nodes) {
    return s(new Operation(Operator.FUNCTION_CALL, nodes));
  }
  // function name(args) { body }
  static FunctionConstructor function(String name, Block body, String...args) {
    List<FormalParam> params = new Vector<FormalParam>();
    for (String arg : args) {
      params.add(s(new FormalParam(arg)));
    }
    return s(new FunctionConstructor(name, params, body));
  }
  static ExpressionStmt assign(Expression lhs, Expression rhs) {
    return s(new ExpressionStmt(s(new Operation(Operator.ASSIGN, lhs, rhs))));
  }

  /** make the given parse tree node synthetic. */
  private static <T extends ParseTreeNode> T s(T t) {
    t.getAttributes().set(ExpressionSanitizer.SYNTHETIC, Boolean.TRUE);
    return t;
  }
  // Can't instantiate
  private TreeConstruction() {}
}
