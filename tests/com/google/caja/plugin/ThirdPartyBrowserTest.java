// Copyright (C) 2013 Google Inc.
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

import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Browser-driving tests for third-party libraries we're interested in.
 *
 * @author kpreid@switchb.org
 */
@RunWith(CatalogRunner.class)
@CatalogRunner.CatalogName("third-party-tests.json")
public class ThirdPartyBrowserTest extends CatalogTestCase {
  /**
   * For QUnit-based tests, read QUnit's status text to determine if progress is
   * being made.
   */
  @Override
  protected void waitForCompletion(final WebDriver driver) {
    final String testResultId = "qunit-testresult-caja-guest-0___";
    if (driver.findElements(By.id(testResultId)).size() == 0) {
      // Not a QUnit test case; use default behavior.
      super.waitForCompletion(driver);
      return;
    }

    // Let it run as long as the report div's text is changing
    WebElement statusElement = driver.findElement(By.id(testResultId));
    String currentStatus = statusElement.getText();
    String lastStatus = null;

    // Check every second.
    // If the text starts with "Tests completed", then we're done.
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
  }

}
