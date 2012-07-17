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
 * Generic taming membrane implementation.
 *
 * @requires WeakMap
 * @overrides window
 * @provides TamingMembrane
 */
function TamingMembrane(privilegedAccess, schema) {

  'use strict';

  var feralByTame = new WeakMap();
  var tameByFeral = new WeakMap();

  // Useless value provided as a safe 'this' value to functions.
  feralByTame.set(privilegedAccess.USELESS, privilegedAccess.USELESS);
  tameByFeral.set(privilegedAccess.USELESS, privilegedAccess.USELESS);

  function isNumericName(n) {
    return typeof n === 'number' || ('' + (+n)) === n;
  }

  function preventExtensions(o) {
    return ((void 0) === o) ? (void 0) : Object.preventExtensions(o);
  }

  /**
   * Records that f is t's feral twin and t is f's tame twin.
   * <p>
   * A <i>feral</i> object is one safe to make accessible to trusted
   * but possibly innocent host code. A <i>tame</i> object is one
   * safe to make accessible to untrusted guest
   * code. tamesTo(f, t) records that f is feral, that t is tamed,
   * and that they are in one-to-one correspondence so that
   * tame(f) === t and untame(t) === f.
   */
  function tamesTo(f, t) {
    if ((f && tameByFeral.has(f)) || (t && feralByTame.has(t))) {
      throw new TypeError('Attempt to multiply tame: ' + f + ', ' + t);
    }
    reTamesTo(f, t);
  }

  function reTamesTo(f, t) {
    var ftype = typeof f;
    if (!f || (ftype !== 'function' && ftype !== 'object')) {
      throw new TypeError('Unexpected feral primitive: ', f);
    }
    var ttype = typeof t;
    if (!t || (ttype !== 'function' && ttype !== 'object')) {
      throw new TypeError('Unexpected tame primitive: ', t);
    }

    tameByFeral.set(f, t);
    feralByTame.set(t, f);
    schema.fix(f);
  }

  /**
   * Private utility functions to tame and untame arrays.
   */
  function tameArray(fa) {
    var ta = [];
    for (var i = 0; i < fa.length; i++) {
      ta[i] = tame(privilegedAccess.getProperty(fa, i));
    }
    return Object.freeze(ta);
  }

  function untameArray(ta) {
    var fa = [];
    for (var i = 0; i < ta.length; i++) {
      privilegedAccess.setProperty(fa, i, untame(ta[i]));
    }
    return Object.freeze(fa);
  }

  function errGet(p) {
    return Object.freeze(function() {
      throw new TypeError('Unreadable property: ' + p);
    });
  }

  function errSet(p) {
    return Object.freeze(function() {
      throw new TypeError('Unwriteable property: ' + p);
    });
  }

  /**
   * Returns a tame object representing f, or undefined on failure.
   */
  function tame(f) {
    if (!f) { return f; }
    var ftype = typeof f;
    if (ftype !== 'function' && ftype !== 'object') {
      // Primitive value; tames to self
      return f;
    } else if (Array.isArray(f)) {
      // No tamesTo(...) for arrays; we copy across the membrane
      return tameArray(f);
    }
    if (tameByFeral.has(f)) { return tameByFeral.get(f); }
    if (privilegedAccess.isDefinedInCajaFrame(f)) { return f; }
    var t = void 0;
    if (ftype === 'object') {
      var ctor = privilegedAccess.directConstructor(f);
      if (ctor === void 0) {
        throw new TypeError('Cannot determine ctor of: ' + f);
      } else if (ctor === privilegedAccess.BASE_OBJECT_CONSTRUCTOR) {
        t = preventExtensions(tameRecord(f));
      } else {
        t = preventExtensions(tamePreviouslyConstructedObject(f, ctor));
      }
    } else if (ftype === 'function') {
      switch (schema.tameAs.get(f)) {
        case schema.tameTypes.CONSTRUCTOR:
          t = tameCtor(f, schema.tameCtorSuper.get(f), schema.tameFunctionName.get(f));
          break;
        case schema.tameTypes.FUNCTION:
          t = tamePureFunction(f, schema.tameFunctionName.get(f));
          break;
        case schema.tameTypes.XO4A:
          t = tameXo4a(f, schema.tameFunctionName.get(f));
          break;
        default:
          t = void 0;
          break;
      }
    }
    if (t) {
      privilegedAccess.banNumerics(t);
      tamesTo(f, t);
    }

    return t;
  }

  function isValidPropertyName(p) {
    return !/.*__$/.test(p);
  }

  // Tame a feral record by iterating over all own properties of the feral
  // record and installing a property handler for each one. Tame object is not
  // frozen; that is up to the caller to do when appropriate.
  function tameRecord(f, t) {
    if (!t) { t = {}; }
    var readOnly = schema.tameAs.get(f) === schema.tameTypes.READ_ONLY_RECORD;
    privilegedAccess.getOwnPropertyNames(f).forEach(function(p) {
      if (isNumericName(p)) { return; }
      if (!isValidPropertyName(p)) { return; }
      var get = function() {
        return tame(privilegedAccess.getProperty(f, p));
      };
      var set = readOnly ? undefined :
          function(v) {
            privilegedAccess.setProperty(f, p, untame(v));
            return v;
          };
      Object.defineProperty(t, p, {
        enumerable: true,
        configurable: false,
        get: get,
        set: set ? set : errSet(p)
      });
    });
    return t;
  }

  function tamePreviouslyConstructedObject(f, fc) {
    if (schema.tameAs.get(fc) !== schema.tameTypes.CONSTRUCTOR) { return void 0; }
    var tc = tame(fc);
    var t = Object.create(tc.prototype);
    tameObjectWithMethods(f, t);
    preventExtensions(t);
    return t;
  }

  function addFunctionPropertyHandlers(f, t) {
    schema.grantAs.getProps(f).forEach(function(p) {
      if (!isValidPropertyName(p)) { return; }
      var get = !schema.grantAs.has(f, p, schema.grantTypes.READ) ? undefined :
          function() {
            return tame(privilegedAccess.getProperty(f, p));
          };
      var set = !schema.grantAs.has(f, p, schema.grantTypes.WRITE) ? undefined :
          function(v) {
            privilegedAccess.setProperty(f, p, untame(v));
            return v;
          };
      if (get || set) {
        Object.defineProperty(t, p, {
          enumerable: true,
          configurable: false,
          get: get ? get : errGet(p),
          set: set ? set : errSet(p)
        });
      }
    });
  }

  function tamePureFunction(f) {
    var t = function(_) {
      // Since it's by definition useless, there's no reason to bother
      // passing untame(USELESS); we just pass USELESS itself.
      return tame(
          schema.applyFeralFunction(
              f,
              privilegedAccess.USELESS,
              untameArray(arguments)));
    };
    addFunctionPropertyHandlers(f, t);
    preventExtensions(t);
    return t;
  }

  function tameCtor(f, fSuper, name) {
    var fPrototype = privilegedAccess.getProperty(f, 'prototype');

    var t = function (_) {
      // Avoid being used as a general-purpose xo4a
      if (!(this instanceof t)) { return; }
      var o = Object.create(fPrototype);
      schema.applyFeralFunction(f, o, untameArray(arguments));
      tameObjectWithMethods(o, this);
      tamesTo(o, this);
      preventExtensions(this);
      privilegedAccess.banNumerics(this);
    };

    if (tameByFeral.get(fPrototype)) {
      throw new TypeError(
          'Prototype of constructor ' + f + ' has already been tamed');
    }

    var tPrototype = (function() {
      if (!fSuper || (fSuper === privilegedAccess.getObjectCtorFor(fSuper))) {
        return {};
      }
      if (!schema.tameAs.get(fSuper) === schema.tameTypes.CONSTRUCTOR) {
        throw new TypeError('Super ctor ' + fSuper + ' not granted as such');
      }
      var tSuper = tame(fSuper);
      return Object.create(tSuper.prototype);
    })();

    tameObjectWithMethods(fPrototype, tPrototype);

    Object.defineProperty(tPrototype, 'constructor', {
      writable: false,
      configurable: false,
      enumerable: true,
      value: t
    });

    privilegedAccess.banNumerics(tPrototype);
    Object.freeze(tPrototype);

    tamesTo(fPrototype, tPrototype);

    // FIXME(ihab.awad): Investigate why this fails *only* in ES53 mode
    // t.name = name;

    t.prototype = tPrototype;
    Object.freeze(t);

    return t;
  }

  function tameXo4a(f) {
    var t = function(_) {
      return tame(
          schema.applyFeralFunction(
              f,
              untame(this),
              untameArray(arguments)));
    };
    addFunctionPropertyHandlers(f, t);
    preventExtensions(t);
    return t;
  }

  function makePrototypeMethod(proto, func) {
    return function(_) {
      if (!inheritsFrom(this, proto)) {
        throw new TypeError('Target object not permitted: ' + this);
      }
      return func.apply(this, arguments);
    };
  }

  function inheritsFrom(o, proto) {
    while (o) {
      if (o === proto) { return true; }
      o = Object.getPrototypeOf(o);
    }
    return false;
  }

  function tameObjectWithMethods(f, t) {
    if (!t) { t = {}; }
    schema.grantAs.getProps(f).forEach(function(p) {
      var get = undefined;
      var set = undefined;
      if (!isValidPropertyName(p)) { return; }
      if (schema.grantAs.has(f, p, schema.grantTypes.METHOD)) {
        get = function() {
          // Note we return a different method each time because we may be
          // called with a different 'this' (the method property may be
          // inherited by begotten objects)
          var self = this;
          return function(_) {
            return tame(
              schema.applyFeralFunction(
                  privilegedAccess.getProperty(f, p),
                  untame(self),
                  untameArray(arguments)));
          };
        };
        Object.defineProperty(t, p, {
          enumerable: true,
          configurable: false,
          get: get,
          set: errSet(p)
        });
      } else {
        get = !schema.grantAs.has(f, p, schema.grantTypes.READ) ? undefined :
          makePrototypeMethod(t, function() {
            return tame(privilegedAccess.getProperty(untame(this), p));
          });
        set = !schema.grantAs.has(f, p, schema.grantTypes.WRITE) ? undefined :
          makePrototypeMethod(t, function(v) {
            privilegedAccess.setProperty(untame(this), p, untame(v));
            return v;
          });
        if (get || set) {
          Object.defineProperty(t, p, {
            enumerable: true,
            configurable: false,
            get: get ? get : errGet(p),
            set: set ? set : errSet(p)
          });
        }
      }
    });
    return t;
  }

  /**
   * Returns a feral object representing t, or undefined on failure.
   */
  function untame(t) {
    if (!t) { return t; }
    var ttype = typeof t;
    if (ttype !== 'function' && ttype !== 'object') {
      // Primitive value; untames to self
      return t;
    } else if (Array.isArray(t)) {
      // No tamesTo(...) for arrays; we copy across the membrane
      return untameArray(t);
    }
    if (feralByTame.has(t)) { return feralByTame.get(t); }
    if (!privilegedAccess.isDefinedInCajaFrame(t)) {
      throw new TypeError('Host object leaked without being tamed');
    }
    var f = void 0;
    if (ttype === 'object') {
      var ctor = privilegedAccess.directConstructor(t);
      if (ctor === privilegedAccess.BASE_OBJECT_CONSTRUCTOR) {
        f = untameCajaRecord(t);
      } else {
        // Check for built-ins
        var tclass = ({}).toString.call(t);
        switch (tclass) {
          case '[object Boolean]':
          case '[object Date]':
          case '[object Number]':
          case '[object RegExp]':
          case '[object String]':
            f = new ctor(t.valueOf()); break;
          default:
            throw new TypeError(
                'Untaming of guest constructed objects unsupported: ' + t);
        }
      }
    } else if (ttype === 'function') {
      f = Object.freeze(untameCajaFunction(t));
    }
    if (f) { tamesTo(f, t); }
    return f;
  }

  function untameCajaFunction(t) {
    // Untaming of *constructors* which are defined in Caja is unsupported.
    // We untame all functions defined in Caja as xo4a.
    return function(_) {
      return untame(t.apply(tame(this), tameArray(arguments)));
    };
  }

  function untameCajaRecord(t) {
    return privilegedAccess.isES5Browser
        ? untameCajaRecordByPropertyHandlers(t)
        : untameCajaRecordByEvisceration(t);
  }

  function untameCajaRecordByEvisceration(t) {
    var f = {};
    privilegedAccess.eviscerate(t, f, untame);
    tameRecord(f, t);
    return f;
  }

  function untameCajaRecordByPropertyHandlers(t) {
    var f = {};
    Object.getOwnPropertyNames(t).forEach(function(p) {
      if (isNumericName(p)) { return; }
      var d = Object.getOwnPropertyDescriptor(t, p);
      var read = d.get || d.hasOwnProperty('value');
      var write = d.set || (d.hasOwnProperty('value') && d.writable);
      var get = !read ? undefined :
          function() {
             return untame(t[p]);
          };
      var set = !write ? undefined :
          function(v) {
            t[p] = tame(v);
            return v;
          };
      if (get || set) {
        Object.defineProperty(f, p, {
          enumerable: true,
          configurable: false,
          get: get ? get : errGet(p),
          set: set ? set : errSet(p)
        });
      }
    });
    return preventExtensions(f);
  }

  function hasTameTwin(f) {
    return tameByFeral.has(f);
  }

  function hasFeralTwin(t) {
    return feralByTame.has(t);
  }

  return Object.freeze({
    tame: tame,
    untame: untame,
    tamesTo: tamesTo,
    reTamesTo: reTamesTo,
    hasTameTwin: hasTameTwin,
    hasFeralTwin: hasFeralTwin
  });
}

// Exports for closure compiler.
if (typeof window !== 'undefined') {
  window['TamingMembrane'] = TamingMembrane;
}
