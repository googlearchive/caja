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

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.reporting.RenderContext;

import java.util.Arrays;

/**
 * An expression that applies an {@link Operator} to a number of operands.
 *
 * @author mikesamuel@gmail.com
 */
public abstract class Operation extends AbstractExpression<Expression> {
  private Operator op;

  protected Operation(Operator op, Expression... params) {
    this.op = op;
    if (null == op) { throw new NullPointerException(); }
    createMutation().appendChildren(Arrays.asList(params)).execute();
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    int nChildren = children().size();
    if (nChildren < minArity(op)) {
      throw new IllegalArgumentException(
          "Too few of children " + nChildren + " for operator " + op);
    } else if (nChildren > maxArity(op)) {
      throw new IllegalArgumentException(
          "Too many children " + nChildren + " for operator " + op);
    }
  }

  public static Operation create(Operator op, Expression... params) {
    switch (op) {
      case ASSIGN: // =
      case ASSIGN_AND: // &=
      case ASSIGN_DIV: // /=
      case ASSIGN_LSH: // <<=
      case ASSIGN_MOD: // %=
      case ASSIGN_MUL: // *=
      case ASSIGN_OR:  // &=
      case ASSIGN_RSH: // >>=
      case ASSIGN_SUB: // -=
      case ASSIGN_SUM: // +=
      case ASSIGN_USH: // >>>=
      case ASSIGN_XOR: // ^=
      case POST_DECREMENT: // x--
      case POST_INCREMENT: // x++
      case PRE_DECREMENT:  // --x
      case PRE_INCREMENT:  // ++x
      {
        return new AssignOperation(op, params);
      }
      case LOGICAL_AND: // &&
      case LOGICAL_OR:  // ||
      case TERNARY:     // ?:
      {
        return new ControlOperation(op, params);
      }
      case COMMA:          // ,
      case CONSTRUCTOR:    // new
      case DELETE:         // delete
      case FUNCTION_CALL:  // ()
      case MEMBER_ACCESS:  // .
      case SQUARE_BRACKET: // []
      case TYPEOF:         // typeof
      case VOID:           // void
      {
        return new SpecialOperation(op, params);
      }
      case ADDITION:             // +
      case BITWISE_AND:          // &
      case BITWISE_OR:           // |
      case BITWISE_XOR:          // ^
      case DIVISION:             // /
      case EQUAL:                // ==
      case GREATER_EQUALS:       // >=
      case GREATER_THAN:         // >
      case IDENTITY:             // unary +
      case IN:                   // in
      case INSTANCE_OF:          // instanceof
      case INVERSE:              // ~
      case LESS_EQUALS:          // <=
      case LESS_THAN:            // <
      case LSHIFT:               // <
      case MODULUS:              // %
      case MULTIPLICATION:       // *
      case NEGATION:             // unary -
      case NOT:                  // !
      case NOT_EQUAL:            // !=
      case RSHIFT:               // >>
      case RUSHIFT:              // >>>
      case STRICTLY_EQUAL:       // ===
      case STRICTLY_NOT_EQUAL:   // !==
      case SUBTRACTION:          // -
      {
        return new SimpleOperation(op, params);
      }
      default: {
        throw new RuntimeException("unexpected: " + op);
      }
    }
  }

  @Override
  public Object getValue() { return op; }

  public Operator getOperator() { return op; }

  @Override
  public boolean isLeftHandSide() {
    switch (op) {
      case MEMBER_ACCESS:
      case SQUARE_BRACKET:
        return true;
      default:
        return false;
    }
  }

  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());
    switch (op.getType()) {
      case PREFIX:
        out.consume(op.getSymbol());
        renderParam(0, rc);
        break;
      case POSTFIX:
        renderParam(0, rc);
        out.mark(FilePosition.endOfOrNull(getFilePosition()));
        out.consume(op.getSymbol());
        break;
      case INFIX:
        renderParam(0, rc);
        switch (getOperator()) {
          default:
            // These spaces are necessary for security.
            // If they are not present, then rendered javascript might include
            // the strings ]]> or </script> which would prevent it from being
            // safely embedded in HTML or XML.
            out.consume(" ");
            out.consume(op.getSymbol());
            out.consume(" ");
            break;
          case MEMBER_ACCESS:
          case COMMA:
            out.consume(op.getSymbol());
            break;
        }
        renderParam(1, rc);
        break;
      case BRACKET:
        renderParam(0, rc);
        out.consume(op.getOpeningSymbol());
        boolean seen = false;
        for (Expression e : children().subList(1, children().size())) {
          if (seen) {
            out.consume(",");
          } else {
            seen = true;
          }
          // make sure that comma operators are properly escaped
          if (!parenthesize(Operator.COMMA, false, e)) {
            e.render(rc);
          } else {
            out.consume("(");
            e.render(rc);
            out.mark(FilePosition.endOfOrNull(e.getFilePosition()));
            out.consume(")");
          }
        }
        out.mark(FilePosition.endOfOrNull(getFilePosition()));
        out.consume(op.getClosingSymbol());
        break;
      case TERNARY:
        renderParam(0, rc);
        out.consume(op.getOpeningSymbol());
        out.consume(" ");
        renderParam(1, rc);
        out.consume(op.getClosingSymbol());
        out.consume(" ");
        renderParam(2, rc);
        break;
    }
  }

  private void renderParam(int i, RenderContext rc) {
    TokenConsumer out = rc.getOut();
    Expression e = children().get(i);
    out.mark(e.getFilePosition());
    if (!parenthesize(op, 0 == i, e)) {
      e.render(rc);
    } else {
      out.consume("(");
      e.render(rc);
      out.mark(FilePosition.endOfOrNull(getFilePosition()));
      out.consume(")");
    }
  }

  private static boolean parenthesize(
      Operator op, boolean firstOp, Expression child) {
    // Parenthesize block-like expressions
    if (child instanceof FunctionConstructor
        || child instanceof ObjectConstructor) {
      // Parenthesize constructors if they're the first op.
      // They can be the right hand of assignments, but they won't parse
      // unparenthesized if used as the first operand in a call, followed by a
      // postfix op, or as part of the condition in a ternary op.
      return firstOp;
    }

    if (child instanceof NumberLiteral) {
      if (firstOp && op == Operator.MEMBER_ACCESS) {
        // Parenthesize numbers and booleans when they're the left hand side of
        // an operator.
        // 3.toString() is not valid, but (3).toString() is.
        return true;
      }

      if (OperatorType.PREFIX == op.getType()) {
        // make sure that -(-3) is not written out as --3, and that -(3) is not
        // written out as the literal node -3.
        return ((NumberLiteral) child).getValue().doubleValue() < 0;
      }
    }

    if (!(child instanceof Operation)) { return false; }

    Operator childOp = ((Operation) child).getOperator();

    if (firstOp && childOp == Operator.FUNCTION_CALL
        && op == Operator.MEMBER_ACCESS) {
      // Don't parenthesize foo().bar since the LHS of the function call must
      // already be parenthesized if it were ambiguous since function call binds
      // less tightly than member access, and the actuals are already
      // parenthesized since the function call operator is "()".
      return false;
    }

    // Parenthesize based on associativity and precedence
    int delta = op.getPrecedence() - childOp.getPrecedence();
    if (delta < 0) {
      // e.g. this is * and child is +
      return true;
    } else if (delta == 0) {
      // LEFT: a + b + c -> (a + b) + c
      // So we need to parenthesize right in a + (b + c)
      // RIGHT: a = b = c -> a = (b = c)
      // And we'd need to parenthesize left in (a = b) = c if that were legal

      // -(-a) is right associative so it is parenthesized

      // ?: is right associative, so in a ? b : c, a would be parenthesized were
      // it a ternary op.
      return (childOp.getAssociativity() == Associativity.LEFT) != firstOp;
    } else {
      return false;
    }
  }

  private static int minArity(Operator op) {
    if (op == Operator.FUNCTION_CALL) { return 1; }
    return op.getType().getArity();
  }

  private static int maxArity(Operator op) {
    if (op == Operator.FUNCTION_CALL) { return Integer.MAX_VALUE; }
    return op.getType().getArity();
  }
}
