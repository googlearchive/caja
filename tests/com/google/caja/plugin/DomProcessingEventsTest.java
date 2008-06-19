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
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.MoreAsserts;
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
    dpe.begin("b");
    dpe.finishAttrs(false);
    dpe.pcdata("HELLO WORLD");
    dpe.end("b");
    assertEmittingCode(
        "IMPORTS___.htmlEmitter___.b('b').f(false).ih('HELLO WORLD').e('b');",
        "<b>HELLO WORLD</b>",
        dpe);
  }

  public void testEscaping() {
    DomProcessingEvents dpe = new DomProcessingEvents();
    dpe.begin("b");
    dpe.finishAttrs(false);
    dpe.pcdata("1 < 2 && 3 > 2");
    dpe.end("b");
    assertEmittingCode(
        "IMPORTS___.htmlEmitter___.b('b').f(false)"
        + ".ih('1 &lt; 2 &amp;&amp; 3 &gt; 2').e('b');",
        "<b>1 &lt; 2 &amp;&amp; 3 &gt; 2</b>",
        dpe);
  }

  public void testOptimization() {
    DomProcessingEvents dpe = new DomProcessingEvents();
    dpe.begin("b");
    dpe.attr("style", "color: blue");
    dpe.finishAttrs(false);
    dpe.pcdata("[ ");
    dpe.begin("a");
    dpe.attr("href", "#");
    dpe.finishAttrs(false);
    dpe.pcdata("1 < 2 && 3 > 2");
    dpe.end("a");
    dpe.pcdata(" ]");
    dpe.end("b");
    assertEmittingCode(
        "IMPORTS___.htmlEmitter___.b('b').a('style', 'color: blue').f(false)"
        + ".ih('[ <a href=\\\"#\\\">1 &lt; 2 &amp;&amp; 3 &gt; 2</a> ]')"
        + ".e('b');",
        "<b style=\"color: blue\">"
        + "[ <a href=\"#\">1 &lt; 2 &amp;&amp; 3 &gt; 2</a> ]"
        + "</b>",
        dpe);
  }

  public void testInterleaving() throws Exception {
    DomProcessingEvents dpe = new DomProcessingEvents();
    dpe.begin("p");
    dpe.attr("id", "foo");
    dpe.finishAttrs(false);
    dpe.pcdata("hello");
    dpe.end("p");
    dpe.begin("p");
    dpe.finishAttrs(false);
    dpe.script(js(fromString("bar();")));
    dpe.end("p");
    dpe.begin("p");
    dpe.attr("id", "baz");
    dpe.finishAttrs(false);
    dpe.pcdata("world");
    dpe.end("p");
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
    dpe.begin("div");
    dpe.finishAttrs(false);
    dpe.begin("p");
    dpe.finishAttrs(false);
    dpe.pcdata("...On the Night's Plutonian shore!");
    dpe.begin("br");
    dpe.attr("title", "Quoth the <raven>, \"Nevermore.\"");
    dpe.finishAttrs(true);
    dpe.pcdata("Much I marvelled");
    dpe.pcdata(" this ungainly fowl...");
    dpe.end("p");
    dpe.begin("style");
    dpe.attr("type", "text/css");
    dpe.finishAttrs(false);
    dpe.cdata("<!--body{ background:white }-->");
    dpe.end("style");
    dpe.end("div");

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
    dpe.begin("foo");
    dpe.finishAttrs(false);
    dpe.begin("bar");
    dpe.attr("baz", jsExpr(fromString("1 + 1")));
    dpe.finishAttrs(true);
    dpe.end("foo");

    assertEmittingCode(
        "IMPORTS___.htmlEmitter___.b('foo').f(false)"
        + ".b('bar').a('baz', 1 + 1).f(true).e('foo');",
        "<foo><bar baz=\"2\"/></foo>",
        dpe);
  }

  public void testAttribsMustBeClosed() throws Exception {
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin("p");
      dpe.end("p");
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin("p");
      dpe.pcdata("Hello");
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin("p");
      dpe.toJavascript(new Block());
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin("p");
      dpe.toJavascript(new Block());
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin("p");
      dpe.attr("foo", "bar");
      dpe.toJavascript(new Block());
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin("p");
      dpe.begin("p");
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin("p");
      dpe.finishAttrs(false);
      dpe.attr("foo", "bar");
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.attr("foo", "bar");
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
      dpe.begin("p");
      dpe.script(js(fromString("foo();")));
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
  }

  public void testUnbalancedTags() {
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin("p");
      dpe.finishAttrs(false);
      dpe.end("q");
      dpe.toJavascript(new Block());
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin("p");
      dpe.finishAttrs(false);
      dpe.toJavascript(new Block());
      fail();
    } catch (IllegalStateException ex) {
      // pass
    }
    try {
      DomProcessingEvents dpe = new DomProcessingEvents();
      dpe.begin("p");
      dpe.finishAttrs(false);
      dpe.begin("p");
      dpe.finishAttrs(false);
      dpe.end("p");
      dpe.end("p");
      dpe.end("p");
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
      dpe.begin("p");
      dpe.attr("id", x);  // defeat optimization
      dpe.finishAttrs(false);
    }
    for (int i = 0; i < 30; ++i) { dpe.end("p"); }

    Block block = new Block();
    dpe.toJavascript(block);

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
          null,
          new RhinoTestBed.Input(getClass(), "console-stubs.js"),
          new RhinoTestBed.Input(getClass(), "/js/jqueryjs/runtest/env.js"),
          new RhinoTestBed.Input(getClass(), "html-emitter.js"),
          new RhinoTestBed.Input(
              "this.location = " + StringLiteral.toQuotedValue(contentUrl) + ";"
              // Set up the HTML emitter.
              + "var IMPORTS___ = {"
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

  static { TestUtil.enableContentUrls(); }
}
