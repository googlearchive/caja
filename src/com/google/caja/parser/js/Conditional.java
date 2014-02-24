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
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Pair;
import java.util.List;

/**
 * An if/else if/else statement.
 *
 * @author mikesamuel@gmail.com
 */
public final class Conditional extends AbstractStatement {
  private static final long serialVersionUID = 7726408694815849867L;

  /** @param value unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public Conditional(
      FilePosition pos, Void value, List<? extends ParseTreeNode> children) {
    super(pos, ParseTreeNode.class);
    createMutation().appendChildren(children).execute();
  }

  public Conditional(
      FilePosition pos, List<Pair<Expression, Statement>> ifClauses,
      Statement elseClause) {
    super(pos, ParseTreeNode.class);
    if (ifClauses.isEmpty()) { throw new IllegalArgumentException(); }
    Mutation m = createMutation();
    for (Pair<Expression, Statement> p : ifClauses) {
      Expression condition = p.a;
      Statement then = p.b;
      if (null == condition || null == then) {
        throw new IllegalArgumentException();
      }
      m.appendChild(condition);
      m.appendChild(then);
    }
    if (null != elseClause) {
      m.appendChild(elseClause);
    }
    m.execute();
  }

  @Override
  public Object getValue() { return null; }

  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());
    List<? extends ParseTreeNode> children = children();
    int i = 0;
    int n = children.size();
    for (; i + 2 <= n; i += 2) {
      Expression condition = (Expression) children.get(i);
      Statement body = (Statement) children.get(i + 1);

      if (i != 0) {
        out.consume("else");
      }
      out.consume("if");
      out.consume("(");
      condition.render(rc);
      out.consume(")");
      boolean hanging = body.hasHangingConditional();
      if (hanging) { out.consume("{"); }
      body.renderBlock(rc, i + 2 < n);
      if (hanging) { out.consume("}"); }
    }
    if (i < n) {
      Statement body = (Statement) children.get(i);
      out.consume("else");
      body.renderBlock(rc, false);
    }
  }

  public boolean hasHangingConditional() {
    List<? extends ParseTreeNode> children = children();
    int n = children.size();
    // If there is no else clause, then an else following would change the
    // meaning.
    if ((n & 1) == 0) { return true; }
    // Otherwise if the else clause has a hanging conditional, then an else
    // would change the meaning.
    return ((Statement) children.get(n - 1)).hasHangingConditional();
  }
}
