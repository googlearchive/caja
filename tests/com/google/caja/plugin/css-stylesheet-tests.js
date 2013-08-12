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

/*
 * @fileoverview
 * These test cases are run in JS against santizeCss.js and against the
 * CssRewriter in Java.
 * This file is meant to be in JSONP.  If you strip comments and the
 * runtest call, you can parse the rest as a JSON object.
 */

runCssSelectorTests([
  {
    // font-weight is special because it only admits specific numbers
    "test_name": "FontWeight",
    "tests": [
      {
        "cssText": "p { font-weight: 100; font-weight: 150; }",
        "golden": ".namespace__ p{font-weight:100}"
      }
    ]
  },
  {
    "test_name": "AtRules",
    "tests": [
      {
        "cssText": "@media print { th { font-weight: bolder } }",
        "golden": "@media print{.namespace__ th{font-weight:bolder}}"
      }
    ]
  },
  {
    "test_name": "UnknownTagsVirtualized",
    "tests": [
      {
        "cssText": "bogus { display: none }",
        "golden": ".namespace__ caja-v-bogus{display:none}"
      },
      {
        "cssText": "a, bogus, i { display: none }",
        "golden": ".namespace__ a, .namespace__ caja-v-bogus, .namespace__ i{display:none}"
      }
    ]
  },
  {
    "test_name": "AttrRewritten",
    "es5only": true,
    "tests": [
      {
        "cssText": "div[id=x] { color: blue }",
        "golden": ".namespace__ div[id=\"x-namespace__\"]{color:blue}"
      },
    ]
  },
  {
    "test_name": "UnknownAttrsVirtualized",
    "es5only": true,
    "tests": [
      {
        "cssText": "div[zwop] { color: blue }",
        "golden": ".namespace__ div[data-caja-zwop]{color:blue}"
      },
      {
        "cssText": "div[zwop=zing] { color: blue }",
        "golden": ".namespace__ div[data-caja-zwop=\"zing\"]{color:blue}"
      },
      {
        "cssText": "div[ng\\:app=zing] { color: blue }",
        "golden": ".namespace__ div[data-caja-ng\\:app=\"zing\"]{color:blue}"
      },
      {
        "cssText": "div[rejectedfortest] { color: blue }",
        "golden": ""
      },
    ]
  },
  {
    "test_name": "BadTagsRemoved",
    "tests": [
      {
        "cssText": "script { display: none }",
        "golden": "",
        "messages": [
          {
            "type": "UNSAFE_TAG",
            "level": "ERROR",
            "args": ["script"]
          }
        ]
      },
      {
        "cssText": "strike, script, strong { display: none }",
        "golden": ".namespace__ strike, .namespace__ strong{display:none}",
        "messages": [
          {
            "type": "UNSAFE_TAG",
            "level": "ERROR",
            "args": ["script"]
          }
        ]
      }
    ]
  },
  {
    "test_name": "BadAttribsRemoved",
    "tests": [
      {
        "cssText": "div[rejectedfortest] { color: blue }",
        "golden": ""
      }
    ]
  },
  {
    "test_name": "InvalidPropertiesRemoved",
    "tests": [
      // visibility takes "hidden", not "none"
      {
        "cssText": "a { visibility: none }",
        "golden": "",
        // The JS side emits an empty property group while the Java version
        // does not.
        "altGolden": ".namespace__ a{}"
      },
      {
        "cssText": "a { visibility: hidden; }", 
        "golden": ".namespace__ a{visibility:hidden}"
      },
      // no such property
      {
        "cssText": "a { bogus: bogus }",
        "golden": "",
        "altGolden": ".namespace__ a{}"
      },
      // make sure it doesn't interfere with others
      {
        "cssText": "a { visibility: none; font-weight: bold }",
        "golden": ".namespace__ a{font-weight:bold}"
      },
      {
        "cssText": "a { font-weight: bold; visibility: none }",
        "golden": ".namespace__ a{font-weight:bold}"
      },
      {
        "cssText": "a { bogus: bogus; font-weight: bold }",
        "golden": ".namespace__ a{font-weight:bold}"
      },
      {
        "cssText": "a { font-weight: bold; bogus: bogus }",
        "golden": ".namespace__ a{font-weight:bold}"
      }
    ]
  },
  {
    "test_name": "UrlContentRemoved",
    "tests": [
      {
        "cssText":
          "a { color: blue; content: url(x.png); text-decoration: underline; }",
        "golden":
          ".namespace__ a{color:blue;text-decoration:underline}"
      }
    ]
  },
  {
    "test_name": "LiteralContentPreserved",
    "tests": [
      {
        "cssText":
          "a { color: blue; content: 'booyah'; text-decoration: underline; }",
        "golden":
          ".namespace__ a{color:blue;content:\"booyah\";"
              + "text-decoration:underline}"
      }
    ]
  },
  {
    "test_name": "AttrRemoved",
    "tests": [
      {
        "cssText": "a:attr(href) { color: blue }",
        "golden": ""
      },
      {
        "cssText": "a:attr(href) { color: blue } b { font-weight: bolder }",
        "golden": ".namespace__ b{font-weight:bolder}"
      }
    ]
  },
  {
    "test_name": "FontNamesQuoted",
    "tests": [
      {
        "cssText":
          "a { font:12pt Times  New Roman, Times,\"Times Old Roman\",serif }",
        "golden": ".namespace__ a{font:12pt 'Times New Roman', 'Times',"
            + " 'Times Old Roman', serif}",
        "altGolden": '.namespace__ a{font:12pt "times new roman" , "times" ,'
            + ' "times old roman" , serif}'
      },
      {
        "cssText": "a { font:bold 12pt Arial Black }",
        "golden": ".namespace__ a{font:bold 12pt 'Arial Black'}",
        "altGolden": '.namespace__ a{font:bold 12pt "arial black"}'
      }
    ]
  },
  {
    "test_name": "Namespacing",
    "tests": [
      {
        "cssText": "a.foo { color:blue }",
        "golden": ".namespace__ a.foo{color:blue}"
      },
      {
        "cssText": "#foo { color: blue }",
        "golden": ".namespace__ #foo-namespace__{color:blue}"
      },
      {
        "cssText": "body.ie6 p { color: blue }",
        "golden": ".namespace__ caja-v-body.ie6 p{color:blue}"
      },
      {
        "cssText": "body { margin: 0; }",
        "golden": ".namespace__ caja-v-body{margin:0}"
      },
      {
        "cssText": "body.ie6 { margin: 0; }",
        "golden": ".namespace__ caja-v-body.ie6{margin:0}"
      },
      {
        "cssText": "* html p { margin: 0; }",
        "golden": ".namespace__ * caja-v-html p{margin:0}"
      },
      {
        "cssText": "* html { margin: 0; }",
        "golden": ".namespace__ * caja-v-html{margin:0}"
      },
      {
        "cssText": "* html > * > p { margin: 0; }",
        "golden": ".namespace__ * caja-v-html>*>p{margin:0}"
      },
      {
        "cssText": "#foo > #bar { color: blue }",
        "golden": ".namespace__ #foo-namespace__>#bar-namespace__{color:blue}"
      },
      {
        "cssText": "#foo .bar { color: blue }",
        "golden": ".namespace__ #foo-namespace__ .bar{color:blue}"
      }
    ]
  },
  {
    "test_name": "UnsafeIdentifiers",
    "tests": [
      {
        "cssText": "a.foo, b#c\\2c d, .e { color:blue }",  // "\\2c " -> ","
        "golden": ".namespace__ a.foo, .namespace__ .e{color:blue}"
      },
      {
        "cssText": "a.foo, .b_c {color: blue}",
        "golden": ".namespace__ a.foo, .namespace__ .b_c{color:blue}"
      },
      {
        "cssText": "a.foo, ._c {color: blue}",
        "golden": ".namespace__ a.foo{color:blue}"
      },
      {
        "cssText": "a._c {_color: blue; margin:0;}",
        "golden": ""
      },
      {
        "cssText": "a#_c {_color: blue; margin:0;}",
        "golden": ""
      },
      {
        "cssText": ".c__ {_color: blue; margin:0;}",
        "golden": ""
      },
      {
        "cssText": "#c__ {_color: blue; margin:0;}",
        "golden": ""
      }
    ]
  },
  {  
    "test_name": "PseudosWhitelisted",
    "tests": [
      {
        "cssText": "a:link, a:badness { color:blue }",
        "golden": ".namespace__ a:link{color:blue}"
      },
      {
        "cssText": "a:visited { color:blue }",
        "golden": ".namespace__ a:visited{color:blue}",
        "messages": []
      },

    // Properties that are on Domado's HISTORY_INSENSITIVE_STYLE_WHITELIST
    // should not be allowed in any rule that correlates with the :visited
    // pseudo selector.
    // TODO: How is this a whitelist then?
      {
        "cssText": 
          "a:visited { color:blue; float:left; _float:left; *float:left }",
        "golden": ".namespace__ a:visited{color:blue}",
        "messages": [
          {
            "type": "DISALLOWED_CSS_PROPERTY_IN_SELECTOR",
            "level": "ERROR",
            "args": [
              "test:1+25@25 - 30@30",
              "float",
              "test:1+1@1 - 10@10"
            ]
          },
          {
            "type": "DISALLOWED_CSS_PROPERTY_IN_SELECTOR",
            "level": "ERROR",
            "args": [
              "test:1+37@37 - 43@43",
              "_float",
              "test:1+1@1 - 10@10"
            ]
          },
          {
            "type": "DISALLOWED_CSS_PROPERTY_IN_SELECTOR",
            "level": "ERROR",
            "args": [
              "test:1+51@51 - 56@56",
              "float",
              "test:1+1@1 - 10@10"
            ]
          }
        ]
      },
      {
        "cssText":
          "a:visited { COLOR:blue; FLOAT:left; _FLOAT:left; *FLOAT:left }",
        "golden": ".namespace__ a:visited{color:blue}"
      },

      {
        "cssText": "*:visited { color: blue; }",
        "golden": ".namespace__ a:visited{color:blue}"
      },
      {
        "cssText": "#foo:visited { color: blue; }",
        "golden": ".namespace__ a#foo-namespace__:visited{color:blue}"
      },
      {
        "cssText": ".foo:link { color: blue; }",
        "golden": ".namespace__ a.foo:link{color:blue}"
      },

      {
        "cssText": ""
        + "#foo:visited, div, .bar:link, p {\n"
        + "  padding: 1px;\n"
        + "  color: blue;\n"
        + "}",
        "golden": ""
        + ".namespace__ a#foo-namespace__:visited, .namespace__ a.bar:link{"
        +   "color:blue\n"
        + "}"
        + ".namespace__ div, .namespace__ p{"
        +   "padding:1px;"
        +   "color:blue"
        + "}",
        "altGolden": ""  // TODO: Fix difference in order in Java.
        + ".namespace__ div, .namespace__ p{"
        +   "padding:1px;"
        +   "color:blue"
        + "}"
        + ".namespace__ a#foo-namespace__:visited, .namespace__ a.bar:link{"
        +   "color:blue"
        + "}"
      },

      {
        "cssText": ""
        + "a#foo-bank {"
        + "  background: 'http://whitelisted-host.com/?bank=X&u=Al';"
        + "  color: purple"
        + "}",
        "golden": ""
        + ".namespace__ a#foo-bank-namespace__{"
        +   "background:url(\"http://whitelisted-host.com/?bank=X&u=Al\");"
        +   "color:purple"
        + "}",
        "messages": []
      },
      // Differs from the previous only in that it has the :visited pseudo
      // selector which means we can't allow it to cause a network fetch because
      // that could leak user history state.

      {
        "cssText": ""
        + "a#foo-bank:visited {"
        + "  background-image: 'http://whitelisted-host.com/?bank=X&u=Al';"
        + "  color: purple"
        + "}",
        "golden": ".namespace__ a#foo-bank-namespace__:visited{color:purple}"
      }
    ]
  },
  {
    "test_name": "NoBadUrls",
    "tests": [
      // ok
      {
        "cssText": "#foo { background: url(/bar.png) }",
        "golden": ".namespace__ #foo-namespace__"
          + "{background:url(\"/foo/bar.png\")}"
      },
      {
        "cssText": "#foo { background: url('/bar.png') }",
        "golden": ".namespace__ #foo-namespace__"
          + "{background:url(\"/foo/bar.png\")}"
      },
      {
        "cssText": "#foo { background: '/bar.png' }",
        "golden": ".namespace__ #foo-namespace__"
          + "{background:url(\"/foo/bar.png\")}"
      },
      {
        "cssText":
          "#foo { background: 'http://whitelisted-host.com/blinky.gif' }",
        "golden":
          ".namespace__ #foo-namespace__"
          + "{background:url(\"http://whitelisted-host.com/blinky.gif\")}"
      },

      // disallowed
      {
        "cssText": "#foo { background: url('http://cnn.com/bar.png') }",
        "golden": "",
        "altGolden": ".namespace__ #foo-namespace__{}"
      },
      {
        "cssText": "#foo { background: 'http://cnn.com/bar.png' }",
        "golden": "",
        "altGolden": ".namespace__ #foo-namespace__{}"
      }
    ]
  },
  {
    // "*" selectors should rewrite properly.
    // http://code.google.com/p/google-caja/issues/detail?id=57
    "test_name": "WildcardSelectors",
    "tests": [
      {
        "cssText": "div * { margin: 0; }",
        "golden": ".namespace__ div *{margin:0}"
      }
    ]
  },
  {
    "test_name": "UnitlessLengths",
    "tests": [
      {
        "cssText": "div { padding: 10 0 5.0 4 }",
        "golden": ".namespace__ div{padding:10px 0 5.0px 4px}",
        "altGolden": ".namespace__ div{padding:10 0 5.0 4}"
      },
      {
        "cssText": "div { margin: -5 5; z-index: 2 }",
        "golden": ".namespace__ div{margin:-5px 5px;z-index:2}",
        "altGolden": ".namespace__ div{margin:-5 5;z-index:2}"
      }
    ]
  },
  {
    "test_name": "UserAgentHacks",
    "tests": [
      {
        "cssText": ""
        + "p {\n"
        + "  color: blue;\n"
        + "  *color: red;\n"
        + "  background-color: green;\n"
        + "  *background-color: yelow;\n"  // misspelled
        + "  font-weight: bold\n"
        + "}",
        "golden": ""
        + ".namespace__ p{"
        +   "color:blue;"
        +   "*color:red;"  // Good user agent hack
        +   "background-color:green;"
        // Bad user-agent hack removed.
        +   "font-weight:bold"
        + "}",
        "altGolden": ""
        + ".namespace__ p{"
        +   "color:blue;"
        // TODO: Implement support for user-agent hacks.
        //+   "*color:red;"  // Good user agent hack
        +   "background-color:green;"
        // Bad user-agent hack removed.
        +   "font-weight:bold"
        + "}",
        "messages": [
          {
            "type": "MALFORMED_CSS_PROPERTY_VALUE",
            "level": "WARNING",
            "args": [
              "background-color",
              "==>yelow<=="
            ]
          }
        ]
      },
      {
        "cssText": "a.c {_color: blue; margin:0;}",
        "golden": ".namespace__ a.c{_color:blue;margin:0}",
        // TODO: implement user agent hacks
        "altGolden": ".namespace__ a.c{margin:0}",
        "messages": []
      }
    ]
  },
  {
    "test_name": "NonCSS21Colors",
    "tests": [
      {
        "cssText": "a.c { color: LightSlateGray; background: ivory; }",
        "golden": ".namespace__ a.c {\n  color: LightSlateGray;\n  background: ivory\n}",
        "altGolden": ".namespace__ a.c{color:lightslategray;background:ivory}",
        "messages": []
      }
    ]
  },
  {
    "test_name": "FixedPositioning",
    "tests": [
      {
        "cssText": "#foo { position: absolute; left: 0px; top: 0px }",
        "golden": ".namespace__ #foo-namespace__{position:absolute;left:0px;top:0px}",
        "messages": []
      },
      {
        "cssText": "#foo { position: fixed; left: 0px; top: 0px }",
        "golden": ".namespace__ #foo-namespace__{left:0px;top:0px}",
        "messages": [
          // TODO(mikesamuel): fix message.
          // "fixed" is well-formed but disallowed.
          {
            "type": "MALFORMED_CSS_PROPERTY_VALUE",
            "level": "WARNING",
            "args": [
              "position",
              "==>fixed<=="
            ]
          }
        ]
      }
    ]
  }]
)
