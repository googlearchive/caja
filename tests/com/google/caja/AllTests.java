// Copyright (C) 2005 Google Inc.
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

package com.google.caja;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Pattern;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author mikesamuel@gmail.com
 */
public class AllTests {
  private static FilenameFilter testClassFilter = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      File test = new File(dir, name);
      return !name.startsWith(".svn")
          && (test.isDirectory() || name.endsWith("Test.java"));
    }
  };


  public static Test suite() throws IOException, ClassNotFoundException {
    TestSuite suite = new TestSuite("Caja Tests");
    // AllTests should be run in the project root directory
    File testRoot = new File("tests");
    int nTests = findAllTests(suite, testRoot);
    if (nTests == 0) { throw new AssertionFailedError("Found no tests"); }
    return suite;
  }

  private static Class<? extends TestCase> mapToTestClass(String className)
      throws ClassNotFoundException {
    return Class.forName(className).asSubclass(TestCase.class);
  }

  private static int findAllTests(TestSuite ts, File... roots)
      throws IOException, ClassNotFoundException {
    int count = 0;
    for (File root : roots) {
      for (File child : root.listFiles(testClassFilter)) {
        count += findAllTests(ts, child, null);
      }
    }
    return count;
  }

  private static int findAllTests(TestSuite ts, File root, String classpath)
      throws IOException, ClassNotFoundException {
    if (root.isDirectory()) {
      int count = 0;
      for (File child : root.listFiles(testClassFilter)) {
        count += findAllTests(
            ts, child,
            (classpath != null ? classpath + "." : "") + root.getName());
      }
      return count;
    }
    String className = classpath + "."
        + root.getName().replaceFirst("[.]java$", "");

    Class<? extends TestCase> testCase = mapToTestClass(className);
    Pattern testFilter = Pattern.compile(
        "(?:" + globToPattern(System.getProperty("test.filter", "*")) + ")$",
        Pattern.DOTALL);

    if (testCase != null && testFilter.matcher(testCase.getName()).find()) {
      ts.addTestSuite(testCase);
    }
    return 1;
  }

  private static String globToPattern(String glob) {
    StringBuilder sb = new StringBuilder();
    int pos = 0;
    for (int i = 0, n = glob.length(); i < n; ++i) {
      char ch = glob.charAt(i);
      if (ch == '*') {
        sb.append(Pattern.quote(glob.substring(pos, i)));
        pos = i + 1;
        // ** matches across package boundaries
        if (pos < n && '*' == glob.charAt(pos)) {
          ++pos;
          sb.append(".*");
        } else {
          sb.append("[^.]*");
        }
      } else if (ch == '?') {
        sb.append(Pattern.quote(glob.substring(pos, i))).append('.');
        pos = i + 1;
      }
    }
    sb.append(Pattern.quote(glob.substring(pos)));
    return sb.toString();
  }
}
