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
 * @requires bridalMaker
 * @requires cajaVM
 * @requires cajaFrameTracker
 * @requires Domado
 * @requires GuestManager
 * @requires Q
 * @requires ses
 * @requires TamingSchema
 * @requires TamingMembrane
 * @requires URI
 * @overrides window
 */

function SESFrameGroup(cajaInt, config, tamingWin, feralWin,
    additionalParams) {
  if (tamingWin !== window) {
    throw new Error('wrong frame');
  }

  tamingWin.ses.mitigateSrcGotchas = additionalParams.mitigateSrcGotchas;

  var USELESS = Object.freeze({ USELESS: 'USELESS' });
  var BASE_OBJECT_CONSTRUCTOR = Object.freeze({});

  var tamingHelper = Object.freeze({
      applyFunction: applyFunction,
      getProperty: getProperty,
      setProperty: setProperty,
      getOwnPropertyNames: getOwnPropertyNames,
      directConstructor: directConstructor,
      getObjectCtorFor: getObjectCtorFor,
      isDefinedInCajaFrame: cajaFrameTracker.isDefinedInCajaFrame,
      isES5Browser: true,
      eviscerate: undefined,
      banNumerics: function() {},
      USELESS: USELESS,
      BASE_OBJECT_CONSTRUCTOR: BASE_OBJECT_CONSTRUCTOR,
      getValueOf: function(o) { return o.valueOf(); },
      weakMapPermitHostObjects: ses.weakMapPermitHostObjects
  });

  var frameGroupTamingSchema = TamingSchema(tamingHelper);
  var frameGroupTamingMembrane =
      TamingMembrane(tamingHelper, frameGroupTamingSchema.control);

  var domado = Domado(null);

  var bridal = bridalMaker(identity, feralWin.document);

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
    grantReadOverride: frameGroupTamingSchema.published.grantTameAsReadOverride,
    adviseFunctionBefore: frameGroupTamingSchema.published.adviseFunctionBefore,
    adviseFunctionAfter: frameGroupTamingSchema.published.adviseFunctionAfter,
    adviseFunctionAround: frameGroupTamingSchema.published.adviseFunctionAround,

    USELESS: USELESS,
    iframe: window.frameElement,

    Q: Q,

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
      var O;
      try {
        O = w.Object;
      } catch (e) {
        // met a different-origin frame, probably
        return;
      }
      if (directCtor === O) {
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

  //----------------

  function makeES5Frame(div, uriPolicy, es5ready, domOpts) {
    var divs = cajaInt.prepareContainerDiv(div, feralWin, domOpts);

    var frameTamingSchema = TamingSchema(tamingHelper);
    var frameTamingMembrane =
        TamingMembrane(tamingHelper, frameTamingSchema.control);
    var domicileAndEmitter = makeDomicileAndEmitter(
        frameTamingMembrane, divs, uriPolicy);
    var domicile = domicileAndEmitter && domicileAndEmitter[0];
    var htmlEmitter = domicileAndEmitter && domicileAndEmitter[1];
    var gman = GuestManager(frameTamingSchema, frameTamingMembrane, divs,
        cajaInt.documentBaseUrl(), domicile, htmlEmitter, window, USELESS,
        uriPolicy, sesRun);
    es5ready(gman);
  }

  //----------------

  function makeDomicileAndEmitter(
      frameTamingMembrane, divs, uriPolicy) {
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

    // Needs to be membraned for exception safety in Domado. But we do not want
    // to have side effects on our arguments, so we construct a wrapper.
    // TODO(kpreid): Instead of reimplementing the taming membrane here, have
    // the host-side code generate a fresh host-side function wrapper which can
    // be tamed. Then neither SES nor ES5/3 frame group code need do this.
    var uriPolicyWrapper = {};
    ['rewrite', 'fetch'].forEach(function(name) {
      if (name in uriPolicy) {
        var f = uriPolicy[name];
        uriPolicyWrapper[name] = function() {
          var args = Array.prototype.slice.call(arguments);
          // Argument 0 of both rewrite and fetch is the URI object, which we
          // need to make sure can be untamed but the taming membrane doesn't
          // natively support. TODO(kpreid): Do this more cleanly, such as by
          // the taming membrane being able to be told about untaming of tame
          // constructed objects, or by having a tame-side advice mechanism.
          var uriArg = arguments[0];
          if (uriArg) {
            if (!uriArg instanceof URI) { throw new Error('oops, not URI'); }
            frameTamingMembrane.tamesTo(uriArg.clone(), uriArg);
          }
          try {
            return frameTamingMembrane.tame(
                f.apply(uriPolicy, Array.prototype.map.call(arguments,
                    frameTamingMembrane.untame)));
          } catch (e) {
            throw frameTamingMembrane.tameException(e);
          }
        };
      }
    });

    var domicile = domado.attachDocument(
      '-' + divs.idClass, uriPolicyWrapper, divs.inner,
      config.targetAttributePresets,
      Object.freeze({
        permitUntaming: permitUntaming,
        tame: frameTamingMembrane.tame,
        untame: frameTamingMembrane.untame,
        tamesTo: frameTamingMembrane.tamesTo,
        reTamesTo: frameTamingMembrane.reTamesTo,
        hasTameTwin: frameTamingMembrane.hasTameTwin,
        hasFeralTwin: frameTamingMembrane.hasFeralTwin,
        tameException: frameTamingMembrane.tameException,
        untameException: frameTamingMembrane.untameException
      }));
    var imports = domicile.window;

    cajaVM.copyToImports(imports, cajaVM.sharedImports);

    var htmlEmitter = new tamingWin.HtmlEmitter(
      identity, domicile.htmlEmitterTarget, uriPolicy.mitigate, domicile,
      window);

    if (!feralWin.___.tamingWindows) {
      feralWin.___.tamingWindows = {};
    }
    feralWin.___.tamingWindows[imports.id___] = tamingWin;

    // Invoked by textual event handlers emitted by Domado.
    // TODO(kpreid): Use a name other than ___ for this purpose; perhaps some
    // property of the 'caja' object.
    feralWin.___.plugin_dispatchEvent___ = domado.plugin_dispatchEvent;

    return [domicile, htmlEmitter];
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
      config.console.log('Failed to load guest content: ' + failure);
    });
  }

  function onerror(message, source, lineNum) {
    config.console.log(
        'Uncaught script error: ' + message +
        ' in source: "' + source +
        '" at line: ' + lineNum);
  }

  /**
   * Given a promise for a fetch() response record, return a promise
   * for its Caja interpretation, a function of (extraImports).
   */
  function loadContent(gman, contentPromise, opt_expectedContentType) {
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

        if (gman.htmlEmitter) {
          // If we have a container but no HTML (only JS) then cause an empty
          // document to exist, much like about:blank.
          gman.htmlEmitter.finish();
        }

        // TODO(kpreid): needs to return completion value unless we
        // deprecate that feature.
        return Q.ref(cajaVM.compileExpr(
          // End of line required to ensure linecomments in theContent
          // do not escape away the closing curlies in the expression
          '(function () {' + theContent + '\n})()'));

      } else if (contentType === 'text/html') {
        // importsAgain always === imports, so ignored
        var writeComplete = gman.imports.document.write(theContent);
        return Q.when(writeComplete, function (importsAgain) {
            // TODO(kpreid): Make fetch() support streaming download,
            // then use it here via repeated document.write().
            gman.htmlEmitter.finish();
            gman.htmlEmitter.signalLoaded();
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
