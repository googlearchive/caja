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

package com.google.caja.parser.js;

import com.google.caja.lexer.FilePosition;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * A literal numeric value.  This class is abstract so that we can distinguish
 * literals based on the format they were parsed from.
 * If in doubt about which implementation to use, use {@link RealLiteral}.
 *
 * @author mikesamuel@gmail.com
 */
public abstract class NumberLiteral extends Literal {
  private static final long serialVersionUID = -5974378121634749014L;

  protected NumberLiteral(FilePosition pos) { super(pos); }

  @Override
  public abstract Number getValue();

  /** Same as {@code getValue().doubleValue()} */
  public abstract double doubleValue();

  @Override
  public boolean getValueInBooleanContext() {
    double n = doubleValue();
    return !(Double.isNaN(n) || 0D == n);
  }

  /**
   * Convert a number to a string using javascript rules.
   * <blockquote>
   * 9.8.1 ToString Applied to the Number Type
   *     <br><br>
   * The operator ToString converts a number m to string format as follows:
   * <ol>
   * <li>If m is NaN, return the string "NaN".
   * <li>If m is +0 or -0, return the string "0".
   * <li>If m is less than zero, return the string concatenation of the string
   *   "-" and ToString(-m).
   * <li>If m is infinity, return the string "Infinity".
   * <li>Otherwise, let n, k, and s be integers such that k >= 1,
   *   10**k-1 <= s < 10**k, the number value for s * 10**(n-k) is m,
   *   and k is as small as possible. Note that k is the number of
   *   digits in the decimal representation of s, that s is not
   *   divisible by 10, and that the least significant digit of s is not
   *   necessarily uniquely determined by these criteria.
   * <li>If k <= n <= 21, return the string consisting of the k digits
   *   of the decimal representation of s (in order, with no leading
   *   zeroes), followed by n-k occurrences of the character '0'.
   * <li>If 0 < n <= 21, return the string consisting of the
   *   most significant n digits of the decimal representation of s,
   *   followed by a decimal point '.', followed by the
   *   remaining k-n digits of the decimal representation of s.
   * <li>If -6 < n <= 0, return the string consisting of the
   *   character '0', followed by a decimal point '.', followed by -n
   *   occurrences of the character '0', followed by the k digits of
   *   the decimal representation of s.
   * <li>Otherwise, if k = 1, return the string consisting of the
   *   single digit of s, followed by lowercase character 'e', followed
   *   by a plus sign '+' or minus sign '-' according to whether n-1 is
   *   positive or negative, followed by the decimal representation of
   *   the integer abs(n-1) (with no leading zeros).
   * <li>Return the string consisting of the most significant digit
   *   of the decimal representation of s, followed by a decimal point
   *   '.', followed by the remaining k-1 digits of the decimal
   *   representation of s, followed by the lowercase character 'e',
   *   followed by a plus sign '+' or minus sign '-' according to
   *   whether n-1 is positive or negative, followed by the decimal
   *   representation of the integer abs(n-1) (with no leading zeros).
   * </ol>
   */
  public static String numberToString(double m) {
    if (Double.isNaN(m)) { return "NaN"; }
    if (m == 0) { return "0"; }  // incl. -0
    if (Double.isInfinite(m)) {
      return m >= 0 ? "Infinity" : "-Infinity";
    }
    return numberToString(new BigDecimal(m, TWENTY_ONE_DIGITS_ROUNDED_TO_ZERO));
  }

  private static final MathContext TWENTY_ONE_DIGITS_ROUNDED_TO_ZERO
      = new MathContext(21, RoundingMode.DOWN);

  public static String numberToString(BigDecimal m) {
    int cmp = BigDecimal.ZERO.compareTo(m);
    if (cmp == 0) {
      return "0";
    }
    if (cmp > 0) {
      return "-" + numberToString(m.abs());
    }

    // Round to 21 digits worth and adjust n-k
    if (m.precision() > 21) {
      m = new BigDecimal(m.unscaledValue(), m.scale(),
                         TWENTY_ONE_DIGITS_ROUNDED_TO_ZERO);
      assert m.precision() <= 21;
    }

    m = m.stripTrailingZeros();

    // Choose n, k, and s such that k >= 1, 10**k-1 <= s < 10**k,
    // the number value for s * 10**(n-k) is m, and k is as small as possible.

    int n, k;
    BigDecimal s;

    s = new BigDecimal(m.unscaledValue(), 0);

    // 10**k is the greatest power of 10 <= s, i.e. k = floor(log10(s))
    k = s.precision() - s.scale();

    // (1) s     = m * 10**m.scale()   by def of BigDecimal.unscaledValue()
    // (2) m     = s * 10**(n-k)       by def of s,n,k
    // (3) m     = s * 10**-m.scale()  from 1
    // (4) n - k = -m.scale()          by 2, 3
    // (5) n     = k - m.scale()
    n = k - m.scale();

    String intRep = s.unscaledValue().toString();  // independent of Locale

    if (k <= n && n <= 21) {
      // 6. If k <= n <= 21, return the string consisting of the k digits
      //   of the decimal representation of s (in order, with no leading
      //   zeroes), followed by n-k occurrences of the character '0'.
      StringBuilder sb = new StringBuilder(n);
      sb.append(intRep.substring(0, k));
      while (sb.length() < n) { sb.append('0'); }
      return sb.toString();
    } else if (0 < n && n <= 21) {
      // 7. If 0 < n <= 21, return the string consisting of the
      //   most significant n digits of the decimal representation of s,
      //   followed by a decimal point '.', followed by the
      //   remaining k-n digits of the decimal representation of s.
      return intRep.substring(0, n) + "."
          + intRep.substring(n, Math.min(intRep.length(), 21));
    } else if (-6 < n && n <= 0) {
      // 8. If -6 < n <= 0, return the string consisting of the
      //   character '0', followed by a decimal point '.', followed by -n
      //   occurrences of the character '0', followed by the k digits of
      //   the decimal representation of s.
      StringBuilder sb = new StringBuilder(2 + -n + k);
      sb.append("0.");
      for (int i = -n; --i >= 0;) { sb.append('0'); }
      sb.append(intRep, 0, k);
      return sb.toString();
    } else if (k == 1) {
      // 9. Otherwise, if k = 1, return the string consisting of the
      //   single digit of s, followed by lowercase character 'e', followed
      //   by a plus sign '+' or minus sign '-' according to whether n-1 is
      //   positive or negative, followed by the decimal representation of
      //   the integer abs(n-1) (with no leading zeros).
      return intRep + ((n - 1) < 0 ? "e-" : "e+") + Math.abs(n - 1);
    } else {
      // 10. Return the string consisting of the most significant digit
      //   of the decimal representation of s, followed by a decimal point
      //   '.', folloarwed by the remaining k-1 digits of the decimal
      //   representation of s, followed by the lowercase character 'e',
      //   followed by a plus sign '+' or minus sign '-' according to
      //   whether n-1 is positive or negative, followed by the decimal
      //   representation of the integer abs(n-1) (with no leading zeros).
      return intRep.substring(0, 1) + "." + intRep.substring(1, k)
          + ((n - 1) < 0 ? "e-" : "e+") + Math.abs(n - 1);
    }
  }

  public String typeOf() { return "number"; }
}
