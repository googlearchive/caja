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
        utils.frame.markFunction(function(name, unused_version, opt_settings) {
      name = '' + name;

      // Pass on only whitelisted settings, and wrap callback.
      var guestCallback = undefined;
      var sanitizedSettings = {};
      if (opt_settings) {
        guestCallback = opt_settings.callback;
        if ('language' in opt_settings) {
          sanitizedSettings.language = '' + opt_settings.language;
        }
        if ('nocss' in opt_settings) {
          sanitizedSettings.nocss = !!(opt_settings.nocss);
        }
        if ('packages' in opt_settings) {
          sanitizedSettings.packages =
              Array.prototype.map.call(opt_settings.packages, String);
        }
      }
      sanitizedSettings.callback = function() {
        utils.reapplyPolicies();
        if (onloads) { onloads.fire(); }
        onloads = undefined;
        guestCallback && guestCallback.call({});
      };

      utils.validateNameAndLoadPolicy(
          'google.' + name,
          function() {
            loadWasCalled = true;
          },
          function(policy) {
            if (policy.customGoogleLoad) {
              policy.customGoogleLoad(name, sanitizedSettings);
            } else {
              google.load(name, policy.version, sanitizedSettings);
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
