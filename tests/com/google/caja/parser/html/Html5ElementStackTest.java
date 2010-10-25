package com.google.caja.parser.html;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.Token;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Lists;

import junit.framework.TestCase;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import java.util.List;

/**
 * Tests for Html5ElementStack.
 */
public class Html5ElementStackTest extends TestCase {
  DOMImplementation domImpl;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    DOMImplementationRegistry registry =
        DOMImplementationRegistry.newInstance();
    domImpl = registry.getDOMImplementation("XML 1.0 Traversal 2.0");
  }

  // Helper method to create an attribute stub.
  private AttrStub createAttrStub(String name, String value) {
    Token<HtmlTokenType> nameToken = Token.instance(
        name, HtmlTokenType.ATTRNAME, FilePosition.UNKNOWN);
    Token<HtmlTokenType> valueToken = Token.instance(
        value, HtmlTokenType.ATTRVALUE, FilePosition.UNKNOWN);
    return new AttrStub(nameToken, valueToken, value);
  }

  public final void testProcessTag() {
    String qname = "html";
    String systemId = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd";
    String publicId = "-//W3C//DTD XHTML 1.0 Transitional//EN";

    DocumentType documentType = domImpl.createDocumentType(
        qname, publicId, systemId);
    Document doc = domImpl.createDocument(null, null, documentType);

    SimpleMessageQueue mq = new SimpleMessageQueue();
    Html5ElementStack stack = new Html5ElementStack(doc, false, mq);
    stack.open(false);

    Token<HtmlTokenType> start = Token.instance(
        "<helloTag", HtmlTokenType.TAGBEGIN, FilePosition.UNKNOWN);
    Token<HtmlTokenType> end = Token.instance(
        ">", HtmlTokenType.ATTRVALUE, FilePosition.UNKNOWN);

    List<AttrStub> list = Lists.newArrayList(
        // Illegal attribute caught in maybeCreateAttributeNS.
        createAttrStub("hi~", "hello~"),

        // Illegal attribute caught in maybeCreateAttribute.
        createAttrStub("data:hi~", "hello~"),
        createAttrStub("xmlns:hi~", "hello~"),

        // No exception.
        createAttrStub("xmlns:", "hello~"),
        createAttrStub("data:", "hello~"),

        // Illegal attribute caught by error check in
        // Html5ElementStack.processTag code for disallowing overriding of the
        // default namespace.
        createAttrStub("xmlns", "http://xmlns/index.html"),

        // Valid attributes.
        createAttrStub("attr1", "value1"),
        createAttrStub("data:attr", "value2"),
        createAttrStub("xmlns:buffalo", "value3"));

    stack.processTag(start, end, list);

    assertEquals(4, mq.getMessages().size());
    final String TEST_NAME = "unknown:///unknown:0+0";
    assertEquals(TEST_NAME + ": ignoring token 'hi~'",
                 mq.getMessages().get(0).toString());
    assertEquals(TEST_NAME + ": ignoring token 'data:hi~'",
                 mq.getMessages().get(1).toString());
    assertEquals(TEST_NAME + ": ignoring token 'xmlns:hi~'",
                 mq.getMessages().get(2).toString());
    assertEquals(TEST_NAME + ": cannot override default XML namespace in HTML",
                 mq.getMessages().get(3).toString());

    Element e = (Element) stack.builderRootElement().getElementsByTagName(
        "hellotag").item(0);
    assertEquals(5, e.getAttributes().getLength());
    assertEquals("value1", e.getAttribute("attr1"));
    assertEquals("hello~", e.getAttribute(
        AttributeNameFixup.fixupNameFromQname("data:")));
    assertEquals("hello~", e.getAttribute(
        AttributeNameFixup.fixupNameFromQname("xmlns:")));
    assertEquals("value2", e.getAttribute(
        AttributeNameFixup.fixupNameFromQname("data:attr")));
    assertEquals("value3", e.getAttribute(
        AttributeNameFixup.fixupNameFromQname("xmlns:buffalo")));
  }
}
