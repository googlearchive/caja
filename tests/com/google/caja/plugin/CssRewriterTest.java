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
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.ElKey;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Lists;
import com.google.caja.util.MoreAsserts;
import com.google.caja.util.Name;
import com.google.caja.util.Sets;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class CssRewriterTest extends CajaTestCase {
  public final void testUnknownTagsRemoved() throws Exception {
    runTest("bogus { display: none }", "");
    runTest("a, bogus, i { display: none }",
            "a, i {\n  display: none\n}");
  }

  public final void testBadTagsRemoved() throws Exception {
    runTest("script { display: none }", "");
    assertMessage(
        true, PluginMessageType.UNSAFE_TAG, MessageLevel.ERROR,
        ElKey.forElement(Namespaces.HTML_DEFAULT, "script"));
    assertNoErrors();
    runTest("strike, script, strong { display: none }",
            "strike, strong {\n  display: none\n}");  // See error
    assertMessage(
        true, PluginMessageType.UNSAFE_TAG, MessageLevel.ERROR,
        ElKey.forElement(Namespaces.HTML_DEFAULT, "script"));
    assertNoErrors();
  }

  public final void testBadAttribsRemoved() throws Exception {
    runTest("div[zwop] { color: blue }", "");
  }

  public final void testInvalidPropertiesRemoved() throws Exception {
    // visibility takes "hidden", not "none"
    runTest("a { visibility: none }", "");
    runTest("a { visibility: hidden; }", "a {\n  visibility: hidden\n}");
    // no such property
    runTest("a { bogus: bogus }", "");
    // make sure it doesn't interfere with others
    runTest("a { visibility: none; font-weight: bold }",
            "a {\n  font-weight: bold\n}");
    runTest("a { font-weight: bold; visibility: none }",
            "a {\n  font-weight: bold\n}");
    runTest("a { bogus: bogus; font-weight: bold }",
            "a {\n  font-weight: bold\n}");
    runTest("a { font-weight: bold; bogus: bogus }",
            "a {\n  font-weight: bold\n}");
  }

  public final void testContentRemoved() throws Exception {
    runTest("a { color: blue; content: 'booyah'; text-decoration: underline; }",
            "a {\n  color: blue;\n  text-decoration: underline\n}");
  }

  public final void testAttrRemoved() throws Exception {
    runTest("a:attr(href) { color: blue }", "");
    runTest("a:attr(href) { color: blue } b { font-weight: bolder }",
            "b {\n  font-weight: bolder\n}");
  }

  public final void testFontNamesQuoted() throws Exception {
    runTest("a { font:12pt Times  New Roman, Times,\"Times Old Roman\",serif }",
            "a {\n  font: 12pt 'Times New Roman', 'Times',"
            + " 'Times Old Roman', serif\n}");
    runTest("a { font:bold 12pt Arial Black }",
            "a {\n  font: bold 12pt 'Arial Black'\n}");
  }

  public final void testNamespacing() throws Exception {
    runTest("a.foo { color:blue }", "a.foo {\n  color: blue\n}");
    runTest("#foo { color: blue }", "#foo {\n  color: blue\n}");
    runTest("body.ie6 p { color: blue }",
            "body.ie6 p {\n  color: blue\n}");
    runTest("body { margin: 0; }", "");  // Not allowed
    runTest("body.ie6 { margin: 0; }", "");  // Not allowed
    runTest("* html p { margin: 0; }", "* html p {\n  margin: 0\n}");
    runTest("* html { margin: 0; }", "");  // Not allowed
    runTest("* html > * > p { margin: 0; }", "");  // Not allowed
    runTest("#foo > #bar { color: blue }",
            "#foo > #bar {\n  color: blue\n}");
    runTest("#foo .bar { color: blue }",
            "#foo .bar {\n  color: blue\n}");
  }

  public final void testUnsafeIdentifiers() throws Exception {
    runTest("a.foo, b#c\\2c d, .e { color:blue }",  // "\\2c " -> ","
            "a.foo, .e {\n  color: blue\n}");
    runTest("a.foo, .b_c {color: blue}",
            "a.foo, .b_c {\n  color: blue\n}");
    runTest("a.foo, ._c {color: blue}",
            "a.foo {\n  color: blue\n}");
    runTest("a._c {_color: blue; margin:0;}", "");
    runTest("a#_c {_color: blue; margin:0;}", "");
    runTest(".c__ {_color: blue; margin:0;}", "");
    runTest("#c__ {_color: blue; margin:0;}", "");
  }

  public final void testPseudosWhitelisted() throws Exception {
    runTest("a:link, a:badness { color:blue }",
            "a:link {\n  color: blue\n}");
    mq.getMessages().clear();
    runTest("a:visited { color:blue }",
            "a:visited {\n  color: blue\n}");
    assertNoErrors();

    // Properties that are on DOMita's HISTORY_INSENSITIVE_STYLE_WHITELIST
    // should not be allowed in any rule that correlates with the :visited
    // pseudo selector.
    mq.getMessages().clear();
    runTest(
        "a:visited { color:blue; float:left; _float:left; *float:left }",
        "a:visited {\n  color: blue\n}");
    assertMessage(
        PluginMessageType.DISALLOWED_CSS_PROPERTY_IN_SELECTOR,
        MessageLevel.ERROR,
        FilePosition.instance(is, 1, 25, 25, 5), Name.css("float"),
        FilePosition.instance(is, 1, 1, 1, 9));
    assertMessage(
        PluginMessageType.DISALLOWED_CSS_PROPERTY_IN_SELECTOR,
        MessageLevel.ERROR,
        FilePosition.instance(is, 1, 37, 37, 6), Name.css("_float"),
        FilePosition.instance(is, 1, 1, 1, 9));
    assertMessage(
        PluginMessageType.DISALLOWED_CSS_PROPERTY_IN_SELECTOR,
        MessageLevel.ERROR,
        FilePosition.instance(is, 1, 51, 51, 5), Name.css("float"),
        FilePosition.instance(is, 1, 1, 1, 9));

    runTest(
        "a:visited { COLOR:blue; FLOAT:left; _FLOAT:left; *FLOAT:left }",
        "a:visited {\n  color: blue\n}");

    runTest(
        "*:visited { color: blue; }",
        "a:visited {\n  color: blue\n}");
    runTest(
        "#foo:visited { color: blue; }",
        "a#foo:visited {\n  color: blue\n}");
    runTest(
        ".foo:link { color: blue; }",
        "a.foo:link {\n  color: blue\n}");

    runTest(
        ""
        + "#foo:visited, div, .bar:link, p {\n"
        + "  padding: 1px;\n"
        + "  color: blue;\n"
        + "}",
        ""
        + "a#foo:visited, a.bar:link {\n"
        + "  color: blue\n"
        + "}\n"
        + "div, p {\n"
        + "  padding: 1px;\n"
        + "  color: blue\n"
        + "}");

    runTest(
        ""
        + "a#foo-bank {"
        + "  background: 'http://whitelisted-host.com/?bank=X&u=Al';"
        + "  color: purple"
        + "}",
        ""
        + "a#foo-bank {\n"
        + "  background: url('http://whitelisted-host.com/?bank=X&u=Al');\n"
        + "  color: purple\n"
        + "}");
    // Differs from the previous only in that it has the :visited pseudo
    // selector which means we can't allow it to cause a network fetch because
    // that could leak user history state.
    mq.getMessages().clear();
    runTest(
        ""
        + "a#foo-bank:visited {"
        + "  background-image: 'http://whitelisted-host.com/?bank=X&u=Al';"
        + "  color: purple"
        + "}",
        ""
        + "a#foo-bank:visited {\n"
        + "  color: purple\n"
        + "}");
  }

  public final void testNoBadUrls() throws Exception {
    // ok
    runTest("#foo { background: url(/bar.png) }",
            "#foo {\n  background: url('/foo/bar.png')\n}");
    runTest("#foo { background: url('/bar.png') }",
            "#foo {\n  background: url('/foo/bar.png')\n}");
    runTest("#foo { background: '/bar.png' }",
            "#foo {\n  background: url('/foo/bar.png')\n}");
    runTest(
        "#foo { background: 'http://whitelisted-host.com/blinky.gif' }",
        "#foo {\n  background: url('http://whitelisted-host.com/blinky.gif')\n}"
        );

    // disallowed
    runTest("#foo { background: url('http://cnn.com/bar.png') }",
            "");
    runTest("#foo { background: 'http://cnn.com/bar.png' }",
            "");
  }

  public final void testSubstitutions() throws Exception {
    try {
      runTest("#foo { left: ${x * 4}px; top: ${y * 4}px; }",
              "", false);
      fail("allowed substitutions when parsing of substitutions disabled");
    } catch (ParseException ex) {
      // pass
    }
    runTest("#foo { left: ${x * 4}px; top: ${y * 4}px; }",
            "#foo {\n  left: ${x * 4}px;\n  top: ${y * 4}px\n}",
            true);
  }

  /**
   * "*" selectors should rewrite properly.
   * <a href="http://code.google.com/p/google-caja/issues/detail?id=57">bug</a>
   */
  public final void testWildcardSelectors() throws Exception {
    runTest("div * { margin: 0; }", "div * {\n  margin: 0\n}", false);
  }

  public final void testUnitlessLengths() throws Exception {
    runTest("div { padding: 10 0 5.0 4 }",
            "div {\n  padding: 10px 0 5.0px 4px\n}", false);
    runTest("div { margin: -5 5; z-index: 2 }",
            "div {\n  margin: -5px 5px;\n  z-index: 2\n}", false);
  }

  public final void testUserAgentHacks() throws Exception {
    runTest(
        ""
        + "p {\n"
        + "  color: blue;\n"
        + "  *color: red;\n"
        + "  background-color: green;\n"
        + "  *background-color: yelow;\n"  // misspelled
        + "  font-weight: bold\n"
        + "}",
        ""
        + "p {\n"
        + "  color: blue;\n"
        + "  *color: red;\n"  // Good user agent hack
        + "  background-color: green;\n"
        // Bad user-agent hack removed.
        + "  font-weight: bold\n"
        + "}"
        );
    assertMessage(PluginMessageType.MALFORMED_CSS_PROPERTY_VALUE,
                  MessageLevel.WARNING,
                  Name.css("background-color"),
                  MessagePart.Factory.valueOf("==>yelow<=="));
    runTest("a.c {_color: blue; margin:0;}",
            "a.c {\n  _color: blue;\n  margin: 0\n}");
    assertNoErrors();
  }

  public final void testNonStandardColors() throws Exception {
    runTest("a.c { color: LightSlateGray; background: ivory; }",
            "a.c {\n  color: #789;\n  background: #fffff0\n}");
    assertMessage(PluginMessageType.NON_STANDARD_COLOR,
                  MessageLevel.LINT, Name.css("lightslategray"),
                  MessagePart.Factory.valueOf("#789"));
    assertMessage(PluginMessageType.NON_STANDARD_COLOR,
                  MessageLevel.LINT, Name.css("ivory"),
                  MessagePart.Factory.valueOf("#fffff0"));
    assertNoErrors();

    FilePosition u = FilePosition.UNKNOWN;
    assertNull(CssRewriter.colorHash(u, Name.css("invisible")));
    // Can get color hashes even for standard colors.
    assertEquals("#00f", CssRewriter.colorHash(u, Name.css("blue")).getValue());
    // Is case insensitive.
    assertEquals("#00f", CssRewriter.colorHash(u, Name.css("Blue")).getValue());
    assertEquals("#00f", CssRewriter.colorHash(u, Name.css("BLUE")).getValue());

    assertEquals("#000", CssRewriter.colorHash(u, 0).getValue());
    assertEquals("#fff", CssRewriter.colorHash(u, 0xffffff).getValue());
    assertEquals("#123", CssRewriter.colorHash(u, 0x112233).getValue());
    // A change in any quartet causes the long form to be used.
    assertEquals(
        "#022233", CssRewriter.colorHash(u, 0x112233 ^ 0x130000).getValue());
    assertEquals(
        "#111333", CssRewriter.colorHash(u, 0x112233 ^ 0x003100).getValue());
    assertEquals(
        "#112220", CssRewriter.colorHash(u, 0x112233 ^ 0x000013).getValue());
  }

  public final void testFixedPositioning() throws Exception {
    runTest("#foo { position: absolute; left: 0px; top: 0px }",
            "#foo {\n  position: absolute;\n  left: 0px;\n  top: 0px\n}");
    assertNoErrors();
    runTest("#foo { position: fixed; left: 0px; top: 0px }",
            "#foo {\n  left: 0px;\n  top: 0px\n}");
    // TODO(mikesamuel): fix message.  "fixed" is well-formed but disallowed.
    assertMessage(true, PluginMessageType.MALFORMED_CSS_PROPERTY_VALUE,
                  MessageLevel.WARNING, Name.css("position"),
                  MessagePart.Factory.valueOf("==>fixed<=="));
    assertNoErrors();
  }

  public final void testUrisCalledWithProperPropertyPart() throws Exception {
    // The CssRewriter needs to rewrite URIs.
    // When it does so it passes
    assertCallsUriRewriterWithPropertyPart(
        "background: 'foo.png'",
        "background::bg-image::image");
    assertCallsUriRewriterWithPropertyPart(
        ""
        + "img.trans {"
        + "  filter: progid:DXImageTransform.Microsoft.AlphaImageLoader("
        + "      src='bar.png', sizingMethod='image');"
        + "}",
        "filter::prog-id::prog-id-alpha-image-loader::page-url");
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

    CssSchema cssSchema = CssSchema.getDefaultCss21Schema(mq);
    new CssValidator(cssSchema, HtmlSchema.getDefault(mq), mq)
        .validateCss(AncestorChain.instance(t));
    new CssRewriter(
        new UriPolicy() {
          public String rewriteUri(
              ExternalReference ref, UriEffect effect, LoaderType loader,
              Map<String, ?> hints) {
            URI uri = ref.getUri();

            if ("test".equals(uri.getScheme())  // Used by CajaTestCase
                && "example.org".equals(uri.getHost())
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
        },
        cssSchema, mq)
        .rewrite(AncestorChain.instance(t));

    {
      StringBuilder msgBuf = new StringBuilder();
      t.formatTree(mc, 0, msgBuf);
      msg += "\n  ->\n" + msgBuf.toString();
    }

    assertEquals(msg, golden, render(t));
  }

  private void assertCallsUriRewriterWithPropertyPart(
      String cssCode, String... expectedParts)
      throws ParseException {
    final Set<String> propertyParts = Sets.newLinkedHashSet();

    CssTree t = cssCode.trim().endsWith("}")
        ? css(fromString(cssCode)) : cssDecls(fromString(cssCode));

    CssSchema cssSchema = CssSchema.getDefaultCss21Schema(mq);
    new CssValidator(cssSchema, HtmlSchema.getDefault(mq), mq)
        .validateCss(AncestorChain.instance(t));
    new CssRewriter(
        new UriPolicy() {
          public String rewriteUri(
              ExternalReference ref, UriEffect effect, LoaderType loader,
              Map<String, ?> hints) {
            propertyParts.add(
                UriPolicyHintKey.CSS_PROP.valueFrom(hints)
                    .getCanonicalForm());
            return ref.getUri().toString();
          }
        },
        cssSchema, mq)
        .rewrite(AncestorChain.instance(t));

    MoreAsserts.assertListsEqual(
        Arrays.asList(expectedParts),
        Lists.newArrayList(propertyParts));
  }
}
