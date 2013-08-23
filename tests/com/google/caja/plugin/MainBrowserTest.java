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

import org.junit.runner.RunWith;

/**
 * Browser-driving tests for Caja itself.
 *
 * @author kpreid@switchb.org
 */
@RunWith(CatalogRunner.class)
@CatalogRunner.CatalogName("browser-tests.json")
public class MainBrowserTest extends CatalogTestCase {
  /**
   * Special case kludge for scan test which takes a long time under ES5/3.
   *
   * TODO(kpreid): Either extend the catalog with timeout data or add a way
   * for the scanner to communicate it's making progress (which must be count-up
   * rather than count-down).
   */
  @Override
  protected int waitForCompletionTimeout() {
    if (entry.getLabel().startsWith("guest-scan-es53-")) {
      return 300000;    // msec
    } else if (entry.getLabel().startsWith("guest-scan-es5-")) {
      return 60000;     // msec
    } else {
      return super.waitForCompletionTimeout();
    }
  }
}
