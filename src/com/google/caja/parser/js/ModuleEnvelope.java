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
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.reporting.RenderContext;

import java.util.List;

/**
 * Translates to a module envelope: <tt>___.loadModule...</tt>.
 * This is not a core JavaScript parse tree node &mdash; it is never produced by
 * {@link Parser}.
 *
 * @author erights@gmail.com
 * @author ihab.awad@gmail.com
 */
public final class ModuleEnvelope extends AbstractStatement {
  /** @param value unused.  This ctor is provided for reflection. */
  public ModuleEnvelope(FilePosition pos, Void value, List<? extends Block> children) {
    super(pos, Block.class);
    assert children.size() == 1;
    createMutation().appendChild(children.get(0)).execute();
  }

  public ModuleEnvelope(Block body) {
    super(body.getFilePosition(), Block.class);
    createMutation().appendChild(body).execute();
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    if (children().size() != 1) {
      throw new IllegalStateException(
          "A ModuleEnvelope may only have one child");
    }
    ParseTreeNode module = children().get(0);
    if (!(module instanceof Block)) {
      throw new ClassCastException("Expected block, not " + module);
    }
  }

  @Override
  public Object getValue() { return null; }

  @Override
  public List<Block> children() {
    return childrenAs(Block.class);
  }

  public Block getModuleBody() { return children().get(0); }

  public void render(RenderContext rc) {
    // FIXME(erights): must emit valid javascript or throw an exception
    rc.getOut().consume("<<<<");
    children().get(0).render(rc);
    rc.getOut().consume(">>>>");
  }

  @Override
  public boolean isTerminal() {
    return true;
  }
}
