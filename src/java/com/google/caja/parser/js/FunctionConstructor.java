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
  private Identifier identifier;
  private List<FormalParam> params;
  private Block body;

  public FunctionConstructor(
      Void value, List<? extends ParseTreeNode> children) {
    createMutation().appendChildren(children).execute();
  }

  
  public FunctionConstructor(
      Identifier identifier, List<FormalParam> params, Block body) {
    createMutation()
        .appendChild(identifier)
        .appendChildren(params)
        .appendChild(body)
        .execute();
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    List<? extends ParseTreeNode> children = children();
    int n = children.size();
    this.identifier = (Identifier) children.get(0);
    this.params = Collections.<FormalParam>unmodifiableList(
        childrenPart(1, n - 1, FormalParam.class));
    for (FormalParam p : params) { }  // Implicit cast will check type
    this.body = (Block) children().get(n - 1);
  }

  public List<FormalParam> getParams() { return params; }

  public Block getBody() { return body; }

  public Identifier getIdentifier() { return identifier; }

  public String getIdentifierName() { return identifier.getName(); }

  @Override
  public Object getValue() { return null; }

  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());
    out.consume("function");
    String name = identifier.getName();
    if (null != name) {
      out.consume(name);
    }
    out.consume("(");
    boolean seen = false;
    for (FormalParam e : params) {
      if (seen) {
        out.consume(",");
      } else {
        seen = true;
      }
      e.render(rc);
    }
    out.consume(")");
    body.renderBlock(rc, false);
  }
}
