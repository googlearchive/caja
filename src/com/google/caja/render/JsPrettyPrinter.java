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
import com.google.caja.util.Callback;

import java.io.IOException;
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

  /**
   * @param out receives the rendered text.
   * @param ioExceptionHandler receives exceptions thrown by out.
   */
  public JsPrettyPrinter(
      Appendable out, Callback<IOException> ioExceptionHandler) {
    super(out, ioExceptionHandler);
  }

  public void setLineLengthLimit(int lineLengthLimit) {
    this.lineLengthLimit = lineLengthLimit;
  }

  public int getLineLengthLimit() { return this.lineLengthLimit; }

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

    Indenter indenter = new Indenter(outputTokens);
    indenter.breakLines();
    indenter.indent(lineLengthLimit);
    return outputTokens;
  }
}
