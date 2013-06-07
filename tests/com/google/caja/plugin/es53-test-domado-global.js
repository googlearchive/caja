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

/**
 * @fileoverview Tests which require a complete document to be loaded for each.
 *
 * @author kpreid@switchb.org
 * @requires caja, jsunitRun, readyToTest
 */

(function () {
  caja.initialize({
    cajaServer: '/caja',
    debug: true,
    forceES5Mode: inES5Mode
  });

  function registerGlobalTest(cond, testName, url, html, callback) {
    jsunitRegisterIf(cond, testName, function globalTestWrapper() {
      var div = createDiv();
      // Load callback is NOT a jsunitCallback because that would be mostly
      // noise.
      caja.load(div, undefined, function(frame) {
        frame.code(url, 'text/html', html)
          .run(createExtraImportsForTesting(caja, frame),
              jsunitCallback(function() {
                callback(frame);
              }, testName, frame));
      });
    });
  }

  function registerGuestTest(testName, html, varargs) {
    var guestTestArgs = Array.prototype.slice.call(arguments, 2);
    registerGlobalTest(
        true,
        testName,
        location.protocol + '//' + location.host + '/',
        html,
        function(frame) {
          // Domado delays onload handlers with setTimeout(,0),
          // so we have to delay globalGuestTest to make sure
          // it's run after all the onloads fire.
          window.setTimeout(function() {
            var tameGT = frame.imports.globalGuestTest;
            assertEquals('typeof globalGuestTest', 'function',
                typeof tameGT);
            frame.untame(tameGT).apply(undefined, guestTestArgs);
            jsunitPass(testName);
          }, 0);
        });
  }

  function fetches(callback) {
    fetch('es53-test-domado-global-html-guest.js', function(htmlGuestJs) {
      fetch('es53-test-domado-global-location.js', function(locationJs) {
        callback(htmlGuestJs, locationJs);
      });
    });
  }

  fetches(function(htmlGuestJs, locationJs) {
    /**
     * Tests of how Caja handles omitted structure in HTML document inputs.
     */
    (function() {
      
      function ti(text) {
        return '<title>' + text + '</title>';
      }

      function registerStructureTest(testName, html, expectHead, expectBody) {
        var expectHtml = '<head>' + expectHead + '</head><body>' + expectBody +
            '</body>';
        registerGuestTest(testName,
            html.replace('$', '<script>' + htmlGuestJs + '</script>'),
            expectHtml.replace('$',
                inES5Mode ? '<script>caja_dynamic_script0___();</script>'
                          : ''));
      }

      registerStructureTest('testFullyExplicit',
          '<html><head><title>t</title></head>' + 
          '<body>b$</body></html>',
          ti('t'), 'b$');

      registerStructureTest('testStartBody',
          '<title>t</title><body>' + 
          'b$',
          ti('t'), 'b$');

      registerStructureTest('testStopHead',
          '<title>t</title></head>' + 
          'b$',
          ti('t'), 'b$');

      registerStructureTest('testFullyImplicit',
          '<title>t</title>' + 
          'b$',
          ti('t'), 'b$');

      registerStructureTest('testJustText',
          'b$',
          '', 'b$');

      // Test that a completely empty document still produces structure.
      registerGlobalTest(true, 'testEmptyInput',
          location.protocol + '//' + location.host + '/',
          '',
          function(frame) {
            var guestHtml = ('<html><head></head><body></body></html>'
                .replace(/<\/?/g, function(m) { return m + 'caja-v-'; }));
            assertEquals(guestHtml, frame.innerContainer.innerHTML);
            jsunitPass('testEmptyInput');
          });

      registerStructureTest('testEmptyVirtualizedElementInHead',
          // Regression test for <body> getting embedded in <head> due to
          // virtualized empty elements being misparsed as non-empty elements
          '<html><head><title>t</title><meta></head>' +
              '<body>b$</body></html>',
          ti('t') + '<meta>',
          'b$');

    }());

    // Is the expected structure generated if a <script> element occurs without
    // any preceding <html> or <head>?
    if (inES5Mode)
    registerGuestTest('testScriptAsFirstThing',
        '<script>' +
        'assertEvaluatesToTrue("documentElement exists",' +
        '    document.documentElement);' +
        'assertEquals("structure",' +
        '    "<head><script ELIDED/></head>",' +
        '    document.documentElement.innerHTML.replace(' +
        '        /<scr()ipt>.*<\\/scr()ipt>/, "<script ELIDED/>"));' +
        'window.globalGuestTest = function() {};' +
        '</script>');

    /**
      * Tests of global structure-referencing property behavior.
      */
    (function () {
      // Behavior of document.title without any title element present
      registerGuestTest('testNoElementTitleProp',
          '<body>' + // TODO(kpreid): That this is required is a HtmlEmitter bug
          '<script>window.globalGuestTest =' +
          '    function globalGuestTest() {' +
          '  assertEquals("head before get", "", ' +
          '      document.getElementsByTagName("head")[0].innerHTML);' +
          '  assertEquals("title before set", "", document.title);' +
          '  assertEquals("head after get", "", ' +
          '      document.getElementsByTagName("head")[0].innerHTML);' +
          '  document.title = "t";' +
          '  assertEquals("title after set", "t", document.title);' +
          '  assertEquals("head after set", "<title>t</title>", ' +
          '      document.getElementsByTagName("head")[0].innerHTML);' +
          '};</script>');
    
      // Behavior of document.body without any *document* element present.
      registerGuestTest('testNoElementBodyProp',
          '<body>old body' +
          '<script>window.globalGuestTest =' +
          '    function globalGuestTest() {' +
          '  document.removeChild(document.documentElement);' +
          '  assertTrue("null", null === document.body);' +
          '  var newBody = document.createElement("body");' +
          '  expectFailure(function() { document.body = newBody; });' +
          '  document.appendChild(document.createElement("html"));' +
          '  document.body = newBody;' +
          '  assertEquals(document.documentElement.innerHTML, ' +
          '    "<body></body>");' +
          '  assertTrue("final body", document.body === newBody);' +
          '};</script>');
    })();

    /**
     * Tests of onload handlers, which are a special case because on* attributes
     * on <body> are actually aliases for event handlers of the _window_ object,
     * so since our virtual window object is not backed by anything we have to
     * implement that behavior ourselves.
     */
    (function () {
      registerGuestTest('testOnloadProp',
          '<body>' + // TODO(kpreid): That this is required is a HtmlEmitter bug
          '<script>' +
          'window.onload = function () { ' + 
          '  window.testresult = (window.testresult || 0)+1; };' +
          'window.globalGuestTest = function globalGuestTest() {' +
          '  assertEquals(1, window.testresult);' +
          '  assertEquals(null, document.body.getAttribute("onload"));' +
          '}</script>');

      registerGuestTest('testOnloadAttr',
          '<script>window.ding = function ding() {' + 
          '  window.testresult = (window.testresult || 0)+1;' +
          '}</script>' +
          '<body onload="ding()">' +
          '<script>window.globalGuestTest =' + 
          '    function globalGuestTest(inES5Mode) {' +
          '  assertEquals(1, window.testresult);' +
          // TODO(kpreid): Rewriter transforms an onload listener into an
          // after-the-body script, so the window.onload property never gets
          // set. Change the rewriter so that it generates
          //    <caja-v-body onload="...">
          //       <script>window.onload = function () { ... };</script>
          // instead. (That is, the string value stays as an attribute and
          // the code is also cajoled.) Then remove this inES5Mode conditional.
          '  if (inES5Mode) {' +
          '    window.onload();' +
          '    assertEquals(2, window.testresult);' +
          '  }' +
          '}</script>',
          inES5Mode);

      registerGuestTest('testOnloadSetAttr',
          '<script>window.ding = function ding() {' + 
          '  window.testresult = (window.testresult || 0)+1;' +
          '}</script>' +
          '<body>' +
          '<script>' + 
          'document.body.setAttribute("onload", "ding()");' +
          'window.globalGuestTest = function globalGuestTest() {' +
          '  assertEquals(1, window.testresult);' +
          '  window.onload();' +
          '  assertEquals(2, window.testresult);' +
          '}</script>');
    })();

    /**
     * Test document.write on finished document.
     */
    (function () {
      // TODO(kpreid): Make this sensitive to Issue 1596 once that is fixed.
      registerGuestTest('testDocumentWriteAfterward',
          'hello world' +
          '<script>' +
          'window.globalGuestTest = function globalGuestTest() {' +
          '  document.write(\'goodbye world\');' +
          '  document.close();' +
          '  assertEquals("goodbye world", ' +
          '      document.documentElement.textContent);' +
          '}</script>');
    })();

    /**
     * Test mutation of global structure.
     */
    (function () {
      registerGuestTest('testGlobalMutation',
          '<body>testGlobalMutation' +
          '<script>' +
          'window.globalGuestTest = function globalGuestTest() {' +
          '  for (var i = 0; document.firstChild && i < 100; i++) {' +
          '    document.removeChild(document.firstChild);' +
          '  }' +
          '  assertEquals(0, document.childNodes.length);' +
          '  var el = document.createElement("foo");' +
          '  document.appendChild(el);' +
          '  assertTrue(el === document.firstChild);' +
          '};' +
          '</script>');
    })();

    /**
     * Issue 1589, html-emitter gets confused in a couple ways when
     * head has script but body doesn't
     */
    registerGlobalTest(!inES5Mode, 'testHtmlEmitterFinishInHead',
        'http://nonexistent/',
        // note, the space is necessary to trigger the bug
        '<html><head> <script>;</script></head>'
            + '<body><div>2</div></body></html>',
        function(frame) {
          var doc = frame.div.firstChild.firstChild;
          var html = canonInnerHtml(doc.innerHTML);
          assertEquals(
            '<caja-v-html>'
              + '<caja-v-head></caja-v-head>'
              + '<caja-v-body><div>2</div></caja-v-body>'
              + '</caja-v-html>',
            html);
          jsunitPass('testHtmlEmitterFinishInHead');
        });

    /**
     * Tests of window.location.
     */
    (function() {
      function registerLocationTest(tag, url, opt_specific) {
        var testName = 'testLocation-' + tag;
        registerGlobalTest(true, testName,
            url,
            '<script>' + locationJs + '</script>',
            function(frame) {
              frame.untame(frame.imports.testLocation)(opt_specific);
              jsunitPass(testName);
            });
      }

      registerLocationTest('original',
          'http://localhost:8000/ant-testlib/com/google/caja/plugin/'
          + 'es53-test-domado-dom-guest.html',
          true);

      registerLocationTest('noPath',
          'https://nopath.test');

      registerLocationTest('path',
          'http://path.test/foo/bar%2Fbaz');

      registerLocationTest('search',
          'http://search.test/foo?bar=ba%26z');

      registerLocationTest('hash',
          'http://hash.test/foo#bar%23baz');
    })();

    readyToTest();
    jsunitRun();
  });
})();
