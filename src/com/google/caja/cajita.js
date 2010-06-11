// Copyright (C) 2007 Google Inc.
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
 * @fileoverview the Cajita runtime library.
 * It is written in Javascript, not Cajita, and would be rejected by the Cajita
 * translator. This module exports two globals:<ol>
 * <li>"___" for use by the output of the Cajita translator and by some
 *     other untranslated Javascript code.
 * <li>"cajita" providing some common services to the Cajita programmer.
 * </ol>
 *
 * @author erights@gmail.com
 * @requires this, json_sans_eval
 * @provides ___, cajita, safeJSON 
 * @overrides Array, Boolean, Date, Function, Number, Object, RegExp, String
 * @overrides Error, EvalError, RangeError, ReferenceError, SyntaxError,
 *   TypeError, URIError
 * @overrides escape, JSON
 */

// TODO(erights): All code text in comments should be enclosed in {@code ...}.

// TODO(ihab.awad): Missing tests in CajitaTest.java for the functionality
// in this file.

// FIXME(erights): After the Caja runtime was started, some of Caja's
// ideas for controlling ES3 attribute helped inspire the new ES5
// attribute control APIs. But in the process, different terms were
// adopted for these attributes. The current correspondences:
// ES3          Caja         ES5
// ReadOnly     canSet       writable
// DontEnum     canEnum      enumerable
// DontDelete   canDelete    configurable
// We should consider migrating our terminology towards ES5's,
// especially changing "set" to "write".


// Add a tag to whitelisted builtin constructors so we can check the class
// cross-frame. Note that Function is not on the list.

Array.typeTag___ = 'Array';
Object.typeTag___ = 'Object';
String.typeTag___ = 'String';
Boolean.typeTag___ = 'Boolean';
Number.typeTag___ = 'Number';
Date.typeTag___ = 'Date';
RegExp.typeTag___ = 'RegExp';
Error.typeTag___ = 'Error';
EvalError.typeTag___ = 'EvalError';
RangeError.typeTag___ = 'RangeError';
ReferenceError.typeTag___ = 'ReferenceError';
SyntaxError.typeTag___ = 'SyntaxError';
TypeError.typeTag___ = 'TypeError';
URIError.typeTag___ = 'URIError';

Object.prototype.proto___ = null;

////////////////////////////////////////////////////////////////////////
// Cajita adds the following common Javascript extensions to ES3
// TODO(erights): Move such extensions to a separate extensions.js.
////////////////////////////////////////////////////////////////////////

/** In anticipation of ES5 */
if (Date.prototype.toISOString === void 0 && 
    typeof Date.prototype.toJSON === 'function') {
  // These are separate functions in ES5. As separate functions, they
  // also tame separately.
  // TODO(erights): Really, to follow ES5, Date.prototype.toISOString
  // should be non-generic and Date.prototype.toJSON should be generic
  // and defined by calling this.toISOString().
  Date.prototype.toISOString = function() {
    return Date.prototype.toJSON.call(this);
  };
}

/**
 * Provide Array.slice similar to Firefox.  You can slice anything, and the
 * result is always an array.  Hazards:
 *
 *  - In IE, Array.prototype.slice.call(void 0) throws an error, and we
 *    need it to return [].
 *
 *  - In IE[678] and Firefox 3, x.slice(0, undefined) returns [] rather
 *    than x.  We don't care about that incompatibility, but we do need
 *    Array.slice(x) to return x.
 *
 *  - In Firefox 3.x, Array.slice works on any array-like x.  We only
 *    handle typeof x === 'object'.
 */
if (Array.slice === void 0) {
  Array.slice = function(self, opt_start, opt_end) {
    if (self && typeof self === 'object') {
      if (opt_end === void 0) { opt_end = self.length; }
      return Array.prototype.slice.call(self, opt_start, opt_end);
    } else {
      return [];
    }
  };
}


/**
 * In anticipation of ES5.
 * <p>
 * Bind this function to <tt>self</tt>, which will serve
 * as the value of <tt>this</tt> during invocation. Curry on a
 * partial set of arguments in <tt>var_args</tt>. Return the curried
 * result as a new function object.
 * <p>
 * Note: Like the built-in Function.prototype.call and
 * Function.prototype.apply, this one is not whitelisted. Instead,
 * it is provided as it would be in future JavaScripts without
 * special knowledge of Caja. This allows Caja code using bind() to
 * work today uncajoled as well. It also suppresses the overriding
 * of 'bind' by the 'in' test in setStatic().
 * <p>
 * Note that this is distinct from the tamed form of bind() made
 * available to Cajita code.
 */
if (Function.prototype.bind === void 0) {
  Function.prototype.bind = function(self, var_args) {
    var thisFunc = this;
    var leftArgs = Array.slice(arguments, 1);
    function funcBound(var_args) {
      var args = leftArgs.concat(Array.slice(arguments, 0));
      return thisFunc.apply(self, args);
    }
    return funcBound;
  };
}

// The following may or may not exist in the browser-supplied
// global scope; declare as a 'var' to avoid errors when we
// check for its existence later.
var escape;

// cajita.js exports the following names to the Javascript global
// namespace. Cajita code can only use the "cajita" object. The "___"
// object is for use by code generated by the Cajita translator, and by
// Javascript code (such as a powerbox) in the embedding application.

var cajita;
var ___;
var safeJSON;

// Explicitly passing in the actual global object to avoid
// ReferenceErrors when referring to potentially nonexistent objects
// like HTMLDivElement.

(function(global) {

  function ToInt32(alleged_int) {
    return alleged_int >> 0;
  }

  function ToUInt32(alleged_int) {
    return alleged_int >>> 0;
  }

  /**
   * Returns the first index at which the specimen is found (by
   * "identical()") or -1 if none, starting at offset i, if present.
   * If i < 0, the offset is relative to the end of the array.
   */
  function arrayIndexOf(specimen, i) {
    var len = ToUInt32(this.length);
    i = ToInt32(i);
    if (i < 0) {
      if ((i += len) < 0) {
        i = 0;
      }
    }
    for (; i < len; ++i) {
      if (i in this && identical(this[i], specimen)) {
        return i;
      }
    }
    return -1;
  }
  Array.prototype.indexOf = arrayIndexOf;

  /**
   * Returns the last index at which the specimen is found (by
   * "identical()") or -1 if none, starting at offset i, if present.
   * If i < 0, the offset is relative to the end of the array.
   */
  function arrayLastIndexOf(specimen, i) {
    var len = ToUInt32(this.length);

    if (isNaN(i)) {
      i = len - 1;
    } else {
      i = ToInt32(i);
      if (i < 0) {
        i += len;
        if (i < 0) {
          return -1;
        }
      } else if (i >= len) {
        i = len - 1;
      }
    }

    for (; i >= 0 ; --i) {
      if (i in this && identical(this[i], specimen)) {
        return i;
      }
    }
    return -1;
  }
  Array.prototype.lastIndexOf = arrayLastIndexOf;

  ////////////////////////////////////////////////////////////////////////
  // Some regular expressions checking for specific suffixes.
  ////////////////////////////////////////////////////////////////////////

  var endsWith_canDelete___ = /_canDelete___$/;
  var endsWith_canRead___ = /_canRead___$/;
  var endsWith_canSet___ = /_canSet___$/;
  var endsWith___ = /___$/;
  var endsWith__ = /__$/;

  ////////////////////////////////////////////////////////////////////////
  // Some very basic primordial methods
  ////////////////////////////////////////////////////////////////////////

  /**
   * A reliable typeof for use by Cajita code, and by uncajoled code
   * (like parts of cajita.js) that require a reliable typeof.
   * <p>
   * Cajita specifies that <tt>typeof new RegExp("x")</tt>
   * evaluate to <tt>'object'</tt>. Unfortunately, on some of Cajita's
   * current target platforms (including at least Safari 3 and Rhino),
   * it returns <tt>'function'</tt> instead. Since the distinction
   * between functions and non-functions is crucial to Cajita, we
   * translate the Cajita <tt>typeof</tt> operator into calls to this
   * <tt>typeOf</tt> function.
   */
  function typeOf(obj) {
    var result = typeof obj;
    if (result !== 'function') { return result; }
    var ctor = obj.constructor;
    if (typeof ctor === 'function' &&
        ctor.typeTag___ === 'RegExp' &&
        obj instanceof ctor) {
      return 'object';
    }
    return 'function';
  }

  if (typeof new RegExp('x') === 'object') {
    // Since typeof does what we want on FF3, IE6, Chrome, and Opera,
    // on these platforms we shouldn't pay for the more expensive
    // typeOf. Note that we declare the safe one but conditionally
    // reset it to the fast one. This is safe even if hoisting causes
    // the declared function to be called before we can replace it.
    typeOf = function fastTypeof(obj) {
      return typeof obj;
    };
  }

  var myOriginalHOP = Object.prototype.hasOwnProperty;
  var myOriginalToString = Object.prototype.toString;

  /**
   * <tt>hasOwnProp(obj, name)</tt> means what
   * <tt>obj.hasOwnProperty(name)</tt> would normally mean in an
   * unmodified Javascript system.
   */
  function hasOwnProp(obj, name) {
    if (!obj) { return false; }
    var t = typeof obj;
    if (t !== 'object' && t !== 'function') {
      // If obj is a primitive, Object(obj) still has no own properties.
      return false;
    }
    return myOriginalHOP.call(obj, name);
  }

  /**
   * Are x and y not observably distinguishable?
   */
  function identical(x, y) {
    if (x === y) {
      // 0 === -0, but they are not identical
      return x !== 0 || 1/x === 1/y;
    } else {
      // NaN !== NaN, but they are identical.
      // NaNs are the only non-reflexive value, i.e., if x !== x,
      // then x is a NaN.
      return x !== x && y !== y;
    }
  }

  /**
   * Inherited by non-frozen simple functions, which freezes them and
   * installs an overriding fastpath CALL___ to themselves.
   */
  function callFault(var_args) {
    return asFunc(this).apply(USELESS, arguments);
  }
  Object.prototype.CALL___ = callFault;

  ////////////////////////////////////////////////////////////////////////
  // Diagnostics and condition enforcement
  ////////////////////////////////////////////////////////////////////////

  /**
   * The initial default logging function does nothing.
   * <p>
   * Note: JavaScript has no macros, so even in the "does nothing"
   * case, remember that the arguments are still evaluated.
   */
  function defaultLogger(str, opt_stop) {}
  var myLogFunc = markFuncFreeze(defaultLogger);

  /**
   * Gets the currently registered logging function.
   */
  function getLogFunc() { return myLogFunc; }

  /**
   * Register newLogFunc as the current logging function, to be called
   * by <tt>___.log(str)</tt> and <tt>___.fail(...)</tt>.
   * <p>
   * A logging function is assumed to have the signature
   * <tt>(str, opt_stop)</tt>, where<ul>
   * <li><tt>str</tt> is the diagnostic string to be logged, and
   * <li><tt>opt_stop</tt>, if present and <tt>true</tt>, indicates
   *     that normal flow control is about to be terminated by a
   *     throw. This provides the logging function the opportunity to
   *     terminate normal control flow in its own way, such as by
   *     invoking an undefined method, in order to trigger a Firebug
   *     stacktrace.
   * </ul>
   */
  function setLogFunc(newLogFunc) { myLogFunc = newLogFunc; }

  /**
   * Calls the currently registered logging function.
   */
  function log(str) { myLogFunc(String(str)); }


  /**
   * Throw, and optionally log, an error whose message is the
   * concatenation of the arguments.
   * <p>
   * The arguments are converted to strings (presumably by an
   * implicit call to ".toString()") and appended together to make
   * the message of the Error that's thrown.
   */
  function fail(var_args) {
    var message = Array.slice(arguments, 0).join('');
    myLogFunc(message, true);
    throw new Error(message);
  }

  /**
   * Like an assert that can't be turned off.
   * <p>
   * Either returns true (on success) or throws (on failure). The
   * arguments starting with <tt>var_args</tt> are converted to
   * strings and appended together to make the message of the Error
   * that's thrown.
   * <p>
   * TODO(erights) We may deprecate this in favor of <pre>
   *     test || fail(var_args...)
   * </pre> or <pre>
   *     if (!test) { fail(var_args...); }
   * </pre>
   */
  function enforce(test, var_args) {
    return test || fail.apply(USELESS, Array.slice(arguments, 1));
  }

  /**
   * Enforces <tt>typeOf(specimen) === typename</tt>, in which case
   * specimen is returned.
   * <p>
   * If not, throws an informative TypeError
   * <p>
   * opt_name, if provided, should be a name or description of the
   * specimen used only to generate friendlier error messages.
   */
  function enforceType(specimen, typename, opt_name) {
    if (typeOf(specimen) !== typename) {
      fail('expected ', typename, ' instead of ', typeOf(specimen),
           ': ', (opt_name || specimen));
    }
    return specimen;
  }

  /**
   * Enforces that specimen is a non-negative integer within the range
   * of exactly representable consecutive integers, in which case
   * specimen is returned.
   * <p>
   * "Nat" is short for "Natural number".
   */
  function enforceNat(specimen) {
    enforceType(specimen, 'number');
    if (Math.floor(specimen) !== specimen) {
      fail('Must be integral: ', specimen);
    }
    if (specimen < 0) {
      fail('Must not be negative: ', specimen);
    }
    // Could pre-compute precision limit, but probably not faster
    // enough to be worth it.
    if (Math.floor(specimen - 1) !== specimen - 1) {
      fail('Beyond precision limit: ', specimen);
    }
    if (Math.floor(specimen - 1) >= specimen) {
      fail('Must not be infinite: ', specimen);
    }
    return specimen;
  }

  /**
   * Returns a function that can typically be used in lieu of
   * <tt>func</tt>, but that logs a deprecation warning on first use.
   * <p>
   * Currently for internal use only, though may make this available
   * on <tt>___</tt> or even <tt>cajita</tt> at a later time, after
   * making it safe for such use. Forwards only arguments to
   * <tt>func</tt> and returns results back, without forwarding
   * <tt>this</tt>. If you want to deprecate an exophoric function,
   * deprecate a bind()ing of that function instead.
   */
  function deprecate(func, badName, advice) {
    var warningNeeded = true;
    return function() {
      if (warningNeeded) {
        log('"' + badName + '" is deprecated.\n' + advice);
        warningNeeded = false;
      }
      return func.apply(USELESS, arguments);
    };
  }

  ////////////////////////////////////////////////////////////////////////
  // Privileged fault handlers
  ////////////////////////////////////////////////////////////////////////

  function debugReference(obj) {
    switch (typeOf(obj)) {
      case 'object': {
        if (obj === null) { return '<null>'; }
        var constr = directConstructor(obj);
        return '[' + ((constr && constr.name) || 'Object') + ']';
      }
      default: {
        return '(' + obj + ':' + typeOf(obj) + ')';
      }
    }
  }

  var myKeeper = {

    toString: function toString() { return '<Logging Keeper>'; },

    handleRead: function handleRead(obj, name) {
      //log('Not readable: (' + debugReference(obj) + ').' + name);
      return void 0;
    },

    handleCall: function handleCall(obj, name, args) {
      fail('Not callable: (', debugReference(obj), ').', name);
    },

    handleSet: function handleSet(obj, name, val) {
      fail('Not writable: (', debugReference(obj), ').', name);
    },

    handleDelete: function handleDelete(obj, name) {
      fail('Not deletable: (', debugReference(obj), ').', name);
    }
  };

  Object.prototype.handleRead___ = function handleRead___(name) {
    var handlerName = name + '_getter___';
    if (this[handlerName]) {
      return this[handlerName]();
    }
    return myKeeper.handleRead(this, name);
  };

  Object.prototype.handleCall___ = function handleCall___(name, args) {
    var handlerName = name + '_handler___';
    if (this[handlerName]) {
      return this[handlerName].call(this, args);
    }
    return myKeeper.handleCall(this, name, args);
  };

  Object.prototype.handleSet___ = function handleSet___(name, val) {
    var handlerName = name + '_setter___';
    if (this[handlerName]) {
      return this[handlerName](val);
    }
    return myKeeper.handleSet(this, name, val);
  };

  Object.prototype.handleDelete___ = function handleDelete___(name) {
    var handlerName = name + '_deleter___';
    if (this[handlerName]) {
      return this[handlerName]();
    }
    return myKeeper.handleDelete(this, name);
  };

  ////////////////////////////////////////////////////////////////////////
  // walking prototype chain, checking JSON containers
  ////////////////////////////////////////////////////////////////////////

  /**
   * Returns the 'constructor' property of obj's prototype.
   * <p>
   * SECURITY TODO(erights): Analyze the security implications
   * of exposing this as a property of the cajita object.
   * <p>
   * By "obj's prototype", we mean the prototypical object that obj
   * most directly inherits from, not the value of its 'prototype'
   * property. We memoize the apparent prototype into 'proto___' to
   * speed up future queries.
   * <p>
   * If obj is a function or not an object, return undefined.
   */
  function directConstructor(obj) {
    if (obj === null) { return void 0; }
    if (obj === void 0) { return void 0; }
    if (typeOf(obj) === 'function') {
      // Since functions return undefined,
      // directConstructor() doesn't provide access to the
      // forbidden Function constructor.
      return void 0;
    }
    obj = Object(obj);
    var result;
    if (myOriginalHOP.call(obj, 'proto___')) {
      var proto = obj.proto___;
      // At this point we know that (typeOf(proto) === 'object')
      if (proto === null) { return void 0; }
      result = proto.constructor;
      // rest of: if (!isPrototypical(result))
      if (result.prototype !== proto || typeOf(result) !== 'function') {
        result = directConstructor(proto);
      }

    } else {
      if (!myOriginalHOP.call(obj, 'constructor')) {
        // TODO(erights): Detect whether this is a valid constructor
        // property in the sense that result is a proper answer. If
        // not, at least give a sensible error, which will be hard to
        // phrase.
        result = obj.constructor;
      } else {
        var oldConstr = obj.constructor;
        if (delete obj.constructor) {
          result = obj.constructor;
          obj.constructor = oldConstr;
        } else if (isPrototypical(obj)) {
          // A difficult case. In Safari, and perhaps according to
          // ES3, the prototypical object created for the default
          // value of a function's 'prototype' property has a
          // non-deletable 'constructor' property. If this is what we
          // have, then we assume it inherits directly from
          // Object.prototype, so the result should be Object.
          log('Guessing the directConstructor of : ' + obj);
          result = Object;
        } else {
          return fail('Discovery of direct constructors unsupported when the ',
                      'constructor property is not deletable: ',
                      obj, '.constructor === ', oldConstr, 
                      '(', obj === global, ')');
        }
      }

      if (typeOf(result) !== 'function' || !(obj instanceof result)) {
        fail('Discovery of direct constructors for foreign begotten ',
             'objects not implemented on this platform.\n');
      }
      if (result.prototype.constructor === result) {
        // Memoize, so it'll be faster next time.
        obj.proto___ = result.prototype;
      }
    }
    return result;
  }

  /**
   * The function category of the whitelisted global constructors
   * defined in ES is the string name of the constructor, allowing
   * isInstanceOf() to work cross-frame. Otherwise, the function
   * category of a function is just the function itself.
   */
  function getFuncCategory(fun) {
    enforceType(fun, 'function');
    if (fun.typeTag___) {
      return fun.typeTag___;
    } else {
      return fun;
    }
  }

  /**
   * Is <tt>obj</tt> a direct instance of a function whose category is
   * the same as the category of <tt>ctor</tt>?
   */
  function isDirectInstanceOf(obj, ctor) {
    var constr = directConstructor(obj);
    if (constr === void 0) { return false; }
    return getFuncCategory(constr) === getFuncCategory(ctor);
  }

  /**
   * Is <tt>obj</tt> an instance of a function whose category is
   * the same as the category of <tt>ctor</tt>?
   */
  function isInstanceOf(obj, ctor) {
    if (obj instanceof ctor) { return true; }
    if (isDirectInstanceOf(obj, ctor)) { return true; }
    // BUG TODO(erights): walk prototype chain.
    // In the meantime, this will fail should it encounter a
    // cross-frame instance of a "subclass" of ctor.
    return false;
  }

  /**
   * A Record is an object whose direct constructor is Object. 
   * <p>
   * These are the kinds of objects that can be expressed as
   * an object literal ("<tt>{...}</tt>") in the JSON language.
   */
  function isRecord(obj) {
    if (!obj) { return false; }
    if (obj.RECORD___ === obj) { return true; }
    if (isDirectInstanceOf(obj, Object)) {
      obj.RECORD___ = obj;
      return true;
    }
    return false;
  }

  /**
   * An Array is an object whose direct constructor is Array.
   * <p>
   * These are the kinds of objects that can be expressed as
   * an array literal ("<tt>[...]</tt>") in the JSON language.
   */
  function isArray(obj) {
    return isDirectInstanceOf(obj, Array);
  }

  /**
   * A JSON container is a non-prototypical object whose direct
   * constructor is Object or Array.
   * <p>
   * These are the kinds of non-primitive objects that can be
   * expressed in the JSON language.
   */
  function isJSONContainer(obj) {
    if (!obj) { return false; }
    if (obj.RECORD___ === obj) { return true; }
    var constr = directConstructor(obj);
    if (constr === void 0) { return false; }
    var typeTag = constr.typeTag___;
    if (typeTag !== 'Object' && typeTag !== 'Array') { return false; }
    return !isPrototypical(obj);
  }

  /**
   * If obj is frozen, Cajita code cannot directly assign to
   * own properties of obj, nor directly add or delete own properties to
   * obj.
   * <p>
   * The status of being frozen is not inherited. If A inherits from
   * B (i.e., if A's prototype is B), A and B each may or may not be
   * frozen independently. (Though if B is prototypical, then it must
   * be frozen.)
   * <p>
   * If <tt>typeof obj</tt> is neither 'object' nor 'function', then
   * it's currently considered frozen.
   */
  function isFrozen(obj) {
    if (!obj) { return true; }
    // TODO(erights): Object(<primitive>) wrappers should also be
    // considered frozen.
    if (obj.FROZEN___ === obj) { return true; }
    var t = typeof obj;
    return t !== 'object' && t !== 'function';
  }

  /**
   * Mark obj as frozen so that Cajita code cannot directly assign to its
   * own properties.
   * <p>
   * If obj is a function, also freeze obj.prototype.
   * <p>
   * This appears as <tt>___.primFreeze(obj)</tt> and is wrapped by
   * <tt>cajita.freeze(obj)</tt>, which applies only to JSON containers.
   * It does a shallow freeze, i.e., if record y inherits from record x,
   * ___.primFreeze(y) will not freeze x.
   */
  function primFreeze(obj) {
    // Fail silently on undefined, since
    //   (function(){
    //     var f = Foo;
    //     if (true) { function Foo() {} }
    //   })();
    // gets translated to (roughly)
    //   (function(){
    //     var Foo;
    //     var f = ___.primFreeze(Foo);
    //     if (true) { Foo = function Foo() {}; }
    //   })();
    if (isFrozen(obj)) { return obj; }

    if (obj.SLOWFREEZE___) {
      // Still true even if SLOWFREEZE___ is inherited.

      // badFlags are names of properties we need to turn off.
      // We accumulate these first, so that we're not in the midst of a
      // for/in loop on obj while we're deleting properties from obj.
      var badFlags = [];
      for (var k in obj) {
        if (endsWith_canSet___.test(k) || endsWith_canDelete___.test(k)) {
          if (obj[k]) {
            badFlags.push(k);
          }
        }
      }
      for (var i = 0; i < badFlags.length; i++) {
        var flag = badFlags[i];
        if (myOriginalHOP.call(obj, flag)) {
          if (!(delete obj[flag])) {
            fail('internal: failed delete: ', debugReference(obj), '.', flag);
          }
        }
        if (obj[flag]) {
          obj[flag] = false;
        }
      }
      // Will only delete it as an own property, so may legitimately
      // fail if obj only inherits SLOWFREEZE___.
      delete obj.SLOWFREEZE___;
    }
    obj.FROZEN___ = obj;
    if (typeOf(obj) === 'function') {
      if (isFunc(obj)) {
        grantCall(obj, 'call');
        grantCall(obj, 'apply');
        obj.CALL___ = obj;
      }
      // Do last to avoid possible infinite recursion.
      if (obj.prototype) { primFreeze(obj.prototype); }
    }
    return obj;
  }

  /**
   * Like primFreeze(obj), but applicable only to JSON containers,
   * (pointlessly but harmlessly) to functions, and to Errors.
   * <p>
   * Errors are constructed objects whose only whitelisted
   * properties are <tt>name</tt> and <tt>message</tt>, both of which
   * are strings on at least all A-grade browsers and on all browsers
   * that conform to either the ES3 or ES5 specs. Therefore, we
   * could consider Errors to simply be frozen. We don't only to avoid
   * slowing down <tt>isFrozen()</tt>, which needs to be really
   * fast. Instead, we allow cajoled code to freeze it. We also
   * freeze it in <tt>tameException()</tt>, so all Errors are frozen
   * by the time cajoled code can catch them.
   */
  function freeze(obj) {
    if (isJSONContainer(obj)) {
      return primFreeze(obj);
    }
    if (typeOf(obj) === 'function') {
      enforce(isFrozen(obj), 'Internal: non-frozen function: ' + obj);
      return obj;
    }
    if (isInstanceOf(obj, Error)) {
      return primFreeze(obj);
    }
    fail('cajita.freeze(obj) applies only to JSON Containers, ',
         'functions, and Errors: ',
         debugReference(obj));
  }
  

  /**
   * Makes a mutable copy of a JSON container.
   * <p>
   * Even if the original is frozen, the copy will still be mutable.
   * It does a shallow copy, i.e., if record y inherits from record x,
   * ___.copy(y) will also inherit from x.
   */
  function copy(obj) {
    if (!isJSONContainer(obj)) {
      fail('cajita.copy(obj) applies only to JSON Containers: ',
           debugReference(obj));
    }
    var result = isArray(obj) ? [] : {};
    forOwnKeys(obj, markFuncFreeze(function(k, v) {
      result[k] = v;
    }));
    return result;
  }

  /**
   * A snapshot of a JSON container is a frozen shallow copy of that
   * container.
   */
  function snapshot(obj) {
    return primFreeze(copy(obj));
  }


  ////////////////////////////////////////////////////////////////////////
  // Accessing property attributes
  ////////////////////////////////////////////////////////////////////////

  /**
   * Tests whether the fast-path canRead flag is set.
   */
  function canRead(obj, name)   {
    if (obj === void 0 || obj === null) { return false; }
    return !!obj[name + '_canRead___'];
  }

  /**
   * Tests whether the fast-path canEnum flag is set.
   */
  function canEnum(obj, name)   {
    if (obj === void 0 || obj === null) { return false; }
    return !!obj[name + '_canEnum___'];
  }

  /**
   * Tests whether the fast-path canCall flag is set, or grantCall() has been
   * called.
   */
  function canCall(obj, name)   {
    if (obj === void 0 || obj === null) { return false; }
    if (obj[name + '_canCall___']) { return true; }
    if (obj[name + '_grantCall___']) {
      fastpathCall(obj, name);
      return true;
    }
    return false;
  }
  /**
   * Tests whether the fast-path canSet flag is set, or grantSet() has been
   * called, on this object itself as an own (non-inherited) attribute.
   */
  function canSet(obj, name) {
    if (obj === void 0 || obj === null) { return false; }
    if (obj[name + '_canSet___'] === obj) { return true; }
    if (obj[name + '_grantSet___'] === obj) {
      fastpathSet(obj, name);
      return true;
    }
    return false;
  }

  /**
   * Tests whether the fast-path canDelete flag is set, on this
   * object itself as an own (non-inherited) attribute.
   */
  function canDelete(obj, name) {
    if (obj === void 0 || obj === null) { return false; }
    return obj[name + '_canDelete___'] === obj;
  }

  /**
   * Sets the fast-path canRead flag.
   * <p>
   * These are called internally to memoize decisions arrived at by
   * other means.
   */
  function fastpathRead(obj, name) {
    if (name === 'toString') { fail("internal: Can't fastpath .toString"); }
    obj[name + '_canRead___'] = obj;
  }

  function fastpathEnum(obj, name) {
    obj[name + '_canEnum___'] = obj;
  }

  /**
   * Simple functions should callable and readable, but methods
   * should only be callable.
   */
  function fastpathCall(obj, name) {
    if (name === 'toString') { fail("internal: Can't fastpath .toString"); }
    if (obj[name + '_canSet___']) {
      obj[name + '_canSet___'] = false;
    }
    if (obj[name + '_grantSet___']) {
      obj[name + '_grantSet___'] = false;
    }
    obj[name + '_canCall___'] = obj;
  }

  /**
   * fastpathSet implies fastpathEnum and fastpathRead.
   * <p>
   * fastpathSet also disables the ability to call and records that
   * more work must be done to freeze this object. 
   */
  function fastpathSet(obj, name) {
    if (name === 'toString') { fail("internal: Can't fastpath .toString"); }
    if (isFrozen(obj)) {
      fail("Can't set .", name, ' on frozen (', debugReference(obj), ')');
    }
    if (typeOf(obj) === 'function') {
      fail("Can't make .", name, 
           ' writable on a function (', debugReference(obj), ')');
    }
    fastpathEnum(obj, name);
    fastpathRead(obj, name);
    if (obj[name + '_canCall___']) {
      obj[name + '_canCall___'] = false;
    }
    if (obj[name + '_grantCall___']) {
      obj[name + '_grantCall___'] = false;
    }
    obj.SLOWFREEZE___ = obj;
    obj[name + '_canSet___'] = obj;
  }

  /**
   * fastpathDelete allows delete of a member on a constructed object via
   * the public API.
   * <p>
   * fastpathDelete also records that more work must be done to freeze
   * this object.  
   * <p>
   * TODO(erights): Having a fastpath flag for this probably doesn't
   * make sense.
   */
  function fastpathDelete(obj, name) {
    if (name === 'toString') { fail("internal: Can't fastpath .toString"); }
    if (isFrozen(obj)) {
      fail("Can't delete .", name, ' on frozen (', debugReference(obj), ')');
    }
    if (typeOf(obj) === 'function') {
      fail("Can't make .", name, 
           ' deletable on a function (', debugReference(obj), ')');
    }
    obj.SLOWFREEZE___ = obj;
    obj[name + '_canDelete___'] = obj;
  }

  /**
   * The various <tt>grant*</tt> functions are called externally by
   * Javascript code to express whitelisting taming decisions.
   */
  function grantRead(obj, name) {
    fastpathRead(obj, name);
  }

  function grantEnum(obj, name) {
    fastpathEnum(obj, name);
  }

  function grantCall(obj, name) {
    fastpathCall(obj, name);
    obj[name + '_grantCall___'] = obj;
  }

  function grantSet(obj, name) {
    fastpathSet(obj, name);
    obj[name + '_grantSet___'] = obj;
  }

  function grantDelete(obj, name) {
    fastpathDelete(obj, name);
  }

  ////////////////////////////////////////////////////////////////////////
  // Primitive objective membrane
  ////////////////////////////////////////////////////////////////////////

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
   * Initialization code can express some taming decisions by calling
   * tamesTo to preregister some feral/tame pairs. 
   * <p>
   * Unlike the subjective membranes created by Domita, in this one,
   * the objects in a tame/feral pair point directly at each other,
   * and thereby retain each other. So long as one is non-garbage the
   * other will be as well. 
   */
  function tamesTo(f, t) {
    var ftype = typeof f;
    if (!f || (ftype !== 'function' && ftype !== 'object')) { 
      fail('Unexpected feral primitive: ', f); 
    }
    var ttype = typeof t;
    if (!t || (ttype !== 'function' && ttype !== 'object')) {
      fail('Unexpected tame primitive: ', t); 
    }

    if (f.TAMED_TWIN___ === t && t.FERAL_TWIN___ === f) { 
      // Just a transient diagnostic until we understand how often
      // this happens.
      log('multiply tamed: ' + f + ', ' + t);
      return; 
    }

    // TODO(erights): Given that we maintain the invariant that 
    // (f.TAMED_TWIN___ === t && hasOwnProp(f, 'TAMED_TWIN___')) iff
    // (t.FERAL_TWIN___ === f && hasOwnProp(t, 'FERAL_TWIN___')), then we
    // could decide to more delicately rely on this invariant and test
    // the backpointing rather than hasOwnProp below.

    if (f.TAMED_TWIN___ && hasOwnProp(f, 'TAMED_TWIN___')) { 
      fail('Already tames to something: ', f); 
    }
    if (t.FERAL_TWIN___ && hasOwnProp(t, 'FERAL_TWIN___')) { 
      fail('Already untames to something: ', t); 
    }
    if (f.FERAL_TWIN___ && hasOwnProp(f, 'FERAL_TWIN___')) { 
      fail('Already tame: ', f); 
    }
    if (t.TAMED_TWIN___ && hasOwnProp(t, 'TAMED_TWIN___')) { 
      fail('Already feral: ', t); 
    }

    f.TAMED_TWIN___ = t;
    t.FERAL_TWIN___ = f;
  }

  /**
   * ___.tamesToSelf(obj) marks obj as both tame and feral.
   * <p>
   * Most tamed objects should be both feral and tame, i.e.,
   * safe to be accessed from both the feral and tame worlds.
   * <p>
   * This is equivalent to tamesTo(obj, obj) but a bit faster by
   * exploiting the knowledge that f and t are the same object.
   */
  function tamesToSelf(obj) {
    var otype = typeof obj;
    if (!obj || (otype !== 'function' && otype !== 'object')) { 
      fail('Unexpected primitive: ', obj); 
    }
    if (obj.TAMED_TWIN___ === obj && obj.FERAL_TWIN___ === obj) { 
      // Just a transient diagnostic until we understand how often
      // this happens.
      log('multiply tamed: ' + obj);
      return; 
    }

    // TODO(erights): Given that we maintain the invariant that 
    // (f.TAMED_TWIN___ === t && hasOwnProp(f, 'TAMED_TWIN___')) iff
    // (t.FERAL_TWIN___ === f && hasOwnProp(t, 'FERAL_TWIN___')), then we
    // could decide to more delicately rely on this invariant and test
    // the backpointing rather than hasOwnProp below.
    if (obj.TAMED_TWIN___ && hasOwnProp(obj, 'TAMED_TWIN___')) { 
      fail('Already tames to something: ', obj); 
    }
    if (obj.FERAL_TWIN___ && hasOwnProp(obj, 'FERAL_TWIN___')) { 
      fail('Already untames to something: ', obj); 
    }

    obj.TAMED_TWIN___ = obj.FERAL_TWIN___ = obj;
  }

  /**
   * 
   * Returns a tame object representing f, or undefined on failure.
   * <ol>
   * <li>All primitives tame and untame to themselves. Therefore,
   *     undefined is only a valid failure indication after checking
   *     that the argument is not undefined. 
   * <li>If f has a registered tame twin, return that.
   * <li>If f is marked tame, then f already is a tame object 
   *     representing f, so return f.
   * <li>If f has an AS_TAMED___() method, call it and then register 
   *     the result as f's tame twin. Unlike the tame/feral
   *     registrations, this method applies even if it is inherited.
   * <li>If f is a Record, call tameRecord(f). We break Records out as
   *     a special case since the only thing all Records inherit from
   *     is Object.prototype, which everything else inherits from as
   *     well. 
   * <li>Indicate failure by returning undefined.
   * </ol>
   * Record taming does not (yet?) deal with record inheritance. 
   * <p>
   * The AS_TAMED___() methods may assume that they are called only by
   * tame() and only on unmarked non-primitive objects. They must
   * therefore either return another unmarked non-primitive object
   * (possibly the same one) or undefined for failure. On returning
   * successfully, tame() will register the pair so AS_TAMED___() does
   * not need to. 
   */
  function tame(f) {
    var ftype = typeof f;
    if (!f || (ftype !== 'function' && ftype !== 'object')) { 
      return f; 
    }
    var t = f.TAMED_TWIN___;
    // Here we do use the backpointing test as a cheap hasOwnProp test.
    if (t && t.FERAL_TWIN___ === f) { return t; }

    var realFeral = f.FERAL_TWIN___;
    if (realFeral && realFeral.TAMED_TWIN___ === f) {
      // If f has a feral twin, then f itself is tame.
      log('Tame-only object from feral side: ' + f);
      return f;
    }
    if (f.AS_TAMED___) {
      t = f.AS_TAMED___();
      if (t) { tamesTo(f, t); }
      return t;
    }
    if (isRecord(f)) {
      t = tameRecord(f);
      // tameRecord does not actually have any possibility of failure,
      // but we can't assume that here.
      if (t) { tamesTo(f, t); }
      return t;
    }
    return undefined;
  }

  /**
   * Returns a feral object representing t, or undefined on failure.
   * <ol>
   * <li>All primitives tame and untame to themselves. Therefore,
   *     undefined is only a valid failure indication after checking
   *     that the argument is not undefined. 
   * <li>If t has a registered feral twin, return that.
   * <li>If t is marked feral, then t already is a feral object 
   *     representing t, so return t.
   * <li>If t has an AS_FERAL___() method, call it and then register 
   *     the result as t's feral twin. Unlike the tame/feral
   *     registrations, this method applies even if it is inherited.
   * <li>If t is a Record, call untameRecord(t).
   * <li>Indicate failure by returning undefined.
   * </ol>
   * Record untaming does not (yet?) deal with record inheritance. 
   * <p>
   * The AS_FERAL___() methods may assume that they are called only by
   * untame() and only on unmarked non-primitive objects. They must
   * therefore either return another unmarked non-primitive object
   * (possibly the same one) or undefined for failure. On returning
   * successfully, untame() will register the pair so AS_FERAL___() does
   * not need to. 
   */
  function untame(t) {
    var ttype = typeof t;
    if (!t || (ttype !== 'function' && ttype !== 'object')) { 
      return t; 
    }
    var f = t.FERAL_TWIN___;
    // Here we do use the backpointing test as a cheap hasOwnProp test.
    if (f && f.TAMED_TWIN___ === t) { return f; }

    var realTame = t.TAMED_TWIN___;
    if (realTame && realTame.FERAL_TWIN___ === t) {
      // If t has a tamed twin, then t itself is feral.
      log('Feral-only object from tame side: ' + t);
      return t;
    }
    if (t.AS_FERAL___) {
      f = t.AS_FERAL___();
      if (f) { tamesTo(f, t); }
      return f;
    }
    if (isRecord(t)) {
      f = untameRecord(t);
      // untameRecord does not actually have any possibility of
      // failure, but we can't assume that here.
      if (f) { tamesTo(f, t); }
      return f;
    }
    return undefined;
  }

  ////////////////////////////////////////////////////////////////////////
  // Taming helpers to be called only by tame() and untame().
  ////////////////////////////////////////////////////////////////////////

  global.AS_TAMED___ = function() {
    fail('global object almost leaked');
  };

  global.AS_FERAL___ = function() {
    fail('global object leaked');
  };

  /**
   * Used in lieu of an AS_TAMED___() method for Records.
   * <p>
   * Assume f is an unmarked Record. Recursively tame all its
   * mentionable enumerable own properties, being careful to
   * handle cycles. Failure to tame a property value only causes
   * that property to be omitted. Freeze the resulting record. If
   * the original record were frozen and all properties tame to
   * themselves, then the Record should tame to itself. 
   */
  function tameRecord(f) {
    var t = {};
    var changed = !isFrozen(f);
    // To handle cycles, provisionally mark f as taming to a fresh
    // t going in and see how the rest tames. Set up a try/finally
    // block to remove these provisional markings even on
    // exceptional exit.
    tamesTo(f, t);      
    try {
      var keys = ownKeys(f);
      var len = keys.length;
      for (var i = 0; i < len; i++) {
        var k = keys[i];
        var fv = f[k];
        var tv = tame(fv);
        if (tv === void 0 && fv !== void 0) {
          changed = true;
        } else {
          if (fv !== tv && fv === fv) { // I hate NaNs
            changed = true;
          }
          t[k] = tv;
        }
      }
    } finally {
      delete f.TAMED_TWIN___;
      delete t.FERAL_TWIN___;
    }
    if (changed) {
      // Although the provisional marks have been removed, our caller
      // will restore them. We do it this way to make tameRecord()
      // more similar to AS_TAMED___() methods.
      return primFreeze(t);
    } else {
      return f;
    }
  }
 
  /**
   * Used in lieu of an AS_FERAL___() method for Records.
   * <p>
   * Assume t is an unmarked Record. Recursively untame all its
   * mentionable enumerable own properties, being careful to
   * handle cycles. Failure to untame a property value only causes
   * that property to be omitted. Freeze the resulting record. If
   * the original record were frozen and all properties untame to
   * themselves, then the Record should untame to itself. 
   */
  function untameRecord(t) {
    var f = {};
    var changed = !isFrozen(t);
    // To handle cycles, provisionally mark t as untaming to a fresh
    // f going in and see how the rest untames. Set up a try/finally
    // block to remove these provisional markings even on
    // exceptional exit.
    tamesTo(f, t);      
    try {
      var keys = ownKeys(t);
      var len = keys.length;
      for (var i = 0; i < len; i++) {
        var k = keys[i];
        var tv = t[k];
        var fv = untame(tv);
        if (fv === void 0 && tv !== void 0) {
          changed = true;
        } else {
          if (tv !== fv && tv === tv) { // I hate NaNs
            changed = true;
          }
          f[k] = fv;
        }
      }
    } finally {
      delete t.FERAL_TWIN___;
      delete f.TAMED_TWIN___;
    }
    if (changed) {
      // Although the provisional marks have been removed, our caller
      // will restore them. We do it this way to make untameRecord()
      // more similar to AS_FERAL___() methods.
      return primFreeze(f);
    } else {
      return t;
    }
  }

  /**
   * 
   * Tame an array into a frozen dense array of tamed elements.
   * <p>
   * Assume f is an unmarked array. Recursively tame all its
   * elements (the values of its uint-named properties between 0 and
   * length-1), being careful to handle cycles. Absence of an index or
   * failure to tame a value only causes the taming at that index to
   * be undefined. Freeze the resulting array. If the original array
   * were frozen and all elements are present and tame to themselves,
   * then the array should tame to itself.
   * <p>
   * SECURITY HAZARD: Having the array tame to itself under the above
   * rule isn't safe if the array contains non-index-named properties
   * with non-tame values, as these will become accessible to
   * untrusted cajoled code. However, it is too expensive to check for
   * these here, so it is the responsibility of the trusted uncajoled
   * code never to cause an array containing such properties to be
   * tamed. TODO(erights): Should add a debugging flag to enable
   * expensive safety checks.
   */
  Array.prototype.AS_TAMED___ = function tameArray() {
    var f = this;
    var t = [];
    var changed = !isFrozen(f);
    // To handle cycles, provisionally mark f as taming to a fresh
    // t going in and see how the rest tames. Set up a try/finally
    // block to remove these provisional markings even on
    // exceptional exit.
    tamesTo(f, t);      
    try {
      var len = f.length;
      for (var i = 0; i < len; i++) {
        if (i in f) {
          var fv = f[i];
          var tv = tame(fv);
          if (fv !== tv && fv === fv) { // I hate NaNs
            changed = true;
          }
          t[i] = tv;
        } else {
          changed = true;
          t[i] = void 0;          
        }
      }
    } finally {
      delete f.TAMED_TWIN___;
      delete t.FERAL_TWIN___;
    }
    if (changed) {
      // Although the provisional marks have been removed, our caller
      // will restore them.
      return primFreeze(t);
    } else {
      // See SECURITY HAZARD note in doc-comment.
      return f;
    }
  };

  /**
   * Untame an array into a frozen dense array of feral elements.
   * <p>
   * Assume f is an unmarked array. Recursively untame all its
   * elements (the values of its uint-named properties between 0 and
   * length-1), being careful to handle cycles. Absence of an index or
   * failure to untame a value only causes the untaming at that index to
   * be undefined. Freeze the resulting array. If the original array
   * were frozen and all elements are present and untame to themselves,
   * then the array should untame to itself.
   * <p>
   * SECURITY HAZARD: Having the array untame to itself under the above
   * rule isn't safe if the array contains non-index-named properties
   * with non-feral values, as these might be accessed by
   * trusted uncajoled code. However, it is too expensive to check for
   * these here, so it is the responsibility of the trusted uncajoled
   * code not to <i>innocently</i> access such properties on a feral
   * array resulting from untaming a tame array. TODO(erights): Should
   * add a debugging flag to enable expensive safety checks.
   */
  Array.prototype.AS_FERAL___ = function untameArray() {
    var t = this;
    var f = [];
    var changed = !isFrozen(t);
    // To handle cycles, provisionally mark t as untaming to a fresh
    // f going in and see how the rest untames. Set up a try/finally
    // block to remove these provisional markings even on
    // exceptional exit.
    tamesTo(f, t);      
    try {
      var len = t.length;
      for (var i = 0; i < len; i++) {
        if (i in t) {
          var tv = t[i];
          var fv = untame(tv);
          if (tv !== fv && tv === tv) { // I hate NaNs
            changed = true;
          }
          f[i] = fv;
        } else {
          changed = true;
          f[i] = void 0;
        }
      }
    } finally {
      delete t.FERAL_TWIN___;
      delete f.TAMED_TWIN___;
    }
    if (changed) {
      // Although the provisional marks have been removed, our caller
      // will restore them.
      return primFreeze(f);
    } else {
      // See SECURITY HAZARD note in doc-comment.
      return t;
    }
  };
  
  /**
   * Constructors and simple-functions tame to themselves by default.
   */
  Function.prototype.AS_TAMED___ = function defaultTameFunc() {
    var f = this;
    if (isFunc(f) || isCtor(f)) { return f; }
    return void 0;
  };
  
  /**
   * Constructors and simple-function untame to themselves by default.
   */
  Function.prototype.AS_FERAL___ = function defaultUntameFunc() {
    var t = this;
    if (isFunc(t) || isCtor(t)) { return t; }
    return void 0;    
  };

  /**
   * Prevent privilege escalation by passing USELESS rather than null,
   * undefined or the global object as the <tt>this</tt> of a call to
   * a real <tt>call</tt>, <tt>apply</tt>, or <tt>bind</tt> method.
   */
  function stopEscalation(val) {
    if (val === null || val === void 0 || val === global) {
      return USELESS;
    }
    return val;
  }

  /**
   * To be installed as the AS_TAMED___() method on feral functions
   * marked by markXo4a().
   * <p>
   * A feral function is marked as xo4a iff it may be given a tame
   * <tt>this</tt> and arguments and must return a tame result. We
   * therefore tame it to a frozen pseudo-function whose call and
   * apply methods calls the original function's apply method
   * directly. 
   */
  function tameXo4a() {
    var xo4aFunc = this;
    function tameApplyFuncWrapper(self, opt_args) {
      return xo4aFunc.apply(stopEscalation(self), opt_args || []);
    }
    markFuncFreeze(tameApplyFuncWrapper);

    function tameCallFuncWrapper(self, var_args) {
      return tameApplyFuncWrapper(self, Array.slice(arguments, 1));
    }
    markFuncFreeze(tameCallFuncWrapper);

    var result = PseudoFunction(tameCallFuncWrapper, tameApplyFuncWrapper);
    result.length = xo4aFunc.length;
    result.toString = markFuncFreeze(xo4aFunc.toString.bind(xo4aFunc));
    return primFreeze(result);
  }

  /**
   * To be installed as the AS_TAMED___() method on feral functions
   * marked by markInnocent().
   * <p>
   * An innocent feral function must be assumed to be exophoric, to
   * expect only a feral this and arguments and to return a feral
   * result. We therefore tame it to a frozen pseudo-function whose
   * call and apply methods calls the original feral function with an
   * untaming of its self and remaining arguments. It will then return 
   * a taming of what the feral function returns.
   */
  function tameInnocent() {
    var feralFunc = this;
    function tameApplyFuncWrapper(self, opt_args) {
      var feralThis = stopEscalation(untame(self));
      var feralArgs = untame(opt_args);
      var feralResult = feralFunc.apply(feralThis, feralArgs || []);
      return tame(feralResult);
    }
    markFuncFreeze(tameApplyFuncWrapper);

    function tameCallFuncWrapper(self, var_args) {
      return tameApplyFuncWrapper(self, Array.slice(arguments, 1));
    }
    markFuncFreeze(tameCallFuncWrapper);

    var result = PseudoFunction(tameCallFuncWrapper, tameApplyFuncWrapper);
    result.length = feralFunc.length;
    result.toString = markFuncFreeze(feralFunc.toString.bind(feralFunc));
    return primFreeze(result);
  }

  /**
   * A Record is a pseudo-function if it inherits from
   * some PseudoFunctionProto.
   * <p>
   * At the moment a pseudo-function is returned from
   * PseudoFunction(), it has working call, apply, and bind
   * methods. However, since pseudo-functions may be mutated, these
   * can be altered or deleted. Neverthess, having determined that an
   * object is a pseudo-function, its clients may (and generally will)
   * assume that all these methods are present and working.
   */
  var PseudoFunctionProto = primFreeze({

    /**
     * A simple default that should generally be overridden.
     */
    toString: markFuncFreeze(function() {
      return 'pseudofunction(var_args) {\n    [some code]\n}';
    }),

    /**
     * Since there's no constructor, to have a general cross-frame
     * pseudo-function test, we test CLASS___ ===
     * 'PseudoFunction'. This CLASS___ will also cause a cajoled
     * ({}).toString.call(pseudoFunc) to return 
     * "[object PseudoFunction]". 
     * <p>
     * TODO(erights): In Cajita, it might be better to return 
     * "[object Object]"; it's not clear. In Valija, it would
     * definitely be better to return "[object Function]".
     */
    CLASS___: 'PseudoFunction',

    /**
     * A pseudo-function untames to an exophoric feral function.
     * <p>
     * The resulting feral function will call the pseudo-function's
     * apply method with a taming of the feral this as the tame self
     * and a taming of its arguments. It will then return an untaming
     * of the result. 
     */
    AS_FERAL___: function untamePseudoFunction() {
      var tamePseudoFunc = this;
      function feralWrapper(var_args) {
        var feralArgs = Array.slice(arguments, 0);
        var tamedSelf = tame(stopEscalation(this));
        var tamedArgs = tame(feralArgs);
        var tameResult = callPub(tamePseudoFunc, 
                                 'apply', 
                                 [tamedSelf, tamedArgs]);
        return untame(tameResult);
      }
      return feralWrapper;
    }
  });

  /**
   * Makes an unfrozen pseudo-function that inherits from
   * PseudoFunctionProto. 
   * <p>
   * Both the callFunc and the opt_applyFunc, if provided, must be
   * simple-functions. If the opt_applyFunc is omitted, it is
   * synthesized from the callFunc.
   * <p>
   * PseudoFunction is not a genuine constructor, because
   * pseudo-functions are Records, not constructed objects.
   * <p>
   * PseudoFunction's caller should set the pseudo-function's toString
   * method to something useful, overriding the default inherited from
   * PseudoFunctionProto. 
   */
  function PseudoFunction(callFunc, opt_applyFunc) {
    callFunc = asFunc(callFunc);
    var applyFunc;
    if (opt_applyFunc) {
      applyFunc = asFunc(opt_applyFunc);
    } else {
      applyFunc = markFuncFreeze(function applyFun(self, opt_args) {
        var args = [self];
        if (opt_args !== void 0 && opt_args !== null) {
          args.push.apply(args, opt_args);
        }
        return callFunc.apply(USELESS, args);
      });
    }

    var result = primBeget(PseudoFunctionProto);
    result.call = callFunc;
    result.apply = applyFunc;
    result.bind = markFuncFreeze(function bindFun(self, var_args) {
      self = stopEscalation(self);
      var args = [USELESS, self].concat(Array.slice(arguments, 1));
      return markFuncFreeze(callFunc.bind.apply(callFunc, args));
    });
    result.length = callFunc.length -1;
    return result;
  }

  ////////////////////////////////////////////////////////////////////////
  // Classifying functions and pseudo-functions
  ////////////////////////////////////////////////////////////////////////

  function isCtor(constr)    {
    return constr && !!constr.CONSTRUCTOR___;
  }
  function isFunc(fun) {
    return fun && !!fun.FUNC___;
  }
  function isXo4aFunc(func) {
    return func && !!func.XO4A___;
  }
  function isPseudoFunc(fun) {
    return fun && fun.CLASS___ === 'PseudoFunction';
  }

  /**
   * Mark <tt>constr</tt> as a constructor which tames to itself by 
   * default, and whose instances are constructed objects which tame 
   * and untame to themselves by default. 
   * <p>
   * A function is tamed and classified by calling one of
   * <tt>markCtor()</tt>, <tt>markXo4a()</tt>, or
   * <tt>markFuncFreeze()</tt>. Each of these checks that the function
   * hasn't already been classified by any of the others. A function
   * which has not been so classified is an <i>unclassifed function</i>.
   * <p>
   * If <tt>opt_Sup</tt> is provided, record that constr.prototype
   * inherits from opt_Sup.prototype. This bookkeeping helps
   * directConstructor().
   * <p>
   * <tt>opt_name</tt>, if provided, should be the name of the constructor
   * function. Currently, this is used only to generate friendlier
   * error messages.
   */
  function markCtor(constr, opt_Sup, opt_name) {
    enforceType(constr, 'function', opt_name);
    if (isFunc(constr)) {
      fail("Simple functions can't be constructors: ", constr);
    }
    if (isXo4aFunc(constr)) {
      fail("Exophoric functions can't be constructors: ", constr);
    }
    constr.CONSTRUCTOR___ = true;
    if (opt_Sup) {
      derive(constr, opt_Sup);
    } else if (constr !== Object) {
      fail('Only "Object" has no super: ', constr);
    }
    if (opt_name) {
      constr.NAME___ = String(opt_name);
    }
    if (constr !== Object && constr !== Array) {
      // Iff constr is not Object nor Array, then any object inheriting from
      // constr.prototype is a constructed object which therefore tames to
      // itself by default. We do this with AS_TAMED___ and AS_FERAL___
      // methods on the prototype so it can be overridden either by overriding
      // these methods or by pre-taming with ___.tamesTo or ___.tamesToSelf.
      constr.prototype.AS_TAMED___ =
        constr.prototype.AS_FERAL___ = function() {
          return this;
        };
    }
    return constr;  // translator freezes constructor later
  }

  function derive(constr, sup) {
    var proto = constr.prototype;
    sup = asCtor(sup);
    if (isFrozen(constr)) {
      fail('Derived constructor already frozen: ', constr);
    }
    if (!(proto instanceof sup)) {
      fail('"' + constr + '" does not derive from "', sup);
    }
    if ('__proto__' in proto && proto.__proto__ !== sup.prototype) {
      fail('"' + constr + '" does not derive directly from "', sup);
    }
    if (!isFrozen(proto)) {
      // Some platforms, like Safari, actually conform to the part
      // of the ES3 spec which states that the constructor property
      // of implicitly created prototypical objects are not
      // deletable. But this prevents the inheritance-walking
      // algorithm (kludge) in directConstructor from working. Thus,
      // we set proto___ here so that directConstructor can skip
      // that impossible case.
      proto.proto___ = sup.prototype;
    }
  }

  /**
   * Initialize argument constructor <i>feralCtor</i> so that it
   * represents a "subclass" of argument constructor <i>someSuper</i>,
   * and return a non-invokable taming of <i>feralCtor</i>. 
   *
   * Given: 
   *
   *   function FeralFoo() { ... some uncajoled constructor ... }
   *   var Foo = extend(FeralFoo, FeralSuper, 'Foo');
   *
   * it will be the case that:
   *
   *   new FeralFoo() instanceof Foo
   *
   * however -- and this is the crucial property -- cajoled code will get an
   * error if it invokes either of:
   *
   *   new Foo()
   *   Foo()
   *
   * This allows us to expose the tame Foo to cajoled code, allowing
   * it to sense that all the FeralFoo instances we give it are
   * instanceof Foo, without granting to cajoled code the means to
   * create any new such instances.
   * 
   * extend() also sets <i>feralCtor</i>.prototype to set up the
   * prototype chain so that 
   *
   *   new FeralFoo() instanceof FeralSuper
   * and
   *   new FeralFoo() instanceof Super
   *
   * @param feralCtor An feral-only uncajoled constructor. This must
   *        NOT be exposed to cajoled code by any other mechanism.
   * @param someSuper Some constructor representing the
   *        superclass. This can be <ul>
   *        <li>a feralCtor that had been provided as a first argument
   *            in a previous call to extend(), 
   *        <li>an inertCtor as returned by a previous call to
   *            extend(), or 
   *        <li>a constructor that has been marked as such by ___.markCtor().
   *        </ul>
   *        In all cases, someSuper.prototype.constructor must be
   *        a constructor that has been marked as such by
   *        ___.markCtor(). 
   * @param opt_name If the returned inert constructor is made
   *        available this should be the property name used.
   *
   * @return a tame inert class constructor as described above.
   */
  function extend(feralCtor, someSuper, opt_name) {
    if (!('function' === typeof feralCtor)) {
      fail('Internal: Feral constructor is not a function');
    }
    someSuper = asCtor(someSuper.prototype.constructor);
    var noop = function () {};
    noop.prototype = someSuper.prototype;
    feralCtor.prototype = new noop();
    feralCtor.prototype.proto___ = someSuper.prototype;

    var inert = function() {
      fail('This constructor cannot be called directly');
    };

    inert.prototype = feralCtor.prototype;
    feralCtor.prototype.constructor = inert;
    markCtor(inert, someSuper, opt_name);
    tamesTo(feralCtor, inert);
    return primFreeze(inert);
  }

  /**
   * Marks a function as a feral exophoric function whose
   * <tt>this</tt> and arguments may be tame and whose results will be
   * tame .
   */
  function markXo4a(func, opt_name) {
    enforceType(func, 'function', opt_name);
    if (isCtor(func)) {
      fail("Internal: Constructors can't be exophora: ", func);
    }
    if (isFunc(func)) {
      fail("Internal: Simple functions can't be exophora: ", func);
    }
    func.XO4A___ = true;
    if (opt_name) {
      func.NAME___ = opt_name;
    }
    func.AS_TAMED___ = tameXo4a;
    return primFreeze(func);
  }

  /**
   * Mark a function innocent if it might expect a feral <tt>this</tt>
   * and arguments and might return a feral result, but should still
   * tame to something that allows untrusted cajoled code to invoke it.
   */
  function markInnocent(func, opt_name) {
    enforceType(func, 'function', opt_name);
    if (isCtor(func)) {
      fail("Internal: Constructors aren't innocent: ", func);
    }
    if (isFunc(func)) {
      fail("Internal: Simple functions aren't innocent: ", func);
    }
    if (isXo4aFunc(func)) {
      fail("Internal: Exophoric functions aren't innocent: ", func);
    }
    if (opt_name) {
      func.NAME___ = opt_name;
    }
    func.AS_TAMED___ = tameInnocent;
    return primFreeze(func);
  }

  /**
   * Mark fun as a simple function and freeze it.
   * <p>
   * simple functions tame to themselves by default.
   * <p>
   * opt_name, if provided, should be the name of the
   * function. Currently, this is used only to generate friendlier
   * error messages. 
   */
  function markFuncFreeze(fun, opt_name) {
    // inline: enforceType(fun, 'function', opt_name);
    if (typeOf(fun) !== 'function') {
      fail('expected function instead of ', typeOf(fun),
           ': ', (opt_name || fun));
    }

    // inline: if (isCtor(fun)) {
    if (fun.CONSTRUCTOR___) {
      fail("Constructors can't be simple functions: ", fun);
    }
    // inline: if (isXo4aFunc(fun)) {
    if (fun.XO4A___) {
      fail("Exophoric functions can't be simple functions: ", fun);
    }
    fun.FUNC___ = opt_name ? String(opt_name) : true;
    return primFreeze(fun);
  }

  /** This "Only" form doesn't freeze */
  function asCtorOnly(constr) {
    if (isCtor(constr) || isFunc(constr)) {
      return constr;
    }
    enforceType(constr, 'function');
    fail("Untamed functions can't be called as constructors: ", constr);
  }

  /** Only constructors and simple functions can be called as constructors */
  function asCtor(constr) {
    return primFreeze(asCtorOnly(constr));
  }

  /**
   * Only simple functions (or Number, String, or Boolean) can be
   * called as simple functions. 
   * <p>
   * It is now <tt>asFunc</tt>'s responsibility to
   * <tt>primFreeze(fun)</tt>.
   */
  function asFunc(fun) {
    if (fun && fun.FUNC___) {
      // fastpath shortcut
      if (fun.FROZEN___ === fun) {
        return fun;
      } else {
        return primFreeze(fun);
      }
    }
    enforceType(fun, 'function');
    if (isCtor(fun)) {
      if (fun === Number || fun === String || fun === Boolean) {
        // TODO(erights): To avoid accidents, <tt>markXo4a</tt>,
        // <tt>markFuncFreeze</tt>, and <tt>markCtor</tt> each ensure
        // that these classifications are exclusive. A function can be
        // classified as in at most one of these categories. However,
        // some primordial type conversion functions like
        // <tt>String</tt> need to be invocable both ways, so we
        // should probably relax this constraint.
        // <p>
        // But before we do, we should reexamine other
        // implications. For example, simple-functions, when called
        // reflectively by <tt>call</tt> or <tt>apply</tt> (and
        // therefore <tt>bind</tt>), ignore their first argument,
        // whereas constructors can be called reflectively by
        // <tt>call</tt> to do super-initialization on behalf of a
        // derived constructor.
        // <p>
        // Curiously, ES3 also defines function behavior different
        // from constructor behavior for <tt>Object</tt>,
        // <tt>Date</tt>, <tt>RegExp</tt>, and <tt>Error</tt>. (Not
        // sure about <tt>Array</tt>.) We should understand these as
        // well before introducing a proper solution.
        return primFreeze(fun);
      }
      fail("Constructors can't be called as simple functions: ", fun);
    }
    if (isXo4aFunc(fun)) {
      fail("Exophoric functions can't be called as simple functions: ", fun);
    }
    fail("Untamed functions can't be called as simple functions: ", fun);
  }

  /**
   * Coerces fun to a genuine simple-function.
   * <p>
   * If fun is an pseudo-function, then return a simple-function that
   * invokes fun's apply method. Otherwise, asFunc().
   */
  function toFunc(fun) {
    if (isPseudoFunc(fun)) {
      return markFuncFreeze(function applier(var_args) {
        return callPub(fun, 'apply', [USELESS, Array.slice(arguments, 0)]);
      });
    }
    return asFunc(fun);
  }

  /**
   * An object is prototypical iff its 'constructor' property points
   * at a genuine function whose 'prototype' property points back at
   * it.
   * <p>
   * Cajita code cannot access or create prototypical objects since
   * the 'prototype' property of genuine functions is inaccessible,
   * and since the transient function used by <tt>beget</tt> to create
   * inheritance chains does not escape.
   */
  function isPrototypical(obj) {
    if (typeOf(obj) !== 'object') { return false; }
    if (obj === null) { return false; }
    var constr = obj.constructor;
    if (typeOf(constr) !== 'function') { return false; }
    return constr.prototype === obj;
  }

  /**
   * Throws an exception if the value is an unmarked function or a
   * prototypical object.
   */
  function asFirstClass(value) {
    switch (typeOf(value)) {
      case 'function': {
        if (isFunc(value) || isCtor(value)) {
          if (isFrozen(value)) {
            return value;
          }
          // TODO(metaweta): make this a cajita-uncatchable exception
          fail('Internal: non-frozen function encountered: ', value);
        } else if (isXo4aFunc(value)) {
          // TODO(metaweta): make this a cajita-uncatchable exception
          // TODO(erights): non-user-hostile error message
          fail('Internal: toxic exophora encountered: ', value);
        } else {
          // TODO(metaweta): make this a cajita-uncatchable exception
          fail('Internal: toxic function encountered: ', value);
        }
        break;
      }
      case 'object': {
        if (value !== null && isPrototypical(value)) {
          // TODO(metaweta): make this a cajita-uncatchable exception
          fail('Internal: prototypical object encountered: ', value);
        }
        return value;
      }
      default: {
        return value;
      }
    }
  }

  ////////////////////////////////////////////////////////////////////////
  // Accessing properties
  ////////////////////////////////////////////////////////////////////////

  /**
   * Can a Cajita client of <tt>obj</tt> read its {@code name} property?
   * <p>
   * If the property is unmentionable (i.e. ends in an '__'), then no.
   * If the property was defined by Cajita code, then yes. If it was
   * whitelisted, then yes. Or if the property is an own property of
   * <i>some</i> JSON container, then yes.
   * <p>
   * Why "some"? If record y inherits from record x, and 'foo' is an own
   * property of x, then canReadPub(y, 'foo') must be true.
   */
  function canReadPub(obj, name) {
    if (typeof name === 'number' && name >= 0) { return name in obj; }
    name = String(name);
    if (obj === null) { return false; }
    if (obj === void 0) { return false; }
    if (obj[name + '_canRead___']) { return (name in Object(obj)); }
    if (endsWith__.test(name)) { return false; }
    if (name === 'toString') { return false; }
    if (!isJSONContainer(obj)) { return false; }
    if (!myOriginalHOP.call(obj, name)) { return false; }
    fastpathRead(obj, name);
    return true;
  }

  function hasOwnPropertyOf(obj, name) {
    if (typeof name === 'number' && name >= 0) { return hasOwnProp(obj, name); }
    name = String(name);
    if (obj && obj[name + '_canRead___'] === obj) { return true; }
    return canReadPub(obj, name) && myOriginalHOP.call(obj, name);
  }

  /**
   * Implements Cajita's <tt><i>name</i> in <i>obj</i></tt>
   */
  function inPub(name, obj) {
    var t = typeof obj;
    if (!obj || (t !== 'object' && t !== 'function')) {
      throw new TypeError('invalid "in" operand: ' + obj);
    }
    obj = Object(obj);
    if (canReadPub(obj, name)) { return true; }
    if (canCallPub(obj, name)) { return true; }
    if ((name + '_getter___') in obj) { return true; }
    if ((name + '_handler___') in obj) { return true; }
    return false;
  }

  /**
   * Called by Caja code attempting to read a property.
   * <p>
   * If it can't then <tt>readPub</tt> returns <tt>undefined</tt> instead.
   */
  function readPub(obj, name) {
    if (typeof name === 'number' && name >= 0) {
      if (typeof obj === 'string') {
        // In partial anticipation of ES5.
        // TODO(erights): Once ES5 settles, revisit this and
        // correctly implement the agreed semantics.
        // Mike Samuel suggested also making it conditional on
        //  (+name) === (name & 0x7fffffff)
        // but then realized that it violates the requirement
        // that the string form be the canonical form of the
        // number. So 'foo'['00'] would be treated the same
        // as 'foo'['0'] which is incorrect. 
        return obj.charAt(name);
      } else {
        return obj[name];
      }
    }
    name = String(name);
    if (canReadPub(obj, name)) { return obj[name]; }
    if (obj === null || obj === void 0) {
      throw new TypeError("Can't read " + name + ' on ' + obj);
    }
    return obj.handleRead___(name);
  }

  /**
   * If <tt>obj</tt> is an object with a property <tt>name</tt> that
   * should be objectively readable from Valija, return
   * <tt>obj[name]</tt>, else <tt>pumpkin</tt>.
   * <p>
   * Provides a fastpath for Valija's <tt>read()</tt> function
   * <tt>$v.r()</tt>. The reason for returning the passed in pumpkin
   * rather than, for example, <tt>undefined</tt>, is so that the
   * caller can pass in a known unique value and distinguish it, on
   * return, from any possible valid value.
   * <p>
   * A property should be objectively readable iff<ul>
   * <li>It is readable from Cajita, and
   * <li><tt>obj</tt> is not a function, and
   * <li>either<ul>
   *     <li><tt>name</tt> is an own property of <tt>obj</tt>, or
   *     <li><tt>obj</tt> inherits <tt>name</tt> from an ancestor that
   *         Cajita considers first-class. The one such possibility is
   *         when <tt>obj</tt> is a record inheriting <tt>name</tt>
   *         from another record. (A record is a non-prototypical
   *         object whose directConstructor is Object.)
   *     </ul>
   * </ul>
   */
  function readOwn(obj, name, pumpkin) {
    if (typeof obj !== 'object' || !obj) {
      if (typeOf(obj) !== 'object') {
        return pumpkin;
      }
    }
    if (typeof name === 'number' && name >= 0) {
      if (myOriginalHOP.call(obj, name)) { return obj[name]; }
      return pumpkin;
    }
    name = String(name);
    if (obj[name + '_canRead___'] === obj) { return obj[name]; }
    if (!myOriginalHOP.call(obj, name)) { return pumpkin; }
    // inline remaining relevant cases from canReadPub
    if (endsWith__.test(name)) { return pumpkin; }
    if (name === 'toString') { return pumpkin; }
    if (!isJSONContainer(obj)) { return pumpkin; }
    fastpathRead(obj, name);
    return obj[name];
  }

  /**
   * Ensure that all the permitsUsed starting at result are forever
   * safe to allow without runtime checks.
   */
  function enforceStaticPath(result, permitsUsed) {
    forOwnKeys(permitsUsed, markFuncFreeze(function(name, subPermits) {
      // Don't factor out since we don't enforce frozen if permitsUsed
      // are empty.
      // TODO(erights): Once we have ES5ish attribute control, it
      // will suffice to enforce that each used property is frozen
      // independent of the object as a whole.
      enforce(isFrozen(result), 'Assumed frozen: ', result);
      if (name === '()') {
        // TODO(erights): Revisit this case
      } else {
        enforce(canReadPub(result, name),
                'Assumed readable: ', result, '.', name);
        if (inPub('()', subPermits)) {
          enforce(canCallPub(result, name),
                  'Assumed callable: ', result, '.', name, '()');
        }
        enforceStaticPath(readPub(result, name), subPermits);
      }
    }));
  }

  /**
   * Privileged code attempting to read an imported value from a module's
   * <tt>IMPORTS___</tt>. This function is NOT available to Cajita code.
   * <p>
   * This delegates to <tt>readOwn()</tt>, and so will only read
   * those properties from module_imports that are objectively visible
   * from both Cajita and Valija.
   */
  function readImport(module_imports, name, opt_permitsUsed) {
    var pumpkin = {};
    var result = readOwn(module_imports, name, pumpkin);
    if (result === pumpkin) {
      log('Linkage warning: ' + name + ' not importable');
      return void 0;
    }
    if (opt_permitsUsed) {
      enforceStaticPath(result, opt_permitsUsed);
    }
    return result;
  }

  /**
   * Can "innocent" code enumerate the named property on this object?
   * <p>
   * "Innocent" code is code which we assume to be ignorant of Caja,
   * not to be actively hostile, but which may be buggy (and
   * therefore accidentally harmful or exploitable). This
   * corresponds to legacy code, such as libraries, that we decide
   * to run untranslated, perhaps hidden or tamed, but which needs
   * to co-exist smoothly with the Caja runtime.
   * <p>
   * An earlier version of canInnocentEnum() filtered out exactly those
   * names ending with a double underbar. It now filters out exactly
   * those names ending in a triple underbar. Cajita code can't see names
   * ending in a double underbar, since existing platforms (like
   * Firefox) use such names for purposes that should be hidden from
   * Caja code. However, it is not up to Caja to shield innocent code
   * from seeing such platform properties. All the magic names Cajita
   * adds for its own internal bookkeeping end in triple underbar, so
   * that is all we need to hide from innocent code.
   */
  function canInnocentEnum(obj, name) {
    name = String(name);
    if (endsWith___.test(name)) { return false; }
    return true;
  }

  /**
   * Would a Cajita for/in loop by a client of obj see this name?
   * <p>
   * For properties defined in Cajita, this is generally the same as
   * canReadPub.  Otherwise according to whitelisting.
   */
  function canEnumPub(obj, name) {
    if (obj === null) { return false; }
    if (obj === void 0) { return false; }
    name = String(name);
    if (obj[name + '_canEnum___']) { return true; }
    if (endsWith__.test(name)) { return false; }
    if (!isJSONContainer(obj)) { return false; }
    if (!myOriginalHOP.call(obj, name)) { return false; }
    fastpathEnum(obj, name);
    if (name === 'toString') { return true; }
    fastpathRead(obj, name);
    return true;
  }

  /**
   * Like canEnumPub, but allows only non-inherited properties.
   */
  function canEnumOwn(obj, name) {
    name = String(name);
    if (obj && obj[name + '_canEnum___'] === obj) { return true; }
    return canEnumPub(obj, name) && myOriginalHOP.call(obj, name);
  }

  /**
   * Returns a new object whose only utility is its identity and (for
   * diagnostic purposes only) its name.
   */
  function Token(name) {
    name = String(name);
    return primFreeze({
      toString: markFuncFreeze(function tokenToString() { return name; }),
      throwable___: true
    });
  }
  markFuncFreeze(Token);

  /**
   * Inside a <tt>cajita.forOwnKeys()</tt>, or <tt>cajita.forAllKeys()</tt>, the
   * body function can terminate early, as if with a conventional
   * <tt>break;</tt>, by doing a <pre>return cajita.BREAK;</pre>
   */
  var BREAK = Token('BREAK');

  /**
   * A unique value that should never be made accessible to untrusted
   * code, for distinguishing the absence of a result from any
   * returnable result.
   * <p>
   * See makeNewModuleHandler's getLastOutcome().
   */
  var NO_RESULT = Token('NO_RESULT');

  /**
   * For each sensible key/value pair in obj, call fn with that
   * pair.
   * <p>
   * If obj is an array, then enumerate indexes. Otherwise, enumerate
   * the canEnumOwn() property names.
   */
  function forOwnKeys(obj, fn) {
    fn = toFunc(fn);
    var keys = ownKeys(obj);
    for (var i = 0; i < keys.length; i++) {
      if (fn(keys[i], readPub(obj, keys[i])) === BREAK) {
        return;
      }
    }
  }

  /**
   * For each sensible key/value pair in obj, call fn with that
   * pair.
   * <p>
   * If obj is an array, then enumerate indexes. Otherwise, enumerate
   * the canEnumPub() property names.
   */
  function forAllKeys(obj, fn) {
    fn = toFunc(fn);
    var keys = allKeys(obj);
    for (var i = 0; i < keys.length; i++) {
      if (fn(keys[i], readPub(obj, keys[i])) === BREAK) {
        return;
      }
    }
  }

  /**
   * Return an array of the publicly readable own keys of obj.
   * <p>
   * If obj is an array, then enumerate indexes. Otherwise, enumerate
   * the canEnumOwn() property names.
   */
  function ownKeys(obj) {
    var result = [];
    if (isArray(obj)) {
      var len = obj.length;
      for (var i = 0; i < len; i++) {
        result.push(i);
      }
    } else {
      for (var k in obj) {
        if (canEnumOwn(obj, k)) {
          result.push(k);
        }
      }
      if (obj !== void 0 && obj !== null && obj.handleEnum___) {
        result = result.concat(obj.handleEnum___(true));
      }
    }
    return result;
  }

  /**
   * Return an array of the publicly readable own and inherited keys of obj.
   * <p>
   * If obj is an array, then enumerate indexes. Otherwise, enumerate
   * the canEnumPub() property names.
   */
  function allKeys(obj) {
    if (isArray(obj)) {
      return ownKeys(obj);
    } else {
      var result = [];
      for (var k in obj) {
        if (canEnumPub(obj, k)) {
          result.push(k);
        }
      }
      if (obj !== void 0 && obj !== null && obj.handleEnum___) {
        result = result.concat(obj.handleEnum___(false));
      }
      return result;
    }
  }

  /**
   * Can this be called as a public method?
   * <p>
   * For genuine methods, they are only callable if the canCall
   * attribute is set. Otherwise, if this property is readable and
   * holds a simple function, then it's also callable as a function,
   * which we can memoize.
   */
  function canCallPub(obj, name) {
    if (obj === null) { return false; }
    if (obj === void 0) { return false; }
    name = String(name);
    if (obj[name + '_canCall___']) { return true; }
    if (obj[name + '_grantCall___']) {
      fastpathCall(obj, name);
      return true;
    }
    if (!canReadPub(obj, name)) { return false; }
    if (endsWith__.test(name)) { return false; }
    if (name === 'toString') { return false; }
    var func = obj[name];
    if (!isFunc(func) && !isXo4aFunc(func)) {
      return false;
    }
    fastpathCall(obj, name);
    return true;
  }

  /**
   * A client of obj tries to call one of its methods.
   */
  function callPub(obj, name, args) {
    name = String(name);
    if (obj === null || obj === void 0) {
      throw new TypeError("Can't call " + name + ' on ' + obj);
    }
    if (obj[name + '_canCall___'] || canCallPub(obj, name)) {
      return obj[name].apply(obj, args);
    }
    if (obj.handleCall___) { return obj.handleCall___(name, args); }
    fail('not callable:', debugReference(obj), '.', name);
  }

  /**
   * Can a client of obj directly assign to its name property?
   * <p>
   * If this property is unmentionable (i.e., ends with a '__') or if this
   * object is frozen, then no.
   * Else if this is an own property defined by Cajita code,
   * then yes. If the object is a JSON container, then
   * yes. Otherwise according to whitelisting decisions.
   */
  function canSetPub(obj, name) {
    name = String(name);
    if (canSet(obj, name)) { return true; }
    if (endsWith__.test(name)) { return false; }
    if (name === 'valueOf') { return false; }
    if (name === 'toString') { return false; }
    return !isFrozen(obj) && isJSONContainer(obj);
  }

  /** A client of obj attempts to assign to one of its properties. */
  function setPub(obj, name, val) {
    // asFirstClass() here would be a useful safety check, to prevent
    // the further propogation of, for example, a leaked toxic
    // function. However, its security benefit is questionable, and
    // the check is expensive in this position.
//  val = asFirstClass(val);
    if (typeof name === 'number' &&
        name >= 0 &&
        // See issue 875
        obj instanceof Array &&
        obj.FROZEN___ !== obj) {
      return obj[name] = val;
    }
    name = String(name);
    if (obj === null || obj === void 0) {
      throw new TypeError("Can't set " + name + ' on ' + obj);
    }
    if (obj[name + '_canSet___'] === obj) {
      return obj[name] = val;
    } else if (canSetPub(obj, name)) {
      fastpathSet(obj, name);
      return obj[name] = val;
    } else {
      return obj.handleSet___(name, val);
    }
  }

  /**
   * Can the given function have the given static method added to it?
   * @param {Function} fun
   * @param {string} staticMemberName an identifier in the public namespace.
   */
  function canSetStatic(fun, staticMemberName) {
    staticMemberName = '' + staticMemberName;
    if (typeOf(fun) !== 'function') {
      log('Cannot set static member of non function: ' + fun);
      return false;
    }
    if (isFrozen(fun)) {
      log('Cannot set static member of frozen function: ' + fun);
      return false;
    }
    if (!isFunc(fun)) {
      log('Can only set static members on simple-functions: ' + fun);
      return false;
    }
    if (staticMemberName === 'toString') {
      // no diagnostic as this is a normal fault-handling case.
      return false;
    }
    // statics are public
    if (endsWith__.test(staticMemberName) || staticMemberName === 'valueOf') {
      log('Illegal static member name: ' + staticMemberName);
      return false;
    }
    // disallows prototype, call, apply, bind
    if (staticMemberName in fun) {
      log('Cannot override static member: ' + staticMemberName);
      return false;
    }
    return true;
  }

  /**
   * Sets a static members of a fun, making sure that it can't be used to
   * override call/apply/bind and other builtin members of function.
   * @param {Function} fun
   * @param {string} staticMemberName an identifier in the public namespace.
   * @param staticMemberValue the value of the static member.
   */
  function setStatic(fun, staticMemberName, staticMemberValue) {
    staticMemberName = '' + staticMemberName;
    if (canSetStatic(fun, staticMemberName)) {
      fun[staticMemberName] = staticMemberValue;
      fastpathEnum(fun, staticMemberName);
      fastpathRead(fun, staticMemberName);
    } else {
      fun.handleSet___(staticMemberName, staticMemberValue);
    }
  }

  /**
   * Can a client of obj delete the named property?
   */
  function canDeletePub(obj, name) {
    name = String(name);
    if (isFrozen(obj)) { return false; }
    if (endsWith__.test(name)) { return false; }
    if (name === 'valueOf') { return false; }
    if (name === 'toString') { return false; }
    if (isJSONContainer(obj)) { return true; }
    return false;
  }

  /**
   * A client of obj can only delete a property of obj if obj is a
   * non-frozen JSON container.
   */
  function deletePub(obj, name) {
    name = String(name);
    if (obj === null || obj === void 0) {
      throw new TypeError("Can't delete " + name + ' on ' + obj);
    }
    if (canDeletePub(obj, name)) {
      // See deleteFieldEntirely for reasons why we don't cache deletability.
      return deleteFieldEntirely(obj, name);
    } else {
      return obj.handleDelete___(name);
    }
  }

  /**
   * Deletes a field removing any cached permissions.
   * @param {object} obj
   * @param {string} name of field in obj to delete.
   * @return {boolean}
   * @throws {Error} if field not deletable or name not in field.
   * @private
   */
  function deleteFieldEntirely(obj, name) {
    // Can't cache fastpath delete since deleting the field should remove
    // all privileges for that field.
    delete obj[name + '_canRead___'];
    delete obj[name + '_canEnum___'];
    delete obj[name + '_canCall___'];
    delete obj[name + '_grantCall___'];
    delete obj[name + '_grantSet___'];
    delete obj[name + '_canSet___'];
    delete obj[name + '_canDelete___'];
    return (delete obj[name]) || (fail('not deleted: ', name), false);
  }

  ////////////////////////////////////////////////////////////////////////
  // Other
  ////////////////////////////////////////////////////////////////////////

  /**
   * This returns a frozen array copy of the original array or
   * array-like object.
   * <p>
   * If a Cajita program makes use of <tt>arguments</tt> in any
   * position other than <tt>arguments.callee</tt>, this is
   * rewritten to use a frozen array copy of arguments instead. This
   * way, if Cajita code passes its arguments to someone else, they
   * are not giving the receiver the rights to access the passing
   * function nor to modify the parameter variables of the passing
   * function.
   */
  function args(original) {
    return primFreeze(Array.slice(original, 0));
  }

  /**
   * When a <tt>this</tt> value must be provided but nothing is
   * suitable, provide this useless object instead.
   */
  var USELESS = Token('USELESS');

  /**
   * A call to cajita.manifest(data) is dynamically ignored, but if the
   * data expression is valid static JSON text, its value is made
   * statically available to the module loader.
   * <p>
   * TODO(erights): Find out if this is still the plan.
   */
  function manifest(ignored) {}

  /**
   * All the extra fields observed in Error objects on any supported
   * browser which seem to carry possibly-useful diagnostic info.
   * <p>
   * By "extra", we means any fields other that those already
   * accessible to cajoled code, namely <tt>name</tt> and
   * <tt>message</tt>. 
   */
  var stackInfoFields = [
    'stack', 'fileName', 'lineNumer', // Seen in FF 3.0.3
    // fileName, lineNumber also seen in Rhino 1.7r1
    'description', // Seen in IE 6.0.2900, but seems identical to "message"
    'stackTrace', // Seen on Opera 9.51 after enabling
                  // "opera:config#UserPrefs|Exceptions Have Stacktrace"
    'sourceURL', 'line' // Seen on Safari 3.1.2
  ];

  /**
   * If given an Error in which hidden diagnostic info may be found,
   * return a record in which that diagnostic info is available to
   * cajoled code. 
   * <p>
   * This is so named because it used to be implemented as the
   * unsealer of a sealer/unsealer pair. TODO(erights) consider
   * renaming and deprecating the current name.
   */
  function callStackUnsealer(ex) {
    if (ex && isInstanceOf(ex, Error)) {
      var stackInfo = {};
      var numStackInfoFields = stackInfoFields.length;
      for (var i = 0; i < numStackInfoFields; i++) {
        var k = stackInfoFields[i];
        if (k in ex) { stackInfo[k] = ex[k]; }
      }
      if ('cajitaStack___' in ex) {
        // Set by cajita-debugmode.js
        stackInfo.cajitaStack = ex.cajitaStack___;
      }
      return primFreeze(stackInfo);
    }
    return void 0;
  }

  /**
   * Receives whatever was caught by a user defined try/catch block.
   *
   * @param ex A value caught in a try block.
   * @return The value to make available to the cajoled catch block.
   */
  function tameException(ex) {
    if (ex && ex.UNCATCHABLE___) { throw ex; }
    try {
      switch (typeOf(ex)) {
        case 'string':
        case 'number':
        case 'boolean': 
        case 'undefined': {
          // Immutable.
          return ex;
        }
        case 'object': {
          if (ex === null) { return null; }
          if (ex.throwable___) { return ex; }
          if (isInstanceOf(ex, Error)) { return primFreeze(ex); }
          return '' + ex;
        }
        case 'function': {
          // According to Pratap Lakhsman's "JScript Deviations" S2.11
          // If the caught object is a function, calling it within the catch
          // supplies the head of the scope chain as the "this value".  The
          // called function can add properties to this object.  This implies
          // that for code of this shape:
          //     var x;
          //     try {
          //       // ...
          //     } catch (E) {
          //       E();
          //       return s;
          //     }
          // The reference to 'x' within the catch is not necessarily to the
          // local declaration of 'x'; this gives Catch the same performance
          // problems as with.

          // We return a different, powerless function instead.
          var name = '' + (ex.name || ex);
          function inLieuOfThrownFunction() {
            return 'In lieu of thrown function: ' + name;
          };
          return markFuncFreeze(inLieuOfThrownFunction, name);
        }
        default: {
          log('Unrecognized exception type: ' + (typeOf(ex)));
          return 'Unrecognized exception type: ' + (typeOf(ex));
        }
      }
    } catch (_) {
      // Can occur if coercion to string fails, or if ex has getters
      // that fail. This function must never throw an exception
      // because doing so would cause control to leave a catch block
      // before the handler fires.
      log('Exception during exception handling.');
      return 'Exception during exception handling.';
    }
  }

  /**
   * Makes a new empty object that directly inherits from <tt>proto</tt>.
   */
  function primBeget(proto) {
    if (proto === null) { fail('Cannot beget from null.'); }
    if (proto === (void 0)) { fail('Cannot beget from undefined.'); }
    function F() {}
    F.prototype = proto;
    var result = new F();
    result.proto___ = proto;
    return result;
  }

  /**
   * Creates a well formed Cajita record from a list of alternating
   * keys and values. 
   * <p>
   * The translator translates Cajita object literals into calls to
   * <tt>initializeMap</tt> so that a potentially toxic function
   * cannot be made the <tt>toString</tt> property of even a temporary
   * object. 
   */
  function initializeMap(list) {
    var result = {};
    for (var i = 0; i < list.length; i += 2) {
      // Call asFirstClass() here to prevent, for example, a toxic
      // function being used as the toString property of an object
      // literal.
      setPub(result, list[i], asFirstClass(list[i + 1]));
    }
    return result;
  }

  ////////////////////////////////////////////////////////////////////////
  // Taming mechanism
  ////////////////////////////////////////////////////////////////////////

  /**
   * Arrange to handle read-faults on <tt>obj[name]</tt>
   * by calling <tt>getHandler()</tt> as a method on
   * the faulted object.
   * <p>
   * In order for this fault-handler to get control, it's important
   * that no one does a conflicting <tt>grantRead()</tt>.
   * FIXME(ben): and fastpathRead()?
   */
  function useGetHandler(obj, name, getHandler) {
    obj[name + '_getter___'] = getHandler;
  }

  /**
   * Arrange to handle call-faults on <tt>obj[name](args...)</tt> by
   * calling <tt>applyHandler(args)</tt> as a method on the faulted
   * object.
   * <p>
   * Note that <tt>applyHandler</tt> is called with a single argument,
   * which is the list of arguments in the original call.
   * <p>
   * In order for this fault-handler to get control, it's important
   * that no one does a conflicting grantCall() or other grants which
   * imply grantCall().
   * FIXME(ben): also fastpath?
   */
  function useApplyHandler(obj, name, applyHandler) {
    obj[name + '_handler___'] = applyHandler;
  }

  /**
   * Arrange to handle call-faults on <tt>obj[name](args...)</tt> by
   * calling <tt>callHandler(args...)</tt> as a method on the faulted
   * object.
   * <p>
   * Note that <tt>callHandler</tt> is called with the same arguments
   * as the original call.
   * <p>
   * In order for this fault-handler to get control, it's important
   * that no one does a conflicting grantCall() or other grants which
   * imply grantCall().
   * FIXME(ben): also fastpath?
   */
  function useCallHandler(obj, name, callHandler) {
    useApplyHandler(obj, name, function callApplier(args) {
      return callHandler.apply(this, args);
    });
  }

  /**
   * Arrange to handle set-faults on <tt>obj[name] = newValue</tt> by
   * calling <tt>setHandler(newValue)</tt> as a method on the faulted
   * object.
   * <p>
   * In order for this fault-handler to get control, it's important
   * that no one does a conflicting grantSet().
   * FIXME(ben): also fastpath?
   */
  function useSetHandler(obj, name, setHandler) {
    obj[name + '_setter___'] = setHandler;
  }

  /**
   * Arrange to handle delete-faults on <tt>delete obj[name]</tt> by
   * calling <tt>deleteHandler()</tt> as a method on the faulted object.
   * <p>
   * In order for this fault-handler to get control, it's important
   * that no one does a conflicting grantDelete().
   * FIXME(ben): also fastpath?
   */
  function useDeleteHandler(obj, name, deleteHandler) {
    obj[name + '_deleter___'] = deleteHandler;
  }

  /**
   * Whilelist obj[name] as a simple frozen function that can be either
   * called or read.
   */
  function grantFunc(obj, name) {
    markFuncFreeze(obj[name], name);
    grantCall(obj, name);
    grantRead(obj, name);
  }

  /**
   * Whitelist proto[name] as a generic exophoric function that can
   * safely be called with its <tt>this</tt> bound to other objects.
   * <p>
   * Since exophoric functions are not first-class, reading
   * proto[name] returns the corresponding pseudo-function -- a record
   * with simple-functions for its call, bind, and apply.
   */
  function grantGenericMethod(proto, name) {
    var func = markXo4a(proto[name], name);
    grantCall(proto, name);
    var pseudoFunc = tame(func);
    useGetHandler(proto, name, function xo4aGetter() {
      return pseudoFunc;
    });
  }

  /**
   * Use func as a virtual generic exophoric function.
   * <p>
   * Since exophoric functions are not first-class, reading
   * proto[name] returns the corresponding pseudo-function -- a record
   * with simple-functions for its call, bind, and apply.
   */
  function handleGenericMethod(obj, name, func) {
    var feral = obj[name];
    if (!hasOwnProp(obj, name)) {
      // TODO(erights): domita would currently generate this warning,
      // and generated warnings currently cause DomitaTest to fail. It
      // is not clear whether the right solution is to make domita not
      // generate these warnings or to not consider the triggering
      // condition to deserve a warning. In any case, DomitaTest
      // should probably become warning tolerant.
      //log('warning: possible taming mistake: (' + obj + ')[' + name + ']');
      feral = func;
    } else if (hasOwnProp(feral, 'TAMED_TWIN___')) {
      // TODO(erights): See above TODO note.
      //log('warning: already tamed: (' + obj + ')[' + name + ']');
      feral = func;      
    }
    useCallHandler(obj, name, func);
    var pseudoFunc = tameXo4a.call(func);
    tamesTo(feral, pseudoFunc);
    useGetHandler(obj, name, function genericGetter() {
      return pseudoFunc;
    });
  }

  /**
   * Virtually replace proto[name] with a fault-handler
   * wrapper that first verifies that <tt>this</tt> inherits from
   * proto.
   * <p>
   * When a pre-existing Javascript method may do something unsafe
   * when applied to a <tt>this</tt> of the wrong type, we need to
   * provide a fault-handler instead to prevent such mis-application.
   * <p>
   * In order for this fault handler to get control, it's important
   * that no one does an grantCall() or other grants which imply
   * grantCall().
   * FIXME(ben): also fastpath?
   */
  function grantTypedMethod(proto, name) {
    var original = proto[name];
    handleGenericMethod(proto, name, function guardedApplier(var_args) {
      if (!inheritsFrom(this, proto)) {
        fail("Can't call .", name, ' on a non ',
             directConstructor(proto), ': ', this);
      }
      return original.apply(this, arguments);
    });
  }

  /**
   * Virtually replace proto[name] with a fault-handler
   * wrapper that first verifies that <tt>this</tt> isn't frozen.
   * <p>
   * When a pre-existing Javascript method would mutate its object,
   * we need to provide a fault handler instead to prevent such
   * mutation from violating Cajita semantics.
   * <p>
   * In order for this fault handler to get control, it's important
   * that no one does an grantCall() or other grants which imply
   * grantCall().
   * FIXME(ben): also fastpath?
   */
  function grantMutatingMethod(proto, name) {
    var original = proto[name];
    handleGenericMethod(proto, name, function nonMutatingApplier(var_args) {
      if (isFrozen(this)) {
        fail("Can't .", name, ' a frozen object');
      }
      return original.apply(this, arguments);
    });
  }

  /**
   * Virtually replace proto[name] with a fault-handler wrapper under
   * the assumption that the original is a generic innocent method.
   * <p>
   * As an innocent method, we assume it is exophoric (uses its
   * <tt>this</tt> parameter), requires a feral <tt>this</tt> and
   * arguments, and returns a feral result. As a generic method, we
   * assume that its <tt>this</tt> may be bound to objects that do not
   * inherit from <tt>proto</tt>.
   * <p>
   * The wrapper will untame <tt>this</tt>. Note that typically
   * <tt>this</tt> will be a constructed object and so will untame to
   * itself. The wrapper will also untame the arguments and tame and
   * return the result.
   */
  function grantInnocentMethod(proto, name) {
    var original = proto[name];
    handleGenericMethod(proto, name, function guardedApplier(var_args) {
      // like tameApplyFuncWrapper() but restated to avoid triple wrapping.
      var feralThis = stopEscalation(untame(this));
      var feralArgs = untame(Array.slice(arguments, 0));
      var feralResult = original.apply(feralThis, feralArgs);
      return tame(feralResult);
    });
  }

  /**
   * Verifies that regexp is something that can appear as a
   * parameter to a Javascript method that would use it in a match.
   * <p>
   * If it is a RegExp, then this match might mutate it, which must
   * not be allowed if regexp is frozen. Otherwise it must be a string.
   */
  function enforceMatchable(regexp) {
    if (isInstanceOf(regexp, RegExp)) {
      if (isFrozen(regexp)) {
        fail("Can't match with frozen RegExp: ", regexp);
      }
    } else {
      enforceType(regexp, 'string');
    }
  }

  /**
   * A shorthand that happens to be useful here.
   * <p>
   * For all i in arg2s: func2(arg1,arg2s[i]).
   */
  function all2(func2, arg1, arg2s) {
    var len = arg2s.length;
    for (var i = 0; i < len; i += 1) {
      func2(arg1, arg2s[i]);
    }
  }

  ////////////////////////////////////////////////////////////////////////
  // Taming decisions
  ////////////////////////////////////////////////////////////////////////

  /// Math

  all2(grantRead, Math, [
    'E', 'LN10', 'LN2', 'LOG2E', 'LOG10E', 'PI', 'SQRT1_2', 'SQRT2'
  ]);
  all2(grantFunc, Math, [
    'abs', 'acos', 'asin', 'atan', 'atan2', 'ceil', 'cos', 'exp', 'floor',
    'log', 'max', 'min', 'pow', 'random', 'round', 'sin', 'sqrt', 'tan'
  ]);

  /// toString

  function grantToString(proto) {
    proto.TOSTRING___ = tame(markXo4a(proto.toString, 'toString'));
  }

  function makeToStringMethod(toStringValue) {
    function toStringMethod(var_args) {
      var args = Array.slice(arguments, 0);
      if (isFunc(toStringValue)) {
        return toStringValue.apply(this, args);
      }
      var toStringValueApply = readPub(toStringValue, 'apply');
      if (isFunc(toStringValueApply)) {
        return toStringValueApply.call(toStringValue, this, args);
      }
      var result = myOriginalToString.call(this);
      log('Not correctly printed: ' + result);
      return result;
    };
    return toStringMethod;
  }

  function toStringGetter() {
    if (hasOwnProp(this, 'toString') &&
        typeOf(this.toString) === 'function' &&
        !hasOwnProp(this, 'TOSTRING___')) {
      grantToString(this);
    }
    return this.TOSTRING___;
  }
  useGetHandler(Object.prototype, 'toString',
                toStringGetter);

  useApplyHandler(Object.prototype, 'toString',
                  function toStringApplier(args) {
    var toStringValue = toStringGetter.call(this);
    return makeToStringMethod(toStringValue).apply(this, args);
  });

  useSetHandler(Object.prototype, 'toString',
                function toStringSetter(toStringValue) {
    if (isFrozen(this) || !isJSONContainer(this)) {
      return myKeeper.handleSet(this, 'toString', toStringValue);
    }
    var firstClassToStringValue = asFirstClass(toStringValue);
    this.TOSTRING___ = firstClassToStringValue;
    this.toString = makeToStringMethod(firstClassToStringValue);
    return toStringValue;
  });

  useDeleteHandler(Object.prototype, 'toString',
                   function toStringDeleter() {
    if (isFrozen(this) || !isJSONContainer(this)) {
      return myKeeper.handleDelete(this, 'toString');
    }
    return (delete this.toString) && (delete this.TOSTRING___);
  });

  /// Object

  markCtor(Object, void 0, 'Object');
  Object.prototype.TOSTRING___ = tame(markXo4a(function() {
    if (this.CLASS___) {
      return '[object ' + this.CLASS___ + ']';
    } else {
      return myOriginalToString.call(this);
    }
  }, 'toString'));
  all2(grantGenericMethod, Object.prototype, [
    'toLocaleString', 'valueOf', 'isPrototypeOf'
  ]);
  grantRead(Object.prototype, 'length');
  handleGenericMethod(Object.prototype, 'hasOwnProperty',
                      function hasOwnPropertyHandler(name) {
    return hasOwnPropertyOf(this, name);
  });
  handleGenericMethod(Object.prototype, 'propertyIsEnumerable',
                      function propertyIsEnumerableHandler(name) {
    name = String(name);
    return canEnumPub(this, name);
  });
  useCallHandler(Object, 'freeze', markFuncFreeze(freeze));
  useGetHandler(Object, 'freeze', function(){return freeze;});

  /// Function

  grantToString(Function.prototype);
  handleGenericMethod(Function.prototype, 'apply',
                      function applyHandler(self, opt_args) {
    return toFunc(this).apply(USELESS, opt_args || []);
  });
  handleGenericMethod(Function.prototype, 'call',
                      function callHandler(self, var_args) {
    return toFunc(this).apply(USELESS, Array.slice(arguments, 1));
  });
  handleGenericMethod(Function.prototype, 'bind',
                      function bindHandler(self, var_args) {
    var thisFunc = toFunc(this);
    var leftArgs = Array.slice(arguments, 1);
    function boundHandler(var_args) {
      var args = leftArgs.concat(Array.slice(arguments, 0));
      return thisFunc.apply(USELESS, args);
    }
    return markFuncFreeze(boundHandler);
  });

  /// Array

  markCtor(Array, Object, 'Array');
  grantFunc(Array, 'slice');
  grantToString(Array.prototype);
  all2(grantTypedMethod, Array.prototype, [ 'toLocaleString' ]);
  all2(grantGenericMethod, Array.prototype, [
    'concat', 'join', 'slice', 'indexOf', 'lastIndexOf'
  ]);
  all2(grantMutatingMethod, Array.prototype, [
    'pop', 'push', 'reverse', 'shift', 'splice', 'unshift'
  ]);
  handleGenericMethod(Array.prototype, 'sort',
                      function sortHandler(comparator) {
    if (isFrozen(this)) {
      fail("Can't sort a frozen array.");
    }
    if (comparator) {
      return Array.prototype.sort.call(this, toFunc(comparator));
    } else {
      return Array.prototype.sort.call(this);
    }
  });

  /// String

  markCtor(String, Object, 'String');
  grantFunc(String, 'fromCharCode');
  grantToString(String.prototype);
  all2(grantTypedMethod, String.prototype, [
    'indexOf', 'lastIndexOf'
  ]);
  all2(grantGenericMethod, String.prototype, [
    'charAt', 'charCodeAt', 'concat',
    'localeCompare', 'slice', 'substr', 'substring',
    'toLowerCase', 'toLocaleLowerCase', 'toUpperCase', 'toLocaleUpperCase'
  ]);

  handleGenericMethod(String.prototype, 'match',
                      function matchHandler(regexp) {
    enforceMatchable(regexp);
    return this.match(regexp);
  });
  handleGenericMethod(String.prototype, 'replace',
                      function replaceHandler(searcher, replacement) {
    enforceMatchable(searcher);
    if (isFunc(replacement)) {
      replacement = asFunc(replacement);
    } else if (isPseudoFunc(replacement)) {
      replacement = toFunc(replacement);
    } else {
      replacement = '' + replacement;
    }
    return this.replace(searcher, replacement);
  });
  handleGenericMethod(String.prototype, 'search',
                      function searchHandler(regexp) {
    enforceMatchable(regexp);
    return this.search(regexp);
  });
  handleGenericMethod(String.prototype, 'split',
                      function splitHandler(separator, limit) {
    enforceMatchable(separator);
    return this.split(separator, limit);
  });

  /// Boolean

  markCtor(Boolean, Object, 'Boolean');
  grantToString(Boolean.prototype);

  /// Number

  markCtor(Number, Object, 'Number');
  all2(grantRead, Number, [
    'MAX_VALUE', 'MIN_VALUE', 'NaN',
    'NEGATIVE_INFINITY', 'POSITIVE_INFINITY'
  ]);
  grantToString(Number.prototype);
  all2(grantTypedMethod, Number.prototype, [
    'toLocaleString', 'toFixed', 'toExponential', 'toPrecision'
  ]);

  /// Date

  markCtor(Date, Object, 'Date');
  grantFunc(Date, 'parse');
  grantFunc(Date, 'UTC');
  grantToString(Date.prototype);
  all2(grantTypedMethod, Date.prototype, [
    'toDateString','toTimeString', 'toUTCString',
    'toLocaleString', 'toLocaleDateString', 'toLocaleTimeString',
    'toISOString', 'toJSON',
    'getDay', 'getUTCDay', 'getTimezoneOffset',

    'getTime', 'getFullYear', 'getUTCFullYear', 'getMonth', 'getUTCMonth',
    'getDate', 'getUTCDate', 'getHours', 'getUTCHours',
    'getMinutes', 'getUTCMinutes', 'getSeconds', 'getUTCSeconds',
    'getMilliseconds', 'getUTCMilliseconds'
  ]);
  all2(grantMutatingMethod, Date.prototype, [
    'setTime', 'setFullYear', 'setUTCFullYear', 'setMonth', 'setUTCMonth',
    'setDate', 'setUTCDate', 'setHours', 'setUTCHours',
    'setMinutes', 'setUTCMinutes', 'setSeconds', 'setUTCSeconds',
    'setMilliseconds', 'setUTCMilliseconds'
  ]);

  /// RegExp

  markCtor(RegExp, Object, 'RegExp');
  grantToString(RegExp.prototype);
  handleGenericMethod(RegExp.prototype, 'exec',
                      function execHandler(specimen) {
    if (isFrozen(this)) {
      fail("Can't .exec a frozen RegExp");
    }
    specimen = String(specimen); // See bug 528
    return this.exec(specimen);
  });
  handleGenericMethod(RegExp.prototype, 'test',
                      function testHandler(specimen) {
    if (isFrozen(this)) {
      fail("Can't .test a frozen RegExp");
    }
    specimen = String(specimen); // See bug 528
    return this.test(specimen);
  });

  all2(grantRead, RegExp.prototype, [
    'source', 'global', 'ignoreCase', 'multiline', 'lastIndex'
  ]);

  /// errors

  markCtor(Error, Object, 'Error');
  grantToString(Error.prototype);
  grantRead(Error.prototype, 'name');
  grantRead(Error.prototype, 'message');
  markCtor(EvalError, Error, 'EvalError');
  markCtor(RangeError, Error, 'RangeError');
  markCtor(ReferenceError, Error, 'ReferenceError');
  markCtor(SyntaxError, Error, 'SyntaxError');
  markCtor(TypeError, Error, 'TypeError');
  markCtor(URIError, Error, 'URIError');


  var sharedImports;

  ////////////////////////////////////////////////////////////////////////
  // Module loading
  ////////////////////////////////////////////////////////////////////////

  var myNewModuleHandler;

  /**
   * Gets the current module handler.
   */
  function getNewModuleHandler() {
    return myNewModuleHandler;
  }

  /**
   * Registers a new-module-handler, to be called back when a new
   * module is loaded.
   * <p>
   * This callback mechanism is provided so that translated Cajita
   * modules can be loaded from a trusted site with the
   * &lt;script&gt; tag, which runs its script as a statement, not
   * an expression. The callback is of the form
   * <tt>newModuleHandler.handle(newModule)</tt>.
   */
  function setNewModuleHandler(newModuleHandler) {
    myNewModuleHandler = newModuleHandler;
  }

  /**
   * A new-module-handler which returns the new module without
   * instantiating it.
   */
  var obtainNewModule = freeze({
    handle: markFuncFreeze(function handleOnly(newModule){ return newModule; })
  });

  function registerClosureInspector(module) {
    if (this && this.CLOSURE_INSPECTOR___ 
        && this.CLOSURE_INSPECTOR___.supportsCajaDebugging) {
      this.CLOSURE_INSPECTOR___.registerCajaModule(module);
    }
  }

  /**
   * Makes and returns a fresh "normal" module handler whose imports
   * are initialized to a copy of the sharedImports.
   * <p>
   * This handles a new module by calling it, passing it the imports
   * object held in this handler. Successive modules handled by the
   * same "normal" handler thereby see a simulation of successive
   * updates to a shared global scope.
   */
  function makeNormalNewModuleHandler() {
    var imports = void 0;
    var lastOutcome = void 0;
    function getImports() {
      if (!imports) { imports = copy(sharedImports); }
      return imports;
    }
    return freeze({
      getImports: markFuncFreeze(getImports),
      setImports: markFuncFreeze(function setImports(newImports) {
        imports = newImports;
      }),

      /**
       * An outcome is a pair of a success flag and a value.
       * <p>
       * If the success flag is true, then the value is the normal
       * result of calling the module function. If the success flag is
       * false, then the value is the thrown error by which the module
       * abruptly terminated.
       * <p>
       * An html page is cajoled to a module that runs to completion,
       * but which reports as its outcome the outcome of its last
       * script block. In order to reify that outcome and report it
       * later, the html page initializes moduleResult___ to
       * NO_RESULT, the last script block is cajoled to set
       * moduleResult___ to something other than NO_RESULT on success
       * but to call handleUncaughtException() on
       * failure, and the html page returns moduleResult___ on
       * completion. handleUncaughtException() records a failed
       * outcome. This newModuleHandler's handle() method will not
       * overwrite an already reported outcome with NO_RESULT, so the
       * last script-block's outcome will be preserved.
       */
      getLastOutcome: markFuncFreeze(function getLastOutcome() {
        return lastOutcome;
      }),

      /**
       * If the last outcome is a success, returns its value;
       * otherwise <tt>undefined</tt>.
       */
      getLastValue: markFuncFreeze(function getLastValue() {
        if (lastOutcome && lastOutcome[0]) {
          return lastOutcome[1];
        } else {
          return void 0;
        }
      }),

      /**
       * Runs the newModule's module function.
       * <p>
       * Updates the last outcome to report the module function's
       * reported outcome. Propagate this outcome by terminating in
       * the same manner.
       */
      handle: markFuncFreeze(function handle(newModule) {
        registerClosureInspector(newModule);
        var outcome = void 0;
        try {
          var result = newModule.instantiate(___, getImports());
          if (result !== NO_RESULT) {
            outcome = [true, result];
          }
        } catch (ex) {
          outcome = [false, ex];
        }
        lastOutcome = outcome;
        if (outcome) {
          if (outcome[0]) {
            return outcome[1];
          } else {
            throw outcome[1];
          }
        } else {
          return void 0;
        }
      }),

      /**
       * This emulates HTML5 exception handling for scripts as discussed at
       * http://code.google.com/p/google-caja/wiki/UncaughtExceptionHandling
       * and see HtmlCompiler.java for the code that calls this.
       * @param exception a raw exception.  Since {@code throw} can raise any
       *   value, exception could be any value accessible to cajoled code, or
       *   any value thrown by an API imported by cajoled code.
       * @param onerror the value of the raw reference "onerror" in top level
       *   cajoled code.  This will likely be undefined much of the time, but
       *   could be anything.  If it is a func, it can be called with
       *   three strings (message, source, lineNum) as the
       *   {@code window.onerror} event handler.
       * @param {string} source a URI describing the source file from which the
       *   error originated.
       * @param {string} lineNum the approximate line number in source at which
       *   the error originated.
       */
      handleUncaughtException: function handleUncaughtException(exception,
                                                                onerror,
                                                                source,
                                                                lineNum) {
        lastOutcome = [false, exception];

        // Cause exception to be rethrown if it is uncatchable.
        tameException(exception);

        var message = 'unknown';
        if ('object' === typeOf(exception) && exception !== null) {
          message = String(exception.message || exception.desc || message);
        }

        // If we wanted to provide a hook for containers to get uncaught
        // exceptions, it would go here before onerror is invoked.

        // See the HTML5 discussion for the reasons behind this rule.
        if (isPseudoFunc(onerror)) { onerror = toFunc(onerror); }
        var shouldReport = (
            isFunc(onerror)
            ? onerror.CALL___(message, String(source), String(lineNum))
            : onerror !== null);
        if (shouldReport !== false) {
          log(source + ':' + lineNum + ': ' + message);
        }
      }
    });
  }

  /**
   * Produces a function module given an object literal module
   */
  function prepareModule(module, load) {
    registerClosureInspector(module);
    function theModule(imports) {
      // The supplied 'imports' contain arguments supplied by the caller of the
      // module. We need to add the primordials (Array, Object, ...) to these
      // before invoking the Cajita module.
      var completeImports = copy(sharedImports);
      completeImports.load = load;
      // Copy all properties, including Cajita-unreadable ones since these may
      // be used by privileged code.
      var k;
      for (k in imports) {
        if (hasOwnProp(imports, k)) { completeImports[k] = imports[k]; }
      }
      return module.instantiate(___, primFreeze(completeImports));
    }
    theModule.FUNC___ = 'theModule';

    // Whitelist certain module properties as visible to Cajita code. These
    // are all primitive values that do not allow two Cajita entities with
    // access to the same module object to communicate.
    setStatic(theModule, 'cajolerName', module.cajolerName);
    setStatic(theModule, 'cajolerVersion', module.cajolerVersion);
    setStatic(theModule, 'cajoledDate', module.cajoledDate);
    setStatic(theModule, 'moduleId', module.moduleId);
    // The below is a transitive freeze because includedModules is an array
    // of strings.
    if (!!module.includedModules) {
      setStatic(theModule, 'includedModules',
                ___.freeze(module.includedModules));
    }

    return primFreeze(theModule);
  }

  /**
   * A module is an object literal containing metadata and an
   * <code>instantiate</code> member, which is a plugin-maker function.
   * <p>
   * loadModule(module) marks module's <code>instantiate</code> member as a
   * func, freezes the module, asks the current new-module-handler to handle it
   * (thereby notifying the handler), and returns the new module.
   */
  function loadModule(module) {
    freeze(module);
    markFuncFreeze(module.instantiate);
    return callPub(myNewModuleHandler, 'handle', [module]);
  }

  var registeredImports = [];

  /**
   * Gets or assigns the id associated with this (assumed to be)
   * imports object, registering it so that
   * <tt>getImports(getId(imports)) === imports</tt>.
   * <p>
   * This system of registration and identification allows us to
   * cajole html such as
   * <pre>&lt;a onmouseover="alert(1)"&gt;Mouse here&lt;/a&gt;</pre>
   * into html-writing JavaScript such as<pre>
   * ___IMPORTS___.document.innerHTML = "
   *  &lt;a onmouseover=\"
   *    (function(___IMPORTS___) {
   *      ___IMPORTS___.alert(1);
   *    })(___.getImports(" + ___.getId(___IMPORTS___) + "))
   *  \"&gt;Mouse here&lt;/a&gt;
   * ";
   * </pre>
   * If this is executed by a plugin whose imports is assigned id 42,
   * it generates html with the same meaning as<pre>
   * &lt;a onmouseover="___.getImports(42).alert(1)"&gt;Mouse here&lt;/a&gt;
   * </pre>
   * <p>
   * An imports is not registered and no id is assigned to it until the
   * first call to <tt>getId</tt>. This way, an imports that is never
   * registered, or that has been <tt>unregister</tt>ed since the last
   * time it was registered, will still be garbage collectable.
   */
  function getId(imports) {
    enforceType(imports, 'object', 'imports');
    var id;
    if ('id___' in imports) {
      id = enforceType(imports.id___, 'number', 'id');
    } else {
      id = imports.id___ = registeredImports.length;
    }
    registeredImports[id] = imports;
    return id;
  }

  /**
   * Gets the imports object registered under this id.
   * <p>
   * If it has been <tt>unregistered</tt> since the last
   * <tt>getId</tt> on it, then <tt>getImports</tt> will fail.
   */
  function getImports(id) {
    var result = registeredImports[enforceType(id, 'number', 'id')];
    if (result === void 0) {
      fail('imports#', id, ' unregistered');
    }
    return result;
  }

  /**
   * If you know that this <tt>imports</tt> no longer needs to be
   * accessed by <tt>getImports</tt>, then you should
   * <tt>unregister</tt> it so it can be garbage collected.
   * <p>
   * After unregister()ing, the id is not reassigned, and the imports
   * remembers its id. If asked for another <tt>getId</tt>, it
   * reregisters itself at its old id.
   */
  function unregister(imports) {
    enforceType(imports, 'object', 'imports');
    if ('id___' in imports) {
      var id = enforceType(imports.id___, 'number', 'id');
      registeredImports[id] = void 0;
    }
  }


  ////////////////////////////////////////////////////////////////////////
  // Guards and Trademarks
  ////////////////////////////////////////////////////////////////////////

  /**
   * The identity function just returns its argument.
   */
  function identity(x) { return x; }

  /**
   * One-arg form is known in scheme as "call with escape
   * continuation" (call/ec), and is the semantics currently 
   * proposed for EcmaScript Harmony's "return to label".
   * 
   * <p>In this analogy, a call to <tt>callWithEjector</tt> emulates a
   * labeled statement. The ejector passed to the <tt>attemptFunc</tt>
   * emulates the label part. The <tt>attemptFunc</tt> itself emulates
   * the statement being labeled. And a call to <tt>eject</tt> with
   * this ejector emulates the return-to-label statement.
   * 
   * <p>We extend the normal notion of call/ec with an
   * <tt>opt_failFunc</tt> in order to give more the sense of a
   * <tt>try/catch</tt> (or similarly, the <tt>escape</tt> special
   * form in E). The <tt>attemptFunc</tt> is like the <tt>try</tt>
   * clause and the <tt>opt_failFunc</tt> is like the <tt>catch</tt>
   * clause. If omitted, <tt>opt_failFunc</tt> defaults to the
   * <tt>identity</tt> function. 
   * 
   * <p><tt>callWithEjector</tt> creates a fresh ejector -- a one
   * argument function -- for exiting from this attempt. It then calls
   * <tt>attemptFunc</tt> passing that ejector as argument. If
   * <tt>attemptFunc</tt> completes without calling the ejector, then
   * this call to <tt>callWithEjector</tt> completes
   * likewise. Otherwise, if the ejector is called with an argument,
   * then <tt>opt_failFunc</tt> is called with that argument. The
   * completion of <tt>opt_failFunc</tt> is then the completion of the
   * <tt>callWithEjector</tt> as a whole.
   * 
   * <p>The ejector stays live until <tt>attemptFunc</tt> is exited,
   * at which point the ejector is disabled. Calling a disabled
   * ejector throws.
   * 
   * <p>In order to emulate the semantics I expect of ES-Harmony's
   * return-to-label and to prevent the reification of the internal
   * token thrown in order to emulate call/ec, <tt>tameException</tt>
   * immediately rethrows this token, preventing Cajita and Valija
   * <tt>catch</tt> clauses from catching it. However,
   * <tt>finally</tt> clauses will still be run while unwinding an
   * ejection. If these do their own non-local exit, that takes
   * precedence over the ejection in progress but leave the ejector
   * live.
   * 
   * <p>Historic note: This was first invented by John C. Reynolds in 
   * <a href="http://doi.acm.org/10.1145/800194.805852"
   * >Definitional interpreters for higher-order programming 
   * languages</a>. Reynold's invention was a special form as in E, 
   * rather than a higher order function as here and in call/ec.
   */
  function callWithEjector(attemptFunc, opt_failFunc) {
    var failFunc = opt_failFunc || identity;
    var disabled = false;
    var token = new Token('ejection');
    token.UNCATCHABLE___ = true;
    var stash = void 0;
    function ejector(result) {
      if (disabled) {
        cajita.fail('ejector disabled');
      } else {
        // don't disable here.
        stash = result;
        throw token;
      }
    }
    markFuncFreeze(ejector);
    try {
      try {
        return callPub(attemptFunc, 'call', [USELESS, ejector]);
      } finally {
        disabled = true;
      }
    } catch (e) {
      if (e === token) {
        return callPub(failFunc, 'call', [USELESS, stash]);
      } else {
        throw e;
      }
    }
  }

  /**
   * Safely invokes <tt>opt_ejector</tt> with <tt>result</tt>.
   * <p>
   * If <tt>opt_ejector</tt> is falsy, disabled, or returns
   * normally, then <tt>eject</tt> throws. Under no conditions does
   * <tt>eject</tt> return normally.
   */
  function eject(opt_ejector, result) {
    if (opt_ejector) {
      callPub(opt_ejector, 'call', [USELESS, result]);
      fail('Ejector did not exit: ', opt_ejector);
    } else {
      fail(result);
    }
  }
  
  /**
   * Internal routine for making a trademark from a table.
   * <p>
   * To untangle a cycle, the guard made by <tt>makeTrademark</tt> is
   * not yet either stamped or frozen. The caller of
   * <tt>makeTrademark</tt> must do both before allowing it to
   * escape. 
   */
  function makeTrademark(typename, table) {
    typename = String(typename);
    return primFreeze({
      toString: markFuncFreeze(function() { return typename + 'Mark'; }),

      stamp: primFreeze({
        toString: markFuncFreeze(function() { return typename + 'Stamp'; }),
        mark___: markFuncFreeze(function(obj) {
          table.set(obj, true);
          return obj;
        })
      }),

      guard: {
        toString: markFuncFreeze(function() { return typename + 'T'; }),
        coerce: markFuncFreeze(function(specimen, opt_ejector) {
          if (table.get(specimen)) { return specimen; }
          eject(opt_ejector,
                'Specimen does not have the "' + typename + '" trademark');
        })
      }
    });
  }

  /**
   * Objects representing guards should be marked as such, so that
   * they will pass the <tt>GuardT</tt> guard.
   * <p>
   * <tt>GuardT</tt> is generally accessible as
   * <tt>cajita.GuardT</tt>. However, <tt>GuardStamp</tt> must not be
   * made generally accessible, but rather only given to code trusted
   * to use it to deem as guards things that act in a guard-like
   * manner: A guard MUST be immutable and SHOULD be idempotent. By
   * "idempotent", we mean that<pre>
   *     var x = g(specimen, ej); // may fail
   *     // if we're still here, then without further failure
   *     g(x) === x
   * </pre>
   */
  var GuardMark = makeTrademark('Guard', newTable(true));
  var GuardT = GuardMark.guard;
  var GuardStamp = GuardMark.stamp;
  primFreeze(GuardStamp.mark___(GuardT));  

  /**
   * The <tt>Trademark</tt> constructor makes a trademark, which is a
   * guard/stamp pair, where the stamp marks and freezes unfrozen
   * records as carrying that trademark and the corresponding guard
   * cerifies objects as carrying that trademark (and therefore as
   * having been marked by that stamp).
   * <p>
   * By convention, a guard representing the type-like concept 'Foo'
   * is named 'FooT'. The corresponding stamp is 'FooStamp'. And the
   * record holding both is 'FooMark'. Many guards also have
   * <tt>of</tt> methods for making guards like themselves but
   * parameterized by further constraints, which are usually other
   * guards. For example, <tt>T.ListT</tt> is the guard representing
   * frozen array, whereas <tt>T.ListT.of(cajita.GuardT)</tt>
   * represents frozen arrays of guards.
   */
  function Trademark(typename) {
    var result = makeTrademark(typename, newTable(true));
    primFreeze(GuardStamp.mark___(result.guard));
    return result;
  }
  markFuncFreeze(Trademark);

  /**
   * First ensures that g is a guard; then does 
   * <tt>g.coerce(specimen, opt_ejector)</tt>.
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
      markFuncFreeze(function(opt_ejector) {
        g.coerce(specimen, opt_ejector);
        return true;
      }),
      markFuncFreeze(function(ignored) {
        return false;
      })
    );
  }

  /**
   * Given that <tt>stamps</tt> is a list of stamps and
   * <tt>record</tt> is a non-frozen record, this marks record with
   * the trademarks of all of these stamps, and then freezes and
   * returns the record.
   * <p>
   * If any of these conditions do not hold, this throws.
   */
  function stamp(stamps, record) {
    if (!isRecord(record)) {
      fail('Can only stamp records: ', record);
    }
    if (isFrozen(record)) {
      fail("Can't stamp frozen objects: ", record);
    }
    var numStamps = stamps.length >>> 0;
    // First ensure that we will succeed before applying any stamps to
    // the record. If we ever extend Cajita with mutating getters, we
    // will need to do more to ensure impossibility of failure after
    // partial stamping.
    for (var i = 0; i < numStamps; i++) {
      if (!('mark___' in stamps[i])) {
        fail("Can't stamp with a non-stamp: ", stamps[i]);
      }
    }
    // Looping again over the same untrusted stamps alleged-array is safe
    // assuming single-threaded execution and non-mutating accessors.
    // If we extend Cajita to allow getters/setters, we'll need to make a 
    // copy of the array above and loop over the copy below.
    for (var i = 0; i < numStamps; i++) {
      // Only works for real stamps, postponing the need for a
      // user-implementable auditing protocol.
      stamps[i].mark___(record);
    }
    return freeze(record);
  }

  ////////////////////////////////////////////////////////////////////////
  // Sealing and Unsealing
  ////////////////////////////////////////////////////////////////////////

  /**
   * Returns a pair of functions such that the seal(x) wraps x in an object
   * so that only unseal can get x back from the object.
   * <p>
   * TODO(erights): The only remaining use as of this writing is
   * in domita for css. Perhaps a refactoring is in order.
   *
   * @return {object} of the form
   *     { seal: function seal(x) { return Token('(box)'); },
   *       unseal: function unseal(box) { return x; } }.
   */
  function makeSealerUnsealerPair() {
    var table = newTable(true);
    var undefinedStandin = {};
    function seal(payload) {
      if (payload === void 0) {
        payload = undefinedStandin;
      }
      var box = Token('(box)');
      table.set(box, payload);
      return box;
    }
    function unseal(box) {
      var payload = table.get(box);
      if (payload === void 0) {
        fail('Sealer/Unsealer mismatch'); 
      } else if (payload === undefinedStandin) {
        return void 0;
      } else {
        return payload;
      }
    }
    return freeze({
      seal: markFuncFreeze(seal),
      unseal: markFuncFreeze(unseal)
    });
  }

  ////////////////////////////////////////////////////////////////////////
  // Needed for Valija
  ////////////////////////////////////////////////////////////////////////

  /**
   * <tt>cajita.construct(ctor, [args...])</tt> invokes a constructor
   * or simple function as a constructor using 'new'.
   */
  function construct(ctor, args) {
    ctor = asCtor(ctor);
    // This works around problems with (new Array()) and (new Date()) where
    // the returned object is not really a Date or Array on SpiderMonkey and
    // other interpreters.
    switch (args.length) {
      case 0:  return new ctor();
      case 1:  return new ctor(args[0]);
      case 2:  return new ctor(args[0], args[1]);
      case 3:  return new ctor(args[0], args[1], args[2]);
      case 4:  return new ctor(args[0], args[1], args[2], args[3]);
      case 5:  return new ctor(args[0], args[1], args[2], args[3], args[4]);
      case 6:  return new ctor(args[0], args[1], args[2], args[3], args[4],
                               args[5]);
      case 7:  return new ctor(args[0], args[1], args[2], args[3], args[4],
                               args[5], args[6]);
      case 8:  return new ctor(args[0], args[1], args[2], args[3], args[4],
                               args[5], args[6], args[7]);
      case 9:  return new ctor(args[0], args[1], args[2], args[3], args[4],
                               args[5], args[6], args[7], args[8]);
      case 10: return new ctor(args[0], args[1], args[2], args[3], args[4],
                               args[5], args[6], args[7], args[8], args[9]);
      case 11: return new ctor(args[0], args[1], args[2], args[3], args[4],
                               args[5], args[6], args[7], args[8], args[9],
                               args[10]);
      case 12: return new ctor(args[0], args[1], args[2], args[3], args[4],
                               args[5], args[6], args[7], args[8], args[9],
                               args[10], args[11]);
      default:
        if (ctor.typeTag___ === 'Array') {
          return ctor.apply(USELESS, args);
        }
        var tmp = function (args) {
          return ctor.apply(this, args);
        };
        tmp.prototype = ctor.prototype;
        return new tmp(args);
    }
  }

  /**
   * Create a unique identification of a given table identity that can
   * be used to invisibly (to Cajita code) annotate a key object to
   * index into a table.
   * <p>
   * magicCount and MAGIC_TOKEN together represent a
   * unique-across-frames value safe against collisions, under the
   * normal Caja threat model assumptions. magicCount and
   * MAGIC_NAME together represent a probably unique across frames
   * value, with which can generate strings in which collision is
   * unlikely but possible.
   * <p>
   * The MAGIC_TOKEN is a unique unforgeable per-Cajita runtime
   * value. magicCount is a per-Cajita counter, which increments each
   * time a new one is needed.
   */
  var magicCount = 0;
  var MAGIC_NUM = Math.random();
  var MAGIC_TOKEN = Token('MAGIC_TOKEN_FOR:' + MAGIC_NUM);
  // Using colons in the below causes it to fail on IE since getting a
  // property whose name contains a colon on a DOM table element causes
  // an exception.
  var MAGIC_NAME = '_index;'+ MAGIC_NUM + ';';

  /**
   * 
   * Creates a new mutable associative table mapping from the
   * identity of arbitrary keys (as defined by tt>identical()</tt>) to
   * arbitrary values.
   * <p>
   * Operates as specified by <a href=
   * "http://wiki.ecmascript.org/doku.php?id=strawman:weak_references#ephemeron_tables"
   * >ephemeron tables</a>, including the "Implementation
   * Considerations" section regarding emulation on ES3, except that,
   * when <tt>opt_useKeyLifetime</tt> is falsy or absent, the keys
   * here may be primitive types as well. 
   * <p>
   * To support Domita, the keys might be host objects.
   */
  function newTable(opt_useKeyLifetime, opt_expectedSize) {
    magicCount++;
    var myMagicIndexName = MAGIC_NAME + magicCount + '___';

    function setOnKey(key, value) {
      var ktype = typeof key;
      if (!key || (ktype !== 'function' && ktype !== 'object')) { 
        fail("Can't use key lifetime on primitive keys: ", key);
      }
      var list = key[myMagicIndexName];
      // To distinguish key from objects that derive from it,
      //    list[0] should be === key
      // For odd positive i,
      //    list[i] is the MAGIC_TOKEN for a Cajita runtime (i.e., a
      //            browser frame in which the Cajita runtime has been
      //            loaded). The myMagicName and the MAGIC_TOKEN
      //            together uniquely identify a table.
      //    list[i+1] is the value stored in that table under this key.
      if (!list || list[0] !== key) {
        key[myMagicIndexName] = [key, MAGIC_TOKEN, value];
      } else {
        var i;
        for (i = 1; i < list.length; i += 2) {
          if (list[i] === MAGIC_TOKEN) { break; }
        }
        list[i] = MAGIC_TOKEN;
        list[i + 1] = value;
      }
    }

    function getOnKey(key) {
      var ktype = typeof key;
      if (!key || (ktype !== 'function' && ktype !== 'object')) { 
        fail("Can't use key lifetime on primitive keys: ", key);
      }
      var list = key[myMagicIndexName];
      if (!list || list[0] !== key) {
        return void 0;
      } else {
        for (var i = 1; i < list.length; i += 2) {
          if (list[i] === MAGIC_TOKEN) { return list[i + 1]; }
        }
        return void 0;
      }
    }

    if (opt_useKeyLifetime) {
      return primFreeze({
        set: markFuncFreeze(setOnKey),
        get: markFuncFreeze(getOnKey)
      });
    }

    var myValues = [];

    function setOnTable(key, value) {
      var index;
      switch (typeof key) {
        case 'object':
        case 'function': {
          if (null === key) { myValues.prim_null = value; return; }
          index = getOnKey(key);
          if (value === void 0) {
            if (index === void 0) {
              return;
            } else {
              setOnKey(key, void 0);
            }
          } else {
            if (index === void 0) {
              index = myValues.length;
              setOnKey(key, index);
            }
          }
          break;
        }
        case 'string': {
          index = 'str_' + key;
          break;
        }
        default: { 
          index = 'prim_' + key;
          break; 
        }
      }
      if (value === void 0) {
        // TODO(erights): Not clear that this is the performant
        // thing to do when index is numeric and < length-1.
        delete myValues[index];
      } else {
        myValues[index] = value;
      }
    }

    /**
     * If the key is absent, returns <tt>undefined</tt>.
     * <p>
     * Users of this table cannot distinguish an <tt>undefined</tt>
     * value from an absent key.
     */
    function getOnTable(key) {
      switch (typeof key) {
        case 'object':
        case 'function': {
          if (null === key) { return myValues.prim_null; }
          var index = getOnKey(key);
          if (void 0 === index) { return void 0; }
          return myValues[index];
        }
        case 'string': { return myValues['str_' + key]; }
        default: { return myValues['prim_' + key]; }
      }
    }

    return primFreeze({
      set: markFuncFreeze(setOnTable),
      get: markFuncFreeze(getOnTable)
    });
  }


  /**
   * Is <tt>allegedParent</tt> on <obj>'s prototype chain?
   * <p>
   * Although in raw JavaScript <tt>'foo' instanceof String</tt> is
   * false, to reduce the differences between primitives and their
   * wrappers, <tt>inheritsFrom('foo', String.prototype)</tt> is true.
   */
  function inheritsFrom(obj, allegedParent) {
    if (null === obj) { return false; }
    if (void 0 === obj) { return false; }
    if (typeOf(obj) === 'function') { return false; }
    if (typeOf(allegedParent) !== 'object') { return false; }
    if (null === allegedParent) { return false; }
    function F() {}
    F.prototype = allegedParent;
    return Object(obj) instanceof F;
  }

  /**
   * Return func.prototype's directConstructor.
   * <p>
   * When following the "classical" inheritance pattern (simulating
   * class-style inheritance as a pattern of prototypical
   * inheritance), func may represent (the constructor of) a class; in
   * which case getSuperCtor() returns (the constructor of) its
   * immediate superclass.
   */
  function getSuperCtor(func) {
    enforceType(func, 'function');
    if (isCtor(func) || isFunc(func)) {
      var result = directConstructor(func.prototype);
      if (isCtor(result) || isFunc(result)) {
        return result;
      }
    }
    return void 0;
  }

  var attribute = new RegExp(
      '^([\\s\\S]*)_(?:canRead|canCall|getter|handler)___$');

  /**
   * Returns a list of all cajita-readable own properties, whether or
   * not they are cajita-enumerable.
   */
  function getOwnPropertyNames(obj) {
    var result = [];
    var seen = {};
    // TODO(erights): revisit once we do ES5ish attribute control.
    var implicit = isJSONContainer(obj);
    for (var k in obj) {
      if (hasOwnProp(obj, k)) {
        if (implicit && !endsWith__.test(k)) {
          if (!myOriginalHOP.call(seen, k)) {
            seen[k] = true;
            result.push(k);
          }
        } else {
          var match = attribute.exec(k);
          if (match !== null) {
            var base = match[1];
            if (!myOriginalHOP.call(seen, base)) {
              seen[base] = true;
              result.push(base);
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * Return the names of the accessible own properties of
   * func.prototype.
   * <p>
   * Since prototypical objects are not themselves accessible in
   * Cajita, this means in effect: the properties contributed by
   * func.prototype that would be accessible on objects that inherit
   * from func.prototype.
   */
  function getProtoPropertyNames(func) {
    enforceType(func, 'function');
    return getOwnPropertyNames(func.prototype);
  }

  /**
   * Return the value associated with func.prototype[name].
   * <p>
   * Since prototypical objects are not themselves accessible in
   * Cajita, this means in effect: If x inherits name from
   * func.prototype, what would the value of x[name] be? If the value
   * associated with func.prototype[name] is an exophoric function
   * (resulting from taming a generic method), then return the
   * corresponding pseudo-function.
   */
  function getProtoPropertyValue(func, name) {
    return asFirstClass(readPub(func.prototype, name));
  }

  /**
   * Like primBeget(), but applicable only to records.
   */
  function beget(parent) {
    if (!isRecord(parent)) {
      fail('Can only beget() records: ', parent);
    }
    var result = primBeget(parent);
    result.RECORD___ = result;
    return result;
  }

  ////////////////////////////////////////////////////////////////////////
  // JSON
  ////////////////////////////////////////////////////////////////////////

  function jsonParseOk(json) {
    try {
      var x = json.parse('{"a":3}');
      return x.a === 3;
    } catch (e) {
      return false;
    }
  }

  function jsonStringifyOk(json) {
    try {
      var x = json.stringify({"a":3, "b__":4}, function replacer(k, v) {
        return (/__$/.test(k) ? void 0 : v);
      });
      return x === '{"a":3}';
    } catch (e) {
      return false;
    }
  }

  var goodJSON = {};
  goodJSON.parse = jsonParseOk(global.JSON) ?
    global.JSON.parse : json_sans_eval.parse;
  goodJSON.stringify = jsonStringifyOk(global.JSON) ?
    global.JSON.stringify : json_sans_eval.stringify;

  safeJSON = primFreeze({
    CLASS___: 'JSON',
    parse: markFuncFreeze(function (text, opt_reviver) {
      var reviver = void 0;
      if (opt_reviver) {
        opt_reviver = toFunc(opt_reviver);
        reviver = function (key, value) {
          return opt_reviver.apply(this, arguments);
        };
      }
      return goodJSON.parse(
          json_sans_eval.checkSyntax(text, function (key) {
            return (key !== 'valueOf' && key !== 'toString'
                    && !endsWith__.test(key));
          }), reviver);
    }),
    stringify: markFuncFreeze(function (obj, opt_replacer, opt_space) {
      switch (typeof opt_space) {
        case 'number': case 'string': case 'undefined': break;
        default: throw new TypeError('space must be a number or string');
      }
      var replacer;
      if (opt_replacer) {
        opt_replacer = toFunc(opt_replacer);
        replacer = function (key, value) {
          if (!canReadPub(this, key)) { return void 0; }
          return opt_replacer.apply(this, arguments);
        };
      } else {
        replacer = function (key, value) {
          return (canReadPub(this, key)) ? value : void 0;
        };
      }
      return goodJSON.stringify(obj, replacer, opt_space);
    })
  });

  ////////////////////////////////////////////////////////////////////////
  // Exports
  ////////////////////////////////////////////////////////////////////////

  cajita = {
    // Diagnostics and condition enforcement
    log: log,
    fail: fail,
    enforce: enforce,
    enforceType: enforceType,

    // walking prototype chain, checking JSON containers
    directConstructor: directConstructor,
    getFuncCategory: getFuncCategory,
    isDirectInstanceOf: isDirectInstanceOf,
    isInstanceOf: isInstanceOf,
    isRecord: isRecord,           isArray: isArray,
    isJSONContainer: isJSONContainer,
    freeze: freeze,               isFrozen: isFrozen,
    copy: copy,                   snapshot: snapshot,

    // Accessing properties
    canReadPub: canReadPub,       readPub: readPub,
    hasOwnPropertyOf: hasOwnPropertyOf,
                                  readOwn: readOwn,
    canEnumPub: canEnumPub,
    canEnumOwn: canEnumOwn,
    canInnocentEnum: canInnocentEnum,
    BREAK: BREAK,
    allKeys: allKeys,             forAllKeys: forAllKeys,
    ownKeys: ownKeys,             forOwnKeys: forOwnKeys,
    canCallPub: canCallPub,       callPub: callPub,
    canSetPub: canSetPub,         setPub: setPub,
    canDeletePub: canDeletePub,   deletePub: deletePub,

    // Object indistinguishability and object-keyed tables
    Token: Token,
    identical: identical,
    newTable: newTable,

    // Guards and Trademarks
    identity: identity,
    callWithEjector: callWithEjector,
    eject: eject,
    GuardT: GuardT,
    Trademark: Trademark,
    guard: guard,
    passesGuard: passesGuard,
    stamp: stamp,

    // Sealing & Unsealing
    makeSealerUnsealerPair: makeSealerUnsealerPair,

    // Other
    USELESS: USELESS,
    manifest: manifest,

    // Needed for Valija
    construct: construct,
    inheritsFrom: inheritsFrom,
    getSuperCtor: getSuperCtor,
    getOwnPropertyNames: getOwnPropertyNames,
    getProtoPropertyNames: getProtoPropertyNames,
    getProtoPropertyValue: getProtoPropertyValue,
    beget: beget,

    PseudoFunctionProto: PseudoFunctionProto,
    PseudoFunction: PseudoFunction,
    isPseudoFunc: isPseudoFunc,

    // deprecated
    enforceNat: deprecate(enforceNat, '___.enforceNat',
                          'Use (x === x >>> 0) instead as a UInt32 test')
  };

  forOwnKeys(cajita, markFuncFreeze(function(k, v) {
    switch (typeOf(v)) {
      case 'object': {
        if (v !== null) { primFreeze(v); }
        break;
      }
      case 'function': {
        markFuncFreeze(v);
        break;
      }
    }
  }));

  sharedImports = {
    cajita: cajita,

    'null': null,
    'false': false,
    'true': true,
    'NaN': NaN,
    'Infinity': Infinity,
    'undefined': void 0,
    parseInt: markFuncFreeze(parseInt),
    parseFloat: markFuncFreeze(parseFloat),
    isNaN: markFuncFreeze(isNaN),
    isFinite: markFuncFreeze(isFinite),
    decodeURI: markFuncFreeze(decodeURI),
    decodeURIComponent: markFuncFreeze(decodeURIComponent),
    encodeURI: markFuncFreeze(encodeURI),
    encodeURIComponent: markFuncFreeze(encodeURIComponent),
    escape: escape ? markFuncFreeze(escape) : (void 0),
    Math: Math,
    JSON: safeJSON,

    Object: Object,
    Array: Array,
    String: String,
    Boolean: Boolean,
    Number: Number,
    Date: Date,
    RegExp: RegExp,

    Error: Error,
    EvalError: EvalError,
    RangeError: RangeError,
    ReferenceError: ReferenceError,
    SyntaxError: SyntaxError,
    TypeError: TypeError,
    URIError: URIError
  };

  forOwnKeys(sharedImports, markFuncFreeze(function(k, v) {
    switch (typeOf(v)) {
      case 'object': {
        if (v !== null) { primFreeze(v); }
        break;
      }
      case 'function': {
        primFreeze(v);
        break;
      }
    }
  }));
  primFreeze(sharedImports);

  ___ = {
    // Diagnostics and condition enforcement
    getLogFunc: getLogFunc,
    setLogFunc: setLogFunc,

    primFreeze: primFreeze,

    // Accessing property attributes.
    canRead: canRead,             grantRead: grantRead,
    canEnum: canEnum,             grantEnum: grantEnum,
    canCall: canCall,        
    canSet: canSet,               grantSet: grantSet,
    canDelete: canDelete,         grantDelete: grantDelete,

    // Module linkage
    readImport: readImport,

    // Classifying functions
    isCtor: isCtor,
    isFunc: isFunc,
    markCtor: markCtor,           extend: extend,
    markFuncFreeze: markFuncFreeze,
    markXo4a: markXo4a,           markInnocent: markInnocent,
    asFunc: asFunc,               toFunc: toFunc,

    // Accessing properties
    inPub: inPub,
    canSetStatic: canSetStatic,   setStatic: setStatic,

    // Other
    typeOf: typeOf,
    hasOwnProp: hasOwnProp,
    args: args,
    deleteFieldEntirely: deleteFieldEntirely,
    tameException: tameException,
    primBeget: primBeget,
    callStackUnsealer: callStackUnsealer,
    RegExp: RegExp,  // Available to rewrite rule w/o risk of masking
    GuardStamp: GuardStamp,
    asFirstClass: asFirstClass,
    initializeMap: initializeMap,
    iM: initializeMap,

    // Taming mechanism
    useGetHandler: useGetHandler,
    useSetHandler: useSetHandler,

    grantFunc: grantFunc,
    grantGenericMethod: grantGenericMethod,
    handleGenericMethod: handleGenericMethod,
    grantTypedMethod: grantTypedMethod,
    grantMutatingMethod: grantMutatingMethod,
    grantInnocentMethod: grantInnocentMethod,

    enforceMatchable: enforceMatchable,
    all2: all2,

    tamesTo: tamesTo,
    tamesToSelf: tamesToSelf,
    tame: tame,
    untame: untame,

    // Module loading
    getNewModuleHandler: getNewModuleHandler,
    setNewModuleHandler: setNewModuleHandler,
    obtainNewModule: obtainNewModule,
    makeNormalNewModuleHandler: makeNormalNewModuleHandler,
    loadModule: loadModule,
    prepareModule: prepareModule,
    NO_RESULT: NO_RESULT,

    getId: getId,
    getImports: getImports,
    unregister: unregister,

    // Deprecated
    grantEnumOnly: deprecate(grantEnum, '___.grantEnumOnly',
                             'Use ___.grantEnum instead.'),
    grantCall: deprecate(grantGenericMethod, '___.grantCall',
                         'Choose a method tamer (e.g., ___.grantFunc,' +
                         '___.grantGenericMethod,etc) according to the ' +
                         'safety properties of calling and reading the ' +
                         'method.'),
    grantGeneric: deprecate(grantGenericMethod, '___.grantGeneric',
                            'Use ___.grantGenericMethod instead.'),
    useApplyHandler: deprecate(useApplyHandler, '___.useApplyHandler',
                               'Use ___.handleGenericMethod instead.'),
    useCallHandler: deprecate(useCallHandler, '___.useCallHandler',
                              'Use ___.handleGenericMethod instead.'),
    handleGeneric: deprecate(useCallHandler, '___.handleGeneric',
                              'Use ___.handleGenericMethod instead.'),
    grantTypedGeneric: deprecate(useCallHandler, '___.grantTypedGeneric',
                              'Use ___.grantTypedMethod instead.'),
    grantMutator: deprecate(useCallHandler, '___.grantMutator',
                              'Use ___.grantMutatingMethod instead.'),
    useDeleteHandler: deprecate(useDeleteHandler, '___.useDeleteHandler',
                                'Refactor to avoid needing to handle ' +
                                'deletions.'),
    isXo4aFunc: deprecate(isXo4aFunc, '___.isXo4aFunc',
                          'Refactor to avoid needing to dynamically test ' +
                          'whether a function is marked exophoric.'),
    xo4a: deprecate(markXo4a, '___.xo4a',
                    'Consider refactoring to avoid needing to explicitly ' +
                    'mark a function as exophoric. Use one of the exophoric ' +
                    'method tamers (e.g., ___.grantGenericMethod) instead.' +
                    'Otherwise, use ___.markXo4a instead.'),
    ctor: deprecate(markCtor, '___.ctor',
                    'Use ___.markCtor instead.'),
    func: deprecate(markFuncFreeze, '___.func',
                    '___.func should not be called ' +
                    'from manually written code.'),
    frozenFunc: deprecate(markFuncFreeze, '___.frozenFunc',
                          'Use ___.markFuncFreeze instead.'),
    markFuncOnly: deprecate(markFuncFreeze, '___.markFuncOnly',
                            '___.markFuncOnly should not be called ' +
                            'from manually written code.'),

    // Taming decisions
    sharedImports: sharedImports
  };

  forOwnKeys(cajita, markFuncFreeze(function(k, v) {
    if (k in ___) {
      fail('internal: initialization conflict: ', k);
    }
    if (typeOf(v) === 'function') {
      grantFunc(cajita, k);
    }
    ___[k] = v;
  }));
  setNewModuleHandler(makeNormalNewModuleHandler());
})(this);
