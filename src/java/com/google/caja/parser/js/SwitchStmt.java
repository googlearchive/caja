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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author mikesamuel@gmail.com
 */
public final class SwitchStmt extends LabeledStatement {
  public SwitchStmt(String label, List<? extends ParseTreeNode> children) {
    super(label);
    createMutation().appendChildren(children).execute();
  }

  public SwitchStmt(
      String label, Expression valueExpr, List<SwitchCase> cases) {
    super(label);
    createMutation().appendChild(valueExpr).appendChildren(cases).execute();
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    List<? extends ParseTreeNode> children = children();
    ParseTreeNode valueExpr = children.get(0);
    if (!(valueExpr instanceof Expression)) {
      throw new ClassCastException(
          "Expected " + Expression.class.getName() + " not "
          + valueExpr.getClass().getName());
    }
    for (ParseTreeNode node : children.subList(1, children.size())) {
      if (!(node instanceof SwitchCase)) {
        throw new ClassCastException(
            "Expected " + SwitchCase.class.getName() + " not "
            + (node != null ? node.getClass().getName() : "<null>"));
      }
    }
  }

  @Override
  public void continues(Map<String, List<ContinueStmt>> contsReaching) {
    // switch statements don't intercept continues
    for (ParseTreeNode child : children()) {
      if (child instanceof Statement) {
        ((Statement) child).continues(contsReaching);
      }
    }
  }

  public void render(RenderContext rc) throws IOException {
    String label = getLabel();
    if (null != label && !"".equals(label)) {
      rc.out.append(label);
      rc.out.append(": ");
    }
    Iterator<? extends ParseTreeNode> it = children().iterator();
    rc.out.append("switch (");
    rc.indent += 2;
    it.next().render(rc);
    rc.indent -= 2;
    rc.out.append(") {");
    rc.indent += 2;
    while (it.hasNext()) {
      rc.newLine();
      SwitchCase caseStmt = (SwitchCase) it.next();
      caseStmt.render(rc);
      if (!caseStmt.isTerminal()) {
        rc.out.append(";");
      }
    }
    rc.indent -= 2;
    rc.newLine();
    rc.out.append("}");
  }

  @Override
  public boolean isTerminal() {
    return true;
  }
}
