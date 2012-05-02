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

  function registerUriCbTest(name, html, cb) {
    registerTest(name, function() {
        var div = createDiv();
        var alreadyPassed;
        var translatedUri;
        caja.load(
            div,
            {
              rewrite: function(uri, effects, ltype, hints) {
                alreadyPassed = cb(name, uri, effects, ltype, hints);
                return translatedUri = 'URICALLBACK[[' + uri + ']]';
              }
            },
            function(frame) {
              frame.code('http://a.com/', 'text/html', html).run(function() {
                if (alreadyPassed) { return; }
                assertTrue(
                    'innerHTML ' + div.innerHTML + ' does not contain '
                        + translatedUri,
                    div.innerHTML.indexOf(translatedUri) !== -1);
                jsunitPass(name);
              });
            });
        });
  }

  function registerUriCbTestCompiledAndDynamic(name, html, cb) {
    registerUriCbTest(
        name + 'Compiled',
        html,
        cb);
    registerUriCbTest(
        name + 'Dynamic',
        '<div id="a"></div>' +
        '<script type="application/javascript">' +
        '  document.getElementById(\'a\').innerHTML = \'' + html + '\'' +
        '</script>',
        cb);
  }

  registerUriCbTestCompiledAndDynamic(
      'testStyleProperty',
      '<div style="background-image: url(http://foo.com/a.jpg);"></div>',
      function(testName, uri, effects, ltype, hints) {
        assertEquals('http://foo.com/a.jpg', uri);
      });

  registerUriCbTestCompiledAndDynamic(
      'testHtmlAnchor',
      '<a href="http://foo.com/a.html">foo</a>',
      function(testName, uri, effects, ltype, hints) {
        assertEquals('http://foo.com/a.html', uri);
      });

  // Since emitCss___ emits CSS to the containing document's HEAD, we cannot
  // simply check innerHTML of the client DIV to see whether the effect was
  // correct. Instead, we have to go to greater extremes by monkey patching the
  // emitCss___ function itself and checking the args it is called with.
  registerTest(
      'testStylesheetCompiled',
      function() {
        var calledPolicy;
        var calledEmitCss;
        caja.load(
            createDiv(),
            {
              rewrite: function(uri, effects, ltype, hints) {
                assertEquals('http://x.com/a.jpg', uri);
                calledPolicy = true;
                return 'URI[[' + uri + ']]';
              }
            },
            function(frame) {
              frame
                  .code(
                      'http://a.com/',
                      'text/html',
                      '<style type="text/css">' +
                      '  p { background-image: url(http://x.com/a.jpg); }' +
                      '</style>')
                  .api({
                    emitCss___: function(css) {
                      var i = css.indexOf('URI[[http://x.com/a.jpg]]');
                      assertTrue(-1 !== i);
                      calledEmitCss = true;
                    }
                  })
                  .run(function() {
                    assertTrue(calledPolicy);
                    assertTrue(calledEmitCss);
                    jsunitPass('testStylesheetCompiled');
                  });
            });
      });

  /*
   * TODO(ihab.awad): Domado does not yet support dynamic <style> elements
  registerUriCbTest(
      'testStylesheetDynamic',
      '<script type="application/javascript">' +
      '  var s = document.createElement(\'style\');' +
      '  s.innerText = ' +
      '      \'p { background-image: url(http://foo.com/a.jpg); }\';' +
      '  document.body.appendChild(s);' +
      '</script>',
      function(testName, uri, effects, ltype, hints) {
        assertEquals('http://foo.com/a.jpg', uri);
      });
  */

  registerUriCbTest(
      'testXhr',
      '<script type="text/javascript">' +
      '  var xhr = new XMLHttpRequest();' +
      '  xhr.open(\'GET\', \'http://foo.com/a.json\');' +
      '  xhr.send(undefined);' +
      '</script>',
      function(testName, uri, effects, ltype, hints) {
        assertEquals('http://foo.com/a.json', uri);
        jsunitPass(testName);
        return true;
      });

  readyToTest();
  jsunitRun();
})();
