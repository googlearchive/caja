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
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.util.Lists;

import java.util.List;

import javax.annotation.Nullable;

/**
 * An abstract renderer for JavaScript tokens that ensures that implementations
 * don't fall afoul of JavaScript's syntactic quirks.
 *
 * @author mikesamuel@gmail.com
 */
abstract class BufferingRenderer implements TokenConsumer {
  private final List<Object> pending = Lists.newArrayList();
  private final Concatenator out;

  /**
   * @param out receives the rendered text.
   */
  BufferingRenderer(Concatenator out) {
    this.out = out;
  }

  /**
   * @throws NullPointerException if out raises an IOException
   *     and ioExceptionHandler is null.
   */
  public final void noMoreTokens() {
    JsTokenAdjacencyChecker adjChecker = new JsTokenAdjacencyChecker();

    String lastToken = null;
    boolean noOutputWritten = true;
    List<String> outputTokens = splitTokens(pending);
    pending.clear();
    String pendingSpace = null;
    for (int i = 0, nTokens = outputTokens.size(); i < nTokens; ++i) {
      String token = outputTokens.get(i);
      if (token.charAt(0) == '\n' || " ".equals(token)) {
        pendingSpace = token;
        continue;
      }
      if (TokenClassification.isComment(token)) {
        // Make sure we don't get into a situation where we have to output
        // a newline to end a line comment, but can't output a newline because
        // it would break a restricted production.
        // When we see a line comment, scan forward until the next non-comment
        // token.  If the canBreakBetween check fails, then remove any
        // line-breaks by rewriting the comment.
        // We have to rewrite multi-line block comments, since ES3 and ES5 say
        // that a multi-line comment is replaced with a newline for the
        // purposes of semicolon insertion.
        //
        // This is inconsistently implemented, but the rewriting works
        // regardless of whether an implementation actually treats the
        // comment as a newline for semicolon insertion.
        String nextToken = null;
        for (int j = i + 1; j < nTokens; ++j) {
          switch (TokenClassification.classify(outputTokens.get(j))) {
            case SPACE: case LINEBREAK: case COMMENT: continue;
            default: break;
          }
          nextToken = outputTokens.get(j);
          break;
        }
        if (!JsRenderUtil.canBreakBetween(lastToken, nextToken)) {
          token = removeLinebreaksFromComment(token);
          if (pendingSpace != null) { pendingSpace = " "; }
        }
      }
      boolean needSpaceBefore = adjChecker.needSpaceBefore(token);
      if (pendingSpace == null && needSpaceBefore) {
        pendingSpace = " ";
      }
      if (pendingSpace != null) {
        if (pendingSpace.charAt(0) == '\n') {
          if (!JsRenderUtil.canBreakBetween(lastToken, token)) {
            pendingSpace = " ";
          } else if (noOutputWritten) {
            pendingSpace = pendingSpace.substring(1);
          }
        }
        out.append(pendingSpace);
        pendingSpace = null;
      }
      out.append(token);
      noOutputWritten = false;
      if (!TokenClassification.isComment(token)) {
        lastToken = token;
      }
    }
    out.noMoreTokens();
  }

  /**
   * May receive line-break or comment tokens.  Implementations may ignore
   * comment tokens, but the client is responsible for making sure that comments
   * are well-formed, do not contain code (e.g. conditional compilation code),
   * and do not violate any containment requirements, such as not containing the
   * string {@code </script>}.
   */
  public final void consume(String text) {
    if ("".equals(text)) { return; }
    pending.add(text);
  }

  public final void mark(@Nullable FilePosition mark) {
    if (mark != null && !InputSource.UNKNOWN.equals(mark.source())) {
      pending.add(mark);
    }
  }

  private static String removeLinebreaksFromComment(String token) {
    if (TokenClassification.isLineComment(token)) {
      token = "/*" + token.substring(2) + "*/";
    }
    StringBuilder sb = new StringBuilder(token);
    // Section 5.1.2 hinges on whether a MultiLineComment contains a
    // line-terminator char, so make sure it does not.
    for (int i = sb.length(); --i >= 0;) {
      if (JsLexer.isJsLineSeparator(sb.charAt(i))) {
        sb.setCharAt(i, ' ');
      }
    }
    // Make sure that turning a line comment into a MultiLineComment didn't
    // cause a */ in the line comment to become lexically significant.
    for (int e = sb.length() - 3, i; (i = sb.lastIndexOf("*/", e)) >= 0;) {
      sb.setCharAt(i + 1, ' ');
    }
    return sb.toString();
  }

  /**
   * Generates a list of output tokens consisting of non-whitespace tokens,
   * space tokens ({@code " "}) and newline tokens ({@code '\n'} followed by
   * any number of spaces).
   * @param tokens a heterogeneous array containing {@code String} tokens and
   *   {@code FilePosition} marks.
   * @return the strings in tokens in order with newline and space tokens
   *   inserted as appropriate.
   */
  abstract List<String> splitTokens(List<Object> tokens);
}
