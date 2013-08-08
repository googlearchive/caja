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

package com.google.caja.util;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

/**
 * See <a href="https://code.google.com/p/google-caja/wiki/CajaTesting"
 * >CajaTesting</a>
 */

public enum TestFlag {
  ANT_FLAGS(
      "test.ant.flags"),
  ANT_TARGETS(
      "test.ant.targets"),
  BROWSER(
      "test.browser"),
  BROWSER_CLOSE(
      "test.browser.close"),
  CAPTURE_PASSES(
      "test.capture.passes"),
  CAPTURE_TO(
      "test.capture.to"),
  CHROME_ARGS(
      "test.chrome.args"),
  CHROME_BINARY(
      "test.chrome.binary"),
  DEBUG(
      "test.debug"),
  DEBUG_BROWSER(
      "test.debug.browser"),
  DEBUG_SERVER(
      "test.debug.server"),
  EXCLUDE(
      "test.exclude"),
  FAILURE_NOT_AN_OPTION(
      "test.failureNotAnOption"),
  FILTER(
      "test.filter"),
  FILTER_METHOD(
      "test.filter.method"),
  SERVER_HOSTNAME(
      "test.server.hostname"),
  SERVER_PORT(
      "test.server.port"),
  THREADS(
      "test.threads"),
  TYPE(
      "test.type"),
  WEBDRIVER_COMMAND(
      "test.webdriver.command"),
  WEBDRIVER_HOST(
      "test.webdriver.host"),
  WEBDRIVER_URL(
      "test.webdriver.url"),
  WEBDRIVER_USER(
      "test.webdriver.user");

  private static class Names {
    private static final Set<String> set = new HashSet<String>();
  }

  private String name;

  TestFlag(String name) {
    this.name = name;
    Names.set.add(name);
  }

  public String getName() {
    return name;
  }

  public static Set<String> all() {
    return Names.set;
  }

  public String getString(String dflt) {
    return System.getProperty(name, dflt);
  }

  public int getInt(int dflt) {
    String value = System.getProperty(name);
    if (value == null || "".equals(value)) {
      return dflt;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw error("Invalid value " + value + " for " + name);
    }
  }

  public boolean truthy() {
    String value = System.getProperty(name);
    if (value == null) { return false; }
    return "1".equals(value) || "true".equalsIgnoreCase(value)
        || "y".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
  }

  private static RuntimeException error(String message) {
    // System.err is captured by ant-junit and goes to the test logs
    // FileDescriptor.err is captured by ant and goes to stdout.
    @SuppressWarnings("resource")
    PrintStream err = new PrintStream(
        new FileOutputStream(FileDescriptor.err), true);
    err.println(message);
    return new RuntimeException(message);
  }
}
