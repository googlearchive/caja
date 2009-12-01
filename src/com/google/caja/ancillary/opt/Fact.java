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

package com.google.caja.ancillary.opt;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.Keyword;
import com.google.caja.parser.js.BooleanLiteral;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;

/**
 * A piece of knowledge about the environment a JavaScript program might run in.
 *
 * @author mikesamuel@gmail.com
 */
public final class Fact {
  enum Type {
    IS,
    /**
     * Indicates that the expression being considered is similar to the value.
     * A value is considered "like" the boolean value it coerces to in a
     * {@link Literal#getValueInBooleanContext() boolean context}.
     */
    LIKE,
    ;
  }

  final Type type;
  final Expression value;

  private Fact(Type type, Expression value) {
    this.type = type;
    this.value = value;
  }

  boolean isLessSpecificThan(Fact that) {
    if (this.type == Type.LIKE && that.type == Type.IS) { return true; }
    return this != that && that == GLOBAL;  // global more specific than truthy
  }

  public static Fact is(Literal value) {
    return new Fact(Type.IS, value);
  }

  private static final FilePosition UNK = FilePosition.UNKNOWN;
  static Fact TRUTHY = new Fact(Type.LIKE, new BooleanLiteral(UNK, true));
  static Fact FALSEY = new Fact(Type.LIKE, new BooleanLiteral(UNK, false));
  static Fact TRUE = is((Literal) TRUTHY.value);
  static Fact FALSE = is((Literal) FALSEY.value);
  static Fact UNDEFINED = new Fact(
      Type.IS,
      Operation.create(UNK, Operator.VOID, new IntegerLiteral(UNK, 0)));
  static Fact GLOBAL = new Fact(
      Type.LIKE, new Reference(new Identifier(UNK, Keyword.THIS.toString())));

  boolean isTruthy() {
    if (this == GLOBAL) { return true; }
    if (this == UNDEFINED) { return false; }
    return ((Literal) value).getValueInBooleanContext();
  }
  boolean isFalsey() { return !isTruthy(); }
  boolean isTrue() {
    return type == Type.IS && value instanceof BooleanLiteral
        && ((BooleanLiteral) value).value;
  }
  boolean isFalse() {
    return type == Type.IS && value instanceof BooleanLiteral
        && !((BooleanLiteral) value).value;
  }
  boolean isGlobal() { return this == GLOBAL; }
  boolean isUndefined() { return this == UNDEFINED; }

  boolean isSubstitutable(boolean isFuzzy) {
    return type == Type.IS || (isFuzzy && this != GLOBAL);
  }

  @Override
  public String toString() { return "[Fact " + type + " " + value + "]"; }
}
