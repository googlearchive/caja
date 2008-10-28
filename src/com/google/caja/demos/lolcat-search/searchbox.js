// Copyright (C) 2008 Google Inc.
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
 * @fileoverview
 * A search box that displays search results, and that delegates display of
 * a result to a separate gadget, the kittens gadget.
 *
 * @author mikesamuel@gmail.com
 */


/**
 * Injected by the container.
 * @type {SearchEngine}
 */
searchEngine;
/**
 * Takes a search result and does something with it.
 * This is injected by the container.
 * @type {Function}.
 */
resultConsumer;
/**
 * Search results as from {@code SearchEngine.webSearch}.
 * @type {Array.<Object>}
 */
var resultsOfLastSearch = [];

/**
 * Onsubmit handler for the search form from <tt>searchbox.html</tt>.
 * @param {HTMLFormElement} form
 */
function doSearch(button) {
  var form = button.getForm();
  var query = form.getElements().q.getValue().replace(
      new RegExp('^\\s+|\\s+$', 'g'), '');
  if (!query) {
    form.getElements().q.focus();
    return;
  }
  searchEngine.webSearch(
      query,
      // Called when the results arrive.
      function (results) {
        resultsOfLastSearch = results.slice(0);

        var resultList = document.getElementById('results');
        for (var child; (child = resultList.getFirstChild());) {
          resultList.removeChild(child);
        }
        // For each link, create an <li> tag containing a link.
        var n = results.length;
        if (!n) {
          resultList.innerHTML = eval(Template('<center>No results</center>'));
          return;
        }
        for (var i = 0; i < n; ++i) {
          var result = results[i];
          var li = document.createElement('LI');
          var snippetText = result.snippetHtml.replace(
              new RegExp('<\/?[A-Za-z][^>]*>', 'g'), ' ');
          var titleText = result.titleHtml.replace(new RegExp('<\/?[A-Za-z][^>]*>', 'g'), ' ');
          li.innerHTML = eval(Template(
              '<b>$titleText</b> &mdash; '
              + '<tt><a href="#">${result.url}</a></tt>'));
          li.addEventListener(
              'click', (function (i) {
                          return function () { return showResult(i); };
                        })(i));
          resultList.appendChild(li);
        }
      });
}

/**
 * Event handler that is called when a result link is clicked.
 * @param {number} index
 */
function showResult(index) {
  var result = resultsOfLastSearch[index];
  if (result) {
    resultConsumer(clone(result));
  }
  return false;
}

/**
 * Clones a raw object.
 * @param {Object} original
 * @return {Object}
 */
function clone(original) {
  var cloned = {};
  caja.forOwnKeys(original, function(k) {
    cloned[k] = original[k];
  });
  return cloned;
}
