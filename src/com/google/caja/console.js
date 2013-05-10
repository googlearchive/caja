// Copyright (C) 2009 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// .............................................................................

/**
 * @fileoverview console support for browsers lacking it
 * This module creates a provides a console similar to the Firebug
 * console on browsers which do not have one.  It appends console
 * messages to an element called 'console-results' if it exists, else
 * it creates and appends such an element to the document.
 *
 * @requires document, setTimeout
 * @overrides console
 */
if ('undefined' === typeof console) {
  var console = (function () {
    function getResultsNode() {
      var resultsNode = document.getElementById('console-results');
      if (!resultsNode) {
        resultsNode = document.createElement('DIV');
        resultsNode.id = 'console-results';
        var where = document.body || document.documentElement;
        where.appendChild(resultsNode);
      }
      return resultsNode;
    }

    function escapeAttrib(s) {
      return s.replace(/&/g, '&amp;').replace(/\"/g, '&quot;')
          .replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    var htmlBuf = null;
    function emitElement(elName, text, className) {
      if (typeof setTimeout !== 'undefined') {
        if (!htmlBuf) {
          htmlBuf = [];
          setTimeout(
              function () {
                var div = document.createElement('DIV');
                div.innerHTML = htmlBuf.join('');
                getResultsNode().appendChild(div);
                htmlBuf = null;
              },
              0);
        }
        htmlBuf.push(
            '<' + elName
            + (className ? ' class="' + escapeAttrib(className) + '">' : '>')
            + escapeAttrib(text)
            + '</' + elName + '>');
      } else {
        var el = document.createElement(elName);
        el.appendChild(document.createTextNode(text));
        if (className) { el.className = className; }
        getResultsNode().appendChild(el);
      }
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
