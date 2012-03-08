// Copyright (C) 2011 Google Inc.
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
 * @fileoverview This checks that caja.js never starts, which is important in
 * cases where some problem *should* cause it to fail.
 *
 * @author ihab.awad@gmail.com
 * @requires caja, jsunitRun, readyToTest
 */

var testFailed = false;

registerTest('testNeverStarts', function testNeverStarts() {
  caja.initialize({
    cajaServer: '/caja'
  });
  caja.whenReady(function() {
    testFailed = true;
    jsunitFail('testNeverStarts');
  });
  caja.load(undefined, undefined, function (frame) {
    testFailed = true;
    jsunitFail('testNeverStarts');
  });
});

readyToTest();
jsunitRun();

var checkFailure = function() {
  if (!testFailed) {
    jsunitPass('testNeverStarts');
  }
}

// In a test server, two seconds is enough for startup to occur
// if it's ever going to happen.
setTimeout(checkFailure, 2000);
