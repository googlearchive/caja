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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Tests the fetching proxy servlet
 *
 * @author jasvir@google.com (Jasvir Nagra)
 */
public final class ProxyServletTest extends ServiceTestCase {
  protected JSONObject requestGet(String queryString) throws Exception {
    return (JSONObject) json((String) super.requestGet(queryString,
        "application/json"));
  }

  private static void assertError(JSONObject result) {
    JSONArray messages = (JSONArray) result.get("messages");
    for (int i = 0; i < messages.size(); i++) {
      JSONObject message = (JSONObject) messages.get(i);
      String levelName = (String) message.get("name");
      if (levelName.contains("ERROR")) { return; }
    }
    fail("Expected errors, but did not find any");
  }

  // Tests that, if the content at the "url=" parameter is an unsupported
  // type, the request is rejected.
  public final void testUnsupportedInputContentType() throws Exception {
    registerUri("http://foo/bar.vb", "zoicks()", "text/vbscript");
    JSONObject result = requestGet(
        "?url=http://foo/bar.vb&mime-type=text/javascript");
    assertNull(result.get("js"));
    assertNull(result.get("html"));
    assertError(result);
  }

  // Tests that, if the content at the "url=" parameter is supported but is
  // not appropriate for the request being made, the request is rejected.
  public final void testNonMatchingInputContentType() throws Exception {
    registerUri("http://foo/bar.gif", "foo()", "text/javascript");
    JSONObject result = requestGet(
        "?url=http://foo/bar.gif&mime-type=image/*");
    assertNull(result.get("js"));
    assertNull(result.get("html"));
    assertError(result);
  }

  public final void testUnexpectedMimeType() throws Exception {
    registerUri("http://foo/bar.gif", "foo()", "text/javascript");
    JSONObject result = requestGet(
        "?url=http://foo/bar.gif&input-mime-type=image/*");
    assertNull(result.get("js"));
    assertNull(result.get("html"));
    assertError(result);
  }

  public final void testCheckIdentifier() throws Exception {
    assertTrue(ProxyServlet.checkIdentifier("f"));
    assertTrue(ProxyServlet.checkIdentifier("f$_"));

    assertFalse(ProxyServlet.checkIdentifier("1"));
    assertFalse(ProxyServlet.checkIdentifier("1a"));
    assertFalse(ProxyServlet.checkIdentifier("a b"));
    assertFalse(ProxyServlet.checkIdentifier("a,b"));
    assertFalse(ProxyServlet.checkIdentifier("a=0,b"));
    assertFalse(ProxyServlet.checkIdentifier("a(b)"));
  }

  /*
   * Note: These following tests are now redundant with browser-side test
   * .../plugin/test-fetch-proxy.js. They have been left in because there's no
   * need to delete them and it's a little better to have Java tests for Java
   * code, but in the event the implementation of the proxy is changed these
   * might as well be discarded.
   */
  public final void testJsonp() throws Exception {
    registerUri("http://foo/bar", "body {}", "text/css");
    String s = (String) requestGet("?url=http://foo/bar"
        + "&input-mime-type=text/css"
        + "&alt=json-in-script"
        + "&callback=foo",
        "text/javascript");
    assertCallbackInJsonp(s, "foo");
    assertSubstringsInJsonp(s, "html", "body {}");
  }

  public final void testJson() throws Exception {
    registerUri("http://foo/bar", "body {}", "text/css");
    String s = (String) requestGet("?url=http://foo/bar"
        + "&input-mime-type=text/css"
        + "&alt=json"
        + "&callback=foo",
        "application/json");
    assertSubstringsInJson(s, "html", "body {}");
  }

  public final void testJsonpAbsent() throws Exception {
    // no registerUri; we want a failure
    requestGet("?url=http://foo/bar"
        + "&input-mime-type=text/css"
        + "&alt=json-in-script"
        + "&callback=foo",
        "text/javascript");
    // TODO(kpreid): assertions about content, not just mime type
  }

  public final void testJsonAbsent() throws Exception {
    // no registerUri; we want a failure
    requestGet("?url=http://foo/bar"
        + "&input-mime-type=text/css"
        + "&alt=json"
        + "&callback=foo",
        "application/json");
    // TODO(kpreid): assertions about content, not just mime type
  }
}
