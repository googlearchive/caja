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
 * @fileoverview Monkey patch almost ES5 platforms into a closer
 * emulation of full <a href=
 * "http://code.google.com/p/es-lab/wiki/SecureableES5">Secureable
 * ES5</a>.
 *
 * <p>Assumes only ES3, but only proceeds to do useful repairs when
 * the platform is close enough to ES5 to be worth attempting
 * repairs. Compatible with almost-ES5, ES5, ES5-strict, and
 * anticipated ES6.
 *
 * <p>Ignore the "...requires ___global_test_function___" below. We
 * create it, use it, and delete it all within this module. But we
 * need to lie to the linter since it can't tell.
 *
 * //provides ses.statuses, ses.ok, ses.is, ses.makeDelayedTamperProof
 * //provides ses.makeCallerHarmless, ses.makeArgumentsHarmless
 * //provides ses.severities, ses.maxSeverity, ses.updateMaxSeverity
 * //provides ses.maxAcceptableSeverityName, ses.maxAcceptableSeverity
 *
 * @author Mark S. Miller
 * @requires ___global_test_function___, ___global_valueOf_function___
 * @requires JSON, navigator, this, eval, document
 * @overrides ses, RegExp, WeakMap, Object, parseInt, repairES5Module
 */
var RegExp;
var ses;

/**
 * <p>Qualifying platforms generally include all JavaScript platforms
 * shown on <a href="http://kangax.github.com/es5-compat-table/"
 * >ECMAScript 5 compatibility table</a> that implement {@code
 * Object.getOwnPropertyNames}. At the time of this writing,
 * qualifying browsers already include the latest released versions of
 * Internet Explorer (9), Firefox (4), Chrome (11), and Safari
 * (5.0.5), their corresponding standalone (e.g., server-side) JavaScript
 * engines, Rhino 1.73, and BESEN.
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
 * which case we use its {@code log, info, warn}, and {@code error}
 * methods. If {@code console.log} is absent, then this file performs
 * its repairs silently.
 *
 * <p>Generally, this file should be run as the first script in a
 * JavaScript context (i.e. a browser frame), as it relies on other
 * primordial objects and methods not yet being perturbed.
 *
 * <p>TODO(erights): This file tries to protect itself from some
 * post-initialization perturbation by stashing some of the
 * primordials it needs for later use, but this attempt is currently
 * incomplete. We need to revisit this when we support Confined-ES5,
 * as a variant of SES in which the primordials are not frozen. See
 * previous failed attempt at <a
 * href="http://codereview.appspot.com/5278046/" >Speeds up
 * WeakMap. Preparing to support unfrozen primordials.</a>. From
 * analysis of this failed attempt, it seems that the only practical
 * way to support CES is by use of two frames, where most of initSES
 * runs in a SES frame, and so can avoid worrying about most of these
 * perturbations.
 */
(function repairES5Module(global) {
  "use strict";

  /**
   * The severity levels.
   *
   * <dl>
   *   <dt>MAGICAL_UNICORN</dt><dd>Unachievable magical mode used for testing.
   *   <dt>SAFE</dt><dd>no problem.
   *   <dt>SAFE_SPEC_VIOLATION</dt>
   *     <dd>safe (in an integrity sense) even if unrepaired. May
   *         still lead to inappropriate failures.</dd>
   *   <dt>NO_KNOWN_EXPLOIT_SPEC_VIOLATION</dt>
   *     <dd>known to introduce an indirect safety issue which,
   *     however, is not known to be exploitable.</dd>
   *   <dt>UNSAFE_SPEC_VIOLATION</dt>
   *     <dd>a safety issue only indirectly, in that this spec
   *         violation may lead to the corruption of assumptions made
   *         by other security critical or defensive code.</dd>
   *   <dt>NOT_OCAP_SAFE</dt>
   *     <dd>a violation of object-capability rules among objects
   *         within a coarse-grained unit of isolation.</dd>
   *   <dt>NOT_ISOLATED</dt>
   *     <dd>an inability to reliably sandbox even coarse-grain units
   *         of isolation.</dd>
   *   <dt>NEW_SYMPTOM</dt>
   *     <dd>some test failed in a way we did not expect.</dd>
   *   <dt>NOT_SUPPORTED</dt>
   *     <dd>this platform cannot even support SES development in an
   *         unsafe manner.</dd>
   * </dl>
   */
  ses.severities = {
    MAGICAL_UNICORN:       { level: -1, description: 'Testing only' },
    SAFE:                  { level: 0, description: 'Safe' },
    SAFE_SPEC_VIOLATION:   { level: 1, description: 'Safe spec violation' },
    NO_KNOWN_EXPLOIT_SPEC_VIOLATION: {
        level: 2, description: 'Unsafe spec violation but no known exploits' },
    UNSAFE_SPEC_VIOLATION: { level: 3, description: 'Unsafe spec violation' },
    NOT_OCAP_SAFE:         { level: 4, description: 'Not ocap safe' },
    NOT_ISOLATED:          { level: 5, description: 'Not isolated' },
    NEW_SYMPTOM:           { level: 6, description: 'New symptom' },
    NOT_SUPPORTED:         { level: 7, description: 'Not supported' }
  };

  /**
   * Statuses.
   *
   * <dl>
   *   <dt>ALL_FINE</dt>
   *     <dd>test passed before and after.</dd>
   *   <dt>REPAIR_FAILED</dt>
   *     <dd>test failed before and after repair attempt.</dd>
   *   <dt>NOT_REPAIRED</dt>
   *     <dd>test failed before and after, with no repair to attempt.</dd>
   *   <dt>REPAIRED_UNSAFELY</dt>
   *     <dd>test failed before and passed after repair attempt, but
   *         the repair is known to be inadequate for security, so the
   *         real problem remains.</dd>
   *   <dt>REPAIRED</dt>
   *     <dd>test failed before and passed after repair attempt,
   *         repairing the problem (canRepair was true).</dd>
   *   <dt>ACCIDENTALLY_REPAIRED</dt>
   *      <dd>test failed before and passed after, despite no repair
   *          to attempt. (Must have been fixed by some other
   *          attempted repair.)</dd>
   *   <dt>BROKEN_BY_OTHER_ATTEMPTED_REPAIRS</dt>
   *      <dd>test passed before and failed after, indicating that
   *          some other attempted repair created the problem.</dd>
   * </dl>
   */
  ses.statuses = {
    ALL_FINE:                          'All fine',
    REPAIR_FAILED:                     'Repair failed',
    NOT_REPAIRED:                      'Not repaired',
    REPAIRED_UNSAFELY:                 'Repaired unsafely',
    REPAIRED:                          'Repaired',
    ACCIDENTALLY_REPAIRED:             'Accidentally repaired',
    BROKEN_BY_OTHER_ATTEMPTED_REPAIRS: 'Broken by other attempted repairs'
  };


  var logger = ses.logger;

  /**
   * As we start to repair, this will track the worst post-repair
   * severity seen so far.
   */
  ses.maxSeverity = ses.severities.SAFE;

  /**
   * {@code ses.maxAcceptableSeverity} is the max post-repair severity
   * that is considered acceptable for proceeding with the SES
   * verification-only strategy.
   *
   * <p>Although <code>repairES5.js</code> can be used standalone for
   * partial ES5 repairs, its primary purpose is to repair as a first
   * stage of <code>initSES.js</code> for purposes of supporting SES
   * security. In support of that purpose, we initialize
   * {@code ses.maxAcceptableSeverity} to the post-repair severity
   * level at which we should report that we are unable to adequately
   * support SES security. By default, this is set to
   * {@code ses.severities.SAFE_SPEC_VIOLATION}, which is the maximum
   * severity that we believe results in no loss of SES security.
   *
   * <p>If {@code ses.maxAcceptableSeverityName} is already set (to a
   * severity property name of a severity below {@code
   * ses.NOT_SUPPORTED}), then we use that setting to initialize
   * {@code ses.maxAcceptableSeverity} instead. For example, if we are
   * using SES only for isolation, then we could set it to
   * 'NOT_OCAP_SAFE', in which case repairs that are inadequate for
   * object-capability (ocap) safety would still be judged safe for
   * our purposes.
   *
   * <p>As repairs proceed, they update {@code ses.maxSeverity} to
   * track the worst case post-repair severity seen so far. When
   * {@code ses.ok()} is called, it return whether {@code
   * ses.maxSeverity} is still less than or equal to
   * {@code ses.maxAcceptableSeverity}, indicating that this platform
   * still seems adequate for supporting SES. In the Caja context, we
   * have the choice of using SES on those platforms which we judge to
   * be adequately repairable, or otherwise falling back to Caja's
   * ES5/3 translator.
   */
  ses.maxAcceptableSeverityName = 
    validateSeverityName(ses.maxAcceptableSeverityName);
  ses.maxAcceptableSeverity = ses.severities[ses.maxAcceptableSeverityName];

  function validateSeverityName(severityName) {
    if (severityName) {
      var sev = ses.severities[severityName];
      if (sev && typeof sev.level === 'number' &&
        sev.level >= ses.severities.SAFE.level &&
        sev.level < ses.severities.NOT_SUPPORTED.level) {
        // do nothing
      } else {
        logger.error('Ignoring bad severityName: ' +
                   severityName + '.');
        severityName = 'SAFE_SPEC_VIOLATION';
      }
    } else {
      severityName = 'SAFE_SPEC_VIOLATION';
    }
    return severityName;
  }
  function severityNameToLevel(severityName) {
    return ses.severities[validateSeverityName(severityName)];
  }

  /**
   * Once this returns false, we can give up on the SES
   * verification-only strategy and fall back to ES5/3 translation.
   */
  ses.ok = function ok(maxSeverity) {
    if ("string" === typeof maxSeverity) {
      maxSeverity = ses.severities[maxSeverity];
    }
    if (!maxSeverity) {
      maxSeverity = ses.maxAcceptableSeverity;
    }
    return ses.maxSeverity.level <= maxSeverity.level;
  };

  /**
   * Update the max based on the provided severity.
   *
   * <p>If the provided severity exceeds the max so far, update the
   * max to match.
   */
  ses.updateMaxSeverity = function updateMaxSeverity(severity) {
    if (severity.level > ses.maxSeverity.level) {
      ses.maxSeverity = severity;
    }
  };

  //////// Prepare for "caller" and "argument" testing and repair /////////

  /**
   * Needs to work on ES3, since repairES5.js may be run on an ES3
   * platform.
   */
  function strictForEachFn(list, callback) {
    for (var i = 0, len = list.length; i < len; i++) {
      callback(list[i], i);
    }
  }

  /**
   * Needs to work on ES3, since repairES5.js may be run on an ES3
   * platform.
   *
   * <p>Also serves as our representative strict function, by contrast
   * to builtInMapMethod below, for testing what the "caller" and
   * "arguments" properties of a strict function reveals.
   */
  function strictMapFn(list, callback) {
    var result = [];
    for (var i = 0, len = list.length; i < len; i++) {
      result.push(callback(list[i], i));
    }
    return result;
  }

  var objToString = Object.prototype.toString;

  /**
   * Sample map early, to obtain a representative built-in for testing.
   *
   * <p>There is no reliable test for whether a function is a
   * built-in, and it is possible some of the tests below might
   * replace the built-in Array.prototype.map, though currently none
   * do. Since we <i>assume</i> (but with no reliable way to check)
   * that repairES5.js runs in its JavaScript context before anything
   * which might have replaced map, we sample it now. The map method
   * is a particularly nice one to sample, since it can easily be used
   * to test what the "caller" and "arguments" properties on a
   * in-progress built-in method reveals.
   */
  var builtInMapMethod = Array.prototype.map;

  var builtInForEach = Array.prototype.forEach;

  /**
   * http://wiki.ecmascript.org/doku.php?id=harmony:egal
   */
  var is = ses.is = Object.is || function(x, y) {
    if (x === y) {
      // 0 === -0, but they are not identical
      return x !== 0 || 1 / x === 1 / y;
    }

    // NaN !== NaN, but they are identical.
    // NaNs are the only non-reflexive value, i.e., if x !== x,
    // then x is a NaN.
    // isNaN is broken: it converts its argument to number, so
    // isNaN("foo") => true
    return x !== x && y !== y;
  };


  /**
   * By the time this module exits, either this is repaired to be a
   * function that is adequate to make the "caller" property of a
   * strict or built-in function harmess, or this module has reported
   * a failure to repair.
   *
   * <p>Start off with the optimistic assumption that nothing is
   * needed to make the "caller" property of a strict or built-in
   * function harmless. We are not concerned with the "caller"
   * property of non-strict functions. It is not the responsibility of
   * this module to actually make these "caller" properties
   * harmless. Rather, this module only provides this function so
   * clients such as startSES.js can use it to do so on the functions
   * they whitelist.
   *
   * <p>If the "caller" property of strict functions are not already
   * harmless, then this platform cannot be repaired to be
   * SES-safe. The only reason why {@code makeCallerHarmless} must
   * work on strict functions in addition to built-in is that some of
   * the other repairs below will replace some of the built-ins with
   * strict functions, so startSES.js will apply {@code
   * makeCallerHarmless} blindly to both strict and built-in
   * functions. {@code makeCallerHarmless} simply need not to complete
   * without breaking anything when given a strict function argument.
   */
  ses.makeCallerHarmless = function assumeCallerHarmless(func, path) {
    return 'Apparently fine';
  };

  /**
   * By the time this module exits, either this is repaired to be a
   * function that is adequate to make the "arguments" property of a
   * strict or built-in function harmess, or this module has reported
   * a failure to repair.
   *
   * Exactly analogous to {@code makeCallerHarmless}, but for
   * "arguments" rather than "caller".
   */
  ses.makeArgumentsHarmless = function assumeArgumentsHarmless(func, path) {
    return 'Apparently fine';
  };

  /**
   * "makeTamperProof()" returns a "tamperProof(obj)" function that
   * acts like "Object.freeze(obj)", except that, if obj is a
   * <i>prototypical</i> object (defined below), it ensures that the
   * effect of freezing properties of obj does not suppress the
   * ability to override these properties on derived objects by simple
   * assignment.
   *
   * <p>Because of lack of sufficient foresight at the time, ES5
   * unfortunately specified that a simple assignment to a
   * non-existent property must fail if it would override a
   * non-writable data property of the same name. (In retrospect, this
   * was a mistake, but it is now too late and we must live with the
   * consequences.) As a result, simply freezing an object to make it
   * tamper proof has the unfortunate side effect of breaking
   * previously correct code that is considered to have followed JS
   * best practices, if this previous code used assignment to
   * override.
   *
   * <p>To work around this mistake, tamperProof(obj) detects if obj
   * is <i>prototypical</i>, i.e., is an object whose own
   * "constructor" is a function whose "prototype" is this obj. For example,
   * Object.prototype and Function.prototype are prototypical.  If so,
   * then when tamper proofing it, prior to freezing, replace all its
   * configurable own data properties with accessor properties which
   * simulate what we should have specified -- that assignments to
   * derived objects succeed if otherwise possible.
   *
   * <p>Some platforms (Chrome and Safari as of this writing)
   * implement the assignment semantics ES5 should have specified
   * rather than what it did specify.
   * "test_ASSIGN_CAN_OVERRIDE_FROZEN()" below tests whether we are on
   * such a platform. If so, "repair_ASSIGN_CAN_OVERRIDE_FROZEN()"
   * replaces "makeTamperProof" with a function that simply returns
   * "Object.freeze", since the complex workaround here is not needed
   * on those platforms.
   *
   * <p>"makeTamperProof" should only be called after the trusted
   * initialization has done all the monkey patching that it is going
   * to do on the Object.* methods, but before any untrusted code runs
   * in this context.
   */
  var makeTamperProof = function defaultMakeTamperProof() {

    // Sample these after all trusted monkey patching initialization
    // but before any untrusted code runs in this frame.
    var gopd = Object.getOwnPropertyDescriptor;
    var gopn = Object.getOwnPropertyNames;
    var getProtoOf = Object.getPrototypeOf;
    var freeze = Object.freeze;
    var isFrozen = Object.isFrozen;
    var defProp = Object.defineProperty;
    var call = Function.prototype.call;

    function tamperProof(obj) {
      if (obj !== Object(obj)) { return obj; }
      var func;
      if ((typeof obj === 'object' || obj === Function.prototype) &&
          !!gopd(obj, 'constructor') &&
          typeof (func = obj.constructor) === 'function' &&
          func.prototype === obj &&
          !isFrozen(obj)) {
        strictForEachFn(gopn(obj), function(name) {
          var value;
          var getterCall;
          function getter() {
            if (obj === this) { return value; }
            if (this === void 0 || this === null) { return void 0; }
            var thisObj = Object(this);
            if (!!gopd(thisObj, name)) { return this[name]; }
            // TODO(erights): If we can reliably uncurryThis() in
            // repairES5.js, the next line should be:
            //   return callFn(getter, getProtoOf(thisObj));
            return getterCall(getProtoOf(thisObj));
          }
          getterCall = call.bind(getter);
          
          function setter(newValue) {
            if (obj === this) {
              throw new TypeError('Cannot set virtually frozen property: ' +
                                  name);
            }
            if (!!gopd(this, name)) {
              this[name] = newValue;
            }
            // TODO(erights): Do all the inherited property checks
            defProp(this, name, {
              value: newValue,
              writable: true,
              enumerable: true,
              configurable: true
            });
          }
          var desc = gopd(obj, name);
          if (desc.configurable && 'value' in desc) {
            value = desc.value;
            getter.prototype = null;
            setter.prototype = null;
            defProp(obj, name, {
              get: getter,
              set: setter,
              // We should be able to omit the enumerable line, since it
              // should default to its existing setting.
              enumerable: desc.enumerable,
              configurable: false
            });
          }
        });
      }
      return freeze(obj);
    }
    return tamperProof;
  };


  var needToTamperProof = [];
  /**
   * Various repairs may expose non-standard objects that are not
   * reachable from startSES's root, and therefore not freezable by
   * startSES's normal whitelist traversal. However, freezing these
   * during repairES5.js may be too early, as it is before WeakMap.js
   * has had a chance to monkey patch Object.freeze if necessary, in
   * order to install hidden properties for its own use before the
   * object becomes non-extensible.
   */
  function rememberToTamperProof(obj) {
    needToTamperProof.push(obj);
  }

  /**
   * Makes and returns a tamperProof(obj) function, and uses it to
   * tamper proof all objects whose tamper proofing had been delayed.
   *
   * <p>"makeDelayedTamperProof()" must only be called once.
   */
  var makeDelayedTamperProofCalled = false;
  ses.makeDelayedTamperProof = function makeDelayedTamperProof() {
    if (makeDelayedTamperProofCalled) {
      throw "makeDelayedTamperProof() must only be called once.";
    }
    var tamperProof = makeTamperProof();
    strictForEachFn(needToTamperProof, tamperProof);
    needToTamperProof = void 0;
    makeDelayedTamperProofCalled = true;
    return tamperProof;
  };

  /**
   * Where the "that" parameter represents a "this" that should have
   * been bound to "undefined" but may be bound to a global or
   * globaloid object.
   *
   * <p>The "desc" parameter is a string to describe the "that" if it
   * is something unexpected.
   */
  function testGlobalLeak(desc, that) {
    if (that === void 0) { return false; }
    if (that === global) { return true; }
    if (objToString.call(that) === '[object Window]') { return true; }
    return desc + ' leaked as: ' + that;
  }

  ////////////////////// Tests /////////////////////
  //
  // Each test is a function of no arguments that should not leave any
  // significant side effects, which tests for the presence of a
  // problem. It returns either
  // <ul>
  // <li>false, meaning that the problem does not seem to be present.
  // <li>true, meaning that the problem is present in a form that we expect.
  // <li>a non-empty string, meaning that there seems to be a related
  //     problem, but we're seeing a symptom different than what we
  //     expect. The string should describe the new symptom. It must
  //     be non-empty so that it is truthy.
  // </ul>
  // All the tests are run first to determine which corresponding
  // repairs to attempt. Then these repairs are run. Then all the
  // tests are rerun to see how they were effected by these repair
  // attempts. Finally, we report what happened.

  /**
   * If {@code Object.getOwnPropertyNames} is missing, we consider
   * this to be an ES3 browser which is unsuitable for attempting to
   * run SES.
   *
   * <p>If {@code Object.getOwnPropertyNames} is missing, there is no
   * way to emulate it.
   */
  function test_MISSING_GETOWNPROPNAMES() {
    return !('getOwnPropertyNames' in Object);
  }

  /**
   * Detects https://bugs.webkit.org/show_bug.cgi?id=64250
   *
   * <p>No workaround attempted. Just reporting that this platform is
   * not SES-safe.
   */
  function test_GLOBAL_LEAKS_FROM_GLOBAL_FUNCTION_CALLS() {
    global.___global_test_function___ = function() { return this; };
    var that = ___global_test_function___();
    delete global.___global_test_function___;
    return testGlobalLeak('Global func "this"', that);
  }

  /**
   * Detects whether the most painful ES3 leak is still with us.
   */
  function test_GLOBAL_LEAKS_FROM_ANON_FUNCTION_CALLS() {
    var that = (function(){ return this; })();
    return testGlobalLeak('Anon func "this"', that);
  }

  var strictThis = this;

  /**
   *
   */
  function test_GLOBAL_LEAKS_FROM_STRICT_THIS() {
    return testGlobalLeak('Strict "this"', strictThis);
  }

  /**
   * Detects
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
      return 'valueOf() threw: ' + err;
    }
    if (that === void 0) {
      // Should report as a safe spec violation
      return false;
    }
    return testGlobalLeak('valueOf()', that);
  }

  /**
   *
   */
  function test_GLOBAL_LEAKS_FROM_GLOBALLY_CALLED_BUILTINS() {
    global.___global_valueOf_function___ = {}.valueOf;
    var that = 'dummy';
    try {
      that = ___global_valueOf_function___();
    } catch (err) {
      if (err instanceof TypeError) { return false; }
      return 'valueOf() threw: ' + err;
    } finally {
      delete global.___global_valueOf_function___;
    }
    if (that === void 0) {
      // Should report as a safe spec violation
      return false;
    }
    return testGlobalLeak('Global valueOf()', that);
  }

  /**
   * Detects https://bugs.webkit.org/show_bug.cgi?id=55736
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
   * Detects http://code.google.com/p/v8/issues/detail?id=1530
   *
   * <p>Detects whether the value of a function's "prototype" property
   * as seen by normal object operations might deviate from the value
   * as seem by the reflective {@code Object.getOwnPropertyDescriptor}
   */
  function test_FUNCTION_PROTOTYPE_DESCRIPTOR_LIES() {
    function foo() {}
    Object.defineProperty(foo, 'prototype', { value: {} });
    return foo.prototype !==
      Object.getOwnPropertyDescriptor(foo, 'prototype').value;
  }

  /**
   * Detects https://bugs.webkit.org/show_bug.cgi?id=55537
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
      return 'Empty strict function has own callee';
    }
    return true;
  }

  /**
   * A strict delete should either succeed, returning true, or it
   * should fail by throwing a TypeError. Under no circumstances
   * should a strict delete return false.
   *
   * <p>This case occurs on IE10preview2.
   */
  function test_STRICT_DELETE_RETURNS_FALSE() {
    if (!RegExp.hasOwnProperty('rightContext')) { return false; }
    var deleted;
    try {
      deleted = delete RegExp.rightContext;
    } catch (err) {
      if (err instanceof TypeError) { return false; }
      return 'Deletion failed with: ' + err;
    }
    if (deleted) { return false; }
    return true;
  }

  /**
   * Detects https://bugzilla.mozilla.org/show_bug.cgi?id=591846
   * as applied to the RegExp constructor.
   *
   * <p>Note that Mozilla lists this bug as closed. But reading that
   * bug thread clarifies that is partially because the code in {@code
   * repair_REGEXP_CANT_BE_NEUTERED} enables us to work around the
   * non-configurability of the RegExp statics.
   */
  function test_REGEXP_CANT_BE_NEUTERED() {
    if (!RegExp.hasOwnProperty('leftContext')) { return false; }
    var deleted;
    try {
      deleted = delete RegExp.leftContext;
    } catch (err) {
      if (err instanceof TypeError) { return true; }
      return 'Deletion failed with: ' + err;
    }
    if (!RegExp.hasOwnProperty('leftContext')) { return false; }
    if (deleted) {
      return 'Deletion of RegExp.leftContext did not succeed.';
    } else {
      // This case happens on IE10preview2, as demonstrated by
      // test_STRICT_DELETE_RETURNS_FALSE.
      return true;
    }
  }

  /**
   * Detects http://code.google.com/p/v8/issues/detail?id=1393
   *
   * <p>This kludge is safety preserving.
   */
  function test_REGEXP_TEST_EXEC_UNSAFE() {
    (/foo/).test('xfoox');
    var match = new RegExp('(.|\r|\n)*','').exec()[0];
    if (match === 'undefined') { return false; }
    if (match === 'xfoox') { return true; }
    return 'regExp.exec() does not match against "undefined".';
  }

  /**
   * Detects https://bugs.webkit.org/show_bug.cgi?id=26382
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
   * Detects http://code.google.com/p/v8/issues/detail?id=892
   *
   * <p>This tests whether the built-in bind method violates the spec
   * by calling the original using its current .apply method rather
   * than the internal [[Call]] method. The workaround is the same as
   * for test_MISSING_BIND -- to replace the built-in bind with one
   * written in JavaScript. This introduces a different bug though: As
   * https://bugs.webkit.org/show_bug.cgi?id=26382#c29 explains, a
   * bind written in JavaScript cannot emulate the specified currying
   * over the construct behavior, and so fails to enable a var-args
   * {@code new} operation.
   */
  function test_BIND_CALLS_APPLY() {
    if (!('bind' in Function.prototype)) { return false; }
    var applyCalled = false;
    function foo() { return [].slice.call(arguments,0).join(','); }
    foo.apply = function fakeApply(self, args) {
      applyCalled = true;
      return Function.prototype.apply.call(this, self, args);
    };
    var b = foo.bind(33,44);
    var answer = b(55,66);
    if (applyCalled) { return true; }
    if (answer === '44,55,66') { return false; }
    return 'Bind test returned "' + answer + '" instead of "44,55,66".';
  }

  /**
   * Demonstrates the point made by comment 29
   * https://bugs.webkit.org/show_bug.cgi?id=26382#c29
   *
   * <p>Tests whether Function.prototype.bind curries over
   * construction ({@code new}) behavior. A built-in bind should. A
   * bind emulation written in ES5 can't.
   */
  function test_BIND_CANT_CURRY_NEW() {
    function construct(f, args) {
      var bound = Function.prototype.bind.apply(f, [null].concat(args));
      return new bound();
    }
    var d;
    try {
      d = construct(Date, [1957, 4, 27]);
    } catch (err) {
      if (err instanceof TypeError) { return true; }
      return 'Curries construction failed with: ' + err;
    }
    if (typeof d === 'string') { return true; } // Opera
    var str = objToString.call(d);
    if (str === '[object Date]') { return false; }
    return 'Unexpected ' + str + ': ' + d;
  }

  /**
   * Detects http://code.google.com/p/google-caja/issues/detail?id=1362
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
      return 'Mutating Date.prototype failed with: ' + err;
    }
    var v = Date.prototype.getFullYear();
    Date.prototype.setFullYear(NaN); // hopefully undoes the damage
    if (v !== v && typeof v === 'number') {
      // NaN indicates we're probably ok.
      // TODO(erights) Should we report this as a symptom anyway, so
      // that we get the repair which gives us a reliable TypeError?
      return false;
    }
    if (v === 1957) { return true; }
    return 'Mutating Date.prototype did not throw';
  }

  /**
   * Detects https://bugzilla.mozilla.org/show_bug.cgi?id=656828
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
      return 'Mutating WeakMap.prototype failed with: ' + err;
    }
    var v = WeakMap.prototype.get(x);
    // Since x cannot escape, there's no observable damage to undo.
    if (v === 86) { return true; }
    return 'Mutating WeakMap.prototype did not throw';
  }

  /**
   * Detects http://code.google.com/p/v8/issues/detail?id=1447
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
   * <p>This kludge is safety preserving but non-transparent, in that
   * the real forEach is frozen even in the success case, since we
   * have to freeze it in order to test for this failure. We could
   * repair this non-transparency by replacing it with a transparent
   * wrapper (as http://codereview.appspot.com/5278046/ does), but
   * since the SES use of this will freeze it anyway and the
   * indirection is costly, we choose not to for now.
   */
  function test_NEED_TO_WRAP_FOREACH() {
    if (!('freeze' in Object)) {
      // Object.freeze is still absent on released Android and would
      // cause a bogus bug detection in the following try/catch code.
      return false;
    }
    if (Array.prototype.forEach !== builtInForEach) {
      // If it is already wrapped, we are confident the problem does
      // not occur, and we need to skip the test to avoid freezing the
      // wrapper.
      return false;
    }
    try {
      ['z'].forEach(function(){ Object.freeze(Array.prototype.forEach); });
      return false;
    } catch (err) {
      if (err instanceof TypeError) { return true; }
      return 'freezing forEach failed with ' + err;
    }
  }

  /**
   * Detects http://code.google.com/p/v8/issues/detail?id=2273
   *
   * A strict mode function should receive a non-coerced 'this'
   * value. That is, in strict mode, if 'this' is a primitive, it
   * should not be boxed
   */
  function test_FOREACH_COERCES_THISOBJ() {
    "use strict";
    var needsWrapping = true;
    [1].forEach(function(){ needsWrapping = ("string" != typeof this); }, "f");
    return needsWrapping;
  }

  /**
   * <p>Sometimes, when trying to freeze an object containing an
   * accessor property with a getter but no setter, Chrome <= 17 fails
   * with <blockquote>Uncaught TypeError: Cannot set property ident___
   * of #<Object> which has only a getter</blockquote>. So if
   * necessary, this kludge overrides {@code Object.defineProperty} to
   * always install a dummy setter in lieu of the absent one.
   *
   * <p>Since this problem seems to have gone away as of Chrome 18, it
   * is no longer as important to isolate and report it.
   *
   * <p>TODO(erights): We should also override {@code
   * Object.getOwnPropertyDescriptor} to hide the presence of the
   * dummy setter, and instead report an absent setter.
   */
  function test_NEEDS_DUMMY_SETTER() {
    if (NEEDS_DUMMY_SETTER_repaired) { return false; }
    if (typeof navigator === 'undefined') { return false; }
    var ChromeMajorVersionPattern = (/Chrome\/(\d*)\./);
    var match = ChromeMajorVersionPattern.exec(navigator.userAgent);
    if (!match) { return false; }
    var ver = +match[1];
    return ver <= 17;
  }
  /** we use this variable only because we haven't yet isolated a test
   * for the problem. */
  var NEEDS_DUMMY_SETTER_repaired = false;

  /**
   * Detects http://code.google.com/p/chromium/issues/detail?id=94666
   */
  function test_FORM_GETTERS_DISAPPEAR() {
    function getter() { return 'gotten'; }

    if (typeof document === 'undefined' ||
       typeof document.createElement !== 'function') {
      // likely not a browser environment
      return false;
    }
    var f = document.createElement("form");
    try {
      Object.defineProperty(f, 'foo', {
        get: getter,
        set: void 0
      });
    } catch (err) {
      // Happens on Safari 5.0.2 on IPad2.
      return 'defining accessor on form failed with: ' + err;
    }
    var desc = Object.getOwnPropertyDescriptor(f, 'foo');
    if (desc.get === getter) { return false; }
    if (desc.get === void 0) { return true; }
    return 'Getter became ' + desc.get;
  }

  /**
   * Detects https://bugzilla.mozilla.org/show_bug.cgi?id=637994
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
   */
  function test_ACCESSORS_INHERIT_AS_OWN() {
    var base = {};
    var derived = Object.create(base);
    function getter() { return 'gotten'; }
    Object.defineProperty(base, 'foo', { get: getter });
    if (!derived.hasOwnProperty('foo') &&
        Object.getOwnPropertyDescriptor(derived, 'foo') === void 0 &&
        Object.getOwnPropertyNames(derived).indexOf('foo') < 0) {
      return false;
    }
    if (!derived.hasOwnProperty('foo') ||
        Object.getOwnPropertyDescriptor(derived, 'foo').get !== getter ||
        Object.getOwnPropertyNames(derived).indexOf('foo') < 0) {
      return 'Accessor properties partially inherit as own properties.';
    }
    Object.defineProperty(base, 'bar', { get: getter, configurable: true });
    if (!derived.hasOwnProperty('bar') &&
        Object.getOwnPropertyDescriptor(derived, 'bar') === void 0 &&
        Object.getOwnPropertyNames(derived).indexOf('bar') < 0) {
      return true;
    }
    return 'Accessor properties inherit as own even if configurable.';
  }

  /**
   * Detects http://code.google.com/p/v8/issues/detail?id=1360
   *
   * Our workaround wraps {@code sort} to wrap the comparefn.
   */
  function test_SORT_LEAKS_GLOBAL() {
    var that = 'dummy';
    [2,3].sort(function(x,y) { that = this; return x - y; });
    if (that === void 0) { return false; }
    if (that === global) { return true; }
    return 'sort called comparefn with "this" === ' + that;
  }

  /**
   * Detects http://code.google.com/p/v8/issues/detail?id=1360
   *
   * <p>Our workaround wraps {@code replace} to wrap the replaceValue
   * if it's a function.
   */
  function test_REPLACE_LEAKS_GLOBAL() {
    var that = 'dummy';
    function capture() { that = this; return 'y';}
    'x'.replace(/x/, capture);
    if (that === void 0) { return false; }
    if (that === capture) {
      // This case happens on IE10preview2. See
      // https://connect.microsoft.com/IE/feedback/details/685928/
      //   bad-this-binding-for-callback-in-string-prototype-replace
      // TODO(erights): When this happens, the kludge.description is
      // wrong.
      return true;
    }
    if (that === global) { return true; }
    return 'Replace called replaceValue function with "this" === ' + that;
  }

  /**
   * Detects
   * https://connect.microsoft.com/IE/feedback/details/
   *   685436/getownpropertydescriptor-on-strict-caller-throws
   *
   * <p>Object.getOwnPropertyDescriptor must work even on poisoned
   * "caller" properties.
   */
  function test_CANT_GOPD_CALLER() {
    var desc = null;
    try {
      desc = Object.getOwnPropertyDescriptor(function(){}, 'caller');
    } catch (err) {
      if (err instanceof TypeError) { return true; }
      return 'getOwnPropertyDescriptor failed with: ' + err;
    }
    if (desc &&
        typeof desc.get === 'function' &&
        typeof desc.set === 'function' &&
        !desc.configurable) {
      return false;
    }
    if (desc &&
        desc.value === null &&
        !desc.writable &&
        !desc.configurable) {
      // Seen in IE9. Harmless by itself
      return false;
    }
    return 'getOwnPropertyDesciptor returned unexpected caller descriptor';
  }

  /**
   * Detects https://bugs.webkit.org/show_bug.cgi?id=63398
   *
   * <p>A strict function's caller should be poisoned only in a way
   * equivalent to an accessor property with a throwing getter and
   * setter.
   *
   * <p>Seen on Safari 5.0.6 through WebKit Nightly r93670
   */
  function test_CANT_HASOWNPROPERTY_CALLER() {
    var answer = void 0;
    try {
      answer = function(){}.hasOwnProperty('caller');
    } catch (err) {
      if (err instanceof TypeError) { return true; }
      return 'hasOwnProperty failed with: ' + err;
    }
    if (answer) { return false; }
    return 'strict_function.hasOwnProperty("caller") was false';
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
    var finallySkipped = true;
    try {
      result = name in base;
    } catch (err) {
      logger.error('New symptom (a): (\'' +
                   name + '\' in <' + baseDesc + '>) threw: ', err);
      // treat this as a safe absence
      result = false;
      return false;
    } finally {
      finallySkipped = false;
      if (result === void 0) {
        logger.error('New symptom (b): (\'' +
                     name + '\' in <' + baseDesc + '>) failed');
      }
    }
    if (finallySkipped) {
      logger.error('New symptom (e): (\'' +
                   name + '\' in <' + baseDesc +
                   '>) inner finally skipped');
    }
    return !!result;
  }

  function has2(base, name, baseDesc) {
    var result = void 0;
    var finallySkipped = true;
    try {
      result = has(base, name, baseDesc);
    } catch (err) {
      logger.error('New symptom (c): (\'' +
                   name + '\' in <' + baseDesc + '>) threw: ', err);
      // treat this as a safe absence
      result = false;
      return false;
    } finally {
      finallySkipped = false;
      if (result === void 0) {
        logger.error('New symptom (d): (\'' +
                     name + '\' in <' + baseDesc + '>) failed');
      }
    }
    if (finallySkipped) {
      logger.error('New symptom (f): (\'' +
                   name + '\' in <' + baseDesc +
                   '>) outer finally skipped');
    }
    return !!result;
  }

  /**
   * Detects https://bugs.webkit.org/show_bug.cgi?id=63398
   *
   * <p>If this reports a problem in the absence of "New symptom (a)",
   * it means the error thrown by the "in" in {@code has} is skipping
   * past the first layer of "catch" surrounding that "in". This is in
   * fact what we're currently seeing on Safari WebKit Nightly Version
   * 5.0.5 (5533.21.1, r91108).
   */
  function test_CANT_IN_CALLER() {
    var answer = void 0;
    try {
      answer = has2(function(){}, 'caller', 'strict_function');
    } catch (err) {
      if (err instanceof TypeError) { return true; }
      return '("caller" in strict_func) failed with: ' + err;
    } finally {}
    if (answer) { return false; }
    return '("caller" in strict_func) was false.';
  }

  /**
   * Detects https://bugs.webkit.org/show_bug.cgi?id=63398
   *
   * <p>If this reports a problem in the absence of "New symptom (a)",
   * it means the error thrown by the "in" in {@code has} is skipping
   * past the first layer of "catch" surrounding that "in". This is in
   * fact what we're currently seeing on Safari WebKit Nightly Version
   * 5.0.5 (5533.21.1, r91108).
   */
  function test_CANT_IN_ARGUMENTS() {
    var answer = void 0;
    try {
      answer = has2(function(){}, 'arguments', 'strict_function');
    } catch (err) {
      if (err instanceof TypeError) { return true; }
      return '("arguments" in strict_func) failed with: ' + err;
    } finally {}
    if (answer) { return false; }
    return '("arguments" in strict_func) was false.';
  }

  /**
   * Detects whether strict function violate caller anonymity.
   */
  function test_STRICT_CALLER_NOT_POISONED() {
    if (!has2(strictMapFn, 'caller', 'a strict function')) { return false; }
    function foo(m) { return m.caller; }
    // using Function so it'll be non-strict
    var testfn = Function('m', 'f', 'return m([m], f)[0];');
    var caller;
    try {
      caller = testfn(strictMapFn, foo);
    } catch (err) {
      if (err instanceof TypeError) { return false; }
      return 'Strict "caller" failed with: ' + err;
    }
    if (testfn === caller) {
      // Seen on IE 9
      return true;
    }
    return 'Unexpected "caller": ' + caller;
  }

  /**
   * Detects whether strict functions are encapsulated.
   */
  function test_STRICT_ARGUMENTS_NOT_POISONED() {
    if (!has2(strictMapFn, 'arguments', 'a strict function')) { return false; }
    function foo(m) { return m.arguments; }
    // using Function so it'll be non-strict
    var testfn = Function('m', 'f', 'return m([m], f)[0];');
    var args;
    try {
      args = testfn(strictMapFn, foo);
    } catch (err) {
      if (err instanceof TypeError) { return false; }
      return 'Strict "arguments" failed with: ' + err;
    }
    if (args[1] === foo) {
      // Seen on IE 9
      return true;
    }
    return 'Unexpected arguments: ' + arguments;
  }

  /**
   * Detects https://bugzilla.mozilla.org/show_bug.cgi?id=591846 as
   * applied to "caller"
   */
  function test_BUILTIN_LEAKS_CALLER() {
    if (!has2(builtInMapMethod, 'caller', 'a builtin')) { return false; }
    function foo(m) { return m.caller; }
    // using Function so it'll be non-strict
    var testfn = Function('a', 'f', 'return a.map(f)[0];');
    var a = [builtInMapMethod];
    a.map = builtInMapMethod;
    var caller;
    try {
      caller = testfn(a, foo);
    } catch (err) {
      if (err instanceof TypeError) { return false; }
      return 'Built-in "caller" failed with: ' + err;
    }
    if (null === caller || void 0 === caller) { return false; }
    if (testfn === caller) { return true; }
    return 'Unexpected "caller": ' + caller;
  }

  /**
   * Detects https://bugzilla.mozilla.org/show_bug.cgi?id=591846 as
   * applied to "arguments"
   */
  function test_BUILTIN_LEAKS_ARGUMENTS() {
    if (!has2(builtInMapMethod, 'arguments', 'a builtin')) { return false; }
    function foo(m) { return m.arguments; }
    // using Function so it'll be non-strict
    var testfn = Function('a', 'f', 'return a.map(f)[0];');
    var a = [builtInMapMethod];
    a.map = builtInMapMethod;
    var args;
    try {
      args = testfn(a, foo);
    } catch (err) {
      if (err instanceof TypeError) { return false; }
      return 'Built-in "arguments" failed with: ' + err;
    }
    if (args === void 0 || args === null) { return false; }
    return true;
  }

  /**
   * Detects http://code.google.com/p/v8/issues/detail?id=893
   */
  function test_BOUND_FUNCTION_LEAKS_CALLER() {
    if (!('bind' in Function.prototype)) { return false; }
    function foo() { return bar.caller; }
    var bar = foo.bind({});
    if (!has2(bar, 'caller', 'a bound function')) { return false; }
    // using Function so it'll be non-strict
    var testfn = Function('b', 'return b();');
    var caller;
    try {
      caller = testfn(bar);
    } catch (err) {
      if (err instanceof TypeError) { return false; }
      return 'Bound function "caller" failed with: ' + err;
    }
    if (caller === void 0 || caller === null) { return false; }
    if (caller === testfn) { return true; }
    return 'Unexpected "caller": ' + caller;
  }

  /**
   * Detects http://code.google.com/p/v8/issues/detail?id=893
   */
  function test_BOUND_FUNCTION_LEAKS_ARGUMENTS() {
    if (!('bind' in Function.prototype)) { return false; }
    function foo() { return bar.arguments; }
    var bar = foo.bind({});
    if (!has2(bar, 'arguments', 'a bound function')) { return false; }
    // using Function so it'll be non-strict
    var testfn = Function('b', 'return b();');
    var args;
    try {
      args = testfn(bar);
    } catch (err) {
      if (err instanceof TypeError) { return false; }
      return 'Bound function "arguments" failed with: ' + err;
    }
    if (args === void 0 || args === null) { return false; }
    return true;
  }

  /**
   * Detects https://bugs.webkit.org/show_bug.cgi?id=70207
   *
   * <p>After deleting a built-in, the problem is that
   * getOwnPropertyNames still lists the name as present, but it seems
   * absent in all other ways.
   */
  function test_DELETED_BUILTINS_IN_OWN_NAMES() {
    if (!('__defineSetter__' in Object.prototype)) { return false; }
    var desc = Object.getOwnPropertyDescriptor(Object.prototype,
                                               '__defineSetter__');
    try {
      try {
        delete Object.prototype.__defineSetter__;
      } catch (err1) {
        return false;
      }
      var names = Object.getOwnPropertyNames(Object.prototype);
      if (names.indexOf('__defineSetter__') === -1) { return false; }
      if ('__defineSetter__' in Object.prototype) {
        // If it's still there, it bounced back. Which is still a
        // problem, but not the problem we're testing for here.
        return false;
      }
      return true;
    } finally {
      Object.defineProperty(Object.prototype, '__defineSetter__', desc);
    }
  }

  /**
   * Detects http://code.google.com/p/v8/issues/detail?id=1769
   */
  function test_GETOWNPROPDESC_OF_ITS_OWN_CALLER_FAILS() {
    try {
      Object.getOwnPropertyDescriptor(Object.getOwnPropertyDescriptor,
                                      'caller');
    } catch (err) {
      if (err instanceof TypeError) { return true; }
      return 'getOwnPropertyDescriptor threw: ' + err;
    }
    return false;
  }

  /**
   * Detects http://code.google.com/p/v8/issues/detail?id=621
   *
   */
  function test_JSON_PARSE_PROTO_CONFUSION() {
    var x;
    try {
      x = JSON.parse('{"__proto__":[]}');
    } catch (err) {
      if (err instanceof TypeError) {
        // We consider it acceptable to fail this case with a
        // TypeError, as our repair below will cause it to do.
        return false;
      }
      return 'JSON.parse failed with: ' + err;
    }
    if (Object.getPrototypeOf(x) !== Object.prototype) { return true; }
    if (Array.isArray(x.__proto__)) { return false; }
    return 'JSON.parse did not set "__proto__" as a regular property';
  }

  /**
   * Detects https://bugs.webkit.org/show_bug.cgi?id=65832
   *
   * <p>On a non-extensible object, it must not be possible to change
   * its internal [[Prototype]] property, i.e., which object it
   * inherits from.
   *
   * TODO(erights): investigate the following:
   * At http://goo.gl/ycCmo Mike Stay says
   * <blockquote>
   * Kevin notes in domado.js that on some versions of FF, event
   * objects switch prototypes when moving between frames. You should
   * probably check out their behavior around freezing and sealing.
   * </blockquote>
   * But I couldn't find it.
   */
  function test_PROTO_NOT_FROZEN() {
    if (!('freeze' in Object)) {
      // Object.freeze and its ilk (including preventExtensions) are
      // still absent on released Android and would
      // cause a bogus bug detection in the following try/catch code.
      return false;
    }
    var x = Object.preventExtensions({});
    if (x.__proto__ === void 0 && !('__proto__' in x)) { return false; }
    var y = {};
    try {
      x.__proto__ = y;
    } catch (err) {
      if (err instanceof TypeError) { return false; }
      return 'Mutating __proto__ failed with: ' + err;
    }
    if (y.isPrototypeOf(x)) { return true; }
    return 'Mutating __proto__ neither failed nor succeeded';
  }

  /**
   * Like test_PROTO_NOT_FROZEN but using defineProperty rather than
   * assignment.
   */
  function test_PROTO_REDEFINABLE() {
    if (!('freeze' in Object)) {
      // Object.freeze and its ilk (including preventExtensions) are
      // still absent on released Android and would
      // cause a bogus bug detection in the following try/catch code.
      return false;
    }
    var x = Object.preventExtensions({});
    if (x.__proto__ === void 0 && !('__proto__' in x)) { return false; }
    var y = {};
    try {
      Object.defineProperty(x, '__proto__', { value: y });
    } catch (err) {
      if (err instanceof TypeError) { return false; }
      return 'Defining __proto__ failed with: ' + err;
    }
    // If x.__proto__ has changed but is not equal to y,
    // we deal with that in the next test.
    return (x.__proto__ === y);
  }


  /**
   * Some versions of v8 fail silently when attempting to assign to __proto__
   */
  function test_DEFINING_READ_ONLY_PROTO_FAILS_SILENTLY() {
    if (!('freeze' in Object)) {
      // Object.freeze and its ilk (including preventExtensions) are
      // still absent on released Android and would
      // cause a bogus bug detection in the following try/catch code.
      return false;
    }
    var x = Object.preventExtensions({});
    if (x.__proto__ === void 0 && !('__proto__' in x)) { return false; }
    var y = {};
    try {
      Object.defineProperty(x, '__proto__', { value: y });
    } catch (err) {
      if (err instanceof TypeError) { return false; }
      return 'Defining __proto__ failed with: ' + err;
    }
    if (x.__proto__ === Object.prototype) {
      return true;
    }
    return "Read-only proto was changed in a strange way.";
  }

  /**
   * Detects http://code.google.com/p/v8/issues/detail?id=1624
   *
   * <p>Both a direct strict eval operator and an indirect strict eval
   * function must not leak top level declarations in the string being
   * evaluated into their containing context.
   */
  function test_STRICT_EVAL_LEAKS_GLOBALS() {
    (1,eval)('"use strict"; var ___global_test_variable___ = 88;');
    if ('___global_test_variable___' in global) {
      delete global.___global_test_variable___;
      return true;
    }
    return false;
  }
  /**
   * Detects http://code.google.com/p/v8/issues/detail?id=2396
   * 
   * <p>Commenting out the eval does the right thing.  Only fails in
   * non-strict mode.
   */
  function test_EVAL_BREAKS_MASKING() {
    var x;
    x = (function a() {
      function a() {}
      eval("");
      return a;
    });
    // x() should be the internal function a(), not itself
    return x() === x;
  }


  /**
   * Detects http://code.google.com/p/v8/issues/detail?id=1645
   */
  function test_PARSEINT_STILL_PARSING_OCTAL() {
    var n = parseInt('010');
    if (n === 10) { return false; }
    if (n === 8)  { return true; }
    return 'parseInt("010") returned ' + n;
  }

  /**
   * Detects https://bugzilla.mozilla.org/show_bug.cgi?id=695577
   *
   * <p>When E4X syntax is accepted in strict code, then without
   * parsing, we cannot prevent untrusted code from expressing E4X
   * literals and so obtaining access to shared E4X prototypes,
   * despite the absence of these prototypes from our whitelist. While
   * https://bugzilla.mozilla.org/show_bug.cgi?id=695579 is also
   * open, we cannot even repair the situation, leading to unpluggable
   * capability leaks. However, we do not test for this additional
   * problem, since E4X is such a can of worms that 695577 is adequate
   * by itself for us to judge this platform to be insecurable without
   * parsing.
   */
  function test_STRICT_E4X_LITERALS_ALLOWED() {
    var x;
    try {
      x = eval('"use strict";(<foo/>);');
    } catch (err) {
      if (err instanceof SyntaxError) { return false; }
      return 'E4X test failed with: ' + err;
    }
    if (x !== void 0) { return true; }
    return 'E4X literal expression had no value';
  }

  /**
   * Detects whether assignment can override an inherited
   * non-writable, non-configurable data property.
   *
   * <p>According to ES5.1, assignment should not be able to do so,
   * which is unfortunate for SES, as the tamperProof function must
   * kludge expensively to ensure that legacy assignments that don't
   * violate best practices continue to work. Ironically, on platforms
   * in which this bug is present, tamperProof can just be cheaply
   * equivalent to Object.freeze.
   */
  function test_ASSIGN_CAN_OVERRIDE_FROZEN() {
    var x = Object.freeze({foo: 88});
    var y = Object.create(x);
    try {
      y.foo = 99;
    } catch (err) {
      if (err instanceof TypeError) { return false; }
      return 'Override failed with: ' + err;
    }
    if (y.foo === 99) { return true; }
    if (y.foo === 88) { return 'Override failed silently'; }
    return 'Unexpected override outcome: ' + y.foo;
  }

  /**
   * Detects whether calling pop on a frozen array can modify the array.
   * See https://bugs.webkit.org/show_bug.cgi?id=75788
   */
  function test_POP_IGNORES_FROZEN() {
    var x = [1,2];
    Object.freeze(x);
    try {
      x.pop();
    } catch (e) {
      if (x.length !== 2) { return 'Unexpected modification of frozen array'; }
      if (x[0] === 1 && x[1] === 2) { return false; }
    }
    if (x.length !== 2) {
      return 'Unexpected silent modification of frozen array';
    }
    return (x[0] !== 1 || x[1] !== 2);
  }


  /**
   * Detects whether calling sort on a frozen array can modify the array.
   * See http://code.google.com/p/v8/issues/detail?id=2419
   */
  function test_SORT_IGNORES_FROZEN() {
    var x = [2,1];
    Object.freeze(x);
    try {
      x.sort();
    } catch (e) {
      if (x.length !== 2) { return 'Unexpected modification of frozen array'; }
      if (x[0] === 2 && x[1] === 1) { return false; }
    }
    if (x.length !== 2) {
      return 'Unexpected silent modification of frozen array';
    }
    return (x[0] !== 2 || x[1] !== 1);
  }

  /**
   * Detects whether calling push on a sealed array can modify the array.
   * See http://code.google.com/p/v8/issues/detail?id=2412
   */
  function test_PUSH_IGNORES_SEALED() {
    var x = [1,2];
    Object.seal(x);
    try {
      x.push(3);
    } catch (e) {
      if (x.length !== 2) { return 'Unexpected modification of frozen array'; }
      if (x[0] === 1 && x[1] === 2) { return false; }
    }
    return (x.length !== 2 || x[0] !== 1 || x[1] !== 2);
  }

  /**
   * In some browsers, assigning to array length can delete
   * non-configurable properties.
   * https://bugzilla.mozilla.org/show_bug.cgi?id=590690
   * TODO(felix8a): file bug for chrome
   */
  function test_ARRAYS_DELETE_NONCONFIGURABLE() {
    var x = [];
    Object.defineProperty(x, 0, { value: 3, configurable: false });
    try {
      x.length = 0;
    } catch (e) {}
    return x.length !== 1 || x[0] !== 3;
  }

  /**
   * In some versions of Chrome, extending an array can
   * modify a read-only length property.
   * http://code.google.com/p/v8/issues/detail?id=2379
   */
  function test_ARRAYS_MODIFY_READONLY() {
    var x = [];
    try {
      Object.defineProperty(x, 'length', {value: 0, writable: false});
      x[0] = 1;
    } catch(e) {}
    return x.length !== 0 || x[0] !== void 0;
  }

  /**
   *
   */
  function test_CANT_REDEFINE_NAN_TO_ITSELF() {
    var descNaN = Object.getOwnPropertyDescriptor(global, 'NaN');
    try {
      Object.defineProperty(global, 'NaN', descNaN);
    } catch (err) {
      if (err instanceof TypeError) { return true; }
      return 'defineProperty of NaN failed with: ' + err;
    }
    return false;
  }

  /**		
   * In Firefox 15+, Object.freeze and Object.isFrozen only work for		
   * descendents of that same Object.		
   */		
  function test_FIREFOX_15_FREEZE_PROBLEM() {		
    if (!document || !document.createElement) { return false; }		
    var iframe = document.createElement('iframe');		
    var where = document.getElementsByTagName('script')[0];		
    where.parentNode.insertBefore(iframe, where);		
    var otherObject = iframe.contentWindow.Object;		
    where.parentNode.removeChild(iframe);		
    var obj = {};		
    otherObject.freeze(obj);		
    return !Object.isFrozen(obj);		
  }

  /**
   * These are all the own properties that appear on Error instances
   * on various ES5 platforms as of this writing.
   *
   * <p>Due to browser bugs, some of these are absent from
   * getOwnPropertyNames (gopn). TODO(erights): File bugs with various
   * browser makers for any own properties that we know to be present
   * but not reported by gopn.
   *
   * <p>TODO(erights): do intelligence with the various browser
   * implementors to find out what other properties are provided by
   * their implementation but absent from gopn, whether on Errors or
   * anything else. Every one of these are potentially fatal to our
   * security until we can examine these.
   *
   * <p>The source form is a list rather than a map so that we can list a
   * name like "message" for each browser section we think it goes in.
   *
   * <p>We thank the following people, projects, and websites for
   * providing some useful intelligence of what property names we
   * should suspect:<ul>
   * <li><a href="http://stacktracejs.com">stacktracejs.com</a>
   * <li>TODO(erights): find message on es-discuss list re
   * "   stack". credit author.
   * </ul>
   */
  var errorInstanceWhitelist = [
    // at least Chrome 16
    'arguments',
    'message',
    'stack',
    'type',

    // at least FF 9
    'fileName',
    'lineNumber',
    'message',
    'stack',

    // at least Safari, WebKit 5.1
    'line',
    'message',
    'sourceId',
    'sourceURL',

    // at least IE 10 preview 2
    'description',
    'message',
    'number',

    // at least Opera 11.60
    'message',
    'stack',
    'stacktrace'
  ];

  var errorInstanceBlacklist = [
    // seen in a Firebug on FF
    'category',
    'context',
    'href',
    'lineNo',
    'msgId',
    'source',
    'trace',
    'correctSourcePoint',
    'correctWithStackTrace',
    'getSourceLine',
    'resetSource'
  ];

  /** Return a fresh one so client can mutate freely */
  function freshErrorInstanceWhiteMap() {
    var result = Object.create(null);
    strictForEachFn(errorInstanceWhitelist, function(name) {
      // We cannot yet use StringMap so do it manually
      // We do this naively here assuming we don't need to worry about
      // __proto__
      result[name] = true;
    });
    return result;
  }

  /**
   * Do Error instances on those platform carry own properties that we
   * haven't yet examined and determined to be SES-safe?
   *
   * <p>A new property should only be added to the
   * errorInstanceWhitelist after inspecting the consequences of that
   * property to determine that it does not compromise SES safety. If
   * some platform maker does add an Error own property that does
   * compromise SES safety, that might be a severe problem, if we
   * can't find a way to deny untrusted code access to that property.
   */
  function test_UNEXPECTED_ERROR_PROPERTIES() {
    var errs = [new Error('e1')];
    try { null.foo = 3; } catch (err) { errs.push(err); }
    var result = false;

    var approvedNames = freshErrorInstanceWhiteMap();

    strictForEachFn(errs, function(err) {
      strictForEachFn(Object.getOwnPropertyNames(err), function(name) {
         if (!(name in approvedNames)) {
           result = 'Unexpected error instance property: ' + name;
           // would be good to terminate early
         }
      });
    });
    return result;
  }

  /**
   * On Firefox 14+ (and probably earlier), error instances have magical
   * properties that do not appear in getOwnPropertyNames until you refer
   * to the property.  This makes test_UNEXPECTED_ERROR_PROPERTIES
   * unreliable, so we can't assume that passing that test is safe.
   */
  function test_ERRORS_HAVE_INVISIBLE_PROPERTIES() {
    var gopn = Object.getOwnPropertyNames;
    var gopd = Object.getOwnPropertyDescriptor;

    var errors = [new Error('e1')];
    try { null.foo = 3; } catch (err) { errors.push(err); }
    for (var i = 0; i < errors.length; i++) {
      var err = errors[i];
      var found = Object.create(null);
      strictForEachFn(gopn(err), function (prop) {
        found[prop] = true;
      });
      var j, prop;
      for (j = 0; j < errorInstanceWhitelist.length; j++) {
        prop = errorInstanceWhitelist[j];
        if (gopd(err, prop) && !found[prop]) {
          return true;
        }
      }
      for (j = 0; j < errorInstanceBlacklist.length; j++) {
        prop = errorInstanceBlacklist[j];
        if (gopd(err, prop) && !found[prop]) {
          return true;
        }
      }
    }
    return false;
  }

  ////////////////////// Repairs /////////////////////
  //
  // Each repair_NAME function exists primarily to repair the problem
  // indicated by the corresponding test_NAME function. But other test
  // failures can still trigger a given repair.


  var call = Function.prototype.call;
  var apply = Function.prototype.apply;

  var hop = Object.prototype.hasOwnProperty;
  var slice = Array.prototype.slice;
  var concat = Array.prototype.concat;
  var getPrototypeOf = Object.getPrototypeOf;
  var unsafeDefProp = Object.defineProperty;
  var isExtensible = Object.isExtensible;

  function patchMissingProp(base, name, missingFunc) {
    if (!(name in base)) {
      Object.defineProperty(base, name, {
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

  /*
   * Fixes both FUNCTION_PROTOTYPE_DESCRIPTOR_LIES and
   * DEFINING_READ_ONLY_PROTO_FAILS_SILENTLY.
   */
  function repair_DEFINE_PROPERTY() {
    function repairedDefineProperty(base, name, desc) {
      if (typeof base === 'function' &&
          name === 'prototype' &&
          'value' in desc) {
        try {
          base.prototype = desc.value;
        } catch (err) {
          logger.warn('prototype fixup failed', err);
        }
      }
      if (!isExtensible(base) && name === '__proto__') {
        throw TypeError('Cannot redefine __proto__ on a non-extensible object');
      }
      return unsafeDefProp(base, name, desc);
    }
    Object.defineProperty(Object, 'defineProperty', {
      value: repairedDefineProperty
    });
  }

  function repair_MISSING_CALLEE_DESCRIPTOR() {
    var realGOPN = Object.getOwnPropertyNames;
    Object.defineProperty(Object, 'getOwnPropertyNames', {
      value: function calleeFix(base) {
        var result = realGOPN(base);
        if (typeof base === 'function') {
          var i = result.indexOf('callee');
          if (i >= 0 && !hop.call(base, 'callee')) {
            result.splice(i, 1);
          }
        }
        return result;
      }
    });
  }

  function repair_REGEXP_CANT_BE_NEUTERED() {
    var UnsafeRegExp = RegExp;
    var FakeRegExp = function RegExpWrapper(pattern, flags) {
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
    Object.defineProperty(FakeRegExp, 'prototype', {
      value: UnsafeRegExp.prototype
    });
    Object.defineProperty(FakeRegExp.prototype, 'constructor', {
      value: FakeRegExp
    });
    RegExp = FakeRegExp;
  }

  function repair_REGEXP_TEST_EXEC_UNSAFE() {
    var unsafeRegExpExec = RegExp.prototype.exec;
    var unsafeRegExpTest = RegExp.prototype.test;

    Object.defineProperty(RegExp.prototype, 'exec', {
      value: function fakeExec(specimen) {
        return unsafeRegExpExec.call(this, String(specimen));
      }
    });
    Object.defineProperty(RegExp.prototype, 'test', {
      value: function fakeTest(specimen) {
        return unsafeRegExpTest.call(this, String(specimen));
      }
    });
  }

  function repair_MISSING_BIND() {

    /**
     * Actual bound functions are not supposed to have a prototype, and
     * are supposed to curry over both the [[Call]] and [[Construct]]
     * behavior of their original function. However, in ES5,
     * functions written in JavaScript cannot avoid having a 'prototype'
     * property, and cannot reliably distinguish between being called as
     * a function vs as a constructor, i.e., by {@code new}.
     *
     * <p>Since the repair_MISSING_BIND emulation produces a bound
     * function written in JavaScript, it cannot faithfully emulate
     * either the lack of a 'prototype' property nor the currying of the
     * [[Construct]] behavior. So instead, we use BOGUS_BOUND_PROTOTYPE
     * to reliably give an error for attempts to {@code new} a bound
     * function. Since we cannot avoid exposing BOGUS_BOUND_PROTOTYPE
     * itself, it is possible to pass in a this-binding which inherits
     * from it without using {@code new}, which will also trigger our
     * error case. Whether this latter error is appropriate or not, it
     * still fails safe.
     *
     * <p>By making the 'prototype' of the bound function be the same as
     * the current {@code thisFunc.prototype}, we could have emulated
     * the [[HasInstance]] property of bound functions. But even this
     * would have been inaccurate, since we would be unable to track
     * changes to the original {@code thisFunc.prototype}. (We cannot
     * make 'prototype' into an accessor to do this tracking, since
     * 'prototype' on a function written in JavaScript is
     * non-configurable.) And this one partially faithful emulation
     * would have come at the cost of no longer being able to reasonably
     * detect construction, in order to safely reject it.
     */
    var BOGUS_BOUND_PROTOTYPE = {
      toString: function BBPToString() { return 'bogus bound prototype'; }
    };
    rememberToTamperProof(BOGUS_BOUND_PROTOTYPE);
    BOGUS_BOUND_PROTOTYPE.toString.prototype = null;
    rememberToTamperProof(BOGUS_BOUND_PROTOTYPE.toString);

    var defProp = Object.defineProperty;
    defProp(Function.prototype, 'bind', {
      value: function fakeBind(self, var_args) {
        var thisFunc = this;
        var leftArgs = slice.call(arguments, 1);
        function funcBound(var_args) {
          if (this === Object(this) &&
              getPrototypeOf(this) === BOGUS_BOUND_PROTOTYPE) {
            throw new TypeError(
              'Cannot emulate "new" on pseudo-bound function.');
          }
          var args = concat.call(leftArgs, slice.call(arguments, 0));
          return apply.call(thisFunc, self, args);
        }
        defProp(funcBound, 'prototype', {
          value: BOGUS_BOUND_PROTOTYPE,
          writable: false,
          configurable: false
        });
        return funcBound;
      },
      writable: true,
      enumerable: false,
      configurable: true
    });
  }

  /**
   * Return a function suitable for using as a forEach argument on a
   * list of method names, where that function will monkey patch each
   * of these names methods on {@code constr.prototype} so that they
   * can't be called on a {@code constr.prototype} itself even across
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
    var grandProto = getPrototypeOf(proto);
    var grandBaseToString = objToString.call(grandProto);
    if (grandBaseToString === '[object ' + classString + ']') {
      throw new TypeError('malformed inheritance: ' + classString);
    }
    if (grandProto !== Object.prototype) {
      logger.log('unexpected inheritance: ' + classString);
    }
    function mutableProtoPatcher(name) {
      if (!hop.call(proto, name)) { return; }
      var originalMethod = proto[name];
      function replacement(var_args) {
        var parent = getPrototypeOf(this);
        if (parent !== proto) {
          // In the typical case, parent === proto, so the above test
          // lets the typical case succeed quickly.
          // Note that, even if parent === proto, that does not
          // necessarily mean that the method application will
          // succeed, since, for example, a non-Date can still inherit
          // from Date.prototype. However, in such cases, the built-in
          // method application will fail on its own without our help.
          if (objToString.call(parent) !== baseToString) {
            // As above, === baseToString does not necessarily mean
            // success, but the built-in failure again would not need
            // our help.
            var thisToString = objToString.call(this);
            if (thisToString === baseToString) {
              throw new TypeError('May not mutate internal state of a ' +
                                  classString + '.prototype');
            } else {
              throw new TypeError('Unexpected: ' + thisToString);
            }
          }
        }
        return originalMethod.apply(this, arguments);
      }
      Object.defineProperty(proto, name, { value: replacement });
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
    Object.defineProperty(Array.prototype, 'forEach', {
      // Taken from https://developer.mozilla.org/en-US/docs/JavaScript/Reference/Global_Objects/Array/forEach
      value: function(callback, thisArg) { 
        var T, k;
        if (this === null || this === undefined) {
          throw new TypeError("this is null or not defined");
        }
        var O = Object(this);
        var len = O.length >>> 0;
        if (objToString.call(callback) !== "[object Function]") {
          throw new TypeError(callback + " is not a function");
        }
        T = thisArg;
        k = 0;
        while(k < len) {
          var kValue;
          if (k in O) {
            kValue = O[k];
            callback.call(T, kValue, k, O);
          }
          k++;
        }
      }
    });
  }


  function repair_NEEDS_DUMMY_SETTER() {
    var defProp = Object.defineProperty;
    var gopd = Object.getOwnPropertyDescriptor;

    function dummySetter(newValue) {
      throw new TypeError('no setter for assigning: ' + newValue);
    }
    dummySetter.prototype = null;
    rememberToTamperProof(dummySetter);

    defProp(Object, 'defineProperty', {
      value: function setSetterDefProp(base, name, desc) {
        if (typeof desc.get === 'function' && desc.set === void 0) {
          var oldDesc = gopd(base, name);
          if (oldDesc) {
            var testBase = {};
            defProp(testBase, name, oldDesc);
            defProp(testBase, name, desc);
            desc = gopd(testBase, name);
            if (desc.set === void 0) { desc.set = dummySetter; }
          } else {
            if (objToString.call(base) === '[object HTMLFormElement]') {
              // This repair was triggering bug
              // http://code.google.com/p/chromium/issues/detail?id=94666
              // on Chrome, causing
              // http://code.google.com/p/google-caja/issues/detail?id=1401
              // so if base is an HTMLFormElement we skip this
              // fix. Since this repair and this situation are both
              // Chrome only, it is ok that we're conditioning this on
              // the unspecified [[Class]] value of base.
              //
              // To avoid the further bug identified at Comment 2
              // http://code.google.com/p/chromium/issues/detail?id=94666#c2
              // We also have to reconstruct the requested desc so that
              // the setter is absent. This is why we additionally
              // condition this special case on the absence of an own
              // name property on base.
              var desc2 = { get: desc.get };
              if ('enumerable' in desc) {
                desc2.enumerable = desc.enumerable;
              }
              if ('configurable' in desc) {
                desc2.configurable = desc.configurable;
              }
              var result = defProp(base, name, desc2);
              var newDesc = gopd(base, name);
              if (newDesc.get === desc.get) {
                return result;
              }
            }
            desc.set = dummySetter;
          }
        }
        return defProp(base, name, desc);
      }
    });
    NEEDS_DUMMY_SETTER_repaired = true;
  }


  function repair_ACCESSORS_INHERIT_AS_OWN() {
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
            !fullDesc.configurable) {
          logger.warn(complaint);
          throw new TypeError(complaint
              + " (Object: " + base + " Property: " + name + ")");
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
          throw new TypeError(complaint + " (During sealing. Object: "
              + base + " Property: " + name + ")");
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
  }

  function repair_SORT_LEAKS_GLOBAL() {
    var unsafeSort = Array.prototype.sort;
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
    Object.defineProperty(Array.prototype, 'sort', {
      value: sortWrapper
    });
  }

  function repair_REPLACE_LEAKS_GLOBAL() {
    var unsafeReplace = String.prototype.replace;
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
    Object.defineProperty(String.prototype, 'replace', {
      value: replaceWrapper
    });
  }

  function repair_CANT_GOPD_CALLER() {
    var unsafeGOPD = Object.getOwnPropertyDescriptor;
    function gopdWrapper(base, name) {
      try {
        return unsafeGOPD(base, name);
      } catch (err) {
        if (err instanceof TypeError &&
            typeof base === 'function' &&
            (name === 'caller' || name === 'arguments')) {
          return (function(message) {
             function fakePoison() { throw new TypeError(message); }
             fakePoison.prototype = null;
             return {
               get: fakePoison,
               set: fakePoison,
               enumerable: false,
               configurable: false
             };
           })(err.message);
        }
        throw err;
      }
    }
    Object.defineProperty(Object, 'getOwnPropertyDescriptor', {
      value: gopdWrapper
    });
  }

  function repair_CANT_HASOWNPROPERTY_CALLER() {
    Object.defineProperty(Object.prototype, 'hasOwnProperty', {
      value: function hopWrapper(name) {
        return !!Object.getOwnPropertyDescriptor(this, name);
      }
    });
  }

  function makeHarmless(magicName, func, path) {
    function poison() {
      throw new TypeError('Cannot access property ' + path);
    }
    poison.prototype = null;
    var desc = Object.getOwnPropertyDescriptor(func, magicName);
    if ((!desc && Object.isExtensible(func)) || desc.configurable) {
      try {
        Object.defineProperty(func, magicName, {
          get: poison,
          set: poison,
          configurable: false
        });
      } catch (cantPoisonErr) {
        return 'Poisoning failed with ' + cantPoisonErr;
      }
      desc = Object.getOwnPropertyDescriptor(func, magicName);
      if (desc &&
          desc.get === poison &&
          desc.set === poison &&
          !desc.configurable) {
        return 'Apparently poisoned';
      }
      return 'Not poisoned';
    }
    if ('get' in desc || 'set' in desc) {
      return 'Apparently safe';
    }
    try {
      Object.defineProperty(func, magicName, {
        value: desc.value === null ? null : void 0,
        writable: false,
        configurable: false
      });
    } catch (cantFreezeHarmlessErr) {
      return 'Freezing harmless failed with ' + cantFreezeHarmlessErr;
    }
    desc = Object.getOwnPropertyDescriptor(func, magicName);
    if (desc &&
        (desc.value === null || desc.value === void 0) &&
        !desc.writable &&
        !desc.configurable) {
      return 'Apparently frozen harmless';
    }
    return 'Did not freeze harmless';
  }

  function repair_BUILTIN_LEAKS_CALLER() {
    // The call to .bind as a method here is fine since it happens
    // after all repairs which might fix .bind and before any
    // untrusted code runs.
    ses.makeCallerHarmless = makeHarmless.bind(void 0, 'caller');
    //logger.info(ses.makeCallerHarmless(builtInMapMethod));
  }

  function repair_BUILTIN_LEAKS_ARGUMENTS() {
    // The call to .bind as a method here is fine since it happens
    // after all repairs which might fix .bind and before any
    // untrusted code runs.
    ses.makeArgumentsHarmless = makeHarmless.bind(void 0, 'arguments');
    //logger.info(ses.makeArgumentsHarmless(builtInMapMethod));
  }

  function repair_DELETED_BUILTINS_IN_OWN_NAMES() {
    var realGOPN = Object.getOwnPropertyNames;
    var repairedHop = Object.prototype.hasOwnProperty;
    function getOnlyRealOwnPropertyNames(base) {
      return realGOPN(base).filter(function(name) {
        return repairedHop.call(base, name);
      });
    }
    Object.defineProperty(Object, 'getOwnPropertyNames', {
      value: getOnlyRealOwnPropertyNames
    });
  }

  function repair_GETOWNPROPDESC_OF_ITS_OWN_CALLER_FAILS() {
    var realGOPD = Object.getOwnPropertyDescriptor;
    function GOPDWrapper(base, name) {
      return realGOPD(base, name);
    }
    Object.defineProperty(Object, 'getOwnPropertyDescriptor', {
      value: GOPDWrapper
    });
  }

  function repair_JSON_PARSE_PROTO_CONFUSION() {
    var unsafeParse = JSON.parse;
    function validate(plainJSON) {
      if (plainJSON !== Object(plainJSON)) {
        // If we were trying to do a full validation, we would
        // validate that it is not NaN, Infinity, -Infinity, or
        // (if nested) undefined. However, we are currently only
        // trying to repair
        // http://code.google.com/p/v8/issues/detail?id=621
        // That's why this special case validate function is private
        // to this repair.
        return;
      }
      var proto = getPrototypeOf(plainJSON);
      if (proto !== Object.prototype && proto !== Array.prototype) {
        throw new TypeError(
          'Parse resulted in invalid JSON. ' +
            'See http://code.google.com/p/v8/issues/detail?id=621');
      }
      Object.keys(plainJSON).forEach(function(key) {
        validate(plainJSON[key]);
      });
    }
    Object.defineProperty(JSON, 'parse', {
      value: function parseWrapper(text, opt_reviver) {
        var result = unsafeParse(text);
        validate(result);
        if (opt_reviver) {
          return unsafeParse(text, opt_reviver);
        } else {
          return result;
        }
      },
      writable: true,
      enumerable: false,
      configurable: true
    });
  }

  function repair_PARSEINT_STILL_PARSING_OCTAL() {
    var badParseInt = parseInt;
    function goodParseInt(n, radix) {
      n = '' + n;
      // This turns an undefined radix into a NaN but is ok since NaN
      // is treated as undefined by badParseInt
      radix = +radix;
      var isHexOrOctal = /^\s*[+-]?\s*0(x?)/.exec(n);
      var isOct = isHexOrOctal ? isHexOrOctal[1] !== 'x' : false;

      if (isOct && (radix !== radix || 0 === radix)) {
        return badParseInt(n, 10);
      }
      return badParseInt(n, radix);
    }
    parseInt = goodParseInt;
  }

  function repair_ASSIGN_CAN_OVERRIDE_FROZEN() {
    makeTamperProof = function simpleMakeTamperProof() {
      return Object.freeze;
    };
  }

  function repair_CANT_REDEFINE_NAN_TO_ITSELF() {
    var defProp = Object.defineProperty;
    // 'value' handled separately
    var attrs = ['writable', 'get', 'set', 'enumerable', 'configurable'];

    defProp(Object, 'defineProperty', {
      value: function(base, name, desc) {
        try {
          return defProp(base, name, desc);
        } catch (err) {
          var oldDesc = Object.getOwnPropertyDescriptor(base, name);
          for (var i = 0, len = attrs.length; i < len; i++) {
            var attr = attrs[i];
            if (attr in desc && desc[attr] !== oldDesc[attr]) { throw err; }
          }
          if (!('value' in desc) || is(desc.value, oldDesc.value)) {
            return base;
          }
          throw err;
        }
      }
    });
  }

  function repair_POP_IGNORES_FROZEN() {
    var pop = Array.prototype.pop;
    var frozen = Object.isFrozen;
    Object.defineProperty(Array.prototype, 'pop', {
      value: function () {
        if (frozen(this)) {
          throw new TypeError('Cannot pop a frozen object.');
        }
        return pop.call(this);
      },
      configurable : true,
      writable: true
    });
  }

  function repair_SORT_IGNORES_FROZEN() {
    var sort = Array.prototype.sort;
    var frozen = Object.isFrozen;
    Object.defineProperty(Array.prototype, 'sort', {
      value: function (compareFn) {
        if (frozen(this)) {
          throw new TypeError('Cannot sort a frozen object.');
        }
        if (arguments.length === 0) {
          return sort.call(this);
        } else {
          return sort.call(this, compareFn);
        }
      },
      configurable: true,
      writable: true
    });
  }

  function repair_PUSH_IGNORES_SEALED() {
    var push = Array.prototype.push;
    var sealed = Object.isSealed;
    Object.defineProperty(Array.prototype, 'push', {
      value: function (compareFn) {
        if (sealed(this)) {
          throw new TypeError('Cannot push onto a sealed object.');
        }
        return push.apply(this, arguments);
      },
      configurable: true,
      writable: true
    });
  }

  ////////////////////// Kludge Records /////////////////////
  //
  // Each kludge record has a <dl>
  //   <dt>description:</dt>
  //     <dd>a string describing the problem</dd>
  //   <dt>test:</dt>
  //     <dd>a predicate testing for the presence of the problem</dd>
  //   <dt>repair:</dt>
  //     <dd>a function which attempts repair, or undefined if no
  //         repair is attempted for this problem</dd>
  //   <dt>preSeverity:</dt>
  //     <dd>an enum (see below) indicating the level of severity of
  //         this problem if unrepaired. Or, if !canRepair, then
  //         the severity whether or not repaired.</dd>
  //   <dt>canRepair:</dt>
  //     <dd>a boolean indicating "if the repair exists and the test
  //         subsequently does not detect a problem, are we now ok?"</dd>
  //   <dt>urls:</dt>
  //     <dd>a list of URL strings, each of which points at a page
  //         relevant for documenting or tracking the bug in
  //         question. These are typically into bug-threads in issue
  //         trackers for the various browsers.</dd>
  //   <dt>sections:</dt>
  //     <dd>a list of strings, each of which is a relevant ES5.1
  //         section number.</dd>
  //   <dt>tests:</dt>
  //     <dd>a list of strings, each of which is the name of a
  //         relevant test262 or sputnik test case.</dd>
  // </dl>
  // These kludge records are the meta-data driving the testing and
  // repairing.

  var severities = ses.severities;
  var statuses = ses.statuses;

  /**
   * First test whether the platform can even support our repair
   * attempts.
   */
  var baseKludges = [
    {
      description: 'Missing getOwnPropertyNames',
      test: test_MISSING_GETOWNPROPNAMES,
      repair: void 0,
      preSeverity: severities.NOT_SUPPORTED,
      canRepair: false,
      urls: [],
      sections: ['15.2.3.4'],
      tests: ['15.2.3.4-0-1']
    }
  ];

  /**
   * Run these only if baseKludges report success.
   */
  var supportedKludges = [
    {
      description: 'Global object leaks from global function calls',
      test: test_GLOBAL_LEAKS_FROM_GLOBAL_FUNCTION_CALLS,
      repair: void 0,
      preSeverity: severities.NOT_ISOLATED,
      canRepair: false,
      urls: ['https://bugs.webkit.org/show_bug.cgi?id=64250'],
      sections: ['10.2.1.2', '10.2.1.2.6'],
      tests: ['10.4.3-1-8gs']
    },
    {
      description: 'Global object leaks from anonymous function calls',
      test: test_GLOBAL_LEAKS_FROM_ANON_FUNCTION_CALLS,
      repair: void 0,
      preSeverity: severities.NOT_ISOLATED,
      canRepair: false,
      urls: [],
      sections: ['10.4.3'],
      tests: ['S10.4.3_A1']
    },
    {
      description: 'Global leaks through strict this',
      test: test_GLOBAL_LEAKS_FROM_STRICT_THIS,
      repair: void 0,
      preSeverity: severities.NOT_ISOLATED,
      canRepair: false,
      urls: [],
      sections: ['10.4.3'],
      tests: ['10.4.3-1-8gs', '10.4.3-1-8-s']
    },
    {
      description: 'Global object leaks from built-in methods',
      test: test_GLOBAL_LEAKS_FROM_BUILTINS,
      repair: void 0,
      preSeverity: severities.NOT_ISOLATED,
      canRepair: false,
      urls: ['https://bugs.webkit.org/show_bug.cgi?id=51097',
             'https://bugs.webkit.org/show_bug.cgi?id=58338',
             'http://code.google.com/p/v8/issues/detail?id=1437',
             'https://connect.microsoft.com/IE/feedback/details/' +
               '685430/global-object-leaks-from-built-in-methods'],
      sections: ['15.2.4.4'],
      tests: ['S15.2.4.4_A14']
    },
    {
      description: 'Global object leaks from globally called built-in methods',
      test: test_GLOBAL_LEAKS_FROM_GLOBALLY_CALLED_BUILTINS,
      repair: void 0,
      preSeverity: severities.NOT_ISOLATED,
      canRepair: false,
      urls: [],
      sections: ['10.2.1.2', '10.2.1.2.6', '15.2.4.4'],
      tests: ['S15.2.4.4_A15']
    },
    {
      description: 'Object.freeze is missing',
      test: test_MISSING_FREEZE_ETC,
      repair: repair_MISSING_FREEZE_ETC,
      preSeverity: severities.NOT_OCAP_SAFE,
      canRepair: false,           // repair for development, not safety
      urls: ['https://bugs.webkit.org/show_bug.cgi?id=55736'],
      sections: ['15.2.3.9'],
      tests: ['15.2.3.9-0-1']
    },
    {
      description: 'A function.prototype\'s descriptor lies',
      test: test_FUNCTION_PROTOTYPE_DESCRIPTOR_LIES,
      repair: repair_DEFINE_PROPERTY,
      preSeverity: severities.UNSAFE_SPEC_VIOLATION,
      canRepair: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=1530',
             'http://code.google.com/p/v8/issues/detail?id=1570'],
      sections: ['15.2.3.3', '15.2.3.6', '15.3.5.2'],
      tests: ['S15.3.3.1_A4']
    },
    {
      description: 'Phantom callee on strict functions',
      test: test_MISSING_CALLEE_DESCRIPTOR,
      repair: repair_MISSING_CALLEE_DESCRIPTOR,
      preSeverity: severities.UNSAFE_SPEC_VIOLATION,
      canRepair: true,
      urls: ['https://bugs.webkit.org/show_bug.cgi?id=55537'],
      sections: ['15.2.3.4'],
      tests: ['S15.2.3.4_A1_T1']
    },
    {
      description: 'Strict delete returned false rather than throwing',
      test: test_STRICT_DELETE_RETURNS_FALSE,
      repair: void 0,
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: false,
      urls: ['https://connect.microsoft.com/IE/feedback/details/' +
               '685432/strict-delete-sometimes-returns-false-' +
               'rather-than-throwing'],
      sections: ['11.4.1'],
      tests: ['S11.4.1_A5']
    },
    {
      description: 'Non-deletable RegExp statics are a' +
        ' global communication channel',
      test: test_REGEXP_CANT_BE_NEUTERED,
      repair: repair_REGEXP_CANT_BE_NEUTERED,
      preSeverity: severities.NOT_OCAP_SAFE,
      canRepair: true,
      urls: ['https://bugzilla.mozilla.org/show_bug.cgi?id=591846',
             'http://wiki.ecmascript.org/doku.php?id=' +
               'conventions:make_non-standard_properties_configurable',
             'https://connect.microsoft.com/IE/feedback/details/' +
               '685439/non-deletable-regexp-statics-are-a-global-' +
               'communication-channel'],
      sections: ['11.4.1'],
      tests: ['S11.4.1_A5']
    },
    {
      description: 'RegExp.exec leaks match globally',
      test: test_REGEXP_TEST_EXEC_UNSAFE,
      repair: repair_REGEXP_TEST_EXEC_UNSAFE,
      preSeverity: severities.NOT_OCAP_SAFE,
      canRepair: true,
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
      preSeverity: severities.UNSAFE_SPEC_VIOLATION,
      canRepair: true,
      urls: ['https://bugs.webkit.org/show_bug.cgi?id=26382',
             'https://bugs.webkit.org/show_bug.cgi?id=42371'],
      sections: ['15.3.4.5'],
      tests: ['S15.3.4.5_A3']
    },
    {
      description: 'Function.prototype.bind calls .apply rather than [[Call]]',
      test: test_BIND_CALLS_APPLY,
      repair: repair_MISSING_BIND,
      preSeverity: severities.UNSAFE_SPEC_VIOLATION,
      canRepair: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=892',
             'http://code.google.com/p/v8/issues/detail?id=828'],
      sections: ['15.3.4.5.1'],
      tests: ['S15.3.4.5_A4']
    },
    {
      description: 'Function.prototype.bind does not curry construction',
      test: test_BIND_CANT_CURRY_NEW,
      repair: void 0, // JS-based repair essentially impossible
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: false,
      urls: ['https://bugs.webkit.org/show_bug.cgi?id=26382#c29'],
      sections: ['15.3.4.5.2'],
      tests: ['S15.3.4.5_A5']
    },
    {
      description: 'Date.prototype is a global communication channel',
      test: test_MUTABLE_DATE_PROTO,
      repair: repair_MUTABLE_DATE_PROTO,
      preSeverity: severities.NOT_OCAP_SAFE,
      canRepair: true,
      urls: ['http://code.google.com/p/google-caja/issues/detail?id=1362'],
      sections: ['15.9.5'],
      tests: []
    },
    {
      description: 'WeakMap.prototype is a global communication channel',
      test: test_MUTABLE_WEAKMAP_PROTO,
      repair: repair_MUTABLE_WEAKMAP_PROTO,
      preSeverity: severities.NOT_OCAP_SAFE,
      canRepair: true,
      urls: ['https://bugzilla.mozilla.org/show_bug.cgi?id=656828'],
      sections: [],
      tests: []
    },
    {
      description: 'Array forEach cannot be frozen while in progress',
      test: test_NEED_TO_WRAP_FOREACH,
      repair: repair_NEED_TO_WRAP_FOREACH,
      preSeverity: severities.UNSAFE_SPEC_VIOLATION,
      canRepair: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=1447'],
      sections: ['15.4.4.18'],
      tests: ['S15.4.4.18_A1', 'S15.4.4.18_A2']
    },
    {
      description: 'Array forEach converts primitive thisObj arg to object',
      test: test_FOREACH_COERCES_THISOBJ,
      repair: repair_NEED_TO_WRAP_FOREACH,
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=2273',
          'https://developer.mozilla.org/en-US/docs/JavaScript/Reference/Global_Objects/Array/forEach'],
      sections: ['15.4.4.18'],
      tests: []
    },
    {
      description: 'Workaround undiagnosed need for dummy setter',
      test: test_NEEDS_DUMMY_SETTER,
      repair: repair_NEEDS_DUMMY_SETTER,
      preSeverity: severities.UNSAFE_SPEC_VIOLATION,
      canRepair: true,
      urls: [],
      sections: [],
      tests: []
    },
    {
      description: 'Getter on HTMLFormElement disappears',
      test: test_FORM_GETTERS_DISAPPEAR,
      repair: repair_NEEDS_DUMMY_SETTER,
      preSeverity: severities.UNSAFE_SPEC_VIOLATION,
      canRepair: true,
      urls: ['http://code.google.com/p/chromium/issues/detail?id=94666',
             'http://code.google.com/p/v8/issues/detail?id=1651',
             'http://code.google.com/p/google-caja/issues/detail?id=1401'],
      sections: ['15.2.3.6'],
      tests: ['S15.2.3.6_A1']
    },
    {
      description: 'Accessor properties inherit as own properties',
      test: test_ACCESSORS_INHERIT_AS_OWN,
      repair: repair_ACCESSORS_INHERIT_AS_OWN,
      preSeverity: severities.UNSAFE_SPEC_VIOLATION,
      canRepair: true,
      urls: ['https://bugzilla.mozilla.org/show_bug.cgi?id=637994'],
      sections: ['8.6.1', '15.2.3.6'],
      tests: ['S15.2.3.6_A2']
    },
    {
      description: 'Array sort leaks global',
      test: test_SORT_LEAKS_GLOBAL,
      repair: repair_SORT_LEAKS_GLOBAL,
      preSeverity: severities.NOT_ISOLATED,
      canRepair: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=1360'],
      sections: ['15.4.4.11'],
      tests: ['S15.4.4.11_A8']
    },
    {
      description: 'String replace leaks global',
      test: test_REPLACE_LEAKS_GLOBAL,
      repair: repair_REPLACE_LEAKS_GLOBAL,
      preSeverity: severities.NOT_ISOLATED,
      canRepair: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=1360',
             'https://connect.microsoft.com/IE/feedback/details/' +
               '685928/bad-this-binding-for-callback-in-string-' +
               'prototype-replace'],
      sections: ['15.5.4.11'],
      tests: ['S15.5.4.11_A12']
    },
    {
      description: 'getOwnPropertyDescriptor on strict "caller" throws',
      test: test_CANT_GOPD_CALLER,
      repair: repair_CANT_GOPD_CALLER,
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: true,
      urls: ['https://connect.microsoft.com/IE/feedback/details/' +
               '685436/getownpropertydescriptor-on-strict-caller-throws'],
      sections: ['15.2.3.3', '13.2', '13.2.3'],
      tests: ['S13.2_A6_T1']
    },
    {
      description: 'strict_function.hasOwnProperty("caller") throws',
      test: test_CANT_HASOWNPROPERTY_CALLER,
      repair: repair_CANT_HASOWNPROPERTY_CALLER,
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: true,
      urls: ['https://bugs.webkit.org/show_bug.cgi?id=63398#c3'],
      sections: ['15.2.4.5', '13.2', '13.2.3'],
      tests: ['S13.2_A7_T1']
    },
    {
      description: 'Cannot "in" caller on strict function',
      test: test_CANT_IN_CALLER,
      repair: void 0,
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: false,
      urls: ['https://bugs.webkit.org/show_bug.cgi?id=63398'],
      sections: ['11.8.7', '13.2', '13.2.3'],
      tests: ['S13.2_A8_T1']
    },
    {
      description: 'Cannot "in" arguments on strict function',
      test: test_CANT_IN_ARGUMENTS,
      repair: void 0,
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: false,
      urls: ['https://bugs.webkit.org/show_bug.cgi?id=63398'],
      sections: ['11.8.7', '13.2', '13.2.3'],
      tests: ['S13.2_A8_T2']
    },
    {
      description: 'Strict "caller" not poisoned',
      test: test_STRICT_CALLER_NOT_POISONED,
      repair: void 0,
      preSeverity: severities.NOT_OCAP_SAFE,
      canRepair: false,
      urls: [],
      sections: ['13.2'],
      tests: ['S13.2.3_A1']
    },
    {
      description: 'Strict "arguments" not poisoned',
      test: test_STRICT_ARGUMENTS_NOT_POISONED,
      repair: void 0,
      preSeverity: severities.NOT_OCAP_SAFE,
      canRepair: false,
      urls: [],
      sections: ['13.2'],
      tests: ['S13.2.3_A1']
    },
    {
      description: 'Built in functions leak "caller"',
      test: test_BUILTIN_LEAKS_CALLER,
      repair: repair_BUILTIN_LEAKS_CALLER,
      preSeverity: severities.NOT_OCAP_SAFE,
      canRepair: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=1643',
             'http://code.google.com/p/v8/issues/detail?id=1548',
             'https://bugzilla.mozilla.org/show_bug.cgi?id=591846',
             'http://wiki.ecmascript.org/doku.php?id=' +
               'conventions:make_non-standard_properties_configurable'],
      sections: [],
      tests: ['Sbp_A10_T1']
    },
    {
      description: 'Built in functions leak "arguments"',
      test: test_BUILTIN_LEAKS_ARGUMENTS,
      repair: repair_BUILTIN_LEAKS_ARGUMENTS,
      preSeverity: severities.NOT_OCAP_SAFE,
      canRepair: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=1643',
             'http://code.google.com/p/v8/issues/detail?id=1548',
             'https://bugzilla.mozilla.org/show_bug.cgi?id=591846',
             'http://wiki.ecmascript.org/doku.php?id=' +
               'conventions:make_non-standard_properties_configurable'],
      sections: [],
      tests: ['Sbp_A10_T2']
    },
    {
      description: 'Bound functions leak "caller"',
      test: test_BOUND_FUNCTION_LEAKS_CALLER,
      repair: repair_MISSING_BIND,
      preSeverity: severities.NOT_OCAP_SAFE,
      canRepair: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=893',
             'https://bugs.webkit.org/show_bug.cgi?id=63398'],
      sections: ['15.3.4.5'],
      tests: ['S13.2.3_A1', 'S15.3.4.5_A1']
    },
    {
      description: 'Bound functions leak "arguments"',
      test: test_BOUND_FUNCTION_LEAKS_ARGUMENTS,
      repair: repair_MISSING_BIND,
      preSeverity: severities.NOT_OCAP_SAFE,
      canRepair: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=893',
             'https://bugs.webkit.org/show_bug.cgi?id=63398'],
      sections: ['15.3.4.5'],
      tests: ['S13.2.3_A1', 'S15.3.4.5_A2']
    },
    {
      description: 'Deleting built-in leaves phantom behind',
      test: test_DELETED_BUILTINS_IN_OWN_NAMES,
      repair: repair_DELETED_BUILTINS_IN_OWN_NAMES,
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: true,
      urls: ['https://bugs.webkit.org/show_bug.cgi?id=70207'],
      sections: ['15.2.3.4'],
      tests: []
    },
    {
      description: 'getOwnPropertyDescriptor on its own "caller" fails',
      test: test_GETOWNPROPDESC_OF_ITS_OWN_CALLER_FAILS,
      repair: repair_GETOWNPROPDESC_OF_ITS_OWN_CALLER_FAILS,
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=1769'],
      sections: ['13.2', '15.2.3.3'],
      tests: []
    },
    {
      description: 'JSON.parse confused by "__proto__"',
      test: test_JSON_PARSE_PROTO_CONFUSION,
      repair: repair_JSON_PARSE_PROTO_CONFUSION,
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=621',
             'http://code.google.com/p/v8/issues/detail?id=1310'],
      sections: ['15.12.2'],
      tests: ['S15.12.2_A1']
    },
    {
      description: 'Prototype still mutable on non-extensible object',
      test: test_PROTO_NOT_FROZEN,
      repair: void 0,
      preSeverity: severities.NOT_OCAP_SAFE,
      canRepair: false,
      urls: ['https://bugs.webkit.org/show_bug.cgi?id=65832'],
      sections: ['8.6.2'],
      tests: ['S8.6.2_A8']
    },
    {
      description: 'Prototype still redefinable on non-extensible object',
      test: test_PROTO_REDEFINABLE,
      repair: void 0,
      preSeverity: severities.NOT_OCAP_SAFE,
      canRepair: false,
      urls: ['https://bugs.webkit.org/show_bug.cgi?id=65832'],
      sections: ['8.6.2'],
      tests: ['S8.6.2_A8'] 
    },
    {
      description: 'Defining __proto__ on non-extensible object fails silently',
      test: test_DEFINING_READ_ONLY_PROTO_FAILS_SILENTLY,
      repair: repair_DEFINE_PROPERTY,
      preSeverity: severities.NO_KNOWN_EXPLOIT_SPEC_VIOLATION,
      canRepair: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=2441'],
      sections: ['8.6.2'],
      tests: [] // TODO(erights): Add to test262
    },
    {
      description: 'Strict eval function leaks variable definitions',
      test: test_STRICT_EVAL_LEAKS_GLOBALS,
      repair: void 0,
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: false,
      urls: ['http://code.google.com/p/v8/issues/detail?id=1624'],
      sections: ['10.4.2.1'],
      tests: ['S10.4.2.1_A1']
    },
    {
      description: 'Eval breaks masking of named functions in non-strict code',
      test: test_EVAL_BREAKS_MASKING,
      repair: void 0,
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: false,
      urls: ['http://code.google.com/p/v8/issues/detail?id=2396'],
      sections: ['10.2'],
      tests: [] // TODO(erights): Add to test262
    },
    {
      description: 'parseInt still parsing octal',
      test: test_PARSEINT_STILL_PARSING_OCTAL,
      repair: repair_PARSEINT_STILL_PARSING_OCTAL,
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=1645'],
      sections: ['15.1.2.2'],
      tests: ['S15.1.2.2_A5.1_T1']
    },
    {
      description: 'E4X literals allowed in strict code',
      test: test_STRICT_E4X_LITERALS_ALLOWED,
      repair: void 0,
      preSeverity: severities.NOT_ISOLATED,
      canRepair: false,
      urls: ['https://bugzilla.mozilla.org/show_bug.cgi?id=695577',
             'https://bugzilla.mozilla.org/show_bug.cgi?id=695579'],
      sections: [],
      tests: []
    },
    {
      description: 'Assignment can override frozen inherited property',
      test: test_ASSIGN_CAN_OVERRIDE_FROZEN,
      repair: repair_ASSIGN_CAN_OVERRIDE_FROZEN,
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: false,
      urls: ['http://code.google.com/p/v8/issues/detail?id=1169',
             'https://mail.mozilla.org/pipermail/es-discuss/' +
               '2011-November/017997.html'],
      sections: ['8.12.4'],
      tests: ['15.2.3.6-4-405']
    },
    {
      description: 'Array.prototype.pop ignores frozeness',
      test: test_POP_IGNORES_FROZEN,
      repair: repair_POP_IGNORES_FROZEN,
      preSeverity: severities.UNSAFE_SPEC_VIOLATION,
      canRepair: true,
      urls: ['https://bugs.webkit.org/show_bug.cgi?id=75788'],
      sections: ['15.4.4.6'],
      tests: [] // TODO(erights): Add to test262
    },
    {
      description: 'Array.prototype.sort ignores frozeness',
      test: test_SORT_IGNORES_FROZEN,
      repair: repair_SORT_IGNORES_FROZEN,
      preSeverity: severities.UNSAFE_SPEC_VIOLATION,
      canRepair: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=2419'],
      sections: ['15.4.4.11'],
      tests: [] // TODO(erights): Add to test262
    },
    {
      description: 'Array.prototype.push ignores sealing',
      test: test_PUSH_IGNORES_SEALED,
      repair: repair_PUSH_IGNORES_SEALED,
      preSeverity: severities.UNSAFE_SPEC_VIOLATION,
      canRepair: true,
      urls: ['http://code.google.com/p/v8/issues/detail?id=2412'],
      sections: ['15.4.4.11'],
      tests: [] // TODO(erights): Add to test262
    },
    {
      description: 'Setting [].length can delete non-configurable elements',
      test: test_ARRAYS_DELETE_NONCONFIGURABLE,
      repair: void 0,
      preSeverity: severities.NO_KNOWN_EXPLOIT_SPEC_VIOLATION,
      canRepair: false,
      urls: ['https://bugzilla.mozilla.org/show_bug.cgi?id=590690'],
      sections: ['15.4.5.2'],
      tests: [] // TODO(erights): Add to test262
    },
    {
      description: 'Extending an array can modify read-only array length',
      test: test_ARRAYS_MODIFY_READONLY,
      repair: void 0,
      preSeverity: severities.NO_KNOWN_EXPLOIT_SPEC_VIOLATION,
      canRepair: false,
      urls: ['http://code.google.com/p/v8/issues/detail?id=2379'],
      sections: ['15.4.5.1.3.f'],
      tests: [] // TODO(erights): Add to test262
    },
    {
      description: 'Cannot redefine global NaN to itself',
      test: test_CANT_REDEFINE_NAN_TO_ITSELF,
      repair: repair_CANT_REDEFINE_NAN_TO_ITSELF,
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: true,
      urls: [], // Seen on WebKit Nightly. TODO(erights): report
      sections: ['8.12.9', '15.1.1.1'],
      tests: [] // TODO(erights): Add to test262
    },
    {
      description: 'Firefox 15 cross-frame freeze problem',
      test: test_FIREFOX_15_FREEZE_PROBLEM,
      repair: void 0,
      preSeverity: severities.NOT_ISOLATED,
      canRepair: false,
      urls: ['https://bugzilla.mozilla.org/show_bug.cgi?id=784892',
             'https://bugzilla.mozilla.org/show_bug.cgi?id=674195'],
      sections: [],
      tests: []
    },
    {
      description: 'Error instances have unexpected properties',
      test: test_UNEXPECTED_ERROR_PROPERTIES,
      repair: void 0,
      preSeverity: severities.NEW_SYMPTOM,
      canRepair: false,
      urls: [],
      sections: [],
      tests: []
    },
    {
      description: 'Error instances may have invisible properties',
      test: test_ERRORS_HAVE_INVISIBLE_PROPERTIES,
      repair: void 0,
      preSeverity: severities.NOT_ISOLATED,
      canRepair: false,
      urls: [],  // TODO(felix8a): need bugs filed
      sections: [],
      tests: []
    }
  ];

  ////////////////////// Testing, Repairing, Reporting ///////////

  var aboutTo = void 0;

  /**
   * Run a set of tests & repairs, and report results.
   *
   * <p>First run all the tests before repairing anything.
   * Then repair all repairable failed tests.
   * Some repair might fix multiple problems, but run each repair at most once.
   * Then run all the tests again, in case some repairs break other tests.
   * And finally return a list of records of results.
   */
  function testRepairReport(kludges) {
    var beforeFailures = strictMapFn(kludges, function(kludge) {
      aboutTo = ['pre test: ', kludge.description];
      return kludge.test();
    });
    var repairs = [];
    strictForEachFn(kludges, function(kludge, i) {
      if (beforeFailures[i]) {
        var repair = kludge.repair;
        if (repair && repairs.lastIndexOf(repair) === -1) {
          aboutTo = ['repair: ', kludge.description];
          repair();
          repairs.push(repair);
        }
      }
    });
    var afterFailures = strictMapFn(kludges, function(kludge) {
      aboutTo = ['post test: ', kludge.description];
      return kludge.test();
    });

    if (Object.isFrozen && Object.isFrozen(Array.prototype.forEach)) {
      // Need to do it anyway, to repair the sacrificial freezing we
      // needed to do to test. Once we can permanently retire this
      // test, we can also retire the redundant repair.
      repair_NEED_TO_WRAP_FOREACH();
    }

    return strictMapFn(kludges, function(kludge, i) {
      var status = statuses.ALL_FINE;
      var postSeverity = severities.SAFE;
      var beforeFailure = beforeFailures[i];
      var afterFailure = afterFailures[i];
      if (beforeFailure) { // failed before
        if (afterFailure) { // failed after
          if (kludge.repair) {
            postSeverity = kludge.preSeverity;
            status = statuses.REPAIR_FAILED;
          } else {
            if (!kludge.canRepair) {
              postSeverity = kludge.preSeverity;
            } // else no repair + canRepair -> problem isn't safety issue
            status = statuses.NOT_REPAIRED;
          }
        } else { // succeeded after
          if (kludge.repair) {
            if (!kludge.canRepair) {
              // repair for development, not safety
              postSeverity = kludge.preSeverity;
              status = statuses.REPAIRED_UNSAFELY;
            } else {
              status = statuses.REPAIRED;
            }
          } else {
            status = statuses.ACCIDENTALLY_REPAIRED;
          }
        }
      } else { // succeeded before
        if (afterFailure) { // failed after
          if (kludge.repair || !kludge.canRepair) {
            postSeverity = kludge.preSeverity;
          } // else no repair + canRepair -> problem isn't safety issue
          status = statuses.BROKEN_BY_OTHER_ATTEMPTED_REPAIRS;
        } else { // succeeded after
          // nothing to see here, move along
        }
      }

      if (typeof beforeFailure === 'string') {
        logger.error('New Symptom: ' + beforeFailure);
        postSeverity = severities.NEW_SYMPTOM;
      }
      if (typeof afterFailure === 'string') {
        logger.error('New Symptom: ' + afterFailure);
        postSeverity = severities.NEW_SYMPTOM;
      }

      ses.updateMaxSeverity(postSeverity);

      return {
        description:   kludge.description,
        preSeverity:   kludge.preSeverity,
        canRepair:     kludge.canRepair,
        urls:          kludge.urls,
        sections:      kludge.sections,
        tests:         kludge.tests,
        status:        status,
        postSeverity:  postSeverity,
        beforeFailure: beforeFailure,
        afterFailure:  afterFailure
      };
    });
  }

  try {
    var reports = testRepairReport(baseKludges);
    if (ses.ok()) {
      reports.push.apply(reports, testRepairReport(supportedKludges));
    }
    logger.reportRepairs(reports);
  } catch (err) {
    ses.updateMaxSeverity(ses.severities.NOT_SUPPORTED);
    var during = aboutTo ? '(' + aboutTo.join('') + ') ' : '';
    logger.error('ES5 Repair ' + during + 'failed with: ', err);
  }

  logger.reportMax();

})(this);
