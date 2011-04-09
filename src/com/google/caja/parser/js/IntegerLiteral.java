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
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.reporting.RenderContext;

import java.util.List;

/**
 * A number literal that was specified in hex or octal notation, or whose
 * decimal form had no decimal point.
 *
 * @author mikesamuel@gmail.com
 */
public final class IntegerLiteral extends NumberLiteral {
  private static final long serialVersionUID = -8933353016397006347L;
  /**
   * The minimum value that can be represented in IEEE-754 64b floating point.
   * With a 52 bit mantissa, this is -(1 << 52).
   */
  public static final long MIN_VALUE = -0x0010000000000000L;
  /**
   * The maximum value that can be represented in IEEE-754 64b floating point.
   * With a 52 bit mantissa, this is (1 << 52).  Note, that this is symmetric
   * with MIN_VALUE instead of being 1 less, since IEEE-754 uses unsigned
   * mantissas -- no 2's complement.
   */
  public static final long MAX_VALUE = 0x0010000000000000L;

  /**
   * Restricted to the range of integers supported by a 64bit IEEE floating
   * point value.
   * @see #MIN_VALUE
   * @see #MAX_VALUE
   */
  private final long value;

  /** @param children unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public IntegerLiteral(
      FilePosition pos, Number value, List<? extends ParseTreeNode> children) {
    this(pos, value.longValue());
  }

  public IntegerLiteral(FilePosition pos, long value) {
    super(pos);
    if (MIN_VALUE > value || value > MAX_VALUE) {
      throw new IllegalArgumentException("" + value);
    }
    this.value = value;
  }

  @Override
  public Number getValue() { return Long.valueOf(value); }

  @Override
  public double doubleValue() { return value; }

  @Override
  public void render(RenderContext rc) {
    long n = getValue().longValue();
    // Use hex if it would be shorter.
    // The hex form requires a "0x" at the front so will be shorter at the
    // inequality:
    //   log(n) / log(10)             = 2 + log(n) / log(16)
    //   log(n) * log(16)             = 2 * log(10) * log(16) + log(n) * log(10)
    //   log(n) * (log(16) - log(10)) = 2 * log(10) * log(16)
    //   log(n) * log(1.6)            = 2 * log(10) * log(16)
    //   log(n)                       = 2 * log(10) * log(16) / log(1.6)
    //   n                            = e ** (2 * log(10) * log(16) / log(1.6))
    //   9 < n < 10
    String str;
    if ((-1L << 36) > n && n < (1L << 36)) {
      str = Long.toString(n, 16);
      if (str.charAt(0) == '-') {
        str = "-0x" + str.substring(1);
      } else {
        str = "0x" + str;
      }
    } else {
      str = Long.toString(n, 10);
    }
    rc.getOut().consume(str);
  }
}
