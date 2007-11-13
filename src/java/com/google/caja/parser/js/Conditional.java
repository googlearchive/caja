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

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Pair;

import java.io.IOException;

import java.util.List;

/**
 *
 * @author mikesamuel@gmail.com
 */
public final class Conditional extends AbstractStatement<ParseTreeNode> {
  public Conditional(
      List<Pair<Expression, Statement>> ifClauses, Statement elseClause) {
    if (ifClauses.isEmpty()) { throw new IllegalArgumentException(); }
    for (Pair<Expression, Statement> p : ifClauses) {
      Expression condition = p.a;
      Statement then = p.b;
      if (null == condition || null == then) {
        throw new IllegalArgumentException();
      }
      this.children.add(condition);
      this.children.add(then);
    }
    if (null != elseClause) {
      this.children.add(elseClause);
    }
    childrenChanged();
  }

  @Override
  public Object getValue() { return null; }

  public void render(RenderContext rc) throws IOException {
    boolean seen = false;
    int i = 0;
    int n = children.size();
    while (i + 2 <= n) {
      Expression condition = (Expression) children.get(i++);
      Statement body = (Statement) children.get(i++);
      rc.out.append(seen ? "else if (" : "if (");
      rc.indent += 2;
      condition.render(rc);
      rc.out.append(")");
      rc.indent -= 2;
      body.renderBlock(rc, true, i < n, i < n);
      seen = true;
    }
    if (i < n) {
      Statement body = (Statement) children.get(i);
      rc.out.append("else");
      body.renderBlock(rc, true, false, false);
    }
  }
}
