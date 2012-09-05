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
 * @provides SESFrameGroup
 * @requires Domado
 * @requires GuestManager
 * @requires Q
 * @requires TamingSchema
 * @requires TamingMembrane
 * @requires bridalMaker
 * @overrides window
 */

function SESFrameGroup(cajaInt, config, tamingWin, feralWin, guestMaker) {
  if (tamingWin !== window) {
    throw new Error('wrong frame');
  }
  if (!tamingWin.___) {
    tamingWin.___ = {};
  }

  var USELESS = Object.freeze({ USELESS: 'USELESS' });
  var BASE_OBJECT_CONSTRUCTOR = Object.freeze({});

  var tamingHelper = Object.freeze({
      applyFunction: applyFunction,
      getProperty: getProperty,
      setProperty: setProperty,
      getOwnPropertyNames: getOwnPropertyNames,
      directConstructor: directConstructor,
      getObjectCtorFor: getObjectCtorFor,
      isDefinedInCajaFrame: isDefinedInCajaFrame,
      isES5Browser: true,
      eviscerate: undefined,
      banNumerics: function() {},
      USELESS: USELESS,
      BASE_OBJECT_CONSTRUCTOR: BASE_OBJECT_CONSTRUCTOR
  });

  var frameGroupTamingSchema = TamingSchema(tamingHelper);
  var frameGroupTamingMembrane =
      TamingMembrane(tamingHelper, frameGroupTamingSchema.control);

  var domado = Domado(null);

  var bridal = bridalMaker(identity, feralWin.document);

  tamingWin.___.plugin_dispatchToHandler___ =
      domado.plugin_dispatchToHandler;

  var unsafe = false;

  var frameGroup = {

    makeDefensibleObject___: makeDefensibleObject,
    makeDefensibleFunction___: makeDefensibleFunction,

    tame: frameGroupTamingMembrane.tame,
    tamesTo: frameGroupTamingMembrane.tamesTo,
    reTamesTo: frameGroupTamingMembrane.reTamesTo,
    untame: frameGroupTamingMembrane.untame,
    unwrapDom: function(o) { return o; },
    markReadOnlyRecord:
        frameGroupTamingSchema.published.markTameAsReadOnlyRecord,
    markFunction: frameGroupTamingSchema.published.markTameAsFunction,
    markCtor: frameGroupTamingSchema.published.markTameAsCtor,
    markXo4a: frameGroupTamingSchema.published.markTameAsXo4a,
    grantMethod: frameGroupTamingSchema.published.grantTameAsMethod,
    grantRead: frameGroupTamingSchema.published.grantTameAsRead,
    grantReadWrite: frameGroupTamingSchema.published.grantTameAsReadWrite,
    adviseFunctionBefore: frameGroupTamingSchema.published.adviseFunctionBefore,
    adviseFunctionAfter: frameGroupTamingSchema.published.adviseFunctionAfter,
    adviseFunctionAround: frameGroupTamingSchema.published.adviseFunctionAround,

    USELESS: USELESS,
    iframe: window.frameElement,

    makeES5Frame: makeES5Frame,
    disableSecurityForDebugger: disableSecurityForDebugger
  };

  return frameGroup;

  //----------------

  function disableSecurityForDebugger(value) {
    unsafe = !!value;
    if (tamingWin) {
      tamingWin.ses.DISABLE_SECURITY_FOR_DEBUGGER = unsafe;
    }
  }

  function makeDefensibleObject(descriptors) {
    return Object.seal(Object.create(Object.prototype, descriptors));
  }

  function makeDefensibleFunction(f) {
    return Object.freeze(function() {
      return f.apply(USELESS, Array.prototype.slice.call(arguments, 0));
    });
  }

  function applyFunction(f, dis, args) {
    return f.apply(dis, args);
  }

  function getProperty(o, p) {
    return o[p];
  }

  function setProperty(o, p, v) {
    return o[p] = v;
  }

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
    var directProto = Object.getPrototypeOf(obj);
    if (!directProto) { return void 0; }
    var directCtor = directProto.constructor;
    if (!directCtor) { return void 0; }
    if (directCtor === feralWin.Object) { return BASE_OBJECT_CONSTRUCTOR; }
    Array.prototype.slice.call(feralWin.frames).forEach(function(w) {
      if (directCtor === w.Object) {
        directCtor = BASE_OBJECT_CONSTRUCTOR;
      }
    });
    return directCtor;
  }

  function getObjectCtorFor(o) {
    if (o === undefined || o === null) {
      return void 0;
    }
    var ot = typeof o;
    if (ot !== 'object' && ot !== 'function') {
      throw new TypeError('Cannot obtain ctor for non-object');
    }
    var proto = undefined;
    while (o) {
      proto = o;
      o = Object.getPrototypeOf(o);
    }
    return proto.constructor;
  }

  function getOwnPropertyNames(o) {
    var r = [];
    Object.getOwnPropertyNames(o).forEach(function(p) {
      if (Object.getOwnPropertyDescriptor(o, p).enumerable) {
        r.push(p);
      }
    });
    return r;
  }

  function inheritsFromPrototype(o, proto) {
    var ot = typeof o;
    if (ot !== 'object' && ot !== 'function') {
      return false;  // primitive
    }
    while (o !== null) {
      if (o === proto) { return true; }
      o = Object.getPrototypeOf(o);
    }
    return false;
  }

  function isDefinedInCajaFrame(o) {
    var result = false;
    Array.prototype.slice.call(feralWin.frames).forEach(function(w) {
      var isCajaFrame =
          (!!w.___ && !!w.cajaVM) ||   // ES53 frame
          (!!w.ses && !!w.cajaVM);     // SES frame
      if (isCajaFrame && inheritsFromPrototype(o, w.Object.prototype)) {
        result = true;
      }
    });
    return result;
  }

  //----------------

  function makeES5Frame(div, uriPolicy, es5ready, domOpts) {
    var divs = cajaInt.prepareContainerDiv(div, feralWin, domOpts);
    guestMaker.make(function (guestWin) {
      var frameTamingSchema = TamingSchema(tamingHelper);
      var frameTamingMembrane =
          TamingMembrane(tamingHelper, frameTamingSchema.control);
      var domicile = makeDomicile(
          frameTamingMembrane, divs, uriPolicy, guestWin);
      var gman = GuestManager(frameTamingSchema, frameTamingMembrane, divs,
          cajaInt.documentBaseUrl(), domicile, guestWin, USELESS, sesRun);
      guestWin.ses.DISABLE_SECURITY_FOR_DEBUGGER = unsafe;
      es5ready(gman);
    });
  }

  //----------------

  function makeDomicile(frameTamingMembrane, divs, uriPolicy, guestWin) {
    if (!divs.inner) { return null; }

    function FeralTwinStub() {}
    FeralTwinStub.prototype.toString = function () {
      return "[feral twin stub:" + tamingWin.taming.tame(this) + "]";
    };

    function permitUntaming(o) {
      if (typeof o === 'object' || typeof o === 'function') {
        frameTamingMembrane.tamesTo(new FeralTwinStub(), o);
      } // else let primitives go normally
    }

    var domicile = domado.attachDocument(
      '-' + divs.idClass, uriPolicy, divs.inner,
      config.targetAttributePresets,
      Object.freeze({
        permitUntaming: permitUntaming,
        tame: frameTamingMembrane.tame,
        untame: frameTamingMembrane.untame,
        tamesTo: frameTamingMembrane.tamesTo,
        reTamesTo: frameTamingMembrane.reTamesTo,
        hasTameTwin: frameTamingMembrane.hasTameTwin,
        hasFeralTwin: frameTamingMembrane.hasFeralTwin
      }));
    var imports = domicile.window;

    // The following code copied from the ES5/3 mode is mostly
    // commented out because the features it supports are not yet
    // available in the extremely incomplete ES5 mode. It is left in
    // as a reminder to implement the corresponding features.
    // In the ES5/SES/CES world, there is no ___ suffix to hide
    // properties, so all such things must be protected by other
    // means.
    //
    // TODO(kpreid): All of this code should disappear as the missing
    // features are implemented, but if it doesn't, remove it or check
    // for what we lost.

    guestWin.cajaVM.copyToImports(imports, guestWin.cajaVM.sharedImports);

    void new tamingWin.HtmlEmitter(
      identity, divs.inner, domicile, guestWin);
    //imports.rewriteUriInCss___ = domicile.rewriteUriInCss.bind(domicile);
    //imports.rewriteUriInAttribute___ =
    //  domicile.rewriteUriInAttribute.bind(domicile);
    //imports.getIdClass___ = domicile.getIdClass.bind(domicile);
    //imports.emitCss___ = domicile.emitCss.bind(domicile);

    //___.getId = cajaInt.getId;
    //___.getImports = cajaInt.getImports;
    //___.unregister = cajaInt.unregister;
    //
    //feralWin.___.getId = cajaInt.getId;
    //feralWin.___.getImports = cajaInt.getImports;
    //feralWin.___.unregister = cajaInt.unregister;
    //
    //guestWin.___.getId = cajaInt.getId;
    //guestWin.___.getImports = cajaInt.getImports;
    //guestWin.___.unregister = cajaInt.unregister;
    //
    //cajaInt.getId(imports);

    if (!feralWin.___.tamingWindows) {
      feralWin.___.tamingWindows = {};
    }
    feralWin.___.tamingWindows[imports.id___] = tamingWin;

    feralWin.___.plugin_dispatchEvent___ = domado.plugin_dispatchEvent;
    feralWin.___.plugin_dispatchToHandler___ =
      function (pluginId, handler, args) {
        var tamingWin = feralWin.___.tamingWindows[pluginId];
        return tamingWin.___.plugin_dispatchToHandler___(
          pluginId, handler, args);
      };

    return domicile;
  }

  function identity(x) { return x; }

  //----------------

  function sesRun(gman, args, moreImports, opt_runDone) {
    if (!moreImports.onerror) {
      moreImports.onerror = onerror;
    }

    // TODO(kpreid): right enumerable/own behavior?
    var imports = gman.imports;
    Object.getOwnPropertyNames(moreImports).forEach(
      function (i) {
        Object.defineProperty(
          imports, i,
          Object.getOwnPropertyDescriptor(moreImports, i));
      });

    // TODO(felix8a): args.flash

    var promise;
    if (args.uncajoledContent !== undefined) {
      promise = loadContent(gman, Q.ref({
        contentType: args.mimeType || 'text/html',
        responseText: args.uncajoledContent
      }));

    } else if (args.cajoledJs !== undefined) {
      throw new Error(
        'Operating in SES mode; pre-cajoled content cannot be loaded');

    } else {
      promise = loadContent(gman, fetch(args.url), args.mimeType);
    }

    Q.when(promise, function (compiledFunc) {
      var result = compiledFunc(imports);
      if (opt_runDone) {
        opt_runDone(result);
      }
    }, function (failure) {
      config.log('Failed to load guest content: ' + failure);
    });
  }

  function onerror(message, source, lineNum) {
    config.log('Uncaught script error: ' + message +
               ' in source: "' + source +
               '" at line: ' + lineNum);
  }

  /**
   * Given a promise for a fetch() response record, return a promise
   * for its Caja interpretation, a function of (extraImports).
   */
  function loadContent(gman, contentPromise, opt_expectedContentType) {
    var guestWin = gman.iframe.contentWindow;

    return Q.when(contentPromise, function (xhrRecord) {
      // TODO(kpreid): Is this safe? Does this match the cajoling
      // service's behavior? Should we reject if these two do not
      // agree?
      var contentType = opt_expectedContentType
        || xhrRecord.contentType;

      var theContent = xhrRecord.responseText;

      if (contentType === 'text/javascript'
          || contentType === 'application/javascript'
          || contentType === 'application/x-javascript'
          || contentType === 'text/ecmascript'
          || contentType === 'application/ecmascript'
          || contentType === 'text/jscript') {
        // TODO(kpreid): Make sure there's only one place (in JS)
        // where this big list of content-type synonyms is defined.

        // TODO(kpreid): needs to return completion value unless we
        // deprecate that feature.
        return Q.ref(guestWin.cajaVM.compileExpr(
          '(function () {' + theContent + '})()'));

      } else if (contentType === 'text/html') {
        // importsAgain always === imports, so ignored
        var writeComplete = gman.imports.document.write(theContent);
        return Q.when(writeComplete, function (importsAgain) {
            // TODO(kpreid): Make fetch() support streaming download,
            // then use it here via repeated document.write().
            gman.domicile.signalLoaded();
            return function() {};
        });
      } else {
        throw new TypeError("Unimplemented content-type " + contentType);
      }
    });
  }

  /**
   * Download the content of the given URL asynchronously, and return a
   * promise for a XHR-ish record containing the response.
   *
   * TODO(kpreid): modify this interface to support streaming download
   * (readyState 3), and make use of it in loadContent.
   */
  function fetch(url) {
    // TODO(kpreid): Review this for robustness/exposing all relevant info
    var pair = Q.defer();
    var resolve = pair.resolve;
    var xhr = bridal.makeXhr();
    xhr.open('GET', url, true);
    xhr.onreadystatechange = function() {
      if (xhr.readyState === 4) {
        if (xhr.status === 200) {
          resolve({
            contentType: xhr.getResponseHeader('Content-Type'),
            responseText: xhr.responseText
          });
        } else {
          resolve(Q.reject(xhr.status + ' ' + xhr.statusText));
        }
      }
    };
    xhr.send(null);
    return pair.promise;
  }

}

// Exports for closure compiler.
if (typeof window !== 'undefined') {
  window['SESFrameGroup'] = SESFrameGroup;
}
