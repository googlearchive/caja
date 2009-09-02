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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    return new ArrayListMultimap<K, V>(new HashMap<K, List<V>>());
  }
  /**
   * Creates an empty Multimap whose values are stored in {@link List}s, and
   * whose key order is insertion order.
   */
  public static <K, V>
  Multimap<K, V> newListLinkedHashMultimap() {
    return new ArrayListMultimap<K, V>(new LinkedHashMap<K, List<V>>());
  }
  /**
   * Creates an empty Multimap whose values are stored in {@link List}s, and
   * whose keys are compared using object identity.
   */
  public static <K, V>
  Multimap<K, V> newListIdentityMultimap() {
    return new ArrayListMultimap<K, V>(new IdentityHashMap<K, List<V>>());
  }
  /**
   * Creates an empty Multimap whose values are stored in hash {@link Set}s.
   */
  public static <K, V>
  Multimap<K, V> newSetHashMultimap() {
    return new LinkedHashSetMultimap<K, V>(new HashMap<K, Set<V>>());
  }
  /**
   * Creates an empty Multimap whose values are stored in hash {@link Set}s, and
   * whose keys order is insertion order.
   */
  public static <K, V>
  Multimap<K, V> newSetLinkedHashMultimap() {
    return new LinkedHashSetMultimap<K, V>(new LinkedHashMap<K, Set<V>>());
  }
  /**
   * Creates an empty Multimap whose values are stored in hash {@link Set}s, and
   * whose keys are compared using object identity.
   */
  public static <K, V>
  Multimap<K, V> newSetIdentityMultimap() {
    return new LinkedHashSetMultimap<K, V>(new IdentityHashMap<K, Set<V>>());
  }

  // Visible for testing
  static abstract class AbstractMultimap<K, V, C extends Collection<V>>
      implements Multimap<K, V> {
    /** Maps to non-empty collections produced by {@link #makeCollection}. */
    private final Map<K, C> underlying;

    AbstractMultimap(Map<K, C> underlying) {
      this.underlying = underlying;
    }

    /** Makes an instance for a value collection. */
    abstract C makeCollection();

    @Override
    public Collection<V> get(K k) {
      C c = underlying.get(k);
      return c != null
          ? Collections.unmodifiableCollection(c)
          : Collections.<V>emptySet();
    }

    @Override
    public boolean isEmpty() { return underlying.isEmpty(); }

    @Override
    public Set<K> keySet() { return underlying.keySet(); }

    @Override
    public boolean put(K k, V v) {
      C c = underlying.get(k);
      boolean result;
      if (c == null) {
        c = makeCollection();
        result = c.add(v);
        if (!c.isEmpty()) { underlying.put(k, c); }
      } else {
        result = c.add(v);
      }
      return result;
    }

    @Override
    public void putAll(K k, Collection<? extends V> v) {
      if (v.isEmpty()) { return; }
      C c = underlying.get(k);
      if (c == null) {
        c = makeCollection();
        c.addAll(v);
        if (!c.isEmpty()) { underlying.put(k, c); }
      } else {
        c.addAll(v);
      }
    }

    @Override
    public boolean remove(K k, V v) {
      C c = underlying.get(k);
      if (c == null) { return false; }
      boolean result = c.remove(v);
      if (c.isEmpty()) { underlying.remove(k); }
      return result;
    }

    @Override
    public void removeAll(K k, Collection<? extends V> v) {
      C c = underlying.get(k);
      if (c != null) {
        c.removeAll(v);
        if (c.isEmpty()) { underlying.remove(k); }
      }
    }
  }

  private static final class ArrayListMultimap<K, V>
      extends AbstractMultimap<K, V, List<V>> {
    ArrayListMultimap(Map<K, List<V>> underlying) { super(underlying); }

    @Override
    List<V> makeCollection() { return new ArrayList<V>(); }
  }

  private static final class LinkedHashSetMultimap<K, V>
      extends AbstractMultimap<K, V, Set<V>> {
    LinkedHashSetMultimap(Map<K, Set<V>> underlying) { super(underlying); }

    @Override
    Set<V> makeCollection() { return new LinkedHashSet<V>(); }
  }
}
