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
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.reporting.RenderContext;

import java.util.List;

/**
 * An expression that evaluates to the value associated with the property of the
 * same name in either the current scope chain, or the object to the left of a
 * dot-operator.
 *
 * @author mikesamuel@gmail.com
 */
public final class Reference extends AbstractExpression {
  private static final long serialVersionUID = -9159066877661689445L;

  /** @param value unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public Reference(FilePosition pos, Void value, List<ParseTreeNode> children) {
    super(pos, Identifier.class);
    ctorAppendChild(children.get(0));
  }

  public Reference(Identifier identifier) {
    super(identifier.getFilePosition(), Identifier.class);
    ctorAppendChild(identifier);
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    List<? extends ParseTreeNode> children = children();
    Identifier identifier = (Identifier) children.get(0);
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

  public Identifier getIdentifier() { return (Identifier) children().get(0); }

  public String getIdentifierName() { return getIdentifier().getName(); }

  @Override
  public Object getValue() { return null; }

  @Override
  public boolean isLeftHandSide() {
    return true;
  }

  public void render(RenderContext rc) {
    Identifier ident = getIdentifier();
    if (ident.getName() == null) {
      throw new IllegalStateException(
          "null name for declaration at " + getFilePosition());
    }
    ident.render(rc);
  }

  public String typeOf() { return null; }
}
