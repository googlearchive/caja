// Copyright (C) 2011 Google Inc.
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

/**
 * @fileoverview Install a leaky WeakMap emulation on platforms that
 * don't provide a built-in one.
 *
 * <p>Assumes that an ES5 platform where, if {@code WeakMap} is
 * already present, then it conforms to the anticipated ES6
 * specification. To run this file on an ES5 or almost ES5
 * implementation where the {@code WeakMap} specification does not
 * quite conform, run <code>repairES5.js</code> first.
 *
 * @author Mark S. Miller
 * @requires ses
 * @overrides WeakMap
 */

/**
 * This {@code WeakMap} emulation is observably equivalent to the
 * ES-Harmony WeakMap, but with leakier garbage collection properties.
 *
 * <p>As with true WeakMaps, in this emulation, a key does not
 * retain maps indexed by that key and (crucially) a map does not
 * retain the keys it indexes. A key by itself also does not retain
 * the values associated with that key.
 *
 * <p>However, the values placed in an emulated WeakMap are retained
 * so long as that map is retained and those associations are not
 * overridden. For example, when used to support membranes, all
 * values exported from a given membrane will live for the lifetime
 * of the membrane. But when the membrane is revoked, all objects
 * encapsulated within that membrane will still be collected. This
 * is the best we can do without VM support.
 *
 * <p>The API implemented here is approximately the API as implemented
 * in FF6.0a1 and agreed to by MarkM, Andreas Gal, and Dave Herman,
 * rather than the offially approved proposal page. TODO(erights):
 * upgrade the ecmascript WeakMap proposal page to explain this API
 * change and present to EcmaScript committee for their approval.
 *
 * <p>The first difference between the emulation here and that in
 * FF6.0a1 is the presence of non enumerable {@code get___, has___,
 * set___, and delete___} methods on WeakMap instances to represent
 * what would be the hidden internal properties of a primitive
 * implementation. Whereas the FF6.0a1 WeakMap.prototype methods
 * require their {@code this} to be a genuine WeakMap instance (i.e.,
 * an object of {@code [[Class]]} "WeakMap}), since there is nothing
 * unforgeable about the pseudo-internal method names used here,
 * nothing prevents these emulated prototype methods from being
 * applied to non-WeakMaps with pseudo-internal methods of the same
 * names.
 *
 * <p>Another difference is that our emulated {@code
 * WeakMap.prototype} is not itself a WeakMap. A problem with the
 * current FF6.0a1 API is that WeakMap.prototype is itself a WeakMap
 * providing ambient mutability and an ambient communications
 * channel. Thus, if a WeakMap is already present and has this
 * problem, repairES5.js wraps it in a safe wrappper in order to
 * prevent access to this channel. (See
 * PATCH_MUTABLE_FROZEN_WEAKMAP_PROTO in repairES5.js).
 */
var WeakMap;

/**
 * If this is a full <a href=
 * "http://code.google.com/p/es-lab/wiki/SecureableES5"
 * >secureable ES5</a> platform and the ES-Harmony {@code WeakMap} is
 * absent, install an approximate emulation.
 *
 * <p>If this is almost a secureable ES5 platform, then WeakMap.js
 * should be run after repairES5.js.
 *
 * <p>See {@code WeakMap} for documentation of the garbage collection
 * properties of this WeakMap emulation.
 */
(function() {
  "use strict";

  if (typeof ses !== 'undefined' && ses.ok && !ses.ok()) {
    // already too broken, so give up
    return;
  }

  if (typeof WeakMap === 'function') {
    // assumed fine, so we're done.
    return;
  }

  var hop = Object.prototype.hasOwnProperty;
  var gopn = Object.getOwnPropertyNames;
  var defProp = Object.defineProperty;

  /**
   * Holds the orginal static properties of the Object constructor,
   * after repairES5 fixes these if necessary to be a more complete
   * secureable ES5 environment, but before installing the following
   * WeakMap emulation overrides and before any untrusted code runs.
   */
  var originalProps = {};
  gopn(Object).forEach(function(name) {
    originalProps[name] = Object[name];
  });

  var NO_IDENT = 'noident:0';

  /**
   * Gets a value which is either NO_IDENT or uniquely identifies the
   * key object, for use in making maps keyed by this key object.
   *
   * <p>Two keys that are <a href=
   * "http://wiki.ecmascript.org/doku.php?id=harmony:egal">egal</a>
   * MUST have the same {@code identity}. Two keys that are not egal
   * MUST either have different identities, or at least one of their
   * identities MUST be {@code NO_IDENT}.
   *
   * An identity is either a string or a const function returning a
   * mostly-unique string. The identity of an object is always either
   * NO_IDENT or such a function. The egal-identity of the function
   * itself is used to resolve collisions on the string returned by
   * the function. If the key is not an object (i.e., a primitive,
   * null, or undefined), then identity(key) throws a TypeError.
   *
   * <p>When a map stores a key's identity rather than the key itself,
   * the map does not cause the key to be retained. See the emulated
   * {@code WeakMap} below for the resulting gc properties.
   *
   * <p>To identify objects with reasonable efficiency on ES5 by
   * itself (i.e., without any object-keyed collections), we need to
   * add a reasonably hidden property to such key objects when we
   * can. This raises three issues:
   * <ul>
   * <li>arranging to add this property to objects before we lose the
   *     chance, and
   * <li>reasonably hiding the existence of this new property from
   *     most JavaScript code.
   * <li>Preventing <i>identity theft</i>, where one object is created
   *     falsely claiming to have the identity of another object.
   * </ul>
   * We do so by
   * <ul>
   * <li>Making the hidden property non-enumerable, so we need not
   *     worry about for-in loops or {@code Object.keys},
   * <li>Making the hidden property an accessor property,
   *     where the hidden property's getter is the identity, and the
   *     value the getter returns is the mostly unique string.
   * <li>monkey patching those reflective methods that would
   *     prevent extensions, to add this hidden property first,
   * <li>monkey patching those methods that would reveal this
   *     hidden property, and
   * <li>monkey patching those methods that would overwrite this
   *     hidden property.
   * </ul>
   * Given our parser-less verification strategy, the remaining
   * non-transparencies which are not easily fixed are
   * <ul>
   * <li>The {@code delete}, {@code in}, property access
   *     ({@code []}, and {@code .}), and property assignment
   *     operations each reveal the presence of the hidden
   *     property. The property access operations also reveal the
   *     randomness provided by {@code Math.random}. This is not
   *     currently an issue but may become one if SES otherwise seeks
   *     to hide {@code Math.random}.
   * </ul>
   * These are not easily fixed because they are primitive operations
   * which cannot be monkey patched. However, because we're
   * representing the precious identity by the identity of the
   * property's getter rather than the value gotten, this identity
   * itself cannot leak or be installed by the above non-transparent
   * operations.
   */
  function identity(key) {
    var name;
    function identGetter() { return name; }
    if (key !== Object(key)) {
      throw new TypeError('Not an object: ' + key);
    }
    var desc = originalProps.getOwnPropertyDescriptor(key, 'ident___');
    if (desc) { return desc.get; }
    if (!originalProps.isExtensible(key)) { return NO_IDENT; }

    name = 'hash:' + Math.random();
    // If the following two lines a swapped, Safari WebKit Nightly
    // Version 5.0.5 (5533.21.1, r87697) crashes.
    // See https://bugs.webkit.org/show_bug.cgi?id=61758
    originalProps.freeze(identGetter.prototype);
    originalProps.freeze(identGetter);

    defProp(key, 'ident___', {
      get: identGetter,
      set: undefined,
      enumerable: false,
      configurable: false
    });
    return identGetter;
  }

  /**
   * Monkey patch operations that would make their first argument
   * non-extensible.
   *
   * <p>The monkey patched versions throw a TypeError if their first
   * argument is not an object, so it should only be used on functions
   * that should throw a TypeError if their first arg is not an
   * object.
   */
  function identifyFirst(base, name) {
    var oldFunc = base[name];
    defProp(base, name, {
      value: function(obj, var_args) {
        identity(obj);
        return oldFunc.apply(this, arguments);
      }
    });
  }
  identifyFirst(Object, 'freeze');
  identifyFirst(Object, 'seal');
  identifyFirst(Object, 'preventExtensions');
  identifyFirst(Object, 'defineProperty');
  identifyFirst(Object, 'defineProperties');

  defProp(Object, 'getOwnPropertyNames', {
    value: function fakeGetOwnPropertyNames(obj) {
      var result = gopn(obj);
      var i = result.indexOf('ident___');
      if (i >= 0) { result.splice(i, 1); }
      return result;
    }
  });

  defProp(Object, 'getOwnPropertyDescriptor', {
    value: function fakeGetOwnPropertyDescriptor(obj, name) {
      if (name === 'ident___') { return undefined; }
      return originalProps.getOwnPropertyDescriptor(obj, name);
    }
  });

  if ('getPropertyNames' in Object) {
    // Not in ES5 but in whitelist as expected for ES-Harmony
    defProp(Object, 'getPropertyNames', {
      value: function fakeGetPropertyNames(obj) {
        var result = originalProps.getPropertyNames(obj);
        var i = result.indexOf('ident___');
        if (i >= 0) { result.splice(i, 1); }
        return result;
      }
    });
  }

  if ('getPropertyDescriptor' in Object) {
    // Not in ES5 but in whitelist as expected for ES-Harmony
    defProp(Object, 'getPropertyDescriptor', {
      value: function fakeGetPropertyDescriptor(obj, name) {
        if (name === 'ident___') { return undefined; }
        return originalProps.getPropertyDescriptor(obj, name);
      }
    });
  }

  defProp(Object, 'create', {
    value: function fakeCreate(parent, pdmap) {
      var result = originalProps.create(parent);
      identity(result);
      if (pdmap) {
        originalProps.defineProperties(result, pdmap);
      }
      return result;
    }
  });

  function constFunc(func) {
    Object.freeze(func.prototype);
    return Object.freeze(func);
  }

  WeakMap = function() {
    var identities = {};
    var values = {};

    function find(key) {
      var id = identity(key);
      var name;
      if (typeof id === 'string') {
        name = id;
        id = key;
      } else {
        name = id();
      }
      var opt_ids = identities[name];
      var i = opt_ids ? opt_ids.indexOf(id) : -1;
      // Using original freeze is safe since this record can't escape.
      return originalProps.freeze({
        name: name,
        id: id,
        opt_ids: opt_ids,
        i: i
      });
    }

    function get___(key, opt_default) {
      var f = find(key);
      return (f.i >= 0) ? values[f.name][f.i] : opt_default;
    }

    function has___(key) {
      return find(key).i >= 0;
    }

    function set___(key, value) {
      var f = find(key);
      var ids = f.opt_ids || (identities[f.name] = []);
      var vals = values[f.name] || (values[f.name] = []);
      var i = (f.i >= 0) ? f.i : ids.length;
      ids[i] = f.id;
      vals[i] = value;
    }

    function delete___(key) {
      var f = find(key);
      if (f.i < 0) { return false; }
      var ids = f.opt_ids;
      var last = ids.length - 1;
      if (last === 0) {
        delete identities[f.name];
        delete values[f.name];
      } else {
        var vals = values[f.name];
        ids[f.i] = ids[last];
        vals[f.i] = vals[last];
        ids.splice(last);
        vals.splice(last);
      }
      return true;
    }

    return Object.create(WeakMap.prototype, {
      get___: { value: constFunc(get___) },
      has___: { value: constFunc(has___) },
      set___: { value: constFunc(set___) },
      delete___: { value: constFunc(delete___) }
    });
  };
  WeakMap.prototype = Object.create(Object.prototype, {
    get: {
      /**
       * Return the value most recently associated with key, or
       * opt_default if none.
       */
      value: function get(key, opt_default) {
        return this.get___(key, opt_default);
      },
      writable: true,
      configurable: true
    },

    has: {
      /**
       * Is there a value associated with key in this WeakMap?
       */
      value: function has(key) {
        return this.has___(key);
      },
      writable: true,
      configurable: true
    },

    set: {
      /**
       * Associate value with key in this WeakMap, overwriting any
       * previous association if present.
       */
      value: function set(key, value) {
        this.set___(key, value);
      },
      writable: true,
      configurable: true
    },

    'delete': {
      /**
       * Remove any association for key in this WeakMap, returning
       * whether there was one.
       *
       * <p>Note that the boolean return here does not work like the
       * {@code delete} operator. The {@code delete} operator returns
       * whether the deletion succeeds at bringing about a state in
       * which the deleted property is absent. The {@code delete}
       * operator therefore returns true if the property was already
       * absent, whereas this {@code delete} method returns false if
       * the association was already absent.
       */
      value: function remove(key) {
        return this.delete___(key);
      },
      writable: true,
      configurable: true
    }
  });

})();
