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

import com.google.caja.render.Concatenator;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;

import java.util.Arrays;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

public class NodesTest extends CajaTestCase {
  public final void testDecode() throws Exception {
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
        Nodes.render(xmlFragment(fromString(
            "<html:td width='10'><svg:Rect width='50'/></html:td>")), false));
    assertEquals(
        "<td width=\"10\"><svg:Rect width=\"50\" /></td>",
        Nodes.render(xmlFragment(fromString(
            "<html:td width='10'><svg:Rect width='50'/></html:td>")), true));
  }

  public final void testRenderWithNonstandardNamespaces() throws Exception {
    assertEquals(
        "<td width=\"10\"><svg:Rect width=\"50\" /></td>",
        Nodes.render(xmlFragment(fromString(
            ""
            + "<html:td width='10' xmlns:s='http://www.w3.org/2000/svg'>"
            + "<s:Rect width='50'/>"
            + "</html:td>")),
            true));
  }

  public final void testRenderWithUnknownNamespace() throws Exception {
    assertEquals(
        ""
        + "<xml:foo>"
        + "<_ns1:baz xmlns:_ns1=\"http://bobs.house.of/XML&amp;BBQ\""
        + " boo=\"howdy\" xml:lang=\"es\" />"
        + "</xml:foo>",
        Nodes.render(xmlFragment(fromString(
            ""
            + "<foo xmlns='http://www.w3.org/XML/1998/namespace'"
            + " xmlns:bar='http://bobs.house.of/XML&BBQ'>"
            + "<bar:baz boo='howdy' xml:lang='es'/>"
            + "</foo>")),
            true));
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
    RenderContext rc = new RenderContext(new Concatenator(sb)).withAsXml(true);
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
    RenderContext rc = new RenderContext(new Concatenator(sb)).withAsXml(false);
    Nodes.render(el, ns, rc);
    rc.getOut().noMoreTokens();
    assertEquals("<html:span title=\"Howdy\"></html:span>", sb.toString());
  }

  public final void testNoSneakyNamespaceDecls1() throws Exception {
    Document doc = DomParser.makeDocument(null, null);
    Element el = doc.createElementNS(
        Namespaces.SVG_NAMESPACE_URI, "span");
    try {
      el.setAttributeNS(
          Namespaces.SVG_NAMESPACE_URI, "xmlns", Namespaces.HTML_NAMESPACE_URI);
    } catch (Exception ex) {
      try {
        el.setAttributeNS(
            Namespaces.HTML_NAMESPACE_URI, "xmlns",
            Namespaces.HTML_NAMESPACE_URI);
      } catch (Exception ex2) {
        el.setAttribute("xmlns", Namespaces.HTML_NAMESPACE_URI);
      }
    }
    el.appendChild(doc.createElementNS(Namespaces.SVG_NAMESPACE_URI, "br"));
    String rendered;
    try {
      rendered = Nodes.render(el);
    } catch (RuntimeException ex) {
      return;  // Failure is an option.
    }
    assertEquals("<svg:span><svg:br/></svg:span>", rendered);
  }

  public final void testNoSneakyNamespaceDecls2() throws Exception {
    Document doc = DomParser.makeDocument(null, null);
    Element el = doc.createElementNS(
        Namespaces.SVG_NAMESPACE_URI, "span");
    try {
      el.setAttributeNS(
          Namespaces.XMLNS_NAMESPACE_URI, "svg", Namespaces.HTML_NAMESPACE_URI);
    } catch (Exception ex) {
      try {
        el.setAttributeNS(
            Namespaces.HTML_NAMESPACE_URI, "xmlns:svg",
            Namespaces.HTML_NAMESPACE_URI);
      } catch (Exception ex2) {
        el.setAttribute("xmlns:svg", Namespaces.HTML_NAMESPACE_URI);
      }
    }
    el.appendChild(doc.createElementNS(Namespaces.SVG_NAMESPACE_URI, "br"));
    String rendered;
    try {
      rendered = Nodes.render(el);
    } catch (RuntimeException ex) {
      return;  // Failure is an option.
    }
    assertEquals("<svg:span><svg:br/></svg:span>", rendered);
  }

  public final void testProcessingInstructions() {
    Document doc = DomParser.makeDocument(null, null);
    ProcessingInstruction pi = doc.createProcessingInstruction("foo", "bar");
    assertEquals("<?foo bar?>", Nodes.render(pi, true));
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
        Nodes.render(pi, true);
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
        Nodes.render(el, /* XML */true));
    try {
      Nodes.render(el, /* HTML */false);
    } catch (IllegalStateException ex) {
      // OK
      return;
    }
    fail("Rendered in html");
  }

  public final void testRenderSpeed() throws Exception {
    Element doc = html(fromResource("amazon.com.html"));
    benchmark(100, doc);  // prime the JIT
    Thread.sleep(250);  // Let the JIT kick-in.
    int microsPerRun = benchmark(250, doc);
    // See extractVarZ in "tools/dashboard/dashboard.pl".
    System.out.println(
        " VarZ:" + getClass().getName() + ".msPerRun=" + microsPerRun);
  }

  private int benchmark(int nRuns, Element el) {
    long t0 = System.nanoTime();
    for (int i = nRuns; --i >= 0;) { Nodes.render(el); }
    return (int) ((((double) (System.nanoTime() - t0)) / nRuns) / 1e3);
  }
}
