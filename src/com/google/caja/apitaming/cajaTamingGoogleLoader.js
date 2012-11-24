// Copyright (C) 2012 Google Inc.
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
 * Defines an object which can be used to tame Google's public APIs (which
 * are in the namespace "google.*") for use by guest code.
 *
 * @author ihab.awad@gmail.com
 * @requires console, document, JSON
 * @overrides caja, google, window
 */
caja.tamingGoogleLoader = (function() {

  function log() {
    var s = '';
    for (var i = 0; i < arguments.length; i++) { s += arguments[i] + ' '; }
    console.log(s);
  }

  function StringMap() {
    var o;
    try { o = Object.create(null); } catch (e) { o = {}; }
    function safe(k) { return k + '$'; }
    function unsafe(k) { return k.slice(0, -1); }
    return {
      get: function(k) {
        return o[safe(k)];
      },
      set: function(k, v) {
        o[safe(k)] = v;
      },
      'delete': function(k) {
        delete o[safe(k)];
      },
      has: function(k) {
        return Object.prototype.hasOwnProperty.call(o, safe(k));
      },
      keys: function(cb) {
        for (var k in o) {
          if (Object.prototype.hasOwnProperty.call(o, k)) {
            cb(unsafe(k));
          }
        }
      }
    };
  }

  function PropertyTamingFlags() {
    var m = caja.iframe.contentWindow.WeakMap();
    return {
      set: function(o, k) {
        if (!m.has(o)) { m.set(o, StringMap()); }
        m.get(o).set(k, true);
      },
      has: function(o, k) {
        return m.has(o) && m.get(o).has(k);
      }
    };
  }

  function EventListenerGroup() {
    var cbs = [];
    return {
      add: function(cb) {
        cbs.push(cb);
      },
      fire: function() {
        for (var i = 0; i < cbs.length; i++) {
          try {
            cbs[i].call({});
          } catch (e) {
            log('Event handler threw: ', e);
          }
        }
      }
    };
  }

  var TamingUtils = function(frame) {

    function type(o) {
      if (Object(o) !== o) {
        return 'primitive';
      }
      switch (Object.prototype.toString.call(o)) {
        case '[object Boolean]':
        case '[object Date]':
        case '[object Function]':
        case '[object Number]':
        case '[object RegExp]':
        case '[object String]':
        case '[object Error]':
          return 'primitive';
        case '[object Array]':
          return 'array';
        default:
          return 'object';
      }
    }

    function copyArray(o) {
      log('copyArray(' + o + ')');
      var result = [];
      for (var i = 0; i < o.length; i++) {
        log('   [' + i + ']');
        result[i] = copyMixed(o[i]);
      }
      return result;
    }

    function copyObject(o) {
      log('copyObject(' + o + ')');
      var result = {};
      for (var key in o) {
        if (o.hasOwnProperty(key) && !/__$/.test(key)) {
          log('   .' + key);
          result[key] = copyMixed(o[key]);
        }
      }
      return result;
    }

    function copyMixed(o) {
      log('copyMixed(' + o + ')');
      switch (type(o)) {
        case 'primitive':
          return o;
        case 'array':
          return copyArray(o);
        case 'object':
          return copyObject(o);
        default:
          throw new TypeError();  // Unreachable
      }
    }

    function copyOneLevel(o) {
      log('copyOneLevel(' + o + ')');
      var result = {};
      for (var key in o) {
        if (o.hasOwnProperty(key) && !/__$/.test(key)) {
          log('   .' + key);
          result[key] = o[key];
        }
      }
      return result;
    }

    function directCopy(r, o, props) {
      for (var i = 0; i < props.length; i++) {
        if (o.hasOwnProperty(props[i]) && !/__$/.test(props[i])) {
          r[props[i]] = o[props[i]];
        }
      }
    }

    function opaqueNode(guestNode) {
      var d = guestNode.ownerDocument.createElement('div');
      // TODO(ihab.awad): This solution is brittle since the cajoled code
      // styles leak in and affect this div.
      d.setAttribute('style', 'width: 100%; height: 100%;');
      frame.domicile.tameNodeAsForeign(d);
      guestNode.appendChild(d);
      return d;
    }

    function opaqueNodeById(origId) {
      if (!frame.hasOwnProperty('opaqueNodeByIdCounter___')) {
        frame.opaqueNodeByIdCounter___ = 0;
      }
      var node = frame.untame(frame.imports.document.getElementById(origId));
      var d = node.ownerDocument.createElement('div');
      var opaqueId = 'opaqueNodeById__' + frame.opaqueNodeByIdCounter___++ + '__' + frame.idSuffix;
      d.setAttribute('id', opaqueId);
      frame.domicile.tameNodeAsForeign(d);
      node.appendChild(d);
      return opaqueId;
    }

    function copyJson(o) {
      if (o === undefined || o === null) { return o; }
      return JSON.parse(JSON.stringify(o, function(key, value) {
        return /__$/.test(key) ? undefined : value;
      }));
    }

    function identity(o) {
      return o;
    }

    function mapArgs() {
      var mappings = arguments;
      return function(f, self, args) {
        var mappedArgs = args.slice(0);
        for (var i = 0; i < mappedArgs.length && i < mappings.length; i++) {
          mappedArgs[i] = mappings[i](mappedArgs[i]);
        }
        return mappedArgs;
      };
    }

    function mapResult() {
      if (arguments.length !== 1) {
        throw new TypeError("mapResult requires exactly one argument");
      }
      var mapping = arguments[0];
      return function(f, self, result) {
        return mapping(result);
      };
    }

    function forallkeys(obj, cb) {
      for (var k in obj) {
        if (!/.*__$/.test(k)) {
          cb(k);
        }
      }
    }

    return {
      identity: identity,
      copyJson: copyJson,
      copyMixed: copyMixed,
      copyOneLevel: copyOneLevel,
      directCopy: directCopy,
      opaqueNode: opaqueNode,
      opaqueNodeById: opaqueNodeById,
      mapArgs: mapArgs,
      mapResult: mapResult,
      forallkeys: forallkeys,
      StringMap: StringMap
    };
  };

  function PolicyEvaluator(frame) {

    var WeakMap = caja.iframe.contentWindow.WeakMap;
    var tamingUtils = TamingUtils(frame);

    function targ(obj, policy) {
      return policy.__subst__ ? policy : obj;
    }

    var fGrantRead = PropertyTamingFlags();
    var fGrantReadOverride = PropertyTamingFlags();
    var fGrantMethod = PropertyTamingFlags();
    var fMarkFunction = WeakMap();
    var fMarkCtor = WeakMap();
    var fAdviseFunction = WeakMap();

    function grantRead(o, k) {
      if (fGrantRead.has(o, k)) { return; }
      log('  + grantRead');
      frame.grantRead(o, k);
      fGrantRead.set(o, k);
    }

    function grantReadOverride(o, k) {
      if (fGrantReadOverride.has(o, k)) { return; }
      log('  + grantReadOverride');
      frame.grantReadOverride(o, k);
      fGrantReadOverride.set(o, k);
    }

    function grantMethod(o, k) {
      if (fGrantMethod.has(o, k)) { return; }
      frame.grantMethod(o, k);
      log('  + grantMethod');
      fGrantMethod.set(o, k);
    }

    function markFunction(o) {
      if (fMarkFunction.has(o)) { return o; }
      var r = frame.markFunction(o);
      log('  + markFunction');
      fMarkFunction.set(o, true);
      return r;
    }

    function markCtor(o, sup) {
      if (fMarkCtor.has(o)) { return o; }
      var r = frame.markCtor(o, sup);
      log('  + markCtor');
      fMarkCtor.set(o, true);
      return r;
    }

    function adviseFunctionBefore(o, advices) {
      for (var i = 0; i < advices.length; i++) {
        frame.adviseFunctionBefore(o, advices[i]);
      }
      log('  + adviseFunctionBefore');
      return o;
    }

    function adviseFunctionAfter(o, advices) {
      for (var i = 0; i < advices.length; i++) {
        frame.adviseFunctionAfter(o, advices[i]);
      }
      log('  + adviseFunctionAfter');
      return o;
    }

    function adviseFunctionAround(o, advices) {
      for (var i = 0; i < advices.length; i++) {
        frame.adviseFunctionAround(o, advices[i]);
      }
      log('  + adviseFunctionAround');
      return o;
    }

    function adviseFunction(o, policy) {
      if (fAdviseFunction.has(o)) { return; }
      if (policy.__before__) { adviseFunctionBefore(o, policy.__before__); }
      if (policy.__after__) { adviseFunctionAfter(o, policy.__after__); }
      if (policy.__around__) { adviseFunctionAround(o, policy.__around__); }
      fAdviseFunction.set(o, true);
    }

    function defCtor(path, obj, policy) {
      log(path + ' defCtor');

      adviseFunction(obj, policy);

      tamingUtils.forallkeys(policy, function(name) {
        if (!obj[name]) {
          log(path + '.' + name + ' skip');
          return;
        }
        log(path + '.' + name + ' grant static');
        grantRead(obj, name);
        if (typeof policy[name] === 'function') {
          markFunction(obj[name]);
        }
      });
      tamingUtils.forallkeys(policy.prototype, function(name) {
        if (typeof policy.prototype[name] === 'function') {
          if (obj.prototype[name]) {
            log(path + '.prototype.' + name + ' grant instance method');
            adviseFunction(obj.prototype[name], policy.prototype[name]);
            grantMethod(obj.prototype, name);
            if (policy.prototype['__' + name + '_OVERRIDE__']) {
              grantReadOverride(obj.prototype, name);
            }
          } else {
            log(path + '.prototype.' + name + ' skip');
          }
        } else {
          log(path + '.prototype.' + name + ' grant instance field');
          grantRead(obj.prototype, name);
          if (policy.prototype['__' + name + '_OVERRIDE__']) {
            grantReadOverride(obj.prototype, name);
          }
        }
      });
      var sup;
      if (policy.__super__ === Object) {
        sup = Object;
      } else {
        sup = window;
        for (var i = 0; i < policy.__super__.length; i++) {
          sup = sup[policy.__super__[i]];
          if (!sup) {
            throw new TypeError(
                'Cannot find path component ' + policy.__super__[i]);
          }
        }
      }

      return markCtor(obj, sup);
    }

    function defFcn(path, obj, policy) {
      log(path + ' defFcn');
      adviseFunction(obj, policy);
      return markFunction(obj);
    }

    function defObj(path, obj, policy) {
      log(path + ' defObj');
      var r = {};
      tamingUtils.forallkeys(policy, function(name) {
        var sub_obj = obj[name];
        if (!sub_obj) {
          log(path + '.' + name + ' skip');
          return;
        }
        var sub_policy = policy[name];
        var sub_path = path + '.' + name;
        var t_sub_policy = typeof sub_policy;
        if (t_sub_policy === 'function') {
          if (sub_policy.__super__) {
            r[name] = defCtor(sub_path, targ(sub_obj, sub_policy), sub_policy);
          } else {
            r[name] = defFcn(sub_path, targ(sub_obj, sub_policy), sub_policy);
          }
        } else if (t_sub_policy === 'object'){
          r[name] = defObj(sub_path, targ(sub_obj, sub_policy), sub_policy);
        } else {
          log(path + '.' + name + ' grant static');
          r[name] = targ(sub_obj, sub_policy);
          grantRead(r, name);
        }
      });
      return frame.markReadOnlyRecord(r);
    }

    return {
      defObj: defObj
    };
  }

  var policyFactoryUrlByName = StringMap();
  var policyFactoryByName = StringMap();
  var pendingPolicyFactoryLoadByName = StringMap();
  var loaderFactories = [];

  function addPolicyFactoryUrl(name, url) {
    policyFactoryUrlByName.set(name, url);
  }

  function addPolicyFactory(name, factory) {
    policyFactoryByName.set(name, factory);
    if (pendingPolicyFactoryLoadByName.has(name)) {
      pendingPolicyFactoryLoadByName.get(name).fire();
      pendingPolicyFactoryLoadByName['delete'](name);
    }
  }

  function addScript(url) {
    var s = document.createElement('script');
    s.setAttribute('src', url);
    document.head.appendChild(s);
  }

  function loadPolicyFactory(name, cb) {
    if (!pendingPolicyFactoryLoadByName.has(name)) {
      addScript(policyFactoryUrlByName.get(name));
      pendingPolicyFactoryLoadByName.set(name, EventListenerGroup());
    }
    pendingPolicyFactoryLoadByName.get(name).add(cb);
  }

  function maybeLoadPolicyFactory(name, cb) {
    if (policyFactoryByName.has(name)) { cb(); }
    else { loadPolicyFactory(name, cb); }
  }

  function addLoaderFactory(aLoaderFactory) {
    loaderFactories.push(aLoaderFactory);
  }

  function isObject(o) {
    return Object.prototype.toString.call(o) === '[object Object]';
  }

  function mergeInto(target, source) {
    for (var key in source) {
      if (source.hasOwnProperty(key) && !/__$/.test(key)) {
        if (isObject(source[key])) {
          if (!target[key]) { target[key] = {}; }
          mergeInto(target[key], source[key]);
        } else {
          target[key] = source[key];
        }
      }
    }
  }

  function applyToFrame(frame, initialEntries) {

    // TODO(ihab.awad): redundant!!!
    var tamingUtils = TamingUtils(frame);

    var framePolicies = {};
    var whitelistedApis = tamingUtils.StringMap();
    var policyEvaluator = PolicyEvaluator(frame);
    var loaders = [];
    var policyByName = tamingUtils.StringMap();

    function loadPolicy(name, cb) {
      if (policyByName.has(name)) {
        window.setTimeout(
            function() { cb(policyByName.get(name)); },
            0);
      } else {
        maybeLoadPolicyFactory(name, function() {
          var policy = policyFactoryByName
              .get(name)
              .call({}, frame, tamingUtils);
          mergeInto(framePolicies, policy.value);
          policyByName.set(name, policy);
          cb(policy);
        });
      }
    }

    function reapplyPolicies() {

      var safeWindow = policyEvaluator.defObj(
          'window', window, framePolicies);

      if (initialEntries) {
        mergeInto(safeWindow, initialEntries);
      }

      for (var i = 0; i < loaders.length; i++) {
        loaders[i].addToSafeWindow(safeWindow);
      }

      var w = caja.iframe.contentWindow;

      for (var key in safeWindow) {
        if (safeWindow.hasOwnProperty(key) && !/__$/.test(key)) {
          var obj = safeWindow[key];
          var tameObj = frame.tame(obj);
          if (w.___ && w.___.copyToImports) {
            var a = {};
            a[key] = tameObj;
            w.___.copyToImports(frame.imports, a);
          } else {
            frame.imports[key] = tameObj;
          }
        }
      }
    }

    function signalOnload() {
      reapplyPolicies();
      for (var i = 0; i < loaders.length; i++) {
        loaders[i].signalOnload();
      }
    }

    function whitelistApi(name) {
      if (!policyFactoryUrlByName.has(name) && !policyFactoryByName.has(name)) {
        throw 'API ' + name + ' has no known policy factory';
      }
      whitelistedApis.set(name, true);
    }

    for (var i = 0; i < loaderFactories.length; i++) {
      loaders.push(loaderFactories[i]({
        EventListenerGroup: EventListenerGroup,
        loadPolicy: loadPolicy,
        tamingUtils: tamingUtils,
        reapplyPolicies: reapplyPolicies,
        frame: frame,
        whitelistedApis: whitelistedApis
      }));
    }

    reapplyPolicies();

    return {
      signalOnload: signalOnload,
      whitelistApi: whitelistApi
    };
  }

  return {
    addPolicyFactoryUrl: addPolicyFactoryUrl,
    addPolicyFactory: addPolicyFactory,
    addLoaderFactory: addLoaderFactory,
    applyToFrame: applyToFrame
  };
})();
