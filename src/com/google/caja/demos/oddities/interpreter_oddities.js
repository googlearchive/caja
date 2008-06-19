// Copyright (C) 2008 Google Inc.
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
 * A set of tests of marginal behavior of different javascript interpreters.
 *
 * @author mikesamuel@gmail.com
 */


function oddities() {

// Test the scoping of catch blocks.
(function () {
  var x = 0, y = 0, z = 0, w = 0;
  (function () {
    var x = 1;
    try {
      throw null;
    } catch (x) {
      var x = 2;
      var y = 3;
      var z;
      w = 4;
    }
    z = 5;
    result('inner-scope: x=' + x + ', y=' + y + ', z=' + z + ', w=' + w);
  })();
  result('outer-scope: x=' + x + ', y=' + y + ', z=' + z + ', w=' + w);
})();


// Test the behavior of 3 ways of declaring functions:
// # function foo() { ... }
// # var foo = function foo() { ... };
// # var foo = function () { ... };
var f;
header('anonymous function');
f = 'GLOBAL';
(function () {
  var fns = [];
  result('before loop, f is : '
         + (f === 'GLOBAL' ? 'global' : f ? 'hoisted' : 'undefined'));
  for (var i = 2; --i >= 0;) {
    var f = function () { return f === fns[0]; };
    fns[i] = f;
  }
  result('sanity check: fns[0] is f: ' + fns[0]());
  result('f in closure binds to outer scope: ' + fns[1]());
  f = null;
  result('f in closure is fns[0] after assignment to outer f: ' + fns[0]());
  result('one function allocated: ' + (fns[0] === fns[1]));
})();

header('named var');
f = 'GLOBAL';
(function () {
  var fns = [];
  result('before loop, f is : '
         + (f === 'GLOBAL' ? 'global' : f ? 'hoisted' : 'undefined'));
  for (var i = 2; --i >= 0;) {
    var f = function f() { return f === fns[0]; };
    fns[i] = f;
  }
  result('sanity check: fns[0] is f: ' + fns[0]());
  result('f in closure binds to outer scope: ' + fns[1]());
  f = null;
  result('f in closure is fns[0] after assignment to outer f: ' + fns[0]());
  result('one function allocated: ' + (fns[0] === fns[1]));
})();

header('function declaration');
f = 'GLOBAL';
(function () {
  var fns = [];
  result('before loop, f is : '
         + (f === 'GLOBAL' ? 'global' : f ? 'hoisted' : 'undefined'));
  for (var i = 2; --i >= 0;) {
    function f() { return f === fns[0]; };
    fns[i] = f;
  }
  result('sanity check: fns[0] is f: ' + fns[0]());
  result('f in closure binds to outer scope: ' + fns[1]());
  f = null;
  result('f in closure is fns[0] after assignment to outer f: ' + fns[0]());
  result('one function allocated: ' + (fns[0] === fns[1]));
})();

header("typeof postincrement n='4'");
(function () {
  var n = '4';
  result('typeof   (n++): ' + (typeof (n++)));
  n = '4';
  result('value of (n++): ' + (n++));
})();
header("typeof postincrement n='four'");
(function () {
  var n = 'four';
  result('typeof   (n++): ' + (typeof (n++)));
  n = 'four';
  result('value of (n++): ' + (n++));
})();

header("typeof preincrement n='4'");
(function () {
  var n = '4';
  result('typeof   (++n): ' + (typeof (++n)));
  n = '4';
  result('value of (++n): ' + (++n));
})();
header("typeof preincrement n='four'");
(function () {
  var n = 'four';
  result('typeof   (++n): ' + (typeof (++n)));
  n = 'four';
  result('value of (++n): ' + (++n));
})();
 
}
