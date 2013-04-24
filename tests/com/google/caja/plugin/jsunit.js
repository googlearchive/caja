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
jsunit.asyncPoll = 50;
jsunit.asyncWait = 1000;
jsunit.alreadyRan = false;

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

/**
 * Register a test that can be run later.
 *
 * Note: opt_idClass is not usually explicitly written; if needed it is inserted
 * by the taming wrapper in browser-test-case.js.
 */
function jsunitRegister(testName, test, opt_idClass) {
  if (testName in jsunit.tests) { throw new Error('dupe test ' + testName); }
  jsunit.tests[testName] = test;
  obtainResultDiv(testName, opt_idClass);
}

function arrayContains(anArray, anElement) {
  for (var i = 0; i < anArray.length; i++) {
    if (anElement === anArray[i]) { return true; }
  }
  return false;
}

function isGroupLogMessages() {
  return (typeof console !== 'undefined'
      && 'group' in console);  // Not on Safari.
}

function startLogMessagesGroup(testName, opt_subTestName) {
  if (isGroupLogMessages()) {
    if (opt_subTestName) {
      console.group('running ' + testName + ' - ' + opt_subTestName)
    } else {
      console.group('running ' + testName);
    }
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

/** Run tests. */
function jsunitRun(opt_testNames, opt_idClass, opt_asyncEval) {
  if (jsunit.alreadyRan) { return; }
  jsunit.alreadyRan = true;

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

  jsunit.testCount = testNames.length;
  if (jsunit.testCount === 0) {
    document.title = 'No tests?';
    throw 'Error: no tests?';
  }

  testNames.sort();
  // jsunitRunNext pops off testNames
  testNames.reverse();
  jsunitRunNext(testNames, opt_idClass, opt_asyncEval);
}

function jsunitRunNext(testNames, opt_idClass, opt_asyncEval) {
  if (testNames.length === 0) {
    if (opt_asyncEval) {
      opt_asyncEval();
    }
    return;
  }

  var testName = testNames.pop();
  startLogMessagesGroup(testName);

  var progress = jsunit.passCount + jsunit.failCount;
  try {
    (typeof setUp === 'function') && setUp();
    jsunit.tests[testName].call();
    (typeof tearDown === 'function') && tearDown();
  } catch (e) {
    jsunit.failCount++;
    jsunit.updateStatus();
    jsunitFinished(testName, 'failed', opt_idClass);
    if (typeof console !== 'undefined') {
      console.error('FAIL: ' + testName + ': ' + e);
      console.error(e);
    }
    throw e;
  } finally {
    endLogMessagesGroup(testName);
    jsunitWait(progress, new Date().getTime(), testNames, opt_idClass,
               opt_asyncEval);
  }
}

// Most of our async tests are basically waiting for a caja.load
// to finish. Launching them in parallel doesn't really finish
// much faster, and has the bad effect of making it look like the
// browser is stalled for a long time (the browser processing
// queue ends up setting up multiple guest frames before any
// guest code gets run).  To avoid that, we wait up to
// jsunit.asyncWait msec to see if a test passed or failed,
// before starting the next test.

function jsunitWait(progress, start, testNames, opt_idClass, opt_asyncEval) {
  // have we made progress yet?
  if (jsunit.passCount + jsunit.failCount === progress) {
    var now = new Date().getTime();
    // should we keep waiting?
    if (now < start + jsunit.asyncWait) {
      setTimeout(function() {
        jsunitWait(progress, start, testNames, opt_idClass, opt_asyncEval);
      }, jsunit.asyncPoll);
      return;
    }
  }
  // we made progress, or we timed out, so start the next test
  setTimeout(function() {
    jsunitRunNext(testNames, opt_idClass, opt_asyncEval);
  }, 1);
  // setTimeout 1 because RhinoExecutor doesn't support 0
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
      if (typeof console !== 'undefined') {
        console.error(e);
      }
    } finally {
      endLogMessagesGroup(id, callbackName);
    }
    return result;
  }
  return typeof ___ !== 'undefined' && ___.markFuncFreeze || opt_frame
           ? (opt_frame
               ? opt_frame.markFunction(callback)
               : ___.markFuncFreeze(callback))
           : callback;
}

function jsunitFinished(id, result, opt_idClass) {
  var node = obtainResultDiv(id, opt_idClass);
  node.appendChild(document.createTextNode(' \u2014 ' + result + ' ' + id));
  var cl = node.className || '';
  // TODO(kpreid): Confirm 'waiting' is obsolete and remove this;
  cl = cl.replace(/\b(clickme|waiting)\b\s*/g, '');
  node.className = cl + ' done ' + result;
}

function jsunitPass(id, opt_idClass) {
  // Note jsunit.pass does validation of the test id, so should occur first
  jsunit.pass(id);
  jsunitFinished(id, 'passed', opt_idClass);
}

function jsunitFail(id) {
  jsunitFinished(id, 'failed');
  fail(id);
}

function obtainResultDiv(id, opt_idClass) {
  var el;
  if (opt_idClass) {
    el = document.getElementById(id + '-' + opt_idClass);
  }
  if (!el) {
    el = document.getElementById(id);
    if (!el) {
      el = document.createElement('div');
      el.setAttribute('id', id);
      el.setAttribute('class', 'testcontainer');
      el.textContent = id;

      var parent = document.body || document.documentElement;
      parent.appendChild(el);
    }
  }
  return el;
}

/** Aim high and you might miss the moon! */
function expectFailure(shouldFail, opt_msg, opt_failFilter) {
  if (typeof shouldFail !== 'function') {
    // Avoid falsely failing expectedly by accidentally passing a non-function
    // and no filter.
    throw new TypeError('Non-function given as shouldFail to expectFailure: ' +
        shouldFail);
  }
  try {
    shouldFail();
  } catch (e) {
    if (opt_failFilter && !opt_failFilter(e)) {
      console.log(opt_msg + ': Caught unexpected failure ' + e + ' (' +
          e.message + ')');
      throw e;
    } else {
      console.log(opt_msg + ': Caught expected failure ' + e + ' (' + e.message
          + ')');
      return;
    }
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
    assertContains(msg, e.message);
    return;
  }
  fail('Did not throw: ' + msg);
}
