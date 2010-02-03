// Copyright (C) 2009 Google Inc.
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

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.util.Assertion;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;
import com.google.caja.util.Pair;
import com.google.caja.util.Sets;
import com.google.caja.util.Strings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import junit.framework.AssertionFailedError;
import org.w3c.dom.Element;

public class CajaWebToolsServletTest extends CajaTestCase {
  CajaWebToolsServlet servlet;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    servlet = new CajaWebToolsServlet("cid", null);
  }

  @Override
  public void tearDown() throws Exception {
    // Make sure we don't construct a test and then not run it.
    assertTrue(tests.isEmpty());
    super.tearDown();
  }

  public final void testBadPath() {
    new ServletTest()
        .get("/bogus")
        .expectStatus(404)
        .expectContentType("text/html; charset=UTF-8")
        .expectContentMatches(
            "File not found /bogus.  Expected a path in \\[.*\\]")
        .send();
    assertNoErrors();
  }

  public final void testIndex() {
    new ServletTest()
        .get("/index")
        .expectStatus(200)
        .expectContentType("text/html; charset=UTF-8")
        .expectContentMatches("<form\\b[^>]*>")
        .expectContentMatches(
            "<textarea[^>]*>&lt;script&gt;&lt;/script&gt;</textarea>")
        .send();
    assertNoErrors();
  }

  public final void testIndexWithPresuppliedInput() {
    new ServletTest()
        .get("/index")
        .param("i", "<p>Hello, World!</p>")
        .expectStatus(200)
        .expectContentType("text/html; charset=UTF-8")
        .expectContentMatches("<form\\b[^>]*>")
        .expectContentMatches(
            "<textarea[^>]*>&lt;p&gt;Hello, World!&lt;/p&gt;</textarea>")
        .send();
    assertNoErrors();
  }

  public final void testIndexWithBadParam() {
    new ServletTest()
        .get("/index")
        .param("bogus", "foo")
        .expectStatus(400)
        .expectContentType("text/html; charset=UTF-8")
        .expectContentMatches("Unrecognized param bogus")
        .send();
    assertNoErrors();
  }

  public final void testEcho() {
    new ServletTest()
        .get("/echo")
        .param("it", "text/javascript")
        .param("i", "t = Date.now ? Date.now() : (new Date).getTime()")
        .param("ot", "JS")
        .param("minify", "no")
        .expectStatus(200)
        .expectContentType("text/javascript; charset=UTF-8")
        .expectContent("t = Date.now? Date.now(): (new Date).getTime();")
        .send();
    assertNoErrors();
  }

  public final void testMinify() {
    new ServletTest()
        .get("/echo")
        .param("it", "text/javascript")
        .param("i", "t = Date.now ? Date.now() : (new Date).getTime()")
        .param("ot", "JS")
        .expectStatus(200)
        .expectContentType("text/javascript; charset=UTF-8")
        .expectContent("t=Date.now?Date.now():(new Date).getTime()")
        .send();
    assertNoErrors();
  }

  public final void testMinifiedWithUA1() {
    new ServletTest()
        .get("/echo")
        .param("it", "text/javascript")
        .param("i", "t = Date.now ? Date.now() : +(new Date)")
        .param("ot", "JS")
        .param("userAgent", "Firefox")
        .expectStatus(200)
        .expectContentType("text/javascript; charset=UTF-8")
        .expectContent("t=Date.now()")
        .send();
    new ServletTest()
        .get("/echo")
        .param("it", "text/javascript")
        .param("i", "t = Date.now ? Date.now() : +(new Date)")
        .param("ot", "JS")
        .param("userAgent", "MSIE")
        .expectStatus(200)
        .expectContentType("text/javascript; charset=UTF-8")
        .expectContent("t=+new Date")
        .send();
    assertNoErrors();
  }

  public final void testMinifiedWithUA2() {
    new ServletTest()
        .get("/echo")
        .param("it", "text/javascript")
        .param("i",
               (""
                + "if (window.addEventListener) {\n"
                + "  window.addEventListener(foo);\n"
                + "} else if (window.attachEvent) {\n"
                + "  window.attachEvent(foo);\n"
                + "}"))
        .param("ot", "JS")
        .param("userAgent", "Firefox")
        .expectStatus(200)
        .expectContentType("text/javascript; charset=UTF-8")
        .expectContent("addEventListener(foo)")
        .send();
    assertNoErrors();
  }

  public final void testMinifiedWithUA3() {
    new ServletTest()
        .get("/echo")
        .param("it", "text/javascript")
        .param("i",
               (""
                + "if (window.addEventListener) {\n"
                + "  window.addEventListener(foo);\n"
                + "} else if (window.attachEvent) {\n"
                + "  window.attachEvent(foo);\n"
                + "}"))
        .param("ot", "JS")
        .param("userAgent", "MSIE")
        .expectStatus(200)
        .expectContentType("text/javascript; charset=UTF-8")
        .expectContent("attachEvent(foo)")
        .send();
    assertNoErrors();
  }

  public final void testMinifyHtml() {
    new ServletTest()
        .get("/echo")
        .param("it", "text/html")
        .param(
            "i",
            ""
            + "<script type=text/javascript>\n"
            + "alert('Hello, World!')\n"
            + "</script>\n"
            + "<ul>\n"
            + "  <li onclick='if (foo()) return bar(); return false'>One</li>\n"
            + "  <li><a href=javascript:baz()>Two</a></li>\n"
            + "  <li>Three</li>\n"
            + "</ul>")
        .param("ot", "HTML")
        .param("minify", "t")
        .param("opt", "t")
        .param("userAgent", "*")
        .expectStatus(200)
        .expectContentType("text/html; charset=UTF-8")
        .expectContent(
            ""
            + "<script>alert('Hello, World!')</script>\n"
            + "<ul><li onclick=\"return foo()?bar():false\">One</li>"
            + "<li><a href=\"javascript:baz%28%29\">Two</a></li>"
            + "<li>Three</li></ul>")
        .send();
    assertNoErrors();
  }

  public final void testMinifyCss1() {
    new ServletTest()
        .get("/echo")
        .param("it", "text/css")
        .param(
            "i",
            ""
            + "p { color: red;"
            + "    background-color: magenta;"
            + "    outline-color: #ff0000;"
            + "    border-top-color: blue; }")  // not simplified to border
        .param("ot", "CSS")
        .param("minify", "t")
        .param("opt", "t")
        .param("userAgent", "*")
        .expectStatus(200)
        .expectContentType("text/css; charset=UTF-8")
        .expectContent(
            "p{color:red;background:#f0f;outline:#f00;border-top:blue}")
        .send();
    assertNoErrors();
  }

  public final void testMinifyCss2() {
    new ServletTest()
        .get("/echo")
        .param("it", "text/css")
        .param(
            "i",
            // Can't reduce both to background.
            "p { background-color: darkblue; background-image: url(foo.gif); }")
        .param("ot", "CSS")
        .param("minify", "t")
        .param("opt", "t")
        .param("userAgent", "*")
        .expectStatus(200)
        .expectContentType("text/css; charset=UTF-8")
        .expectContent(
            "p{background-color:#00008b;background-image:url('foo.gif')}")
        .send();
    assertNoErrors();
  }

  public final void testMinifyCss3() {
    new ServletTest()
        .get("/echo")
        .param("it", "text/css")
        .param(
            "i",
            // Valid, but can't reduce.
            ""
            + "p { background-image: purple; }"
            + "q { background-color: purple; }"
            + "s { background-image: 'foo.gif'; }")
        .param("ot", "CSS")
        .param("minify", "t")
        .param("opt", "t")
        .param("userAgent", "*")
        .expectStatus(200)
        .expectContentType("text/css; charset=UTF-8")
        .expectContent(
            ""
            + "p{background-image:purple}"
            + "q{background:purple}"
            + "s{background:'foo.gif'}")
        .send();
    assertNoErrors();
  }

  public final void testRenamed() {
    new ServletTest()
        .get("/echo")
        .param("it", "text/javascript")
        .param("i", (""
                     + "function hypotenuse(xCoord, yCoord) {"
                     + " return Math.sqrt(xCoord * xCoord + yCoord * yCoord);"
                     + "}"))
        .param("ot", "JS")
        .expectStatus(200)
        .expectContentType("text/javascript; charset=UTF-8")
        .expectContent("function hypotenuse(a,b){return Math.sqrt(a*a+b*b)}")
        .send();
    assertNoErrors();
  }

  public final void testAsciiOnly() {
    new ServletTest()
        .get("/echo")
        .param("it", "text/javascript")
        .param("i", "alert('\u1234')")
        .param("ot", "JS")
        .param("asciiOnly", "t")
        .expectStatus(200)
        .expectContentType("text/javascript; charset=UTF-8")
        .expectContent("alert('\\u1234')")
        .send();
    assertNoErrors();
  }

  public final void testHtmlInputs() {
    new ServletTest()
        .get("/echo")
        .param("i", "<script>function hi(msg) { alert(msg); }</script>")
        .expectStatus(200)
        .expectContentType("text/html; charset=UTF-8")
        .expectHeader("Content-disposition",
                      "attachment; filename=\"caja_tools_output.html\"")
        .expectContent("<script>function hi(a){alert(a)}</script>")
        .send();
    assertNoErrors();
  }


  public final void testLint() {
    new ServletTest()
        .get("/lint")
        .param("i", (""
                     + "/**\n"
                     + " * @requires alert\n"
                     + " * @provides foo\n"
                     + " */\n"
                     + "var foo = (function () {\n"
                     + "  var x, y = 1, z;\n"
                     + "  z = 2;\n"
                     + "  return x + y + z;\n"
                     + "})();"))
        .expectStatus(200)
        .expectContentType("text/html; charset=UTF-8")
        .expectContentMatches(
            ""
            + "<h4 title=\"SYMBOL_NOT_LIVE\">[^<.]+\\.js:8\\+10 - 11:"
            + " Symbol x may be used before being initialized</h4>")
        .expectContentMatches(
            "return <span class=\"problem\">x</span> \\+ y \\+ z;")
        .expectContentMatches(
            ""
            + "<h4 title=\"UNUSED_REQUIRE\">[^<.]+\\.js:"
            + " @requires alert not used</h4>")
        .send();
    assertNoErrors();
  }

  public final void testLintIgnoresCajaWhitelist() {
    new ServletTest()
        .get("/lint")
        .param("it", "text/css")
        .param("i", ("p { position: fixed }\n"  // Disallowed in Caja
                     + "q { position: relative }\n"  // OK in both
                     + "s { position: bogus }"))  // Malformed
        .expectStatus(200)
        .expectContentType("text/html; charset=UTF-8")
        .expectContentMatches("<h2 class=\"summary\">1 Warning </h2>")
        .expectContentMatches("<span class=\"problem\">bogus</span>")
        .send();
    assertNoErrors();
  }

  public final void testHtmlLint() {
    new ServletTest()
        .get("/lint")
        .param("i", ("<select value=foo><option></option></select>"))
        .expectStatus(200)
        .expectContentType("text/html; charset=UTF-8")
        .expectContentMatches(
            ""
            + "<h4 title=\"UNKNOWN_ATTRIB\">[^<.]+\\.html:1\\+9 - 14:"
            + " Unknown attribute value on &lt;select&gt;</h4>")
        .expectContentMatches(
            "&lt;select <span class=\"problem\">value</span>=foo&gt;")
        .send();
    assertNoErrors();
  }

  public final void testHtmlDocEcho() {
    new ServletTest()
        .get("/echo")
        .param("i", "<html><head><title>Hello</title></hed><body>World!</body>")
        .param("minLevel", "LOG")
        .expectStatus(200)
        .expectContentType("text/html; charset=UTF-8")
        .expectHeader("Content-disposition",
                      "attachment; filename=\"caja_tools_output.html\"")
        .expectContent(
            "<html><head><title>Hello</title></head><body>World!</body></html>")
        .send();
    assertNoErrors();
  }

  public final void testDoc() {
    new ServletTest()
        .get("/doc")
        .param("ip", "frobbit.js")
        .param("i", "/** Frobbits nobbits */function frobbit(nobbit) {}")
        .param("ot", "JSON")
        .expectStatus(200)
        .expectContentType("application/json; charset=UTF-8")
        .expectContentMatches(
            " \"frobbit\": \\{\\s*\"@description\": \"Frobbits nobbits \",")
        .send();
  }

  public final void testDocJar() {
    new ServletTest()
        .get("/doc")
        .param("ip", "foo.js")
        .param("i", "/**@fileoverview foo*/ /**Foo*/var foo = {};")
        .param("ip", "bar.js")
        .param("i", "/**@fileoverview bar*/ /**Bar*/var bar = {};")
        .expectStatus(200)
        .expectContentType("application/zip")
        .expectZip()
        .zipFileExists("/jsdoc/")
        .zipFileExists("/jsdoc/jsdoc.json")
        .zipFileExists("/jsdoc/index.html")
        .zipFileExists("/jsdoc/src-bar.js.html")
        .zipFileExists("/jsdoc/src-foo.js.html")
        .zipFileExists("/jsdoc/file-bar.js.html")
        .zipFileExists("/jsdoc/file-foo.js.html")
        .send();
  }

  public final void testHelp() {
    new ServletTest()
        .get("/help")
        .expectStatus(200)
        .expectContentType("text/html; charset=UTF-8")
        .expectContentMatches("<a href=\"index\">index</a>")
        .expectContentMatches(
            "<tr><th>&amp;i=\u2026</th><td>an input source file</td></tr>")
        .send();
  }

  public final void testLintPageTips() {
    new ServletTest()
        .get("/lint")
        .param("it", "text/javascript")
        .param("i", "avast('foo')")
        .expectStatus(200)
        .expectContentType("text/html; charset=UTF-8")
        .expectContentMatches(
            // Since there's a file under ancillary/servlet/files describing
            // the error message, there should be a tip link.
            ""
            + "<a class=\"help\" href=\"files-cid/UNDEFINED_SYMBOL_tip.html\""
            + " target=\"help\">")
        .send();
  }

  public final void testExample1() throws Exception {
    new ServletTest()
        .get("/lint")
        .param("it", "text/html")
        .param("i", exampleCode("Whacky HTML"))
        .param("opt", "true")
        .param("minify", "true")
        .expectStatus(200)
        .expectContentType("text/html; charset=UTF-8")
        .expectContentMatches(
            ":3\\+18 - 23:"
            + " End tag &#39;h2&#39; seen but there were unclosed elements")
        .expectContentMatches(
            "&lt;h1&gt;Hello, World!<span class=\"problem\">&lt;/h2&gt;</span>"
            )
        .expectContentMatches(
            ":2\\+15 - 25: Symbol HelloWorld has not been defined")
        .expectContentMatches(
            "&lt;script&gt;alert\\(<span class=\"problem\">HelloWorld</span>\\)"
            )
        .expectContentMatches(
            "\\Q"  // Quote special characters until next \E
            + "<h2 class=\"summary\">1 Error, 1 Lint"
            + " (121B \u2192 86B; output is 71.1% of the original)</h2>"
            + "\\E"
            )
        .expectContentMatches(
            "\\Q"
            + "&lt;style&gt;p{color:pink}&lt;/style&gt;\n"
            + "&lt;script&gt;alert(HelloWorld)&lt;/script&gt;\n"
            + "&lt;h1&gt;Hello, World!&lt;/h1&gt;"
            + "\\E"
            )
        .send();
  }

  public final void testExample2() throws Exception {
    new ServletTest()
        .get("/lint")
        .param("it", "text/css")
        .param("i", exampleCode("Whacky CSS"))
        .param("opt", "true")
        .param("minify", "true")
        .expectStatus(200)
        .expectContentType("text/html; charset=UTF-8")
        .expectContentMatches(":1\\+12 - 22: unknown tag blockquite")
        .expectContentMatches("3\\+3 - 9: unknown css property colour")
        .expectContentMatches(
            "\\Q"  // Quote special characters until next \E
            + "<h2 class=\"summary\">2 Errors"
            + " (70B \u2192 48B; output is 68.6% of the original)</h2>"
            + "\\E"
            )
        .expectContentMatches(
            "\\Qh1,p.bar,blockquite{background:#f0f;colour:blue}\\E"
            )
        .send();
  }

  public final void testIndexPageLinks() throws IOException {
    // Check that all the local links from the index page are live.
    Result result = new ServletTest().get("/index")
        .expectStatus(200).send();
    Matcher m = Pattern.compile("\"files[-\\w]*/([^\"]*)\"")
        .matcher(result.content.getText());
    int nLinks = 0;
    while (m.find()) {
      String relUrl = "files/" + Nodes.decode(m.group(1));
      assertTrue(relUrl, servlet.staticFiles.exists(relUrl));
      ++nLinks;
    }
    assertTrue(nLinks != 0);
  }

  private Set<ServletTest> tests = Sets.newIdentityHashSet();
  private class ServletTest {
    private final List<Assertion> reqs = Lists.newArrayList();
    private String path;
    private List<Pair<String, String>> params = Lists.newArrayList();
    private Result result;

    ServletTest() { tests.add(this); }

    ServletTest get(String path) {
      assertNull(this.path);
      assertNotNull(path);
      this.path = path;
      return this;
    }

    ServletTest param(String key, String value) {
      assertNotNull(key);
      assertNotNull(value);
      params.add(Pair.pair(key, value));
      return this;
    }

    ServletTest expectStatus(final int status) {
      reqs.add(new Assertion() {
        public void test() throws AssertionFailedError {
          if (status != result.status) {
            try {
              result.content.toOutputStream(System.err);
              System.err.println();
            } catch (IOException ex) {
              ex.printStackTrace();
            }
          }
          assertEquals(status, result.status);
        }
      });
      return this;
    }

    ServletTest expectContentType(final String contentType) {
      reqs.add(new Assertion() {
        public void test() throws AssertionFailedError {
          assertEquals(contentType, result.getContentType());
        }
      });
      return this;
    }

    ServletTest expectHeader(final String name, final String value) {
      reqs.add(new Assertion() {
        public void test() throws AssertionFailedError {
          for (Pair<String, String> header : result.headers) {
            if (name.equals(header.a) && value.equals(header.b)) { return; }
          }
          fail("Missing header " + name + ":" + value
               + " not in " + result.headers);
        }
      });
      return this;
    }

    ServletTest expectContentMatches(String re) {
      return expectContentMatches(Pattern.compile(re));
    }

    ServletTest expectContentMatches(final Pattern p) {
      reqs.add(new Assertion() {
        public void test() throws AssertionFailedError {
          try {
            String text = result.content.getText();
            assertTrue(
                "Does not match " + p.pattern() + "\n" + text,
                p.matcher(text).find());
          } catch (IOException ex) {
            fail(ex.toString());
          }
        }
      });
      return this;
    }

    ServletTest expectContent(final String expected) {
      reqs.add(new Assertion() {
        public void test() throws AssertionFailedError {
          try {
            assertEquals(expected, result.content.getText());
          } catch (IOException ex) {
            fail(ex.toString());
          }
        }
      });
      return this;
    }

    private Set<String> zipFileContent;
    private void checkZipFile() throws IOException {
      if (zipFileContent == null) {
        Set<String> files = Sets.newLinkedHashSet();
        ByteArrayOutputStream bout = new ByteArrayOutputStream(
            (int) result.content.byteLength());
        result.content.toOutputStream(bout);
        byte[] zipB = bout.toByteArray();
        ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipB));
        for (ZipEntry ze; (ze = zin.getNextEntry()) != null;) {
          files.add(ze.getName());
        }
        zipFileContent = files;
      }
    }

    ServletTest expectZip() {
      reqs.add(new Assertion() {
        public void test() throws AssertionFailedError {
          try {
            checkZipFile();
          } catch (IOException ex) {
            fail(ex.toString());
          }
        }
      });
      return this;
    }

    ServletTest zipFileExists(final String path) {
      reqs.add(new Assertion() {
        public void test() throws AssertionFailedError {
          try {
            checkZipFile();
          } catch (IOException ex) {
            fail(ex.toString());
          }
          assertTrue(path + " not in " + zipFileContent.toString(),
                     zipFileContent.contains(path));
        }
      });
      return this;
    }

    Result send() {
      assertTrue(tests.remove(this));
      result = servlet.handle(path, params);
      mq.getMessages().addAll(result.mq.getMessages());
      assertFalse("no assertions", reqs.isEmpty());
      for (Assertion a : reqs) { a.test(); }
      return result;
    }
  }

  private static final String HTML_NS = Namespaces.HTML_NAMESPACE_URI;
  /** Source code from an example link in index.quasi.html */
  private String exampleCode(String exampleTitle) throws Exception {
    Element root = html(fromResource("index.quasi.html"));
    for (Element example : Nodes.nodeListIterable(
             root.getElementsByTagNameNS(HTML_NS, "li"),
             Element.class)) {
      for (Element link : Nodes.nodeListIterable(
          example.getElementsByTagNameNS(HTML_NS, "a"), Element.class)) {
        if (!exampleTitle.equals(link.getFirstChild().getNodeValue())) {
          continue;
        }
        String uri = link.getAttributeNS(HTML_NS, "href");
        if (!Strings.toLowerCase(uri).startsWith("javascript:")) { continue; }
        String js = URLDecoder.decode(
            uri.substring("javascript:".length()), "UTF-8");
        return findSource(js(fromString(js)));
      }
    }
    return null;
  }

  private static final String findSource(ParseTreeNode js) {
    Map<String, ParseTreeNode> bindings = Maps.newHashMap();
    if (QuasiBuilder.match("(replaceSource(@s, @t))", js, bindings)) {
      ParseTreeNode s = bindings.get("s");
      if (s instanceof StringLiteral) {
        return ((StringLiteral) s).getUnquotedValue();
      }
    }
    for (ParseTreeNode child : js.children()) {
      String src = findSource(child);
      if (src != null) { return src; }
    }
    return null;
  }
}
