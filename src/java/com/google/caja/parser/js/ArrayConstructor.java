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
import java.util.List;

/**
 * Sometimes called an array literal, a shorthand for initializing an array.
 *
 * @author mikesamuel@gmail.com
 */
public final class ArrayConstructor
    extends AbstractExpression<Expression> {
  public ArrayConstructor(List<? extends Expression> elements) {
    this.children.addAll(elements);
    childrenChanged();
  }

  @Override
  public Object getValue() { return null; }

  public void render(RenderContext rc) throws IOException {
    rc.out.append("[");
    rc.indent += 2;
    boolean seen = false;
    for (Expression e : children) {
      if (seen) {
        rc.out.append(", ");
      } else {
        seen = true;
      }
      if (!(e instanceof Operation
            && Operator.COMMA == ((Operation) e).getOperator())) {
        e.render(rc);
      } else {
        rc.out.append("(");
        e.render(rc);
        rc.out.append(")");
      }
    }
    rc.indent -= 2;
    rc.out.append("]");
  }
}
