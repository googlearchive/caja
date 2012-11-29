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

  function registerGuestTest(testName, html, varargs) {
    var guestTestArgs = Array.prototype.slice.call(arguments, 2);

    registerTest(testName,
        function guestTestWrapper() {
      var div = createDiv();
      caja.load(div, undefined, function (frame) {
        frame.code(
            location.protocol + '//' + location.host + '/',
            'text/html',
            html)
          .run(createExtraImportsForTesting(caja, frame),
              jsunitCallback(function(result) {
                frame.untame(frame.imports.globalGuestTest).apply(undefined,
                    guestTestArgs);
                jsunitPass(testName);
              }, testName, frame));
      });
    });
  }

  fetch('es53-test-domado-global-html-guest.js', function (htmlGuestJs) {
    /**
     * Tests of how Caja handles omitted structure in HTML document inputs.
     */
    (function() {
      
      function ti(text) {
        return '<title>' + text + '</title>';
      }
      
      function assertGuestHtmlCorrect(frame, div) {
        var vdocContainer = div.getElementsByClassName("vdoc-container___")[0];
        debugger;
        assertEquals('<html><head><title>t</title></head><body>b</body>',
            vdocContainer.innerHTML);
      }

      function registerStructureTest(testName, html, expectHead, expectBody) {
        registerGuestTest(testName,
            html.replace('$', '<script>' + htmlGuestJs + '</script>'),
            '<head>' + expectHead + '</head><body>' + expectBody + '</body>');
      }

      registerStructureTest('testFullyExplicit',
          '<html><head><title>t</title></head>' + 
          '<body>b$</body></html>',
          ti('t'), 'b');

      registerStructureTest('testStartBody',
          '<title>t</title><body>' + 
          'b$',
          ti('t'), 'b');

      registerStructureTest('testStopHead',
          '<title>t</title></head>' + 
          'b$',
          ti('t'), 'b');

      registerStructureTest('testFullyImplicit',
          '<title>t</title>' + 
          'b$',
          ti('t'), 'b');

      registerStructureTest('testJustText',
          'b$',
          '', 'b');

      registerStructureTest('testEmptyVirtualizedElementInHead',
          // Regression test for <body> getting embedded in <head> due to
          // virtualized empty elements being misparsed as non-empty elements
          '<html><head><title>t</title><meta></head>' + 
          '<body>b$</body></html>',
          ti('t') + '<meta>',
          'b');

    }());

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
    
      // TODO(kpreid): Test document.body
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

    readyToTest();
    jsunitRun();
  });
})();
