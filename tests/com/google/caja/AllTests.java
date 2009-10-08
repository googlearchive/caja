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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * @author mikesamuel@gmail.com
 */
public class AllTests {
  private static Pattern testFilter = 
    Pattern.compile(System.getProperty("test.filter", ""));

  private static FilenameFilter testClassFilter = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      File test = new File(dir, name);
      return !name.startsWith(".svn") && 
        (test.isDirectory() || name.endsWith("Test.java"));
    }
  };


  @SuppressWarnings("unchecked")
  public static Test suite() throws IOException, ClassNotFoundException {
    TestSuite suite = new TestSuite("Caja Tests");
    // AllTests should be run in the project root directory
    File testRoot = new File("tests");
    findAllTests(suite, testRoot);
    return suite;
  }

  @SuppressWarnings("unchecked")
  private static Class<? extends TestCase> mapToTestClass(String className) 
    throws ClassNotFoundException {
    return (Class<? extends TestCase>) Class.forName(className);
  }

  private static void findAllTests(TestSuite ts, File... roots) 
    throws IOException, ClassNotFoundException {
    for (File root : roots) {
      File[] childs = root.listFiles(testClassFilter);
      for(File child : childs) {
        findAllTests(ts, child, null);
      }
    }
  }
  
  private static void findAllTests(TestSuite ts, File root, String classpath)
    throws IOException, ClassNotFoundException {
    if (root.isDirectory()) {
      final File[] childs = root.listFiles(testClassFilter);
      for(File child : childs) {
        findAllTests(ts, child, classpath == null ? root.getName() :
                                classpath + "." + root.getName());
      }
      return;
    }
    String className = classpath + "." +
      root.getName().replaceFirst("[.]java$", "");
    
    Class<? extends TestCase> testCase = mapToTestClass(className);
    if (testCase != null && testFilter.matcher(testCase.getName()).find()) {
      ts.addTestSuite(testCase);
    }
  }
}
