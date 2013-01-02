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
 * @fileoverview Test autoswitching to ES5 supported browsers.
 * Relies on requesting always achievable NEW_SYMPTOM level of security
 * to gurantee support on modern browsers.
 *
 * @author jasvir@gmail.com
 * @requires caja, jsunitRun, readyToTest
 */

(function () {
  document.title += ' {closured=' + !caja.closureCanary + '}';

  var uriPolicy = caja.policy.net.ALL;
  var inES5Mode = false;

  caja.initialize({
    cajaServer: '/caja',
    debug: true,
    disableSecurityForDebugger: true,
    es5Mode: undefined,
    // Guaranteed achievable level of insecurity - should cause es5 to run
    maxAcceptableSeverity: 'NEW_SYMPTOM'
  }, 
  function(details) {
    inES5Mode = details['es5Mode'];
    assertTrue("Ran wrong mode", inES5Mode);
  },
  function() { fail('Unexpectedly failed to switch to run es5'); });

  registerTest('testES5Autodetected', function testES5Autodetected() {
    caja.load(undefined, uriPolicy, function (frame) {
      var extraImports = createExtraImportsForTesting(caja, frame);
      extraImports.inES5Mode = inES5Mode;
      frame.code('es53-test-assert-es5mode.js', 'text/javascript')
           .api(extraImports)
           .run(function(result) {
             jsunitPass('testES5Autodetected');
           });
    });
  });

  readyToTest();
  jsunitRun();

})();
