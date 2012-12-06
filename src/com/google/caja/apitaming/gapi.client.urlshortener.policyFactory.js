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
 * Policy factory for "gapi.client.adsense" API.
 *
 * @author ihab.awad@gmail.com
 * @requires caja
 * @overrides window
 */
caja.tamingGoogleLoader.addPolicyFactory(
  'gapi.client.urlshortener', function(frame, utils) {

  function request() {
    var o = function() {};
    o.__after__ = [
      function(f, self, r) {
        // Returns instance of non exposed class that we can't tame
        return {
          execute: frame.markFunction(function(callback) {
            return r.execute(callback);
          })
        };
      }
    ];
    return o;
  }

  return {
    value: {
      gapi: {
        client: {
          urlshortener: {
            url: {
              get: request()
            }
          }
        }
      }
    }
  };
});
