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

import java.util.Formatter;
import java.util.List;
import java.util.ListIterator;

import junit.framework.Assert;
import junit.framework.ComparisonFailure;

/**
 * Extensions to junit.framework.Asserts that can be statically imported as by
 * {@code import static com.google.caja.util.MoreAsserts.*;}.
 *
 * @author mikesamuel@gmail.com
 */
// TODO(felix8a): remove SuppressWarnings after full conversion to junit4
@SuppressWarnings("deprecation")
public final class MoreAsserts {

  /**
   * Fails iff the contents of the two lists differ according to
   * {@code Object.equals}.
   * Tries to present the differences as nice diffs.
   */
  public static <T> void assertListsEqual(
      List<? extends T> expected, List<? extends T> actual) {
    assertListsEqual(expected, actual, 2);
  }

  /**
   * Fails iff the contents of the two lists differ according to
   * {@code Object.equals}.
   * Tries to present the differences as nice diffs.
   * <p>
   * TODO(mikesamuel): maybe actually diff using
   *     http://www.incava.org/projects/java/java-diff/
   *
   * @param diffContext the number of extra lines to show if there are errors.
   */
  public static <T> void assertListsEqual(
      List<? extends T> expected, List<? extends T> actual, int diffContext) {
    int m = expected.size();
    int n = actual.size();

    int commonPrefix = 0;
    {
      ListIterator<? extends T> i = expected.listIterator();
      ListIterator<? extends T> j = actual.listIterator();

      while (i.hasNext() && j.hasNext() && areEqual(i.next(), j.next())) {
        ++commonPrefix;
      }
    }

    if (commonPrefix == Math.max(m, n)) {
      // All are equal
      return;
    }

    int commonSuffix = 0;
    if (commonPrefix != Math.min(m, n)) {
      ListIterator<? extends T> i = expected.listIterator(m);
      ListIterator<? extends T> j = actual.listIterator(n);

      int max = Math.min(m, n) - commonPrefix;
      while (commonSuffix < max && i.hasPrevious() && j.hasPrevious()
             && areEqual(i.previous(), j.previous())) {
        ++commonSuffix;
      }
    }

    throw new ComparisonFailure(
        "Expected: {{{\n"
        + snippet(expected,
                  Math.max(commonPrefix - diffContext, 0),
                  Math.min(m, m - commonSuffix + diffContext), 84)
        + "\n}}} != {{{\n"
        + snippet(actual,
                  Math.max(commonPrefix - diffContext, 0),
                  Math.min(n, n - commonSuffix + diffContext), 84)
        + "\n}}}",
        snippet(expected, 0, expected.size(), Integer.MAX_VALUE),
        snippet(actual, 0, actual.size(), Integer.MAX_VALUE));
  }

  public static void assertStartsWith(String expected, String actual) {
    if (expected.length() > actual.length()) {
      Assert.fail("Expected string that starts with: {{{\n"
          + expected
          + "\n}}} != {{{\n"
          + actual
          + "\n}}}");
    } else {
      Assert.assertEquals(expected, actual.substring(0, expected.length()));
    }
  }

  private static String snippet(List<?> a, int start, int end, int maxlen) {
    StringBuilder sb = new StringBuilder();
    if (start != 0) {
      sb.append("\t...");
    }

    @SuppressWarnings("resource")
    Formatter f = new Formatter(sb);
    int index = start;
    for (Object item : a.subList(start, end)) {
      if (sb.length() != 0) { sb.append('\n'); }
      if (item != null) {
        String type = item.getClass().getSimpleName();
        f.format("\t%3d %s: %s", Integer.valueOf(index),
            abbreviatedString("" + item, maxlen - type.length()), type);
      } else {
        f.format("\t%3d <null>", Integer.valueOf(index));
      }
      ++index;
    }
    if (end < a.size()) {
      if (sb.length() != 0) { sb.append('\n'); }
      sb.append("\t...");
    }
    return sb.toString();
  }

  private static String abbreviatedString(String s, int maxLen) {
    s = s.replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("\r", "\\r");
    if (s.length() > maxLen) {
      System.err.println("<<<" + s + ">>>");
      int headLen = (maxLen - 3) / 2;
      int tailLen = maxLen - 3 - headLen;
      s = s.substring(0, headLen) + "..." + s.substring(s.length() - tailLen);
    }
    return "`" + s + "`";
  }

  private static boolean areEqual(Object a, Object b) {
    return a != null ? a.equals(b) : b == null;
  }

  private MoreAsserts() { /* not instantiable */ }
}
