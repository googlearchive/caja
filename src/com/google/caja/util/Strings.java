// Copyright (C) 2008 Google Inc.
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

import java.util.Locale;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Locale independent versions of String case-insensitive operations.
 * <p>
 * The normal case insensitive operators {@link String#toLowerCase}
 * and {@link String#equalsIgnoreCase} depend upon the current locale.
 * In the Turkish locale, uppercasing "i" yields a dotted I "\u0130",
 * and lowercasing "I" yields a dotless i "\u0131".
 * <p>
 * These are convenience methods for avoiding that problem.
 * <p>
 * Note, regex matching does not have this problem, because
 * Pattern.CASE_INSENSITIVE is ascii-only case folding unless you
 * also ask for Pattern.UNICODE_CASE.
 * <p>
 * @author mikesamuel@gmail.com, felix8a@gmail.com
 */
@ParametersAreNonnullByDefault
public final class Strings {

  /* Avoids Turkish 'i' problem. */
  public static boolean eqIgnoreCase(@Nullable String a, @Nullable String b) {
    if (a == null) { return b == null; }
    if (b == null) { return false; }
    return lower(a).equals(lower(b));
  }

  /** Avoids Turkish 'i' problem. */
  public static String lower(String s) {
    return s.toLowerCase(Locale.ENGLISH);
  }

  /** Avoids Turkish 'i' problem. */
  public static String upper(String s) {
    return s.toUpperCase(Locale.ENGLISH);
  }

  private Strings() { /* uninstantiable */ }
}
