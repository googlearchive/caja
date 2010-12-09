// Copyright (C) 2010 Google Inc.
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
 * List data type.
 *
 * @return a list.
 */

'use strict';

// Array Remove - By John Resig (MIT Licensed)
var arrayRemove = function(array, from, to) {
  var rest = array.slice((to || from) + 1 || array.length);
  array.length = from < 0 ? array.length + from : from;
  return array.push.apply(array, rest);
};

var contents = [];
var stop = Object.freeze({});

var self = {
  stop: Object.freeze({}),
  push: function(o) {
    contents.push(o);
  },
  length: function() {
    return contents.length;
  },
  getAt: function(i) {
    return contents[Number(i)];
  },
  setAt: function(i, o) {
    contents[Number(i)] = o;
  },
  remove: function(o) {
    for (var i = 0; i < contents.length; i++) {
      if (contents[i] === o) {
        arrayRemove(contents, i, i);
        return;
      }
    }
  },
  removeAt: function(i) {
    i = Number(i);
    arrayRemove(contents, i, i);
  },
  each: function(f) {
    try {
      // Sensitive to in-loop mutations
      for (var i = 0; i < contents.length; i++) {
        if (f(contents[i]) === stop) {
          return;
        }
      }
    } catch (e) {
      cajaVM.log('Sadness: ' + e);
    }
  }
};

self.asReadOnly = Object.freeze({
  stop: self.stop,
  length: self.length,
  getAt: self.getAt,
  each: self.each
});

/* return */ Object.freeze(self)