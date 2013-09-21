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

package com.google.caja.tracing;

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.parser.quasiliteral.RewriterMessageType;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Executor;
import com.google.caja.util.Lists;
import com.google.caja.util.RhinoTestBed;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import java.io.StringReader;
import java.util.List;

/**
 * Tests for the tracing rewriter.
 *
 * @author ihab.awad@gmail.com
 */
@RunWith(value = AllTests.class)
public final class TracingRewriterTest {

  //////////////////////////////////////////////////////////////////////////////
  // JUnit test framework-fu to get tests dynamically added by JavaScript.
  // This allows us to author the JavaScript code for the tests in a JavaScript
  // file of its own, rather than embedding it as strings inside Java code.

  public static Test suite() throws Exception {
    TestSuite suite = new TestSuite();
    for (Test test : collectTestCases()) {
      System.err.println(test);
      suite.addTest(test);
    }
    return suite;
  }

  public static class TestCollector {
    public List<Test> tests = Lists.newArrayList();

    public void addTraceTest(
        String name, String codeToTest, String traceEvents) {
      tests.add(new TraceTest(name, codeToTest, traceEvents));
    }

    public void addCompileErrorTest(
        String name, String codeToTest, String type, String level) {
      tests.add(new CompileErrorTest(name, codeToTest, type, level));
    }
  }

  private static Test[] collectTestCases() throws Exception {
    TestCollector tc = new TestCollector();
    RhinoTestBed.runJs(
        tc,
        new Executor.Input(
            TracingRewriterTest.class,
            "tracingRewriterTestCases.js"));
    return tc.tests.toArray(new Test[] {});
  }

  //////////////////////////////////////////////////////////////////////////////
  // Actual test cases

  public abstract static class TracingTestCase extends CajaTestCase {

    protected final String codeToTest;

    public TracingTestCase(String name, String codeToTest) {
      super(name);
      this.codeToTest = codeToTest;
    }

    protected String rewrite(String code) throws Exception {
      ParseTreeNode input = new UncajoledModule(js(fromString(code)));
      ParseTreeNode output =
          new com.google.caja.tracing.TracingRewriter(mq).expand(input);
      return "'use strict';\n" + render(output);
    }

    protected Executor.Input resourceInput(String name) throws Exception {
      return new Executor.Input(getClass(), name);
    }

    protected Executor.Input stringInput(String code) throws Exception {
      return new Executor.Input(new StringReader(code), getName());
    }
  }

  public static class TraceTest extends TracingTestCase {

    protected final String traceEvents;

    public TraceTest(String name, String codeToTest, String traceEvents) {
      super(name, codeToTest);
      this.traceEvents = traceEvents;
    }

    public void runTest() throws Exception {
      RhinoTestBed.runJs(
          resourceInput(
              "/com/google/caja/tracing/tracing.js"),
          stringInput(
              "var RESULT = (void 0);"),
          stringInput(
              codeToTest),
          stringInput(
              "var EXPECTED_RESULT = RESULT;" +
              "RESULT = (void 0);" +
              "TRACING.clear();"),
          stringInput(
              traceEvents),
          stringInput(
              "var EXPECTED_TRACE = TRACING.getTrace();" +
              "TRACING.clear();"),
          stringInput(
              rewrite(codeToTest)),
          stringInput(
              "var ACTUAL_RESULT = RESULT;" +
              "RESULT = (void 0);" +
              "var ACTUAL_TRACE = TRACING.getTrace();" +
              "TRACING.clear();"),
          resourceInput(
              "tracingRewriterTestAssertTraces.js"));
    }
  }

  public static class CompileErrorTest extends TracingTestCase {

    protected final String type;
    protected final String level;

    public CompileErrorTest(
        String name, String codeToTest, String type, String level) {
      super(name, codeToTest);
      this.type = type;
      this.level = level;
    }

    public void runTest() throws Exception {
      try {
        rewrite(codeToTest);
      } catch (Exception e) {}
      assertMessage(
          RewriterMessageType.valueOf(type),
          MessageLevel.valueOf(level));
    }
  }
}
