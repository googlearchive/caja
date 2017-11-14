/// Copyright (c) 2010 Google Inc. 
/// 
/// Redistribution and use in source and binary forms, with or without modification, are permitted provided
/// that the following conditions are met: 
///    * Redistributions of source code must retain the above copyright notice, this list of conditions and
///      the following disclaimer. 
///    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and 
///      the following disclaimer in the documentation and/or other materials provided with the distribution.  
///    * Neither the name of Google Inc. nor the names of its contributors may be used to
///      endorse or promote products derived from this software without specific prior written permission.
/// 
/// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
/// IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
/// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
/// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
/// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
/// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
/// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
/// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

// a shell-based testrunner, based on the ES5 Conformance test suite
// http://es5conform.codeplex.com

// Patched for Caja: required for function proxies
Object.freeze(Function.prototype);

this.ES5Harness = (function () {
  var tests = [];

  function report(prefix, test, optMessage) {
    print(prefix + ' '+test.id+' ' + (optMessage ? '('+optMessage+')' : ''));
  };

  // copied from ES5Conform/SimpleTestHarness/sth.js with slight modifications
  var cachedGlobal = this; // 'this' assumed to be the global object
  var globalState = {
    undefined: cachedGlobal.undefined,
    NaN: cachedGlobal.NaN,
    Infinity: cachedGlobal.Infinity,
    Object: cachedGlobal.Object,
    Array: cachedGlobal.Array,
    Function: cachedGlobal.Function,
    String: cachedGlobal.String,
    Number: cachedGlobal.Number,
    Boolean: cachedGlobal.Boolean,
    RegExp: cachedGlobal.RegExp,
    Math: cachedGlobal.Math,
    Error: cachedGlobal.Error,
    eval: cachedGlobal.eval,
    parseInt: cachedGlobal.parseInt,
    parseFloat: cachedGlobal.parseFloat,
    isNaN: cachedGlobal.isNaN,
    isFinite: cachedGlobal.isFinite,
    EvalError: cachedGlobal.EvalError,
    RangeError: cachedGlobal.RangeError,
    ReferenceError: cachedGlobal.ReferenceError,
    SyntaxError: cachedGlobal.SyntaxError,
    TypeError: cachedGlobal.TypeError,
    URIError: cachedGlobal.URIError
  };
  function restoreGlobals() {
    for (var prop in globalState) {
      if (globalState.hasOwnProperty(prop) && cachedGlobal[prop] !== globalState[prop]) {
        cachedGlobal[prop] = globalState[prop];
      }
    }
  }

  return {
        
    loadTest: function(filename) {
      try {
        load(filename);
      } catch (e) {
        restoreGlobals();
        // register a mock test that will fail with the early error
        this.registerTest({
          id: filename,
          path: filename,
          description: 'early error in '+filename,
          test: function() { throw e; }
        });
      }
    },
    
    registerTest: function(test) {
      tests.push(test);
    },
    
    // if verbose set to true, print both passed and failed tests
    // if verbose set to false, print only failed tests
    startTesting: function(verbose) {
      var passed  = 0;
      var failed  = 0;
      var error   = 0;
      var skipped = 0;
      var test;
      for (var i = 0; i < tests.length; i++) {
        test = tests[i];
        restoreGlobals();
        try {
          if (test.precondition && !test.precondition()) {
            skipped++;
            report('[skip]', test);
            continue;
          }
          if (test.test()) {
            passed++;
            if (verbose) { report('[pass]', test); }
          } else {
            failed++;
            report('[fail]', test);
          }
        } catch(e) {
          if (e instanceof TestFailure) {
            failed++
            report('[fail]', test, e.msg);
          } else {
            error++;
            report('[ err]', test, ''+ e);            
          }
        }
      }
      print('[done] error: '+ error   +
                  '  fail: '+ failed  +
                  '  skip: '+ skipped +
                  '  pass: '+ passed  +
                 '  total: '+ tests.length);
    }
  }
})();

// ----------------------------------------------
// Copied from ES5Conform/SimpleTestHarness/sth.js

// ----------------------------------------------
// helpers that unittests can use (typically in
// their prereq function).
// ----------------------------------------------
function fnExists(f) {
  if (typeof(f) === "function") {
    return true;
  }
}

var supportsStrict = undefined;
function fnSupportsStrict() {
   "use strict";
   if (supportsStrict!==undefined) return supportsStrict;
   try {eval('with ({}) {}'); supportsStrict=false;} catch (e) {supportsStrict=true;};     
   return supportsStrict;
  }

function fnGlobalObject() {
  return (function () {return this}).call(null);
  }


function compareArray(aExpected, aActual) {
  if (aActual.length != aExpected.length) {
    return false;
  }

  aExpected.sort();
  aActual.sort();

  var s;
  for (var i = 0; i < aExpected.length; i++) {
    if (aActual[i] != aExpected[i]) {
      return false;
    }
  }
  
  return true;
}


// ----------------------------------------------
// Additional general-purpose testing utilities
// provided by the headless test runner

// deep-compares two arrays, checking if they have the
// same structure. Non-array elements are tested for
// equality using ===
function sameStructure(aExpected, aActual) {
  if (aActual.length != aExpected.length) {
    return false;
  }
  for (var i = 0; i < aExpected.length; i++) {
    if (aActual[i] !== aExpected[i]) {
      if (aActual[i] instanceof Array &&
          aExpected[i] instanceof Array &&
          sameStructure(aExpected[i], aActual[i])) {
          continue;
      }
      return false;
    }
  }
  return true;
}

function TestFailure(msg) {
  this.msg = msg;
}

function fail(msg) {
  throw new TestFailure(msg);
}

function assertEq(what, expected, got) {
  if (expected !== got) {
	  fail(what + ": expected: " + expected + ", got: " + got);
  }
}

function assert(what, cond) {
  if (!cond) { fail(what + " failed"); };
}

function assertMatches(what, regexp, input) {
  if (!(typeof input === "string" && input.match(regexp)!==undefined)) {
    fail(what + ':'+input+'failed to match '+regexp);
  }
}

function assertThrows(what, type, fun) {
  var success = false;
  try {
    fun();
    success = true;
  } catch (e) {
    if (!(e instanceof type)) {
      throw e;
    }
  }
  if (success) fail(what);
}