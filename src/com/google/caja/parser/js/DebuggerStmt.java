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

import com.google.caja.reporting.RenderContext;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import java.util.List;

/**
 * A statement that is a noop in normal execution, but will trigger a breakpoint
 * if a debugger is attached.
 *
 * @author mikesamuel@gmail.com
 */
public final class DebuggerStmt extends AbstractStatement {
  private static final long serialVersionUID = 2458000650731417741L;

  /**
   * This ctor is provided for reflection.
   * @param value unused.
   * @param children unused.
   */
  @ReflectiveCtor
  public DebuggerStmt(
      FilePosition pos, Void value, List<? extends ParseTreeNode> children) {
    super(pos, NoChildren.class);
  }

  public DebuggerStmt(FilePosition pos) { super(pos, NoChildren.class); }

  @Override
  public Object getValue() { return null; }

  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());
    out.consume("debugger");
  }

  public boolean hasHangingConditional() { return false; }
}
