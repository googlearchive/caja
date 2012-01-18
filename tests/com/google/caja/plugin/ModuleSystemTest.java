// Copyright (C) 2011 Google Inc.
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

package com.google.caja.plugin;

import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

/**
 * @author ihab.awad@gmail.com
 */
public class ModuleSystemTest extends BrowserTestCase {
  public final void testJsonRequest() throws Exception {
    runBrowserTest("json-rest-transport-test.html");
  }

  public final void testCajolingServiceClient() throws Exception {
    runBrowserTest("cajoling-service-client-test.html");
  }

  public final void testLoadModule() throws Exception {
    runBrowserTest("load-module-test.html");
  }

  @Override
  protected void addServlets(Context servlets) {
    servlets.addServlet(
        new ServletHolder(new JsonTestServlet()),
       "/jsonTest");
  }
}
