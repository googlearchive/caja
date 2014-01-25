// Copyright (C) 2013 Google Inc.
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

var preFrozen = Object.freeze({});
var loadSesScript = document.createElement('script');
loadSesScript.src = '../ses/initSES.js';
loadSesScript.onload = function() {
  readyToTest();
  jsunitRun();
};
document.body.appendChild(loadSesScript);

jsunitRegister('testOk', function() {
  assertEquals(false, ses.ok('MAGICAL_UNICORN'));
  assertEquals(true, ses.ok('NEW_SYMPTOM'));

  // Check behavior given invalid severity name
  expectFailure(function() {
    ses.ok('FOO');
  });

  jsunitPass();
});

jsunitRegister('testWeakMap', function() {
  // Based on https://raw.github.com/drses/weak-map/master/verify.js
  var growingMap = new WeakMap();
  var shrinkingMap = new WeakMap();
  var emptyMap = new WeakMap();
  var postFrozen = {};

  assertEquals(growingMap, growingMap.set(postFrozen, 10));
  assertEquals(growingMap, growingMap.set(preFrozen, 11));
  assertEquals(shrinkingMap, shrinkingMap.set(postFrozen, 20));
  assertEquals(shrinkingMap, shrinkingMap.set(preFrozen, 21));

  assertEquals(10, growingMap.get(postFrozen));
  assertEquals(11, growingMap.get(preFrozen));

  Object.freeze(postFrozen);

  assertEquals(20, shrinkingMap.get(postFrozen));
  assertEquals(true, shrinkingMap.has(postFrozen));

  assertEquals(21, shrinkingMap.get(preFrozen));
  assertEquals(true, shrinkingMap.has(preFrozen));

  assertEquals(void 0, emptyMap.get(preFrozen));
  assertEquals(false, emptyMap.has(preFrozen));

  assertEquals(true, shrinkingMap.delete(postFrozen));
  assertEquals(false, shrinkingMap.has(postFrozen));
  assertEquals(false, shrinkingMap.delete(postFrozen));

  assertEquals(true, shrinkingMap.delete(preFrozen));
  assertEquals(false, shrinkingMap.has(preFrozen));
  assertEquals(false, shrinkingMap.delete(preFrozen));

  assertEquals(false, emptyMap.delete(postFrozen));
  assertEquals(false, emptyMap.delete(preFrozen));

  jsunitPass();
});
