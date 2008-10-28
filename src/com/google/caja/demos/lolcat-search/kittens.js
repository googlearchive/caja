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
 * A gadget that shows a picture of a kitten.
 *
 * @author mikesamuel@gmail.com
 */


/** @type {SearchEngine} */
searchEngine;

/**
 * @param {string} query a search query.
 * @param {string} snippet a search result snippet.
 */
exports.showKitten = function showKitten(result) {
  var title = result.titleHtml.replace(new RegExp('<\/?\\w[^>]*>', 'g'), '');
  var snippet = result.snippetHtml.replace(new RegExp('<\/?\\w[^>]*>', 'g'), '');

  // Make sure that the kittens table shows a loading image until the kitty is
  // loaded.
  // This appears to apply to all tables, but will only apply to tables that
  // have the gadget's DOM prefix as a class.
  renderKittenTable('loading.jpg', snippet);

  // Search for cat pictures.
  searchEngine.imageSearch(
      '(+kitten OR +cat) ' + title,
      function (imageResults) {
        var n = imageResults.length;
        if (!n) {
          renderKittenTable('error.jpg', snippet);
          return;
        }

        // Pick one at random.
        var k = 0;  // (Math.random() * n) | 0;  FAKE FOR DEMO
        console.log('chose ' + k + ' from ' + imageResults.length);

        // Display it.
        renderKittenTable(imageResults[k].url, snippet);
      });
};

function renderKittenTable(imageUrl, snippet) {
  document.getElementById('base').innerHTML = eval(Template(
      '<table><tr><td align=center><img src="${imageUrl}"></tr>'
      + '<tr><td class=snippet align=center>${katTranzlator(snippet || "")}'
      + '</table>'));
}
