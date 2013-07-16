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
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.Logs;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.util.TestFlag;

/**
 * Wrapper around a WebDriver instance.
 */

class WebDriverHandle {
  private RemoteWebDriver driver = null;
  private boolean canExecuteScript = true;
  private boolean reportedVersion = false;

  // Don't keep more than this many failed test windows. (Otherwise
  // a broken tree can overload a machine with browser windows.)
  private static final int MAX_FAILS_KEPT = 9;
  private static int fails_kept = 0;

  WebDriver begin() {
    if (driver == null) {
      driver = makeDriver();
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
    return driver;
  }

  // Selenium 2.33 has trouble executing script on Firefox 23 and later
  private Object executeScript(String script) {
    if (canExecuteScript) {
      try {
        return driver.executeScript(script);
      } catch (WebDriverException e) {
        canExecuteScript = false;
        log("executeScript failed: " + e);
      }
    }
    return null;
  }

  private void reportVersion(RemoteWebDriver driver) {
    if (reportedVersion) { return; }
    reportedVersion = true;
    Capabilities caps = driver.getCapabilities();
    String name = caps.getBrowserName();
    if (name == null) { name = "unknown"; }
    String version = caps.getVersion();
    if (version == null) { version = "unknown"; }
    // Firefox's version is something like "20.0", which doesn't tell
    // you the exact build, so we also try to report buildID.
    String build = (String) executeScript(
        "return String(navigator.buildID || '')");
    if (build != null && !"".equals(build)) {
      version += " build " + build;
    }
    log("webdriver: browser " + name + " version " + version);
  }

  private void log(String s) {
    // System.err is captured by junit and goes into ant-reports
    System.err.println(s);

    // FileDescriptor.err is captured by ant and goes to stdout.
    // We don't close err since that would close FileDescriptor.err
    @SuppressWarnings("resource")
    PrintStream err = new PrintStream(
        new FileOutputStream(FileDescriptor.err), true);
    err.println(s);
  }

  void end(boolean passed) {
    // If a test fails, drop the driver handle without close or quit,
    // leaving the browser open, which is helpful for debugging.
    if (!passed && !TestFlag.BROWSER_CLOSE.truthy()
        && fails_kept++ < MAX_FAILS_KEPT) {
      driver = null;
    } else if (TestFlag.BROWSER_REUSE.truthy()) {
      // TODO(felix8a): this occasionally causes chromedriver to hang
      driver.get("about:blank");
    } else {
      closeDriver();
    }
  }

  void release() {
    closeDriver();
  }

  private void closeDriver() {
    if (driver != null) {
      try {
        driver.quit();
      } finally {
        driver = null;
      }
    }
  }

  private RemoteWebDriver makeDriver() {
    DesiredCapabilities dc = new DesiredCapabilities();

    String browserType = TestFlag.BROWSER.getString("firefox");

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
      if (chromeArgs != null) {
        String[] args = chromeArgs.split(";");
        chromeOpts.addArguments(args);
      }
      dc.setCapability(ChromeOptions.CAPABILITY, chromeOpts);
    }

    String url = TestFlag.WEBDRIVER_URL.getString("");

    if (!"".equals(url)) {
      dc.setBrowserName(browserType);
      dc.setJavascriptEnabled(true);
      try {
        return new RemoteWebDriver(new URL(url), dc);
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

  public void captureResults(String name) {
    if (driver == null) { return; }
    String dir = TestFlag.CAPTURE_TO.getString("");
    if ("".equals(dir)) { return; }
    if (!dir.endsWith("/")) { dir = dir + "/"; }

    // Try to capture the final html
    String source = driver.getPageSource();
    if (source != null) {
      saveToFile(dir + name + ".capture.html", source);
    }

    // Try to capture a screenshot
    if (driver instanceof TakesScreenshot) {
      TakesScreenshot ss = (TakesScreenshot) driver;
      try {
        byte[] bytes = ss.getScreenshotAs(OutputType.BYTES);
        saveToFile(dir + name + ".capture.png", bytes);
      } catch (WebDriverException e) {
        log("screenshot failed: " + e);
      }
    }

    // Try to capture logs
    // This is currently not really useful.
    // - ChromeDriver doesn't support log capture at all
    // - FirefoxDriver gives you Error Console messages not Web Console
    Logs logs = driver.manage().logs();
    if (logs != null) {
      if (logs.getAvailableLogTypes().contains(LogType.BROWSER)) {
        LogEntries entries = logs.get(LogType.BROWSER);
        if (entries != null) {
          StringBuilder sb = new StringBuilder();
          for (LogEntry e : entries) {
            sb.append(e.toString() + "\n");
          }
          saveToFile(dir + name + ".capture.log", sb.toString());
        }
      }
    }
  }

  private void saveToFile(String fileName, String str) {
    try {
      saveToFile(fileName, str.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new SomethingWidgyHappenedError(e);
    }
  }

  private void saveToFile(String fileName, byte[] bytes) {
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(fileName);
      out.write(bytes);
      out.close();
    } catch (IOException e) {
      throw new SomethingWidgyHappenedError(e);
    }
  }
}
