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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Shortcuts for creating maps.
 * Inspired by
 * <a href="http://code.google.com/p/google-collections/">Google Collections</a>
 * but does not use any type suppressions.
 */
public final class Maps {
  public static <K, V>
  Map<K, V> newHashMap() {
    return new HashMap<K, V>();
  }

  public static <K, V>
  Map<K, V> newHashMap(Map<? extends K, ? extends V> map) {
    return new HashMap<K, V>(map);
  }

  public static <K, V>
  Map<K, V> newLinkedHashMap() {
    return new LinkedHashMap<K, V>();
  }

  public static <K, V>
  Map<K, V> newLinkedHashMap(Map<? extends K, ? extends V> map) {
    return new LinkedHashMap<K, V>(map);
  }

  public static <K, V>
  Map<K, V> newIdentityHashMap() {
    return new IdentityHashMap<K, V>();
  }

  public static <K, V>
  Map<K, V> newIdentityHashMap(Map<? extends K, ? extends V> map) {
    return new IdentityHashMap<K, V>(map);
  }

  public static <K extends Comparable<K>, V>
  SortedMap<K, V> newTreeMap() {
    return new TreeMap<K, V>();
  }

  public static <K extends Comparable<K>, V>
  SortedMap<K, V> newTreeMap(Map<? extends K, ? extends V> map) {
    return new TreeMap<K, V>(map);
  }

  public static <K, V>
  SortedMap<K, V> newTreeMap(Comparator<? super K> cmp) {
    return new TreeMap<K, V>(cmp);
  }

  public static <K, V>
  SortedMap<K, V> newTreeMap(
      Comparator<? super K> cmp, Map<? extends K, ? extends V> map) {
    SortedMap<K, V> m = new TreeMap<K, V>(cmp);
    m.putAll(map);
    return m;
  }

  public static <K, V> ImmutableMapBuilder<K, V> immutableMap() {
    return new ImmutableMapBuilder<K, V>(new LinkedHashMap<K, V>());
  }

  public static <K, V>
  ImmutableMapBuilder<K, V> immutableSortedMap(Comparator<? super K> keyCmp) {
    return new ImmutableMapBuilder<K, V>(new TreeMap<K, V>(keyCmp));
  }

  public static final class ImmutableMapBuilder<K, V> {
    private Map<K, V> map;
    ImmutableMapBuilder(Map<K, V> emptyMap) { this.map = emptyMap; }

    public ImmutableMapBuilder<K, V> put(K key, V value) {
      map.put(key, value);
      return this;
    }

    public ImmutableMapBuilder<K, V> putAll(Map<K, V> map) {
      map.putAll(map);
      return this;
    }

    public Map<K, V> create() {
      Map<K, V> map = this.map;
      if (map == null) { throw new IllegalStateException(); }
      this.map = null;
      return Collections.unmodifiableMap(map);
    }
  }

  private Maps() {}
}
