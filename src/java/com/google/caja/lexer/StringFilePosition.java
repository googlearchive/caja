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

/**
 * Modified version of FilePosition.
 *
 * <p>TODO(ihab): Delete and replaced with an InputSource with a content: URI.
 *
 * @author rdub@google.com (Ryan Williams)
 */
public final class StringFilePosition extends FilePosition {

  private StringInputSource stringSource;

  // These store how far this String is from the beginning of its original
  // resting place, and are factored into the calculations so that absolute line
  // and char numbers presented to the user will be correct.
  // For example, if we get an error at a:b in the contents of a <script> tag
  // that actually began at at c:d of the original input, we want to tell the
  // user that they have an error at (a+c):b (or c:(b+d) if a == 1, i.e. the
  // error occurred on the first line of the <script> tag which was already
  // a distance of d from the start of the line.
  private int lineDeltaFromFile, charDeltaInLineFromFile;

  private StringFilePosition(StringInputSource source, int startCharInFile,
      int endCharInFile, int lineDeltaFromFile, int charDeltaInLineFromFile) {
    super(source, 1, 1, startCharInFile, 1, 1, 1, endCharInFile, 1);
    stringSource = source;
    this.lineDeltaFromFile = lineDeltaFromFile;
    this.charDeltaInLineFromFile = charDeltaInLineFromFile;
    computeLineInfo();
  }

  public StringFilePosition(FilePosition pos) throws ParseException {
    this(new StringInputSource(pos.readSource()),
        1,
        pos.length(),
        pos.startLineNo - 1,
        pos.startCharInLine);
  }

  public StringFilePosition(StringInputSource stringSource) {
    this(stringSource, 1, stringSource.getContent().length(), 0, 0);
  }

  private void computeLineInfo() {
    int pos = 0;
    int lineNo = 1;
    int lastLinePos = -1;
    for (; pos < this.startCharInFile; pos++) {
      if (stringSource.charAt(pos) == '\n') {
        lineNo++;
        lastLinePos = pos;
      }
    }
    this.startLineNo = lineNo + lineDeltaFromFile;
    this.startCharInLine =
        (lastLinePos == -1 ? charDeltaInLineFromFile : pos - lastLinePos);
    for (; pos < this.endCharInFile; pos++) {
      if (stringSource.charAt(pos) == '\n') {
        lineNo++;
        lastLinePos = pos;
      }
    }
    this.endLineNo = lineNo + lineDeltaFromFile;
    this.endCharInLine = pos - lastLinePos;
  }

  public String read() {
    return stringSource.getContent().substring(startCharInFile - 1,
        endCharInFile);
  }

  @Override
  public StringInputSource source() {
    return stringSource;
  }
}