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

package com.google.caja.parser;

import com.google.caja.SomethingWidgyHappenedError;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Provides a similar interface to {@code java.text.Normalizer}, but will
 * compile and run on JDK1.5 in a stricter mode.
 *
 * @author mikesamuel@gmail.com
 */
final class Normalizer {
  private static final Method IS_NORMALIZED;
  private static final Object NORMAL_FORM_C;

  static {
    Method isNormalized = null;
    Object normalFormC = null;
    try {
      Class<?> normalizer = Class.forName("java.text.Normalizer");
      Class<?> normalizerForm = Class.forName("java.text.Normalizer$Form");

      isNormalized = normalizer.getMethod(
          "isNormalized", CharSequence.class, normalizerForm);
      normalFormC = normalizerForm.getField("NFC").get(null);
    } catch (ClassNotFoundException ex) {
      // JVM versions < 1.5 don't provide Normalizer.
      // Use heuristic below.
    } catch (IllegalAccessException ex) {
      throw new SomethingWidgyHappenedError(
          "Normalizer exists but is unexpectedly inaccessible", ex);
    } catch (NoSuchFieldException ex) {
    	// AppEngine doesn't provide Normalizer.Form.
    	// Use heuristic below.
      throw new SomethingWidgyHappenedError(
          "Normalizer.Form unexpectedly missing", ex);
    } catch (NoSuchMethodException ex) {
        // Don't use the normalizer.
        // Use heuristic below.
      throw new SomethingWidgyHappenedError(
          "Normalizer unexpectedly missing methods", ex);
    }

    IS_NORMALIZED = isNormalized;
    NORMAL_FORM_C = normalFormC;
  }

  /**
   * A conservative heuristic as to whether s is normalized according to Unicode
   * Normal Form C.  It is heuristic, because Caja needs to run with versions
   * of the Java standard libraries that do not include normalization.
   * @return false if s is not normalized.
   */
  public static boolean isNormalized(CharSequence s) {
    if (IS_NORMALIZED != null) {
      try {
        return ((Boolean) IS_NORMALIZED.invoke(null, s, NORMAL_FORM_C))
            .booleanValue();
      } catch (IllegalAccessException ex) {
        throw new SomethingWidgyHappenedError(
            "Normalizer unexpectedly uninvokable", ex);
      } catch (InvocationTargetException ex) {
        Throwable th = ex.getTargetException();
          throw new SomethingWidgyHappenedError(
              "Normalizer unexpectedly uninvokable", th);
      }
    }

    // From http://unicode.org/reports/tr15/#D6
    // Legacy character sets are classified into three categories
    // based on their normalization behavior with accepted
    // transcoders.
    // 1. Prenormalized. Any string in the character set is already in
    //    Normalization Form X.
    //    For example, ISO 8859-1 is prenormalized in NFC.
    // ...
    for (int i = s.length(); --i >= 0;) {
      char ch = s.charAt(i);
      // Codepoints in [32, 126] U [160, 255] are identical in both Unicode and
      // ISO 8859-1.
      // Codepoints in [0, 31] and [127, 159] are not part of ISO 8859-1.  They
      // are control characters in Unicode, and disallowed in identifiers so
      // will never reach here.
      if (ch >= 256) { return false; }
    }
    return true;
  }
}
