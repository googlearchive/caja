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
 * A try - catch - finally block.
 *
 * @author mikesamuel@gmail.com
 */
public final class TryStmt extends AbstractStatement {
  private static final long serialVersionUID = 8846827862159681652L;

  // Local member variables are only changed in childrenChanged(),
  // so this class satisfies the immutability contract of the superclass.
  private Block body;
  private CatchStmt cat;
  private FinallyStmt fin;

  /** @param value unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public TryStmt(
      FilePosition pos, Void value, List<? extends Statement> children) {
    super(pos, Statement.class);
    createMutation().appendChildren(children).execute();
  }

  public TryStmt(
      FilePosition pos, Block body, CatchStmt cat, FinallyStmt fin) {
    super(pos, Statement.class);
    Mutation m = createMutation().appendChild(body);
    if (cat != null) {
      m.appendChild(cat);
    }
    if (fin != null) {
      m.appendChild(fin);
    }
    m.execute();
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    List<? extends Statement> children = children();
    this.body = (Block) children.get(0);
    Statement stmt1 = children.get(1);
    Statement stmt2 = children.size() >= 3 ? children.get(2) : null;
    if (stmt2 != null) {
      this.cat = (CatchStmt) stmt1;
      this.fin = (FinallyStmt) stmt2;
    } else if (stmt1 instanceof FinallyStmt) {
      this.cat = null;
      this.fin = (FinallyStmt) stmt1;
    } else {
      this.cat = (CatchStmt) stmt1;
      this.fin = null;
    }
  }

  public Block getBody() { return this.body; }
  public CatchStmt getCatchClause() { return this.cat; }
  public FinallyStmt getFinallyClause() { return this.fin; }

  @Override
  public Object getValue() { return null; }

  @Override
  public List<? extends Statement> children() {
    return childrenAs(Statement.class);
  }

  public void render(RenderContext rc) {
    rc.getOut().mark(getFilePosition());
    rc.getOut().consume("try");
    body.renderBlock(rc, false);
    if (null != cat) {
      cat.renderBlock(rc, null != fin);
    }
    if (null != fin) {
      fin.renderBlock(rc, false);
    }
  }

  public boolean hasHangingConditional() { return false; }
}
