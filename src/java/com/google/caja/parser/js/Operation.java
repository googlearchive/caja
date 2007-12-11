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

import com.google.caja.reporting.RenderContext;

import java.io.IOException;

import java.util.Arrays;

/**
 * An expression that applies an {@link Operator} to a number of operands.
 *
 * @author mikesamuel@gmail.com
 */
public class Operation extends AbstractExpression<Expression> {
  private Operator op;

  public Operation(Operator op, Expression... params) {
    this.op = op;
    if (null == op) { throw new NullPointerException(); }
    createMutation().appendChildren(Arrays.asList(params)).execute();
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

  public void render(RenderContext rc) throws IOException {
    switch (op.getType()) {
      case PREFIX:
        rc.out.append(op.getSymbol());
        if (Character.isLetterOrDigit(op.getSymbol().charAt(0))) {
          rc.out.append(' ');
        }
        renderParam(0, rc);
        break;
      case POSTFIX:
        renderParam(0, rc);
        rc.out.append(op.getSymbol());
        break;
      case INFIX:
        renderParam(0, rc);
        switch (getOperator()) {
          default:
            // These spaces are necessary for security.
            // If they are not present, then rendered javascript might include
            // the strings ]]> or </script> which would prevent it from being
            // safely embedded in HTML or XML.
            rc.out.append(" ")
                .append(op.getSymbol())
                .append(" ");
            break;
          case MEMBER_ACCESS:
            rc.out.append(op.getSymbol());
            break;
          case COMMA:
            rc.out.append(op.getSymbol()).append(" ");
            break;
        }
        renderParam(1, rc);
        break;
      case BRACKET:
        renderParam(0, rc);
        rc.out.append(op.getOpeningSymbol());
        boolean seen = false;
        rc.indent += 2;
        for (Expression e : children().subList(1, children().size())) {
          if (seen) {
            rc.out.append(", ");
          } else {
            seen = true;
          }
          // make sure that comma operators are properly escaped
          if (!parenthesize(Operator.COMMA, false, e)) {
            e.render(rc);
          } else {
            rc.out.append("(");
            rc.indent += 2;
            e.render(rc);
            rc.indent -= 2;
            rc.out.append(")");
          }
        }
        rc.indent -= 2;
        rc.out.append(op.getClosingSymbol());
        break;
      case TERNARY:
        renderParam(0, rc);
        rc.out.append(" ").append(op.getOpeningSymbol()).append(" ");
        renderParam(1, rc);
        rc.out.append(" ").append(op.getClosingSymbol()).append(" ");
        renderParam(2, rc);
        break;
    }
  }

  private void renderParam(int i, RenderContext rc) throws IOException {
    Expression e = children().get(i);
    if (!parenthesize(op, 0 == i, e)) {
      e.render(rc);
    } else {
      rc.out.append("(");
      rc.indent += 2;
      e.render(rc);
      rc.indent -= 2;
      rc.out.append(")");
    }
  }

  private static boolean parenthesize(
      Operator op, boolean firstOp, Expression child) {
    // Parenthesize blocklike expressions
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

    // Parenthesize based on associativity and precedence
    Operator childOp = ((Operation) child).getOperator();
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
      // it a trinary op
      return (childOp.getAssociativity() == Associativity.LEFT) != firstOp;
    } else {
      return false;
    }
  }
}
