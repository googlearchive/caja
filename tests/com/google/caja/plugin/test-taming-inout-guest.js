// Copyright (C) 2010 Google Inc.
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

jsunitRegister('testHostPureFunctions',
               function testHostPureFunctions() {
  // Arguments are un-tamed en route to host
  tamedApi.tamedHostPureFunction(
      'assertEquals(getFeralTestObject(), a);',
      getTamedTestObject());
  // 'this' is cleared to USELESS en route to host
  tamedApi.tamedHostPureFunction.call(
      getTamedTestObject(),
      'assertEquals(USELESS, this);');
  // Return value is tamed en route to guest
  assertEquals(
      getTamedTestObject(),
      tamedApi.tamedHostPureFunction('getFeralTestObject();'));
  pass('testHostPureFunctions');
});

jsunitRegister('testHostArrayElements',
               function testHostArrayElements() {
  // Array elements are tamed en route to guest
  var a = tamedApi.tamedHostPureFunction('[ getFeralTestObject() ];');
  assertEquals(1, a.length);
  assertEquals(getTamedTestObject(), a[0]);
  pass('testHostArrayElements');
});

jsunitRegister('testHostRecordProperties',
               function testHostRecordProperties() {
  // Record property is tamed en route to guest
  assertEquals(getTamedTestObject(), tamedApi.tamedHostRecord.prop);
  // Ensure guest can write to the property
  tamedApi.tamedHostRecord.prop = 42;
  evalInHost('assertEquals(42, api.tamedHostRecord.prop);');
  // Record property is un-tamed en route to host
  tamedApi.tamedHostRecord.prop = getTamedTestObject();
  evalInHost('assertEquals(getFeralTestObject(), api.tamedHostRecord.prop);');
  pass('testHostRecordProperties');
});

jsunitRegister('testHostConstructedObjectProperties',
               function testHostConstructedObjectProperties() {
  // Property is tamed en route to guest
  var x = new tamedApi.Ctor();
  evalInHost('assertEquals(42, frame.untame(a).prop);', x);
  evalInHost('frame.untame(a).prop = getFeralTestObject();', x);
  assertEquals(getTamedTestObject(), x.prop);
  // Property is un-tamed en route to host
  var y = new tamedApi.Ctor();
  evalInHost('assertEquals(42, frame.untame(a).prop);', y);
  y.prop = getTamedTestObject();
  evalInHost('assertEquals(getFeralTestObject(), frame.untame(a).prop);', y);
  pass('testHostConstructedObjectProperties');
});

jsunitRegister('testHostConstructedObjectMethods',
               function testHostConstructedObjectMethods() {
var o = new tamedApi.Ctor();
// Arguments are un-tamed en route to host
o.meth(
    'assertEquals(getFeralTestObject(), a);',
    getTamedTestObject());
// 'this' is un-tamed en route to host
o.meth('assertEquals(this, frame.untame(frame.tame(this)));');
o.meth('assertTrue(this instanceof api.Ctor);');
// Return value is tamed en route to guest
assertEquals(
    getTamedTestObject(),
    o.meth('getFeralTestObject();'));
  pass('testHostConstructedObjectMethods');
});

// TESTS ON UN-TAMED GUEST OBJECTS

// In the below, we use 'tamedApi.tamedHostPureFunction as a generic host
// function that we have established (above) correctly tames the arguments
// we pass into it. This function is not itself under test below.

jsunitRegister('testGuestPureFunctions',
               function testGuestPureFunctions() {
  // Arguments are tamed en route to guest
  (function() {
    var called = false;
    var func = function(arg) {
      called = true;
      assertEquals(getTamedTestObject(), arg);
    };
    tamedApi.tamedHostPureFunction('a(getFeralTestObject());', func);
    assertTrue(called);
  });
  // 'this' is tamed en route to guest
  (function() {
    var called = false;
    var rec = {};
    var func = function() {
      assertEquals(rec, this);
      assertTrue(called);
    };
    tamedApi.tamedHostPureFunction('b.call(a);', rec, func);
    assertTrue(called);
  });
  // Return value is un-tamed en route to host
  (function() {
    var called = false;
    var func = function() {
      called = true;
      return getTamedTestObject();
    };
    tamedApi.tamedHostPureFunction(
        'assertEquals(getFeralTestObject(), a());', func);
    assertTrue(called);
  });
  pass('testGuestPureFunctions');
});

jsunitRegister('testGuestArrayElements',
               function testGuestArrayElements() {
  // Array elements are un-tamed en route to host
  var a = [ getTamedTestObject() ];
  tamedApi.tamedHostPureFunction('assertEquals(1, a.length);', a);
  tamedApi.tamedHostPureFunction(
      'assertEquals(getFeralTestObject(), a[0]);', a);
  pass('testGuestArrayElements');
});

jsunitRegister('testGuestRecordProperties',
               function testGuestRecordProperties() {
  var rec = {
    prop: getTamedTestObject()
  };
  // Record property is un-tamed en route to host
  tamedApi.tamedHostPureFunction(
      'assertEquals(getFeralTestObject(), a.prop);', rec);
  // Ensure host can write to the property
  tamedApi.tamedHostPureFunction('a.prop = 42;', rec);
  assertEquals(42, rec.prop);
  // Record property is tamed en route to guest
  tamedApi.tamedHostPureFunction('a.prop = getFeralTestObject();', rec);
  assertEquals(getTamedTestObject(), rec.prop);
  pass('testGuestRecordProperties');
});

jsunitRegister('testGuestConstructedObjectProperties',
               function testGuestConstructedObjectProperties() {
  // Un-taming of guest constructed objects is unsupported.
  var Ctor = function() {
    this.prop = getTamedTestObject();
  };
  var o = new Ctor();
  expectFailure(function() {
    // Pass 'o' into some tamed function in an attempt to untame it.
    tamedApi.tamedHostPureFunction('1;', o);
  });
  pass('testGuestConstructedObjectProperties');
});

jsunitRegister('testGuestConstructedObjectMethods',
               function testGuestConstructedObjectMethods() {
  // Un-taming of guest constructed objects is unsupported.
  // See testGuestConstructedObjectProperties for tests.
  // This test is a placeholder in case we add support for the condition.
  pass('testGuestConstructedObjectMethods');
});

jsunitRegisterIf(
    typeof ArrayBuffer !== 'undefined',
    'testGuestTypedArrays',
    function testGuestTypedArrays() {
  // Note: We haven't written a corresponding guest-to-host test, because the
  // handling of typed arrays is fully symmetric (copyUnmemoized) and has no
  // reason to behave differently.

  var buf = new Uint8Array([1, 2, 3]).buffer;
  tamedApi.tamedHostPureFunction(
      'assertTrue("value " + a, a instanceof frame.imports.ArrayBuffer);', buf);
  tamedApi.tamedHostPureFunction(
      'assertEquals("buffer length", 3, a.byteLength);', buf);
  tamedApi.tamedHostPureFunction(
      'assertEquals("buffer element", 2, new Uint8Array(a)[1]);', buf);

  // Testing array type and also specifically the lack of buffer sharing.
  var a = new Uint8Array([1, 2, 3]);
  var b = new Uint8Array(a.buffer);
  tamedApi.tamedHostPureFunction(
      'assertTrue("array " + a, a instanceof frame.imports.Uint8Array);', a, b);
  tamedApi.tamedHostPureFunction(
      'assertNotEquals("array buffer", a.buffer, b.buffer);', a, b);
  tamedApi.tamedHostPureFunction('a[0] = 99;', a);
  // no persistent mutation and sharing
  tamedApi.tamedHostPureFunction(
      'assertEquals("not mutated", 2, a[0] + b[0]);', a, b);
  assertEquals('guest not mutated', 1, a[0]);

  // DataView
  var array = new Uint8Array([0, 0, 0, 1, 0, 1]);
  var vbuf = array.buffer;
  var view = new DataView(vbuf, 1, 4);
  tamedApi.tamedHostPureFunction(
    'assertTrue("view: " + a, a instanceof frame.imports.DataView);',
    view, vbuf);
  tamedApi.tamedHostPureFunction(
    'assertNotEquals("equality", a.buffer, b);', view, vbuf);
  tamedApi.tamedHostPureFunction('assertEquals("bo", 1, a.byteOffset);', view);
  tamedApi.tamedHostPureFunction('assertEquals("bl", 4, a.byteLength);', view);
  tamedApi.tamedHostPureFunction(
    'assertEquals("value", 256, a.getUint32(0));', view);
  tamedApi.tamedHostPureFunction(
    'a.setUint32(0, 10);', view);
  assertEquals('mutation check 1', 1, array[3]);
  assertEquals('mutation check 2', 0, array[4]);

  pass('testGuestTypedArrays');
});


jsunitRegister('testMembraneViolation',
               function testMembraneViolation() {
  expectFailure(function() {
    return tamedApi.tamedHostPureFunction('a ? "ok" : "absent";',
        getFeralTestObject());
  }, 'feral object on tame side', function(e) {
    return /Feral object found on tame side/.test(e.message);
  });
  expectFailure(function() {
    return tamedApi.tamedHostPureFunction('getTamedTestObject();');
  }, 'tame object on feral side', function(e) {
    return /Tame object found on feral side/.test(e.message);
  });
  pass('testMembraneViolation');
});
