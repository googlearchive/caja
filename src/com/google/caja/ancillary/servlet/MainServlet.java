// Copyright (C) 2010 Google Inc.
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

import java.net.URI;
import java.security.SecureRandom;

/**
 * A CajaWebToolsServlet that is wired to the outside world and has a
 * convenient zero argument ctor.
 *
 * @author mikesamuel@gmail.com
 */
public final class MainServlet extends CajaWebToolsServlet {

  private static String makeCacheId() {
    return Integer.toString(new SecureRandom().nextInt(1 << 30), 36);
  }

  private static URI makeUserAgentDbUri() {
    return URI.create(System.getProperty(
        "caja.webservice.useragentDb",
        UserAgentDb.BROWSERSCOPE_WEB_SERVICE.toString()));
  }

  /** Zero argument ctor for Jetty. */
  public MainServlet() { super(makeCacheId(), makeUserAgentDbUri()); }
}
