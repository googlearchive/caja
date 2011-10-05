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
import com.google.caja.service.CajolingService;
import com.google.caja.service.CajolingServlet;
import com.google.caja.util.CajaTestCase;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.WebDriver;

import java.util.List;

/**
 * Test case class with tools for controlling a web browser running pages from a
 * local web server.
 *
 * @author maoziqing@gmail.com (Ziqing Mao)
 * @author kpreid@switchb.org (Kevin Reid)
 */
public abstract class BrowserTestCase extends CajaTestCase {
  protected Server server;

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

  protected final RewritingResourceHandler cajaStatic =
      new RewritingResourceHandler();
  { cajaStatic.setResourceBase("./ant-war/"); }

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

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Override
  public void tearDown() throws Exception {
    cajaStatic.clear();
    setTestBuildVersion(null);
    super.tearDown();
  }

  protected void closeWebDriver() {
    mwwd.stop();
  }

  /**
   * Start a local web server on the port specified by portNumber().
   */
  protected void startLocalServer() {
    server = new Server(portNumber());

    // static file serving for tests
    final ResourceHandler resource_handler = new ResourceHandler();
    resource_handler.setResourceBase(".");

    // caja (=playground for now) server under /caja directory
    final String subdir = "/caja";
    final ContextHandler caja = new ContextHandler(subdir);
    {
      // TODO(kpreid): deploy the already-configured war instead of manually
      // plumbing
      final String service = "/cajole";

      // cajoling service -- Servlet setup code gotten from
      // <http://docs.codehaus.org/display/JETTY/Embedding+Jetty> @ 2010-06-30
      Context servlets = new Context(server, "/", Context.NO_SESSIONS);
      servlets.addServlet(
        new ServletHolder(
          new CajolingServlet(
            new CajolingService(BuildInfo.getInstance(),
                                "http://localhost:" + portNumber() +
                                    subdir + service))),
        service);

      // Hook for subclass to add more servlets
      addServlets(servlets);

      final HandlerList handlers = new HandlerList();
      handlers.setHandlers(new Handler[] {
          cajaStatic,
          servlets,
          new DefaultHandler()});
      caja.setHandler(handlers);
    }

    final HandlerList handlers = new HandlerList();
    handlers.setHandlers(new Handler[] {
        resource_handler,
        caja,
        new DefaultHandler()});
    server.setHandler(handlers);

    try {
      server.start();
    } catch (Exception e) {
      fail("Starting the local web server failed!");
    }
  }

  /**
   * Stop the local web server
   */
  protected void stopLocalServer() {
    try {
      server.stop();
    } catch (Exception e) {
      // the server will be turned down when the test exits
    }
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

  /**
   * The RewritingResourceHandler used for static files.
   */
  protected RewritingResourceHandler getCajaStatic() {
    return cajaStatic;
  }

  /**
   * The port the local web server will run on.
   */
  protected int portNumber() {
    return 8000;
  }

  private static final String START_AND_WAIT_FLAG =
      "caja.BrowserTestCase.startAndWait";
  private static final long START_AND_WAIT_MILLIS =
      1000 * 60 * 60 * 24;  // 24 hours

  /**
   * Start the web server and browser, go to pageName, call driveBrowser(driver,
   * pageName), and then clean up.
   */
  protected void runBrowserTest(String pageName) {
    if (checkHeadless()) return;  // TODO: print a warning here?
    startLocalServer();
    if (System.getProperty(START_AND_WAIT_FLAG) != null) {
      try {
        Thread.sleep(START_AND_WAIT_MILLIS);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    try {
      WebDriver driver = mwwd.newWindow();
      driver.get("http://localhost:" + portNumber()
                 + "/ant-lib/com/google/caja/plugin/"
                 + pageName);
      driveBrowser(driver, pageName);
      driver.close();
      // Note that if the tests fail, this will not be reached and the window
      // will not be closed. This is useful for debugging test failures.
    } finally {
      stopLocalServer();
    }
  }

  protected void runTestDriver(String testDriver) {
    runTestDriver(testDriver, false);
    runTestDriver(testDriver, true);
  }

  protected void runTestCase(String testCase) {
    runTestCase(testCase, false);
    runTestCase(testCase, true);
  }

  protected void runTestDriver(String testDriver, boolean es5) {
    runBrowserTest("browser-test-case.html?es5=" + es5
        + "&test-driver=" + testDriver);
  }

  protected void runTestCase(String testCase, boolean es5) {
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
