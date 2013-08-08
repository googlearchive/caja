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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import com.google.gwt.thirdparty.guava.common.base.Charsets;

/**
 * Wrapper around a WebDriver instance.
 */

class WebDriverHandle {
  private RemoteWebDriver driver = null;
  private boolean canExecuteScript = true;
  private boolean reportedVersion = false;
  private String firstWindow = null;
  private int windowSeq = 0;
  private boolean windowOpened = false;

  // Don't keep more than this many failed test windows. (Otherwise
  // a broken tree can overload a machine with browser windows.)
  private static final int MAX_FAILS_KEPT = 9;
  private static int failsKept = 0;

  WebDriver begin() {
    if (driver == null) {
      driver = makeDriver();
      firstWindow = driver.getWindowHandle();
      reportVersion(driver);
      try {
        driver.manage().timeouts().pageLoadTimeout(15, TimeUnit.SECONDS);
      } catch (WebDriverException e) {
        Echo.echo("failed to set pageLoadTimeout: " + e);
        // harmless, ignore
      }
      try {
        driver.manage().timeouts().setScriptTimeout(5, TimeUnit.SECONDS);
      } catch (WebDriverException e) {
        Echo.echo("failed to setScriptTimeout: " + e);
        // harmless, ignore
      }
    }
    // Try to open a new window
    String name = "cajatest" + (windowSeq++);
    windowOpened = (Boolean) executeScript(
        "return !!window.open('', '" + name + "');");
    if (windowOpened) {
      driver.switchTo().window(name);
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
        Echo.echo("executeScript failed: " + e);
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
    Echo.echo("webdriver: browser " + name + " version " + version);
  }

  void end(boolean passed) {
    // If a test fails, drop the driver handle without close or quit,
    // leaving the browser open, which is helpful for debugging.
    if (!passed && !TestFlag.BROWSER_CLOSE.truthy()
        && failsKept++ < MAX_FAILS_KEPT) {
      driver = null;
    } else {
      try {
        // If we're reusing the same browser, close the current window.
        if (windowOpened) {
          driver.close();
          driver.switchTo().window(firstWindow);
        } else {
          driver.get("about:blank");
        }
      } catch (Exception e) {
        Echo.echo("window cleanup failed: " + e);
        closeDriver();
      }
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

  private static RemoteWebDriver makeDriver() {
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

  public void captureResults(String name, boolean passed) {
    if (driver == null) { return; }

    if (passed && !TestFlag.CAPTURE_PASSES.truthy()) { return; }

    String dir = TestFlag.CAPTURE_TO.getString("");
    if ("".equals(dir)) { return; }

    if (!dir.endsWith("/")) { dir = dir + "/"; }
    dir += passed ? "pass/" : "fail/";
    mkdirs(dir);

    // Try to capture the final html
    try {
      String source = driver.getPageSource();
      if (source != null) {
        saveToFile(dir + name + ".capture.html", source);
      }
    } catch (WebDriverException e) {
      Echo.echo("capture html failed: " + e);
    }

    // Try to capture a screenshot
    if (driver instanceof TakesScreenshot) {
      TakesScreenshot ss = (TakesScreenshot) driver;
      try {
        byte[] bytes = ss.getScreenshotAs(OutputType.BYTES);
        saveToFile(dir + name + ".capture.png", bytes);
      } catch (WebDriverException e) {
        Echo.echo("capture screenshot failed: " + e);
      }
    }

    // Try to capture logs
    try {
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
    } catch (WebDriverException e) {
      Echo.echo("capture logs failed: " + e);
    }
  }

  private static void mkdirs(String dirName) {
    File dir = new File(dirName);
    if (!dir.isDirectory() && !dir.mkdirs()) {
      Echo.echo("couldn't mkdir " + dirName);
    }
  }

  private static void saveToFile(String fileName, String str) {
    saveToFile(fileName, str.getBytes(Charsets.UTF_8));
  }

  private static void saveToFile(String fileName, byte[] bytes) {
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(fileName);
      try {
        out.write(bytes);
      } finally {
        out.close();
      }
    } catch (IOException e) {
      throw new SomethingWidgyHappenedError(e);
    }
  }
}
