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

function assertSelector(
    source, prefix, expected, opt_onUntranslatableSelector) {
  var tokens = lexCss(source);
  var sanitized = sanitizeCssSelectors(
    tokens,
    {
      containerClass: prefix,
      idSuffix: '-' + prefix,
      tagPolicy: function(el, args) { return { tagName: el }; }
    },
    opt_onUntranslatableSelector);
  assertArrayEquals(expected, sanitized);
}

function assertProperty(propName, source, expected) {
  var tokens = lexCss(source);
  sanitizeCssProperty(propName, tokens);
  assertEquals(expected, tokens.join(' '));
}

function assertSanitizedStylesheet(golden, input) {
  var stylesheet = sanitizeStylesheet(
      'http://example.com/baseurl', input,
      {
        containerClass: 'scopeClass',
        idSuffix: '-suffix',
        tagPolicy: function (elName, attrs) { return []; }
      });
  assertArrayEquals(input, golden, stylesheet);
}

jsunitRegister('testFontFamily',
               function testFontFamily() {
  var tokens = ['Arial', ' ', 'Black', ',', 'monospace', ',',
                'expression(', 'return', ' ', '"pwned"', ')', ',', 'Helvetica',
                ' ', '#88ff88'];
  sanitizeCssProperty('font', tokens);
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
  assertProperty('font', 'bold 2em monospace', 'bold 2em monospace');
  assertProperty('font', '20pt Calibri', '20pt "calibri"');
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
  sanitizeCssProperty('background', tokens, sanitizeUrl);
  assertEquals(
      'url(\"proxy?url=foo.png\") transparent / url(\"proxy?url=Bar.png\")',
      tokens.join(' '));

  // Without any URL sanitizer.
  tokens = ['bogus', ' ', '"foo.png"', ' ', 'transparent', ' ', '/',
            'url("Bar.png")'];
  sanitizeCssProperty('background', tokens);
  assertEquals('transparent /', tokens.join(' '));

  tokens = ['/', '*', '"/*zoicks*/"', ' ',
            '"\') url(\\22javascript:alert(1337)\")\"', '"</style>"'];
  sanitizeCssProperty('background', tokens, sanitizeUrl);
  assertEquals(
    '/ url(\"proxy?url=%2F%2azoicks%2a%2F\")'
    + ' url(\"proxy?url=%27%29%20url%28%22javascript%3Aalert%281337%29%22%29\")'
    + ' url(\"proxy?url=%3C%2Fstyle%3E\")',
    tokens.join(' '));

  // Don't require the URL sanitizer to protect string boundaries.
  tokens = ['/', '*', '"/*zoicks*/"', ' ',
            '"\') url(\\22javascript:alert(1337)\")\"', '"</style>"'];
  sanitizeCssProperty('background', tokens, function (s) { return s; });
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
      'background', tokens,
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
  sanitizeCssProperty('padding', tokens);
  // .0 -> 0.0 others unchanged.
  assertEquals('0 0.0 0. 0.0', tokens.join(' '));

  tokens = ['-0', ' ', '-.0', ' ', '-0.', ' ', '-0.0', ' '];
  sanitizeCssProperty('padding', tokens);
  // Negative numbers capped at 0.
  assertEquals('0 0 0 0', tokens.join(' '));

  tokens = ['+0', ' ', '+.0', ' ', '+0.', ' ', '+0.0'];
  sanitizeCssProperty('padding', tokens);
  // + sign dropped.
  assertEquals('0 0.0 0. 0.0', tokens.join(' '));

  tokens = ['7', ' ', '.5', ' ', '23.', ' ', '1.25'];
  sanitizeCssProperty('padding', tokens);
  // .5 -> 0.5 others unchanged.
  assertEquals('7 0.5 23. 1.25', tokens.join(' '));

  tokens = [' ', '-7', ' ', '-.5', ' ', '-23.', ' ', '-1.25'];
  sanitizeCssProperty('padding', tokens);
  // Negative numbers capped at 0.
  assertEquals('0 0 0 0', tokens.join(' '));

  // Negative margins are allowed.
  tokens = [' ', '-7', ' ', '-.5', ' ', '-23.', ' ', '-1.25'];
  sanitizeCssProperty('margin', tokens);
  assertEquals('-7 -0.5 -23. -1.25', tokens.join(' '));

  tokens = ['+7', ' ', '+.5', ' ', '+23.', ' ', '+1.25'];
  sanitizeCssProperty('padding', tokens);
  // + sign dropped.
  assertEquals('7 0.5 23. 1.25', tokens.join(' '));

  tokens = ['#123', ' ', '-', ' ', '5', ' ', '"5"'];
  sanitizeCssProperty('padding', tokens);
  assertEquals('5', tokens.join('5'));

  // Quantities are OK.
  tokens = ['7cm', ' ', '.5em', ' ', '23.mm', ' ', '1.25px'];
  sanitizeCssProperty('padding', tokens);
  assertEquals('7cm 0.5em 23.mm 1.25px', tokens.join(' '));

  tokens = ['+7cm', ' ', '+.5em', ' ', '+23.mm', ' ', '+1.25px'];
  sanitizeCssProperty('padding', tokens);
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
  sanitizeCssProperty('font-family', tokens);
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
    sanitizeCssProperty('color', tokens);
    assertEquals(colors[i],
                 colors[i].replace(/\s+/g, '').toLowerCase(),
                 tokens.join('').replace(/\s+/g, '').toLowerCase());
  }

  for (var i = 0; i < notcolors.length; ++i) {
    var tokens = lexCss(notcolors[i]);
    sanitizeCssProperty('color', tokens);
    assertTrue(notcolors[i], tokens.length == 0);
  }
  jsunit.pass();
});

jsunitRegister('testBorder',
               function testBorder() {
  var source = '1px solid rgb(0,0,0)';
  var expect = '1px solid rgb(0 , 0 , 0)';
  assertProperty('border', source, expect);
  jsunit.pass();
});

jsunitRegister('testColonsInSelectors',
               function testColonsInSelectors() {
  var source = 'input.cl\\:a\\3a ss[type = "text"], input#foo\\:bar';
  var tokens = lexCss(source);
  var sanitized = sanitizeCssSelectors(
    tokens,
    {
      containerClass: 'sfx',
      idSuffix: '-sfx',
      tagPolicy: function(el, args) { return { tagName: el }; }
    });
  assertArrayEquals(
    [['.sfx input.cl\\:a\\:ss[type="text"]',
      '.sfx input#foo\\:bar-sfx'], []],
    sanitized);
  jsunit.pass();
});

jsunitRegister('testCssSelectors',
               function testCssSelectors() {
  assertSelector("#foo:visited", "sfx", [[], [".sfx a#foo-sfx:visited"]]);
  assertSelector("#foo:link", "sfx", [[], [".sfx a#foo-sfx:link"]]);

  assertSelector("#foo:active", "sfx", [[".sfx #foo-sfx:active"], []]);
  assertSelector("#foo:after", "sfx", [[".sfx #foo-sfx:after"], []]);
  assertSelector("#foo:before", "sfx", [[".sfx #foo-sfx:before"], []]);
  assertSelector(
    "#foo:first-child", "sfx", [[".sfx #foo-sfx:first-child"], []]);
  assertSelector(
    "#foo:first-letter", "sfx", [[".sfx #foo-sfx:first-letter"], []]);
  assertSelector("#foo:focus", "sfx", [[".sfx #foo-sfx:focus"], []]);
  assertSelector("#foo:hover", "sfx", [[".sfx #foo-sfx:hover"], []]);
  assertSelector("#foo:bogus", "sfx", [[], []]);
  jsunit.pass();
});

jsunitRegister('testAttrSelectors',
               function testAttrSelectors() {
  assertSelector(
    "div[class*='substr']", "sfx", [[".sfx div[class*=\"substr\"]"], []]);
  assertSelector(
    "div[class|='substr' i]", "sfx", [[".sfx div[class|=\"substr\" i]"], []]);
  assertSelector(
    "p[title |= \"sub\"]", "sfx", [[".sfx p[title|=\"sub\"]"], []]);
  assertSelector(
    "p[id ~= \"\\\"\"]", "sfx", [[".sfx p[id~=\"\\22 -sfx\"]"], []]);
  // ids allowed on any element.  unquoted values are quoted.
  assertSelector("*[id ~= foo]", "sfx", [[".sfx *[id~=\"foo-sfx\"]"], []]);
  // id existence check allowed
  assertSelector("*[id]", "sfx", [[".sfx *[id]"], []]);
  assertSelector(
    "input[type=text]", "sfx", [[".sfx input[type=\"text\"]"], []]);
  // Can't deal with case insensitive matches of case sensitive suffix.
  assertSelector("p[id ~= \"\\\"\" i]", "sfx", [[], []]);
  // Drop empty values for suffix and prefix operators instead of turning a
  // predicate that always fails into one that can succeed.
  assertSelector("p[id^='']", "sfx", [[], []]);
  // URIs require rewriting, so it isn't meaningful to match against URIs.
  // Also, it leaks the base URL.
  // Maybe store the original of URI attrs in a custom attr.
  assertSelector("a[href*='?pwd=hello-kitty']", "sfx", [[], []]);
  assertSelector("a[href]", "sfx", [[".sfx a[href]"], []]);
  assertSelector("A[href]", "sfx", [[".sfx a[href]"], []]);
  assertSelector("A[HREF]", "sfx", [[".sfx a[href]"], []]);
  jsunit.pass();
});

jsunitRegister('testMixedSubselectorsOrderIndependent',
               function testMixedSubselectorsOrderIndependent() {
  assertSelector(
    "div[title=foo].c1#id.c2", "zzz",
    [[".zzz div.c1#id-zzz.c2[title=\"foo\"]"], []]);
  jsunit.pass();
});

jsunitRegister('testGradients',
               function testGradients() {
  var source = 'linear-gradient(to bottom right, red, rgb(255,0,0))';
  var expect = 'linear-gradient(to bottom right , red , rgb(255 , 0 , 0))';
  assertProperty('background-image', source, expect);
  jsunit.pass();
});

jsunitRegister('testUntranslatableSelectorHandler',
               function testUntranslatableSelectorHandler() {
  // No handler
  assertSelector(
      "p.bar, b, bad[], p.foo", "x",
      [[".x p.bar", ".x b", ".x p.foo"], []]);
  // Explicitly permissive
  assertSelector(
      "p.bar, b, bad[], p.foo", "x",
      [[".x p.bar", ".x b", ".x p.foo"], []],
      function () { return true; });
  // Fail early.
  assertSelector(
      "p.bar, b, bad[], p.foo", "x",
      null,
      function () { return false; });
  // Don't fail early on nothing
  assertSelector(
      "p,,p", "x",
      [[".x p", ".x p"], []],
      function () { return false; });
  // Test the callback value.
  var source =
    "p, b.bad__, p, div > *[bogus], p[title=@], div + p[id=bad bad],"
    + " a[href~='untranslatable'], a:visited, p, b#bad__, b#__bad";
  var expected = [[".x p", ".x p", ".x p"], [".x a:visited"]];
  var log = [];
  assertSelector(
      source, "x", expected,
      function (toks) { log.push(toks); return 1; });
  assertArrayEquals(
      "Untranslatable Handler Log",
      [
        ["b", ".", "bad__"],
        ["div", ">", "*", "[", "bogus", "]"],
        ["p", "[", "title", "=", "@", "]"],
        ["div", "+", "p", "[", "id", "=", "bad", "bad", "]"],
        ["a", "[", "href", "~=", "\"untranslatable\"", "]"],
        ["b", "#bad__"],
        ["b", "#__bad"]
      ],
      log);
  jsunit.pass();
});

jsunitRegister('testImportant', function testImportant() {
  assertSanitizedStylesheet(
      ''
      + '.scopeClass p{color:red !important;}'
      + '.scopeClass q{color:green !important;}'
      + '.scopeClass #id-suffix{color:blue;}',
      ''
      + 'p { color: red !important }\n'
      + 'q { color: green ! Important }\n'
      + '#id { color: blue }');

  jsunit.pass();
});

jsunitRegister('testIssue1804', function testIssue1804() {
  assertSelector("a,", "sfx", [[".sfx a"], []]);
  assertSelector(",a", "sfx", [[".sfx a"], []]);
  assertSelector(",",  "sfx", [[], []]);
  assertSelector("a[", "sfx", [[], []]);
  jsunit.pass();
});

jsunitRegister('testMediaQueries', function testMediaQueries() {
  var passing = [
    '',
    ' ',
    'all',
    ' all',
    'all ',
    ' all ',
    'not all',
    'all and(color)',
    'screen and (max-width: 6in) and (color), (color)',
    '(min-orientation:portrait)',
    '(min-width: 700px)',
    '(min-width: 700px) and (orientation: landscape)',
    'tv and (min-width: 700px) and (orientation: landscape)',
    '(min-width: 700px), handheld and (orientation: landscape)',
    'not all and (monochrome)',
    'not screen and (color), print and (color)',
    'only screen and (color)',
    'all and (color)',
    'all and (min-color: 4)',
    'all and ( min-color : 4 )',
    'all and (color-index)',
    'all and (min-color-index: 256)',
    'screen and (min-aspect-ratio: 1/1)',
    'screen and (device-aspect-ratio: 16/9), screen and (device-aspect-ratio: 16/10)',
    'screen and (max-device-width: 799px)',
    'handheld and (grid) and (max-width: 15em)',
    'all and (monochrome)',
    'all and (min-monochrome: 8)',
    'all and (orientation: portrait)',
    'print and (min-resolution: 300dpi)',
    'screen and (min-resolution: 2dppx)',
    'tv and (scan: progressive)',
    'handheld and (min-width: 20em), screen and (min-width: 20em)',
    'print and (min-width: 8.5in)',
    'screen and (min-width: 500px) and (max-width: 800px)',
    '(min-resolution: 192dpi)',
    'print and (min-width: 25cm)',
    'screen and (min-width: 400px) and (max-width: 700px)',
    'handheld and (min-width: 20em), \n  screen and (min-width: 20em)',
    'screen and (device-width: 800px)',
    'screen and (device-height: 600px)',
    'all and (orientation:landscape)',
    'screen and (device-aspect-ratio: 16/9)',
    'screen and (device-aspect-ratio: 2560 / 1440)',
    '(min-color: 1)',
    '(min-color:2)',
    'all and (color-index)',
    'all and (min-color-index: 1)',
    'all and (min-color-index: 256)',
    '(monochrome)',
    '(min-monochrome: 1)',
    'print and (monochrome)',
    'print and (min-resolution: 118dpcm)',
    'handheld and (grid) and (max-device-height: 7em)',
    '(script) and (pointer: fine)',
    'only screen and (pointer) and (hover:1)',
    'screen and (luminosity:dim)',
    '(MONOCHROME)',
    '(Color:1)',
    'Screen AND (Min-device-Width: 600PX)'
  ];
  var failing = [
    'not all',
    'only and or',
    '(bogus)',
    '(screen)',
    'min-width:800px',
    'screen and ((color:1))',
    'only (color)',
    'only (color:1)',
    'screen, tv, ',
    '(min-device-width:6in',
    '(min-device-width:',
    '(min-device-width'
  ];
  function normalizeQuery(css) {
    if (/\S/.test(css) && !/^\s*\w/.test(css)) { css = 'not all , ' + css; }
    css = css.replace(/\s+/g, ' ');
    return css.replace(/(^|[^\w\-]) /g, '$1') // Strip spaces after punctuation.
              .replace(/ (?=$|[^\w\-])/g, '') // Strip spaces before punctuation.
              .toLowerCase();
  }
  for (var i = 0, n = passing.length; i < n; ++i) {
    var css = passing[i];
    var tokens = lexCss(css);
    var sanitized = sanitizeMediaQuery(tokens);
    assertEquals(css, normalizeQuery(css), normalizeQuery(sanitized)); 
  }
  for (var i = 0, n = failing.length; i < n; ++i) {
    var css = failing[i];
    var tokens = lexCss(css);
    var sanitized = sanitizeMediaQuery(tokens);
    assertEquals(css, 'not all', normalizeQuery(sanitized)); 
  }
  jsunit.pass();
});

jsunitRegister('testKeyframes', function testKeyframes() {
  // Mixture of example 1 and example 2 from
  // http://dev.w3.org/csswg/css-animations/
  var input = [
    'div {',
    '  animation-name: diagonal-Slide;',
    '  animation-duration: 5s;',
    '  animation-iteration-count: 10;',
    '}',
    '',
    '@keyframes diagonal-Slide {',
    '',
    '  from {',
    '    left: 0;',
    '    top: 0;',
    '  }',
    '',
    '  50% {',
    '    left: 55px;',
    '  }',
    '',
    '  51%, 52%,53% {',
    '    left: 56px;',
    '  }',
    '',
    '  to {',
    '    left: 100px;',
    '    top: 100px;',
    '  }',
    '}',
    ''].join('\n');

  assertSanitizedStylesheet(
      ''
      + '.scopeClass div{'
      + 'animation-name:diagonal-Slide-suffix;'
      + 'animation-duration:5s;'
      + 'animation-iteration-count:10;'
      + '}'
      + '@keyframes diagonal-Slide-suffix{'
      + 'from{left:0;top:0;}'
      + '50%{left:55px;}'
      + '51%,52%,53%{left:56px;}'
      + 'to{left:100px;top:100px;}'
      + '}',
      input);

  assertSanitizedStylesheet(
      '@-webkit-keyframes xyzzy-suffix{from{left:0;}}',
      '@-webkit-keyframes xyzzy { from { left: 0 } }');

  // "Rules" in @keyframes must match from/to/<percentage>
  assertSanitizedStylesheet(
      '@keyframes foo-suffix{}',
      ''
      + '@keyframes foo {'
      + '  whence { left: 0; top: 100px }'
      + '  .foo { left: 100px; top: 0 }'
      + '  a[href] { right: 50px }'
      + '}');

  // Drop @keyframes with bad IDs.
  assertSanitizedStylesheet(
      ''
      + '.scopeClass b{color:blue;}'
      + '.scopeClass p{color:pink;}',

      ''
      + '@keyframes foo__  { from { left: 0 } }'
      + '@keyframes foo_ _ { from { left: 0 } }'
      + 'b { color: blue }'
      + '@keyframes "foo"  { from { left: 0 } }'
      + '@keyframes        { from { left: 0 } }'
      + 'p { color: pink }');

  jsunit.pass();
});

jsunitRegister('testTransitions', function testTransitions() {
  assertSanitizedStylesheet(
      ['.scopeClass li{',
         'transition:background-color linear 1s;',
         'background:blue;',
       '}',
       '.scopeClass li:hover{',
         'background-color:green;',
         'transition-duration:2s;',
       '}',
       '.scopeClass div{',
         'transition-property:opacity , , left;',  // bogus elided
         'transition-duration:2s , 4s;',
         'transition-timing-function:',
           'ease , step-start , cubic-bezier(0.1 , 0.7 , 1.0 , 0.1);',
       '}'
      ].join(''),
      ['li {',
       '  transition: background-color linear 1s;',
       '  background: blue;',
       '}',
       'li:hover {',
       '  background-color: green;',
       '  transition-duration: 2s;',
       '}',
       'div {',
       '  transition-property: opacity, bogus, left;',
       '  transition-duration: 2s, 4s;',
       '  transition-timing-function:',
       '    ease, step-start, cubic-bezier(0.1, 0.7, 1.0, 0.1)',
       '}'
      ].join('\n'));
  jsunit.pass();
});

jsunitRegister('testTransform', function testTransform() {
  assertSanitizedStylesheet(
      ['.scopeClass p{',
         'transform:translate(1%);',
         'transform:translatex(2%) translatey(3%);',
       '}',
      ].join(''),
      ['p {',
       '  transform: translate(1%);',
       '  transform: translatex(2%) translateY(3%);',
       '}',
      ].join('\n'));
  jsunit.pass();
});
