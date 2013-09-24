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

import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

@SuppressWarnings("static-method")
public class CollectionsTest extends TestCase {
  public final void testListMultimaps() {
    Multimap<String, String> m = Multimaps.newListHashMultimap();
    assertTrue(m.isEmpty());
    assertEquals("[]", m.keySet().toString());
    assertEquals("[]", m.get("foo").toString());

    m.put("foo", "bar");
    assertEquals("[foo]", m.keySet().toString());
    assertEquals("[bar]", m.get("foo").toString());
    assertFalse(m.isEmpty());

    m.put("foo", "bar");
    assertEquals("[foo]", m.keySet().toString());
    assertEquals("[bar, bar]", m.get("foo").toString());
    assertFalse(m.isEmpty());

    m.remove("foo", "bar");
    assertEquals("[foo]", m.keySet().toString());
    assertEquals("[bar]", m.get("foo").toString());
    assertFalse(m.isEmpty());

    m.remove("foo", "bar");
    assertEquals("[]", m.keySet().toString());
    assertEquals("[]", m.get("foo").toString());
    assertTrue(m.isEmpty());

    m.putAll("baz", Arrays.asList("boo", "far", "boo", "bob"));
    assertEquals("[baz]", m.keySet().toString());
    assertEquals("[boo, far, boo, bob]", m.get("baz").toString());
    assertFalse(m.isEmpty());

    m.removeAll("baz", Arrays.asList("bar", "boo", "far"));
    assertEquals("[baz]", m.keySet().toString());
    assertEquals("[bob]", m.get("baz").toString());
    assertFalse(m.isEmpty());

    m.removeAll("baz", Arrays.asList("bar", "bob"));
    assertEquals("[]", m.keySet().toString());
    assertEquals("[]", m.get("baz").toString());
    assertTrue(m.isEmpty());
  }

  public final void testSetMultimaps() {
    Multimap<String, String> m = Multimaps.newSetHashMultimap();
    assertTrue(m.isEmpty());
    assertEquals("[]", m.keySet().toString());
    assertEquals("[]", m.get("foo").toString());

    m.put("foo", "bar");
    assertEquals("[foo]", m.keySet().toString());
    assertEquals("[bar]", m.get("foo").toString());
    assertFalse(m.isEmpty());

    m.put("foo", "bar");
    assertEquals("[foo]", m.keySet().toString());
    assertEquals("[bar]", m.get("foo").toString());
    assertFalse(m.isEmpty());

    m.remove("foo", "bar");
    assertEquals("[]", m.keySet().toString());
    assertEquals("[]", m.get("foo").toString());
    assertTrue(m.isEmpty());

    m.putAll("baz", Arrays.asList("boo", "far", "boo", "bob"));
    assertEquals("[baz]", m.keySet().toString());
    assertEquals("[boo, far, bob]", m.get("baz").toString());
    assertFalse(m.isEmpty());

    m.removeAll("baz", Arrays.asList("bar", "boo", "far"));
    assertEquals("[baz]", m.keySet().toString());
    assertEquals("[bob]", m.get("baz").toString());
    assertFalse(m.isEmpty());

    m.removeAll("baz", Arrays.asList("bar", "bob"));
    assertEquals("[]", m.keySet().toString());
    assertEquals("[]", m.get("baz").toString());
    assertTrue(m.isEmpty());
  }

  public final void testPetulantCollection() {
    Multimap<String, String> m
        = new Multimaps.MultimapImpl<String, String, Collection<String>>(
            new Multimaps.Maker<Map<String, Collection<String>>>() {
              public Map<String, Collection<String>> newInstance() {
                return Maps.newHashMap();
              }
            },
            new Multimaps.Maker<Collection<String>>() {
              public Collection<String> newInstance() {
                return new PetulantCollection();
              }
            });
    assertTrue(m.isEmpty());
    assertEquals("[]", m.keySet().toString());
    assertEquals("[]", m.get("foo").toString());

    m.put("foo", "bar");
    assertEquals("[]", m.keySet().toString());
    assertEquals("[]", m.get("foo").toString());
    assertTrue(m.isEmpty());

    m.put("foo", "bar");
    assertEquals("[]", m.keySet().toString());
    assertEquals("[]", m.get("foo").toString());
    assertTrue(m.isEmpty());

    m.remove("foo", "bar");
    assertEquals("[]", m.keySet().toString());
    assertEquals("[]", m.get("foo").toString());
    assertTrue(m.isEmpty());

    m.remove("foo", "bar");
    assertEquals("[]", m.keySet().toString());
    assertEquals("[]", m.get("foo").toString());
    assertTrue(m.isEmpty());

    m.putAll("baz", Arrays.asList("boo", "far"));
    assertEquals("[]", m.keySet().toString());
    assertEquals("[]", m.get("foo").toString());
    assertTrue(m.isEmpty());

    m.removeAll("baz", Arrays.asList("bar", "boo", "far"));
    assertEquals("[]", m.keySet().toString());
    assertEquals("[]", m.get("foo").toString());
    assertTrue(m.isEmpty());
  }

  public final void testClone() {
    Multimap<String, String> m = Multimaps.newListLinkedHashMultimap();
    m.put("a", "A");
    m.put("b", "B");
    m.put("b", "BEE");
    m.put("d", "D");

    Multimap<String, String> m2 = m.clone();
    m2.put("a", "AYE");
    m2.put("c", "C");
    m2.remove("b", "BEE");

    assertEquals("[A]", m.get("a").toString());
    assertEquals("[B, BEE]", m.get("b").toString());
    assertEquals("[]", m.get("c").toString());
    assertEquals("[D]", m.get("d").toString());
    assertEquals("[A, AYE]", m2.get("a").toString());
    assertEquals("[B]", m2.get("b").toString());
    assertEquals("[C]", m2.get("c").toString());
    assertEquals("[D]", m2.get("d").toString());
  }

  private static class PetulantCollection implements Collection<String> {
    public boolean add(String e) { return false; }
    public boolean addAll(Collection<? extends String> c) { return false; }
    public void clear() { /* noop */ }
    public boolean contains(Object o) { return false; }
    public boolean containsAll(Collection<?> c) { return false; }
    public boolean isEmpty() { return true; }
    public Iterator<String> iterator() {
      return Collections.<String>emptyList().iterator();
    }
    public boolean remove(Object o) { return false; }
    public boolean removeAll(Collection<?> c) { return false; }
    public boolean retainAll(Collection<?> c) { return false; }
    public int size() { return 0; }
    public Object[] toArray() { return new Object[0]; }
    public <T> T[] toArray(T[] a) { return a; }
  }
}
