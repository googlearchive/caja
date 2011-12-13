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
 * @requires document, setTimeout, window
 * @provides caja
 */

var caja = (function () {
  var cajaBuildVersion = '%VERSION%';
  var defaultServer = 'http://caja.appspot.com/';
  var defaultFrameGroup;
  var readyQueue = [];
  var registeredImports = [];
  var nextId = 0;

  var UNREADY = 'UNREADY', PENDING = 'PENDING', READY = 'READY';
  var state = UNREADY;

  var GUESS = 'GUESS';

  var containerStyle =
    '.caja_outerContainer___ {' +
    '  display: inline;' +
    '  margin: 0;' +
    '  overflow: auto;' +
    '  padding: 0;' +
    '  position: relative;' +
    '}' +
    '.caja_innerContainer___, .caja_outerContainer___ > * {' +
    '  height: 100%;' +
    '  margin: 0;' +
    '  padding: 0;' +
    '  position: relative;' +
    '}';

  var uriPolicies = {
    net: {
      NO_NETWORK: {
        rewrite: function () { return null; }
      },
      ALL: {
        rewrite: function (uri) { return String(uri); }
      },
      only: policyOnly
    }
  };

  var caja = {
    // Normal entry points
    initialize: initialize,
    load: load,
    whenReady: whenReady,

    // URI policies
    policy: uriPolicies,

    // Reference to the taming frame in the default frameGroup
    iframe: null,

    // Taming functions for the default frameGroup
    grantMethod: premature,
    grantRead: premature,
    grantReadWrite: premature,
    markCtor: premature,
    markFunction: premature,
    markReadOnlyRecord: premature,
    markXo4a: premature,
    tame: premature,

    // Esoteric functions
    initFeralFrame: initFeralFrame,
    makeFrameGroup: makeFrameGroup,
    configure: makeFrameGroup
  };

  // Internal functions made available to FrameGroup maker
  var cajaInt = {
    documentBaseUrl: documentBaseUrl,
    getId: getId,
    getImports: getImports,
    joinUrl: joinUrl,
    loadCajaFrame: loadCajaFrame,
    prepareContainerDiv: prepareContainerDiv,
    unregister: unregister
  };

  return caja;

  //----------------

  function premature() {
    throw new Error('Calling taming function before Caja is ready');
  }

  /**
   * Returns a URI policy that allows one URI and denies the rest.
   */
  function policyOnly(allowedUri) {
    allowedUri = String(allowedUri);
    return {
      rewrite: function (uri) {
        uri = String(uri);
        return uri === allowedUri ? uri : null;
      }
    };
  }

  /**
   * Creates the default frameGroup with the given config.
   * See {@code makeFrameGroup} for config parameters.
   */
  function initialize(config) {
    if (state !== UNREADY) {
      throw new Error('Caja cannot be initialized more than once');
    }
    state = PENDING;
    makeFrameGroup(config, function (frameGroup) {
      defaultFrameGroup = frameGroup;
      caja.iframe = frameGroup.iframe;
      for (var i in caja) {
        if (caja[i] === premature) {
          caja[i] = frameGroup[i];
        }
      }
      state = READY;
      whenReady();
    });
  }

  /**
   * Creates a guest frame in the default frameGroup.
   */
  function load(div, uriPolicy, loadDone, opt_idClass) {
    uriPolicy = uriPolicy || caja.policy.net.NO_NETWORK;
    if (state === UNREADY) {
      initialize({});
    }
    whenReady(function () {
      defaultFrameGroup.makeES5Frame(div, uriPolicy, loadDone, opt_idClass);
    });
  }

  /**
   * Defers func until the default frameGroup is ready.
   */
  function whenReady(func) {
    if (typeof func === 'function') {
      readyQueue.push(func);
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
   *     forceES5Mode - If set to true or false, forces or prohibits ES5
   *         mode, rather than autodetecting browser capabilities. This
   *         should be used strictly for debugging purposes.
   *
   * @param config an object literal containing configuration paramters.
   * @param frameGroupReady function to be called back with a reference to
   *     the newly created frame group.
   */
  function makeFrameGroup(config, frameGroupReady) {
    initFeralFrame(window);
    config = resolveConfig(config);
    // TODO(felix8a): this should be === false, but SES isn't ready,
    // and fails on non-ES5 browsers (frameGroupReady doesn't run)
    if (config.forceES5Mode !== true || unableToSES()) {
      initES53(config, frameGroupReady);
    } else {
      trySES(config, frameGroupReady);
    }
  }

  // Returns a full config based on the given partial config.
  function resolveConfig(partial) {
    partial = partial || {};
    var full = {};
    full.server = String(
      partial.server || partial.cajaServer || defaultServer);
    full.resources = String(partial.resources || full.server);
    full.debug = !!partial.debug;
    full.forceES5Mode =
      'forceES5Mode' in partial ? !!partial.forceES5Mode : GUESS;
    if (partial.console) {
      full.console = partial.console;
    } else if (window.console && typeof window.console.log === 'function') {
      full.console = window.console;
    } else {
      full.console = undefined;
    }
    full.log = partial.log || function (varargs) {
      full.console && full.console.log.apply(full.console, arguments);
    };
    return full;
  }

  function initFeralFrame(feralWin) {
    if (feralWin.Object.FERAL_FRAME_OBJECT___ === feralWin.Object) {
      return;
    }
    addStyle(feralWin.document, containerStyle);
    feralWin.___ = {};
    feralWin.Object.FERAL_FRAME_OBJECT___ = feralWin.Object;
  }

  function addStyle(doc, styleText) {
    var style = doc.createElement('style');
    style.setAttribute('type', 'text/css');
    // IE style nodes need to be added to the DOM before setting cssText
    // http://msdn.microsoft.com/en-us/library/ms533698(v=vs.85).aspx
    doc.getElementsByTagName('head')[0].appendChild(style);
    if (style.styleSheet) {
      style.styleSheet.cssText = containerStyle;  // IE
    } else {
      style.appendChild(doc.createTextNode(containerStyle));
    }
  }

  //----------------

  function initES53(config, frameGroupReady) {
    // TODO(felix8a): with api change, can start cajoler early too
    var guestMaker = makeFrameMaker(config, 'es53-guest-frame');
    loadCajaFrame(config, 'es53-taming-frame', function (tamingWin) {
      var fg = tamingWin.ES53FrameGroup(
          cajaInt, config, tamingWin, window, guestMaker);
      frameGroupReady(fg);
    });
  }

  function trySES(config, frameGroupReady) {
    var guestMaker = makeFrameMaker(config, 'ses-guest-frame');
    loadCajaFrame(config, 'ses-taming-frame', function (tamingWin) {
      if (canSES(tamingWin.ses, config.forceES5Mode)) {
        var fg = tamingWin.SESFrameGroup(
            cajaInt, config, tamingWin, window, guestMaker);
        frameGroupReady(fg);
      } else {
        config.log('Unable to use SES.  Switching to ES53.');
        // TODO(felix8a): set a cookie to remember this?
        initES53(config, frameGroupReady);
      }
    });
  }

  function canSES(ses, force) {
    return (ses.ok() ||
            (force && ses.maxSeverity < ses.severities.NOT_ISOLATED));
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
      preload: function () {
        if (preState === IDLE) {
          preState = LOADING;
          preWin = null;
          loadCajaFrame(config, filename, function (win) {
            preWin = win;
            consumeIfReady();
          });
        }
      },
      make: function (onReady) {
        if (preState === LOADING) {
          preState = WAITING;
          preReady = onReady;
          consumeIfReady();
        } else {
          loadCajaFrame(config, filename, onReady);
        }
      }
    };
    self.preload();
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
    var url = joinUrl(
      config.resources,
      cajaBuildVersion + '/' + filename + (config.debug ? '.js' : '.opt.js'));
    // The particular interleaving of async events shown below has been found
    // necessary to get the right behavior on Firefox 3.6. Otherwise, the
    // iframe silently fails to invoke the cajaIframeDone___ callback.
    setTimeout(function () {
      frameWin.cajaIframeDone___ = function () {
        versionCheck(config, frameWin, filename);
        frameReady(frameWin);
      };
      setTimeout(function () {
        installScript(frameWin, url);
      }, 0);
    }, 0);
  }

  // Throws an error if frameWin has the wrong Caja version
  function versionCheck(config, frameWin, filename) {
    if (cajaBuildVersion !== frameWin.cajaBuildVersion) {
      var message =
        'Version error: caja.js version ' + cajaBuildVersion +
        ' does not match ' + filename + ' version ' +
        frameWin.cajaBuildVersion;
      config.log(message);
      throw new Error(message);
    }
  }

  function prepareContainerDiv(div, feralWin, opt_idClass) {
    if (div && feralWin.document !== div.ownerDocument) {
      throw '<div> provided for ES5 frame must be in main document';
    }
    var idClass = opt_idClass || ('caja-guest-' + nextId++ + '___');
    var inner = null;
    var outer = null;
    if (div) {
      inner = div.ownerDocument.createElement('div');
      inner.setAttribute('class', 'caja_innerContainer___');
      inner.setAttribute('title', '<Untrusted Content Title>');
      outer = div.ownerDocument.createElement('div');
      outer.setAttribute('class', 'caja_outerContainer___');
      // Move existing children (like static HTML produced by the cajoler)
      // into the inner container.
      while (div.firstChild) {
        inner.appendChild(div.firstChild);
      }
      outer.appendChild(inner);
      div.appendChild(outer);
    }
    return {
      idClass: idClass,
      inner: inner,
      outer: outer
    };
  }

  // Creates a new iframe and returns its contentWindow.
  function createFrame(opt_className) {
    var frame = document.createElement('iframe');
    frame.style.display = "none";
    frame.width = 0;
    frame.height = 0;
    frame.className = opt_className || '';
    document.body.appendChild(frame);
    return frame.contentWindow;
  }

  function installScript(frameWin, scriptUrl) {
    var frameDoc = frameWin.document;
    var script = frameDoc.createElement('script');
    script.setAttribute('type', 'text/javascript');
    script.src = scriptUrl;
    frameDoc.body.appendChild(script);
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
      id = enforceType(imports.id___, 'number', 'id');
    } else {
      id = imports.id___ = registeredImports.length;
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
      var id = enforceType(imports.id___, 'number', 'id');
      registeredImports[id] = void 0;
    }
  }
})();
