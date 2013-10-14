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
function TamingMembrane(helper, schema) {

  'use strict';

  var feralByTame = new WeakMap();
  var tameByFeral = new WeakMap();
  helper.weakMapPermitHostObjects(tameByFeral);

  // Useless value provided as a safe 'this' value to functions.
  feralByTame.set(helper.USELESS, helper.USELESS);
  tameByFeral.set(helper.USELESS, helper.USELESS);

  // Distinguished value returned when directConstructor() wishes to indicate
  // that the direct constructor of some object is the native primordial
  // "Object" function of some JavaScript frame.
  var BASE_OBJECT_CONSTRUCTOR = Object.freeze({});

  function directConstructor(obj) {
    if (obj === null) { return void 0; }
    if (obj === void 0) { return void 0; }
    if ((typeof obj) !== 'object') {
      // Regarding functions, since functions return undefined,
      // directConstructor() doesn't provide access to the
      // forbidden Function constructor.
      // Otherwise, we don't support finding the direct constructor
      // of a primitive.
      return void 0;
    }
    var directProto = Object.getPrototypeOf(obj);
    if (!directProto) { return void 0; }
    var directCtor = directProto.constructor;
    if (!directCtor) { return void 0; }
    helper.allFrames().forEach(function(w) {
      var O;
      try {
        O = w.Object;
      } catch (e) {
        // met a different-origin frame, probably
        return;
      }
      if (directCtor === O) {
        if (!Object.prototype.hasOwnProperty.call(directProto, 'constructor')) {
          // detect prototypes which just didn't bother to set .constructor and
          // inherited it from Object (Safari's DOMException is the motivating
          // case).
          directCtor = void 0;
        } else {
          directCtor = BASE_OBJECT_CONSTRUCTOR;
        }
      }
    });
    return directCtor;
  }

  function getObjectCtorFor(o) {
    if (o === undefined || o === null) {
      return void 0;
    }
    var ot = typeof o;
    if (ot !== 'object' && ot !== 'function') {
      throw new TypeError('Cannot obtain ctor for non-object');
    }
    var proto = undefined;
    while (o) {
      proto = o;
      o = Object.getPrototypeOf(o);
    }
    return proto.constructor;
  }

  function isNumericName(n) {
    return typeof n === 'number' || ('' + (+n)) === n;
  }

  function preventExtensions(o) {
    return ((void 0) === o) ? (void 0) : Object.preventExtensions(o);
  }

  // Applies a function 'feralFunction' ensuring that either the return
  // value is tamed or the thrown exception is tamed and rethrown.
  function applyFeralFunction(feralFunction, feralThis, feralArguments) {
    try {
      return tame(
          schema.applyFeralFunction(
              feralFunction,
              feralThis,
              feralArguments));
    } catch (e) {
      throw tameException(e);
    }
  }

  // Applies a guest-side function 'tameFunction' ensuring that either the
  // return value is untamed or the thrown exception is untamed and rethrown.
  function applyTameFunction(tameFunction, tameThis, tameArguments) {
    try {
      return untame(tameFunction.apply(tameThis, tameArguments));
    } catch (e) {
      throw untameException(e);
    }
  }

  function getFeralProperty(feralObject, feralProp) {
    try {
      return tame(feralObject[feralProp]);
    } catch (e) {
      throw tameException(e);
    }
  }

  function setFeralProperty(feralObject, feralProp, feralValue) {
    try {
      feralObject[feralProp] = feralValue;
    } catch (e) {
      throw tameException(e);
    }
  }

  function getTameProperty(tameObject, tameProp) {
    try {
      return untame(tameObject[tameProp]);
    } catch (e) {
      throw untameException(e);
    }
  }

  function setTameProperty(tameObject, tameProp, tameValue) {
    try {
      tameObject[tameProp] = tameValue;
    } catch (e) {
      throw untameException(e);
    }
  }

  // Given a builtin object "o" provided by either a guest or host frame,
  // return a copy constructed in the taming frame. Return undefined if
  // "o" is not a builtin object. Note that we only call this function if we
  // know that "o" is *not* a primitive.
  function copyBuiltin(o) {
    var t = void 0;
    switch (Object.prototype.toString.call(o)) {
      case '[object Boolean]':
        t = new Boolean(o.valueOf());
        break;
      case '[object Date]':
        t = new Date(o.valueOf());
        break;
      case '[object Number]':
        t = new Number(o.valueOf());
        break;
      case '[object RegExp]':
        t = new RegExp(
            o.source,
            (o.global ? 'g' : '') +
            (o.ignoreCase ? 'i' : '') +
            (o.multiline ? 'm' : ''));
        break;
      case '[object String]':
        t = new String(o.valueOf());
        break;
      case '[object Error]':
      case '[object DOMException]':
        // paranoia -- Error constructor is specified to stringify
        var msg = '' + o.message;
        var name = o.name;
        switch (name) {
          case 'Error':
            t = new Error(msg);
            break;
          case 'EvalError':
            t = new EvalError(msg);
            break;
          case 'RangeError':
            t = new RangeError(msg);
            break;
          case 'ReferenceError':
            t = new ReferenceError(msg);
            break;
          case 'SyntaxError':
            t = new SyntaxError(msg);
            break;
          case 'TypeError':
            t = new TypeError(msg);
            break;
          case 'URIError':
            t = new URIError(msg);
            break;
          // no case for DOMException as DOMException is not constructible
          // (and also not whitelisted, and in general more funky).
          default:
            t = new Error(msg);
            t.name = '' + name;
            break;
        }
    }
    return t;
  }

  // This is a last resort for passing a safe "demilitarized zone" exception
  // across the taming membrane in cases where passing the actual thrown
  // exception is either problematic or not known to be safe.
  function makeNeutralException(e) {
    var str = 'Error';
    try {
      str = e.toString();
    } catch (ex) {}
    return new Error(str);
  }

  function tameException(f) {
    var t = void 0;
    try { t = tame(f); } catch (e) {}
    if (t !== void 0) { return t; }
    return makeNeutralException(f);
  }

  function untameException(t) {
    var f = void 0;
    try { f = untame(t); } catch (e) {}
    if (f !== void 0) { return f; }
    return makeNeutralException(t);
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
      var et = tameByFeral.get(f);
      var ef = feralByTame.get(t);
      throw new TypeError('Attempt to multiply tame: ' + f + 
          (ef ? ' (already ' + (ef === f ? 'same' : ef) + ')' : '') +
          ' <-> ' + t +
          (et ? ' (already ' + (et === t ? 'same' : et) + ')' : ''));
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
      ta[i] = tame(fa[i]);
    }
    return Object.freeze(ta);
  }

  function untameArray(ta) {
    var fa = [];
    for (var i = 0; i < ta.length; i++) {
      fa[i] = untame(ta[i]);
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
    if (f !== Object(f)) {
      // Primitive value; tames to self
      return f;
    }
    var ftype = typeof f;
    if (Array.isArray(f)) {
      // No tamesTo(...) for arrays; we copy across the membrane
      return tameArray(f);
    }
    var t = tameByFeral.get(f);
    if (t) { return t; }
    if (feralByTame.has(f)) {
      throw new TypeError('Tame object found on feral side of taming membrane: '
          + f + '. The membrane has previously been compromised.');
    }
    if (ftype === 'object') {
      var ctor = directConstructor(f);
      if (ctor === BASE_OBJECT_CONSTRUCTOR) {
        t = preventExtensions(tameRecord(f));
      } else {
        t = copyBuiltin(f);
        if (t === void 0) {
          if (ctor === void 0) {
            throw new TypeError('Cannot determine ctor of: ' + f);
          } else {
            t = tamePreviouslyConstructedObject(f, ctor);
          }
        }
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
    Object.keys(f).forEach(function(p) {
      if (isNumericName(p)) { return; }
      if (!isValidPropertyName(p)) { return; }
      var get = function() {
        return getFeralProperty(f, p);
      };
      var set = readOnly ? undefined :
          function(v) {
            setFeralProperty(f, p, untame(v));
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
    if (schema.tameAs.get(fc) !== schema.tameTypes.CONSTRUCTOR) {
      return void 0;
    }
    var tc = tame(fc);
    var t = Object.create(tc.prototype);
    tameObjectWithMethods(f, t);
    return t;
  }

  function addFunctionPropertyHandlers(f, t) {
    schema.grantAs.getProps(f).forEach(function(p) {
      if (!isValidPropertyName(p)) { return; }
      var get = !schema.grantAs.has(f, p, schema.grantTypes.READ) ? undefined :
          function() {
            return getFeralProperty(f, p);
          };
      var set = !schema.grantAs.has(f, p, schema.grantTypes.WRITE) ? undefined :
          function(v) {
            setFeralProperty(f, p, untame(v));
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

  // CAUTION: It is ESSENTIAL that we pass USELESS, not (void 0), when
  // calling down to a feral function. That function may not be declared
  // in "strict" mode, and so would receive [window] as its "this" arg if
  // we called it with (void 0), which would be a serious vulnerability.

  function tamePureFunction(f) {
    var t = function(_) {
      return applyFeralFunction(
          f,
          helper.USELESS,  // See notes on USELESS above
          untameArray(arguments));
    };
    addFunctionPropertyHandlers(f, t);
    preventExtensions(t);
    return t;
  }

  function tameCtor(f, fSuper, name) {
    var fPrototype = f.prototype;

    var t = function (_) {
      if (!(this instanceof t)) {
        // Call as a function
        return applyFeralFunction(
            f,
            (void 0),
            untameArray(arguments));
      } else {
        // Call as a constructor
        var o = Object.create(fPrototype);
        applyFeralFunction(f, o, untameArray(arguments));
        tameObjectWithMethods(o, this);
        tamesTo(o, this);
      }
    };

    if (tameByFeral.get(fPrototype)) {
      throw new TypeError(
          'Prototype of constructor ' + f + ' has already been tamed');
    }

    tameRecord(f, t);

    var tPrototype = (function() {
      if (!fSuper || (fSuper === getObjectCtorFor(fSuper))) {
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
      return applyFeralFunction(
          f,
          untame(this),
          untameArray(arguments));
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

  function makeStrictPrototypeMethod(proto, func) {
    return function(_) {
      if ((this === proto) || !inheritsFrom(this, proto)) {
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

  function makePropertyGetter(f, t, p) {
    if (schema.grantAs.has(f, p, schema.grantTypes.METHOD)) {
      // METHOD access implies READ, and requires careful wrapping of the
      // feral method being exposed
      return makePrototypeMethod(t, function() {
        var self = this;
        return function(_) {
          return applyFeralFunction(
              untame(self)[p],
              untame(self),
              untameArray(arguments));
        };
      });
    } else if (schema.grantAs.has(f, p, schema.grantTypes.READ)) {
      // Default READ access implies normal taming of the property value
      return makePrototypeMethod(t, function() {
        return getFeralProperty(untame(this), p);
      });
    } else {
      return undefined;
    }
  }

  function makePropertySetter(f, t, p) {
    var override =
      schema.grantAs.has(f, p, schema.grantTypes.OVERRIDE) ||
      (schema.grantAs.has(f, p, schema.grantTypes.METHOD) &&
       schema.grantAs.has(f, p, schema.grantTypes.WRITE));

    if (override) {
      return makeStrictPrototypeMethod(t, function(v) {
        setFeralProperty(untame(this), p, untame(v));
        return v;
      });
    } else if (schema.grantAs.has(f, p, schema.grantTypes.WRITE)) {
      return makePrototypeMethod(t, function(v) {
        setFeralProperty(untame(this), p, untame(v));
        return v;
      });
    } else {
      return undefined;
    }
  }

  function defineObjectProperty(f, t, p) {
    var get = makePropertyGetter(f, t, p);
    var set = makePropertySetter(f, t, p);
    if (get || set) {
      Object.defineProperty(t, p, {
        enumerable: true,
        configurable: false,
        get: get ? get : errGet(p),
        set: set ? set : errSet(p)
      });
    }
  }

  function tameObjectWithMethods(f, t) {
    if (!t) { t = {}; }
    schema.grantAs.getProps(f).forEach(function(p) {
      if (isValidPropertyName(p)) {
        defineObjectProperty(f, t, p);
      }
    });
    return t;
  }

  /**
   * Returns a feral object representing t, or undefined on failure.
   */
  function untame(t) {
    if (t !== Object(t)) {
      // Primitive value; untames to self
      return t;
    }
    var ttype = typeof t;
    if (Array.isArray(t)) {
      // No tamesTo(...) for arrays; we copy across the membrane
      return untameArray(t);
    }
    var f = feralByTame.get(t);
    if (f) { return f; }
    if (tameByFeral.has(t)) {
      throw new TypeError('Feral object found on tame side of taming membrane: '
          + t + '. The membrane has previously been compromised.');
    }
    if (!helper.isDefinedInCajaFrame(t)) {
      throw new TypeError('Host object leaked without being tamed');
    }
    if (ttype === 'object') {
      var ctor = directConstructor(t);
      if (ctor === BASE_OBJECT_CONSTRUCTOR) {
        f = untameCajaRecord(t);
      } else {
        f = copyBuiltin(t);
        if (f === void 0) {
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
      return applyTameFunction(t, tame(this), tameArray(arguments));
    };
  }

  function untameCajaRecord(t) {
    var f = {};
    Object.getOwnPropertyNames(t).forEach(function(p) {
      var d = Object.getOwnPropertyDescriptor(t, p);
      var read = d.get || d.hasOwnProperty('value');
      var write = d.set || (d.hasOwnProperty('value') && d.writable);
      var get = !read ? undefined :
          function() {
             return getTameProperty(t, p);
          };
      var set = !write ? undefined :
          function(v) {
            setTameProperty(t, p, tame(v));
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
    hasFeralTwin: hasFeralTwin,
    
    // Any code which bypasses the membrane (e.g. in order to provide its own
    // tame twins, as Domado does) must also filter exceptions resulting from
    // control flow crossing the membrane.
    tameException: tameException,
    untameException: untameException
  });
}

// Exports for closure compiler.
if (typeof window !== 'undefined') {
  window['TamingMembrane'] = TamingMembrane;
}
