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
import com.google.caja.parser.ParserBase;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Pair;

import java.util.Iterator;
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
 * @author mikesamuel@gmail.com
 */
public final class ObjectConstructor extends AbstractExpression {
  /** @param value unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public ObjectConstructor(
      FilePosition pos, Void value, List<? extends Expression> children) {
    super(pos, Expression.class);
    createMutation().appendChildren(children).execute();
  }

  public ObjectConstructor(
      FilePosition pos, List<Pair<Literal, Expression>> properties) {
    super(pos, Expression.class);
    Mutation m = createMutation();
    for (Pair<Literal, Expression> p : properties) {
      m.appendChild(p.a);
      m.appendChild(p.b);
    }
    m.execute();
  }

  public ObjectConstructor(FilePosition pos) {
    super(pos, Expression.class);
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

    for (int i = 0; i < children.size(); ++i) {
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

  @Override
  public List<? extends Expression> children() {
    return childrenAs(Expression.class);
  }

  public Expression getValue(String key) {
    List<? extends Expression> children = children();
    for (int i = 0, n = children.size(); i < n; i += 2) {
      StringLiteral sl = (StringLiteral) children.get(i);
      if (key.equals(sl.getUnquotedValue())) {
        return children.get(i + 1);
      }
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
    Iterator<? extends Expression> els = children().iterator();
    while (els.hasNext()) {
      Expression key = els.next(),
               value = els.next();
      if (seen) {
        out.consume(",");
        out.consume("\n");
      } else {
        seen = true;
      }
      String uqVal;
      if (rc.rawObjKeys()
          && key instanceof StringLiteral
          && ParserBase.isJavascriptIdentifier(
              uqVal = ((StringLiteral) key).getUnquotedValue())
          && !("get".equals(uqVal) || "set".equals(uqVal))) {
        out.consume(uqVal);
      } else {
        key.render(rc);
      }
      out.consume(":");
      if (!Operation.is(value, Operator.COMMA)) {
        value.render(rc);
      } else {
        out.mark(value.getFilePosition());
        out.consume("(");
        value.render(rc);
        out.consume(")");
      }
    }
    out.mark(FilePosition.endOfOrNull(getFilePosition()));
    out.consume("}");
  }

  public String typeOf() { return "object"; }
}
