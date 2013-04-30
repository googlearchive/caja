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

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.google.caja.util.TestFlag;

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
  private static RemoteWebDriver driver = null;
  private static int refCount = 0;
  private static String firstWindow = null;
  private static int windowSeq = 1;
  private static int keptWindows = 0;

  WebDriverHandle() {
    refCount += 1;
  }

  WebDriver makeWindow() {
    if (driver == null) {
      driver = makeDriver();
      reportVersion(driver);
      firstWindow = driver.getWindowHandle();
      try {
        driver.manage().timeouts().pageLoadTimeout(15, TimeUnit.SECONDS);
      } catch (WebDriverException e) {
        log("failed to set pageLoadTimeout: " + e.toString());
        // harmless, ignore
      }
      try {
        driver.manage().timeouts().setScriptTimeout(5, TimeUnit.SECONDS);
      } catch (WebDriverException e) {
        log("failed to setScriptTimeout: " + e.toString());
        // harmless, ignore
      }
    }
    // We try to run tests in a fresh window in an existing session,
    // so we can avoid session startup overhead for each test.
    // In some webdriver implementations (such as Safari), the
    // window.open will fail, which is fine, we'll just run the test
    // in the base window and open a new session for the next test.
    JavascriptExecutor jsexec = driver;
    String name = "cajatestwin" + (windowSeq++);
    Boolean result = (Boolean) jsexec.executeScript(
        "return !!window.open('', '" + name + "')");
    if (result) {
      driver.switchTo().window(name);
    }
    return driver;
  }

  void reportVersion(RemoteWebDriver driver) {
    Capabilities caps = driver.getCapabilities();
    String name = caps.getBrowserName();
    if (name == null) { name = "unknown"; }
    String version = caps.getVersion();
    if (version == null) { version = "unknown"; }
    log("- webdriver browser " + name + " version " + version);
  }

  void log(String s) {
    // System.err is captured by junit and goes into ant-reports
    System.err.println(s);

    // FileDescriptor.err is captured by ant and goes to stdout.
    // We don't close err since that would close FileDescriptor.err
    @SuppressWarnings("resource")
    PrintStream err = new PrintStream(
        new FileOutputStream(FileDescriptor.err), true);
    err.println(s);
  }

  String getBrowserType() {
    return TestFlag.BROWSER.getString("firefox");
  }

  void closeWindow() {
    if (driver == null) { return; }
    driver.close();
    try {
      driver.switchTo().window(firstWindow);
    } catch (NoSuchWindowException e) {
      // if makeWindow didn't succeed in creating a new window, then we
      // closed our only window, and we'll need a new webdriver session.
      driver.quit();
      driver = null;
    }
  }

  void keepOpen() {
    keptWindows++;
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
        if (0 < keptWindows) {
          // .close() quits the browser if there are no more windows, but
          // helpers like chromedriver stay running.
          driver.close();
        } else {
          driver.quit();
        }
        driver = null;
      }
    }
  }

  private RemoteWebDriver makeDriver() {
    DesiredCapabilities dc = new DesiredCapabilities();

    // Chrome driver is odd in that the path to Chrome is specified
    // by a desiredCapability when you start a session. The other
    // browser drivers will read a java system property on start.
    String chrome = TestFlag.CHROME_BINARY.getString(null);
    if (chrome != null) {
      dc.setCapability("chrome.binary", chrome);
    }

    String browserType = getBrowserType();
    String webdriver = TestFlag.WEBDRIVER_URL.getString("");
    if (!"".equals(webdriver)) {
      dc.setBrowserName(browserType);
      dc.setJavascriptEnabled(true);
      try {
        return new RemoteWebDriver(new URL(webdriver), dc);
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    } else if ("chrome".equals(browserType)) {
      return new ChromeDriver(dc);
    } else if ("firefox".equals(browserType)) {
      return new FirefoxDriver();
    } else if ("safari".equals(browserType)) {
      // TODO(felix8a): local safari doesn't work yet
      return new SafariDriver();
    } else {
      throw new RuntimeException("No local driver for browser type '"
          + browserType + "'");
    }
  }
}
