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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Tests the running the cajoler as a webservice
 *
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class CajolingServiceTest extends ServiceTestCase {
  private final String cajaBuildVersionParam =
      "&build-version=" + BuildInfo.getInstance().getBuildVersion();

  protected JSONObject requestGet(String queryString) throws Exception {
    return (JSONObject) json((String) super.requestGet(queryString,
        "application/json"));
  }

  protected JSONObject requestPost(
      String queryString,
      byte[] content,
      String contentType,
      String contentEncoding) throws Exception {
    return (JSONObject) json((String) super.requestPost(queryString, content,
        contentType, contentEncoding, "application/json"));
  }

  protected static void assertNoError(JSONObject result)  {
    JSONArray messages = (JSONArray) result.get("messages");
    for (int i = 0; i < messages.size(); i++) {
      JSONObject message = (JSONObject) messages.get(i);
      String levelName = (String) message.get("name");
      assertFalse(levelName.contains("ERROR"));
    }
  }

  protected static void assertError(JSONObject result) {
    JSONArray messages = (JSONArray) result.get("messages");
    for (int i = 0; i < messages.size(); i++) {
      JSONObject message = (JSONObject) messages.get(i);
      String levelName = (String) message.get("name");
      if (levelName.contains("ERROR")) { return; }
    }
    fail("Expected errors, but did not find any");
  }

  protected static void assertErrorMessage(
      JSONObject result, String substring) {
    JSONArray messages = (JSONArray) result.get("messages");
    for (int i = 0; i < messages.size(); i++) {
      JSONObject message = (JSONObject) messages.get(i);
      String messageString = (String) message.get("message");
      if (messageString.contains(substring)) { return; }
    }
    fail("Expected error message \"" + substring + "\" but did not find it");
  }

  // Tests that, if the content at the "url=" parameter is an unsupported
  // type, the request is rejected.
  public final void testUnsupportedInputContentType() throws Exception {
    registerUri("http://foo/bar.vb", "zoicks()", "text/vbscript");
    JSONObject result = requestGet(
        "?url=http://foo/bar.vb&mime-type=text/javascript"
            + cajaBuildVersionParam);
    assertNull(result.get("js"));
    assertNull(result.get("html"));
    assertError(result);
  }

  // Tests that, if the content at the "url=" parameter is supported but is
  // not appropriate for the request being made, the request is rejected.
  public final void testNonMatchingInputContentType() throws Exception {
    registerUri("http://foo/bar.gif", "foo()", "text/javascript");
    JSONObject result = requestGet(
        "?url=http://foo/bar.gif&mime-type=image/*"
        + cajaBuildVersionParam);
    assertNull(result.get("js"));
    assertNull(result.get("html"));
    assertError(result);
  }

  public final void testUnexpectedMimeType() throws Exception {
    registerUri("http://foo/bar.gif", "foo()", "text/javascript");
    JSONObject result = requestGet(
        "?url=http://foo/bar.gif&input-mime-type=image/*"
        + cajaBuildVersionParam);
    assertNull(result.get("js"));
    assertNull(result.get("html"));
    assertError(result);
  }
}
