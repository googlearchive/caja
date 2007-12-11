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
 *
 * @author mikesamuel@gmail.com
 */
public final class ForLoop extends Loop implements NestedScope {
  private Statement initializer;
  private Expression condition;
  private Statement increment;
  private Statement body;

  public ForLoop(String label, Statement initializer, Expression cond,
                 Statement increment, Statement body) {
    super(label);
    if (null == initializer) { throw new NullPointerException(); }
    if (null == increment) { throw new NullPointerException(); }
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
    this.condition = (Expression) children.get(1);
    this.increment = (Statement) children.get(2);
    this.body = (Statement) children.get(3);
  }

  @Override
  public Expression getCondition() { return condition; }
  @Override
  public Statement getBody() { return body; }
  @Override
  public boolean isDoLoop() { return false; }
  public Statement getInitializer() { return this.initializer; }
  public Statement getIncrement() { return this.increment; }

  public void render(RenderContext rc) throws IOException {
    String label = getLabel();
    if (null != label && !"".equals(label)) {
      rc.out.append(label);
      rc.out.append(": ");
    }

    rc.out.append("for (");
    rc.indent += 2;
    initializer.render(rc);
    rc.out.append("; ");
    getCondition().render(rc);
    rc.out.append("; ");
    increment.render(rc);
    rc.indent -= 2;
    rc.out.append(")");
    getBody().renderBlock(rc, true, false, false);
  }
}
