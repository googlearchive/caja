// Copyright (C) 2012 Google Inc.
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

assertSelector("#c__", [[], []]);
assertSelector("#foo .bar", [["#foo .bar"], []]);
assertSelector("#foo > #bar", [["#foo > #bar"], []]);
assertSelector("#foo", [["#foo"], []]);
assertSelector("#foo:visited", [[], ["a#foo:visited"]]);
assertSelector("#foo:visited, div, .bar:link, p",
               [["div", "p"], ["a#foo:visited", "a.bar:link"]]);
assertSelector("* html > * > p", [[], []]);
assertSelector("* html p", [["* html p"], []]);
assertSelector("* html", [[], []]);
assertSelector("*:visited", [[], ["a:visited"]]);
assertSelector(".c__", [[], []]);
assertSelector(".foo:link", [[], ["a.foo:link"]]);
assertSelector("a", [["a"], []]);
assertSelector("a#_c", [[], []]);
assertSelector("a#foo-bank", [["a#foo-bank"], []]);
assertSelector("a#foo-bank:visited", [[], ["a#foo-bank:visited"]]);
assertSelector("a, bogus, i", [["a", "i"], []]);
assertSelector("a._c", [[], []]);
assertSelector("a.c", [["a.c"], []]);
assertSelector("a.foo", [["a.foo"], []]);
assertSelector("a.foo, ._c", [["a.foo"], []]);
assertSelector("a.foo, .b_c", [["a.foo", ".b_c"], []]);
assertSelector("a.foo, b#c\2c d, .e", [["a.foo", ".e"], []]);
assertSelector("a:attr(href)", [[], []]);
assertSelector("a:attr(href), b", [["b"], []]);
assertSelector("a:link, a:badness", [[], ["a:link"]]);
assertSelector("a:visited", [[], ["a:visited"]]);
assertSelector("body", [[], []]);
assertSelector("body.ie6 p", [["body.ie6 p"], []]);
assertSelector("body.ie6", [[], []]);
assertSelector("bogus", [[], []]);
assertSelector("div *", [["div *"], []]);
assertSelector("div", [["div"], []]);
assertSelector("div[zwop]", [[], []]);
assertSelector("p", [["p"], []]);
assertSelector("script", [[], []]);
assertSelector("strike, script, strong", [["strike", "strong"], []]);

function assertSelector(selectorText, safeSelectors) {
  var selectorTokens = [];
  parseCssStylesheet(
      selectorText + ' {}',
      {
        startRuleset: function (selector) {
          selectorTokens.push.apply(selectorTokens, selector);
        }
      });
  var selectors = sanitizeCssSelectors(selectorTokens);
  assertEquals(
      selectorText,
      JSON.stringify(safeSelectors),
      JSON.stringify(selectors));
}
