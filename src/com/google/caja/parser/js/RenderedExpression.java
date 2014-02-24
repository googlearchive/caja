// Copyright (C) 2011 Google Inc.
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
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.RenderContext;
import java.util.List;

/**
 * A RenderedExpression is a Javascript expression tree rendered as a string,
 * which is cheaper to clone and/or cache than an actual ParseTreeNode tree.
 * <p>
 * Used by {@link CajoledModule#flattenProperty}.
 */
public final class RenderedExpression extends AbstractExpression {
  private static final long serialVersionUID = 1L;

  private final String value;

  @ReflectiveCtor
  public RenderedExpression(FilePosition pos, String value, List<?> children) {
    this(pos, value);
    assert children.isEmpty();
  }

  public RenderedExpression(ParseTreeNode n, boolean minify) {
    this(render(n, minify));
  }

  public RenderedExpression(String value) {
    this(FilePosition.UNKNOWN, value);
  }

  public RenderedExpression(FilePosition pos, String value) {
    super(pos, RenderedExpression.class);
    this.value = value;
  }

  @Override
  public Object getValue() {
    return value;
  }

  @Override
  public String typeOf() {
    return null;
  }

  @Override
  public void render(RenderContext r) {
    r.getOut().consume(value);
  }

  private static String render(ParseTreeNode n, boolean minify) {
    StringBuilder buf = new StringBuilder();
    TokenConsumer tc;
    if (minify) {
      tc = new JsMinimalPrinter(new Concatenator(buf));
    } else {
      tc = new JsPrettyPrinter(new Concatenator(buf));
    }
    RenderContext rc = new RenderContext(tc);
    n.render(rc);
    tc.noMoreTokens();
    return buf.toString();
  }
}
