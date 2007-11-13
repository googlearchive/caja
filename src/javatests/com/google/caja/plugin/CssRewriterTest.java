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
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.css.CssTree;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Criterion;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;

import java.util.Collections;

import junit.framework.TestCase;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class CssRewriterTest extends TestCase {
  private MessageQueue mq;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    if (null == mq) {
      mq = new EchoingMessageQueue(
          new PrintWriter(new OutputStreamWriter(System.err)),
          new MessageContext(), false);
    }
  }

  public void testUnknownTagsRemoved() throws Exception {
    runTest("bogus { display: none }", "");
    runTest("a, bogus, i { display: none }",
            ".test a, .test i {\n  display: none\n}");
  }

  // TODO(ihab): Make final decision whether to keep or remove. This test was
  // disabled since we decided to support more arbitrary HTML, but the deeper
  // implications are not yet clear.
  public void testBadTagsRemoved() throws Exception {
    if (false) {
      runTest("script { display: none }", "");
      runTest("strike, script, strong { display: none }",
              ".test strike, .test strong {\n  display: none\n}");
    }
  }

  public void testBadAttribsRemoved() throws Exception {
    runTest("div[zwop] { color: blue }", "");
  }

  public void testInvalidPropertiesRemoved() throws Exception {
    // visibility takes "hidden", not "none"
    runTest("a { visibility: none }", "");
    runTest("a { visibility: hidden; }", ".test a {\n  visibility: hidden\n}");
    // no such property
    runTest("a { bogus: bogus }", "");
    // make sure it doesn't interfere with others
    runTest("a { visibility: none; font-weight: bold }",
            ".test a {\n  font-weight: bold\n}");
    runTest("a { font-weight: bold; visibility: none }",
            ".test a {\n  font-weight: bold\n}");
    runTest("a { bogus: bogus; font-weight: bold }",
            ".test a {\n  font-weight: bold\n}");
    runTest("a { font-weight: bold; bogus: bogus }",
            ".test a {\n  font-weight: bold\n}");
  }

  public void testContentRemoved() throws Exception {
    runTest("a { color: blue; content: 'booyah'; text-decoration: underline; }",
            ".test a {\n  color: blue;\n  text-decoration: underline\n}");
  }

  public void testAttrRemoved() throws Exception {
    runTest("a:attr(href) { color: blue }", "");
    runTest("a:attr(href) { color: blue } b { font-weight: bolder }",
            ".test b {\n  font-weight: bolder\n}");
  }

  public void testNamespacing() throws Exception {
    runTest("a.foo { color:blue }", ".test a.test-foo {\n  color: blue\n}");
    runTest("#foo { color: blue }", ".test #test-foo {\n  color: blue\n}");
    runTest("body.ie6 p { color: blue }",
            "body.ie6 .test p {\n  color: blue\n}");
    runTest("#foo > #bar { color: blue }",
            ".test #test-foo > #test-bar {\n  color: blue\n}");
    runTest("#foo .bar { color: blue }",
            ".test #test-foo .test-bar {\n  color: blue\n}");
  }

  public void testUnsafeIdentifiers() throws Exception {
    runTest("a.foo, b#c\\2c d, .e { color:blue }",  // "\\2c " -> ","
            ".test a.test-foo, .test .test-e {\n  color: blue\n}");
  }

  public void testPseudosWhitelisted() throws Exception {
    runTest("a:link, a:badness { color:blue }",
            ".test a:link {\n  color: blue\n}");
  }

  public void testNoBadUrls() throws Exception {
    // ok
    runTest("#foo { background: url(/bar.png) }",
            ".test #test-foo {\n  background: url('/foo/bar.png')\n}");
    runTest("#foo { background: url('/bar.png') }",
            ".test #test-foo {\n  background: url('/foo/bar.png')\n}");
    runTest("#foo { background: '/bar.png' }",
            ".test #test-foo {\n  background: '/foo/bar.png'\n}");

    // disallowed
    runTest("#foo { background: url('http://cnn.com/bar.png') }",
            "");
    runTest("#foo { background: 'http://cnn.com/bar.png' }",
            "");
  }

  public void testSubstitutions() throws Exception {
    try {
      runTest("#foo { left: $(x * 4)px; top: $(y * 4)px; }",
              "", false);
      fail("allowed substitutions when parsing of substitutions disabled");
    } catch (ParseException ex) {
      // pass
    }
    runTest("#foo { left: $(x * 4)px; top: $(y * 4)px; }",
            ".test #test-foo {\n  left: $(x * 4)px;\n  top: $(y * 4)px\n}",
            true);
  }

  private void runTest(String css, String golden) throws Exception {
    runTest(css, golden, false);
  }

  private void runTest(String css, String golden, boolean allowSubstitutions)
      throws Exception {
    MessageContext mc = new MessageContext();
    mc.relevantKeys = Collections.singleton(CssValidator.INVALID);

    CssTree t = parseCss(css, allowSubstitutions);
    String msg;
    {
      StringBuilder msgBuf = new StringBuilder();
      t.formatTree(mc, 0, msgBuf);
      msg = msgBuf.toString();
    }

    new CssRewriter(
        new PluginMeta("Plugin", "test", "/foo", "rootDiv", false), mq)
            .rewrite(t);

    {
      StringBuilder msgBuf = new StringBuilder();
      t.formatTree(mc, 0, msgBuf);
      msg += "\n  ->\n" + msgBuf.toString();
    }

    StringBuilder actual = new StringBuilder();
    t.render(new RenderContext(new MessageContext(), actual));
    System.err.println("\n\nactual=[[" + actual + "]]");
    assertEquals(msg, golden, actual.toString());
  }

  private CssTree parseCss(String css, boolean allowSubstitutions)
      throws Exception {
    InputSource is = new InputSource(new URI("test://" + getClass().getName()));
    CharProducer cp = CharProducer.Factory.create(new StringReader(css), is);
    try {
      CssLexer lexer = new CssLexer(cp, allowSubstitutions);
      TokenQueue<CssTokenType> tq = new TokenQueue<CssTokenType>(
          lexer, cp.getCurrentPosition().source(),
          new Criterion<Token<CssTokenType>>() {
            public boolean accept(Token<CssTokenType> t) {
              return CssTokenType.SPACE != t.type
                  && CssTokenType.COMMENT != t.type;
            }
          });
      CssParser p = new CssParser(tq);
      CssTree t = p.parseStyleSheet();
      new CssValidator(mq).validateCss(t);
      tq.expectEmpty();
      return t;
    } finally {
      cp.close();
    }
  }
}
