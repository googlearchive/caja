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

runTests([
  {
    "test_name": "UnknownTagsRemoved",
    "tests": [
      {
        "cssText": "bogus { display: none }",
        "golden": ""
      },
      {
        "cssText": "a, bogus, i { display: none }",
        "golden": "a, i {\n  display: none\n}"
      }
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
        "golden": "strike, strong {\n  display: none\n}",
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
        "cssText": "div[zwop] { color: blue }",
        "golden": ""
      },
    ]
  },
  {
    "test_name": "InvalidPropertiesRemoved",
    "tests": [
      // visibility takes "hidden", not "none"
      {
        "cssText": "a { visibility: none }",
        "golden": ""
      },
      {
        "cssText": "a { visibility: hidden; }", 
        "golden": "a {\n  visibility: hidden\n}"
      },
      // no such property
      {
        "cssText": "a { bogus: bogus }",
        "golden": ""
      },
      // make sure it doesn't interfere with others
      {
        "cssText": "a { visibility: none; font-weight: bold }",
        "golden": "a {\n  font-weight: bold\n}"
      },
      {
        "cssText": "a { font-weight: bold; visibility: none }",
        "golden": "a {\n  font-weight: bold\n}"
      },
      {
        "cssText": "a { bogus: bogus; font-weight: bold }",
        "golden": "a {\n  font-weight: bold\n}"
      },
      {
        "cssText": "a { font-weight: bold; bogus: bogus }",
        "golden": "a {\n  font-weight: bold\n}"
      }
    ]
  },
  {
    "test_name": "ContentRemoved",
    "tests": [
      {
        "cssText":
          "a { color: blue; content: 'booyah'; text-decoration: underline; }",
        "golden": "a {\n  color: blue;\n  text-decoration: underline\n}"
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
        "golden": "b {\n  font-weight: bolder\n}"
      }
    ]
  },
  {
    "test_name": "FontNamesQuoted",
    "tests": [
      {
        "cssText":
          "a { font:12pt Times  New Roman, Times,\"Times Old Roman\",serif }",
        "golden": "a {\n  font: 12pt 'Times New Roman', 'Times',"
            + " 'Times Old Roman', serif\n}"
      },
      {
        "cssText": "a { font:bold 12pt Arial Black }",
        "golden": "a {\n  font: bold 12pt 'Arial Black'\n}"
      }
    ]
  },
  {
    "test_name": "Namespacing",
    "tests": [
      {
        "cssText": "a.foo { color:blue }",
        "golden": "a.foo {\n  color: blue\n}"
      },
      {
        "cssText": "#foo { color: blue }",
        "golden": "#foo {\n  color: blue\n}"
      },
      {
        "cssText": "body.ie6 p { color: blue }",
        "golden": "body.ie6 p {\n  color: blue\n}"
      },
      {
        "cssText": "body { margin: 0; }",
        "golden": ""
      },  // Not allowed
      {
        "cssText": "body.ie6 { margin: 0; }",
        "golden": ""
      },  // Not allowed
      {
        "cssText": "* html p { margin: 0; }",
        "golden": "* html p {\n  margin: 0\n}"
      },
      {
        "cssText": "* html { margin: 0; }",
        "golden": ""
      },  // Not allowed
      {
        "cssText": "* html > * > p { margin: 0; }",
        "golden": ""
      },  // Not allowed
      {
        "cssText": "#foo > #bar { color: blue }",
        "golden": "#foo > #bar {\n  color: blue\n}"
      },
      {
        "cssText": "#foo .bar { color: blue }",
        "golden": "#foo .bar {\n  color: blue\n}"
      }
    ]
  },
  {
    "test_name": "UnsafeIdentifiers",
    "tests": [
      {
        "cssText": "a.foo, b#c\\2c d, .e { color:blue }",  // "\\2c " -> ","
        "golden": "a.foo, .e {\n  color: blue\n}"
      },
      {
        "cssText": "a.foo, .b_c {color: blue}",
        "golden": "a.foo, .b_c {\n  color: blue\n}"
      },
      {
        "cssText": "a.foo, ._c {color: blue}",
        "golden": "a.foo {\n  color: blue\n}"
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
        "golden": "a:link {\n  color: blue\n}"
      },
      {
        "cssText": "a:visited { color:blue }",
        "golden": "a:visited {\n  color: blue\n}",
        "messages": []
      },

    // Properties that are on DOMita's HISTORY_INSENSITIVE_STYLE_WHITELIST
    // should not be allowed in any rule that correlates with the :visited
    // pseudo selector.
    // TODO: How is this a whitelist then?
      {
        "cssText": 
          "a:visited { color:blue; float:left; _float:left; *float:left }",
        "golden": "a:visited {\n  color: blue\n}",
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
        "golden": "a:visited {\n  color: blue\n}"
      },

      {
        "cssText": "*:visited { color: blue; }",
        "golden": "a:visited {\n  color: blue\n}"
      },
      {
        "cssText": "#foo:visited { color: blue; }",
        "golden": "a#foo:visited {\n  color: blue\n}"
      },
      {
        "cssText": ".foo:link { color: blue; }",
        "golden": "a.foo:link {\n  color: blue\n}"
      },

      {
        "cssText": ""
        + "#foo:visited, div, .bar:link, p {\n"
        + "  padding: 1px;\n"
        + "  color: blue;\n"
        + "}",
        "golden": ""
        + "a#foo:visited, a.bar:link {\n"
        + "  color: blue\n"
        + "}\n"
        + "div, p {\n"
        + "  padding: 1px;\n"
        + "  color: blue\n"
        + "}"
      },

      {
        "cssText": ""
        + "a#foo-bank {"
        + "  background: 'http://whitelisted-host.com/?bank=X&u=Al';"
        + "  color: purple"
        + "}",
        "golden": ""
        + "a#foo-bank {\n"
        + "  background: url('http://whitelisted-host.com/?bank=X&u=Al');\n"
        + "  color: purple\n"
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
        "golden": ""
        + "a#foo-bank:visited {\n"
        + "  color: purple\n"
        + "}"
      }
    ]
  },
  {
    "test_name": "NoBadUrls",
    "tests": [
      // ok
      {
        "cssText": "#foo { background: url(/bar.png) }",
        "golden": "#foo {\n  background: url('/foo/bar.png')\n}"
      },
      {
        "cssText": "#foo { background: url('/bar.png') }",
        "golden": "#foo {\n  background: url('/foo/bar.png')\n}"
      },
      {
        "cssText": "#foo { background: '/bar.png' }",
        "golden": "#foo {\n  background: url('/foo/bar.png')\n}"
      },
      {
        "cssText":
          "#foo { background: 'http://whitelisted-host.com/blinky.gif' }",
        "golden":
          "#foo {\n  background: url('http://whitelisted-host.com/blinky.gif')\n}"
      },

      // disallowed
      {
        "cssText": "#foo { background: url('http://cnn.com/bar.png') }",
        "golden": ""
      },
      {
        "cssText": "#foo { background: 'http://cnn.com/bar.png' }",
        "golden": ""
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
        "golden": "div * {\n  margin: 0\n}"
      }
    ]
  },
  {
    "test_name": "UnitlessLengths",
    "tests": [
      {
        "cssText": "div { padding: 10 0 5.0 4 }",
        "golden": "div {\n  padding: 10px 0 5.0px 4px\n}"
      },
      {
        "cssText": "div { margin: -5 5; z-index: 2 }",
        "golden": "div {\n  margin: -5px 5px;\n  z-index: 2\n}"
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
        + "p {\n"
        + "  color: blue;\n"
        + "  *color: red;\n"  // Good user agent hack
        + "  background-color: green;\n"
        // Bad user-agent hack removed.
        + "  font-weight: bold\n"
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
        "golden": "a.c {\n  _color: blue;\n  margin: 0\n}",
        "messages": []
      }
    ]
  },
  {
    "test_name": "NonStandardColors",
    "tests": [
      {
        "cssText": "a.c { color: LightSlateGray; background: ivory; }",
        "golden": "a.c {\n  color: #789;\n  background: #fffff0\n}",
        "messages": [
          {
            "type": "NON_STANDARD_COLOR",
            "level": "LINT",
            "args": [
              "lightslategray",
              "#789"
            ]
          },
          {
            "type": "NON_STANDARD_COLOR",
            "level": "LINT",
            "args": [
              "ivory",
              "#fffff0"
            ]
          }
        ]
      }
    ]
  },
  {
    "test_name": "FixedPositioning",
    "tests": [
      {
        "cssText": "#foo { position: absolute; left: 0px; top: 0px }",
        "golden": "#foo {\n  position: absolute;\n  left: 0px;\n  top: 0px\n}",
        "messages": []
      },
      {
        "cssText": "#foo { position: fixed; left: 0px; top: 0px }",
        "golden": "#foo {\n  left: 0px;\n  top: 0px\n}",
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
