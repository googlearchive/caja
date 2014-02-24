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
 * A group of statements executed serially
 * (except that FunctionDeclarations are hoisted).
 *
 * @author mikesamuel@gmail.com
 */
public final class Block extends AbstractStatement implements NestedScope {
  private static final long serialVersionUID = 9160842282840140257L;

  /** @param value unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public Block(
      FilePosition pos, Void value, List<? extends Statement> children) {
    this(pos, children);
  }

  public Block(FilePosition pos, List<? extends Statement> elements) {
    super(pos, Statement.class);
    ctorAppendChildren(elements);
  }

  public Block(FilePosition pos) { super(pos, Statement.class); }

  public Block() { this(FilePosition.UNKNOWN); }

  @Override
  public List<? extends Statement> children() {
    return childrenAs(Statement.class);
  }

  public void prepend(Statement statement) {
    insertBefore(statement, children().isEmpty() ? null : children().get(0));
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    boolean first = true;
    for (ParseTreeNode child : children()) {
      if (first) {
        first = false;
      } else if (child instanceof DirectivePrologue) {
        throw new IllegalArgumentException("Misplaced directive prologoue " +
                                           child.getFilePosition());
      }
      if (!(child instanceof Statement)) {
        throw new ClassCastException("Expected statement, not " + child);
      }
    }
  }

  @Override
  public Object getValue() { return null; }

  @Override
  public void renderBlock(RenderContext rc, boolean terminate) {
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());
    out.consume("{");
    renderBody(rc);
    out.consume("}");
  }

  public void renderBody(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    for (Statement stmt : children()) {
      out.mark(stmt.getFilePosition());
      stmt.render(rc);
      if (!stmt.isTerminal()) {
        out.mark(FilePosition.endOfOrNull(stmt.getFilePosition()));
        out.consume(";");
      }
    }
    out.mark(FilePosition.endOfOrNull(getFilePosition()));
  }

  public void render(RenderContext rc) {
    renderBlock(rc, false);
  }

  @Override
  public boolean isTerminal() {
    return true;
  }

  public boolean hasHangingConditional() { return false; }
}
