// Copyright (C) 2011 Google Inc.
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

package com.google.caja.lang.css;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Lists;
import com.google.caja.util.MoreAsserts;
import com.google.caja.util.Sets;
import com.google.common.collect.ImmutableList;

public class PartitionsTest extends CajaTestCase {
  public final void testPartitions() {
    List<String> a = ImmutableList.of("foo", "bar", "baz", "boo");
    List<String> b = ImmutableList.of("baz", "boo", "foo", "far", "bar");
    List<String> c = ImmutableList.of("boo", "boo", "far");
    List<String> d = ImmutableList.of("unicorn", "yeti", "loch ness");
    List<String> e = ImmutableList.of();
    List<String> f = ImmutableList.of("rainbow");
    List<String> g = ImmutableList.of("rainbow");
    List<String> h = Lists.newArrayList((String) null, "yeti", "unicorn");

    List<List<String>> sets = ImmutableList.of(a, b, c, d, e, f, g, h);

    Partitions.Partition<String> p = Partitions.partition(
        sets, String.class, null);

    assertPartitionOf(p, sets);

    assertPartition(
        p,
        "bar;baz;foo",
        "unicorn;yeti",
        "null",
        "boo",
        "far",
        "loch ness",
        "rainbow");
  }

  private static <T> void assertPartitionOf(
      Partitions.Partition<T> p, List<List<T>> sets) {
    assertEquals(p.unions.length, sets.size());

    // Make sure partition elements are disjoint and union to universe.
    BitSet universe = new BitSet(p.universe.length);
    universe.set(0, p.universe.length);
    BitSet union = new BitSet(p.universe.length);
    for (int[] partitionElement : p.partition) {
      assertSortedAndUnique(partitionElement);
      for (int elIndex : partitionElement) {
        if (union.get(elIndex)) {
          fail("partition elements not disjoint "
              + Arrays.toString(p.partition));
        }
        union.set(elIndex);
      }
    }
    assertEquals(universe, union);

    // Make sure unioning reproduces the input sets.
    for (int i = 0; i < sets.size(); ++i) {
      List<T> set = sets.get(i);
      List<T> actual = Lists.newArrayList();
      for (int partitionIndex : p.unions[i]) {
        for (int elIndex : p.partition[partitionIndex]) {
          actual.add(p.universe[elIndex]);
        }
      }
      assertEquals(
          "" + set,
          Sets.newLinkedHashSet(set),
          Sets.newLinkedHashSet(actual));
    }
  }

  private static <T> void assertPartition(
      Partitions.Partition<?> p, String... golden) {
    String[] actual = new String[p.partition.length];
    int i = 0;
    for (int[] partition : p.partition) {
      StringBuilder sb = new StringBuilder();
      boolean first = true;
      for (int elIndex : partition) {
        if (first) {
          first = false;
        } else {
          sb.append(';');
        }
        sb.append(p.universe[elIndex]);
      }
      actual[i++] = sb.toString();
    }
    MoreAsserts.assertListsEqual(Arrays.asList(golden), Arrays.asList(actual));
  }

  private static void assertSortedAndUnique(int[] arr) {
    if (arr.length == 0) { return; }
    for (int i = 1; i < arr.length; ++i) {
      if (arr[i - 1] >= arr[i]) {
        fail("Not sorted and unique " + arr[i - 1] + " >= " + arr[i]);
      }
    }
  }
}
