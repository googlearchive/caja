// Copyright (C) 2008-2012 Google Inc.
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
 * <li>Security Review is pending.
 * <li><code>===</code> and <code>!==</code> on node lists will not
 *   behave the same as with untamed node lists.  Specifically, it is
 *   not always true that {@code nodeA.childNodes === nodeA.childNodes}.
 * </ul>
 *
 * <p>
 * TODO(ihab.awad): Our implementation of getAttribute (and friends)
 * is such that standard DOM attributes which we disallow for security
 * reasons (like 'form:enctype') are placed in the "virtual" attributes
 * map (the data-caja-* namespace). They appear to be settable and gettable,
 * but their values are ignored and do not have the expected semantics
 * per the DOM API. This is because we do not have a column in
 * html4-defs.js stating that an attribute is valid but explicitly
 * blacklisted. Alternatives would be to always throw upon access to
 * these attributes; to make them always appear to be null; etc. Revisit
 * this decision if needed.
 *
 * @author mikesamuel@gmail.com (original Domita)
 * @author kpreid@switchb.org (port to ES5)
 * @requires console
 * @requires bridalMaker, cajaVM, cssSchema, lexCss, URI
 * @requires parseCssDeclarations, sanitizeCssProperty, unicode
 * @requires html, html4, htmlSchema
 * @requires WeakMap, Proxy
 * @requires CSS_PROP_BIT_HISTORY_INSENSITIVE
 * @requires HtmlEmitter
 * @provides Domado
 * @overrides window
 */

// The Turkish i seems to be a non-issue, but abort in case it is.
if ('I'.toLowerCase() !== 'i') { throw 'I/i problem'; }

// TODO(kpreid): Review whether multiple uses of np() should be coalesced for
// efficiency.

var Domado = (function() {
  'use strict';

  var isVirtualizedElementName = htmlSchema.isVirtualizedElementName;
  var realToVirtualElementName = htmlSchema.realToVirtualElementName;
  var virtualToRealElementName = htmlSchema.virtualToRealElementName;

  var cajaPrefix = 'data-caja-';
  var cajaPrefRe = new RegExp('^' + cajaPrefix);

  // From RFC3986
  var URI_SCHEME_RE = new RegExp(
      '^' +
      '(?:' +
        '([^:\/?# ]+)' +         // scheme
      ':)?'
  );

  var ALLOWED_URI_SCHEMES = /^(?:https?|mailto)$/i;

  /**
   * Tests if the given uri has an allowed scheme.
   * This matches the logic in UriPolicyNanny#apply
   */
  function allowedUriScheme(uri) {
    var parsed = ('' + uri).match(URI_SCHEME_RE);
    return (parsed && (!parsed[1] || ALLOWED_URI_SCHEMES.test(parsed[1])));
  }

  function uriFetch(naiveUriPolicy, uri, mime, callback) {
    if (!naiveUriPolicy || !callback
      || 'function' !== typeof naiveUriPolicy.fetch) {
      return;
    }
    if (allowedUriScheme(uri)) {
      naiveUriPolicy.fetch(uri, mime, callback);
    } else {
      naiveUriPolicy.fetch(undefined, mime, callback);
    }
  }

  function uriRewrite(naiveUriPolicy, uri, effects, ltype, hints) {
    if (!naiveUriPolicy) { return null; }
    return allowedUriScheme(uri) ?
        'function' === typeof naiveUriPolicy.rewrite ?
          naiveUriPolicy.rewrite(uri, effects, ltype, hints)
        : null
      : null;
  }

  // TODO(kpreid): If not used for the upcoming modularity-of-element-tamings
  // refactoring, eliminate the domitaModules object.
  var domitaModules = {};

  domitaModules.proxiesAvailable = typeof Proxy !== 'undefined';

  // The proxy facilities provided by Firefox and ES5/3 differ in whether the
  // proxy itself (or rather 'receiver') is the first argument to the 'get'
  // traps. This autoswitches as needed, removing the first argument.
  domitaModules.permuteProxyGetSet = (function () {
    var needToSwap = false;

    if (domitaModules.proxiesAvailable) {
      var testHandler = {
        set: function (a, b, c) {
          if (a === proxy && b === "foo" && c === 1) {
            needToSwap = true;
          } else if (a === "foo" && b === 1 && c === proxy) {
            // needToSwap already false
          } else if (a === "foo" && b === 1 && c === undefined) {
            throw new Error('Proxy implementation does not provide proxy '
                + 'parameter: ' + Array.prototype.slice.call(arguments, 0));
          } else {
            throw new Error('internal: Failed to understand proxy arguments: '
                + Array.prototype.slice.call(arguments, 0));
          }
          return true;
        }
      };
      var proxy = Proxy.create(testHandler);
      proxy.foo = 1;
    }

    if (needToSwap) {
      return {
        getter: function (getFunc) {
          function permutedGetter(proxy, name) {
            return getFunc.call(this, name, proxy);
          };
          permutedGetter.unpermuted = getFunc;
          return permutedGetter;
        },
        setter: function (setFunc) {
          function permutedSetter(proxy, name, value) {
            return setFunc.call(this, name, value, proxy);
          };
          permutedSetter.unpermuted = setFunc;
          return permutedSetter;
        }
      };
    } else {
      return {
        getter: function (getFunc) {
          getFunc.unpermuted = getFunc;
          return getFunc;
        },
        setter: function (setFunc) {
          setFunc.unpermuted = setFunc;
          return setFunc;
        }
      };
    }
  })();

  domitaModules.canHaveEnumerableAccessors = (function () {
    // Firefox bug causes enumerable accessor properties to appear as own
    // properties of children. SES patches this by prohibiting enumerable
    // accessor properties. We work despite the bug by making all such
    // properties non-enumerable using this flag.
    try {
      Object.defineProperty({}, "foo", {
        enumerable: true,
        configurable: false,
        get: function () {}
      });
      return true;
    } catch (e) {
      return false;
    }
  })();

  domitaModules.getPropertyDescriptor = function (o, n) {
    if (o === null || o === undefined) {
      return undefined;
    } else {
      return Object.getOwnPropertyDescriptor(o, n)
          || domitaModules.getPropertyDescriptor(Object.getPrototypeOf(o), n);
    }
  };

  // This is a simple forwarding proxy handler. Code copied 2011-05-24 from
  // <http://wiki.ecmascript.org/doku.php?id=harmony:proxy_defaulthandler>
  // with modifications to make it work on ES5-not-Harmony-but-with-proxies as
  // provided by Firefox 4.0.1.
  domitaModules.ProxyHandler = function (target) {
    this.target = target;
  };
  domitaModules.ProxyHandler.prototype = {
    // == fundamental traps ==

    // Object.getOwnPropertyDescriptor(proxy, name) -> pd | undefined
    getOwnPropertyDescriptor: function(name) {
      var desc = Object.getOwnPropertyDescriptor(this.target, name);
      if (desc !== undefined) { desc.configurable = true; }
      return desc;
    },

    // Object.getPropertyDescriptor(proxy, name) -> pd | undefined
    getPropertyDescriptor: function(name) {
      var desc = Object.getPropertyDescriptor(this.target, name);
      if (desc !== undefined) { desc.configurable = true; }
      return desc;
    },

    // Object.getOwnPropertyNames(proxy) -> [ string ]
    getOwnPropertyNames: function() {
      return Object.getOwnPropertyNames(this.target);
    },

    // Object.getPropertyNames(proxy) -> [ string ]
    getPropertyNames: function() {
      return Object.getPropertyNames(this.target);
    },

    // Object.defineProperty(proxy, name, pd) -> undefined
    defineProperty: function(name, desc) {
      return Object.defineProperty(this.target, name, desc);
    },

    // delete proxy[name] -> boolean
    'delete': function(name) { return delete this.target[name]; },

    // Object.{freeze|seal|preventExtensions}(proxy) -> proxy
    fix: function() {
      // As long as target is not frozen,
      // the proxy won't allow itself to be fixed
      if (!Object.isFrozen(this.target)) {
        return undefined;
      }
      var props = {};
      for (var name in this.target) {
        props[name] = Object.getOwnPropertyDescriptor(this.target, name);
      }
      return props;
    },

    // == derived traps ==

    // name in proxy -> boolean
    has: function(name) { return name in this.target; },

    // ({}).hasOwnProperty.call(proxy, name) -> boolean
    hasOwn: function(name) {
      return ({}).hasOwnProperty.call(this.target, name);
    },

    // proxy[name] -> any
    get: domitaModules.permuteProxyGetSet.getter(
        function(name, proxy) { return this.target[name]; }),

    // proxy[name] = value
    set: domitaModules.permuteProxyGetSet.setter(
        function(name, value, proxy) { this.target[name] = value; }),

    // for (var name in proxy) { ... }
    enumerate: function() {
      var result = [];
      for (var name in this.target) { result.push(name); };
      return result;
    },

    /*
    // if iterators would be supported:
    // for (var name in proxy) { ... }
    iterate: function() {
      var props = this.enumerate();
      var i = 0;
      return {
        next: function() {
          if (i === props.length) throw StopIteration;
          return props[i++];
        }
      };
    },*/

    // Object.keys(proxy) -> [ string ]
    keys: function() { return Object.keys(this.target); }
  };
  cajaVM.def(domitaModules.ProxyHandler);


  /**
   * Like object[propName] = value, but DWIMs enumerability.
   *
   * This is also used as a workaround for possible bugs/unfortunate-choices in
   * SES, where a non-writable property cannot be overridden by an own property
   * by simple assignment. Or maybe I (kpreid) am misunderstanding what the
   * right thing is.
   *
   * The property's enumerability is inherited from the ancestor's property's
   * descriptor. The property is not writable or configurable.
   *
   * TODO(kpreid): Attempt to eliminate the need for uses of this. Some may be
   * due to a fixed bug in ES5/3.
   */
  domitaModules.setOwn = function (object, propName, value) {
    propName += '';
    // IE<=8, DOM objects are missing 'valueOf' property'
    var desc = domitaModules.getPropertyDescriptor(object, propName);
    Object.defineProperty(object, propName, {
      enumerable: desc ? desc.enumerable : false,
      value: value
    });
  };

  /**
   * Checks that a user-supplied callback is a function. Return silently if the
   * callback is valid; throw an exception if it is not valid.
   *
   * TODO(kpreid): Is this conversion to ES5-world OK?
   *
   * @param aCallback some user-supplied "function-like" callback.
   */
  domitaModules.ensureValidCallback =
      function (aCallback) {

    if ('function' !== typeof aCallback) {
      throw new Error('Expected function not ' + typeof aCallback);
    }
  };

  /**
   * This combines trademarks with amplification, and is really a thin wrapper
   * on WeakMap. It allows objects to have an arbitrary collection of private
   * properties, which can only be accessed by those holding the amplifier 'p'
   * (which, in most cases, should be only a particular prototype's methods.)
   *
   * Unlike trademarks, this does not freeze the object. It is assumed that the
   * caller makes the object sufficiently frozen for its purposes and/or that
   * the private properties are all that needs protection.
   *
   * This is designed to be more efficient and straightforward than using both
   * trademarks and per-private-property sealed boxes or weak maps.
   *
   * Capability design note: This facility provides sibling amplification (the
   * ability for one object to access the private state of other similar
   * objects).
   */
  domitaModules.Confidence = (function () {
    var setOwn = domitaModules.setOwn;

    function Confidence(typename) {
      var table = new WeakMap();

      this.confide = cajaVM.def(function (object, opt_sameAs) {
        //console.debug("Confiding:", object);
        if (table.get(object) !== undefined) {
          if (table.get(object)._obj !== object) {
            throw new Error("WeakMap broke! " + object + " vs. " +
                table.get(object)._obj);
          }
          throw new Error(typename + " has already confided in " + object);
        }

        var privates;
        var proto = Object.getPrototypeOf(object);
        if (opt_sameAs !== undefined) {
          privates = this.p(opt_sameAs);
        } else {
          privates = {_obj: object};
        }

        table.set(object, privates);
      });

      var guard = this.guard = cajaVM.makeTableGuard(table, typename,
          'This operation requires a ' + typename);

      /**
       * Wrap a method to enforce that 'this' is a confidant, and also
       * freeze it.
       *
       * This plays the role ___.grantTypedMethod did in Domita.
       */
      this.protectMethod = function (method) {
        //cajaVM.def(method);  // unnecessary in theory.  TODO(felix8a): verify
        function protectedMethod(var_args) {
          return method.apply(guard.coerce(this), arguments);
        }
        setOwn(protectedMethod, "toString", cajaVM.def(function () {
          // TODO(kpreid): this causes function body spamminess in firebug
          return "[" + typename + "]" + method.toString();
        }));
        return cajaVM.def(protectedMethod);
      };


      /**
       * Get the private properties of the object, or throw.
       */
      this.p = cajaVM.def(function (object) {
        var p = table.get(object);
        if (p === undefined) {
          guard.coerce(object);  // borrow failure
          throw new Error("can't happen");
        } else {
          return p;
        }
      });

      this.typename = typename;
    }
    setOwn(Confidence.prototype, "toString", Object.freeze(function () {
      return this.typename + 'Confidence';
    }));

    return cajaVM.def(Confidence);
  })();

  domitaModules.ExpandoProxyHandler = (function () {
    var getPropertyDescriptor = domitaModules.getPropertyDescriptor;
    var ProxyHandler = domitaModules.ProxyHandler;

    /**
     * A handler for a proxy which implements expando properties by forwarding
     * them to a separate object associated (by weak map) with the real node.
     *
     * The {@code target} is treated as if it were the prototype of this proxy,
     * and the expando properties as own properties of this proxy, except that
     * expando properties cannot hide target properties.
     *
     * The client SHOULD call ExpandoProxyHandler.register(proxy, target) to
     * enable use of ExpandoProxyHandler.unwrap.
     * TODO(kpreid): Wouldn't this mapping better be handled just by the client
     * since this is not defensively consistent and we don't need it here?
     *
     * Note the following exophoric hazard: if there are multiple expando
     * proxies with the same {@code storage}, and an accessor property is set on
     * one, and then that property is read on the other, the {@code this} seen
     * by the accessor get/set functions will be the latter proxy rather than
     * the former. Therefore, it should never be the case that two expando
     * proxies have the same storage and provide different authority, unless
     * every proxy which is editable provides a superset of the authority
     * provided by all others (which is the case for the use with TameNodes in
     * Domado).
     *
     * @param {Object} target The tame node to forward methods to.
     * @param {boolean} editable Whether modifying the properties is allowed.
     * @param {Object} storage The object to store the expando properties on.
     */
    function ExpandoProxyHandler(target, editable, storage) {
      this.editable = editable;
      this.storage = storage;
      this.target = target;
    }
    var expandoProxyTargets = new WeakMap();
    // Not doing this because it implements all the derived traps, requiring
    // us to do the same. Instead, we use its prototype selectively, whee.
    //inherit(ExpandoProxyHandler, ProxyHandler);

    ExpandoProxyHandler.register = function (proxy, handler) {
      expandoProxyTargets.set(proxy, handler);
    };

    /**
     * If obj is an expando proxy, return the underlying object. This is used
     * to apply non-expando properties to the object outside of its constructor.
     *
     * This is not robust against spoofing because it is only used during
     * initialization.
     */
    ExpandoProxyHandler.unwrap = function (obj) {
      return expandoProxyTargets.has(obj) ? expandoProxyTargets.get(obj) : obj;
    };

    ExpandoProxyHandler.prototype.getOwnPropertyDescriptor = function (name) {
      if (name === "ident___") {
        // Caja WeakMap emulation internal property
        return Object.getOwnPropertyDescriptor(this, "ident");
      } else {
        return Object.getOwnPropertyDescriptor(this.storage, name);
      }
    };
    ExpandoProxyHandler.prototype.getPropertyDescriptor = function (name) {
      var desc = this.getOwnPropertyDescriptor(name)
          || getPropertyDescriptor(this.target, name);
      return desc;
    };
    ExpandoProxyHandler.prototype.getOwnPropertyNames = function () {
      return Object.getOwnPropertyNames(
          this.storage || {});
    };
    ExpandoProxyHandler.prototype.defineProperty = function (name, descriptor) {
      if (name === "ident___") {
        if (descriptor.set === null) descriptor.set = undefined;
        Object.defineProperty(this, "ident", descriptor);
      } else if (name in this.target) {
        // Forwards everything already defined (not expando).
        return ProxyHandler.prototype.defineProperty.call(this, name,
            descriptor);
      } else {
        if (!this.editable) { throw new Error(NOT_EDITABLE); }
        Object.defineProperty(this.storage, name, descriptor);
        return true;
      }
      return false;
    };
    ExpandoProxyHandler.prototype['delete'] = function (name) {
      if (name === "ident___") {
        return false;
      } else if (name in this.target) {
        // Forwards everything already defined (not expando).
        return ProxyHandler.prototype['delete'].call(this, name);
      } else {
        if (!this.editable) { throw new Error(NOT_EDITABLE); }
        return delete this.storage[name];
      }
      return false;
    };
    ExpandoProxyHandler.prototype.fix = function () {
      // TODO(kpreid): Implement fixing, because it is possible to freeze a
      // host DOM node and so ours should support it too.
      return undefined;
    };

    // Derived traps
    ExpandoProxyHandler.prototype.get = domitaModules.permuteProxyGetSet.getter(
        function (name) {
      // Written for an ES5/3 bug, but probably useful for efficiency too.
      if (name === "ident___") {
        // Caja WeakMap emulation internal property
        return this.ident;
      } else if (Object.prototype.hasOwnProperty.call(this.storage, name)) {
        return this.storage[name];
      } else {
        return this.target[name];
      }
    });
    ExpandoProxyHandler.prototype.enumerate = function () {
      // This derived trap shouldn't be necessary, but Firebug won't complete
      // properties without it (FF 4.0.1, Firebug 1.7.1, 2011-06-02).
      var names = [];
      for (var k in this.target) names.push(k);
      for (var k in this.storage) names.push(k);
      return names;
    };
    ExpandoProxyHandler.prototype.set = domitaModules.permuteProxyGetSet.setter(
        function (name, val, receiver) {
      // NOTE: this was defined to work around what might be a FF4 or
      // FF4+initSES bug or a bug in our get[Own]PropertyDescriptor that made
      // the innerHTML setter not be invoked.
      // It is, in fact, an exact copy of the default derived trap code from
      // https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Proxy
      // as of 2011-05-25.
      var desc = this.getOwnPropertyDescriptor(name);
      if (desc) {
        if ('writable' in desc) {
          if (desc.writable) {
            desc.value = val;
            this.defineProperty(name, desc);
            return true;
          } else {
            return false;
          }
        } else { // accessor
          if (desc.set) {
            desc.set.call(receiver, val);
            return true;
          } else {
            return false;
          }
        }
      }
      desc = this.getPropertyDescriptor(name);
      if (desc) {
        if ('writable' in desc) {
          if (desc.writable) {
            // fall through
          } else {
            return false;
          }
        } else { // accessor
          if (desc.set) {
            desc.set.call(receiver, val);
            return true;
          } else {
            return false;
          }
        }
      }
      this.defineProperty(name, {
        value: val,
        writable: true,
        enumerable: true,
        configurable: true});
      return true;
    });

    return cajaVM.def(ExpandoProxyHandler);
  })();

  /** XMLHttpRequest or an equivalent on IE 6. */
  domitaModules.XMLHttpRequestCtor = function (makeDOMAccessible,
      XMLHttpRequest, ActiveXObject, XDomainRequest) {
    if (XMLHttpRequest &&
      makeDOMAccessible(new XMLHttpRequest()).withCredentials !== undefined) {
      return XMLHttpRequest;
    } else if (XDomainRequest) { 
      return function XDomainRequestObjectForIE() {
        var xdr = makeDOMAccessible(new XDomainRequest());
        xdr.onload = function () {
          if ('function' === typeof xdr.onreadystatechange) {
            xdr.status = 200;
            xdr.readyState = 4;
            xdr.onreadystatechange.call(xdr, null, false);
          }
        };
        var errorHandler = function () {
          if ('function' === typeof xdr.onreadystatechange) {
            xdr.status = 500;
            xdr.readyState = 4;
            xdr.onreadystatechange.call(xdr, null, false);
          }
        };
        xdr.onerror = errorHandler;
        xdr.ontimeout = errorHandler;
        return xdr;
      };
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
            var candidate = activeXClassIds[+i];
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
      taming,
      rulebreaker,
      xmlHttpRequestMaker,
      naiveUriPolicy,
      getBaseURL) {
    var Confidence = domitaModules.Confidence;
    var setOwn = domitaModules.setOwn;
    var canHaveEnumerableAccessors = domitaModules.canHaveEnumerableAccessors;
    // See http://www.w3.org/TR/XMLHttpRequest/

    // TODO(ihab.awad): Improve implementation (interleaving, memory leaks)
    // per http://www.ilinsky.com/articles/XMLHttpRequest/

    var TameXHRConf = new Confidence('TameXMLHttpRequest');
    var p = TameXHRConf.p.bind(TameXHRConf);
    var method = TameXHRConf.protectMethod;

    // Note: Since there is exactly one TameXMLHttpRequest per feral XHR, we do
    // not use an expando proxy and always let clients set expando properties
    // directly on this. This simplifies implementing onreadystatechange.
    function TameXMLHttpRequest() {
      TameXHRConf.confide(this);
      var xhr = p(this).feral =
          rulebreaker.makeDOMAccessible(new xmlHttpRequestMaker());
      taming.tamesTo(xhr, this);
    }
    Object.defineProperties(TameXMLHttpRequest.prototype, {
      onreadystatechange: {
        enumerable: canHaveEnumerableAccessors,
        set: method(function (handler) {
          // TODO(ihab.awad): Do we need more attributes of the event than
          // 'target'? May need to implement full "tame event" wrapper similar
          // to DOM events.
          var self = this;
          p(this).feral.onreadystatechange = function (event) {
            var evt = { target: self };
            return handler.call(void 0, evt);
          };
          // Store for later direct invocation if need be
          p(this).handler = handler;
        })
      },
      readyState: {
        enumerable: canHaveEnumerableAccessors,
        get: method(function () {
          // The ready state should be a number
          return Number(p(this).feral.readyState);
        })
      },
      responseText: {
        enumerable: canHaveEnumerableAccessors,
        get: method(function () {
          var result = p(this).feral.responseText;
          return (result === undefined || result === null) ?
            result : String(result);
        })
      },
      responseXML: {
        enumerable: canHaveEnumerableAccessors,
        get: method(function () {
          var feralXml = p(this).feral.responseXML;
          if (feralXml === null || feralXml === undefined) {
            // null = 'The response did not parse as XML.'
            return null;
          } else {
            // TODO(ihab.awad): Implement a taming layer for XML. Requires
            // generalizing the HTML node hierarchy as well so we have a unified
            // implementation.

            // This kludge is just enough to keep the jQuery tests from freezing.
            var node = {nodeName: '#document'};
            node.cloneNode = function () { return node; };
            node.toString = function () {
              return 'Caja does not support XML.';
            };
            return {documentElement: node};
          }
        })
      },
      status: {
        enumerable: canHaveEnumerableAccessors,
        get: method(function () {
          var result = p(this).feral.status;
          return (result === undefined || result === null) ?
            result : Number(result);
        })
      },
      statusText: {
        enumerable: canHaveEnumerableAccessors,
        get: method(function () {
          var result = p(this).feral.statusText;
          return (result === undefined || result === null) ?
            result : String(result);
        })
      }
    });
    TameXMLHttpRequest.prototype.open = method(function (
        method, URL, opt_async, opt_userName, opt_password) {
      method = String(method);
      URL = URI.utils.resolve(getBaseURL(), String(URL));
      // The XHR interface does not tell us the MIME type in advance, so we
      // must assume the broadest possible.
      var safeUri = uriRewrite(
          naiveUriPolicy,
          URL, html4.ueffects.SAME_DOCUMENT, html4.ltypes.DATA,
          {
            "TYPE": "XHR",
            "XHR_METHOD": method,
            "XHR": true  // Note: this hint is deprecated
          });
      // If the uriPolicy rejects the URL, we throw an exception, but we do
      // not put the URI in the exception so as not to put the caller at risk
      // of some code in its stack sniffing the URI.
      if ("string" !== typeof safeUri) { throw 'URI violates security policy'; }
      switch (arguments.length) {
      case 2:
        p(this).async = true;
        p(this).feral.open(method, safeUri);
        break;
      case 3:
        p(this).async = opt_async;
        p(this).feral.open(method, safeUri, Boolean(opt_async));
        break;
      case 4:
        p(this).async = opt_async;
        p(this).feral.open(
            method, safeUri, Boolean(opt_async), String(opt_userName));
        break;
      case 5:
        p(this).async = opt_async;
        p(this).feral.open(
            method, safeUri, Boolean(opt_async), String(opt_userName),
            String(opt_password));
        break;
      default:
        throw 'XMLHttpRequest cannot accept ' + arguments.length + ' arguments';
        break;
      }
    });
    TameXMLHttpRequest.prototype.setRequestHeader = method(
        function (label, value) {
      p(this).feral.setRequestHeader(String(label), String(value));
    });
    TameXMLHttpRequest.prototype.send = method(function(opt_data) {
      if (arguments.length === 0) {
        // TODO(ihab.awad): send()-ing an empty string because send() with no
        // args does not work on FF3, others?
        p(this).feral.send('');
      } else if (typeof opt_data === 'string') {
        p(this).feral.send(opt_data);
      } else /* if XML document */ {
        // TODO(ihab.awad): Expect tamed XML document; unwrap and send
        p(this).feral.send('');
      }

      // Firefox does not call the 'onreadystatechange' handler in
      // the case of a synchronous XHR. We simulate this behavior by
      // calling the handler explicitly.
      if (p(this).feral.overrideMimeType) {
        // This is Firefox
        if (!p(this).async && p(this).handler) {
          var evt = { target: this };
          p(this).handler.call(void 0, evt);
        }
      }
    });
    TameXMLHttpRequest.prototype.abort = method(function () {
      p(this).feral.abort();
    });
    TameXMLHttpRequest.prototype.getAllResponseHeaders = method(function () {
      var result = p(this).feral.getAllResponseHeaders();
      return (result === undefined || result === null) ?
        result : String(result);
    });
    TameXMLHttpRequest.prototype.getResponseHeader = method(
        function (headerName) {
      var result = p(this).feral.getResponseHeader(String(headerName));
      return (result === undefined || result === null) ?
        result : String(result);
    });
    /*SES*/setOwn(TameXMLHttpRequest.prototype, "toString", method(function () {
      return 'Not a real XMLHttpRequest';
    }));

    return cajaVM.def(TameXMLHttpRequest);
  };

  // TODO(kpreid): Review whether this has unnecessary features (as we're
  // statically generating Style accessors rather than proxy/handler-ing
  // Domita did).
  domitaModules.CssPropertiesCollection = function(aStyleObject) {
    var canonicalStylePropertyNames = {};
    // Maps style property names, e.g. cssFloat, to property names, e.g. float.
    var cssPropertyNames = {};

    for (var cssPropertyName in cssSchema) {
      var baseStylePropertyName = cssPropertyName.replace(
          /-([a-z])/g, function (_, letter) { return letter.toUpperCase(); });
      var canonStylePropertyName = baseStylePropertyName;
      cssPropertyNames[baseStylePropertyName]
          = cssPropertyNames[canonStylePropertyName]
          = cssPropertyName;
      var alts = cssSchema[cssPropertyName].cssAlternates;
      if (alts) {
        for (var i = alts.length; --i >= 0;) {
          cssPropertyNames[alts[+i]] = cssPropertyName;
          // Handle oddities like cssFloat/styleFloat.
          if (alts[+i] in aStyleObject
              && !(canonStylePropertyName in aStyleObject)) {
            canonStylePropertyName = alts[+i];
          }
        }
      }
      canonicalStylePropertyNames[cssPropertyName] = canonStylePropertyName;
    }

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
      },
      forEachCanonical: function (f) {
        for (var p in cssPropertyNames) {
          if (cssPropertyNames.hasOwnProperty(p)) {
            f(p);
          }
        }
      }
    };
  };

  cajaVM.def(domitaModules);

  var NOT_EDITABLE = "Node not editable.";
  var UNSAFE_TAGNAME = "Unsafe tag name.";
  var UNKNOWN_TAGNAME = "Unknown tag name.";
  var INDEX_SIZE_ERROR = "Index size error.";

  /**
   * Authorize the Domado library.
   *
   * The result of this constructor is almost stateless. The exceptions are
   * that each instance has unique trademarks for certain types of tamed
   * objects, and a shared map allowing separate virtual documents to dispatch
   * events across them. (TODO(kpreid): Confirm this explanation is good.)
   *
   * @param {Object} opt_rulebreaker. If necessary, authorities to break the
   *     ES5/3 taming membrane and work with the taming-frame system. If
   *     running under SES, pass null instead.
   * @return A record of functions attachDocument, dispatchEvent, and
   *     dispatchToHandler.
   */
  return cajaVM.def(function Domado(opt_rulebreaker) {
    // Everything in this scope but not in function attachDocument() below
    // does not contain lexical references to a particular DOM instance, but
    // may have some kind of privileged access to Domado internals.

    // This is only used if opt_rulebreaker is absent (because the plugin ids
    // are normally managed by es53 when it is not). TODO(kpreid): Is there a
    // more sensible place to put this management, which would be used in both
    // modes?
    var importsToId = new WeakMap(true);
    var idToImports = [];
    var nextPluginId = 0;

    // This parameter is supplied in the ES5/3 case and not in the ES5+SES case.
    var rulebreaker = opt_rulebreaker ? opt_rulebreaker : cajaVM.def({
      // These are the stub versions used when in ES5+SES.
      makeDOMAccessible: function (o) {return o;},
      makeFunctionAccessible: function (o) {return o;},
      writeToPixelArray: function (source, target, length) {
        // See the use below for why this exists.
        for (var i = length-1; i >= 0; i--) {
          target[+i] = source[+i];
        }
      },

      getId: function (imports) {
        if (importsToId.has(imports)) {
          return importsToId.get(imports);
        } else {
          var id = nextPluginId++;
          importsToId.set(imports, id);
          idToImports[id] = imports;
          return id;
        }
      },
      getImports: function (id) {
        var imports = idToImports[id];
        if (imports === undefined) {
          throw new Error('Internal: imports#', id, ' unregistered');
        }
        return imports;
      }
    });

    var makeDOMAccessible = rulebreaker.makeDOMAccessible;
    var makeFunctionAccessible = rulebreaker.makeFunctionAccessible;

    var Confidence = domitaModules.Confidence;
    var ProxyHandler = domitaModules.ProxyHandler;
    var ExpandoProxyHandler = domitaModules.ExpandoProxyHandler;
    var setOwn = domitaModules.setOwn;
    var canHaveEnumerableAccessors = domitaModules.canHaveEnumerableAccessors;

    function inherit(sub, souper) {
      sub.prototype = Object.create(souper.prototype);
      Object.defineProperty(sub.prototype, "constructor", {
        value: sub,
        writable: true,
        configurable: true
      });
    }

    /**
     * For each enumerable p: d in propDescs, do the equivalent of
     *
     *   Object.defineProperty(object, p, d)
     *
     * except that the property descriptor d can have additional keys:
     *
     *    extendedAccessors:
     *      If present and true, property accessor functions receive the
     *      property name as an additional argument.
     */
    function definePropertiesAwesomely(object, propDescs) {
      for (var prop in propDescs) {
        var desc = {};
        for (var k in propDescs[prop]) {
          desc[k] = propDescs[prop][k];
        }
        if ('get' in desc || 'set' in desc) {
          // Firefox bug workaround; see comments on canHaveEnumerableAccessors.
          desc.enumerable = desc.enumerable && canHaveEnumerableAccessors;
        }
        if (desc.extendedAccessors) {
          delete desc.extendedAccessors;
          (function (prop, extGet, extSet) {  // @*$#*;%#<$ non-lexical scoping
            if (extGet) {
              desc.get = cajaVM.def(function () {
                return extGet.call(this, prop);
              });
            }
            if (desc.set) {
              desc.set = cajaVM.def(function (value) {
                return extSet.call(this, value, prop);
              });
            }
          })(prop, desc.get, desc.set);
        }
        if (desc.get && !Object.isFrozen(desc.get)) {
          if (typeof console !== 'undefined') {
            console.warn("Getter for ", prop, " of ", object,
                " is not frozen; fixing.");
          }
          cajaVM.def(desc.get);
        }
        if (desc.set && !Object.isFrozen(desc.set)) {
          if (typeof console !== 'undefined') {
            console.warn("Setter for ", prop, " of ", object,
                " is not frozen; fixing.");
          }
          cajaVM.def(desc.set);
        }
        Object.defineProperty(object, prop, desc);
      }
    }

    function forOwnKeys(obj, fn) {
      for (var i in obj) {
        if (!Object.prototype.hasOwnProperty.call(obj, i)) { continue; }
        fn(i, obj[i]);
      }
    }

    // value transforming functions
    function identity(x) { return x; }
    function defaultToEmptyStr(x) { return x || ''; }

    // Array Remove - By John Resig (MIT Licensed)
    function arrayRemove(array, from, to) {
      var rest = array.slice((to || from) + 1 || array.length);
      array.length = from < 0 ? array.length + from : from;
      return array.push.apply(array, rest);
    }

    // It is tempting to name this table "burglar".
    var windowToDomicile = new WeakMap();

    var TameEventConf = new Confidence('TameEvent');
    var TameEventT = TameEventConf.guard;
    var TameImageDataConf = new Confidence('TameImageData');
    var TameImageDataT = TameImageDataConf.guard;
    var TameGradientConf = new Confidence('TameGradient');
    var TameGradientT = TameGradientConf.guard;

    var eventMethod = TameEventConf.protectMethod;

    // Define a wrapper type for known safe HTML, and a trademarker.
    // This does not actually use the trademarking functions since trademarks
    // cannot be applied to strings.
    var safeHTMLTable = new WeakMap(true);
    function Html(htmlFragment) {
      // Intentionally using == rather than ===.
      var h = String(htmlFragment == null ? '' : htmlFragment);
      safeHTMLTable.put(this, htmlFragment);
      return cajaVM.def(this);
    }
    function htmlToString() {
      return safeHTMLTable.get(this);
    }
    setOwn(Html.prototype, 'valueOf', htmlToString);
    setOwn(Html.prototype, 'toString', htmlToString);
    function safeHtml(htmlFragment) {
      // Intentionally using == rather than ===.
      return (htmlFragment instanceof Html)
          ? safeHTMLTable.get(htmlFragment)
          : html.escapeAttrib(String(htmlFragment == null ? '' : htmlFragment));
    }
    function blessHtml(htmlFragment) {
      return (htmlFragment instanceof Html)
          ? htmlFragment
          : new Html(htmlFragment);
    }
    cajaVM.def([Html, safeHtml, blessHtml]);

    var XML_SPACE = '\t\n\r ';

    var JS_SPACE = '\t\n\r ';
    // An identifier that does not end with __.
    var JS_IDENT = '(?:[a-zA-Z_][a-zA-Z0-9$_]*[a-zA-Z0-9$]|[a-zA-Z])_?';
    var SIMPLE_HANDLER_PATTERN = new RegExp(
        '^[' + JS_SPACE + ']*'
        + '(return[' + JS_SPACE + ']+)?'  // Group 1 is present if it returns.
        + '(' + JS_IDENT + ')[' + JS_SPACE + ']*'  // Group 2 is a func name.
        // Which can be passed optionally the event, and optionally this node.
        + '\\((?:event'
          + '(?:[' + JS_SPACE + ']*,[' + JS_SPACE + ']*this)?'
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

    // Trim whitespace from the beginning and end of a string, using this
    // definition of whitespace:
    // per http://www.whatwg.org/specs/web-apps/current-work/multipage/common-microsyntaxes.html#space-character
    function trimHTML5Spaces(input) {
      return input.replace(/^[ \t\r\n\f]+|[ \t\r\n\f]+$/g, '');
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

    var cssSealerUnsealerPair = cajaVM.makeSealerUnsealerPair();

    /*
     * Implementations of setTimeout, setInterval, clearTimeout, and
     * clearInterval that only allow simple functions as timeouts and
     * that treat timeout ids as capabilities.
     * This is safe even if accessed across frame since the same
     * map is never used with more than one version of setTimeout.
     */
    function tameSetAndClear(target, set, clear, setName, clearName) {
      var ids = new WeakMap();
      makeFunctionAccessible(set);
      makeFunctionAccessible(clear);
      function tameSet(action, delayMillis) {
        // Existing browsers treat a timeout/interval of null or undefined as a
        // noop.
        var id;
        if (action) {
          if (typeof action !== 'function') {
            // Early error for usability -- we also defend below.
            // This check is not *necessary* for security.
            throw new Error(
                setName + ' called with a ' + typeof action + '.'
                + '  Please pass a function instead of a string of JavaScript');
          }
          // actionWrapper defends against:
          //   * Passing a string-like object which gets taken as code.
          //   * Non-standard arguments to the callback.
          //   * Non-standard effects of callback's return value.
          var actionWrapper = function() {
            action();
          };
          id = set(actionWrapper, delayMillis | 0);
        } else {
          id = undefined;
        }
        var tamed = {};
        ids.set(tamed, id);
        return tamed;
      }
      function tameClear(id) {
        // From https://developer.mozilla.org/en/DOM/window.clearTimeout says:
        //   Notes:
        //   Passing an invalid ID to clearTimeout does not have any effect
        //   (and doesn't throw an exception).

        // WeakMap will throw on these, so early exit.
        if (typeof id !== 'object' || id == null) { return; }

        var feral = ids.get(id);
        if (feral !== undefined) clear(feral);  // noop if not found
      }
      target[setName] = cajaVM.def(tameSet);
      target[clearName] = cajaVM.def(tameClear);
      return target;
    }

    function makeScrollable(bridal, element) {
      var overflow = bridal.getComputedStyle(element, void 0).overflow;
      switch (overflow && overflow.toLowerCase()) {
        case 'visible':
        case 'hidden':
          makeDOMAccessible(element.style);
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
      makeDOMAccessible(element.style);
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

      var style = makeDOMAccessible(element.currentStyle);
      if (!style) {
        style = makeDOMAccessible(
            bridalMaker.getWindow(element, makeDOMAccessible)
            .getComputedStyle(element, void 0));
      }

      makeDOMAccessible(element.style);

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

    /**
     * Access policies
     *
     * Each of these objects is a policy for what type of access (read/write,
     * read-only, or none) is permitted to a Node or NodeList. Each policy
     * object determines the access for the associated node and its children.
     * The childPolicy may be overridden if the node is an opaque or foreign
     * node.
     *
     * Definitions:
     *    childrenVisible:
     *      This node appears to have the children it actually does; otherwise,
     *      appears to have no children.
     *    attributesVisible:
     *      This node appears to have the attributes it actually does;
     *      otherwise, appears to have no attributes.
     *    editable:
     *      This node's attributes and properties (other than children) may be
     *      modified.
     *    childrenEditable:
     *      This node's childNodes list may be modified, and its children are
     *      both editable and childrenEditable.
     *
     * These flags can express several meaningless cases; in particular, the
     * 'editable but not visible' cases do not occur.
     */
    var protoNodePolicy = {
      requireEditable: function () {
        if (!this.editable) {
          throw new Error(NOT_EDITABLE);
        }
      },
      requireChildrenEditable: function () {
        if (!this.childrenEditable) {
          throw new Error(NOT_EDITABLE);
        }
      },
      requireUnrestricted: function () {
        if (!this.unrestricted) {
          throw new Error("Node is restricted");
        }
      },
      assertRestrictedBy: function (policy) {
        if (!this.childrenVisible   && policy.childrenVisible ||
            !this.attributesVisible && policy.attributesVisible ||
            !this.editable          && policy.editable ||
            !this.childrenEditable  && policy.childrenEditable ||
            !this.upwardNavigation  && policy.upwardNavigation ||
            !this.unrestricted      && policy.unrestricted) {
          throw new Error("Domado internal error: non-monotonic node policy");
        }
      }
    };
    // We eagerly await ES6 offering some kind of literal-with-prototype...
    var nodePolicyEditable = Object.create(protoNodePolicy);
    nodePolicyEditable.toString = function () { return "nodePolicyEditable"; };
    nodePolicyEditable.childrenVisible = true;
    nodePolicyEditable.attributesVisible = true;
    nodePolicyEditable.editable = true;
    nodePolicyEditable.childrenEditable = true;
    nodePolicyEditable.upwardNavigation = true;
    nodePolicyEditable.unrestricted = true;
    nodePolicyEditable.childPolicy = nodePolicyEditable;

    var nodePolicyReadOnly = Object.create(protoNodePolicy);
    nodePolicyReadOnly.toString = function () { return "nodePolicyReadOnly"; };
    nodePolicyReadOnly.childrenVisible = true;
    nodePolicyReadOnly.attributesVisible = true;
    nodePolicyReadOnly.editable = false;
    nodePolicyReadOnly.childrenEditable = false;
    nodePolicyReadOnly.upwardNavigation = true;
    nodePolicyReadOnly.unrestricted = true;
    nodePolicyReadOnly.childPolicy = nodePolicyReadOnly;

    var nodePolicyReadOnlyChildren = Object.create(protoNodePolicy);
    nodePolicyReadOnlyChildren.toString =
        function () { return "nodePolicyReadOnlyChildren"; };
    nodePolicyReadOnlyChildren.childrenVisible = true;
    nodePolicyReadOnlyChildren.attributesVisible = true;
    nodePolicyReadOnlyChildren.editable = true;
    nodePolicyReadOnlyChildren.childrenEditable = false;
    nodePolicyReadOnlyChildren.upwardNavigation = true;
    nodePolicyReadOnlyChildren.unrestricted = true;
    nodePolicyReadOnlyChildren.childPolicy = nodePolicyReadOnly;

    var nodePolicyOpaque = Object.create(protoNodePolicy);
    nodePolicyOpaque.toString = function () { return "nodePolicyOpaque"; };
    nodePolicyOpaque.childrenVisible = true;
    nodePolicyOpaque.attributesVisible = false;
    nodePolicyOpaque.editable = false;
    nodePolicyOpaque.childrenEditable = false;
    nodePolicyOpaque.upwardNavigation = true;
    nodePolicyOpaque.unrestricted = false;
    nodePolicyOpaque.childPolicy = nodePolicyReadOnly;

    var nodePolicyForeign = Object.create(protoNodePolicy);
    nodePolicyForeign.toString = function () { return "nodePolicyForeign"; };
    nodePolicyForeign.childrenVisible = false;
    nodePolicyForeign.attributesVisible = false;
    nodePolicyForeign.editable = false;
    nodePolicyForeign.childrenEditable = false;
    nodePolicyForeign.upwardNavigation = false;
    nodePolicyForeign.unrestricted = false;
    Object.defineProperty(nodePolicyForeign, "childPolicy", {
      get: function () {
        throw new Error("Foreign node childPolicy should never be consulted");
      }
    });
    cajaVM.def([
      nodePolicyEditable,
      nodePolicyReadOnly,
      nodePolicyReadOnlyChildren,
      nodePolicyOpaque,
      nodePolicyForeign
    ]);

    // Used for debugging policy decisions; see calls in TameBackedNode.
    //function TracedNodePolicy(policy, note, source) {
    //  var wrap = Object.create(policy);
    //  wrap.trace = [note, source].concat(source && source.trace || []);
    //  setOwn(wrap, 'toString', function() {
    //    return policy.toString() + "<<" + wrap.trace + ">>";
    //  });
    //  return wrap;
    //}

    /**
     * Add a tamed document implementation to a Gadget's global scope.
     *
     * Has the side effect of adding the classes "vdoc-container___" and
     * idSuffix.substring(1) to the containerNode.
     *
     * @param {string} idSuffix a string suffix appended to all node IDs.
     *     It should begin with "-" and end with "___".
     * @param {Object} uriPolicy an object like <pre>{
     *   rewrite: function (uri, uriEffect, loaderType, hints) {
     *      return safeUri
     *   }
     * }</pre>.
     *       * uri: the uri to be rewritten
     *       * uriEffect: the effect that allowing a URI to load has (@see
     *         UriEffect.java).
     *       * loaderType: type of loader that would load the URI or the
     *         rewritten
     *         version.
     *       * hints: record that describes the context in which the URI
     *         appears.
     *         If a hint is not present it should not be relied upon.
     *     The rewrite function should be idempotent to allow rewritten HTML
     *     to be reinjected.
     * @param {Node} containerNode an HTML node to contain the children of the
     *     virtual Document node provided to Cajoled code.
     * @param {Object} optTargetAttributePresets a record containing the presets
     *     (default and whitelist) for the HTML "target" attribute.
     * @param {Object} taming. An interface to a taming membrane.
     * @return {Object} A collection of privileged access tools, plus the tamed
     *     {@code document} and {@code window} objects under those names. This
     *     object is known as a "domicile".
     */
    function attachDocument(
      idSuffix, naiveUriPolicy, containerNode, optTargetAttributePresets,
        taming) {

      if (arguments.length < 3) {
        throw new Error(
            'attachDocument arity mismatch: ' + arguments.length);
      }
      if (!optTargetAttributePresets) {
        optTargetAttributePresets = {
          'default': '_blank',
          whitelist: [ '_blank', '_self' ]
        };
      }

      var domicile = {
        // True when we're executing a handler for events like click
        handlingUserAction: false
      };
      var pluginId;

      var vdocContainsForeignNodes = false;

      containerNode = makeDOMAccessible(containerNode);
      var document = containerNode.nodeType === 9  // Document node
          ? containerNode
          : containerNode.ownerDocument;
      document = makeDOMAccessible(document);
      var docEl = makeDOMAccessible(document.documentElement);
      var bridal = bridalMaker(makeDOMAccessible, document);

      var window = bridalMaker.getWindow(containerNode, makeDOMAccessible);
      window = makeDOMAccessible(window);

      var elementPolicies = {};
      elementPolicies.form = function (attribs) {
        // Forms must have a gated onsubmit handler or they must have an
        // external target.
        var sawHandler = false;
        for (var i = 0, n = attribs.length; i < n; i += 2) {
          if (attribs[+i] === 'onsubmit') {
            sawHandler = true;
          }
        }
        if (!sawHandler) {
          attribs.push('onsubmit', 'return false');
        }
        return forceAutocompleteOff(attribs);
      };
      elementPolicies.input = function (attribs) {
        return forceAutocompleteOff(attribs);
      };

      function forceAutocompleteOff(attribs) {
        var a = [];
        for (var i = 0, n = attribs.length; i < n; i += 2) {
          if (attribs[+i] !== 'autocomplete') {
            a.push(attribs[+i], attribs[+i+1]);
          }
        }
        a.push('autocomplete', 'off');
        return a;
      }

      // TODO(kpreid): should elementPolicies be exported in domicile?

      // On IE, turn <canvas> tags into canvas elements that explorercanvas
      // will recognize
      bridal.initCanvasElements(containerNode);

      // The private properties used in TameNodeConf are:
      //    feral (feral node)
      //    policy (access policy)
      //    Several specifically for TameHTMLDocument.
      // Furthermore, by virtual of being scoped inside attachDocument,
      // TameNodeT also indicates that the object is a node from the *same*
      // virtual document.
      // TODO(kpreid): Review how necessary it is to scope this inside
      // attachDocument. The issues are:
      //   * Using authority or types from a different virtual document (check
      //     the things that used to be TameHTMLDocument.doc___ in particular)
      //   * Using nodes from a different real document (Domita would seem to
      //     be vulnerable to this?)
      var TameNodeConf = new Confidence('TameNode');
      var TameNodeT = TameNodeConf.guard;
      var np = TameNodeConf.p.bind(TameNodeConf);

      // A map from tame nodes to their expando proxies, used when only the tame
      // node is available and the proxy is needed to return to the client.
      var tamingProxies = new WeakMap();

      /**
       * Call this on every TameNode after it is constructed, and use its return
       * value instead of the node.
       *
       * TODO(kpreid): Is this the best way to handle things which need to be
       * done after the outermost constructor?
       */
      function finishNode(node) {
        var feral = np(node).feral;

        if (domitaModules.proxiesAvailable) {
          // If running with proxies, it is indicative of something going wrong
          // if our objects are mutated (the expando proxy handler should
          // intercept writes). If running without proxies, then we need to be
          // unfrozen so that assignments to expando fields work.
          Object.freeze(node);

          // The proxy construction is deferred until now because the ES5/3
          // implementation of proxies requires that the proxy's prototype is
          // frozen.
          var proxiedNode = Proxy.create(np(node).proxyHandler, node);
          delete np(node).proxyHandler;  // no longer needed

          ExpandoProxyHandler.register(proxiedNode, node);
          TameNodeConf.confide(proxiedNode, node);
          tamingProxies.set(node, proxiedNode);

          node = proxiedNode;
        }

        if (feral) {
          if (feral.nodeType === 1) {
            // Elements must only be tamed once; to do otherwise would be
            // a bug in Domado.
            taming.tamesTo(feral, node);
          } else {
            // Other node types are tamed every time they are encountered;
            // we simply remember the latest taming here.
            taming.reTamesTo(feral, node);
          }
        } else {
          // If guest code passes a node of its own with no feral counterpart to
          // host code, we pass the empty object "{}". This is a safe behavior
          // until experience determines we need something more complex.
          taming.tamesTo({}, node);
        }

        return node;
      }

      var nodeMethod = TameNodeConf.protectMethod;

      /**
       * Sanitizes the value of a CSS property, the {@code red} in
       * {@code color:red}.
       * @param cssPropertyName a canonical CSS property name
       *    {@code "font-family"} not {@code "fontFamily"}.
       */
      function sanitizeStyleProperty(cssPropertyName, tokens) {
        var schema = cssSchema[cssPropertyName];
        if (!schema) {
          tokens.length = 0;
          return false;
        }
        sanitizeCssProperty(
            cssPropertyName,
            schema, tokens,
            naiveUriPolicy
            ? function (url) {
                return uriRewrite(
                    naiveUriPolicy,
                    url, html4.ueffects.SAME_DOCUMENT,
                    html4.ltypes.SANDBOXED,
                    {
                      "TYPE": "CSS",
                      "CSS_PROP": cssPropertyName
                    });
              }
            : null,
            domicile.pseudoLocation.href);
        return tokens.length !== 0;
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
        parseCssDeclarations(
            String(styleAttrValue),
            {
              declaration: function (property, value) {
                property = property.toLowerCase();
                sanitizeStyleProperty(property, value);
                sanitizedDeclarations.push(property + ': ' + value.join(' '));
              }
            });
        return sanitizedDeclarations.join(' ; ');
      }

      /** Sanitize HTML applying the appropriate transformations. */
      function sanitizeHtml(htmlText) {
        var out = [];
        htmlSanitizer(htmlText, out);
        return out.join('');
      }
      function sanitizeAttrs(tagName, attribs) {
        var n = attribs.length;
        var needsTargetAttrib =
            html4.ATTRIBS.hasOwnProperty(tagName + '::target');
        for (var i = 0; i < n; i += 2) {
          var attribName = attribs[+i];
          if ('target' === attribName) { needsTargetAttrib = false; }
          var value = attribs[+i + 1];
          var atype = null, attribKey;
          if ((attribKey = tagName + '::' + attribName,
               html4.ATTRIBS.hasOwnProperty(attribKey)) ||
              (attribKey = '*::' + attribName,
               html4.ATTRIBS.hasOwnProperty(attribKey))) {
            atype = html4.ATTRIBS[attribKey];
            value = rewriteAttribute(tagName, attribName, atype, value);
            if (atype === html4.atype.URI &&
              !!value && value.charAt(0) === '#') {
              needsTargetAttrib = false;
            }
          } else if (!/__$/.test(attribKey)) {
            attribName = attribs[+i] = cajaPrefix + attribs[+i];
          } else {
            value = null;
          }
          if (value !== null && value !== void 0) {
            attribs[+i + 1] = value;
          } else {
            // Swap last attribute name/value pair in place, and reprocess here.
            // This could affect how user-agents deal with duplicate attributes.
            attribs[+i + 1] = attribs[--n];
            attribs[+i] = attribs[--n];
            i -= 2;
          }
        }
        attribs.length = n;
        if (needsTargetAttrib) {
          attribs.push('target', optTargetAttributePresets['default']);
        }
        var policy = elementPolicies[tagName];
        if (policy && elementPolicies.hasOwnProperty(tagName)) {
          return policy(attribs);
        }
        return attribs;
      }
      function tagPolicy(tagName, attrs) {
        var schemaElem = htmlSchema.element(tagName);
        if (!schemaElem.allowed) {
          if (schemaElem.shouldVirtualize) {
            return {
              tagName: htmlSchema.virtualToRealElementName(tagName),
              attribs: sanitizeAttrs(tagName, attrs)
            };
          } else {
            return null;
          }
        } else {
          return {
            attribs: sanitizeAttrs(tagName, attrs)
          };
        }
      }
      var htmlSanitizer = html.makeHtmlSanitizer(tagPolicy);

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
          case html4.atype.URI:
            if (realValue && '#' === realValue.charAt(0)) {
              return unsuffix(realValue, idSuffix, realValue);
            } else {
              return realValue;
            }
          case html4.atype.URI_FRAGMENT:
            if (realValue && '#' === realValue.charAt(0)) {
              return unsuffix(realValue, idSuffix, null);
            } else {
              return null;
            }
          default:
            return realValue;
        }
      }

      function getSafeTargetAttribute(tagName, attribName, value) {
        if (value !== null) {
          value = String(value);
          for (var i = 0; i < optTargetAttributePresets.whitelist.length; ++i) {
            if (optTargetAttributePresets.whitelist[i] === value) {
              return value;
            }
          }
        }
        return optTargetAttributePresets['default'];
      }

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
            // TODO(felix8a): annoying that this has to be in two places
            if (attribName === 'autocomplete'
                && (tagName === 'input' || tagName === 'form')) {
              return 'off';
            }
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
            var fnNameExpr, doesReturn;
            var match = value.match(SIMPLE_HANDLER_PATTERN);
            if (match) {
              // Translate a handler that calls a simple function like
              //   return foo(this, event)
              doesReturn = !!match[1];
              fnNameExpr = '"' + match[2] + '"';
                  // safe because match[2] must be an identifier
            } else if (cajaVM.compileExpr) {
              // Compile arbitrary handler code (only in SES mode)
              doesReturn = true;
              var handlerFn = cajaVM.compileExpr(
                '(function(event) { ' + value + ' })'
              )(tameWindow);
              fnNameExpr = domicile.handlers.push(handlerFn) - 1;
            } else {
              if (typeof console !== 'undefined') {
                console.log('Rejecting complex event handler ' + tagName + ' ' +
                    attribName + '="' + value + '"');
              }
              return null;
            }
            var trustedHandler = (doesReturn ? 'return ' : '')
                + '___.plugin_dispatchEvent___('
                + 'this, event, ' + pluginId + ', '
                + fnNameExpr + ');';
            if (attribName === 'onsubmit') {
              trustedHandler =
                'try { ' + trustedHandler + ' } finally { return false; }';
            }
            return trustedHandler;
          case html4.atype.URI:
            value = String(value);
            // URI fragments reference contents within the document and
            // aren't subject to the URI policy
            if (value.charAt(0) === '#' && isValidId(value.substring(1))) {
              return value + idSuffix;
            }
            value = URI.utils.resolve(domicile.pseudoLocation.href, value);
            if (!naiveUriPolicy) { return null; }
            var schemaAttr = htmlSchema.attribute(tagName, attribName);
            return uriRewrite(
                naiveUriPolicy,
                value,
                schemaAttr.uriEffect,
                schemaAttr.loaderType,
                {
                  "TYPE": "MARKUP",
                  "XML_ATTR": attribName,
                  "XML_TAG": tagName
                }) || null;
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
              var propName = cssPropertiesAndValues[+i];
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
            return getSafeTargetAttribute(tagName, attribName, value);
          default:
            return null;
        }
      }
      
      // Implementation of HTML5 "HTML fragment serialization algorithm"
      // per http://www.whatwg.org/specs/web-apps/current-work/multipage/the-end.html#html-fragment-serialization-algorithm
      // as of 2012-09-11.
      //
      // Per HTML5: "Warning! It is possible that the output of this algorithm,
      // if parsed with an HTML parser, will not return the original tree
      // structure." Therefore, an innerHTML round-trip on a safe (from Caja's
      // perspective) but malicious DOM may be able to attack guest code.
      // TODO(kpreid): Evaluate desirability of prohibiting the worst cases of
      // this in our DOM mutators.
      function htmlFragmentSerialization(tameRoot) {
        tameRoot = TameNodeT.coerce(tameRoot);
        var sl = [];

        // Note: This algorithm is implemented in terms of tame nodes, not
        // feral nodes; therefore, it requires no access checks as it yields
        // only information which clients can obtain by object access.
        function recur(tameParent) {
          var nodes = tameParent.childNodes;
          var nNodes = nodes.length;
          for (var i = 0; i < nNodes; i++) {
            var tameCurrent = nodes.item(i);
            switch (tameCurrent.nodeType) {
              case 1:  // Element
                // TODO(kpreid): namespace issues
                var tagName = tameCurrent.tagName;
                if (tagName === undefined) {
                  // foreign node case
                  continue;
                }
                tagName = tagName.toLowerCase();
                    // TODO(kpreid): not conformant
                sl.push('<', tagName);
                var attrs = tameCurrent.attributes;
                var nAttrs = attrs.length;
                for (var j = 0; j < nAttrs; j++) {
                  var attr = attrs.item(j);
                  var aName = attr.name;
                  if (aName === 'target') {
                    // hide Caja-added link target attributes
                    // TODO(kpreid): Shouldn't these be hidden in the attributes
                    // list? This special case (and the one below) is emulating
                    // tested-for behavior in a previous .innerHTML
                    // implementation, not written from first principles.
                    continue;
                  }
                  var aValue = attr.value;
                  if (aValue === null) {
                    // rejected by virtualizeAttributeValue
                    // TODO(kpreid): Shouldn't these be hidden in the attributes
                    // list?
                    continue;
                  }
                  // TODO(kpreid): check escapeAttrib conformance
                  sl.push(' ', attr.name, '="', html.escapeAttrib(aValue), '"');
                }
                sl.push('>');
                switch (tagName) {
                  case 'area':
                  case 'base':
                  case 'basefont':
                  case 'bgsound':
                  case 'br':
                  case 'col':
                  case 'command':
                  case 'embed':
                  case 'frame':
                  case 'hr':
                  case 'img':
                  case 'input':
                  case 'keygen':
                  case 'link':
                  case 'meta':
                  case 'param':
                  case 'source':
                  case 'track':
                  case 'wbr':
                    // do nothing
                    break;
                  case 'pre':
                  case 'textarea':
                  case 'listing':
                    if (tameCurrent.firstChild &&
                        tameCurrent.firstChild.nodeType === 3 &&
                        tameCurrent.firstChild.data[0] === '\n') {
                      sl.push('\n');
                    }
                    // fallthrough
                  default:
                    recur(tameCurrent);
                    sl.push('</', tagName, '>');
                }
                break;
              case 3:  // Text
                switch (tameCurrent.parentNode.tagName.toLowerCase()) {
                    // TODO(kpreid): namespace
                  case 'style':
                  case 'script':
                  case 'xmp':
                  case 'iframe':
                  case 'noembed':
                  case 'noframes':
                  case 'plaintext':
                  case 'noscript':
                    sl.push(tameCurrent.data);
                    break;
                  default:
                    sl.push(html.escapeAttrib(tameCurrent.data));
                    break;
                }
                break;
              case 8:  // Comment
                sl.push('<', '!--', tameCurrent.data, '-->');
                break;
              case 7:  // ProcessingInstruction
                sl.push('<?', tameCurrent.target, ' ', tameCurrent.data, '>');
                break;
              case 10:  // DocumentType
                sl.push('<', '!DOCTYPE ', tameCurrent.name, '>');
                break;
              default:
                if (typeof console !== 'undefined') {
                  console.error('Domado internal: HTML fragment serialization '
                      + 'algorithm met unexpected node type '
                      + tameCurrent.nodeType);
                }
                break;
            }
          }
        }
        recur(tameRoot);

        return sl.join('');
      }

      // Property descriptors which are independent of any feral object.
      /**
       * Property descriptor which throws on access.
       */
      var P_UNIMPLEMENTED = {
        enumerable: true,
        get: cajaVM.def(function () {
          throw new Error('Not implemented');
        })
      };
      /**
       * Property descriptor for an unsettable constant attribute (like DOM
       * attributes such as nodeType).
       */
      function P_constant(value) {
        return { enumerable: true, value: value };
      }

      /**
       * Construct property descriptors suitable for taming objects which use
       * the specified confidence, such that confidence.p(obj).feral is the
       * feral object to forward to and confidence.p(obj).policy is a node
       * policy object for writability decisions.
       *
       * Lowercase properties are property descriptors; uppercase ones are
       * constructors for parameterized property descriptors.
       */
      function PropertyTaming(confidence) {
        var p = confidence.p;
        var method = confidence.protectMethod;

        return cajaVM.def({
          /**
           * Property descriptor for properties which have the value the feral
           * object does and are not assignable.
           */
          ro: {
            enumerable: true,
            extendedAccessors: true,
            get: method(function (prop) {
              return p(this).feral[prop];
            })
          },

          /**
           * Property descriptor for properties which have the value the feral
           * object does, and are assignable if the wrapper is editable.
           */
          rw: {
            enumerable: true,
            extendedAccessors: true,
            get: method(function (prop) {
              return p(this).feral[prop];
            }),
            set: method(function (value, prop) {
              var privates = p(this);
              privates.policy.requireEditable();
              privates.feral[prop] = value;
            })
          },

          /**
           * Property descriptor for properties which have the value the feral
           * object does, and are assignable (with a predicate restricting the
           * values which may be assigned) if the wrapper is editable.
           * TODO(kpreid): Use guards instead of predicates.
           */
          RWCond: function (predicate) {
            return {
              enumerable: true,
              extendedAccessors: true,
              get: method(function (prop) {
                return p(this).feral[prop];
              }),
              set: method(function (value, prop) {
                var privates = p(this);
                privates.policy.requireEditable();
                if (predicate(value)) {
                  privates.feral[prop] = value;
                }
              })
            };
          },

          /**
           * Property descriptor for properties which have a different name
           * than what they map to (e.g. labelElement.htmlFor vs.
           * <label for=...>).
           * This function changes the name the extended accessor property
           * descriptor 'desc' sees.
           */
          Rename: function (mapName, desc) {
            return {
              enumerable: true,
              extendedAccessors: true,
              get: method(function (prop) {
                return desc.get.call(this, mapName);
              }),
              set: method(function (value, prop) {
                return desc.set.call(this, value, mapName);
              })
            };
          },

          /**
           * Property descriptor for forwarded properties which have node values
           * which may be nodes that might be outside of the virtual document.
           */
          related: {
            enumerable: true,
            extendedAccessors: true,
            get: method(function (prop) {
              var privates = p(this);
              if (privates.policy.upwardNavigation) {
                // TODO(kpreid): Can we move this check *into* tameRelatedNode?
                return tameRelatedNode(privates.feral[prop], defaultTameNode);
              } else {
                return null;
              }
            })
          },

          /**
           * Property descriptor which maps to an attribute or property, is
           * assignable, and has the value transformed in some way.
           * @param {boolean} useAttrGetter true if the getter should delegate
           *     to {@code this.getAttribute}.  That method is assumed to
           *     already be trusted though {@code toValue} is still called on
           *     the result.
           *     If false, then {@code toValue} is called on the result of
           *     accessing the name property on the underlying element, a
           *     possibly untrusted value.
           * @param {Function} toValue transforms the attribute or underlying
           *     property value retrieved according to the useAttrGetter flag
           *     above to the value of the defined property.
           * @param {boolean} useAttrSetter like useAttrGetter but for a setter.
           *     Switches between the name property on the underlying node
           *     (the false case) or using this's {@code setAttribute} method
           *     (the true case).
           * @param {Function} fromValue called on the input before it is passed
           *     through according to the flag above.  This receives untrusted
           *     values, and can do any vetting or transformation.  If
           *     {@code useAttrSetter} is true then it need not do much value
           *     vetting since the {@code setAttribute} method must do its own
           *     vetting.
           */
          filter: function (useAttrGetter, toValue, useAttrSetter, fromValue) {
            var desc = {
              enumerable: true,
              extendedAccessors: true,
              get: useAttrGetter
                  ? method(function (name) {
                      return toValue.call(this, this.getAttribute(name));
                    })
                  : method(function (name) {
                      return toValue.call(this, p(this).feral[name]);
                    })
            };
            if (fromValue) {
              desc.set = useAttrSetter
                  ? method(function (value, name) {
                      this.setAttribute(name, fromValue.call(this, value));
                    })
                  : method(function (value, name) {
                      var privates = p(this);
                      privates.policy.requireEditable();
                      privates.feral[name] = fromValue.call(this, value);
                    });
            }
            return desc;
          },
          filterAttr: function(toValue, fromValue) {
            return NP.filter(true, toValue, true, fromValue);
          },
          filterProp: function(toValue, fromValue) {
            return NP.filter(false, toValue, false, fromValue);
          }
        });
      }
      cajaVM.def(PropertyTaming);  // and its prototype

      // TODO(kpreid): We have completely unrelated things called 'np' and 'NP'.
      var NP = new PropertyTaming(TameNodeConf);

      // Node-specific property accessors:
      /**
       * Property descriptor for forwarded properties which have node values
       * and are descendants of this node.
       */
      var NP_tameDescendant = {
        enumerable: true,
        extendedAccessors: true,
        get: nodeMethod(function (prop) {
          var privates = np(this);
          if (privates.policy.childrenVisible) {
            return defaultTameNode(np(this).feral[prop]);
          } else {
            return null;
          }
        })
      };

      var nodeExpandos = new WeakMap(true);
      /**
       * Return the object which stores expando properties for a given
       * host DOM node.
       */
      function getNodeExpandoStorage(node) {
        var s = nodeExpandos.get(node);
        if (s === undefined) {
          nodeExpandos.set(node, s = {});
        }
        return s;
      }
      
      var nodeClassNoImplWarnings = {};

      function makeTameNodeByType(node) {
        switch (node.nodeType) {
          case 1:  // Element
            var tagName = node.tagName.toLowerCase();
            var schemaElem = htmlSchema.element(tagName);
            if (schemaElem.allowed || tagName === 'script') {
              // <script> is specifically allowed because we make provisions
              // for controlling its content and src.
              var domInterfaceName = schemaElem.domInterface;
              if (nodeTamers.hasOwnProperty(domInterfaceName)) {
                return new nodeTamers[domInterfaceName](node);
              } else {
                if (!nodeClassNoImplWarnings[domInterfaceName]) {
                  nodeClassNoImplWarnings[domInterfaceName] = true;
                  if (typeof console !== 'undefined') {
                    console.warn("Domado: " + domInterfaceName + " is not " +
                        "tamed; its specific properties/methods will not be " +
                        "available on <" +
                        htmlSchema.realToVirtualElementName(tagName) + ">.");
                  }
                }
                return new TameElement(node);
              }
            } else {
              // If an unrecognized or unsafe node, return a
              // placeholder that doesn't prevent tree navigation,
              // but that doesn't allow mutation or leak attribute
              // information.
              return new TameOpaqueNode(node);
            }
          case 2:  // Attr
            // Cannot generically wrap since we must have access to the
            // owner element
            throw 'Internal: Attr nodes cannot be generically wrapped';
          case 3:  // Text
          case 4:  // CDATA Section Node
            return new TameTextNode(node);
          case 8:  // Comment
            return new TameCommentNode(node);
          case 11: // Document Fragment
            return new TameBackedNode(node);
          default:
            return new TameOpaqueNode(node);
        }
      }

      /**
       * returns a tame DOM node.
       * @param {Node} node
       * @param {boolean} foreign
       * @see <a href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html"
       *       >DOM Level 2</a>
       */
      function defaultTameNode(node, foreign) {
        if (node === null || node === void 0) { return null; }
        node = makeDOMAccessible(node);
        // TODO(mikesamuel): make sure it really is a DOM node

        if (taming.hasTameTwin(node)) {
          return taming.tame(node);
        }

        if (foreign) {
          vdocContainsForeignNodes = true;
        }

        var tamed = foreign
            ? new TameForeignNode(node)
            : makeTameNodeByType(node);
        tamed = finishNode(tamed);

        return tamed;
      }

      function tameRelatedNode(node, tameNodeCtor) {
        if (node === null || node === void 0) { return null; }
        if (node === np(tameDocument).feralContainerNode) {
          return tameDocument;
        }

        node = makeDOMAccessible(node);

        // Catch errors because node might be from a different domain.
        try {
          var doc = node.ownerDocument;
          for (var ancestor = node;
              ancestor;
              ancestor = makeDOMAccessible(ancestor.parentNode)) {
            if (isContainerNode(ancestor)) {
              // is within the virtual document
              return tameNodeCtor(node);
            } else if (ancestor === doc) {
              // didn't find evidence of being within the virtual document
              return null;
            }
          }
          // permit orphaned nodes
          return tameNodeCtor(node);
        } catch (e) {}
        return null;
      }

      domicile.tameNodeAsForeign = function(node) {
        return defaultTameNode(node, true);
      };

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

      function nodeListEqualsArray(nodeList, array) {
        var nll = getNodeListLength(nodeList);
        if (nll !== array.length) {
          return false;
        } else {
          for (var i = 0; i < nll; i++) {
            if (nodeList[i] !== array[i]) {
              return false;
            }
          }
          return true;
        }
      }

      // Commentary on foreign node children in NodeLists:
      //
      // The children of a foreign node are an implementation detail which
      // guest code should not be permitted to see. Therefore, we must hide them
      // from appearing in NodeLists. This would be a straightforward matter of
      // filtering, except that NodeLists are "live", reflecting DOM changes
      // immediately; and DOM changes change the membership and numeric indexes
      // of the NodeList.
      //
      // One could imagine caching the outcomes: given an index, scan the host
      // list until the required number of visible-to-guest nodes have been
      // found, cache the indexes and node, and then validate the cache entry
      // later by comparing indexes, but that is not sufficient; consider if a
      // foreign child is deleted, and at the same time a guest-visible node
      // is added in a similar document position; then the index of a guest node
      // which is after that position *should* increase, but this cache cannot
      // tell.
      //
      // Therefore, we do cache the list, but we must re-validate the cache
      // from 0 up to the desired index on every access.
      /**
       * This is NOT a node list taming. This is a component for performing
       * foreign node filtering only.
       */
      function NodeListFilter(feralNodeList) {
        feralNodeList = makeDOMAccessible(feralNodeList);
        var expectation = [];
        var filteredCache = [];

        function calcUpTo(index) {
          var feralLength = getNodeListLength(feralNodeList);
          var feralIndex = 0;

          // Validate cache
          if (feralLength < expectation.length) {
            expectation = [];
            filteredCache = [];
            feralIndex = 0;
          } else {
            for (
                ;
                feralIndex < expectation.length && feralIndex < feralLength;
                feralIndex++) {
              if (feralNodeList[feralIndex] !== expectation[feralIndex]) {
                expectation = [];
                filteredCache = [];
                feralIndex = 0;
                break;
              }
            }
          }

          // Extend cache
          nodeListScan: for (
              ;
              feralIndex < feralLength && filteredCache.length <= index;
              feralIndex++) {
            var node = feralNodeList[feralIndex];
            expectation.push(node);
            makeDOMAccessible(node);
            // Filter out foreign nodes' descendants
            walkUp: for (
                var ancestor = makeDOMAccessible(node.parentNode);
                ancestor !== null;
                ancestor = makeDOMAccessible(ancestor.parentNode)) {
              if (taming.hasTameTwin(ancestor)) {
                if (taming.tame(ancestor) instanceof TameForeignNode) {
                  // Every foreign node is already tamed as foreign, by
                  // definition.
                  continue nodeListScan;
                } else {
                  // Reached a node known to be non-foreign.
                  break walkUp;
                }
              }
            }
            // Not a foreign node descendant, so include it in the list.
            filteredCache.push(node);
          }
        }
        return cajaVM.def({
          getLength: function() {
            if (vdocContainsForeignNodes) {
              calcUpTo(Infinity);
              return filteredCache.length;
            } else {
              return getNodeListLength(feralNodeList);
            }
          },
          item: function(i) {
            if (vdocContainsForeignNodes) {
              calcUpTo(i);
              return filteredCache[i];
            } else {
              return feralNodeList[i];
            }
          }
        });
      }

      /**
       * Constructs a NodeList-like object.
       *
       * @param tamed a JavaScript array that will be populated and decorated
       *     with the DOM NodeList API. If it has existing elements they will
       *     precede the actual NodeList elements.
       * @param nodeList an array-like object supporting a "length" property
       *     and "[]" numeric indexing, or a raw DOM NodeList;
       * @param opt_tameNodeCtor a function for constructing tame nodes
       *     out of raw DOM nodes.
       */
      function mixinNodeList(tamed, nodeList, opt_tameNodeCtor) {
        // TODO(kpreid): Under a true ES5 environment, node lists should be
        // proxies so that they preserve liveness of the original lists.
        // This should be controlled by an option.
        // UPDATE: We have live NodeLists as TameNodeList and TameOptionsList.
        // This is not live, but is used in less-mainstream cases.

        var visibleList = new NodeListFilter(nodeList);

        var limit = visibleList.getLength();
        if (limit > 0 && !opt_tameNodeCtor) {
          throw 'Internal: Nonempty mixinNodeList() without a tameNodeCtor';
        }

        for (var i = tamed.length, j = 0;
             j < limit && visibleList.item(j);
             ++i, ++j) {
          tamed[+i] = opt_tameNodeCtor(visibleList.item(+j));
        }

        // Guard against accidental leakage of untamed nodes
        nodeList = visibleList = null;

        tamed.item = cajaVM.def(function (k) {
          k &= 0x7fffffff;
          if (k !== k) { throw new Error(); }
          return tamed[+k] || null;
        });

        return tamed;
      }

      function rebuildTameListConstructors(ArrayLike) {
        TameNodeList = makeTameNodeList();
        TameNodeList.prototype = Object.create(ArrayLike.prototype);
        Object.defineProperty(TameNodeList.prototype, 'constructor',
            { value: TameNodeList });
        Object.freeze(TameNodeList.prototype);
        Object.freeze(TameNodeList);
        TameOptionsList = makeTameOptionsList();
        TameOptionsList.prototype = Object.create(ArrayLike.prototype);
        Object.defineProperty(TameOptionsList.prototype, 'constructor',
            { value: TameOptionsList });
        Object.freeze(TameOptionsList.prototype);
        Object.freeze(TameOptionsList);
      }

      function makeTameNodeList() {
        return function TNL(nodeList, tameNodeCtor) {
            var visibleList = new NodeListFilter(nodeList);
            function getItem(i) {
              i = +i;
              if (i >= visibleList.getLength()) { return void 0; }
              return tameNodeCtor(visibleList.item(i));
            }
            var getLength = visibleList.getLength.bind(visibleList);
            var len = +getLength();
            var ArrayLike = cajaVM.makeArrayLike(len);
            if (!(TameNodeList.prototype instanceof ArrayLike)) {
              rebuildTameListConstructors(ArrayLike);
            }
            var result = ArrayLike(TameNodeList.prototype, getItem, getLength);
            Object.defineProperty(result, 'item',
                { value: Object.freeze(getItem) });
            return result;
          };
      }

      var TameNodeList = Object.freeze(makeTameNodeList());

      function makeTameOptionsList() {
        return function TOL(nodeList, opt_tameNodeCtor) {
            var visibleList = new NodeListFilter(nodeList);
            function getItem(i) {
              i = +i;
              return opt_tameNodeCtor(visibleList.item(i));
            }
            var getLength = visibleList.getLength.bind(visibleList);
            var len = +getLength();
            var ArrayLike = cajaVM.makeArrayLike(len);
            if (!(TameOptionsList.prototype instanceof ArrayLike)) {
              rebuildTameListConstructors(ArrayLike);
            }
            var result = ArrayLike(
                TameOptionsList.prototype, getItem, getLength);
            Object.defineProperty(result, 'selectedIndex', {
                get: function () { return +nodeList.selectedIndex; }
              });
            return result;
          };
      }

      var TameOptionsList = Object.freeze(makeTameOptionsList());

      /**
       * Return a fake node list containing tamed nodes.
       * @param {Array.<TameNode>} array of tamed nodes.
       * @return an array that duck types to a node list.
       */
      function fakeNodeList(array) {
        array.item = cajaVM.def(function(i) { return array[+i]; });
        return Object.freeze(array);
      }

      /**
       * Constructs an HTMLCollection-like object which indexes its elements
       * based on their NAME attribute.
       *
       * @param tamed a JavaScript array that will be populated and decorated
       *     with the DOM HTMLCollection API.
       * @param nodeList an array-like object supporting a "length" property
       *     and "[]" numeric indexing.
       * @param opt_tameNodeCtor a function for constructing tame nodes
       *     out of raw DOM nodes.
       *
       * TODO(kpreid): Per
       * <http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-75708506>
       * this should be looking up ids as well as names. (And not returning
       * nodelists, but is that for compatibility?)
       */
      function mixinHTMLCollection(tamed, nodeList, opt_tameNodeCtor) {
        mixinNodeList(tamed, nodeList, opt_tameNodeCtor);

        var tameNodesByName = {};
        var tameNode;

        for (var i = 0; i < tamed.length && (tameNode = tamed[+i]); ++i) {
          var name = void 0;
          if (tameNode.getAttribute) { name = tameNode.getAttribute('name'); }
          if (name && !(name.charAt(name.length - 1) === '_' || (name in tamed)
                       || name === String(name & 0x7fffffff))) {
            if (!tameNodesByName[name]) { tameNodesByName[name] = []; }
            tameNodesByName[name].push(tameNode);
          }
        }

        for (var name in tameNodesByName) {
          var tameNodes = tameNodesByName[name];
          if (tameNodes.length > 1) {
            tamed[name] = fakeNodeList(tameNodes);
          } else {
            tamed[name] = tameNodes[0];
          }
        }

        tamed.namedItem = cajaVM.def(function(name) {
          name = String(name);
          if (name.charAt(name.length - 1) === '_') {
            return null;
          }
          if (Object.prototype.hasOwnProperty.call(tamed, name)) {
            return cajaVM.passesGuard(TameNodeT, tamed[name])
                ? tamed[name] : tamed[name][0];
          }
          return null;
        });

        return tamed;
      }

      function tameHTMLCollection(nodeList, opt_tameNodeCtor) {
        return Object.freeze(
            mixinHTMLCollection([], nodeList, opt_tameNodeCtor));
      }

      function tameGetElementsByTagName(rootNode, tagName) {
        tagName = String(tagName);
        var eflags = 0;
        if (tagName !== '*') {
          tagName = tagName.toLowerCase();
          tagName = virtualToRealElementName(tagName);
        }
        return new TameNodeList(rootNode.getElementsByTagName(tagName),
            defaultTameNode);
      }

      /**
       * Implements http://www.whatwg.org/specs/web-apps/current-work/#dom-document-getelementsbyclassname
       * using an existing implementation on browsers that have one.
       */
      function tameGetElementsByClassName(rootNode, className) {
        className = String(className);

        // The quotes below are taken from the HTML5 draft referenced above.

        // "having obtained the classes by splitting a string on spaces"
        // Instead of using split, we use match with the global modifier so that
        // we don't have to remove leading and trailing spaces.
        var classes = className.match(/[^\t\n\f\r ]+/g);

        // Filter out classnames in the restricted namespace.
        for (var i = classes ? classes.length : 0; --i >= 0;) {
          var classi = classes[+i];
          if (FORBIDDEN_ID_PATTERN.test(classi)) {
            classes[+i] = classes[classes.length - 1];
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
          return new TameNodeList(
              rootNode.getElementsByClassName(
                  classes.join(' ')), defaultTameNode);
        } else {
          // Add spaces around each class so that we can use indexOf later to
          // find a match.
          // This use of indexOf is strictly incorrect since
          // http://www.whatwg.org/specs/web-apps/current-work/#reflecting-content-attributes-in-dom-attributes
          // does not normalize spaces in unordered sets of unique
          // space-separated tokens.  This is not a problem since HTML5
          // compliant implementations already have a getElementsByClassName
          // implementation, and legacy
          // implementations do normalize according to comments on issue 935.

          // We assume standards mode, so the HTML5 requirement that
          //   "If the document is in quirks mode, then the comparisons for the
          //    classes must be done in an ASCII case-insensitive  manner,"
          // is not operative.
          var nClasses = classes.length;
          for (var i = nClasses; --i >= 0;) {
            classes[+i] = ' ' + classes[+i] + ' ';
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
               j < limit && (candidate = candidates[+j]);
               ++j) {
            var candidateClass = ' ' + candidate.className + ' ';
            for (var i = nClasses; --i >= 0;) {
              if (-1 === candidateClass.indexOf(classes[+i])) {
                continue candidate_loop;
              }
            }
            var tamed = defaultTameNode(candidate);
            if (tamed) {
              matches[++k] = tamed;
            }
          }
          // "the method must return a live NodeList object"
          return fakeNodeList(matches);
        }
      }

      function makeEventHandlerWrapper(thisNode, listener) {
        domitaModules.ensureValidCallback(listener);
        function wrapper(event) {
          return plugin_dispatchEvent(
              thisNode, event, rulebreaker.getId(tameWindow), listener);
        }
        return wrapper;
      }

      // Implementation of EventTarget::addEventListener
      function tameAddEventListener(name, listener, useCapture) {
        name = String(name);
        useCapture = Boolean(useCapture);
        var privates = np(this);
        var feral = privates.feral;
        privates.policy.requireEditable();
        var list = privates.wrappedListeners;
        if (!list) {
          list = privates.wrappedListeners = [];
        }
        if (searchForListener(list, name, listener, useCapture) === null) {
          var wrappedListener = makeEventHandlerWrapper(
              privates.feral, listener);
          wrappedListener = bridal.addEventListener(
              privates.feral, name, wrappedListener, useCapture);
          list.push({
            n: name,
            l: listener,
            c: useCapture,
            wrapper: wrappedListener
          });
        }
      }

      // Implementation of EventTarget::removeEventListener
      function tameRemoveEventListener(name, listener, useCapture) {
        name = String(name);
        useCapture = Boolean(useCapture);
        var privates = np(this);
        var feral = privates.feral;
        privates.policy.requireEditable();
        var list = privates.wrappedListeners;
        if (!list) { return; }
        var match = searchForListener(list, name, listener, useCapture);
        if (match !== null) {
          bridal.removeEventListener(
              privates.feral, name, list[match].wrapper, useCapture);
          arrayRemove(list, match, match);
        }
      }

      function searchForListener(list, name, listener, useCapture) {
        for (var i = list.length; --i >= 0;) {
          var record = list[+i];
          if (record.n === name &&
              record.l === listener &&
              record.c === useCapture) {
            return i;
          }
        }
        return null;
      }

      // A map of tamed node classes, keyed by DOM Level 2 standard name, which
      // will be exposed to the client (and are not usable as constructors).
      var nodeClasses = {};
      // Ditto, but the taming constructors instead of the useless ones.
      var nodeTamers = {};

      /**
       * This does three things:
       *
       * Replace tamedCtor's prototype with one whose prototype is someSuper.
       *
       * Hide the constructor of the products of tamedCtor, replacing it with a
       * function which just throws (but can still be used for instanceof
       * checks).
       *
       * Register the inert ctor under the given name if specified.
       */
      function inertCtor(tamedCtor, someSuper, opt_name) {
        inherit(tamedCtor, someSuper);

        var inert = function() {
          throw new TypeError('This constructor cannot be called directly.');
        };
        inert.prototype = tamedCtor.prototype;
        Object.freeze(inert);  // not def, because inert.prototype must remain
        setOwn(tamedCtor.prototype, "constructor", inert);

        if (opt_name !== undefined) {
          nodeClasses[opt_name] = inert;
          nodeTamers[opt_name] = tamedCtor;
        }

        return inert;
      }

      // We have now set up most of the 'support' facilities and are starting to
      // define node taming classes.

      /**
       * Base class for a Node wrapper.  Do not create directly -- use the
       * tameNode factory instead.
       *
       * NOTE that all TameNodes should have the TameNodeT trademark, but it is
       * not applied here since that freezes the object, and also because of the
       * forwarding proxies used for catching expando properties.
       *
       * @param {policy} Mutability policy to apply.
       * @constructor
       */
      function TameNode(policy) {
        TameNodeConf.confide(this);
        if (!policy || !policy.requireEditable) {
          throw new Error("Domado internal error: Policy missing or invalid");
        }
        np(this).policy = policy;
        return this;
      }
      inertCtor(TameNode, Object, 'Node');
      definePropertiesAwesomely(TameNode.prototype, {
        baseURI: {
          enumerable: canHaveEnumerableAccessors,
          get: nodeMethod(function() {
            return domicile.pseudoLocation.href;
          })
        },
        ownerDocument: {
          // tameDocument is not yet defined at this point so can't be a constant
          enumerable: canHaveEnumerableAccessors,
          get: nodeMethod(function() { return tameDocument; })
        }
      });
      /**
       * Print this object according to its tamed class name; also note for
       * debugging purposes if it is actually the prototype instance.
       */
      setOwn(TameNode.prototype, "toString", cajaVM.def(function() {
        return nodeToStringSearch(this, this);
      }));
      function nodeToStringSearch(self, prototype) {
        // recursion base case
        if (typeof prototype !== 'object' || prototype === Object.prototype) {
          return Object.prototype.toString.call(self);
        }

        var ctor = prototype.constructor;
        for (var name in nodeClasses) { // TODO(kpreid): less O(n)
          if (nodeClasses[name] === ctor) {
            if (ctor.prototype === self) {
              return "[domado PROTOTYPE OF " + name + "]";
            } else {
              return "[domado object " + name + ' ' + self.nodeName + "]";
            }
          }
        }

        // try next ancestor
        return nodeToStringSearch(self, Object.getPrototypeOf(prototype));
      }
      // abstract TameNode.prototype.nodeType
      // abstract TameNode.prototype.nodeName
      // abstract TameNode.prototype.nodeValue
      // abstract TameNode.prototype.cloneNode
      // abstract TameNode.prototype.appendChild
      // abstract TameNode.prototype.insertBefore
      // abstract TameNode.prototype.removeChild
      // abstract TameNode.prototype.replaceChild
      // abstract TameNode.prototype.firstChild
      // abstract TameNode.prototype.lastChild
      // abstract TameNode.prototype.nextSibling
      // abstract TameNode.prototype.previousSibling
      // abstract TameNode.prototype.parentNode
      // abstract TameNode.prototype.getElementsByTagName
      // abstract TameNode.prototype.getElementsByClassName
      // abstract TameNode.prototype.childNodes
      // abstract TameNode.prototype.attributes
      cajaVM.def(TameNode);  // and its prototype

      /**
       * A tame node that is backed by a real node.
       *
       * Note that the constructor returns a proxy which delegates to 'this';
       * subclasses should apply properties to 'this' but return the proxy.
       *
       * @param {Function} opt_proxyType The constructor of the proxy handler
       *     to use, defaulting to ExpandoProxyHandler.
       * @constructor
       */
      function TameBackedNode(node, opt_policy, opt_proxyType) {
        node = makeDOMAccessible(node);

        if (!node) {
          throw new Error('Creating tame node with undefined native delegate');
        }

        // Determine access policy
        var parent = makeDOMAccessible(node.parentNode);
        var parentPolicy;
        if (!parent || isContainerNode(parent) || isContainerNode(node)) {
          parentPolicy = null;
        } else {
          // Parent is inside the vdoc.
          parentPolicy = np(defaultTameNode(parent)).policy;
        }
        var policy;
        if (opt_policy) {
          if (parentPolicy) {
            parentPolicy.childPolicy.assertRestrictedBy(opt_policy);
          }
          policy = opt_policy;
          //policy = new TracedNodePolicy(policy, "explicit", null);
        } else if (isContainerNode(parent)) {
          // Virtual document root -- stop implicit recursion and define the
          // root policy. If we wanted to be able to define a "entire DOM
          // read-only" policy, this is where to hook it in.
          policy = nodePolicyEditable;
          //policy = new TracedNodePolicy(policy, "child-of-root", null);
        } else if (parentPolicy) {
          policy = parentPolicy.childPolicy;
          //policy = new TracedNodePolicy(policy,
          //    "childPolicy of " + parent.nodeName, parentPolicy);
        } else {
          policy = nodePolicyEditable;
          //policy = new TracedNodePolicy(policy, "isolated", null);
        }

        TameNode.call(this, policy);

        np(this).feral = node;

        if (domitaModules.proxiesAvailable) {
          np(this).proxyHandler = new (opt_proxyType || ExpandoProxyHandler)(
              this, policy.editable, getNodeExpandoStorage(node));
        }
      }
      inertCtor(TameBackedNode, TameNode);
      definePropertiesAwesomely(TameBackedNode.prototype, {
        nodeType: NP.ro,
        nodeName: NP.ro,
        nodeValue: NP.ro,
        firstChild: NP_tameDescendant, // TODO(kpreid): Must be disableable
        lastChild: NP_tameDescendant,
        nextSibling: NP.related,
        previousSibling: NP.related,
        parentNode: NP.related,
        childNodes: {
          enumerable: true,
          get: cajaVM.def(function () {
            var privates = np(this);
            if (privates.policy.childrenVisible) {
              return new TameNodeList(np(this).feral.childNodes,
                  defaultTameNode);
            } else {
              return fakeNodeList([]);
            }
          })
        },
        attributes: {
          enumerable: true,
          get: cajaVM.def(function () {
            var privates = np(this);
            if (privates.policy.attributesVisible) {
              var thisNode = privates.feral;
              var tameNodeCtor = function(node) {
                return new TameBackedAttributeNode(node, thisNode);
              };
              // TODO(kpreid): There is no test which caught a previous
              // editability policy failure here
              return new TameNodeList(thisNode.attributes, tameNodeCtor);
            } else {
              return fakeNodeList([]);
            }
          })
        }
      });
      TameBackedNode.prototype.cloneNode = nodeMethod(function (deep) {
        np(this).policy.requireUnrestricted();
        var clone = bridal.cloneNode(np(this).feral, Boolean(deep));
        // From http://www.w3.org/TR/DOM-Level-2-Core/core.html#ID-3A0ED0A4
        //   "Note that cloning an immutable subtree results in a mutable copy"
        return defaultTameNode(clone);
      });
      /** Is it OK to make 'child' a child of 'parent'? */
      function checkAdoption(parent, child) {
        // Child must be editable since appendChild can remove it from its
        // parent.
        np(parent).policy.requireChildrenEditable();
        np(child).policy.requireEditable();
        // Sanity check: this cannot currently happen but if it does then we
        // need to rethink the calculation of policies.
        np(parent).policy.childPolicy.assertRestrictedBy(np(child).policy);
      }
      TameBackedNode.prototype.appendChild = nodeMethod(function (child) {
        child = child || {};
        child = TameNodeT.coerce(child);

        checkAdoption(this, child);

        np(this).feral.appendChild(np(child).feral);
        return child;
      });
      TameBackedNode.prototype.insertBefore = nodeMethod(
          function(toInsert, child) {
        toInsert = TameNodeT.coerce(toInsert);
        if (child === void 0) { child = null; }

        if (child !== null) {
          child = TameNodeT.coerce(child);
          // TODO(kpreid): This child is not being mutated except for its
          // previousSibling, so why are we rejecting here?
          np(child).policy.requireEditable();
        }
        checkAdoption(this, toInsert);

        np(this).feral.insertBefore(
            np(toInsert).feral, child !== null ? np(child).feral : null);
        return toInsert;
      });
      TameBackedNode.prototype.removeChild = nodeMethod(function(child) {
        child = TameNodeT.coerce(child);
        np(this).policy.requireChildrenEditable();
        np(child).policy.requireEditable();
        np(this).feral.removeChild(np(child).feral);
        return child;
      });
      TameBackedNode.prototype.replaceChild = nodeMethod(
          function(newChild, oldChild) {
        newChild = TameNodeT.coerce(newChild);
        oldChild = TameNodeT.coerce(oldChild);

        checkAdoption(this, newChild);
        np(oldChild).policy.requireEditable();

        np(this).feral.replaceChild(np(newChild).feral, np(oldChild).feral);
        return oldChild;
      });
      TameBackedNode.prototype.hasChildNodes = nodeMethod(function() {
        if (np(this).policy.childrenVisible) {
          return !!np(this).feral.hasChildNodes();
        } else {
          return false;
        }
      });
      // http://www.w3.org/TR/DOM-Level-2-Events/events.html#Events-EventTarget
      // "The EventTarget interface is implemented by all Nodes"
      TameBackedNode.prototype.dispatchEvent = nodeMethod(function(evt) {
        evt = TameEventT.coerce(evt);
        bridal.dispatchEvent(np(this).feral, TameEventConf.p(evt).feral);
      });

      if (docEl.contains) {  // typeof is 'object' on IE
        TameBackedNode.prototype.contains = nodeMethod(function (other) {
          if (other === null || other === void 0) { return false; }
          other = TameNodeT.coerce(other);
          var otherNode = np(other).feral;
          return np(this).feral.contains(otherNode);
        });
      }
      if ('function' ===
          typeof docEl.compareDocumentPosition) {
        /**
         * Speced in <a href="http://www.w3.org/TR/DOM-Level-3-Core/core.html#Node3-compareDocumentPosition">DOM-Level-3</a>.
         */
        TameBackedNode.prototype.compareDocumentPosition = nodeMethod(
            function (other) {
          other = TameNodeT.coerce(other);
          var otherNode = np(other).feral;
          if (!otherNode) { return 0; }
          var bitmask = +np(this).feral.compareDocumentPosition(otherNode);
          // To avoid leaking information about the relative positioning of
          // different roots, if neither contains the other, then we mask out
          // the preceding/following bits.
          // 0x18 is (CONTAINS | CONTAINED)
          // 0x1f is all the bits documented at
          //   http://www.w3.org/TR/DOM-Level-3-Core/core.html#DocumentPosition
          //   except IMPLEMENTATION_SPECIFIC
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
        });
        if (!Object.prototype.hasOwnProperty.call(TameBackedNode.prototype,
            'contains')) {
          // http://www.quirksmode.org/blog/archives/2006/01/contains_for_mo.html
          TameBackedNode.prototype.contains = nodeMethod(function (other) {
            if (other === null || other === void 0) { return false; }
            var docPos = this.compareDocumentPosition(other);
            return !(!(docPos & 0x10) && docPos);
          });
        }
      }
      cajaVM.def(TameBackedNode);  // and its prototype

      /**
       * A fake node that is not backed by a real DOM node.
       * @constructor
       */
      function TamePseudoNode() {
        // Note inconsistency: we have an editable policy, for the sake of our
        // children, but don't actually allow direct mutation.
        TameNode.call(this, nodePolicyEditable);

        if (domitaModules.proxiesAvailable) {
          // finishNode will wrap 'this' with an actual proxy later.
          np(this).proxyHandler = new ExpandoProxyHandler(this, true, {});
        }
      }
      inertCtor(TamePseudoNode, TameNode);
      TamePseudoNode.prototype.appendChild =
      TamePseudoNode.prototype.insertBefore =
      TamePseudoNode.prototype.removeChild =
      TamePseudoNode.prototype.replaceChild = nodeMethod(function () {
        if (typeof console !== 'undefined') {
          console.log("Node not editable; no action performed.");
        }
        return void 0;
      });
      TamePseudoNode.prototype.hasChildNodes = nodeMethod(function () {
        return this.firstChild != null;
      });
      definePropertiesAwesomely(TamePseudoNode.prototype, {
        firstChild: { enumerable: true, get: nodeMethod(function () {
          var children = this.childNodes;
          return children.length ? children[0] : null;
        })},
        lastChild: { enumerable: true, get: nodeMethod(function () {
          var children = this.childNodes;
          return children.length ? children[children.length - 1] : null;
        })},
        nextSibling: { enumerable: true, get: nodeMethod(function () {
          var self = tamingProxies.get(this) || this;
          var parentNode = this.parentNode;
          if (!parentNode) { return null; }
          var siblings = parentNode.childNodes;
          for (var i = siblings.length - 1; --i >= 0;) {
            if (siblings[+i] === self) { return siblings[i + 1]; }
          }
          return null;
        })},
        previousSibling: { enumerable: true, get: nodeMethod(function () {
          var self = tamingProxies.get(this) || this;
          var parentNode = this.parentNode;
          if (!parentNode) { return null; }
          var siblings = parentNode.childNodes;
          for (var i = siblings.length; --i >= 1;) {
            if (siblings[+i] === self) { return siblings[i - 1]; }
          }
          return null;
        })}
      });
      cajaVM.def(TamePseudoNode);  // and its prototype

      // Restricted node types:

      // An opaque node is traversible but not manipulable by guest code. This
      // is the default taming for unrecognized nodes or nodes not explicitly
      // whitelisted.
      function TameOpaqueNode(node) {
        TameBackedNode.call(this, node, nodePolicyOpaque);
      }
      inertCtor(TameOpaqueNode, TameBackedNode);
      cajaVM.def(TameOpaqueNode);

      // A foreign node is one supplied by some external system to the guest
      // code, which the guest code may lay out within its own DOM tree but may
      // not traverse into in any way.
      function TameForeignNode(node) {
        TameBackedNode.call(this, node, nodePolicyForeign);
      }
      inertCtor(TameForeignNode, TameBackedNode);
      TameForeignNode.prototype.getElementsByTagName = function (tagName) {
        // needed because TameForeignNode doesn't inherit TameElement
        return fakeNodeList([]);
      };
      TameForeignNode.prototype.getElementsByClassName = function (className) {
        // needed because TameForeignNode doesn't inherit TameElement
        return fakeNodeList([]);
      };
      cajaVM.def(TameForeignNode);

      // Non-element node types:

      function TameTextNode(node) {
        assert(node.nodeType === 3);
        TameBackedNode.call(this, node);
      }
      inertCtor(TameTextNode, TameBackedNode, 'Text');
      var textAccessor = {
        enumerable: true,
        get: nodeMethod(function () {
          return np(this).feral.nodeValue;
        }),
        set: nodeMethod(function (value) {
          np(this).policy.requireEditable();
          np(this).feral.nodeValue = String(value || '');
        })
      };
      definePropertiesAwesomely(TameTextNode.prototype, {
        nodeValue: textAccessor,
        textContent: textAccessor,
        innerText: textAccessor,
        data: textAccessor
      });
      cajaVM.def(TameTextNode);  // and its prototype

      function TameCommentNode(node) {
        assert(node.nodeType === 8);
        TameBackedNode.call(this, node);
      }
      inertCtor(TameCommentNode, TameBackedNode, 'Comment');
      cajaVM.def(TameCommentNode);  // and its prototype

      /**
       * Plays the role of an Attr node for TameElement objects.
       */
      function TameBackedAttributeNode(node, ownerElement) {
        if (ownerElement === undefined) throw new Error(
            "ownerElement undefined");
        TameBackedNode.call(this, node,
            np(defaultTameNode(ownerElement)).policy);
        np(this).ownerElement = ownerElement;
      }
      inertCtor(TameBackedAttributeNode, TameBackedNode, 'Attr');
      setOwn(TameBackedAttributeNode.prototype, 'cloneNode',
          nodeMethod(function (deep) {
        var clone = bridal.cloneNode(np(this).feral, Boolean(deep));
        // From http://www.w3.org/TR/DOM-Level-2-Core/core.html#ID-3A0ED0A4
        //   "Note that cloning an immutable subtree results in a mutable copy"
        return new TameBackedAttributeNode(clone, np(this).ownerElement);
      }));
      var nameAccessor = {
        enumerable: true,
        get: nodeMethod(function () {
          var name = np(this).feral.name;
          if (cajaPrefRe.test(name)) {
            name = name.substring(cajaPrefix.length);
          }
          return name;
        })
      };
      var valueAccessor = {
        enumerable: true,
        get: nodeMethod(function () {
           return this.ownerElement.getAttribute(this.name);
        }),
        set: nodeMethod(function (value) {
          return this.ownerElement.setAttribute(this.name, value);
        })
      };
      definePropertiesAwesomely(TameBackedAttributeNode.prototype, {
        nodeName: nameAccessor,
        name: nameAccessor,
        specified: {
          enumerable: true,
          get: nodeMethod(function () {
            return this.ownerElement.hasAttribute(this.name);
          })
        },
        nodeValue: valueAccessor,
        value: valueAccessor,
        ownerElement: {
          enumerable: true,
          get: nodeMethod(function () {
            return defaultTameNode(np(this).ownerElement);
          })
        },
        nodeType: P_constant(2),
        firstChild:      P_UNIMPLEMENTED,
        lastChild:       P_UNIMPLEMENTED,
        nextSibling:     P_UNIMPLEMENTED,
        previousSibling: P_UNIMPLEMENTED,
        parentNode:      P_UNIMPLEMENTED,
        childNodes:      P_UNIMPLEMENTED,
        attributes:      P_UNIMPLEMENTED
      });
      var notImplementedNodeMethod = {
        enumerable: true,
        value: nodeMethod(function () {
          throw new Error('Not implemented.');
        })
      };
      ['appendChild', 'insertBefore', 'removeChild', 'replaceChild',
          ].forEach(function (m) {
        Object.defineProperty(
          TameBackedAttributeNode.prototype, m, notImplementedNodeMethod);
      });
      cajaVM.def(TameBackedAttributeNode);  // and its prototype

      // Register set handlers for onclick, onmouseover, etc.
      function registerElementScriptAttributeHandlers(tameElementPrototype) {
        var seenAlready = {};
        var attrNameRe = /::(.*)/;
        for (var html4Attrib in html4.ATTRIBS) {
          if (html4.atype.SCRIPT === html4.ATTRIBS[html4Attrib]) {
            (function (attribName) {
              // Attribute names are defined per-element, so we will see
              // duplicates here.
              if (Object.prototype.hasOwnProperty.call(
                  seenAlready, attribName)) {
                return;
              }
              seenAlready[attribName] = true;

              Object.defineProperty(tameElementPrototype, attribName, {
                enumerable: canHaveEnumerableAccessors,
                configurable: false,
                set: nodeMethod(function eventHandlerSetter(listener) {
                  np(this).policy.requireEditable();
                  if (!listener) {  // Clear the current handler
                    np(this).feral[attribName] = null;
                  } else {
                    // This handler cannot be copied from one node to another
                    // which is why getters are not yet supported.
                    np(this).feral[attribName] = makeEventHandlerWrapper(
                        np(this).feral, listener);
                  }
                  return listener;
                })
              });
            })(html4Attrib.match(attrNameRe)[1]);
          }
        }
      }

      // Elements in general and specific elements:

      /**
       * See comments on TameBackedNode regarding return value.
       * @constructor
       */
      function TameElement(node, opt_policy, opt_proxyType) {
        assert(node.nodeType === 1);
        var obj = TameBackedNode.call(this, node, opt_policy, opt_proxyType);
        np(this).geometryDelegate = node;
        return obj;
      }
      nodeClasses.Element = inertCtor(TameElement, TameBackedNode,
          'HTMLElement');
      registerElementScriptAttributeHandlers(TameElement.prototype);
      TameElement.prototype.blur = nodeMethod(function () {
        np(this).feral.blur();
      });
      TameElement.prototype.focus = nodeMethod(function () {
        return domicile.handlingUserAction && np(this).feral.focus();
      });
      // IE-specific method.  Sets the element that will have focus when the
      // window has focus, without focusing the window.
      if (docEl.setActive) {
        TameElement.prototype.setActive = nodeMethod(function () {
          return domicile.handlingUserAction && np(this).feral.setActive();
        });
      }
      // IE-specific method.
      if (docEl.hasFocus) {
        TameElement.prototype.hasFocus = nodeMethod(function () {
          return np(this).feral.hasFocus();
        });
      }
      TameElement.prototype.getAttribute = nodeMethod(function (attribName) {
        if (!np(this).policy.attributesVisible) { return null; }
        var feral = np(this).feral;
        attribName = String(attribName).toLowerCase();
        if (/__$/.test(attribName)) {
          throw new TypeError('Attributes may not end with __');
        }
        var tagName = feral.tagName.toLowerCase();
        var atype = htmlSchema.attribute(tagName, attribName).type;
        if (atype === void 0) {
          return feral.getAttribute(cajaPrefix + attribName);
        }
        var value = bridal.getAttribute(feral, attribName);
        if ('string' !== typeof value) { return value; }
        return virtualizeAttributeValue(atype, value);
      });
      TameElement.prototype.getAttributeNode = nodeMethod(function (name) {
        if (!np(this).policy.attributesVisible) { return null; }
        var feral = np(this).feral;
        var hostDomNode = feral.getAttributeNode(name);
        if (hostDomNode === null) { return null; }
        return new TameBackedAttributeNode(hostDomNode, feral);
      });
      TameElement.prototype.hasAttribute = nodeMethod(function (attribName) {
        var feral = np(this).feral;
        attribName = String(attribName).toLowerCase();
        var tagName = feral.tagName.toLowerCase();
        var atype = htmlSchema.attribute(tagName, attribName).type;
        if (atype === void 0) {
          return bridal.hasAttribute(feral, cajaPrefix + attribName);
        } else {
          return bridal.hasAttribute(feral, attribName);
        }
      });
      TameElement.prototype.setAttribute = nodeMethod(
          function (attribName, value) {
        //console.debug("setAttribute", this, attribName, value);
        var feral = np(this).feral;
        np(this).policy.requireEditable();
        attribName = String(attribName).toLowerCase();
        if (/__$/.test(attribName)) {
          throw new TypeError('Attributes may not end with __');
        }
        if (!np(this).policy.attributesVisible) { return null; }
        var tagName = feral.tagName.toLowerCase();
        var atype = htmlSchema.attribute(tagName, attribName).type;
        if (atype === void 0) {
          bridal.setAttribute(feral, cajaPrefix + attribName, value);
        } else {
          var sanitizedValue = rewriteAttribute(
              tagName, attribName, atype, value);
          if (sanitizedValue !== null) {
            bridal.setAttribute(feral, attribName, sanitizedValue);
            if (html4.ATTRIBS.hasOwnProperty(tagName + '::target') &&
              atype === html4.atype.URI) {
              if (sanitizedValue.charAt(0) === '#') {
                feral.removeAttribute('target');
              } else {
                bridal.setAttribute(feral, 'target',
                  getSafeTargetAttribute(tagName, 'target',
                    bridal.getAttribute(feral, 'target')));
              }
            }
          } else {
            if (typeof console !== 'undefined') {
              console.warn('Rejecting <' + tagName + '>.setAttribute(',
                  attribName, ',', value, ')');
            }
          }
        }
        return value;
      });
      TameElement.prototype.removeAttribute = nodeMethod(function (attribName) {
        var feral = np(this).feral;
        np(this).policy.requireEditable();
        attribName = String(attribName).toLowerCase();
        if (/__$/.test(attribName)) {
          throw new TypeError('Attributes may not end with __');
        }
        var tagName = feral.tagName.toLowerCase();
        var atype = htmlSchema.attribute(tagName, attribName).type;
        if (atype === void 0) {
          feral.removeAttribute(cajaPrefix + attribName);
        } else {
          feral.removeAttribute(attribName);
        }
      });
      TameElement.prototype.getElementsByTagName = nodeMethod(
          function(tagName) {
        return tameGetElementsByTagName(np(this).feral, tagName);
      });
      TameElement.prototype.getElementsByClassName = nodeMethod(
          function(className) {
        return tameGetElementsByClassName(np(this).feral, className);
      });
      TameElement.prototype.getBoundingClientRect = nodeMethod(function () {
        var feral = np(this).feral;
        var elRect = bridal.getBoundingClientRect(feral);
        var vdoc = bridal.getBoundingClientRect(
            np(this.ownerDocument).feralContainerNode);
        var vdocLeft = vdoc.left, vdocTop = vdoc.top;
        return ({
                  top: elRect.top - vdocTop,
                  left: elRect.left - vdocLeft,
                  right: elRect.right - vdocLeft,
                  bottom: elRect.bottom - vdocTop
                });
      });
      TameElement.prototype.updateStyle = nodeMethod(function (style) {
        var feral = np(this).feral;
        np(this).policy.requireEditable();
        var cssPropertiesAndValues = cssSealerUnsealerPair.unseal(style);
        if (!cssPropertiesAndValues) { throw new Error(); }

        var styleNode = feral.style;
        for (var i = 0; i < cssPropertiesAndValues.length; i += 2) {
          var propName = cssPropertiesAndValues[+i];
          var propValue = cssPropertiesAndValues[i + 1];
          // If the propertyName differs between DOM and CSS, there will
          // be a semicolon between the two.
          // E.g., 'background-color;backgroundColor'
          // See CssTemplate.toPropertyValueList.
          var semi = propName.indexOf(';');
          if (semi >= 0) { propName = propName.substring(semi + 1); }
          styleNode[propName] = propValue;
        }
      });
      TameElement.prototype.addEventListener =
          nodeMethod(tameAddEventListener);
      TameElement.prototype.removeEventListener =
          nodeMethod(tameRemoveEventListener);
      function innerTextOf(rawNode, out) {
        switch (rawNode.nodeType) {
          case 1:  // Element
            if (htmlSchema.element(rawNode.tagName).allowed) {
              // Not an opaque node.
              for (var c = rawNode.firstChild; c; c = c.nextSibling) {
                c = makeDOMAccessible(c);
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
              c = makeDOMAccessible(c);
              innerTextOf(c, out);
            }
            break;
        }
      }
      (function() {
        var geometryDelegateProperty = {
          extendedAccessors: true,
          enumerable: true,
          get: nodeMethod(function (prop) {
            return np(this).geometryDelegate[prop];
          })
        };
        var geometryDelegatePropertySettable =
            Object.create(geometryDelegateProperty);
        geometryDelegatePropertySettable.set =
            nodeMethod(function (value, prop) {
          np(this).policy.requireEditable();
          np(this).geometryDelegate[prop] = +value;
        });
        definePropertiesAwesomely(TameElement.prototype, {
          clientLeft: geometryDelegateProperty,
          clientTop: geometryDelegateProperty,
          clientWidth: geometryDelegateProperty,
          clientHeight: geometryDelegateProperty,
          offsetLeft: geometryDelegateProperty,
          offsetTop: geometryDelegateProperty,
          offsetWidth: geometryDelegateProperty,
          offsetHeight: geometryDelegateProperty,
          scrollLeft: geometryDelegatePropertySettable,
          scrollTop: geometryDelegatePropertySettable,
          scrollWidth: geometryDelegateProperty,
          scrollHeight: geometryDelegateProperty
        });
      })();
      var textContentProp = {
        enumerable: true,
        get: nodeMethod(function () {
          var text = [];
          innerTextOf(np(this).feral, text);
          return text.join('');
        }),
        set: nodeMethod(function (newText) {
          // This operation changes the child node list (but not other
          // properties of the element) so it checks childrenEditable. Note that
          // this check is critical to security, as else a client can set the
          // textContent of a <script> element to execute scripts.
          np(this).policy.requireChildrenEditable();
          var newTextStr = newText != null ? String(newText) : '';
          var el = np(this).feral;
          for (var c; (c = el.firstChild);) { el.removeChild(c); }
          if (newTextStr) {
            el.appendChild(el.ownerDocument.createTextNode(newTextStr));
          }
        })
      };
      var tagNameAttr = {
        enumerable: true,
        get: nodeMethod(function () {
          return realToVirtualElementName(String(np(this).feral.tagName));
        })
      };
      definePropertiesAwesomely(TameElement.prototype, {
        id: NP.filterAttr(defaultToEmptyStr, identity),
        className: {
          enumerable: true,
          get: nodeMethod(function () {
            return this.getAttribute('class') || '';
          }),
          set: nodeMethod(function (classes) {
            return this.setAttribute('class', String(classes));
          })
        },
        title: NP.filterAttr(defaultToEmptyStr, String),
        dir: NP.filterAttr(defaultToEmptyStr, String),
        textContent: textContentProp,
        innerText: textContentProp,
        // Note: Per MDN, innerText is actually subtly different than
        // textContent, in that innerText does not include text hidden via
        // styles, per MDN. We do not implement this difference.
        nodeName: tagNameAttr,
        tagName: tagNameAttr,
        style: NP.filter(
            false,
            nodeMethod(function (styleNode) {
              TameStyle || buildTameStyle();
              return new TameStyle(styleNode, np(this).policy.editable, this);
            }),
            true, identity),
        innerHTML: {
          enumerable: true,
          get: nodeMethod(function () {
            return htmlFragmentSerialization(this);
          }),
          set: nodeMethod(function (htmlFragment) {
            // This operation changes the child node list (but not other
            // properties of the element) so it checks childrenEditable. Note
            // that this check is critical to security, as else a client can set
            // the innerHTML of a <script> element to execute scripts.
            np(this).policy.requireChildrenEditable();
            var node = np(this).feral;
            var schemaElem = htmlSchema.element(node.tagName);
            if (!schemaElem.allowed) {
              throw new Error("Can't set .innerHTML of non-whitelisted <" +
                  node.tagName + ">");
            }
            var isRCDATA = schemaElem.contentIsRCDATA;
            var htmlFragmentString;
            if (!isRCDATA && htmlFragment instanceof Html) {
              htmlFragmentString = '' + safeHtml(htmlFragment);
            } else if (htmlFragment === null) {
              htmlFragmentString = '';
            } else {
              htmlFragmentString = '' + htmlFragment;
            }
            var sanitizedHtml;
            if (isRCDATA) {
              sanitizedHtml = html.normalizeRCData(htmlFragmentString);
            } else {
              sanitizedHtml = sanitizeHtml(htmlFragmentString);
            }
            node.innerHTML = sanitizedHtml;
            return htmlFragment;
          })
        },
        offsetParent: {
          enumerable: true,
          get: cajaVM.def(function () {
            var feralOffsetParent = np(this).feral.offsetParent;
            if (!feralOffsetParent) {
              return feralOffsetParent;
            } else if (feralOffsetParent === containerNode) {
              // Return the body if the node is contained in the body. This is
              // emulating how browsers treat offsetParent and the real <BODY>.
              var feralBody = np(tameDocument.body).feral;
              for (var ancestor = makeDOMAccessible(np(this).feral.parentNode);
                   ancestor !== containerNode;
                   ancestor = makeDOMAccessible(ancestor.parentNode)) {
                if (ancestor === feralBody) {
                  return defaultTameNode(feralBody);
                }
              }
              return null;
            } else {
              return tameRelatedNode(feralOffsetParent, defaultTameNode);
            }
          })
        },
        accessKey: NP.rw,
        tabIndex: NP.rw
      });
      cajaVM.def(TameElement);  // and its prototype

      /**
       * Define a taming class for a subclass of HTMLElement.
       *
       * @param {Array} record.superclass The tame superclass constructor
       *     (defaults to TameElement) with parameters (this, node, policy,
       *     opt_proxyType).
       * @param {Array} record.names The element names which should be tamed
       *     using this class.
       * @param {String} record.domClass The DOM-specified class name.
       * @param {Object} record.properties The custom properties this class
       *     should have (in the format accepted by definePropertiesAwesomely).
       * @param {function} record.construct Code to invoke at the end of
       *     construction; takes and returns self.
       * @param {boolean} record.forceChildrenNotEditable Whether to force the
       *     child node list and child nodes to not be mutable.
       * @return {function} The constructor.
       */
      function defineElement(record) {
        var superclass = record.superclass || TameElement;
        var proxyType = record.proxyType;
        var construct = record.construct || identity;
        var shouldBeVirtualized = "virtualized" in record
            ? record.virtualized : false;
        var opt_policy = record.forceChildrenNotEditable
            ? nodePolicyReadOnlyChildren : null;
        function TameSpecificElement(node) {
          var isVirtualized = htmlSchema.isVirtualizedElementName(node.tagName);
          if (shouldBeVirtualized !== null &&
              !isVirtualized !== !shouldBeVirtualized) {
            throw new Error("Domado internal inconsistency: " + node.tagName +
                " has inconsistent virtualization state with class " +
                record.domClass);
          }
          superclass.call(this, node, opt_policy, proxyType);
          construct.call(this);
        }
        inertCtor(TameSpecificElement, superclass, record.domClass);
        definePropertiesAwesomely(TameSpecificElement.prototype,
            record.properties || {});
        // Note: cajaVM.def will be applied to all registered node classes
        // later, so users of defineElement don't need to.
        return TameSpecificElement;
      }
      cajaVM.def(defineElement);
      
      /**
       * For elements which have no properties at all, but we want to define in
       * in order to be explicitly complete (suppress the no-implementation
       * warning).
       */
      function defineTrivialElement(domClass) {
        return defineElement({domClass: domClass});
      }

      defineElement({
        domClass: 'HTMLAnchorElement',
        properties: {
          hash: NP.filter(
            false,
            function (value) { return unsuffix(value, idSuffix, value); },
            false,
            // TODO(felix8a): add suffix if href is self
            identity),
          // TODO(felix8a): fragment rewriting?
          href: NP.filter(false, identity, true, identity)
        }
      });

      defineTrivialElement('HTMLBRElement');

      var TameBodyElement = defineElement({
        virtualized: true,
        domClass: 'HTMLBodyElement'
      });
      setOwn(TameBodyElement.prototype, 'setAttribute', nodeMethod(
          function (attrib, value) {
        TameElement.prototype.setAttribute.call(this, attrib, value);
        var attribName = String(attrib).toLowerCase();
        // Window event handlers are exposed as content attributes on <body>
        // and <frameset>
        // <http://www.whatwg.org/specs/web-apps/current-work/multipage/webappapis.html#handler-window-onload>
        // as of 2012-09-14
        // Note: We only currently implement onload.
        if (attribName === 'onload') {
          // We do not use the main event-handler-attribute rewriter here
          // because it generates event-handler strings, not functions -- and 
          // for the TameWindow there is no real element to hang those handler
          // strings on. TODO(kpreid): refactor to fix that.
          if (cajaVM.compileExpr) { // ES5 case: eval available
            // Per http://www.whatwg.org/specs/web-apps/current-work/multipage/webappapis.html#event-handler-attributes
            tameWindow[attribName] = cajaVM.compileExpr(
                'function cajaEventHandlerAttribFn_' + attribName +
                '(event) {\n' + value + '\n}')(tameWindow);
          } else {
            var match = value.match(SIMPLE_HANDLER_PATTERN);
            if (!match) { return; }
            //var doesReturn = match[1];  // not currently used
            var fnName = match[2];
            // TODO(kpreid): Synthesize a load event object.
            tameWindow[attribName] =
                function () { tameWindow[fnName].call(this, {}, this); };
          }
        }
      }));

      // http://dev.w3.org/html5/spec/Overview.html#the-canvas-element
      (function() {
        // If the host browser does not have getContext, then it must not
        // usefully
        // support canvas, so we don't either; skip registering the canvas
        // element
        // class.
        // TODO(felix8a): need to call bridal.initCanvasElement
        var e = makeDOMAccessible(document.createElement('canvas'));
        if (typeof e.getContext !== 'function')
          return;

        // TODO(kpreid): snitched from Caja runtime; review whether we actually
        // need this (the Canvas spec says that invalid values should be ignored
        // and we don't do that in a bunch of places);
        /**
         * Enforces <tt>typeOf(specimen) === typename</tt>, in which case
         * specimen is returned.
         * <p>
         * If not, throws an informative TypeError
         * <p>
         * opt_name, if provided, should be a name or description of the
         * specimen used only to generate friendlier error messages.
         */
        function enforceType(specimen, typename, opt_name) {
          if (typeof specimen !== typename) {
            throw new Error('expected ', typename, ' instead of ',
                typeof specimen, ': ', (opt_name || specimen));
          }
          return specimen;
        }

        var TameContext2DConf = new Confidence('TameContext2D');
        var ContextP = new PropertyTaming(TameContext2DConf);

        function matchesStyleFully(cssPropertyName, value) {
          if (typeof value !== "string") { return false; }
          var tokens = lexCss(value);
          var k = 0;
          for (var i = 0, n = tokens.length; i < n; ++i) {
            var tok = tokens[i];
            if (tok !== ' ') { tokens[k++] = tok; }
          }
          tokens.length = k;
          // sanitizeCssProperty always lowercases
          var unfiltered = tokens.join(' ').toLowerCase();
          sanitizeCssProperty(cssPropertyName,
                              cssSchema[cssPropertyName], tokens);
          return unfiltered === tokens.join(' ') ? unfiltered : false;
        }

        function isFont(value) {
          return !!matchesStyleFully('font', value);
        }
        function isColor(value) {
          // Note: we're testing against the pattern for the CSS "color:"
          // property, but what is actually referenced by the draft canvas spec
          // is
          // the CSS syntactic element <color>, which is why we need to
          // specifically exclude "inherit".
          var style = matchesStyleFully('color', value);
          return style && style.toLowerCase() !== 'inherit';
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
          for (var i = strings.length; --i >= 0;) {
            table[strings[+i]] = table;
          }
          return cajaVM.def(function (string) {
            return typeof string === 'string' && table[string] === table;
          });
        }
        function canonColor(colorString) {
          // http://dev.w3.org/html5/2dcontext/ says the color shall be returned
          // only as #hhhhhh, not as names.
          return colorNameTable[" " + colorString] || colorString;
        }
        function TameImageData(imageData) {
          imageData = makeDOMAccessible(imageData);
          var p = TameImageDataConf.p;

          // Since we can't interpose indexing, we can't wrap the
          // CanvasPixelArray
          // so we have to copy the pixel data. This is horrible, bad, and
          // awful.
          // TODO(kpreid): No longer true in ES5-land; we can interpose but not
          // under ES5/3. Use proxies conditional on the same switch that
          // controls
          // liveness of node lists.
          var tameImageData = {
            toString: cajaVM.def(function () {
                return "[Domita Canvas ImageData]"; }),
            width: Number(imageData.width),
            height: Number(imageData.height)
          };
          TameImageDataConf.confide(tameImageData);
          taming.permitUntaming(tameImageData);

          // used to unwrap for passing to putImageData
          p(tameImageData).feral = imageData;

          // lazily constructed tame copy, backs .data accessor; also used to
          // test whether we need to write-back the copy before a putImageData
          p(tameImageData).tamePixelArray = undefined;

          definePropertiesAwesomely(tameImageData, {
            data: {
              enumerable: true,
              // Accessor used so we don't need to copy if the client is just
              // blitting (getImageData -> putImageData) rather than inspecting
              // the pixels.
              get: cajaVM.def(function () {
                if (!p(tameImageData).tamePixelArray) {

                  var bareArray = imageData.data;
                  // Note: On Firefox 4.0.1, at least, pixel arrays cannot have
                  // added properties (such as our w___). Therefore, for
                  // writing,
                  // we use a special routine, and we don't do
                  // makeDOMAccessible
                  // because it would have no effect. An alternative approach
                  // would be to muck with the "Uint8ClampedArray" prototype.

                  var length = bareArray.length;
                  var tamePixelArray = { // not frozen, user-modifiable
                    // TODO: Investigate whether it would be an optimization to
                    // make this an array with properties added.
                    toString: cajaVM.def(function () {
                        return "[Domita CanvasPixelArray]"; }),
                    _d_canvas_writeback: function () {
                      // This is invoked just before each putImageData

                      // TODO(kpreid): shouldn't be a public method (but is
                      // harmless).

                      rulebreaker.writeToPixelArray(
                        tamePixelArray, bareArray, length);
                    }
                  };
                  for (var i = length-1; i >= 0; i--) {
                    tamePixelArray[+i] = bareArray[+i];
                  }
                  p(tameImageData).tamePixelArray = tamePixelArray;
                }
                return p(tameImageData).tamePixelArray;
              })
            }
          });
          return Object.freeze(tameImageData);
        }
        function TameGradient(gradient) {
          gradient = makeDOMAccessible(gradient);
          var tameGradient = {
            toString: cajaVM.def(function () {
                return "[Domita CanvasGradient]"; }),
            addColorStop: cajaVM.def(function (offset, color) {
              enforceType(offset, 'number', 'color stop offset');
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
          TameGradientConf.confide(tameGradient);
          TameGradientConf.p(tameGradient).feral = gradient;
          taming.tamesTo(gradient, tameGradient);
          return Object.freeze(tameGradient);
        }
        function enforceFinite(value, name) {
          enforceType(value, 'number', name);
          if (!isFinite(value)) {
            throw new Error("NOT_SUPPORTED_ERR");
            // TODO(kpreid): should be a DOMException per spec
          }
        }

        function TameCanvasElement(node) {
          // TODO(kpreid): review whether this can use defineElement
          TameElement.call(this, node);

          // helpers for tame context
          var context = makeDOMAccessible(node.getContext('2d'));
          function tameFloatsOp(count, verb) {
            var m = makeFunctionAccessible(context[verb]);
            return cajaVM.def(function () {
              if (arguments.length !== count) {
                throw new Error(verb + ' takes ' + count + ' args, not ' +
                                arguments.length);
              }
              for (var i = 0; i < count; i++) {
                enforceType(arguments[+i], 'number', verb + ' argument ' + i);
              }
              // The copy-into-array is necessary in ES5/3 because host DOM
              // won't take an arguments object from inside of ES53.
              m.apply(context, Array.prototype.slice.call(arguments));
            });
          }
          function tameRectMethod(m, hasResult) {
            makeFunctionAccessible(m);
            return cajaVM.def(function (x, y, w, h) {
              if (arguments.length !== 4) {
                throw new Error(m + ' takes 4 args, not ' +
                                arguments.length);
              }
              enforceType(x, 'number', 'x');
              enforceType(y, 'number', 'y');
              enforceType(w, 'number', 'width');
              enforceType(h, 'number', 'height');
              if (hasResult) {
                return m.call(context, x, y, w, h);
              } else {
                m.call(context, x, y, w, h);
              }
            });
          }
          function tameDrawText(m) {
            makeFunctionAccessible(m);
            return cajaVM.def(function (text, x, y, maxWidth) {
              enforceType(text, 'string', 'text');
              enforceType(x, 'number', 'x');
              enforceType(y, 'number', 'y');
              switch (arguments.length) {
              case 3:
                m.apply(context, Array.prototype.slice.call(arguments));
                return;
              case 4:
                enforceType(maxWidth, 'number', 'maxWidth');
                m.apply(context, Array.prototype.slice.call(arguments));
                return;
              default:
                throw new Error(m + ' cannot accept ' + arguments.length +
                                    ' arguments');
              }
            });
          }
          function tameGetMethod(prop) {
            return cajaVM.def(function () { return context[prop]; });
          }
          function tameSetMethod(prop, validator) {
            return cajaVM.def(function (newValue) {
              if (validator(newValue)) {
                context[prop] = newValue;
              }
              return newValue;
            });
          }
          var CP_STYLE = {
            enumerable: true,
            extendedAccessors: true,
            get: TameContext2DConf.protectMethod(function (prop) {
              var value = context[prop];
              if (typeof(value) == "string") {
                return canonColor(value);
              } else if (cajaVM.passesGuard(TameGradientT,
                                            taming.tame(value))) {
                return taming.tame(value);
              } else {
                throw new Error("Internal: Can't tame value " + value + " of " +
                     prop);
              }
            }),
            set: cajaVM.def(function (newValue, prop) {
              if (isColor(newValue)) {
                context[prop] = newValue;
              } else if (typeof(newValue) === "object" &&
                         cajaVM.passesGuard(TameGradientT, newValue)) {
                context[prop] = TameGradientConf.p(newValue).feral;
              } // else do nothing
              return newValue;
            })
          };
          function tameSimpleOp(m) {  // no return value
            makeFunctionAccessible(m);
            return cajaVM.def(function () {
              if (arguments.length !== 0) {
                throw new Error(m + ' takes no args, not ' + arguments.length);
              }
              m.call(context);
            });
          }

          // Design note: We generally reject the wrong number of arguments,
          // unlike default JS behavior. This is because we are just passing
          // data
          // through to the underlying implementation, but we don't want to pass
          // on anything which might be an extension we don't know about, and it
          // is better to fail explicitly than to leave the client wondering
          // about
          // why their extension usage isn't working.

          // http://dev.w3.org/html5/2dcontext/
          // TODO(kpreid): Review this for converting to prototypical objects
          var tameContext2d = np(this).tameContext2d = {
            toString: cajaVM.def(function () {
                return "[Domita CanvasRenderingContext2D]"; }),

            save: tameSimpleOp(context.save),
            restore: tameSimpleOp(context.restore),

            scale: tameFloatsOp(2, 'scale'),
            rotate: tameFloatsOp(1, 'rotate'),
            translate: tameFloatsOp(2, 'translate'),
            transform: tameFloatsOp(6, 'transform'),
            setTransform: tameFloatsOp(6, 'setTransform'),

            createLinearGradient: function (x0, y0, x1, y1) {
              if (arguments.length !== 4) {
                throw new Error('createLinearGradient takes 4 args, not ' +
                                arguments.length);
              }
              enforceType(x0, 'number', 'x0');
              enforceType(y0, 'number', 'y0');
              enforceType(x1, 'number', 'x1');
              enforceType(y1, 'number', 'y1');
              return new TameGradient(
                context.createLinearGradient(x0, y0, x1, y1));
            },
            createRadialGradient: function (x0, y0, r0, x1, y1, r1) {
              if (arguments.length !== 6) {
                throw new Error('createRadialGradient takes 6 args, not ' +
                                arguments.length);
              }
              enforceType(x0, 'number', 'x0');
              enforceType(y0, 'number', 'y0');
              enforceType(r0, 'number', 'r0');
              enforceType(x1, 'number', 'x1');
              enforceType(y1, 'number', 'y1');
              enforceType(r1, 'number', 'r1');
              return new TameGradient(context.createRadialGradient(
                x0, y0, r0, x1, y1, r1));
            },

            createPattern: function (imageElement, repetition) {
              // Consider what policy to have wrt reading the pixels from image
              // elements before implementing this.
              throw new Error(
                  'Domita: canvas createPattern not yet implemented');
            },

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
            arc: function (x, y, radius, startAngle, endAngle, anticlockwise) {
              if (arguments.length !== 6) {
                throw new Error('arc takes 6 args, not ' + arguments.length);
              }
              enforceType(x, 'number', 'x');
              enforceType(y, 'number', 'y');
              enforceType(radius, 'number', 'radius');
              enforceType(startAngle, 'number', 'startAngle');
              enforceType(endAngle, 'number', 'endAngle');
              enforceType(anticlockwise, 'boolean', 'anticlockwise');
              if (radius < 0) {
                throw new Error(INDEX_SIZE_ERROR);
                // TODO(kpreid): should be a DOMException per spec
              }
              context.arc(x, y, radius, startAngle, endAngle, anticlockwise);
            },
            fill: tameSimpleOp(context.fill),
            stroke: tameSimpleOp(context.stroke),
            clip: tameSimpleOp(context.clip),

            isPointInPath: function (x, y) {
              enforceType(x, 'number', 'x');
              enforceType(y, 'number', 'y');
              return enforceType(context.isPointInPath(x, y), 'boolean');
            },

            fillText: tameDrawText(context.fillText),
            strokeText: tameDrawText(context.strokeText),
            measureText: function (string) {
              if (arguments.length !== 1) {
                throw new Error('measureText takes 1 arg, not ' +
                    arguments.length);
              }
              enforceType(string, 'string', 'measureText argument');
              return context.measureText(string);
            },

            drawImage: function (imageElement) {
              // Consider what policy to have wrt reading the pixels from image
              // elements before implementing this.
              throw new Error('Domita: canvas drawImage not yet implemented');
            },

            createImageData: function (sw, sh) {
              if (arguments.length !== 2) {
                throw new Error('createImageData takes 2 args, not ' +
                                arguments.length);
              }
              enforceType(sw, 'number', 'sw');
              enforceType(sh, 'number', 'sh');
              return new TameImageData(context.createImageData(sw, sh));
            },
            getImageData: tameRectMethod(function (sx, sy, sw, sh) {
              return TameImageData(context.getImageData(sx, sy, sw, sh));
            }, true),
            putImageData: function
                (tameImageData, dx, dy, dirtyX, dirtyY,
                    dirtyWidth, dirtyHeight) {
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
              var tamePixelArray =
                TameImageDataConf.p(tameImageData).tamePixelArray;
              if (tamePixelArray) {
                tamePixelArray._d_canvas_writeback();
              }
              context.putImageData(TameImageDataConf.p(tameImageData).feral,
                                   dx, dy, dirtyX, dirtyY,
                                   dirtyWidth, dirtyHeight);
            }
          };

          if ("drawFocusRing" in context) {
            tameContext2d.drawFocusRing = function
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
              enforceType(x, 'number', 'x');
              enforceType(y, 'number', 'y');
              enforceType(canDrawCustom, 'boolean', 'canDrawCustom');

              // On safety of using the untamed node here: The only information
              // drawFocusRing takes from the node is whether it is focused.
              return enforceType(
                  context.drawFocusRing(np(tameElement).feral, x, y,
                                        canDrawCustom),
                  'boolean');
            };
          }

          definePropertiesAwesomely(tameContext2d, {
            // We filter the values supplied to setters in case some browser
            // extension makes them more powerful, e.g. containing scripting or
            // a URL.
            // TODO(kpreid): Do we want to filter the *getters* as well?
            // Scenarios: (a) canvas shared with innocent code, (b) browser
            // quirks?? If we do, then what should be done with a bad value?
            globalAlpha: ContextP.RWCond(
                function (v) { return typeof v === "number" &&
                                      0.0 <= v && v <= 1.0;     }),
            globalCompositeOperation: ContextP.RWCond(
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
                ])),
            strokeStyle: CP_STYLE,
            fillStyle: CP_STYLE,
            lineWidth: ContextP.RWCond(
                function (v) { return typeof v === "number" &&
                                      0.0 < v && v !== Infinity; }),
            lineCap: ContextP.RWCond(
                StringTest([
                  "butt",
                  "round",
                  "square"
                ])),
            lineJoin: ContextP.RWCond(
                StringTest([
                  "bevel",
                  "round",
                  "miter"
                ])),
            miterLimit: ContextP.RWCond(
                  function (v) { return typeof v === "number" &&
                                        0 < v && v !== Infinity; }),
            shadowOffsetX: ContextP.RWCond(
                  function (v) {
                    return typeof v === "number" && isFinite(v); }),
            shadowOffsetY: ContextP.RWCond(
                  function (v) {
                    return typeof v === "number" && isFinite(v); }),
            shadowBlur: ContextP.RWCond(
                  function (v) { return typeof v === "number" &&
                                        0.0 <= v && v !== Infinity; }),
            shadowColor: {
              enumerable: true,
              extendedAccessors: true,
              get: CP_STYLE.get,
              set: ContextP.RWCond(isColor).set
            },

            font: ContextP.RWCond(isFont),
            textAlign: ContextP.RWCond(
                StringTest([
                  "start",
                  "end",
                  "left",
                  "right",
                  "center"
                ])),
            textBaseline: ContextP.RWCond(
                StringTest([
                  "top",
                  "hanging",
                  "middle",
                  "alphabetic",
                  "ideographic",
                  "bottom"
                ]))
          });
          TameContext2DConf.confide(tameContext2d);
          TameContext2DConf.p(tameContext2d).policy = np(this).policy;
          TameContext2DConf.p(tameContext2d).feral = context;
          cajaVM.def(tameContext2d);
          taming.permitUntaming(tameContext2d);
        }  // end of TameCanvasElement
        inertCtor(TameCanvasElement, TameElement, 'HTMLCanvasElement');
        TameCanvasElement.prototype.getContext = function (contextId) {

          // TODO(kpreid): We can refine this by inventing a ReadOnlyCanvas
          // object to return in this situation, which allows getImageData and
          // so on but not any drawing. Not bothering to do that for now; if
          // you have a use for it let us know.
          np(this).policy.requireEditable();

          enforceType(contextId, 'string', 'contextId');
          switch (contextId) {
            case '2d':
              // TODO(kpreid): Need to be lazy once we support other context
              // types.
              return np(this).tameContext2d;
            default:
              // http://dev.w3.org/html5/spec/the-canvas-element.html#the-canvas-element
              // "If contextId is not the name of a context supported by the
              // user agent, return null and abort these steps."
              return null;
          }
        };
        definePropertiesAwesomely(TameCanvasElement.prototype, {
          height: NP.filter(false, identity, false, Number),
          width: NP.filter(false, identity, false, Number)
        });
      })();

      defineTrivialElement('HTMLDListElement');
      defineTrivialElement('HTMLDivElement');

      function FormElementAndExpandoProxyHandler(target, editable, storage) {
        ExpandoProxyHandler.call(this, target, editable, storage);
      }
      inherit(FormElementAndExpandoProxyHandler, ExpandoProxyHandler);
      setOwn(FormElementAndExpandoProxyHandler.prototype,
          'getOwnPropertyDescriptor', function (name) {
        if (name !== 'ident___' &&
            Object.prototype.hasOwnProperty.call(this.target.elements, name)) {
          return Object.getOwnPropertyDescriptor(this.target.elements, name);
        } else {
          return ExpandoProxyHandler.prototype.getOwnPropertyDescriptor
              .call(this, name);
        }
      });
      setOwn(FormElementAndExpandoProxyHandler.prototype,
          'get', domitaModules.permuteProxyGetSet.getter(function (name) {
        if (name !== 'ident___' &&
            Object.prototype.hasOwnProperty.call(this.target.elements, name)) {
          return this.target.elements[name];
        } else {
          return ExpandoProxyHandler.prototype.get.unpermuted.call(this, name);
        }
      }));
      setOwn(FormElementAndExpandoProxyHandler.prototype, 'getOwnPropertyNames',
          function () {
        // TODO(kpreid): not quite right result set
        return Object.getOwnPropertyNames(this.target.elements);
      });
      setOwn(FormElementAndExpandoProxyHandler.prototype, 'delete',
          function (name) {
        if (name === "ident___") {
          return false;
        } else if (Object.prototype.hasOwnProperty.call(
                       this.target.elements, name)) {
          return false;
        } else {
          return ExpandoProxyHandler.prototype['delete'].call(this, name);
        }
      });
      cajaVM.def(FormElementAndExpandoProxyHandler);

      var TameFormElement = defineElement({
        domClass: 'HTMLFormElement',
        proxyType: FormElementAndExpandoProxyHandler,
        properties: {
          action: NP.filterAttr(defaultToEmptyStr, String),
          elements: {
            enumerable: true,
            get: nodeMethod(function () {
              return tameHTMLCollection(
                  np(this).feral.elements, defaultTameNode);
            })
          },
          enctype: NP.filterAttr(defaultToEmptyStr, String),
          method: NP.filterAttr(defaultToEmptyStr, String),
          target: NP.filterAttr(defaultToEmptyStr, String)
        },
        construct: function () {
          // Freeze length at creation time since we aren't live.
          // TODO(kpreid): Revise this when we have live node lists.
          Object.defineProperty(this, "length", {
            value: np(this).feral.length
          });
        }
      });
      // TODO(felix8a): need to test handlingUserAction.
      TameFormElement.prototype.submit = nodeMethod(function () {
        np(this).policy.requireEditable();
        return domicile.handlingUserAction && np(this).feral.submit();
      });
      TameFormElement.prototype.reset = nodeMethod(function () {
        np(this).policy.requireEditable();
        return np(this).feral.reset();
      });

      defineTrivialElement('HTMLHeadingElement');
      defineTrivialElement('HTMLHRElement');

      defineElement({
        virtualized: true,
        domClass: 'HTMLHeadElement'
      });

      defineElement({
        virtualized: true,
        domClass: 'HTMLHtmlElement'
      });

      var TameIFrameElement = defineElement({
        domClass: 'HTMLIFrameElement',
        construct: function () {
          np(this).childrenEditable = false;
        },
        properties: {
          align: {
            enumerable: true,
            get: nodeMethod(function () {
              return np(this).feral.align;
            }),
            set: nodeMethod(function (alignment) {
              np(this).policy.requireEditable();
              alignment = String(alignment);
              if (alignment === 'left' ||
                  alignment === 'right' ||
                  alignment === 'center') {
                np(this).feral.align = alignment;
              }
            })
          },
          frameBorder: {
            enumerable: true,
            get: nodeMethod(function () {
              return np(this).feral.frameBorder;
            }),
            set: nodeMethod(function (border) {
              np(this).policy.requireEditable();
              border = String(border).toLowerCase();
              if (border === '0' || border === '1' ||
                  border === 'no' || border === 'yes') {
                np(this).feral.frameBorder = border;
              }
            })
          },
          height: NP.filterProp(identity, Number),
          width:  NP.filterProp(identity, Number),
          src: NP.filterAttr(identity, identity), // rewrite handled for attr
          name: NP.filterAttr(identity, identity), // rejection handled for attr
          contentDocument: {
            enumerable: true,
            get: cajaVM.def(function () {
              return contentDomicile(this).document;
            })
          },
          contentWindow: {
            enumerable: true,
            get: cajaVM.def(function () {
              return contentDomicile(this).window;
            })
          }
        }
      });
      function contentDomicile(tameIFrame) {
        // TODO(kpreid): Once we support loading content via src=, we will need
        // to consider whether this should always allow access to said content,
        // and probably other issues.
        var privates = np(tameIFrame);
        var frameFeralDoc = makeDOMAccessible(privates.feral.contentDocument);
        if (!privates.contentDomicile ||
            frameFeralDoc !== privates.seenContentDocument) {
          if (!frameFeralDoc) {
            return {document: null, window: null};
          }

          var subDomicile = privates.contentDomicile = attachDocument(
              '-caja-iframe___', naiveUriPolicy, frameFeralDoc,
              optTargetAttributePresets, taming);
          privates.seenContentDocument = frameFeralDoc;

          // Replace document structure with virtualized forms
          // TODO(kpreid): Use an alternate HTML schema (requires refactoring)
          // which makes <html> <head> <body> permitted (in particular,
          // non-opaque) so that this is unnecessary.
          var child;
          while ((child = makeDOMAccessible(frameFeralDoc.lastChild))) {
            frameFeralDoc.removeChild(child);
          }
          var tdoc = subDomicile.document;
          var thtml = tdoc.createElement('html');
          thtml.appendChild(tdoc.createElement('head'));
          thtml.appendChild(tdoc.createElement('body'));
          frameFeralDoc.appendChild(subDomicile.feralNode(thtml));
              // cannot do this via pseudo-node

          void HtmlEmitter; // placeholder
          var emitter = new HtmlEmitter(
              makeDOMAccessible, subDomicile.htmlEmitterTarget, subDomicile);
          emitter.finish();
        }
        return privates.contentDomicile;
      }

      var TameImageElement = defineElement({
        domClass: 'HTMLImageElement',
        properties: {
          alt: NP.filterProp(String, String),
          height: NP.filterProp(Number, Number),
          src: NP.filter(false, String, true, identity),
          width: NP.filterProp(Number, Number)
        }
      });
      var featureTestImage = makeDOMAccessible(document.createElement('img'));
      if ("naturalWidth" in featureTestImage) {
        definePropertiesAwesomely(TameImageElement.prototype, {
          naturalHeight: NP.filterProp(Number, Number),
          naturalWidth: NP.filterProp(Number, Number)
        });
      }
      if ("complete" in featureTestImage) {
        definePropertiesAwesomely(TameImageElement.prototype, {
          complete: NP.filterProp(Boolean, Boolean)
        });
      }

      function toInt(x) { return x | 0; }
      var TameFormField = defineElement({
        properties: {
          autofocus: NP.ro,
          disabled: NP.rw,
          form: NP.related,
          maxLength: NP.rw,
          name: NP.rw,
          value: NP.filter(
            false, function (x) { return x == null ? null : String(x); },
            false, function (x) { return x == null ? '' : '' + x; })
        }
      });
      
      var TameInputElement = defineElement({
        superclass: TameFormField,
        domClass: 'HTMLInputElement',
        properties: {
          checked: NP.filterProp(identity, Boolean),
          defaultChecked: NP.rw,
          defaultValue: NP.filter(
            false, function (x) { return x == null ? null : String(x); },
            false, function (x) { return x == null ? '' : '' + x; }),
          readOnly: NP.rw,
          selectedIndex: NP.filterProp(identity, toInt),
          size: NP.rw,
          type: NP.rw
        }
      });
      TameInputElement.prototype.select = nodeMethod(function () {
        np(this).feral.select();
      });

      defineElement({
        superclass: TameFormField,
        domClass: 'HTMLButtonElement',
        properties: {
          type: NP.rw
        }
      });

      defineElement({
        superclass: TameFormField,
        domClass: 'HTMLSelectElement',
        properties: {
          multiple: NP.rw,
          options: {
            enumerable: true,
            get: nodeMethod(function () {
              return new TameOptionsList(
                  np(this).feral.options, defaultTameNode, 'name');
            })
          },
          selectedIndex: NP.filterProp(identity, toInt),
          type: NP.ro
        }
      });

      defineElement({
        superclass: TameFormField,
        domClass: 'HTMLTextAreaElement',
        properties: {
          type: NP.rw
        }
      });

      defineElement({
        domClass: 'HTMLLabelElement',
        properties: {
          htmlFor: NP.Rename("for", NP.filterAttr(identity, identity))
        }
      });

      defineElement({
        domClass: 'HTMLOptionElement',
        properties: {
          defaultSelected: NP.filterProp(Boolean, Boolean),
          disabled: NP.filterProp(Boolean, Boolean),
          form: NP.related,
          index: NP.filterProp(Number),
          label: NP.filterProp(String, String),
          selected: NP.filterProp(Boolean, Boolean),
          text: NP.filterProp(String, String),
          // TODO(kpreid): Justify these specialized filters.
          value: NP.filterProp(
            function (x) { return x == null ? null : String(x); },
            function (x) { return x == null ? '' : '' + x; })
        }
      });
      
      defineTrivialElement('HTMLParagraphElement');
      defineTrivialElement('HTMLPreElement');

      function dynamicCodeDispatchMaker(privates) {
        window.cajaDynamicScriptCounter =
          window.cajaDynamicScriptCounter ?
            window.cajaDynamicScriptCounter + 1 : 0;
        var name = "caja_dynamic_script" +
          window.cajaDynamicScriptCounter + '___';
        window[name] = function() {
          try {
            if (privates.src &&
              'function' === typeof domicile.evaluateUntrustedExternalScript) {
              domicile.evaluateUntrustedExternalScript(privates.src);
            }
          } finally {
            window[name] = undefined;
          }
        };
        return name + "();";
      }

      var TameScriptElement = defineElement({
        domClass: 'HTMLScriptElement',
        forceChildrenNotEditable: true,
        properties: {
          src: NP.filter(false, identity, true, identity)
        },
        construct: function () {
          var privates = np(this);
          privates.feral.appendChild(
            document.createTextNode(
              dynamicCodeDispatchMaker(privates)));
        }
      });

      setOwn(TameScriptElement.prototype, 'setAttribute', nodeMethod(
          function (attrib, value) {
        var feral = np(this).feral;
        np(this).policy.requireEditable();
        TameElement.prototype.setAttribute.call(this, attrib, value);
        var attribName = String(attrib).toLowerCase();
        if ("src" === attribName) {
          np(this).src = String(value);
        }
      }));

      defineTrivialElement('HTMLSpanElement');

      defineElement({
        domClass: 'HTMLTableColElement',
        properties: {
          align: NP.filterProp(identity, identity),
          vAlign: NP.filterProp(identity, identity)
        }
      });
      
      defineTrivialElement('HTMLTableCaptionElement');
      
      var TameTableCellElement = defineElement({
        domClass: 'HTMLTableCellElement',
        properties: {
          colSpan: NP.filterProp(identity, identity),
          rowSpan: NP.filterProp(identity, identity),
          cellIndex: NP.ro,
          noWrap: NP.filterProp(identity, identity) // HTML5 Obsolete
        }
      });
      defineElement({
        superclass: TameTableCellElement,
        domClass: 'HTMLTableDataCellElement'
      });
      defineElement({
        superclass: TameTableCellElement,
        domClass: 'HTMLTableHeaderCellElement'
      });

      function requireIntIn(idx, min, max) {
        if (idx !== (idx | 0) || idx < min || idx > max) {
          throw new Error(INDEX_SIZE_ERROR);
        }
      }

      var TameTableRowElement = defineElement({
        domClass: 'HTMLTableRowElement',
        properties: {
          cells: {
            // TODO(kpreid): It would be most pleasing to find a way to generalize
            // all the accessors which are of the form
            //     return new TameNodeList(np(this).feral...., ..., ...)
            enumerable: true,
            get: nodeMethod(function () {
              return new TameNodeList(np(this).feral.cells, defaultTameNode);
            })
          },
          rowIndex: NP.ro,
          sectionRowIndex: NP.ro
        }
      });
      TameTableRowElement.prototype.insertCell = nodeMethod(function (index) {
        np(this).policy.requireEditable();
        requireIntIn(index, -1, np(this).feral.cells.length);
        return defaultTameNode(
            np(this).feral.insertCell(index),
            np(this).editable);
      });
      TameTableRowElement.prototype.deleteCell = nodeMethod(function (index) {
        np(this).policy.requireEditable();
        requireIntIn(index, -1, np(this).feral.cells.length);
        np(this).feral.deleteCell(index);
      });

      var TameTableSectionElement = defineElement({
        domClass: 'HTMLTableSectionElement',
        properties: {
          rows: {
            enumerable: true,
            get: nodeMethod(function () {
              return new TameNodeList(np(this).feral.rows, defaultTameNode);
            })
          }
        }
      });
      TameTableSectionElement.prototype.insertRow = nodeMethod(function(index) {
        np(this).policy.requireEditable();
        requireIntIn(index, -1, np(this).feral.rows.length);
        return defaultTameNode(np(this).feral.insertRow(index));
      });
      TameTableSectionElement.prototype.deleteRow = nodeMethod(function(index) {
        np(this).policy.requireEditable();
        requireIntIn(index, -1, np(this).feral.rows.length);
        np(this).feral.deleteRow(index);
      });

      var TameTableElement = defineElement({
        superclass: TameTableSectionElement,  // nonstandard but sound
        domClass: 'HTMLTableElement',
        properties: {
          tBodies: {
            enumerable: true,
            get: nodeMethod(function () {
              if (np(this).policy.childrenVisible) {
                return new TameNodeList(np(this).feral.tBodies,
                    defaultTameNode);
              } else {
                return fakeNodeList([]);
              }
            })
          },
          tHead: NP_tameDescendant,
          tFoot: NP_tameDescendant,
          cellPadding: NP.filterAttr(Number, fromInt),
          cellSpacing: NP.filterAttr(Number, fromInt),
          border:      NP.filterAttr(Number, fromInt)
        }
      });
      TameTableElement.prototype.createTHead = nodeMethod(function () {
        np(this).policy.requireEditable();
        return defaultTameNode(np(this).feral.createTHead());
      });
      TameTableElement.prototype.deleteTHead = nodeMethod(function () {
        np(this).policy.requireEditable();
        np(this).feral.deleteTHead();
      });
      TameTableElement.prototype.createTFoot = nodeMethod(function () {
        np(this).policy.requireEditable();
        return defaultTameNode(np(this).feral.createTFoot());
      });
      TameTableElement.prototype.deleteTFoot = nodeMethod(function () {
        np(this).policy.requireEditable();
        np(this).feral.deleteTFoot();
      });
      TameTableElement.prototype.createCaption = nodeMethod(function () {
        np(this).policy.requireEditable();
        return defaultTameNode(np(this).feral.createCaption());
      });
      TameTableElement.prototype.deleteCaption = nodeMethod(function () {
        np(this).policy.requireEditable();
        np(this).feral.deleteCaption();
      });
      TameTableElement.prototype.insertRow = nodeMethod(function (index) {
        np(this).policy.requireEditable();
        requireIntIn(index, -1, np(this).feral.rows.length);
        return defaultTameNode(np(this).feral.insertRow(index));
      });
      TameTableElement.prototype.deleteRow = nodeMethod(function (index) {
        np(this).policy.requireEditable();
        requireIntIn(index, -1, np(this).feral.rows.length);
        np(this).feral.deleteRow(index);
      });

      defineElement({
        virtualized: true,
        domClass: 'HTMLTitleElement'
      });
      
      defineTrivialElement('HTMLUListElement');

      defineElement({
        virtualized: null,
        domClass: 'HTMLUnknownElement'
      });
      
      // We are now done with all of the specialized element taming classes.

      // Oddball constructors. There are only two of these and we implement
      // both. (Caveat: In actual browsers, new Image().constructor == Image
      // != HTMLImageElement. We don't implement that.)
      // TODO(kpreid): There are more oddball constructors in HTML5, e.g. Audio,
      // and a notation for it in HTML5's WebIDL. Generalize this to support
      // that.
      
      // Per https://developer.mozilla.org/en-US/docs/DOM/Image as of 2012-09-24
      function TameImageFun(width, height) {
        var element = tameDocument.createElement('img');
        if (width !== undefined) { element.width  = width; }
        if (height !== undefined) { element.height = height; }
        return element;
      }
      cajaVM.def(TameImageFun);
      
      // Per https://developer.mozilla.org/en-US/docs/DOM/Option
      // as of 2012-09-24
      function TameOptionFun(text, value, defaultSelected, selected) {
        var element = tameDocument.createElement('option');
        if (text !== undefined) { element.text = text; }
        if (value !== undefined) { element.value = value; }
        if (defaultSelected !== undefined) {
          element.defaultSelected = defaultSelected;
        }
        if (selected !== undefined) { element.selected = selected; }
        return element;
      }
      cajaVM.def(TameOptionFun);

      // Taming of Events:

      // coerce null and false to 0
      function fromInt(x) { return '' + (x | 0); }

      function tameEvent(event) {
        event = makeDOMAccessible(event);
        if (!taming.hasTameTwin(event)) {
          var tamed = new TameEvent(event);
          taming.tamesTo(event, tamed);
        }
        return taming.tame(event);
      }

      var ep = TameEventConf.p.bind(TameEventConf);

      var EP_RELATED = {
        enumerable: true,
        extendedAccessors: true,
        get: eventMethod(function (prop) {
          return tameRelatedNode(ep(this).feral[prop], defaultTameNode);
        })
      };

      function P_e_view(transform) {
        return {
          enumerable: true,
          extendedAccessors: true,
          get: eventMethod(function (prop) {
            return transform(ep(this).feral[prop]);
          })
        };
      }

      function TameEvent(event) {
        assert(!!event);
        TameEventConf.confide(this);
        ep(this).feral = event;
        return this;
      }
      inertCtor(TameEvent, Object, 'Event');
      definePropertiesAwesomely(TameEvent.prototype, {
        type: {
          enumerable: true,
          get: eventMethod(function () {
            return bridal.untameEventType(String(ep(this).feral.type));
          })
        },
        eventPhase: P_e_view(Number),
        target: {
          enumerable: true,
          get: eventMethod(function () {
            var event = ep(this).feral;
            return tameRelatedNode(
                event.target || event.srcElement, defaultTameNode);
          })
        },
        srcElement: {
          enumerable: true,
          get: eventMethod(function () {
            return tameRelatedNode(ep(this).feral.srcElement, defaultTameNode);
          })
        },
        currentTarget: {
          enumerable: true,
          get: eventMethod(function () {
            var e = ep(this).feral;
            return tameRelatedNode(e.currentTarget, defaultTameNode);
          })
        },
        relatedTarget: {
          enumerable: true,
          get: eventMethod(function () {
            var e = ep(this).feral;
            var t = e.relatedTarget;
            if (!t) {
              if (e.type === 'mouseout') {
                t = e.toElement;
              } else if (e.type === 'mouseover') {
                t = e.fromElement;
              }
            }
            return tameRelatedNode(t, defaultTameNode);
          }),
          // relatedTarget is read-only.  this dummy setter is because some code
          // tries to workaround IE by setting a relatedTarget when it's not
          // set.
          // code in a sandbox can't tell the difference between "falsey because
          // relatedTarget is not supported" and "falsey because relatedTarget
          // is outside sandbox".
          set: eventMethod(function () {})
        },
        fromElement: EP_RELATED,
        toElement: EP_RELATED,
        pageX: P_e_view(Number),
        pageY: P_e_view(Number),
        altKey: P_e_view(Boolean),
        ctrlKey: P_e_view(Boolean),
        metaKey: P_e_view(Boolean),
        shiftKey: P_e_view(Boolean),
        button: P_e_view(function (v) { return v && Number(v); }),
        clientX: P_e_view(Number),
        clientY: P_e_view(Number),
        screenX: P_e_view(Number),
        screenY: P_e_view(Number),
        which: P_e_view(function (v) { return v && Number(v); }),
        keyCode: P_e_view(function (v) { return v && Number(v); })
      });
      TameEvent.prototype.stopPropagation = eventMethod(function () {
        // TODO(mikesamuel): make sure event doesn't propagate to dispatched
        // events for this gadget only.
        // But don't allow it to stop propagation to the container.
        if (ep(this).feral.stopPropagation) {
          ep(this).feral.stopPropagation();
        } else {
          ep(this).feral.cancelBubble = true;
        }
      });
      TameEvent.prototype.preventDefault = eventMethod(function () {
        // TODO(mikesamuel): make sure event doesn't propagate to dispatched
        // events for this gadget only.
        // But don't allow it to stop propagation to the container.
        if (ep(this).feral.preventDefault) {
          ep(this).feral.preventDefault();
        } else {
          ep(this).feral.returnValue = false;
        }
      });
      setOwn(TameEvent.prototype, "toString", eventMethod(function () {
        return '[domado object Event]';
      }));
      cajaVM.def(TameEvent);  // and its prototype

      function TameCustomHTMLEvent(event) {
        var self = TameEvent.call(this, event);
        if (domitaModules.proxiesAvailable) {
          Object.preventExtensions(this);  // required by ES5/3 proxy emulation
          var proxy = Proxy.create(
              new ExpandoProxyHandler(self, true, {}), self);
          ExpandoProxyHandler.register(proxy, self);
          TameEventConf.confide(proxy, self);
          self = proxy;
        }
        return self;
      }
      inherit(TameCustomHTMLEvent, TameEvent);
      TameCustomHTMLEvent.prototype.initEvent
          = eventMethod(function (type, bubbles, cancelable) {
        bridal.initEvent(ep(this).feral, type, bubbles, cancelable);
      });
      setOwn(TameCustomHTMLEvent.prototype, "toString", eventMethod(function () {
        return '[Fake CustomEvent]';
      }));
      cajaVM.def(TameCustomHTMLEvent);  // and its prototype

      function TameHTMLDocument(doc, container, domain) {
        TamePseudoNode.call(this);

        np(this).feralDoc = doc;
        np(this).feralContainerNode = container;
        np(this).onLoadListeners = [];
        np(this).onDCLListeners = [];

        var tameContainer = defaultTameNode(container);
        np(this).tameContainerNode = tameContainer;

        definePropertiesAwesomely(this, {
          domain: P_constant(domain)
        });

        installLocation(this);
      }
      nodeClasses.Document =
          inertCtor(TameHTMLDocument, TamePseudoNode, 'HTMLDocument');
      definePropertiesAwesomely(TameHTMLDocument.prototype, {
        nodeType: P_constant(9),
        nodeName: P_constant('#document'),
        nodeValue: P_constant(null),
        childNodes: { enumerable: true, get: nodeMethod(function () {
          return np(this).tameContainerNode.childNodes;
        })},
        attributes: { enumerable: true, get: nodeMethod(function () {
          return fakeNodeList([]);
        })},
        parentNode: P_constant(null),
        body: { enumerable: true, get: nodeMethod(function () {
          for (var n = this.documentElement.firstChild; n; n = n.nextSibling) {
            // Note: Standard def. also includes FRAMESET elements but we don't
            // currently support them.
            if (n.nodeName === "BODY") { return n; }
          }
          return null;
        })},
        documentElement: {
          enumerable: true,
          get: cajaVM.def(function () {
            var n;
            // In principle, documentElement should be our sole child, but
            // sometimes that gets screwed up, and we end up with more than
            // one child.  Returning something other than the pseudo <html>
            // element will mess up many things, so we first try finding
            // the <html> element
            for (n = this.firstChild; n; n = n.nextSibling) {
              if (n.nodeName === "HTML") { return n; }
            }
            // No <html>, so return the first child that's an element
            for (n = this.firstChild; n; n = n.nextSibling) {
              if (n.nodeType === 1) { return n; }
            }
            // None of our children are elements, fail
            return null;
          })},
        forms: { enumerable: true, get: nodeMethod(function () {
          var tameForms = [];
          for (var i = 0; i < document.forms.length; i++) {
            var tameForm = tameRelatedNode(
              makeDOMAccessible(document.forms).item(i), defaultTameNode);
            // tameRelatedNode returns null if the node is not part of
            // this node's virtual document.
            if (tameForm !== null) { tameForms.push(tameForm); }
          }
          return fakeNodeList(tameForms);
        })},
        title: {
          // TODO(kpreid): get the title element pointer in conformant way

          // http://www.whatwg.org/specs/web-apps/current-work/multipage/dom.html#document.title
          // as of 2012-08-14
          enumerable: true,
          get: nodeMethod(function() {
            var titleEl = this.getElementsByTagName('title')[0];
            return titleEl ? trimHTML5Spaces(titleEl.textContent) : "";
          }),
          set: nodeMethod(function(value) {
            var titleEl = this.getElementsByTagName('title')[0];
            if (!titleEl) {
              var head = this.getElementsByTagName('head')[0];
              if (head) {
                titleEl = this.createElement('title');
                head.appendChild(titleEl);
              } else {
                return;
              }
            }
            titleEl.textContent = value;
          })
        },
        compatMode: P_constant('CSS1Compat'),
        ownerDocument: P_constant(null)
      });
      TameHTMLDocument.prototype.getElementsByTagName = nodeMethod(
          function (tagName) {
        tagName = String(tagName).toLowerCase();
        return tameGetElementsByTagName(np(this).feralContainerNode, tagName);
      });
      TameHTMLDocument.prototype.getElementsByClassName = nodeMethod(
          function (className) {
        return tameGetElementsByClassName(
            np(this).feralContainerNode, className);
      });
      TameHTMLDocument.prototype.addEventListener =
          nodeMethod(function (name, listener, useCapture) {
            if (name === 'DOMContentLoaded') {
              domitaModules.ensureValidCallback(listener);
              np(tameDocument).onDCLListeners.push(listener);
            } else {
              return np(this).tameContainerNode.addEventListener(
                  name, listener, useCapture);
            }
          });
      TameHTMLDocument.prototype.removeEventListener =
          nodeMethod(function (name, listener, useCapture) {
            return np(this).tameContainerNode.removeEventListener(
                name, listener, useCapture);
          });
      TameHTMLDocument.prototype.createComment = nodeMethod(function (text) {
        return defaultTameNode(np(this).feralDoc.createComment(" "));
      });
      TameHTMLDocument.prototype.createDocumentFragment = nodeMethod(function () {
        np(this).policy.requireEditable();
        return defaultTameNode(np(this).feralDoc.createDocumentFragment());
      });
      TameHTMLDocument.prototype.createElement = nodeMethod(function (tagName) {
        np(this).policy.requireEditable();
        tagName = String(tagName).toLowerCase();
        tagName = htmlSchema.virtualToRealElementName(tagName);
        var newEl = np(this).feralDoc.createElement(tagName);
        if ("canvas" == tagName) {
          bridal.initCanvasElement(newEl);
        }
        if (elementPolicies.hasOwnProperty(tagName)) {
          var attribs = elementPolicies[tagName]([]);
          if (attribs) {
            for (var i = 0; i < attribs.length; i += 2) {
              bridal.setAttribute(newEl, attribs[+i], attribs[i + 1]);
            }
          }
        }
        return defaultTameNode(newEl);
      });
      TameHTMLDocument.prototype.createTextNode = nodeMethod(function (text) {
        np(this).policy.requireEditable();
        return defaultTameNode(np(this).feralDoc.createTextNode(
            text !== null && text !== void 0 ? '' + text : ''));
      });
      TameHTMLDocument.prototype.getElementById = nodeMethod(function (id) {
        id += idSuffix;
        var node = np(this).feralDoc.getElementById(id);
        return defaultTameNode(node);
      });
      // http://www.w3.org/TR/DOM-Level-2-Events/events.html
      // #Events-DocumentEvent-createEvent
      TameHTMLDocument.prototype.createEvent = nodeMethod(function (type) {
        type = String(type);
        if (type !== 'HTMLEvents') {
          // See https://developer.mozilla.org/en/DOM/document.createEvent#Notes
          // for a long list of event ypes.
          // See http://www.w3.org/TR/DOM-Level-2-Events/events.html
          // #Events-eventgroupings
          // for the DOM2 list.
          throw new Error('Unrecognized event type ' + type);
        }
        var document = np(this).feralDoc;
        var rawEvent;
        if (document.createEvent) {
          rawEvent = document.createEvent(type);
        } else {
          rawEvent = document.createEventObject();
          rawEvent.eventType = 'ondataavailable';
        }
        var tamedEvent = new TameCustomHTMLEvent(rawEvent);
        taming.tamesTo(rawEvent, tamedEvent);
        return tamedEvent;
      });
      TameHTMLDocument.prototype.write = nodeMethod(function() {
        if (!domicile.writeHook) {
          throw new Error('document.write not provided for this document');
        }
        return domicile.writeHook.write.apply(undefined, arguments);
      });
      TameHTMLDocument.prototype.writeln = nodeMethod(function() {
        if (!domicile.writeHook) {
          throw new Error('document.writeln not provided for this document');
        }
        // We don't write the \n separately rather than copying args, because
        // the HTML parser would rather get fewer larger chunks.
        var args = Array.prototype.slice.call(arguments);
        args.push("\n");
        domicile.writeHook.write.apply(undefined, args);
      });
      TameHTMLDocument.prototype.open = nodeMethod(function() {
        if (!domicile.writeHook) {
          throw new Error('document.open not provided for this document');
        }
        return domicile.writeHook.open();
      });
      TameHTMLDocument.prototype.close = nodeMethod(function() {
        if (!domicile.writeHook) {
          throw new Error('document.close not provided for this document');
        }
        return domicile.writeHook.close();
      });
      cajaVM.def(TameHTMLDocument);  // and its prototype
      domicile.setBaseUri = cajaVM.def(function(base) {
        var parsed = URI.parse(base);
        var host = null;
        if (parsed.hasDomain()) {
          host = parsed.hasPort() ? parsed.getDomain() + ':' + parsed.getPort() : null;
        }
        domicile.pseudoLocation = {
          href: parsed.toString(),
          hash: parsed.getFragment(),
          host: host,
          hostname: parsed.getDomain(),
          port: parsed.getPort(),
          protocol: parsed.hasScheme() ? parsed.getScheme() + ':' : null,
          pathname: parsed.getPath(),
          search: parsed.getQuery()
        };
      });

      // Called by the html-emitter when the virtual document has been loaded.
      domicile.signalLoaded = cajaVM.def(function () {
        // TODO(kpreid): Review if this rewrite of the condition here is correct
        var self = tameDocument;
        var listeners = np(self).onDCLListeners;
        np(self).onDCLListeners = [];
        for (var i = 0, n = listeners.length; i < n; ++i) {
          window.setTimeout(listeners[+i], 0);
        }
        var onload = tameWindow.onload;
        if (onload) {
          window.setTimeout(onload, 0);
        }
        listeners = np(self).onLoadListeners;
        np(self).onLoadListeners = [];
        for (var i = 0, n = listeners.length; i < n; ++i) {
          window.setTimeout(listeners[+i], 0);
        }
      });

      // For JavaScript handlers.  See function dispatchEvent below
      domicile.handlers = [];
      domicile.tameNode = cajaVM.def(defaultTameNode);
      domicile.feralNode = cajaVM.def(function (tame) {
        return np(tame).feral;  // NOTE: will be undefined for pseudo nodes
      });
      domicile.tameEvent = cajaVM.def(tameEvent);
      domicile.blessHtml = cajaVM.def(blessHtml);
      domicile.blessCss = cajaVM.def(function (var_args) {
        var arr = [];
        for (var i = 0, n = arguments.length; i < n; ++i) {
          arr[+i] = arguments[+i];
        }
        return cssSealerUnsealerPair.seal(arr);
      });
      domicile.htmlAttr = cajaVM.def(function (s) {
        return html.escapeAttrib(String(s || ''));
      });
      domicile.html = cajaVM.def(safeHtml);
      domicile.fetchUri = cajaVM.def(function (uri, mime, callback) {
        uriFetch(naiveUriPolicy, uri, mime, callback);
      });
      domicile.rewriteUri = cajaVM.def(function (uri, mimeType, opt_hints) {
        // (SAME_DOCUMENT, SANDBOXED) is chosen as the "reasonable" set of
        // defaults for this function, which is only used by TCB components
        // to rewrite URIs for sources of data. We assume these sources of
        // data provide no exit from the sandbox, and their effects are shown
        // in the same HTML document view as the Caja guest.
        // TODO(ihab.awad): Rename this function to something more specific
        return uriRewrite(
            naiveUriPolicy,
            String(uri),
            html4.ueffects.SAME_DOCUMENT,
            html4.ltypes.SANDBOXED,
            opt_hints || {});
      });
      domicile.suffix = cajaVM.def(function (nmtokens) {
        var p = String(nmtokens).replace(/^\s+|\s+$/g, '').split(/\s+/g);
        var out = [];
        for (var i = 0; i < p.length; ++i) {
          var nmtoken = rewriteAttribute(null, null, html4.atype.ID, p[+i]);
          if (!nmtoken) { throw new Error(nmtokens); }
          out.push(nmtoken);
        }
        return out.join(' ');
      });
      domicile.suffixStr = idSuffix;
      domicile.ident = cajaVM.def(function (nmtokens) {
        var p = String(nmtokens).replace(/^\s+|\s+$/g, '').split(/\s+/g);
        var out = [];
        for (var i = 0; i < p.length; ++i) {
          var nmtoken = rewriteAttribute(null, null, html4.atype.CLASSES, p[+i]);
          if (!nmtoken) { throw new Error(nmtokens); }
          out.push(nmtoken);
        }
        return out.join(' ');
      });
      domicile.rewriteUriInCss = cajaVM.def(function (value, propName) {
        return value
          ? uriRewrite(naiveUriPolicy, value, html4.ueffects.SAME_DOCUMENT,
                html4.ltypes.SANDBOXED,
                {
                  "TYPE": "CSS",
                  "CSS_PROP": propName
                })
          : void 0;
      });
      domicile.rewriteUriInAttribute = cajaVM.def(
          function (value, tagName, attribName) {
        if (value.charAt(0) === '#' && isValidId(value.substring(1))) {
          return value + idSuffix;
        }
        var schemaAttr = htmlSchema.attribute(tagName, attribName);
        return value
          ? uriRewrite(naiveUriPolicy, value, schemaAttr.uriEffect,
                schemaAttr.loaderType, {
                  "TYPE": "MARKUP",
                  "XML_ATTR": attribName,
                  "XML_TAG": tagName
                })
          : void 0;
      });
      domicile.rewriteTargetAttribute = cajaVM.def(
          function (value, tagName, attribName) {
        // TODO(ihab.awad): Parrots much of the code in sanitizeAttrs; refactor
        var atype = null, attribKey;
        if ((attribKey = tagName + '::' + attribName,
             html4.ATTRIBS.hasOwnProperty(attribKey))
            || (attribKey = '*::' + attribName,
                html4.ATTRIBS.hasOwnProperty(attribKey))) {
          atype = html4.ATTRIBS[attribKey];
          return rewriteAttribute(tagName, attribName, atype, value);
        }
        return null;
      });

      // Taming of Styles:

      // defer construction
      var TameStyle = null;
      var TameComputedStyle = null;

      function buildTameStyle() {

        var aStyleForCPC = docEl.style;
        aStyleForCPC = makeDOMAccessible(aStyleForCPC);
        var allCssProperties = domitaModules.CssPropertiesCollection(
            aStyleForCPC);

        // Sealed internals for TameStyle objects, not to be exposed.
        var TameStyleConf = new Confidence('Style');

        function allowProperty(cssPropertyName) {
          return allCssProperties.isCssProp(cssPropertyName);
        };

        /**
         * http://www.w3.org/TR/DOM-Level-2-Style/css.html#CSS-CSSStyleDeclaration
         */
        TameStyle = function (style, editable, tameEl) {
          style = makeDOMAccessible(style);

          TameStyleConf.confide(this);
          TameStyleConf.p(this).feral = style;
          TameStyleConf.p(this).editable = editable;
          TameStyleConf.p(this).tameElement = tameEl;

          TameStyleConf.p(this).readByCanonicalName = function(canonName) {
            return String(style[canonName] || '');
          };
          TameStyleConf.p(this).writeByCanonicalName = function(canonName, val) {
            style[canonName] = val;
          };
        };
        inertCtor(TameStyle, Object /*, 'Style'*/);
            // cannot export lazily
        TameStyle.prototype.getPropertyValue =
            cajaVM.def(function (cssPropertyName) {
          cssPropertyName = String(cssPropertyName || '').toLowerCase();
          if (!allowProperty(cssPropertyName)) { return ''; }
          var canonName = allCssProperties.getCanonicalPropFromCss(
              cssPropertyName);
          return TameStyleConf.p(this).readByCanonicalName(canonName);
        });
        setOwn(TameStyle.prototype, "toString", cajaVM.def(function () {
          return '[domado object Style]';
        }));
        definePropertiesAwesomely(TameStyle.prototype, {
          cssText: {
            enumerable: canHaveEnumerableAccessors,
            set: cajaVM.def(function (value) {
              var p = TameStyleConf.p(this);
              if (typeof p.feral.cssText === 'string') {
                p.feral.cssText = sanitizeStyleAttrValue(value);
              } else {
                // If the browser doesn't support setting cssText, then fall
                // back to setting the style attribute of the containing
                // element.  This won't work for style declarations that are
                // part of stylesheets and not attached to elements.
                p.tameElement.setAttribute('style', value);
              }
              return true;
            })
          }
        });
        allCssProperties.forEachCanonical(function (stylePropertyName) {
          // TODO(kpreid): make each of these generated accessors more
          // specialized for this name to reduce runtime cost.
          Object.defineProperty(TameStyle.prototype, stylePropertyName, {
            enumerable: canHaveEnumerableAccessors,
            get: cajaVM.def(function () {
              if (!TameStyleConf.p(this).feral
                  || !allCssProperties.isCanonicalProp(stylePropertyName)) {
                return void 0;
              }
              var cssPropertyName =
                  allCssProperties.getCssPropFromCanonical(stylePropertyName);
              if (!allowProperty(cssPropertyName)) { return void 0; }
              var canonName =
                  allCssProperties.getCanonicalPropFromCss(cssPropertyName);
              return TameStyleConf.p(this).readByCanonicalName(canonName);
            }),
            set: cajaVM.def(function (value) {
              var p = TameStyleConf.p(this);
              if (!p.editable) { throw new Error('style not editable'); }
              stylePropertyName = String(stylePropertyName);
              if (!allCssProperties.isCanonicalProp(stylePropertyName)) {
                throw new Error('Unknown CSS property name ' + stylePropertyName);
              }
              var cssPropertyName =
                  allCssProperties.getCssPropFromCanonical(stylePropertyName);
              if (!allowProperty(cssPropertyName)) { return void 0; }
              var tokens = lexCss(value);
              if (tokens.length === 0
                 || (tokens.length === 1 && tokens[0] === ' ')) {
                value = '';
              } else {
                if (!sanitizeStyleProperty(cssPropertyName, tokens)) {
                  console.log('bad value `' + value + '` for CSS property '
                                  + stylePropertyName);
                }
                value = tokens.join(' ');
              }
              var canonName =
                  allCssProperties.getCanonicalPropFromCss(cssPropertyName);
              p.writeByCanonicalName(canonName, value);
              return true;
            })
          });
        });
        cajaVM.def(TameStyle);  // and its prototype

        function isNestedInAnchor(el) {
          for (;
              el && el != containerNode;
              el = makeDOMAccessible(el.parentNode)) {
            if (el.tagName && el.tagName.toLowerCase() === 'a') {
              return true;
            }
          }
          return false;
        }

        TameComputedStyle = function (rawElement, pseudoElement) {
          rawElement = rawElement || document.createElement('div');
          TameStyle.call(
              this,
              bridal.getComputedStyle(rawElement, pseudoElement),
              false);
          TameStyleConf.p(this).rawElement = rawElement;
          TameStyleConf.p(this).pseudoElement = pseudoElement;

          var superReadByCanonicalName =
              TameStyleConf.p(this).readByCanonicalName;
          TameStyleConf.p(this).readByCanonicalName = function(canonName) {
            var propName = allCssProperties.getCssPropFromCanonical(canonName);
            var schemaElement = cssSchema[propName];
            var canReturnDirectValue =
                (schemaElement
                 && (schemaElement.cssPropBits
                     & CSS_PROP_BIT_HISTORY_INSENSITIVE))
                || !isNestedInAnchor(this.rawElement);
            if (canReturnDirectValue) {
              return superReadByCanonicalName.call(this, canonName);
            } else {
              return TameStyleConf.p(
                      new TameComputedStyle(containerNode, this.pseudoElement))
                  .readByCanonicalName(canonName);
            }
          };
          TameStyleConf.p(this).writeByCanonicalName = function(canonName) {
            throw 'Computed styles not editable: This code should be unreachable';
          };
        };
        inertCtor(TameComputedStyle, TameStyle);
        setOwn(TameComputedStyle.prototype, "toString", cajaVM.def(function () {
          return '[Fake Computed Style]';
        }));
        cajaVM.def(TameComputedStyle);  // and its prototype
      }

      // Note: nodeClasses.XMLHttpRequest is a ctor that *can* be directly
      // called by cajoled code, so we do not use inertCtor().
      nodeClasses.XMLHttpRequest = domitaModules.TameXMLHttpRequest(
          taming,
          rulebreaker,
          domitaModules.XMLHttpRequestCtor(
              makeDOMAccessible,
              makeFunctionAccessible(window.XMLHttpRequest),
              makeFunctionAccessible(window.ActiveXObject),
              makeFunctionAccessible(window.XDomainRequest)),
          naiveUriPolicy,
          function () { return domicile.pseudoLocation.href; });
      cajaVM.def(nodeClasses.XMLHttpRequest);

      /**
       * given a number, outputs the equivalent css text.
       * @param {number} num
       * @return {string} an CSS representation of a number suitable for both html
       *    attribs and plain text.
       */
      domicile.cssNumber = cajaVM.def(function (num) {
        if ('number' === typeof num && isFinite(num) && !isNaN(num)) {
          return '' + num;
        }
        throw new Error(num);
      });
      /**
       * given a number as 24 bits of RRGGBB, outputs a properly formatted CSS
       * color.
       * @param {number} num
       * @return {string} a CSS representation of num suitable for both html
       *    attribs and plain text.
       */
      domicile.cssColor = cajaVM.def(function (color) {
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
      });
      domicile.cssUri = cajaVM.def(function (uri, mimeType, prop) {
        uri = String(uri);
        if (!naiveUriPolicy) { return null; }
        return uriRewrite(
            naiveUriPolicy,
            uri,
            html4.ueffects.SAME_DOCUMENT,
            html4.ltypes.SANDBOXED,
            {
              "TYPE": "CSS",
              "CSS_PROP": prop
            });
      });

      /**
       * Create a CSS stylesheet with the given text and append it to the DOM.
       * @param {string} cssText a well-formed stylesheet production.
       */
      domicile.emitCss = cajaVM.def(function (cssText) {
        this.getCssContainer().appendChild(
            bridal.createStylesheet(document, cssText));
      });
      /** The node to which gadget stylesheets should be added. */
      domicile.getCssContainer = cajaVM.def(function () {
        var e = document.getElementsByTagName('head')[0];
        e = makeDOMAccessible(e);
        return e;
      });
      domicile.tagPolicy = tagPolicy;  // used by CSS rewriter

      if (!/^-/.test(idSuffix)) {
        throw new Error('id suffix "' + idSuffix + '" must start with "-"');
      }
      if (!/___$/.test(idSuffix)) {
        throw new Error('id suffix "' + idSuffix + '" must end with "___"');
      }
      var idClass = idSuffix.substring(1);
      var idClassPattern = new RegExp(
          '(?:^|\\s)' + idClass.replace(/[\.$]/g, '\\$&') + '(?:\\s|$)');
      /**
       * Is this the node whose children are the children of the virtual
       * document?
       */
      function isContainerNode(node) {
        return node === containerNode ||
            (node &&
             node.nodeType === 1 &&
             idClassPattern.test(node.className));
      }
      /** A per-gadget class used to separate style rules. */
      domicile.getIdClass = cajaVM.def(function () {
        return idClass;
      });
      // enforce id class on container
      if (containerNode.nodeType !== 9) {  // not a document (top level)
        bridal.setAttribute(containerNode, 'class',
            bridal.getAttribute(containerNode, 'class')
            + ' ' + idClass + ' vdoc-container___');
      }

      // bitmask of trace points
      //    0x0001 plugin_dispatchEvent
      domicile.domitaTrace = 0;
      domicile.getDomitaTrace = cajaVM.def(
          function () { return domicile.domitaTrace; }
      );
      domicile.setDomitaTrace = cajaVM.def(
          function (x) { domicile.domitaTrace = x; }
      );

      /**
       * See http://www.whatwg.org/specs/web-apps/current-work/multipage/browsers.html#window for the full API.
       */
      function TameWindow() {

        // These descriptors were chosen to resemble actual ES5-supporting browser
        // behavior.
        // The document property is defined below.
        installLocation(this);
        Object.defineProperty(this, "navigator", {
          value: tameNavigator,
          configurable: false,
          enumerable: true,
          writable: false
        });
        taming.permitUntaming(this);
      }
      inertCtor(TameWindow, Object, 'Window');
      // Methods of TameWindow are established later.
      setOwn(TameWindow.prototype, "toString", cajaVM.def(function() {
        return "[domado object Window]";
      }));

      // Location object -- used by Document and Window and so must be created
      // before each.
      function TameLocation() {
        // TODO(mikesamuel): figure out a mechanism by which the container can
        // specify the gadget's apparent URL.
        // See http://www.whatwg.org/specs/web-apps/current-work/multipage/history.html#location0
        var tameLocation = this;
        function defineLocationField(f, dflt) {
          Object.defineProperty(tameLocation, f, {
            configurable: false,
            enumerable: true,
            get: function() { 
              return String(domicile.pseudoLocation[f] || dflt); 
            }
          });
        }
        defineLocationField('href', 'http://nosuchhost.invalid:80/');
        defineLocationField('hash', '');
        defineLocationField('host', 'nosuchhost.invalid:80');
        defineLocationField('hostname', 'nosuchhost.invalid');
        defineLocationField('pathname', '/');
        defineLocationField('port', '80');
        defineLocationField('protocol', 'http:');
        defineLocationField('search', '');
        setOwn(tameLocation, 'toString',
          cajaVM.def(function() { return tameLocation.href; }));
      }
      inertCtor(TameLocation, Object);
      var tameLocation = new TameLocation();
      function installLocation(obj) {
        Object.defineProperty(obj, "location", {
          value: tameLocation,
          configurable: false,
          enumerable: true,
          writable: false  // Writable in browsers, but has a side-effect
                           // which we don't implement.
        });
      }

      // See spec at http://www.whatwg.org/specs/web-apps/current-work/multipage/browsers.html#navigator
      // We don't attempt to hide or abstract userAgent details since
      // they are discoverable via side-channels we don't control.
      var navigator = makeDOMAccessible(window.navigator);
      var tameNavigator = cajaVM.def({
        appName: String(navigator.appName),
        appVersion: String(navigator.appVersion),
        platform: String(navigator.platform),
        // userAgent should equal the string sent in the User-Agent HTTP header.
        userAgent: String(navigator.userAgent),
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

      // Under ES53, the set/clear pairs get invoked with 'this' bound
      // to USELESS, which causes problems on Chrome unless they're wrapped
      // this way.
      tameSetAndClear(
          TameWindow.prototype,
          function (code, millis) { return window.setTimeout(code, millis); },
          function (id) { return window.clearTimeout(id); },
          'setTimeout', 'clearTimeout');
      tameSetAndClear(
          TameWindow.prototype,
          function (code, millis) { return window.setInterval(code, millis); },
          function (id) { return window.clearInterval(id); },
          'setInterval', 'clearInterval');
      TameWindow.prototype.addEventListener = cajaVM.def(
          function (name, listener, useCapture) {
        if (name === 'load') {
          domitaModules.ensureValidCallback(listener);
          np(tameDocument).onLoadListeners.push(listener);
        } else if (name === 'DOMContentLoaded') {
          domitaModules.ensureValidCallback(listener);
          np(tameDocument).onDCLListeners.push(listener);
        } else {
          // TODO: need a testcase for this
          tameDocument.addEventListener(name, listener, useCapture);
        }
      });
      TameWindow.prototype.removeEventListener = cajaVM.def(
          function (name, listener, useCapture) {
        if (name === 'load' || name === 'DOMContentLoaded') {
          var listeners = np(tameDocument)[name === 'load' ?
              'onLoadListeners' : 'onDCLListeners'];
          var k = 0;
          for (var i = 0, n = listeners.length; i < n; ++i) {
            listeners[i - k] = listeners[+i];
            if (listeners[+i] === listener) {
              ++k;
            }
          }
          listeners.length -= k;
        } else {
          tameDocument.removeEventListener(name, listener, useCapture);
        }
      });
      TameWindow.prototype.dispatchEvent = cajaVM.def(function (evt) {
        // TODO(ihab.awad): Implement
      });
      TameWindow.prototype.scrollBy = cajaVM.def(function(dx, dy) {
        // The window is always auto scrollable, so make the apparent window
        // body scrollable if the gadget tries to scroll it.
        if (dx || dy) {
          makeScrollable(bridal, np(tameDocument).feralContainerNode);
        }
        tameScrollBy(np(tameDocument).feralContainerNode, dx, dy);
      });
      TameWindow.prototype.scrollTo = cajaVM.def(function(x, y) {
        // The window is always auto scrollable, so make the apparent window
        // body scrollable if the gadget tries to scroll it.
        makeScrollable(bridal, np(tameDocument).feralContainerNode);
        tameScrollTo(np(tameDocument).feralContainerNode, x, y);
      });
      TameWindow.prototype.resizeTo = cajaVM.def(function(w, h) {
        tameResizeTo(np(tameDocument).feralContainerNode, w, h);
      });
      TameWindow.prototype.resizeBy = cajaVM.def(function(dw, dh) {
        tameResizeBy(np(tameDocument).feralContainerNode, dw, dh);
      });
        /** A partial implementation of getComputedStyle. */
      TameWindow.prototype.getComputedStyle = cajaVM.def(
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
        TameComputedStyle || buildTameStyle();
        return new TameComputedStyle(
            np(tameElement).feral,
            pseudoElement);
      });
      // NOT PROVIDED on window:
      // event: a global on IE.  We always define it in scopes that can handle
      //        events.
      // opera: defined only on Opera.

      // misc getters
      forOwnKeys({
        pageXOffset: cajaVM.def(function() { return this.scrollX; }),
        pageYOffset: cajaVM.def(function() { return this.scrollY; }),
        // Note: np(this.document) is amplification; this is only a safe pattern
        // when the method is harmless if applied to other nodes.
        scrollX: cajaVM.def(function() {
            return np(this.document).feralContainerNode.scrollLeft; }),
        scrollY: cajaVM.def(function() {
            return np(this.document).feralContainerNode.scrollTop; }),
        innerHeight: cajaVM.def(function() {
            return np(this.document).feralContainerNode.offsetHeight; }),
        innerWidth: cajaVM.def(function() {
            return np(this.document).feralContainerNode.offsetWidth; }),
        outerHeight: cajaVM.def(function() {
            return np(this.document).feralContainerNode.offsetHeight; }),
        outerWidth: cajaVM.def(function() {
            return np(this.document).feralContainerNode.offsetWidth; })
      }, function(propertyName, handler) {
        Object.defineProperty(TameWindow.prototype, propertyName, {
          enumerable: canHaveEnumerableAccessors,
          get: handler
        });
      });

      // No-op functions/methods
      [
        'blur', 'focus',
        'close',
        'moveBy', 'moveTo',
        'print',
        'stop'
      ].forEach(function(name) {
        var notify = true;
        Object.defineProperty(TameWindow.prototype, name, {
          // Matches Firefox 17.0
          // Chrome 25.0.1329.0 canary has configurable:false
          configurable: true,
          enumerable: true,
          writable: true,
          value: cajaVM.def(function domadoNoop() {
            if (notify) {
              notify = false;
              if (typeof console !== 'undefined') {
                console.warn('Domado: ignoring window.' + name + '(\u2026).');
              }
            }
          })
        });
      });

      // Stub properties
      ['locationbar', 'personalbar', 'menubar', 'scrollbars', 'statusbar',
          'toolbar'].forEach(function(name) {
        // Firefox's class name is (exported) BarProp, Chrome's is (nonexported)
        // BarInfo.
        // visible: false was chosen to reflect what the Caja environment
        // provides (e.g. there is no location bar displaying the URL), not
        // browser behavior (which, for Firefox, is to have .visible be false if
        // and only if the window was created by a window.open specifying that,
        // whether or not the relevant toolbar actually is hidden).
        TameWindow.prototype[name] = cajaVM.def({visible: false});
      });

      cajaVM.def(TameWindow);  // and its prototype

      // Freeze exported classes. Must occur before TameHTMLDocument is
      // instantiated.
      for (var name in nodeClasses) {
        var ctor = nodeClasses[name];
        cajaVM.def(ctor);  // and its prototype
      }
      Object.freeze(nodeClasses);
          // fail hard if late-added item wouldn't be frozen

      var tameDocument = new TameHTMLDocument(
          document,
          containerNode,
          // TODO(jasvir): Properly wire up document.domain
          // by untangling the cyclic dependence between
          // TameWindow and TameDocument
          String(undefined || 'nosuchhost.invalid'));
      domicile.htmlEmitterTarget = containerNode;

      var tameWindow = new TameWindow();



      // Attach reflexive properties to 'window' object
      var windowProps = ['top', 'self', 'opener', 'parent', 'window'];
      var wpLen = windowProps.length;
      for (var i = 0; i < wpLen; ++i) {
        var prop = windowProps[+i];
        tameWindow[prop] = tameWindow;
      }

      if (np(tameDocument).policy.editable) {
        // Powerful singleton authority not granted for RO document
        tameDocument.defaultView = tameWindow;

        // Hook for document.write support.
        domicile.sanitizeAttrs = sanitizeAttrs;
      }

      // Iterate over all node classes, assigning them to the Window object
      // under their DOM Level 2 standard name. They have been frozen above.
      for (var name in nodeClasses) {
        var ctor = nodeClasses[name];
        Object.defineProperty(tameWindow, name, {
          enumerable: true,
          configurable: true,
          writable: true,
          value: ctor
        });
      }

      // TODO(ihab.awad): Build a more sophisticated virtual class hierarchy by
      // having a table of subclass relationships and implementing them.

      // If a node class name in this list is not defined using defineElement or
      // inertCtor above, then it will now be bound to the HTMLElement class.
      var allDomNodeClasses = htmlSchema.getAllKnownScriptInterfaces();
      var defaultNodeClassCtor = nodeClasses.HTMLElement;
      for (var i = 0; i < allDomNodeClasses.length; i++) {
        var className = allDomNodeClasses[+i];
        if (!(className in tameWindow)) {
          Object.defineProperty(tameWindow, className, {
            enumerable: true,
            configurable: true,
            writable: true,
            value: defaultNodeClassCtor
          });
        }
      }

      tameWindow.Image = TameImageFun;
      tameWindow.Option = TameOptionFun;

      tameDocument = finishNode(tameDocument);

      domicile.window = tameWindow;
      domicile.document = tameDocument;
      Object.defineProperty(tameWindow, 'document', {
        value: tameDocument,
        configurable: false,
        enumerable: true,
        writable: false
      });

      pluginId = rulebreaker.getId(tameWindow);
      windowToDomicile.set(tameWindow, domicile);

      // Install virtual UA stylesheet.
      if (!document.caja_gadgetStylesheetInstalled) (function () {
        document.caja_gadgetStylesheetInstalled = true;
        
        var element = makeDOMAccessible(document.createElement("style"));
        element.setAttribute("type", "text/css");
        element.textContent = (
          // Visually contains the virtual document
          ".vdoc-container___ {" +
            "position:relative!important;" +
            "overflow:auto!important;" +
            "clip:rect(auto,auto,auto,auto)!important;" + // paranoia
          "}" +

          // Styles for HTML elements that we virtualize, and so do not get the
          // normal UA stylesheet rules applied:

          // Should be the intersection of HTML5 spec's list and our virtualized
          // (i.e. non-whitelisted) elements. Source:
          // <http://www.whatwg.org/specs/web-apps/current-work/multipage/rendering.html#the-css-user-agent-style-sheet-and-presentational-hints>
          "caja-v-base,caja-v-basefont,caja-v-head,caja-v-link,caja-v-meta," +
          "caja-v-noembed,caja-v-noframes,caja-v-param,caja-v-source," +
          "caja-v-track,caja-v-title{" +
            "display:none;" + 
          "}" +

          "caja-v-html, caja-v-body {" +
            "display:block;" +
          "}"
        );
        domicile.getCssContainer().appendChild(element);
      })();

      return domicile;
    }

    /**
     * Function called from rewritten event handlers to dispatch an event safely.
     */
    function plugin_dispatchEvent(thisNode, event, pluginId, handler) {
      var window = bridalMaker.getWindow(thisNode, makeDOMAccessible);
      event = makeDOMAccessible(event || window.event);
      // support currentTarget on IE[678]
      if (!event.currentTarget) {
        event.currentTarget = thisNode;
      }
      var imports = rulebreaker.getImports(pluginId);
      var domicile = windowToDomicile.get(imports);
      var node = domicile.tameNode(thisNode);
      var isUserAction = eventIsUserAction(event, window);
      try {
        return dispatch(
          isUserAction, pluginId, handler,
          [ node, domicile.tameEvent(event), node ]);
      } catch (ex) {
        imports.onerror(ex.message, 'unknown', 0);
      }
    }

    /**
     * Return true if event is a user action that can be expected to do
     * click(), focus(), etc.
     */
    function eventIsUserAction(event, window) {
      if (!(event instanceof window.UIEvent)) { return false; }
      switch (event.type) {
        case 'click':
        case 'dblclick':
        case 'keypress':
        case 'keydown':
        case 'keyup':
        case 'mousedown':
        case 'mouseup':
        case 'touchstart':
        case 'touchend':
          return true;
      }
      return false;
    }

    /**
     * Called when user clicks on a javascript: link.
     */
    function plugin_dispatchToHandler(pluginId, handler, args) {
      return dispatch(true, pluginId, handler, args);
    }

    function dispatch(isUserAction, pluginId, handler, args) {
      var domicile = windowToDomicile.get(rulebreaker.getImports(pluginId));
      if (domicile.domitaTrace & 0x1 && typeof console != 'undefined') {
        var sig = ('' + handler).match(/^function\b[^\)]*\)/);
        console.log(
            'Dispatch pluginId=' + pluginId +
            ', handler=' + (sig ? sig[0] : handler) +
            ', args=' + args);
      }
      switch (typeof handler) {
        case 'number':
          handler = domicile.handlers[+handler];
          break;
        case 'string':
          var fn = void 0;
          fn = domicile.window[handler];
          handler = fn && typeof fn.call === 'function' ? fn : void 0;
          break;
        case 'function': case 'object': break;
        default:
          throw new Error(
              'Expected function as event handler, not ' + typeof handler);
      }
      domicile.handlingUserAction = isUserAction;
      try {
        return handler.call.apply(handler, args);
      } catch (ex) {
        // guard against IE discarding finally blocks
        domicile.handlingUserAction = false;
        throw ex;
      } finally {
        domicile.handlingUserAction = false;
      }
    }

    return cajaVM.def({
      attachDocument: attachDocument,
      plugin_dispatchEvent: plugin_dispatchEvent,
      plugin_dispatchToHandler: plugin_dispatchToHandler,
      getDomicileForWindow: windowToDomicile.get.bind(windowToDomicile)
    });
  });
})();

// Exports for closure compiler.
if (typeof window !== 'undefined') {
  window['Domado'] = Domado;
}
