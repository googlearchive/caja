// Copyright (C) 2009 Google Inc.
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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.mortbay.jetty.servlet.Context;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.LocalServer;
import com.google.caja.util.TestFlag;
import com.google.caja.util.ThisHostName;
import com.google.common.base.Joiner;

/**
 * Test case class with tools for controlling a web browser running pages from
 * a local web server.
 * <p>
 * Browser testing is described in more detail at the
 * <a href="http://code.google.com/p/google-caja/wiki/CajaTesting"
 *   >CajaTesting wiki page</a>
 *
 * @author maoziqing@gmail.com (Ziqing Mao)
 * @author kpreid@switchb.org (Kevin Reid)
 */
public abstract class BrowserTestCase {
  // Constructed @BeforeClass to share a single web browser.
  private static WebDriverHandle wdh;

  protected String testBuildVersion = null;

  protected final BuildInfo buildInfo = new BuildInfo() {
    @Override public void addBuildInfo(MessageQueue mq) {
      BuildInfo.getInstance().addBuildInfo(mq);
    }
    @Override public String getBuildInfo() {
      return BuildInfo.getInstance().getBuildInfo();
    }
    @Override public String getBuildVersion() {
      return (testBuildVersion != null)
          ? testBuildVersion
          : BuildInfo.getInstance().getBuildVersion();
    }
    @Override public String getBuildTimestamp() {
      return BuildInfo.getInstance().getBuildTimestamp();
    }
    @Override public long getCurrentTime() {
      return BuildInfo.getInstance().getCurrentTime();
    }
  };

  private final LocalServer localServer = new LocalServer(
      new LocalServer.ConfigureContextCallback() {
        @Override public void configureContext(Context ctx) {
          addServlets(ctx);
        }
      });

  @BeforeClass
  public static void setUpClass() throws Exception {
    wdh = new WebDriverHandle();
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    wdh.release();
  }

  /**
   * Set a custom build version for testing. This will be used by the cajoling
   * service to stamp outgoing cajoled modules. Set this to <code>null</code>
   * to disable custom test build version and revert to default behavior.
   *
   * @param version the desired test build version.
   */
  protected void setTestBuildVersion(String version) {
    testBuildVersion = version;
  }

  static protected PrintStream errStream = null;

  // The ant junit runner captures System.err.  This returns a handle
  // to fd 2 for messages we want to go to the real stderr.
  static protected PrintStream getErr() {
    if (errStream == null) {
      errStream = new PrintStream(
          new FileOutputStream(FileDescriptor.err), true);
    }
    return errStream;
  }

  protected String runBrowserTest(boolean isKnownFailure, String pageName,
      String... params) throws Exception {
    int serverPort = TestFlag.SERVER_PORT.getInt(0);

    if (TestFlag.DEBUG_BROWSER.truthy() || TestFlag.DEBUG_SERVER.truthy()) {
      pageName = "test-index.html";
      params = null;
      serverPort = TestFlag.SERVER_PORT.getInt(8000);
    }

    String result = "";
    boolean passed = false;
    try {
      try {
        localServer.start(serverPort);
      } catch (Exception e) {
        getErr().println(e);
        throw e;
      }

      String localhost = TestFlag.SERVER_HOSTNAME.getString(null);
      if (localhost == null) {
        if (TestFlag.WEBDRIVER_URL.truthy()) {
          localhost = ThisHostName.value();
        } else {
          localhost = "localhost";
        }
      }
      String page = "http://" + localhost + ":" + localServer.getPort()
              + "/ant-testlib/com/google/caja/plugin/" + pageName;
      if (params != null && params.length > 0) {
        page += "?" + Joiner.on("&").join(params);
      }
      getErr().println("- Try " + page);

      if (TestFlag.DEBUG_SERVER.truthy()) {
        Thread.currentThread().join();
      }
      WebDriver driver = wdh.makeWindow();
      driver.get(page);
      if (TestFlag.DEBUG_BROWSER.truthy()) {
        Thread.currentThread().join();
      }

      result = driveBrowser(driver);
      passed = true;
    } finally {
      localServer.stop();
      // It's helpful for debugging to keep failed windows open.
      if (!passed && !isKnownFailure && !TestFlag.BROWSER_CLOSE.truthy()) {
        wdh.keepOpen();
      } else {
        wdh.closeWindow();
      }
    }
    return result;
  }

  protected static String escapeUri(String s) {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeUri(s, sb);
    return sb.toString();
  }

  protected static String[] add(String[] arr, String... rest) {
    String[] result = new String[arr.length + rest.length];
    System.arraycopy(arr, 0, result, 0, arr.length);
    System.arraycopy(rest, 0, result, arr.length, rest.length);
    return result;
  }

  /**
   * Do what should be done with the browser.
   */
  protected String driveBrowser(final WebDriver driver) {
    // 20s because test-domado-dom startup is very very very slow in es53 mode,
    // and something we're doing is leading to huge unpredictable slowdowns
    // in random test startup; perhaps we're holding onto a lot of ram and
    // we're losing on swapping/gc time.  unclear.
    countdown(20000, 200, new Countdown() {
      @Override public String toString() { return "startup"; }
      public int run() {
        List<WebElement> readyElements = driver.findElements(
            By.className("readytotest"));
        return readyElements.size() == 0 ? 1 : 0;
      }
    });

    // 4s because test-domado-dom-events has non-click tests that can block
    // for a nontrivial amount of time, so our clicks aren't necessarily
    // processed right away.
    countdown(4000, 200, new Countdown() {
      private List<WebElement> clickingList = null;
      @Override public String toString() {
        return "clicking done (Remaining elements = " +
            renderElements(clickingList) + ")";
      }
      public int run() {
        clickingList = driver.findElements(By.xpath(
            "//*[contains(@class,'clickme')]/*"));
        for (WebElement e : clickingList) {
          // TODO(felix8a): webdriver fails if e has been removed
          e.click();
        }
        return clickingList.size();
      }
    });

    // override point
    waitForCompletion(driver);

    // check the title of the document
    String title = driver.getTitle();
    assertTrue("The title shows " + title, title.contains("all tests passed"));
    return title;
  }

  /**
   * After startup and clicking is done, wait an appropriate amount of time
   * for tests to pass.
   */
  protected void waitForCompletion(final WebDriver driver) {
    // 10s because the es53 cajoler is slow the first time it runs.
    countdown(10000, 200, new Countdown() {
      private List<WebElement> waitingList = null;
      @Override public String toString() {
        return "completion (Remaining elements = " +
            renderElements(waitingList) + ")";
      }
      public int run() {
        // TODO(felix8a): this used to check for just class "waiting", but now
        // "waiting" is redundant and should be removed.
        waitingList = driver.findElements(By.xpath(
            "//*[contains(@class,'testcontainer')"
            + " and not(contains(@class,'done'))"
            + " and not(contains(@class,'manual'))]"));
        return waitingList.size();
      }
    });
  }

  /**
   * Run 'c' every 'intervalMillis' until it returns 0,
   * or 'timeoutMillis' have passed since the value has changed.
   */
  protected static void countdown(
      int timeoutMillis, int intervalMillis, Countdown c) {
    int lastValue = -1;
    long endTime = System.currentTimeMillis() + timeoutMillis;
    int value;
    while ((value = c.run()) != 0) {
      long now = System.currentTimeMillis();
      if (value != lastValue) {
        endTime = now + timeoutMillis;
        lastValue = value;
      }
      if (endTime < now) {
        fail(timeoutMillis + " ms passed while waiting for: " + c);
      }
      try {
        Thread.sleep(intervalMillis);
      } catch (InterruptedException e) {
        // keep going
      }
    }
  }

  protected static String renderElements(List<WebElement> elements) {
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    for (int i = 0, n = elements.size(); i < n; i++) {
      if (i != 0) { sb.append(", "); }
      WebElement el = elements.get(i);
      sb.append('<').append(el.getTagName());
      String id = el.getAttribute("id");
      if (id != null) {
        sb.append(" id=\"");
        Escaping.escapeXml(id, false, sb);
        sb.append('"');
      }
      String className = el.getAttribute("class");
      if (className != null) {
        sb.append(" class=\"");
        Escaping.escapeXml(className, false, sb);
        sb.append('"');
      }
      sb.append('>');
    }
    sb.append(']');
    return sb.toString();
  }

  /**
   * Add servlets as desired specific to a given test case.
   *
   * @param servlets a Jetty Context to which servlets can be added.
   */
  protected void addServlets(Context servlets) {
    // Adds none but may be overridden.
  }

  public interface Countdown {
    int run();
  }
}
