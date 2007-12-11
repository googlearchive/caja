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
   * inclusive starts of ranges, and odd members are the inclusive ends.
   * <p>
   * E.g., { 1, 5, 6, 10 } represents the set ( 1, 2, 3, 4, 6, 7, 8, 9 ).
   */
  private final int[] ranges;

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

  public static SparseBitSet withRanges(int[] ranges) {
    ranges = ranges.clone();
    if ((ranges.length & 1) != 0) { throw new IllegalArgumentException(); }
    for (int i = 1; i < ranges.length; ++i) {
      if (ranges[i] <= ranges[i - 1]) {
        throw new IllegalArgumentException();
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
