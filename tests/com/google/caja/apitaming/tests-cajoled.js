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
      rewrite: function(uri, a, b, hints) {
        console.log('Rewrite URI: ', uri, ' with hints: ', hints);
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
      t.whitelistApi('google.picker');
      t.whitelistApi('google.visualization');
      t.whitelistApi('google.maps');
      t.whitelistApi('gapi.client.urlshortener');

      var tameConsole = frame.tame(frame.markReadOnlyRecord({
        log: frame.markFunction(function(s) {
          console.log(s);
        })
      }));

      var tameAlert = frame.tame(frame.markFunction(function(s) {
        window.alert('Test ' + test + ' alerts: ' + s);
      }));

      frame
          .code(testUrl, 'text/html')
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
        'google.picker',
        './google.picker.policyFactory.js');
    caja.tamingGoogleLoader.addPolicyFactoryUrl(
        'google.visualization',
        './google.visualization.policyFactory.js');
    caja.tamingGoogleLoader.addPolicyFactoryUrl(
        'google.maps',
        './google.maps.policyFactory.js');
    caja.tamingGoogleLoader.addPolicyFactoryUrl(
        'gapi.client.urlshortener',
        './gapi.client.urlshortener.policyFactory.js');

    caja.initialize({
      cajaServer: getUrlParam('cajaServer'),
      maxAcceptableSeverity: 'NEW_SYMPTOM',
      debug: (getUrlParam('debug') === 'true')
    });

    for (var i = 0; i < tests.length; i++) {
      runTest(tests[i]);
    }
  }

  loadScript([
    getUrlParam('cajaServer') + '/caja.js',
    './cajaTamingGoogleLoader.js',
    './google.load.loaderFactory.js',
    './gapi.client.load.loaderFactory.js'
    ],
    function() { getTests(runTests); });
})();



