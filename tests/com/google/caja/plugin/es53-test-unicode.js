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
    var api = { result : undefined };
    caja.load(undefined, uriPolicy, function (frame) {
      frame.code('http://test.fake/fake.url', 'text/javascript', code)
        .api({ api: frame.tame(api) })
        .run(function (_) {
          assertEquals(result, api.result);
          jsunitPass(name);
        });
    });
  }

  function basicFailingTest(name, code) {
    var api = {};
    caja.load(createDiv(), uriPolicy, function (frame) {
      // Uses html rather than js since the only way to catch
      // top-level errors thrown by guest code is by providing an
      // onerror available only 
      api.onerror =
        frame.tame(frame.markFunction(function() { jsunitPass(name); }));
      frame.code('http://test.fake/fake.url', 'text/html',
        '<script>' + code + '</script>')
        .api(api)
        .run(function (_) {
          fail();
        });
    });
  }

  registerTest('testUnicode1', function () { basicPassingTest('testUnicode1', 
    // Only ascii
    'api.result = 42;', 42); 
  });
  registerTest('testUnicode2', function () { basicPassingTest('testUnicode2', 
    // Identifier with encoded character
    'var \\u0061 = 43; api.result = a;', 43);
  });
  registerTest('testUnicode3', function () { basicPassingTest('testUnicode3',
    // Reserved keyword as identifier with encoded character
    'var a = {}; a.wit\\u0068 = 44; api.result = a.wit\\u0068;', 44);
  });

  // TODO(jasvir): Re-enable this test in ES5 mode when unicode parsing upstream
  // is supported.  Just this test is commented out rather than marking the entire
  // test with FailureIsAnOption to avoid spurious test failures being masked.
  if (!inES5Mode) {
    registerTest('testUnicode4', function () { basicPassingTest('testUnicode4',
      // Regex containing encoded characters (from jquery)
      ''
      + 'var ID = /#((?:[\\w\\u00c0-\\uFFFF\\-]|\\\\.)+)/;'
      + 'api.result = "#\\u00610b\\u0200{}".match(ID)[1];'
      , "a0b\u0200");
    });
  }
  registerTest('testUnicode5', function () { basicPassingTest('testUnicode5',
    // Identifier with unencoded unicode character
    'var \u0100 = 46; api.result = \u0100;', 46);
  });
  registerTest('testUnicode6', function () { basicPassingTest('testUnicode6',
    // Identifier with unencoded unicode character
    'var \u00A0w = 47; api.result = w;', 47);
  });

  // Encoded spaces are not allowed in identifiers
  registerTest('testUnicode7',
    function testUnicode7() {
      basicFailingTest('testUnicode7',
        'var a\\u2009 = 46; api.result = a\\u2009;');
    
  });

  // Unencoded spaces are not allowed in identifiers
  registerTest('testUnicode8',
    function testUnicode8() {
      basicFailingTest('testUnicode8',
        'var a\u2009 = 46; api.result = a\u2009;');
  });

  // Issue1637 Parse breaks on unicode escapes if they consist of more than 4
  // hex characters 
  registerTest('testUnicode9', function () { basicPassingTest('testUnicode9',
    'api.result = "\u003c123"', "\u003c123");
  });

  readyToTest();
  jsunitRun();
})();
