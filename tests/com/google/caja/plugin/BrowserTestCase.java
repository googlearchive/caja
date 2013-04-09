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

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

import com.google.common.base.Joiner;
import org.mortbay.jetty.servlet.Context;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Test case class with tools for controlling a web browser running pages from
 * a local web server.
 * <p>
 * Browser testing is described in more detail at the
 * <a href="http://code.google.com/p/google-caja/wiki/CajaTesting"
 *   >CajaTesting wiki page</a>
 * <p>
 * Useful system properties:
 * <dl>
 *   <dt>caja.test.browser</dt>
 *   <dd>Which browser driver to use. Default is "firefox".</dd>
 *
 *   <dt>caja.test.browserPath</dt>
 *   <dd>Override location of browser executable.  Currently only
 *   for Chrome (sets chrome.binary for webdriver).</dd>
 *
 *   <dt>caja.test.closeBrowser</dt>
 *   <dd>When true, always close browser when done. Normally when a browser
 *   test fails, we try to keep the browser open so the error can be
 *   manually inspected. This flag disables that.</dd>
 *
 *   <dt>caja.test.remote</dt>
 *   <dd>URL of a remote webdriver, which should usually be something like
 *   "http://hostname:4444/wd/hub".  If unset, use a local webdriver.</dd>
 *
 *   <dt>caja.test.serverOnly</dt>
 *   <dd>When true, start server and wait</dd>
 *
 *   <dt>caja.test.serverPort</dt>
 *   <dd>What port to use for the localhost webserver. Default is 8000 when
 *   using one of the manual testing options (serverOnly or startAndWait).
 *   Otherwise, default is 0 (which chooses any available port).</dd>
 *
 *   <dt>caja.test.startAndWait</dt>
 *   <dd>When true, start server and browser and wait</dd>
 *
 *   <dt>caja.test.thishostname</dt>
 *   <dd>Hostname that a remote browser should use to contact the
 *   localhost server. If unset, guesses a non-loopback hostname.</dd>
 * </dl>
 * <p>
 * Type parameter D is for data passed in to subclass overrides of driveBrowser.
 *
 * @author maoziqing@gmail.com (Ziqing Mao)
 * @author kpreid@switchb.org (Kevin Reid)
 */
public abstract class BrowserTestCase<D> extends CajaTestCase {
  // TODO(felix8a): gather flags
  private static final String CLOSE_BROWSER = "caja.test.closeBrowser";
  private static final String REMOTE = "caja.test.remote";
  private static final String SERVER_ONLY = "caja.test.serverOnly";
  private static final String SERVER_PORT = "caja.test.serverPort";
  private static final String START_AND_WAIT = "caja.test.startAndWait";

  // We acquire a WebDriverHandle on construction because the test runner
  // constructs all the TestCase objects before running any of them, and
  // we want WebDriverHandle's refcount to stay nonzero as long as possible.
  private WebDriverHandle wdh = new WebDriverHandle();

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

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Override
  public void tearDown() throws Exception {
    // WebDriverHandle closes the browser after all tests call release.
    wdh.release();
    setTestBuildVersion(null);
    super.tearDown();
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

  /**
   * Start the web server and browser, go to pageName, call driveBrowser(driver,
   * pageName), and then clean up.
   */
  protected String runBrowserTest(String pageName, String... params)
      throws Exception {
    return runBrowserTest(pageName, null, params);
  }

  protected String runBrowserTest(String pageName, D data,
      String... params) throws Exception {
    int serverPort = intProp(SERVER_PORT, 0);

    if (flag(SERVER_ONLY) || flag(START_AND_WAIT)) {
      pageName = "test-index.html";
      params = null;
      serverPort = intProp(SERVER_PORT, 8000);
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

      String localhost = "localhost";
      if (System.getProperty(REMOTE) != null) {
        localhost = localServer.hostname();
      }
      String page = "http://" + localhost + ":" + localServer.getPort()
              + "/ant-testlib/com/google/caja/plugin/" + pageName;
      if (params != null && params.length > 0) {
        page += "?" + Joiner.on("&").join(params);
      }
      getErr().println("- Try " + page);

      if (flag(SERVER_ONLY)) {
        Thread.currentThread().join();
      }
      WebDriver driver = wdh.makeWindow();
      driver.get(page);
      if (flag(START_AND_WAIT)) {
        Thread.currentThread().join();
      }

      result = driveBrowser(driver, data, pageName);
      passed = true;
    } finally {
      localServer.stop();
      // It's helpful for debugging to keep failed windows open.
      if (passed || isKnownFailure() || flag(CLOSE_BROWSER)) {
        wdh.closeWindow();
      }
    }
    return result;
  }

  static protected boolean flag(String name) {
    String value = System.getProperty(name);
    return value != null && !"".equals(value) && !"0".equals(value)
        && !"false".equalsIgnoreCase(value);
  }

  static protected int intProp(String name, int dflt) {
    String value = System.getProperty(name);
    if (value == null || "".equals(value)) {
      return dflt;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      getErr().println("Invalid value " + value + " for " + name);
      throw e;
    }
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

  protected static String[] add(String[] arr, String... rest) {
    String[] result = new String[arr.length + rest.length];
    System.arraycopy(arr, 0, result, 0, arr.length);
    System.arraycopy(rest, 0, result, arr.length, rest.length);
    return result;
  }

  /**
   * Do what should be done with the browser.
   *
   * @param data
   *          Parameter from runBrowserTest, for use by subclasses; must be null
   *          but subclasses overriding this method may make use of it.
   * @param pageName
   *          The tail of a URL. Unused in this implementation.
   */
  protected String driveBrowser(
      final WebDriver driver, final D data, final String pageName) {
    if (data != null) {
      throw new IllegalArgumentException(
          "data parameter is not used and should be null");
    }

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

    // check the title of the document
    String title = driver.getTitle();
    assertTrue("The title shows " + title, title.contains("all tests passed"));
    return title;
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

  /**
   * Helper to respond to browser differences.
   */
  D firefoxVsChrome(D firefox, D chrome) {
    String type = wdh.getBrowserType();
    // In the event that we support testing on more browsers, this should be
    // redesigned appropriately, rather than being a long if-else.
    if ("firefox".equals(type)) {
      return firefox;
    } else if ("chrome".equals(type)) {
      return chrome;
    } else {
      return firefox;
    }
  }
}
