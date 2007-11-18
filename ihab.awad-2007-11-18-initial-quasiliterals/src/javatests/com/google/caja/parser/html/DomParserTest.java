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

package com.google.caja.parser.html;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.util.Criterion;

import java.io.StringReader;
import java.net.URI;

import junit.framework.TestCase;

/**
 * testcase for {@link DomParser}.
 *
 * @author mikesamuel@gmail.com
 */
public class DomParserTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  static final String DOM1_XML = (
      "\n"
      + "<foo a=\"b\" c =\"d\" e = \"&lt;&quot;f&quot;&amp;amp;\">\n"
      + "<bar/> <bar /> before <!-- comment --> after \n"
      + "Hello &lt;there&gt;\n"
      + "<baz><![CDATA[Hello <there>]]></baz>\n"
      + "</foo>\n"
      + "\n"
      + "\n"
      + "\n"
      );

  static final String DOM1_GOLDEN = "Tag : foo\n"
        + "  Attrib : a\n"
        + "    Value : b\n"
        + "  Attrib : c\n"
        + "    Value : d\n"
        + "  Attrib : e\n"
        + "    Value : <\"f\"&amp;\n"
        + "  Text : \n"
        + "\n"
        + "  Tag : bar\n"
        + "  Text :  \n"
        + "  Tag : bar\n"
        + "  Text :  before  after \n"
        + "Hello <there>\n"
        + "\n"
        + "  Tag : baz\n"
        + "    CData : Hello <there>\n"
        + "  Text : \n"
        + "";

  public void testParseDom() throws Exception {
    InputSource is = new InputSource(
        URI.create("test:///" + DomParserTest.class.getName()));
    TokenQueue<HtmlTokenType> tq;
    if (false) {
      CharProducer cp = CharProducer.Factory.create(
          new StringReader(DOM1_XML), is);
      HtmlLexer lexer = new HtmlLexer(cp);
      lexer.setTreatedAsXml(true);
      tq = new TokenQueue<HtmlTokenType>(
          lexer, is, Criterion.Factory.<Token<HtmlTokenType>>optimist());
      while (!tq.isEmpty()) {
        Token<HtmlTokenType> t = tq.pop();
        System.err.println("t.type=" + t.type + ", text=[" + t.text + "]");
      }
    }
    {
      CharProducer cp = CharProducer.Factory.create(
          new StringReader(DOM1_XML), is);
      HtmlLexer lexer = new HtmlLexer(cp);
      lexer.setTreatedAsXml(true);
      tq = new TokenQueue<HtmlTokenType>(
          lexer, is, Criterion.Factory.<Token<HtmlTokenType>>optimist());
    }
    DomTree t = DomParser.parseDocument(tq);
    StringBuilder actual = new StringBuilder();
    t.format(new MessageContext(), actual);
    assertEquals(DOM1_GOLDEN, actual.toString());
  }
}
