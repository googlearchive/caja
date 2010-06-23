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
 * @fileoverview the Valija runtime library.
 * <p>
 * This file is written in Cajita and requires the portions of
 * cajita.js relevant to Cajita. It additionally depends on one
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
 * @requires cajita, loader, outers
 * @provides valijaMaker
 */

'use strict';
'use cajita';

var valijaMaker = (function(outers) {

  /**
   * Simulates a monkey-patchable <tt>Object.prototype</tt> except
   * that it also inherits from the real <tt>Object.prototype</tt>.
   */
  var ObjectPrototype = {constructor: Object};

  /**
   * Simulates a monkey-patchable <tt>Function.prototype</tt> except
   * that it inherits from <tt>cajita.PseudoFunctionProto</tt> rather
   * than <tt>ObjectPrototype</tt>.
   * <p>
   * $v.dis(aFunction) creates a disfunction instance inheriting from
   * DisfunctionPrototype, each with its own specific call() and
   * apply() methods which capture and use the function provided to
   * dis(). In addition, DisfunctionPrototype provides generic call(),
   * apply(), and bind() disfunctions, in order to simulate
   * Function.prototype.
   */
  var DisfunctionPrototype = cajita.beget(cajita.PseudoFunctionProto);

  var Disfunction = cajita.beget(DisfunctionPrototype);
  Disfunction.prototype = DisfunctionPrototype;
  Disfunction.length = 1;
  DisfunctionPrototype.constructor = Disfunction;

  /**
   * Simulates a monkey-patchable <tt>Function</tt> object
   */
  outers.Function = Disfunction;

  var ObjectShadow = cajita.beget(DisfunctionPrototype);
  ObjectShadow.prototype = ObjectPrototype;

  /**
   * Like Cajita's Object.freeze, except that, when applied to an
   * actual function (which must therefore already be frozen), will
   * freeze its shadow instead.
   */
  ObjectShadow.freeze = function(obj) {
    if (typeof obj === 'function') {
      cajita.freeze(getShadow(obj));
    } else {
      cajita.freeze(obj);
    }
    return obj;
  };

  var FuncHeader = new RegExp(
    // Capture the function name if present.
    // Use absence of spaces or open parens, rather than presence of
    // identifier chars, so we don't need to worry about charset
    // issues (beyond the definition of \s).
    '^\\s*function\\s*([^\\s\\(]*)\\s*\\(' +
      // Skip a first '$dis' parameter if present.
      '(?:\\$dis,?\\s*)?' +
      // Capture any remaining arguments until the matching close paren.
      // TODO(erights): Once EcmaScript and Valija allow patterns in parameter
      // position, a close paren will no longer be a reliable indication of
      // the end of the parameter list, so we'll need to revisit this.
      '([^\\)]*)\\)'); // don't care what's after the close paren

  function disfuncToString($dis) {
    var callFn = $dis.call;
    if (callFn) {
      var printRep = callFn.toString();
      var match = FuncHeader.exec(printRep);
      if (null !== match) {
        var name = $dis.name;
        if (name === void 0) { name = match[1]; }
        return 'function ' + name + '(' + match[2] +
          ') {\n  [cajoled code]\n}';
      }
      return printRep;
    }
    return 'disfunction(var_args){\n   [cajoled code]\n}';
  }

  DisfunctionPrototype.toString = dis(disfuncToString, 'toString');

  outers.Function = Disfunction;

  /**
   * A table mapping from <i>function categories</i> to the
   * monkey-patchable shadow object that POE associates with that
   * function category.
   */
  var myPOE = cajita.newTable();

  myPOE.set(cajita.getFuncCategory(Object), ObjectShadow);

  /**
   * Returns the monkey-patchable POE shadow of <tt>func</tt>'s
   * category, creating it and its parents as needed.
   */
  function getShadow(func) {
    cajita.enforceType(func, 'function');
    var cat = cajita.getFuncCategory(func);
    var result = myPOE.get(cat);
    if (void 0 === result) {
      result = cajita.beget(DisfunctionPrototype);
      var parentFunc = cajita.getSuperCtor(func);
      var parentShadow;
      if (typeof parentFunc === 'function') {
        parentShadow = getShadow(parentFunc);
      } else {
        parentShadow = ObjectShadow;
      }
      var proto = cajita.beget(parentShadow.prototype);
      result.prototype = proto;
      proto.constructor = func;

      var statics = cajita.getOwnPropertyNames(func);
      for (var i = 0; i < statics.length; i++) {
        var k = statics[i];
        if (k !== 'valueOf') {
          result[k] = func[k];
        }
      }

      var meths = cajita.getProtoPropertyNames(func);
      for (var i = 0; i < meths.length; i++) {
        var k = meths[i];
        if (k !== 'valueOf') {
          var v = cajita.getProtoPropertyValue(func, k);
          // TODO(erights): If the resolution of bug #814 is for
          // 'typeof aPseudoFunction' to be 'function', then the following
          // test should be rewritten. 
          if (typeof v === 'object' && 
              v !== null && 
              typeof v.call === 'function') {
            v = dis(v.call, k);
          }
          proto[k] = v;
        }
      }

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
  function getFakeProtoOf(func) {
    if (typeof func === 'function') {
      var shadow = getShadow(func);
      return shadow.prototype;
    } else if (typeof func === 'object' && func !== null) {
      return func.prototype;
    } else {
      return void 0;
    }
  }

  /**
   * Handle Valija <tt>typeof <i>obj</i></tt>.
   * <p>
   * If <tt>obj</tt> inherits from <tt>PseudoFunction</tt> then return
   * 'function'. Otherwise as normal.
   */
  function typeOf(obj) {
    var result = typeof obj;
    if (result !== 'object') { return result; }
    if (cajita.isPseudoFunc(obj)) { return 'function';  }
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
      return cajita.inheritsFrom(obj, getFakeProtoOf(func));
    }
  }

  /**
   * A unique object to be passed as the third argument to
   * cajita.readOwn(), so that it can pass it back to us to indicate a
   * quickly testable failure, or pass back anything else instead to
   * indicate a quickly testable success.
   */
  var pumpkin = {};

  /**
   * Handle Valija <tt><i>obj</i>[<i>name</i>]</tt>.
   */
  function read(obj, name) {
    var result = cajita.readOwn(obj, name, pumpkin);
    if (result !== pumpkin) { return result; }

    if (typeof obj === 'function') {
      return getShadow(obj)[name];
    }
    if (obj === null || obj === void 0) {
      throw new TypeError('Cannot read property "' + name + '" from ' + obj);
    }

    // BUG TODO(erights): figure out why things break when the
    // following line (which really shouldn't be there) is deleted.
    if (name in new Object(obj)) { return obj[name];}

    var stepParent = getFakeProtoOf(cajita.directConstructor(obj));
    if (stepParent !== (void 0) &&
        name in new Object(stepParent) &&
        name !== 'valueOf') {
      return stepParent[name];
    }
    return obj[name];
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
    return func.apply(cajita.USELESS, args);
  }

  /**
   * Handle Valija <tt><i>obj</i>[<i>name</i>](<i>args...</i>)</tt>.
   */
  function callMethod(obj, name, args) {
    var m = read(obj, name);
    if (!m) {
      throw new TypeError('callMethod: ' + obj + ' has no method ' + name);
    }
    return m.apply(obj, args);
  }

  /**
   * Handle Valija <tt>new <i>ctor</i>(<i>args...</i>)</tt>.
   */
  function construct(ctor, args) {
    if (typeof ctor === 'function') {
      return cajita.construct(ctor, args);
    }
    var result = cajita.beget(ctor.prototype);
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
    var template = cajita.PseudoFunction(callFn);

    var result = cajita.beget(DisfunctionPrototype);
    result.call = callFn;
    result.apply = template.apply;
// TODO(erights): Investigate why things break if the following line
// is uncommented out.
//    result.bind = template.bind;
    var disproto = cajita.beget(ObjectPrototype);
    result.prototype = disproto;
    disproto.constructor = result;
    result.length = template.length;
    // TODO(erights): Why are we testing for the empty string here?
    if (opt_name !== void 0 && opt_name !== '') {
      result.name = opt_name;
    }
    return result;
  }

  /**
   * The Valija code <tt>Function.prototype.call</tt> evaluates to a
   * generic disfunction which can be applied to anything with a
   * callable <tt>apply</tt> method, such as simple-functions and
   * pseudo-functions. 
   */
  function disfuncCall($dis, self, var_args) {
    return $dis.apply(self, Array.slice(arguments, 2));
  }
  DisfunctionPrototype.call = dis(disfuncCall, 'call');

  /**
   * The Valija code <tt>Function.prototype.apply</tt> evaluates to a
   * generic disfunction which can be applied to anything with a
   * callable <tt>apply</tt> method, such as simple-functions and
   * pseudo-functions.
   * <p>
   * Since other objects may inherit from DisfunctionPrototype, and
   * since disfunctions actually do, this generic apply method
   * requires that $dis provides a directly cajita-callable apply
   * method, so that it will fail if it simply inherits this one.
   */
  function disfuncApply($dis, self, args) {
    return $dis.apply(self, args);
  }
  DisfunctionPrototype.apply = dis(disfuncApply, 'apply');

  /**
   * The Valija code <tt>Function.prototype.bind</tt> evaluates to a
   * generic disfunction which can be applied to anything with a
   * callable <tt>apply</tt> method, such as simple-functions and
   * pseudo-functions.
   */
  function disfuncBind($dis, self, var_args) {
    var leftArgs = Array.slice(arguments, 2);
    function disfuncBound(var_args) {
      return $dis.apply(self, leftArgs.concat(Array.slice(arguments, 0)));
    };
    return disfuncBound;
  }
  DisfunctionPrototype.bind = dis(disfuncBind, 'bind');
  

  function getOuters() {
    cajita.enforceType(outers, 'object');
    return outers;
  }

  function readOuter(name) {
    var result = cajita.readOwn(outers, name, pumpkin);
    if (result !== pumpkin) { return result; }
    if (canReadRev(name, outers)) {
      return read(outers, name);
    } else {
      throw new ReferenceError('Outer variable not found: ' + name);
    }
  }

  function readOuterSilent(name) {
    if (canReadRev(name, outers)) {
      return read(outers, name);
    } else {
      return void 0;
    }
  }

  function setOuter(name, val) {
    return outers[name] = val;
  }

  function initOuter(name) {
    if (canReadRev(name, outers)) { return; }
    set(outers, name, void 0);
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
    cajita.forAllKeys(obj, function(name) {
      result.push(name);
    });
    cajita.forAllKeys(getSupplement(obj), function(name) {
      // TODO(erights): fix this once DONTENUM properties are better
      // settled in ES-Harmony.
      if (!(name in obj) && name !== 'constructor') {
        result.push(name);
      }
    });
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
      var ctor = cajita.directConstructor(obj);
      return getFakeProtoOf(ctor);
    }
  }

  /* Cajita does not allow throwing powerful objects because
   * it's too hard to keep them in mind when doing a security 
   * review.  So we construct a powerless object to throw in
   * its place, stash the actual exception in $v using the
   * powerless object as a key, the retrieve the original in the
   * catch block.  This also means that you can't throw a powerful
   * object between Valija sandboxes, which is as it should be.
   */
  var t = cajita.newTable();
  var undefIndicator = {};
  
  function exceptionTableSet(ex) {
    var result = cajita.Token('' + ex);
    t.set(result, (ex === void 0) ? undefIndicator : ex);
    return result;
  }
  
  function exceptionTableRead(key) {
    var v = t.get(key);
    t.set(key, void 0);
    return (v === void 0) ? key : ((v === undefIndicator) ? void 0 : v);
  }

  function disArgs(original) {
    return cajita.args(Array.slice(original, 1));
  }

  //If you change these names, also change them in PermitTemplate.java
  return cajita.freeze({
    typeOf: typeOf,
    instanceOf: instanceOf,
    tr: exceptionTableRead,
    ts: exceptionTableSet,

    r: read,
    s: set,
    cf: callFunc,
    cm: callMethod,
    construct: construct,
    getOuters: getOuters,
    ro: readOuter,
    ros: readOuterSilent,
    so: setOuter,
    initOuter: initOuter,
    remove: remove,
    keys: keys,
    canReadRev: canReadRev,
    disArgs: disArgs,

    dis: dis
  });
});

// This conditional allows this code to work uncajoled without a
// loader, in which case the top level "var valijaMaker = ..." will export
// 'valijaMaker' globally.
if (typeof loader !== 'undefined') {
  loader.provide(valijaMaker);
}

// If the Valija module is called with the new-style Cajita module convention,
// passing an 'outers' as a Cajita-level parameter, the value returned from
// instantiating the module should be the fully constructed '$v' object, not
// just valijaMaker.
if (typeof outers !== 'undefined') {
  valijaMaker(outers);
}
