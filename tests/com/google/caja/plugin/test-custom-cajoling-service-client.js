// Copyright (C) 2013 Google Inc.
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
 * @fileoverview Tests of supplying a custom cajoling service client to
 * the caja.js API.
 *
 * @author ihab.awad@gmail.com
 * @requires caja, jsunitRun, readyToTest
 */

(function () {

  var cajolingServiceClient = (function() {

    function fetch(result) {
      var url =
          '/caja/cajole' +
          '?url=' + encodeURIComponent('http://succeed/test.js') +
          '&build-version=' + cajaBuildVersion +
          '&directive=ES53' +
          '&renderer=pretty' +
          '&input-mime-type=application/javascript' +
          '&alt=json';
      var request = new XMLHttpRequest();
      request.onreadystatechange = function() {
        if (request.readyState === 4 && request.status === 200) {
          result.resolve(JSON.parse(request.responseText));
        }
      };
      request.open('POST', url, true);
      request.setRequestHeader('Content-Type', 'application/javascript');
      request.send('x * y;');
    }

    function cajole(url) {
      var result = caja.Q.defer();
      if (/fail/.test(url)) {
        window.setTimeout(function() {
          result.resolve(caja.Q.reject('Not working'));
        }, 1);
      } else if (/succeed/.test(url)) {
        fetch(result);
      }
      return result.promise;
    }

    return {
      cajoleUrl: function(url, mimeType) {
        return cajole(url);
      },
      cajoleContent: function(url, content, mimeType, domOpts) {
        return cajole(url);
      }
    };
  })();

  caja.initialize({
    cajaServer: '/caja',
    debug: !minifiedMode,
    forceES5Mode: inES5Mode,
    cajolingServiceClient: cajolingServiceClient
  });

  jsunitRegister('testCajoleUrl', function testCajoleUrl() {
    caja.load(undefined, undefined, function (frame) {
      frame
          .code(
              'http://succeed/test.js',
              'application/javascript')
          .api({ x: 17, y: 37 })
          .run(function(result) {
            assertEquals(17 * 37, result);
            jsunitPass('testCajoleUrl');
          });
    });
  });

  jsunitRegister('testCajoleContent', function testCajoleContent() {
    caja.load(undefined, undefined, function (frame) {
      frame
          .code(
              'http://succeed/test.js',
              'application/javascript',
              'CONTENT WILL NOT BE USED')
          .api({ x: 17, y: 37 })
          .run(function(result) {
            assertEquals(17 * 37, result);
            jsunitPass('testCajoleContent');
          });
    });
  });

  readyToTest();
  jsunitRun();
})();
