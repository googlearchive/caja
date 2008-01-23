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
 * @fileoverview a simple html sanitizer
 *
 * @author msamuel@google.com
 */


var sanitizeHtml;

(function () {
/** defines which tags are allowed. */
var HTML_WHITELIST_ = {
    'p': {},
    'b': {},
    'i': {},
    'u': {},
    'br': {},
    'blockquote': {},
    'address': {},
    'ul': {},
    'ol': {},
    'li': {},
    'sub': {},
    'sup': {}
};
var HTML_TOKENS_ = [
    /^<!-.*?-->/,
    /^<!(?:"[\s\S]*?"|'[\s\S]*?'|[^>])*>/,
    /^<(script|style|xmp|plaintext|title|textarea)[\s\S]*?<\/\1[^>]*>/i,
    /^<\/?\w(?:"[\s\S]*?"|'[\s\S]*?'|[^>])*>/,
    /^[^<]+/,
    /^</
    ];
var ATTRIB_PATTERN_ = 
    /^\s+(\w+)(?:\s*=\s*([^\"\'<>\s]+|\"[^\"<>]*\"|\'[^\'<>]*\'))?/;
/**
 * given html, returns "safe" html.
 *
 * @param {string} html
 * @return {string} html.
 */
function sanitizeHtml(html) {
  var out = [];
  while (html) {
    var tok = null;
    for (var i = 0; i < HTML_TOKENS_.length; ++i) {
      var m = html.match(HTML_TOKENS_[i]);
      if (m) {
        tok = (typeof m) == 'string' ? m : m[0];
        html = html.substring(tok.length);
        break;
      }
    }
    if (tok.charAt(0) === '<') {
      if (tok == '<') {
        out.push('&lt;');
      } else {
        var m = tok.match(/<(\/?)(\w+)/);
        if (m) {
          var closeTag = !!m[1];
          var tagName = m[2].toLowerCase();
          if (tagName in HTML_WHITELIST_) {
            var attribWhitelist = HTML_WHITELIST_[tagName];
            if (closeTag) {
              out.push('</', tagName, '>');
            } else {
              out.push('<', tagName);
              var attribs = tok.substring(tagName.length + 1, tok.length - 1);
              for (var m = null;
                   attribs && (m = attribs.match(ATTRIB_PATTERN_)) != null;
                   attribs = attribs.substring(m[0].length)) {
                var name = m[1];
                var value = m[2];
                if (value == null) {
                  value = name;
                }
                var uqValue = value;
                if (!value || '\"\''.indexOf(value.charAt(0)) < 0) {
                  value = '"' + value + '"';
                } else {
                  uqValue = value.substring(1, value.length - 1);
                }
                if (name in attribWhitelist
                    && attribWhitelist[name].test(uqValue)) {
                  out.push(' ', name, '=', value);
                }
              }
              out.push('>');
            }
          }
        }
      }
    } else {
      out.push(tok);
    }
  }
  return out.join('');
}

this.sanitizeHtml = sanitizeHtml;
})(); 
