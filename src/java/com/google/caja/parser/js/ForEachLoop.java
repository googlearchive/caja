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

import java.io.IOException;
import java.util.List;

/**
 * <code>for (key in container) body</code>
 *
 * @author mikesamuel@gmail.com
 */
public final class ForEachLoop extends LabeledStatement implements NestedScope {
  private Statement var;
  private Expression container;
  private Statement body;

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
      String label, Reference var, Expression container, Statement body) {
    super(label);
    ExpressionStmt varStmt = new ExpressionStmt(var);
    varStmt.setFilePosition(var.getFilePosition());
    createMutation()
        .appendChild(varStmt)
        .appendChild(container)
        .appendChild(body)
        .execute();
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    List<? extends ParseTreeNode> children = children();
    this.var = (Statement) children.get(0);
    this.container = (Expression) children.get(1);
    this.body = (Statement) children.get(2);
  }

  public void render(RenderContext rc) throws IOException {
    String label = getLabel();
    if (null != label && !"".equals(label)) {
      rc.out.append(label);
      rc.out.append(": ");
    }

    rc.out.append("for (");
    rc.indent += 2;
    var.render(rc);
    rc.out.append(" in ");
    container.render(rc);
    rc.indent -= 2;
    rc.out.append(")");
    body.renderBlock(rc, true, false, false);
  }
}
