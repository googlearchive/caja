// Copyright (C) 2009 Google Inc.
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

import java.util.Collection;
import java.util.Set;

/**
 * A map like object where keys map to {@link Collection}s of values.
 *
 * @author mikesamuel@gmail.com
 */
public interface Multimap<K, V> extends Cloneable {
  /** The set of keys with a non empty value collection. */
  Set<K> keySet();
  /**
   * An immutable collection of all values put for k
   * and not subsequently removed.
   */
  Collection<V> get(K k);
  /** Adds the given value to the collection of values for the given key. */
  boolean put(K k, V v);
  void putAll(K k, Collection<? extends V> v);
  boolean remove(K k, V v);
  void removeAll(K k, Collection<? extends V> v);
  /** True if there are no values in the map. */
  boolean isEmpty();
  Multimap<K, V> clone();
}
