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
        "<caja-v-bogus id=\"bold\">Hello</caja-v-bogus>");
  }
  public final void testUnknownEverything() throws Exception {
    assertValid(
        htmlFragment(fromString("<bogus unknown=\"bogus\">Hello</bogus>")),
        "<caja-v-bogus data-caja-unknown=\"bogus\">Hello</caja-v-bogus>");
  }
  // Disallowed elements are virtualized so that guest code sees the DOM it
  // expects. Disallowed attributes on allowed elements are removed.
  // TODO(kpreid): Why don't we virtualize disallowed attributes?
  public final void testDisallowedScriptElement() throws Exception {
    assertInvalid(
        htmlFragment(fromString("a<script>disallowed</script>b")),
        "a<script>disallowed</script>b",
        "ERROR: Disallowed element: script");
    assertInvalid(
        htmlFragment(fromString(
            "a<script src=http://can-link-to.com/ >disallowed</script>b")),
        "a<script src=\"http://can-link-to.com/\">disallowed</script>b",
        "ERROR: Disallowed element: script");
  }
  public final void testDisallowedStyleElement() throws Exception {
    assertInvalid(
        htmlFragment(fromString(
            "a<style>p { color: expression(disallowed()) }</style>b")),
        "a<style>p { color: expression(disallowed()) }</style>b",
        "ERROR: Disallowed element: style");
  }
  public final void testUnsafeAppletElement() throws Exception {
    assertValid(
        htmlFragment(fromString(
            ""
            + "<applet><param name=zoicks value=ack>"
            + "<a href=http://can-link-to.com/ >disallowed</a></applet>")),
        "<caja-v-applet><caja-v-param data-caja-value=\"ack\">"
        + "</caja-v-param>"
        + "<a href=\"http://can-link-to.com/\">disallowed</a>"
        + "</caja-v-applet>");
  }
  public final void testUnsafeBaseElement() throws Exception {
    assertValid(
        htmlFragment(fromString(
            "<base href='http://can-link-to.com/'>disallowed")),
        "<caja-v-base data-caja-href=\"http://can-link-to.com/\">"
        + "</caja-v-base>disallowed");
  }
  public final void testUnsafeBasefontElement() throws Exception {
    assertValid(
        htmlFragment(fromString("<basefont size=4>disallowed")),
        "<caja-v-basefont data-caja-size=\"4\"></caja-v-basefont>disallowed");
  }
  public final void testUnsafeFrameElement() throws Exception {
    // TODO(kpreid): Figure out where the text content is disappearing to.
    // We don't actually care about framesets, but there might be a bug.
    assertValid(
        htmlFragment(fromString(
            ""
            + "<frameset><frame data-caja-src='http://can-link-to.com/'>"
            + "disallowed</frame></frameset>")),
        "<caja-v-frameset><caja-v-frame "
            + "data-caja-src=\"http://can-link-to.com/\"></caja-v-frame>"
            + "</caja-v-frameset>");
    // Frames outside framesets are thrown out by the parser.
    assertValid(
        htmlFragment(fromString(
            "<frame src='http://can-link-to.com/'>disallowed</frame>")),
        "disallowed");
  }
  public final void testUnsafeFramesetElement() throws Exception {
    // TODO(kpreid): Figure out where the text content is disappearing to.
    // We don't actually care about framesets, but there might be a bug.
    assertValid(
        htmlFragment(fromString("<frameset>disallowed</frameset>")),
        "<caja-v-frameset></caja-v-frameset>");
  }
  public final void testUnsafeIframeElement() throws Exception {
    assertValid(
        htmlFragment(fromString(
            ""
            + "<iframe src='http://can-link-to.com/'"
            + " name='foo' id='bar' width=3>"
            + "disallowed</iframe>")),
        "<iframe width=\"3\""
        + ">disallowed</iframe>",
        "WARNING: removing disallowed attribute id on tag iframe",
        "WARNING: removing disallowed attribute name on tag iframe",
        "WARNING: removing disallowed attribute src on tag iframe");
  }
  public final void testIsindexElementRewrittenSafely() throws Exception {
    assertValid(
        htmlFragment(fromString("<isindex name=foo>rewritten")),
        ""
        + "<form><hr /><p><label>This is a searchable index."
        + " Insert your search keywords here:"
        + " <input name=\"isindex\" /></label></p><hr /></form>rewritten");
  }
  public final void testUnsafeLinkElement() throws Exception {
    assertValid(
        htmlFragment(fromString(
            "<link rev=Contents href='http://can-link-to.com/'>disallowed")),
        "<caja-v-link data-caja-href=\"http://can-link-to.com/\""
        + " data-caja-rev=\"Contents\">"
        + "</caja-v-link>disallowed");
  }
  public final void testUnsafeMetaElement() throws Exception {
    assertValid(
        htmlFragment(fromString(
            ""
            + "<meta http-equiv='refresh'"
            + " content='5;url=http://can-link-to.com/'>"
            + "disallowed")),
        "<caja-v-meta data-caja-content=\"5;url=http://can-link-to.com/\""
            + " data-caja-http-equiv=\"refresh\">"
            + "</caja-v-meta>disallowed");
  }
  public final void testUnsafeObjectElement() throws Exception {
    // TODO(kpreid): param ends up w/ unvirtualized name= because...?
    assertValid(
        htmlFragment(fromString(
            "<object><param name=zoicks value=ack>disallowed</object>")),
        "<caja-v-object>"
        + "<caja-v-param data-caja-value=\"ack\">"
        + "</caja-v-param>disallowed</caja-v-object>");
  }
  public final void testUnsafeTitleElement() throws Exception {
    assertValid(
        htmlFragment(fromString(
            "<title>disallowed</title>")),
        "<caja-v-title>disallowed</caja-v-title>");
  }
  public final void testUnsafeXmpElement() throws Exception {
    assertValid(
        htmlFragment(fromString("<xmp>disallowed</xmp>")),
        "<caja-v-xmp>disallowed</caja-v-xmp>");
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
  public final void testElementVirtualization1() throws Exception {
    assertValid(
        xmlFragment(fromString("<title>A title</title>")),
        "<caja-v-title>A title</caja-v-title>");
  }
  public final void testElementVirtualization2() throws Exception {
    assertValid(
        xmlFragment(fromString("<body bgcolor=\"red\">Zoicks</body>")),
        "<caja-v-body data-caja-bgcolor=\"red\">Zoicks</caja-v-body>");
  }
  public final void testElementVirtualization3() throws Exception {
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
            "</p>Three<caja-v-x>Four</caja-v-x>" +
            "</caja-v-body></caja-v-html>");
  }
  public final void testElementVirtualization4() throws Exception {
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
            "<caja-v-body data-caja-alpha=\"a\" data-caja-background=\"#bbb\">" +
            "</caja-v-body></caja-v-html>");
  }
  public final void testIgnoredElement() throws Exception {
    assertValid(
        htmlFragment(fromString(
             "<p>Foo"
             + "<noscript>ignorable</noscript>"
             + "<p>Bar")),
        "<p>Foo</p><p>Bar</p>",
        "WARNING: removing ignorable element noscript"
        );
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
  public final void testUnsafeAttrs() throws Exception {
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

  private void assertInvalid(Node input, String golden, String... warnings) {
    sanitize(input, golden, false, warnings);
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
