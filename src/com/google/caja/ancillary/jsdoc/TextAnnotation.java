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

package com.google.caja.ancillary.jsdoc;

import com.google.caja.lexer.FilePosition;
import com.google.caja.reporting.RenderContext;
import java.util.Collections;

/**
 * A chunk of comment text.
 * @author mikesamuel@gmail.com
 */
public final class TextAnnotation extends AbstractAnnotation {
  TextAnnotation(String text, FilePosition pos) {
    super(text, Collections.<Annotation>emptyList(), pos);
    if (text == null) { throw new NullPointerException(); }
  }

  public void render(RenderContext r) {
    r.getOut().mark(getFilePosition());
    r.getOut().consume(getValue());
  }

  public TextAnnotation slice(int start, int end) {
    return slice(getValue(), getFilePosition(), start, end);
  }

  public static TextAnnotation slice(
      String value, FilePosition pos, int start, int end) {
    int sline = pos.startLineNo(), scil = pos.startCharInLine();
    // The below may give incorrect results if the CharProducer that produced
    // this annotation does unescaping, but it should be close enough to produce
    // good error messages.
    for (int i = 0; i < start; ++i) {
      char ch = value.charAt(i);
      if (ch == '\r') {
        if (i + 1 >= value.length() || '\n' != value.charAt(i + 1)) { ++sline; }
        scil = 1;
      } else if (ch == '\n') {
        ++sline;
        scil = 1;
      } else {
        ++scil;
      }
    }
    return new TextAnnotation(
        value.substring(start, end),
        FilePosition.instance(
            pos.source(),
            sline, pos.startCharInFile() + start, scil, end - start));
  }
}
