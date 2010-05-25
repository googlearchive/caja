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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.util.Lists;
import com.google.caja.util.MoreAsserts;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class HtmlQuasiBuilderTest extends TestCase {
  private HtmlQuasiBuilder hb;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    hb = HtmlQuasiBuilder.getBuilder(DomParser.makeDocument(null, null));
  }

  public final void testSubstV() {
    assertEquals(
        "<b title=\"My title\">&lt;Hello&gt;</b>",
        Nodes.render(hb.substV(
            "<b title=@title>@text</b>",
            "title", "My title",
            "text", "<Hello>")));
    assertEquals(
        "<b title=\"My title\">&lt;Hello&gt;</b>",
        Nodes.render(hb.substV(
            "<b title=\"@title\">@text</b>",
            "title", "My title",
            "text", "<Hello>")));
    assertEquals(
        "<b title=\"My title\">&lt;Hello&gt;</b>",
        Nodes.render(hb.substV(
            "<b title='@title'>@text</b>",
            "title", "My title",
            "text", "<Hello>")));
    assertEquals(
        "<b title=\"My title\">Hello <i>World</i></b>",
        Nodes.render(hb.substV(
            "<b title=@title>@a  @b</b>",
            "title", "My title",
            "a", "Hello",
            "b", hb.substV("<i>@t</i>", "t", "World")
            )));
    assertEquals(
        "<b title=\"My title\"><i>Hello</i> World</b>",
        Nodes.render(hb.substV(
            "<b title=@title>@text</b>",
            "title", "My title",
            "text",
            hb.substV("<i>@w1</i>@w2", "w1", "Hello", "w2", " World")
            )));
    assertEquals(
        "<select class=\"type-select\"></select>",
        Nodes.render(hb.substV(
            "<select class=type-select></select>")));
    assertEquals(
        ""
        + "<table summary=\"&#34;Quoted&#34;, a &lt; b &amp;&amp; c &gt; d\">"
        + "<tbody><tr>"
        + "<td>&lt;&#34;Quoted&#34;, a &lt; b &amp;&amp; c &gt; d&gt;</td>"
        + "</tr></tbody>"
        + "</table>",
        Nodes.render(hb.substV(
            "<table summary=@s><tr><td>&lt;@s></table>",
            "s", "\"Quoted\", a < b && c > d"
            )));
  }

  public final void testFramesetsAndBodies() throws Exception {
    assertEquals(
        "<html><head></head><body>Hello</body></html>",
        Nodes.render(hb.substV(
            "<html><head></head><body>@text</body></html>",
            "text", "Hello")));
    assertEquals(
        "<html><head></head><body><div>Hello</div></body></html>",
        Nodes.render(hb.substV(
            "<html><head></head><body>@div</body></html>",
            "div", hb.toFragment("<div>Hello</div>"))));
    assertEquals(
        ""
        + "<html><head></head>"
        + "<frameset><frame src=\"foo.html\"></frame></frameset></html>",
        Nodes.render(hb.substV(
            "<html><head></head><body>@fs</body></html>",
            "fs", hb.toFragment("<frameset><frame src=foo.html></frameset>"))));
    assertEquals(
        ""
        + "<html><head></head>"
        + "<frameset><frame src=\"foo.html\"></frame></frameset></html>",
        Nodes.render(hb.substV(
            "<html><head></head>@fs</html>",
            "fs", hb.toFragment("<frameset><frame src=foo.html></frameset>"))));
    assertEquals(
        ""
        + "<html><head></head>  "
        + "<frameset><frame src=\"foo.html\"></frame></frameset></html>",
        Nodes.render(hb.substV(
            "<html><head></head>  @fs  </html>",
            "fs", hb.toFragment("<frameset><frame src=foo.html></frameset>"))));
  }

  public final void testAttributeValues() {
    assertEquals(
        "<input checked=\"checked\" type=\"checkbox\" />",
        Nodes.render(hb.substV(
            "<input checked='@checked' type=@type>",
            "type", "checkbox",
            "checked", true)));
    assertEquals(
        "<input type=\"radio\" />",
        Nodes.render(hb.substV(
            "<input checked=\"@checked\" type=@type />",
            "type", "radio",
            "checked", false)));
    assertEquals(
        "<select><option selected=\"selected\">Foo</option></select>",
        Nodes.render(hb.substV(
            "<select><option SELECTED=\"@selected\">Foo</option></select>",
            "selected", true)));
  }

  public final void testMultiUse() throws Exception {
    assertEquals(
        "<br /><br />@x<br />",
        Nodes.render(hb.substV(
            "@x@x&#64;x@x",
            "x", hb.toFragment("<br />"))));
  }

  public final void testToFragment() throws Exception {
    DocumentFragment f = hb.toFragment("&mdash;");
    assertEquals("\u2014", ((Text) f.getFirstChild()).getNodeValue());
    assertEquals("&#8212;", Nodes.render(f));
  }

  public final void testFilePositions() throws Exception {
    Document inputDoc = DomParser.makeDocument(null, null);
    InputSource is = new InputSource(URI.create("p:///x"));

    // Produce file positions based on lines with the following lengths
    // 1: 1
    // 2: 1
    // 3: 1
    // 4: 41
    // 5: 1
    // 6: 41
    // 7: 5
    // 8: 41
    String forty = "0123456789012345678901234567890123456789";
    CharProducer cp = CharProducer.Factory.fromString(
        "\n\n\n" + forty + "\n\n" + forty + "\n    \n" + forty,
        is);

    Attr title = inputDoc.createAttributeNS(
        Namespaces.HTML_NAMESPACE_URI, "description");
    title.setNodeValue("Hello, World!");
    Nodes.setFilePositionFor(title, cp.filePositionForOffsets(12, 42));
    Nodes.setFilePositionForValue(title, cp.filePositionForOffsets(22, 42));

    Text boldText = inputDoc.createTextNode("BOLD!");

    Text italicText = inputDoc.createTextNode("/74l1c");
    Element italicEl = inputDoc.createElementNS(
        Namespaces.HTML_NAMESPACE_URI, "SPAN");
    italicEl.appendChild(italicText);
    italicEl.setAttributeNS(Namespaces.HTML_NAMESPACE_URI, "id", "i");
    Nodes.setFilePositionFor(italicEl, cp.filePositionForOffsets(49, 79));

    Node n = hb.substV(
        "<b title=@title>@boldText</b>@plainText<i>@italicEl</i>",
        "title", title,
        "boldText", boldText,
        "plainText", "Plain Text",
        "italicEl", italicEl);

    MoreAsserts.assertListsEqual(
        Arrays.asList(
            "#document-fragment : ???",
            "  b : ???",
            "    title : /x@4+10 - 40",
            "      #text : /x@4+20 - 40",
            "    #text : ???",
            "  #text : ???",
            "  i : ???",
            "    SPAN : /x@6+5 - 35",
            "      id : ???",
            "        #text : ???",
            "      #text : ???"
            ),
        nodePositions(n));

    HtmlQuasiBuilder.usePosition(cp.filePositionForOffsets(0, 100), n);

    assertEquals(
        ""
        + "<b title=\"Hello, World!\">BOLD!</b>"
        + "Plain Text"
        + "<i><span id=\"i\">/74l1c</span></i>",
        Nodes.render(n));

    MoreAsserts.assertListsEqual(
        Arrays.asList(
            "#document-fragment : /x@1+1 - 8+10",
            "  b : /x@4+10 - 40",
            "    title : /x@4+10 - 40",
            "      #text : /x@4+20 - 40",
            "    #text : /x@4+40",
            "  #text : /x@4+40",
            "  i : /x@4+40 - 6+35",
            "    SPAN : /x@6+5 - 35",
            "      id : /x@6+5",
            "        #text : /x@6+5",
            "      #text : /x@6+5"
            ),
        nodePositions(n));
  }

  public final void testProblematicElements() {
    assertEquals(
        "option",
        onlyElement(hb.substV("<option>Foo</option>")).getLocalName());
    assertEquals(
        "option",
        onlyElement(hb.substV("<OPTION>Foo</OPTION>")).getLocalName());
    assertEquals(
        "thead",
        onlyElement(hb.substV("<thead>Foo</thead>")).getLocalName());
    assertEquals(
        "tbody",
        onlyElement(hb.substV("<tbody>Foo</tbody>")).getLocalName());
    assertEquals(
        "tfoot",
        onlyElement(hb.substV("<tfoot></tfoot>")).getLocalName());
    assertEquals(
        "caption",
        onlyElement(hb.substV("<caption>Foo</caption>")).getLocalName());
    assertEquals(
        "tr", onlyElement(hb.substV("<tr><td>Foo</td></tr>")).getLocalName());
    assertEquals(
        "td", onlyElement(hb.substV("<td>Foo</td>")).getLocalName());
    assertEquals(
        "th", onlyElement(hb.substV("<th>Foo</th>")).getLocalName());
    assertEquals(
        "p", onlyElement(hb.substV("<p>Not problematic")).getLocalName());
  }

  private static Element onlyElement(Node node) {
    assertTrue(node.getNodeName(), node instanceof DocumentFragment);
    Element el = (Element) node.getFirstChild();
    assertNotNull(el);
    assertNull("" + el.getNextSibling(), el.getNextSibling());
    return el;
  }

  private static List<String> nodePositions(Node n) {
    List<String> out = Lists.newArrayList();
    appendNodePositions(n, 0, out);
    return out;
  }

  private static void appendNodePositions(Node n, int depth, List<String> out) {
    StringBuilder sb = new StringBuilder();
    for (int i = depth; --i >= 0;) { sb.append("  "); }
    sb.append(n.getNodeName()).append(" : ");
    FilePosition p = Nodes.getFilePositionFor(n);
    if (InputSource.UNKNOWN.equals(p.source())) {
      sb.append("???");
    } else {
      sb.append(p.source().getUri().getPath()).append('@')
          .append(p.startLineNo()).append('+').append(p.startCharInLine());
      if (p.length() != 0) {
        sb.append(" - ");
        if (p.endLineNo() != p.startLineNo()) {
          sb.append(p.endLineNo()).append('+');
        }
        sb.append(p.endCharInLine());
      }
    }
    out.add(sb.toString());

    if (n instanceof Element) {
      for (Attr a : Nodes.attributesOf((Element) n)) {
        appendNodePositions(a, depth + 1, out);
      }
    }
    for (Node c : Nodes.childrenOf(n)) {
      appendNodePositions(c, depth + 1, out);
    }
  }
}
