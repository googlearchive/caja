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
import com.google.caja.reporting.MessageContext;
import com.google.caja.util.Callback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link SideBySideRenderer} that caches its results so that it can print
 * out a two column table containing the original source code in comments to
 * the left of the translated code.
 *
 * @author mikesamuel@gmail.com
 */
public class TabularSideBySideRenderer extends SideBySideRenderer {
  private final List<TableRow> rows = new ArrayList<TableRow>();
  private final MessageContext mc;
  private final Appendable out;
  private final Callback<IOException> exHandler;
  private boolean closed = false;

  public TabularSideBySideRenderer(
      Map<InputSource, ? extends CharSequence> originalSource,
      MessageContext mc, Appendable out, Callback<IOException> exHandler) {
    super(originalSource);
    if (out == null) { throw new NullPointerException(); }
    this.mc = mc;
    this.out = out;
    this.exHandler = exHandler;
  }

  @Override
  protected TokenConsumer makeRenderer(StringBuilder sb) {
    return new JsPrettyPrinter(sb, null);
  }

  @Override
  protected void emitLine(FilePosition pos, String orig, String rendered) {
    String[] origLines = splitLines(trimNewlines(orig));
    String[] renderedLines = splitLines(trimNewlines(rendered));
    int n = Math.min(origLines.length, renderedLines.length);
    for (int i = 0; i < n; ++i) {
      rows.add(new CodeRow(origLines[i], renderedLines[i]));
    }
    for (int i = n; i < origLines.length; ++i) {
      rows.add(new CodeRow(origLines[i], ""));
    }
    for (int i = n; i < renderedLines.length; ++i) {
      rows.add(new CodeRow("", renderedLines[i]));
    }
  }

  @Override
  protected void switchSource(FilePosition prev, FilePosition next) {
    rows.add(new FileRow(next.source().getShortName(mc.inputSources)));
  }

  @Override
  public void noMoreTokens() {
    if (closed) { return; }
    closed = true;
    super.noMoreTokens();

    try {
      int[] widths = layoutRows(rows);
      StringBuilder actual = new StringBuilder();
      for (TableRow row : rows) {
        out.append(row.toString(widths)).append('\n');
      }
    } catch (IOException ex) {
      exHandler.handle(ex);
    }
  }

  /** Calculates width of columns. */
  private static int[] layoutRows(List<TableRow> rows) {
    int nCols = 0;
    for (TableRow row : rows) { nCols = Math.max(nCols, row.nCols()); }

    int[] widths = new int[nCols];
    for (int n = nCols; n > 0; --n) {
      // Rows with fewer columns extend their rightmost column.
      int spaceToRight = 0;
      for (int i = nCols; --i >= n;) {
        spaceToRight += widths[i];
      }
      for (TableRow row : rows) {
        if (n != row.nCols()) { continue; }
        for (int i = 0; i < n; ++i) {
          widths[i] = Math.max(widths[i], row.minWidth(i) - spaceToRight);
        }
      }
    }
    return widths;
  }

  /** A row in a table that can calculate its minimum width. */
  private static interface TableRow {
    int nCols();
    int minWidth(int col);
    String toString(int[] colWidths);
  }

  /**
   * A row that displays the same line of source code before and after
   * rewriting.
   */
  private static class CodeRow implements TableRow {
    private final String orig, rendered;
    CodeRow(String orig, String rendered) {
      this.orig = orig;
      this.rendered = rendered;
    }

    public int nCols() { return 2; }

    public int minWidth(int col) {
      return (col == 0
              ? origString(0) + 1
              : renderedString(0)).toString().length();
    }

    String origString(int minWidth) {
      StringBuilder sb = new StringBuilder();
      sb.append("/* ");
      sb.append(
          orig.replaceAll("[\\p{javaIdentifierIgnorable}]", "\uFFFD")
          .replace('@', '\uFFFD')
          .replace("*/", "\uFFFD/")
          .replace("<!", "<\uFFFD")
          .replace("-->", "-\uFFFD>")
          .replace("<!", "<\uFFFD")
          .replace("]]>", "]\uFFFD>")
          .replace("</", "<\uFFFD"));
      // Leave 4 spaces for the comment end and space between columns.
      while (sb.length() < minWidth - 4) {
        sb.append(' ');
      }
      sb.append(" */");
      return sb.toString();
    }

    String renderedString(int minWidth) {
      return rendered;
    }

    public String toString(int[] colWidths) {
      String col0 = origString(colWidths[0]),
          col1 = renderedString(colWidths[1]);
      return "".equals(col1) ? col0 : col0 + " " + col1;
    }
  }

  /**
   * A row that indicates a change in the original soruce file.
   */
  private static class FileRow implements TableRow {
    private final String path;
    FileRow(String path) {
      this.path = path;
    }

    public int nCols() { return 1; }

    public int minWidth(int col) { return path.length() + 6; }

    public String toString(int[] colWidths) {
      int width = 0;
      for (int w : colWidths) { width += w; }
      StringBuilder sb = new StringBuilder();
      sb.append("/*");
      int padding = (width - 6 - path.length());
      for (int i = padding / 2; --i >= 0;) { sb.append('*'); }
      sb.append(' ').append(path).append(' ');
      for (int i = (padding - (padding / 2)); --i >= 0;) { sb.append('*'); }
      sb.append("*/");
      return sb.toString();
    }
  }

  private static String[] splitLines(String s) {
    if ("".equals(s)) { return new String[0]; }
    return s.split("\r\n?|\n");
  }

  private static String trimNewlines(String s) {
    int start = 0;
    int end = s.length();
    while (start < end) {
      char ch = s.charAt(start);
      if (ch != '\r' && ch != '\n') { break; }
      ++start;
    }
    while (end > start) {
      char ch = s.charAt(end - 1);
      if (ch != '\r' && ch != '\n') { break; }
      --end;
    }
    return s.substring(start, end);
  }
}
