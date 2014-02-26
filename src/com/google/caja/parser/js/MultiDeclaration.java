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

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.reporting.RenderContext;
import java.util.List;

/**
 * A group of declarations as in
 *   {@code var a, b, c;}
 * or
 *   {@code for (var a, b, c; ;)}
 *
 * @author mikesamuel@gmail.com
 */
public final class MultiDeclaration extends AbstractStatement {
  private static final long serialVersionUID = 4634282561782526243L;

  /** @param value unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public MultiDeclaration(
      FilePosition pos, Void value, List<? extends Declaration> children) {
    this(pos, children);
  }

  public MultiDeclaration(FilePosition pos, List<? extends Declaration> decls) {
    super(pos, Declaration.class);
    createMutation().appendChildren(decls).execute();
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    if (children().isEmpty()) { throw new IllegalStateException(); }
    for (ParseTreeNode child : children()) {
      if (!(child instanceof Declaration)) {
        throw new IllegalArgumentException("Child must be a declaration");
      }
    }
  }

  @Override
  public Object getValue() { return null; }

  @Override
  public List<? extends Declaration> children() {
    return childrenAs(Declaration.class);
  }

  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());
    if (children().isEmpty()) {
      throw new SomethingWidgyHappenedError("Empty MultiDeclaration");
    }
    out.consume("var");
    boolean seen = false;
    for (Declaration decl : children()) {
      if (seen) {
        out.consume(",");
      } else {
        seen = true;
      }
      decl.renderShort(rc);
    }
  }

  public boolean hasHangingConditional() { return false; }
}
