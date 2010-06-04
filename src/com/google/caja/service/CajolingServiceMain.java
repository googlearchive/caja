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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

import java.io.IOException;

/**
 * A executable that starts a cajoling service which proxies connections:<ul>
 *   <li>cajole any javascript
 *   <li>cajoles any gadgets
 *   <li>checks requested and retrieved mime-types
 * </ul>
 *
 * @author jasvir@gmail.com (Jasvir Nagra)
 */
public class CajolingServiceMain {
  public static void main(String[] args) throws Exception {
    // http://docs.codehaus.org/display/JETTY/Embedding+Jetty
    int port = 8887;
    Server server = new Server(port);

    final CajolingServlet servlet = new CajolingServlet(new CajolingService(
        BuildInfo.getInstance(), "http://localhost:" + port));

    server.setHandler(new AbstractHandler() {
      public void handle(
          String target, HttpServletRequest req, HttpServletResponse resp,
          int dispatch)
          throws ServletException {
        try {
          servlet.service(req, resp);
        } catch (IOException e) {
          throw (ServletException) new ServletException().initCause(e);
        }
      }
    });
    server.start();
  }
}

