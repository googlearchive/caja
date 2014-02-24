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

/**
 * Introduces a variable into the current scope.
 *
 * @author mikesamuel@gmail.com
 */
public class Declaration extends AbstractStatement {
  private static final long serialVersionUID = 8412211687133669346L;

  // Local member variables are only changed in childrenChanged(),
  // so this class satisfies the immutability contract of the superclass.
  private Identifier identifier;
  private Expression initializer;

  /** @param value unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public Declaration(
      FilePosition pos, Void value, List<? extends ParseTreeNode> children) {
    super(pos, ParseTreeNode.class);
    createMutation().appendChildren(children).execute();
  }

  public Declaration(
      FilePosition pos, Identifier identifier, Expression initializer) {
    super(pos, ParseTreeNode.class);
    Mutation m = createMutation();
    m.appendChild(identifier);
    if (null != initializer) { m.appendChild(initializer); }
    m.execute();
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    List<? extends ParseTreeNode> children = children();
    this.identifier = (Identifier) children.get(0);
    if (this.identifier.getName() == null) {
      throw new NullPointerException();
    }
    this.initializer = (children.size() > 1
                        ? (Expression) children.get(1)
                        : null);
    if (children.size() > 2) {
      throw new IllegalArgumentException(
          "Declaration has extraneous children "
          + children.subList(2, children.size()));
    }
  }

  public Identifier getIdentifier() { return this.identifier; }

  public String getIdentifierName() { return this.identifier.getName(); }

  public Expression getInitializer() { return this.initializer; }

  @Override
  public Object getValue() { return null; }

  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());
    out.consume("var");
    renderShort(rc);
  }

  /**
   * Renders the short form without the "var" keyword.
   * This is used in multi declarations, such as in
   * {@code for (var a = 0, b = 1, ...)}.
   */
  void renderShort(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());
    if (identifier.getName() == null) {
      throw new IllegalStateException(
          "null name for declaration at " + getFilePosition());
    }
    identifier.render(rc);
    if (null != initializer) {
      out.consume("=");
      boolean isComma = Operation.is(initializer, Operator.COMMA);
      if (isComma) { out.consume("("); }
      initializer.render(rc);
      if (isComma) { out.consume(")"); }
    }
  }

  public boolean hasHangingConditional() { return false; }
}
