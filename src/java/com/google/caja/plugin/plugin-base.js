// Copyright (C) 2006 Google Inc.
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
 * provides the protected functions used to guarantee plugin namespacing at
 * runtime.
 *
 * THIS FILE IS OBSOLETE.
 * TODO(mikesamuel): replace with DOMita, and replace wrap_capability with
 * Mike Stay's trademarking.
 */

var plugin_hasOwnProperty___ = Object.hasOwnProperty;

var plugin_token___ = {};


/**
 * dispatches an event to an event handler in a safe way.
 * @param {Event} event the unsafe event object.
 * @param {HTMLElement} domNode the domNode the handler is on.
 * @return {Boolean} true to continue event bubbling.
 */
function plugin_dispatchEvent___(event, domNode, outersId, handler) {
  // TODO: turn event into a capability
  var safeEvent = null;
  var safeNode = null;
  var outers = ___.getOuters(outersId);
  if (domNode && domNode !== window) {
    safeNode = outers.plugin_domNode___(domNode, outers.nsPrefix___);
  }
  try {
    var result = !!(handler.call(safeNode, safeEvent));
  } catch (e) {
    var stack = (e && 'stack' in e) ? '' + e.stack : null;
    plugin_log___('handler failed with exception ' + e + '\n\n' + stack);
  }
  return result;
}

function initPlugin(___OUTERS___, nsPrefix, rootId, pathPrefix) {

  /**
   * given a URI, checks whether it is relative or not.  If not, return a
   * placeholder url, "about:blank".
   * @param {String} uri a uri to namespace.  Should be relative.
   * @return {String} a properly namespaced url,
   *   or 'about:blank' if uri is unsafe.
   */
  function plugin_checkUriRelative___(uri) {
    // TODO: should we allow mailto urls?
    if ('string' === typeof uri) {

      // from Appendix B of RFC 3986
      var parts = uri.match(
          /^(([^:\/?#]+):)?(\/\/([^\/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?$/);
      if (parts) {
        do {
          // scheme    = $2
          // authority = $4
          // path      = $5
          // query     = $7
          // fragment  = $9
          var scheme = parts[2];
          var authority = parts[4];
          if (!(scheme || authority)) {
            var ch0 = uri.charAt(0);
            // We do not need to protect against changes to the page's url,
            // because external pages can already cause a page to load with
            // whatever anchor they like.
            // Anchor attacks like
            // archives.neohapsis.com/archives/fulldisclosure/2007-01/0062.html
            // are not relevant because this page is not PDF.
            // Any pages that do printing by redirecting to self with some kind
            // of cookie causing it to serve PDF might suffer.  I don't think
            // anyone does this.
            if (ch0 === '#') { return uri; }

            var path = decodeURIComponent(parts[5]);
            // disallow paths that use .. to get outside the plugin base
            if (path.indexOf('..') >= 0) {
              var origPath = path;
              // collapse ./foo to foo
              for (var tmp;
                   (tmp = path.replace(/(^|\/)\.\//g, '$1')) != path;
                   path = tmp);
              // collapse foo/../bar to bar
              for (var tmp;
                   (tmp = path.replace(
                      /(^|\/)([^\.\/][^\/]*|\.[^\.\/]|\.[^\/]{2,})\/\.\.(\/|$)/,
                      '$1')) != path;
                   path = tmp);
              // if the path is blank or still has a .. in it, then log and
              // return below
              if (!path || /(?:^|\/)\.\.(?:\/|$)/.test(path)) { break; }
              if (path !== origPath) {
                uri = path + (parts[6] || '') + (parts[8] || '');
                ch0 = uri.charAt(0);
              }
            }

            if (pathPrefix && ch0 !== '/') { uri = '/' + uri; }
            return pathPrefix + uri;
          }
        } while (false);
      }
    }
    plugin_log___('unsafe url: ' + uri);
    return 'about:blank';
  }

  /**
   * gets the given member of the given container.
   * @param {Object} m the container
   * @param {Number|String} k the key.  Coerced to a string if not a number.
   * @return m[k] or Undefined.
   */
  function plugin_get___(m, k) {
    // If it's a number, it's safe
    // If it's == null (== null, allows both null and undefined), then it's
    // safe, but probably useless.
    if ('number' !== (typeof k) && null != k) {
      // coerce to a string
      k = '' + k;
      // disallow access to protected variables and the protected namespace
      if ('constructor' === k ||
          ('prototype' === k
           // allow access to user defined prototypes and constructors
           && !plugin_hasOwnProperty___.call(m, k)) ||
          // disallow access to trusted functions that call into untrusted code
          (('caller' === k || '__caller__' === k) && 'function' === typeof m)) {
        // undefined is a reference, so to make sure we return literals as
        //  placeholder values, we use the void operator
        plugin_log___('attempted unsafe access to: ' + k, null);
        return void 0;
      }
    }
    // the regular lookup that this function replaces
    return m[k];
  }

  /**
   * html escape a plain text string.
   *
   * <p>This is suitable for an html snippet -- not an attribute value since
   * those have different escaping requirements and something that can be
   * blessed as a safe html snippet may not be a safe attribute value.
   * {@link #plugin_htmlAttr___} should be used for attribute values.
   *
   * @param {String|Object} text a string of plain text to escape or an html
   *   snippet from plugin_blessHtml___.
   * @return {String} html
   */
  var plugin_html___;
  /**
   * take an html snippet and return an object that can be embedded straight
   * into a GXP.  This can be used to allow trusted code to bless html that it
   * knows is safe to embed, such as user generated html that has already been
   * sanitized.  It is also used by the gxp compiler to bless the output of a
   * gxp since the gxp is known to generate safe html.
   *
   * @param {String} html an html snippet
   * @return {Object} an object that can be given to untrusted text to embed in
   *   a gxp literally.
   */
  var plugin_blessHtml___;
  /**
   * take a css declaration group (e.g. the value of a style
   * attribute) and return an object that can be safely set as a
   * node's style attribute..  This can be used to allow trusted code
   * to bless CSS that it knows is safe, such CSS from a CSS template,
   * or from an existing node's style attribute.
   *
   * @param {String} css a css declaration group.  A declaration group is a
   *   semicolon separated series of <code>decalration</code> productions as
   *   defined in the <a href="http://www.w3.org/TR/REC-CSS2/grammar.html">
   *   CSS 2 grammar</a>.
   * @return {Object} an object that can be passed to a
   *   <code>plugin_domNode___</code>'s <code>setStyle</code> method.
   */
  var plugin_blessCss___;
  /**
   * wraps a dom node to allow access to certain operations.
   * This implementation assumes that the untrusted code is allowed to operate
   * on the subtree rooted at element.  The return value has the following
   * methods:<pre>
   * getNodeType, getNodeName, getNodeValue, getFirstChild, getNextSibling
   *     as per the W3C DOM Node type.
   * getAttribute
   *     as per the W3C DOM Element type.
   * getId, getClass, setClass
   *     getters/setters for the id and class HTML attributes that only expose
   *     ids and classes owned by the calling plugin
   * getInnerHTML, setInnerHTML
   *     getter/setter for the node's innerHTML property.  setInnerHTML can be
   *     used to replace the contents of the node with "safe html", such as the
   *     output from a GXP.
   * getStyle, setStyle
   *     getter/setter for the node's style property.
   * getBounds
   *     get the width, height, and x and y coordingates of the top left.
   *     This returns an object likw { x: 0, y: 0, w: 800, h: 600 } or will
   *     fill an object passed in.  All fields are in pixel-space.
   * </pre>
   * @param {HTMLElement} element
   * @return {Object}
   */
  var plugin_domNode___;
  (function () {
    var token = {};
    /**
     * a snippet of safe html
     * @constructor
     */
    function HtmlSnippetInternal(html) {
      this.html_ = html;
    }
    HtmlSnippetInternal.prototype.toString = function () { return this.html_; };
    var consAndUnlock = protect(HtmlSnippetInternal, token, ['toString']);
    var HtmlSnippet = consAndUnlock[0];
    var unlockHtmlSnippet = consAndUnlock[1];

    plugin_html___ = function (text) {
      if ('string' !== typeof text) {
        if (text instanceof HtmlSnippet) {
          var htmlSnippet = unlockHtmlSnippet(text);
          return htmlSnippet ? htmlSnippet.html_ : '';
        }
        text = '' + text;
      }

      // this actually turns out to be faster than doing one replace with a
      // functional substitution, and it works on Safari.  Did not test with
      // large strings on IE.
      return text.replace(/&/g, '&amp;').replace(/</g, '&lt;')
          .replace(/>/g, '&gt;');
    };

    plugin_blessHtml___ = function (html) {
      return new HtmlSnippet(token, html);
    };

    function CssDeclarationGroupInternal(css) {
      this.css_ = css;
    }
    CssDeclarationGroupInternal.prototype.toString =
      function () { return this.css_; };
    var consAndUnlock = protect(
        CssDeclarationGroupInternal, token, ['toString']);
    var CssDeclarationGroup = consAndUnlock[0];
    var unlockCssDeclarationGroup = consAndUnlock[1];

    plugin_blessCss___ = function (css) {
      return new CssDeclarationGroup(token, css);
    };

    function TameDomNode(node, nsPrefix) {
      console.log('got node ' + node);
      console.assert(node instanceof DOMNode);
      this.node_ = node;
      this.nsPrefix_ = nsPrefix || '';
      // nsPrefix is an html nmtoken prefix so no need to escape
      this.nsPrefixPattern_ =
        nsPrefix
        ? new RegExp('(?:^|\\s)' + nsPrefix + '-([^\\s]+)', 'g')
        : /([^\s]+)/g;
    }
    TameDomNode.prototype.getNodeType = function () {
      return this.node_.nodeType;
    };
    TameDomNode.prototype.getNodeName = function () {
      return this.node_.nodeName;
    };
    TameDomNode.prototype.getNodeValue = function () {
      return this.node_.nodeValue;
    };
    TameDomNode.prototype.stripPrefix_ = function (nmtoken) {
      var i = 0;
      if (/^\s/.test(nmtoken)) { i = 1; }
      var prefix = this.nsPrefix_;
      if (prefix) { i += prefix.length + 1; }
      return nmtoken.substring(i) || null;
    };
    TameDomNode.prototype.getId = function () {
      // return null if it doesn't start with nsPrefix else strip nsPrefix
      var id = this.node_.id;
      var m = id && id.match(this.nsPrefixPattern_);
      return m ? this.stripPrefix_(m[0]) : null;
    };
    TameDomNode.prototype.getClass = function () {
      var cn = this.node_.className;
      var prefixed = cn && cn.match(this.nsPrefixPattern_);
      if (!prefixed) { return ''; }
      var classes = [];
      for (var i = prefixed.length; --i >= 0;) {
        classes[i] = this.stripPrefix_(prefixed[i]);
      }
      return classes.join(' ');
    };
    TameDomNode.prototype.setClass = function (newClassName) {
      if ('string' !== typeof newClassName) { return; }
      var node = this.node_;
      // add the nsPrefix and preserve any existing ones that don't have
      // nsPrefix
      var className = (node.className || '').replace(
          this.nsPrefixPattern_, '');
      var toAdd = newClassName.split(/\s+/g);
      var out = [className];
      var prefix = ' ' + this.nsPrefix_ + '-';
      for (var i = 0, n = toAdd.length; i < n; ++i) {
        var name = toAdd[i];
        // require a well-formed nmtoken
        if (!NMTOKEN_RE___.test(name)) { continue; }
        out.push(prefix, name);
      }
      node.className = out.join('');
    };
    /**
     * A blacklist of elements' whose content is really html.
     * Some elements' innerHTML should not be blessed because it's not html.
     *
     * I think a blacklist is ok here because the gxp compiler elsewhere
     * whitelists elements so any elements added to the html spec that don't
     * have html content and that show up here must come from trusted, and so
     * are presumably well formed for that tag type.
     *
     * TODO: figure out what to do about Safari bug
     * http://bugs.webkit.org/show_bug.cgi?id=12744 which causes PRE to not
     * escape it's content properly in some cases.
     */
    var unblessable = { 'LISTING': null, 'PLAINTEXT': null, 'XMP': null,
                        'STYLE': null, 'SCRIPT': null };
    TameDomNode.prototype.getInnerHTML = function () {
      var node = this.node_;
      var innerHtml = node.innerHTML;
      console.log('got innerHTML: ' + innerHTML);
      if (innerHtml == null) { return innerHtml; }
      var tagName = node.tagName.toUpperCase();
      return tagName in unblessable ?
        innerHtml : plugin_blessHtml___(innerHtml);
    };
    TameDomNode.prototype.setInnerHTML = function (html) {
      if ('object' !== typeof html) {
        this.node_.innerHTML = html_sanitize(html);
        return void 0;
      }
      console.log('unlocked:' + unlockHtmlSnippet(html).html_);
      console.log('this.node_=' + this.node_);
      this.node_.innerHTML = unlockHtmlSnippet(html).html_;
    };
    TameDomNode.prototype.appendInnerHTML = function (html) {
      if ('object' !== typeof html) {
        this.node_.innerHTML += html_sanitize(html);
        //throw ('must pass in a safe html snippet, not type ' + typeof html);
        return void 0;
      }
      this.node_.innerHTML += unlockHtmlSnippet(html).html_;
    };
    TameDomNode.prototype.getStyle = function () {
      var node = this.node_;
      var style = node.style;
      var css;
      if (!style || 'cssText' in style) {
        css = style.cssText;
      } else {
        css = node.getAttribute('style');
      }
      if (!css) { return null; }
      return plugin_blessCss___(css);
    };
    TameDomNode.prototype.setStyle = function (css) {
      if ('object' !== typeof css) {
        throw ('must pass in a safe css snippet, not type ' + typeof css);
      }
      css = unlockCssDeclarationGroup(css);
      this.node_.style.cssText = css.css_;
    };
    TameDomNode.prototype.getFirstChild = function () {
      var child = this.node_.firstChild;
      return child ? new TameDomNode(child, this.nsPrefix_) : null;
    };
    TameDomNode.prototype.getNextSibling = function () {
      var sibling = this.node_.nextSibling;
      return sibling ? new TameDomNode(sibling, this.nsPrefix_) : null;
    };
    // We do not provide a getParent method since we're assuming that a plugin
    // has privileges to a subtree.  We could revisit this by allowing access to
    // a readonly version perhaps by introducing a readonly superclass for
    // TameDomNode
    TameDomNode.prototype.getAttribute = function (attrName) {
      return this.node_.getAttribute(attrName);
    };
    TameDomNode.prototype.getBounds = function (opt_rect) {
      // TODO(msamuel): Implement
    };
    TameDomNode.prototype.getOffsetLeft = function () {
      return this.node_.offsetLeft;
    };
    TameDomNode.prototype.getOffsetWidth = function () {
      return this.node_.offsetWidth;
    };
    TameDomNode.prototype.getOffsetTop = function () {
      return this.node_.offsetTop;
    };
    TameDomNode.prototype.getOffsetHeight = function () {
      return this.node_.offsetHeight;
    };
    TameDomNode.prototype.toString = function () {
      return '[TameDomNode ' + this.node_.nodeName + ']';
    };
    TameDomNode.prototype.equals = function (other) {
      if (other instanceof TameDomNode) { other = unlockDom(other); }
      return other instanceof TameDomNode && this.node_ === other.node_;
    };

    ___.ctor(TameDomNode);
    ___.all2(___.allowMethod, TameDomNode,
        [ 'getNodeType', 'getNodeName', 'getNodeValue', 'getId', 'getClass',
          'setClass', 'getInnerHTML', 'setInnerHTML', 'getStyle', 'setStyle',
          'getFirstChild', 'getNextSibling', 'getAttribute', 'getBounds',
          'getOffsetLeft', 'getOffsetTop', 'getOffsetWidth', 'getOffsetHeight',
          'equals', 'toString', 'appendInnerHTML' ]);

    plugin_domNode___ = function (domNode) {
      return domNode ? new TameDomNode(domNode, nsPrefix) : null;
    };

  })();

  /**
   * escapes an html attribute value
   *
   * @param {String} text a string of plain text to escape.
   * @return {String} html
   */
  function plugin_htmlAttr___(text) {
    if ('string' !== typeof text) { text = '' + text; }
    // this actually turns out to be faster than doing one replace with a
    // functional substitution, and it works on Safari.  Did not test with
    // large strings on IE.
    return text.replace(/&/g, '&amp;').replace(/</g, '&lt;')
      .replace(/>/g, '&gt;').replace(/\042/g, '&quot;')
      // don't use &apos since this is shorter and works for arbitrary xml.
      .replace(/\047/g, '&#39;');
  }

  /**
   * a pettern that matches a subset of nmtokens.
   * http://www.w3.org/TR/1998/REC-xml-19980210#NT-Nmtoken allows non-latin
   * letters, numbers, combining characters, and identifier characters.
   * This regexp does not because that might open up encoding and
   * charset-inference vulnerabilities, and because the correct pattern takes
   * a bit over 3kB.
   * @type {RegExp}
   */
  NMTOKEN_RE___ = /^\w(?:[\w\-:_\.]*\w)?$/;
  /**
   * prefixes the given name tokens
   * @param {String} nmTokens html or css identifiers separated by whitespace
   * @return {String} name tokens prefixed
   */
  function plugin_prefix___(nmTokens) {
    var toks = nmTokens.split(/\s+/);
    var out = [];
    var prefixDash = nsPrefix + '-';
    for (var i = 0; i < toks.length; ++i) {
      var tok = toks[i];
      if (tok === '') { continue; }
      if (!NMTOKEN_RE___.test(tok)) {
        // require a well formed nmtoken
        plugin_log___('malformed NMTOKEN: ' + tok);
        continue;
      }
      if (out.length) { out.push(' '); }
      out.push(prefixDash, tok);
    }
    return out.join('');
  }

  /**
   * maintains a runtime invariant.
   * @param {Boolean} cond the condition to test.
   * @return void if cond is true.  Otherwise exits with an exception.
   */
  function plugin_require___(cond) {
    if (!cond) { throw 'Assertion Failed'; }
  }

  function plugin_log___(msg) {
    console && console.log((___OUTERS___.name || '<unknown>') + ' : ' + msg);
  }

  (function() {
    // define classes to hold the timeout and interval ids
    function TimeoutIdInternal(id) { this.id_ = id; }
    TimeoutIdInternal.prototype.toString = function () {
      return '' + this.id_;
    };

    var consAndUnlock = protect(
        TimeoutIdInternal, plugin_token___, ['toString']);
    TimeoutId = consAndUnlock[0];
    unlockTimeoutId = consAndUnlock[1];

    function IntervalIdInternal(id) { this.id_ = id; }
    IntervalIdInternal.prototype.toString = function () {
      return '' + this.id_;
    };

    consAndUnlock = protect(IntervalIdInternal, plugin_token___, ['toString']);
    IntervalId = consAndUnlock[0];
    unlockIntervalId = consAndUnlock[1];

    /**
     * wraps setTimeout to prevent it from invoking eval.
     * Invokes an operation after a delay.
     * @param {Function} fn the operation to perform after at least the given
     *   delay.
     * @param {Number} delayMillis the minimum amount of time to wait.
     */
    ___OUTERS___.setTimeout = function (fn, delayMillis) {
      if ('function' !== typeof fn) {
        plugin_log___("setTimeout invoked with a string instead of a function");
        return null;
      }
      if ('number' !== typeof delayMillis || !(delayMillis >= 0)) {
        plugin_log___(
            'Bad delay ' + delayMillis + ' : ' + (typeof delayMillis));
        return null;
      }
      var me = this;
      // avoid attacks where a function-like object, such as a regexp, may
      // coerce to a string with code.
      return new TimeoutId(
          plugin_token___,
          setTimeout(function () { fn.call(me); }, delayMillis));
    };

    /**
     * wraps clearTimeout to make sure the plugin can only clear timeouts it
     * set.
     */
    ___OUTERS___.clearTimeout = function (id) {
      id = unlockTimeoutId(id);
      if (id) { clearTimeout(id.id_); }
    };

    /**
     * wraps setInterval to prevent it from invoking eval.
     * Invokes an operation repeatedly.
     * @param {Function} fn the operation to perform.
     * @param {Number} periodMillis the minimum amount of time between
     *   occurrences and the minimum amount of time before the first occurrence.
     */
    ___OUTERS___.setInterval = function (fn, delayMillis) {
      if ('function' !== typeof fn) {
        plugin_log___(
            "setInterval invoked with a string instead of a function");
        return null;
      }
      if ('number' !== typeof periodMillis || !(periodMillis >= 0)) {
        plugin_log___('Bad period ' + periodMillis + ' : ' +
                      (typeof periodMillis));
        return null;
      }
      var me = this;
      // avoid attacks where a function-like object, such as a regexp, may
      // coerce to a string with code.
      return new IntervalId(
          plugin_token___,
          setInterval(function () { fn.call(me); }, periodMillis));
    };

    /**
     * wraps clearInterval to make sure the plugin can only clear intervals it
     * set.
     */
    ___OUTERS___.clearInterval = function (id) {
      id = unlockIntervalId(id);
      if (id instanceof IntervalIdInternal) { clearinterval(id.id_); }
    };

  })();

  
  // Functions used to format the output from CSS substitutions
  /**
   * given a number, outputs the equivalent css text.
   * @param {Number} num
   * @return {String} an CSS representation of a number suitable for both html
   *    attribs and plain text.
   */
  ___OUTERS___.plugin_cssNumber___ = function (num) {
    return ('number' === typeof num && isFinite(num) ? '' + num : '0');
  };
  /**
   * given a number as 24 bits of RRGGBB, outputs a properly formatted CSS
   * color.
   * @param {Number} num
   * @return {String} an CSS representation of num suitable for both html
   *    attribs and plain text.
   */
  ___OUTERS___.plugin_cssColor___ = function (color) {
    // TODO: maybe whitelist the color names defined for CSS if the arg is a
    // string.
    if ('number' !== typeof color || !color) { return '#000'; }
    var hex = '0123456789abcdef'.split('');
    return '#' + hex[(color >> 20) & 0xf] + hex[(color >> 16) & 0xf] +
      hex[(color >> 12) & 0xf] + hex[(color >> 8) & 0xf] +
      hex[(color >> 4) & 0xf] + hex[color & 0xf];
  };
  /**
   * given a relative URI, outputs a properly formatted CSS uri.
   * It must be escaped before embedding in html or an html attribute.
   * @param {String} uri
   * @return {String} an CSS representation of uri
   */
  ___OUTERS___.plugin_cssUri___ = function (uri) {
    uri = plugin_checkUriRelative___(uri);
    if (/[\'\\]/.test(uri)) { uri = 'about:blank'; }
    return 'url(\'' + uri + '\')';
  };

  ___OUTERS___.plugin_checkUriRelative___ = plugin_checkUriRelative___;

  ___OUTERS___.plugin_prefix___ = plugin_prefix___;

  ___OUTERS___.plugin_html___ = plugin_html___;

  ___OUTERS___.plugin_htmlAttr___ = plugin_htmlAttr___;
  
  ___OUTERS___.plugin_blessHtml___ = plugin_blessHtml___;

  ___OUTERS___.plugin_blessCss___ = plugin_blessCss___;

  ___OUTERS___.nsPrefix = nsPrefix;

  if (nsPrefix) {
    var fullPrefix = nsPrefix + '-';
    ___OUTERS___.document = {
      getElementById: ___.simpleFunc(function (id) {
        if ('string' !== typeof id || !id) { throw 'Invalid id ' + id; }
        id = fullPrefix + id;
        var domNode = document.getElementById(id);
        console.log('looked up ' + domNode + ' for ' + id);
        return domNode ? plugin_domNode___(domNode) : null;
      })
    };
  }
  
  // TODO: capabilities for CSS snippets so CSS templates can be used
  // standalone, and so that the style from one node can be taken and injected
  // into another
}
