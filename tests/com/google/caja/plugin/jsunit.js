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

var jsunit = {};
jsunit.tests = {};
jsunit.testCount = 0;
jsunit.passTests = {};
jsunit.passCount = 0;
jsunit.failCount = 0;
jsunit.testIdStack = [];
jsunit.originalTitle = '';

jsunit.getCurrentTestId = function() {
  return jsunit.testIdStack.length
      ? jsunit.testIdStack[jsunit.testIdStack.length - 1]
      : undefined;
};

jsunit.pushTestId = function(id) {
  if (!jsunit.tests[id]) {
    throw new Error('TEST ERROR: push unregistered test ID \"' + id + '\"');
  }
  jsunit.testIdStack.push(id);
};

jsunit.popTestId = function() {
  if (!jsunit.getCurrentTestId()) {
    throw new Error('TEST ERROR: pop empty test ID stack');
  }
  jsunit.testIdStack.pop();
};

// at the moment, every test should explicitly call jsunit.pass(),
// because some tests don't pass until event handlers fire.
// TODO: create jsunitRegisterAsync(), then make passes implicit.
jsunit.pass = function(opt_id) {
  if (!jsunit.getCurrentTestId() && !opt_id) {
    console.error('TEST ERROR: pass without a test ID');
    return;
  }
  var id = opt_id || jsunit.getCurrentTestId();
  if (id in jsunit.passTests) {
    throw new Error('dupe pass ' + id);
  }
  jsunit.passTests[id] = true;
  jsunit.passCount += 1;
  jsunit.updateStatus();
  if (typeof console !== 'undefined') {
    console.log('PASS: ' + id);
  }
};

jsunit.updateStatus = function() {
  var status = '';
  if (!jsunit.failCount && jsunit.passCount == jsunit.testCount) {
    status += 'all tests passed:';
  }
  status += ' ' + jsunit.failCount + '/' + jsunit.testCount + ' fail';
  status += ' ' + jsunit.passCount + '/' + jsunit.testCount + ' pass';
  document.title = status + ' - ' + jsunit.originalTitle;
};

/** Register a test that can be run later. */
function jsunitRegister(testName, test) {
  if (testName in jsunit.tests) { throw new Error('dupe test ' + testName); }
  jsunit.tests[testName] = test;
}

function arrayContains(anArray, anElement) {
  for (var i = 0; i < anArray.length; i++) {
    if (anElement === anArray[i]) { return true; }
  }
  return false;
}

function logToConsole(e) {
  if (e.isJsUnitException) {
    console.error(
        e.comment + '\n' + e.jsUnitMessage + '\n' + e.stackTrace);
  } else {
    console.error((e.message || '' + e) + '\n' + e.stack);
  }
}

function isGroupLogMessages() {
  return (typeof console !== 'undefined'
      && 'group' in console);  // Not on Safari.
}

function startLogMessagesGroup(testName, opt_subTestName) {
  if (isGroupLogMessages()) {
    opt_subTestName
        ? console.group('running %s - %s', testName, opt_subTestName)
        : console.group('running %s', testName);
    console.time(testName);
  }
  jsunit.pushTestId(testName);
}

function endLogMessagesGroup(testName, opt_subTestName) {
  if (isGroupLogMessages()) {
    console.timeEnd(testName);
    console.groupEnd();
  }
  jsunit.popTestId();
}

/** Run tests. */
function jsunitRun(opt_testNames) {
  document.title += ' (' + (navigator.appName
                            ? navigator.appName + ' ' + navigator.appVersion
                            : navigator.userAgent) + ')';
  jsunit.originalTitle = document.title;

  var testNames = [];
  for (var k in jsunit.tests) {
    if (jsunit.tests.hasOwnProperty(k)) {
      if (!opt_testNames || arrayContains(arguments, k)) {
        testNames.push(k);
      }
    }
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

  jsunit.testCount = 0;
  for (var i = 0; i < testNames.length; ++i) {
    if (!testFilter || testFilter.test(testName)) {
      jsunit.testCount++;
    }
  }

  var firstFailure = null;
  jsunit.failCount = 0;
  for (var i = 0; i < testNames.length; ++i) {
    var testName = testNames[i];
    if (testFilter && !testFilter.test(testName)) { continue; }
    startLogMessagesGroup(testName);
    try {
      (typeof setUp === 'function') && setUp();
      jsunit.tests[testName].call(this);
      (typeof tearDown === 'function') && tearDown();
    } catch (e) {
      firstFailure = firstFailure || e;
      jsunit.failCount++;
      jsunit.updateStatus();
      if (typeof console !== 'undefined') {
        console.log('FAIL: ' + testName);
        logToConsole(e);
      }
    } finally {
      endLogMessagesGroup(testName);
    }
  }

  jsunit.updateStatus();
  if (firstFailure) {
    throw firstFailure;
  }
  (typeof console !== 'undefined' && 'group' in console)
      && (console.group(document.title), console.groupEnd());
}

/** Register a callback within a running test. */
function jsunitCallback(aFunction, opt_id) {
  if (!jsunit.getCurrentTestId() && !opt_id) {
    throw new Error('TEST ERROR: jsunitCallback without a test ID');
  }
  var id = opt_id || jsunit.getCurrentTestId();
  var callbackName = aFunction.name || '<anonymous>';
  return ___.markFuncFreeze(function(opt_args) {
    var result = undefined;
    startLogMessagesGroup(id, callbackName);
    try {
      result = aFunction.apply(undefined, arguments);
    } catch (e) {
      logToConsole(e);
    } finally {
      endLogMessagesGroup(id, callbackName);
    }
    return result;
  });
}