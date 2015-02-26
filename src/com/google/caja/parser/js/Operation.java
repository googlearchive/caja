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

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.Keyword;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.reporting.RenderContext;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

/**
 * An expression that applies an {@link Operator} to a number of operands.
 *
 * @author mikesamuel@gmail.com
 */
public abstract class Operation extends AbstractExpression {
  private static final long serialVersionUID = -4339753364752801666L;
  private final Operator op;

  protected Operation(
      FilePosition pos, Operator op, List<? extends Expression> params) {
    super(pos, Expression.class);
    this.op = op;
    if (null == op) { throw new NullPointerException(); }
    ctorAppendChildren(params);
  }

  public static boolean is(ParseTreeNode n, Operator op) {
    return n instanceof Operation && op == ((Operation) n).getOperator();
  }

  public static boolean is(ParseTreeNode n, OperatorCategory cat) {
    return n instanceof Operation
        && cat == ((Operation) n).getOperator().getCategory();
  }

  @Override
  public List<? extends Expression> children() {
    return childrenAs(Expression.class);
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    List<? extends Expression> children = children();
    int nChildren = children.size();
    if (nChildren < minArity(op)) {
      throw new IllegalArgumentException(
          "Too few of children " + nChildren + " for operator " + op);
    } else if (nChildren > maxArity(op)) {
      throw new IllegalArgumentException(
          "Too many children " + nChildren + " for operator " + op);
    }
    if (Operator.MEMBER_ACCESS == op) {
      if (!(children.get(1) instanceof Reference)) {
        throw new IllegalArgumentException(
            "Bad child of . operator : " + children().get(1));
      }
    }
    if (OperatorCategory.ASSIGNMENT == op.getCategory()) {
      if (!children.get(0).isLeftHandSide()) {
        throw new IllegalArgumentException(
            "Invalid assignment " + op + " with left hand side "
            + children.get(0));
      }
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
        throw new SomethingWidgyHappenedError("unexpected: " + op);
    }
  }

  public static Operation createInfix(
      Operator op, Expression left, Expression right) {
    assert op.getType() == OperatorType.INFIX;
    return create(
        FilePosition.span(left.getFilePosition(), right.getFilePosition()),
        op, left, right);
  }

  public static Operation createTernary(
      Expression left, Expression middle, Expression right) {
    return create(
        FilePosition.span(left.getFilePosition(), right.getFilePosition()),
        Operator.TERNARY, left, middle, right);
  }

  public static Operation undefined(FilePosition pos) {
    return create(pos, Operator.VOID, new IntegerLiteral(pos, 0));
  }

  @Override
  public Object getValue() { return op; }

  public Operator getOperator() { return op; }

  @Override
  public Expression simplifyForSideEffect() {
    List<? extends Expression> operands;
    switch (op) {
      case LOGICAL_OR: case LOGICAL_AND: case COMMA: {
        operands = children();
        Expression sa = operands.get(0).simplifyForSideEffect();
        Expression sb = operands.get(1).simplifyForSideEffect();
        return sb == null ? sa : sa == null ? sb : this;
      }
      case TERNARY:
        operands = children();
        if (operands.get(1).simplifyForSideEffect() == null
            && operands.get(2).simplifyForSideEffect() == null) {
          return operands.get(0).simplifyForSideEffect();
        }
        return this;
      case NOT: case TYPEOF: return children().get(0).simplifyForSideEffect();
      case VOID: return children().get(0).simplifyForSideEffect();
      case POST_INCREMENT:  // i++ => ++i
        return Operation.create(
            getFilePosition(), Operator.PRE_INCREMENT, children().get(0));
      case POST_DECREMENT:
        return Operation.create(
            getFilePosition(), Operator.PRE_DECREMENT, children().get(0));
      default: break;
    }
    return this;
  }

  @Override
  public @Nullable Boolean conditionResult() {
    Boolean opResult;
    List<? extends Expression> operands = children();
    switch (op) {
      case COMMA:
        return operands.get(1).conditionResult();
      case CONSTRUCTOR:
        return true;
      case FUNCTION_CALL:
        return null;
      case LOGICAL_AND:
        opResult = operands.get(0).conditionResult();
        if (opResult != null) {
          return !opResult ? false : operands.get(1).conditionResult();
        } else {
          opResult = operands.get(1).conditionResult();
          if (opResult != null && !opResult) { return false; }
        }
        break;
      case LOGICAL_OR:
        opResult = operands.get(0).conditionResult();
        if (opResult != null) {
          return opResult ? true : operands.get(1).conditionResult();
        } else {
          opResult = operands.get(1).conditionResult();
          if (opResult != null && opResult) { return true; }
        }
        break;
      case TERNARY:
        opResult = operands.get(0).conditionResult();
        if (opResult != null) {
          return operands.get(opResult ? 1 : 2).conditionResult();
        } else {
          Boolean a = operands.get(1).conditionResult();
          Boolean b = operands.get(2).conditionResult();
          return a != null && a.equals(b) ? a : null;
        }
      case NOT:
        opResult = operands.get(0).conditionResult();
        return opResult != null ? !opResult : null;
      case VOID:
        return false;
      default: break;
    }
    return null;
  }

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
        if (op == Operator.CONSTRUCTOR) {
          // We emit zero-arg constructors without (), because it's shorter,
          // but that has implications in parenthesize() below.
          int n = children().size();
          if (1 < n) {
            out.consume("(");
            for (int k = 1; k < n; k++) {
              if (1 < k) { out.consume(","); }
              renderParam(k, rc);
            }
            out.consume(")");
          }
        }
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

  private static boolean isKeyword(String name) {
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
      // ({ x: 3 }).x is OK, but { x: 3 }.x can be confused with a statement.
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
        // unnecessarily parenthesizing them things less readable.
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

    // Since we emit zero-arg constructors without parens, we might have
    // to add parens to distinguish between (new A)() and (new A()).
    if (op == Operator.FUNCTION_CALL
        && firstOp
        && childOp == Operator.CONSTRUCTOR
        && child.children().size() == 1) {
      return true;
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

      if (op.getType() == OperatorType.PREFIX) {
        if (op == Operator.CONSTRUCTOR) {
          // The new operator tends to steal parentheses from calls.
          // There are two forms of the new operator:
          //   (1) new <Expression>
          //   (2) new <Expression>()
          // If the <Expression> has parentheses, then they might be stolen by
          // the new operator turning form 1 into form 2, as when <Expression>
          // is:
          //   foo().bar
          return mightHaveParenthesesStealableByNew((Operation) child);
        } else {
          // -(-a) is right associative but does not need to be parenthesized.
          // The JS printers prevent the two - signs from being merged into a
          // single -- token.
          return false;
        }
      }

      // ?: is right associative, so in a ? b : c, a would be parenthesized were
      // it a ternary op.
      return childOp.getAssociativity() == Associativity.LEFT != firstOp;
    } else {
      return false;
    }
  }

  private static int minArity(Operator op) {
    if (op == Operator.FUNCTION_CALL) { return 1; }
    if (op == Operator.CONSTRUCTOR) { return 1; }
    return op.getType().getArity();
  }

  private static int maxArity(Operator op) {
    if (op == Operator.FUNCTION_CALL) { return Integer.MAX_VALUE; }
    if (op == Operator.CONSTRUCTOR) { return Integer.MAX_VALUE; }
    return op.getType().getArity();
  }

  public String typeOf() {
    List<? extends Expression> operands = children();
    switch (getOperator()) {
      case PRE_INCREMENT: case PRE_DECREMENT: case TO_NUMBER: case NEGATION:
      case INVERSE: case MULTIPLICATION: case DIVISION: case MODULUS:
      case SUBTRACTION: case LSHIFT: case RSHIFT: case RUSHIFT:
      case BITWISE_AND: case BITWISE_OR: case BITWISE_XOR:
      case ASSIGN_MUL: case ASSIGN_DIV: case ASSIGN_MOD: case ASSIGN_SUB:
      case ASSIGN_LSH: case ASSIGN_RSH: case ASSIGN_USH: case ASSIGN_AND:
      case ASSIGN_XOR: case ASSIGN_OR:
        return "number";
      case DELETE: case IN: case NOT: case LESS_THAN: case GREATER_THAN:
      case LESS_EQUALS: case GREATER_EQUALS: case INSTANCE_OF: case EQUAL:
      case NOT_EQUAL: case STRICTLY_EQUAL: case STRICTLY_NOT_EQUAL:
        return "boolean";
      case VOID: return "undefined";
      case LOGICAL_OR: case LOGICAL_AND: case TERNARY: {
        String t1 = operands.get(operands.size() - 2).typeOf();
        String t2 = operands.get(operands.size() - 1).typeOf();
        return (t1 != null && t1.equals(t2)) ? t1 : null;
      }
      case ADDITION: {
        String t1 = operands.get(operands.size() - 2).typeOf();
        String t2 = operands.get(operands.size() - 1).typeOf();
        if ("string".equals(t1) || "string".equals(t2)) { return "string"; }
        if ("number".equals(t1) && "number".equals(t2)) { return "number"; }
        return null;
      }
      case TYPEOF: return "string";  // Trippy
      case COMMA:
        return operands.get(1).typeOf();
      default: return null;
    }
  }

  @Override
  public Expression fold(boolean isFn) {
    if (getOperator() == Operator.FUNCTION_CALL) { return foldCall(); }
    Expression folded;
    switch (children().size()) {
      case 1: folded = foldUnaryOp(); break;
      case 2: folded = foldBinaryOp(); break;
      case 3: folded = foldTernaryOp(); break;
      default: folded = this;
    }
    if (isFn && isFnSpecialForm(folded) && !isFnSpecialForm(this)) {
      if (is(this, Operator.COMMA)) {
        List<? extends Expression> operands = children();
        Expression left = operands.get(0);
        Expression right = operands.get(1);
        if (right == folded
            && left instanceof IntegerLiteral && left.getValue().equals(0L)) {
          return this;
        }
      }
      FilePosition pos = getFilePosition();
      folded = Operation.create(
          pos, Operator.COMMA,
          new IntegerLiteral(FilePosition.startOf(pos), 0), folded);
    }
    return folded;
  }

  private static final Expression[] NO_EXPRESSIONS = new Expression[0];
  private Expression foldUnaryOp() {
    Expression operand = children().get(0);
    switch (op) {
      case NOT:
        Boolean b = operand.conditionResult();
        if (b != null && operand.simplifyForSideEffect() == null) {
          return new BooleanLiteral(getFilePosition(), !b);
        }
        if (operand instanceof Operation) {
          Operation opr = (Operation) operand;
          Operator negation;
          switch (opr.getOperator()) {
            // Cannot do the same around comparison because of NaN.
            case NOT_EQUAL: negation = Operator.EQUAL; break;
            case EQUAL: negation = Operator.NOT_EQUAL; break;
            case STRICTLY_NOT_EQUAL: negation = Operator.STRICTLY_EQUAL; break;
            case STRICTLY_EQUAL: negation = Operator.STRICTLY_NOT_EQUAL; break;
            default: negation = null; break;
          }
          if (negation != null) {
            return Operation.create(
                getFilePosition(), negation,
                opr.children().toArray(NO_EXPRESSIONS));
          }
        }
        return this;
      case TYPEOF:
        String type = operand.typeOf();
        if (type != null && operand.simplifyForSideEffect() == null) {
          return StringLiteral.valueOf(getFilePosition(), type);
        }
        return this;
      default:
        break;
    }
    Object v = toLiteralValue(operand);
    if (v != null) {
      FilePosition pos = getFilePosition();
      switch (getOperator()) {
        case NEGATION:
          if (v instanceof Number) {
            if (operand instanceof IntegerLiteral) {
              long n = ((IntegerLiteral) operand).getValue().longValue();
              if (n != Long.MIN_VALUE && n != 0) {
                return new IntegerLiteral(pos, -n);
              }
            }
            return new RealLiteral(pos, -((Number) v).doubleValue());
          }
          break;
        case INVERSE:
          if (v instanceof Number) {
            return new IntegerLiteral(pos, ~((Number) v).longValue());
          }
          break;
        case TO_NUMBER:
          if (v instanceof Number) { return children().get(0); }
          break;
        default: break;
      }
    }
    return this;
  }

  private Expression foldTernaryOp() {
    if (getOperator() == Operator.TERNARY) {
      Expression cond = children().get(0);
      Boolean condResult = cond.conditionResult();
      if (condResult != null && cond.simplifyForSideEffect() == null) {
        return children().get(condResult ? 1 : 2);
      }
    }
    return this;
  }

  private Expression foldBinaryOp() {
    List<? extends Expression> operands = children();
    Expression left = operands.get(0), right = operands.get(1);
    Operator op = getOperator();
    if (op == Operator.LOGICAL_AND || op == Operator.LOGICAL_OR) {
      Boolean bv = left.conditionResult();
      if (bv != null) {
        Expression sideEffect = left.simplifyForSideEffect();
        if (bv == (op == Operator.LOGICAL_AND)) {
          return sideEffect == null
              ? right
              : Operation.createInfix(Operator.COMMA, sideEffect, right);
        } else {
          return left;
        }
      } else {
        bv = right.conditionResult();
        // foo != bar && true -> foo != bar
        if (bv != null && bv.booleanValue() == (op == Operator.LOGICAL_AND)
            && "boolean".equals(left.typeOf())
            && "boolean".equals(right.typeOf())
            && right.simplifyForSideEffect() == null) {
          return left;
        }
      }
    } else if (op == Operator.MEMBER_ACCESS) {
      if (left instanceof StringLiteral) {
        Reference r = (Reference) right;
        if ("length".equals(r.getIdentifierName())) {
          return new IntegerLiteral(
              getFilePosition(),
              ((StringLiteral) left).getUnquotedValue().length());
        }
      }
    } else if (op == Operator.COMMA) {
      if (left.simplifyForSideEffect() == null) { return right; }
    }
    Object lhs = toLiteralValue(left);
    Object rhs = toLiteralValue(right);
    if (lhs != null && rhs != null) {
      FilePosition pos = getFilePosition();
      switch (op) {
        case EQUAL: case STRICTLY_EQUAL:
        case NOT_EQUAL: case STRICTLY_NOT_EQUAL:
          boolean isStrict =  op == Operator.STRICTLY_EQUAL
              || op == Operator.STRICTLY_NOT_EQUAL;
          boolean isEqual = op == Operator.EQUAL
              || op == Operator.STRICTLY_EQUAL;
          boolean areEqual = lhs.equals(rhs);
          if (isStrict || areEqual
              || lhs.getClass().equals(rhs.getClass())) {
            return new BooleanLiteral(pos, areEqual == isEqual);
          }
          break;
        case ADDITION:
          if ((lhs instanceof String) || (rhs instanceof String)) {
            if (lhs instanceof Number) {
              lhs = NumberLiteral.numberToString(
                  ((Number) lhs).doubleValue());
            } else if (rhs instanceof Number) {
              rhs = NumberLiteral.numberToString(
                  ((Number) rhs).doubleValue());
            }
            return StringLiteral.valueOf(pos, "" + lhs + rhs);
          }
          break;
        default: break;
      }
      if (lhs instanceof Number && rhs instanceof Number) {
        double a = ((Number) lhs).doubleValue();
        double b = ((Number) rhs).doubleValue();
        if (isIntOp(op)) {
          long result;
          switch (op) {
            case BITWISE_AND: result = toInt32(a)  &   toInt32(b); break;
            case BITWISE_OR:  result = toInt32(a)  |   toInt32(b); break;
            case BITWISE_XOR: result = toInt32(a)  ^   toInt32(b); break;
            case LSHIFT:      result = toInt32(a)  <<  toUint32(b); break;
            case RSHIFT:      result = toInt32(a)  >>  toUint32(b); break;
            case RUSHIFT:     result = toUint32(a) >>> toUint32(b); break;
            default: return this;
          }
          return new IntegerLiteral(pos, result);
        }
        double result;
        switch (op) {
          case ADDITION: result = a + b; break;
          case SUBTRACTION: result = a - b; break;
          case MULTIPLICATION: result = a * b; break;
          case DIVISION: result = a / b; break;
          case MODULUS: result = Math.IEEEremainder(a, b); break;
          default: return this;
        }
        return new RealLiteral(pos, result);
      }
    }
    return this;
  }

  /**
   * Fold some common string operations like indexOf which show up frequently
   * in userAgent testing code.  This helps when we've inlined the value of
   * navigator.userAgent.
   */
  private Expression foldCall() {
    List<? extends Expression> operands = children();
    Expression fn = operands.get(0);
    if (fn instanceof FunctionConstructor) {
      // Inline calls to zero argument functions given zero arguments that
      // contain a single return statement or a single expression statement
      // that do not mention this or arguments.
      if (operands.size() == 1) {
        FunctionConstructor fc = (FunctionConstructor) fn;
        if (fc.getParams().isEmpty() && fc.getIdentifierName() == null) {
          switch (fc.getBody().children().size()) {
            case 0: return undefined(getFilePosition());
            case 1:
              Statement s = fc.getBody().children().get(0);
              if (s instanceof ReturnStmt) {
                Expression e = ((ReturnStmt) s).getReturnValue();
                if (e == null) { return undefined(getFilePosition()); }
                if (!mentionsThisOrArguments(e)) { return e; }
              } else if (s instanceof ExpressionStmt) {
                Expression e = ((ExpressionStmt) s).getExpression();
                if (!mentionsThisOrArguments(e)) {
                  return create(getFilePosition(), Operator.VOID, e);
                }
              }
              break;
          }
        }
      }
    } else if (is(fn, Operator.MEMBER_ACCESS)
               && fn.children().get(0) instanceof StringLiteral) {
      StringLiteral sl = (StringLiteral) fn.children().get(0);
      String methodName = ((Reference) fn.children().get(1))
          .getIdentifierName();
      if ("indexOf".equals(methodName)) {
        if (operands.size() == 2) {
          Expression target = operands.get(1);
          if (target instanceof StringLiteral) {
            int index = sl.getUnquotedValue().indexOf(
                ((StringLiteral) target).getUnquotedValue());
            return new IntegerLiteral(getFilePosition(), index);
          }
        }
      }
    }
    return this;
  }

  private static final Object UNDEFINED = new Object() {
    @Override public String toString() { return "undefined"; }
  };
  private static Object toLiteralValue(Expression e) {
    if (is(e, Operator.VOID) && e.simplifyForSideEffect() == null) {
      return UNDEFINED;
    }
    if (e instanceof Literal) {
      if (e instanceof StringLiteral) {
        return ((StringLiteral) e).getUnquotedValue();
      }
      if (e instanceof NumberLiteral) {
        return ((NumberLiteral) e).doubleValue();
      }
      if (e instanceof BooleanLiteral || e instanceof NullLiteral) {
        return e.getValue();
      }
    }
    return null;
  }

  private static boolean isIntOp(Operator op) {
    switch (op) {
      case BITWISE_AND:
      case BITWISE_OR:
      case BITWISE_XOR:
      case LSHIFT:
      case RSHIFT:
      case RUSHIFT:
        return true;
      default:
        return false;
    }
  }


  private static final long TWO_TO_THE_32 = 0x100000000L;

  // For ToInt32 and ToUInt32, see sections 9.5 and 9.6 of ES5
  // http://wiki.ecmascript.org/doku.php?id=es3.1:es3.1_proposal_working_draft
  //
  // For Java %, see
  // http://java.sun.com/docs/books/jls/third_edition/html/expressions.html#15.17.3
  //
  // For Java (int) and (long), see
  // http://java.sun.com/docs/books/jls/third_edition/html/conversions.html#25363

  static long toInt32(double number) {
    // 1. Let number be the result of calling ToNumber on the input argument.
    // 2. If number is NaN, +0, -0, +Inf, or -Inf, return +0.
    // 3. Let posInt be sign(number) * floor(abs(number)).
    number = (number < 0.0 ? Math.ceil(number) : Math.floor(number));
    // 4. Let int32bit be posInt modulo 2**32; that is, a finite integer value k
    // of Number type with positive sign and less than 2**32 in magnitude such
    // that the mathematical difference of posInt and k is mathematically an
    // integer multiple of 2**32.
    double int32bit = number % TWO_TO_THE_32;  // Handles Inf and NaN from (2)
    // 5. If int32bit is greater than or equal to 2**31, return
    // int32bit - 2**32, otherwise return int32bit.
    return (int) (long) int32bit;
  }

  static long toUint32(double number) {
    // 1. Let number be the result of calling ToNumber on the input argument.
    // 2. If number is NaN, +0, -0, +Inf, or -Inf, return +0.
    // 3. Let posInt be sign(number) * floor(abs(number)).
    number = (number < 0.0 ? Math.ceil(number) : Math.floor(number));
    // 4. Let int32bit be posInt modulo 2**32; that is, a finite integer value k
    // of Number type with positive sign and less than 2**32 in magnitude such
    // that the mathematical difference of posInt and k is mathematically an
    // integer multiple of 2**32.
    double int32bit = number % TWO_TO_THE_32;  // Handles Inf and NaN from (2)
    // 5. Return int32bit.
    return ((long) int32bit) & 0xffffffffL;
  }

  private static boolean mentionsThisOrArguments(Expression e) {
    if (e instanceof Reference) {
      String s = ((Reference) e).getIdentifierName();
      return ("this".equals(s) || "arguments".equals(s));
    } else if (e instanceof FunctionConstructor) { return false; }
    for (ParseTreeNode c : e.children()) {
      if (c instanceof Expression && mentionsThisOrArguments((Expression) c)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isFnSpecialForm(Expression e) {
    if (e instanceof Reference) {
      Reference r = (Reference) e;
      return "eval".equals(r.getIdentifierName());
    } else if (e instanceof Operation) {
      Operator op = ((Operation) e).getOperator();
      return Operator.MEMBER_ACCESS == op || Operator.SQUARE_BRACKET == op;
    } else {
      return false;
    }
  }

  private static boolean mightHaveParenthesesStealableByNew(Operation oper) {
    Operator op = oper.getOperator();
    if (op == Operator.FUNCTION_CALL) { return true; }
    if (op.getPrecedence() > Operator.CONSTRUCTOR.getPrecedence()) {
      return false;
    }
    int lastToCheck;
    switch (op) {
      case SQUARE_BRACKET: lastToCheck = 1; break;
      case MEMBER_ACCESS: lastToCheck = 2; break;
      default: return true;
    }
    for (int i = 0; i < lastToCheck; ++i) {
      Expression child = oper.children().get(i);
      if (child instanceof Operation
          && mightHaveParenthesesStealableByNew((Operation) child)) {
        return true;
      }
    }
    return false;
  }
}
