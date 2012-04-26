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
 * @requires TamingMembrane
 * @requires jsonRestTransportMaker
 * @requires URI
 * @overrides window
 */

function ES53FrameGroup(cajaInt, config, tamingWin, feralWin, guestMaker) {
  // Note: in IE<9, window !== window.self
  if (tamingWin !== window.self) {
    throw new Error('wrong frame');
  }

  var cajoler = cajolingServiceClientMaker(
    cajaInt.joinUrl(config.server, 'cajole'),
    jsonRestTransportMaker(),
    true,
    config.debug,
    config.console);

  var tamingMembrane = TamingMembrane(recordWithMethods(
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
      'BASE_OBJECT_CONSTRUCTOR', tamingWin.___.BASE_OBJECT_CONSTRUCTOR));

  markCallableWithoutMembrane(permitUntaming);
  markCallableWithoutMembrane(untamesToWrapper);
  markCallableWithoutMembrane(insiderTame);
  markCallableWithoutMembrane(insiderUntame);
  markCallableWithoutMembrane(insiderTamesTo);
  markCallableWithoutMembrane(insiderHasTameTwin);

  // On IE<=8 you can't add properties to text nodes or attribute nodes.
  // We detect that here and set a flag ie8nodes for makeDOMAccessible().
  // Note, this flag has to be set before the call to Domado() 
  var ie8nodes = false;
  try {
    feralWin.document.createTextNode('x').v___ = 1;
  } catch (e) {
    ie8nodes = true;
  }

  var domado = Domado(
      recordWithMethods(
        'permitUntaming', permitUntaming,
        'untamesToWrapper', untamesToWrapper,
        'tame', insiderTame,
        'untame', insiderUntame,
        'tamesTo', insiderTamesTo,
        'hasTameTwin', insiderHasTameTwin),
      makeDomadoRuleBreaker());

  var frameGroup = {
  
    makeDefensibleObject___: ___.makeDefensibleObject,
    makeDefensibleFunction___: ___.makeDefensibleFunction,  
  
    tame: tamingMembrane.tame,
    untame: tamingMembrane.untame,
    unwrapDom: unwrapDom,
    markReadOnlyRecord: tamingMembrane.markTameAsReadOnlyRecord,
    markFunction: tamingMembrane.markTameAsFunction,
    markCtor: tamingMembrane.markTameAsCtor,
    markXo4a: tamingMembrane.markTameAsXo4a,
    grantMethod: tamingMembrane.grantTameAsMethod,
    grantRead: tamingMembrane.grantTameAsRead,
    grantReadWrite: tamingMembrane.grantTameAsReadWrite,

    USELESS: tamingWin.___.USELESS,
    iframe: window.frameElement,

    makeES5Frame: makeES5Frame
  };

  return frameGroup;

  //----------------
  
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

  /**
   * Allow a guest constructed object (such as a canvas context) to
   * be passed through the taming membrane (largely uselessly) by giving
   * it a stub feral twin. This exists primarily for the test suite.
   */
  function permitUntaming(o) {
    if (typeof o === 'object' || typeof o === 'function') {
      tamingMembrane.tamesTo(new FeralTwinStub(), o);
    } // else let primitives go normally
  }

  /**
   * Allow a guest-constructed DOM node to be
   * tamed to a domicile-specific wrapper.
   */
  function untamesToWrapper(feral, tame) {
    tamingMembrane.tamesTo(new FeralNodeWrapper(feral), tame);
  }

  function insiderTame(f) {
    return tamingMembrane.tame(f);
  }

  function insiderUntame(f) {
    return tamingMembrane.untame(f);
  }

  function insiderTamesTo(f, t) {
    return tamingMembrane.tamesTo(f, t);
  }

  function insiderHasTameTwin(o) {
    return tamingMembrane.hasTameTwin(o);
  }

  //----------------

  function makeES5Frame(div, uriPolicy, es5ready, domOpts) {
    var divs = cajaInt.prepareContainerDiv(div, feralWin, domOpts);
    guestMaker.make(function (guestWin) {
      var domicile = makeDomicile(divs, uriPolicy, guestWin);
      var gman = GuestManager(divs, domicile, guestWin, es53run);
      gman._loader = guestWin.loadModuleMaker(
        cajaInt.documentBaseUrl(), cajoler, URI.utils);
      es5ready(gman);
    });
  }

  function makeDomicile(divs, uriPolicy, guestWin) {
    if (!divs.inner) { return null; }

    // Needs to be accessible by Domado. But markFunction must be done at
    // most once, so markFunction(uriPolicy.rewrite) would only work once,
    // and having side effects on our arguments is best avoided.
    var uriPolicyWrapper = ___.whitelistAll({
      rewrite: ___.markFunc(function (uri, uriEffect, loaderType, hints) {
        return tamingMembrane.tame(uriPolicy.rewrite(
          uri, uriEffect, loaderType, hints));
      })
    });

    // The Domita implementation is obtained from the taming window,
    // since we wish to protect Domita and its dependencies from the
    // ability of guest code to modify the shared primordials.

    // TODO(kpreid): This is probably wrong: we're replacing the feral
    // record imports with the tame constructed object 'window'.

    var domicile = domado.attachDocument(
      '-' + divs.idClass, uriPolicyWrapper, divs.inner);
    var imports = domicile.window;

    // Add JavaScript globals to the DOM window object.
    ___.copyToImports(imports, guestWin.___.sharedImports);

    // These ___ variables are interfaces used by cajoled code.
    imports.htmlEmitter___ = new HtmlEmitter(
      makeDOMAccessible, divs.inner, domicile, imports);
    imports.rewriteUriInCss___ = domicile.rewriteUriInCss.bind(domicile);
    imports.rewriteUriInAttribute___ =
      domicile.rewriteUriInAttribute.bind(domicile);
    imports.getIdClass___ = domicile.getIdClass.bind(domicile);
    imports.emitCss___ = domicile.emitCss.bind(domicile);
    imports.tameNodeAsForeign___ = domicile.tameNodeAsForeign.bind(domicile);

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

    return domicile;
  }

  //----------------

  function es53run(gman, args, moreImports, opt_runDone) {
    function runModule(module) {
      var result = module.instantiate___(guestWin.___, gman.imports);
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
    if (args.flash) {
      var tamingFlash = tamingWin.cajaFlash;
      if (gman.domicile && tamingFlash && tamingFlash.init) {
        tamingFlash.init(
          feralWin, gman.imports, tamingWin, gman.domicile, guestWin);
      }
    }

    if (args.isCajoled) {
      if (gman.domicile && args.cajoledHtml !== undefined) {
        gman.innerContainer.innerHTML = args.cajoledHtml;
      }
      runModule(guestWin.prepareModuleFromText___(args.cajoledJs));

    } else if (args.uncajoledContent !== undefined) {
      Q.when(
        // unspecified mimeType here means html
        cajoler.cajoleContent(
          args.url, args.uncajoledContent, args.mimeType || 'text/html',
          gman.idClass),
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
    config.log('Uncaught script error: ' + message +
               ' in source: "' + source +
               '" at line: ' + lineNum);
  }

  //----------------
      
  function makeDomadoRuleBreaker() {
    // TODO(felix8a): should markFunc be markFuncFreeze?
    var ruleBreaker = {
      makeDOMAccessible: ___.markFunc(makeDOMAccessible),
      makeFunctionAccessible: ___.markFunc(function (f) {
        return markCallableWithoutMembrane(f);
      }),
      writeToPixelArray: ___.markFunc(writeToPixelArray),
      getId: ___.markFunc(function () {
        return cajaInt.getId.apply(undefined, arguments);
      }),
      getImports: ___.markFunc(function () {
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
      tamingMembrane.tamesTo(func, func);
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

    // Testing for own properties, not 'in', because some quirk of Firefox
    // makes event objects appear as if they have the taming frame's
    // prototype after being passed into taming frame code (!), so we want
    // to be able to override Object.prototype.v___ etc. Except for that,
    // it would be safer to not allow applying this to apparently defined-
    // in-taming-frame objects.
    if ((typeof o === 'object' || typeof o === 'function')
        && o !== null
        && !Object.prototype.hasOwnProperty.call(o, 'v___')) {
      // IE<=8 needs wrappers for text nodes and attribute nodes.  Note, we
      // make no effort to return the same wrapper for the same node.
      // TODO(felix8a): verify the contract violation is unimportant.
      if (ie8nodes && node.nodeType && node.nodeType !== 1) {
        o = { node___: node };
      }
      o.v___ = function (p) {
        return node[p];
      };
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

  function FeralTwinStub() {}
  function FeralNodeWrapper(node) { this.node = node; }
  function unwrapDom(wrapper) {
    if (wrapper instanceof FeralNodeWrapper) {
      return wrapper.node;
    }
    return wrapper;
  }
}

// Exports for closure compiler.
if (typeof window !== 'undefined') {
  window['ES53FrameGroup'] = ES53FrameGroup;
}
