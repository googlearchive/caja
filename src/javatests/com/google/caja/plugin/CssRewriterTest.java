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

import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.css.CssTree;
import com.google.caja.util.CajaTestCase;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class CssRewriterTest extends CajaTestCase {
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

  public void testFontNamesQuoted() throws Exception {
    runTest("a { font:12pt Times  New Roman, Times,\"Times Old Roman\",serif }",
            ".test a {\n  font: 12pt 'Times New Roman', 'Times',"
            + " 'Times Old Roman', serif\n}");
  }

  public void testNamespacing() throws Exception {
    runTest("a.foo { color:blue }", ".test a.test-foo {\n  color: blue\n}");
    runTest("#foo { color: blue }", ".test #test-foo {\n  color: blue\n}");
    runTest("body.ie6 p { color: blue }",
            "body.ie6 .test p {\n  color: blue\n}");
    runTest("body { margin: 0; }", ".test body {\n  margin: 0\n}");
    runTest("body.ie6 { margin: 0; }", ".test body.ie6 {\n  margin: 0\n}");
    runTest("#foo > #bar { color: blue }",
            ".test #test-foo > #test-bar {\n  color: blue\n}");
    runTest("#foo .bar { color: blue }",
            ".test #test-foo .test-bar {\n  color: blue\n}");
  }

  public void testUnsafeIdentifiers() throws Exception {
    runTest("a.foo, b#c\\2c d, .e { color:blue }",  // "\\2c " -> ","
            ".test a.test-foo, .test .test-e {\n  color: blue\n}");
    runTest("a.foo, .b_c {color: blue}",
    ".test a.test-foo, .test .test-b_c {\n  color: blue\n}");
    runTest("a.foo, ._c {color: blue}",
    ".test a.test-foo {\n  color: blue\n}");
    runTest("a.c {_color: blue; margin:0;}",
    ".test a.test-c {\n  margin: 0\n}");
    runTest("a._c {_color: blue; margin:0;}",
    "");
    runTest("a#_c {_color: blue; margin:0;}",
    "");
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
    runTest("#foo { background: 'http://whitelisted-host.com/blinky.gif' }",
            ".test #test-foo {\n  background:"
            + " 'http://whitelisted-host.com/blinky.gif'\n}");

    // disallowed
    runTest("#foo { background: url('http://cnn.com/bar.png') }",
            "");
    runTest("#foo { background: 'http://cnn.com/bar.png' }",
            "");
  }

  public void testSubstitutions() throws Exception {
    try {
      runTest("#foo { left: ${x * 4}px; top: ${y * 4}px; }",
              "", false);
      fail("allowed substitutions when parsing of substitutions disabled");
    } catch (ParseException ex) {
      // pass
    }
    runTest("#foo { left: ${x * 4}px; top: ${y * 4}px; }",
            ".test #test-foo {\n  left: ${x * 4}px;\n  top: ${y * 4}px\n}",
            true);
  }

  /**
   * "*" selectors should rewrite properly.
   * <a href="http://code.google.com/p/google-caja/issues/detail?id=57">bug</a>
   */
  public void testWildcardSelectors() throws Exception {
    runTest("div * { margin: 0; }", ".test div * {\n  margin: 0\n}", false);
  }

  public void testUnitlessLengths() throws Exception {
    runTest("div { padding: 10 0 5.0 4 }",
            ".test div {\n  padding: 10px 0 5.0px 4px\n}", false);
    runTest("div { margin: -5 5; z-index: 2 }",
            ".test div {\n  margin: -5px 5px;\n  z-index: 2\n}", false);
  }

  private void runTest(String css, String golden) throws Exception {
    runTest(css, golden, false);
  }

  private void runTest(String css, String golden, boolean allowSubstitutions)
      throws Exception {
    mq.getMessages().clear();
    mc.relevantKeys = Collections.singleton(CssValidator.INVALID);

    CssTree t = css(fromString(css), allowSubstitutions);

    String msg;
    {
      StringBuilder msgBuf = new StringBuilder();
      t.formatTree(mc, 0, msgBuf);
      msg = msgBuf.toString();
    }

    new CssValidator(CssSchema.getDefaultCss21Schema(mq),
                     HtmlSchema.getDefault(mq), mq)
        .validateCss(new AncestorChain<CssTree>(t));
    new CssRewriter(
        new PluginMeta(
            "test",
            new PluginEnvironment() {
              public CharProducer loadExternalResource(
                  ExternalReference ref, String mimeType) {
                return null;
              }
              public String rewriteUri(ExternalReference ref, String mimeType) {
                URI uri = ref.getUri();

                if (uri.getScheme() == null
                    && uri.getHost() == null
                    && uri.getPath() != null
                    && uri.getPath().startsWith("/")) {
                  try {
                    return new URI(null, null, "/foo" + uri.getPath(),
                                   uri.getQuery(), uri.getFragment())
                        .toString();
                  } catch (URISyntaxException ex) {
                    ex.printStackTrace();
                    return null;
                  }
                } else if ("whitelisted-host.com".equals(uri.getHost())) {
                  return uri.toString();
                } else {
                  return null;
                }
              }
            }),
        mq)
        .rewrite(new AncestorChain<CssTree>(t));

    {
      StringBuilder msgBuf = new StringBuilder();
      t.formatTree(mc, 0, msgBuf);
      msg += "\n  ->\n" + msgBuf.toString();
    }

    String actual = render(t);
    System.err.println("\n\nactual=[[" + actual + "]]");
    assertEquals(msg, golden, actual);
  }
}
