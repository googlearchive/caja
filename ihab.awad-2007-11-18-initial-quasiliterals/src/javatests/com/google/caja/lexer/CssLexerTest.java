// Copyright (C) 2006 Google Inc.
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

package com.google.caja.lexer;

import com.google.caja.util.TestUtil;

import junit.framework.TestCase;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public class CssLexerTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testLex() throws Exception {
    CharProducer cp =
      TestUtil.getResourceAsProducer(getClass(), "csslexerinput1.css");
    CssLexer lexer = new CssLexer(cp, true);
    StringBuilder sb = new StringBuilder();
    while (lexer.hasNext()) {
      Token<CssTokenType> t = lexer.next();
      sb.append(abbr(t.type.name())).append(" [")
        .append(escape(t.text)).append("]: ").append(t.pos).append('\n');
      assert t.text.length() == t.pos.endCharInFile() - t.pos.startCharInFile()
           : t.text + ": " + t.pos;
    }
    assertEquals(TestUtil.readResource(getClass(), "csslexergolden1.txt")
                 .trim(),
                 sb.toString().trim());
  }

  private static final String escape(String s) {
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); ++i) {
      char ch = s.charAt(i);
      switch (ch) {
        case '\n': sb.append("\\n"); break;
        case '\r': sb.append("\\r"); break;
        case '\\': sb.append("\\\\"); break;
        default: sb.append(ch); break;
      }
    }
    return sb.toString();
  }

  private static String abbr(String s) {
    int n = s.length();
    return n < 4 ? s + "    ".substring(0, 4 - n) : s.substring(0, 4);
  }
}
