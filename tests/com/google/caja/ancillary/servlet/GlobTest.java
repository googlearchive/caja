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

package com.google.caja.ancillary.servlet;

import com.google.caja.util.CajaTestCase;

import java.util.regex.Pattern;

public class GlobTest extends CajaTestCase {
  public final void testGlobToRegex() {
    assertGlob("", "");
    assertGlob("\\Qfoo\\E", "foo");
    assertGlob("\\Qfoo\\E.*", "foo*");
    assertGlob("\\Qfoo\\E.*\\Qbar\\E", "foo*bar");
    assertGlob("\\Qfoo\\E.", "foo?");
    assertGlob("\\Qfoo\\E.\\Qbar\\E", "foo?bar");
    assertGlob("\\Qfoo\\E\\Q*bar\\E", "foo\\*bar");
    assertGlob("\\Qfoo\\E\\Q?\\E", "foo\\?");
    assertGlob("\\Qfoo\\E\\Qn\\E", "foo\\n", "foon");
    assertGlob("\\Qfoo\\E\\QQ\\E", "foo\\Q", "fooQ");
  }

  public final void testRegexToGlob() {
    assertEquals("foo?*bar", Glob.regexToGlob(Pattern.compile("foo.+bar")));
    assertNotConvertibleToGlob("(foo)");
    assertNotConvertibleToGlob("[foo]");
    assertNotConvertibleToGlob("foo{2}");
    assertNotConvertibleToGlob("^foo");
    assertNotConvertibleToGlob("foo$");
  }

  private void assertGlob(String regex, String glob) {
    assertGlob(regex, glob, glob);
  }

  private void assertGlob(String regex, String glob, String normGlob) {
    Pattern p = Glob.globToRegex(glob);
    assertEquals(regex, p.pattern());
    assertEquals(normGlob, Glob.regexToGlob(p));
  }

  private void assertNotConvertibleToGlob(String regex) {
    Pattern p = Pattern.compile(regex);
    try {
      Glob.regexToGlob(p);
    } catch (IllegalArgumentException ex) {
      return;
    }
    fail("Not convertible to regex: " + regex);
  }
}
