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
 * @fileoverview Defines test cases for tracing rewriter.
 *
 * @author ihab.awad@gmail.com
 */

var funcExprRE = /^\s*\(\s*function\s*\(\s*\)\s*\{(.*)\}\s*\)\s*$/;

function stmts(f) {
  return funcExprRE.exec(f.toSource())[1];
}

function t(name, codeToTest, tracingEvents) {
  caja___.addTraceTest(name, stmts(codeToTest), stmts(tracingEvents));
}

function tS(name, codeToTestString, tracingEvents) {
  caja___.addTraceTest(name, codeToTestString, stmts(tracingEvents));
}

function tE(name, codeToTest, type, level) {
  caja___.addCompileErrorTest(name, codeToTest, type, level);
}

////////////////////////////////////////////////////////////////////////////////

t(
  'testMethodCall',
  function () {
    var o = {
      foo: function(x, y) { return x + y; }
    };
    RESULT = o.foo(1, 2);
  },
  function() {
    TRACING.pushFrame((void 0), 'http://example.org/testMethodCall');
    TRACING.pushCallsite('method', (void 0), 'foo');
    TRACING.pushFrame((void 0), '');
    TRACING.popFrame();
    TRACING.popCallsite('return');
    TRACING.popFrame();
  });

t(
  'testFunctionCall',
  function() {
    function foo(x, y) { return x + y; }
    var bar = foo;
    RESULT = bar(1, 2);
  },
  function() {
    TRACING.pushFrame((void 0), 'http://example.org/testFunctionCall');
    TRACING.pushCallsite('function', (void 0), 'bar');
    TRACING.pushFrame((void 0), 'foo');
    TRACING.popFrame();
    TRACING.popCallsite('return');
    TRACING.popFrame();
  });

t(
  'testCtorCall',
  function() {
    function Foo(x, y) { this.z = x + y; }
    var bar = new Foo(42, 17);
    RESULT = bar.z;
  },
  function() {
    TRACING.pushFrame((void 0), 'http://example.org/testCtorCall');
    TRACING.pushCallsite('construct', (void 0), 'Foo');
    TRACING.pushFrame((void 0), 'Foo');
    TRACING.popFrame();
    TRACING.popCallsite('return');
    TRACING.popFrame();
  });

t(
  'testNestedFunctionCall',
  function () {
    function foo(x) { return (x == 0) ? 33 : foo(x - 1); }
    RESULT = foo(1);
  }, function () {
    TRACING.pushFrame((void 0), 'http://example.org/testNestedFunctionCall');
    TRACING.pushCallsite('function', (void 0), 'foo');
    TRACING.pushFrame((void 0), 'foo');
    TRACING.pushCallsite('function', (void 0), 'foo');
    TRACING.pushFrame((void 0), 'foo');
    TRACING.popFrame();
    TRACING.popCallsite('return');
    TRACING.popFrame();
    TRACING.popCallsite('return');
    TRACING.popFrame();
  });

t(
  'testComplexLhsMethodCall',
  function() {
    var o = {
      foo: function theFoo(x) {
        return {
          bar: function theBar(y) {
            return x + y;
          }
        };
      }
    };
    RESULT = o.foo(1).bar(2);
  },
  function() {
    TRACING.pushFrame((void 0), 'http://example.org/testComplexLhsMethodCall');
    TRACING.pushCallsite('method', (void 0), 'foo');
    TRACING.pushFrame((void 0), 'theFoo');
    TRACING.popFrame();
    TRACING.popCallsite('return');
    TRACING.pushCallsite('method', (void 0), 'bar');
    TRACING.pushFrame((void 0), 'theBar');
    TRACING.popFrame();
    TRACING.popCallsite('return');
    TRACING.popFrame();
  });

t(
  'testException',
  function() {
    function foo() {
      try { throw 'foo thrown'; } finally { throw 'zoo thrown'; }
    }
    function bar() {
      foo();
    }
    function baz() {
      try { bar(); } finally { bar(); }
    }
    try { baz(); } catch (e) { RESULT = e; }
  },
  function() {
    TRACING.pushFrame((void 0), 'http://example.org/testException');
    TRACING.pushCallsite('function', (void 0), 'baz');
    TRACING.pushFrame((void 0), 'baz');
    TRACING.pushCallsite('function', (void 0), 'bar');
    TRACING.pushFrame((void 0), 'bar');
    TRACING.pushCallsite('function', (void 0), 'foo');
    TRACING.pushFrame((void 0), 'foo');
    TRACING.popFrame();
    TRACING.popCallsite('exception');
    TRACING.popFrame();
    TRACING.popCallsite('exception');
    TRACING.pushCallsite('function', (void 0), 'bar');
    TRACING.pushFrame((void 0), 'bar');
    TRACING.pushCallsite('function', (void 0), 'foo');
    TRACING.pushFrame((void 0), 'foo');
    TRACING.popFrame();
    TRACING.popCallsite('exception');
    TRACING.popFrame();
    TRACING.popCallsite('exception');
    TRACING.popFrame();
    TRACING.popCallsite('exception');
    TRACING.popFrame();
  });

t(
  'testLog',
  function() {
    function foo() {
      var x = 3, y = 4;
      TRACING.log(x + y);
      return x * y;
    }
    RESULT = foo();
  },
  function() {
    TRACING.pushFrame((void 0), 'http://example.org/testLog');
    TRACING.pushCallsite('function', (void 0), 'foo');
    TRACING.pushFrame((void 0), 'foo');
    TRACING.log('x + y = 7');
    TRACING.popFrame();
    TRACING.popCallsite('return');
    TRACING.popFrame();
  });


tS(
  'testPositions',
  'function foo() {\n' +
  '  return 9;\n' +
  '}\n' +
  'foo();',
  function() {
    TRACING.pushFrame(
        'testPositions:1+1@1',
        'http://example.org/testPositions');
    TRACING.pushCallsite(
        'function',
        'testPositions:4+1@32 - 4@35',
        'foo');
    TRACING.pushFrame(
        'testPositions:1+16@16',
        'foo');
    TRACING.popFrame();
    TRACING.popCallsite('return');
    TRACING.popFrame();
  }
);

tE(
  'tracingVariable',
  function() {
    var x = 3, TRACING = 4;
  },
  'CANNOT_ASSIGN_TO_IDENTIFIER',
  'FATAL_ERROR');

tE(
  'tracingFormalParam',
  function() {
    function foo(TRACING) { }
  },
  'CANNOT_ASSIGN_TO_IDENTIFIER',
  'FATAL_ERROR');

tE(
  'tracingReference',
  function() {
    var x = TRACING;
  },
  'CANNOT_ASSIGN_TO_IDENTIFIER',
  'FATAL_ERROR');
