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
 * Sets up a caja container for hosting the cajoled tests.
 * Requires that es53.js, jsUnitCore.js and env.js be loaded first.
 */

var imports = ___.copy(___.sharedImports);
var exports = {};
imports.DefineOwnProperty___('onerror', {
  value: ___.markFuncFreeze(function(a,b,c){}),
  enumerable: true,
  writable: true,
  configurable: true});
imports.DefineOwnProperty___('fail', {
  value: ___.markFuncFreeze(fail),
  enumerable: true,
  writable: false,
  configurable: false});
imports.DefineOwnProperty___('assertEquals', {
  value: ___.markFuncFreeze(assertEquals),
  enumerable: true,
  writable: false,
  configurable: false});
imports.DefineOwnProperty___('assertTrue', {
  value: ___.markFuncFreeze(assertTrue),
  enumerable: true,
  writable: false,
  configurable: false});
imports.DefineOwnProperty___('assertFalse', {
  value: ___.markFuncFreeze(assertFalse),
  enumerable: true,
  writable: false,
  configurable: false});
imports.DefineOwnProperty___('assertThrows', {
  value: ___.markFuncFreeze(assertThrows),
  enumerable: true,
  writable: false,
  configurable: false});
imports.rewriteTargetAttribute___ =
    ___.markFuncFreeze(function(value, tagName, attribName) {
      return "rewritten-" + value;
    });
imports.document = document;
imports.console = console;
___.markFuncFreeze(console.log);
// Included in order to test this function;
// stamp should never be made available to real caja code.
imports.stamp = ___.markFuncFreeze(___.stamp);
imports.exports = exports;
___.getNewModuleHandler().setImports(imports);
if (typeof _junit_ !== 'undefined') {
  // Propagate test failures upwards.
  ___.getNewModuleHandler().handleUncaughtException = (function (orig) {
        return function (exception, onerror, source, lineNum) {
          // Propagate test failures outside script blocks.
          // See fail() in asserts.js.
          if (_junit_.isAssertionFailedError(exception)) {
            throw exception;
          }
          return orig.call(___.USELESS, exception, onerror, source, lineNum);
        };
      })(___.getNewModuleHandler().handleUncaughtException);
}
imports.htmlEmitter___ = new HtmlEmitter(
    function (n) {return n;}, document.getElementById("test-test"));
