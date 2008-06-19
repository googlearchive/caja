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
 * A CharProducer that wraps another CharProducer to provide a finite amount
 * of lookahead.
 *
 * @author mikesamuel@gmail.com
 */
class LookaheadCharProducer implements CharProducer {
  /**
   * The characters that have been read from p but not yet consumed are in
   * {@code lookahead[lookaheadPos : lookaheadLimit]}.
   */
  private final int[] lookahead;
  /** Positions corresponding to the lookehead chars. */
  private final MutableFilePosition[] lookaheadLoc;
  /**
   * The position of the first character in lookahead that has been read
   * from p but which has not yet been consumed.
   * If lookaheadLimit == lookaheadPos, then there is no such char.
   */
  private int lookaheadPos;
  /**
   * The position past the last character in lookahead that has been read
   * from p but which has not yet been consumed.
   */
  private int lookaheadLimit;
  /** The underlying char producer. */
  private final CharProducer p;

  LookaheadCharProducer(CharProducer p, int lookaheadLength) {
    this.lookahead = new int[lookaheadLength];
    this.lookaheadLoc = new MutableFilePosition[lookaheadLength];
    for (int i = lookaheadLoc.length; --i >= 0;) {
      lookaheadLoc[i] = new MutableFilePosition();
    }
    this.p = p;
  }

  public FilePosition getCurrentPosition() {
    FilePosition fp;
    if (lookaheadLimit == lookaheadPos) {
      fp = p.getCurrentPosition();
    } else {
      fp = lookaheadLoc[lookaheadPos].toFilePosition();
    }
    return fp;
  }

  public boolean getCurrentPosition(MutableFilePosition posBuf) {
    boolean result;
    if (lookaheadLimit == lookaheadPos) {
      result = p.getCurrentPosition(posBuf);
    } else {
      lookaheadLoc[lookaheadPos].copyTo(posBuf);
      result = true;
    }
    return result;
  }

  public void close() throws IOException {
    p.close();
    lookaheadLimit = lookaheadPos = 0;
  }

  public int read() throws IOException {
    if (lookaheadPos == lookaheadLimit) { fetch(1); }
    return lookahead[lookaheadPos++];
  }

  /** Returns the i-th character after the current without consuming it. */
  int peek(int i) {
    assert lookaheadPos + i < lookaheadLimit;
    return lookahead[lookaheadPos + i];
  }

  /**
   * Make the last character read or consumed available for reading again.
   * @throws IllegalStateException if the last character was not fetched before
   *     reading, or fetch has been called since it was read.
   */
  void pushback() {
    if (lookaheadPos <= 0) { throw new IllegalStateException(); }
   --lookaheadPos;
  }

  /** The numer of tokens available to {@link #peek} and {@link #consume} */
  int limit() {
    return lookaheadLimit - lookaheadPos;
  }

  /**
   * Makes sure at least one character is in the lookahead buffer and returns
   * the first character in the lookahead buffer without consuming it.
   */
  int lookahead() throws IOException {
    fetch(1);
    return lookahead[lookaheadPos];
  }

  /** Consume n characters from the lookahead buffer. */
  void consume(int nChars) {
    assert lookaheadPos + nChars <= lookaheadLimit;
    lookaheadPos += nChars;
  }

  /**
   * Increase the limit to at least n by fetching n characters into the
   * lookahead buffer.
   */
  void fetch(int n) throws IOException {
    int limit;
    if (lookaheadLimit == lookaheadPos) {
      lookaheadLimit = lookaheadPos = 0;
      limit = n;
    } else {
      int nInLookahead = lookaheadLimit - lookaheadPos;
      if (lookaheadPos + n > lookahead.length) {
        // need more space, so shift stuff back
        System.arraycopy(lookahead, lookaheadPos, lookahead, 0, nInLookahead);
        System.arraycopy(lookaheadLoc, lookaheadPos, lookaheadLoc, 0,
                         nInLookahead);
        lookaheadLimit = nInLookahead;
        lookaheadPos = 0;
        limit = n;
      } else {
        limit = lookaheadPos + n;
      }
    }
    while (lookaheadLimit < limit) {
      p.getCurrentPosition(lookaheadLoc[lookaheadLimit]);
      lookahead[lookaheadLimit++] = p.read();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("pos=").append(lookaheadPos).append(", limit=")
        .append(lookaheadLimit).append('[');
    for (int i = 0; i < lookahead.length; ++i) {
      if (i != 0) { sb.append(", "); }
      sb.append(lookahead[i]);
    }
    return sb.append(']').toString();
  }
}
