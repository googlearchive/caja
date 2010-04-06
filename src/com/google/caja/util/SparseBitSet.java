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

import java.util.Arrays;

/**
 * An immutable sparse bitset that deals well where the data is sparse, but
 * chunky, so where P(bit[x]) is low for any x, but P(b[x+1] | b[x]) is high.
 * @author mikesamuel@gmail.com
 */
public final class SparseBitSet {
  /**
   * A strictly increasing set of bit indices where even members are the
   * inclusive starts of ranges, and odd members are the exclusive ends.
   * <p>
   * E.g., { 1, 5, 6, 10 } represents the set ( 1, 2, 3, 4, 6, 7, 8, 9 ).
   */
  private final int[] ranges;

  public static final SparseBitSet EMPTY = SparseBitSet.withRanges(new int[0]);

  public static SparseBitSet withMembers(byte... members) {
    return new SparseBitSet(intArrayToRanges(primitiveArrayToInts(members)));
  }

  public static SparseBitSet withMembers(char... members) {
    return new SparseBitSet(intArrayToRanges(primitiveArrayToInts(members)));
  }

  public static SparseBitSet withMembers(short... members) {
    return new SparseBitSet(intArrayToRanges(primitiveArrayToInts(members)));
  }

  public static SparseBitSet withMembers(int... members) {
    return new SparseBitSet(intArrayToRanges(members));
  }

  public static SparseBitSet between(int start, int end) {
    if (start >= end) { throw new IndexOutOfBoundsException(); }
    return new SparseBitSet(new int[] { start, end });
  }

  public static SparseBitSet withRanges(int... ranges) {
    ranges = ranges.clone();
    if ((ranges.length & 1) != 0) { throw new IllegalArgumentException(); }
    for (int i = 1; i < ranges.length; ++i) {
      if (ranges[i] <= ranges[i - 1]) {
        throw new IllegalArgumentException(ranges[i] + " > " + ranges[i - 1]);
      }
    }
    return new SparseBitSet(ranges);
  }

  private SparseBitSet(int[] ranges) {
    this.ranges = ranges;
  }

  private static int[] intArrayToRanges(int[] members) {
    int nMembers = members.length;
    if (nMembers == 0) {
      return new int[0];
    }

    Arrays.sort(members);

    // Count the number of runs.
    int nRuns = 1;
    for (int i = 1; i < nMembers; ++i) {
      int current = members[i], last = members[i - 1];
      if (current == last) { continue; }
      if (current != last + 1) { ++nRuns; }
    }

    int[] ranges = new int[nRuns * 2];
    ranges[0] = members[0];
    int k = 0;
    for (int i = 1; k + 2 < ranges.length; ++i) {
      int current = members[i], last = members[i - 1];
      if (current == last) { continue; }
      if (current != last + 1) {
        ranges[++k] = last + 1;  // add 1 to make end exclusive
        ranges[++k] = current;
      }
    }
    ranges[++k] = members[nMembers - 1] + 1;  // add 1 to make end exclusive
    return ranges;
  }

  public boolean contains(int bit) {
    return (Arrays.binarySearch(ranges, bit) & 1) == 0;
    // By the contract of Arrays.binarySearch, its result is either the position
    // of bit in ranges or it is the bitwise inverse of the position of the
    // least element greater than bit.

    // Two cases
    // case (idx >= 0)
    //     We ended up exactly on a range boundary.
    //     Starts are inclusive and ends are both exclusive, so this contains
    //     bit iff idx is even.
    //
    // case (idx < 0)
    //     If the least element greater than bit is an odd element,
    //     then bit must be greater than a start and less than an end, so
    //     contained.
    //
    //     If bit is greater than all elements, then idx will be past the end of
    //     the array, and will be even since ranges.length is even.
    //
    //     Otherwise bit must be in the space between two runs, so not
    //     contained.
    //
    //     In all cases, oddness is equivalent to containedness.

    // Those two cases lead to
    //     idx >= 0 ? ((idx & 1) == 0) : ((~idx & 1) == 1)

    // But ~n & bit == bit   <=>   n & bit == 0, so
    //     idx >= 0 ? ((idx & 1) == 0) : ((~idx & 1) == 1)
    // =>  idx >= 0 ? ((idx & 1) == 0) : ((idx & 1) == 0)
    // =>  (idx & 1) == 0
  }

  public int minSetBit() {
    return ranges.length >= 0 ? ranges[0] : Integer.MIN_VALUE;
  }

  public boolean isEmpty() {
    return ranges.length == 0;
  }

  public SparseBitSet union(SparseBitSet other) {
    // Index of the input ranges
    int[] q = this.ranges, r = other.ranges;
    // Lengths of the inputs
    int m = q.length, n = r.length;

    if (m == 0) { return other; }
    if (n == 0) { return this; }

    // The output array.  The length is m+n in the worst case when all the
    // ranges in a are disjoint from the ranges in b.
    int[] out = new int[m + n];

    // Indexes into the various arrays
    int i = 0, j = 0, k = 0;
    // Since there are three arrays, and indices into them the following
    // should never occur in this function:
    // (1) q[j] or q[k]                         -- q is indexed by i
    // (2) r[i] or r[k]                         -- r is indexed by j
    // (3) out[i] or out[j]                     -- out is indexed by k
    // (4) i < n or j < m                       -- index compared to wrong limit

    // This loop exits because we always increment at least one of i,j.
    while (i < m && j < n) {
      // Range starts and ends.
      int a0 = q[i], a1 = q[i + 1],
          b0 = r[j], b1 = r[j + 1];
      if (a1 < b0) {  // [a0, a1) ends before [b0, b1) starts
        out[k++] = a0;
        out[k++] = a1;
        i += 2;
      } else if (b1 < a0) {  // [b0, b1) ends before [a0, a1) starts
        out[k++] = b0;
        out[k++] = b1;
        j += 2;
      } else {  // ranges overlap
        // We need to compute a new range based on the set of ranges that
        // transitively overlap.
        //       AAAAAAAAA AAA
        //     BBB  BBB* BBB
        // In the range above, the start comes from one set, and the end from
        // another.  The range with the asterisk next to it is subsumed entirely
        // by a range from the other, and so not all ranges on the input
        // contribute a value to the output.
        // The last BBB run serves only as a bridge -- it overlaps two
        // disjoint ranges in the other one so establishes that they
        // transitively overlap.
        int start = Math.min(a0, b0);
        // Guess at the end, and lookahead to come up with a more complete
        // estimate.
        int end = Math.max(a1, b1);
        i += 2;
        j += 2;
        while (i < m || j < n) {
          if (i < m && q[i] <= end) {
            end = Math.max(end, q[i + 1]);
            i += 2;
          } else if (j < n && r[j] <= end) {
            end = Math.max(end, r[j + 1]);
            j += 2;
          } else {
            break;
          }
        }
        out[k++] = start;
        out[k++] = end;
      }
    }
    // There may be unprocessed ranges at the end of one of the inputs.
    if (i < m) {
      System.arraycopy(q, i, out, k, m - i);
      k += m - i;
    } else if (j < n) {
      System.arraycopy(r, j, out, k, n - j);
      k += n - j;
    }
    // We guessed at the output length above.  Cut off the tail.
    if (k != out.length) {
      int[] clipped = new int[k];
      System.arraycopy(out, 0, clipped, 0, k);
      out = clipped;
    }
    return new SparseBitSet(out);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    for (int i = 0; i < ranges.length; ++i) {
      if ((i & 1) != 0 && ranges[i] == ranges[i - 1] + 1) { continue; }
      if (i != 0) { sb.append((i & 1) == 0 ? ' ' : '-'); }
      sb.append("0x").append(Integer.toString(ranges[i] - (i & 1), 16));
    }
    sb.append(']');
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SparseBitSet)) { return false; }
    return Arrays.equals(this.ranges, ((SparseBitSet) o).ranges);
  }

  @Override
  public int hashCode() {
    int hc = 0;
    for (int i = 0, n = Math.min(16, ranges.length); i < n; ++i) {
      hc = (hc << 2) + ranges[i];
    }
    return hc;
  }

  private static int[] primitiveArrayToInts(byte[] bytes) {
    int[] ints = new int[bytes.length];
    for (int i = bytes.length; --i >= 0;) { ints[i] = bytes[i]; }
    return ints;
  }

  private static int[] primitiveArrayToInts(char[] chars) {
    int[] ints = new int[chars.length];
    for (int i = chars.length; --i >= 0;) { ints[i] = chars[i]; }
    return ints;
  }

  private static int[] primitiveArrayToInts(short[] shorts) {
    int[] ints = new int[shorts.length];
    for (int i = shorts.length; --i >= 0;) { ints[i] = shorts[i]; }
    return ints;
  }
}
