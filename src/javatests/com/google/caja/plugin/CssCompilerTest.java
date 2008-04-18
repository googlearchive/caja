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
import com.google.caja.parser.js.Statement;
import com.google.caja.util.CajaTestCase;

public class CssCompilerTest extends CajaTestCase {
  public void testSimpleRule() {
    assertCompiledCss(
        "p {color:purple}",
        "[ '.', ' p {\\n  color: purple\\n}' ]");
  }

  public void testClassRule() {
    assertCompiledCss(
        ".foo .bar {color:blue}",
        "[ '.', ' .foo .bar {\\n  color: blue\\n}' ]");
  }

  public void testIdRule() {
    assertCompiledCss(
        "#foo {color:blue}",
        "[ '.', ' #foo-', ' {\\n  color: blue\\n}' ]");
    assertCompiledCss(
        "p#foo #baz{color:blue}",
        "[ '.', ' p#foo-', ' #baz-', ' {\\n  color: blue\\n}' ]");
  }

  public void testBodyMarker() {
    assertCompiledCss(
        "body.ie6 p {color:blue}",
        "[ 'body.ie6 .', ' p {\\n  color: blue\\n}' ]");
    assertCompiledCss(
        "body.ie6#zoicks p {color:blue}",
        "[ 'body.ie6#zoicks-', ' .', ' p {\\n  color: blue\\n}' ]");
    assertCompiledCss(  // Body markers cannot apply to the body directly.
        "body.ie6 {color:blue}",
        "[ '.', ' body.ie6 {\\n  color: blue\\n}' ]");
  }

  public void testCompoundRule() {
    assertCompiledCss(
        "a, b {color:blue}",
        "[ '.', ' a, .', ' b {\\n  color: blue\\n}' ]");
  }

  public void testDescendentRule() {
    assertCompiledCss(
        "#foo > #bar { color: blue }",
        "[ '.', ' #foo-', ' > #bar-', ' {\\n  color: blue\\n}' ]");
  }

  /**
   * "*" selectors should rewrite properly.
   * <a href="http://code.google.com/p/google-caja/issues/detail?id=57">bug</a>
   */
  public void testWildcardSelectors() throws Exception {
    assertCompiledCss(
        "div * { margin: 0; }",
        "[ '.', ' div * {\\n  margin: 0;\\n}' ]");
  }

  private void assertCompiledCss(String input, String golden) {
    try {
      Statement s = new CssCompiler().compileCss(css(fromString(input)));
      assertEquals(golden, stripBoilerPlate(render(s)));
    } catch (ParseException ex) {
      fail(input);
    }
  }

  private String stripBoilerPlate(String s) {
    String pre = "___OUTERS___.emitCss___(";
    String post = ".join(___OUTERS___.getIdClass___()))";
    if (s.startsWith(pre) && s.endsWith(post)) {
      return s.substring(pre.length(), s.length() - post.length());
    }
    return s;
  }
}
