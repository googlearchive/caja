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
public final class ExpressionStmt extends AbstractStatement<Expression> {
  private Expression expr;

  /** @param value unused.  This ctor is provided for reflection. */
  public ExpressionStmt(Void value, List<? extends Expression> children) {
    this(children.get(0));
  }

  public ExpressionStmt(Expression expr) {
    appendChild(expr);
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    this.expr = children().get(0);
    if (1 != children().size()) { throw new IllegalStateException(); }
  }

  public Expression getExpression() { return expr; }

  @Override
  public Object getValue() { return null; }

  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());
    if (expr instanceof FunctionConstructor
        || expr instanceof ObjectConstructor) {
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
      out.consume("(");
      expr.render(rc);
      out.consume(")");
    } else {
      expr.render(rc);
    }
  }
}
