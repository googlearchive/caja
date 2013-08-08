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

package com.google.caja.parser.js;

import java.math.BigDecimal;
import junit.framework.TestCase;

@SuppressWarnings("static-method")
public class NumberLiteralTest extends TestCase {
  public final void testNumberToString() {
    assertEquals("NaN", NumberLiteral.numberToString(Double.NaN));
    assertEquals("Infinity",
                 NumberLiteral.numberToString(Double.POSITIVE_INFINITY));
    assertEquals("-Infinity",
                 NumberLiteral.numberToString(Double.NEGATIVE_INFINITY));
    assertEquals("0", NumberLiteral.numberToString(0.0d));
    assertEquals("0", NumberLiteral.numberToString(-0.0d));
    assertEquals("1", NumberLiteral.numberToString(1.0d));
    assertEquals("-1", NumberLiteral.numberToString(-1.0d));
    assertNumberToString("-0.1", "-0.1");
    assertNumberToString("0.1", ".1");
    assertNumberToString("0.1", "00.100");
    assertNumberToString("0.00001", "0.00001");
    assertNumberToString("0.000011", "0.000011");
    assertNumberToString("0.000099", "0.000099");
    assertNumberToString("0.0000999999999999999999999",
                         "0.0000999999999999999999999999");
    assertNumberToString("0.000199999999999999999999",
                         "0.000199999999999999999999999");
    assertNumberToString("0.000199999999999999999999",
                         "0.000199999999999999999999989");
    assertNumberToString("9.9e-25", "0.00000000000000000000000099");
    assertNumberToString("1e+31", "10000000000000000000000000000000");
    assertNumberToString("1.1e+31", "11000000000000000000000000000000");
    assertNumberToString("9.9e+31", "99000000000000000000000000000000");
    // Not rounded up
    assertNumberToString("9.99999999999999999999e+31",
                         "99999999999999999999999999999999");
  }

  private static void assertNumberToString(String golden, String input) {
    assertEquals(
        input, golden, NumberLiteral.numberToString(new BigDecimal(input)));
  }
}
