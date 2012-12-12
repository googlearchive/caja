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
 * @fileoverview
 * Loader factory for Google APIs loader
 *
 * @author ihab.awad@gmail.com
 * @overrides caja, google, gapi, console
 */

caja.tamingGoogleLoader.addLoaderFactory(function(utils) {

  var mf = utils.frame.markFunction;

  var topLevelCallback = (function() {
    var callback;
    var isLoadClient;
    var isOnload;

    function maybeCall() {
      if (callback && isLoadClient && isOnload) {
        try { callback.call({}); } catch (e) { /* ignore */ }
        callback = {};
      }
    }

    return {
      setCallback: function(cb) {
        if (callback) {
          throw new Error('Can only set global callback once');
        }
        callback = cb;
      },
      signalLoadClient: function() {
        isLoadClient = true;
        maybeCall();
      },
      signalOnload: function() {
        isOnload = true;
        maybeCall();
      }
    };
  })();

  function addToSafeWindow(safeWindow) {
    if (!safeWindow.gapi) {
      safeWindow.gapi = {};
    }

    if (!safeWindow.gapi.client) {
      safeWindow.gapi.client = {};
    }

    safeWindow.gapi.load = mf(function(name, callback) {
      name = '' + name;
      if (name !== 'client') {
        throw new Error('gapi.load() only accepts "client" as first argument');
      }
      if (!callback) {
        throw new Error('gapi.load() requires a callback as second argument');
      }
      topLevelCallback.setCallback(callback);
      gapi.load('client', function() {
        topLevelCallback.signalLoadClient();
      });
    });

    safeWindow.gapi.client.load = mf(function(name, version, callback) {
      var fullName = 'gapi.client.' + name;
      version = '' + version;

      // This is our front line of defense against a malicious guest
      // trying to break us by supplying a dumb API name like '__proto__'.
      if (!utils.whitelistedApis.has(fullName)) {
        throw 'API ' + name + ' is not whitelisted for your application';
      }

      utils.loadPolicy(fullName, function(policy) {
        gapi.client.load(name, version, function() {
          utils.reapplyPolicies();
          callback && callback.call({});
        });
      });
    });

    safeWindow.gapi.client.setApiKey = mf(function(apiKey) {
      // Container should provide API key, since container is also in charge
      // of OAuth grants (see 'authorize' below).
    });

    if (!safeWindow.gapi.auth) {
      safeWindow.gapi.auth = {};
    }

    safeWindow.gapi.auth.authorize = mf(function(args) {
      // Should be done by the container
    });
  }

  function signalOnload() {
    topLevelCallback.signalOnload();
  }

  return {
    addToSafeWindow: addToSafeWindow,
    signalOnload: signalOnload
  };
});
