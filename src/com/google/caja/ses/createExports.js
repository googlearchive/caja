// Copyright (C) 2012 Google Inc.
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
 * Trivial shim for mitigateGotchas's dependencies' module exports.
 *
 * This fails to be a proper loader in that it gives all the modules a single
 * exports object, conflating their namespaces; this just happens to work for
 * the case we care about. If we start doing anything remotely more interesting
 * we should be more correct about it, perhaps using the makeSimpleAMDLoader
 * already available.
 *
 * @author jasvir@gmail.com
 * @author kpreid@switchb.org
 * \@provides exports, require, define
 */
var exports = {};
function require(name) {
  return exports;
}
function define(d, f) {
  f(exports);
}
define.amd = true;