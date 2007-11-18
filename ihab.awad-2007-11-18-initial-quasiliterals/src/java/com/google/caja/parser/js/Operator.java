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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Information about a javascript operator including its precedence and
 * associativity.
 *
 * @author mikesamuel@gmail.com
 */
public enum Operator {

  // from precedence tables at
  // http://www.codehouse.com/javascript/precedence/
  SQUARE_BRACKET(OperatorType.BRACKET, 1, Associativity.LEFT, "[]"),
  MEMBER_ACCESS(OperatorType.INFIX, 1, Associativity.LEFT, "."),
  CONSTRUCTOR(OperatorType.PREFIX, 1, Associativity.RIGHT, "new"),
  FUNCTION_CALL(OperatorType.BRACKET, 2, Associativity.LEFT, "()"),
  POST_INCREMENT(OperatorType.POSTFIX, 3, null, "++"),
  POST_DECREMENT(OperatorType.POSTFIX, 3, null, "--"),
  DELETE(OperatorType.PREFIX, 4, Associativity.RIGHT, "delete"),
  VOID(OperatorType.PREFIX, 4, Associativity.RIGHT, "void"),
  TYPEOF(OperatorType.PREFIX, 4, Associativity.RIGHT, "typeof"),
  PRE_INCREMENT(OperatorType.PREFIX, 4, Associativity.RIGHT, "++"),
  PRE_DECREMENT(OperatorType.PREFIX, 4, Associativity.RIGHT, "--"),
  IDENTITY(OperatorType.PREFIX, 4, Associativity.RIGHT, "+"),
  NEGATION(OperatorType.PREFIX, 4, Associativity.RIGHT, "-"),
  INVERSE(OperatorType.PREFIX, 4, Associativity.RIGHT, "~"),
  NOT(OperatorType.PREFIX, 4, Associativity.RIGHT, "!"),
  MULTIPLICATION(OperatorType.INFIX, 5, Associativity.LEFT, "*"),
  DIVISION(OperatorType.INFIX, 5, Associativity.LEFT, "/"),
  MODULUS(OperatorType.INFIX, 5, Associativity.LEFT, "%"),
  ADDITION(OperatorType.INFIX, 6, Associativity.LEFT, "+"),
  SUBTRACTION(OperatorType.INFIX, 6, Associativity.LEFT, "-"),
  LSHIFT(OperatorType.INFIX, 7, Associativity.LEFT, "<<"),
  RSHIFT(OperatorType.INFIX, 7, Associativity.LEFT, ">>"),
  RUSHIFT(OperatorType.INFIX, 7, Associativity.LEFT, ">>>"),
  LESS_THAN(OperatorType.INFIX, 8, Associativity.LEFT, "<"),
  GREATER_THAN(OperatorType.INFIX, 8, Associativity.LEFT, ">"),
  LESS_EQUALS(OperatorType.INFIX, 8, Associativity.LEFT, "<="),
  GREATER_EQUALS(OperatorType.INFIX, 8, Associativity.LEFT, ">="),
  INSTANCE_OF(OperatorType.INFIX, 8, Associativity.LEFT, "instanceof"),
  IN(OperatorType.INFIX, 8, Associativity.LEFT, "in"),
  EQUAL(OperatorType.INFIX, 9, Associativity.LEFT, "=="),
  NOT_EQUAL(OperatorType.INFIX, 9, Associativity.LEFT, "!="),
  STRICTLY_EQUAL(OperatorType.INFIX, 9, Associativity.LEFT, "==="),
  STRICTLY_NOT_EQUAL(OperatorType.INFIX, 9, Associativity.LEFT, "!=="),
  BITWISE_AND(OperatorType.INFIX, 10, Associativity.LEFT, "&"),
  BITWISE_XOR(OperatorType.INFIX, 11, Associativity.LEFT, "^"),
  BITWISE_OR(OperatorType.INFIX, 12, Associativity.LEFT, "|"),
  LOGICAL_AND(OperatorType.INFIX, 13, Associativity.LEFT, "&&"),
  LOGICAL_OR(OperatorType.INFIX, 14, Associativity.LEFT, "||"),
  TERNARY(OperatorType.TERNARY, 15, Associativity.RIGHT, "?:"),
  ASSIGN(OperatorType.INFIX, 16, Associativity.RIGHT, "="),
  ASSIGN_MUL(OperatorType.INFIX, 16, Associativity.RIGHT, "*=",
             Operator.MULTIPLICATION),
  ASSIGN_DIV(OperatorType.INFIX, 16, Associativity.RIGHT, "/=",
             Operator.DIVISION),
  ASSIGN_MOD(OperatorType.INFIX, 16, Associativity.RIGHT, "%=",
             Operator.MODULUS),
  ASSIGN_SUM(OperatorType.INFIX, 16, Associativity.RIGHT, "+=",
             Operator.ADDITION),
  ASSIGN_SUB(OperatorType.INFIX, 16, Associativity.RIGHT, "-=",
             Operator.SUBTRACTION),
  ASSIGN_LSH(OperatorType.INFIX, 16, Associativity.RIGHT, "<<=",
             Operator.LSHIFT),
  ASSIGN_RSH(OperatorType.INFIX, 16, Associativity.RIGHT, ">>=",
             Operator.RSHIFT),
  ASSIGN_USH(OperatorType.INFIX, 16, Associativity.RIGHT, ">>>=",
             Operator.RUSHIFT),
  ASSIGN_AND(OperatorType.INFIX, 16, Associativity.RIGHT, "&=",
             Operator.BITWISE_AND),
  ASSIGN_XOR(OperatorType.INFIX, 16, Associativity.RIGHT, "^=",
             Operator.BITWISE_XOR),
  ASSIGN_OR(OperatorType.INFIX, 16, Associativity.RIGHT, "|=",
            Operator.BITWISE_OR),
  COMMA(OperatorType.INFIX, 17, Associativity.LEFT, ","),
  ;

  private OperatorType type;
  private int precedence;
  private Associativity associativity;
  private String symbol;
  /**
   * null or the non assignment operator that performs the non assignment
   * portion of this operators function.
   */
  private Operator assignmentDelegate;

  Operator(OperatorType type, int precedence,
      Associativity assoc, String symbol) {
    this(type, precedence, assoc, symbol, null);
  }

  Operator(OperatorType type, int precedence,
      Associativity assoc, String symbol, Operator assignmentDelegate) {
    if (assignmentDelegate != null &&
        assignmentDelegate.getType() != OperatorType.INFIX) {
      throw new IllegalArgumentException(
          assignmentDelegate + " cannot be used as an assignment delegate");
    }
    this.type = type;
    this.precedence = precedence;
    this.associativity = assoc;
    this.symbol = symbol;
    this.assignmentDelegate = assignmentDelegate;
  }

  public OperatorType getType() { return type; }
  public int getPrecedence() { return precedence; }
  public Associativity getAssociativity() { return associativity; }
  public String getSymbol() { return symbol; }
  public String getClosingSymbol() {
    switch (type) {
      default:
        return null;
      case BRACKET:  case TERNARY:
        return symbol.substring(symbol.length() / 2);  // "()" -> ")"
    }
  }
  public String getOpeningSymbol() {
    switch (type) {
      default:
        return symbol;
      case BRACKET:  case TERNARY:
        return symbol.substring(0, symbol.length() / 2);  // "()" -> "("
    }
  }
  public Operator getAssignmentDelegate() { return assignmentDelegate; }

  // Group operations by OperationType so that we can query for infix
  // operators matching a given symbol.
  private static Map<OperatorType, Map<String, Operator> > symbolsByType;

  public static Operator lookupOperation(String symbol, OperatorType type) {
    Initializer.initSymbols();
    return symbolsByType.get(type).get(symbol);
  }

  private static class Initializer {
    static void initSymbols() {
      // placeholder method to force loading of this class
    }
    static {
      symbolsByType = new EnumMap<OperatorType, Map<String, Operator>>(
          OperatorType.class);
      for (OperatorType opType : OperatorType.values()) {
        Map<String, Operator> symbolMap = new HashMap<String, Operator>();
        symbolsByType.put(opType, symbolMap);
      }
      for (Operator op : Operator.values()) {
        if (null != symbolsByType.get(op.getType())
            .put(op.getOpeningSymbol(), op)) {
          throw new AssertionError("Duplicate symbol " + op.getSymbol()
                                   + " for type " + op.getType());
        }
      }
    }
  }

  static {
    for (Operator op : values()) {
      Operator assignmentDelegate = op.assignmentDelegate;
      if (assignmentDelegate != null
          && (assignmentDelegate == ASSIGN
              || assignmentDelegate.assignmentDelegate != null)) {
        throw new AssertionError(
            op + " cannot delegate assignment to an assignment operator: "
            + assignmentDelegate);
      }
    }
  }

}
