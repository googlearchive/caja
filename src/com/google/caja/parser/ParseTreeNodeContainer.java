// Copyright (C) 2007 Google Inc.
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

package com.google.caja.parser;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * A simple implementation of {@code ParseTreeNode} which acts as a container
 * for an arbitrary list of {@code ParseTreeNode}s.
 *
 * <p>This is used as a convenience wrapper allowing a quasiliteral pattern to
 * return a list of {@code ParseTreeNode}s in a type-safe manner.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class ParseTreeNodeContainer extends AbstractParseTreeNode {
  private static final long serialVersionUID = -1979856467228608958L;

  /** @param value unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public ParseTreeNodeContainer(
      FilePosition pos, Void value, List<? extends ParseTreeNode> children) {
    super(pos);
    ctorAppendChildren(children);
  }

  public ParseTreeNodeContainer(List<? extends ParseTreeNode> children) {
    super(FilePosition.UNKNOWN);
    ctorAppendChildren(children);
  }

  public ParseTreeNodeContainer() {
    this(Collections.<ParseTreeNode>emptyList());
  }

  @Override
  public Object getValue() { return null; }

  public void render(RenderContext rc) {
    throw new UnsupportedOperationException();
  }

  public TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> exHandler) {
    throw new UnsupportedOperationException();
  }
}
