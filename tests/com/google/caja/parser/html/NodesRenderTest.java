// Copyright (C) 2012 Google Inc.
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
import com.google.caja.reporting.MarkupRenderMode;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.FailureIsAnOption;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

import junit.framework.AssertionFailedError;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class tests safety and fidelity of {@link Nodes#render}.
 * <p>
 * Caja's security depends on faithful interpretation of Caja's output.
 * Ideally Caja's XML output will always be interpreted as XML rather than
 * HTML (and vice-versa), but this type of misinterpretation is an easy
 * mistake to make.
 * <p>
 * So what we're trying to do here is guarantee that Caja's XML and HTML
 * outputs are always "safe", whether they're subsequently interpreted or
 * misinterpreted as XML or HTML.
 * <p>
 * There are a couple types of tests here:
 *
 * <ul>
 * <li>"precise" - Most DOM trees can be rendered in a way that will be
 * interpreted the same way by XML and HTML parsers.</li>
 *
 * <li>"robust" - Some DOM trees cannot be rendered precisely, but we can
 * ensure that the only difference between XML and HTML interpretation is
 * the values of inert text content. This is basically a guarantee that it's
 * impossible for inert text to become misinterpreted as script.</li>
 *
 * <li>"htmlOnly" - Some DOM trees can be rendered safely as HTML (and
 * misinterpretation as XML is harmless), but can't be rendered safely
 * as XML</li>
 *
 * <li>"xmlOnly" - Some DOM trees can be rendered safely as XML (and
 * misinterpretation as HTML is harmless), but can't be rendered safely
 * as HTML</li>
 *
 * <li>"unsafe" - Some DOM trees are unsafe to render as HTML or XML.</li>
 * </ul>
 *
 * @author felix8a@gmail.com
 */

public class NodesRenderTest extends CajaTestCase {

  public final void testEmpty() {
    precise("");
    assertRender("", "");
  }

  public final void testDiv1() {
    precise("<div></div><div>&lt;/div></div>");
    assertRender(
        "<div></div><div>&lt;/div&gt;</div>",
        "<div></div><div>&lt;/div></div>");
  }

  // <![CDATA[]]> has meaning in XML but not in HTML.  These next few tests
  // verify a couple properties:
  // - If a dom tree was parsed as XML and has a CDATASection node, the
  //   node doesn't get rendered with "<![CDATA[...]]>", which would
  //   screw up HTML interpretation.</li>
  // - If a dom tree has a text node containing "<![CDATA[", it doesn't
  //   get rendered as "<![CDATA[", which would screw up XML interpretation.

  public final void testCdata1() {
    precise("1&lt;<![CDATA[2&lt;]]><![CDATA[3<]]>4&gt;5");
    assertRender(
        "1&lt;2&amp;lt;3&lt;4&gt;5",
        "1&lt;<![CDATA[2&lt;]]><![CDATA[3<]]>4&gt;5");
    assertRender(
        "1&lt;&lt;![CDATA[2&lt;]]&gt;&lt;![CDATA[3&lt;]]&gt;4&gt;5",
        "1&lt;&lt;![CDATA[2&lt;]]>&lt;![CDATA[3&lt;]]>4&gt;5");
  }

  public final void testCdata2() {
    precise("<div>1&lt;<![CDATA[2&lt;]><![CDATA[3<]]>4&gt;5</div>");
    assertRender(
        "<div>1&lt;2&amp;lt;3&lt;4&gt;5</div>",
        "<div>1&lt;<![CDATA[2&lt;]]><![CDATA[3<]]>4&gt;5</div>");
    assertRender(
        "<div>1&lt;&lt;![CDATA[2&lt;]]&gt;&lt;![CDATA[3&lt;]]&gt;4&gt;5</div>",
        "<div>1&lt;&lt;![CDATA[2&lt;]]>&lt;![CDATA[3&lt;]]>4&gt;5</div>");
  }

  public final void testCdata3() {
    // <title> is RCDATA so this should be possible
    precise("<title>1&lt;<![CDATA[2&lt;]><![CDATA[3<]]>4&gt;5</title>");
    assertRender(
        "<title>1&lt;2&amp;lt;3&lt;4&gt;5</title>",
        "<title>1&lt;<![CDATA[2&lt;]]><![CDATA[3<]]>4&gt;5</title>");
    assertRender(
        "<title>1&lt;&lt;![CDATA[2&lt;]]&gt;&lt;![CDATA[3&lt;]]&gt;4&gt;5</title>",
        "<title>1&lt;&lt;![CDATA[2&lt;]]>&lt;![CDATA[3&lt;]]>4&gt;5</title>");
  }

  /*TODO(felix8a)*/ @FailureIsAnOption
  public final void testCdata4() {
    // <xmp> is RAWTEXT, no entity decoding, so precise is impossible
    robust("<xmp>1&lt;<![CDATA[2&lt;]><![CDATA[3<]]>4&gt;5</xmp>");
  }

  public final void testXmp1() {
    // xmp can never contain "</xmp>", and there's no way to escape it.
    xmlOnly(xml("<xmp>1&lt;/xmp>2</xmp>"));
  }

  public final void testXmp2() {
    // non-ascii characters are not renderable safely in xmp
    xmlOnly(xml("<xmp>\u0000\u0080\u0131</xmp>"));
    assertRenderXml(
        "<xmp>&#0;&#128;&#305;</xmp>",
        "<xmp>\u0000\u0080\u0131</xmp>");
  }

  /*TODO(felix8a)*/ @FailureIsAnOption
  public final void testXmp3() {
    robust(html("<xmp> a<b </xmp>"));
  }

  public final void testScript1() {
    precise("<script> a </script>");
    assertRender(
        "<script> a </script>",
        "<script> a </script>");
  }

  /*TODO(felix8a)*/ @FailureIsAnOption
  public final void testScript2() {
    precise(html(
        "<script>//!<[CDATA[\n"
        + "a<b;\n"
        + "//]]></script>"));
  }

  /*TODO(felix8a)*/ @FailureIsAnOption
  public final void testScript3() {
    htmlOnly(html(
        "<script> a<b; </script>"));
  }

  /*TODO(felix8a)*/ @FailureIsAnOption
  public final void testScript4() {
    unsafe(xml("<script> '&lt;/script>' </script>"));
  }

  // --------

  private void assertRenderHtml(String expected, String test) {
    assertEquals(expected, renderHtml(xml(test)));
  }

  private void assertRenderXml(String expected, String test) {
    assertEquals(expected, renderXml(xml(test)));
  }

  private void assertRender(String expected, String test) {
    Node tree = xml(test);
    assertEquals(expected, renderHtml(tree));
    assertEquals(expected, renderXml(tree));
  }

  /**
   * Check that an output string has the same meaning, whether it's parsed
   * as HTML or as XML.
   */
  private void preciseOutput(String output) {
    // TODO(felix8a): parser warnings here should be fatal?
    Node asHtml = html(output);
    Node asXml = xml(output);
    assertIdenticalRender(asHtml, asXml);
    assertIdenticalStructure(asHtml, asXml);
  }

  /**
   * Check that our rendering of a DOM tree has the same meaning, whether
   * the rendering is parsed as HTML or as XML.
   */
  private void precise(Node tree) {
    preciseOutput(Nodes.render(tree));
    preciseOutput(Nodes.render(tree, MarkupRenderMode.HTML));
    preciseOutput(Nodes.render(tree, MarkupRenderMode.XML));
    // We're only making this guarantee for the common render options.
  }

  private void precise(String input) {
    // Note, parser warnings here are unimportant. TODO(felix8a): really?
    precise(html(input));
    precise(xml(input));
  }

  // --------

  /**
   * Check that our rendering of a DOM tree has the same structure, whether
   * the rendering is parsed as HTML or as XML.
   * <p>
   * Differences in text content are ignored, but differences in attributes
   * and attribute values are significant.
   * <p>
   * Basically, we want to make sure that when an HTML output string is
   * interpreted as XML or vice versa, that the misinterpretation does not
   * turn inactive text into active script.
   */
  private void robust(Node tree) {
    String asHtml = renderHtml(tree);
    assertIdenticalStructure(tree, html(asHtml));
    assertIdenticalStructure(tree, xml(asHtml));
    String asXml = renderXml(tree);
    assertIdenticalStructure(tree, html(asXml));
    assertIdenticalStructure(tree, xml(asXml));
  }

  private void robust(String source) {
    robust(html(source));
    robust(xml(source));
  }

  // --------

  /**
   * Check that when we refuse to render a DOM tree as XML, we can
   * render it as HTML, and the HTML rendering is safe when misinterpreted
   * as XML.
   */
  private void htmlOnly(Node tree) {
    try {
      String result = renderXml(tree);
      throw new AssertionFailedError(
          "Unexpected renderXmlsuccess: " + result);
    } catch (UncheckedUnrenderableException e) {}
    String result = renderHtml(tree);
    assertIdenticalStructure(html(result), xml(result));
  }

  /**
   * Check that when we refuse to render a DOM tree as HTML, we can
   * render it as XML, and the XML rendering is safe when misinterpreted
   * as HTML.
   */
  private void xmlOnly(Node tree) {
    try {
      String result = renderHtml(tree);
      throw new AssertionFailedError(
          "Unexpected renderHtml success: " + result);
    } catch (UncheckedUnrenderableException e) {}
    String result = renderXml(tree);
    assertIdenticalStructure(html(result), xml(result));
  }

  /**
   * Check that when we refuse to render a DOM tree as HTML or as XML
   */
  private void unsafe(Node tree) {
    try {
      String result = renderHtml(tree);
      throw new AssertionFailedError(
          "Unexpected renderHtml success: " + result);
    } catch (UncheckedUnrenderableException e) {}
    try {
      String result = renderXml(tree);
      throw new AssertionFailedError(
          "Unexpected renderXmlsuccess: " + result);
    } catch (UncheckedUnrenderableException e) {}
  }

  // --------

  private void assertIdenticalRender(Node n1, Node n2) {
    String s1 = renderXml(n1);
    String s2 = renderXml(n2);
    assertEquals(s1, s2);
  }

  private void assertIdenticalStructure(Node n1, Node n2) {
    assertEquals(n1.getNodeType(), n2.getNodeType());
    switch (n1.getNodeType()) {
      case Node.ELEMENT_NODE:
        assertIdenticalStructure((Element) n1, (Element) n2);
        break;
      case Node.DOCUMENT_FRAGMENT_NODE:
        assertIdenticalStructure(n1.getChildNodes(), n2.getChildNodes());
        break;
      default:
        throw new IllegalStateException("Unexpected node type");
    }
  }

  // These are HTML tags whose contents have security implications.
  private static final Set<String> RISKY_ELEMENTS = ImmutableSet.of(
      "noembed", "noframes", "noscript", "script", "style");

  private void assertIdenticalStructure(Element e1, Element e2) {
    assertEquals(e1.getTagName(), e2.getTagName());

    // For RISKY_ELEMENTS, text equivalence is important.
    String tagName = e1.getTagName().toLowerCase();
    if (RISKY_ELEMENTS.contains(tagName)) {
      assertIdenticalRender(e1, e2);
      return;
    }

    // Check that attributes are identical
    NamedNodeMap a1 = e1.getAttributes();
    NamedNodeMap a2 = e2.getAttributes();
    assertEquals(a1.getLength(), a2.getLength());
    for (int i = 0; i < a1.getLength(); i++) {
      Node attr = a1.item(i);
      assertEquals(attr.getNodeValue(), a2.getNamedItem(attr.getNodeName()));
    }

    // Check that children are identical, ignoring unimportant text.
    assertIdenticalStructure(e1.getChildNodes(), e2.getChildNodes());
  }

  private void assertIdenticalStructure(NodeList nl1, NodeList nl2) {
    int p1 = nextElement(nl1, 0);
    int p2 = nextElement(nl2, 0);
    while (p1 < nl1.getLength() && p2 < nl2.getLength()) {
      assertIdenticalStructure((Element) nl1.item(p1), (Element) nl2.item(p2));
      p1 = nextElement(nl1, p1 + 1);
      p2 = nextElement(nl2, p2 + 1);
    }
    assertTrue(nl1.getLength() <= nextElement(nl1, p1));
    assertTrue(nl2.getLength() <= nextElement(nl2, p2));
  }

  // Return the next element node at or after pos
  private int nextElement(NodeList nl, int pos) {
    while (pos < nl.getLength()) {
      switch (nl.item(pos).getNodeType()) {
        case Node.CDATA_SECTION_NODE:
        case Node.TEXT_NODE:
          pos++;
          break;
        case Node.ELEMENT_NODE:
          return pos;
        default:
          throw new IllegalStateException("Unexpected node type");
      }
    }
    return nl.getLength();
  }

  private DocumentFragment html(String source) {
    try {
      return htmlFragment(fromString(source));
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  private DocumentFragment xml(String source) {
    try {
      return xmlFragment(fromString(source));
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  private String renderXml(Node node) {
    return Nodes.render(node, MarkupRenderMode.XML);
  }

  private String renderHtml(Node node) {
    return Nodes.render(node, MarkupRenderMode.HTML);
  }

}
