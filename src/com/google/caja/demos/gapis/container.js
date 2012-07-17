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

  // URL parameter parsing code from blog at:
  // http://www.netlobo.com/url_query_string_javascript.html
  function getUrlParam(name) {
    name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
    var regexS = "[\\?&]"+name+"=([^&#]*)";
    var regex = new RegExp(regexS);
    var results = regex.exec(window.location.href);
    return (results == null) ? "" : results[1];
  }

  function copyJson(o) {
    return JSON.parse(JSON.stringify(o, function(key, value) {
      return /__$/.test(key) ? void 0 : value;
    }));
  }

  function jsonInputsAdvice(f, self, args) {
    return args.map(copyJson);
  }

  var cajaServer = getUrlParam('cajaServer');

  var cajaScript = document.createElement('script');
  cajaScript.setAttribute('src', cajaServer + '/caja.js');
  cajaScript.onload = cajaReady;
  document.head.appendChild(cajaScript);

  function cajaReady() {

    caja.initialize({
      cajaServer: cajaServer,
      forceES5Mode: true,
      debug: true
    });

    var guestDiv = document.getElementById('guest');

    caja.load(guestDiv, caja.policy.net.ALL, function(frame) {

      var onloads = (function() {
        var fired = false;
        var cbs = [];
        return {
          add: function(cb) {
            if (fired) { return; }
            cbs.push(cb);
          },
          fire: function() {
            if (fired) { return; }
            for (var i = 0; i < cbs.length; i++) {
              try { cbs[i](); } catch (e) {}
            }
            fired = true;
            cbs = undefined;
          }
        };
      })();

      var loadPending = false;

      function addLoadTo(safeGoogle) {

        safeGoogle.load = caja.markFunction(function(name, opt_version, opt_info) {
          loadPending = true;
          if (!opt_info) { opt_info = {}; }
          var guestCallback = opt_info.callback;
          opt_info.callback = function() {
            frame.imports.google = frame.tame(addLoadTo(defGoogle(frame)));
            onloads.fire();
            guestCallback && guestCallback();
          };
          google.load(name, opt_version, opt_info);
        });

        caja.adviseFunctionBefore(safeGoogle.load, function(f, self, args) {
          var opts = args[2];
          if (opts) {
            var cb = opts.callback;
            delete opts.callback;
            opts = copyJson(opts);
            opts.callback = cb;
          }
          return [
            args[0],
            args[1],
            opts
          ];
        });

        safeGoogle.setOnLoadCallback = caja.markFunction(function(guestCallback) {
          onloads.add(guestCallback);
        });

        return safeGoogle;
      }

      frame.code('guest.html', 'text/html')
           .api({
             google: frame.tame(caja.markReadOnlyRecord(addLoadTo({}))),
             console: frame.tame(caja.markReadOnlyRecord({
               log: caja.markFunction(function(s) {
                 console.log(s);
               })
             }))
           })
           .run(function() {
             if (!loadPending) { onloads.fire(); }
           });
    });
  }
})();



