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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author mikesamuel@gmail.com
 */
public final class SwitchStmt extends LabeledStatement {
  private static final long serialVersionUID = -7284973291082281855L;

  @ReflectiveCtor
  public SwitchStmt(
      FilePosition pos, String label, List<? extends ParseTreeNode> children) {
    super(pos, label, ParseTreeNode.class);
    createMutation().appendChildren(children).execute();
  }

  public SwitchStmt(
      FilePosition pos, String label,
      Expression valueExpr, List<SwitchCase> cases) {
    super(pos, label, ParseTreeNode.class);
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

  @Override
  public boolean isTargetForContinue() { return false; }

  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());
    String label = getRenderedLabel();
    if (null != label) {
      out.consume(label);
      out.consume(":");
    }
    Iterator<? extends ParseTreeNode> it = children().iterator();
    out.consume("switch");
    out.consume("(");
    it.next().render(rc);
    out.consume(")");
    out.consume("{");
    while (it.hasNext()) {
      SwitchCase caseStmt = (SwitchCase) it.next();
      caseStmt.render(rc);
    }
    out.mark(FilePosition.endOfOrNull(getFilePosition()));
    out.consume("}");
  }

  @Override
  public boolean isTerminal() {
    return true;
  }

  public boolean hasHangingConditional() { return false; }
}
