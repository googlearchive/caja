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
 * engines, Rhino 1.73, BESEN.
 *
 * <p>On such not-quite-ES5 platforms, some elements of these
 * emulations may lose SES safety, as enumerated in the comment on
 * each kludge record in the {@code kludges} array below. The platform
 * must at least provide {@code Object.getOwnPropertyNames}, because
 * it cannot reasonably be emulated.
 *
 * <p>This file is useful by itself, as it has no dependencies on the
 * rest of SES. It creates no new global bindings, but merely repairs
 * standard globals or standard elements reachable from standard
 * globals. If the future-standard {@code WeakMap} global is present,
 * as it is currently on FF7.0a1, then it will repair it in place. The
 * one non-standard element that this file uses is {@code console} if
 * present, in order to report the repairs it found necessary, in
 * which case we use its {@code log, info, warn, and error}
 * methods. If {@code console.log} is absent, then this file performs
 * its repairs silently.
 *
 * <p>Generally, this file should be run as the first script in a
 * JavaScript context (i.e. a browser frame), as it replies on other
 * primordial objects and methods not yet being perturbed.
 *
 * <p>TODO(erights): This file tries to protects itself from most
 * post-initialization perturbation, by stashing the primordials it
 * needs for later use, but this attempt is currently incomplete. We
 * need to revisit this when we support Confined-ES5, as a variant of
 * SES in which the primordials are not frozen.
 */
(function(global) {
  "use strict";

  var logger;
  function logNowhere(str) {}

  if (typeof console !== 'undefined' && 'log' in console) {
    // We no longer test (typeof console.log === 'function') since,
    // on IE9 and IE10preview, in violation of the ES5 spec, it
    // is callable but has typeof "object".
    // TODO(erights): report to MS.

    // TODO(erights): This assumes without checking that if
    // console.log is present, then console has working log, info,
    // warn, and error methods. Check that this is actually the case
    // on all platforms we care about, or, if not, do something
    // fancier here.
    logger = console;
  } else {
    logger = {
      log: logNowhere,
      info: logNowhere,
      warn: logNowhere,
      error: logNowhere
    };
  }

  if (!Object.getOwnPropertyNames) {
    var complaint = 'Please upgrade to a JavaScript platform ' +
      'which implements Object.getOwnPropertyNames';
    logger.error(complaint);
    throw new EvalError(complaint);
  }

  /**
   * Tests for https://bugs.webkit.org/show_bug.cgi?id=64250
   *
   * <p>No workaround attempted. Just reporting that this platform is
   * not SES-safe.
   */
  function test_GLOBAL_LEAKS_FROM_GLOBAL_FUNCTION_CALLS() {
    global.___global_test_function___ = function() { return this; };
    var that = ___global_test_function___();
    delete global.___global_test_function___;
    if (that === void 0) { return false; }
    if (that === global) { return true; }
    logger.error('New symptom: this leaked as: ' + that);
    return true;
  }

  /**
   *
   */
  function test_GLOBAL_LEAKS_FROM_ANON_FUNCTION_CALLS() {
    var that = (function(){ return this; })();
    if (that === void 0) { return false; }
    if (that === global) { return true; }
    logger.error('New symptom: this leaked as: ' + that);
    return true;
  }

  /**
   * Tests for
   * https://bugs.webkit.org/show_bug.cgi?id=51097
   * https://bugs.webkit.org/show_bug.cgi?id=58338
   * http://code.google.com/p/v8/issues/detail?id=1437
   *
   * <p>No workaround attempted. Just reporting that this platform is
   * not SES-safe.
   */
  function test_GLOBAL_LEAKS_FROM_BUILTINS() {
    var v = {}.valueOf;
    var that = 'dummy';
    try {
      that = v();
    } catch (err) {
      if (err instanceof TypeError) { return false; }
      logger.error('New symptom: ' +
                   'valueOf() threw ' + err);
      return true;
    }
    return true;
  }


  /**
   * Workaround for https://bugs.webkit.org/show_bug.cgi?id=55736
   *
   * <p>As of this writing, the only major browser that does implement
   * Object.getOwnPropertyNames but not Object.freeze etc is the
   * released Safari 5 (JavaScriptCore). The Safari beta 5.0.4
   * (5533.20.27, r84622) already does implement freeze, which is why
   * this WebKit bug is listed as closed. When the released Safari has
   * this fix, we can retire this kludge.
   *
   * <p>This kludge is <b>not</b> safety preserving. The emulations it
   * installs if needed do not actually provide the safety that the
   * rest of SES relies on.
   */
  function test_MISSING_FREEZE_ETC() {
    return !('freeze' in Object);
  }


  /**
   * Workaround for https://bugs.webkit.org/show_bug.cgi?id=55537
   *
   * This bug is fixed on the latest Safari beta 5.0.5 (5533.21.1,
   * r88603). When the released Safari has this fix, we can retire
   * this kludge.
   *
   * <p>This kludge is safety preserving.
   */
  function test_MISSING_CALLEE_DESCRIPTOR() {
    function foo(){}
    if (Object.getOwnPropertyNames(foo).indexOf('callee') < 0) { return false; }
    if (foo.hasOwnProperty('callee')) {
      logger.error('New symptom: empty strict function has own callee');
    }
    return true;
  }


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
      if (!(err instanceof TypeError)) {
        logger.error('New symptom: deletion failed with ' + err);
      }
      return true;
    }
    if (!RegExp.hasOwnProperty('leftContext')) { return false; }
    if (deletion) {
      logger.error('New symptom: Deletion of RegExp.leftContext failed.');
    }
    return true;
  }


  /**
   * Work around for http://code.google.com/p/v8/issues/detail?id=1393
   *
   * <p>This kludge is safety preserving.
   */
  function test_REGEXP_TEST_EXEC_UNSAFE() {
    (/foo/).test('xfoox');
    var match = new RegExp('(.|\r|\n)*','').exec()[0];
    if (match === 'undefined') { return false; }
    if (match !== 'xfoox') {
      logger.error('New symptom: ' +
                   'regExp.exec() does not match against "undefined".');
    }
    return true;
  }


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
    return !('bind' in Function.prototype);
  }


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
      logger.error('New symptom: Mutating Date.prototype failed with ' + err);
      return true;
    }
    var v = Date.prototype.getFullYear();
    if (v !== v && typeof v === 'number') {
      // NaN indicates we're probably ok.
      return false;
    }
    if (v !== 1957) {
      logger.error('New symptom: Mutating Date.prototype did not throw');
    }
    return true;
  }


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
      logger.error('New symptom: ' +
                   'Mutating WeakMap.prototype failed with ' + err);
      return true;
    }
    var v = WeakMap.prototype.get(x);
    if (v !== 86) {
      logger.error('New symptom: Mutating WeakMap.prototype did not throw');
    }
    return true;
  }


  /**
   * Workaround for http://code.google.com/p/v8/issues/detail?id=1447
   *
   * <p>This bug is fixed as of V8 r8258 bleeding-edge, but is not yet
   * available in the latest dev-channel Chrome (13.0.782.15 dev).
   *
   * <p>Unfortunately, an ES5 strict method wrapper cannot emulate
   * absence of a [[Construct]] behavior, as specified for the Chapter
   * 15 built-in methods. The installed wrapper relies on {@code
   * Function.prototype.apply}, as inherited by original, obeying its
   * contract.
   *
   * <p>This kludge is safety preserving.
   */
  function test_NEED_TO_WRAP_FOREACH() {
    if (!('freeze' in Object)) {
      // Object.freeze is still absent on released Safari and would
      // cause a bogus bug detection in the following try/catch code.
      return false;
    }
    try {
      ['z'].forEach(function(){ Object.freeze(Array.prototype.forEach); });
      return false;
    } catch (err) {
      if (!(err instanceof TypeError)) {
        logger.error('New Symptom: freezing forEach failed with ' + err);
      }
      return true;
    }
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
    return (typeof navigator !== 'undefined' &&
            (/Chrome/).test(navigator.userAgent));
  }


  /**
   * Work around for https://bugzilla.mozilla.org/show_bug.cgi?id=637994
   *
   * <p>On Firefox 4 an inherited non-configurable accessor property
   * appears to be an own property of all objects which inherit this
   * accessor property. This is fixed as of Forefox Nightly 7.0a1
   * (2011-06-21).
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
   *
   * <p>As support for Domado, it is possible to override this
   * restriction by adding the flag 
   * "ses_ignoreBug_propertyWillAppearAsOwn" to the property
   * descriptor. We assume that applying JSON.stringify to DOM nodes
   * is not interesting. TODO(kpreid): But does the general 
   * possibility of creating objects which if inherited from create
   * apparent own properties on their children break any security
   * properties?
   */
  function test_ACCESSORS_INHERIT_AS_OWN() {
    var base = {};
    var derived = Object.create(base);
    function getter() { return 'gotten'; }
    Object.defineProperty(base, 'foo', {get: getter});
    if (!derived.hasOwnProperty('foo') &&
        Object.getOwnPropertyDescriptor(derived, 'foo') === void 0 &&
        Object.getOwnPropertyNames(derived).indexOf('foo') < 0) {
      return false;
    }
    if (!derived.hasOwnProperty('foo') ||
        Object.getOwnPropertyDescriptor(derived, 'foo').get !== getter ||
        Object.getOwnPropertyNames(derived).indexOf('foo') < 0) {
      logger.error('New symptom: ' +
                   'Accessor properties partially inherit as own properties.');
    }
    Object.defineProperty(base, 'bar', {get: getter, configurable: true});
    if (!derived.hasOwnProperty('bar') &&
        Object.getOwnPropertyDescriptor(derived, 'bar') === void 0 &&
        Object.getOwnPropertyNames(derived).indexOf('bar') < 0) {
      return true;
    }
    logger.error('New symptom: ' +
                 'Accessor properties inherit as own even if configurable.');
    return true;
  }


  /**
   * Workaround for http://code.google.com/p/v8/issues/detail?id=1360
   *
   * Our workaround wraps {@code sort} to wrap the comparefn.
   */
  function test_SORT_LEAKS_GLOBAL() {
    var that = 'dummy';
    [2,3].sort(function(x,y) { that = this; return x - y; });
    if (that === void 0) { return false; }
    if (that !== global) {
      logger.error('New symptom: ' +
                   'sort called comparefn with "this" === ' + that);
    }
    return true;
  }


  /**
   * Workaround for http://code.google.com/p/v8/issues/detail?id=1360
   *
   * <p>Our workaround wraps {@code replace} to wrap the replaceValue
   * if it's a function.
   */
  function test_REPLACE_LEAKS_GLOBAL() {
    var that = 'dummy';
    'x'.replace(/x/, function() { that = this; return 'y';});
    if (that === void 0) { return false; }
    if (that !== global) {
      logger.error('New symptom: replace called replaceValue function ' +
                   'with "this" === ' + that);
    }
    return true;
  }


  /**
   * Protect an 'in' with a try/catch to workaround a bug in Safari
   * WebKit Nightly Version 5.0.5 (5533.21.1, r89741).
   *
   * <p>See https://bugs.webkit.org/show_bug.cgi?id=63398
   *
   * <p>Notes: We're seeing exactly
   * <blockquote>
   *   New symptom (c): ('caller' in &lt;a bound function&gt;) threw:
   *   TypeError: Cannot access caller property of a strict mode
   *   function<br>
   *   New symptom (c): ('arguments' in &lt;a bound function&gt;)
   *   threw: TypeError: Can't access arguments object of a strict
   *   mode function
   * </blockquote>
   * which means we're skipping both the catch and the finally in
   * {@code has} while hitting the catch in {@code has2}. Further, if
   * we remove one of these finally clauses (forget which) and rerun
   * the example, if we're under the debugger the browser crashes. If
   * we're not, then the TypeError escapes both catches.
   */
  function has(base, name, baseDesc) {
    var result = void 0;
    try {
      result = name in base;
    } catch (err) {
      logger.error('New symptom (a): (\'' +
                   name + '\' in <' + baseDesc + '>) threw: ' + err);
      // treat this as a safe absence
      result = false;
      return false;
    } finally {
      if (result === void 0) {
        logger.error('New symptom (b): (\'' +
                     name + '\' in <' + baseDesc + '>) failed');
      }
    }
    return !!result;
  }

  function has2(base, name, baseDesc) {
    var result;
    try {
      result = has(base, name, baseDesc);
    } catch (err) {
      logger.error('New symptom (c): (\'' +
                   name + '\' in <' + baseDesc + '>) threw: ' + err);
      // treat this as a safe absence
      result = false;
      return false;
    } finally {
      if (result === void 0) {
        logger.error('New symptom (d): (\'' +
                     name + '\' in <' + baseDesc + '>) failed');
      }
    }
    return !!result;
  }

  /**
   * No workaround (yet?) for
   * https://bugzilla.mozilla.org/show_bug.cgi?id=591846 as applied to
   * "caller"
   */
  function test_BUILTIN_LEAKS_CALLER() {
    var map = Array.prototype.map;
    if (!has(map, 'caller', 'a builtin')) { return false; }
    try {
      delete map.caller;
    } catch (err) { }
    if (!has(map, 'caller', 'a builtin')) { return false; }
    function foo() { return map.caller; }
    // using Function so it'll be non-strict
    var testfn = Function('f', 'return [1].map(f)[0];');
    var caller;
    try {
      caller = testfn(foo);
    } catch (err) {
      if (err instanceof TypeError) { return false; }
      logger.error('New symptom: builtin "caller" failed with: ' + err);
      return true;
    }
    if (null === caller || void 0 === caller) { return false; }
    if (testfn === caller) { return true; }
    logger.error('New symptom: Unexpected "caller": ' + caller);
    return true;
  }

  /**
   * No workaround (yet?) for
   * https://bugzilla.mozilla.org/show_bug.cgi?id=591846 as applied to
   * "arguments"
   */
  function test_BUILTIN_LEAKS_ARGUMENTS() {
    var map = Array.prototype.map;
    if (!has(map, 'arguments', 'a builtin')) { return false; }
    try {
      delete map.arguments;
    } catch (err) { }
    if (!has(map, 'arguments', 'a builtin')) { return false; }
    function foo() { return map.arguments; }
    // using Function so it'll be non-strict
    var testfn = Function('f', 'return [1].map(f)[0];');
    var args;
    try {
      args = testfn(foo);
    } catch (err) {
      if (err instanceof TypeError) { return false; }
      logger.error('New symptom: builtin "arguments" failed with: ' + err);
      return true;
    }
    if (args === void 0 || args === null) { return false; }
    return true;
  }

  /**
   * Workaround for http://code.google.com/p/v8/issues/detail?id=893
   */
  function test_BOUND_FUNCTION_LEAKS_CALLER() {
    if (!('bind' in Function.prototype)) { return false; }
    function foo() { return bar.caller; }
    var bar = foo.bind({});
    if (!has2(bar, 'caller', 'a bound function')) { return false; }
    try {
      delete bar.caller;
    } catch (err) { }
    if (!has2(bar, 'caller', 'a bound function')) {
      logger.error('New symptom: "caller" on bound functions can be deleted.');
      return true;
    }
    // using Function so it'll be non-strict
    var testfn = Function('f', 'return f();');
    var caller;
    try {
      caller = testfn(bar);
    } catch (err) {
      if (err instanceof TypeError) { return false; }
      logger.error('New symptom: bound function "caller" failed with: ' + err);
      return true;
    }
    if ([testfn, void 0, null].indexOf(caller) >= 0) { return false; }
    logger.error('New symptom: Unexpected "caller": ' + caller);
    return true;
  }

  /**
   * Workaround for http://code.google.com/p/v8/issues/detail?id=893
   */
  function test_BOUND_FUNCTION_LEAKS_ARGUMENTS() {
    if (!('bind' in Function.prototype)) { return false; }
    function foo() { return bar.arguments; }
    var bar = foo.bind({});
    if (!has2(bar, 'arguments', 'a bound function')) { return false; }
    try {
      delete bar.arguments;
    } catch (err) { }
    if (!has2(bar, 'arguments', 'a bound function')) {
      logger.error('New symptom: ' +
                   '"arguments" on bound functions can be deleted.');
      return true;
    }
    // using Function so it'll be non-strict
    var testfn = Function('f', 'return f();');
    var args;
    try {
      args = testfn(bar);
    } catch (err) {
      if (err instanceof TypeError) { return false; }
      logger.error('New symptom: ' +
                   'bound function "arguments" failed with: ' + err);
      return true;
    }
    if (args === void 0 || args === null) { return false; }
    return true;
  }

  /**
   * Workaround for http://code.google.com/p/v8/issues/detail?id=621
   *
   */
  function test_JSON_PARSE_PROTO_CONFUSION() {
    var x = JSON.parse('{"__proto__":[]}');
    if (Object.getPrototypeOf(x) !== Object.prototype) {
      return true;
    }
    if (!Array.isArray(x.__proto__)) {
      logger.error('New symptom: JSON.parse did not set "__proto__" as ' +
                   'a regular property');
      return true;
    }
    return false;
  }


  //////////////// END KLUDGE SWITCHES ///////////

  var call = Function.prototype.call;
  var apply = Function.prototype.apply;

  var hop = Object.prototype.hasOwnProperty;
  var objToString = Object.prototype.toString;
  var slice = Array.prototype.slice;
  var concat = Array.prototype.concat;
  var defProp = Object.defineProperty;
  var getPrototypeOf = Object.getPrototypeOf;

  function repair_MISSING_CALLEE_DESCRIPTOR() {
    var realGOPN = Object.getOwnPropertyNames;
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
  }

  function repair_REGEXP_CANT_BE_NEUTERED() {
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

  function repair_REGEXP_TEST_EXEC_UNSAFE() {
    var unsafeRegExpExec = RegExp.prototype.exec;
    unsafeRegExpExec.call = call;
    var unsafeRegExpTest = RegExp.prototype.test;
    unsafeRegExpTest.call = call;

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

  function repair_MISSING_FREEZE_ETC() {
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

  function repair_MISSING_BIND() {
    patchMissingProp(Function.prototype, 'bind',
                     function fakeBind(self, var_args) {
      var thisFunc = this;
      var leftArgs = slice.call(arguments, 1);
      function funcBound(var_args) {
        var args = concat.call(leftArgs, slice.call(arguments, 0));
        return apply.call(thisFunc, self, args);
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
      throw new TypeError('unexpected: ' + baseToString + ' -- instead of '
          + '[object ' + classString + ']');
    }
    if (getPrototypeOf(proto) !== Object.prototype) {
      throw new TypeError('unexpected inheritance: ' + classString);
    }
    function mutableProtoPatcher(name) {
      if (!hop.call(proto, name)) { return; }
      var originalMethod = proto[name];
      originalMethod.apply = apply;
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
      defProp(proto, name, {value: replacement});
    }
    return mutableProtoPatcher;
  }


  function repair_MUTABLE_DATE_PROTO() {
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

  function repair_MUTABLE_WEAKMAP_PROTO() {
    // Note: coordinate this list with maintanence of whitelist.js
    ['set',
     'delete'].forEach(makeMutableProtoPatcher(WeakMap, 'WeakMap'));
  }


  function repair_NEED_TO_WRAP_FOREACH() {
    (function() {
      var forEach = Array.prototype.forEach;
      defProp(Array.prototype, 'forEach', {
        value: function forEachWrapper(callbackfn, opt_thisArg) {
          return apply.call(forEach, this, arguments);
        }
      });
    })();
  }


  function repair_NEEDS_DUMMY_SETTER() {
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
                logger.warn('Undiagnosed call to setter for ident___');
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

          if ('get' in fullDesc && fullDesc.set === void 0) {
            fullDesc.set = dummySetter;
          }
          return defProp(base, name, fullDesc);
        }
      });
    })();
  }


  function repair_ACCESSORS_INHERIT_AS_OWN() {
    (function(){
      // restrict these
      var defProp = Object.defineProperty;
      var freeze = Object.freeze;
      var seal = Object.seal;

      // preserve illusion
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
              !fullDesc.configurable &&
              !desc.ses_ignoreBug_propertyWillAppearAsOwn) {
            logger.warn(complaint);
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
              logger.error('New symptom: ' +
                           '"' + name + '" already non-configurable');
            }
            logger.warn(complaint);
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
          if (isBadAccessor(base, name)) { return void 0; }
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

  function repair_SORT_LEAKS_GLOBAL() {
   (function(){
      var unsafeSort = Array.prototype.sort;
      unsafeSort.call = call;
      function sortWrapper(opt_comparefn) {
        function comparefnWrapper(x, y) {
          return opt_comparefn(x, y);
        }
        if (arguments.length === 0) {
          return unsafeSort.call(this);
        } else {
          return unsafeSort.call(this, comparefnWrapper);
        }
      }
      defProp(Array.prototype, 'sort', { value: sortWrapper });
    })();
  }

  function repair_REPLACE_LEAKS_GLOBAL() {
    (function(){
      var unsafeReplace = String.prototype.replace;
      unsafeReplace.call = call;
      function replaceWrapper(searchValue, replaceValue) {
        var safeReplaceValue = replaceValue;
        function replaceValueWrapper(m1, m2, m3) {
          return replaceValue(m1, m2, m3);
        }
        if (typeof replaceValue === 'function') {
          safeReplaceValue = replaceValueWrapper;
        }
        return unsafeReplace.call(this, searchValue, safeReplaceValue);
      }
      defProp(String.prototype, 'replace', { value: replaceWrapper });
    })();
  }


  var kludges = [
    {
      description: 'Global object leaks from global function calls',
      test: test_GLOBAL_LEAKS_FROM_GLOBAL_FUNCTION_CALLS,
      repair: void 0,
      canRepairSafely: false,
      urls: ['https://bugs.webkit.org/show_bug.cgi?id=64250'],
      sections: ['10.2.1.2', '10.2.1.2.6'],
      tests: []
    },
    {
      description: 'Global object leaks from anonymous function calls',
      test: test_GLOBAL_LEAKS_FROM_ANON_FUNCTION_CALLS,
      repair: void 0,
      canRepairSafely: false,
      urls: [],
      sections: [],
      tests: []
    },
    {
      description: 'Global object leaks from built-in methods',
      test: test_GLOBAL_LEAKS_FROM_BUILTINS,
      repair: void 0,
      canRepairSafely: false,
      urls: ['https://bugs.webkit.org/show_bug.cgi?id=51097',
             'https://bugs.webkit.org/show_bug.cgi?id=58338',
             'http://code.google.com/p/v8/issues/detail?id=1437'],
      sections: ['15.2.4.4'],
      tests: ['S15.2.4.4_A14']
    },
    {
      description: 'Object.freeze is missing',
      test: test_MISSING_FREEZE_ETC,
      repair: repair_MISSING_FREEZE_ETC,
      canRepairSafely: false,
      urls: ['https://bugs.webkit.org/show_bug.cgi?id=55736'],
      sections: ['15.2.3.9'],
      tests: []
    },
    {
      description: 'Phantom callee on strict functions',
      test: test_MISSING_CALLEE_DESCRIPTOR,
      repair: repair_MISSING_CALLEE_DESCRIPTOR,
      canRepairSafely: true,
      urls: ['https://bugs.webkit.org/show_bug.cgi?id=55537'],
      sections: ['15.2.3.4'],
      tests: []
    },
    {
      description: 'Cannot delete ambient mutable RegExp.leftContext',
      test: test_REGEXP_CANT_BE_NEUTERED,
      repair: repair_REGEXP_CANT_BE_NEUTERED,
      canRepairSafely: true,
      urls: ['https://bugzilla.mozilla.org/show_bug.cgi?id=591846',
             'http://wiki.ecmascript.org/doku.php?id=' +
             'conventions:make_non-standard_properties_configurable'],
      sections: [],
      tests: []
    },
    {
      description: 'RegExp.exec leaks match globally',
      test: test_REGEXP_TEST_EXEC_UNSAFE,
      repair: repair_REGEXP_TEST_EXEC_UNSAFE,
      canRepairSafely: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=1393',
             'http://code.google.com/p/chromium/issues/detail?id=75740',
             'https://bugzilla.mozilla.org/show_bug.cgi?id=635017',
             'http://code.google.com/p/google-caja/issues/detail?id=528'],
      sections: ['15.10.6.2'],
      tests: ['S15.10.6.2_A12']
    },
    {
      description: 'Function.prototype.bind is missing',
      test: test_MISSING_BIND,
      repair: repair_MISSING_BIND,
      canRepairSafely: true,
      urls: ['https://bugs.webkit.org/show_bug.cgi?id=26382'],
      sections: ['15.3.4.5'],
      tests: []
    },
    {
      description: 'Date.prototype is a global communication channel',
      test: test_MUTABLE_DATE_PROTO,
      repair: repair_MUTABLE_DATE_PROTO,
      canRepairSafely: true,
      urls: ['http://code.google.com/p/google-caja/issues/detail?id=1362'],
      sections: ['15.9.5'],
      tests: []
    },
    {
      description: 'WeakMap.prototype is a global communication channel',
      test: test_MUTABLE_WEAKMAP_PROTO,
      repair: repair_MUTABLE_WEAKMAP_PROTO,
      canRepairSafely: true,
      urls: ['https://bugzilla.mozilla.org/show_bug.cgi?id=656828'],
      sections: [],
      tests: []
    },
    {
      description: 'Array forEach cannot be frozen while in progress',
      test: test_NEED_TO_WRAP_FOREACH,
      repair: repair_NEED_TO_WRAP_FOREACH,
      canRepairSafely: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=1447'],
      sections: ['15.4.4.18'],
      tests: ['S15.4.4.18_A1', 'S15.4.4.18_A2']
    },
    {
      description: 'Workaround undiagnosed need for dummy setter',
      test: test_NEEDS_DUMMY_SETTER,
      repair: repair_NEEDS_DUMMY_SETTER,
      canRepairSafely: true,
      urls: [],
      sections: [],
      tests: []
    },
    {
      description: 'Accessor properties inherit as own properties',
      test: test_ACCESSORS_INHERIT_AS_OWN,
      repair: repair_ACCESSORS_INHERIT_AS_OWN,
      canRepairSafely: true,
      urls: ['https://bugzilla.mozilla.org/show_bug.cgi?id=637994'],
      sections: [],
      tests: []
    },
    {
      description: 'Array sort leaks global',
      test: test_SORT_LEAKS_GLOBAL,
      repair: repair_SORT_LEAKS_GLOBAL,
      canRepairSafely: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=1360'],
      sections: ['15.4.4.11'],
      tests: ['S15.4.4.11_A8']
    },
    {
      description: 'String replace leaks global',
      test: test_REPLACE_LEAKS_GLOBAL,
      repair: repair_REPLACE_LEAKS_GLOBAL,
      canRepairSafely: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=1360'],
      sections: ['15.5.4.11'],
      tests: ['S15.5.4.11_A12']
    },
    {
      description: 'Built in functions leak "caller"',
      test: test_BUILTIN_LEAKS_CALLER,
      repair: void 0,
      canRepairSafely: false,
      urls: ['http://code.google.com/p/v8/issues/detail?id=1548',
             'https://bugzilla.mozilla.org/show_bug.cgi?id=591846',
             'http://wiki.ecmascript.org/doku.php?id=' +
             'conventions:make_non-standard_properties_configurable'],
      sections: [],
      tests: []
    },
    {
      description: 'Built in functions leak "arguments"',
      test: test_BUILTIN_LEAKS_ARGUMENTS,
      repair: void 0,
      canRepairSafely: false,
      urls: ['http://code.google.com/p/v8/issues/detail?id=1548',
             'https://bugzilla.mozilla.org/show_bug.cgi?id=591846',
             'http://wiki.ecmascript.org/doku.php?id=' +
             'conventions:make_non-standard_properties_configurable'],
      sections: [],
      tests: []
    },
    {
      description: 'Bound functions leak "caller"',
      test: test_BOUND_FUNCTION_LEAKS_CALLER,
      repair: repair_MISSING_BIND,
      canRepairSafely: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=893',
             'https://bugs.webkit.org/show_bug.cgi?id=63398'],
      sections: ['15.3.4.5'],
      tests: ['S15.3.4.5_A1']
    },
    {
      description: 'Bound functions leak "arguments"',
      test: test_BOUND_FUNCTION_LEAKS_ARGUMENTS,
      repair: repair_MISSING_BIND,
      canRepairSafely: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=893',
             'https://bugs.webkit.org/show_bug.cgi?id=63398'],
      sections: ['15.3.4.5'],
      tests: ['S15.3.4.5_A2']
    },
    {
      description: 'JSON.parse confused by "__proto__"',
      test: test_JSON_PARSE_PROTO_CONFUSION,
      repair: void 0,
      canRepairSafely: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=621',
             'http://code.google.com/p/v8/issues/detail?id=1310'],
      sections: ['15.12.2'],
      tests: ['S15.12.2_A1']
    }
  ];


  // first run all the tests before repairing anything
  // then repair all repairable failed tests
  // then run all the tests again, in case some repairs break other tests

  var beforeFailures = kludges.map(function(kludge) {
    return kludge.test();
  });
  var repairs = [];
  kludges.forEach(function(kludge, i) {
    if (beforeFailures[i]) {
      var repair = kludge.repair;
      if (repair && repairs.lastIndexOf(repair) === -1) {
        repair();
        // Same repair might fix multiple problems, but run at most once.
        repairs.push(repair);
      }
    }
  });
  var afterFailures = kludges.map(function(kludge) {
    return kludge.test();
  });

  var seemsSafe = true;

  kludges.forEach(function(kludge, i) {
    var status = '';
    var level = 'warn';
    if (beforeFailures[i]) { // failed before
      if (afterFailures[i]) { // failed after
        seemsSafe = false;
        level = 'error';
        if (kludge.repair) {
          status = 'Repair failed';
        } else {
          status = 'Not repaired';
        }
      } else {
        if (kludge.repair) {
          status = 'Repaired';
        } else {
          status = 'Accidentally repaired';
        }
      }
    } else { // succeeded before
      if (afterFailures[i]) { // failed after
        seemsSafe = false;
        level = 'error';
        status = 'Broken by other attempted repairs';
      } else { // succeeded after
        // nothing to see here, move along
        return;
      }
    }
    var note = '';
    if (!kludge.canRepairSafely) {
      seemsSafe = false;
      note = 'This platform is not SES-safe. ';
    }
    //logger[level](i + ' ' + status + ': ' +
    //              kludge.description + '. ' + note +
    //              // TODO(erights): select most relevant URL based on platform
    //              (kludge.urls[0] ? 'See ' + kludge.urls[0] : ''));
  });

  // TODO(erights): If we arrive here with the platform still in a
  // non-SES-safe state, we should indicate that somehow so that our
  // client (such as SES) can decide to abort. We should *not* simply
  // throw an exception, because that limits the utility of this
  // module for non-SES uses.
  return seemsSafe;

})(this);
