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

import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.parser.html.Nodes;
import com.google.caja.reporting.MarkupRenderMode;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.MoreAsserts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Node;

public class TemplateSanitizerTest extends CajaTestCase {
  public final void testSingleElement() throws Exception {
    assertValid(htmlFragment(fromString("<br/>")), "<br />");
  }
  public final void testText() throws Exception {
    assertValid(htmlFragment(fromString("Hello World")), "Hello World");
  }
  public final void testFormattingElement() throws Exception {
    assertValid(htmlFragment(fromString("<b>Hello</b>")), "<b>Hello</b>");
  }
  public final void testUnknownAttribute() throws Exception {
    assertValid(
        htmlFragment(fromString("<b unknown=\"bogus\">Hello</b>")),
        "<b data-caja-unknown=\"bogus\">Hello</b>");
  }
  public final void testKnownAttribute() throws Exception {
    assertValid(htmlFragment(fromString("<b id=\"bold\">Hello</b>")),
                "<b id=\"bold\">Hello</b>");
  }
  public final void testUnknownElement() throws Exception {
    assertValid(
        htmlFragment(fromString("<bogus id=\"bold\">Hello</bogus>")),
        "Hello",
        "WARNING: removing unknown tag bogus",
        "WARNING: removing attribute id when folding bogus into parent");
  }
  public final void testUnknownEverything() throws Exception {
    assertValid(
        htmlFragment(fromString("<bogus unknown=\"bogus\">Hello</bogus>")),
        "Hello",
        "WARNING: removing unknown tag bogus",
        "WARNING: removing attribute unknown when folding bogus into parent"
        );
  }
  public final void testDisallowedScriptElement() throws Exception {
    assertValid(
        htmlFragment(fromString("<script>disallowed</script>")),
        "disallowed",
        "WARNING: removing disallowed tag script");
    assertValid(
        htmlFragment(fromString(
            "<script src=http://can-link-to.com/ >disallowed</script>")),
        "disallowed",
        "WARNING: removing disallowed tag script");
  }
  public final void testDisallowedAppletElement() throws Exception {
    assertValid(
        htmlFragment(fromString(
            ""
            + "<applet><param name=zoicks value=ack>"
            + "<a href=http://can-link-to.com/ >disallowed</a></applet>")),
        "<a href=\"http://can-link-to.com/\">disallowed</a>",
        "WARNING: removing disallowed tag applet",
        "WARNING: removing disallowed tag param");
  }
  public final void testDisallowedBaseElement() throws Exception {
    assertValid(
        htmlFragment(fromString(
            "<base href='http://can-link-to.com/'>disallowed")),
        "disallowed",
        "WARNING: removing disallowed tag base");
  }
  public final void testDisallowedBasefontElement() throws Exception {
    assertValid(
        htmlFragment(fromString("<basefont size=4>disallowed")),
        "disallowed",
        "WARNING: removing disallowed tag basefont");
  }
  public final void testDisallowedFrameElement() throws Exception {
    assertValid(
        htmlFragment(fromString(
            ""
            + "<frameset><frame src='http://can-link-to.com/'>"
            + "disallowed</frame></frameset>")),
        "",
        "WARNING: removing disallowed tag frameset",
        "WARNING: removing disallowed tag frame");
    // Frames outside framesets are thrown out by the parser.
    assertValid(
        htmlFragment(fromString(
            "<frame src='http://can-link-to.com/'>disallowed</frame>")),
        "disallowed");
  }
  public final void testDisallowedFramesetElement() throws Exception {
    assertValid(
        htmlFragment(fromString("<frameset>disallowed</frameset>")),
        "",
        "WARNING: removing disallowed tag frameset");
  }
  public final void testDisallowedIframeElement() throws Exception {
    assertValid(
        htmlFragment(fromString(
            ""
            + "<iframe src='http://can-link-to.com/'"
            + " name='foo' id='bar' width=3>"
            + "disallowed</iframe>")),
        "<iframe width=\"3\">disallowed</iframe>",
        "WARNING: removing disallowed attribute src on tag iframe",
        "WARNING: removing disallowed attribute name on tag iframe",
        "WARNING: removing disallowed attribute id on tag iframe");
  }
  public final void testIsindexElementRewrittenSafely() throws Exception {
    assertValid(
        htmlFragment(fromString("<isindex name=foo>rewritten")),
        ""
        + "<form><hr /><p><label>This is a searchable index."
        + " Insert your search keywords here:"
        + " <input name=\"isindex\" /></label></p><hr /></form>rewritten");
  }
  public final void testDisallowedLinkElement() throws Exception {
    assertValid(
        htmlFragment(fromString(
            "<link rev=Contents href='http://can-link-to.com/'>disallowed")),
        "disallowed",
        "WARNING: removing disallowed tag link");
  }
  public final void testDisallowedMetaElement() throws Exception {
    assertValid(
        htmlFragment(fromString(
            ""
            + "<meta http-equiv='refresh'"
            + " content='5;url=http://can-link-to.com/'>"
            + "disallowed")),
        "disallowed",
        "WARNING: removing disallowed tag meta");
  }
  public final void testDisallowedObjectElement() throws Exception {
    assertValid(
        htmlFragment(fromString(
            "<object><param name=zoicks value=ack>disallowed</object>")),
        "disallowed",
        "WARNING: removing disallowed tag object",
        "WARNING: removing disallowed tag param");
  }
  public final void testDisallowedStyleElement() throws Exception {
    assertValid(
        htmlFragment(fromString(
            "<style>p { color: expression(disallowed()) }</style>")),
        "p { color: expression(disallowed()) }",
        "WARNING: removing disallowed tag style");
  }
  public final void testVirtualizedTitleElement() throws Exception {
    assertValid(
        htmlFragment(fromString(
            "<title>virtualized</title>")),
        "<caja-v-title>virtualized</caja-v-title>");
  }
  public final void testAttributeValidity() throws Exception {
    assertValid(
        htmlFragment(fromString("<form><input type=text></form>")),
        "<form><input type=\"text\" /></form>");
  }
  public final void testAttributePatternsTagSpecific() throws Exception {
    assertValid(
        htmlFragment(fromString("<input type=text>")),
        "<input type=\"text\" />");
    assertValid(
        htmlFragment(fromString("<button type=submit>")),
        "<button type=\"submit\"></button>");
    assertValid(
        htmlFragment(fromString("<BUTTON TYPE=SUBMIT>")),
        "<button type=\"SUBMIT\"></button>");
    assertValid(
        htmlFragment(fromString("<button type=text>")),
        "<button></button>",
        "WARNING: attribute type cannot have value text");
    assertValid(
        htmlFragment(fromString("<BUTTON TYPE=TEXT>")),
        "<button></button>",
        "WARNING: attribute type cannot have value TEXT");
  }
  public final void testIllegalAttributeValue() throws Exception {
    assertValid(
        htmlFragment(fromString("<form><input type=x></form>")),
        "<form><input /></form>",
        "WARNING: attribute type cannot have value x");
  }
  public final void testDisallowedElement2() throws Exception {
    assertValid(
        htmlFragment(fromString("<xmp>disallowed</xmp>")),
        "disallowed",
        "WARNING: removing unknown tag xmp");
  }
  public final void testDisallowedElement3() throws Exception {
    assertValid(
        htmlFragment(fromString("<meta http-equiv='refresh' content='1'/>")),
        "",
        "WARNING: removing disallowed tag meta");
  }
  public final void testElementVirtualization1() throws Exception {
    assertValid(
        xmlFragment(fromString("<title>A title</title>")),
        "<caja-v-title>A title</caja-v-title>");
  }
  public final void testElementVirtualization2() throws Exception {
    assertValid(
        xmlFragment(fromString("<body bgcolor=\"red\">Zoicks</body>")),
        "<caja-v-body bgcolor=\"red\">Zoicks</caja-v-body>");
  }
  public final void testElementVirtualizationAndFolding1() throws Exception {
    assertValid(
        xmlFragment(fromString(
            "<html>"
            + "<head>"
            + "<title>Blah</title>"
            + "<p>Foo</p>"
            + "</head>"
            + "<body>"
            + "<p>One</p>"
            + "<p styleo=\"color: red\">Two</p>"
            + "Three"
            + "<x>Four</x>"
            + "</body>"
            + "</html>")),
        "<caja-v-html><caja-v-head><caja-v-title>Blah</caja-v-title>" +
            "<p>Foo</p></caja-v-head><caja-v-body><p>One</p>" +
            "<p data-caja-styleo=\"color: red\">Two" +
            "</p>ThreeFour" +
            "</caja-v-body></caja-v-html>",
        "WARNING: removing unknown tag x");
  }
  public final void testElementVirtualizationAndFolding2() throws Exception {
    assertValid(
        xmlFragment(fromString(
            "<html>"
            + "<head>"
            + "<title>Blah</title>"
            + "<p>Foo</p>"
            + "</head>"
            + "<body>"
            + "<p>One</p>"
            + "<p>Two</p>"
            + "Three"
            + "<p>Four</p>"
            + "</body>"
            + "</html>")),
        "<caja-v-html><caja-v-head><caja-v-title>Blah</caja-v-title>" +
            "<p>Foo</p></caja-v-head><caja-v-body>" +
            "<p>One</p><p>Two</p>Three<p>Four</p>" +
            "</caja-v-body></caja-v-html>");
  }
  public final void testAttrsOnVirtualizedElement() throws Exception {
    // confirming that (multiple) attributes on virtualized elements are
    // preserved, and that attrs are sanitized
    assertValid(
        htmlFragment(fromString(
            "<html alpha='a' beta='b'>"
            + "<body alpha='a' background='#bbb'></body></html>")),
        "<caja-v-html data-caja-alpha=\"a\" data-caja-beta=\"b\">" +
            "<caja-v-head></caja-v-head>" + 
            "<caja-v-body background=\"#bbb\" data-caja-alpha=\"a\">" +
            "</caja-v-body></caja-v-html>");
  }
  public final void testIgnoredElement() throws Exception {
    assertValid(
        htmlFragment(fromString(
             "<p>Foo"
             + "<noscript>ignorable</noscript>"
             + "<p>Bar")),
        "<p>Foo</p><p>Bar</p>",
        "WARNING: removing disallowed tag noscript");
  }
  public final void testDupeAttrs() throws Exception {
    assertValid(
        xmlFragment(fromString(
            "<font color=\"red\" color=\"blue\">Purple</font>")),
        //         ^^^^^
        //            1
        //   123456789012
        "<font color=\"red\">Purple</font>",
        "WARNING: attribute color duplicates one at testDupeAttrs:1+7 - 12");
  }
  public final void testDisallowedAttrs() throws Exception {
    assertValid(
        htmlFragment(fromString(
            "<a href=\"foo.html\" charset=\"utf-7\">foo</a>")),
        "<a href=\"foo.html\">foo</a>",
        "WARNING: removing disallowed attribute charset on tag a");
  }

  public final void testStrangeIds() throws Exception {
    String html =
      "<input name=\"tag[]\" />\n"
      + "<input name=\"form$location\" />\n"
      + "<span id=\"23skiddoo\">a</span>\n"
      + "<span id=\"8675309\">b</span>\n";
    assertValid(
        htmlFragment(fromString(html)),
        html);
  }

  public final void testStrangeTargets() throws Exception {
    String html = "<a target=\"foo\">a</a>";
    assertValid(
        htmlFragment(fromString(html)),
        html);
  }

  private void assertValid(Node input, String golden, String... warnings) {
    sanitize(input, golden, true, warnings);
  }

  private void sanitize(
      Node input, String golden, boolean valid, String... warnings) {
    boolean validated = new TemplateSanitizer(HtmlSchema.getDefault(mq), mq)
        .sanitize(input);

    List<String> actualWarnings = new ArrayList<String>();
    for (Message msg : mq.getMessages()) {
      if (MessageLevel.WARNING.compareTo(msg.getMessageLevel()) <= 0) {
        String msgText = msg.format(mc);
        msgText = msgText.substring(msgText.indexOf(": ") + 1);
        actualWarnings.add(msg.getMessageLevel().name() + ":" + msgText);
      }
    }
    mq.getMessages().clear();
    MoreAsserts.assertListsEqual(Arrays.asList(warnings), actualWarnings);

    assertEquals(valid, validated);

    if (golden != null) {
      assertEquals(golden, Nodes.render(input, MarkupRenderMode.XML));
    }
  }
}
