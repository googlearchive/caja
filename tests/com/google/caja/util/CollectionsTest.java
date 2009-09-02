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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import junit.framework.TestCase;

public class CollectionsTest extends TestCase {
  public void testListMultimaps() {
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

  public void testSetMultimaps() {
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

  public void testPetulantCollection() {
    Multimap<String, String> m
        = new Multimaps.AbstractMultimap<String, String, Collection<String>>(
            new HashMap<String, Collection<String>>()) {
              @Override
              Collection<String> makeCollection() {
                return new PetulantCollection();
              }
            };
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

  private static class PetulantCollection implements Collection<String> {
    @Override
    public boolean add(String e) { return false; }
    @Override
    public boolean addAll(Collection<? extends String> c) {
      return false;
    }
    @Override
    public void clear() {}
    @Override
    public boolean contains(Object o) { return false; }
    @Override
    public boolean containsAll(Collection<?> c) { return false; }
    @Override
    public boolean isEmpty() { return true; }
    @Override
    public Iterator<String> iterator() {
      return Collections.<String>emptyList().iterator();
    }
    @Override
    public boolean remove(Object o) { return false; }
    @Override
    public boolean removeAll(Collection<?> c) { return false; }
    @Override
    public boolean retainAll(Collection<?> c) { return false; }
    @Override
    public int size() { return 0; }
    @Override
    public Object[] toArray() { return new Object[0]; }
    @Override
    public <T> T[] toArray(T[] a) { return a; }
  }
}
