// Copyright (C) 2011 Google Inc.
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
 * This is a pure-ES5 implementation of ejectors, guards, and trademarks as
 * otherwise provided by ES5/3. It is incorporated into the cajaVM object by
 * hookupSESPlus.js.
 *
 * <p>Assumes ES5. Compatible with ES5-strict.
 *
 * // provides ses.ejectorsGuardsTrademarks
 * @author kpreid@switchb.org
 * @requires WeakMap, cajaVM
 * @overrides ses, ejectorsGuardsTrademarksModule
 */
var ses;

(function ejectorsGuardsTrademarksModule(){
  "use strict";

  ses.ejectorsGuardsTrademarks = function ejectorsGuardsTrademarks() {

    /**
     * During the call to {@code ejectorsGuardsTrademarks}, {@code
     * ejectorsGuardsTrademarks} must not call {@code cajaVM.def},
     * since startSES.js has not yet finished cleaning things. See the
     * doc-comments on the {@code extensions} parameter of
     * startSES.js.
     *
     * <p>Instead, we define here some conveniences for freezing just
     * enough without prematurely freezing primodial objects
     * transitively reachable from these.
     */
    var freeze = Object.freeze;
    var constFunc = cajaVM.constFunc;


    /**
     * Returns a new object whose only utility is its identity and (for
     * diagnostic purposes only) its name.
     */
    function Token(name) {
      name = '' + name;
      return freeze({
        toString: constFunc(function tokenToString() {
          return name;
        })
      });
    }


    ////////////////////////////////////////////////////////////////////////
    // Ejectors
    ////////////////////////////////////////////////////////////////////////

    /**
     * One-arg form is known in scheme as "call with escape
     * continuation" (call/ec).
     *
     * <p>In this analogy, a call to {@code callWithEjector} emulates a
     * labeled statement. The ejector passed to the {@code attemptFunc}
     * emulates the label part. The {@code attemptFunc} itself emulates
     * the statement being labeled. And a call to {@code eject} with
     * this ejector emulates the return-to-label statement.
     *
     * <p>We extend the normal notion of call/ec with an
     * {@code opt_failFunc} in order to give more the sense of a
     * {@code try/catch} (or similarly, the {@code escape} special
     * form in E). The {@code attemptFunc} is like the {@code try}
     * clause and the {@code opt_failFunc} is like the {@code catch}
     * clause. If omitted, {@code opt_failFunc} defaults to the
     * {@code identity} function.
     *
     * <p>{@code callWithEjector} creates a fresh ejector -- a one
     * argument function -- for exiting from this attempt. It then calls
     * {@code attemptFunc} passing that ejector as argument. If
     * {@code attemptFunc} completes without calling the ejector, then
     * this call to {@code callWithEjector} completes
     * likewise. Otherwise, if the ejector is called with an argument,
     * then {@code opt_failFunc} is called with that argument. The
     * completion of {@code opt_failFunc} is then the completion of the
     * {@code callWithEjector} as a whole.
     *
     * <p>The ejector stays live until {@code attemptFunc} is exited,
     * at which point the ejector is disabled. Calling a disabled
     * ejector throws.
     *
     * <p>Historic note: This was first invented by John C. Reynolds in
     * <a href="http://doi.acm.org/10.1145/800194.805852"
     * >Definitional interpreters for higher-order programming
     * languages</a>. Reynold's invention was a special form as in E,
     * rather than a higher order function as here and in call/ec.
     */
    function callWithEjector(attemptFunc, opt_failFunc) {
      var failFunc = opt_failFunc || function (x) { return x; };
      var disabled = false;
      var token = new Token('ejection');
      var stash = void 0;
      function ejector(result) {
        if (disabled) {
          throw new Error('ejector disabled');
        } else {
          // don't disable here.
          stash = result;
          throw token;
        }
      }
      constFunc(ejector);
      try {
        try {
          return attemptFunc(ejector);
        } finally {
          disabled = true;
        }
      } catch (e) {
        if (e === token) {
          return failFunc(stash);
        } else {
          throw e;
        }
      }
    }

    /**
     * Safely invokes {@code opt_ejector} with {@code result}.
     * <p>
     * If {@code opt_ejector} is falsy, disabled, or returns
     * normally, then {@code eject} throws. Under no conditions does
     * {@code eject} return normally.
     */
    function eject(opt_ejector, result) {
      if (opt_ejector) {
        opt_ejector(result);
        throw new Error('Ejector did not exit: ', opt_ejector);
      } else {
        throw new Error(result);
      }
    }

    ////////////////////////////////////////////////////////////////////////
    // Sealing and Unsealing
    ////////////////////////////////////////////////////////////////////////

    function makeSealerUnsealerPair() {
      var boxValues = new WeakMap(true); // use key lifetime

      function seal(value) {
        var box = freeze({});
        boxValues.set(box, value);
        return box;
      }
      function optUnseal(box) {
        return boxValues.has(box) ? [boxValues.get(box)] : null;
      }
      function unseal(box) {
        var result = optUnseal(box);
        if (result === null) {
          throw new Error("That wasn't one of my sealed boxes!");
        } else {
          return result[0];
        }
      }
      return freeze({
        seal: constFunc(seal),
        unseal: constFunc(unseal),
        optUnseal: constFunc(optUnseal)
      });
    }


    ////////////////////////////////////////////////////////////////////////
    // Trademarks
    ////////////////////////////////////////////////////////////////////////

    var stampers = new WeakMap(true);

    /**
     * Internal routine for making a trademark from a table.
     *
     * <p>To untangle a cycle, the guard made by {@code makeTrademark}
     * is not yet stamped. The caller of {@code makeTrademark} must
     * stamp it before allowing the guard to escape.
     *
     * <p>Note that {@code makeTrademark} is called during the call to
     * {@code ejectorsGuardsTrademarks}, and so must not call {@code
     * cajaVM.def}.
     */
    function makeTrademark(typename, table) {
      typename = '' + typename;

      var stamp = freeze({
        toString: constFunc(function() { return typename + 'Stamp'; })
      });

      stampers.set(stamp, function(obj) {
        table.set(obj, true);
        return obj;
      });

      return freeze({
        toString: constFunc(function() { return typename + 'Mark'; }),
        stamp: stamp,
        guard: freeze({
          toString: constFunc(function() { return typename + 'T'; }),
          coerce: constFunc(function(specimen, opt_ejector) {
            if (!table.get(specimen)) {
              eject(opt_ejector,
                    'Specimen does not have the "' + typename + '" trademark');
            }
            return specimen;
          })
        })
      });
    }

    /**
     * Objects representing guards should be marked as such, so that
     * they will pass the {@code GuardT} guard.
     * <p>
     * {@code GuardT} is generally accessible as
     * {@code cajaVM.GuardT}. However, {@code GuardStamp} must not be
     * made generally accessible, but rather only given to code trusted
     * to use it to deem as guards things that act in a guard-like
     * manner: A guard MUST be immutable and SHOULD be idempotent. By
     * "idempotent", we mean that<pre>
     *     var x = g(specimen, ej); // may fail
     *     // if we're still here, then without further failure
     *     g(x) === x
     * </pre>
     */
    var GuardMark = makeTrademark('Guard', new WeakMap());
    var GuardT = GuardMark.guard;
    var GuardStamp = GuardMark.stamp;
    stampers.get(GuardStamp)(GuardT);

    /**
     * The {@code Trademark} constructor makes a trademark, which is a
     * guard/stamp pair, where the stamp marks and freezes unfrozen
     * records as carrying that trademark and the corresponding guard
     * cerifies objects as carrying that trademark (and therefore as
     * having been marked by that stamp).
     *
     * <p>By convention, a guard representing the type-like concept
     * 'Foo' is named 'FooT'. The corresponding stamp is
     * 'FooStamp'. And the record holding both is 'FooMark'. Many
     * guards also have {@code of} methods for making guards like
     * themselves but parameterized by further constraints, which are
     * usually other guards. For example, {@code T.ListT} is the guard
     * representing frozen array, whereas {@code
     * T.ListT.of(cajaVM.GuardT)} represents frozen arrays of guards.
     */
    function Trademark(typename) {
      var result = makeTrademark(typename, new WeakMap());
      stampers.get(GuardStamp)(result.guard);
      return result;
    };

    /**
     * Given that {@code stamps} is a list of stamps and
     * {@code record} is a non-frozen object, this marks record with
     * the trademarks of all of these stamps, and then freezes and
     * returns the record.
     * <p>
     * If any of these conditions do not hold, this throws.
     */
    function stamp(stamps, record) {
      // TODO: Should nonextensible objects be stampable?
      if (Object.isFrozen(record)) {
        throw new TypeError("Can't stamp frozen objects: " + record);
      }
      stamps = Array.prototype.slice.call(stamps, 0);
      var numStamps = stamps.length;
      // First ensure that we will succeed before applying any stamps to
      // the record.
      var i;
      for (i = 0; i < numStamps; i++) {
        if (!stampers.has(stamps[i])) {
          throw new TypeError("Can't stamp with a non-stamp: " + stamps[i]);
        }
      }
      for (i = 0; i < numStamps; i++) {
        // Only works for real stamps, postponing the need for a
        // user-implementable auditing protocol.
        stampers.get(stamps[i])(record);
      }
      return freeze(record);
    };

    ////////////////////////////////////////////////////////////////////////
    // Guards
    ////////////////////////////////////////////////////////////////////////

    /**
     * First ensures that g is a guard; then does
     * {@code g.coerce(specimen, opt_ejector)}.
     */
    function guard(g, specimen, opt_ejector) {
      g = GuardT.coerce(g); // failure throws rather than ejects
      return g.coerce(specimen, opt_ejector);
    }

    /**
     * First ensures that g is a guard; then checks whether the specimen
     * passes that guard.
     * <p>
     * If g is a coercing guard, this only checks that g coerces the
     * specimen to something rather than failing. Note that trademark
     * guards are non-coercing, so if specimen passes a trademark guard,
     * then specimen itself has been marked with that trademark.
     */
    function passesGuard(g, specimen) {
      g = GuardT.coerce(g); // failure throws rather than ejects
      return callWithEjector(
        constFunc(function(opt_ejector) {
          g.coerce(specimen, opt_ejector);
          return true;
        }),
        constFunc(function(ignored) {
          return false;
        })
      );
    }


    /**
     * Create a guard which passes all objects present in {@code table}.
     * This may be used to define trademark-like systems which do not require
     * the object to be frozen.
     *
     * {@code typename} is used for toString and {@code errorMessage} is used
     * when an object does not pass the guard.
     */
    function makeTableGuard(table, typename, errorMessage) {
      var g = {
        toString: constFunc(function() { return typename + 'T'; }),
        coerce: constFunc(function(specimen, opt_ejector) {
          if (Object(specimen) === specimen && table.get(specimen)) {
            return specimen;
          }
          eject(opt_ejector, errorMessage);
        })
      };
      stamp([GuardStamp], g);
      return freeze(g);
    }

    ////////////////////////////////////////////////////////////////////////
    // Exporting
    ////////////////////////////////////////////////////////////////////////

    return freeze({
      callWithEjector: constFunc(callWithEjector),
      eject: constFunc(eject),
      makeSealerUnsealerPair: constFunc(makeSealerUnsealerPair),
      GuardT: GuardT,
      makeTableGuard: constFunc(makeTableGuard),
      Trademark: constFunc(Trademark),
      guard: constFunc(guard),
      passesGuard: constFunc(passesGuard),
      stamp: constFunc(stamp)
    });
  };
})();
