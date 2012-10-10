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

package com.google.caja.plugin;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * @author metaweta@gmail.com
 */
public class JQueryTest extends BrowserTestCase {

  /**
   * Do what should be done with the browser.
   * @param pageName The tail of a URL.  Unused in this implementation
   * @param data The number of jQuery tests we expect to pass.
   */
  protected String driveBrowser(
      final WebDriver driver, int data, final String pageName) {
    final String testResultId = "qunit-testresult-caja-guest-0___";
    // Find the reporting div
    poll(20000, 200, new Check() {
      @Override public String toString() { return "startup"; }
      public boolean run() {
        List<WebElement> readyElements = driver.findElements(
            By.id(testResultId));
        return readyElements.size() != 0;
      }
    });

    // Let it run as long as the report div's text is changing
    WebElement statusElement = driver.findElement(By.id(testResultId));
    String currentStatus = statusElement.getText();
    String lastStatus = null;
    
    // Check every second.
    // If the text starts with "Tests completed" then
    //    exit the loop and check that the right number passed
    // If the text has changed, reset the time limit.
    int limit = 30; // tries
    for (int chances = limit; chances > 0; --chances) {
      if (currentStatus.startsWith("Tests completed")) { break; }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        // Keep trying
      }
      statusElement = driver.findElement(By.id(testResultId));
      lastStatus = currentStatus;
      currentStatus = statusElement.getText();
      if (!lastStatus.equals(currentStatus)) {
        chances = limit;
      }
    }

    String result = "\n" + data + " tests";
    Assert.assertThat(currentStatus, Matchers.containsString(result));
    return currentStatus;
  }

  /**
   * Run a page of jQuery tests.
   * @param testCase The page name.
   * @param data The number of tests we expect to pass.  We should update this
   *             monotonically as we improve Caja.
   */
  protected String runJQueryTestCase(
      String testCase, int data, String... params)
      throws Exception {
    return runBrowserTest("browser-test-case.html",
        data,
        add(params,
            "es5=true",
            "test-case=" + escapeUri(
                "/ant-testlib/js/jqueryjs/test/" +
                testCase +
                "-uncajoled.html"),
            "jQuery=true"));
  }

  public final void testCore() throws Exception {
    runJQueryTestCase("core", 716);
  }

  public final void testCallbacks() throws Exception {
    runJQueryTestCase("callbacks", 839);
  }

  public final void testDeferred() throws Exception {
    runJQueryTestCase("deferred", 219);
  }

  public final void testSupport() throws Exception {
    runJQueryTestCase("support", 1);
  }

  public final void testData() throws Exception {
    runJQueryTestCase("data", 276);
  }

  public final void testQueue() throws Exception {
    runJQueryTestCase("queue", 42);
  }

  public final void testAttributes() throws Exception {
    runJQueryTestCase("attributes", 391);
  }

  public final void testEvent() throws Exception {
    runJQueryTestCase("event", 368);
  }

  public final void testSelector() throws Exception {
    runJQueryTestCase("selector", 24);
  }

  public final void testTraversing() throws Exception {
    runJQueryTestCase("traversing", 281);
  }

  public final void testManipulation() throws Exception {
    runJQueryTestCase("manipulation", 0);
  }

  public final void testCSS() throws Exception {
    runJQueryTestCase("css", 139);
  }

  // Currently doesn't work because jQuery needs a PHP sever for ajax tests.
  /*
    public final void testAJAX() throws Exception {
      runJQueryTestCase("ajax", ??);
    }
  */

  public final void testEffects() throws Exception {
    runJQueryTestCase("effects", 480);
  }

  public final void testOffset() throws Exception {
    runJQueryTestCase("offset", 5);
  }

  public final void testDimensions() throws Exception {
    runJQueryTestCase("dimensions", 0);
  }

  public final void testExports() throws Exception {
    runJQueryTestCase("exports", 1);
  }
}
