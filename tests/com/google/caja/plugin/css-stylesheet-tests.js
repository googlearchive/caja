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
    "test_name": "UnknownTagsRemoved",
    "tests": [
      {
        "cssText": "bogus { display: none }",
        "golden": ""
      },
      {
        "cssText": "a, bogus, i { display: none }",
        "golden": "a, i{display:none}"
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
        "golden": "strike, strong{display:none}",
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
        "golden": "",
        // The JS side emits an empty property group while the Java version
        // does not.
        "altGolden": "a{}"
      },
      {
        "cssText": "a { visibility: hidden; }", 
        "golden": "a{visibility:hidden}"
      },
      // no such property
      {
        "cssText": "a { bogus: bogus }",
        "golden": "",
        "altGolden": "a{}"
      },
      // make sure it doesn't interfere with others
      {
        "cssText": "a { visibility: none; font-weight: bold }",
        "golden": "a{font-weight:bold}"
      },
      {
        "cssText": "a { font-weight: bold; visibility: none }",
        "golden": "a{font-weight:bold}"
      },
      {
        "cssText": "a { bogus: bogus; font-weight: bold }",
        "golden": "a{font-weight:bold}"
      },
      {
        "cssText": "a { font-weight: bold; bogus: bogus }",
        "golden": "a{font-weight:bold}"
      }
    ]
  },
  {
    "test_name": "ContentRemoved",
    "tests": [
      {
        "cssText":
          "a { color: blue; content: 'booyah'; text-decoration: underline; }",
        "golden": "a{color:blue;text-decoration:underline}"
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
        "golden": "b{font-weight:bolder}"
      }
    ]
  },
  {
    "test_name": "FontNamesQuoted",
    "tests": [
      {
        "cssText":
          "a { font:12pt Times  New Roman, Times,\"Times Old Roman\",serif }",
        "golden": "a{font:12pt 'Times New Roman', 'Times',"
            + " 'Times Old Roman', serif}",
        "altGolden": 'a{font:12pt "times new roman" , "times" ,'
            + ' "times old roman" , serif}'
      },
      {
        "cssText": "a { font:bold 12pt Arial Black }",
        "golden": "a{font:bold 12pt 'Arial Black'}",
        "altGolden": 'a{font:bold 12pt "arial black"}'
      }
    ]
  },
  {
    "test_name": "Namespacing",
    "tests": [
      {
        "cssText": "a.foo { color:blue }",
        "golden": "a.foo{color:blue}"
      },
      {
        "cssText": "#foo { color: blue }",
        "golden": "#foo{color:blue}"
      },
      {
        "cssText": "body.ie6 p { color: blue }",
        "golden": "body.ie6 p{color:blue}"
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
        "golden": "* html p{margin:0}"
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
        "golden": "#foo > #bar{color:blue}"
      },
      {
        "cssText": "#foo .bar { color: blue }",
        "golden": "#foo .bar{color:blue}"
      }
    ]
  },
  {
    "test_name": "UnsafeIdentifiers",
    "tests": [
      {
        "cssText": "a.foo, b#c\\2c d, .e { color:blue }",  // "\\2c " -> ","
        "golden": "a.foo, .e{color:blue}"
      },
      {
        "cssText": "a.foo, .b_c {color: blue}",
        "golden": "a.foo, .b_c{color:blue}"
      },
      {
        "cssText": "a.foo, ._c {color: blue}",
        "golden": "a.foo{color:blue}"
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
        "golden": "a:link{color:blue}",
        "altGolden": "a:link{}"  // TODO: Allow history sensitive in JS.
      },
      {
        "cssText": "a:visited { color:blue }",
        "golden": "a:visited{color:blue}",
        "altGolden": "a:visited{}",  // TODO: Allow history sensitive in JS.
        "messages": []
      },

    // Properties that are on DOMita's HISTORY_INSENSITIVE_STYLE_WHITELIST
    // should not be allowed in any rule that correlates with the :visited
    // pseudo selector.
    // TODO: How is this a whitelist then?
      {
        "cssText": 
          "a:visited { color:blue; float:left; _float:left; *float:left }",
        "golden": "a:visited{color:blue}",
        "altGolden": "a:visited{}",
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
        "golden": "a:visited{color:blue}",
        "altGolden": "a:visited{}"  // TODO
      },

      {
        "cssText": "*:visited { color: blue; }",
        "golden": "a:visited{color:blue}",
        "altGolden": "a:visited{}"  // TODO
      },
      {
        "cssText": "#foo:visited { color: blue; }",
        "golden": "a#foo:visited{color:blue}",
        "altGolden": "a#foo:visited{}"  // TODO
      },
      {
        "cssText": ".foo:link { color: blue; }",
        "golden": "a.foo:link{color:blue}",
        "altGolden": "a.foo:link{}"  // TODO
      },

      {
        "cssText": ""
        + "#foo:visited, div, .bar:link, p {\n"
        + "  padding: 1px;\n"
        + "  color: blue;\n"
        + "}",
        "golden": ""
        + "a#foo:visited, a.bar:link{"
        +   "color:blue\n"
        + "}"
        + "div, p{"
        +   "padding:1px;"
        +   "color:blue"
        + "}",
        "altGolden": ""  // TODO: Fix difference in order in Java.
        + "div, p{"
        +   "padding:1px;"
        +   "color:blue"
        + "}"
        + "a#foo:visited, a.bar:link{"
        //+   "color:blue\n"  // TODO
        + "}"
      },

      {
        "cssText": ""
        + "a#foo-bank {"
        + "  background: 'http://whitelisted-host.com/?bank=X&u=Al';"
        + "  color: purple"
        + "}",
        "golden": ""
        + "a#foo-bank{"
        +   "background:url('http://whitelisted-host.com/?bank=X&u=Al');"
        +   "color:purple"
        + "}",
        // TODO: integrate URL policy into CSS sanitizer.
        "altGolden": "a#foo-bank{color:purple}",
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
        "golden": "a#foo-bank:visited{color:purple}",
        "altGolden": "a#foo-bank:visited{}" // TODO
      }
    ]
  },
  {
    "test_name": "NoBadUrls",
    "tests": [
      // ok
      {
        "cssText": "#foo { background: url(/bar.png) }",
        "golden": "#foo{background:url('/foo/bar.png')}",
        //"altGolden": '#foo{backgroud:url("/foo/bar.png")}'  TODO
        "altGolden": '#foo{}'
      },
      {
        "cssText": "#foo { background: url('/bar.png') }",
        "golden": "#foo{background:url('/foo/bar.png')}",
        //"altGolden": '#foo{background:url("/foo/bar.png")}'  TODO
        "altGolden": '#foo{}'
      },
      {
        "cssText": "#foo { background: '/bar.png' }",
        "golden": "#foo{background:url('/foo/bar.png')}",
        //"altGolden": '#foo{background:url("/foo/bar.png")}'  TODO
        "altGolden": '#foo{}'
      },
      {
        "cssText":
          "#foo { background: 'http://whitelisted-host.com/blinky.gif' }",
        "golden":
          "#foo{background:url('http://whitelisted-host.com/blinky.gif')}",
        "altGolden":
//        '#foo{background:url("http://whitelisted-host.com/blinky.gif")}'
          '#foo{}'
      },

      // disallowed
      {
        "cssText": "#foo { background: url('http://cnn.com/bar.png') }",
        "golden": "",
        "altGolden": "#foo{}"
      },
      {
        "cssText": "#foo { background: 'http://cnn.com/bar.png' }",
        "golden": "",
        "altGolden": "#foo{}"
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
        "golden": "div *{margin:0}"
      }
    ]
  },
  {
    "test_name": "UnitlessLengths",
    "tests": [
      {
        "cssText": "div { padding: 10 0 5.0 4 }",
        "golden": "div{padding:10px 0 5.0px 4px}",
        "altGolden": "div{padding:10 0 5.0 4}"
      },
      {
        "cssText": "div { margin: -5 5; z-index: 2 }",
        "golden": "div{margin:-5px 5px;z-index:2}",
        "altGolden": "div{margin:-5 5;z-index:2}"
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
        + "p{"
        +   "color:blue;"
        +   "*color:red;"  // Good user agent hack
        +   "background-color:green;"
        // Bad user-agent hack removed.
        +   "font-weight:bold"
        + "}",
        "altGolden": ""
        + "p{"
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
        "golden": "a.c{_color:blue;margin:0}",
        // TODO: implement user agent hacks
        "altGolden": "a.c{margin:0}",
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
        // TODO: see if special color names work when quoted.
        "altGolden": "a.c{color:lightslategray;background:ivory}",
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
        "golden": "#foo{position:absolute;left:0px;top:0px}",
        "messages": []
      },
      {
        "cssText": "#foo { position: fixed; left: 0px; top: 0px }",
        "golden": "#foo{left:0px;top:0px}",
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
