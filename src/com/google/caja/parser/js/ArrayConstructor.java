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
import com.google.caja.reporting.RenderContext;

import java.util.List;

/**
 * Sometimes called an array literal, a shorthand for initializing an array.
 *
 * @author mikesamuel@gmail.com
 */
public final class ArrayConstructor extends AbstractExpression {
  /** @param value unused.  This ctor is provided for reflection. */
  public ArrayConstructor(Void value, List<? extends Expression> children) {
    this(children);
  }

  public ArrayConstructor(List<? extends Expression> elements) {
    super(Expression.class);
    createMutation().appendChildren(elements).execute();
  }

  @Override
  public Object getValue() { return null; }

  @Override
  public List<Expression> children() {
    return childrenAs(Expression.class);
  }

  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    FilePosition pos = getFilePosition();
    out.mark(pos);
    out.consume("[");
    Expression last = null;
    for (Expression e : children()) {
      if (last != null) {
        out.consume(",");
      }
      last = e;
      if (!(e instanceof Operation
            && Operator.COMMA == ((Operation) e).getOperator())) {
        e.render(rc);
      } else {
        out.consume("(");
        e.render(rc);
        out.consume(")");
      }
    }
    out.mark(FilePosition.endOfOrNull(pos));
    out.consume("]");
  }
}
