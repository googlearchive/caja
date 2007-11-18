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
 * Introduces a variable into the current scope.
 *
 * @author mikesamuel@gmail.com
 */
public class Declaration extends AbstractStatement<Expression> {
  private Reference reference;
  private Expression initializer;

  public Declaration(String identifier, Expression initializer) {
    children.add(new Reference(identifier));
    if (null != initializer) { children.add(initializer); }
    childrenChanged();
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    this.reference = (Reference)children.get(0);    
    this.initializer = children.size() < 2 ? null : children.get(1);
    if (children.size() > 2) {
      throw new IllegalArgumentException(
          "Declaration should only have at most 2 children");
    }
  }

  public String getIdentifier() { return reference.getIdentifier(); }

  public Expression getInitializer() { return this.initializer; }

  @Override
  public String getValue() { return reference.getIdentifier(); }

  public void render(RenderContext rc) throws IOException {
    rc.out.append("var ");
    renderShort(rc);
  }

  /**
   * Renders the short form without the "var" keyword.
   * This is used in multi declarations, such as in
   * {@code for (var a = 0, b = 1, ...)}.
   */
  void renderShort(RenderContext rc) throws IOException {
    rc.out.append(reference.getIdentifier());
    if (null != initializer) {
      rc.out.append(" = ");
      initializer.render(rc);
    }
  }
}
