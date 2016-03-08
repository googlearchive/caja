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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A mapping from a group of strings with a particular format to suffixes that
 * unambiguously identify the original.
 *
 * <p>
 * If we needed to abbreviate the set of file paths:
 * <ul>
 * <li><tt>/tmp/foo/bar.txt</tt>
 * <li><tt>/tmp/baz.txt</tt>
 * <li><tt>/tmp/boo/bar.txt</tt>
 * </ul>
 * This class would come up with the mapping
 * <ul>
 * <li><tt>/tmp/foo/bar.txt</tt> &rarr; <tt>foo/bar.txt</tt>
 * <li><tt>/tmp/baz.txt</tt> &rarr; <tt>baz.txt</tt>
 * <li><tt>/tmp/boo/bar.txt</tt> &rarr; <tt>boo/bar.txt</tt>
 * </ul>
 * by choosing the shortest suffixes that start after the <code>/</code>
 * separator for each item that are not suffixes of any other element in the
 * set.
 *
 * @author mikesamuel@gmail.com
 */
public final class Abbreviator {
  private final Map<String, String> itemToAbbrev = new HashMap<String, String>();
  private final String sep;

  /**
   * @param items items to generate abbreviations for.
   * @param sep a string that is likely to appear as a substring of an item
   *    repeatedly.  A suitable sep for file paths or URIs might be {@code "/"}.
   */
  public Abbreviator(Set<String> items, String sep) {
    this.sep = sep;
    Map<String, String> abbrevToItem = new HashMap<String, String>();
    for (String item : items) { insert(abbrevToItem, item, ""); }
    for (Map.Entry<String, String> e : abbrevToItem.entrySet()) {
      if (e.getValue() != null) {
        itemToAbbrev.put(e.getValue(), e.getKey());
      }
    }
  }

  /**
   * If item was in the input set, then the shortest unambiguous suffix of item
   * that is not a suffix of any other item in the input set, or item if item
   * was not in the input set.
   */
  public String unambiguousAbbreviationFor(String item) {
    String abbrev = itemToAbbrev.get(item);
    return abbrev != null ? abbrev : item;
  }

  /**
   * Insert an entry in a map of abbreviations to items.
   *
   * @param abbrevToItem The table. Null-valued entries mark ambiguous
   *        abbreviations.
   * @param item The unabbreviated item to insert.
   * @param abbrev The shortest abbreviation so far known to be insufficiently
   *        specific; the inserted abbreviation will always be longer than this
   *        if possible.
   */
  private void insert(
      Map<String, String> abbrevToItem, String item, String abbrev) {
    // Find the next longer abbreviation to attempt.
    abbrev = expand(item, abbrev);
    if (!abbrevToItem.containsKey(abbrev)) {
      // It is unambiguous; just insert it.
      abbrevToItem.put(abbrev, item);
    } else {
      // It conflicts with an existing (longer or equal) abbreviation.
      String other = abbrevToItem.get(abbrev);
      if (!item.equals(other)) {  // Skip if exact item already present.
        if (!abbrev.equals(other)) {
          // The other item can be expressed longer.
          // (If this condition is false, then the other item is a suffix of
          // this one, and so the other item is left as-is.)

          // Mark this abbreviation as ambiguous.
          abbrevToItem.put(abbrev, null);
          // Re-insert the other item with its longer abbreviation.
          if (other != null) { insert(abbrevToItem, other, abbrev); }
        }
        if (other == null && item.equals(abbrev)) {
          // Item is a suffix of an existing item. Insert it and do not attempt
          // to find a longer abbreviation (which would infinitely recurse).
          abbrevToItem.put(abbrev, item);
        } else {
          // Try again to insert this item, with a longer abbreviation.
          insert(abbrevToItem, item, abbrev);
        }
      }
    }
  }

  /**
   * Returns a longer suffix of original.
   * @param item a path like <tt>/a/b/c</tt>.
   * @param abbrev a suffix of item like <tt>c</tt>.
   * @return item or a suffix of item that is longer than abbrev and which
   *     starts after a '/' character in item.
   */
  private String expand(String item, String abbrev) {
    int seplen = sep.length();
    int suffixStart = item.length() - (abbrev.length() + 1);
    int slash = item.lastIndexOf(sep, suffixStart - seplen);
    return slash >= 0 ? item.substring(slash + seplen) : item;
  }
}