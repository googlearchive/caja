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
 * The result of a code translation step, such as an HTML to JS compiler.
 * This is not a real javascript parse tree node since it will never be created
 * by the JS parser.
 *
 * @author mikesamuel@gmail.com
 */
public final class TranslatedCode extends AbstractStatement {
  /** @param value unused.  This ctor is provided for reflection. */
  public TranslatedCode(
      FilePosition pos, Void value, List<? extends Statement> children) {
    super(pos, Statement.class);
    appendChild(children.get(0));
    assert children.size() == 1;
  }

  public TranslatedCode(Statement body) {
    super(body.getFilePosition(), Statement.class);
    appendChild(body);
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    if (children().size() != 1) {
      throw new IllegalStateException("TranslatedCode may only have one child");
    }
    ParseTreeNode module = children().get(0);
    if (!(module instanceof Statement)) {
      throw new ClassCastException("Expected block, not " + module);
    }
  }

  @Override
  public Object getValue() { return null; }

  @Override
  public List<Statement> children() {
    return childrenAs(Statement.class);
  }

  public Statement getTranslation() { return children().get(0); }

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
