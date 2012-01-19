// Copyright (C) 2011 Google Inc.
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

package com.google.caja.lang.css;

/**
 * A bit in a bitfield describing a CSS property.
 *
 * @author Mike Samuel <mikesamuel@gmail.com>
 */
public enum CssPropBit {
  QUANTITY(1),
  HASH_VALUE(2),
  NEGATIVE_QUANTITY(4),
  QSTRING_CONTENT(8),
  QSTRING_URL(16),
  HISTORY_INSENSITIVE(32),
  ;

  /** a single bit. */
  public final int jsValue;

  CssPropBit(int jsValue) { this.jsValue = jsValue; }
}
