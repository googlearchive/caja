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
 * An exception handler that is part of a {@link TryStmt}.
 *
 * @author mikesamuel@gmail.com
 */
public final class CatchStmt extends AbstractStatement {
  private static final long serialVersionUID = 7105207645966261505L;

  // Local member variables are only changed in childrenChanged(),
  // so this class satisfies the immutability contract of the superclass.
  private Declaration exception;
  private Block body;

  /** @param value unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public CatchStmt(
      FilePosition pos, Void value, List<? extends ParseTreeNode> children) {
    this(pos, (Declaration) children.get(0), (Block) children.get(1));
  }

  public CatchStmt(FilePosition pos, Declaration exception, Block body) {
    super(pos, Statement.class);
    createMutation()
        .appendChild(exception)
        .appendChild(body)
        .execute();
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    List<? extends ParseTreeNode> children = children();
    this.exception = (Declaration) children.get(0);
    this.body = (Block) children.get(1);
  }

  public Declaration getException() { return exception; }
  public Block getBody() { return body; }

  @Override
  public Object getValue() { return null; }

  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());
    out.consume("catch");
    out.consume("(");
    out.mark(exception.getFilePosition());
    exception.getIdentifier().render(rc);
    out.consume(")");
    body.renderBlock(rc, false);
  }

  public boolean hasHangingConditional() { return false; }
}
