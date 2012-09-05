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

/** @fileoverview testcases for sanitizecss.js */

jsunitRegister('testFontFamily',
               function testFont() {
  var tokens = ['Arial', ' ', 'Black', ',', 'monospace', ',',
                'expression(', 'return', ' ', '"pwned"', ')', ',', 'Helvetica',
                ' ', '#88ff88'];
  sanitizeCssProperty('font', cssSchema['font'], tokens);
  // Arial Black is quoted to prevent conflation with an unknown keyword.
  // Black is a valid keyword, but for color, not font.
  // monospace is a valid keyword for font so is not quoted.
  // expression(...) is not allowed for font so is rejected wholesale -- the
  // internal string "pwned" is not passed through.
  // "Helvetica" is a single keyword that is quoted.
  // The comma is allowed punctuation in font.
  // Hash values are not allowed so are dropped.
  assertEquals('"arial black" , monospace , , "helvetica"', tokens.join(' '));
  jsunit.pass();
});

jsunitRegister('testFont',
               function testFont() {
  var tokens = ['bold', ' ', '2em', ' ', 'monospace'];
  sanitizeCssProperty('font', cssSchema['font'], tokens);
  assertEquals('bold 2em monospace', tokens.join(' '));
  jsunit.pass();
});

jsunitRegister('testBackground',
               function testBackground() {
  function sanitizeUrl(url) {
    return 'proxy?url=' + encodeURIComponent(url);
  }

  var tokens;
  tokens = ['bogus', ' ', '"foo.png"', ' ', 'transparent', ' ', '/',
            'URL("Bar.png")'];
  sanitizeCssProperty('background', cssSchema['background'], tokens, sanitizeUrl);
  assertEquals(
      'url(\"proxy?url=foo.png\") transparent / url(\"proxy?url=Bar.png\")',
      tokens.join(' '));

  // Without any URL sanitizer.
  tokens = ['bogus', ' ', '"foo.png"', ' ', 'transparent', ' ', '/',
            'url("Bar.png")'];
  sanitizeCssProperty('background', cssSchema['background'], tokens);
  assertEquals('transparent /', tokens.join(' '));

  tokens = ['/', '*', '"/*zoicks*/"', ' ',
            '"\') url(\\22javascript:alert(1337)\")\"', '"</style>"'];
  sanitizeCssProperty('background', cssSchema['background'], tokens, sanitizeUrl);
  assertEquals(
    '/ url(\"proxy?url=%2F%2azoicks%2a%2F\")'
    + ' url(\"proxy?url=%27%29%20url%28%22javascript%3Aalert%281337%29%22%29\")'
    + ' url(\"proxy?url=%3C%2Fstyle%3E\")',
    tokens.join(' '));

  // Don't require the URL sanitizer to protect string boundaries.
  tokens = ['/', '*', '"/*zoicks*/"', ' ',
            '"\') url(\\22javascript:alert(1337)\")\"', '"</style>"'];
  sanitizeCssProperty(
      'background', cssSchema['background'], tokens, function (s) { return s; });
  assertEquals(
    '/ url(\"/%2azoicks%2a/\")'
    // javascript: does not appear after url(" and embedded quotes and parens
    // are escaped.
    + ' url(\"%27%29 url%28%22javascript:alert%281337%29%22%29\")'
    // </style not a substring.
    + ' url(\"%3c/style%3e\")',
    tokens.join(' '));

  // The URL sanitizer should be able to block some URLs and allow others.
  tokens = ['url("")', ' ', '"#"', ' ', 'url("#")', ' ',
            'url("javascript:evil()")'];
  sanitizeCssProperty(
      'background', cssSchema['background'], tokens,
      function (s) {
        // Reject anything with a colon entirely, and strip fragments.
        s = s.replace(/#[\s\S]*$/, '');
        return /:/.test(s) ? null : s;
      });
  assertEquals(
      'url("") url("") url("") url("about:blank")',
      tokens.join(' '));

  jsunit.pass();
});

jsunitRegister('testQuantities',
               function testQuantities() {
  var tokens;

  tokens = ['0', ' ', '.0', ' ', '0.', ' ', '0.0', ' '];
  sanitizeCssProperty('padding', cssSchema['padding'], tokens);
  // .0 -> 0.0 others unchanged.
  assertEquals('0 0.0 0. 0.0', tokens.join(' '));

  tokens = ['-0', ' ', '-.0', ' ', '-0.', ' ', '-0.0', ' '];
  sanitizeCssProperty('padding', cssSchema['padding'], tokens);
  // Negative numbers capped at 0.
  assertEquals('0 0 0 0', tokens.join(' '));

  tokens = ['+0', ' ', '+.0', ' ', '+0.', ' ', '+0.0'];
  sanitizeCssProperty('padding', cssSchema['padding'], tokens);
  // + sign dropped.
  assertEquals('0 0.0 0. 0.0', tokens.join(' '));

  tokens = ['7', ' ', '.5', ' ', '23.', ' ', '1.25'];
  sanitizeCssProperty('padding', cssSchema['padding'], tokens);
  // .5 -> 0.5 others unchanged.
  assertEquals('7 0.5 23. 1.25', tokens.join(' '));

  tokens = [' ', '-7', ' ', '-.5', ' ', '-23.', ' ', '-1.25'];
  sanitizeCssProperty('padding', cssSchema['padding'], tokens);
  // Negative numbers capped at 0.
  assertEquals('0 0 0 0', tokens.join(' '));

  // Negative margins are allowed.
  tokens = [' ', '-7', ' ', '-.5', ' ', '-23.', ' ', '-1.25'];
  sanitizeCssProperty('margin', cssSchema['margin'], tokens);
  assertEquals('-7 -0.5 -23. -1.25', tokens.join(' '));

  tokens = ['+7', ' ', '+.5', ' ', '+23.', ' ', '+1.25'];
  sanitizeCssProperty('padding', cssSchema['padding'], tokens);
  // + sign dropped.
  assertEquals('7 0.5 23. 1.25', tokens.join(' '));

  tokens = ['#123', ' ', '-', ' ', '5', ' ', '"5"'];
  sanitizeCssProperty('padding', cssSchema['padding'], tokens);
  assertEquals('5', tokens.join('5'));

  // Quantities are OK.
  tokens = ['7cm', ' ', '.5em', ' ', '23.mm', ' ', '1.25px'];
  sanitizeCssProperty('padding', cssSchema['padding'], tokens);
  assertEquals('7cm 0.5em 23.mm 1.25px', tokens.join(' '));

  tokens = ['+7cm', ' ', '+.5em', ' ', '+23.mm', ' ', '+1.25px'];
  sanitizeCssProperty('padding', cssSchema['padding'], tokens);
  assertEquals('7cm 0.5em 23.mm 1.25px', tokens.join(' '));

  // Font-family does not allow quantities at all.
  tokens = ['7', ' ', '.5', ' ', '23.', ' ', '1.25',
            '-7', ' ', '-.5', ' ', '-23.', ' ', '-1.25',
            '+7', ' ', '+.5', ' ', '+23.', ' ', '+1.25',
            '7cm', ' ', '.5em', ' ', '23.mm', ' ', '1.25px',
            '-7cm', ' ', '-.5em', ' ', '-23.mm', ' ', '-1.25px',
            '+7cm', ' ', '+.5em', ' ', '+23.mm', ' ', '+1.25px',
            '0', ' ', '.0', ' ', '-0', '+0', '0.0',
            '/'];
  sanitizeCssProperty('font-family', cssSchema['font-family'], tokens);
  assertEquals('', tokens.join(' '));

  jsunit.pass();
});

jsunitRegister('testColor',
               function testColor() {
  var colors = [
      'red',
      'Red', 
      'RED', 
      'Gray',
      // TODO: enable
      //'grey',
      //'chartreuse',
      '#abc',
      '#123',
      '#000',
      '#ABC123',
      'rgb(0, 0, 100%)',
      'rgb( 127, 64 , 255 )',
      'rgba(128,255,128,50%)'];
  var notcolors = [
      // Finding words that are not X11 colors is harder than you think.
      'killitwithfire',
      'invisible',
      'expression(red=blue)',
      '#aa-1bb',
      '#expression',
      '#doevil'
  ];

  for (var i = 0; i < colors.length; ++i) {
    var tokens = lexCss(colors[i]);
    sanitizeCssProperty('color', cssSchema['color'], tokens);
    assertEquals(colors[i],
                 colors[i].replace(/\s+/g, '').toLowerCase(),
                 tokens.join('').replace(/\s+/g, '').toLowerCase());
  }

  for (var i = 0; i < notcolors.length; ++i) {
    var tokens = lexCss(notcolors[i]);
    sanitizeCssProperty('color', cssSchema['color'], tokens);
    assertTrue(notcolors[i], tokens.length == 0);
  }
  jsunit.pass();
});

jsunitRegister('testBorder',
               function testBorder() {
  var source = '1px solid rgb(0,0,0)';
  var expect = '1px:solid:rgb( 0 , 0 , 0 )';
  var tokens = lexCss(source);
  sanitizeCssProperty('border', cssSchema['border'], tokens);
  assertEquals(expect, tokens.join(':'));
  jsunit.pass();
});
