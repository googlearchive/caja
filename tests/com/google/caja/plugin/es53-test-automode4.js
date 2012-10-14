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
 * to force failover on modern browsers.
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
    // Unachievable level of security - should cause es5 not to run
    maxAcceptableSeverity: 'MAGICAL_UNICORN'
  }, 
  function(details) { fail('Unexpectedly succeeded running ES5'); },
  function(e) {
    assertStringContains("browser is unsupported", String(e)); 

    // Try again using es53 explicitly this time
    caja.initialize({
      cajaServer: '/caja',
      debug: true,
      es5Mode: false
    }, 
    function(details) { assertFalse("Ran wrong mode", details['es5Mode']); },
    function() { fail('Unexpectedly failed to ES55'); });
  });

  registerTest('testES5ExplicitFailoverToES53', 
      function testES5ExplicitFailoverToES53() {
    caja.load(undefined, uriPolicy, function (frame) {
      frame.code('es53-test-assert-es53mode.js', 'text/javascript')
           .api(createExtraImportsForTesting(caja, frame))
           .run(function(result) {
             jsunitPass('testES5ExplicitFailoverToES53');
           });
    });
  });

  readyToTest();
  jsunitRun();

})();
