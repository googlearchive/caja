// Copyright (C) 2008 Google Inc.
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
 * Supporting scripts for the cajoling testbed.
 *
 * @author mikesamuel@gmail.com
 */

/**
 * Returns an instance of CajaApplet that exposes public methods as javascript
 * methods.
 * @see CajaApplet.java
 * @return {CajaApplet}
 */
function getCajoler() {
  return document.applets.cajoler;
}

/**
 * Reads caja code and configuration from the testbed form, cajoles it, and
 * displays the output in the current HTML page.
 */
var cajole = (function () {
  /**
   * Converts a plain text string to an HTML encoded string suitable for
   * inclusion in PCDATA or an HTML attribute value.
   * @param {string} s
   * @return {string}
   */
  function escapeHtml(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/\042/g, '&quot;');
  }

  /**
   * Extract cajoled ecmascript from DefaultGadgetRewriter's output.
   * This removes the envelope created by
   * DefaultGadgetRewriter.rewriteContent(String).
   */
  function loadCaja(htmlText) {
    var m = htmlText.match(
        /^\s*<script\b[^>]*>([\s\S]*)<\/script\b[^>]*>\s*$/i);
    if (m) {
      var script = m[1];

      testImports.clearHtml___();
      document.getElementById('caja-stacks').style.display = 'none';
      try {
        eval(script);
      } catch (ex) {
        var cajaStack = ex.cajaStack___
            && ___.unsealCallerStack(ex.cajaStack___);
        if (cajaStack) {
          document.getElementById('caja-stacks').style.display = '';
          document.getElementById('caja-stack').appendChild(
              document.createTextNode(cajaStack.join('\n')));
        }
        throw ex;
      }
    } else {
      (typeof console !== 'undefined')
      && console.warn('Failed to eval cajoled output %s', html);
    }
  }

  function cajole() {
    var inputs = document.forms.cajolerForm.elements;
    var result = getCajoler().cajole(
        inputs.src.value.replace(/^\s+|\s+$/g, ''),
        Boolean(inputs.embeddable.checked),
        Boolean(inputs.debugSymbols.checked));
    var cajoledOutput = result[0];
    var messages = String(result[1]);

    if (cajoledOutput !== null) {
      cajoledOutput = String(cajoledOutput);
      document.getElementById('output').innerHTML
          = prettyPrintOne(escapeHtml(cajoledOutput));

      loadCaja(cajoledOutput);
    } else {
      document.getElementById('output').innerHTML
          = '<center class="failure">Failed<\/center>';
    }
    document.getElementById('messages').innerHTML = messages || '';
  }

  return cajole;
})();

/**
 * Concatenates all text node leaves of the given DOM subtree to produce the
 * equivalent of IE's innerText attribute.
 */
var innerText = (function () {
  function innerText(node) {
    var s = [];
    innerTextHelper(node, s);
    return s.join();
  }

  function innerTextHelper(node, buf) {
    for (var child = node.firstChild; child; child = child.nextSibling) {
      if (child.nodeType === 3) {
        buf.push(child.nodeValue);
      } else {
        innerTextHelper(child, buf);
      }
    }
  }

  return innerText;
})();


/** The fake global scope for the testbed gadget. */
var testImports;
(function () {
  testImports = ___.copy(___.sharedImports);
  testImports.caja.result = ___.primFreeze(___.simpleFunc(function (o) {
    var type = document.createElement('span');
    type.className = 'type';
    type.appendChild(document.createTextNode(typeString(o)));

    var entry = document.createElement('div');
    entry.className = 'result';
    entry.appendChild(type);
    entry.appendChild(document.createTextNode(repr(o)));

    document.getElementById('eval-results').appendChild(entry);
  }));

  function typeString(o) {
    if (typeof o === 'object') {
      if (o === null) { return 'null'; }
      var ctor = ___.directConstructor(o);
      var name;
      if (ctor) {
        name = ctor.NAME___;
        if (!name && ('name' in ctor) && !___.hasOwnProp(ctor, 'name')) {
          name = ctor.name;
        }
        if (name) { return String(name); }
      }
    }
    return typeof o;
  }

  // Escape one character by javascript string literal rules
  function escapeOne(ch) {
    var i = ch.charCodeAt(0);
    if (i < 0x80) {
      switch (i) {
        case 9: return '\t';
        case 0x0a: return '\\n';
        case 0x0d: return '\\r';
        case 0x22: return '\\"';
        case 0x5c: return '\\\\';
        default: return (i < 0x10 ? '\\x0' : '\\x') + i.toString(16);
      }
    }
    var hex = i.toString(16);
    while (hex.length < 4) { hex = '0' + hex; }
    return '\\u' + hex;
  }

  // Builds part of the repr of a JSON map.
  function reprKeyValuePair(els) {
    return ___.simpleFunc(function (k, v) {
      els.push(repr(k) + ': ' + repr(v));
    });
  }

  // Like the python function, but produces a debugging string instead of
  // one that can be evaled.
  function repr(o) {
    switch (typeof o) {
      case 'string':
        return ('"'
                + o.replace(/[^\x20\x21\x23-\x5b\x5d-\x7f]/g, escapeOne)
                + '"');
      case 'object': case 'function':
        if (o === null) { break; }
        if (caja.isJSONContainer(o)) {
          var els = [];
          if ('length' in o
              && !(Object.prototype.propertyIsEnumerable.call(o, 'length'))
              ) {
            for (var i = 0; i < o.length; ++i) {
              els.push(repr(o[i]));
            }
            return '[' + els.join(', ') + ']';
          } else {
            caja.each(o, reprKeyValuePair(els));
            return els.length ? '{ ' + els.join(', ') + ' }' : '{}';
          }
        }
        return '\u00ab' + o + '\u00bb';
    }
    return String(o);
  }

  ___.getNewModuleHandler().setImports(testImports);
  attachDocumentStub(
       '-xyz___',
       {
         rewrite:
             function (uri, mimeType) {
               if (!/^https?:\/\//i.test(uri)) { return null; }
               return 'http://gadget-proxy/?url=' + encodeURIComponent(uri)
                   + '&mimeType=' + encodeURIComponent(mimeType);
             }
       },
       testImports);
  testImports.clearHtml___ = function () {
    document.getElementById('caja-html').innerHTML = (
        '<center style="color: gray">eval<\/center>');
    testImports.htmlEmitter___ = new HtmlEmitter(
        document.getElementById('caja-html'));
  };
  /**
   * Put styles inside a node that is cleared for each gadget so that
   * styles don't persist across invocations of cajole.
   */
  testImports.getCssContainer___ = function () {
    return document.getElementById('caja-html');
  };
})();
