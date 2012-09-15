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
    throw new Error('TEST ERROR: pass without a test ID');
  }
  var id = opt_id || jsunit.getCurrentTestId();
  if (!(id in jsunit.tests)) {
    throw new Error(
        'TEST ERROR: pass(' + id + ') with no such registered test');
  }
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
  if (!jsunit.failCount && jsunit.passCount === jsunit.testCount) {
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
  } else if (e.description) {
    console.error(e.description + '\n' + e.stackTrace);
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

function jsunitFilter(filter) {
  jsunitFilter.filter = filter;
}

var jsunitTestsRun = false;

/** Run tests. */
function jsunitRun(opt_testNames) {
  if (jsunitTestsRun) { return; }
  jsunitTestsRun = true;

  document.title += ' (' + (navigator.appName
                            ? navigator.appName + ' ' + navigator.appVersion
                            : navigator.userAgent) + ')';
  jsunit.originalTitle = document.title;

  var filter = jsunitFilter.filter || /.?/;
  var testNames = [];
  for (var k in jsunit.tests) {
    if (jsunit.tests.hasOwnProperty(k)) {
      if (!opt_testNames || arrayContains(arguments, k)) {
        if (filter.test(k)) {
          testNames.push(k);
        }
      }
    }
  }
  testNames.sort();

  jsunit.testCount = testNames.length;

  if (jsunit.testCount === 0) {
    document.title = 'No tests?';
    throw 'Error: no tests?';
  }

  var firstFailure = null;
  jsunit.failCount = 0;
  for (var i = 0; i < testNames.length; ++i) {
    var testName = testNames[i];
    startLogMessagesGroup(testName);
    try {
      (typeof setUp === 'function') && setUp();
      jsunit.tests[testName].call();
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
function jsunitCallback(aFunction, opt_id, opt_frame) {
  if (!aFunction || typeof aFunction !== 'function') {
    throw new Error('TEST ERROR: jsunitCallback without a valid function');
  }
  if (!jsunit.getCurrentTestId() && !opt_id) {
    throw new Error('TEST ERROR: jsunitCallback without a test ID');
  }
  var id = opt_id || jsunit.getCurrentTestId();
  var callbackName = aFunction.name || '<anonymous>';
  function callback(opt_args) {
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
  }
  return typeof ___ !== 'undefined'
           ? (opt_frame
               ? opt_frame.markFunction(callback)
               : ___.markFuncFreeze(callback))
           : callback;
}

function jsunitPass(id) {
  jsunit.pass(id);
  var node = document.getElementById(id) || makeResultDiv(id);
  node.appendChild(document.createTextNode('Passed ' + id));
  var cl = node.className || '';
  cl = cl.replace(/\b(clickme|waiting)\b\s*/g, '');
  cl += ' passed';
  node.className = cl;
}

function jsunitFail(id) {
  var node = document.getElementById(id) || makeResultDiv(id);
  node.appendChild(document.createTextNode('Failed ' + id));
  var cl = node.className || '';
  cl = cl.replace(/\b(clickme|waiting)\b\s*/g, '');
  cl += ' failed';
  node.className = cl;
  fail(id);
}

function makeResultDiv(id) {
  var el = document.createElement('div');
  el.id = id;
  el.className = 'testcontainer';
  var parent = document.body || document.documentElement;
  parent.insertBefore(el, parent.firstChild);
  return el;
}

/** Aim high and you might miss the moon! */
function expectFailure(shouldFail, opt_msg, opt_failFilter) {
  try {
    shouldFail();
  } catch (e) {
    if (opt_failFilter && !opt_failFilter(e)) { throw e; }
    console.log(
        'Caught expected failure ' + e + ' (' + e.message + ')');
    return;
  }
  fail(opt_msg || 'Expected failure');
}

function assertFailsSafe(canFail, assertionsIfPasses) {
  try {
    canFail();
  } catch (e) {
    return;
  }
  assertionsIfPasses();
}

function assertThrowsMsg(shouldThrow, msg) {
  try {
    shouldThrow();
  } catch (e) {
    assertTrue(e.message.indexOf(msg) > -1);
  }
}
