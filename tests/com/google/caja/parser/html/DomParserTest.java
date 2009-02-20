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

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Criterion;
import com.google.caja.util.Join;
import com.google.caja.util.MoreAsserts;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * testcase for {@link DomParser}.
 * http://james.html5.org/parsetree.html is a useful resource for testing
 * HTML related tests.
 *
 * @author mikesamuel@gmail.com
 */
public class DomParserTest extends CajaTestCase {
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

  static final String DOM1_GOLDEN = (
      "Tag : foo\n"
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
      );

  public void testParseDom() throws Exception {
    TokenQueue<HtmlTokenType> tq = tokenizeTestInput(DOM1_XML, true);
    DomTree t = new DomParser(tq, true, mq).parseDocument();
    StringBuilder actual = new StringBuilder();
    t.format(new MessageContext(), actual);
    assertEquals(DOM1_GOLDEN, actual.toString());
  }

  public void testOneRootXmlElement() throws Exception {
    TokenQueue<HtmlTokenType> tq = tokenizeTestInput("<foo/><bar/>", true);
    try {
      new DomParser(tq, true, mq).parseDocument();
    } catch (ParseException ex) {
      assertEquals(DomParserMessageType.MISPLACED_CONTENT,
                   ex.getCajaMessage().getMessageType());
      // Passed.  Expect to fail with a message about <bar/>
      return;
    }
    fail("Parsing of an XML document with multiple roots did not fail");
  }

  public void testEmptyFragment() throws Exception {
    assertParsedMarkup(Arrays.<String>asList(),
                       Arrays.asList("Fragment 1+1-1+1"),
                       Arrays.<String>asList(),
                       Arrays.asList(""),
                       null,
                       true);
    assertParsedMarkup(Arrays.asList(" "),
                       Arrays.asList("Fragment 1+1-1+2"),
                       Arrays.<String>asList(),
                       Arrays.asList(""),
                       null,
                       true);
  }

  public void testParseDirective() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<!DOCTYPE html",
            "PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN",
            "DTD/xhtml1-strict.dtd\">",
            "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">",
            "<head>",
            "</head>",
            "<body>",
            "</body>",
            "</html>"
        ),
        Arrays.asList(
            "Tag : html 5+1-10+8",
            "  Attrib : xmlns 5+7-5+12",
            "    Value : http://www.w3.org/1999/xhtml 5+13-5+43",
            "  Attrib : xml:lang 5+44-5+52",
            "    Value : en 5+53-5+57",
            "  Attrib : lang 5+58-5+62",
            "    Value : en 5+63-5+67",
            "  Text : \n 5+68-6+1",
            "  Tag : head 6+1-7+8",
            "    Text : \n 6+7-7+1",
            "  Text : \n 7+8-8+1",
            "  Tag : body 8+1-9+8",
            "    Text : \n\n 8+7-10+1"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">",
            "<head>",
            "</head>",
            "<body>",
            "",
            "</body></html>"
            )
        );
  }

  public void testTextOnlyFragment() throws Exception {
    for (int i = 0; i < 2; ++i) {
      boolean isXml = i == 0;
      assertParsedMarkup(
          Arrays.asList("Hello, world!"),
          Arrays.asList(
              "Fragment 1+1-1+14",
              "  Text : Hello, world! 1+1-1+14"),
          Arrays.<String>asList(),
          Arrays.asList("Hello, world!"),
          isXml,
          true);
    }
  }

  public void testHtml1() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html><head>",
            "",
            "<title>Foo<a> &amp; Bar & Baz</a></title>",
            "",
            "</head>",
            "<body bgcolor=white>",
            "  <BoDY onload=panic()>",
            "<body onerror=panic() onload=dontpanic()>",
            "",
            "</body>",
            "<taBLe>",
            "<td>Howdy</html></tablE>"),
        Arrays.asList(
            "Tag : html 1+1-12+25",  // </html> inside <table> ignored
            "  Tag : head 1+7-5+8",
            "    Text : \n\n 1+13-3+1",
            "    Tag : title 3+1-3+42",
            "      Text : Foo<a> & Bar & Baz</a> 3+8-3+34",
            "    Text : \n\n 3+42-5+1",
            "  Text : \n 5+8-6+1",
            "  Tag : body 6+1-12+25",
            "    Attrib : bgcolor 6+7-6+14",
            "      Value : white 6+15-6+20",
            // Include attributes folded in from other body tags
            "    Attrib : onload 7+9-7+15",
            "      Value : panic() 7+16-7+23",
            "    Attrib : onerror 8+7-8+14",
            "      Value : panic() 8+15-8+22",
            // Normalized text from in between body tags
            "    Text : \n  \n\n\n\n 6+21-11+1",
            "    Tag : table 11+1-12+25",  // Name is canonicalized
            "      Text : \n 11+8-12+1",
            "      Tag : tbody 12+1-12+17",
            "        Tag : tr 12+1-12+17",
            "          Tag : td 12+1-12+17",
            "            Text : Howdy 12+5-12+10"
            ),
        Arrays.asList(
            "LINT testHtml1:7+3 - 24: "
            + "'body' start tag found but the 'body' element is already open.",
            "LINT testHtml1:8+1 - 42: "
            + "'body' start tag found but the 'body' element is already open.",
            "LINT testHtml1:11+1 - 8: Stray 'table' start tag.",
            "LINT testHtml1:12+1 - 5: 'td' start tag in table body.",
            "LINT testHtml1:12+10 - 17: Stray end tag 'html'."
            ),
        Arrays.asList(
            "<html><head>",
            "",
            // Entities in title consistently escaped
            "<title>Foo&lt;a&gt; &amp; Bar &amp; Baz&lt;/a&gt;</title>",
            "",
            "</head>",
            // Merged attributes
            "<body bgcolor=\"white\" onload=\"panic()\" onerror=\"panic()\">",
            "  ",
            "",
            "",
            "",
            "<table>",
            // Implied body and rows in table
            "<tbody><tr><td>Howdy</td></tr></tbody></table></body></html>"
            )
        );
  }

  public void testHtml2() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html>",
            "  <head>",
            "    <link rel=stylesheet href=styles-1.css>",
            "    <meta http-equiv=charset content=utf-8 />",
            "  </head>",
            "  <script src='foo.js'></script>",
            "  <body>",
            "    <script type='text/javascript'>//<![CDATA[",
            "      foo() && bar();",
            "    //]]></script>",
            "</html>"),
        Arrays.asList(
            "Tag : html 1+1-11+8",  // </html> inside <table> ignored
            "  Text : \n   1+7-2+3",
            "  Tag : head 2+3-6+24",
            "    Text : \n     2+9-3+5",
            "    Tag : link 3+5-3+44",
            "      Attrib : rel 3+11-3+14",
            "        Value : stylesheet 3+15-3+25",
            "      Attrib : href 3+26-3+30",
            "        Value : styles-1.css 3+31-3+43",
            "    Text : \n     3+44-4+5",
            "    Tag : meta 4+5-4+46",
            "      Attrib : http-equiv 4+11-4+21",
            "        Value : charset 4+22-4+29",
            "      Attrib : content 4+30-4+37",
            "        Value : utf-8 4+38-4+43",
            "    Text : \n   4+46-5+3",
            "    Tag : script 6+3-6+33",
            "      Attrib : src 6+11-6+14",
            "        Value : foo.js 6+15-6+23",
            "  Text : \n  \n   5+10-7+3",
            "  Tag : body 7+3-11+1",
            "    Text : \n     7+9-8+5",
            "    Tag : script 8+5-10+19",
            "      Attrib : type 8+13-8+17",
            "        Value : text/javascript 8+18-8+35",
            ("      Text : //<![CDATA[\n      foo() && bar();\n    //]]>"
             + " 8+36-10+10"),
            "    Text : \n 10+19-11+1"
            ),
        Arrays.asList(
            "LINT testHtml2:6+3 - 24:"
            + " 'script' element between 'head' and 'body'."
            ),
        Arrays.asList(
            "<html>",
            "  <head>",
            "    <link rel=\"stylesheet\" href=\"styles-1.css\" />",
            "    <meta http-equiv=\"charset\" content=\"utf-8\" />",
            "  <script src=\"foo.js\"></script></head>",
            "  ",
            "  <body>",
            "    <script type=\"text/javascript\">//<![CDATA[",
            "      foo() && bar();",
            "    //]]></script>",
            "</body></html>"
            )
        );
  }

  public void testBeforeHead() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html><SCRIPT>foo()</scriPt></html>"),
        Arrays.asList(
            "Tag : html 1+1-1+36",
            "  Tag : head 1+7-1+29",
            "    Tag : script 1+7-1+29",
            "      Text : foo() 1+15-1+20",
            "  Tag : body 1+29-1+29"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<html><head><script>foo()</script></head><body></body></html>"
            )
        );
  }

  public void testMinimalHtml() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html></html>"),
        Arrays.asList(
            "Tag : html 1+1-1+14",
            "  Tag : head 1+7-1+7",
            "  Tag : body 1+7-1+7"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<html><head></head><body></body></html>"
            )
        );
  }

  public void testMinimalFrameset() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html><frameset></frameset></html>"),
        Arrays.asList(
            "Tag : html 1+1-1+35",
            "  Tag : head 1+7-1+7",
            "  Tag : frameset 1+7-1+28"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<html><head></head><frameset></frameset></html>"
            )
        );
  }

  public void testSpuriousCloseTagBeforeHead() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html></script></html>"),
        Arrays.asList(
            "Tag : html 1+1-1+23",
            "  Tag : head 1+16-1+16",
            "  Tag : body 1+16-1+16"
            ),
        Arrays.asList(
            ("LINT testSpuriousCloseTagBeforeHead:1+7 - 16"
             + ": Stray end tag 'script'.")
            ),
        Arrays.asList(
            "<html><head></head><body></body></html>"
            )
        );
  }

  public void testHeadless() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html><body>Hello World</body></html>"),
        Arrays.asList(
            "Tag : html 1+1-1+38",
            "  Tag : head 1+7-1+7",
            "  Tag : body 1+7-1+31",
            "    Text : Hello World 1+13-1+24"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<html><head></head><body>Hello World</body></html>"
            )
        );
  }

  public void testLooseStyleTagEndsInHead() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html><style>",
            "body:before { content: 'Hello ' }",
            "body:after { content: \"World\" }",
            "</style></html>"),
        Arrays.asList(
            "Tag : html 1+1-4+16",
            "  Tag : head 1+7-4+9",
            "    Tag : style 1+7-4+9",
            ("      Text : \nbody:before { content: 'Hello ' }"
                        + "\nbody:after { content: \"World\" }\n 1+14-4+1"),
            "  Tag : body 4+9-4+9"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<html><head><style>",
            "body:before { content: 'Hello ' }",
            "body:after { content: \"World\" }",
            "</style></head><body></body></html>"
            )
        );
  }

  public void testDoubleHeadedHtml() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html><head><head><body></body></html>"),
        Arrays.asList(
            "Tag : html 1+1-1+39",
            "  Tag : head 1+7-1+19",
            "  Tag : body 1+19-1+32"
            ),
        Arrays.asList(
            "LINT testDoubleHeadedHtml:1+13 - 19:"
            + " Start tag for 'head' seen when 'head' was already open."
            ),
        Arrays.asList(
            "<html><head></head><body></body></html>"
            )
        );
  }

  public void testBodyFragment() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<body><p>&#60;Bar&#x3E;<p>Baz</body>"),
        Arrays.asList(
            "Fragment 1+1-1+37",
            "  Tag : p 1+7-1+24",
            "    Text : <Bar> 1+10-1+24",  // Text contains decoded value
            "  Tag : p 1+24-1+37",
            "    Text : Baz 1+27-1+30"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<p>&lt;Bar&gt;</p><p>Baz</p>"
            )
        );
  }

  public void testFragmentThatEndsWithAComment() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<p>Hello</p>  <!-- Zoicks -->   "),
        Arrays.asList(
            "Fragment 1+1-1+33",
            "  Tag : p 1+1-1+13",
            "    Text : Hello 1+4-1+9",
            "  Text :       1+13-1+33"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<p>Hello</p>     "
            )
        );
  }

  public void testTableFragment() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<table>",
            "<tr><th>Hi<td>There",
            "</table>"),
        Arrays.asList(
            "Fragment 1+1-3+9",
            "  Tag : table 1+1-3+9",
            "    Text : \n 1+8-2+1",
            "    Tag : tbody 2+1-3+1",
            "      Tag : tr 2+1-3+1",
            "        Tag : th 2+5-2+11",
            "          Text : Hi 2+9-2+11",
            "        Tag : td 2+11-3+1",
            "          Text : There\n 2+15-3+1"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<table>",
            "<tbody><tr><th>Hi</th><td>There",
            "</td></tr></tbody></table>"
            )
        );
  }

  public void testFragmentWithClose() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "</head>",  // Nothing here but text
            "<body>Foo"),
        Arrays.asList(
            "Fragment 1+1-2+10",
            "  Text : \nFoo 1+8-2+10"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "",
            "Foo"
            )
        );
  }

  public void testMisplacedTitle() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html><body><title>What head?</title></body></html>"),
        Arrays.asList(
            "Tag : html 1+1-1+52",
            "  Tag : head 1+7-1+30",
            "    Tag : title 1+13-1+38",
            "      Text : What head? 1+20-1+30",
            "  Tag : body 1+7-1+45"
            ),
        Arrays.<String>asList(
            "LINT testMisplacedTitle:1+13 - 20:"
            + " 'title' element found inside 'body'."
            ),
        Arrays.asList(
            "<html><head><title>What head?</title></head><body></body></html>"
            )
        );
  }

  public void testBodyWithAttributeInFragment() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(  // A fragment with body attributes is a whole document
            "<div>",
            "<p>Foo<",
            "<body bgcolor=white>",
            "<p>Bar</body>"),
        Arrays.asList(
            "Fragment 1+1-4+14",
            "  Tag : html 1+1-4+14",
            "    Tag : head 1+1-1+1",
            "    Tag : body 1+1-4+14",
            "      Attrib : bgcolor 3+7-3+14",
            "        Value : white 3+15-3+20",
            "      Tag : div 1+1-4+14",
            "        Text : \n 1+6-2+1",
            "        Tag : p 2+1-4+1",
            "          Text : Foo<\n\n 2+4-4+1",
            "        Tag : p 4+1-4+14",
            "          Text : Bar 4+4-4+7"
            ),
        Arrays.asList(
            "LINT testBodyWithAttributeInFragment:3+1 - 21:"
            + " 'body' start tag found but the 'body' element is already open.",
            "LINT testBodyWithAttributeInFragment:4+7 - 14:"
            + " End tag for 'body' seen but there were unclosed elements."
            ),
        Arrays.asList(
            "<html><head></head><body bgcolor=\"white\"><div>",
            "<p>Foo&lt;",
            "",
            "</p><p>Bar</p></div></body></html>"
            )
        );
  }

  public void testListFragment() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            // </br> is treated as a <br>.  <br></br> -- 2 for the price of 1!
            "<div></br>",
            "<p>Foo<br>",
            "</html >",
            "<ul><li>One</ul>"
            ),
        Arrays.asList(
            "Fragment 1+1-4+17",
            "  Tag : div 1+1-4+17",
            "    Tag : br 1+6-1+11",
            "    Text : \n 1+11-2+1",
            "    Tag : p 2+1-4+1",
            "      Text : Foo 2+4-2+7",
            "      Tag : br 2+7-2+11",
            "      Text : \n\n 2+11-4+1",
            "    Tag : ul 4+1-4+17",
            "      Tag : li 4+5-4+12",
            "        Text : One 4+9-4+12"
            ),
        Arrays.asList(
            "LINT testListFragment:1+6 - 11: End tag 'br'.",
            "LINT testListFragment:3+1 - 9:"
            + " End tag for 'html' seen but there were unclosed elements.",
            "LINT testListFragment:4+1 - 5: Stray 'ul' start tag.",
            "LINT testListFragment:4+17:"
            + " End of file seen and there were open elements."
            ),
        Arrays.asList(
            "<div><br />",
            // </br> is interpreted as <br> so we need to make sure <br> does
            // not have an end tag.
            "<p>Foo<br />",
            "",
            "</p><ul><li>One</li></ul></div>"
            )
        );
  }

  public void testFormsNotNested() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<form action= 'fly'>",
            "<input type=radio name=method value=rockets>",
            "<form action=walk>",
            "<input type=radio name=method value=wings>",
            "<input type=submit value ='Take off'>",
            "</form></form>"
            ),
        Arrays.asList(
            "Fragment 1+1-6+15",
            "  Tag : form 1+1-6+8",
            "    Attrib : action 1+7-1+13",
            "      Value : fly 1+15-1+20",
            "    Text : \n 1+21-2+1",
            "    Tag : input 2+1-2+45",
            "      Attrib : type 2+8-2+12",
            "        Value : radio 2+13-2+18",
            "      Attrib : name 2+19-2+23",
            "        Value : method 2+24-2+30",
            "      Attrib : value 2+31-2+36",
            "        Value : rockets 2+37-2+44",
            "    Text : \n\n 2+45-4+1",
            "    Tag : input 4+1-4+43",
            "      Attrib : type 4+8-4+12",
            "        Value : radio 4+13-4+18",
            "      Attrib : name 4+19-4+23",
            "        Value : method 4+24-4+30",
            "      Attrib : value 4+31-4+36",
            "        Value : wings 4+37-4+42",
            "    Text : \n 4+43-5+1",
            "    Tag : input 5+1-5+38",
            "      Attrib : type 5+8-5+12",
            "        Value : submit 5+13-5+19",
            "      Attrib : value 5+20-5+25",
            "        Value : Take off 5+27-5+37",
            "    Text : \n 5+38-6+1"
            ),
        Arrays.asList(
            "LINT testFormsNotNested:3+1 - 19: Saw a 'form' start tag"
            + ", but there was already an active 'form' element.",
            "LINT testFormsNotNested:6+8 - 15: End tag 'form' seen but"
            + " there were unclosed elements."
            ),
        Arrays.asList(
            "<form action=\"fly\">",
            "<input type=\"radio\" name=\"method\" value=\"rockets\" />",
            "",
            "<input type=\"radio\" name=\"method\" value=\"wings\" />",
            "<input type=\"submit\" value=\"Take off\" />",
            "</form>"
            )
        );
  }

  public void testListNesting() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<ul id=unordered-list>",
            "<li>From Chaos,</li></li>",  // Warn on useless </li>
            "<li>and Disorder; arise",
            "<li><ol id=ordered-list>",
            "  <li><div>Harmony,",  // Div makes us look harder to find li
            "  <li>Balance,</li>",
            "  <li>and Punctuation!?</li></li>",  // Second </li> is significant
            "</ul>"
            ),
        Arrays.asList(
            "Fragment 1+1-8+6",
            "  Tag : ul 1+1-8+6",
            "    Attrib : id 1+5-1+7",
            "      Value : unordered-list 1+8-1+22",
            "    Text : \n 1+23-2+1",
            "    Tag : li 2+1-2+21",
            "      Text : From Chaos, 2+5-2+16",
            "    Text : \n 2+26-3+1",
            "    Tag : li 3+1-4+1",
            "      Text : and Disorder; arise\n 3+5-4+1",
            "    Tag : li 4+1-7+34",
            "      Tag : ol 4+5-7+29",
            "        Attrib : id 4+9-4+11",
            "          Value : ordered-list 4+12-4+24",
            "        Text : \n   4+25-5+3",
            "        Tag : li 5+3-6+3",
            "          Tag : div 5+7-6+3",
            "            Text : Harmony,\n   5+12-6+3",
            "        Tag : li 6+3-6+20",
            "          Text : Balance, 6+7-6+15",
            "        Text : \n   6+20-7+3",
            "        Tag : li 7+3-7+29",
            "          Text : and Punctuation!? 7+7-7+24",
            "    Text : \n 7+34-8+1"
            ),
        Arrays.asList(
            // TODO(mikesamuel): this error message seems to be a bug.
            // There is an error there, but the close tag is spurious.
            "LINT testListNesting:2+21 - 26:"
            + " End tag 'li' seen but there were unclosed elements.",
            "LINT testListNesting:6+3 - 7:"
            + " A 'li' start tag was seen but the previous 'li' element"
            + " had open children.",
            // Ditto wrong message
            "LINT testListNesting:7+29 - 34:"
            + " End tag 'li' seen but there were unclosed elements."
            ),
        Arrays.asList(
            "<ul id=\"unordered-list\">",
            "<li>From Chaos,</li>",
            "<li>and Disorder; arise",
            "</li><li><ol id=\"ordered-list\">",
            "  <li><div>Harmony,",
            "  </div></li><li>Balance,</li>",
            "  <li>and Punctuation!?</li></ol></li>",
            "</ul>"
            )
        );
  }

  public void testParagraphInterrupters() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<body><p>Hi <pre>There</pre>",
            "<p>Uh <dl><dt>Zero<dd>None<ol><li>Nested<dt><div><p>One<dd>1</dl>",
            "<p>Li</dl>ne<hr>s",
            "<p>Well <plaintext>Runs till the end of the <p>Document",
            "No matter <p>What"
            ),
        Arrays.asList(
            "Tag : html 1+1-5+18",
            "  Tag : head 1+1-1+1",
            "  Tag : body 1+1-5+18",
            "    Tag : p 1+7-1+13",
            "      Text : Hi  1+10-1+13",
            "    Tag : pre 1+13-1+29",
            "      Text : There 1+18-1+23",
            "    Text : \n 1+29-2+1",
            "    Tag : p 2+1-2+7",
            "      Text : Uh  2+4-2+7",
            "    Tag : dl 2+7-2+66",
            "      Tag : dt 2+11-2+19",
            "        Text : Zero 2+15-2+19",
            "      Tag : dd 2+19-2+61",
            "        Text : None 2+23-2+27",
            "        Tag : ol 2+27-2+61",
            "          Tag : li 2+31-2+61",
            "            Text : Nested 2+35-2+41",
            // DT don't jump out of an OL or UL list
            "            Tag : dt 2+41-2+56",
            "              Tag : div 2+45-2+56",
            "                Tag : p 2+50-2+56",
            "                  Text : One 2+53-2+56",
            "            Tag : dd 2+56-2+61",
            "              Text : 1 2+60-2+61",
            "    Text : \n 2+66-3+1",
            "    Tag : p 3+1-3+13",
            // Not interrupted by </dl>.  End tags do not interrupt <p>s
            "      Text : Line 3+4-3+13",
            "    Tag : hr 3+13-3+17",
            "    Text : s\n 3+17-4+1",
            "    Tag : p 4+1-4+9",
            "      Text : Well  4+4-4+9",
            "    Tag : plaintext 4+9-5+18",
            ("      Text : Runs till the end of the <p>Document"
             + "\nNo matter <p>What"
             + " 4+20-5+18")
            ),
        Arrays.asList(
            "LINT testParagraphInterrupters:2+56 - 60:"
            + " A definition list item start tag was seen but the previous"
            + " definition list item element had open children.",
            "LINT testParagraphInterrupters:2+61 - 66:"
            + " End tag 'dl' seen but there were unclosed elements.",
            "LINT testParagraphInterrupters:3+6 - 11:"
            + " End tag 'dl' seen but there were unclosed elements.",
            "LINT testParagraphInterrupters:5+18:"
            + " End of file seen and there were open elements."
            ),
        Arrays.asList(
            "<html><head></head><body><p>Hi </p><pre>There</pre>",
            ("<p>Uh </p><dl><dt>Zero</dt><dd>None<ol><li>Nested"
             + "<dt><div><p>One</p></div></dt><dd>1</dd></li></ol></dd></dl>"),
            "<p>Line</p><hr />s",
            "<p>Well </p><plaintext>Runs till the end of the <p>Document",
            // Not correct but I don't want to muck with DomTree rendering
            // just for plaintext.
            "No matter <p>What</plaintext></body></html>"
            )
        );
  }

  public void testParagraphNotNested() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html>",
            "<p>Foo</p>",
            "<p>Bar",
            "<p>Baz</p></p>",  // Second close p opens a paragraph because...
            "</html>"
            ),
        Arrays.asList(
            "Tag : html 1+1-5+8",
            "  Text : \n 1+7-2+1",
            "  Tag : head 2+1-2+1",
            "  Tag : body 2+1-5+1",
            "    Tag : p 2+1-2+11",
            "      Text : Foo 2+4-2+7",
            "    Text : \n 2+11-3+1",
            "    Tag : p 3+1-4+1",
            "      Text : Bar\n 3+4-4+1",
            "    Tag : p 4+1-4+11",
            "      Text : Baz 4+4-4+7",
            "    Tag : p 4+11-4+15",
            "    Text : \n 4+15-5+1"
            ),
        Arrays.asList(
            "LINT testParagraphNotNested:4+11 - 15:"
            + " End tag 'p' seen but there were unclosed elements."
            ),
        Arrays.asList(
            "<html>",
            "<head></head><body><p>Foo</p>",
            "<p>Bar",
            "</p><p>Baz</p><p></p>",
            "</body></html>"
            )
        );
  }

  public void testAnyHeadingTagCloses() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html>",
            "<p>Foo",
            "<h1><p>Bar</H3>",
            "",
            "<h2>Baz",
            "",
            "<<h3>Boo</h2>",  // Close tag closes h3, not enclosing h2
            "",
            "<p>Far></h3></h1>"
            ),
        Arrays.asList(
            "Tag : html 1+1-9+18",
            "  Text : \n 1+7-2+1",
            "  Tag : head 2+1-2+1",
            "  Tag : body 2+1-9+18",
            "    Tag : p 2+1-3+1",
            "      Text : Foo\n 2+4-3+1",
            "    Tag : h1 3+1-3+16",
            "      Tag : p 3+5-3+11",
            "        Text : Bar 3+8-3+11",
            "    Text : \n\n 3+16-5+1",
            "    Tag : h2 5+1-9+13",
            "      Text : Baz\n\n< 5+5-7+2",
            "      Tag : h3 7+2-7+14",
            "        Text : Boo 7+6-7+9",
            "      Text : \n\n 7+14-9+1",
            "      Tag : p 9+1-9+8",
            "        Text : Far> 9+4-9+8"
            ),
        Arrays.asList(
            "LINT testAnyHeadingTagCloses:3+11 - 16:"
            + " End tag 'h3' seen but there were unclosed elements.",
            "LINT testAnyHeadingTagCloses:7+9 - 14:"
            + " End tag 'h2' seen but there were unclosed elements.",
            "LINT testAnyHeadingTagCloses:9+8 - 13:"
            + " End tag 'h3' seen but there were unclosed elements.",
            "LINT testAnyHeadingTagCloses:9+13 - 18:"
            + " End tag 'h1' seen but there were unclosed elements."
            ),
        Arrays.asList(
            "<html>",
            "<head></head><body><p>Foo",
            "</p><h1><p>Bar</p></h1>",
            "",
            "<h2>Baz",
            "",
            "&lt;<h3>Boo</h3>",
            "",
            "<p>Far&gt;</p></h2></body></html>"
            )
        );
  }

  public void testLinksDontNest() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<div>",
            "<a href=foo>Foo",
            // Links don't nest
            "<a href=bar>Bar",
            // unless they do.  Table is a scoping element.
            "<table><caption><a href=baz>Baz</table>boo",
            "</a>"
            ),
        Arrays.asList(
            "Fragment 1+1-5+5",
            "  Tag : div 1+1-5+5",
            "    Text : \n 1+6-2+1",
            "    Tag : a 2+1-3+1",
            "      Attrib : href 2+4-2+8",
            "        Value : foo 2+9-2+12",
            "      Text : Foo\n 2+13-3+1",
            "    Tag : a 3+1-5+5",
            "      Attrib : href 3+4-3+8",
            "        Value : bar 3+9-3+12",
            "      Text : Bar\n 3+13-4+1",
            "      Tag : table 4+1-4+40",
            "        Tag : caption 4+8-4+32",
            "          Tag : a 4+17-4+32",
            "            Attrib : href 4+20-4+24",
            "              Value : baz 4+25-4+28",
            "            Text : Baz 4+29-4+32",
            "      Text : boo\n 4+40-5+1"
            ),
        Arrays.<String>asList(
            "LINT testLinksDontNest:3+1 - 13:"
            + " An 'a' start tag seen with already an active 'a' element.",
            "LINT testLinksDontNest:4+32 - 40:"
            + " 'table' closed but 'caption' was still open.",
            "LINT testLinksDontNest:4+32 - 40:"
            + " Unclosed elements on stack.",
            "LINT testLinksDontNest:5+5:"
            + " End of file seen and there were open elements."
            ),
        Arrays.asList(
            "<div>",
            "<a href=\"foo\">Foo",
            "</a><a href=\"bar\">Bar",
            "<table><caption><a href=\"baz\">Baz</a></caption></table>boo",
            "</a></div>"
            )
        );
  }

  public void testFormattingElements() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<body>",
            "<a href=foo>Foo<nobr>Bar</a>",
            "",
            "<b><font COLOR=BLUE>bold&blue</b>",
            "still blue</font>",
            "boring",
            "<span><b>Foo</span>Bar</b></span>"
            ),
        Arrays.asList(
            "Tag : html 1+1-7+34",
            "  Tag : head 1+1-1+1",
            "  Tag : body 1+1-7+34",
            "    Text : \n 1+7-2+1",
            "    Tag : a 2+1-2+29",
            "      Attrib : href 2+4-2+8",
            "        Value : foo 2+9-2+12",
            "      Text : Foo 2+13-2+16",
            "      Tag : nobr 2+16-2+25",
            "        Text : Bar 2+22-2+25",
            "    Tag : nobr 2+16-7+27",
            "      Text : \n\n 2+29-4+1",
            "      Tag : b 4+1-4+34",
            "        Tag : font 4+4-4+30",
            "          Attrib : color 4+10-4+15",
            "            Value : BLUE 4+16-4+20",
            "          Text : bold&blue 4+21-4+30",
            "      Tag : font 4+4-5+18",
            "        Attrib : color 4+10-4+15",
            "          Value : BLUE 4+16-4+20",
            "        Text : \nstill blue 4+34-5+11",
            "      Text : \nboring\n 5+18-7+1",
            "      Tag : span 7+1-7+20",
            "        Tag : b 7+7-7+13",
            "          Text : Foo 7+10-7+13",
            "      Tag : b 7+7-7+27",
            "        Text : Bar 7+20-7+23"
            ),
        Arrays.asList(
            "LINT testFormattingElements:2+25 - 29:"
            + " End tag 'a' violates nesting rules.",
            "LINT testFormattingElements:4+30 - 34:"
            + " End tag 'b' violates nesting rules.",
            "LINT testFormattingElements:7+13 - 20: Unclosed element 'b'.",
            "LINT testFormattingElements:7+27 - 34: Unclosed element 'nobr'."
            ),
        Arrays.asList(
            "<html><head></head><body>",
            "<a href=\"foo\">Foo<nobr>Bar</nobr></a><nobr>",
            "",
            ("<b><font color=\"BLUE\">bold&amp;blue</font></b>"
             + "<font color=\"BLUE\">"),
            "still blue</font>",
            "boring",
            "<span><b>Foo</b></span><b>Bar</b></nobr></body></html>"
            )
        );
  }

  public void testObjectElements() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<object>",
            "  <param name=\"foo\" value=\"bar\">",
            "  <param name=\"baz\" value=\"boo\">",
            "</object>"
            ),
        Arrays.asList(
            "Fragment 1+1-4+10",
            "  Tag : object 1+1-4+10",
            "    Text : \n   1+9-2+3",
            "    Tag : param 2+3-2+33",
            "      Attrib : name 2+10-2+14",
            "        Value : foo 2+15-2+20",
            "      Attrib : value 2+21-2+26",
            "        Value : bar 2+27-2+32",
            "    Text : \n   2+33-3+3",
            "    Tag : param 3+3-3+33",
            "      Attrib : name 3+10-3+14",
            "        Value : baz 3+15-3+20",
            "      Attrib : value 3+21-3+26",
            "        Value : boo 3+27-3+32",
            "    Text : \n 3+33-4+1"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<object>",
            "  <param name=\"foo\" value=\"bar\" />",
            "  <param name=\"baz\" value=\"boo\" />",
            "</object>"
            )
        );
  }

  public void testButtonsDontNest() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<div>",
            "<button>Foo",
            "<button>Bar",
            // Like links, they don't nest except when they do.
            "<table><td><button>Baz</table>",
            "</button></button></button>"
            ),
        Arrays.asList(
            "Fragment 1+1-5+28",
            "  Tag : div 1+1-5+28",
            "    Text : \n 1+6-2+1",
            "    Tag : button 2+1-3+1",
            "      Text : Foo\n 2+9-3+1",
            "    Tag : button 3+1-5+10",
            "      Text : Bar\n 3+9-4+1",
            "      Tag : table 4+1-4+31",
            "        Tag : tbody 4+8-4+23",
            "          Tag : tr 4+8-4+23",
            "            Tag : td 4+8-4+23",
            "              Tag : button 4+12-4+23",
            "                Text : Baz 4+20-4+23",
            "      Text : \n 4+31-5+1"
            ),
        Arrays.asList(
            "LINT testButtonsDontNest:3+1 - 9:"
            + " 'button' start tag seen when there was an open 'button'"
            + " element in scope.",
            "LINT testButtonsDontNest:4+8 - 12:"
            + " 'td' start tag in table body.",
            "LINT testButtonsDontNest:4+23 - 31: Unclosed elements.",
            "LINT testButtonsDontNest:5+10 - 19:"
            + " End tag 'button' seen but there were unclosed elements.",
            "LINT testButtonsDontNest:5+19 - 28:"
            + " End tag 'button' seen but there were unclosed elements.",
            "LINT testButtonsDontNest:5+28:"
            + " End of file seen and there were open elements."
            ),
        Arrays.asList(
            "<div>",
            "<button>Foo",
            "</button><button>Bar",
            ("<table><tbody><tr><td><button>Baz"
             + "</button></td></tr></tbody></table>"),
            "</button></div>"
            )
        );
  }

  public void testImageTag() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<image src='foo.gif?a=b&c=d'>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+30",
            "  Tag : img 1+1-1+30",
            "    Attrib : src 1+8-1+11",
            "      Value : foo.gif?a=b&c=d 1+12-1+29"
            ),
        Arrays.<String>asList(
            "LINT testImageTag:1+1 - 30: Saw a start tag 'image'."
            ),
        Arrays.asList(
            "<img src=\"foo.gif?a=b&amp;c=d\" />"
            )
        );
  }

  public void testIsIndex() throws Exception {
    // Its semantics are really weird and it's deprecated, so drop it.
    assertParsedHtml(
        Arrays.asList(
            "<div><isindex prompt='Blah blah'></div>"
            ),
        Arrays.asList(  // WTF!?!
            "Tag : html 1+1-1+40",
            "  Tag : head 1+1-1+1",
            "  Tag : body 1+1-1+40",
            "    Tag : div 1+1-1+40",
            "      Tag : form 1+6-1+6",
            "        Tag : hr 1+6-1+6",
            "        Tag : p 1+6-1+6",
            "          Tag : label 1+6-1+6",
            "            Text : Blah blah 1+33-1+34",
            "            Tag : input 1+6-1+6",
            "              Attrib : name 1+6-1+6",
            "                Value : isindex 1+6-1+6",
            "        Tag : hr 1+6-1+6"
            ),
        Arrays.asList(
            "LINT testIsIndex:1+6 - 34: 'isindex' seen."
            ),
        Arrays.asList(
            "<html><head></head><body><div><form><hr /><p>"
            + "<label>Blah blah<input name=\"isindex\" /></label>"
            + "</p><hr /></form></div></body></html>"
            )
        );
  }

  public void testDisablersAreCdata() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<noframes><iframe src=foo></noframes></noscript>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+49",
            "  Tag : noframes 1+1-1+38",
            "    Text : <iframe src=foo> 1+11-1+27"
            ),
        Arrays.asList(
            ("LINT testDisablersAreCdata:1+38 - 49:"
             + " Stray end tag 'noscript'.")
            ),
        Arrays.asList(
            "<noframes><iframe src=foo></noframes>"
            )
        );
  }

  public void testInputElements() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<form>",
            "<textarea>",  // First and last newlines stripped
            "  <form action='a?b=c&d=e&amp;f=g'>",  // content is RCDATA
            " </form>",
            "</textarea>",
            "",
            "<select>",
            "  <option>One",
            "  <option>Two</option>",
            "<select>",  // Sometimes an open tag is a close tag.
            "  <option>Three</option>",  // Option tags outside selects ignored
            "</select>",
            "</select>",
            "<optgroup><option>Four</option></optgroup>",
            "</form>"
            ),
        Arrays.asList(
            "Fragment 1+1-15+8",
            "  Tag : form 1+1-15+8",
            "    Text : \n 1+7-2+1",
            "    Tag : textarea 2+1-5+12",
            "      Text :   <form action='a?b=c&d=e&f=g'>\n </form>\n 2+11-5+1",
            "    Text : \n\n 5+12-7+1",
            "    Tag : select 7+1-10+9",
            "      Text : \n   7+9-8+3",
            "      Tag : option 8+3-9+3",
            "        Text : One\n   8+11-9+3",
            "      Tag : option 9+3-9+23",
            "        Text : Two 9+11-9+14",
            "      Text : \n 9+23-10+1",
            "    Text : \n  Three\n\n\nFour\n 10+9-15+1"
            ),
        Arrays.<String>asList(
            "LINT testInputElements:10+1 - 9:"
            + " 'select' start tag where end tag expected.",
            "LINT testInputElements:11+3 - 11: Stray start tag 'option'.",
            "LINT testInputElements:12+1 - 10: Stray end tag 'select'.",
            "LINT testInputElements:13+1 - 10: Stray end tag 'select'.",
            "LINT testInputElements:14+1 - 11: Stray start tag 'optgroup'.",
            "LINT testInputElements:14+11 - 19: Stray start tag 'option'."
            ),
        Arrays.asList(
            "<form>",
            "<textarea>  &lt;form action=&#39;a?b=c&amp;d=e&amp;f=g&#39;&gt;",
            " &lt;/form&gt;",
            "</textarea>",
            "",
            "<select>",
            "  <option>One",
            "  </option><option>Two</option>",
            "</select>",
            "  Three",
            "",
            "",
            "Four",
            "</form>"
            )
        );
  }

  public void testUnknownTagsNest() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html><foo><bar>baz</bar></baz>boo<command></html>"
            ),
        Arrays.asList(
            "Tag : html 1+1-1+51",
            "  Tag : head 1+7-1+7",
            "  Tag : body 1+7-1+44",
            "    Tag : foo 1+7-1+26",
            "      Tag : bar 1+12-1+26",
            "        Text : baz 1+17-1+20",
            "    Text : boo 1+32-1+35",
            "    Tag : command 1+35-1+51"
            ),
        Arrays.asList(
            "LINT testUnknownTagsNest:1+26 - 32: Unclosed element 'foo'.",
            "LINT testUnknownTagsNest:1+44 - 51:"
            + " End tag for 'html' seen but there were unclosed elements."
            ),
        Arrays.asList(
            ("<html><head></head><body>"
             + "<foo><bar>baz</bar></foo>boo<command></command></body></html>")
            )
        );
  }

  public void testRegularTags() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<span><span>Foo</span>Bar</span></span>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+40",
            "  Tag : span 1+1-1+33",
            "    Tag : span 1+7-1+23",
            "      Text : Foo 1+13-1+16",
            "    Text : Bar 1+23-1+26"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<span><span>Foo</span>Bar</span>"
            )
        );
  }

  public void testXmp() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<p><b>Foo</b><xmp><b>Foo</b></xmp><b>Foo</b></p>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+49",
            "  Tag : p 1+1-1+49",
            "    Tag : b 1+4-1+14",
            "      Text : Foo 1+7-1+10",
            "    Tag : xmp 1+14-1+35",
            "      Text : <b>Foo</b> 1+19-1+29",
            "    Tag : b 1+35-1+45",
            "      Text : Foo 1+38-1+41"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<p><b>Foo</b><xmp><b>Foo</b></xmp><b>Foo</b></p>"
            )
        );
  }

  public void testColumnGroups() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<table><caption>Test</caption>",
            "<colgroup>",
            "  <col class=red>",
            "  <col class=green>",
            "  <col class=blue>",
            "</colgroup>",
            "<tr><th>red<th>green<th>blue",
            "</table>"
            ),
        Arrays.asList(
            "Fragment 1+1-8+9",
            "  Tag : table 1+1-8+9",
            "    Tag : caption 1+8-1+31",
            "      Text : Test 1+17-1+21",
            "    Text : \n 1+31-2+1",
            "    Tag : colgroup 2+1-6+12",
            "      Text : \n   2+11-3+3",
            "      Tag : col 3+3-3+18",
            "        Attrib : class 3+8-3+13",
            "          Value : red 3+14-3+17",
            "      Text : \n   3+18-4+3",
            "      Tag : col 4+3-4+20",
            "        Attrib : class 4+8-4+13",
            "          Value : green 4+14-4+19",
            "      Text : \n   4+20-5+3",
            "      Tag : col 5+3-5+19",
            "        Attrib : class 5+8-5+13",
            "          Value : blue 5+14-5+18",
            "      Text : \n 5+19-6+1",
            "    Text : \n 6+12-7+1",
            "    Tag : tbody 7+1-8+1",
            "      Tag : tr 7+1-8+1",
            "        Tag : th 7+5-7+12",
            "          Text : red 7+9-7+12",
            "        Tag : th 7+12-7+21",
            "          Text : green 7+16-7+21",
            "        Tag : th 7+21-8+1",
            "          Text : blue\n 7+25-8+1"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<table><caption>Test</caption>",
            "<colgroup>",
            "  <col class=\"red\" />",
            "  <col class=\"green\" />",
            "  <col class=\"blue\" />",
            "</colgroup>",
            "<tbody><tr><th>red</th><th>green</th><th>blue",
            "</th></tr></tbody></table>"
            )
        );
  }

  public void testMalformedTables() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<div><table>",
            "  <col class=red>",
            "  <col class=green>",
            "  <col class=blue>",
            "  <tfoot>",
            "    <tr><th>red<th>green<th>blue",
            "  </tbody></tfoot>",  // tbody does not close tfoot
            "  <table>",  // Opens a new table
            "</colgroup></table>"
            ),
        Arrays.asList(
            "Fragment 1+1-9+20",
            "  Tag : div 1+1-9+20",
            "    Tag : table 1+6-8+3",
            "      Text : \n   1+13-2+3",
            "      Tag : colgroup 2+3-5+3",
            "        Tag : col 2+3-2+18",
            "          Attrib : class 2+8-2+13",
            "            Value : red 2+14-2+17",
            "        Text : \n   2+18-3+3",
            "        Tag : col 3+3-3+20",
            "          Attrib : class 3+8-3+13",
            "            Value : green 3+14-3+19",
            "        Text : \n   3+20-4+3",
            "        Tag : col 4+3-4+19",
            "          Attrib : class 4+8-4+13",
            "            Value : blue 4+14-4+18",
            "        Text : \n   4+19-5+3",
            "      Tag : tfoot 5+3-7+19",
            "        Text : \n     5+10-6+5",
            "        Tag : tr 6+5-7+11",
            "          Tag : th 6+9-6+16",
            "            Text : red 6+13-6+16",
            "          Tag : th 6+16-6+25",
            "            Text : green 6+20-6+25",
            "          Tag : th 6+25-7+11",
            "            Text : blue\n   6+29-7+3",
            "      Text : \n   7+19-8+3",
            "    Tag : table 8+3-9+20",
            "      Text : \n 8+10-9+1"
            ),
        Arrays.asList(
            "LINT testMalformedTables:7+3 - 11: Stray end tag 'tbody'.",
            "LINT testMalformedTables:8+3 - 10: Start tag for 'table'"
            + " seen but the previous 'table' is still open.",
            "LINT testMalformedTables:9+1 - 12: Stray end tag 'colgroup'.",
            "LINT testMalformedTables:9+20: End of file seen and there were"
            + " open elements."),
        Arrays.asList(
            "<div><table>",
            "  <colgroup><col class=\"red\" />",
            "  <col class=\"green\" />",
            "  <col class=\"blue\" />",
            "  </colgroup><tfoot>",
            "    <tr><th>red</th><th>green</th><th>blue",
            "  </th></tr></tfoot>",
            "  </table><table>",
            "</table></div>"
            )
        );
  }

  public void testMoreTables() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<div>",
            "<p><table>",
            "<p>Foo</p>",
            "<tr>",
            "<caption><tr>",
            "</table>"
            ),
        Arrays.asList(
            "Fragment 1+1-6+9",
            "  Tag : div 1+1-6+9",
            "    Text : \n 1+6-2+1",
            "    Tag : p 2+1-2+4",
            "    Tag : p 3+1-3+11",
            "      Text : Foo 3+4-3+7",
            "    Tag : table 2+4-6+9",
            "      Text : \n\n 2+11-4+1",
            "      Tag : tbody 4+1-5+1",
            "        Tag : tr 4+1-5+1",
            "          Text : \n 4+5-5+1",
            "      Tag : caption 5+1-5+10",
            "      Tag : tbody 5+10-6+1",
            "        Tag : tr 5+10-6+1",
            "          Text : \n 5+14-6+1"
            ),
        Arrays.asList(
            "LINT testMoreTables:3+1 - 4: Start tag 'p' seen in 'table'.",
            "LINT testMoreTables:3+7 - 11: Stray end tag 'p'.",
            "LINT testMoreTables:5+10 - 14:"
            + " Stray 'tr' start tag in 'caption'.",
            "LINT testMoreTables:6+9:"
            + " End of file seen and there were open elements."
            ),
        Arrays.asList(
            "<div>",
            "<p></p><p>Foo</p><table>",
            "",
            "<tbody><tr>",
            "</tr></tbody><caption></caption><tbody><tr>",
            "</tr></tbody></table></div>"
            )
        );
  }

  public void testEvenMoreTables() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<table><colgroup><col></col><caption></colgroup></caption>",
            "</thead></table>"
            ),
        Arrays.asList(
            "Fragment 1+1-2+17",
            "  Tag : table 1+1-2+17",
            "    Tag : colgroup 1+8-1+29",
            "      Tag : col 1+18-1+23",
            "    Tag : caption 1+29-1+59",
            "    Text : \n 1+59-2+1"
            ),
        Arrays.asList(
            "LINT testEvenMoreTables:1+23 - 29: Stray end tag 'col'.",
            "LINT testEvenMoreTables:1+38 - 49: Stray end tag 'colgroup'.",
            "LINT testEvenMoreTables:2+1 - 9: Stray end tag 'thead'."
            ),
        Arrays.asList(
            "<table><colgroup><col /></colgroup><caption></caption>",
            "</table>"
            )
        );
  }

  public void testTablesBonus() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<div><table><tbody></body></br><tr></td></tr></table></div>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+60",
            "  Tag : div 1+1-1+60",
            "    Tag : br 1+27-1+32",
            "    Tag : table 1+6-1+54",
            "      Tag : tbody 1+13-1+46",
            "        Tag : tr 1+32-1+46"
            ),
        Arrays.<String>asList(
            "LINT testTablesBonus:1+20 - 27: Stray end tag 'body'.",
            "LINT testTablesBonus:1+27 - 32: Stray end tag 'br'.",
            "LINT testTablesBonus:1+27 - 32: End tag 'br'.",
            "LINT testTablesBonus:1+36 - 41: Stray end tag 'td'."
            ),
        Arrays.asList(
            "<div><br /><table><tbody><tr></tr></tbody></table></div>"
            )
        );
  }

  public void testTableRows() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(  // table does not end because none of these close the th
            "<div><table><tr><hr><td></th></html><select></td></table></div>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+64",
            "  Tag : div 1+1-1+64",
            "    Tag : hr 1+17-1+21",
            "    Tag : table 1+6-1+64",
            "      Tag : tbody 1+13-1+64",
            "        Tag : tr 1+13-1+64",
            "          Tag : td 1+21-1+64",
            "            Tag : select 1+37-1+64"
            ),
        Arrays.<String>asList(
            "LINT testTableRows:1+17 - 21: Start tag 'hr' seen in 'table'.",
            "LINT testTableRows:1+25 - 30: Stray end tag 'th'.",
            "LINT testTableRows:1+30 - 37: Stray end tag 'html'.",
            "LINT testTableRows:1+45 - 50: Stray end tag 'td'",
            "LINT testTableRows:1+50 - 58: Stray end tag 'table'",
            "LINT testTableRows:1+58 - 64: Stray end tag 'div'",
            "LINT testTableRows:1+64:"
            + " End of file seen and there were open elements."
            ),
        Arrays.asList(
            ("<div><hr /><table><tbody><tr><td><select></select>"
             + "</td></tr></tbody></table></div>")
            )
        );
  }

  public void testSelects() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<select>",
            "</optgroup><optgroup>",
            "<optgroup>",
            "<option>1</optgroup>",
            "</table><hr></select>"
            ),
        Arrays.asList(
            "Fragment 1+1-5+22",
            "  Tag : select 1+1-5+22",
            "    Text : \n 1+9-2+1",
            "    Tag : optgroup 2+12-3+1",
            "      Text : \n 2+22-3+1",
            "    Tag : optgroup 3+1-4+21",
            "      Text : \n 3+11-4+1",
            "      Tag : option 4+1-4+10",
            "        Text : 1 4+9-4+10",
            "    Text : \n 4+21-5+1"
            ),
        Arrays.asList(
            "LINT testSelects:2+1 - 12: Stray end tag 'optgroup'",
            "LINT testSelects:5+1 - 9: Stray end tag 'table'",
            "LINT testSelects:5+9 - 13: Stray 'hr' start tag."
            ),
        Arrays.asList(
            "<select>",
            "<optgroup>",
            "</optgroup><optgroup>",
            "<option>1</option></optgroup>",
            "</select>"
            )
        );
  }

  public void testTrailingEndPhase() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html></html><br>"
            ),
        Arrays.asList(
            "Tag : html 1+1-1+18",
            "  Tag : head 1+7-1+7",
            "  Tag : body 1+7-1+18",
            "    Tag : br 1+14-1+18"
            ),
        Arrays.asList(
            "LINT testTrailingEndPhase:1+14 - 18: Stray 'br' start tag."
            ),
        Arrays.asList(
            "<html><head></head><body><br /></body></html>"
            )
        );
  }

  public void testValuelessAttributes() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<input type=checkbox checked>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+30",
            "  Tag : input 1+1-1+30",
            "    Attrib : type 1+8-1+12",
            "      Value : checkbox 1+13-1+21",
            "    Attrib : checked 1+22-1+29",
            "      Value : checked 1+22-1+29"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<input type=\"checkbox\" checked=\"checked\" />"
            )
        );
  }

  public void testNoDoctypeGuessAsHtml() throws Exception {
    assertParsedMarkup(
        Arrays.asList(
            "<xmp><br/></xmp>"
            ),
        Arrays.asList(
            "Tag : html 1+1-1+17",
            "  Tag : head 1+1-1+1",
            "  Tag : body 1+1-1+17",
            "    Tag : xmp 1+1-1+17",
            "      Text : <br/> 1+6-1+11"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<html><head></head><body><xmp><br/></xmp></body></html>"
            ),
        null, false);
    // From Issue 556
    assertParsedMarkup(
        Arrays.asList(
            "<script>document.write('</b');</script>"
            ),
        Arrays.asList(
            "Tag : html 1+1-1+40",
            "  Tag : head 1+1-1+40",
            "    Tag : script 1+1-1+40",
            "      Text : document.write('</b'); 1+9-1+31",
            "  Tag : body 1+40-1+40"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<html><head><script>document.write('</b');</script></head>"
            + "<body></body></html>"
            ),
        null, false);
  }

  public void testDoctypeGuessAsHtml() throws Exception {
    assertParsedMarkup(
        Arrays.asList(
            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">",
            "<xmp><br/></xmp>"
            ),
        Arrays.asList(
            "Fragment 1+56-2+17",
            "  Tag : xmp 2+1-2+17",
            "    Text : <br/> 2+6-2+11"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<xmp><br/></xmp>"
            ),
        null,
        // We have one of these type guessing tests parse a fragment to tests
        // the behavior of fragments around DOCTYPEs.
        true);
  }

  public void testDoctypeGuessAsXhtml() throws Exception {
    assertParsedMarkup(
        Arrays.asList(
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"",
            "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">",
            "<xmp><br/></xmp>"
            ),
        Arrays.asList(
            "Tag : xmp 3+1-3+17",
            "  Tag : br 3+6-3+11"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<xmp><br /></xmp>"
            ),
        null, false);
  }

  public void testXmlPrologueTreatedAsXml() throws Exception {
    assertParsedMarkup(
        Arrays.asList(
            "<?xml version=\"1.0\"?>",
            "<xmp><br/></xmp>"
            ),
        Arrays.asList(
            "Tag : xmp 2+1-2+17",
            "  Tag : br 2+6-2+11"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<xmp><br /></xmp>"
            ),
        null, false);
  }

  public void testFileExtensionsBasedContentTypeGuessing() throws Exception {
    // Override input sources, so that DomParser has a file extension available
    // when deciding whether to treat the input as HTML or XML.
    this.is = new InputSource(URI.create("test:///" + getName() + ".html"));
    assertParsedMarkup(
        Arrays.asList(
            "<xmp><br/></xmp>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+17",
            "  Tag : xmp 1+1-1+17",
            "    Text : <br/> 1+6-1+11"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<xmp><br/></xmp>"
            ),
        null, true);

    this.is = new InputSource(URI.create("test:///" + getName() + ".xml"));
    assertParsedMarkup(
        Arrays.asList(
            "<xmp><br/></xmp>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+17",
            "  Tag : xmp 1+1-1+17",
            "    Tag : br 1+6-1+11"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<xmp><br /></xmp>"
            ),
        null, true);

    this.is = new InputSource(URI.create("test:///" + getName() + ".xhtml"));
    assertParsedMarkup(
        Arrays.asList(
            "<xmp><br/></xmp>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+17",
            "  Tag : xmp 1+1-1+17",
            "    Tag : br 1+6-1+11"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<xmp><br /></xmp>"
            ),
        null, true);
  }

  public void testQualifiedNameTreatedAsXml() throws Exception {
    assertParsedMarkup(
        Arrays.asList(
            "<html:xmp><br/></html:xmp>"
            ),
        Arrays.asList(
            "Tag : html:xmp 1+1-1+27",
            "  Tag : br 1+11-1+16"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<html:xmp><br /></html:xmp>"
            ),
        null, false);
  }

  public void testAmbiguousAttributes() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<a href= title=foo>bar</a>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+27",
            "  Tag : a 1+1-1+27",
            "    Attrib : href 1+4-1+8",
            "      Value : title=foo 1+10-1+19",
            "    Text : bar 1+20-1+23"
            ),
        Arrays.asList(
            "WARNING testAmbiguousAttributes:1+4 - 19:"
            + " attribute href has ambiguous value \"title=foo\""),
        Arrays.asList(
            "<a href=\"title=foo\">bar</a>"
            )
        );
    assertParsedHtmlFragment(
        Arrays.asList(
            "<a href= \"title=foo\">bar</a>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+29",
            "  Tag : a 1+1-1+29",
            "    Attrib : href 1+4-1+8",
            "      Value : title=foo 1+10-1+21",
            "    Text : bar 1+22-1+25"
            ),
        Arrays.<String>asList(),  // No warning since not ambiguous
        Arrays.asList(
            "<a href=\"title=foo\">bar</a>"
            )
        );
  }

  public void testEndTagCruft() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<a href=foo>bar</a href>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+25",
            "  Tag : a 1+1-1+25",
            "    Attrib : href 1+4-1+8",
            "      Value : foo 1+9-1+12",
            "    Text : bar 1+13-1+16"
            ),
        Arrays.asList(
            "WARNING testEndTagCruft:1+20 - 24: ignoring token href"),
        Arrays.asList(
            "<a href=\"foo\">bar</a>"
            )
        );
    assertParsedHtmlFragment(
        Arrays.asList(
            "<a href=foo>bar</a \n \t\r\n >"
            ),
        Arrays.asList(
            "Fragment 1+1-3+3",
            "  Tag : a 1+1-3+3",
            "    Attrib : href 1+4-1+8",
            "      Value : foo 1+9-1+12",
            "    Text : bar 1+13-1+16"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<a href=\"foo\">bar</a>"
            )
        );
  }

  public void testMisplacedQuotes() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<span title=malformed attribs' do=don't id=foo checked",
            "onclick=\"a<b\">Bar</span>"
            ),
        Arrays.asList(
            "Fragment 1+1-2+25",
            "  Tag : span 1+1-2+25",
            "    Attrib : title 1+7-1+12",
            "      Value : malformed attribs 1+13-1+31",
            "    Attrib : do 1+32-1+34",
            "      Value : don't 1+35-1+40",
            "    Attrib : id 1+41-1+43",
            "      Value : foo 1+44-1+47",
            "    Attrib : checked 1+48-1+55",
            "      Value : checked 1+48-1+55",
            "    Attrib : onclick 2+1-2+8",
            "      Value : a<b 2+9-2+14",
            "    Text : Bar 2+15-2+18"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<span title=\"malformed attribs\" do=\"don&#39;t\" id=\"foo\""
            + " checked=\"checked\" onclick=\"a&lt;b\">Bar</span>"
            )
        );
  }

  public void testShortTags() throws Exception {
    // See comments in html-sanitizer-test.js as to why we don't bother with
    // short tags.  In short, they are not in HTML5 and not implemented properly
    // in existing HTML4 clients.
    assertParsedHtmlFragment(
        Arrays.asList(
            "<p<a href=\"/\">first part of the text</> second part"
            ),
        Arrays.asList(
            "Fragment 1+1-1+52",
            "  Tag : p 1+1-1+52",
            "    Attrib : <a 1+3-1+5",
            "      Value : <a 1+3-1+5",
            "    Attrib : href 1+6-1+10",
            "      Value : / 1+11-1+14",
            "    Text : first part of the text</> second part 1+15-1+52"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<p &lt;a=\"&lt;a\" href=\"/\">"
            + "first part of the text&lt;/&gt; second part</p>"
            )
        );
    try {
      htmlFragment(fromString("<p/b/"));
      fail("Expected parse exception");
    } catch (ParseException ex) {
      assertEquals(
          MessageType.END_OF_FILE, ex.getCajaMessage().getMessageType());
    }
  }

  public void testLeadingAndTrailingContent() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "xyzw<body></body>xyzw"
            ),
        Arrays.asList(
            "Tag : html 1+1-1+22",
            "  Tag : head 1+1-1+1",
            "  Tag : body 1+1-1+22",  // Expanded to contain text content.
            "    Text : xyzwxyzw 1+1-1+22"  // Spans both text chunks.
            ),
        Arrays.asList(
            "LINT testLeadingAndTrailingContent:1+5 - 11:"
            + " 'body' start tag found but the 'body' element is already open.",
            "LINT testLeadingAndTrailingContent:1+18 - 22:"
            + " Non-space character after body."
            ),
        Arrays.asList(
            "<html><head></head><body>xyzwxyzw</body></html>"
            ));

    assertParsedHtml(
        Arrays.asList(
            "\r\n \t<body></body>\r\n \t"
            ),
        Arrays.asList(
            "Tag : html 2+3-3+3",
            "  Tag : head 2+3-2+3",
            "  Tag : body 2+3-2+16",
            "    Text : \n \t 2+16-3+3"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<html><head></head><body>",
            " \t</body></html>"
            ));
  }

  public void testRender() throws Exception {
    DomTree t = xmlFragment(fromString(
        ""
        + "<script><![CDATA[ foo() < bar() ]]></script>\n"
        + "<p />\n"
        + "<![CDATA[ 1 < 2 && 3 > 4 ]]>\n"
        + "<xmp>1 &lt; 2</xmp>\n"
        + "<script> foo() &lt; bar() </script>"));

    // Rendered as XML
    assertEquals(
        ""
        + "<script><![CDATA[ foo() < bar() ]]></script>\n"
        + "<p />\n"
        + "<![CDATA[ 1 < 2 && 3 > 4 ]]>\n"
        + "<xmp>1 &lt; 2</xmp>\n"
        + "<script> foo() &lt; bar() </script>",
        render(t, true));
    // Rendered as HTML
    assertEquals(
        ""
        + "<script> foo() < bar() </script>\n"
        + "<p></p>\n"
        + " 1 &lt; 2 &amp;&amp; 3 &gt; 4 \n"
        + "<xmp>1 < 2</xmp>\n"
        + "<script> foo() < bar() </script>",
        render(t, false));
  }

  public void testUnrenderableXMLTree1() throws Exception {
    DomTree t = xmlFragment(fromString("<xmp><![CDATA[ </xmp> ]]></xmp>"));
    assertEquals("<xmp><![CDATA[ </xmp> ]]></xmp>", render(t, true));
    try {
      String badness = render(t, false);
      fail("Bad HTML rendered: " + badness);
    } catch (IllegalStateException ex) {
      // Cannot produce <xmp></xmp></xmp> safely in HTML.
    }
  }

  public void testUnrenderableXMLTree2() throws Exception {
    DomTree t = xmlFragment(fromString("<xmp><![CDATA[ </xM]]>p </xmp>"));
    assertEquals("<xmp><![CDATA[ </xM]]>p </xmp>", render(t, true));
    try {
      String badness = render(t, false);
      fail("Bad HTML rendered: " + badness);
    } catch (IllegalStateException ex) {
      // Cannot produce <xmp> </xMp </xmp> safely in HTML.
    }
  }

  public void testUnrenderableXMLTree3() throws Exception {
    DomTree t = xmlFragment(fromString("<xmp> &lt;/XM<!-- -->P&gt; </xmp>"));
    assertEquals("<xmp> &lt;/XMP&gt; </xmp>", render(t, true));
    try {
      String badness = render(t, false);
      fail("Bad HTML rendered: " + badness);
    } catch (IllegalStateException ex) {
      // Cannot produce <xmp> </XMP> </xmp> safely in HTML.
    }
  }

  public void testCommentsHidingCdataEnd() throws Exception {
    DomTree t = xmlFragment(fromString("<xmp> <!-- </xmp> --> </xmp>"));
    assertEquals("<xmp>  </xmp>", render(t, true));
    assertEquals("<xmp>  </xmp>", render(t, false));
  }

  private void assertParsedHtml(
      List<String> htmlInput,
      List<String> expectedParseTree,
      List<String> expectedMessages,
      List<String> expectedOutputHtml)
      throws ParseException {
    assertParsedMarkup(htmlInput, expectedParseTree, expectedMessages,
                       expectedOutputHtml, false, false);
  }

  private void assertParsedHtmlFragment(
      List<String> htmlInput,
      List<String> expectedParseTree,
      List<String> expectedMessages,
      List<String> expectedOutputHtml)
      throws ParseException {
    assertParsedMarkup(htmlInput, expectedParseTree, expectedMessages,
                       expectedOutputHtml, false, true);
  }

  private void assertParsedMarkup(
      List<String> htmlInput,
      List<String> expectedParseTree,
      List<String> expectedMessages,
      List<String> expectedOutputHtml,
      Boolean asXml,
      boolean fragment)
      throws ParseException {

    System.err.println("\n\nStarting " + getName() + "\n===================");
    mq.getMessages().clear();

    DomParser p;
    if (asXml != null) {  // specified
      TokenQueue<HtmlTokenType> tq = tokenizeTestInput(
          Join.join("\n", htmlInput), asXml);
      p = new DomParser(tq, asXml, mq);
    } else {
      HtmlLexer lexer = new HtmlLexer(fromString(Join.join("\n", htmlInput)));
      p = new DomParser(lexer, is, mq);
      asXml = lexer.getTreatedAsXml();
    }
    DomTree tree = fragment ? p.parseFragment() : p.parseDocument();

    List<String> actualParseTree = new ArrayList<String>();
    formatWithLinePositions(tree, mc, 0, new IdentityHashMap<DomTree, Void>(),
                            actualParseTree);
    MoreAsserts.assertListsEqual(expectedParseTree, actualParseTree);

    List<String> actualMessages = new ArrayList<String>();
    for (Message message : mq.getMessages()) {
      String messageText = (message.getMessageLevel().name() + " "
                            + message.format(mc));
      actualMessages.add(messageText);
    }
    MoreAsserts.assertListsEqual(expectedMessages, actualMessages, 0);

    MoreAsserts.assertListsEqual(
        expectedOutputHtml, Arrays.asList(render(tree, asXml).split("\n")));
    DomTree clone = tree.clone();
    MoreAsserts.assertListsEqual(
        expectedOutputHtml, Arrays.asList(render(clone, asXml).split("\n")));
  }

  private String render(DomTree t, boolean asXml) {
    StringBuilder sb = new StringBuilder();
    RenderContext rc = new MarkupRenderContext(
        mc, t.makeRenderer(sb, null), asXml);
    t.render(rc);
    rc.getOut().noMoreTokens();
    return sb.toString();
  }

  private TokenQueue<HtmlTokenType> tokenizeTestInput(
      String sgmlInput, boolean asXml) {
    HtmlLexer lexer = new HtmlLexer(fromString(sgmlInput));
    lexer.setTreatedAsXml(asXml);
    return new TokenQueue<HtmlTokenType>(
        lexer, is, Criterion.Factory.<Token<HtmlTokenType>>optimist());
  }

  private void formatWithLinePositions(
      DomTree tree, MessageContext mc, int depth,
      IdentityHashMap<DomTree, ?> seen, List<? super String> out) {

    StringBuilder sb = new StringBuilder();
    for (int i = depth; --i >= 0;) { sb.append("  "); }
    sb.append(tree.toString());
    FilePosition pos = tree.getFilePosition();
    if (pos != null) {
      sb.append(' ')
          .append(pos.startLineNo()).append('+').append(pos.startCharInLine())
          .append('-')
          .append(pos.endLineNo()).append('+').append(pos.endCharInLine());
    }

    if (seen.containsKey(tree)) {
      sb.append(" !DUPE");
      out.add(sb.toString());
      return;
    } else {
      seen.put(tree, null);
      out.add(sb.toString());
    }

    for (DomTree child : tree.children()) {
      formatWithLinePositions(child, mc, depth + 1, seen, out);
    }
  }
}
