// Copyright (C) 2008 Google Inc.
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

package com.google.caja.plugin;

import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.parser.html.Nodes;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.MoreAsserts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Node;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public class HtmlSanitizerTest extends CajaTestCase {
  public void testSingleElement() throws Exception {
    assertValid(htmlFragment(fromString("<br/>")), "<br />");
  }
  public void testText() throws Exception {
    assertValid(htmlFragment(fromString("Hello World")), "Hello World");
  }
  public void testFormattingElement() throws Exception {
    assertValid(htmlFragment(fromString("<b>Hello</b>")), "<b>Hello</b>");
  }
  public void testUnknownAttribute() throws Exception {
    assertValid(htmlFragment(fromString("<b unknown=\"bogus\">Hello</b>")),
                "<b>Hello</b>",
                "WARNING: removing unknown attribute unknown on b");
  }
  public void testKnownAttribute() throws Exception {
    assertValid(htmlFragment(fromString("<b id=\"bold\">Hello</b>")),
                "<b id=\"bold\">Hello</b>");
  }
  public void testUnknownElement() throws Exception {
    assertValid(
        htmlFragment(fromString("<bogus id=\"bold\">Hello</bogus>")),
        "Hello",
        "WARNING: removing unknown tag bogus",
        "WARNING: removing attribute id when folding bogus into parent");
  }
  public void testUnknownEverything() throws Exception {
    assertValid(
        htmlFragment(fromString("<bogus unknown=\"bogus\">Hello</bogus>")),
        "Hello",
        "WARNING: removing unknown tag bogus",
        "WARNING: removing unknown attribute unknown on bogus");
  }
  public void testDisallowedElement() throws Exception {
    assertValid(htmlFragment(fromString("<script>disallowed</script>")),
                "disallowed",
                "WARNING: removing disallowed tag script");
  }
  public void testAttributeValidity() throws Exception {
    assertValid(htmlFragment(fromString("<form><input type=text></form>")),
                "<form><input type=\"text\" /></form>");
  }
  public void testAttributePatternsTagSpecific() throws Exception {
    assertValid(htmlFragment(fromString("<input type=text>")),
                "<input type=\"text\" />");
    assertValid(htmlFragment(fromString("<button type=submit>")),
                "<button type=\"submit\"></button>");
    assertValid(htmlFragment(fromString("<BUTTON TYPE=SUBMIT>")),
                "<button type=\"SUBMIT\"></button>");
    assertValid(htmlFragment(fromString("<button type=text>")),
                "<button></button>",
                "WARNING: attribute type cannot have value text");
    assertValid(htmlFragment(fromString("<BUTTON TYPE=TEXT>")),
                "<button></button>",
                "WARNING: attribute type cannot have value TEXT");
  }
  public void testIllegalAttributeValue() throws Exception {
    assertValid(htmlFragment(fromString("<form><input type=x></form>")),
                "<form><input /></form>",
                "WARNING: attribute type cannot have value x");
  }
  public void testDisallowedElement2() throws Exception {
    assertValid(htmlFragment(fromString("<xmp>disallowed</xmp>")),
                "disallowed",
                "WARNING: removing unknown tag xmp");
  }
  public void testDisallowedElement3() throws Exception {
    assertValid(
        htmlFragment(fromString("<meta http-equiv='refresh' content='1'/>")),
        "",
        "WARNING: removing disallowed tag meta",
        "WARNING: removing attribute content when folding meta into parent",
        "WARNING: removing attribute http-equiv when folding meta into parent");
  }
  public void testDisallowedElement4() throws Exception {
    assertValid(xmlFragment(fromString("<title>A title</title>")), "",
                "WARNING: removing disallowed tag title");
  }
  public void testElementFolding1() throws Exception {
    assertValid(
        xmlFragment(fromString("<body bgcolor=\"red\">Zoicks</body>")),
        "Zoicks",
        "WARNING: folding element body into parent",
        "WARNING: removing attribute bgcolor when folding body into parent");
  }
  public void testElementFolding2() throws Exception {
    assertValid(xmlFragment(fromString("<body>Zoicks</body>")),
                "Zoicks", "WARNING: folding element body into parent");
  }
  public void testElementFolding3() throws Exception {
    assertValid(xmlFragment(fromString(
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
                "<p>Foo</p><p>One</p><p>Two</p>ThreeFour",
                "WARNING: folding element html into parent",
                "WARNING: folding element head into parent",
                "WARNING: removing disallowed tag title",
                "WARNING: folding element body into parent",
                "WARNING: removing unknown attribute styleo on p",
                "WARNING: removing unknown tag x");
  }
  public void testElementFolding4() throws Exception {
    assertValid(xmlFragment(fromString(
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
                "<p>Foo</p><p>One</p><p>Two</p>Three<p>Four</p>",
                "WARNING: folding element html into parent",
                "WARNING: folding element head into parent",
                "WARNING: removing disallowed tag title",
                "WARNING: folding element body into parent");
  }
  public void testIgnoredElement() throws Exception {
    assertValid(
        htmlFragment(fromString(
             "<p>Foo"
             + "<noscript>ignorable</noscript>"
             + "<p>Bar")),
        "<p>Foo</p><p>Bar</p>",
        "WARNING: removing disallowed tag noscript");
  }
  public void testDupeAttrs() throws Exception {
    assertValid(
        xmlFragment(fromString(
            "<font color=\"red\" color=\"blue\">Purple</font>")),
        //         ^^^^^
        //            1
        //   123456789012
        "<font color=\"red\">Purple</font>",
        "WARNING: attribute color duplicates one at testDupeAttrs:1+7 - 12");
  }
  public void testDisallowedAttrs() throws Exception {
    assertValid(
        htmlFragment(fromString(
            "<a href=\"foo.html\" charset=\"utf-7\">foo</a>")),
        "<a href=\"foo.html\">foo</a>",
        "WARNING: removing disallowed attribute charset on tag a");
  }

  private void assertValid(Node input, String golden, String... warnings)
      throws Exception {
    sanitize(input, golden, true, warnings);
  }

  private void sanitize(
      Node input, String golden, boolean valid, String... warnings)
      throws Exception {
    boolean validated = new HtmlSanitizer(HtmlSchema.getDefault(mq), mq)
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
      assertEquals(golden, Nodes.render(input));
    }
  }
}
