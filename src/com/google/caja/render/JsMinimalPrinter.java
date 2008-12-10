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
 * A compact JavaScript renderer.
 *
 * @author mikesamuel@gmail.com
 */
public final class JsMinimalPrinter extends BufferingRenderer {
  /** Number of characters written to out since the last linebreak. */
  private int charInLine;
  /**
   * Line length below which the printer will not wrap lines.
   * At or above this limit, the printer will try to replace a space with
   * a line-break.
   */
  private int lineLengthLimit = 80;
  /** The last non-space/comment token. */
  private String lastToken;
  private JsTokenAdjacencyChecker adjChecker = new JsTokenAdjacencyChecker();

  /**
   * @param out receives the rendered text.
   * @param ioExceptionHandler receives exceptions thrown by out.
   */
  public JsMinimalPrinter(
      Appendable out, Callback<IOException> ioExceptionHandler) {
    super(out, ioExceptionHandler);
  }

  /** Visible for testing.  Should not be used by clients. */
  void setLineLengthLimit(int lineLengthLimit) {
    this.lineLengthLimit = lineLengthLimit;
  }

  @Override
  List<String> splitTokens(List<Object> tokens) {
    List<String> outputTokens = new ArrayList<String>();

    for (Object tokenEl : tokens) {
      if (tokenEl instanceof FilePosition) { continue; }
      String text = (String) tokenEl;
      TokenClassification tClass = TokenClassification.classify(text);
      if (tClass == null) { continue; }
      switch (tClass) {
        case LINEBREAK:
        case SPACE:
        case COMMENT:
          continue;
        default: break;
      }

      // Write any whitespace before the token.
      if (adjChecker.needSpaceBefore(text)) {
        // Some security tools/proxies/firewalls break on really long javascript
        // lines.
        if (charInLine >= lineLengthLimit
            && JsRenderUtil.canBreakBetween(lastToken, text)) {
          charInLine = 0;
          outputTokens.add("\n");
        } else if (charInLine != 0) {
          ++charInLine;
          outputTokens.add(" ");
        }
      }

      // Actually write the token.
      charInLine += text.length();
      outputTokens.add(text);

      lastToken = text;
    }
    return outputTokens;
  }
}
