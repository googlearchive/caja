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

package com.google.caja.demos.playground;

import com.google.caja.demos.playground.client.CajolingServiceResult;
import com.google.caja.demos.playground.server.GWTCajolingServiceImpl;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.util.CajaTestCase;

/**
 * Tests the running the playground webservice
 *
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class GWTCajolingServiceImplTest extends CajaTestCase {
  private GWTCajolingServiceImpl service;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // Set up the playground service with a mock fetcher
    service = new GWTCajolingServiceImpl();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    service = null;
  }

  private void assertCajoles(String uri, String content) {
    CajolingServiceResult result = service.cajole(
        null, uri, content, false, null);
    assertTrue(result.getHtml() != null);
  }

  private void assertFailsWithError(String uri, String content,
      MessageLevel lvl) {
    CajolingServiceResult result = service.cajole(
        null, uri, content, false, null);
    assertNull(result.getHtml());
    assertNull(result.getJavascript());
    for (String message : result.getMessages()) {
      if (message.startsWith(lvl.name())) {
        return;
      }
    }
  }

  public final void testSimpleCajoling() {
    assertCajoles("http://foo/baz.html", "<script>var a=1;</script>");
  }

  // Issue 1179
  public final void testErrorReporting() {
    assertFailsWithError("http://foo/bar.html", "<script>var a=b[];</script>",
        MessageLevel.ERROR);
  }
}
