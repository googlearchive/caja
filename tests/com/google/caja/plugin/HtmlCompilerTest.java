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
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Statement;
import com.google.caja.util.CajaTestCase;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public class HtmlCompilerTest extends CajaTestCase {

  public void testTargetsRewritten() throws Exception {
    assertOutput(
        "IMPORTS___.htmlEmitter___.b('a').a('href', '/testplugin/foo')"
        + ".a('target', '_blank').f(false).ih('hello').e('a');",

        "<a href=\"foo\" target=\"_self\">hello</a>");
  }

  public void testFormRewritten() throws Exception {
    assertOutput(
        "IMPORTS___.htmlEmitter___.b('form').a('onsubmit', 'return false')"
        + ".f(false).e('form');",
        "<form></form>");
  }

  public void testNamesRewritten() throws Exception {
    assertOutput(
        "IMPORTS___.htmlEmitter___.b('p')"
        + ".a('name', 'hi-' + IMPORTS___.getIdClass___())"
        + ".f(false).e('p');",

        "<p name=\"hi\"/>");
  }

  public void testFormName() throws Exception {
    assertOutput(
        "IMPORTS___.htmlEmitter___.b('form')"
        + ".a('name', 'hi-' + IMPORTS___.getIdClass___())"
        + ".a('onsubmit', 'return false').f(false).e('form');",

        "<form name=\"hi\"></form>");
  }

  // See bug 722
  public void testFormOnSubmitTrue() throws Exception {
    assertOutput(
        ""
        + "IMPORTS___.htmlEmitter___.b('form')"
        + "  .h('onsubmit',"
        + "     'return plugin_dispatchEvent___(this, event, '"
        + "     + ___.getId(IMPORTS___) + ', \\'c_1___\\')')"
        + "  .f(false).e('form');"
        + "IMPORTS___.c_1___ = function (event, thisNode___) {"
        + "  try {"
        + "    return true;"
        + "  } finally {"
        + "    return false;"
        + "  }"
        + "};",
        "<form onsubmit=\"return true;\"></form>");
  }

  // See bug 722
  public void testFormOnSubmitEmpty() throws Exception {
    assertOutput(
        ""
        + "IMPORTS___.htmlEmitter___.b('form')"
        + "  .h('onsubmit',"
        + "     'return plugin_dispatchEvent___(this, event, '"
        + "     + ___.getId(IMPORTS___) + ', \\'c_1___\\')')"
        + "  .f(false).e('form');"
        + "IMPORTS___.c_1___ = function (event, thisNode___) {"
        + "  try {"
        + "  } finally {"
        + "    return false;"
        + "  }"
        + "};",
        "<form onsubmit=\"\"></form>");
  }

  public void testImageSrc() throws Exception {
    assertOutput(
        "IMPORTS___.htmlEmitter___.b('img')"
        + ".a('src', '/testplugin/blank.gif')"
        + ".a('width', '20').f(true);",
        "<img src=\"blank.gif\" width=\"20\"/>");
  }

  public void testStyleRewriting() throws Exception {
    assertOutput(
        "IMPORTS___.htmlEmitter___.b('div')"
        + ".a('style', 'position: absolute;"
        + "\\nbackground: url(\\'/testplugin/bg-image\\')')"
        + ".f(false)"
        + ".ih('\\nHello\\n').e('div').pc('\\n');",

        "<div style=\"position: absolute; background: url('bg-image')\">\n"
        + "Hello\n"
        + "</div>\n");
  }

  public void testEmptyStyleRewriting() throws Exception {
    assertOutput(
        "IMPORTS___.htmlEmitter___.b('div').f(false)"
        + ".ih('\\nHello\\n').e('div').pc('\\n');",

        "<div style=>\nHello\n</div>\n");
    assertOutput(
        "IMPORTS___.htmlEmitter___.b('div').f(false)"
        + ".ih('\\nHello\\n').e('div').pc('\\n');",

        "<div style=\"\">\nHello\n</div>\n");
    assertOutput(
        "IMPORTS___.htmlEmitter___.pc('Hello, World!');",

        "<style type=text/css></style>Hello, World!");
    assertOutput(
        "IMPORTS___.htmlEmitter___.pc('Hello, World!');",

        "<style type=text/css>/* Noone here */</style>Hello, World!");
  }

  public void testEmptyScriptRewriting() throws Exception {
    assertOutput(
        "IMPORTS___.htmlEmitter___.b('div').f(false)"
        + ".ih('\\nHello\\n').e('div').pc('\\n');",

        "<div onclick=\"\">\nHello\n</div>\n");
  }

  private void assertOutput(String golden, String htmlText) throws Exception {
    HtmlCompiler htmlc = new HtmlCompiler(
        CssSchema.getDefaultCss21Schema(mq), HtmlSchema.getDefault(mq),
        mc, mq, makeTestPluginMeta());
    Block compiled = htmlc.compileDocument(htmlFragment(fromString(htmlText)));
    // TODO(mikesamuel): find a common place for removePseudoNodes.
    DomProcessingEventsTest.removePseudoNodes(compiled);
    for (Statement handler : htmlc.getEventHandlers()) {
      compiled.appendChild(handler);
    }
    if (!ParseTreeNodes.deepEquals(js(fromString(golden)), compiled)) {
      fail(render(compiled));
    }
  }

  private PluginMeta makeTestPluginMeta() {
    return new PluginMeta(
        new PluginEnvironment() {
            public CharProducer loadExternalResource(
                ExternalReference ref, String mimeType) {
              return null;
            }
            public String rewriteUri(ExternalReference ref, String mimeType) {
              URI uri = ref.getUri();

              if (uri.getScheme() == null
                  && uri.getHost() == null
                  && uri.getPath() != null) {
                try {
                  String path = uri.getPath();
                  path = (path.startsWith("/") ? "/testplugin" : "/testplugin/")
                      + path;
                  return new URI(
                      null, null, path, uri.getQuery(), uri.getFragment())
                      .toString();
                } catch (URISyntaxException ex) {
                  ex.printStackTrace();
                  return null;
                }
              } else {
                return null;
              }
            }
        });
  }
}
