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

import com.google.caja.SomethingWidgyHappenedError;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Combines strings around a separators.
 *
 * @author mikesamuel@gmail.com
 */
public class Join {

  /** Join items on separator. */
  public static String join(CharSequence sep, CharSequence... items) {
    int n = items.length;
    int sumOfLengths = sep.length() * n;
    for (int i = n; --i >= 0;) { sumOfLengths += items[i].length(); }
    StringBuilder sb = new StringBuilder(sumOfLengths);
    join(sb, sep, items);
    return sb.toString();
  }

  /** Join items on separator. */
  public static String join(
      CharSequence sep, Iterable<? extends CharSequence> items) {
    StringBuilder sb = new StringBuilder();
    join(sb, sep, items);
    return sb.toString();
  }

  /** Join items on separator, appending the result to out. */
  public static void join(
      Appendable out, CharSequence sep, Iterable<? extends CharSequence> items)
      throws IOException {
    Iterator<? extends CharSequence> it = items.iterator();
    if (!it.hasNext()) { return; }

    out.append(it.next());
    while (it.hasNext()) {
      out.append(sep).append(it.next());
    }
  }

  /** Join items on separator, appending the result to out. */
  public static void join(StringBuilder out, CharSequence sep,
                          Iterable<? extends CharSequence> items) {
    try {
      join((Appendable) out, sep, items);
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError(
          "StringBuilder does not throw IOException", ex);
    }
  }

  /** Join items on separator, appending the result to out. */
  public static void join(
      Appendable out, CharSequence sep, CharSequence... items)
      throws IOException {
    join(out, sep, Arrays.asList(items));
  }

  /** Join items on separator, appending the result to out. */
  public static void join(
      StringBuilder out, CharSequence sep, CharSequence... items) {
    try {
      join((Appendable) out, sep, Arrays.asList(items));
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError(
          "StringBuilder does not throw IOException", ex);
    }
  }
}
