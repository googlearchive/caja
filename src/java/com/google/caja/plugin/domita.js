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
 * Requires caja.js, html-defs.js, html-sanitizer.js, unicode.js.
 *
 * Caveats:
 * - This is not a full implementation.
 * - Security Review is pending.
 * - <code>===</code> and <code>!==</code> on tamed DOM nodes will not behave
 *   the same as with untamed nodes.  Specifically, it is not always true that
 *   {@code document.getElementById('foo') === document.getElementById('foo')}.
 *
 * @author mikesamuel@gmail.com
 */


/**
 * Add a tamed document implementation to a Gadget's global scope.
 *
 * @param {string} idPrefix a string prefix prepended to all node IDs.
 * @param {Object} uriCallback an object like <pre>{
 *       rewrite: function (uri, mimeType) { return safeUri }
 *     }</pre>.
 *     The rewrite function should be idempotent to allow rewritten HTML
 *     to be reinjected.
 * @param {Object} outers the gadget's global scope.
 */
attachDocumentStub = (function () {
  var tameNodeSecret = {};

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

  function assert(cond) {
    if (!cond) {
      console && (console.log('domita assertion failed'), console.trace());
      throw new Error();
    }
  }

  /**
   * Add setter and getter hooks so that the caja {@code node.innerHTML = '...'}
   * works as expected.
   */
  function exportFields(ctor, fields) {
    for (var i = fields.length; --i >= 0;) {
      var field = fields[i];
      var fieldUCamel = field.charAt(0).toUpperCase() + field.substring(1);
      var getterName = 'get' + fieldUCamel;
      var setterName = 'set' + fieldUCamel;
      var count = 0;
      if (ctor.prototype.hasOwnProperty(getterName)) {
        ++count;
        ___.useGetHandler(
           ctor.prototype, field, ctor.prototype[getterName]);
      }
      if (ctor.prototype.hasOwnProperty(setterName)) {
        ++count;
        ___.useSetHandler(
           ctor.prototype, field, ctor.prototype[setterName]);
      }
      if (!count) {
        throw new Error('Failed to export field ' + field + ' on ' + ctor);
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

  // TODO(mikesamuel): replace with Mike Stay's trademarking.
  function makeSealerUnsealerPair(exposeAsPrimitive) {
    var cache;
    function seal(x) {
      var o = { test___: function () { cache = x; } };
      if (exposeAsPrimitive) {
        o.valueOf = function (typeHint) { return x; };
        o.toString = function () { return String(x); };
      }
      return o;
    }
    function unseal(sealed) {
      var x;
      try {
        cache = null;
        sealed && sealed.test___ && sealed.test___();
        x = cache;
      } finally {
        cache = null;
      }
      return x;
    }
    return { seal: seal, unseal: unseal };
  }

  var htmlSealerUnsealerPair = makeSealerUnsealerPair(true);
  var cssSealerUnsealerPair = makeSealerUnsealerPair(false);

  // See above for a description of this function.
  function attachDocumentStub(idPrefix, uriCallback, outers) {
    var elementPolicies = {};
    elementPolicies.FORM = function (attribs) {
      // Forms must have a gated onsubmit handler or they must have an
      // external target.
      var sawHandler = false;
      for (var i = 0, n = attribs.length; i < n; i += 2) {
        if (attribs[i] === 'ONSUBMIT') {
          sawHandler = true;
        }
      }
      if (!sawHandler) {
        attribs.push('ONSUBMIT', 'return false');
      }
      return attribs;
    };
    elementPolicies.A = elementPolicies.AREA = function (attribs) {
      // Anchor tags must have a target.
      attribs.push('TARGET', '_blank');
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
            if (html4.ATTRIBS.hasOwnProperty(attribName)) {
              var atype = html4.ATTRIBS[attribName];
              value = rewriteAttribute(tagName, attribName, atype, value);
            } else {
              value = null;
            }
            if (value != null) {  // intentionally matches undefined
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
            if (attribName === 'TARGET') { continue; }
            var atype = html4.ATTRIBS[attribName];
            var value = attribs[i + 1];
            if (atype === html4.atype.IDREF) {
              if (value.length <= idPrefix.length
                  || idPrefix !== value.substring(0, idPrefix.length)) {
                continue;
              }
              value = value.substring(idPrefix.length);
            }
            if (value != null) {
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

    function rewriteAttribute(tagName, attribName, type, value) {
      switch (type) {
        case html4.atype.IDREF:
          value = String(value);
          if (!(value && isXmlName(value))) { return null; }
          return idPrefix + value;
        case html4.atype.NAME:
          value = String(value);
          if (!(value && isXmlName(value))) { return null; }
          return value;
        case html4.atype.NMTOKENS:
          value = String(value);
          if (!(value && isXmlNmTokens(value))) { return null; }
          return value;
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
          var pluginId = ___.getId(outers);
          value = (doesReturn ? 'return ' : '') + 'plugin_dispatchEvent___('
              + 'this, event || window.event, ' + pluginId + ', "'
              + fnName + '");';
          if (attribName === 'ONSUBMIT') {
            value = 'try { ' + value + ' } finally { return false; }';
          }
          return value;
        case html4.atype.URI:
          value = String(value);
          if (!uriCallback) { return null; }
          // TODO(mikesamuel): determine mime type properly.
          return uriCallback.rewrite(value, '*/*') || null;
        case html4.atype.STYLE:
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
        case html4.atype.FRAME:
          value = String(value);
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
      if (node == null) { return null; }  // Returns null for undefined.
      var tamed;
      switch (node.nodeType) {
        case 1:  // Element
          switch (node.tagName.toUpperCase()) {
            case 'FORM':
              tamed = new TameFormElement(node, editable);
              break;
            case 'INPUT':
              tamed = new TameInputElement(node, editable);
              break;
            default:
              tamed = new TameElement(node, editable);
              break;
          }
          break;
        case 3:  // Text
          tamed = new TameTextNode(node, editable);
          break;
        default:
          return null;
      }
      return tamed;
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
      tamed.item = function (k) {
        k &= 0x7fffffff;
        if (isNaN(k)) { throw new Error(); }
        return this[k] || null;
      };
      // TODO(mikesamuel): if opt_keyAttrib, could implement getNamedItem
      return tamed;
    }

    /**
     * Base class for a Node wrapper.  Do not create directly -- use the
     * tameNode factory instead.
     * @constructor
     */
    function TameNode(node, editable) {
      this.node___ = node;
      this.editable___ = editable;
    }
    TameNode.prototype.secret___ = tameNodeSecret;
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
      if (child.secret___ !== tameNodeSecret
          || !this.editable___ || !child.editable___) {
        throw new Error();
      }
      this.node___.appendChild(child.node___);
    };
    TameNode.prototype.insertBefore = function (child) {
      if (child.secret___ !== tameNodeSecret
          || !this.editable___ || !child.editable___) {
        throw new Error();
      }
      this.node___.insertBefore(child.node___);
    };
    TameNode.prototype.removeChild = function (child) {
      if (child.secret___ !== tameNodeSecret || !this.editable___) {
        throw new Error();
      }
      this.node___.removeChild(child.node___);
    };
    TameNode.prototype.replaceChild = function (child, replacement) {
      if (child.secret___ !== tameNodeSecret
          || replacement.secret___ !== tameNodeSecret
          || !this.editable___
          || !replacement.editable___) {
        throw new Error();
      }
      this.node___.replaceChild(child.node___, replacement.node___);
    };
    TameNode.prototype.getFirstChild = function () {
      return tameNode(this.node___.firstChild);
    };
    TameNode.prototype.getLastChild = function () {
      return tameNode(this.node___.lastChild);
    };
    TameNode.prototype.getNextSibling = function () {
      return tameNode(this.node___.nextSibling);
    };
    TameNode.prototype.getPrevSibling = function () {
      return tameNode(this.node___.prevSibling);
    };
    TameNode.prototype.getElementsByTagName = function (tagName) {
      return tameNodeList(
          this.node___.getElementsByTagName(String(tagName)), this.editable___);
    };
    ___.ctor(TameNode, undefined, 'TameNode');
    ___.all2(
       ___.allowMethod, TameNode,
       ['getNodeType', 'getNodeValue', 'getNodeName',
        'appendChild', 'insertBefore', 'removeChild', 'replaceChild',
        'getFirstChild', 'getLastChild', 'getNextSibling', 'getPrevSibling',
        'getElementsByTagName']);
    exportFields(TameNode, ['nodeType', 'nodeValue', 'nodeName', 'firstChild',
                            'lastChild', 'nextSibling', 'prevSibling']);

    function TameTextNode(node, editable) {
      assert(node.nodeType === 3);
      TameNode.call(this, node, editable);
    }
    extend(TameTextNode, TameNode);
    TameTextNode.prototype.setNodeValue = function (value) {
      if (!this.editable___) { throw new Error(); }
      this.node___.nodeValue = String(value || '');
    };
    TameTextNode.prototype.getData = TameTextNode.prototype.getNodeValue;
    TameTextNode.prototype.setData = TameTextNode.prototype.setNodeValue;
    TameTextNode.prototype.toString = function () {
      return '#text';
    };
    ___.ctor(TameTextNode, undefined, 'TameNode');
    ___.all2(___.allowMethod, TameTextNode,
             ['setNodeValue', 'getData', 'setData']);
    exportFields(TameTextNode, ['nodeValue', 'data']);


    function TameElement(node, editable) {
      assert(node.nodeType === 1);
      TameNode.call(this, node, editable);
    }
    extend(TameElement, TameNode);
    TameElement.prototype.getId = function () {
      return this.getAttribute('ID') || '';
    };
    TameElement.prototype.setId = function (newId) {
      return this.setAttribute('ID', newId);
    };
    TameElement.prototype.getAttribute = function (name) {
      name = String(name).toUpperCase();
      var type = html4.ATTRIBS[name];
      if (type === undefined || !html4.ATTRIBS.hasOwnProperty(name)) {
        return null;
      }
      var value = this.node___.getAttribute(name);
      if ('string' !== typeof value) { return value; }
      switch (type) {
        case html4.atype.IDREF:
          var n = idPrefix.length;
          if (value && value.length >= n
              && idPrefix === value.substring(0, n)) {
            return value.substring(n);
          }
          return '';
        default:
          return value;
      }
    };
    TameElement.prototype.setAttribute = function (name, value) {
      if (!this.editable___) { throw new Error(); }
      name = String(name).toUpperCase();
      var type = html4.ATTRIBS[name];
      if (type === undefined || !html4.ATTRIBS.hasOwnProperty(name)) {
        throw new Error();
      }
      var sanitizedValue = rewriteAttribute(
          this.node___.tagName, name, type, value);
      if (sanitizedValue !== null) {
        this.node___.setAttribute(name, sanitizedValue);
      }
    };
    TameElement.prototype.getClassName = function () {
      return getAttribute('class');
    };
    TameElement.prototype.setClassName = function (classes) {
      if (!this.editable___) { throw new Error(); }
      return this.getAttribute('class', String(classes));
    };
    TameElement.prototype.getTagName = TameNode.prototype.getNodeName;
    TameElement.prototype.getInnerHTML = function () {
      var tagName = this.node___.tagName.toUpperCase();
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
        // TODO(mikesamuel): seal this?
        innerHtml = tameInnerHtml(innerHtml);
      }
      // TODO(mikesamuel): rewrite ids
      return innerHtml;
    };
    TameElement.prototype.setInnerHTML = function (html) {
      if (!this.editable___) { throw new Error(); }
      var tagName = this.node___.tagName.toUpperCase();
      if (!html4.ELEMENTS.hasOwnProperty(tagName)) { throw new Error(); }
      var flags = html4.ELEMENTS[tagName];
      if (flags & html4.eflags.UNSAFE) { throw new Error(); }
      if (flags & html4.eflags.RCDATA) {
        html = String(html || '');
        html = html.normalizeRCData(html);
      } else {
        var unsealed = htmlSealerUnsealerPair.unseal(html);
        if (unsealed) {
          html = unsealed;
        } else {
          html = sanitizeHtml(String(html || ''));
        }
      }
      this.node___.innerHTML = html;
    };
    TameElement.prototype.setStyle = function (style) {
      this.setAttribute('STYLE', style);
    };
    TameElement.prototype.getStyle = function () {
      return this.node___.style.cssText;
    };
    TameElement.prototype.updateStyle = function (style) {
      if (!this.editable___) { throw new Error(); }
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
        style[propName] = propValue;
      }
    };
    TameElement.prototype.addEventListener = function (name, listener, bubble) {
      if (!this.editable___) { throw new Error(); }
      if ('function' !== typeof listener) {
        throw new Error();
      }
      name = String(name);
      if (this.node___.addEventListener) {
        this.node___.addEventListener(
            name,
            function (event) {
              return plugin_dispatchEvent___(
                  this, event || window.event, ___.getId(outers),
                  listener);
            }, bubble === undefined ? undefined : Boolean(bubble));
      } else {
        var thisNode = this;
        this.node___.attachEvent('on' + name, dispatcher);
      }
    };
    TameElement.prototype.toString = function () {
      return '<' + this.node___.tagName + '>';
    };
    ___.ctor(TameElement, TameNode, 'TameElement');
    ___.all2(
       ___.allowMethod, TameElement,
       ['addEventListener', 'getAttribute', 'setAttribute',
        'getClassName', 'setClassName', 'getId', 'setId',
        'getInnerHTML', 'setInnerHTML', 'updateStyle', 'getStyle', 'setStyle',
        'getTagName']);
    exportFields(TameElement,
                 ['className', 'id', 'innerHTML', 'tagName', 'style']);


    function TameFormElement(node, editable) {
      TameElement.call(this, node, editable);
    }
    extend(TameFormElement, TameElement);
    TameFormElement.prototype.getElements = function () {
      return tameNodeList(this.node___.elements, this.editable___, 'name');
    };
    ___.ctor(TameFormElement, TameElement, 'TameFormElement');
    ___.all2(___.allowMethod, TameFormElement, ['getElements']);
    exportFields(TameFormElement, ['elements']);


    function TameInputElement(node, editable) {
      TameElement.call(this, node, editable);
    }
    extend(TameInputElement, TameElement);
    TameInputElement.prototype.getValue = function () {
      var value = this.node___.value;
      return value == null ? null : String(value); // Return null for undefined.
    };
    TameInputElement.prototype.setValue = function (newValue) {
      if (!this.editable___) { throw new Error(); }
      // == matches undefined
      this.node___.value = (newValue == null ? '' : '' + value);
    };
    TameInputElement.prototype.focus = function () {
      if (!this.editable___) { throw new Error(); }
      this.node___.focus();
    };
    TameInputElement.prototype.blur = function () {
      if (!this.editable___) { throw new Error(); }
      this.node___.blur();
    };
    TameInputElement.prototype.getForm = function () {
      return tameNode(this.node___.form);
    };
    ___.ctor(TameInputElement, TameElement, 'TameInputElement');
    ___.all2(___.allowMethod, TameInputElement,
             ['getValue', 'setValue', 'focus', 'getForm']);
    exportFields(TameInputElement, ['value', 'form']);


    function TameEvent(event) {
      this.event___ = event;
    }
    TameEvent.prototype.getType = function () {
      return String(this.event___.type);
    };
    TameEvent.prototype.getTarget = function () {
      return tameNode(this.event___.target, true);
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
      return this.event___.stopPropagation();
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
    TameEvent.prototype.getWhich = function () {
      var w = this.event___.which;
      return w && Number(w);
    };
    TameEvent.prototype.getKeyCode = function () {
      var kc = this.event___.keyCode;
      return kc && Number(kc);
    };
    TameEvent.prototype.toString = function () { return 'Not a real event'; };
    TameEvent.prototype.current
    ___.ctor(TameEvent, undefined, 'TameEvent');
    ___.all2(___.allowMethod, TameEvent,
             ['getType', 'getTarget', 'getPageX', 'getPageY', 'stopPropagation',
              'getAltKey', 'getCtrlKey', 'getMetaKey', 'getShiftKey',
              'getButton', 'getClientX', 'getClientY',
              'getKeyCode', 'getWhich']);
    exportFields(TameEvent, ['type', 'target', 'pageX', 'pageY', 'altKey',
                             'ctrlKey', 'metaKey', 'shiftKey', 'button',
                             'clientX', 'clientY', 'keyCode', 'which']);


    function TameDocument(doc, editable) {
      this.doc___ = doc;
      this.editable___ = editable;
    }
    TameDocument.prototype.createElement = function (tagName) {
      if (!this.editable___) { throw new Error(); }
      tagName = String(tagName).toUpperCase();
      if (!html4.ELEMENTS.hasOwnProperty(tagName)) { throw new Error(); }
      var flags = html4.ELEMENTS[tagName];
      if (flags & html4.eflags.UNSAFE) { throw new Error(); }
      var newEl = this.doc___.createElement(tagName);
      if (elementPolicies.hasOwnProperty(tagName)) {
        var attribs = elementPolicies[tagName]([]);
        if (attribs) {
          for (var i = 0; i < attribs.length; i += 2) {
            newEl.setAttribute(attribs[i], attribs[i + 1]);
          }
        }
      }
      return tameNode(newEl, true);
    };
    TameDocument.prototype.createTextNode = function (text) {
      if (!this.editable___) { throw new Error(); }
      return tameNode(  // == matches undefined
          this.doc___.createTextNode(text != null ? '' + text : ''), true);
    };
    TameDocument.prototype.getElementById = function (id) {
      id = idPrefix + id;
      var node = this.doc___.getElementById(id);
      return tameNode(node, this.editable___);
    };
    TameDocument.prototype.toString = function () { return '[Fake Document]'; };
    ___.ctor(TameDocument, undefined, 'TameDocument');
    ___.all2(___.allowMethod, TameDocument,
             ['createElement', 'createTextNode', 'getElementById']);

    outers.tameNode___ = tameNode;
    outers.tameEvent___ = function (event) { return new TameEvent(event); };
    outers.blessHtml___ = htmlSealerUnsealerPair.seal;
    outers.blessCss___ = function (var_args) {
      var arr = [];
      for (var i = 0, n = arguments.length; i < n; ++i) {
        arr[i] = arguments[i];
      }
      return cssSealerUnsealerPair.seal(arr);
    }
    outers.htmlAttr___ = function (s) {
      return html.escapeAttrib(String(s || ''));
    };
    outers.html___ = function (s) {
      var unsealed = htmlSealerUnsealerPair.unseal(s);
      if (unsealed) { return unsealed; }
      return html.escapeAttrib(String(s || ''));
    };
    outers.rewriteUri___ = function (uri, mimeType) {
      var s = rewriteAttribute(null, null, html4.atype.URI, uri);
      if (!s) { throw new Error(); }
      return s;
    };
    outers.prefix___ = function (nmtokens) {
      var p = String(nmtokens).replace(/^\s+|\s+$/g, '').split(/\s+/g);
      var out = [];
      for (var i = 0; i < p.length; ++i) {
        nmtoken = rewriteAttribute(null, null, html4.atype.IDREF, p[i]);
        if (!nmtoken) { throw new Error(nmtokens); }
        out.push(nmtoken);
      }
      return out.join(' ');
    };

    /**
     * given a number, outputs the equivalent css text.
     * @param {number} num
     * @return {string} an CSS representation of a number suitable for both html
     *    attribs and plain text.
     */
    outers.cssNumber___ = function (num) {
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
    outers.cssColor___ = function (color) {
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
    outers.cssUri___ = function (uri, mimeType) {
      var s = rewriteAttribute(null, null, html4.atype.URI, uri);
      if (!s) { throw new Error(); }
      return s;
    };

    outers.document = new TameDocument(document, true);
  }

  return attachDocumentStub;
})();

/**
 * Function called from rewritten event handlers to dispatch an event safely.
 */
function plugin_dispatchEvent___(thisNode, event, pluginId, handler) {
  (typeof console !== 'undefined' && console.log) &&
  console.log(
      'Dispatch %s event thisNode=%o, event=%o, pluginId=%o, handler=%o',
      event.type, thisNode, event, pluginId, handler);
  var outers = ___.getOuters(pluginId);
  switch (typeof handler) {
    case 'string':
      handler = outers[handler];
      break;
    case 'function':
      break;
    default:
      throw new Error(
          'Expected function as event handler, not ' + typeof handler);
  }
  return (___.asSimpleFunc(handler))(
      outers.tameNode___(thisNode), outers.tameEvent___(event));
}
