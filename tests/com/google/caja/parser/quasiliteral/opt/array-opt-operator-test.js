/**
 * @fileoverview
 * Supporting JS for ArrayIndexOptimizationTest.java.
 *
 * @author mikesamuel@gmail.com
 */

var nullValue;
var zero;
var negOne;
var posOne;
var truthy;
var untruthy;
var emptyString;
var numberLikeString;
var fooString;
var lengthString;
var anObj;
var sneaky;
var f;

/**
 * Make sure that the given value is a publicly visible array member.
 * @param {function () : *} delayedValue a function that can be executed to
 *     produce the candidate member name.
 */
function requireArrayMember(delayedValue) {
  // Set some variables that are referenced by code generated in
  // ArrayIndexOptimizationTest
  undefined = void 0;
  nullValue = null;
  zero = 0;
  negOne = -1;
  posOne = 1;
  truthy = true;
  untruthy = false;
  emptyString = '';
  numberLikeString = '4';
  fooString = 'foo';
  lengthString = 'length';
  anObj = {};
  sneaky = { valueOf: (function () {
                         return function () { return (++i & 1) ? 4 : 'foo'; }
                       })() };
  f = function () { return 'bar'; };

  var v;
  try {
    v = delayedValue();
  } catch (e) {
    // If operator returns abnormally, it hasn't returned an invalid key.
    return;
  }
  switch (typeof v) {
    case 'number': case 'undefined':
      return;
    case 'string':
      if (v === 'length') { return; }  // White-listed
      if (('' + (+v)) === v) { return; }  // String form of a number
      break;
  }
  fail(v + ' : ' + typeof v + ' from ' + delayedValue);
}
