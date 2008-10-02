// Copyright (C) 2007 Google Inc.
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
 * Stubs out JSUnit by providing a function that finds all the testcases defined
 * in the global scope, and run them.
 *
 * @author mikesamuel@gmail.com
 */

var jsunitTests = {};

/** Register a test that can be run later. */
function jsunitRegister(testName, test) {
  if (testName in jsunitTests) { throw new Error('dupe test ' + testName); }
  jsunitTests[testName] = test;
}

/** Run tests. */
function jsunitRun() {
  document.title += ' (' + (navigator.appName
                            ? navigator.appName + ' ' + navigator.appVersion
                            : navigator.userAgent) + ')';
  var originalTitle = document.title;

  var testNames = [];
  for (var k in jsunitTests) {
    if (jsunitTests.hasOwnProperty(k)) { testNames.push(k); }
  }
  testNames.sort();

  var firstFailure = null;
  var nFailures = 0;
  for (var i = 0; i < testNames.length; ++i) {
    var testName = testNames[i];
    var groupLogMessages = (typeof console !== 'undefined'
                            && 'group' in console);  // Not on Safari.
    if (groupLogMessages) {
      console.group('running %s', testName);
      console.time(testName);
    }
    try {
      (typeof setUp === 'function') && setUp();
      jsunitTests[testName].call(this);
      (typeof tearDown === 'function') && tearDown();
    } catch (e) {
      firstFailure = firstFailure || e;
      nFailures++;
      (typeof console !== 'undefined')
          && (console.error((e.message || '' + e) + '\n' + e.stack));
    } finally {
      if (groupLogMessages) {
        console.timeEnd(testName);
        console.groupEnd();
      }
    }
  }

  if (testNames.length && !nFailures) {
    document.title = originalTitle + ' - all tests passed';
  } else {
    var msg = nFailures + '/' + testNames.length + ' failed';
    document.title = (originalTitle + ' - ' + msg);
    throw firstFailure || new Error(msg);
  }
  (typeof console !== 'undefined' && 'group' in console)
      && (console.group(document.title), console.groupEnd());
}

if ('undefined' === typeof console) {
  var console = (function () {
    function getResultsNode() {
      var resultsNode = document.getElementById('console-results');
      if (!resultsNode) {
        resultsNode = document.createElement('DIV');
        resultsNode.id = 'console-results';
        document.body.appendChild(resultsNode);
      }
      return resultsNode;
    }

    function emitElement(elName, text, className) {
      var el = document.createElement(elName);
      el.appendChild(document.createTextNode(text));
      if (className) { el.className = className; }
      getResultsNode().appendChild(el);
    }

    function toMessage(args) {
      var msg = String(args[0]);
      if (args.length > 1) {
        var i = 0;
        msg = msg.replace(/%(?:%|([a-z%]))/g,
                          function (_, p) { return p ? args[++i] : '%'; });
      }
      return msg;
    }

    var timers = {};

    return {
      group: function () { emitElement('h2', toMessage(arguments)); },
      groupEnd: function () {},
      time: function (name) { timers[name] = (new Date).getTime(); },
      timeEnd: function (name) {
        var t0 = timers[name];
        delete timers[name];
        if (t0) {
          var dt = (new Date).getTime() - t0;
          this.log(name + ' : ' + dt + ' ms');
        }
      },
      log: function () { emitElement('div', toMessage(arguments), 'log'); },
      warn: function () { emitElement('div', toMessage(arguments), 'warn'); },
      error: function () { emitElement('div', toMessage(arguments), 'err'); },
      info: function () { emitElement('div', toMessage(arguments), 'info'); },
      trace: function () {}
    };
  })();
}
