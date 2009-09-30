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

import junit.framework.TestCase;

public class NameTest extends TestCase {
  public final void testCss() {
    assertEquals("color", Name.css("color").getCanonicalForm());
    assertEquals("color", Name.css("colOr").getCanonicalForm());
    assertEquals("color", Name.css("COLOR").getCanonicalForm());
  }

  public final void testHtml() {
    assertEquals("foo", Name.html("foo").getCanonicalForm());
    assertEquals("foo", Name.html("Foo").getCanonicalForm());
    assertEquals("foo", Name.html("FOO").getCanonicalForm());
    assertEquals("foo:bar", Name.html("foo:bar").getCanonicalForm());
    assertEquals("Foo:Bar", Name.html("Foo:Bar").getCanonicalForm());
    assertEquals("FOO:BAR", Name.html("FOO:BAR").getCanonicalForm());
  }

  public final void testXml() {
    assertEquals("foo", Name.xml("foo").getCanonicalForm());
    assertEquals("Foo", Name.xml("Foo").getCanonicalForm());
    assertEquals("FOO", Name.xml("FOO").getCanonicalForm());
    assertEquals("foo:bar", Name.xml("foo:bar").getCanonicalForm());
    assertEquals("Foo:Bar", Name.xml("Foo:Bar").getCanonicalForm());
    assertEquals("FOO:BAR", Name.xml("FOO:BAR").getCanonicalForm());
  }
}
