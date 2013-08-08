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

import com.google.caja.lexer.Keyword;
import com.google.caja.util.SafeIdentifierMaker;

import java.util.Iterator;
import junit.framework.TestCase;

@SuppressWarnings("static-method")
public class SafeIdentifierMakerTest extends TestCase {
  public final void testSeries() {
    Iterator<String> maker = new SafeIdentifierMaker("abc".toCharArray());
    assertEquals("a", maker.next());
    assertEquals("b", maker.next());
    assertEquals("c", maker.next());
    assertEquals("aa", maker.next());
    assertEquals("ab", maker.next());
    assertEquals("ac", maker.next());
    assertEquals("ba", maker.next());
    assertEquals("bb", maker.next());
    assertEquals("bc", maker.next());
    assertEquals("ca", maker.next());
    assertEquals("cb", maker.next());
    assertEquals("cc", maker.next());
    assertEquals("aaa", maker.next());
    assertEquals("aab", maker.next());
    assertEquals("aac", maker.next());
    assertEquals("aba", maker.next());
  }

  public final void testNoKeywords() {
    assertTrue(Keyword.isKeyword("in"));
    Iterator<String> maker = new SafeIdentifierMaker("in".toCharArray());
    assertEquals("i", maker.next());
    assertEquals("n", maker.next());
    assertEquals("ii", maker.next());
    // "in" not emitted
    assertEquals("ni", maker.next());
    assertEquals("nn", maker.next());
    assertEquals("iii", maker.next());
    assertEquals("iin", maker.next());
    assertEquals("ini", maker.next());
    assertEquals("inn", maker.next());
  }

  public final void testNoEval() {
    Iterator<String> maker = new SafeIdentifierMaker("aelv".toCharArray());
    assertEquals("a", maker.next());
    assertEquals("e", maker.next());
    assertEquals("l", maker.next());
    assertEquals("v", maker.next());
    assertEquals("aa", maker.next());
    assertEquals("ae", maker.next());
    assertEquals("al", maker.next());
    assertEquals("av", maker.next());
    // Skip the rest of the 2s
    for (int i = 12; --i >= 0;) {  // 12 = |(e,l,v)x(a,e,l,v)|
      String s = maker.next();
      assertEquals(s, 2, s.length());
    }
    // Skip the rest of the 3s
    for (int i = 64; --i >= 0;) {
      String s = maker.next();
      assertEquals(s, 3, s.length());
    }
    // Skip the 4s that start with a
    for (int i = 64; --i >= 0;) {
      String s = maker.next();
      assertEquals(s, 4, s.length());
      assertTrue(s, s.startsWith("a"));
    }
    // Skip the 4s that start with e[~v]
    for (int i = 48; --i >= 0;) {
      String s = maker.next();
      assertEquals(s, 4, s.length());
      assertTrue(s, s.startsWith("e") && !s.startsWith("ev"));
    }
    assertEquals("evaa", maker.next());
    assertEquals("evae", maker.next());
    assertEquals("evav", maker.next());
    // Skip the 4s that start with eve, evl, and evv
    for (int i = 12; --i >= 0;) {
      String s = maker.next();
      assertEquals(s, 4, s.length());
      assertTrue(
          s, s.startsWith("eve") || s.startsWith("evl") || s.startsWith("evv"));
    }
    // Skip the 4s that start with l and v
    for (int i = 128; --i >= 0;) {
      String s = maker.next();
      assertEquals(s, 4, s.length());
      assertTrue(s, s.startsWith("l") || s.startsWith("v"));
    }
    assertEquals("aaaaa", maker.next());
  }
}
