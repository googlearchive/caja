// Copyright (C) 2007 Google Inc.
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
 * These mimic the methods of the same name in jsUnitCore by Ed Hieatt which
 * in turn mimic junit.
 */

function fail(msg) {
  if (console) { console.log(msg); }
  if (Error) { throw new Error(msg); }
  throw msg;
}

function assertEquals() {
  function commonPrefix(a, b) {
    var n = 0;
    while (n < a.length && n < b.length && a.charAt(n) === b.charAt(n)) {
      ++n;
    }
    return n;
  }
  function commonSuffix(a, b, limit) {
    var i = a.length, j = b.length;
    while (i > limit && j > limit && a.charAt(i - 1) == b.charAt(j - 1)) {
      --i;
      --j;
    }
    return a.length - i;
  }

  var msg, a, b;
  switch (arguments.length) {
    case 2:
      msg = null;
      a = arguments[0];
      b = arguments[1];
      break;
    case 3:
      msg = arguments[0];
      a = arguments[1];
      b = arguments[2];
      break;
    default: throw 'missing arguments ' + argumetns;
  }
  if (a !== b) {
    if (typeof a == 'string' && typeof b == 'string') {
      var prefix = commonPrefix(a, b);
      var suffix = commonSuffix(a, b, prefix);
      msg = (msg ? msg + ' :: ' : '') + '<<' + a.substring(0, prefix) + '#' +
        a.substring(prefix, a.length - suffix) + '#'  +
        a.substring(a.length - suffix) + '>>' +
        ' != <<' + b.substring(0, prefix) + '#' +
        b.substring(prefix, b.length - suffix) + '#'  +
        b.substring(b.length - suffix) + '>>';
    } else {
      msg = (msg ? msg + ' :: ' : '') + '<<' + a + '>> : ' + (typeof a) +
        ' != <<' + b + '>> : ' + (typeof b);
    }
    fail(msg);
  }
}

function assertTrue() {
  switch (arguments.length) {
    case 1:
      assertEquals(true, arguments[0]);
      break;
    case 2:
      assertEquals(arguments[0], true, arguments[1]);
      break;
    default: throw 'missing arguments ' + arguments;
  }
}

function assertFalse() {
  switch (arguments.length) {
    case 1:
      assertEquals(false, arguments[0]);
      break;
    case 2:
      assertEquals(arguments[0], false, arguments[1]);
      break;
    default: throw 'missing arguments ' + arguments;
  }
}
