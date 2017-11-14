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
  var inES5Mode = false;

  caja.initialize({
    cajaServer: '/caja',
    debug: !minifiedMode,
    es5Mode: undefined,
    // Unachievable level of security - should cause es5 => es53 failover
    maxAcceptableSeverity: 'MAGICAL_UNICORN'
  }, 
  function(details) {
    inES5Mode = details['es5Mode'];
    assertFalse("Ran wrong mode", inES5Mode);
  },
  function() { fail('Unexpectedly failed to switch to es53'); });

  jsunitRegister('testES5FailsoverToES53', function testES5FailsoverToES53() {
    caja.load(undefined, uriPolicy, function (frame) {
      var extraImports = createExtraImportsForTesting(caja, frame);
      extraImports.inES5Mode = inES5Mode;
      frame.code('test-assert-es53mode.js', 'text/javascript')
           .api(extraImports)
           .run(function(result) {
             jsunitPass('testES5FailsoverToES53');
           });
    });
  });

  readyToTest();
  jsunitRun();

})();
