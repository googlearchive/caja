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
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.util.Join;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Renderers rewritten source code interleaved with the original.  E.g.
 * {@code
 *   // Rewritten by cajoler.
 *   muckWith(                    IMPORTS___.muckWith(
 *       document.forms[0])           IMPORTS___.document.forms[0]);
 * }
 *
 * @author mikesamuel@gmail.com
 */
public abstract class SideBySideRenderer implements TokenConsumer {
  private final Map<InputSource, String[]> originalSourceLines;
  private final Map<InputSource, Integer> maxLineSeen
      = new HashMap<InputSource, Integer>();
  private final TokenConsumer renderer;
  private FilePosition lastPos;
  private FilePosition mark;
  private FilePosition bufStart;
  private StringBuilder renderedBuf;

  public SideBySideRenderer(
      Map<InputSource, ? extends CharSequence> originalSource) {
    this.originalSourceLines = new HashMap<InputSource, String[]>();
    for (Map.Entry<InputSource, ? extends CharSequence> e
         : originalSource.entrySet()) {
      this.originalSourceLines.put(
          e.getKey(), e.getValue().toString().split("\r\n?|\n"));
    }
    this.renderedBuf = new StringBuilder();
    this.renderer = makeRenderer(this.renderedBuf);
  }

  /**
   * Called when rendered tokens have been processed for a line of original
   * source.
   *
   * @param startOfLine a file position into the original source code.
   * @param original zero or more lines of original source code.
   * @param rendered one or more lines of rendered source code.
   */
  protected abstract void emitLine(
      FilePosition startOfLine, String original, String rendered);

  /**
   * Called when we render a token from a different source than previously.
   * This method does nothing, but may be overridden.
   * @param previous the token from which the last rendered token came.
   * @param next the token from which the next rendered token will come,
   *   unless switchSource is called again before {@link #consume}.
   */
  protected void switchSource(FilePosition previous, FilePosition next) {}

  protected abstract TokenConsumer makeRenderer(StringBuilder renderedSrc);

  public void mark(FilePosition pos) {
    if (pos != null) { this.mark = pos; }
    renderer.mark(pos);
  }

  public void consume(String text) {
    if (!(mark != null
          ? lastPos != null && mark.source().equals(lastPos.source())
          : lastPos == null)) {
      emitLine();
      switchSource(lastPos, mark);
    } else if (lastPos != null) {
      if (mark.startLineNo() > lastPos.startLineNo()
          && mark.startLineNo() >= lastLineNo(mark.source())) {
        emitLine();
      }
    }

    renderer.consume(text);
    lastPos = mark;
  }

  public void noMoreTokens() {
    renderer.noMoreTokens();
    emitLine();
  }

  private void emitLine() {
    String renderedSrc = renderedBuf.toString();
    renderedBuf.setLength(0);
    if (renderedSrc.length() > 0 && renderedSrc.charAt(0) == '\n') {
      renderedSrc = renderedSrc.substring(1);
    }
    renderer.consume("\n");

    if ("".equals(renderedSrc) && bufStart == null) { return; }

    String originalSrc = "";
    if (bufStart != null) {
      int startLine = lastLineNo(bufStart.source()) + 1;
      int endLine = lastPos.endLineNo();
      if (lastPos.endCharInLine() == 1) { --endLine; }
      originalSrc = originalSourceSnippet(
          bufStart.source(), startLine, endLine);
      maxLineSeen.put(bufStart.source(), endLine);
    }

    emitLine(bufStart, originalSrc, renderedSrc);

    bufStart = mark;
  }

  private String originalSourceSnippet(
      InputSource src, int startLine, int endLine) {
    // FilePosition lines are 1-indexed, but arrays are zero-indexed.
    startLine -= 1;
    endLine -= 1;

    String[] lines = originalSourceLines.get(src);
    if (lines == null || startLine >= lines.length) { return ""; }
    endLine = Math.min(endLine, lines.length - 1);

    return Join.join(
        "\n", Arrays.asList(lines).subList(startLine, endLine + 1));
  }

  private int lastLineNo(InputSource src) {
    Integer ln = maxLineSeen.get(src);
    return ln != null ? ln : 0;
  }
}
