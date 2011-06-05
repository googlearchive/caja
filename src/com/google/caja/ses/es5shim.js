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
 * "http://code.google.com/p/es-lab/wiki/SecureableES5" >secureable
 * ES5</a>.
 *
 * <p>Qualifying platforms generally include all JavaScript platforms
 * shown on <a href="http://kangax.github.com/es5-compat-table/"
 * >ECMAScript 5 compatibility table</a> that implement {@code
 * Object.getOwnPropertyNames}. At the time of this writing,
 * qualifying browsers already include the latest released versions of
 * Internet Explorer (9), Firefox (4), Chrome (11), and Safari
 * (5.0.5), their corresponding standalone (e.g., server-side) JavaScript
 * engines, and Rhino 1.73 and BESEN.
 *
 * <p>On such not-quite-ES5 platforms, some elements of these
 * emulations may lose SES safety, as enumerated in the comment on
 * each kludge-switch variable below. The platform must at least
 * provide Object.getOwnPropertyNames, because it cannot reasonably be
 * emulated.
 *
 * <p>This file is useful by itself, as it has no dependencies on the
 * rest of SES. It creates no new global bindings, but merely repairs
 * standard globals or standard elements reachable from standard
 * globals. If the future-standard {@code WeakMap} global is present,
 * as it is currently on FF7.0a1, then it will repair it in place. The
 * one non-standard element that this file uses is {@code console.log}
 * if present, in order to report the repairs it found necessary. If
 * {@code console.log} is absent, then this file performs its repairs
 * silently.
 *
 * <p>Generally, this file should be run as the first script in a
 * JavaScript context (i.e. a browser frame), as it replies on other
 * primordial objects and methods not yet being perturbed.
 *
 * TODO(erights): This file tries to protects itself from most
 * post-initialization perturbation, by stashing the primordials it
 * needs for later use, but this attempt is currently incomplete. For
 * example, the method wrappers installed if {@code
 * test_NEED_TO_WRAP_METHODS()} use the current binding of {@code
 * Function.prototype.apply} to access the wrapped method. We need to
 * revisit this when we support Confined-ES5, as a variant of SES in
 * which the primordials are not frozen.
 */
(function() {
  "use strict";

  function log(str) {
    if (typeof console !== 'undefined' && 'log' in console) {
      // We no longer test (typeof console.log === 'function') since,
      // on IE9 and IE10preview, in violation of the ES5 spec, it
      // is callable but has typeof "object".
      // TODO(erights): report to MS.
      console.log(str);
    }
  }

  if (!Object.getOwnPropertyNames) {
    var complaint = 'Please upgrade to a JavaScript platform ' +
      'which implements Object.getOwnPropertyNames';
    log(complaint);
    throw new EvalError(complaint);
  }

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
   */
  function test_MISSING_CALLEE_DESCRIPTOR() {
    function foo(){}
    if (Object.getOwnPropertyNames(foo).indexOf('callee') < 0) { return false; }
    if (foo.hasOwnProperty('callee')) {
      log('New symptom: empty strict function has own callee');
    } else {
      log('Phantom callee on strict functions. ' +
          'See https://bugs.webkit.org/show_bug.cgi?id=55537');
    }
    return true;
  }
  var TOLERATE_MISSING_CALLEE_DESCRIPTOR = test_MISSING_CALLEE_DESCRIPTOR();


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
  function test_REGEXP_CANT_BE_NEUTERED() {
    if (!RegExp.hasOwnProperty('leftContext')) { return false; }
    var deletion;
    try {
      deletion = delete RegExp.leftContext;
    } catch (err) {
      log('Cannot delete ambient mutable RegExp.leftContext. ' +
          'See https://bugzilla.mozilla.org/show_bug.cgi?id=591846');
      return true;
    }
    if (!RegExp.hasOwnProperty('leftContext')) { return false; }
    if (deletion) {
      log('New symptom: Deletion of RegExp.leftContext failed. ' +
          'See https://bugzilla.mozilla.org/show_bug.cgi?id=591846');
    } else {
      // strict delete should never return false, so if this happens
      // it indicates an additional bug in strict delete.
      log('A strict "delete RegExp.leftContext" returned false. ' +
          'See https://bugzilla.mozilla.org/show_bug.cgi?id=591846');
    }
    return true;
  }
  var TOLERATE_REGEXP_CANT_BE_NEUTERED = test_REGEXP_CANT_BE_NEUTERED();


  /**
   * Work around for http://code.google.com/p/google-caja/issues/detail?id=528
   *
   * <p>This kludge is safety preserving.
   */
  function test_REGEXP_TEST_EXEC_UNSAFE() {
    (/foo/).test('xfoox');
    var match = new RegExp('(.|\r|\n)*','').exec()[0];
    if (match === 'undefined') { return false; }
    if (match === 'xfoox') {
      log('RegExp.exec leaks match globally. ' +
          'See http://code.google.com/p/google-caja/issues/detail?id=528');
    } else {
      log('New symptom: regExp.exec() does not match against "undefined".');
    }
    return true;
  }
  var TOLERATE_REGEXP_TEST_EXEC_UNSAFE = test_REGEXP_TEST_EXEC_UNSAFE();


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
  function test_MISSING_FREEZE_ETC() {
    if ('freeze' in Object) { return false; }
    log('Object.freeze is missing. ' +
        'See https://bugs.webkit.org/show_bug.cgi?id=55736');
    return true;
  }
  var TOLERATE_MISSING_FREEZE_ETC = test_MISSING_FREEZE_ETC();


  /**
   * Workaround for https://bugs.webkit.org/show_bug.cgi?id=26382
   *
   * <p>As of this writing, the only major browser that does implement
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
  function test_MISSING_BIND() {
    if ('bind' in Function.prototype) { return false; }
    log('Function.prototype.bind is missing. ' +
        'See https://bugs.webkit.org/show_bug.cgi?id=26382');
    return true;
  }
  var TOLERATE_MISSING_BIND = test_MISSING_BIND();


  /**
   * Workaround for http://code.google.com/p/google-caja/issues/detail?id=1362
   *
   * <p>This is an unfortunate oversight in the ES5 spec: Even if
   * Date.prototype is frozen, it is still defined to be a Date, and
   * so has mutable state in internal properties that can be mutated
   * by the primordial mutation methods on Date.prototype, such as
   * {@code Date.prototype.setFullYear}.
   *
   * <p>This kludge is safety preserving.
   */
  function test_MUTABLE_DATE_PROTO() {
    try {
      Date.prototype.setFullYear(1957);
    } catch (err) {
      if (err instanceof TypeError) { return false; }
      log('New symptom: Mutating Date.prototype failed with ' + err);
      return true;
    }
    var v = Date.prototype.getFullYear();
    if (v !== v && typeof v === 'number') {
      // NaN indicates we're probably ok.
      return false;
    }
    if (v === 1957) {
      log('Date.prototype is a global communication channel. ' +
          'See http://code.google.com/p/google-caja/issues/detail?id=1362');
    } else {
      log('New symptom: Mutating Date.prototype did not throw');
    }
    return true;
  }
  var TOLERATE_MUTABLE_DATE_PROTO = test_MUTABLE_DATE_PROTO();


  /**
   * Workaround for https://bugzilla.mozilla.org/show_bug.cgi?id=656828
   *
   * <p>A bug in the current FF6.0a1 implementation: Even if
   * WeakMap.prototype is frozen, it is still defined to be a WeakMap,
   * and so has mutable state in internal properties that can be
   * mutated by the primordial mutation methods on WeakMap.prototype,
   * such as {@code WeakMap.prototype.set}.
   *
   * <p>This kludge is safety preserving.
   *
   * <p>TODO(erights): Update the ES spec page to reflect the current
   * agreement with Mozilla.
   */
  function test_MUTABLE_WEAKMAP_PROTO() {
    if (typeof WeakMap !== 'function') { return false; }
    var x = {};
    try {
      WeakMap.prototype.set(x, 86);
    } catch (err) {
      if (err instanceof TypeError) { return false; }
      log('New symptom: Mutating WeakMap.prototype failed with ' + err);
      return true;
    }
    var v = WeakMap.prototype.get(x);
    if (v === 86) {
      log('WeakMap.prototype is a global communication channel. ' +
          'See https://bugzilla.mozilla.org/show_bug.cgi?id=656828');
    } else {
      log('New symptom: Mutating WeakMap.prototype did not throw');
    }
    return true;
  }
  var TOLERATE_MUTABLE_WEAKMAP_PROTO = test_MUTABLE_WEAKMAP_PROTO();


  /**
   * TODO(erights): isolate and report the V8 bug mentioned below.
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
   * contract.
   *
   * <p>Although we have not yet diagnosed the motivating bug, as far
   * as we can tell, this kludge is safety preserving.
   */
  function test_NEED_TO_WRAP_METHODS() {
    if (!(/Chrome/).test(navigator.userAgent)) { return false; }
    log('Workaround undiagnosed need to wrap some methods.');
    return true;
  }
  var METHODS_TO_WRAP = [];
  if (test_NEED_TO_WRAP_METHODS()) {
    METHODS_TO_WRAP = [{base: Array.prototype, name: 'forEach'}];
  }

  /**
   * TODO(erights): isolate and report the V8 bug mentioned below.
   *
   * <p>Sometimes, when trying to freeze an object containing an
   * accessor property with a getter but no setter, Chrome fails with
   * <blockquote>Uncaught TypeError: Cannot set property ident___ of
   * #<Object> which has only a getter</blockquote>. So if necessary,
   * this kludge overrides {@code Object.defineProperty} to always
   * install a dummy setter in lieu of the absent one.
   *
   * <p>TODO(erights): We should also override {@code
   * Object.getOwnPropertyDescriptor} to hide the presence of the
   * dummy setter, and instead report an absent setter.
   */
  function test_NEEDS_DUMMY_SETTER() {
    if (!(/Chrome/).test(navigator.userAgent)) { return false; }
    log('Workaround undiagnosed need for dummy setter.');
    return true;
  }
  var TOLERATE_NEEDS_DUMMY_SETTER = test_NEEDS_DUMMY_SETTER();

  /**
   * Work around for https://bugzilla.mozilla.org/show_bug.cgi?id=637994
   *
   * <p>On Firefoxes at least 4 through 7.0a1, an inherited
   * non-configurable accessor property appears to be an own property
   * of all objects which inherit this accessor property.
   *
   * <p>Our workaround wraps hasOwnProperty, getOwnPropertyNames, and
   * getOwnPropertyDescriptor to heuristically decide when an accessor
   * property looks like it is apparently own because of this bug, and
   * suppress reporting its existence.
   *
   * <p>However, it is not feasible to likewise wrap JSON.stringify,
   * and this bug will cause JSON.stringify to be misled by inherited
   * enumerable non-configurable accessor properties. To prevent this,
   * we wrap defineProperty, freeze, and seal to prevent the creation
   * of <i>enumerable</i> non-configurable accessor properties on
   * those platforms with this bug.
   *
   * <p>A little known fact about JavaScript is that {@code
   * Object.prototype.propertyIsEnumerable} actually tests whether a
   * property is both own and enumerable. Assuming that our wrapping
   * of defineProperty, freeze, and seal prevents the occurrence of an
   * enumerable non-configurable accessor property, it should also
   * prevent the occurrence of this bug for any enumerable property,
   * and so we do not need to wrap propertyIsEnumerable.
   *
   * <p>This kludge seems to be safety preserving, but the issues are
   * delicate and not well understood.
   */
  function test_ACCESSORS_INHERIT_AS_OWN() {
    var base = {};
    var derived = Object.create(base);
    function getter() { return 'gotten'; }
    Object.defineProperty(base, 'foo', {get: getter});
    if (!derived.hasOwnProperty('foo') &&
        Object.getOwnPropertyDescriptor(derived, 'foo') === undefined &&
        Object.getOwnPropertyNames(derived).indexOf('foo') < 0) {
      return false;
    }
    if (derived.hasOwnProperty('foo') &&
        Object.getOwnPropertyDescriptor(derived, 'foo').get === getter &&
        Object.getOwnPropertyNames(derived).indexOf('foo') >= 0) {
      log('Accessor properties inherit as own properties. ' +
          'See https://bugzilla.mozilla.org/show_bug.cgi?id=637994');
    } else {
      log('New symptom: ' +
          'Accessor properties partially inherit as own properties.');
    }
    Object.defineProperty(base, 'bar', {get: getter, configurable: true});
    if (!derived.hasOwnProperty('bar') &&
        Object.getOwnPropertyDescriptor(derived, 'bar') === undefined &&
        Object.getOwnPropertyNames(derived).indexOf('bar') < 0) {
      return true;
    }
    log('New symptom: ' +
        'Accessor properties inherit as own even if configurable.');
    return true;
  }
  var TOLERATE_ACCESSORS_INHERIT_AS_OWN = test_ACCESSORS_INHERIT_AS_OWN();



  //////////////// END KLUDGE SWITCHES ///////////

  var hop = Object.prototype.hasOwnProperty;
  var slice = Array.prototype.slice;
  var objToString = Object.prototype.toString;
  var defProp = Object.defineProperty;
  var getPrototypeOf = Object.getPrototypeOf;


  if (TOLERATE_MISSING_CALLEE_DESCRIPTOR) {
    (function(realGOPN) {
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

  if (TOLERATE_REGEXP_CANT_BE_NEUTERED) {
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

  if (TOLERATE_REGEXP_TEST_EXEC_UNSAFE) {
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


  if (TOLERATE_MUTABLE_DATE_PROTO) {
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

  if (TOLERATE_MUTABLE_WEAKMAP_PROTO) {
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


  if (TOLERATE_NEEDS_DUMMY_SETTER) {
   (function() {
      var defProp = Object.defineProperty;
      var gopd = Object.getOwnPropertyDescriptor;
      var freeze = Object.freeze;
      var complained = false;

      defProp(Object, 'defineProperty', {
        value: function(base, name, desc) {
          function dummySetter(newValue) {
            if (name === 'ident___') {
              // The setter for ident___ seems to be called during
              // the built-in freeze, which indicates an
              // undiagnosed bug. By the logic of initSES, it should
              // be impossible to call the ident___ setter.
              // TODO(erights): isolate and report this.
              if (!complained) {
                log('Undiagnosed call to setter for ident___');
                complained = true;
              }
              //
              // If the following debugger line is uncommented, then
              // under the Chrome debugger, this crashes the page.
              // TODO(erights): isolate and report this.
              //
              //debugger;
            } else {
              throw new TypeError('Cannot set ".' + name + '"');
            }
          }
          freeze(dummySetter.prototype);
          freeze(dummySetter);

          var oldDesc = gopd(base, name);
          var testBase = {};
          if (oldDesc) {
            defProp(testBase, name, oldDesc);
          }
          defProp(testBase, name, desc);
          var fullDesc = gopd(testBase, name);

          if ('get' in fullDesc && fullDesc.set === undefined) {
            fullDesc.set = dummySetter;
          }
          return defProp(base, name, fullDesc);
        }
      });
    })();
  }


  if (TOLERATE_ACCESSORS_INHERIT_AS_OWN) {
    (function(){
      // restrict these
      var defProp = Object.defineProperty;
      var freeze = Object.freeze;
      var seal = Object.seal;

      // preserve illusion
      var hop = Object.prototype.hasOwnProperty;
      var gopn = Object.getOwnPropertyNames;
      var gopd = Object.getOwnPropertyDescriptor;

      var complaint = 'Workaround for ' +
        'https://bugzilla.mozilla.org/show_bug.cgi?id=637994 ' +
        ' prohibits enumerable non-configurable accessor properties.';

      function isBadAccessor(derived, name) {
        var desc = gopd(derived, name);
        if (!desc || !('get' in desc)) { return false; }
        var base = getPrototypeOf(derived);
        if (!base) { return false; }
        var superDesc = gopd(base, name);
        if (!superDesc || !('get' in superDesc)) { return false; }
        return (desc.get &&
                !desc.configurable && !superDesc.configurable &&
                desc.get === superDesc.get &&
                desc.set === superDesc.set &&
                desc.enumerable === superDesc.enumerable);
      }

      defProp(Object, 'defineProperty', {
        value: function definePropertyWrapper(base, name, desc) {
          var oldDesc = gopd(base, name);
          var testBase = {};
          if (oldDesc && !isBadAccessor(base, name)) {
            defProp(testBase, name, oldDesc);
          }
          defProp(testBase, name, desc);
          var fullDesc = gopd(testBase, name);

          if ('get' in fullDesc &&
              fullDesc.enumerable &&
              !fullDesc.configurable) {
            log(complaint);
            throw new TypeError(complaint);
          }
          return defProp(base, name, fullDesc);
        }
      });

      function ensureSealable(base) {
        gopn(base).forEach(function(name) {
          var desc = gopd(base, name);
          if ('get' in desc && desc.enumerable) {
            if (!desc.configurable) {
              log('New symptom: "' + name + '" already non-configurable');
            }
            log(complaint);
            throw new TypeError(complaint);
          }
        });
      }

      defProp(Object, 'freeze', {
        value: function freezeWrapper(base) {
          ensureSealable(base);
          return freeze(base);
        }
      });

      defProp(Object, 'seal', {
        value: function sealWrapper(base) {
          ensureSealable(base);
          return seal(base);
        }
      });

      defProp(Object.prototype, 'hasOwnProperty', {
        value: function hasOwnPropertyWrapper(name) {
          return hop.call(this, name) && !isBadAccessor(this, name);
        }
      });

      defProp(Object, 'getOwnPropertyDescriptor', {
        value: function getOwnPropertyDescriptorWrapper(base, name) {
          if (isBadAccessor(base, name)) { return undefined; }
          return gopd(base, name);
        }
      });

      defProp(Object, 'getOwnPropertyNames', {
        value: function getOwnPropertyNamesWrapper(base) {
          return gopn(base).filter(function(name) {
            return !isBadAccessor(base, name);
          });
        }
      });

    })();
  }

})();
