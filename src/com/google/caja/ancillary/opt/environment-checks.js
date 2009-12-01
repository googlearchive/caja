// Copyright (C) 2009 Google Inc.
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
 * Collects information about the JavaScript environment into a JSON file which
 * can be used to inform user-agent specific optimizations as documented at
 * <a href="http://code.google.com/p/google-caja/wiki/UserAgentContext">
 * UserAgentContext</a>.
 *
 * @author mikesamuel@gmail.com
 */

"use strict";

var env = (function () {
  /**
   * Combines an expression with a fallback to use if the first expression
   * failes with an exception.
   */
  function alt(mayThrow, altValue) {
    return ('(function(){try{return(' + mayThrow + ');}catch(e){return('
            + altValue + ');}})()');
  }

  /**
   * Side-effect free JS expressions that give information about the environment
   * in which JS runs.
   */
  var codeSnippets = [
      // Get information about the browser that we can use when trying to
      // map a User-Agent request header to an environment file.
      'navigator.userAgent',
      'navigator.appName',
      'navigator.appVersion',
      'navigator.platform',
      // Check ES global definitions
      'typeof undefined',
      'Infinity === 1/0',
      'NaN !== NaN',
      //// Does window alias the global object?
      '!!this.window && this === window',
      //// Is EcmaScript 5 strict mode present?
      '!(function () { return this; }.call(null))',
      //// Check whether native implementations are available
      'typeof JSON',
      'typeof addEventListener',
      // IE makes a lot of its functions, objects.
      // Fun fact: but not ActiveXObject.
      'typeof attachEvent',
      '!!window.attachEvent',
      'typeof document.getElementsByClassName',
      'typeof document.documentElement.getElementsByClassName',
      '!!document.all',
      'typeof Date.now',
      // Is the extended createElement syntax available?
      alt("document.createElement('<input type=\"radio\">').type === 'radio'",
          'false'),
      // Is the styleSheet member available.
      // http//yuiblog.com/blog/2007/06/07/style/
      "typeof document.createElement('style').styleSheet",
      'typeof document.body.style.cssText',
      'typeof XMLHttpRequest',
      'typeof ActiveXObject',
      'typeof getComputedStyle',
      'typeof document.body.currentStyle',
      '!!document.body.currentStyle',
      'typeof document.documentElement.compareDocumentPosition',
      'typeof document.documentElement.contains',
      '!!document.documentElement.contains',
      'typeof document.createEvent',
      'typeof document.createRange',
      'typeof document.documentElement.doScroll',
      '!!typeof document.documentElement.doScroll',
      'typeof document.documentElement.getBoundingClientRect',
      '!!document.documentElement.getBoundingClientRect',
      '"sourceIndex" in document.documentElement',
      'typeof document.createEventObject',
      '!!document.createEventObject',
      'typeof Date.prototype.toISOString',
      'typeof Date.prototype.toJSON',
      'typeof Array.slice',
      'typeof Function.prototype.bind',
      'typeof Object.prototype.toSource',
      'typeof uneval',
      'typeof getSelection',
      '!!(document && document.selection)',
      //// Check for known bugs and inconsistencies
      // Do functions not leak dangerous info in negative indices?
      'void 0 === ((function(){})[-2])',
      // Do function expressions not muck with the local scope?
      'void 0 === ((function(){var b,a=function b(){};return b;})())',
      // Do function scope frames inherit from Object.prototype?
      // http://yura.thinkweb2.com/named-function-expressions/#spidermonkey-peculiarity
      ('0 === (function () {'
       + ' var toString = 0; return (function () { return toString; })();'
       + ' })()'),
      // Do exceptions scope properly?
      '(function(){var e=true;try{throw false;}catch(e){}return e;})()',
      // Are regex functions or objects?
      "typeof new RegExp('x')",
      // Are strings indexable
      "'a'==('a'[0])",
      // Are functions declared only if reachable?
      '(function(){var a;if(0)function a(){}return void 0===a;})()',
      // Is __proto__ defined for objects?
      'typeof ({}).__proto__',
      // Does setAttribute need only the two parameters?
      'document.body.setAttribute.length === 2',
      // Are format control characters lexically significant?
      'eval("\'\u200d\'").length === 1',
      // Does string.split work properly?
      "'a,,a'.split(',').length === 3"
  ];

  var environment = {};
  for (var i = 0, n = codeSnippets.length; i < n; ++i) {
    var codeSnippet = codeSnippets[i];
    var result = void 0;
    try {
      // Execute in the global scope
      result = (new Function('return (' + codeSnippet + ')'))();
    } catch (ex) {
      if (typeof console !== 'undefined' && console.warn) {
        console.warn('Error on (%s): %o', codeSnippet, ex);
      }
      continue;
    }
    if (result === void 0) {
      continue;
    }
    // Map the code snippet to the result it produces.
    environment[codeSnippet] = result;
  }
  return environment;
})();
