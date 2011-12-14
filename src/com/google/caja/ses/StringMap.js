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
 * @fileoverview Implements StringMap - a map api for strings.
 *
 * @author Mark S. Miller
 * @author Jasvir Nagra
 * @requires cajaVM
 * @provides StringMap
 */

function StringMap() {

  function assertString(x) {
    if ('string' !== typeof(x)) {
      throw new TypeError('Not a string: ' + String(x));
    }
    return x;
  }

  var def;
  if ('undefined' !== typeof cajaVM) {
    def = cajaVM.def;
  } else {
    def = Object.freeze;
  }

  var objAsMap = Object.create(null);
  return def({
    get: function(key) {
        return objAsMap[assertString(key) + '$']; 
      },
    set: function(key, value) {
        objAsMap[assertString(key) + '$'] = value;
      },
    has: function(key) {
        return (assertString(key) + '$') in objAsMap;
      },
    'delete': function(key) {
        return delete objAsMap[assertString(key) + '$']; 
      }
  });
}
