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
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.TranslatedCode;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.MoreAsserts;
import com.google.caja.util.Name;
import com.google.caja.util.RhinoTestBed;
import com.google.caja.util.TestUtil;

import java.io.IOException;
import java.util.Arrays;

public class DomProcessingEventsTest extends CajaTestCase {
  public void testNoEvents() {
    DomProcessingEvents dpe = new DomProcessingEvents();
    assertEmittingCode("", "", dpe);
  }

  public void testTextNode() {
    DomProcessingEvents dpe = new DomProcessingEvents();
    dpe.pcdata("hello world");
    assertEmittingCode(
        "IMPORTS___.htmlEmitter___.pc('hello world');", "hello world", dpe);
  }

  public void testSimpleElement() {
    DomProcessingEvents dpe = new DomProcessingEvents();
    dpe.begin(Name.html("b"));
    dpe.finishAttrs(false);
    dpe.pcdata("HELLO WORLD");
    dpe.end(Name.html("b"));
    assertEmittingCode(
        "IMPORTS___.htmlEmitter___.b('b').f(false).ih('HELLO WORLD').e('b');",
        "<b>HELLO WORLD</b>",
        dpe);
  }

  public void testEscaping() {
    DomProcessingEvents dpe = new DomProcessingEvents();
    dpe.begin(Name.html("b"));
    dpe.finishAttrs(false);
    dpe.pcdata("1 < 2 && 3 > 2");
    dpe.end(Name.html("b"));
    assertEmittingCode(
        "IMPORTS___.htmlEmitter___.b('b').f(false)"
        + ".ih('1 &lt; 2 &amp;&amp; 3 &gt; 2').e('b');",
        "<b>1 &lt; 2 &amp;&amp; 3 &gt; 2</b>",
        dpe);
  }

  public void testOptimization() {
    DomProcessingEvents dpe = new DomProcessingEvents();
    dpe.begin(Name.html("b"));
    dpe.attr(Name.html("style"), "color: blue");
    dpe.finishAttrs(false);
    dpe.pcdata("[ ");
    dpe.begin(Name.html("a"));
    dpe.attr(Name.html("href"), "#");
    dpe.finishAttrs(false);
    dpe.pcdata("1 < 2 && 3 > 2");
    dpe.end(Name.html("a"));
    dpe.pcdata(" ]");
    dpe.end(Name.html("b"));
    assertEmittingCode(
        "IMPORTS___.htmlEmitter___.b('b').a('style', 'color: blue').f(false)"
        + ".ih('[ <a href=\\\"#\\\">1 &lt; 2 &amp;&amp; 3 &gt; 2</a> ]')"
        + ".e('b');",
        "<b style=\"color: blue\">"
        + "[ <a href=\"#\">1 &lt; 2 &amp;&amp; 3 &gt; 2</a> ]"
        + "</b>",
        dpe);
  }

  public void testDeOptimization() throws Exception {
    DomProcessingEvents dpe;

    // Some elements can't be created at the top level by innerHTML.  So
    //    var select = document.createElement('SELECT');
    //    select.innerHTML = '<option>1</option>;
    // doesn't work on some browsers.
    // But the below does work, so another test below allows <OPTION>s to be
    // optimized properly:
    //    var div = document.createElement('DIV');
    //    div.innerHTML = '<select><option>1</option></select>';
    // See bug 730 for more details.
    dpe = new DomProcessingEvents();
    dpe.begin(Name.html("select"));
    dpe.finishAttrs(false);
    dpe.begin(Name.html("option"));
    dpe.finishAttrs(false);
    dpe.pcdata("1");
    dpe.end(Name.html("option"));
    dpe.begin(Name.html("option"));
    dpe.finishAttrs(false);
    dpe.pcdata("2");
    dpe.end(Name.html("option"));
    dpe.end(Name.html("select"));
    assertEmittingCode(
        "IMPORTS___.htmlEmitter___.b('select').f(false)"
        + ".b('option')"
        + ".f(false)"
        + ".ih('1')"
        + ".e('option')"
        + ".b('option')"
        + ".f(false)"
        + ".ih('2')"
        + ".e('option')"
        + ".e('select');",
        "<select><option>1</option><option>2</option></select>",
        dpe);

    dpe = new DomProcessingEvents();
    dpe.begin(Name.html("table"));
    dpe.finishAttrs(false);
    dpe.begin(Name.html("tbody"));
    dpe.finishAttrs(false);
    dpe.begin(Name.html("tr"));
    dpe.finishAttrs(false);
    dpe.begin(Name.html("td"));
    dpe.finishAttrs(false);
    dpe.pcdata("1");
    dpe.end(Name.html("td"));
    dpe.begin(Name.html("td"));
    dpe.finishAttrs(false);
    dpe.pcdata("2");
    dpe.end(Name.html("td"));
    dpe.end(Name.html("tr"));
    dpe.end(Name.html("tbody"));
    dpe.end(Name.html("table"));
    assertEmittingCode(
        "IMPORTS___.htmlEmitter___.b('table').f(false)"
        + ".b('tbody')"
        + ".f(false)"
        + ".b('tr')"
        + ".f(false)"
        + ".b('td')"
        + ".f(false)"
        + ".ih('1')"
        + ".e('td')"
        + ".b('td')"
        + ".f(false)"
        + ".ih('2')"
        + ".e('td')"
        + ".e('tr')"
        + ".e('tbody')"
        + ".e('table');",
        "<table><tbody><tr><td>1</td><td>2</td></tr></tbody></table>",
        dpe);
  }

  public void testReDeOptimization() throws Exception {
    DomProcessingEvents dpe;

    dpe = new DomProcessingEvents();
    dpe.begin(Name.html("div"));
    dpe.finishAttrs(false);
    dpe.begin(Name.html("select"));
    dpe.finishAttrs(false);
    dpe.begin(Name.html("option"));
    dpe.finishAttrs(false);
    dpe.pcdata("1");
    dpe.end(Name.html("option"));
    dpe.begin(Name.html("option"));
    dpe.finishAttrs(false);
    dpe.pcdata("2");
    dpe.end(Name.html("option"));
    dpe.end(Name.html("select"));
    dpe.end(Name.html("div"));
    assertEmittingCode(
        "IMPORTS___.htmlEmitter___.b('div').f(false)"
        + ".ih('<select><option>1</option><option>2</option></select>')"
        + ".e('div');",
        "<div><select><option>1</option><option>2</option></select></div>",
        dpe);

    dpe = new DomProcessingEvents();
    dpe.begin(Name.html("div"));
    dpe.finishAttrs(false);
    dpe.begin(Name.html("table"));
    dpe.finishAttrs(false);
    dpe.begin(Name.html("tbody"));
    dpe.finishAttrs(false);
    dpe.begin(Name.html("tr"));
    dpe.finishAttrs(false);
    dpe.begin(Name.html("td"));
    dpe.finishAttrs(false);
    dpe.pcdata("1");
    dpe.end(Name.html("td"));
    dpe.begin(Name.html("td"));
    dpe.finishAttrs(false);
    dpe.pcdata("2");
    dpe.end(Name.html("td"));
    dpe.end(Name.html("tr"));
    dpe.end(Name.html("tbody"));
    dpe.end(Name.html("table"));
    dpe.end(Name.html("div"));
    assertEmittingCode(
        "IMPORTS___.htmlEmitter___.b('div').f(false)"
        + ".ih('<table><tbody><tr><td>1</td><td>2</td></tr></tbody></table>')"
        + ".e('div');",
        "<div><table><tbody><tr><td>1</td>"
        + "<td>2</td></tr></tbody></table></div>",
        dpe);
  }

  public void testInterleaving() throws Exception {
    DomProcessingEvents dpe = new DomProcessingEvents();
    dpe.begin(Name.html("p"));
    dpe.attr(Name.html("id"), "foo");
    dpe.finishAttrs(false);
    dpe.pcdata("hello");
    dpe.end(Name.html("p"));
    dpe.begin(Name.html("p"));
    dpe.finishAttrs(false);
    dpe.script(js(fromString("bar();")));
    dpe.end(Name.html("p"));
    dpe.begin(Name.html("p"));
    dpe.attr(Name.html("id"), "baz");
    dpe.finishAttrs(false);
    dpe.pcdata("world");
    dpe.end(Name.html("p"));
    assertEmittingCode(
        "IMPORTS___.htmlEmitter___.b('p').a('id', 'foo').f(false)"
        + ".ih('hello')"
        + ".e('p')"
        + ".b('p').f(false);"
        + "{ bar(); }"
        + "IMPORTS___.htmlEmitter___.e('p')"
        + ".b('p').a('id', 'baz').f(false)"
        + ".ih('world')"
        + ".e('p');",

        "<p id=\"foo\">hello</p><p/>BAR<p id=\"baz\">world</p>",

        dpe);
  }

  public void testRenderingToInnerHtml() {
    DomProcessingEvents dpe = new DomProcessingEvents();
    dpe.begin(Name.html("div"));
    dpe.finishAttrs(false);
    dpe.begin(Name.html("p"));
    dpe.finishAttrs(false);
    dpe.pcdata("...On the Night's Plutonian shore!");
    dpe.begin(Name.html("br"));
    dpe.attr(Name.html("title"), "Quoth the <raven>, \"Nevermore.\"");
    dpe.finishAttrs(true);
    dpe.pcdata("Much I marvelled");
    dpe.pcdata(" this ungainly fowl...");
    dpe.end(Name.html("p"));
    dpe.begin(Name.html("style"));
    dpe.attr(Name.html("type"), "text/css");
    dpe.finishAttrs(false);
    dpe.cdata("<!--body{ background:white }-->");
    dpe.end(Name.html("style"));
    dpe.end(Name.html("div"));

    assertEmittingCode(
        "IMPORTS___.htmlEmitter___.b('div').f(false).ih('"
        + "<p>...On the Night&#39;s Plutonian shore!"
        + "<br title=\\\"Quoth the &lt;raven&gt;, &quot;Nevermore.&quot;\\\" />"
        + "Much I marvelled this ungainly fowl...</p>"
        + "<style type=\\\"text/css\\\"><!--body{ background:white }--></style>"
        + "').e('div');",

        "<div>"
        + "<p>...On the Night's Plutonian shore!"
        + "<br title=\"Quoth the &lt;raven&gt;, &quot;Nevermore.&quot;\"/>"
        + "Much I marvelled this ungainly fowl...</p>"
        + "<style type=\"text/css\">body{ background:white }</style>"
        + "</div>",

        dpe);
  }

  public void testDynamicAttributes() throws Exception {
    DomProcessingEvents dpe = new DomProcessingEvents();
    dpe.begin(Name.html("foo"));
    dpe.finishAttrs(false);
    dpe.begin(Name.html("bar"));
    dpe.attr(Name.html("baz"), jsExpr(fromString("1 + 1")));
    dpe.finishAttrs(true);
    dpe.end(Name.html("foo"));

    assertEmittingCode(
        "IMPORTS___.htmlEmitter___.b('foo').f(false)"
        + ".b('bar').a('baz', 1 + 1).f(true).e('foo');",
        "<foo><bar baz=\"2\"/></foo>",
        dpe);
  }

  public void testAttribsMustBeClosed() throws Exception {
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.end(Name.html("p"));
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.pcdata("Hello");
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.toJavascript(new Block());
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.toJavascript(new Block());
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.attr(Name.html("foo"), "bar");
      dpe.toJavascript(new Block());
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.begin(Name.html("p"));
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.finishAttrs(false);
      dpe.attr(Name.html("foo"), "bar");
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.attr(Name.html("foo"), "bar");
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.finishAttrs(false);
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.script(js(fromString("foo();")));
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
  }

  public void testUnbalancedTags() {
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.finishAttrs(false);
      dpe.end(Name.html("q"));
      dpe.toJavascript(new Block());
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.finishAttrs(false);
      dpe.toJavascript(new Block());
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin(Name.html("p"));
      dpe.finishAttrs(false);
      dpe.begin(Name.html("p"));
      dpe.finishAttrs(false);
      dpe.end(Name.html("p"));
      dpe.end(Name.html("p"));
      dpe.end(Name.html("p"));
      dpe.toJavascript(new Block());
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
  }

  public void testTooMuchRecursionFix() throws Exception {
    Expression x = jsExpr(fromString("x"));
    DomProcessingEvents dpe = new DomProcessingEvents();
    for (int i = 0; i < 30; ++i) {
      dpe.begin(Name.html("p"));
      dpe.attr(Name.html("id"), x);  // defeat optimization
      dpe.finishAttrs(false);
    }
    for (int i = 0; i < 30; ++i) { dpe.end(Name.html("p")); }

    Block block = new Block();
    dpe.toJavascript(block);
    removePseudoNodes(block);

    String prefix = "\n  IMPORTS___.htmlEmitter___";
    String startOne = ".b('p').a('id', x).f(false)";
    String startTen = (startOne + startOne + startOne + startOne + startOne
                       + startOne + startOne + startOne + startOne + startOne);
    String endOne = ".e('p')";
    String endTen = (endOne + endOne + endOne + endOne + endOne
                     + endOne + endOne + endOne + endOne + endOne);

    assertEquals(
        "{"
        + prefix + startTen + startTen + startTen + endTen + ";"
        // Split across two lines
        + prefix + endTen + endTen + ";"
        + "\n}",
        render(block));
  }

  private void assertEmittingCode(
      String goldenJs, String goldenHtml, DomProcessingEvents dpe) {
    Block actual = new Block();
    dpe.toJavascript(actual);
    removePseudoNodes(actual);
    try {
      Block golden = ("".equals(goldenJs)
                      ? new Block()
                      : js(fromString(goldenJs)));
      MoreAsserts.assertListsEqual(
          Arrays.asList(golden.toStringDeep().split("\n")),
          Arrays.asList(actual.toStringDeep().split("\n")));
    } catch (ParseException ex) {
      fail(goldenJs);
    }

    try {
      String contentUrl = TestUtil.makeContentUrl(
          "<html><head/><body><div id=\"base\"/></body></html>");
      String actualHtml = (String) RhinoTestBed.runJs(
          new RhinoTestBed.Input(getClass(), "console-stubs.js"),
          new RhinoTestBed.Input(getClass(), "/js/jqueryjs/runtest/env.js"),
          new RhinoTestBed.Input(
              "this.location = " + StringLiteral.toQuotedValue(contentUrl),
              "setup-document"),
          new RhinoTestBed.Input(getClass(), "bridal.js"),
          new RhinoTestBed.Input(getClass(), "html-emitter.js"),
          new RhinoTestBed.Input(
              // Set up the HTML emitter.
              "var IMPORTS___ = {"
              + "  htmlEmitter___: new HtmlEmitter("
              + "      document.getElementById('base'))"
              + "};"
              // Define a function that can be called from a script block to
              // interleave an operation that modifies the DOM.
              + "function bar() {"
              + "  document.getElementById('base')"
              + "      .appendChild(document.createTextNode('BAR'));"
              + "}",
              getName()),
          new RhinoTestBed.Input(goldenJs, getName()),
          // Return the rendered HTML for comparison against golden.
          new RhinoTestBed.Input(
              "document.getElementById('base').innerHTML", getName())
          );
      assertEquals(goldenHtml, actualHtml);
    } catch (IOException ex) {
      fail(ex.toString());
    }
  }

  static void removePseudoNodes(ParseTreeNode node) {
    assert !(node instanceof TranslatedCode);
    node.acceptPostOrder(new Visitor() {
        public boolean visit(AncestorChain<?> ac) {
          if (ac.node instanceof TranslatedCode) {
            ((MutableParseTreeNode) ac.parent.node).replaceChild(
                ((TranslatedCode) ac.node).getTranslation(), ac.node);
          }
          return true;
        }
      }, null);
  }

  static { TestUtil.enableContentUrls(); }
}
