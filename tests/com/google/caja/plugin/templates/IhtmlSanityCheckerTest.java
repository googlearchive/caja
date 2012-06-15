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

package com.google.caja.plugin.templates;

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.html.AttribKey;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.ElKey;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.parser.html.Nodes;
import com.google.caja.reporting.MarkupRenderMode;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.CajaTestCase;

import java.io.StringReader;

import org.w3c.dom.Element;

public class IhtmlSanityCheckerTest extends CajaTestCase {

  public final void testEmptyTemplate() throws Exception {
    runTest(
        "<ihtml:template formals=\"\" name=\"hi\"></ihtml:template>",
        "<ihtml:template formals='' name='hi'/>");
  }
  public final void testHtmlInTemplate() throws Exception {
    runTest(
        "<ihtml:template formals=\"\" name=\"hi\"><p>Hi</p></ihtml:template>",
        "<ihtml:template formals=\"\" name=\"hi\"><p>Hi</p></ihtml:template>");
  }
  public final void testSimpleMessage() throws Exception {
    runTest(
        ""
        + "<ihtml:template formals=\"a\" name=\"t\">"
        + "<ihtml:message name=\"hi\">Hello</ihtml:message>"
        + "</ihtml:template>",
        ""
        + "<ihtml:template formals='a' name='t'>"
        + "<ihtml:message name='hi'>Hello</ihtml:message>"
        + "</ihtml:template>");
  }
  public final void testMultipleFormals() throws Exception {
    runTest(
        ""
        + "<ihtml:template formals=\"a b\" name=\"t\">"
        + "<ihtml:message name=\"hi\">Hello</ihtml:message>"
        + "</ihtml:template>",
        ""
        + "<ihtml:template formals='a b' name='t'>"
        + "<ihtml:message name='hi'>Hello</ihtml:message>"
        + "</ihtml:template>");
  }
  public final void testMultipleVars() throws Exception {
    runTest(
        "<ihtml:do vars=\"a _b\"></ihtml:do>",
        "<ihtml:do vars=\"a _b\" />");
  }
  public final void testUnnamedMessage() throws Exception {
    runTest(
        "<ihtml:template formals=\"a b\" name=\"t\"></ihtml:template>",
        ""
        + "<ihtml:template formals='a b' name='t'>"
        + "<ihtml:message>Hello</ihtml:message>"
        + "</ihtml:template>",
        new Message(IhtmlMessageType.MISSING_ATTRIB,
                    elKey("ihtml:message"), attrKey("ihtml:message", "name"),
                    FilePosition.instance(is, 1, 40, 40, 36))
        );
  }
  public final void testMisnamedMessage() throws Exception {
    runTest(
        "<ihtml:template formals=\"a b\" name=\"t\"></ihtml:template>",
        ""
        + "<ihtml:template formals=\"a b\" name=\"t\">"
        + "<ihtml:message name=\"x__\">"
        + "Hello"
        + "</ihtml:message>"
        + "</ihtml:template>",
        new Message(IhtmlMessageType.BAD_ATTRIB,
                    FilePosition.instance(is, 1, 55, 55, 10),
                    elKey("ihtml:message"), attrKey("ihtml:message", "name"),
                    MessagePart.Factory.valueOf("x__"))
        );
  }
  public final void testMessageWithPlaceholder() throws Exception {
    runTest(
        ""
        + "<ihtml:template formals=\"x\" name=\"t\">"
        + "<ihtml:message name=\"hi\">"
        + "Hello "
        + "<ihtml:ph name=\"planet\"></ihtml:ph>"
        + "<ihtml:dynamic expr=\"planet\"></ihtml:dynamic>"
        + "<ihtml:eph></ihtml:eph>!"
        + "</ihtml:message>"
        + "</ihtml:template>",
        ""
        + "<ihtml:template formals=\"x\" name=\"t\">"
        + "<ihtml:message name=\"hi\">"
        + "Hello "
        + "<ihtml:ph name=\"planet\"/>"
        + "<ihtml:dynamic expr=\"planet\"/>"
        + "<ihtml:eph/>!"
        + "</ihtml:message>"
        + "</ihtml:template>");
  }
  public final void testBadPlaceholderName() throws Exception {
    runTest(
        ""
        + "<ihtml:template formals=\"\" name=\"t\">"
        + "<ihtml:message name=\"hi\">"
        + "Hello "
        + "<ihtml:dynamic expr=\"planet\"></ihtml:dynamic>"
        + "!"
        + "</ihtml:message>"
        + "</ihtml:template>",
        ""
        + "<ihtml:template formals=\"\" name=\"t\">"
        + "<ihtml:message name=\"hi\">"
        + "Hello "
        + "<ihtml:ph name=\"if\"/>"
        + "<ihtml:dynamic expr=\"planet\"/>"
        + "<ihtml:eph/>!"
        + "</ihtml:message>"
        + "</ihtml:template>",
        new Message(
            IhtmlMessageType.BAD_ATTRIB,
            FilePosition.instance(is, 1, 78, 78, 9),
            elKey("ihtml:ph"), attrKey("ihtml:ph", "name"),
            MessagePart.Factory.valueOf("if")),
        new Message(
            IhtmlMessageType.ORPHANED_PLACEHOLDER_END,
            FilePosition.instance(is, 1, 119, 119, 12)));
  }
  public final void testUnnamedPlaceholder() throws Exception {
    runTest(
        ""
        + "<ihtml:template formals=\"\" name=\"t\">"
        + "<ihtml:message name=\"hi\">"
        + "Hello "
        + "<ihtml:dynamic expr=\"planet\"></ihtml:dynamic>"
        + "!"
        + "</ihtml:message>"
        + "</ihtml:template>",
        ""
        + "<ihtml:template formals=\"\" name=\"t\">"
        + "<ihtml:message name=\"hi\">"
        + "Hello "
        + "<ihtml:ph/>"
        + "<ihtml:dynamic expr=\"planet\"/>"
        + "<ihtml:eph/>!"
        + "</ihtml:message>"
        + "</ihtml:template>",
        new Message(
            IhtmlMessageType.MISSING_ATTRIB,
            elKey("ihtml:ph"),attrKey("ihtml:ph", "name"),
            FilePosition.instance(is, 1, 68, 68, 11)),
        new Message(
            IhtmlMessageType.ORPHANED_PLACEHOLDER_END,
            FilePosition.instance(is, 1, 109, 109, 12)));
  }
  public final void testNestedMessage() throws Exception {
    runTest(
        ""
        + "<ihtml:template formals=\"\" name=\"t\">"
        + "<ihtml:message name=\"hi\">"
        + "Hello "
        + "<ihtml:ph name=\"planet\"></ihtml:ph>"
        + "<ihtml:dynamic expr=\"planet\"></ihtml:dynamic>"
        + "<ihtml:eph></ihtml:eph>!"
        + "</ihtml:message>"
        + "</ihtml:template>",
        ""
        + "<ihtml:template formals=\"\" name=\"t\">"
        + "<ihtml:message name=\"hi\">"
        + "<ihtml:message name=\"there\" />"
        + "Hello "
        + "<ihtml:ph name=\"planet\"/>"
        + "<ihtml:dynamic expr=\"planet\"/>"
        + "<ihtml:eph/>!"
        + "</ihtml:message>"
        + "</ihtml:template>",
        new Message(
            IhtmlMessageType.NESTED_MESSAGE,
            FilePosition.instance(is, 1, 62, 62, 30),
            FilePosition.instance(is, 1, 37, 37, 145)));
  }
  public final void testUnclosedPlaceholder() throws Exception {
    runTest(
        ""
        + "<ihtml:template formals=\"\" name=\"t\">"
        + "<ihtml:message name=\"hi\">"
        + "Hello "
        + "<ihtml:dynamic expr=\"planet\"></ihtml:dynamic>!"
        + "</ihtml:message>"
        + "</ihtml:template>",
        ""
        + "<ihtml:template formals=\"\" name=\"t\">"
        + "<ihtml:message name=\"hi\">"
        + "Hello "
        + "<ihtml:ph name=\"planet\"/>"
        + "<ihtml:dynamic expr=\"planet\"/>!"
        + "</ihtml:message>"
        + "</ihtml:template>",
        new Message(
            IhtmlMessageType.UNCLOSED_PLACEHOLDER,
            FilePosition.instance(is, 1, 68, 68, 56)));
  }
  public final void testUnclosedPlaceholder2() throws Exception {
    runTest(
        ""
        + "<ihtml:template formals=\"\" name=\"t\">"
        + "<ihtml:message name=\"hi\">"
        + "Hello "
        + "<ihtml:dynamic expr=\"planet\"></ihtml:dynamic>"
        + "<ihtml:ph name=\"punc\"></ihtml:ph>"
        + "!"
        + "<ihtml:eph></ihtml:eph>"
        + "</ihtml:message>"
        + "</ihtml:template>",
        ""
        + "<ihtml:template formals=\"\" name=\"t\">"
        + "<ihtml:message name=\"hi\">"
        + "Hello "
        + "<ihtml:ph name=\"planet\"/>"
        + "<ihtml:dynamic expr=\"planet\"/>"
        + "<ihtml:ph name=\"punc\"/>"
        + "!"
        + "<ihtml:eph/>"
        + "</ihtml:message>"
        + "</ihtml:template>",
        new Message(
            IhtmlMessageType.UNCLOSED_PLACEHOLDER,
            // Ends before placeholder punc.
            FilePosition.instance(is, 1, 68, 68, 55)));
  }
  public final void testOrphanedPlaceholder() throws Exception {
    runTest(
        ""
        + "<ihtml:template formals=\"\" name=\"t\">"
        + "Hello "
        + "<ihtml:dynamic expr=\"planet\"></ihtml:dynamic>!"
        + "</ihtml:template>",
        ""
        + "<ihtml:template formals=\"\" name=\"t\">"
        + "Hello "
        + "<ihtml:ph name=\"planet\"/>"
        + "<ihtml:dynamic expr=\"planet\"/>!"
        + "<ihtml:eph/>"
        + "</ihtml:template>",
        new Message(
            IhtmlMessageType.ORPHANED_PLACEHOLDER,
            FilePosition.instance(is, 1, 43, 43, 25)),
        new Message(
            IhtmlMessageType.ORPHANED_PLACEHOLDER,
            FilePosition.instance(is, 1, 99, 99, 12)));
  }
  public final void testOrphanedPlaceholderEnd() throws Exception {
    runTest(
        ""
        + "<ihtml:template formals=\"\" name=\"t\">"
        + "Hello "
        + "<ihtml:dynamic expr=\"planet\"></ihtml:dynamic>!"
        + "</ihtml:template>",
        ""
        + "<ihtml:template formals=\"\" name=\"t\">"
        + "Hello "
        + "<ihtml:dynamic expr=\"planet\"/>!"
        + "<ihtml:eph/>"
        + "</ihtml:template>",
        new Message(
            IhtmlMessageType.ORPHANED_PLACEHOLDER,
            FilePosition.instance(is, 1, 74, 74, 12)));
  }
  public final void testIhtmlElementInMessageOutsidePlaceholder()
      throws Exception {
    runTest(
        ""
        + "<ihtml:template formals=\"\" name=\"t\"></ihtml:template>",
        ""
        + "<ihtml:template formals=\"\" name=\"t\">"
        + "<ihtml:message name='SayHowdy'>"
        + "Hello "
        + "<ihtml:dynamic expr=\"planet\"/>!"
        + "</ihtml:message>"
        + "</ihtml:template>",
        new Message(
            IhtmlMessageType.IHTML_IN_MESSAGE_OUTSIDE_PLACEHOLDER,
            FilePosition.instance(is, 1, 74, 74, 30),
            MessagePart.Factory.valueOf("dynamic")));
  }
  public final void testTemplateNames() throws Exception {
    runTest(
        "<ihtml:template formals=\"x\" name=\"hi\"></ihtml:template>",
        ""
        + "<ihtml:template formals='x' name='hi'>"
        + "<ihtml:template name='3nested' formals='a,,x,if,3' zoinks='ahoy'/>"
        + "</ihtml:template>",
        new Message(
            IhtmlMessageType.BAD_ATTRIB,
            FilePosition.instance(is, 1, 55, 55, 14),
            elKey("ihtml:template"), attrKey("ihtml:template", "name"),
            MessagePart.Factory.valueOf("3nested")),
        new Message(
            IhtmlMessageType.BAD_ATTRIB,
            FilePosition.instance(is, 1, 70, 70, 19),
            elKey("ihtml:template"), attrKey("ihtml:template", "formals"),
            MessagePart.Factory.valueOf("a,,x,if,3")),
        new Message(
            IhtmlMessageType.BAD_ATTRIB,
            FilePosition.instance(is, 1, 90, 90, 13),
            elKey("ihtml:template"), attrKey("ihtml:template", "zoinks"),
            MessagePart.Factory.valueOf("ahoy")));
  }
  public final void testCalls() throws Exception {
    runTest(
        ""
        + "<ihtml:template formals=\"\" name=\"hi\">\n"
        // TODO(mikesamuel): move parameters into a separate namespace so they
        // don't collide with the template param.
        + "  <ihtml:call baz=\"boo\" foo=\"bar\" template=\"bye\">"
          + "</ihtml:call>\n"
        + "</ihtml:template>",
        ""
        + "<ihtml:template name='hi' formals=''>\n"
        + "  <ihtml:call ihtml:template='bye' foo='bar' baz='boo'/>\n"
        + "</ihtml:template>");
  }
  public final void testBadCall() throws Exception {
    runTest(
        ""
        + "<ihtml:template formals=\"\" name=\"hi\">\n"
        + "  \n"
        + "</ihtml:template>",
        ""
        + "<ihtml:template name='hi' formals=''>\n"
        + "  <ihtml:call foo='bar' baz:boo='far'/>\n"
        + "</ihtml:template>",
        new Message(
            IhtmlMessageType.MISSING_ATTRIB,
            FilePosition.instance(is, 2, 41, 3, 37),
            elKey("ihtml:call"),
            attrKey("ihtml:call", "ihtml:template")),
        new Message(
            IhtmlMessageType.BAD_ATTRIB,
            FilePosition.instance(is, 2, 63, 25, 13),
            elKey("ihtml:call"),
            AttribKey.forAttribute(
                new Namespaces(Namespaces.XML_SPECIAL, "baz", "unknown:///baz"),
                elKey("ihtml:call"), "baz:boo"),
            MessagePart.Factory.valueOf("far")),
        new Message(
            MessageType.NO_SUCH_NAMESPACE,
            FilePosition.fromLinePositions(is, 2, 25, 2, 32),
            MessagePart.Factory.valueOf("baz"),
            MessagePart.Factory.valueOf("baz:boo"))
        );
  }
  public final void testMisplacedPlaceholderContent() throws Exception {
    runTest(
        ""
        + "<ihtml:template formals=\"\" name=\"t\">\n"
        + "  <ihtml:message name=\"m\">\n"
        + "    \n"
        + "    \n"
        + "  </ihtml:message>\n"
        + "</ihtml:template>",
        ""
        + "<ihtml:template formals=\"\" name=\"t\">\n"
        + "  <ihtml:message name=\"m\">\n"
        + "    <ihtml:ph name=\"p\">Hi</ihtml:ph>\n"
        + "    <ihtml:eph>There</ihtml:eph>\n"
        + "  </ihtml:message>\n"
        + "</ihtml:template>",
        new Message(
            IhtmlMessageType.INAPPROPRIATE_CONTENT,
            FilePosition.instance(is, 3, 88, 24, 2),
            MessagePart.Factory.valueOf("ph")),
        new Message(
            IhtmlMessageType.INAPPROPRIATE_CONTENT,
            FilePosition.instance(is, 4, 117, 16, 5),
            MessagePart.Factory.valueOf("eph")));
  }
  public final void testBadAttr() throws Exception {
    runTest(
        ""
        + "<ihtml:template formals=\"\" name=\"t\">\n"
        + "  \n"
        + "  <div><ihtml:element>p</ihtml:element></div>\n"
        + "</ihtml:template>",
        ""
        + "<ihtml:template formals=\"\" name=\"t\">\n"
        + "  <ihtml:element bogus=\"\">p</ihtml:element>\n"
        + "  <div><ihtml:element>p</ihtml:element></div>\n"
        + "</ihtml:template>",
        new Message(
            IhtmlMessageType.BAD_ATTRIB,
            FilePosition.instance(is, 2, 55, 18, 8),
            elKey("ihtml:element"), attrKey("ihtml:element", "bogus"),
            MessagePart.Factory.valueOf("")));
  }
  public final void testDynamicAttr() throws Exception {
    runTest(
        ""
        + "<ihtml:template formals=\"\" name=\"t\">\n"
        + "  <a>\n"
        + "    <ihtml:attribute name=\"href\">"
              + "<ihtml:dynamic expr=\"url\"></ihtml:dynamic>"
            + "</ihtml:attribute>\n"
        + "    <ihtml:attribute name=\"title\">\n"
        + "      <ihtml:message name=\"linkHover\">Howdy</ihtml:message>\n"
        + "    </ihtml:attribute>\n"
        + "    Link Text\n"
        + "    \n"
        + "  </a>\n"
        + "</ihtml:template>",
        ""
        + "<ihtml:template formals=\"\" name=\"t\">\n"
        + "  <a>\n"
        + "    <ihtml:attribute name=\"href\"><ihtml:dynamic expr=\"url\""
        + "     /></ihtml:attribute>\n"
        + "    <ihtml:attribute name=\"title\">\n"
        + "      <ihtml:message name=\"linkHover\">Howdy</ihtml:message>\n"
        + "    </ihtml:attribute>\n"
        + "    Link Text\n"
        + "    <ihtml:attribute>onclick=\"badness()\"</ihtml:attribute>\n"
        + "  </a>\n"
        + "</ihtml:template>",
        new Message(
            IhtmlMessageType.MISSING_ATTRIB,
            FilePosition.instance(is, 8, 264, 5, 54),
            elKey("ihtml:attribute"), attrKey("ihtml:attribute", "name")));
  }
  public final void testMisplacedElementAndAttribute() throws Exception {
    runTest(
        ""
        + "<ihtml:template formals=\"\" name=\"t\">\n"
        + "  <ihtml:element></ihtml:element>\n"
        + "  <ihtml:message name=\"m\">\n"
        + "    \n"
        + "    <ihtml:ph name=\"ph\"></ihtml:ph>\n"
        + "      \n"
        + "    <ihtml:eph></ihtml:eph>\n"
        + "  </ihtml:message>\n"
        + "  <ihtml:do init=\"maybe\">\n"
        + "    <ihtml:element></ihtml:element>\n"
        + "  <ihtml:else></ihtml:else>\n"
        + "    <ihtml:element></ihtml:element>\n"
        + "  </ihtml:do>\n"
        + "</ihtml:template>",
        ""
        + "<ihtml:template formals=\"\" name=\"t\">\n"
        + "  <ihtml:element/>\n"  // OK inside templates
        + "  <ihtml:message name=\"m\">\n"
        + "    <ihtml:element/>\n"  // But not inside messages
        + "    <ihtml:ph name=\"ph\"/>\n"
        + "      <ihtml:element/>\n"  // Not even inside messages
        + "    <ihtml:eph/>\n"
        + "  </ihtml:message>\n"
        + "  <ihtml:do init=\"maybe\">\n"
        + "    <ihtml:element/>\n"  // OK inside a conditional
        + "  <ihtml:else/>\n"
        + "    <ihtml:element/>\n"  // OK inside a conditional's alternate
        + "  </ihtml:do>\n"
        + "</ihtml:template>",
        new Message(
            IhtmlMessageType.MISPLACED_ELEMENT,
            FilePosition.instance(is, 4, 88, 5, 16),
            MessagePart.Factory.valueOf("ihtml:element"),
            MessagePart.Factory.valueOf("ihtml:message")),
        new Message(
            IhtmlMessageType.MISPLACED_ELEMENT,
            FilePosition.instance(is, 6, 137, 7, 16),
            MessagePart.Factory.valueOf("ihtml:element"),
            MessagePart.Factory.valueOf("ihtml:message")));
  }
  public final void testContentType() throws Exception {
    // The callingContext attribute is set by a later pass, so make sure it
    // can't be passed in.
    runTest(
        ""
        + "<ihtml:template formals=\"\" name=\"main\">\n"
        + "  \n"
        + "</ihtml:template>",
        ""
        + "<ihtml:template formals=\"\" name=\"main\">\n"
        + "  <ihtml:template formals=\"\" name=\"sub\"\n"
        + "   " + IHTML.CALLING_CONTEXT_ATTR + "=\"div\">\n"
        + "  </ihtml:template>\n"
        + "</ihtml:template>",
        new Message(
            IhtmlMessageType.BAD_ATTRIB,
            FilePosition.instance(is, 3, 4, 4, 20),
            elKey("ihtml:template"),
            attrKey("ihtml:template", "callingContext"),
            MessagePart.Factory.valueOf("div")));
  }

  private void runTest(
      String goldenIhtml, String inputIhtml, Message... expectedMessages)
      throws Exception {
    Element ihtmlRoot = new DomParser(
        DomParser.makeTokenQueue(
            FilePosition.startOfFile(is), new StringReader(inputIhtml), true,
            false),
        true, mq)
        .parseDocument();
    new IhtmlSanityChecker(mq).check(ihtmlRoot);

    for (Message msg : expectedMessages) {
      assertMessage(true, msg.getMessageType(), msg.getMessageLevel(),
                    msg.getMessageParts().toArray(new MessagePart[0]));
    }
    assertMessagesLessSevereThan(MessageLevel.WARNING);

    String checkedIhtml = Nodes.render(ihtmlRoot, MarkupRenderMode.XML);
    assertEquals(goldenIhtml, checkedIhtml);
  }

  static ElKey elKey(String qname) {
    return ElKey.forElement(Namespaces.HTML_DEFAULT, qname);
  }

  static AttribKey attrKey(String elQName, String aQName) {
    return AttribKey.forAttribute(
        Namespaces.HTML_DEFAULT,
        ElKey.forElement(Namespaces.HTML_DEFAULT, elQName), aQName);
  }
}
