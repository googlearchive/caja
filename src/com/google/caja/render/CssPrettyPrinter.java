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
import com.google.caja.lexer.TokenConsumer;
import com.google.common.collect.Lists;

import java.util.List;

import javax.annotation.Nullable;

/**
 * A formatter that indents code for CSS paying careful attention to where
 * adding whitespace between tokens would change tokenization.
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
  private final List<Integer> indentStack = Lists.newArrayList();
  /** Number of characters written to out since the last line-break. */
  private int charInLine;

  /** True if the last token needs a following space. */
  private char pendingSpace = '\0';

  /**
   * @param out receives the rendered text.  Typically a {@link Concatenator}.
   */
  public CssPrettyPrinter(TokenConsumer out) {
    super(out);
  }

  public CssPrettyPrinter(StringBuilder out) {
    this(new Concatenator(out));
  }

  public void mark(@Nullable FilePosition pos) { out.mark(pos); }

  @Override
  public void consume(String text) {
    TokenClassification tClass = TokenClassification.classify(text);
    if (tClass == null) { return; }
    switch (tClass) {
      case LINEBREAK:
        // Allow external code to force line-breaks.
        // This allows us to create a composite-renderer that renders
        // original source code next to translated source code.
        if (pendingSpace == '\n') { newLine(); }
        pendingSpace = '\n';
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
          // If we're rendering a declaration group, e.g. inside an HTML style
          // attribute, separate them with spaces, but if we're pretty printing
          // a stylesheet, put newlines between declarations.
          spaceAfter = indentStack.isEmpty() ? ' ' : '\n';
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

  private void indent() {
    if (charInLine != 0) { return; }
    int indent = getIndentation();

    charInLine += indent;
    String spaces = "                ";
    while (indent >= spaces.length()) {
      out.consume(spaces);
      indent -= spaces.length();
    }
    if (indent != 0) {
      out.consume(spaces.substring(0, indent));
    }
  }

  private void newLine() {
    if (charInLine == 0) { return; }
    charInLine = 0;
    out.consume("\n");
  }

  private void space() {
    if (charInLine != 0) {
      out.consume(" ");
      ++charInLine;
    }
  }

  private void emit(String s) {
    out.consume(s);
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
