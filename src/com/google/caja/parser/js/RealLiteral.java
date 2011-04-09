// Copyright (C) 2006 Google Inc.
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

import java.util.List;

/**
 * A number literal backed by a double.
 *
 * @author mikesamuel@gmail.com
 */
public final class RealLiteral extends NumberLiteral {
  private static final long serialVersionUID = -2331544091012208500L;
  private final double value;

  /** @param children unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public RealLiteral(
      FilePosition pos, Number value, List<? extends ParseTreeNode> children) {
    this(pos, value.doubleValue());
  }

  public RealLiteral(FilePosition pos, double value) {
    super(pos);
    this.value = value;
  }

  @Override
  public Number getValue() { return Double.valueOf(value); }

  @Override
  public double doubleValue() { return value; }

  @Override
  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    // Render special values in a way that is independent of the current
    // environment.
    if (Double.isNaN(value)) {
      out.consume("(");
      out.consume("0");
      out.consume("/");
      out.consume("0");
      out.consume(")");
    } else if (Double.isInfinite(value)) {
      out.consume("(");
      if (value < 0) { out.consume("-"); }
      out.consume("1");
      out.consume("/");
      out.consume("0");
      out.consume(")");
    } else if (value == 0 && (1d/value) < 0) {
      out.consume("(");
      out.consume("-");
      out.consume("0");
      out.consume(")");
    } else {
      super.render(rc);
    }
  }
}
