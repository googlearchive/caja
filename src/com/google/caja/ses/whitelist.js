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
 * @fileoverview Exports {@code ses.whitelist}, a recursively defined
 * JSON record enumerating all the naming paths in the ES5.1 spec,
 * those de-facto extensions that we judge to be safe, and SES and
 * Dr. SES extensions provided by the SES runtime.
 *
 * <p>Assumes only ES3. Compatible with ES5, ES5-strict, or
 * anticipated ES6.
 *
 * //provides ses.whitelist
 * @author Mark S. Miller,
 * @overrides ses, whitelistModule
 */
var ses;

/**
 * <p>Each JSON record enumerates the disposition of the properties on
 * some corresponding primordial object, with the root record
 * representing the global object. For each such record, the values
 * associated with its property names can be
 * <ul>
 * <li>Another record, in which case this property is simply
 *     whitelisted and that next record represents the disposition of
 *     the object which is its value. For example, {@code "Object"}
 *     leads to another record explaining what properties {@code
 *     "Object"} may have and how each such property, if present,
 *     and its value should be tamed.
 * <li>true, in which case this property is simply whitelisted. The
 *     value associated with that property is still traversed and
 *     tamed, but only according to the taming of the objects that
 *     object inherits from. For example, {@code "Object.freeze"} leads
 *     to true, meaning that the {@code "freeze"} property of {@code
 *     Object} should be whitelisted and the value of the property (a
 *     function) should be further tamed only according to the
 *     markings of the other objects it inherits from, like {@code
 *     "Function.prototype"} and {@code "Object.prototype").
 *     If the property is an accessor property, it is not
 *     whitelisted (as invoking an accessor might not be meaningful,
 *     yet the accessor might return a value needing taming).
 * <li>"maybeAccessor", in which case this accessor property is simply
 *     whitelisted and its getter and/or setter are tamed according to
 *     inheritance. If the property is not an accessor property, its
 *     value is tamed according to inheritance.
 * <li>"*", in which case this property on this object is whitelisted,
 *     as is this property as inherited by all objects that inherit
 *     from this object. The values associated with all such properties
 *     are still traversed and tamed, but only according to the taming
 *     of the objects that object inherits from. For example, {@code
 *     "Object.prototype.constructor"} leads to "*", meaning that we
 *     whitelist the {@code "constructor"} property on {@code
 *     Object.prototype} and on every object that inherits from {@code
 *     Object.prototype} that does not have a conflicting mark. Each
 *     of these is tamed as if with true, so that the value of the
 *     property is further tamed according to what other objects it
 *     inherits from.
 * </ul>
 *
 * The members of the whitelist are either
 * <ul>
 * <li>(uncommented) defined by the ES5.1 normative standard text,
 * <li>(questionable) provides a source of non-determinism, in
 *     violation of pure object-capability rules, but allowed anyway
 *     since we've given up on restricting JavaScript to a
 *     deterministic subset.
 * <li>(ES5 Appendix B) common elements of de facto JavaScript
 *     described by the non-normative Appendix B.
 * <li>(Harmless whatwg) extensions documented at
 *     <a href="http://wiki.whatwg.org/wiki/Web_ECMAScript"
 *     >http://wiki.whatwg.org/wiki/Web_ECMAScript</a> that seem to be
 *     harmless. Note that the RegExp constructor extensions on that
 *     page are <b>not harmless</b> and so must not be whitelisted.
 * <li>(ES-Harmony proposal) accepted as "proposal" status for
 *     EcmaScript-Harmony.
 * </ul>
 *
 * <p>With the above encoding, there are some sensible whitelists we
 * cannot express, such as marking a property both with "*" and a JSON
 * record. This is an expedient decision based only on not having
 * encountered such a need. Should we need this extra expressiveness,
 * we'll need to refactor to enable a different encoding.
 *
 * <p>We factor out {@code true} into the variable {@code t} just to
 * get a bit better compression from simple minifiers.
 */
(function whitelistModule() {
  "use strict";

  if (!ses) { ses = {}; }

  var t = true;
  var TypedArrayWhitelist;  // defined and used below
  ses.whitelist = {
    cajaVM: {                        // Caja support
      // This object is present here only to make it itself processed by the
      // whitelist, not to make it accessible by this path.
      '[[ThrowTypeError]]': t,

      log: t,
      tamperProof: t,
      constFunc: t,
      Nat: t,
      def: t,
      is: t,

      compileExpr: t,
      confine: t,
      compileModule: t,              // experimental
      compileProgram: t,             // Cannot be implemented in just ES5.1.
      eval: t,
      Function: t,

      sharedImports: t,
      makeImports: t,
      copyToImports: t,

      GuardT: {
        coerce: t
      },
      makeTableGuard: t,
      Trademark: {
        stamp: t
      },
      guard: t,
      passesGuard: t,
      stamp: t,
      makeSealerUnsealerPair: t,

      makeArrayLike: {
        canBeFullyLive: t
      }
    },
    WeakMap: {       // ES-Harmony proposal as currently implemented by FF6.0a1
      prototype: {
        // Note: coordinate this list with maintenance of repairES5.js
        get: t,
        set: t,
        has: t,
        'delete': t
      }
    },
    StringMap: {  // A specialized approximation of ES-Harmony's Map.
      prototype: {} // Technically, the methods should be on the prototype,
                    // but doing so while preserving encapsulation will be
                    // needlessly expensive for current usage.
    },
// As of this writing, the WeakMap emulation in WeakMap.js relies on
// the unguessability and undiscoverability of HIDDEN_NAME, a
// secret property name. However, on a platform with built-in
// Proxies, if whitelisted but not properly monkey patched, proxies
// could be used to trap and thereby discover HIDDEN_NAME. So until we
// (TODO(erights)) write the needed monkey patching of proxies, we
// omit them from our whitelist.
//
// We now have an additional reason to omit Proxy from the whitelist.
// The makeBrandTester in repairES5 uses Allen's trick at
// https://esdiscuss.org/topic/tostringtag-spoofing-for-null-and-undefined#content-59
// , but testing reveals that, on FF 35.0.1, a proxy on an exotic
// object X will pass this brand test when X will. This is fixed as of
// FF Nightly 38.0a1.
//
//    Proxy: {                         // ES-Harmony proposal
//      create: t,
//      createFunction: t
//    },
    escape: t,                       // ES5 Appendix B
    unescape: t,                     // ES5 Appendix B
    Object: {
      // If any new methods are added here that may reveal the
      // HIDDEN_NAME within WeakMap.js, such as the proposed
      // getOwnPropertyDescriptors or getPropertyDescriptors, then
      // extend WeakMap.js to monkey patch these to avoid revealing
      // HIDDEN_NAME.
      getPropertyDescriptor: t,      // ES-Harmony proposal
      getPropertyNames: t,           // ES-Harmony proposal
      is: t,                         // ES-Harmony proposal
      prototype: {

        // Whitelisted only to work around a Chrome debugger
        // stratification bug (TODO(erights): report). These are
        // redefined in startSES.js in terms of standard methods, so
        // that we can be confident they introduce no non-standard
        // possibilities.
        __defineGetter__: t,
        __defineSetter__: t,
        __lookupGetter__: t,
        __lookupSetter__: t,

        constructor: '*',
        toString: '*',
        toLocaleString: '*',
        valueOf: t,
        hasOwnProperty: t,
        isPrototypeOf: t,
        propertyIsEnumerable: t
      },
      getPrototypeOf: t,
      getOwnPropertyDescriptor: t,
      getOwnPropertyNames: t,
      create: t,
      defineProperty: t,
      defineProperties: t,
      seal: t,
      freeze: t,
      preventExtensions: t,
      isSealed: t,
      isFrozen: t,
      isExtensible: t,
      keys: t
    },
    NaN: t,
    Infinity: t,
    undefined: t,
    // eval: t,                      // Whitelisting under separate control
                                     // by TAME_GLOBAL_EVAL in startSES.js
    parseInt: t,
    parseFloat: t,
    isNaN: t,
    isFinite: t,
    decodeURI: t,
    decodeURIComponent: t,
    encodeURI: t,
    encodeURIComponent: t,
    Function: {
      prototype: {
        apply: t,
        call: t,
        bind: t,
        prototype: '*',
        length: '*',
        arity: '*',                  // non-std, deprecated in favor of length
        name: '*'                    // non-std
      }
    },
    Array: {
      prototype: {
        concat: t,
        join: t,
        pop: t,
        push: t,
        reverse: t,
        shift: t,
        slice: t,
        sort: t,
        splice: t,
        unshift: t,
        indexOf: t,
        lastIndexOf: t,
        every: t,
        some: t,
        forEach: t,
        map: t,
        filter: t,
        reduce: t,
        reduceRight: t,
        length: t
      },
      isArray: t
    },
    String: {
      prototype: {
        substr: t,                   // ES5 Appendix B
        anchor: t,                   // Harmless whatwg
        big: t,                      // Harmless whatwg
        blink: t,                    // Harmless whatwg
        bold: t,                     // Harmless whatwg
        fixed: t,                    // Harmless whatwg
        fontcolor: t,                // Harmless whatwg
        fontsize: t,                 // Harmless whatwg
        italics: t,                  // Harmless whatwg
        link: t,                     // Harmless whatwg
        small: t,                    // Harmless whatwg
        strike: t,                   // Harmless whatwg
        sub: t,                      // Harmless whatwg
        sup: t,                      // Harmless whatwg
        trimLeft: t,                 // non-standard
        trimRight: t,                // non-standard
        valueOf: t,
        charAt: t,
        charCodeAt: t,
        concat: t,
        indexOf: t,
        lastIndexOf: t,
        localeCompare: t,
        match: t,
        replace: t,
        search: t,
        slice: t,
        split: t,
        substring: t,
        toLowerCase: t,
        toLocaleLowerCase: t,
        toUpperCase: t,
        toLocaleUpperCase: t,
        trim: t,
        length: '*'
      },
      fromCharCode: t
    },
    Boolean: {
      prototype: {
        valueOf: t
      }
    },
    Number: {
      prototype: {
        valueOf: t,
        toFixed: t,
        toExponential: t,
        toPrecision: t
      },
      MAX_VALUE: t,
      MIN_VALUE: t,
      NaN: t,
      NEGATIVE_INFINITY: t,
      POSITIVE_INFINITY: t
    },
    Math: {
      E: t,
      LN10: t,
      LN2: t,
      LOG2E: t,
      LOG10E: t,
      PI: t,
      SQRT1_2: t,
      SQRT2: t,

      abs: t,
      acos: t,
      asin: t,
      atan: t,
      atan2: t,
      ceil: t,
      cos: t,
      exp: t,
      floor: t,
      log: t,
      max: t,
      min: t,
      pow: t,
      random: t,                     // questionable
      round: t,
      sin: t,
      sqrt: t,
      tan: t
    },
    Date: {                          // no-arg Date constructor is questionable
      prototype: {
        // Note: coordinate this list with maintanence of repairES5.js
        getYear: t,                  // ES5 Appendix B
        setYear: t,                  // ES5 Appendix B
        toGMTString: t,              // ES5 Appendix B
        toDateString: t,
        toTimeString: t,
        toLocaleString: t,
        toLocaleDateString: t,
        toLocaleTimeString: t,
        valueOf: t,
        getTime: t,
        getFullYear: t,
        getUTCFullYear: t,
        getMonth: t,
        getUTCMonth: t,
        getDate: t,
        getUTCDate: t,
        getDay: t,
        getUTCDay: t,
        getHours: t,
        getUTCHours: t,
        getMinutes: t,
        getUTCMinutes: t,
        getSeconds: t,
        getUTCSeconds: t,
        getMilliseconds: t,
        getUTCMilliseconds: t,
        getTimezoneOffset: t,
        setTime: t,
        setFullYear: t,
        setUTCFullYear: t,
        setMonth: t,
        setUTCMonth: t,
        setDate: t,
        setUTCDate: t,
        setHours: t,
        setUTCHours: t,
        setMinutes: t,
        setUTCMinutes: t,
        setSeconds: t,
        setUTCSeconds: t,
        setMilliseconds: t,
        setUTCMilliseconds: t,
        toUTCString: t,
        toISOString: t,
        toJSON: t
      },
      parse: t,
      UTC: t,
      now: t                         // questionable
    },
    RegExp: {
      prototype: {
        exec: t,
        test: t,
        source: '*',
        global: '*',
        ignoreCase: '*',
        multiline: '*',
        lastIndex: '*',
        options: '*',                // non-std
        sticky: '*'                  // non-std
      }
    },
    Error: {
      prototype: {
        name: '*',
        message: '*'
      }
    },
    EvalError: {
      prototype: t
    },
    RangeError: {
      prototype: t
    },
    ReferenceError: {
      prototype: t
    },
    SyntaxError: {
      prototype: t
    },
    TypeError: {
      prototype: t
    },
    URIError: {
      prototype: t
    },
    JSON: {
      parse: t,
      stringify: t
    },
    ArrayBuffer: {                   // Khronos Typed Arrays spec; ops are safe
      length: t,  // does not inherit from Function.prototype on Chrome
      name: t,  // ditto
      isView: t,
      prototype: {
        byteLength: 'maybeAccessor',
        slice: t
      }
    },
    Int8Array: TypedArrayWhitelist = {  // Typed Arrays spec
      length: t,  // does not inherit from Function.prototype on Chrome
      name: t,  // ditto
      BYTES_PER_ELEMENT: t,
      prototype: {
        buffer: 'maybeAccessor',
        byteOffset: 'maybeAccessor',
        byteLength: 'maybeAccessor',
        length: 'maybeAccessor',
        BYTES_PER_ELEMENT: t,
        set: t,
        subarray: t
      }
    },
    Uint8Array: TypedArrayWhitelist,
    Uint8ClampedArray: TypedArrayWhitelist,
    Int16Array: TypedArrayWhitelist,
    Uint16Array: TypedArrayWhitelist,
    Int32Array: TypedArrayWhitelist,
    Uint32Array: TypedArrayWhitelist,
    Float32Array: TypedArrayWhitelist,
    Float64Array: TypedArrayWhitelist,
    DataView: {                      // Typed Arrays spec
      length: t,  // does not inherit from Function.prototype on Chrome
      name: t,  // ditto
      prototype: {
        buffer: 'maybeAccessor',
        byteOffset: 'maybeAccessor',
        byteLength: 'maybeAccessor',
        getInt8: t,
        getUint8: t,
        getInt16: t,
        getUint16: t,
        getInt32: t,
        getUint32: t,
        getFloat32: t,
        getFloat64: t,
        setInt8: t,
        setUint8: t,
        setInt16: t,
        setUint16: t,
        setInt32: t,
        setUint32: t,
        setFloat32: t,
        setFloat64: t
      }
    }
  };
})();
