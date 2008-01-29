// Copyright (C) 2006 Google Inc.
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
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Criterion;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;

import junit.framework.TestCase;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public class GxpValidatorTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testValidate() throws Exception {
    validate("<br/>", true);
    validate("<b>Hello</b>", true);
    validate("<b unknown=\"bogus\">Hello</b>", false);
    validate("<b id=\"bold\">Hello</b>", true);
    validate("<bogus id=\"bold\">Hello</bogus>", false);
    validate("<bogus unknown=\"bogus\">Hello</bogus>", false);
    // TODO(ihab): Remove this and allow client custom whitelists.
    // See class HtmlWhitelist for details.
    if (false) { validate("<script>disallowed</script>", false); }
    validate("<b><gxp:attr name=\"id\">hi</gxp:attr>Hello</b>", true);
    validate("<b expr:id=\"yo()\">Hello</b>", true);
    validate("<b><gxp:attr name=\"bogus\">hi</gxp:attr>Hello</b>", false);
    validate("<b expr:bogus=\"yo()\">Hello</b>", false);
    validate("<b><gxp:attr>hi</gxp:attr>Hello</b>", false);
  }

  private void validate(String html, boolean valid) throws Exception {
    InputSource is = new InputSource(new URI("test:///"));
    StringReader sr = new StringReader(html);
    MessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(new OutputStreamWriter(System.err)),
        new MessageContext());
    HtmlLexer lexer = new HtmlLexer(CharProducer.Factory.create(sr, is));
    lexer.setTreatedAsXml(true);
    TokenQueue<HtmlTokenType> tq = new TokenQueue<HtmlTokenType>(
        lexer, is, Criterion.Factory.<Token<HtmlTokenType>>optimist());
    DomTree t = DomParser.parseDocument(
        tq, OpenElementStack.Factory.createXmlElementStack());
    assertEquals(html, valid,
                 new GxpValidator(mq).validate(new AncestorChain<DomTree>(t)));
  }
}
