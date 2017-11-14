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

// Force SES to not abort mid-load so we can test as best we can.
// This severity is too high for any use other than development.
var ses = ses || {};
ses.maxAcceptableSeverityName = 'NEW_SYMPTOM';

// Values we need to obtain or construct before SES is initialized.
var preFrozen = Object.freeze({});
var unsafeFunction = Function;  // See TAME_GLOBAL_EVAL in startSES.js.

var loadSesScript = document.createElement('script');
loadSesScript.src = '../ses/initSES.js';
loadSesScript.onload = function() {
  readyToTest();
  jsunitRun();
};
document.body.appendChild(loadSesScript);

jsunitRegister('testOk', function() {
  assertEquals(false, ses.ok('MAGICAL_UNICORN'));
  assertEquals(true, ses.ok('NEW_SYMPTOM'));

  // Check behavior given invalid severity name
  expectFailure(function() {
    ses.ok('FOO');
  });

  jsunitPass();
});

jsunitRegister('testWeakMap', function() {
  // Based on https://raw.github.com/drses/weak-map/master/verify.js
  var growingMap = new WeakMap();
  var shrinkingMap = new WeakMap();
  var emptyMap = new WeakMap();
  var postFrozen = {};

  assertEquals(growingMap, growingMap.set(postFrozen, 10));
  assertEquals(growingMap, growingMap.set(preFrozen, 11));
  assertEquals(shrinkingMap, shrinkingMap.set(postFrozen, 20));
  assertEquals(shrinkingMap, shrinkingMap.set(preFrozen, 21));

  assertEquals(10, growingMap.get(postFrozen));
  assertEquals(11, growingMap.get(preFrozen));

  Object.freeze(postFrozen);

  assertEquals(20, shrinkingMap.get(postFrozen));
  assertEquals(true, shrinkingMap.has(postFrozen));

  assertEquals(21, shrinkingMap.get(preFrozen));
  assertEquals(true, shrinkingMap.has(preFrozen));

  assertEquals(void 0, emptyMap.get(preFrozen));
  assertEquals(false, emptyMap.has(preFrozen));

  assertEquals(true, shrinkingMap.delete(postFrozen));
  assertEquals(false, shrinkingMap.has(postFrozen));
  assertEquals(false, shrinkingMap.delete(postFrozen));

  assertEquals(true, shrinkingMap.delete(preFrozen));
  assertEquals(false, shrinkingMap.has(preFrozen));
  assertEquals(false, shrinkingMap.delete(preFrozen));

  assertEquals(false, emptyMap.delete(postFrozen));
  assertEquals(false, emptyMap.delete(preFrozen));

  jsunitPass();
});

jsunitRegister('testAtLeastFreeVarNamesOutput', function() {
  // Verify that atLeastFreeVarNames comes up with the maybe-var-names we
  // expect it to.
  assertArrayEquals(
      ['foo', 'bar', 'baz', 'window', '{foo}', '"', 'foo bar', 'u77indow', '$'],
      ses.atLeastFreeVarNames(
          'foo.bar("baz", \\u0077indow, \\u007bfoo\\u007d, \\u0022, ' +
          'foo\\u0020bar, "\167indow", \\u77indow, $)'));
  jsunitPass();
});

jsunitRegister('testAtLeastFreeVarNamesVersusEval', function() {
  // Verify that atLeastFreeVarNames does not disagree with the browser's
  // parser in dangerous ways.

  var names = [
    'window',
    '\\u0077indow',
    '$',
    '\\167indow',
    '\\u007bfoo\\u007d',
    '\\u0022foo\\u0022',
    'foo\\u0020bar',
    '\\u77indow',
  ];

  var safes = [];
  var syntaxErrors = [];
  var nonIdentifiers = [];
  names.forEach(function(name) {
    var r = ses.atLeastFreeVarNames(name);
    if (r.length === 0) {
      // atLeastFreeVarNames recognizes that some non-identifiers
      // ('\\167indow') are not identifiers and omits them from the
      // returned list. Others are conversatively listed, which we
      // test below.
      nonIdentifiers.push(name);
      return;
    }
    assertEquals(name + ' count', 1, r.length);

    // atLeastFreeVarNames's purpose is to return a list of names which should
    // be bound so as to shadow anything unsafe in the environment. Therefore,
    // this test program creates that situation by shadowing the name-as-written
    // with the name as returned by atLeastFreeVarNames.
    var testFunctionBody =
        "var " + name + " = 'unsafe';" +
        "return (function() {" +
          "var " + r[0] + " = 'safe';" +
          "return " + name + ";" +
        "}());";

    // Test with both browser eval (in the form of Function()) and SES eval.
    // Browser eval because we want a direct route to the browser's parser for
    // comparison with atLeastFreeVarNames; SES eval because we want to test the
    // correctness of SES eval.
    [
      {name: 'browser', f: function(body) { return unsafeFunction(body)(); }},
      {name: 'caja', f: cajaVM.eval}
    ].forEach(function (evaluator) {
      var result;
      try {
        // Using SES eval (in the guise of Function) is arguably not quite right
        // here, as we are primarily testing the browser's JS parser, not the
        // lexical lookup behavior of eval, and SES eval has added complexity
        // (rewriting) that _might_ mask a bug.
        //
        // On the other hand, SES eval is what we actually care about the
        // security of, so perhaps it should be changed.
        result = evaluator.f(testFunctionBody);
      } catch (e) {
        if (e instanceof SyntaxError) {
          syntaxErrors.push(name);
          return;
        }
      }
      assertEquals(
          name + ' ' + evaluator.name + ' eval ' + testFunctionBody,
          'safe',
          result);
      safes.push(name);
    });
  });

  // Each name occurs twice because of the two evals tested.
  assertArrayEquals(
      ['window', 'window', '\\u0077indow', '\\u0077indow', '$', '$'], safes);
  assertArrayEquals([
    '\\u007bfoo\\u007d', '\\u007bfoo\\u007d',
    '\\u0022foo\\u0022', '\\u0022foo\\u0022',
    'foo\\u0020bar', 'foo\\u0020bar',
    '\\u77indow', '\\u77indow',
  ], syntaxErrors);
  assertArrayEquals(['\\167indow'], nonIdentifiers);


  jsunitPass();
});

jsunitRegister('testAtLeastFreeVarNamesOnNewUnicodeEscapes', function() {
  // We happen to reject these names now with a SyntaxError even
  // though they are valid JavaScript, merely because we currently use
  // JSON.parse as an temporary expedient. The purpose of this test,
  // therefore, is not really to test that we get this error, but to
  // ensure that these names are not ignored.
  var names = ['\\u{77}indow', '\\u{0077}indow', '\\u{}indow'];
  names.forEach(function(name) {
    try {
      ses.atLeastFreeVarNames(name);
    } catch (e) {
      assertTrue(e instanceof SyntaxError);
      return;
    }
    fail('Unexpectedly succeeded to parse ' + name);
  });
  jsunitPass();
});
