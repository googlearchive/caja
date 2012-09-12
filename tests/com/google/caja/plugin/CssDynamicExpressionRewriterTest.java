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

import com.google.caja.lang.css.CssSchema;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class CssDynamicExpressionRewriterTest extends CajaTestCase {
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

  // TODO(kpreid): These tests were written to exercise the
  //     body -> .vdoc-body___
  // special case in pre-element-virtualization Caja. Replace these test cases
  // with something providing more useful coverage.
  public final void testBodyMarker() {
    assertCompiledCss(
        "body.ie6 p {color:blue}",
        "[ '.', ' caja-v-body.ie6 p {\\n  color: blue\\n}' ]");
    assertCompiledCss(
        "body.ie6#zoicks p {color:blue}",
        "[ '.', ' caja-v-body.ie6#zoicks-', ' p {\\n  color: blue\\n}' ]");
    assertCompiledCss(
        "body.ie6 {color:blue}",
        "[ '.', ' caja-v-body.ie6 {\\n  color: blue\\n}' ]");
    assertCompiledCss(
        "body { font-size: 12pt }",
        "[ '.', ' caja-v-body {\\n  font-size: 12pt\\n}' ]");
  }

  public final void testCompoundRule() {
    assertCompiledCss(
        "a, b {color:blue}",
        "[ '.', ' a, .', ' b {\\n  color: blue\\n}' ]");
  }

  public final void testDescendentRule() {
    assertCompiledCss(
        "#foo > #bar { color: blue }",
        "[ '.', ' #foo-', ' \\x3e #bar-', ' {\\n  color: blue\\n}' ]");
  }

  /**
   * "*" selectors should rewrite properly.
   * <a href="http://code.google.com/p/google-caja/issues/detail?id=57">bug</a>
   */
  public final void testWildcardSelectors() {
    assertCompiledCss(
        "div * { margin: 0; }",
        "[ '.', ' div * {\\n  margin: 0\\n}' ]");
  }

  public final void testStaticIdClass() {
    assertCompiledCss(
        "#a > #b, .c { color: blue }",
        "[ '.xyz___ #a-xyz___ \\x3e #b-xyz___, .xyz___ .c"
        + " {\\n  color: blue\\n}' ]",
        false);
  }

  public final void testRewriteUnsafeUris1() throws Exception {
    assertRewriteUris(
        "  p { background: url(foo.png) }"
        + "a { background: url(bar.png) }",
        "[ '.', ' p {\\n  background: url(\\'foo.png\\')\\n}\\n.', "
        + "' a {\\n  background: "
        + "url(' + IMPORTS___.rewriteUriInCss___("
        + "'bar.png', 'background') + ')\\n}' ]",
        Arrays.asList("foo.png"),
        Arrays.asList("bar.png"));
  }

  public final void testRewriteUnsafeUris2() throws Exception {
    assertRewriteUris(
        "  p { background: url(\"foo'.png\") }"
        + "a { background: url(\"bar'.png\") }",
        "[ '.', ' p {\\n  background: url(\\'foo%27.png\\')\\n}\\n.', "
        + "' a {\\n  background: "
        + "url(' + IMPORTS___.rewriteUriInCss___("
        + "'bar\\'.png', 'background') + ')\\n}' ]",
        Arrays.asList("foo'.png"),
        Arrays.asList("bar'.png"));
  }

  private void assertRewriteUris(String input,
                                 String golden,
                                 List<String> safeUris,
                                 List<String> unsafeUris)
      throws Exception {
    assertCompiledCss(
        safeUnsafe(
            css(fromString(input)),
            safeUris,
            unsafeUris),
        golden);
  }

  private void assertCompiledCss(String input, String golden) {
    assertCompiledCss(input, golden, true);
  }

  private void assertCompiledCss(CssTree.StyleSheet css, String golden) {
    assertCompiledCss(css, golden, true);
  }

  private void assertCompiledCss(String input, String golden, boolean dynamic) {
    try {
      assertCompiledCss(css(fromString(input)), golden, dynamic);
    } catch (ParseException ex) {
      fail(input);
    }
  }

  private void assertCompiledCss(
      CssTree.StyleSheet css,
      String golden,
      boolean dynamic) {
    PluginMeta pm = new PluginMeta();
    if (!dynamic) { pm.setIdClass("xyz___"); }
    new CssRewriter(null, CssSchema.getDefaultCss21Schema(mq), mq)
        .rewrite(AncestorChain.instance(css));
    new CssDynamicExpressionRewriter(pm).rewriteCss(css);
    ArrayConstructor ac = CssDynamicExpressionRewriter.cssToJs(css);
    assertEquals(golden, render(ac, 160));
  }

  private CssTree.StyleSheet safeUnsafe(CssTree.StyleSheet css,
                                        final List<String> safeUris,
                                        final List<String> unsafeUris) {
    css.acceptPreOrder(new Visitor() {
      public boolean visit(AncestorChain<?> ancestors) {
        ParseTreeNode node = ancestors.node;
        if (node instanceof CssTree.UriLiteral) {
          CssTree parent = (CssTree) ancestors.parent.node;
          assert(null != parent);
          String value = ((CssTree.CssLiteral) node).getValue();
          CssTree.UriLiteral repl = null;
          if (safeUris.contains(value)) {
            repl = new SafeUriLiteral(
                node.getFilePosition(), URI.create(value));
          } else if (unsafeUris.contains(value)) {
            repl = new UnsafeUriLiteral(
                node.getFilePosition(), URI.create(value));
          }
          if (repl != null) {
            parent.replaceChild(repl, node);
          } else {
            fail("URI literal " + value + " unaccounted for by test");
          }
        }
        return true;
      }
    }, null);
    return css;
  }

  private static String render(ParseTreeNode node, int limit) {
    StringBuilder sb = new StringBuilder();
    JsPrettyPrinter pp = new JsPrettyPrinter(sb);
    pp.setLineLengthLimit(limit);
    node.render(new RenderContext(pp));
    pp.noMoreTokens();
    return sb.toString();
  }
}
