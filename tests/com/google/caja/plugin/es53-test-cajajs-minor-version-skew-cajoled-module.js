// Copyright (C) 2012 Google Inc.
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
 * @fileoverview This test attempts to run some guest code as a
 * cajoled module, where the embedded Caja version number in the
 * cajoled module does not match the version of the JavaScript files
 * exactly but differs in the minor version.
 *
 * @author jasvir@gmail.com
 * @author ihab.awad@gmail.com
 * @requires caja, jsunitRun, readyToTest
 */

jsunitRegister('testMinorVersionSkew', function testMinorVersionSkew() {
  fetch('es53-test-guest.out.js', function(script) {
    script = script.replace(
        new RegExp(cajaBuildVersion, 'g'),
        cajaBuildVersion + 'M2');
    caja.initialize({
      cajaServer: '/caja',
      debug: !minifiedMode,
      es5Mode: false
    });
    caja.load(undefined, undefined, function (frame) {
      caja.iframe.contentWindow.console = console;
      frame.iframe.contentWindow.console = console;
      clientSideLoaded = true;
      try {
        frame.cajoled('/', script, undefined)
             .api({ x : 1, y : 2 })
             .run(function(result) {
               jsunitPass('testMinorVersionSkew');
             });
      } catch (e) {
        fail('testMinorVersionSkew');
      }
    });
  });
});

// Start the actual testing.

readyToTest();
jsunitRun();
