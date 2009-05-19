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
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.Parser;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageTypeInt;
import com.google.caja.reporting.RenderContext;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import junit.framework.TestCase;

public abstract class CajaTestCase extends TestCase {
  protected InputSource is;
  protected MessageContext mc;
  protected MessageQueue mq;

  @Override
  protected void setUp() throws Exception {
    this.is = new InputSource(URI.create("test:///" + getName()));
    this.mc = new MessageContext();
    mc.addInputSource(is);
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
    this.mc.addInputSource(is);
    return CharProducer.Factory.create(new StringReader(content), is);
  }

  protected CharProducer fromResource(String resourcePath) throws IOException {
    URI uri = TestUtil.getResource(getClass(), resourcePath);
    if (uri == null) {
      throw new FileNotFoundException(resourcePath);
    }
    InputSource is = new InputSource(uri);
    CharProducer cp = TestUtil.getResourceAsProducer(getClass(), resourcePath);
    mc.addInputSource(is);
    return cp;
  }

  protected Block js(CharProducer cp) throws ParseException {
    return js(cp, false);
  }

  protected Expression jsExpr(CharProducer cp) throws ParseException {
    return jsExpr(cp, false);
  }

  protected Block js(CharProducer cp, boolean quasi) throws ParseException {
    return js(cp, JsTokenQueue.NO_COMMENT, quasi);
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
    JsTokenQueue tq = new JsTokenQueue(
        lexer, sourceOf(cp), JsTokenQueue.NO_COMMENT);
    Parser p = new Parser(tq, mq, quasi);
    Expression e = p.parseExpression(true);
    tq.expectEmpty();
    return e;
  }

  protected Block quasi(CharProducer cp) throws ParseException {
    return js(cp, true);
  }

  protected Element xml(CharProducer cp) throws ParseException {
    return (Element) parseMarkup(cp, true, true);
  }

  protected DocumentFragment xmlFragment(CharProducer cp) throws ParseException {
    return (DocumentFragment) parseMarkup(cp, true, false);
  }

  protected Element html(CharProducer cp) throws ParseException {
    return (Element) parseMarkup(cp, false, true);
  }

  protected DocumentFragment htmlFragment(CharProducer cp) throws ParseException {
    return (DocumentFragment) parseMarkup(cp, false, false);
  }

  private Node parseMarkup(CharProducer cp, boolean asXml, boolean asDoc)
      throws ParseException {
    InputSource is = sourceOf(cp);
    HtmlLexer lexer = new HtmlLexer(cp);
    lexer.setTreatedAsXml(asXml);
    TokenQueue<HtmlTokenType> tq = new TokenQueue<HtmlTokenType>(lexer, is);
    DomParser p = new DomParser(tq, asXml, mq);
    Node t = asDoc
        ? p.parseDocument()
        : p.parseFragment(DomParser.makeDocument(null, null));
    tq.expectEmpty();
    return t;
  }

  protected Element markup(CharProducer cp) throws ParseException {
    HtmlLexer lexer = new HtmlLexer(cp);
    DomParser p = new DomParser(lexer, sourceOf(cp), mq);
    return p.parseDocument();
  }

  protected DocumentFragment markupFragment(CharProducer cp)
      throws ParseException {
    HtmlLexer lexer = new HtmlLexer(cp);
    DomParser p = new DomParser(lexer, sourceOf(cp), mq);
    return p.parseFragment(DomParser.makeDocument(null, null));
  }

  protected CssTree.StyleSheet css(CharProducer cp) throws ParseException {
    return css(cp, false);
  }

  protected CssTree.StyleSheet css(CharProducer cp, boolean substs)
      throws ParseException {
    TokenQueue<CssTokenType> tq = cssTokenQueue(cp, substs);
    CssTree.StyleSheet ss = new CssParser(tq, mq, MessageLevel.FATAL_ERROR)
        .parseStyleSheet();
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
    CssTree.DeclarationGroup dg = new CssParser(
        tq, mq, MessageLevel.FATAL_ERROR).parseDeclarationGroup();
    tq.expectEmpty();
    return dg;
  }

  private TokenQueue<CssTokenType> cssTokenQueue(
      CharProducer cp, boolean substs) {
    return CssParser.makeTokenQueue(cp, mq, substs);
  }

  protected String render(ParseTreeNode node) {
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = node.makeRenderer(sb, null);
    node.render(new RenderContext(tc));
    tc.noMoreTokens();
    return sb.toString();
  }

  protected String minify(ParseTreeNode node) {
    // Make sure it's a JS node.
    StringBuilder sb = new StringBuilder();
    if (!(node.makeRenderer(sb, null) instanceof JsPrettyPrinter)) {
      throw new ClassCastException(node.getClass().getName());
    }
    TokenConsumer tc = new JsMinimalPrinter(new Concatenator(sb));
    node.render(new RenderContext(tc));
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
      MessageTypeInt type, MessageLevel level, MessagePart... expectedParts) {
    assertMessage(false, type, level, expectedParts);
  }

  protected void assertMessage(
      boolean consume, MessageTypeInt type, MessageLevel level,
      MessagePart... expectedParts) {
    Message closest = null;
    int closestScore = Integer.MIN_VALUE;
    for (Message msg : mq.getMessages()) {
      int score = 0;
      if (msg.getMessageType() == type) { ++score; }
      if (msg.getMessageLevel() == level) { ++score; }
      score -= partsMissing(msg, expectedParts);
      if (score == 2) {
        if (consume) {
          mq.getMessages().remove(msg);
        }
        return;
      }
      if (score > closestScore) {
        closest = msg;
        closestScore = score;
      }
    }
    if (closest == null) {
      fail("No message found of type " + type + " and level " + level);
    } else {
      fail("Failed to find message.  Closest match was " + closest.format(mc)
           + " with parts " + closest.getMessageParts());
    }
  }

  protected void assertNoMessage(MessageTypeInt type) {
    for (Message msg : mq.getMessages()) {
      if (msg.getMessageType() == type) { fail(msg.format(mc)); }
    }
  }

  private static int partsMissing(Message msg, MessagePart... parts) {
    int missing = 0;
    outerLoop:
    for (MessagePart expectedPart : parts) {
      for (MessagePart candidate : msg.getMessageParts()) {
        if (candidate.equals(expectedPart)) { continue outerLoop; }
      }
      ++missing;
    }
    return missing;
  }

  private InputSource sourceOf(CharProducer cp) {
    return cp.getSourceBreaks(0).source();
  }
}
