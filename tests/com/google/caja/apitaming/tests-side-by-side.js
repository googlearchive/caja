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

  function addIfr(id, url) {
    var ifr = document.createElement('iframe');
    ifr.setAttribute('src', url);
    document.getElementById(id).appendChild(ifr);
  }

  var testSnippet = getUrlParam('test')
      ? 'test=' + getUrlParam('test')
      : 'testsList=' + getUrlParam('testsList');

  var cajaServer = getCajaServer();

  addIfr('unc',  './tests-uncajoled.html?' + testSnippet);
  addIfr('es53', './tests-cajoled.html?' + testSnippet + '&cajaServer=' + cajaServer + '&forceES5Mode=false');
  addIfr('es5',  './tests-cajoled.html?' + testSnippet + '&cajaServer=' + cajaServer + '&forceES5Mode=true');
})();



