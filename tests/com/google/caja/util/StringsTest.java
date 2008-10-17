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

public class StringsTest extends TestCase {
  public void testEqualsIgnoreCase() {
    assertTrue(Strings.equalsIgnoreCase(null, null));
    assertTrue(Strings.equalsIgnoreCase("", ""));
    assertTrue(Strings.equalsIgnoreCase("foo", "foo"));
    assertTrue(Strings.equalsIgnoreCase("foo", "FOO"));
    assertTrue(Strings.equalsIgnoreCase("FOO", "foo"));
    assertTrue(Strings.equalsIgnoreCase("123", "123"));
    assertTrue(Strings.equalsIgnoreCase("FOO-bar", "foo-BAR"));
    assertFalse(Strings.equalsIgnoreCase(null, ""));
    assertFalse(Strings.equalsIgnoreCase("", null));
    assertFalse(Strings.equalsIgnoreCase("", "foo"));
    assertFalse(Strings.equalsIgnoreCase("food", "foo"));
    assertFalse(Strings.equalsIgnoreCase("foo", "FOOd"));
    assertFalse(Strings.equalsIgnoreCase("123", "456"));
    assertFalse(Strings.equalsIgnoreCase("\u0391", "\u03B1"));
  }

  public void testIsLowerCase() {
    assertTrue(Strings.isLowerCase(""));
    assertTrue(Strings.isLowerCase("foo"));
    assertTrue(Strings.isLowerCase("123"));
    assertTrue(Strings.isLowerCase("foo-bar"));
    assertTrue(Strings.isLowerCase("\u0391"));
    assertTrue(Strings.isLowerCase("\u03B1"));
    assertFalse(Strings.isLowerCase("FOO"));
    assertFalse(Strings.isLowerCase("Foo"));
    assertFalse(Strings.isLowerCase("fooD"));
    assertFalse(Strings.isLowerCase("fooD!"));
  }

  public void testToLowerCase() {
    assertEquals("", Strings.toLowerCase(""));
    assertEquals("foo", Strings.toLowerCase("foo"));
    assertEquals("foo", Strings.toLowerCase("FOO"));
    assertEquals("foo", Strings.toLowerCase("Foo"));
    assertEquals("123", Strings.toLowerCase("123"));
    assertEquals("foo-bar", Strings.toLowerCase("foo-BAR"));
    assertEquals("foo-bar", Strings.toLowerCase("FOO-bar"));
    assertEquals("food", Strings.toLowerCase("food"));
    assertEquals("food", Strings.toLowerCase("FOOd"));
    assertEquals("456", Strings.toLowerCase("456"));
    assertEquals("\u0391", Strings.toLowerCase("\u0391"));
    assertEquals("\u03B1", Strings.toLowerCase("\u03B1"));
    assertEquals(
        " !\"#$%&\\'()*+,-./0123456789:;<=>?@abcdefghijklmn"
        + "opqrstuvwxyz[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~",
        Strings.toLowerCase(
            " !\"#$%&\\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMN"
            + "OPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"));
  }

  public void testToUpperCase() {
    assertEquals("", Strings.toUpperCase(""));
    assertEquals("FOO", Strings.toUpperCase("foo"));
    assertEquals("FOO", Strings.toUpperCase("FOO"));
    assertEquals("FOO", Strings.toUpperCase("Foo"));
    assertEquals("123", Strings.toUpperCase("123"));
    assertEquals("FOO-BAR", Strings.toUpperCase("foo-BAR"));
    assertEquals("FOO-BAR", Strings.toUpperCase("FOO-bar"));
    assertEquals("FOOD", Strings.toUpperCase("food"));
    assertEquals("FOOD", Strings.toUpperCase("FOOd"));
    assertEquals("456", Strings.toUpperCase("456"));
    assertEquals("\u0391", Strings.toUpperCase("\u0391"));
    assertEquals("\u03B1", Strings.toUpperCase("\u03B1"));
    assertEquals(
        " !\"#$%&\\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMN"
        + "OPQRSTUVWXYZ[\\]^_`ABCDEFGHIJKLMNOPQRSTUVWXYZ{|}~",
        Strings.toUpperCase(
            " !\"#$%&\\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMN"
            + "OPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"));
  }
}
