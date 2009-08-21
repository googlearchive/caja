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
* Perform the domita test automatically.
*
* @author maoziqing@gmail.com (Ziqing Mao)
*/
public class DomitaTest extends CajaTestCase {
  final int clickRoundLimit = 10;
  final int waitRoundLimit = 10;

  Server server;

  /**
   * Start a local web server on port 8000.
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
  }

  /**
   * Stop the local web server
   */
  public void StopLocalServer() {
    try {
      server.stop();
    } catch (Exception e) {
      // the server will be turned down when the test exits
    }
  }

  public final void testDomitaCajita() {
    exercise("domita_test.html");
  }

  public final void testDomitaValija() {
    exercise("domita_test.html?valija=1");
  }

  /**
   * Automatically click the elements with a class name containing "clickme".
   * Repeat until all tests are passed, or the number of rounds exceeds the
   * threshold.
   */
  public void exercise(String pageName) {
    if (checkHeadless()) return;
    StartLocalServer();
    try {
      exerciseFirefox(pageName);
    } finally {
      StopLocalServer();
    }
  }

  void exerciseFirefox(String pageName) {
    //System.setProperty("webdriver.firefox.bin", "/usr/bin/firefox");
    WebDriver driver = new FirefoxDriver();

    driver.get("http://localhost:8000/"
        + "ant-lib/com/google/caja/plugin/"
        + pageName);

    int clickRounds = 0;
    for (; clickRounds < clickRoundLimit; clickRounds++) {
      List<WebElement> clickingList =
         driver.findElements(By.xpath("//*[contains(@class,'clickme')]/*"));
      if (clickingList.size() == 0) {
        break;
      }
      for (WebElement e : clickingList) {
        e.click();
      }
    }
    assertTrue("Too many click rounds.", clickRounds < clickRoundLimit);

    int waitRounds = 0;
    for (; waitRounds < waitRoundLimit; waitRounds++) {
      List<WebElement> waitingList =
         driver.findElements(By.xpath("//*[contains(@class,'waiting')]/*"));
      if (waitingList.size() == 0) {
        break;
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {}
    }
    assertTrue("Too many wait rounds.", waitRounds < waitRoundLimit);

    // check the title of the document
    String title = driver.getTitle();
    assertTrue("The title shows " + title.substring(title.lastIndexOf("-") + 1),
        title.endsWith("all tests passed"));
    
    driver.quit();
  }
}
