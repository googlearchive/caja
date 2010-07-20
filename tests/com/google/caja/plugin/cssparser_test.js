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

function assertParsedCss(golden, cssText) {
  var actual = [];
  function handler(propName, tokens) {
    actual.push(propName, '(' + tokens.join(' ') + ')');
  }
  cssparser.parse(cssText, handler);
  assertEquals(golden.join('\n'), actual.join('\n'));
}

jsunitRegister("testCssParser_empty",
               function testCssParser_empty() {
  assertParsedCss([], '');
  assertParsedCss([], ' ');
  assertParsedCss([], '\t');
  assertParsedCss([], '\r');
  assertParsedCss([], '\n');
  assertParsedCss([], '\r\n');
  assertParsedCss([], '\f');
  assertParsedCss([], ' /* foo */ ');
  jsunit.pass();
});

jsunitRegister("testCssParser_color",
               function testCssParser_color() {
  assertParsedCss(['color', '(red)'], 'color:red');
  assertParsedCss(['color', '(red)'], ' color:red');
  assertParsedCss(['color', '(red)'], 'color :red');
  assertParsedCss(['color', '(red)'], ' color: red');
  assertParsedCss(['color', '(red)'], 'color:red ');
  assertParsedCss(['color', '(red)'], 'color:\nred');
  assertParsedCss(['color', '(red)'], 'color:\r\nred');
  assertParsedCss(['color', '(red)'], 'color:\tred');
  assertParsedCss(['color', '(red)'], 'color:\fred');
  assertParsedCss(['color', '(#f00)'], 'color:#f00');
  assertParsedCss(['color', '(#00ff00)'], 'color:#00ff00');
  jsunit.pass();
});

jsunitRegister("testCssParser_border",
               function testCssParser_border() {
  assertParsedCss(['border', '(1px solid black)'], 'border : 1px solid  black');
  jsunit.pass();
});

jsunitRegister("testCssParser_multiple",
               function testCssParser_multiple() {
  assertParsedCss(
      ['font-family', '(Courier)', 'background', '(red)'],
      'font-family: Courier; color:; background: red;');
  assertParsedCss(
      ['font-family', '(Courier)', 'background', '(red)'],
      'font-family: Courier; color:; background: red');
  jsunit.pass();
});

jsunitRegister('testCssParser_dashed',
               function testCssParser_dashed() {
  assertParsedCss(
      ['-moz-border-radius', '(0)'],
      '-moz-border-radius: 0');
  assertParsedCss(
      ['-moz-border-radius', '(3px)'],
      '-moz-border-radius: 3px');
  assertParsedCss(
      ['-moz-border-radius', '(3)'],
      '-moz-border-radius: 3');
  assertParsedCss(
      ['-moz-border-radius', '(0.5em)'],
      '-moz-border-radius: 0.5em');
  assertParsedCss(
      ['-moz-border-radius', '(.5em)'],
      '-moz-border-radius: .5em');
  assertParsedCss(
      ['-moz-border-radius', '(0.5em)'],
      '-moz-border-radius: 0.5 em');
  jsunit.pass();
});

jsunitRegister("testCssParser_strings",
               function testCssParser_strings() {
  assertParsedCss(['content', '(".")'], 'content: "."');
  assertParsedCss(['content', '(".")'], "content: '.'");
  assertParsedCss(['content', '("\'")'], 'content: "\'"');
  assertParsedCss(['content', '("\\22 \\22 ")'], "content: '\"\"'");
  assertParsedCss(['content', '("foo\\d \\a bar")'], "content: 'foo\\\r\nbar'");
  jsunit.pass();
});

jsunitRegister("testCssParser_urls",
               function testCssParser_urls() {
  assertParsedCss(
      ['background-image', '(url("foo:bar"))'],
      'background-image: url("foo:bar")');
  assertParsedCss(
      ['background-image', '(url("foo:bar"))'],
      'background-image: url(\'foo:bar\')');
  assertParsedCss(
      ['background-image', '(url("foo:\\22 bar\\22 "))'],
      'background-image: url(\'foo:\"bar\"\')');
  assertParsedCss(
      ['background-image', '(url("foo:\\22 bar\\5c \\22 "))'],
      'Background-Image: URL(\'foo:\"bar\\5c\"\')');
  assertParsedCss(
      ['background-image', '(url("foo"))'],
      'Background-Image: Url(foo)');
  assertParsedCss(
      ['background-image', '(url("bar\\5c -boo"))'],
      'Background-Image: URL(bar\\5c-boo)');
  jsunit.pass();
});

jsunitRegister('testIssue1161',
               function testIssue1161() {
  assertParsedCss(
      ['padding', '(0)', 'margin', '(5px 10px)'],
      ';padding:0;margin:5px 10px;');
  jsunit.pass();
});

jsunitRegister('testCssParser_toUrl',
               function testCssParser_toUrl() {
  assertEquals(null, cssparser.toUrl(''));
  assertEquals(null, cssparser.toUrl('foo'));
  assertEquals('', cssparser.toUrl('url()'));
  assertEquals('', cssparser.toUrl('URL()'));
  assertEquals('', cssparser.toUrl('url("")'));
  assertEquals('', cssparser.toUrl('url(\'\')'));
  assertEquals('bAr-bAz', cssparser.toUrl('url(b\\41r-b\\41z)'));
  jsunit.pass();
});

jsunitRegister('testCssParser_toCssStr',
               function testCssParser_toCssStr() {
  assertEquals('""', cssparser.toCssStr(''));
  assertEquals('"a"', cssparser.toCssStr('a'));
  assertEquals('"\\22 "', cssparser.toCssStr('"'));
  assertEquals('"\\22 \\22 "', cssparser.toCssStr('""'));
  assertEquals('"\\5c 22 \\5c 22 "', cssparser.toCssStr('\\22 \\22 '));
  jsunit.pass();
});
