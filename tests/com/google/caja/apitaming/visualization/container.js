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

  function loadScript(src, cb) {
    var script = document.createElement('script');
    script.setAttribute('src', src);
    script.onload = cb;
    document.head.appendChild(script);
  }

  // URL parameter parsing code from blog at:
  // http://www.netlobo.com/url_query_string_javascript.html
  function getUrlParam(name) {
    name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
    var regexS = "[\\?&]"+name+"=([^&#]*)";
    var regex = new RegExp(regexS);
    var results = regex.exec(window.location.href);
    return decodeURIComponent((results == null) ? "" : results[1]);
  }

  var cajaServer = getUrlParam('cajaServer');
  var forceES5Mode = (getUrlParam('forceES5Mode') === 'true');

  loadScript(cajaServer + '/caja.js', function() {
    loadScript('../cajaTamingGoogleLoader.js', cajaReady);
  });

  function cajaReady() {

    caja.tamingGoogleLoader.addPolicyFactoryUrl(
        'visualization',
        '../visualizationPolicyFactory.js');

    caja.initialize({
      cajaServer: cajaServer,
      forceES5Mode: forceES5Mode,
      debug: true
    });

    var guestDiv = document.getElementById('guest');

    var uriPolicy = {
      rewrite: function(uri) {
        return /.*evil.*/.test(uri) ? undefined : uri;
      },
      fetch: function(url, mime, callback) {
        return caja.policy.net.ALL.fetch(url, mime, callback);
      }
    };

    caja.load(guestDiv, uriPolicy, function(frame) {

      var t = caja.tamingGoogleLoader.applyToFrame(frame);
      t.whitelistApi('visualization');

      var tameConsole = frame.tame(caja.markReadOnlyRecord({
        log: caja.markFunction(function(s) {
          console.log(s);
        })
      }));

      frame
          .code('guest.html', 'text/html')
          .api({
            console: tameConsole,
          })
          .run(function() {
            t.signalOnload();
          });
    });
  }
})();



