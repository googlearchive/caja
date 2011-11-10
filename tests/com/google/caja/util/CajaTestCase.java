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

import com.google.caja.AllTests;
import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.GuessContentType;
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
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageTypeInt;
import com.google.caja.reporting.RenderContext;

import java.awt.GraphicsEnvironment;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public abstract class CajaTestCase extends TestCase {
  protected InputSource is;
  protected MessageContext mc;
  protected MessageQueue mq;

  /**
   * For random tests we choose a seed by using a system property so that
   * failing random tests can be repeated.
   */
  protected static final long SEED = Long.parseLong(
      System.getProperty("junit.seed", "" + System.currentTimeMillis()));
  private static boolean dumpedSeed;
  @Override
  protected void setUp() throws Exception {
    synchronized (CajaTestCase.class) {
      if (!dumpedSeed) {
        dumpedSeed = true;
        // Make sure it shows up in the junit test runner trace.
        System.err.println("junit.seed=" + SEED);
      }
    }
    this.is = new InputSource(URI.create("test://example.org/" + getName()));
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

  protected CharProducer fromString(String... content) {
    return fromString(Join.join("\n", content), is);
  }

  protected CharProducer fromString(String content, InputSource is) {
    this.mc.addInputSource(is);
    return CharProducer.Factory.create(new StringReader(content), is);
  }

  protected CharProducer fromString(String content, FilePosition pos) {
    this.mc.addInputSource(is);
    return CharProducer.Factory.create(new StringReader(content), pos);
  }

  protected CharProducer fromResource(String resourcePath)
      throws IOException {
    URI resource = TestUtil.getResource(getClass(), resourcePath);
    if (resource == null) { throw new FileNotFoundException(resourcePath); }
    return fromResource(resourcePath, new InputSource(resource));
  }

  protected CharProducer fromResource(String resourcePath, InputSource is)
      throws IOException {
    return dataFromResource(resourcePath, is).getTextualContent();
  }

  protected FetchedData dataFromResource(String resourcePath, InputSource is)
      throws IOException {
    ContentType guess = GuessContentType.guess(null, resourcePath, null);
    InputStream in = TestUtil.getResourceAsStream(getClass(), resourcePath);
    mc.addInputSource(is);
    return FetchedData.fromStream(
        in, guess != null ? guess.mimeType : "", "UTF-8", is);
  }

  protected String plain(CharProducer cp) {
    return cp.toString(cp.getOffset(), cp.getLimit());
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
    TokenQueue<HtmlTokenType> tq = new TokenQueue<HtmlTokenType>(
        lexer, is, DomParser.SKIP_COMMENTS);
    DomParser p = new DomParser(tq, asXml, mq);
    Node t = asDoc ? p.parseDocument() : p.parseFragment();
    tq.expectEmpty();
    return t;
  }

  protected Element markup(CharProducer cp) throws ParseException {
    return new DomParser(new HtmlLexer(cp), false, sourceOf(cp), mq)
        .parseDocument();
  }

  protected DocumentFragment markupFragment(CharProducer cp)
      throws ParseException {
    return new DomParser(new HtmlLexer(cp), false, sourceOf(cp), mq)
        .parseFragment();
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

  public static String render(MessageQueue mq) {
    StringBuilder sb = new StringBuilder();
    for (Message m : mq.getMessages()) {
      try {
        m.format(new MessageContext(), sb);
      } catch (IOException e) {
        sb.append(e.toString());
      }
      sb.append("\n");
    }
    return sb.toString();
  }

  public static String render(ParseTreeNode node) {
    if (node == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = node.makeRenderer(sb, null);
    node.render(new RenderContext(tc));
    tc.noMoreTokens();
    return sb.toString();
  }

  /**
   * Returns a source code string for the given program without surrounding
   * curly braces.
   */
  public static String renderProgram(Block program) {
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = program.makeRenderer(sb, null);
    program.renderBody(new RenderContext(tc));
    tc.noMoreTokens();
    return sb.toString();
  }

  protected String formatShort(FilePosition p) {
    StringBuilder sb = new StringBuilder();
    try {
      p.formatShort(sb);
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError(
          "IOException from StringBuilder", ex);
    }
    return sb.toString();
  }

  protected String minify(ParseTreeNode node) {
    if (node == null) {
      return null;
    }
    // Make sure it's a JS node.
    StringBuilder sb = new StringBuilder();
    if (!(node.makeRenderer(sb, null) instanceof JsPrettyPrinter)) {
      throw new ClassCastException(node.getClass().getName());
    }
    TokenConsumer tc = new JsMinimalPrinter(sb);
    node.render(new RenderContext(tc));
    tc.noMoreTokens();
    return sb.toString();
  }

  /**
   * Ensures that a given node is cloneable by calling {@code clone()} on it and
   * checking sanity of the result. Tests for specific {@code ParseTreeNode}
   * subsystems should invoke this on a substantial set of example trees to
   * guard against problems creeping into the {@code clone()} implementations.
   *
   * @param node a {@code ParseTreeNode}.
   */
  protected void assertCloneable(ParseTreeNode node) {
    assertDeepEquals(node, node.clone());
  }

  /**
   * Ensures that two {@code ParseTreeNode} trees are deeply equal in the
   * topology and types of nodes in each tree, and in the {@code getValue()} and
   * {@code getFilePosition()} of each respective node.
   *
   * @param a a {@code ParseTreeNode}.
   * @param b a {@code ParseTreeNode}.
   */
  protected void assertDeepEquals(ParseTreeNode a, ParseTreeNode b) {
    assertEquals(a.getValue(), b.getValue());
    assertEquals(a.getFilePosition(), b.getFilePosition());
    assertEquals(a.children().size(), b.children().size());

    for (int i = 0; i < a.children().size(); ++i) {
      assertDeepEquals(a.children().get(i), b.children().get(i));
    }
  }

  /**
   * Ensures that two {@code Arrays} are of the same length and corresponding
   * objects are equal
   *
   * @param a a {@code Array of objects}.
   * @param b a {@code Array of objects}.
   */
  protected static void assertArrayEquals(Object[] a, Object[] b) {
    assertEquals(a.length, b.length);
    for (int i=0; i < a.length; i++) {
      // So that eclipse diff works more friendly
      if (a[i] instanceof String && b[i] instanceof String) {
        assertEquals((String)a[i], (String)b[i]);
      } else {
        assertEquals(a[i], b[i]);
      }
    }
  }

  protected void assertContains(String haystack, String needle) {
    assertTrue("Expected result to contain <" + needle + ">",
        haystack != null && haystack.contains(needle));
  }

  protected void assertNotContains(String haystack, String needle) {
    assertTrue("Expected result to not contain <" + needle + ">",
        haystack != null && !haystack.contains(needle));
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

  protected void assertNoWarnings() {
    assertMessagesLessSevereThan(MessageLevel.WARNING);
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

  protected void assertSerializable(Object o) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject(o);
    oos.close();
    assertTrue(out.toByteArray().length > 0);
  }

  private static int partsMissing(Message msg, MessagePart... parts) {
    int missing = 0;
    outerLoop:
    for (MessagePart expectedPart : parts) {
      for (MessagePart candidate : msg.getMessageParts()) {
        if (candidate.equals(expectedPart)) { continue outerLoop; }
        if (candidate instanceof FilePosition
            && expectedPart instanceof FilePosition) {
          FilePosition a = (FilePosition) candidate;
          FilePosition b = (FilePosition) expectedPart;
          // Ignore startCharInFile for purposes of testing to make tests more
          // robust against changes.
          if (a.source().equals(b.source())
              && a.startLineNo() == b.startLineNo()
              && a.startCharInLine() == b.startCharInLine()
              && a.endLineNo() == b.endLineNo()
              && a.endCharInLine() == b.endCharInLine()) {
            continue outerLoop;
          }
        }
      }
      ++missing;
    }
    return missing;
  }

  private InputSource sourceOf(CharProducer cp) {
    return cp.getSourceBreaks(0).source();
  }

  /**
   * Returns true in headless testing environments.
   * A headless testing environment has to set the "test.headless"
   * property, and actually be headless.
   */
  protected boolean checkHeadless() {
    if (Boolean.getBoolean("test.headless")) {
      assertTrue("test.headless==true in non-headless environment",
          GraphicsEnvironment.isHeadless());
      System.err.println(getName() + " skipped in headless testing");
      return true;
    }

    assertFalse("test.headless==false in headless environment",
        GraphicsEnvironment.isHeadless());
    return false;
  }

  @Override
  protected void runTest() throws Throwable {
  // Support filtering of test methods via the Java system property
  // "test.method.filter".  This can be used in conjunction with
  // "test.filter".
    String filterGlob = System.getProperty("test.method.filter");
    if (filterGlob != null) {
      // TODO: Maybe move globToPattern into util.
      Pattern methodFilter = Pattern.compile(
          AllTests.globToPattern(filterGlob), Pattern.DOTALL);
      if (!methodFilter.matcher(getName()).find()) {
        System.err.println("Skipping " + getName());
        return;
      }
    }

    // In Eclipse, to suppress known test failures,
    // (1) right click on the test in the package explorer and choose properties
    // (2) Choose the Run/Debug Settings tab
    // (3) Choose your favorite launch configuration and click edit.
    //     If there is none, make one by running the test.
    // (4) In the "Environment" tab, add a property
    //     test.suppressKnownFailures=true
    if ("true".equals("test.suppressKnownFailures")) {
      try {
        Method method = getClass().getMethod(getName(), new Class[0]);
        if (method.isAnnotationPresent(FailureIsAnOption.class)) {
          try {
            super.runTest();
          } catch (Throwable th) {
            System.err.println("Suppressing known failure of " + getName());
            th.printStackTrace();
          }
        }
        return;
      } catch (NoSuchMethodException ex) {
        // skip
      }
    }
    super.runTest();
  }
}
