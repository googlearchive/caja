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
public class InnocentHandlerTest extends ServiceTestCase {
  public final void testInnocentJs() throws Exception {
    registerUri("http://foo/innocent.js", "for (var k in x) { k; }",
        "text/javascript");
    assertEquals("{\n" +
        "  var x0___;\n" +
        "  for (x0___ in x) {\n" +
        "    if (x0___.match(/___$/)) { continue; }\n" +
        "    k = x0___;\n" +
        "    { k; }\n" +
        "  }\n" +
        "}",
        requestGet("?url=http://foo/innocent.js&mime-type=text/javascript" +
                "&transform=INNOCENT"));
  }
}
