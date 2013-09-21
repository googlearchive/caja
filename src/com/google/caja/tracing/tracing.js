// Copyright (C) 2013 Google Inc.
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
 * @fileoverview The tracing runtime library.
 *
 * @author ihab.awad@gmail.com
 * @provides TRACING
 */

var TRACING = (function() {

  // Array Remove - By John Resig (MIT Licensed)
  function arrayRemove(a, from, to) {
    var rest = a.slice((to || from) + 1 || a.length);
    a.length = from < 0 ? a.length + from : from;
    return Array.prototype.push.apply(a, rest);
  };

  var settings;
  var active = false;

  var rootCtx, ctx = {
    level: 0,
    type: 'frame',
    pos: 'ROOT',
    name: 'ROOT',
    parent: (void 0),
    entries: []
  };

  function stringify(o) {
    // Exclude 'parent' pointers since these create cycles.
    return JSON.stringify(o, function(k, v) {
      if (k === 'parent' || k === 'result') {
        return undefined;
      }
      return v;
    }, ' ');
  }

  function removeObj(a, o) {
    for (var i = 0; i < a.length; i++) {
      if (a[i] === o) {
        arrayRemove(a, i, i);
        return;
      }
    }
  }

  function doPop() {
    if (!active) {
      removeObj(ctx.parent.entries, ctx);
    }
    ctx = ctx.parent;
  }

  function clear() {
    for (var c = ctx; c.parent; c = c.parent) {
      c.parent.entries = [ c ];
    }
  }

  function pushFrame(pos, name) {
    var newCtx = {
      level: ctx.level + 1,
      type: 'frame',
      pos: pos,
      name: name,
      parent: ctx,
      entries: []
    };
    ctx.entries.push(newCtx);
    ctx = newCtx;
  }

  function popFrame() {
    var o = ctx.result;
    delete ctx.result;
    if (!ctx.parent) {
      throw new Error('null parent!!');
    }
    doPop();
    return o;
  }

  function pushCallsite(type, pos, name) {
    var newCtx = {
      level: ctx.level + 1,
      type: type,
      pos: pos,
      name: name,
      parent: ctx,
      entries: []
    };
    ctx.entries.push(newCtx);
    ctx = newCtx;
  }

  function popCallsite(result, opt_retval) {
    ctx.result = result;
    doPop();
    return opt_retval;
  }

  function log(pos, s) {
    ctx.entries.push({
      type: 'log',
      pos: pos,
      value: s
    });
  }

  function send(name) {
    var xhr = new XMLHttpRequest();
    xhr.open('POST', settings.sendEndpoint + name);
    xhr.send(stringify(ctx) + '\n');
  }

  function initialize(opts) {
    settings = opts;
  }

  function setActive(v) {
    active = !!v;
  }

  function getTrace() {
    return rootCtx;
  }

  return {
    _uf: pushFrame,
    _of: popFrame,
    _uc: pushCallsite,
    _oc: popCallsite,

    pushFrame: pushFrame,
    popFrame: popFrame,
    pushCallsite: pushCallsite,
    popCallsite: popCallsite,

    log: log,

    clear: clear,
    setActive: setActive,

    send: send,

    getTrace: getTrace,

    initialize: initialize,
  };
})();
