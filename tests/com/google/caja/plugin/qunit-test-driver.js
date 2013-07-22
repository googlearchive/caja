// Copyright (C) 2010 Google Inc.
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

function numberByClass(container, className) {
  var el = container.getElementsByClassName(className)[0];
  return el ? parseInt(el.textContent) : null;
}

var testCase = getUrlParam('test-case');

if (!testCase) {
  throw new Error('Parameter "test-case" not specified in URL');
}

if (getUrlParam('minified') === 'false') {
  // Enable un-minified jQuery source files for easier debugging
  testCase = testCase + '&dev=true';
}

caja.makeFrameGroup(basicCajaConfig, function(frameGroup) {
  frameGroup.makeES5Frame(
      createDiv(),
      caja.policy.net.ALL,
      function(frame) {
        frame.url(testCase).run(null, runCallback);
      });
});

function runCallback(result) {
  var i = setInterval(function() {
    if (document.getElementById('qunit-testresult-caja-guest-0___')) {
      readyToTest();
      jsunitRun();
    }
  }, 100);
}

jsunitRegister('qunitChecker', function qunitChecker() {
  var passCount = getUrlParam('expected-pass');
  if (passCount === 'all') {
    // done
  } else if (passCount[0] === '{') {
    passCount = JSON.parse(passCount);
    if (/Firefox/.test(navigator.userAgent)) {
      passCount = passCount['firefox'] || 'all';
    } else if (/Chrome/.test(navigator.userAgent)) {
      passCount = passCount['chrome'] || 'all';
    } else {
      console.warn('Unknown user-agent, assuming all tests should pass.');
      passCount = 'all';
    }
  } else {
    passCount = parseInt(passCount);
  }
  document.getElementById('qunitChecker').textContent =
      'Expecting ' + passCount + ' QUnit tests to pass.'

  var i = setInterval(function() {
    var statusElement =
        document.getElementById('qunit-testresult-caja-guest-0___');
    var currentStatus = statusElement ? statusElement.textContent : '';
    if (/^Tests completed/.exec(currentStatus)) {
      clearInterval(i);

      jsunitCallback(function() {
        var passed = numberByClass(statusElement, 'passed');
        var failed = numberByClass(statusElement, 'failed');
        if (passCount !== 'all') {
          assertEquals(currentStatus, passCount, passed);
        } else {
          assertEquals(currentStatus, 0, failed);
        }

        jsunitPass('qunitChecker');
      }, 'qunitChecker')();
    }
  }, 100);

});
