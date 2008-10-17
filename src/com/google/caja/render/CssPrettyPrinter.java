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
import java.util.ArrayList;
import java.util.List;

/**
 * A formatter that indents code for CSS paying careful attention to where
 * adding whitespace between tokens would change tokenization..
 *
 * @author mikesamuel@gmail.com
 */
public final class CssPrettyPrinter extends AbstractRenderer {
  /**
   * Stack of indentation positions.
   * Curly brackets indent to two past the last stack position and
   * parenthetical blocks indent to the open parenthesis.
   *
   * A non-negative number indicates a curly bracket indentation and a negative
   * number a parenthetical indentation.
   */
  private List<Integer> indentStack = new ArrayList<Integer>();
  /** Number of characters written to out since the last linebreak. */
  private int charInLine;

  /** True if the last token needs a following space. */
  private char pendingSpace = '\0';

  /**
   * @param out receives the rendered text.
   * @param ioExceptionHandler receives exceptions thrown by out.
   */
  public CssPrettyPrinter(
      Appendable out, Callback<IOException> ioExceptionHandler) {
    super(out, ioExceptionHandler);
  }

  public void mark(FilePosition pos) {}

  @Override
  protected void append(String text) throws IOException {
    TokenClassification tClass = TokenClassification.classify(text);
    if (tClass == null) { return; }
    switch (tClass) {
      case LINEBREAK:
        // Allow external code to force linebreaks.
        // This allows us to create a composite-renderer that renders
        // original source code next to translated source code.
        newLine();
        return;
      case SPACE:
        if (pendingSpace != '\n') { pendingSpace = ' '; }
        return;
      case COMMENT:
        emit(text);
        return;
      default: break;
    }

    char spaceBefore = pendingSpace;
    pendingSpace = '\0';
    char spaceAfter = '\0';

    Integer nextIndent = null;
    if (text.length() == 1) {
      char ch0 = text.charAt(0);
      switch (ch0) {
        case '{':
          if (spaceBefore != '\n') {
            spaceBefore = ' ';
          }
          nextIndent = getIndentation() + 2;
          spaceAfter = '\n';
          break;
        case '}':
          spaceAfter = spaceBefore = '\n';
          popIndentStack();
          break;
        case ',':
          spaceBefore = '\0';
          spaceAfter = ' ';
          break;
        case ';':
          spaceBefore = '\0';
          spaceAfter = '\n';
          break;
      }
    }

    switch (spaceBefore) {
      case '\n':
        newLine();
        break;
      case ' ':
        space();
        break;
    }

    indent();
    emit(text);
    if (nextIndent != null) {
      pushIndent(nextIndent);
    }

    pendingSpace = spaceAfter;
  }

  private int getIndentation() {
    return indentStack.isEmpty() ? 0 : indentStack.get(indentStack.size() - 1);
  }

  private void pushIndent(int indent) {
    indentStack.add(indent);
  }

  private void popIndentStack() {
    if (!indentStack.isEmpty()) {
      indentStack.remove(indentStack.size() - 1);
    }
  }

  private void indent() throws IOException {
    if (charInLine != 0) { return; }
    int indent = getIndentation();

    charInLine += indent;
    String spaces = "                ";
    while (indent >= spaces.length()) {
      out.append(spaces);
      indent -= spaces.length();
    }
    out.append(spaces, 0, indent);
  }

  private void newLine() throws IOException {
    if (charInLine == 0) { return; }
    charInLine = 0;
    out.append("\n");
  }

  private void space() throws IOException {
    if (charInLine != 0) {
      out.append(" ");
      ++charInLine;
    }
  }

  private void emit(CharSequence s) throws IOException {
    out.append(s);
    int n = s.length();
    for (int i = n; --i >= 0;) {
      char ch = s.charAt(i);
      if (ch == '\r' || ch == '\n') {
        charInLine = n - i;
        break;
      }
    }
    charInLine += n;
  }
}
