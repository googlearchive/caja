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
 * @requires ___, cajaVM
 * @provides taming
 */

var taming = (function() {

  ////////////////////////////////////////////////////////////////////////
  // Primitive objective membrane
  ////////////////////////////////////////////////////////////////////////

  // Property whitelisting markers on tamings of host objects. This is disjoint
  // from the ES5/3 property descriptor material to allow local reasoning about
  // the taming layer in isolation. The markers are deliberately inheritable
  // via the prototype chain.
  //
  // Attribute suffixes ending with '_twl___' (for "taming white list") are
  // claimed by this implementation.
  //
  // For any given property called 'name', and a tamed twin 't', we have:
  //
  //   t[name + '_r_twl___'] is truthy    means 'name' is tamed as whitelisted
  //                                      for reading
  //
  //   t[name + '_w_twl___'] is truthy    means 'name' is tamed as whitelisted
  //                                      for writing or deleting
  //
  //   t[name + '_m_twl___'] is truthy    means 'name' is tamed as whitelisted
  //                                      for invocation as a method
  //
  // Additionally, the following marker is supported:
  //
  //   t['readonly_twl___'] is truthy     means the tame object 't' is tamed
  //                                      such that it appears to Caja code as
  //                                      read-only.

  /////////////////////////////////////////////////////////////////////////////
  // Material imported from es53.js

  var USELESS = ___.USELESS;
  var freeze = ___.freeze;
  var markFunc = ___.markFunc;
  var markFuncFreeze = ___.markFuncFreeze;
  var Token = cajaVM.Token;
  var isDefinedInCajaFrame = ___.isDefinedInCajaFrame;
  var rawDelete = ___.rawDelete;
  var getter = ___.getter;
  var setter = ___.setter;
  var directConstructor = ___.directConstructor;
  var BASE_OBJECT_CONSTRUCTOR = ___.BASE_OBJECT_CONSTRUCTOR;

  /////////////////////////////////////////////////////////////////////////////
  // Material copied from es53.js

  var endsWith__ = /__$/;
  var endsWith_e___ = /([\s\S]*)_e___$/;
  var startsWithNUM___ = /^NUM___/;

  function isNumericName(n) {
    return typeof n === 'number' || ('' + (+n)) === n;
  }

  /////////////////////////////////////////////////////////////////////////////
  // Material moved from es53.js

  /**
   * A value that makes a tamed constructor merely instantiate a tamed twin
   * with the proper prototype chain and return, rather than completing the
   * semantics of the original constructor. This value is private to this file.
   */
  var TAME_CTOR_CREATE_OBJECT_ONLY = Token('TAME_CTOR_CREATE_OBJECT_ONLY');

  /**
   * Precondition:
   * {@code obj} must not be {@code null} or {@code undefined}.
   * {@code name} must be a string or a number.
   * Postcondition:
   * If {@code name} is a number, a string encoding of a number, or
   * the string {@code 'NUM___'}, then we must return {@code false}.
   */
  function hasAccessor(obj, name) {
    var valueFlag = name + '_v___';
    return valueFlag in obj && !obj[valueFlag];
  }

  // TODO(ihab.awad): Test that host objects created using beget() -- in other
  // words, their prototype is neither 'Object' nor the prototype of a
  // previously tamed constructor function -- behave safely (even if the
  // behavior is a corner case).

  // The arguments to the following functions are as follows:
  //     t      -- a tamed twin
  //     mode   -- one of 'r', 'w' or 'm'
  //     p      -- a property name

  function isWhitelistedProperty(t, mode, p) {
    return !!t[p + '_' + mode + '_twl___'];
  }

  function whitelistProperty(t, mode, p) {
    t[p + '_' + mode + '_twl___'] = t;
  }

  function isWhitelistedReadOnly(t) {
    return !!t['readonly_twl___'];
  }

  function whitelistReadOnly(t) {
    t['readonly_twl___'] = t;
  }

  /**
   * Records that f is t's feral twin and t is f's tame twin.
   * <p>
   * A <i>feral</i> object is one safe to make accessible to trusted
   * but possibly innocent uncajoled code. A <i>tame</i> object is one
   * safe to make accessible to untrusted cajoled
   * code. ___tamesTo(f, t) records that f is feral, that t is tamed,
   * and that they are in one-to-one correspondence so that
   * ___.tame(f) === t and ___.untame(t) === f.
   * <p>
   * All primitives already tame and untame to themselves, so tamesTo
   * only accepts non-primitive arguments. The characteristic of being
   * marked tame or feral only applies to the object itself, not to
   * objects which inherit from it. TODO(erights): We should probably
   * check that a derived object does not get markings that conflict
   * with the markings on its base object.
   * <p>
   * Unlike the subjective membranes created by Domita, in this one,
   * the objects in a tame/feral pair point directly at each other,
   * and thereby retain each other. So long as one is non-garbage the
   * other will be as well.
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

    if (f.TAMED_TWIN___ === t && t.FERAL_TWIN___ === f) {
      throw new TypeError('Attempt to multiply tame: ' + f + ', ' + t);
    }

    // TODO(erights): Given that we maintain the invariant that
    // (f.TAMED_TWIN___ === t && hasOwnProp(f, 'TAMED_TWIN___')) iff
    // (t.FERAL_TWIN___ === f && hasOwnProp(t, 'FERAL_TWIN___')), then we
    // could decide to more delicately rely on this invariant and test
    // the backpointing rather than hasOwnProp below.
    if (f.TAMED_TWIN___ && f.hasOwnProperty('TAMED_TWIN___')) {
      throw new TypeError('Inconsistently tames to something: ', f);
    }
    if (t.FERAL_TWIN___ && t.hasOwnProperty('FERAL_TWIN___')) {
      throw new TypeError('Inconsistently untames to something: ', t);
    }

    f.TAMED_TWIN___ = t;
    t.FERAL_TWIN___ = f;
  }

  /**
   * Private utility functions to tame and untame arrays.
   */
  function tameArray(fa) {
    var ta = [];
    for (var i = 0; i < fa.length; i++) {
      ta[i] = tame(fa[i]);
    }
    return freeze(ta);
  }

  function untameArray(ta) {
    var fa = [];
    for (var i = 0; i < ta.length; i++) {
      fa[i] = untame(ta[i]);
    }
    return fa;
  }

  /**
   * Returns a tame object representing f, or undefined on failure.
   * <ol>
   * <li>All primitives tame and untame to themselves. Therefore,
   *     undefined is only a valid failure indication after checking
   *     that the argument is not undefined.
   * <li>If f has a registered tame twin, return that.
   * <li>If f is a Record, call tameRecordWithPureFunctions(f).
   *     We break Records out as a special case since the only thing
   *     all Records inherit from is Object.prototype, which everything
   *     else inherits from as well.
   * <li>If f is a Function, call tameFunction(f).
   * <li>Indicate failure by returning undefined.
   * </ol>
   * Record taming does not (yet?) deal with record inheritance.
   */
  function tame(f) {
    if (!f) { return f; }
    // Here we do use the backpointing test as a cheap hasOwnProp test.
    if (f.TAMED_TWIN___ && f.TAMED_TWIN___.FERAL_TWIN___ === f) {
      return f.TAMED_TWIN___;
    }
    var ftype = typeof f;
    if (ftype !== 'function' && ftype !== 'object') {
      // Primitive value; tames to self
      return f;
    } else if (Array.isArray(f)) {
      // No tamesTo(...) for arrays; we copy across the membrane
      return tameArray(f);
    }
    if (isDefinedInCajaFrame(f)) { return f; }
    var t = void 0;
    if (ftype === 'object') {
      var ctor = directConstructor(f);
      if (ctor === void 0) {
        throw new TypeError('Cannot determine ctor of: ' + f);
      } else if (ctor === BASE_OBJECT_CONSTRUCTOR) {
        t = tameRecord(f);
      } else {
        t = tamePreviouslyConstructedObject(f, ctor);
      }
    } else if (ftype === 'function') {
      // If not previously tamed via a 'markTameAs*' call, then it is
      // not a whitelisted function and should be neutered
      t = void 0;
    }
    if (t) { tamesTo(f, t); }
    return t;
  }

  function makeWhitelistingHasProperty(t, f, propertyModesToCheck) {
    return function(p) {
      p = '' + p;
      if (!(p in f)) { return false; }
      if (isNumericName(p)) { return false; }
      // 'propertyModesToCheck' is always statically defined to have
      // one or two elements. This method is *not* linear in the number
      // of properties an object has.
      for (var i = 0; i < propertyModesToCheck.length; i++) {
        if (isWhitelistedProperty(t, propertyModesToCheck[i], p)) {
            return true;
        }
      }
      return false;
    };
  }

  function makeEnumerate(t, f) {
    return function() {
      var result = {};
      for (var p in f) {
        if (t.HasProperty___(p)) {
          result.DefineOwnProperty___(p, {enumerable: true});
        }
      }
      return result;
    };
  }

  function addFunctionPropertyHandlers(t, f) {
    t.v___ = function (p) {  // [[Get]]
      p = '' + p;
      if (p === 'call' || p === 'bind' || p === 'apply') {
        return Function.prototype.v___.call(t, p);
      }
      if (isNumericName(p)) { return void 0; }
      if (!endsWith__.test(p)) {
        if (isWhitelistedProperty(t, 'r', p)) {
          return tame(f[p]);
        }
      }
      return void 0;
    };
    t.w___ = function (p, v) {  // [[Put]]
      p = '' + p;
      if (!isNumericName(p) && !endsWith__.test(p)) {
        if (isWhitelistedProperty(t, 'w', p) && !isWhitelistedReadOnly(t)) {
          f[p] = untame(v);
          return v;
        }
      }
      throw new TypeError('Not writeable: ' + p);
    };
    t.c___ = function (p) {  // [[Delete]]
      p = '' + p;
      if (!isNumericName(p) && !endsWith__.test(p)) {
        if (isWhitelistedProperty(t, 'w', p) && !isWhitelistedReadOnly(t)) {
          if (delete f[p]) { return true; }
        }
      }
      throw new TypeError('Not deleteable: ' + p);
    };
    t.HasProperty___ = makeWhitelistingHasProperty(t, f, [ 'r' ]);
    t.e___ = makeEnumerate(t, f);
  }

  function tameCtor(f, fSuper, name) {
    // TODO(ihab.awad): assign 'name'
    var instantiator = function () { };
    instantiator.prototype = f.prototype;

    var tPrototype = (function() {
      if (!fSuper || (fSuper === fSuper.FERAL_FRAME_OBJECT___)) { return {}; }
      var tSuper = fSuper.TAMED_TWIN___;
      if (!tSuper) { throw new TypeError('Super ctor not yet tamed'); }
      function tmp() { }
      tmp.prototype = tSuper.prototype;
      return new tmp();
    })();

    whitelistProperty(tPrototype, 'r', 'constructor');
    tameObjectWithMethods(f.prototype, tPrototype);
    tamesTo(f.prototype, tPrototype);

    var t = markFunc(function (_) {
      if (arguments.length > 0
          && arguments[0] === TAME_CTOR_CREATE_OBJECT_ONLY) {
        return;
      }
      var o = new instantiator();
      f.apply(o, untameArray(arguments));
      tameObjectWithMethods(o, this);
      tamesTo(o, this);
    });
    t.prototype = tPrototype;
    tPrototype.constructor = t;
    t.IS_TAMED_CTOR___ = t;

    addFunctionPropertyHandlers(t, f);

    whitelistProperty(t, 'r', 'prototype');

    return t;
  }

  function tamePureFunction(f) {
    var t = markFunc(function(_) {
      // Since it's by definition useless, there's no reason to bother
      // passing untame(USELESS); we just pass USELESS itself.
      return tame(f.apply(USELESS, untameArray(arguments)));
    });

    addFunctionPropertyHandlers(t, f);

    return t;
  }

  function tameXo4a(f) {
    var t = markFunc(function(_) {
      return tame(f.apply(untame(this), untameArray(arguments)));
    });

    addFunctionPropertyHandlers(t, f);

    return t;
  }

  function tameRecord(f, t) {
    if (!t) { t = {}; }
    t.v___ = function (p) {  // [[Get]]
      p = '' + p;
      if (isNumericName(p)) { return void 0; }
      if (!endsWith__.test(p)) {
        // Is p an accessor property? Used only for eviscerated t.
        var g = getter(t, p);
        if (g) { return g.f___(this); }

        // Accessor property with only setter
        if (hasAccessor(t, p)) {
          return void 0;
        }

        return tame(f[p]);
      }
      return void 0;
    };
    t.w___ = function (p, v) {  // [[Put]]
      p = '' + p;
      if (!isNumericName(p) && !endsWith__.test(p)) {
        // Is p an accessor property? Used only for eviscerated t.
        var s = setter(t, p);
        if (s) { s.f___(t, [v]); return v; }

        // Write value property if it is not a read-only accessor property
        // and the object itself is not read-only.
        if (!hasAccessor(t, p) && !isWhitelistedReadOnly(t)) {
          f[p] = untame(v);
          return v;
        }
      }
      throw new TypeError('Not writeable: ' + p);
    };
    t.c___ = function (p) {  // [[Delete]]
      p = '' + p;
      if (!isNumericName(p) && !endsWith__.test(p)) {
        if (!isWhitelistedReadOnly(t)) {
          if (t[p + '_v___']) {
            // Delete the local property if present. This only happens when
            // p is an accessor property on an eviscerated t.
            return Object.prototype.c___.call(t, p);
          } else {
            if (delete f[p]) { return true; }
          }
          // See http://code.google.com/p/google-caja/issues/detail?id=1392
          return;
        }
      }
      throw new TypeError('Not deleteable: ' + p);
    };
    t.m___ = function (p, as) {  // invoke method
      p = '' + p;
      var tf = t.v___(p);
      if ((typeof tf) === 'function') {
        // The property value is whitelisted to tame to a function, so call it
        return tf.apply(USELESS, as);
      }
      throw new TypeError('Not a function: ' + p);
    };
    t.HasProperty___ = function(p) {
      p = '' + p;
      if (isNumericName(p)) { return false; }
      return (p + '_v___' in this)
          || ((p in f) && !endsWith__.test(p));
    };

    var feralEnumerate = makeEnumerate(t, f);
    t.e___ = function() {
      var result = feralEnumerate();

      // For eviscerated t, add remaining properties from t (which will be
      // accessor properties only).
      for (var p in t) {
        if (!t.hasOwnProperty(p)) { continue; }
        if (isNumericName(p)) { continue; }
        if (startsWithNUM___.test(p) && endsWith__.test(p)) { continue; }
        var m = p.match(endsWith_e___);
        if (m) { result[p] = t[p]; }
      }
      return result;
    };

    return t;
  }

  function tameObjectWithMethods(f, t) {
    if (!t) { t = {}; }
    t.v___ = function (p) {  // [[Get]]
      p = '' + p;
      var fv = f[p];
      var fvt = typeof fv;
      if (fvt === 'function' && p === 'constructor') {
        // Special case to retrieve 'constructor' property,
        // which we automatically whitelist for reading
        return tame(f[p]);
      } else {
        if (fvt === 'function' && p !== 'constructor') {
          if (isWhitelistedProperty(t, 'm', p)) {
            return markFuncFreeze(function (_) {
              return tame(f[p].apply(f, untameArray(arguments)));
            });
          }
        } else if (isWhitelistedProperty(t, 'r', p)) {
          return tame(f[p]);
        }
      }
      return void 0;
    };
    t.w___ = function (p, v) {  // [[Put]]
      p = '' + p;
      if (!isNumericName(p) && !endsWith__.test(p)) {
        if (isWhitelistedProperty(t, 'w', p) && !isWhitelistedReadOnly(t)) {
          f[p] = untame(v);
          return v;
        }
      }
      throw new TypeError('Not writeable: ' + p);
    };
    t.c___ = function (p) {  // [[Delete]]
      p = '' + p;
      if (!isNumericName(p) && !endsWith__.test(p)) {
        if (isWhitelistedProperty(t, 'w', p) && !isWhitelistedReadOnly(t)) {
          if (delete f[p]) { return true; }
        }
      }
      throw new TypeError('Not deleteable: ' + p);
    };
    t.m___ = function (p, as) {  // invoke method
      p = '' + p;
      if (!isNumericName(p) && !endsWith__.test(p)) {
        if (typeof f[p] === 'function') {
          if (isWhitelistedProperty(t, 'm', p)) {
            return tame(f[p].apply(f, untameArray(as)));
          }
        }
      }
      throw new TypeError('Not a function: ' + p);
    };
    t.HasProperty___ = makeWhitelistingHasProperty(t, f, [ 'r', 'm' ]);
    t.e___ = makeEnumerate(t, f);

    return t;
  }

  function tamePreviouslyConstructedObject(f, fc) {
    var tc = tame(fc);
    if (tc && tc.IS_TAMED_CTOR___) {
      var t = new tc(TAME_CTOR_CREATE_OBJECT_ONLY);
      tameObjectWithMethods(f, t);
      return t;
    } else {
      return void 0;
    }
  }

  /**
   * Returns a feral object representing t, or undefined on failure.
   * <ol>
   * <li>All primitives tame and untame to themselves. Therefore,
   *     undefined is only a valid failure indication after checking
   *     that the argument is not undefined.
   * <li>If t has a registered feral twin, return that.
   */
  function untame(t) {
    if (!t) { return t; }
    // Here we do use the backpointing test as a cheap hasOwnProp test.
    if (t.FERAL_TWIN___ && t.FERAL_TWIN___.TAMED_TWIN___ === t) {
      return t.FERAL_TWIN___;
    }
    var ttype = typeof t;
    if (ttype !== 'function' && ttype !== 'object') {
      // Primitive value; untames to self
      return t;
    } else if (Array.isArray(t)) {
      // No tamesTo(...) for arrays; we copy across the membrane
      return untameArray(t);
    }
    if (!isDefinedInCajaFrame(t)) {
      throw new TypeError('Host object leaked without being tamed');
    }
    var f = void 0;
    if (ttype === 'object') {
      var ctor = directConstructor(t);
      if (ctor === BASE_OBJECT_CONSTRUCTOR) {
        f = untameCajaRecord(t);
      } else {
        throw new TypeError(
            'Untaming of guest constructed objects unsupported: ' + t);
      }
    } else if (ttype === 'function') {
      f = untameCajaFunction(t);
    }
    if (f) { tamesTo(f, t); }
    return f;
  }

  function untameCajaRecord(t) {
    var f = {};
    eviscerate(t, f);
    tameRecord(f, t);
    return f;
  }

  function untameCajaFunction(t) {
    // Taming of *constructors* which are defined in Caja is unsupported.
    // We tame all functions defined in Caja as xo4a (they receive the
    // 'this' value supplied by the host-side caller because the
    // ES53 compiler adds the necessary checks to make sure the
    // 'this' value they receive is safe.
    return function(_) {
      return untame(t.apply(tame(this), tameArray(arguments)));
    };
  }

  // Remove own value properties from tame object t and copy untame(them) to
  // feral object f, because t is about to be made into an object that forwards
  // to f.
  //
  // Accessor properties are left alone, because they cannot be implemented
  // on the host side.
  function eviscerate(t, f) {
    t.ownKeys___().forEach(function(p) {
      if (t[p + '_v___'] || isNumericName(p)) {
        // is a data property (not an accessor property)

        f[p] = untame(t[p]);

        if (!rawDelete(t, p)) {
          throw new TypeError(
              'Eviscerating: ' + t + ' failed to delete prop: ' + p);
        }
      }
    });
  }

  function markTameAsReadOnlyRecord(f) {
    if (isDefinedInCajaFrame(f)) {
      throw new TypeError('Taming controls not for Caja objects: ' + f);
    }
    if (f.TAMED_TWIN___) {
      throw new TypeError('Already tamed: ' + f);
    }
    var ftype = typeof f;
    if (ftype === 'object') {
       var ctor = directConstructor(f);
      if (ctor === BASE_OBJECT_CONSTRUCTOR) {
        var t =  tameRecord(f);
        whitelistReadOnly(t);
        tamesTo(f, t);
        return f;
      } else {
        throw new TypeError('Not instanceof Object: ' + f);
      }
    } else {
      throw new TypeError('Not an object: ' + f);
    }
  }

  function markTameAsFunction(f, name) {
    if (isDefinedInCajaFrame(f)) {
      throw new TypeError('Taming controls not for Caja objects: ' + f);
    }
    if (f.TAMED_TWIN___) {
      throw new TypeError('Already tamed: ' + f);
    }
    var t = tamePureFunction(f);
    tamesTo(f, t);
    return f;
  }

  function markTameAsCtor(ctor, opt_super, name) {
    if (isDefinedInCajaFrame(ctor)) {
      throw new TypeError('Taming controls not for Caja objects: ' + ctor);
    }
    if (ctor.TAMED_TWIN___) {
      throw new TypeError('Already tamed: ' + ctor);
    }
    var ctype = typeof ctor;
    var stype = typeof opt_super;
    if (ctype !== 'function') {
      throw new TypeError('Cannot tame ' + ctype + ' as ctor');
    }
    if (opt_super && stype !== 'function') {
      throw new TypeError('Cannot tame ' + stype + ' as superclass ctor');
    }
    var t = tameCtor(ctor, opt_super, name);
    tamesTo(ctor, t);
    return ctor;
  }

  function markTameAsXo4a(f, name) {
    if (isDefinedInCajaFrame(f)) {
      throw new TypeError('Taming controls not for Caja objects: ' + f);
    }
    if (f.TAMED_TWIN___) {
      throw new TypeError('Already tamed: ' + f);
    }
    if ((typeof f) !== 'function') {
      throw new TypeError('Not a function: ' + f);
    }
    var t = tameXo4a(f);
    tamesTo(f, t);
    return f;
  }

  function grantTameAsMethod(ctor, name) {
    if (isDefinedInCajaFrame(ctor)) {
      throw new TypeError('Taming controls not for Caja objects: ' + ctor);
    }
    if (!ctor.TAMED_TWIN___) {
      throw new TypeError('Not yet tamed: ' + ctor);
    }
    if (!ctor.TAMED_TWIN___.IS_TAMED_CTOR___ === ctor.TAMED_TWIN___) {
      throw new TypeError('Not a tamed ctor: ' + ctor);
    }
    var tameProto = tame(ctor.prototype);
    whitelistProperty(tameProto, 'm', name);
  }

  function grantTameAsRead(f, name) {
    if (isDefinedInCajaFrame(f)) {
      throw new TypeError('Taming controls not for Caja objects: ' + f);
    }
    var t = tame(f);
    whitelistProperty(t, 'r', name);
  }

  function grantTameAsReadWrite(f, name) {
    if (isDefinedInCajaFrame(f)) {
      throw new TypeError('Taming controls not for Caja objects: ' + f);
    }
    var t = tame(f);
    whitelistProperty(t, 'r', name);
    whitelistProperty(t, 'w', name);
  }

  return {
    tame: tame,
    untame: untame,
    tamesTo: tamesTo,
    markTameAsReadOnlyRecord: markTameAsReadOnlyRecord,
    markTameAsFunction: markTameAsFunction,
    markTameAsCtor: markTameAsCtor,
    markTameAsXo4a: markTameAsXo4a,
    grantTameAsMethod: grantTameAsMethod,
    grantTameAsRead: grantTameAsRead,
    grantTameAsReadWrite: grantTameAsReadWrite,
    USELESS: USELESS
  };
})();
