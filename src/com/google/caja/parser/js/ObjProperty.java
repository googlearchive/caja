// Copyright (C) 2010 Google Inc.
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
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.ParserBase;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;

import java.io.IOException;
import java.util.List;

/**
 * A property name and definitions associated with one property in an object.
 * <code>{ a: b, c: d }</code> has two properties.
 * A single {@code ObjProperty} may not uniquely define the value of a property
 * in an object ; an object may validly have two with the same name, e.g. a
 * getter and a setter.
 *
 * @author mikesamuel@gmail.com
 */
public abstract class ObjProperty extends AbstractParseTreeNode {
  private static final long serialVersionUID = 3384973990326624609L;

  public ObjProperty(StringLiteral name, Expression value) {
    this(FilePosition.span(name.getFilePosition(), value.getFilePosition()),
         name, value);
  }

  public ObjProperty(FilePosition pos, StringLiteral name, Expression value) {
    super(pos, Expression.class);
    this.createMutation().appendChild(name).appendChild(value).execute();
  }

  /**
   * Provided for reflection.
   * @param value unused
   */
  protected ObjProperty(
      FilePosition pos, Void value, List<? extends Expression> children) {
    this(pos, (StringLiteral) children.get(0), children.get(1));
    assert children.size() == 2;
  }

  @Override public final Object getValue() { return null; }

  @Override
  public List<? extends Expression> children() {
    return childrenAs(Expression.class);
  }

  public final StringLiteral getPropertyNameNode() {
    return (StringLiteral) children().get(0);
  }

  @Override
  public void childrenChanged() {
    super.childrenChanged();
    if (2 != children().size()) { throw new IndexOutOfBoundsException(); }
    getPropertyName();
  }

  public final String getPropertyName() {
    return ((StringLiteral) children().get(0)).getUnquotedValue();
  }

  public boolean isPropertyNameQuoted() {
    StringLiteral name = (StringLiteral) children().get(0);
    String rawName = name.getValue();
    if (rawName.length() == 0) { return false; }
    char ch0 = rawName.charAt(0);
    return ch0 == '"' || ch0 == '\'';
  }

  public final TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> handler) {
    return new JsPrettyPrinter(new Concatenator(out, handler));
  }

  protected final void renderPropertyName(
      RenderContext rc, boolean preferUnquoted) {
    StringLiteral key = (StringLiteral) children().get(0);
    TokenConsumer out = rc.getOut();
    boolean unquoted = preferUnquoted;
    switch (rc.propertyNameQuotingMode()) {
      case NO_QUOTES: unquoted = true; break;
      case PRESERVE_QUOTES:
        String name = key.getValue();
        if (name.length() == 0) {
          unquoted = true;
        } else {
          char ch = name.charAt(0);
          if (ch != '"' && ch != '\'') {
            unquoted = true;
          }
        }
        break;
      default: break;
    }
    if (unquoted || preferUnquoted) {
      String uqVal = key.getUnquotedValue();
      if (ParserBase.isJavascriptIdentifier(uqVal)
          && !("get".equals(uqVal) || "set".equals(uqVal))) {
        out.mark(key.getFilePosition());
        out.consume(uqVal);
        return;
      }
    }
    key.render(rc);
  }
}
