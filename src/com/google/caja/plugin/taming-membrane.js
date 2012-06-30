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
function TamingMembrane(privilegedAccess) {

  'use strict';

  function PropertyFlags() {
    var map = WeakMap();
    return Object.freeze({
      has: function(obj, prop, flag) {
        prop = '' + prop;
        return map.has(obj) &&
            map.get(obj).hasOwnProperty(prop) &&
            map.get(obj)[prop].indexOf(flag) !== -1;
      },
      set: function(obj, prop, flag) {
        prop = '' + prop;
        if (!map.has(obj)) {
          // Note: Object.create(null) not supported in ES5/3
          map.set(obj, {});
        }
        var o = map.get(obj);
        if (!o.hasOwnProperty(prop)) {
          o[prop] = [];
        }
        if (o[prop].indexOf(flag) === -1) {
          o[prop].push(flag);
        }
      },
      getProps: function(obj) {
        if (!map.has(obj)) { return []; }
        return Object.getOwnPropertyNames(map.get(obj));
      }
    });
  }

  var grantTypes = Object.freeze({
    METHOD: 'method',
    READ: 'read',
    WRITE: 'write'
  });

  var grantAs = PropertyFlags();

  var tameTypes = Object.freeze({
    CONSTRUCTOR: 'constructor',
    FUNCTION: 'function',
    XO4A: 'xo4a',
    READ_ONLY_RECORD: 'read_only_record'
  });

  var tameAs = new WeakMap();

  var tameFunctionName = new WeakMap();
  var tameCtorSuper = new WeakMap();

  var feralByTame = new WeakMap();
  var tameByFeral = new WeakMap();

  var functionAdviceBefore = new WeakMap();
  var functionAdviceAfter = new WeakMap();
  var functionAdviceAround = new WeakMap();

  function composeAround(f) {
    // TODO(ihab.awad): Optimize so we don't create then discard several
    // new closures for each invocation
    var func = function(self, args) {
      return privilegedAccess.applyFunction(f, self, args);
    };
    if (functionAdviceAround.has(f)) {
      functionAdviceAround.get(f).forEach(function(advice) {
        var cur = func;  // capture value
        func = function(self, args) {
          return privilegedAccess.applyFunction(
              advice,
              privilegedAccess.USELESS,
              [cur, self, args]);
        };
      });
    }
    return func;
  }

  function composeBefore(f, self, args) {
    if (functionAdviceBefore.has(f)) {
      functionAdviceBefore.get(f).forEach(function(advice) {
        args = privilegedAccess.applyFunction(
            advice,
            privilegedAccess.USELESS,
            [f, self, args]);
      });
    }
    return args;
  }

  function composeAfter(f, self, result) {
    if (functionAdviceAfter.has(f)) {
      functionAdviceAfter.get(f).forEach(function(advice) {
        result = privilegedAccess.applyFunction(
            advice,
            privilegedAccess.USELESS,
            [f, self, result]);
      });
    }
    return result;
  }

  function applyFeralFunction(f, self, args) {
    return composeAfter(
        f,
        self,
        composeAround(f)(
            self,
            composeBefore(
                f,
                self,
                args)));
  }

  function checkCanControlTaming(f) {
    var to = typeof f;
    if (!f || (to !== 'function' && to !== 'object')) {
      throw new TypeError('Taming controls not for non-objects: ' + f);
    }
    if (tameByFeral.has(f)) {
      throw new TypeError('Taming controls not for already tamed: ' + f);
    }
    if (privilegedAccess.isDefinedInCajaFrame(f)) {
      throw new TypeError('Taming controls not for Caja objects: ' + f);
    }
  }

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
    var ftype = typeof f;
    if (!f || (ftype !== 'function' && ftype !== 'object')) {
      throw new TypeError('Unexpected feral primitive: ', f);
    }
    var ttype = typeof t;
    if (!t || (ttype !== 'function' && ttype !== 'object')) {
      throw new TypeError('Unexpected tame primitive: ', t);
    }

    if (tameByFeral.has(f)) {
      throw new TypeError('Attempt to multiply tame: ' + f + ', ' + t);
    }

    if (feralByTame.has(t)) {
      throw new TypeError('Attempt to multiply tame: ' + f + ', ' + t);
    }

    tameByFeral.set(f, t);
    feralByTame.set(t, f);
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
      switch (tameAs.get(f)) {
        case tameTypes.CONSTRUCTOR:
          t = tameCtor(f, tameCtorSuper.get(f), tameFunctionName.get(f));
          break;
        case tameTypes.FUNCTION:
          t = tamePureFunction(f, tameFunctionName.get(f));
          break;
        case tameTypes.XO4A:
          t = tameXo4a(f, tameFunctionName.get(f));
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
    var readOnly = tameAs.get(f) === tameTypes.READ_ONLY_RECORD;
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
    if (tameAs.get(fc) !== tameTypes.CONSTRUCTOR) { return void 0; }
    var tc = tame(fc);
    var t = Object.create(tc.prototype);
    tameObjectWithMethods(f, t);
    preventExtensions(t);
    return t;
  }

  function addFunctionPropertyHandlers(f, t) {
    grantAs.getProps(f).forEach(function(p) {
      if (!isValidPropertyName(p)) { return; }
      var get = !grantAs.has(f, p, grantTypes.READ) ? undefined :
          function() {
            return tame(privilegedAccess.getProperty(f, p));
          };
      var set = !grantAs.has(f, p, grantTypes.WRITE) ? undefined :
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
          applyFeralFunction(
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
      applyFeralFunction(f, o, untameArray(arguments));
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
      if (!tameAs.get(fSuper) === tameTypes.CONSTRUCTOR) {
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
          applyFeralFunction(
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
    grantAs.getProps(f).forEach(function(p) {
      var get = undefined;
      var set = undefined;
      if (!isValidPropertyName(p)) { return; }
      if (grantAs.has(f, p, grantTypes.METHOD)) {
        get = function() {
          // Note we return a different method each time because we may be
          // called with a different 'this' (the method property may be
          // inherited by begotten objects)
          var self = this;
          return function(_) {
            return tame(
              applyFeralFunction(
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
        get = !grantAs.has(f, p, grantTypes.READ) ? undefined :
          makePrototypeMethod(t, function() {
            return tame(privilegedAccess.getProperty(untame(this), p));
          });
        set = !grantAs.has(f, p, grantTypes.WRITE) ? undefined :
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

  function checkNonNumeric(prop) {
    if (isNumericName(prop)) {
      throw new TypeError('Cannot control numeric property names: ' + prop);
    }
  }

  ///////////////////////////////////////////////////////////////////////////

  function markTameAsReadOnlyRecord(f) {
    checkCanControlTaming(f);
    tameAs.set(f, tameTypes.READ_ONLY_RECORD);
    return f;
  }

  function markTameAsFunction(f, name) {
    checkCanControlTaming(f);
    tameAs.set(f, tameTypes.FUNCTION);
    tameFunctionName.set(f, name);
    return f;
  }

  function markTameAsCtor(ctor, opt_super, name) {
    checkCanControlTaming(ctor);
    var ctype = typeof ctor;
    var stype = typeof opt_super;
    if (ctype !== 'function') {
      throw new TypeError('Cannot tame ' + ctype + ' as ctor');
    }
    if (opt_super && stype !== 'function') {
      throw new TypeError('Cannot tame ' + stype + ' as superclass ctor');
    }
    tameAs.set(ctor, tameTypes.CONSTRUCTOR);
    tameFunctionName.set(ctor, name);
    tameCtorSuper.set(ctor, opt_super);
    return ctor;
  }

  function markTameAsXo4a(f, name) {
    checkCanControlTaming(f);
    var ftype = typeof f;
    if (ftype !== 'function') {
      throw new TypeError('Cannot tame ' + ftype + ' as function');
    }
    tameAs.set(f, tameTypes.XO4A);
    tameFunctionName.set(f, name);
    return f;
  }

  function grantTameAsMethod(f, prop) {
    checkCanControlTaming(f);
    checkNonNumeric(prop);
    grantAs.set(f, prop, grantTypes.METHOD);
  }

  function grantTameAsRead(f, prop) {
    checkCanControlTaming(f);
    checkNonNumeric(prop);
    grantAs.set(f, prop, grantTypes.READ);
  }

  function grantTameAsReadWrite(f, prop) {
    checkCanControlTaming(f);
    checkNonNumeric(prop);
    grantAs.set(f, prop, grantTypes.READ);
    grantAs.set(f, prop, grantTypes.WRITE);
  }

  // Met the ghost of Greg Kiczales at the Hotel Advice.
  // This is what I told him as I gazed into his eyes:
  // Objects were for contracts,
  // Functions made for methods,
  // Membranes made for interposing semantics around them!

  function adviseFunctionBefore(f, advice) {
    if (!functionAdviceBefore.get(f)) { functionAdviceBefore.set(f, []); }
    functionAdviceBefore.get(f).push(advice);
  }
  
  function adviseFunctionAfter(f, advice) {
    if (!functionAdviceAfter.get(f)) { functionAdviceAfter.set(f, []); }
    functionAdviceAfter.get(f).push(advice);
  }

  function adviseFunctionAround(f, advice) {
    if (!functionAdviceAround.get(f)) { functionAdviceAround.set(f, []); }
    functionAdviceAround.get(f).push(advice);
  }

  function hasTameTwin(f) {
    return tameByFeral.has(f);
  }

  ///////////////////////////////////////////////////////////////////////////

  return Object.freeze({
    tame: tame,
    markTameAsReadOnlyRecord: markTameAsReadOnlyRecord,
    markTameAsFunction: markTameAsFunction,
    markTameAsCtor: markTameAsCtor,
    markTameAsXo4a: markTameAsXo4a,
    grantTameAsMethod: grantTameAsMethod,
    grantTameAsRead: grantTameAsRead,
    grantTameAsReadWrite: grantTameAsReadWrite,
    adviseFunctionBefore: adviseFunctionBefore,
    adviseFunctionAfter: adviseFunctionAfter,
    adviseFunctionAround: adviseFunctionAround,
    untame: untame,
    tamesTo: tamesTo,
    hasTameTwin: hasTameTwin
  });
}

// Exports for closure compiler.
if (typeof window !== 'undefined') {
  window['TamingMembrane'] = TamingMembrane;
}
