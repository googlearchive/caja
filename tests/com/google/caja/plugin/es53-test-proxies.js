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
 * @fileoverview Runs Tom van Cutsem's ES5 Proxy tests.
 *
 * @author metaweta@gmail.com
 * @requires caja, jsunitRun, readyToTest
 */

(function () {
  var uriCallback = {
    rewrite: function (uri) {
      return uri;
    }
  };

  caja.initialize({
    cajaServer: 'http://localhost:8000/caja',
    debug: true
  });
  

  registerTest('testProxies', function testProxies() {
    var div = createDiv();
    caja.load(div, uriCallback, function (frame) {
      frame.code('proxies/proxytests.html')
          .run(function (_) {
        assertStringContains(
          'error: 0 fail: 0 skip: 0',
          canonInnerHtml(div.innerHTML));
        jsunitPass('testProxies');
      });
    });
  });

  readyToTest();
  jsunitRun();
})();