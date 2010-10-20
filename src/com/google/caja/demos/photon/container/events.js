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
 * Event support object.
 *
 * @require names an aray of strings containing the names of the events that
 *     will be fired.
 */

'use strict';
'use cajita';

var listeners = {};

(function() {
  var i;
  for (i = 0; i < names.length; i++) {
    listeners[names[i]] = [];
  }
})();

function listen(name, listener) {
  if (!listeners[name]) { return; }
  listeners[name].push(listener);
}

function unlisten(name, listener) {
  var i;
  if (!listeners[name]) { return; }
  for (i = 0; i < listeners[name].length; i++) {
    if (listeners[name][i] === listener) {
      listeners[name].remove(i);
    }
  }
}

// TODO(ihab.awad): Support sending more (variable) arguments with fire()
function fire(name) {
  if (!listeners[name]) { return; }
  for (var i = 0; i < listeners[name].length; i++) {
    listeners[name][i].call(undefined, name);
  }
}

/* return */ cajita.freeze({
  listen: listen,
  unlisten: unlisten,
  fire: fire
});
