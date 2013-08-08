// Copyright (C) 2011 Google Inc.
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
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.Token;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Lists;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import java.util.List;

/**
 * Tests for Html5ElementStack.
 */
public class Html5ElementStackTest extends CajaTestCase {
  Html5ElementStack stack;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    DOMImplementationRegistry registry =
        DOMImplementationRegistry.newInstance();
    DOMImplementation domImpl = registry.getDOMImplementation(
        "XML 1.0 Traversal 2.0");

    String qname = "html";
    String systemId = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd";
    String publicId = "-//W3C//DTD XHTML 1.0 Transitional//EN";

    DocumentType documentType = domImpl.createDocumentType(
        qname, publicId, systemId);
    Document doc = domImpl.createDocument(null, null, documentType);
    mq = new SimpleMessageQueue();

    stack = new Html5ElementStack(doc, false, mq);
    stack.open(false);
  }

  // Helper method to create an attribute stub.
  private static AttrStub createAttrStub(String name, String value) {
    Token<HtmlTokenType> nameToken = Token.instance(
        name, HtmlTokenType.ATTRNAME, FilePosition.UNKNOWN);
    Token<HtmlTokenType> valueToken = Token.instance(
        value, HtmlTokenType.ATTRVALUE, FilePosition.UNKNOWN);
    return new AttrStub(nameToken, valueToken, value);
  }

  public final void testProcessTag() {
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

  public final void testProcessTagMalformedTagName() {
    Token<HtmlTokenType> start = Token.instance(
        "<img.jpg\"", HtmlTokenType.TAGBEGIN, FilePosition.UNKNOWN);
    Token<HtmlTokenType> end = Token.instance(
        ">", HtmlTokenType.ATTRVALUE, FilePosition.UNKNOWN);
    List<AttrStub> list = Lists.newArrayList();

    stack.processTag(start, end, list);

    assertEquals(1, mq.getMessages().size());
    assertMessage(MessageType.INVALID_TAG_NAME, MessageLevel.WARNING,
        FilePosition.UNKNOWN,
        MessagePart.Factory.valueOf("img.jpg\""));

    assertNull(stack.builderRootElement());
  }
}
