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

import com.google.caja.lexer.TokenConsumer;
import com.google.caja.util.Callback;
import java.io.Flushable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

abstract class BufferingRenderer implements TokenConsumer {
  private final List<String> pending = new ArrayList<String>();
  protected final Appendable out;
  private final Callback<IOException> ioExceptionHandler;
  /** True if an IOException has been raised. */
  private boolean closed;

  /**
   * @param out receives the rendered text.
   * @param ioExceptionHandler receives exceptions thrown by out.
   */
  BufferingRenderer(Appendable out, Callback<IOException> ioExceptionHandler) {
    this.out = out;
    this.ioExceptionHandler = ioExceptionHandler;
  }

  /**
   * @throws NullPointerException if out raises an IOException
   *     and ioExceptionHandler is null.
   */
  public final void noMoreTokens() {
    JsTokenAdjacencyChecker adjChecker = new JsTokenAdjacencyChecker();
    try {
      String lastToken = null;
      LineData lineData = splitLines(pending);
      int[] numTokensPerLine = lineData.numTokensPerLine;
      int[] indentation = lineData.indentation;
      int nLines = numTokensPerLine.length;
      int tokIdx = 0;
      for (int lineNum = 0; lineNum < nLines; ++lineNum) {
        int numTokens = numTokensPerLine[lineNum];
        if (lineNum != 0) {
          String firstOfLine = numTokens == 0 ? null : pending.get(tokIdx);
          if (JsRenderUtil.canBreakBetween(lastToken, firstOfLine)) {
            out.append("\n");
            lastToken = null;
          } else {
            out.append(" ");
          }
        }
        if (numTokens != 0) {
          indent(indentation[lineNum], out);
          for (int endIdx = tokIdx + numTokens; tokIdx < endIdx; ++tokIdx) {
            String token = pending.get(tokIdx);
            // This needs to be invoked for all tokens since adjChecker is
            // stateful.
            boolean needSpaceBefore = adjChecker.needSpaceBefore(token);
            if (lastToken != null
                && (needSpaceBefore || wantSpaceBetween(lastToken, token))) {
              out.append(" ");
            }
            out.append(token);
            lastToken = token;
          }
        }
      }
      if (out instanceof Flushable) {
        ((Flushable) out).flush();
      }
    } catch (IOException ex) {
      if (!closed) {
        closed = true;
        ioExceptionHandler.handle(ex);
      }
    }
  }

  public final void consume(String text) {
    if ("".equals(text)) { return; }
    pending.add(text);
  }

  private static void indent(int nSpaces, Appendable out) throws IOException {
    while (nSpaces > 16) {
      out.append("                ");
      nSpaces -= 16;
    }
    if (nSpaces != 0) { out.append("                ".substring(0, nSpaces)); }
  }

  class LineData {
    int[] numTokensPerLine;
    int[] indentation;
  }

  abstract LineData splitLines(List<String> tokens);
  abstract boolean wantSpaceBetween(String before, String after);
}
