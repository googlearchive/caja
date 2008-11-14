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

import com.google.caja.lexer.TokenConsumer;
import com.google.caja.util.Callback;

import java.io.Flushable;
import java.io.IOException;

/**
 * A {@link TokenConsumer} that adds the tokens, possibly with intervening
 * whitespace to an {@link Appendable} stream.
 *
 * @author mikesamuel@gmail.com
 */
abstract class AbstractRenderer implements TokenConsumer {
  protected final Appendable out;
  private final Callback<IOException> ioExceptionHandler;
  /** True if an IOException has been raised. */
  private boolean closed;

  /**
   * @param out receives the rendered text.
   * @param ioExceptionHandler receives exceptions thrown by out.
   */
  AbstractRenderer(Appendable out, Callback<IOException> ioExceptionHandler) {
    this.out = out;
    this.ioExceptionHandler = ioExceptionHandler;
  }

  public final void noMoreTokens() {
    if (out instanceof Flushable) {
      try {
        ((Flushable) out).flush();
      } catch (IOException ex) {
        if (!closed) {
          closed = true;
          ioExceptionHandler.handle(ex);
        }
      }
    }
  }

  /**
   * Handles writing the given token to the output {@link Appendable}.
   * @throws IOException iff writing to out raises an IOException.
   */
  protected abstract void append(String text) throws IOException;

  /**
   * @throws NullPointerException if out raises an IOException
   *     and ioExceptionHandler is null.
   */
  public final void consume(String text) {
    if (closed) { return; }
    try {
      append(text);
    } catch (IOException ex) {
      closed = true;
      ioExceptionHandler.handle(ex);
    }
  }
}
