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
 * - <code>===</code> and <code>!==</code> on tamed DOM nodes will not behave
 *   the same as with untamed nodes.  Specifically, it is not always true that
 *   {@code document.getElementById('foo') === document.getElementById('foo')}.
 * - Properties backed by setters/getters like {@code HTMLElement.innerHTML}
 *   will not appear to uncajoled code as DOM nodes do, since they are
 *   implemented using cajita property handlers.
 *
 * @author mikesamuel@gmail.com
 */


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
      var propertyAndValue = declarations[i].split(':');
      var property = trimCssSpaces(propertyAndValue[0]).toLowerCase();
      var value = trimCssSpaces(propertyAndValue[1]);
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

  function assert(cond) {
    if (!cond) {
      (typeof console !== 'undefined')
      && (console.log('domita assertion failed'), console.trace());
      throw new Error();
    }
  }

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
  function attachDocumentStub(idSuffix, uriCallback, imports) {
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

      // TODO(benl): replace this with a proper cache
      if (node.tamed___)
 	return node.tamed___;

      var tamed;
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

      node.tamed___ = tamed;
      return tamed;
    }

    function tameRelatedNode(node, editable) {
      if (node === null || node === void 0) { return null; }
      // catch errors because node might be from a different domain
      try {
        for (var ancestor = node; ancestor; ancestor = ancestor.parentNode) {
          // TODO(mikesamuel): replace with cursors so that subtrees are delegable
          if (idClass === ancestor.className) {  // TODO: handle multiple classes.
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
      for (var i = nodeList.length; --i >= 0;) {
        var node = tameNode(nodeList.item(i), editable);
        tamed[i] = node;
        // Make the node available via its name if doing so would not mask
        // any properties of tamed.
        var key = opt_keyAttrib && node.getAttribute(opt_keyAttrib);
        if (key && !(/_$/.test(key) || (key in tamed)
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

    // Implementation of EventTarget::addEventListener
    function tameAddEventListener(name, listener, useCapture) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      if (!this.wrappedListeners___) { this.wrappedListeners___ = []; }
      name = String(name);
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
          this.wrappedListeners___ =
              arrayRemove(this.wrappedListeners___, i, i);
          break;
        }
      }
      if (!wrappedListener) { return; }
      name = String(name);
      bridal.removeEventListener(this.node___, name, wrappedListener, useCapture);
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
        'ownerDocument',
        'childNodes'];

    /**
     * Base class for a Node wrapper.  Do not create directly -- use the
     * tameNode factory instead.
     * @constructor
     */
    function TameNode(node, editable) {
      if (!node) {
        throw new Error('Creating tame node with undefined native delegate');
      }
      this.node___ = node;
      this.editable___ = editable;
      ___.stamp(tameNodeTrademark, this, true);
      exportFields(this, tameNodeFields);
    }
    nodeClasses.Node = TameNode;
    TameNode.prototype.getNodeType = function () {
      return this.node___.nodeType;
    };
    TameNode.prototype.getNodeName = function () {
      return this.node___.nodeName;
    };
    TameNode.prototype.getNodeValue = function () {
      return this.node___.nodeValue;
    };
    TameNode.prototype.appendChild = function (child) {
      // Child must be editable since appendChild can remove it from its parent.
      cajita.guard(tameNodeTrademark, child);
      if (!this.editable___ || !child.editable___) {
        throw new Error(NOT_EDITABLE);
      }
      this.node___.appendChild(child.node___);
    };
    TameNode.prototype.insertBefore = function (toInsert, child) {
      cajita.guard(tameNodeTrademark, toInsert);
      if (child === void 0) { child = null; }
      if (child !== null) { cajita.guard(tameNodeTrademark, child); }
      if (!this.editable___ || !toInsert.editable___) {
        throw new Error(NOT_EDITABLE);
      }
      this.node___.insertBefore(
          toInsert.node___, child !== null ? child.node___ : null);
    };
    TameNode.prototype.removeChild = function (child) {
      cajita.guard(tameNodeTrademark, child);
      if (!this.editable___ || !child.editable___) {
        throw new Error(NOT_EDITABLE);
      }
      this.node___.removeChild(child.node___);
    };
    TameNode.prototype.replaceChild = function (child, replacement) {
      cajita.guard(tameNodeTrademark, child);
      cajita.guard(tameNodeTrademark, replacement);
      if (!this.editable___ || !replacement.editable___) {
        throw new Error(NOT_EDITABLE);
      }
      this.node___.replaceChild(child.node___, replacement.node___);
    };
    TameNode.prototype.getFirstChild = function () {
      return tameNode(this.node___.firstChild, this.editable___);
    };
    TameNode.prototype.getLastChild = function () {
      return tameNode(this.node___.lastChild, this.editable___);
    };
    TameNode.prototype.getNextSibling = function () {
      // TODO(mikesamuel): replace with cursors so that subtrees are delegable
      return tameNode(this.node___.nextSibling, this.editable___);
    };
    TameNode.prototype.getPreviousSibling = function () {
      // TODO(mikesamuel): replace with cursors so that subtrees are delegable
      return tameNode(this.node___.previousSibling, this.editable___);
    };
    TameNode.prototype.getParentNode = function () {
      return tameRelatedNode(this.node___.parentNode, this.editable___);
    };
    TameNode.prototype.getElementsByTagName = function (tagName) {
      return tameNodeList(
          this.node___.getElementsByTagName(String(tagName)), this.editable___);
    };
    TameNode.prototype.getOwnerDocument = function() {
      return imports.document;
    };
    TameNode.prototype.getElementsByClassName = function(className) {
      return tameNodeList(
	this.node___.getElementsByClassName(String(className),
					    this.editable___));
    };
    TameNode.prototype.getChildNodes = function() {
      return tameNodeList(this.node___.childNodes, this.editable___);
    };
    TameNode.prototype.hasChildNodes = function() {
      return !!this.node___.hasChildNodes();
    };
    ___.ctor(TameNode, void 0, 'TameNode');
    var tameNodeMembers = [
        'getNodeType', 'getNodeValue', 'getNodeName',
        'appendChild', 'insertBefore', 'removeChild', 'replaceChild',
        'getFirstChild', 'getLastChild', 'getNextSibling', 'getPreviousSibling',
        'getElementsByTagName',
        'getOwnerDocument',
        'hasChildNodes'
    ];
    ___.all2(___.grantTypedGeneric, TameNode.prototype, tameNodeMembers);

    function TameOpaqueNode(node, editable) {
      TameNode.call(this, node, editable);
    }
    extend(TameOpaqueNode, TameNode);
    TameOpaqueNode.prototype.getNodeValue = TameNode.prototype.getNodeValue;
    TameOpaqueNode.prototype.getNodeType = TameNode.prototype.getNodeType;
    TameOpaqueNode.prototype.getNodeName = TameNode.prototype.getNodeName;
    TameOpaqueNode.prototype.getNextSibling = TameNode.prototype.getNextSibling;
    TameOpaqueNode.prototype.getPreviousSibling
        = TameNode.prototype.getPreviousSibling;
    TameOpaqueNode.prototype.getFirstChild = TameNode.prototype.getFirstChild;
    TameOpaqueNode.prototype.getLastChild = TameNode.prototype.getLastChild;
    TameOpaqueNode.prototype.getParentNode = TameNode.prototype.getParentNode;
    TameOpaqueNode.prototype.getChildNodes = TameNode.prototype.getChildNodes;
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
      TameNode.call(this, node, editable);
      exportFields(this, ['nodeValue', 'data']);
    }
    extend(TameTextNode, TameNode);
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
    ___.ctor(TameTextNode, void 0, 'TameNode');
    ___.all2(___.grantTypedGeneric, TameTextNode.prototype,
             ['setNodeValue', 'getData', 'setData']);

    function TameCommentNode(node, editable) {
      assert(node.nodeType === 8);
      TameNode.call(this, node, editable);
    }
    extend(TameCommentNode, TameNode);
    nodeClasses.CommentNode = TameCommentNode;
    TameCommentNode.prototype.toString = function () {
      return '#comment';
    };
    ___.ctor(TameCommentNode, void 0, 'TameNode');

    function TameElement(node, editable) {
      assert(node.nodeType === 1);
      TameNode.call(this, node, editable);
      exportFields(this,
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
    extend(TameElement, TameNode);
    nodeClasses.Element = TameElement;
    nodeClasses.HTMLElement = TameElement;
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
        return '';
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
        return false;
      }
      return this.node___.hasAttribute( name );
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
        throw new Error();
      }
      var sanitizedValue = rewriteAttribute(tagName, attribName, atype, value);
      if (sanitizedValue !== null) {
        bridal.setAttribute(this.node___, attribName, sanitizedValue);
      }
      return value;
    };
    TameElement.prototype.removeAttribute = function (name) {
      if (!this.editable___) { throw new Error(); }
      name = String(name).toLowerCase();
      var type = html4.ATTRIBS[name];
      if (type === void 0 || !html4.ATTRIBS.hasOwnProperty(name)) {
        // Can't remove an attribute you can't read
        return;
      }
      this.node___.removeAttribute( name );
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
      if (!this.editable___) { throw new Error(); }
      return this.setAttribute('title', String(classes));
    };
    TameElement.prototype.getDir = function () {
      return this.getAttribute('dir') || '';
    };
    TameElement.prototype.setDir = function (classes) {
      if (!this.editable___) { throw new Error(); }
      return this.setAttribute('dir', String(classes));
    };
    TameElement.prototype.getTagName = TameNode.prototype.getNodeName;
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
    ___.ctor(TameElement, TameNode, 'TameElement');
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
      exportFields(this, ['href']);
    }
    extend(TameAElement, TameElement);
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
      exportFields(this, ['action', 'elements', 'enctype', 'method', 'target']);
    }
    extend(TameFormElement, TameElement);
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
      if (!this.editable___) { throw new Error(); }
      return this.setAttribute('action', String(newVal));
    };
    TameFormElement.prototype.getElements = function () {
      return tameNodeList(this.node___.elements, this.editable___, 'name');
    };
    TameFormElement.prototype.getEnctype = function () {
      return this.getAttribute('enctype') || '';
    };
    TameFormElement.prototype.setEnctype = function (newVal) {
      if (!this.editable___) { throw new Error(); }
      return this.setAttribute('enctype', String(newVal));
    };
    TameFormElement.prototype.getMethod = function () {
      return this.getAttribute('method') || '';
    };
    TameFormElement.prototype.setMethod = function (newVal) {
      if (!this.editable___) { throw new Error(); }
      return this.setAttribute('method', String(newVal));
    };
    TameFormElement.prototype.getTarget = function () {
      return this.getAttribute('target') || '';
    };
    TameFormElement.prototype.setTarget = function (newVal) {
      if (!this.editable___) { throw new Error(); }
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
      exportFields(this,
                   ['form', 'value',
                    'checked', 'disabled', 'readOnly',
                    'options', 'selected', 'selectedIndex',
                    'name', 'accessKey', 'tabIndex', 'text',
                    'defaultChecked', 'defaultSelected', 'maxLength',
                    'size', 'type', 'index', 'label',
                    'multiple', 'cols', 'rows']);
    }
    extend(TameInputElement, TameElement);
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
      if (!this.editable___) { throw new Error(); }
      this.node___.checked = newValue;
      return newValue;
    };
    TameInputElement.prototype.getDisabled = function () {
      return this.node___.disabled;
    };
    TameInputElement.prototype.setDisabled = function (newValue) {
      if (!this.editable___) { throw new Error(); }
      this.node___.disabled = newValue;
      return newValue;
    };
    TameInputElement.prototype.getReadOnly = function () {
      return this.node___.readOnly;
    };
    TameInputElement.prototype.setReadOnly = function (newValue) {
      if (!this.editable___) { throw new Error(); }
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
      if (!this.editable___) { throw new Error(); }
      this.node___.selected = newValue;
      return newValue;
    };
    TameInputElement.prototype.getSelectedIndex = function () {
      return this.node___.selectedIndex;
    };
    TameInputElement.prototype.setSelectedIndex = function (newValue) {
      if (!this.editable___) { throw new Error(); }
      this.node___.selectedIndex = newValue;
      return newValue;
    };
    TameInputElement.prototype.getName = function () {
      return this.node___.name;
    };
    TameInputElement.prototype.setName = function (newValue) {
      if (!this.editable___) { throw new Error(); }
      this.node___.name = newValue;
      return newValue;
    };
    TameInputElement.prototype.getAccessKey = function () {
      return this.node___.accessKey;
    };
    TameInputElement.prototype.setAccessKey = function (newValue) {
      if (!this.editable___) { throw new Error(); }
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
      if (!this.editable___) { throw new Error(); }
      this.node___.tabIndex = newValue;
      return newValue;
    };
    TameInputElement.prototype.getDefaultChecked = function () {
      return this.node___.defaultChecked;
    };
    TameInputElement.prototype.setDefaultChecked = function (newValue) {
      if (!this.editable___) { throw new Error(); }
      this.node___.defaultChecked = newValue;
      return newValue;
    };
    TameInputElement.prototype.getMaxLength = function () {
      return this.node___.maxLength;
    };
    TameInputElement.prototype.setMaxLength = function (newValue) {
      if (!this.editable___) { throw new Error(); }
      this.node___.maxLength = newValue;
      return newValue;
    };
    TameInputElement.prototype.getSize = function () {
      return this.node___.size;
    };
    TameInputElement.prototype.setSize = function (newValue) {
      if (!this.editable___) { throw new Error(); }
      this.node___.size = newValue;
      return newValue;
    };
    TameInputElement.prototype.getType = function () {
      return String(this.node___.type);
    };
    TameInputElement.prototype.setType = function (newValue) {
      if (!this.editable___) { throw new Error(); }
      this.node___.type = newValue;
      return newValue;
    };
    TameInputElement.prototype.getIndex = function () {
      return this.node___.index;
    };
    TameInputElement.prototype.setIndex = function (newValue) {
      if (!this.editable___) { throw new Error(); }
      this.node___.index = newValue;
      return newValue;
    };
    TameInputElement.prototype.getLabel = function () {
      return this.node___.label;
    };
    TameInputElement.prototype.setLabel = function (newValue) {
      if (!this.editable___) { throw new Error(); }
      this.node___.label = newValue;
      return newValue;
    };
    TameInputElement.prototype.getMultiple = function () {
      return this.node___.multiple;
    };
    TameInputElement.prototype.setMultiple = function (newValue) {
      if (!this.editable___) { throw new Error(); }
      this.node___.multiple = newValue;
      return newValue;
    };
    TameInputElement.prototype.getCols = function () {
      return this.node___.cols;
    };
    TameInputElement.prototype.setCols = function (newValue) {
      if (!this.editable___) { throw new Error(); }
      this.node___.cols = newValue;
      return newValue;
    };
    TameInputElement.prototype.getRows = function () {
      return this.node___.rows;
    };
    TameInputElement.prototype.setRows = function (newValue) {
      if (!this.editable___) { throw new Error(); }
      this.node___.rows = newValue;
      return newValue;
    };
    ___.ctor(TameInputElement, TameElement, 'TameInputElement');
    ___.all2(___.grantTypedGeneric, TameInputElement.prototype,
             ['getValue', 'setValue', 'focus', 'getForm', 'getType', 'select']);


    function TameImageElement(node, editable) {
      TameElement.call(this, node, editable);
      exportFields(this, ['src', 'alt']);
    }
    extend(TameImageElement, TameElement);
    nodeClasses.HTMLImageElement = TameImageElement;
    TameImageElement.prototype.getSrc = function () {
      return this.node___.src;
    };
    TameImageElement.prototype.setSrc = function (src) {
      if (!this.editable___) { throw new Error(); }
      this.setAttribute('src', src);
      return src;
    };
    TameImageElement.prototype.getAlt = function () {
      return this.node___.alt;
    };
    TameImageElement.prototype.setAlt = function (src) {
      if (!this.editable___) { throw new Error(); }
      this.node___.alt = src;
      return src;
    };
    ___.ctor(TameImageElement, TameElement, 'TameImageElement');
    ___.all2(___.grantTypedGeneric, TameImageElement.prototype,
             ['getSrc', 'setSrc', 'getAlt', 'setAlt']);


    function TameTableCompElement(node, editable) {
      TameElement.call(this, node, editable);
      exportFields(this,
                   ['colSpan','cells','rowSpan','rows','rowIndex','align',
		    'vAlign','nowrap']);
    }
    extend(TameTableCompElement, TameElement);
    TameTableCompElement.prototype.getColSpan = function () {
      return this.node___.colSpan;
    };
    TameTableCompElement.prototype.setColSpan = function (newValue) {
      if (!this.editable___) { throw new Error(); }
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
      if (!this.editable___) { throw new Error(); }
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
      if (!this.editable___) { throw new Error(); }
      this.node___.align = newValue;
      return newValue;
    };
    TameTableCompElement.prototype.getVAlign = function () {
      return this.node___.vAlign;
    };
    TameTableCompElement.prototype.setVAlign = function (newValue) {
      if (!this.editable___) { throw new Error(); }
      this.node___.vAlign = newValue;
      return newValue;
    };
    TameTableCompElement.prototype.getNowrap = function () {
      return this.node___.nowrap;
    };
    TameTableCompElement.prototype.setNowrap = function (newValue) {
      if (!this.editable___) { throw new Error(); }
      this.node___.nowrap = newValue;
      return newValue;
    };
    ___.ctor(TameTableCompElement, TameElement, 'TameTableCompElement');
 

    function TameTableElement(node, editable) {
      TameTableCompElement.call(this, node, editable);
      exportFields(this,
                   ['tBodies','tHead','tFoot']);
    }
    extend(TameTableElement, TameTableCompElement);
    nodeClasses.HTMLTableElement = TameTableElement;
    TameTableElement.prototype.getTBodies = function () {
      return tameNodeList( this.node___.tBodies, this.editable___ );
    };
    TameTableElement.prototype.getTHead = function () {
      return tameNode( this.node___.tHead, this.editable___ );
    };
    TameTableElement.prototype.getTFoot = function () {
      return tameNode( this.node___.tFoot, this.editable___ );
    };
    TameTableElement.prototype.createTHead = function() {
      if (!this.editable___) { throw new Error(); }
      return tameNode( this.node___.createTHead(), this.editable___ );
    };
    TameTableElement.prototype.deleteTHead = function() {
      if (!this.editable___) { throw new Error(); }
      this.node___.deleteTHead();
    };
    TameTableElement.prototype.createTFoot = function() {
      if (!this.editable___) { throw new Error(); }
      return tameNode( this.node___.createTFoot(), this.editable___ );
    };
    TameTableElement.prototype.deleteTFoot = function() {
      if (!this.editable___) { throw new Error(); }
      this.node___.deleteTFoot();
    };
    ___.ctor(TameTableElement, TameTableCompElement, 'TameTableElement');
    ___.all2(___.grantTypedGeneric, TameTableElement.prototype,
             ['createTHead', 'deleteTHead','createTFoot', 'deleteTFoot']);


    function TameEvent(event) {
      this.event___ = event;
      ___.stamp(tameEventTrademark, this, true);
      exportFields(this, ['type', 'target', 'pageX', 'pageY', 'altKey',
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
      if( this.event___.stopPropagation )
          this.event___.stopPropagation();
      else
          this.event___.cancelBubble = true;
    };
    TameEvent.prototype.preventDefault = function () {
      // TODO(mikesamuel): make sure event doesn't propagate to dispatched
      // events for this gadget only.
      // But don't allow it to stop propagation to the container.
      if( this.event___.preventDefault )
          this.event___.preventDefault();
      else
          this.event___.returnValue = false;
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


    function TameDocument(doc, editable) {
      this.doc___ = doc;
      this.editable___ = editable;
    }
    extend(TameDocument, TameNode);
    nodeClasses.HTMLDocument = TameDocument;
    TameDocument.prototype.addEventListener =
        function (name, listener, useCapture) {
          // TODO(ihab.awad): Implement
        };
    TameDocument.prototype.removeEventListener =
        function (name, listener, useCapture) {
          // TODO(ihab.awad): Implement
        };
    TameDocument.prototype.dispatchEvent =
        function (evt) {
          // TODO(ihab.awad): Implement
        };
    TameDocument.prototype.createElement = function (tagName) {
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
    TameDocument.prototype.createTextNode = function (text) {
      if (!this.editable___) { throw new Error(NOT_EDITABLE); }
      return tameNode(this.doc___.createTextNode(
          text !== null && text !== void 0 ? '' + text : ''), true);
    };
    TameDocument.prototype.getElementById = function (id) {
      id += idSuffix;
      var node = this.doc___.getElementById(id);
      return tameNode(node, this.editable___);
    };
    TameDocument.prototype.getElementsByTagName = function(tagName) {
       //FIXME: this does not consider nested modules, but thats fine for now
       var base = imports.htmlEmitter___.cursor_[0];
       return tameNodeList(
         base.getElementsByTagName(String(tagName)), this.editable___);
     };
    TameDocument.prototype.toString = function () { return '[Fake Document]'; };
    TameDocument.prototype.write = function(text) {
      // TODO(ihab.awad): Needs implementation
      cajita.log('Called document.write() with: ' + text);
    };
    ___.ctor(TameDocument, void 0, 'TameDocument');
    ___.all2(___.grantTypedGeneric, TameDocument.prototype,
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
        nmtoken = rewriteAttribute(null, null, html4.atype.ID, p[i]);
        if (!nmtoken) { throw new Error(nmtokens); }
        out.push(nmtoken);
      }
      return out.join(' ');
    };
    imports.ident___ = function (nmtokens) {
      var p = String(nmtokens).replace(/^\s+|\s+$/g, '').split(/\s+/g);
      var out = [];
      for (var i = 0; i < p.length; ++i) {
        nmtoken = rewriteAttribute(null, null, html4.atype.CLASSES, p[i]);
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

    function TameXMLHttpRequest() {
      exportFields(this, ['requestHeader', 'onreadystatechange', 'readyState',
                          'responseText', 'responseXML', 'responseBody',
                          'status', 'statusText']);
      alert('Created new XHR: ' + this);
    }
    nodeClasses.XMLHttpRequest = TameXMLHttpRequest;
    TameXMLHttpRequest.prototype.abort = function () {
      // TODO(ihab.awad): Implement
    };
    TameXMLHttpRequest.prototype.getAllResponseHeaders = function () {
      // TODO(ihab.awad): Implement
      return "";
    };
    TameXMLHttpRequest.prototype.getResponseHeader = function (headerName) {
      // TODO(ihab.awad): Implement
      return "";
    };
    TameXMLHttpRequest.prototype.open = function (
        method, URL, opt_async, opt_userName, opt_password) {
      // TODO(ihab.awad): Implement
    };
    TameXMLHttpRequest.prototype.send = function(content) {
      // TODO(ihab.awad): Implement
    };
    TameXMLHttpRequest.prototype.setRequestHeader = function (label, value) {
      // TODO(ihab.awad): Implement
    };
    TameXMLHttpRequest.prototype.setOnreadystatechange = function (value) {
      // TODO(ihab.awad): Implement
    };
    TameXMLHttpRequest.prototype.getReadyState = function () {
      // TODO(ihab.awad): Implement
      return 0;
    };
    TameXMLHttpRequest.prototype.getResponseText = function () {
      // TODO(ihab.awad): Implement
      return "";
    };
    TameXMLHttpRequest.prototype.getResponseXML = function () {
      // TODO(ihab.awad): Implement
      return {};
    };
    TameXMLHttpRequest.prototype.getResponseBody = function () {
      // TODO(ihab.awad): Implement
      return "";
    };
    TameXMLHttpRequest.prototype.getStatus = function () {
      // TODO(ihab.awad): Implement
      return 404;
    };
    TameXMLHttpRequest.prototype.getStatusText = function () {
      // TODO(ihab.awad): Implement
      return "Not Found";
    };
    TameXMLHttpRequest.prototype.toString = function () {
      // TODO(ihab.awad): Implement
      return 'Not a real XMLHttpRequest';
    };
    ___.ctor(TameXMLHttpRequest, void 0, 'TameXMLHttpRequest');
    ___.all2(___.grantTypedGeneric, TameXMLHttpRequest.prototype,
             ['abort', 'getAllResponseHeaders', 'getResponseHeader', 'open',
               'send']);

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
     * @param {Number} num
     * @return {String} an CSS representation of num suitable for both html
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

    imports.document = new TameDocument(document, true);

    // TODO(mikesamuel): figure out a mechanism by which the container can
    // specify the gadget's apparent URL.
    // See http://www.whatwg.org/specs/web-apps/current-work/multipage/history.html#location0
    var tameLocation = ___.primFreeze({
      toString: function () { return tameLocation.href; },
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
      // For Valija, attach window object members to outers instead so that the
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
  (typeof console !== 'undefined' && console.log) &&
  console.log(
      'Dispatch %s event thisNode=%o, event=%o, pluginId=%o, handler=%o',
      event.type, thisNode, event, pluginId, handler);
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
  ___.startCallerStack && ___.startCallerStack();
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
