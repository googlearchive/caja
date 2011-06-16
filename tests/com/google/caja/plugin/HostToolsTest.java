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

import java.util.List;

import com.google.caja.util.FailureIsAnOption;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Run the JSUnit tests in host-tools-test.html in a web browser.
 *
 * TODO(kpreid): Figure out why RhinoTestBed.runJsUnittestFromHtml isn't
 * sufficient to run the tests.
 *
 * @author kpreid@switchb.org
 */
public class HostToolsTest extends BrowserTestCase {
  @FailureIsAnOption
  public final void testHostTools() {
    runBrowserTest("host-tools-test.html");
  }

  @Override
  protected void driveBrowser(final WebDriver driver, final String pageName) {
    poll(10000, 200, new Check() {
      @Override public String toString() { return "startup"; }
      public boolean run() {
        List<WebElement> readyElements = driver.findElements(
            By.xpath("//*[@class='readytotest']"));
        return readyElements.size() != 0;
      }
    });

    poll(15000, 1000, new Check() {
      @Override public String toString() { return "completion"; }
      public boolean run() {
        return driver.getTitle().contains("all tests passed");
      }
    });
  }
}
