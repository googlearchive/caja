// Copyright (C) 2010 Google Inc.
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
 * Utilities for dealing with CSS source code.
 *
 * @author mikesamuel@gmail.com
 * @provides cssparser
 */

var cssparser = (function ()
{
  var ucaseLetter = /[A-Z]/g;
  function lcaseOne(ch) { return String.fromCharCode(ch.charCodeAt(0) | 32); };
  var LCASE = ('i' === 'I'.toLowerCase())
      ? function (s) { return s.toLowerCase(); }
      // Rhino's toLowerCase is broken.
      : function (s) { return s.replace(ucaseLetter, lcaseOne); };

  // CSS Lexical Grammar rules.
  // CSS lexical grammar from http://www.w3.org/TR/CSS21/grammar.html
  // The comments below are mostly copied verbatim from the grammar.

  // "@import"               {return IMPORT_SYM;}
  // "@page"                 {return PAGE_SYM;}
  // "@media"                {return MEDIA_SYM;}
  // "@charset"              {return CHARSET_SYM;}
  var KEYWORD = '(?:\\@(?:import|page|media|charset))';

  // nl                      \n|\r\n|\r|\f ; a newline
  var NEWLINE = '\\n|\\r\\n|\\r|\\f';

  // h                       [0-9a-f]      ; a hexadecimal digit
  var HEX = '[0-9a-f]';

  // nonascii                [\200-\377]
  var NON_ASCII = '[^\\0-\\177]';

  // unicode                 \\{h}{1,6}(\r\n|[ \t\r\n\f])?
  var UNICODE = '(?:(?:\\\\' + HEX + '{1,6})(?:\\r\\n|[ \t\\r\\n\\f])?)';

  // escape                  {unicode}|\\[^\r\n\f0-9a-f]
  var ESCAPE = '(?:' + UNICODE + '|\\\\[^\\r\\n\\f0-9a-f])';

  // nmstart                 [_a-z]|{nonascii}|{escape}
  var NMSTART = '(?:[_a-z]|' + NON_ASCII + '|' + ESCAPE + ')';

  // nmchar                  [_a-z0-9-]|{nonascii}|{escape}
  var NMCHAR = '(?:[_a-z0-9-]|' + NON_ASCII + '|' + ESCAPE + ')';

  // ident                   -?{nmstart}{nmchar}*
  var IDENT = '-?' + NMSTART + NMCHAR + '*';

  // name                    {nmchar}+
  var NAME = NMCHAR + '+';

  // hash
  var HASH = '#' + NAME;

  // string1                 \"([^\n\r\f\\"]|\\{nl}|{escape})*\"  ; "string"
  var STRING1 = '"(?:[^\\"\\\\]|\\\\[^])*"';

  // string2                 \'([^\n\r\f\\']|\\{nl}|{escape})*\'  ; 'string'
  var STRING2 = "'(?:[^\\'\\\\]|\\\\[^])*'";

  // string                  {string1}|{string2}
  var STRING = '(?:' + STRING1 + '|' + STRING2 + ')';

  // num                     [0-9]+|[0-9]*"."[0-9]+
  var NUM = '(?:[0-9]*\\.[0-9]+|[0-9]+)';

  // s                       [ \t\r\n\f]
  var SPACE = '[ \\t\\r\\n\\f]';

  // w                       {s}*
  var WHITESPACE = SPACE + '*';

  // url special chars
  var URL_SPECIAL_CHARS = '[!#$%&*-~]';

  // url chars               ({url_special_chars}|{nonascii}|{escape})*
  var URL_CHARS
      = '(?:' + URL_SPECIAL_CHARS + '|' + NON_ASCII + '|' + ESCAPE + ')*';

  // url
  var URL = (
      'url\\(' + WHITESPACE + '(?:' + STRING + '|' + URL_CHARS + ')'
      + WHITESPACE + '\\)');

  // comments
  // see http://www.w3.org/TR/CSS21/grammar.html
  var COMMENT = '/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/';

  // {E}{M}             {return EMS;}
  // {E}{X}             {return EXS;}
  // {P}{X}             {return LENGTH;}
  // {C}{M}             {return LENGTH;}
  // {M}{M}             {return LENGTH;}
  // {I}{N}             {return LENGTH;}
  // {P}{T}             {return LENGTH;}
  // {P}{C}             {return LENGTH;}
  // {D}{E}{G}          {return ANGLE;}
  // {R}{A}{D}          {return ANGLE;}
  // {G}{R}{A}{D}       {return ANGLE;}
  // {M}{S}             {return TIME;}
  // {S}                {return TIME;}
  // {H}{Z}             {return FREQ;}
  // {K}{H}{Z}          {return FREQ;}
  // %                  {return PERCENTAGE;}
  var UNIT = '(?:em|ex|px|cm|mm|in|pt|pc|deg|rad|grad|ms|s|hz|khz|%)';

  // {num}{UNIT|IDENT}                   {return NUMBER;}
  var QUANTITY = NUM + '(?:' + WHITESPACE + UNIT + '|' + IDENT + ')?';

  // "<!--"                  {return CDO;}
  // "-->"                   {return CDC;}
  // "~="                    {return INCLUDES;}
  // "|="                    {return DASHMATCH;}
  // {w}"{"                  {return LBRACE;}
  // {w}"+"                  {return PLUS;}
  // {w}">"                  {return GREATER;}
  // {w}","                  {return COMMA;}
  var PUNC =  '<!--|-->|~=|[|=\\{\\+>,:;()]';

  var PROP_DECLS_TOKENS = new RegExp(
      '(?:'
      + [STRING, COMMENT, QUANTITY, URL, NAME, HASH, IDENT, SPACE + '+', PUNC]
          .join('|')
      + ')',
      'gi');

  var IDENT_RE = new RegExp('^(?:' + IDENT + ')$', 'i');
  var URL_RE = new RegExp('^(?:' + URL + ')$', 'i');
  var NON_HEX_ESC_RE = /\\(?:\r\n?|[^0-9A-Fa-f\r]|$)/g;
  var SPACE_RE = new RegExp(SPACE + '+', 'g');
  var BS = /\\/g;
  var DQ = /"/g;

  /** A replacer that deals with non hex backslashes. */
  function normEscs(x) {
    var out = '';
    // x could be '\\' in which case we return '' or it could be '\\\r\n' in
    // which case we escape both.
    // In the normal case where the length is 2 we end up turning any special
    // characters like \\, \", and \' into CSS escape sequences.
    for (var i = 1, n = x.length; i < n; ++i) {
      out += '\\' + x.charCodeAt(i).toString(16) + ' ';
    }
    return out;
  }

  function toCssStr(s) {
    return '"' + (s.replace(BS, '\\5c ').replace(DQ, '\\22 ')) + '"';
  }

  /**
   * Parser for CSS declaration groups that extracts property name, value
   * pairs.
   *
   * <p>
   * This method does not validate the CSS property value.  To do that, match
   * {@link css.properties} against the raw value in the handler.
   *
   * @param {string} cssText of CSS property declarations like
   *     {@code color:red}.
   * @param {function (string, Array.<string>) : void} handler
   *     receives each CSS property name and the tokenized value
   *     minus spaces and comments.
   */
  function parse(cssText, handler) {
    var toks = ('' + cssText).match(PROP_DECLS_TOKENS);
    if (!toks) { return; }
    var propName = null;
    var buf = [];
    var k = 0;
    for (var i = 0, n = toks.length; i < n; ++i) {
      var tok = toks[i];
      switch (tok.charCodeAt(0)) {
        // Skip spaces.  We can do this in properties even if they are
        // significant in rules.
        case 0x9: case 0xa: case 0xc: case 0xd: case 0x20: continue;
        case 0x27:  // Convert to double quoted string.
          tok = '"' + tok.substring(1, tok.length - 1).replace(DQ, '\\22 ')
              + '"';
          // $FALL-THROUGH$
        case 0x22: tok = tok.replace(NON_HEX_ESC_RE, normEscs); break;
        case 0x2f:  // slashes may start comments
          if ('*' === tok.charAt(1)) { continue; }
          break;
        // dot or digit
        case 0x2e:
        case 0x30: case 0x31: case 0x32: case 0x33: case 0x34:
        case 0x35: case 0x36: case 0x37: case 0x38: case 0x39:
          // 0.5 em  =>  0.5em
          tok = tok.replace(SPACE_RE, '');
          break;
        case 0x3a:  // colons separate property names from values
          // Remember the property name.
          if (k === 1 && IDENT_RE.test(buf[0])) {
            propName = LCASE(buf[0]);
          } else {
            propName = null;
          }
          k = buf.length = 0;
          continue;
        case 0x3b:  // semicolons separate name/value pairs
          if (propName) {
            if (buf.length) { handler(propName, buf.slice(0)); }
            propName = null;
          }
          k = buf.length = 0;
          continue;
        case 0x55: case 0x75:  // letter u
          var url = toUrl(tok);
          if (url !== null) { tok = 'url(' + toCssStr(url) + ')'; }
          break;
      }
      buf[k++] = tok;
    }
    if (propName && buf.length) { handler(propName, buf.slice(0)); }
  }

  var unicodeEscape
      = /\\(?:([0-9a-fA-F]{1,6})(?:\r\n?|[ \t\f\n])?|[^\r\n\f0-9a-f])/g;
  function decodeOne(_, hex) {
    return hex ? String.fromCharCode(parseInt(hex, 16)) : _.charAt(1);
  }
  /**
   * Given a css token, returns the URL contained therein or null.
   * @param {string} cssToken
   * @return {string|null}
   */
  function toUrl(cssToken) {
    if (!URL_RE.test(cssToken)) { return null; }
    cssToken = cssToken.replace(/^url[\s\(]+|[\s\)]+$/gi, '');
    switch (cssToken.charAt(0)) {
      case '"': case '\'':
        cssToken = cssToken.substring(1, cssToken.length - 1);
        break;
    }
    return cssToken.replace(unicodeEscape, decodeOne);
  }

  return {
    'parse': parse,
    'toUrl': toUrl,
    'toCssStr': toCssStr
  };
})();
