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

var RegExp;

/**
 * @fileoverview Monkey patch almost ES5 platforms into a closer
 * emulation of full <a href=
 * "http://code.google.com/p/es-lab/wiki/SecureableES5"
 * >secureable ES5</a>.
 *
 * <p>On not-quite-ES5 platforms, some elements of these emulations
 * may lose SES safety, as enumerated in the comment on each
 * kludge-switch variable below. The platform must at least provide
 * Object.getOwnPropertyNames, because it cannot reasonably be
 * emulated.
 */
(function() {
  "use strict";


  /////////////// KLUDGE SWITCHES ///////////////

  /////////////////////////////////
  // The following are only the minimal kludges needed for the current
  // Firefox, Safari, or the current Chrome Beta. At the time of
  // this writing, these are Firefox 4.0, Safari 5.0.4 (5533.20.27)
  // and Chrome 12.0.742.12 dev
  // As these move forward, kludges can be removed until we simply
  // rely on ES5.

  /**
   * Workaround for https://bugs.webkit.org/show_bug.cgi?id=55537
   *
   * <p>This kludge is safety preserving.
   *
   * <p>TODO(erights): Turning on this kludge is expensive, so we
   * should auto-detect at initialization time whether we need to on
   * this platform.
   */
  //var TOLERATE_MISSING_CALLEE_DESCRIPTOR = false;
  var TOLERATE_MISSING_CALLEE_DESCRIPTOR = true;

  /**
   * Work around for https://bugzilla.mozilla.org/show_bug.cgi?id=591846
   * as applied to the RegExp constructor.
   *
   * <p>Note that Mozilla lists this bug as closed. But reading that
   * bug thread clarifies that is partially because the following code
   * allows us to work around the non-configurability of the RegExp
   * statics.
   *
   * <p>This kludge is safety preserving.
   */
  //var REGEXP_CANT_BE_NEUTERED = false;
  var REGEXP_CANT_BE_NEUTERED = true;

  /**
   * Work around for http://code.google.com/p/google-caja/issues/detail?id=528
   *
   * <p>This kludge is safety preserving.
   */
  //var REGEXP_TEST_EXEC_UNSAFE = false;
  var REGEXP_TEST_EXEC_UNSAFE = true;

  /**
   * Workaround for https://bugs.webkit.org/show_bug.cgi?id=55736
   *
   * <p>As of this writing, the only major browser that does implement
   * Object.getOwnPropertyNames but not Object.freeze etc is the
   * released Safari 5 (JavaScriptCore). The Safari beta 5.0.4
   * (5533.20.27, r84622) already does, which is why this WebKit bug
   * is listed as closed. When the released Safari has this fix, we
   * can retire this kludge.
   *
   * <p>This kludge is <b>not</b> safety preserving. The emulations it
   * installs if needed do not actually provide the safety that the
   * rest of SES relies on.
   */
  //var TOLERATE_MISSING_FREEZE_ETC = false;
  var TOLERATE_MISSING_FREEZE_ETC = true;

  /**
   * Workaround for https://bugs.webkit.org/show_bug.cgi?id=26382.
   *
   * As of this writing, the only major browser that does implement
   * Object.getOwnPropertyNames but not Function.prototype.bind is
   * Safari 5 (JavaScriptCore), including the current Safari beta
   * 5.0.4 (5533.20.27, r84622).
   *
   * <p>This kludge is safety preserving. But see
   * https://bugs.webkit.org/show_bug.cgi?id=26382#c25 for why this
   * kludge cannot faithfully implement the specified semantics.
   *
   * <p>See also https://bugs.webkit.org/show_bug.cgi?id=42371
   */
  //var TOLERATE_MISSING_BIND = false;
  var TOLERATE_MISSING_BIND = true;

  /**
   * Workaround for an unfortunate oversight in the ES5 spec: Even if
   * Date.prototype is frozen, it is still defined to be a Date, and
   * so has mutable state in internal properties that can be mutated
   * by the primordial mutation methods on Date.prototype, such as
   * {@code Date.prototype.setFullYear}.
   *
   * <p>TODO(erights): find an appropriate venue to report this bug
   * and report it.
   */
  //var PATCH_MUTABLE_FROZEN_DATE_PROTO = false;
  var PATCH_MUTABLE_FROZEN_DATE_PROTO = true;

  /**
   * Workaround for a bug in the current FF6.0a1 implementation: Even
   * if WeakMap.prototype is frozen, it is still defined to be a
   * WeakMap, and so has mutable state in internal properties that can
   * be mutated by the primordial mutation methods on
   * WeakMap.prototype, such as {@code WeakMap.prototype.set}.
   *
   * <p>TODO(erights): Update the ES spec page to reflect the current
   * agreement with Mozilla, and file a bug against the current Mozilla
   * implementation.
   */
  //var PATCH_MUTABLE_FROZEN_WEAKMAP_PROTO = false;
  var PATCH_MUTABLE_FROZEN_WEAKMAP_PROTO = true;

  /**
   * <p>TODO(erights): isolate and report the V8 bug mentioned below.
   *
   * <p>This list of records represents the known occurrences of some
   * non-isolated, and thus, not yet reported bug on Chrome/v8 only,
   * that results in "Uncaught TypeError: Cannot redefine property:
   * defineProperty".
   *
   * <p>Each record consists of a base object and the name of a
   * built in method found on that base object. This method is
   * replaced with a strict wrapping function that preserves the
   * [[Call]] behavior of the original method but does not provoke the
   * undiagnosed Uncaught TypeError bug above.
   *
   * <p>Unfortunately, an ES5 strict method wrapper cannot emulate
   * absence of a [[Construct]] behavior, as specified for the Chapter
   * 15 built-in methods. The installed wrapper relies on {@code
   * Function.prototype.apply}, as inherited by original, obeying its
   * contract. TODO(erights): We need to revisit this when we support
   * Confined-ES5, as a variant of SES in which the primordials are
   * not frozen.
   *
   * <p>Although we have not yet diagnosed the motivating bug, as far
   * as we can tell, this kludge is safety preserving.
   */
  //var METHODS_TO_WRAP = [];
  var METHODS_TO_WRAP = [{base: Array.prototype, name: 'forEach'}];


  //////////////// END KLUDGE SWITCHES ///////////

  var hop = Object.prototype.hasOwnProperty;
  var slice = Array.prototype.slice;
  var objToString = Object.prototype.toString;
  var defProp = Object.defineProperty;
  var getPrototypeOf = Object.getPrototypeOf;

  if (TOLERATE_MISSING_CALLEE_DESCRIPTOR) {
    (function(realGOPN) {
      if (!realGOPN) {
        throw new EvalError('Please upgrade to a JavaScript platform ' +
                            'which implements Object.getOwnPropertyNames');
      }
      Object.getOwnPropertyNames = function calleeFix(base) {
        var result = realGOPN(base);
        if (typeof base === 'function') {
          var i = result.indexOf('callee');
          if (i >= 0 && !hop.call(base, 'callee')) {
            result.splice(i, 1);
          }
        }
        return result;
      };
    })(Object.getOwnPropertyNames);
  }

  if (REGEXP_CANT_BE_NEUTERED) {
    var UnsafeRegExp = RegExp;
    var FakeRegExp = function FakeRegExp(pattern, flags) {
      switch (arguments.length) {
        case 0: {
          return UnsafeRegExp();
        }
        case 1: {
          return UnsafeRegExp(pattern);
        }
        default: {
          return UnsafeRegExp(pattern, flags);
        }
      }
    };
    FakeRegExp.prototype = UnsafeRegExp.prototype;
    FakeRegExp.prototype.constructor = FakeRegExp;
    RegExp = FakeRegExp;
  }

  if (REGEXP_TEST_EXEC_UNSAFE) {
    var unsafeRegExpExec = RegExp.prototype.exec;
    var unsafeRegExpTest = RegExp.prototype.test;
    RegExp.prototype.exec = function fakeExec(specimen) {
      return unsafeRegExpExec.call(this, String(specimen));
    };
    RegExp.prototype.test = function fakeTest(specimen) {
      return unsafeRegExpTest.call(this, String(specimen));
    };
  }

  function patchMissingProp(base, name, missingFunc) {
    if (!(name in base)) {
      defProp(base, name, {
        value: missingFunc,
        writable: true,
        enumerable: false,
        configurable: true
      });
    }
  }

  if (TOLERATE_MISSING_FREEZE_ETC) {
    patchMissingProp(Object, 'freeze',
                     function fakeFreeze(obj) { return obj; });
    patchMissingProp(Object, 'seal',
                     function fakeSeal(obj) { return obj; });
    patchMissingProp(Object, 'preventExtensions',
                     function fakePreventExtensions(obj) { return obj; });
    patchMissingProp(Object, 'isFrozen',
                     function fakeIsFrozen(obj) { return false; });
    patchMissingProp(Object, 'isSealed',
                     function fakeIsSealed(obj) { return false; });
    patchMissingProp(Object, 'isExtensible',
                     function fakeIsExtensible(obj) { return true; });
  }

  if (TOLERATE_MISSING_BIND) {
    patchMissingProp(Function.prototype, 'bind',
                     function fakeBind(self, var_args) {
      var thisFunc = this;
      var leftArgs = slice.call(arguments, 1);
      function funcBound(var_args) {
        var args = leftArgs.concat(slice.call(arguments, 0));
        return thisFunc.apply(self, args);
      }
      delete funcBound.prototype;
      return funcBound;
    });
  }

  /**
   * Return a function suitable for using as a forEach argument on a
   * list of method names, where that function will monkey patch each
   * of these names methods on {@code constr.prototype} so that they
   * can't be called on {@code constr.prototype} itself even across
   * frames.
   *
   * <p>This only works when {@code constr} corresponds to an internal
   * [[Class]] property whose value is {@code classString}. To test
   * for {@code constr.prototype} cross-frame, we observe that for all
   * objects of this [[Class]], only the prototypes directly inherit
   * from an object that does not have this [[Class]].
   */
  function makeMutableProtoPatcher(constr, classString) {
    var proto = constr.prototype;
    var baseToString = objToString.call(proto);
    if (baseToString !== '[object ' + classString + ']') {
      throw new TypeError('unexpected: ' + baseToString);
    }
    if (getPrototypeOf(proto) !== Object.prototype) {
      throw new TypeError('unexpected inheritance: ' + classString);
    }
    function mutableProtoPatcher(name) {
      if (!hop.call(proto, name)) { return; }
      var originalMethod = proto[name];
      function replacement(var_args) {
        var parent = getPrototypeOf(this);
        if (objToString.call(parent) !== baseToString) {
          var thisToString = objToString.call(this);
          if (thisToString === baseToString) {
            throw new TypeError('May not mutate internal state of a ' +
                                classString + '.prototype');
          } else {
            throw new TypeError('Unexpected: ' + thisToString);
          }
        }
        return originalMethod.apply(this, arguments);
      }
      proto[name] = replacement;
    }
    return mutableProtoPatcher;
  }

  if (PATCH_MUTABLE_FROZEN_DATE_PROTO) {
    // Note: coordinate this list with maintenance of whitelist.js
    ['setYear',
     'setTime',
     'setFullYear',
     'setUTCFullYear',
     'setMonth',
     'setUTCMonth',
     'setDate',
     'setUTCDate',
     'setHours',
     'setUTCHours',
     'setMinutes',
     'setUTCMinutes',
     'setSeconds',
     'setUTCSeconds',
     'setMilliseconds',
     'setUTCMilliseconds'].forEach(makeMutableProtoPatcher(Date, 'Date'));
  }

  if (PATCH_MUTABLE_FROZEN_WEAKMAP_PROTO && typeof WeakMap === 'function') {
    // Note: coordinate this list with maintanence of whitelist.js
    ['set',
     'delete'].forEach(makeMutableProtoPatcher(WeakMap, 'WeakMap'));
  }

  // Since the METHODS_TO_WRAP list may (and currently does)
  // contain {base: Array.prototype, name:'forEach'}, we loop
  // through these with a for loop rather than using the forEach
  // method itself.
  for (var i = 0, len = METHODS_TO_WRAP.length; i < len; i++) {
    var r = METHODS_TO_WRAP[i];
    (function(original) {
      r.base[r.name] = function wrapper(var_args) {
        return original.apply(this, arguments);
      };
    })(r.base[r.name]);
  }

})();
