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

/**
 * A {@link TokenConsumer} that adds the tokens, possibly with changes to
 * intervening whitespace or normalization of tokens to a simpler TokenConsumer
 * such as a {@link Concatenator} that dumps tokens to an output buffer.
 *
 * @author mikesamuel@gmail.com
 */
abstract class AbstractRenderer implements TokenConsumer {
  protected final TokenConsumer out;

  /**
   * @param out receives the rendered text.
   */
  AbstractRenderer(TokenConsumer out) {
    this.out = out;
  }

  public final void noMoreTokens() {
    out.noMoreTokens();
  }

  /**
   * @throws NullPointerException if out raises an IOException
   *     and ioExceptionHandler is null.
   */
  public abstract void consume(String text);
}
