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

import java.io.IOException;

/**
 * A CharProducer that provides a single character of lookahead.
 *
 * @author mikesamuel@gmail.com
 */
final class LookaheadCharProducer implements CharProducer {

  private final CharProducer cp;
  private int lookahead = -1;
  private CharProducer.MutableFilePosition lookaheadPos =
    new CharProducer.MutableFilePosition();

  static LookaheadCharProducer create(CharProducer cp) {
    return (cp instanceof LookaheadCharProducer)
      ? (LookaheadCharProducer) cp
      : new LookaheadCharProducer(cp);
  }

  private LookaheadCharProducer(CharProducer cp) {
    assert null != cp;
    this.cp = cp;
  }

  /** Looks at the next character without consuming it. */
  public int lookahead() throws IOException {
    if (lookahead < 0) {
      cp.getCurrentPosition(lookaheadPos);
      lookahead = cp.read();
    }
    return lookahead;
  }

  public int read() throws IOException {
    int ch;
    if (lookahead < 0) {
      ch = cp.read();
    } else {
      ch = lookahead;
      lookahead = -1;
    }
    return ch;
  }

  public void close() throws IOException {
    cp.close();
    lookahead = -1;
  }

  public boolean getCurrentPosition(CharProducer.MutableFilePosition out) {
    if (lookahead < 0) {
      return cp.getCurrentPosition(out);
    } else {
      lookaheadPos.copyTo(out);
      return true;
    }
  }

  public FilePosition getCurrentPosition() {
    if (lookahead < 0) {
      return cp.getCurrentPosition();
    } else {
      return lookaheadPos.toFilePosition();
    }
  }
}
