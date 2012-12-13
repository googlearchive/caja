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

(function () {

  // TODO(felix8a): these lists can get out of sync with reality

  // List of [testfile, mode].
  // If mode is omitted, both es5 and es53 are emitted.
  var test_cases = [
    ['es53-test-basic-functions-guest.html'],
    ['es53-test-domado-canvas-guest.html'],
    ['es53-test-domado-dom-guest.html'],
    ['es53-test-domado-forms-guest.html'],
    ['es53-test-inline-script.html'],
    ['es53-test-external-script-guest.html', 'es5'],
    ['es53-test-language-guest.html'],
    ['es53-test-precajole-guest.html', 'es53']
  ];

  var test_drivers = [
    ['es53-test-apitaming.js'],
    ['es53-test-automode-1.js'],
    ['es53-test-automode-2.js'],
    ['es53-test-automode-3.js'],
    ['es53-test-automode-4.js'],
    ['es53-test-cajajs-invocation.js'],
    ['es53-test-cajajs-minor-version-skew-cajoler-response.js'
     + '&cajajs=/caja/testing/skew-mmm/caja.js', 'es53'],
    ['es53-test-cajajs-never-starts.js'
     + '&cajajs=/caja/testing/skew-0000/caja.js'],
    ['es53-test-cajajs-never-starts.js'
     + '&resources=/caja/testing/nonexistent'],
    ['es53-test-cajajs-never-starts.js'
     + '&resources=/caja/testing/skew-0000'],
    ['es53-test-cajajs-version-skew-cajoled-module.js', 'es53'],
    ['es53-test-cajajs-version-skew-cajoler-response.js'
     + '&cajajs=/caja/testing/skew-0000/caja.js', 'es53'],
    ['es53-test-cajajs-version-skew-js-files.js'],
    ['es53-test-client-uri-rewriting.js'],
    ['es53-test-container-overflow.js'],
    ['es53-test-defensible-objects.js'],
    ['es53-test-domado-global.js'],
    ['es53-test-domado-events.js'],
    ['es53-test-domado-foreign.js'],
    ['es53-test-domado-special.js'],
    ['es53-test-proxies.js', 'es53'],
    ['es53-test-relative-urls.js'],
    ['es53-test-taming-errors.js'],
    ['es53-test-taming-inout.js'],
    ['es53-test-taming-primitives.js'],
    ['es53-test-taming-tamed.js'],
    ['es53-test-taming-untamed.js'],
    ['es53-test-unicode.js']
  ];

  var bare_tests = [
    'modules-test.html',
  ];

  var headless_rhino_tests = [
    'csslexer-test.html',
    'cssparser_test.html',
    'css-stylesheet-test.html',
    'html-emitter-test.html',
    'html-sanitizer-test.html',
    'html-sanitizer-minified-test.html',
    'html-sanitizer-regress.html',
    'html-css-sanitizer-test.html',
    'html-css-sanitizer-minified-test.html',
    'sanitizecss_test.html',
    'uri_test.html',
  ];

  var jquery = [
    'core',
    'callbacks',
    'deferred',
    'support',
    'data',
    'queue',
    'attributes',
    'event',
    'selector',
    'traversing',
    'manipulation',
    'css',
    'ajax',
    'effects',
    'offset',
    'dimensions',
    'exports'
  ];

  var jqueryui = [
    'accordion',
    'autocomplete',
    'button',
    'core',
    'datepicker',
    'dialog',
    'effects',
    'menu',
    'position',
    'progressbar',
    'slider',
    'spinner',
    'tabs',
    'tooltip',
    'widget'
  ];

  var tests_ul = document.getElementById('tests');

  function addSeparator() {
    tests_ul.appendChild(document.createElement('br'));
  }

  function addItem(html) {
    var li = document.createElement('li');
    li.innerHTML = html;
    tests_ul.appendChild(li);
  }

  function addBare(url, text) {
    addItem('<a href="' + url + '">' + (text || url) + '</a>');
  }

  function addModed(url, text, mode, minify) {
    var html = '';
    if (!mode || mode === 'es53') {
      html += '[<a href="' + url + '&es5=false&minified=false">es53</a> ';
      html += '<a href="' + url + '&es5=false&minified=true">min</a>] ';
    } else {
      html += '[<s>es53</s> <s>min</s>] ';
    }
    if (!mode || mode === 'es5') {
      html += '[<a href="' + url + '&es5=true&minified=false">es5</a> ';
      html += '<a href="' + url + '&es5=true&minified=true">min</a>] ';
    } else {
      html += '[<s>es5</s> <s>min</s>] ';
    }
    html += text || url;
    addItem(html);
  }

  function addQUnit(text, rawUrl) {
    addModed('browser-test-case.html?jQuery=true&test-case=' + rawUrl, text,
        'es5');
  }

  function forEach(array, callback) {
    for (var i = 0; i < array.length; i++) {
      callback(array[i]);
    }
  }

  forEach(headless_rhino_tests, addBare);

  addSeparator();

  forEach(bare_tests, addBare);

  addSeparator();

  forEach(test_cases, function(item) {
    addModed('browser-test-case.html?test-case=' + item[0], item[0], item[1]);
  });

  addSeparator();

  forEach(test_drivers, function(item) {
    addModed('browser-test-case.html?test-driver=' + item[0], item[0], item[1]);
  });

  addSeparator()

  addModed('cajajs-bare-test.html?', 'cajajs-bare-test.html', null);

  addSeparator();

  forEach(jquery, function(test) {
    addQUnit('jQuery ' + test,
             '/ant-lib/js/jqueryjs/test/' + test + '-uncajoled.html');
  })

  forEach(jqueryui, function(test) {
    addQUnit('jQuery UI ' + test,
             '/third_party/js/jquery-ui/tests/unit/' + test + '/' + test +
                 '.html');
  })

})();
