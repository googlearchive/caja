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

package com.google.caja.reporting;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;

import java.io.IOException;
import java.util.Map;

/**
 * Given original source code, produces snippets for error messages.
 * <p>
 * For a {@link Message} message like {@code file:16+10-13: bar not defined},
 * the snippet might look like
 * <pre>
 *     file:16 var foo = bar() + baz
 *                       ^^^
 * </pre>
 *
 * @author mikesamuel@gmail.com
 */
public class SnippetProducer {
  private static final int DEFAULT_MAX_WIDTH = 80;
  private static final int DEFAULT_TAB_WIDTH = 8;

  private final Map<InputSource, ? extends CharSequence> originalSource;
  protected final MessageContext mc;
  protected final int maxWidth, tabWidth;

  public SnippetProducer(
      Map<InputSource, ? extends CharSequence> originalSource,
      MessageContext mc) {
    this(originalSource, mc, DEFAULT_MAX_WIDTH, DEFAULT_TAB_WIDTH);
  }

  public SnippetProducer(
      Map<InputSource, ? extends CharSequence> originalSource,
      MessageContext mc,
      int maxWidth) {
    this(originalSource, mc, maxWidth, DEFAULT_TAB_WIDTH);
  }

  public SnippetProducer(
      Map<InputSource, ? extends CharSequence> originalSource,
      MessageContext mc, int maxWidth, int tabWidth) {
    this.originalSource = originalSource;
    this.mc = mc;
    this.maxWidth = maxWidth;
    this.tabWidth = tabWidth;
  }

  public final String getSnippet(Message msg) {
    StringBuilder snippet = new StringBuilder();
    for (MessagePart mp : msg.getMessageParts()) {
      if (!(mp instanceof FilePosition)) { continue; }
      FilePosition pos = (FilePosition) mp;
      int len = snippet.length();
      if (len != 0) { snippet.append('\n'); }
      int snippetStart = snippet.length();
      try {
        appendSnippet(pos, snippet);
      } catch (IOException ex) {
        throw new SomethingWidgyHappenedError(
            "StringBuilders shouldn't throw IOExceptions", ex);
      }
      // If no content written by appendSnippet, then remove the newline.
      if (snippet.length() == snippetStart) { snippet.setLength(len); }
    }
    return snippet.toString();
  }

  public final void appendSnippet(FilePosition pos, Appendable out)
      throws IOException {
    InputSource src = pos.source();
    CharSequence sourceCode = originalSource.get(src);
    if (sourceCode == null) { return; }  // Can't write.

    // Pick a representative line from pos.
    int lineNo = pos.startLineNo();
    // TODO(mikesamuel): startCharInLine shouldn't ever return a number < 1
    int start = Math.max(0, pos.startCharInLine() - 1);
    CharSequence line = fetchLine(sourceCode, lineNo);

    if (line != null
        && (line.length() == 0 || isLinebreak(line.charAt(0)))
        && lineNo + 1 <= pos.endLineNo()) {
      // If the start of the pos is a newline, advance to the next.
      ++lineNo;
      start = 0;
      line = fetchLine(sourceCode, lineNo);
    }
    if (line == null) { return; }

    // Be paranoid about position since we don't want bad positions or errors
    // in the originalSource map to prevent us from reporting errors at all.
    start = Math.min(line.length(), start);
    int end = Math.max(
        Math.min((pos.endLineNo() == lineNo
                  ? pos.endCharInLine() - 1 : Integer.MAX_VALUE),
                 line.length()),
        start);

    // Reduce line to maxWidth of context.
    if (0 < maxWidth && maxWidth < line.length()) {
      end = Math.min(end, start + maxWidth);
      int left = Math.max(0, end - maxWidth);
      int right = Math.min(line.length(), left + maxWidth);
      line = line.subSequence(left, right);
      start -= left;
      end -= left;
    }

    formatSnippet(pos,
        FilePosition.instance(src, lineNo, 1, line.length() + 1),
        line, start, end, out);
  }

  /**
   * May be overridden to format a snippet differently, e.g. by HTML escaping
   * line and inserting tags around {@code line[start:end]}.
   *
   * @param errorPosition actual unmodified error fileposition
   * @param snippetPos line granularity error position of the snippet
   * @param end >= start.  Implementations should take care to provide some
   *   useful information if end == start, since a zero length range might
   *   be used to indicate where information is missing, or where inferred
   *   content was inserted.
   * @throws IOException only if out raised an IOException.
   */
  protected void formatSnippet(FilePosition errorPosition,
      FilePosition snippetPos, CharSequence line, int start, int end,
      Appendable out)
      throws IOException {
    // Write out "file:14: <line-of-sourcecode>"
    StringBuilder posBuf = new StringBuilder();
    formatFilePosition(snippetPos, posBuf);
    posBuf.append(": ");
    int filePosLength = posBuf.length();

    int nSpaces = start + filePosLength;
    int nCarets = end - start;

    out.append(posBuf);
    // Expand tabs so that the carets line up with the source.
    int nExtraSpaces = expandTabs(line, 0, start, 0, out);
    int nExtraCarets = expandTabs(line, start, end, nExtraSpaces, out);
    expandTabs(line, end, line.length(), nExtraSpaces + nExtraCarets, out);
    if (line.length() == 0 || !isLinebreak(line.charAt(line.length() - 1))) {
      // If the line is the last in the file, it may not end with a newline.
      out.append("\n");
    }
    repeat("                ", nSpaces + nExtraSpaces, out);
    repeat("^^^^^^^^^^^^^^^^", Math.max(nCarets + nExtraCarets, 1), out);
  }

  /**
   * May be overridden to format a position differently, e.g. by inserting links
   * to source files.
   */
  protected void formatFilePosition(FilePosition pos, Appendable out)
      throws IOException {
    pos.source().format(mc, out);
    out.append(":");
    out.append(String.valueOf(pos.startLineNo()));
  }

  /** Append count characters from pattern onto out, repeating if necessary. */
  private static void repeat(String pattern, int count, Appendable out)
      throws IOException {
    while (count >= pattern.length()) {
      out.append(pattern);
      count -= pattern.length();
    }
    if (count > 0) { out.append(pattern, 0, count); }
  }


  // The scheme below does not take into account different languages'
  // different definitions of newline, but it does use the same scheme as
  // CharProducer's language agnostic line counting scheme which agrees
  // with source code editors.
  // CharProducer does not bump the lineNo counter on codepoints 0x2028,2029.
  private static CharSequence fetchLine(CharSequence seq, int lineNo) {
    int pos = 0;
    for (int i = lineNo; --i >= 1;) {
      pos = posPastNextLinebreak(seq, pos);
    }
    int start = pos;
    int end = posPastNextLinebreak(seq, pos);
    if (start < end) {
      return seq.subSequence(start, end);
    }
    return null;
  }

  private static int indexOf(
      CharSequence seq, char ch, int fromIndex, int toIndex) {
    for (int i = fromIndex; i < toIndex; ++i) {
      if (seq.charAt(i) == ch) { return i; }
    }
    return -1;
  }

  private int expandTabs(
      CharSequence seq, int start, int end, int nExpanded, Appendable out)
      throws IOException {
    final String SPACES = "        ";
    int tabIdx = indexOf(seq, '\t', start, end);
    if (tabIdx < 0) {
      out.append(seq, start, end);
      return 0;
    }
    int nExtra = 0;
    int done = start;
    do {
      out.append(seq, done, tabIdx);
      int nBefore = nExtra + tabIdx + nExpanded;
      int nSpaces = tabWidth - (nBefore % tabWidth);
      nExtra += nSpaces - 1;
      while (nSpaces >= SPACES.length()) {
        out.append(SPACES);
        nSpaces -= SPACES.length();
      }
      out.append(SPACES, 0, nSpaces);
      done = tabIdx + 1;
    } while ((tabIdx = indexOf(seq, '\t', done, end)) >= 0);
    out.append(seq, done, end);
    return nExtra;
  }

  private static int posPastNextLinebreak(CharSequence seq, int pos) {
    int len = seq.length();
    for (;pos < len; ++pos) {
      char ch = seq.charAt(pos);
      if (ch == '\n') { return pos + 1; }
      if (ch == '\r') {
        return pos + ((pos + 1 < len && '\n' == seq.charAt(pos + 1)) ? 2 : 1);
      }
    }
    return len;
  }

  private static boolean isLinebreak(char ch) {
    return ch == '\r' || ch == '\n';
  }
}
