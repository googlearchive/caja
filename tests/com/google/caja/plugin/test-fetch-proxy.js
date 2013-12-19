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
 * @fileoverview Tests of the fetching proxy server.
 *
 * @author kpreid@switchb.org
 * @requires caja, jsunitRun, readyToTest, basicCajaConfig, location
 */

(function() {
  'use strict';

  // xhr wrapper for exercising explicit requests, specialized for use in tests
  // note uses jsunitCallback and associates with the calling test
  function tfetch(url, expectedMimeType, callback) {
    var request = new XMLHttpRequest();
    request.open('GET', url.toString(), true);
    request.onreadystatechange = jsunitCallback(function() {
      if (request.readyState === 4) {
        if (request.status === 200) {
          console.log('Response: ' + request.responseText);
          assertEquals('response Content-Type', expectedMimeType,
              request.getResponseHeader('Content-Type').split(';')[0]);
          callback(request.responseText);
        } else {
          fail('Unexpected response: ' + request.statusText);
        }
      }
    });
    request.send();
  }

  var JSONP_RE = /^([a-zA-Z_]+)\((\{.*\})\);$/;

  function docURL(name) {
    return location.protocol + '//' + location.host +
        '/ant-testlib/com/google/caja/plugin/' + name;
  }

  function docURLForURL(name) {
    return encodeURIComponent(docURL(name));
  }

  function assertSuccessfulResponse(content, response) {
    assertEquals('object', typeof response);
    assertEquals(content, response.html);
    if ('messages' in response) {
      assertEquals(0, response.messages.length);
    }
  }

  function assertErrorResponse(response) {
    assertEquals('object', typeof response);
    assertFalse('html' in response);
    if ('messages' in response) {
      assertTrue('error message present', response.messages.length > 0);
    }
  }

  // --- What we're testing ---

  // Note: If we need tests for a different server, add URL parameters to
  // select it here.
  var server = basicCajaConfig['cajaServer'];
  var fetcher = caja.policy.net.fetcher.USE_AS_PROXY(server);


  // TODO(kpreid): Test USE_AS_PROXY itself (e.g. exactly what URL it is
  // constructing and sending to the server)


  // --- Server tests ---
  // Testing that the server behaves properly as an HTTP server independent of
  // our client.

  jsunitRegister('testServerJsonp', function() {
    tfetch(
      server + '/proxy?url=' + docURLForURL('test-fetch-proxy-fixture.css')
          + '&input-mime-type=text/css'
          + '&alt=json-in-script'
          + '&callback=foo'
          + '&build-version=' + cajaBuildVersion,
      'text/javascript',
      function(response) {
        var match = JSONP_RE.exec(response);
        assertTrue('is JSONP', !!match);
        assertEquals('foo', match[1]);
        assertSuccessfulResponse('body {}', JSON.parse(match[2]));
        jsunitPass();
      });
  });

  // TODO(kpreid): We no longer care about JSON output; remove server support
  // for it and then remove this test
  jsunitRegister('testServerJson', function() {
    tfetch(
      server + '/proxy?url=' + docURLForURL('test-fetch-proxy-fixture.css')
          + '&input-mime-type=text/css'
          + '&alt=json'
          + '&callback=foo'
          + '&build-version=' + cajaBuildVersion,
      'application/json',
      function(response) {
        assertSuccessfulResponse('body {}', JSON.parse(response));
        jsunitPass();
      });
  });

  jsunitRegister('testServerJsonpAbsent', function() {
    tfetch(
      server + '/proxy?url=' + docURLForURL('test-fetch-proxy-nonexistent.css')
          + '&input-mime-type=text/css'
          + '&alt=json-in-script'
          + '&callback=foo'
          + '&build-version=' + cajaBuildVersion,
      'text/javascript',
      function(response) {
        var match = JSONP_RE.exec(response);
        assertTrue('is JSONP', !!match);
        assertEquals('foo', match[1]);
        assertErrorResponse(JSON.parse(match[2]));
        jsunitPass();
      });
  });

  jsunitRegister('testServerJsonAbsent', function() {
    tfetch(
      server + '/proxy?url=' + docURLForURL('test-fetch-proxy-nonexistent.css')
          + '&input-mime-type=text/css'
          + '&alt=json'
          + '&callback=foo'
          + '&build-version=' + cajaBuildVersion,
      'application/json',
      function(response) {
        assertErrorResponse(JSON.parse(response));
        jsunitPass();
      });
  });


  // --- End-to-end tests ---
  // Testing both proxy server behavior and the USE_AS_PROXY client glue.

  jsunitRegister('testBasic', function() {
    fetcher(docURL('test-fetch-proxy-fixture.css'), 'text/css',
        jsunitCallback(function(response) {
          console.log('Response:', response);
          assertSuccessfulResponse('body {}', response);
          jsunitPass();
        }));
  });
  
  jsunitRegister('testError', function() {
    fetcher(docURL('test-fetch-proxy-nonexistent.css'), 'text/css',
        jsunitCallback(function(response) {
          console.log('Response:', response);
          assertErrorResponse(response);
          jsunitPass();
        }));
  });
  
  jsunitRegister('testUnexpectedMimeType', function() {
    fetcher(docURL('test-fetch-proxy-fixture.css'), 'text/javascript',
        jsunitCallback(function(response) {
          console.log('Response:', response);
          assertErrorResponse(response);
          jsunitPass();
        }));
  });
  
  jsunitRegister('testUnicode', function() {
    // not actually JS, but the current fetcher only permits CSS and JS
    fetcher(docURL('test-fetch-proxy-fixture-unicode.ujs'), 'text/javascript',
        jsunitCallback(function(response) {
          console.log('Response:', response);
          assertSuccessfulResponse('1\u0968\ud800\udd09', response);
          jsunitPass();
        }));
  });

  readyToTest();
  jsunitRun();
})();
