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

/**
 * @fileoverview Tests of different ways to invoke the 'caja.js' API to
 * create an ES53 frame.
 *
 * @author ihab.awad@gmail.com
 * @requires caja, jsunitRun, readyToTest
 */

(function () {
  function splitHtmlAndScript(combinedHtml) {
    return combinedHtml.match(
      /^([\s\S]*?)<script[^>]*>([\s\S]*?)<\/script>\s*$/)
      .slice(1);
  }

  function fetch(url, cb) {
    var xhr = bridalMaker(function (x){return x;}, document).makeXhr();
    xhr.open('GET', url, true);
    xhr.onreadystatechange = function() {
      if (xhr.readyState === 4) {
        if (xhr.status === 200) {
          cb(xhr.responseText);
        } else {
          throw new Error('Failed to load ' + url + ' : ' + xhr.status);
        }
      }
    };
    xhr.send(null);
  }

  /**
   * Assert that a cajoled and loaded es53-test-guest.js has the right 
   * results.
   */
  function assertGuestJsCorrect(frame, div, result) {
    // TODO(kpreid): reenable or declare completion value unsupported
    if (!inES5Mode) {
      assertEquals(12, result);
    } else {
      console.warn('JS completion value not yet supported by ES5 mode; '
          + 'not testing.');
    }
  }

  /**
   * Assert that a cajoled and loaded es53-test-guest.html has the right 
   * results.
   */
  function assertGuestHtmlCorrect(frame, div) {
    assertStringContains('static html', div.innerHTML);
    assertStringContains('edited html', div.innerHTML);
    assertStringContains('dynamic html', div.innerHTML);
    if (!inES5Mode) {
      // TODO(kpreid): Reenable after CSS works in ES5 mode
      assertEquals('small-caps',
          document.defaultView.getComputedStyle(
              document.getElementById('foo-' + frame.idSuffix),
              null).fontVariant);
    } else {
      console.warn('CSS not yet supported by ES5; not testing.');
    }
  }


  // NOTE: Identity URI rewriter (as shown below) is for testing only; this
  // would be unsafe for production code!
  var uriCallback = {
    rewrite: function (uri) { return uri; }
  };

  caja.initialize({
    cajaServer: 'http://localhost:8000/caja',
    debug: true,
    forceES5Mode: inES5Mode
  });

  registerTest('testReinitialization', function testReinitialization() {
    try {
      caja.initialize({
        cajaServer: 'http://localhost:8000/caja',
        debug: true
      });
    } catch (e) {
      assertStringContains('Caja cannot be initialized more than once', 
          String(e));
      jsunitPass('testReinitialization');
    }
  });

  // TODO(kpreid): Enable for ES5 once HTML scripting works
  if (!inES5Mode)
  registerTest('testBuilderApiHtml', function testBuilderApiHtml() {
    var div = createDiv();
    caja.load(div, uriCallback, function (frame) {
      frame.code('es53-test-guest.html', 'text/html')
           .run(function(result) {
              assertGuestHtmlCorrect(frame, div);
              jsunitPass('testBuilderApiHtml');
           });
    });
  });

  registerTest('testBuilderApiJs', function testBuilderApiJs() {
    var div = createDiv();
    caja.load(div, uriCallback, function (frame) {
      var extraImports = { x: 4, y: 3 };
      frame.code('es53-test-guest.js', 'text/javascript')
           .api(extraImports)
           .run(function(result) {
             assertGuestJsCorrect(frame, div, result);
             jsunitPass('testBuilderApiJs');
           });
    });
  });

  registerTest('testBuilderApiJsNoDom', function testBuilderApiJsNoDom() {
    caja.load(undefined, uriCallback, function (frame) {
      var extraImports = { x: 4, y: 3 };
      frame.code('es53-test-guest.js', 'text/javascript')
           .api(extraImports)
           .run(function(result) {
             assertGuestJsCorrect(frame, undefined, result);
             jsunitPass('testBuilderApiJsNoDom');
           });
    });
  });

  registerTest('testBuilderApiNetUndefined', 
      function testBuilderApiNetUndefined() {
    var div = createDiv();
    caja.load(div, undefined, function (frame) {
      frame.code('http://localhost:8080/', 'text/html',
          '<a href="http://fake1.url/foo">fake1</a>' + 
          '<a href="http://fake2.url/foo">fake2</a>'
          )
        .run(function (result) {
          assertStringDoesNotContain('http://fake1.url/foo', div.innerHTML);
          assertStringDoesNotContain('http://fake2.url/foo', div.innerHTML);
          jsunitPass('testBuilderApiNetUndefined');
        });
    });
  });

  registerTest('testBuilderApiNetNone', function testBuilderApiNetNone() {
    var div = createDiv();
    caja.load(div, caja.policy.net.NO_NETWORK, function (frame) {
      frame.code('http://localhost:8080/', 'text/html',
          '<a href="http://fake1.url/foo">fake1</a>' + 
          '<a href="http://fake2.url/foo">fake2</a>'
          )
        .run(function (result) {
          assertStringDoesNotContain('http://fake1.url/foo', div.innerHTML);
          assertStringDoesNotContain('http://fake2.url/foo', div.innerHTML);
          jsunitPass('testBuilderApiNetNone');
        });
    });
  });

  registerTest('testBuilderApiNetAll', function testBuilderApiNetAll() {
    var div = createDiv();
    caja.load(div, caja.policy.net.ALL, function (frame) {
      frame.code('http://localhost:8080/', 'text/html',
          '<a href="http://fake1.url/foo">fake1</a>' + 
          '<a href="http://fake2.url/foo">fake2</a>'
          )
        .run(function (result) {
          assertStringContains('http://fake1.url/foo', div.innerHTML);
          assertStringContains('http://fake2.url/foo', div.innerHTML);
          jsunitPass('testBuilderApiNetAll');
        });
    });
  });

  registerTest('testBuilderApiNetHost', function testBuilderApiNetHost() {
    var div = createDiv();
    caja.load(div,
        caja.policy.net.only("http://fake1.url/foo"), function (frame) {
      frame.code('http://localhost:8080/', 'text/html',
          '<a href="http://fake1.url/foo">fake1</a>' + 
          '<a href="http://fake2.url/foo">fake2</a>' 
          )
        .run(function (result) {
          assertStringContains('http://fake1.url/foo', div.innerHTML);
          assertStringDoesNotContain('http://fake2.url/foo', div.innerHTML);
          jsunitPass('testBuilderApiNetHost');
        });
    });
  });

  // TODO(kpreid): Enable for ES5 once HTML scripting works
  if (!inES5Mode)
  registerTest('testBuilderApiContentHtml',
      function testBuilderApiContentHtml() {
    var div = createDiv();
    caja.load(div, uriCallback, function (frame) {
        fetch('es53-test-guest.html', function(resp) {
          frame.code('http://localhost:8080/', 'text/html', resp)
            .run(function (result) {
              assertGuestHtmlCorrect(frame, div);
              jsunitPass('testBuilderApiContentHtml');
            });
        });
    });
  });

  registerTest('testBuilderApiContentJs', function testBuilderApiContentJs() {
    var div = createDiv();
    caja.load(div, uriCallback, function (frame) {
      var extraImports = { x: 4, y: 3 };
      fetch('es53-test-guest.js', function(resp) {
        frame.code('http://localhost:8080/', 'application/javascript', resp)
             .api(extraImports)
             .run(function (result) {
               assertGuestJsCorrect(frame, div, result);
               jsunitPass('testBuilderApiContentJs');
             });
      });
    });
  });

  if (!inES5Mode)
  registerTest('testBuilderApiContentCajoledHtml',
      function testBuilderApiContentCajoledHtml() {
    var div = createDiv();
    caja.load(div, uriCallback, function (frame) {
      fetch('es53-test-guest.out.html', function(resp) {
        var htmlAndJs = splitHtmlAndScript(resp);

        frame.cajoled('http://localhost:8080/', htmlAndJs[1], htmlAndJs[0])
          .run(function (result) {
            assertGuestHtmlCorrect(frame, div);
            jsunitPass('testBuilderApiContentCajoledHtml');
          });
      });
    });
  });

  if (!inES5Mode)
  registerTest('testBuilderApiContentCajoledJs',
      function testBuilderApiContentCajoledJs() {
    var div = createDiv();
    caja.load(div, uriCallback, function (frame) {
      var extraImports = { x: 4, y: 3 };
      fetch('es53-test-guest.out.js', function(script) {
        frame.cajoled(undefined, script, undefined)
             .api(extraImports)
             .run(function (result) {
               assertGuestJsCorrect(frame, div, result);
               jsunitPass('testBuilderApiContentCajoledJs');
             });
      });
    });
  });

  caja.configure({
    cajaServer: 'http://localhost:8000/caja',
    debug: true,
    forceES5Mode: inES5Mode
  }, function (frameGroup) {

    // TODO(ihab.awad): Test 'base url' functionality, esp. for "content" cases
    if (!inES5Mode)
    registerTest('testContentCajoledHtml', function testContentCajoledHtml() {
      fetch('es53-test-guest.out.html', function(resp) {
        var htmlAndScript = splitHtmlAndScript(resp);
        var div = createDiv();
        frameGroup.makeES5Frame(div, uriCallback, function (frame) {
          frame.contentCajoled('http://localhost:8080/',
                               htmlAndScript[1], htmlAndScript[0])
               .run({}, function (result) {
            assertGuestHtmlCorrect(frame, div);
            jsunitPass('testContentCajoledHtml');
          });
        });
      });
    });

    if (!inES5Mode)
    registerTest('testContentCajoledJs', function testContentCajoledJs() {
      fetch('es53-test-guest.out.js', function(script) {
        frameGroup.makeES5Frame(undefined, uriCallback, function (frame) {
          var extraImports = { x: 4, y: 3 };
          frame.contentCajoled(undefined, script, undefined)
               .run(extraImports, function (result) {
            assertGuestJsCorrect(frame, undefined, result);
            jsunitPass('testContentCajoledJs');
          });
        });
      });
    });

    if (!inES5Mode)
    registerTest('testNoImports', function testNoImports() {
      fetch('es53-test-guest.out.html', function(resp) {
        var htmlAndScript = splitHtmlAndScript(resp);
        var div = createDiv();
        frameGroup.makeES5Frame(div, uriCallback, function (frame) {
          frame.contentCajoled('http://localhost:8080/',
                               htmlAndScript[1], htmlAndScript[0])
               .run(undefined, function (result) {
            assertGuestHtmlCorrect(frame, div);
            jsunitPass('testNoImports');
          });
        });
      });
    });

    // TODO(ihab.awad): Implement 'urlCajoled' case and enable the below.
    // registerTest('testUrlCajoledHtml', function testUrlCajoledHtml() { });
    // registerTest('testUrlCajoledJs', function testUrlCajoledJs() { });

    // TODO(kpreid): Enable for ES5 once HTML scripting works
    if (!inES5Mode)
    registerTest('testContentHtml', function testContentHtml() {
      fetch('es53-test-guest.html', function(resp) {
        var div = createDiv();
        frameGroup.makeES5Frame(div, uriCallback, function (frame) {
          frame.content('http://localhost:8080/', resp, 'text/html')
              .run({}, function (result) {
            assertGuestHtmlCorrect(frame, div);
            jsunitPass('testContentHtml');
          });
        });
      });
    });

    registerTest('testContentJs', function testContentJs() {
      fetch('es53-test-guest.js', function(resp) {
        frameGroup.makeES5Frame(undefined, uriCallback, function (frame) {
          var extraImports = { x: 4, y: 3 };
          frame.content('http://localhost:8080/',
                        resp,
                        'application/javascript')
              .run(extraImports, function (result) {
            assertGuestJsCorrect(frame, undefined, result);
            jsunitPass('testContentJs');
          });
        });
      });
    });

    // TODO(kpreid): Enable for ES5 once HTML scripting works
    if (!inES5Mode)
    registerTest('testUrlHtml', function testUrlHtml() {
      var div = createDiv();
      frameGroup.makeES5Frame(div, uriCallback, function (frame) {
        frame.url('es53-test-guest.html').run({}, function (result) {
          assertGuestHtmlCorrect(frame, div);
          jsunitPass('testUrlHtml');
        });
      });
    });
    
    registerTest('testUrlJs', function testUrlJs() {
      frameGroup.makeES5Frame(undefined, uriCallback, function (frame) {
        var extraImports = { x: 4, y: 3 };
        frame.url('es53-test-guest.js').run(extraImports, function (result) {
          assertGuestJsCorrect(frame, undefined, result);
          jsunitPass('testUrlJs');
        });
      });
    });

    // TODO(kpreid): Enable for ES5 once HTML scripting works
    if (!inES5Mode)
    registerTest('testUrlHtmlWithMimeType', function testUrlHtml() {
      var div = createDiv();
      frameGroup.makeES5Frame(div, uriCallback, function (frame) {
        frame.url('es53-test-guest.html', 'text/html').run({},
            function (result) {
          assertGuestHtmlCorrect(frame, div);
          jsunitPass('testUrlHtmlWithMimeType');
        });
      });
    });
    readyToTest();
    jsunitRun();
  });

})();
