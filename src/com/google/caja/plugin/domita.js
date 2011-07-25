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
 * A partially tamed browser object model based on
 * <a href="http://www.w3.org/TR/DOM-Level-2-HTML/Overview.html"
 * >DOM-Level-2-HTML</a> and specifically, the
 * <a href="http://www.w3.org/TR/DOM-Level-2-HTML/ecma-script-binding.html"
 * >ECMAScript Language Bindings</a>.
 *
 * Caveats:<ul>
 * <li>This is not a full implementation.
 * <li>Security Review is pending.
 * <li><code>===</code> and <code>!==</code> on node lists will not
 *   behave the same as with untamed node lists.  Specifically, it is
 *   not always true that {@code nodeA.childNodes === nodeA.childNodes}.
 * <li>Properties backed by setters/getters like {@code HTMLElement.innerHTML}
 *   will not appear to uncajoled code as DOM nodes do, since they are
 *   implemented using cajita property handlers.
 * </ul>
 *
 * <p>
 * TODO(ihab.awad): Our implementation of getAttribute (and friends)
 * is such that standard DOM attributes which we disallow for security
 * reasons (like 'form:enctype') are placed in the "virtual"
 * attributes map (this.node___.attributes___). They appear to be
 * settable and gettable, but their values are ignored and do not have
 * the expected semantics per the DOM API. This is because we do not
 * have a column in html4-defs.js stating that an attribute is valid
 * but explicitly blacklisted. Alternatives would be to always throw
 * upon access to these attributes; to make them always appear to be
 * null; etc. Revisit this decision if needed.
 *
 * <p>
 * TODO(ihab.awad): Come up with a uniform convention (and helper functions,
 * etc.) for checking that a user-supplied callback is a valid Cajita function
 * or Valija Disfunction.
 *
 * @author mikesamuel@gmail.com
 * @requires console
 * @requires clearInterval, clearTimeout, setInterval, setTimeout, cssparser
 * @requires ___, bridal, bridalMaker, css, html, html4, unicode
 * @provides attachDocumentStub, plugin_dispatchEvent___,
 *     plugin_dispatchToHandler___
 * @overrides domitaModules
 */

var domitaModules;
if (!domitaModules) { domitaModules = {}; }

domitaModules.classUtils = function() {
  function getterSetterSuffix(name) {
    return String.fromCharCode(name.charCodeAt(0) & ~32)
        + name.substring(1) + '___';
  }

  /**
   * Add setter and getter hooks so that the caja {@code node.innerHTML = '...'}
   * works as expected.
   */
  function exportFields(object, fields) {
    for (var i = fields.length; --i >= 0;) {
      var field = fields[i];
      var suffix = getterSetterSuffix(field);
      var getterName = 'get' + suffix;
      var setterName = 'set' + suffix;
      var count = 0;
      if (object[getterName]) {
        ++count;
        ___.useGetHandler(
           object, field, object[getterName]);
      }
      if (object[setterName]) {
        ++count;
        ___.useSetHandler(
           object, field, object[setterName]);
      }
      if (!count) {
        throw new Error('Failed to export field ' + field + ' on ' + object);
      }
    }
  }

  /**
   * Apply a supplied list of getter and setter functions to a given object.
   *
   * @param object an object to be decorated with getters and setters
   * implementing some properties.
   *
   * @param handlers an object containing the handler functions in the form:
   *
   *     {
   *       <propName> : { get: <getHandlerFcn>, set: <setHandlerFcn> },
   *       <propName> : { get: <getHandlerFcn>, set: <setHandlerFcn> },
   *       ...
   *     }
   *
   * For each <propName> entry, the "get" field is required, but the "set"
   * field may be empty; this implies that <propName> is a read-only property.
   */
  function applyAccessors(object, handlers) {
    function propertyOnlyHasGetter(_) {
      throw new TypeError('setting a property that only has a getter');
    }

    ___.forOwnKeys(handlers,
                   ___.markFuncFreeze(function (propertyName, def) {
      var setter = def.set || propertyOnlyHasGetter;
      ___.useGetHandler(object, propertyName, def.get);
      ___.useSetHandler(object, propertyName, setter);
    }));
  }

  function hasGetHandler(object, propertyName) {
    return !!object[propertyName + '_g___'];
  }

  function hasSetHandler(object, propertyName) {
    return !!object[propertyName + '_s___'];
  }

  /**
   * Checks that a user-supplied callback is either a Cajita function or a
   * Valija Disfuction. Return silently if the callback is valid; throw an
   * exception if it is not valid.
   *
   * @param aCallback some user-supplied "function-like" callback.
   */
  function ensureValidCallback(aCallback) {
    if (typeof aCallback === 'function' && aCallback.i___) { return; }
    throw new Error('Expected valid guest function not ' + typeof aCallback);
  }

  return {
    exportFields: exportFields,
    ensureValidCallback: ensureValidCallback,
    applyAccessors: applyAccessors,
    getterSetterSuffix: getterSetterSuffix,
    hasGetHandler: hasGetHandler,
    hasSetHandler: hasSetHandler
  };
};

/** XMLHttpRequest or an equivalent on IE 6. */
domitaModules.XMLHttpRequestCtor = function (XMLHttpRequest, ActiveXObject) {
  if (XMLHttpRequest) {
    return XMLHttpRequest;
  } else if (ActiveXObject) {
    // The first time the ctor is called, find an ActiveX class supported by
    // this version of IE.
    var activeXClassId;
    return function ActiveXObjectForIE() {
      if (activeXClassId === void 0) {
        activeXClassId = null;
        /** Candidate Active X types. */
        var activeXClassIds = [
            'MSXML2.XMLHTTP.5.0', 'MSXML2.XMLHTTP.4.0',
            'MSXML2.XMLHTTP.3.0', 'MSXML2.XMLHTTP',
            'MICROSOFT.XMLHTTP.1.0', 'MICROSOFT.XMLHTTP.1',
            'MICROSOFT.XMLHTTP'];
        for (var i = 0, n = activeXClassIds.length; i < n; i++) {
          var candidate = activeXClassIds[i];
          try {
            void new ActiveXObject(candidate);
            activeXClassId = candidate;
            break;
          } catch (e) {
            // do nothing; try next choice
          }
        }
        activeXClassIds = null;
      }
      return new ActiveXObject(activeXClassId);
    };
  } else {
    throw new Error('ActiveXObject not available');
  }
};

domitaModules.TameXMLHttpRequest = function(
    xmlHttpRequestMaker,
    uriCallback) {
  var classUtils = domitaModules.classUtils();

  // See http://www.w3.org/TR/XMLHttpRequest/

  // TODO(ihab.awad): Improve implementation (interleaving, memory leaks)
  // per http://www.ilinsky.com/articles/XMLHttpRequest/

  function TameXMLHttpRequest() {
    this.xhr___ = this.FERAL_TWIN___ = new xmlHttpRequestMaker();
    this.FERAL_TWIN___.TAMED_TWIN___ = this;
    classUtils.exportFields(
        this,
        ['onreadystatechange', 'readyState', 'responseText', 'responseXML',
         'status', 'statusText']);
  }
  var FROZEN = "Object is frozen.";
  var INVALID_SUFFIX = "Property names may not end in '__'.";
  var endsWith__ = /__$/;
  TameXMLHttpRequest.prototype.handleRead___ = function (name) {
    name = '' + name;
    if (endsWith__.test(name)) { return void 0; }
    var handlerName = name + '_getter___';
    if (this[handlerName]) {
      return this[handlerName]();
    }
    if (___.hasOwnProp(this.xhr___.properties___, name)) {
      return this.xhr___.properties___[name];
    } else {
      return void 0;
    }
  };
  TameXMLHttpRequest.prototype.handleCall___ = function (name, args) {
    name = '' + name;
    if (endsWith__.test(name)) { throw new Error(INVALID_SUFFIX); }
    var handlerName = name + '_handler___';
    if (this[handlerName]) {
      return this[handlerName].call(this, args);
    }
    if (___.hasOwnProp(this.xhr___.properties___, name)) {
      return this.xhr___.properties___[name].call(this, args);
    } else {
      throw new TypeError(name + ' is not a function.');
    }
  };
  TameXMLHttpRequest.prototype.handleSet___ = function (name, val) {
    name = '' + name;
    if (endsWith__.test(name)) { throw new Error(INVALID_SUFFIX); }
    if (Object.isFrozen(this)) { throw new Error(FROZEN); }
    var handlerName = name + '_setter___';
    if (this[handlerName]) {
      return this[handlerName](val);
    }
    if (!this.xhr___.properties___) {
      this.xhr___.properties___ = {};
    }
    this[name + '_c___'] = this;
    this[name + '_v___'] = false;
    this[name + '_e___'] = this;
    return this.xhr___.properties___[name] = val;
  };
  TameXMLHttpRequest.prototype.handleDelete___ = function (name) {
    name = '' + name;
    if (endsWith__.test(name)) { throw new Error(INVALID_SUFFIX); }
    if (Object.isFrozen(this)) { throw new Error(FROZEN); }
    var handlerName = name + '_deleter___';
    if (this[handlerName]) {
      return this[handlerName]();
    }
    if (this.xhr___.properties___) {
      return (
          delete this.xhr___.properties___[name]
          && delete this[name + '_c___']
          && delete this[name + '_e___']
          && delete this[name + '_v___']);
    } else {
      return true;
    }
  };
  TameXMLHttpRequest.prototype.setOnreadystatechange___ = function (handler) {
    // TODO(ihab.awad): Do we need more attributes of the event than 'target'?
    // May need to implement full "tame event" wrapper similar to DOM events.
    var self = this;
    this.xhr___.onreadystatechange = function(event) {
      var evt = {};
      evt.DefineOwnProperty___('target', {
        value: self,
        writable: true,
        enumerable: true,
        configurable: true
      });
      return ___.callPub(handler, 'call', [void 0, evt]);
    };
    // Store for later direct invocation if need be
    this.handler___ = handler;
  };
  TameXMLHttpRequest.prototype.getReadyState___ = function () {
    // The ready state should be a number
    return Number(this.xhr___.readyState);
  };
  TameXMLHttpRequest.prototype.open = function (
      method, URL, opt_async, opt_userName, opt_password) {
    method = String(method);
    // The XHR interface does not tell us the MIME type in advance, so we
    // must assume the broadest possible.
    var safeUri = uriCallback.rewrite(
        String(URL), html4.ueffects.SAME_DOCUMENT, html4.ltypes.SANDBOXED);
    // If the uriCallback rejects the URL, we throw an exception, but we do not
    // put the URI in the exception so as not to put the caller at risk of some
    // code in its stack sniffing the URI.
    if ("string" !== typeof safeUri) { throw 'URI violates security policy'; }
    switch (arguments.length) {
    case 2:
      this.async___ = true;
      this.xhr___.open(method, safeUri);
      break;
    case 3:
      this.async___ = opt_async;
      this.xhr___.open(method, safeUri, Boolean(opt_async));
      break;
    case 4:
      this.async___ = opt_async;
      this.xhr___.open(
          method, safeUri, Boolean(opt_async), String(opt_userName));
      break;
    case 5:
      this.async___ = opt_async;
      this.xhr___.open(
          method, safeUri, Boolean(opt_async), String(opt_userName),
          String(opt_password));
      break;
    default:
      throw 'XMLHttpRequest cannot accept ' + arguments.length + ' arguments';
      break;
    }
  };
  TameXMLHttpRequest.prototype.setRequestHeader = function (label, value) {
    this.xhr___.setRequestHeader(String(label), String(value));
  };
  TameXMLHttpRequest.prototype.send = function(opt_data) {
    if (arguments.length === 0) {
      // TODO(ihab.awad): send()-ing an empty string because send() with no
      // args does not work on FF3, others?
      this.xhr___.send('');
    } else if (typeof opt_data === 'string') {
      this.xhr___.send(opt_data);
    } else /* if XML document */ {
      // TODO(ihab.awad): Expect tamed XML document; unwrap and send
      this.xhr___.send('');
    }

    // Firefox does not call the 'onreadystatechange' handler in
    // the case of a synchronous XHR. We simulate this behavior by
    // calling the handler explicitly.
    if (this.xhr___.overrideMimeType) {
      // This is Firefox
      if (!this.async___ && this.handler___) {
        var evt = { target: this };
        ___.callPub(this.handler___, 'call', [void 0, evt]);
      }
    }
  };
  TameXMLHttpRequest.prototype.abort = function () {
    this.xhr___.abort();
  };
  TameXMLHttpRequest.prototype.getAllResponseHeaders = function () {
    var result = this.xhr___.getAllResponseHeaders();
    return (result === undefined || result === null) ?
      result : String(result);
  };
  TameXMLHttpRequest.prototype.getResponseHeader = function (headerName) {
    var result = this.xhr___.getResponseHeader(String(headerName));
    return (result === undefined || result === null) ?
      result : String(result);
  };
  TameXMLHttpRequest.prototype.getResponseText___ = function () {
    var result = this.xhr___.responseText;
    return (result === undefined || result === null) ?
      result : String(result);
  };
  TameXMLHttpRequest.prototype.getResponseXML___ = function () {
    // TODO(ihab.awad): Implement a taming layer for XML. Requires generalizing
    // the HTML node hierarchy as well so we have a unified implementation.
    return {};
  };
  TameXMLHttpRequest.prototype.getStatus___ = function () {
    var result = this.xhr___.status;
    return (result === undefined || result === null) ?
      result : Number(result);
  };
  TameXMLHttpRequest.prototype.getStatusText___ = function () {
    var result = this.xhr___.statusText;
    return (result === undefined || result === null) ?
      result : String(result);
  };
  TameXMLHttpRequest.prototype.toString = ___.markFuncFreeze(function () {
    return 'Not a real XMLHttpRequest';
  });
  ___.markCtor(TameXMLHttpRequest, Object, 'TameXMLHttpRequest');
  ___.all2(___.grantTypedMethod, TameXMLHttpRequest.prototype,
           ['open', 'setRequestHeader', 'send', 'abort',
            'getAllResponseHeaders', 'getResponseHeader']);

  return TameXMLHttpRequest;
};

domitaModules.CssPropertiesCollection =
    function(cssPropertyNameCollection, anElement, css) {
  var canonicalStylePropertyNames = {};
  // Maps style property names, e.g. cssFloat, to property names, e.g. float.
  var cssPropertyNames = {};

  ___.forOwnKeys(cssPropertyNameCollection,
                 ___.markFuncFreeze(function (cssPropertyName) {
    var baseStylePropertyName = cssPropertyName.replace(
        /-([a-z])/g, function (_, letter) { return letter.toUpperCase(); });
    var canonStylePropertyName = baseStylePropertyName;
    cssPropertyNames[baseStylePropertyName]
        = cssPropertyNames[canonStylePropertyName]
        = cssPropertyName;
    if (css.alternates.hasOwnProperty(canonStylePropertyName)) {
      var alts = css.alternates[canonStylePropertyName];
      for (var i = alts.length; --i >= 0;) {
        cssPropertyNames[alts[i]] = cssPropertyName;
        // Handle oddities like cssFloat/styleFloat.
        if (alts[i] in anElement.style
            && !(canonStylePropertyName in anElement.style)) {
          canonStylePropertyName = alts[i];
        }
      }
    }
    canonicalStylePropertyNames[cssPropertyName] = canonStylePropertyName;
  }));

  return {
    isCanonicalProp: function (p) {
      return cssPropertyNames.hasOwnProperty(p);
    },
    isCssProp: function (p) {
      return canonicalStylePropertyNames.hasOwnProperty(p);
    },
    getCanonicalPropFromCss: function (p) {
      return canonicalStylePropertyNames[p];
    },
    getCssPropFromCanonical: function(p) {
      return cssPropertyNames[p];
    }
  };
};

/**
 * Add a tamed document implementation to a Gadget's global scope.
 *
 * Has the side effect of adding the classes "vdoc-body___" and
 * idSuffix.substring(1) to the pseudoBodyNode.
 *
 * @param {string} idSuffix a string suffix appended to all node IDs.
 *     It should begin with "-" and end with "___".
 * @param {Object} uriCallback an object like <pre>{
 *       rewrite: function (uri, uriEffect, loaderType, hints) { return safeUri }
 *     }</pre>.
 *       * uri: the uri to be rewritten
 *       * uriEffect: the effect that allowing a URI to load has (@see UriEffect.java).
 *       * loaderType: type of loader that would load the URI or the rewritten version.
 *       * hints: record that describes the context in which the URI appears.  If a hint is not 
 *         present it should not be relied upon.
 *     The rewrite function should be idempotent to allow rewritten HTML
 *     to be reinjected.
 * @param {Object} imports the gadget's global scope.
 * @param {Node} pseudoBodyNode an HTML node to act as the "body" of the
 *     virtual document provided to Cajoled code.
 * @param {Object} optPseudoWindowLocation a record containing the
 *     properties of the browser "window.location" object, which will
 *     be provided to the Cajoled code.
 */
var attachDocumentStub = (function () {
  // Array Remove - By John Resig (MIT Licensed)
  function arrayRemove(array, from, to) {
    var rest = array.slice((to || from) + 1 || array.length);
    array.length = from < 0 ? array.length + from : from;
    return array.push.apply(array, rest);
  }

  var TameNodeMark = ___.Trademark('TameNode');
  var TameNodeT = TameNodeMark.guard;
  var TameEventMark = ___.Trademark('TameEvent');
  var TameEventT = TameEventMark.guard;
  var TameImageDataMark = ___.Trademark('TameImageData');
  var TameImageDataT = TameImageDataMark.guard;
  var TameGradientMark = ___.Trademark('TameGradient');
  var TameGradientT = TameGradientMark.guard;

  // Define a wrapper type for known safe HTML, and a trademarker.
  // This does not actually use the trademarking functions since trademarks
  // cannot be applied to strings.
  function Html(htmlFragment) { this.html___ = String(htmlFragment || ''); }
  Html.prototype.valueOf = Html.prototype.toString =
      ___.markFuncFreeze(function () { return this.html___; });
  function safeHtml(htmlFragment) {
    return (htmlFragment instanceof Html)
        ? htmlFragment.html___
        : html.escapeAttrib(String(htmlFragment || ''));
  }
  function blessHtml(htmlFragment) {
    return (htmlFragment instanceof Html)
        ? htmlFragment
        : new Html(htmlFragment);
  }

  var XML_SPACE = '\t\n\r ';

  var JS_SPACE = '\t\n\r ';
  // An identifier that does not end with __.
  var JS_IDENT = '(?:[a-zA-Z_][a-zA-Z0-9$_]*[a-zA-Z0-9$]|[a-zA-Z])_?';
  var SIMPLE_HANDLER_PATTERN = new RegExp(
      '^[' + JS_SPACE + ']*'
      + '(return[' + JS_SPACE + ']+)?'  // Group 1 is present if it returns.
      + '(' + JS_IDENT + ')[' + JS_SPACE + ']*'  // Group 2 is a function name.
      // Which can be passed optionally this node, and optionally the event.
      + '\\((?:this'
        + '(?:[' + JS_SPACE + ']*,[' + JS_SPACE + ']*event)?'
        + '[' + JS_SPACE + ']*)?\\)'
      // And it can end with a semicolon.
      + '[' + JS_SPACE + ']*(?:;?[' + JS_SPACE + ']*)$');

  // These id patterns match the ones in HtmlAttributeRewriter.

  var VALID_ID_CHAR =
      unicode.LETTER + unicode.DIGIT + '_'
      + '$\\-.:;=()\\[\\]'
      + unicode.COMBINING_CHAR + unicode.EXTENDER;

  var VALID_ID_PATTERN = new RegExp(
      '^[' + VALID_ID_CHAR + ']+$');

  var VALID_ID_LIST_PATTERN = new RegExp(
      '^[' + XML_SPACE + VALID_ID_CHAR + ']*$');

  var FORBIDDEN_ID_PATTERN = new RegExp('__\\s*$');

  var FORBIDDEN_ID_LIST_PATTERN = new RegExp('__(?:\\s|$)');

  function isValidId(s) {
    return !FORBIDDEN_ID_PATTERN.test(s)
        && VALID_ID_PATTERN.test(s);
  }

  function isValidIdList(s) {
    return !FORBIDDEN_ID_LIST_PATTERN.test(s)
        && VALID_ID_LIST_PATTERN.test(s);
  }

  // Trim whitespace from the beginning and end of a CSS string.

  function trimCssSpaces(input) {
    return input.replace(/^[ \t\r\n\f]+|[ \t\r\n\f]+$/g, '');
  }

  /**
   * The plain text equivalent of a CSS string body.
   * @param {string} s the body of a CSS string literal w/o quotes
   *     or CSS identifier.
   * @return {string} plain text.
   * {@updoc
   * $ decodeCssString('')
   * # ''
   * $ decodeCssString('foo')
   * # 'foo'
   * $ decodeCssString('foo\\\nbar\\\r\nbaz\\\rboo\\\ffar')
   * # 'foobarbazboofar'
   * $ decodeCssString('foo\\000a bar\\000Abaz')
   * # 'foo' + '\n' + 'bar' + '\u0ABA' + 'z'
   * $ decodeCssString('foo\\\\bar\\\'baz')
   * # "foo\\bar'baz"
   * }
   */
  function decodeCssString(s) {
    // Decode a CSS String literal.
    // From http://www.w3.org/TR/CSS21/grammar.html
    //     string1    \"([^\n\r\f\\"]|\\{nl}|{escape})*\"
    //     unicode    \\{h}{1,6}(\r\n|[ \t\r\n\f])?
    //     escape     {unicode}|\\[^\r\n\f0-9a-f]
    //     s          [ \t\r\n\f]+
    //     nl         \n|\r\n|\r|\f
    return s.replace(
        /\\(?:(\r\n?|\n|\f)|([0-9a-f]{1,6})(?:\r\n?|[ \t\n\f])?|(.))/gi,
        function (_, nl, hex, esc) {
          return esc || (nl ? '' : String.fromCharCode(parseInt(hex, 16)));
        });
  }

  /**
   * Sanitize the 'style' attribute value of an HTML element.
   *
   * @param styleAttrValue the value of a 'style' attribute, which we
   * assume has already been checked by the caller to be a plain String.
   *
   * @return a sanitized version of the attribute value.
   */
  function sanitizeStyleAttrValue(styleAttrValue) {
    var sanitizedDeclarations = [];
    cssparser.parse(
        String(styleAttrValue),
        function (property, value) {
          property = property.toLowerCase();
          if (css.properties.hasOwnProperty(property)
              && css.properties[property].test(value + '')) {
            sanitizedDeclarations.push(property + ': ' + value);
          }
        });
    return sanitizedDeclarations.join(' ; ');
  }

  function mimeTypeForAttr(tagName, attribName) {
    if (attribName === 'src') {
      if (tagName === 'img') { return 'image/*'; }
      if (tagName === 'script') { return 'text/javascript'; }
    }
    return '*/*';
  }

  // TODO(ihab.awad): Does this work on IE, where console output
  // goes to a DOM node?
  function assert(cond) {
    if (!cond) {
      if (typeof console !== 'undefined') {
        console.error('domita assertion failed');
        console.trace();
      }
      throw new Error("Domita assertion failed");
    }
  }

  var classUtils = domitaModules.classUtils();

  var cssSealerUnsealerPair = ___.makeSealerUnsealerPair();

  // Implementations of setTimeout, setInterval, clearTimeout, and
  // clearInterval that only allow simple functions as timeouts and
  // that treat timeout ids as capabilities.
  // This is safe even if accessed across frame since the same
  // trademark value is never used with more than one version of
  // setTimeout.
  var TimeoutIdMark = ___.Trademark('TimeoutId');
  var TimeoutIdT = TimeoutIdMark.guard;

  function tameSetTimeout(timeout, delayMillis) {
    // Existing browsers treat a timeout of null or undefined as a noop.
    var timeoutId;
    if (timeout) {
      if (typeof timeout === 'string') {
        throw new Error(
            'setTimeout called with a string.'
            + '  Please pass a function instead of a string of javascript');
      }
      timeoutId = setTimeout(
          function () { ___.callPub(timeout, 'call', [___.USELESS]); },
          delayMillis | 0);
    } else {
      // tameClearTimeout checks for NaN and handles it specially.
      timeoutId = NaN;
    }
    return ___.stamp([TimeoutIdMark.stamp],
                     { timeoutId___: timeoutId });
  }
  ___.markFuncFreeze(tameSetTimeout);
  function tameClearTimeout(timeoutId) {
    if (timeoutId === null || timeoutId === (void 0)) { return; }
    try {
      timeoutId = TimeoutIdT.coerce(timeoutId);
    } catch (e) {
      // From https://developer.mozilla.org/en/DOM/window.clearTimeout says:
      // Notes:
      // Passing an invalid ID to clearTimeout does not have any effect
      // (and doesn't throw an exception).
      return;
    }
    var rawTimeoutId = timeoutId.timeoutId___;
    // Skip NaN values created for null timeouts above.
    if (rawTimeoutId === rawTimeoutId) { clearTimeout(rawTimeoutId); }
  }
  ___.markFuncFreeze(tameClearTimeout);

  var IntervalIdMark = ___.Trademark('IntervalId');
  var IntervalIdT = IntervalIdMark.guard;

  function tameSetInterval(interval, delayMillis) {
    // Existing browsers treat an interval of null or undefined as a noop.
    var intervalId;
    if (interval) {
      if (typeof interval === 'string') {
        throw new Error(
            'setInterval called with a string.'
            + '  Please pass a function instead of a string of javascript');
      }
      intervalId = setInterval(
          function () { ___.callPub(interval, 'call', [___.USELESS]); },
          delayMillis | 0);
    } else {
      intervalId = NaN;
    }
    return ___.stamp([IntervalIdMark.stamp],
                     { intervalId___: intervalId });
  }
  ___.markFuncFreeze(tameSetInterval);
  function tameClearInterval(intervalId) {
    if (intervalId === null || intervalId === (void 0)) { return; }
    try {
      intervalId = IntervalIdT.coerce(intervalId);
    } catch (e) {
      // See comment about corresponding error handling in clearTimeout.
      return;
    }
    var rawIntervalId = intervalId.intervalId___;
    if (rawIntervalId === rawIntervalId) { clearInterval(rawIntervalId); }
  }
  ___.markFuncFreeze(tameClearInterval);

  function makeScrollable(element) {
    var window = bridal.getWindow(element);
    var overflow = null;
    if (element.currentStyle) {
      overflow = element.currentStyle.overflow;
    } else if (window.getComputedStyle) {
      overflow = window.getComputedStyle(element, void 0).overflow;
    } else {
      overflow = null;
    }
    switch (overflow && overflow.toLowerCase()) {
      case 'visible':
      case 'hidden':
        element.style.overflow = 'auto';
        break;
    }
  }

  /**
   * Moves the given pixel within the element's frame of reference as close to
   * the top-left-most pixel of the element's viewport as possible without
   * moving the viewport beyond the bounds of the content.
   * @param {number} x x-coord of a pixel in the element's frame of reference.
   * @param {number} y y-coord of a pixel in the element's frame of reference.
   */
  function tameScrollTo(element, x, y) {
    if (x !== +x || y !== +y || x < 0 || y < 0) {
      throw new Error('Cannot scroll to ' + x + ':' + typeof x + ','
                      + y + ' : ' + typeof y);
    }
    element.scrollLeft = x;
    element.scrollTop = y;
  }

  /**
   * Moves the origin of the given element's view-port by the given offset.
   * @param {number} dx a delta in pixels.
   * @param {number} dy a delta in pixels.
   */
  function tameScrollBy(element, dx, dy) {
    if (dx !== +dx || dy !== +dy) {
      throw new Error('Cannot scroll by ' + dx + ':' + typeof dx + ', '
                      + dy + ':' + typeof dy);
    }
    element.scrollLeft += dx;
    element.scrollTop += dy;
  }

  function guessPixelsFromCss(cssStr) {
    if (!cssStr) { return 0; }
    var m = cssStr.match(/^([0-9]+)/);
    return m ? +m[1] : 0;
  }

  function tameResizeTo(element, w, h) {
    if (w !== +w || h !== +h) {
      throw new Error('Cannot resize to ' + w + ':' + typeof w + ', '
                      + h + ':' + typeof h);
    }
    element.style.width = w + 'px';
    element.style.height = h + 'px';
  }

  function tameResizeBy(element, dw, dh) {
    if (dw !== +dw || dh !== +dh) {
      throw new Error('Cannot resize by ' + dw + ':' + typeof dw + ', '
                      + dh + ':' + typeof dh);
    }
    if (!dw && !dh) { return; }

    // scrollWidth is width + padding + border.
    // offsetWidth is width + padding + border, but excluding the non-visible
    // area.
    // clientWidth iw width + padding, and like offsetWidth, clips to the
    // viewport.
    // margin does not count in any of these calculations.
    //
    // scrollWidth/offsetWidth
    //   +------------+
    //   |            |
    //
    // +----------------+
    // |                | Margin-top
    // | +------------+ |
    // | |############| | Border-top
    // | |#+--------+#| |
    // | |#|        |#| | Padding-top
    // | |#| +----+ |#| |
    // | |#| |    | |#| | Height
    // | |#| |    | |#| |
    // | |#| +----+ |#| |
    // | |#|        |#| |
    // | |#+--------+#| |
    // | |############| |
    // | +------------+ |
    // |                |
    // +----------------+
    //
    //     |        |
    //     +--------+
    //     clientWidth (but excludes content outside viewport)

    var style = element.currentStyle;
    if (!style) {
      style = bridal.getWindow(element).getComputedStyle(element, void 0);
    }

    // We guess the padding since it's not always expressed in px on IE
    var extraHeight = guessPixelsFromCss(style.paddingBottom)
        + guessPixelsFromCss(style.paddingTop);
    var extraWidth = guessPixelsFromCss(style.paddingLeft)
        + guessPixelsFromCss(style.paddingRight);

    var goalHeight = element.clientHeight + dh;
    var goalWidth = element.clientWidth + dw;

    var h = goalHeight - extraHeight;
    var w = goalWidth - extraWidth;

    if (dh) { element.style.height = Math.max(0, h) + 'px'; }
    if (dw) { element.style.width = Math.max(0, w) + 'px'; }

    // Correct if our guesses re padding and borders were wrong.
    // We may still not be able to resize if e.g. the deltas would take
    // a dimension negative.
    if (dh && element.clientHeight !== goalHeight) {
      var hError = element.clientHeight - goalHeight;
      element.style.height = Math.max(0, h - hError) + 'px';
    }
    if (dw && element.clientWidth !== goalWidth) {
      var wError = element.clientWidth - goalWidth;
      element.style.width = Math.max(0, w - wError) + 'px';
    }
  }

  // See above for a description of this function.
  function attachDocumentStub(
      idSuffix, uriCallback, imports, pseudoBodyNode, optPseudoWindowLocation) {
    var pluginId = ___.getId(imports);
    var document = pseudoBodyNode.ownerDocument;
    var bridal = bridalMaker(document);
    var window = bridal.getWindow(pseudoBodyNode);

    if (arguments.length < 4) {
      throw new Error('arity mismatch: ' + arguments.length);
    }
    if (!optPseudoWindowLocation) {
        optPseudoWindowLocation = {};
    }
    var elementPolicies = {};
    elementPolicies.form = function (attribs) {
      // Forms must have a gated onsubmit handler or they must have an
      // external target.
      var sawHandler = false;
      for (var i = 0, n = attribs.length; i < n; i += 2) {
        if (attribs[i] === 'onsubmit') {
          sawHandler = true;
        }
      }
      if (!sawHandler) {
        attribs.push('onsubmit', 'return false');
      }
      return attribs;
    };
    elementPolicies.a = elementPolicies.area = function (attribs) {
      // Anchor tags must always have the target '_blank'.
      attribs.push('target', '_blank');
      return attribs;
    };

    // On IE, turn an <canvas> tags into canvas elements that explorercanvas
    // will recognize 
    bridal.initCanvasElements(pseudoBodyNode);

    /** Sanitize HTML applying the appropriate transformations. */
    function sanitizeHtml(htmlText) {
      var out = [];
      htmlSanitizer(htmlText, out);
      return out.join('');
    }
    function sanitizeAttrs(tagName, attribs) {
      var n = attribs.length;
      for (var i = 0; i < n; i += 2) {
        var attribName = attribs[i];
        var value = attribs[i + 1];
        var atype = null, attribKey;
        if ((attribKey = tagName + '::' + attribName,
             html4.ATTRIBS.hasOwnProperty(attribKey))
            || (attribKey = '*::' + attribName,
                html4.ATTRIBS.hasOwnProperty(attribKey))) {
          atype = html4.ATTRIBS[attribKey];
          value = rewriteAttribute(tagName, attribName, atype, value);
        } else {
          value = null;
        }
        if (value !== null && value !== void 0) {
          attribs[i + 1] = value;
        } else {
          // Swap last attribute name/value pair in place, and reprocess here.
          // This could affect how user-agents deal with duplicate attributes.
          attribs[i + 1] = attribs[--n];
          attribs[i] = attribs[--n];
          i -= 2;
        }
      }
      attribs.length = n;
      var policy = elementPolicies[tagName];
      if (policy && elementPolicies.hasOwnProperty(tagName)) {
        return policy(attribs);
      }
      return attribs;
    }
    var htmlSanitizer = html.makeHtmlSanitizer(sanitizeAttrs);

    /**
     * If str ends with suffix,
     * and str is not identical to suffix,
     * then return the part of str before suffix.
     * Otherwise return fail.
     */
    function unsuffix(str, suffix, fail) {
      if (typeof str !== 'string') return fail;
      var n = str.length - suffix.length;
      if (0 < n && str.substring(n) === suffix) {
        return str.substring(0, n);
      } else {
        return fail;
      }
    }

    var ID_LIST_PARTS_PATTERN = new RegExp(
      '([^' + XML_SPACE + ']+)([' + XML_SPACE + ']+|$)', 'g');

    /** Convert a real attribute value to the value seen in a sandbox. */
    function virtualizeAttributeValue(attrType, realValue) {
      realValue = String(realValue);
      switch (attrType) {
        case html4.atype.GLOBAL_NAME:
        case html4.atype.ID:
        case html4.atype.IDREF:
          return unsuffix(realValue, idSuffix, null);
        case html4.atype.IDREFS:
          return realValue.replace(ID_LIST_PARTS_PATTERN,
              function(_, id, spaces) {
                return unsuffix(id, idSuffix, '') + (spaces ? ' ' : '');
              });
        case html4.atype.URI_FRAGMENT:
          if (realValue && '#' === realValue.charAt(0)) {
            realValue = unsuffix(realValue.substring(1), idSuffix, null);
            return realValue ? '#' + realValue : null;
          } else {
            return null;
          }
          break;
        default:
          return realValue;
      }
    }

    /**
     * Undoes some of the changes made by sanitizeHtml, e.g. stripping ID
     * prefixes.
     */
    function tameInnerHtml(htmlText) {
      var out = [];
      innerHtmlTamer(htmlText, out);
      return out.join('');
    }
    var innerHtmlTamer = html.makeSaxParser({
        startTag: function (tagName, attribs, out) {
          out.push('<', tagName);
          for (var i = 0; i < attribs.length; i += 2) {
            var aname = attribs[i];
            var atype = getAttributeType(tagName, aname);
            var value = attribs[i + 1];
            if (aname !== 'target' && atype !== void 0) {
              value = virtualizeAttributeValue(atype, value);
              if (typeof value === 'string') {
                out.push(' ', aname, '="', html.escapeAttrib(value), '"');
              }
            }
          }
          out.push('>');
        },
        endTag: function (name, out) { out.push('</', name, '>'); },
        pcdata: function (text, out) { out.push(text); },
        rcdata: function (text, out) { out.push(text); },
        cdata: function (text, out) { out.push(text); }
      });

    /**
     * Returns a normalized attribute value, or null if the attribute should
     * be omitted.
     * <p>This function satisfies the attribute rewriter interface defined in
     * {@link html-sanitizer.js}.  As such, the parameters are keys into
     * data structures defined in {@link html4-defs.js}.
     *
     * @param {string} tagName a canonical tag name.
     * @param {string} attribName a canonical tag name.
     * @param type as defined in html4-defs.js.
     *
     * @return {string|null} null to indicate that the attribute should not
     *   be set.
     */
    function rewriteAttribute(tagName, attribName, type, value) {
      switch (type) {
        case html4.atype.NONE:
          return String(value);
        case html4.atype.CLASSES:
          // note, className is arbitrary CDATA.
          value = String(value);
          if (!FORBIDDEN_ID_LIST_PATTERN.test(value)) {
            return value;
          }
          return null;
        case html4.atype.GLOBAL_NAME:
        case html4.atype.ID:
        case html4.atype.IDREF:
          value = String(value);
          if (value && isValidId(value)) {
            return value + idSuffix;
          }
          return null;
        case html4.atype.IDREFS:
          value = String(value);
          if (value && isValidIdList(value)) {
            return value.replace(ID_LIST_PARTS_PATTERN,
                function(_, id, spaces) {
                  return id + idSuffix + (spaces ? ' ' : '');
                });
          }
          return null;
        case html4.atype.LOCAL_NAME:
          value = String(value);
          if (value && isValidId(value)) {
            return value;
          }
          return null;
        case html4.atype.SCRIPT:
          value = String(value);
          // Translate a handler that calls a simple function like
          //   return foo(this, event)

          // TODO(mikesamuel): integrate cajita compiler to allow arbitrary
          // cajita in event handlers.
          var match = value.match(SIMPLE_HANDLER_PATTERN);
          if (!match) { return null; }
          var doesReturn = match[1];
          var fnName = match[2];
          value = (doesReturn ? 'return ' : '') + 'plugin_dispatchEvent___('
              + 'this, event, ' + pluginId + ', "'
              + fnName + '");';
          if (attribName === 'onsubmit') {
            value = 'try { ' + value + ' } finally { return false; }';
          }
          return value;
        case html4.atype.URI:
          value = String(value);
          if (!uriCallback) { return null; }
          return uriCallback.rewrite(
              value, getUriEffect(tagName, attribName), getLoaderType(tagName, attribName),
              { "XML_ATTR": attribName}) || null;
        case html4.atype.URI_FRAGMENT:
          value = String(value);
          if (value.charAt(0) === '#' && isValidId(value.substring(1))) {
            return value + idSuffix;
          }
          return null;
        case html4.atype.STYLE:
          if ('function' !== typeof value) {
            return sanitizeStyleAttrValue(String(value));
          }
          var cssPropertiesAndValues = cssSealerUnsealerPair.unseal(value);
          if (!cssPropertiesAndValues) { return null; }

          var css = [];
          for (var i = 0; i < cssPropertiesAndValues.length; i += 2) {
            var propName = cssPropertiesAndValues[i];
            var propValue = cssPropertiesAndValues[i + 1];
            // If the propertyName differs between DOM and CSS, there will
            // be a semicolon between the two.
            // E.g., 'background-color;backgroundColor'
            // See CssTemplate.toPropertyValueList.
            var semi = propName.indexOf(';');
            if (semi >= 0) { propName = propName.substring(0, semi); }
            css.push(propName + ' : ' + propValue);
          }
          return css.join(' ; ');
        // Frames are ambient, so disallow reference.
        case html4.atype.FRAME_TARGET:
        default:
          return null;
      }
    }

    function makeCache() {
      var cache = ___.newTable(false);
      cache.set(null, null);
      cache.set(void 0, null);
      return cache;
    }

    var editableTameNodeCache = makeCache();
    var readOnlyTameNodeCache = makeCache();

    /**
     * returns a tame DOM node.
     * @param {Node} node
     * @param {boolean} editable
     * @see <a href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html"
     *       >DOM Level 2</a>
     */
    function defaultTameNode(node, editable) {
      if (node === null || node === void 0) { return null; }
      // TODO(mikesamuel): make sure it really is a DOM node

      var cache = editable ? editableTameNodeCache : readOnlyTameNodeCache;
      var tamed = cache.get(node);
      if (tamed !== void 0) {
        return tamed;
      }
      switch (node.nodeType) {
        case 1:  // Element
          var tagName = node.tagName.toLowerCase();
          switch (tagName) {
            case 'a':
              tamed = new TameAElement(node, editable);
              break;
            case 'canvas':
              tamed = new TameCanvasElement(node, editable);
              break;
            case 'form':
              tamed = new TameFormElement(node, editable);
              break;
            case 'select':
            case 'button':
            case 'option':
            case 'textarea':
            case 'input':
              tamed = new TameInputElement(node, editable);
              break;
            case 'iframe':
              tamed = new TameIFrameElement(node, editable);
              break;
            case 'img':
              tamed = new TameImageElement(node, editable);
              break;
            case 'label':
              tamed = new TameLabelElement(node, editable);
              break;
            case 'script':
              tamed = new TameScriptElement(node, editable);
              break;
            case 'td':
            case 'thead':
            case 'tfoot':
            case 'tbody':
            case 'th':
              tamed = new TameTableCompElement(node, editable);
              break;
            case 'tr':
              tamed = new TameTableRowElement(node, editable);
              break;
            case 'table':
              tamed = new TameTableElement(node, editable);
              break;
            default:
              if (!html4.ELEMENTS.hasOwnProperty(tagName)
                  || (html4.ELEMENTS[tagName] & html4.eflags.UNSAFE)) {
                // If an unrecognized or unsafe node, return a
                // placeholder that doesn't prevent tree navigation,
                // but that doesn't allow mutation or leak attribute
                // information.
                tamed = new TameOpaqueNode(node, editable);
              } else {
                tamed = new TameElement(node, editable, editable);
              }
              break;
          }
          break;
        case 2:  // Attr
          // Cannot generically wrap since we must have access to the
          // owner element
          throw 'Internal: Attr nodes cannot be generically wrapped';
          break;
        case 3:  // Text
        case 4:  // CDATA Section Node
          tamed = new TameTextNode(node, editable);
          break;
        case 8:  // Comment
          tamed = new TameCommentNode(node, editable);
          break;
        case 11: // Document Fragment
          tamed = new TameBackedNode(node, editable, editable);
          break;
        default:
          tamed = new TameOpaqueNode(node, editable);
          break;
      }

      if (node.nodeType === 1) {
        cache.set(node, tamed);
      }
      return tamed;
    }

    function tameRelatedNode(node, editable, tameNodeCtor) {
      if (node === null || node === void 0) { return null; }
      if (node === tameDocument.body___) {
        if (tameDocument.editable___ && !editable) {
          // FIXME: return a non-editable version of body.
          throw new Error(NOT_EDITABLE);
        }
        return tameDocument.getBody___();
      }

      // Catch errors because node might be from a different domain.
      try {
        var docElem = node.ownerDocument.documentElement;
        for (var ancestor = node; ancestor; ancestor = ancestor.parentNode) {
          if (idClassPattern.test(ancestor.className)) {
            return tameNodeCtor(node, editable);
          } else if (ancestor === docElem) {
            return null;
          }
        }
        return tameNodeCtor(node, editable);
      } catch (e) {}
      return null;
    }

    /**
     * Returns the length of a raw DOM Nodelist object, working around
     * NamedNodeMap bugs in IE, Opera, and Safari as discussed at
     * http://code.google.com/p/google-caja/issues/detail?id=935
     *
     * @param nodeList a DOM NodeList.
     *
     * @return the number of nodes in the NodeList.
     */
    function getNodeListLength(nodeList) {
      var limit = nodeList.length;
      if (limit !== +limit) { limit = 1/0; }
      return limit;
    }

    /**
     * Constructs a NodeList-like object.
     *
     * @param tamed a JavaScript array that will be populated and decorated
     *     with the DOM NodeList API.
     * @param nodeList an array-like object supporting a "length" property
     *     and "[]" numeric indexing, or a raw DOM NodeList;
     * @param editable whether the tame nodes wrapped by this object
     *     should permit editing.
     * @param opt_tameNodeCtor a function for constructing tame nodes
     *     out of raw DOM nodes.
     */
    function mixinNodeList(tamed, nodeList, editable, opt_tameNodeCtor) {
      var limit = getNodeListLength(nodeList);
      if (limit > 0 && !opt_tameNodeCtor) {
        throw 'Internal: Nonempty mixinNodeList() without a tameNodeCtor';
      }

      for (var i = 0; i < limit && nodeList[i]; ++i) {
        tamed[i] = opt_tameNodeCtor(nodeList[i], editable);
      }

      // Guard against accidental leakage of untamed nodes
      nodeList = null;

      tamed.item = ___.markFuncFreeze(function (k) {
        k &= 0x7fffffff;
        if (k !== k) { throw new Error(); }
        return tamed[k] || null;
      });

      ___.grantFunc(tamed, 'item');

      return tamed;
    }

    function tameNodeList(nodeList, editable, opt_tameNodeCtor) {
      return ___.freeze(
          mixinNodeList([], nodeList, editable, opt_tameNodeCtor));
    }

    function tameOptionsList(nodeList, editable, opt_tameNodeCtor) {
      var nl = mixinNodeList([], nodeList, editable, opt_tameNodeCtor);
      nl.selectedIndex = +nodeList.selectedIndex;
      ___.grantRead(nl, 'selectedIndex');
      return ___.freeze(nl);
    }

    /**
     * Return a fake node list containing tamed nodes.
     * @param {Array.<TameNode>} array of tamed nodes.
     * @return an array that duck types to a node list.
     */
    function fakeNodeList(array) {
      var f = function(i) {
        return array[i];
      };
      array.item = ___.markFuncFreeze(f);
      ___.grantFunc(array, 'item');
      return ___.freeze(array);
    }

    /**
     * Constructs an HTMLCollection-like object which indexes its elements
     * based on their NAME attribute.
     *
     * @param tamed a JavaScript array that will be populated and decorated
     *     with the DOM HTMLCollection API.
     * @param nodeList an array-like object supporting a "length" property
     *     and "[]" numeric indexing.
     * @param editable whether the tame nodes wrapped by this object
     *     should permit editing.
     * @param opt_tameNodeCtor a function for constructing tame nodes
     *     out of raw DOM nodes.
     */
    function mixinHTMLCollection(tamed, nodeList, editable, opt_tameNodeCtor) {
      mixinNodeList(tamed, nodeList, editable, opt_tameNodeCtor);

      var tameNodesByName = {};
      var tameNode;

      for (var i = 0; i < tamed.length && (tameNode = tamed[i]); ++i) {
        var name = tameNode.getAttribute('name');
        if (name && !(name.charAt(name.length - 1) === '_' || (name in tamed)
                     || name === String(name & 0x7fffffff))) {
          if (!tameNodesByName[name]) { tameNodesByName[name] = []; }
          tameNodesByName[name].push(tameNode);
        }
      }

      ___.forOwnKeys(
        tameNodesByName,
        ___.markFuncFreeze(function (name, tameNodes) {
          if (tameNodes.length > 1) {
            tamed[name] = fakeNodeList(tameNodes);
            ___.grantRead(tamed, name);
          } else {
            tamed[name] = tameNodes[0];
            ___.grantRead(tamed, name);
          }
        }));

      tamed.namedItem = ___.markFuncFreeze(function(name) {
        name = String(name);
        if (name.charAt(name.length - 1) === '_') {
          return null;
        }
        if (___.hasOwnProp(tamed, name)) {
          return ___.passesGuard(TameNodeT, tamed[name])
              ? tamed[name] : tamed[name][0];
        }
        return null;
      });
      ___.grantFunc(tamed, 'namedItem');

      return tamed;
    }

    function tameHTMLCollection(nodeList, editable, opt_tameNodeCtor) {
      return ___.freeze(
          mixinHTMLCollection([], nodeList, editable, opt_tameNodeCtor));
    }

    function tameGetElementsByTagName(rootNode, tagName, editable) {
      tagName = String(tagName);
      if (tagName !== '*') {
        tagName = tagName.toLowerCase();
        if (!___.hasOwnProp(html4.ELEMENTS, tagName)
            || html4.ELEMENTS[tagName] & html4.ELEMENTS.UNSAFE) {
          // Allowing getElementsByTagName to work for opaque element types
          // would leak information about those elements.
          return new fakeNodeList([]);
        }
      }
      return tameNodeList(
          rootNode.getElementsByTagName(tagName), editable, defaultTameNode);
    }

    /**
     * Implements http://www.whatwg.org/specs/web-apps/current-work/#dom-document-getelementsbyclassname
     * using an existing implementation on browsers that have one.
     */
    function tameGetElementsByClassName(rootNode, className, editable) {
      className = String(className);

      // The quotes below are taken from the HTML5 draft referenced above.

      // "having obtained the classes by splitting a string on spaces"
      // Instead of using split, we use match with the global modifier so that
      // we don't have to remove leading and trailing spaces.
      var classes = className.match(/[^\t\n\f\r ]+/g);

      // Filter out classnames in the restricted namespace.
      for (var i = classes ? classes.length : 0; --i >= 0;) {
        var classi = classes[i];
        if (FORBIDDEN_ID_PATTERN.test(classi)) {
          classes[i] = classes[classes.length - 1];
          --classes.length;
        }
      }

      if (!classes || classes.length === 0) {
        // "If there are no tokens specified in the argument, then the method
        //  must return an empty NodeList" [instead of all elements]
        // This means that
        //     htmlEl.ownerDocument.getElementsByClassName(htmlEl.className)
        // will return an HtmlCollection containing htmlElement iff
        // htmlEl.className contains a non-space character.
        return fakeNodeList([]);
      }

      // "unordered set of unique space-separated tokens representing classes"
      if (typeof rootNode.getElementsByClassName === 'function') {
        return tameNodeList(
            rootNode.getElementsByClassName(
                classes.join(' ')), editable, defaultTameNode);
      } else {
        // Add spaces around each class so that we can use indexOf later to find
        // a match.
        // This use of indexOf is strictly incorrect since
        // http://www.whatwg.org/specs/web-apps/current-work/#reflecting-content-attributes-in-dom-attributes
        // does not normalize spaces in unordered sets of unique space-separated
        // tokens.  This is not a problem since HTML5 compliant implementations
        // already have a getElementsByClassName implementation, and legacy
        // implementations do normalize according to comments on issue 935.

        // We assume standards mode, so the HTML5 requirement that
        //   "If the document is in quirks mode, then the comparisons for the
        //    classes must be done in an ASCII case-insensitive  manner,"
        // is not operative.
        var nClasses = classes.length;
        for (var i = nClasses; --i >= 0;) {
          classes[i] = ' ' + classes[i] + ' ';
        }

        // We comply with the requirement that the result is a list
        //   "containing all the elements in the document, in tree order,"
        // since the spec for getElementsByTagName has the same language.
        var candidates = rootNode.getElementsByTagName('*');
        var matches = [];
        var limit = candidates.length;
        if (limit !== +limit) { limit = 1/0; }  // See issue 935
        candidate_loop:
        for (var j = 0, candidate, k = -1;
             j < limit && (candidate = candidates[j]);
             ++j) {
          var candidateClass = ' ' + candidate.className + ' ';
          for (var i = nClasses; --i >= 0;) {
            if (-1 === candidateClass.indexOf(classes[i])) {
              continue candidate_loop;
            }
          }
          var tamed = defaultTameNode(candidate, editable);
          if (tamed) {
            matches[++k] = tamed;
          }
        }
        // "the method must return a live NodeList object"
        return fakeNodeList(matches);
      }
    }

    function makeEventHandlerWrapper(thisNode, listener) {
      classUtils.ensureValidCallback(listener);
      function wrapper(event) {
        return plugin_dispatchEvent___(
            thisNode, event, ___.getId(imports), listener);
      }
      return wrapper;
    }

    var NOT_EDITABLE = "Node not editable.";
    var INVALID_SUFFIX = "Property names may not end in '__'.";
    var UNSAFE_TAGNAME = "Unsafe tag name.";
    var UNKNOWN_TAGNAME = "Unknown tag name.";
    var INDEX_SIZE_ERROR = "Index size error.";

    /**
     * Define a property with the given name on the given ctor's prototype that
     * is accessible to untrusted code.
     * @param {Function} ctor the ctor for the class to modify.
     * @param {string} name the name of the property to define.
     * @param {boolean} useAttrGetter true if the getter should delegate to
     *     {@code ctor.prototype.getAttribute}.  That method is assumed to
     *     already be trusted though {@code toValue} is still called on the
     *     result.
     *     If false, then {@code toValue} is called on the result of accessing
     *     the name property on the underlying element, a possibly untrusted
     *     value.
     * @param {Function} toValue transforms the attribute or underlying property
     *     value retrieved according to the useAttrGetter flag above to the
     *     value of the defined property.
     * @param {boolean} useAttrSetter like useAttrGetter but for a setter.
     *     Switches between the name property on the underlying node
     *     (the false case) or using ctor's {@code setAttribute} method
     *     (the true case).
     * @param {Function} fromValue called on the input before it is passed
     *     through according to the flag above.  This receives untrusted values,
     *     and can do any vetting or transformation.  If {@code useAttrSetter}
     *     is true then it need not do much value vetting since the
     *     {@code setAttribute} method must do its own vetting.
     */
    function defProperty(
        ctor, name, useAttrGetter, toValue, useAttrSetter, fromValue) {
      // Given the name foo, the suffix is Foo___ so we end up defining
      // inaccessible methods getFoo___ and setFoo__ on ctor's prototype below.
      var getterSetterSuffix = classUtils.getterSetterSuffix(name);
      var proto = ctor.prototype;
      if (toValue) {
        proto['get' + getterSetterSuffix] = useAttrGetter
            ? function () {
                return toValue.call(this, this.getAttribute(name));
              }
            : function () {
                return toValue.call(this, this.node___[name]);
              };
      }
      if (fromValue) {
        proto['set' + getterSetterSuffix] = useAttrSetter
            ? function (value) {
                this.setAttribute(name, fromValue.call(this, value));
                return value;
              }
            : function (value) {
                if (!this.editable___) { throw new Error(NOT_EDITABLE); }
                this.node___[name] = fromValue.call(this, value);
                return value;
              };
      }
    }
    function defAttributeAlias(ctor, name, toValue, fromValue) {
      defProperty(ctor, name, true, toValue, true, fromValue);
    }

    // Implementation of EventTarget::addEventListener
    function tameAddEventListener(name, listener, useCapture) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      if (!this.wrappedListeners___) { this.wrappedListeners___ = []; }
      useCapture = Boolean(useCapture);
      var wrappedListener = makeEventHandlerWrapper(this.node___, listener);
      wrappedListener = bridal.addEventListener(
          this.node___, name, wrappedListener, useCapture);
      wrappedListener.originalListener___ = listener;
      this.wrappedListeners___.push(wrappedListener);
    }

    // Implementation of EventTarget::removeEventListener
    function tameRemoveEventListener(name, listener, useCapture) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      if (!this.wrappedListeners___) { return; }
      var wrappedListener = null;
      for (var i = this.wrappedListeners___.length; --i >= 0;) {
        if (this.wrappedListeners___[i].originalListener___ === listener) {
          wrappedListener = this.wrappedListeners___[i];
          arrayRemove(this.wrappedListeners___, i, i);
          break;
        }
      }
      if (!wrappedListener) { return; }
      bridal.removeEventListener(
          this.node___, name, wrappedListener, useCapture);
    }

    // A map of tamed node classes, keyed by DOM Level 2 standard name, which
    // will be exposed to the client.
    var nodeClasses = {};

    function inertCtor(tamedCtor, someSuper, name) {
      return nodeClasses[name] = ___.extend(tamedCtor, someSuper, name);
    }

    var tameNodeFields = [
        'nodeType', 'nodeValue', 'nodeName', 'firstChild',
        'lastChild', 'nextSibling', 'previousSibling', 'parentNode',
        'ownerDocument', 'childNodes', 'attributes'];

    /**
     * Base class for a Node wrapper.  Do not create directly -- use the
     * tameNode factory instead.
     * @param {boolean} editable true if the node's value, attributes, children,
     *     or custom properties are mutable.
     * @constructor
     */
    function TameNode(editable) {
      this.editable___ = editable;
      TameNodeMark.stamp.mark___(this);
      classUtils.exportFields(this, tameNodeFields);
    }
    inertCtor(TameNode, Object, 'Node');
    TameNode.prototype.getOwnerDocument___ = function () {
      // TODO(mikesamuel): upward navigation breaks capability discipline.
      if (!this.editable___ && tameDocument.editable___) {
        throw new Error(NOT_EDITABLE);
      }
      return tameDocument;
    };
    // abstract TameNode.prototype.getNodeType___
    // abstract TameNode.prototype.getNodeName___
    // abstract TameNode.prototype.getNodeValue___
    // abstract TameNode.prototype.cloneNode
    // abstract TameNode.prototype.appendChild
    // abstract TameNode.prototype.insertBefore
    // abstract TameNode.prototype.removeChild
    // abstract TameNode.prototype.replaceChild
    // abstract TameNode.prototype.getFirstChild___
    // abstract TameNode.prototype.getLastChild___
    // abstract TameNode.prototype.getNextSibling___
    // abstract TameNode.prototype.getPreviousSibling___
    // abstract TameNode.prototype.getParentNode___
    // abstract TameNode.prototype.getElementsByTagName
    // abstract TameNode.prototype.getElementsByClassName
    // abstract TameNode.prototype.getChildNodes___
    // abstract TameNode.prototype.getAttributes___
    var tameNodePublicMembers = [
        'cloneNode',
        'appendChild', 'insertBefore', 'removeChild', 'replaceChild',
        'getElementsByClassName', 'getElementsByTagName', 'dispatchEvent',
        'hasChildNodes'
        ];

    /**
     * A tame node that is backed by a real node.
     * @param {boolean} childrenEditable true iff the child list is mutable.
     * @constructor
     */
    function TameBackedNode(node, editable, childrenEditable) {
      if (!node) {
        throw new Error('Creating tame node with undefined native delegate');
      }
      this.node___ = node;
      this.childrenEditable___ = editable && childrenEditable;
      this.FERAL_TWIN___ = node;
      try {
        // bug 1330 IE<=8 doesn't allow arbitrary properties on text nodes,
        // so this can fail, but failures are mostly harmless.
        node.TAMED_TWIN___ = this;
      } catch (ex) {
        ___.log("Warning: couldn't set TAMED_TWIN___ for " + node);
      }
      TameNode.call(this, editable);
    }
    ___.extend(TameBackedNode, TameNode);
    TameBackedNode.prototype.getNodeType___ = function () {
      return this.node___.nodeType;
    };
    TameBackedNode.prototype.getNodeName___ = function () {
      return this.node___.nodeName;
    };
    TameBackedNode.prototype.getNodeValue___ = function () {
      return this.node___.nodeValue;
    };
    TameBackedNode.prototype.cloneNode = function (deep) {
      var clone = bridal.cloneNode(this.node___, Boolean(deep));
      // From http://www.w3.org/TR/DOM-Level-2-Core/core.html#ID-3A0ED0A4
      //     "Note that cloning an immutable subtree results in a mutable copy"
      return defaultTameNode(clone, true);
    };
    TameBackedNode.prototype.appendChild = function (child) {
      // Child must be editable since appendChild can remove it from its parent.
      child = TameNodeT.coerce(child);
      if (!this.childrenEditable___ || !child.editable___) {
        throw new Error(NOT_EDITABLE);
      }
      this.node___.appendChild(child.node___);
      return child;
    };
    TameBackedNode.prototype.insertBefore = function (toInsert, child) {
      toInsert = TameNodeT.coerce(toInsert);
      if (child === void 0) { child = null; }
      if (child !== null) {
        child = TameNodeT.coerce(child);
        if (!child.editable___) {
          throw new Error(NOT_EDITABLE);
        }
      }
      if (!this.childrenEditable___ || !toInsert.editable___) {
        throw new Error(NOT_EDITABLE);
      }
      this.node___.insertBefore(
          toInsert.node___, child !== null ? child.node___ : null);
      return toInsert;
    };
    TameBackedNode.prototype.removeChild = function (child) {
      child = TameNodeT.coerce(child);
      if (!this.childrenEditable___ || !child.editable___) {
        throw new Error(NOT_EDITABLE);
      }
      this.node___.removeChild(child.node___);
      return child;
    };
    TameBackedNode.prototype.replaceChild = function (newChild, oldChild) {
      newChild = TameNodeT.coerce(newChild);
      oldChild = TameNodeT.coerce(oldChild);
      if (!this.childrenEditable___ || !newChild.editable___
          || !oldChild.editable___) {
        throw new Error(NOT_EDITABLE);
      }
      this.node___.replaceChild(newChild.node___, oldChild.node___);
      return oldChild;
    };
    TameBackedNode.prototype.getFirstChild___ = function () {
      return defaultTameNode(this.node___.firstChild, this.childrenEditable___);
    };
    TameBackedNode.prototype.getLastChild___ = function () {
      return defaultTameNode(this.node___.lastChild, this.childrenEditable___);
    };
    TameBackedNode.prototype.getNextSibling___ = function () {
      return tameRelatedNode(this.node___.nextSibling, this.editable___,
                             defaultTameNode);
    };
    TameBackedNode.prototype.getPreviousSibling___ = function () {
      return tameRelatedNode(this.node___.previousSibling, this.editable___,
                             defaultTameNode);
    };
    TameBackedNode.prototype.getParentNode___ = function () {
      return tameRelatedNode(
          this.node___.parentNode, this.editable___, defaultTameNode);
    };
    TameBackedNode.prototype.getElementsByTagName = function (tagName) {
      return tameGetElementsByTagName(
          this.node___, tagName, this.childrenEditable___);
    };
    TameBackedNode.prototype.getElementsByClassName = function (className) {
      return tameGetElementsByClassName(
          this.node___, className, this.childrenEditable___);
    };
    TameBackedNode.prototype.getChildNodes___ = function () {
      return tameNodeList(
          this.node___.childNodes, this.childrenEditable___, defaultTameNode);
    };
    TameBackedNode.prototype.getAttributes___ = function () {
      var thisNode = this.node___;
      var tameNodeCtor = function(node, editable) {
        return new TameBackedAttributeNode(node, editable, thisNode);
      };
      return tameNodeList(
          this.node___.attributes, this.editable___, tameNodeCtor);
    };
    var endsWith__ = /__$/;
    // TODO(erights): Come up with some notion of a keeper chain so we can
    // say, "let every other keeper try to handle this first".
    TameBackedNode.prototype.handleRead___ = function (name) {
      name = String(name);
      if (endsWith__.test(name)) { return void 0; }
      if (classUtils.hasGetHandler(this, name)) {
        return this.v___(name);
      }
      if (classUtils.hasGetHandler(this, name.toLowerCase())) {
        return this.v___(name.toLowerCase());
      }
      if (___.hasOwnProp(this.node___.properties___, name)) {
        return this.node___.properties___[name];
      } else {
        return void 0;
      }
    };
    TameBackedNode.prototype.handleCall___ = function (name, args) {
      name = String(name);
      if (endsWith__.test(name)) { throw new Error(INVALID_SUFFIX); }
      var handlerName = name + '_handler___';
      if (this[handlerName]) {
        return this[handlerName].call(this, args);
      }
      handlerName = handlerName.toLowerCase();
      if (this[handlerName]) {
        return this[handlerName].call(this, args);
      }
      if (___.hasOwnProp(this.node___.properties___, name)) {
        return this.node___.properties___[name].call(this, args);
      } else {
        throw new TypeError(name + ' is not a function.');
      }
    };
    TameBackedNode.prototype.handleSet___ = function (name, val) {
      name = String(name);
      if (endsWith__.test(name)) { throw new Error(INVALID_SUFFIX); }
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      if (classUtils.hasSetHandler(this, name)) {
        return this.w___(name, val);
      }
      if (classUtils.hasSetHandler(this, name.toLowerCase())) {
        return this.w___(name.toLowerCase(), val);
      }
      if (!this.node___.properties___) {
        this.node___.properties___ = {};
      }
      this[name + '_c___'] = this;
      this[name + '_v___'] = false;
      this[name + '_e___'] = this;
      return this.node___.properties___[name] = val;
    };
    TameBackedNode.prototype.handleDelete___ = function (name) {
      name = String(name);
      if (endsWith__.test(name)) { throw new Error(INVALID_SUFFIX); }
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      var handlerName = name + '_deleter___';
      if (this[handlerName]) {
        return this[handlerName]();
      }
      handlerName = handlerName.toLowerCase();
      if (this[handlerName]) {
        return this[handlerName]();
      }
      if (this.node___.properties___) {
        return (
            delete this.node___.properties___[name]
            && delete this[name + '_c___']
            && delete this[name + '_e___']
            && delete this[name + '_v___']);
      } else {
        return true;
      }
    };
    /**
     * @param {boolean} ownFlag ignored
     */
    TameBackedNode.prototype.handleEnum___ = function (ownFlag) {
      // TODO(metaweta): Add code to list all the other handled stuff we know
      // about.
      if (this.node___.properties___) {
        return ___.allKeys(this.node___.properties___);
      }
      return [];
    };
    TameBackedNode.prototype.hasChildNodes = function () {
      return !!this.node___.hasChildNodes();
    };
    // http://www.w3.org/TR/DOM-Level-2-Events/events.html#Events-EventTarget :
    // "The EventTarget interface is implemented by all Nodes"
    TameBackedNode.prototype.dispatchEvent = function dispatchEvent(evt) {
      evt = TameEventT.coerce(evt);
      bridal.dispatchEvent(this.node___, evt.event___);
    };
    ___.all2(
        ___.grantTypedMethod, TameBackedNode.prototype, tameNodePublicMembers);
    if (document.documentElement.contains) {  // typeof is 'object' on IE
      TameBackedNode.prototype.contains = function (other) {
        other = TameNodeT.coerce(other);
        var otherNode = other.node___;
        return this.node___.contains(otherNode);
      };
    }
    if ('function' ===
        typeof document.documentElement.compareDocumentPosition) {
      /**
       * Speced in <a href="http://www.w3.org/TR/DOM-Level-3-Core/core.html#Node3-compareDocumentPosition">DOM-Level-3</a>.
       */
      TameBackedNode.prototype.compareDocumentPosition = function (other) {
        other = TameNodeT.coerce(other);
        var otherNode = other.node___;
        if (!otherNode) { return 0; }
        var bitmask = +this.node___.compareDocumentPosition(otherNode);
        // To avoid leaking information about the relative positioning of
        // different roots, if neither contains the other, then we mask out
        // the preceding/following bits.
        // 0x18 is (CONTAINS | CONTAINED)
        // 0x1f is all the bits documented at
        //     http://www.w3.org/TR/DOM-Level-3-Core/core.html#DocumentPosition
        //     except IMPLEMENTATION_SPECIFIC
        // 0x01 is DISCONNECTED
        /*
        if (!(bitmask & 0x18)) {
          // TODO: If they are not under the same virtual doc root, return
          // DOCUMENT_POSITION_DISCONNECTED instead of leaking information
          // about PRECEDING | FOLLOWING.
        }
        */
        // Firefox3 returns spurious PRECEDING and FOLLOWING bits for
        // disconnected trees.
        // https://bugzilla.mozilla.org/show_bug.cgi?id=486002
        if (bitmask & 1) {
          bitmask &= ~6;
        }
        return bitmask & 0x1f;
      };
      if (!___.hasOwnProp(TameBackedNode.prototype, 'contains')) {
        // http://www.quirksmode.org/blog/archives/2006/01/contains_for_mo.html
        TameBackedNode.prototype.contains = function (other) {
          var docPos = this.compareDocumentPosition(other);
          return !(!(docPos & 0x10) && docPos);
        };
      }
    }
    ___.all2(function (o, k) {
               if (___.hasOwnProp(o, k)) { ___.grantTypedMethod(o, k);  }
             }, TameBackedNode.prototype,
             ['contains', 'compareDocumentPosition']);

    /**
     * A fake node that is not backed by a real DOM node.
     * @constructor
     */
    function TamePseudoNode(editable) {
      TameNode.call(this, editable);
      this.properties___ = {};
      this.TAMED_TWIN___ = this.FERAL_TWIN___ = this;
    }
    ___.extend(TamePseudoNode, TameNode);
    TamePseudoNode.prototype.appendChild =
    TamePseudoNode.prototype.insertBefore =
    TamePseudoNode.prototype.removeChild =
    TamePseudoNode.prototype.replaceChild = function () {
      ___.log("Node not editable; no action performed.");
      return void 0;
    };
    TamePseudoNode.prototype.getFirstChild___ = function () {
      var children = this.getChildNodes___();
      return children.length ? children[0] : null;
    };
    TamePseudoNode.prototype.getLastChild___ = function () {
      var children = this.getChildNodes___();
      return children.length ? children[children.length - 1] : null;
    };
    TamePseudoNode.prototype.getNextSibling___ = function () {
      var parentNode = this.getParentNode___();
      if (!parentNode) { return null; }
      var siblings = parentNode.getChildNodes___();
      for (var i = siblings.length - 1; --i >= 0;) {
        if (siblings[i] === this) { return siblings[i + 1]; }
      }
      return null;
    };
    TamePseudoNode.prototype.getPreviousSibling___ = function () {
      var parentNode = this.getParentNode___();
      if (!parentNode) { return null; }
      var siblings = parentNode.getChildNodes___();
      for (var i = siblings.length; --i >= 1;) {
        if (siblings[i] === this) { return siblings[i - 1]; }
      }
      return null;
    };
    TamePseudoNode.prototype.handleRead___ = function (name) {
      name = String(name);
      if (endsWith__.test(name)) { return void 0; }
      var handlerName = name + '_getter___';
      if (this[handlerName]) {
        return this[handlerName]();
      }
      handlerName = handlerName.toLowerCase();
      if (this[handlerName]) {
        return this[handlerName]();
      }
      if (___.hasOwnProp(this.properties___, name)) {
        return this.properties___[name];
      } else {
        return void 0;
      }
    };
    TamePseudoNode.prototype.handleCall___ = function (name, args) {
      name = String(name);
      if (endsWith__.test(name)) { throw new Error(INVALID_SUFFIX); }
      var handlerName = name + '_handler___';
      if (this[handlerName]) {
        return this[handlerName].call(this, args);
      }
      handlerName = handlerName.toLowerCase();
      if (this[handlerName]) {
        return this[handlerName].call(this, args);
      }
      if (___.hasOwnProp(this.properties___, name)) {
        return this.properties___[name].call(this, args);
      } else {
        throw new TypeError(name + ' is not a function.');
      }
    };
    TamePseudoNode.prototype.handleSet___ = function (name, val) {
      name = String(name);
      if (endsWith__.test(name)) { throw new Error(INVALID_SUFFIX); }
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      var handlerName = name + '_setter___';
      if (this[handlerName]) {
        return this[handlerName](val);
      }
      handlerName = handlerName.toLowerCase();
      if (this[handlerName]) {
        return this[handlerName](val);
      }
      if (!this.properties___) {
        this.properties___ = {};
      }
      this[name + '_c___'] = this;
      this[name + '_v___'] = false;
      this[name + '_e___'] = this;
      return this.properties___[name] = val;
    };
    TamePseudoNode.prototype.handleDelete___ = function (name) {
      name = String(name);
      if (endsWith__.test(name)) { throw new Error(INVALID_SUFFIX); }
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      var handlerName = name + '_deleter___';
      if (this[handlerName]) {
        return this[handlerName]();
      }
      handlerName = handlerName.toLowerCase();
      if (this[handlerName]) {
        return this[handlerName]();
      }
      if (this.properties___) {
        return (
            delete this.properties___[name]
            && delete this[name + '_c___']
            && delete this[name + '_e___']
            && delete this[name + '_v___']);
      } else {
        return true;
      }
    };
    TamePseudoNode.prototype.handleEnum___ = function (ownFlag) {
      // TODO(metaweta): Add code to list all the other handled stuff we know
      // about.
      if (this.properties___) {
        return ___.allKeys(this.properties___);
      }
      return [];
    };
    TamePseudoNode.prototype.hasChildNodes = function () {
      return this.getFirstChild___() != null;
    };
    ___.all2(
        ___.grantTypedMethod, TamePseudoNode.prototype, tameNodePublicMembers);

    var commonElementPropertyHandlers = {
      clientWidth: {
        get: function () { return this.getGeometryDelegate___().clientWidth; }
      },
      clientHeight: {
        get: function () { return this.getGeometryDelegate___().clientHeight; }
      },
      offsetLeft: {
        get: function () { return this.getGeometryDelegate___().offsetLeft; }
      },
      offsetTop: {
        get: function () { return this.getGeometryDelegate___().offsetTop; }
      },
      offsetWidth: {
        get: function () { return this.getGeometryDelegate___().offsetWidth; }
      },
      offsetHeight: {
        get: function () { return this.getGeometryDelegate___().offsetHeight; }
      },
      scrollLeft: {
        get: function () { return this.getGeometryDelegate___().scrollLeft; },
        set: function (x) {
          if (!this.editable___) { throw new Error(NOT_EDITABLE); }
          this.getGeometryDelegate___().scrollLeft = +x;
          return x;
        }
      },
      scrollTop: {
        get: function () { return this.getGeometryDelegate___().scrollTop; },
        set: function (y) {
          if (!this.editable___) { throw new Error(NOT_EDITABLE); }
          this.getGeometryDelegate___().scrollTop = +y;
          return y;
        }
      },
      scrollWidth: {
        get: function () { return this.getGeometryDelegate___().scrollWidth; }
      },
      scrollHeight: {
        get: function () { return this.getGeometryDelegate___().scrollHeight; }
      }
    };

    function TamePseudoElement(
        tagName, tameDoc, childNodesGetter, parentNodeGetter, innerHTMLGetter,
        geometryDelegate, editable) {
      TamePseudoNode.call(this, editable);
      this.tagName___ = tagName;
      this.tameDoc___ = tameDoc;
      this.childNodesGetter___ = childNodesGetter;
      this.parentNodeGetter___ = parentNodeGetter;
      this.innerHTMLGetter___ = innerHTMLGetter;
      this.geometryDelegate___ = geometryDelegate;
      classUtils.exportFields(this, ['tagName', 'innerHTML']);
      classUtils.applyAccessors(this, commonElementPropertyHandlers);
    }
    ___.extend(TamePseudoElement, TamePseudoNode);
    // TODO(mikesamuel): make nodeClasses work.
    TamePseudoElement.prototype.getNodeType___ = function () { return 1; };
    TamePseudoElement.prototype.getNodeName___
        = TamePseudoElement.prototype.getTagName___
        = function () { return this.tagName___; };
    TamePseudoElement.prototype.getNodeValue___ = function () { return null; };
    TamePseudoElement.prototype.getAttribute
        = function (attribName) { return null; };
    TamePseudoElement.prototype.setAttribute
        = function (attribName, value) { };
    TamePseudoElement.prototype.hasAttribute
        = function (attribName) { return false; };
    TamePseudoElement.prototype.removeAttribute
        = function (attribName) { };
    TamePseudoElement.prototype.getOwnerDocument___
        = function () { return this.tameDoc___; };
    TamePseudoElement.prototype.getChildNodes___
        = function () { return this.childNodesGetter___(); };
    TamePseudoElement.prototype.getAttributes___
        = function () { return tameNodeList([], false, undefined); };
    TamePseudoElement.prototype.getParentNode___
        = function () { return this.parentNodeGetter___(); };
    TamePseudoElement.prototype.getInnerHTML___
        = function () { return this.innerHTMLGetter___(); };
    TamePseudoElement.prototype.getElementsByTagName = function (tagName) {
      tagName = String(tagName).toLowerCase();
      if (tagName === this.tagName___) {
        // Works since html, head, body, and title can't contain themselves.
        return fakeNodeList([]);
      }
      return this.getOwnerDocument___().getElementsByTagName(tagName);
    };
    TamePseudoElement.prototype.getElementsByClassName = function (className) {
      return this.getOwnerDocument___().getElementsByClassName(className);
    };
    TamePseudoElement.prototype.getBoundingClientRect = function () {
      return this.geometryDelegate___.getBoundingClientRect();
    };
    TamePseudoElement.prototype.getGeometryDelegate___ = function () {
      return this.geometryDelegate___;
    };
    TamePseudoElement.prototype.toString = ___.markFuncFreeze(function () {
      return '<' + this.tagName___ + '>';
    });
    ___.all2(___.grantTypedMethod, TamePseudoElement.prototype,
             ['getAttribute', 'setAttribute',
              'hasAttribute', 'removeAttribute',
              'getBoundingClientRect', 'getElementsByTagName']);

    function TameOpaqueNode(node, editable) {
      TameBackedNode.call(this, node, editable, editable);
    }
    ___.extend(TameOpaqueNode, TameBackedNode);
    TameOpaqueNode.prototype.getNodeValue___
        = TameBackedNode.prototype.getNodeValue___;
    TameOpaqueNode.prototype.getNodeType___
        = TameBackedNode.prototype.getNodeType___;
    TameOpaqueNode.prototype.getNodeName___
        = TameBackedNode.prototype.getNodeName___;
    TameOpaqueNode.prototype.getNextSibling___
        = TameBackedNode.prototype.getNextSibling___;
    TameOpaqueNode.prototype.getPreviousSibling___
        = TameBackedNode.prototype.getPreviousSibling___;
    TameOpaqueNode.prototype.getFirstChild___
        = TameBackedNode.prototype.getFirstChild___;
    TameOpaqueNode.prototype.getLastChild___
        = TameBackedNode.prototype.getLastChild___;
    TameOpaqueNode.prototype.getParentNode___
        = TameBackedNode.prototype.getParentNode___;
    TameOpaqueNode.prototype.getChildNodes___
        = TameBackedNode.prototype.getChildNodes___;
    TameOpaqueNode.prototype.getOwnerDocument___
        = TameBackedNode.prototype.getOwnerDocument___;
    TameOpaqueNode.prototype.getElementsByTagName
        = TameBackedNode.prototype.getElementsByTagName;
    TameOpaqueNode.prototype.getElementsByClassName
        = TameBackedNode.prototype.getElementsByClassName;
    TameOpaqueNode.prototype.hasChildNodes
        = TameBackedNode.prototype.hasChildNodes;
    TameOpaqueNode.prototype.getAttributes___
        = function () { return tameNodeList([], false, undefined); };
    for (var i = tameNodePublicMembers.length; --i >= 0;) {
      var k = tameNodePublicMembers[i];
      if (!TameOpaqueNode.prototype.hasOwnProperty(k)) {
        TameOpaqueNode.prototype[k] = ___.markFuncFreeze(function () {
          throw new Error('Node is opaque');
        });
      }
    }
    ___.all2(
        ___.grantTypedMethod, TameOpaqueNode.prototype, tameNodePublicMembers);

    function TameTextNode(node, editable) {
      assert(node.nodeType === 3);

      // The below should not be strictly necessary since childrenEditable for
      // TameScriptElements is always false, but it protects against tameNode
      // being called naively on a text node from container code.
      var pn = node.parentNode;
      if (editable && pn) {
        if (1 === pn.nodeType
            && (html4.ELEMENTS[pn.tagName.toLowerCase()]
                & html4.eflags.UNSAFE)) {
          // Do not allow mutation of text inside script elements.
          // See the testScriptLoading testcase for examples of exploits.
          editable = false;
        }
      }

      TameBackedNode.call(this, node, editable, editable);
      classUtils.exportFields(
          this, ['nodeValue', 'data', 'textContent', 'innerText']);
    }
    inertCtor(TameTextNode, TameBackedNode, 'Text');
    TameTextNode.prototype.setNodeValue___ = function (value) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.nodeValue = String(value || '');
      return value;
    };
    TameTextNode.prototype.getTextContent___
        = TameTextNode.prototype.getInnerText___
        = TameTextNode.prototype.getData___
        = TameTextNode.prototype.getNodeValue___;
    TameTextNode.prototype.setTextContent___
        = TameTextNode.prototype.setInnerText___
        = TameTextNode.prototype.setData___
        = TameTextNode.prototype.setNodeValue___;
    TameTextNode.prototype.toString = ___.markFuncFreeze(function () {
      return '#text';
    });

    function TameCommentNode(node, editable) {
      assert(node.nodeType === 8);
      TameBackedNode.call(this, node, editable, editable);
    }
    inertCtor(TameCommentNode, TameBackedNode, 'CommentNode');
    TameCommentNode.prototype.toString = ___.markFuncFreeze(function () {
      return '#comment';
    });

    function lookupAttribute(map, tagName, attribName) {
      var attribKey;
      attribKey = tagName + '::' + attribName;
      if (map.hasOwnProperty(attribKey)) {
        return map[attribKey];
      }
      attribKey = '*::' + attribName;
      if (map.hasOwnProperty(attribKey)) {
        return map[attribKey];
      }
      return void 0;
    }
    function getAttributeType(tagName, attribName) {
      return lookupAttribute(html4.ATTRIBS, tagName, attribName);
    }
    function getLoaderType(tagName, attribName) {
      return lookupAttribute(html4.LOADERTYPES, tagName, attribName);
    }
    function getUriEffect(tagName, attribName) {
      return lookupAttribute(html4.URIEFFECTS, tagName, attribName);
    }

    /**
     * Plays the role of an Attr node for TameElement objects.
     */
    function TameBackedAttributeNode(node, editable, ownerElement) {
      TameBackedNode.call(this, node, editable);
      this.ownerElement___ = ownerElement;
      classUtils.exportFields(this,
          ['name', 'specified', 'value', 'ownerElement']);
    }
    inertCtor(TameBackedAttributeNode, TameBackedNode, 'Attr');
    TameBackedAttributeNode.prototype.getNodeName___ =
    TameBackedAttributeNode.prototype.getName___ =
        function () { return String(this.node___.name); };
    TameBackedAttributeNode.prototype.getSpecified___ = function () {
      return defaultTameNode(this.ownerElement___, this.editable___)
          .hasAttribute(this.getName___());
    };
    TameBackedAttributeNode.prototype.getNodeValue___ =
    TameBackedAttributeNode.prototype.getValue___ = function () {
      return defaultTameNode(this.ownerElement___, this.editable___)
          .getAttribute(this.getName___());
    };
    TameBackedAttributeNode.prototype.setNodeValue___ =
    TameBackedAttributeNode.prototype.setValue___ = function (value) {
      return defaultTameNode(this.ownerElement___, this.editable___)
          .setAttribute(this.getName___(), value);
    };
    TameBackedAttributeNode.prototype.getOwnerElement___ = function () {
      return defaultTameNode(this.ownerElement___, this.editable___);
    };
    TameBackedAttributeNode.prototype.getNodeType___ = function () {
      return 2;
    };
    TameBackedAttributeNode.prototype.cloneNode = function (deep) {
      var clone = bridal.cloneNode(this.node___, Boolean(deep));
      // From http://www.w3.org/TR/DOM-Level-2-Core/core.html#ID-3A0ED0A4
      //     "Note that cloning an immutable subtree results in a mutable copy"
      return new TameBackedAttributeNode(clone, true, this.ownerElement____);
    };
    TameBackedAttributeNode.prototype.appendChild =
    TameBackedAttributeNode.prototype.insertBefore =
    TameBackedAttributeNode.prototype.removeChild =
    TameBackedAttributeNode.prototype.replaceChild =
    TameBackedAttributeNode.prototype.getFirstChild___ =
    TameBackedAttributeNode.prototype.getLastChild___ =
    TameBackedAttributeNode.prototype.getNextSibling___ =
    TameBackedAttributeNode.prototype.getPreviousSibling___ =
    TameBackedAttributeNode.prototype.getParentNode___ =
    TameBackedAttributeNode.prototype.getElementsByTagName =
    TameBackedAttributeNode.prototype.getElementsByClassName =
    TameBackedAttributeNode.prototype.getChildNodes___ =
    TameBackedAttributeNode.prototype.getAttributes___ = function () {
      throw new Error ("Not implemented.");
    };
    TameBackedAttributeNode.prototype.toString = 
        ___.markFuncFreeze(function () {
          return '[Fake attribute node]';
        });

    // Register set handlers for onclick, onmouseover, etc.
    function registerElementScriptAttributeHandlers(aTameElement) {
      var attrNameRe = /::(.*)/;
      for (var html4Attrib in html4.ATTRIBS) {
        if (html4.atype.SCRIPT === html4.ATTRIBS[html4Attrib]) {
          (function (attribName) {
            ___.useSetHandler(
                aTameElement,
                attribName,
                function eventHandlerSetter(listener) {
                  if (!this.editable___) { throw new Error(NOT_EDITABLE); }
                  if (!listener) {  // Clear the current handler
                    this.node___[attribName] = null;
                  } else {
                    // This handler cannot be copied from one node to another
                    // which is why getters are not yet supported.
                    this.node___[attribName] = makeEventHandlerWrapper(
                        this.node___, listener);
                  }
                  return listener;
                });
           })(html4Attrib.match(attrNameRe)[1]);
        }
      }
    }

    function TameElement(node, editable, childrenEditable) {
      assert(node.nodeType === 1);
      TameBackedNode.call(this, node, editable, childrenEditable);
      classUtils.exportFields(
          this,
          ['className', 'id', 'innerHTML', 'tagName', 'style',
           'offsetParent', 'title', 'dir', 'innerText', 'textContent']);
      classUtils.applyAccessors(this, commonElementPropertyHandlers);
      registerElementScriptAttributeHandlers(this);
    }
    nodeClasses.Element = inertCtor(TameElement, TameBackedNode, 'HTMLElement');
    TameElement.prototype.blur = function () {
      this.node___.blur();
    };
    TameElement.prototype.focus = function () {
      if (imports.isProcessingEvent___) {
        this.node___.focus();
      }
    };
    // IE-specific method.  Sets the element that will have focus when the
    // window has focus, without focusing the window.
    if (document.documentElement.setActive) {
      TameElement.prototype.setActive = function () {
        if (imports.isProcessingEvent___) {
          this.node___.setActive();
        }
      };
      ___.grantTypedMethod(TameElement.prototype, 'setActive');
    }
    // IE-specific method.
    if (document.documentElement.hasFocus) {
      TameElement.prototype.hasFocus = function () {
        return this.node___.hasFocus();
      };
      ___.grantTypedMethod(TameElement.prototype, 'hasFocus');
    }
    defAttributeAlias(TameElement, 'id', defaultToEmptyStr, identity);
    TameElement.prototype.getAttribute = function (attribName) {
      attribName = String(attribName).toLowerCase();
      var tagName = this.node___.tagName.toLowerCase();
      var atype = getAttributeType(tagName, attribName);
      if (atype === void 0) {
        // Unrecognized attribute; use virtual map
        if (this.node___.attributes___) {
          return this.node___.attributes___[attribName] || null;
        }
        return null;
      }
      var value = bridal.getAttribute(this.node___, attribName);
      if ('string' !== typeof value) { return value; }
      return virtualizeAttributeValue(atype, value);
    };
    TameElement.prototype.getAttributeNode = function (name) {
      var hostDomNode = this.node___.getAttributeNode(name);
      if (hostDomNode === null) { return null; }
      return new TameBackedAttributeNode(
          hostDomNode, this.editable___, this.node___);
    };
    TameElement.prototype.hasAttribute = function (attribName) {
      attribName = String(attribName).toLowerCase();
      var tagName = this.node___.tagName.toLowerCase();
      var atype = getAttributeType(tagName, attribName);
      if (atype === void 0) {
        // Unrecognized attribute; use virtual map
        return !!(
            this.node___.attributes___ &&
            ___.hasOwnProp(this.node___.attributes___, attribName));
      } else {
        return bridal.hasAttribute(this.node___, attribName);
      }
    };
    TameElement.prototype.setAttribute = function (attribName, value) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      attribName = String(attribName).toLowerCase();
      var tagName = this.node___.tagName.toLowerCase();
      var atype = getAttributeType(tagName, attribName);
      if (atype === void 0) {
        // Unrecognized attribute; use virtual map
        if (!this.node___.attributes___) { this.node___.attributes___ = {}; }
        this.node___.attributes___[attribName] = String(value);
      } else {
        var sanitizedValue = rewriteAttribute(
            tagName, attribName, atype, value);
        if (sanitizedValue !== null) {
          bridal.setAttribute(this.node___, attribName, sanitizedValue);
        }
      }
      return value;
    };
    TameElement.prototype.removeAttribute = function (attribName) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      attribName = String(attribName).toLowerCase();
      var tagName = this.node___.tagName.toLowerCase();
      var atype = getAttributeType(tagName, attribName);
      if (atype === void 0) {
        // Unrecognized attribute; use virtual map
        if (this.node___.attributes___) {
          delete this.node___.attributes___[attribName];
        }
      } else {
        this.node___.removeAttribute(attribName);
      }
    };
    function simpleProp(o, name, value) {
      o.DefineOwnProperty___(name, {
        value: value,
        writable: true,
        enumerable: true,
        configurable: true
      });
    }
    TameElement.prototype.getBoundingClientRect = function () {
      var elRect = bridal.getBoundingClientRect(this.node___);
      var vbody = bridal.getBoundingClientRect(
          this.getOwnerDocument___().body___);
      var vbodyLeft = vbody.left, vbodyTop = vbody.top;
      var r = {};
      simpleProp(r, 'top',    elRect.top    - vbodyTop);
      simpleProp(r, 'left',   elRect.left   - vbodyLeft);
      simpleProp(r, 'right',  elRect.right  - vbodyLeft);
      simpleProp(r, 'bottom', elRect.bottom - vbodyTop);
      return r;
    };
    TameElement.prototype.getClassName___ = function () {
      return this.getAttribute('class') || '';
    };
    TameElement.prototype.setClassName___ = function (classes) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      return this.setAttribute('class', String(classes));
    };
    function defaultToEmptyStr(x) { return x || ''; }
    defAttributeAlias(TameElement, 'title', defaultToEmptyStr, String);
    defAttributeAlias(TameElement, 'dir', defaultToEmptyStr, String);
    function innerTextOf(rawNode, out) {
      switch (rawNode.nodeType) {
        case 1:  // Element
          var tagName = rawNode.tagName.toLowerCase();
          if (html4.ELEMENTS.hasOwnProperty(tagName)
              && !(html4.ELEMENTS[tagName] & html4.eflags.UNSAFE)) {
            // Not an opaque node.
            for (var c = rawNode.firstChild; c; c = c.nextSibling) {
              innerTextOf(c, out);
            }
          }
          break;
        case 3:  // Text Node
        case 4:  // CDATA Section Node
          out[out.length] = rawNode.data;
          break;
        case 11:  // Document Fragment
          for (var c = rawNode.firstChild; c; c = c.nextSibling) {
            innerTextOf(c, out);
          }
          break;
      }
    }
    TameElement.prototype.getTextContent___
        = TameElement.prototype.getInnerText___
        = function () {
      var text = [];
      innerTextOf(this.node___, text);
      return text.join('');
    };
    TameElement.prototype.setTextContent___
        = TameElement.prototype.setInnerText___
        = function (newText) {
      // This operation changes the child node list (but not other properties
      // of the element) so it checks childrenEditable. Note that this check is
      // critical to security, as else a client can set the innerHTML of
      // a <script> element to execute scripts.
      if (!this.childrenEditable___) { throw new Error(NOT_EDITABLE); }
      var newTextStr = newText != null ? String(newText) : '';
      var el = this.node___;
      for (var c; (c = el.firstChild);) { el.removeChild(c); }
      if (newTextStr) {
        el.appendChild(el.ownerDocument.createTextNode(newTextStr));
      }
      return newText;
    };
    TameElement.prototype.getTagName___
        = TameBackedNode.prototype.getNodeName___;
    TameElement.prototype.getInnerHTML___ = function () {
      var tagName = this.node___.tagName.toLowerCase();
      if (!html4.ELEMENTS.hasOwnProperty(tagName)) {
        return '';  // unknown node
      }
      var flags = html4.ELEMENTS[tagName];
      var innerHtml = this.node___.innerHTML;
      if (flags & html4.eflags.CDATA) {
        innerHtml = html.escapeAttrib(innerHtml);
      } else if (flags & html4.eflags.RCDATA) {
        // Make sure we return PCDATA.
        // For RCDATA we only need to escape & if they're not part of an entity.
        innerHtml = html.normalizeRCData(innerHtml);
      } else {
        // If we blessed the resulting HTML, then this would round trip better
        // but it would still not survive appending, and it would propagate
        // event handlers where the setter of innerHTML does not expect it to.
        innerHtml = tameInnerHtml(innerHtml);
      }
      return innerHtml;
    };
    TameElement.prototype.setInnerHTML___ = function (htmlFragment) {
      // This operation changes the child node list (but not other properties
      // of the element) so it checks childrenEditable. Note that this check is
      // critical to security, as else a client can set the innerHTML of
      // a <script> element to execute scripts.
      if (!this.childrenEditable___) { throw new Error(NOT_EDITABLE); }
      var tagName = this.node___.tagName.toLowerCase();
      if (!html4.ELEMENTS.hasOwnProperty(tagName)) { throw new Error(); }
      var flags = html4.ELEMENTS[tagName];
      if (flags & html4.eflags.UNSAFE) { throw new Error(); }
      var isRcData = flags & html4.eflags.RCDATA;
      var htmlFragmentString;
      if (!isRcData && htmlFragment instanceof Html) {
        htmlFragmentString = '' + safeHtml(htmlFragment);
      } else if (htmlFragment === null) {
        htmlFragmentString = '';
      } else {
        htmlFragmentString = '' + htmlFragment;
      }
      var sanitizedHtml;
      if (isRcData) {
        sanitizedHtml = html.normalizeRCData(htmlFragmentString);
      } else {
        sanitizedHtml = sanitizeHtml(htmlFragmentString);
      }
      this.node___.innerHTML = sanitizedHtml;
      return htmlFragment;
    };
    function identity(x) { return x; }
    defProperty(
        TameElement, 'style',
        false, 
        function (styleNode) {
          return new TameStyle(styleNode, this.editable___, this);
        },
        true, identity);
    TameElement.prototype.updateStyle = function (style) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      var cssPropertiesAndValues = cssSealerUnsealerPair.unseal(style);
      if (!cssPropertiesAndValues) { throw new Error(); }

      var styleNode = this.node___.style;
      for (var i = 0; i < cssPropertiesAndValues.length; i += 2) {
        var propName = cssPropertiesAndValues[i];
        var propValue = cssPropertiesAndValues[i + 1];
        // If the propertyName differs between DOM and CSS, there will
        // be a semicolon between the two.
        // E.g., 'background-color;backgroundColor'
        // See CssTemplate.toPropertyValueList.
        var semi = propName.indexOf(';');
        if (semi >= 0) { propName = propName.substring(semi + 1); }
        styleNode[propName] = propValue;
      }
    };

    TameElement.prototype.getOffsetParent___ = function () {
      return tameRelatedNode(
          this.node___.offsetParent, this.editable___, defaultTameNode);
    };
    TameElement.prototype.getGeometryDelegate___ = function () {
      return this.node___;
    };
    TameElement.prototype.toString = ___.markFuncFreeze(function () {
      return '<' + this.node___.tagName + '>';
    });
    TameElement.prototype.addEventListener = tameAddEventListener;
    TameElement.prototype.removeEventListener = tameRemoveEventListener;
    ___.all2(
       ___.grantTypedMethod, TameElement.prototype,
       ['addEventListener', 'removeEventListener',
        'blur', 'focus',
        'getAttribute', 'setAttribute',
        'removeAttribute', 'hasAttribute',
        'getAttributeNode',
        'getBoundingClientRect',
        'updateStyle']);

    function TameAElement(node, editable) {
      TameElement.call(this, node, editable, editable);
      classUtils.exportFields(this, ['href']);
    }
    inertCtor(TameAElement, TameElement, 'HTMLAnchorElement');
    defProperty(TameAElement, 'href', false, identity, true, identity);

    // http://dev.w3.org/html5/spec/Overview.html#the-canvas-element
    var TameCanvasElement = (function() {
      function isFont(value) {
        return typeof(value) == "string" && css.properties.font.test(value);
      }
      function isColor(value) {
        // Note: we're testing against the pattern for the CSS "color:"
        // property, but what is actually referenced by the draft canvas spec is
        // the CSS syntactic element <color>, which is why we need to
        // specifically exclude "inherit".
        return typeof(value) == "string" && css.properties.color.test(value) &&
            value !== "inherit";
      }
      var colorNameTable = {
        // http://dev.w3.org/csswg/css3-color/#html4 as cited by
        // http://dev.w3.org/html5/2dcontext/#dom-context-2d-fillstyle
        // TODO(kpreid): avoid duplication with table in CssRewriter.java
        " black":   "#000000",
        " silver":  "#c0c0c0",
        " gray":    "#808080",
        " white":   "#ffffff",
        " maroon":  "#800000",
        " red":     "#ff0000",
        " purple":  "#800080",
        " fuchsia": "#ff00ff",
        " green":   "#008000",
        " lime":    "#00ff00",
        " olive":   "#808000",
        " yellow":  "#ffff00",
        " navy":    "#000080",
        " blue":    "#0000ff",
        " teal":    "#008080",
        " aqua":    "#00ffff"
      };
      function StringTest(strings) {
        var table = {};
        // The table itself as a value is a marker to avoid running into
        // Object.prototype properties.
        for (var i = strings.length; --i >= 0;) { table[strings[i]] = table; }
        return ___.markFuncFreeze(function (string) {
          return typeof string === 'string' && table[string] === table;
        });
      }
      function canonColor(colorString) {
        // http://dev.w3.org/html5/2dcontext/ says the color shall be returned
        // only as #hhhhhh, not as names.
        return colorNameTable[" " + colorString] || colorString;
      }
      function TameImageData(imageData) {
        // Since we can't interpose indexing, we can't wrap the CanvasPixelArray
        // so we have to copy the pixel data. This is horrible, bad, and awful.
        var tameImageData = {
          toString: ___.markFuncFreeze(function () {
              return "[Domita Canvas ImageData]"; }),
          width: ___.enforceType(imageData.width, "number"),
          height: ___.enforceType(imageData.height, "number"),
        
          // used to unwrap for passing to putImageData
          bare___: imageData,
        
           // lazily constructed tame copy, backs .data accessor; also used to
           // test whether we need to write-back the copy before a putImageData
          tamePixelArray___: undefined
        };
        ___.tamesTo(imageData, tameImageData);
        TameImageDataMark.stamp.mark___(tameImageData);
        classUtils.applyAccessors(tameImageData, {
          data: {
            // Accessor used so we don't need to copy if the client is just
            // blitting (getImageData -> putImageData) rather than inspecting
            // the pixels.
            get: ___.markFuncFreeze(function () {
              if (!tameImageData.tamePixelArray___) {
                var bareArray = imageData.data;
                var length = bareArray.length;
                var tamePixelArray = { // not frozen, user-modifiable
                  // TODO: Investigate whether it would be an optimization to
                  // make this an array with properties added.
                  toString: ___.markFuncFreeze(function () {
                      return "[Domita CanvasPixelArray]"; }),
                  canvas_writeback___: function () {
                    // This is invoked just before each putImageData
                    for (var i = length-1; i >= 0; i--) {
                      bareArray[i] = tamePixelArray[i];
                    }
                  }
                };
                for (var i = length-1; i >= 0; i--) {
                  tamePixelArray[i] = bareArray[i];
                }
                tameImageData.tamePixelArray___ = tamePixelArray;
              }
              return tameImageData.tamePixelArray___;
            })
          }
        });
        return ___.primFreeze(tameImageData);
      }
      function TameGradient(gradient) {
        var tameGradient = {
          toString: ___.markFuncFreeze(function () {
              return "[Domita CanvasGradient]"; }),
          addColorStop: ___.markFuncFreeze(function (offset, color) {
            ___.enforceType(offset, 'number', 'color stop offset');
            if (!(0 <= offset && offset <= 1)) {
              throw new Error(INDEX_SIZE_ERROR);
              // TODO(kpreid): should be a DOMException per spec
            }
            if (!isColor(color)) {
              throw new Error("SYNTAX_ERR");
              // TODO(kpreid): should be a DOMException per spec
            }
            gradient.addColorStop(offset, color);
          })
        };
        ___.tamesTo(gradient, tameGradient);
        TameGradientMark.stamp.mark___(tameGradient);
        return ___.primFreeze(tameGradient);
      }
      function enforceFinite(value, name) {
        ___.enforceType(value, 'number', name);
        if (!isFinite(value)) {
          throw new Error("NOT_SUPPORTED_ERR");
          // TODO(kpreid): should be a DOMException per spec
        }
      }

      function TameCanvasElement(node, editable) {
        TameElement.call(this, node, editable, editable);
        classUtils.exportFields(
            this,
            ['height', 'width']);
      
        // helpers for tame context
        var context = node.getContext('2d');
        function tameFloatsOp(count, verb) {
          return ___.markFuncFreeze(function () {
            if (arguments.length !== count) {
              throw new Error(verb + ' takes ' + count + ' args, not ' +
                              arguments.length);
            }
            for (var i = 0; i < count; i++) {
              ___.enforceType(arguments[i], 'number', verb + ' argument ' + i);
            }
            context[verb].apply(context, arguments);
          });
        }
        function tameRectMethod(m, hasResult) {
          return ___.markFuncFreeze(function (x, y, w, h) {
            if (arguments.length !== 4) {
              throw new Error(m + ' takes 4 args, not ' +
                              arguments.length);
            }
            ___.enforceType(x, 'number', 'x');
            ___.enforceType(y, 'number', 'y');
            ___.enforceType(w, 'number', 'width');
            ___.enforceType(h, 'number', 'height');
            if (hasResult) {
              return m.call(context, x, y, w, h);            
            } else {
              m.call(context, x, y, w, h);
            }
          });
        }
        function tameDrawText(m) {
          return ___.markFuncFreeze(function (text, x, y, maxWidth) {
            ___.enforceType(text, 'string', 'text');
            ___.enforceType(x, 'number', 'x');
            ___.enforceType(y, 'number', 'y');
            switch (arguments.length) {
            case 3:
              m.apply(context, arguments);
              return;
            case 4:
              ___.enforceType(maxWidth, 'number', 'maxWidth');
              m.apply(context, arguments);
              return;
            default:
              throw new Error(m + ' cannot accept ' + arguments.length +
                                  ' arguments');
            }
          });
        }
        function tameGetMethod(prop) {
          return ___.markFuncFreeze(function () { return context[prop]; });
        }
        function tameSetMethod(prop, validator) {
          return ___.markFuncFreeze(function (newValue) {
            if (validator(newValue)) {
              context[prop] = newValue;
            }
            return newValue;
          });
        }
        function tameGetStyleMethod(prop) {
          return ___.markFuncFreeze(function () {
            var value = context[prop];
            if (typeof(value) == "string") {
              return canonColor(value);
            } else if (___.passesGuard(TameGradientT, ___.tame(value))) {
              return ___.tame(value);
            } else {
              throw("Internal: Can't tame value " + value + " of " + prop);
            }
          });
        }
        function tameSetStyleMethod(prop) {
          return ___.markFuncFreeze(function (newValue) {
            if (isColor(newValue)) {
              context[prop] = newValue;
            } else if (typeof(newValue) === "object" &&
                       ___.passesGuard(TameGradientT, newValue)) {
              context[prop] = ___.untame(newValue);
            } // else do nothing
            return newValue;
          });
        }
        function tameSimpleOp(m) {  // no return value
          return ___.markFuncFreeze(function () {
            if (arguments.length !== 0) {
              throw new Error(m + ' takes no args, not ' + arguments.length);
            }
            m.call(context);
          });
        }
      
        // Design note: We generally reject the wrong number of arguments,
        // unlike default JS behavior. This is because we are just passing data
        // through to the underlying implementation, but we don't want to pass
        // on anything which might be an extension we don't know about, and it
        // is better to fail explicitly than to leave the client wondering about
        // why their extension usage isn't working.
      
        // http://dev.w3.org/html5/2dcontext/
        this.tameContext2d___ = {
          toString: ___.markFuncFreeze(function () {
              return "[Domita CanvasRenderingContext2D]"; }),

          save: tameSimpleOp(context.save),
          restore: tameSimpleOp(context.restore),
        
          scale: tameFloatsOp(2, 'scale'),
          rotate: tameFloatsOp(1, 'rotate'),
          translate: tameFloatsOp(2, 'translate'),
          transform: tameFloatsOp(6, 'transform'),
          setTransform: tameFloatsOp(6, 'setTransform'),
        
          createLinearGradient: ___.markFuncFreeze(function (x0, y0, x1, y1) {
            if (arguments.length !== 4) {
              throw new Error('createLinearGradient takes 4 args, not ' +
                              arguments.length);
            }
            ___.enforceType(x0, 'number', 'x0');
            ___.enforceType(y0, 'number', 'y0');
            ___.enforceType(x1, 'number', 'x1');
            ___.enforceType(y1, 'number', 'y1');
            return TameGradient(context.createLinearGradient(x0, y0, x1, y1));
          }),
          createRadialGradient: ___.markFuncFreeze(function (x0, y0, r0, x1, y1, r1) {
            if (arguments.length !== 6) {
              throw new Error('createRadialGradient takes 6 args, not ' +
                              arguments.length);
            }
            ___.enforceType(x0, 'number', 'x0');
            ___.enforceType(y0, 'number', 'y0');
            ___.enforceType(r0, 'number', 'r0');
            ___.enforceType(x1, 'number', 'x1');
            ___.enforceType(y1, 'number', 'y1');
            ___.enforceType(r1, 'number', 'r1');
            return TameGradient(context.createRadialGradient(x0, y0, r0, x1, y1, r1));
          }),

          createPattern: ___.markFuncFreeze(function (imageElement, repetition) {
            // Consider what policy to have wrt reading the pixels from image
            // elements before implementing this.
            throw new Error('Domita: canvas createPattern not yet implemented');
          }),
        
                
          clearRect:  tameRectMethod(context.clearRect,  false),
          fillRect:   tameRectMethod(context.fillRect,   false),
          strokeRect: tameRectMethod(context.strokeRect, false),

          beginPath: tameSimpleOp(context.beginPath),
          closePath: tameSimpleOp(context.closePath),
          moveTo: tameFloatsOp(2, 'moveTo'),
          lineTo: tameFloatsOp(2, 'lineTo'),
          quadraticCurveTo: tameFloatsOp(4, 'quadraticCurveTo'),
          bezierCurveTo: tameFloatsOp(6, 'bezierCurveTo'),
          arcTo: tameFloatsOp(5, 'arcTo'),
          rect: tameFloatsOp(4, 'rect'),
          arc: ___.markFuncFreeze(function (x, y, radius, startAngle, endAngle,
                                            anticlockwise) {
            if (arguments.length !== 6) {
              throw new Error('arc takes 6 args, not ' + arguments.length);
            }
            ___.enforceType(x, 'number', 'x');
            ___.enforceType(y, 'number', 'y');
            ___.enforceType(radius, 'number', 'radius');
            ___.enforceType(startAngle, 'number', 'startAngle');
            ___.enforceType(endAngle, 'number', 'endAngle');
            ___.enforceType(anticlockwise, 'boolean', 'anticlockwise');
            if (radius < 0) {
              throw new Error(INDEX_SIZE_ERROR);
              // TODO(kpreid): should be a DOMException per spec
            }
            context.arc(x, y, radius, startAngle, endAngle, anticlockwise);
          }),
          fill: tameSimpleOp(context.fill),
          stroke: tameSimpleOp(context.stroke),
          clip: tameSimpleOp(context.clip),
        
          isPointInPath: ___.markFuncFreeze(function (x, y) {
            ___.enforceType(x, 'number', 'x');
            ___.enforceType(y, 'number', 'y');
            return ___.enforceType(context.isPointInPath(x, y), 'boolean');
          }),
        
          fillText: tameDrawText(context.fillText),
          strokeText: tameDrawText(context.strokeText),
          measureText: ___.markFuncFreeze(function (string) {
            if (arguments.length !== 1) {
              throw new Error('measureText takes 1 arg, not ' + arguments.length);
            }
            ___.enforceType(string, 'string', 'measureText argument');
            return context.measureText(string);
          }),
        
          drawImage: ___.markFuncFreeze(function (imageElement) {
            // Consider what policy to have wrt reading the pixels from image
            // elements before implementing this.
            throw new Error('Domita: canvas drawImage not yet implemented');
          }),
        
          createImageData: ___.markFuncFreeze(function (sw, sh) {
            if (arguments.length !== 2) {
              throw new Error('createImageData takes 2 args, not ' +
                              arguments.length);
            }
            ___.enforceType(sw, 'number', 'sw');
            ___.enforceType(sh, 'number', 'sh');
            return TameImageData(context.createImageData(sw, sh));
          }),
          getImageData: tameRectMethod(function (sx, sy, sw, sh) {
            return TameImageData(context.getImageData(sx, sy, sw, sh));
          }, true),
          putImageData: ___.markFuncFreeze(function 
              (tameImageData, dx, dy, dirtyX, dirtyY, dirtyWidth, dirtyHeight) {
            tameImageData = TameImageDataT.coerce(tameImageData);
            enforceFinite(dx, 'dx');
            enforceFinite(dy, 'dy');
            switch (arguments.length) {
            case 3:
              dirtyX = 0;
              dirtyY = 0;
              dirtyWidth = tameImageData.width;
              dirtyHeight = tameImageData.height;
              break;
            case 7:
              enforceFinite(dirtyX, 'dirtyX');
              enforceFinite(dirtyY, 'dirtyY');
              enforceFinite(dirtyWidth, 'dirtyWidth');
              enforceFinite(dirtyHeight, 'dirtyHeight');
              break;
            default:
              throw 'putImageData cannot accept ' + arguments.length +
                  ' arguments';
            }
            if (tameImageData.tamePixelArray___) {
              tameImageData.tamePixelArray___.canvas_writeback___();
            }
            context.putImageData(tameImageData.bare___, dx, dy, dirtyX, dirtyY,
                                 dirtyWidth, dirtyHeight);
          })
        };
      
        if ("drawFocusRing" in context) {
          this.tameContext2d___.drawFocusRing = ___.markFuncFreeze(function
              (tameElement, x, y, canDrawCustom) {
            switch (arguments.length) {
            case 3:
              canDrawCustom = false;
              break;
            case 4:
              break;
            default:
              throw 'drawFocusRing cannot accept ' + arguments.length +
                  ' arguments';
            }
            tameElement = TameNodeT.coerce(tameElement);
            ___.enforceType(x, 'number', 'x');
            ___.enforceType(y, 'number', 'y');
            ___.enforceType(canDrawCustom, 'boolean', 'canDrawCustom');
          
            // On safety of using the untamed node here: The only information
            // drawFocusRing takes from the node is whether it is focused.
            return ___.enforceType(
                context.drawFocusRing(tameElement.node___, x, y, canDrawCustom),
                'boolean');
          });
        }
      
        classUtils.applyAccessors(this.tameContext2d___, {
          // We filter the values supplied to setters in case some browser
          // extension makes them more powerful, e.g. containing scripting or a
          // URL.
          // TODO(kpreid): Do we want to filter the *getters* as well? Scenarios:
          // (a) canvas shared with innocent code, (b) browser quirks?? If we do,
          // then what should be done with a bad value?
          globalAlpha: {
            get: tameGetMethod('globalAlpha'),
            set: tameSetMethod('globalAlpha',
                function (v) { return typeof v === "number" &&
                                      0.0 <= v && v <= 1.0;     })
          },
          globalCompositeOperation: {
            get: tameGetMethod('globalCompositeOperation'),
            set: tameSetMethod('globalCompositeOperation',
                StringTest([
                  "source-atop",
                  "source-in",
                  "source-out",
                  "source-over",
                  "destination-atop",
                  "destination-in",
                  "destination-out",
                  "destination-over",
                  "lighter",
                  "copy",
                  "xor"
                ]))
          },
          strokeStyle: {
            get: tameGetStyleMethod('strokeStyle'),
            set: tameSetStyleMethod('strokeStyle')
          },
          fillStyle: {
            get: tameGetStyleMethod('fillStyle'),
            set: tameSetStyleMethod('fillStyle')
          },
          lineWidth: {
            get: tameGetMethod('lineWidth'),
            set: tameSetMethod('lineWidth',
                function (v) { return typeof v === "number" &&
                                      0.0 < v && v !== Infinity; })
          },
          lineCap: {
            get: tameGetMethod('lineCap'),
            set: tameSetMethod('lineCap', StringTest([
              "butt",
              "round",
              "square"
            ]))
          },
          lineJoin: {
            get: tameGetMethod('lineJoin'),
            set: tameSetMethod('lineJoin', StringTest([
              "bevel",
              "round",
              "miter"
            ]))
          },
          miterLimit: {
            get: tameGetMethod('miterLimit'),
            set: tameSetMethod('miterLimit',
                function (v) { return typeof v === "number" &&
                                      0 < v && v !== Infinity; })
          },
          shadowOffsetX: {
            get: tameGetMethod('shadowOffsetX'),
            set: tameSetMethod('shadowOffsetX',
                function (v) { return typeof v === "number" && isFinite(v); })
          },
          shadowOffsetY: {
            get: tameGetMethod('shadowOffsetY'),
            set: tameSetMethod('shadowOffsetY',
                function (v) { return typeof v === "number" && isFinite(v); })
          },
          shadowBlur: {
            get: tameGetMethod('shadowBlur'),
            set: tameSetMethod('shadowBlur',
                function (v) { return typeof v === "number" &&
                                      0.0 <= v && v !== Infinity; })
          },
          shadowColor: {
            get: tameGetStyleMethod('shadowColor'),
            set: tameSetMethod('shadowColor', isColor)
          },
        
          font: {
            get: tameGetMethod('font'),
            set: tameSetMethod('font', isFont)
          },
          textAlign: {
            get: tameGetMethod('textAlign'),
            set: tameSetMethod('textAlign', StringTest([
              "start",
              "end",
              "left",
              "right",
              "center"
            ]))
          },
          textBaseline: {
            get: tameGetMethod('textBaseline'),
            set: tameSetMethod('textBaseline', StringTest([
              "top",
              "hanging",
              "middle",
              "alphabetic",
              "ideographic",
              "bottom"
            ]))
          }
        });
        ___.primFreeze(this.tameContext2d___);
      }  // end of TameCanvasElement
      inertCtor(TameCanvasElement, TameElement, 'HTMLCanvasElement');
      TameCanvasElement.prototype.getContext = function (contextId) {
      
        // TODO(kpreid): We can refine this by inventing a ReadOnlyCanvas object
        // to return in this situation, which allows getImageData and so on but
        // not any drawing. Not bothering to do that for now; if you have a use
        // for it let us know.
        if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      
        ___.enforceType(contextId, 'string', 'contextId');
        switch (contextId) {
          case '2d':
            return this.tameContext2d___;
          default:
            // http://dev.w3.org/html5/spec/the-canvas-element.html#the-canvas-element
            // says: The getContext(contextId, args...) method of the canvas
            // element, when invoked, must run the following steps:
            // [...]
            //     If contextId is not the name of a context supported by the
            //     user agent, return null and abort these steps.
            //
            // However, Mozilla throws and WebKit returns undefined instead.
            // Returning undefined rather than null is closer to the spec than
            // throwing.
            return undefined;
            throw new Error('Unapproved canvas contextId');
        }
      };
      defProperty(TameCanvasElement, 'height', false, identity, false, Number);
      defProperty(TameCanvasElement, 'width', false, identity, false, Number);
      ___.all2(___.grantTypedMethod, TameCanvasElement.prototype,
               ['getContext']);
      
      return TameCanvasElement;
    })();

    // http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-40002357
    function TameFormElement(node, editable) {
      TameElement.call(this, node, editable, editable);
      this.length = node.length;
      classUtils.exportFields(
          this, ['action', 'elements', 'enctype', 'method', 'target']);
    }
    inertCtor(TameFormElement, TameElement, 'HTMLFormElement');
    TameFormElement.prototype.handleRead___ = function (name) {
      name = String(name);
      if (endsWith__.test(name)) { return void 0; }
      // TODO(ihab.awad): Due to the following bug:
      //     http://code.google.com/p/google-caja/issues/detail?id=997
      // the read handlers get called on the *prototypes* as well as the
      // instances on which they are installed. In that case, we just
      // defer to the super handler, which works for now.
      if (___.passesGuard(TameNodeT, this)) {
        var tameElements = this.getElements___();
        if (___.hasOwnProp(tameElements, name)) { return tameElements[name]; }
      }
      return TameBackedNode.prototype.handleRead___.call(this, name);
    };
    TameFormElement.prototype.submit = function () {
      return this.node___.submit();
    };
    TameFormElement.prototype.reset = function () {
      return this.node___.reset();
    };
    defAttributeAlias(TameFormElement, 'action', defaultToEmptyStr, String);
    TameFormElement.prototype.getElements___ = function () {
      return tameHTMLCollection(
          this.node___.elements, this.editable___, defaultTameNode);
    };
    defAttributeAlias(TameFormElement, 'enctype', defaultToEmptyStr, String);
    defAttributeAlias(TameFormElement, 'method', defaultToEmptyStr, String);
    defAttributeAlias(TameFormElement, 'target', defaultToEmptyStr, String);
    TameFormElement.prototype.reset = function () {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.reset();
    };
    TameFormElement.prototype.submit = function () {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.submit();
    };
    ___.all2(___.grantTypedMethod, TameFormElement.prototype,
             ['reset', 'submit']);


    function TameInputElement(node, editable) {
      TameElement.call(this, node, editable, editable);
      classUtils.exportFields(
          this,
          ['form', 'value', 'defaultValue',
           'checked', 'disabled', 'readOnly',
           'options', 'selected', 'selectedIndex',
           'name', 'accessKey', 'tabIndex', 'text',
           'defaultChecked', 'defaultSelected', 'maxLength',
           'size', 'type', 'index', 'label',
           'multiple', 'cols', 'rows']);
    }
    inertCtor(TameInputElement, TameElement, 'HTMLInputElement');
    defProperty(TameInputElement, 'checked', false, identity, false, Boolean);
    defProperty(
        TameInputElement, 'defaultChecked', false, identity, false, identity);
    defProperty(
        TameInputElement, 'value',
        false, function (x) { return x == null ? null : String(x); },
        false, function (x) { return x == null ? '' : '' + x; });
    defProperty(
        TameInputElement, 'defaultValue',
        false, function (x) { return x == null ? null : String(x); },
        false, function (x) { return x == null ? '' : '' + x; });
    TameInputElement.prototype.select = function () {
      this.node___.select();
    };
    TameInputElement.prototype.getForm___ = function () {
      return tameRelatedNode(
          this.node___.form, this.editable___, defaultTameNode);
    };
    defProperty(TameInputElement, 'disabled', false, identity, false, identity);
    defProperty(TameInputElement, 'readOnly', false, identity, false, identity);
    TameInputElement.prototype.getOptions___ = function () {
      return tameOptionsList(
          this.node___.options, this.editable___, defaultTameNode, 'name');
    };
    defProperty(
        TameInputElement, 'selected', false, identity, false, identity);
    defProperty(
        TameInputElement, 'defaultSelected', false, identity, false, Boolean);
    function toInt(x) { return x | 0; }
    defProperty(
        TameInputElement, 'selectedIndex', false, identity, false, toInt);
    defProperty(TameInputElement, 'name', false, identity, false, identity);
    defProperty(
        TameInputElement, 'accessKey', false, identity, false, identity);
    defProperty(TameInputElement, 'tabIndex', false, identity, false, identity);
    defProperty(TameInputElement, 'text', false, String);
    defProperty(
        TameInputElement, 'maxLength', false, identity, false, identity);
    defProperty(TameInputElement, 'size', false, identity, false, identity);
    defProperty(TameInputElement, 'type', false, identity, false, identity);
    defProperty(TameInputElement, 'index', false, identity, false, identity);
    defProperty(TameInputElement, 'label', false, identity, false, identity);
    defProperty(TameInputElement, 'multiple', false, identity, false, identity);
    defProperty(TameInputElement, 'cols', false, identity, false, identity);
    defProperty(TameInputElement, 'rows', false, identity, false, identity);
    ___.all2(___.grantTypedMethod, TameInputElement.prototype, ['select']);


    function TameImageElement(node, editable) {
      TameElement.call(this, node, editable, editable);
      classUtils.exportFields(this, ['src', 'alt']);
    }
    inertCtor(TameImageElement, TameElement, 'HTMLImageElement');
    defProperty(TameImageElement, 'src', false, identity, true, identity);
    defProperty(TameImageElement, 'alt', false, identity, false, String);

    function TameLabelElement(node, editable) {
      TameElement.call(this, node, editable, editable);
      classUtils.exportFields(this, ['htmlFor']);
    }
    inertCtor(TameLabelElement, TameElement, 'HTMLLabelElement');
    TameLabelElement.prototype.getHtmlFor___ = function () {
      return this.getAttribute('for');
    };
    TameLabelElement.prototype.setHtmlFor___ = function (id) {
      this.setAttribute('for', id);
      return id;
    };

    /**
     * A script element wrapper that allows setting of a src that has been
     * rewritten by a URI policy, but not modifying of textual content.
     */
    function TameScriptElement(node, editable) {
      // Make the child list immutable so that text content can't be added
      // or removed.
      TameElement.call(this, node, editable, false);
      classUtils.exportFields(this, ['src']);
    }
    inertCtor(TameScriptElement, TameElement, 'HTMLScriptElement');
    defProperty(TameScriptElement, 'src', false, identity, true, identity);

    function TameIFrameElement(node, editable) {
      // Make the child list immutable so that text content can't be added
      // or removed.
      TameElement.call(this, node, editable, false);
      classUtils.exportFields(
          this,
          ['align', 'frameBorder', 'height', 'width']);
    }
    inertCtor(TameIFrameElement, TameElement, "HTMLIFrameElement");
    TameIFrameElement.prototype.getAlign___ = function () {
      return this.node___.align;
    };
    TameIFrameElement.prototype.setAlign___ = function (alignment) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      alignment = String(alignment);
      if (alignment === 'left' ||
          alignment === 'right' ||
          alignment === 'center') {
        this.node___.align = alignment;
      }
    };
    TameIFrameElement.prototype.getAttribute = function(attr) {
      var attrLc = String(attr).toLowerCase();
      if (attrLc !== 'name' && attrLc !== 'src') {
        return TameElement.prototype.getAttribute.call(this, attr);
      }
      return null;
    };
    TameIFrameElement.prototype.setAttribute = function(attr, value) {
      var attrLc = String(attr).toLowerCase();
      // The 'name' and 'src' attributes are whitelisted for all tags in
      // html4-attributes-whitelist.json, since they're needed on tags
      // like <img>.  Because there's currently no way to filter attributes
      // based on the tag, we have to blacklist these two here.
      if (attrLc !== 'name' && attrLc !== 'src') {
        return TameElement.prototype.setAttribute.call(this, attr, value);
      }
      ___.log('Cannot set the [' + attrLc + '] attribute of an iframe.');
      return value;
    };
    TameIFrameElement.prototype.getFrameBorder___ = function () {
      return this.node___.frameBorder;
    };
    TameIFrameElement.prototype.setFrameBorder___ = function (border) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      border = String(border).toLowerCase();
      if (border === '0' || border === '1' ||
          border === 'no' || border === 'yes') {
        this.node___.frameBorder = border;
      }
    };
    defProperty(TameIFrameElement, 'height', false, identity, false, Number);
    defProperty(TameIFrameElement, 'width', false, identity, false, Number);
    TameIFrameElement.prototype.handleRead___ = function (name) {
      var nameLc = String(name).toLowerCase();
      if (nameLc !== 'src' && nameLc !== 'name') {
        return TameElement.prototype.handleRead___.call(this, name);
      }
      return undefined;
    };
    TameIFrameElement.prototype.handleSet___ = function (name, value) {
      var nameLc = String(name).toLowerCase();
      if (nameLc !== 'src' && nameLc !== 'name') {
        return TameElement.prototype.handleSet___.call(this, name, value);
      }
      ___.log('Cannot set the [' + nameLc + '] property of an iframe.');
      return value;
    };
    ___.all2(___.grantTypedMethod, TameIFrameElement.prototype,
             ['getAttribute', 'setAttribute']);



    function TameTableCompElement(node, editable) {
      TameElement.call(this, node, editable, editable);
      classUtils.exportFields(
          this,
          ['colSpan', 'cells', 'cellIndex', 'rowSpan', 'rows', 'rowIndex',
           'align', 'vAlign', 'nowrap', 'sectionRowIndex']);
    }
    ___.extend(TameTableCompElement, TameElement);
    defProperty(
        TameTableCompElement, 'colSpan', false, identity, false, identity);
    TameTableCompElement.prototype.getCells___ = function () {
      return tameNodeList(
          this.node___.cells, this.editable___, defaultTameNode);
    };
    TameTableCompElement.prototype.getCellIndex___ = function () {
      return this.node___.cellIndex;
    };
    defProperty(
        TameTableCompElement, 'rowSpan', false, identity, false, identity);
    TameTableCompElement.prototype.getRows___ = function () {
      return tameNodeList(this.node___.rows, this.editable___, defaultTameNode);
    };
    TameTableCompElement.prototype.getRowIndex___ = function () {
      return this.node___.rowIndex;
    };
    TameTableCompElement.prototype.getSectionRowIndex___ = function () {
      return this.node___.sectionRowIndex;
    };
    defProperty(
        TameTableCompElement, 'align', false, identity, false, identity);
    defProperty(
        TameTableCompElement, 'vAlign', false, identity, false, identity);
    defProperty(
        TameTableCompElement, 'nowrap', false, identity, false, identity);
    TameTableCompElement.prototype.insertRow = function (index) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      requireIntIn(index, -1, this.node___.rows.length);
      return defaultTameNode(this.node___.insertRow(index), this.editable___);
    };
    TameTableCompElement.prototype.deleteRow = function (index) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      requireIntIn(index, -1, this.node___.rows.length);
      this.node___.deleteRow(index);
    };
    ___.all2(___.grantTypedMethod, TameTableCompElement.prototype,
             ['insertRow', 'deleteRow']);

    function requireIntIn(idx, min, max) {
      if (idx !== (idx | 0) || idx < min || idx > max) {
        throw new Error(INDEX_SIZE_ERROR);
      }
    }

    function TameTableRowElement(node, editable) {
      TameTableCompElement.call(this, node, editable);
    }
    inertCtor(TameTableRowElement, TameTableCompElement, 'HTMLTableRowElement');
    TameTableRowElement.prototype.insertCell = function (index) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      requireIntIn(index, -1, this.node___.cells.length);
      return defaultTameNode(
          this.node___.insertCell(index),
          this.editable___);
    };
    TameTableRowElement.prototype.deleteCell = function (index) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      requireIntIn(index, -1, this.node___.cells.length);
      this.node___.deleteCell(index);
    };
    ___.all2(___.grantTypedMethod, TameTableRowElement.prototype,
             ['insertCell', 'deleteCell']);

    function TameTableElement(node, editable) {
      TameTableCompElement.call(this, node, editable);
      classUtils.exportFields(
          this,
          ['tBodies', 'tHead', 'tFoot', 'cellPadding', 'cellSpacing', 'border']
          );
    }
    inertCtor(TameTableElement, TameTableCompElement, 'HTMLTableElement');
    TameTableElement.prototype.getTBodies___ = function () {
      return tameNodeList(
          this.node___.tBodies, this.editable___, defaultTameNode);
    };
    TameTableElement.prototype.getTHead___ = function () {
      return defaultTameNode(this.node___.tHead, this.editable___);
    };
    TameTableElement.prototype.getTFoot___ = function () {
      return defaultTameNode(this.node___.tFoot, this.editable___);
    };
    TameTableElement.prototype.createTHead = function () {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      return defaultTameNode(this.node___.createTHead(), this.editable___);
    };
    TameTableElement.prototype.deleteTHead = function () {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.deleteTHead();
    };
    TameTableElement.prototype.createTFoot = function () {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      return defaultTameNode(this.node___.createTFoot(), this.editable___);
    };
    TameTableElement.prototype.deleteTFoot = function () {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.deleteTFoot();
    };
    TameTableElement.prototype.createCaption = function () {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      return defaultTameNode(this.node___.createCaption(), this.editable___);
    };
    TameTableElement.prototype.deleteCaption = function () {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.deleteCaption();
    };
    TameTableElement.prototype.insertRow = function (index) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      requireIntIn(index, -1, this.node___.rows.length);
      return defaultTameNode(this.node___.insertRow(index), this.editable___);
    };
    TameTableElement.prototype.deleteRow = function (index) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      requireIntIn(index, -1, this.node___.rows.length);
      this.node___.deleteRow(index);
    };
    function fromInt(x) { return '' + (x | 0); }  // coerce null and false to 0
    defAttributeAlias(TameTableElement, 'cellPadding', Number, fromInt);
    defAttributeAlias(TameTableElement, 'cellSpacing', Number, fromInt);
    defAttributeAlias(TameTableElement, 'border', Number, fromInt);

    ___.all2(___.grantTypedMethod, TameTableElement.prototype,
             ['createTHead', 'deleteTHead', 'createTFoot', 'deleteTFoot',
              'createCaption', 'deleteCaption', 'insertRow', 'deleteRow']);

    function tameEvent(event) {
      if (event.TAMED_TWIN___) { return event.TAMED_TWIN___; }
      return new TameEvent(event);
    }

    function TameEvent(event) {
      assert(!!event);
      this.event___ = this.FERAL_TWIN___ = event;
      event.TAMED_TWIN___ = this;
      TameEventMark.stamp.mark___(this);
      classUtils.exportFields(
          this,
          ['type', 'target', 'pageX', 'pageY', 'altKey',
           'ctrlKey', 'metaKey', 'shiftKey', 'button',
           'screenX', 'screenY',
           'currentTarget', 'relatedTarget',
           'fromElement', 'toElement',
           'srcElement',
           'clientX', 'clientY', 'keyCode', 'which']);
    }
    inertCtor(TameEvent, Object, 'Event');
    TameEvent.prototype.getType___ = function () {
      return bridal.untameEventType(String(this.event___.type));
    };
    TameEvent.prototype.getTarget___ = function () {
      var event = this.event___;
      return tameRelatedNode(
          event.target || event.srcElement, true, defaultTameNode);
    };
    TameEvent.prototype.getSrcElement___ = function () {
      return tameRelatedNode(this.event___.srcElement, true, defaultTameNode);
    };
    TameEvent.prototype.getCurrentTarget___ = function () {
      var e = this.event___;
      return tameRelatedNode(e.currentTarget, true, defaultTameNode);
    };
    TameEvent.prototype.getRelatedTarget___ = function () {
      var e = this.event___;
      var t = e.relatedTarget;
      if (!t) {
        if (e.type === 'mouseout') {
          t = e.toElement;
        } else if (e.type === 'mouseover') {
          t = e.fromElement;
        }
      }
      return tameRelatedNode(t, true, defaultTameNode);
    };
    // relatedTarget is read-only.  this dummy setter is because some code
    // tries to workaround IE by setting a relatedTarget when it's not set.
    // code in a sandbox can't tell the difference between "falsey because
    // relatedTarget is not supported" and "falsey because relatedTarget is
    // outside sandbox".
    TameEvent.prototype.setRelatedTarget___ = function (newValue) {
      return newValue;
    };
    TameEvent.prototype.getFromElement___ = function () {
      return tameRelatedNode(this.event___.fromElement, true, defaultTameNode);
    };
    TameEvent.prototype.getToElement___ = function () {
      return tameRelatedNode(this.event___.toElement, true, defaultTameNode);
    };
    TameEvent.prototype.getPageX___ = function () {
      return Number(this.event___.pageX);
    };
    TameEvent.prototype.getPageY___ = function () {
      return Number(this.event___.pageY);
    };
    TameEvent.prototype.stopPropagation = function () {
      // TODO(mikesamuel): make sure event doesn't propagate to dispatched
      // events for this gadget only.
      // But don't allow it to stop propagation to the container.
      if (this.event___.stopPropagation) {
        this.event___.stopPropagation();
      } else {
        this.event___.cancelBubble = true;
      }
    };
    TameEvent.prototype.preventDefault = function () {
      // TODO(mikesamuel): make sure event doesn't propagate to dispatched
      // events for this gadget only.
      // But don't allow it to stop propagation to the container.
      if (this.event___.preventDefault) {
        this.event___.preventDefault();
      } else {
        this.event___.returnValue = false;
      }
    };
    TameEvent.prototype.getAltKey___ = function () {
      return Boolean(this.event___.altKey);
    };
    TameEvent.prototype.getCtrlKey___ = function () {
      return Boolean(this.event___.ctrlKey);
    };
    TameEvent.prototype.getMetaKey___ = function () {
      return Boolean(this.event___.metaKey);
    };
    TameEvent.prototype.getShiftKey___ = function () {
      return Boolean(this.event___.shiftKey);
    };
    TameEvent.prototype.getButton___ = function () {
      var e = this.event___;
      return e.button && Number(e.button);
    };
    TameEvent.prototype.getClientX___ = function () {
      return Number(this.event___.clientX);
    };
    TameEvent.prototype.getClientY___ = function () {
      return Number(this.event___.clientY);
    };
    TameEvent.prototype.getScreenX___ = function () {
      return Number(this.event___.screenX);
    };
    TameEvent.prototype.getScreenY___ = function () {
      return Number(this.event___.screenY);
    };
    TameEvent.prototype.getWhich___ = function () {
      var w = this.event___.which;
      return w && Number(w);
    };
    TameEvent.prototype.getKeyCode___ = function () {
      var kc = this.event___.keyCode;
      return kc && Number(kc);
    };
    TameEvent.prototype.toString = 
        ___.markFuncFreeze(function () { return '[Fake Event]'; });
    ___.all2(___.grantTypedMethod, TameEvent.prototype,
             ['stopPropagation', 'preventDefault']);

    function TameCustomHTMLEvent(event) {
      TameEvent.call(this, event);
      this.properties___ = {};
    }
    ___.extend(TameCustomHTMLEvent, TameEvent);
    TameCustomHTMLEvent.prototype.initEvent
        = function (type, bubbles, cancelable) {
      bridal.initEvent(this.event___, type, bubbles, cancelable);
    };
    TameCustomHTMLEvent.prototype.handleRead___ = function (name) {
      name = String(name);
      if (endsWith__.test(name)) { return void 0; }
      var handlerName = name + '_getter___';
      if (this[handlerName]) {
        return this[handlerName]();
      }
      if (___.hasOwnProp(this.event___.properties___, name)) {
        return this.event___.properties___[name];
      } else {
        return void 0;
      }
    };
    TameCustomHTMLEvent.prototype.handleCall___ = function (name, args) {
      name = String(name);
      if (endsWith__.test(name)) { throw new Error(INVALID_SUFFIX); }
      var handlerName = name + '_handler___';
      if (this[handlerName]) {
        return this[handlerName].call(this, args);
      }
      if (___.hasOwnProp(this.event___.properties___, name)) {
        return this.event___.properties___[name].call(this, args);
      } else {
        throw new TypeError(name + ' is not a function.');
      }
    };
    TameCustomHTMLEvent.prototype.handleSet___ = function (name, val) {
      name = String(name);
      if (endsWith__.test(name)) { throw new Error(INVALID_SUFFIX); }
      var handlerName = name + '_setter___';
      if (this[handlerName]) {
        return this[handlerName](val);
      }
      if (!this.event___.properties___) {
        this.event___.properties___ = {};
      }
      this[name + '_c___'] = this;
      this[name + '_v___'] = false;
      this[name + '_e___'] = this;
      return this.event___.properties___[name] = val;
    };
    TameCustomHTMLEvent.prototype.handleDelete___ = function (name) {
      name = String(name);
      if (endsWith__.test(name)) { throw new Error(INVALID_SUFFIX); }
      var handlerName = name + '_deleter___';
      if (this[handlerName]) {
        return this[handlerName]();
      }
      if (this.event___.properties___) {
        return (
            delete this.event___.properties___[name]
            && delete this[name + '_c___']
            && delete this[name + '_e___']
            && delete this[name + '_v___']);
      } else {
        return true;
      }
    };
    TameCustomHTMLEvent.prototype.handleEnum___ = function (ownFlag) {
      // TODO(metaweta): Add code to list all the other handled stuff we know
      // about.
      if (this.event___.properties___) {
        return ___.allKeys(this.event___.properties___);
      }
      return [];
    };
    TameCustomHTMLEvent.prototype.toString = ___.markFuncFreeze(function () {
      return '[Fake CustomEvent]';
    });
    ___.grantTypedMethod(TameCustomHTMLEvent.prototype, 'initEvent');

    function TameHTMLDocument(doc, body, domain, editable) {
      TamePseudoNode.call(this, editable);
      this.doc___ = doc;
      this.body___ = body;
      this.domain___ = domain;
      this.onLoadListeners___ = [];
      var tameDoc = this;

      var tameBody = defaultTameNode(body, editable);
      this.tameBody___ = tameBody;
      // TODO(mikesamuel): create a proper class for BODY, HEAD, and HTML along
      // with all the other specialized node types.
      var tameBodyElement = new TamePseudoElement(
          'BODY',
          this,
          function () {
            return tameNodeList(body.childNodes, editable, defaultTameNode);
          },
          function () { return tameHtmlElement; },
          function () { return tameInnerHtml(body.innerHTML); },
          tameBody,
          editable);
      ___.forOwnKeys(
          { appendChild: 0, removeChild: 0, insertBefore: 0, replaceChild: 0 },
          ___.markFuncFreeze(function (k) {
            tameBodyElement[k] = tameBody[k].bind(tameBody);
            ___.grantFunc(tameBodyElement, k);
          }));

      var title = doc.createTextNode(body.getAttribute('title') || '');
      var tameTitleElement = new TamePseudoElement(
          'TITLE',
          this,
          function () { return [defaultTameNode(title, false)]; },
          function () { return tameHeadElement; },
          function () { return html.escapeAttrib(title.nodeValue); },
          null,
          editable);
      var tameHeadElement = new TamePseudoElement(
          'HEAD',
          this,
          function () { return [tameTitleElement]; },
          function () { return tameHtmlElement; },
          function () {
            return '<title>' + tameTitleElement.getInnerHTML___() + '</title>';
          },
          null,
          editable);
      var tameHtmlElement = new TamePseudoElement(
          'HTML',
          this,
          function () { return [tameHeadElement, tameBodyElement]; },
          function () { return tameDoc; },
          function () {
            return ('<head>' + tameHeadElement.getInnerHTML___()
                    + '<\/head><body>'
                    + tameBodyElement.getInnerHTML___() + '<\/body>');
          },
          tameBody,
          editable);
      if (body.contains) {  // typeof is 'object' on IE
        tameHtmlElement.contains = function (other) {
          other = TameNodeT.coerce(other);
          var otherNode = other.node___;
          return body.contains(otherNode);
        };
        ___.grantFunc(tameHtmlElement, 'contains');
      }
      if ('function' === typeof body.compareDocumentPosition) {
        /**
         * Speced in <a href="http://www.w3.org/TR/DOM-Level-3-Core/core.html#Node3-compareDocumentPosition">DOM-Level-3</a>.
         */
        tameHtmlElement.compareDocumentPosition = function (other) {
          other = TameNodeT.coerce(other);
          var otherNode = other.node___;
          if (!otherNode) { return 0; }
          var bitmask = +body.compareDocumentPosition(otherNode);
          // To avoid leaking information about the relative positioning of
          // different roots, if neither contains the other, then we mask out
          // the preceding/following bits.
          // 0x18 is (CONTAINS | CONTAINED).
          // 0x1f is all the bits documented at
          // http://www.w3.org/TR/DOM-Level-3-Core/core.html#DocumentPosition
          // except IMPLEMENTATION_SPECIFIC.
          // 0x01 is DISCONNECTED.
          /*
          if (!(bitmask & 0x18)) {
            // TODO: If they are not under the same virtual doc root, return
            // DOCUMENT_POSITION_DISCONNECTED instead of leaking information
            // about PRECEEDED | FOLLOWING.
          }
          */
          return bitmask & 0x1f;
        };
        if (!___.hasOwnProp(tameHtmlElement, 'contains')) {
          // http://www.quirksmode.org/blog/archives/2006/01/contains_for_mo.html
          tameHtmlElement.contains = (function (other) {
            var docPos = this.compareDocumentPosition(other);
            return !(!(docPos & 0x10) && docPos);
          }).bind(tameHtmlElement);
          ___.grantFunc(tameHtmlElement, 'contains');
        }
        ___.grantFunc(tameHtmlElement, 'compareDocumentPosition');
      }
      this.documentElement___ = tameHtmlElement;
      classUtils.exportFields(
          this, ['documentElement', 'body', 'title', 'domain', 'forms',
                 'compatMode']);
    }
    inertCtor(TameHTMLDocument, TamePseudoNode, 'HTMLDocument');
    TameHTMLDocument.prototype.getNodeType___ = function () { return 9; };
    TameHTMLDocument.prototype.getNodeName___
        = function () { return '#document'; };
    TameHTMLDocument.prototype.getNodeValue___ = function () { return null; };
    TameHTMLDocument.prototype.getChildNodes___
        = function () { return [this.documentElement___]; };
    TameHTMLDocument.prototype.getAttributes___ = function () { return []; };
    TameHTMLDocument.prototype.getParentNode___ = function () { return null; };
    TameHTMLDocument.prototype.getElementsByTagName = function (tagName) {
      tagName = String(tagName).toLowerCase();
      switch (tagName) {
        case 'body': return fakeNodeList([ this.getBody___() ]);
        case 'head': return fakeNodeList([ this.getHead___() ]);
        case 'title': return fakeNodeList([ this.getTitle___() ]);
        case 'html': return fakeNodeList([ this.getDocumentElement___() ]);
        default:
          var nodes = tameGetElementsByTagName(
              this.body___, tagName, this.editable___);
          if (tagName === '*') {
            nodes.unshift(this.getBody___());
            nodes.unshift(this.getTitle___());
            nodes.unshift(this.getHead___());
            nodes.unshift(this.getDocumentElement___());
          }
          return nodes;
      }
    };
    TameHTMLDocument.prototype.getDocumentElement___ = function () {
      return this.documentElement___;
    };
    TameHTMLDocument.prototype.getBody___ = function () {
      return this.documentElement___.getLastChild___();
    };
    TameHTMLDocument.prototype.getHead___ = function () {
      return this.documentElement___.getFirstChild___();
    };
    TameHTMLDocument.prototype.getTitle___ = function () {
      return this.getHead___().getFirstChild___();
    };
    TameHTMLDocument.prototype.getDomain___ = function () {
      return this.domain___;
    };
    TameHTMLDocument.prototype.getElementsByClassName = function (className) {
      return tameGetElementsByClassName(
          this.body___, className, this.editable___);
    };
    TameHTMLDocument.prototype.addEventListener =
        function (name, listener, useCapture) {
          return this.tameBody___.addEventListener(name, listener, useCapture);
        };
    TameHTMLDocument.prototype.removeEventListener =
        function (name, listener, useCapture) {
          return this.tameBody___.removeEventListener(
              name, listener, useCapture);
        };
    TameHTMLDocument.prototype.createComment = function (text) {
      return defaultTameNode(this.doc___.createComment(" "), true);
    };
    TameHTMLDocument.prototype.createDocumentFragment = function () {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      return defaultTameNode(this.doc___.createDocumentFragment(), true);
    };
    TameHTMLDocument.prototype.createElement = function (tagName) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      tagName = String(tagName).toLowerCase();
      if (!html4.ELEMENTS.hasOwnProperty(tagName)) {
        throw new Error(UNKNOWN_TAGNAME + "[" + tagName + "]");
      }
      var flags = html4.ELEMENTS[tagName];
      // Script exemption allows dynamic loading of proxied scripts.
      if ((flags & html4.eflags.UNSAFE) && !(flags & html4.eflags.SCRIPT)) {
         ___.log(UNSAFE_TAGNAME + "[" + tagName + "]: no action performed");
        return null;
      }
      var newEl = this.doc___.createElement(tagName);
      if ("canvas" == tagName) {
        bridal.initCanvasElement(newEl);
      }
      if (elementPolicies.hasOwnProperty(tagName)) {
        var attribs = elementPolicies[tagName]([]);
        if (attribs) {
          for (var i = 0; i < attribs.length; i += 2) {
            bridal.setAttribute(newEl, attribs[i], attribs[i + 1]);
          }
        }
      }
      return defaultTameNode(newEl, true);
    };
    TameHTMLDocument.prototype.createTextNode = function (text) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      return defaultTameNode(this.doc___.createTextNode(
          text !== null && text !== void 0 ? '' + text : ''), true);
    };
    TameHTMLDocument.prototype.getElementById = function (id) {
      id += idSuffix;
      var node = this.doc___.getElementById(id);
      return defaultTameNode(node, this.editable___);
    };
    TameHTMLDocument.prototype.getForms___ = function () {
      var tameForms = [];
      for (var i = 0; i < this.doc___.forms.length; i++) {
        var tameForm = tameRelatedNode(
          this.doc___.forms.item(i), this.editable___, defaultTameNode);
        // tameRelatedNode returns null if the node is not part of
        // this node's virtual document.
        if (tameForm !== null) { tameForms.push(tameForm); }
      }
      return fakeNodeList(tameForms);
    };
    TameHTMLDocument.prototype.getCompatMode___ = function () {
      return 'CSS1Compat';
    };
    TameHTMLDocument.prototype.toString = ___.markFuncFreeze(function () {
      return '[Fake Document]';
    });
    // http://www.w3.org/TR/DOM-Level-2-Events/events.html
    // #Events-DocumentEvent-createEvent
    TameHTMLDocument.prototype.createEvent = function (type) {
      type = String(type);
      if (type !== 'HTMLEvents') {
        // See https://developer.mozilla.org/en/DOM/document.createEvent#Notes
        // for a long list of event ypes.
        // See http://www.w3.org/TR/DOM-Level-2-Events/events.html
        // #Events-eventgroupings
        // for the DOM2 list.
        throw new Error('Unrecognized event type ' + type);
      }
      var document = this.doc___;
      var rawEvent;
      if (document.createEvent) {
        rawEvent = document.createEvent(type);
      } else {
        rawEvent = document.createEventObject();
        rawEvent.eventType = 'ondataavailable';
      }
      rawEvent.creatorDoc___ = document;
      var tamedEvent = new TameCustomHTMLEvent(rawEvent);
      return tamedEvent;
    };
    TameHTMLDocument.prototype.getOwnerDocument___ = function () {
      return null;
    };
    // Called by the html-emitter when the virtual document has been loaded.
    TameHTMLDocument.prototype.signalLoaded___ = function () {
      var onload = imports.window && imports.window.v___('onload');
      if (onload) {
        setTimeout(
            function () { ___.callPub(onload, 'call', [___.USELESS]); },
            0);
      }
      var listeners = this.onLoadListeners___;
      this.onLoadListeners___ = [];
      for (var i = 0, n = listeners.length; i < n; ++i) {
        (function (listener) {
          setTimeout(
              function () { ___.callPub(listener, 'call', [___.USELESS]); },
              0);
        })(listeners[i]);
      }
    };

    ___.all2(___.grantTypedMethod, TameHTMLDocument.prototype,
             ['addEventListener', 'removeEventListener',
              'createComment', 'createDocumentFragment',
              'createElement', 'createEvent', 'createTextNode',
              'getElementById', 'getElementsByClassName',
              'getElementsByTagName']);


    // For JavaScript handlers.  See plugin_dispatchEvent___ below
    imports.handlers___ = [];
    imports.TameHTMLDocument___ = TameHTMLDocument;  // Exposed for testing
    imports.tameNode___ = defaultTameNode;
    imports.feralNode___ = ___.markFuncFreeze(function(tameNode) {
      tameNode = TameNodeT.coerce(tameNode);
      return tameNode.node___;
    });
    imports.tameEvent___ = tameEvent;
    imports.blessHtml___ = blessHtml;
    imports.blessCss___ = function (var_args) {
      var arr = [];
      for (var i = 0, n = arguments.length; i < n; ++i) {
        arr[i] = arguments[i];
      }
      return cssSealerUnsealerPair.seal(arr);
    };
    imports.htmlAttr___ = function (s) {
      return html.escapeAttrib(String(s || ''));
    };
    imports.html___ = safeHtml;
    imports.rewriteUri___ = function (uri, mimeType) {
      var s = rewriteAttribute(null, null, html4.atype.URI, uri);
      if (!s) { throw new Error(); }
      return s;
    };
    imports.suffix___ = function (nmtokens) {
      var p = String(nmtokens).replace(/^\s+|\s+$/g, '').split(/\s+/g);
      var out = [];
      for (var i = 0; i < p.length; ++i) {
        var nmtoken = rewriteAttribute(null, null, html4.atype.ID, p[i]);
        if (!nmtoken) { throw new Error(nmtokens); }
        out.push(nmtoken);
      }
      return out.join(' ');
    };
    imports.ident___ = function (nmtokens) {
      var p = String(nmtokens).replace(/^\s+|\s+$/g, '').split(/\s+/g);
      var out = [];
      for (var i = 0; i < p.length; ++i) {
        var nmtoken = rewriteAttribute(null, null, html4.atype.CLASSES, p[i]);
        if (!nmtoken) { throw new Error(nmtokens); }
        out.push(nmtoken);
      }
      return out.join(' ');
    };
    imports.rewriteUriInCss___ = function(value) {
      return value
        ? uriCallback.rewrite(value, html4.ueffects.SAME_DOCUMENT,
              html4.ltypes.SANDBOXED, {})
        : void 0;
    };
    imports.rewriteUriInAttribute___ = function(value, tagName, attribName) {
      return value
        ? uriCallback.rewrite(value, getUriEffect(tagName, attribName),
              getLoaderType(tagName, attribName), {"XML_ATTR": attribName})
        : void 0;
    };

    var allCssProperties = domitaModules.CssPropertiesCollection(
        css.properties, document.documentElement, css);
    var historyInsensitiveCssProperties = domitaModules.CssPropertiesCollection(
        css.HISTORY_INSENSITIVE_STYLE_WHITELIST, document.documentElement, css);

    /**
     * http://www.w3.org/TR/DOM-Level-2-Style/css.html#CSS-CSSStyleDeclaration
     */
    function TameStyle(style, editable, tameEl) {
      this.style___ = style;
      this.editable___ = editable;
      this.tameEl___ = tameEl;
    }
    inertCtor(TameStyle, Object, 'Style');
    TameStyle.prototype.readByCanonicalName___ = function(canonName) {
      return String(this.style___[canonName] || '');
    };
    TameStyle.prototype.writeByCanonicalName___ = function(canonName, val) {
      this.style___[canonName] = val;
    };
    TameStyle.prototype.allowProperty___ = function (cssPropertyName) {
      return allCssProperties.isCssProp(cssPropertyName);
    };
    TameStyle.prototype.handleRead___ = function (stylePropertyName) {
      var self = this;
      if (String(stylePropertyName) === 'getPropertyValue') {
        return ___.markFuncFreeze(function(args) {
          return TameStyle.prototype.getPropertyValue.call(self, args);
        });
      }
      if (!this.style___
          || !allCssProperties.isCanonicalProp(stylePropertyName)) {
        return void 0;
      }
      var cssPropertyName =
          allCssProperties.getCssPropFromCanonical(stylePropertyName);
      if (!this.allowProperty___(cssPropertyName)) { return void 0; }
      var canonName = allCssProperties.getCanonicalPropFromCss(cssPropertyName);
      return this.readByCanonicalName___(canonName);
    };
    TameStyle.prototype.handleCall___ = function(name, args) {
      if (String(name) === 'getPropertyValue') {
        return TameStyle.prototype.getPropertyValue.call(this, args);
      }
      throw 'Cannot handle method ' + String(name);
    };
    TameStyle.prototype.getPropertyValue = function (cssPropertyName) {
      cssPropertyName = String(cssPropertyName || '').toLowerCase();
      if (!this.allowProperty___(cssPropertyName)) { return ''; }
      var canonName = allCssProperties.getCanonicalPropFromCss(cssPropertyName);
      return this.readByCanonicalName___(canonName);
    };
    TameStyle.prototype.handleSet___ = function (stylePropertyName, value) {
      if (!this.editable___) { throw new Error('style not editable'); }
      stylePropertyName = String(stylePropertyName);
      if (stylePropertyName === 'cssText') {
        if (typeof this.style___.cssText === 'string') {
          this.style___.cssText = sanitizeStyleAttrValue(value);
        } else {
          // If the browser doesn't support setting cssText, then fall back to
          // setting the style attribute of the containing element.  This won't
          // work for style declarations that are part of stylesheets and not
          // attached to elements.
          this.tameEl___.setAttribute('style', value);
        }
        return value;
      }
      if (!allCssProperties.isCanonicalProp(stylePropertyName)) {
        throw new Error('Unknown CSS property name ' + stylePropertyName);
      }
      var cssPropertyName =
          allCssProperties.getCssPropFromCanonical(stylePropertyName);
      if (!this.allowProperty___(cssPropertyName)) { return void 0; }
      var pattern = css.properties[cssPropertyName];
      if (!pattern) { throw new Error('style not editable'); }
      var val = '' + (value || '');
      // CssPropertyPatterns.java only allows styles of the form
      // url("...").  See the BUILTINS definition for the "uri" symbol.
      val = val.replace(
          /\burl\s*\(\s*\"([^\"]*)\"\s*\)/gi,
          function (_, url) {
            var decodedUrl = decodeCssString(url);
            var rewrittenUrl = uriCallback
                ? uriCallback.rewrite(
                    decodedUrl, html4.ueffects.SAME_DOCUMENT, html4.ltypes.SANDBOXED,
                    { "CSS_PROP": cssPropertyName})
                : null;
            if (!rewrittenUrl) {
              rewrittenUrl = 'about:blank';
            }
            return 'url("'
                + rewrittenUrl.replace(
                    /[\"\'\{\}\(\):\\]/g,
                    function (ch) {
                      return '\\' + ch.charCodeAt(0).toString(16) + ' ';
                    })
                + '")';
          });
      if (val && !pattern.test(val + ' ')) {
        throw new Error('bad value `' + val + '` for CSS property '
                        + stylePropertyName);
      }
      var canonName = allCssProperties.getCanonicalPropFromCss(cssPropertyName);
      this.writeByCanonicalName___(canonName, val);
      return value;
    };
    TameStyle.prototype.toString =
        ___.markFuncFreeze(function () { return '[Fake Style]'; });

    function isNestedInAnchor(rawElement) {
      for ( ; rawElement && rawElement != pseudoBodyNode;
           rawElement = rawElement.parentNode) {
        if (rawElement.tagName.toLowerCase() === 'a') { return true; }
      }
      return false;
    }

    function TameComputedStyle(rawElement, pseudoElement) {
      rawElement = rawElement || document.createElement('div');
      TameStyle.call(
          this,
          bridal.getComputedStyle(rawElement, pseudoElement),
          false);
      this.rawElement___ = rawElement;
      this.pseudoElement___ = pseudoElement;
    }
    ___.extend(TameComputedStyle, TameStyle);
    TameComputedStyle.prototype.readByCanonicalName___ = function(canonName) {
      var canReturnDirectValue =
          historyInsensitiveCssProperties.isCanonicalProp(canonName)
          || !isNestedInAnchor(this.rawElement___);
      if (canReturnDirectValue) {
        return TameStyle.prototype.readByCanonicalName___.call(this, canonName);
      } else {
        return new TameComputedStyle(pseudoBodyNode, this.pseudoElement___)
            .readByCanonicalName___(canonName);
      }
    };
    TameComputedStyle.prototype.writeByCanonicalName___ = function(canonName) {
      throw 'Computed styles not editable: This code should be unreachable';
    };
    TameComputedStyle.prototype.toString = ___.markFuncFreeze(function () {
      return '[Fake Computed Style]';
    });

    // Note: nodeClasses.XMLHttpRequest is a ctor that *can* be directly
    // called by cajoled code, so we do not use inertCtor().
    nodeClasses.XMLHttpRequest = domitaModules.TameXMLHttpRequest(
        domitaModules.XMLHttpRequestCtor(
            window.XMLHttpRequest,
            window.ActiveXObject),
        uriCallback);

    /**
     * given a number, outputs the equivalent css text.
     * @param {number} num
     * @return {string} an CSS representation of a number suitable for both html
     *    attribs and plain text.
     */
    imports.cssNumber___ = function (num) {
      if ('number' === typeof num && isFinite(num) && !isNaN(num)) {
        return '' + num;
      }
      throw new Error(num);
    };
    /**
     * given a number as 24 bits of RRGGBB, outputs a properly formatted CSS
     * color.
     * @param {number} num
     * @return {string} a CSS representation of num suitable for both html
     *    attribs and plain text.
     */
    imports.cssColor___ = function (color) {
      // TODO: maybe whitelist the color names defined for CSS if the arg is a
      // string.
      if ('number' !== typeof color || (color != (color | 0))) {
        throw new Error(color);
      }
      var hex = '0123456789abcdef';
      return '#' + hex.charAt((color >> 20) & 0xf)
          + hex.charAt((color >> 16) & 0xf)
          + hex.charAt((color >> 12) & 0xf)
          + hex.charAt((color >> 8) & 0xf)
          + hex.charAt((color >> 4) & 0xf)
          + hex.charAt(color & 0xf);
    };
    imports.cssUri___ = function (uri, mimeType) {
      var s = rewriteAttribute(null, null, html4.atype.URI, uri);
      if (!s) { throw new Error(); }
      return s;
    };

    /**
     * Create a CSS stylesheet with the given text and append it to the DOM.
     * @param {string} cssText a well-formed stylesheet production.
     */
    imports.emitCss___ = function (cssText) {
      this.getCssContainer___().appendChild(
          bridal.createStylesheet(document, cssText));
    };
    /** The node to which gadget stylesheets should be added. */
    imports.getCssContainer___ = function () {
      return document.getElementsByTagName('head')[0];
    };

    if (!/^-/.test(idSuffix)) {
      throw new Error('id suffix "' + idSuffix + '" must start with "-"');
    }
    if (!/___$/.test(idSuffix)) {
      throw new Error('id suffix "' + idSuffix + '" must end with "___"');
    }
    var idClass = idSuffix.substring(1);
    var idClassPattern = new RegExp(
        '(?:^|\\s)' + idClass.replace(/[\.$]/g, '\\$&') + '(?:\\s|$)');
    /** A per-gadget class used to separate style rules. */
    imports.getIdClass___ = function () {
      return idClass;
    };
    // enforce id class on element
    bridal.setAttribute(pseudoBodyNode, "class",
        bridal.getAttribute(pseudoBodyNode, "class")
        + " " + idClass + " vdoc-body___");

    // bitmask of trace points
    //    0x0001 plugin_dispatchEvent
    imports.domitaTrace___ = 0;
    imports.getDomitaTrace = ___.markFuncFreeze(
        function () { return imports.domitaTrace___; }
    );
    imports.setDomitaTrace = ___.markFuncFreeze(
        function (x) { imports.domitaTrace___ = x; }
    );

   function assignToImports(target, name, value) {
     if (target.DefineOwnProperty___) {
        target.DefineOwnProperty___(name, {
          value: value,
          writable: true,
          enumerable: true,
          configurable: true
        });
     } else {
       target[name] =  value;
     }
   }

    // TODO(mikesamuel): remove these, and only expose them via window once
    // Valija works
    imports.setTimeout = tameSetTimeout;
    imports.setInterval = tameSetInterval;
    imports.clearTimeout = tameClearTimeout;
    imports.clearInterval = tameClearInterval;

    var tameDocument = new TameHTMLDocument(
        document,
        pseudoBodyNode,
        String(optPseudoWindowLocation.hostname || 'nosuchhost.fake'),
        true);
    assignToImports(imports, 'document', tameDocument);
    imports.document.tameNode___ = imports.tameNode___;
    imports.document.feralNode___ = imports.feralNode___;
    imports.document.tameNode___ = imports.tameNode___;
    imports.document.feralNode___ = imports.feralNode___;

    // TODO(mikesamuel): figure out a mechanism by which the container can
    // specify the gadget's apparent URL.
    // See http://www.whatwg.org/specs/web-apps/current-work/multipage/history.html#location0
    var tameLocation = ___.primFreeze({
      toString: ___.markFuncFreeze(function () { return tameLocation.href; }),
      href: String(optPseudoWindowLocation.href
                   || 'http://nosuchhost.fake:80/'),
      hash: String(optPseudoWindowLocation.hash || ''),
      host: String(optPseudoWindowLocation.host || 'nosuchhost.fake:80'),
      hostname: String(optPseudoWindowLocation.hostname || 'nosuchhost.fake'),
      pathname: String(optPseudoWindowLocation.pathname || '/'),
      port: String(optPseudoWindowLocation.port || '80'),
      protocol: String(optPseudoWindowLocation.protocol || 'http:'),
      search: String(optPseudoWindowLocation.search || '')
      });

    // See spec at http://www.whatwg.org/specs/web-apps/current-work/multipage/browsers.html#navigator
    // We don't attempt to hide or abstract userAgent details since
    // they are discoverable via side-channels we don't control.
    var tameNavigator = ___.primFreeze({
      appName: String(window.navigator.appName),
      appVersion: String(window.navigator.appVersion),
      platform: String(window.navigator.platform),
      // userAgent should equal the string sent in the User-Agent HTTP header.
      userAgent: String(window.navigator.userAgent),
      // Custom attribute indicating Caja is active.
      cajaVersion: '1.0'
      });

    /**
     * Set of allowed pseudo elements as described at
     * http://www.w3.org/TR/CSS2/selector.html#q20
     */
    var PSEUDO_ELEMENT_WHITELIST = {
      // after and before disallowed since they can leak information about
      // arbitrary ancestor nodes.
      'first-letter': true,
      'first-line': true
    };

    /**
     * See http://www.whatwg.org/specs/web-apps/current-work/multipage/browsers.html#window for the full API.
     */
    function TameWindow() {
      this.properties___ = {};
      this.FERAL_TWIN___ = this.TAMED_TWIN___ = this;
    }

    /**
     * An <a href=
     * href=http://www.w3.org/TR/DOM-Level-2-Views/views.html#Views-AbstractView
     * >AbstractView</a> implementation that exposes styling, positioning, and
     * sizing information about the current document's pseudo-body.
     * <p>
     * The AbstractView spec specifies very little in its IDL description, but
     * mozilla defines it thusly:<blockquote>
     *   document.defaultView is generally a reference to the window object
     *   for the document, however that is not defined in the specification
     *   and can't be relied upon for all host environments, particularly as
     *   not all browsers implement it.
     * </blockquote>
     * <p>
     * We can't provide access to the tamed window directly from document
     * since it is the global scope of valija code, and so access to another
     * module's tamed window provides an unbounded amount of authority.
     * <p>
     * Instead, we expose styling, positioning, and sizing properties
     * via this class.  All of this authority is already available from the
     * document.
     */
    function TameDefaultView() {
      // TODO(mikesamuel): Implement in terms of
      //     http://www.w3.org/TR/cssom-view/#the-windowview-interface
      // TODO: expose a read-only version of the document
      this.document = tameDocument;
      // Exposing an editable default view that pointed to a read-only
      // tameDocument via document.defaultView would allow escalation of
      // authority.
      assert(tameDocument.editable___);
      this.FERAL_TWIN___ = this.TAMED_TWIN___ = this;
      ___.grantRead(this, 'document');
    }

    ___.forOwnKeys({
      document: tameDocument,
      location: tameLocation,
      navigator: tameNavigator,
      setTimeout: tameSetTimeout,
      setInterval: tameSetInterval,
      clearTimeout: tameClearTimeout,
      clearInterval: tameClearInterval,
      addEventListener: ___.markFuncFreeze(
          function (name, listener, useCapture) {
            if (name === 'load') {
              classUtils.ensureValidCallback(listener);
              tameDocument.onLoadListeners___.push(listener);
            } else {
              // TODO: need a testcase for this
              tameDocument.addEventListener(name, listener, useCapture);
            }
          }),
      removeEventListener: ___.markFuncFreeze(
          function (name, listener, useCapture) {
            if (name === 'load') {
              var listeners = tameDocument.onLoadListeners___;
              var k = 0;
              for (var i = 0, n = listeners.length; i < n; ++i) {
                listeners[i - k] = listeners[i];
                if (listeners[i] === listener) {
                  ++k;
                }
              }
              listeners.length -= k;
            } else {
              tameDocument.removeEventListener(name, listener, useCapture);
            }
          }),
      dispatchEvent: ___.markFuncFreeze(function (evt) {
        // TODO(ihab.awad): Implement
      })
    }, ___.markFuncFreeze(function (propertyName, value) {
      TameWindow.prototype[propertyName] = value;
      ___.grantRead(TameWindow.prototype, propertyName);
    }));
    ___.forOwnKeys({
      scrollBy: ___.markFuncFreeze(
          function (dx, dy) {
            // The window is always auto scrollable, so make the apparent window
            // body scrollable if the gadget tries to scroll it.
            if (dx || dy) { makeScrollable(tameDocument.body___); }
            tameScrollBy(tameDocument.body___, dx, dy);
          }),
      scrollTo: ___.markFuncFreeze(
          function (x, y) {
            // The window is always auto scrollable, so make the apparent window
            // body scrollable if the gadget tries to scroll it.
            makeScrollable(tameDocument.body___);
            tameScrollTo(tameDocument.body___, x, y);
          }),
      resizeTo: ___.markFuncFreeze(
          function (w, h) {
            tameResizeTo(tameDocument.body___, w, h);
          }),
      resizeBy: ___.markFuncFreeze(
          function (dw, dh) {
            tameResizeBy(tameDocument.body___, dw, dh);
          }),
      /** A partial implementation of getComputedStyle. */
      getComputedStyle: ___.markFuncFreeze(
          // Pseudo elements are suffixes like :first-line which constrain to
          // a portion of the element's content as defined at
          // http://www.w3.org/TR/CSS2/selector.html#q20
          function (tameElement, pseudoElement) {
            tameElement = TameNodeT.coerce(tameElement);
            // Coerce all nullish values to undefined, since that is the value
            // for unspecified parameters.
            // Per bug 973: pseudoElement should be null according to the
            // spec, but mozilla docs contradict this.
            // From https://developer.mozilla.org/En/DOM:window.getComputedStyle
            //     pseudoElt is a string specifying the pseudo-element to match.
            //     Should be an empty string for regular elements.
            pseudoElement = (pseudoElement === null || pseudoElement === void 0
                             || '' === pseudoElement)
                ? void 0 : String(pseudoElement).toLowerCase();
            if (pseudoElement !== void 0
                && !PSEUDO_ELEMENT_WHITELIST.hasOwnProperty(pseudoElement)) {
              throw new Error('Bad pseudo element ' + pseudoElement);
            }
            // No need to check editable since computed styles are readonly.
            return new TameComputedStyle(
                tameElement.node___,
                pseudoElement);
          })

      // NOT PROVIDED
      // event: a global on IE.  We always define it in scopes that can handle
      //        events.
      // opera: defined only on Opera.
    }, ___.markFuncFreeze(function (propertyName, value) {
      TameWindow.prototype[propertyName] = value;
      ___.grantRead(TameWindow.prototype, propertyName);
      TameDefaultView.prototype[propertyName] = value;
      ___.grantRead(TameDefaultView.prototype, propertyName);
    }));
    if (!Object.prototype.DefineOwnProperty___) {
      TameWindow.prototype.handleRead___ = function (name) {
        name = String(name);
        if (endsWith__.test(name)) { return void 0; }
        var handlerName = name + '_getter___';
        if (this[handlerName]) {
          return this[handlerName]();
        }
        if (___.hasOwnProp(this, name)) {
          return this[name];
        } else {
          return void 0;
        }
      };
      TameWindow.prototype.handleSet___ = function (name, val) {
        name = String(name);
        if (endsWith__.test(name)) { throw new Error(INVALID_SUFFIX); }
        var handlerName = name + '_setter___';
        if (this[handlerName]) {
          return this[handlerName](val);
        }
        this[name + '_c___'] = this;
        this[name + '_v___'] = false;
        this[name + '_e___'] = this;
        return this[name] = val;
      };
      TameWindow.prototype.handleDelete___ = function (name) {
        name = String(name);
        if (endsWith__.test(name)) { throw new Error(INVALID_SUFFIX); }
        var handlerName = name + '_deleter___';
        if (this[handlerName]) {
          return this[handlerName]();
        }
        return delete this[name + '_c___']
            && delete this[name + '_e___']
            && delete this[name + '_v___'];
      };
    }

    var tameWindow = new TameWindow();
    var tameDefaultView = new TameDefaultView(tameDocument.editable___);

    function propertyOnlyHasGetter(_) {
      throw new TypeError('setting a property that only has a getter');
    }
    ___.forOwnKeys({
      // We define all the window positional properties relative to
      // the fake body element to maintain the illusion that the fake
      // document is completely defined by the nodes under the fake body.
      clientLeft: {
        get: function () { return tameDocument.body___.clientLeft; }
      },
      clientTop: {
        get: function () { return tameDocument.body___.clientTop; }
      },
      clientHeight: {
        get: function () { return tameDocument.body___.clientHeight; }
      },
      clientWidth: {
        get: function () { return tameDocument.body___.clientWidth; }
      },
      offsetLeft: {
        get: function () { return tameDocument.body___.offsetLeft; }
      },
      offsetTop: {
        get: function () { return tameDocument.body___.offsetTop; }
      },
      offsetHeight: {
        get: function () { return tameDocument.body___.offsetHeight; }
      },
      offsetWidth: {
        get: function () { return tameDocument.body___.offsetWidth; }
      },
      // page{X,Y}Offset appear only as members of window, not on all elements
      // but http://www.howtocreate.co.uk/tutorials/javascript/browserwindow
      // says that they are identical to the scrollTop/Left on all browsers but
      // old versions of Safari.
      pageXOffset: {
        get: function () { return tameDocument.body___.scrollLeft; }
      },
      pageYOffset: {
        get: function () { return tameDocument.body___.scrollTop; }
      },
      scrollLeft: {
        get: function () { return tameDocument.body___.scrollLeft; },
        set: function (x) { tameDocument.body___.scrollLeft = +x; return x; }
      },
      scrollTop: {
        get: function () { return tameDocument.body___.scrollTop; },
        set: function (y) { tameDocument.body___.scrollTop = +y; return y; }
      },
      scrollHeight: {
        get: function () { return tameDocument.body___.scrollHeight; }
      },
      scrollWidth: {
        get: function () { return tameDocument.body___.scrollWidth; }
      }
    }, ___.markFuncFreeze(function (propertyName, def) {
      var views = [tameWindow, tameDefaultView, tameDocument.getBody___(),
                   tameDocument.getDocumentElement___()];
      var setter = def.set || propertyOnlyHasGetter, getter = def.get;
      for (var i = views.length; --i >= 0;) {
        var view = views[i];
        ___.useGetHandler(view, propertyName, getter);
        ___.useSetHandler(view, propertyName, setter);
      }
    }));

    ___.forOwnKeys({
      innerHeight: function () { return tameDocument.body___.clientHeight; },
      innerWidth: function () { return tameDocument.body___.clientWidth; },
      outerHeight: function () { return tameDocument.body___.clientHeight; },
      outerWidth: function () { return tameDocument.body___.clientWidth; }
    }, ___.markFuncFreeze(function (propertyName, handler) {
      // TODO(mikesamuel): define on prototype.
      ___.useGetHandler(tameWindow, propertyName, handler);
      ___.useGetHandler(tameDefaultView, propertyName, handler);
    }));

    // Attach reflexive properties to 'window' object
    var windowProps = ['top', 'self', 'opener', 'parent', 'window'];
    var wpLen = windowProps.length;
    for (var i = 0; i < wpLen; ++i) {
      var prop = windowProps[i];
      if (Object.prototype.DefineOwnProperty___) {
        tameWindow.DefineOwnProperty___(prop, {
          value: tameWindow,
          writable: true,
          enumerable: true,
          configurable: true
        });
      } else {
        tameWindow[prop] = tameWindow;
        ___.grantRead(tameWindow, prop);
      }
    }

    if (tameDocument.editable___) {
      tameDocument.defaultView = tameDefaultView;
      ___.grantRead(tameDocument, 'defaultView');
      // Hook for document.write support.
      tameDocument.sanitizeAttrs___ = sanitizeAttrs;
    }

    // Iterate over all node classes, assigning them to the Window object
    // under their DOM Level 2 standard name.
    ___.forOwnKeys(nodeClasses, ___.markFuncFreeze(function(name, ctor) {
      ___.primFreeze(ctor);
      if (Object.prototype.DefineOwnProperty___) {
        tameWindow.DefineOwnProperty___(name, {
          value: ctor,
          writable: true,
          enumerable: true,
          configurable: true
        });
      } else {
        tameWindow[name] = ctor;
        ___.grantRead(tameWindow, name);
      }
    }));

    // TODO(ihab.awad): Build a more sophisticated virtual class hierarchy by
    // creating a table of actual subclasses and instantiating tame nodes by
    // table lookups. This will allow the client code to see a truly consistent
    // DOM class hierarchy.
    var defaultNodeClasses = [
      'HTMLAppletElement',
      'HTMLAreaElement',
      'HTMLBaseElement',
      'HTMLBaseFontElement',
      'HTMLBodyElement',
      'HTMLBRElement',
      'HTMLButtonElement',
      'HTMLDirectoryElement',
      'HTMLDivElement',
      'HTMLDListElement',
      'HTMLFieldSetElement',
      'HTMLFontElement',
      'HTMLFrameElement',
      'HTMLFrameSetElement',
      'HTMLHeadElement',
      'HTMLHeadingElement',
      'HTMLHRElement',
      'HTMLHtmlElement',
      'HTMLIFrameElement',
      'HTMLIsIndexElement',
      'HTMLLabelElement',
      'HTMLLegendElement',
      'HTMLLIElement',
      'HTMLLinkElement',
      'HTMLMapElement',
      'HTMLMenuElement',
      'HTMLMetaElement',
      'HTMLModElement',
      'HTMLObjectElement',
      'HTMLOListElement',
      'HTMLOptGroupElement',
      'HTMLOptionElement',
      'HTMLParagraphElement',
      'HTMLParamElement',
      'HTMLPreElement',
      'HTMLQuoteElement',
      'HTMLScriptElement',
      'HTMLSelectElement',
      'HTMLStyleElement',
      'HTMLTableCaptionElement',
      'HTMLTableCellElement',
      'HTMLTableColElement',
      'HTMLTableElement',
      'HTMLTableRowElement',
      'HTMLTableSectionElement',
      'HTMLTextAreaElement',
      'HTMLTitleElement',
      'HTMLUListElement'
    ];

    var defaultNodeClassCtor = nodeClasses.Element;
    for (var i = 0; i < defaultNodeClasses.length; i++) {
      if (Object.prototype.DefineOwnProperty___) {
        tameWindow.DefineOwnProperty___(defaultNodeClasses[i], {
          value: defaultNodeClassCtor,
          writable: true,
          enumerable: true,
          configurable: true
        });
      } else {
        tameWindow[defaultNodeClasses[i]] = defaultNodeClassCtor;
        ___.grantRead(tameWindow, defaultNodeClasses[i]);
      }
    }

    var outers = imports.outers;
    if (___.isJSONContainer(outers)) {
      // For Valija, use the window object as outers.
      ___.forOwnKeys(outers, ___.markFuncFreeze(function(k, v) {
        if (!(k in tameWindow)) {
          // No need to check for DefineOwnProperty___; this case
          // occurs iff we are in Valija, not ES53.
          tameWindow[k] = v;
          ___.grantRead(tameWindow, k);
        }
      }));
      imports.outers = tameWindow;
    } else {
      assignToImports(imports, 'window', tameWindow);
    }
  }

  return attachDocumentStub;
})();

/**
 * Function called from rewritten event handlers to dispatch an event safely.
 */
function plugin_dispatchEvent___(thisNode, event, pluginId, handler) {
  event = (event || bridal.getWindow(thisNode).event);
  // support currentTarget on IE[678]
  if (!event.currentTarget) {
    event.currentTarget = thisNode;
  }
  var imports = ___.getImports(pluginId);
  var node = imports.tameNode___(thisNode, true);
  return plugin_dispatchToHandler___(
      pluginId, handler, [ node, imports.tameEvent___(event), node ]);
}

function plugin_dispatchToHandler___(pluginId, handler, args) {
  var sig = ('' + handler).match(/^function\b[^\)]*\)/);
  var imports = ___.getImports(pluginId);
  if (imports.domitaTrace___ & 0x1) {
    ___.log(
        'Dispatch pluginId=' + pluginId +
        ', handler=' + (sig ? sig[0] : handler) +
        ', args=' + args);
  }
  switch (typeof handler) {
    case 'number':
      handler = imports.handlers___[handler];
      break;
    case 'string':
      var fn = void 0;
      var tameWin = void 0;
      var $v = ___.readPub(imports, '$v');
      if ($v) {
        fn = ___.callPub($v, 'ros', [handler]);
        if (!fn) { tameWin = ___.callPub($v, 'ros', ['window']); }
      }
      if (!fn) {
        fn = ___.readPub(imports, handler);
        if (!fn) {
          if (!tameWin) { tameWin = ___.readPub(imports, 'window'); }
          if (tameWin) { fn = ___.readPub(tameWin, handler); }
        }
      }
      handler = fn && typeof fn.call === 'function' ? fn : void 0;
      break;
    case 'function': case 'object': break;
    default:
      throw new Error(
          'Expected function as event handler, not ' + typeof handler);
  }
  if (___.startCallerStack) { ___.startCallerStack(); }
  imports.isProcessingEvent___ = true;
  try {
    return ___.callPub(handler, 'call', args);
  } catch (ex) {
    if (ex && ex.cajitaStack___ && 'undefined' !== (typeof console)) {
      console.error(
          'Event dispatch %s: %s', handler, ex.cajitaStack___.join('\n'));
    }
    throw ex;
  } finally {
    imports.isProcessingEvent___ = false;
  }
}
