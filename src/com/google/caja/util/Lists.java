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
import java.util.LinkedList;
import java.util.List;

/**
 * Shortcuts for creating lists.
 * Use com.google.common.collect.Lists instead
 */
@Deprecated
public final class Lists {
  public static <E>
  List<E> newArrayList() {
    return new ArrayList<E>();
  }

  public static <E>
  List<E> newArrayList(int capacity) {
    return new ArrayList<E>(capacity);
  }

  public static <E>
  List<E> newArrayList(Collection<? extends E> els) {
    return new ArrayList<E>(els);
  }

  public static <E>
  List<E> newArrayList(Iterable<? extends E> els) {
    List<E> list = new ArrayList<E>();
    for (E el : els) { list.add(el); }
    return list;
  }

  public static <E>
  List<E> newArrayList(E... els) {
    List<E> list = new ArrayList<E>(els.length);
    for (E el : els) { list.add(el); }
    return list;
  }

  public static <E>
  LinkedList<E> newLinkedList() {
    return new LinkedList<E>();
  }

  public static <E>
  LinkedList<E> newLinkedList(Collection<? extends E> els) {
    return new LinkedList<E>(els);
  }

  public static <E>
  LinkedList<E> newLinkedList(Iterable<? extends E> els) {
    LinkedList<E> list = new LinkedList<E>();
    for (E el : els) { list.add(el); }
    return list;
  }

  public static <E>
  LinkedList<E> newLinkedList(E... els) {
    LinkedList<E> list = new LinkedList<E>();
    for (E el : els) { list.add(el); }
    return list;
  }

  private Lists() { /* uninstantiable */ }
}
