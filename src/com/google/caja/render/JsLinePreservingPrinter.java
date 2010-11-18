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

import javax.annotation.Nullable;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;

/**
 * Renders tokens with spaces or line breaks in between in such a way that
 * line numbers are preserved.
 *
 * @author mikesamuel@gmail.com
 */
public class JsLinePreservingPrinter extends AbstractRenderer {
  private final InputSource is;
  private int lineNumber = 1;
  private String pendingSpace;

  public JsLinePreservingPrinter(InputSource is, Concatenator out) {
    super(out);
    if (is == null) { throw new NullPointerException(); }
    this.is = is;
  }

  @Override
  public void consume(String text) {
    if ("".equals(text.trim())) { return; }
    if (pendingSpace != null) { out.consume(pendingSpace); }
    out.consume(text);
    pendingSpace = " ";
  }

  public void mark(@Nullable FilePosition pos) {
    if (pos != null && is.equals(pos.source())) {
      int line = pos.startLineNo();
      StringBuilder sb = new StringBuilder();
      while (lineNumber < line) {
        sb.append('\n');
        ++lineNumber;
      }
      if (sb.length() != 0) {
        pendingSpace = sb.toString();
      }
    }
  }
}
