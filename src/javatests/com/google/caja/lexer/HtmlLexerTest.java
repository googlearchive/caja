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

import com.google.caja.util.TestUtil;

import junit.framework.TestCase;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class HtmlLexerTest extends TestCase {

  public void testHtmlLexer() throws Exception {
    // Read the input.
    InputSource input = new InputSource(
        TestUtil.getResource(getClass(), "htmllexerinput1.html"));

    // Do the lexing.
    CharProducer p = CharProducer.Factory.create(input);
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
    // Read the input.
    InputSource input = new InputSource(
        TestUtil.getResource(getClass(), "htmllexerinput2.xml"));

    // Do the lexing.
    CharProducer p = CharProducer.Factory.create(input);
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
}
