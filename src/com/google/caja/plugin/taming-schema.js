// Copyright (C) 2012 Google Inc.
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
 * Schema for taming membrane.
 *
 * @requires WeakMap
 * @overrides window
 * @provides TamingSchema
 */
function TamingSchema(helper) {

  'use strict';

  function PropertyFlags() {
    var map = new WeakMap();
    return Object.freeze({
      has: function(obj, prop, flag) {
        prop = '$' + prop;
        return map.has(obj) &&
            map.get(obj).hasOwnProperty(prop) &&
            map.get(obj)[prop].indexOf(flag) !== -1;
      },
      set: function(obj, prop, flag) {
        prop = '$' + prop;
        if (!map.has(obj)) {
          // Note: Object.create(null) not supported in ES5/3
          map.set(obj, {});
        }
        var o = map.get(obj);
        if (!o.hasOwnProperty(prop)) {
          o[prop] = [];
        }
        if (o[prop].indexOf(flag) === -1) {
          o[prop].push(flag);
        }
      },
      getProps: function(obj) {
        if (!map.has(obj)) { return []; }
        return Object.getOwnPropertyNames(map.get(obj))
            .map(function(s) { return s.substring(1); });
      }
    });
  }

  var grantTypes = Object.freeze({
    METHOD: 'method',
    READ: 'read',
    WRITE: 'write',
    OVERRIDE: 'override'
  });

  var grantAs = PropertyFlags();

  var tameTypes = Object.freeze({
    CONSTRUCTOR: 'constructor',
    FUNCTION: 'function',
    XO4A: 'xo4a',
    READ_ONLY_RECORD: 'read_only_record'
  });

  // All WeakMaps we use deal in host objects, so have a shortcut
  function makeWeakMap() {
    var map = new WeakMap();
    helper.weakMapPermitHostObjects(map);
    return map;
  }

  var tameAs = makeWeakMap();

  var tameFunctionName = makeWeakMap();
  var tameCtorSuper = makeWeakMap();

  var functionAdvice = makeWeakMap();

  function applyFeralFunction(f, self, args) {
    return initAdvice(f)(self, args);
  }

  function isNumericName(n) {
    return typeof n === 'number' || ('' + (+n)) === n;
  }

  function checkNonNumeric(prop) {
    if (isNumericName(prop)) {
      throw new TypeError('Cannot control numeric property names: ' + prop);
    }
  }

  var fixed = makeWeakMap();

  function checkCanControlTaming(f) {
    var to = typeof f;
    if (!f || (to !== 'function' && to !== 'object')) {
      throw new TypeError('Taming controls not for non-objects: ' + f);
    }
    if (fixed.has(f)) {
      throw new TypeError('Taming controls not for already tamed: ' + f);
    }
    if (helper.isDefinedInCajaFrame(f)) {
      throw new TypeError('Taming controls not for Caja objects: ' + f);
    }
  }

  function fix(f) {
    fixed.set(f, true);
  }

  function markTameAsReadOnlyRecord(f) {
    checkCanControlTaming(f);
    tameAs.set(f, tameTypes.READ_ONLY_RECORD);
    return f;
  }

  function markTameAsFunction(f, name) {
    checkCanControlTaming(f);
    tameAs.set(f, tameTypes.FUNCTION);
    tameFunctionName.set(f, name);
    return f;
  }

  function markTameAsCtor(ctor, opt_super, name) {
    checkCanControlTaming(ctor);
    var ctype = typeof ctor;
    var stype = typeof opt_super;
    if (ctype !== 'function') {
      throw new TypeError('Cannot tame ' + ctype + ' as ctor');
    }
    if (opt_super && stype !== 'function') {
      throw new TypeError('Cannot tame ' + stype + ' as superclass ctor');
    }
    tameAs.set(ctor, tameTypes.CONSTRUCTOR);
    tameFunctionName.set(ctor, name);
    tameCtorSuper.set(ctor, opt_super);
    return ctor;
  }

  function markTameAsXo4a(f, name) {
    checkCanControlTaming(f);
    var ftype = typeof f;
    if (ftype !== 'function') {
      throw new TypeError('Cannot tame ' + ftype + ' as function');
    }
    tameAs.set(f, tameTypes.XO4A);
    tameFunctionName.set(f, name);
    return f;
  }

  function grantTameAsMethod(f, prop) {
    checkCanControlTaming(f);
    checkNonNumeric(prop);
    grantAs.set(f, prop, grantTypes.METHOD);
    grantAs.set(f, prop, grantTypes.READ);
  }

  function grantTameAsRead(f, prop) {
    checkCanControlTaming(f);
    checkNonNumeric(prop);
    grantAs.set(f, prop, grantTypes.READ);
  }

  function grantTameAsReadWrite(f, prop) {
    checkCanControlTaming(f);
    checkNonNumeric(prop);
    grantAs.set(f, prop, grantTypes.READ);
    grantAs.set(f, prop, grantTypes.WRITE);
  }

  function grantTameAsReadOverride(f, prop) {
    checkCanControlTaming(f);
    checkNonNumeric(prop);
    grantAs.set(f, prop, grantTypes.READ);
    grantAs.set(f, prop, grantTypes.OVERRIDE);
  }

  // Met the ghost of Greg Kiczales at the Hotel Advice.
  // This is what I told him as I gazed into his eyes:
  // Objects were for contracts,
  // Functions made for methods,
  // Membranes made for interposing semantics around them!

  function initAdvice(f) {
    if (!functionAdvice.has(f)) {
      functionAdvice.set(f, function tamingNullAdvice(self, args) {
        return f.apply(self, args);
      });
    }
    return functionAdvice.get(f);
  }

  function adviseFunctionBefore(f, advice) {
    var p = initAdvice(f);
    functionAdvice.set(f, function tamingBeforeAdvice(self, args) {
      return p(self, advice(f, self, args));
    });
  }
  
  function adviseFunctionAfter(f, advice) {
    var p = initAdvice(f);
    functionAdvice.set(f, function tamingAfterAdvice(self, args) {
      return advice(f, self, p(self, args));
    });
  }

  function adviseFunctionAround(f, advice) {
    var p = initAdvice(f);
    functionAdvice.set(f, function tamingAroundAdvice(self, args) {
      return advice(p, self, args);
    });
  }

  ///////////////////////////////////////////////////////////////////////////

  return Object.freeze({
    // Public facet, providing taming controls to clients
    published: Object.freeze({
      markTameAsReadOnlyRecord: markTameAsReadOnlyRecord,
      markTameAsFunction: markTameAsFunction,
      markTameAsCtor: markTameAsCtor,
      markTameAsXo4a: markTameAsXo4a,
      grantTameAsMethod: grantTameAsMethod,
      grantTameAsRead: grantTameAsRead,
      grantTameAsReadWrite: grantTameAsReadWrite,
      grantTameAsReadOverride: grantTameAsReadOverride,
      adviseFunctionBefore: adviseFunctionBefore,
      adviseFunctionAfter: adviseFunctionAfter,
      adviseFunctionAround: adviseFunctionAround
    }),
    // Control facet, exposed to taming membrane instances
    control: Object.freeze({
      grantTypes: grantTypes,
      grantAs: grantAs,
      tameTypes: tameTypes,
      tameAs: tameAs,
      tameFunctionName: tameFunctionName,
      tameCtorSuper: tameCtorSuper,
      applyFeralFunction: applyFeralFunction,
      fix: fix
    })});
}

// Exports for closure compiler.
if (typeof window !== 'undefined') {
  window['TamingSchema'] = TamingSchema;
}
