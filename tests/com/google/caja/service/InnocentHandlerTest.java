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

/**
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class InnocentHandlerTest extends ServiceTestCase {
  public final void testInnocentJs() throws Exception {
    registerUri("http://foo/innocent.js", "for (var k in x) { k; }",
        "text/javascript");
    assertSubstringsInJson(
        (String) requestGet("?url=http://foo/innocent.js"
            + "&input-mime-type=text/javascript"
            + "&transform=INNOCENT"),
        "js",
        "if (x0___.match(/___$/)) { continue }");
  }
  
  public final void testInnocentJsWithJsonpCallback() throws Exception {
    registerUri("http://foo/innocent.js", "for (var k in x) { k; }",
        "text/javascript");

    {
      String s = (String) requestGet("?url=http://foo/innocent.js"
          + "&input-mime-type=text/javascript"
          + "&transform=INNOCENT"
          + "&alt=json-in-script"
          + "&callback=foo");
      assertCallbackInJsonp(s, "foo");
      assertSubstringsInJsonp(
          s,
          "js",
          "if (x0___.match(/___$/)) { continue }");
    }

    try {
      assertCallbackInJsonp(
          (String) requestGet("?url=http://foo/innocent.js"
              + "&input-mime-type=text/javascript"
              + "&transform=INNOCENT"
              + "&alt=json-in-script"
              + "&callback=foo.bar"),
          "foo.bar");
      fail("Failed to reject non-identifier JSONP callback");
    } catch (AssertionFailedError e) {
      // Success
    }

    try {
      assertCallbackInJsonp(
          (String) requestGet("?url=http://foo/innocent.js"
              + "&input-mime-type=text/javascript"
              + "&transform=INNOCENT"
              + "&callback=foo.bar"),
          "foo.bar");
      fail("Added JSONP callback when not requested");
    } catch (AssertionFailedError e) {
      // Success
    }

    try {
      assertCallbackInJsonp(
          (String) requestGet("?url=http://foo/innocent.js"
              + "&input-mime-type=text/javascript"
              + "&transform=INNOCENT"
              + "&alt=json"
              + "&callback=foo.bar"),
          "foo.bar");
      fail("Added JSONP callback when not requested");
    } catch (AssertionFailedError e) {
      // Success
    }
  }
}
