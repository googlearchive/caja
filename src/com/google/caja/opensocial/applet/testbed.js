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
 * <p>
 * This supports input forms that can be instantiated multiple times to
 * simulate multiple gadgets in the same frame.
 * Different forms are distinguished by a unique suffix, and use the following
 * identifiers:
 * <dl>
 *   <dt><code>'cajolerForm' + uiSuffix</code></dt>
 *   <dd>The FORM containing the testbed source code</dd>
 *   <dt><code>'messages' + uiSuffix</code></dt>
 *   <dd>Messages from the Cajoler's MessageQueue with snippets.</dd>
 *   <dt><code>'output' + uiSuffix</code></dt>
 *   <dd>Container for cajoled output.</dd>
 *   <dt><code>'eval-results' + uiSuffix</code></dt>
 *   <dd>Container for result of last expression in source code.</dd>
 *   <dt><code>'cajita-stacks' + uiSuffix</code></dt>
 *   <dd>Parent of container for debug mode stack traces a la
 *     <tt>cajita-debugmode.js</tt>.</dd>
 *   <dt><code>'cajita-stack' + uiSuffix</code></dt>
 *   <dd>Container for debug mode stack traces.</dd>
 *   <dt><code>'caja-html' + uiSuffix</code></dt>
 *   <dd>Container for HTML rendered by cajoled code.</dd>
 * </dl>
 * All UI suffixes start with a '.' which is allowed in XML IDs and CLASSes.
 *
 * <p>
 * Gadgets can exports their public API by attaching methods and fields to
 * the <code>exports</code> object.  E.g. the 'module.1' module could do<pre>
 *   exports.doSomething = function () { ... };
 * </pre>
 * which makes the <code>doSomething</code> function available to other modules
 * via<pre>
 *   loadModule('gadget.1').doSomething();
 * </pre>
 *
 * @author mikesamuel@gmail.com
 */


/** UI suffixes of all registered testbeds. */
var testbeds = [];

/** URL to use when no proxy URL is provided */
var BOGUS_PROXY_URL = 'http://bogus-proxy.google.com';

/** A registry of the public APIs of each of the testbed applets. */
var gadgetPublicApis = {
  // Predefine a honeypot so we can try to exploit confused deputies
  'keystoneKop': ___.primFreeze({
        // Not marked simple.  It is a breach if a gadget can get the container
        // to call this on their behalf.
        f: function() {
          alert('You get a cookie ' + [].join.call(arguments, ', '));
        }
      })
};

if ('undefined' === typeof prettyPrintOne) {
  // So it works without prettyprinting when disconnected from the network.
  prettyPrintOne = function (html) { return html; };
  prettyPrint = function () {};
}

/**
 * Returns an instance of CajaApplet that exposes public methods as javascript
 * methods.
 * @see CajaApplet.java
 * @return {com.google.caja.opensocial.applet.CajaApplet}
 */
function getCajoler() {
  return document.applets.cajoler;
}


/** Get the protocol, host, and port of a <tt>bin/testbed-proxy.py</tt>. */
var getTestbedServer = (function () {
  /** Parses the URL to pick out CGI parameters. */
  function getCgiParams() {
    var parts = (location.search || '').split(/[\?&]/g);
    var params = {};
    for (var i = parts.length; --i >= 0;) {
      var part = parts[i];
      var eq = part.indexOf('=');
      var key, val;
      if (eq < 0) {
        key = decodeURIComponent(part);
        val = '';
      } else {
        key = decodeURIComponent(part.substring(0, eq));
        val = decodeURIComponent(part.substring(eq + 1));
      }
      (params[key] || (params[key] = [])).push(val);
    }
    return params;
  }

  var testbedServer;
  return function getTestbedServer() {
    if (testbedServer === void 0) {
      var backend = getCgiParams().backend;
      testbedServer = (backend && backend.length === 1)
          ? backend[0]
          : BOGUS_PROXY_URL;
    }
    return testbedServer;
  };
})();

/** Fills out the form with a "Conway's Game of Life" gadget. */
function lifecode(form) {
  form.elements.src.value =
      "<!-- Cellular automaton gadget. This code is in the public domain. -->\n" +
      "<script>var handle = null;</script>\n" +
      "<textarea id=\"area\">It's alive!</textarea>\n" +
      "<input type=\"button\" value=\"go\"\n" +
      "       onclick=\"if (handle === null) { handle = setInterval(update, 1000); }\">\n" +
      "<input type=\"button\" value=\"stop\"\n" +
      "       onclick=\"if (handle !== null) { clearInterval(handle); handle = null; }\">\n" +
      "<script>\n" +
      "// Size of the grid\nvar size = 40;\n" +
      "// Text area for displaying the CA\n" +
      "var ta = document.getElementById('area');\n" +
      "ta.setAttribute('rows', size);\n" +
      "ta.setAttribute('cols', size*2);\n\n" +
      "// Set up the internal version of the grid\n" +
      "// ca[t][x][y]\n" +
      "// t alternates between 0 and 1\n" +
      "var ca = [];\n" +
      "var t, x, y;\n" +
      "for (t = 0; t < 2; ++t) {\n" +
      "  ca[t] = [];\n" +
      "  for (x = 0; x < size; ++x) {\n" +
      "    ca[t][x] = new Array(size);\n" +
      "  }\n" +
      "}\n\n" +
      "// Start with a pi heptomino\n" +
      "var s = Math.floor(size / 2);\n" +
      "ca[0][s][s] = 1;\n" +
      "ca[0][s+1][s] = 1;\n" +
      "ca[0][s+2][s] = 1;\n" +
      "ca[0][s][s+1] = 1;\n" +
      "ca[0][s+2][s+1] = 1;\n" +
      "ca[0][s][s+2] = 1;\n" +
      "ca[0][s+2][s+2] = 1;\n\n\n" +
      "// Compute number of neighbors (on a torus)\n" +
      "function count(ca, t, x, y) {\n" +
      "  var sum = 0, i, j;\n" +
      "  for (i = -1; i < 2; ++i) {\n" +
      "    for (j = -1; j < 2; ++j) {\n" +
      "      if (!i && !j) continue;\n" +
      "\n" +
      "      var k = x + i;\n" +
      "      if (k < 0) { k += size; }\n" +
      "      else if (k >= size) { k -= size; }\n" +
      "\n" +
      "      var m = y + j;\n" +
      "      if (m < 0) { m += size; }\n" +
      "      else if (m >= size) { m -= size; }\n" +
      "\n" +
      "      sum += !!(ca[t][k][m]);\n" +
      "    }\n" +
      "  }\n" +
      "  return sum;\n" +
      "}\n\n" +
      "// Display loop\n" +
      "function display(ca, t) {\n" +
      "  var x, y, str = \"\";\n" +
      "  for (y = 0; y < size; ++y) {\n" +
      "    for (x = 0; x < size; ++x) {\n" +
      "      str += ca[t][x][y] ? \"()\" : \"  \";\n" +
      "    }\n" +
      "    str += \"\\n\";\n" +
      "  }\n" +
      "  ta.innerHTML = str;\n" +
      "}\n\n" +
      "// Update loop\n" +
      "function update() {\n" +
      "  for (x = 0; x < size; ++x) {\n" +
      "    for (y = 0; y < size; ++y) {\n" +
      "      var c = count(ca, t, x, y);\n" +
      "      // Conway's rules\n" +
      "      if (c === 2) { ca[1-t][x][y] = ca[t][x][y]; }\n" +
      "      else if (c === 3) { ca[1-t][x][y] = 1; }\n" +
      "      else { ca[1-t][x][y] = 0; }\n" +
      "    }\n" +
      "  }\n" +
      "  t = 1 - t;\n" +
      "  display(ca, t);\n" +
      "}\n" +
      "t=0;\n" +
      "</script>";
}

/**
 * Reads caja code and configuration from the testbed form, cajoles
 * it, runs it, and displays the output in the current HTML page.
 * @param {HTMLFormElement} form
 */
var cajole = (function () {
  /**
   * Extract cajoled ecmascript from DefaultGadgetRewriter's output.
   * This removes the envelope created by
   * DefaultGadgetRewriter.rewriteContent(String).
   * @param {string} htmlText from the cajoler
   * @param {string} uiSuffix suffix of testbed identifiers as described above.
   */
  function loadCaja(htmlText, uiSuffix) {
    var m = htmlText.match(
        /^\s*<script\b[^>]*>([\s\S]*)<\/script\b[^>]*>\s*$/i);
    if (m) {
      var script = m[1];
      var imports = getImports(uiSuffix);

      imports.clearHtml___();
      var stackTrace = document.getElementById('cajita-stacks' + uiSuffix)
      stackTrace.style.display = 'none';

      // Provide an object into which the module can export its public API.
      imports.exports = {};
      if (document.getElementById("VALIJA_MODE" + uiSuffix).checked) {
        imports.$v = valijaMaker(imports);
      }
      // Set up the outer new module handler
      ___.setNewModuleHandler(imports.outerNewModuleHandler___);

      // Load the script
      try {
        eval(script);
        gadgetPublicApis['gadget' + uiSuffix] = ___.primFreeze(imports.exports);
      } catch (ex) {
        var cajitaStack = ex.cajitaStack___
            && ___.unsealCallerStack(ex.cajitaStack___);
        if (cajitaStack) {
          stackTrace.style.display = '';
          document.getElementById('cajita-stack' + uiSuffix).appendChild(
              document.createTextNode(cajitaStack.join('\n')));
        }
        throw ex;
      }
    } else {
      (typeof console !== 'undefined')
      && console.warn('Failed to eval cajoled output %s', html);
    }
  }

  /** Log to a logging service running on localhost. See bin/testbed-proxy.py */
  function logToServer(msg) {
    var logForm = document.getElementById('logForm');
    if (!logForm) {
      var testbedServer = getTestbedServer();
      if (testbedServer === BOGUS_PROXY_URL) { return; }
      logForm = document.createElement('FORM');
      logForm.id = 'logForm';
      logForm.method = 'POST';
      logForm.action = testbedServer + '/log';
      var msgInput = document.createElement('INPUT');
      msgInput.type = 'hidden';
      msgInput.name = 'msg';
      msgInput.id = 'logFormMsg';
      logForm.target = 'logFrame';

      var logFrame = document.createElement('IFRAME');
      logFrame.name = logForm.target;
      logFrame.style.visibility = 'hidden';
      logFrame.width = logFrame.height = '1';
      document.body.appendChild(logFrame);
      document.body.appendChild(logForm);
      logForm.appendChild(msgInput);
    }
    document.getElementById('logFormMsg').value = msg;
    logForm.submit();
  }

  function cajole(form) {
    var uiSuffix = form.id.replace(/^[^\.]+/, '');

    var inputs = form.elements;
    var features = ['testbedServer=' + getTestbedServer().replace(/,/g, '%2C')];
    // See CajaApplet.Feature
    cajita.forOwnKeys(
        { EMBEDDABLE: true, DEBUG_SYMBOLS: true, VALIJA_MODE: true },
        ___.simpleFrozenFunc(function (featureName) {
          if (inputs[featureName + uiSuffix].checked) {
          features.push(featureName);
          }
        }));
    features = features.join(',');

    var src = inputs.src.value.replace(/^\s+|\s+$/g, '');

    logToServer('features:' + features + '\nsrc:' + src);

    var messages = '';
    document.getElementById('output' + uiSuffix).innerHTML = '';

    var result = eval(String(getCajoler().cajole(src, features)));
    var cajoledOutput = result[0];
    messages = String(result[1]);

    document.getElementById('messages' + uiSuffix).innerHTML = (
        messages || '<center><i>No Messages</i></center>');

    if (cajoledOutput !== null) {
      cajoledOutput = String(cajoledOutput);
      document.getElementById('output' + uiSuffix).innerHTML = prettyPrintOne(
          indentAndWrapCode(cajoledOutput));

      loadCaja(cajoledOutput, uiSuffix);
    }
  }

  return cajole;
})();

/**
 * Concatenates all text node leaves of the given DOM subtree to produce the
 * equivalent of IE's innerText attribute.
 * @param {Node} node
 * @return {string}
 */
var innerText = (function () {
  function innerText(node) {
    var s = [];
    innerTextHelper(node, s);
    return s.join('');
  }

  function innerTextHelper(node, buf) {
    for (var child = node.firstChild; child; child = child.nextSibling) {
      switch (child.nodeType) {
        case 3:
          buf.push(child.nodeValue); break;
        case 1:
          if ('BR' === child.nodeName) {
            buf.push('\n');
            break;
          }
          // fall through
        default:
          innerTextHelper(child, buf);
          break;
      }
    }
  }

  return innerText;
})();


/**
 * Gets the fake global scope for the testbed gadget with the given ui suffix.
 * @param {string} uiSuffix
 * @return {Object} imports object for the given uiSuffix.
 */
var getImports = (function () {
  var importsByUiSuffix = {};

  /**
   * Returns a string describing the type of the given object.
   * For a primitive, uses typeof, but for a constructed object tries to
   * determine the constructor name.
   */
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

  /** Escape one character by javascript string literal rules. */
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

  /** Builds part of the repr of a JSON map. */
  function reprKeyValuePair(els) {
    return ___.simpleFrozenFunc(function (k, v) {
      els.push(repr(k) + ': ' + repr(v));
    });
  }

  /**
   * Like the python function, but produces a debugging string instead of
   * one that can be evaled.
   */
  function repr(o) {
    if (Object.prototype.toSource && typeof o === 'object' && o !== null) {
      return Object.prototype.toSource.call(o);
    }
    try {
      switch (typeof o) {
        case 'string':
          return ('"'
                  + o.replace(/[^\x20\x21\x23-\x5b\x5d-\x7f]/g, escapeOne)
                  + '"');
        case 'object': case 'function':
          if (o === null) { break; }
          if (cajita.isJSONContainer(o)) {
            var els = [];
            if ('length' in o
                && !(Object.prototype.propertyIsEnumerable.call(o, 'length'))
                ) {
              for (var i = 0; i < o.length; ++i) {
                els.push(repr(o[i]));
              }
              return '[' + els.join(', ') + ']';
            } else {
              cajita.forOwnKeys(o, reprKeyValuePair(els));
              return els.length ? '{ ' + els.join(', ') + ' }' : '{}';
            }
          }
          return '\u00ab' + o + '\u00bb';
      }
      return String(o);
    } catch (e) {
      return "This object is recursive, so we're not going to try to print it.";
    }
  }

  function makeOuterNewModuleHandler(imports, uiSuffix) {
    var superHandler = ___.makeNormalNewModuleHandler();
    superHandler.setImports(imports);
    var inner = ___.beget(superHandler);
    inner.handle = ___.simpleFrozenFunc(function(newModule) {
      try {
	return ___.callPub(superHandler, 'handle', 
			   [___.simpleFrozenFunc(newModule)]);
      } finally {
	var outcome = superHandler.getLastOutcome();
	var type = document.createElement('span');
	type.className = 'type';
	type.appendChild(document.createTextNode(typeString(outcome[1])));

	var entry = document.createElement('div');
	entry.className = 'result';
	entry.appendChild(type);
	entry.appendChild(document.createTextNode(repr(outcome[1])));
	if (!outcome[0]) {
	  // TODO(erights): color something red
	}
	document.getElementById('eval-results' +
				uiSuffix).appendChild(entry);
      }
    });
    ___.freeze(inner);
    var outer = ___.beget(superHandler);
    outer.handle = ___.simpleFrozenFunc(function(newModule) {
      ___.setNewModuleHandler(inner);
      return ___.callPub(superHandler, 'handle', 
			 [___.simpleFrozenFunc(newModule)]);
    });
    return ___.freeze(outer);
  }

  function getImports(uiSuffix) {
    if (uiSuffix in importsByUiSuffix) {
      return importsByUiSuffix[uiSuffix];
    }

    var testImports = ___.copy(___.sharedImports);    
    var idClass = 'xyz' + ___.getId(testImports) + '___';
    attachDocumentStub(
         '-' + idClass,
         {
           rewrite:
               function (uri, mimeType) {
                 if (!/^https?:\/\//i.test(uri)) { return null; }
                 var testbedServer = getTestbedServer();
                 return (testbedServer + '/proxy?url='
                         + encodeURIComponent(uri)
                         + '&mimeType=' + encodeURIComponent(mimeType));
               }
         },
         testImports);
    testImports.clearHtml___ = function () {
      var htmlContainer = document.getElementById('caja-html' + uiSuffix);
      htmlContainer.className = idClass;
      htmlContainer.innerHTML = '';
      testImports.htmlEmitter___ = new HtmlEmitter(htmlContainer);      
    };
    /**
     * Put styles inside a node that is cleared for each gadget so that
     * styles don't persist across invocations of cajole.
     */
    testImports.getCssContainer___ = function () {
      return document.getElementById('caja-html' + uiSuffix);
    };
    /** Provide a way to load another gadget's public API. */
    testImports.loadModule = ___.simpleFrozenFunc(
        function (moduleName) {
          moduleName = String(moduleName);
          return ___.canEnumPub(gadgetPublicApis, moduleName)
              ? gadgetPublicApis[moduleName]
              : void 0;
        });

    testImports.outerNewModuleHandler___ = 
      makeOuterNewModuleHandler(testImports, uiSuffix);

    return importsByUiSuffix[uiSuffix] = testImports;
  }

  return getImports;
})();

/**
 * Copies the given DOM node and rewrites IDs to be unique as a poor man's
 * Maps templates.
 * @param {Node} domTree
 * @param {string} domSuffix
 * @return {Node}
 */
function renderTemplate(domTree, domSuffix) {
  function suffixAttrib(node, attribName) {
    // IE is flaky around hasAttribute and setAttribute.  Using the attributes
    // NodeList directly works reliably.
    if (node.attributes[attribName] && node.attributes[attribName].value) {
      node.attributes[attribName].value += domSuffix;
    }
  }

  function fixNamesAndIds(node, inForm) {
    if (node.nodeType === 1) {
      suffixAttrib(node, 'id');
      suffixAttrib(node, 'for');
      if (!inForm) {
        suffixAttrib(node, 'name');
        inForm = 'FORM' === node.nodeName;
      }
    }
    for (var child = node.firstChild; child; child = child.nextSibling) {
      fixNamesAndIds(child, inForm);
    }
  }
  domTree = domTree.cloneNode(true);
  fixNamesAndIds(domTree, false);
  return domTree;
}

/**
 * Add to the list of registered testbeds.
 * @param {string} uiSuffix
 */
function registerTestbed(uiSuffix) {
  testbeds.push(uiSuffix);
}

function initTestbeds() {
  for (var i = 0; i < testbeds.length; ++i) {
    getImports(testbeds[i]).clearHtml___();
  }
}

function loadExampleInto(containerNode, form) {
  form.elements.src.value = innerText(containerNode);
}

/**
 * Given generated source code, identify indented blocks, and wrap them
 * in divs with left margins so that code wraps nicely, but maintains
 * indentation for subsequent lines.
 * @param {string} code a plain text string.
 * @return {string} html.
 */
function indentAndWrapCode(code) {
  /**
   * Converts a plain text string to an HTML encoded string suitable for
   * inclusion in PCDATA or an HTML attribute value.
   * @param {string} s
   * @return {string} html
   */
  function escapeHtml(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/\042/g, '&quot;')
        .replace(/ /g, '\xA0');
  }

  /** Accumulates chunks of HTML. */
  var htmlOut = [];
  /**
   * Stack of number of spaces before open blocks in reverse order.
   * The current block, which has the highest indent level, is always in
   * position 0.
   */
  var indentStack = [0];

  /** Append chunks of html to htmlOut for a single line of code. */
  function processLine(line) {
    var len = line.length;
    var pos = 0;  // Length of the prefix of line processed so far.

    // Count the number of spaces at the beginning so we can construct nested
    // <blockquote> chunks around indentation changes.
    while (pos < len && line.charAt(pos) == ' ') { ++pos; }
    if (pos === len) {
      htmlOut.push('<br>');
      return;
    }
    if (pos !== indentStack[0]) {
      if (pos < indentStack[0]) {
        do {
          indentStack.shift();
          htmlOut.push('</div>');
        } while (pos < indentStack[0]);
      } else if (pos > indentStack[0]) {
        indentStack.unshift(pos);
        htmlOut.push('<div class="indentedblock">');
      }
    }

    // Walk over the code and introduce <wbr>s at commas and brackets
    htmlOut.push('<div class="line-of-code">');
    var strDelim = null;
    for (var i = pos; i < len; ++i) {
      var ch = line.charAt(i);
      switch (ch) {
      case '"': case "'":
        if (strDelim === null) {
          strDelim = ch;
        } else if (strDelim === ch) {
          strDelim = null;
        }
        break;
      case '\\':
        if (strDelim !== null) { ++i; }
        break;
      // Since we replace spaces with non-breaking spaces, explicitly insert
      // <WBR>s around puncutation to allow breaking outside literals.
      case ',': case '(': case ')': case '{': case '}': case '[': case ']':
        if (strDelim === null) {
          htmlOut.push(escapeHtml(line.substring(pos, i + 1)), '<wbr>');
          pos = i + 1;
        }
        break;
      }
    }
    htmlOut.push(escapeHtml(line.substring(pos)), '</div>');
  }

  var lines = code.split(/\r\n?|\n/g);
  for (var i = 0, n = lines.length; i < n; ++i) { processLine(lines[i]); }
  for (var i = indentStack.length; --i >= 1;) { htmlOut.push('</div>'); }
  return htmlOut.join('');
}
