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
    var xhr = bridal.makeXhr();
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

  function assertStringContains(chunk, text) {
    if (new RegExp(chunk).test(text)) { return; }
    fail('Cannot find <<' + chunk + '>> in <<' + text + '>>');
  }

  function uriCallback(uri, mimeType) { return uri; }

  caja.configure({
    cajaServer: 'http://localhost:8000/caja',
    debug: true
  }, function (frameGroup) {

    // TODO(ihab.awad): Test 'base url' functionality, esp. for "content" cases

    jsunitRegister('testContentCajoledHtml', function testContentCajoledHtml() {
      fetch('es53-test-guest.out.html', function(resp) {
        var htmlAndScript = splitHtmlAndScript(resp);
        var div = createDiv();
        frameGroup.makeES5Frame(div, uriCallback, function (frame) {
          frame.contentCajoled('http://localhost:8080/',
                               htmlAndScript[1], htmlAndScript[0])
               .run({}, function (result) {
            assertStringContains('static html', div.innerHTML);
            assertStringContains('dynamic html', div.innerHTML);
            jsunit.pass('testContentCajoledHtml');
          });
        });
      });
    });

    jsunitRegister('testContentCajoledJs', function testContentCajoledJs() {
      fetch('es53-test-guest.out.js', function(script) {
        frameGroup.makeES5Frame(undefined, uriCallback, function (frame) {
          var extraImports = { x: 4, y: 3 };
          frame.contentCajoled(undefined, script, undefined)
               .run(extraImports, function (result) {
            assertEquals(12, result);
            jsunit.pass('testContentCajoledJs');
          });
        });
      });
    });

    jsunitRegister('testNoImports', function testNoImports() {
      fetch('es53-test-guest.out.html', function(resp) {
        var htmlAndScript = splitHtmlAndScript(resp);
        var div = createDiv();
        frameGroup.makeES5Frame(div, uriCallback, function (frame) {
          frame.contentCajoled('http://localhost:8080/',
                               htmlAndScript[1], htmlAndScript[0])
               .run(undefined, function (result) {
            assertStringContains('static html', div.innerHTML);
            assertStringContains('dynamic html', div.innerHTML);
            jsunit.pass('testNoImports');
          });
        });
      });
    });

    // TODO(ihab.awad): Implement 'urlCajoled' case and enable the below.
    // jsunitRegister('testUrlCajoledHtml', function testUrlCajoledHtml() { });
    // jsunitRegister('testUrlCajoledJs', function testUrlCajoledJs() { });

    // TODO(ihab.awad): Implement 'content' case and enable the below.
    // jsunitRegister('testContentHtml', function testContentHtml() { });
    // jsunitRegister('testContentJs', function testContentJs() { });

    jsunitRegister('testUrlHtml', function testUrlHtml() {
      var div = createDiv();
      frameGroup.makeES5Frame(div, uriCallback, function (frame) {
        frame.url('es53-test-guest.html').run({}, function (result) {
          assertStringContains('static html', div.innerHTML);
          assertStringContains('dynamic html', div.innerHTML);
          jsunit.pass('testUrlHtml');
        });
      });
    });

    jsunitRegister('testUrlJs', function testUrlJs() {
      frameGroup.makeES5Frame(undefined, uriCallback, function (frame) {
        var extraImports = { x: 4, y: 3 };
        frame.url('es53-test-guest.js').run(extraImports, function (result) {
          assertEquals(12, result);
          jsunit.pass('testUrlCajoledJs');
        });
      });
    });

    readyToTest();
    jsunitRun();
  });
})();
