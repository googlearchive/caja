// Copyright (C) 2007 Google Inc.
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

import java.util.NoSuchElementException;

/**
 * A TokenStream that lazily fetches one token at a time.
 *
 * @author msamuel@gmail.com (Mike Samuel)
 */
abstract class AbstractTokenStream<T extends TokenType>
    implements TokenStream<T> {
  private Token<T> tok;

  public final boolean hasNext() throws ParseException {
    if (tok == null) { tok = produce(); }
    return tok != null;
  }

  public Token<T> next() throws ParseException {
    if (this.tok == null) { this.tok = produce(); }
    Token<T> t = this.tok;
    if (t == null) { throw new NoSuchElementException(); }
    this.tok = null;
    return t;
  }

  protected abstract Token<T> produce() throws ParseException;
}
