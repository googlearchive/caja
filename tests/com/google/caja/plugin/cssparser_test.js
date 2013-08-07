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

/** @fileoverview testcases for cssparser.js */

function assertParsedCssStylesheet(golden, cssText) {
  assertParsedCss(golden, cssText, parseCssStylesheet);
}

function assertParsedCssDecls(golden, cssText) {
  assertParsedCss(golden, cssText, parseCssDeclarations);
}

function assertParsedCss(golden, cssText, parseCss) {
  var logArr = [];
  function log(ev, args) {
    logArr.push(ev, Array.prototype.slice.call(args));
  }

  var handler = {
    startStylesheet: function () {
      log("startStylesheet", arguments);
    },
    endStylesheet: function () {
      log("endStylesheet", arguments);
    },
    startAtrule: function (atIdent, headerArray) {
      log("startAtrule", arguments);
    },
    endAtrule: function () {
      log("endAtrule", arguments);
    },
    startBlock: function () {
      log("startBlock", arguments);
    },
    endBlock: function () {
      log("endBlock", arguments);
    },
    startRuleset: function (selectorArray) {
      log("startRuleset", arguments);
    },
    endRuleset: function () {
      log("endRuleset", arguments);
    },
    declaration: function (property, valueArray) {
      log("declaration", arguments);
    }
  };
  parseCss(cssText, handler);
  var goldenArr = [];
  for (var i = 0, n = golden.length; i < n; ++i) {
    var e = golden[i];
    goldenArr.push(e);
    if (i+1 < n && typeof golden[i+1] !== 'string') {
      goldenArr.push(golden[++i]);
    } else {
      goldenArr.push([]);
    }
  }

  assertArrayEquals(goldenArr, logArr);
}

jsunitRegister("testCssParser",
               function testCssParser() {
  var cssText = [
    '@import "foo.css";',
    '@media print { th { font-weight: bolder } }',
    'p.clazz q, s { color: blue; }',
    'DIV { z-index: -1; }',
    'BODY { background: url(bg.png); font-family: Arial }',
    '* html a[href~="^https?://"]:after { content: "[ext]" }'
  ].join("\n");
  assertParsedCssStylesheet(
    [
      'startStylesheet',
      'startAtrule', ['@import',['"foo.css"']],
      'endAtrule',
      'startAtrule', ['@media',['print']],
      'startBlock',
      'startRuleset', [['th']],
      'declaration', ['font-weight',['bolder']],
      'endRuleset',
      'endBlock',
      'endAtrule',
      'startRuleset', [['p','.','clazz',' ','q',',',' ','s']],
      'declaration', ['color',['blue']],
      'endRuleset',
      'startRuleset', [['DIV']],
      'declaration', ['z-index',['-1']],
      'endRuleset',
      'startRuleset', [['BODY']],
      'declaration', ['background',['url("bg.png")']],
      'declaration', ['font-family',['Arial']],  // TODO: quote arial
      'endRuleset',
      'startRuleset', [['*',' ','html',' ','a','[','href','~=','"^https?://"',']',':','after']],
      'declaration', ['content',['"[ext]"']],
      'endRuleset',
      'endStylesheet'
    ], cssText);
  jsunit.pass();
});

jsunitRegister("testCssParser_empty",
               function testCssParser_empty() {
  assertParsedCssStylesheet(['startStylesheet', 'endStylesheet'], '');
  assertParsedCssStylesheet(['startStylesheet', 'endStylesheet'], ' ');
  assertParsedCssStylesheet(['startStylesheet', 'endStylesheet'], '\t');
  assertParsedCssStylesheet(['startStylesheet', 'endStylesheet'], '\r');
  assertParsedCssStylesheet(['startStylesheet', 'endStylesheet'], '\n');
  assertParsedCssStylesheet(['startStylesheet', 'endStylesheet'], '\r\n');
  assertParsedCssStylesheet(['startStylesheet', 'endStylesheet'], '\f');
  assertParsedCssStylesheet(['startStylesheet', 'endStylesheet'], ' /* foo */ ');
  jsunit.pass();
});

jsunitRegister("testCssParser_color",
               function testCssParser_color() {
  var golden = ['declaration', ['color', ['red']]];
  assertParsedCssDecls(golden, 'color:red');
  assertParsedCssDecls(golden, ' color:red');
  assertParsedCssDecls(golden, 'color :red');
  assertParsedCssDecls(golden, ' color: red');
  assertParsedCssDecls(golden, 'color:red ');
  assertParsedCssDecls(golden, 'color:\nred');
  assertParsedCssDecls(golden, 'color:\r\nred');
  assertParsedCssDecls(golden, 'color:\tred');
  assertParsedCssDecls(golden, 'color:\fred');
  assertParsedCssDecls(['declaration', ['color', ['#f00']]], 'color:#f00');
  assertParsedCssDecls(
      ['declaration', ['color', ['#00ff00']]], 'color:#00ff00');
  jsunit.pass();
});

jsunitRegister("testCssParser_border",
               function testCssParser_border() {
  assertParsedCssDecls(['declaration', ['border', ['1px', 'solid', 'black']]],
                       'border : 1px solid  black');
  jsunit.pass();
});

jsunitRegister("testCssParser_multiple",
               function testCssParser_multiple() {
  assertParsedCssDecls(
      ['declaration', ['font-family', ['Courier']],  // TODO: quote courier
       'declaration', ['background', ['red']]],
      'font-family: Courier; color:; background: red;');
  assertParsedCssDecls(
      ['declaration', ['font-family', ['Courier']],  // TODO: quote courier
       'declaration', ['background', ['red']]],
      'font-family: Courier; color:; background: red');
  jsunit.pass();
});

jsunitRegister('testCssParser_dashed',
               function testCssParser_dashed() {
  assertParsedCssDecls(
      ['declaration', ['-moz-border-radius', ['0']]],
      '-moz-border-radius: 0');
  assertParsedCssDecls(
      ['declaration', ['-moz-border-radius', ['3px']]],
      '-moz-border-radius: 3px');
  assertParsedCssDecls(
      ['declaration', ['-moz-border-radius', ['3']]],
      '-moz-border-radius: 3');
  assertParsedCssDecls(
      ['declaration', ['-moz-border-radius', ['0.5em']]],
      '-moz-border-radius: 0.5em');
  assertParsedCssDecls(
      ['declaration', ['-moz-border-radius', ['.5em']]],
      '-moz-border-radius: .5em');
  assertParsedCssDecls(
      ['declaration', ['-moz-border-radius', ['0.5', 'em']]], // TODO join em
      '-moz-border-radius: 0.5 em');
  jsunit.pass();
});

jsunitRegister("testCssParser_strings",
               function testCssParser_strings() {
  assertParsedCssDecls(
      ['declaration', ['content', ['"."']]], 'content: "."');
  assertParsedCssDecls(
      ['declaration', ['content', ['"."']]], "content: '.'");
  assertParsedCssDecls(
      ['declaration', ['content', ['"\'"']]], 'content: "\'"');
  assertParsedCssDecls(
      ['declaration', ['content', ['"\\22 \\22 "']]], "content: '\"\"'");
  assertParsedCssDecls(
      ['declaration', ['content', ['"foobar"']]],
      "content: 'foo\\\r\nbar'");
  jsunit.pass();
});

jsunitRegister("testCssParser_urls",
               function testCssParser_urls() {
  assertParsedCssDecls(
      ['declaration', ['background-image', ['url("foo:bar")']]],
      'background-image: url("foo:bar")');
  assertParsedCssDecls(
      ['declaration', ['background-image', ['url("foo:bar")']]],
      'background-image: url(\'foo:bar\')');
  assertParsedCssDecls(
      ['declaration', ['background-image', ['url("foo:%22bar%22")']]],
      'background-image: url(\'foo:\"bar\"\')');
  assertParsedCssDecls(
      ['declaration', ['background-image', ['url("foo:%22bar%5c%22")']]],
      'Background-Image: URL(\'foo:\"bar\\5c\"\')');
  assertParsedCssDecls(
      ['declaration', ['background-image', ['url("foo")']]],
      'Background-Image: Url(foo)');
  assertParsedCssDecls(
      ['declaration', ['background-image', ['url("bar%5c-boo")']]],
      'Background-Image: URL(bar\\5c-boo)');
  jsunit.pass();
});

jsunitRegister('testIssue1161',
               function testIssue1161() {
  assertParsedCssDecls(
      ['declaration', ['padding', ['0']],
       'declaration', ['margin', ['5px', '10px']]],
      'padding:0;margin:5px 10px;');
  jsunit.pass();
});

jsunitRegister('testErrorRecovery', function testErrorRecovery() {
  assertParsedCssDecls(
      ['declaration', ['color', ['red']]],
      'font:1:2;color:red');
  jsunit.pass();
});

jsunitRegister("testAttributeSelectors",
               function testAttributeSelectors() {
  assertParsedCssStylesheet(
      ['startStylesheet', [],
       'startRuleset',    [['a', '[', 'href', ']']],
       'declaration',     ['color', ['blue']],
       'endRuleset',      [],
       'endStylesheet',   []],
      'a[href] { color: blue }');
  assertParsedCssStylesheet(
      ['startStylesheet', [],
       'startRuleset',    [['input', '.', 'cl:ass',
                            '[', 'xml:lang', ' ', '~=', ' ', '"en:us"', ']']],
       'declaration',     ['color', ['blue']],
       'endRuleset',      [],
       'endStylesheet',   []],
      'input.cl\\:ass[xml\\:lang ~= "en:us"] { color: blue }');
  jsunit.pass();
});

// https://code.google.com/p/google-caja/issues/detail?id=1770
// check parsing and error recovery when there are no spaces
jsunitRegister('testMinifiedCss1', function testMinifiedCss1() {
  assertParsedCssStylesheet(
      ['startStylesheet', [],
       'startRuleset',    [['a']],
       'endRuleset',      [],
       'startRuleset',    [['b']],
       'declaration',     ['color', ['blue']],
       'endRuleset',      [],
       'endStylesheet',   []],
      'a{bogus}b{color:blue}');
  jsunit.pass();
});

jsunitRegister('testMinifiedCss2', function testMinifiedCss2() {
  assertParsedCssStylesheet(
      ['startStylesheet', [],
       'startAtrule',     ['@something', []],
       'startBlock',
       'startRuleset',    [['a']],
       'declaration',     ['color', ['red']],
       'endRuleset',      [],
       'endBlock',
       'endAtrule',
       'startRuleset',    [['b']],
       'declaration',     ['color', ['blue']],
       'endRuleset',      [],
       'endStylesheet',   []],
      '@something{a{color:red}}b{color:blue}');
  jsunit.pass();
});

jsunitRegister('testMinifiedCss3', function testMinifiedCss3() {
  assertParsedCssStylesheet(
      ['startStylesheet', [],
       'startAtrule',     ['@something', []],
       'startBlock',
       'endBlock',
       'endAtrule',
       'startRuleset',    [['b']],
       'declaration',     ['color', ['blue']],
       'endRuleset',      [],
       'endStylesheet',   []],
      '@something{bogus}b{color:blue}');
  jsunit.pass();
});

jsunitRegister('testMinifiedCss4', function testMinifiedCss4() {
  assertParsedCssStylesheet(
      ['startStylesheet', [],
       'startRuleset',    [['b']],
       'declaration',     ['color', ['red']],
       'declaration',     ['color', ['blue']],
       'endRuleset',      [],
       'endStylesheet',   []],
      'b{color:red;*zoom:1;color:blue}');
  jsunit.pass();
});

jsunitRegister('testMinifiedCss5', function testMinifiedCss5() {
  assertParsedCssStylesheet(
      ['startStylesheet', [],
       'startRuleset',    [['b']],
       'declaration',     ['color', ['red']],
       'declaration',     ['color', ['blue']],
       'endRuleset',      [],
       'startRuleset',    [['c']],
       'declaration',     ['color', ['green']],
       'endRuleset',      [],
       'endStylesheet',   []],
      'b{color:red;zoom:;color:blue;zoom:}c{color:green}');
  jsunit.pass();
});

jsunitRegister('testKeyframes', function testKeyframes() {
  // Mixture of example 1 and example 2 from
  // http://dev.w3.org/csswg/css-animations/
  var input = [
    'div {',
    '  animation-name: diagonal-slide;',
    '  animation-duration: 5s;',
    '  animation-iteration-count: 10;',
    '}',
    '',
    '@keyframes diagonal-slide {',
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
    '@-webkit-keyframes xyzzy {}',
    ''].join('\n');
  assertParsedCssStylesheet(
    [
      "startStylesheet",     [],
        "startRuleset",      [["div"]],
          "declaration",     ["animation-name", ["diagonal-slide"]],
          "declaration",     ["animation-duration", ["5s"]],
          "declaration",     ["animation-iteration-count", ["10"]],
        "endRuleset",        [],
        "startAtrule",       ["@keyframes", ["diagonal-slide"]],
          "startBlock",      [],
            "startRuleset",  [["from"]],  // Not really a ruleset.
              "declaration", ["left", ["0"]],
              "declaration", ["top", ["0"]],
            "endRuleset",    [],
            "startRuleset",  [["50%"]],   // Not really a ruleset.
              "declaration", ["left", ["55px"]],
            "endRuleset",    [],
            "startRuleset",  [["51%", ",", " ", "52%", ",", "53%"]],
              "declaration", ["left", ["56px"]],
            "endRuleset",    [],
            "startRuleset",  [["to"]],    // Not really a ruleset.
              "declaration", ["left", ["100px"]],
              "declaration", ["top", ["100px"]],
            "endRuleset",    [],
          "endBlock",        [],
        "endAtrule",         [],
        "startAtrule",       ["@-webkit-keyframes", ["xyzzy"]],
          "startBlock",      [],
          "endBlock",        [],
        "endAtrule",         [],
      "endStylesheet",       []
    ],
    input);
  jsunit.pass();
});
