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

import com.google.caja.service.TestHttpServletRequest;
import com.google.caja.service.TestHttpServletResponse;
import com.google.caja.util.CajaTestCase;

import java.util.Arrays;

public class StaticFilesTest extends CajaTestCase {
  public final void testBogusUrls() throws Exception {
    StaticFiles f = new StaticFiles("cacheId");
    assertFalse(f.exists("files/bogus"));
    assertFalse(f.exists("files/bogus.html"));
    assertTrue(f.exists("files/index.js"));
    assertTrue(f.exists("files/styles.css"));
  }

  public final void testServesBinary() throws Exception {
    StaticFiles f = new StaticFiles("cacheId");
    TestHttpServletRequest req = new TestHttpServletRequest("");
    TestHttpServletResponse resp = new TestHttpServletResponse();
    f.serve("files/tools-28.gif", req, resp);
    assertEquals(200, resp.getStatus());
    assertEquals("image/gif", resp.getHeaders().get("content-type"));
    byte[] bytes = (byte[]) resp.getOutputObject();
    byte[] first6 = new byte[6];
    byte[] golden = "GIF89a".getBytes("UTF-8");
    System.arraycopy(bytes, 0, first6, 0, 6);
    assertTrue(
        Arrays.toString(first6) + " != " + Arrays.toString(golden),
        Arrays.equals(golden, first6));
  }

  public final void testServesTextCompressed() throws Exception {
    StaticFiles f = new StaticFiles("cacheId");
    TestHttpServletRequest req = new TestHttpServletRequest("");
    TestHttpServletResponse resp = new TestHttpServletResponse();
    f.serve("files/styles.css", req, resp);
    assertEquals(200, resp.getStatus());
    assertEquals("text/css; charset=UTF-8",
                 resp.getHeaders().get("content-type"));
    String css = (String) resp.getOutputObject();
    assertTrue(css.startsWith("body{background-color:#fff"));
    assertFalse(css.matches("\\s[{}]|[{}]\\s"));
  }

  public final void test404s() throws Exception {
    StaticFiles f = new StaticFiles("cacheId");
    TestHttpServletRequest req = new TestHttpServletRequest("");
    TestHttpServletResponse resp = new TestHttpServletResponse();
    f.serve("files/nosuchfile.txt", req, resp);
    assertEquals(404, resp.getStatus());
  }
}
