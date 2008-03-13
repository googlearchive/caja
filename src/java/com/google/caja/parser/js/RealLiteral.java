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

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.reporting.RenderContext;

import java.io.IOException;
import java.util.List;

/**
 * A number literal backed by a double.
 *
 * @author mikesamuel@gmail.com
 */
public final class RealLiteral extends NumberLiteral {
  public final double value;

  public RealLiteral(Number value, List<? extends ParseTreeNode> children) {
    this(value.doubleValue());
  }
  
  public RealLiteral(double value) { this.value = value; }

  @Override
  public Number getValue() { return Double.valueOf(value); }

  @Override
  public double doubleValue() { return value; }

  @Override
  public void render(RenderContext rc) throws IOException {
    // Render special values in a way that is independent of the current
    // environment.
    if (Double.isNaN(value)) {
      rc.out.append("(0 / 0)");
    } else if (Double.isInfinite(value)) {
      rc.out.append(value >= 0 ? "(1 / 0)" : "(-1 / 0)");
    } else {
      super.render(rc);
    }
  }
}
