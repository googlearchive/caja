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
 * Requires that caja.js, asserts.js and browser-stubs.js be loaded first.
 */

var imports = ___.copy(___.sharedImports);
var exports = {};
imports.fail = fail;
___.simpleFunc(fail);
imports.assertEquals = assertEquals;
___.simpleFunc(assertEquals);
imports.assertTrue = assertTrue;
___.simpleFunc(assertTrue);
imports.assertFalse = assertFalse;
___.simpleFunc(assertFalse);
imports.document = document;
imports.console = console;
___.simpleFunc(console.log);
// Included in order to test this function; 
// stamp should never be made avaliable to real caja code.W
imports.stamp = ___.stamp;
___.simpleFunc(___.stamp);
imports.exports = exports;
___.getNewModuleHandler().setImports(imports);
imports.htmlEmitter___ = new HtmlEmitter(document.getElementById("test-test"));
