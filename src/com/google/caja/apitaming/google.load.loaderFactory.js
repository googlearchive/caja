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
 * Loader factory for Google loader
 *
 * @author ihab.awad@gmail.com
 * @overrides caja, google, console
 */
caja.tamingGoogleLoader.addLoaderFactory(function(utils) {

  var onloads = utils.EventListenerGroup();
  var loadWasCalled = false;

  function addToSafeWindow(safeWindow) {
    if (!safeWindow.google) {
      safeWindow.google = {};
    }

    safeWindow.google.load =
        utils.frame.markFunction(function(name, opt_ver, opt_info) {

          // This is our front line of defense against a malicious guest
          // trying to break us by supplying a dumb API name like '__proto__'.
          if (!utils.whitelistedApis.has('google.' + name)) {
            throw 'API ' + name + ' is not whitelisted for your application';
          }

          loadWasCalled = true;

          utils.loadPolicy('google.' + name, function(policy) {
            var guestCallback = undefined;

            if (opt_info) {
              guestCallback = opt_info.callback;
              opt_info.callback = undefined;
              opt_info = utils.tamingUtils.copyJson(opt_info);
            } else {
              opt_info = {};
            }

            opt_info.callback = function() {
              utils.reapplyPolicies();
              if (onloads) { onloads.fire(); }
              onloads = undefined;
              guestCallback && guestCallback.call({});
            };

            if (policy.customGoogleLoad) {
              policy.customGoogleLoad(name, opt_info);
            } else {
              google.load(name, policy.version, opt_info);
            }
          });
        });

    safeWindow.google.setOnLoadCallback =
        utils.frame.markFunction(function(cb) {
          if (onloads) { onloads.add(cb); }
        });
  }

  function signalOnload() {
    // After the guest code loads, we call its 'onload' right away if it has
    // not made any 'google.load()' calls. Otherwise, we need to wait until
    // the load() requests return.
    if (!loadWasCalled) {
      onloads.fire();
      onloads = undefined;
    }
  }

  return {
    addToSafeWindow: addToSafeWindow,
    signalOnload: signalOnload
  };
});
