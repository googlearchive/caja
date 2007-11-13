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

/**
 *
 * @author mikesamuel@gmail.com
 */
public class WhileLoop extends Loop {
  private Expression condition;
  private Statement body;
  private boolean isDoLoop;

  public WhileLoop(String label, Expression condition, Statement body,
                   boolean isDoLoop) {
    super(label);
    if (isDoLoop) {
      children.add(body);
      children.add(condition);
    } else {
      children.add(condition);
      children.add(body);
    }
    childrenChanged();
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    this.isDoLoop = children.get(0) instanceof Statement;
    if (isDoLoop) {
      this.condition = (Expression) children.get(1);
      this.body = (Statement) children.get(0);
    } else {
      this.condition = (Expression) children.get(0);
      this.body = (Statement) children.get(1);
    }
  }

  @Override
  public Expression getCondition() { return condition; }
  @Override
  public Statement getBody() { return body; }
  @Override
  public boolean isDoLoop() { return isDoLoop; }

  public void render(RenderContext rc) throws IOException {
    String label = getLabel();
    if (null != label && !"".equals(label)) {
      rc.out.append(label);
      rc.out.append(": ");
    }

    if (isDoLoop()) {
      rc.out.append("do");
      body.renderBlock(rc, true, true, true);
      rc.out.append("while (");
      rc.indent += 2;
      condition.render(rc);
      rc.indent -= 2;
      rc.out.append(")");
    } else {
      rc.out.append("while (");
      rc.indent += 2;
      condition.render(rc);
      rc.indent -= 2;
      rc.out.append(")");
      body.renderBlock(rc, true, false, false);
    }
  }
}
