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
import com.google.caja.lexer.InputSource;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Formatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * A {@link TokenConsumer} that prints its output by writing out the
 * original source in comments, followed by the translated source.
 *
 * @author ihab.awad@gmail.com
 */
public class SourceSnippetRenderer implements TokenConsumer {

  private static final class OriginalSourceLine {
    private final int lineNo;
    private final String text;
    private Map<Integer, Integer> evidence = Maps.newHashMap();

    public OriginalSourceLine(int lineNo, String text) {
      this.lineNo = lineNo;
      this.text = text;
    }

    public int getLineNo() { return lineNo; }

    public String getText() { return text; }

    public void addEvidence(int renderedLine, int weight) {
      if (!evidence.containsKey(renderedLine)) {
        evidence.put(renderedLine, 0);
      }
      evidence.put(renderedLine, evidence.get(renderedLine) + weight);
    }

    public int computeBestRenderedLine() {
      int bestLine = -1;
      int bestEvidence = 0;

      for (int renderedLine : evidence.keySet()) {
        if (evidence.get(renderedLine) > bestEvidence) {
          bestLine = renderedLine;
          bestEvidence = evidence.get(renderedLine);
        }
      }

      return bestLine;
    }
  }

  private class RenderedSourceLine {
    private final StringBuilder textBuffer = new StringBuilder();
    private final Map<InputSource, Map<Integer, OriginalSourceLine>> lines
        = Maps.newHashMap();

    public RenderedSourceLine(String text) {
      textBuffer.append(text);
    }

    public void appendText(String text) {
      textBuffer.append(text);
    }

    public void addOriginalSourceLine(
        InputSource inputSource,
        int lineNo,
        OriginalSourceLine line) {
      Map<Integer, OriginalSourceLine> m = lines.get(inputSource);
      if (m == null) {
        m = Maps.newHashMap();
        lines.put(inputSource, m);
      }
      m.put(lineNo, line);
    }

    public void render(Concatenator out) {
      for (InputSource s : lines.keySet()) {
        out.consume("\n");
        out.consume("// *** ");
        out.consume(mc.abbreviate(s));
        out.consume(" ***\n");

        Map<Integer, OriginalSourceLine> forSource = lines.get(s);
        List<Integer> sortedLines = Lists.newArrayList(forSource.keySet());
        Collections.sort(sortedLines);

        for (int l : sortedLines) {
          OriginalSourceLine sl = forSource.get(l);
          out.consume("// ");
          out.consume(formatLineNo(sl.getLineNo()));
          out.consume(": ");
          out.consume(scrubJsString(forSource.get(l).getText()));
          out.consume("\n");
        }
        out.consume("\n");
      }

      out.consume(shrinkSpaces(textBuffer.toString()));
      out.consume("\n");
    }
  }

  private final MessageContext mc;
  private final RenderContext rc;
  private final TokenConsumer delegateRenderer;
  private final List<RenderedSourceLine> renderedLines = Lists.newArrayList();
  private final Map<InputSource, List<OriginalSourceLine>> originalSourceLines
      = Maps.newHashMap();
  private final List<FilePosition> marks = Lists.newArrayList();
  private final StringBuilder renderedTextAccumulator = new StringBuilder();

  public SourceSnippetRenderer(
      Map<InputSource, ? extends CharSequence> originalSource,
      MessageContext mc, RenderContext rc) {
    this.mc = mc;
    this.rc = rc;
    this.delegateRenderer = new JsPrettyPrinter(
        new Concatenator(renderedTextAccumulator));
    buildOriginalSourceLines(originalSource);
    renderedLines.add(new RenderedSourceLine(""));
  }

  public void mark(@Nullable FilePosition pos) {
    delegateRenderer.mark(pos);
    delegateRenderer.consume("/*@" + marks.size() + "*/");
    marks.add(pos);
  }

  public void consume(String text) {
    if (TokenClassification.isComment(text)) { return; }
    delegateRenderer.consume(text);
  }

  public void noMoreTokens() {
    delegateRenderer.noMoreTokens();

    int consumed = 0;
    String renderedText = renderedTextAccumulator.toString();
    Matcher m = Pattern.compile(" */\\*@([0-9]+)\\*/").matcher(renderedText);
    FilePosition currentMark = FilePosition.UNKNOWN;
    boolean first = true;
    while (m.find()) {
      String chunk = renderedText.substring(consumed, m.start());
      consumed = m.end();

      if ("".equals(chunk)) { continue; }

      if (first) {
        chunk = chunk.replaceFirst("^[\r\n]+", "");
        first = false;
      }
      processCurrentMark(currentMark, splitLines(chunk));
      currentMark = marks.get(Integer.parseInt(m.group(1)));
    }
    processCurrentMark(currentMark, splitLines(renderedText.substring(consumed)));
    processProgram();
    renderOutput();
  }

  private void buildOriginalSourceLines(
      Map<InputSource, ? extends CharSequence> originalSource) {
    for (Map.Entry<InputSource, ? extends CharSequence> entry
        : originalSource.entrySet()) {
      List<OriginalSourceLine> lines = Lists.newArrayList();
      String[] text = splitLines(entry.getValue().toString());
      for (int i = 0; i < text.length; i++) {
        lines.add(new OriginalSourceLine(i, text[i]));
      }
      originalSourceLines.put(entry.getKey(), lines);
    }
  }

  private static String[] splitLines(String s) {
    return s.split("\r\n?|\n");
  }

  private void processCurrentMark(FilePosition mark, String[] textLines) {
    if (textLines.length == 0) { return; }

    // The zeroth element is an addition to the current line
    renderedLines.get(renderedLines.size() - 1).appendText(textLines[0]);
    addEvidenceForCurrentMark(mark, textLines[0].length());

    // Subsequent elements add new lines
    for (int i = 1; i < textLines.length; ++i) {
      renderedLines.add(new RenderedSourceLine(textLines[i]));
      addEvidenceForCurrentMark(mark, textLines[i].length());
    }
  }

  private void addEvidenceForCurrentMark(FilePosition mark, int evidence) {
    if (mark == null || InputSource.UNKNOWN.equals(mark.source())) { return; }

    List<OriginalSourceLine> sourceList =
        originalSourceLines.get(mark.source());
    if (sourceList == null) { return; }

    for (int l = mark.startLineNo() - 1; l <= (mark.endLineNo() - 1); ++l) {
      sourceList.get(l).addEvidence(renderedLines.size() - 1, evidence);
    }
  }

  private void processProgram() {
    for (InputSource is : originalSourceLines.keySet()) {
      List<OriginalSourceLine> orig = originalSourceLines.get(is);
      int lastRenderedLine = 0;

      for (int lineIdx = 0; lineIdx < orig.size(); ++lineIdx) {
        OriginalSourceLine line = orig.get(lineIdx);
        int bsl = line.computeBestRenderedLine();

        System.out.println("***** " + bsl + " --- " + line.getText());

        if (bsl < 0) { bsl = lastRenderedLine; }
        renderedLines.get(bsl).addOriginalSourceLine(is, lineIdx, line);
      }
    }
  }

  private void renderOutput() {
    for (RenderedSourceLine rl : renderedLines) {
      rl.render((Concatenator) rc.getOut());
    }
  }

  private static String scrubJsString(String orig) {
    return orig
        .replaceAll("[\\p{javaIdentifierIgnorable}]", "\uFFFD")
        .replace('@', '\uFFFD')
        .replace("*/", "\uFFFD/")
        .replace("<!", "<\uFFFD")
        .replace("-->", "-\uFFFD>")
        .replace("]]>", "]\uFFFD>")
        .replace("</", "<\uFFFD");
  }

  private static String shrinkSpaces(String orig) {
    return orig.replaceAll("^ +", "");
  }

  private static String formatLineNo(int lineNo) {
    StringBuilder sb = new StringBuilder();
    Formatter f = new Formatter(sb);
    f.format("%5d", lineNo + 1);
    return sb.toString();
  }
}