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
import com.google.caja.lexer.Keyword;
import com.google.caja.reporting.RenderContext;
import com.google.caja.parser.ParseTreeNode;

import java.util.Arrays;
import java.util.List;

/**
 * An expression that applies an {@link Operator} to a number of operands.
 *
 * @author mikesamuel@gmail.com
 */
public abstract class Operation extends AbstractExpression {
  private Operator op;

  protected Operation(
      FilePosition pos, Operator op, List<? extends Expression> params) {
    super(pos, Expression.class);
    this.op = op;
    if (null == op) { throw new NullPointerException(); }
    createMutation().appendChildren(params).execute();
  }

  @Override
  public List<? extends Expression> children() {
    return childrenAs(Expression.class);
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

  public static Operation create(
      FilePosition pos, Operator op, Expression... params) {
    switch (op.getCategory()) {
      case ASSIGNMENT:
        return new AssignOperation(pos, op, Arrays.asList(params));
      case CONTROL:
        return new ControlOperation(pos, op, Arrays.asList(params));
      case SPECIAL:
        return new SpecialOperation(pos, op, Arrays.asList(params));
      case SIMPLE:
        return new SimpleOperation(pos, op, Arrays.asList(params));
      default:
        throw new RuntimeException("unexpected: " + op);
    }
  }

  public static Operation createInfix(
      Operator op, Expression left, Expression right) {
    assert op.getType() == OperatorType.INFIX;
    return create(
        FilePosition.span(left.getFilePosition(), right.getFilePosition()),
        op, left, right);
  }

  public static Operation undefined(FilePosition pos) {
    return create(pos, Operator.VOID, new IntegerLiteral(pos, 0));
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
            renderParam(1, rc);
            break;
          case MEMBER_ACCESS:
            renderMemberAccess(rc);
            break;
          case COMMA:
            out.consume(op.getSymbol());
            renderParam(1, rc);
            break;
        }
        break;
      case BRACKET:
        // Note that FUNCTION_CALL is a BRACKET operator; this is why we can
        // have any number of child expressions, not just two.
        renderParam(0, rc);
        out.consume(op.getOpeningSymbol());
        boolean seen = false;
        for (ParseTreeNode e : children().subList(1, children().size())) {
          if (seen) {
            out.consume(",");
          } else {
            seen = true;
          }
          // make sure that comma operators are properly escaped
          if (!parenthesize(Operator.COMMA, false, (Expression) e)) {
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
    ParseTreeNode e = children().get(i);
    out.mark(e.getFilePosition());
    if (!parenthesize(op, 0 == i, (Expression) e)) {
      e.render(rc);
    } else {
      out.consume("(");
      e.render(rc);
      out.mark(FilePosition.endOfOrNull(getFilePosition()));
      out.consume(")");
    }
  }

  private void renderMemberAccess(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    if (isKeywordAccess()) {
      out.consume(Operator.SQUARE_BRACKET.getOpeningSymbol());
      StringLiteral.renderUnquotedValue(getMemberName(), rc);
      out.consume(Operator.SQUARE_BRACKET.getClosingSymbol());
    } else {
      out.consume(op.getSymbol());
      renderParam(1, rc);
    }
  }

  private boolean isKeywordAccess() {
    return getOperator() == Operator.MEMBER_ACCESS
        && children().get(1) instanceof Reference
        && isKeyword(getMemberName());
  }

  private String getMemberName() {
    return ((Reference) children().get(1)).getIdentifierName();
  }

  private boolean isKeyword(String name) {
    return Keyword.fromString(name) != null;
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

    boolean isDividend = firstOp && (op == Operator.DIVISION
                                     || op == Operator.ASSIGN_DIV);
    if (isDividend) {
      if (!(child instanceof Reference || child instanceof NumberLiteral
            || child instanceof Operation)) {
        // Parenthesize the left operand to division operators to reduce the
        // chance of the left being mis-parsed as a statement or something
        // else that can precede a regular expression.
        return true;
      }
    }

    if (!(child instanceof Operation)) { return false; }

    Operator childOp = ((Operation) child).getOperator();

    if (firstOp) {
      if (childOp == Operator.FUNCTION_CALL && op == Operator.MEMBER_ACCESS) {
        // Don't parenthesize foo().bar since the LHS of the function call must
        // already be parenthesized if it were ambiguous since function call
        // binds less tightly than member access, and the actuals are already
        // parenthesized since the function call operator is "()".
        return false;
      }
      if (isDividend) {
        // By inspection of the grammar, a slash after a function call
        // or a member access is a division op, so no chance of
        // lexical ambiguity here.  These are also common enough that
        // unecessarily parenthesizing them things less readable.
        if (childOp != Operator.FUNCTION_CALL
            && childOp != Operator.MEMBER_ACCESS) {
          return true;
        }
      }
      // Make sure that the sequence -- > never appears in embedded mode
      // rendered source, since according to HTML4,
      //   White space is not permitted between the markup declaration open
      //   delimiter("<!") and the comment open delimiter ("--"), but is
      //   permitted between the comment close delimiter ("--") and the
      //   markup declaration close delimiter (">").
      if (childOp == Operator.POST_DECREMENT) {
        switch (op) {
          case ASSIGN_RSH: case ASSIGN_USH:
          case RSHIFT: case RUSHIFT:
          case GREATER_THAN: case GREATER_EQUALS:
            return true;
          default:
            break;
        }
      }
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
