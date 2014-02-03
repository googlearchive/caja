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
 * Note that despite the name, this implements gapi.load as well as
 * gapi.client.load, for historical reasons.
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

    safeWindow.gapi.load = mf(function(name, optionsOrCallback) {
      name = '' + name;

      var callback;
      if (typeof optionsOrCallback === 'function') {
        callback = optionsOrCallback;
      } else if (typeof optionsOrCallback === 'object' &&
          typeof (callback = optionsOrCallback.callback) === 'function') {
        // read once and assigned in condition
      } else {
        throw new Error('gapi.load() requires a callback as second argument');
      }

      if (name === 'client') {
        topLevelCallback.setCallback(callback);
        gapi.load('client', function() {
          topLevelCallback.signalLoadClient();
        });
      } else {
        // TODO(kpreid): Kludge. Replace this with a more general mechanism
        // when we have more information about how APIs are named.
        if ((/\./).test(name)) {
          // Reject dotted names as otherwise we'd permit names like
          // "client.urlshortener" which come from gapi.client.load instead.
          // All other malformed names will be caught by the whitelist.
          throw new Error('API name should not contain "." characters.');
        }
        var fullName = name === 'picker' ? 'google.picker' : 'gapi.' + name;

        utils.validateNameAndLoadPolicy(fullName, undefined, function(policy) {
          gapi.load(name, function() {
            utils.reapplyPolicies();
            callback && callback.call({});
          });
        });
      }
    });

    safeWindow.gapi.client.load = mf(function(name, version, callback) {
      var fullName = 'gapi.client.' + name;
      version = '' + version;

      utils.validateNameAndLoadPolicy(fullName, undefined, function(policy) {
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
