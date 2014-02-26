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
 * Sometimes called an object literal, a shorthand for constructing an object
 * with a declared set of properties.  I avoid the term object literal since
 * every time the expression is evaluated, it results in a new object, and
 * subexpressions need not be literal.
 *
 * <p>E.g.
 * <code>{ x: 0, y : 1 }</code>
 *
 * TODO(ihab.awad): Remove class ObjProperty and refactor this class so that
 * its children are simply an even-numbered list of Expression objects,
 * alternating between StringLiteral and Expression. This would restore the
 * fact that any child of an Expression node is itself an Expression node.
 *
 * @author mikesamuel@gmail.com
 */
public final class ObjectConstructor extends AbstractExpression {
  private static final long serialVersionUID = -6215544894374731498L;

  /** @param value unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public ObjectConstructor(
      FilePosition pos, Void value, List<? extends ObjProperty> properties) {
    super(pos, ObjProperty.class);
    createMutation().appendChildren(properties).execute();
  }

  public ObjectConstructor(
      FilePosition pos, List<? extends ObjProperty> properties) {
    this(pos, null, properties);
  }

  public ObjectConstructor(FilePosition pos) {
    super(pos, ObjProperty.class);
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();

    for (ParseTreeNode prop : children()) {
      if (!(prop instanceof ObjProperty)) {
        throw new ClassCastException(prop.getClass().getName());
      }
    }
  }

  @Override
  public Object getValue() { return null; }

  @Override
  public List<? extends ObjProperty> children() {
    return childrenAs(ObjProperty.class);
  }

  public ObjProperty propertyWithName(String key) {
    for (ObjProperty prop : children()) {
      if (key.equals(prop.getPropertyName())) { return prop; }
    }
    return null;
  }

  @Override
  public Boolean conditionResult() { return true; }

  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());
    out.consume("{");
    boolean seen = false;
    for (ObjProperty prop : children()) {
      if (seen) {
        out.consume(",");
        out.consume("\n");
      } else {
        seen = true;
      }
      prop.render(rc);
    }
    out.mark(FilePosition.endOfOrNull(getFilePosition()));
    out.consume("}");
  }

  public String typeOf() { return "object"; }
}
