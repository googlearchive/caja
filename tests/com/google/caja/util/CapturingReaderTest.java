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

import java.io.StringReader;
import java.io.IOException;

/**
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class CapturingReaderTest extends TestCase {
  public void testExpansion() throws Exception {
    String s = "The quick brown fox";
    CapturingReader cr = new CapturingReader(new StringReader(s), 0);
    for (int i = 0; i < s.length(); i++) {
      assertEquals(s.charAt(i), cr.read());
    }
    assertEquals(s, cr.getCapture().toString());
  }

  public void testPastEnd() throws Exception {
    CapturingReader cr = new CapturingReader(new StringReader("ab"));
    assertEquals('a', cr.read());
    assertEquals('b', cr.read());
    assertEquals(-1, cr.read());
    assertEquals("ab", cr.getCapture().toString());
  }

  public void testAutoClose() throws Exception {
    CapturingReader cr = new CapturingReader(new StringReader("abcdef"));
    assertEquals('a', cr.read());
    assertEquals('b', cr.read());
    assertEquals("ab", cr.getCapture().toString());
    try {
      cr.read();
      fail("getCapture() should have closed the reader");
    } catch (IOException e) {
      return;
    }
  }

  public void testManualClose() throws Exception {
    CapturingReader cr = new CapturingReader(new StringReader("abcdef"));
    assertEquals('a', cr.read());
    assertEquals('b', cr.read());
    cr.close();
    try {
      cr.read();
      fail("Reader should be closed");
    } catch (IOException e) {
      return;
    }
  }
}