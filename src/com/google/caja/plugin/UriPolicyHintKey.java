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

package com.google.caja.plugin;

import com.google.caja.parser.html.AttribKey;
import com.google.caja.util.Name;

import java.util.Map;

/**
 * Common values for the type hint keys used in the {@code hints} parameter to
 * {@link UriPolicy#rewriteUri}.
 *
 * @author MikeSamuel <mikesamuel@gmail.com>
 */
public final class UriPolicyHintKey<T> {
  public final Class<T> valueType;
  public final String key;

  private UriPolicyHintKey(Class<T> valueType, String key) {
    this.valueType = valueType;
    this.key = key;
  }

  private static <T> UriPolicyHintKey<T> inst(
      Class<T> valueType, String key) {
    return new UriPolicyHintKey<T>(valueType, key);
  }

  /** A {@link CssValidator#CSS_PROPERTY_PART CSS property part}. */
  public static final UriPolicyHintKey<Name> CSS_PROP
      = inst(Name.class, "CSS_PROP");

  /**
   * An {@link AttribKey} describing the HTML attribute in whose value the
   * URI appears.
   */
  public static final UriPolicyHintKey<String> XML_ATTR
      = inst(String.class, "XML_ATTR");

  public T valueFrom(Map<String, ?> hints) {
    return valueType.cast(hints.get(key));
  }

  @Override public String toString() { return key; }
}
