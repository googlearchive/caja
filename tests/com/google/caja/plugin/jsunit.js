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
 *
 * TODO(kpreid): Clean up stuff not intended to be exported.
 * @requires navigator, setTimeout, ___, console,
 *     assertContains, fail
 * @provides jsunit, jsunitRegisterIf, jsunitRegister,
 *     jsunitRegisterAuxiliaryStatus, jsunitRun, jsunitPass, jsunitFail,
 *     jsunitFinished, jsunitFilter, jsunitCallback
 *     isGroupLogMessages, startLogMessagesGroup, endLogMessagesGroup,
 *     inDocument, obtainResultDiv, jsunitWait, jsunitRunNext, arrayContains,
 *     jsunitValidStatuses
 *     expectFailure, assertFailsSafe, assertThrowsMsg
 * @overrides _junit_, setUp, tearDown, document
 */

var jsunit = {};
jsunit.tests = {};
jsunit.testCount = 0;
jsunit.passCount = 0;
jsunit.failCount = 0;
jsunit.testIdStack = [];
jsunit.baseTitle = undefined;
jsunit.asyncPoll = 50;
jsunit.asyncWait = 1000;
jsunit.alreadyRan = false;
// TODO(kpreid): Kludge. The JUnit tests which run JavaScript want it to
// complete synchronously, so this flag disables all setTimeouts in this
// module.
jsunit.beSynchronous = typeof _junit_ !== 'undefined';

jsunit.getCurrentTestId = function() {
  return jsunit.testIdStack.length
      ? jsunit.testIdStack[jsunit.testIdStack.length - 1]
      : undefined;
};

jsunit.isTestComplete = function(id) {
  return jsunit.tests[id].status !== 'new';
};

jsunit.pushTestId = function(id) {
  if (!jsunit.tests[id]) {
    jsunit.panic('push unregistered test ID \"' + id + '\"', true);
  }
  jsunit.testIdStack.push(id);
};

jsunit.popTestId = function() {
  if (!jsunit.getCurrentTestId()) {
    jsunit.panic('pop empty test ID stack', true);
  }
  jsunit.testIdStack.pop();
};

// at the moment, every test should explicitly call jsunit.pass(),
// because some tests don't pass until event handlers fire.
// TODO: create jsunitRegisterAsync(), then make passes implicit.
jsunit.pass = function(opt_id) {
  if (!jsunit.getCurrentTestId() && !opt_id) {
    jsunit.panic('pass without a test ID', true);
  }
  var id = opt_id || jsunit.getCurrentTestId();
  var testRecord = jsunit.tests[id];
  if (!testRecord) {
    jsunit.panic('pass(' + id + ') with no such registered test', true);
  }
  if (testRecord.status !== 'new') {
    jsunit.panic('pass of test already ' + testRecord.status + ': ' + id, true);
  }
  jsunitFinished(id, 'passed');
  if (typeof console !== 'undefined') {
    console.log('PASS: ' + id);
  }
};

jsunit.fail = function(id, error) {
  var testRecord = jsunit.tests[id];
  switch (testRecord.status) {
    case 'failed':
      // duplicate failures (due to callbacks) are not surprising
      return;
    case 'passed':
      // TODO(kpreid): Make this a hard failure even if this pass-then-fail test
      // happens to be the last test in the page.
      break;
    case 'new':
    default:
      break;
  }
  jsunitFinished(id, 'failed');
  if (typeof console !== 'undefined') {
    console.error('FAIL: ' + id + ': ' + error + '\n', error);
  }
};

/**
 * Ensure that an unexpected problem causes test suite failure, as opposed to
 * throwing an exception (which might just be uncaught) or causing a specific
 * registered test to fail.
 */
jsunit.panic = (function() {
  var panicCount = 0;
  return function panic(msg, alsoThrow) {
    var testName = 'PANIC #' + ++panicCount;
    jsunitRegisterAuxiliaryStatus(testName);
    jsunit.tests[testName].resultContainer.appendChild(
        document.createTextNode(': ' + msg));
    jsunit.fail(testName, msg);
    if (alsoThrow) {
      throw new Error('PANIC: ' + msg);
    }
  };
})();

jsunit.updateStatus = function() {
  if (jsunit.baseTitle === undefined) {
    jsunit.baseTitle = document.title + ' (' +
        (navigator.appName
         ? navigator.appName + ' ' + navigator.appVersion
         : navigator.userAgent) + ')';
  }

  var status = '';
  if (!jsunit.failCount && jsunit.passCount === jsunit.testCount) {
    status += 'all tests passed:';
  }
  status += ' ' + jsunit.failCount + '/' + jsunit.testCount + ' fail';
  status += ' ' + jsunit.passCount + '/' + jsunit.testCount + ' pass';
  document.title = status + ' - ' + jsunit.baseTitle;
};

/**
 * Register a test function to be run by jsunitRun.
 *
 * Note: opt_idClass is not usually explicitly written; if needed it is inserted
 * by the taming wrapper in browser-test-case.js.
 */
function jsunitRegisterIf(shouldRun, testName, test, opt_idClass) {
  if (testName in jsunit.tests) {
    jsunit.panic('dupe test ' + testName, true);
  }
  if (jsunit.alreadyRan) {
    jsunit.panic('Too late to register a new test', true);
  }
  jsunit.tests[testName] = {
    status: 'new',
    test: test,
    resultContainer: obtainResultDiv(testName, opt_idClass)
  };
  if (shouldRun) {
    jsunit.testCount++;
    jsunit.updateStatus();
  } else {
    // temporarily registering skipped tests is a kludge to let jsunitFinished
    // interact with the resultContainer
    jsunitFinished(testName, 'skipped');
    delete jsunit.tests[testName];
  }
}

/**
 * Register a test to be run by jsunitRun. See jsunitRegisterIf for details.
 */
function jsunitRegister(testName, test, opt_idClass) {
  jsunitRegisterIf(true, testName, test, opt_idClass);
}

/**
 * Register a thing which can pass/fail, but does not need to be executed
 * separately (probably because it was started by a registered test).
 * For 'internal' use.
 */
function jsunitRegisterAuxiliaryStatus(testName) {
  // TODO(kpreid): This is a kludge invented for assertAsynchronousRequirement
  // (and also used for panics). It would be better to make async requirements
  // subparts of one test.
  if (testName in jsunit.tests) {
    jsunit.panic('dupe test ' + testName, true);
  }
  jsunit.tests[testName] = {
    status: 'new',
    test: function() {},
    resultContainer: obtainResultDiv(testName, undefined)
  };
  jsunit.testCount++;
  jsunit.updateStatus();
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
      console.group('running ' + testName + ' - ' + opt_subTestName);
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
function jsunitRun(opt_testNames, opt_asyncEval) {
  if (jsunit.alreadyRan) { return; }
  jsunit.alreadyRan = true;

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

  if (jsunit.testCount === 0) {
    jsunit.panic('No tests registered!', true);
  }

  testNames.sort();
  // jsunitRunNext pops off testNames
  testNames.reverse();
  jsunitRunNext(testNames, opt_asyncEval);
}

function jsunitRunNext(testNames, opt_asyncEval) {
  if (testNames.length === 0) {
    if (opt_asyncEval) {
      opt_asyncEval();
    }
    return;
  }

  var testName = testNames.pop();
  var testRecord = jsunit.tests[testName];
  startLogMessagesGroup(testName);

  var progress = jsunit.passCount + jsunit.failCount;
  try {
    (typeof setUp === 'function') && setUp();
    testRecord.test.call();
    (typeof tearDown === 'function') && tearDown();
  } catch (e) {
    jsunit.fail(testName, e);
    throw e;
  } finally {
    endLogMessagesGroup(testName);
    if (jsunit.beSynchronous) {
      jsunitRunNext(testNames, opt_asyncEval);
    } else {
      jsunitWait(progress, new Date().getTime(), testNames, opt_asyncEval);
    }
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

function jsunitWait(progress, start, testNames, opt_asyncEval) {
  // have we made progress yet?
  if (jsunit.passCount + jsunit.failCount === progress) {
    var now = new Date().getTime();
    // should we keep waiting?
    if (now < start + jsunit.asyncWait) {
      setTimeout(function() {
        jsunitWait(progress, start, testNames, opt_asyncEval);
      }, jsunit.asyncPoll);
      return;
    }
  }
  // we made progress, or we timed out, so start the next test
  setTimeout(function() {
    jsunitRunNext(testNames, opt_asyncEval);
  }, 1);
  // setTimeout 1 because RhinoExecutor doesn't support 0
}

/** Register a callback within a running test. */
function jsunitCallback(aFunction, opt_id, opt_frame) {
  if (!aFunction || typeof aFunction !== 'function') {
    jsunit.panic('jsunitCallback without a valid function', true);
  }
  if (!jsunit.getCurrentTestId() && !opt_id) {
    jsunit.panic('jsunitCallback without a test ID', true);
  }
  var id = opt_id || jsunit.getCurrentTestId();
  var callbackName = aFunction.name || '<anonymous>';
  function callback(opt_args) {
    var result = undefined;
    startLogMessagesGroup(id, callbackName);
    try {
      result = aFunction.apply(undefined, arguments);
    } catch (e) {
      jsunit.fail(id, e);
    } finally {
      endLogMessagesGroup(id, callbackName);
    }
    return result;
  }
  return typeof ___ !== 'undefined' && ___.markConstFunc || opt_frame
           ? (opt_frame
               ? opt_frame.markFunction(callback)
               : ___.markConstFunc(callback))
           : callback;
}

var jsunitValidStatuses = ['passed', 'failed', 'skipped'];

function jsunitFinished(id, status) {
  var testRecord = jsunit.tests[id];
  if (jsunitValidStatuses.indexOf(status) === -1) {
    throw new Error('bad jsunitFinished status: ' + status);
  }
  var node = testRecord.resultContainer;
  if (!inDocument(node)) {
    if (typeof console !== 'undefined') {
      console.error('test ' + id + ' lost its result container');
    }
    status = 'failed';
    node = testRecord.resultContainer = obtainResultDiv(id);
  }

  // Update internal state
  var oldStatus = testRecord.status;
  testRecord.status = status;

  // Update DOM state
  node.appendChild(document.createTextNode(' \u2014 ' + status + ' ' + id));
  var cl = node.className || '';
  // TODO(kpreid): Confirm 'waiting' is obsolete and remove this;
  cl = cl.replace(/\b(clickme|waiting)\b\s*/g, '');
  node.className = cl + ' done ' + status;

  // Update summary
  jsunit.passCount += (status === 'passed') - (oldStatus === 'passed');
  jsunit.failCount += (status === 'failed') - (oldStatus === 'failed');
  jsunit.updateStatus();
}

// legacy alias
function jsunitPass(id) {
  jsunit.pass(id);
}

function jsunitFail(id) {
  jsunit.fail(id);
  fail(id);
}

function obtainResultDiv(id, opt_idClass) {
  var el = undefined;
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

function inDocument(node) {
  return node === document || (node !== null && inDocument(node.parentNode));
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
