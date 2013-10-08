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

(function() {

  function a(str, url) {
    var l = document.createElement('a');
    l.innerHTML = str;
    l.setAttribute('href', url);
    return l;
  }

  function td(n) {
    var t = document.createElement('td');
    t.appendChild(n);
    return t;
  }

  function tr() {
    var r = document.createElement('tr');
    for (var i = 0; i < arguments.length; i++) {
      r.appendChild(arguments[i]);
    }
    return r;
  }

  function code(str) {
    var s = document.createElement('code');
    s.innerHTML = str;
    return s;
  }

  var cajaServer = getCajaServer();

  getTests(function(tests) {
    for (var i = 0; i < tests.length; i++) {
      document.getElementById('t').appendChild(tr(
        td(code(tests[i])),
        td(a('[sbs]',  './tests-side-by-side.html?cajaServer=' + cajaServer + '&test=' + tests[i])),
        td(a('[unc]',  './tests-uncajoled.html?test=' + tests[i])),
        td(a('[caja]',  './tests-cajoled.html?test=' + tests[i] + '&cajaServer=' + cajaServer))
      ));
    }
  });
})();