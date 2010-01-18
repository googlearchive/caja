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

import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

/**
 * Starts up the servlet using jetty, by default, on port 8080.
 *
 * @author mikesamuel@gmail.com
 */
public class Main {

  /** Starts a server on port 8080. */
  public static void main(String[] args) throws Exception {
    int port;
    switch (args.length) {
      case 0: port = 8080; break;
      case 1: port = Integer.parseInt(args[0]); break;
      default:
        throw new Exception("What are these command line parameters for?");
    }
    Server server = new Server(port);
    String cacheId = Integer.toString(new SecureRandom().nextInt(1 << 30), 36);
    URI uadb = URI.create(System.getProperty(
        "caja.webservice.useragentDb",
        UserAgentDb.BROWSERSCOPE_WEB_SERVICE.toString()));
    final CajaWebToolsServlet servlet = new CajaWebToolsServlet(cacheId, uadb);
    server.setHandler(new AbstractHandler() {
      public void handle(
          String tgt, HttpServletRequest req, HttpServletResponse resp,
          int dispatch)
          throws IOException {
        String method = req.getMethod();
        if ("GET".equals(method)) {
          servlet.doGet(req, resp);
        } else if ("POST".equals(method)) {
          servlet.doPost(req, resp);
        }
      }
    });

    server.start();
  }
}
