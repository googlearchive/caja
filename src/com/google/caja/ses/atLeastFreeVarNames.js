// Copyright (C) 2011 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @fileoverview Export a {@code ses.atLeastFreeVarNames} function for
 * internal use by the SES-on-ES5 implementation, which enumerates at
 * least the identifiers which occur freely in a source text string.
 *
 * <p>Assumes only ES3. Compatible with ES5, ES5-strict, or
 * anticipated ES6.
 *
 * // provides ses.atLeastFreeVarNames
 * @author Mark S. Miller
 * @requires StringMap
 * @overrides ses, atLeastFreeVarNamesModule
 */
var ses;

/**
 * Calling {@code ses.atLeastFreeVarNames} on a {@code programSrc}
 * string argument, the result should include at least all the free
 * variable names of {@code programSrc} as own properties. It is
 * harmless to include other strings as well.
 *
 * <p>Assuming a programSrc that parses as a strict Program,
 * atLeastFreeVarNames(programSrc) returns a Record whose enumerable
 * own property names must include the names of all the free variables
 * occuring in programSrc. It can include as many other strings as is
 * convenient so long as it includes these. The value of each of these
 * properties should be {@code true}.
 *
 * <p>TODO(erights): On platforms that support Proxies (currently only
 * FF4 and later), we should stop using atLeastFreeVarNames, since a
 * {@code with(aProxy) {...}} should reliably intercept all free
 * variable accesses without needing any prior scan.
 */
(function atLeastFreeVarNamesModule() {
  "use strict";

   if (!ses) { ses = {}; }

  /////////////// KLUDGE SWITCHES ///////////////

  /**
   * Currently we use this to limit the input text to ascii only
   * without backslash-u escapes, in order to simply our identifier
   * gathering.
   *
   * <p>This is only a temporary development hack. TODO(erights): fix.
   */
  function LIMIT_SRC(programSrc) {
    if ((/[^\u0000-\u007f]/).test(programSrc)) {
      throw new EvalError('Non-ascii text not yet supported');
    }
    if ((/\\u/).test(programSrc)) {
      throw new EvalError('Backslash-u escape encoded text not yet supported');
    }
  }

  /**
   * Return a regexp that can be used repeatedly to scan for the next
   * identifier.
   *
   * <p>The current implementation is safe only because of the above
   * LIMIT_SRC. To do this right takes quite a lot of unicode
   * machinery. See the "Identifier" production at
   * http://es-lab.googlecode.com/svn/trunk/src/parser/es5parser.ojs
   * which depends on
   * http://es-lab.googlecode.com/svn/trunk/src/parser/unicode.js
   *
   * <p>This is only a temporary development hack. TODO(erights): fix.
   * 
   * If this regexp is changed compileExprLater.js should be checked for
   * correct escaping of freeNames.
   * 
   */
  function SHOULD_MATCH_IDENTIFIER() { return (/(\w|\$)+/g); }


  //////////////// END KLUDGE SWITCHES ///////////

  ses.atLeastFreeVarNames = function atLeastFreeVarNames(programSrc) {
    programSrc = String(programSrc);
    LIMIT_SRC(programSrc);
    // Now that we've temporarily limited our attention to ascii...
    var regexp = SHOULD_MATCH_IDENTIFIER();
    // Once we decide this file can depends on ES5, the following line
    // should say "... = Object.create(null);" rather than "... = {};"
    var result = [];
    var found = StringMap();
    var a;
    while ((a = regexp.exec(programSrc))) {
      // Note that we could have avoided the while loop by doing
      // programSrc.match(regexp), except that then we'd need
      // temporary storage proportional to the total number of
      // apparent identifiers, rather than the total number of
      // apparently unique identifiers.
      var name = a[0];

      if (!found.has(name)) {
        result.push(name);
        found.set(name, true);
      }
    }
    return result;
  };

})();
