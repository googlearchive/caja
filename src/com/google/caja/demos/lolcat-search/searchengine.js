/**
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @fileoverview
 * An interface to Google's AJAX Search APIs.
 *
 * @see http://code.google.com/apis/ajaxsearch/documentation/reference.html
 * @author mikesamuel@gmail.com
 */


function SearchEngine() {
  this.webSearch_ = new GwebSearch();
  this.imageSearch_ = new GimageSearch();
  this.imageSearch_.setRestriction(
      GSearch.RESTRICT_SAFESEARCH, GSearch.SAFESEARCH_STRICT);
}

SearchEngine.prototype.doSearch_
    = function (engine, query, processResult, callback) {
  engine.clearResults();
  engine.execute(query);
  engine.setSearchCompleteCallback(
      null,
      function () {
        var results = [];
        for (var i = 0, n = engine.results.length; i < n; ++i) {
          var result = processResult(engine.results[i]);
          result && results.push(result);
        }
        callback(results);
      });
};

SearchEngine.prototype.webSearch = function (query, callback) {
  return this.doSearch_(
      this.webSearch_,
      query,
      function (result) {
        return {
              url: result.unescapedUrl,
              titleHtml: result.title,
              snippetHtml: result.content
            };
      },
      callback);
};

SearchEngine.prototype.imageSearch = function (query, callback) {
  return this.doSearch_(
      this.imageSearch_,
      query,
      function (result) {
        var imageUrl = result.unescapedUrl;
        return imageUrl && /\.(jpg|gif|png)/.test(imageUrl)
            ? {
                url: result.unescapedUrl,
                titleHtml: result.title,
                snippetHtml: result.content
              }
            : null;
      },
      callback);
};

// Define what gadgets can access.
___.ctor(SearchEngine, undefined, 'SearchEngine');
___.all2(___.allowMethod, SearchEngine, ['webSearch', 'imageSearch']);
