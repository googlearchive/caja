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

import java.util.Arrays;

/**
 * Tests the running the cajoler as a webservice
 *
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class CajolingServiceTest extends ServiceTestCase {

  // Tests that, if the content at the "url=" parameter is an unsupported
  // type, the request is rejected.
  public final void testUnsupportedInputContentType() throws Exception {
    registerUri("http://foo/bar.vb", "zoicks()", "text/vbscript");
    assertEquals(
        "ERROR",
        requestGet("?url=http://foo/bar.vb&mime-type=text/javascript"));
  }

  // Tests that, if the content at the "url=" parameter is supported but is
  // not appropriate for the request being made, the request is rejected.
  public final void testNonMatchingInputContentType() throws Exception {
    registerUri("http://foo/bar.gif", "foo()", "text/javascript");
    assertEquals("ERROR",
        requestGet("?url=http://foo/bar.gif&mime-type=image/*"));
  }

  // Tests requests for various mime types work.
  public final void testSupportedContentTypes() throws Exception {
    registerUri("http://foo/bar.js", "foo()", "text/javascript");
    registerUri("http://foo/bar.html", "<b>Hello</b>", "text/html");
    assertFalse("ERROR".equals(
        requestGet("?url=http://foo/bar.js&mime-type=text/javascript")));
    assertFalse("ERROR".equals(
        requestGet("?url=http://foo/bar.html&mime-type=text/html")));
    assertFalse("ERROR".equals(
        requestGet("?url=http://foo/bar.html&input-mime-type=text/html&" +
        		"output-mime-type=text/javascript")));
    assertEquals("ERROR",
        requestGet("?url=http://foo/bar.js&input-mime-type=text/javascript&" +
        		"output-mime-type=text/html"));
  }

  // Tests error messages are escaped.
  public final void testErrorMessagesEscaped() throws Exception {
    registerUri("http://foo/bar.js", "with(){};", "text/javascript");
    assertEquals("ERROR",
        (String)requestGet("?url=http://foo/bar.js&mime-type=text/javascript"));
  }
  
  // Tests that POST-ing to the service works just as well as GET-ting from it.
  public final void testPost() throws Exception {
    byte[] byteData = { (byte) 0x47, (byte) 0x49, (byte) 0x46,
                        (byte) 0x39, (byte) 0x38, (byte) 0x61 };
    assertTrue(Arrays.equals(
        (byte[]) requestPost(
            "?url=http://foo/bar.gif&mime-type=image/*",
            byteData,
            "image/gif",
            null),
        byteData));
  }

  public final void testUnexpectedMimeType() throws Exception {
    registerUri("http://foo/bar.gif", "foo()", "text/javascript");
    assertEquals("ERROR",
        requestGet("?url=http://foo/bar.gif&mime-type=image/*"));
  }
}
