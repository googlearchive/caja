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
public final class Block
    extends AbstractStatement<Statement> implements NestedScope {
  public Block(Void value, List<? extends Statement> children) {
    this(children);
  }

  public Block(List<? extends Statement> elements) {
    createMutation().appendChildren(elements).execute();
  }

  public Block() {}

  public void prepend(Statement statement) {
    insertBefore(statement, children().isEmpty() ? null : children().get(0));
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    for (ParseTreeNode child : children()) {
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
    for (Statement stmt : children()) {
      out.mark(stmt.getFilePosition());
      stmt.render(rc);
      if (!stmt.isTerminal()) {
        out.mark(FilePosition.endOfOrNull(stmt.getFilePosition()));
        out.consume(";");
      }
    }
    out.mark(FilePosition.endOfOrNull(getFilePosition()));
    out.consume("}");
  }

  public void render(RenderContext rc) {
    renderBlock(rc, false);
  }

  @Override
  public boolean isTerminal() {
    return true;
  }
}
