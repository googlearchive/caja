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

package com.google.caja.parser;

import junit.framework.TestCase;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
@SuppressWarnings("static-method")
public class ParserBaseTest extends TestCase {
  public final void testIsJavascriptIdentifier() {
    for (String s : new String[] {
             "$", "_", "$$", "_foo_bar", "fooBar", "h3", "i", "___",
             "__proto__", "FOO_BAR_BAZ_", "i18n", "ev\\u0061l",
         }) {
      assertTrue(s, ParserBase.isJavascriptIdentifier(s));
    }
    for (String s : new String[] {
            "\u0101\u0107\u0115", "\\u0101\\u0107\\u0115",
         }) {
      assertEquals(
          s, true, ParserBase.isJavascriptIdentifier(s));
    }
    for (String s : new String[] {
             "", "3$", "1", "\u0000", "", "a-b", "a.b", "3_", "a=b",
             "(a)", "'a'", "\\a", "\\u012", "\\u", "\\u0000", "ev\u0060l",
         }) {
      assertFalse(s, ParserBase.isJavascriptIdentifier(s));
    }
  }

  public final void testDecodeIdentifier() {
    assertEquals("eval", ParserBase.decodeIdentifier("eval"));
    assertEquals("eval", ParserBase.decodeIdentifier("ev\\u0061l"));
  }
}
