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

import com.google.caja.lexer.ParseException;
import com.google.caja.render.Concatenator;
import com.google.caja.render.CssPrettyPrinter;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.TestUtil;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class CssTreeTest extends CajaTestCase {

  public final void testRender1() throws Exception {
    runRenderTest("cssrendergolden1.txt", "cssparserinput1.css", false);
  }

  public final void testRender2() throws Exception {
    runRenderTest("cssrendergolden2.txt", "cssparserinput2.css", false);
  }

  public final void testRender4() throws Exception {
    // Make sure we don't have <!-- or --> in output in paranoid mode.
    runRenderTest("cssrendergolden4.txt", "cssparserinput4.css", true);
  }

  public final void testRenderFilters() throws Exception {
    runRenderTest("cssrendergolden-filters.css", "cssparserinput-filters.css",
                  false);
  }

  public final void testRenderUserAgentHacks() throws Exception {
    // User agent hacks do not show up in rendered output.
    runRenderTest(
        "cssrendergolden-uahacks.css", "cssparserinput-uahacks.css", false);
    runRenderTest(
        "cssrendergolden-uahacks.css", "cssparserinput-uahacks.css", true);
  }

  public final void testClone1() throws Exception {
    CssTree t = css(fromResource("cssparserinput1.css"));
    assertEquals(render(t), render(t.clone()));
  }

  public final void testClone2() throws Exception {
    CssTree t = css(fromString(
        "span { background-image:url('//www.example.org/image.gif'); }"));
    assertEquals(render(t), render(t.clone()));
  }

  public final void testStringRendering() throws Exception {
    assertRenderedForm(
        "a {\n  background: ''\n}",
        "a { background: '' }");

    assertRenderedForm(
        "a {\n  background: 'foo\\5C bar\\27 Baz\\22zoicks\\A '\n}",
        "a { background: 'foo\\\\bar\\'Baz\\\"zoicks\\0A ' }");

    assertRenderedForm(
        "a {\n  background: '\\3C/script\\3E\\3C b\\3E '\n}",
        "a { background: \"</script><b>\" }");

    assertRenderedForm(
        "a {\n  background: '\\5D\\5D\\3E '\n}",
        "a { background: ']]>' }");
    assertRenderedForm(
        "a {\n  background: '\\5D\\5D\\3E '\n}",
        "a { background: ']]\\3E ' }");
  }

  public final void testUrlRendering() throws Exception {
    assertRenderedForm(
        "a {\n  background: url('')\n}",
        "a { background: url('') }");

    assertRenderedForm(
        "a {\n  background: url('foo')\n}",
        "a { background: url(foo) }");

    assertRenderedForm(
        "a {\n  background: url('foo')\n}",
        "a { background: url('foo') }");

    assertRenderedForm(
        "a {\n  background: url('url%28%27hi%27%29')\n}",
        "a { background: url(\"url('hi')\") }");
  }

  public final void testParanoidUrlRendering() throws Exception {
    try {
      assertRenderedForm(
          "a {\n  background: url('\\3C/script')\n}",
          "a { background: url('</script') }");
    } catch (ParseException ex) {
      // pass
    }

    try {
      assertRenderedForm(
          "a {\n  background: url('\\3C/script')\n}",
          "a { background: url(</script) }");
    } catch (ParseException ex) {
      // pass
    }

    try {
      assertRenderedForm(
          "a {\n  background: url('\\5D\\5D\\3E ')\n}",
          "a { background: url(]]>) }");
    } catch (ParseException ex) {
      // pass
    }

    try {
      assertRenderedForm(
          "a {\n  background: url('\\5D\\5D\\3E ')\n}",
          "a { background: url(']]>') }");
    } catch (ParseException ex) {
      // pass
    }

    try {
      assertRenderedForm(
          "a {\n  background: url('\\3C\\21DOCTYPE')\n}",
          "a { background: url(/<!DOCTYPE) }");
    } catch (ParseException ex) {
      // pass
    }

    try {
      assertRenderedForm(
          "a {\n  background: url('\\3C\\21DOCTYPE')\n}",
          "a { background: url('/<!DOCTYPE') }");
    } catch (ParseException ex) {
      // pass
    }
  }

  public final void testIdentifierEscaping() throws Exception {
    assertRenderedForm(
        "Le caja .no es #un rect\\E1ngulo {\n}",
        "Le caja .no es #un rect\u00E1ngulo {}");

    // '\x34' == '4'
    assertRenderedForm(
        "\\34is a number and an identifier {\n}",
        "\\34is a number and an identifier {}");

    assertRenderedForm(
        "\\34 0 is a number and an identifier too {\n}",
        "\\34 0 is a number and an identifier too {}");
  }

  public final void testPrio() throws Exception {
    assertRenderedForm(
        "sky {\n  color: #0000ff !important\n}",
        "sky { color: #0000ff !important }");
  }

  public final void testOperators() throws Exception {
    assertRenderedForm(
        "hi {\n  x: f(-3);\n  y: +g(2)\n}",
        "hi { x: f(-3); y: +g( 2 ) }");
  }

  public final void testCombinatorRules() throws Exception {
    assertRenderedForm(
        "hello > world {\n  color: blue\n}",
        "hello>world { color: blue }");
    assertRenderedForm(
        "hello + world {\n  color: blue\n}",
        "hello+world { color: blue }");
  }

  private void runRenderTest(
      String goldenFile, String inputFile, boolean paranoid)
      throws Exception {
    String golden = TestUtil.readResource(getClass(), goldenFile);
    CssTree.StyleSheet stylesheet = css(fromResource(inputFile));
    StringBuilder sb = new StringBuilder();
    CssPrettyPrinter csspp = new CssPrettyPrinter(new Concatenator(sb));
    RenderContext rc = new RenderContext(csspp)
        .withAsciiOnly(true).withEmbeddable(paranoid);
    stylesheet.render(rc);
    assertEquals(golden.trim(), sb.toString().trim());
  }

  private void assertRenderedForm(String golden, String cssInput)
      throws Exception {
    CssTree.StyleSheet stylesheet = css(fromString(cssInput));

    StringBuilder sb = new StringBuilder();
    CssPrettyPrinter csspp = new CssPrettyPrinter(new Concatenator(sb));
    RenderContext rc = new RenderContext(csspp).withAsciiOnly(true);
    stylesheet.render(rc);
    csspp.noMoreTokens();
    String actual = sb.toString();

    assertEquals(actual, golden, actual);
  }

  // TODO(mikesamuel): test rendering of @imports, @page, @font-face,
  // and other directives etc.
  // TODO(mikesamuel): test rendering of unicode range literals.
}
