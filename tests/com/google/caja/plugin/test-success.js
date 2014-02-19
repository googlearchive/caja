// Copyright (C) 2014 Google Inc.
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
 * @fileoverview Test that non-modified startup is successful (SES is happy).
 *
 * @author kpreid@switchb.org
 * @requires caja, jsunitRegister, jsunitPass, jsunitRun, readyToTest, createDiv
 */

(function () {
  jsunitRegister('testSuccess', function testSuccess() {
    caja.initialize(
        {
          cajaServer: '/caja',
          debug: true,
          maxAcceptableSeverity: 'NO_KNOWN_EXPLOIT_SPEC_VIOLATION'
        },
        function() {},
        function(err) {
          jsunitFail('testSuccess', err);
        });

    caja.load(createDiv(), caja.policy.net.ALL, function(frame) {
      frame.code(
          location.protocol + '//' + location.host + '/',
          'text/html',
          '')
          .run(function(result) {
              jsunitPass('testSuccess');
           });
    });
  });

  readyToTest();
  jsunitRun();
})();
