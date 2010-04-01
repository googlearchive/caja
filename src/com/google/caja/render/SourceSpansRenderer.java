// Copyright (C) 2009 Google Inc.
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
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.NullLiteral;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;
import com.google.caja.util.Sets;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * A {@link TokenConsumer} that prints its output, and gives the original
 * {@link FilePosition} source of each character of the output.
 *
 * See
 * http://google-caja.googlecode.com/svn/trunk/doc/html/compiledModuleFormat/index.html
 *
 * @author ihab.awad@gmail.com
 */
public class SourceSpansRenderer implements TokenConsumer {

  // A helper for declaring in/out function parameters.
  private static class Slot<T> {
    public T value;
  }

  private static final Comparator<InputSource> INPUT_SOURCE_COMPARATOR =
      new Comparator<InputSource>() {
        public int compare(InputSource x, InputSource y) {
          if (x == null && y == null) { return 0; }
          if (x == null) { return -1; }
          if (y == null) { return 1; }
          return x.getUri().compareTo(y.getUri());
        }
      };

  private static final Pattern MARK_PATTERN = Pattern.compile(
      " */\\*@([0-9]+)\\*/");

  private final MessageContext mc = new MessageContext();
  private final InputSource cajoledOutputFilename;
  private final RenderContext c;
  private final StringBuilder programTextAccumulator = new StringBuilder();
  private final TokenConsumer delegateRenderer;
  private final List<FilePosition> marks = Lists.newArrayList();
  private String programText;
  private final List<String> sourceLocationMap = Lists.newArrayList();

  /**
   * Create a SourceSpansRenderer.
   *
   * @param cajoledOutputFilename the filename that the client of this class
   *     wishes to call the cajoled output (which is otherwise anonymous).
   */
  public SourceSpansRenderer(
      InputSource cajoledOutputFilename, RenderContext c) {
    this.cajoledOutputFilename = cajoledOutputFilename;
    this.c = c;
    this.delegateRenderer = new JsPrettyPrinter(
        new Concatenator(programTextAccumulator));
    ((JsPrettyPrinter) delegateRenderer).setBreakAfterComment(false);
  }

  public void mark(FilePosition pos) {
    delegateRenderer.mark(pos);
    delegateRenderer.consume("/*@" + marks.size() + "*/");
    marks.add(pos == null ? FilePosition.UNKNOWN : pos);
  }

  public void consume(String text) {
    if (TokenClassification.isComment(text)) { return; }
    delegateRenderer.consume(text);
  }

  public void noMoreTokens() {
    delegateRenderer.noMoreTokens();
    programText = programTextAccumulator.toString();

    programTextAccumulator.delete(0, programTextAccumulator.length());

    List<List<FilePosition>> allPositionsByLine = buildSourcePositionMappings();
    programText = programTextAccumulator.toString();

    compressSourcePositionMappings(allPositionsByLine);
  }

  private List<List<FilePosition>> buildSourcePositionMappings() {
    String[] renderedLines = splitLines(programText);
    Slot<FilePosition> currentPosition = new Slot<FilePosition>();
    currentPosition.value = FilePosition.UNKNOWN;
    List<List<FilePosition>> allPositionsByLine = Lists.newArrayList();

    for (String renderedLine : renderedLines) {
      allPositionsByLine.add(
          buildSourcePositionMappingForLine(currentPosition, renderedLine));
    }

    marks.clear();

    return allPositionsByLine;
  }

  private List<FilePosition> buildSourcePositionMappingForLine(
      Slot<FilePosition> currentPosition, String line) {
    List<FilePosition> result = Lists.newArrayList();
    Matcher m = MARK_PATTERN.matcher(line);
    int consumed = 0;

    while (m.find()) {
      String chunk = line.substring(consumed, m.start());
      programTextAccumulator.append(chunk);

      for (int i = 0; i < chunk.length(); i++) {
        result.add(currentPosition.value);
      }

      consumed = m.end();
      currentPosition.value = marks.get(Integer.parseInt(m.group(1)));
    }

    if (consumed < line.length()) {
      programTextAccumulator.append(line, consumed, line.length());

      for (int i = 0; i < line.length() - consumed; i++) {
        result.add(currentPosition.value);
      }
    }

    programTextAccumulator.append('\n');

    return result;
  }

  private void compressSourcePositionMappings(
      List<List<FilePosition>> allPositionsByLine) {
    // For each line, file position indices, as in --
    //   [2,3,,,,,,,4]
    List<List<Integer>> linePositionIndicesByLine = Lists.newArrayList();

    // For each line, input sources on that line, as in --
    //   ["x.js", "y.js", "z.js"]
    List<Set<InputSource>> inputSourcesByLine = Lists.newArrayList();

    // Map from each file position seen to its array index, keyed by file
    // position to allow efficient searching while we build up the structures
    Map<FilePosition, Integer> tableIndexByFilePosition = Maps.newHashMap();

    // Table of file positions
    List<FilePosition> filePositionTable = Lists.newArrayList();

    for (int lineIdx = 0; lineIdx < allPositionsByLine.size(); lineIdx++) {
      linePositionIndicesByLine.add(Lists.<Integer>newArrayList());
      inputSourcesByLine.add(
          Sets.<InputSource>newTreeSet(INPUT_SOURCE_COMPARATOR));

      for (int charIdx = 0; charIdx < allPositionsByLine.get(lineIdx).size();
           charIdx++) {

        FilePosition currentPos = FilePosition.startOf(
            allPositionsByLine.get(lineIdx).get(charIdx));

        inputSourcesByLine.get(lineIdx).add(currentPos.source());

        if (!currentPos.source().equals(InputSource.UNKNOWN)) {
          mc.addInputSource(currentPos.source());
        }

        Integer tableIndex = tableIndexByFilePosition.get(currentPos);
        if (tableIndex == null) {
          tableIndexByFilePosition.put(currentPos, filePositionTable.size());
          tableIndex = filePositionTable.size();
          filePositionTable.add(currentPos);
        }

        linePositionIndicesByLine.get(lineIdx).add(tableIndex);
      }
    }

    renderSourcePositionMappings(
        linePositionIndicesByLine,
        inputSourcesByLine,
        filePositionTable);
  }

  private void renderSourcePositionMappings(
      List<List<Integer>> linePositionIndicesByLine,
      List<Set<InputSource>> inputSourcesByLine,
      List<FilePosition> filePositionTable) {
    FilePosition unk = FilePosition.UNKNOWN;
    // Input source might be null.
    String inputSource = renderInputSource(mc, cajoledOutputFilename);
    Literal fileLit;
    if (inputSource == null) {
      fileLit = new NullLiteral(unk);
    } else {
      fileLit = StringLiteral.valueOf(unk, inputSource);
    }
    ObjectConstructor oc = (ObjectConstructor) QuasiBuilder.substV(
        "({ count: @count, file: @file })",
        "file", fileLit,
        "count", new IntegerLiteral(unk, linePositionIndicesByLine.size()));
    StringBuilder header = new StringBuilder("/** Begin line maps. **/");
    TokenConsumer tc = new JsMinimalPrinter(new Concatenator(header));
    oc.render(new RenderContext(tc).withJson(true)
              .withAsciiOnly(c.isAsciiOnly()).withEmbeddable(c.isEmbeddable()));
    tc.noMoreTokens();
    sourceLocationMap.add(header.toString());

    for (int i = 0; i < linePositionIndicesByLine.size(); i++) {
      sourceLocationMap.add(toJson(linePositionIndicesByLine.get(i), c));
    }

    sourceLocationMap.add("/** Begin file information. **/");

    List<Object> line = Lists.newArrayList();
    for (int i = 0; i < inputSourcesByLine.size(); i++) {
      line.clear();
      for (InputSource p : inputSourcesByLine.get(i)) {
        line.add(renderInputSource(mc, p));
      }
      sourceLocationMap.add(toJson(line, c));
    }

    sourceLocationMap.add("/** Begin mapping definitions. **/");

    for (int i = 0; i < filePositionTable.size(); i++) {
      line.clear();
      line.add(renderInputSource(mc, filePositionTable.get(i).source()));
      line.add(filePositionTable.get(i).startLineNo());
      line.add(filePositionTable.get(i).startCharInLine());
      sourceLocationMap.add(toJson(line, c));
    }
  }

  private static String toJson(List<?> els, RenderContext c) {
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    String sep = "";
    for (Object o : els) {
      sb.append(sep);
      sep = ",";
      if (o instanceof Number || o instanceof Boolean) {
        sb.append(o);
      } else if (o == null) {
        sb.append("null");
      } else {
        sb.append('"');
        Escaping.escapeJsString(
            (CharSequence) o, c.isAsciiOnly(), c.isEmbeddable(), sb);
        sb.append('"');
      }
    }
    return sb.append(']').toString();
  }

  /**
   * Return the rendered text of the input program.
   *
   * <p>This is returned as a single string with embedded newlines. Consumers
   * of this string should never manipulate it further, so as not to risk
   * changing its meaning. Contrast {@link #getSourceLocationMap()}.
   */
  public String getProgramText() {
    return programText;
  }

  /**
   * Return the source location map for the input program. This will contain
   * internal <em>abbreviated</em> references to {@code InputSource}s. To
   * find out what the abbreviated names are, ask the
   * {@link #getMessageContext()} of this {@code SourceSpansRenderer} to
   * {@link MessageContext#abbreviate(com.google.caja.lexer.InputSource)}
   * each {@code InputSource}.
   *
   * <p>This is returned as an array of strings, one per line of output.
   * Consumers may either write these to a file (say), adding a newline after
   * each one, or embed each line into (say) a string literal.
   *
   * <p>Each line is a valid JavaScript expression or a comment. If a consumer
   * embeds the line into (say) a string literal, they are responsible for
   * escaping it properly.
   */
  public List<String> getSourceLocationMap() {
    return sourceLocationMap;
  }

  /**
   * @return the {@link MessageContext} used by this object, which gives
   * the set of {@link InputSource}s used by the processed source, and can
   * supply the abbreviated names, via
   * {@link MessageContext#abbreviate(com.google.caja.lexer.InputSource)},
   * which are actually mentioned in the source location map.
   */
  public MessageContext getMessageContext() {
    return mc;
  }

  private static String[] splitLines(String s) {
    return s.split("\r\n?|\n", -1);
  }

  private static String renderInputSource(MessageContext mc, InputSource is) {
    return InputSource.UNKNOWN.equals(is) ? null : mc.abbreviate(is);
  }
}
