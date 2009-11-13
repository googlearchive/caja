// Copyright (C) 2009 Google Inc.
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

'use strict';
'use cajita';

/**
 * @author maoziqing@gmail.com
 * 
 * A test file for asynchronous module loading in Cajita
 */
var m = load.async('../c');
var f1 = function(module) {
  var r1 = module({x: x});
  var r2 = module({x: y});
  return r1 + r2;
};
var f2 = function(reason) {
  fail('Loading module C failed, ' + reason);
};
Q.when(m, f1, f2);
