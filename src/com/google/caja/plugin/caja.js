// Copyright (C) 2010 Google Inc.
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
 * @fileoverview ... TODO ihab.awad
 * @author kpreid@switchb.org
 * @author ihab.awad@gmail.com
 * @author jasvir@gmail.com
 * \@requires document, setTimeout, XMLHttpRequest
 * \@overrides window
 * \@provides caja
 */

var caja = (function () {
  var cajaBuildVersion = '%VERSION%';
  var defaultServer = 'https://caja.appspot.com/';
  var defaultFrameGroup;
  var readyQueue = [];
  var registeredImports = [];
  var nextId = 0;

  var UNREADY = 'UNREADY', PENDING = 'PENDING', READY = 'READY';
  var state = UNREADY;

  var GUESS = 'GUESS';

  var ajaxCounter = 1;
  var unsafe = false;

  var loaderDocument;
  function proxyFetchMaker(proxyServer) {
    return function (url, mime, callback) {
      if (!url) {
        callback(undefined);
        return;
      }
      var rndName = 'caja_ajax_' + ajaxCounter++;
      window[rndName] = function (result) {
        try {
          callback(result);
        } finally {
          // GC yourself
          window[rndName] = undefined;
        }
      };
      // TODO(jasvir): Make it so this does not pollute the host page
      // namespace but rather just the loaderFrame
      installSyncScript(rndName,
        proxyServer ? String(proxyServer) : caja['server']
        + '/cajole?url=' + encodeURIComponent(url)
        + '&input-mime-type=' + encodeURIComponent(mime)
        + '&transform=PROXY'
        + '&callback=' + encodeURIComponent(rndName)
        + '&alt=json-in-script');
    };
  }

  function xhrFetcher(url, mime, callback) {
    var request = new XMLHttpRequest();
    request.open('GET', url, true);
    request.overrideMimeType(mime);
    request.onreadystatechange = function() {
      if(request.readyState == 4) {
        callback({ "html": request.responseText });
      }
    };
    request.send();
  }

  var uriPolicies = {
    'net': {
      'rewriter': {
        'NO_NETWORK': function () { return null; },
        'ALL': function (uri) { return String(uri); }
      },
      'fetcher': {
        'USE_XHR': xhrFetcher,
        'USE_AS_PROXY': proxyFetchMaker
      },
      'NO_NETWORK': {
        'rewrite': function () { return null; },
        'fetch': function() { }
      },
      'ALL': {
        'rewrite': function (uri) { return String(uri); },
        'fetch': proxyFetchMaker(undefined)
      },
      'only': policyOnly
    },

    'ATTRIBUTETYPES': undefined,
    'LOADERTYPES': undefined,
    'URIEFFECTS': undefined
  };

  var caja = {
    // Normal entry points
    'initialize': initialize,
    'load': load,
    'whenReady': whenReady,

    // URI policies
    'policy': uriPolicies,

    // Reference to the taming frame in the default frameGroup
    'iframe': null,

    // Reference to the USELESS object for function invocation (for testing)
    'USELESS': undefined,

    // Taming functions for the default frameGroup
    'tame': premature,
    'tamesTo': premature,
    'reTamesTo': premature,
    'untame': premature,
    'unwrapDom': premature,
    'markReadOnlyRecord': premature,
    'markFunction': premature,
    'markCtor': premature,
    'markXo4a': premature,
    'grantMethod': premature,
    'grantRead': premature,
    'grantReadWrite': premature,
    'adviseFunctionBefore': premature,
    'adviseFunctionAfter': premature,
    'adviseFunctionAround': premature,
    'makeDefensibleObject___': premature,
    'makeDefensibleFunction___': premature,

    // Esoteric functions
    'initFeralFrame': initFeralFrame,
    'makeFrameGroup': makeFrameGroup,
    'configure': makeFrameGroup,
    'disableSecurityForDebugger': disableSecurityForDebugger,
    'Q': premature,

    // For use by the Caja test suite only. Should not be used for any other
    // purpose and is hard to use correctly.
    'testing_makeDomadoRuleBreaker': premature,

    // unused, removed by Closure
    closureCanary: 1
  };

  // Internal functions made available to FrameGroup maker
  var cajaInt = {
    'documentBaseUrl': documentBaseUrl,
    'getId': getId,
    'getImports': getImports,
    'joinUrl': joinUrl,
    'loadCajaFrame': loadCajaFrame,
    'prepareContainerDiv': prepareContainerDiv,
    'unregister': unregister,
    'readPropertyAsHostFrame': readPropertyAsHostFrame
  };

  //----------------

  function premature() {
    throw new Error('Calling taming function before Caja is ready');
  }

  function disableSecurityForDebugger(value) {
    unsafe = !!value;
    if (defaultFrameGroup) {
      defaultFrameGroup['disableSecurityForDebugger'](value);
    }
  }

  /**
   * Returns a URI policy that allows one URI and denies the rest.
   */
  function policyOnly(allowedUri) {
    allowedUri = String(allowedUri);
    return {
      'rewrite': function (uri) {
        uri = String(uri);
        return uri === allowedUri ? uri : null;
      }
    };
  }

  /**
   * Creates the default frameGroup with the given config.
   * See {@code makeFrameGroup} for config parameters.
   */
  function initialize(config /*, opt_onSuccess, opt_onFailure */) {
    if (state !== UNREADY) {
      throw new Error('Caja cannot be initialized more than once');
    }
    var onSuccess = arguments[1];
    var onFailure = arguments[2];
    state = PENDING;
    makeFrameGroup(config, function (frameGroup, es5Mode) {
      defaultFrameGroup = frameGroup;
      caja['iframe'] = frameGroup['iframe'];
      caja['USELESS'] = frameGroup['USELESS'];
      for (var i in caja) {
        if (caja[i] === premature) {
          caja[i] = frameGroup[i];
        }
      }
      frameGroup['disableSecurityForDebugger'](unsafe);
      state = READY;
      var detail = {};
      detail['es5Mode'] = es5Mode;
      if ("function" === typeof onSuccess) {
        onSuccess(detail);
      }
      whenReady(null);
    }, function(err) {
      state = UNREADY;
      onFailure(err);
    });
  }

  /**
   * Creates a guest frame in the default frameGroup.
   */
  function load(div, uriPolicy, loadDone, domOpts) {
    uriPolicy = uriPolicy || caja['policy']['net']['NO_NETWORK'];
    if (state === UNREADY) {
      initialize({});
    }
    whenReady(function () {
      defaultFrameGroup['makeES5Frame'](div, uriPolicy, loadDone, domOpts);
    });
  }

  /**
   * Defers func until the default frameGroup is ready.
   */
  function whenReady(opt_func) {
    if (typeof opt_func === 'function') {
      readyQueue.push(opt_func);
    }
    if (state === READY) {
      for (var i = 0; i < readyQueue.length; i++) {
        setTimeout(readyQueue[i], 0);
      }
      readyQueue = [];
    }
  }

  /**
   * Create a Caja frame group. A frame group maintains a relationship with a
   * Caja server and some configuration parameters. Most Web pages will only
   * need to create one frame group.
   *
   * Recognized configuration parameters are:
   *
   *     server - the URL to a Caja server. Except for unique cases,
   *         this must be the server from which the "caja.js" script was
   *         sourced.
   *
   *     resources - the URL to a directory containing the resource files.
   *         If not specified, it defaults to the value of 'server'.
   *
   *     debug - whether debugging is supported. At the moment, debug support
   *         means that the files loaded by Caja are un-minified to help with
   *         tracking down problems.
   *
   *     es5Mode - If set to true or false, forces or prohibits ES5
   *         mode, rather than autodetecting browser capabilities
   *         capable of supporting at least maxAcceptableSeverity.
   *
   *     maxAcceptableSeverity - Severity of browser bugs greater than
   *         this level cause failover from ES5 to ES5/3 if es5Mode
   *         is undefined
   *
   *     forceES5Mode - If set to true or false, forces or prohibits ES5
   *         mode, rather than autodetecting browser capabilities.
   *         Equivalent to setting es5Mode and maxAcceptableSeverity
   *         to the most insecure value. This should be used strictly
   *         for testing/debugging purposes.
   *
   *     console - Optional user-supplied alternative to the browser's native
   *         'console' object.
   *
   *     targetAttributePresets - Optional structure giving default and
   *         whitelist for the 'target' parameter of anchors and forms.
   *
   *     log - Optional user-supplied alternative to the browser's native
   *         'console.log' function.
   *
   *     flashbridge - Optional, location of flashbridge.swf.  This needs
   *         to be on the same domain as the host page.
   *
   * @param config an object literal containing configuration parameters.
   * @param frameGroupReady function to be called back with a reference to
   *     the newly created frame group.
   */
  function makeFrameGroup(config, frameGroupReady, onFailure) {
    initFeralFrame(window);
    config = resolveConfig(config);
    caja['server'] = config['server'];
    if (config['es5Mode'] === false || 
        (config['es5Mode'] !== true && unableToSES())) {
      initES53(config, frameGroupReady, onFailure);
    } else {
      trySES(config, frameGroupReady, onFailure);
    }
  }

  /**
   * Returns a full config based on the given partial config.
   */
  function resolveConfig(partial) {
    partial = partial || {};
    var full = {};
    full['server'] = String(
      partial['server'] || partial['cajaServer'] || defaultServer);
    full['resources'] = String(partial['resources'] || full['server']);
    full['debug'] = !!partial['debug'];
    // Full config no longer has forceES5Mode
    // forceES5Mode passes it's value on to es5Mode and maxAcceptableSeverity
    if ('forceES5Mode' in partial && 'es5Mode' in partial) {
      throw new Error(
        'Cannot use both forceES5Mode and es5Mode in the same config');
    }
    if (partial['forceES5Mode'] !== undefined) {
      full['es5Mode'] = !!partial['forceES5Mode'];
      full['maxAcceptableSeverity'] = 'NOT_ISOLATED';
    } else {
      full['es5Mode'] =
        partial['es5Mode'] === undefined ? GUESS : !!partial['es5Mode'];
      full['maxAcceptableSeverity'] = 
        String(partial['maxAcceptableSeverity'] || 'SAFE_SPEC_VIOLATION');
    }
     
    if (partial['console']) {
      full['console'] = partial['console'];
    } else if (window['console']
        && typeof(window['console']['log']) === 'function') {
      full['console'] = window['console'];
    } else {
      full['console'] = undefined;
    }
    full['log'] = partial['log'] || function (varargs) {
      full['console'] && full['console']['log']
          .apply(full['console'], arguments);
    };
    if (partial['targetAttributePresets']) {
      if (!partial['targetAttributePresets']['default']) {
        throw 'targetAttributePresets must contain a default';
      }
      if (!partial['targetAttributePresets']['whitelist']) {
        throw 'targetAttributePresets must contain a whitelist';
      }
      if (partial['targetAttributePresets']['whitelist']['length'] === 0) {
        throw 'targetAttributePresets.whitelist array must be nonempty';
      }
      full['targetAttributePresets'] = partial['targetAttributePresets'];
    }
    if (typeof(partial['cajolingServiceClient']) === 'object'){
      full['cajolingServiceClient'] = partial['cajolingServiceClient'];
    }
    return full;
  }

  function initFeralFrame(feralWin) {
    if (feralWin['Object']['FERAL_FRAME_OBJECT___'] === feralWin['Object']) {
      return;
    }
    feralWin['___'] = {};
    feralWin['Object']['FERAL_FRAME_OBJECT___'] = feralWin['Object'];
  }

  //----------------

  function initES53(config, frameGroupReady, onFailure) {
    // TODO(felix8a): with api change, can start cajoler early too
    var guestMaker = makeFrameMaker(config, 'es53-guest-frame');
    loadCajaFrame(config, 'es53-taming-frame', function (tamingWin) {
      var fg = tamingWin['ES53FrameGroup'](
          cajaInt, config, tamingWin, window, guestMaker);
      frameGroupReady(fg, false /* es5Mode */);
    });
  }

  function trySES(config, frameGroupReady, onFailure) {
    var guestMaker = makeFrameMaker(config, 'ses-guest-frame');
    loadCajaFrame(config, 'ses-taming-frame', function (tamingWin) {
      var mustSES = config['es5Mode'] === true;
      if (canSES(tamingWin['ses'], config['maxAcceptableSeverity'])) {
        var fg = tamingWin['SESFrameGroup'](
            cajaInt, config, tamingWin, window, guestMaker);
        frameGroupReady(fg, true /* es5Mode */);
      } else if (!mustSES) {
        config['log']('Unable to use SES.  Switching to ES53.');
        // TODO(felix8a): set a cookie to remember this?
        initES53(config, frameGroupReady, onFailure);
      } else {
        var err = new Error('ES5 mode requested but browser is unsupported');
        if ("function" === typeof onFailure) {
          onFailure(err);
        } else {
          throw err;
        }
      }
    });
  }

  function canSES(ses, severity) {
    return ses['ok'](ses['severities'][severity]);
  }

  // Fast rejection of SES.  If this works, repairES5 might still fail, and
  // we'll fall back to ES53 then.
  function unableToSES() {
    return !Object.getOwnPropertyNames;
  }

  //----------------

  /**
   * Returns an object that wraps loadCajaFrame() with preload support.
   * Calling frameMaker.preload() will start creation of a new frame now,
   * and make it available to a later call to frameMaker.make().
   */
  function makeFrameMaker(config, filename) {
    var IDLE = 'IDLE', LOADING = 'LOADING', WAITING = 'WAITING';
    var preState = IDLE, preWin, preReady;
    var self = {
      'preload': function () {
        if (preState === IDLE) {
          preState = LOADING;
          preWin = null;
          loadCajaFrame(config, filename, function (win) {
            preWin = win;
            consumeIfReady();
          });
        }
      },
      'make': function (onReady) {
        if (preState === LOADING) {
          preState = WAITING;
          preReady = onReady;
          consumeIfReady();
        } else {
          loadCajaFrame(config, filename, onReady);
        }
      }
    };
    self['preload']();
    return self;

    function consumeIfReady() {
      if (preState === WAITING && preWin) {
        var win = preWin, ready = preReady;
        preState = IDLE;
        preWin = null;
        preReady = null;
        ready(win);
      }
    }
  }

  //----------------

  function loadCajaFrame(config, filename, frameReady) {
    var frameWin = createFrame(filename);
    // debuggable or minified.  ?debug=1 inhibits compilation in shindig
    var suffix = config['debug'] ? '.js?debug=1' : '.opt.js?debug=1';
    var url = joinUrl(
      config['resources'],
      cajaBuildVersion + '/' + filename + suffix);
    // The particular interleaving of async events shown below has been found
    // necessary to get the right behavior on Firefox 3.6. Otherwise, the
    // iframe silently fails to invoke the cajaIframeDone___ callback.
    setTimeout(function () {
      frameWin['cajaIframeDone___'] = function () {
        versionCheck(config, frameWin, filename);
        frameReady(frameWin);
      };
      // TODO(jasvir): Test what the latency doing this on all browsers is
      // and why its necessary
      setTimeout(function () {
        installAsyncScript(frameWin, url);
      }, 0);
    }, 0);
  }

  // Throws an error if frameWin has the wrong Caja version
  function versionCheck(config, frameWin, filename) {
    if (cajaBuildVersion !== frameWin['cajaBuildVersion']) {
      var message = 'Version error: caja.js version ' + cajaBuildVersion +
        ' does not match ' + filename + ' version ' +
        frameWin['cajaBuildVersion'] + '.';

      var majorCajaVersion = String(cajaBuildVersion).split(/[mM]/)[0];
      var majorWinVersion =
        String(frameWin['cajaBuildVersion']).split(/[mM]/)[0];
      if (majorCajaVersion === majorWinVersion) {
        message += '  Continuing because major versions match.';
        config['log'](message);
      } else {
        config['log'](message);
        throw new Error(message);
      }
    }
  }

  function prepareContainerDiv(div, feralWin, domOpts) {
    if (div && feralWin['document'] !== div.ownerDocument) {
      throw '<div> provided for ES5 frame must be in main document';
    }
    domOpts = domOpts || {};
    var opt_idClass = domOpts ? domOpts['idClass'] : void 0;
    var idClass = opt_idClass || ('caja-guest-' + nextId++ + '___');
    var inner = null;
    var outer = null;
    if (div) {
      // Class-name hooks: The host page can
      // * match all elements between its content and the guest content as
      //   .caja-vdoc-wrapper
      // * match the outermost such element using .caja-vdoc-outer
      // * match the innermost such element using .caja-vdoc-inner
      // This scheme has been chosen to be potentially forward-compatible in the
      // event that we switch to more or less than 2 wrappers.
      
      inner = div.ownerDocument.createElement('div');
      inner.className = 'caja-vdoc-inner caja-vdoc-wrapper';
      inner.style.display = 'block';
      inner.style.position = 'relative';

      outer = div.ownerDocument.createElement('div');
      outer.className = 'caja-vdoc-outer caja-vdoc-wrapper';
      outer.style.position = 'relative';
      outer.style.overflow = 'hidden';
      outer.style.display = 'block';
      outer.style.margin = '0';
      outer.style.padding = '0';
      // Move existing children (like static HTML produced by the cajoler)
      // into the inner container.
      while (div.firstChild) {
        inner.appendChild(div.firstChild);
      }
      outer.appendChild(inner);
      div.appendChild(outer);
    }
    return {
      'idClass': idClass,
      'inner': inner,
      'outer': outer
    };
  }

  // Creates a new iframe and returns its contentWindow.
  function createFrame(opt_className) {
    var frame = document.createElement('iframe');
    frame.style.display = "none";
    frame.width = 0;
    frame.height = 0;
    frame.className = opt_className || '';
    var where = document.getElementsByTagName('script')[0];
    where.parentNode.insertBefore(frame, where);
    return frame.contentWindow;
  }

  function installAsyncScript(frameWin, scriptUrl) {
    var frameDoc = frameWin['document'];
    var script = frameDoc.createElement('script');
    script.setAttribute('type', 'text/javascript');
    script.src = scriptUrl;
    frameDoc.body.appendChild(script);
  }

  // TODO(jasvir): This should pulled into a utility js file
  function escapeAttr(s) {
    var ampRe = /&/g;
    var ltRe = /[<]/g;
    var gtRe = />/g;
    var quotRe = /\"/g;
    return ('' + s).replace(ampRe, '&amp;')
      .replace(ltRe, '&lt;')
      .replace(gtRe, '&gt;')
      .replace(quotRe, '&#34;');
  }

  function installSyncScript(name, url) {
     if (!loaderDocument) {
       loaderDocument = createFrame('loader-frame').document;
     }
     // TODO(jasvir): This assignment pins the parent's handler
     // function and, iiuc, this reference is never cleared out.
     var result = ''
       + ('<script>var $name = parent.window["$name"];<\/script>'
           .replace(/[$]name/g, name))
       + ('<script type="text/javascript" src="$url"><\/script>'
           .replace(/[$]url/g, escapeAttr(url)));
     loaderDocument.write(result);
   }

  function joinUrl(base, path) {
    base = base.replace(/\/+$/, '');
    path = path.replace(/^\/+/, '');
    return base + '/' + path;
  }

  function documentBaseUrl() {
    var bases = document.getElementsByTagName('base');
    if (bases.length == 0) {
      return document.location.toString();
    } else if (bases.length == 1) {
      var href = bases[0].href;
      if (typeof href !== 'string') {
        throw new Error('Caja loader error: <base> without a href.');
      }
      return href;
    } else {
      throw new Error('Caja loader error: document contains multiple <base>.');
    }
  }

  //----------------

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
   * Read the given property of the given object. Exists only to work
   * around browser bugs where the answer depends on who's asking the
   * question.
   */
  function readPropertyAsHostFrame(object, property) {
    return object[property];
  }

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
      id = enforceType(imports['id___'], 'number', 'id');
    } else {
      id = imports['id___'] = registeredImports.length;
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
      var id = enforceType(imports['id___'], 'number', 'id');
      registeredImports[id] = void 0;
    }
  }

  return caja;
})();

// Exports for closure compiler.
if (typeof window !== 'undefined') {
  window['caja'] = caja;
}
