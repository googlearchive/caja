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
 * @fileoverview Allows containers to mimic swfobject for cajoled gadgets
 *
 * @author felix8a@gmail.com
 * @requires document
 * @overrides window
 * @provides cajaFlash
 */

// TODO(felix8a): Flash objects instantiated this way are never gc'ed

var cajaFlash = {};

(function() {

  // Get an Object without Caja.
  var cleanFrame = document.createElement('iframe');
  var where = document.getElementsByTagName('script')[0];
  where.parentNode.insertBefore(cleanFrame, where);
  var cleanObject = cleanFrame.contentWindow.Object;
  var cleanString = cleanFrame.contentWindow.String;
  where.parentNode.removeChild(cleanFrame);

  // Convert a tame object into a clean string->string map
  function cleanStringMap(o) {
    var result = cleanObject();
    if (!o) { return result; }
    if (o.allKeys___) {
      // es53 object
      var keys = o.allKeys___();
      for (var i = 0; i < keys.length; i++) {
        result[cleanString(keys[i])] = cleanString(o.v___(keys[i]));
      }
    } else {
      // non-es53 object
      for (var k in o) {
        if (!/__$/.test(k)) {
          result[cleanString(k)] = cleanString(o[k]);
        }
      }
    }
    return result;
  }

  // Return a function that throws "not implemented yet".
  function unimp(o, name) {
    o[name] = function() {
      throw Error(name + ' is not implemented yet.');
    };
  }

  // Given two version strings, return the higher version.
  function versionMax(a, b) {
    if (!a) { return b; }
    if (!b) { return a; }
    var av = a.split(/[.]/);
    var bv = b.split(/[.]/);
    var n = Math.min(av.length, bv.length);
    for (var i = 0; i < n; i++) {
      var avi = +av[i];
      var bvi = +bv[i];
      if (avi < bvi) {
        return b;
      } else if (bvi < avi) {
        return a;
      }
    }
    return (av.length < bv.length) ? b : a;
  }

  function initCallbacks(feralWin, tamingWin, domicile) {
    if (!feralWin.caja.policy) {
      feralWin.caja.policy = feralWin.Object();
    }
    if (!feralWin.caja.policy.flash) {
      feralWin.caja.policy.flash = feralWin.Object();
    }
    var flash = feralWin.caja.policy.flash;

    // A map from context id (integer) to swf object.
    flash.objects = feralWin.Array();

    // Called when bridge finishes loading target swf.
    flash.onLoaderInit = function onLoaderInit(context) {};

    // Called when bridge fails to load target swf.
    flash.onLoaderError = function onLoaderError(context) {};

    // Called to service ExternalInterface.addCallback()
    flash.onAddCallback = function onAddCallback(context, fnName) {
      var m = /^caja_(\w+)/.exec(fnName);
      if (!m) {
        throw Error('bad function name ' + fnName);
      }
      var baseFnName = m[1];
      var obj = flash.objects[context];
      if (!obj || !obj[fnName]) {
        throw Error('bad context ' + context);
      }
      var el = domicile.tameNode(obj, true);
      if (!el) {
        throw Error("Can't tame " + obj);
      }
      el[baseFnName] = function (/*varargs*/) {
        var args = Array.prototype.slice.call(arguments);
        var result = obj[fnName].apply(obj, feralWin.caja.untame(args));
        return feralWin.caja.tame(result);
      };
      tamingWin.___.markConstFunc(el[baseFnName], baseFnName);
      if (!tamingWin.___.canRead(el, baseFnName)) {
        tamingWin.___.grantRead(el, baseFnName);
      }
    };

    // Called to service ExternalInterface.call()
    flash.onCall = function onCall(context, fnName, args) {
      var fn = domicile.window[fnName];
      if (!tamingWin.___.isFunction(fn)) { return void 0; }
      var result = fn.f___(feralWin.caja.USELESS, feralWin.caja.tame(args));
      return feralWin.caja.untame(result);
    };

    // Called to service flash.net.navigateToURL()
    flash.onNavigateToURL = function onNavigateToURL(context, req) {
      // TODO(felix8a): use domicile.rewriteUri (which doesn't work)
      var rewritten = domicile.rewriteUriInAttribute(req.url, 'a', 'href');
      if (!rewritten) {
        throw Error('URI policy denied ' + req.url);
      }
      if (!window.open(rewritten, '_blank')) {
        throw Error('Failed to open ' + rewritten);
      }
    };
  }

  // http://code.google.com/p/swfobject/wiki/api
  // http://code.google.com/p/swfobject/source/browse/wiki/api.wiki?r=383

  function initSwfobject(feralWin, tamingWin, domicile, opt_flashbridge) {
    if (!feralWin.swfobject) { return; }

    var swf = domicile.window.swfobject;
    if (!swf) {
      swf = domicile.window.swfobject = tamingWin.Object();
      tamingWin.___.grantRead(domicile.window, 'swfobject');
    }

    swf.ua = feralWin.caja.tame(feralWin.swfobject.ua);

    swf.embedSWF = function embedSWF(
      swfUrl, id, width, height, version,
      expressInstall, flashvars, params, attrs, cb)
    {
      var context = feralWin.caja.policy.flash.objects.length;
      feralWin.caja.policy.flash.objects[context] = feralWin.Object();

      // TODO(felix8a): use domicile.rewriteUri (which doesn't work)
      var rewritten = domicile.rewriteUriInAttribute(swfUrl, 'img', 'src');
      if (!rewritten) {
        throw Error('URI policy denied ' + swfUrl);
      }
      var outSwfUrl = (
        (opt_flashbridge || 'flashbridge.swf') +
        '?__CAJA_cajaContext=' + context +
        '&__CAJA_src=' + encodeURIComponent(swfUrl));
      // TODO(felix8a): maybe should be rewritten url, but proxied swfs
      // have odd implications

      var outId = domicile.suffix(id);
      var outWidth = +width;
      var outHeight = +height;
      // 11.2 is the last supported version on linux and solaris
      var outVersion = versionMax(version, '11.2');
      var outExpressInstall = false;
      var outFlashvars = cleanStringMap(flashvars, tamingWin);

      var outParams = cleanStringMap(params, tamingWin);
      // needed for flashbridge to load the target swf
      outParams.allowNetworking = 'all';
      // allow script for flashbridge but not for target swf
      outParams.allowScriptAccess = 'same-domain';
      // make flash honor the html visual stack
      outParams.wmode = 'transparent';

      // TODO(felix8a): support attrs
      var outAttrs = cleanObject();

      var outCb = function (args) {
        feralWin.caja.policy.flash.objects[context] = args.ref;
        if (!tamingWin.___.isFunction(cb)) { return; }
        var tameArgs = {
          success: args.success,
          id: args.id,
          ref: domicile.tameNode(args.ref, true)
        };
        tameArgs = feralWin.caja.tame(tameArgs);
        cb.f___(feralWin.caja.USELESS, [tameArgs]);
      };

      feralWin.swfobject.embedSWF(
        outSwfUrl, outId, outWidth, outHeight, outVersion,
        outExpressInstall, outFlashvars, outParams, outAttrs, outCb);
    };

    // TODO(felix8a): implement some more of these swfobject functions
    unimp(swf, 'registerObject');
    unimp(swf, 'getObjectById');
    unimp(swf, 'getFlashPlayerVersion');
    unimp(swf, 'hasFlashPlayerVersion');
    unimp(swf, 'addLoadEvent');
    unimp(swf, 'addDomLoadEvent');
    unimp(swf, 'createSWF');
    unimp(swf, 'removeSWF');
    unimp(swf, 'createCSS');
    unimp(swf, 'getQueryParamValue');
    unimp(swf, 'switchOffAutoHideShow');
    unimp(swf, 'showExpressInstall');
    tamingWin.___.whitelistAll(swf);
  }

  function findElByClass(domicile, name) {
    var els = domicile.document.getElementsByClassName(name);
    return els && els[0] && domicile.feralNode(els[0]);
  }

  // Setup functions and callbacks for tamed Flash.
  cajaFlash.init = function init(feralWin, tamingWin, domicile,
      opt_flashbridge) {
    initCallbacks(feralWin, tamingWin, domicile);
    initSwfobject(feralWin, tamingWin, domicile, opt_flashbridge);

    // Called from html-emitter
    function cajaHandleEmbed(params) {
      var el = findElByClass(domicile, params.id);
      if (!el) { return; }
      el.id = domicile.suffix(params.id);
      if (domicile.window.swfobject) {
        domicile.window.swfobject.embedSWF(
          params.src, params.id, params.width, params.height);
      }
    }
    domicile.window.DefineOwnProperty___('cajaHandleEmbed', {
      value: tamingWin.___.markConstFunc(cajaHandleEmbed)
    });
  };
})();

// Exports for closure compiler.
if (typeof window !== 'undefined') {
  window['cajaFlash'] = cajaFlash;
}
