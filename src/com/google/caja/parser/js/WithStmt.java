// Copyright (C) 2008 Google Inc.
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
 * ES3-12.10: The with statement adds a computed object to the front
 * of the scope chain of the current execution context, then executes a
 * statement with this augmented scope chain, then restores the scope
 * chain. (ES5 expresses this in terms of environment records but with
 * similar effect.)
 *
 * @author mikesamuel@gmail.com
 */
public final class WithStmt extends AbstractStatement
    implements NestedScope {
  private static final long serialVersionUID = -466457790772474853L;

  /** @param value unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public WithStmt(
      FilePosition pos, Void value, List<? extends Statement> children) {
    super(pos, ParseTreeNode.class);
    createMutation().appendChildren(children).execute();
  }

  public WithStmt(FilePosition pos, Expression scopeObject, Statement body) {
    super(pos, ParseTreeNode.class);
    createMutation()
        .appendChild(scopeObject)
        .appendChild(body)
        .execute();
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    if (children().size() != 2) { throw new IllegalStateException(); }
    getScopeObject();
    getBody();
  }

  public Expression getScopeObject() { return (Expression) children().get(0); }
  public Statement getBody() { return (Statement) children().get(1); }

  @Override
  public Object getValue() { return null; }

  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.consume("with");
    out.consume("(");
    getScopeObject().render(rc);
    out.consume(")");
    getBody().renderBlock(rc, false);
  }

  public boolean hasHangingConditional() {
    return getBody().hasHangingConditional();
  }
}
