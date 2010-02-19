// Copyright (C) 2010 Google Inc.
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

package com.google.caja.ancillary.servlet;

import com.google.caja.util.CajaTestCase;

public class HtmlReducerTest extends CajaTestCase {
  public final void testEmpty() throws Exception {
    assertSameReduced("");
  }

  public final void testHtml() throws Exception {
    assertReduced("", "<html></html>");
    assertReduced("<html lang=en>", "<html lang=en></html>");
    assertReduced("<html><!--comment-->", "<html><!--comment--></html>");
    assertReduced("</html><!--comment-->", "<html></html><!--comment-->");
  }

  public final void testHead() throws Exception {
    assertReduced("", "<head></head>");
    assertReduced("<title></title>", "<head><title></title></head>");
    assertReduced("<head><!-- comment -->", "<head><!-- comment --></head>");
    assertReduced("</head> ", "<head></head> ");
  }

  public final void testBody() throws Exception {
    assertReduced("", "<body></body>");
    assertReduced(
        "<body><script>alert('Hello World')</script>",
        "<body><script>alert('Hello World')</script></body>");
    assertReduced(
        "<body><style>p { color: pink }</style>",
        "<body><style>p { color: pink }</style></body>");
    assertReduced("<div>foo</div>", "<body><div>foo</div></body>");
    assertReduced("</body><!--comment-->", "<body></body><!--comment-->");
    assertSameReduced(
        "<body><script>alert('Hello World')</script></body><!--comment-->");
  }

  public final void testListItem() throws Exception {
    assertReduced(
        "<ol><li>One<li>Two<li>Three</li> <li>Four</ol>",
        "<ol><li>One</li><li>Two</li><li>Three</li> <li>Four</li></ol>");
    assertReduced(
        "<ul><li>One<li>Two<li>Three</li> </ul>",
        "<ul><li>One</li><li>Two</li><li>Three</li> </ul>");
  }

  public final void testDefinitionList() throws Exception {
    assertReduced(
        "<dl><dd>Foo<dt>bar<dd>baz<dd>boo</dl>",
        "<dl><dd>Foo</dd><dt>bar</dt><dd>baz</dd><dd>boo</dd></dl>");
  }

  public final void testParagraphs() throws Exception {
    assertReduced(
        "<p>x <a>y</a><p>z<p>w<pre>c</pre><p>a</p><b>b</b>",
        "<p>x <a>y</a></p><p>z</p><p>w</p><pre>c</pre><p>a</p><b>b</b>");
  }

  public final void testOption() throws Exception {
    assertReduced("<option>", "<option></option>");
    assertReduced("<optgroup>", "<optgroup></optgroup>");
    assertReduced(
        "<select><option></select>", "<select><option></option></select>");
    assertReduced(
        "<select><optgroup></select>",
        "<select><optgroup></optgroup></select>");
    assertReduced(
        "<select><optgroup><option><option></select>",
        ""
        + "<select><optgroup><option></option><option></option></optgroup>"
        + "</select>");
    assertReduced(
        "<select><optgroup><option><option></optgroup><option></select>",
        ""
        + "<select><optgroup><option></option><option></option></optgroup>"
        + "<option></option></select>");
  }

  public final void testColumns() throws Exception {
    assertReduced(
        "<colgroup><col /><col /><colgroup><col />",
        "<colgroup><col /><col /></colgroup><colgroup><col /></colgroup>");
  }

  public final void testTableSections() throws Exception {
    assertSameReduced("<table></table>");
    assertReduced("<table><tbody></table>", "<table><tbody></tbody></table>");
    assertReduced(
        "<table><tr></table>", "<table><tbody><tr></tr></tbody></table>");
    assertReduced(
        "<table><thead><tbody><tfoot></table>",
        "<table><thead></thead><tbody></tbody><tfoot></tfoot></table>");
  }

  public final void testRows() throws Exception {
    assertReduced(
        "<table><tr><td>foo<th>bar<td>baz</table>",
        "<table><tbody><tr><td>foo</td><th>bar</th><td>baz</td></tbody></table>"
        );
  }

  public final void testAttributes() throws Exception {
    assertReduced(
        "<tbody xml:lang=\"en\"><tr>",
        "<tbody xml:lang=\"en\"><tr></tr></tbody>");
  }

  private void assertReduced(String expected, String input) throws Exception {
    StringBuilder actual = new StringBuilder();
    HtmlReducer.reduce(input, actual);
    assertEquals(expected, actual.toString());
  }
  private void assertSameReduced(String html) throws Exception {
    assertReduced(html, html);
  }
}
