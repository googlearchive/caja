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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides a map with transactional semantics.
 * This map may be queried and modified, and the underlying map will not change
 * until {@link #commit} is performed.
 *
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public class TransactionSafeMap<K, V> implements Map<K, V> {

  /**
   * The only value not of type V in changes.  Indicates that an item has been
   * removed from wrapped.  Only keys in wrapped can have value TOMBSTONE.
   */
  private static final Object TOMBSTONE = new Object();

  private final Map<K, V> wrapped;
  private Map<K, Object> changes = null;
  /**
   * Set of keys removed from wrapped.  Needed to compute size efficiently.
   * Invariants
   * - deleted only contains keys in wrapped.
   * - k in deleted <-> changes[k] == TOMBSTONE
   */
  private Set<K> deleted = Collections.<K>emptySet();
  private int size;

  /**
   * @param m a map that is considered owned -- it must not be modified
   *    subsequently by the caller or any other code.
   */
  public TransactionSafeMap(Map<K, V> m) {
    if (null == m) { throw new NullPointerException(); }
    this.wrapped = m;
    this.size = this.wrapped.size();
  }

  @SuppressWarnings("unchecked")
  public V get(Object k) {
    if (null != changes && changes.containsKey(k)) {
      Object result = changes.get(k);
      return TOMBSTONE != result ? (V) result : null;
    } else {
      return wrapped.get(k);
    }
  }

  public boolean containsKey(Object k) {
    if (null != changes && changes.containsKey(k)) {
      return TOMBSTONE != changes.get(k);
    } else {
      return wrapped.containsKey(k);
    }
  }

  /** unsupported */
  public boolean containsValue(Object v) {
    throw new UnsupportedOperationException();
  }

  /** An immutable view of the keys in this map. */
  public Set<K> keySet() {
    if (null == changes) {
      return Collections.<K, V>unmodifiableMap(wrapped).keySet();
    }
    HashSet<K> keys = new HashSet<K>(wrapped.keySet());
    keys.addAll(changes.keySet());
    keys.removeAll(deleted);
    return Collections.<K>unmodifiableSet(keys);
  }

  /** An immutable view of this map's entries. */
  @SuppressWarnings("unchecked")
  public Set<Map.Entry<K, V>> entrySet() {
    if (null == changes) {
      return Collections.<K, V>unmodifiableMap(wrapped).entrySet();
    }
    Set<Map.Entry<K, V>> entries = new HashSet<Map.Entry<K, V>>();
    for (Map.Entry<K, V> e : wrapped.entrySet()) {
      entries.add(entry(e.getKey(), e.getValue()));
    }
    for (Map.Entry<K, Object> e : changes.entrySet()) {
      Object v = e.getValue();
      if (TOMBSTONE == v) {
        entries.remove(e.getKey());
      } else {
        entries.add(entry(e.getKey(), (V) e.getValue()));
      }
    }
    return Collections.<Map.Entry<K, V>>unmodifiableSet(entries);
  }

  private Map.Entry<K, V> entry(final K k, final V v) {
    return new Map.Entry<K, V>() {
      public K getKey() { return k; }
      public V getValue() { return v; }
      public V setValue(V _) { throw new UnsupportedOperationException(); }
      @Override
      public int hashCode() {
        return (getKey() == null ? 0 : getKey().hashCode()) ^
               (getValue() == null ? 0 : getValue().hashCode());
      }
      @Override
      public boolean equals(Object o) {
        if (!(o instanceof Map.Entry)) { return false; }
        Map.Entry<?, ?> that = (Map.Entry<?, ?>) o;
        return
          (this.getKey() == null ?
           that.getKey() == null : this.getKey().equals(that.getKey()))  &&
          (this.getValue() == null ?
           that.getValue() == null : this.getValue().equals(that.getValue()));
      }
    };
  }

  /** unsupported */
  public Collection<V> values() {
    throw new UnsupportedOperationException();
  }

  public void clear() {
    if (size == 0) { return; }
    if (null == changes) {
      changes = new HashMap<K, Object>();
    } else {
      changes.clear();
    }
    for (K k : wrapped.keySet()) { changes.put(k, TOMBSTONE); }
    this.size = 0;
  }

  @SuppressWarnings("unchecked")
  public V put(K k, V v) {
    V ov;
    if (null == changes || !changes.containsKey(k)) {
      if (wrapped.containsKey(k)) {
        ov = wrapped.get(k);
        if (v == ov) { return ov; }
      } else {
        ++size;
        ov = null;
      }
      if (null == changes) { changes = new HashMap<K, Object>(); }
    } else {
      Object o = changes.get(k);
      if (TOMBSTONE == o) {
        ++size;
        ov = null;
      } else {
        ov = (V) o;
      }
    }
    changes.put(k, v);
    return ov;
  }

  @SuppressWarnings("unchecked")
  public V remove(Object k) {
    if (null == changes || !changes.containsKey(k)) {
      if (wrapped.containsKey(k)) {
        V o = wrapped.get(k);
        if (null == changes) { changes = new HashMap<K, Object>(); }
        changes.put((K) k, TOMBSTONE);
        --size;
        return o;
      } else {
        return null;
      }
    } else {
      Object o = changes.get(k);
      if (TOMBSTONE == o) { return null; }
      changes.put((K) k, TOMBSTONE);
      --size;
      return (V) o;
    }
  }

  public void putAll(Map<? extends K, ? extends V> m) {
    for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }

  public boolean isEmpty() {
    return 0 == size;
  }

  public int size() {
    return size;
  }

  @SuppressWarnings("unchecked")
  public void commit() {
    if (null != changes) {
      for (Map.Entry<K, Object> e : changes.entrySet()) {
        K k = e.getKey();
        Object v = e.getValue();
        if (TOMBSTONE != v) {
          wrapped.put(k, (V) v);
        } else {
          wrapped.remove(k);
        }
      }
      changes = null;
    }
  }

  public void rollback() {
    changes = null;
    size = wrapped.size();
  }

}
