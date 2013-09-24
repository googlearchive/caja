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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shortcuts for creating {@link Multimap}s.
 * Inspired by
 * <a href="http://code.google.com/p/google-collections/">Google Collections</a>
 * but does not use any type suppressions.
 *
 * @author mikesamuel@gmail.com
 */
public class Multimaps {
  /** Creates an empty Multimap whose values are stored in {@link List}s. */
  public static <K, V>
  Multimap<K, V> newListHashMultimap() {
    return new MultimapImpl<K, V, List<V>>(
        new HashMapMaker<K, List<V>>(), new ListMaker<V>());
  }
  /**
   * Creates an empty Multimap whose values are stored in {@link List}s, and
   * whose key order is insertion order.
   */
  public static <K, V>
  Multimap<K, V> newListLinkedHashMultimap() {
    return new MultimapImpl<K, V, List<V>>(
        new LinkedHashMapMaker<K, List<V>>(), new ListMaker<V>());
  }
  /**
   * Creates an empty Multimap whose values are stored in {@link List}s, and
   * whose keys are compared using object identity.
   */
  public static <K, V>
  Multimap<K, V> newListIdentityMultimap() {
    return new MultimapImpl<K, V, List<V>>(
        new IdentityHashMapMaker<K, List<V>>(), new ListMaker<V>());
  }
  /**
   * Creates an empty Multimap whose values are stored in hash {@link Set}s.
   */
  public static <K, V>
  Multimap<K, V> newSetHashMultimap() {
    return new MultimapImpl<K, V, Set<V>>(
        new HashMapMaker<K, Set<V>>(), new SetMaker<V>());
  }
  /**
   * Creates an empty Multimap whose values are stored in hash {@link Set}s, and
   * whose keys order is insertion order.
   */
  public static <K, V>
  Multimap<K, V> newSetLinkedHashMultimap() {
    return new MultimapImpl<K, V, Set<V>>(
        new LinkedHashMapMaker<K, Set<V>>(), new SetMaker<V>());
  }
  /**
   * Creates an empty Multimap whose values are stored in hash {@link Set}s, and
   * whose keys are compared using object identity.
   */
  public static <K, V>
  Multimap<K, V> newSetIdentityMultimap() {
    return new MultimapImpl<K, V, Set<V>>(
        new IdentityHashMapMaker<K, Set<V>>(), new SetMaker<V>());
  }

  static interface Maker<T> {
    T newInstance();
  }

  private static class HashMapMaker<K, V> implements Maker<Map<K, V>> {
    public Map<K, V> newInstance() { return Maps.newHashMap(); }
  }

  private static class LinkedHashMapMaker<K, V> implements Maker<Map<K, V>> {
    public Map<K, V> newInstance() { return Maps.newLinkedHashMap(); }
  }

  private static class IdentityHashMapMaker<K, V> implements Maker<Map<K, V>> {
    public Map<K, V> newInstance() { return Maps.newIdentityHashMap(); }
  }

  private static class ListMaker<T> implements Maker<List<T>> {
    public List<T> newInstance() { return Lists.newArrayList(); }
  }

  private static class SetMaker<T> implements Maker<Set<T>> {
    public Set<T> newInstance() { return Sets.newLinkedHashSet(); }
  }

  // Visible for testing
  static final class MultimapImpl<K, V, C extends Collection<V>>
      implements Multimap<K, V> {
    private final Maker<Map<K, C>> mapMaker;
    private final Maker<C> collectionMaker;
    /** Maps to non-empty collections produced by {@link #collectionMaker}. */
    private final Map<K, C> underlying;

    MultimapImpl(Maker<Map<K, C>> mapMaker, Maker<C> collectionMaker) {
      this.mapMaker = mapMaker;
      this.collectionMaker = collectionMaker;
      this.underlying = mapMaker.newInstance();
    }

    @Override
    public final Multimap<K, V> clone() {
      MultimapImpl<K, V, C> clone = new MultimapImpl<K, V, C>(
          mapMaker, collectionMaker);
      clone.underlying.putAll(this.underlying);
      for (Map.Entry<K, C> e : clone.underlying.entrySet()) {
        C c = collectionMaker.newInstance();
        c.addAll(e.getValue());
        e.setValue(c);
      }
      return clone;
    }

    public Collection<V> get(K k) {
      C c = underlying.get(k);
      return c != null
          ? Collections.unmodifiableCollection(c)
          : Collections.<V>emptySet();
    }

    public boolean isEmpty() { return underlying.isEmpty(); }

    public Set<K> keySet() { return underlying.keySet(); }

    public boolean put(K k, V v) {
      C c = underlying.get(k);
      boolean result;
      if (c == null) {
        c = collectionMaker.newInstance();
        result = c.add(v);
        if (!c.isEmpty()) { underlying.put(k, c); }
      } else {
        result = c.add(v);
      }
      return result;
    }

    public void putAll(K k, Collection<? extends V> v) {
      if (v.isEmpty()) { return; }
      C c = underlying.get(k);
      if (c == null) {
        c = collectionMaker.newInstance();
        c.addAll(v);
        if (!c.isEmpty()) { underlying.put(k, c); }
      } else {
        c.addAll(v);
      }
    }

    public boolean remove(K k, V v) {
      C c = underlying.get(k);
      if (c == null) { return false; }
      boolean result = c.remove(v);
      if (c.isEmpty()) { underlying.remove(k); }
      return result;
    }

    public void removeAll(K k, Collection<? extends V> v) {
      C c = underlying.get(k);
      if (c != null) {
        c.removeAll(v);
        if (c.isEmpty()) { underlying.remove(k); }
      }
    }
  }
}
