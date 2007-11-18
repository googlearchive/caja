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
import com.google.caja.util.Pair;

import java.io.IOException;

import java.util.Iterator;
import java.util.List;

/**
 * Sometimes called an object literal, a shorthand for constructing an object
 * with a declared set of properties.  I avoid the term object literal since
 * everytime the expression is evaluated, it results in a new object, and
 * subexpressions need not be literal.
 *
 * <p>E.g.
 * <code>{ x: 0, y : 1}</code>
 *
 * @author mikesamuel@gmail.com
 */
public final class ObjectConstructor extends AbstractExpression<Expression> {
  public ObjectConstructor(List<Pair<Literal, Expression>> properties) {
    for (Pair<Literal, Expression> p : properties) {
      children.add(p.a);
      children.add(p.b);
    }
    childrenChanged();
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    // Make sure that all children are expressions and that the left hand sides
    // are literals.

    // There need to be an even number of children, so to add or remove a pair
    // use the transaction safe {@link MutableParseTreeNode#createMutation}.
    List<? extends Expression> children = children();

    if (0 != (children.size() & 1)) {
      throw new IllegalArgumentException("Odd number of children");
    }

    for (int i = children.size(); --i >= 0;) {
      Expression e = children.get(i);
      if ((i & 1) == 0) {
        if (!(e instanceof StringLiteral)) {
          throw new ClassCastException(
              "object field must be a string literal, not " + e);
        }
      }
    }
  }

  @Override
  public Object getValue() { return null; }

  public void render(RenderContext rc) throws IOException {
    rc.out.append("{");
    rc.indent += 2;
    boolean seen = false;
    Iterator<Expression> els = children.iterator();
    while (els.hasNext()) {
      Expression key = els.next(),
                 value = els.next();
      if (seen) {
        rc.out.append(",");
      } else {
        seen = true;
      }
      rc.newLine();
      key.render(rc);
      rc.out.append(": ");
      if (!(value instanceof Operation
            && Operator.COMMA == ((Operation) value).getOperator())) {
        value.render(rc);
      } else {
        rc.out.append("(");
        value.render(rc);
        rc.out.append(")");
      }
    }
    rc.indent -= 2;
    rc.newLine();
    rc.out.append("}");
  }
}
