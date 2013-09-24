// Copyright (C) 2013 Google Inc.
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
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.List;

/**
 * @author ihab.awad@gmail.com
 */
public final class PlainModule extends AbstractParseTreeNode {
  /** @param value unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public PlainModule(
      FilePosition pos, Void value, List<? extends Block> children) {
    // Call to 'this' must be first in the ctor. But we check for sanity later.
    this(pos, children.get(0));
    if (children.size() != 1) {
      throw new RuntimeException(
          "PlainModule must be constructed with one Block");
    }
  }

  public PlainModule(Block block) {
    this(FilePosition.UNKNOWN, block);
  }

  public PlainModule(FilePosition pos, Block block) {
    super(pos, Block.class);
    ctorAppendChildren(Lists.newArrayList(block));
    makeImmutable();
  }

  @Override
  public Object getValue() {
    return null;
  }

  @Override
  public List<? extends Block> children() {
    return childrenAs(Block.class);
  }

  @Override
  public final TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> exHandler) {
    return new JsPrettyPrinter(new Concatenator(out, exHandler));
  }

  @Override
  public void render(RenderContext rc) {
    // Render the Statements in the contained Block without rendering
    // the surrounding curly braces
    for (ParseTreeNode n : children().get(0).children()) {
      ((Statement) n).renderBlock(rc, true);
    }
  }
}
