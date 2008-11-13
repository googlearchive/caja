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
import com.google.caja.util.Callback;
import com.google.caja.reporting.MessageContext;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Formatter;
import java.io.IOException;

/**
 * A {@link TokenConsumer} that prints its output by writing out the
 * original source in comments, followed by the translated source.
 *
 * @author ihab.awad@gmail.com
 */
public abstract class SourceSnippetRenderer implements TokenConsumer {

  /**
   * An Appendable to which the renderer we delegate to will append text;
   * we capture this text by adding it to 'renderedTextAccumulator'
   */
  private final Appendable rendererAppendable = new Appendable() {
    public Appendable append(CharSequence charSequence)
        throws IOException {
      renderedTextAccumulator.append(charSequence);
      return this;
    }

    public Appendable append(CharSequence charSequence, int i, int j)
        throws IOException {
      renderedTextAccumulator.append(charSequence, i, j);
      return this;
    }

    public Appendable append(char c)
        throws IOException {
      renderedTextAccumulator.append(c);
      return this;
    }
  };

  private class OriginalSourceLine {
    private final InputSource source;
    private final int lineNo;
    private final String text;
    private Map<Integer, Integer> evidence = new HashMap<Integer, Integer>();
    
    public OriginalSourceLine(InputSource source, int lineNo, String text) {
      this.source = source;
      this.lineNo = lineNo;
      this.text = text;
    }

    public InputSource getSource() { return source; }

    public int getLineNo() { return lineNo; }

    public String getText() { return text; }

    public void addEvidence(int renderedLine, int weight) {
      if (!evidence.containsKey(renderedLine)) {
        evidence.put(renderedLine, 0);
      }
      evidence.put(
          renderedLine,
          evidence.get(renderedLine) + weight);
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
    private final int lineNo;
    private final StringBuilder textBuffer = new StringBuilder();
    private final Map<InputSource, Map<Integer, OriginalSourceLine>> lines =
        new HashMap<InputSource, Map<Integer, OriginalSourceLine>>();
    
    public RenderedSourceLine(int lineNo, String text) {
      this.lineNo = lineNo;
      textBuffer.append(text);
    }

    public int getLineNo() { return lineNo; }

    public void appendText(String text) {
      textBuffer.append(text);
    }

    public void addOriginalSourceLine(
        InputSource inputSource,
        int lineNo,
        OriginalSourceLine line) {
      Map<Integer, OriginalSourceLine> m = lines.get(inputSource);
      if (m == null) {
        m = new HashMap<Integer, OriginalSourceLine>();
        lines.put(inputSource, m);
      }
      m.put(lineNo, line);
    }

    public void render(Appendable out) throws IOException {
      for (InputSource s : lines.keySet()) {
        out
            .append("\n")
            .append("// *** ")
            .append(s.getShortName(mc.inputSources))
            .append(" ***\n");
        
        Map<Integer, OriginalSourceLine> forSource = lines.get(s);
        List<Integer> sortedLines = new ArrayList<Integer>(forSource.keySet());
        Collections.sort(sortedLines);

        for (int l : sortedLines) {
          OriginalSourceLine sl = forSource.get(l);
          out
              .append("// ")
              .append(formatLineNo(sl.getLineNo()))
              .append(": ")
              .append(scrubJsString(forSource.get(l).getText()))
              .append("\n");
        }
        out.append("\n");
      }

      out
          .append(shrinkSpaces(textBuffer.toString()))
          .append("\n");
    }
  }

  private final Appendable out;
  private final MessageContext mc;
  private final Callback<IOException> exHandler;
  private final TokenConsumer delegateRenderer;
  private final List<RenderedSourceLine> renderedLines
      = new ArrayList<RenderedSourceLine>();
  private final Map<InputSource, List<OriginalSourceLine>> originalSourceLines
      = new HashMap<InputSource, List<OriginalSourceLine>>();
  private FilePosition currentMark = FilePosition.UNKNOWN;
  private StringBuilder renderedTextAccumulator = new StringBuilder();

  public SourceSnippetRenderer(
      Map<InputSource, ? extends CharSequence> originalSource,
      MessageContext mc,
      Appendable out,
      Callback<IOException> exHandler) {
    this.out = out;
    this.mc = mc;
    this.exHandler = exHandler;
    delegateRenderer = createDelegateRenderer(rendererAppendable, exHandler);
    buildOriginalSourceLines(originalSource);
    renderedLines.add(new RenderedSourceLine(0, ""));
  }

  public void mark(FilePosition pos) {
    processCurrentMark();
    currentMark = pos;
    delegateRenderer.mark(pos);
  }

  public void consume(String text) {
    delegateRenderer.consume(text);
  }
  
  public void noMoreTokens() {
    delegateRenderer.noMoreTokens();
    processCurrentMark();
    processProgram();
    renderOutput();
  }

  protected abstract TokenConsumer createDelegateRenderer(
      Appendable out,
      Callback<IOException> exHandler);

  private void buildOriginalSourceLines(
      Map<InputSource, ? extends CharSequence> originalSource) {
    for (InputSource is : originalSource.keySet()) {
      List<OriginalSourceLine> lines = new ArrayList<OriginalSourceLine>();
      String[] text = originalSource.get(is).toString().split("\r\n?|\n");
      for (int i = 0; i < text.length; i++) {
        lines.add(new OriginalSourceLine(is, i, text[i]));
      }
      originalSourceLines.put(is, lines);
    }
  }

  private void processCurrentMark() {
    String[] textLines = renderedTextAccumulator.toString().split("\n");
    renderedTextAccumulator = new StringBuilder();
    if (textLines.length == 0) { return; }

    // The zeroth element is an addition to the current line
    renderedLines.get(renderedLines.size() - 1).appendText(textLines[0]);
    addEvidenceForCurrentMark(textLines[0].length());

    // Subsequent elements add new lines
    for (int i = 1; i < textLines.length; ++i) {
      renderedLines.add(
          new RenderedSourceLine(renderedLines.size(), textLines[i]));
      addEvidenceForCurrentMark(textLines[i].length());
    }
  }

  private void addEvidenceForCurrentMark(int evidence) {
    if (currentMark == FilePosition.UNKNOWN || currentMark == null) { return; }

    List<OriginalSourceLine> sourceList =
        originalSourceLines.get(currentMark.source());
    if (sourceList == null) { return; }
    
    for (int l = currentMark.startLineNo() - 1;
         l <= (currentMark.endLineNo() - 1);
         ++l) {
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
      try {
        rl.render(out);
      } catch (IOException e) {
        exHandler.handle(e);
      }
    }
  }

  private static String scrubJsString(String orig) {
    return orig
        .replaceAll("[\\p{javaIdentifierIgnorable}]", "\uFFFD")
        .replace('@', '\uFFFD')
        .replace("*/", "\uFFFD/")
        .replace("<!", "<\uFFFD")
        .replace("-->", "-\uFFFD>")
        .replace("<!", "<\uFFFD")
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