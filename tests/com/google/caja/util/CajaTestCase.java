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

package com.google.caja.util;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.JsTokenType;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.Parser;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageTypeInt;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.LinkedHashSet;
import junit.framework.TestCase;

public abstract class CajaTestCase extends TestCase {
  protected InputSource is;
  protected MessageContext mc;
  protected MessageQueue mq;

  @Override
  protected void setUp() throws Exception {
    this.is = new InputSource(URI.create("test:///" + getName()));
    this.mc = new MessageContext();
    this.mc.inputSources = new LinkedHashSet<InputSource>();
    this.mq = TestUtil.createTestMessageQueue(this.mc);
  }

  @Override
  protected void tearDown() throws Exception {
    this.is = null;
    this.mc = null;
    this.mq = null;
  }

  protected CharProducer fromString(String content) {
    return fromString(content, is);
  }

  protected CharProducer fromString(String content, InputSource is) {
    this.mc.inputSources.add(is);
    return CharProducer.Factory.create(new StringReader(content), is);
  }

  protected CharProducer fromResource(String resourcePath) throws IOException {
    InputSource is = new InputSource(
        TestUtil.getResource(getClass(), resourcePath));
    CharProducer cp = TestUtil.getResourceAsProducer(getClass(), resourcePath);
    mc.inputSources.add(is);
    return cp;
  }

  protected Block js(CharProducer cp) throws ParseException {
    return js(cp, false);
  }

  protected Expression jsExpr(CharProducer cp) throws ParseException {
    return jsExpr(cp, false);
  }

  protected Block js(CharProducer cp, boolean quasi) throws ParseException {
    return js(cp, noJsComments(), quasi);
  }

  protected Block js(
      CharProducer cp, Criterion<Token<JsTokenType>> filt, boolean quasi)
      throws ParseException {
    JsLexer lexer = new JsLexer(cp);
    JsTokenQueue tq = new JsTokenQueue(lexer, sourceOf(cp), filt);
    Parser p = new Parser(tq, mq, quasi);
    Block b = p.parse();
    tq.expectEmpty();
    return b;
  }

  protected Expression jsExpr(CharProducer cp, boolean quasi)
      throws ParseException {
    JsLexer lexer = new JsLexer(cp);
    JsTokenQueue tq = new JsTokenQueue(lexer, sourceOf(cp), noJsComments());
    Parser p = new Parser(tq, mq, quasi);
    Expression e = p.parseExpression(true);
    tq.expectEmpty();
    return e;
  }

  private static Criterion<Token<JsTokenType>> noJsComments() {
    return new Criterion<Token<JsTokenType>>() {
          public boolean accept(Token<JsTokenType> t) {
            return t.type != JsTokenType.COMMENT;
          }
        };
  }

  protected Block quasi(CharProducer cp) throws ParseException {
    return js(cp, true);
  }

  protected DomTree xml(CharProducer cp) throws ParseException {
    return parseMarkup(cp, true, true);
  }

  protected DomTree xmlFragment(CharProducer cp) throws ParseException {
    return parseMarkup(cp, true, false);
  }

  protected DomTree html(CharProducer cp) throws ParseException {
    return parseMarkup(cp, false, true);
  }

  protected DomTree htmlFragment(CharProducer cp) throws ParseException {
    return parseMarkup(cp, false, false);
  }

  private DomTree parseMarkup(CharProducer cp, boolean asXml, boolean asDoc)
      throws ParseException {
    InputSource is = sourceOf(cp);
    HtmlLexer lexer = new HtmlLexer(cp);
    lexer.setTreatedAsXml(asXml);
    TokenQueue<HtmlTokenType> tq = new TokenQueue<HtmlTokenType>(lexer, is);
    DomParser p = new DomParser(tq, asXml, mq);
    DomTree t = asDoc ? p.parseDocument() : p.parseFragment();
    tq.expectEmpty();
    return t;
  }

  protected DomTree markup(CharProducer cp) throws ParseException {
    HtmlLexer lexer = new HtmlLexer(cp);
    DomParser p = new DomParser(lexer, sourceOf(cp), mq);
    return p.parseDocument();
  }

  protected DomTree markupFragment(CharProducer cp) throws ParseException {
    HtmlLexer lexer = new HtmlLexer(cp);
    DomParser p = new DomParser(lexer, sourceOf(cp), mq);
    return p.parseFragment();
  }

  protected CssTree.StyleSheet css(CharProducer cp) throws ParseException {
    return css(cp, false);
  }

  protected CssTree.StyleSheet css(CharProducer cp, boolean substs)
      throws ParseException {
    TokenQueue<CssTokenType> tq = cssTokenQueue(cp, substs);
    CssTree.StyleSheet ss = new CssParser(tq).parseStyleSheet();
    tq.expectEmpty();
    return ss;
  }

  protected CssTree.DeclarationGroup cssDecls(CharProducer cp)
      throws ParseException {
    return cssDecls(cp, false);
  }

  protected CssTree.DeclarationGroup cssDecls(CharProducer cp, boolean substs)
      throws ParseException {
    TokenQueue<CssTokenType> tq = cssTokenQueue(cp, substs);
    CssTree.DeclarationGroup dg = new CssParser(tq).parseDeclarationGroup();
    tq.expectEmpty();
    return dg;
  }

  private TokenQueue<CssTokenType> cssTokenQueue(
      CharProducer cp, boolean substs) {
    CssLexer lexer = new CssLexer(cp, substs);
    return new TokenQueue<CssTokenType>(
        lexer, cp.getCurrentPosition().source(),
        new Criterion<Token<CssTokenType>>() {
          public boolean accept(Token<CssTokenType> t) {
            return CssTokenType.SPACE != t.type
                && CssTokenType.COMMENT != t.type;
          }
        });
  }

  protected String render(ParseTreeNode node) {
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = node.makeRenderer(sb, null);
    node.render(new RenderContext(mc, tc));
    tc.noMoreTokens();
    return sb.toString();
  }

  protected void assertMessagesLessSevereThan(MessageLevel level) {
    for (Message msg : mq.getMessages()) {
      if (level.compareTo(msg.getMessageLevel()) <= 0) {
        fail(msg.format(mc));
      }
    }    
  }

  protected void assertNoErrors() {
    assertMessagesLessSevereThan(MessageLevel.ERROR);
  }

  protected void assertMessage(
      MessageTypeInt type,
      MessageLevel level) {
    for (Message msg : mq.getMessages()) {
      if (msg.getMessageType() == type && msg.getMessageLevel() == level) {
        return;
      }
    }
    fail("No message found of type " + type + " and level " + level);
  }

  private InputSource sourceOf(CharProducer cp) {
    return cp.getCurrentPosition().source();
  }
}
