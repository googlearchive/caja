// Copyright (C) 2006 Google Inc.
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
 * CSS 2 token types.
 *
 * @author mikesamuel@gmail.com
 */
public enum CssTokenType implements TokenType {
  SPACE,
  COMMENT,
  PUNCTUATION,
  STRING,
  IDENT,
  HASH,
  SYMBOL, // @keyword
  DIRECTIVE, // !keyword
  QUANTITY, // ems, exs, length, angle, time, etc.
  URI,
  FUNCTION,
  UNICODE_RANGE,
  /**
   * Not part of the CSS lexical grammar, but in some parsing modes, matches
   * a <code>$(...)</code> style substitution where the ellipsis allows any
   * run of non-comment javascript tokens with matched parentheses.
   * The token may have a unit-suffix such as px, em, %, etc.
   */
  SUBSTITUTION,
  ;
}
