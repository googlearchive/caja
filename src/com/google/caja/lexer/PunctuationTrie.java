// Copyright (C) 2005 Google Inc.
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

package com.google.caja.lexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A trie used to separate punctuation tokens in a run of non-whitespace
 * characters by preferring the longest punctuation string possible in a
 * greedy left-to-right scan.
 *
 * @author mikesamuel@gmail.com
 */
public final class PunctuationTrie<T> {
  private final char[] childMap;
  private final PunctuationTrie<T>[] children;
  private final boolean terminal;
  private final T value;

  /**
   * @param elements not empty, non null.
   */
  public PunctuationTrie(Map<String, T> elements) {
    this(sortedUniqEntries(elements), 0);
  }

  private PunctuationTrie(List<Map.Entry<String, T>> elements, int depth) {
    this(elements, depth, 0, elements.size());
  }

  /**
   * @param elements not empty, non null.  Not modified.
   * @param depth the depth in the tree.
   * @param start an index into punctuationStrings of the first string in this
   *   subtree.
   * @param end an index into punctuationStrings past the last string in this
   *   subtree.
   */
  private PunctuationTrie(
      List<Map.Entry<String, T>> elements, int depth, int start, int end) {
    this.terminal = depth == elements.get(start).getKey().length();
    if (this.terminal) {
      this.value = elements.get(start).getValue();
      if (start + 1 == end) {  // base case
        this.childMap = ZERO_CHARS;
        this.children = ownedChildArray(ZERO_TRIES);
        return;
      } else {
        ++start;
      }
    } else {
      this.value = null;
    }
    int childCount = 0;
    {
      int last = -1;
      for (int i = start; i < end; ++i) {
        char ch = elements.get(i).getKey().charAt(depth);
        if (ch != last) {
          ++childCount;
          last = ch;
        }
      }
    }
    this.childMap = new char[childCount];
    this.children = ownedChildArray(new PunctuationTrie[childCount]);
    int childStart = start;
    int childIndex = 0;
    char lastCh = elements.get(start).getKey().charAt(depth);
    for (int i = start + 1; i < end; ++i) {
      char ch = elements.get(i).getKey().charAt(depth);
      if (ch != lastCh) {
        childMap[childIndex] = lastCh;
        children[childIndex++] = new PunctuationTrie<T>(
          elements, depth + 1, childStart, i);
        childStart = i;
        lastCh = ch;
      }
    }
    childMap[childIndex] = lastCh;
    children[childIndex++] = new PunctuationTrie<T>(
        elements, depth + 1, childStart, end);
  }

  /** Does this node correspond to a complete string in the input set. */
  public boolean isTerminal() { return terminal; }

  public T getValue() { return value; }

  @SuppressWarnings("unchecked")
  private static <T> PunctuationTrie<T>[] ownedChildArray(
      PunctuationTrie<?>[] unfilledArray) {
    // This method must only be called with a newly created array or an array
    // of size 0.
    // Since all the elements of this array is null, and it either has no
    // mutable elements (because it is of size 0), or is not reachable by a
    // more general type, it is typesafe to cast it to a more specific type.
    return (PunctuationTrie<T>[]) unfilledArray;
  }

  /**
   * The child corresponding to the given character.
   * @return null if no such trie.
   */
  public PunctuationTrie<T> lookup(char ch) {
    int i = Arrays.binarySearch(childMap, ch);
    return i >= 0 ? children[i] : null;
  }

  /**
   * The descendant of this trie corresponding to the string for this trie
   * appended with s.
   * @param s non null.
   * @return null if no such trie.
   */
  public PunctuationTrie<T> lookup(CharSequence s) {
    PunctuationTrie<T> t = this;
    for (int i = 0, n = s.length(); i < n; ++i) {
      t = t.lookup(s.charAt(i));
      if (null == t) { break; }
    }
    return t;
  }

  public boolean contains(char ch) {
    return Arrays.binarySearch(childMap, ch) >= 0;
  }

  private static <T> List<Map.Entry<String, T>> sortedUniqEntries(
      Map<String, T> m) {
    return new ArrayList<Map.Entry<String, T>>(
        new TreeMap<String, T>(m).entrySet());
  }

  private static final char[] ZERO_CHARS = new char[0];
  private static final PunctuationTrie<?>[] ZERO_TRIES = new PunctuationTrie[0];

  /**
   * Append all strings s such that {@code this.lookup(s).isTerminal()} to the
   * given list in lexical order.
   */
  public void toStringList(List<String> strings) {
    toStringList("", strings);
  }

  private void toStringList(String prefix, List<String> strings) {
    if (terminal) { strings.add(prefix); }
    for (int i = 0, n = childMap.length; i < n; ++i) {
      children[i].toStringList(prefix + childMap[i], strings);
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    toStringBuilder(0, sb);
    return sb.toString();
  }

  private void toStringBuilder(int depth, StringBuilder sb) {
    sb.append(terminal ? "terminal" : "nonterminal");
    ++depth;
    for (int i = 0; i < childMap.length; ++i) {
      sb.append('\n');
      for (int d = 0; d < depth; ++d) {
        sb.append('\t');
      }
      sb.append('\'').append(childMap[i]).append("' ");
      children[i].toStringBuilder(depth, sb);
    }
  }
}
