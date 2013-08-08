// Copyright (C) 2007 Google Inc.
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

import java.util.BitSet;
import java.util.Random;

/**
 * @author msamuel@google.com (Mike Samuel)
 */
@SuppressWarnings("static-method")
public class SparseBitSetTest extends CajaTestCase {

  public final void testAgainstRegularImplementation() {
    Random rnd = new Random(SEED);

    for (int run = 10; --run >= 0;) {
      // Fill with bits in the range [0x1000, 0x3000).
      BitSet bs = new BitSet();
      for (int i = 0x1000; --i >= 0;) {
        bs.set(0x1000 + rnd.nextInt(0x3000));
      }

      // Create an equivalent sparse bit set
      int[] members = new int[bs.cardinality()];
      for (int i = -1, k = 0; k < members.length; ++k) {
        members[k] = i = bs.nextSetBit(i + 1);
      }
      SparseBitSet sbs = SparseBitSet.withMembers(members);

      // Check all bits including past the min/max bit
      for (int i = 0; i < 0x5000; ++i) {
        if (bs.get(i) != sbs.contains(i)) {
          fail("sbs=" + sbs + ", bs=" + bs + ", difference at bit " + i);
        }
      }
    }
  }

  public final void testEmptySparseBitSet() {
    SparseBitSet sbs = SparseBitSet.withRanges(new int[0]);
    for (int i = -1000; i < 1000; ++i) {
      assertFalse(sbs.contains(i));
    }
    assertEquals("[]", sbs.toString());
  }

  public final void testSparseBitSetFactories() {
    SparseBitSet bsbs = SparseBitSet.withMembers(new byte[] { 0, 1, 4, 9 });
    assertEquals(bsbs.toString(), "[0x0-0x1 0x4 0x9]", bsbs.toString());
    SparseBitSet ssbs = SparseBitSet.withMembers(new short[] { 0, 1, 4, 9 });
    assertEquals("[0x0-0x1 0x4 0x9]", ssbs.toString());
    SparseBitSet csbs = SparseBitSet.withMembers(new char[] { 0, 1, 4, 9 });
    assertEquals("[0x0-0x1 0x4 0x9]", csbs.toString());
    SparseBitSet isbs = SparseBitSet.withMembers(new int[] { 0, 1, 4, 9 });
    assertEquals("[0x0-0x1 0x4 0x9]", isbs.toString());

    SparseBitSet esbs = SparseBitSet.withMembers(new int[0]);

    assertEquals(bsbs, ssbs);
    assertEquals(bsbs, csbs);
    assertEquals(bsbs, isbs);
    assertFalse(bsbs.equals(esbs));
    assertFalse(bsbs.equals(null));
    assertFalse(bsbs.equals(new Object()));

    assertEquals(bsbs.hashCode(), ssbs.hashCode());
    assertEquals(bsbs.hashCode(), csbs.hashCode());
    assertEquals(bsbs.hashCode(), isbs.hashCode());
    assertFalse(bsbs.hashCode() == esbs.hashCode());
  }

  public final void testRangeConstructor() {
    try {
      SparseBitSet.withRanges(new int[] { 1 });
      fail("Mismatched ranges");
    } catch (IllegalArgumentException ex) {
      // pass
    }

    try {
      SparseBitSet.withRanges(new int[] { 1, 4, 4, 5 });
      fail("Discontiguous ranges");
    } catch (IllegalArgumentException ex) {
      // pass
    }

    try {
      SparseBitSet.withRanges(new int[] { 4, 5, 1, 3 });
      fail("Misordered ranges");
    } catch (IllegalArgumentException ex) {
      // pass
    }

    try {
      SparseBitSet.withRanges(new int[] { 0, 0 });
      fail("Empty range");
    } catch (IllegalArgumentException ex) {
      // pass
    }
  }

  public final void testDupeMembers() {
    SparseBitSet sbs1 = SparseBitSet.withMembers(new int[] { 0, 1, 4, 9 });
    assertEquals(sbs1.toString(), "[0x0-0x1 0x4 0x9]", sbs1.toString());

    SparseBitSet sbs2 = SparseBitSet.withMembers(new int[] { 9, 1, 4, 1, 0 });
    assertEquals(sbs2.toString(), "[0x0-0x1 0x4 0x9]", sbs2.toString());

    assertEquals(sbs1, sbs2);
    assertEquals(sbs1.hashCode(), sbs2.hashCode());

    for (int i = -10; i < 20; ++i) {
      assertEquals("" + i, sbs1.contains(i), sbs2.contains(i));
    }
  }

  public final void testUnion() {
    //                 1               2               3
    // 0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0
    //    AAAAAAAAA      A A A A   A AAA   AAA A A
    //  BBB  BBB  BBB BBB        B B    BBB
    //  UUUUUUUUUUUUU UUUU U U U U U UUUUUUUUU U U
    SparseBitSet a = SparseBitSet.withRanges(new int[] {
        0x03, 0x0C, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19,
        0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x21, 0x24, 0x27, 0x28, 0x29,
        0x2A, 0x2B });
    SparseBitSet b = SparseBitSet.withRanges(new int[] {
        0x01, 0x04, 0x06, 0x09, 0x0B, 0x0E, 0x0F, 0x12, 0x1A, 0x1B,
        0x1C, 0x1D, 0x21, 0x24 });
    SparseBitSet empty = SparseBitSet.withMembers(new int[0]);

    assertEquals(empty, empty.union(empty));
    assertEquals(a, a.union(empty));
    assertEquals(b, empty.union(b));

    SparseBitSet aUb = a.union(b);
    assertEquals(
        "[0x1-0xd 0xf-0x12 0x14 0x16 0x18 0x1a 0x1c 0x1e-0x26 0x28 0x2a]",
        aUb.toString());
    assertEquals(aUb, b.union(a));

    // Check that a and b not changed by operation
    assertEquals(
        "[0x3-0xb 0x12 0x14 0x16 0x18 0x1a 0x1c 0x1e-0x20 0x24-0x26 0x28 0x2a]",
        a.toString());
    assertEquals(
        "[0x1-0x3 0x6-0x8 0xb-0xd 0xf-0x11 0x1a 0x1c 0x21-0x23]",
        b.toString());
  }
}
