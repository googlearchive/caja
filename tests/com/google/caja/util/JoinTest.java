// Copyright 2007 Google Inc. All Rights Reserved.

package com.google.caja.util;

import junit.framework.TestCase;

/**
 * @author msamuel@google.com (Mike Samuel)
 */
@SuppressWarnings("static-method")
public class JoinTest extends TestCase {
  public final void testJoin() {
    assertEquals("", Join.join(""));
    assertEquals("", Join.join("foo"));
    assertEquals("barFOObaz", Join.join("FOO", "bar", "baz"));
    assertEquals("barFOObazFOO", Join.join("FOO", "bar", "baz", ""));
    assertEquals(",,,", Join.join(",", "", "", "", ""));
  }
}
