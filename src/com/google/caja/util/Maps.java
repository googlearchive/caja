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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Shortcuts for creating maps.
 * Use com.google.common.collect.Maps from Guava instead
 */
@Deprecated
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
    private boolean canUseEnumMap = true;
    @SuppressWarnings("rawtypes")
    private Class<? extends Enum> enumKeyType;
    ImmutableMapBuilder(Map<K, V> emptyMap) { this.map = emptyMap; }

    public ImmutableMapBuilder<K, V> put(K key, V value) {
      if (canUseEnumMap) {
        if (enumKeyType != null) {
          if (!enumKeyType.isInstance(key)) {  // Values from different enums
            canUseEnumMap = false;
            enumKeyType = null;
          }
        } else if (key instanceof Enum<?>) {
          enumKeyType = Enum.class.cast(key).getClass();
        } else {
          canUseEnumMap = false;
        }
      }
      map.put(key, value);
      return this;
    }

    public ImmutableMapBuilder<K, V> putAll(Map<? extends K, ? extends V> map) {
      if (canUseEnumMap) {
        for (Map.Entry<? extends K, ? extends V> e : map.entrySet()) {
          put(e.getKey(), e.getValue());
        }
      } else {
        this.map.putAll(map);
      }
      return this;
    }

    public Map<K, V> create() {
      if (this.map.isEmpty()) { return Collections.<K, V>emptyMap(); }
      Map<K, V> map;
      if (canUseEnumMap) {
        map = Maps.<K, V>makeEnumMap(enumKeyType);
        map.putAll(this.map);
      } else {
        map = this.map;
      }
      if (map == null) { throw new IllegalStateException(); }
      this.map = null;
      return Collections.unmodifiableMap(map);
    }
  }

  // This is legit because enumKeyType above is both an enum type (checked at
  // runtime in the EnumMap ctor) and is the type of a subclass of K.
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static <K, V>
  Map<K, V> makeEnumMap(Class<? extends Enum> t) { return new EnumMap(t); }

  private Maps() { /* uninstantiable */ }
}
