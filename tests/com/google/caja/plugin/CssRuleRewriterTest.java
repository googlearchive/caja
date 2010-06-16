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

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;

public class CssRuleRewriterTest extends CajaTestCase {
  public final void testSimpleRule() {
    assertCompiledCss(
        "p {color:purple}",
        "[ '.', ' p {\\n  color: purple\\n}' ]");
  }

  public final void testClassRule() {
    assertCompiledCss(
        ".foo .bar {color:blue}",
        "[ '.', ' .foo .bar {\\n  color: blue\\n}' ]");
    assertCompiledCss(
        ".foo.bar {color:blue}",
        "[ '.', ' .foo.bar {\\n  color: blue\\n}' ]");
  }

  public final void testIdRule() {
    assertCompiledCss(
        "#foo {color:blue}",
        "[ '.', ' #foo-', ' {\\n  color: blue\\n}' ]");
    assertCompiledCss(
        "p#foo #baz{color:blue}",
        "[ '.', ' p#foo-', ' #baz-', ' {\\n  color: blue\\n}' ]");
  }

  public final void testBodyMarker() {
    assertCompiledCss(
        "body.ie6 p {color:blue}",
        // For a id suffix X we get
        // .vdoc-body___.ie6.X p which applies to all p that are
        // descendants of virtual bodies with the ie6 class and the X class.
        "[ '.vdoc-body___.ie6.', ' p {\\n  color: blue\\n}' ]");
    assertCompiledCss(
        "body.ie6#zoicks p {color:blue}",
        "[ '.vdoc-body___.ie6#zoicks-', '.', ' p {\\n  color: blue\\n}' ]");
    assertCompiledCss(
        "body.ie6 {color:blue}",
        "[ '.vdoc-body___.ie6.', ' {\\n  color: blue\\n}' ]");
    assertCompiledCss(
        "body { font-size: 12pt }",
        "[ '.vdoc-body___.', ' {\\n  font-size: 12pt\\n}' ]");
  }

  public final void testCompoundRule() {
    assertCompiledCss(
        "a, b {color:blue}",
        "[ '.', ' a, .', ' b {\\n  color: blue\\n}' ]");
  }

  public final void testDescendentRule() {
    assertCompiledCss(
        "#foo > #bar { color: blue }",
        "[ '.', ' #foo-', ' > #bar-', ' {\\n  color: blue\\n}' ]");
  }

  /**
   * "*" selectors should rewrite properly.
   * <a href="http://code.google.com/p/google-caja/issues/detail?id=57">bug</a>
   */
  public final void testWildcardSelectors() {
    assertCompiledCss(
        "div * { margin: 0; }",
        "[ '.', ' div * {\\n  margin: 0;\\n}' ]");
  }

  public final void testStaticIdClass() {
    assertCompiledCss(
        "#a > #b, .c { color: blue }",
        "[ '.xyz___ #a-xyz___ > #b-xyz___, .xyz___ .c {\\n  color: blue\\n}' ]",
        false);
  }

  private void assertCompiledCss(String input, String golden) {
    assertCompiledCss(input, golden, true);
  }

  private void assertCompiledCss(String input, String golden, boolean dynamic) {
    try {
      PluginMeta pm = new PluginMeta();
      if (!dynamic) { pm.setIdClass("xyz___"); }
      CssTree.StyleSheet css = css(fromString(input));
      new CssRuleRewriter(pm).rewriteCss(css);
      ArrayConstructor ac = CssRuleRewriter.cssToJs(css);
      assertEquals(golden, render(ac, 160));
    } catch (ParseException ex) {
      fail(input);
    }
  }

  private static String render(ParseTreeNode node, int limit) {
    StringBuilder sb = new StringBuilder();
    JsPrettyPrinter pp = new JsPrettyPrinter(new Concatenator(sb));
    pp.setLineLengthLimit(limit);
    node.render(new RenderContext(pp));
    pp.noMoreTokens();
    return sb.toString();
  }
}
