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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.tools.BuildCommand;
import com.google.caja.util.Callback;
import com.google.caja.util.Name;
import com.google.caja.util.TestUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.util.List;

public class DomProcessingEventsTestData {
  private final Appendable out;
  private final MessageQueue mq;

  DomProcessingEventsTestData(Appendable out, MessageQueue mq) {
    this.out = out;
    this.mq = mq;
  }

  public static class Builder implements BuildCommand {
    public void build(List<File> inputs, List<File> deps, File output)
        throws IOException {
      MessageQueue mq = new SimpleMessageQueue();
      Writer out = new OutputStreamWriter(
          new FileOutputStream(output), "UTF-8");
      try {
        (new DomProcessingEventsTestData(out, mq)).emit();
      } catch (ParseException ex) {
        // build can't recover from failure to parse a hand-coded test input
        throw new RuntimeException(ex);
      } finally {
        out.close();
      }
      for (Message msg : mq.getMessages()) {
        if (MessageLevel.WARNING.compareTo(msg.getMessageLevel()) <= 0) {
          System.err.println(msg);
        }
      }
    }
  }

  void emit() throws IOException, ParseException {
    DomProcessingEvents dpe;
    dpe = new DomProcessingEvents();
    compileTest("testNoEvents", "", "", dpe);

    dpe = new DomProcessingEvents();
    dpe.pcdata("hello world");
    compileTest(
        "testTextNode",
        "IMPORTS___.htmlEmitter___.pc('hello world');", "hello world", dpe);

    dpe = new DomProcessingEvents();
    dpe.begin(Name.html("b"));
    dpe.finishAttrs(false);
    dpe.pcdata("HELLO WORLD");
    dpe.end(Name.html("b"));
    compileTest(
        "testSimpleElement",
        "IMPORTS___.htmlEmitter___.b('b').f(false).ih('HELLO WORLD').e('b');",
        "<b>HELLO WORLD</b>",
        dpe);

    dpe = new DomProcessingEvents();
    dpe.begin(Name.html("b"));
    dpe.finishAttrs(false);
    dpe.pcdata("1 < 2 && 3 > 2");
    dpe.end(Name.html("b"));
    compileTest(
        "testEscaping",
        "IMPORTS___.htmlEmitter___.b('b').f(false)"
        + ".ih('1 &lt; 2 &amp;&amp; 3 &gt; 2').e('b');",
        "<b>1 &lt; 2 &amp;&amp; 3 &gt; 2</b>",
        dpe);

    dpe = new DomProcessingEvents();
    dpe.begin(Name.html("b"));
    dpe.attr(Name.html("style"), "color: blue");
    dpe.finishAttrs(false);
    dpe.pcdata("[ ");
    dpe.begin(Name.html("a"));
    dpe.attr(Name.html("href"), "http://foo.com/");
    dpe.finishAttrs(false);
    dpe.pcdata("1 < 2 && 3 > 2");
    dpe.end(Name.html("a"));
    dpe.pcdata(" ]");
    dpe.end(Name.html("b"));
    compileTest(
        "testOptimization",
        "IMPORTS___.htmlEmitter___.b('b').a('style', 'color: blue').f(false)"
        + ".ih('[ <a href=\\\""
        + "http://foo.com/\\\">1 &lt; 2 &amp;&amp; 3 &gt; 2</a> ]')"
        + ".e('b');",
        "<b style=\"color: blue\">"
        + "[ <a href=\"http://foo.com/\">1 &lt; 2 &amp;&amp; 3 &gt; 2</a> ]"
        + "</b>",
        dpe);

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
    compileTest(
        "testDeOptimization1",
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
    compileTest(
        "testDeOptimization2",
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
    compileTest(
        "testReDeOptimization1",
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
    compileTest(
        "testReDeOptimization2",
        "IMPORTS___.htmlEmitter___.b('div').f(false)"
        + ".ih('<table><tbody><tr><td>1</td><td>2</td></tr></tbody></table>')"
        + ".e('div');",
        "<div><table><tbody><tr><td>1</td>"
        + "<td>2</td></tr></tbody></table></div>",
        dpe);

    dpe = new DomProcessingEvents();
    dpe.begin(Name.html("p"));
    dpe.attr(Name.html("id"), "foo");
    dpe.finishAttrs(false);
    dpe.pcdata("hello");
    dpe.end(Name.html("p"));
    dpe.begin(Name.html("p"));
    dpe.finishAttrs(false);
    dpe.script(parse("bar();", "testInterleaving"));
    dpe.end(Name.html("p"));
    dpe.begin(Name.html("p"));
    dpe.attr(Name.html("id"), "baz");
    dpe.finishAttrs(false);
    dpe.pcdata("world");
    dpe.end(Name.html("p"));
    compileTest(
        "testInterleaving",
        "IMPORTS___.htmlEmitter___.b('p').a('id', 'foo').f(false)"
        + ".ih('hello')"
        + ".e('p')"
        + ".b('p').f(false);"
        + "{ bar(); }"
        + "IMPORTS___.htmlEmitter___.e('p')"
        + ".b('p').a('id', 'baz').f(false)"
        + ".ih('world')"
        + ".e('p');",

        "<p id=\"foo\">hello</p><p></p>BAR<p id=\"baz\">world</p>",

        dpe);

    dpe = new DomProcessingEvents();
    dpe.begin(Name.html("div"));
    dpe.finishAttrs(false);
    dpe.begin(Name.html("p"));
    dpe.finishAttrs(false);
    dpe.pcdata("...On the Night's Plutonian shore!");
    dpe.begin(Name.html("br"));
    dpe.attr(Name.html("title"), "Quoth the <raven>, \"Nevermore.\"");
    dpe.finishAttrs(true);
    dpe.pcdata("Much I marvelled");
    dpe.pcdata(" this ungainly fowl");
    dpe.end(Name.html("p"));
    dpe.begin(Name.html("textarea"));
    dpe.attr(Name.html("name"), "discourse");
    dpe.finishAttrs(false);
    dpe.pcdata("to hear discourse so <plainly>");
    dpe.end(Name.html("textarea"));
    dpe.pcdata(", ...");
    dpe.end(Name.html("div"));

    compileTest(
        "testRenderingToInnerHtml",
        "IMPORTS___.htmlEmitter___.b('div').f(false).ih('"
        + "<p>...On the Night&#39;s Plutonian shore!"
        + "<br title=\\\"Quoth the &lt;raven&gt;, &quot;Nevermore.&quot;\\\" />"
        + "Much I marvelled this ungainly fowl</p>"
        + "<textarea name=\\\"discourse\\\">"
        + "to hear discourse so &lt;plainly&gt;"
        + "</textarea>, ..."
        + "').e('div');",

        "<div>"
        + "<p>...On the Night's Plutonian shore!"
        + "<br title=\"Quoth the &lt;raven&gt;, &quot;Nevermore.&quot;\">"
        + "Much I marvelled this ungainly fowl</p>"
        + "<textarea name=\"discourse\">"
        + "to hear discourse so &lt;plainly&gt;"
        + "</textarea>, ..."
        + "</div>",

        dpe);

    dpe = new DomProcessingEvents();
    dpe.begin(Name.html("foo"));
    dpe.finishAttrs(false);
    dpe.begin(Name.html("bar"));
    dpe.attr(Name.html("baz"), parseExpr("1 + 1", "testDynamicAttributes"));
    dpe.finishAttrs(true);
    dpe.end(Name.html("foo"));

    compileTest(
        "testDynamicAttributes",
        "IMPORTS___.htmlEmitter___.b('foo').f(false)"
        + ".b('bar').a('baz', 1 + 1).f(true).e('foo');",
        "<foo><bar baz=\"2\"></bar></foo>",
        dpe);
  }

  private void compileTest(
      String testName, String goldenJs, String goldenHtml,
      DomProcessingEvents dpe)
      throws IOException, ParseException {
    Block actual = new Block();
    dpe.toJavascript(actual);
    TestUtil.removePseudoNodes(actual);

    Statement stmt = new ExpressionStmt((Expression) QuasiBuilder.substV(
        ""
        + "jsunitRegister(@nameStr, function @name() {"
        + "    clearTestOutput();"
        + "    @actual;"
        + "    assertEquals(@nameStr + '#js', @goldenJs, @actualSource);"
        // We can't use assertHTMLEquals since IE eats non-standard tags.
        + "    assertEquals(@nameStr + '#html',"
        + "                 @goldenHtml, getTestOutputInnerHTML());"
        + "});",

        "nameStr", StringLiteral.valueOf(testName),
        "name", new Identifier(testName),
        "actual", actual,
        "goldenJs", StringLiteral.valueOf(render(parse(goldenJs, testName))),
        "actualSource", StringLiteral.valueOf(render(actual)),
        "goldenHtml", StringLiteral.valueOf(goldenHtml)
        ));

    render(stmt, out);
    out.append("\n");
  }

  private Parser makeParser(String js, String testName) {
    InputSource is = new InputSource(URI.create("test:///" + testName));
    CharProducer cp = CharProducer.Factory.create(new StringReader(js), is);
    JsLexer lexer = new JsLexer(cp);
    JsTokenQueue tq = new JsTokenQueue(lexer, is, JsTokenQueue.NO_COMMENT);
    return new Parser(tq, mq, false);
  }

  private Block parse(String js, String testName) throws ParseException {
    Parser p = makeParser(js, testName);
    if (p.getTokenQueue().isEmpty()) { return new Block(); }
    Block b = p.parse();
    p.getTokenQueue().expectEmpty();
    return b;
  }

  private Expression parseExpr(String js, String testName)
      throws ParseException {
    Parser p = makeParser(js, testName);
    Expression e = p.parseExpression(true);
    p.getTokenQueue().expectEmpty();
    return e;
  }

  private static void render(Statement node, Appendable out)
      throws IOException {
    TokenConsumer tc = node.makeRenderer(out, new ErrorWrapper());
    try {
      RenderContext rc = new RenderContext(new MessageContext(), tc);
      node.render(rc);
      if (!node.isTerminal()) { tc.consume(";"); }
      tc.noMoreTokens();
    } catch (WrappedIOException ex) {
      throw (IOException) ex.getCause();
    }
  }

  private static String render(ParseTreeNode node) {
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = node.makeRenderer(sb, null);
    RenderContext rc = new RenderContext(new MessageContext(), tc);
    node.render(rc);
    tc.noMoreTokens();
    return sb.toString();
  }

  private static class ErrorWrapper implements Callback<IOException> {
    public void handle(IOException ex) {
      throw new WrappedIOException(ex);
    }
  }
  private static class WrappedIOException extends RuntimeException {
    WrappedIOException(IOException ex) { initCause(ex); }
  }
}
