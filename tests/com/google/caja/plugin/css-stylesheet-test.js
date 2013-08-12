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

// Called from css-stylesheet-tests.js in a JSONP style.
function runCssSelectorTests(testGroups) {
  var virtualization = {
    containerClass: 'namespace__',
    idSuffix: '-namespace__',
    tagPolicy: function(t, a) {
      // Implementation of virtualization policy for test cases -- the
      // real version of this lives in html-schema.js.
      // TODO(kpreid): Arrange so that we can exercise the sanitizer's
      // behavior given an alternate policy function.
      var eflags = t in html4.ELEMENTS ? html4.ELEMENTS[t]
                                       : html4.eflags.VIRTUALIZED;
      if (eflags & html4.eflags.VIRTUALIZED) {
        return {'tagName': 'caja-v-' + t};
      } else if (!(eflags & html4.eflags.UNSAFE)) {
        return {};
      } else {
        return null;
      }
    },
    virtualizeAttrName: function(elName, attrName) {
      if (attrName === 'rejectedfortest') {
        return null;
      } else if (elName + '::' + attrName in html4.ATTRIBS ||
          '*::' + attrName in html4.ATTRIBS) {
        return attrName;
      } else {
        return 'data-caja-' + attrName;
      }
    }
  };

  // Create a test method that will be called by jsUnit.
  function makeTestFunction(testGroup) {
    var tests = testGroup.tests;
    return function cssTestFn() {
      for (var i = 0, n = tests.length; i < n; ++i) {
        var test = tests[i];
        var input = test.cssText;
        var golden = test.golden;
        assertEquals(
            name + ' tests[' + i + '].cssText', 'string', typeof input);
        assertEquals(
            name + ' tests[' + i + '].golden', 'string', typeof golden);

        var actual = sanitizeStylesheet('',
            test.cssText, virtualization, sanitizeUri);
        // The Java version produces property groups without a trailing
        // ';' since the semicolon is technically a separator in CSS.
        // This JavaScript version does not because it is simpler to
        // just treat it as a terminator.
        actual = actual.replace(/;\}/g, '}');
        if (golden !== actual && 'string' === typeof test.altGolden) {
          golden = test.altGolden;
        }
        assertEquals('stylesheet test ' + i + ': ' + input, golden, actual);
      }
      jsunitPass();
    };
  }

  for (var j = 0, m = testGroups.length; j < m; ++j) {
    var testGroup = testGroups[j];
    var name = testGroup.test_name;
    assertEquals('testGroups[' + j + '].name', 'string', typeof name);
    jsunitRegister(name, makeTestFunction(testGroup));
  }
}

function sanitizeUri(uri) {
  uri = URI.resolve(
      URI.parse('test://example.org/test'),
      URI.parse(uri));
  if ('test' === uri.getScheme()
      && 'example.org' == uri.getDomain()
      && /^\//.test(uri.getPath() || '')) {
    return new URI(
      null, null, null, null,
      '/foo' + uri.getRawPath(), uri.getRawQuery(), uri.getRawFragment())
      .toString();
  } else if ('whitelisted-host.com' === uri.getDomain()) {
    return uri.toString();
  } else {
    return null;
  }
}
