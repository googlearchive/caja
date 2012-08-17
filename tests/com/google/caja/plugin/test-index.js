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
    ['es53-test-cajajs-invocation.js&minified=false'],
    ['es53-test-cajajs-invocation.js&minified=true'],
    ['es53-test-cajajs-version-skew-cajoled-module.js', 'es53'],
    ['es53-test-client-uri-rewriting.js'],
    ['es53-test-container-overflow.js'],
    ['es53-test-defensible-objects.js'],
    ['es53-test-domado-events.js'],
    ['es53-test-domado-foreign.js'],
    ['es53-test-domado-special.js'],
    ['es53-test-proxies.js', 'es53'],
    ['es53-test-relative-urls.js'],
    ['es53-test-taming-errors.js'],
    ['es53-test-taming-inout.js'],
    ['es53-test-taming-tamed.js'],
    ['es53-test-taming-untamed.js'],
    ['es53-test-unicode.js']
  ];

  var bare_tests = [
    'modules-test.html',
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

  function addModed(url, text, mode) {
    var html = '';
    if (!mode || mode === 'es53') {
      html += '[<a href="' + url + '&es5=false">es53</a>] ';
    } else {
      html += '[<s>es53</s>] ';
    }
    if (!mode || mode === 'es5') {
      html += '[<a href="' + url + '&es5=true">es5</a>] ';
    } else {
      html += '[<s>es5</s>] ';
    }
    html += text || url;
    addItem(html);
  }

  var i, item;

  for (i = 0; i < bare_tests.length; i++) {
    addBare(bare_tests[i]);
  }

  addSeparator();

  for (i = 0; i < test_cases.length; i++) {
    item = test_cases[i];
    addModed('browser-test-case.html?test-case=' + item[0], item[0], item[1]);
  }

  addSeparator();

  for (i = 0; i < test_drivers.length; i++) {
    item = test_drivers[i];
    addModed('browser-test-case.html?test-driver=' + item[0], item[0], item[1]);
  }

  addSeparator()

  addModed('cajajs-bare-test.html?', 'cajajs-bare-test.html', null);

})();
