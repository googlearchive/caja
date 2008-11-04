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
 * A char producer that presents a file with line continuations, backslashes
 * ('\') that negate a following newline, as a single unbroken stream of
 * characters.
 *
 * <p>This is an undocumented feature of javascript that all major interpreters
 * implement.</p>
 *
 * <p>A backslash escapes a following backslash, so that if there is an even
 * number of backslashes before a newline, then the following newline is
 * considered significant.  Empirically, this agrees  with Firefox's and IE's
 * behavior, and I know of no interpreters that it doesn't agree with.
 *
 * @author mikesamuel@gmail.com
 */
final class LineContinuingCharProducer implements CharProducer {
  private final CharProducer p;
  private int lookahead = -1;
  private final MutableFilePosition lookaheadPos = new MutableFilePosition();
  private final MutableFilePosition last = new MutableFilePosition();
  private final MutableFilePosition last2 = new MutableFilePosition();
  private boolean tokenBreak = false;

  LineContinuingCharProducer(CharProducer p) {
    this.p = p;
  }

  public int read() throws IOException {
    if (lookahead >= 0) {
      int ch = lookahead;
      lookahead = -1;
      lookaheadPos.copyTo(last);
      return ch;
    }
    tokenBreak = false;
    p.getCurrentPosition(last);
    do {
      int ch = p.read();
      if ('\\' != ch) {
        return ch;
      }
      p.getCurrentPosition(lookaheadPos);
      int ch2 = p.read();
      if (ch2 >= 0) {
        if (JsLexer.isJsLineSeparator((char) ch2)) {
          // Make sure that post advanceLast, the last position is on the start
          // of the new line (following the newline)
          p.getCurrentPosition(last2);
          tokenBreak = true;
          continue;
        }
        this.lookahead = ch2;
      }
      return ch;
    } while (true);
  }

  public FilePosition getCurrentPosition() {
    if (lookahead < 0) {
      return p.getCurrentPosition();
    } else {
      return lookaheadPos.toFilePosition();
    }
  }

  public FilePosition getLastPosition() {
    return null != last ? last.toFilePosition() : null;
  }

  public boolean getCurrentPosition(MutableFilePosition posBuf) {
    if (lookahead < 0) {
      return p.getCurrentPosition(posBuf);
    } else {
      lookaheadPos.copyTo(posBuf);
      return true;
    }
  }

  public void pushback(int ch) {
    this.lookahead = ch;
    last.copyTo(lookaheadPos);
  }

  public void advanceLast() {
    if (tokenBreak) {
      last2.copyTo(lookahead >= 0 ? lookaheadPos : last);
    }
  }

  public boolean tokenBreak() { return tokenBreak; }

  public void close() throws IOException { p.close(); }
}
