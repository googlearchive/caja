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
import org.mortbay.jetty.servlet.Context;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.WebDriver;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Test case class with tools for controlling a web browser running pages from a
 * local web server.
 *
 * <p>
 * To debug, run <pre>
 * ant -D{@link BrowserTestCase#START_AND_WAIT_FLAG caja.BrowserTestCase.startAndWait}=true -Dtest.filter=&lt;TestCaseName&gt; runtests
 * </pre>.  Be sure to fill in {@code <TestCaseName>} with the name of the test
 * you want to debug and look for a URL in the test log output.
 * If you need more fine-grained filtering, use {@code -Dtest.method.filter} to
 * filter by method name.
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

  private static final String START_AND_WAIT_FLAG =
      "caja.BrowserTestCase.startAndWait";
  private static final long START_AND_WAIT_MILLIS =
      1000 * 60 * 60 * 24;  // 24 hours

  /**
   * Start the web server and browser, go to pageName, call driveBrowser(driver,
   * pageName), and then clean up.
   */
  protected void runBrowserTest(String pageName) throws Exception {
    if (checkHeadless()) return;  // TODO: print a warning here?
    localServer.start();
    String testUrl = ("http://localhost:" + portNumber
                      + "/ant-lib/com/google/caja/plugin/test-index.html");
    if (System.getProperty(START_AND_WAIT_FLAG) != null) {
      // The test runner may catch output so go directly to file descriptor 2.
      OutputStream out = new FileOutputStream(FileDescriptor.err);
      try {
        // Print out the URL so that someone can use ant -Dtest.filter to
        // choose the specific test they want instead of having to compute the
        // URL by inspection of the test code.
        out.write(("Waiting for interactive test run.\nTry " + testUrl + "\n")
                  .getBytes("UTF-8"));
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      // No need to release a file descriptor that was open prior.
      try {
        Thread.sleep(START_AND_WAIT_MILLIS);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    try {
      WebDriver driver = mwwd.newWindow();
      driver.get(testUrl);
      driveBrowser(driver, pageName);
      driver.close();
      // Note that if the tests fail, this will not be reached and the window
      // will not be closed. This is useful for debugging test failures.
    } finally {
      localServer.stop();
    }
  }

  protected void runTestDriver(String testDriver) throws Exception {
    runTestDriver(testDriver, false);
    runTestDriver(testDriver, true);
  }
  
  protected void runTestCase(String testCase) throws Exception {
    runTestCase(testCase, false);
    runTestCase(testCase, true);
  }

  protected void runTestDriver(String testDriver, boolean es5)
      throws Exception {
    runBrowserTest("browser-test-case.html?es5=" + es5
        + "&test-driver=" + testDriver);
  }

  protected void runTestCase(String testCase, boolean es5) throws Exception {
    runBrowserTest("browser-test-case.html?es5=" + es5
        + "&test-case=" + testCase);
  }

  /**
   * Do what should be done with the browser.
   * @param pageName The tail of a URL.  Unused in this implementation
   */
  protected void driveBrowser(final WebDriver driver, final String pageName) {
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
    public WebDriver newWindow() {
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
