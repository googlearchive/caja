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

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.reporting.RenderContext;

import java.util.List;
import java.util.Map;

/**
 * A parse tree node that is not evaluated for its value.
 * Includes declarations, flow control, and nodes evaluated for side-effects.
 *
 * @author mikesamuel@gmail.com
 */
public interface Statement extends ParseTreeNode {
  // TODO(mikesamuel): breaksReaching should probably take a parameter of type
  // Map<? super String, ? super BreakStmt>, and equivalently for contsReaching.
  // TODO(mikesamuel): rename breaks and continues to collectBreakTargets and
  // collectContinueTargets respectively.

  /**
   * Accumulates the set of labels that may be broken out of by statements
   * under this node.  The empty String represents a label-less break.
   * @param breaksReaching a mutable map.  Modified in place.
   */
  void breaks(Map<String, List<BreakStmt>> breaksReaching);

  /**
   * Accumulates the set of labels that may be continued to by statements
   * under this node.  The empty String represents a label-less continue.
   * @param contsReaching a mutable map.  Modified in place.
   */
  void continues(Map<String, List<ContinueStmt>> contsReaching);

  /**
   * True iff the rendered form end with a close curly bracket that is not part
   * of an expression.
   */
  boolean isTerminal();

  /**
   * Called to render the statement as part of another statement.
   *
   * @param rc non null.
   * @param terminate should the statement be terminated -- followed with a
   *   semicolon if not a block.
   */
  void renderBlock(RenderContext rc, boolean terminate);

  /**
   * True if the rendered form of the statement would consume more tokens if
   * parsed followed by the tokens "else" and ";".
   */
  boolean hasHangingConditional();
}
