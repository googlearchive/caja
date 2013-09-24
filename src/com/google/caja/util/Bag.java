// Copyright (C) 2010 Google Inc.
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

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;

/**
 * A simple bag implementation.
 * @author mikesamuel@gmail.com
 */
public class Bag<T> {
  public static <T> Bag<T> newHashBag() {
    return new Bag<T>(Maps.<T, Integer>newLinkedHashMap());
  }
  public static <T extends Comparable<T>> Bag<T> newTreeBag() {
    return new Bag<T>(Maps.<T, Integer>newTreeMap());
  }
  public static <T> Bag<T> newIdentityHashBag() {
    return new Bag<T>(Maps.<T, Integer>newIdentityHashMap());
  }

  private final Map<T, Integer> counts;

  private Bag(Map<T, Integer> counts) { this.counts = counts; }

  /** Adds 1 to the count for k and returns the count prior to addition. */
  public int incr(T k) { return incr(k, 1); }

  /** Adds delta to the count for k and returns the count prior to addition. */
  public int incr(T k, int delta) {
    Integer i = counts.get(k);
    if (i == null) { i = 0; }
    int next = i.intValue() + delta;
    if (next < 0) {
      throw new java.lang.ArithmeticException();
    }
    if (next == 0) {
      counts.remove(k);
    } else {
      counts.put(k, i + delta);
    }
    return i;
  }

  public int get(T k) {
    Integer i = counts.get(k);
    return i == null ? 0 : i.intValue();
  }

  public int reset(T k) {
    Integer i = counts.remove(k);
    return i == null ? 0 : i.intValue();
  }

  public Set<T> nonZeroCounts() {
    return counts.keySet();
  }

  public void clear() {
    counts.clear();
  }

  @Override
  public int hashCode() { return counts.hashCode(); }

  @Override
  public boolean equals(Object o) {
    return o instanceof Bag<?> && counts.equals(((Bag<?>) o).counts);
  }

  @Override
  public String toString() { return counts.toString(); }
}
