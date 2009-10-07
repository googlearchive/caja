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

/**
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class JsHandlerTest extends ServiceTestCase {
  public final void testSimpleJs() throws Exception {
    registerUri("http://foo/bar.js", "g(1);", "text/javascript");
    assertEquals(
        valijaModule("moduleResult___ = $v.cf($v.ro('g'), [ 1 ]);"),
        requestGet("?url=http://foo/bar.js&mime-type=text/javascript" +
                "&transform=VALIJA"));
  }

  public final void testAltJscriptMimeType() throws Exception {
    registerUri(
        "http://foo/bar.js", "f();", "application/x-javascript");
    assertEquals(
        valijaModule("moduleResult___ = $v.cf($v.ro('f'), [ ]);"),
        requestGet("?url=http://foo/bar.js&mime-type=text/javascript" +
            "&transform=VALIJA"));
  }
}
