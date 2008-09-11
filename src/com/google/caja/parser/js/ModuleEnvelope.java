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

import java.util.List;

/**
 * Translates to a module envelope: <tt>___.loadModule...</tt>
 *
 * @author erights@gmail.com
 * @author ihab.awad@gmail.com
 */
public final class ModuleEnvelope extends AbstractStatement<Block> {
  /** @param value unused.  This ctor is provided for reflection. */
  public ModuleEnvelope(Void value, List<? extends Block> children) {
    this(children.get(0));
  }

  public ModuleEnvelope(Block body) {
    createMutation().appendChild(body).execute();
  }

  public ModuleEnvelope() {}

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    if (children().size() != 1) {
      throw new IllegalStateException(
          "A ModuleEnvelope may only have one child");
    }
    if (!(children().get(0) instanceof Block)) {
      throw new ClassCastException(
          "Expected block, not " + children().get(0));
    }
  }

  @Override
  public Object getValue() { return null; }

  public void render(RenderContext rc) {
    rc.getOut().consume("<<<<");
    children().get(0).render(rc);
    rc.getOut().consume(">>>>");
  }

  @Override
  public boolean isTerminal() {
    return false;
  }
}
