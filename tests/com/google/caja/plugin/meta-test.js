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

/**
 * @fileoverview Test our test framework.
 *
 * @author kpreid@switchb.org
 * @requires jsunitRegister, jsunitRun, readyToTest, inES5Mode
 */

(function() {
  var s = document.createElement('style');
  s.textContent = 'iframe {' +
    'opacity: 0.5;' +
    'display: block;' +
    'box-sizing: border-box;' +
    'margin: 0;' +
    'border: .3em solid white;' +
    'padding: 0;' +
    'width: 100%;' +
  '}';
  document.getElementsByTagName('head')[0].appendChild(s);
}());

function makeMetaTestFrame(driverOrCase, url) {
  var baseURL = getUrlParam('test-driver');
  var frameURL = 'browser-test-case.html?es5=' + inES5Mode + '&test-' +
      driverOrCase + '=' + URI.utils.resolve(baseURL, url)
  var container =
      document.getElementById(jsunit.getCurrentTestId()) || document.body;

  container.appendChild(document.createTextNode(' '));

  var link = document.createElement('a');
  link.textContent = 'unframed';
  link.href = frameURL;
  container.appendChild(link);

  var frame = document.createElement('iframe');
  frame.src = frameURL;
  container.appendChild(frame);

  var w = frame.contentWindow;
  return w;
}

function pollStatus(w, callback) {
  var id = jsunit.getCurrentTestId();
  if (!id) { throw new Error('pollTitle: must be in a test'); }
  callback = jsunitCallback(callback);

  var title = undefined;
  var i = setInterval(function() {
    if (w.document) {
      var newTitle = w.document.title;
      if (newTitle !== title) {
        title = newTitle;
        var i = interpretTitle(title);
        callback(i.state, i.pass, i.fail, i.total);
        if (jsunit.isTestComplete(id)) {
          clearInterval(i);
        }
      }
    }
  }, 10);
}

function interpretTitle(title) {
  if (title === '' || /^Browser test case/.test(title)) {
    return {
      state: 'startup',
      pass: 0,
      fail: 0,
      total: 0
    };
  }
  
  // Past startup: check formatting
  var match = /^(all tests passed: |)(\d+)\/(\d+) fail (\d+)\/\3 pass/
      .exec(title);
  if (!match) {
    throw new Error('Unexpected test title: ' + title);
  }
  var fail, total, pass;
  var result = {
    fail: fail = parseInt(match[2]),
    total: total = parseInt(match[3]),
    pass: pass = parseInt(match[4])
  };
  if (result.pass + result.fail > result.total) {
    throw new Error('Too many passes or fails: ' + title);
  }

  // Check specific cases, determine state
  if (match[1] === 'all tests passed: ') {
    assertEquals('pass === total', total, pass);
    result.state = 'pass';
  } else {
    if (pass === total) {
      throw new Error('Title failed to pass: ' + title);
    } else if (fail > 0 && fail + pass === total) {
      result.state = 'fail';
    } else {
      result.state = 'running';
    }
  }

  return result;
}

// Test various failing cases: no tests should pass.
jsunitRegister('testCaseFail', function testCaseFail() {
  var w = makeMetaTestFrame('case', 'meta-test-fails.html');
  pollStatus(w, function(state, pass, fail, total) {
    assertNotEquals('pass', state);
    if (state === 'fail') {
      assertEquals('no tests passed', 0, pass);
      assertEquals('expected number of tests failed', 3, fail);
      jsunitPass('testCaseFail');
    }
  });
});

// Test that an asynchronous requirement can succeed.
jsunitRegister('testCaseAsyncPass', function testCaseAsyncPass() {
  var w = makeMetaTestFrame('case', 'meta-test-async-passes.js');
  pollStatus(w, function(state, pass, fail, total) {
    assertNotEquals('fail', state);
    if (state === 'pass') {
      jsunitPass('testCaseAsyncPass');
    }
  });
});

// Test that an asynchronous requirement causes failure even if the rest of the
// suite passes promptly.
jsunitRegister('testCaseAsyncFail', function testCaseAsyncFail() {
  var w = makeMetaTestFrame('case', 'meta-test-async-fails.js');
  pollStatus(w, function(state, pass, fail, total) {
    assertNotEquals('pass', state);
    if (state === 'fail') {
      jsunitPass('testCaseAsyncFail');
    }
  });
});

// Test that failures not corresponding to any test are counted as reasons to
// fail. This test is independent of testCaseFail because it requires an
// explicitly counted number of failures.
jsunitRegister('testPanic', function testPanic() {
  // TODO(kpreid): is a .html file because top-level .js handling doesn't
  // continue after errors.
  var w = makeMetaTestFrame('case', 'meta-test-panic.html');
  pollStatus(w, function(state, pass, fail, total) {
    assertNotEquals('pass', state);
    if (state === 'fail') {
      assertEquals('expected passes', 1, pass);
      assertEquals('expected panics', 2, fail);
      jsunitPass('testPanic');
    }
  });
});

jsunitRegister('testNoTests', function testNoTests() {
  var w = makeMetaTestFrame('case', 'meta-test-no-tests.js');
  pollStatus(w, function(state, pass, fail, total) {
    assertNotEquals('pass', state);
    if (state === 'fail') {
      assertEquals('pass', 0, pass);
      assertEquals('fail', 1, fail);
      jsunitPass('testNoTests');
    }
  });
});

jsunitRegister('testOnlySkippedTests', function testOnlySkippedTests() {
  var w = makeMetaTestFrame('case', 'meta-test-skipped-test.js');
  pollStatus(w, function(state, pass, fail, total) {
    assertNotEquals('fail', state);
    if (state === 'pass') {
      assertEquals('pass', 0, pass);
      assertEquals('fail', 0, fail);
      jsunitPass('testOnlySkippedTests');
    }
  });
});

readyToTest();
jsunitRun();
