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

(function() {

  function base(url) {
    if (url.indexOf('?') !== -1) {
      url = url.substring(0, url.indexOf('?'));
    }
    if (url.lastIndexOf('/') !== -1) {
      url = url.substring(0, url.lastIndexOf('/'));
    }
    return url;
  }

  function runTest(test) {

    var testUrl = base('' + document.location) + '/' + test

    var uriPolicy = {
      rewrite: function(uri) {
        return /.*evil.*/.test(uri) ? undefined : uri;
      },
      fetch: function(url, mime, callback) {
        return caja.policy.net.ALL.fetch(url, mime, callback);
      }
    };

    var guestDiv = document.createElement('div');
    guestDiv.setAttribute('class', 'testFrame');
    document.body.appendChild(guestDiv);

    caja.load(guestDiv, uriPolicy, function(frame) {

      var t = caja.tamingGoogleLoader.applyToFrame(frame);
      t.whitelistApi('picker');
      t.whitelistApi('visualization');
      t.whitelistApi('maps');

      var tameConsole = frame.tame(frame.markReadOnlyRecord({
        log: frame.markFunction(function(s) {
          console.log(s);
        })
      }));

      var tameAlert = frame.tame(frame.markFunction(function(s) {
        window.alert('Test ' + test + ' alerts: ' + s);
      }));

      frame
          .code(testUrl, 'text/html' /*, testContent */)
          .api({
            console: tameConsole,
            alert: tameAlert
          })
          .run(function() {
            t.signalOnload();
          });
    });
  }

  function runTests(tests) {
    caja.tamingGoogleLoader.addPolicyFactoryUrl(
      'picker',
      './pickerPolicyFactory.js');
    caja.tamingGoogleLoader.addPolicyFactoryUrl(
      'visualization',
      './visualizationPolicyFactory.js');
    caja.tamingGoogleLoader.addPolicyFactoryUrl(
      'maps',
      './mapsPolicyFactory.js');

    caja.initialize({
      cajaServer: getUrlParam('cajaServer'),
      forceES5Mode: (getUrlParam('forceES5Mode') === 'true'),
      debug: true
    });

    for (var i = 0; i < tests.length; i++) {
      runTest(tests[i]);
    }
  }

  (function() {
    var cajaServer = getUrlParam('cajaServer');
    loadScript(cajaServer + '/caja.js', function() {
      loadScript('./cajaTamingGoogleLoader.js', function() {
        getTests(runTests);
      });
    });
  })();
})();



