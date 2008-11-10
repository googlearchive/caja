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

/**
 * A placeholder that can be used in the child type parameter to
 * AbstractParseTreeNode to indicate that the node is always a leaf in the parse
 * tree.
 *
 * @author mikesamuel@gmail.com
 */
final class NoChildren extends AbstractExpression<NoChildren> {
  private NoChildren() { /* Not instantiable. */ }
  @Override
  public Object getValue() { throw new AssertionError(); }
  public void render(RenderContext rc) { throw new AssertionError(); }
}
