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

/**
 * A formatter that renders CSS with the minimal amount of whitespace that
 * does not change meaning.
 *
 * @author mikesamuel@gmail.com
 */
public final class CssMinimalPrinter extends AbstractRenderer {
  /** Number of characters written to out since the last line-break. */
  private int charInLine;

  private char lastToken = '\0';
  /** True if the last token needs a following space. */
  private char pendingSpace = '\0';

  /**
   * @param out receives the rendered text.  Typically a {@link Concatenator}.
   */
  public CssMinimalPrinter(TokenConsumer out) {
    super(out);
  }

  public void mark(FilePosition pos) { out.mark(pos); }

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
        if (pendingSpace != '\n' && lastToken != ':') { pendingSpace = ' '; }
        return;
      default: break;
    }

    char spaceBefore = pendingSpace;
    pendingSpace = '\0';

    if (text.length() == 1) {
      char ch0 = text.charAt(0);
      switch (ch0) {
        case '{': case '}': case ',': case ';': case ':':
          spaceBefore = '\0';
          break;
      }
      lastToken = ch0;
    } else {
      lastToken = '\0';
    }

    switch (spaceBefore) {
      case '\n':
        newLine();
        break;
      case ' ':
        space();
        break;
    }

    emit(text);
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
