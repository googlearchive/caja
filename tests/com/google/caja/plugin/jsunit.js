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

var jsunitTests = {};

/** Register a test that can be run later. */
function jsunitRegister(testName, test) {
  if (testName in jsunitTests) { throw new Error('dupe test ' + testName); }
  jsunitTests[testName] = test;
}

/** Run tests. */
function jsunitRun() {
  document.title += ' (' + (navigator.appName
                            ? navigator.appName + ' ' + navigator.appVersion
                            : navigator.userAgent) + ')';
  var originalTitle = document.title;

  var testNames = [];
  for (var k in jsunitTests) {
    if (jsunitTests.hasOwnProperty(k)) { testNames.push(k); }
  }
  testNames.sort();

  var queryParams = (function () {
    var queryParams = {};
    var parts = location.search.match(/[^&?]+/g) || [];
    for (var i = 0, n = parts.length; i < n; ++i) {
      var part = parts[i];
      var eq = part.indexOf('=');
      var key = decodeURIComponent(eq < 0 ? part : part.substring(0, eq));
      var value = decodeURIComponent(eq >= 0 ? part.substring(eq + 1) : '');
      var values = queryParams.hasOwnProperty(key)
          ? queryParams[key]
          : (queryParams[key] = []);
      values.push(value);
    }
    return queryParams;
  })();

  // If loaded with ?test.filter=Foo, should run testFoo and testFooBar, but not
  // testBar.
  var testFilter = null;
  if (queryParams['test.filter']) {
    testFilter = new RegExp(queryParams['test.filter'][0]);
  }

  var firstFailure = null;
  var nFailures = 0;
  for (var i = 0; i < testNames.length; ++i) {
    var testName = testNames[i];
    if (testFilter && !testFilter.test(testName)) { continue; }
    var groupLogMessages = (typeof console !== 'undefined'
                            && 'group' in console);  // Not on Safari.
    if (groupLogMessages) {
      console.group('running %s', testName);
      console.time(testName);
    }
    try {
      (typeof setUp === 'function') && setUp();
      jsunitTests[testName].call(this);
      (typeof tearDown === 'function') && tearDown();
    } catch (e) {
      firstFailure = firstFailure || e;
      nFailures++;
      if (typeof console !== 'undefined') {
        if (e.isJsUnitException) {
          console.error(
              e.comment + '\n' + e.jsUnitMessage + '\n' + e.stackTrace);
        } else {
          console.error((e.message || '' + e) + '\n' + e.stack);
        }
      }
    } finally {
      if (groupLogMessages) {
        console.timeEnd(testName);
        console.groupEnd();
      }
    }
  }

  if (testNames.length && !nFailures) {
    document.title = originalTitle + ' - all tests passed';
  } else {
    var msg = nFailures + '/' + testNames.length + ' failed';
    document.title = (originalTitle + ' - ' + msg);
    throw firstFailure || new Error(msg);
  }
  (typeof console !== 'undefined' && 'group' in console)
      && (console.group(document.title), console.groupEnd());
}
