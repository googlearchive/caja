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

import junit.framework.TestCase;

@SuppressWarnings("static-method")
public class StringsTest extends TestCase {
  public final void testEqIgnoreCase() {
    assertTrue(Strings.eqIgnoreCase(null, null));
    assertTrue(Strings.eqIgnoreCase("", ""));
    assertTrue(Strings.eqIgnoreCase("foo", "foo"));
    assertTrue(Strings.eqIgnoreCase("foo", "FOO"));
    assertTrue(Strings.eqIgnoreCase("FOO", "foo"));
    assertTrue(Strings.eqIgnoreCase("123", "123"));
    assertTrue(Strings.eqIgnoreCase("FOO-bar", "foo-BAR"));
    assertTrue(Strings.eqIgnoreCase("FOO^bar", "foo^BAR"));
    assertTrue(Strings.eqIgnoreCase("FOO bar", "foo BAR"));
    assertTrue(Strings.eqIgnoreCase("FOO~bar", "foo~BAR"));
    assertTrue(Strings.eqIgnoreCase("foo-BAR", "FOO-bar"));
    assertTrue(Strings.eqIgnoreCase("foo^BAR", "FOO^bar"));
    assertTrue(Strings.eqIgnoreCase("foo BAR", "FOO bar"));
    assertTrue(Strings.eqIgnoreCase("foo~BAR", "FOO~bar"));
    // Unequal due to characters on various sides of the letter blocks.
    assertFalse(Strings.eqIgnoreCase("foo-BAR", "FOO^bar"));
    assertFalse(Strings.eqIgnoreCase("foo^BAR", "FOO-bar"));
    assertFalse(Strings.eqIgnoreCase("foo BAR", "FOO~bar"));
    assertFalse(Strings.eqIgnoreCase("foo~BAR", "FOO bar"));
    // Check chars one below [aA] and one above [zZ]
    assertFalse(Strings.eqIgnoreCase("@", "`"));
    assertFalse(Strings.eqIgnoreCase("`", "@"));
    assertFalse(Strings.eqIgnoreCase("[", "{"));
    assertFalse(Strings.eqIgnoreCase("{", "["));
    // More unequal strings
    assertFalse(Strings.eqIgnoreCase(null, ""));
    assertFalse(Strings.eqIgnoreCase("", null));
    assertFalse(Strings.eqIgnoreCase("", "foo"));
    assertFalse(Strings.eqIgnoreCase("food", "foo"));
    assertFalse(Strings.eqIgnoreCase("foo", "FOOd"));
    assertFalse(Strings.eqIgnoreCase("123", "456"));
    assertTrue(Strings.eqIgnoreCase("\u0391", "\u03b1"));
    assertTrue(Strings.eqIgnoreCase(
        " !\"#$%&\\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMN"
        + "OPQRSTUVWXYZ[\\]^_`ABCDEFGHIJKLMNOPQRSTUVWXYZ{|}~",
        " !\"#$%&\\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMN"
        + "OPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"));
    assertTrue(Strings.eqIgnoreCase(
        " !\"#$%&\\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMN"
        + "OPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~",
        " !\"#$%&\\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMN"
        + "OPQRSTUVWXYZ[\\]^_`ABCDEFGHIJKLMNOPQRSTUVWXYZ{|}~"));
    assertTrue(Strings.eqIgnoreCase(
        " !\"#$%&\\'()*+,-./0123456789:;<=>?@abcdefghijklmn"
        + "opqrstuvwxyz[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~",
        " !\"#$%&\\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMN"
        + "OPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"));
    assertTrue(Strings.eqIgnoreCase(
        " !\"#$%&\\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMN"
        + "OPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~",
        " !\"#$%&\\'()*+,-./0123456789:;<=>?@abcdefghijklmn"
        + "opqrstuvwxyz[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"));
  }

  public final void testLower() {
    assertEquals("", Strings.lower(""));
    assertEquals("foo", Strings.lower("foo"));
    assertEquals("foo", Strings.lower("FOO"));
    assertEquals("foo", Strings.lower("Foo"));
    assertEquals("123", Strings.lower("123"));
    assertEquals("foo-bar", Strings.lower("foo-BAR"));
    assertEquals("foo-bar", Strings.lower("FOO-bar"));
    assertEquals("food", Strings.lower("food"));
    assertEquals("food", Strings.lower("FOOd"));
    assertEquals("456", Strings.lower("456"));
    assertEquals("\u03b1", Strings.lower("\u0391"));
    assertEquals("\u03b1", Strings.lower("\u03B1"));
    assertEquals("@`[{", Strings.lower("@`[{"));
    assertEquals(
        " !\"#$%&\\'()*+,-./0123456789:;<=>?@abcdefghijklmn"
        + "opqrstuvwxyz[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~",
        Strings.lower(
            " !\"#$%&\\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMN"
            + "OPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"));
  }

  public final void testUpper() {
    assertEquals("", Strings.upper(""));
    assertEquals("FOO", Strings.upper("foo"));
    assertEquals("FOO", Strings.upper("FOO"));
    assertEquals("FOO", Strings.upper("Foo"));
    assertEquals("123", Strings.upper("123"));
    assertEquals("FOO-BAR", Strings.upper("foo-BAR"));
    assertEquals("FOO-BAR", Strings.upper("FOO-bar"));
    assertEquals("FOOD", Strings.upper("food"));
    assertEquals("FOOD", Strings.upper("FOOd"));
    assertEquals("456", Strings.upper("456"));
    assertEquals("\u0391", Strings.upper("\u0391"));
    assertEquals("\u0391", Strings.upper("\u03B1"));
    assertEquals("@`[{", Strings.upper("@`[{"));
    assertEquals(
        " !\"#$%&\\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMN"
        + "OPQRSTUVWXYZ[\\]^_`ABCDEFGHIJKLMNOPQRSTUVWXYZ{|}~",
        Strings.upper(
            " !\"#$%&\\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMN"
            + "OPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"));
  }
}
