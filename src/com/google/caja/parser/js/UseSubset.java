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

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;

import java.io.IOException;
import java.util.List;

/**
 * Identifies a fail-stop subset of ECMAScript, e.g. "strict" mode.
 *
 * @author mikesamuel@gmail.com
 * @see UseSubsetDirective
 */
public final class UseSubset extends AbstractParseTreeNode {
  private final String subsetName;

  /** @param children unused.  This ctor is provided for reflection. */
  public UseSubset(
      FilePosition pos, String subsetName, List<NoChildren> children) {
    this(pos, subsetName);
  }

  public UseSubset(FilePosition pos, String subsetName) {
    super(pos);
    if (subsetName == null) { throw new NullPointerException(); }
    this.subsetName = subsetName;
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    if (!children().isEmpty()) { throw new IndexOutOfBoundsException(); }
  }

  @Override
  public String getValue() { return subsetName; }

  public String getSubsetName() { return subsetName; }

  public void render(RenderContext rc) {
    StringBuilder escaped = new StringBuilder();
    escaped.append('\'');  // Not allowed in JSON so always use single quotes.
    Escaping.escapeJsString(subsetName, true, true, escaped);
    escaped.append('\'');
    rc.getOut().consume(escaped.toString());
  }

  public final TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> exHandler) {
    return new JsPrettyPrinter(out, exHandler);
  }
}
