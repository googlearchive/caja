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

// define a class to protect
function Cap(x, y) {
  this.x = x;
  this.y = y;
}
Cap.prototype.getX = function () { return this.x; };
Cap.prototype.setX = function (x) { this.x = x; };
Cap.prototype.getY = function () { return this.y; };
Cap.prototype.setY = function (y) { this.y = y; };


if (!('fail' in this)) {  // in case we're run without jsunit
  function fail(msg) { throw ('Error' in window ? new Error(msg) : msg); }
  function assertEquals(msg, a, b) {
    if (arguments.length === 2) { b = a; a = msg; }
    if (a !== b) {
      fail(msg + ' : <' + a + '>:' + (typeof a) + ' != <' + b + '>:' +
           (typeof b));
    }
  }
}


function testProtect() {
  var isSafari = 'navigator' in this && /Safari/.test(navigator.userAgent);

  // Wrap class in a protected class
  // This should normally be kept very very secret.
  // Do not define in a globally accessible scope.
  var token = {};
  var constructorAndUnlockFn = protect(Cap, token, ['setX', 'getX', 'getY']);
  var ProtectedCap = constructorAndUnlockFn[0];
  var unlock = constructorAndUnlockFn[1];
  // Untrusted code should not have access to the above.  See the Protect
  // methods documentation for ways to achieve this.

  // Create 2 protected instances
  var inst1 = new ProtectedCap(token, 1, 2);
  var inst2 = new ProtectedCap(token, 3, 4);
  // Check that different instances have different state
  assertEquals('x1', 1, inst1.getX());
  assertEquals('x2', 3, inst2.getX());
  assertEquals('y1', 2, inst1.getY());
  assertEquals('y2', 4, inst2.getY());
  // Change x
  inst1.setX(5);
  inst2.setX(6);
  // Only x changed
  assertEquals("x1'", 5, inst1.getX());
  assertEquals("x2'", 6, inst2.getX());
  assertEquals('y1', 2, inst1.getY());
  assertEquals('y2', 4, inst2.getY());
  // setY is not available so call fails
  try { inst1.setY(7); fail("y1'"); } catch (e) {}
  try { inst2.setY(8); fail("y2'"); } catch (e) {}
  // and state not modified
  assertEquals("x1'", 5, inst1.getX());
  assertEquals("x2'", 6, inst2.getX());
  assertEquals('y1', 2, inst1.getY());
  assertEquals('y2', 4, inst2.getY());
  // Try to access hiddens
  assertEquals('underlying', undefined, inst1.underlying);
  assertEquals('__underlying__', undefined, inst1.__underlying__({}));
  assertEquals('token', undefined, inst1.token);

  // Check for replay attacks on __underlying__
  function assertNotSensitive(o) {
    if (o == token) {
      fail('token exposed to untrusted code');
    } else if (o instanceof Cap) {
      fail('protected value exposed to untrusted code');
    }
  }
  var origUnderlying = inst1.__underlying__;
  inst1.__underlying__ = function () {
    var result = origUnderlying.apply(this, arguments);
    assertNotSensitive(result);
    for (var i = arguments.length; --i >= 0;) {
      assertNotSensitive(arguemnts[i]);
    }
    return result;
  };
  unlock(inst1);
  // restore the original instance
  inst1.__underlying__ = origUnderlying;

  // Replacing a method does not affect other instances
  inst1.getX = function () { return -1; };
  assertEquals("x1''", -1, inst1.getX());
  assertEquals("x2''", 6, inst2.getX());
  assertEquals('y1', 2, inst1.getY());
  assertEquals('y2', 4, inst2.getY());
  // Assume prototype cannot be changed which should be enforced by source
  // rewriting.
  // Otherwise getY changes would be reflected in inst2 which could cause
  // code to be executed with someone else's privileges.
  // Either way, make sure they're not seen in protected mode, which could
  // cause code to be executed as privileged.
  inst1.constructor.prototype.getY = function () { return -2; };
  assertEquals('protected y1', 2, unlock(inst1).getY());
  assertEquals('protected y2', 4, unlock(inst2).getY());
  delete inst1.constructor.prototype.getY
  // Make sure that untrusted code can't spoof a capability.
  var spoof = undefined;
  try {
    spoof = new (inst2.constructor)(1, 2);
    fail('spoofing worked 1');
  } catch (e) {
    // On Safari 2, inst1.constructor is Object, not the actual constructor.
    if (!isSafari) {
      assertEquals('spoofing 1', undefined, spoof);
    } else {  // make sure it is a blank object, it has no members
      for (var k in spoof) {
        fail('spoofing 1 ' + k);
      }
    }
  }
  try {
    spoof = new (inst2.constructor)(null, 1, 2);  // no token
    fail('spoofing worked 2');
  } catch (e) {
    if (!isSafari) {
      assertEquals('spoofing 2', undefined, spoof);
    } else {  // make sure it is a blank object, it has no members
      for (var k in spoof) {
        fail('spoofing 2' + k);
      }
    }
  }
  try {
    spoof = new (inst2.constructor)({}, 1, 2);  // a bogus token
    fail('spoofing worked 3');
  } catch (e) {
    if (!isSafari) {
      assertEquals('spoofing 3', undefined, spoof);
    } else {  // make sure it is a blank object, it has no members
      for (var k in spoof) {
        fail('spoofing 3 ' + k);
      }
    }
  }
}

// TODO(msamuel): add a testcase for the exploit where __underlying__ is
// replaced with something that throws an exception for one object, and then a
// second object is unlocked whose __underlying__ is a no-op.
// Under no circumstances should the first object's backing instance be returned
// for the call to unwrap that takes the second object.
