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

import com.google.caja.lexer.FilePosition;

import java.util.List;

/**
 * A formatter that indents code for a C-style language with statement
 * delimited by curly brackets, and expression blocks delimited by
 * square brackets and parentheses.
 *
 * @author mikesamuel@gmail.com
 */
public final class JsPrettyPrinter extends BufferingRenderer {
  private int lineLengthLimit = 80;
  private boolean breakAfterComment = true;

  /**
   * @param out receives the rendered text.
   */
  public JsPrettyPrinter(Concatenator out) {
    super(out);
  }

  public JsPrettyPrinter(StringBuilder out) {
    this(new Concatenator(out));
  }

  public void setLineLengthLimit(int lineLengthLimit) {
    this.lineLengthLimit = lineLengthLimit;
  }

  public int getLineLengthLimit() { return this.lineLengthLimit; }

  public void setBreakAfterComment(boolean breakAfterComment) {
    this.breakAfterComment = breakAfterComment;
  }

  public boolean isBreakAfterComment() { return this.breakAfterComment; }

  @Override
  List<String> splitTokens(List<Object> tokens) {
    Spacer spacer = new Spacer();
    for (Object lineEl : tokens) {
      if (lineEl instanceof FilePosition) {
        spacer.processMark((FilePosition) lineEl);
      } else {
        spacer.processToken((String) lineEl);
      }
    }
    List<String> outputTokens = spacer.getOutputTokens();

    Indenter indenter = new Indenter(outputTokens, breakAfterComment);
    indenter.breakLines();
    indenter.indent(lineLengthLimit);
    return outputTokens;
  }
}
