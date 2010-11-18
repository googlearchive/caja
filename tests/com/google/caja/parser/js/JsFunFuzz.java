// Copyright (C) 2008 Google Inc.
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

package com.google.caja.parser.js;

import com.google.caja.lexer.ParseException;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Executor;
import com.google.caja.util.RhinoTestBed;

import java.io.IOException;

/**
 * Fuzzer for testing JS parsing using jsfunfuzz
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class JsFunFuzz extends CajaTestCase {

  /**
   * Number of fuzzed cases to generate
   */
  private static final int MAX_NUMBER_OF_TESTS = 10;

  /**
   * Generate and return a new javascript string
   * @return snippet of fuzzed javascript
   */
  private String fudgeroonify() {
    try {
      return (String) RhinoTestBed.runJs(
          new Executor.Input(getClass(), "/js/jsfunfuzz/jsfunfuzz.js"));
    } catch (IOException e) {
      fail("JS Fuzzer jsfunfuzz.js not found");
      return null;
    }
  }

  /**
   * Linewraps test cases at the 60 character boundary
   *
   * Useful for generating java test cases for pasting
   * into a JUnit test which meet the style guide
   */
  private String quoteAndWrap(String testCase) {
    StringBuilder testBlock = new StringBuilder( "      \"\"\n");
    for (int start = 0; start < testCase.length(); start += 60) {
      int end = (start + 60 > testCase.length())
          ? testCase.length()
          : start + 60;
      String piece = testCase.substring(start, end);
      testBlock.append("     + ");
      testBlock.append("\"" + piece + "\"\n");
    }
    return testBlock.toString();
  }

  /**
   * Generates a snippet of Java code suitable for pasting into a JUnit test
   */
  private String generateTestCase(String testCase, int testCount, String e) {
      StringBuilder test = new StringBuilder();
      test.append("  // Should not throw " + e + "\n");
      test.append("  public final void testParse" + testCount + "() {\n");
      test.append("    throwsParseException(\n");
      test.append(quoteAndWrap(testCase) + "\n");
      test.append("    );\n");
      test.append(")}\n\n");
      return test.toString();
  }

  /**
   * Test parser against a snippet of fuzzed javascript
   *
   * Fail if the parser throws anything other than a ParseException.
   * Formats and prints the failing case to stderr for pasting in to JUnit tests
   */
  public final void testParsesFuzz() {
    StringBuilder testCases = new StringBuilder();
    String randomJs = "";
    boolean failed = false;
    for (int testCaseCount = 0; testCaseCount < MAX_NUMBER_OF_TESTS;
         testCaseCount++) {
      try {
        randomJs = fudgeroonify();
        js(fromString(randomJs));
      } catch (ParseException e) {
        // Only ParseExceptions are ok
      } catch (Throwable e) {
        failed = true;
        testCases.append(
            generateTestCase(randomJs, testCaseCount, e.getMessage()));
      }
    }
    System.err.print(testCases.toString());
    if (failed) {
      fail();
    }
  }
}