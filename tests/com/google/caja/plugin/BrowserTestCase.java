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

import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.LocalServer;
import com.google.caja.util.RewritingResourceHandler;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.util.List;

import com.google.common.base.Joiner;
import org.mortbay.jetty.servlet.Context;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 * Test case class with tools for controlling a web browser running pages from a
 * local web server.
 *
 * @author maoziqing@gmail.com (Ziqing Mao)
 * @author kpreid@switchb.org (Kevin Reid)
 */
public abstract class BrowserTestCase extends CajaTestCase {
  // This being static is a horrible kludge to be able to reuse the Firefox
  // instance between individual tests. There is no narrower scope we can use,
  // unless we were to move to JUnit 4 style tests, which have per-class setup.
  static final MultiWindowWebDriver mwwd = Boolean.getBoolean("test.headless")
      ? null : new MultiWindowWebDriver();
  static {
    if (mwwd != null) {
      Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
        public void run() {
          mwwd.stop();
        }
      }));
    }
  }

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

  private final int portNumber = 8000;

  private final LocalServer localServer = new LocalServer(
      portNumber,
      new LocalServer.ConfigureContextCallback() {
        @Override public void configureContext(Context ctx) {
          addServlets(ctx);
        }
      });

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Override
  public void tearDown() throws Exception {
    localServer.getCajaStatic().clear();
    setTestBuildVersion(null);
    super.tearDown();
  }

  protected void closeWebDriver() {
    mwwd.stop();
  }

  protected RewritingResourceHandler getCajaStatic() {
    return localServer.getCajaStatic();
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

  private static final String SERVER_ONLY =
      "caja.BrowserTestCase.serverOnly";
  private static final String START_AND_WAIT =
      "caja.BrowserTestCase.startAndWait";

  /**
   * Start the web server and browser, go to pageName, call driveBrowser(driver,
   * pageName), and then clean up.
   */
  protected String runBrowserTest(String pageName, String... params)
      throws Exception {
    if (flag(SERVER_ONLY) || flag(START_AND_WAIT)) {
      pageName = "test-index.html";
    }
    String page = "http://localhost:" + portNumber
        + "/ant-testlib/com/google/caja/plugin/" + pageName;
    if (params.length > 0) {
      page += "?" + Joiner.on("&").join(params);
    }
    // The test runner may catch output so go directly to file descriptor 2.
    PrintStream err = new PrintStream(
        new FileOutputStream(FileDescriptor.err), false, "UTF-8");
    err.println("- Try " + page);
    String result = "";
    try {
      try {
        localServer.start();
      } catch (Exception e) {
        err.println(e);
        throw e;
      }

      if (flag(SERVER_ONLY)) {
        Thread.currentThread().join();
      }

      WebDriver driver = mwwd.newWindow();
      driver.get(page);
      if (flag(START_AND_WAIT)) {
        Thread.currentThread().join();
      }

      result = driveBrowser(driver, pageName);
      driver.close();
      // Note that if the tests fail, this will not be reached and the window
      // will not be closed. This is useful for debugging test failures.
    } finally {
      localServer.stop();
    }
    return result;
  }

  protected boolean flag(String name) {
    return System.getProperty(name) != null;
  }

  protected String runTestDriver(String testDriver, String... params)
      throws Exception {
    return runTestDriver(testDriver, true, params) + "\n"
        + runTestDriver(testDriver, false, params);
  }

  protected String runTestCase(String testCase, String... params)
      throws Exception {
    return runTestCase(testCase, true, params) + "\n"
        + runTestCase(testCase, false, params);
  }

  protected String runTestDriver(
      String testDriver, boolean es5, String... params)
      throws Exception {
    return runBrowserTest("browser-test-case.html",
        add(params,
            "es5=" + es5,
            "test-driver=" + escapeUri(testDriver)));
  }

  protected String runTestCase(
      String testCase, boolean es5, String... params)
      throws Exception {
    return runBrowserTest("browser-test-case.html",
        add(params,
            "es5=" + es5,
            "test-case=" + escapeUri(testCase)));
  }

  protected static String escapeUri(String s) {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeUri(s, sb);
    return sb.toString();
  }

  private static String[] add(String[] arr, String... rest) {
    String[] result = new String[arr.length + rest.length];
    System.arraycopy(arr, 0, result, 0, arr.length);
    System.arraycopy(rest, 0, result, arr.length, rest.length);
    return result;
  }

  /**
   * Do what should be done with the browser.
   * @param pageName The tail of a URL.  Unused in this implementation
   */
  protected String driveBrowser(final WebDriver driver, final String pageName) {
    poll(20000, 200, new Check() {
      @Override public String toString() { return "startup"; }
      public boolean run() {
        List<WebElement> readyElements = driver.findElements(
            By.xpath("//*[@class='readytotest']"));
        return readyElements.size() != 0;
      }
    });

    poll(20000, 1000, new Check() {
      private List<WebElement> clickingList = null;
      @Override public String toString() {
        return "clicking done (Remaining elements = " +
            renderElements(clickingList) + ")";
      }
      public boolean run() {
        clickingList = driver.findElements(By.xpath(
            "//*[contains(@class,'clickme')]/*"));
        for (WebElement e : clickingList) {
          e.click();
        }
        return clickingList.isEmpty();
      }
    });

    poll(80000, 1000, new Check() {
      private List<WebElement> waitingList = null;
      @Override public String toString() {
        return "completion (Remaining elements = " +
            renderElements(waitingList) + ")";
      }
      public boolean run() {
        waitingList =
            driver.findElements(By.xpath("//*[contains(@class,'waiting')]"));
        return waitingList.isEmpty();
      }
    });

    // check the title of the document
    String title = driver.getTitle();
    assertTrue("The title shows " + title, title.contains("all tests passed"));
    return title;
  }

  /**
   * Run 'c' every 'intervalMillis' milliseconds until it returns true or
   * 'timeoutSecs' seconds have passed (in which case, fail).
   */
  protected static void poll(
      int timeoutMillis, int intervalMillis, Check c) {
    int rounds = 0;
    int limit = timeoutMillis / intervalMillis;
    for (; rounds < limit; rounds++) {
      if (c.run()) {
        break;
      }
      try {
        Thread.sleep(intervalMillis);
      } catch (InterruptedException e) {
        // keep going
      }
    }
    assertTrue(
        timeoutMillis + " ms passed while waiting for: " + c + ".",
        rounds < limit);
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

  public interface Check {
    boolean run();
  }

  /**
   * A wrapper for WebDriver providing the ability to open new windows on
   * demand.
   *
   * It lazily creates the actual WebDriver upon newWindow(). There is still
   * only one actual WebDriver.
   *
   * @author kpreid@switchb.org (Kevin Reid)
   */
  private static class MultiWindowWebDriver {
    private WebDriver driver;
    private String firstWindowHandle;
    private int nextName = 0;

    /**
     * Create a new window and return the WebDriver, which has been switched
     * to it.
     */
    public WebDriver newWindow() throws Exception {
      if (driver == null) {
        driver = new FirefoxDriver();
        driver.get("about:blank");
        firstWindowHandle = driver.getWindowHandle();
      }

      driver.switchTo().window(firstWindowHandle);
      String name = "btcwin" + (nextName++);
      driver.get("javascript:window.open('','" + name + "');'This%20is%20the%20"
          + "BrowserTestCase%20bootstrap%20window.'");
      driver.switchTo().window(name);
      return driver;
    }

    /**
     * Close the browser if and only if all opened windows have been closed;
     * else clean up but leave those windows open.
     */
    public void stop() {
      if (driver == null) {
        return;
      }

      if (driver.getWindowHandles().size() <= 1) {
        // quit if no failures (extra windows)
        driver.quit();
      } else {
        // but our window-opener window is not interesting
        driver.switchTo().window(firstWindowHandle);
        driver.close();
      }

      driver = null;
    }
  }
}
