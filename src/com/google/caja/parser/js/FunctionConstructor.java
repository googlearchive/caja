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
    extends AbstractExpression implements NestedScope {
  private static final long serialVersionUID = 4183249730129328478L;

  // Local member variables are only changed in childrenChanged(),
  // so this class satisfies the immutability contract of the superclass.
  private Identifier identifier;
  private List<FormalParam> params;
  private Block body;

  /** @param value unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public FunctionConstructor(
      FilePosition pos, Void value, List<? extends ParseTreeNode> children) {
    super(pos, ParseTreeNode.class);
    createMutation().appendChildren(children).execute();
  }


  public FunctionConstructor(
      FilePosition pos, Identifier identifier, List<FormalParam> params,
      Block body) {
    super(pos, ParseTreeNode.class);
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
    for (ParseTreeNode p : params) {
      if (!(p instanceof FormalParam)) {
        throw new ClassCastException(p.getClass().getName());
      }
    }
    this.body = (Block) children().get(n - 1);
  }

  public List<FormalParam> getParams() { return params; }

  public Block getBody() { return body; }

  public Identifier getIdentifier() { return identifier; }

  public String getIdentifierName() { return identifier.getName(); }

  @Override
  public Object getValue() { return null; }

  @Override
  public Boolean conditionResult() { return true; }

  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());
    out.consume("function");
    String name = identifier.getName();
    if (null != name) {
      out.consume(name);
    }
    renderActuals(rc);
    renderBody(rc);
  }

  void renderActuals(RenderContext rc) {
    TokenConsumer out = rc.getOut();
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
  }

  void renderBody(RenderContext rc) {
    body.renderBlock(rc, false);
  }

  public String typeOf() { return "function"; }
}
