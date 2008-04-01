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

package com.google.caja.plugin;

/**
 * The types that a css term can take.
 *
 * @author mikesamuel@gmail.com
 */
public enum CssPropertyPartType {
  ANGLE,
  COLOR,
  FREQUENCY,
  IDENT,
  LENGTH,
  INTEGER,
  /**
   * A word that should be quoted to disambiguate it.
   * A series of loose words separated by spaces is considered equivalent to
   * a quoted string containing the words separated by a single space (0x20)
   * character.
   * <p>
   * From <a href="http://www.w3.org/TR/CSS21/fonts.html#font-family-prop">
   * CSS2.1 S15.3</a>,
   * <blockquote>Font family names that happen to be the same as a
   * keyword value (e.g. 'initial', 'inherit', ...) must be
   * quoted to prevent confusion with the keywords with the same
   * names.</blockquote>
   *
   * @see com.google.caja.lang.css.CssSchema#isKeyword
   */
  LOOSE_WORD,
  NUMBER,
  PERCENTAGE,
  SPECIFIC_VOICE,
  STRING,
  TIME,
  URI,
  ;
}
