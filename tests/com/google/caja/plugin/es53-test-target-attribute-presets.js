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

/**
 * @fileoverview Makes sure that the client-side rewriting of the "target"
 * attribute in guest HTML/CSS input works properly.
 *
 * @author ihab.awad@gmail.com
 * @requires caja, jsunitRun, readyToTest
 */

(function () {

  function aMaker(test) {
    return function(node, str) {
      var html = canonInnerHtml(node.innerHTML);
      if (!test(html, str)) {
        fail('Inner HTML [[' + html + ']] does not contain [[' + str + ']]');
      }
    }
  }
  var assertInnerHtmlContains = aMaker(
    function(html, str) { return html.indexOf(str) !== -1; });
  var assertInnerHtmlDoesNotContain = aMaker(
    function(html, str) { return html.indexOf(str) === -1; });

  caja.initialize({
    cajaServer: '/caja',
    debug: true,
    forceES5Mode: inES5Mode,
    targetAttributePresets: {
      default: 'foo',
      whitelist: [ 'foo', 'bar', '_blank' ]
    }
  });

  function registerTargetTest(name, html /*, expected... */) {
    var expected = [].slice.call(arguments, 2);
    assertTrue(expected.length > 0);
    registerTest(name + 'Compiled', function() {
      var div = createDiv();
      caja.load(div, null, function(frame) {
        frame.code('http://a.com/', 'text/html', html)
            .run(function() {
              for (var i = 0; i < expected.length; i++) {
                assertInnerHtmlContains(div, expected[i]);
              }
              jsunitPass(name + 'Compiled');
            });
      });
    });
    registerTest(name + 'Dynamic', function() {
      var div = createDiv();
      caja.load(div, null, function(frame) {
        frame.code('http://a.com/', 'text/html',
            '<div id="a"></div>' +
            '<script type="text/javascript">' +
            '  document.getElementById(\'a\').innerHTML = \'' + html + '\';' +
            '</script>')
            .run(function() {
              for (var i = 0; i < expected.length; i++) {
                assertInnerHtmlContains(div, expected[i]);
              }
              jsunitPass(name + 'Dynamic');
            });
      });
    });
  }

  registerTargetTest(
      'testFormTargetNone',
      '<form action="http://example.com/"></form>',
      'target="foo"');

  registerTargetTest(
      'testFormTargetNoValue',
      '<form target action="http://example.com/"></form>',
      'target="foo"');

  registerTargetTest(
      'testFormTargetEmptyValue',
      '<form action="http://example.com/" target=></form>',
      'target="foo"');

  registerTargetTest(
      'testAnchorTargetNone',
      '<a>a</a>',
      'target="foo"');

  registerTargetTest(
      'testAnchorTargetDefault',
      '<a target="foo">a</a>',
      'target="foo"');

  registerTargetTest(
      'testAnchorTargetWhitelist',
      '<a target="bar">a</a>',
      'target="bar"');

  registerTargetTest(
      'testAnchorTargetIllegal',
      '<a target="baz">a</a>',
      'target="foo"');

  registerTest('testAnchorTargetSetAttribute', function() {
    var div = createDiv();
    caja.load(div, null, function(frame) {
      frame.code('http://a.com/', 'text/html',
          '<a id="a" target="bar">a</a>' +
          '<script type="text/javascript">' +
          '  document.getElementById(\'a\')' +
          '      .setAttribute(\'target\', \'baz\');' +
          '</script>')
          .run(function() {
            assertInnerHtmlContains(div, 'target="foo"');
            jsunitPass('testAnchorTargetSetAttribute');
          });
    });
  });

  registerTargetTest(
      'testUriFragTargetDefault',
      '<a href="#foo" target="foo">a</a>',
      'target="foo"', 'href="#foo-caja-guest-');

  registerTargetTest(
      'testUriFragTargetWhitelist',
      '<a href="#foo" target="bar">a</a>',
      'target="bar"', 'href="#foo-caja-guest-');

  registerTargetTest(
      'testUriFragTargetIllegal',
      '<a href="#foo" target="baz">a</a>',
      'target="foo"', 'href="#foo-caja-guest-');

  registerTargetTest(
      'testUriFragTargetNone',
      '<a href="#foo">a</a>',
      'href="#foo-caja-guest-');

  registerTest('testUriFragTargetSetAttribute', function() {
    var div = createDiv();
    caja.load(div, null, function(frame) {
      frame.code('http://a.com/', 'text/html',
          '<a id="a">a</a>' +
          '<script type="text/javascript">' +
          '  document.getElementById(\'a\')' +
          '      .setAttribute(\'href\', \'#foo\');' +
          '</script>')
          .run(function() {
            assertInnerHtmlDoesNotContain(div, 'target=');
            jsunitPass('testUriFragTargetSetAttribute');
          });
    });
  });

  registerTest('testUriFragTargetReSetAttribute', function() {
    var div = createDiv();
    caja.load(div, caja.policy.net.ALL, function(frame) {
      frame.code('http://b.com/', 'text/html',
          '<a id="b" href="#bar">a</a>' +
          '<script type="text/javascript">' +
          '  document.getElementById(\'b\')' +
          '      .setAttribute(\'href\', \'http://example.com/\');' +
          '</script>')
          .run(function() {
            assertInnerHtmlContains(div, 'target="foo"');
            jsunitPass('testUriFragTargetReSetAttribute');
          });
    });
  });

  readyToTest();
  jsunitRun();
})();
