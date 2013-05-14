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
  function headerComment(element, comment) {
    if (comment !== '') {
      var commentEl = document.createElement('div');
      commentEl.className = 'comment';
      commentEl.textContent = comment;
      element.appendChild(commentEl);
    }
  }
  function hiddenComment(element, comment) {
    if (comment !== '') {
      element.title = comment;
    }
  }
  function makeCatalogOutput(list) {
    return {
      addGroup: function(label, comment) {
        var entry = list.appendChild(document.createElement('li'));
        entry.appendChild(document.createTextNode(label));
        headerComment(entry, comment);
        var sublist = entry.appendChild(document.createElement('ul'));
        return makeCatalogOutput(sublist);
      },
      addMiniGroup: function(label, comment) {
        var entry = list.appendChild(document.createElement('li'));
        var sublist = entry.appendChild(document.createElement('span'));
        entry.appendChild(document.createTextNode(' ' + label));
        headerComment(entry, comment);
        return makeMiniOutput(sublist);
      },
      addTest: function(url, label, longLabel, comment, manual, mayFail) {
        var entry = list.appendChild(document.createElement('li'));
        var a = entry.appendChild(document.createElement('a'));
        a.href = url;
        a.textContent = label;
        headerComment(entry, comment);
        if (manual) { a.className = 'manual'; }
      },
      addNonTest: function(label, comment) {
        var entry = list.appendChild(document.createElement('li'));
        var a = entry.appendChild(document.createElement('del'));
        a.textContent = label;
        headerComment(entry, comment);
      },
    };
  }

  function makeMiniOutput(list) {
    function space() {
      if (list.hasChildNodes()) {
        list.appendChild(document.createTextNode(' '));
      }
    }
    function addSubMini(label, comment) {
      space();
      var entry = list.appendChild(document.createElement('span'));
      hiddenComment(entry, comment);
      entry.appendChild(document.createTextNode(label + '['));
      var sublist = entry.appendChild(document.createElement('span'));
      entry.appendChild(document.createTextNode(']'));
      return makeMiniOutput(sublist);
    }
    return {
      addTest: function(url, label, longLabel, comment, manual, mayFail) {
        space();
        var a = list.appendChild(document.createElement('a'));
        a.href = url;
        a.textContent = label;
        hiddenComment(a, comment);
        if (manual) { a.className = 'manual'; }
      },
      addNonTest: function(label, comment) {
        space();
        var a = list.appendChild(document.createElement('del'));
        a.textContent = label;
        hiddenComment(a, comment);
      },
      addGroup: addSubMini,
      addMiniGroup: addSubMini
    };
  }

  function loadCatalog(id, url, callback) {
    var list = document.getElementById(id);
    var request = new XMLHttpRequest();
    request.open('GET', url, true);
    request.onreadystatechange = function() {
      if (request.readyState == 4) {
        parseTestCatalog(
            JSON.parse(request.responseText),
            makeCatalogOutput(list),
            false);
        if (callback) { callback(); }
      }
    };
    request.send();
  }

  loadCatalog('tests', 'browser-tests.json', function() {
    loadCatalog('tests', '../ses/ses-tests.json');
  });
  loadCatalog('thirdparty', 'third-party-tests.json');

})();
