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

import junit.framework.AssertionFailedError;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class JsHandlerTest extends ServiceTestCase {
  private static void checkWithCallback(
      String jsonpCallback,
      String emitted,
      String... expectedSubstrings) throws Exception {
    Pattern p = Pattern.compile("^" + jsonpCallback + "\\((\\{.*\\})\\)$");
    Matcher m = p.matcher(emitted);
    assertTrue(m.matches());
    assertSubstringInJson(m.group(1), "js", expectedSubstrings);
  }

  public final void testJs() throws Exception {
    registerUri("http://foo/bar.js", "g(1);", "text/javascript");
    assertSubstringInJson(
        (String) requestGet("?url=http://foo/bar.js"
            + "&input-mime-type=text/javascript"),
        "js",
        "moduleResult___=$v.cf($v.ro('g'),[1]);");
  }

  public final void testJsWithJsonpCallback() throws Exception {
    registerUri("http://foo/bar.js", "g(1);", "text/javascript");
    checkWithCallback(
        "foo",
        (String) requestGet("?url=http://foo/bar.js"
            + "&input-mime-type=text/javascript"
            + "&callback=foo"),
        "moduleResult___=$v.cf($v.ro('g'),[1]);");
    try {
      checkWithCallback(
          "foo.bar",
          (String) requestGet("?url=http://foo/bar.js"
              + "&input-mime-type=text/javascript"
              + "&callback=foo.bar"),
          "moduleResult___=$v.cf($v.ro('g'),[1]);");
      fail("Failed to reject non-identifier JSONP callback");
    } catch (AssertionFailedError e) {
      // Success
    }
  }

  public final void testCajitaJs() throws Exception {
    registerUri("http://foo/bar.js", "g(1);", "text/javascript");
    assertSubstringInJson(
        (String) requestGet("?url=http://foo/bar.js"
            + "&input-mime-type=text/javascript"
            + "&directive=CAJITA"),
        "js",
        "var g=___.readImport(IMPORTS___,'g');",
        "moduleResult___=g.CALL___(1);");
  }
}
