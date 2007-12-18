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

package com.google.caja.parser.css;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Criterion;
import com.google.caja.util.TestUtil;

import java.io.StringReader;
import java.net.URI;
import junit.framework.TestCase;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class CssTreeTest extends TestCase {

  public void testRender() throws Exception {
    String golden = TestUtil.readResource(getClass(), "cssrendergolden1.txt");
    CssTree.StyleSheet stylesheet;
    CharProducer cp = TestUtil.getResourceAsProducer(
        getClass(), "cssparserinput1.css");
    try {
      stylesheet = parseStyleSheet(cp);
    } finally {
      cp.close();
    }
    StringBuilder sb = new StringBuilder();
    RenderContext rc = new RenderContext(new MessageContext(), sb);
    stylesheet.render(rc);
    assertEquals(golden.trim(), sb.toString().trim());
  }

  // TODO(msamuel): Test rendering of !important and combinators in selectors.

  public void testStringRendering() throws Exception {
    assertRenderedForm(
        "a {\n  background: ''\n}",
        "a { background: '' }", false);
    assertRenderedForm(
        "a {\n  background: ''\n}",
        "a { background: '' }", true);

    assertRenderedForm(
        "a {\n  background: 'foo\\5C bar\\27 Baz\\22zoicks\\A '\n}",
        "a { background: 'foo\\\\bar\\'Baz\\\"zoicks\\0A' }", false);
    assertRenderedForm(
        "a {\n  background: 'foo\\5C bar\\27 Baz\\22zoicks\\A '\n}",
        "a { background: 'foo\\\\bar\\'Baz\\\"zoicks\\0A ' }", true);

    assertRenderedForm(
        "a {\n  background: '</script><b>'\n}",
        "a { background: '</script><b>' }", false);
    assertRenderedForm(
        "a {\n  background: '\\3C/script\\3E\\3C b\\3E '\n}",
        "a { background: \"</script><b>\" }", true);

    assertRenderedForm(
        "a {\n  background: ']]>'\n}",
        "a { background: ']]>' }", false);
    assertRenderedForm(
        "a {\n  background: ']]>'\n}",
        "a { background: ']]\\3E ' }", false);
    assertRenderedForm(
        "a {\n  background: ']]\\3E '\n}",
        "a { background: ']]\\3E' }", true);
    assertRenderedForm(
        "a {\n  background: ']]\\3E '\n}",
        "a { background: ']]\\3E ' }", true);
    assertRenderedForm(
        "a {\n  background: ']]\\3E '\n}",
        "a { background: ']]>' }", true);
  }

  public void testUrlRendering() throws Exception {
    assertRenderedForm(
        "a {\n  background: url('')\n}",
        "a { background: url('') }", false);
    assertRenderedForm(
        "a {\n  background: url('')\n}",
        "a { background: url('') }", true);

    assertRenderedForm(
        "a {\n  background: url('foo')\n}",
        "a { background: url(foo) }", false);
    assertRenderedForm(
        "a {\n  background: url('foo')\n}",
        "a { background: url(foo) }", true);

    assertRenderedForm(
        "a {\n  background: url('foo')\n}",
        "a { background: url('foo') }", false);
    assertRenderedForm(
        "a {\n  background: url('foo')\n}",
        "a { background: url('foo') }", true);
  }

  public void testParanoidUrlRendering() throws Exception {
    try {
      assertRenderedForm(
          "a {\n  background: url('\\3C/script')\n}",
          "a { background: url('</script') }", true);
    } catch (ParseException ex) {
      // pass
    }

    try {
      assertRenderedForm(
          "a {\n  background: url('\\3C/script')\n}",
          "a { background: url(</script) }", true);
    } catch (ParseException ex) {
      // pass
    }

    try {
      assertRenderedForm(
          "a {\n  background: url(']]\\3E ')\n}",
          "a { background: url(]]>) }", true);
    } catch (ParseException ex) {
      // pass
    }

    try {
      assertRenderedForm(
          "a {\n  background: url(']]\\3E ')\n}",
          "a { background: url(']]>') }", true);
    } catch (ParseException ex) {
      // pass
    }

    try {
      assertRenderedForm(
          "a {\n  background: url('\\3C!DOCTYPE')\n}",
          "a { background: url(/<!DOCTYPE) }", true);
    } catch (ParseException ex) {
      // pass
    }

    try {
      assertRenderedForm(
          "a {\n  background: url('\\3C!DOCTYPE')\n}",
          "a { background: url('/<!DOCTYPE') }", true);
    } catch (ParseException ex) {
      // pass
    }
  }

  public void testIdentifierEscaping() throws Exception {
    assertRenderedForm(
        "Le caja .no es #un rect\\E1ngulo {\n  \n}",
        "Le caja .no es #un rect\u00E1ngulo {}", true);

    // '\x34' == '4'
    assertRenderedForm(
        "\\34is a number and an identifier {\n  \n}",
        "\\34is a number and an identifier {}", true);

    assertRenderedForm(
        "\\34 0 is a number and an identifier too {\n  \n}",
        "\\34 0 is a number and an identifier too {}", true);
  }

  public void testPrio() throws Exception {
    assertRenderedForm(
        "sky {\n  color: #0000ff !important\n}",
        "sky { color: #0000ff !important }",
        false);
  }

  public void testOperators() throws Exception {
    assertRenderedForm(
        "hi {\n  x: f(-3);\n  y: +g(2)\n}",
        "hi { x: f(-3); y: +g( 2 ) }", true);
  }

  private static void assertRenderedForm(
      String golden, String cssInput, boolean paranoid)
      throws Exception {
    InputSource is = new InputSource(new URI("content", cssInput, null));

    MessageContext mc = new MessageContext();
    CharProducer cp = CharProducer.Factory.create(
        new StringReader(cssInput), is);
    CssTree.StyleSheet stylesheet = parseStyleSheet(cp);
    cp.close();

    StringBuilder sb = new StringBuilder();
    RenderContext rc = new RenderContext(mc, sb, paranoid);
    stylesheet.render(rc);
    String actual = sb.toString();

    assertEquals(actual, golden, actual);
  }

  private static CssTree.StyleSheet parseStyleSheet(CharProducer cp)
      throws Exception {

    CssLexer lexer = new CssLexer(cp);
    TokenQueue<CssTokenType> tq = new TokenQueue<CssTokenType>(
        lexer, cp.getCurrentPosition().source(),
        new Criterion<Token<CssTokenType>>() {
          public boolean accept(Token<CssTokenType> t) {
            return CssTokenType.SPACE != t.type
            && CssTokenType.COMMENT != t.type;
          }
        });
    CssParser p = new CssParser(tq);
    CssTree.StyleSheet stylesheet = p.parseStyleSheet();
    tq.expectEmpty();
    return stylesheet;
  }

  // TODO(mikesamuel): test rendering of @imports, @page, @font-face,
  // and other directives etc.
  // TODO(mikesamuel): test rendering of unicode range literals.
}
