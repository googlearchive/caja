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

  ////////////////////////////////////////////////////////////////////////
  // TODO(erights): nothing in this file is tested yet
  ////////////////////////////////////////////////////////////////////////

/**
 * @fileoverview the Valija runtime library.
 * <p>
 * This file is written in Cajita and requires the portions of
 * caja.js relevant to Cajita. It additionally depends on one
 * container-provided import, "<tt>loader</tt>", which this file calls
 * as <tt>loader.provide(valija);</tt>. Since this file has the
 * relative path 
 * <tt>com/google/caja/valija-cajita.js</tt>, we assume a POLA loader
 * will associate the provided value
 * <tt>'com.google.caja.valija'</tt>. <tt>loader.provide()</tt> is the
 * strawman <tt>loader.return()</tt> proposed at the bottom of <a href= 
 * "http://google-caja.googlecode.com/svn/trunk/doc/html/cajaModuleSystem/"
 * >Caja Module System</a> but renamed to avoid conflicting with a
 * keyword.
 * <p>
 * The Valija->Cajita translator begins every module with
 * <pre>var valija = loader.require('com.google.caja.valija');</pre>
 * This is distinct from the strawman <tt>loader.load()</tt> proposed
 * at the bottom of <a href=
 * "http://google-caja.googlecode.com/svn/trunk/doc/html/cajaModuleSystem/"
 * >Caja Module System</a>, in that each loaded module is instantiated
 * <i>at most once per loading context with imports determined by the
 * container</i>, rather than once per <tt>load()</tt> with imports
 * determined than the importing module.
 * <p>
 * A container can thereby choose to provide multiple module instances
 * access to the same loading context, in which case these instances
 * can communicate with each other. Such module instances jointly form
 * a single plugin. This enables all the modules instances in a single
 * Valija plugin to share the same mutable POE-table state. For Valija,
 * the plugin is thus the only defensible unit of isolation. 
 * <p>
 * Although <tt>valija-cajita.js</tt> is written with the expectation
 * that it and the output by the Valija->Cajita translator will be
 * cajoled, safety aside, this file uncajoled should work with the
 * output of the Valija->Cajita translator, when that output is also
 * not cajoled. 
 * 
 * @author erights@gmail.com
 */

var valijaMaker = (function(outers) {

  /**
   * Simulates a monkey-patchable <tt>Object.prototype</tt>.
   */
  var ObjectPrototype = {constructor: Object};

  /**
   * Simulates a monkey-patchable <tt>Function.prototype</tt>.
   * <p>
   * Currently the call(), apply(), and bind() methods are
   * genuine functions on each Disfunction instance, rather being
   * disfunctions inherited from DisfunctionPrototype. This is needed
   * for call() and apply(), but bind() could probably become an
   * inherited disfunction. 
   */
  var DisfunctionPrototype = caja.beget(ObjectPrototype);

  var Disfunction = caja.beget(DisfunctionPrototype);
  Disfunction.prototype = DisfunctionPrototype,
  Disfunction.length = 1;
  DisfunctionPrototype.constructor = Disfunction;

  /**
   * Simulates a monkey-patchable <tt>Function</tt> object
   */
   outers.Function = Disfunction;

  var ObjectShadow = caja.beget(DisfunctionPrototype);
  ObjectShadow.prototype = ObjectPrototype;

  /**
   * A table mapping from <i>function categories</i> to the
   * monkey-patchable shadow object that POE associates with that
   * function category. 
   */
  var myPOE = caja.newTable();

  myPOE.set(caja.getFuncCategory(Object), ObjectShadow);

  /**
   * Returns the monkey-patchable POE shadow of <tt>func</tt>'s
   * category, creating it and its parents as needed.
   */
  function getShadow(func) {
    caja.enforceType(func, 'function');
    var cat = caja.getFuncCategory(func);
    var result = myPOE.get(cat);
    if (void 0 === result) {
      result = caja.beget(DisfunctionPrototype);
      var parentFunc = caja.getSuperCtor(func);
      var parentShadow;
      if (typeof parentFunc === 'function') {
        parentShadow = getShadow(parentFunc);
      } else {
        parentShadow = caja.beget(DisfunctionPrototype);
      }
      result.prototype = caja.beget(parentShadow.prototype);
      result.prototype.constructor = func;
      myPOE.set(cat, result);
    }
    return result;
  }
  
  /** 
   * Handle Valija <tt><i>func</i>.prototype</tt>.
   * <p>
   * If <tt>func</tt> is a genuine function, return its shadow's
   * pseudo-prototype, creating it (and its parent pseudo-prototypes)
   * if needed. Otherwise as normal.
   */
  function getPrototypeOf(func) {
    if (typeof func === 'function') {
      var shadow = getShadow(func);
      return shadow.prototype;
    } else if (typeof func === 'object' && func !== null) {
        return func.prototype;
    } else {
      return (void 0);
    }
  }
  
  /** 
   * Handle Valija <tt>typeof <i>obj</i></tt>.
   * <p>
   * If <tt>obj</tt> inherits from DisfunctionPrototype, then return
   * 'function'. Otherwise as normal.
   */
  function typeOf(obj) {
    var result = typeof obj;
    if (result !== 'object') { return result; }
    if (null === obj) { return result; }
    if (caja.inheritsFrom(DisfunctionPrototype)) { return 'function'; }
    return result;
  }
  
  /** 
   * Handle Valija <tt><i>obj</i> instanceof <i>func</i></tt>.
   * <p>
   * If <tt>func</tt> is a genuine function, then test whether
   * <tt>obj</tt> inherits from either <tt>func.prototype</tt> or
   * <tt>func</tt>'s POE pseudo-prototype. Otherwise tests if <obj>
   * inherits from <tt>func.prototype</tt>.
   */
  function instanceOf(obj, func) {
    if (typeof func === 'function' && obj instanceof func) {
      return true;
    } else {
      return caja.inheritsFrom(obj, getPrototypeOf(func));
    }
  }

  /**
   * Handle Valija <tt><i>obj</i>[<i>name</i>]</tt>.
   */
  function read(obj, name) {
    if (typeof obj === 'function') {
      var shadow = getShadow(obj);
      if (name in shadow) {
        return shadow[name];
      } else {
        return obj[name];
      }
    }
    // BUG TODO(erights): Should check in order 1) obj's own
    // properties, 2) getPrototypeOf(ctor)'s properties, 3) obj's
    // inherited properties.
    if (name in obj) {
      return obj[name];
    }
    var ctor = caja.directConstructor(obj);
    return getPrototypeOf(ctor)[name];
  }

  /** 
   * Handle Valija <tt><i>obj</i>[<i>name</i>] = <i>newValue</i></tt>.
   */
  function set(obj, name, newValue) {
    if (typeof obj === 'function') {
      getShadow(obj)[name] = newValue;
    } else {
      obj[name] = newValue;
    }
    return newValue;
  }

  /** 
   * Handle Valija <tt><i>func</i>(<i>args...</i>)</tt>.
   */
  function callFunc(func, args) {
    return func.apply(caja.USELESS, args);
  }

  /** 
   * Handle Valija <tt><i>obj</i>[<i>name</i>](<i>args...</i>)</tt>.
   */
  function callMethod(obj, name, args) {
    return read(obj, name).apply(obj, args);
  }

  /** 
   * Handle Valija <tt>new <i>ctor</i>(<i>args...</i>)</tt>.
   */
  function construct(ctor, args) {
    if (typeof ctor === 'function') {
      return caja.construct(ctor, args);
    }
    var result = caja.beget(ctor.prototype);
    var altResult = ctor.apply(result, args);
    switch (typeof altResult) {
      case 'object': {
        if (null !== altResult) { return altResult; }
        break;
      }
      case 'function': {
        return altResult;
      }
    }
    return result;
  }
  
  /** 
   * Handle Valija <tt>function <i>opt_name</i>(...){...}</tt>.
   */
  function dis(callFn, opt_name) {
    caja.enforceType(callFn, 'function');

    var result = caja.beget(DisfunctionPrototype);
    result.call = callFn;
    result.apply = function(self, args) {
      return callFn.apply(caja.USELESS, [self].concat(args));
    };
    result.bind = function(self, var_args) {
      var leftArgs = Array.slice(arguments, 0);
      return function(var_args) {
        return callFn.apply(caja.USELESS, 
                            leftArgs.concat(Array.slice(arguments, 0)));
      };
    };
    result.prototype = caja.beget(ObjectPrototype);
    result.prototype.constructor = result;
    result.length = callFn.length -1;
    if (opt_name !== void 0) {
      result.name = opt_name;
    }
    return result;
  }

  function getOuters() {
    caja.enforceType(outers, "object");
    return outers;
  }

  function readOuter(name) {
    if (canReadRev(name, outers)) {
      return read(outers, name);
    } else {
      throw new ReferenceError('not found: ' + name);
    }
  }

  function setOuter(name, val) {
    return outers[name] = val;
  }

  function initOuter(name) {
    if (canReadRev(name, outers)) { return; }
    set(outers, name, undefined);
  }

  function remove(obj, name) {
    if (typeof obj === 'function') {
      var shadow = getShadow(obj);
      return delete shadow[name];
    } else {
      return delete obj[name];
    }
  }

  function keys(obj) {
    var result = [];
    for (var name in obj) {
      result.push(name);
    }
    for (name in getSupplement(obj)) {
      // TODO(erights): fix this once DONTENUM properties are better settled in ES-Harmony.
      if (!(name in obj) && name !== 'constructor') {
        result.push(name);
      }
    }
    return result;
  }

  function canReadRev(name, obj) {
    if (name in obj) { return true; }
    return name in getSupplement(obj);
  }

  /**
   * Return the object to be used as the per-plugin subjective
   * supplement to obj and its actual inheritance chain.
   */
  function getSupplement(obj) {
    if (typeof obj === 'function') {
      return getShadow(obj);
    } else {
      var ctor = caja.directConstructor(obj);
      return getPrototypeOf(ctor);
    }
  }

  return caja.freeze({
    typeOf: typeOf,
    instanceOf: instanceOf,

    read: read,
    set: set,
    callFunc: callFunc,
    callMethod: callMethod,
    construct: construct,
    getOuters: getOuters,
    readOuter: readOuter,
    setOuter: setOuter,
    initOuter: initOuter,    
    remove: remove,
    keys: keys,
    canReadRev: canReadRev,

    dis: dis,
  });
});

// This conditional allows this code to work uncajoled without a
// loader, in which case the top level "var valija = ..." will export
// 'valija' globally.
if (typeof loader !== 'undefined') {
  loader.provide(valijaMaker);
}
