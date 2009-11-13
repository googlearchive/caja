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
 * A test file for recursive asynchronous module loading in Cajita
 */
var result = Q.defer();
if (x <= 0) {
  result.resolve(-1);
}
else if (x == 1) {
  result.resolve(1);
}
else {
  var m = load.async('./recursion');
  Q.when(m, function(module) {
	var r = module({x: x - 1, load: load, Q: Q});
	Q.when(r, function(r) {result.resolve(x * r); });
  });
}
result.promise;
