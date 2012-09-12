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
 * @fileoverview Tests of how Caja handles omitted structure in HTML document
 * inputs.
 *
 * @author kpreid@switchb.org
 * @requires caja, jsunitRun, readyToTest
 */

(function () {
  function assertGuestHtmlCorrect(frame, div) {
    var vdocContainer = div.getElementsByClassName("vdoc-body___")[0];
    debugger;
    assertEquals('<html><head><title>t</title></head><body>b</body>',
        vdocContainer.innerHTML);
  }

  caja.initialize({
    cajaServer: '/caja',
    debug: true,
    forceES5Mode: inES5Mode
  });
  
  fetch('es53-test-domado-dom-global-guest.js', function (tester) {
    function registerDomGlobalTest(testName, html, expectTitle, expectBody) {
      registerTest(testName,
          function domGlobalTestFn() {
        var div = createDiv();
        caja.load(div, undefined, function (frame) {
          frame.code(
              location.protocol + '//' + location.host + '/',
              'text/html',
              html.replace('$', '<script>'+tester+'</script>'))
            .run(createExtraImportsForTesting(caja, frame),
                function(result) {
                  frame.untame(frame.imports.globalGuestTest)(expectTitle,
                      expectBody);
                  jsunitPass(testName);
                });
        });
      });
    }

    registerDomGlobalTest('testFullyExplicit',
        '<html><head><title>t</title></head>' + 
        '<body>b$</body></html>',
        't', 'b');

    registerDomGlobalTest('testStartBody',
        '<title>t</title><body>' + 
        'b$',
        't', 'b');

    registerDomGlobalTest('testStopHead',
        '<title>t</title></head>' + 
        'b$',
        't', 'b');

    registerDomGlobalTest('testFullyImplicit',
        '<title>t</title>' + 
        'b$',
        't', 'b');

    registerDomGlobalTest('testJustText',
        'b$',
        null, 'b');

    readyToTest();
    jsunitRun();
  });
})();
