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
import java.util.Collections;

/**
 * Sometimes called a function literal or a closure, an expression that
 * constructs a new function.
 *
 * <p>E.g.
 * <code>function () { return 0; }</code>
 *
 * @author mikesamuel@gmail.com
 */
public final class FunctionConstructor
    extends AbstractExpression<ParseTreeNode> implements NestedScope {
  private String identifier;
  private List<FormalParam> params;
  private Block body;

  public FunctionConstructor(
      String identifier, List<FormalParam> params, Block body) {
    this.identifier = identifier;
    children.addAll(params);
    children.add(body);
    childrenChanged();
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    int n = children.size();
    this.params = Collections.<FormalParam>unmodifiableList(
      childrenPart(0, n - 1, FormalParam.class));
    this.body = (Block) children.get(n - 1);
  }

  public List<FormalParam> getParams() { return params; }

  public Block getBody() { return body; }

  public String getName() { return identifier; }
  public void clearName() { identifier = null; }

  @Override
  public Object getValue() { return identifier; }

  public void render(RenderContext rc) throws IOException {
    rc.out.append("function ");
    if (null != identifier) { rc.out.append(identifier); }
    rc.out.append('(');
    rc.indent += 2;
    boolean seen = false;
    for (FormalParam e : params) {
      if (seen) {
        rc.out.append(", ");
      } else {
        seen = true;
      }
      e.render(rc);
    }
    rc.indent -= 2;
    rc.out.append(')');
    body.renderBlock(rc, true, false, false);
  }
}
