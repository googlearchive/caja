// Copyright (C) 2016 Google Inc.
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

// Test-driver file for test-security-guest.html.

// TODO(kpreid): Duplicating code found in apitaming/tests-utils.js and other
// places. Maybe put this into browser-test-case.js or similar and refactor
// other uses of this pattern.
function loadScript(src, cb) {
  "use strict";

  if (src instanceof Array) {
    if (src.length === 0) {
      cb();
    } else {
      loadScript(src[0], function() {
        loadScript(src.slice(1), cb);
      });
    }
  } else {
    var script = document.createElement('script');
    script.setAttribute('src', src);
    script.onload = cb;
    document.head.appendChild(script);
  }
}

function evilGlobalFunction() {
  var el = document.createElement('div');
  document.body.appendChild(el);
  el.textContent = 'Global function invoked! Failed security test!';
  el.className = 'unsafe-global';  // checked for in individual tests
}

// TODO(kpreid): All this Google API loading glue is duplicated from
// ./tests-cajoled.js and should be improved somehow.
loadScript([
  'https://www.google.com/jsapi',
  'https://apis.google.com/js/client.js',
  '../apitaming/cajaTamingGoogleLoader.js',
  '../apitaming/google.load.loaderFactory.js',
  '../apitaming/gapi.client.load.loaderFactory.js',
], function() {
  "use strict";
  caja.tamingGoogleLoader.addPolicyFactoryUrl(
      'google.picker',
      '../apitaming/google.picker.policyFactory.js');
  caja.tamingGoogleLoader.addPolicyFactoryUrl(
      'google.visualization',
      '../apitaming/google.visualization.policyFactory.js');
  caja.tamingGoogleLoader.addPolicyFactoryUrl(
      'google.maps',
      '../apitaming/google.maps.policyFactory.js');
  caja.tamingGoogleLoader.addPolicyFactoryUrl(
      'gapi.client.urlshortener',
      '../apitaming/gapi.client.urlshortener.policyFactory.js');

  caja.initialize(basicCajaConfig);

  caja.load(
      createDiv(),
      undefined,
      function(frame) {
        var extraImports = createExtraImportsForTesting(caja, frame);

        var t = caja.tamingGoogleLoader.applyToFrame(frame);
        t.whitelistApi('google.picker');
        t.whitelistApi('google.visualization');
        t.whitelistApi('google.maps');
        t.whitelistApi('gapi.client.urlshortener');

        frame.code('../apitaming/test-security-guest.html')
             .api(extraImports)
             .run(function(result) {
                    readyToTest();
                  });
      });
})
