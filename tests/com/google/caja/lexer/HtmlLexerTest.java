// Copyright (C) 2005 Google Inc.
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

import com.google.caja.util.CajaTestCase;
import com.google.caja.util.MoreAsserts;
import com.google.caja.util.TestUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class HtmlLexerTest extends CajaTestCase {

  public void testHtmlLexer() throws Exception {
    // Do the lexing.
    CharProducer p = fromResource("htmllexerinput1.html");
    StringBuilder actual = new StringBuilder();
    try {
      HtmlLexer lexer = new HtmlLexer(p);
      lex(lexer, actual);
    } finally {
      p.close();
    }

    // Get the golden.
    String golden = TestUtil.readResource(getClass(), "htmllexergolden1.txt");

    // Compare.
    assertEquals(golden, actual.toString());
  }

  public void testXmlLexer() throws Exception {
    // Do the lexing.
    CharProducer p = fromResource("htmllexerinput2.xml");
    StringBuilder actual = new StringBuilder();
    try {
      HtmlLexer lexer = new HtmlLexer(p);
      lexer.setTreatedAsXml(true);
      lex(lexer, actual);
    } finally {
      p.close();
    }

    // Get the golden.
    String golden = TestUtil.readResource(getClass(), "htmllexergolden2.txt");

    // Compare.
    assertEquals(golden, actual.toString());
  }

  public void testEofInTag() throws Exception {
    assertTokens("<div", true, "TAGBEGIN: <div");
    assertTokens("</div", true, "TAGBEGIN: </div");
    assertTokens("<div\n", true, "TAGBEGIN: <div");
    assertTokens("</div\n", true, "TAGBEGIN: </div");
    assertTokens("<div", false, "TAGBEGIN: <div");
    assertTokens("</div", false, "TAGBEGIN: </div");
    assertTokens("<div\n", false, "TAGBEGIN: <div");
    assertTokens("</div\n", false, "TAGBEGIN: </div");
  }

  private void lex(HtmlLexer lexer, Appendable out) throws Exception {
    int maxTypeLength = 0;
    for (HtmlTokenType t : HtmlTokenType.values()) {
      maxTypeLength = Math.max(maxTypeLength, t.name().length());
    }

    while (lexer.hasNext()) {
      Token<HtmlTokenType> t = lexer.next();
      // Do C style escaping of the token text so that each token in the golden
      // file can fit on one line.
      String escaped = t.text.replace("\\", "\\\\").replace("\n", "\\n");
      String type = t.type.toString();
      while (type.length() < maxTypeLength) { type += " "; }
      out.append(type).append(" [").append(escaped).append("]  :  ")
         .append(t.pos.toString()).append("\n");
    }
  }

  private void assertTokens(String markup, boolean asXml, String... golden)
      throws ParseException {
    HtmlLexer lexer = new HtmlLexer(fromString(markup));
    lexer.setTreatedAsXml(asXml);
    List<String> actual = new ArrayList<String>();
    while (lexer.hasNext()) {
      Token<HtmlTokenType> t = lexer.next();
      actual.add(t.type + ": " + t.text);
    }
    MoreAsserts.assertListsEqual(Arrays.asList(golden), actual);
  }
}
