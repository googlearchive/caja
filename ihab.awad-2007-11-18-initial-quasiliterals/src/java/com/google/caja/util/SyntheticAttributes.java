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

package com.google.caja.util;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A set of attributes attached to a parse tree node that have been inferred by
 * the parser.
 *
 * @author mikesamuel@gmail.com
 */
public final class SyntheticAttributes
    extends AbstractMap<SyntheticAttributeKey<?>, Object> {

  private Map<SyntheticAttributeKey<?>, Object> attributes =
      new HashMap<SyntheticAttributeKey<?>, Object>();

  @SuppressWarnings("unchecked")
  public <T> T get(SyntheticAttributeKey<T> k) {
    return (T) attributes.get(k);
  }

  @Override
  public Object get(Object k) {
    return attributes.get(k);
  }

  /**
   * associate the value v with the key k.
   * @param k non null.
   * @param v non null.
   * @return the old value associated with k or null if none.
   */
  @SuppressWarnings("unchecked")
  public <T> T set(SyntheticAttributeKey<T> k, T v) {
    if (!(null == v || k.getType().isInstance(v))) {
      throw new ClassCastException(v + " to " + k.getType());
    }
    return (T) attributes.put(k, v);
  }

  @Deprecated
  @Override
  public Object put(SyntheticAttributeKey<?> k, Object v) {
    if (!(null == v || k.getType().isInstance(v))) {
      throw new ClassCastException(v + " to " + k.getType());
    }
    return attributes.put(k, v);
  }

  /**
   * @return true iff the value associated with the given key is
   * {@link Boolean#TRUE}.
   */
  public boolean is(SyntheticAttributeKey<Boolean> k) {
    return Boolean.TRUE.equals(attributes.get(k));
  }

  /**
   * @see #remove(Object)
   */
  @SuppressWarnings("unchecked")
  public <T> T remove(SyntheticAttributeKey<T> k) {
    return (T) attributes.remove(k);
  }

  @Override
  public Object remove(Object k) { return attributes.remove(k); }

  @Override
  public int size() { return attributes.size(); }

  @Override
  public boolean containsKey(Object k) { return attributes.containsKey(k); }

  @Override
  public boolean containsValue(Object v) { return attributes.containsValue(v); }

  /**
   * @return an immutable entry set to force proper type checking of keys
   *     and values.  Mutability could be implemented, but it's not currently
   *     used.
   */
  @Override
  public Set<Map.Entry<SyntheticAttributeKey<?>, Object>> entrySet() {
    return Collections.unmodifiableMap(attributes).entrySet();
  }
}
