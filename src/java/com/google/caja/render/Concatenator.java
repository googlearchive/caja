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

package com.google.caja.render;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.util.Callback;

import java.io.Flushable;
import java.io.IOException;

/**
 * A {@link com.google.caja.reporting.RenderContext} renderer that concatenates
 * tokens.  Suitable for rendering {@link com.google.caja.parser.html.DomTree}s.
 *
 * @author mikesamuel@gmail.com
 */
public final class Concatenator implements TokenConsumer {
  private final Appendable out;
  private final Callback<IOException> ioExceptionHandler;
  private boolean closed;

  /**
   * @param out receives the rendered text.
   * @param ioExceptionHandler receives exceptions thrown by out.
   */
  public Concatenator(
      Appendable out, Callback<IOException> ioExceptionHandler) {
    this.out = out;
    this.ioExceptionHandler = ioExceptionHandler;
  }

  public void noMoreTokens() {
    if (out instanceof Flushable) {
      try {
        ((Flushable) out).flush();
      } catch (IOException ex) {
        closed = true;
        if (!closed) {
          ioExceptionHandler.handle(ex);
        }
      }
    }
  }

  public void mark(FilePosition pos) {}

  /**
   * @throws NullPointerException if out raises an IOException
   *     and ioExceptionHandler is null.
   */
  public void consume(String text) {
    if (closed) { return; }
    try {
      out.append(text);
    } catch (IOException ex) {
      closed = true;
      ioExceptionHandler.handle(ex);
    }
  }
}
