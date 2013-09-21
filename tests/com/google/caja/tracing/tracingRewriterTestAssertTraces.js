// Copyright (C) 2013 Google Inc.
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

/**
 * @fileoverview Checks that two JSON values, representing program traces, are
 * equal. Implements shortcuts so that test cases do not always have to provide
 * file positions for every piece of the expected values if this is not
 * important for the test case in question; this makes most test cases far less
 * tedious to write and maintain.
 *
 * @author ihab.awad@gmail.com
 */

assertEqualTop(EXPECTED_RESULT, ACTUAL_RESULT);
assertEqualTop(EXPECTED_TRACE, ACTUAL_TRACE);

////////////////////////////////////////////////////////////////////////////////

function stringify(o) {
  // Exclude 'parent' pointers since these create cycles.
  return JSON.stringify(o, function(k, v) {
    if (k === 'parent' || k === 'result' || k === 'level') { return undefined; }
    return v;
  });
}

function fail(expected, actual, message) {
  throw new Error(
    stringify(expected) + ' !== ' +
    stringify(actual));
}

function assertArraysEqual(expected, actual) {
  if (expected.length !== actual.length) {
    throw new Error(expected, actual, "array lengths mismatched");
  }
  for (var i = 0; i < expected.length; i++) {
    assertEqual(expected[i], actual[i]);
  }
}

function assertObjectsEqual(expected, actual) {
  // Merge keys of expected and actual to ensure we check them all.
  var allKeys = {};
  function add(k) { allKeys[k] = 1; }
  Object.getOwnPropertyNames(expected).forEacy(add);
  Object.getOwnPropertyNames(actual).forEach(add);
  delete allKeys.pos;
  delete allKeys.parent;
  Object.getOwnPropertyNames(allKeys).map(function(k) {
    assertEqual(expected[k], actual[k]);
  });
}

function assertPrimitivesEqual(expected, actual) {
  if (expected !== actual) {
    fail(expected, actual, "value mismatch");
  }
}

function assertEqual(expected, actual) {
  if (expected === void 0 || actual === void 0) {
    assertPrimitivesEqual(expected, actual);
  } else if ((typeof expected === 'string') && (typeof actual === 'string')) {
    assertPrimitivesEqual(expected, actual);
  } else if ((typeof expected === 'number') && (typeof actual === 'number')) {
    assertPrimitivesEqual(expected, actual);
  } else if (expected.length && actual.length) {
    assertArraysEqual(expected, actual);
  } else {
    assertObjectsEqual(expected, actual);
  }
}

function assertEqualTop(expected, actual) {
  try {
    assertEqual(expected, actual);
  } catch (e) {
    // Allow JUnit development tools to show a string diff of the
    // two JSON strings for comparison.
    _junit_.assertJsonEquals(
        e.message,
        stringify(expected),
        stringify(actual));
  }
}