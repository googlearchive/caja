// Copyright (C) 2012 Google Inc.
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

(function () {
  var uriPolicy = {
    rewrite: function (uri) {
      return uri;
    }
  };

  caja.initialize({
    cajaServer: '/caja',
    debug: true,
    forceES5Mode: inES5Mode
  });
  
  function basicPassingTest(name, code, result) {
    var ipc = { result: 'untouched' };
    caja.load(undefined, uriPolicy, jsunitCallback(function (frame) {
      frame.code('http://test.fake/fake.url', 'text/javascript', code)
        .api({ ipc: frame.tame(ipc) })
        .run(jsunitCallback(function (_) {
          assertEquals(result, ipc.result);
          jsunitPass(name);
        }));
    }));
  }

  function basicFailingTest(name, code) {
    var ipc = { result: 'untouched' };
    var gotError = false;
    caja.load(createDiv(), uriPolicy, jsunitCallback(function (frame) {
      // Uses html rather than js since the only way to catch
      // top-level errors thrown by guest code is by providing an
      // onerror handler
      var onerror =
        frame.tame(frame.markFunction(function() {
          gotError = true;
        }));
      frame.code('http://test.fake/fake.url', 'text/html',
        '<script>' + code + '</script>')
        .api({ ipc: frame.tame(ipc), onerror: onerror })
        .run(jsunitCallback(function (_) {
          assertEquals('untouched', ipc.result);
          assertTrue(gotError);
          jsunitPass(name);
        }));
    }));
  }

  registerTest('testUnicode1', function () { basicPassingTest('testUnicode1', 
    // Only ascii
    'ipc.result = 42;', 42); 
  });
  registerTest('testUnicode2', function () { basicPassingTest('testUnicode2', 
    // Identifier with encoded character
    'var \\u0061 = 43; ipc.result = a;', 43);
  });
  registerTest('testUnicode3', function () { basicPassingTest('testUnicode3',
    // Reserved keyword as identifier with encoded character
    'var a = {}; a.wit\\u0068 = 44; ipc.result = a.wit\\u0068;', 44);
  });

  // TODO(jasvir): Re-enable this test in ES5 mode when unicode parsing upstream
  // is supported.  Just this test is commented out rather than marking the entire
  // test with FailureIsAnOption to avoid spurious test failures being masked.
  if (!inES5Mode) {
    registerTest('testUnicode4', function () { basicPassingTest('testUnicode4',
      // Regex containing encoded characters (from jquery)
      ''
      + 'var ID = /#((?:[\\w\\u00c0-\\uFFFF\\-]|\\\\.)+)/;'
      + 'ipc.result = "#\\u00610b\\u0200{}".match(ID)[1];'
      , "a0b\u0200");
    });
  }
  registerTest('testUnicode5', function () { basicPassingTest('testUnicode5',
    // Identifier with unencoded unicode character
    'var \u0100 = 46; ipc.result = \u0100;', 46);
  });
  registerTest('testUnicode6', function () { basicPassingTest('testUnicode6',
    // Identifier with unencoded unicode character
    'var \u00A0w = 47; ipc.result = w;', 47);
  });

  // Encoded spaces are not allowed in identifiers
  registerTest('testUnicode7',
    function testUnicode7() {
      basicFailingTest('testUnicode7',
        'var a\\u2009z = 48; ipc.result = a\\u2009z;');

  });

  // Unencoded spaces are not allowed in identifiers
  registerTest('testUnicode8',
    function testUnicode8() {
      basicFailingTest('testUnicode8',
        'var a\u2009z = 49; ipc.result = a\u2009z;');
  });

  // Issue1637 Parse breaks on unicode escapes if they consist of more than 4
  // hex characters 
  registerTest('testUnicode9', function () { basicPassingTest('testUnicode9',
    'ipc.result = "\u003c123"', "\u003c123");
  });

  readyToTest();
  jsunitRun();
})();
