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
import com.google.caja.reporting.RenderContext;
import java.util.List;

/**
 *
 * @author mikesamuel@gmail.com
 */
public final class ReturnStmt extends AbstractStatement {
  private static final long serialVersionUID = 4757771638977210517L;

  // Local member variables are only changed in childrenChanged(),
  // so this class satisfies the immutability contract of the superclass.
  private Expression returnValue;

  /** @param value unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public ReturnStmt(
      FilePosition pos, Void value, List<? extends Expression> children) {
    super(pos, Expression.class);
    createMutation().appendChildren(children).execute();
  }

  public ReturnStmt(FilePosition pos, Expression returnValue) {
    super(pos, Expression.class);
    if (null != returnValue) {
      appendChild(returnValue);
    }
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    if (children().size() >= 2) { throw new IllegalArgumentException(); }
    this.returnValue = children().isEmpty() ? null : children().get(0);
  }

  /**
   * Null if nothing is returned, which implicitly returns the undefined value.
   */
  public Expression getReturnValue() { return returnValue; }

  @Override
  public Object getValue() { return null; }

  @Override
  public List<? extends Expression> children() {
    return childrenAs(Expression.class);
  }

  public void render(RenderContext rc) {
    rc.getOut().mark(getFilePosition());
    rc.getOut().consume("return");
    if (null != returnValue) {
      returnValue.render(rc);
    }
  }

  public boolean hasHangingConditional() { return false; }
}
