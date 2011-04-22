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

package com.google.caja.parser.html;

import com.google.caja.lexer.ParseException;
import com.google.caja.render.Concatenator;
import com.google.caja.reporting.MarkupRenderMode;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.lexer.FilePosition;
import com.google.caja.util.MoreAsserts;

import java.util.Arrays;
import java.io.IOException;
import java.io.StringReader;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

public class NodesTest extends CajaTestCase {
  public final void testDecode() {
    assertEquals(Nodes.decode("1 &lt; 2 &amp;&amp; 4 &gt; &quot;3&quot;"),
                 "1 < 2 && 4 > \"3\"");
    assertEquals("", Nodes.decode(""));
    assertEquals("No entities here", Nodes.decode("No entities here"));
    assertEquals("No entities here & there",
                 Nodes.decode("No entities here & there"));
    // Test that interrupted escapes and escapes at beginning and end of file
    // are handled gracefully.
    assertEquals("\\\\u000a", Nodes.decode("\\\\u000a"));
    assertEquals("\n", Nodes.decode("&#x00000a;"));
    assertEquals("\n", Nodes.decode("&#x0000a;"));
    assertEquals("\n", Nodes.decode("&#x000a;"));
    assertEquals("\n", Nodes.decode("&#x00a;"));
    assertEquals("\n", Nodes.decode("&#x0a;"));
    assertEquals("\n", Nodes.decode("&#xa;"));
    assertEquals(String.valueOf(Character.toChars(0x10000)),
                 Nodes.decode("&#x10000;"));
    assertEquals("&#xa", Nodes.decode("&#xa"));
    assertEquals("&#x00ziggy", Nodes.decode("&#x00ziggy"));
    assertEquals("&#xa00z;", Nodes.decode("&#xa00z;"));
    assertEquals("&#\n", Nodes.decode("&#&#x000a;"));
    assertEquals("&#x\n", Nodes.decode("&#x&#x000a;"));
    assertEquals("&#xa\n", Nodes.decode("&#xa&#x000a;"));
    assertEquals("&#\n", Nodes.decode("&#&#xa;"));
    assertEquals("&#x", Nodes.decode("&#x"));
    assertEquals("&#x0", Nodes.decode("&#x0"));
    assertEquals("&#", Nodes.decode("&#"));

    assertEquals("\\", Nodes.decode("\\"));
    assertEquals("&", Nodes.decode("&"));

    assertEquals("&#000a;", Nodes.decode("&#000a;"));
    assertEquals("\n", Nodes.decode("&#10;"));
    assertEquals("\n", Nodes.decode("&#010;"));
    assertEquals("\n", Nodes.decode("&#0010;"));
    assertEquals("\n", Nodes.decode("&#00010;"));
    assertEquals("\n", Nodes.decode("&#000010;"));
    assertEquals("\n", Nodes.decode("&#0000010;"));
    assertEquals("\t", Nodes.decode("&#9;"));

    assertEquals("&#10", Nodes.decode("&#10"));
    assertEquals("&#00ziggy", Nodes.decode("&#00ziggy"));
    assertEquals("&#\n", Nodes.decode("&#&#010;"));
    assertEquals("&#0\n", Nodes.decode("&#0&#010;"));
    assertEquals("&#01\n", Nodes.decode("&#01&#10;"));
    assertEquals("&#\n", Nodes.decode("&#&#10;"));
    assertEquals("&#1", Nodes.decode("&#1"));
    assertEquals("&#10", Nodes.decode("&#10"));

    // test the named escapes
    assertEquals("<", Nodes.decode("&lt;"));
    assertEquals(">", Nodes.decode("&gt;"));
    assertEquals("\"", Nodes.decode("&quot;"));
    assertEquals("'", Nodes.decode("&apos;"));
    assertEquals("&", Nodes.decode("&amp;"));
    assertEquals("&lt;", Nodes.decode("&amp;lt;"));
    assertEquals("&", Nodes.decode("&AMP;"));
    assertEquals("&AMP", Nodes.decode("&AMP"));
    assertEquals("&", Nodes.decode("&AmP;"));
    assertEquals("\u0391", Nodes.decode("&Alpha;"));
    assertEquals("\u03b1", Nodes.decode("&alpha;"));

    assertEquals("&;", Nodes.decode("&;"));
    assertEquals("&bogus;", Nodes.decode("&bogus;"));
  }

  public final void testRenderOfEmbeddedXml() throws Exception {
    assertEquals(
        "<td width=\"10\"><svg:Rect width=\"50\"></svg:Rect></td>",
        Nodes.render(
            xmlFragment(fromString(
                "<html:td width='10'><svg:Rect width='50'/></html:td>")),
            MarkupRenderMode.HTML));
    assertEquals(
        "<td width=\"10\"><svg:Rect width=\"50\" /></td>",
        Nodes.render(
            xmlFragment(fromString(
                "<html:td width='10'><svg:Rect width='50'/></html:td>")),
            MarkupRenderMode.XML));
  }

  public final void testRenderWithNonstandardNamespaces() throws Exception {
    assertEquals(
        "<td width=\"10\"><svg:Rect width=\"50\" /></td>",
        Nodes.render(xmlFragment(fromString(
            ""
            + "<html:td width='10' xmlns:s='http://www.w3.org/2000/svg'>"
            + "<s:Rect width='50'/>"
            + "</html:td>")),
            MarkupRenderMode.XML));
  }

  final String TEST_XML = (
      "<foo>\n"
      + "before <!-- Test Data --> after \n"
      + "<!-- [if IE ]>"
      + "<link href=\"iecss.css\" rel=\"stylesheet\" type=\"text/css\">"
      + "<![endif]-->"
      + "</foo>");
  final String RENDER_WITH_COMMENTS = TEST_XML;
  final String RENDER_NO_COMMENTS = "<foo>\n"
      + "before  after \n"
      + "</foo>";
  public final void testCommentsRemoved() throws Exception {
    TokenQueue<HtmlTokenType> tq = DomParser.makeTokenQueue(
        FilePosition.startOfFile(is), new StringReader(TEST_XML), true, true);
    Element el = new DomParser(tq, true, mq).parseDocument();
    assertEquals(RENDER_NO_COMMENTS, Nodes.render(el, MarkupRenderMode.HTML));
    assertEquals(RENDER_NO_COMMENTS, Nodes.render(el, MarkupRenderMode.XML));
  }

  public final void testCommentsPreservedInUnsafeMode() throws Exception {
    Node el = parse(TEST_XML);
    assertRendersUnsafe(RENDER_WITH_COMMENTS, el, MarkupRenderMode.HTML);
    assertRendersUnsafe(RENDER_WITH_COMMENTS, el, MarkupRenderMode.XML);
  }
  
  public final void testIllegalCharactersInComment() throws Exception {
    assertFailsToRenderUnsafe("<!-- -- -->", MarkupRenderMode.HTML,
        "XML/HTML comment", "contains '--'");
    assertFailsToRenderUnsafe("<!-- -- -->", MarkupRenderMode.XML,
        "XML/HTML comment", "contains '--'");
    assertFailsToRenderUnsafe("<!-->>>-->", MarkupRenderMode.HTML,
        "XML/HTML comment", "starts with '>'");
    assertFailsToRenderUnsafe("<!-->>>-->", MarkupRenderMode.XML,
        "XML/HTML comment", "starts with '>'");
  }

  private Node parse(String xml) throws Exception {
    TokenQueue<HtmlTokenType> tq = DomParser.makeTokenQueue(
        FilePosition.startOfFile(is), new StringReader(xml), true, true);
    return new DomParser(tq, true, mq).parseFragment();
  }
  
  private void assertRendersUnsafe(String expected, Node el,
      MarkupRenderMode mode) throws Exception {
    try {
      assertEquals(expected, Nodes.renderUnsafe(el, mode));
    } catch (IllegalStateException e) {
      fail(e.getMessage());
    }
  }

  private void assertFailsToRenderUnsafe(
      String xml, MarkupRenderMode mode, String... messages) 
      throws Exception {
    try {
      TokenQueue<HtmlTokenType> tq = DomParser.makeTokenQueue(
          FilePosition.startOfFile(is), new StringReader(xml), true, true);
      DocumentFragment el = new DomParser(tq, true, mq).parseFragment();
      Nodes.renderUnsafe(el, mode);
      fail("No error rendering illegal fragment");
    } catch (IllegalStateException e) {
      for (String m : messages) {
        assertTrue("Missing message:" + m, e.getMessage().contains(m));
      }
    }
  }

  public final void testRenderWithUnknownNamespace() throws Exception {
    DocumentFragment fragment = xmlFragment(fromString(
        ""
        + "<foo xmlns='http://www.w3.org/XML/1998/namespace'"
        + " xmlns:bar='http://bobs.house.of/XML&BBQ'>"
        + "<bar:baz boo='howdy' xml:lang='es'/>"
        + "</foo>"));
    // Remove any XMLNS attributes and prefixes.
    Element el = (Element) fragment.getFirstChild();
    while (el.getAttributes().getLength() != 0) {
      el.removeAttributeNode((Attr) el.getAttributes().item(0));
    }
    el.setPrefix("");
    el.getFirstChild().setPrefix("");
    assertEquals(
        ""
        + "<xml:foo>"
        + "<_ns1:baz xmlns:_ns1=\"http://bobs.house.of/XML&amp;BBQ\""
        + " boo=\"howdy\" xml:lang=\"es\" />"
        + "</xml:foo>",
        Nodes.render(fragment, MarkupRenderMode.XML));
  }

  public final void testRenderWithMaskedInputNamespace1() throws Exception {
    DocumentFragment fragment = xmlFragment(fromString(
        "<svg:foo><svg:bar xmlns:svg='" + Namespaces.HTML_NAMESPACE_URI
        + "'/></svg:foo>"));
    assertEquals("<svg:foo><bar></bar></svg:foo>", Nodes.render(fragment));
  }

  public final void testRenderWithMaskedInputNamespace2() throws Exception {
    DocumentFragment fragment = xmlFragment(fromString(
        "<svg:foo><svg:bar xmlns:svg='http://foo/'/></svg:foo>"));
    assertEquals(
        "<svg:foo><_ns1:bar xmlns:_ns1=\"http://foo/\"></_ns1:bar></svg:foo>",
        Nodes.render(fragment));
  }

  public final void testRenderWithMaskedOutputNamespace1() throws Exception {
    DocumentFragment fragment = xmlFragment(fromString(
        "<svg:foo><xml:bar/></svg:foo>"));
    Namespaces ns = new Namespaces(
        Namespaces.HTML_DEFAULT, "svg", Namespaces.XML_NAMESPACE_URI);
    StringBuilder sb = new StringBuilder();
    RenderContext rc = new RenderContext(new Concatenator(sb))
        .withMarkupRenderMode(MarkupRenderMode.XML);
    Nodes.render(fragment, ns, rc);
    rc.getOut().noMoreTokens();
    assertEquals(
        ""
        + "<_ns2:foo xmlns:_ns2=\"http://www.w3.org/2000/svg\">"
        + "<svg:bar /></_ns2:foo>",
        sb.toString());
  }

  public final void testHtmlNamesNormalized() throws Exception {
    Document doc = DomParser.makeDocument(null, null);
    Element el = doc.createElementNS(Namespaces.HTML_NAMESPACE_URI, "SPAN");
    el.setAttributeNS(Namespaces.HTML_NAMESPACE_URI, "TITLE", "Howdy");

    assertEquals("<span title=\"Howdy\"></span>", Nodes.render(el));

    Namespaces ns = new Namespaces(
        Namespaces.HTML_DEFAULT, "html", Namespaces.HTML_NAMESPACE_URI);
    StringBuilder sb = new StringBuilder();
    RenderContext rc = new RenderContext(new Concatenator(sb))
        .withMarkupRenderMode(MarkupRenderMode.HTML);
    Nodes.render(el, ns, rc);
    rc.getOut().noMoreTokens();
    assertEquals("<html:span title=\"Howdy\"></html:span>", sb.toString());
  }

  public final void testNoSneakyNamespaceDecls1() throws Exception {
    Document doc = DomParser.makeDocument(null, null);
    Element el = doc.createElementNS(Namespaces.SVG_NAMESPACE_URI, "span");
    try {
      el.setAttributeNS(
          Namespaces.SVG_NAMESPACE_URI, "xmlns", Namespaces.HTML_NAMESPACE_URI);
    } catch (Exception ex) {
      try {
        el.setAttributeNS(
            Namespaces.HTML_NAMESPACE_URI, "xmlns",
            Namespaces.HTML_NAMESPACE_URI);
      } catch (Exception ex2) {
        // OK
      }
    }
    el.appendChild(doc.createElementNS(Namespaces.SVG_NAMESPACE_URI, "br"));
    String rendered;
    try {
      rendered = Nodes.render(el, MarkupRenderMode.XML);
    } catch (RuntimeException ex) {
      return;  // Failure is an option.
    }
    assertEquals("<svg:span><svg:br /></svg:span>", rendered);
  }

  public final void testNoSneakyNamespaceDecls2() throws Exception {
    Document doc = DomParser.makeDocument(null, null);
    Element el = doc.createElementNS(Namespaces.SVG_NAMESPACE_URI, "span");
    try {
      el.setAttributeNS(
            Namespaces.HTML_NAMESPACE_URI, "xmlns:svg",
            Namespaces.HTML_NAMESPACE_URI);
      } catch (Exception ex2) {
      // OK
      }
    el.appendChild(doc.createElementNS(Namespaces.SVG_NAMESPACE_URI, "br"));
    String rendered;
    try {
      rendered = Nodes.render(el, MarkupRenderMode.XML);
    } catch (RuntimeException ex) {
      return;  // Failure is an option.
    }
    assertEquals("<svg:span><svg:br /></svg:span>", rendered);
  }

  public final void testNoSneakyNamespaceDecls3() throws Exception {
    Document doc = DomParser.makeDocument(null, null);
    Element el = doc.createElementNS(
        Namespaces.SVG_NAMESPACE_URI, "span");
    // Override a definition used elsewhere.
    el.setAttribute("xmlns:svg", Namespaces.HTML_NAMESPACE_URI);

    el.appendChild(doc.createElementNS(Namespaces.SVG_NAMESPACE_URI, "br"));
    String rendered;
    try {
      rendered = Nodes.render(el, MarkupRenderMode.XML);
    } catch (RuntimeException ex) {
      return;  // Failure is an option.
    }
    assertEquals("<svg:span><svg:br /></svg:span>", rendered);
  }

  public final void testProcessingInstructions() {
    Document doc = DomParser.makeDocument(null, null);
    ProcessingInstruction pi = doc.createProcessingInstruction("foo", "bar");
    assertEquals("<?foo bar?>", Nodes.render(pi, MarkupRenderMode.XML));
  }

  public final void testDocumentType() throws ParseException {
    String[] docTypes = {
        "<!DOCTYPE html PUBLIC "
        + "\"-//W3C//DTD HTML 4.01 Transitional//EN\" "
        + "\"http://www.w3.org/TR/html4/loose.dtd\">",
        "<!DOCTYPE html PUBLIC "
        + "\"-//W3C//DTD XHTML 1.0 Transitional//EN\" "
        + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">",
        "<!DOCTYPE html>"
    };
    for (String docType : docTypes) {
      Document doc = DomParser.makeDocument(DoctypeMaker.parse(docType), null);
      Element el = html(fromString("<html><b>my text</b></html>"));
      doc.appendChild(doc.adoptNode(el));
      MoreAsserts.assertStartsWith(docType,
          Nodes.render(doc.getDoctype(), el, null));
    }
  }

  public final void testBadDocumentType() throws ParseException {
    // bad system id
    assertDocType(
        "<!DOCTYPE html PUBLIC "
        + "\"-//W3C//DTD HTML 4.01 Transitional//EN\">",
        "<!DOCTYPE html PUBLIC "
        + "\"-//W3C//DTD HTML 4.01 Transitional//EN\" "
        + "\"javascript:alert(1);\">");
    /*
     *  unrecognized doctype
     *  if needed, the whitelist of good doctypes can be expanded without
     *  violating security as long as its a content type that the rewriter
     *  knows how to contain.
     */
    assertNoDocType(
        "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" "
        + "\"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");
  }

  public final void testBadProcessingInstructions() {
    Document doc = DomParser.makeDocument(null, null);
    for (String[] badPi : new String[][] {
           { "xml", "foo" }, { "XmL", "foo" },
           { "foo?><script>alert(1)</script>", "<?bar baz" },
           { "ok", "foo?><script>alert(1)</script><?foo bar" },
         }) {
      try {
        ProcessingInstruction pi = doc.createProcessingInstruction(
            badPi[0], badPi[1]);
        Nodes.render(pi, MarkupRenderMode.XML);
      } catch (IllegalStateException ex) {
        continue;  // OK
      } catch (DOMException ex) {
        continue;  // OK
      }
      fail("Rendered " + Arrays.toString(badPi));
    }
  }

  public final void testProcessingInstructionInHtml() {
    Document doc = DomParser.makeDocument(null, null);
    ProcessingInstruction pi = doc.createProcessingInstruction(
        "foo", "<script>alert(1)</script>");
    Element el = doc.createElementNS(Namespaces.HTML_NAMESPACE_URI, "div");
    el.appendChild(pi);
    assertEquals(
        "<div><?foo <script>alert(1)</script>?></div>",
        Nodes.render(el, MarkupRenderMode.XML));
    try {
      Nodes.render(el, MarkupRenderMode.HTML);
    } catch (IllegalStateException ex) {
      // OK
      return;
    }
    fail("Rendered in html");
  }

  public final void testRenderWithBrokenNekoDom() throws Exception {
    Element el = html(fromString("<a href='foo.html'>bar</a>"));
    assertEquals(
        "<html><head></head><body><a href=\"foo.html\">bar</a></body></html>",
        Nodes.render(new NullLocalNameMembrane().wrap(el, Element.class)));
  }

  public final void testRenderModes() throws Exception {
    DocumentFragment f = htmlFragment(fromString(
        "<input checked name=foo type=checkbox>"));
    assertEquals(
        "<input checked=\"checked\" name=\"foo\" type=\"checkbox\" />",
        Nodes.render(f, MarkupRenderMode.XML));
    assertEquals(
        "<input checked=\"checked\" name=\"foo\" type=\"checkbox\" />",
        Nodes.render(f, MarkupRenderMode.HTML));
    assertEquals(
        "<input checked name=\"foo\" type=\"checkbox\">",
        Nodes.render(f, MarkupRenderMode.HTML4_BACKWARDS_COMPAT));
  }

  public final void testEscapingTextSpansStyle() {
    checkEscapingTextSpans("style", "body { font-family: \"<!--\" }", null);
    checkEscapingTextSpans("style", "body { font-family: \"-->\" }", null);
    checkEscapingTextSpans("style", "<!-- body { color: #000 }  -->", true);
    checkEscapingTextSpans("style", "<!-- /* </style> */ -->", false);
    checkEscapingTextSpans(
        "style", "<!-- /* </style */ a > b\n{ color: alert(1337) } -->", false);
  }

  public final void testEscapingTextSpansScript() {
    checkEscapingTextSpans("script", "a <!--b", null);
    checkEscapingTextSpans("script", "a--> b", null);
    checkEscapingTextSpans("script", "a <!--b && c--> d", true);
    checkEscapingTextSpans(
        "script", "<!--document.write('<script>f()</script>')-->", true);
  }

  public final void testEscapingTextSpansTitle() {
    checkEscapingTextSpans("title", "<!--", null);
    checkEscapingTextSpans("title", "-->", null);
    checkEscapingTextSpans("title", "<!-- wtf -->", true);
  }

  public final void testEscapingTextSpansTextarea() {
    checkEscapingTextSpans("textarea", "<!--", null);
    checkEscapingTextSpans("textarea", "-->", null);
    checkEscapingTextSpans("textarea", "<!-- text -->", true);
  }

  public final void testEscapingTextSpansNoscript() {
    checkEscapingTextSpans("noscript", "<!--", null);
    checkEscapingTextSpans("noscript", "-->", null);
    checkEscapingTextSpans("noscript", "<!-- no script here -->", true);
    checkEscapingTextSpans("noscript", "<!-- </noscript> -->", false);
  }

  private void checkEscapingTextSpans(
      String elName, String textContent, Boolean pass) {
    Document doc = DomParser.makeDocument(null, null);
    Element el = doc.createElementNS(Namespaces.HTML_NAMESPACE_URI, elName);
    el.appendChild(doc.createTextNode(textContent));
    try {
      String s = Nodes.render(el, MarkupRenderMode.HTML);
      // There is either no <!-- or a following -->.
      if (s.contains("<!--")) {
        assertTrue(s, s.indexOf("<!--") < s.indexOf("-->"));
      } else {
        assertFalse(s, s.contains("-->"));
      }
      if (pass != null) {
        assertTrue(pass);
      }
    } catch (IllegalArgumentException ex) {
      if (pass != null) {
        assertFalse(pass);
      }
    }
  }

  public final void testRenderSpeed() throws Exception {
    Element doc = html(fromResource("amazon.com.html"));
    benchmark(100, doc);  // prime the JIT
    Thread.sleep(250);  // Let the JIT kick-in.
    int microsPerRun = benchmark(250, doc);
    // See extractVarZ in "tools/dashboard/dashboard.pl".
    // This should be named usPerRun, but there is already history with the
    // broken name.
    System.out.println(
        " VarZ:" + getClass().getName() + ".msPerRun=" + microsPerRun);
  }

  private int benchmark(int nRuns, Element el) {
    long t0 = System.nanoTime();
    for (int i = nRuns; --i >= 0;) { Nodes.render(el); }
    return (int) ((((double) (System.nanoTime() - t0)) / nRuns) / 1e3);
  }

  private void assertNoDocType(String docType)
      throws ParseException {
    Document doc = DomParser.makeDocument(DoctypeMaker.parse(docType), null);
    Element el = html(fromString("TEST NODE"));
    doc.appendChild(doc.adoptNode(el));
    assertFalse(Nodes.render(doc.getDoctype(), el, null)
        .matches("[<][!]DOCTYPE"));
  }

  private void assertDocType(String expected, String docType)
      throws ParseException {
    Document doc = DomParser.makeDocument(DoctypeMaker.parse(docType), null);
    Element el = html(fromString("TEST NODE"));
    doc.appendChild(doc.adoptNode(el));
    MoreAsserts.assertStartsWith(expected,
        Nodes.render(doc.getDoctype(), el, null));
  }
}
