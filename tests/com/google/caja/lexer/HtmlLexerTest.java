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

  public final void testHtmlLexer() throws Exception {
    // Do the lexing.
    CharProducer p = fromResource("htmllexerinput1.html");
    StringBuilder actual = new StringBuilder();
    lex(new HtmlLexer(p), actual);

    // Get the golden.
    String golden = fromResource("htmllexergolden1.txt").toString();

    // Compare.
    assertEquals(golden, actual.toString());
  }

  public final void testXmlLexer() throws Exception {
    // Do the lexing.
    CharProducer p = fromResource("htmllexerinput2.xml");
    StringBuilder actual = new StringBuilder();
    HtmlLexer lexer = new HtmlLexer(p);
    lexer.setTreatedAsXml(true);
    lex(lexer, actual);

    // Get the golden.
    String golden = TestUtil.readResource(getClass(), "htmllexergolden2.txt");

    // Compare.
    assertEquals(golden, actual.toString());
  }

  public final void testEofInTag() throws Exception {
    assertTokens("<div", true, "TAGBEGIN: <div");
    assertTokens("</div", true, "TAGBEGIN: </div");
    assertTokens("<div\n", true, "TAGBEGIN: <div");
    assertTokens("</div\n", true, "TAGBEGIN: </div");
    assertTokens("<div", false, "TAGBEGIN: <div");
    assertTokens("</div", false, "TAGBEGIN: </div");
    assertTokens("<div\n", false, "TAGBEGIN: <div");
    assertTokens("</div\n", false, "TAGBEGIN: </div");
  }

  public final void testPartialTagInCData() throws Exception {
    assertTokens(
        "<script>w('</b')</script>", false,
        "TAGBEGIN: <script",
        "TAGEND: >",
        "UNESCAPED: w('</b')",
        "TAGBEGIN: </script",
        "TAGEND: >");
  }

  public final void testUrlEndingInSlashOutsideQuotes() throws Exception {
    assertTokens(
        "<a href=http://foo.com/>Clicky</a>", false,
        "TAGBEGIN: <a",
        "ATTRNAME: href",
        "ATTRVALUE: http://foo.com/",
        "TAGEND: >",
        "TEXT: Clicky",
        "TAGBEGIN: </a",
        "TAGEND: >");
    assertTokens(
        "<a href=http://foo.com/>Clicky</a>", true,
        "TAGBEGIN: <a",
        "ATTRNAME: href",
        "ATTRVALUE: http://foo.com/",
        "TAGEND: >",
        "TEXT: Clicky",
        "TAGBEGIN: </a",
        "TAGEND: >");
  }

  public final void testIEConditionalComments() throws Exception {
    String ieCondComment1 =
        "<!-- [if lte IE6]>"
        + "<link href=\"iecss.css\" rel=\"stylesheet\" type=\"text/css\">"
        + "<!--Text--><![endif]-->";
    assertTokens(ieCondComment1, false, "COMMENT: " + ieCondComment1);
    // Test this in XML mode to make sure it does not attempt to recognize IE
    // conditional comments.
    assertTokens(ieCondComment1,
        true,
        "COMMENT: <!-- [if lte IE6]><link href=\"iecss.css\" "
        + "rel=\"stylesheet\" type=\"text/css\"><!--Text-->",
        "CDATA: <![endif]-->");

    String ieCondComment2 =
        "<!-- [if lte IE6]>"
        + "<script>alert(\"This could be an --> IE 6 browser.\");</script>"
        + "<![endif]-->";
    assertTokens(ieCondComment2, false, "COMMENT: " + ieCondComment2);

    // Downlevel-revealed type of comments.
    String ieCondComment3 =
        "<![if !IE | gte IE 7]>"
        + "<link rel=\"stylesheet\" href=\"special.css\" type=\"text/css\" />"
        + "<![endif]>";
    assertTokens(ieCondComment3, false,
        "IE_DR_COMMENT_BEGIN: <![if !IE | gte IE 7]>",
        "TAGBEGIN: <link",
        "ATTRNAME: rel",
        "ATTRVALUE: \"stylesheet\"",
        "ATTRNAME: href",
        "ATTRVALUE: \"special.css\"",
        "ATTRNAME: type",
        "ATTRVALUE: \"text/css\"",
        "TAGEND: />",
        "IE_DR_COMMENT_END: <![endif]>");
  }

  public final void testShortTags() throws Exception {
    // See comments in html-sanitizer-test.js as to why we don't bother with
    // short tags.  In short, they are not in HTML5 and not implemented properly
    // in existing HTML4 clients.
    assertTokens(
        "<p<a href=\"/\">first part of the text</> second part", false,
        "TAGBEGIN: <p",
        "ATTRNAME: <a",
        "ATTRNAME: href",
        "ATTRVALUE: \"/\"",
        "TAGEND: >",
        "TEXT: first part of the text</> second part");
    assertTokens(
        "<p/b/", false,
        "TAGBEGIN: <p",
        "ATTRNAME: /",
        "ATTRNAME: b/");
    assertTokens(
        "<p<b>", false,
        "TAGBEGIN: <p",
        "ATTRNAME: <b",
        "TAGEND: >");
  }

  private static void lex(HtmlLexer lexer, Appendable out) throws Exception {
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
