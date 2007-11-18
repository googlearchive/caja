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

import java.util.Arrays;
import java.util.TreeSet;

/**
 * A trie used to separate punctuation tokens in a run of non-whatespace
 * characters by preferring the longest punctution string possible in a
 * greedy left-to-right scan.
 *
 * @author mikesamuel@gmail.com
 */
final class PunctuationTrie {
  private final char[] childMap;
  private final PunctuationTrie[] children;
  private final boolean terminal;

  /**
   * @param punctuationStrings not empty, non null.
   */
  PunctuationTrie(String[] punctuationStrings) {
    this(sortedUniqCopy(punctuationStrings), 0, 0, punctuationStrings.length);
  }

  /**
   * @param punctuationStrings not empty, non null.  Must not be modified.
   * @param depth the depth in the tree.
   * @param start an index into punctuationStrings of the first string in this
   *   subtree.
   * @param end an index into punctuationStrings past the last string in this
   *   subtree.
   */
  private PunctuationTrie(
      String[] punctuationStrings, int depth, int start, int end) {
    this.terminal = depth == punctuationStrings[start].length();
    if (this.terminal) {
      if (start + 1 == end) {  // base case
        this.childMap = ZERO_CHARS;
        this.children = ZERO_TRIES;
        return;
      } else {
        ++start;
      }
    }
    int childCount = 0;
    {
      int last = -1;
      for (int i = start; i < end; ++i) {
        char ch = punctuationStrings[i].charAt(depth);
        if (ch != last) {
          ++childCount;
          last = ch;
        }
      }
    }
    this.childMap = new char[childCount];
    this.children = new PunctuationTrie[childCount];
    int childStart = start;
    int childIndex = 0;
    char lastCh = punctuationStrings[start].charAt(depth);
    for (int i = start + 1; i < end; ++i) {
      char ch = punctuationStrings[i].charAt(depth);
      if (ch != lastCh) {
        childMap[childIndex] = lastCh;
        children[childIndex++] = new PunctuationTrie(
          punctuationStrings, depth + 1, childStart, i);
        childStart = i;
        lastCh = ch;
      }
    }
    childMap[childIndex] = lastCh;
    children[childIndex++] = new PunctuationTrie(
        punctuationStrings, depth + 1, childStart, end);
  }

  /** Does this node correspond to a complete string in the input set. */
  public boolean isTerminal() { return this.terminal; }

  /**
   * The child corresponding to the given character.
   * @return null if no such trie.
   */
  public PunctuationTrie lookup(char ch) {
    int i = Arrays.binarySearch(childMap, ch);
    return i >= 0 ? children[i] : null;
  }

  /**
   * The descendent of this trie corresponding to the string for this trie
   * appended with s.
   * @param s non null.
   * @return null if no such trie.
   */
  public PunctuationTrie lookup(CharSequence s) {
    PunctuationTrie t = this;
    for (int i = 0, n = s.length(); i < n; ++i) {
      t = t.lookup(s.charAt(i));
      if (null == t) { break; }
    }
    return t;
  }

  public boolean contains(char ch) {
    return Arrays.binarySearch(childMap, ch) >= 0;
  }

  private static String[] sortedUniqCopy(String[] arr) {
    return new TreeSet<String>(Arrays.asList(arr)).toArray(new String[0]);
  }

  private static final char[] ZERO_CHARS = new char[0];
  private static final PunctuationTrie[] ZERO_TRIES = new PunctuationTrie[0];

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
