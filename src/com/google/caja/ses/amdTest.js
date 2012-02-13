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
 * Tests anon case. Tests importing other modules.
 * @requires define
 */

define(['amdTest1', 'amdTestDir/amdTest2', 'amdTest3'],
function(amdTest1,              amdTest2,   amdTest3) {
  "use strict";

  // debugger; // See if we can step into amdTest3.js
  var text3 = amdTest3();
  return amdTest1 + amdTest2 + text3;
});
