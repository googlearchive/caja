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
 * Types of javascript tokens.
 *
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public enum JsTokenType implements TokenType {
  COMMENT,
  STRING,
  REGEXP,
  PUNCTUATION,
  WORD,
  KEYWORD,
  INTEGER,
  FLOAT,
  /**
   * An escape that occurs before a slash in a whitespace token as in
   * <pre>
   * foo \
   * bar
   * </pre>
   */
  LINE_CONTINUATION,
  ;
}
