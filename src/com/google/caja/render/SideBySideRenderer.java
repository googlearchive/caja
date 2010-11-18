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
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;
import com.google.caja.util.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Renders rewritten source code interleaved with the original.  E.g.
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
  private final Map<InputSource, Integer> maxLineSeen = Maps.newHashMap();
  private final TokenConsumer renderer;
  private FilePosition lastPos;
  private FilePosition mark;
  private FilePosition chunkStart;
  /** Chunks of original source. */
  private final List<Chunk> chunks = Lists.newArrayList();
  private StringBuilder renderedBuf;

  public SideBySideRenderer(
      Map<InputSource, ? extends CharSequence> originalSource) {
    this.originalSourceLines = Maps.newHashMap();
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
   * @param previous the source from which the last rendered token came.
   * @param next the source from which the next rendered token will come,
   *   unless switchSource is called again before {@link #consume}.
   */
  protected void switchSource(InputSource previous, InputSource next) {
    // noop
  }

  protected abstract TokenConsumer makeRenderer(StringBuilder renderedSrc);

  public void mark(@Nullable FilePosition pos) {
    if (pos != null) { this.mark = pos; }
    renderer.mark(pos);
  }

  public void consume(String text) {
    if (TokenClassification.isComment(text)) { return; }
    if (!(mark != null
          ? lastPos != null && mark.source().equals(lastPos.source())
          : lastPos == null)) {
      emitLine();
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
    emitLine();
    renderer.noMoreTokens();

    String renderedSrc = renderedBuf.toString();
    renderedBuf.setLength(0);

    InputSource lastSource = null;
    for (Pair<String, Integer> chunk : splitChunks(renderedSrc)) {
      String renderedChunk = chunk.a;
      int chunkIndex = chunk.b;
      Chunk originalChunk = (
          chunkIndex >= 0 ? chunks.get(chunkIndex) : new Chunk("", null));
      InputSource source = (
          originalChunk.start != null ? originalChunk.start.source() : null);

      if (!"".equals(renderedChunk) || !"".equals(originalChunk.src)) {
        if (!(source != null
            ? lastSource != null && source.equals(lastSource)
            : lastSource == null)) {
          switchSource(lastSource, source);
        }

        emitLine(originalChunk.start, originalChunk.src, renderedChunk);
      }
      lastSource = source;
    }
  }

  private void emitLine() {
    String originalSrc = "";
    if (chunkStart != null) {
      int startLine = lastLineNo(chunkStart.source()) + 1;
      int endLine = lastPos.endLineNo();
      if (lastPos.endCharInLine() == 1) { --endLine; }
      originalSrc = originalSourceSnippet(
          chunkStart.source(), startLine, endLine);
      maxLineSeen.put(chunkStart.source(), endLine);
    }

    int chunkId = chunks.size();
    chunks.add(new Chunk(originalSrc, chunkStart));
    renderer.consume("/*@" + chunkId + "*/");
    renderer.consume("\n");

    chunkStart = mark;
  }

  private static List<Pair<String, Integer>> splitChunks(String renderedSrc) {
    Pattern p = Pattern.compile(" */\\*@([0-9]+)\\*/(?:\n|\r\n?|$)");
    Matcher m = p.matcher(renderedSrc);
    int start = 0;
    List<Pair<String, Integer>> chunks = Lists.newArrayList();
    while (m.find()) {
      int chunkIndex = Integer.parseInt(m.group(1));
      chunks.add(
          Pair.pair(renderedSrc.substring(start, m.start()), chunkIndex));
      start = m.end();
    }
    chunks.add(Pair.pair(renderedSrc.substring(start), -1));
    return chunks;
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

  private static final class Chunk {
    private final String src;
    private final FilePosition start;
    Chunk(String src, FilePosition start) {
      this.start = start;
      this.src = src;
    }
  }
}
