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
import java.util.List;

/**
 * A statement that contains an expression as its only child.  When control
 * reaches this statement, the expression is invoked, presumably for its side
 * effects, and the value is discarded.
 *
 * @author mikesamuel@gmail.com
 */
public final class ExpressionStmt extends AbstractStatement {
  private static final long serialVersionUID = 4277971387206538109L;

  /** @param value unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public ExpressionStmt(
      FilePosition pos, Void value, List<? extends Expression> children) {
    this(pos, children.get(0));
  }

  public ExpressionStmt(FilePosition pos, Expression expr) {
    super(pos, Expression.class);
    ctorAppendChild(expr);
  }

  public ExpressionStmt(Expression expr) {
    super(expr.getFilePosition(), Expression.class);
    ctorAppendChild(expr);
  }

  @Override
  protected void childrenChanged() {
    if (1 != children().size()) { throw new IllegalStateException(); }
  }

  public Expression getExpression() { return (Expression) children().get(0); }

  @Override
  public Object getValue() { return null; }

  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());
    Expression e = getExpression();
    if (e instanceof FunctionConstructor
        || e instanceof ObjectConstructor
        || startsWithRegex(e)) {
      // We need to parenthesize Object constructors because otherwise an
      // object constructor with only one entry:
      //   { x : 4 }
      // is ambiguous.  It could be a block containing a labeled expression
      // statement, and depending on semicolon insertion.

      // We need to parenthesize Function constructors because otherwise
      // we might output something like
      //   function a () {
      //     ;
      //   };
      // which is interpreted as two statements -- a declaration and a noop for
      // the semicolon.

      // Rhino fails to parse
      //   if(...)/foo/.test(x)?bar:baz;
      // so we parenthesize operator trees whose left-most operand is a regex
      // literal.

      out.consume("(");
      e.render(rc);
      out.consume(")");
    } else {
      e.render(rc);
    }
  }

  public boolean hasHangingConditional() { return false; }

  private static boolean startsWithRegex(Expression e) {
    while (e instanceof Operation) {
      Operation op = (Operation) e;
      if (op.getOperator().getType() == OperatorType.PREFIX) { break; }
      e = op.children().get(0);
    }
    return e instanceof RegexpLiteral;
  }
}
