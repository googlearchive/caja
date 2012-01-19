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

import com.google.caja.util.Bag;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

class Partitions {
  static class Partition<T> {
    /**
     * For each input set S, a set of indices I into partition such that
     * <ol>
     * <li>S' is the minimal set s.t. i is in S' when universe[i] is in S.
     * <li>S' = union( partition[i] for i in I ).
     * </ul>
     */
    int[][] unions;
    /**
     * The union of the input sets, sorted if comparable or if a comparator
     * was supplied.
     */
    T[] universe;
    /**
     * A partition of the set (0..universe.length).
     * Each element is a sorted-unique array of indices into universe.
     * Each element is disjoint w.r.t. every other element.
     */
    int[][] partition;
  }

  /** A monotonically increasing slice of an {@code int[]}. */
  private static class ISlice {
    /**
     * Elements between 0 (inclusive) and count (exclusive) are strictly
     * monotonically increasing.
     */
    int[] els;
    /** Count of usable elements in els. */
    int count;

    /** Modify {@code this} to remove any elements in r. */
    boolean subtract(ISlice r) {
      int i = 0, j = 0, k = 0;
      while (i < count && j < r.count) {
        int a = els[i], b = r.els[j];
        if (a < b) {
          els[k++] = els[i++];
        } else if (a == b) {
          ++i;
          ++j;
        } else {
          ++j;
        }
      }
      if (i != count) {
        int n = count - i;
        System.arraycopy(els, i, els, k, n);
        k += n;
      }
      if (count != k) {
        count = k;
        return true;  // The row changed.
      }
      return false;
    }

    /** Modify {@code this} to be the intersection of this and r. */
    void preserve(ISlice r) {
      int i = 0, j = 0, k = 0;
      while (i < count && j < r.count) {
        int a = els[i], b = r.els[j];
        if (a < b) {
          ++i;
        } else if (a == b) {
          els[k++] = a;
          ++i;
          ++j;
        } else {
          ++j;
        }
      }
      count = k;
    }

    boolean contains(int el) {
     return Arrays.binarySearch(els, 0, count, el) >= 0;
    }

    @Override
    public ISlice clone() {
      ISlice clone = new ISlice();
      clone.els = toArray();
      clone.count = count;
      return clone;
    }

    int[] toArray() {
      return Arrays.copyOfRange(els, 0, count);
    }

    @Override public String toString() {
      return Arrays.toString(toArray());
    }
  }

  /**
   * Computes a partition of union(sets) such that each set can be
   * efficiently represented as a union of a subset of that partition.
   *
   * <p>
   * This may be very inefficient if sets contains a large number of empty
   * elements.
   */
  static <T> Partition<T> partition(
      Iterable<? extends Iterable<T>> sets, Class<T> type,
      Comparator<?> cmp) {
    // Compute the unique elements of sets.
    T[] universe;
    int nSets = 0;
    Map<T, Integer> elToIndex = Maps.newHashMap();
    {
      Bag<T> bag = Bag.newHashBag();
      for (Iterable<T> set : sets) {
        for (T el : set) {
          bag.incr(el);
        }
        ++nSets;
      }
      Set<T> uniq = bag.nonZeroCounts();
      // Have to box to fit in a HashBag.
      universe = allocateReferenceTypeArray(type, uniq.size());
      {
        int i = 0;
        for (T el : uniq) { universe[i++] = el; }
      }

      tryToSort(universe, cmp);
      for (int i = 0; i < universe.length; ++i) {
        elToIndex.put(universe[i], i);
      }
    }
//  System.err.println("universe=" + Arrays.toString(universe));

    // Compute a table from sets that includes a row per input set.
    // The row is a sorted array slice of indices into universe.
    // We will destructively modify rows when we create a partition element by
    // removing all elements from each row that are in the partition so we also
    // maintain a count of non zero rows and columns.
    ISlice nonZeroCols = new ISlice();
    nonZeroCols.els = new int[nonZeroCols.count = universe.length];
    for (int i = 0; i < nonZeroCols.count; ++i) { nonZeroCols.els[i] = i; }

    ISlice[] matrix = new ISlice[nSets];
    {
      int i = 0;
      BitSet rowBits = new BitSet(universe.length);
      for (Iterable<T> set : sets) {
        rowBits.clear();
        for (T el : set) {
          int index = elToIndex.get(el);
          if (!rowBits.get(index)) {
            rowBits.set(index);
          }
        }
        ISlice row = new ISlice();
        row.count = rowBits.cardinality();
        row.els = new int[row.count];
        for (int b = -1, j = 0; (b = rowBits.nextSetBit(b+1)) >= 0;) {
          row.els[j++] = b;
        }
        Arrays.sort(row.els);
        matrix[i++] = row;
      }
    }

    ISlice nonZeroRows = new ISlice();
    nonZeroRows.els = new int[nSets];
    for (int i = 0; i < matrix.length; ++i) {
      if (matrix[i].count != 0) {
        nonZeroRows.els[nonZeroRows.count++] = i;
      }
    }


    // An ordered list of disjoint sets.
    List<ISlice> partition = Lists.newArrayList();
    // For each input sets, the indices of elements in partition that must be
    // unioned to produce the corresponding set S' as described in the javadoc.
    ISlice[] unionIndices = new ISlice[nSets];
    for (int i = 0; i < nSets; ++i) {
      unionIndices[i] = new ISlice();
      unionIndices[i].els = new int[universe.length];
    }

    while (nonZeroRows.count != 0) {
//    System.err.println("Start");
//    System.err.println("\tnonZeroRows=" + nonZeroRows);
//    System.err.println("\tnonZeroCols=" + nonZeroCols);
//    System.err.println("\tmatrix=" + Arrays.toString(matrix));
//    System.err.println("\tpartition=" + partition);
      ISlice bestPartitionElement = null;
      for (int i = 0; i < nonZeroCols.count; ++i) {
        int el = nonZeroCols.els[i];
        ISlice candidate = null;
        // Compute the intersection of all rows containing el.
        for (int j = 0; j < nonZeroRows.count; ++j) {
          ISlice r = matrix[nonZeroRows.els[j]];
          if (r.contains(el)) {
            if (candidate == null) {
              candidate = r.clone();
            } else {
//            String before = "" + r;
              candidate.preserve(r);
//            System.err.println("\tFor " + el + ", with " + r + ", candidate=" + before + " -> " + candidate);
            }
          }
        }
        // Subtract out anything that is in a set that does not contain el.
        assert candidate != null;
        for (int j = 0; j < nonZeroRows.count; ++j) {
          ISlice r = matrix[nonZeroRows.els[j]];
          if (!r.contains(el)) {
            candidate.subtract(r);
          }
        }
        // Now we know the maximal set of elements that always co-occur with el.
        if (bestPartitionElement == null
            || bestPartitionElement.count < candidate.count) {
          bestPartitionElement = candidate;
        }
      }

      // Add best to the partition and update the matrix by subtracting the
      // columns in best, and any rows that are subsets of best.
      assert bestPartitionElement != null;
      int partitionIndex = partition.size();
      partition.add(bestPartitionElement);

//    System.err.println("\t====\n\tbest=" + bestPartitionElement);
      nonZeroCols.subtract(bestPartitionElement);
//    System.err.println("\tnonZeroCols<-" + nonZeroCols);
      int k = 0;
      for (int j = 0; j < nonZeroRows.count; ++j) {
        int rowIndex = nonZeroRows.els[j];
        ISlice r = matrix[rowIndex];
//      String before = "" + r;
        boolean changed = r.subtract(bestPartitionElement);
//      System.err.println("\trow " + rowIndex + "=" + before + "->" + r + (changed ? ", changed" : ", no change"));
        if (changed) {
          unionIndices[rowIndex]
              .els[unionIndices[rowIndex].count++] = partitionIndex;
        }
        if (r.count != 0) {
          // Keep it in nonZeroRows.
          nonZeroRows.els[k++] = rowIndex;
        } else {
          assert changed;
        }
      }
      nonZeroRows.count = k;

//    System.err.println("\t====\n\tnonZeroRows <- " + nonZeroRows);
//    System.err.println("\tnonZeroCols <- " + nonZeroCols);
//    System.err.println("\tmatrix <- " + Arrays.toString(matrix));

      if (bestPartitionElement.count == 1) {
        // All candidates are equally bad, handle as long tail below.
        break;
      }
    }

    // Handle the long tail by creating a single element partition for each
    // non-zero row.
    {
      int[] elementToPartition = new int[universe.length];
      for (int i = 0; i < nonZeroCols.count; ++i) {
        int el = nonZeroCols.els[i];
        ISlice singleton = new ISlice();
        singleton.count = 1;
        singleton.els = new int[] { el };
        elementToPartition[el] = partition.size();
        partition.add(singleton);
      }
      nonZeroCols.count = 0;

      for (int j = 0; j < nonZeroRows.count; ++j) {
        int rowIndex = nonZeroRows.els[j];
        ISlice r = matrix[rowIndex];
        ISlice union = unionIndices[rowIndex];
        for (int i = 0; i < r.count; ++i) {
          int el = r.els[i];
          union.els[union.count++] = elementToPartition[el];
        }
        r.count = 0;
      }
      nonZeroRows.count = 0;
    }

    // Construct the output.
    Partition<T> result = new Partition<T>();
    result.universe = universe;
    result.unions = new int[unionIndices.length][];
    for (int i = 0; i < unionIndices.length; ++i) {
      result.unions[i] = unionIndices[i].toArray();
    }
    result.partition = new int[partition.size()][];
    for (int i = 0; i < result.partition.length; ++i) {
      result.partition[i] = partition.get(i).toArray();
    }

    return result;
  }

  @SuppressWarnings("unchecked")
  private static <T> T[] allocateReferenceTypeArray(Class<T> type, int n) {
    // If a primitive type was passed in, this is un-type safe, but that should
    // not introduce type-unsafety since <T> in partition cannot bind to a
    // primitive type unless some-other unchecked cast was unsafe.
    if (type.isPrimitive()) { throw new AssertionError(type.getName()); }
    // This is type-safe when type is not-primitive.
    return (T[]) Array.newInstance(type, n);
  }

  @SuppressWarnings("unchecked")
  private static <T> void tryToSort(T[] arr, Comparator<?> cmp) {
    if (cmp == null) {
      // Type safe.
      final Class<T> tClazz = (Class<T>) arr.getClass().getComponentType();
      if (!Comparable.class.isAssignableFrom(tClazz)) { return; }
      // Don't use the natural ordering since that can't handle null.
      cmp = new Comparator<Comparable<?>>() {
        public int compare(Comparable<?> a, Comparable<?> b) {
          T at = tClazz.cast(a), bt = tClazz.cast(b);
          if (at == null) {
            return bt == null ? 0 : -1;
          } else if (bt == null) {
            return 1;
          }
          // This is not technically type-safe, as at could implement
          // Comparable<SomeDisjointType>.
          // If at does obey the contract for Comparable it will, at worst,
          // result in a ClassCastException.
          return ((Comparable<? super T>) at).compareTo(bt);
        }
      };
    }
    // This is type-unsafe since a client can pass in a comparator of the
    // wrong type.  This should result in a ClassCastException at worst which
    // aborts partitioning.
    Arrays.sort(arr, (Comparator<? super T>) cmp);
  }

}
