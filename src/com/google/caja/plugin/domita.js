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
 * Requires cajita.js, css-defs.js, html4-defs.js, html-emitter.js,
 * html-sanitizer.js, unicode.js.
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
 * @author mikesamuel@gmail.com
 * @requires console, document, window
 * @requires clearInterval, clearTimeout, setInterval, setTimeout
 * @requires ___, bridal, cajita, css, html, html4, unicode
 * @provides attachDocumentStub, plugin_dispatchEvent___
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
    var activeXClassId;
    // The first time the ctor is called, 
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
            new ActiveXObject(candidate);
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
    if (safeUri === undefined) { throw 'URI violates security policy'; }
    switch (arguments.length) {
    case 2:
      this.xhr___.open(method, safeUri);
      break;
    case 3:
      this.xhr___.open(method, safeUri, Boolean(opt_async));
      break;
    case 4:
      this.xhr___.open(
          method, safeUri, Boolean(opt_async), String(opt_userName));
      break;
    case 5:
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
    } else if (opt_data instanceof String) {
      this.xhr___.send(opt_data);
      return;
    } else /* if XML document */ {
      // TODO(ihab.awad): Expect tamed XML document; unwrap and send
      throw 'Sending XML data not yet supported';
    }
  };
  TameXMLHttpRequest.prototype.abort = function () {
    this.xhr___.abort();
  };
  TameXMLHttpRequest.prototype.getAllResponseHeaders = function () {
    return String(this.xhr___.getAllResponseHeaders());
  };
  TameXMLHttpRequest.prototype.getResponseHeader = function (headerName) {
    return String(this.xhr___.getResponseHeader(String(headerName)));
  };
  TameXMLHttpRequest.prototype.getResponseText = function () {
    return String(this.xhr___.responseText);
  };
  TameXMLHttpRequest.prototype.getResponseXML = function () {
    // TODO(ihab.awad): Implement a taming layer for XML. Requires generalizing
    // the HTML node hierarchy as well so we have a unified implementation.
    return {};
  };
  TameXMLHttpRequest.prototype.getStatus = function () {
    return Number(this.xhr___.status);
  };
  TameXMLHttpRequest.prototype.getStatusText = function () {
    return String(this.xhr___.statusText);
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
        console.log('domita assertion failed');
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

  // See above for a description of this function.
  function attachDocumentStub(idSuffix, uriCallback, imports, pseudoBodyNode) {
    if (arguments.length !== 4) {
      throw new Error('arity mismatch: ' + arguments.length);
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
      for (var i = nodeList.length; --i >= 0;) {
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
      node = nodeList = null;

      tamed.item = ___.frozenFunc(function (k) {
        k &= 0x7fffffff;
        if (isNaN(k)) { throw new Error(); }
        return tamed[k] || null;
      });
      // TODO(mikesamuel): if opt_keyAttrib, could implement getNamedItem
      return cajita.freeze(tamed);
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
      wrapper.originalListener___ = listener;
      return wrapper;
    }

    var NOT_EDITABLE = "Node not editable.";
    var INVALID_SUFFIX = "Property names may not end in '__'.";

    // Implementation of EventTarget::addEventListener
    function tameAddEventListener(name, listener, useCapture) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      if (!this.wrappedListeners___) { this.wrappedListeners___ = []; }
      name = String(name);
      useCapture = Boolean(useCapture);
      var wrappedListener = makeEventHandlerWrapper(this.node___, listener);
      this.wrappedListeners___.push(wrappedListener);
      bridal.addEventListener(this.node___, name, wrappedListener, useCapture);
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
      name = String(name);
      bridal.removeEventListener(
           this.node___, name, wrappedListener, useCapture);
    }

    // Implementation of EventTarget::dispatchEvent
    function tameDispatchEvent(evt) {
      cajita.guard(tameEventTrademark, evt);
      // TODO(ihab.awad): Complete and test implementation
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
    var tameNodeMembers = [
        'getNodeType', 'getNodeValue', 'getNodeName',
        'appendChild', 'insertBefore', 'removeChild', 'replaceChild',
        'getFirstChild', 'getLastChild', 'getNextSibling', 'getPreviousSibling',
        'getElementsByTagName',
        'getOwnerDocument',
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
      return tameNodeList(
          this.node___.getElementsByTagName(String(tagName)), this.editable___);
    };
    if (typeof document.getElementsByClassName !== 'undefined') {
      TameBackedNode.prototype.getElementsByClassName = function (className) {
        return tameNodeList(
            this.node___.getElementsByClassName(String(className)),
            this.editable___);
      };
    }
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
    // Abstract TamePseudoNode.prototype.getNodeType
    // Abstract TamePseudoNode.prototype.getNodeName
    // Abstract TamePseudoNode.prototype.getNodeValue
    // Abstract TamePseudoNode.prototype.getAttributes
    // Abstract TamePseudoNode.prototype.getChildNodes
    // Abstract TamePseudoNode.prototype.getParentNode
    // Abstract TamePseudoNode.prototype.getElementsByTagName
    // Abstract TamePseudoNode.prototype.getElementsByClassName
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
      var handlerName = name + '_getter___';
      if (this[handlerName]) {
        return this[handlerName]();
      }
      if (this.properties___.hasOwnProperty(name)) {
        return this.properties___[name];
      } else {
        return void 0;
      }
    };
    TamePseudoNode.prototype.handleCall___ = function (name, args) {
      var handlerName = name + '_handler___';
      if (this[handlerName]) {
        return this[handlerName].call(this, args);
      }
      if (this.properties___.hasOwnProperty(name)) {
        return this.properties___[name].call(this, args);
      } else {
        throw new TypeError(name + ' is not a function.');
      }
    };
    TamePseudoNode.prototype.handleSet___ = function (name, val) {
      var handlerName = name + '_setter___';
      if (this[handlerName]) {
        return this[handlerName](val);
      }
      this[name + '_canEnum___'] = true;
      return this.properties___[name] = val;
    };
    TamePseudoNode.prototype.handleDelete___ = function (name) {
      var handlerName = name + '_deleter___';
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
    /**
     * @param {boolean} ownFlag ignored
     */
    TamePseudoNode.prototype.handleEnum___ = function (ownFlag) {
      // TODO(metaweta): Add code to list all the other handled stuff we know
      // about.
      if (this.node___.properties___) {
        return cajita.allKeys(this.node___.properties___);
      }
      return [];
    };
    TamePseudoNode.prototype.hasChildNodes = function () {
      return !!this.node___.hasChildNodes();
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
      return tameNodeList(
          this.body___.getElementsByTagName(tagName), this.editable___);
    };
    if (typeof document.getElementsByClassName !== 'undefined') {
      TamePseudoElement.prototype.getElementsByClassName
          = function (className) {
        return this.getOwnerDocument().getElementsByClassName(className);
      };
    }
    TamePseudoElement.prototype.toString = function () {
      return '<' + this.tagName___ + '>';
    };
    ___.ctor(TamePseudoElement, TamePseudoNode, 'TamePseudoElement');
    ___.all2(___.grantTypedGeneric, TameElement.prototype, ['getTagName']);


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

    function TameTextNode(node, editable) {
      assert(node.nodeType === 3);
      TameBackedNode.call(this, node, editable);
      classUtils.exportFields(this, ['nodeValue', 'data']);
    }
    classUtils.extend(TameTextNode, TameBackedNode);
    nodeClasses.TextNode = TameTextNode;
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

    function TameElement(node, editable) {
      assert(node.nodeType === 1);
      TameBackedNode.call(this, node, editable);
      classUtils.exportFields(
          this,
          ['className', 'id', 'innerHTML', 'tagName', 'style',
            'offsetLeft', 'offsetTop', 'offsetWidth', 'offsetHeight',
            'offsetParent',
            'scrollLeft',
            'scrollTop',
            'scrollWidth',
            'scrollHeight',
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
      var attribKey;
      var atype;
      if ((attribKey = tagName + ':' + attribName,
          html4.ATTRIBS.hasOwnProperty(attribKey))
          || (attribKey = '*:' + attribName,
              html4.ATTRIBS.hasOwnProperty(attribKey))) {
        atype = html4.ATTRIBS[attribKey];
      } else {
        return String(
            (this.node___.attributes___ && 
            this.node___.attributes___[attribName]) || '');
      }
      var value = this.node___.getAttribute(attribName);
      if ('string' !== typeof value) { return value; }
      switch (atype) {
        case html4.atype.ID:
        case html4.atype.IDREF:
        case html4.atype.IDREFS:
          if (!value) { return ''; }
          var n = idSuffix.length;
          var len = value.length;
          var end = len - n;
          if (end > 0 && idSuffix === value.substring(end, len)) {
            return value.substring(0, end);
          }
          return '';
        default:
          return value;
      }
    };
    TameElement.prototype.hasAttribute = function (name) {
      name = String(name).toLowerCase();
      var type = html4.ATTRIBS[name];
      if (type === undefined || !html4.ATTRIBS.hasOwnProperty(name)) {
        return !!(
            this.node___.attributes___ && 
            ___.hasOwnProp(this.node___.attributes___, name));
      }
      return this.node___.hasAttribute(name);
    };
    TameElement.prototype.setAttribute = function (attribName, value) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      attribName = String(attribName).toLowerCase();
      var tagName = this.node___.tagName.toLowerCase();
      var attribKey;
      var atype;
      if ((attribKey = tagName + ':' + attribName,
           html4.ATTRIBS.hasOwnProperty(attribKey))
          || (attribKey = '*:' + attribName,
              html4.ATTRIBS.hasOwnProperty(attribKey))) {
        atype = html4.ATTRIBS[attribKey];
      } else {
        if (!this.node___.attributes___) { this.node___.attributes___ = {}; }
        this.node___.attributes___[attribName] = String(value);
        return value; 
      }
      var sanitizedValue = rewriteAttribute(tagName, attribName, atype, value);
      if (sanitizedValue !== null) {
        bridal.setAttribute(this.node___, attribName, sanitizedValue);
      }
      return value;
    };
    TameElement.prototype.removeAttribute = function (name) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      name = String(name).toLowerCase();
      var type = html4.ATTRIBS[name];
      if (type === void 0 || !html4.ATTRIBS.hasOwnProperty(name)) {
        // Can't remove an attribute you can't read
        if (this.node___.attributes___) {
          delete this.node___.attributes___[name];
        }
      }
      this.node___.removeAttribute(name);
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
    TameElement.prototype.dispatchEvent = tameDispatchEvent;
    ___.ctor(TameElement, TameBackedNode, 'TameElement');
    ___.all2(
       ___.grantTypedGeneric, TameElement.prototype,
       ['addEventListener', 'removeEventListener', 'dispatchEvent',
        'getAttribute', 'setAttribute',
        'removeAttribute',
        'hasAttribute',
        'getClassName', 'setClassName', 'getId', 'setId',
        'getInnerHTML', 'setInnerHTML', 'updateStyle', 'getStyle', 'setStyle',
        'getTagName', 'getOffsetLeft', 'getOffsetTop', 'getOffsetWidth',
        'getOffsetHeight']);

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
      return this.getAttribute('action');
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
          ['form', 'value',
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
      var value = this.node___.value;
      return value === null || value === void 0 ? null : String(value);
    };
    TameInputElement.prototype.setValue = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.value = (
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
    TameInputElement.prototype.getChecked = function () {
      return this.node___.checked;
    };
    TameInputElement.prototype.setChecked = function (newValue) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      this.node___.checked = newValue;
      return newValue;
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
      return !!this.node___.defaultSelected;
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
      this.node___.selectedIndex = newValue;
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


    function TameEvent(event) {
      this.event___ = event;
      ___.stamp(tameEventTrademark, this, true);
      classUtils.exportFields(
          this,
          ['type', 'target', 'pageX', 'pageY', 'altKey',
            'ctrlKey', 'metaKey', 'shiftKey', 'button',
            'screenX', 'screenY',
            'relatedTarget',
            'fromElement', 'toElement',
            'srcElement',
            'clientX', 'clientY', 'keyCode', 'which']);
    }
    nodeClasses.Event = TameEvent;
    TameEvent.prototype.getType = function () {
      return String(this.event___.type);
    };
    TameEvent.prototype.getTarget = function () {
      var event = this.event___;
      return tameRelatedNode(event.target || event.srcElement, true);
    };
    TameEvent.prototype.getSrcElement = function () {
      return tameRelatedNode(this.event___.srcElement, true);
    };
    TameEvent.prototype.getRelatedTarget = function () {
      var e = this.event___;
      var t = e.relatedTarget;
      if (! t) {
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
    TameEvent.prototype.toString = function () { return 'Not a real event'; };
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

    // This duck types to a node list; we can't expose real nodelists.
    function FakeNodeList(array) {
      array.item = ___.func(function(i) { return array[i]; });
      return cajita.freeze(array);
    }

    function TameHTMLDocument(doc, body, editable) {
      TamePseudoNode.call(this, editable);
      this.doc___ = doc;
      this.body___ = body;
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
      classUtils.exportFields(this, ['documentElement', 'body']);
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
      if (tagName === "body") { return FakeNodeList( [ this.getBody() ] ); }
      else if (tagName === "head") { return FakeNodeList( [ this.getHead() ] ); }
      return tameNodeList(
          this.body___.getElementsByTagName(tagName), this.editable___);
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
    if (typeof document.getElementsByClassName !== 'undefined') {
      TameHTMLDocument.prototype.getElementsByClassName = function (className) {
        return tameNodeList(
            this.body___.getElementsByClassName(className), this.editable___);
      };
    }
    TameHTMLDocument.prototype.addEventListener =
        function (name, listener, useCapture) {
          // TODO(ihab.awad): Implement
        };
    TameHTMLDocument.prototype.removeEventListener =
        function (name, listener, useCapture) {
          // TODO(ihab.awad): Implement
        };
    TameHTMLDocument.prototype.dispatchEvent =
        function (evt) {
          // TODO(ihab.awad): Implement
        };
    TameHTMLDocument.prototype.createElement = function (tagName) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      tagName = String(tagName).toLowerCase();
      if (!html4.ELEMENTS.hasOwnProperty(tagName)) { throw new Error(); }
      var flags = html4.ELEMENTS[tagName];
      if (flags & html4.eflags.UNSAFE) { throw new Error(); }
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
    ___.ctor(TameHTMLDocument, TamePseudoNode, 'TameHTMLDocument');
    ___.all2(___.grantTypedGeneric, TameHTMLDocument.prototype,
             ['addEventListener', 'removeEventListener', 'dispatchEvent',
              'createElement', 'createTextNode', 'getElementById',
              'getElementsByTagName', 'getElementsByClassName', 'write']);


    imports.tameNode___ = tameNode;
    imports.tameEvent___ = function (event) { return new TameEvent(event); };
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

    var tameDocument = new TameHTMLDocument(document, pseudoBodyNode, true);
    imports.document = tameDocument;

    // TODO(mikesamuel): figure out a mechanism by which the container can
    // specify the gadget's apparent URL.
    // See http://www.whatwg.org/specs/web-apps/current-work/multipage/history.html#location0
    var tameLocation = ___.primFreeze({
      toString: ___.frozenFunc(function () { return tameLocation.href; }),
      href: 'http://nosuchhost,fake/',
      hash: '',
      host: 'nosuchhost,fake',
      hostname: 'nosuchhost,fake',
      pathname: '/',
      port: '',
      protocol: 'http:',
      search: ''
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

    // See http://www.whatwg.org/specs/web-apps/current-work/multipage/browsers.html#window for the full API.
    // TODO(mikesamuel): This implements only the parts needed by prototype.
    // The rest can be added on an as-needed basis as long as DOMado rules are
    // obeyed.
    var tameWindow = {
      document: imports.document,
      location: tameLocation,
      navigator: tameNavigator,
      setTimeout: tameSetTimeout,
      setInterval: tameSetInterval,
      clearTimeout: tameClearTimeout,
      clearInterval: tameClearInterval,
      scrollTo: ___.frozenFunc(
          function (x, y) {
            // Per DOMado rules, the window can only be scrolled in response to
            // a user action.  Hence the isProcessingEvent___ check.
            if ('number' === typeof x
                && 'number' === typeof y
                && !isNaN(x - y)
                && imports.isProcessingEvent___) {
              window.scrollTo(x, y);
            }
          }),
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

      // NOT PROVIDED
      // event: a global on IE.  We always define it in scopes that can handle
      //        events.
      // opera: defined only on Opera.
      // pageXOffset, pageYOffset: used if document.body.scroll{Left,Top}
      //        unavailable
    };

    // Attach reflexive properties to 'window' object
    tameWindow.top = tameWindow.self = tameWindow.opener = tameWindow.parent
        = tameWindow.window = tameWindow;

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
      // For Valija, attach window object members to outers instead so that
      // the members of window show up as global variables as well.
      for (var k in tameWindow) {
        if (!___.hasOwnProp(outers, k) && ___.canEnumPub(tameWindow, k)) {
          var v = tameWindow[k];
          outers[k] = v === tameWindow ? outers : v;
        }
      }
      outers.window = outers;
    } else {
      cajita.freeze(tameWindow);
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
    console.log(
        'Dispatch %s event thisNode=%o, event=%o, pluginId=%o, handler=%o',
        (event && event.type), thisNode, event, pluginId, handler);
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
