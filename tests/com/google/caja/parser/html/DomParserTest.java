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

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.reporting.DevNullMessageQueue;
import com.google.caja.reporting.MarkupRenderMode;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Function;
import com.google.caja.util.Join;
import com.google.caja.util.Lists;
import com.google.caja.util.MoreAsserts;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Testcase for {@link DomParser}.
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
      "Element : foo 2+1-6+7\n"
      + "  Attrib : a 2+6-2+7\n"
      + "    Value : b 2+8-2+11\n"
      + "  Attrib : c 2+12-2+13\n"
      + "    Value : d 2+15-2+18\n"
      + "  Attrib : e 2+19-2+20\n"
      + "    Value : <\"f\"&amp; 2+23-2+51\n"
      + "  Text : \\n 2+52-3+1\n"
      + "  Element : bar 3+1-3+7\n"
      + "  Text :   3+7-3+8\n"
      + "  Element : bar 3+8-3+15\n"
      + "  Text :  before  after \\nHello <there>\\n 3+15-5+1\n"
      + "  Element : baz 5+1-5+37\n"
      + "    CDATA : Hello <there> 5+6-5+31\n"
      + "  Text : \\n 5+37-6+1"
      );

  static final String DOM1_XML_RENDERED_GOLDEN = (
      ""
      + "<foo a=\"b\" c=\"d\" e=\"&lt;&#34;f&#34;&amp;amp;\">\n"
      + "<bar></bar> <bar></bar> before  after \n"
      + "Hello &lt;there&gt;\n"
      + "<baz>Hello &lt;there&gt;</baz>\n"
      + "</foo>"
      );

  static final String DOM2_HTML = (
      ""
      + "<html>\n"
      + "  <head>\n"
      + "    <title>Title</title>\n"
      + "  </head>\n"
      + "  <body onload=foo()>\n"
      + "    <!-- a comment -->\n"
      + "    <p>Foo &lt; Bar</p>\n"
      + "    <script>function foo() { alert('Hello, World!'); }</script>\n"
      + "    <ul><li>One</li><li>Two</li></ul>\n"
      + "  </body>\n"
      + "</html>"
      );

  static final String DOM2_HTML_RENDERED_GOLDEN = (
      ""
      + "<html><head>\n"
      + "    <title>Title</title>\n"
      + "  </head>\n"
      + "  <body onload=\"foo()\">\n"
      + "    \n"
      + "    <p>Foo &lt; Bar</p>\n"
      + "    <script>function foo() { alert('Hello, World!'); }</script>\n"
      + "    <ul><li>One</li><li>Two</li></ul>\n"
      + "  \n"
      + "</body></html>"
      );

  static final String DOM3_XML = (
      "<element src=\"http://example.org/valid-src\"/>");

  static final String DOM3_XML_RENDERED_GOLDEN = (
      "<element src=\"http://example.org/valid-src\"></element>");

  private static <T> Iterable<T> cartesianProduct(
      final Function<Object[], T> fn, Object[]... options) {
    final Object[][] opts = new Object[options.length][];
    for (int i = opts.length; --i >= 0;) {
      // NPE on null input collections, and handle the empty output case here
      // since the iterator code below assumes that it is not exhausted the
      // first time through fetch.
      if (options[i].length == 0) { return Collections.emptySet(); }
      opts[i] = options[i].clone();
    }
    return new Iterable<T>() {
      public Iterator<T> iterator() {
        return new Iterator<T>() {
          final int[] pos = new int[opts.length];
          boolean hasPending;
          T pending;
          boolean exhausted;

          public boolean hasNext() {
            fetch();
            return hasPending;
          }

          public T next() {
            fetch();
            if (!hasPending) { throw new NoSuchElementException(); }
            T out = pending;
            pending = null;  // release for GC
            hasPending = false;
            return out;
          }

          public void remove() { throw new UnsupportedOperationException(); }

          private void fetch() {
            if (hasPending || exhausted) { return; }
            // Produce a result.
            int n = pos.length;
            Object[] args = new Object[n];
            for (int j = n; --j >= 0;) { args[j] = opts[j][pos[j]]; }
            pending = fn.apply(args);
            hasPending = true;
            // Increment to next.
            for (int i = n; --i >= 0;) {
              if (++pos[i] < opts[i].length) {
                for (int j = n; --j > i;) { pos[j] = 0; }
                return;
              }
            }
            exhausted = true;
          }
        };
      }
    };
  }

  private Iterable<DomParser> allPossibleConfigurations(
      final String input, Boolean asXml) {
    Boolean[] xmlConfigs = asXml != null
        ? new Boolean[] { asXml } : new Boolean[] { false, true };
    Boolean[] needsDebugData = new Boolean[] { false, true };
    Boolean[] wantsComments = new Boolean[] { false, true };

    return cartesianProduct(new Function<Object[], DomParser>() {
      public DomParser apply(Object[] args) {
        boolean asXml = (Boolean) args[0];
        boolean needsDebugData = (Boolean) args[1];
        boolean wantsComments = (Boolean) args[2];
        TokenQueue<HtmlTokenType> tq = tokenizeTestInput(
            input, asXml, wantsComments);
        DomParser p = new DomParser(tq, asXml, mq);
        p.setNeedsDebugData(needsDebugData);
        return p;
      }
    }, xmlConfigs, needsDebugData, wantsComments);
  }

  public final void testIllegalAttributes() throws Exception {
    String helloTag =
        "<hello "
        + "xmlns:data = 'http://1.com' "
        + "attr = 'value' "
        + "data:attr = 'value2' "

        // Illegal attribute caught in maybeCreateAttributeNS.
        + "hi~ = 'v' "

        // Illegal attribute caught in maybeCreateAttribute.
        + "data:hi~ = 'v' "

        // Illegal attribute caught in DomParser.createAttributeAndAddToElement.
        + "data: = 'buff' "
        + "xmlns: = 'buffalo' "

        // Illegal attribute caught by error check in
        // Html5ElementStack.processTag code for disallowing overriding of the
        // default namespace.
        + "xmlns = 'http://xmlns/index.html'>";

    String[] htmlInput = {
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 1.0 Transitional//EN\"",
        "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">",
        "<html><body>",
        helloTag,
        "</hello>",
        "</body></html>" };
    TokenQueue<HtmlTokenType> tq = tokenizeTestInput(
        Join.join("\n", htmlInput), false, true);
    DomParser parser = new DomParser(tq, false, mq);

    Document doc = parser.parseDocument().getOwnerDocument();
    assertEquals(5, mq.getMessages().size());

    final String TEST_NAME = "test://example.org/testIllegalAttributes";
    assertEquals(TEST_NAME+ ":4+72 - 75: ignoring token 'hi~'",
                 mq.getMessages().get(0).toString());
    assertEquals(TEST_NAME + ":4+82 - 90: ignoring token 'data:hi~'",
                 mq.getMessages().get(1).toString());
    assertEquals(TEST_NAME +
                 ":4+131 - 136: cannot override default XML namespace in HTML",
                 mq.getMessages().get(2).toString());
    assertEquals(TEST_NAME + ":4+97 - 102: ignoring token 'data:'",
                 mq.getMessages().get(3).toString());
    assertEquals(TEST_NAME + ":4+112 - 118: ignoring token 'xmlns:'",
                 mq.getMessages().get(4).toString());

    Element elem = (Element) doc.getElementsByTagName("hello").item(0);
    assertEquals(3, elem.getAttributes().getLength());
    assertEquals("value", elem.getAttribute("attr"));
    assertEquals("value2", elem.getAttribute("data:attr"));
    assertEquals("http://1.com", elem.getAttribute("xmlns:data"));
  }

  public final void testIllegalElementInBuilderStartTag() throws Exception {
    String[] htmlInput = {
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 1.0 Transitional//EN\"",
        "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">",
        "<html><body>",
        "<li><a href='url'>MY BOOK ON AMAZON<img.jpg\"><blah>hello</blah></a>",
        "</body></html>" };
    TokenQueue<HtmlTokenType> tq = tokenizeTestInput(
        Join.join("\n", htmlInput), false, true);
    DomParser parser = new DomParser(tq, false, mq);

    Document doc = parser.parseDocument().getOwnerDocument();
    assertEquals(1, mq.getMessages().size());
    assertMessage(MessageType.INVALID_TAG_NAME, MessageLevel.WARNING);

    Element elem = (Element) doc.getElementsByTagName("blah").item(0);
    assertEquals("hello", elem.getTextContent());

    String expected =
        "<html><head></head><body>\n" +
        "<li><a href=\"url\">MY BOOK ON AMAZON<blah>hello</blah></a>\n" +
        "</li></body></html>";
    String serialized = Nodes.render(doc);
    assertEquals(expected, serialized);
  }

  // TODO(gagan): Refactor this test to use assertParsedHtml.
  public final void testIllegalElementInFixup() throws Exception {
    // Browsers like chrome and firefox do create elements like "a:b~", but
    // it's not a valid w3c attribute name.
    String[] htmlInput = {
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 1.0 Transitional//EN\"",
        "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">",
        "<html><body>",
        "<a href='url1'>Deal?; Tracking the Oil> ",
        "<Sect: blah News; Intl</a><BR>",
        "<a:b~>helo helo <p>helloo</p></a:b~>",
        "</body></html>" };
    TokenQueue<HtmlTokenType> tq = tokenizeTestInput(
        Join.join("\n", htmlInput), false, true);
    DomParser parser = new DomParser(tq, false, mq);

    Document doc = parser.parseDocument().getOwnerDocument();
    assertEquals(8, mq.getMessages().size());

    assertMessage(MessageType.INVALID_TAG_NAME, MessageLevel.WARNING,
        FilePosition.instance(is, 6, 207, 0),
        MessagePart.Factory.valueOf("a:b~"));
    assertMessage(MessageType.INVALID_TAG_NAME, MessageLevel.WARNING,
        FilePosition.instance(is, 6, 236, 0),
        MessagePart.Factory.valueOf("a:b~"));
    assertMessage(DomParserMessageType.GENERIC_SAX_ERROR, MessageLevel.LINT,
        FilePosition.instance(is, 7, 244, 0, 7),
        MessagePart.Factory.valueOf("End tag for 'body' seen but there were " +
            "unclosed elements."));
    assertMessage(MessageType.INVALID_TAG_NAME, MessageLevel.WARNING,
        FilePosition.instance(is, 5, 176, 0),
        MessagePart.Factory.valueOf("Sect:"));

    Node sectNode = doc.getElementsByTagName("Sect:").item(0);
    assertEquals("Sect:", sectNode.getNodeName());
    assertEquals(1, sectNode.getAttributes().getLength());
    assertNotNull(sectNode.getAttributes().getNamedItem("blah"));

    // sectNode.outerHtml:
    // <sect: blah="blah"><br />helo helo <p>helloo</p></sect:>
    assertEquals(4, sectNode.getChildNodes().getLength());
  }


  public final void testXmlnsTags() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<HTML xmlns:fb=\"http://www.facebook.com/2008/fbml\" >",
            "<body>",
            "<FB:LIKE width=\"300\" ></FB:LIKE>",
            "</body></html>"
        ),
        Arrays.asList(
            "Element : html 1+1-4+15",
            "  Attrib : xmlns:fb 1+7-1+15",
            "    Value : http://www.facebook.com/2008/fbml 1+16-1+51",
            "  Element : head 2+1-2+1",
            "  Element : body 2+1-4+8",
            "    Text : \\n 2+7-3+1",
            "    Element : fb:like 3+1-3+33",
            "      Attrib : width 3+10-3+15",
            "        Value : 300 3+16-3+21",
            "    Text : \\n 3+33-4+1"
        ),
        Arrays.<String>asList(
            "LINT testXmlnsTags:3+1 - 23:"
            + " Element name 'FB:LIKE' cannot be represented as XML 1.0."
        ),
        Arrays.asList(
            ("<html xmlns:fb=\"http://www.facebook.com/2008/fbml\">"
             + "<head></head><body>"),
            "<fb:like width=\"300\"></fb:like>",
            "</body></html>"
        )
    );
  }


  public final void testXmlnsMissingTags() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<HTML xmlns=\"http://www.w3.org/1999/xhtml\" >",
            "<body>",
            "</body></html>"
        ),
        Arrays.asList(
            "Element : html 1+1-3+15",
            "  Element : head 2+1-2+1",
            "  Element : body 2+1-3+8",
            "    Text : \\n 2+7-3+1"
        ),
        Arrays.<String>asList(
        ),
        Arrays.asList(
            ("<html xmlns=\"http://www.w3.org/1999/xhtml\">"
             + "<head></head><body>"),
            "</body></html>"
        )
    );
  }

  public final void testParseDom() throws Exception {
    TokenQueue<HtmlTokenType> tq = tokenizeTestInput(DOM1_XML, true, false);
    Element el = new DomParser(tq, true, mq).parseDocument();
    assertEquals(DOM1_GOLDEN, formatToString(el, true));
  }

  public final void testParseXmlManyWays() throws Exception {
    for (DomParser p : allPossibleConfigurations(DOM1_XML, true)) {
      String config = (
          "asXml=" + p.asXml() + ", needsDebugData=" + p.getNeedsDebugData()
          + ", wantsComments=" + p.getWantsComments());
      Node document = p.parseDocument();
      assertEquals(config, DOM1_XML_RENDERED_GOLDEN, Nodes.render(document));
    }
  }

  public final void testParseHtmlManyWays() throws Exception {
    for (DomParser p : allPossibleConfigurations(DOM2_HTML, false)) {
      String config = (
          "asXml=" + p.asXml() + ", needsDebugData=" + p.getNeedsDebugData()
          + ", wantsComments=" + p.getWantsComments());
      Node document = p.parseDocument();
      assertEquals(config, DOM2_HTML_RENDERED_GOLDEN, Nodes.render(document));
    }
  }

  public final void testParseFirstLineComment() throws Exception {
    String DOM_WITH_COMMENT = "<!-- This is a comment -->\n" + DOM2_HTML;
    TokenQueue<HtmlTokenType> tq = tokenizeTestInput(
        DOM_WITH_COMMENT, false, true);
    Element el = new DomParser(tq, false, mq).parseDocument();
    assertEquals(DOM2_HTML_RENDERED_GOLDEN, Nodes.render(el));
  }

  public final void testParseLastLineComment() throws Exception {
    String DOM_WITH_COMMENT = DOM2_HTML + "<!-- This is a comment -->";
    TokenQueue<HtmlTokenType> tq = tokenizeTestInput(
        DOM_WITH_COMMENT, false, true);
    Element el = new DomParser(tq, false, mq).parseDocument();
    assertEquals(DOM2_HTML_RENDERED_GOLDEN, Nodes.render(el));
  }

  public final void testParseIllegalComment() throws Exception {
    String DOM_WITH_BAD_COMMENT = DOM2_HTML + "<!-- A --bad-- comment -->";
    TokenQueue<HtmlTokenType> tq = tokenizeTestInput(
        DOM_WITH_BAD_COMMENT, false, true);
    Element el = new DomParser(tq, false, mq).parseDocument();
    assertEquals(DOM2_HTML_RENDERED_GOLDEN, Nodes.render(el));

    assertEquals(1, mq.getMessages().size());
    assertMessage(MessageType.INVALID_HTML_COMMENT, MessageLevel.WARNING);
  }

  public final void testParseXmlFragmentManyWays() throws Exception {
    for (DomParser p : allPossibleConfigurations(DOM3_XML, false)) {
      String config = (
          "asXml=" + p.asXml() + ", needsDebugData=" + p.getNeedsDebugData()
          + ", wantsComments=" + p.getWantsComments());
      Node fragment = p.parseFragment();
      assertEquals(config, DOM3_XML_RENDERED_GOLDEN, Nodes.render(fragment));
    }
  }

  public final void testOneRootXmlElement() {
    TokenQueue<HtmlTokenType> tq = tokenizeTestInput(
        "<foo/><bar/>", true, false);
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

  public final void testEmptyFragment() throws Exception {
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

  public final void testParseDirective() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<!DOCTYPE html",
            "PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN",
            "DTD/xhtml1-strict.dtd\">",
            ("<html xmlns=\"http://www.w3.org/1999/xhtml\""
             + " xml:lang=\"en\" lang=\"en\">"),
            "<head>",
            "</head>",
            "<body>",
            "</body>",
            "</html>"
        ),
        Arrays.asList(
            "Element : html 5+1-10+8",
            "  Attrib : lang 5+58-5+62",
            "    Value : en 5+63-5+67",
            "  Attrib : xml:lang 5+44-5+52",
            "    Value : en 5+53-5+57",
            "  Element : head 6+1-7+8",
            "    Text : \\n 6+7-7+1",
            "  Text : \\n 7+8-8+1",
            "  Element : body 8+1-9+8",
            "    Text : \\n\\n 8+7-10+1"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            ("<html xmlns=\"http://www.w3.org/1999/xhtml\""
             + " lang=\"en\" xml:lang=\"en\"><head>"),
            "</head>",
            "<body>",
            "",
            "</body></html>"
            )
        );
  }

  public final void testTextOnlyFragment() throws Exception {
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

  public final void testHtml1() throws Exception {
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
            "Element : html 1+1-12+25",  // </html> inside <table> ignored
            "  Element : head 1+7-5+8",
            "    Text : \\n\\n 1+13-3+1",
            "    Element : title 3+1-3+42",
            "      Text : Foo<a> & Bar & Baz</a> 3+8-3+34",
            "    Text : \\n\\n 3+42-5+1",
            "  Text : \\n 5+8-6+1",
            "  Element : body 6+1-12+25",
            "    Attrib : bgcolor 6+7-6+14",
            "      Value : white 6+15-6+20",
            // Include attributes folded in from other body tags
            "    Attrib : onerror 8+7-8+14",
            "      Value : panic() 8+15-8+22",
            "    Attrib : onload 7+9-7+15",
            "      Value : panic() 7+16-7+23",
            // Normalized text from in between body tags
            "    Text : \\n  \\n\\n\\n\\n 6+21-11+1",
            "    Element : table 11+1-12+25",  // Name is canonicalized
            "      Text : \\n 11+8-12+1",
            "      Element : tbody 12+1-12+17",
            "        Element : tr 12+1-12+17",
            "          Element : td 12+1-12+17",
            "            Text : Howdy 12+5-12+10"
            ),
        Arrays.asList(
            "LINT testHtml1:7+3 - 24: "
            + "'body' start tag found but the 'body' element is already open.",
            "LINT testHtml1:8+1 - 42: "
            + "'body' start tag found but the 'body' element is already open.",
            "WARNING testHtml1:8+23 - 29:"
            + " attribute onload duplicates one at testHtml1:7+9 - 15",
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
            "<body bgcolor=\"white\" onerror=\"panic()\" onload=\"panic()\">",
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

  public final void testHtml2() throws Exception {
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
            "Element : html 1+1-11+8",  // </html> inside <table> ignored
            "  Element : head 2+3-5+10",
            "    Text : \\n     2+9-3+5",
            "    Element : link 3+5-3+44",
            "      Attrib : href 3+26-3+30",
            "        Value : styles-1.css 3+31-3+43",
            "      Attrib : rel 3+11-3+14",
            "        Value : stylesheet 3+15-3+25",
            "    Text : \\n     3+44-4+5",
            "    Element : meta 4+5-4+46",
            "      Attrib : content 4+30-4+37",
            "        Value : utf-8 4+38-4+43",
            "      Attrib : http-equiv 4+11-4+21",
            "        Value : charset 4+22-4+29",
            "    Text : \\n   4+46-5+3",
            "    Element : script 6+3-6+33",
            "      Attrib : src 6+11-6+14",
            "        Value : foo.js 6+15-6+23",
            "  Text : \\n  \\n   5+10-7+3",
            "  Element : body 7+3-11+1",
            "    Text : \\n     7+9-8+5",
            "    Element : script 8+5-10+19",
            "      Attrib : type 8+13-8+17",
            "        Value : text/javascript 8+18-8+35",
            ("      Text : //<![CDATA[\\n      foo() && bar();\\n    //]]>"
             + " 8+36-10+10"),
            "    Text : \\n 10+19-11+1"
            ),
        Arrays.asList(
            "LINT testHtml2:6+3 - 24:"
            + " 'script' element between 'head' and 'body'."
            ),
        Arrays.asList(
            "<html><head>",
            "    <link href=\"styles-1.css\" rel=\"stylesheet\" />",
            "    <meta content=\"utf-8\" http-equiv=\"charset\" />",
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

  public final void testBeforeHead() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html><SCRIPT>foo()</scriPt></html>"),
        Arrays.asList(
            "Element : html 1+1-1+36",
            "  Element : head 1+7-1+29",
            "    Element : script 1+7-1+29",
            "      Text : foo() 1+15-1+20",
            "  Element : body 1+29-1+29"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<html><head><script>foo()</script></head><body></body></html>"
            )
        );
  }

  public final void testMinimalHtml() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html></html>"),
        Arrays.asList(
            "Element : html 1+1-1+14",
            "  Element : head 1+7-1+7",
            "  Element : body 1+7-1+7"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<html><head></head><body></body></html>"
            )
        );
    assertParsedHtml(
        Arrays.asList(
            "<HTML></HTML>"),
        Arrays.asList(
            "Element : html 1+1-1+14",
            "  Element : head 1+7-1+7",
            "  Element : body 1+7-1+7"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<html><head></head><body></body></html>"
            )
        );
  }

  public final void testMinimalFrameset() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html><frameset></frameset></html>"),
        Arrays.asList(
            "Element : html 1+1-1+35",
            "  Element : head 1+7-1+7",
            "  Element : frameset 1+7-1+28"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<html><head></head><frameset></frameset></html>"
            )
        );
  }

  public final void testSpuriousCloseTagBeforeHead() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html></script></html>"),
        Arrays.asList(
            "Element : html 1+1-1+23",
            "  Element : head 1+16-1+16",
            "  Element : body 1+16-1+16"
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

  public final void testHeadless() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html><body>Hello World</body></html>"),
        Arrays.asList(
            "Element : html 1+1-1+38",
            "  Element : head 1+7-1+7",
            "  Element : body 1+7-1+31",
            "    Text : Hello World 1+13-1+24"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<html><head></head><body>Hello World</body></html>"
            )
        );
  }

  public final void testLooseStyleTagEndsInHead() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html><style>",
            "body:before { content: 'Hello ' }",
            "body:after { content: \"World\" }",
            "</style></html>"),
        Arrays.asList(
            "Element : html 1+1-4+16",
            "  Element : head 1+7-4+9",
            "    Element : style 1+7-4+9",
            ("      Text : \\nbody:before { content: 'Hello ' }"
                        + "\\nbody:after { content: \"World\" }\\n 1+14-4+1"),
            "  Element : body 4+9-4+9"
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

  public final void testDoubleHeadedHtml() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html><head><head><body></body></html>"),
        Arrays.asList(
            "Element : html 1+1-1+39",
            "  Element : head 1+7-1+19",
            "  Element : body 1+19-1+32"
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

  public final void testBodyFragment() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<body><p>&#60;Bar&#x3E;<p>Baz</body>"),
        Arrays.asList(
            "Fragment 1+1-1+37",
            "  Element : p 1+7-1+24",
            "    Text : <Bar> 1+10-1+24",  // Text contains decoded value
            "  Element : p 1+24-1+37",
            "    Text : Baz 1+27-1+30"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<p>&lt;Bar&gt;</p><p>Baz</p>"
            )
        );
  }

  public final void testFramesetFragment() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<frameset><frame src=foo.html /></frameset>"),
        Arrays.asList(
            "Fragment 1+1-1+44",
            "  Element : frameset 1+1-1+44",
            "    Element : frame 1+11-1+33",
            "      Attrib : src 1+18-1+21",
            "        Value : foo.html 1+22-1+30"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<frameset><frame src=\"foo.html\"></frame></frameset>"
            )
        );
  }

  public final void testFramesetFragment2() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            ""
            + "<frameset cols=\"20%,80%\">\n"
            + "  <frame src=\"foo.html\" />\n"
            + "  <frame src=\"bar.html\" />\n"
            + "</frameset>"),
        Arrays.asList(
            "Fragment 1+1-4+12",
            "  Element : frameset 1+1-4+12",
            "    Attrib : cols 1+11-1+15",
            "      Value : 20%,80% 1+16-1+25",
            "    Text : \\n   1+26-2+3",
            "    Element : frame 2+3-2+27",
            "      Attrib : src 2+10-2+13",
            "        Value : foo.html 2+14-2+24",
            "    Text : \\n   2+27-3+3",
            "    Element : frame 3+3-3+27",
            "      Attrib : src 3+10-3+13",
            "        Value : bar.html 3+14-3+24",
            "    Text : \\n 3+27-4+1"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<frameset cols=\"20%,80%\">",
            "  <frame src=\"foo.html\"></frame>",
            "  <frame src=\"bar.html\"></frame>",
            "</frameset>"
            )
        );
  }

  public final void testFragmentThatEndsWithAComment() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<p>Hello</p>  <!-- Zoicks -->   "),
        Arrays.asList(
            "Fragment 1+1-1+33",
            "  Element : p 1+1-1+13",
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

  public final void testFragmentThatEndsWithACommentRetained()
      throws Exception {
    assertParsedHtmlFragmentWithComments(
        Arrays.asList(
            "<p>Hello</p>  <!-- Zoicks -->   "),
        Arrays.asList(
            "Fragment 1+1-1+33",
            "  Element : p 1+1-1+13",
            "    Text : Hello 1+4-1+9",
            "  Text :    1+13-1+15",
            "  Comment :  Zoicks  1+15-1+30",
            "  Text :     1+30-1+33"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            // Parses as comment, but comment is suppressed by Nodes.render()
            "<p>Hello</p>     "
            )
        );
  }

  public final void testFragmentWithTopLevelHtmlNodeRetained()
      throws Exception {
    assertParsedHtmlFragmentWithComments(
        Arrays.asList(
            "<html>",
            "<head><script>foo</script></head>",
            "<!-- Above body -->",
            "<body>",
            "  <!-- In body -->",
            "</body>",
            "</html>"),
        Arrays.asList(
            "Fragment 1+1-7+8",
            "  Element : html 1+1-7+8",
            "    Element : head 2+1-2+34",
            "      Element : script 2+7-2+27",
            "        Text : foo 2+15-2+18",
            "    Text : \\n 2+34-3+1",
            "    Comment :  Above body  3+1-3+20",
            "    Text : \\n 3+20-4+1",
            "    Element : body 4+1-6+8",
            "      Text : \\n   4+7-5+3",
            "      Comment :  In body  5+3-5+19",
            "      Text : \\n\\n 5+19-7+1"),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            // Again comment is parsed but suppressed for now at output.
            "<html><head><script>foo</script></head>",
            "",
            "<body>",
            "  ",
            "",
            "</body></html>")
        );
  }

  public final void testTableFragment() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<table>",
            "<tr><th>Hi<td>There",
            "</table>"),
        Arrays.asList(
            "Fragment 1+1-3+9",
            "  Element : table 1+1-3+9",
            "    Text : \\n 1+8-2+1",
            "    Element : tbody 2+1-3+1",
            "      Element : tr 2+1-3+1",
            "        Element : th 2+5-2+11",
            "          Text : Hi 2+9-2+11",
            "        Element : td 2+11-3+1",
            "          Text : There\\n 2+15-3+1"
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

  public final void testFragmentWithClose() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "</head>",  // Nothing here but text
            "<body>Foo"),
        Arrays.asList(
            "Fragment 1+1-2+10",
            "  Text : \\nFoo 1+8-2+10"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "",
            "Foo"
            )
        );
  }

  public final void testMisplacedTitle() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html><body><title>What head?</title></body></html>"),
        Arrays.asList(
            "Element : html 1+1-1+52",
            "  Element : head 1+7-1+7",
            "  Element : body 1+7-1+45",
            "    Element : title 1+13-1+38",
            "      Text : What head? 1+20-1+30"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<html><head></head><body><title>What head?</title></body></html>"
            )
        );
  }

  public final void testBodyWithAttributeInFragment() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(  // A fragment with body attributes is a whole document
            "<div>",
            "<p>Foo<",
            "<body bgcolor=white>",
            "<p>Bar</body>"),
        Arrays.asList(
            "Fragment 1+1-4+14",
            "  Element : html 1+1-4+14",
            "    Element : head 1+1-1+1",
            "    Element : body 1+1-4+14",
            "      Attrib : bgcolor 3+7-3+14",
            "        Value : white 3+15-3+20",
            "      Element : div 1+1-4+14",
            "        Text : \\n 1+6-2+1",
            "        Element : p 2+1-4+1",
            "          Text : Foo<\\n\\n 2+4-4+1",
            "        Element : p 4+1-4+14",
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

  public final void testListFragment() throws Exception {
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
            "  Element : div 1+1-4+17",
            "    Element : br 1+6-1+11",
            "    Text : \\n 1+11-2+1",
            "    Element : p 2+1-4+1",
            "      Text : Foo 2+4-2+7",
            "      Element : br 2+7-2+11",
            "      Text : \\n\\n 2+11-4+1",
            "    Element : ul 4+1-4+17",
            "      Element : li 4+5-4+12",
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

  public final void testFormsNotNested() throws Exception {
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
            "  Element : form 1+1-6+8",
            "    Attrib : action 1+7-1+13",
            "      Value : fly 1+15-1+20",
            "    Text : \\n 1+21-2+1",
            "    Element : input 2+1-2+45",
            "      Attrib : name 2+19-2+23",
            "        Value : method 2+24-2+30",
            "      Attrib : type 2+8-2+12",
            "        Value : radio 2+13-2+18",
            "      Attrib : value 2+31-2+36",
            "        Value : rockets 2+37-2+44",
            "    Text : \\n\\n 2+45-4+1",
            "    Element : input 4+1-4+43",
            "      Attrib : name 4+19-4+23",
            "        Value : method 4+24-4+30",
            "      Attrib : type 4+8-4+12",
            "        Value : radio 4+13-4+18",
            "      Attrib : value 4+31-4+36",
            "        Value : wings 4+37-4+42",
            "    Text : \\n 4+43-5+1",
            "    Element : input 5+1-5+38",
            "      Attrib : type 5+8-5+12",
            "        Value : submit 5+13-5+19",
            "      Attrib : value 5+20-5+25",
            "        Value : Take off 5+27-5+37",
            "    Text : \\n 5+38-6+1"
            ),
        Arrays.asList(
            "LINT testFormsNotNested:3+1 - 19: Saw a 'form' start tag"
            + ", but there was already an active 'form' element."
            + " Nested forms are not allowed. Ignoring the tag.",
            "LINT testFormsNotNested:6+8 - 15: Stray end tag 'form'."
            ),
        Arrays.asList(
            "<form action=\"fly\">",
            "<input name=\"method\" type=\"radio\" value=\"rockets\" />",
            "",
            "<input name=\"method\" type=\"radio\" value=\"wings\" />",
            "<input type=\"submit\" value=\"Take off\" />",
            "</form>"
            )
        );
  }

  public final void testListNesting() throws Exception {
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
            "  Element : ul 1+1-8+6",
            "    Attrib : id 1+5-1+7",
            "      Value : unordered-list 1+8-1+22",
            "    Text : \\n 1+23-2+1",
            "    Element : li 2+1-2+21",
            "      Text : From Chaos, 2+5-2+16",
            "    Text : \\n 2+26-3+1",
            "    Element : li 3+1-4+1",
            "      Text : and Disorder; arise\\n 3+5-4+1",
            "    Element : li 4+1-7+34",
            "      Element : ol 4+5-7+29",
            "        Attrib : id 4+9-4+11",
            "          Value : ordered-list 4+12-4+24",
            "        Text : \\n   4+25-5+3",
            "        Element : li 5+3-6+3",
            "          Element : div 5+7-6+3",
            "            Text : Harmony,\\n   5+12-6+3",
            "        Element : li 6+3-6+20",
            "          Text : Balance, 6+7-6+15",
            "        Text : \\n   6+20-7+3",
            "        Element : li 7+3-7+29",
            "          Text : and Punctuation!? 7+7-7+24",
            "    Text : \\n 7+34-8+1"
            ),
        Arrays.asList(
            // TODO(mikesamuel): this error message seems to be a bug.
            // There is an error there, but the close tag is spurious.
            "LINT testListNesting:2+21 - 26:"
            + " No 'li' element in scope but a 'li' end tag seen.",
            "LINT testListNesting:6+3 - 7: Unclosed elements inside a list.",
            // Ditto wrong message
            "LINT testListNesting:7+29 - 34:"
            + " End tag for 'li' seen, but there were unclosed elements."
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

  public final void testParagraphInterrupters() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<body><p>Hi <pre>There</pre>",
            "<p>Uh <dl><dt>Zero<dd>None<ol><li>Nested<dt><div><p>One<dd>1</dl>",
            "<p>Li</dl>ne<hr>s",
            "<p>Well <plaintext>Runs till the end of the <p>Document",
            "No matter <p>What"
            ),
        Arrays.asList(
            "Element : html 1+1-5+18",
            "  Element : head 1+1-1+1",
            "  Element : body 1+1-5+18",
            "    Element : p 1+7-1+13",
            "      Text : Hi  1+10-1+13",
            "    Element : pre 1+13-1+29",
            "      Text : There 1+18-1+23",
            "    Text : \\n 1+29-2+1",
            "    Element : p 2+1-2+7",
            "      Text : Uh  2+4-2+7",
            "    Element : dl 2+7-2+66",
            "      Element : dt 2+11-2+19",
            "        Text : Zero 2+15-2+19",
            "      Element : dd 2+19-2+61",
            "        Text : None 2+23-2+27",
            "        Element : ol 2+27-2+61",
            "          Element : li 2+31-2+61",
            "            Text : Nested 2+35-2+41",
            // DT don't jump out of an OL or UL list
            "            Element : dt 2+41-2+56",
            "              Element : div 2+45-2+56",
            "                Element : p 2+50-2+56",
            "                  Text : One 2+53-2+56",
            "            Element : dd 2+56-2+61",
            "              Text : 1 2+60-2+61",
            "    Text : \\n 2+66-3+1",
            "    Element : p 3+1-3+13",
            // Not interrupted by </dl>.  End tags do not interrupt <p>s
            "      Text : Line 3+4-3+13",
            "    Element : hr 3+13-3+17",
            "    Text : s\\n 3+17-4+1",
            "    Element : p 4+1-4+9",
            "      Text : Well  4+4-4+9",
            "    Element : plaintext 4+9-5+18",
            ("      Text : Runs till the end of the <p>Document"
             + "\\nNo matter <p>What"
             + " 4+20-5+18")
            ),
        Arrays.asList(
            "LINT testParagraphInterrupters:2+56 - 60:"
            + " Unclosed elements inside a list.",
            "LINT testParagraphInterrupters:2+61 - 66:"
            + " End tag 'dl' seen but there were unclosed elements.",
            "LINT testParagraphInterrupters:3+6 - 11:"
            + " Stray end tag 'dl'.",
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

  public final void testParagraphNotNested() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html>",
            "<p>Foo</p>",
            "<p>Bar",
            "<p>Baz</p></p>",  // Second close p opens a paragraph because...
            "</html>"
            ),
        Arrays.asList(
            "Element : html 1+1-5+8",
            "  Element : head 2+1-2+1",
            "  Element : body 2+1-5+1",
            "    Element : p 2+1-2+11",
            "      Text : Foo 2+4-2+7",
            "    Text : \\n 2+11-3+1",
            "    Element : p 3+1-4+1",
            "      Text : Bar\\n 3+4-4+1",
            "    Element : p 4+1-4+11",
            "      Text : Baz 4+4-4+7",
            "    Element : p 4+11-4+15",
            "    Text : \\n 4+15-5+1"
            ),
        Arrays.asList(
            "LINT testParagraphNotNested:4+11 - 15:"
            + " No 'p' element in scope but a 'p' end tag seen."
            ),
        Arrays.asList(
            "<html><head></head><body><p>Foo</p>",
            "<p>Bar",
            "</p><p>Baz</p><p></p>",
            "</body></html>"
            )
        );
  }

  public final void testAnyHeadingTagCloses() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html>",
            "<p>Foo",
            "<h1><p>Bar</H3>",
            "",
            "<h2>Baz",  // Closes at open <h3>
            "",
            "<<h3>Boo</h2>",  // Close tag closes h3, not enclosing h2
            "",
            "<p>Far></h3></h1>"
            ),
        Arrays.asList(
            "Element : html 1+1-9+18",
            "  Element : head 2+1-2+1",
            "  Element : body 2+1-9+18",
            "    Element : p 2+1-3+1",
            "      Text : Foo\\n 2+4-3+1",
            "    Element : h1 3+1-3+16",
            "      Element : p 3+5-3+11",
            "        Text : Bar 3+8-3+11",
            "    Text : \\n\\n 3+16-5+1",
            "    Element : h2 5+1-7+2",
            "      Text : Baz\\n\\n< 5+5-7+2",
            "    Element : h3 7+2-7+14",
            "      Text : Boo 7+6-7+9",
            "    Text : \\n\\n 7+14-9+1",
            "    Element : p 9+1-9+18",
            "      Text : Far> 9+4-9+8"
            ),
        Arrays.asList(
            "LINT testAnyHeadingTagCloses:3+11 - 16:"
            + " End tag 'h3' seen but there were unclosed elements.",
            "LINT testAnyHeadingTagCloses:7+2 - 6:"
            + " Heading cannot be a child of another heading.",
            "LINT testAnyHeadingTagCloses:7+9 - 14:"
            + " End tag 'h2' seen but there were unclosed elements.",
            "LINT testAnyHeadingTagCloses:9+8 - 13:"
            + " Stray end tag 'h3'.",
            "LINT testAnyHeadingTagCloses:9+13 - 18:"
            + " Stray end tag 'h1'."
            ),
        Arrays.asList(
            "<html><head></head><body><p>Foo",
            "</p><h1><p>Bar</p></h1>",
            "",
            "<h2>Baz",
            "",
            "&lt;</h2><h3>Boo</h3>",
            "",
            "<p>Far&gt;</p></body></html>"
            )
        );
  }

  public final void testLinksDontNest() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<div>",
            "<a href=foo>Foo",
            // Links don't nest
            "<a href=bar>Bar",
            // unless they do.  Table is a scoping element.
            "<table><caption><a href=http://baz/>Baz</table>boo",
            "</a>"
            ),
        Arrays.asList(
            "Fragment 1+1-5+5",
            "  Element : div 1+1-5+5",
            "    Text : \\n 1+6-2+1",
            "    Element : a 2+1-3+1",
            "      Attrib : href 2+4-2+8",
            "        Value : foo 2+9-2+12",
            "      Text : Foo\\n 2+13-3+1",
            "    Element : a 3+1-5+5",
            "      Attrib : href 3+4-3+8",
            "        Value : bar 3+9-3+12",
            "      Text : Bar\\n 3+13-4+1",
            "      Element : table 4+1-4+48",
            "        Element : caption 4+8-4+40",
            "          Element : a 4+17-4+40",
            "            Attrib : href 4+20-4+24",
            "              Value : http://baz/ 4+25-4+36",
            "            Text : Baz 4+37-4+40",
            "      Text : boo\\n 4+48-5+1"
            ),
        Arrays.<String>asList(
            "LINT testLinksDontNest:3+1 - 13:"
            + " An 'a' start tag seen with already an active 'a' element.",
            "LINT testLinksDontNest:4+40 - 48:"
            + " 'table' closed but 'caption' was still open.",
            "LINT testLinksDontNest:4+40 - 48:"
            + " Unclosed elements on stack.",
            "LINT testLinksDontNest:5+5:"
            + " End of file seen and there were open elements."
            ),
        Arrays.asList(
            "<div>",
            "<a href=\"foo\">Foo",
            "</a><a href=\"bar\">Bar",
            (""
             + "<table><caption><a href=\"http://baz/\">Baz</a></caption>"
             + "</table>boo"),
            "</a></div>"
            )
        );
  }

  public final void testEntities() throws Exception {
    // Test how we deal with missing semicolons.
    assertParsedHtmlFragment(
        Arrays.asList(
            "Hello, &lt;World&gt!<b title='Foo &iexcl'>",
            "&amp &AMP &Amp &bogus &#123 &#123; Foo &#x7D &#X7d</b>",
            "<script>if (x&lt&gt) { alert('hello'); }</script>"
            ),
        Arrays.asList(
            "Fragment 1+1-3+50",
            "  Text : Hello, <World>! 1+1-1+21",  // entity in text fixed
            "  Element : b 1+21-2+55",
            "    Attrib : title 1+24-1+29",
            "      Value : Foo \u00A1 1+30-1+42", // entity values fixed
            // mixed case names not fixed.  upper case names fixed.
            // numeric entities fixed.  bogus entity names not affected.
            "    Text : \\n& & &Amp &bogus { { Foo } } 1+43-2+51",
            "  Text : \\n 2+55-3+1",
            "  Element : script 3+1-3+50",
            // &lt &gt are not rewritten because they are in CDATA.
            "    Text : if (x&lt&gt) { alert('hello'); } 3+9-3+41"
            ),
        Arrays.asList(
            "WARNING testEntities:1+1 - 21:"
            + " HTML entity missing closing semicolon &gt",
            "WARNING testEntities:1+30 - 42:"
            + " HTML entity missing closing semicolon &iexcl",
            "WARNING testEntities:1+43 - 2+51:"
            + " HTML entity missing closing semicolon &amp",
            "WARNING testEntities:1+43 - 2+51:"
            + " HTML entity missing closing semicolon &AMP",
            "WARNING testEntities:1+43 - 2+51:"
            + " HTML entity missing closing semicolon &#123",
            "WARNING testEntities:1+43 - 2+51:"
            + " HTML entity missing closing semicolon &#x7D",
            "WARNING testEntities:1+43 - 2+51:"
            + " HTML entity missing closing semicolon &#X7d"
            ),
        Arrays.asList(
            "Hello, &lt;World&gt;!<b title=\"Foo &#161;\">",
            "&amp; &amp; &amp;Amp &amp;bogus { { Foo } }</b>",
            "<script>if (x&lt&gt) { alert('hello'); }</script>"
            )
        );
  }

  public final void testFormattingElements() throws Exception {
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
            "Element : html 1+1-7+34",
            "  Element : head 1+1-1+1",
            "  Element : body 1+1-7+34",
            "    Text : \\n 1+7-2+1",
            "    Element : a 2+1-2+29",
            "      Attrib : href 2+4-2+8",
            "        Value : foo 2+9-2+12",
            "      Text : Foo 2+13-2+16",
            "      Element : nobr 2+16-2+25",
            "        Text : Bar 2+22-2+25",
            "    Element : nobr 2+16-7+34",
            "      Text : \\n\\n 2+29-4+1",
            "      Element : b 4+1-4+34",
            "        Element : font 4+4-4+30",
            "          Attrib : color 4+10-4+15",
            "            Value : BLUE 4+16-4+20",
            "          Text : bold&blue 4+21-4+30",
            "      Element : font 4+4-5+18",
            "        Attrib : color 4+10-4+15",
            "          Value : BLUE 4+16-4+20",
            "        Text : \\nstill blue 4+34-5+11",
            "      Text : \\nboring\\n 5+18-7+1",
            "      Element : span 7+1-7+20",
            "        Element : b 7+7-7+13",
            "          Text : Foo 7+10-7+13",
            "      Element : b 7+7-7+27",
            "        Text : Bar 7+20-7+23"
            ),
        Arrays.asList(
            "LINT testFormattingElements:2+25 - 29:"
            + " End tag 'a' violates nesting rules.",
            "LINT testFormattingElements:4+30 - 34:"
            + " End tag 'b' violates nesting rules.",
            "LINT testFormattingElements:7+13 - 20:"
            + " End tag 'span' seen but there were unclosed elements.",
            "LINT testFormattingElements:7+27 - 34:"
            + " Stray end tag 'span'.",
            "LINT testFormattingElements:7+34:"
            + " End of file seen and there were open elements."
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

  public final void testObjectElements() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<object>",
            "  <param name=\"foo\" value=\"bar\">",
            "  <param name=\"baz\" value=\"boo\">",
            "</object>"
            ),
        Arrays.asList(
            "Fragment 1+1-4+10",
            "  Element : object 1+1-4+10",
            "    Text : \\n   1+9-2+3",
            "    Element : param 2+3-2+33",
            "      Attrib : name 2+10-2+14",
            "        Value : foo 2+15-2+20",
            "      Attrib : value 2+21-2+26",
            "        Value : bar 2+27-2+32",
            "    Text : \\n   2+33-3+3",
            "    Element : param 3+3-3+33",
            "      Attrib : name 3+10-3+14",
            "        Value : baz 3+15-3+20",
            "      Attrib : value 3+21-3+26",
            "        Value : boo 3+27-3+32",
            "    Text : \\n 3+33-4+1"
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

  public final void testButtonsDontNest() throws Exception {
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
            "  Element : div 1+1-5+28",
            "    Text : \\n 1+6-2+1",
            "    Element : button 2+1-3+1",
            "      Text : Foo\\n 2+9-3+1",
            "    Element : button 3+1-5+10",
            "      Text : Bar\\n 3+9-4+1",
            "      Element : table 4+1-4+31",
            "        Element : tbody 4+8-4+23",
            "          Element : tr 4+8-4+23",
            "            Element : td 4+8-4+23",
            "              Element : button 4+12-4+23",
            "                Text : Baz 4+20-4+23",
            "      Text : \\n 4+31-5+1"
            ),
        Arrays.asList(
            "LINT testButtonsDontNest:3+1 - 9:"
            + " 'button' start tag seen when there was an open 'button'"
            + " element in scope.",
            "LINT testButtonsDontNest:4+8 - 12:"
            + " 'td' start tag in table body.",
            "LINT testButtonsDontNest:4+23 - 31: Unclosed elements.",
            "LINT testButtonsDontNest:5+10 - 19:"
            + " Stray end tag 'button'.",
            "LINT testButtonsDontNest:5+19 - 28:"
            + " Stray end tag 'button'.",
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

  public final void testImageTag() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<image src='foo.gif?a=b&c=d'>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+30",
            "  Element : img 1+1-1+30",
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

  public final void testIsIndex() throws Exception {
    // Its semantics are really weird and it's deprecated, so drop it.
    assertParsedHtml(
        Arrays.asList(
            "<div><isindex prompt='Blah blah'></div>"
            ),
        Arrays.asList(  // WTF!?!
            "Element : html 1+1-1+40",
            "  Element : head 1+1-1+1",
            "  Element : body 1+1-1+40",
            "    Element : div 1+1-1+40",
            "      Element : form 1+6-1+6",
            "        Element : hr 1+6-1+6",
            "        Element : p 1+6-1+6",
            "          Element : label 1+6-1+6",
            "            Text : Blah blah 1+6-1+14", // Position of the open tag
            "            Element : input 1+6-1+6",
            "              Attrib : name unknown",
            "                Value : isindex unknown",
            "        Element : hr 1+6-1+6"
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

  public final void testDisablersAreCdata() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<noframes><iframe src=foo></noframes></noscript>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+49",
            "  Element : noframes 1+1-1+38",
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

  public final void testInputElements() throws Exception {
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
            "  <option>Three</option>",
            "</select>",
            "</select>",
            "<optgroup><option>Four</option></optgroup>",
            "</form>"
            ),
        Arrays.asList(
            "Fragment 1+1-15+8",
            "  Element : form 1+1-15+8",
            "    Text : \\n 1+7-2+1",
            "    Element : textarea 2+1-5+12",
            "      Text : "
                + "  <form action='a?b=c&d=e&f=g'>\\n </form>\\n 2+11-5+1",
            "    Text : \\n\\n 5+12-7+1",
            "    Element : select 7+1-10+9",
            "      Text : \\n   7+9-8+3",
            "      Element : option 8+3-9+3",
            "        Text : One\\n   8+11-9+3",
            "      Element : option 9+3-9+23",
            "        Text : Two 9+11-9+14",
            "      Text : \\n 9+23-10+1",
            "    Text : \\n   10+9-11+3",
            "    Element : option 11+3-11+25",
            "      Text : Three 11+11-11+16",
            "    Text : \\n\\n\\n 11+25-14+1",
            "    Element : optgroup 14+1-14+43",
            "      Element : option 14+11-14+32",
            "        Text : Four 14+19-14+23",
            "    Text : \\n 14+43-15+1"
            ),
        Arrays.<String>asList(
            "LINT testInputElements:10+1 - 9:"
            + " 'select' start tag where end tag expected.",
            "LINT testInputElements:12+1 - 10: Stray end tag 'select'.",
            "LINT testInputElements:13+1 - 10: Stray end tag 'select'."
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
            "  <option>Three</option>",
            "",
            "",
            "<optgroup><option>Four</option></optgroup>",
            "</form>"
            )
        );
  }

  public final void testUnknownTagsNest() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html><foo><bar>baz</bar></baz>boo<command></html>"
            ),
        Arrays.asList(
            "Element : html 1+1-1+51",
            "  Element : head 1+7-1+7",
            "  Element : body 1+7-1+51",
            "    Element : foo 1+7-1+51",
            "      Element : bar 1+12-1+26",
            "        Text : baz 1+17-1+20",
            "      Text : boo 1+32-1+35",
            "      Element : command 1+35-1+44"
            ),
        Arrays.asList(
            "LINT testUnknownTagsNest:1+26 - 32: Stray end tag 'baz'.",
            "LINT testUnknownTagsNest:1+44 - 51:"
            + " End tag for 'html' seen but there were unclosed elements."
            ),
        Arrays.asList(
            ("<html><head></head><body>"
             + "<foo><bar>baz</bar>boo<command></command></foo></body></html>")
            )
        );
  }

  public final void testRegularTags() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<span><span>Foo</span>Bar</span></span>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+40",
            "  Element : span 1+1-1+33",
            "    Element : span 1+7-1+23",
            "      Text : Foo 1+13-1+16",
            "    Text : Bar 1+23-1+26"
            ),
        Arrays.<String>asList(
            "LINT testRegularTags:1+33 - 40: Stray end tag 'span'."
            ),
        Arrays.asList(
            "<span><span>Foo</span>Bar</span>"
            )
        );
  }

  public final void testXmp() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<p><b>Foo</b><xmp><b>Foo</b></xmp><b>Foo</b></p>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+49",
            "  Element : p 1+1-1+49",
            "    Element : b 1+4-1+14",
            "      Text : Foo 1+7-1+10",
            "    Element : xmp 1+14-1+35",
            "      Text : <b>Foo</b> 1+19-1+29",
            "    Element : b 1+35-1+45",
            "      Text : Foo 1+38-1+41"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<p><b>Foo</b><xmp><b>Foo</b></xmp><b>Foo</b></p>"
            )
        );
  }

  public final void testColumnGroups() throws Exception {
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
            "  Element : table 1+1-8+9",
            "    Element : caption 1+8-1+31",
            "      Text : Test 1+17-1+21",
            "    Text : \\n 1+31-2+1",
            "    Element : colgroup 2+1-6+12",
            "      Text : \\n   2+11-3+3",
            "      Element : col 3+3-3+18",
            "        Attrib : class 3+8-3+13",
            "          Value : red 3+14-3+17",
            "      Text : \\n   3+18-4+3",
            "      Element : col 4+3-4+20",
            "        Attrib : class 4+8-4+13",
            "          Value : green 4+14-4+19",
            "      Text : \\n   4+20-5+3",
            "      Element : col 5+3-5+19",
            "        Attrib : class 5+8-5+13",
            "          Value : blue 5+14-5+18",
            "      Text : \\n 5+19-6+1",
            "    Text : \\n 6+12-7+1",
            "    Element : tbody 7+1-8+1",
            "      Element : tr 7+1-8+1",
            "        Element : th 7+5-7+12",
            "          Text : red 7+9-7+12",
            "        Element : th 7+12-7+21",
            "          Text : green 7+16-7+21",
            "        Element : th 7+21-8+1",
            "          Text : blue\\n 7+25-8+1"
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

  public final void testMalformedTables() throws Exception {
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
            "  Element : div 1+1-9+20",
            "    Element : table 1+6-8+3",
            "      Text : \\n   1+13-2+3",
            "      Element : colgroup 2+3-5+3",
            "        Element : col 2+3-2+18",
            "          Attrib : class 2+8-2+13",
            "            Value : red 2+14-2+17",
            "        Text : \\n   2+18-3+3",
            "        Element : col 3+3-3+20",
            "          Attrib : class 3+8-3+13",
            "            Value : green 3+14-3+19",
            "        Text : \\n   3+20-4+3",
            "        Element : col 4+3-4+19",
            "          Attrib : class 4+8-4+13",
            "            Value : blue 4+14-4+18",
            "        Text : \\n   4+19-5+3",
            "      Element : tfoot 5+3-7+19",
            "        Text : \\n     5+10-6+5",
            "        Element : tr 6+5-7+11",
            "          Element : th 6+9-6+16",
            "            Text : red 6+13-6+16",
            "          Element : th 6+16-6+25",
            "            Text : green 6+20-6+25",
            "          Element : th 6+25-7+11",
            "            Text : blue\\n   6+29-7+3",
            "      Text : \\n   7+19-8+3",
            "    Element : table 8+3-9+20",
            "      Text : \\n 8+10-9+1"
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

  public final void testMoreTables() throws Exception {
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
            "  Element : div 1+1-6+9",
            "    Text : \\n 1+6-2+1",
            "    Element : p 2+1-6+9",
            "      Element : p 3+1-3+11",
            "        Text : Foo 3+4-3+7",
            "      Element : table 2+4-6+9",
            "        Text : \\n\\n 2+11-4+1",
            "        Element : tbody 4+1-5+1",
            "          Element : tr 4+1-5+1",
            "            Text : \\n 4+5-5+1",
            "        Element : caption 5+1-5+10",
            "        Element : tbody 5+10-6+1",
            "          Element : tr 5+10-6+1",
            "            Text : \\n 5+14-6+1"
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
            "<p><p>Foo</p><table>",
            "",
            "<tbody><tr>",
            "</tr></tbody><caption></caption><tbody><tr>",
            "</tr></tbody></table></p></div>"
            )
        );
  }

  public final void testEvenMoreTables() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<table><colgroup><col></col><caption></colgroup></caption>",
            "</thead></table>"
            ),
        Arrays.asList(
            "Fragment 1+1-2+17",
            "  Element : table 1+1-2+17",
            "    Element : colgroup 1+8-1+29",
            "      Element : col 1+18-1+23",
            "    Element : caption 1+29-1+59",
            "    Text : \\n 1+59-2+1"
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

  public final void testTablesBonus() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<div><table><tbody></body></br><tr></td></tr></table></div>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+60",
            "  Element : div 1+1-1+60",
            "    Element : br 1+27-1+32",
            "    Element : table 1+6-1+54",
            "      Element : tbody 1+13-1+46",
            "        Element : tr 1+32-1+46"
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

  public final void testTableRows() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(  // table does not end because none of these close the th
            "<div><table><tr><hr><td></th></html><select></td></table></div>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+64",
            "  Element : div 1+1-1+64",
            "    Element : hr 1+17-1+21",
            "    Element : table 1+6-1+58",
            "      Element : tbody 1+13-1+50",
            "        Element : tr 1+13-1+50",
            "          Element : td 1+21-1+50",
            "            Element : select 1+37-1+45"
            ),
        Arrays.<String>asList(
            "LINT testTableRows:1+17 - 21: Start tag 'hr' seen in 'table'.",
            "LINT testTableRows:1+25 - 30: Stray end tag 'th'.",
            "LINT testTableRows:1+30 - 37: Stray end tag 'html'.",
            "LINT testTableRows:1+45 - 50: 'td' end tag with 'select' open."
            ),
        Arrays.asList(
            ("<div><hr /><table><tbody><tr><td><select></select>"
             + "</td></tr></tbody></table></div>")
            )
        );
  }

  public final void testSelects() throws Exception {
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
            "  Element : select 1+1-5+22",
            "    Text : \\n 1+9-2+1",
            "    Element : optgroup 2+12-3+1",
            "      Text : \\n 2+22-3+1",
            "    Element : optgroup 3+1-4+21",
            "      Text : \\n 3+11-4+1",
            "      Element : option 4+1-4+10",
            "        Text : 1 4+9-4+10",
            "    Text : \\n 4+21-5+1"
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

  public final void testTrailingEndPhase() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "<html></html><br>"
            ),
        Arrays.asList(
            "Element : html 1+1-1+18",
            "  Element : head 1+7-1+7",
            "  Element : body 1+7-1+18",
            "    Element : br 1+14-1+18"
            ),
        Arrays.asList(
            "LINT testTrailingEndPhase:1+14 - 18: Stray 'br' start tag."
            ),
        Arrays.asList(
            "<html><head></head><body><br /></body></html>"
            )
        );
  }

  public final void testValuelessAttributes() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<input type=checkbox checked>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+30",
            "  Element : input 1+1-1+30",
            "    Attrib : checked 1+22-1+29",
            "      Value : checked 1+22-1+29",
            "    Attrib : type 1+8-1+12",
            "      Value : checkbox 1+13-1+21"
            ),
        Arrays.<String>asList(
            ),
        Arrays.asList(
            "<input checked=\"checked\" type=\"checkbox\" />"
            )
        );
  }

  public final void testNoDoctypeGuessAsHtml() throws Exception {
    assertParsedMarkup(
        Arrays.asList(
            "<xmp><br/></xmp>"
            ),
        Arrays.asList(
            "Element : html 1+1-1+17",
            "  Element : head 1+1-1+1",
            "  Element : body 1+1-1+17",
            "    Element : xmp 1+1-1+17",
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
            "Element : html 1+1-1+40",
            "  Element : head 1+1-1+40",
            "    Element : script 1+1-1+40",
            "      Text : document.write('</b'); 1+9-1+31",
            "  Element : body 1+40-1+40"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<html><head><script>document.write('</b');</script></head>"
            + "<body></body></html>"
            ),
        null, false);
  }

  public final void testDoctypeGuessAsHtml() throws Exception {
    assertParsedMarkup(
        Arrays.asList(
            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">",
            "<xmp><br/></xmp>"
            ),
        Arrays.asList(
            "Fragment 1+56-2+17",
            "  Element : xmp 2+1-2+17",
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

  public final void testFindDoctypeIgnoresLeadingWhitespace() throws Exception {
    String[] htmlInput = {
        " \t\r\n <!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 1.0 Transitional//EN\"",
        "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">",
        "<xmp><br/></xmp>" };
    TokenQueue<HtmlTokenType> tq = tokenizeTestInput(
        Join.join("\n", htmlInput), false, true);
    DomParser parser = new DomParser(tq, false, mq);

    Document doc = parser.parseDocument().getOwnerDocument();
    assertEquals("-//W3C//DTD HTML 1.0 Transitional//EN",
                 doc.getDoctype().getPublicId());
    assertEquals("http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd",
                 doc.getDoctype().getSystemId());
    assertEquals("html",
                 doc.getDoctype().getName());

    assertParsedMarkup(Arrays.asList(htmlInput),
        Arrays.asList(
            "Element : html 4+1-4+17",
            "  Element : head 4+1-4+1",
            "  Element : body 4+1-4+17",
            "    Element : xmp 4+1-4+17",
            "      Text : <br/> 4+6-4+11"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<html><head></head><body><xmp><br/></xmp></body></html>"
            ),
        false, false);
  }

  public final void testDoctypeGuessAsXhtml() throws Exception {
    assertParsedMarkup(
        Arrays.asList(
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"",
            "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">",
            "<xmp><br/></xmp>"
            ),
        Arrays.asList(
            "Element : xmp 3+1-3+17",
            "  Element : br 3+6-3+11"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<xmp><br /></xmp>"
            ),
        null, false);
  }

  public final void testDoctypeGuessAsSVG() throws Exception {
    assertParsedMarkup(
        Arrays.asList(
            "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"",
            " \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">",
            "<svg><rect width='100' height='100' x='50' y='50'",
            " style='color:red'/></svg>"
            ),
        Arrays.asList(
            "Element : svg 3+1-4+27",
            "  Element : rect 3+6-4+21",
            "    Attrib : height 3+24-3+30",
            "      Value : 100 3+31-3+36",
            "    Attrib : style 4+2-4+7",
            "      Value : color:red 4+8-4+19",
            "    Attrib : width 3+12-3+17",
            "      Value : 100 3+18-3+23",
            "    Attrib : x 3+37-3+38",
            "      Value : 50 3+39-3+43",
            "    Attrib : y 3+44-3+45",
            "      Value : 50 3+46-3+50"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<svg:svg><svg:rect height=\"100\" style=\"color:red\""
            + " width=\"100\" x=\"50\" y=\"50\" /></svg:svg>"
            ),
        null, false);
  }

  public final void testXmlPrologueTreatedAsXml() throws Exception {
    assertParsedMarkup(
        Arrays.asList(
            "<?xml version=\"1.0\"?>",
            "<xmp><br/></xmp>"
            ),
        Arrays.asList(
            "Element : xmp 2+1-2+17",
            "  Element : br 2+6-2+11"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<xmp><br /></xmp>"
            ),
        null, false);
  }

  public final void testFileExtensionsBasedContentTypeGuessing()
      throws ParseException {
    // Override input sources, so that DomParser has a file extension available
    // when deciding whether to treat the input as HTML or XML.
    this.is = new InputSource(URI.create("test:///" + getName() + ".html"));
    assertParsedMarkup(
        Arrays.asList(
            "<xmp><br/></xmp>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+17",
            "  Element : xmp 1+1-1+17",
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
            "  Element : xmp 1+1-1+17",
            "    Element : br 1+6-1+11"
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
            "  Element : xmp 1+1-1+17",
            "    Element : br 1+6-1+11"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<xmp><br /></xmp>"
            ),
        null, true);
  }

  public final void testQualifiedNameTreatedAsXml() throws Exception {
    assertParsedMarkup(
        Arrays.asList(
            "<html:xmp><br/></html:xmp>"
            ),
        Arrays.asList(
            "Element : html:xmp 1+1-1+27",
            "  Element : br 1+11-1+16"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<xmp><br /></xmp>"
            ),
        null, false);
  }

  public final void testAmbiguousAttributes() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<a href= title=foo>bar</a>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+27",
            "  Element : a 1+1-1+27",
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
            "  Element : a 1+1-1+29",
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

  public final void testEndTagCruft() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<a href=foo>bar</a href>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+25",
            "  Element : a 1+1-1+25",
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
            "  Element : a 1+1-3+3",
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

  public final void testMisplacedQuotes() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<span title=malformed attribs' do=don't id=foo checked",
            "onclick=\"a<b\">Bar</span>"
            ),
        Arrays.asList(
            "Fragment 1+1-2+25",
            "  Element : span 1+1-2+25",
            "    Attrib : checked 1+48-1+55",
            "      Value : checked 1+48-1+55",
            "    Attrib : do 1+32-1+34",
            "      Value : don't 1+35-1+40",
            "    Attrib : id 1+41-1+43",
            "      Value : foo 1+44-1+47",
            "    Attrib : onclick 2+1-2+8",
            "      Value : a<b 2+9-2+14",
            "    Attrib : title 1+7-1+12",
            "      Value : malformed attribs 1+13-1+31",
            "    Text : Bar 2+15-2+18"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<span checked=\"checked\" do=\"don&#39;t\" id=\"foo\""
            + " onclick=\"a&lt;b\" title=\"malformed attribs\">Bar</span>"
            )
        );
  }

  public final void testShortTags() throws Exception {
    // See comments in html-sanitizer-test.js as to why we don't bother with
    // short tags.  In short, they are not in HTML5 and not implemented properly
    // in existing HTML4 clients.
    assertParsedHtmlFragment(
        Arrays.asList(
            "<p<a href=\"/\">first part of the text</> second part"
            ),
        Arrays.asList(
            "Fragment 1+1-1+52",
            // "<a" ignored since it is neither a valid element name nor a valid
            // tag name.
            "  Element : p 1+1-1+52",
            "    Attrib : href 1+6-1+10",
            "      Value : / 1+11-1+14",
            "    Text : first part of the text</> second part 1+15-1+52"
            ),
        Arrays.asList(
            "WARNING testShortTags:1+3 - 5: ignoring token '<a'"),
        Arrays.asList(
            "<p href=\"/\">first part of the text&lt;/&gt; second part</p>"
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

  public final void testLeadingAndTrailingContent() throws Exception {
    assertParsedHtml(
        Arrays.asList(
            "xyzw<body></body>xyzw"
            ),
        Arrays.asList(
            "Element : html 1+1-1+22",
            "  Element : head 1+1-1+1",
            "  Element : body 1+1-1+22",  // Expanded to contain text content.
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
            "Element : html 2+3-3+3",
            "  Element : head 2+3-2+3",
            "  Element : body 2+3-2+16",
            "    Text : \\n \t 2+16-3+3"
            ),
        Arrays.<String>asList(),
        Arrays.asList(
            "<html><head></head><body>",
            " \t</body></html>"
            ));
  }

  public final void testCommentsInCdataSection1() throws Exception {
    assertParsedMarkup(
        Arrays.asList("<foo><![CDATA[ <!-- <bar/> --> ]]></foo>"),
        Arrays.asList(
            "Element : foo 1+1-1+41",
            "  CDATA :  <!-- <bar/> -->  1+6-1+35"),
        Arrays.<String>asList(),
        Arrays.asList("<foo><![CDATA[ <!-- <bar/> --> ]]></foo>"),
        true, false);
  }

  public final void testCommentsInCdataSection2() throws Exception {
    assertParsedMarkup(
        Arrays.asList("<foo><![CDATA[ <!-- <bar/> ]]></foo>"),
        Arrays.asList(
            "Element : foo 1+1-1+37",
            "  CDATA :  <!-- <bar/>  1+6-1+31"),
        Arrays.<String>asList(),
        Arrays.asList("<foo><![CDATA[ <!-- <bar/> ]]></foo>"),
        true, false);
  }


  public final void testEmbeddedXmlInHtml() throws Exception {
    assertParsedHtmlFragment(
        Arrays.asList(
            "<DIV><os:If condition='${foo}'><Br/></os:If></div>"
            ),
        Arrays.asList(
            "Fragment 1+1-1+51",
            "  Element : div 1+1-1+51",
            "    Element : os:If 1+6-1+45",
            "      Attrib : condition 1+13-1+22",
            "        Value : ${foo} 1+23-1+31",
            "      Element : br 1+32-1+37"
            ),
        Arrays.<String>asList(
            // TODO: Why is this here?
            "LINT testEmbeddedXmlInHtml:1+6 - 32:"
            + " Element name 'os:If' cannot be represented as XML 1.0."),
        Arrays.asList("<div><os:If condition=\"${foo}\"><br /></os:If></div>"));
  }

  public final void testRender() throws Exception {
    DocumentFragment t = xmlFragment(fromString(
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
        Nodes.render(t, MarkupRenderMode.XML));
    // Rendered as HTML
    assertEquals(
        ""
        + "<script> foo() < bar() </script>\n"
        + "<p></p>\n"
        + " 1 &lt; 2 &amp;&amp; 3 &gt; 4 \n"
        + "<xmp>1 < 2</xmp>\n"
        + "<script> foo() < bar() </script>",
        Nodes.render(t, MarkupRenderMode.HTML));
  }

  public final void testUnrenderableXMLTree1() throws Exception {
    DocumentFragment t = xmlFragment(
        fromString("<xmp><![CDATA[ </xmp> ]]></xmp>"));
    assertEquals(
        "<xmp><![CDATA[ </xmp> ]]></xmp>",
        Nodes.render(t, MarkupRenderMode.XML));
    try {
      String badness = Nodes.render(t, MarkupRenderMode.HTML);
      fail("Bad HTML rendered: " + badness);
    } catch (IllegalArgumentException ex) {
      // Cannot produce <xmp></xmp></xmp> safely in HTML.
    }
  }

  public final void testUnrenderableXMLTree2() throws Exception {
    DocumentFragment t = xmlFragment(
        fromString("<xmp><![CDATA[ </xM]]>p </xmp>"));
    assertEquals(
        "<xmp><![CDATA[ </xM]]>p </xmp>",
        Nodes.render(t, MarkupRenderMode.XML));
    try {
      String badness = Nodes.render(t, MarkupRenderMode.HTML);
      fail("Bad HTML rendered: " + badness);
    } catch (IllegalArgumentException ex) {
      // Cannot produce <xmp> </xMp </xmp> safely in HTML.
    }
  }

  public final void testUnrenderableXMLTree3() throws Exception {
    DocumentFragment t = xmlFragment(
        fromString("<xmp> &lt;/XM<!-- -->P&gt; </xmp>"));
    assertEquals(
        "<xmp> &lt;/XMP&gt; </xmp>", Nodes.render(t, MarkupRenderMode.XML));
    try {
      String badness = Nodes.render(t, MarkupRenderMode.HTML);
      fail("Bad HTML rendered: " + badness);
    } catch (IllegalArgumentException ex) {
      // Cannot produce <xmp> </XMP> </xmp> safely in HTML.
    }
  }

  public final void testCommentsHidingCdataEnd() throws Exception {
    DocumentFragment t = xmlFragment(
        fromString("<xmp> <!-- </xmp> --> </xmp>"));
    assertEquals("<xmp>  </xmp>", Nodes.render(t, MarkupRenderMode.XML));
    assertEquals("<xmp>  </xmp>", Nodes.render(t, MarkupRenderMode.HTML));
  }

  public final void testEofMessageDueToMismatchedQuotes() {
    ParseException pex = null;
    try {
      htmlFragment(fromString("<foo><bar baz='boo></bar></foo>"));
    } catch (ParseException ex) {
      pex = ex;
    }
    if (pex == null) {
      fail("Mismatched quote did not result in exception");
    } else {
      Message msg = pex.getCajaMessage();
      assertEquals(DomParserMessageType.UNCLOSED_TAG, msg.getMessageType());
      assertEquals(
          "testEofMessageDueToMismatchedQuotes:1+6@6 - 10@10",
          msg.getMessageParts().get(0).toString());
    }
  }

  public final void testIssue1207() {
    ParseException pex = null;
    try {
      xmlFragment(fromString("<?xml ?><html><"));
    } catch (ParseException ex) {
      pex = ex;
    }
    assertNotNull(pex);
  }

  public final void testIssue1211XmlnsOnScript() throws Exception {
    DocumentFragment f = htmlFragment(fromString(
        ""
        + "<script type=\"text/os-data\"\n"
        + "    xmlns:os=\"http://ns.opensocial.org/2008/markup\">\n"
        + "  <os:ViewerRequest key=\"viewer\"/>\n"
        + "</script>"));
    assertEquals(
        ""
        + "<script type=\"text/os-data\""
        + " xmlns:os=\"http://ns.opensocial.org/2008/markup\">\n"
        + "  <os:ViewerRequest key=\"viewer\"/>\n"
        + "</script>",
        Nodes.render(f));
  }

  public final void testIssue1211DefaultXmlnsOnScript() throws Exception {
    DocumentFragment f = htmlFragment(fromString(
        ""
        + "<script type=\"text/os-data\"\n"
        + "    xmlns=\"http://ns.opensocial.org/2008/markup\">\n"
        + "  <ViewerRequest key=\"viewer\"/>\n"
        + "</script>"));
    assertEquals(
        ""
        + "<script type=\"text/os-data\">\n"
        + "  <ViewerRequest key=\"viewer\"/>\n"
        + "</script>",
        Nodes.render(f));
  }

  public final void testIssue1211XmlnsOnDiv() throws Exception {
    DocumentFragment f = htmlFragment(fromString(
        ""
        + "<div xmlns:os=\"http://ns.opensocial.org/2008/markup\">\n"
        + "  <os:ViewerRequest key=\"viewer\"/>\n"
        + "</div>"));
    assertEquals(
        ""
        + "<div xmlns:os=\"http://ns.opensocial.org/2008/markup\">\n"
        + "  <os:ViewerRequest key=\"viewer\">\n"
        + "</os:ViewerRequest></div>",
        Nodes.render(f));
  }

  public final void testIssue1211WithPrefixNs() throws Exception {
    DocumentFragment f = htmlFragment(fromString(
        ""
        + "<div xmlns:f=\"http://ns.opensocial.org/2008/markup\">\n"
        + "  <f:foo bar=\"baz\" f:boo=\"far\"/>\n"
        + "</div>"));
    assertEquals(
        ""
        + "<div xmlns:f=\"http://ns.opensocial.org/2008/markup\">\n"
        + "  <f:foo bar=\"baz\" boo=\"far\">\n"
        + "</f:foo></div>",
        Nodes.render(f));
  }

  public final void testIssueOSTemplateMarkup() throws Exception {
    String data = ""
      + "<div xmlns:osx=\"http://ns.opensocial.org/2008/extensions\">\n"
      + "<osx:NavigateToApp>\n"
      + "<img border=\"0\" title=\"Justin\" src=\"foo.gif\">\n"
      + "</osx:NavigateToApp>\n"
      // The second time osx:NavigateToApp appears it has a null namespace uri
      // This causes getLocalName during rendering to return null and thus
      // throw a IllegalStateException
      + "<osx:NavigateToApp>\n"
      + "<span class=\"profile-link\">foo</span>"
      + "</osx:NavigateToApp>"
      + "</div>";
    DocumentFragment f = htmlFragment(fromString(data));
    assertEquals(""
        + "<div xmlns:osx=\"http://ns.opensocial.org/2008/extensions\">\n"
        + "<osx:NavigateToApp>\n"
        + "<img border=\"0\" src=\"foo.gif\" title=\"Justin\" />\n"
        + "</osx:NavigateToApp>\n"
        + "<osx:NavigateToApp>\n"
        + "<span class=\"profile-link\">foo</span>"
        + "</osx:NavigateToApp>"
        + "</div>",
        Nodes.render(f));
  }

  public final void testOrphanedQuotesInTags() throws Exception {
    String input = "<html><head></head>"
    + "<body><div \"></div>"
    + "<div a=\"b\" ></div>"
    + "<div \"test\" ></div>"
    + "<div id=\"test\" \"></div>"
    + "</body></html>";

    String output = "<html><head></head>"
    + "<body><div></div>"
    + "<div a=\"b\"></div>"
    + "<div></div>"
    + "<div id=\"test\"></div>"
    + "</body></html>";

    assertParsedHtml(Arrays.asList(input),
        Arrays.asList(
            "Element : html 1+1-1+113",
            "  Element : head 1+7-1+20",
            "  Element : body 1+20-1+106",
            "    Element : div 1+26-1+39",
            "    Element : div 1+39-1+57",
            "      Attrib : a 1+44-1+45",
            "        Value : b 1+46-1+49",
            "    Element : div 1+57-1+76",
            "    Element : div 1+76-1+99",
            "      Attrib : id 1+81-1+83",
            "        Value : test 1+84-1+90"
            ),
        Arrays.asList(
            "WARNING testOrphanedQuotesInTags:1+31 - 32: ignoring token '\"'",
            "WARNING testOrphanedQuotesInTags:1+62 - 63: ignoring token '\"'",
            "WARNING testOrphanedQuotesInTags:1+63 - 68: ignoring token"
                + " 'test\"'",
            "WARNING testOrphanedQuotesInTags:1+91 - 92: ignoring token '\"'"),
        Arrays.asList(output)
        );
  }

  public final void testIssue1212() throws Exception {
    Node fragment = htmlFragment(fromString("<b>Hello, world</b><!---->"));
    assertEquals("<b>Hello, world</b>", Nodes.render(fragment));
    Node doc = html(fromString("<b>Hello, world</b><!---->"));
    assertEquals(
        "<html><head></head><body><b>Hello, world</b></body></html>",
        Nodes.render(doc));
  }

  public final void testParserSpeed() throws Exception {
    assertFalse(CajaTreeBuilder.DEBUG);  // Don't run 100 times if verbose.
    benchmark(100);  // prime the JIT
    Thread.sleep(250);  // Let the JIT kick-in.
    int microsPerRun = benchmark(250);
    // See extractVarZ in "tools/dashboard/dashboard.pl".
    System.out.println(
        " VarZ:" + getClass().getName() + ".msPerRun=" + microsPerRun);
  }

  private int benchmark(int nRuns) throws IOException, ParseException {
    CharProducer testInput = fromResource("amazon.com.html");
    InputSource is = testInput.getSourceBreaks(0).source();
    MessageQueue mq = DevNullMessageQueue.singleton();
    long t0 = System.nanoTime();
    for (int i = nRuns; --i >= 0;) {
      HtmlLexer lexer = new HtmlLexer(testInput.clone());
      lexer.setTreatedAsXml(false);
      TokenQueue<HtmlTokenType> tq = new TokenQueue<HtmlTokenType>(
          lexer, is, DomParser.SKIP_COMMENTS);
      DomParser p = new DomParser(tq, false, mq);
      p.setNeedsDebugData(false);
      p.parseDocument();
    }
    return (int) ((((double) (System.nanoTime() - t0)) / nRuns) / 1e3);
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

  private void assertParsedHtmlFragmentWithComments(
      List<String> htmlInput,
      List<String> expectedParseTree,
      List<String> expectedMessages,
      List<String> expectedOutputHtml)
      throws ParseException {
    assertParsedMarkup(htmlInput, expectedParseTree, expectedMessages,
                       expectedOutputHtml, false, true, true);
  }

  private void assertParsedMarkup(
      List<String> htmlInput,
      List<String> expectedParseTree,
      List<String> expectedMessages,
      List<String> expectedOutputHtml,
      Boolean asXml,
      boolean fragment)
      throws ParseException {
    assertParsedMarkup(htmlInput, expectedParseTree, expectedMessages,
                       expectedOutputHtml, asXml, fragment, false);
  }

  private void assertParsedMarkup(
      List<String> htmlInput,
      List<String> expectedParseTree,
      List<String> expectedMessages,
      List<String> expectedOutputHtml,
      Boolean asXml,
      boolean fragment,
      boolean wantsComments)
      throws ParseException {

    System.err.println("\n\nStarting " + getName() + "\n===================");
    mq.getMessages().clear();

    DomParser p;
    if (asXml != null) {  // specified
      TokenQueue<HtmlTokenType> tq = tokenizeTestInput(
          Join.join("\n", htmlInput), asXml, wantsComments);
      p = new DomParser(tq, asXml, mq);
    } else {
      HtmlLexer lexer = new HtmlLexer(fromString(Join.join("\n", htmlInput)));
      p = new DomParser(lexer, wantsComments, is, mq);
      asXml = lexer.getTreatedAsXml();
    }
    Node tree = fragment ? p.parseFragment() : p.parseDocument();

    List<String> actualParseTree = formatLines(tree);
    MoreAsserts.assertListsEqual(expectedParseTree, actualParseTree);

    List<String> actualMessages = Lists.newArrayList();
    for (Message message : mq.getMessages()) {
      String messageText = (message.getMessageLevel().name() + " "
                            + message.format(mc));
      actualMessages.add(messageText);
    }
    MoreAsserts.assertListsEqual(expectedMessages, actualMessages, 0);

    MarkupRenderMode rm = asXml ? MarkupRenderMode.XML : MarkupRenderMode.HTML;
    MoreAsserts.assertListsEqual(
        expectedOutputHtml,
        Arrays.asList(Nodes.render(tree, rm).split("\n")));
    Node clone = tree.cloneNode(true);
    MoreAsserts.assertListsEqual(
        expectedOutputHtml,
        Arrays.asList(Nodes.render(clone, rm).split("\n")));

    // Make sure that parsing with and without debug data return the same tree
    // structure.
    Node treeWithoutDebugData;
    {
      TokenQueue<HtmlTokenType> tq = tokenizeTestInput(
          Join.join("\n", htmlInput), asXml, wantsComments);
      DomParser noDebugParser = new DomParser(
          tq, p.asXml(), DevNullMessageQueue.singleton());
      treeWithoutDebugData = fragment
          ? noDebugParser.parseFragment()
          : noDebugParser.parseDocument();
    }

    assertEquals(
        "Comparing parse with and without debug data",
        formatToString(tree, false),
        formatToString(treeWithoutDebugData, false));
  }

  private TokenQueue<HtmlTokenType> tokenizeTestInput(
      String sgmlInput, boolean asXml, boolean wantsComments) {
    try {
      return DomParser.makeTokenQueue(
          FilePosition.startOfFile(is), new StringReader(sgmlInput),
          asXml, wantsComments);
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError(ex);
    }
  }

  private static List<String> formatLines(Node node) {
    StringBuilder sb = new StringBuilder();
    try {
      new Formatter(true, sb).format(node);
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError(ex);
    }
    return sb.length() == 0
        ? Collections.<String>emptyList()
        : Arrays.asList(sb.toString().split("\n"));
  }

  private static String formatToString(Node node, boolean withDebugData) {
    StringBuilder sb = new StringBuilder();
    try {
      new Formatter(withDebugData, sb).format(node);
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError(ex);
    }
    return sb.toString();
  }

  private static class Formatter {
    final Appendable out;
    final boolean withDebugData;

    Formatter(boolean withDebugData, Appendable out) {
      this.withDebugData = withDebugData;
      this.out = out;
    }

    void format(Node node) throws IOException {
      format(node, 0);
    }

    void format(Node node, int depth) throws IOException {
      for (int i = depth; --i >= 0;) { out.append("  "); }
      switch (node.getNodeType()) {
        case Node.DOCUMENT_NODE:
          out.append("Document");
          formatPosition(Nodes.getFilePositionFor(node));
          formatChildren(node, depth);
          break;
        case Node.DOCUMENT_FRAGMENT_NODE:
          out.append("Fragment");
          formatPosition(Nodes.getFilePositionFor(node));
          formatChildren(node, depth);
          break;
        case Node.ELEMENT_NODE:
          out.append("Element : ");
          out.append(node.getNodeName());
          formatPosition(Nodes.getFilePositionFor(node));
          NamedNodeMap attrs = ((Element) node).getAttributes();
          for (int i = 0, n = attrs.getLength(); i < n; ++i) {
            out.append("\n");
            format(attrs.item(i), depth + 1);
          }
          formatChildren(node, depth);
          break;
        case Node.ATTRIBUTE_NODE:
          out.append("Attrib : ");
          out.append(node.getNodeName());
          formatPosition(Nodes.getFilePositionFor(node));
          out.append("\n  ");
          for (int i = depth; --i >= 0;) { out.append("  "); }
          out.append("Value : ");
          formatValue(node.getNodeValue());
          formatPosition(Nodes.getFilePositionForValue((Attr) node));
          break;
        case Node.CDATA_SECTION_NODE:
          out.append("CDATA : ");
          formatValue(node.getNodeValue());
          formatPosition(Nodes.getFilePositionFor(node));
          break;
        case Node.TEXT_NODE:
          out.append("Text : ");
          formatValue(node.getNodeValue());
          formatPosition(Nodes.getFilePositionFor(node));
          break;
        case Node.COMMENT_NODE:
          out.append("Comment : ");
          formatValue(node.getNodeValue());
          formatPosition(Nodes.getFilePositionFor(node));
          break;
        default:
          out.append(node.getNodeName());
          formatPosition(Nodes.getFilePositionFor(node));
          break;
      }
    }

    void formatChildren(Node node, int depth) throws IOException {
      for (Node c = node.getFirstChild(); c != null; c = c.getNextSibling()) {
        out.append("\n");
        format(c, depth + 1);
      }
    }

    void formatValue(String value) throws IOException {
      out.append(value.replace("\\", "\\\\").replace("\n", "\\n")
          .replace("\r", "\\r"));
    }

    void formatPosition(FilePosition pos) throws IOException {
      if (!withDebugData) { return; }
      if (pos == null) { return; }
      if (1 == pos.startCharInFile() && 0 == pos.length()
          && InputSource.UNKNOWN.equals(pos.source())) {
        out.append(" unknown");
      } else {
        out.append(' ')
            .append(String.valueOf(pos.startLineNo()))
            .append('+')
            .append(String.valueOf(pos.startCharInLine()))
            .append('-')
            .append(String.valueOf(pos.endLineNo()))
            .append('+')
            .append(String.valueOf(pos.endCharInLine()));
      }
    }
  }
}
