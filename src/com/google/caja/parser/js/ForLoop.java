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
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.reporting.RenderContext;
import java.util.List;

/**
 *
 * @author mikesamuel@gmail.com
 */
public final class ForLoop extends Loop implements NestedScope {
  private static final long serialVersionUID = 295928890053083020L;

  // Local member variables are only changed in childrenChanged(),
  // so this class satisfies the immutability contract of the superclass.
  private Statement initializer;
  private Expression condition;
  private Statement increment;
  private Statement body;

  @ReflectiveCtor
  public ForLoop(
      FilePosition pos, String value, List<? extends ParseTreeNode> children) {
    this(pos, value,
         (Statement) children.get(0),
         (Expression) children.get(1),
         (Statement) children.get(2),
         (Statement) children.get(3));
  }

  public ForLoop(
      FilePosition pos, String label, Statement initializer, Expression cond,
      Statement increment, Statement body) {
    super(pos, label, ParseTreeNode.class);
    createMutation()
        .appendChild(initializer)
        .appendChild(cond)
        .appendChild(increment)
        .appendChild(body)
        .execute();
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    List<? extends ParseTreeNode> children = children();
    this.initializer = (Statement) children.get(0);
    if (!(this.initializer instanceof Declaration
          || this.initializer instanceof ExpressionStmt
          || this.initializer instanceof MultiDeclaration
          || this.initializer instanceof Noop)) {
      throw new IllegalArgumentException("" + this.initializer);
    }
    this.condition = (Expression) children.get(1);
    this.increment = (Statement) children.get(2);
    this.body = (Statement) children.get(3);
  }

  @Override
  public Expression getCondition() { return condition; }
  @Override
  public Statement getBody() { return body; }
  public Statement getInitializer() { return this.initializer; }
  public Statement getIncrement() { return this.increment; }

  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());
    String label = getRenderedLabel();
    if (null != label) {
      out.consume(label);
      out.consume(":");
    }
    out.consume("for");
    out.consume("(");
    if (!(initializer instanceof Noop)) { initializer.render(rc); }
    out.consume(JsMinimalPrinter.NOOP);
    if (!(condition instanceof BooleanLiteral
          && ((BooleanLiteral) condition).getValue())) {
      condition.render(rc);
    }
    out.consume(JsMinimalPrinter.NOOP);
    if (!(increment instanceof Noop)) { increment.render(rc); }
    out.consume(")");
    getBody().renderBlock(rc, false);
  }

  public boolean hasHangingConditional() {
    return body.hasHangingConditional();
  }
}
