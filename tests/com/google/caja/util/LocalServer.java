// Copyright (C) 2012 Google Inc.
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

package com.google.caja.util;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.service.ProxyServlet;

/**
 * Encapsulates the management of a localhost Web server running on an
 * arbitrary port, serving up the Caja resources and servlets, for testing.
 */
public class LocalServer {
  private final ConfigureContextCallback contextCallback;
  private Server server;

  public interface ConfigureContextCallback {
    void configureContext(Context ctx);
  }

  public LocalServer(ConfigureContextCallback contextCallback) {
    this.contextCallback = contextCallback;
  }

  public int getPort() {
    int port = server.getConnectors()[0].getLocalPort();
    // Occasionally port is -1 after the server is started, not sure why yet.
    if (port < 0) {
      System.err.println("port=" + port);
      throw new SomethingWidgyHappenedError("port=" + port);
    }
    return port;
  }

  /**
   * Start a local web server listening at the given TCP port.  port==0 will
   * choose an arbitrary unused port.
   */
  public void start(int port) throws Exception {
    server = new Server(port);

    final ResourceHandler cajaStatic = new ResourceHandler();
    cajaStatic.setResourceBase("./ant-war/");

    // static file serving for tests
    final ResourceHandler resource_handler = new ResourceHandler();
    resource_handler.setResourceBase(".");
    resource_handler.getMimeTypes().addMimeMapping(
        "ujs", "text/javascript;charset=utf-8");

    // caja (=playground for now) server under /caja directory
    final String subdir = "/caja";
    final ContextHandler caja = new ContextHandler(subdir);
    {
      final String service = "/proxy";

      // fetching proxy service -- Servlet setup code gotten from
      // <http://docs.codehaus.org/display/JETTY/Embedding+Jetty> @ 2010-06-30
      Context servlets = new Context(server, "/", Context.NO_SESSIONS);
      servlets.addServlet(new ServletHolder(new ProxyServlet()), service);

      // Hook for subclass to add more servlets
      if (contextCallback != null) {
        contextCallback.configureContext(servlets);
      }

      final HandlerList handlers = new HandlerList();
      handlers.setHandlers(new Handler[]{
          cajaStatic,
          servlets,
          new DefaultHandler()});
      caja.setHandler(handlers);
    }

    final HandlerList handlers = new HandlerList();
    handlers.setHandlers(new Handler[] {
        resource_handler,
        caja,
        new DefaultHandler()});
    server.setHandler(handlers);

    server.start();
  }

  /**
   * Stop the local web server
   */
  public void stop() throws Exception {
    // In case of exceptions, the server will be turned down when the test exits
    server.stop();
  }
}
