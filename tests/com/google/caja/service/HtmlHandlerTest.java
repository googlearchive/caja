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

package com.google.caja.service;

import com.google.caja.reporting.BuildInfo;
import junit.framework.AssertionFailedError;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.caja.reporting.MessageLevel;

/**
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class HtmlHandlerTest extends ServiceTestCase {
  public final void testHtml2Json() throws Exception {
    assertHtml2Json("*/*");
    assertHtml2Json("text/html");
    assertHtml2Json("text/html");
  }

  public final void testHtmlWithJsonpCallback() throws Exception {
    registerUri("http://foo/bar.html", "<p>some random text</p>", "text/html");

    {
      String s = (String) requestGet("?url=http://foo/bar.html"
          + "&input-mime-type=text/html"
          + "&alt=json-in-script"
          + "&callback=foo"
          + "&build-version=" + BuildInfo.getInstance().getBuildVersion());
      assertCallbackInJsonp(s, "foo");
      assertSubstringsInJsonp(s, "html", "some random text");
    }

    try {
      assertCallbackInJsonp(
          (String) requestGet("?url=http://foo/bar.html"
              + "&input-mime-type=text/html"
              + "&alt=json-in-script"
              + "&callback=foo.bar"
              + "&build-version=" + BuildInfo.getInstance().getBuildVersion()),
          "foo.bar");
      fail("Failed to reject non-identifier JSONP callback");
    } catch (RuntimeException e) {
      assertContainsIgnoreSpace(e.toString(), "Detected XSS attempt");
    }

    try {
      assertCallbackInJsonp(
          (String) requestGet("?url=http://foo/bar.html"
              + "&input-mime-type=text/html"
              + "&callback=foo.bar"
              + "&build-version=" + BuildInfo.getInstance().getBuildVersion()),
          "foo.bar");
      fail("Added JSONP callback when not requested");
    } catch (AssertionFailedError e) {
      // Success
    }

    try {
      assertCallbackInJsonp(
          (String) requestGet("?url=http://foo/bar.html"
              + "&input-mime-type=text/html"
              + "&alt=json"
              + "&callback=foo.bar"
              + "&build-version=" + BuildInfo.getInstance().getBuildVersion()),
          "foo.bar");
      fail("Added JSONP callback when not requested");
    } catch (AssertionFailedError e) {
      // Success
    }
  }

  public final void testEmbeddedUris() throws Exception {
    registerUri(
        "http://foo/bar.css", "a { background-image: url(http://foo/baz.png) }",
        "text/css");
    registerUri(
        "http://foo/index.html",
        "<link rel=stylesheet href=bar.css><a href=\"shizzle.html\">Clicky</a>",
        "text/html");
    String result = (String) requestGet(
        "?url=http://foo/index.html&input-mime-type=text/html"
        + "&output-mime-type=text/html&sext=true&idclass=foo___"
        + "&build-version=" + BuildInfo.getInstance().getBuildVersion());
    JSONObject json = (JSONObject) json(result);
    assertContainsIgnoreSpace(
        (String) json.get("js"),
        "background-image: url('+"
        + "IMPORTS___.rewriteUriInCss___('http://foo/baz.png', 'background-image')+"
        + "')");
    assertContainsIgnoreSpace(
        (String) json.get("html"),
        "<a id=\"id_1___\" target=\"_blank\">");
    assertContainsIgnoreSpace(
        (String) json.get("js"),
        "IMPORTS___.rewriteUriInAttribute___("
        + "'http://foo/shizzle.html','a','href'"
        + ")");
    assertContainsIgnoreSpace(
        (String) json.get("js"),
        "IMPORTS___.rewriteTargetAttribute___('_blank','a','target')");
  }

  public final void testTargetAttribs() throws Exception {
    registerUri(
        "http://foo/index.html",
        ""
        + "<a href=\"shizzle.html\">Default</a>"
        + "<a href=\"shizzle.html\" target=\"_self\">Self</a>"
        + "<a href=\"shizzle.html\" target=\"_blank\">Blank</a>"
        + "<a href=\"shizzle.html\" target=\"_top\">Top</a>"
        + "<a href=\"shizzle.html\" target=\"_parent\">Parent</a>"
        + "<a href=\"shizzle.html\" target=\"foo\">Foo</a>",
        "text/html");
    String result = (String) requestGet(
        "?url=http://foo/index.html&input-mime-type=text/html"
        + "&output-mime-type=text/html&sext=true&idclass=foo___"
        + "&build-version=" + BuildInfo.getInstance().getBuildVersion());
    JSONObject json = (JSONObject) json(result);

    assertContainsIgnoreSpace(
        (String) json.get("html"),
        "<a id=\"id_1___\" target=\"_blank\">Default</a>");
    assertContainsIgnoreSpace(
        (String) json.get("js"),
        "IMPORTS___.rewriteTargetAttribute___('_blank','a','target')");
    assertContainsIgnoreSpace(
        (String) json.get("html"),
        "<a id=\"id_2___\" target=\"_blank\">Self</a>");
    assertContainsIgnoreSpace(
        (String) json.get("js"),
        "IMPORTS___.rewriteTargetAttribute___('_self','a','target')");
    assertContainsIgnoreSpace(
        (String) json.get("html"),
        "<a id=\"id_3___\" target=\"_blank\">Blank</a>");
    assertContainsIgnoreSpace(
        (String) json.get("js"),
        "IMPORTS___.rewriteTargetAttribute___('_blank','a','target')");
    assertContainsIgnoreSpace(
        (String) json.get("html"),
        "<a id=\"id_4___\" target=\"_blank\">Top</a>");
    assertContainsIgnoreSpace(
        (String) json.get("js"),
        "IMPORTS___.rewriteTargetAttribute___('_top','a','target')");
    assertContainsIgnoreSpace(
        (String) json.get("html"),
        "<a id=\"id_5___\" target=\"_blank\">Parent</a>");
    assertContainsIgnoreSpace(
        (String) json.get("js"),
        "IMPORTS___.rewriteTargetAttribute___('_parent','a','target')");
    assertContainsIgnoreSpace(
        (String) json.get("html"),
        "<a id=\"id_6___\" target=\"_blank\">Foo</a>");
    assertContainsIgnoreSpace(
        (String) json.get("js"),
        "IMPORTS___.rewriteTargetAttribute___('foo','a','target')");
  }
  
  private void assertHtml2Json(String inputMimeType)
      throws Exception {
    registerUri(
        "http://foo/bar.html",
        "<p foo align=center>hi</p><script>42;</script><p>bye</p>",
        "text/html");

    Object result = json((String) requestGet(
        "?url=http://foo/bar.html&input-mime-type=" + inputMimeType
        + "&build-version=" + BuildInfo.getInstance().getBuildVersion()));
    assertNotNull(result);
    assertTrue(result instanceof JSONObject);
    JSONObject json = (JSONObject) result;

    // Check html generation is correct
    assertEquals(
        "<p align=\"center\" data-caja-foo=\"foo\">" +
        "hi<span id=\"id_2___\"></span></p><p>bye</p>",
        (String) json.get("html"));
    assertEquals("{"
        + "___.loadModule({"
        + "'instantiate':function(___,IMPORTS___){"
          + "return ___.prepareModule({"
              + "'instantiate':function(___,IMPORTS___){"
                + "var\ndis___=IMPORTS___;var moduleResult___,el___,emitter___;"
                + "moduleResult___=___.NO_RESULT;"
                + "{"
                  + "emitter___=IMPORTS___.htmlEmitter___;"
                  + "emitter___.discard(emitter___.attach('id_2___'))"
                + "}"
                + "return moduleResult___"
              + "},"
              + "'cajolerName':'com.google.caja',"
              + "'cajolerVersion':'testBuildVersion',"
              + "'cajoledDate':0}).instantiate___(___,IMPORTS___),"
              + "___.prepareModule({"
                + "'instantiate':function(___,IMPORTS___){var\n"
                  + "dis___=IMPORTS___;"
                  + "var moduleResult___;"
                  + "moduleResult___=___.NO_RESULT;"
                  + "try{"
                    + "{"
                      + "moduleResult___=42"
                    + "}"
                  + "}"
                  + "catch(ex___){"
                    + "___.getNewModuleHandler().handleUncaughtException("
                      + "ex___,"
                      + "IMPORTS___.onerror_v___?IMPORTS___.onerror:"
                          + "___.ri(IMPORTS___,'onerror'),'bar.html','1')"
                      + "}"
                  + "return moduleResult___"
                + "},"
                + "'cajolerName':'com.google.caja',"
                + "'cajolerVersion':'testBuildVersion',"
                + "'cajoledDate':0"
                + "})"
                + ".instantiate___(___,IMPORTS___),___.prepareModule({"
                + "'instantiate':function(___,IMPORTS___){"
                  + "var\ndis___=IMPORTS___;"
                  + "var moduleResult___,el___,emitter___;"
                  + "moduleResult___=___.NO_RESULT;"
                  + "{"
                    + "emitter___=IMPORTS___.htmlEmitter___;"
                    + "el___=emitter___.finish();"
                    + "emitter___.signalLoaded()"
                  + "}"
                  + "return moduleResult___"
                + "},"
                + "'cajolerName':'com.google.caja',"
                + "'cajolerVersion':'testBuildVersion',"
                + "'cajoledDate':0"
              + "}).instantiate___(___,IMPORTS___)"
            + "},"
          + "'cajolerName':'com.google.caja',"
          + "'cajolerVersion':'testBuildVersion',"
          + "'cajoledDate':0"
          + "})"
        + "}",
      (String)json.get("js"));

    assertTrue(json.get("messages") instanceof JSONArray);
    JSONArray messages = (JSONArray)json.get("messages");
    assertMessagesLessSevereThan(messages, MessageLevel.ERROR);
  }

  public final void testErrors2Json() throws Exception {
    registerUri("http://foo/bar.html",
      "<script>with(foo){}</script>", "text/html");

    Object result = json((String) requestGet(
        "?url=http://foo/bar.html&input-mime-type=text/html"
        + "&build-version=" + BuildInfo.getInstance().getBuildVersion()));
    assertTrue(result instanceof JSONObject);
    JSONObject json = (JSONObject) result;

    assertTrue(json.containsKey("messages"));
    JSONArray messages = (JSONArray)json.get("messages");
    boolean containsError = false;
    for (Object msg : messages) {
      JSONObject jsonMsg = (JSONObject) msg;
      containsError = containsError || jsonMsg.get("name").equals("ERROR");
    }
    assertTrue(containsError);
  }
}
