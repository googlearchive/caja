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
 * @provides ES53FrameGroup
 * @overrides ___
 * @requires cajolingServiceClientMaker
 * @requires Domado
 * @requires GuestManager
 * @requires HtmlEmitter
 * @requires Q
 * @requires TamingSchema
 * @requires TamingMembrane
 * @requires jsonRestTransportMaker
 * @requires URI
 * @overrides window
 */

function ES53FrameGroup(cajaInt, config, tamingWin, feralWin, guestMaker,
    additionalParams) {
  // Note: in IE<9, window !== window.self
  if (tamingWin !== window.self) {
    throw new Error('wrong frame');
  }

  var cajoler = config.cajolingServiceClient || cajolingServiceClientMaker(
    cajaInt.joinUrl(config.server, 'cajole'),
    jsonRestTransportMaker(),
    true,
    config.debug,
    config.console);

  var tamingHelper = recordWithMethods(
      'applyFunction', markCallableWithoutMembrane(applyFunction),
      'getProperty', markCallableWithoutMembrane(getProperty),
      'setProperty', markCallableWithoutMembrane(setProperty),
      'getOwnPropertyNames', markCallableWithoutMembrane(getOwnPropertyNames),
      'directConstructor', tamingWin.___.directConstructor,
      'getObjectCtorFor', markCallableWithoutMembrane(getObjectCtorFor),
      'isDefinedInCajaFrame', tamingWin.___.isDefinedInCajaFrame,
      'isES5Browser', false,
      'eviscerate', markCallableWithoutMembrane(eviscerate),
      'banNumerics', markCallableWithoutMembrane(banNumerics),
      'USELESS', tamingWin.___.USELESS,
      'BASE_OBJECT_CONSTRUCTOR', tamingWin.___.BASE_OBJECT_CONSTRUCTOR,
      'getValueOf', markCallableWithoutMembrane(getValueOf),
      'weakMapPermitHostObjects', tamingWin.cajaVM.identity);

  var frameGroupTamingSchema = TamingSchema(tamingHelper);
  var frameGroupTamingMembrane =
      TamingMembrane(tamingHelper, frameGroupTamingSchema.control);

  // On IE<=8 you can't add properties to text nodes or attribute nodes.
  // We detect that here and set a flag ie8nodes for makeDOMAccessible().
  // Note, this flag has to be set before the call to Domado()
  var ie8nodes = false;
  try {
    feralWin.document.createTextNode('x').v___ = 1;
  } catch (e) {
    ie8nodes = true;
  }

  var readPropertyAsHostFrame = cajaInt.readPropertyAsHostFrame;
  var domado = Domado(makeDomadoRuleBreaker());

  var unsafe = false;

  var frameGroup = {

    makeDefensibleObject___: ___.makeDefensibleObject,
    makeDefensibleFunction___: ___.makeDefensibleFunction,

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

    USELESS: tamingWin.___.USELESS,
    iframe: window.frameElement,

    Q: tamingWin.Q,

    makeES5Frame: makeES5Frame,
    disableSecurityForDebugger: disableSecurityForDebugger,
    
    // For use by the Caja test suite only. Should not be used for any other
    // purpose and is hard to use correctly.
    testing_makeDomadoRuleBreaker: makeDomadoRuleBreaker
  };

  return frameGroup;

  //----------------

  function disableSecurityForDebugger(value) {
    unsafe = !!value;
    if (tamingWin) {
      tamingWin.___.DISABLE_SECURITY_FOR_DEBUGGER = unsafe;
    }
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

  function getOwnPropertyNames(o) {
    // Create result in taming window so it has es53.js wunderbar properties
    //   var result = new tamingWin.Array();
    // Must create using 'eval' in taming window due to Firefox bug:
    //   https://bugzilla.mozilla.org/show_bug.cgi?id=709529
    // h/t metaweta@gmail.com for workaround
    var result = tamingWin.eval('new Array();');
    for (var p in o) {
      if (o.hasOwnProperty(p)) {
        if (!/.*__$/.test(p)) {
          result.push(p);
        }
      }
    }
    return result;
  }

  function isNumericName(n) {
    return typeof n === 'number' || ('' + (+n)) === n;
  }

  function eviscerate(t, f, untame) {
    t.ownKeys___().forEach(function(p) {
      var useProperty =
          // Is 'p' a data (not accessor) property on 't'?
          t[p + '_v___']
          // Is 'p' numeric (which we always whitelisted as a data property)?
          || isNumericName(p);
      if (useProperty) {
        f[p] = untame(t[p]);
        if (!tamingWin.___.rawDelete(t, p)) {
          throw new TypeError(
              'Eviscerating: ' + t + ' failed to delete prop: ' + p);
        }
      }
    });
  }

  function getObjectCtorFor(o) {
    return o.FERAL_FRAME_OBJECT___;
  }

  function banNumerics(o) {
    delete o.NUM____w___;
  }

  function getValueOf(o) {
    return o.valueOf();
  }

  function recordWithMethods(_) {
    // Create result in taming window so it has es53.js wunderbar properties
    //   var o = new tamingWin.Object();
    // Must create using 'eval' in taming window due to Firefox bug:
    //   https://bugzilla.mozilla.org/show_bug.cgi?id=709529
    // h/t metaweta@gmail.com for workaround
    var o = tamingWin.eval('new Object();');
    var i = 0;
    while (i < arguments.length) {
      var p = arguments[i++];
      var v = arguments[i++];
      o[p] = v;
      o[p + '_v___'] = true;
      if (typeof v === 'function') {
        o[p + '_m___'] = true;
      }
    }
    o.freeze___();
    return o;
  }

  //----------------

  function makeES5Frame(div, uriPolicy, es5ready, domOpts) {
    var divs = cajaInt.prepareContainerDiv(div, feralWin, domOpts);
    guestMaker.make(function (guestWin) {
      var frameTamingSchema =
          TamingSchema(tamingHelper);
      var frameTamingMembrane =
          TamingMembrane(tamingHelper, frameTamingSchema.control);
      var domicileAndEmitter = makeDomicileAndEmitter(
          frameTamingMembrane, divs, uriPolicy, guestWin);
      var domicile = domicileAndEmitter && domicileAndEmitter[0];
      var htmlEmitter = domicileAndEmitter && domicileAndEmitter[1];
      var gman = GuestManager(frameTamingSchema, frameTamingMembrane, divs, 
          cajaInt.documentBaseUrl(), domicile, htmlEmitter, guestWin,
          tamingWin.___.USELESS, uriPolicy, es53run);
      gman._loader = guestWin.loadModuleMaker(
        cajaInt.documentBaseUrl(), cajoler, URI.utils);
      guestWin.___.DISABLE_SECURITY_FOR_DEBUGGER = unsafe;
      es5ready(gman);
    });
  }

  function makeDomicileAndEmitter(
      frameTamingMembrane, divs, uriPolicy, guestWin) {
    if (!divs.inner) { return null; }

    // Needs to be accessible by Domado. But markFunction must be done at
    // most once, so markFunction(uriPolicy.rewrite) would only work once,
    // and having side effects on our arguments is best avoided.
    var uriPolicyWrapper = ___.whitelistAll({
      rewrite: ___.markFunc(function (uri, uriEffect, loaderType, hints) {
        if (uri) {
          // Make URI (a constructed object) exported across the membrane.
          // Paranoia: using our non-cajoled copy of uri.js in this frame
          // (as opposed to just uri.clone()), so that we don't expose as much
          // of es53 to the host.
          frameTamingMembrane.tamesTo(URI.parse(uri.toString()), uri);
        }

        return frameTamingMembrane.tame(uriPolicy.rewrite(
          frameTamingMembrane.untame(uri), uriEffect, loaderType, hints));
      })
    });

    // The Domado implementation is obtained from the taming window,
    // since we wish to protect Domado and its dependencies from the
    // ability of guest code to modify the shared primordials.

    // TODO(kpreid): This is probably wrong: we're replacing the feral
    // record imports with the tame constructed object 'window' (issue 1399).

    var targetAttributePresets = undefined;
    if (config.targetAttributePresets) {
      targetAttributePresets = {};
      targetAttributePresets['default'] =
          config.targetAttributePresets['default'];
      targetAttributePresets.whitelist =
          Array.prototype.slice.call(config.targetAttributePresets.whitelist);
      ___.whitelistAll(targetAttributePresets, true);
    }

    function FeralTwinStub() {}

    function permitUntaming(o) {
      if (typeof o === 'object' || typeof o === 'function') {
        frameTamingMembrane.tamesTo(new FeralTwinStub(), o);
     } // else let primitives go normally
    }
    markCallableWithoutMembrane(permitUntaming);

    markCallableWithoutMembrane(frameTamingMembrane.tame);
    markCallableWithoutMembrane(frameTamingMembrane.untame);
    markCallableWithoutMembrane(frameTamingMembrane.tamesTo);
    markCallableWithoutMembrane(frameTamingMembrane.reTamesTo);
    markCallableWithoutMembrane(frameTamingMembrane.hasTameTwin);
    markCallableWithoutMembrane(frameTamingMembrane.hasFeralTwin);

    var domicile = domado.attachDocument(
      '-' + divs.idClass, uriPolicyWrapper, divs.inner,
      targetAttributePresets,
      recordWithMethods(
        'permitUntaming', permitUntaming,
        'tame', frameTamingMembrane.tame,
        'untame', frameTamingMembrane.untame,
        'tamesTo', frameTamingMembrane.tamesTo,
        'reTamesTo', frameTamingMembrane.reTamesTo,
        'hasTameTwin', frameTamingMembrane.hasTameTwin,
        'hasFeralTwin', frameTamingMembrane.hasFeralTwin,
        'tameException', frameTamingMembrane.tameException,
        'untameException', frameTamingMembrane.untameException));
    var imports = domicile.window;

    // Add JavaScript globals to the DOM window object.
    ___.copyToImports(imports, guestWin.___.sharedImports);

    var htmlEmitter = new HtmlEmitter(makeDOMAccessible,
        domicile.htmlEmitterTarget, 
        undefined /* cajoling proxy unused in es53 */, domicile, imports);

    // These ___ variables are interfaces used by cajoled code.
    imports.htmlEmitter___ = htmlEmitter;
    imports.rewriteUriInCss___ = domicile.rewriteUriInCss.bind(domicile);
    imports.rewriteUriInAttribute___ =
      domicile.rewriteUriInAttribute.bind(domicile);
    imports.rewriteTargetAttribute___ =
      domicile.rewriteTargetAttribute.bind(domicile);
    imports.getIdClass___ = domicile.getIdClass.bind(domicile);
    imports.emitCss___ = domicile.emitCss.bind(domicile);

    ___.getId = cajaInt.getId;
    ___.getImports = cajaInt.getImports;
    ___.unregister = cajaInt.unregister;

    feralWin.___.getId = cajaInt.getId;
    feralWin.___.getImports = cajaInt.getImports;
    feralWin.___.unregister = cajaInt.unregister;

    guestWin.___.getId = cajaInt.getId;
    guestWin.___.getImports = cajaInt.getImports;
    guestWin.___.unregister = cajaInt.unregister;

    cajaInt.getId(imports);

    if (!feralWin.___.tamingWindows) {
      feralWin.___.tamingWindows = {};
    }
    feralWin.___.tamingWindows[imports.id___] = tamingWin;

    // domado innerHTML sanitizer uses feralWin.___.plugin_dispatchEvent___
    // html-emitter uses guestWin.___.plugin_dispatchEvent___
    feralWin.___.plugin_dispatchEvent___ = domado.plugin_dispatchEvent;
    guestWin.___.plugin_dispatchEvent___ = domado.plugin_dispatchEvent;
    feralWin.___.plugin_dispatchToHandler___ =
      function (pluginId, handler, args) {
        var tamingWin = feralWin.___.tamingWindows[pluginId];
        return tamingWin.___.plugin_dispatchToHandler___(
            pluginId, handler, args);
      };

    return [domicile, htmlEmitter];
  }

  //----------------

  function es53run(gman, args, moreImports, opt_runDone) {
    function runModule(module) {
      var result = module.instantiate___(guestWin.___, gman.imports);

      if (gman.htmlEmitter) {
        // Ensure that if we have a DOM container but no HTML (only JS), in
        // which case the cajoler gives us a module which does not touch
        // HtmlEmitter, then there is still an empty document generated.
        // htmlEmitter.finish() is robust against extra invocations.
        gman.htmlEmitter.finish();
      }

      if (opt_runDone) {
        opt_runDone(result);
      }
    }

    if (!moreImports.onerror) {
      moreImports.onerror = ___.markFunc(onerror);
    }
    var guestWin = gman.iframe.contentWindow;
    ___.copyToImports(gman.imports, moreImports);

    // TODO(felix8a): not right for multiple guests
    if (args.flash && gman.domicile && tamingWin.cajaFlash) {
      tamingWin.cajaFlash.init(
        feralWin, tamingWin, gman.domicile, config.flashbridge);
    }

    if (args.cajoledJs !== undefined) {
      if (gman.domicile && args.cajoledHtml !== undefined) {
        gman.innerContainer.innerHTML = args.cajoledHtml;
      }
      runModule(guestWin.prepareModuleFromText___(args.cajoledJs));

    } else if (args.uncajoledContent !== undefined) {
      Q.when(
        // unspecified mimeType here means html
        cajoler.cajoleContent(
          args.url, args.uncajoledContent, args.mimeType || 'text/html',
          { idClass: gman.idClass }),
        function (jsonModule) {
          guestWin.Q.when(
            gman._loader.loadCajoledJson___(args.url, jsonModule),
            function (module) {
              runModule(module);
            },
            function (ex) {
              throw new Error('Error loading module: ' + ex);
            });
        },
        function (ex) {
          throw new Error('Error cajoling content: ' + ex);
        });

    } else {
      // uncajoled url
      Q.when(
        // unspecified mimeType here means loader will guess from url
        gman._loader.async(args.url, args.mimeType),
        function (module) {
          runModule(module);
        },
        function (ex) {
          throw new Error('Error loading module: ' + ex);
        });
    }
  }

  function onerror(message, source, lineNum) {
    config.console.log(
        'Uncaught script error: ' + message +
        ' in source: "' + source +
        '" at line: ' + lineNum);
  }

  //----------------

  function makeDomadoRuleBreaker() {
    var ruleBreaker = {
      makeDOMAccessible: ___.markConstFunc(makeDOMAccessible),
      makeFunctionAccessible: ___.markConstFunc(function (f) {
        return markCallableWithoutMembrane(f);
      }),
      writeToPixelArray: ___.markConstFunc(writeToPixelArray),
      copyLengthPropertyIfUninterceptable: ___.markConstFunc(
          function(source, target) {
        if (source.GetOwnProperty___('length')) {
          target.length = source.v___('length');
        }
      }),
      getId: ___.markConstFunc(function() {
        return cajaInt.getId.apply(undefined, arguments);
      }),
      getImports: ___.markConstFunc(function() {
        return cajaInt.getImports.apply(undefined, arguments);
      })
    };
    return ___.whitelistAll(ruleBreaker);
  }

  /**
   * Permit func to be called by cajoled code without modifying the
   * arguments. This should only be used for stuff which ignores the taming
   * membrane deliberately.
   */
  function markCallableWithoutMembrane(func) {
    if (func !== undefined && !func.i___) {
      func.i___ = function () {
        // hide that this is being invoked as a method
        return Function.prototype.apply.call(func, undefined, arguments);
      };
      func.f___ = function (dis, as) {
        // hide that this is being invoked as a method
        return Function.prototype.apply.call(func, dis, as);
      };
      func.new___ = function () {
        if (arguments.length !== 0) {
          throw new TypeError("construction with args not implemented");
        } else {
          return new func();
        }
      };
      func.call_m___ = func;
      func.apply_m___ = func;
    }
    return func;
  }

  // On Firefox 4.0.1, at least, canvas pixel arrays cannot have added
  // properties (such as our w___). Therefore to be able to write them we
  // need uncajoled code to do it. An alternative approach would be to
  // muck with the "Uint8ClampedArray" prototype.
  function writeToPixelArray(source, target, length) {
    for (var i = length-1; i >= 0; i--) {
      target[+i] = source[+i];
    }
  }

  /**
   * This function adds magic ES5/3-runtime properties on an object from
   * the host DOM such that it can be accessed as if it were a guest
   * object. It effectively whitelists everything.
   *
   * This completely breaks the invariants of the ES5/3 taming membrane
   * and the resulting object should under no circumstance be given to
   * untrusted code.
   *
   * It returns its argument, both for convenience and because bridal.js
   * is written to be adaptable to an environment where this action
   * requires wrappers. (Domado is not.)
   */
  function makeDOMAccessible(node) {
    var o = node;

    // This accepts functions because some objects are incidentally
    // functions. makeDOMAccessible does not make functions callable.

    // Not a kind of value which has properties
    if (!((typeof o === 'object' || typeof o === 'function')
          && o !== null)) {
      return o;
    }
    
    if (!('v___' in o)) {
      // IE<=8 needs wrappers for text nodes and attribute nodes.  Note, we
      // make no effort to return the same wrapper for the same node.
      // TODO(felix8a): verify the contract violation is unimportant.
      if (ie8nodes && node.nodeType && node.nodeType !== 1) {
        o = { node___: node };
      }
      o.v___ = function (p) {
        // In Chrome 22.0.1229.94, node.childNodes gets a different
        // prototype depending on the context of the calling function;
        // once we no longer care about that, we can return node[p]
        // instead.
        return readPropertyAsHostFrame(node, p);
      };
      o.v___.ThisIsMakeDomAccessible = true;
      o.w___ = function (p, v) {
        node[p] = v;
      };
      o.m___ = function (p, as) {
        // From es53 tameObjectWithMethods without the membrane features.
        p = '' + p;
        if (('' + (+p)) !== p && !(/__$/).test(p)) {
          var method = node[p];
          if (typeof method === 'function') {
            return method.apply(node, as);
          }
          // IE<=8 DOM methods are objects not functions
          if (typeof method === 'object' &&
              (method+'').substr(0, 10) === '\nfunction ') {
            // IE<=8 DOM methods lack .apply
            return Function.prototype.apply.call(method, node, unwrapNodes(as));
          }
        }
        throw new TypeError('Not a function: ' + p);
      };
      o.HasProperty___ = function (p) { return p in node; };
    } else /* object has v___ already */ {
      if (!o.v___.ThisIsMakeDomAccessible) {
        // object is a ES53 object but not by us, which indicates a serious
        // problem.
        throw new Error(
            'shouldn\'t happen: ES5/3 object passed to makeDOMAccessible');
      }
    }
    
    return o;
  }

  // This does shallow unwrapping of IE<=8 wrapped nodes, which is
  // sufficient to handle guest code calling DOM functions like
  // removeChild.  This may not be sufficient if a caja environment has
  // tamed functions that expect to receive arrays of nodes or structures
  // containing nodes.
  function unwrapNodes(as) {
    var o = [];
    for (var i = 0; i < as.length; as++) {
      o[i] = as[i].node___ || as[i];
    }
    return o;
  }
}

// Exports for closure compiler.
if (typeof window !== 'undefined') {
  window['ES53FrameGroup'] = ES53FrameGroup;
}
