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

package com.google.caja.plugin;

import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.util.CajaTestCase;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.util.List;

/**
 * Run the JSUnit tests in host-tools-test.html in a web browser.
 *
 * TODO(kpreid): Figure out why RhinoTestBed.runJsUnittestFromHtml isn't
 * sufficient to run the tests.
 *
 * TODO(kpreid): Refactor to eliminate duplicate code with DomitaTest.
 *
 * @author kpreid@switchb.org
 */
public class HostToolsTest extends CajaTestCase {
  final int waitRoundLimit = 15;

  Server server;
  Process cajoler;

  /**
   * Start a local web server on port 8000, and the cajoling service as usual.
   */
  public void StartLocalServer() {
    server = new Server(8000);
    ResourceHandler resource_handler = new ResourceHandler();
    resource_handler.setResourceBase(".");
    HandlerList handlers = new HandlerList();
    handlers.setHandlers(new Handler[]{resource_handler,new DefaultHandler()});
    server.setHandler(handlers);

    try {
      server.start();
    } catch (Exception e) {
      fail("Starting the local web server failed!");
    }
    
    try {
      cajoler = Runtime.getRuntime().exec("ant runserver");
    } catch (Exception e) {
      fail("Starting the local cajoler failed!");
    }
    
    try {
      Thread.sleep(1000);
      // TODO(kpreid): fix race condition: learn when cajoler service is up and
      // running, then let tests proceed. How feasible is it to boot the
      // cajoling service in this jvm?
    } catch (InterruptedException e) {}
  }

  /**
   * Stop the local web server and cajoling service.
   */
  public void StopLocalServer() {
    try {
      server.stop();
    } catch (Exception e) {
      // the server will be turned down when the test exits
    }
    
    if (cajoler != null) {
      cajoler.destroy();
    }
  }

  public final void testHostTools() {
    if (checkHeadless()) return; // TODO: print a warning here?
    StartLocalServer();
    try {
      exerciseFirefox("host-tools-test.html");
    } finally {
      StopLocalServer();
    }
  }

  void exerciseFirefox(String pageName) {
    WebDriver driver = new FirefoxDriver();

    driver.get("http://localhost:8000/ant-lib/com/google/caja/plugin/"
               + pageName);

    int waitForCompletionRounds = 0;
    for (; waitForCompletionRounds < waitRoundLimit;
         waitForCompletionRounds++) {
      if (driver.getTitle().contains("all tests passed")) {
        break;
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {}
    }
    assertTrue(
        "Too many wait rounds.",
        waitForCompletionRounds < waitRoundLimit);

    // check the title of the document
    String title = driver.getTitle();
    assertTrue("The title shows " + title, title.contains("all tests passed"));

    driver.quit();
  }
}
