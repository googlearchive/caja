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
 * @fileoverview Tests "makeDefensibleObject" and "makeDefensibleFunction".
 *
 * @author ihab.awad@gmail.com
  */

(function () {

  caja.initialize({
    cajaServer: '/caja',
    debug: true,
    forceES5Mode: inES5Mode
  });

  // Set up basic stuff
  var uriPolicy = {
    rewrite: function (uri, uriEffect, loaderType, hints) { return uri; }
  };

  caja.load(undefined, uriPolicy, function (frame) {

    var extraImports = createExtraImportsForTesting(caja, frame);

    var called = false;

    extraImports.defensibleFunction = caja.makeDefensibleFunction___(
        function(o) {
          assertEquals(caja.USELESS, this);
          called = true;
          return o;
        });

    extraImports.isDefensibleFunctionCalled = caja.tame(caja.markFunction(
        function() {
          var result = called;
          called = false;
          return result;
        }));

    extraImports.defensibleObject = caja.makeDefensibleObject___({
          rwProp: {
            value: 42,
            writable: true,
            enumerable: true,
            configurable: false
          },
          roProp: {
            value: 49,
            writable: false,
            enumerable: true,
            configurable: false
          },
        });

    frame.code('es53-test-defensible-objects-guest.js')
         .api(extraImports)
         .run(function (_) {
             readyToTest();
             jsunitRun();
           });
  });
})();
