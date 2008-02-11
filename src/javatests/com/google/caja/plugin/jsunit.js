// Copyright (C) 2007 Google Inc.
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

/**
 * @fileoverview
 * Stubs out JSUnit by providing a function that finds all the testcases defined
 * in the global scope, and run them.
 *
 * @author mikesamuel@gmail.com
 */

/** Run tests. */
function jsunitRun() {
  var testNames = [];
  for (var k in this) {
    if (/^test/.test(k) && 'function' === typeof this[k]) {
      testNames.push(k);
    }
  }

  assertTrue(testNames.length > 0);

  testNames.sort();

  for (var i = 0; i < testNames.length; ++i) {
    var testName = testNames[i];
    this.console && (console.group('running %s', testName),
                     console.time(testName));
    try {
      this.setUp && this.setUp();
      this[testName]();
      this.tearDown && this.tearDown();
    } finally {
      this.console && (console.timeEnd(testName),
                       console.groupEnd());
    }
  }
}
