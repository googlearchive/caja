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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Shortcuts for creating sets.
 * Use com.google.common.collect.Sets instead
 */
@Deprecated
public final class Sets {
  public static <E>
  Set<E> newHashSet() {
    return new HashSet<E>();
  }

  public static <E>
  Set<E> newHashSet(Collection<? extends E> els) {
    return new HashSet<E>(els);
  }

  public static <E>
  Set<E> newHashSet(Iterable<? extends E> els) {
    Set<E> s = new HashSet<E>();
    for (E el : els) { s.add(el); }
    return s;
  }

  public static <E>
  Set<E> newHashSet(E... els) {
    Set<E> s = new HashSet<E>(els.length);
    for (E el : els) { s.add(el); }
    return s;
  }

  public static <E>
  Set<E> newLinkedHashSet() {
    return new LinkedHashSet<E>();
  }

  public static <E>
  Set<E> newLinkedHashSet(Collection<? extends E> els) {
    return new LinkedHashSet<E>(els);
  }

  public static <E>
  Set<E> newLinkedHashSet(Iterable<? extends E> els) {
    Set<E> s = new LinkedHashSet<E>();
    for (E el : els) { s.add(el); }
    return s;
  }

  public static <E>
  Set<E> newLinkedHashSet(E... els) {
    Set<E> s = new LinkedHashSet<E>(els.length);
    for (E el : els) { s.add(el); }
    return s;
  }

  public static <E>
  Set<E> newIdentityHashSet() {
    return new LinkedHashSet<E>();
  }

  public static <E>
  Set<E> newIdentityHashSet(Set<? extends E> set) {
    return new LinkedHashSet<E>(set);
  }

  public static <E extends Comparable<? super E>>
  SortedSet<E> newTreeSet() {
    return new TreeSet<E>();
  }

  public static <E extends Comparable<? super E>>
  SortedSet<E> newTreeSet(Collection<? extends E> els) {
    return new TreeSet<E>(els);
  }

  public static <E extends Comparable<? super E>>
  SortedSet<E> newTreeSet(Iterable<? extends E> els) {
    SortedSet<E> s = new TreeSet<E>();
    for (E el : els) { s.add(el); }
    return s;
  }

  public static <E extends Comparable<? super E>>
  Set<E> newTreeSet(E... els) {
    Set<E> s = new TreeSet<E>();
    for (E el : els) { s.add(el); }
    return s;
  }

  public static <E>
  SortedSet<E> newTreeSet(Comparator<? super E> cmp) {
    return new TreeSet<E>(cmp);
  }

  public static <E>
  SortedSet<E> newTreeSet(
      Comparator<? super E> cmp, Collection<? extends E> els) {
    SortedSet<E> s = new TreeSet<E>(cmp);
    s.addAll(els);
    return s;
  }

  public static <E>
  SortedSet<E> newTreeSet(
      Comparator<? super E> cmp, Iterable<? extends E> els) {
    SortedSet<E> s = new TreeSet<E>(cmp);
    for (E el : els) { s.add(el); }
    return s;
  }

  public static <E extends Comparable<E>>
  Set<E> newTreeSet(Comparator<? super E> cmp, E... els) {
    Set<E> s = new TreeSet<E>(cmp);
    for (E el : els) { s.add(el); }
    return s;
  }

  public static <E> Set<E> immutableSet(E... elements) {
    switch (elements.length) {
      case 0: return Collections.emptySet();
      case 1: return Collections.singleton(elements[0]);
      default: return Collections.unmodifiableSet(newLinkedHashSet(elements));
    }
  }

  public static <E> Set<E> immutableSet(Collection<? extends E> elementsExtended) {
    Set<E> elements = new HashSet<>();
    elements.addAll(elementsExtended);
    switch (elements.size()) {
      case 0: return Collections.emptySet();
      case 1: return Collections.<E>singleton(elements.iterator().next());
      default: return Collections.unmodifiableSet(newLinkedHashSet(elements));
    }
  }

  public static <E> Set<E> immutableSet(Iterable<? extends E> elementsExtended) {
    Set<E> elements = new HashSet<>();
    for (E elem : elementsExtended) {
      elements.add(elem);
    }
    return Collections.unmodifiableSet(newLinkedHashSet(elements));
  }

  /** <tt>a U b</tt> */
  public static <T> Set<T> union(
      Collection<? extends T> a, Collection<? extends T> b) {
    Set<T> u = new LinkedHashSet<>();
    u.addAll(a);
    u.addAll(b);
    return u;
  }

  public static <T> Set<T> intersection(
      Collection<? extends T> a, Collection<? extends T> b) {
    // Use the smaller to construct i
    if (b.size() > a.size()) {
      Collection<? extends T> t = a;
      a = b;
      b = t;
    }
    Set<T> pureB = new HashSet<>();
    pureB.addAll(b);
    Set<T> i = newLinkedHashSet(pureB);
    i.retainAll(a);
    return i;
  }

  /** <tt>(pos - neg)</tt>. */
  public static <T> Set<T> difference(
      Collection<? extends T> pos, Collection<? extends T> neg) {
    Set<T> purePos = new HashSet<>();
    purePos.addAll(pos);
    Set<T> d = newLinkedHashSet(purePos);
    d.removeAll(neg);
    return d;
  }

  private Sets() { /* uninstantiable */ }
}
