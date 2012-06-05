// Copyright (C) 2011 Google Inc.
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
 * JavaScript support for client-side CSS sanitization.
 * The CSS property schema API is defined in CssPropertyPatterns.java which
 * is used to generate css-defs.js.
 *
 * @author mikesamuel@gmail.com
 * \@requires CSS_PROP_BIT_ALLOWED_IN_LINK
 * \@requires CSS_PROP_BIT_HASH_VALUE
 * \@requires CSS_PROP_BIT_NEGATIVE_QUANTITY
 * \@requires CSS_PROP_BIT_QSTRING_CONTENT
 * \@requires CSS_PROP_BIT_QSTRING_URL
 * \@requires CSS_PROP_BIT_QUANTITY
 * \@requires CSS_PROP_BIT_Z_INDEX
 * \@requires cssSchema
 * \@requires decodeCss
 * \@requires html4
 * \@overrides window
 * \@requires parseCssStylesheet
 * \@provides sanitizeCssProperty
 * \@provides sanitizeCssSelectors
 * \@provides sanitizeStylesheet
 */

/**
 * Given a series of normalized CSS tokens, applies a property schema, as
 * defined in CssPropertyPatterns.java, and sanitizes the tokens in place.
 * @param property a property name.
 * @param propertySchema a property of cssSchema as defined by
 *    CssPropertyPatterns.java
 * @param tokens as parsed by lexCss.  Modified in place.
 * @param opt_naiveUriRewriter a URI rewriter; an object with a "rewrite"
 *     function that takes a URL and returns a safe URL.
 */
var sanitizeCssProperty = (function () {
  var NOEFFECT_URL = 'url("about:blank")';
  /**
   * The set of characters that need to be normalized inside url("...").
   * We normalize newlines because they are not allowed inside quoted strings,
   * normalize quote characters, angle-brackets, and asterisks because they
   * could be used to break out of the URL or introduce targets for CSS
   * error recovery.  We normalize parentheses since they delimit unquoted
   * URLs and calls and could be a target for error recovery.
   */
  var NORM_URL_REGEXP = /[\n\f\r\"\'()*<>]/g;
  /** The replacements for NORM_URL_REGEXP. */
  var NORM_URL_REPLACEMENTS = {
    '\n': '%0a',
    '\f': '%0c',
    '\r': '%0d',
    '"':  '%22',
    '\'': '%27',
    '(':  '%28',
    ')':  '%29',
    '*':  '%2a',
    '<':  '%3c',
    '>':  '%3e'
  };


  function normalizeUrl(s) {
    if ('string' === typeof s) {
      return 'url("' + s.replace(NORM_URL_REGEXP, normalizeUrlChar) + '")';
    } else {
      return NOEFFECT_URL;
    }
  }
  function normalizeUrlChar(ch) {
    return NORM_URL_REPLACEMENTS[ch];
  }

  // From RFC3986
  var URI_SCHEME_RE = new RegExp(
      '^' +
      '(?:' +
        '([^:\/?# ]+)' +         // scheme
      ':)?'
  );

  var ALLOWED_URI_SCHEMES = /^(?:https?|mailto)$/i;

  function safeUri(uri, prop, naiveUriRewriter) {
    if (!naiveUriRewriter) { return null; }
    var parsed = ('' + uri).match(URI_SCHEME_RE);
    if (parsed && (!parsed[1] || ALLOWED_URI_SCHEMES.test(parsed[1]))) {
      return naiveUriRewriter(uri, prop);
    } else {
      return null;
    }
  }

  function unionArrays(arrs) {
    var map = {};
    for (var i = arrs.length; --i >= 0;) {
      var arr = arrs[i];
      for (var j = arr.length; --j >= 0;) {
        map[arr[j]] = ALLOWED_LITERAL;
      }
    }
    return map;
  }

  /**
   * Normalize tokens within a function call they can match against
   * cssSchema[propName].cssExtra.
   * @return the exclusive end in tokens of the function call.
   */
  function normalizeFunctionCall(tokens, start) {
    var parenDepth = 1, end = start + 1, n = tokens.length;
    while (end < n && parenDepth) {
      // TODO: Can URLs appear in functions?
      var token = tokens[end++];
      parenDepth += (token === '(' ? 1 : token === ')' ? -1 : 0);
    }
    return end;
  }

  // Used as map value to avoid hasOwnProperty checks.
  var ALLOWED_LITERAL = {};

  return function (property, propertySchema, tokens, opt_naiveUriRewriter) {
    var propBits = propertySchema.cssPropBits;
    // Used to determine whether to treat quoted strings as URLs or
    // plain text content, and whether unrecognized keywords can be quoted
    // to treate ['Arial', 'Black'] equivalently to ['"Arial Black"'].
    var qstringBits = propBits & (
        CSS_PROP_BIT_QSTRING_CONTENT | CSS_PROP_BIT_QSTRING_URL);
    // TODO(mikesamuel): Figure out what to do with props like
    // content that admit both URLs and strings.

    // Used to join unquoted keywords into a single quoted string.
    var lastQuoted = NaN;
    var i = 0, k = 0;
    for (;i < tokens.length; ++i) {
      // Has the effect of normalizing hex digits, keywords,
      // and function names.
      var token = tokens[i].toLowerCase();
      var cc = token.charCodeAt(0), cc1, cc2, isnum1, isnum2, end;
      var litGroup, litMap;
      token = (
        // Strip out spaces.  Normally cssparser.js dumps these, but we
        // strip them out in case the content doesn't come via cssparser.js.
        (cc === ' '.charCodeAt(0)) ? ''
        : (cc === '"'.charCodeAt(0)) ? (  // Quoted string.
          (qstringBits === CSS_PROP_BIT_QSTRING_URL && opt_naiveUriRewriter)
          // Sanitize and convert to url("...") syntax.
          // Treat url content as case-sensitive.
          ? (normalizeUrl(safeUri(
                decodeCss(tokens[i].substring(1, token.length - 1)),
                property,
                opt_naiveUriRewriter)))
          // Drop if plain text content strings not allowed.
          : (qstringBits === CSS_PROP_BIT_QSTRING_CONTENT) ? token : '')
        // Preserve hash color literals if allowed.
        : (cc === '#'.charCodeAt(0) && /^#(?:[0-9a-f]{3}){1,2}$/.test(token))
        ? (propBits & CSS_PROP_BIT_HASH_VALUE ? token : '')
        : ('0'.charCodeAt(0) <= cc && cc <= '9'.charCodeAt(0))
        // A number starting with a digit.
        ? ((propBits & CSS_PROP_BIT_QUANTITY)
          ? ((propBits & CSS_PROP_BIT_Z_INDEX)
            ? (token.match(/^\d{1,7}$/) ? token : '')
            : token)
          : '')
        // Normalize quantities so they don't start with a '.' or '+' sign and
        // make sure they all have an integer component so can't be confused
        // with a dotted identifier.
        // This can't be done in the lexer since ".4" is a valid rule part.
        : (cc1 = token.charCodeAt(1),
           cc2 = token.charCodeAt(2),
           isnum1 = '0'.charCodeAt(0) <= cc1 && cc1 <= '9'.charCodeAt(0),
           isnum2 = '0'.charCodeAt(0) <= cc2 && cc2 <= '9'.charCodeAt(0),
           // +.5 -> 0.5 if allowed.
           (cc === '+'.charCodeAt(0)
            && (isnum1 || (cc1 === '.'.charCodeAt(0) && isnum2))))
          ? ((propBits & CSS_PROP_BIT_QUANTITY)
            ? ((propBits & CSS_PROP_BIT_Z_INDEX)
              ? (token.match(/^\+\d{1,7}$/) ? token : '')
              : ((isnum1 ? '' : '0') + token.substring(1)))
            : '')
        // -.5 -> -0.5 if allowed otherwise -> 0 if quantities allowed.
        : (cc === '-'.charCodeAt(0)
           && (isnum1 || (cc1 === '.'.charCodeAt(0) && isnum2)))
          ? ((propBits & CSS_PROP_BIT_NEGATIVE_QUANTITY)
             ? ((propBits & CSS_PROP_BIT_Z_INDEX)
               ? (token.match(/^\-\d{1,7}$/) ? token : '')
               : ((isnum1 ? '-' : '-0') + token.substring(1)))
             : ((propBits & CSS_PROP_BIT_QUANTITY) ? '0' : ''))
        // .5 -> 0.5 if allowed.
        : (cc === '.'.charCodeAt(0) && isnum1)
        ? ((propBits & CSS_PROP_BIT_QUANTITY) ? '0' + token : '')
        // Handle url("...") by rewriting the body.
        : ('url(' === token.substring(0, 4))
        ? ((opt_naiveUriRewriter && (qstringBits & CSS_PROP_BIT_QSTRING_URL))
           ? normalizeUrl(safeUri(
                tokens[i].substring(5, token.length - 2),
                property,
                opt_naiveUriRewriter))
           : '')
        // Handle func(...) and literal tokens
        // such as keywords and punctuation.
        : (
          // Step 1. Combine func(...) into something that can be compared
          // against propertySchema.cssExtra.
          (token.charAt(token.length-1) === '(')
          && (end = normalizeFunctionCall(tokens, i),
              // When tokens is
              //   ['x', ' ', 'rgb(', '255', ',', '0', ',', '0', ')', ' ', 'y']
              // and i is the index of 'rgb(' and end is the index of ')'
              // splices tokens to where i now is the index of the whole call:
              //   ['x', ' ', 'rgb( 255 , 0 , 0 )', ' ', 'y']
              tokens.splice(i, end - i,
                            token = tokens.slice(i, end).join(' '))),
          litGroup = propertySchema.cssLitGroup,
          litMap = (litGroup
                    ? (propertySchema.cssLitMap
                       // Lazily compute the union from litGroup.
                       || (propertySchema.cssLitMap = unionArrays(litGroup)))
                    : ALLOWED_LITERAL),  // A convenient empty object.
          (litMap[token] === ALLOWED_LITERAL
           || propertySchema.cssExtra && propertySchema.cssExtra.test(token)))
          // Token is in the literal map or matches extra.
          ? token
          : (/^\w+$/.test(token)
             && (qstringBits === CSS_PROP_BIT_QSTRING_CONTENT))
          // Quote unrecognized keywords so font names like
          //    Arial Bold
          // ->
          //    "Arial Bold"
          ? (lastQuoted+1 === k
             // If the last token was also a keyword that was quoted, then
             // combine this token into that.
             ? (tokens[lastQuoted] = tokens[lastQuoted]
                .substring(0, tokens[lastQuoted].length-1) + ' ' + token + '"',
                token = '')
             : (lastQuoted = k, '"' + token + '"'))
          // Disallowed.
          : '');
      if (token) {
        tokens[k++] = token;
      }
    }
    // For single URL properties, if the URL failed to pass the sanitizer,
    // then just drop it.
    if (k === 1 && tokens[0] === NOEFFECT_URL) { k = 0; }
    tokens.length = k;
  };
})();

/**
 * Given a series of tokens, returns two lists of sanitized selectors.
 * @param {Array.<string>} selectors In the form produces by csslexer.js.
 * @param {string} suffix a suffix that is added to all IDs and which is
 *    used as a CLASS names so that the returned selectors will only match
 *    nodes under one with suffix as a class name.
 *    If suffix is {@code "sfx"}, the selector
 *    {@code ["a", "#foo", " ", "b", ".bar"]} will be namespaced to
 *    {@code [".sfx", " ", "a", "#foo-sfx", " ", "b", ".bar"]}.
 * @return {Array.<Array.<string>>} an array of length 2 where the zeroeth
 *    element contains history-insensitive selectors and the first element
 *    contains history-sensitive selectors.
 */
function sanitizeCssSelectors(selectors, suffix) {
  // Produce two distinct lists of selectors to sequester selectors that are
  // history sensitive (:visited), so that we can disallow properties in the
  // property groups for the history sensitive ones.
  var historySensitiveSelectors = [];
  var historyInsensitiveSelectors = [];

  // Remove any spaces that are not operators.
  var k = 0, i;
  for (i = 0; i < selectors.length; ++i) {
    if (!(selectors[i] == ' '
          && (selectors[i-1] == '>' || selectors[i+1] == '>'))) {
      selectors[k++] = selectors[i];
    }
  }
  selectors.length = k;

  // Split around commas.  If there is an error in one of the comma separated
  // bits, we throw the whole away, but the failure of one selector does not
  // affect others.
  var n = selectors.length, start = 0;
  for (i = 0; i < n; ++i) {
    if (selectors[i] == ',') {
      processSelector(start, i);
      start = i+1;
    }
  }
  processSelector(start, n);


  function processSelector(start, end) {
    var historySensitive = false;

    // Space around commas is not an operator.
    if (selectors[start] === ' ') { ++start; }
    if (end-1 !== start && selectors[end] === ' ') { --end; }

    // Split the selector into element selectors, content around
    // space (ancestor operator) and '>' (descendant operator).
    var out = [];
    var lastOperator = start;
    var elSelector = '';
    for (var i = start; i < end; ++i) {
      var tok = selectors[i];
      var isChild = (tok === '>');
      if (isChild || tok === ' ') {
        // We've found the end of a single link in the selector chain.
        // We disallow absolute positions relative to html.
        elSelector = processElementSelector(lastOperator, i, false);
        if (!elSelector || (isChild && /^html/i.test(elSelector))) {
          return;
        }
        lastOperator = i+1;
        out.push(elSelector, isChild ? ' > ' : ' ');
      }
    }
    elSelector = processElementSelector(lastOperator, end, true);
    if (!elSelector) { return; }
    out.push(elSelector);

    function processElementSelector(start, end, last) {
      var debugStart = start, debugEnd = end;

      // Split the element selector into three parts.
      // DIV.foo#bar:hover
      //    ^       ^
      // el classes pseudo
      var element, classId, pseudoSelector, tok, elType;
      element = '';
      if (start < end) {
        tok = selectors[start].toLowerCase();
        if (tok === '*'
            || (tok === 'body' && start+1 !== end && !last)
            || ('number' === typeof (elType = html4.ELEMENTS[tok])
                && !(elType & html4.eflags['UNSAFE']))) {
          ++start;
          element = tok;
        }
      }
      classId = '';
      while (start < end) {
        tok = selectors[start];
        if (tok.charAt(0) === '#') {
          if (/^#_|__$|[^#0-9A-Za-z:_\-]/.test(tok)) { return null; }
          // Rewrite ID elements to include the suffix.
          classId += tok + '-' + suffix;
        } else if (tok === '.') {
          if (++start < end
              && /^[0-9A-Za-z:_\-]+$/.test(tok = selectors[start])
              && !/^_|__$/.test(tok)) {
            classId += '.' + tok;
          } else {
            return null;
          }
        } else {
          break;
        }
        ++start;
      }
      pseudoSelector = '';
      if (start < end && selectors[start] === ':') {
        tok = selectors[++start];
        if (tok === 'visited' || tok === 'link') {
          if (!/^[a*]?$/.test(element)) {
            return null;
          }
          historySensitive = true;
          pseudoSelector = ':' + tok;
          element = 'a';
          ++start;
        }
      }
      if (start === end) {
        return element + classId + pseudoSelector;
      }
      return null;
    }


    var safeSelector = out.join('');
    if (/^body\b/.test(safeSelector)) {
      // Substitute the class that is attached to pseudo body elements for
      // the body element.
      safeSelector = '.vdoc-body___.' + suffix + safeSelector.substring(4);
    } else {
      // Namespace the selector so that it only matches under
      // a node with suffix in its CLASS attribute.
      safeSelector = '.' + suffix + ' ' + safeSelector;
    }

    (historySensitive
     ? historySensitiveSelectors
     : historyInsensitiveSelectors).push(safeSelector);
  }

  return [historyInsensitiveSelectors, historySensitiveSelectors];
}

var sanitizeStylesheet = (function () {
  var allowed = {};
  var cssMediaTypeWhitelist = {
    'braille': allowed,
    'embossed': allowed,
    'handheld': allowed,
    'print': allowed,
    'projection': allowed,
    'screen': allowed,
    'speech': allowed,
    'tty': allowed,
    'tv': allowed
  };

  /**
   * Given a series of sanitized tokens, removes any properties that would
   * leak user history if allowed to style links differently depending on
   * whether the linked URL is in the user's browser history.
   * @param {Array.<string>} blockOfProperties
   */
  function sanitizeHistorySensitive(blockOfProperties) {
    var elide = false;
    for (var i = 0, n = blockOfProperties.length; i < n-1; ++i) {
      var token = blockOfProperties[i];
      if (':' === blockOfProperties[i+1]) {
        elide = !(cssSchema[token].cssPropBits & CSS_PROP_BIT_ALLOWED_IN_LINK);
      }
      if (elide) { blockOfProperties[i] = ''; }
      if (';' === token) { elide = false; }
    }
    return blockOfProperties.join('');
  }

  /**
   * @param {string} cssText a string containing a CSS stylesheet.
   * @param {string} suffix a suffix that is added to all IDs and which is
   *    used as a CLASS names so that the returned selectors will only match
   *    nodes under one with suffix as a class name.
   *    If suffix is {@code "sfx"}, the selector
   *    {@code ["a", "#foo", " ", "b", ".bar"]} will be namespaced to
   *    {@code [".sfx", " ", "a", "#foo-sfx", " ", "b", ".bar"]}.
   * @param {function(string, string)} opt_naiveUriRewriter maps URLs of media
   *    (images, sounds) that appear as CSS property values to sanitized
   *    URLs or null if the URL should not be allowed as an external media
   *    file in sanitized CSS.
   */
  return function /*sanitizeStylesheet*/(
       cssText, suffix, opt_naiveUriRewriter) {
    var safeCss = void 0;
    // A stack describing the { ... } regions.
    // Null elements indicate blocks that should not be emitted.
    var blockStack = [];
    // True when the content of the current block should be left off safeCss.
    var elide = false;
    parseCssStylesheet(
        cssText,
        {
          startStylesheet: function () {
            safeCss = [];
          },
          endStylesheet: function () {
          },
          startAtrule: function (atIdent, headerArray) {
            if (elide) {
              atIdent = null;
            } else if (atIdent === '@media') {
              headerArray = headerArray.filter(
                function (mediaType) {
                  return cssMediaTypeWhitelist[mediaType] == allowed;
                });
              if (headerArray.length) {
                safeCss.push(atIdent, headerArray.join(','), '{');
              } else {
                atIdent = null;
              }
            } else {
              if (atIdent === '@import') {
                // TODO: Use a logger instead.
                if (window.console) {
                  window.console.log(
                      '@import ' + headerArray.join(' ') + ' elided');
                }
              }
              atIdent = null;  // Elide the block.
            }
            elide = !atIdent;
            blockStack.push(atIdent);
          },
          endAtrule: function () {
            var atIdent = blockStack.pop();
            if (!elide) {
              safeCss.push(';');
            }
            checkElide();
          },
          startBlock: function () {
            // There are no bare blocks in CSS, so we do not change the
            // block stack here, but instead in the events that bracket
            // blocks.
            if (!elide) {
              safeCss.push('{');
            }
          },
          endBlock: function () {
            if (!elide) {
              safeCss.push('}');
              elide = true;  // skip any semicolon from endAtRule.
            }
          },
          startRuleset: function (selectorArray) {
            var historySensitiveSelectors = void 0;
            var removeHistoryInsensitiveSelectors = false;
            if (!elide) {
              var selectors = sanitizeCssSelectors(selectorArray, suffix);
              var historyInsensitiveSelectors = selectors[0];
              historySensitiveSelectors = selectors[1];
              if (!historyInsensitiveSelectors.length
                  && !historySensitiveSelectors.length) {
                elide = true;
              } else {
                var selector = historyInsensitiveSelectors.join(', ');
                if (!selector) {
                  // If we have only history sensitive selectors,
                  // use an impossible rule so that we can capture the content
                  // for later processing by
                  // history insenstive content for use below.
                  selector = 'head > html';
                  removeHistoryInsensitiveSelectors = true;
                }
                safeCss.push(selector, '{');
              }
            }
            blockStack.push(
                elide
                ? null
                // Sometimes a single list of selectors is split in two,
                //   div, a:visited
                // because we want to allow some properties for DIV that
                // we don't want to allow for A:VISITED to avoid leaking
                // user history.
                // Store the history sensitive selectors and the position
                // where the block starts so we can later create a copy
                // of the permissive tokens, and filter it to handle the
                // history sensitive case.
                : {
                    historySensitiveSelectors: historySensitiveSelectors,
                    endOfSelectors: safeCss.length - 1,  // 1 is open curly
                    removeHistoryInsensitiveSelectors:
                       removeHistoryInsensitiveSelectors
                  });
          },
          endRuleset: function () {
            var rules = blockStack.pop();
            var propertiesEnd = safeCss.length;
            if (!elide) {
              safeCss.push('}');
              if (rules) {
                var extraSelectors = rules.historySensitiveSelectors;
                if (extraSelectors.length) {
                  var propertyGroupTokens = safeCss.slice(rules.endOfSelectors);
                  safeCss.push(extraSelectors.join(', '),
                               sanitizeHistorySensitive(propertyGroupTokens));
                }
              }
            }
            if (rules && rules.removeHistoryInsensitiveSelectors) {
              safeCss.splice(
                // -1 and +1 account for curly braces.
                rules.endOfSelectors - 1, propertiesEnd + 1);
            }
            checkElide();
          },
          declaration: function (property, valueArray) {
            if (!elide) {
              var schema = cssSchema[property];
              if (schema) {
                sanitizeCssProperty(property, schema, valueArray, opt_naiveUriRewriter);
                if (valueArray.length) {
                  safeCss.push(property, ':', valueArray.join(' '), ';');
                }
              }
            }
          }
        });
    function checkElide() {
      elide = blockStack.length !== 0
          && blockStack[blockStack.length-1] !== null;
    }
    return safeCss.join('');
  };
})();

// Exports for closure compiler.
if (typeof window !== 'undefined') {
  window['sanitizeCssProperty'] = sanitizeCssProperty;
  window['sanitizeCssSelectors'] = sanitizeCssSelectors;
  window['sanitizeStylesheet'] = sanitizeStylesheet;
}
