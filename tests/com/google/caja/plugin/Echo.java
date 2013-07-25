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

package com.google.caja.plugin;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Convenience functions for diagnostics during tests.
 *
 * <p>Caja's ant junit config buries error messages in xml files
 * that are written when tests are done, and it's annoying to
 * start a test and find out 10 minutes later that they all failed because
 * of some browser testing configuration problem.
 *
 * <p>So, this is a method that tries to output directly to the
 * terminal/console (as well as to the usual places), so that when
 * you start browser tests, you can notice configuration problems early.
 */
public class Echo {
  public static void echo(String s) {
    // System.err is captured by junit and goes into ant-reports
    System.err.println(s);

    // FileDescriptor.err is captured by ant and goes to stdout.
    // We don't close err since that would close FileDescriptor.err
    @SuppressWarnings("resource")
    PrintStream err = new PrintStream(
        new FileOutputStream(FileDescriptor.err), true);
    err.println(s);
  }

  public static void echo(Exception e) {
    echo(e.toString());
  }

  public static void rethrow(Exception e) throws Exception {
    echo(e);
    throw e;
  }
}
