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
   * @param passCount The number of tests we expect to pass, or null for all.
   */
  protected String driveBrowser(
      final WebDriver driver, Object passCount, final String pageName) {
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

    String result = passCount != null
        ? "\n" + passCount + " tests"
        : "passed, 0 failed";
    Assert.assertThat(currentStatus, Matchers.containsString(result));
    return currentStatus;
  }

  /**
   * Run a page of jQuery tests.
   * @param testCase The page name.
   * @param passCount The number of tests we expect to pass, or null for all.
   *             We should update this monotonically as we improve Caja.
   */
  protected String runJQueryTestCase(
      String testCase, Integer passCount, String... params)
      throws Exception {
    return runBrowserTest("browser-test-case.html",
        passCount,
        add(params,
            "es5=true",
            "test-case=" + escapeUri(
                "/ant-testlib/js/jqueryjs/test/" +
                testCase +
                "-uncajoled.html"),
            "jQuery=true"));
  }

  public final void testCore() throws Exception {
    runJQueryTestCase("core", 1284);
    // Current modifications made to test suite:
    //   * Removed unnecessary octal literal.
    // Current failure categories:
    //   * Complaint about lack of PHP server
    //   * TODO(jasvir): window.eval is absent (this includes the jQuery('html')
    //     failure)
    //   * We don't implement XML yet.
    //   * We don't implement iframes yet.
    //   * We don't implement document.styleSheets.
    //   * We don't implement document.getElementsByName.
  }

  public final void testCallbacks() throws Exception {
    runJQueryTestCase("callbacks", null);
    // Current modifications made to test suite:
    //   * Adjusted "context is window" test assuming callee is non-strict
  }

  public final void testDeferred() throws Exception {
    runJQueryTestCase("deferred", null);
    // Current modifications made to test suite:
    //   * Adjusted tests assuming callee is non-strict
  }

  public final void testSupport() throws Exception {
    runJQueryTestCase("support", 1);
    // Current failure categories:
    //   * We don't implement iframes yet.
  }

  public final void testData() throws Exception {
    runJQueryTestCase("data", 290);
    // Current failure categories:
    //   * We don't implement iframes yet (used incidentally).
  }

  public final void testQueue() throws Exception {
    runJQueryTestCase("queue", null);
  }

  public final void testAttributes() throws Exception {
    runJQueryTestCase("attributes", 417);
    // Current failure categories:
    //   * URI rewriting is visible to the guest.
    //   * Simple event handler rewriting is visible to the guest.
    //   * We don't implement XML yet.
    //   * Unknown - "Second radio was checked when clicked" - .click() problem?
    //   * Rejection of HTML5 autofocus attribute assignment is visible.
    //   * Removing style= attributes is misbehaving according to jQuery.
    //   * We don't support tabindex on non-form-elements yet (HTML5).
    //   * We don't support document.createAttribute yet.
    //   * Something to do with multiple-select.
    //   * Expects a form name/id (?) to be reflected on document.
  }

  public final void testEvent() throws Exception {
    runJQueryTestCase("event", 377);
    // Current failure categories:
    //   * Various lost-signal failures:
    //        in 'bind(),live(),delegate() with non-null,defined data'
    //        live() and delegate() tests
    //        trigger() tests
    //   * We don't implement document.createEvent of other than 'HTMLEvents'
    //   * We don't implement iframes yet.
    //   * jQuery reports leak in 'bind(name, false), unbind(name, false)'
    //   * submit listeners not firing in 'trigger(type, [data], [fn])'
    //   * "Object [domado object HTMLInputElement] has no method 'click'"
    //   * Something about "quickIs".
  }

  public final void testSelector() throws Exception {
    runJQueryTestCase("selector", 25);
    // Current failure categories:
    //   * We don't implement iframes yet.
  }

  public final void testTraversing() throws Exception {
    runJQueryTestCase("traversing", 286);
    // Current failure categories:
    //   * We don't implement iframes yet.
  }

  public final void testManipulation() throws Exception {
    runJQueryTestCase("manipulation", 473);
    // Current modifications made to test suite:
    //   * Removed SES-incompatible Array.prototype modification; was only for
    //     testing jQuery robustness.
    // Current failure categories:
    //   * We don't implement some new-in-HTML5 DOM features yet.
    //   * We don't implement XML yet.
    //   * We don't provide window.eval.
    //   * Something wrong with checked radio buttons.
    //   * We don't make non-JS script blocks readable/preserved.
  }

  public final void testCSS() throws Exception {
    runJQueryTestCase("css", 196);
    // Current failure categories:
    //   * We don't implement SVG (fill-opacity CSS property).
    //   * Something doesn't work such that defaultDisplay() in jquery falls
    //     back to a strategy creating an iframe, which we don't implement.
    //     This means that .show() and presumably .hide() doesn't work.
  }

  // Currently doesn't work because jQuery needs a PHP sever for ajax tests.
  /*
    public final void testAJAX() throws Exception {
      runJQueryTestCase("ajax", ??);
    }
  */

  public final void testEffects() throws Exception {
    runJQueryTestCase("effects", 528);
    // Current modifications made to test suite:
    //   * Fixed maybe-accidental undeclared global 'calls'.
    // Current failure categories:
    //   * We don't implement SVG (fill-opacity CSS property).
  }

  public final void testOffset() throws Exception {
    runJQueryTestCase("offset", 18);
    // Current failure categories:
    //   * We don't implement iframes yet.
  }

  public final void testDimensions() throws Exception {
    runJQueryTestCase("dimensions", 133);
    // Current modifications made to test suite:
    //   * Fixed nested function in strict mode.
    // Current failure categories:
    //   * We don't implement iframes yet.
  }

  public final void testExports() throws Exception {
    runJQueryTestCase("exports", null);
  }
}
