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

import java.io.Serializable;

/**
 * Encapsulates an {@link InputSource} and the positions of newlines in that
 * source file.
 *
 * @author mikesamuel@gmail.com
 */
public final class SourceBreaks implements Serializable {
  private static final long serialVersionUID = 7399048719164090106L;
  private final InputSource src;
  private int nLines;
  private boolean immutable = false;
  /**
   * The first {@link #nLines} elements are a sorted array of code-unit indices
   * corresponding to the position at which a line ends.  A line ends after the
   * line-break character that ends it.  A line which ends with
   * a CRLF sequence is considered as ending after the LF.  The last line in a
   * file need never end.
   * There are no zero-length lines.
   */
  private int[] lineNums = new int[1024];
  /** The line-number on which character zero falls. */
  private int lineNumberZero;

  public SourceBreaks(InputSource src, int lineNumberZero) {
    this.src = src;
    this.lineNumberZero = lineNumberZero;
  }

  public InputSource source() { return src; }

  int charInLineAt(int charInFile) {
    int lineno = lineAt(charInFile) - lineNumberZero;
    if (lineno == 0) { return charInFile; }
    return charInFile - lineNums[lineno - 1] + 1;
  }

  private int cachedCharInFile = -1, cachedAnswer = -1;
  int lineAt(int charInFile) {
    if (cachedCharInFile == charInFile) {
      return cachedAnswer + lineNumberZero;
    }
    cachedCharInFile = charInFile;

    int lineNum = boundedBinarySearch(lineNums, charInFile, nLines);
    if (lineNum < 0) {
      lineNum = ~lineNum;
    } else {
      ++lineNum;
    }

    return (cachedAnswer = lineNum) + lineNumberZero;
  }

  public FilePosition toFilePosition(int charInFile) {
    return new FilePosition(this, charInFile, 0);
  }

  public FilePosition toFilePosition(int startCharInFile, int endCharInFile) {
    return new FilePosition(
        this, startCharInFile, endCharInFile - startCharInFile);
  }

  /**
   * Add a line break.
   * @param charInFile the index of the first character of the new line,
   *     or if the new line has no characters, the index that would be the
   *     first character if it had any.
   *     This must be strictly greater than any value previously passed to
   *     this method for this instance.
   */
  public void lineStartsAt(int charInFile) {
    if (immutable) {
      throw new UnsupportedOperationException();
    }
    // TODO(jasvir): Issue 1502
    //assert nLines == 0 || charInFile > lineNums[nLines - 1];
    if (nLines == lineNums.length) {
      int[] newLineNums = new int[nLines * 2];
      System.arraycopy(lineNums, 0, newLineNums, 0, nLines);
      lineNums = newLineNums;
    }
    lineNums[nLines++] = charInFile;
    cachedCharInFile = -1;
  }

  public void makeImmutable() {
    this.immutable = true;
  }

  public boolean isImmutable() {
    return immutable;
  }

  /**
   * Like {@link java.util.Arrays#binarySearch} but doesn't assume the entire
   * array is full.
   *
   * @param arr array to be searched
   * @param target target the value to be searched for
   * @param limit index of the last element to be included in the search
   * @return index of the target, if it is contained in the array;
   *    otherwise, a negative value.
   */
  private static int boundedBinarySearch(int[] arr, int target, int limit) {
    int lo = 0;
    int hi = limit - 1;
    while (lo <= hi) {
      int midpoint = (lo + hi) >>> 1;
      int el = arr[midpoint];
      if (target > el) {
        lo = midpoint + 1;
      } else if (target < el) {
        hi = midpoint - 1;
      } else {
        return midpoint;
      }
    }
    return ~lo;
  }
}
