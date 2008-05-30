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

import java.util.List;

/**
 *
 * @author mikesamuel@gmail.com
 */
public final class Reference extends AbstractExpression<ParseTreeNode> {
  private Identifier identifier;

  public Reference(Void value, List<ParseTreeNode> children) {
    this((Identifier) children.get(0));
  }

  public Reference(Identifier identifier) {
    appendChild(identifier);
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    List<? extends ParseTreeNode> children = children();
    this.identifier = (Identifier) children.get(0);
    if (identifier.getName() == null) {
      throw new NullPointerException(
          "Cannot build Reference with null identifier");
    }
    if (children.size() > 1) {
      throw new IllegalArgumentException(
          "Reference has extraneous children "
          + children.subList(1, children.size()));
    }
  }

  public Identifier getIdentifier() { return this.identifier; }

  public String getIdentifierName() { return this.identifier.getName(); }

  @Override
  public Object getValue() { return null; }

  @Override
  public boolean isLeftHandSide() {
    return true;
  }

  public void render(RenderContext rc) {
    String name = getIdentifierName();
    if (name == null) {
      throw new IllegalStateException(
          "null name for declaration at " + getFilePosition());
    }
    rc.getOut().mark(getFilePosition());
    rc.getOut().consume(name);
  }
}
