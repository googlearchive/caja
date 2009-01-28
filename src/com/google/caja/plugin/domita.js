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
 * Caveats:
 * - This is not a full implementation.
 * - Security Review is pending.
 * - <code>===</code> and <code>!==</code> on node lists will not
 *   behave the same as with untamed node lists.  Specifically, it is
 *   not always true that {@code nodeA.childNodes === nodeA.childNodes}.
 * - Properties backed by setters/getters like {@code HTMLElement.innerHTML}
 *   will not appear to uncajoled code as DOM nodes do, since they are
 *   implemented using cajita property handlers.
 *
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
 * @author mikesamuel@gmail.com
 * @requires console, document, window
 * @requires clearInterval, clearTimeout, setInterval, setTimeout
 * @requires ___, bridal, cajita, css, html, html4, unicode
 * @provides attachDocumentStub, domitaModules, plugin_dispatchEvent___
 */

var domitaModules;
if (!domitaModules) { domitaModules = {}; }

domitaModules.classUtils = function() {

  /**
   * Add setter and getter hooks so that the caja {@code node.innerHTML = '...'}
   * works as expected.
   */
  function exportFields(object, fields) {
    for (var i = fields.length; --i >= 0;) {
      var field = fields[i];
      var fieldUCamel = field.charAt(0).toUpperCase() + field.substring(1);
      var getterName = 'get' + fieldUCamel;
      var setterName = 'set' + fieldUCamel;
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
   * Makes the first a subclass of the second.
   */
  function extend(subClass, baseClass) {
    var noop = function () {};
    noop.prototype = baseClass.prototype;
    subClass.prototype = new noop();
    subClass.prototype.constructor = subClass;
  }

  return {
    exportFields: exportFields,
    extend: extend
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
    this.xhr___ = new xmlHttpRequestMaker();
    classUtils.exportFields(
        this,
        ['onreadystatechange', 'readyState', 'responseText', 'responseXML',
         'status', 'statusText']);
  }
  TameXMLHttpRequest.prototype.setOnreadystatechange = function (handler) {
    // TODO(ihab.awad): Do we need more attributes of the event than 'target'?
    // May need to implement full "tame event" wrapper similar to DOM events.
    var self = this;
    this.xhr___.onreadystatechange = function(event) {
      var evt = { target: self };
      return ___.callPub(handler, 'call', [void 0, evt]);
    };
    // Store for later direct invocation if need be
    this.handler___ = handler;
  };
  TameXMLHttpRequest.prototype.getReadyState = function () {
    // The ready state should be a number
    return Number(this.xhr___.readyState);
  };
  TameXMLHttpRequest.prototype.open = function (
      method, URL, opt_async, opt_userName, opt_password) {
    method = String(method);
    // The XHR interface does not tell us the MIME type in advance, so we
    // must assume the broadest possible.
    var safeUri = uriCallback.rewrite(String(URL), "*/*");
    // If the uriCallback rejects the URL, we throw an exception, but we do not
    // put the URI in the exception so as not to put the caller at risk of some
    // code in its stack sniffing the URI.
    if (safeUri === void 0) { throw 'URI violates security policy'; }
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
  TameXMLHttpRequest.prototype.getResponseText = function () {
    var result = this.xhr___.responseText;
    return (result === undefined || result === null) ?
      result : String(result);
  };
  TameXMLHttpRequest.prototype.getResponseXML = function () {
    // TODO(ihab.awad): Implement a taming layer for XML. Requires generalizing
    // the HTML node hierarchy as well so we have a unified implementation.
    return {};
  };
  TameXMLHttpRequest.prototype.getStatus = function () {
    var result = this.xhr___.status;
    return (result === undefined || result === null) ?
      result : Number(result);
  };
  TameXMLHttpRequest.prototype.getStatusText = function () {
    var result = this.xhr___.statusText;
    return (result === undefined || result === null) ?
      result : String(result);
  };
  TameXMLHttpRequest.prototype.toString = function () {
    return 'Not a real XMLHttpRequest';
  };
  ___.ctor(TameXMLHttpRequest, void 0, 'TameXMLHttpRequest');
  ___.all2(___.grantTypedGeneric, TameXMLHttpRequest.prototype,
           ['open', 'setRequestHeader', 'send', 'abort',
            'getAllResponseHeaders', 'getResponseHeader']);

  return TameXMLHttpRequest;
};

/**
 * Add a tamed document implementation to a Gadget's global scope.
 *
 * @param {string} idSuffix a string suffix appended to all node IDs.
 * @param {Object} uriCallback an object like <pre>{
 *       rewrite: function (uri, mimeType) { return safeUri }
 *     }</pre>.
 *     The rewrite function should be idempotent to allow rewritten HTML
 *     to be reinjected.
 * @param {Object} imports the gadget's global scope.
 * @param {Node} pseudoBodyNode an HTML node to act as the "body" of the
 *     virtual document provided to Cajoled code.
 * @param {Object} optPseudoWindowLocation a record containing the
 *     properties of the browser "window.location" object, which will
 *     be provided to the Cajoled code.
 */
attachDocumentStub = (function () {
  // Array Remove - By John Resig (MIT Licensed)
  function arrayRemove(array, from, to) {
    var rest = array.slice((to || from) + 1 || array.length);
    array.length = from < 0 ? array.length + from : from;
    return array.push.apply(array, rest);
  }

  var tameNodeTrademark = {};
  var tameEventTrademark = {};

  // Define a wrapper type for known safe HTML, and a trademarker.
  // This does not actually use the trademarking functions since trademarks
  // cannot be applied to strings.
  function Html(htmlFragment) { this.html___ = String(htmlFragment || ''); }
  Html.prototype.valueOf = Html.prototype.toString
      = function () { return this.html___; };
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

  var XML_NAME_PATTERN = new RegExp(
      '^[' + unicode.LETTER + '_:][' + unicode.LETTER + unicode.DIGIT + '.\\-_:'
      + unicode.COMBINING_CHAR + unicode.EXTENDER + ']*$');

  var XML_NMTOKEN_PATTERN = new RegExp(
      '^[' + unicode.LETTER + unicode.DIGIT + '.\\-_:'
      + unicode.COMBINING_CHAR + unicode.EXTENDER + ']+$');

  var XML_NMTOKENS_PATTERN = new RegExp(
      '^(?:[' + XML_SPACE + ']*[' + unicode.LETTER + unicode.DIGIT + '.\\-_:'
      + unicode.COMBINING_CHAR + unicode.EXTENDER + ']+)+[' + XML_SPACE + ']*$'
      );

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

  /**
   * Coerces the string to a valid XML Name.
   * @see http://www.w3.org/TR/2000/REC-xml-20001006#NT-Name
   */
  function isXmlName(s) {
    return XML_NAME_PATTERN.test(s);
  }

  /**
   * Coerces the string to valid XML Nmtokens
   * @see http://www.w3.org/TR/2000/REC-xml-20001006#NT-Nmtokens
   */
  function isXmlNmTokens(s) {
    return XML_NMTOKENS_PATTERN.test(s);
  }

  // Trim whitespace from the beginning and end of a CSS string.

  function trimCssSpaces(input) {
    return input.replace(/^[ \t\r\n\f]+|[ \t\r\n\f]+$/g, '');
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
    var declarations = styleAttrValue.split(/;/g);

    for (var i = 0; declarations && i < declarations.length; i++) {
      var parts = declarations[i].split(':');
      var property = trimCssSpaces(parts[0]).toLowerCase();
      var value = trimCssSpaces(parts.slice(1).join(":"));
      // TODO(mikesamuel): make a separate function to map between
      // CSS property names and style object members while handling
      // float/cssFloat properly.
      var stylePropertyName = property.replace(  // font-size -> fontSize
          /-[a-z]/g, function (m) { return m.substring(1).toUpperCase(); });
      if (css.properties.hasOwnProperty(stylePropertyName)
          && css.properties[stylePropertyName].test(value + ' ')) {
        sanitizedDeclarations.push(property + ': ' + value);
      }
    }

    return sanitizedDeclarations.join(' ; ');
  }

  function mimeTypeForAttr(tagName, attribName) {
    if (tagName === 'img' && attribName === 'src') { return 'image/*'; }
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
      throw new Error();
    }
  }

  var classUtils = domitaModules.classUtils();

  var cssSealerUnsealerPair = cajita.makeSealerUnsealerPair();

  // Implementations of setTimeout, setInterval, clearTimeout, and
  // clearInterval that only allow simple functions as timeouts and
  // that treat timeout ids as capabilities.
  // This is safe even if accessed across frame since the same
  // trademark value is never used with more than one version of
  // setTimeout.
  var timeoutIdTrademark = {};
  function tameSetTimeout(timeout, delayMillis) {
    var timeoutId = setTimeout(
        function () { ___.callPub(timeout, 'call', [___.USELESS]); },
        delayMillis | 0);
    return ___.freeze(___.stamp(timeoutIdTrademark,
                          { timeoutId___: timeoutId }));
  }
  ___.frozenFunc(tameSetTimeout);
  function tameClearTimeout(timeoutId) {
    ___.guard(timeoutIdTrademark, timeoutId);
    clearTimeout(timeoutId.timeoutId___);
  }
  ___.frozenFunc(tameClearTimeout);
  var intervalIdTrademark = {};
  function tameSetInterval(interval, delayMillis) {
    var intervalId = setInterval(
        function () { ___.callPub(interval, 'call', [___.USELESS]); },
        delayMillis | 0);
    return ___.freeze(___.stamp(intervalIdTrademark,
                          { intervalId___: intervalId }));
  }
  ___.frozenFunc(tameSetInterval);
  function tameClearInterval(intervalId) {
    ___.guard(intervalIdTrademark, intervalId);
    clearInterval(intervalId.intervalId___);
  }
  ___.frozenFunc(tameClearInterval);

  function makeScrollable(element) {
    var overflow;
    if (element.currentStyle) {
      overflow = element.currentStyle.overflow;
    } else if (window.getComputedStyle) {
      overflow = window.getComputedStyle(element, void 0).overflow;
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
      style = window.getComputedStyle(element, void 0);
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
      // Anchor tags must have a target.
      attribs.push('target', '_blank');
      return attribs;
    };


    /** Sanitize HTML applying the appropriate transformations. */
    function sanitizeHtml(htmlText) {
      var out = [];
      htmlSanitizer(htmlText, out);
      return out.join('');
    }
    var htmlSanitizer = html.makeHtmlSanitizer(
        function sanitizeAttributes(tagName, attribs) {
          for (var i = 0; i < attribs.length; i += 2) {
            var attribName = attribs[i];
            var value = attribs[i + 1];
            var atype = null, attribKey;
            if ((attribKey = tagName + ':' + attribName,
                 html4.ATTRIBS.hasOwnProperty(attribKey))
                || (attribKey = '*:' + attribName,
                    html4.ATTRIBS.hasOwnProperty(attribKey))) {
              atype = html4.ATTRIBS[attribKey];
              value = rewriteAttribute(tagName, attribName, atype, value);
            } else {
              value = null;
            }
            if (value !== null && value !== void 0) {
              attribs[i + 1] = value;
            } else {
              attribs.splice(i, 2);
              i -= 2;
            }
          }
          var policy = elementPolicies[tagName];
          if (policy && elementPolicies.hasOwnProperty(tagName)) {
            return policy(attribs);
          }
          return attribs;
        });

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
            var attribName = attribs[i];
            if (attribName === 'target') { continue; }
            var attribKey;
            var atype;
            if ((attribKey = tagName + ':' + attribName,
                html4.ATTRIBS.hasOwnProperty(attribKey))
                || (attribKey = '*:' + attribName,
                    html4.ATTRIBS.hasOwnProperty(attribKey))) {
              atype = html4.ATTRIBS[attribKey];
            } else {
              return '';
            }
            var value = attribs[i + 1];
            switch (atype) {
              case html4.atype.ID:
              case html4.atype.IDREF:
              case html4.atype.IDREFS:
                if (value.length <= idSuffix.length
                    || (idSuffix
                        !== value.substring(value.length - idSuffix.length))) {
                  continue;
                }
                value = value.substring(0, value.length - idSuffix.length);
                break;
            }
            if (value !== null) {
              out.push(' ', attribName, '="', html.escapeAttrib(value), '"');
            }
          }
          out.push('>');
        },
        endTag: function (name, out) { out.push('</', name, '>'); },
        pcdata: function (text, out) { out.push(text); },
        rcdata: function (text, out) { out.push(text); },
        cdata: function (text, out) { out.push(text); }
      });

    var illegalSuffix = /__(?:\s|$)/;
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
        case html4.atype.ID:
        case html4.atype.IDREF:
        case html4.atype.IDREFS:
          value = String(value);
          if (value && !illegalSuffix.test(value) && isXmlName(value)) {
            return value + idSuffix;
          }
          return null;
        case html4.atype.CLASSES:
        case html4.atype.GLOBAL_NAME:
        case html4.atype.LOCAL_NAME:
          value = String(value);
          if (value && !illegalSuffix.test(value) && isXmlNmTokens(value)) {
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
          var pluginId = ___.getId(imports);
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
          // TODO(mikesamuel): determine mime type properly.
          return uriCallback.rewrite(
              value, mimeTypeForAttr(tagName, attribName)) || null;
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
        case html4.atype.FRAME_TARGET:
          // Frames are ambient, so disallow reference.
          return null;
        default:
          return String(value);
      }
    }

    function makeCache() {
      var cache = cajita.newTable(false);
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
    function tameNode(node, editable) {
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
          if (!html4.ELEMENTS.hasOwnProperty(tagName)
              || (html4.ELEMENTS[tagName] & html4.eflags.UNSAFE)) {
            // If an unrecognized node, return a placeholder that
            // doesn't prevent tree navigation, but that doesn't allow
            // mutation or leak attribute information.
            tamed = new TameOpaqueNode(node, editable);
            break;
          }
          switch (tagName) {
            case 'a':
              tamed = new TameAElement(node, editable);
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
            case 'img':
              tamed = new TameImageElement(node, editable);
              break;
            case 'td':
            case 'tr':
            case 'thead':
            case 'tfoot':
            case 'tbody':
            case 'th':
              tamed = new TameTableCompElement(node, editable);
              break;
            case 'table':
              tamed = new TameTableElement(node, editable);
              break;
            default:
              tamed = new TameElement(node, editable);
              break;
          }
          break;
        case 2:  // Attr
          tamed = new TameAttrNode(node, editable);
          break;
        case 3:  // Text
          tamed = new TameTextNode(node, editable);
          break;
        case 8:  // Comment
          tamed = new TameCommentNode(node, editable);
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

    function tameRelatedNode(node, editable) {
      if (node === null || node === void 0) { return null; }
      // catch errors because node might be from a different domain
      try {
        for (var ancestor = node; ancestor; ancestor = ancestor.parentNode) {
          // TODO(mikesamuel): replace with cursors so that subtrees are
          // delegable.
          // TODO: handle multiple classes.
          if (idClass === ancestor.className) {
            return tameNode(node, editable);
          }
        }
      } catch (e) {}
      return null;
    }

    /**
     * Returns a NodeList like object.
     */
    function tameNodeList(nodeList, editable, opt_keyAttrib) {
      var tamed = [];
      var node;

      // Work around NamedNodeMap bugs in IE, Opera, and Safari as discussed
      // at http://code.google.com/p/google-caja/issues/detail?id=935
      var limit = nodeList.length;
      if (limit !== +limit) { limit = 1/0; }
      for (var i = 0; i < limit && (node = nodeList[i]); ++i) {
        node = tameNode(nodeList.item(i), editable);
        tamed[i] = node;
        // Make the node available via its name if doing so would not mask
        // any properties of tamed.
        var key = opt_keyAttrib && node.getAttribute(opt_keyAttrib);
        // TODO(mikesamuel): if key in tamed, we have an ambiguous match.
        // Include neither?  This may happen with radio buttons in a form's
        // elements list.
        if (key && !(key.charAt(key.length - 1) === '_' || (key in tamed)
                     || key === String(key & 0x7fffffff))) {
          tamed[key] = node;
        }
      }
      node = nodeList = untamed = null;

      tamed.item = ___.frozenFunc(function (k) {
        k &= 0x7fffffff;
        if (k !== k) { throw new Error(); }
        return tamed[k] || null;
      });
      // TODO(mikesamuel): if opt_keyAttrib, could implement getNamedItem
      return cajita.freeze(tamed);
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
      return tameNodeList(rootNode.getElementsByTagName(tagName), editable);
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
        if (illegalSuffix.test(classi) || !isXmlNmTokens(classi)) {
          classes[i] = classes[nClasses - 1];
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
            rootNode.getElementsByClassName(classes.join(' ')), editable);
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
          var tamed = tameNode(candidate, editable);
          if (tamed) {
            matches[++k] = tamed;
          }
        }
        // "the method must return a live NodeList object"
        return fakeNodeList(matches);
      }
    }

    function makeEventHandlerWrapper(thisNode, listener) {
      if ('function' !== typeof listener
          // Allow disfunctions
          && !('object' === (typeof listener) && listener !== null
               && ___.canCallPub(listener, 'call'))) {
        throw new Error('Expected function not ' + typeof listener);
      }
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
      var wrappedListener;
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
      ___.stamp(tameNodeTrademark, this, true);
      classUtils.exportFields(this, tameNodeFields);
    }
    TameNode.prototype.getOwnerDocument = function () {
      // TODO(mikesamuel): upward navigation breaks capability discipline.
      if (!this.editable___ && tameDocument.editable___) {
        throw new Error(NOT_EDITABLE);
      }
      return tameDocument;
    };
    nodeClasses.Node = TameNode;
    ___.ctor(TameNode, void 0, 'TameNode');
    // abstract TameNode.prototype.getNodeType
    // abstract TameNode.prototype.getNodeName
    // abstract TameNode.prototype.getNodeValue
    // abstract TameNode.prototype.cloneNode
    // abstract TameNode.prototype.appendChild
    // abstract TameNode.prototype.insertBefore
    // abstract TameNode.prototype.removeChild
    // abstract TameNode.prototype.replaceChild
    // abstract TameNode.prototype.getFirstChild
    // abstract TameNode.prototype.getLastChild
    // abstract TameNode.prototype.getNextSibling
    // abstract TameNode.prototype.getPreviousSibling
    // abstract TameNode.prototype.getParentNode
    // abstract TameNode.prototype.getElementsByTagName
    // abstract TameNode.prototype.getElementsByClassName
    // abstract TameNode.prototype.getChildNodes
    // abstract TameNode.prototype.getAttributes
    var tameNodeMembers = [
        'getNodeType', 'getNodeValue', 'getNodeName', 'cloneNode',
        'appendChild', 'insertBefore', 'removeChild', 'replaceChild',
        'getFirstChild', 'getLastChild', 'getNextSibling', 'getPreviousSibling',
        'getElementsByClassName', 'getElementsByTagName',
        'getOwnerDocument',
        'dispatchEvent',
        'hasChildNodes'
        ];


    /**
     * A tame node that is backed by a real node.
     * @constructor
     */
    function TameBackedNode(node, editable) {
      if (!node) {
        throw new Error('Creating tame node with undefined native delegate');
      }
      this.node___ = node;
      TameNode.call(this, editable);
    }
    classUtils.extend(TameBackedNode, TameNode);
    TameBackedNode.prototype.getNodeType = function () {
      return this.node___.nodeType;
    };
    TameBackedNode.prototype.getNodeName = function () {
      return this.node___.nodeName;
    };
    TameBackedNode.prototype.getNodeValue = function () {
      return this.node___.nodeValue;
    };
    TameBackedNode.prototype.cloneNode = function (deep) {
      var clone = bridal.cloneNode(this.node___, Boolean(deep));
      // From http://www.w3.org/TR/DOM-Level-2-Core/core.html#ID-3A0ED0A4
      //     "Note that cloning an immutable subtree results in a mutable copy"
      return tameNode(clone, true);
    };
    TameBackedNode.prototype.appendChild = function (child) {
      // Child must be editable since appendChild can remove it from its parent.
      cajita.guard(tameNodeTrademark, child);
      if (!this.editable___ || !child.editable___) {
        throw new Error(NOT_EDITABLE);
      }
      this.node___.appendChild(child.node___);
    };
    TameBackedNode.prototype.insertBefore = function (toInsert, child) {
      cajita.guard(tameNodeTrademark, toInsert);
      if (child === void 0) { child = null; }
      if (child !== null) { cajita.guard(tameNodeTrademark, child); }
      if (!this.editable___ || !toInsert.editable___) {
        throw new Error(NOT_EDITABLE);
      }
      this.node___.insertBefore(
          toInsert.node___, child !== null ? child.node___ : null);
    };
    TameBackedNode.prototype.removeChild = function (child) {
      cajita.guard(tameNodeTrademark, child);
      if (!this.editable___ || !child.editable___) {
        throw new Error(NOT_EDITABLE);
      }
      this.node___.removeChild(child.node___);
    };
    TameBackedNode.prototype.replaceChild = function (child, replacement) {
      cajita.guard(tameNodeTrademark, child);
      cajita.guard(tameNodeTrademark, replacement);
      if (!this.editable___ || !replacement.editable___) {
        throw new Error(NOT_EDITABLE);
      }
      this.node___.replaceChild(child.node___, replacement.node___);
    };
    TameBackedNode.prototype.getFirstChild = function () {
      return tameNode(this.node___.firstChild, this.editable___);
    };
    TameBackedNode.prototype.getLastChild = function () {
      return tameNode(this.node___.lastChild, this.editable___);
    };
    TameBackedNode.prototype.getNextSibling = function () {
      // TODO(mikesamuel): replace with cursors so that subtrees are delegable
      return tameNode(this.node___.nextSibling, this.editable___);
    };
    TameBackedNode.prototype.getPreviousSibling = function () {
      // TODO(mikesamuel): replace with cursors so that subtrees are delegable
      return tameNode(this.node___.previousSibling, this.editable___);
    };
    TameBackedNode.prototype.getParentNode = function () {
      var parent = this.node___.parentNode;
      if (parent === tameDocument.body___) {
        if (tameDocument.editable___ && !this.editable___) {
          // FIXME: return a non-editable version of body.
          throw new Error(NOT_EDITABLE);
        }
        return tameDocument.getBody();
      }
      return tameRelatedNode(this.node___.parentNode, this.editable___);
    };
    TameBackedNode.prototype.getElementsByTagName = function (tagName) {
      return tameGetElementsByTagName(this.node___, tagName, this.editable___);
    };
    TameBackedNode.prototype.getElementsByClassName = function (className) {
      return tameGetElementsByClassName(
          this.node___, className, this.editable___);
    };
    TameBackedNode.prototype.getChildNodes = function () {
      return tameNodeList(this.node___.childNodes, this.editable___);
    };
    TameBackedNode.prototype.getAttributes = function () {
      return tameNodeList(this.node___.attributes, this.editable___);
    };
    var endsWith__ = /__$/;
    // TODO(erights): Come up with some notion of a keeper chain so we can
    // say, "let every other keeper try to handle this first".
    TameBackedNode.prototype.handleRead___ = function (name) {
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
      var handlerName = name + '_setter___';
      if (this[handlerName]) {
        return this[handlerName](val);
      }
      handlerName = handlerName.toLowerCase();
      if (this[handlerName]) {
        return this[handlerName](val);
      }
      if (!this.node___.properties___) {
        this.node___.properties___ = {};
      }
      this[name + '_canEnum___'] = true;
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
            && delete this[name + '_canEnum___']);
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
        return cajita.allKeys(this.node___.properties___);
      }
      return [];
    };
    TameBackedNode.prototype.hasChildNodes = function () {
      return !!this.node___.hasChildNodes();
    };
    // http://www.w3.org/TR/DOM-Level-2-Events/events.html#Events-EventTarget :
    // "The EventTarget interface is implemented by all Nodes"
    TameBackedNode.prototype.dispatchEvent = function dispatchEvent(evt) {
      cajita.guard(tameEventTrademark, evt);
      bridal.dispatchEvent(this.node___, evt.event___);
    };
    ___.ctor(TameBackedNode, TameNode, 'TameBackedNode');
    ___.all2(___.grantTypedGeneric, TameBackedNode.prototype, tameNodeMembers);

    /**
     * A fake node that is not backed by a real DOM node.
     * @constructor
     */
    function TamePseudoNode(editable) {
      TameNode.call(this, editable);
      this.properties___ = {};
    }
    classUtils.extend(TamePseudoNode, TameNode);
    TamePseudoNode.prototype.appendChild =
    TamePseudoNode.prototype.insertBefore =
    TamePseudoNode.prototype.removeChild =
    TamePseudoNode.prototype.replaceChild =
        function (child) { throw new Error(NOT_EDITABLE); };
    TamePseudoNode.prototype.getFirstChild = function () {
      var children = this.getChildNodes();
      return children.length ? children[0] : null;
    };
    TamePseudoNode.prototype.getLastChild = function () {
      var children = this.getChildNodes();
      return children.length ? children[children.length - 1] : null;
    };
    TamePseudoNode.prototype.getNextSibling = function () {
      var parentNode = this.getParentNode();
      if (!parentNode) { return null; }
      var siblings = parentNode.getChildNodes();
      for (var i = siblings.length - 1; --i >= 0;) {
        if (siblings[i] === this) { return siblings[i + 1]; }
      }
      return null;
    };
    TamePseudoNode.prototype.getPreviousSibling = function () {
      var parentNode = this.getParentNode();
      if (!parentNode) { return null; }
      var siblings = parentNode.getChildNodes();
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
      this[name + '_canEnum___'] = true;
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
            && delete this[name + '_canEnum___']);
      } else {
        return true;
      }
    };
    TamePseudoNode.prototype.handleEnum___ = function (ownFlag) {
      // TODO(metaweta): Add code to list all the other handled stuff we know
      // about.
      if (this.properties___) {
        return cajita.allKeys(this.properties___);
      }
      return [];
    };
    TamePseudoNode.prototype.hasChildNodes = function () {
      return this.getFirstChild() != null;
    };
    ___.ctor(TamePseudoNode, TameNode, 'TamePseudoNode');
    ___.all2(___.grantTypedGeneric, TamePseudoNode.prototype, tameNodeMembers);


    function TamePseudoElement(
        tagName, tameDoc, childNodesGetter, parentNodeGetter, innerHTMLGetter,
        editable) {
      TamePseudoNode.call(this, editable);
      this.tagName___ = tagName;
      this.tameDoc___ = tameDoc;
      this.childNodesGetter___ = childNodesGetter;
      this.parentNodeGetter___ = parentNodeGetter;
      this.innerHTMLGetter___ = innerHTMLGetter;
      classUtils.exportFields(this, ['tagName', 'innerHTML']);
    }
    classUtils.extend(TamePseudoElement, TamePseudoNode);
    // TODO(mikesamuel): make nodeClasses work.
    TamePseudoElement.prototype.getNodeType = function () { return 1; };
    TamePseudoElement.prototype.getNodeName
        = function () { return this.tagName___; };
    TamePseudoElement.prototype.getTagName
        = function () { return this.tagName___; };
    TamePseudoElement.prototype.getNodeValue = function () { return null; };
    TamePseudoElement.prototype.getAttribute
        = function (attribName) { return ''; };
    TamePseudoElement.prototype.hasAttribute
        = function (attribName) { return false; };
    TamePseudoElement.prototype.getOwnerDocument
        = function () { return this.tameDoc___; };
    TamePseudoElement.prototype.getChildNodes
        = function () { return this.childNodesGetter___(); };
    TamePseudoElement.prototype.getAttributes
        = function () { return tameNodeList([], false); };
    TamePseudoElement.prototype.getParentNode
        = function () { return this.parentNodeGetter___(); };
    TamePseudoElement.prototype.getInnerHTML
        = function () { return this.innerHTMLGetter___(); };
    TamePseudoElement.prototype.getElementsByTagName = function (tagName) {
      tagName = String(tagName).toLowerCase();
      if (tagName === this.tagName___) {
        // Works since html, head, body, and title can't contain themselves.
        return fakeNodeList([]);
      }
      return this.getOwnerDocument().getElementsByTagName(tagName);
    };
    TamePseudoElement.prototype.getElementsByClassName = function (className) {
      return this.getOwnerDocument().getElementsByClassName(className);
    };
    TamePseudoElement.prototype.toString = function () {
      return '<' + this.tagName___ + '>';
    };
    ___.ctor(TamePseudoElement, TamePseudoNode, 'TamePseudoElement');
    ___.all2(___.grantTypedGeneric, TamePseudoElement.prototype,
             ['getTagName', 'getAttribute', 'hasAttribute']);


    function TameOpaqueNode(node, editable) {
      TameBackedNode.call(this, node, editable);
    }
    classUtils.extend(TameOpaqueNode, TameBackedNode);
    TameOpaqueNode.prototype.getNodeValue
        = TameBackedNode.prototype.getNodeValue;
    TameOpaqueNode.prototype.getNodeType
        = TameBackedNode.prototype.getNodeType;
    TameOpaqueNode.prototype.getNodeName
        = TameBackedNode.prototype.getNodeName;
    TameOpaqueNode.prototype.getNextSibling
        = TameBackedNode.prototype.getNextSibling;
    TameOpaqueNode.prototype.getPreviousSibling
        = TameBackedNode.prototype.getPreviousSibling;
    TameOpaqueNode.prototype.getFirstChild
        = TameBackedNode.prototype.getFirstChild;
    TameOpaqueNode.prototype.getLastChild
        = TameBackedNode.prototype.getLastChild;
    TameOpaqueNode.prototype.getParentNode
        = TameBackedNode.prototype.getParentNode;
    TameOpaqueNode.prototype.getChildNodes
        = TameBackedNode.prototype.getChildNodes;
    TameOpaqueNode.prototype.getAttributes
        = function () { return tameNodeList([], false); };
    for (var i = tameNodeMembers.length; --i >= 0;) {
      var k = tameNodeMembers[i];
      if (!TameOpaqueNode.prototype.hasOwnProperty(k)) {
        TameOpaqueNode.prototype[k] = ___.frozenFunc(function () {
          throw new Error('Node is opaque');
        });
      }
    }
    ___.all2(___.grantTypedGeneric, TameOpaqueNode.prototype, tameNodeMembers);

    function TameAttrNode(node, editable) {
      assert(node.nodeType === 2);
      TameBackedNode.call(this, node, editable);
      classUtils.exportFields(
          this, ['name', 'nodeValue', 'value', 'specified']);
    }
    classUtils.extend(TameAttrNode, TameBackedNode);
    nodeClasses.Attr = TameAttrNode;
    TameAttrNode.prototype.setNodeValue = function (value) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.nodeValue = String(value || '');
      return value;
    };
    TameAttrNode.prototype.getName = TameAttrNode.prototype.getNodeName;
    TameAttrNode.prototype.getValue = TameAttrNode.prototype.getNodeValue;
    TameAttrNode.prototype.setValue = TameAttrNode.prototype.setNodeValue;
    TameAttrNode.prototype.getSpecified = function () {
      return this.node___.specified;
    };
    TameAttrNode.prototype.toString = function () {
      return '#attr';
    };
    ___.ctor(TameAttrNode, TameBackedNode, 'TameAttrNode');

    function TameTextNode(node, editable) {
      assert(node.nodeType === 3);
      TameBackedNode.call(this, node, editable);
      classUtils.exportFields(this, ['nodeValue', 'data']);
    }
    classUtils.extend(TameTextNode, TameBackedNode);
    nodeClasses.Text = TameTextNode;
    TameTextNode.prototype.setNodeValue = function (value) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.nodeValue = String(value || '');
      return value;
    };
    TameTextNode.prototype.getData = TameTextNode.prototype.getNodeValue;
    TameTextNode.prototype.setData = TameTextNode.prototype.setNodeValue;
    TameTextNode.prototype.toString = function () {
      return '#text';
    };
    ___.ctor(TameTextNode, TameBackedNode, 'TameTextNode');
    ___.all2(___.grantTypedGeneric, TameTextNode.prototype,
             ['setNodeValue', 'getData', 'setData']);

    function TameCommentNode(node, editable) {
      assert(node.nodeType === 8);
      TameBackedNode.call(this, node, editable);
    }
    classUtils.extend(TameCommentNode, TameBackedNode);
    nodeClasses.CommentNode = TameCommentNode;
    TameCommentNode.prototype.toString = function () {
      return '#comment';
    };
    ___.ctor(TameCommentNode, TameBackedNode, 'TameCommentNode');

    function getAttributeType(tagName, attribName){
      var attribKey;
      attribKey = tagName + ':' + attribName;
      if (html4.ATTRIBS.hasOwnProperty(attribKey)) {
        return html4.ATTRIBS[attribKey];
      }
      attribKey = '*:' + attribName;
      if (html4.ATTRIBS.hasOwnProperty(attribKey)) {
        return html4.ATTRIBS[attribKey];
      }
      return void 0;
    }

    function TameElement(node, editable) {
      assert(node.nodeType === 1);
      TameBackedNode.call(this, node, editable);
      classUtils.exportFields(
          this,
          ['className', 'id', 'innerHTML', 'tagName', 'style',
           'clientWidth', 'clientHeight',
           'offsetLeft', 'offsetTop', 'offsetWidth', 'offsetHeight',
           'offsetParent',
           'scrollLeft', 'scrollTop',
           'scrollWidth', 'scrollHeight',
           'title',
           'dir']);
    }
    classUtils.extend(TameElement, TameBackedNode);
    nodeClasses.Element = nodeClasses.HTMLElement = TameElement;
    TameElement.prototype.getId = function () {
      return this.getAttribute('id') || '';
    };
    TameElement.prototype.setId = function (newId) {
      return this.setAttribute('id', newId);
    };
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
      var value = this.node___.getAttribute(attribName);
      if ('string' !== typeof value) { return value; }
      switch (atype) {
        case html4.atype.ID:
        case html4.atype.IDREF:
        case html4.atype.IDREFS:
          if (!value) { return null; }
          var n = idSuffix.length;
          var len = value.length;
          var end = len - n;
          if (end > 0 && idSuffix === value.substring(end, len)) {
            return value.substring(0, end);
          }
          return null;
        default:
          if ('' === value) {
            // IE creates attribute nodes for any attribute in the HTML schema
            // so even when they are deleted, there will be a value, usually
            // the empty string.
            var attr = this.node___.getAttributeNode(attribName);
            if (attr && !attr.specified) { return null; }
          }
          return value;
      }
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
        var node = this.node___;
        if (node.hasAttribute) {  // Non IE
          return node.hasAttribute(attribName);
        } else {
          var attr = node.getAttributeNode(attribName);
          return attr !== null && attr.specified;
        }
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
    TameElement.prototype.getClassName = function () {
      return this.getAttribute('class') || '';
    };
    TameElement.prototype.setClassName = function (classes) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      return this.setAttribute('class', String(classes));
    };
    TameElement.prototype.getTitle = function () {
      return this.getAttribute('title') || '';
    };
    TameElement.prototype.setTitle = function (classes) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      return this.setAttribute('title', String(classes));
    };
    TameElement.prototype.getDir = function () {
      return this.getAttribute('dir') || '';
    };
    TameElement.prototype.setDir = function (classes) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      return this.setAttribute('dir', String(classes));
    };
    TameElement.prototype.getTagName = TameBackedNode.prototype.getNodeName;
    TameElement.prototype.getInnerHTML = function () {
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
        // TODO(mikesamuel): for RCDATA we only need to escape & if they're not
        // part of an entity.
        innerHtml = html.normalizeRCData(innerHtml);
      } else {
        innerHtml = tameInnerHtml(innerHtml);
      }
      return innerHtml;
    };
    TameElement.prototype.setInnerHTML = function (htmlFragment) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      var tagName = this.node___.tagName.toLowerCase();
      if (!html4.ELEMENTS.hasOwnProperty(tagName)) { throw new Error(); }
      var flags = html4.ELEMENTS[tagName];
      if (flags & html4.eflags.UNSAFE) { throw new Error(); }
      var sanitizedHtml;
      if (flags & html4.eflags.RCDATA) {
        sanitizedHtml = html.normalizeRCData(String(htmlFragment || ''));
      } else {
        sanitizedHtml = (htmlFragment instanceof Html
                        ? safeHtml(htmlFragment)
                        : sanitizeHtml(String(htmlFragment || '')));
      }
      this.node___.innerHTML = sanitizedHtml;
      return htmlFragment;
    };
    TameElement.prototype.setStyle = function (style) {
      this.setAttribute('style', style);
      return this.getStyle();
    };
    TameElement.prototype.getStyle = function () {
      return new TameStyle(this.node___.style, this.editable___);
    };
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

    TameElement.prototype.getClientWidth = function () {
      return this.node___.clientWidth;
    };
    TameElement.prototype.getClientHeight = function () {
      return this.node___.clientHeight;
    };
    TameElement.prototype.getOffsetLeft = function () {
      return this.node___.offsetLeft;
    };
    TameElement.prototype.getOffsetTop = function () {
      return this.node___.offsetTop;
    };
    TameElement.prototype.getOffsetWidth = function () {
      return this.node___.offsetWidth;
    };
    TameElement.prototype.getOffsetHeight = function () {
      return this.node___.offsetHeight;
    };
    TameElement.prototype.getOffsetParent = function () {
      return tameRelatedNode(this.node___.offsetParent, this.editable___);
    };
    TameElement.prototype.getScrollLeft = function () {
      return this.node___.scrollLeft;
    };
    TameElement.prototype.getScrollTop = function () {
      return this.node___.scrollTop;
    };
    TameElement.prototype.setScrollLeft = function (x) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.scrollLeft = +x;
      return x;
    };
    TameElement.prototype.setScrollTop = function (y) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.scrollTop = +y;
      return y;
    };
    TameElement.prototype.getScrollWidth = function () {
      return this.node___.scrollWidth;
    };
    TameElement.prototype.getScrollHeight = function () {
      return this.node___.scrollHeight;
    };
    TameElement.prototype.toString = function () {
      return '<' + this.node___.tagName + '>';
    };
    TameElement.prototype.addEventListener = tameAddEventListener;
    TameElement.prototype.removeEventListener = tameRemoveEventListener;
    ___.ctor(TameElement, TameBackedNode, 'TameElement');
    ___.all2(
       ___.grantTypedGeneric, TameElement.prototype,
       ['addEventListener', 'removeEventListener',
        'getAttribute', 'setAttribute',
        'removeAttribute',
        'hasAttribute',
        'getClassName', 'setClassName', 'getId', 'setId',
        'getInnerHTML', 'setInnerHTML', 'updateStyle', 'getStyle', 'setStyle',
        'getTagName']);

    // Register set handlers for onclick, onmouseover, etc.
    (function () {
      var attrNameRe = /:(.*)/;
      for (var html4Attrib in html4.ATTRIBS) {
        if (html4.atype.SCRIPT === html4.ATTRIBS[html4Attrib]) {
          (function (attribName) {
            ___.useSetHandler(
                TameElement.prototype,
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
                    return listener;
                  }
                });
           })(html4Attrib.match(attrNameRe)[1]);
        }
      }
    })();

    function TameAElement(node, editable) {
      TameElement.call(this, node, editable);
      classUtils.exportFields(this, ['href']);
    }
    classUtils.extend(TameAElement, TameElement);
    nodeClasses.HTMLAnchorElement = TameAElement;
    TameAElement.prototype.focus = function () {
      this.node___.focus();
    };
    TameAElement.prototype.getHref = function () {
      return this.node___.href;
    };
    TameAElement.prototype.setHref = function (href) {
      this.setAttribute('href', href);
      return href;
    };
    ___.ctor(TameAElement, TameElement, 'TameAElement');
    ___.all2(___.grantTypedGeneric, TameAElement.prototype,
             ['getHref', 'setHref', 'focus']);

    // http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-40002357
    function TameFormElement(node, editable) {
      TameElement.call(this, node, editable);
      this.length = node.length;
      classUtils.exportFields(
          this,
          ['action', 'elements', 'enctype', 'method', 'target']);
    }
    classUtils.extend(TameFormElement, TameElement);
    nodeClasses.HTMLFormElement = TameFormElement;
    TameFormElement.prototype.submit = function () {
      return this.node___.submit();
    };
    TameFormElement.prototype.reset = function () {
      return this.node___.reset();
    };
    TameFormElement.prototype.getAction = function () {
      return this.getAttribute('action') || '';
    };
    TameFormElement.prototype.setAction = function (newVal) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      return this.setAttribute('action', String(newVal));
    };
    TameFormElement.prototype.getElements = function () {
      return tameNodeList(this.node___.elements, this.editable___, 'name');
    };
    TameFormElement.prototype.getEnctype = function () {
      return this.getAttribute('enctype') || '';
    };
    TameFormElement.prototype.setEnctype = function (newVal) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      return this.setAttribute('enctype', String(newVal));
    };
    TameFormElement.prototype.getMethod = function () {
      return this.getAttribute('method') || '';
    };
    TameFormElement.prototype.setMethod = function (newVal) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      return this.setAttribute('method', String(newVal));
    };
    TameFormElement.prototype.getTarget = function () {
      return this.getAttribute('target') || '';
    };
    TameFormElement.prototype.setTarget = function (newVal) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      return this.setAttribute('target', String(newVal));
    };
    TameFormElement.prototype.reset = function () {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.reset();
    };
    TameFormElement.prototype.submit = function () {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.submit();
    };
    ___.ctor(TameFormElement, TameElement, 'TameFormElement');
    ___.all2(___.grantTypedGeneric, TameFormElement.prototype,
             ['getElements', 'reset', 'submit']);


    function TameInputElement(node, editable) {
      TameElement.call(this, node, editable);
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
    classUtils.extend(TameInputElement, TameElement);
    nodeClasses.HTMLInputElement = TameInputElement;
    TameInputElement.prototype.getChecked = function () {
      return this.node___.checked;
    };
    TameInputElement.prototype.setChecked = function (checked) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      return (this.node___.checked = !!checked);
    };
    TameInputElement.prototype.getValue = function () {
      // For <option> elements, Firefox returns a value even when no value
      // attribute is present, using the contained text, but IE does not.
      var value = this.node___.value;
      return value === null || value === void 0 ? null : String(value);
    };
    TameInputElement.prototype.setValue = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.value = (
          newValue === null || newValue === void 0 ? '' : '' + newValue);
      return newValue;
    };
    TameInputElement.prototype.getDefaultValue = function () {
      var value = this.node___.defaultValue;
      return value === null || value === void 0 ? null : String(value);
    };
    TameInputElement.prototype.setDefaultValue = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.defaultValue = (
          newValue === null || newValue === void 0 ? '' : '' + newValue);
      return newValue;
    };
    TameInputElement.prototype.focus = function () {
      this.node___.focus();
    };
    TameInputElement.prototype.blur = function () {
      this.node___.blur();
    };
    TameInputElement.prototype.select = function () {
      this.node___.select();
    };
    TameInputElement.prototype.getForm = function () {
      return tameRelatedNode(this.node___.form, this.editable___);
    };
    TameInputElement.prototype.getDisabled = function () {
      return this.node___.disabled;
    };
    TameInputElement.prototype.setDisabled = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.disabled = newValue;
      return newValue;
    };
    TameInputElement.prototype.getReadOnly = function () {
      return this.node___.readOnly;
    };
    TameInputElement.prototype.setReadOnly = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.readOnly = newValue;
      return newValue;
    };
    TameInputElement.prototype.getOptions = function () {
      return tameNodeList(this.node___.options, this.editable___, 'name');
    };
    TameInputElement.prototype.getDefaultSelected = function () {
      return this.node___.defaultSelected;
    };
    TameInputElement.prototype.setDefaultSelected = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.defaultSelected = !!newValue;
      return newValue;
    };
    TameInputElement.prototype.getSelected = function () {
      return this.node___.selected;
    };
    TameInputElement.prototype.setSelected = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.selected = newValue;
      return newValue;
    };
    TameInputElement.prototype.getSelectedIndex = function () {
      return this.node___.selectedIndex;
    };
    TameInputElement.prototype.setSelectedIndex = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.selectedIndex = (newValue | 0);
      return newValue;
    };
    TameInputElement.prototype.getName = function () {
      return this.node___.name;
    };
    TameInputElement.prototype.setName = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.name = newValue;
      return newValue;
    };
    TameInputElement.prototype.getAccessKey = function () {
      return this.node___.accessKey;
    };
    TameInputElement.prototype.setAccessKey = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.accessKey = newValue;
      return newValue;
    };
    TameInputElement.prototype.getTabIndex = function () {
      return this.node___.tabIndex;
    };
    TameInputElement.prototype.getText = function () {
        return String(this.node___.text);
    };
    TameInputElement.prototype.setTabIndex = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.tabIndex = newValue;
      return newValue;
    };
    TameInputElement.prototype.getDefaultChecked = function () {
      return this.node___.defaultChecked;
    };
    TameInputElement.prototype.setDefaultChecked = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.defaultChecked = newValue;
      return newValue;
    };
    TameInputElement.prototype.getMaxLength = function () {
      return this.node___.maxLength;
    };
    TameInputElement.prototype.setMaxLength = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.maxLength = newValue;
      return newValue;
    };
    TameInputElement.prototype.getSize = function () {
      return this.node___.size;
    };
    TameInputElement.prototype.setSize = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.size = newValue;
      return newValue;
    };
    TameInputElement.prototype.getType = function () {
      return String(this.node___.type);
    };
    TameInputElement.prototype.setType = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.type = newValue;
      return newValue;
    };
    TameInputElement.prototype.getIndex = function () {
      return this.node___.index;
    };
    TameInputElement.prototype.setIndex = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.index = newValue;
      return newValue;
    };
    TameInputElement.prototype.getLabel = function () {
      return this.node___.label;
    };
    TameInputElement.prototype.setLabel = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.label = newValue;
      return newValue;
    };
    TameInputElement.prototype.getMultiple = function () {
      return this.node___.multiple;
    };
    TameInputElement.prototype.setMultiple = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.multiple = newValue;
      return newValue;
    };
    TameInputElement.prototype.getCols = function () {
      return this.node___.cols;
    };
    TameInputElement.prototype.setCols = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.cols = newValue;
      return newValue;
    };
    TameInputElement.prototype.getRows = function () {
      return this.node___.rows;
    };
    TameInputElement.prototype.setRows = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.rows = newValue;
      return newValue;
    };
    ___.ctor(TameInputElement, TameElement, 'TameInputElement');
    ___.all2(___.grantTypedGeneric, TameInputElement.prototype,
             ['getValue', 'setValue', 'focus', 'getForm', 'getType', 'select']);


    function TameImageElement(node, editable) {
      TameElement.call(this, node, editable);
      classUtils.exportFields(this, ['src', 'alt']);
    }
    classUtils.extend(TameImageElement, TameElement);
    nodeClasses.HTMLImageElement = TameImageElement;
    TameImageElement.prototype.getSrc = function () {
      return this.node___.src;
    };
    TameImageElement.prototype.setSrc = function (src) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.setAttribute('src', src);
      return src;
    };
    TameImageElement.prototype.getAlt = function () {
      return this.node___.alt;
    };
    TameImageElement.prototype.setAlt = function (src) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.alt = src;
      return src;
    };
    ___.ctor(TameImageElement, TameElement, 'TameImageElement');
    ___.all2(___.grantTypedGeneric, TameImageElement.prototype,
             ['getSrc', 'setSrc', 'getAlt', 'setAlt']);


    function TameTableCompElement(node, editable) {
      TameElement.call(this, node, editable);
      classUtils.exportFields(
          this,
          ['colSpan','cells','rowSpan','rows','rowIndex','align',
           'vAlign','nowrap']);
    }
    classUtils.extend(TameTableCompElement, TameElement);
    TameTableCompElement.prototype.getColSpan = function () {
      return this.node___.colSpan;
    };
    TameTableCompElement.prototype.setColSpan = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.colSpan = newValue;
      return newValue;
    };
    TameTableCompElement.prototype.getCells = function () {
      return tameNodeList(this.node___.cells, this.editable___);
    };
    TameTableCompElement.prototype.getRowSpan = function () {
      return this.node___.rowSpan;
    };
    TameTableCompElement.prototype.setRowSpan = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.rowSpan = newValue;
      return newValue;
    };
    TameTableCompElement.prototype.getRows = function () {
      return tameNodeList(this.node___.rows, this.editable___);
    };
    TameTableCompElement.prototype.getRowIndex = function () {
      return this.node___.rowIndex;
    };
    TameTableCompElement.prototype.getAlign = function () {
      return this.node___.align;
    };
    TameTableCompElement.prototype.setAlign = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.align = newValue;
      return newValue;
    };
    TameTableCompElement.prototype.getVAlign = function () {
      return this.node___.vAlign;
    };
    TameTableCompElement.prototype.setVAlign = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.vAlign = newValue;
      return newValue;
    };
    TameTableCompElement.prototype.getNowrap = function () {
      return this.node___.nowrap;
    };
    TameTableCompElement.prototype.setNowrap = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.nowrap = newValue;
      return newValue;
    };
    ___.ctor(TameTableCompElement, TameElement, 'TameTableCompElement');


    function TameTableElement(node, editable) {
      TameTableCompElement.call(this, node, editable);
      classUtils.exportFields(this, ['tBodies','tHead','tFoot']);
    }
    classUtils.extend(TameTableElement, TameTableCompElement);
    nodeClasses.HTMLTableElement = TameTableElement;
    TameTableElement.prototype.getTBodies = function () {
      return tameNodeList(this.node___.tBodies, this.editable___);
    };
    TameTableElement.prototype.getTHead = function () {
      return tameNode(this.node___.tHead, this.editable___);
    };
    TameTableElement.prototype.getTFoot = function () {
      return tameNode(this.node___.tFoot, this.editable___);
    };
    TameTableElement.prototype.createTHead = function () {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      return tameNode(this.node___.createTHead(), this.editable___);
    };
    TameTableElement.prototype.deleteTHead = function () {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.deleteTHead();
    };
    TameTableElement.prototype.createTFoot = function () {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      return tameNode(this.node___.createTFoot(), this.editable___);
    };
    TameTableElement.prototype.deleteTFoot = function () {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.deleteTFoot();
    };
    ___.ctor(TameTableElement, TameTableCompElement, 'TameTableElement');
    ___.all2(___.grantTypedGeneric, TameTableElement.prototype,
             ['createTHead', 'deleteTHead','createTFoot', 'deleteTFoot']);

    function tameEvent(event) {
      if (event.tamed___) { return event.tamed___; }
      return event.tamed___ = new TameEvent(event);
    }

    function TameEvent(event) {
      this.event___ = event;
      ___.stamp(tameEventTrademark, this, true);
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
    nodeClasses.Event = TameEvent;
    TameEvent.prototype.getType = function () {
      return bridal.untameEventType(String(this.event___.type));
    };
    TameEvent.prototype.getTarget = function () {
      var event = this.event___;
      return tameRelatedNode(event.target || event.srcElement, true);
    };
    TameEvent.prototype.getSrcElement = function () {
      return tameRelatedNode(this.event___.srcElement, true);
    };
    TameEvent.prototype.getCurrentTarget = function () {
      var e = this.event___;
      return tameRelatedNode(e.currentTarget, true);
    };
    TameEvent.prototype.getRelatedTarget = function () {
      var e = this.event___;
      var t = e.relatedTarget;
      if (!t) {
        if (e.type === 'mouseout') {
          t = e.toElement;
        } else if (e.type === 'mouseover') {
          t = e.fromElement;
        }
      }
      return tameRelatedNode(t, true);
    };
    TameEvent.prototype.getFromElement = function () {
      return tameRelatedNode(this.event___.fromElement, true);
    };
    TameEvent.prototype.getToElement = function () {
      return tameRelatedNode(this.event___.toElement, true);
    };
    TameEvent.prototype.getPageX = function () {
      return Number(this.event___.pageX);
    };
    TameEvent.prototype.getPageY = function () {
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
    TameEvent.prototype.getAltKey = function () {
      return Boolean(this.event___.altKey);
    };
    TameEvent.prototype.getCtrlKey = function () {
      return Boolean(this.event___.ctrlKey);
    };
    TameEvent.prototype.getMetaKey = function () {
      return Boolean(this.event___.metaKey);
    };
    TameEvent.prototype.getShiftKey = function () {
      return Boolean(this.event___.shiftKey);
    };
    TameEvent.prototype.getButton = function () {
      var e = this.event___;
      return e.button && Number(e.button);
    };
    TameEvent.prototype.getClientX = function () {
      return Number(this.event___.clientX);
    };
    TameEvent.prototype.getClientY = function () {
      return Number(this.event___.clientY);
    };
    TameEvent.prototype.getScreenX = function () {
      return Number(this.event___.screenX);
    };
    TameEvent.prototype.getScreenY = function () {
      return Number(this.event___.screenY);
    };
    TameEvent.prototype.getWhich = function () {
      var w = this.event___.which;
      return w && Number(w);
    };
    TameEvent.prototype.getKeyCode = function () {
      var kc = this.event___.keyCode;
      return kc && Number(kc);
    };
    TameEvent.prototype.toString = function () { return '[Fake Event]'; };
    ___.ctor(TameEvent, void 0, 'TameEvent');
    ___.all2(___.grantTypedGeneric, TameEvent.prototype,
             ['getType', 'getTarget', 'getPageX', 'getPageY', 'stopPropagation',
              'getAltKey', 'getCtrlKey', 'getMetaKey', 'getShiftKey',
              'getButton', 'getClientX', 'getClientY',
              'getScreenX', 'getScreenY',
              'getRelatedTarget',
              'getFromElement', 'getToElement',
              'getSrcElement',
              'preventDefault',
              'getKeyCode', 'getWhich']);

    function TameCustomHTMLEvent(event) {
      TameEvent.call(this, event);
      this.properties___ = {};
    }
    classUtils.extend(TameCustomHTMLEvent, TameEvent);
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
      handlerName = handlerName.toLowerCase();
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
      handlerName = handlerName.toLowerCase();
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
      handlerName = handlerName.toLowerCase();
      if (this[handlerName]) {
        return this[handlerName](val);
      }
      if (!this.event___.properties___) {
        this.event___.properties___ = {};
      }
      this[name + '_canEnum___'] = true;
      return this.event___.properties___[name] = val;
    };
    TameCustomHTMLEvent.prototype.handleDelete___ = function (name) {
      name = String(name);
      if (endsWith__.test(name)) { throw new Error(INVALID_SUFFIX); }
      var handlerName = name + '_deleter___';
      if (this[handlerName]) {
        return this[handlerName]();
      }
      handlerName = handlerName.toLowerCase();
      if (this[handlerName]) {
        return this[handlerName]();
      }
      if (this.event___.properties___) {
        return (
            delete this.event___.properties___[name]
            && delete this[name + '_canEnum___']);
      } else {
        return true;
      }
    };
    TameCustomHTMLEvent.prototype.handleEnum___ = function (ownFlag) {
      // TODO(metaweta): Add code to list all the other handled stuff we know
      // about.
      if (this.event___.properties___) {
        return cajita.allKeys(this.event___.properties___);
      }
      return [];
    };
    TameCustomHTMLEvent.prototype.toString = function () {
      return '[Fake CustomEvent]';
    };
    ___.grantTypedGeneric(TameCustomHTMLEvent.prototype, 'initEvent');
    ___.ctor(TameCustomHTMLEvent, TameEvent, 'TameCustomHTMLEvent');

    /**
     * Return a fake node list containing tamed nodes.
     * @param {Array.<TameNode>} array of tamed nodes.
     * @return an array that duck types to a node list.
     */
    function fakeNodeList(array) {
      array.item = ___.func(function(i) { return array[i]; });
      return cajita.freeze(array);
    }

    function TameHTMLDocument(doc, body, domain, editable) {
      TamePseudoNode.call(this, editable);
      this.doc___ = doc;
      this.body___ = body;
      this.domain___ = domain;
      var tameDoc = this;

      var tameBody = tameNode(body, editable);
      // TODO(mikesamuel): create a proper class for BODY, HEAD, and HTML along
      // with all the other specialized node types.
      var tameBodyElement = new TamePseudoElement(
          'BODY',
          this,
          function () { return tameNodeList(body.childNodes, editable); },
          function () { return tameHtmlElement; },
          function () { return tameInnerHtml(body.innerHTML); },
          editable);
      cajita.forOwnKeys(
          { appendChild: 0, removeChild: 0, insertBefore: 0, replaceChild: 0 },
          ___.frozenFunc(function (k) {
            tameBodyElement[k] = tameBody[k].bind(tameBody);
            ___.grantFunc(tameBodyElement, k);
          }));

      var title = doc.createTextNode(body.getAttribute('title') || '');
      var tameTitleElement = new TamePseudoElement(
          'TITLE',
          this,
          function () { return [tameNode(title, false)]; },
          function () { return tameHeadElement; },
          function () { return html.escapeAttrib(title.nodeValue); },
          editable);
      var tameHeadElement = new TamePseudoElement(
          'HEAD',
          this,
          function () { return [tameTitleElement]; },
          function () { return tameHtmlElement; },
          function () {
            return '<title>' + tameTitleElement.getInnerHTML() + '</title>';
          },
          editable);
      var tameHtmlElement = new TamePseudoElement(
          'HTML',
          this,
          function () { return [tameHeadElement, tameBodyElement]; },
          function () { return tameDoc; },
          function () {
            return ('<head>' + tameHeadElement.getInnerHTML + '<\/head><body>'
                    + tameBodyElement.getInnerHTML() + '<\/body>');
          },
          editable);
      this.documentElement___ = tameHtmlElement;
      classUtils.exportFields(this, ['documentElement', 'body', 'title', 'domain']);
    }
    classUtils.extend(TameHTMLDocument, TamePseudoNode);
    nodeClasses.HTMLDocument = TameHTMLDocument;
    TameHTMLDocument.prototype.getNodeType = function () { return 9; };
    TameHTMLDocument.prototype.getNodeName
        = function () { return '#document'; };
    TameHTMLDocument.prototype.getNodeValue = function () { return null; };
    TameHTMLDocument.prototype.getChildNodes
        = function () { return [this.documentElement___]; };
    TameHTMLDocument.prototype.getAttributes = function () { return []; };
    TameHTMLDocument.prototype.getParentNode = function () { return null; };
    TameHTMLDocument.prototype.getElementsByTagName = function (tagName) {
      tagName = String(tagName).toLowerCase();
      switch (tagName) {
        case 'body': return fakeNodeList([ this.getBody() ]);
        case 'head': return fakeNodeList([ this.getHead() ]);
        case 'title': return fakeNodeList([ this.getTitle() ]);
        case 'html': return fakeNodeList([ this.getDocumentElement() ]);
        default:
          return tameGetElementsByTagName(
              this.body___, tagName, this.editable___);
      }
    };
    TameHTMLDocument.prototype.getDocumentElement = function () {
      return this.documentElement___;
    };
    TameHTMLDocument.prototype.getBody = function () {
      return this.documentElement___.getLastChild();
    };
    TameHTMLDocument.prototype.getHead = function () {
      return this.documentElement___.getFirstChild();
    };
    TameHTMLDocument.prototype.getTitle = function () {
      return this.getHead().getFirstChild();
    };
    TameHTMLDocument.prototype.getDomain = function () {
      return this.domain___;
    };
    TameHTMLDocument.prototype.getElementsByClassName = function (className) {
      return tameGetElementsByClassName(
          this.body___, className, this.editable___);
    };
    TameHTMLDocument.prototype.addEventListener =
        function (name, listener, useCapture) {
          // TODO(ihab.awad): Implement
        };
    TameHTMLDocument.prototype.removeEventListener =
        function (name, listener, useCapture) {
          // TODO(ihab.awad): Implement
        };
    TameHTMLDocument.prototype.createElement = function (tagName) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      tagName = String(tagName).toLowerCase();
      if (!html4.ELEMENTS.hasOwnProperty(tagName)) {
        throw new Error(UNKNOWN_TAGNAME + "[" + tagName + "]");
      }
      var flags = html4.ELEMENTS[tagName];
      if (flags & html4.eflags.UNSAFE) {
        throw new Error(UNSAFE_TAGNAME + "[" + tagName + "]");
      }
      var newEl = this.doc___.createElement(tagName);
      if (elementPolicies.hasOwnProperty(tagName)) {
        var attribs = elementPolicies[tagName]([]);
        if (attribs) {
          for (var i = 0; i < attribs.length; i += 2) {
            bridal.setAttribute(newEl, attribs[i], attribs[i + 1]);
          }
        }
      }
      return tameNode(newEl, true);
    };
    TameHTMLDocument.prototype.createTextNode = function (text) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      return tameNode(this.doc___.createTextNode(
          text !== null && text !== void 0 ? '' + text : ''), true);
    };
    TameHTMLDocument.prototype.getElementById = function (id) {
      id += idSuffix;
      var node = this.doc___.getElementById(id);
      return tameNode(node, this.editable___);
    };
    TameHTMLDocument.prototype.toString = function () {
      return '[Fake Document]';
    };
    TameHTMLDocument.prototype.write = function (text) {
      // TODO(mikesamuel): Needs implementation
      cajita.log('Called document.write() with: ' + text);
    };
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
      var tamedEvent = new TameCustomHTMLEvent(rawEvent);
      rawEvent.tamed___ = tamedEvent;
      return tamedEvent;
    };
    ___.ctor(TameHTMLDocument, TamePseudoNode, 'TameHTMLDocument');
    ___.all2(___.grantTypedGeneric, TameHTMLDocument.prototype,
             ['addEventListener', 'removeEventListener',
              'createElement', 'createTextNode', 'createEvent',
              'getElementById', 'getElementsByClassName',
              'getElementsByTagName',
              'write']);


    imports.tameNode___ = tameNode;
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

    function TameStyle(style, editable) {
      this.style___ = style;
      this.editable___ = editable;
    }
    nodeClasses.Style = TameStyle;
    for (var styleProperty in css.properties) {
      if (!cajita.canEnumOwn(css.properties, styleProperty)) { continue; }
      (function (propertyName) {
         ___.useGetHandler(
             TameStyle.prototype, propertyName,
             function () {
               if (!this.style___) { return void 0; }
               return String(this.style___[propertyName] || '');
             });
         var pattern = css.properties[propertyName];
         ___.useSetHandler(
             TameStyle.prototype, propertyName,
             function (val) {
               if (!this.editable___) { throw new Error('style not editable'); }
               val = '' + (val || '');
               if (val && !pattern.test(val + ' ')) {
                 throw new Error('bad value `' + val + '` for CSS property '
                                 + propertyName);
               }
               // CssPropertyPatterns.java only allows styles of the form
               // url("...").  See the BUILTINS definition for the "uri" symbol.
               val = val.replace(/\burl\s*\(\s*\"([^\"]*)\"\s*\)/gi,
                                 function (_, url) {
                 // TODO(mikesamuel): recognize and rewrite URLs.
                 throw new Error('url in style ' + url);
               });
               this.style___[propertyName] = val;
             });
       })(styleProperty);
    }
    TameStyle.prototype.toString = function () { return '[Fake Style]'; };

    /**
     * Set of properties accessible on computed style.
     * This list is a conservative one compiled by looking at what prototype.js
     * needs to be able to do visibility, containment, and layout calculations.
     * If expanded, it should not allow an attacker to probe the user's history
     * as described at https://bugzilla.mozilla.org/show_bug.cgi?id=147777
     */
    var COMPUTED_STYLE_WHITELIST = {
      'display': true,
      'filter': true,
      'float': true,
      'height': true,
      'opacity': true,
      'overflow': true,
      'position': true,
      'visibility': true,
      'width': true
    };
    function TameComputedStyle(style) {
      this.style___ = style;
    }
    cajita.forOwnKeys(
        COMPUTED_STYLE_WHITELIST,
        ___.func(
            function (propertyName, _) {
              ___.useGetHandler(
                  TameComputedStyle.prototype, propertyName,
                  function () {
                    if (!this.style___) { return void 0; }
                    return String(this.style___[propertyName] || '');
                  });
            }));
    TameComputedStyle.prototype.toString = function () {
      return '[Fake Computed Style]';
    };

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
    var idClass = idSuffix.substring(1);
    /** A per-gadget class used to separate style rules. */
    imports.getIdClass___ = function () {
      return idClass;
    };

    // TODO(mikesamuel): remove these, and only expose them via window once
    // Valija works
    imports.setTimeout = tameSetTimeout;
    imports.setInterval = tameSetInterval;
    imports.clearTimeout = tameClearTimeout;
    imports.clearInterval = tameClearInterval;

    var tameDocument = new TameHTMLDocument(
        document,
        pseudoBodyNode,
        String(optPseudoWindowLocation.hostname) || 'nosuchhost,fake',
        true);
    imports.document = tameDocument;

    // TODO(mikesamuel): figure out a mechanism by which the container can
    // specify the gadget's apparent URL.
    // See http://www.whatwg.org/specs/web-apps/current-work/multipage/history.html#location0
    var tameLocation = ___.primFreeze({
      toString: ___.frozenFunc(function () { return tameLocation.href; }),
      href: String(optPseudoWindowLocation.href) || 'http://nosuchhost,fake/',
      hash: String(optPseudoWindowLocation.hash) || '',
      host: String(optPseudoWindowLocation.host) || 'nosuchhost,fake',
      hostname: String(optPseudoWindowLocation.hostname) || 'nosuchhost,fake',
      pathname: String(optPseudoWindowLocation.pathname) || '/',
      port: String(optPseudoWindowLocation.port) || '',
      protocol: String(optPseudoWindowLocation.protocol) || 'http:',
      search: String(optPseudoWindowLocation.search) || ''
      });

    // See spec at http://www.whatwg.org/specs/web-apps/current-work/multipage/browsers.html#navigator
    var tameNavigator = ___.primFreeze({
      appCodeName: 'Caja',
      appName: 'Sandbox',
      appVersion: '1.0',  // Should we expose the user's Locale here?
      language: '',  // Should we expose the user's Locale here?
      platform: 'Caja',
      oscpu: 'Caja',
      vendor: '',
      vendorSub: '',
      product: 'Caja',
      productSub: '',
      userAgent: 'Caja/1.0'
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
      // TODO: expose a read-only version of the document
      this.document = tameDocument;
      // Exposing an editable default view that pointed to a read-only
      // tameDocument via document.defaultView would allow escalation of
      // authority.
      assert(tameDocument.editable___);
      ___.grantRead(this, 'document');
    }

    cajita.forOwnKeys({
      document: imports.document,
      location: tameLocation,
      navigator: tameNavigator,
      setTimeout: tameSetTimeout,
      setInterval: tameSetInterval,
      clearTimeout: tameClearTimeout,
      clearInterval: tameClearInterval,
      addEventListener: ___.frozenFunc(
          function (name, listener, useCapture) {
            // TODO(ihab.awad): Implement
          }),
      removeEventListener: ___.frozenFunc(
          function (name, listener, useCapture) {
            // TODO(ihab.awad): Implement
          }),
      dispatchEvent: ___.frozenFunc(
          function (evt) {
            // TODO(ihab.awad): Implement
          })
    }, ___.func(function (propertyName, value) {
      TameWindow.prototype[propertyName] = value;
      ___.grantRead(TameWindow.prototype, propertyName);
    }));
    cajita.forOwnKeys({
      scrollBy: ___.frozenFunc(
          function (dx, dy) {
            // The window is always auto scrollable, so make the apparent window
            // body scrollable if the gadget tries to scroll it.
            if (dx || dy) { makeScrollable(tameDocument.body___); }
            tameScrollBy(tameDocument.body___, dx, dy);
          }),
      scrollTo: ___.frozenFunc(
          function (x, y) {
            // The window is always auto scrollable, so make the apparent window
            // body scrollable if the gadget tries to scroll it.
            makeScrollable(tameDocument.body___);
            tameScrollTo(tameDocument.body___, x, y);
          }),
      resizeTo: ___.frozenFunc(
          function (w, h) {
            tameResizeTo(tameDocument.body___, w, h);
          }),
      resizeBy: ___.frozenFunc(
          function (dw, dh) {
            tameResizeBy(tameDocument.body___, dw, dh);
          }),
      /** A partial implementation of getComputedStyle. */
      getComputedStyle: ___.frozenFunc(
          // Pseudo elements are suffixes like :first-line which constrain to
          // a portion of the element's content as defined at
          // http://www.w3.org/TR/CSS2/selector.html#q20
          function (tameElement, pseudoElement) {
            cajita.guard(tameNodeTrademark, tameElement);
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
              throw new Error('Bad pseudo class ' + pseudoElement);
            }
            // No need to check editable since computed styles are readonly.

            var rawNode = tameElement.node___;
            if (rawNode.currentStyle && pseudoElement === void 0) {
              return new TameComputedStyle(rawNode.currentStyle);
            } else if (window.getComputedStyle) {
              var rawStyleNode = window.getComputedStyle(
                  tameElement.node___, pseudoElement);
              return new TameComputedStyle(rawStyleNode);
            } else {
              throw new Error(
                  'Computed style not available for pseudo element '
                  + pseudoElement);
            }
          })

      // NOT PROVIDED
      // event: a global on IE.  We always define it in scopes that can handle
      //        events.
      // opera: defined only on Opera.
    }, ___.func(function (propertyName, value) {
      TameWindow.prototype[propertyName] = value;
      ___.grantRead(TameWindow.prototype, propertyName);
      TameDefaultView.prototype[propertyName] = value;
      ___.grantRead(TameDefaultView.prototype, propertyName);
    }));
    TameWindow.prototype.handleRead___ = function (name) {
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
      handlerName = handlerName.toLowerCase();
      if (this[handlerName]) {
        return this[handlerName](val);
      }
      this[name + '_canEnum___'] = true;
      this[name + '_canRead___'] = true;
      return this[name] = val;
    };
    TameWindow.prototype.handleDelete___ = function (name) {
      name = String(name);
      if (endsWith__.test(name)) { throw new Error(INVALID_SUFFIX); }
      var handlerName = name + '_deleter___';
      if (this[handlerName]) {
        return this[handlerName]();
      }
      handlerName = handlerName.toLowerCase();
      if (this[handlerName]) {
        return this[handlerName]();
      }
      return (
          delete this[name]
          && delete this[name + '_canEnum___']
          && delete this[name + '_canRead___']);
    };
    TameWindow.prototype.handleEnum___ = function (ownFlag) {
      // TODO(metaweta): Add code to list all the other handled stuff we know
      // about.
      return cajita.allKeys(this);
    };

    var tameWindow = new TameWindow();
    var tameDefaultView = new TameDefaultView(tameDocument.editable___);

    function propertyOnlyHasGetter(_) {
      throw new TypeError('setting a property that only has a getter');
    }
    cajita.forOwnKeys({
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
    }, ___.func(function (propertyName, def) {
      // TODO(mikesamuel): define on prototype.
      ___.useGetHandler(tameWindow, propertyName, def.get);
      ___.useSetHandler(tameWindow, propertyName,
                        def.set || propertyOnlyHasGetter);
      ___.useGetHandler(tameDefaultView, propertyName, def.get);
      ___.useSetHandler(tameDefaultView, propertyName,
                        def.set || propertyOnlyHasGetter);
    }));


    // Attach reflexive properties to 'window' object
    var windowProps = ['top', 'self', 'opener', 'parent', 'window'];
    var wpLen = windowProps.length;
    for (var i = 0; i < wpLen; ++i) {
      var prop = windowProps[i];
      tameWindow[prop] = tameWindow;
      ___.grantRead(tameWindow, prop);
    }

    if (tameDocument.editable___) {
      tameDocument.defaultView = tameDefaultView;
      ___.grantRead(tameDocument, 'defaultView');
    }

    // Iterate over all node classes, assigning them to the Window object
    // under their DOM Level 2 standard name.
    cajita.forOwnKeys(nodeClasses, ___.func(function(name, ctor) {
      ___.primFreeze(ctor);
      tameWindow[name] = ctor;
      ___.grantRead(tameWindow, name);
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

    var defaultNodeClassCtor = ___.primFreeze(TameElement);
    for (var i = 0; i < defaultNodeClasses.length; i++) {
      tameWindow[defaultNodeClasses[i]] = defaultNodeClassCtor;
      ___.grantRead(tameWindow, defaultNodeClasses[i]);
    }

    var outers = imports.outers;
    if (___.isJSONContainer(outers)) {
      // For Valija, attach use the window object as outers.
      cajita.forOwnKeys(outers, ___.func(function(k, v) {
        if (!(k in tameWindow)) {
          tameWindow[k] = v;
          ___.grantRead(tameWindow, k);
        }
      }));
      imports.outers = tameWindow;
    } else {
      imports.window = tameWindow;
    }
  }

  return attachDocumentStub;
})();

/**
 * Function called from rewritten event handlers to dispatch an event safely.
 */
function plugin_dispatchEvent___(thisNode, event, pluginId, handler) {
  event = (event || window.event);
  if (typeof console !== 'undefined' && console.log) {
    var sig = String(handler).match(/^function\b[^\)]*\)/);
    console.log(
        'Dispatch %s event thisNode=%o, event=%o, pluginId=%o, handler=%o',
        (event && event.type), thisNode, event, pluginId,
        sig ? sig[0] : handler);
  }
  var imports = ___.getImports(pluginId);
  switch (typeof handler) {
    case 'string':
      handler = imports[handler];
      break;
    case 'function': case 'object':
      break;
    default:
      throw new Error(
          'Expected function as event handler, not ' + typeof handler);
  }
  if (___.startCallerStack) { ___.startCallerStack(); }
  imports.isProcessingEvent___ = true;
  try {
    return ___.callPub(
        handler, 'call',
        [imports.tameNode___(thisNode, true),
         imports.tameEvent___(event)]);
  } catch (ex) {
    if (ex && ex.cajitaStack___ && 'undefined' !== (typeof console)) {
      console.error('Event dispatch %s: %s',
          handler, ___.unsealCallerStack(ex.cajitaStack___).join('\n'));
    }
    throw ex;
  } finally {
    imports.isProcessingEvent___ = false;
  }
}
