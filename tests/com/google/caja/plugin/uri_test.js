// Copyright (C) 2010 Google Inc.
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

/** @fileoverview testcases for uri.js */

jsunitRegister("testUriParse",
               function testUriParse() {
  var uri = URI.parse('http://www.google.com:80/path?q=query#fragmento');
  assertEquals('http', uri.getScheme());
  assertEquals(null, uri.getCredentials());
  assertEquals('www.google.com', uri.getDomain());
  assertEquals('80', uri.getPort());
  assertEquals('/path', uri.getPath());
  assertEquals('q=query', uri.getQuery());
  assertEquals('fragmento', uri.getFragment());

  assertEquals('terer258+foo@gmail.com',
               URI.parse('mailto:terer258+foo@gmail.com').getPath());
  jsunit.pass();
});

jsunitRegister("testUriCreate",
               function testUriCreate() {
  assertEquals(
      'http://www.google.com:80/search%20path?q=what%20to%20eat%2Bdrink%3F',
      URI.create('http', null, 'www.google.com', 80,
                 '/search path', [ 'q', 'what to eat+drink?' ], null)
      .toString());
  assertEquals(
      'http://www.google.com:81/search%20path?q=what%20to%20eat%2Bdrink%3F',
      URI.create('http', null, 'www.google.com', 81, '/search path',
                 { 'q': 'what to eat+drink?' }, null)
      .toString());
  assertEquals(
      'http://www.google.com:80/search%20path?q=what%20to%20eat%2Bdrink%3F',
      URI.create('http', null, 'www.google.com', 80, '/search path',
                 { 'q': 'what to eat+drink?' }, null)
      .toString());

  assertEquals(
      'http://www.google.com/search%20path?q=what%20to%20eat%2Bdrink%3F',
      URI.create('http', null, 'www.google.com', null, '/search path',
                 { 'q': 'what to eat+drink?' }, null)
      .toString());
  jsunit.pass();
});

jsunitRegister("testRelativeUris",
               function testRelativeUris() {
  assertTrue(!URI.parse('?hello').hasPath());
  jsunit.pass();
});

jsunitRegister("testAbsolutePathResolution",
               function testAbsolutePathResolution() {
  assertEquals('http://www.google.com:80/foo',
               URI.resolve(
                   URI.parse('http://www.google.com:80/path?q=query#fragmento'),
                   URI.parse('/foo')
                   ).toString()
               );
  assertEquals('http://www.google.com:80/foo/bar',
               URI.resolve(
                   URI.parse('http://www.google.com:80/search/'),
                   URI.parse('/foo/bar')
                   ).toString()
               );
  jsunit.pass();
});

jsunitRegister("testRelativePathResolution",
               function testRelativePathResolution() {
  assertEquals('http://www.google.com:80/foo',
               URI.resolve(
                   URI.parse('http://www.google.com:80/path?q=query#fragmento'),
                   URI.parse('foo')
                   ).toString()
               );
  assertEquals('http://www.google.com:80/foo/bar',
               URI.resolve(
                   URI.parse('http://www.google.com:80/search'),
                   URI.parse('foo/bar')
                   ).toString()
               );
  assertEquals('http://www.google.com:80/search/foo/bar',
               URI.resolve(
                   URI.parse('http://www.google.com:80/search/'),
                   URI.parse('foo/bar')
                   ).toString()
               );
  assertEquals('bar',
               URI.resolve(
                   URI.parse('foo'),
                   URI.parse('bar'))
                   .toString()
               );
  assertEquals('http://example.com/bar/y.js',
               URI.resolve(
                   URI.parse('http://example.com/foo/x.html'),
                   URI.parse('../bar/y.js')
                   ).toString()
               );
  assertEquals('http://example.com/foo/bar/y.js',
               URI.resolve(
                   URI.parse('http://example.com/foo/baz/x.html'),
                   URI.parse('../bar/y.js')
                   ).toString()
               );
  jsunit.pass();
});

jsunitRegister("testDomainResolution",
               function testDomainResolution() {
  assertEquals('https://www.google.com/foo/bar',
               URI.resolve(
                   URI.parse('https://www.fark.com:443/search/'),
                   URI.parse('//www.google.com/foo/bar')
                   ).toString()
               );
  assertEquals('http://www.google.com/',
               URI.resolve(
                   URI.parse('http://www.fark.com/search/'),
                   URI.parse('//www.google.com/')
                   ).toString()
               );
  jsunit.pass();
});

jsunitRegister("testQueryResolution",
               function testQueryResolution() {
  assertEquals('http://www.google.com/search?q=new%20search',
               URI.resolve(
                   URI.parse('http://www.google.com/search?q=old+search'),
                   URI.parse('?q=new%20search')
                   ).toString()
               );

  assertEquals('http://www.google.com/search?q=new%20search',
               URI.resolve(
                   URI.parse('http://www.google.com/search?q=old+search#hi'),
                   URI.parse('?q=new%20search')
                   ).toString()
               );
  jsunit.pass();
});

jsunitRegister("testFragmentResolution",
               function testFragmentResolution() {
  assertEquals('http://www.google.com/foo/bar?q=hi#there',
               URI.resolve(
                   URI.parse('http://www.google.com/foo/bar?q=hi'),
                   URI.parse('#there')
                   ).toString()
               );

  assertEquals('http://www.google.com/foo/bar?q=hi#there',
               URI.resolve(
                   URI.parse('http://www.google.com/foo/bar?q=hi#you'),
                   URI.parse('#there')
                   ).toString()
               );
  jsunit.pass();
});

jsunitRegister("testBogusResolution",
               function testBogusResolution() {
  assertEquals('a://completely.different/url',
               URI.resolve(
                   URI.parse('some:base/url'),
                   URI.parse('a://completely.different/url')
                   ).toString()
               );
  jsunit.pass();
});

jsunitRegister("testParameterGetters",
               function testParameterGetters() {
  function assertArraysEqual(l1, l2) {
    if (!l1 || !l2) {
      assertEquals(l1, l2);
      return;
    }
    var l1s = l1.toString(), l2s = l2.toString();
    assertEquals(l1s, l2s);
    assertEquals(l1s, l1.length, l2.length);
    for (var i = 0; i < l1s.length; ++i) {
      assertEquals("part " + i + " of " + l1s.length + " in " + l1s,
                   l1[i], l2[i]);
    }
  }

  assertArraysEqual(['v1', 'v2'],
                    URI.parse('/path?a=b&key=v1&c=d&key=v2&keywithsuffix=v3')
                        .getParameterValues('key'));
  assertEquals('v1',
               URI.parse('/path?key=v1&c=d&keywithsuffix=v3&key=v2')
                   .getParameterValue('key'));
  assertEquals('v1=v2',
               URI.parse('/path?key=v1=v2').getParameterValue('key'));

  assertEquals('v1=v2=v3',
               URI.parse('/path?key=v1=v2=v3').getParameterValue('key'));
  assertArraysEqual(null,
                    URI.parse('/path?key=v1&c=d&keywithsuffix=v3&key=v2')
                        .getParameterValue('nosuchkey'));
  // test boundary conditions
  assertArraysEqual(['v1', 'v2'],
                    URI.parse('/path?key=v1&c=d&key=v2&keywithsuffix=v3')
                        .getParameterValues('key'));
  assertArraysEqual(['v1', 'v2'],
                    URI.parse('/path?key=v1&c=d&keywithsuffix=v3&key=v2')
                        .getParameterValues('key'));
  // test no =
  assertArraysEqual(
      [''], URI.parse('/path?key').getParameterValues('key'));
  assertEquals('', URI.parse('/path?key').getParameterValue('key'));
  assertEquals('', URI.parse('/path?foo=bar&key').getParameterValue('key'));

  var u = URI.parse('/path?a=b&key=v1&c=d&key=v2&keywithsuffix=v3');
  assertArraysEqual(['a', 'b',
                     'key', 'v1',
                     'c', 'd',
                     'key', 'v2',
                     'keywithsuffix', 'v3'],
                    u.getAllParameters('key'));
  assertArraysEqual(u.getParameterValues('a'), ['b']);
  assertArraysEqual(u.getParameterValues('key'), ['v1', 'v2']);
  assertArraysEqual(u.getParameterValues('c'), ['d']);
  assertArraysEqual(u.getParameterValues('keywithsuffix'), ['v3']);
  assertArraysEqual(u.getParameterValues('KeyWITHSuffix'), []);
  jsunit.pass();
});

jsunitRegister("testParameterSetters",
               function testParameterSetters() {
  assertEquals('/path?a=b&key=newval&c=d&keywithsuffix=v3',
               URI.parse('/path?a=b&key=v1&c=d&key=v2&keywithsuffix=v3')
               .setParameterValues('key', 'newval').toString());

  assertEquals('/path?a=b&key=1&c=d&key=2&keywithsuffix=v3&key=3',
               URI.parse('/path?a=b&key=v1&c=d&key=v2&keywithsuffix=v3')
               .setParameterValues('key', [ '1', '2', '3' ]).toString());
  jsunit.pass();
});

jsunitRegister("testEncoding",
               function testEncoding() {
  assertEquals('/foo bar baz',
               URI.parse('/foo%20bar%20baz').getPath());
  assertEquals('/foo%20bar%20baz',
               URI.parse('/foo%20bar%20baz').getRawPath());
  assertEquals('/foo+bar+baz',
               URI.parse('/foo+bar+baz').getPath());
  assertEquals('/foo+bar+baz',
               URI.parse('/foo+bar+baz').getRawPath());
  jsunit.pass();
});

jsunitRegister("testSetters",
               function testSetters() {
  function uristr(uri) { return ('' + uri); }
  var uri = URI.parse('http://www.google.com:80/path?q=query#fragmento');
  assertEquals('https://www.google.com:80/path?q=query#fragmento',
               uristr(uri.setScheme('https')));
  assertEquals('https://%E1%B8%A1oogle.com:80/path?q=query#fragmento',
               uristr(uri.setDomain('\u1e21oogle.com')));
  assertEquals('https://%E1%B8%A1oogle.com:443/path?q=query#fragmento',
               uristr(uri.setPort(443)));
  assertEquals(
      'https://%E1%B8%A1oogle.com:443/search%20path/?q=query#fragmento',
      uristr(uri.setPath('/search path/')));
  assertEquals(
      'https://%E1%B8%A1oogle.com:443/search%20path/?q=some%20stuff&lang=en' +
      '#fragmento',
      uristr(uri.setParameterValues('q', 'some stuff')
                 .setParameterValues('lang', 'en')));
  assertEquals(
      'https://%E1%B8%A1oogle.com:443/search%20path/?q=some%20stuff&lang=en',
      uristr(uri.setFragment(null)));
  assertEquals(
      'https://%E1%B8%A1oogle.com/search%20path/?q=some%20stuff&lang=en',
      uristr(uri.setPort(null)));
  assertEquals(
      'https://user:pwd@%E1%B8%A1oogle.com/search%20path/' +
      '?q=some%20stuff&lang=en',
      uristr(uri.setCredentials('user:pwd')));
  jsunit.pass();
});

jsunitRegister("testTreatmentOfAt1",
               function testTreatmentOfAt1() {
  var uri = URI.parse('http://www.google.com?q=johndoe@example.com');
  assertEquals('http', uri.getScheme());
  assertEquals('www.google.com', uri.getDomain());
  assertEquals('q=johndoe@example.com', uri.getQuery());

  assertEquals('http://www.google.com?q=johndoe%40example.com',
               URI.create('http', null, 'www.google.com', null, null,
                          ['q', 'johndoe@example.com'], null).toString());
  assertEquals('http://www.google.com?q=johndoe%40example.com',
               URI.create('http', null, 'www.google.com', null, null,
                          'q=johndoe@example.com', null).toString());
  jsunit.pass();
});

jsunitRegister("testTreatmentOfAt2",
               function testTreatmentOfAt2() {
  var uri = URI.parse('http://www/~johndoe@example.com/foo');
  assertEquals('http', uri.getScheme());
  assertEquals('www', uri.getDomain());
  assertEquals('/~johndoe@example.com/foo', uri.getPath());

  assertEquals('http://www/~johndoe@example.com/foo',
               URI.create('http', null, 'www', null,
                          '/~johndoe@example.com/foo', null, null) + '');
  jsunit.pass();
});

jsunitRegister("testTreatmentOfAt3",
               function testTreatmentOfAt3() {
  var uri = URI.parse('ftp://skroob:1234@teleport/~skroob@vacuum');
  assertEquals('ftp', uri.getScheme());
  assertEquals('skroob:1234', uri.getCredentials());
  assertEquals('teleport', uri.getDomain());
  assertEquals('/~skroob@vacuum', uri.getPath());

  assertEquals('ftp://skroob:1234@teleport/~skroob@vacuum',
               URI.create('ftp', 'skroob:1234', 'teleport', null,
                          '/~skroob@vacuum', null, null).toString());
  jsunit.pass();
});

jsunitRegister("testTreatmentOfAt4",
               function testTreatmentOfAt4() {
  assertEquals('ftp://darkhelmet:45%4078@teleport/~dhelmet@vacuum',
               URI.create('ftp', 'darkhelmet:45@78', 'teleport', null,
                          '/~dhelmet@vacuum', null, null).toString());
  jsunit.pass();
});

jsunitRegister("testUriParseAcceptsThingsWithToString",
               function testUriParseAcceptsThingsWithToString() {
  // Ensure that URI.parse coerces random types to strings.
  var uriStr = 'http://www.google.com:80/path?q=query#fragmento';
  var flipflop = 0;
  var uri = URI.parse(
      {
        toString: function() {
          return (++flipflop & 1) ? uriStr : uriStr + 'flop';
        }
      });
  assertEquals('http://www.google.com:80/path?q=query#fragmento',
               uri.toString());
  assertEquals('http://www.google.com:80/path?q=query#fragmento',
               uri.toString());
  jsunit.pass();
});

jsunitRegister("testClone",
               function testClone() {
  var uri1 = URI.parse('http://www.google.com:8080/foo');
  var uri2 = uri1.clone();

  uri2.setParameterValues('q', 'bar');
  assertFalse(uri1.getParameterValue('q') == 'bar');
  assertEquals('bar', uri2.getParameterValue('q'));
  jsunit.pass();
});

jsunitRegister("testPathConcatenation",
               function testPathConcatenation() {
  // Check accordenance with RFC 3986, section 5.2.4
  assertResolvedEquals('bar', '', 'bar');
  assertResolvedEquals('/bar', '/', 'bar');
  assertResolvedEquals('/bar', '/foo', '/bar');
  assertResolvedEquals('/foo/foo', '/foo/bar', 'foo');
  assertResolvedEquals('/boo/foo', '/foo/../boo/bar', 'foo');
  assertResolvedEquals('/boo/foo', '/foo/../boo/bar', 'foo');
  assertResolvedEquals('/foo/baz', '/foo/bar/boo', '../baz');
  assertResolvedEquals('foo/baz', '../foo/bar/boo', '../baz');
  assertResolvedEquals('foo/bar/baz', '../foo/bar/boo', 'baz');
  assertResolvedEquals('foo/baz', '../../foo/bar/boo', '../baz');
  assertResolvedEquals('baz', '..', 'baz');
  assertResolvedEquals('/baz', '/..', 'baz');
  assertResolvedEquals('foo/...', 'foo/', '...');
  jsunit.pass();
});

jsunitRegister("testPathConcatenationDontRemoveForEmptyUri",
               function testPathConcatenationDontRemoveForEmptyUri() {
  // Resolving URIs with empty path should not result in dot segments removal.
  // See: algorithm in section 5.2.2: code inside 'if (R.path == "")' clause.
  assertResolvedEquals('/search/../foo', '/search/../foo', '');
  assertResolvedEquals('/search/./foo', '/search/./foo', '');
  jsunit.pass();
});

jsunitRegister("testRemoveParameter",
               function testRemoveParameter() {
  assertEquals('/path?a=b&c=d&keywithsuffix=v3',
               URI.parse('/path?a=b&key=v1&c=d&key=v2&keywithsuffix=v3')
               .removeParameter('key').toString());
  jsunit.pass();
});

jsunitRegister("testFragmentEncoding",
               function testFragmentEncoding() {
  var allowedInFragment = /[A-Za-z0-9\-\._~!$&\'()*+,;=:@\/?]/g;

  var sb = [];
  for (var i = 33; i < 500; i++) {  // arbitrarily use first 500 chars.
    sb.push(String.fromCharCode(i));
  }
  var testString = sb.join('');

  var fragment = URI.parse('').setFragment(testString).toString();

  // Remove first '#' character.
  fragment = fragment.substr(1);

  // Strip all percent encoded characters, as they're ok.
  fragment = fragment.replace(/%[0-9A-Fa-f]{2}/g, '');

  // Remove allowed characters.
  fragment = fragment.replace(allowedInFragment, '');

  // Only illegal characters should remain, which is a fail.
  assertEquals('String should be empty : ' + fragment, 0, fragment.length);
  jsunit.pass();
});

// Tests, that creating URI from components and then
// getting the components back yields equal results.
// The special attention is payed to test proper encoding
// and decoding of URI components.
jsunitRegister("testComponentsAfterUriCreate",
               function testComponentsAfterUriCreate() {
  var createdUri = URI.create('%40',  // scheme
                              '%41',  // user info
                              '%42',  // domain
                              43,     // port
                              '%44',  // path
                              '%45',  // query
                              '%46'); // fragment

  assertEquals('%40', createdUri.getScheme());
  assertEquals('%41', createdUri.getCredentials());
  assertEquals('%42', createdUri.getDomain());
  assertEquals('43', createdUri.getPort());
  assertEquals('%44', createdUri.getPath());
  assertEquals('%45', createdUri.getRawQuery());
  assertEquals('%46', createdUri.getFragment());
  jsunit.pass();
});

// Tests setting the query string and then reading back
// query parameter values.
jsunitRegister("testSetQueryAndGetParameterValue",
               function testSetQueryAndGetParameterValue() {
  var uri = URI.parse('');

  uri.setRawQuery('i=j&k');
  assertEquals('?i=j&k', uri.toString());
  assertEquals('i=j&k', uri.getQuery());
  assertEquals('i=j&k', uri.getRawQuery());
  assertEquals('j', uri.getParameterValue('i'));
  assertEquals('', uri.getParameterValue('k'));

  // Sets query as decoded string.
  uri.setQuery('i=j&k');
  assertEquals('?i%3Dj%26k', uri.toString());
  assertEquals('i=j&k', uri.getQuery());
  assertEquals('i%3Dj%26k', uri.getRawQuery());
  assertEquals('', uri.getParameterValue('i=j&k'));
  assertNull(uri.getParameterValue('i'));
  assertNull(uri.getParameterValue('k'));

  // Sets query as encoded string.
  uri.setRawQuery('i=j%26k');
  assertEquals('?i=j%26k', uri.toString());
  assertEquals('i=j&k', uri.getQuery());
  assertEquals('i=j%26k', uri.getRawQuery());
  assertEquals('j&k', uri.getParameterValue('i'));
  assertNull(uri.getParameterValue('k'));
  jsunit.pass();
});

// Tests setting query parameter values and the reading back the query string.
jsunitRegister("testSetParameterValueAndGetQuery",
               function testSetParameterValueAndGetQuery() {
  var uri = URI.parse('');

  uri.setParameterValues('a', 'b&c');
  assertEquals('?a=b%26c', uri.toString());
  assertEquals('a=b&c', uri.getQuery());
  assertEquals('a=b%26c', uri.getRawQuery());

  uri.setParameterValues('a', 'b%26c');
  assertEquals('?a=b%2526c', uri.toString());
  assertEquals('a=b%26c', uri.getQuery());
  assertEquals('a=b%2526c', uri.getRawQuery());
  jsunit.pass();
});


// Tests that building a URI with a query string and then reading it back
// gives the same result.
jsunitRegister("testQueryNotModified",
               function testQueryNotModified() {
  assertEquals('?foo', URI.parse('?foo').toString());
  assertEquals('?foo=', URI.parse('?foo=').toString());
  assertEquals('?foo=bar', URI.parse('?foo=bar').toString());
  assertEquals('?&=&=&', URI.parse('?&=&=&').toString());
  jsunit.pass();
});

jsunitRegister("testCollapseDots",
               function testCollapseDots() {
  assertEquals('', URI.collapse_dots(''));
  assertEquals('/', URI.collapse_dots('/'));
  assertEquals('', URI.collapse_dots('./'));
  assertEquals('', URI.collapse_dots('.'));
  assertEquals('/', URI.collapse_dots('/.'));
  assertEquals('foo', URI.collapse_dots('foo'));
  assertEquals('/foo', URI.collapse_dots('/foo'));
  assertEquals('/foo/', URI.collapse_dots('/foo/'));
  assertEquals('foo/', URI.collapse_dots('foo/'));
  assertEquals('../foo/bar', URI.collapse_dots('../foo/bar'));
  assertEquals('../foo', URI.collapse_dots('../foo'));
  assertEquals('bar', URI.collapse_dots('foo/../bar'));
  assertEquals('/bar', URI.collapse_dots('/foo/../bar'));
  assertEquals('/bar/', URI.collapse_dots('/foo/../bar/'));
  assertEquals('', URI.collapse_dots('foo/../'));
  assertEquals('', URI.collapse_dots('foo/..'));
  assertEquals('foo/baz', URI.collapse_dots('foo/bar/../baz'));
  assertEquals('foo/boo/faz',
               URI.collapse_dots('foo/bar/baz/../../boo/far/../faz'));
  assertEquals('foo/boo/faz',
               URI.collapse_dots('foo//bar/baz/..//../boo//far/../faz'));
  jsunit.pass();
});

function assertResolvedEquals(expected, base, other) {
  assertEquals(expected, '' + URI.resolve(URI.parse(base), URI.parse(other)));
}
