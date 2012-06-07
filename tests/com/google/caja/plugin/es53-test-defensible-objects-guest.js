// Copyright (C) 2011 Google Inc.
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

jsunitRegister('testDefensibleFunction',
               function testDefensibleFunction() {
  assertEquals('function', (typeof defensibleFunction));
  assertEquals(5, defensibleFunction(5));
  assertTrue(isDefensibleFunctionCalled());
  expectFailure(function() {
    defensibleFunction.foo = 99;
  });
  expectFailure(function() {
    Object.defineProperty(defensibleFunction, 'foo', {
      value: 99
    });
  });
  pass('testDefensibleFunction');
});

jsunitRegister('testDefensibleObject',
               function testDefensibleObject() {
  assertEquals('object', (typeof defensibleObject));
  // rwProp
  assertEquals(42, defensibleObject.rwProp);
  defensibleObject.rwProp = 99;
  assertEquals(99, defensibleObject.rwProp);
  // roProp
  assertEquals(49, defensibleObject.roProp);
  expectFailure(function() {
    defensibleObject.roProp = 99;
  });
  assertEquals(49, defensibleObject.roProp);
  // configurability
  expectFailure(function() {
    Object.defineProperty(defensibleFunction, 'rwProp', {
      value: 99
    });
  });
  expectFailure(function() {
    Object.defineProperty(defensibleFunction, 'foo', {
      value: 99
    });
  });
  pass('testDefensibleObject');
});
