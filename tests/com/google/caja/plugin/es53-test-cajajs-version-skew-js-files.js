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
 * @fileoverview This test attempts to run a simple piece of guest code. It
 * is intended to be invoked in a number of situations under which the version
 * number of some underlying piece of the infrastructure is incorrect (i.e.,
 * does not match the "expected" version number baked into the "caja.js" file).
 *
 * @author ihab.awad@gmail.com
 * @requires caja, jsunitRun, readyToTest
 */

// Alias the console to capture messages for future matching.

var consoleMessages = "";

// Callback for error checking

var checkErrorsInterval = undefined;

var testConsole = {
  log: function(msg) { consoleMessages += msg; },
  toString: function() { return 'fake console for skew'; }
};

// Register the test case.

registerTest('testVersionSkew', function testVersionSkew() {
  caja.initialize({
    cajaServer: '/caja',
    console: testConsole
  });
  caja.load(undefined, undefined, function (frame) {
    var extraImports = { x: 4, y: 3 };
    frame.code('es53-test-guest.js', 'text/javascript')
         .api(extraImports)
         .run(function(result) {
           clearInterval(checkErrorsInterval);
           // If we succeed in running, we fail the test!
           fail('testVersionSkew');
         });
  });
});

// Start the actual testing.

readyToTest();
jsunitRun();

// Check for error strings in the console and pass if the expected error
// is seen.

function checkErrors() {
  // TODO(ihab.awad): If we can pass the expected error message on the URL
  // to the test, we can look for custom errors for each individual case. We
  // would have to URL-encode/decode the expected message.
  if (/Version error/.test(consoleMessages)) {
    clearInterval(checkErrorsInterval);
    jsunitPass('testVersionSkew');
  }
}

checkErrorsInterval = setInterval(checkErrors, 125);
