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
 * @fileoverview Test failover from ES5 to ES53 on unsupported browsers.
 * Relies on requesting unachievable MAGICAL_UNICORN level of security
 * to force failover to ES53 on modern browsers.
 *
 * @author jasvir@gmail.com
 * @requires caja, jsunitRun, readyToTest
 */

(function () {
  document.title += ' {closured=' + !caja.closureCanary + '}';

  var uriPolicy = caja.policy.net.ALL;

  caja.initialize({
    cajaServer: '/caja',
    debug: true,
    es5Mode: true,
    // Unachievable level of security - should cause es5 to refuse to run
    maxAcceptableSeverity: 'MAGICAL_UNICORN'
  }, 
  function(details) { fail('Unexpectedly succeeded running ES5'); },
  function(e) { 
    assertStringContains("browser is unsupported", String(e)); 
    jsunitPass('testES5Fails');
  });

  registerTest('testES5Fails', function testES5Fails() {
    caja.load(undefined, uriPolicy, function (frame) {
      frame.code('https://fake.url/', 'text/javascript',
           'throw "Should not have run"')
           .api(createExtraImportsForTesting(caja, frame))
           .run(function(result) {
             fail('Unexpectedly succeeded running ES5');
           });
    });
  });

  readyToTest();
  jsunitRun();

})();
