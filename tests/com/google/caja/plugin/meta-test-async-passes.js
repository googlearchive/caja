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

// Used by meta-test.js for testing our test framework.
//
// Checking that an asynchronous requirement which does not immediately pass
// will eventually be recognized as passing.

jsunitRegister('testAsyncPasses', function() {
  var i = 0;
  assertAsynchronousRequirement(
      'this requirement passes',
      jsunitCallback(function() { return i++ > 2; }));
  pass('testAsyncPasses');
});

jsunitRegister('testAsyncPassesDummyOther', function() {
  pass('testAsyncPassesDummyOther');
});