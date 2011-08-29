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
 * @fileoverview Makes sure that the client-side rewriting of URIs embedded
 * in guest HTML/CSS input works properly.
 *
 * @author ihab.awad@gmail.com
 * @author jasvir@gmail.com
 * @requires caja, jsunitRun, readyToTest
 */

(function () {

  var uriCallback = {
    rewrite: function (uri) {
      return 'URICALLBACK[[' + uri + ']]';
    }
  };

  caja.initialize({
    cajaServer: 'http://localhost:8000/caja',
    debug: true,
    forceES5Mode: inES5Mode
  });
  

  registerTest('testUriInAttr', function testUriInAttr() {
    var div = createDiv();
    caja.load(div, uriCallback, function (frame) {
      frame.code('es53-test-client-uri-rewriting-guest.html')
          .run(function (_) {
        assertStringContains(
          canonInnerHtml(
              '<a href="URICALLBACK[['
              + 'http://localhost:8000/ant-lib/'
              + 'com/google/caja/plugin/bar.html'
              + ']]" target="_blank">bar</a>'),
          canonInnerHtml(div.innerHTML));
        jsunitPass('testUriInAttr');
      });
    });
  });

  registerTest('testUriInCss', function testUriInCss() {
    var div = createDiv();
    caja.load(div, uriCallback, function (frame) {
      var emittedCss;
      var originalEmitCss = frame.imports.emitCss___;
      frame.imports.emitCss___ = function(cssText) {
        if (emittedCss) { throw 'cannot handle multiple emitCss___'; }
         emittedCss = cssText;
         originalEmitCss.call(this, cssText);
      };

      frame.code('es53-test-client-uri-rewriting-guest.html')
          .run(function (_) {
        assertStringContains(
          'url(URICALLBACK[['
          + 'http://localhost:8000/ant-lib/com/google/caja/plugin/foo.png'
          + ']])',
          emittedCss);
        jsunitPass('testUriInCss');
      });
    });
  });

  registerTest('testDynamicUriPolicy', function testUriInCss() {
    var div = createDiv();
    var xhrDynamicPolicy = {
        rewrite: function (uri, effects, ltype, hints) {
          assert(typeof hints !== "undefined");
          assert(hints["XHR"]);
          return 'xhrTest.txt';
        }
    };
    caja.load(div, xhrDynamicPolicy, function (frame) {
      var extraImports = createExtraImportsForTesting(caja, frame);

      frame.code('http://localhost:8080/', 'text/html', ''
          + '<script>'
          + '  var request = new XMLHttpRequest();'
          + '  request.open("GET", "non-existent.html",'
          + '    true);'
          + '  request.onreadystatechange = function(event) {'
          + '      if (request.readyState == 4) {'
          + '        assertEquals("The quick brown fox", request.responseText);'
          + '        jsunitPass("testDynamicUriPolicy");'
          + '      }'
          + '  };'
          + '  request.send();'
          + '<\/script>')
          .api(extraImports)
          .run()
    });
  });

  readyToTest();
  jsunitRun();
})();
