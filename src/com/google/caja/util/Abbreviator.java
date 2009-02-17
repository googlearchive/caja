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
    Map<String, String> abbrevToUri = new HashMap<String, String>();
    for (String item : items) { insert(abbrevToUri, item, ""); }
    for (Map.Entry<String, String> e : abbrevToUri.entrySet()) {
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

  private void insert(
      Map<String, String> abbrevToUri, String item, String abbrev) {
    abbrev = expand(item, abbrev);
    if (!abbrevToUri.containsKey(abbrev)) {
      abbrevToUri.put(abbrev, item);
    } else {
      String other = abbrevToUri.get(abbrev);
      if (!item.equals(other)) {
        if (!abbrev.equals(other)) {
          abbrevToUri.put(abbrev, null);
          if (other != null) { insert(abbrevToUri, other, abbrev); }
        }
        insert(abbrevToUri, item, abbrev);
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