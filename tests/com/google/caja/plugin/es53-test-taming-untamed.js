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

/**
 * @fileoverview ES53 tests of un-taming guest objects for use by host.
 *
 * @author ihab.awad@gmail.com
 * @requires caja, jsunitRun, readyToTest
 */

(function () {

  caja.initialize({
    cajaServer: '/caja',
    debug: true,
    forceES5Mode: inES5Mode
  });

  // Set up basic stuff
  var div = createDiv();
  var uriPolicy = {
    rewrite: function (uri, uriEffect, loaderType, hints) { return uri; }
  };

  caja.load(div, uriPolicy, function (frame) {

    // Invoke cajoled tests
    var extraImports = createExtraImportsForTesting(caja, frame);

    extraImports.tamingFrameUSELESS = caja.USELESS;

    if (!inES5Mode) {
      extraImports.guestFrameUSELESS = frame.iframe.contentWindow.___.USELESS;
    }

    // An object that can be used by cajoled guest code to store some state
    // between invocations of the "eval" functions (defined below).
    var state = {};

    // A generic function to eval() code in the host.
    // This function does *NOT* untame/tame its args/return value
    extraImports.directEval = function(s, a, b, c) {
      return eval(String(s));
    };
    extraImports.directEval.i___ = extraImports.directEval;

    // A generic function to eval() code in the host.
    // This function untames/tames its args/return value
    extraImports.tameEval =
        frame.tame(caja.markFunction(function(s, a, b, c) {
          return eval(String(s));
        }));

    extraImports.tamedJson = frame.tame({a: 1});

    frame.code('es53-test-taming-untamed-guest.html')
         .api(extraImports)
         .run(function (_) {
             readyToTest();
             jsunitRun();
           });
  });
})();
