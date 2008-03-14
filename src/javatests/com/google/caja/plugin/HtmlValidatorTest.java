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
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.Token;
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
import com.google.caja.util.Criterion;
import com.google.caja.util.MoreAsserts;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public class HtmlValidatorTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testSingleElement() throws Exception {
    assertValid("<br/>", "<br />");
  }
  public void testText() throws Exception {
    assertValid("Hello World", "Hello World");
  }
  public void testFormattingElement() throws Exception {
    assertValid("<b>Hello</b>", "<b>Hello</b>");
  }
  public void testUnknownAttribute() throws Exception {
    assertInvalid("<b unknown=\"bogus\">Hello</b>",
                  "FATAL_ERROR: unknown attribute unknown on b");
  }
  public void testKnownAttribute() throws Exception {
    assertValid("<b id=\"bold\">Hello</b>", "<b id=\"bold\">Hello</b>");
  }
  public void testUnknownElement() throws Exception {
    assertInvalid("<bogus id=\"bold\">Hello</bogus>",
                  "FATAL_ERROR: unknown tag bogus");
  }
  public void testUnknownEverything() throws Exception {
    assertInvalid("<bogus unknown=\"bogus\">Hello</bogus>",
                  "FATAL_ERROR: unknown tag bogus",
                  "FATAL_ERROR: unknown attribute unknown on bogus");
  }
  public void testDisallowedElement() throws Exception {
    assertInvalid("<script>disallowed</script>",
                  "FATAL_ERROR: tag script is not allowed");
  }
  public void testAttributeValidity() throws Exception {
    assertValid("<form><input type=text></form>",
                "<form><input type=\"text\" /></form>");
  }
  public void testAttributePatternsTagSpecific() throws Exception {
    assertValid("<input type=text>", "<input type=\"text\" />");
    assertValid("<button type=submit>", "<button type=\"submit\"></button>");
    assertValid("<BUTTON TYPE=SUBMIT>", "<button type=\"SUBMIT\"></button>");
    assertInvalid("<button type=text>",
                  "ERROR: attribute type cannot have value text");
    assertInvalid("<BUTTON TYPE=TEXT>",
                  "ERROR: attribute type cannot have value TEXT");
  }
  public void testIllegalAttributeValue() throws Exception {
    assertInvalid("<form><input type=x></form>",
                  "ERROR: attribute type cannot have value x");
  }
  public void testDisallowedElement2() throws Exception {
    assertInvalid("<xmp>disallowed</xmp>", "FATAL_ERROR: unknown tag xmp");
  }
  public void testDisallowedElement3() throws Exception {
    assertInvalid("<meta http-equiv='refresh' content='1'/>",
                  "FATAL_ERROR: tag meta is not allowed");
  }
  public void testDisallowedElement4() throws Exception {
    assertValid("<title>A title</title>", "",
                "WARNING: tag title is not allowed");
  }
  public void testDisallowedElement5() throws Exception {
    assertInvalid("<body bgcolor=\"red\">Zoicks</body>",
                  "FATAL_ERROR: tag html is not allowed",
                  "FATAL_ERROR: tag head is not allowed",
                  "FATAL_ERROR: tag body is not allowed");
  }
  public void testIgnoredElement() throws Exception {
    assertValid(
        "<p>Foo"
        + "<noscript>ignorable</noscript>"
        + "<p>Bar",
        "<p>Foo</p><p>Bar</p>",
        "WARNING: tag noscript is not allowed");
  }

  private void assertValid(String html, String golden, String... warnings)
      throws Exception {
    validate(html, golden, true, warnings);
  }

  private void assertInvalid(String html, String... warnings)
      throws Exception {
    validate(html, null, false, warnings);
  }

  private void validate(
      String html, String golden, boolean valid, String... warnings)
      throws Exception {
    InputSource is = new InputSource(new URI("test:///" + getName()));
    StringReader sr = new StringReader(html);
    MessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(new OutputStreamWriter(System.err)),
        new MessageContext());
    HtmlLexer lexer = new HtmlLexer(CharProducer.Factory.create(sr, is));
    lexer.setTreatedAsXml(false);
    TokenQueue<HtmlTokenType> tq = new TokenQueue<HtmlTokenType>(
        lexer, is, Criterion.Factory.<Token<HtmlTokenType>>optimist());
    DomTree t = DomParser.parseFragment(
        tq, OpenElementStack.Factory.createHtml5ElementStack(mq));

    boolean validated = new HtmlValidator(HtmlSchema.getDefault(mq), mq)
        .validate(new AncestorChain<DomTree>(t));

    MessageContext mc = new MessageContext();
    List<String> actualWarnings = new ArrayList<String>();
    for (Message msg : mq.getMessages()) {
      if (MessageLevel.WARNING.compareTo(msg.getMessageLevel()) <= 0) {
        String msgText = msg.format(mc);
        msgText = msgText.substring(msgText.indexOf(": ") + 1);
        actualWarnings.add(msg.getMessageLevel().name() + ":" + msgText);
      }
    }
    MoreAsserts.assertListsEqual(Arrays.asList(warnings), actualWarnings);

    assertEquals(html, valid, validated);

    if (golden != null) {
      StringBuilder sb = new StringBuilder();
      t.render(new RenderContext(mc, sb));
      assertEquals(html, golden, sb.toString());
    }
  }
}
