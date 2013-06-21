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

(function() {
  caja.initialize(basicCajaConfig);
  var container = createDiv();
  caja.load(
      container,
      {
        rewrite: function (uri, uriEffect, loaderType, hints) { return uri; }
      },
      function(frame) {
        var extraImports = createExtraImportsForTesting(caja, frame);

        extraImports.withFailureOnLeakedEvent =
            frame.tame(frame.markFunction(function(name, body) {
          var oops = false;
          function listener() {
            // exception deferred to avoid catch in event handler
            oops = true;
          }
          // useCapture=true to be less (accidentally-) interceptible
          container.addEventListener(name, listener, true);
          container.addEventListener(name, listener, false);
          try {
            var res = body();
            if (oops) {
              fail(name + ' event leaked to host');
            }
            return res;
          } finally {
            container.removeEventListener(name, listener, true);
            container.removeEventListener(name, listener, false);
          }
        }));

        frame.code('es53-test-domado-events-guest.html')
             .api(extraImports)
             .run(function(result) {
                   readyToTest();
                   jsunitRun(null, asyncRequirements.evaluate);
                 });
  });
}());
