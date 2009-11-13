// Copyright (C) 2009 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @author maoziqing@gmail.com
 * 
 * A test file for asynchronous commonJS-style module loading in Valija
 */
env.assertEquals(env.x, 6);
exports.inc = function(x) {
  var r = env.Q.defer();
  var m = require.async('../add');
  env.Q.when(m, function(module) {
                  r.resolve(module.add(x, 1));
                },
                function(reason) {
                  r.resolve(
                      env.Q.reject("Loading module Add failed, " + reason)); 
                });
  return r.promise;
};
