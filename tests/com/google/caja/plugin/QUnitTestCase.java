package com.google.caja.plugin;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public abstract class QUnitTestCase extends BrowserTestCase<Integer> {

  public QUnitTestCase() {
    super();
  }

  /**
   * Do what should be done with the browser.
   * @param pageName The tail of a URL.  Unused in this implementation
   * @param passCount The number of tests we expect to pass, or null for all.
   */
  @Override
  protected String driveBrowser(final WebDriver driver, Integer passCount,
      final String pageName) {
    final String testResultId = "qunit-testresult-caja-guest-0___";
    // Find the reporting div
    countdown(20000, 200, new Countdown() {
      @Override public String toString() { return "startup"; }
      public int run() {
        List<WebElement> readyElements = driver.findElements(
            By.id(testResultId));
        return readyElements.size() == 0 ? 1 : 0;
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

    int passed = numberByClass(statusElement, "passed");
    int failed = numberByClass(statusElement, "failed");
    if (passCount != null) {
      assertEquals(currentStatus, (int)passCount, passed);
    } else {
      assertEquals(currentStatus, 0, failed);
    }
    return currentStatus;
  }

  private int numberByClass(WebElement container, String className) {
    return Integer.parseInt(
        container.findElement(By.className(className)).getText());
  }

  /**
   * Run a page of QUnit tests.
   * @param testCase The page name.
   * @param passCount The number of tests we expect to pass, or null for all.
   *             We should update this monotonically as we improve Caja.
   */
  protected String runQUnitTestCase(String testCase, Integer passCount,
      String... params) throws Exception {
    return runBrowserTest("browser-test-case.html",
        passCount,
        add(params,
            "es5=true",
            "test-case=" + escapeUri(
                getTestURL(testCase)),
            "jQuery=true"));
  }

  /**
   * Generate a full URL from a test case name.
   * @param testCase The parameter to runQUnitTestCase.
   * @return URL for HTML to run in Caja.
   */
  protected abstract String getTestURL(String testCase);
}