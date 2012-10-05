// Copyright (C) 2005 Google Inc.
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

package com.google.caja.lexer;

import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import java.io.IOException;
import java.io.Serializable;

import javax.annotation.Nullable;

/**
 * The range of characters in a source file occupied by a token or a group of
 * tokens.
 *
 * <p>Fields are 1 indexed since that is how most text editors display line and
 * character (UTF-16 code-unit) positions.  The start numbers are inclusive, and
 * the end numbers are exclusive, so the number of characters in a token is
 * (startCharInFile - endCharInFile).
 *
 * <p>Line numbers are 1 greater than the number of line breaks where a
 * line-break is defined greedily as one of {{{CR}}}, {{{LF}}}, {{{CRLF}}}.
 * Bash, javascript, or python style line-continuations like<pre>
 *   a, b, c = 0, 1, \
 *       2
 * </pre>
 * are treated as line breaks.
 * Line numbers are determined solely based on the characters in the file and do
 * <b>not</b> represent a logical unit of a program.
 *
 * <p>File Positions are independent of the language being parsed.
 *
 * @author mikesamuel@gmail.com
 */
public final class FilePosition implements MessagePart, Serializable {
  private static final long serialVersionUID = 1867023076917057795L;

  public static final FilePosition UNKNOWN = FilePosition.instance(
      InputSource.UNKNOWN, 1, 0, 0);

  private final SourceBreaks breaks;
  private final int startCharInFile, length;

  FilePosition(SourceBreaks breaks, int startCharInFile, int length) {
    // TODO(mikesamuel): FilePosition.UNKNOWN makes this assertion fail
    // assert length >= 0 : "length = " + length + " < 0";
    this.breaks = breaks;
    breaks.makeImmutable();
    this.startCharInFile = startCharInFile;
    this.length = length;
  }

  SourceBreaks getBreaks() { return breaks; }

  public InputSource source() { return breaks.source(); }
  /**
   * 1 greater than the number of newlines between the start of the token and
   * the beginning of the file.
   */
  public int startLineNo() { return breaks.lineAt(startCharInFile); }
  /**
   * 1 greater than the number of characters since the beginning of the file.
   */
  public int startCharInFile() { return startCharInFile; }
  /**
   * 1 greater than the number of characters since the last newline character.
   */
  public int startCharInLine() { return breaks.charInLineAt(startCharInFile); }
  public int endLineNo() { return breaks.lineAt(endCharInFile()); }
  public int endCharInFile() { return startCharInFile + length; }
  public int endCharInLine() { return breaks.charInLineAt(endCharInFile()); }
  public int length() { return length; }

  public FilePosition narrowTo(int offset, int length) {
    return breaks.toFilePosition(
        startCharInFile + offset, startCharInFile + offset + length);
  }

  public static FilePosition between(FilePosition a, FilePosition b) {
    if (a.getBreaks() != b.getBreaks()) { return FilePosition.UNKNOWN; }
    int start = a.startCharInFile() + a.length();
    return new FilePosition(a.getBreaks(), start, b.startCharInFile() - start);
  }

  public static FilePosition instance(
      InputSource source, int lineNo, int charInFile, int charInLine) {
    return instance(source, lineNo, charInFile, charInLine, 0);
  }

  public static FilePosition instance(
      InputSource source, int lineNo,
      int charInFile, int charInLine, int length) {
    SourceBreaks breaks = new SourceBreaks(source, lineNo - 1);
    breaks.lineStartsAt(charInFile - charInLine + 1);
    return breaks.toFilePosition(charInFile, charInFile + length);
  }

  /** Produce a FilePosition divorced from SourceBreaks to ease testing. */
  public static FilePosition fromLinePositions(
      InputSource source,
      int startLineNo, int startCharInLine, int endLineNo, int endCharInLine) {
    int lineNo = startLineNo - 1;
    SourceBreaks b = new SourceBreaks(source, lineNo);

    // Construct source breaks as if we were parsing a file.
    int charInFile = 1;
    int charInLine = 1;
    while (lineNo < startLineNo) {
      b.lineStartsAt(++charInFile);
      ++lineNo;
    }
    int delta = startCharInLine - charInLine;
    charInFile += delta;
    charInLine += delta;
    int startCharInFile = charInFile;

    while (lineNo < endLineNo) {
      charInLine = 1;
      b.lineStartsAt(++charInFile);
      ++lineNo;
    }
    delta = endCharInLine - charInLine;
    charInFile += delta;
    charInLine += delta;
    int endCharInFile = charInFile;

    return b.toFilePosition(startCharInFile, endCharInFile);
  }

  public static FilePosition startOfFile(InputSource source) {
    return instance(source, 1, 1, 1);
  }

  public static FilePosition span(FilePosition start, FilePosition end) {
    if (start == end) { return start; }
    if (!start.source().equalsAndNotUnknown(end.source())) {
      return FilePosition.UNKNOWN;
    }

    return start.getBreaks().toFilePosition(
        start.startCharInFile(), end.endCharInFile());
  }

  public static FilePosition startOf(FilePosition fp) {
    if (fp.length() == 0) { return fp; }
    return new FilePosition(fp.getBreaks(), fp.startCharInFile(), 0);
  }

  public static FilePosition endOf(FilePosition fp) {
    if (fp.length() == 0) { return fp; }
    return new FilePosition(
        fp.getBreaks(), fp.startCharInFile() + fp.length(), 0);
  }

  public static FilePosition endOfOrNull(@Nullable FilePosition fp) {
    return fp != null ? endOf(fp) : null;
  }

  public final void format(MessageContext context, Appendable out)
      throws IOException {
    source().format(context, out);
    out.append(":")
        .append(String.valueOf(this.startLineNo()))
        .append("+")
        .append(String.valueOf(this.startCharInLine()));
    if (this.startCharInFile() != this.endCharInFile()) {
      out.append(" - ");
      if (this.startLineNo() != this.endLineNo()) {
        out.append(String.valueOf(this.endLineNo()))
            .append("+");
      }
      out.append(String.valueOf(this.endCharInLine()));
    }
  }

  public final void formatShort(Appendable out) throws IOException {
    MessageContext mc = new MessageContext();
    InputSource is = source();
    mc.addInputSource(is);
    out.append(mc.abbreviate(is))
        .append(":")
        .append(String.valueOf(this.startLineNo()));
  }

  @Override
  public final String toString() {
    String fn = this.source().getUri().toString();
    StringBuilder sb = new StringBuilder();
    sb.append(fn.substring(fn.lastIndexOf('/') + 1))
      .append(':').append(this.startLineNo()).append('+')
      .append(this.startCharInLine()).append('@')
      .append(this.startCharInFile());
    if (this.startCharInFile() != this.endCharInFile()) {
      sb.append(" - ");
      if (this.endLineNo() != this.startLineNo()) {
        sb.append(this.endLineNo()).append('+');
      }
      sb.append(this.endCharInLine()).append('@')
        .append(this.endCharInFile());
    }
    return sb.toString();
  }

  @Override
  public final boolean equals(@Nullable Object o) {
    if (!(o instanceof FilePosition)) { return false; }
    FilePosition that = (FilePosition) o;
    // equals and hashCode depend only on information available
    // without walking the newline array.
    return (
        this.startCharInFile == that.startCharInFile
        && this.length == that.length
        && this.source().equals(that.source())
        );
  }

  @Override
  public final int hashCode() {
    return (
        this.source().hashCode()
        ^ this.startCharInFile
        ^ this.length
        );
  }
}
