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

/**
 * Base class for case and default blocks.
 * @see SwitchStmt
 */
public abstract class SwitchCase extends AbstractStatement {
  private static final long serialVersionUID = 3781500259502644405L;

  protected SwitchCase(FilePosition pos) { super(pos, ParseTreeNode.class); }

  public boolean hasHangingConditional() { return false; }

  // TODO: instead of using a block, introduce a statement collection as a super
  // class of Block since Block is going to have specific semantics as a scoping
  // construct in ES6.
  public abstract Block getBody();

  protected abstract void renderHead(RenderContext rc);

  public final void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());
    renderHead(rc);
    out.consume(":");
    out.consume("\n");
    Block body = getBody();
    rc.getOut().mark(body.getFilePosition());
    body.renderBody(rc);
  }
}
