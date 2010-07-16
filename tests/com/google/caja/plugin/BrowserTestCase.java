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

package com.google.caja.plugin;

import com.google.caja.service.CajolingServlet;
import com.google.caja.util.CajaTestCase;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.WebDriver;

/**
 * Test case class with tools for controlling a web browser running pages from a
 * local web server.
 *
 * @author maoziqing@gmail.com (Ziqing Mao)
 * @author kpreid@switchb.org (Kevin Reid)
 */
public abstract class BrowserTestCase extends CajaTestCase {
  protected Server server;

  /**
   * Start a local web server on the port specified by portNumber().
   */
  protected void StartLocalServer() {
    server = new Server(portNumber());
    
    // static file serving for tests
    final ResourceHandler resource_handler = new ResourceHandler();
    resource_handler.setResourceBase(".");
    
    // caja (=playground for now) server under /caja directory
    final ContextHandler caja = new ContextHandler("/caja");
    {
      // TODO(kpreid): deploy the already-configured war instead of manually
      // plumbing 
      
      // static file serving
      final ResourceHandler caja_static = new ResourceHandler();
      caja_static.setResourceBase("./ant-war/");
    
      // cajoling service -- Servlet setup code gotten from
      // <http://docs.codehaus.org/display/JETTY/Embedding+Jetty> @ 2010-06-30
      Context servlets = new Context(server, "/", Context.NO_SESSIONS);
      servlets.addServlet(new ServletHolder(new CajolingServlet()), "/cajole");

      final HandlerList handlers = new HandlerList();
      handlers.setHandlers(new Handler[] {
          caja_static,
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

    try {
      server.start();
    } catch (Exception e) {
      fail("Starting the local web server failed!");
    }
  }

  /**
   * Stop the local web server
   */
  protected void StopLocalServer() {
    try {
      server.stop();
    } catch (Exception e) {
      // the server will be turned down when the test exits
    }
  }

  /**
   * The port the local web server will run on.
   */
  protected int portNumber() {
    return 8000;
  }
  
  /**
   * Start the web server and browser, go to pageName, call driveBrowser(driver,
   * pageName), and then clean up.
   */
  protected void runBrowserTest(String pageName) {
    if (checkHeadless()) return;  // TODO: print a warning here?
    StartLocalServer();
    try {
      WebDriver driver = new FirefoxDriver();

      driver.get("http://localhost:8000/ant-lib/com/google/caja/plugin/"
                 + pageName);
      driveBrowser(driver, pageName);
      driver.quit();
      // Note that if the tests fail, this will not be reached and the browser
      // will not be quit. This is useful for debugging test failures.
    } finally {
      StopLocalServer();
    }
  }
  
  /**
   * Do what should be done with the browser.
   */
  abstract protected void driveBrowser(WebDriver driver, String pageName);
  
  /**
   * Run 'c' every 'intervalMillis' milliseconds until it returns true or
   * 'timeoutSecs' seconds have passed (in which case, fail).
   */
  public static void poll(
      int timeoutMillis, int intervalMillis, Check c) {
    int rounds = 0;
    int limit = timeoutMillis / intervalMillis;
    for (; rounds < limit; rounds++) {
      if (c.run()) {
        break;
      }
      try {
        Thread.sleep(intervalMillis);
      } catch (InterruptedException e) {}
    }
    assertTrue(
        timeoutMillis + " ms passed while waiting for: " + c + ".",
        rounds < limit);
  }
  
  public interface Check {
    boolean run();
  }  
}
