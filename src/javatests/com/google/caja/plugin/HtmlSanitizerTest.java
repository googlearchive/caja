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
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.html.OpenElementStack;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.MoreAsserts;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public class HtmlSanitizerTest extends TestCase {
  private static final PrintWriter err
      = new PrintWriter(new OutputStreamWriter(System.err));
  private InputSource is;
  private MessageContext mc;
  private MessageQueue mq;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    is = new InputSource(new URI("test:///" + getName()));
    mc = new MessageContext();
    mc.inputSources = Collections.singletonList(is);
    mq = new EchoingMessageQueue(err, mc);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    is = null;
    mc = null;
    mq = null;
    err.flush();
  }

  public void testSingleElement() throws Exception {
    assertValid(html("<br/>"), "<br />");
  }
  public void testText() throws Exception {
    assertValid(html("Hello World"), "Hello World");
  }
  public void testFormattingElement() throws Exception {
    assertValid(html("<b>Hello</b>"), "<b>Hello</b>");
  }
  public void testUnknownAttribute() throws Exception {
    assertInvalid(html("<b unknown=\"bogus\">Hello</b>"),
                  "ERROR: unknown attribute unknown on b");
  }
  public void testKnownAttribute() throws Exception {
    assertValid(html("<b id=\"bold\">Hello</b>"), "<b id=\"bold\">Hello</b>");
  }
  public void testUnknownElement() throws Exception {
    assertInvalid(html("<bogus id=\"bold\">Hello</bogus>"),
                  "ERROR: unknown tag bogus");
  }
  public void testUnknownEverything() throws Exception {
    assertInvalid(html("<bogus unknown=\"bogus\">Hello</bogus>"),
                  "ERROR: unknown tag bogus",
                  "ERROR: unknown attribute unknown on bogus");
  }
  public void testDisallowedElement() throws Exception {
    assertInvalid(html("<script>disallowed</script>"),
                  "ERROR: tag script is not allowed");
  }
  public void testAttributeValidity() throws Exception {
    assertValid(html("<form><input type=text></form>"),
                "<form><input type=\"text\" /></form>");
  }
  public void testAttributePatternsTagSpecific() throws Exception {
    assertValid(html("<input type=text>"), "<input type=\"text\" />");
    assertValid(html("<button type=submit>"),
                "<button type=\"submit\"></button>");
    assertValid(html("<BUTTON TYPE=SUBMIT>"),
                "<button type=\"SUBMIT\"></button>");
    assertInvalid(html("<button type=text>"),
                  "ERROR: attribute type cannot have value text");
    assertInvalid(html("<BUTTON TYPE=TEXT>"),
                  "ERROR: attribute type cannot have value TEXT");
  }
  public void testIllegalAttributeValue() throws Exception {
    assertInvalid(html("<form><input type=x></form>"),
                  "ERROR: attribute type cannot have value x");
  }
  public void testDisallowedElement2() throws Exception {
    assertInvalid(html("<xmp>disallowed</xmp>"), "ERROR: unknown tag xmp");
  }
  public void testDisallowedElement3() throws Exception {
    assertInvalid(html("<meta http-equiv='refresh' content='1'/>"),
                  "ERROR: tag meta is not allowed");
  }
  public void testDisallowedElement4() throws Exception {
    assertValid(xml("<title>A title</title>"), "",
                "WARNING: tag title is not allowed");
  }
  public void testElementFolding1() throws Exception {
    assertInvalid(xml("<body bgcolor=\"red\">Zoicks</body>"),
                  "WARNING: folding element body into parent",
                  "ERROR: cannot fold attribute bgcolor on body into parent");
  }
  public void testElementFolding2() throws Exception {
    assertValid(xml("<body>Zoicks</body>"),
                "Zoicks", "WARNING: folding element body into parent");
  }
  public void testElementFolding3() throws Exception {
    assertInvalid(xml("<html>"
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
                      + "</html>"),
                  "WARNING: folding element html into parent",
                  "WARNING: folding element head into parent",
                  "WARNING: tag title is not allowed",
                  "WARNING: folding element body into parent",
                  "ERROR: unknown attribute styleo on p",
                  "ERROR: unknown tag x");
  }
  public void testElementFolding4() throws Exception {
    assertValid(xml("<html>"
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
                    + "</html>"),
                "<p>Foo</p><p>One</p><p>Two</p>Three<p>Four</p>",
                "WARNING: folding element html into parent",
                "WARNING: folding element head into parent",
                "WARNING: tag title is not allowed",
                "WARNING: folding element body into parent");
  }
  public void testIgnoredElement() throws Exception {
    assertValid(
        html("<p>Foo"
             + "<noscript>ignorable</noscript>"
             + "<p>Bar"),
        "<p>Foo</p><p>Bar</p>",
        "WARNING: tag noscript is not allowed");
  }
  public void testDupeAttrs() throws Exception {
    assertValid(
        xml("<font color=\"red\" color=\"blue\">Purple</font>"),
        "<font color=\"red\">Purple</font>",
        //     ^^^^^
        // 3456789012345678901234567890123
        //        1         2         3
        "WARNING: attribute color duplicates one at testDupeAttrs:1+7 - 12");
  }

  private void assertValid(DomTree input, String golden, String... warnings)
      throws Exception {
    sanitize(input, golden, true, warnings);
  }

  private void assertInvalid(DomTree input, String... warnings)
      throws Exception {
    sanitize(input, null, false, warnings);
  }

  private void sanitize(
      DomTree input, String golden, boolean valid, String... warnings)
      throws Exception {
    mq.getMessages().clear();
    boolean validated = new HtmlSanitizer(HtmlSchema.getDefault(mq), mq)
        .sanitize(new AncestorChain<DomTree>(input));

    List<String> actualWarnings = new ArrayList<String>();
    for (Message msg : mq.getMessages()) {
      if (MessageLevel.WARNING.compareTo(msg.getMessageLevel()) <= 0) {
        String msgText = msg.format(mc);
        msgText = msgText.substring(msgText.indexOf(": ") + 1);
        actualWarnings.add(msg.getMessageLevel().name() + ":" + msgText);
      }
    }
    MoreAsserts.assertListsEqual(Arrays.asList(warnings), actualWarnings);

    assertEquals(valid, validated);

    if (golden != null) {
      StringBuilder sb = new StringBuilder();
      input.render(new RenderContext(mc, sb));
      assertEquals(golden, sb.toString());
    }
  }

  private DomTree html(String html) throws ParseException {
    return parse(html, false);
  }

  private DomTree xml(String xml) throws ParseException {
    return parse(xml, true);
  }

  private DomTree parse(String markup, boolean asXml) throws ParseException {
    TokenQueue<HtmlTokenType> tq = DomParser.makeTokenQueue(
        is, new StringReader(markup), asXml);
    DomTree t = DomParser.parseFragment(
        tq, asXml ? OpenElementStack.Factory.createXmlElementStack()
                  : OpenElementStack.Factory.createHtml5ElementStack(mq));
    tq.expectEmpty();
    return t;
  }
}
