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

import com.google.caja.reporting.RenderContext;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author mikesamuel@gmail.com
 */
public final class BreakStmt extends AbstractStatement {
  private static final long serialVersionUID = -8486796747707954777L;
  private final String label;

  /** @param children unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public BreakStmt(
      FilePosition pos, String value, List<? extends ParseTreeNode> children) {
    this(pos, value);
  }

  public BreakStmt(FilePosition pos, String label) {
    super(pos, Statement.class);
    this.label = label;
  }

  @Override
  public void breaks(Map<String, List<BreakStmt>> breaksReaching) {
    List<BreakStmt> breaks = breaksReaching.get(this.label);
    if (null == breaks) {
      breaksReaching.put(label, breaks = new ArrayList<BreakStmt>());
    }
    breaks.add(this);
  }

  @Override
  public String getValue() { return label; }

  public String getLabel() { return label; }

  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());
    out.consume("break");
    if (null != label && !"".equals(label)) {
      out.consume(label);
    }
  }

  public boolean hasHangingConditional() { return false; }
}
