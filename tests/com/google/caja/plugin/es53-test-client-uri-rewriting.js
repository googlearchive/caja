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
    cajaServer: '/caja',
    debug: true,
    forceES5Mode: inES5Mode
  });
  
  registerTest('testUriInAttr', function testUriInAttr() {
    var div = createDiv();
    caja.load(div, uriCallback, function (frame) {
      frame.code('es53-test-client-uri-rewriting-guest.html')
          .run(function (_) {
        var result = canonInnerHtml(div.innerHTML);
        assertStringContains(
          canonInnerHtml(
              '<a href="URICALLBACK[['
              + 'http://localhost:8000/ant-lib/'
              + 'com/google/caja/plugin/bar.html'
              + ']]" target="_self">default</a>'
              + '<a href="URICALLBACK[['
              + 'http://localhost:8000/ant-lib/'
              + 'com/google/caja/plugin/bar.html'
              + ']]" target="_blank">top</a>'
              + '<a href="URICALLBACK[['
              + 'http://localhost:8000/ant-lib/'
              + 'com/google/caja/plugin/bar.html'
              + ']]" target="_self">self</a>'
              + '<a href="URICALLBACK[['
              + 'http://localhost:8000/ant-lib/'
              + 'com/google/caja/plugin/bar.html'
              + ']]" target="_blank">parent</a>'
          ),
          result);
        assertStringDoesNotContain('javascript:', result);
        assertStringDoesNotContain('invalid:', result);
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
        assertStringDoesNotContain('javascript:', emittedCss);
        assertStringDoesNotContain('invalid:', emittedCss);
        jsunitPass('testUriInCss');
      });
    });
  });

  registerTest('testDynamicUriPolicy', function testDynamicUriPolicy() {
    var div = createDiv();
    var dynamicPolicy = {
        rewrite: function (uri, effects, ltype, hints) {
          if (/^xhr/.test(uri)) {
            assert(typeof hints !== "undefined");
            assert(!!hints["XHR"]);
            return 'xhrTest.txt';
          } else {
            return 'URI#' + uri;
          }
        }
    };
    caja.load(div, dynamicPolicy, function (frame) {
      // TODO(felix8a): createExtraImportsForTesting doesn't like being
      // called more than once on the same framegroup object
      var extraImports = createExtraImportsForTesting(caja, frame);

      frame.code(
          location.protocol + '//' + location.host + '/',
          'text/html', ''
          + '<div id="a"></div>'
          + '<script>'

          + '  var a = document.getElementById("a");'

          + '  a.innerHTML = "<a href=javascript:1></a>";'
          + '  assertStringDoesNotContain("javascript:", a.innerHTML);'

          + '  a.innerHTML = "<a href=invalid://1></a>";'
          + '  assertStringDoesNotContain("invalid:", a.innerHTML);'

          + '  a.innerHTML = "<p style=\'background: url(http://1)\'>";'
          + '  assertStringContains("URI#http:", a.innerHTML);'

          + '  a.innerHTML = "<p style=\'background: url(javascript:1)\'>";'
          + '  assertStringDoesNotContain("javascript:", a.innerHTML);'

          + '  a.innerHTML = "<p style=\'background: url(invalid://1)\'>";'
          + '  assertStringDoesNotContain("invalid:", a.innerHTML);'

          + '  var request = new XMLHttpRequest();'
          + '  request.open("GET", "xhr-nonexistent", true);'
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
