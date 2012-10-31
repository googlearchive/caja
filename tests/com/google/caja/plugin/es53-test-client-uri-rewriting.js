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
  
  // Since emitCss___ emits CSS to the containing document's HEAD, we cannot
  // simply check innerHTML of the client DIV to see whether the effect was
  // correct. Instead, we have to go to greater extremes by monkey patching the
  // emitCss___ function itself and checking the args it is called with.
  function patchEmitCss(frame) {
    var o = { emittedCss: null };
    function capture(f) {
      return function (cssText) {
        if (o.emittedCss) { throw 'cannot handle multiple emitCss'; }
        o.emittedCss = cssText;
        f.call(this, cssText);
      }
    }
    frame.imports.emitCss___ = capture(frame.imports.emitCss___);  // ES5/3
    frame.domicile.emitCss = capture(frame.domicile.emitCss);  // SES
    return o;
  }

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
              + 'http://localhost:8000/ant-testlib/'
              + 'com/google/caja/plugin/bar.html'
              + ']]" target="_blank">default</a>'
              + '<a href="URICALLBACK[['
              + 'http://localhost:8000/ant-testlib/'
              + 'com/google/caja/plugin/bar.html'
              + ']]" target="_blank">top</a>'
              + '<a href="URICALLBACK[['
              + 'http://localhost:8000/ant-testlib/'
              + 'com/google/caja/plugin/bar.html'
              + ']]" target="_self">self</a>'
              + '<a href="URICALLBACK[['
              + 'http://localhost:8000/ant-testlib/'
              + 'com/google/caja/plugin/bar.html'
              + ']]" target="_blank">parent</a>'
              + '<a href="URICALLBACK[['
              + 'http://localhost:8000/ant-testlib/'
              + 'com/google/caja/plugin/bar.html'
              + ']]" target="_blank">foo</a>'
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
    caja.load(div, uriCallback, jsunitCallback(function frameCb(frame) {
      var capture = patchEmitCss(frame);
      frame.code('es53-test-client-uri-rewriting-guest.html')
          .run(jsunitCallback(function runCb(_) {
        // TODO(kpreid): kludge, should accept both or we should change our
        // rewriters to consistently use one quote form. I don't understand how
        // the no-quotes path occurs; CssTree.java seems to use single quotes.
        var q = inES5Mode ? '"' : '';  
        
        assertStringContains(
          'url(' + q + 'URICALLBACK[['
          + 'http://localhost:8000/ant-testlib/com/google/caja/plugin/foo.png'
          + ']]' + q + ')',
          capture.emittedCss);
        assertStringDoesNotContain('javascript:', capture.emittedCss);
        assertStringDoesNotContain('invalid:', capture.emittedCss);
        jsunitPass('testUriInCss');
      }));
    }));
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
    caja.load(div, dynamicPolicy, jsunitCallback(function frameCb(frame) {
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
    }));
  });

  function registerUriCbTest(name, html, cb) {
    registerTest(name, function() {
        cb = jsunitCallback(cb);
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
            jsunitCallback(function frameCb(frame) {
              frame.code('http://a.com/', 'text/html', html).run(
                  jsunitCallback(function runCb() {
                if (alreadyPassed) { return; }
                assertTrue(
                    'innerHTML ' + div.innerHTML + ' does not contain '
                        + translatedUri,
                    div.innerHTML.indexOf(translatedUri) !== -1);
                jsunitPass(name);
              }));
            }));
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
        assertEquals('CSS', hints.TYPE);
        assertEquals('background-image', hints.CSS_PROP);
      });

  registerUriCbTestCompiledAndDynamic(
      'testHtmlAnchor',
      '<a href="http://foo.com/a.html">foo</a>',
      function(testName, uri, effects, ltype, hints) {
        assertEquals('http://foo.com/a.html', uri);
        assertEquals('MARKUP', hints.TYPE);
        assertEquals('a', hints.XML_TAG);
        assertEquals('href', hints.XML_ATTR);
      });

  registerTest(
      'testStylesheetCompiled',
      function() {
        var calledPolicy = false;
        caja.load(
            createDiv(),
            {
              rewrite: jsunitCallback(function rewrite(
                  uri, effects, ltype, hints) {
                assertEquals('http://x.com/a.jpg', uri);
                assertEquals('CSS', hints.TYPE);
                assertEquals('background-image', hints.CSS_PROP);
                calledPolicy = true;
                return 'URI[[' + uri + ']]';
              })
            },
            jsunitCallback(function frameCb(frame) {
              var capture = patchEmitCss(frame);
              frame
                  .code(
                      'http://a.com/',
                      'text/html',
                      '<style type="text/css">' +
                      '  p { background-image: url(http://x.com/a.jpg); }' +
                      '</style>')
                  .run(jsunitCallback(function runCb() {
                    assertTrue('calledPolicy', calledPolicy);
                    assertStringContains('URI[[http://x.com/a.jpg]]',
                        capture.emittedCss);
                    jsunitPass('testStylesheetCompiled');
                  }));
            }));
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
        assertEquals('CSS', hints.TYPE);
        assertEquals('background-image', hints.CSS_PROP);
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
        assertEquals('XHR', hints.TYPE);
        assertEquals('GET', hints.XHR_METHOD);
        jsunitPass(testName);
        return true;
      });

  readyToTest();
  jsunitRun();
})();
