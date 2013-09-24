// Copyright (C) 2013 Google Inc.
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

import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * A set-like object that supports a type-safe {@code contains(T)} method
 * instead of the more error-prone {@code Collection<T>.contains(Object)}.
 *
 * This supports containment checks and iteration, and equality, but no other
 * set or collection operations.
 *
 * @param <T> The type of element.
 */
public final class TypesafeSet<T> implements Iterable<T> {
  private final Set<T> contents;

  private TypesafeSet(Iterable<? extends T> els) {
    this.contents = ImmutableSet.copyOf(els);
  }

  public static final <T> TypesafeSet<T> of(T... els) {
    return new TypesafeSet<T>(Arrays.asList(els));
  }

  public static final <T> TypesafeSet<T> of(Iterable<? extends T> els) {
    return new TypesafeSet<T>(els);
  }

  public boolean contains(T el) {
    return contents.contains(el);
  }

  public Iterator<T> iterator() {
    return contents.iterator();
  }

  public Set<T> asSet() {
    return contents;
  }

  @Override
  public String toString() {
    return contents.toString();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof TypesafeSet<?>
        && contents.equals(((TypesafeSet<?>) o).contents);
  }

  @Override
  public int hashCode() {
    return contents.hashCode();
  }
}
