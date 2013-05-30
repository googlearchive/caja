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
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;

import com.google.caja.util.TestFlag;

/**
 * Wrapper around a WebDriver instance for our multi-window usage pattern.
 *
 * WebDriver browser setup is kind of slow, so we reuse a browser for an entire
 * class's worth of tests via @BeforeClass and @AfterClass.
 */

class WebDriverHandle {
  private RemoteWebDriver driver = null;
  private String firstWindow = null;
  private int windowSeq = 1;
  private int keptWindows = 0;

  WebDriverHandle() {
  }

  WebDriver makeWindow() {
    if (driver == null) {
      driver = makeDriver();
      firstWindow = driver.getWindowHandle();
      reportVersion(driver);
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
    String name = "cajatestwin" + (windowSeq++);
    Boolean result = (Boolean) driver.executeScript(
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
    // Firefox's version is something like "20.0", which doesn't tell
    // you the exact build, so we also try to report buildID.
    String build = (String) driver.executeScript(
        "return String(navigator.buildID || '')");
    if (build != null && !"".equals(build)) {
      version += " build " + build;
    }
    log("webdriver: browser " + name + " version " + version);
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
    if (firstWindow == null) {
      // we failed sometime during initialization; quit and try again.
      driver.quit();
      driver = null;
      return;
    }
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

  private RemoteWebDriver makeDriver() {
    DesiredCapabilities dc = new DesiredCapabilities();

    String browserType = getBrowserType();

    if ("chrome".equals(browserType)) {
      // Chrome driver is odd in that the path to Chrome is specified
      // by a desiredCapability when you start a session. The other
      // browser drivers will read a java system property on start.
      // This applies to both remote Chrome and local Chrome.
      ChromeOptions chromeOpts = new ChromeOptions();
      String chromeBin = TestFlag.CHROME_BINARY.getString(null);
      if (chromeBin != null) {
        chromeOpts.setBinary(chromeBin);
      }
      String chromeArgs = TestFlag.CHROME_ARGS.getString(null);
      if (chromeArgs!= null) {
        String[] args = chromeArgs.split(";");
        chromeOpts.addArguments(args);
      }
      dc.setCapability(ChromeOptions.CAPABILITY, chromeOpts);
    }

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
