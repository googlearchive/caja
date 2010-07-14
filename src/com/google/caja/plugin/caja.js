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
 * @fileoverview Caja loader.
 *
 * In this mode of operation, the Cajita runtime is not loaded into the host
 * page. Therefore, there is no interference with any other library which might
 * be affected by the properties Cajita adds to every object. Instead, Cajita,
 * all further Caja runtime components, and all cajoled modules/gadgets are
 * loaded into an invisible <iframe> -- but are still given access to HTML 
 * elements in the original page.
 *
 * Usage:
 * <script src="caja.js"></script>
 * <script>
 *   loadCaja(function (caja) {
 *     ...
 *   }[, options])
 * </script>
 *
 * The loading in the iframe is (almost) necessarily asynchronous, so you
 * provide a callback to invoke when it is ready. The argument 'caja'
 * has the properties: cajita, hostTools, Q, ___. 
 *
 * hostTools is a HostTools instance (see
 * <http://code.google.com/p/google-caja/wiki/HostTools>) which has been
 * preconfigured to work with the specified Caja server and load modules
 * relative to the document in which caja.js is loading.
 *
 * options may be an object with the properties 'debug' and 'cajaServer'.
 * 'debug' if true causes unminified code to load, and possibly other help
 * for debugging. 'cajaServer' defaults to "http://caja.appspot.com/".
 *
 * @author kpreid@switchb.org
 * @requires document, setTimeout
 * @provides loadCaja
 */

var loadCaja;
(function () {
  
  function documentBaseURL() {
    var bases = document.getElementsByTagName("base");
    if (bases.length == 0) {
      return document.location.toString();
    } else if (bases.length == 1) {
      var href = bases[0].href;
      if (typeof href !== "string") {
        throw new Error("Caja loader error: <base> without a href.");
      }
      return href;
    } else {
      throw new Error("Caja loader error: document contains multiple <base>.");
    }
  }
  
  loadCaja = function (readyCallback, options) {
    if (typeof(readyCallback) !== "function") {
      throw new Error("loadCaja: readyCallback not given or not a function: " +
          readyCallback);
    }

    if (typeof(options) == "undefined") {
      options = {};
    } else if (typeof(options) !== "object") {
      throw new Error("loadCaja: options not an object, probably wrong: " +
          options);
    }

    var cajaServer = options.cajaServer;
    var debug = options.debug;
    
    if (typeof(cajaServer) === "undefined") {
      cajaServer = "http://caja.appspot.com/";
    } else if (typeof(cajaServer) !== "string") {
      throw new Error("loadCaja: nonstring specified as Caja server: " +
          cajaServer);
    }
    
    // create iframe to load Cajita runtime in
    var frame = document.createElement("iframe");
    // TODO: Put a class on this so if the host page cares about 'all iframes'
    // it can filter this out?
    
    // hide it
    frame.style.display = "none";
    frame.width = 0;
    frame.height = 0;

    // stick it arbitrarily in the document
    document.body.appendChild(frame);

    // arrange for client to be notified when all is ready
    function cajaIframeDone() {
      var w = frame.contentWindow;

      var hostTools = new w.HostTools();
      hostTools.setCajolerService(cajaServer + "cajole");
      hostTools.setBaseURL(documentBaseURL());
          // otherwise would be the frame's base URL
      
      // TODO(kpreid): justify this set of refs as being sufficient to do
      // everything the host page might want to do with Caja
      readyCallback({
        cajita: w.cajita,
        hostTools: hostTools,
        Q: w.Q,
        ___: w.___
      });
    }
    frame.contentWindow.cajaIframeDone = cajaIframeDone;

    var filename = debug ? "caja-iframe.js" : "caja-iframe-minified.js";
    
    function installScript() {
      // .contentDocument not IE-compatible
      var fd = frame.contentWindow.document;

      var fscript = fd.createElement("script");
      fscript.setAttribute("type", "text/javascript");
      fscript.src = cajaServer + filename;
      fd.body.appendChild(fscript);
    }
    
    // If this is done immediately instead of deferred, then on Firefox 3.6 the
    // iframe silently fails to show any DOM changes or load any scripts.
    setTimeout(installScript, 0);
  };
})();
