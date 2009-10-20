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

package com.google.caja.ancillary.linter;

import com.google.caja.lexer.CharProducer;

/**
 * Presents a CharProducer as a CharSequence,
 *
 * @author mikesamuel@gmail.com
 */
final class FileContent implements CharSequence {
  private final CharProducer cp;
  private final int offset;
  private final int length;

  FileContent(CharProducer cp) {
    this(cp.clone(), cp.getOffset(), cp.getLength());
  }

  private FileContent(CharProducer cp, int offset, int length) {
    this.cp = cp;
    this.offset = offset;
    this.length = length;
  }

  public char charAt(int index) {
    if (index < 0 || index >= length) { throw new IndexOutOfBoundsException(); }
    return cp.getBuffer()[index + offset];
  }

  public int length() {
    return length;
  }

  public CharSequence subSequence(int start, int end) {
    return new FileContent(cp, offset + start, end - start);
  }
}
