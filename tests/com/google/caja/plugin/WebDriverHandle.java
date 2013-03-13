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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.UnsupportedCommandException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Manages WebDriver instances.
 *
 * WebDriver browser setup is kind of slow, so ideally we'd start only one
 * before running tests, then tear it down when tests are done. However,
 * there's no easy way to run something after all tests are done.
 *
 * So what we're doing here is, every BrowserTestCase uses WebDriverHandle,
 * and calls release() when done. WebDriverHandle keeps a static refcount,
 * and tears down its static WebDriver instance when refcount==0.
 *
 * This is kind of gross, but other options are worse. In particular, using
 * Runtime shutdown hooks is flaky because WebDriver also uses shutdown
 * hooks, and we can't guarantee order of execution.
 *
 * If we convert to Junit4, we can use @BeforeClass and @AfterClass instead
 * of refcount.
 */

class WebDriverHandle {
  // TODO(felix8a): gather flags
  private static final String BROWSER = "caja.test.browser";
  private static final String BROWSER_PATH = "caja.test.browserPath";
  private static final String REMOTE = "caja.test.remote";

  private static WebDriver driver = null;
  private static int refCount = 0;
  private static String firstWindow = null;
  private static int windowSeq = 1;

  private final static String browserType =
      System.getProperty(BROWSER, "firefox");

  WebDriverHandle() {
    refCount += 1;
  }

  WebDriver makeWindow() {
    if (driver == null) {
      driver = makeDriver();
      firstWindow = driver.getWindowHandle();
      try {
        driver.manage().timeouts().pageLoadTimeout(15, TimeUnit.SECONDS);
        driver.manage().timeouts().setScriptTimeout(5, TimeUnit.SECONDS);
      } catch (UnsupportedCommandException e) {
        // ignore
      }
    }
    JavascriptExecutor jsexec = (JavascriptExecutor) driver;
    String name = "cajatestwin" + (windowSeq++);
    Boolean result = (Boolean) jsexec.executeScript(
        "return !!window.open('', '" + name + "')");
    if (result) {
      driver.switchTo().window(name);
    }
    return driver;
  }

  String getBrowserType() {
    return browserType;
  }

  void closeWindow() {
    driver.close();
    driver.switchTo().window(firstWindow);
  }

  void release() {
    refCount -= 1;
    if (refCount <= 0) {
      refCount = 0;
      if (driver != null) {
        if (firstWindow != null) {
          driver.switchTo().window(firstWindow);
          firstWindow = null;
        }
        driver.close();
        driver = null;
      }
    }
  }

  private WebDriver makeDriver() {
    String browserPath = System.getProperty(BROWSER_PATH);
    String remote = System.getProperty(REMOTE, "");
    DesiredCapabilities dc = new DesiredCapabilities();
    if (!"".equals(remote)) {
      dc.setBrowserName(browserType);
      dc.setJavascriptEnabled(true);
      try {
        return new RemoteWebDriver(new URL(remote), dc);
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }
    if ("chrome".equals(browserType)) {
      if (browserPath != null) {
        dc.setCapability("chrome.binary", browserPath);
      }
      return new ChromeDriver(dc);
    } else if ("firefox".equals(browserType)) {
      return new FirefoxDriver();
    } else {
      throw new RuntimeException("No local driver for browser type '"
          + browserType + "'");
    }
  }
}
