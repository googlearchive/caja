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
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A skeleton for Statement implementations.
 *
 * @author mikesamuel@gmail.com
 */
public abstract class AbstractStatement
    extends AbstractParseTreeNode implements Statement {
  private static final long serialVersionUID = -3327463812140885737L;

  @ReflectiveCtor
  public AbstractStatement(
      FilePosition pos, Class<? extends ParseTreeNode> childClass) {
    super(pos, childClass);
  }

  /**
   * Accumulates the set of labels that may be broken out of by statements
   * under this node.  The empty String represents a labelless break.
   * @param breaksReaching a mutable map.  Modified in place.
   */
  public void breaks(Map<String, List<BreakStmt>> breaksReaching) {
    for (ParseTreeNode child : children()) {
      if (child instanceof Statement) {
        ((Statement) child).breaks(breaksReaching);
      }
    }
  }

  /**
   * Accumulates the set of labels that may be continued to by statements
   * under this node.  The empty String represents a labelless continue.
   * @param contsReaching a mutable map.  Modified in place.
   */
  public void continues(Map<String, List<ContinueStmt>> contsReaching) {
    for (ParseTreeNode child : children()) {
      if (child instanceof Statement) {
        ((Statement) child).continues(contsReaching);
      }
    }
  }

  /** Does the rendered form end with a close curly bracket? */
  public boolean isTerminal() {
    List<? extends ParseTreeNode> children = children();
    if (children.isEmpty()) { return false; }
    ParseTreeNode lastChild = children.get(children.size() - 1);
    return lastChild instanceof Statement
        && ((Statement) lastChild).isTerminal();
  }

  /**
   * Called to render the statement as part of another statement.
   *
   * @param rc non null.
   * @param terminate should the statement be terminated -- followed with a
   *   semicolon if not a block.
   */
  public void renderBlock(RenderContext rc, boolean terminate) {
    render(rc);
    // Incorrect, but no need to calculate isTerminal() unless terminate || post
    boolean terminal = (terminate) && isTerminal();
    if (terminate && !terminal) {
      rc.getOut().mark(FilePosition.endOfOrNull(getFilePosition()));
      rc.getOut().consume(";");
    }
  }

  public final TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> exHandler) {
    return new JsPrettyPrinter(new Concatenator(out, exHandler));
  }
}
