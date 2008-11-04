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
import java.net.URI;
import java.util.Collections;

/**
 * The range of characters in a source file occupied by a token or a group of
 * tokens.
 *
 * <p>Fields are 1 indexed since that is how most text editors display line and
 * character positions.  The start numbers are inclusive, and the end numbers
 * are exclusive, so the number of characters in a token is
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
public final class FilePosition implements MessagePart {
  // TODO(mikesamuel): need unittests
  private final InputSource source;
  private final int startLineNo;
  private final int startCharInFile;
  private final int startCharInLine;
  private final int endLineNo;
  private final int endCharInFile;
  private final int endCharInLine;

  /** a special position for predefineds like the <tt>String</tt> function. */
  public static final FilePosition PREDEFINED = instance(
      new InputSource(URI.create("predefined:///predefined")), 0, 0, 0);
  public static final FilePosition UNKNOWN = instance(
      new InputSource(URI.create("unknown:///unknown")), 0, 0, 0);

  private FilePosition(
    InputSource source,
    int startLineNo, int startCharInFile, int startCharInLine,
    int endLineNo, int endCharInFile, int endCharInLine) {

    this.source = source;
    this.startLineNo = startLineNo;
    this.startCharInFile = startCharInFile;
    this.startCharInLine = startCharInLine;
    this.endLineNo = endLineNo;
    this.endCharInFile = endCharInFile;
    this.endCharInLine = endCharInLine;
  }

  public static FilePosition instance(
      InputSource source,
      int startLineNo, int startCharInFile, int startCharInLine,
      int endLineNo, int endCharInFile, int endCharInLine) {
    return new FilePosition(
        source,
        startLineNo, startCharInFile, startCharInLine,
        endLineNo, endCharInFile, endCharInLine);
  }

  public static FilePosition instance(
      InputSource source, int lineNo, int charInFile, int charInLine) {
    return instance(source, lineNo, charInFile, charInLine,
                    lineNo, charInFile, charInLine);
  }

  public static FilePosition between(FilePosition a, FilePosition b) {
    return instance(a.source(),
                    a.endLineNo(), a.endCharInFile(), a.endCharInLine(),
                    b.startLineNo(), b.startCharInFile(), b.startCharInLine());
  }

  public static FilePosition startOfFile(InputSource source) {
    return instance(source, 1, 1, 1);
  }

  public static FilePosition span(FilePosition start, FilePosition end) {
    if (start == end) { return start; }
    if (!start.source.equals(end.source)
        || start.startCharInFile > end.endCharInFile) {
      throw new IllegalArgumentException(start + ", " + end);
    }
    return instance(
        start.source,
        start.startLineNo, start.startCharInFile, start.startCharInLine,
        end.endLineNo(), end.endCharInFile(), end.endCharInLine());
  }

  public static FilePosition startOf(FilePosition fp) {
    return FilePosition.instance(
        fp.source, fp.startLineNo, fp.startCharInFile, fp.startCharInLine);
  }

  public static FilePosition endOf(FilePosition fp) {
    return FilePosition.instance(
        fp.source, fp.endLineNo, fp.endCharInFile, fp.endCharInLine);
  }

  public static FilePosition startOfOrNull(FilePosition fp) {
    return fp != null ? startOf(fp) : null;
  }

  public static FilePosition endOfOrNull(FilePosition fp) {
    return fp != null ? endOf(fp) : null;
  }

  public InputSource source() { return this.source; }
  /**
   * 1 greater than the number of newlines between the start of the token and
   * the beginning of the file.
   */
  public int startLineNo() { return this.startLineNo; }
  /**
   * 1 greater than the number of characters since the beginning of the file.
   */
  public int startCharInFile() { return this.startCharInFile; }
  /**
   * 1 greater than the number of characters since the last newline character.
   */
  public int startCharInLine() { return this.startCharInLine; }
  public int endLineNo() { return this.endLineNo; }
  public int endCharInFile() { return this.endCharInFile; }
  public int endCharInLine() { return this.endCharInLine; }
  public int length() { return this.endCharInFile - this.startCharInFile; }

  public void format(MessageContext context, Appendable out)
      throws IOException {
    if (!FilePosition.PREDEFINED.equals(this)) {
      source.format(context, out);
      out.append(":")
        .append(String.valueOf(this.startLineNo))
        .append("+")
        .append(String.valueOf(this.startCharInLine));
      if (this.startCharInFile != this.endCharInFile) {
        out.append(" - ");
        if (this.startLineNo != this.endLineNo) {
          out.append(String.valueOf(this.endLineNo))
            .append("+");
        }
        out.append(String.valueOf(this.endCharInLine));
      }
    } else {
      out.append("predefined");
    }
  }

  public void formatShort(Appendable out) throws IOException {
    if (!FilePosition.PREDEFINED.equals(this)) {
      out.append(source.getShortName(Collections.<InputSource>emptyList()))
        .append(":")
        .append(String.valueOf(this.startLineNo));
    } else {
      out.append("predefined");
    }
  }

  @Override
  public String toString() {
    String fn = this.source.getUri().toString();
    StringBuilder sb = new StringBuilder();
    sb.append(fn.substring(fn.lastIndexOf('/') + 1))
      .append(':').append(this.startLineNo).append('+')
      .append(this.startCharInLine).append('@')
      .append(this.startCharInFile);
    if (this.startCharInFile != this.endCharInFile) {
      sb.append(" - ");
      if (this.endLineNo != this.startLineNo) {
        sb.append(this.endLineNo).append('+');
      }
      sb.append(this.endCharInLine).append('@')
        .append(this.endCharInFile);
    }
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof FilePosition)) { return false; }
    FilePosition that = (FilePosition) o;
    return (
        this.startCharInFile == that.startCharInFile
        && this.endCharInFile == that.endCharInFile
        && this.source.equals(that.source)
        && this.startLineNo == that.startLineNo
        && this.endLineNo == that.endLineNo
        && this.startCharInLine == that.startCharInLine
        && this.endCharInLine == that.endCharInLine
        );
  }

  @Override
  public int hashCode() {
    return (
        this.source.hashCode()
        ^ this.startCharInFile()
        ^ (this.endCharInFile - this.startCharInFile)
        );
  }
}
