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
 * Supporting scripts for the cajoling testbed.
 *
 * @author mikesamuel@gmail.com
 * @overrides prettyPrint, prettyPrintOne
 * @provides indentAndWrapCode
 */

if ('undefined' === typeof prettyPrintOne) {
  // So it works without prettyprinting when disconnected from the network.
  prettyPrintOne = function (html) { return html; };
  prettyPrint = function () {};
}


/**
 * Given generated source code, identify indented blocks, and wrap them
 * in divs with left margins so that code wraps nicely, but maintains
 * indentation for subsequent lines.
 * @param {string} code a plain text string.
 * @return {string} html.
 */
function indentAndWrapCode(code) {
  /**
   * Converts a plain text string to an HTML encoded string suitable for
   * inclusion in PCDATA or an HTML attribute value.
   * @param {string} s
   * @return {string} html
   */
  function escapeHtml(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/\042/g, '&quot;')
        .replace(/ /g, '\xA0');
  }

  /** Accumulates chunks of HTML. */
  var htmlOut = [];
  /**
   * Stack of number of spaces before open blocks in reverse order.
   * The current block, which has the highest indent level, is always in
   * position 0.
   */
  var indentStack = [0];

  /** Append chunks of html to htmlOut for a single line of code. */
  function processLine(line) {
    var len = line.length;
    var pos = 0;  // Length of the prefix of line processed so far.

    // Count the number of spaces at the beginning so we can construct nested
    // <blockquote> chunks around indentation changes.
    while (pos < len && line.charAt(pos) == ' ') { ++pos; }
    if (pos === len) {
      htmlOut.push('<br>');
      return;
    }
    if (pos !== indentStack[0]) {
      if (pos < indentStack[0]) {
        do {
          indentStack.shift();
          htmlOut.push('</div>');
        } while (pos < indentStack[0]);
      } else if (pos > indentStack[0]) {
        indentStack.unshift(pos);
        htmlOut.push('<div class="indentedblock">');
      }
    }

    // Walk over the code and introduce <wbr>s at commas and brackets
    htmlOut.push('<div class="line-of-code">');
    var strDelim = null;
    for (var i = pos; i < len; ++i) {
      var ch = line.charAt(i);
      switch (ch) {
      case '"': case "'":
        if (strDelim === null) {
          strDelim = ch;
        } else if (strDelim === ch) {
          strDelim = null;
        }
        break;
      case '\\':
        if (strDelim !== null) { ++i; }
        break;
      // Since we replace spaces with non-breaking spaces, explicitly insert
      // <WBR>s around puncutation to allow breaking outside literals.
      case ',': case '(': case ')': case '{': case '}': case '[': case ']':
        if (strDelim === null) {
          htmlOut.push(escapeHtml(line.substring(pos, i + 1)), '<wbr>');
          pos = i + 1;
        }
        break;
      }
    }
    htmlOut.push(escapeHtml(line.substring(pos)), '</div>');
  }

  var lines = code.split(/\r\n?|\n/g);
  for (var i = 0, n = lines.length; i < n; ++i) { processLine(lines[i]); }
  for (var i = indentStack.length; --i >= 1;) { htmlOut.push('</div>'); }
  return htmlOut.join('');
}
