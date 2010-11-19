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
 * Scripts for browser_test_case.html.
 *
 * This page sets up a complete testing environment using ES53, and provides
 * quick and easy access to jsUnit test functions. For programmatic testing,
 * it is invoked by Java class BrowserTestCase.
 *
 *
 * ***** URL PARAMETERS *****
 *
 * This page is invoked with one or the other of the following parameters:
 *
 *   ?test-driver=<javascripturl>
 *
 *       Loads the script at <javascripturl> into the top level JavaScript
 *       context of this page as a test driver. This script is run un-cajoled;
 *       it may set up some further testing environment then use the functions
 *       provided by this page to load cajoled code and performs tests.
 *
 *   ?test-case=<htmlurl>
 *
 *       Invokes a default test driver that cajoles and loads the HTML file
 *       at <htmlurl> in a sandbox where jsUnit functions and other utilities
 *       are provided by default (see description of standard imports below).
 *       The HTML file is expected to register and run tests.
 *
 * In case both parameters are provided, "test-driver" is given priority.
 *
 *
 * ***** TOP LEVEL VARIABLES PROVIDED *****
 *
 * This script defines the following symbols at the top level that can be
 * used (or, in some cases, overridden) by test drivers.
 *
 *   getUrlParam(name)
 *
 *      Given the name of a URL parameter, obtain its value as specified in
 *      the URL used to invoke this document, or the empty string if the
 *      parameter is not specified.
 *
 *   readyToTest()
 *
 *       Must be called by the test driver when all test setup is complete.
 *       This is essential when the test is being invoked programmatically
 *       by Java class BrowserTestCase.
 *
 *   createDiv()
 *
 *       Simple utility to create and <DIV>, append it to the document body
 *       and return it.
 *
 *   createExtraImportsForTesting(frameGroup, frame)
 *
 *       Given an ES5 frame object, returns a set of standard imports that can
 *       be provided to cajoled code, tamed for use by the cajoled code. The
 *       standard imports are described below.
 *
 *   caja
 *
 *       At the time the test driver is running, an instance of the "caja"
 *       object, as defined in "c/g/c/caja.js", will be available.
 *
 *   setUp(), tearDown()
 *
 *       Functions expected by jsUnit that the test driver may override.
 *
 *   jsunitRegister, jsunitRun, assertTrue(), assertEquals(), ...
 *
 *       All jsUnit objects and functions are available at the top level.
 *
 *
 * ***** CONTENTS OF THE STANDARD IMPORTS *****
 *
 *   console
 *
 *       A console object.
 *
 *   jsunitRegister, jsunitRun, assertTrue(), assertEquals(), ...
 *
 *       Tamed versions of all jsUnit objects and functions are provided.
 *
 *  [ TODO(ihab.awad): Document more as we determine they are useful. ]
 *
 */
function setUp() { }
function tearDown() { }

// URL parameter parsing code from blog at:
// http://www.netlobo.com/url_query_string_javascript.html
function getUrlParam(name) {
  name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
  var regexS = "[\\?&]"+name+"=([^&#]*)";
  var regex = new RegExp(regexS);
  var results = regex.exec(window.location.href);
  return (results == null) ? "" : results[1];
}

function pageLoaded___() {
  var scriptTag = document.createElement('script');
  scriptTag.setAttribute('src',
      getUrlParam('test-driver')
      || 'default-test-driver.js');
  document.body.appendChild(scriptTag);
}

function readyToTest() {
  document.getElementById('automatedTestingReadyIndicator')
      .className = 'readytotest';
}

function createDiv() {
  var d = document.createElement('div');
  document.body.appendChild(d);
  return d;
}

function createExtraImportsForTesting(frameGroup, frame) {
  var standardImports = {};

  standardImports.readyToTest = frameGroup.tame(readyToTest);
  standardImports.jsunitRun = frameGroup.tame(jsunitRun);
  standardImports.jsunitRegister = frameGroup.tame(jsunitRegister);
  standardImports.jsunitCallback = jsunitCallback;
  standardImports.console = frameGroup.tame({
    log: function () {
      console.log.apply(console, arguments);
    },
    warn: function () {
      console.warn.apply(console, arguments);
    },
    error: function () {
      console.error.apply(console, arguments);
    },
    trace: function () {
      console.trace ? console.trace()
          : console.error.apply(console, arguments);
    }
  });

  standardImports.$ = frameGroup.tame(function(id) {
    return frame.imports.document.getElementById(id);
  });

  // Give unfiltered DOM access so we can check the results of actions.
  standardImports.directAccess = frameGroup.tame({
    // Allow testing of emitHtml by exposing it for testing
    click: function (tameNode) {
      tameNode.node___.click();
    },
    emitCssHook: function (css) {
      standardImports.emitCss___(css.join('xyz___'));
    },
    getInnerHTML: function (tameNode) {
      return tameNode.node___.innerHTML;
    },
    getAttribute: function (tameNode, name) {
      return tameNode.node___.getAttribute(name);
    },
    getBodyNode: function () {
      return frame.imports.tameNode___(/* TODO(ihab.awad) ??? */ testDomContainer);
    },
    getComputedStyle: function (tameNode, styleProp) {
      var node = tameNode.node___;
      if (node.currentStyle) {
        return node.currentStyle[styleProp.replace(
            /-([a-z])/g,
            function (_, letter) {
              return letter.toUpperCase();
            })];
      } else if (window.getComputedStyle) {
        return window.getComputedStyle(node, null)
            .getPropertyValue(styleProp);
      } else {
        return null;
      }
    },
    // Lets tests check that an outer hull breach -- access to
    // an unexecuted script node -- does not allow a full breach.
    makeUnattachedScriptNode: function () {
      var s = document.createElement('script');
      s.appendChild(document.createTextNode('/* intentionally blank */'));
      return frame.imports.tameNode___(s, true);
    },
  });

  // Marks a container green to indicate that test passed
  standardImports.pass = frameGroup.tame(function (id) {
    jsunit.pass(id);
    var node = frame.imports.document.getElementById(id);
    if (!node) return;
    node = node.node___;
    node.appendChild(document.createTextNode('Passed ' + id));
    var cl = node.className || '';
    cl = cl.replace(/\b(clickme|waiting)\b\s*/g, '');
    cl += ' passed';
    node.className = cl;
  });

  /** Aim high and you might miss the moon! */
  standardImports.expectFailure =
      frameGroup.tame(function (shouldFail, opt_msg, opt_failFilter) {
        try {
          shouldFail();
        } catch (e) {
          if (opt_failFilter && !opt_failFilter(e)) { throw e; }
          console.log('Caught expected failure ' + e + ' (' + e.message + ')');
          return;
        }
        fail(opt_msg || 'Expected failure');
      });

  standardImports.assertFailsSafe =
      frameGroup.tame(function (canFail, assertionsIfPasses) {
        try {
          canFail();
        } catch (e) {
          return;
        }
        assertionsIfPasses();
      });

  var jsunitFns = [
      'assert', 'assertContains', 'assertEquals', 'assertEvaluatesToFalse',
      'assertEvaluatesToTrue', 'assertFalse', 'assertHTMLEquals',
      'assertHashEquals', 'assertNotEquals', 'assertNotNull',
      'assertNotUndefined', 'assertNull', 'assertRoughlyEquals',
      'assertTrue', 'assertObjectEquals', 'assertUndefined', 'error',
      'fail', 'setUp', 'tearDown'];
  for (var i = jsunitFns.length; --i >= 0;) {
    var name = jsunitFns[i];
    if (standardImports.hasOwnProperty(name)) {
      throw new Error('already defined', name);
    }
    standardImports[name] = frameGroup.tame(window[name]);
  }

  return standardImports;
}
