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
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.reporting.RenderContext;
import java.util.List;

/**
 *
 * @author mikesamuel@gmail.com
 */
public final class Noop extends AbstractStatement {
  private static final long serialVersionUID = 7334467105913341242L;

  /** @param value unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public Noop(FilePosition p, Void value, List<? extends Statement> children) {
    super(p, NoChildren.class);
    assert children.isEmpty();
  }

  public Noop(FilePosition pos) { super(pos, NoChildren.class); }

  @Override
  public Object getValue() { return null; }

  public void render(RenderContext rc) {
    rc.getOut().consume(JsMinimalPrinter.NOOP);
  }

  public boolean hasHangingConditional() { return false; }

  @Override public boolean isTerminal() { return true; }
}
