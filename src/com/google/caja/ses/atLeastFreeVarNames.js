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
 * // provides ses.limitSrcCharset
 * @author Mark S. Miller
 * @requires JSON
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

  // Section 7.2 ES5 recognizes the following whitespace characters
  // FEFF           ; BOM
  // 0009 000B 000C ; White_Space # Cc
  // 0020           ; White_Space # Zs       SPACE
  // 00A0           ; White_Space # Zs       NO-BREAK SPACE
  // 1680           ; White_Space # Zs       OGHAM SPACE MARK
  // 180E           ; White_Space # Zs       MONGOLIAN VOWEL SEPARATOR
  // 2000..200A     ; White_Space # Zs  [11] EN QUAD..HAIR SPACE
  // 2028           ; White_Space # Zl       LINE SEPARATOR
  // 2029           ; White_Space # Zp       PARAGRAPH SEPARATOR
  // 202F           ; White_Space # Zs       NARROW NO-BREAK SPACE
  // 205F           ; White_Space # Zs       MEDIUM MATHEMATICAL SPACE
  // 3000           ; White_Space # Zs       IDEOGRAPHIC SPACE

  // Unicode characters which have the Zs property are an open set and can
  // grow.  Not all versions of a browser treat Zs characters the same.
  // The trade off is as follows:
  //   * if SES treats a character as non-whitespace which the browser
  //      treats as whitespace, a sandboxed program would be able to
  //      break out of the sandbox.  SES avoids this by encoding any
  //      characters outside the range of well understood characters
  //      and disallowing unusual whitespace characters which are
  //      rarely used and may be treated non-uniformly by browsers.
  //   * if SES treats a character as whitespace which the browser
  //      treats as non-whitespace, a sandboxed program will be able
  //      to break out of the SES sandbox.  However, at worst it might
  //      be able to read, write and execute globals which have the
  //      corresponding whitespace character.  This is a limited
  //      breach because it is exceedingly rare for browser functions
  //      or powerful host functions to have names which contain
  //      potential whitespace characters.  At worst, sandboxed
  //      programs would be able to communicate with each other.
  //
  // We are conservative with the whitespace characters we accept.  We
  // deny whitespace > u00A0 to make unexpected functional differences
  // in sandboxed programs on browsers even if it was safe to allow them.
  var OTHER_WHITESPACE = new RegExp(
    '[\\uFEFF\\u1680\\u180E\\u2000-\\u2009\\u200a'
    + '\\u2028\\u2029\\u200f\\u205F\\u3000]');

  /**
   * We use this to limit the input text to ascii only text or \u00A0
   * which is no-break-space. All other characters are encoded using
   * backslash-u escapes.
   */
  ses.limitSrcCharset = function(programSrc) {
    if (OTHER_WHITESPACE.test(programSrc)) {
      return { error: 'Disallowing unusual unicode whitespace characters' };
    }
    programSrc = programSrc.replace(/([\u0080-\u009f\u00a1-\uffff])/g,
      function(_, u) {
        return '\\u' + ('0000' + u.charCodeAt(0).toString(16)).slice(-4);
      });
    return { programSrc: programSrc };
  };

  /**
   * Return a regexp that can be used repeatedly to scan for the next
   * identifier. It works correctly in concert with
   * ses.limitSrcCharset above.
   *
   * Note that ES6 section 11.8.4 extends the syntax of identifiers to
   * include the unicode escape sequence
   * backslash-u-opencurly-hexdigits-closecurly. By including it in
   * this pattern, we ensure that we still recognize an identifier as
   * a unit. However, this is not valid JSON syntax, so the JSON.parse
   * will reject these with an exception rather than decode it into an
   * identifier string.
   */
  function SHOULD_MATCH_IDENTIFIER() {
    return /(?:\w|\\u[0-9a-fA-F]{4}|\\u{[0-9a-fA-F]*}|\$)+/g;
  }

  /**
   * Sequences beginning with a digit will pass the
   * SHOULD_MATCH_IDENTIFIER() pattern but are trivially not
   * identifiers. This returns true only if name does not begin with a
   * digit.
   */
  function filterIdentifier(name) {
    return /^\D/.test(name);
  }

  //////////////// END KLUDGE SWITCHES ///////////

  ses.DISABLE_SECURITY_FOR_DEBUGGER = false;

  ses.atLeastFreeVarNames = function atLeastFreeVarNames(programSrc) {
    programSrc = ''+programSrc;
    var limited = ses.limitSrcCharset(programSrc);
    if (!('programSrc' in limited)) {
      throw new EvalError(limited.error);
    } else {
      programSrc = limited.programSrc;
    }
    // Now that we've temporarily limited our attention to ascii...
    var regexp = SHOULD_MATCH_IDENTIFIER();
    // Once we decide this file can depends on ES5, the following line
    // should say "... = Object.create(null);" rather than "... = {};"
    var result = [];
    var found = new Set();
    // webkit js debuggers rely on ambient global eval
    // http://code.google.com/p/chromium/issues/detail?id=145871
    if (ses.DISABLE_SECURITY_FOR_DEBUGGER) {
      found.add('eval');
    }
    var a;
    while ((a = regexp.exec(programSrc))) {
      // Note that we could have avoided the while loop by doing
      // programSrc.match(regexp), except that then we'd need
      // temporary storage proportional to the total number of
      // apparent identifiers, rather than the total number of
      // apparently unique identifiers.
      var name = a[0];

      if (filterIdentifier(name)) {
        // Parse \u escapes occurring inside the name. No other special
        // characters can occur, because the input is known to match
        // SHOULD_MATCH_IDENTIFIER(). SHOULD_MATCH_IDENTIFIER() will
        // admit backslash-u-opencurly-hexdigits-closecurly which
        // JSON.parse will reject with an error. We could and probably
        // should eventually admit this standard non-JSON escape
        // sequence, but for simplicity and lack of current demand, we
        // do not yet do so.
        try {
          name = JSON.parse('"' + name + '"');
        } catch (err) {
          if (err instanceof SyntaxError) {
            // Since we are rejecting a valid program, at least make
            // the diagnostic more informative.
            err.message += ' in ' + name;
          }
          throw err;
        }

        if (!found.has(name)) {
          result.push(name);
          found.add(name);
        }
      }
    }
    return result;
  };

})();
