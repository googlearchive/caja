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
  final int clickingRoundLimit = 10;
  
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
  
  /**
   * Automatically click the elements with a class name containing "clickme". 
   * Repeat until all tests are passed, or the number of rounds exceeds the
   * threshold.
   */
  public void testDomita() {
    StartLocalServer();
    
    //System.setProperty("webdriver.firefox.bin", "/usr/bin/firefox");
    WebDriver driver = new FirefoxDriver();
    
    driver.get("http://localhost:8000/"
        + "ant-lib/com/google/caja/plugin/domita_test.html");
    
    int roundCount = 0;
    do
    {
      List<WebElement> clickingList = 
         driver.findElements(By.xpath("//*[contains(@class,'clickme')]/*"));
      
      if (clickingList.size() == 0) {
        break;
      }
      
      for (WebElement e : clickingList) {
        e.click();
      }
      
      roundCount++;
    } while (roundCount <= clickingRoundLimit);
    
    assertTrue("Too many clicking rounds.", roundCount <= clickingRoundLimit);
    
    // check the title of the document
    String title = driver.getTitle();
    assertTrue("The title shows " + title.substring(title.lastIndexOf("-") + 1),
        title.endsWith("all tests passed"));
    
    //driver.quit();
    StopLocalServer();
  }
}
