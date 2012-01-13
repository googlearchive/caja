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
 * @fileoverview the ES5/3 runtime library.
 *
 * <p>It is written in Javascript, not ES5, and would be rejected by the
 * ES53Rewriter. This module exports two globals intended for normal use:<ol>
 * <li>"___" for use by the output of the ES53Rewriter and by some
 *     other untranslated Javascript code.
 * <li>"es53" providing some common services to the ES5/3 programmer.
 * </ol>
 * <p>It also exports the following additional globals:<ul>
 * <li>"safeJSON" why is this being exported?
 * </ul>
 *
 * @author metaweta@gmail.com
 * @requires json_sans_eval, cajaBuildVersion, taming
 * @provides ___, safeJSON, WeakMap, cajaVM
 * @overrides Array, Boolean, Date, Function, Number, Object, RegExp, String
 * @overrides Error, EvalError, RangeError, ReferenceError, SyntaxError,
 *   TypeError, URIError, ArrayLike
 * @overrides escape, JSON, Proxy
 */

var ___, cajaVM, safeJSON, WeakMap, ArrayLike, Proxy;

(function () {
  // For computing the [[Class]] internal property
  var classProp = Object.prototype.toString;
  var gopd = Object.getOwnPropertyDescriptor;
  var defProp = Object.defineProperty;
  
  // Given an object defined in an es53 frame, we can tell which
  // Object.prototype it inherits from.
  Object.prototype.baseProto___ = Object.prototype;

  var slice = Array.prototype.slice; 
  var push = Array.prototype.push;


  // Workarounds for FF2 and FF3.0 for
  // https://bugzilla.mozilla.org/show_bug.cgi?id=507453

  var antidote = function() { return void 0; };
  function deodorize(original, end) {
    if (original.__defineGetter__) {
      for (var i = end; i < 0; ++i) {
        original.__defineGetter__(i, antidote);
      }
    }
  }

  function isDeodorized(original, sprop) {
    if (original.__lookupGetter__) {
      return original.__lookupGetter__(sprop) === antidote;
    }
    return false;
  }

  // Blacklist built from:
  // http://www.thespanner.co.uk/2009/07/14/hidden-firefox-properties-revisited/
  // [args, actuals length, callee, formals length, func name, caller]
  deodorize(Function.prototype, -6);

  // [string length]
  deodorize(String.prototype, -1);

  // [source, global, ignore case, last index, multiline, sticky]
  deodorize(RegExp.prototype, -6);

  // [input, multiline, last match, last capture, lcontext, rcontext]
  deodorize(RegExp, -6);

  /**
   * Caja-specific properties
   *
   * Caja combines all numeric properties and uses the special name
   * {@code NUM___} to refer to the single property descriptor.
   * Numeric properties must be enumerable data properties.
   * If the special descriptor is absent, it defaults to
   * {writable:true, configurable:true, enumerable:true, get:void 0, set:void 0}
   *
   * Note that each of the six attributes starts with a different letter.
   * Each property has eight associated properties: six for the attributes
   * and two for writable and callable fastpath flags
   *
   * {@code obj[name + '_v___'] === obj}  means that {@code name} is
   *                                      a data property on {@code obj}.
   * {@code obj.hasOwnProperty(name + '_v___') &&
   *       obj[name + '_v___'] === false} means that {@code name} is an
   *                                      accessor property on {@code obj}.
   * {@code obj[name + '_w___'] === obj}  means that {@code name} is
   *                                      writable (fastpath).
   * {@code obj[name + '_gw___'] === obj} means that {@code name} is
   *                                      writable (grant).
   * {@code obj[name + '_c___'] === obj}  means that {@code name} is
   *                                      configurable.
   * {@code obj[name + '_e___'] === obj}  means that {@code name} is
   *                                      enumurable.
   * {@code obj[name + '_g___']}          is the getter for
   *                                      {@code name} on {@code obj}.
   * {@code obj[name + '_s___']}          is the setter for
   *                                      {@code name} on {@code obj}.
   * {@code obj[name + '_m___'] truthy}   means that {@code name} is
   *                                      callable as a method (fastpath).
   *
   * To prevent accidental misinterpretation of the above inherited
   * attribute descriptors, whenever any are defined for a given
   * {@code obj} and {@code name}, all eight must be. If {@code name}
   * is a string encoding of a number (i.e., where {@code name ===
   * String(+name)}), then all of the above attributes must not be
   * defined directly for {@code name}. Instead, the effective
   * attributes of {@code name} are covered by the actual attributes
   * of {@code 'NUM___'}.
   *
   * Another property suffix commonly used in the code is for virtualized
   * methods; since innocent code and existing host code like domita rely
   * on the original bindings of primordial methods, guest code should not
   * be allowed to change the original bindings; {@code virtualize} installs
   * ES5 getters and setters that store the guest view of the property.
   *
   * {@code obj[name + '_virt___']}       is the virtual version of a primordial
   *                                      method that's exposed to guest code.
   *
   * Per-object properties:
   *
   * {@code obj.ne___ === obj}            means that {@code obj} is not
   *                                      extensible.
   * {@code obj.z___ === obj}             means that {@code obj} is frozen.
   * {@code obj.proxy___ === obj}         means that obj is a proxy.
   * {@code '___' in obj}                 means that {@code obj} is a global
   *                                      object and shouldn't be used as
   *                                      'this'.
   *
   * {@code obj.v___(p)}                  = {@code obj.[[Get]](p)}
   * {@code obj.w___(p,v)}                = {@code obj.[[Put]](p,v)}
   * {@code obj.c___(p)}                  = {@code obj.[[Delete]](p)}
   * {@code obj.m___(p, [as])}            invokes {@code p} as a method safely;
   *                                      it may set the {@code '_m___'}
   *                                      fastpath on {@code obj}.
   * {@code obj.e___()}                   returns an object whose enumerable
   *                                      keys are the ones to iterate over.
   * {@code obj.freeze___()}              freezes the object; on proxies invokes
   *                                      the fix handler.
   * {@code obj.pe___()}                  prevents the object from being
   *                                      extended; on proxies it invokes the
   *                                      fix handler.
   * {@code obj.seal___()}                prevents the object from being
   *                                      extended and makes all own properties
   *                                      nonconfigurable; on proxies it invokes
   *                                      the fix handler.
   * {@code obj.keys___()}                returns the list of enumerable own
   *                                      properties.
   * {@code obj.ownKeys___()}             returns the list of all own
   *                                      properties.
   * {@code obj.allKeys___()}             returns the list of all properties.
   *
   * {@code g.f___(dis, [as])}            is the tamed version of {@code g},
   *                                      though it uses {@code apply}'s
   *                                      interface.
   * {@code g.i___(as)}                   = g.f___(USELESS, [as])
   * {@code g.new___(as)}                 is the tamed version of {@code g}
   *                                      used for constructing an object of
   *                                      class {@code g}.
   * {@code g.ok___ === true}             means g is non-toxic.
   * {@code ___.tamesTo(feral, tamed)}    installs inverse properties
   *                                      {@code feral.TAMED_TWIN___ = tamed},
   *                                      {@code tamed.FERAL_TWIN___ = feral}.
   * {@code ___.tame(obj)}                uses the {@code *_TWIN___} fastpath.
   *                                      if possible; if that fails, it invokes
   *                                      explicit taming functions.
   * {@code ___.untame(obj)}              is similar, but goes the other way.
   *
   * Since creating function instances is a common pattern and reading
   * properties of a function instance is not, we defer whitelisting the
   * prototype, length, and name properties.
   *
   * {@code f.name___}                    holds the value of the deferred name
   *                                      property of a function instance until
   *                                      it's installed.
   */

  // We have to define it even on Firefox, since the built-in slice doesn't
  // throw when given null or undefined.
  Array.slice = markFunc(function (dis, startIndex) { // , endIndex
      dis = ToObject(dis);
      if (arguments.length > 2) {
        var endIndex = arguments[2];
        return slice.call(dis, startIndex, endIndex);
      } else {
        return slice.call(dis, startIndex);
      }
    });

  // Missing on IE
  if (!Array.prototype.forEach) {
    Array.prototype.forEach = function(fun) { //, thisp
      var dis = ToObject(this);
      var len = dis.length >>> 0;
      if ('function' !== typeof fun) {
        throw new TypeError("Expected function but got " + (typeof fun));
      }

      var thisp = arguments[1];
      for (var i = 0; i < len; i++) {
        if (i in dis) {
          fun.call(thisp, dis[i], i, dis);
        }
      }
    };
  }

  var hasOwnProperty = Object.prototype.hasOwnProperty;
  // In IE<9, this is an approximation of Object.keys, because a few own
  // properties like 'toString' are never enumerable.
  var fastOwnKeys = Object.keys || function (o) {
    var keys = [];
    for (var k in o) {
      if (hasOwnProperty.call(o, k)) { keys.push(k); }
    }
    return keys;
  };

  Object.prototype.freeze___ = function freeze___() {
      // Frozen means all the properties are neither writable nor
      // configurable, and the object itself is not extensible.
      // Cajoled setters that change properties of the object will
      // fail like any other attempt to change the properties.
      // Tamed setters should check before changing a property.
      if (this.z___ === this) { return this; }
      // Allow function instances to install their instance properties
      // before freezing them.
      if (this.v___ === deferredV) {
        this.v___('length');
      }
      var keys = fastOwnKeys(this);
      for (var k = 0, n = keys.length; k < n; k++) {
        var i = keys[k];
        if (i.length <= 5 || i.substr(i.length - 5) !== '_v___') { continue; }
        var P = i.substr(0, i.length - 5);
        this[P + '_c___'] = false;
        this[P + '_gw___'] = false;
        this[P + '_w___'] = false;
      }
      // inline this.hasNumerics___()
      if (!this.NUM____v___ === this) {
        this.NUM____v___ = this;
        this.NUM____e___ = this;
        this.NUM____g___ = void 0;
        this.NUM____s___ = void 0;
      }
      this.NUM____c___ = false;
      this.NUM____w___ = false;
      this.NUM____m___ = false;
      this.NUM____gw___ = false;
      // Make this non-extensible.
      this.ne___ = this;
      // Cache frozen state.
      this.z___ = this;
      return this;
    };

  ////////////////////////////////////////////////////////////////////////
  // Functions to walk the prototype chain
  ////////////////////////////////////////////////////////////////////////

  /**
   * An object is prototypical iff its 'constructor' property points
   * at a genuine function whose 'prototype' property points back at
   * it.
   */
  function isPrototypical(obj) {
    if ((typeof obj) !== 'object') { return false; }
    if (obj === null) { return false; }
    var constr = obj.constructor;
    if ((typeof constr) !== 'function') { return false; }
    return constr.prototype === obj;
  }

  var BASE_OBJECT_CONSTRUCTOR = {};

  /**
   * Returns the 'constructor' property of obj's prototype.
   * <p>
   * By "obj's prototype", we mean the prototypical object that obj
   * most directly inherits from, not the value of its 'prototype'
   * property. We memoize the apparent prototype into 'Prototype___' to
   * speed up future queries.
   * <p>
   * If obj is a function or not an object, return undefined.
   * <p>
   * If the object is determined to be directly constructed by the 'Object'
   * function in *some* frame, we return distinguished marker value
   * BASE_OBJECT_CONSTRUCTOR.
   */
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
    var result;
    if (obj.hasOwnProperty('Prototype___')) {
      var proto = obj.Prototype___;
      // At this point we know that (typeOf(proto) === 'object')
      if (proto === null) { return void 0; }
      result = proto.constructor;
      // rest of: if (!isPrototypical(result))
      if (result.prototype !== proto || (typeof result) !== 'function') {
        result = directConstructor(proto);
      }
    } else {
      if (!obj.hasOwnProperty('constructor')) {
        // TODO(erights): Detect whether this is a valid constructor
        // property in the sense that result is a proper answer. If
        // not, at least give a sensible error, which will be hard to
        // phrase.
        result = obj.constructor;
      } else {
        var oldConstr = obj.constructor;
        // TODO(erights): This code assumes that any 'constructor' property
        // revealed by deleting the own 'constructor' must be the constructor
        // we're interested in.
        if (delete obj.constructor) {
          result = obj.constructor;
          obj.constructor = oldConstr;
        } else if (isPrototypical(obj)) {
          log('Guessing the directConstructor of : ' + obj);
          return BASE_OBJECT_CONSTRUCTOR;
        } else {
          throw new TypeError('Discovery of direct constructors unsupported '
              + 'when the constructor property is not deletable: '
              + obj + '.constructor === ' + oldConstr);
        }
      }

      if ((typeof result) !== 'function' || !(obj instanceof result)) {
        if (obj === obj.baseProto___) {
          return void 0;
        }
        throw new TypeError('Discovery of direct constructors for foreign '
            + 'begotten objects not implemented on this platform');
      }
      if (result.prototype.constructor === result) {
        // Memoize, so it'll be faster next time.
        obj.Prototype___ = result.prototype;
      }
    }
    // If the result is marked as the 'Object' constructor from some feral
    // frame, return the distinguished marker value.
    if (result === result.FERAL_FRAME_OBJECT___) {
      return BASE_OBJECT_CONSTRUCTOR;
    }
    // If the result is the 'Object' constructor from some Caja frame,
    // return the distinguished marker value.
    if (result === obj.CAJA_FRAME_OBJECT___) {
      return BASE_OBJECT_CONSTRUCTOR;
    }
    return result;
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
      throw new TypeError('Internal: Feral constructor is not a function');
    }
    someSuper = someSuper.prototype.constructor;
    var noop = function () {};
    if (someSuper.new___ === noop.new___) {
      throw new TypeError('Internal: toxic function encountered!');
    }
    noop.prototype = someSuper.prototype;
    feralCtor.prototype = new noop();
    feralCtor.prototype.Prototype___ = someSuper.prototype;

    var inert = function() {
        throw new TypeError('This constructor cannot be called directly.');
      };

    inert.prototype = feralCtor.prototype;
    feralCtor.prototype.constructor = inert;
    taming.tamesTo(feralCtor, inert);
    return markFuncFreeze(inert);
  }

  /**
   * A marker for all objects created within a Caja frame.
   */
  Object.prototype.CAJA_FRAME_OBJECT___ = Object;

  function isDefinedInCajaFrame(o) {
    return !!o.CAJA_FRAME_OBJECT___;
  }

  /**
   * The property descriptor for numerics
   */
  Object.prototype.NUM____v___ = Object.prototype;
  Object.prototype.NUM____gw___ = false;
  Object.prototype.NUM____w___ = false;
  Object.prototype.NUM____m___ = false;
  Object.prototype.NUM____c___ = false;
  Object.prototype.NUM____e___ = Object.prototype;
  Object.prototype.NUM____g___ = void 0;
  Object.prototype.NUM____s___ = void 0;
  Object.prototype.hasNumerics___ = function () {
      return this.NUM____v___ === this;
    };

  function isFrozen(obj) {
    return obj.z___ === obj;
  }

  /**
   * The property descriptor for array lengths
   */
  // This flag is only used when doing a dynamic lookup of the length property.
  Array.prototype.length_v___ = false;
  Array.prototype.length_gw___ = false;
  Array.prototype.length_w___ = false;
  Array.prototype.length_m___ = false;
  Array.prototype.length_c___ = false;
  Array.prototype.length_e___ = false;

  /**
   * Setter for {@code length}.  This is necessary because
   * shortening an array by setting its length may delete numeric properties.
   */
  Array.prototype.length_s___ = markFunc(function (val) {
      // Freezing an array needs to freeze the length property.
      if (this.z___ === this) {
        throw new TypeError('Cannot change the length of a frozen array.');
      }
      val = ToUint32(val);
      // Since increasing the length does not add properties,
      // we don't need to check extensibility.
      if (val >= this.length) {
        return this.length = val;
      }
      // Decreasing the length may delete properties, so
      // we need to check that numerics are configurable.
      if (!this.hasNumerics___() || this.NUM____c___ === this) {
        return this.length = val;
      }
      throw new TypeError(
          'Shortening the array may delete non-configurable elements.');
    });

  /**
   * Getter for {@code length}.  Only necessary for returning
   * a property descriptor map and dynamic lookups, since reading
   * {@code length} is automatically whitelisted.
   */
  Array.prototype.length_g___ = markFunc(function () { return this.length; });

  // Replace {@code undefined} and {@code null} by
  // {@code USELESS} for use as {@code this}.  If dis is a global
  // (which we try to detect by looking for the ___ property),
  // then we throw an error (external hull breach).
  function safeDis(dis) {
    if (dis === null || dis === void 0) { return USELESS; }
    if (Type(dis) !== 'Object') { return dis; }
    if ('___' in dis) {
      throw new Error('Internal: toxic global!');
    }
    return dis;
  }

  var endsWith__ = /__$/;
  var endsWith_e___ = /([\s\S]*)_e___$/;
  var endsWith_v___ = /([\s\S]*)_v___$/;
  var startsWithNUM___ = /^NUM___/;

  function assertValidPropertyName(P) {
    if (endsWith__.test(P)) {
      throw new TypeError('Properties may not end in double underscore.');
    }
  }

  function callFault(var_args) {
    throw new Error('Internal: toxic function encountered!');
  }

  /**
   * Returns the getter, if any, associated with the accessor property
   * {@code name}.
   *
   * Precondition:
   * {@code obj} must not be {@code null} or {@code undefined}.
   * {@code name} must be a string or a number.
   * Postcondition:
   * If {@code name} is a number, a string encoding of a number, or
   * the string {@code 'NUM___'}, then we must return {@code undefined}.
   */
  function getter(obj, name) {
    return obj[name + '_g___'];
  }

  /**
   * Returns the setter, if any, associated with the accessor property
   * {@code name}.
   *
   * Precondition:
   * {@code obj} must not be {@code null} or {@code undefined}.
   * {@code name} must be a string or a number.
   * Postcondition:
   * If {@code name} is a number, a string encoding of a number, or
   * the string {@code 'NUM___'}, then we must return {@code undefined}.
   */
  function setter(obj, name) {
    return obj[name + '_s___'];
  }

  /**
   * Precondition:
   * {@code obj} must not be {@code null} or {@code undefined}.
   * {@code name} must be a string or a number.
   * Postcondition:
   * If {@code name} is a number, a string encoding of a number, or
   * the string {@code 'NUM___'}, then we must return {@code false}.
   */
  function hasOwnAccessor(obj, name) {
    var valueFlag = name + '_v___';
    return obj.hasOwnProperty(valueFlag) && !obj[valueFlag];
  }

  /**
   * Precondition:
   * {@code obj} must not be {@code null} or {@code undefined}.
   * {@code name} must be a string that is not the string encoding of
   *              a number; {@code name} may be {@code 'NUM___'}.
   */
  function fastpathWrite(obj, name) {
    obj[name + '_gw___'] = obj;
    obj[name + '_m___'] = false;
    obj[name + '_w___'] = obj;
  }

  /**
   * Precondition:
   * {@code obj} must not be {@code null} or {@code undefined}.
   * {@code name} must be a string that is not the string encoding of
   *              a number; {@code name} may be {@code 'NUM___'}.
   */
  function fastpathMethod(obj, name) {
    obj[name + '_w___'] = false;
    obj[name + '_m___'] = true;
  }

  ////////////////////////////////////////////////////////////////////////
  // Creating defensible (transitively frozen) objects
  ////////////////////////////////////////////////////////////////////////

  // We defer actual definition of the structure since we cannot create the
  // necessary data structures (newTable()) yet
  var deferredDefended = [];
  var addToDefended = function(root) {
    deferredDefended.push(root);
  };

  var functionInstanceVoidNameGetter = markFunc(function() { return ''; });
  // Must freeze in a separate step to break circular dependency
  addToDefended(freeze(functionInstanceVoidNameGetter));

  /**
   * We defer the creation of these properties until they're asked for.
   */
  function installFunctionInstanceProps(f) {
    var name = f.name___;
    delete f.name___;
    // Object.prototype.DefineOwnProperty___ may not be defined yet
    f.prototype_v___ = f;
    f.prototype_w___ = f;
    f.prototype_gw___ = f;
    f.prototype_c___ = false;
    f.prototype_e___ = false;
    f.prototype_g___ = void 0;
    f.prototype_s___ = void 0;
    f.prototype_m___ = false;
    f.length_v___ = f;
    f.length_w___ = false;
    f.length_gw___ = false;
    f.length_c___ = false;
    f.length_e___ = false;
    f.length_g___ = void 0;
    f.length_s___ = void 0;
    f.length_m___ = false;
    // Rhino prohibits setting the name property of function instances,
    // so we install a getter instead.
    f.name_v___ = false;
    f.name_w___ = false;
    f.name_gw___ = false;
    f.name_c___ = false;
    f.name_e___ = false;
    f.name_g___ = ((name === '')
        ? functionInstanceVoidNameGetter
        : markFuncFreeze(function() {return name;}));
    f.name_s___ = void 0;
    f.name_m___ = false;

    // Add to the list of defended (transitively frozen) objects so that
    // the def(...) function does not encounter these (newly created) functions
    // and go into an infinite loop freezing them.
    addToDefended(f.name_g___);
  }

  function deferredV(name) {
    delete this.v___;
    delete this.w___;
    delete this.c___;
    delete this.DefineOwnProperty___;
    installFunctionInstanceProps(this);
    // Object.prototype.v___ may not be defined yet
    return this.v___ ? this.v___(name) : void 0;
  }

  function deferredW(name, val) {
    delete this.v___;
    delete this.w___;
    delete this.c___;
    delete this.DefineOwnProperty___;
    installFunctionInstanceProps(this);
    return this.w___(name, val);
  }

  function deferredC(name) {
    delete this.v___;
    delete this.w___;
    delete this.c___;
    delete this.DefineOwnProperty___;
    installFunctionInstanceProps(this);
    return this.c___(name);
  }

  function deferredDOP(name, desc) {
    delete this.v___;
    delete this.w___;
    delete this.c___;
    delete this.DefineOwnProperty___;
    installFunctionInstanceProps(this);
    return this.DefineOwnProperty___(name, desc);
  }

  /**
   * For taming a simple function or a safe exophoric function (only reads
   * whitelisted properties of {@code this}).
   */
  function markFunc(fn, name) {
    if (!isFunction(fn)) {
      throw new TypeError('Expected a function instead of ' + fn);
    }
    if (fn.f___ !== Function.prototype.f___ &&
        fn.f___ !== fn.apply) {
      throw new TypeError('The function is already tamed ' +
         'or not from this frame.\n' + fn.f___);
    }
    fn.f___ = fn.apply;
    fn.ok___ = true;
    fn.new___ = fn;
    // Anonymous functions get a 'name' that is the empty string
    fn.name___ = ((name === '' || name === void 0)
        ? '' : '' + name);
    fn.v___ = deferredV;
    fn.w___ = deferredW;
    fn.c___ = deferredC;
    fn.DefineOwnProperty___ = deferredDOP;
    var p = fn.prototype;
    if (p && // must be truthy
        typeof p === 'object' && // must be an object
        // must not already have constructor whitelisted.
        !p.hasOwnProperty('constructor_v___')) {
      p.constructor_v___ = p;
      p.constructor_w___ = p;
      p.constructor_gw___ = p;
      p.constructor_c___ = p;
      p.constructor_e___ = false;
      p.constructor_g___ = void 0;
      p.constructor_s___ = void 0;
      p.constructor_m___ = false;
    }
    return fn;
  }

  /**
   * Declares that it is safe for cajoled code to call this as a
   * function.
   *
   * <p>This may be because it's this-less, or because it's a cajoled
   * function that sanitizes its this on entry.
   */
  function markSafeFunc(fn, name) {
    markFunc(fn, name);
    fn.i___ = fn;
    return fn;
  }

  function markFuncFreeze(fn, name) {
    return freeze(markFunc(fn, name));
  }

  /**
   * Is the property {@code name} whitelisted as a value on {@code obj}?
   *
   * Precondition:
   * {@code obj} must not be {@code null} or {@code undefined}.
   * {@code name} must be a string that is not the string encoding
   *              of a number; {@code name} may be {@code 'NUM___'}.
   */
  function hasValue(obj, name) {
    // This doesn't need an "|| name === 'NUM___'" since, for all obj,
    // (obj.NUM____v___) is truthy
    return !!obj[name + '_v___'];
  }

  /**
   * Is the property {@code name} whitelisted as an own value on {@code obj}?
   *
   * Precondition:
   * {@code obj} must not be {@code null} or {@code undefined}.
   * {@code name} must be a string that is not the string encoding
   *              of a number; {@code name} may be {@code 'NUM___'}.
   */
  function hasOwnValue(obj, name) {
    return obj[name + '_v___'] === obj || name === 'NUM___';
  }

  /**
   * Precondition:
   * {@code obj} must not be {@code null} or {@code undefined}.
   * {@code name} must be a string that is not the string encoding
   *              of a number; {@code name} may be {@code 'NUM___'}.
   */
  Object.prototype.HasOwnProperty___ = function (name) {
      return this.hasOwnProperty(name + '_v___') || name === 'NUM___';
    };

  /**
   * Tests whether the fast-path _w___ flag is set, or grantWrite() has been
   * called, on this object itself as an own (non-inherited) attribute.
   * Determines the value of the writable: attribute of property descriptors.
   *
   * Preconditions:
   * {@code obj} must not be {@code null} or {@code undefined}.
   * {@code name} must be a string that is not the string encoding
   *              of a number; {@code name} may be {@code 'NUM___'}.
   */
  function isWritable(obj, name) {
    if (obj[name + '_w___'] === obj) { return true; }
    if (obj[name + '_gw___'] === obj) {
      obj[name + '_m___'] = false;
      obj[name + '_w___'] = obj;
      return true;
    }
    // Frozen and preventExtensions implies hasNumerics
    if (name === 'NUM___' && !obj.hasNumerics___()) {
      obj.NUM____v___ = obj;
      obj.NUM____gw___ = obj;
      obj.NUM____w___ = false;
      obj.NUM____c___ = obj;
      obj.NUM____e___ = obj;
      obj.NUM____g___ = void 0;
      obj.NUM____s___ = void 0;
      obj.NUM____m___ = false;
      return true;
    }
    return false;
  }

  /**
   * Tests whether {@code grantEnumerate} has been called on the property
   * {@code name} of this object or one of its ancestors.
   *
   * Precondition:
   * {@code obj} must not be {@code null} or {@code undefined}.
   * {@code name} must be a string that is not the string encoding
   *              of a number; {@code name} may be {@code 'NUM___'}.
   */
  function isEnumerable(obj, name) {
    // This doesn't need an "|| name === 'NUM___'" since, for all obj,
    // (obj.NUM____e___) is truthy
    return !!obj[name + '_e___'];
  }

  /**
   * Tests whether {@code grantConfigure} has been called on the property
   * {@code name} of this object or one of its ancestors.
   *
   * Precondition:
   * {@code obj} must not be {@code null} or {@code undefined}.
   * {@code name} must be a string that is not the string encoding
   *     of a number; {@code name} may be 'NUM___'.
   */
  function isConfigurable(obj, name) {
    return obj[name + '_c___'] === obj ||
        (name === 'NUM___' && !obj.hasNumerics___());
  }

  function isExtensible(obj) {
    return Type(obj) === 'Object' && obj.ne___ !== obj;
  }

  /**
   * Tests whether an assignment to {@code obj[name]} would extend the object.
   *
   * Precondition:
   * {@code obj} must not be {@code null} or {@code undefined}.
   * {@code name} must be a string.
   */
  function wouldExtend(obj, name) {
    // Would assigning to a numeric index extend the object?
    if (isNumericName(name)) {
      return !obj.hasOwnProperty(name);
    }
    // If name is an own data property, then writing to it won't extend obj.
    if (hasOwnValue(obj, name)) { return false; }
    // If name is an inherited accessor property, invoking the
    // setter won't extend obj. (In any uncajoled setter where it
    // might, the taming must throw an error instead.)
    if (obj[name + '_s___']) { return false; }
    return true;
  }

  function isArray(obj) {
    return classProp.call(obj) === '[object Array]';
  }

  function isFunction(obj) {
    return classProp.call(obj) === '[object Function]';
  }

  function isError(obj) {
    return classProp.call(obj) === '[object Error]';
  }

  /**
   * Returns an object whose enumerable keys are the ones to iterate over.
   * For most objects, this will be the object itself; for proxies and tamings
   * it may be different.
   */
  Object.prototype.e___ = function() {
      return this;
    };

  function allKeys(obj) {
    return obj.allKeys___();
  }

  Object.prototype.allKeys___ = function () {
      var i, m, result = [];
      for (i in this) {
        if (isNumericName(i)) {
          result.push(i);
        } else {
          if (startsWithNUM___.test(i) && endsWith__.test(i)) { continue; }
          m = i.match(endsWith_v___);
          if (m) { result.push(m[1]); }
        }
      }
      return result;
    };

  function ownEnumKeys(obj) {
    return obj.keys___();
  }

  Object.prototype.keys___ = function () {
      var i, m, result = [];
      for (i in this) {
        if (!this.hasOwnProperty(i)) { continue; }
        if (isNumericName(i)) {
          result.push(i);
        } else {
          if (startsWithNUM___.test(i) && endsWith__.test(i)) { continue; }
          m = i.match(endsWith_e___);
          if (m && this[i]) { result.push(m[1]); }
        }
      }
      return result;
    };

  Object.prototype.ownKeys___ = function () {
      var i, m, result = [];
      var keys = fastOwnKeys(this);
      for (var k = 0, n = keys.length; k < n; k++) {
        i = keys[k];
        // inline isNumericName(i)
        if (typeof i === 'number' || ('' + (+i)) === i) {
          result.push(i);
        } else if (5 < i.length && i.substr(i.length - 5) === '_v___'
                   && i.substr(0, 6) !== 'NUM___') {
          result.push(i.substr(0, i.length - 5));
        }
      }
      return result;
    };

  function ownUntamedKeys(obj) {
    var i, m, result = [];
    for (i in obj) {
      if (obj.hasOwnProperty(i) && (isNumericName(i) || !endsWith__.test(i))) {
        result.push(i);
      }
    }
    return result;
  }

  /**
   * Returns a new object whose only utility is its identity and (for
   * diagnostic purposes only) its name.
   */
  function Token(name) {
    name = '' + name;
    return snowWhite({
        toString: markFuncFreeze(function tokenToString() {
            return name;
          }),
        throwable___: true
      });
  }

  /**
   * Checks if {@code n} is governed by the {@code NUM___} property descriptor.
   *
   * Preconditions:
   *   {@code typeof n === 'number'} or {@code 'string'}
   */
  function isNumericName(n) {
    return typeof n === 'number' || ('' + (+n)) === n;
  }

  ////////////////////////////////////////////////////////////////////////
  // JSON
  ////////////////////////////////////////////////////////////////////////

  // TODO: Can all this JSON stuff be moved out of the TCB?
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
      var x = json.stringify({'a':3, 'b__':4}, function replacer(k, v) {
          return (/__$/.test(k) ? void 0 : v);
        });
      if (x !== '{"a":3}') {
        return false;
      }
      // ie8 has a buggy JSON unless this update has been applied:
      //   http://support.microsoft.com/kb/976662
      // test for one of the known bugs.
      x = json.stringify(void 0, 'invalid');
      return x === void 0;
    } catch (e) {
      return false;
    }
  }

  var goodJSON = {};
  var parser = jsonParseOk(JSON) ? JSON.parse : json_sans_eval.parse;
  goodJSON.parse = markFunc(function () {
      return whitelistAll(parser.apply(this, arguments), true);
    });
  goodJSON.stringify = markFunc(jsonStringifyOk(JSON) ?
      JSON.stringify : json_sans_eval.stringify);

  safeJSON = snowWhite({
      CLASS___: 'JSON',
      parse: markFunc(function (text, opt_reviver) {
          var reviver = void 0;
          if (opt_reviver) {
            reviver = markFunc(function (key, value) {
                return opt_reviver.f___(this, arguments);
              });
          }
          return goodJSON.parse(
              json_sans_eval.checkSyntax(
                  text,
                  function (key) {
                    return !endsWith__.test(key);
                  }),
              reviver);
        }),
      stringify: markFunc(function (obj, opt_replacer, opt_space) {
          switch (typeof opt_space) {
            case 'number': case 'string': case 'undefined': break;
            default: throw new TypeError('space must be a number or string');
          }
          var replacer;
          if (opt_replacer) {
            replacer = markFunc(function (key, value) {
                if (!this.HasProperty___(key)) { return void 0; }
                return opt_replacer.f___(this, arguments);
              });
          } else {
            replacer = markFunc(function (key, value) {
                return (this.HasProperty___(key) || key === '') ?
                    value :
                    void 0;
              });
          }
          return goodJSON.stringify(obj, replacer, opt_space);
        })
    });


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
  markFunc(getLogFunc);

  /**
   * Register newLogFunc as the current logging function, to be called
   * by {@code ___.log(str)}.
   * <p>
   * A logging function is assumed to have the signature
   * {@code (str, opt_stop)}, where<ul>
   * <li>{@code str} is the diagnostic string to be logged, and
   * <li>{@code opt_stop}, if present and {@code true}, indicates
   *     that normal flow control is about to be terminated by a
   *     throw. This provides the logging function the opportunity to
   *     terminate normal control flow in its own way, such as by
   *     invoking an undefined method, in order to trigger a Firebug
   *     stacktrace.
   * </ul>
   */
  function setLogFunc(newLogFunc) { myLogFunc = newLogFunc; }
  markFunc(setLogFunc);

  /**
   * Calls the currently registered logging function.
   */
  function log(str) { myLogFunc('' + str); }
  markFunc(log);

  /**
   * Like an assert that can't be turned off.
   * <p>
   * Either returns true (on success) or throws (on failure). The
   * arguments starting with {@code var_args} are converted to
   * strings and appended together to make the message of the Error
   * that's thrown.
   * <p>
   * TODO(erights) We may deprecate this in favor of <pre>
   *     if (!test) { throw new Error(var_args...); }
   * </pre>
   */
  function enforce(test, var_args) {
    if (!test) { throw new Error(slice.call(arguments, 1).join('')); }
    return true;
  }

  /**
   * Enforces {@code typeof specimen === typename}, in which case
   * specimen is returned.
   * <p>
   * If not, throws an informative TypeError
   * <p>
   * opt_name, if provided, should be a name or description of the
   * specimen used only to generate friendlier error messages.
   */
  function enforceType(specimen, typename, opt_name) {
    if (typeof specimen !== typename) {
      throw new TypeError('expected ' + typename + ' instead of ' +
          typeof specimen + ': ' + (opt_name || specimen));
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
      throw new TypeError('Must be integral: ' + specimen);
    }
    if (specimen < 0) {
      throw new TypeError('Must not be negative: ' + specimen);
    }
    // Could pre-compute precision limit, but probably not faster
    // enough to be worth it.
    if (Math.floor(specimen - 1) !== specimen - 1) {
      throw new TypeError('Beyond precision limit: ' + specimen);
    }
    if (Math.floor(specimen - 1) >= specimen) {
      throw new TypeError('Must not be infinite: ' + specimen);
    }
    return specimen;
  }

  /**
   * Returns a function that can typically be used in lieu of
   * {@code func}, but that logs a deprecation warning on first use.
   * <p>
   * Currently for internal use only, though may make this available
   * on {@code ___} or even {@code es53} at a later time, after
   * making it safe for such use. Forwards only arguments to
   * {@code func} and returns results back, without forwarding
   * {@code this}. If you want to deprecate an exophoric function,
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
   * identity of arbitrary keys (as defined by <tt>identical()</tt>) to
   * arbitrary values.
   *
   * <p>Operates as specified by <a href=
   * "http://wiki.ecmascript.org/doku.php?id=harmony:weak_maps"
   * >weak maps</a>, including the optional parameters of the old
   * <a href=
   * "http://wiki.ecmascript.org/doku.php?id=strawman:ephemeron_tables&rev=1269457867#implementation_considerations"
   * >Implementation Considerations</a> section regarding emulation on
   * ES3, except that, when {@code opt_useKeyLifetime} is falsy or
   * absent, the keys here may be primitive types as well.
   *
   * <p> To support Domita, the keys might be host objects.
   */
  function newTable(opt_useKeyLifetime, opt_expectedSize) {
    magicCount++;
    var myMagicIndexName = MAGIC_NAME + magicCount + '___';

    function setOnKey(key, value) {
      var ktype = typeof key;
      if (!key || (ktype !== 'function' && ktype !== 'object')) {
        throw new TypeError("Can't use key lifetime on primitive keys: " + key);
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
        throw new TypeError("Can't use key lifetime on primitive keys: " + key);
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

    function hasOnKey(key) {
      var ktype = typeof key;
      if (!key || (ktype !== 'function' && ktype !== 'object')) {
        throw new TypeError("Can't use key lifetime on primitive keys: " + key);
      }
      var list = key[myMagicIndexName];
      if (!list || list[0] !== key) {
        return false;
      } else {
        for (var i = 1; i < list.length; i += 2) {
          if (list[i] === MAGIC_TOKEN) { return true; }
        }
        return false;
      }
    }

    if (opt_useKeyLifetime) {
      return snowWhite({
          set: markFuncFreeze(setOnKey),
          get: markFuncFreeze(getOnKey),
          has: markFuncFreeze(hasOnKey)
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
     * If the key is absent, returns {@code undefined}.
     * <p>
     * Users of this table cannot distinguish an {@code undefined}
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

    /**
     * Returns whether this table contains the {@code key}.
     */
    function hasOnTable(key) {
      switch (typeof key) {
        case 'object':
        case 'function': {
          if (null === key) { return 'prim_null' in myValues; }
          var index = getOnKey(key);
          return void 0 !== index;
        }
        case 'string': { return ('str_' + key) in myValues; }
        default: { return ('prim_' + key) in myValues; }
      }
    }

    return snowWhite({
        set: markFuncFreeze(setOnTable),
        get: markFuncFreeze(getOnTable),
        has: markFuncFreeze(hasOnTable)
      });
  }

  WeakMap = WeakMap ?
      (function (WeakMap) {
        return markFunc(function () {
          var result = WeakMap();
          // DefineOwnProperty___ may not be defined yet.
          markFunc(result.get);
          result.get_v___ = result;
          result.get_c___ = false;
          result.get_w___ = false;
          result.get_gw___ = result;
          result.get_e___ = result;
          result.get_m___ = false;
          result.get_g___ = false;
          result.get_s___ = false;

          markFunc(result.set);
          result.set_v___ = result;
          result.set_c___ = false;
          result.set_w___ = false;
          result.set_gw___ = result;
          result.set_e___ = result;
          result.set_m___ = false;
          result.set_g___ = false;
          result.set_s___ = false;

          markFunc(result.has);
          result.has_v___ = result;
          result.has_c___ = false;
          result.has_w___ = false;
          result.has_gw___ = result;
          result.has_e___ = result;
          result.has_m___ = false;
          result.has_g___ = false;
          result.has_s___ = false;
          return result;
        });
      })(WeakMap) :
      markFunc(function () { return newTable(true); });

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
   * IMPORTS___.document.innerHTML = "
   *  &lt;a onmouseover=\"
   *    (function(IMPORTS___) {
   *      IMPORTS___.alert(1);
   *    })(___.getImports(" + ___.getId(IMPORTS___) + "))
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
      throw new Error('Internal: imports#', id, ' unregistered');
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
  // Creating defensible (transitively frozen) objects.
  ////////////////////////////////////////////////////////////////////////

  var defended = newTable();
  /**
   * To define a defended object is to freeze it and all objects
   * transitively reachable from it via transitive reflective
   * property traversal.
   */

  // These will be set to the values originally exposed to guest code; since
  // guest code may change Object.getPrototypeOf, etc., we cache some methods.
  var origGetPrototypeOf, origGetOwnPropertyDescriptor;

  var def = markFuncFreeze(function (root) {
    var i, n;
    var defending = newTable();
    var defendingList = [];
    var todo = [root];
    while (todo.length) {
      var val = todo.pop();
      if (val !== Object(val) || defended.get(val) || defending.get(val)) {
        continue;
      }
      defending.set(val, true);
      defendingList.push(val);
      freeze(val);
      todo.push(origGetPrototypeOf(val));
      var keys = val.ownKeys___();
      for (i = 0, n = keys.length; i < n; i++) {
        var desc = origGetOwnPropertyDescriptor(val, keys[i]);
        todo.push(desc.value);
        todo.push(desc.get);
        todo.push(desc.set);
      }
    }
    for (i = 0, n = defendingList.length; i < n; i++) {
      defended.set(defendingList[i], true);
    }
    return root;
  });

  addToDefended = markFuncFreeze(function(root) {
    defended.set(root, true);
  });

  deferredDefended.forEach(function(o) { addToDefended(o); });
  deferredDefended = void 0;

  ////////////////////////////////////////////////////////////////////////
  // Tokens
  ////////////////////////////////////////////////////////////////////////

  /**
   * When a {@code this} value must be provided but nothing is
   * suitable, provide this useless object instead.
   */
  var USELESS = Token('USELESS');

  /**
   * A unique value that should never be made accessible to untrusted
   * code, for distinguishing the absence of a result from any
   * returnable result.
   * <p>
   * See makeNewModuleHandler's getLastOutcome().
   */
  var NO_RESULT = Token('NO_RESULT');

  ////////////////////////////////////////////////////////////////////////
  // Guards and Trademarks
  ////////////////////////////////////////////////////////////////////////

  /**
   * The identity function just returns its argument.
   */
  function identity(x) { return x; }

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
    var failFunc = opt_failFunc || identity;
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
    markFuncFreeze(ejector);
    try {
      try {
        return attemptFunc.m___('call', [USELESS, ejector]);
      } finally {
        disabled = true;
      }
    } catch (e) {
      if (e === token) {
        return failFunc.m___('call', [USELESS, stash]);
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
      opt_ejector.m___('call', [USELESS, result]);
      throw new Error('Ejector did not exit: ', opt_ejector);
    } else {
      throw new Error(result);
    }
  }

  /**
   * Internal routine for making a trademark from a table.
   * <p>
   * To untangle a cycle, the guard made by {@code makeTrademark} is
   * not yet either stamped or frozen. The caller of
   * {@code makeTrademark} must do both before allowing it to
   * escape.
   */
  function makeTrademark(typename, table) {
    typename = '' + typename;
    return snowWhite({
        toString: markFuncFreeze(function() { return typename + 'Mark'; }),

        stamp: snowWhite({
          toString: markFuncFreeze(function() { return typename + 'Stamp'; }),
          mark___: markFuncFreeze(function(obj) {
            table.set(obj, true);
            return obj;
          })
        }),

        guard: snowWhite({
          toString: markFuncFreeze(function() { return typename + 'T'; }),
          coerce: markFuncFreeze(function(specimen, opt_ejector) {
            if (table.get(specimen)) { return specimen; }
            eject(opt_ejector,
                  'Specimen does not have the "' + typename + '" trademark');
          })
        })
      });
  }

  /**
   * Objects representing guards should be marked as such, so that
   * they will pass the {@code GuardT} guard.
   * <p>
   * {@code GuardT} is generally accessible as
   * {@code cajita.GuardT}. However, {@code GuardStamp} must not be
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
  freeze(GuardStamp.mark___(GuardT));

  /**
   * The {@code Trademark} constructor makes a trademark, which is a
   * guard/stamp pair, where the stamp marks and freezes unfrozen
   * records as carrying that trademark and the corresponding guard
   * cerifies objects as carrying that trademark (and therefore as
   * having been marked by that stamp).
   * <p>
   * By convention, a guard representing the type-like concept 'Foo'
   * is named 'FooT'. The corresponding stamp is 'FooStamp'. And the
   * record holding both is 'FooMark'. Many guards also have
   * {@code of} methods for making guards like themselves but
   * parameterized by further constraints, which are usually other
   * guards. For example, {@code T.ListT} is the guard representing
   * frozen array, whereas {@code T.ListT.of(cajita.GuardT)}
   * represents frozen arrays of guards.
   */
  function Trademark(typename) {
    var result = makeTrademark(typename, newTable(true));
    freeze(GuardStamp.mark___(result.guard));
    return result;
  }
  markFuncFreeze(Trademark);

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
   * Given that {@code stamps} is a list of stamps and
   * {@code record} is a non-frozen object, this marks record with
   * the trademarks of all of these stamps, and then freezes and
   * returns the record.
   * <p>
   * If any of these conditions do not hold, this throws.
   */
  function stamp(stamps, record) {
    // TODO: Should nonextensible objects be stampable?
    if (isFrozen(record)) {
      throw new TypeError("Can't stamp frozen objects: " + record);
    }
    stamps = Array.slice(stamps, 0);
    var numStamps = stamps.length;
    // First ensure that we will succeed before applying any stamps to
    // the record.
    var i;
    for (i = 0; i < numStamps; i++) {
      if (!('mark___' in stamps[i])) {
        throw new TypeError("Can't stamp with a non-stamp: " + stamps[i]);
      }
    }
    for (i = 0; i < numStamps; i++) {
      // Only works for real stamps, postponing the need for a
      // user-implementable auditing protocol.
      stamps[i].mark___(record);
    }
    return freeze(record);
  }

  /**
   * Create a guard which passes all objects present in {@code table}.
   * This may be used to define trademark-like systems which do not require
   * the object to be frozen.
   *
   * @param {string} typename Used for toString results.
   * @param {string} errorMessage Used when an object does not pass the guard.
   */
  function makeTableGuard(table, typename, errorMessage) {
    var g = whitelistAll({
      toString: markFuncFreeze(function() { return typename + 'T'; }),
      coerce: markFuncFreeze(function(specimen, opt_ejector) {
        if (table.get(specimen)) { return specimen; }
        eject(opt_ejector, errorMessage);
      })
    });
    stamp([GuardStamp], g);
    return g;
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
        throw new TypeError('Sealer/Unsealer mismatch');
      } else if (payload === undefinedStandin) {
        return void 0;
      } else {
        return payload;
      }
    }
    return snowWhite({
        seal: markFuncFreeze(seal),
        unseal: markFuncFreeze(unseal)
      });
  }

  /**
   * A call to cajita.manifest(data) is dynamically ignored, but if the
   * data expression is valid static JSON text, its value is made
   * statically available to the module loader.
   * <p>
   * TODO(erights): Find out if this is still the plan.
   */
  function manifest(ignored) {}

  ///////////////////////////////////////////////////////////////////
  // Specification
  ///////////////////////////////////////////////////////////////////

  /**
   * 4. Overview
   */

  /**
   * 4.2 Language Overview
   */

  /**
   * 8. Types
   */

  function Type(x) {
    switch (typeof x) {
      case 'undefined': { return 'Undefined'; }
      case 'boolean': { return 'Boolean'; }
      case 'string': { return 'String'; }
      case 'number': { return 'Number'; }
      default: { return x ? 'Object' : 'Null'; }
    }
  }

  /**
   * 8.6 Object type
   */

  // 8.6.1
  var attributeDefaults = {
      value: void 0,
      get: void 0,
      set: void 0,
      writable: false,
      enumerable: false,
      configurable: false
    };

  // 8.6.2
  function isPrimitive(x) {
    return Type(x) !== 'Object';
  }

  /**
   * 8.10 The Property Descriptor and Property Identifier Specification Types
   */

  // 8.10.1
  function IsAccessorDescriptor(Desc) {
    if (Desc === void 0) { return false; }
    if ('get' in Desc) { return true; }
    if ('set' in Desc) { return true; }
    return false;
  }

  // 8.10.2
  function IsDataDescriptor(Desc) {
    if (Desc === void 0) { return false; }
    if ('value' in Desc) { return true; }
    if ('writable' in Desc) { return true; }
    return false;
  }

  // 8.10.3
  function IsGenericDescriptor(Desc) {
    if (Desc === void 0) { return false; }
    if (!IsAccessorDescriptor(Desc) && !IsDataDescriptor(Desc)) {
      return true;
    }
    return false;
  }

  // 8.10.4
  // Converts an internal property descriptor to an external one.
  function FromPropertyDescriptor(Desc) {
    function copyProp(Desc, obj, name) {
      obj.DefineOwnProperty___(name, {
          value: Desc[name],
          writable: true,
          enumerable: true,
          configurable: true
        });
    }

    // 1. If Desc is undefined, then return undefined.
    if (Desc === void 0) { return void 0; }
    // 2. Let obj be the result of creating a new object
    //    as if by the expression new Object() where Object is the standard
    //    built-in constructor with that name.
    var obj = {};
    // 3. If IsDataDescriptor(Desc) is true, then
    if (IsDataDescriptor(Desc)) {
      // a. Call the [[DefineOwnProperty]] internal method of obj
      //    with arguments "value", Property Descriptor {
      //      [[Value]]:Desc.[[Value]],
      //      [[Writable]]: true,
      //      [[Enumerable]]: true,
      //      [[Configurable]]: true
      //    }, and false.
      copyProp(Desc, obj, 'value');
      // b. Call the [[DefineOwnProperty]] internal method of obj
      //    with arguments "writable", Property Descriptor {[[Value]]:
      //    Desc.[[Writable]], [[Writable]]: true, [[Enumerable]]:
      //    true, [[Configurable]]: true}, and false.
      copyProp(Desc, obj, 'writable');
    }
    // 4. Else, IsAccessorDescriptor(Desc) must be true, so
    else {
      // a. Call the [[DefineOwnProperty]] internal method of obj
      //    with arguments "get", Property Descriptor {[[Value]]:
      //    Desc.[[Get]], [[Writable]]: true, [[Enumerable]]: true,
      //    [[Configurable]]: true}, and false.
      copyProp(Desc, obj, 'get');
      // b. Call the [[DefineOwnProperty]] internal method of obj
      //    with arguments "set", Property Descriptor {[[Value]]:
      //    Desc.[[Set]], [[Writable]]: true, [[Enumerable]]: true,
      //    [[Configurable]]: true}, and false.
      copyProp(Desc, obj, 'set');
    }
    // 5. Call the [[DefineOwnProperty]] internal method of obj with
    //    arguments "enumerable", Property Descriptor {[[Value]]:
    //    Desc.[[Enumerable]], [[Writable]]: true, [[Enumerable]]: true,
    //    [[Configurable]]: true}, and false.
    copyProp(Desc, obj, 'enumerable');
    // 6. Call the [[DefineOwnProperty]] internal method of obj with
    //    arguments "configurable", Property Descriptor {[[Value]]:
    //    Desc.[[Configurable]], [[Writable]]: true, [[Enumerable]]:
    //    true, [[Configurable]]: true}, and false.
    copyProp(Desc, obj, 'configurable');
    // 7. Return obj.
    return obj;
  }

  // 8.10.5
  // Converts an external property descriptor to an internal one.
  function ToPropertyDescriptor(Obj) {
    // 1. If Type(Obj) is not Object throw a TypeError exception.
    if (Type(Obj) !== 'Object') {
      throw new TypeError('Expected an object.');
    }
    // 2. Let desc be the result of creating a new Property
    //    Descriptor that initially has no fields.
    var desc = {};
    // 3. If the result of calling the [[HasProperty]]
    //    internal method of Obj with argument "enumerable" is true, then
    //   a. Let enum be the result of calling the [[Get]]
    //      internal method of Obj with "enumerable".
    //   b. Set the [[Enumerable]] field of desc to ToBoolean(enum).
    if (Obj.HasProperty___('enumerable')) {
      desc.enumerable = !!Obj.v___('enumerable');
    }
    // 4. If the result of calling the [[HasProperty]]
    //    internal method of Obj with argument "configurable" is true, then
    //   a. Let conf  be the result of calling the [[Get]]
    //      internal method of Obj with argument "configurable".
    //   b. Set the [[Configurable]] field of desc to ToBoolean(conf).
    if (Obj.HasProperty___('configurable')) {
      desc.configurable = !!Obj.v___('configurable');
    }
    // 5. If the result of calling the [[HasProperty]]
    //    internal method of Obj with argument "value" is true, then
    //   a. Let value be the result of calling the [[Get]]
    //      internal method of Obj with argument "value".
    //   b. Set the [[Value]] field of desc to value.
    if (Obj.HasProperty___('value')) {
      desc.value = Obj.v___('value');
    }
    // 6. If the result of calling the [[HasProperty]]
    //    internal method of Obj with argument "writable" is true, then
    // a. Let writable be the result of calling the [[Get]]
    //    internal method of Obj with argument "writable".
    // b. Set the [[Writable]] field of desc to ToBoolean(writable).
    if (Obj.HasProperty___('writable')) {
      desc.writable = !!Obj.v___('writable');
    }
    // 7. If the result of calling the [[HasProperty]]
    //    internal method of Obj with argument "get" is true, then
    if (Obj.HasProperty___('get')) {
      // a. Let getter be the result of calling the [[Get]]
      //    internal method of Obj with argument "get".
      var getter = Obj.v___('get');
      // b. If IsCallable(getter) is false and getter is not
      //    undefined, then throw a TypeError exception.
      if (!isFunction(getter) && getter !== void 0) {
        throw new TypeError('Getter attributes must be functions or undef.');
      }
      // c. Set the [[Get]] field of desc to getter.
      desc.get = asFirstClass(getter);
    }
    // 8. If the result of calling the [[HasProperty]]
    //    internal method of Obj with argument "set" is true, then
    if (Obj.HasProperty___('set')) {
      // a. Let setter be the result of calling the [[Get]]
      //    internal method of Obj with argument "set".
      var setter = Obj.v___('set');
      // b. If IsCallable(setter) is false and setter is not
      //    undefined, then throw a TypeError exception.
      if (!isFunction(setter) && setter !== void 0) {
        throw new TypeError('Setter attributes must be functions or undef.');
      }
      // c. Set the [[Set]] field of desc to setter.
      desc.set = asFirstClass(setter);
    }
    // 9. If either desc.[[Get]] or desc.[[Set]] are present, then
    if ('set' in desc || 'get' in desc) {
      // a. If either desc.[[Value]] or desc.[[Writable]] are present,
      //    then throw a TypeError exception.
      if ('value' in desc) {
        throw new TypeError('Accessor properties must not have a value.');
      }
      if ('writable' in desc) {
        throw new TypeError('Accessor properties must not be writable.');
      }
    }
    // 10. Return desc.
    return desc;
  }

  /**
   * 8.12 Algorithms for Object Internal Methods
   */
  // 8.12.1
  // Returns internal property descriptor or undefined.
  Object.prototype.GetOwnProperty___ = function GetOwnProperty___(P) {
      var O = this;
      //inline if (isNumericName(P)) {
      if (typeof P === 'number' || ('' + (+P)) === P) {
        if (O.hasOwnProperty(P)) {
          return {
              value: O[P],
              writable: isWritable(O, 'NUM___'),
              configurable: isConfigurable(O, 'NUM___'),
              enumerable: true
            };
        } else {
          return void 0;
        }
      }
      P = '' + P;
      // inline assertValidPropertyName(P);
      if (endsWith__.test(P)) {
        throw new TypeError('Properties may not end in double underscore.');
      }

      // 1. If O doesn't have an own property with name P, return undefined.
      // Inline HasOwnProperty___.  Works with proxies because GetOwnProperty___
      // also gets overridden.
      if (!O.hasOwnProperty(P + '_v___')) {
        return void 0;
      }

      // 2. Let D be a newly created Property Descriptor with no fields.
      var D = {};
      // 3. Let X be O's own property named P.
      // 4. If X is a data property, then
      if (hasValue(O, P)) {
        // a. Set D.[[Value]] to the value of X's [[Value]] attribute.
        D.value = O[P];
        // b. Set D.[[Writable]] to the value of X's [[Writable]] attribute
        D.writable = isWritable(O, P);
      } else {
        // 5. Else X is an accessor property, so
        // a. Set D.[[Get]] to the value of X's [[Get]] attribute.
        D.get = getter(O, P);
        // b. Set D.[[Set]] to the value of X's [[Set]] attribute.
        D.set = setter(O, P);
      }
      // 6. Set D.[[Enumerable]] to the value of X's [[Enumerable]] attribute.
      D.enumerable = isEnumerable(O, P);
      // 7. Set D.[[Configurable]] to the value of X's
      // [[Configurable]] attribute.
      D.configurable = isConfigurable(O, P);
      // 8. Return D.
      return D;
    };

  // 8.12.3
  Object.prototype.v___ = function v___(P) {
      P = '' + P;
      if (isNumericName(P)) { return this[P]; }
      assertValidPropertyName(P);
      // Is P an accessor property on this?
      var g = getter(this, P);
      if (g) { return g.f___(this); }
      // Is it whitelisted as a value?
      if (hasValue(this, P)) { return this[P]; }
      return void 0;
    };

  // 8.12.5
  // This follows the philosophy that the ES5 spec was mistaken to prohibit
  // overriding read-only properties.  Chrome also follows this pholosophy.
  // http://wiki.ecmascript.org/doku.php?id=strawman:fixing_override_mistake
  Object.prototype.w___ = function w___(P, V) {
      var thisExtensible = isExtensible(this);
      P = '' + P;
      assertValidPropertyName(P);
      if (!thisExtensible) {
        if (wouldExtend(this, P)) {
          throw new TypeError("Could not create the property '" +
              P + "': " + this + " is not extensible.");
        }
      }

      // At this point, obj is either (extensible) or
      // (non-extensible but already has the property in question).

      V = asFirstClass(V);

      // Numeric names can't be emulated accessors.
      if(isNumericName(P)) {
        if (isWritable(this, 'NUM___')) {
          return this[P] = V;
        } else {
          throw new TypeError("The property '" + P + "' is not writable.");
        }
      }
      // Is P an accessor property on obj?
      var s = setter(this, P);
      if (s) { s.f___(this, [V]); return V; }

      // If P is an own data property,
      if (this[P + '_v___'] === this) {
        // and it's writable, then write;
        if (isWritable(this, P)) {
          fastpathWrite(this, P);
          return this[P] = V;
        }
        // otherwise throw.
        throw new TypeError("The property '" + P + "' is not writable.");
      }

      // At this point, the object is known to be extensible and not to have the
      // property whitelisted.  We need to check if the property exists but
      // is purposely not whitelisted.
      
      // If it doesn't exist,
      if (!this.hasOwnProperty(P)) {
        // then create it;
        this.DefineOwnProperty___(P, {
            value: V,
            writable: true,
            configurable: true,
            enumerable: true
          });
        return V;
      }
      // otherwise throw.
      throw new TypeError("The property '" + P + "' is not writable.");
    };

  // 8.12.6
  /**
   * Precondition: P is a number or string; this is to prevent testing
   * P and the string coersion having side effects.
   */
  Object.prototype.HasProperty___ = function (P) {
      if (isNumericName(P)) { return P in this; }
      return (P + '_v___' in this);
    };

  // Delete the own property P from O without refusing non-configurability.
  function rawDelete(O, P) {
    return delete O[P]
        && delete O[P + '_v___']
        && delete O[P + '_w___']
        && delete O[P + '_gw___']
        && delete O[P + '_g___']
        && delete O[P + '_s___']
        && delete O[P + '_c___']
        && delete O[P + '_e___']
        && delete O[P + '_m___'];
  }

  // 8.12.7
  Object.prototype.c___ = function (P) {
      var O = this;
      P = '' + P;
      // 1. Let desc be the result of calling the [[GetOwnProperty]]
      //    internal method of O with property name P.
      var desc = O.GetOwnProperty___(P);
      // 2. If desc is undefined, then return true.
      if (!desc) {
        return true;
      }
      // 3. If desc.[[Configurable]] is true, then
      if (desc.configurable) {
        if (isNumericName(P)) {
          if (isDeodorized(O, P)) {
            throw new TypeError("Cannot delete Firefox-specific antidote '"
                + P + "' on " + O);
          } else {
            delete O[P];
            return true;
          }
        }
        // a. Remove the own property with name P from O.
        rawDelete(O, P);
        // b. Return true.
        return true;
      }
      // 4. Else if Throw, then throw a TypeError exception.
      // [This is strict mode, so Throw is always true.]
      throw new TypeError("Cannot delete '" + P + "' on " + O);
      // 5. Return false.
    };

  // 8.12.9
  // Preconditions:
  //   Desc is an internal property descriptor.
  //   P is a valid property name.
  Object.prototype.DefineOwnProperty___ = 
    function DefineOwnProperty___(P, Desc) {
      //inline if (isNumericName(P)) {
      if (typeof P === 'number' || ('' + (+P)) === P) {
        throw new TypeError('Cannot define numeric properties.');
      }
      var O = this;
      P = '' + P;
      // 1. Let current be the result of calling the [[GetOwnProperty]]
      //    internal method of O with property name P.
      var current = O.GetOwnProperty___(P);
      // 2. Let extensible be the value of the [[Extensible]] internal
      //    property of O.

      //inline var extensible = isExtensible(O);
      var extensible = Type(O) === 'Object' && O.ne___ !== O;
      // 3. If current is undefined and extensible is false, then Reject.
      if (!current && !extensible) {
        throw new TypeError('This object is not extensible.');
      }
      // 4. If current is undefined and extensible is true, then
      if (!current && extensible) {
        // a. If  IsGenericDescriptor(Desc) or IsDataDescriptor(Desc)
        //    is true, then
        if (IsDataDescriptor(Desc) || IsGenericDescriptor(Desc)) {
          // i. Create an own data property named P of object O whose
          //    [[Value]], [[Writable]], [[Enumerable]] and
          //    [[Configurable]] attribute values are described by
          //    Desc. If the value of an attribute field of Desc is
          //    absent, the attribute of the newly created property is
          //    set to its default value.
          O[P] = Desc.value;
          O[P + '_v___'] = O;
          O[P + '_w___'] = false;
          O[P + '_gw___'] = Desc.writable ? O : false;
          O[P + '_e___'] = Desc.enumerable ? O : false;
          O[P + '_c___'] = Desc.configurable ? O : false;
          O[P + '_g___'] = void 0;
          O[P + '_s___'] = void 0;
          O[P + '_m___'] = false;
        }
        // b. Else, Desc must be an accessor Property Descriptor so,
        else {
          // i. Create an own accessor property named P of object O
          //    whose [[Get]], [[Set]], [[Enumerable]] and
          //    [[Configurable]] attribute values are described by
          //    Desc. If the value of an attribute field of Desc is
          //    absent, the attribute of the newly created property is
          //    set to its default value.
          if (Desc.configurable) { O[P] = void 0; }
          O[P + '_v___'] = false;
          O[P + '_w___'] =  O[P + '_gw___'] = false;
          O[P + '_e___'] = Desc.enumerable ? O : false;
          O[P + '_c___'] = Desc.configurable ? O : false;
          O[P + '_g___'] = Desc.get;
          O[P + '_s___'] = Desc.set;
          O[P + '_m___'] = false;
        }
        // c. Return true.
        return true;
      }
      // 5. Return true, if every field in Desc is absent.
      if (!('value' in Desc ||
          'writable' in Desc ||
          'enumerable' in Desc ||
          'configurable' in Desc ||
          'get' in Desc ||
          'set' in Desc)) {
        return true;
      }
      // 6. Return true, if every field in Desc also occurs in current
      //    and the value of every field in Desc is the same value as the
      //    corresponding field in current when compared using the
      //    SameValue algorithm (9.12).
      var allHaveAppearedAndAreTheSame = true;
      for (var i in Desc) {
        if (!Desc.hasOwnProperty(i)) { continue; }
        if (!SameValue(current[i], Desc[i])) {
          allHaveAppearedAndAreTheSame = false;
          break;
        }
      }
      if (allHaveAppearedAndAreTheSame) { return true; }
      // 7. If the [[Configurable]] field of current is false then
      if (!current.configurable) {
        // a. Reject, if the [Configurable]] field of Desc is true.
        if (Desc.configurable) {
          throw new TypeError("The property '" + P +
              "' is not configurable.");
        }
        // b. Reject, if the [[Enumerable]] field of Desc is present
        //    and the [[Enumerable]] fields of current and Desc are
        //    the Boolean negation of each other.
        if ('enumerable' in Desc && Desc.enumerable !== current.enumerable) {
          throw new TypeError("The property '" + P +
              "' is not configurable.");
        }
      }
      var iddCurrent = IsDataDescriptor(current);
      var iddDesc = IsDataDescriptor(Desc);
      // 8. If IsGenericDescriptor(Desc) is true, then no further
      //    validation is required.
      if (IsGenericDescriptor(Desc)) {
        // Do nothing
      }
      // 9. Else, if IsDataDescriptor(current) and IsDataDescriptor(Desc)
      //    have different results, then
      else if (iddCurrent !== iddDesc) {
        // a. Reject, if the [[Configurable]] field of current is false.
        if (!current.configurable) {
          throw new TypeError("The property '" + P +
              "' is not configurable.");
        }
        // b. If IsDataDescriptor(current) is true, then
        if (iddCurrent) {
          // i. Convert the property named P of object O from a data
          //    property to an accessor property. Preserve the existing
          //    values of the converted property's [[Configurable]] and
          //    [[Enumerable]] attributes and set the rest of the
          //    property's attributes to their default values.
          O[P] = void 0;
          O[P + '_v___'] = false;
          O[P + '_w___'] =  O[P + '_gw___'] = false;
          // O[P + '_e___'] = O[P + '_e___'];
          // O[P + '_c___'] = O[P + '_c___'];
          O[P + '_g___'] = void 0;
          O[P + '_s___'] = void 0;
          O[P + '_m___'] = false;
        }
        // c. Else,
        else {
          // i. Convert the property named P of object O from an
          //    accessor property to a data property. Preserve the
          //    existing values of the converted property's
          //    [[Configurable]] and [[Enumerable]] attributes and set
          //    the rest of the property's attributes to their default
          //    values.
          O[P] = Desc.value;
          O[P + '_v___'] = O;
          O[P + '_w___'] = O[P + '_gw___'] = false;
          // O[P + '_e___'] = O[P + '_e___'];
          // O[P + '_c___'] = O[P + '_c___'];
          O[P + '_g___'] = void 0;
          O[P + '_s___'] = void 0;
          O[P + '_m___'] = false;
        }
      }
      // 10. Else, if IsDataDescriptor(current) and
      //     IsDataDescriptor(Desc) are both true, then
      else if (iddCurrent && iddDesc) {
        // a. If the [[Configurable]] field of current is false, then
        if (!current.configurable) {
          // i. Reject, if the [[Writable]] field of current is false
          //    and the [[Writable]] field of Desc is true.
          if (!current.writable && Desc.writable) {
            throw new TypeError("The property '" + P +
                "' is not configurable.");
          }
          // ii. If the [[Writable]] field of current is false, then
          if (!current.writable) {
            // 1. Reject, if the [[Value]] field of Desc is present and
            //    SameValue(Desc.[[Value]], current.[[Value]]) is false.
            if ('value' in Desc && !SameValue(Desc.value, current.value)) {
              throw new TypeError("The property '" + P +
                  "' is not writable.");
            }
          }
        }
        // b. else, the [[Configurable]] field of current is true, so
        //    any change is acceptable. (Skip to 12)
      }
      // 11. Else, IsAccessorDescriptor(current) and
      //     IsAccessorDescriptor(Desc) are both true so,
      else {
        // a. If the [[Configurable]] field of current is false, then
        if (!current.configurable) {
          // i. Reject, if the [[Set]] field of Desc is present and
          //    SameValue(Desc.[[Set]], current.[[Set]]) is false.
          // ii. Reject, if the [[Get]] field of Desc is present and
          //     SameValue(Desc.[[Get]], current.[[Get]]) is false.
          if (('set' in Desc && !SameValue(Desc.set, current.set)) ||
              ('get' in Desc && !SameValue(Desc.get, current.get))) {
            throw new TypeError("The property '" + P +
                "' is not configurable.");
          }
        }
      }
      // 12. For each attribute field of Desc that is present,
      //     set the correspondingly named attribute of the property
      //     named P of object O to the value of the field.
      if (iddDesc) {
        O[P] = Desc.value;
        O[P + '_v___'] = O;
        O[P + '_gw___'] = Desc.writable ? O : false;
        O[P + '_g___'] = O[P + '_s___'] = void 0;
      } else {
        O[P + '_v___'] = false;
        O[P + '_gw___'] = false;
        O[P + '_g___'] = Desc.get;
        O[P + '_s___'] = Desc.set;
      }
      O[P + '_e___'] = Desc.enumerable ? O : false;
      O[P + '_c___'] = Desc.configurable ? O : false;
      O[P + '_m___'] = false;
      O[P + '_w___'] = false;
      // 13. Return true.
      return true;
    };

  /**
   * 9 Type Conversion and Testing
   */

  // 9.6
  function ToUint32(input) {
    return input >>> 0;
  }

  // 9.9
  function ToObject(input) {
    if (input === void 0 || input === null) {
        throw new TypeError('Cannot convert ' + input + ' to Object.');
    }
    return Object(input);
  }

  // 9.12
  /**
   * Are x and y not observably distinguishable?
   */
  function SameValue(x, y) {
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
   * 11 Expressions
   */

  /**
   * Throws an exception if the value is an unmarked function.
   */
  function asFirstClass(value) {
    if (isFunction(value) && !value.ok___) {
      throw new Error('Internal: toxic function encountered!');
    }
    return value;
  }

  // 11.1.5
  /**
   * Creates a well-formed ES5 record from a list of alternating
   * keys and values.
   */
  function initializeMap(list) {
    var result = {};
    var accessors = {};
    var i;
    for (i = 0; i < list.length; i += 2) {
      if (typeof list[i] === 'string') {
        if (result.hasOwnProperty(list[i])) {
          throw new SyntaxError('Duplicate keys: ' + list[i]);
        }
        if (isNumericName(list[i])) {
          result[list[i]] = asFirstClass(list[i + 1]);
        } else {
          result.DefineOwnProperty___(
              list[i],
              {
                value: asFirstClass(list[i + 1]),
                writable: true,
                enumerable: true,
                configurable: true
              });
        }
      } else {
        var name = list[i][0];
        if (isNumericName(name)) {
          throw new TypeError('Accessors not supported for numerics.');
        }
        var type = list[i][1];
        accessors[name] = accessors[name] || {};
        if (accessors[name].hasOwnProperty(type)) {
          throw new SyntaxError('Duplicate accessor keys: ' +
              type + ' ' + list[i]);
        }
        accessors[name][type] = asFirstClass(list[i + 1]);
      }
    }
    for (i in accessors) {
      if (endsWith__.test(i)) { continue; }
      if (!accessors.hasOwnProperty(i)) { continue; }
      result.DefineOwnProperty___(i, {
          get: accessors[i].get,
          set: accessors[i].set,
          enumerable: true,
          configurable: true
        });
    }
    return result;
  }

  // 11.2.3
  /**
   * Makes a [[ThrowTypeError]] function, as defined in section 13.2.3
   * of the ES5 spec.
   *
   * <p>The informal name for the [[ThrowTypeError]] function, defined
   * in section 13.2.3 of the ES5 spec, is the "poison pill". The poison
   * pill is simply a no-argument function that, when called, always
   * throws a TypeError. Since we wish this TypeError to carry useful
   * diagnostic info, we violate the ES5 spec by defining 4 poison
   * pills with 4 distinct identities.
   *
   * <p>A poison pill is installed as the getter & setter of the
   * de-jure (arguments.callee) and de-facto non-strict magic stack
   * inspection properties, which no longer work in ES5/strict, since
   * they violate encapsulation. Rather than simply remove them,
   * access to these properties is poisoned in order to catch errors
   * earlier when porting old non-strict code.
   */
  function makePoisonPill(badThing) {
    function poisonPill() {
      throw new TypeError('' + badThing + ' is forbidden by ES5/strict');
    }
    return markFunc(poisonPill);
  }
  var poisonFuncCaller = makePoisonPill("A function's .caller");
  var poisonFuncArgs = makePoisonPill("A function's .arguments");

  /**
   * Function calls g(args) get translated to g.f___(USELESS, args)
   * Tamed functions and cajoled functions install an overriding fastpath f___
   * to apply, the original Function.prototype.apply.
   */
  Function.prototype.f___ = callFault;
  Function.prototype.i___ = function(var_args) {
      return this.f___(USELESS, slice.call(arguments, 0));
    };
  Function.prototype.new___ = callFault;
  Function.prototype.DefineOwnProperty___('arguments', {
      enumerable: false,
      configurable: false,
      get: poisonFuncArgs,
      set: poisonFuncArgs
    });
  Function.prototype.DefineOwnProperty___('caller', {
      enumerable: false,
      configurable: false,
      get: poisonFuncCaller,
      set: poisonFuncCaller
    });

  // 11.2.4
  var poisonArgsCallee = makePoisonPill('arguments.callee');
  var poisonArgsCaller = makePoisonPill('arguments.caller');

  /**
   * Given either an array or an actual arguments object, return
   * Cajita's emulation of an ES5/strict arguments object.
   */
  function args(original) {
    var result = initializeMap(['length', 0]);
    push.apply(result, original);
    result.CLASS___ = 'Arguments';
    result.DefineOwnProperty___(
        'callee',
        {
          enumerable: false,
          configurable: false,
          get: poisonArgsCallee,
          set: poisonArgsCallee
        });
    result.DefineOwnProperty___(
        'caller',
        {
          enumerable: false,
          configurable: false,
          get: poisonArgsCaller,
          set: poisonArgsCaller
        });
    return result;
  }

  // 11.8.7
  /**
   * Implements ES5's {@code <i>name</i> in <i>obj</i>}
   *
   * Precondition: name is a string
   */
  function isIn(name, obj) {
    var t = Type(obj);
    if (t !== 'Object') {
      throw new TypeError('Invalid "in" operand: ' + obj);
    }
    return obj.HasProperty___(name);
  }

  /**
   * 15 Standard Built-in ECMAScript Objects
   */

  // Sets up a per-object getter and setter.  Necessary to prevent
  // guest code from messing with expectations of host and innocent code.
  // If innocent code needs access to the guest properties, explicitly tame
  // it that way.
  function virtualize(obj, name, fun) {
    var vname = name + '_virt___';
    obj[vname] = fun ? markFunc(fun) : obj[name] ? markFunc(obj[name]) : void 0;
    obj.DefineOwnProperty___(name, {
        get: markFunc(function () {
            return this[vname];
          }),
        set: markFunc(function (val) {
            if (!isFunction(val)) {
              throw new TypeError('Expected a function instead of ' + val);
            }
            if (isFrozen(this)) {
              throw new TypeError('This object is frozen.');
            }
            if (!isExtensible(this) &&
                !this.hasOwnProperty(vname)) {
              throw new TypeError('This object is not extensible.');
            }
            this[vname] = asFirstClass(val);
          }),
        enumerable: false,
        configurable: false
      });
  }

  // 15.1.3.1--4
  markFunc(decodeURI);
  markFunc(decodeURIComponent);
  markFunc(encodeURI);
  markFunc(encodeURIComponent);

  // 15.2.1.1
  Object.f___ = markFunc(function (dis, as) {
      var len = as.length;
      if (len === 0 || as[0] === null || as[0] === void 0) {
        return {};
      }
      return ToObject(as[0]);
    });

  // 15.2.2.1
  Object.new___ = markFunc(function (value) {
      return Object.f___(USELESS, [value]);
    });

  // 15.2.3.1
  Object.DefineOwnProperty___('prototype', {
      value: Object.prototype,
      writable: false,
      enumerable: false,
      configurable: false
    });

  // 15.2.3.2
  // Prefer the browser's built-in version.
  if (!Object.getPrototypeOf) {
    Object.getPrototypeOf = function (obj) {
        if (Type(obj) !== 'Object') {
          throw new TypeError('Not an object.');
        }
        if (!Object.hasOwnProperty('Prototype___')) {
          // If there's no built-in version, fall back to __proto__.
          if ({}.__proto__ === Object.prototype) {
            obj.Prototype___ = obj.__proto__;
          } else {
            // If that fails, use directConstructor to give our best guess.
            var constr = directConstructor(obj);
            if (constr === BASE_OBJECT_CONSTRUCTOR) {
              obj.Prototype___ = obj.baseProto___;
            } else if (constr === void 0) {
              obj.Prototype___ = null;
            } else {
              obj.Prototype___ = constr.prototype;
            }
          }
        }
        return obj.Prototype___;
      };
  }
  origGetPrototypeOf = Object.getPrototypeOf;

  // The Chrome/Safari/Webkit debugger injects a script that expects to
  // call Object.getOwnPropertyNames and Object.getOwnPropertyDescriptor on
  // arbitrary objects from any frame.

  // The es53 implementations below only work on objects that inherit from
  // es53's Object, so we virtualize instead of overriding.

  // Note, the es5 spec says these should be configurable:true, but if we
  // do that, DefineOwnProperty___ will set the real value to undefined.

  // 15.2.3.3
  // This is the original implementation exposed to guest code,
  // which may change it.
  origGetOwnPropertyDescriptor = function (obj, P) {
      // 1. If Type(object) is not Object throw a TypeError exception.
      if (Type(obj) !== 'Object') {
        throw new TypeError('Expected an object.');
      }
      // 2. Let name be ToString(P).
      var name = '' + P;
      // 3. Let desc be the result of calling the [[GetOwnProperty]]
      //    internal method of obj with argument name.
      var desc = obj.GetOwnProperty___(name);
      // 4. Return the result of calling FromPropertyDescriptor(desc).
      return desc ? FromPropertyDescriptor(desc) : void 0;
    };
  virtualize(Object, 'getOwnPropertyDescriptor', origGetOwnPropertyDescriptor);

  // 15.2.3.4
  virtualize(Object, 'getOwnPropertyNames',
      function (obj) { return obj.ownKeys___(); });

  // 15.2.3.5
  /**
   * Makes a new empty object that directly inherits from {@code proto}.
   */
  function beget(proto) {
    if (proto === null) {
      throw new TypeError('Cannot beget from null.');
    }
    if (proto === (void 0)) {
      throw new TypeError('Cannot beget from undefined.');
    }
    function F() {}
    F.prototype = proto;
    var result = new F();
    return result;
  }

  // The algorithm below doesn't care whether Properties is absent
  // or undefined, so we can simplify.
  Object.create = function (O, opt_Properties) {
      // 1. If Type(O) is not Object or Null throw a TypeError exception.
      // (ES3 doesn't support null prototypes.)
      if (Type(O) !== 'Object') {
        throw new TypeError('Expected an object.');
      }
      // 2. Let obj be the result of creating a new object
      //    as if by the expression new Object() where Object
      //    is the standard built-in constructor with that name
      // 3. Set the [[Prototype]] internal property of obj to O.
      var obj = beget(O);
      // 4. If the argument Properties is present
      // and not undefined, add own properties to obj
      // as if by calling the standard built-in function
      // Object.defineProperties with arguments obj and Properties.
      if (opt_Properties !== void 0) {
        DefineProperties(obj, opt_Properties);
      }
      // 5. Return obj.
      return obj;
    };

  // 15.2.3.6
  Object.defineProperty = function (O, P, Attributes) {
      // 1. If Type(O) is not Object throw a TypeError exception.
      if (Type(O) !== 'Object') {
        throw new TypeError('Expected an object.');
      }
      // 2. Let name be ToString(P).
      var name = '' + P;
      // 3. Let desc be the result of calling
      //    ToPropertyDescriptor with Attributes as the argument.
      var desc = ToPropertyDescriptor(Attributes);
      // 4. Call the [[DefineOwnProperty]] internal method of O
      //    with arguments name, desc, and true.
      // (We don't need 'true' because we always throw in strict mode.)
      O.DefineOwnProperty___(name, desc);
      // 5. Return O.
      return O;
    };

  // 15.2.3.7
  function DefineProperties(O, Properties) {
    // 1. If Type(O) is not Object throw a TypeError exception.
    if (Type(O) !== 'Object') {
      throw new TypeError('Expected an object.');
    }
    // 2. Let props be ToObject(Properties).
    var props = ToObject(Properties);
    // 3. Let names be an internal list containing
    //    the names of each enumerable own property of props.
    var names = ownEnumKeys(props);
    // 4. Let descriptors be an empty internal List.
    var descriptors = [];
    // 5. For each element P of names in list order,
    var len = names.length;
    var i, P, desc;
    for (i = 0; i < len; ++i) {
      P = names[i];
      // a. Let descObj be the result of calling the [[Get]]
      //    internal method of props with P as the argument.
      var descObj = props.v___(P);
      // b. Let desc be the result of calling ToPropertyDescriptor
      //    with descObj as the argument.
      desc = ToPropertyDescriptor(descObj);
      // c. Append desc to the end of descriptors.
      descriptors.push(desc);
    }
    // 6. For each element desc of descriptors in list order,
      // a. Call the [[DefineOwnProperty]] internal method
      //    of O with arguments P, desc, and true.
    // This part of the spec is nonsense.  I'm following Besen's
    // interpretation: see line 31479 of
    // http://besen.svn.sourceforge.net/viewvc/besen/trunk/src/BESEN.pas?revision=27&view=markup

    // TODO: The latest draft errata fixes this. We'll be ratifying
    // these errata at the upcoming EcmaScript meeting on 7/28 &
    // 7/29. Watch this space.
    for (i = 0; i < len; ++i) {
      P = names[i];
      desc = descriptors[i];
      O.DefineOwnProperty___(P, desc);
    }
    // 7. Return O.
    return O;
  }
  Object.defineProperties = DefineProperties;

  // 15.2.3.8
  Object.seal = function (O) {
      // 1. If Type(O) is not Object throw a TypeError exception.
      if (Type(O) !== 'Object') {
        throw new TypeError('Only objects may be sealed.');
      }
      return O.seal___();
    };

  Object.prototype.seal___ = function () {
      // Allow function instances to install their instance properties
      // before sealing them.
      if (this.v___ === deferredV) {
        this.v___('length');
      }
      // 2. For each own property name P of this,
      for (var i in this) {
        if (!this.hasOwnProperty(i)) { continue; }
        if (isNumericName(i)) { continue; }
        var m = i.match(endsWith_v___);
        if (!m) { continue; }
        var P = m[1];
        // a. Let desc be the result of calling the [[GetOwnProperty]]
        //    internal method of this with P.
        // b. If desc.[[Configurable]] is true, set
        //    desc.[[Configurable]] to false.
        // c. Call the [[DefineOwnProperty]] internal method of this with P,
        //    desc, and true as arguments.
        this[P + '_c___'] = false;
      }
      if (!this.hasNumerics___()) {
        this.NUM____v___ = this;
        this.NUM____gw___ = this;
        this.NUM____w___ = this;
        this.NUM____m___ = false;
        this.NUM____e___ = this;
        this.NUM____g___ = void 0;
        this.NUM____s___ = void 0;
      }
      this.NUM____c___ = false;
      // 3. Set the [[Extensible]] internal property of this to false.
      this.ne___ = this;
      // 4. Return this.
      return this;
    };

  // 15.2.3.9
  /**
   * Whitelists all the object's own properties that do not
   * end in __ and have not already been whitelisted.
   * If opt_deep is true, recurses on objects and
   * assumes the object has no cycles through accessible keys.
   */
  function whitelistAll(obj, opt_deep) {
    var keys = fastOwnKeys(obj);
    for (var k = 0, n = keys.length; k < n; k++) {
      var i = keys[k], val = obj[i];
      if (i.substr(i.length - 2) !== '__' && !((i + '_v___') in obj)) {
        if (opt_deep && val && typeof val === 'object') {
          whitelistAll(val, true);
        }
        obj[i + '_v___'] = obj;
        obj[i + '_w___'] = false;
        obj[i + '_gw___'] = false;
        obj[i + '_e___'] = obj;
        obj[i + '_c___'] = false;
        obj[i + '_g___'] = void 0;
        obj[i + '_s___'] = void 0;
        obj[i + '_m___'] = false;
        if (val && val.f___ === Function.prototype.f___) {
          // inline isFunction(val)
          if (classProp.call(val) === '[object Function]') {
            markFunc(val);
          }
        } else {
          // inline isFunction(val)
          if (classProp.call(val) === '[object Function]') {
            val.ok___ = true;
          }
        }
      }
    }
    return obj;
  }

  // TODO: Where this is used, do we really want frozenness
  // that is transitive across property traversals?
  function snowWhite(obj) {
    return freeze(whitelistAll(obj));
  }
      
  function makeDefensibleFunction(f) {
    return markFuncFreeze(function(_) {
      return f.apply(USELESS, slice.call(arguments, 0));
    });
  }
  
  function makeDefensibleObject(descriptors) {
    var td = {};
    for (var k in descriptors) {
      if (!descriptors.hasOwnProperty(k)) { continue; }
      if (isNumericName(k)) { throw 'Cannot define numeric property: ' + k; }
      td.DefineOwnProperty___(k, {
        value: FromPropertyDescriptor(descriptors[k]),
        enumerable: true,
        writable: false
      }); 
    }
    return Object.seal(Object.create(Object.prototype, td));  
  }

  function freeze(obj) {
      if (Type(obj) !== 'Object') {
        throw new TypeError('Only objects may be frozen.');
      }
      return obj.freeze___();
  }
  Object.freeze = freeze;

  // 15.2.3.10
  Object.preventExtensions = function (obj) {
      if (Type(obj) !== 'Object') {
        throw new TypeError('Only objects may be made non-extensible.');
      }
      return obj.pe___();
    };

  Object.prototype.pe___ = function () {
      if (!this.hasNumerics___()) {
        this.NUM____v___ = this;
        this.NUM____e___ = this;
        this.NUM____g___ = void 0;
        this.NUM____s___ = void 0;
        this.NUM____c___ = this;
        this.NUM____gw___ = this;
        this.NUM____w___ = this;
        this.NUM____m___ = false;
      }
      this.ne___ = this;
      return this;
    };

  // 15.2.3.11
  Object.isSealed = function (O) {
      // 1. If Type(O) is not Object throw a TypeError exception.
      if (Type(O) !== 'Object') {
        throw new TypeError('Only objects may be frozen.');
      }
      // 2. For each named own property name P of O,
      // a. Let desc be the result of calling the [[GetOwnProperty]]
      //    internal method of O with P.
      // b. If desc.[[Configurable]] is true, then return false.
      for (var i in O) {
        if (endsWith__.test(i)) { continue; }
        if (!O.hasOwnProperty(i)) { continue; }
        if (isNumericName(i)) { continue; }
        if (O[i + '_c___']) { return false; }
      }
      // 3. If the [[Extensible]] internal property of O is false, then
      //    return true.
      if (O.ne___ === O) { return true; }
      // 4. Otherwise, return false.
      return false;
    };

  // 15.2.3.12
  Object.isFrozen = isFrozen;

  // 15.2.3.13
  Object.isExtensible = isExtensible;

  // 15.2.3.14
  // virtualized to avoid confusing the webkit/safari/chrome debugger
  virtualize(Object, 'keys', ownEnumKeys);
  // TODO(felix8a): ES5 says this should be configurable: true

  (function () {
    var objectStaticMethods = [
        'getPrototypeOf',
        // getOwnPropertyDescriptor is virtual
        // getOwnPropertyNames is virtual
        'create',
        'defineProperty',
        'defineProperties',
        'seal',
        'freeze',
        'preventExtensions',
        'isSealed',
        'isFrozen',
        'isExtensible'
        // keys is virtual
      ];
    var i, len = objectStaticMethods.length;
    for (i = 0; i < len; ++i) {
      var name = objectStaticMethods[i];
      Object.DefineOwnProperty___(name, {
          value: markFunc(Object[name]),
          writable: true,
          enumerable: false,
          configurable: true
        });
    }
  })();

  // 15.2.4.1
  Object.DefineOwnProperty___('constructor', {
      value: Object,
      writable: false,
      enumerable: false,
      configurable: false
    });

  // 15.2.4.2
  Object.prototype.toString = markFunc(function() {
      if (this.CLASS___) { return '[object ' + this.CLASS___ + ']'; }
      return classProp.call(this);
    });
  Object.prototype.DefineOwnProperty___('toString', {
      get: markFunc(function () {
        return this.toString.orig___ ? this.toString.orig___ : this.toString;
      }),
      set: markFunc(function (val) {
        if (!isFunction(val)) {
          throw new TypeError('Expected a function instead of ' + val);
        }
        if (isFrozen(this)) {
          throw new TypeError("Won't set toString on a frozen object.");
        }
        val = asFirstClass(val);
        this.toString = markFunc(function (var_args) {
            return val.f___(safeDis(this), arguments);
          });
        this.toString.orig___ = val;
      }),
      enumerable: false,
      configurable: false
    });

  // 15.2.4.4
  markFunc(Object.prototype.valueOf);
  Object.prototype.DefineOwnProperty___('valueOf', {
      get: markFunc(function () {
          return this.valueOf.orig___ ? this.valueOf.orig___ : this.valueOf;
        }),
      set: markFunc(function (val) {
          if (!isFunction(val)) {
            throw new TypeError('Expected a function instead of ' + val);
          }
          if (isFrozen(this)) {
            throw new TypeError("Won't set valueOf on a frozen object.");
          }
          val = asFirstClass(val);
          this.valueOf = markFunc(function (var_args) {
              return val.f___(safeDis(this), arguments);
            });
          this.valueOf.orig___ = val;
        }),
      enumerable: false,
      configurable: false
    });

  // 15.2.4.5
  virtualize(Object.prototype, 'hasOwnProperty', function (P) {
      if (isNumericName(P)) { return this.hasOwnProperty(P); }
      return this.HasOwnProperty___(P);
    });

  // 15.2.4.7
  virtualize(Object.prototype, 'propertyIsEnumerable', function (V) {
      return isEnumerable(this, '' + V);
    });

  // 15.2.4.3, 5--7
  virtualize(Object.prototype, 'toLocaleString');
  virtualize(Object.prototype, 'isPrototypeOf');

  // 15.2.4
  // NOT extensible under ES5/3
  freeze(Object.prototype);

  // 15.3 Function
  var FakeFunction = function () {
      throw new
          Error('Internal: FakeFunction should not be directly invocable.');
    };

  FakeFunction.toString = (function (str) {
      return function () {
          return str;
        };
    })(Function.toString());

  // 15.3.1
  Function.f___ = FakeFunction.f___ = markFunc(function() {
      throw new Error('Invoking the Function constructor is unsupported.');
    });

  // 15.3.2
  Function.new___ = FakeFunction.new___ = markFunc(function () {
      throw new Error('Constructing functions dynamically is unsupported.');
    });

  // 15.3.3.1
  FakeFunction.DefineOwnProperty___('prototype', {
      value: Function.prototype,
      writable: false,
      enumerable: false,
      configurable: false
    });

  // 15.3.4.1
  Function.prototype.DefineOwnProperty___('constructor', {
      value: FakeFunction,
      writable: true,
      enumerable: false,
      configurable: false
    });

  // 15.3.4.2
  (function () {
    var orig = Function.prototype.toString;
    Function.prototype.toString = markFunc(function () {
        if (this.toString___) { return this.toString___(); };
        return orig.call(this);
      });
    })();

  // 15.3.4.3--5
  virtualize(Function.prototype, 'call', function (dis, var_args) {
      return this.apply(safeDis(dis), slice.call(arguments, 1));
    });
  virtualize(Function.prototype, 'apply', function (dis, as) {
      return this.apply(safeDis(dis), as ? slice.call(as, 0) : void 0);
    });
  /**
   * Bind this function to <tt>self</tt>, which will serve
   * as the value of <tt>this</tt> during invocation. Curry on a
   * partial set of arguments in <tt>var_args</tt>. Return the curried
   * result as a new function object.
   */
  Function.prototype.bind = markFunc(function(self, var_args) {
      var thisFunc = safeDis(this);
      var leftArgs = slice.call(arguments, 1);
      function funcBound(var_args) {
        var args = leftArgs.concat(slice.call(arguments, 0));
        return thisFunc.apply(safeDis(self), args);
      }
      // 15.3.5.2
      delete funcBound.prototype;
      funcBound.f___ = funcBound.apply;
      funcBound.new___ = function () {
          throw "Constructing the result of a bind() not yet implemented.";
        };
      funcBound.ok___ = thisFunc.ok___;
      return funcBound;
    });
  virtualize(Function.prototype, 'bind');

  // 15.4 Array

  // 15.4.1--2
  markFunc(Array);

  // 15.4.3.1
  Array.DefineOwnProperty___('prototype', {
      value: Array.prototype,
      writable: false,
      enumerable: false,
      configurable: false
    });

  // 15.4.3.2
  Array.isArray = markFunc(isArray);
  Array.DefineOwnProperty___('isArray', {
      value: Array.isArray,
      writable: true,
      enumerable: false,
      configurable: true
    });

  // Array.slice
  virtualize(Array, 'slice');

  // 15.4.4.1
  Array.prototype.DefineOwnProperty___('constructor', {
      value: Array,
      writable: true,
      enumerable: false,
      configurable: false
    });

  // 15.4.4.2
  markFunc(Array.prototype.toString);

  // 15.4.4.3--6
  (function () {
    var methods = [
        'toLocaleString',
        'concat',
        'join',
        'pop'
      ];
    for (var i = 0, len = methods.length; i < len; ++i) {
      virtualize(Array.prototype, methods[i]);
    }
  })();

  // 15.4.4.7--9

  // Array generics can add a length property; static accesses are
  // whitelisted by the cajoler, but dynamic ones need this.
  function whitelistLengthIfItExists(dis) {
    if (('length' in dis) && !('length_v___' in dis)) {
      dis.DefineOwnProperty___('length', {
          value: dis.length,
          writable: true,
          configurable: true,
          enumerable: true
        });
    }
  }

  function guardedVirtualize(obj, name) {
    var orig = obj[name];
    virtualize(obj, name, function (var_args) {
        if (!isExtensible(this)) {
          throw new TypeError("This object is not extensible.");
        }
        var dis = safeDis(this);
        var result = orig.apply(dis, arguments);
        whitelistLengthIfItExists(dis);
        return result;
      });
  }

  (function () {
    // Reverse can create own numeric properties.
    var methods = [
        'push',
        'reverse',
        'shift',
        'splice',
        'unshift'
      ];
    for (var i = 0, len = methods.length; i < len; ++i) {
      guardedVirtualize(Array.prototype, methods[i]);
    }
  })();

  // 15.4.4.10
  virtualize(Array.prototype, 'slice');

  // 15.4.4.11
  virtualize(Array.prototype, 'sort', function (comparefn) {
      // This taming assumes that sort only modifies {@code this},
      // even though it may read numeric properties on the prototype chain.
      if (!isWritable(this, 'NUM___')) {
        throw new TypeError(
            'Cannot sort an object whose ' +
            'numeric properties are not writable.');
      }
      if (!isExtensible(this)) {
        throw new TypeError(
            'Cannot sort an object that is not extensible.');
      }
      var result = (comparefn ?
          Array.prototype.sort.call(
              this,
              markFunc(function(var_args){
                return comparefn.f___(this, arguments);
              })
            ) :
          Array.prototype.sort.call(this));
      whitelistLengthIfItExists(this);
      return result;
    });

  // 15.4.4.14
  Array.prototype.indexOf = markFunc(function (value, fromIndex) {
      // length is always readable
      var len = this.length >>> 0;
      if (!len) { return -1; }
      var i = fromIndex || 0;
      if (i >= len) { return -1; }
      if (i < 0) { i += len; }
      for (; i < len; i++) {
        if (!this.hasOwnProperty(i)) {
          continue;
        }
        // Numerics are always readable
        if (value === this[i]) { return i; }
      }
      return -1;
    });
  virtualize(Array.prototype, 'indexOf');

  // 15.4.4.15
  Array.prototype.lastIndexOf = function (value, fromIndex) {
      // length is always readable
      var len = this.length;
      if (!len) { return -1; }
      var i = arguments[1] || len;
      if (i < 0) { i += len; }
      i = Math.min___(i, len - 1);
      for (; i >= 0; i--) {
        if (!this.hasOwnProperty(i)) {
          continue;
        }
        if (value === this[i]) { return i; }
      }
      return -1;
    };
  virtualize(Array.prototype, 'lastIndexOf');

  // For protecting methods that use the map-reduce API against
  // inner hull breaches. For example, we don't want cajoled code
  // to be able to use {@code every} to invoke a toxic function as
  // a filter, for instance.
  //
  // {@code fun} must not be marked as callable.
  // {@code fun} expects
  // - a function {@code block} to use (like the filter in {@code every})
  // - an optional object {@code thisp} to use as {@code this}
  // It wraps {@code block} in a function that invokes its taming.
  function createOrWrap(obj, name, fun) {
    virtualize(obj, name);
    var vname = name + '_virt___';
    if (!obj[name]) {
      // Create
      obj[vname] = fun;
    } else {
      // Wrap
      obj[vname] = (function (orig) {
          return function (block) { //, thisp
              var a = slice.call(arguments, 0);
              // Replace block with the taming of block
              a[0] = markFunc(function(var_args) {
                  return block.f___(this, arguments);
                });
              // Invoke the original function on the tamed
              // {@code block} and optional {@code thisp}.
              return orig.apply(this, a);
            };
        })(obj[name]);
    }
    markFunc(obj[vname]);
  }

  // 15.4.4.16
  createOrWrap(Array.prototype, 'every', function (block, thisp) {
      var len = this.length >>> 0;
      for (var i = 0; i < len; i++) {
        if (!block.f___(thisp, [this[i]])) { return false; }
      }
      return true;
    });

  // 15.4.4.17
  createOrWrap(Array.prototype, 'some', function (block, thisp) {
      var len = this.length >>> 0;
      for (var i = 0; i < this.length; i++) {
        if (block.f___(thisp, [this[i]])) { return true; }
      }
      return false;
    });

  // 15.4.4.18
  virtualize(Array.prototype, 'forEach', function (block, thisp) {
      var len = this.length >>> 0;
      for (var i = 0; i < len; i++) {
        if (i in this) {
          block.f___(thisp, [this[i], i, this]);
        }
      }
    });

  // 15.4.4.19
  // https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Objects/Array/map
  createOrWrap(Array.prototype, 'map', function (fun, thisp) {
      var len = this.length >>> 0;
      if (!isFunction(fun)) {
        throw new TypeError('Expected a function instead of ' + fun);
      }
      var res = new Array(len);
      for (var i = 0; i < len; i++) {
        if (i in this) {
          res[i] = fun.f___(thisp, [this[i], i, this]);
        }
      }
      return res;
    });

  // 15.4.4.20
  createOrWrap(Array.prototype, 'filter', function (block, thisp) {
      var values = [];
      var len = this.length >>> 0;
      for (var i = 0; i < len; i++) {
        if (block.f___(thisp, [this[i]])) {
          values.push(this[i]);
        }
      }
      return values;
    });

  // 15.4.4.21
  // https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Objects/Array/reduce
  createOrWrap(Array.prototype, 'reduce', function(fun) { // , initial
      // {@code fun} is of the form
      // function(previousValue, currentValue, index, array) { ... }
      var len = this.length >>> 0;
      if (!isFunction(fun)) {
        throw new TypeError('Expected a function instead of ' + fun);
      }
      // no value to return if no initial value and an empty array
      if (len === 0 && arguments.length === 1) {
        throw new TypeError('Expected an initial value or a non-empty array.');
      }
      var i = 0;
      var rv = void 0;
      if (arguments.length >= 2) {
        rv = arguments[1];
      } else {
        do {
          if (i in this) {
            rv = this[i++];
            break;
          }
          // if array contains no values, no initial value to return
          if (++i >= len) {
            throw new TypeError('Expected non-empty array.');
          }
        } while (true);
      }
      for (; i < len; i++) {
        if (i in this) {
          rv = fun.f___(USELESS, [rv, this[i], i, this]);
        }
      }
      return rv;
    });

  // 15.4.4.22
  // https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Objects/Array/reduceRight
  createOrWrap(Array.prototype, 'reduceRight', function(fun) { // , initial
      var len = this.length >>> 0;
      if (!isFunction(fun)) {
        throw new TypeError('Expected a function instead of ' + fun);
      }
      // no value to return if no initial value, empty array
      if (len === 0 && arguments.length === 1) {
        throw new TypeError('Expected an initial value or a non-empty array.');
      }
      var i = len - 1;
      var rv = void 0;
      if (arguments.length >= 2) {
        rv = arguments[1];
      } else {
        do {
          if (i in this) {
            rv = this[i--];
            break;
          }
          // if array contains no values, no initial value to return
          if (--i < 0) {
            throw new TypeError('Expected a non-empty array.');
          }
        } while (true);
      }
      for (; i >= 0; i--) {
        if (i in this) {
          rv = fun.f___(USELESS, [rv, this[i], i, this]);
        }
      }
      return rv;
    });

  // 15.5 String

  // 15.5.1--2
  markFunc(String);

  // 15.5.3.1
  String.DefineOwnProperty___('prototype', {
      value: String.prototype,
      writable: false,
      enumerable: false,
      configurable: false
    });

  // 15.5.3.2
  virtualize(String, 'fromCharCode');

  // 15.5.4.1
  String.prototype.DefineOwnProperty___('constructor', {
      value: String.prototype.constructor,
      writable: true,
      enumerable: false,
      configurable: false
    });

  // 15.5.4.2
  markFunc(String.prototype.toString);

  // 15.5.4.3
  markFunc(String.prototype.valueOf);

  // 15.5.4.4--9, 13, 15--20
  // and the nonstandard but universally implemented substr.
  (function () {
    var methods = [
        'charAt',
        'charCodeAt',
        'concat',
        'indexOf',
        'lastIndexOf',
        'localeCompare',
        'slice',
        'substring',
        'toLowerCase',
        'toLocaleLowerCase',
        'toUpperCase',
        'toLocaleUpperCase',
        'substr'
      ];
    var i, len = methods.length;
    for (i = 0; i < len; ++i) {
      virtualize(String.prototype, methods[i]);
    }
  })();

  // 15.5.4.10, 12, 14
  /**
   * Verifies that regexp is something that can appear as a
   * parameter to a Javascript method that would use it in a match.
   * <p>
   * If it is a RegExp, then this match might mutate it, which must
   * not be allowed if regexp is frozen.
   *
   * Returns: a boolean indicating whether {@code regexp} should be
   * cast to a String
   */
  function enforceMatchable(regexp) {
    if (regexp instanceof RegExp) {
      if (isFrozen(regexp)) {
        throw new Error("Can't match with frozen RegExp: " + regexp);
      }
      return false;
    }
    return true;
  }

  function tameStringRegExp(orig) {
    return markFunc(function (regexp) {
        var cast = enforceMatchable(regexp);
        return orig.call(this, cast ? ('' + regexp) : regexp);
      });
  }

  (function () {
    var methods = [
        'match',
        'search',
        'split'
      ];
    for (var i = 0, len = methods.length; i < len; ++i) {
      virtualize(
          String.prototype,
          methods[i],
          tameStringRegExp(String.prototype[methods[i]]));
    }
  })();

  // 15.5.4.11
  virtualize(String.prototype, 'replace', function (searcher, replacement) {
      var cast = enforceMatchable(searcher);
      if (isFunction(replacement)) {
        replacement = asFirstClass(replacement);
      } else {
        replacement = '' + replacement;
      }
      return String.prototype.replace.call(
          this,
          cast ? ('' + searcher) : searcher,
          replacement
        );
    });

  // 15.5.4.20
  // http://blog.stevenlevithan.com/archives/faster-trim-javascript
  var trimBeginRegexp = /^\s\s*/;
  var trimEndRegexp = /\s\s*$/;
  virtualize(String.prototype, 'trim', function () {
      return ('' + this).
          replace(trimBeginRegexp, '').
          replace(trimEndRegexp, '');
    });

  // 15.6 Boolean

  // 15.6.1--2
  markFunc(Boolean);

  // 15.6.3.1
  Boolean.DefineOwnProperty___('prototype', {
      value: Boolean.prototype,
      writable: false,
      enumerable: false,
      configurable: false
    });

  // 15.6.4.1
  Boolean.prototype.DefineOwnProperty___('constructor', {
      value: Boolean.prototype.constructor,
      writable: true,
      enumerable: false,
      configurable: false
    });

  // 15.7 Number

  // 15.7.1--2
  markFunc(Number);

  // 15.7.3.1--6
  (function () {
    var props = [
        'prototype',
        'MAX_VALUE',
        'MIN_VALUE',
        // 'NaN' is automatically readable since it's a numeric property
        'NEGATIVE_INFINITY',
        'POSITIVE_INFINITY'
      ];
    var i, len = props.length;
    for (i = 0; i < len; ++i) {
      Number.DefineOwnProperty___(props[i], {
          value: Number[props[i]],
          writable: false,
          enumerable: false,
          configurable: false
        });
    }
  })();

  // 15.7.4.1
  Number.prototype.DefineOwnProperty___('constructor', {
      value: Number.prototype.constructor,
      writable: true,
      enumerable: false,
      configurable: false
    });

  // 15.7.4.2
  markFunc(Number.prototype.toString);

  // 15.7.4.4
  markFunc(Number.prototype.valueOf);

  // 15.7.4.3, 5--7
  (function (){
    var methods = [
        'toLocaleString',
        'toFixed',
        'toExponential',
        'toPrecision'
      ];
    var i, len = methods.length;
    for (i = 0; i < len; ++i) {
      virtualize(Number.prototype, methods[i]);
    }
  })();

  // 15.8 Math

  // 15.8.1.1--8
  (function (){
    var props = [
        'E',
        'LN10',
        'LN2',
        'LOG2E',
        'LOG10E',
        'PI',
        'SQRT1_2',
        'SQRT2'
      ];
    var i, len = props.length;
    for (i = 0; i < len; ++i) {
      Math.DefineOwnProperty___(props[i], {
          value: Math[props[i]],
          writable: false,
          enumerable: false,
          configurable: false
        });
    }
  })();

  // 15.8.2.1--18
  (function (){
    var methods = [
        'abs',
        'acos',
        'asin',
        'atan',
        'atan2',
        'ceil',
        'cos',
        'exp',
        'floor',
        'log',
        'max',
        'min',
        'pow',
        'random',
        'round',
        'sin',
        'sqrt',
        'tan'
      ];
    var i, len = methods.length;
    for (i = 0; i < len; ++i) {
      virtualize(Math, methods[i]);
    }
  })();

  // 15.9 Date

  // 15.9.1--3
  markFunc(Date);

  // 15.9.4.1
  Date.DefineOwnProperty___('prototype', {
      value: Date.prototype,
      writable: false,
      enumerable: false,
      configurable: false
    });

  // 15.9.4.2--4
  (function () {
    var staticMethods = [
        'parse',
        'UTC',
        'now'
      ];
    var i, len = staticMethods.length;
    for (i = 0; i < len; ++i) {
      virtualize(Date, staticMethods[i]);
    }
  })();

  // 15.9.5.1
  Date.prototype.DefineOwnProperty___('constructor', {
      value: Date.prototype.constructor,
      writable: true,
      enumerable: false,
      configurable: false
    });

  // 15.9.5.2
  markFunc(Date.prototype.toString);

  // 15.9.5.8
  markFunc(Date.prototype.valueOf);

  // 15.9.5.3--7, 9--44
  (function () {
    var methods = [
        'toDateString',
        'toTimeString',
        'toLocaleString',
        'toLocaleDateString',
        'toLocaleTimeString',
        'getTime',
        'getFullYear',
        'getMonth',
        'getDate',
        'getDay',
        'getHours',
        'getMinutes',
        'getSeconds',
        'getUTCSeconds',
        'getUTCMinutes',
        'getUTCHours',
        'getUTCDay',
        'getUTCDate',
        'getUTCMonth',
        'getUTCFullYear',
        'getMilliseconds',
        'getTimezoneOffset',
        'setFullYear',
        'setMonth',
        'setDate',
        'setHours',
        'setMinutes',
        'setSeconds',
        'setMilliseconds',
        'setTime',
        'toISOString',
        'toJSON'
      ];
    for (var i = 0; i < methods.length; ++i) {
      virtualize(Date.prototype, methods[i]);
    }
  })();

  // 15.10 RegExp

  // 15.10.5
  RegExp.f___ = markFunc(function (dis___, as) {
      var pattern = as[0], flags = as[1];
      if (classProp.call(pattern) === '[object RegExp]'
          && flags === void 0) {
        return pattern;
      }
      switch (as.length) {
        case 0:
          return new RegExp.new___();
        case 1:
          return new RegExp.new___(pattern);
        default:
          return new RegExp.new___(pattern, flags);
      }
    });

  RegExp.new___ = markFunc(function (pattern, flags){
      var re;
      switch (arguments.length) {
        case 0:
          re = new RegExp();
          break;
        case 1:
          re = new RegExp(pattern);
          break;
        default:
          re = new RegExp(pattern, flags);
      }
      var instanceProps = [
          'source',
          'global',
          'ignoreCase',
          'multiline'
        ];
      for (var i = 0; i < instanceProps.length; ++i) {
        re.DefineOwnProperty___(instanceProps[i], {
            value: re[instanceProps[i]],
            writable: false,
            enumerable: false,
            configurable: false
          });
      }
      re.DefineOwnProperty___('lastIndex', {
          value: re.lastIndex,
          writable: true,
          enumerable: false,
          configurable: false
        });
      return re;
    });

  // 15.10.5.1
  RegExp.DefineOwnProperty___('prototype', {
      value: RegExp.prototype,
      writable: false,
      enumerable: false,
      configurable: false
    });

  RegExp.prototype.DefineOwnProperty___('constructor', {
      value: RegExp,
      writable: true,
      enumerable: false,
      configurable: false
    });

  // Invoking exec and test with no arguments uses ambient data,
  // so we force them to be called with an argument, even if undefined.

  // 15.10.6.2
  virtualize(RegExp.prototype, 'exec', function (specimen) {
      return RegExp.prototype.exec.call(safeDis(this), specimen);
    });

  // 15.10.6.3
  virtualize(RegExp.prototype, 'test', function (specimen) {
      return RegExp.prototype.test.call(safeDis(this), specimen);
    });


  // 15.11 Error

  // 15.11.1, 2
  markFunc(Error);

  // 15.11.3.1
  Error.DefineOwnProperty___('prototype', {
      value: Error.prototype,
      enumerable: false,
      configurable: false,
      writable: true
    });

  // 15.11.4.1
  Error.prototype.DefineOwnProperty___('constructor', {
      value: Error,
      enumerable: false,
      configurable: false,
      writable: true
    });

  // 15.11.4.2
  Error.prototype.DefineOwnProperty___('name', {
      value: 'Error',
      enumerable: false,
      configurable: false,
      writable: true
    });

  // 15.11.4.3
  Error.prototype.DefineOwnProperty___('message', {
      value: '',
      enumerable: false,
      configurable: false,
      writable: true
    });

  // 15.11.4.4
  markFunc(Error.prototype.toString);

  // 15.11.6
  markFunc(EvalError);
  markFunc(RangeError);
  markFunc(ReferenceError);
  markFunc(SyntaxError);
  markFunc(TypeError);
  markFunc(URIError);

  ////////////////////////////////////////////////////////////////////////
  // ArrayLike
  ////////////////////////////////////////////////////////////////////////

  // makeArrayLike() produces a constructor for the purpose of taming
  // things like nodeLists.  The result, ArrayLike, takes an instance of 
  // ArrayLike and two functions, getItem and getLength, which put
  // it in a position to do taming on demand.
  var makeArrayLike, itemMap = WeakMap(), lengthMap = WeakMap();
  var lengthGetter = markFuncFreeze(function () {
      var getter = lengthMap.get(this);
      return getter ? getter.i___() : void 0;
    });
  freeze(lengthGetter.prototype);

  var nativeProxies = Proxy && (function () {
      var obj = {0: 'hi'};
      var p = Proxy.create({
          get: function () {
            var P = arguments[0];
            if (typeof P !== 'string') { P = arguments[1]; }
            return obj[P];
          }
        });
      return p[0] === 'hi';
    })();
  var numericGetters = (function () {
      var obj = {};
      try {
        defProp(obj, 0, {
            get: function () { return obj; }
          });
        if (obj[0] !== obj) { return false; }
      }
      catch (_) { return false; }
      return true;
    })();

  if (nativeProxies) { (function () {
    // Make ArrayLike.prototype be a native proxy object that intercepts
    // lookups of numeric properties and redirects them to getItem, and
    // similarly for length.
    // TODO: provide ArrayLike.prototype.constructor
    ArrayLike = markFunc(function(proto, getItem, getLength) {
        if (Type(proto) !== 'Object') {
          throw new TypeError('Expected proto to be an object.');
        }
          if (!(proto instanceof ArrayLike)) {
            throw new TypeError('Expected proto to be instanceof ArrayLike.');
          }
        var obj = beget(proto);
        itemMap.set(obj, getItem);
        lengthMap.set(obj, getLength);
        return obj;
      });

    // These are the handler methods for the proxy.
    var propDesc = function (P) {
        var opd = ownPropDesc(P);
        if (opd) {
          return opd;
        } else {
          return gopd(Object.prototype, P);
        }
      };
    var ownPropDesc = function (P) {
        // If P is 'length' or a number, handle the lookup; otherwise
        // pass it on to Object.prototype.
        P = '' + P;
        if (P === 'length') {
          return { get: lengthGetter };
        } else if (isNumericName(P)) {
          var get = markFuncFreeze(function () {
              var getter = itemMap.get(this);
              return getter ? getter.i___(+P) : void 0;
            });
          freeze(get.prototype);
          return {
              get: get,
              enumerable: true,
              configurable: true
            };
        }
        return void 0;
      };
    var has = function (P) {
        // The proxy has a length, numeric indices, and behaves
        // as though it inherits from Object.prototype.
        P = '' + P;
        return (P === 'length') || 
            isNumericName(P) || 
            P in Object.prototype;
      };
    var hasOwn = function (P) {
        // The proxy has a length and numeric indices.
        P = '' + P;
        return (P === 'length') || 
            isNumericName(P);
      };
    var gpn = function () {
        var result = gopn ();
        var objPropNames = Object.getOwnPropertyNames(Object.prototype);
        result.push.apply(result, objPropNames);
        return result;
      };
    var gopn = function () {
        var lenGetter = lengthMap.get(this);
        if (!lenGetter) { return void 0; }
        var len = lenGetter.i___();
        var result = ['length'];
        for (var i = 0; i < len; ++i) {
          result.push('' + i);
        }
        return result;
      };
    var del = function (P) {
        P = '' + P;
        if ((P === 'length') || ('' + +P === P)) { return false; }
        return true;
      };

    ArrayLike.prototype = Proxy.create({
        getPropertyDescriptor: propDesc,
        getOwnPropertyDescriptor: ownPropDesc,
        has: has,
        hasOwn: hasOwn,
        getPropertyNames: gpn,
        getOwnPropertyNames: gopn,
        'delete': del,
        fix: function() { return void 0; }
      },
      Object.prototype);
    ArrayLike.DefineOwnProperty___('prototype', {
        value: ArrayLike.prototype
      });
    freeze(ArrayLike);
    makeArrayLike = markFunc(function () { return ArrayLike; });
  })();} else if (numericGetters) { (function () {
    // Make ArrayLike.prototype be an object with a fixed set of numeric
    // getters.  To tame larger lists, replace ArrayLike and its prototype
    // using makeArrayLike(newLength).

    // See http://graphics.stanford.edu/~seander/bithacks.html#RoundUpPowerOf2
    function nextUInt31PowerOf2(v) {
      v &= 0x7fffffff;
      v |= v >> 1;
      v |= v >> 2;
      v |= v >> 4;
      v |= v >> 8;
      v |= v >> 16;
      return v + 1;
    }

    // The current function whose prototype has the most numeric getters.
    var BiggestArrayLike = void 0;
    var maxLen = 0;
    makeArrayLike = markFunc(function(length) {
        if (!BiggestArrayLike || length > maxLen) {
          var len = nextUInt31PowerOf2(length);
          // Create a new ArrayLike constructor to replace the old one.
          var BAL = markFunc(function(proto, getItem, getLength) {
              if (Type(proto) !== 'Object') {
                throw new TypeError('Expected proto to be an object.');
              }
              if (!(proto instanceof BAL)) {
                throw new TypeError(
                    'Expected proto to be instanceof ArrayLike.');
              }
              var obj = beget(proto);
              itemMap.set(obj, getItem);
              lengthMap.set(obj, getLength);
              return obj;
            });
          // Install native numeric getters.
          for (var i = 0; i < len; i++) {
            (function(j) {
              defProp(BAL.prototype, j, {
                  get: markFuncFreeze(function() {
                    var itemGetter = itemMap.get(this);
                    return itemGetter ? itemGetter.i___(j) : void 0;
                  }),
                  enumerable: true
                });
            })(i);
          }
          // Install native length getter.
          defProp(BAL.prototype, 'length', { get: lengthGetter });
          // Whitelist prototype and prototype.constructor for ES5/3.
          BAL.DefineOwnProperty___('prototype', { value: BAL.prototype });
          BAL.prototype.DefineOwnProperty___('constructor', { value: BAL });
          // Freeze and cache the result
          freeze(BAL);
          freeze(BAL.prototype);
          BiggestArrayLike = BAL;
          maxLen = len;
        }
        return BiggestArrayLike;
      });
  })(); } else {
    // ArrayLike constructs a frozen array in the absence of better support.
    ArrayLike = markFunc(function(proto, getItem, getLength) {
        if (Type(proto) !== 'Object') {
          throw new TypeError("Expected proto to be an object.");
        }
        var obj = beget(proto);
        var len = +getLength.i___();
        obj.DefineOwnProperty___('length', {
            value: len
          });
        for (var i = 0; i < len; ++i) {
          obj[i] = getItem.i___(i);
        }
        // Install the NUM___ flags
        isWritable(obj, 'NUM___');
        // Make numeric indices read-only
        obj.NUM____w___ = false;
        return obj;
      });
    ArrayLike.DefineOwnProperty___('prototype', {
        value: ArrayLike.prototype
      });
    ArrayLike.prototype.DefineOwnProperty___('constructor', {
        value: ArrayLike
      });
    freeze(ArrayLike);
    freeze(ArrayLike.prototype);
    makeArrayLike = markFunc(function () { return ArrayLike; });
  }

  ////////////////////////////////////////////////////////////////////////
  // Proxies
  ////////////////////////////////////////////////////////////////////////

  // Default implementations for derived traps invoke code supplied by
  // the guest on objects supplied by the guest, so we have to be careful.
  var defaultDerivedTraps = {
      has: function(name, proxy) {
          var dis = safeDis(this);
          return !!(dis.getPropertyDescriptor_m___ ?
              dis.getPropertyDescriptor(name, proxy) :
              dis.m___('getPropertyDescriptor', [name, proxy]));
        },
      hasOwn: function(name, proxy) {
          var dis = safeDis(this);
          return !!(dis.getOwnPropertyDescriptor_m___ ?
              dis.getOwnPropertyDescriptor(name, proxy) :
              dis.m___('getOwnPropertyDescriptor', [name, proxy]));
        },
      get: function(name, proxy) {
          var dis = safeDis(this);
          var desc = dis.getPropertyDescriptor_m___ ?
              dis.getPropertyDescriptor(name) :
              dis.m___('getPropertyDescriptor', [name]);
          if (desc === void 0) { return void 0; }
          if ('value_v___' in desc) {
            return desc.value_v___ ? desc.value : desc.v___('value');
          } else {
            var get = desc.get_v___ ? desc.get : desc.v___('get');
            if (!isFunction(get)) {
              return void 0;
            }
            return get.f___(safeDis(proxy), []);
          }
        },
      set: function(name, val, proxy) {
          var dis = safeDis(this);
          var desc = dis.getOwnPropertyDescriptor_m___ ?
              dis.getOwnPropertyDescriptor(name) :
              dis.m___('getOwnPropertyDescriptor', [name]);
          var set;
          if (desc) {
            if ('writable_v___' in desc) { // data
              if (desc.writable_v___ ?
                  desc.writable :
                  desc.v___('writable')) {
                desc.value_w___ === desc ?
                    desc.value = val :
                    desc.w___('value', val);
                dis.defineProperty_m___ ?
                    dis.defineProperty(name, desc) :
                    dis.m___('defineProperty', [name, desc]);
                return true;
              } else {
                return false;
              }
            } else { // accessor
              set = desc.set_v___ ? desc.set : desc.v___('set');
              if (isFunction(set)) {
                set.f___(safeDis(proxy), [val]);
                return true;
              } else {
                return false;
              }
            }
          }
          desc = dis.getPropertyDescriptor_m___ ?
              dis.getPropertyDescriptor(name) :
              dis.m___('getPropertyDescriptor', [name]);
          if (desc) {
            if ('writable_v___' in desc) { // data
              if (desc.writable_v___ ?
                  desc.writable :
                  desc.v___('writable')) {
                // fall through
              } else {
                return false;
              }
            } else { // accessor
              set = desc.set_v___ ? desc.set : desc.v___('set');
              if (isFunction(set)) {
                set.f___(safeDis(proxy), [val]);
                return true;
              } else {
                return false;
              }
            }
          }
          desc = initializeMap([
              'value', val,
              'writable', true,
              'enumerable', true,
              'configurable', true]);
          dis.defineProperty_m___ ?
              dis.defineProperty(name, desc) :
              dis.m___('defineProperty', [name, desc]);
          return true;
        },
      enumerate: function(proxy) {
          var dis = safeDis(this);
          var names = dis.getPropertyNames_m___ ?
              dis.getPropertyNames(proxy) :
              dis.m___('getPropertyNames', [proxy]);
          var filter = markFunc(function (name) {
                var desc = dis.getPropertyDescriptor_m___ ?
                    dis.getPropertyDescriptor(name, proxy) :
                    dis.m___('getPropertyDescriptor', [name, proxy]);
                return desc.enumerable_v___ ?
                    desc.enumerable :
                    desc.v___('enumerable');
            });
          return names.filter_m___ ?
              names.filter(filter) :
              names.m___('filter', [filter]);
        },
      keys: function(proxy) {
          var dis = safeDis(this);
          var names = dis.getOwnPropertyNames_m___ ?
              dis.getOwnPropertyNames(proxy) :
              dis.m___('getOwnPropertyNames', [proxy]);
          var filter = markFunc(function (name) {
                var desc = dis.getOwnPropertyDescriptor_m___ ?
                    dis.getOwnPropertyDescriptor(name, proxy) :
                    dis.m___('getOwnPropertyDescriptor', [name, proxy]);
                return desc.enumerable_v___ ?
                    desc.enumerable :
                    desc.v___('enumerable');
            });
          return names.filter_m___ ?
              names.filter(filter) :
              names.m___('filter', [filter]);
        }
    };

  function prepareProxy(proxy, handler) {
    proxy.proxy___ = proxy;
    proxy.v___ = function (P) {
        if (this !== proxy) {
          throw new TypeError(
              'Inheritance from proxies not implemented yet.');
        }
        P = '' + P;
        // Inline numeric name check
        if ('' + +P === P) { return proxy[P]; }
        assertValidPropertyName(P);
        var get = handler.get_v___ ? handler.get : handler.v___('get');
        if (isFunction(get)) {
          return get.f___(handler, [P, proxy]);
        } else {
          return defaultDerivedTraps.get.apply(handler, [P, proxy]);
        }
      };
    proxy.w___ = function (P, V) {
        if (this !== proxy) {
          throw new TypeError(
              'Inheritance from proxies not implemented yet.');
        }
        P = '' + P;
        V = asFirstClass(V);
        // Inline numeric name check
        if ('' + +P === P) { return proxy[P] = V; }
        assertValidPropertyName(P);
        var result;
        var set = handler.set_v___ ? handler.set : handler.v___('set');
        if (isFunction(set)) {
          result = set.f___(handler, [P, V, proxy]);
        } else {
          result = defaultDerivedTraps.set.apply(handler, [P, V, proxy]);
        }
        if (!result) { throw new TypeError('Failed to set ' + P); }
        return V;
      };
    proxy.c___ = function (P) {
        if (this !== proxy) {
          throw new TypeError(
              'Inheritance from proxies not implemented yet.');
        }
        P = '' + P;
        if ('' + +P === P) {
          // Allow deleting numeric properties since we allow get & set.
          delete proxy[P];
        }
        assertValidPropertyName(P);
        var deleter = handler.delete_v___ ? handler['delete'] :
            handler.v___('delete');
        var result = deleter.f___(handler, [P, proxy]);
        if (!result) { throw new TypeError('Unable to delete ' + P); }
        return true;
      };
    proxy.m___ = function (P, args) {
        if (this !== proxy) {
          throw new TypeError(
              'Inheritance from proxies not implemented yet.');
        }
        // First look it up, then call it.
        P = '' + P;
        var method;
        // Inline numeric name check
        if ('' + +P === P) { method = proxy[P]; }
        else {
          assertValidPropertyName(P);
          var get = handler.get_v___ ? handler.get : handler.v___('get');
          if (!isFunction(get)) {
            method = defaultDerivedTraps.get.apply(handler, [P, proxy]);
          } else {
            method = get.f___(handler, [P, proxy]);
          }
        }
        return method.f___(proxy, slice.call(args, 0));
      };
    proxy.e___ = function () {
        if (this !== proxy) {
          throw new TypeError(
              'Inheritance from proxies not implemented yet.');
        }
        var names;
        var enumerate = handler.enumerate___ ? handler.enumerate :
            handler.v___('enumerate');
        if (isFunction(enumerate)) {
          names = enumerate.f___(handler, [proxy]);
        } else {
          names = defaultDerivedTraps.enumerate.apply(handler, [proxy]);
        }
        var result = {};
        for (var i = 0, len = names.length; i < len; ++i) {
          var name = '' + names[i];
          assertValidPropertyName(name);
          result.DefineOwnProperty___(name, { enumerable: true });
        }
        return result;
      };
    proxy.keys___ = function () {
        if (this !== proxy) {
          throw new TypeError(
              'Inheritance from proxies not implemented yet.');
        }
        var names;
        var keys = handler.keys_v___ ? handler.keys : handler.v___('keys');
        if (isFunction(keys)) {
          names = keys.f___(handler, [proxy]);
        } else {
          names = defaultDerivedTraps.keys.apply(handler, [proxy]);
        }
        var result = [];
        var i, len = names.length, seen = {};
        for (i = 0; i < len; ++i) {
          var name = '' + names[i];
          assertValidPropertyName(name);
          if (seen[name]) { continue; }
          result.push('' + name);
          seen[name] = 1;
        }
        return result;
      };
    proxy.ownKeys___ = function () {
        if (this !== proxy) {
          throw new TypeError(
              'Inheritance from proxies not implemented yet.');
        }
        var names;
        var keys = handler.getOwnPropertyNames_v___ ?
            handler.getOwnPropertyNames :
            handler.v___('getOwnPropertyNames');
        names = keys.f___(handler, [proxy]);
        var result = [];
        var i, len = names.length, seen = {};
        for (i = 0; i < len; ++i) {
          var name = '' + names[i];
          assertValidPropertyName(name);
          if (seen[name]) { continue; }
          result.push(name);
          seen[name] = 1;
        }
        return result;
      };
    proxy.allKeys___ = function () {
        if (this !== proxy) {
          throw new TypeError(
              'Inheritance from proxies not implemented yet.');
        }
        var names;
        var keys = handler.getPropertyNames_v___ ?
            handler.getPropertyNames :
            handler.v___('getPropertyNames');
        names = keys.f___(handler, [proxy]);
        var result = [];
        var i, len = names.length, seen = {};
        for (i = 0; i < len; ++i) {
          var name = '' + names[i];
          assertValidPropertyName(name);
          if (seen[name]) { continue; }
          result.push('' + name);
          seen[name] = 1;
        }
        return result;
      };
    // TODO: If we need to let the fix trap know about numeric properties
    // set on the proxy object itself (since the proxy wasn't handling them),
    // we can pass a map of {numeric: descriptor} pairs as a second
    // argument to to handler.fix(), which can echo them back if it chooses to.
    function fix() {
      if (this !== proxy) {
        throw new TypeError(
            'Inheritance from proxies not implemented yet.');
      }
      if (proxy.fixing___) {
        throw new TypeError('Recursive fixing prohibited.');
      }
      var descMap = void 0;
      try {
        proxy.fixing___ = true;
        var fixer = handler.fix_v___ ? handler.fix : handler.v___('fix');
        descMap = fixer.f___(handler, [proxy]);
      } finally {
        delete proxy.fixing___;
      }
      if (!descMap) {
        throw new TypeError('Unable to fix the proxy.');
      }
      var isSafeFunc = isFunction(proxy) && proxy.ok___;
      var constructTrap = proxy.new___;
      var i;
      for (i in proxy) {
        if (proxy.hasOwnProperty(i)) { delete proxy[i]; }
      }
      if (isSafeFunc) {
        markFunc(proxy);
        proxy.new___ = constructTrap;
      }
      var keys = descMap.keys___();
      var len = keys.length;
      for (i = 0; i < len; ++i) {
        if (isNumericName(keys[i])) {
          var desc = ToPropertyDescriptor(descMap.v___(keys[i]));
          if (!IsDataDescriptor(desc) ||
              !desc.writable ||
              !desc.configurable ||
              !desc.enumerable) {
            throw new TypeError('Numeric properties returned from a fix trap ' +
                'must be writable, enumerable, configurable data properties.');
          }
          proxy[keys[i]] = desc.value;
        } else {
          proxy.DefineOwnProperty___(keys[i], descMap.v___(keys[i]));
        }
      }
    }
    proxy.freeze___ = function () {
        fix.call(this);
        // The fix function above deletes proxy.freeze___, so this call
        // goes to Object.prototype.freeze___
        return this.freeze___();
      };
    proxy.pe___ = function () {
        fix.call(this);
        // The fix function above deletes proxy.pe___, so this call
        // goes to Object.prototype.pe___
        return this.pe___();
      };
    proxy.seal___ = function () {
        fix.call(this);
        // The fix function above deletes proxy.seal___, so this call
        // goes to Object.prototype.seal___
        return this.seal___();
      };
    // desc is an internal property descriptor
    proxy.DefineOwnProperty___ = function (P, desc) {
        if (this !== proxy) {
          throw new TypeError(
              'Inheritance from proxies not implemented yet.');
        }
        P = '' + P;
        var extDesc = desc ? FromPropertyDescriptor(desc) : void 0;
        return (handler.defineProperty_m___ ?
            handler.defineProperty(P, extDesc, proxy) :
            handler.m___('defineProperty', [P, extDesc, proxy]));
      };
    proxy.GetOwnProperty___ = function (P) {
        if (this !== proxy) {
          throw new TypeError(
              'Inheritance from proxies not implemented yet.');
        }
        P = '' + P;
        var desc = (handler.getOwnPropertyDescriptor_m___ ?
            handler.getOwnPropertyDescriptor(P, proxy) :
            handler.m___('getOwnPropertyDescriptor', [P, proxy]));
        var intDesc = desc ? ToPropertyDescriptor(desc) : void 0;
        if (intDesc && !intDesc.configurable) {
          throw new TypeError('Proxy properties must be configurable.');
        }
        return intDesc;
     };
    proxy.HasProperty___ = function (P) {
        if (this !== proxy) {
          throw new TypeError(
              'Inheritance from proxies not implemented yet.');
        }
        P = '' + P;
        var has = handler.has_v___ ? handler.has : handler.v___('has');
        if (isFunction(has)) {
          return !!(has.f___(handler, [P, proxy]));
        } else {
          return defaultDerivedTraps.has.apply(handler, [P, proxy]);
        }
      };
    proxy.HasOwnProperty___ = function (P) {
        if (this !== proxy) {
          throw new TypeError(
              'Inheritance from proxies not implemented yet.');
        }
        P = '' + P;
        var hasOwn = handler.hasOwn_v___ ? handler.hasOwn :
            handler.v___('hasOwn');
        if (isFunction(hasOwn)) {
          return !!(hasOwn.f___(handler, [P, proxy]));
        } else {
          return defaultDerivedTraps.hasOwn.apply(handler, [P, proxy]);
        }
      };
  }

  var CajaProxy = {};
  CajaProxy.DefineOwnProperty___('create', {
      value: markFuncFreeze(function CajaProxy_create(handler, proto) {
          if (Type(handler) !== 'Object') {
            throw new TypeError("Expected handler to be an object.");
          }
          var proxy = void 0;
          if (proto === void 0 || proxy === null) {
            proxy = {};
          } else {
            var typeProto = Type(proto);
            if (typeProto !== 'Object') {
              throw new TypeError(
                  'Proto must be an object, not ' + typeProto);
            }
            if (proto.proxy___) {
              throw new TypeError('Proxies cannot inherit from proxies.');
            }
            var p = proto;
            while (p !== p.baseProto___) {
              if (p.ne___ !== p) {
                throw new TypeError(
                    'Proxies cannot inherit from extensible objects.');
              }
              p = origGetPrototypeOf(p);
            }
            proxy = beget(proto);
            // Override all the fastpath properties so a fastpath on proto
            // doesn't give a fastpath on proxy, which must always use
            // the handlers.
            for (var i in proto) {
              var m = endsWith_v___.test(i);
              if (!m) { continue; }
              var P = m[1];
              proxy[P + '_v___'] = false;
              proxy[P + '_w___'] = false;
              proxy[P + '_gw___'] = false;
              proxy[P + '_c___'] = false;
              proxy[P + '_e___'] = false;
              proxy[P + '_m___'] = false;
              proxy[P + '_g___'] = void 0;
              proxy[P + '_s___'] = void 0;
            }
          }
          prepareProxy(proxy, handler);
          return proxy;
        }),
      enumerable: true
    });

  CajaProxy.DefineOwnProperty___('createFunction', {
      value: markFuncFreeze(function (handler, callTrap, constructTrap) {
          if (Type(handler) !== 'Object') {
            throw new TypeError('Expected handler to be an object.');
          }
          if (!isFunction(callTrap)) {
            throw new TypeError('Expected callTrap to be a function.');
          }
          if (constructTrap === void 0) {
            constructTrap = callTrap;
          }
          if (!isFunction(constructTrap)) {
            throw new 
              TypeError("Construct trap must be a function or undefined.");
          }
          var proto = Function.prototype;
          // Here we know the prototype chain, so we can optimize.
          if (proto.ne___ !== proto) {
            throw new TypeError(
                  'Function.prototype must not be extensible to create ' +
                  'function proxies.');
          }
          var proxy = markFunc(function (var_args) {
              return callTrap.f___(safeDis(this), slice.call(arguments, 0));
            });
          // Install deferred handlers
          proxy.v___('length');
          // Override any fastpathed properties on Function.prototype
          for (var i in proto) {
            var m = endsWith_v___.test(i);
            if (!m) { continue; }
            var P = m[1];
            proxy[P + '_v___'] = false;
            proxy[P + '_w___'] = false;
            proxy[P + '_gw___'] = false;
            proxy[P + '_c___'] = false;
            proxy[P + '_e___'] = false;
            proxy[P + '_m___'] = false;
            proxy[P + '_g___'] = void 0;
            proxy[P + '_s___'] = void 0;
          }
          prepareProxy(proxy, handler);
          proxy.new___ = function (var_args) {
              return constructTrap.apply(this, slice.call(arguments, 0));
            };
          if (constructTrap) { proxy.prototype = constructTrap.prototype; }
          return proxy;
        }),
      enumerable: true
    });

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
  var obtainNewModule = snowWhite({
    handle: markFuncFreeze(function handleOnly(newModule){ return newModule; })
  });

  /**
   * Enable the use of Closure Inspector, nee LavaBug
   */
  function registerClosureInspector(module) {
    if (this && this.CLOSURE_INSPECTOR___
        && this.CLOSURE_INSPECTOR___.supportsCajaDebugging) {
      this.CLOSURE_INSPECTOR___.registerCajaModule(module);
    }
  }

  /**
   * Makes a mutable copy of an object.
   * <p>
   * Even if the original is frozen, the copy will still be mutable.
   * It does a shallow copy, i.e., if record y inherits from record x,
   * ___.copy(y) will also inherit from x.
   * Copies all whitelisted properties, not just enumerable ones.
   * All resulting properties are writable, enumerable, and configurable.
   */
  function copy(obj) {
    // TODO(ihab.awad): Primordials may not be frozen; is this safe?
    var result = Array.isArray(obj) ? [] : {};
    var keys = obj.ownKeys___(), len = keys.length;
    for (var i = 0; i < len; ++i) {
      var k = keys[i], v = obj[k];
      if (isNumericName(k)) { result[k] = v; }
      else {
        result.DefineOwnProperty___(k, {
            value: v,
            writable: true,
            enumerable: true,
            configurable: true
          });
      }
    }
    return result;
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
    return snowWhite({
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
              log(outcome[1]);
              if (outcome[1].stack) { log(outcome[1].stack); }
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
      handleUncaughtException: markFuncFreeze(
          function handleUncaughtException(exception,
                                           onerror,
                                           source,
                                           lineNum) {
            lastOutcome = [false, exception];

            // Cause exception to be rethrown if it is uncatchable.
            var message = exception;
            if ('object' === typeof exception && exception !== null) {
              message = '' + (exception.message || exception.desc || message);
            }

            // If we wanted to provide a hook for containers to get uncaught
            // exceptions, it would go here before onerror is invoked.

            // See the HTML5 discussion for the reasons behind this rule.
            if (!isFunction(onerror)) {
              throw new TypeError(
                  'Expected onerror to be a function or undefined.');
            }
            var shouldReport = onerror.i___(
                message,
                '' + source,
                '' + lineNum);
            if (shouldReport !== false) {
              log(source + ':' + lineNum + ': ' + message);
            }
          })
    });
  }

  function isFlag(name) {
    return /_v___$/ .test(name)
        || /_w___$/ .test(name)
        || /_gw___$/.test(name)
        || /_c___$/ .test(name)
        || /_e___$/ .test(name)
        || /_g___$/ .test(name)
        || /_s___$/ .test(name)
        || /_m___$/ .test(name);
  }

  function copyToImports(imports, source) {
    for (var p in source) {
      if (source.hasOwnProperty(p)) {
        if (/__$/.test(p)) {
          if (!isFlag(p)) {
            // Caja hidden property on IMPORTS -- these are used by Domita
            imports[p] = source[p];
          }
        } else if (isNumericName(p)) {
          // Set directly
          imports[p] = source[p];
        } else {
          imports.DefineOwnProperty___(p, {
            value: source[p],
            writable: true,
            enumerable: true,
            configurable: true
          });
        }
      }
    }
  }

  /**
   * Produces a function module given an object literal module
   */
  function prepareModule(module, load) {
    if (cajaBuildVersion !== module.cajolerVersion) {
      throw new TypeError(
          "Version error: Expected " + cajaBuildVersion +
          " but was " + module.cajolerVersion);
    }
    registerClosureInspector(module);
    function theModule(extraImports) {
      var imports = copy(sharedImports);
      copyToImports({
        load: load,
        cajaVM: cajaVM
      });
      copyToImports(imports, extraImports);
      return module.instantiate(___, imports);
    }

    // Whitelist certain module properties as visible to guest code. These
    // are all primitive values that do not allow two guest entities with
    // access to the same module object to communicate.
    var props = ['cajolerName', 'cajolerVersion', 'cajoledDate', 'moduleURL'];
    for (var i = 0; i < props.length; ++i) {
      theModule.DefineOwnProperty___(props[i], {
          value: module[props[i]],
          writable: false,
          enumerable: true,
          configurable: false
        });
    }
    // The below is a transitive freeze because includedModules is an array
    // of strings.
    if (!!module.includedModules) {
      theModule.DefineOwnProperty___('includedModules', {
          value: freeze(module.includedModules),
          writable: false,
          enumerable: true,
          configurable: false
        });
    }

    // Provide direct access to 'instantiate' for privileged use
    theModule.instantiate___ = function(___, IMPORTS___) {
      return module.instantiate(___, IMPORTS___);
    };

    return markFuncFreeze(theModule);
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
    return myNewModuleHandler.m___('handle', [module]);
  }

  // *********************************************************************
  // * Cajita Taming API
  // * Reproduced here for Domita's and Shindig's use; new
  // * tamings should be done with the ES5 API.
  // *********************************************************************

  function grantFunc(obj, name) {
    obj.DefineOwnProperty___(name, {
        value: markFuncFreeze(obj[name]),
        writable: false,
        enumerable: false,
        configurable: false
      });
  }

  function grantRead(obj, name) {
    obj.DefineOwnProperty___(name, {
        value: obj[name],
        writable: false,
        enumerable: false,
        configurable: false
      });
  }

  /**
   * Install a getter for proto[name] that returns a wrapper that
   * first verifies that {@code this} inherits from proto.
   * <p>
   * When a pre-existing Javascript method may do something unsafe
   * when applied to a {@code this} of the wrong type, we need to
   * prevent such mis-application.
   */
  function grantTypedMethod(proto, name) {
    name = '' + name;
    var original = proto[name];
    var f = function () {};
    f.prototype = proto;
    proto.DefineOwnProperty___(name, {
        value: markFunc(function guardedApplier(var_args) {
            if (!(this instanceof f)) {
              throw new TypeError(
                  'Tamed method applied to the wrong class of object.');
            }
            return original.apply(this, slice.call(arguments, 0));
          }),
        enumerable: false,
        configurable: true,
        writable: true
      });
  }

  /**
   * Install a getter for proto[name] under the assumption
   * that the original is a generic innocent method.
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
    proto.DefineOwnProperty___(name, {
        enumerable: false,
        configurable: false,
        get: markFunc(function () {
            return function guardedApplier(var_args) {
                var feralThis = safeDis(taming.untame(this));
                var feralArgs = taming.untame(slice.call(arguments, 0));
                var feralResult = original.apply(feralThis, feralArgs);
                return taming.tame(feralResult);
              };
          }),
        set: void 0
      });
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

  /**
   * Inside a <tt>___.forOwnKeys()</tt> or <tt>___.forAllKeys()</tt>, the
   * body function can terminate early, as if with a conventional
   * <tt>break;</tt>, by doing a <pre>return ___.BREAK;</pre>
   */
  var BREAK = Token('BREAK');

  /**
   * Used in domita with the name "forOwnKeys" for iterating over
   * JSON containers.
   */
  function forOwnNonCajaKeys(obj, fn) {
    for (var i in obj) {
      if (!obj.hasOwnProperty(i)) { continue; }
      if (endsWith__.test(i)) { continue; }
      if (fn(i, obj[i]) === BREAK) {
        return;
      }
    }
  }

  // TODO(metaweta): Deprecate this API, since it requires that we leave
  // configurable set to true in order to use both a getter and a setter.
  function useGetHandler(obj, name, getHandler) {
    getHandler = markFunc(getHandler);
    name = '' + name;
    var desc = obj.GetOwnProperty___(name);
    if (!desc || !IsAccessorDescriptor(desc)) {
      desc = {
          enumerable: false,
          configurable: true,
          get: getHandler,
          set: void 0
        };
    } else {
      desc.get = getHandler;
    }
    obj.DefineOwnProperty___(name, desc);
  }

  function useSetHandler(obj, name, setHandler) {
    setHandler = markFunc(setHandler);
    name = '' + name;
    var desc = obj.GetOwnProperty___(name);
    if (!IsAccessorDescriptor(desc)) {
      desc = {
          enumerable: false,
          configurable: true,
          get: void 0,
          set: setHandler
        };
    } else {
      desc.set = setHandler;
    }
    obj.DefineOwnProperty___(name, desc);
  }

  function hasOwnProp(obj, name) {
    return obj && obj.hasOwnProperty(name);
  }

  // *********************************************************************
  // * Exports
  // *********************************************************************

  cajaVM = whitelistAll({
      // Diagnostics and condition enforcement
      log: log,
      enforce: enforce,
      enforceType: enforceType,
      enforceNat: enforceNat,

      // Object indistinguishability
      Token: Token,

      // Guards and Trademarks
      identity: identity,
      callWithEjector: callWithEjector,
      eject: eject,
      GuardT: GuardT,
      Trademark: Trademark,
      guard: guard,
      passesGuard: passesGuard,
      stamp: stamp,
      makeTableGuard: makeTableGuard,

      // Sealing & Unsealing
      makeSealerUnsealerPair: makeSealerUnsealerPair,

      // Defensible objects
      def: def,

      // Other
      makeArrayLike: makeArrayLike,
      isFunction: isFunction,
      USELESS: USELESS,
      manifest: manifest,
      allKeys: allKeys
    });

  function readImport(imports, name) {
    name = '' + name;
    if (imports.HasProperty___(name)) {
      return imports.v___(name);
    }
    throw new ReferenceError(name + ' is not defined.');
  }

  function declareImport(imports, name) {
    if (imports.HasProperty___(name)) {
      return;
    }
    imports.w___(name, void 0);
  }

  function writeImport(imports, name, value) {
    if (imports.HasProperty___(name)) {
      imports.w___(name, value);
      return value;
    }
    throw new ReferenceError(name + ' is not defined.');
  }

  function goodParseInt(n, radix) {
    n = '' + n;
    // This turns an undefined radix into a NaN but is ok since NaN
    // is treated as undefined by parseInt
    radix = +radix;
    var isHexOrOctal = /^\s*[+-]?\s*0(x?)/.exec(n);
    var isOct = isHexOrOctal ? isHexOrOctal[1] !== 'x' : false;

    if (isOct && (radix !== radix || 0 === radix)) {
      return parseInt(n, 10);
    }
    return parseInt(n, radix);
  }

  var sharedImports = whitelistAll({
      cajaVM: cajaVM,

      'null': null,
      'false': false,
      'true': true,
      'NaN': NaN,
      'Infinity': Infinity,
      'undefined': void 0,
      parseInt: markFunc(goodParseInt),
      parseFloat: markFunc(parseFloat),
      isNaN: markFunc(isNaN),
      isFinite: markFunc(isFinite),
      decodeURI: markFunc(decodeURI),
      decodeURIComponent: markFunc(decodeURIComponent),
      encodeURI: markFunc(encodeURI),
      encodeURIComponent: markFunc(encodeURIComponent),
      escape: escape ? markFunc(escape) : (void 0),
      Math: Math,
      JSON: safeJSON,

      Object: Object,
      Array: Array,
      String: String,
      Boolean: Boolean,
      Number: Number,
      Date: Date,
      RegExp: RegExp,
      Function: FakeFunction,
      Proxy: CajaProxy,

      Error: Error,
      EvalError: EvalError,
      RangeError: RangeError,
      ReferenceError: ReferenceError,
      SyntaxError: SyntaxError,
      TypeError: TypeError,
      URIError: URIError,

      // ES-Harmony future features
      WeakMap: WeakMap
    });

  Object.prototype.m___ = function (name, as) {
      name = '' + name;
      if (this[name + '_m___']) {
        return this[name].f___(this, as);
      }
      var m = this.v___(name);
      if (typeof m !== 'function') {
        throw new TypeError(
            "The property '" + name + "' is not a function.");
      }
      // Fastpath the method on the object pointed to by name_v___
      // which is truthy iff it's a data property
      var ownerObj = this[name + '_v___'];
      if (ownerObj && ownerObj !== Function.prototype) {
        fastpathMethod(ownerObj, name);
      }
      return m.f___(this, as);
    };

  ___ = {
      sharedImports: sharedImports,
      USELESS: USELESS,
      BREAK: BREAK,
      args: args,
      deodorize: deodorize,
      copy: copy,
      i: isIn,
      iM: initializeMap,
      f: markSafeFunc,
      markFunc: markFunc,
      markFuncFreeze: markFuncFreeze,
      Trademark: Trademark,
      makeSealerUnsealerPair: makeSealerUnsealerPair,
      getId: getId,
      getImports: getImports,
      unregister: unregister,
      newTable: newTable,
      whitelistAll: whitelistAll,
      snowWhite: snowWhite,
      makeDefensibleFunction: makeDefensibleFunction,
      makeDefensibleObject: makeDefensibleObject,
      ri: readImport,
      di: declareImport,
      wi: writeImport,
      copyToImports: copyToImports,
      // Cajita API
      grantRead: grantRead,
      grantFunc: grantFunc,
      grantTypedMethod: grantTypedMethod,
      grantInnocentMethod: grantInnocentMethod,
      all2: all2,
      hasOwnProp: hasOwnProp,
      forOwnKeys: forOwnNonCajaKeys,
      markCtor: markFuncFreeze,
      useGetHandler: useGetHandler,
      useSetHandler: useSetHandler,
      primFreeze: snowWhite,
      isJSONContainer: isExtensible,
      getLogFunc: getLogFunc,
      setLogFunc: setLogFunc,
      callPub: function (obj, name, args) { return obj.m___(name, args); },
      readPub: function (obj, name) { return obj.v___(name); },
      canRead: function (obj, name) { return (name + '_v___') in obj; },
      freeze: freeze,
      // Module loading
      getNewModuleHandler: getNewModuleHandler,
      setNewModuleHandler: setNewModuleHandler,
      obtainNewModule: obtainNewModule,
      makeNormalNewModuleHandler: makeNormalNewModuleHandler,
      prepareModule: prepareModule,
      loadModule: loadModule,
      NO_RESULT: NO_RESULT,
      // Defensible objects
      def: def,
      // Taming
      extend: extend,
      isDefinedInCajaFrame: isDefinedInCajaFrame,
      rawDelete: rawDelete,
      getter: getter,
      setter: setter,
      directConstructor: directConstructor,
      BASE_OBJECT_CONSTRUCTOR: BASE_OBJECT_CONSTRUCTOR
    };
  var cajaVMKeys = ownEnumKeys(cajaVM);
  for (var i = 0; i < cajaVMKeys.length; ++i) {
    ___[cajaVMKeys[i]] = cajaVM[cajaVMKeys[i]];
  }
  setNewModuleHandler(makeNormalNewModuleHandler());
})();
