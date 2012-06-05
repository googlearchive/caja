// Copyright (C) 2011 Google Inc.
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
 * @fileoverview Trivial test of simple AMD loader.
 * Tests anon case. No dependencies. With Axel's CommonJS Adapter
 * boilerplate. See http://www.2ality.com/2011/11/module-gap.html
 *
 * @requires define, require
 * @overrides module
 */

({ define: typeof define === "function" ?
    define :
    function(A,F) { module.exports = F.apply(null, A.map(require)); }}).
define([], function() {
  "use strict";

  return function() {
    return 'test';
  };
});
