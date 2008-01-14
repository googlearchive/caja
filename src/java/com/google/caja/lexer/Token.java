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
 * A lexical token.
 *
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public final class Token<T extends TokenType> {
  public final String text;
  public final T type;
  public final FilePosition pos;

  public static <TT extends TokenType>
  Token<TT> instance(String text, TT type, FilePosition pos) {
    return new Token<TT>(text, type, pos);
  }

  private Token(String text, T type, FilePosition pos) {
    this.text = text;
    this.type = type;
    this.pos = pos;
  }

  @Override
  public String toString() { return text; }
}
