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
  /** Are non-negative numeric quantities allowed? */
  QUANTITY(1),
  /** Are hash color literals allowed. */
  HASH_VALUE(2),
  /** Are negative numeric quantities allowed? */
  NEGATIVE_QUANTITY(4),
  /** Are quoted strings allowed. */
  QSTRING(8),
  /** Are URLs allowed. */
  URL(16),
  /**
   * Allowed to be read via computed style directly without computing the style
   * as if the link were not visited.
   */
  HISTORY_INSENSITIVE(32),
  /**
   * Can identifiers that are not reserved words be treated as their literal
   * text instead of being treated as symbols.
   */
  UNRESERVED_WORD(64),
  /** Are unicode ranges allowed? */
  UNICODE_RANGE(128),
  /**
   * Allowed to be specified in a history-sensitive manner in a CSS stylesheet.
   */
  ALLOWED_IN_LINK(256),
  /**
   * Non-keyword terms treated as global names that need to be namespaced.
   */
  GLOBAL_NAME(512),
  /**
   * Non-keyword terms treated as property names that need to match an allowed
   * property in the schema.
   */
  PROPERTY_NAME(1024),
  ;

  /** a single bit. */
  public final int jsValue;

  CssPropBit(int jsValue) { this.jsValue = jsValue; }
}
