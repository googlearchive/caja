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

import java.io.Serializable;
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
    extends AbstractMap<SyntheticAttributeKey<?>, Object>
    implements Serializable {
  private static final long serialVersionUID = 1124793823020078634L;
  /**
   * True iff this has its own copy without clobbering another maps attributes.
   * The copy constructor below does copy-on-write of the underlying map.
   */
  private boolean hasOwnCopy;
  private Map<SyntheticAttributeKey<?>, Object> attributes;
  private boolean immutable = false;

  public SyntheticAttributes() {
    clear();
  }

  public SyntheticAttributes(SyntheticAttributes sa) {
    attributes = sa.attributes;
    immutable = sa.immutable;
    sa.hasOwnCopy = false;
  }

  public void makeImmutable() {
    if (immutable) { return; }
    requireOwnCopy();
    immutable = true;
  }

  public boolean isImmutable() {
    return immutable;
  }

  @Override
  public void clear() {
    if (immutable) {
      throw new UnsupportedOperationException();
    }
    attributes = Collections.emptyMap();
    hasOwnCopy = false;
  }

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
    if (immutable) {
      throw new UnsupportedOperationException();
    }
    if (!(null == v || k.getType().isInstance(v))) {
      throw new ClassCastException(v + " to " + k.getType());
    }
    requireOwnCopy();
    return (T) attributes.put(k, v);
  }

  @Deprecated
  @Override
  public Object put(SyntheticAttributeKey<?> k, Object v) {
    if (immutable) {
      throw new UnsupportedOperationException();
    }
    if (!(null == v || k.getType().isInstance(v))) {
      throw new ClassCastException(v + " to " + k.getType());
    }
    requireOwnCopy();
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
    if (immutable) {
      throw new UnsupportedOperationException();
    }
    return (T) remove((Object) k);
  }

  @Override
  public Object remove(Object k) {
    if (immutable) {
      throw new UnsupportedOperationException();
    }
    if (!hasOwnCopy) {
      if (attributes.isEmpty()) { return null; }
      requireOwnCopy();
    }
    return attributes.remove(k);
  }

  private void requireOwnCopy() {
    if (!hasOwnCopy) {
      attributes = new HashMap<SyntheticAttributeKey<?>, Object>(attributes);
      hasOwnCopy = true;
    }
  }

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
