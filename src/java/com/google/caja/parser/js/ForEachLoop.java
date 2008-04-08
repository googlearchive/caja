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
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.reporting.RenderContext;

import java.util.List;

/**
 * <code>for (key in container) body</code>
 *
 * @author mikesamuel@gmail.com
 */
public final class ForEachLoop extends LabeledStatement implements NestedScope {
  private Statement keyReceiver;
  private Expression container;
  private Statement body;

  public ForEachLoop(String value, List<? extends ParseTreeNode> children) {
    super(value);
    createMutation().appendChildren(children).execute();
  }

  public ForEachLoop(String label, Declaration var, Expression container,
                     Statement body) {
    super(label);
    createMutation()
        .appendChild(var)
        .appendChild(container)
        .appendChild(body)
        .execute();
  }

  public ForEachLoop(
      String label, Expression lvalue, Expression container, Statement body) {
    super(label);
    ExpressionStmt varStmt = new ExpressionStmt(lvalue);
    varStmt.setFilePosition(lvalue.getFilePosition());
    createMutation()
        .appendChild(varStmt)
        .appendChild(container)
        .appendChild(body)
        .execute();
  }

  public Expression getContainer() { return container; }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    List<? extends ParseTreeNode> children = children();
    this.keyReceiver = (Statement) children.get(0);
    if (keyReceiver instanceof ExpressionStmt) {
      Expression e = ((ExpressionStmt) keyReceiver).getExpression();
      if (!e.isLeftHandSide()) {
        throw new IllegalArgumentException("Not an lvalue: " + e);
      }
    }
    this.container = (Expression) children.get(1);
    this.body = (Statement) children.get(2);
  }

  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());
    String label = getLabel();
    if (null != label && !"".equals(label)) {
      out.consume(label);
      out.consume(":");
    }
    out.consume("for");
    out.consume("(");
    keyReceiver.render(rc);
    out.consume("in");
    container.render(rc);
    out.consume(")");
    body.renderBlock(rc, false);
  }
}
