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

package com.google.caja.render;

/**
 * Encapsulates information necessary to decide how to indent a line-break at a
 * point in a stream of tokens.
 */
final class Indent {
  final int spaces;
  /**
   * Are we in a () or [] block.
   * Semicolons are not treated as statement separators in that context.
   */
  final boolean parenthetical;
  /**
   * True iff we are in a statement.
   * E.g. in {@code
   *    var foo = x
   *        + y;
   * }
   * the <code>+</code> is indented past the beginning of the previous line
   * because we have not yet seen a semicolon.
   */
  final boolean inStatement;

  Indent(int spaces, boolean parenthetical) {
    this(spaces, parenthetical, false);
  }

  Indent(int spaces, boolean parenthetical, boolean inStatement) {
    this.spaces = spaces;
    this.parenthetical = parenthetical;
    this.inStatement = inStatement;
  }

  int getIndentLevel() {
    return this.spaces + (this.inStatement ? 2 : 0);
  }

  Indent withInStatement(boolean inStatement) {
    inStatement = inStatement && !parenthetical;
    if (inStatement == this.inStatement) { return this; }
    return new Indent(this.spaces, this.parenthetical, inStatement);
  }

  @Override
  public String toString() {
    return "[Indent sp=" + spaces + ", paren=" + parenthetical + ", ins="
        + inStatement + "]";
  }
}
